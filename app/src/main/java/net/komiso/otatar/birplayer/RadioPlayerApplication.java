package net.komiso.otatar.birplayer;

import android.app.Application;

/**
 * Created by o.tatar on 31-Oct-16.
 * Class used for tracking NowPlayingActivity visibility
 */
public class RadioPlayerApplication extends Application {

    private static boolean activityVisible;

    public static boolean isActivityVisible() {
        return activityVisible;
    }

    public static void activityResumed() {
        activityVisible = true;
    }

    public static void activityPaused() {
        activityVisible = false;
    }

}
