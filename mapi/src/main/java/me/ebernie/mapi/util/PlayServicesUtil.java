package me.ebernie.mapi.util;

import com.google.android.gms.common.ConnectionResult;

/**
 * Created by andhie on 03/10/2015.
 */
public class PlayServicesUtil {

    public static String getFriendlyName(ConnectionResult result) {
        String msg;
        switch (result.getErrorCode()) {
            case ConnectionResult.API_UNAVAILABLE:
                msg = "API_UNAVAILABLE";
                break;
            case ConnectionResult.CANCELED:
                msg = "CANCELED";
                break;
            case ConnectionResult.DEVELOPER_ERROR:
                msg = "DEVELOPER_ERROR";
                break;
            case ConnectionResult.DRIVE_EXTERNAL_STORAGE_REQUIRED:
                msg = "DRIVE_EXTERNAL_STORAGE_REQUIRED";
                break;
            case ConnectionResult.INTERNAL_ERROR:
                msg = "INTERNAL_ERROR";
                break;
            case ConnectionResult.INTERRUPTED:
                msg = "INTERRUPTED";
                break;
            case ConnectionResult.INVALID_ACCOUNT:
                msg = "INVALID_ACCOUNT";
                break;
            case ConnectionResult.LICENSE_CHECK_FAILED:
                msg = "LICENSE_CHECK_FAILED";
                break;
            case ConnectionResult.NETWORK_ERROR:
                msg = "NETWORK_ERROR";
                break;
            case ConnectionResult.RESOLUTION_REQUIRED:
                msg = "RESOLUTION_REQUIRED";
                break;
            case ConnectionResult.SERVICE_DISABLED:
                msg = "SERVICE_DISABLED";
                break;
            case ConnectionResult.SERVICE_INVALID:
                msg = "SERVICE_INVALID";
                break;
            case ConnectionResult.SERVICE_MISSING:
                msg = "SERVICE_MISSING";
                break;
            case ConnectionResult.SERVICE_MISSING_PERMISSION:
                msg = "SERVICE_MISSING_PERMISSION";
                break;
            case ConnectionResult.SERVICE_UPDATING:
                msg = "SERVICE_UPDATING";
                break;
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                msg = "SERVICE_VERSION_UPDATE_REQUIRED";
                break;
            case ConnectionResult.SIGN_IN_FAILED:
                msg = "SIGN_IN_FAILED";
                break;
            case ConnectionResult.SUCCESS:
                msg = "SUCCESS";
                break;
            case ConnectionResult.TIMEOUT:
                msg = "TIMEOUT";
                break;

            default:
                msg = "Unknown Error Code : " + result.getErrorCode();
        }
        return msg;
    }

}
