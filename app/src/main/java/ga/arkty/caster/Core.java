package ga.arkty.caster;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.MediaRouteChooserDialog;
import android.support.v7.app.MediaRouteChooserDialogFragment;
import android.support.v7.app.MediaRouteControllerDialog;
import android.support.v7.app.MediaRouteControllerDialogFragment;
import android.support.v7.app.MediaRouteDialogFactory;

import com.google.android.libraries.cast.companionlibrary.cast.CastConfiguration;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;

import java.util.Locale;

/**
 * Author: Andrey Khitryy
 * Email: andrey.khitryy@gmail.com
 */

public class Core extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        CastConfiguration options = new CastConfiguration.Builder("CC1AD845")
                .enableAutoReconnect()
                .enableCaptionManagement()
                .enableDebug()
                .enableLockScreen()
                .enableNotification()
                .enableWifiReconnection()
                .setMediaRouteDialogFactory(new MediaRouteDialogFactory(){

                    @Override
                    public MediaRouteChooserDialogFragment onCreateChooserDialogFragment() {
                        return new MediaRouteChooserDialogFragment() {
                            @Override
                            public MediaRouteChooserDialog onCreateChooserDialog(Context context, Bundle savedInstanceState) {
                                return new MediaRouteChooserDialog(context, R.style.Theme_MediaRouter_Light);
                            }
                        };
                    }

                    @NonNull
                    @Override
                    public MediaRouteControllerDialogFragment onCreateControllerDialogFragment() {
                        return new MediaRouteControllerDialogFragment(){
                            @Override
                            public MediaRouteControllerDialog onCreateControllerDialog(Context context, Bundle savedInstanceState) {
                                return new MediaRouteControllerDialog(context, R.style.Theme_MediaRouter_Light);
                            }
                        };
                    }
                })
                .setCastControllerImmersive(true)
                .setLaunchOptions(false, Locale.getDefault())
                .setNextPrevVisibilityPolicy(CastConfiguration.NEXT_PREV_VISIBILITY_POLICY_DISABLED)
                .addNotificationAction(CastConfiguration.NOTIFICATION_ACTION_REWIND, false)
                .addNotificationAction(CastConfiguration.NOTIFICATION_ACTION_PLAY_PAUSE, true)
                .addNotificationAction(CastConfiguration.NOTIFICATION_ACTION_DISCONNECT, true)
                .setForwardStep(10)
                .build();
        VideoCastManager.initialize(this, options);
    }
}
