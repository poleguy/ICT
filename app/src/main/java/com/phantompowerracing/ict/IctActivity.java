package com.phantompowerracing.ict;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.Random;

//import java.util.ArrayList;
//http://stackoverflow.com/questions/18279302/how-do-i-perform-a-java-callback-between-classes
interface SpeedCallback {
    void setSpeed(double speed);
}
// Will this work?
// http://stackoverflow.com/questions/34918675/android-location-service-didnt-work-in-background
// or this?
// http://stackoverflow.com/questions/36761116/how-to-fused-location-run-in-background-service-continuously
// https://gist.github.com/blackcj/20efe2ac885c7297a676

//public class IctActivity extends AppCompatActivity implements
public class IctActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        SpeedCallback,
        LocationListener{

    private final AudioPlayer audioPlayer = new AudioPlayer("ICT_turkey.wav");

    Handler smoothHandler = new Handler();
    int delay = 100; // msec
    double smoothedRate = 0.0;

    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;

    //private ArrayList<String> arrayList;
    //private ClientListAdapter mAdapter;

    private Ict ict;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ict);
        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Log.d("onCreate","onCreate");
        super.onStart();

        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        turnOffScreen();
        // http://stackoverflow.com/questions/26982778/android-5-0-lollipop-and-4-4-kitkat-ignores-my-wifi-network-enablenetwork-is?rq=1

        //http://stackoverflow.com/questions/8818290/how-do-i-connect-to-a-specific-wi-fi-network-in-android-programmatically
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", "ESP_DCE4AC");
        //wifiConfig.SSID = String.format("\"%s\"", "244Wesley_708-848-3835");
        //wifiConfig.preSharedKey = String.format("\"%s\"", "dietz network key");

        WifiManager wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
//remember id
        int netId = wifiManager.addNetwork(wifiConfig);
        wifiManager.setWifiEnabled(true);
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();

        // set up car network
        //WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder request = new NetworkRequest.Builder();
        request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
                //.setNetworkSpecifier("00:1a:70:ee:69:b6"); // 244Wesley
                //.setNetworkSpecifier("001a70ee69b6"); // 244Wesley
                //.setNetworkSpecifier("244Wesley_708-848-3835"); // 244Wesley
                //.setNetworkSpecifier("1a:fe:34:dc:e4:ac"); // ESP_DCE4AC
        final NetworkRequest nr = request.build();
        //cm.registerNetworkCallback(nr, new ConnectivityManager.NetworkCallback() {
        cm.requestNetwork(nr, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {


                WifiManager wifiManager = (WifiManager) getSystemService (Context.WIFI_SERVICE);
                WifiInfo info = wifiManager.getConnectionInfo ();
                String ssid  = info.getSSID();
                Log.d("IctNetwork","onAvailable" + ssid);
                if (ssid == "ESP_DCE4AC") {
                    ConnectivityManager.setProcessDefaultNetwork(network);
                    //ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback();
                    //cm.requestNetwork(nr, networkCallback );
                }
            }
            @Override
            public void onLosing(Network network, int ms) {
                Log.d("IctNetwork","onLosing");
            }
            @Override
            public void onLost(Network network) {
                Log.d("IctNetwork","onLost");
            }
        });
        //        cm.requestNetwork(new NetworkRequest.Builder()
//                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
//                        .setNetworkSpecifier("00:1a:70:ee:69:b6") // 244Wesley
//                        //.setNetworkSpecifier("1a:fe:34:dc:e4:ac") // ESP_DCE4AC
//                        .build(),
//                new ConnectivityManager.NetworkCallback() {
//                    public void onAvailable(Network network) {
//                        Log.d("IctNetwork","onAvailable");
//                    }
//                });


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String ip_address = sharedPreferences.getString("car_ip_address", "192.168.1.74");
        int ip_port = Integer.parseInt(sharedPreferences.getString("car_ip_port", "23"));

        ict = new Ict(this, ip_address, ip_port);
        ict.register(this); // register callback

        // connect to the server
        ict.startPollingCar();

        // acquire a wake lock:
        // http://stackoverflow.com/questions/29743886/android-gps-location-in-service-off-if-device-sleep


        //in onCreate of your service
        //PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //cpuWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
        //        "gps_service");
        //cpuWakeLock.acquire();

		// https://developer.android.com/training/basics/firstapp/starting-activity.html#RespondToButton
		// start second activity to connect to car and get speed data
		//Intent intent = new Intent(this, ClientActivity.class);
        //intent.putExtra(EXTRA_MESSAGE, message);
        //startActivity(intent);


        audioPlayer.start(); // start thread, use .play() to actually play

        //FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        //FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        ImageButton imgButton =(ImageButton)findViewById(R.id.imageButton);
        if (imgButton != null) {
            imgButton.setOnClickListener(new View.OnClickListener() {
                //        fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                  //audioPlayerB("/sdcard","Carmen Ring.mp3");
//                  //audioPlayerB("/sdcard/Download","ICT_turkey.wav");
//                  //audioPlayerB("/data/local","tis.wav");
                    ImageButton imgButton =(ImageButton)findViewById(R.id.imageButton);
                    if(imgButton != null) {
                        if (audioPlayer.isPlaying()) {
                            audioPlayer.pause();
                            Log.d("click", "pause");
                            imgButton.setImageResource(R.drawable.mr_ic_play_light);
                        } else {
                            audioPlayer.play();
                            Log.d("click", "play");
                            imgButton.setImageResource(R.drawable.mr_ic_pause_light);
                        }
                    }
                }
            });
        }


        Button buttonUpload =(Button)findViewById(R.id.buttonUpload);
        //final Context context = this;
        if (buttonUpload != null) {
            buttonUpload.setOnClickListener(new View.OnClickListener() {
                //        fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Button button =(Button)findViewById(R.id.buttonUpload);
                    if(ict != null) {
                        Log.d("upload","about to upload");
                        ict.upload();
                    }
                }
            });
        }
        Button buttonClearLogs =(Button)findViewById(R.id.buttonClearLogs);
        if (buttonClearLogs != null) {
            buttonClearLogs.setOnClickListener(new View.OnClickListener() {
                //        fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Button button =(Button)findViewById(R.id.buttonUpload);
                    if(ict != null) {
                        Log.d("clear_logs","clearing logs");
                        ict.clearLogs();
                    }
                }
            });
        }

        // Enables app to handle 23+ (M+) style permissions.

        //if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        //        != PackageManager.PERMISSION_GRANTED) {
        // Check Permissions Now
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                    REQUEST_GPS_PERMISSION);
        //} else {
        // permission has been granted, continue as usual
        //    mGpsPermissionApproved = true;
        // }
        mGpsPermissionApproved =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;

        //mGpsPermissionNeededMessage = getString(R.string.permission_rationale);
        //mAcquiringGpsMessage = getString(R.string.acquiring_gps);


        //SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //mSpeedLimit = sharedPreferences.getInt(PREFS_SPEED_LIMIT_KEY, SPEED_LIMIT_DEFAULT_MPH);

        mSpeed = 0;

        mWaitingForGpsSignal = true;



          /*
         * If this hardware doesn't support GPS, we warn the user. Note that when such device is
         * connected to a phone with GPS capabilities, the framework automatically routes the
         * location requests from the phone. However, if the phone becomes disconnected and the
         * wearable doesn't support GPS, no location is recorded until the phone is reconnected.
         */
        if (!hasGps()) {
            Log.w(TAG, "This hardware doesn't have GPS, so we warn user.");
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.gps_not_available))
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            dialog.cancel();
                        }
                    })
                    .setCancelable(false)
                    .create()
                    .show();
        }


        setupViews();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();


        //super.onStart();
        //mGoogleApiClient.connect();
        //.addApi(Wearable.API)
        //requestLocation();

        smoothHandler.postDelayed(new Runnable() {
            public void run () {
                // filter every 100msec

                double alpha = 0.05;
                smoothedRate = m_relative_speed * alpha + smoothedRate * (1.0 - alpha);
                if (audioPlayer != null) {
                    audioPlayer.setPlaybackSpeed(smoothedRate);
                }
                //Log.d("smooth", "new rate " + smoothedRate);
                smoothHandler.postDelayed(this,delay);
            }
        }, delay);

    }

    public void turnOnScreen(){
        // turn on screen
        Log.v("ProximityActivity", "ON!");
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "tag");
        mWakeLock.acquire();
    }

    //@TargetApi(21) //Suppress lint error for PROXIMITY_SCREEN_OFF_WAKE_LOCK
    public void turnOffScreen(){
        // turn off screen
        Log.v("ProximityActivity", "OFF!");
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "tag");
        mWakeLock.acquire();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ict, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //http://stackoverflow.com/questions/17163505/how-to-add-new-item-to-setting-menu-at-android
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            //ict.upload();
            //return true;
            Intent activity =new Intent(this, ShowSettingsActivity.class);
            activity.putExtra("EXTRA_CORRPUT_READ_COUNT", ict.corruptReadCount);
            activity.putExtra("EXTRA_GOOD_READ_COUNT", ict.goodReadCount);
            activity.putExtra("EXTRA_TOTAL_READ_COUNT", ict.totalReadCount);
            activity.putExtra("EXTRA_READS_PER_SECOND", ict.readRate);
            startActivity(activity);
            return true;
        }
        if (id == R.id.action_exit) {



            //http://stackoverflow.com/questions/17719634/how-to-exit-an-android-app-using-code
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            // change this to finish all processes via a message
            android.os.Process.killProcess(android.os.Process.myPid());
            return true;
        }
        if (id == R.id.action_settings) {
            //http://stackoverflow.com/questions/4169714/how-to-call-activity-from-a-menu-item-in-android
            Intent myIntent = new Intent(this, PrefsActivity.class);
            startActivity(myIntent);
            //startActivityForResult(myIntent, 0);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
//    public void audioPlayer(String path, String fileName){
//        //set up MediaPlayer
//        MediaPlayer mp = new MediaPlayer();
//
//        try {
//            mp.setDataSource(path + File.separator + fileName);
//            mp.prepare();
//            mp.start();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    double m_relative_speed = 0.5;




    private void setupViews() {
        //mSpeedLimitTextView = (TextView) findViewById(R.id.max_speed_text);
        //mSpeedTextView = (TextView) findViewById(R.id.current_speed_text);
        mCurrentSpeedMphTextView = (TextView) findViewById(R.id.current_speed_mph);
        mRelativePlaybackSpeedTextView = (TextView) findViewById(R.id.relative_playback_speed);
        mTextViewPwm = (TextView) findViewById(R.id.textViewPwm);
        mTextViewCurrent = (TextView) findViewById(R.id.textViewCurrent);
        mTextViewThrottle = (TextView) findViewById(R.id.textViewThrottle);

        //mGpsPermissionImageView = (ImageView) findViewById(R.id.gps_permission);
        //mGpsIssueTextView = (TextView) findViewById(R.id.gps_issue_text);
        mBlinkingGpsStatusDotView = findViewById(R.id.dot);

        updateActivityViewsBasedOnLocationPermissions();
    }


    // source: https://github.com/googlesamples/android-SpeedTracker/blob/master/Wearable/src/main/java/com/example/android/wearable/speedtracker/WearableMainActivity.java
    private static final String TAG = "LocationActivity";
    private boolean mGpsPermissionApproved;
    //private static final long UPDATE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(1);
    private static final long UPDATE_INTERVAL_MS = 100;
    private static final long FASTEST_INTERVAL_MS = 100;
    // Id to identify Location permission request.
    private static final int REQUEST_GPS_PERMISSION = 1;

    private static final double MPH_IN_METERS_PER_SECOND = 2.23694;
    private static final long INDICATOR_DOT_FADE_AWAY_MS = 500L;

    private GoogleApiClient mGoogleApiClient;
    private boolean mWaitingForGpsSignal;
    private double mSpeed;

    //private String mGpsPermissionNeededMessage;
    //private String mAcquiringGpsMessage;

    private TextView mCurrentSpeedMphTextView;
    private TextView mRelativePlaybackSpeedTextView;
    private TextView mTextViewPwm;
    private TextView mTextViewCurrent;
    private TextView mTextViewThrottle;
    private View mBlinkingGpsStatusDotView;

    private Handler mHandler = new Handler();


    @Override
    public void onConnected(Bundle bundle) {

        Log.d(TAG, "onConnected()");
        requestLocation();
    }
    private void requestLocation() {
        Log.d(TAG, "requestLocation()");

        /*
         * mGpsPermissionApproved covers 23+ (M+) style permissions. If that is already approved or
         * the device is pre-23, the app uses mSaveGpsLocation to save the user's location
         * preference.
         */
        if (mGpsPermissionApproved) {

            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(UPDATE_INTERVAL_MS)
                    .setFastestInterval(FASTEST_INTERVAL_MS);

            LocationServices.FusedLocationApi
                    .requestLocationUpdates(mGoogleApiClient, locationRequest, this)
                    .setResultCallback(new ResultCallback<Status>() {

                        @Override
                        public void onResult(Status status) {
                            if (status.getStatus().isSuccess()) {
                                if (Log.isLoggable(TAG, Log.DEBUG)) {
                                    Log.d(TAG, "Successfully requested location updates");
                                }
                            } else {
                                Log.e(TAG,
                                        "Failed in requesting location updates, "
                                                + "status code: "
                                                + status.getStatusCode() + ", message: " + status
                                                .getStatusMessage());
                            }
                        }
                    });
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended(): connection to location client suspended");

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed(): " + connectionResult.getErrorMessage());
    }


    public void setPlaybackSpeed() {
        // hit speed of
        // 2.0 at 20MPH
        // 1.0 at 6.7MPH
        // 0.5 at 0MPH
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //http://stackoverflow.com/questions/17844511/android-preferences-error-string-cannot-be-cast-to-int
        int normalSpeed = Integer.parseInt(sharedPreferences.getString("normal_speed", "20"));
        double minRate = 0.3;
        double slope = (1.0-minRate);

        m_relative_speed =  minRate+slope*mSpeed/(normalSpeed);



    }



    Random r = new Random();
    @Override
    public void onLocationChanged(Location location) {
        //Log.d(TAG, "onLocationChanged() : " + location);
        //Log.d("onLocation","thread: " + android.os.Process.myTid());

        if (mWaitingForGpsSignal) {
            mWaitingForGpsSignal = false;
            updateActivityViewsBasedOnLocationPermissions();
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean useRandom = sharedPreferences.getBoolean("random_speed", false);
        boolean useFast = sharedPreferences.getBoolean("fast_speed", false);
        boolean useCar = sharedPreferences.getBoolean("speed_from_car", false);
        //http://stackoverflow.com/questions/17844511/android-preferences-error-string-cannot-be-cast-to-int
        int normalSpeed = Integer.parseInt(sharedPreferences.getString("normal_speed", "20"));
        if (useRandom) {
            // mode to test with random speed changes
            int minSpeed = 0; // MPH
            int maxSpeed = normalSpeed; // MPH
            mSpeed = r.nextInt(maxSpeed - minSpeed) + minSpeed;
        } else if(useFast) {
            mSpeed = normalSpeed * 1.5 * MPH_IN_METERS_PER_SECOND;
        } else if(useCar) {
            //
        } else
        {
            mSpeed = location.getSpeed() * MPH_IN_METERS_PER_SECOND;

        }
        setPlaybackSpeed();

        updateSpeedInViews();
        //addLocationEntry(location.getLatitude(), location.getLongitude());
    }

    // callback for ict
    public void setSpeed(double speed) {
        mSpeed = speed;
    }

    private void updateSpeedInViews() {

        if (mGpsPermissionApproved) {

            String s = String.format("PWM: %.1f %%, %.1f %%", ict.pwm1, ict.pwm2);
            mTextViewPwm.setText(s);

            s = String.format("current: %.1f A, %.1f A", ict.i1, ict.i2);
            mTextViewCurrent.setText(s);

            s = String.format("throttle: %.1f A, %.1f A", ict.iThrottle1, ict.iThrottle2);
            mTextViewThrottle.setText(s);

            s = String.format("speed %.1f MPH, %.1f rpm\n good reads: %d, total reads: %d", mSpeed, ict.rpm1, ict.goodReadCount,ict.totalReadCount);
            mCurrentSpeedMphTextView.setText(s);
            mRelativePlaybackSpeedTextView.setText(String.format(getString(R.string.playback_speed_format), m_relative_speed));

            //mSpeedLimitTextView.setText(getString(R.string.speed_limit, mSpeedLimit));
            //mSpeedTextView.setText(String.format(getString(R.string.speed_format), mSpeed));

            // Adjusts the color of the speed based on its value relative to the speed limit.
            //SpeedState state = SpeedState.ABOVE;
            //if (mSpeed <= mSpeedLimit - 5) {
            //    state = SpeedState.BELOW;
            //} else if (mSpeed <= mSpeedLimit) {
            //    state = SpeedState.CLOSE;
            //}

            //mSpeedTextView.setTextColor(getResources().getColor(state.getColor()));

            // Causes the (green) dot blinks when new GPS location data is acquired.
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mBlinkingGpsStatusDotView.setVisibility(View.VISIBLE);
                    //Log.d("green_dot","got gps update");
                }
            });
            mBlinkingGpsStatusDotView.setVisibility(View.VISIBLE);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBlinkingGpsStatusDotView.setVisibility(View.INVISIBLE);
                }
            }, INDICATOR_DOT_FADE_AWAY_MS);
        }
    }
    /**
     * Adjusts the visibility of views based on location permissions.
     */
    private void updateActivityViewsBasedOnLocationPermissions() {

        /*
         * If the user has approved location but we don't have a signal yet, we let the user know
         * we are waiting on the GPS signal (this sometimes takes a little while). Otherwise, the
         * user might think something is wrong.
         */
        if (mGpsPermissionApproved && mWaitingForGpsSignal) {
//
//            // We are getting a GPS signal w/ user permission.
//            mGpsIssueTextView.setText(mAcquiringGpsMessage);
//            mGpsIssueTextView.setVisibility(View.VISIBLE);
//            mGpsPermissionImageView.setImageResource(R.drawable.ic_gps_saving_grey600_96dp);
//
//            mSpeedTextView.setVisibility(View.GONE);
//            mSpeedLimitTextView.setVisibility(View.GONE);
            mCurrentSpeedMphTextView.setVisibility(View.GONE);

        } else if (mGpsPermissionApproved) {

//            mGpsIssueTextView.setVisibility(View.GONE);
//
//            mSpeedTextView.setVisibility(View.VISIBLE);
//            mSpeedLimitTextView.setVisibility(View.VISIBLE);
            mCurrentSpeedMphTextView.setVisibility(View.VISIBLE);
//            mGpsPermissionImageView.setImageResource(R.drawable.ic_gps_saving_grey600_96dp);

        } else {

            // User needs to enable location for the app to work.
//            mGpsIssueTextView.setVisibility(View.VISIBLE);
//            mGpsIssueTextView.setText(mGpsPermissionNeededMessage);
//            mGpsPermissionImageView.setImageResource(R.drawable.ic_gps_not_saving_grey600_96dp);
//
//            mSpeedTextView.setVisibility(View.GONE);
//            mSpeedLimitTextView.setVisibility(View.GONE);
            mCurrentSpeedMphTextView.setVisibility(View.GONE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        Log.d(TAG, "onRequestPermissionsResult(): " + permissions);


        if (requestCode == REQUEST_GPS_PERMISSION) {
            Log.i(TAG, "Received response for GPS permission request.");

            if ((grantResults.length == 1)
                    && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.i(TAG, "GPS permission granted.");
                mGpsPermissionApproved = true;

                if(mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    requestLocation();
                }

            } else {
                Log.i(TAG, "GPS permission NOT granted.");
                mGpsPermissionApproved = false;
            }

            updateActivityViewsBasedOnLocationPermissions();

        }
    }

    /**
     * Returns {@code true} if this device has the GPS capabilities.
     */
    private boolean hasGps() {
        boolean gps =getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
        return gps;
    }

    public void onGpsPermissionClick(View view) {

        if (!mGpsPermissionApproved) {

            Log.i(TAG, "Location permission has NOT been granted. Requesting permission.");

            // On 23+ (M+) devices, GPS permission not granted. Request permission.
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_GPS_PERMISSION);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
        if ((mGoogleApiClient != null) && (mGoogleApiClient.isConnected()) &&
                (mGoogleApiClient.isConnecting())) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        ict.disconnect();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        // connect to the server
        if (ict != null) {
            ict.startPollingCar();
        }
    }

    public class LocationReceiver extends BroadcastReceiver {

        private String TAG = this.getClass().getSimpleName();

        private LocationResult mLocationResult;

        @Override
        public void onReceive(Context context, Intent intent) {
            // Need to check and grab the Intent's extras like so
            if(LocationResult.hasResult(intent)) {
                this.mLocationResult = LocationResult.extractResult(intent);
                Log.i(TAG, "Location Received: " + this.mLocationResult.toString());
            }

        }
    }



    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
