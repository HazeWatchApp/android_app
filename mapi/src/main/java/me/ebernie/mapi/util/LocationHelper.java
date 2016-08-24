package me.ebernie.mapi.util;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.format.DateUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import my.codeandroid.hazewatch.R;

/**
 * This class helps in handling {@link GoogleApiClient} lifecycle, requests Permissions
 * and also provides Location updates
 */
public class LocationHelper extends Fragment implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, ResultCallback<LocationSettingsResult> {

    private static final String TAG = LocationHelper.class.getName();

    /**
     * Attaches {@link LocationHelper} to the given {@link FragmentActivity}
     *
     * @param activity
     */
    public static void attach(FragmentActivity activity) {
        FragmentManager fm = activity.getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(TAG);
        if (fragment == null) {
            fm.beginTransaction()
                    .add(new LocationHelper(), TAG)
                    .commit();
        }
    }

    /**
     * Forward {@link FragmentActivity#onActivityResult(int, int, Intent)} to {@link LocationHelper}
     *
     * @param activity
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public static void onActivityResult(FragmentActivity activity, int requestCode, int resultCode, Intent data) {
        Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(TAG);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    private static final String REQUESTING_LOCATION_UPDATES_KEY = "requesting_loc";
    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    private static final int PERMISSION_REQUEST_LOCATION = 44;
    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final int REQUEST_CHECK_SETTINGS = 1011;

    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;
    private boolean mRequestingLocationUpdates = true;

    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create a GoogleApiClient instance
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if (savedInstanceState != null) {
            mResolvingError = savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
            mRequestingLocationUpdates = savedInstanceState.getBoolean(REQUESTING_LOCATION_UPDATES_KEY);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mResolvingError) {  // more about this later
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            checkLocationSettingsAndStart();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        super.onStop();
        stopLocationUpdates();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == Activity.RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            }
        } else if (requestCode == REQUEST_CHECK_SETTINGS) {
            mResolvingError = false;
            switch (resultCode) {
                case Activity.RESULT_OK:
                    // All required changes were successfully made
                case Activity.RESULT_CANCELED:
                    // The user was asked to change settings, but chose not to
                default:
                    // use startLocationUpdates instead of checkLocationSettingsAndStart
                    // dont care the user's decision, we will proceed with getting location
                    // since users might have location but not the settings we wanted.
                    startLocationUpdates();
                    break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // We can now safely use the API we requested access to
                Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                LocationUtil.updateLocation(location);

                mRequestingLocationUpdates = true;
                checkLocationSettingsAndStart();
            } else {
                // Permission was denied or request was cancelled
                Snackbar.make(getActivity().getWindow().getDecorView(),
                        R.string.location_denied,
                        Snackbar.LENGTH_LONG)
                        .show();

                AnalyticsManager.sendEvent(AnalyticsManager.CAT_UX,
                        AnalyticsManager.ACTION_PERMISSION_DENIED,
                        Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        // Connected to Google Play services!
        // The good stuff goes here.

        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // Display UI and wait for user interaction
                buildRationaleDialog().show();

            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_LOCATION);
            }
        } else {
            // handle everything at onRequestPermissionsResult
            onRequestPermissionsResult(PERMISSION_REQUEST_LOCATION,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    new int[]{PackageManager.PERMISSION_GRANTED});
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.

        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (connectionResult.hasResolution()) {
            try {
                mResolvingError = true;
                connectionResult.startResolutionForResult(getActivity(), REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            GoogleApiAvailability.getInstance()
                    .getErrorDialog(getActivity(), connectionResult.getErrorCode(), REQUEST_RESOLVE_ERROR)
                    .show();
            mResolvingError = true;
        }

        AnalyticsManager.sendEvent(AnalyticsManager.CAT_UX,
                AnalyticsManager.ACTION_GMS_NO_RESOLUTION,
                PlayServicesUtil.getFriendlyName(connectionResult) + ", hasResolution = " + connectionResult.hasResolution());
    }

    @Override
    public void onLocationChanged(Location location) {
        LocationUtil.updateLocation(location);
    }

    protected LocationRequest createLocationRequest() {
        LocationRequest request = new LocationRequest();
        request.setInterval(DateUtils.SECOND_IN_MILLIS * 10);
        request.setFastestInterval(DateUtils.SECOND_IN_MILLIS);
        request.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        request.setNumUpdates(3);

        return request;
    }

    protected void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mGoogleApiClient.isConnected()) {
                LocationRequest request = createLocationRequest();
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, request, this);
            }
        }
    }

    protected void stopLocationUpdates() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    private AlertDialog buildRationaleDialog() {
        AnalyticsManager.sendEvent(AnalyticsManager.CAT_UX, AnalyticsManager.ACTION_SHOW_RATIONALE_DIALOG, null);

        return new AlertDialog.Builder(getActivity())
                .setMessage(R.string.location_rationale)
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                PERMISSION_REQUEST_LOCATION);
                    }
                })
                .create();
    }

    private void checkLocationSettingsAndStart() {
        if (mGoogleApiClient.isConnected()) {
            final LocationRequest request = createLocationRequest();
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(request);

            PendingResult<LocationSettingsResult> settingsResult =
                    LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

            settingsResult.setResultCallback(this);
        }
    }

    @Override
    public void onResult(LocationSettingsResult result) {
        final Status status = result.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                // All location settings are satisfied. The client can initialize location
                // requests here.
                startLocationUpdates();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                // Location settings are not satisfied. But could be fixed by showing the user
                // a dialog.
                try {
                    mResolvingError = true;
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    status.startResolutionForResult(getActivity(), REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {
                    // Ignore the error.
                    e.printStackTrace();
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                // Location settings are not satisfied. However, we have no way to fix the
                // settings so we won't show the dialog.
                startLocationUpdates();
                break;
        }

    }
}
