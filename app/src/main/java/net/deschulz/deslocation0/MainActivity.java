package net.deschulz.deslocation0;

import android.*;
import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity implements
        LocationListener, GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = "LocationDebug";
    private static final int UPDATE_INTERVAL = 15 * 1000;
    private static final int FASTEST_UPDATE_INTERVAL = 2 * 1000;
    private static final int REQUEST_CODE_PERMISSIONS = 101;

    private GoogleApiClient mApiClient;
    /* Metadata about updates we want to receive */
    private LocationRequest mLocationRequest;
    /* Last-known device location */
    private Location mCurrentLocation;
    private TextView mLatitude;
    private TextView mLongitude;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mLatitude = (TextView)findViewById(R.id.latitude);
        mLongitude = (TextView)findViewById(R.id.longitude);

        //Verify play services is active and up to date
        int resultCode = GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(this);
        switch (resultCode) {
            case ConnectionResult.SUCCESS:
                Log.d(TAG, "Google Play Services is ready to go!");
                break;
            default:
                showPlayServicesError(resultCode);
                return;
        }

        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .build();

        mLocationRequest = LocationRequest.create()
                //Set the required accuracy level
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                //Set the desired (inexact) frequency of location updates
                .setInterval(UPDATE_INTERVAL)
                //Throttle the max rate of update requests
                .setFastestInterval(FASTEST_UPDATE_INTERVAL);

        // At this point, we are ready to go, but haven't started yet.  Wait for
        // a button press before beginning to listen.
    }

    /* button handler */
    public void doUpdatePosition(View v) {
        Log.d(TAG, "doUpdatePosition()");
        mApiClient.connect();

    }

    @Override
    protected void onStart() {
        //mApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        //mApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");

        //When we move into the foreground, attach to Play Services
        //mApiClient.connect();
    }

    @Override
    public void onPause() {
        super.onPause();
        //Disable updates when we are not in the foreground
        if (mApiClient.isConnected()) {
            LocationServices.FusedLocationApi
                    .removeLocationUpdates(mApiClient, this);
        }
        //Detach from Play Services
        mApiClient.disconnect();
    }

    /* update the display and disconnect */
    private void updateDisplay() {
        Log.d(TAG, "updateDisplay()");
        if (mCurrentLocation != null) {
            mLatitude.setText(String.format("%.7f", mCurrentLocation.getLatitude()));
            mLongitude.setText(String.format("%.7f", mCurrentLocation.getLongitude()));
        }
    }
    /*
     * When Play Services is missing or at the wrong version, the client
     * library will assist with a dialog to help the user update.
     */
    private void showPlayServicesError(int errorCode) {
        GoogleApiAvailability.getInstance()
                .showErrorDialogFragment(this, errorCode, 10,
                        new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                finish();
                            }
                        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connected to Play Services");

        //Get last known location immediately
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_CODE_PERMISSIONS);
            return;
        }
        Log.d(TAG, "calling fetchAndListenForLocation");

        fetchAndListenForLocation();
    }

    @SuppressWarnings("MissingPermission")
    private void fetchAndListenForLocation() {
        mCurrentLocation = LocationServices.FusedLocationApi
                .getLastLocation(mApiClient);

        // the first time we start, we'll be looking at the cached position since
        // we haven't looked at the GPS yet.  Maybe this should populate the form
        // with nothing instead of calling updateDisplay()?
        if (mCurrentLocation != null) {
            Log.d(TAG, "This is the previous location -- update to it, or clear?");
            updateDisplay();
        }
        //Register for updates
        LocationServices.FusedLocationApi.requestLocationUpdates(mApiClient,
                mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    /**
     * LocationListener Callbacks
     */

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Received location update");
        mCurrentLocation = location;
        updateDisplay();
        // just get one update and then disconnect
        mApiClient.disconnect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions,
                grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            for (int i = 0; i < grantResults.length; i++) {
                int grantResult = grantResults[i];
                String permission = permissions[i];
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission " + permission
                            + " is required for this application to work.");
                    finish();
                    return;
                }
            }
        }
    }
}
