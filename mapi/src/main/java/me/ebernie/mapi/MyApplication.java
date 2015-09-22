package me.ebernie.mapi;

import android.app.Application;

import my.codeandroid.hazewatch.R;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Created by andhie on 9/19/15.
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath(getString(R.string.font_regular))
                        .setFontAttrId(R.attr.fontPath)
                        .build()
        );
    }
}
