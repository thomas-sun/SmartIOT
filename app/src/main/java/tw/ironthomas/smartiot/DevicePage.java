package tw.ironthomas.smartiot;


import android.app.AlertDialog;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.zxing.activity.CaptureActivity;
import com.wrapp.floatlabelededittext.FloatLabeledEditText;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import javax.net.ssl.SSLSocketFactory;


public class DevicePage extends AppCompatActivity implements IVLCVout.Callback {

    MqttAndroidClient mqttAndroidClient;

    String server_ip;
    String camera_ip;

    private LibVLC mLibVLC = null;
    private Media mMedia = null;
    private MediaPlayer mMediaPlayer = null;
    private FrameLayout mVideoSurfaceFrame = null;

    private int mVideoHeight = 0;
    private int mVideoWidth = 0;
    private int mVideoVisibleHeight = 0;
    private int mVideoVisibleWidth = 0;
    private int mVideoSarNum = 0;
    private int mVideoSarDen = 0;

    ImageView img_camera;
    Button btn_up;
    Button btn_down;
    Button btn_stop;
    Button btn_lock;
    private SurfaceView mVideoSurface = null;

    String str_btn1;
    String str_btn2;
    String str_btn3;
    String str_btn4;
    int security;
    int m_screen_width;
    int m_screen_hight;
    TextView txt_device_name;

    String device_name;
    String manager_id;
    String device_id;
    boolean b_has_camera;

    public void setVibrate(){
        Vibrator myVibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
        myVibrator.vibrate(100);
    }

    boolean check_data()
    {
        if(device_name == null || manager_id == null || device_id == null || server_ip == null)
            return false;

        if(device_name.isEmpty() || manager_id.isEmpty() || device_id.isEmpty() || server_ip.isEmpty())
            return false;

        return true;
    }

    void GetScreenSize()
    {
        WindowManager manager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        m_screen_width = display.getWidth();
        m_screen_hight = display.getHeight();
    }

    void press(final String cmd)
    {
        setVibrate();
        publishMessage("press "+cmd);
    }


    void set_online(final boolean on_line)
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                if(txt_device_name != null) {
                    if (on_line == true)
                        txt_device_name.setTextColor(0xff00FF00);
                    else
                        txt_device_name.setTextColor(0xff000000);
                }

            }
        });
    }



    @Override
    protected void onPause(){
        super.onPause();
        try {
            mqttAndroidClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        onBackPressed();
    }


    void UpdateDeviceUI(String device_id, String item, String name) {
        SQLiteDatabase db = DBHelper.getDatabase(this);
        ContentValues cv = new ContentValues();

        cv.put(item, name);
        String where = "DEVICE_ID ='" + device_id + "'";

        // 執行修改資料並回傳修改的資料數量是否成功
        db.update("devices", cv, where, null);
    }


    void SetButtonText(final Button btn, final String str)
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                btn.setText(str);
            }
        });
    }

    void rename_dlg(final Button btn, final String db_key, final String old_name)
    {
        final LinearLayout item = (LinearLayout)LayoutInflater.from(DevicePage.this).inflate(R.layout.rename, null);
        EditText editText = ((FloatLabeledEditText)item.findViewById(R.id.txt_name)).getEditText();


        editText.setText(old_name);
        new AlertDialog.Builder(DevicePage.this)
                .setTitle(R.string.modify_device_name)
                .setView(item)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String str = ((FloatLabeledEditText)item.findViewById(R.id.txt_name)).getEditText().getText().toString();
                        if(!str.equals("")) {
                            UpdateDeviceUI(device_id, db_key, str);
                            SetButtonText(btn, str);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                    }
                })
                .show();

    }

    public void publishMessage(String publishMessage){

        String publishTopic = manager_id + "/" + device_id;

        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(publishMessage.getBytes());
            mqttAndroidClient.publish(publishTopic, message);

            if(!mqttAndroidClient.isConnected()){

            }
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent it = this.getIntent();
        if(it == null)
            finish();

        device_name = it.getStringExtra("device_name");
        manager_id = it.getStringExtra("manager_id");
        device_id = it.getStringExtra("device_id");
        server_ip = it.getStringExtra("server_ip");
        camera_ip = it.getStringExtra("camera_ip");

        str_btn1 = it.getStringExtra("btn1");
        str_btn2 = it.getStringExtra("btn2");
        str_btn3 = it.getStringExtra("btn3");
        str_btn4 = it.getStringExtra("btn4");
        security = it.getIntExtra("security", 0);

        if(camera_ip == null || camera_ip.equals("") || camera_ip.equals("none")) {
            b_has_camera = false;
            setContentView(R.layout.device_page);
        }
        else {
            b_has_camera = true;
            setContentView(R.layout.device_page_with_camera);
        }

        if(check_data() == false)
            finish();


        if(b_has_camera) {
            GetScreenSize();
            mVideoSurface = (SurfaceView) findViewById(R.id.surface_video);

            int video_size = (m_screen_width * 7) / 10;
            // TODO Auto-generated method stub
            android.widget.RelativeLayout.LayoutParams params = new android.widget.RelativeLayout.LayoutParams(video_size, video_size);
            params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
            params.addRule(RelativeLayout.BELOW, R.id.device_name);



            //params.gravity = Gravity.CENTER_HORIZONTAL;
            mVideoSurface.setLayoutParams(params);


            mVideoSurface.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    setVibrate();


                    Intent intent = new Intent(DevicePage.this, CameraSettingPage.class);
                    intent.putExtra("device_id", device_id);
                    startActivity(intent);
                    return false;
                }
            });

            mLibVLC = new LibVLC(this);
            mMediaPlayer = new MediaPlayer(mLibVLC);
            mVideoSurface = (SurfaceView) findViewById(R.id.surface_video);

        } else {
            img_camera = (ImageView)this.findViewById(R.id.img_camera);
            img_camera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setVibrate();

                    Intent intent = new Intent(DevicePage.this, CameraSettingPage.class);
                    intent.putExtra("device_id", device_id);
                    startActivity(intent);

                }
            });
        }



        txt_device_name =(TextView)this.findViewById(R.id.device_name);
        txt_device_name.setText(device_name);





        btn_up =(Button)this.findViewById(R.id.btn_up);
        btn_up.setText(str_btn1);
        btn_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                press("0");
            }
        });
        btn_up.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                rename_dlg((Button)view, "BTN1", str_btn1);
                return false;
            }
        });

        btn_down =(Button)this.findViewById(R.id.btn_down);
        btn_down.setText(str_btn3);
        btn_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                press("1");
            }
        });
        btn_down.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                rename_dlg((Button)view, "BTN3", str_btn3);
                return false;
            }
        });

        btn_stop =(Button)this.findViewById(R.id.btn_stop);
        btn_stop.setText(str_btn2);
        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                press("3");
            }
        });
        btn_stop.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                rename_dlg((Button)view, "BTN2", str_btn2);
                return false;
            }
        });


        btn_lock =(Button)this.findViewById(R.id.btn_lock);
        btn_lock.setText(str_btn4);
        btn_lock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                press("2");
            }
        });
        btn_lock.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                rename_dlg((Button)view, "BTN4", str_btn4);
                return false;
            }
        });


        SharedPreferences editor =
                PreferenceManager.getDefaultSharedPreferences(this);
        boolean safety = editor.getBoolean("safety", true);

        if(safety == true) {

            // 防止誤觸
            Intent intent = getIntent();
            if (!(intent != null && intent.getBooleanExtra("from_setting", false) == true)) {

                final EditText m_dialog_content = new EditText(this);
                new AlertDialog.Builder(this).setTitle(R.string.type_number).setIcon(
                        android.R.drawable.ic_dialog_info).setView(
                        m_dialog_content).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        if (!m_dialog_content.getText().toString().equals("1234"))
                            onBackPressed();

                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        onBackPressed();
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        onBackPressed();
                    }
                }).show();
            }
        }

        String serverUri;
        if(security == 0) serverUri = "tcp://"+server_ip;
        else serverUri = "ssl://"+server_ip;


        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri,  manager_id);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    //addToHistory("Reconnected to : " + serverURI);
                    // Because Clean Session is true, we need to re-subscribe
                    //subscribeToTopic();
                    Log.i("MQTT","重新連線了");
                    set_online(true);
                } else {
                    //addToHistory("Connected to: " + serverURI);
                    Log.i("MQTT","連線好了");
                    set_online(true);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                //addToHistory("The Connection was lost.");
                Log.i("MQTT","連線遺失");
                set_online(false);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                //addToHistory("Incoming message: " + new String(message.getPayload()));
                Log.i("MQTT","收到訊息");
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);

        if(security == 1)
            mqttConnectOptions.setSocketFactory(SSLSocketFactory.getDefault());

        // 支援ssl 參考
/*
        InputStream certificates = getResources().openRawResource(R.raw.server);
        InputStream pkcs12File = getResources().openRawResource(R.raw.client);
        String password = "111111";
        InputStream bksFile = HttpsUtils.pkcs12ToBks(pkcs12File, password);
        HttpsUtils.SSLParams sslParams = HttpsUtils.getSslSocketFactory(new InputStream[]{certificates}, bksFile, password);

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setSocketFactory(sslParams.sSLSocketFactory);
   */




        try {
            //addToHistory("Connecting to " + serverUri);
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    //subscribeToTopic();
                    //Log.i("MQTT","連線成功");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    //Log.i("MQTT","連線失敗");
                    //addToHistory("Failed to connect to: " + serverUri);
                }
            });


            if(mqttAndroidClient.isConnected()){
                txt_device_name.setTextColor(0xff00FF00);
            }


        } catch (MqttException ex){
            ex.printStackTrace();
            Log.i("MQTT","出錯了");
        }


        }




    @Override
    public void onNewLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        mVideoWidth = width;
        mVideoHeight = height;
        mVideoVisibleWidth = visibleWidth;
        mVideoVisibleHeight = visibleHeight;
        mVideoSarNum = sarNum;
        mVideoSarDen = sarDen;
        updateVideoSurfaces();
    }


    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {
    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {
    }

    private void updateVideoSurfaces() {

        return;
    }

    @Override
    public void onStart() {
        super.onStart();
        if(!b_has_camera)
            return;

        int cacheSize[] = {200,500,800,1100,1500};
        int network_cacheing = 2;


        final IVLCVout vlcVout = mMediaPlayer.getVLCVout();
        vlcVout.setVideoView(mVideoSurface);

        vlcVout.attachViews();
        mMediaPlayer.getVLCVout().addCallback(this);

        mMedia = new Media(mLibVLC, Uri.parse(camera_ip));
        mMedia.setHWDecoderEnabled(true, true);

        mMedia.addOption(":network-caching="+cacheSize[network_cacheing]);

        mMediaPlayer.setMedia(mMedia);
        mMediaPlayer.play();


    }

    @Override
    public void onStop() {
        super.onStop();
        if(!b_has_camera)
            return;
        mMediaPlayer.stop();
        mMediaPlayer.getVLCVout().detachViews();
        mMediaPlayer.getVLCVout().removeCallback(this);
    }

}
