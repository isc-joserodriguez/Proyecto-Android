package com.example.shepe.controlbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.Set;
import java.util.UUID;



public class MainActivity extends AppCompatActivity {
    // GUI Components
    private Handler manGraphs;
    private Runnable mTimer1;
    private LineGraphSeries<DataPoint> mSeries1;
    private Runnable mTimer2;
    private LineGraphSeries<DataPoint> mSeries2;
    private double graph2LastXValue1;
    private double graph2LastXValue2;


    private TextView mBluetoothStatus;
    private EditText mReadBuffer;
    private Button btnBlue;
    private Button btnOnOff;
    private Button btnGraphs;
    private Button mScanBtn;
    private Button mOffBtn;
    private Button mListPairedDevicesBtn;
    private Button mDiscoverBtn;
    private Button enviar;
    private BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private ArrayAdapter<String> mBTArrayAdapter;
    private ListView mDevicesListView;
    private EditText temperatura;
    private EditText humedad;
    private LinearLayout panelBluetooth;
    private LinearLayout panelGraphs;
    private LinearLayout contenedor;
    private LinearLayout botones;
    private Boolean encendido;
    private double s1;
    private double s2;


    private Handler mHandler; // Our main handler that will receive callback notifications
    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier


    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothStatus = findViewById(R.id.bluetoothStatus);
        mReadBuffer = findViewById(R.id.readBuffer);
        mScanBtn = findViewById(R.id.scan);
        mOffBtn = findViewById(R.id.off);
        mDiscoverBtn = findViewById(R.id.discover);
        mListPairedDevicesBtn = findViewById(R.id.PairedBtn);
        btnBlue = findViewById(R.id.btnBlue);
        btnOnOff = findViewById(R.id.btnOnOff);
        btnGraphs = findViewById(R.id.btnGraphs);
        enviar = findViewById(R.id.enviar);
        temperatura = findViewById(R.id.temperatura);
        humedad= findViewById(R.id.humedad);
        panelBluetooth=findViewById(R.id.PanelBluetooth);
        panelGraphs=findViewById(R.id.PanelGraphs);
        contenedor=findViewById(R.id.Contenedor);
        botones=findViewById(R.id.Botones);
        panelBluetooth.setVisibility(View.VISIBLE);
        panelBluetooth.setVisibility(View.INVISIBLE);
        panelGraphs.setVisibility(View.INVISIBLE);
        manGraphs = new Handler();
        graph2LastXValue1=1;
        graph2LastXValue2=1;
        s1=0.0;
        s2=0.0;

        GraphView graph1 = findViewById(R.id.graph1);
        mSeries1 = new LineGraphSeries<>();
        graph1.addSeries(mSeries1);
        graph1.getViewport().setXAxisBoundsManual(true);
        graph1.getViewport().setMinX(0);
        graph1.getViewport().setMaxX(graph2LastXValue1+10);
        graph1.getViewport().setMinY(0);
        graph1.getViewport().setMaxY(s1+10.0);
        graph1.setTitle("Temperatura");



        GraphView graph2 = findViewById(R.id.graph2);
        mSeries2 = new LineGraphSeries<>();
        graph2.addSeries(mSeries2);
        graph2.getViewport().setXAxisBoundsManual(true);
        graph2.getViewport().setMinX(0);
        graph2.getViewport().setMaxX(graph2LastXValue2+10);
        graph2.setTitle("Humedad");


        encendido=false;


        mBTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

        mDevicesListView = findViewById(R.id.devicesListView);
        mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);





        mHandler = new Handler(){
            public void handleMessage(android.os.Message msg){
                if(msg.what == MESSAGE_READ){
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }



                    s1=Double.parseDouble(readMessage.split("-")[0].split(",")[0])/10;
                    s2=Double.parseDouble(readMessage.split("-")[0].split(",")[1])/10;
                    mReadBuffer.setText("Temperatura: "+s1+" Humedad: "+s2);

                }

                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1)
                        mBluetoothStatus.setText("Conectado a: " + (String)(msg.obj));
                    else
                        mBluetoothStatus.setText("Falló la conexión");
                }
            }
        };

        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            mBluetoothStatus.setText("Estado: Bluetooth no encontrado");
            Toast.makeText(getApplicationContext(),"Dispositivo bluetooth no encontrado!",Toast.LENGTH_SHORT).show();
        }
        else {
            enviar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!(temperatura.getText().toString().isEmpty() && humedad.getText().toString().isEmpty())){
                        String cad=encendido+","+temperatura.getText().toString()+","+humedad.getText().toString();
                        if(mConnectedThread != null && !cad.equals("")) //First check to make sure thread created
                            mConnectedThread.write(cad);
                    }else{
                        Toast.makeText(MainActivity.this,"Ingresa una temperatura y una humedad",Toast.LENGTH_LONG).show();
                    }
                }
            });
            btnOnOff.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!encendido&&mConnectedThread != null){
                        btnOnOff.setText("Apagar");
                        mConnectedThread.write("true,0,0");
                        encendido=!encendido;
                        mTimer1 = new Runnable() {
                            @Override
                            public void run() {
                                graph2LastXValue1 += 1;
                                mSeries1.appendData(new DataPoint(graph2LastXValue1,s1), true, (int)graph2LastXValue1+10);
                                mHandler.postDelayed(this, 1000);
                            }
                        };
                        manGraphs.postDelayed(mTimer1, 1000);

                        mTimer2 = new Runnable() {
                            @Override
                            public void run() {
                                graph2LastXValue2 += 1;
                                mSeries2.appendData(new DataPoint(graph2LastXValue2, s2), true, (int)graph2LastXValue2+10);
                                mHandler.postDelayed(this, 1000);
                            }
                        };
                        manGraphs.postDelayed(mTimer2, 1000);


                    }else{
                        recreate();
                        if(mConnectedThread != null) mConnectedThread.write("false,0,0");
                    }

                }

            });
            btnGraphs.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    panelGraphs.setVisibility(View.VISIBLE);
                    panelBluetooth.setVisibility(View.INVISIBLE);

                    contenedor.removeAllViews();
                    contenedor.addView(botones,0);
                    contenedor.addView(panelGraphs,1);
                    contenedor.addView(panelBluetooth,2);
                }
            });


            mScanBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothOn(v);
                }
            });

            mOffBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    bluetoothOff(v);
                }
            });

            mListPairedDevicesBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v){
                    listPairedDevices(v);
                }
            });

            mDiscoverBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    discover(v);
                }
            });
        }

        btnBlue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                panelBluetooth.setVisibility(View.VISIBLE);
                panelGraphs.setVisibility(View.INVISIBLE);
                contenedor.removeAllViews();
                contenedor.addView(botones,0);
                contenedor.addView(panelBluetooth,1);
                contenedor.addView(panelGraphs,2);
            }
        });
    }

    private void bluetoothOn(View view){
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothStatus.setText("Bluetooth Activado");
            Toast.makeText(getApplicationContext(),"Bluetooth encendido",Toast.LENGTH_SHORT).show();

        }
        else{
            Toast.makeText(getApplicationContext(),"Bluetooth ya esta encendido", Toast.LENGTH_SHORT).show();
        }
    }

    // Enter here after user selects "yes" or "no" to enabling radio
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data){
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                mBluetoothStatus.setText("Activado");
            }
            else
                mBluetoothStatus.setText("Desactivado");
        }
    }

    private void bluetoothOff(View view){
        mBTAdapter.disable(); // turn off
        mBluetoothStatus.setText("Bluetooth Desactivado");
        Toast.makeText(getApplicationContext(),"Bluetooth apagado", Toast.LENGTH_SHORT).show();
    }

    private void discover(View view){
        // Check if the device is already discovering
        if(mBTAdapter.isDiscovering()){
            mBTAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(),"Se detuvo el escaneo",Toast.LENGTH_SHORT).show();
        }
        else{
            if(mBTAdapter.isEnabled()) {
                mBTArrayAdapter.clear(); // clear items
                mBTAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Se inició el escaneo", Toast.LENGTH_SHORT).show();
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
            else{
                Toast.makeText(getApplicationContext(), "Bluetooth no está encendido", Toast.LENGTH_SHORT).show();
            }
        }
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    private void listPairedDevices(View view){
        mPairedDevices = mBTAdapter.getBondedDevices();
        if(mBTAdapter.isEnabled()) {
            // put it's one to the adapter
            for (BluetoothDevice device : mPairedDevices)
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

            Toast.makeText(getApplicationContext(), "Mostrar vinculados", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(getApplicationContext(), "Bluetooth no encendido", Toast.LENGTH_SHORT).show();
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            if(!mBTAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth no encendido", Toast.LENGTH_SHORT).show();
                return;
            }

            mBluetoothStatus.setText("Conectando...");
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0,info.length() - 17);

            // Spawn a new thread to avoid blocking the GUI one
            new Thread()
            {
                public void run() {
                    boolean fail = false;

                    BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

                    try {
                        mBTSocket = createBluetoothSocket(device);
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(getBaseContext(), "Falló la creación del socket", Toast.LENGTH_SHORT).show();
                    }
                    // Establish the Bluetooth socket connection.
                    try {
                        mBTSocket.connect();
                    } catch (IOException e) {
                        try {
                            fail = true;
                            mBTSocket.close();
                            mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                    .sendToTarget();
                        } catch (IOException e2) {
                            //insert code to deal with this
                            Toast.makeText(getBaseContext(), "Falló la creación del socket", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if(fail == false) {
                        mConnectedThread = new ConnectedThread(mBTSocket);
                        mConnectedThread.start();

                        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                                .sendToTarget();
                    }
                }
            }.start();
        }
    };

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connection with BT device using UUID
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.available();
                    if(bytes != 0) {
                        SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                        bytes = mmInStream.available(); // how many bytes are ready to be read?
                        bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget(); // Send the obtained bytes to the UI activity
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }
    }
}
