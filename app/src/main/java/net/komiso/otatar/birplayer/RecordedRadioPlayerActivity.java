package net.komiso.otatar.birplayer;

import android.app.Activity;
import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import net.komiso.otatar.biplayer.R;

import java.io.File;
import java.util.ArrayList;

public class RecordedRadioPlayerActivity extends AppCompatActivity {

    //Log TAG
    private static final String LOG_TAG = "RecRadioPlayerLog";

    public static String EXTRA_POSITION = "extra_position";
    public static String EXTRA_REC_LIST = "extra_rec_list";

    //Search view
    private SearchView searchView;

    //Toolbar
    private Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorded_radio_player);

        // Start service, if it has't been started
        startService(RadioPlayerService.newStartIntent(this));

        //From the top, create toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar1);
        toolbar.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.main_color_drawable, null));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Back pressed, just finish!");

                Intent intent = new Intent(RecordedRadioPlayerActivity.this, RadioPlayerService.class);
                intent.setAction(RadioPlayerFragment.ACTION_STOP);
                startService(intent);
                finish();
            }
        });

        Intent intent = getIntent();

        Fragment fragment = RecordedRadioPlayerFragment.newInstance(intent.getIntExtra(EXTRA_POSITION, 0), (ArrayList<File>) intent.getSerializableExtra(EXTRA_REC_LIST));
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.frame_layout1, fragment);
        ft.setCustomAnimations(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
        ft.commit();

    }
}
