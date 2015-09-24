package me.ebernie.mapi;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import io.fabric.sdk.android.Fabric;
import my.codeandroid.hazewatch.BuildConfig;
import my.codeandroid.hazewatch.R;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Custom Application class
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Set up Crashlytics, disabled for debug builds
        Crashlytics cKit = new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build();

        // Initialize Fabric with the debug-disabled crashlytics.
        Fabric.with(this, cKit);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath(getString(R.string.font_regular))
                        .setFontAttrId(R.attr.fontPath)
                        .build()
        );
    }
}
