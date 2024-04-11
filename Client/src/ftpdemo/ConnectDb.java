package ftpdemo;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.Properties;

import oracle.jdbc.pool.OracleDataSource;

/**
 * 此類別用於建立與 Oracle 資料庫的連線。
 */
public class ConnectDb {
    private String dbName;
    private String user;
    private String password;
    private String notesId;
    //        final static String TNSNAME = "C:\\Ora12c\\network\\ADMIN";
    final static String TNSNAME = "C:/app/Administrator/product/11.2.0/client_1/network/admin";

    /**
     * 建構子,使用提供的屬性初始化資料庫連線參數。
     *
     * @param prop 包含資料庫連線參數的屬性對象
     */
    public ConnectDb(Properties prop) {
        super();
        this.user = prop.getProperty("db.username");
        this.password = prop.getProperty("db.password");
        this.dbName = prop.getProperty("db.alias");
        this.notesId = prop.getProperty("notesId");
    }

    /**
     * 獲取與 Oracle 資料庫的連線。
     *
     * @return 與資料庫的連線對象
     * @throws SQLException 如果建立連線時發生 SQL 異常
     */
    public Connection getConnection() throws SQLException {
        System.setProperty("oracle.net.tns_admin", TNSNAME);
        OracleDataSource ods = new OracleDataSource();
        ods.setUser(user);
        ods.setPassword(password);
        ods.setTNSEntryName(dbName);
        ods.setDriverType("thin");
        Connection conn = ods.getConnection();
        conn.setClientInfo("OCSID.ACTION", "Ncftpget");

        return conn;
    }
}
