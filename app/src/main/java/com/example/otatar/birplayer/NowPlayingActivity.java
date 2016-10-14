package com.example.otatar.birplayer;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import java.util.ArrayList;


public class NowPlayingActivity extends AppCompatActivity {

    /* View Pager */
    private ViewPager viewPager;

    private ArrayList<RadioStation> radioStations = new ArrayList<>();

    /* Starting radio station position */
    private int radioStationPosition;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);

        /* View Pager */
        viewPager = (ViewPager) findViewById(R.id.viewpager);

       /*  Fragment fragment = new RadioPlayerFragment();
        Fragment fragment = new TestJsonFragment();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.frame1, fragment);
        //ft.addToBackStack(null);
        ft.commit(); */

        /* List Type */
        if (getIntent().getBooleanExtra(MainActivity.EXTRA_LIST_TYPE, false)) {
            radioStations = MainActivity.radioStationsPartial;
        } else {
            radioStations = MainActivity.radioStationsAll;
        }

        /* Radio station position */
        radioStationPosition = getIntent().getIntExtra(MainActivity.EXTRA_RADIO_STATION_POSITION, 0);


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

        //Set the start position
        viewPager.setCurrentItem(radioStationPosition);

        //Show button on toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

    }

    /**
     * When back button is pressed, go to previous activity
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            finish();

            //Make new activity slide in
            overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
        }

        return super.onOptionsItemSelected(item);
    }

}
