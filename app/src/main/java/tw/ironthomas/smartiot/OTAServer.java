package tw.ironthomas.smartiot;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class OTAServer {

    String fwid;
    byte [] user1 = null;
    byte [] user2 = null;
    ServerSocket ss;
    public SettingPage m_sp;
    boolean shutdown_flag;

    public void Shutdown()
    {
        shutdown_flag = true;
        if(ss != null) {
            try {
                ss.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    public void run(OTAParam param_in) throws Exception {

        //load_firmware("/sdcard/firmware.zip");

        m_sp.on_ota_start();


        shutdown_flag = false;
        ss = new ServerSocket();
        ss.setReuseAddress(true);
        ss.bind(new InetSocketAddress(param_in.port));
        Socket serverSocket;

        while(shutdown_flag == false) {
            serverSocket = null;
            try {
                serverSocket = ss.accept();
            }
            catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            if(serverSocket != null) {
                OTAClientTask serverAsyncTask = new OTAClientTask();
                OTAParam param = new OTAParam();

                param.m_socket = serverSocket;
                param.fwid = param_in.fwid;
                param.user1 = param_in.user1;
                param.user2 = param_in.user2;
                param.event = m_sp;

                serverAsyncTask.execute(param);
            }



        }
        ss = null;

    }
}