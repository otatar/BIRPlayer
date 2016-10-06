package com.example.otatar.birplayer;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import java.util.ArrayList;


public class NowPlayingActivity extends AppCompatActivity {

    private static String SERVER_URL = "http://10.0.2.2:8081/get_stations";

    /* Array list that contains radio station objects */
    private ArrayList<RadioStation> radioStations;

    /* Reference to radio station lab object */
    private RadioStationLab radioStationLab;

    /* View Pager */
    private ViewPager viewPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);


        /* Reference to radio station lab object */
        radioStationLab = RadioStationLab.get(this);

         /* Register listener for database refresh */
        radioStationLab.setActivity(new RadioStationLab.RadioStationRefreshable() {
            @Override
            public void onRadioStationRefreshed() {
                Log.d("MAIN", "Refreshed!");
                // Get radio stations from database
                radioStations = radioStationLab.getRadioStationsDB();

                /* Inform Viewpager's adapter about change */
                viewPager.getAdapter().notifyDataSetChanged();
            }
        });

        /* View Pager */
        viewPager = (ViewPager) findViewById(R.id.viewpager);

       /*  Fragment fragment = new RadioPlayerFragment();
        Fragment fragment = new TestJsonFragment();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.frame1, fragment);
        //ft.addToBackStack(null);
        ft.commit(); */

        //Load radio stations from web into database
        radioStationLab.getRadioStationsWeb(SERVER_URL);

        // Get radio stations from database
        radioStations = radioStationLab.getRadioStationsDB();

        //Start service
        startService(RadioPlayerService.newStartIntent(this));


        // We need fragment manager
        FragmentManager fragmentManager = getSupportFragmentManager();

        /* Setting up ViewPager Adapter */
        viewPager.setAdapter(new FragmentStatePagerAdapter(fragmentManager) {

            @Override
            public Fragment getItem(int position) {
                RadioStation radioStation = radioStations.get(position);
                return RadioPlayerFragment.newInstance(radioStation);
            }

            @Override
            public int getCount() {
                return radioStations.size();
            }
        });


        /* Setting up OnPageChangeListener */
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

                Log.i("Main", "onPageSelected");

                /* When page changes stop media player if it is playing */
                Intent stopIntent = new Intent(NowPlayingActivity.this, RadioPlayerService.class);
                stopIntent.setAction(RadioPlayerFragment.ACTION_STOP);
                startService(stopIntent);

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });


    }
}
