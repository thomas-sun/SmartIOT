package tw.ironthomas.smartiot;


import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class OTAParam {

    public Socket m_socket = null;
    public String server_ip;
    public int port;
    public OTAEvent event;


    public String fwid;
    public byte [] user1;
    public byte [] user2;

    private boolean unpackZip(String path)
    {
        InputStream is;
        ZipInputStream zis;
        int count;
        byte [] tmp = new byte[1024];

        try
        {
            String filename;
            is = new FileInputStream(path);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;

            while ((ze = zis.getNextEntry()) != null)
            {
                filename = ze.getName();

                if(filename.equals("fwid.txt")) {
                    ByteArrayOutputStream bao = new ByteArrayOutputStream(1024);
                    while ((count = zis.read(tmp)) != -1)
                    {
                        bao.write(tmp, 0, count);
                    }
                    bao.close();
                    fwid =new String(bao.toByteArray(),"UTF-8");
                } else if(filename.equals("user1.bin")) {
                    ByteArrayOutputStream bao = new ByteArrayOutputStream((int)ze.getSize());
                    while ((count = zis.read(tmp)) != -1)
                    {
                        bao.write(tmp, 0, count);
                    }
                    bao.close();
                    user1 = bao.toByteArray();
                } else if(filename.equals("user2.bin")) {
                    ByteArrayOutputStream bao = new ByteArrayOutputStream((int)ze.getSize());
                    while ((count = zis.read(tmp)) != -1)
                    {
                        bao.write(tmp, 0, count);
                    }
                    bao.close();
                    user2 = bao.toByteArray();
                }
                zis.closeEntry();
            }
            zis.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    boolean load_firmware(String filepath)
    {
        fwid = null;
        user1 = null;
        user2 = null;

        unpackZip(filepath);

        if(fwid == null || user1 == null || user2 == null)
            return false;

        return true;
    }
}