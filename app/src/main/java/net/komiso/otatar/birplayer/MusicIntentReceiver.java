package net.komiso.otatar.birplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by o.tatar on 06-Oct-16.
 * Simple broadcast receiver listening for audio that becomes noisy (e.g unplugging headphones)
 */
public class MusicIntentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent.getAction().equals(
                android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {

            // Signal RadioPlayerService to pause playback
            Intent stopIntent = new Intent(ctx, RadioPlayerService.class);
            stopIntent.setAction(RadioPlayerFragment.ACTION_PAUSE);

        }
    }

}
