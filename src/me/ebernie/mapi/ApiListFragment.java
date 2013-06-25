package me.ebernie.mapi;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher.OnRefreshListener;
import uk.co.senab.actionbarpulltorefresh.library.viewdelegates.AbsListViewDelegate;

import me.ebernie.mapi.api.DataApi;
import me.ebernie.mapi.db.DatabaseHelper.PersistableDataListener;
import me.ebernie.mapi.model.AirPolutionIndex;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class ApiListFragment extends Fragment implements
		PersistableDataListener, OnNavigationListener, OnRefreshListener {

	private Multimap<String, AirPolutionIndex> indices = HashMultimap.create();
	private GridView grid;
	private ArrayAdapter<String> navAdapter;
	private ArrayList<AirPolutionIndex> tmp = new ArrayList<AirPolutionIndex>();
	private int currentSelection = 0;
	private Typeface robotoLightItalic = null;
	private Typeface robotoLight = null;
	private PullToRefreshAttacher pullToRefreshHelper;
	private static final String PREF_KEY_STATE_SELECTION = "state_selection";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// async call to fetch AirPolutionIndex
		DataApi.INSTANCE.getIndex(getActivity(), this);
		setRetainInstance(true);
		setHasOptionsMenu(true);
		robotoLightItalic = Typeface.createFromAsset(getActivity().getAssets(),
				"fonts/Roboto-LightItalic.ttf");
		robotoLight = Typeface.createFromAsset(getActivity().getAssets(),
				"fonts/Roboto-Light.ttf");
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu, menu);
	}

	public void setPullToRefreshHelper(PullToRefreshAttacher pullToRefreshHelper) {
		this.pullToRefreshHelper = pullToRefreshHelper;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_refresh:
			pullToRefreshHelper.setRefreshing(true);
			DataApi.INSTANCE.getIndex(getActivity(), this, true);
			return true;
		default:
			return super.onOptionsItemSelected(item);

		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_api_list, null);
		grid = (GridView) view.findViewById(R.id.gridView_api);
		PullToRefreshAttacher.ViewDelegate handler = new AbsListViewDelegate();
		pullToRefreshHelper = ((MainActivity) getActivity())
				.getPullToRefreshHelper();
		pullToRefreshHelper.setRefreshableView(grid, handler, this);
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		getActivity().getActionBar().setListNavigationCallbacks(navAdapter,
				ApiListFragment.this);
		if (!indices.isEmpty()) {
			getActivity().getActionBar().setSelectedNavigationItem(currentSelection);
		}
	}

	class AirPolutionIndexAdapter extends ArrayAdapter<AirPolutionIndex> {
		private final List<AirPolutionIndex> indices;
		private final int layout;
		private Date currentDate = new Date();
		private Date sevenAm = new Date();
		private Date elevenAm = new Date();
		private Date fivePm = new Date();

		public AirPolutionIndexAdapter(Context context, int textViewResourceId,
				List<AirPolutionIndex> indices) {
			super(context, textViewResourceId);
			this.indices = indices;
			this.layout = textViewResourceId;
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, 7);
			sevenAm.setTime(cal.getTimeInMillis());
			cal.set(Calendar.HOUR_OF_DAY, 11);
			elevenAm.setTime(cal.getTimeInMillis());
			cal.set(Calendar.HOUR_OF_DAY, 17);
			fivePm.setTime(cal.getTimeInMillis());
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater().inflate(layout,
						null);
				ViewHolder holder = new ViewHolder(convertView);
				convertView.setTag(holder);
			}
			ViewHolder holder = (ViewHolder) convertView.getTag();
			AirPolutionIndex index = getItem(position);
			holder.townArea.setText(index.getArea());
			// determine current time
			if (currentDate.after(sevenAm) && currentDate.before(elevenAm)) {
				holder.curTimeIndex.setText(index.getTime1());
				holder.curTime.setText(R.string.seven);
				holder.curTimeIndex.setTextColor(getColor(index.getTime1()));

				holder.time1.setText(R.string.eleven);
				holder.index1.setText(index.getTime2());
				holder.index2.setTextColor(getColor(index.getTime2()));

				holder.time2.setText(R.string.five);
				holder.index2.setText(index.getTime3());
				holder.index2.setTextColor(getColor(index.getTime3()));

			} else if (currentDate.after(elevenAm)
					&& currentDate.before(fivePm)) {
				holder.curTimeIndex.setText(index.getTime2());
				holder.curTime.setText(R.string.eleven);
				holder.curTimeIndex.setTextColor(getColor(index.getTime2()));

				holder.time1.setText(R.string.seven);
				holder.index1.setText(index.getTime1());
				holder.index1.setTextColor(getColor(index.getTime1()));

				holder.time2.setText(R.string.five);
				holder.index2.setText(index.getTime3());
				holder.index2.setTextColor(getColor(index.getTime3()));
			} else {
				holder.curTimeIndex.setText(index.getTime3());
				holder.curTime.setText(R.string.five);
				holder.curTimeIndex.setTextColor(getColor(index.getTime3()));

				holder.time1.setText(R.string.seven);
				holder.index1.setText(index.getTime1());
				holder.index1.setTextColor(getColor(index.getTime1()));

				holder.time2.setText(R.string.eleven);
				holder.index2.setText(index.getTime2());
				holder.index2.setTextColor(getColor(index.getTime2()));
			}
			return convertView;
		}

		private int getColor(String valStr) {
			int color = Color.parseColor("#000000");
			int val = 0;
			if (!TextUtils.isEmpty(valStr) && !"--".equals(valStr)) {
				val = Integer.valueOf(valStr);
			}
			if (val > 0 && val <= 50) {
				color = Color.parseColor("#00a651");
			} else if (val > 51 && val <= 100) {
				color = Color.parseColor("#99cc00");
			} else if (val > 100 && val <= 200) {
				color = Color.parseColor("#ffbb33");
			} else if (val > 200 && val <= 300) {
				color = Color.parseColor("#ff4444");
			} else {
				color = Color.parseColor("#cc0000");
			}

			return color;
		}

		@Override
		public int getCount() {
			return indices.size();
		}

		@Override
		public AirPolutionIndex getItem(int position) {
			return indices.get(position);
		}

		private class ViewHolder {
			final TextView townArea;
			// final TextView state;
			final TextView time1;
			final TextView time2;
			final TextView curTimeIndex;
			final TextView curTime;
			final TextView index1;
			final TextView index2;

			ViewHolder(View view) {
				townArea = (TextView) view.findViewById(R.id.town_area);
				townArea.setTypeface(robotoLight);
				// state = view.findViewById(R.id.)
				curTimeIndex = (TextView) view.findViewById(R.id.curIndex);
				curTimeIndex.setTypeface(robotoLight);
				index1 = (TextView) view.findViewById(R.id.index1);
				index1.setTypeface(robotoLight);
				index2 = (TextView) view.findViewById(R.id.index2);
				index2.setTypeface(robotoLight);

				time1 = (TextView) view.findViewById(R.id.time1);
				time1.setTypeface(robotoLightItalic);
				time2 = (TextView) view.findViewById(R.id.time2);
				time2.setTypeface(robotoLightItalic);
				curTime = (TextView) view.findViewById(R.id.curTime);
				curTime.setTypeface(robotoLightItalic);

			}
		}

	}

	@Override
	public void updateList(List<AirPolutionIndex> index) {
		indices.clear();
		indices.put(getString(R.string.all_states), null);
		for (AirPolutionIndex api : index) {
			if (api.getState() != null) {
				indices.put(api.getState(), api);
			}
		}
		// prep actionbar nav list
		ActionBar ab = getActivity().getActionBar();
		String[] states = new String[indices.keySet().size()];
		TreeSet<String> sort = new TreeSet<String>();
		sort.addAll(indices.keySet());
		sort.toArray(states);
		navAdapter = new ArrayAdapter<String>(ab.getThemedContext(),
				android.R.layout.simple_spinner_dropdown_item, states);
		ab.setListNavigationCallbacks(navAdapter, ApiListFragment.this);
		ab.setSelectedNavigationItem(currentSelection);
		indices.remove(getString(R.string.all_states), null);
//		tmp.clear();
//		tmp.addAll(indices.values());
//		grid.setAdapter(new AirPolutionIndexAdapter(getActivity(),
//				R.layout.fragment_api_list_row, tmp));

		pullToRefreshHelper.setRefreshComplete();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		DataApi.INSTANCE.destroy();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		SharedPreferences prefs = getActivity().getSharedPreferences(
			      "me.ebernie.mapi", Context.MODE_PRIVATE);
		int selection = prefs.getInt(PREF_KEY_STATE_SELECTION, 0);
		if (selection != 0) {
			currentSelection = selection;
			getActivity().getActionBar().setSelectedNavigationItem(currentSelection);
		}
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		currentSelection = itemPosition;
		refreshScreenBasedOnSelection();
		SharedPreferences prefs = getActivity().getSharedPreferences(
			      "me.ebernie.mapi", Context.MODE_PRIVATE);
		prefs.edit().putInt(PREF_KEY_STATE_SELECTION, currentSelection).commit();
		return true;
	}

	private void refreshScreenBasedOnSelection() {
		String item = navAdapter.getItem(currentSelection);
		tmp.clear();
		if (getString(R.string.all_states).equals(item)) {
			tmp.addAll(indices.values());
		} else {
			tmp.addAll(indices.get(item));
		}
		grid.setAdapter(new AirPolutionIndexAdapter(getActivity(),
				R.layout.fragment_api_list_row, tmp));
	}

	@Override
	public void onRefreshStarted(View view) {
		DataApi.INSTANCE.getIndex(getActivity(), this, true);
	}
}
