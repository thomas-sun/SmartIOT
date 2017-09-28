package tw.ironthomas.smartiot;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;

import tw.ironthomas.smartiot.smartconfig.EsptouchTask;
import tw.ironthomas.smartiot.smartconfig.IEsptouchListener;
import tw.ironthomas.smartiot.smartconfig.IEsptouchResult;
import tw.ironthomas.smartiot.smartconfig.IEsptouchTask;
import tw.ironthomas.smartiot.smartconfig.task.__IEsptouchTask;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

public class SmartConfigTask extends AsyncTask<String, Void, List<IEsptouchResult>> {
    final static String TAG = "SmartIOT";

    int handshake;
    public SettingPage m_sp = null;


    private IEsptouchTask mEsptouchTask;
    // without the lock, if the user tap confirm and cancel quickly enough,
    // the bug will arise. the reason is follows:
    // 0. task is starting created, but not finished
    // 1. the task is cancel for the task hasn't been created, it do nothing
    // 2. task is created
    // 3. Oops, the task should be cancelled, but it is running
    private final Object mLock = new Object();


    private IEsptouchListener myListener = new IEsptouchListener() {

        @Override
        public void onEsptouchResultAdded(final IEsptouchResult result) {
            // onEsptoucResultAddedPerform(result);
        }
    };


    @Override
    protected void onPreExecute() {
        ProgressDialog dlg = new ProgressDialog(m_sp);
        m_sp.mProgressDialog = dlg;

        Resources r = m_sp.getResources();
        dlg.setMessage(r.getString(R.string.scanning));
        dlg.setCanceledOnTouchOutside(false);
        dlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                synchronized (mLock) {
                    if (__IEsptouchTask.DEBUG) {
                        Log.i(TAG, "progress dialog is canceled");
                    }
                    if (mEsptouchTask != null) {
                        mEsptouchTask.interrupt();
                    }
                }
            }
        });


        dlg.setButton(DialogInterface.BUTTON_POSITIVE,
                r.getString(R.string.wait), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        dlg.show();
        dlg.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);

    }

    @Override
    protected List<IEsptouchResult> doInBackground(String... params) {
        int taskResultCount = -1;
        synchronized (mLock) {
            // !!!NOTICE
            String apSsid = m_sp.getWifiAdmin().getWifiConnectedSsidAscii(params[0]);
            String apBssid = params[1];
            String apPassword = params[2];
            String taskResultCountStr = params[3];
            taskResultCount = Integer.parseInt(taskResultCountStr);
            mEsptouchTask = new EsptouchTask(apSsid, apBssid, apPassword, m_sp);
            mEsptouchTask.setEsptouchListener(myListener);
        }
        List<IEsptouchResult> resultList = mEsptouchTask.executeForResults(taskResultCount);
        return resultList;
    }


    void start_handshake(final InetAddress addr)
    {
        handshake = 0;

        Thread rt = new Thread(new Runnable() {
            @Override
            public void run() {
                DatagramSocket ds = null;
                boolean bSuccess = false;
                try {
                    ds = new DatagramSocket(19760);    //把這個Socket綁在1024這個Port
                    ds.setBroadcast(true);
                    ds.setSoTimeout(30000);
                    DatagramPacket receivedPacket = new DatagramPacket(new byte[256], 256);
                    Log.i(TAG, "start receive");
                    while(handshake != -1) {
                        ds.receive(receivedPacket);

                        int len = receivedPacket.getLength();
                        if(len != 0) {
                            String msg = new String(receivedPacket.getData(), 0, len);
                            Log.i(TAG, "receivedPacket:" + msg);
                            if(msg.startsWith("**#smart_device#** id_ok")) {
                                if(handshake == 0)
                                    handshake = 1;
                            } else if(msg.startsWith("**#smart_device#** device_id:")) {
                                bSuccess = true;
                                handshake = 2;

                                String device_name = m_sp.mDevice_name.getText().toString();
                                if(device_name.isEmpty()) {
                                    device_name = "No name";
                                }

                                int idx_product_id = msg.indexOf("product_id:");
                                String product_id;
                                String device_id;

                                if(idx_product_id > 0) {
                                    device_id = msg.substring(29, idx_product_id-1); // **#smart_device#** device_id:
                                    product_id = msg.substring(idx_product_id+11); // **#smart_device#** device_id:
                                } else {
                                    device_id = msg.substring(29); // **#smart_device#** device_id:
                                    product_id = "0";
                                }


                                m_sp.on_sc_new_user(device_name, device_id);
                                break;
                            }

                        }else {
                            Log.i(TAG, "receivedPacket: 0");
                        }
                    }
                    ds.close();
                    ds = null;

                } catch (IOException e) {
                    e.printStackTrace();
                    if(ds != null) {
                        ds.close();
                        ds = null;
                    }
                }


                if(bSuccess == true) {
                    m_sp.progress_box_confirm(R.string.pairing_success);
                } else {
                    m_sp.progress_box_confirm(R.string.pairing_failed);
                }


            }
        });
        rt.start();


        Thread st = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramSocket socket = new DatagramSocket();
                    DatagramPacket localDatagramPacket = null;
                    String  msg;
                    String server_path;
                    if(m_sp.m_ssh)
                        server_path = " server_s://"+m_sp.m_server_ip;
                    else
                        server_path = " server://"+m_sp.m_server_ip;

                    for (int x = 0; x < 30 && handshake != -1; x++) {
                        if(handshake == 0) {
                            msg = "**#smart_manager#** manager_id:" + m_sp.mManager_ID + server_path +"\r\n";
                            localDatagramPacket = new DatagramPacket(msg.getBytes(), msg.length(), addr, 19760);
                            socket.send(localDatagramPacket);
                        }
                        else if(handshake == 1) {
                            msg = "**#smart_manager#** device_id\r\n";
                            localDatagramPacket = new DatagramPacket(msg.getBytes(), msg.length(), addr, 19760);
                            socket.send(localDatagramPacket);
                        }
                        else if(handshake == 2) {
                            msg = "**#smart_manager#** over\r\n";
                            localDatagramPacket = new DatagramPacket(msg.getBytes(), msg.length(), addr, 19760);
                            socket.send(localDatagramPacket);
                        }

                        Thread.sleep(1000);
                    }


                    socket.close();
                } catch (UnknownHostException e) {

                } catch (SocketException e) {
                    if (__IEsptouchTask.DEBUG) {
                        Log.e(TAG, "SocketException");
                    }
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        st.start();

    }


    @Override
    protected void onPostExecute(List<IEsptouchResult> result) {
        IEsptouchResult firstResult = result.get(0);
        // check whether the task is cancelled and no results received
        if (!firstResult.isCancelled()) {
            int count = 0;
            if (firstResult.isSuc()) {
                m_sp.progress_box_message(R.string.handshake);
                start_handshake(firstResult.getInetAddress());

            } else {
                m_sp.progress_box_confirm(R.string.scan_failed);
            }
        }
    }
}

