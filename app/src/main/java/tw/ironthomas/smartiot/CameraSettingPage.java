package tw.ironthomas.smartiot;


import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.zxing.activity.CaptureActivity;
import com.wrapp.floatlabelededittext.FloatLabeledEditText;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.HWDecoderUtil;

public class CameraSettingPage extends AppCompatActivity implements IVLCVout.Callback {
    final static String TAG = "SmartIOT";

    private LibVLC mLibVLC = null;
    private Media   mMedia = null;
    private MediaPlayer mMediaPlayer = null;
    private FrameLayout mVideoSurfaceFrame = null;
    private SurfaceView mVideoSurface = null;

    int m_screen_width;
    int m_screen_hight;

    private int mVideoHeight = 0;
    private int mVideoWidth = 0;
    private int mVideoVisibleHeight = 0;
    private int mVideoVisibleWidth = 0;
    private int mVideoSarNum = 0;
    private int mVideoSarDen = 0;
    private int mChannelNumber = 0;

    public EditText mPassword;
    public EditText mId;
    public EditText mServerIP;
    public EditText mRTSP;
    public TextView mChannel;

    RadioButton rbtn_hikvision;
    RadioButton rbtn_other;

    Button btn_play;
    String device_id;
    String str_channel;


    public void setVibrate(){
        Vibrator myVibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
        myVibrator.vibrate(100);
    }

    void GetScreenSize()
    {
        WindowManager manager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        m_screen_width = display.getWidth();
        m_screen_hight = display.getHeight();
    }

    void UpdateCameraIP(String device_id, String camera_ip) {
        SQLiteDatabase db = DBHelper.getDatabase(this);
        ContentValues cv = new ContentValues();

        cv.put("CAMERA_IP", camera_ip);
        String where = "DEVICE_ID ='" + device_id + "'";

        db.update("devices", cv, where, null);
    }

    void play_stream(String camera_url)
    {
        mMedia = new Media(mLibVLC, Uri.parse(camera_url));
        mMedia.setHWDecoderEnabled(true, true);
        mMedia.addOption(":network-caching=800");
        mMediaPlayer.setMedia(mMedia);
        mMediaPlayer.play();
        Log.i(TAG, "play");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_setting_page);


        Intent it = this.getIntent();
        if(it == null)
            finish();

        device_id = it.getStringExtra("device_id");

        mLibVLC = new LibVLC(this);
        mMediaPlayer = new MediaPlayer(mLibVLC);
        mVideoSurface = (SurfaceView) findViewById(R.id.surface_video);

        GetScreenSize();

        int video_size = (m_screen_width * 7) / 10;

        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(video_size, video_size);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        mVideoSurface.setLayoutParams(params);


        rbtn_hikvision = (RadioButton)this.findViewById(R.id.hikvision_nvr);
        rbtn_other = (RadioButton)this.findViewById(R.id.other_nvr);
        rbtn_hikvision.setChecked(true);
        rbtn_other.setChecked(false);
        mPassword = ((FloatLabeledEditText)this.findViewById(R.id.edit_password)).getEditText();
        mId = ((FloatLabeledEditText)this.findViewById(R.id.txt_id)).getEditText();
        mServerIP = ((FloatLabeledEditText)this.findViewById(R.id.edit_server_ip)).getEditText();
        mRTSP = ((FloatLabeledEditText)this.findViewById(R.id.edit_rtsp)).getEditText();



        str_channel = CameraSettingPage.this.getResources().getString(R.string.channel);
        mChannel =(TextView)this.findViewById(R.id.channel);
        mChannelNumber = 1;
        mChannel.setText(str_channel+"1");




        SeekBar sb_channel;
        sb_channel = (SeekBar) findViewById(R.id.seekBar_channel);
        sb_channel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                mChannelNumber = progress + 1;
                mChannel.setText(str_channel+mChannelNumber);
            }
        } );


        btn_play =(Button)this.findViewById(R.id.btn_play);
        btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setVibrate();
                String camera_url = "";


                if(rbtn_hikvision.isChecked() == true) {

                    String password = mPassword.getText().toString();
                    String id = mId.getText().toString();
                    String ip =  mServerIP.getText().toString();

                    if(ip == null || id == null || password == null || ip.equals("") || id.equals("") || password.equals("")) {
                        mMediaPlayer.stop();
                        mMediaPlayer.setMedia(null);
                        UpdateCameraIP(device_id, "none");
                    } else {
                        //mMedia = new Media(mLibVLC, Uri.parse("rtsp://id:password@xxx.xxx.xxx.xxx/Streaming/Channels/ch02"));
                        camera_url = "rtsp://"+id+":"+password+"@"+ip+"/Streaming/Channels/"+mChannelNumber+"02";
                        UpdateCameraIP(device_id, camera_url);
                    }



                } else {
                    camera_url =  mRTSP.getText().toString();
                    if(camera_url == null || camera_url.equals("") ) {
                        mMediaPlayer.stop();
                        mMediaPlayer.setMedia(null);
                        UpdateCameraIP(device_id, "none");
                    } else {
                        UpdateCameraIP(device_id, camera_url);
                    }
                }

                if(camera_url.equals("") == false) {
                    play_stream(camera_url);
                }

            }
        });



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

       final IVLCVout vlcVout = mMediaPlayer.getVLCVout();
        vlcVout.setVideoView(mVideoSurface);

        vlcVout.attachViews();
        mMediaPlayer.getVLCVout().addCallback(this);

    }

    @Override
    public void onStop() {
        super.onStop();
        mMediaPlayer.stop();
        mMediaPlayer.getVLCVout().detachViews();
        mMediaPlayer.getVLCVout().removeCallback(this);
    }

}
