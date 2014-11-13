package android.support.v17.leanback.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

/**
 * A helper class for implementing a glue layer between a
 * {@link PlaybackOverlayFragment} and a
 * {@link android.support.v4.media.session.MediaControllerCompat}.
 */
public abstract class MediaControllerGlue extends PlaybackControlGlue {
    private static final String TAG = "MediaControllerGlue";
    private static final boolean DEBUG = false;

    private MediaControllerCompat mMediaController;

    private final MediaControllerCompat.Callback mCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (DEBUG) Log.v(TAG, "onMetadataChanged");
            MediaControllerGlue.this.onMetadataChanged();
        }
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            if (DEBUG) Log.v(TAG, "onPlaybackStateChanged");
            onStateChanged();
        }
        @Override
        public void onSessionDestroyed() {
            if (DEBUG) Log.v(TAG, "onSessionDestroyed");
            mMediaController = null;
        }
        @Override
        public void onSessionEvent(String event, Bundle extras) {
            if (DEBUG) Log.v(TAG, "onSessionEvent");
        }
    };

    /**
     * Constructor for the glue.
     *
     * <p>The {@link PlaybackOverlayFragment} must be passed in.
     * A {@link android.support.v17.leanback.widget.OnItemViewClickedListener} and
     * {@link PlaybackOverlayFragment.InputEventHandler}
     * will be set on the fragment.
     * </p>
     *
     * @param context
     * @param fragment
     * @param seekSpeeds Array of seek speeds for fast forward and rewind.
     */
    public MediaControllerGlue(Context context,
                               PlaybackOverlayFragment fragment,
                               int[] seekSpeeds) {
        super(context, fragment, seekSpeeds);
    }

    /**
     * Constructor for the glue.
     *
     * <p>The {@link PlaybackOverlayFragment} must be passed in.
     * A {@link android.support.v17.leanback.widget.OnItemViewClickedListener} and
     * {@link PlaybackOverlayFragment.InputEventHandler}
     * will be set on the fragment.
     * </p>
     *
     * @param context
     * @param fragment
     * @param fastForwardSpeeds Array of seek speeds for fast forward.
     * @param rewindSpeeds Array of seek speeds for rewind.
     */
    public MediaControllerGlue(Context context,
                               PlaybackOverlayFragment fragment,
                               int[] fastForwardSpeeds,
                               int[] rewindSpeeds) {
        super(context, fragment, fastForwardSpeeds, rewindSpeeds);
    }

    /**
     * Attaches to the given media controller.
     */
    public void attachToMediaController(MediaControllerCompat mediaController) {
        if (mediaController != mMediaController) {
            if (DEBUG) Log.v(TAG, "New media controller " + mediaController);
            detach();
            mMediaController = mediaController;
            if (mMediaController != null) {
                mMediaController.registerCallback(mCallback);
            }
            onMetadataChanged();
            onStateChanged();
        }
    }

    /**
     * Detaches from the media controller.  Must be called when the object is no longer
     * needed.
     */
    public void detach() {
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mCallback);
        }
        mMediaController = null;
    }

    /**
     * Returns the media controller currently attached.
     */
    public final MediaControllerCompat getMediaController() {
        return mMediaController;
    }

    @Override
    public boolean hasValidMedia() {
        return mMediaController != null && mMediaController.getMetadata() != null;
    }

    @Override
    public boolean isMediaPlaying() {
        return mMediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING;
    }

    @Override
    public int getCurrentSpeedId() {
        int speed = (int) mMediaController.getPlaybackState().getPlaybackSpeed();
        if (speed == 0) {
            return PLAYBACK_SPEED_PAUSED;
        } else if (speed == 1) {
            return PLAYBACK_SPEED_NORMAL;
        } else if (speed > 0) {
            int[] seekSpeeds = getFastForwardSpeeds();
            for (int index = 0; index < seekSpeeds.length; index++) {
                if (speed == seekSpeeds[index]) {
                    return PLAYBACK_SPEED_FAST_L0 + index;
                }
            }
        } else {
            int[] seekSpeeds = getRewindSpeeds();
            for (int index = 0; index < seekSpeeds.length; index++) {
                if (-speed == seekSpeeds[index]) {
                    return -PLAYBACK_SPEED_FAST_L0 - index;
                }
            }
        }
        Log.w(TAG, "Couldn't find index for speed " + speed);
        return PLAYBACK_SPEED_INVALID;
    }

    @Override
    public CharSequence getMediaTitle() {
        return mMediaController.getMetadata().getDescription().getTitle();
    }

    @Override
    public CharSequence getMediaSubtitle() {
        return mMediaController.getMetadata().getDescription().getSubtitle();
    }

    @Override
    public int getMediaDuration() {
        return (int) mMediaController.getMetadata().getLong(
                MediaMetadataCompat.METADATA_KEY_DURATION);
    }

    @Override
    public int getCurrentPosition() {
        return (int) mMediaController.getPlaybackState().getPosition();
    }

    @Override
    public Drawable getMediaArt() {
        Bitmap bitmap = mMediaController.getMetadata().getDescription().getIconBitmap();
        return bitmap == null ? null : new BitmapDrawable(getContext().getResources(), bitmap);
    }

    @Override
    public long getSupportedActions() {
        long result = 0;
        long actions = mMediaController.getPlaybackState().getActions();
        if ((actions & PlaybackStateCompat.ACTION_PLAY_PAUSE) != 0) {
            result |= ACTION_PLAY_PAUSE;
        }
        if ((actions & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
            result |= ACTION_SKIP_TO_NEXT;
        }
        if ((actions & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
            result |= ACTION_SKIP_TO_PREVIOUS;
        }
        if ((actions & PlaybackStateCompat.ACTION_FAST_FORWARD) != 0) {
            result |= ACTION_FAST_FORWARD;
        }
        if ((actions & PlaybackStateCompat.ACTION_REWIND) != 0) {
            result |= ACTION_REWIND;
        }
        return result;
    }

    @Override
    protected void startPlayback(int speed) {
        if (DEBUG) Log.v(TAG, "startPlayback speed " + speed);
        if (speed == PLAYBACK_SPEED_NORMAL) {
            mMediaController.getTransportControls().play();
        } else if (speed > 0) {
            mMediaController.getTransportControls().fastForward();
        } else {
            mMediaController.getTransportControls().rewind();
        }
    }

    @Override
    protected void pausePlayback() {
        if (DEBUG) Log.v(TAG, "pausePlayback");
        mMediaController.getTransportControls().pause();
    }

    @Override
    protected void skipToNext() {
        if (DEBUG) Log.v(TAG, "skipToNext");
        mMediaController.getTransportControls().skipToNext();
    }

    @Override
    protected void skipToPrevious() {
        if (DEBUG) Log.v(TAG, "skipToPrevious");
        mMediaController.getTransportControls().skipToPrevious();
    }
}
