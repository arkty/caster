package ga.arkty.caster;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;

import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumer;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private VideoCastManager castManager;
    private VideoCastConsumer castConsumer;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        castManager = VideoCastManager.getInstance();
        castConsumer = new SingleVideoCastConsumer(this,
                "https://goo.gl/E80S9M",
                "Jet Packs Was Yes", "Periphery",
                "http://fugostudios.com/wp-content/uploads/2012/02/periphery720p-600x338.jpg", "video/mp4") {
            @Override
            public void onPlaybackFinished() {
                disconnectDevice();
            }

            @Override
            public void onQueueLoad(MediaQueueItem[] items, int startIndex,
                                    int repeatMode, JSONObject customData)
                    throws TransientNetworkDisconnectionException, NoConnectionException {

                castManager.queueLoad(items, startIndex, repeatMode, customData);
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        castManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        return castManager.onDispatchVolumeKeyEvent(event, 0.05)
                || super.dispatchKeyEvent(event);
    }

    @Override
    protected void onResume() {
        castManager = VideoCastManager.getInstance();
        if (castManager != null) {
            castManager.addVideoCastConsumer(castConsumer);
            castManager.incrementUiCounter();
        }

        super.onResume();
    }

    @Override
    protected void onPause() {
        castManager.decrementUiCounter();
        castManager.removeVideoCastConsumer(castConsumer);
        super.onPause();
    }

    private void disconnectDevice() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
               castManager.disconnect();
            }
        },500);
    }
}
