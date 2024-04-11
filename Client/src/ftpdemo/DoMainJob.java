package ftpdemo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;

import java.nio.file.Path;

import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;

import java.nio.file.attribute.BasicFileAttributes;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * 此類別負責執行主要工作,從遠端 FTP 伺服器下載檔案或目錄,
 * 將它們移動到指定的本地位置,並執行其他相關任務。
 */
public class DoMainJob {
    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    /*-R – 於上傳目錄的時後用的到，example: ncftpput -m -u ftpuser -p ftppassword serverIP /目的根目錄/ 來源目錄/
      -m – 當目的端無此目錄，來源端上傳檔案時建立此目錄，example: ncftpput -m -u ftpuser -p ftppassword serverIP /目的根目錄/不存在的目錄 來源檔案
      -DD  下載完畢刪除來源檔案
      -z – 要續傳
      -Z – 不要續傳(預設)
    */

    /**
     * main。
     *
     * @param args 命令列參數,預期只有一個參數用於指定屬性檔案路徑
     */
    public static void main(String[] args) {
        try {
            Properties props = loadProperties(args);
            executeJob(props);
        } catch (IOException e) {
            LOGGER.severe("Failed to load properties file: " + e.getMessage());
        }
    }

    /**
     * executeJob。
     *
     * @param props 用於執行工作的屬性
     * @throws IOException 如果發生 I/O 錯誤
     */
    private static void executeJob(Properties props) throws IOException {
        String logPath = props.getProperty("ftp.logdir");
        setupLogging(logPath);

        LOGGER.info("FTP file download from Tpfp01 ...");

        boolean shouldMoveFiles = isTrue(props.getProperty("move.mk", "move.mk has not set"));
        boolean shouldExecuteStoredProcedure = isTrue(props.getProperty("store.mk", "store.mk has not set"));
        //判斷是否整擋
        if (shouldExecuteStoredProcedure) {
            executeStoredProcedure(props);
        }
        //是否把檔案搬到備份目錄
        if (shouldMoveFiles) {
            moveFiles(props);
        }
        //是否清除超過30天的log檔
        if (isTrue(props.getProperty("del.log.mk", "del.log.mk has not set"))) {
            deleteOldLogFiles(Paths.get(logPath), 30);
        }
    }

    /**
     * isTrue。
     *
     * @param value 要評估的值
     * @return 如果值代表真值則返回true,否則返回false
     */
    private static boolean isTrue(String value) {
        return "Y".equalsIgnoreCase(value) || Boolean.parseBoolean(value);
    }

    /**
     * setupLogging。
     *
     * @param logPath 日誌檔案路徑
     * @throws IOException 如果發生 I/O 錯誤
     */
    private static void setupLogging(String logPath) throws IOException {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd.HHmmss");
        Date date = new Date();
        String executedTime = dateFormat.format(date);
        String logFileName = String.format("%s/%s.log", logPath, executedTime);
        File logFile = new File(logFileName);
        PrintStream logStream = new PrintStream(logFile);
        System.setOut(logStream);
        System.setErr(logStream);
    }

    /**
     * 執行指定的store procedure。
     *
     * @param props 包含存儲過程名稱和其他配置的屬性
     */
    private static void executeStoredProcedure(Properties props) {
        //        String dbAlias = props.getProperty("db.alias", "db.alias has not set");
        //        String username = props.getProperty("db.username", "db.username has not set");
        //        String password = props.getProperty("db.password", "db.password has not set");
        String procedure = props.getProperty("store.procedure", "store.procedure has not set");
        boolean useRemotePath = isTrue(props.getProperty("use.remote.path"));

        LOGGER.info("Connecting to database [" + props.getProperty("db.alias") + "] and executing procedure...");

        try (Connection conn = new ConnectDb(props).getConnection();
             CallableStatement cstmt = conn.prepareCall("{call " + procedure + "}")) {

            cstmt.execute();
            LOGGER.info("Procedure executed successfully: " + procedure);

            if (useRemotePath) {
                downloadRemoteFiles(props, conn, true); //使用遠端目錄下載
            } else {
                downloadRemoteFiles(props, conn, false); //指定遠端檔案下載
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to execute stored procedure: " + e.getMessage());
        }
    }

    /**
     * 下載伺服器上的檔案。
     *
     * @param props       包含遠端路徑和本地路徑的屬性
     * @param conn        資料庫連線
     * @param isDirectory 指示是否下載整個目錄,如果為false則下載單個檔案
     * @throws SQLException 如果發生SQL異常
     */
    private static void downloadRemoteFiles(Properties props, Connection conn,
                                            boolean isDirectory) throws SQLException {
        LOGGER.info("Retrieving remote " + (isDirectory ? "paths" : "files") + " for FTP download...");

        String query =
            "SELECT sqno, remotepath, " + (isDirectory ? "localpath" : "remotefile, localpath") + " FROM ftpgetdemo";

        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rset = stmt.executeQuery()) {

            while (rset.next()) {
                int sqno = rset.getInt(1);
                String remotePath = rset.getString(2);
                String localPath = isDirectory ? rset.getString(3) : rset.getString(4);
                String remoteFile = isDirectory ? null : rset.getString(3);

                LOGGER.info("SQNO: " + sqno + ", REMOTEPATH: " + remotePath +
                            (isDirectory ? ", LOCALPATH: " + localPath : ", REMOTEFILE: " + remoteFile));

                if (isDirectory) {
                    loginRemoteFtpThenDownload(props, remotePath, null, localPath, true); // 下載整個目錄
                } else {
                    loginRemoteFtpThenDownload(props, remotePath, remoteFile, localPath, false); // 下載單個文件
                }
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.severe("Failed to download remote " + (isDirectory ? "directories" : "files") + ": " +
                          e.getMessage());
        }
    }

    /**
     * 登入FTP伺服器進行下載檔案。
     *
     * @param props      包含 FTP 主機、使用者名稱和密碼的屬性
     * @param remotePath 遠端路徑
     * @param remoteFile 遠端檔案名稱,如果為null則下載整個目錄
     * @param localPath  本地路徑
     * @param isDirectory 指示是否下載整個目錄
     * @throws IOException            如果發生 I/O 錯誤
     * @throws InterruptedException   如果進程被中斷
     */
    private static void loginRemoteFtpThenDownload(Properties props, String remotePath, String remoteFile,
                                                   String localPath, boolean isDirectory) throws IOException,
                                                                                                 InterruptedException {
        String ftpHost = props.getProperty("ftp.host", "ftp.host has not set");
        String ftpUsername = props.getProperty("ftp.username", "ftp.username has not set");
        String ftpPassword = props.getProperty("ftp.password", "ftp.password has not set");
        String ftpLogDir = props.getProperty("ftp.logdir", "ftp.logdir has not set");
        String logfile = new SimpleDateFormat("yyyyMMdd.HHmmss").format(new Date());

        String[] args;
        if (isDirectory) {
            args = new String[] {
                "i:/bin/ncftpget.exe", "-u", ftpUsername, "-p", ftpPassword, "-d", ftpLogDir + logfile + "_.log", "-R",
                ftpHost, localPath, remotePath
            };
        } else {
            //下載指定檔案
            args = new String[] {
                "i:/bin/ncftpget.exe", "-u", ftpUsername, "-p", ftpPassword, "-d", ftpLogDir + logfile + "_.log", "-R",
                ftpHost, localPath, remotePath + "/" + remoteFile
            };
        }
        //建立暫存目錄
        createDirectory(localPath);
        //        執行下載
        executeProcess(args);
    }

    /**
     * createDirectory。
     *
     * @param path 要建立的目錄路徑
     * @throws IOException 如果發生 I/O 錯誤
     */
    private static void createDirectory(String path) throws IOException {
        //建立指定路徑之資料夾
        File f = new File(path);
        if (f.mkdirs())
            System.out.println("建立目錄:" + path);
    }

    /**
     * 執行FTP下載(啟動ncftpget)。
     *
     * @param args 命令列參數
     * @throws IOException            如果發生 I/O 錯誤
     * @throws InterruptedException   如果進程被中斷
     */
    private static void executeProcess(String[] args) throws IOException, InterruptedException {
        LOGGER.info("Starting FTP download...");
        // 創建進程建構器
        ProcessBuilder pb = new ProcessBuilder(args);
        // 啟動進程
        Process process = pb.start();
        // 等待命令執行完成
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            LOGGER.info("File(s) downloaded successfully.");
        } else {
            LOGGER.severe("Failed to download file(s). Exit code: " + exitCode);
            throw new IOException("Failed to download file(s). Exit code: " + exitCode);
        }
    }

    /**
     * 將下載檔案移置備份目錄。
     *
     * @param props 包含來源和目的地路徑的屬性
     * @throws IOException 如果發生 I/O 錯誤
     */
    private static void moveFiles(Properties props) throws IOException {
        Path srcPath = Paths.get(props.getProperty("move.srcPath", "move.srcPath has not set"));
        Path dstPath = Paths.get(props.getProperty("move.dstPath", "move.destPath has not set"));

        LOGGER.info("Copying files from: " + srcPath + " to: " + dstPath);
        copyDirectory(srcPath, dstPath); //複製來源檔案到備份目錄
        LOGGER.info("Deleting files from: " + srcPath);
        deleteDirectory(srcPath); //刪除來源檔案
    }

    /**
     * 複製來源目錄。
     *
     * @param source 來源目錄路徑
     * @param target 目的地目錄路徑
     * @throws IOException 如果發生 I/O 錯誤
     */
    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectory(targetDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * 刪除指定目錄。
     *
     * @param directory 要刪除的目錄路徑
     * @throws IOException 如果發生 I/O 錯誤
     */
    private static void deleteDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * 刪除超過保留天數的log檔。
     *
     * @param logDir     日誌目錄路徑
     * @param daysToKeep 要保留的天數
     * @throws IOException 如果發生 I/O 錯誤
     */
    private static void deleteOldLogFiles(Path logDir, int daysToKeep) throws IOException {
        long thresholdMillis = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L);

        Files.walkFileTree(logDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (attrs.lastModifiedTime().toMillis() < thresholdMillis) {
                    Files.delete(file);
                    LOGGER.info("Deleted old log file: " + file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * 載入屬性檔。
     *
     * @param args 命令列參數,預期包含屬性檔案路徑
     * @return 屬性對象
     * @throws IOException 如果發生 I/O 錯誤
     */
    private static Properties loadProperties(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: java DoFtp2 <properties_file>");
        }

        String propFilePath = args[0];
        Properties properties = new Properties();

        try (InputStream input = Files.newInputStream(Paths.get(propFilePath))) {
            properties.load(input);
        }
        return properties;
    }
}
