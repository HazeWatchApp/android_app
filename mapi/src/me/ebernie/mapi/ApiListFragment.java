package me.ebernie.mapi;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import me.ebernie.mapi.api.DataApi;
import me.ebernie.mapi.db.DatabaseHelper.PersistableDataListener;
import me.ebernie.mapi.model.AirPolutionIndex;
import my.codeandroid.hazewatch.R;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher.OnRefreshListener;
import uk.co.senab.actionbarpulltorefresh.library.viewdelegates.AbsListViewDelegate;

@SuppressLint("SimpleDateFormat")
public class ApiListFragment extends Fragment implements
        PersistableDataListener, OnNavigationListener, OnRefreshListener {

    private Multimap<String, AirPolutionIndex> indices = HashMultimap.create();
    private GridView grid;
    private ArrayAdapter<String> navAdapter;
    private ArrayList<AirPolutionIndex> tmp = new ArrayList<AirPolutionIndex>();
    private int currentSelection = 0;
    private Typeface robotoLightItalic = null;
    private Typeface robotoLight = null;
    private Typeface robotoBold = null;
    private PullToRefreshAttacher pullToRefreshHelper;
    private Animation stackFromBottom;
    private View progressBar;
    private TextView emptyText;
    //	private Date updateDate = new Date();
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM");
//	private TextView date;
//	private boolean isDateVisible = false;

    private static final String PREF_KEY_STATE_SELECTION = "state_selection";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        robotoLightItalic = Typeface.createFromAsset(getActivity().getAssets(),
                "fonts/Roboto-LightItalic.ttf");
        robotoLight = Typeface.createFromAsset(getActivity().getAssets(),
                "fonts/Roboto-Light.ttf");
        robotoBold = Typeface.createFromAsset(getActivity().getAssets(),
                "fonts/Roboto-Bold.ttf");
        stackFromBottom = AnimationUtils
                .makeInChildBottomAnimation(getActivity());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu, menu);
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
        grid.setEmptyView(view.findViewById(R.id.empty_layout));
        View emptyView = grid.getEmptyView();
        emptyText = (TextView) emptyView.findViewById(R.id.empty_text);
        emptyText.setTypeface(robotoLightItalic);
        progressBar = emptyView.findViewById(R.id.progress);
        PullToRefreshAttacher.ViewDelegate handler = new AbsListViewDelegate();
        pullToRefreshHelper = ((MainActivity) getActivity())
                .getPullToRefreshHelper();
        pullToRefreshHelper.setRefreshableView(grid, handler, this);
//		date = (TextView) view.findViewById(R.id.date);
//		date.setTypeface(robotoBold);
//		if (isDateVisible) {
//			date.setVisibility(View.VISIBLE);
//		}
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ActionBar ab = getActivity().getActionBar();
        ab.setListNavigationCallbacks(navAdapter, ApiListFragment.this);
        if (!indices.isEmpty()) {
            ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            ab.setSelectedNavigationItem(currentSelection);
        } else {
            // async call to fetch AirPolutionIndex
            pullToRefreshHelper.setRefreshing(true);
            DataApi.INSTANCE.getIndex(getActivity(), this);
        }
    }

    class AirPolutionIndexAdapter extends ArrayAdapter<AirPolutionIndex> {
        private final List<AirPolutionIndex> indices;
        private final int layout;
        private Date currentTime = new Date();
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
            cal.set(Calendar.MINUTE, 0);
            sevenAm.setTime(cal.getTimeInMillis());
            cal.set(Calendar.HOUR_OF_DAY, 11);
            cal.set(Calendar.MINUTE, 0);
            elevenAm.setTime(cal.getTimeInMillis());
            cal.set(Calendar.HOUR_OF_DAY, 17);
            cal.set(Calendar.MINUTE, 0);
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

            if (currentTime.before(elevenAm)) {
                holder.curTimeIndex.setText(index.getSevenAmIndex());
                holder.curTime.setText(R.string.seven);
                holder.curTimeIndex.setTextColor(getColor(index
                        .getSevenAmIndex()));

                holder.time1.setText(R.string.eleven);
                holder.index1.setText(index.getElevenAmIndex());
                holder.index2.setTextColor(getColor(index.getElevenAmIndex()));

                holder.time2.setText(R.string.five);
                holder.index2.setText(index.getFivePmIndex());
                holder.index2.setTextColor(getColor(index.getFivePmIndex()));

            } else if (currentTime.after(elevenAm)
                    && currentTime.before(fivePm)) {
                // if 11 am has data, just show
                if (hasData(index.getElevenAmIndex())) {
                    holder.curTimeIndex.setText(index.getElevenAmIndex());
                    holder.curTime.setText(R.string.eleven);
                    holder.curTimeIndex.setTextColor(getColor(index
                            .getElevenAmIndex()));

                    holder.time1.setText(R.string.seven);
                    holder.index1.setText(index.getSevenAmIndex());
                    holder.index1
                            .setTextColor(getColor(index.getSevenAmIndex()));
                } else {
                    // if eleven am has no data, then show 7 am
                    holder.curTimeIndex.setText(index.getSevenAmIndex());
                    holder.curTime.setText(R.string.seven);
                    holder.curTimeIndex.setTextColor(getColor(index
                            .getSevenAmIndex()));
                    // eleven am gets shown in the smaller display
                    holder.time1.setText(R.string.eleven);
                    holder.index1.setText(index.getElevenAmIndex());
                    holder.index1.setTextColor(getColor(index
                            .getElevenAmIndex()));
                }

                holder.time2.setText(R.string.five);
                holder.index2.setText(index.getFivePmIndex());
                holder.index2.setTextColor(getColor(index.getFivePmIndex()));

            } else {
                // if 5pm has data, display as usual
                if (hasData(index.getFivePmIndex())) {
                    holder.curTimeIndex.setText(index.getFivePmIndex());
                    holder.curTime.setText(R.string.five);
                    holder.curTimeIndex.setTextColor(getColor(index
                            .getFivePmIndex()));

                    holder.time1.setText(R.string.seven);
                    holder.index1.setText(index.getSevenAmIndex());
                    holder.index1
                            .setTextColor(getColor(index.getSevenAmIndex()));

                    holder.time2.setText(R.string.eleven);
                    holder.index2.setText(index.getElevenAmIndex());
                    holder.index2.setTextColor(getColor(index
                            .getElevenAmIndex()));

                } else {
                    // if 5pm has no data, show eleven am data
                    holder.curTimeIndex.setText(index.getElevenAmIndex());
                    holder.curTime.setText(R.string.eleven);
                    holder.curTimeIndex.setTextColor(getColor(index
                            .getElevenAmIndex()));

                    holder.time1.setText(R.string.seven);
                    holder.index1.setText(index.getSevenAmIndex());
                    holder.index1
                            .setTextColor(getColor(index.getSevenAmIndex()));

                    holder.time2.setText(R.string.five);
                    holder.index2.setText(index.getFivePmIndex());
                    holder.index2
                            .setTextColor(getColor(index.getFivePmIndex()));

                }

            }

            return convertView;
        }

        private boolean hasData(String val) {
            return !"--".equals(val);
        }

        private int getColor(String valStr) {
            int color = Color.parseColor("#000000");
            int val = 0;
            if (hasData(valStr)) {
                val = Integer.valueOf(valStr);
            }
            if (val > 0 && val <= 50) {
                color = Color.parseColor("#00a651");
            } else if (val > 50 && val <= 100) {
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

        pullToRefreshHelper.setRefreshComplete();

        if (getActivity() == null) {
            return;
        }

        ActionBar ab = getActivity().getActionBar();
        if ((index != null && !index.isEmpty())) {
            grid.setVisibility(View.VISIBLE);
            LayoutAnimationController gridAnim = new LayoutAnimationController(
                    stackFromBottom);
            grid.setLayoutAnimation(gridAnim);
            grid.getLayoutAnimation().start();
            indices.clear();
            indices.put(getString(R.string.all_states), null);
            for (AirPolutionIndex api : index) {
                if (api.getState() != null) {
                    indices.put(api.getState(), api);
                }
            }

            // prep actionbar nav list
            ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            String[] states = new String[indices.keySet().size()];
            TreeSet<String> sort = new TreeSet<String>();
            sort.addAll(indices.keySet());
            sort.toArray(states);
            navAdapter = new ArrayAdapter<String>(ab.getThemedContext(),
                    android.R.layout.simple_spinner_dropdown_item, states);
            ab.setListNavigationCallbacks(navAdapter, ApiListFragment.this);
            ab.setSelectedNavigationItem(currentSelection);

            indices.remove(getString(R.string.all_states), null);

            SharedPreferences prefs = getActivity().getSharedPreferences(
                    "me.ebernie.mapi", Context.MODE_PRIVATE);
            int selection = prefs.getInt(PREF_KEY_STATE_SELECTION, 0);
            if (selection != 0
                    && ab.getNavigationItemCount() > currentSelection) {
                currentSelection = selection;
                ab.setSelectedNavigationItem(currentSelection);
            }
        } else if (indices.isEmpty() && index == null) {
            // show emptyView
            grid.setVisibility(View.GONE);
//			date.setVisibility(View.GONE);
            progressBar.animate().alpha(0).setDuration(200)
                    .setListener(new AnimatorListener() {

                        @Override
                        public void onAnimationStart(Animator animation) {
                            // TODO Auto-generated method stub

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {
                            // TODO Auto-generated method stub

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            progressBar.setVisibility(View.GONE);
                            emptyText.setVisibility(View.VISIBLE);
                            emptyText.animate().alpha(1).setDuration(200)
                                    .start();
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            // TODO Auto-generated method stub

                        }
                    }).start();

            ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DataApi.INSTANCE.destroy();
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        currentSelection = itemPosition;
        refreshScreenBasedOnSelection();
        SharedPreferences prefs = getActivity().getSharedPreferences(
                "me.ebernie.mapi", Context.MODE_PRIVATE);
        prefs.edit().putInt(PREF_KEY_STATE_SELECTION, currentSelection)
                .commit();
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
        Collections.sort(tmp);
        grid.setAdapter(new AirPolutionIndexAdapter(getActivity(),
                R.layout.fragment_api_list_row, tmp));
    }

    @Override
    public void onRefreshStarted(View view) {
        DataApi.INSTANCE.getIndex(getActivity(), this, true);
    }

    @Override
    public void setUpdateDate(Date date) {
        Crouton.makeText(getActivity(), getString(R.string.last_update) + " "
                + sdf.format(date), Style.INFO).show();

//		this.updateDate = date;
//		this.date.setText(getString(R.string.last_update) + " "
//				+ sdf.format(updateDate));
//		this.date.setVisibility(View.VISIBLE);
//		this.isDateVisible = true;
    }
}
