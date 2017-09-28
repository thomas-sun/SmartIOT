package tw.ironthomas.smartiot;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;



public class OTAClientTask extends AsyncTask<OTAParam, Integer , Long> {

    Socket m_socket = null;
    DataOutputStream m_outToClient = null;

    public Map<String, String> splitHttpParams(String content) {
        Map<String, String> paramMap = new HashMap<>();
        StringTokenizer tokenizer = new StringTokenizer(content, "&");
        while (tokenizer.hasMoreElements()) {
            String keyValue = tokenizer.nextToken();
            int index = keyValue.indexOf("=");
            if (index > 0) {
                String key = keyValue.substring(0, index);
                paramMap.put(key, keyValue.substring(index + 1));
            }
        }
        return paramMap;
    }

    protected Long doInBackground(OTAParam... cs) {
        Map<String, String> params;
        m_socket = cs[0].m_socket;

        System.out.println("Connection, sending data.");
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(m_socket.getInputStream()));
            m_outToClient = new DataOutputStream(m_socket.getOutputStream());

            String requestMessageLine = in.readLine();
            StringTokenizer tokenizedLine = new StringTokenizer(requestMessageLine);

            String token = tokenizedLine.nextToken();


            if (token.equals("GET")) {
                String fileName = tokenizedLine.nextToken();


                if (fileName.startsWith("/check") == true) {
                    int param_ptr = fileName.indexOf("?");
                    if(param_ptr > 0) {
                        params = splitHttpParams(fileName.substring(param_ptr+1));

                        String id = params.get("FWID");
                        if(id != null && id.equals(cs[0].fwid) ) {
                            m_outToClient.writeBytes("ok\r\n\r\n");
                            cs[0].event.on_ota_notification(R.string.check_firmware_version);
                            return 0L;
                        }
                    }

                    m_outToClient.writeBytes("fail\r\n\r\n");
                    cs[0].event.on_ota_finish(false);
                    return 0L;
                } else
                if (fileName.startsWith("/finish") == true) {
                    int param_ptr = fileName.indexOf("?");
                    if(param_ptr > 0) {
                        params = splitHttpParams(fileName.substring(param_ptr+1));

                        String result = params.get("result");
                        if(result != null && result.equals("success") ) {
                            m_outToClient.writeBytes("ok\r\n\r\n");
                            cs[0].event.on_ota_finish(true);
                            return 0L;
                        }
                    }
                    m_outToClient.writeBytes("fail\r\n\r\n");
                    cs[0].event.on_ota_finish(false);
                    return 0L;
                } else
                if (fileName.equals("/user1.bin") == true) {
                    cs[0].event.on_ota_notification(R.string.device_download_firmware);
                    m_outToClient.writeBytes("HTTP/1.0 200 OK\r\nContent-Type: application/octet-stream\r\nAccept-Ranges: bytes\r\nContent-Length: "+cs[0].user1.length+" \r\n\r\n");
                    m_outToClient.write(cs[0].user1, 0, cs[0].user1.length);
                    cs[0].event.on_ota_notification(R.string.download_complete);
                } else
                if (fileName.equals("/user2.bin") == true) {
                    cs[0].event.on_ota_notification(R.string.device_download_firmware);
                    m_outToClient.writeBytes("HTTP/1.0 200 OK\r\nContent-Type: application/octet-stream\r\nAccept-Ranges: bytes\r\nContent-Length: "+cs[0].user2.length+" \r\n\r\n");
                    m_outToClient.write(cs[0].user2, 0, cs[0].user2.length);
                    cs[0].event.on_ota_notification(R.string.download_complete);
                }

                return 0L;


            } else if (token.equals("HEAD")) {
                String fileName = tokenizedLine.nextToken();
                int len;
                if(fileName.equals("/user1.bin"))
                    len = cs[0].user1.length;
                else if(fileName.equals("/user2.bin"))
                    len = cs[0].user2.length;
                else len = 0;

                m_outToClient.writeBytes("HTTP/1.0 200 OK\r\nContent-Type: application/octet-stream\r\nAccept-Ranges: bytes\r\nContent-Length: "+len+" \r\n\r\n");
                return 0L;
            } else {
                m_outToClient.writeBytes("fail\r\n\r\n");
                cs[0].event.on_ota_notification(R.string.unrecognized_command);
                cs[0].event.on_ota_finish(false);
                return 0L;
            }

        } catch (IOException e1) {
            e1.printStackTrace();
        }

        //if (isCancelled())

        //publishProgress((int) ((i / (float) count) * 100));
        return 0L;
    }

    protected void onProgressUpdate(Integer... progress) {



    }

    protected void onPostExecute(Long result) {
        if (m_socket != null) {

            try {
                m_outToClient.flush();
                m_socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            m_outToClient = null;
            m_socket = null;
        }
    }
}