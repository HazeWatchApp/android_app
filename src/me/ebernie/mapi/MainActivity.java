package me.ebernie.mapi;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

public class MainActivity extends Activity {

	private PullToRefreshAttacher mPullToRefreshHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPullToRefreshHelper = new PullToRefreshAttacher(this);

		ActionBar ab = getActionBar();
		ab.setIcon(R.drawable.hazeicon);
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		ab.setTitle(null);

		Fragment fragment = getFragmentManager().findFragmentById(android.R.id.content);
		if (fragment == null) {
			fragment = new ApiListFragment();
			// this ugly call is here because retrieving mPullToRefreshHelper
			// from within the fragment gave us a null :(
//			((ApiListFragment) fragment)
//					.setPullToRefreshHelper(mPullToRefreshHelper);
			getFragmentManager().beginTransaction().add(android.R.id.content, fragment)
					.commit();
		}

	}

	public PullToRefreshAttacher getPullToRefreshHelper() {
		return mPullToRefreshHelper;
	}
}
