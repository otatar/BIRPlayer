package com.example.otatar.birplayer;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.dgreenhalgh.android.simpleitemdecoration.linear.DividerItemDecoration;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    //Log tag
    private static final String LOG_TAG = "MainActivityLog";

    private static String SERVER_URL = "http://10.0.2.2:8081/get_stations";

    // Bundle string to denote radio station list type
    public static final String EXTRA_LIST_TYPE = "bundle_list_type";

    //Bundle string to denote radio station position in list
    public static final String EXTRA_RADIO_STATION_POSITION = "bundle_radio_station_position";

    // Whether there is a Wi-Fi connection.
    private static boolean wifiConnected = false;

    // Whether there is a mobile connection.
    private static boolean mobileConnected = false;

    //Drawer Layout
    private DrawerLayout drawerLayout;

    //Drawer toggle
    private ActionBarDrawerToggle actionBarDrawerToggle;

    //List View
    private ListView drawerList;

    //Recycler View
    private RecyclerView recyclerView;

    //Recycler View Adapter
    private RadioStationAdapter radioStationAdapter;

    /* Array list that contains all radio station objects */
    public static ArrayList<RadioStation> radioStationsAll;

    /* Array list that contains partial radio station objects */
    public static ArrayList<RadioStation> radioStationsPartial;

    /* Flag selector for list type */
    private Boolean isRadioStationListPartial = false;

    /* Reference to radio station lab object */
    private RadioStationLab radioStationLab;


    /**
     * Class needed for recycler view
     */
    private class RadioStationHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView listStationName;
        private TextView listStationLocation;
        private TextView listStationGenre;
        private CheckBox listFavorite;

        public RadioStationHolder(View itemView) {
            super(itemView);

            listStationName = (TextView) itemView.findViewById(R.id.list_radio_name);
            listStationGenre = (TextView) itemView.findViewById(R.id.list_radio_genre);
            listStationLocation = (TextView) itemView.findViewById(R.id.list_radio_location);
            listFavorite = (CheckBox) itemView.findViewById(R.id.list_favorite);

            //Register holder for onClick events
            itemView.setOnClickListener(this);

        }

        void bindRadioStation(RadioStation radioStation) {

            Log.d(LOG_TAG, getClass().getName() + ":bindRadioStation");

            listStationName.setText(radioStation.getRadioStationName());
            listStationLocation.setText(radioStation.getRadioStationLocation());
            listStationGenre.setText(radioStation.getRadioStationGenre());
        }

        @Override
        public void onClick(View v) {

            Log.d(LOG_TAG, getClass().getName() + ":onClick");
            Log.d(LOG_TAG, "Clicked on: "+ listStationName.getText() +" at position " +
                     getAdapterPosition());

            //Launch now playing activity and set wiewpager on clicked radio station
            Intent intent = new Intent(getApplicationContext(), NowPlayingActivity.class);
            intent.putExtra(EXTRA_LIST_TYPE, isRadioStationListPartial);
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

        /* Reference to radio station lab object */
        radioStationLab = RadioStationLab.get(this);

        //Load radio stations from web into database
        radioStationLab.getRadioStationsWeb(SERVER_URL);

        // Get radio stations from database
        radioStationsAll = radioStationLab.getRadioStationsDB();

        /* Register listener for database refresh */
        radioStationLab.setActivity(new RadioStationLab.RadioStationRefreshable() {
            @Override
            public void onRadioStationRefreshed() {
                Log.d(LOG_TAG, "Radio station database refreshed!");
                // Get radio stations from database
                radioStationsAll = radioStationLab.getRadioStationsDB();

                /* Inform Viewpager's adapter about change */
                recyclerView.getAdapter().notifyDataSetChanged();
            }
        });


        //Get references to views
        drawerLayout = (DrawerLayout) findViewById(R.id.navigation_drawer);
        drawerList = (ListView) findViewById(R.id.list_view);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        //Init nav drawer
        drawerList.setAdapter(new ArrayAdapter<String>(this,
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
                        break;

                    case 1:
                        //Favorite, do nothing for now
                        break;

                    case 2:
                        //Show pop stations
                        MainActivity.radioStationsPartial = new ArrayList<>();
                        RadioStationLab.filterRadioStationsByGenre(RadioStationLab.radioGenrePop);
                        isRadioStationListPartial = true;
                        radioStationAdapter.setRadioStationList(radioStationsPartial);
                        radioStationAdapter.notifyDataSetChanged();
                        break;

                    case 3:
                        //Show folk stations
                        MainActivity.radioStationsPartial = new ArrayList<>();
                        RadioStationLab.filterRadioStationsByGenre(RadioStationLab.radioGenreFolk);
                        isRadioStationListPartial = true;
                        radioStationAdapter.setRadioStationList(radioStationsPartial);
                        radioStationAdapter.notifyDataSetChanged();
                        break;

                    case 4:

                        //Show radio stations in Sarajevo
                        MainActivity.radioStationsPartial = new ArrayList<>();
                        RadioStationLab.filterRadioStationsByLocation("Sarajevo");
                        isRadioStationListPartial = true;
                        radioStationAdapter.setRadioStationList(radioStationsPartial);
                        radioStationAdapter.notifyDataSetChanged();

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
            }

            //Called on completely close state
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
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

        // Check network connection and alter user about it
        checkAndAlertNetworkConnection();

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


    /**
     * Checks network connection and alerts user about that
     */
    private void checkAndAlertNetworkConnection() {
        switch (NetworkUtil.checkNetworkConnection(this)) {

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


