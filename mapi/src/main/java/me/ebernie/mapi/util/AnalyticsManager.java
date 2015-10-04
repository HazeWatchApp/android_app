/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.ebernie.mapi.util;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;

import my.codeandroid.hazewatch.BuildConfig;
import my.codeandroid.hazewatch.R;


public class AnalyticsManager {

    public static final String CAT_UX = "UX";
    public static final String ACTION_SWIPE_REFRESH = "Pull-To-Refresh";
    public static final String ACTION_CLICK_EXPAND = "Expand Card";
    public static final String ACTION_CLICK_COLLAPSE = "Collapse Card";
    public static final String ACTION_SHOW_RATIONALE_DIALOG = "Rationale Dialog";
    public static final String ACTION_GMS_NO_RESOLUTION = "GMS No Resolution";
    public static final String ACTION_PERMISSION_DENIED = "Permission Denied";

    private static Context sAppContext = null;

    private static Tracker mTracker;
    private final static String TAG = AnalyticsManager.class.getSimpleName();

    public static synchronized void setTracker(Tracker tracker) {
        mTracker = tracker;
    }

    private static boolean canSend() {
        return sAppContext != null && mTracker != null;
    }

    public static void sendScreenView(String screenName) {
        if (canSend()) {
            mTracker.setScreenName(screenName);
            mTracker.send(new HitBuilders.AppViewBuilder().build());
            Log.d(TAG, "Screen View recorded: " + screenName);
        } else {
            Log.d(TAG, "Screen View NOT recorded (analytics disabled or not ready).");
        }
    }

    public static void sendEvent(String category, String action, String label, long value) {
        if (canSend()) {
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory(category)
                    .setAction(action)
                    .setLabel(label)
                    .setValue(value)
                    .build());

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Event recorded:");
                Log.d(TAG, "\tCategory: " + category);
                Log.d(TAG, "\tAction: " + action);
                Log.d(TAG, "\tLabel: " + label);
                Log.d(TAG, "\tValue: " + value);
            }
        } else {
            Log.d(TAG, "Analytics event ignored (analytics disabled or not ready).");
        }
    }

    public static void sendEvent(String category, String action, String label) {
        sendEvent(category, action, label, 0);
    }

    public Tracker getTracker() {
        return mTracker;
    }

    public static synchronized void initializeAnalyticsTracker(Context context) {
        sAppContext = context.getApplicationContext();
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(context);
            mTracker = analytics.newTracker(context.getString(R.string.ga_tracking_id));
            mTracker.enableExceptionReporting(true);
            mTracker.enableAdvertisingIdCollection(true);
            mTracker.enableAutoActivityTracking(false);

            if (BuildConfig.DEBUG) {
                analytics.setDryRun(true);
                analytics.getLogger()
                        .setLogLevel(Logger.LogLevel.VERBOSE);
            }
        }
    }

}
