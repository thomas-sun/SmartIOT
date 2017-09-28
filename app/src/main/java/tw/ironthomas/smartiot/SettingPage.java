package tw.ironthomas.smartiot;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import com.google.zxing.activity.CaptureActivity;
import tw.ironthomas.smartiot.smartconfig.EspWifiAdminSimple;
import tw.ironthomas.smartiot.smartconfig.task.__IEsptouchTask;
import com.wrapp.floatlabelededittext.FloatLabeledEditText;


import java.util.Random;


public class SettingPage extends AppCompatActivity implements OTAEvent, SCEvent {
    final static String TAG = "SmartIOT";
    Button btn_add;
    Button btn_scan;
    Button btn_upgrade_firmware;
    Switch sw_safety;
    Switch sw_ssh;
    public OTAServer m_ota;


    TextView mSSID;
    String mBSSID;
    public EditText mPassword;
    public EditText mDevice_name;
    public EditText mServer_ip;
    public ProgressDialog mProgressDialog;
    private EspWifiAdminSimple mWifiAdmin;
    public String mManager_ID;
    public String m_server_ip;
    public boolean m_ssh;
    public String m_local_ip;
    boolean bOTA;



    Activity mSelf;

    char[] chars_table = {
            'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
            'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
            '0','1','2','3','4','5','6','7','8','9' };

    public String KeyGenerator(int len)
    {

        Random ran = new Random();
        char [] key = new char[len];
        for (int x = 0; x < len; x++) {
            key[x] = chars_table[ran.nextInt(62)];
        }
        return new String(key);

    }


    public void setVibrate(){
        Vibrator myVibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
        myVibrator.vibrate(100);
    }

    public EspWifiAdminSimple getWifiAdmin()
    {
        return mWifiAdmin;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // display the connected ap's ssid

        String apSsid = mWifiAdmin.getWifiConnectedSsid();
        if (apSsid != null) {
            mSSID.setText(apSsid);
        } else {
            mSSID.setText("");
        }

        m_local_ip = mWifiAdmin.getWifiConnectedIpAddress();

        //String ip =  mWifiAdmin.getWifiConnectedIpAddress();
        // check whether the wifi is connected
        boolean isApSsidEmpty = TextUtils.isEmpty(apSsid);
        btn_add.setEnabled(!isApSsidEmpty);

    }


    void progress_box_message(final int msg)
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                Resources r = SettingPage.this.getResources();
                if(mProgressDialog != null)
                mProgressDialog.setMessage(r.getString(msg));
            }
        });
    }


    void progress_box_confirm(final int msg)
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                Resources r = SettingPage.this.getResources();
                mProgressDialog.setMessage(r.getString(msg));
                mProgressDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setEnabled(true);
                mProgressDialog.getButton(DialogInterface.BUTTON_POSITIVE).setText(
                        R.string.yes);

            }
        });
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            String key = data.getExtras().getString("result");

            if (key.startsWith("**smart_rc_device_personal**")) {

                boolean ssh;
                int idx_device_id = key.indexOf("device_id:");
                int idx_device_name = key.indexOf("device_name:");
                int idx_manager_id = key.indexOf("manager_id:");
                int idx_server_ip0 = key.indexOf("server_ip:");
                int idx_server_ip1 = key.indexOf("server_s_ip:");
                int idx_camera_ip = key.indexOf("camera_ip:");
                int server_tag_length;
                int idx_server_ip;
                if(idx_server_ip1 > idx_server_ip0) {
                    idx_server_ip = idx_server_ip1;
                    server_tag_length = 12;
                    ssh = true;
                } else {
                    idx_server_ip = idx_server_ip0;
                    server_tag_length = 10;
                    ssh = false;
                }


                if(!(idx_device_name > idx_device_id && idx_manager_id > idx_device_name && idx_server_ip > idx_manager_id)) {
                    MessageBox(R.string.error_format);
                    return;
                }

                String device_id = key.substring(idx_device_id + 10, idx_device_name - 1);
                String device_name64 = key.substring(idx_device_name + 12, idx_manager_id - 1);
                String manager_id = key.substring(idx_manager_id + 11, idx_server_ip - 1);
                String server_ip;
                String camera_ip;

                if(idx_camera_ip > 0) {
                    server_ip = key.substring(idx_server_ip + server_tag_length, idx_camera_ip - 1);
                    camera_ip = key.substring(idx_camera_ip + 10);
                } else {
                    server_ip = key.substring(idx_server_ip + server_tag_length);
                    camera_ip = "none";
                }



                String device_name =  new String(Base64.decode(device_name64.getBytes(), Base64.NO_WRAP));

                if(FindDevice(device_id) == false) {
                    AddDeviceDB(manager_id, device_name, device_id, server_ip, camera_ip, ssh == true ? 1 : 0);
                } else {
                    UpdateDeviceDB(manager_id, device_name, device_id, server_ip, camera_ip, ssh == true ? 1 : 0);
                }
                setVibrate();
                MessageBox(R.string.device_has_been_added);

            }

        }
    }

    @Override
    public void onBackPressed() {
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(this).edit();

        editor.putString("server_ip",mServer_ip.getText().toString());
        editor.commit();

        super.onBackPressed();
    }


    void MessageBox(final int title)
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                new AlertDialog.Builder(SettingPage.this).setTitle(title).setIcon(
                        android.R.drawable.ic_dialog_info).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {

                    }
                }).show();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_page);
        mSelf = this;


        mManager_ID = KeyGenerator(32);


        mSSID = ((FloatLabeledEditText)this.findViewById(R.id.txt_ssid)).getEditText();
        mPassword = ((FloatLabeledEditText)this.findViewById(R.id.edit_password)).getEditText();
        mDevice_name = ((FloatLabeledEditText)this.findViewById(R.id.edit_device_name)).getEditText();
        mServer_ip = ((FloatLabeledEditText)this.findViewById(R.id.edit_server_ip)).getEditText();




        SharedPreferences editor =
                PreferenceManager.getDefaultSharedPreferences(this);


        m_ssh = editor.getBoolean("ssh", false);
        boolean safety = editor.getBoolean("safety", true);
        mServer_ip.setText(editor.getString("server_ip","iot.eclipse.org:1883"));


        mWifiAdmin = new EspWifiAdminSimple(this);





        btn_upgrade_firmware =(Button)this.findViewById(R.id.btn_upgrade_firmware);
        btn_upgrade_firmware.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setVibrate();

                String apSsid = mSSID.getText().toString();
                String apPassword = mPassword.getText().toString();
                String apBssid = mWifiAdmin.getWifiConnectedBssid();

                if (__IEsptouchTask.DEBUG) {
                    Log.d(TAG, "mBtnConfirm is clicked, mEdtApSsid = " + apSsid
                            + ", " + " mEdtApPassword = " + apPassword);
                }

                OTAParam param = new OTAParam();
                if(param.load_firmware("/sdcard/firmware.zip") == false) {
                    MessageBox(R.string.firmware_load_fail);
                } else {
                    param.server_ip = m_local_ip;
                    param.port = 3721;

                    UpgradeFirmwareTask uft = new UpgradeFirmwareTask();
                    uft.m_sp = SettingPage.this;
                    uft.param = param;
                    uft.execute(apSsid, apBssid, apPassword, "1");
                }



            }
        });



        btn_add =(Button)this.findViewById(R.id.btn_add);
        btn_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setVibrate();

                m_server_ip = mServer_ip.getText().toString();
                if(m_server_ip.equals("")) {
                    MessageBox(R.string.server_ip_empty);
                    return;
                }

                String apSsid = mSSID.getText().toString();
                String apPassword = mPassword.getText().toString();
                String apBssid = mWifiAdmin.getWifiConnectedBssid();

                if (__IEsptouchTask.DEBUG) {
                    Log.d(TAG, "mBtnConfirm is clicked, mEdtApSsid = " + apSsid
                            + ", " + " mEdtApPassword = " + apPassword);
                }

                SmartConfigTask sct = new SmartConfigTask();
                sct.m_sp = SettingPage.this;
                sct.execute(apSsid, apBssid, apPassword, "1");
            }
        });



        btn_scan =(Button)this.findViewById(R.id.btn_scan);
        btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setVibrate();
                Intent intent = new Intent(SettingPage.this, CaptureActivity.class);
                startActivityForResult(intent, 1);
            }
        });



        sw_ssh =(Switch) this.findViewById(R.id.sw_ssh);
        sw_ssh.setChecked(m_ssh);
        sw_ssh.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                m_ssh = isChecked;
                SharedPreferences.Editor editor =
                        PreferenceManager.getDefaultSharedPreferences(SettingPage.this).edit();
                editor.putBoolean("ssh", m_ssh);
                editor.commit();
            }
        });



        sw_safety =(Switch) this.findViewById(R.id.sw_safety);
        sw_safety.setChecked(safety);
        sw_safety.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor =
                        PreferenceManager.getDefaultSharedPreferences(SettingPage.this).edit();
                editor.putBoolean("safety", isChecked);
                editor.commit();
            }
        });



    }

    void run_ota_server(final OTAParam param)
    {

        progress_box_message(R.string.check_firmware_package);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                m_ota = new OTAServer();
                try {

                    m_ota.m_sp = SettingPage.this;
                    m_ota.run(param);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                m_ota = null;
            }
        });
        t.start();
    }


    // 讀取所有記事資料
    public boolean FindDevice(String device_id) {

        SQLiteDatabase db = DBHelper.getDatabase(this);

        if(db == null)
            return false;

        String sql = "select * from devices where DEVICE_ID='" + device_id+"'";


        Cursor cursor = db.rawQuery(sql, null);

        while (cursor.moveToNext()) {
            return true;
        }

        return false;
    }



    void AddDeviceDB(String manager_id, String device_name, String device_id, String server_ip, String camera_ip, int securite)
    {
        Resources r = this.getResources();
        SQLiteDatabase db = DBHelper.getDatabase(this);
        ContentValues values = new ContentValues();
        values.put("MANAGER_ID", manager_id);
        values.put("DEVICE_NAME", device_name);
        values.put("DEVICE_ID", device_id);
        values.put("SERVER_IP", server_ip);
        values.put("CAMERA_IP", camera_ip);
        values.put("BTN1", r.getString(R.string.up));
        values.put("BTN2", r.getString(R.string.stop));
        values.put("BTN3", r.getString(R.string.down));
        values.put("BTN4", r.getString(R.string.lock));
        values.put("SECURITY", securite);
        db.insert("devices", null, values);
    }


    void UpdateDeviceDB(String manager_id, String device_name, String device_id, String server_ip, int securite)
    {
        SQLiteDatabase db = DBHelper.getDatabase(this);
        ContentValues cv = new ContentValues();

        cv.put("MANAGER_ID", manager_id);
        cv.put("DEVICE_NAME", device_name);
        cv.put("SERVER_IP", server_ip);
        cv.put("SECURITY", securite);

        String where = "DEVICE_ID ='" + device_id+"'";

        // 執行修改資料並回傳修改的資料數量是否成功
        db.update("devices", cv, where, null);
    }

    void UpdateDeviceDB(String manager_id, String device_name, String device_id, String server_ip, String camera_ip, int securite)
    {
        SQLiteDatabase db = DBHelper.getDatabase(this);
        ContentValues cv = new ContentValues();

        cv.put("MANAGER_ID", manager_id);
        cv.put("DEVICE_NAME", device_name);
        cv.put("SERVER_IP", server_ip);
        cv.put("CAMERA_IP", camera_ip);
        cv.put("SECURITY", securite);

        String where = "DEVICE_ID ='" + device_id+"'";

        // 執行修改資料並回傳修改的資料數量是否成功
        db.update("devices", cv, where, null);
    }

    void DeleteDeviceDB(String device_id)
    {
        SQLiteDatabase db = DBHelper.getDatabase(this);
        String where = "DEVICE_ID ='" + device_id+"'";
        db.delete("devices", where , null);
    }

    // OTAEvent
    @Override
    public void on_ota_notification(int hr) {
        progress_box_message(hr);
    }
    @Override
    public void on_ota_finish(boolean hr) {
        if(bOTA) {
            bOTA = false;
            m_ota.Shutdown();
            if(hr)
                progress_box_confirm(R.string.upgrade_success);
            else
                progress_box_confirm(R.string.upgrade_fail);
        }

    }
    @Override
    public void on_ota_start() {
        bOTA = true;
    }


    //  SCEvent
    @Override
    public void on_sc_new_user(String device_name, String device_id) {
        if(FindDevice(device_id) == false) {
            AddDeviceDB(mManager_ID, device_name, device_id, m_server_ip, "none", m_ssh == true ? 1 : 0);
        } else {
            UpdateDeviceDB(mManager_ID, device_name, device_id, m_server_ip, m_ssh == true ? 1 : 0);
        }
        setVibrate();
    }

}
