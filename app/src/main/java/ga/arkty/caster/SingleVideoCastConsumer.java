package ga.arkty.caster;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.common.images.WebImage;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;

import org.json.JSONObject;

import java.util.List;

/**
 * Author: Andrey Khitryy
 * Email: andrey.khitryy@gmail.com
 */

public abstract class SingleVideoCastConsumer extends VideoCastConsumerImpl {

    private AppCompatActivity activity;

    private final String videoUrl;
    private final String title;
    private final String subtitle;
    private final String imageUrl;
    private final String contentType;

    public SingleVideoCastConsumer(AppCompatActivity activity, String videoUrl, String title, String subtitle, String imageUrl, String contentType) {
        this.activity = activity;
        this.videoUrl = videoUrl;
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.contentType = contentType;
    }

    public abstract void onPlaybackFinished();
    public abstract void onQueueLoad(final MediaQueueItem[] items, final int startIndex, final int repeatMode,
                                     final JSONObject customData) throws TransientNetworkDisconnectionException, NoConnectionException;

    @Override
    public void onMediaQueueUpdated(List<MediaQueueItem> queueItems, MediaQueueItem item, int repeatMode, boolean shuffle) {
        if(queueItems != null && queueItems.size() == 0) {
            onPlaybackFinished();
        }
    }

    @Override
    public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId,
                                       boolean wasLaunched) {
        activity.invalidateOptionsMenu();
        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);

        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, subtitle);
        movieMetadata.putString(MediaMetadata.KEY_TITLE, title);
        movieMetadata.addImage(new WebImage(Uri.parse(imageUrl)));

        MediaInfo info = new MediaInfo.Builder(videoUrl)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(contentType)
                .setMetadata(movieMetadata)
                .build();

        MediaQueueItem item = new MediaQueueItem.Builder(info).build();
        try {
            onQueueLoad(new MediaQueueItem[]{item}, 0, MediaStatus.REPEAT_MODE_REPEAT_OFF, null);
        } catch (TransientNetworkDisconnectionException e) {
            e.printStackTrace();
        } catch (NoConnectionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisconnected() {
        activity.invalidateOptionsMenu();
    }
}
