package com.example.otatar.birplayer;


import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.dgreenhalgh.android.simpleitemdecoration.linear.DividerItemDecoration;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import es.claucookie.miniequalizerlibrary.EqualizerView;

import static com.example.otatar.birplayer.R.drawable.recycler_list_divider;


/**
 * A simple {@link Fragment} subclass.
 */
public class RadioStationListFragment extends Fragment {

    //Log tag
    private static final String LOG_TAG = "RadioStationListFrag";

    private static String SERVER_URL = "http://10.0.2.2:8081/get_stations";
    private static String REMOTE_SERVER_URL = "http://82.118.0.14:8081/get_stations";

    // Bundle string to denote radio station list type
    public static final String EXTRA_LIST_TYPE = "bundle_list_type";

    //Bundle string to denote radio station position in list
    public static final String EXTRA_RADIO_STATION_POSITION = "bundle_radio_station_position";

    // Bundle string to denote radio station list name
    public static final String EXTRA_LIST_TITLE = "bundle_list_title";

    //Swipe refresh
    private SwipeRefreshLayout swipeRefresh;

    //Recycler View
    private RecyclerView recyclerView;

    //Recycler View Adapter
    private RadioStationAdapter radioStationAdapter;

    //Search view
    private SearchView searchView;

    /* Array list that contains all radio station objects */
    private ArrayList<RadioStation> radioStationsList = new ArrayList<>();


    /* Flag selector for list type */
    private Boolean isRadioStationListPartial = false;

    /* Reference to radio station lab object */
    private RadioStationLab radioStationLab;

    /* Reference to activity title */
    String activityTitle;

    /* Container Activity */
    private Main2Activity containerActivity;

    /* Radio Station selected listener */
    private OnRadioStationSelectedListener radioStationSelectedListener;

    /* */
    public static int list_type = 0;


    /**
     * Interface implemented by container activity
     */
    public interface OnRadioStationSelectedListener {
        public void onRadioStationSelected(RadioStation radioStation);
    }


    /**
     * Class needed for recycler view
     */
    private class RadioStationHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView listStationName;
        private TextView listStationLocation;
        private TextView listStationGenre;
        private CheckBox listFavorite;
        private EqualizerView listEqualizer;

        public RadioStationHolder(View itemView) {
            super(itemView);

            listStationName = (TextView) itemView.findViewById(R.id.list_radio_name);
            listStationGenre = (TextView) itemView.findViewById(R.id.list_radio_genre);
            listStationLocation = (TextView) itemView.findViewById(R.id.list_radio_location);
            listFavorite = (CheckBox) itemView.findViewById(R.id.list_favorite);
            listEqualizer = (EqualizerView) itemView.findViewById(R.id.mini_equalizer);

            //Register holder for onClick events
            itemView.setOnClickListener(this);

        }

        void bindRadioStation(final RadioStation radioStation) {

            Log.d(LOG_TAG, getClass().getName() + ":bindRadioStation");

            // Set the content of views
            listStationName.setText(radioStation.getRadioStationName());
            listStationLocation.setText(radioStation.getRadioStationLocation());
            listStationGenre.setText(radioStation.getRadioStationGenre());
            listFavorite.setChecked(radioStation.getFavorite());
            Log.d(LOG_TAG, "Radio: " + radioStation.getRadioStationName() + ": " + radioStation.getPlaying());
            if (radioStation.getPlaying()) {
                listEqualizer.animateBars();
                listEqualizer.setVisibility(View.VISIBLE);
            } else {
                listEqualizer.stopBars();
                listEqualizer.setVisibility(View.GONE);
            }

            listFavorite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(LOG_TAG, getClass().getName() + ":onClick");

                    radioStation.setFavorite(listFavorite.isChecked());
                    RadioStationLab.updateFavoriteRadioStation(radioStation, listFavorite.isChecked());
                }
            });
        }

        @Override
        public void onClick(View v) {

            Log.d(LOG_TAG, getClass().getName() + ":onClick");
            Log.d(LOG_TAG, "Clicked on: "+ listStationName.getText() +" at position " +
                    getAdapterPosition());

            //searchView.clearFocus();

            radioStationSelectedListener.onRadioStationSelected(radioStationsList.get(getAdapterPosition()));

        }
    }


    /**
     * Recycler Adapter Class
     */
    private class RadioStationAdapter extends RecyclerView.Adapter<RadioStationHolder> {

        private ArrayList<RadioStation> radioStationList;
        private Activity activity;

        public RadioStationAdapter(Activity activity, ArrayList<RadioStation> radioStationList) {
            this.radioStationList = radioStationList;
            this.activity = activity;

        }

        //Setter for radioStationList
        public void setRadioStationList(ArrayList<RadioStation> radioStationList) {
            this.radioStationList = radioStationList;
        }

        @Override
        public RadioStationHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            LayoutInflater layoutInflater = LayoutInflater.from(activity);
            View view = layoutInflater.inflate(R.layout.list_radio_station, parent, false);

            return new RadioStationHolder(view);
        }

        @Override
        public void onBindViewHolder(RadioStationHolder holder, int position) {

            RadioStation radioStation = radioStationList.get(position);
            holder.bindRadioStation(radioStation);
        }

        @Override
        public int getItemCount() {
            return radioStationList.size();
        }
    }


    /**
     * Method to instantiate and initialize new RadioStationListFragment object
     * @param refresh
     * @return RadioStationListFragment
     */
     public static RadioStationListFragment newInstance(Boolean refresh) {

        Bundle args = new Bundle();
         args.putBoolean("refresh", refresh);

        RadioStationListFragment fragment = new RadioStationListFragment();
        fragment.setArguments(args);
        return fragment;

    }



    public RadioStationListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View v = inflater.inflate(R.layout.fragment_radio_station_list, container, false);

        recyclerView = (RecyclerView) v.findViewById(R.id.recycler_view_fragment);
        swipeRefresh = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_fragment);

        /* Reference to radio station lab object */
        radioStationLab = RadioStationLab.get(getActivity());

        if (getArguments().getBoolean("refresh")) {

            //Start SwipeRefreshLayout animation (this is workaround, cause there is a bug in api: https://code.google.com/p/android/issues/detail?id=77712)
            swipeRefresh.post(new Runnable() {
                @Override
                public void run() {
                    if (Main2Activity.getInternetConnection() != NetworkUtil.TYPE_NOT_CONNECTED) {
                        swipeRefresh.setRefreshing(true);
                    }
                }
            });

            // Load radio stations from web into database
            radioStationLab.getRadioStationsWeb(REMOTE_SERVER_URL);

            /* Register listener for database refresh */
            radioStationLab.setRadioStationRefreshable(new RadioStationLab.RadioStationRefreshable() {

                //When database is refreshed
                @Override
                public void onRadioStationRefreshed(String s) {

                    //If we didn't receive json object, then it is probably internet problem
                    try {
                        JSONObject jsonObject = new JSONObject(s);
                    } catch (JSONException e) {
                        //Notify user via snack bar
                        Snackbar snackbar = Snackbar
                                .make(v.findViewById(R.id.frame_fragment_layout),
                                        getString(R.string.server_unreachable), Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.ok, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {

                                    }
                                });
                        // Changing action text color
                        snackbar.setActionTextColor(Color.YELLOW);

                        // Changing action button text color
                        View sbView = snackbar.getView();
                        TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
                        textView.setTextColor(Color.WHITE);

                        snackbar.show();

                    }
                    radioStationsList.clear();
                    radioStationsList.addAll(RadioStationLab.getRadioStationsAll());
                    Log.d(LOG_TAG, "Radio station data refreshed!" + radioStationsList.size());

                /* Inform Viewpager's adapter about change */
                    recyclerView.getAdapter().notifyDataSetChanged();

                    //If refreshing animation is running, stop it
                    if (swipeRefresh.isRefreshing()) {
                        Log.d(LOG_TAG, "Refreshing...");
                        swipeRefresh.setRefreshing(false);
                    }


                    //Show recycler view
                    recyclerView.setVisibility(View.VISIBLE);
                }
            });
        }


        radioStationsList.clear();
        radioStationsList.addAll(RadioStationLab.getRadioStationsAll());
        radioStationAdapter = new RadioStationAdapter(getActivity(), radioStationsList);

        //If the list is partial
        if (list_type != 0) {
            filterRadioStations(list_type);
            radioStationAdapter.notifyDataSetChanged();
        }

        //Setting up recycler view
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Setting list divider
        Drawable dividerDrawable = ContextCompat.getDrawable(getActivity(), recycler_list_divider);
        recyclerView.addItemDecoration(new DividerItemDecoration(dividerDrawable));

        //Setting adapter
        recyclerView.setAdapter(radioStationAdapter);
        recyclerView.setVisibility(View.VISIBLE);

        // Register listener for swipe refresh, refreshes radio list in background
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d(LOG_TAG, getClass().getName() + ":onRefresh");

                if (isRadioStationListPartial) {
                    //Well do nothing
                    swipeRefresh.setRefreshing(false);
                } else {
                    // Load radio stations from web into database
                    radioStationLab.getRadioStationsWeb(REMOTE_SERVER_URL);
                }
            }
        });


        return v;
    }

    /**
     * Filters list of radio stations using RadioStationLab's functions
     * @param type
     */
    public void filterRadioStations(int type) {

            switch (type) {

                case -1:
                    //Just return
                    return;
                case 0:
                    //All, show all radio stations
                    radioStationsList.clear();
                    radioStationsList.addAll(RadioStationLab.getRadioStationsAll());
                    radioStationAdapter.setRadioStationList(radioStationsList);
                    radioStationAdapter.notifyDataSetChanged();
                    break;
                case 1:
                    //Favorite
                    radioStationsList.clear();
                    radioStationsList.addAll(RadioStationLab.filterRadioStationByFavorite());
                    isRadioStationListPartial = true;
                    radioStationAdapter.setRadioStationList(radioStationsList);
                    radioStationAdapter.notifyDataSetChanged();
                    break;
                case 2:
                    //Pop
                    radioStationsList.clear();
                    radioStationsList.addAll(RadioStationLab.filterRadioStationsByGenre(RadioStationLab.radioGenrePop));
                    isRadioStationListPartial = true;
                    radioStationAdapter.setRadioStationList(radioStationsList);
                    radioStationAdapter.notifyDataSetChanged();
                    break;
                case 3:
                    //Folk
                    radioStationsList.clear();
                    radioStationsList.addAll(RadioStationLab.filterRadioStationsByGenre(RadioStationLab.radioGenreFolk));
                    isRadioStationListPartial = true;
                    radioStationAdapter.setRadioStationList(radioStationsList);
                    radioStationAdapter.notifyDataSetChanged();
                    break;
                case 4:
                    //Location
                    radioStationsList.clear();
                    radioStationsList.addAll(RadioStationLab.filterRadioStationsByLocation("Sarajevo"));
                    isRadioStationListPartial = true;
                    radioStationAdapter.setRadioStationList(radioStationsList);
                    radioStationAdapter.notifyDataSetChanged();
                    break;
                default:
                    //Do nothing

            }

            //Set list type
            list_type = type;

            //Set subtitle
            containerActivity.setActivitySubtitle(getResources().getStringArray(R.array.drawer_items)[type]);

    }


    public void filterRadioStationByName(String name) {

        Log.d(LOG_TAG, "filterRadioStationsByName()");

        /*radioStationsList.clear();
        radioStationsList = RadioStationLab.filterRadioStationByName(name);
        isRadioStationListPartial = true;
        radioStationAdapter.setRadioStationList(radioStationsList);
        radioStationAdapter.notifyDataSetChanged();*/

        ArrayList<RadioStation> list = new ArrayList<>();

        //Go through stations
        for (RadioStation radioStation : radioStationsList) {

            if (radioStation.getRadioStationName().toLowerCase().contains(name.toLowerCase())) {
                list.add(radioStation);
            }

        }

        isRadioStationListPartial = true;
        radioStationAdapter.setRadioStationList(list);
        radioStationAdapter.notifyDataSetChanged();

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Activity a = null;

        if (context instanceof Activity){
            a=(Activity) context;
        }

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            radioStationSelectedListener = (OnRadioStationSelectedListener) a;
            containerActivity = (Main2Activity) a;
        } catch (ClassCastException e) {
            throw new ClassCastException(a.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

}
