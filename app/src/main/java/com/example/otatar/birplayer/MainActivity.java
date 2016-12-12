package com.example.otatar.birplayer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.graphics.drawable.ColorDrawable;

import com.dgreenhalgh.android.simpleitemdecoration.linear.DividerItemDecoration;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import es.claucookie.miniequalizerlibrary.EqualizerView;

public class MainActivity extends AppCompatActivity {

    //Log tag
    private static final String LOG_TAG = "MainActivityLog";

    private static String SERVER_URL = "http://10.0.2.2:8081/get_stations";
    private static String REMOTE_SERVER_URL = "http://82.118.0.14:8081/get_stations";

    // Bundle string to denote radio station list type
    public static final String EXTRA_LIST_TYPE = "bundle_list_type";

    //Bundle string to denote radio station position in list
    public static final String EXTRA_RADIO_STATION_POSITION = "bundle_radio_station_position";

    // Bundle string to denote radio station list name
    public static final String EXTRA_LIST_TITLE = "bundle_list_title";

    // Whether there is a internet connection.
    public static int internetConnection;

    //Drawer Layout
    private DrawerLayout drawerLayout;

    //Drawer toggle
    private ActionBarDrawerToggle actionBarDrawerToggle;

    //List View
    private ListView drawerList;

    //Swipe refresh
    private SwipeRefreshLayout swipeRefresh;

    //Recycler View
    private RecyclerView recyclerView;

    //Recycler View Adapter
    private RadioStationAdapter radioStationAdapter;

    //Search view
    private SearchView searchView;

    /* Array list that contains all radio station objects */
    public static ArrayList<RadioStation> radioStationsAll = new ArrayList<>();

    /* Array list that contains partial radio station objects */
    public static ArrayList<RadioStation> radioStationsPartial;

    /* Flag selector for list type */
    private Boolean isRadioStationListPartial = false;

    /* Reference to radio station lab object */
    private RadioStationLab radioStationLab;

    /* Reference to activity title */
    String activityTitle;


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
            //listStationGenre = (TextView) itemView.findViewById(R.id.list_radio_genre);
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
            //listStationGenre.setText(radioStation.getRadioStationGenre());
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

            searchView.clearFocus();

            //Launch now playing activity and set viewpager on clicked radio station
            Intent intent = new Intent(getApplicationContext(), NowPlayingActivity.class);
            intent.putExtra(EXTRA_LIST_TYPE, isRadioStationListPartial);
            intent.putExtra(EXTRA_LIST_TITLE, activityTitle);
            intent.putExtra(EXTRA_RADIO_STATION_POSITION, getAdapterPosition());
            startActivity(intent);

            //Slide out this activity
            overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);

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



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(LOG_TAG, "Main onCreate");

        // Check network connection and alter user about it
        checkAndAlertNetworkConnection();

        //Get references to views
        drawerLayout = (DrawerLayout) findViewById(R.id.navigation_drawer);
        drawerList = (ListView) findViewById(R.id.list_view);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        //Start SwipeRefreshLayout animation (this is workaround, cause there is a bug in api: https://code.google.com/p/android/issues/detail?id=77712)
        swipeRefresh.post(new Runnable() {
            @Override
            public void run() {
                if (internetConnection != NetworkUtil.TYPE_NOT_CONNECTED) {
                    swipeRefresh.setRefreshing(true);
                }
            }
        });

        /* Reference to radio station lab object */
        radioStationLab = RadioStationLab.get(MainActivity.this);

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
                    //Notify user via snackbar
                    Snackbar snackbar = Snackbar
                            .make(findViewById(R.id.coordinator_layout),
                                    getString(R.string.server_unreachable), Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.ok, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                }
                            });
                    snackbar.show();

                }
                Log.d(LOG_TAG, "Radio station data refreshed!" + radioStationsAll.size());

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

        //Init nav drawer
        drawerList.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.drawer_items)));

        //Set listener for clicks on list
        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //Close drawer
                drawerLayout.closeDrawer(drawerList);

                switch (position) {
                    case 0:
                        //All, show all stations
                        isRadioStationListPartial = false;
                        radioStationAdapter.setRadioStationList(radioStationsAll);
                        radioStationAdapter.notifyDataSetChanged();
                        activityTitle = getResources().getStringArray(R.array.drawer_items)[0];
                        setTitle(activityTitle);
                        break;

                    case 1:
                        //Favorite
                        MainActivity.radioStationsPartial = new ArrayList<>();
                        RadioStationLab.filterRadioStationByFavorite();
                        isRadioStationListPartial = true;
                        radioStationAdapter.setRadioStationList(radioStationsPartial);
                        radioStationAdapter.notifyDataSetChanged();
                        activityTitle = getResources().getStringArray(R.array.drawer_items)[1];
                        setTitle(activityTitle);
                        break;

                    case 2:
                        //Show pop stations
                        MainActivity.radioStationsPartial = new ArrayList<>();
                        RadioStationLab.filterRadioStationsByGenre(RadioStationLab.radioGenrePop);
                        isRadioStationListPartial = true;
                        radioStationAdapter.setRadioStationList(radioStationsPartial);
                        radioStationAdapter.notifyDataSetChanged();
                        activityTitle = getResources().getStringArray(R.array.drawer_items)[2];
                        setTitle(activityTitle);
                        break;

                    case 3:
                        //Show folk stations
                        MainActivity.radioStationsPartial = new ArrayList<>();
                        RadioStationLab.filterRadioStationsByGenre(RadioStationLab.radioGenreFolk);
                        isRadioStationListPartial = true;
                        radioStationAdapter.setRadioStationList(radioStationsPartial);
                        radioStationAdapter.notifyDataSetChanged();
                        activityTitle = getResources().getStringArray(R.array.drawer_items)[3];
                        setTitle(activityTitle);
                        break;

                    case 4:

                        //Show radio stations in Sarajevo
                        MainActivity.radioStationsPartial = new ArrayList<>();
                        RadioStationLab.filterRadioStationsByLocation("Sarajevo");
                        isRadioStationListPartial = true;
                        radioStationAdapter.setRadioStationList(radioStationsPartial);
                        radioStationAdapter.notifyDataSetChanged();
                        activityTitle = getResources().getStringArray(R.array.drawer_items)[4];
                        setTitle(activityTitle);
                        break;

                }

            }
        });

        //Action bar drawer toggle
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.string.open_drawer, R.string.close_drawer) {

            //Called on completely open state
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }

            //Called on completely close state
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
            }

        };

        //Add drawer toggle
        drawerLayout.addDrawerListener(actionBarDrawerToggle);

        //Show button on toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        //Setting up recycler view
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        radioStationAdapter = new RadioStationAdapter(this, radioStationsAll);
        // Setting list divider
        Drawable dividerDrawable = ContextCompat.getDrawable(this, android.R.drawable.divider_horizontal_textfield);
        recyclerView.addItemDecoration(new DividerItemDecoration(dividerDrawable));
        //Setting adapter
        recyclerView.setAdapter(radioStationAdapter);

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
                    radioStationLab.getRadioStationsWeb(SERVER_URL);
                }
            }
        });


    }



    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        actionBarDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        actionBarDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(LOG_TAG, "onQueryTextSubmit: " + query);

                MainActivity.radioStationsPartial = new ArrayList<>();
                RadioStationLab.filterRadioStationByName(query);
                isRadioStationListPartial = true;
                radioStationAdapter.setRadioStationList(radioStationsPartial);
                radioStationAdapter.notifyDataSetChanged();

                searchView.clearFocus();

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(LOG_TAG, "onQueryTextChange: " + newText);

                MainActivity.radioStationsPartial = new ArrayList<>();
                RadioStationLab.filterRadioStationByName(newText);
                isRadioStationListPartial = true;
                radioStationAdapter.setRadioStationList(radioStationsPartial);
                radioStationAdapter.notifyDataSetChanged();

                return true;
            }

        });

        return super.onCreateOptionsMenu(menu);



    }

    /**
     * Checks network connection and alerts user about that
     */
    private void checkAndAlertNetworkConnection() {
        switch (internetConnection = NetworkUtil.checkNetworkConnection(this)) {

            case NetworkUtil.TYPE_WIFI:
                //Do nothing
                break;

            case NetworkUtil.TYPE_MOBILE:
                //Alert user about connection with a dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.mobile_connection_hint)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        //Start NowPlayingActivity
                                        // Intent intent = new Intent(getApplicationContext(), NowPlayingActivity.class);
                                        //startActivity(intent);

                                    }
                                }
                        );

                AlertDialog dialog = builder.create();
                dialog.show();
                break;

            case NetworkUtil.TYPE_NOT_CONNECTED:
                //Alert user about connection with a dialog
                AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
                builder1.setMessage(R.string.no_connection)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                }
                        );

                AlertDialog dialog1 = builder1.create();
                dialog1.show();
                break;

        }
    }

}


