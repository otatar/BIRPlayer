package net.komiso.otatar.birplayer;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import net.komiso.otatar.biplayer.R;

import java.io.File;
import java.util.ArrayList;

public class Main2Activity extends AppCompatActivity implements RadioStationListFragment.OnRadioStationSelectedListener,
                                                                RadioPlayerFragment.OnRadioStationChangedListener,
                                                                RadioStationListFragment.OnRadioStationSelectedAndPlayListener,
                                                                RadioStationListFragment.OnRecordingPlayListener,
                                                                RadioStationListFragment.OnRecordingSelectedListener {

    //Log TAG
    private static final String LOG_TAG = "Main2Activity";

    //Bundle keys
    public static String SELECTED_RADIO_STATION = "selected_position";
    public static String REC_FILEPATH = "rec_filepath";

    private static final int REQUEST_WRITE_STORAGE = 112;

    //Whether we have permission granted for write storage
    public static boolean storagePerm = false;

    // Whether there is a internet connection.
    public static int internetConnection;

    private static boolean radioStationListFragmentInForeground;

    //Drawer Layout
    private DrawerLayout drawerLayout;

    //Drawer toggle
    private StaticActionBarDrawerToggle actionBarDrawerToggle;

    //List View
    private ListView drawerList;

    //Tab layout
    private TabLayout tabs;

    //Search view
    private SearchView searchView;

    //Toolbar
    private Toolbar toolbar;

    //RadioStationFragment
    private RadioStationListFragment currentFragment;

    //Selected radio station
    private static int selectedRadioStationPosition = -1;

    //Selected radio station list
    private static ArrayList<RadioStation> selectedRadioStationList = new ArrayList<>();


    /**
     * Implementation of interface OnRadioStationSelected
     *
     * @param position Selected position
     */
    @Override
    public void onRadioStationSelected(int position, ArrayList<RadioStation> radioStationList, Boolean startPlayer) {

        if (!startPlayer) {
            selectedRadioStationPosition = position;
            selectedRadioStationList.clear();
            selectedRadioStationList.addAll(radioStationList);
            return;
        }

        Log.d(LOG_TAG, "onRadioStationSelected");
        Log.d(LOG_TAG, "Selected radio station: " + radioStationList.get(position).getRadioStationName());

        //If we clicked on different station from one currently playing
        if (selectedRadioStationPosition != position) {

            selectedRadioStationPosition = position;
            selectedRadioStationList.clear();
            selectedRadioStationList.addAll(radioStationList);

         /* Stop media player if it is playing */
            Intent stopIntent = new Intent(this, RadioPlayerService.class);
            stopIntent.setAction(RadioPlayerFragment.ACTION_STOP);
            startService(stopIntent);
        }

        //Start RadioPlayerFragment
        radioStationListFragmentInForeground = false;
        Fragment fragment = RadioPlayerFragment.newInstance(position, selectedRadioStationList);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.frame_layout, fragment);
        ft.setCustomAnimations(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
        ft.commit();

        //Change tab
        changeTab();
    }

    /**
     * Implementation of interface OnRadioStationChanged
     * @param position Changed position
     */
    @Override
    public void onRadioStationChanged(int position) {

        Log.d(LOG_TAG, "onRadioStationChanged");
        Log.d(LOG_TAG, "Changed to radio station: " + selectedRadioStationList.get(position).getRadioStationName());

        selectedRadioStationPosition = position;

    }


    @Override
    public void onRadioStationSelectedAndPlay(RadioStation radioStation, Boolean stopRadioStation) {

        Log.d(LOG_TAG, "onRadioStationSelectedAndPlay");

        if (!stopRadioStation) {

            //First, stop previous radio station
            Intent stopIntent = new Intent(this, RadioPlayerService.class);
            stopIntent.setAction(RadioPlayerFragment.ACTION_STOP);
            startService(stopIntent);

            //Play
            Intent playIntent = new Intent(this, RadioPlayerService.class);
            playIntent.setAction(RadioPlayerFragment.ACTION_INIT_AND_START);
            playIntent.putExtra(SELECTED_RADIO_STATION, radioStation);
            startService(playIntent);

        } else {

            //Stop
            Intent stopIntent = new Intent(this, RadioPlayerService.class);
            stopIntent.setAction(RadioPlayerFragment.ACTION_STOP);
            startService(stopIntent);

        }

    }


    @Override
    public void onRecordingPlay(String recFilePath, Boolean stopRadioStation) {

        Log.d(LOG_TAG, "onRecordingPlay");

        if (!stopRadioStation) {

            //First, stop previous radio station
           /* Intent stopIntent = new Intent(this, RadioPlayerService.class);
            stopIntent.setAction(RadioPlayerFragment.ACTION_STOP);
            startService(stopIntent);*/

            //Play
            Intent playIntent = new Intent(this, RadioPlayerService.class);
            playIntent.setAction(RadioPlayerFragment.ACTION_REC_START);
            playIntent.putExtra(REC_FILEPATH, recFilePath);
            startService(playIntent);

        } else {

            //Stop
            Intent stopIntent = new Intent(this, RadioPlayerService.class);
            stopIntent.setAction(RadioPlayerFragment.ACTION_PAUSE_REC);
            startService(stopIntent);

        }

    }


    @Override
    public void onRecordingSelected(int position, ArrayList<File> recordedRadioStationList) {

        //Start recorded radio player activity
        Intent intent = new Intent(this, RecordedRadioPlayerActivity.class);
        intent.putExtra(RecordedRadioPlayerActivity.EXTRA_POSITION, position);
        intent.putExtra(RecordedRadioPlayerActivity.EXTRA_REC_LIST, recordedRadioStationList);

        startActivity(intent);


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        //We are on API > 22 we need permission for data storage
        int MyVersion = Build.VERSION.SDK_INT;
        if (MyVersion > Build.VERSION_CODES.LOLLIPOP_MR1) {

            //We need to ask for permission*******************************
            int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (permission != PackageManager.PERMISSION_GRANTED) {
                Log.i(LOG_TAG, "Permission to record denied");

                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(this.getString(R.string.storage_permission))
                            .setTitle(this.getString(R.string.storage_permission_title));

                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int id) {
                            Log.i(LOG_TAG, "Clicked");
                            makeRequest();
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();

                } else {
                    makeRequest();
                }

            } else {

                //We already have the permission
                storagePerm = true;
            }


        }


        // Start service, if it has't been started
        startService(RadioPlayerService.newStartIntent(this));

        /* Wiring up things */
        /* Find tabs*/
        tabs = (TabLayout) findViewById(R.id.tabs);
        //Get references to views
        drawerLayout = (DrawerLayout) findViewById(R.id.navigation_drawer);
        drawerList = (ListView) findViewById(R.id.list_view);

        //From the top, create toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.main_color_drawable, null));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

         //Init Navigation Drawer

        //Show list header
        View drawerHeader = getLayoutInflater().inflate(R.layout.drawer_header_view, null);
        drawerList.addHeaderView(drawerHeader);

        // Drawer list icons
        int[] arrayIcons = {
                R.drawable.list_all_icon,
                R.drawable.list_favorite_icon,
                R.drawable.list_pop_icon,
                R.drawable.list_folk_icon,
                R.drawable.list_sarajevo_icon,
                R.drawable.list_recorded_icon
        };
        drawerList.setAdapter(new CustomDrawerListAdapter(this, getResources().getStringArray(R.array.drawer_items),
                arrayIcons));

        //Set listener for clicks on list
        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Check current fragment
                try {
                    currentFragment = (RadioStationListFragment) getSupportFragmentManager().findFragmentById(R.id.frame_layout);

                } catch (ClassCastException e) {
                    Log.d(LOG_TAG, "RadioStationList not loaded, load it!");
                    changeTab();
                    getSupportFragmentManager().executePendingTransactions();
                    currentFragment = (RadioStationListFragment) getSupportFragmentManager().findFragmentByTag("visible_fragment");
                }

                //Close drawer
                drawerLayout.closeDrawer(drawerList);

                //Filter list currentFragment (we have a header and it is index 0)
                Log.d(LOG_TAG, "Clicked on position: " + (position - 1));
               currentFragment.filterRadioStations(position - 1);
            }

        });

        actionBarDrawerToggle = new StaticActionBarDrawerToggle(drawerLayout, toolbar, R.drawable.drawer_button_1,
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

        //Create tabs
        tabs.addTab(tabs.newTab().setText(R.string.tab_radio_station_list), true);
        tabs.addTab(tabs.newTab().setText(R.string.tab_now_playing));


        // OnTabSelectedListener
        tabs.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                Log.d(LOG_TAG, "onTabSelected()");
                Log.d(LOG_TAG, "Selected: " + tab.getText() + " tab " + tab.getPosition());

                if (tab.getPosition() == 0) {
                    //Start RadioStationListFragment
                    radioStationListFragmentInForeground = true;
                    Fragment fragment;
                    if(selectedRadioStationList.size() == 0) {
                        fragment = RadioStationListFragment.newInstance(false, null);
                    } else {
                        fragment = RadioStationListFragment.newInstance(false, selectedRadioStationList.get(selectedRadioStationPosition));
                    }

                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.frame_layout, fragment, "visible_fragment");
                    ft.commit();

                } else if (tab.getPosition() == 1) {

                    if (selectedRadioStationPosition == -1) {
                        //No radio station selected, show dialog
                        Log.d(LOG_TAG, "No radio station selected");
                        showAlertDialog(getResources().getString(R.string.no_radio_station_selected));
                        changeTab();

                    } else {
                        //Start RadioPlayerFragment
                        radioStationListFragmentInForeground = false;
                        Fragment fragment = RadioPlayerFragment.newInstance(selectedRadioStationPosition, selectedRadioStationList);
                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        ft.replace(R.id.frame_layout, fragment, "visible_fragment");
                        ft.commit();

                    }

                }

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        //Get intent that started the activity
        Intent intent = getIntent();
        Log.d(LOG_TAG, "Intent action: " + intent.getAction());

        if (intent.getAction().equals(Intent.ACTION_RUN)) {

            //We have been started from notification, so fire up RadioPlayerFragment
            Log.d(LOG_TAG, "Intent Position: " + selectedRadioStationPosition);
            changeTab();


        } else if (intent.getAction().equals(Intent.ACTION_MAIN) && savedInstanceState != null) {

            //Restarted due to change in configuration (screen rotation)
            if (radioStationListFragmentInForeground) {

                Fragment fragment;

                if (selectedRadioStationPosition != -1) {
                    fragment = RadioStationListFragment.newInstance(false, selectedRadioStationList.get(selectedRadioStationPosition));
                } else {
                    fragment = RadioStationListFragment.newInstance(false, null);
                }

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.add(R.id.frame_layout, fragment);
                ft.commit();
            } else {
                changeTab();
            }

        } else {

            //Whe have been started by the launcher, so start the RadioStationListFragment

            // Check network connection and alter user about it
            checkAndAlertNetworkConnection();

            radioStationListFragmentInForeground = true;

            Fragment fragment = RadioStationListFragment.newInstance(true, null);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.frame_layout, fragment);
            ft.commit();
        }

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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);

        final MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(LOG_TAG, "onQueryTextSubmit: " + query);

                //Check current fragment
                try {
                    currentFragment = (RadioStationListFragment) getSupportFragmentManager().findFragmentById(R.id.frame_layout);

                } catch (ClassCastException e) {
                    Log.d(LOG_TAG, "RadioStationList not loaded, load it!");
                    changeTab();
                    getSupportFragmentManager().executePendingTransactions();
                    currentFragment = (RadioStationListFragment) getSupportFragmentManager().findFragmentByTag("visible_fragment");

                }

                currentFragment.filterRadioStationByName(query);
                searchView.clearFocus();

                return true;
            }


            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(LOG_TAG, "onQueryTextChange: " + newText);
                //Check current fragment
                try {
                    currentFragment = (RadioStationListFragment) getSupportFragmentManager().findFragmentById(R.id.frame_layout);

                } catch (ClassCastException e) {
                    Log.d(LOG_TAG, "RadioStationList not loaded, load it!");
                    changeTab();
                    getSupportFragmentManager().executePendingTransactions();
                    currentFragment = (RadioStationListFragment) getSupportFragmentManager().findFragmentByTag("visible_fragment");

                }

                currentFragment.filterRadioStationByName(newText);

                return true;
            }

        });

        return super.onCreateOptionsMenu(menu);

    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        searchView.clearFocus();
        return super.dispatchTouchEvent(ev);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy()");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(LOG_TAG, "onSaveInstanceState()");

    }


    /**
     * Function to make permission request for DATA STORAGE
     */
    protected void makeRequest() {

        Log.d(LOG_TAG, "makeRequest()");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
    }


    /**
     * Function to take care of permission  request response
     * @param requestCode
     * @param permissions
     * @param grantResults
     */


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {

                if (grantResults.length == 0
                        || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED) {

                    Log.d(LOG_TAG, "Permission has been denied by user");
                    storagePerm = false;

                } else {

                    Log.d(LOG_TAG, "Permission has been granted by user");
                    storagePerm = true;

                }

                return;
            }
        }
    }


    /**
     * Setting activity's subtitle
     * @param subtitle Activity subtitle
     */
    public void setActivitySubtitle(String subtitle) {

        Log.d(LOG_TAG, "setActivitySubtitle");

        toolbar.setSubtitle(subtitle);

    }



    /**
     * Checks network connection and alerts user about that
     */
    private void checkAndAlertNetworkConnection() {
        switch (Main2Activity.internetConnection = NetworkUtil.checkNetworkConnection(this)) {

            case NetworkUtil.TYPE_WIFI:
                //Do nothing
                break;

            case NetworkUtil.TYPE_MOBILE:
                //Alert user about connection with a dialog
                showAlertDialog(getResources().getString(R.string.mobile_connection_hint));
                break;

            case NetworkUtil.TYPE_NOT_CONNECTED:
                //Alert user about connection with a dialog
                showAlertDialog(getResources().getString(R.string.no_connection));

                break;

        }
    }

    public static int getInternetConnection() {
        return internetConnection;
    }


    /**
     * Change current tab selected
     */
    private void changeTab() {

        if (tabs.getSelectedTabPosition() == 0) {
            TabLayout.Tab tab = tabs.getTabAt(1);
            tab.select();

        } else if (tabs.getSelectedTabPosition() == 1) {
            TabLayout.Tab tab = tabs.getTabAt(0);
            tab.select();
        }

    }


    private void showAlertDialog(String message) {

        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage(message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }
                );

        AlertDialog dialog1 = builder1.create();
        dialog1.show();

        Button dialogBtn =  dialog1.getButton(DialogInterface.BUTTON_POSITIVE);
        dialogBtn.setTextColor(Color.BLUE);
    }


}
