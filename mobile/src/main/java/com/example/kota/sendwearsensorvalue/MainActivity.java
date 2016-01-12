package com.example.kota.sendwearsensorvalue;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
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

import java.util.Collection;
import java.util.HashSet;
import java.util.StringTokenizer;

//branchTestPush

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener {

    private CSV csv = new CSV();

    public TextView receivedMsg;
    public DrawableView view;

    private static final String TAG = "mobileActivity";

    //onMessageReceivedのパス
    private static final String KEY_IN_RESOLUTION = "is_in_resolution";
    public static final String START_ACTIVITY_PATH = "/start/MainActivity";
    public static final String CONNECT_FITNESS = "/connect/fitness";
    public static final String STOP_SENSORS = "/stop/sensors";
    public static final String CSV_WRITE = "/csv/write";

    private boolean sendMsg = true; //true = wearとセンサ値のやり取りができる , false = wearとのやり取りを中断する
    private boolean communicating = false; //時計側のセンサと通信をしているか
    private String totalCsvWrite = ""; //csvに書き込む文字列
    private String graphValue = "";

    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1;
    /**
     * Google API client.
     */
    private GoogleApiClient mGoogleApiClient;
    /**
     * Determines if the client is in a resolution state, and
     * waiting for resolution intent to return.
     */
    private boolean mWearIsInResolution;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mWearIsInResolution = savedInstanceState.getBoolean(KEY_IN_RESOLUTION, false);
        }

        setContentView(R.layout.activity_main);

        receivedMsg = (TextView) findViewById(R.id.recievedMsg);

        view = (DrawableView) findViewById(R.id.graph);

        Button stopSensors = (Button) findViewById(R.id.msgButton);
        stopSensors.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (communicating) {
                    sendMsg = false;
                    new StartWearableTask().execute();
                } else {
                    showToast("通信をしていません");
                }
            }
        });
        Button startSensors = (Button) findViewById(R.id.startSensors);
        startSensors.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!communicating) {
                    sendMsg = true;
                    new StartWearableTask().execute();
                } else {
                    showToast("通信中です");
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*****
     * 時計との送受信
     *****/

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(CONNECT_FITNESS)) { //通信
            //Log.i("mobile", "onMessageReceived");
            if (!communicating) communicating = true;
            final String message = new String(messageEvent.getData());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    receivedMsg.setText(message);
                    totalCsvWrite += message;
                    graphValue += totalCsvWrite;

                    String[] graph = graphValue.split("\n", 0);
                    StringTokenizer sensorValue;
                    for (int i = 0; i < graph.length - 1; i++) {
                        sensorValue = new StringTokenizer(graph[i], ",");
                        int j = 0;
                        String[] data = new String[8];
                        while (sensorValue.hasMoreTokens()) {
                            data[j] = sensorValue.nextToken();
                            j++;
                        }
                        if (data[0] != null && data[1] != null && data[2] != null && data[3] != null && data[4] != null
                                && data[5] != null && data[6] != null && data[7] != null) {
                            view.setSensorValue(data[0],
                                    Float.valueOf(data[1]),
                                    Float.valueOf(data[2]),
                                    Float.valueOf(data[3]),
                                    Float.valueOf(data[7]));
                        }
                    }
                    graphValue = "";
                    //}
                    new StartWearableTask().execute();
                }
            });
        } else if (messageEvent.getPath().equals(CSV_WRITE)) { //通信終了時
            Log.i("mobile", "CSV_WRITE");
            if (communicating) communicating = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    csv.totalWrite(totalCsvWrite);
                    Toast.makeText(MainActivity.this, "completeTotalCsvWrite", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }
        return results;
    }

    private class StartWearableTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            Collection<String> nodes = getNodes();
            for (String n : nodes) {
                sendStartActivityMessage(n);
            }
            return null;
        }
    }

    private void sendStartActivityMessage(String nodeId) {
        if (sendMsg) {
            Wearable.MessageApi.sendMessage(//通信開始の合図
                    mGoogleApiClient, nodeId, START_ACTIVITY_PATH, new byte[0]).setResultCallback(
                    new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            if (!sendMessageResult.getStatus().isSuccess()) {
                                Log.e(TAG, "Failed to send msg with status code: "
                                        + sendMessageResult.getStatus().getStatusCode());
                            }
                        }
                    }
            );
        } else {
            Wearable.MessageApi.sendMessage(//通信終了の合図
                    mGoogleApiClient, nodeId, STOP_SENSORS, new byte[0]).setResultCallback(
                    new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            if (!sendMessageResult.getStatus().isSuccess()) {
                                Log.e(TAG, "Failed to send msg with status code: "
                                        + sendMessageResult.getStatus().getStatusCode());
                            }
                        }
                    }
            );
        }

    }

    /*****
     * START_GoogleAPIに接続する
     *****/

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "GoogleApiClient connected");
        // TODO: Start making API requests.
        Wearable.MessageApi.addListener(mGoogleApiClient, this);

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient connection suspended");
        retryConnecting();

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "GoogleApiClient connection failed: " + connectionResult.toString());
        if (!connectionResult.hasResolution()) {
            // Show a localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(
                    connectionResult.getErrorCode(), this, 0, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            retryConnecting();
                        }
                    }).show();
            return;
        }
        // If there is an existing resolution error being displayed or a resolution
        // activity has started before, do nothing and wait for resolution
        // progress to be completed.
        if (mWearIsInResolution) {
            return;
        }
        mWearIsInResolution = true;
        try {
            connectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
            retryConnecting();
        }

    }

    /**
     * Called when the Activity is made visible.
     * A connection to Play Services need to be initiated as
     * soon as the activity is visible. Registers {@code ConnectionCallbacks}
     * and {@code OnConnectionFailedListener} on the
     * activities itself.
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    /**
     * Called when activity gets invisible. Connection to Play Services needs to
     * be disconnected as soon as an activity is invisible.
     */
    @Override
    protected void onStop() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        }
        super.onStop();
    }

    /**
     * Saves the resolution state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IN_RESOLUTION, mWearIsInResolution);
    }

    /**
     * Handles Google Play Services resolution callbacks.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_RESOLUTION:
                //retryConnecting();
                mWearIsInResolution = false;
                if (resultCode == RESULT_OK) {
                    // Make sure the app is not already connected or attempting to connect
                    if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                        mGoogleApiClient.connect();
                    }
                }
                break;
        }
    }

    public void retryConnecting() {
        mWearIsInResolution = false;
        if (!mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }

    }

    /*****
     * END_GoogleAPIに接続する
     *****/

    private void showToast(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
