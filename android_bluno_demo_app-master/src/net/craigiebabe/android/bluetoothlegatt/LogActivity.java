package net.craigiebabe.android.bluetoothlegatt;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.content.ContentValues.TAG;

public class LogActivity extends Activity {
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private LineChart chart[] = new LineChart[3];

    private Button btns[] = new Button[3];
    private String mDataTemp;
    private String mDeviceAddress;

    private BluetoothLeService mBluetoothLeService;
    private ArrayList Log_Data[] = new ArrayList[3];
    private String index_str[] = {"temp","humid","ph"};

    private void saveFile(){
        File saveFile = null;
        if( Build.VERSION.SDK_INT < 29) saveFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/lolSaveFiles");
        else saveFile = LogActivity.this.getExternalFilesDir("/lolSaveFiles");

        if(!saveFile.exists())
            saveFile.mkdir();
        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(saveFile+"/savedData.txt", false));
            for(int j = 0 ; j < 3; j++) {
                for (int i = 0; i < Log_Data[j].size(); i++) {
                    if (i == 0) {
                        buf.append(index_str[j] + ",");
                        Log.e(TAG, index_str[j]);
                    }
                    buf.append(Double.toString((double) Log_Data[j].get(i)));
                    if (i != Log_Data[j].size() - 1)
                        buf.append(",");
                }
                buf.newLine();
            }
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadFile(){
        String readLine = null;
        File saveFile = null;

        if( Build.VERSION.SDK_INT < 29) saveFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/lolSaveFiles");
        else saveFile = LogActivity.this.getExternalFilesDir("/lolSaveFiles");
        if(saveFile == null)
            saveFile.mkdir();
        try {
            BufferedReader buf = new BufferedReader(new FileReader(saveFile+"/savedData.txt"));
            while ((readLine = buf.readLine()) != null) {
                String[] Data = readLine.split(",");
                if(Data.length > 0) {
                    if (Data[0].compareTo(index_str[0]) == 0) {
                        for (int i = 1; i < Data.length; i++) {
                            Log_Data[0].add(Double.parseDouble(Data[i]));
                            addEntry(Double.parseDouble(Data[i]), 0);
                        }
                    } else if (Data[0].compareTo(index_str[1]) == 0) {
                        for (int i = 1; i < Data.length; i++) {
                            Log_Data[1].add(Double.parseDouble(Data[i]));
                            addEntry(Double.parseDouble(Data[i]), 1);
                        }
                    } else if (Data[0].compareTo(index_str[2]) == 0) {
                        for (int i = 1; i < Data.length; i++) {
                            Log_Data[2].add(Double.parseDouble(Data[i]));
                            addEntry(Double.parseDouble(Data[i]), 2);
                        }
                    }
                }
            }
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void addEntry(double num, int index) {

        LineData data = chart[index].getData();

        if (data == null) {
            data = new LineData();
            chart[index].setData(data);
        }

        ILineDataSet set = data.getDataSetByIndex(0);
        // set.addEntry(...); // can be called as well

        if (set == null) {
            set = createSet();
            data.addDataSet(set);
        }



        data.addEntry(new Entry((float)set.getEntryCount(), (float)num), 0);
        data.notifyDataChanged();

        // let the chart know it's data has changed
        chart[index].notifyDataSetChanged();

        chart[index].setVisibleXRangeMaximum(150);
        // this automatically refreshes the chart (calls invalidate())
        chart[index].moveViewTo(data.getEntryCount(), 50f, YAxis.AxisDependency.LEFT);

    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "Real-time Line Data");
        set.setLineWidth(1f);
        set.setDrawValues(false);
        set.setValueTextColor(Color.WHITE);
        set.setColor(Color.WHITE);
        set.setMode(LineDataSet.Mode.LINEAR);
        set.setDrawCircles(false);
        set.setHighLightColor(Color.rgb(190, 190, 190));

        return set;
    }
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                finish();
            }
            // Automatically connects to the device upon successful start-up initialisation.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
        }
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CHARACTERISTIC_WRITE_COMPLETE);
        return intentFilter;
    }
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                // disconnect
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayCharacteristicData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private void displayCharacteristicData(String data) {
        if (data != null) {
            if(mDataTemp != null){
                mDataTemp += data;
            }
            else{
                mDataTemp = data;
            }

            if(mDataTemp.endsWith("\n") == true) {
                //display here!
                String[] Data = mDataTemp.split("#");
                if(Data.length == 6) {
                    addEntry(Double.parseDouble(Data[0]), 0);
                    addEntry(Double.parseDouble(Data[1]), 1);
                    addEntry(Double.parseDouble(Data[2].replace("\n","")), 2);
                }
                mDataTemp = null;
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
        saveFile();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        Log_Data[0] = new ArrayList();
        Log_Data[1] = new ArrayList();
        Log_Data[2] = new ArrayList();

        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        chart[0] = (LineChart) findViewById(R.id.chart);
        chart[1] = (LineChart) findViewById(R.id.chart2);
        chart[2] = (LineChart) findViewById(R.id.chart3);

        btns[0] = (Button) findViewById(R.id.btn1);
        btns[1] = (Button) findViewById(R.id.btn2);
        btns[2] = (Button) findViewById(R.id.btn3);

        for(int i = 0 ; i < 3; i++){
            chart[i].setDrawGridBackground(true);
            chart[i].setBackgroundColor(Color.BLACK);
            chart[i].setGridBackgroundColor(Color.BLACK);

            // touch gestures (false-비활성화)
            chart[i].setTouchEnabled(false);

            // scaling and dragging (false-비활성화)
            chart[i].setDragEnabled(false);
            chart[i].setScaleEnabled(false);

            //auto scale
            chart[i].setAutoScaleMinMaxEnabled(true);

            // if disabled, scaling can be done on x- and y-axis separately
            chart[i].setPinchZoom(false);

            //X축
            chart[i].getXAxis().setDrawGridLines(true);
            chart[i].getXAxis().setDrawAxisLine(false);
            chart[i].getXAxis().setEnabled(true);
            chart[i].getXAxis().setDrawGridLines(false);
            YAxis leftAxis[] = new YAxis[3];

            leftAxis[i] = chart[i].getAxisLeft();
            leftAxis[i].setEnabled(true);
            leftAxis[i].setTextColor(Color.BLACK);
            leftAxis[i].setDrawGridLines(true);
            leftAxis[i].setGridColor(Color.BLACK);

            YAxis rightAxis[] = new YAxis[3];

            rightAxis[i] = chart[i].getAxisRight();
            rightAxis[i].setEnabled(false);

            chart[i].invalidate();
        }

        btns[0].setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                Random random = new Random();
                double randomValue = random.nextDouble();
                Log_Data[0].add(randomValue);
                addEntry(randomValue,0);
            }
        });

        btns[1].setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                Random random = new Random();
                double randomValue = random.nextDouble();
                Log_Data[1].add(randomValue);
                addEntry(randomValue,1);
            }
        });

        btns[2].setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                Random random = new Random();
                double randomValue = random.nextDouble();
                Log_Data[2].add(randomValue);
                addEntry(randomValue,2);
            }
        });

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        loadFile();
    }
}
