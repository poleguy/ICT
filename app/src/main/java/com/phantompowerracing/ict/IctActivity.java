package com.phantompowerracing.ict;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.media.MediaPlayer;
import java.io.File;

import android.os.Handler;


import android.app.AlertDialog;
import android.content.DialogInterface;

import android.content.pm.PackageManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;


import java.util.Random;
import java.util.concurrent.TimeUnit;
import android.support.v4.app.ActivityCompat;
import android.location.Location;
import android.support.annotation.NonNull;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import android.Manifest;
import android.widget.TextView;

public class IctActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        LocationListener{

    private final AudioPlayer audioPlayer = new AudioPlayer("ICT_turkey.wav");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ict);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        audioPlayer.start(); // start thread, use .play() to actually play

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
 

                //audioPlayerB("/sdcard","Carmen Ring.mp3");
                //audioPlayerB("/sdcard/Download","ICT_turkey.wav");
                //audioPlayerB("/data/local","tis.wav");
                audioPlayer.play();


            }
        });

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

        mGpsPermissionNeededMessage = getString(R.string.permission_rationale);
        mAcquiringGpsMessage = getString(R.string.acquiring_gps);


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
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
            startActivity(new Intent(this, ShowSettingsActivity.class));
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
    public void audioPlayer(String path, String fileName){
        //set up MediaPlayer
        MediaPlayer mp = new MediaPlayer();

        try {
            mp.setDataSource(path + File.separator + fileName);
            mp.prepare();
            mp.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    double m_relative_speed = 0.5;




    private void setupViews() {
        //mSpeedLimitTextView = (TextView) findViewById(R.id.max_speed_text);
        //mSpeedTextView = (TextView) findViewById(R.id.current_speed_text);
        mCurrentSpeedMphTextView = (TextView) findViewById(R.id.current_speed_mph);
        mRelativePlaybackSpeedTextView = (TextView) findViewById(R.id.relative_playback_speed);

        //mGpsPermissionImageView = (ImageView) findViewById(R.id.gps_permission);
        //mGpsIssueTextView = (TextView) findViewById(R.id.gps_issue_text);
        mBlinkingGpsStatusDotView = findViewById(R.id.dot);

        updateActivityViewsBasedOnLocationPermissions();
    }


    // source: https://github.com/googlesamples/android-SpeedTracker/blob/master/Wearable/src/main/java/com/example/android/wearable/speedtracker/WearableMainActivity.java
    private static final String TAG = "LocationActivity";
    private boolean mGpsPermissionApproved;
    private static final long UPDATE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(5);
    private static final long FASTEST_INTERVAL_MS = TimeUnit.SECONDS.toMillis(5);
    // Id to identify Location permission request.
    private static final int REQUEST_GPS_PERMISSION = 1;

    private static final float MPH_IN_METERS_PER_SECOND = 2.23694f;
    private static final long INDICATOR_DOT_FADE_AWAY_MS = 500L;

    private GoogleApiClient mGoogleApiClient;
    private boolean mWaitingForGpsSignal;
    private float mSpeed;

    private String mGpsPermissionNeededMessage;
    private String mAcquiringGpsMessage;

    private TextView mCurrentSpeedMphTextView;
    private TextView mRelativePlaybackSpeedTextView;
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
        if (audioPlayer != null) {
            //int rate = (int) (((double) mPlaybackRate) * (m_relative_speed));
            audioPlayer.setPlaybackSpeed(m_relative_speed);
            //Log.d("DecodeActivity", "new rate " + rate);
        }


    }

    Random r = new Random();
    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged() : " + location);
        Log.d("onLocation","thread: " + android.os.Process.myTid());

        if (mWaitingForGpsSignal) {
            mWaitingForGpsSignal = false;
            updateActivityViewsBasedOnLocationPermissions();
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean useRandom = sharedPreferences.getBoolean("random_speed", false);
        //http://stackoverflow.com/questions/17844511/android-preferences-error-string-cannot-be-cast-to-int
        int normalSpeed = Integer.parseInt(sharedPreferences.getString("normal_speed", "20"));
        if (useRandom) {
            // mode to test with random speed changes
            int minSpeed = 0; // MPH
            int maxSpeed = normalSpeed; // MPH
            mSpeed = r.nextInt(maxSpeed - minSpeed) + minSpeed;
        } else {
            mSpeed = location.getSpeed() * MPH_IN_METERS_PER_SECOND;
        }
        setPlaybackSpeed();

        updateSpeedInViews();
        //addLocationEntry(location.getLatitude(), location.getLongitude());
    }


    private void updateSpeedInViews() {

        if (mGpsPermissionApproved) {
            mCurrentSpeedMphTextView.setText(String.format(getString(R.string.speed_format), mSpeed));
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
        if ((mGoogleApiClient != null) && (mGoogleApiClient.isConnected()) &&
                (mGoogleApiClient.isConnecting())) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }
}
