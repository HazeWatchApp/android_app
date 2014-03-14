package me.ebernie.mapi;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

import my.codeandroid.hazewatch.R;

public class MainActivity extends Activity {

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ActionBar ab = getActionBar();
		ab.setIcon(R.drawable.hazeicon);
		ab.setTitle(null);

		Fragment fragment = getFragmentManager().findFragmentById(android.R.id.content);
		if (fragment == null) {
			fragment = new ApiListFragment();
			getFragmentManager().beginTransaction().add(android.R.id.content, fragment)
					.commit();
		}

	}

}
