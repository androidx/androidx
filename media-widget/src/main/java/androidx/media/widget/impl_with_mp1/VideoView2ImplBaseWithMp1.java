/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.media.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaControllerCompat.PlaybackInfo;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.media.AudioAttributesCompat;
import androidx.media.DataSourceDesc;
import androidx.media.MediaItem2;
import androidx.media.MediaMetadata2;
import androidx.media.SessionToken2;
import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaItemStatus;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;
import androidx.palette.graphics.Palette;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Base implementation of VideoView2.
 */
@RequiresApi(21) // TODO correct minSdk API use incompatibilities and remove before release.
class VideoView2ImplBaseWithMp1
        implements VideoView2Impl, VideoViewInterfaceWithMp1.SurfaceListener {
    private static final String TAG = "VideoView2ImplBase_1";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private static final long DEFAULT_SHOW_CONTROLLER_INTERVAL_MS = 2000;

    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    private static final int INVALID_TRACK_INDEX = -1;
    private static final float INVALID_SPEED = 0f;

    private static final int SIZE_TYPE_EMBEDDED = 0;
    private static final int SIZE_TYPE_FULL = 1;
    private static final int SIZE_TYPE_MINIMAL = 2;

    private AccessibilityManager mAccessibilityManager;
    private AudioManager mAudioManager;
    private AudioAttributes mAudioAttributes;
    private int mAudioFocusType = AudioManager.AUDIOFOCUS_GAIN; // legacy focus gain
    private boolean mAudioFocused = false;

    private Pair<Executor, VideoView2.OnCustomActionListener> mCustomActionListenerRecord;
    private VideoView2.OnViewTypeChangedListener mViewTypeChangedListener;

    private VideoViewInterfaceWithMp1 mCurrentView;
    private VideoTextureViewWithMp1 mTextureView;
    private VideoSurfaceViewWithMp1 mSurfaceView;

    protected MediaPlayer mMediaPlayer;
    private DataSourceDesc mDsd;
    private Uri mUri;
    private Map<String, String> mHeaders;
    private MediaControlView2 mMediaControlView;
    protected MediaSessionCompat mMediaSession;
    private MediaControllerCompat mMediaController;
    private MediaMetadata2 mMediaMetadata;
    private MediaMetadataRetriever mRetriever;
    private boolean mNeedUpdateMediaType;
    private Bundle mMediaTypeData;
    private String mTitle;

    private WindowManager mManager;
    private Resources mResources;
    private View mMusicView;
    private Drawable mMusicAlbumDrawable;
    private String mMusicTitleText;
    private String mMusicArtistText;
    private int mPrevWidth;
    private int mPrevHeight;
    private int mDominantColor;
    private int mSizeType;

    private PlaybackStateCompat.Builder mStateBuilder;
    private List<PlaybackStateCompat.CustomAction> mCustomActionList;

    private int mTargetState = STATE_IDLE;
    private int mCurrentState = STATE_IDLE;
    private int mCurrentBufferPercentage;
    private long mSeekWhenPrepared;  // recording the seek position while preparing

    private int mVideoWidth;
    private int mVideoHeight;

    protected ArrayList<Integer> mVideoTrackIndices;
    protected ArrayList<Integer> mAudioTrackIndices;

    // selected video/audio/subtitle track index as MediaPlayer returns
    protected int mSelectedVideoTrackIndex;
    protected int mSelectedAudioTrackIndex;

    private float mSpeed;
    private float mFallbackSpeed;  // keep the original speed before 'pause' is called.
    private float mVolumeLevelFloat;
    private int mVolumeLevel;
    protected VideoView2 mInstance;

    private long mShowControllerIntervalMs;

    private MediaRouter mMediaRouter;
    private MediaRouteSelector mRouteSelector;
    private MediaRouter.RouteInfo mRoute;
    private RoutePlayer mRoutePlayer;

    private final MediaRouter.Callback mRouterCallback = new MediaRouter.Callback() {
        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            if (route.supportsControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)) {
                // Stop local playback (if necessary)
                resetPlayer();
                mRoute = route;
                mRoutePlayer = new RoutePlayer(mInstance.getContext(), route);
                mRoutePlayer.setPlayerEventCallback(new RoutePlayer.PlayerEventCallback() {
                    @Override
                    public void onPlayerStateChanged(MediaItemStatus itemStatus) {
                        PlaybackStateCompat.Builder psBuilder = new PlaybackStateCompat.Builder();
                        psBuilder.setActions(RoutePlayer.PLAYBACK_ACTIONS);
                        long position = itemStatus.getContentPosition();
                        switch (itemStatus.getPlaybackState()) {
                            case MediaItemStatus.PLAYBACK_STATE_PENDING:
                                psBuilder.setState(PlaybackStateCompat.STATE_NONE, position, 0);
                                mCurrentState = STATE_IDLE;
                                break;
                            case MediaItemStatus.PLAYBACK_STATE_PLAYING:
                                psBuilder.setState(PlaybackStateCompat.STATE_PLAYING, position, 1);
                                mCurrentState = STATE_PLAYING;
                                break;
                            case MediaItemStatus.PLAYBACK_STATE_PAUSED:
                                psBuilder.setState(PlaybackStateCompat.STATE_PAUSED, position, 0);
                                mCurrentState = STATE_PAUSED;
                                break;
                            case MediaItemStatus.PLAYBACK_STATE_BUFFERING:
                                psBuilder.setState(
                                        PlaybackStateCompat.STATE_BUFFERING, position, 0);
                                mCurrentState = STATE_PAUSED;
                                break;
                            case MediaItemStatus.PLAYBACK_STATE_FINISHED:
                                psBuilder.setState(PlaybackStateCompat.STATE_STOPPED, position, 0);
                                mCurrentState = STATE_PLAYBACK_COMPLETED;
                                break;
                        }

                        PlaybackStateCompat pbState = psBuilder.build();
                        mMediaSession.setPlaybackState(pbState);

                        MediaMetadataCompat.Builder mmBuilder = new MediaMetadataCompat.Builder();
                        mmBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                                itemStatus.getContentDuration());
                        mMediaSession.setMetadata(mmBuilder.build());
                    }
                });
                // Start remote playback (if necessary)
                // TODO: b/77556429
                mRoutePlayer.openVideo(mUri);
            }
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route, int reason) {
            if (mRoute != null && mRoutePlayer != null) {
                mRoutePlayer.release();
                mRoutePlayer = null;
            }
            if (mRoute == route) {
                mRoute = null;
            }
            if (reason != MediaRouter.UNSELECT_REASON_ROUTE_CHANGED) {
                // TODO: Resume local playback  (if necessary)
                // TODO: b/77556429
                openVideo(mUri, mHeaders);
            }
        }
    };

    @Override
    public void initialize(
            VideoView2 instance, Context context,
            @Nullable AttributeSet attrs, int defStyleAttr) {
        mInstance = instance;

        mVideoWidth = 0;
        mVideoHeight = 0;
        mSpeed = 1.0f;
        mFallbackSpeed = mSpeed;
        mShowControllerIntervalMs = DEFAULT_SHOW_CONTROLLER_INTERVAL_MS;

        mAccessibilityManager = (AccessibilityManager) context.getSystemService(
                Context.ACCESSIBILITY_SERVICE);

        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mAudioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build();

        mInstance.setFocusable(true);
        mInstance.setFocusableInTouchMode(true);
        mInstance.requestFocus();

        mTextureView = new VideoTextureViewWithMp1(context);
        mSurfaceView = new VideoSurfaceViewWithMp1(context);
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        mTextureView.setLayoutParams(params);
        mSurfaceView.setLayoutParams(params);
        mTextureView.setSurfaceListener(this);
        mSurfaceView.setSurfaceListener(this);

        mInstance.addView(mTextureView);
        mInstance.addView(mSurfaceView);

        boolean enableControlView = (attrs == null) || attrs.getAttributeBooleanValue(
                "http://schemas.android.com/apk/res/android",
                "enableControlView", true);
        if (enableControlView) {
            mMediaControlView = new MediaControlView2(context);
        }

        // Choose surface view by default
        int viewType = (attrs == null) ? VideoView2.VIEW_TYPE_SURFACEVIEW
                : attrs.getAttributeIntValue(
                "http://schemas.android.com/apk/res/android",
                "viewType", VideoView2.VIEW_TYPE_SURFACEVIEW);
        if (viewType == VideoView2.VIEW_TYPE_SURFACEVIEW) {
            Log.d(TAG, "viewType attribute is surfaceView.");
            mTextureView.setVisibility(View.GONE);
            mSurfaceView.setVisibility(View.VISIBLE);
            mCurrentView = mSurfaceView;
        } else if (viewType == VideoView2.VIEW_TYPE_TEXTUREVIEW) {
            Log.d(TAG, "viewType attribute is textureView.");
            mTextureView.setVisibility(View.VISIBLE);
            mSurfaceView.setVisibility(View.GONE);
            mCurrentView = mTextureView;
        }

        MediaRouteSelector.Builder builder = new MediaRouteSelector.Builder();
        builder.addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        builder.addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO);
        builder.addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO);
        mRouteSelector = builder.build();
    }

    /**
     * Sets MediaControlView2 instance. It will replace the previously assigned MediaControlView2
     * instance if any.
     *
     * @param mediaControlView a media control view2 instance.
     * @param intervalMs a time interval in milliseconds until VideoView2 hides MediaControlView2.
     */
    @Override
    public void setMediaControlView2(MediaControlView2 mediaControlView, long intervalMs) {
        mMediaControlView = mediaControlView;
        mShowControllerIntervalMs = intervalMs;
        mMediaControlView.setRouteSelector(mRouteSelector);

        if (mInstance.isAttachedToWindow()) {
            attachMediaControlView();
        }
    }

    /**
     * Returns MediaControlView2 instance which is currently attached to VideoView2 by default or by
     * {@link #setMediaControlView2} method.
     */
    @Override
    public MediaControlView2 getMediaControlView2() {
        return mMediaControlView;
    }

    /**
     * Sets MediaMetadata2 instance. It will replace the previously assigned MediaMetadata2 instance
     * if any.
     *
     * @param metadata a MediaMetadata2 instance.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void setMediaMetadata(MediaMetadata2 metadata) {
      //mProvider.setMediaMetadata_impl(metadata);
    }

    /**
     * Returns MediaMetadata2 instance which is retrieved from MediaPlayer inside VideoView2 by
     * default or by {@link #setMediaMetadata} method.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public MediaMetadata2 getMediaMetadata() {
        return mMediaMetadata;
    }

    /**
     * Returns MediaController instance which is connected with MediaSession that VideoView2 is
     * using. This method should be called when VideoView2 is attached to window, or it throws
     * IllegalStateException, since internal MediaSession instance is not available until
     * this view is attached to window. Please check {@link View#isAttachedToWindow}
     * before calling this method.
     *
     * @throws IllegalStateException if interal MediaSession is not created yet.
     * @hide  TODO: remove
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public MediaControllerCompat getMediaController() {
        if (mMediaSession == null) {
            throw new IllegalStateException("MediaSession instance is not available.");
        }
        return mMediaController;
    }

    /**
     * Returns {@link SessionToken2} so that developers create their own
     * {@link androidx.media.MediaController2} instance. This method should be called when
     * VideoView2 is attached to window, or it throws IllegalStateException.
     *
     * @throws IllegalStateException if interal MediaSession is not created yet.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public SessionToken2 getMediaSessionToken() {
        //return mProvider.getMediaSessionToken_impl();
        return null;
    }

    /**
     * Shows or hides closed caption or subtitles if there is any.
     * The first subtitle track will be chosen if there multiple subtitle tracks exist.
     * Default behavior of VideoView2 is not showing subtitle.
     * @param enable shows closed caption or subtitles if this value is true, or hides.
     */
    @Override
    public void setSubtitleEnabled(boolean enable) {
        // No-op on API < 28
    }

    /**
     * Returns true if showing subtitle feature is enabled or returns false.
     * Although there is no subtitle track or closed caption, it can return true, if the feature
     * has been enabled by {@link #setSubtitleEnabled}.
     */
    @Override
    public boolean isSubtitleEnabled() {
        // Not supported on API < 28
        return false;
    }

    /**
     * Sets playback speed.
     *
     * It is expressed as a multiplicative factor, where normal speed is 1.0f. If it is less than
     * or equal to zero, it will be just ignored and nothing will be changed. If it exceeds the
     * maximum speed that internal engine supports, system will determine best handling or it will
     * be reset to the normal speed 1.0f.
     * @param speed the playback speed. It should be positive.
     */
    @Override
    public void setSpeed(float speed) {
        if (speed <= 0.0f) {
            Log.e(TAG, "Unsupported speed (" + speed + ") is ignored.");
            return;
        }
        mSpeed = speed;
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            applySpeed();
        }
        updatePlaybackState();
    }

    /**
     * Returns playback speed.
     *
     * It returns the same value that has been set by {@link #setSpeed}, if it was available value.
     * If {@link #setSpeed} has not been called before, then the normal speed 1.0f will be returned.
     */
    @Override
    public float getSpeed() {
        return mSpeed;
    }

    /**
     * Sets which type of audio focus will be requested during the playback, or configures playback
     * to not request audio focus. Valid values for focus requests are
     * {@link AudioManager#AUDIOFOCUS_GAIN}, {@link AudioManager#AUDIOFOCUS_GAIN_TRANSIENT},
     * {@link AudioManager#AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK}, and
     * {@link AudioManager#AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE}. Or use
     * {@link AudioManager#AUDIOFOCUS_NONE} to express that audio focus should not be
     * requested when playback starts. You can for instance use this when playing a silent animation
     * through this class, and you don't want to affect other audio applications playing in the
     * background.
     *
     * @param focusGain the type of audio focus gain that will be requested, or
     *                  {@link AudioManager#AUDIOFOCUS_NONE} to disable the use audio focus during
     *                  playback.
     */
    @Override
    public void setAudioFocusRequest(int focusGain) {
        if (focusGain != AudioManager.AUDIOFOCUS_NONE
                && focusGain != AudioManager.AUDIOFOCUS_GAIN
                && focusGain != AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                && focusGain != AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                && focusGain != AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE) {
            throw new IllegalArgumentException("Illegal audio focus type " + focusGain);
        }
        mAudioFocusType = focusGain;
    }

    /**
     * Sets the {@link AudioAttributesCompat} to be used during the playback of the video.
     *
     * @param attributes non-null <code>AudioAttributesCompat</code>.
     */
    @Override
    public void setAudioAttributes(@NonNull AudioAttributesCompat attributes) {
        if (attributes == null) {
            throw new IllegalArgumentException("Illegal null AudioAttributesCompat");
        }
        mAudioAttributes = (AudioAttributes) attributes.unwrap();
    }

    /**
     * Sets video path.
     *
     * @param path the path of the video.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void setVideoPath(String path) {
        setVideoUri(Uri.parse(path));
    }

    /**
     * Sets video URI.
     *
     * @param uri the URI of the video.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void setVideoUri(Uri uri) {
        setVideoUri(uri, null);
    }

    /**
     * Sets video URI using specific headers.
     *
     * @param uri     the URI of the video.
     * @param headers the headers for the URI request.
     *                Note that the cross domain redirection is allowed by default, but that can be
     *                changed with key/value pairs through the headers parameter with
     *                "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value
     *                to disallow or allow cross domain redirection.
     */
    @Override
    public void setVideoUri(Uri uri, @Nullable Map<String, String> headers) {
        mSeekWhenPrepared = 0;
        openVideo(uri, headers);
    }

    /**
     * Sets {@link MediaItem2} object to render using VideoView2. Alternative way to set media
     * object to VideoView2 is {@link #setDataSource}.
     * @param mediaItem the MediaItem2 to play
     * @see #setDataSource
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void setMediaItem(@NonNull MediaItem2 mediaItem) {
    }

    /**
     * Sets {@link DataSourceDesc} object to render using VideoView2.
     * @param dataSource the {@link DataSourceDesc} object to play.
     * @see #setMediaItem
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void setDataSource(@NonNull DataSourceDesc dataSource) {
    }

    /**
     * Selects which view will be used to render video between SurfacView and TextureView.
     *
     * @param viewType the view type to render video
     * <ul>
     * <li>{@link #VideoView2.VIEW_TYPE_SURFACEVIEW}
     * <li>{@link #VideoView2.VIEW_TYPE_TEXTUREVIEW}
     * </ul>
     */
    @Override
    public void setViewType(@VideoView2.ViewType int viewType) {
        if (viewType == mCurrentView.getViewType()) {
            return;
        }
        VideoViewInterfaceWithMp1 targetView;
        if (viewType == VideoView2.VIEW_TYPE_TEXTUREVIEW) {
            Log.d(TAG, "switching to TextureView");
            targetView = mTextureView;
        } else if (viewType == VideoView2.VIEW_TYPE_SURFACEVIEW) {
            Log.d(TAG, "switching to SurfaceView");
            targetView = mSurfaceView;
        } else {
            throw new IllegalArgumentException("Unknown view type: " + viewType);
        }
        ((View) targetView).setVisibility(View.VISIBLE);
        targetView.takeOver(mCurrentView);
        mInstance.requestLayout();
    }

    /**
     * Returns view type.
     *
     * @return view type. See {@see setViewType}.
     */
    @Override
    @VideoView2.ViewType
    public int getViewType() {
        return mCurrentView.getViewType();
    }

    /**
     * Sets custom actions which will be shown as custom buttons in {@link MediaControlView2}.
     *
     * @param actionList A list of {@link PlaybackStateCompat.CustomAction}. The return value of
     *                   {@link PlaybackStateCompat.CustomAction#getIcon()} will be used to draw
     *                   buttons in {@link MediaControlView2}.
     * @param executor executor to run callbacks on.
     * @param listener A listener to be called when a custom button is clicked.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void setCustomActions(List<PlaybackStateCompat.CustomAction> actionList,
            Executor executor, VideoView2.OnCustomActionListener listener) {
        mCustomActionList = actionList;
        mCustomActionListenerRecord = new Pair<>(executor, listener);

        // Create a new playback builder in order to clear existing the custom actions.
        mStateBuilder = null;
        updatePlaybackState();
    }

    /**
     * Registers a callback to be invoked when a view type change is done.
     * {@see #setViewType(int)}
     * @param l The callback that will be run
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void setOnViewTypeChangedListener(VideoView2.OnViewTypeChangedListener l) {
        mViewTypeChangedListener = l;
    }

    @Override
    public void onAttachedToWindowImpl() {
        // Create MediaSession
        mMediaSession = new MediaSessionCompat(mInstance.getContext(), "VideoView2MediaSession");
        mMediaSession.setCallback(new MediaSessionCallback());
        mMediaSession.setActive(true);
        mMediaController = mMediaSession.getController();
        attachMediaControlView();
        if (mCurrentState == STATE_PREPARED) {
            extractTracks();
            extractMetadata();
            extractAudioMetadata();
            if (mNeedUpdateMediaType) {
                mMediaSession.sendSessionEvent(
                        MediaControlView2.EVENT_UPDATE_MEDIA_TYPE_STATUS,
                        mMediaTypeData);
                mNeedUpdateMediaType = false;
            }
        }

        mMediaRouter = MediaRouter.getInstance(mInstance.getContext());
        mMediaRouter.setMediaSessionCompat(mMediaSession);
        mMediaRouter.addCallback(mRouteSelector, mRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    @Override
    public void onDetachedFromWindowImpl() {
        mMediaSession.release();
        mMediaSession = null;
        mMediaController = null;
    }

    @Override
    public void onTouchEventImpl(MotionEvent ev) {
        if (DEBUG) {
            Log.d(TAG, "onTouchEvent(). mCurrentState=" + mCurrentState
                    + ", mTargetState=" + mTargetState);
        }
        if (ev.getAction() == MotionEvent.ACTION_UP && mMediaControlView != null) {
            if (!isMusicMediaType() || mSizeType != SIZE_TYPE_FULL) {
                toggleMediaControlViewVisibility();
            }
        }
    }

    @Override
    public void onTrackballEventImpl(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP && mMediaControlView != null) {
            if (!isMusicMediaType() || mSizeType != SIZE_TYPE_FULL) {
                toggleMediaControlViewVisibility();
            }
        }
    }

    @Override
    public void onMeasureImpl(int widthMeasureSpec, int heightMeasureSpec) {
        if (isMusicMediaType()) {
            int currWidth = mInstance.getMeasuredWidth();
            int currHeight = mInstance.getMeasuredHeight();
            if (mPrevWidth != currWidth || mPrevHeight != currHeight) {
                Point screenSize = new Point();
                mManager.getDefaultDisplay().getSize(screenSize);
                int screenWidth = screenSize.x;
                int screenHeight = screenSize.y;

                if (currWidth == screenWidth && currHeight == screenHeight) {
                    int orientation = retrieveOrientation();
                    if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                        inflateMusicView(R.layout.full_landscape_music);
                    } else {
                        inflateMusicView(R.layout.full_portrait_music);
                    }

                    if (mSizeType != SIZE_TYPE_FULL) {
                        mSizeType = SIZE_TYPE_FULL;
                        // Remove existing mFadeOut callback
                        mMediaControlView.removeCallbacks(mFadeOut);
                        mMediaControlView.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (mSizeType != SIZE_TYPE_EMBEDDED) {
                        mSizeType = SIZE_TYPE_EMBEDDED;
                        inflateMusicView(R.layout.embedded_music);
                        // Add new mFadeOut callback
                        mMediaControlView.postDelayed(mFadeOut, mShowControllerIntervalMs);
                    }
                }
                mPrevWidth = currWidth;
                mPrevHeight = currHeight;
            }
        }
    }

    ///////////////////////////////////////////////////
    // Implements VideoViewInterfaceWithMp1.SurfaceListener
    ///////////////////////////////////////////////////

    @Override
    public void onSurfaceCreated(View view, int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated(). mCurrentState=" + mCurrentState
                    + ", mTargetState=" + mTargetState + ", width/height: " + width + "/" + height
                    + ", " + view.toString());
        }
        if (needToStart()) {
            mMediaController.getTransportControls().play();
        }
    }

    @Override
    public void onSurfaceDestroyed(View view) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceDestroyed(). mCurrentState=" + mCurrentState
                    + ", mTargetState=" + mTargetState + ", " + view.toString());
        }
    }

    @Override
    public void onSurfaceChanged(View view, int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceChanged(). width/height: " + width + "/" + height
                    + ", " + view.toString());
        }
    }

    @Override
    public void onSurfaceTakeOverDone(VideoViewInterfaceWithMp1 view) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceTakeOverDone(). Now current view is: " + view);
        }
        mCurrentView = view;
        if (mViewTypeChangedListener != null) {
            mViewTypeChangedListener.onViewTypeChanged(mInstance, view.getViewType());
        }
        if (needToStart()) {
            mMediaController.getTransportControls().play();
        }
    }

    ///////////////////////////////////////////////////
    // Protected or private methods
    ///////////////////////////////////////////////////

    private void attachMediaControlView() {
        // Get MediaController from MediaSession and set it inside MediaControlView
        mMediaControlView.setController(mMediaSession.getController());

        LayoutParams params =
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mInstance.addView(mMediaControlView, params);
    }

    protected boolean isInPlaybackState() {
        return (mMediaPlayer != null || mRoutePlayer != null)
                && mCurrentState != STATE_ERROR
                && mCurrentState != STATE_IDLE
                && mCurrentState != STATE_PREPARING;
    }

    private boolean needToStart() {
        return (mMediaPlayer != null || mRoutePlayer != null)
                && isAudioGranted()
                && isWaitingPlayback();
    }

    private boolean isMusicMediaType() {
        return mVideoTrackIndices != null && mVideoTrackIndices.size() == 0;
    }

    private boolean isWaitingPlayback() {
        return mCurrentState != STATE_PLAYING && mTargetState == STATE_PLAYING;
    }

    private boolean isAudioGranted() {
        return mAudioFocused || mAudioFocusType == AudioManager.AUDIOFOCUS_NONE;
    }

    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener =
            new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    mAudioFocused = true;
                    if (needToStart()) {
                        mMediaController.getTransportControls().play();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    mAudioFocused = false;
                    if (isInPlaybackState() && mMediaPlayer.isPlaying()) {
                        mMediaController.getTransportControls().pause();
                    } else {
                        mTargetState = STATE_PAUSED;
                    }
            }
        }
    };

    @SuppressWarnings("deprecation")
    private void requestAudioFocus(int focusType) {
        int result;
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            AudioFocusRequest focusRequest;
            focusRequest = new AudioFocusRequest.Builder(focusType)
                    .setAudioAttributes(mAudioAttributes)
                    .setOnAudioFocusChangeListener(mAudioFocusListener)
                    .build();
            result = mAudioManager.requestAudioFocus(focusRequest);
        } else {
            result = mAudioManager.requestAudioFocus(mAudioFocusListener,
                    AudioManager.STREAM_MUSIC,
                    focusType);
        }
        if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            mAudioFocused = false;
        } else if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mAudioFocused = true;
        } else if (result == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
            mAudioFocused = false;
        }
    }

    // Creates a MediaPlayer instance and prepare playback.
    private void openVideo(Uri uri, Map<String, String> headers) {
        resetPlayer();
        mUri = uri;
        if (isRemotePlayback()) {
            // TODO: b/77556429
            mRoutePlayer.openVideo(uri);
            return;
        }

        try {
            Log.d(TAG, "openVideo(): creating new MediaPlayer instance.");
            mMediaPlayer = new MediaPlayer();
            final Context context = mInstance.getContext();
            setupMediaPlayer(context, uri, headers);

            // we don't set the target state here either, but preserve the
            // target state that was there before.
            mCurrentState = STATE_PREPARING;
            mMediaPlayer.prepareAsync();

            // Save file name as title since the file may not have a title Metadata.
            mTitle = uri.getPath();
            String scheme = uri.getScheme();
            if (scheme != null && scheme.equals("file")) {
                mTitle = uri.getLastPathSegment();
                mRetriever = new MediaMetadataRetriever();
                mRetriever.setDataSource(context, uri);
            }

            if (DEBUG) {
                Log.d(TAG, "openVideo(). mCurrentState=" + mCurrentState
                        + ", mTargetState=" + mTargetState);
            }
        } catch (IOException | IllegalArgumentException ex) {
            Log.w(TAG, "Unable to open content: " + uri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer,
                    MediaPlayer.MEDIA_ERROR_UNKNOWN, MediaPlayer.MEDIA_ERROR_IO);
        }
    }

    /**
     * Used in openVideo(). Setup MediaPlayer and related objects before calling prepare.
     */
    protected void setupMediaPlayer(Context context, Uri uri, Map<String, String> headers)
            throws IOException {
        mSurfaceView.setMediaPlayer(mMediaPlayer);
        mTextureView.setMediaPlayer(mMediaPlayer);
        mCurrentView.assignSurfaceToMediaPlayer(mMediaPlayer);

        mMediaPlayer.setOnPreparedListener(mPreparedListener);
        mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
        mMediaPlayer.setOnCompletionListener(mCompletionListener);
        mMediaPlayer.setOnSeekCompleteListener(mSeekCompleteListener);
        mMediaPlayer.setOnErrorListener(mErrorListener);
        mMediaPlayer.setOnInfoListener(mInfoListener);
        mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);

        mCurrentBufferPercentage = -1;
        mMediaPlayer.setDataSource(context, uri, headers);
        mMediaPlayer.setAudioAttributes(mAudioAttributes);
    }

    /*
     * Reset the media player in any state
     */
    @SuppressWarnings("deprecation")
    private void resetPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mTextureView.setMediaPlayer(null);
            mSurfaceView.setMediaPlayer(null);
            mCurrentState = STATE_IDLE;
            mTargetState = STATE_IDLE;
            if (mAudioFocusType != AudioManager.AUDIOFOCUS_NONE) {
                mAudioManager.abandonAudioFocus(null);
            }
        }
        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    private void updatePlaybackState() {
        if (mStateBuilder == null) {
            long playbackActions = PlaybackStateCompat.ACTION_PLAY
                    | PlaybackStateCompat.ACTION_PAUSE
                    | PlaybackStateCompat.ACTION_REWIND | PlaybackStateCompat.ACTION_FAST_FORWARD
                    | PlaybackStateCompat.ACTION_SEEK_TO;
            mStateBuilder = new PlaybackStateCompat.Builder();
            mStateBuilder.setActions(playbackActions);

            if (mCustomActionList != null) {
                for (PlaybackStateCompat.CustomAction action : mCustomActionList) {
                    mStateBuilder.addCustomAction(action);
                }
            }
        }
        mStateBuilder.setState(getCorrespondingPlaybackState(),
                mMediaPlayer.getCurrentPosition(), mSpeed);
        if (mCurrentState != STATE_ERROR
                && mCurrentState != STATE_IDLE
                && mCurrentState != STATE_PREPARING) {
            if (mCurrentBufferPercentage == -1) {
                mStateBuilder.setBufferedPosition(-1);
            } else {
                mStateBuilder.setBufferedPosition(
                        (long) (mCurrentBufferPercentage / 100.0 * mMediaPlayer.getDuration()));
            }
        }

        // Set PlaybackState for MediaSession
        if (mMediaSession != null) {
            PlaybackStateCompat state = mStateBuilder.build();
            mMediaSession.setPlaybackState(state);
        }
    }

    private int getCorrespondingPlaybackState() {
        switch (mCurrentState) {
            case STATE_ERROR:
                return PlaybackStateCompat.STATE_ERROR;
            case STATE_IDLE:
                return PlaybackStateCompat.STATE_NONE;
            case STATE_PREPARING:
                return PlaybackStateCompat.STATE_CONNECTING;
            case STATE_PREPARED:
                return PlaybackStateCompat.STATE_PAUSED;
            case STATE_PLAYING:
                return PlaybackStateCompat.STATE_PLAYING;
            case STATE_PAUSED:
                return PlaybackStateCompat.STATE_PAUSED;
            case STATE_PLAYBACK_COMPLETED:
                return PlaybackStateCompat.STATE_STOPPED;
            default:
                return -1;
        }
    }

    private final Runnable mFadeOut = new Runnable() {
        @Override
        public void run() {
            if (mCurrentState == STATE_PLAYING) {
                mMediaControlView.setVisibility(View.GONE);
            }
        }
    };

    private void showController() {
        if (mMediaControlView == null || !isInPlaybackState()
                || (isMusicMediaType() && mSizeType == SIZE_TYPE_FULL)) {
            return;
        }
        mMediaControlView.removeCallbacks(mFadeOut);
        mMediaControlView.setVisibility(View.VISIBLE);
        if (mShowControllerIntervalMs != 0
                && !mAccessibilityManager.isTouchExplorationEnabled()) {
            mMediaControlView.postDelayed(mFadeOut, mShowControllerIntervalMs);
        }
    }

    private void toggleMediaControlViewVisibility() {
        if (mMediaControlView.getVisibility() == View.VISIBLE) {
            mMediaControlView.removeCallbacks(mFadeOut);
            mMediaControlView.setVisibility(View.GONE);
        } else {
            showController();
        }
    }

    private void applySpeed() {
        if (android.os.Build.VERSION.SDK_INT < 23) {
            return;
        }
        PlaybackParams params = mMediaPlayer.getPlaybackParams().allowDefaults();
        if (mSpeed != params.getSpeed()) {
            try {
                params.setSpeed(mSpeed);
                mMediaPlayer.setPlaybackParams(params);
                mFallbackSpeed = mSpeed;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "PlaybackParams has unsupported value: " + e);
                float fallbackSpeed = mMediaPlayer.getPlaybackParams().allowDefaults().getSpeed();
                if (fallbackSpeed > 0.0f) {
                    mFallbackSpeed = fallbackSpeed;
                }
                mSpeed = mFallbackSpeed;
            }
        }
    }

    private boolean isRemotePlayback() {
        if (mMediaController == null) {
            return false;
        }
        PlaybackInfo playbackInfo = mMediaController.getPlaybackInfo();
        return playbackInfo != null
                && playbackInfo.getPlaybackType() == PlaybackInfo.PLAYBACK_TYPE_REMOTE;
    }

    protected void extractTracks() {
        MediaPlayer.TrackInfo[] trackInfos = mMediaPlayer.getTrackInfo();
        mVideoTrackIndices = new ArrayList<>();
        mAudioTrackIndices = new ArrayList<>();
        for (int i = 0; i < trackInfos.length; ++i) {
            int trackType = trackInfos[i].getTrackType();
            if (trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_VIDEO) {
                mVideoTrackIndices.add(i);
            } else if (trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
                mAudioTrackIndices.add(i);
            }
        }
        // Select first tracks as default
        if (mVideoTrackIndices.size() > 0) {
            mSelectedVideoTrackIndex = 0;
        }
        if (mAudioTrackIndices.size() > 0) {
            mSelectedAudioTrackIndex = 0;
        }

        Bundle data = new Bundle();
        data.putInt(MediaControlView2.KEY_VIDEO_TRACK_COUNT, mVideoTrackIndices.size());
        data.putInt(MediaControlView2.KEY_AUDIO_TRACK_COUNT, mAudioTrackIndices.size());
        mMediaSession.sendSessionEvent(MediaControlView2.EVENT_UPDATE_TRACK_STATUS, data);
    }

    protected void doShowSubtitleCommand(Bundle args) {
        // No-op
    }

    protected void doHideSubtitleCommand() {
        // No-op
    }

    private void extractMetadata() {
        if (mRetriever == null) {
            return;
        }
        // Get and set duration and title values as MediaMetadata for MediaControlView2
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
        String title = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        if (title != null) {
            mTitle = title;
        }
        builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, mTitle);
        builder.putLong(
                MediaMetadataCompat.METADATA_KEY_DURATION, mMediaPlayer.getDuration());

        if (mMediaSession != null) {
            mMediaSession.setMetadata(builder.build());
        }
    }

    @SuppressWarnings("deprecation")
    private void extractAudioMetadata() {
        if (mRetriever == null || !isMusicMediaType()) {
            return;
        }

        mResources = mInstance.getResources();
        mManager = (WindowManager) mInstance.getContext().getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);

        byte[] album = mRetriever.getEmbeddedPicture();
        if (album != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(album, 0, album.length);
            mMusicAlbumDrawable = new BitmapDrawable(bitmap);

            Palette.Builder builder = Palette.from(bitmap);
            builder.generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    mDominantColor = palette.getDominantColor(0);
                    if (mMusicView != null) {
                        mMusicView.setBackgroundColor(mDominantColor);
                    }
                }
            });
        } else {
            mMusicAlbumDrawable = mResources.getDrawable(R.drawable.ic_default_album_image);
        }

        String title = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        if (title != null) {
            mMusicTitleText = title;
        } else {
            mMusicTitleText = mResources.getString(R.string.mcv2_music_title_unknown_text);
        }

        String artist = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        if (artist != null) {
            mMusicArtistText = artist;
        } else {
            mMusicArtistText = mResources.getString(R.string.mcv2_music_artist_unknown_text);
        }

        // Send title and artist string to MediaControlView2
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
        builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, mMusicTitleText);
        builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mMusicArtistText);
        mMediaSession.setMetadata(builder.build());

        // Display Embedded mode as default
        mInstance.removeView(mSurfaceView);
        mInstance.removeView(mTextureView);
        inflateMusicView(R.layout.embedded_music);
    }

    private int retrieveOrientation() {
        DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
        int width = dm.widthPixels;
        int height = dm.heightPixels;

        return (height > width)
                ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    }

    private void inflateMusicView(int layoutId) {
        mInstance.removeView(mMusicView);

        LayoutInflater inflater = (LayoutInflater) mInstance.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(layoutId, null);
        v.setBackgroundColor(mDominantColor);

        ImageView albumView = v.findViewById(R.id.album);
        if (albumView != null) {
            albumView.setImageDrawable(mMusicAlbumDrawable);
        }

        TextView titleView = v.findViewById(R.id.title);
        if (titleView != null) {
            titleView.setText(mMusicTitleText);
        }

        TextView artistView = v.findViewById(R.id.artist);
        if (artistView != null) {
            artistView.setText(mMusicArtistText);
        }

        mMusicView = v;
        mInstance.addView(mMusicView, 0);
    }

    private MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
            new MediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(
                        MediaPlayer mp, int width, int height) {
                    if (DEBUG) {
                        Log.d(TAG, "onVideoSizeChanged(): size: " + width + "/" + height);
                    }
                    mVideoWidth = mp.getVideoWidth();
                    mVideoHeight = mp.getVideoHeight();
                    if (DEBUG) {
                        Log.d(TAG, "onVideoSizeChanged(): mVideoSize:" + mVideoWidth + "/"
                                + mVideoHeight);
                    }
                    if (mVideoWidth != 0 && mVideoHeight != 0) {
                        mInstance.requestLayout();
                    }
                }
            };

    private MediaPlayer.OnPreparedListener mPreparedListener =
            new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            if (DEBUG) {
                Log.d(TAG, "OnPreparedListener(). mCurrentState=" + mCurrentState
                        + ", mTargetState=" + mTargetState);
            }
            mCurrentState = STATE_PREPARED;
            // Create and set playback state for MediaControlView2
            updatePlaybackState();

            if (mMediaSession != null) {
                extractTracks();
                extractMetadata();
                extractAudioMetadata();
            }

            if (mMediaControlView != null) {
                mMediaControlView.setEnabled(true);
            }
            int videoWidth = mp.getVideoWidth();
            int videoHeight = mp.getVideoHeight();

            // mSeekWhenPrepared may be changed after seekTo() call
            long seekToPosition = mSeekWhenPrepared;
            if (seekToPosition != 0) {
                mMediaController.getTransportControls().seekTo(seekToPosition);
            }

            if (videoWidth != 0 && videoHeight != 0) {
                if (videoWidth != mVideoWidth || videoHeight != mVideoHeight) {
                    mVideoWidth = videoWidth;
                    mVideoHeight = videoHeight;
                    mInstance.requestLayout();
                }

                if (needToStart()) {
                    mMediaController.getTransportControls().play();
                }
            } else {
                // We don't know the video size yet, but should start anyway.
                // The video size might be reported to us later.
                if (needToStart()) {
                    mMediaController.getTransportControls().play();
                }
            }
            // Get and set duration and title values as MediaMetadata for MediaControlView2
            MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();

            builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, mTitle);
            builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mMediaPlayer.getDuration());

            if (mMediaSession != null) {
                mMediaSession.setMetadata(builder.build());

                if (mNeedUpdateMediaType) {
                    mMediaSession.sendSessionEvent(
                            MediaControlView2.EVENT_UPDATE_MEDIA_TYPE_STATUS, mMediaTypeData);
                    mNeedUpdateMediaType = false;
                }
            }
        }
    };

    private MediaPlayer.OnSeekCompleteListener mSeekCompleteListener =
            new MediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(MediaPlayer mp) {
                    updatePlaybackState();
                }
    };

    private MediaPlayer.OnCompletionListener mCompletionListener =
            new MediaPlayer.OnCompletionListener() {
        @Override
        @SuppressWarnings("deprecation")
        public void onCompletion(MediaPlayer mp) {
            mCurrentState = STATE_PLAYBACK_COMPLETED;
            mTargetState = STATE_PLAYBACK_COMPLETED;
            updatePlaybackState();
            if (mAudioFocusType != AudioManager.AUDIOFOCUS_NONE) {
                mAudioManager.abandonAudioFocus(null);
            }
        }
    };

    private MediaPlayer.OnInfoListener mInfoListener = new MediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            if (what == MediaPlayer.MEDIA_INFO_METADATA_UPDATE) {
                extractTracks();
            }
            return true;
        }
    };

    private MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int frameworkErr, int implErr) {
            if (DEBUG) {
                Log.d(TAG, "Error: " + frameworkErr + "," + implErr);
            }
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            updatePlaybackState();

            if (mMediaControlView != null) {
                mMediaControlView.setVisibility(View.GONE);
            }
            return true;
        }
    };

    private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
            new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            mCurrentBufferPercentage = percent;
            updatePlaybackState();
        }
    };

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onCommand(String command, Bundle args, ResultReceiver receiver) {
            if (isRemotePlayback()) {
                mRoutePlayer.onCommand(command, args, receiver);
            } else {
                switch (command) {
                    case MediaControlView2.COMMAND_SHOW_SUBTITLE:
                        doShowSubtitleCommand(args);
                        break;
                    case MediaControlView2.COMMAND_HIDE_SUBTITLE:
                        doHideSubtitleCommand();
                        break;
                    case MediaControlView2.COMMAND_SELECT_AUDIO_TRACK:
                        int audioIndex = args.getInt(MediaControlView2.KEY_SELECTED_AUDIO_INDEX,
                                INVALID_TRACK_INDEX);
                        if (audioIndex != INVALID_TRACK_INDEX) {
                            int audioTrackIndex = mAudioTrackIndices.get(audioIndex);
                            if (audioTrackIndex != mSelectedAudioTrackIndex) {
                                mSelectedAudioTrackIndex = audioTrackIndex;
                                mMediaPlayer.selectTrack(mSelectedAudioTrackIndex);
                            }
                        }
                        break;
                    case MediaControlView2.COMMAND_SET_PLAYBACK_SPEED:
                        float speed = args.getFloat(
                                MediaControlView2.KEY_PLAYBACK_SPEED, INVALID_SPEED);
                        if (speed != INVALID_SPEED && speed != mSpeed) {
                            setSpeed(speed);
                            mSpeed = speed;
                        }
                        break;
                    case MediaControlView2.COMMAND_MUTE:
                        mVolumeLevel = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                        break;
                    case MediaControlView2.COMMAND_UNMUTE:
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mVolumeLevel, 0);
                        break;
                }
            }
            showController();
        }

        @Override
        public void onCustomAction(final String action, final Bundle extras) {
            mCustomActionListenerRecord.first.execute(new Runnable() {
                @Override
                public void run() {
                    mCustomActionListenerRecord.second.onCustomAction(action, extras);
                }
            });
            showController();
        }

        @Override
        public void onPlay() {
            if (!isAudioGranted()) {
                requestAudioFocus(mAudioFocusType);
            }

            if ((isInPlaybackState() && mCurrentView.hasAvailableSurface()) || isMusicMediaType()) {
                if (isRemotePlayback()) {
                    mRoutePlayer.onPlay();
                } else {
                    applySpeed();
                    mMediaPlayer.start();
                    mCurrentState = STATE_PLAYING;
                    updatePlaybackState();
                }
                mCurrentState = STATE_PLAYING;
            }
            mTargetState = STATE_PLAYING;
            if (DEBUG) {
                Log.d(TAG, "onPlay(). mCurrentState=" + mCurrentState
                        + ", mTargetState=" + mTargetState);
            }
            showController();
        }

        @Override
        public void onPause() {
            if (isInPlaybackState()) {
                if (isRemotePlayback()) {
                    mRoutePlayer.onPlay();
                    mCurrentState = STATE_PAUSED;
                } else if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                    mCurrentState = STATE_PAUSED;
                    updatePlaybackState();
                }
            }
            mTargetState = STATE_PAUSED;
            if (DEBUG) {
                Log.d(TAG, "onPause(). mCurrentState=" + mCurrentState
                        + ", mTargetState=" + mTargetState);
            }
            showController();
        }

        @Override
        public void onSeekTo(long pos) {
            if (isInPlaybackState()) {
                if (isRemotePlayback()) {
                    mRoutePlayer.onPlay();
                } else {
                    if (android.os.Build.VERSION.SDK_INT < 26) {
                        mMediaPlayer.seekTo((int) pos);
                    } else {
                        mMediaPlayer.seekTo(pos, MediaPlayer.SEEK_PREVIOUS_SYNC);
                    }
                    mSeekWhenPrepared = 0;
                }
            } else {
                mSeekWhenPrepared = pos;
            }
            showController();
        }

        @Override
        public void onStop() {
            if (isRemotePlayback()) {
                mRoutePlayer.onPlay();
            } else {
                resetPlayer();
            }
            showController();
        }
    }
}
