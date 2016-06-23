package ga.arkty.caster;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumer;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.android.libraries.cast.companionlibrary.widgets.IntroductoryOverlay;

import org.json.JSONObject;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";

    private VideoCastManager castManager;
    private VideoCastConsumer castConsumer;
    private Toolbar toolbar;

    private IntroductoryOverlay mOverlay;
    private MenuItem mMediaRouterMenuItem;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
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

            @Override
            public void onCastAvailabilityChanged(boolean castPresent) {
                if (castPresent) {
                    showOverlay();
                }
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        mMediaRouterMenuItem = castManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        return castManager.onDispatchVolumeKeyEvent(event, 0.05)
                || super.dispatchKeyEvent(event);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume() was called");
        castManager = VideoCastManager.getInstance();
        if (null != castManager) {
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

    private void showOverlay() {
        if(mOverlay != null) {
            mOverlay.remove();
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mMediaRouterMenuItem.isVisible()) {
                    mOverlay = new IntroductoryOverlay.Builder(MainActivity.this)
                            .setMenuItem(mMediaRouterMenuItem)
                            .setTitleText(R.string.app_name)
                            .setSingleTime()
                            .setOnDismissed(new IntroductoryOverlay.OnOverlayDismissedListener() {
                                @Override
                                public void onOverlayDismissed() {
                                    Log.d(TAG, "overlay is dismissed");
                                    mOverlay = null;
                                }
                            })
                            .build();
                    mOverlay.show();
                }
            }
        }, 1000);
    }
}
