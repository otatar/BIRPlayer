package com.example.otatar.birplayer; /**
 * Created by o.tatar on 08-Dec-16.
 * Activity only to show splash screen during main activity startup
 */

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.otatar.birplayer.Main2Activity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, Main2Activity.class);
        intent.setAction(Intent.ACTION_MAIN);
        startActivity(intent);
        finish();
    }
}