package tw.ironthomas.smartiot;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;


import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Button btn_setting;
    ListView list_devices;
    DeviceAdapter adapter;
    int m_screen_width;
    int m_screen_hight;


    public Bitmap CreateQRcode(String QRCodeContent, int w, int h)
    {
        int QRCodeWidth = w;
        int QRCodeHeight = h;

        Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        MultiFormatWriter writer = new MultiFormatWriter();
        try
        {
            // L(7%)，M(15%)，Q(25%)，H(30%)
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

            BitMatrix result = writer.encode(QRCodeContent, BarcodeFormat.QR_CODE, QRCodeWidth, QRCodeHeight, hints);

            Bitmap bitmap = Bitmap.createBitmap(QRCodeWidth, QRCodeHeight, Bitmap.Config.ARGB_8888);

            for (int y = 0; y<QRCodeHeight; y++)
            {
                for (int x = 0;x<QRCodeWidth; x++)
                {
                    bitmap.setPixel(x, y, result.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            //SaveQRcode(bitmap);
            return bitmap;
        }
        catch (WriterException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;

    }

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


    @Override
    protected void onResume() {
        super.onResume();
        RefreshDevice();
    }

    public void onClick(View v){
        setVibrate();
        if(v.getId() == R.id.menu) {
            final DeviceItem it = (DeviceItem)v.getTag();
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(it.device_name)
                    .setPositiveButton(R.string.share, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setVibrate();

                            AlertDialog.Builder alertadd = new AlertDialog.Builder(MainActivity.this);
                            LayoutInflater factory = LayoutInflater.from(MainActivity.this);
                            final View view = factory.inflate(R.layout.barcode, null);
                            ImageView iv = (ImageView)view.findViewById(R.id.dialog_imageview);

                            int qs;
                            if(m_screen_width <  m_screen_hight)
                                qs = (m_screen_width * 6)/10;
                            else
                                qs = (m_screen_hight * 6)/10;

                            String str64 = new String(Base64.encode(it.device_name.getBytes(), Base64.NO_WRAP));
                            String camera_ip;
                            if(it.camera_ip == null || it.camera_ip.equals("")) {
                                camera_ip = " camera_ip:none";
                            } else {
                                camera_ip = " camera_ip:"+it.camera_ip;
                            }


                            if(it.security == 1) {
                                iv.setImageBitmap(CreateQRcode("**smart_rc_device_personal** device_id:" + it.device_id + " device_name:"+ str64 + " manager_id:"+it.manager_id + " server_s_ip:"+it.server_ip + camera_ip, qs, qs));
                            } else {
                                iv.setImageBitmap(CreateQRcode("**smart_rc_device_personal** device_id:" + it.device_id + " device_name:"+ str64 + " manager_id:"+it.manager_id + " server_ip:"+it.server_ip, qs, qs));
                            }


                            alertadd.setView(view);

                            alertadd.show();

                        }
                    })
                    .setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setVibrate();
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle(R.string.delete_device)
                                    .setMessage(R.string.confirm_delete)
                                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            DeleteDeviceDB(it.device_id);
                                            RefreshDevice();
                                        }
                                    })
                                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    })
                                    .show();
                        }
                    })
                    .show();



        }
        else if(v.getId() == R.id.item) {
            DeviceItem it = (DeviceItem)v.getTag();
            Intent intent = new Intent(this, DevicePage.class);
            intent.putExtra("device_name", it.device_name);
            intent.putExtra("device_id", it.device_id);
            intent.putExtra("manager_id", it.manager_id);
            intent.putExtra("server_ip", it.server_ip);
            intent.putExtra("camera_ip", it.camera_ip);

            intent.putExtra("btn1", it.btn1);
            intent.putExtra("btn2", it.btn2);
            intent.putExtra("btn3", it.btn3);
            intent.putExtra("btn4", it.btn4);
            intent.putExtra("security", it.security);


            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setVibrate();
                Intent intent = new Intent(MainActivity.this, SettingPage.class);
                startActivityForResult(intent, 1);
            }
        });


        GetScreenSize();


        list_devices = (ListView) findViewById(R.id.devices);
        adapter = new DeviceAdapter(this, this);
        list_devices.setAdapter(adapter);
        //RefreshDevice();

    }

    class Item
    {
        public String manager_id;
        public String device_name;
        public String device_id;
        public String server_ip;
        public String camera_ip;

        public String btn1;
        public String btn2;
        public String btn3;
        public String btn4;

        public int security;
    }

    // 把Cursor目前的資料包裝為物件
    public Item getRecord(Cursor cursor) {
        // 準備回傳結果用的物件
        Item result = new Item();

        result.manager_id = cursor.getString(1);
        result.device_name = cursor.getString(2);
        result.device_id = cursor.getString(3);
        result.server_ip = cursor.getString(4);
        result.camera_ip = cursor.getString(5);

        result.btn1 = cursor.getString(6);
        result.btn2 = cursor.getString(7);
        result.btn3 = cursor.getString(8);
        result.btn4 = cursor.getString(9);

        result.security = cursor.getInt(10);



        // 回傳結果
        return result;
    }


    public List<Item> getAll() {

        SQLiteDatabase db = DBHelper.getDatabase(this);
        List<Item> result = new ArrayList<>();

        Cursor cursor = db.rawQuery("select * from devices", null);

        while (cursor.moveToNext()) {
            result.add(getRecord(cursor));
        }

        cursor.close();
        return result;
    }

    void DeleteDeviceDB(String device_id)
    {
        SQLiteDatabase db = DBHelper.getDatabase(this);
        String where = "DEVICE_ID ='" + device_id+"'";
        db.delete("devices", where , null);
    }


    void RefreshDevice()
    {
        adapter.clear();
        List<Item> devices = getAll();
        for (Item device : devices) {

            adapter.add(device.manager_id
                    , device.device_id
                    , device.device_name
                    , device.server_ip
                    , device.camera_ip
                    , device.btn1
                    , device.btn2
                    , device.btn3
                    , device.btn4
                    , device.security
            );
        }
    }
}
