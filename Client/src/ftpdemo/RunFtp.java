package ftpdemo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * RunFtp。
 */
public class RunFtp {
    private BufferedReader error;
    private BufferedReader op;
    private int exitVal;

    /**
     * executeExe。
     *
     * @param exeFilePath 可執行檔案的路徑
     * @param args        傳遞給可執行檔案的參數
     * @throws Exception 如果執行可執行檔案時發生異常
     */
    public void executeExe(String exeFilePath, String args) throws Exception {
        try {
            Runtime re = Runtime.getRuntime();
            Process command = re.exec("cmd /c " + exeFilePath + " " + args);
            //            System.out.println("Process exitValue: " + exitVal);
            this.error = new BufferedReader(new InputStreamReader(command.getErrorStream()));
            this.op = new BufferedReader(new InputStreamReader(command.getInputStream()));
            // Wait for the application to Finish
            command.waitFor();
            this.exitVal = command.exitValue();
            if (this.exitVal != 0) {
                throw new IOException("Failed to execute exe, " + this.getExecutionLog());
            }
        } catch (final IOException | InterruptedException e) {
            throw new Exception(e);
        }
    }

    /**
     * getExecutionLog。
     *
     * @return 執行日誌字串,包含退出值、錯誤輸出和標準輸出
     * @throws IOException 如果讀取執行日誌時發生 I/O 異常
     */
    public String getExecutionLog() throws IOException {
        String error = "";
        String line;
        while ((line = this.error.readLine()) != null) {
            error = error + "\n" + line;
        }
        String output = "";
        while ((line = this.op.readLine()) != null) {
            output = output + "\n" + line;
        }
        this.error.close();
        this.op.close();
        return "exitVal: " + this.exitVal + ", error: " + error + ", output: " + output;
    }
}

