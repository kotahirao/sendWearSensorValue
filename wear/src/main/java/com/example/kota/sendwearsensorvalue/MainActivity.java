package com.example.kota.sendwearsensorvalue;

import android.app.Activity;
import android.content.IntentSender;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener,SensorEventListener {

    private TextView mTextNowTime;
    private TextView mTextHeart;
    private TextView mTextAccelex;
    private TextView mTextAcceley;
    private TextView mTextAccelez;
    private TextView mTextGyrox;
    private TextView mTextGyroy;
    private TextView mTextGyroz;
    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private SensorManager manager;
    private boolean mRegisteredSensor = false; //センサリスナーの登録の有無

    //API Variables
    private GoogleApiClient mClient;
    public static final String CONNECT_FITNESS = "/connect/fitness";
    public static final String START_ACTIVITY_PATH = "/start/MainActivity";
    public static final String STOP_SENSORS = "/stop/sensors";
    public static final String CSV_WRITE = "/csv/write";
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private boolean mResolvingError = false;

    private boolean canSendMsg = true; //handheldにセンサ値を送れるか否か
    private boolean write_csv = false; //通信終了時にhandheld側でcsvを書いてもらう
    private boolean startSensors = false;
    private String _msg = ""; //メッセージバッファ
    private String msg = ""; //実際送るメッセージ
    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(Wearable.API)
                .addOnConnectionFailedListener(this)
                .build();

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextNowTime = (TextView) stub.findViewById(R.id.nowTime);
                mTextHeart = (TextView) stub.findViewById(R.id.bpm);
                mTextAccelex = (TextView) stub.findViewById(R.id.accele_x);
                mTextAcceley = (TextView) stub.findViewById(R.id.accele_y);
                mTextAccelez = (TextView) stub.findViewById(R.id.accele_z);
            }
        });
        manager = (SensorManager)getSystemService(SENSOR_SERVICE);
    }

    /*****センサー*****/

    @Override
    public void onSensorChanged(SensorEvent event) {

        if(!startSensors)return; //handheld側からのスタート合図待ち

        if (mTextNowTime != null &&
                mTextAccelex != null &&
                mTextAcceley != null &&
                mTextAccelez != null &&
                mTextHeart != null &&
                mTextGyrox != null &&
                mTextGyroy != null &&
                mTextGyroz != null) {

            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    mTextAccelex.setText(String.valueOf(event.values[0]));
                    mTextAcceley.setText(String.valueOf(event.values[1]));
                    mTextAccelez.setText(String.valueOf(event.values[2]));
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    mTextGyrox.setText(String.valueOf(event.values[0]));
                    mTextGyroy.setText(String.valueOf(event.values[1]));
                    mTextGyroz.setText(String.valueOf(event.values[2]));
                    break;
                case Sensor.TYPE_HEART_RATE:
                    mTextHeart.setText(String.valueOf(event.values[0]));
                    break;
            }

            long currentTimeMillis = System.currentTimeMillis();
            Date date = new Date(currentTimeMillis);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss:SSS");

            if (mTextNowTime != null) mTextNowTime.setText(simpleDateFormat.format(date));

            String newMsg = simpleDateFormat.format(date) + ","
                    + mTextAccelex.getText() + ","
                    + mTextAcceley.getText() + ","
                    + mTextAccelez.getText() + ","
                    + mTextGyrox.getText() + ","
                    + mTextGyroy.getText() + ","
                    + mTextGyroz.getText() + ","
                    + mTextHeart.getText() + "\n";
            _msg += newMsg;
            count++;
            Log.i("count", String.valueOf(count));
            if (canSendMsg) {
                msg = _msg;
                new ConnectFitnessTask().execute();
                _msg = "";
                canSendMsg = false;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onPause(){
        super.onPause();
        unRegistSensor();
    }
    public void unRegistSensor(){
        if (mRegisteredSensor) {
            manager.unregisterListener(this);
            mRegisteredSensor = false;
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        setSensors();
    }
    public void setSensors(){
        List<Sensor> sensorsList = manager.getSensorList(Sensor.TYPE_ALL);
        String str = "実装されているセンサー一覧:\n";
        for(Sensor s : sensorsList) {
            str += s.getName() + "\n";
        }
        Log.i("sensors", str);

        ArrayList<List<Sensor>> sensors = new ArrayList<>();
        sensors.add(manager.getSensorList(Sensor.TYPE_ACCELEROMETER));
        sensors.add(manager.getSensorList(Sensor.TYPE_GYROSCOPE));
        sensors.add(manager.getSensorList(Sensor.TYPE_HEART_RATE));

        for(List<Sensor> sensor : sensors){
            if(sensor.size()>0){
                mRegisteredSensor = manager.registerListener(this,
                        sensor.get(0),
                        SensorManager.SENSOR_DELAY_GAME);
//                         SensorManager.SENSOR_DELAY_GAME);

            }
        }
        mRegisteredSensor = true;
    }


    /*****時計との送受信*****/


    //handheldからメッセージが来た時の処理
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(START_ACTIVITY_PATH)){
            Log.i("wear","onMessageReceived");
            if(count == 0)startSensors = true;
            if(!mRegisteredSensor){
                setSensors();
            }
            canSendMsg = true;

        }else if(messageEvent.getPath().equals(STOP_SENSORS)){ // センサを止める
                Log.i("wear", "sensor unRegistered");
                showToast("計測終了");
                canSendMsg = true;
                write_csv = true;
                msg = _msg;
                count = 0;
                new ConnectFitnessTask().execute();
                _msg = "";
                canSendMsg = false;
                unRegistSensor();

        }

    }
    //handheld側にメッセージを送る
    private void sendHandheldFitnessPrompt (String nodeId){
        if(!write_csv) {
            Wearable.MessageApi.sendMessage(mClient, nodeId, CONNECT_FITNESS, msg.getBytes()).setResultCallback(
                    new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            if (!sendMessageResult.getStatus().isSuccess()) {
                                Log.e(LOG_TAG, "Failed to send msg, status code: " +
                                        sendMessageResult.getStatus().getStatusCode());
                            }
                        }
                    }
            );
        }else{
            write_csv = false;
            Wearable.MessageApi.sendMessage(mClient,nodeId,CSV_WRITE,msg.getBytes()).setResultCallback(
                    new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            if (!sendMessageResult.getStatus().isSuccess()) {
                                Log.e(LOG_TAG, "Failed to send msg, status code: " +
                                        sendMessageResult.getStatus().getStatusCode());
                            }
                        }
                    }
            );
        }
    }

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }
        return results;
    }

    // Send Prompt to Handheld to start FitnessAPI
    private class ConnectFitnessTask extends AsyncTask<Void,Void,Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Collection<String> nodes = getNodes();
            for (String n : nodes){
                sendHandheldFitnessPrompt(n);
            }
            return null;
        }
    }








    /*****START GoogleAPIに接続する*****/

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError){
            mClient.connect();
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        Wearable.MessageApi.removeListener(mClient, this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(LOG_TAG, "onConnected");
        //Can now use WearableAPI

        //setupMessageListeners
        Wearable.MessageApi.addListener(mClient, this);

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOG_TAG, "Connection suspended");

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(LOG_TAG, "onConnectionFailed");

        if (mResolvingError){
            //currently resolving an error
            return;
        }
        else if (connectionResult.hasResolution()){
            try {
                mResolvingError = true;
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            }
            catch (IntentSender.SendIntentException e){
                //Error with resolution intent. Try again
                mClient.connect();
            }
        }
        else{
            //no resolution
            //display Error dialog
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(),
                    this, REQUEST_RESOLVE_ERROR);
        }

    }

    /*****END GoogleAPIに接続する*****/

    private void showToast(final String str){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
