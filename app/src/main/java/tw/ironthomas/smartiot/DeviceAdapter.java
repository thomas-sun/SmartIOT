package tw.ironthomas.smartiot;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import tw.ironthomas.smartiot.R;

import java.util.ArrayList;
import java.util.List;



public class DeviceAdapter extends ArrayAdapter<DeviceItem>
{
    private LayoutInflater mLayInf;
    private List<DeviceItem> items;
    View.OnClickListener onclick;



    public DeviceAdapter(Context context, View.OnClickListener onclick)
    {
        super(context, R.layout.devices);

        mLayInf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //this.context = context;
        items = new ArrayList<DeviceItem>();
        this.onclick = onclick;

    }


    @Override
    public int getCount()
    {
        return items.size();
    }

    @Override
    public DeviceItem getItem(int position)
    {
        return items.get(position);
    }

    public void add(String manager_id
            , String device_id
            , String device_name
            , String server_ip
            , String camera_ip
            , String btn1
            , String btn2
            , String btn3
            , String btn4
            , int security
    )
    {
        DeviceItem it = new DeviceItem();
        it.manager_id = manager_id;
        it.device_id = device_id;
        it.device_name = device_name;
        it.server_ip = server_ip;
        it.camera_ip = camera_ip;

        it.btn1 = btn1;
        it.btn2 = btn2;
        it.btn3 = btn3;
        it.btn4 = btn4;
        it.security = security;

        items.add(it);
        notifyDataSetChanged();
    }


    public void clear()
    {
        items.clear();
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        if (convertView == null)
            convertView = mLayInf.inflate(R.layout.devices, parent, false);


        LinearLayout layout = (LinearLayout) convertView.findViewById(R.id.item);
        layout.setOnClickListener(onclick);
        layout.setTag(items.get(position));

        TextView txtView = (TextView) convertView.findViewById(R.id.id);

        ImageView img = (ImageView) convertView.findViewById(R.id.img);
        img.setImageResource(R.mipmap.rc);

        Button btn_menu = (Button) convertView.findViewById(R.id.menu);


        btn_menu.setOnClickListener(onclick);
        btn_menu.setTag(items.get(position));


        txtView.setText(items.get(position).device_name);

        return convertView;
    }
}