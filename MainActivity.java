package com.phenom.phenom_epilepsy;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.os.Handler;
import android.os.Message;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice hc05;
    TextView textView, data_cropped;
    EditText id_call, id_msg;
    String call_to, msg_to;
    String req_device = "H-C-2010-06-01";
    final String must_call = "PHENOM_CALL", must_msg = "PHENOM_MESSAGE";
    String writeMessage;
    Intent callIntent = new Intent(Intent.ACTION_CALL);
    Button set_details;
    ConnectThread mConnectThread;
    ConnectedThread mConnectedThread;
    BluetoothDevice mDevice = null;
    InputStream mmInStream;
    OutputStream mmOutStream;
    Context mContext;
    String msg;
    public double latitude, longitude;
    public GPSTracker gps;

    String to_start = "*";
    byte[] write_bytes_s = to_start.getBytes();

    public void bt_setup() {
       /* mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            Log.i("paired no:", String.valueOf(pairedDevices.size()));
            for (BluetoothDevice device : pairedDevices) {
                mDevice = device;
            }
        }
        mConnectThread= new ConnectThread(mDevice);

        mConnectThread.start();*/
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        hc05 = mBluetoothAdapter.getRemoteDevice("98:D3:61:F5:C3:1C");
        Log.i("hc05 name---", hc05.getName());
        mConnectThread = new ConnectThread(hc05);
        mConnectThread.start();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.recv);
        id_msg = (EditText) findViewById(R.id.id_num_msg);
        id_call = (EditText) findViewById(R.id.id_num_call);
        data_cropped = (TextView) findViewById(R.id.datacrop);
        set_details = (Button) findViewById(R.id.pick_contact);

        mContext = this;
       // gps=new GPSTracker(MainActivity.this);
       /* if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        } else {
            Toast.makeText(mContext,"You need have granted permission",Toast.LENGTH_SHORT).show();
            gps = new GPSTracker(MainActivity.this);
            if(gps.canGetLocation()) {

                latitude = gps.getLatitude();
                longitude = gps.getLongitude();
            }
            else {
                gps.showSettingsAlert();
            }
        }*/

        set_details.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                call_to = id_call.getText().toString();
                msg_to = id_msg.getText().toString();
                call_to="+91"+call_to;
                msg_to="+91"+msg_to;
                Toast toast = Toast.makeText(getApplicationContext(), "The contacts have been updated", Toast.LENGTH_SHORT);
                toast.show();
            }
        });
        bt_setup();
    }
    Handler mHandler = new Handler() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void handleMessage(Message msg) {
            byte[] writeBuf = (byte[]) msg.obj;
            int begin = (int)msg.arg1;
            int end = (int)msg.arg2;
            Log.i( "Handler","entered handler class");
            switch(msg.what) {
                case 1:
                    writeMessage = new String(writeBuf);
                    writeMessage = writeMessage.substring(begin, end);
                    Log.i( "Received",writeMessage );
                    textView.setText(writeMessage);
                    break;
            }
            if(writeMessage=="connectByte")
            {
                Log.i("CONNECTIVITY","connectbyte received");
                try {
                    mmOutStream.write(write_bytes_s);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(writeMessage.equals(must_call))
            {
                if(isPermissionGranted()){
                    call_action();
                }
                Log.i("Handler if:","Called");
                textView.setText("Called");
                try {
                    mmOutStream.write(write_bytes_s);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(writeMessage.equals(must_msg))
            {

                Log.i("Handler if:","Messaging");
                gps = new GPSTracker(MainActivity.this);
                if(gps.canGetLocation()) {
                    latitude = gps.getLatitude();
                    longitude = gps.getLongitude();
                    //Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
                    Log.i("MSG_BLOCK","Latitude="+latitude);
                    Log.i("MSG_BLOCK","Longitude="+longitude);
                    Phenom_Message();
                    Log.i("MUST_MSG_BLOCK","You entered the block again");
                    data_cropped.setText("Messaged");
                } else {
                    gps.showSettingsAlert();
                }
                try {
                    mmOutStream.write(write_bytes_s);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            }
    };
    public void call_action(){
        callIntent.setData(Uri.parse("tel:"+call_to));
        startActivity(callIntent);
        return;
    }
    public  boolean isPermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.CALL_PHONE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("TAG","Permission is granted");
                return true;
            } else {

                Log.v("TAG","Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v("TAG","Permission is granted");
            return true;
        }
    }

    public void Phenom_Message()
    {
        if(is_msg_permission_granted())
        {
            msg="Please help me! \n Iam down with Epilepsy \n My Coordinates are:\n "+latitude+","+longitude;
            Intent intent=new Intent(getApplicationContext(),MainActivity.class);
            PendingIntent pi= PendingIntent.getActivity(getApplicationContext(), 0, intent,0);
            SmsManager sms= SmsManager.getDefault();
            sms.sendTextMessage(msg_to, null, msg, pi,null);
            Log.i("Phenom_Block", msg);
            Log.i("Phenom_Block","Message Sent");
            Toast.makeText(getApplicationContext(), "Message Sent successfully!",
                    Toast.LENGTH_LONG).show();
        }
        else {
            Log.i("PHENOM_MSG_BLOCK","Msg permission is declined");
        }

    }

    public boolean is_msg_permission_granted()
    {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.SEND_SMS)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("TAG","Permission is granted");
                return true;
            } else {

                Log.v("TAG","Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v("TAG","Permission is granted");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode)
        {
            case 1: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Permission granted", Toast.LENGTH_SHORT).show();
                    call_action();
                } else {
                    Toast.makeText(getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private  final BluetoothDevice mmDevice;
        private   UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;
            Log.i("CT","IN connect thread block");
            if(mmDevice==null)
            {
                Log.i("ConnectThread=","NULL DEVICE");
            }
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.i("socket","exception in sockets");
            }
            mmSocket = tmp;
            if(mmSocket==null)
            {
                Log.i("ConnectThread","NullSocket");
            }
        }
        public void run() {
            mBluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
                Log.i("Connectthread block","connected -mmsocket");
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.i("Socket exception","mmsocket exception");
                }
                return;
            }
            mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();

        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        //private final InputStream mmInStream;
        // private final OutputStream mmOutStream;
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            write(write_bytes_s);
            Log.i("Connected thread","done buddy");
        }
        public void run() {
            byte[] buffer = new byte[1024];
            int begin = 0;
            int bytes = 0;
            while (true) {
                try {
                    bytes += mmInStream.read(buffer, bytes, buffer.length - bytes);
                    for (int i = begin; i < bytes; i++) {
                        if (buffer[i] == "#".getBytes()[0]) {
                            mHandler.obtainMessage(1, begin, i, buffer).sendToTarget();
                            begin = i + 1;
                            if (i == bytes - 1) {
                                bytes = 0;
                                begin = 0;
                            }
                        }
                    }
                }
                catch (IOException e) {
                     Log.i( "Received","exception in run loop" );
                    break;
                }
            }
        }
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}

