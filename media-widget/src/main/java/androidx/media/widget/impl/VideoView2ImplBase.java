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
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.media.AudioAttributesCompat;
import androidx.media.BaseMediaPlayer;
import androidx.media.DataSourceDesc;
import androidx.media.MediaItem2;
import androidx.media.MediaMetadata2;
import androidx.media.MediaPlayer2;
import androidx.media.MediaSession2;
import androidx.media.SessionCommand2;
import androidx.media.SessionCommandGroup2;
import androidx.media.SessionToken2;
import androidx.media.SubtitleData2;
import androidx.media.subtitle.Cea708CaptionRenderer;
import androidx.media.subtitle.ClosedCaptionRenderer;
import androidx.media.subtitle.SubtitleController;
import androidx.media.subtitle.SubtitleTrack;
import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaItemStatus;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;
import androidx.palette.graphics.Palette;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Base implementation of VideoView2.
 */
@RequiresApi(28)
class VideoView2ImplBase implements VideoView2Impl, VideoViewInterface.SurfaceListener {
    private static final String TAG = "VideoView2ImplBase";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    private static final int INVALID_TRACK_INDEX = -1;
    private static final int SIZE_TYPE_EMBEDDED = 0;
    private static final int SIZE_TYPE_FULL = 1;

    private AudioAttributesCompat mAudioAttributes;

    private VideoView2.OnViewTypeChangedListener mViewTypeChangedListener;

    private VideoViewInterface mCurrentView;
    private VideoTextureView mTextureView;
    private VideoSurfaceView mSurfaceView;

    private MediaPlayer2 mMediaPlayer;
    private MediaItem2 mMediaItem;
    private List<MediaItem2> mPlayList;
    private MediaControlView2 mMediaControlView;
    private MediaSession2 mMediaSession;
    private String mTitle;
    private Executor mCallbackExecutor;

    private WindowManager mManager;
    private View mMusicView;
    private Drawable mMusicAlbumDrawable;
    private String mMusicTitleText;
    private String mMusicArtistText;
    private boolean mIsMusicMediaType;
    private int mPrevWidth;
    private int mPrevHeight;
    private int mDominantColor;
    private int mSizeType;

    private int mTargetState = STATE_IDLE;
    private int mCurrentState = STATE_IDLE;
    private long mSeekWhenPrepared;  // recording the seek position while preparing

    private int mVideoWidth;
    private int mVideoHeight;

    private ArrayList<Integer> mVideoTrackIndices;
    private ArrayList<Integer> mAudioTrackIndices;
    private ArrayList<Pair<Integer, SubtitleTrack>> mSubtitleTrackIndices;
    private SubtitleController mSubtitleController;

    // selected audio/subtitle track index as MediaPlayer returns
    private int mSelectedAudioTrackIndex;
    private int mSelectedSubtitleTrackIndex;

    private SubtitleAnchorView mSubtitleAnchorView;
    private boolean mSubtitleEnabled;

    private VideoView2 mInstance;

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
                      /*
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
                    */
                    }
                });
                // Start remote playback (if necessary)
                // TODO: b/77556429
                if (mMediaItem != null) {
                    DataSourceDesc dsd = mMediaItem.getDataSourceDesc();
                    if (dsd != null && dsd.getUri() != null) {
                        mRoutePlayer.openVideo(dsd.getUri());
                    }
                }
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
                openVideo();
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
        mSelectedSubtitleTrackIndex = INVALID_TRACK_INDEX;

        mAudioAttributes = new AudioAttributesCompat.Builder()
                .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                .setContentType(AudioAttributesCompat.CONTENT_TYPE_MOVIE).build();

        mCallbackExecutor = MainHandlerExecutor.getExecutor(context);

        mInstance.setFocusable(true);
        mInstance.setFocusableInTouchMode(true);
        mInstance.requestFocus();

        mTextureView = new VideoTextureView(context);
        mSurfaceView = new VideoSurfaceView(context);
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        mTextureView.setLayoutParams(params);
        mSurfaceView.setLayoutParams(params);
        mTextureView.setSurfaceListener(this);
        mSurfaceView.setSurfaceListener(this);

        mInstance.addView(mTextureView);
        mInstance.addView(mSurfaceView);

        mSubtitleAnchorView = new SubtitleAnchorView(context);
        mSubtitleAnchorView.setLayoutParams(params);
        mSubtitleAnchorView.setBackgroundColor(0);
        mInstance.addView(mSubtitleAnchorView);

        boolean enableControlView = (attrs == null) || attrs.getAttributeBooleanValue(
                "http://schemas.android.com/apk/res/android",
                "enableControlView", true);
        if (enableControlView) {
            mMediaControlView = new MediaControlView2(context);
        }

        mSubtitleEnabled = (attrs == null) || attrs.getAttributeBooleanValue(
                "http://schemas.android.com/apk/res/android",
                "enableSubtitle", false);

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
        mMediaControlView.setRouteSelector(mRouteSelector);
        mMediaControlView.setShowControllerInterval(intervalMs);

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
     */
    @Override
    public void setMediaMetadata(MediaMetadata2 metadata) {
        if (mMediaItem != null) {
            mMediaItem.setMetadata(metadata);
        }
    }

    /**
     * Returns MediaMetadata2 instance which is retrieved from MediaPlayer inside VideoView2 by
     * default or by {@link #setMediaMetadata} method.
     */
    @Override
    public MediaMetadata2 getMediaMetadata() {
        return (mMediaItem != null) ? mMediaItem.getMetadata() : null;
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
        return null;
    }

    /**
     * Returns {@link SessionToken2} so that developers create their own
     * {@link androidx.media.MediaController2} instance. This method should be called when
     * VideoView2 is attached to window, or it throws IllegalStateException.
     *
     * @throws IllegalStateException if interal MediaSession is not created yet.
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public SessionToken2 getMediaSessionToken() {
        checkMediaSession();
        return mMediaSession.getToken();
    }

    /**
     * Shows or hides closed caption or subtitles if there is any.
     * The first subtitle track will be chosen if there multiple subtitle tracks exist.
     * Default behavior of VideoView2 is not showing subtitle.
     * @param enable shows closed caption or subtitles if this value is true, or hides.
     */
    @Override
    public void setSubtitleEnabled(boolean enable) {
        if (enable != mSubtitleEnabled) {
            selectOrDeselectSubtitle(enable);
        }
        mSubtitleEnabled = enable;
    }

    /**
     * Returns true if showing subtitle feature is enabled or returns false.
     * Although there is no subtitle track or closed caption, it can return true, if the feature
     * has been enabled by {@link #setSubtitleEnabled}.
     */
    @Override
    public boolean isSubtitleEnabled() {
        return mSubtitleEnabled;
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
        checkMediaSession();
        mMediaSession.setPlaybackSpeed(speed);
    }

    /**
     * Returns playback speed.
     *
     * It returns the same value that has been set by {@link #setSpeed}, if it was available value.
     * If {@link #setSpeed} has not been called before, then the normal speed 1.0f will be returned.
     */
    @Override
    public float getSpeed() {
        checkMediaSession();
        return mMediaSession.getPlaybackSpeed();
    }

    /**
     * Sets the {@link AudioAttributesCompat} to be used during the playback of the video.
     *
     * @param attributes non-null <code>AudioAttributesCompat</code>.
     */
    @Override
    public void setAudioAttributes(@NonNull AudioAttributesCompat attributes) {
        if (attributes == null) {
            throw new IllegalArgumentException("Illegal null AudioAttributes");
        }
        mAudioAttributes = attributes;
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
        DataSourceDesc.Builder builder = new DataSourceDesc.Builder();
        builder.setDataSource(mInstance.getContext(), uri, headers, null);
        setDataSource(builder.build());
    }

    /**
     * Sets {@link MediaItem2} object to render using VideoView2. Alternative way to set media
     * object to VideoView2 is {@link #setDataSource}.
     * @param mediaItem the MediaItem2 to play
     * @see #setDataSource
     *
     * @hide TODO unhide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void setMediaItem(@NonNull MediaItem2 mediaItem) {
        // TODO add an API such as "setPlayList(List<MediaItem2>)"
        if (mPlayList != null) {
            mPlayList.clear();
        } else {
            mPlayList = new ArrayList<MediaItem2>();
        }
        mPlayList.add(mediaItem);
        mMediaItem = mediaItem;
        mSeekWhenPrepared = 0;
        openVideo();
    }

    /**
     * Sets {@link DataSourceDesc} object to render using VideoView2.
     * @param dataSource the {@link DataSourceDesc} object to play.
     * @see #setMediaItem
     *
     * @hide TODO unhide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void setDataSource(@NonNull DataSourceDesc dataSource) {
        MediaItem2 mediaItem = new MediaItem2.Builder(MediaItem2.FLAG_PLAYABLE)
                .setDataSourceDesc(dataSource)
                .build();
        setMediaItem(mediaItem);
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
        VideoViewInterface targetView;
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
    @VideoView2.ViewType
    @Override
    public int getViewType() {
        return mCurrentView.getViewType();
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
        // Note: MediaPlayer2 and MediaSession2 instances are created in onAttachedToWindow()
        // and closed in onDetachedFromWindow().
        if (mMediaPlayer == null) {
            mMediaPlayer = MediaPlayer2.create();

            mSurfaceView.setMediaPlayer(mMediaPlayer);
            mTextureView.setMediaPlayer(mMediaPlayer);
            mCurrentView.assignSurfaceToMediaPlayer(mMediaPlayer);

            if (mMediaSession != null) {
                mMediaSession.updatePlayer(mMediaPlayer.getBaseMediaPlayer(), null, null);
            }
        } else {
            if (!mCurrentView.assignSurfaceToMediaPlayer(mMediaPlayer)) {
                Log.w(TAG, "failed to assign surface");
            }
        }

        if (mMediaSession == null) {
            final Context context = mInstance.getContext();
            mMediaSession = new MediaSession2.Builder(context)
                    .setPlayer(mMediaPlayer.getBaseMediaPlayer())
                    .setId("VideoView2_" + mInstance.toString())
                    .setSessionCallback(mCallbackExecutor, new MediaSessionCallback())
                    .build();
        }

        attachMediaControlView();
        mMediaRouter = MediaRouter.getInstance(mInstance.getContext());
        /*
        (b/79723218)
        mMediaRouter.setMediaSession2(mMediaSession);
        mMediaRouter.addCallback(mRouteSelector, mRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
                */
    }

    @Override
    public void onDetachedFromWindowImpl() {
        mMediaPlayer.close();
        mMediaSession.close();
        mMediaPlayer = null;
        mMediaSession = null;
    }

    @Override
    public void onTouchEventImpl(MotionEvent ev) {
        if (DEBUG) {
            Log.d(TAG, "onTouchEvent(). mCurrentState=" + mCurrentState
                    + ", mTargetState=" + mTargetState);
        }
    }

    @Override
    public void onTrackballEventImpl(MotionEvent ev) {
        if (DEBUG) {
            Log.d(TAG, "onTrackBallEvent(). mCurrentState=" + mCurrentState
                    + ", mTargetState=" + mTargetState);
        }
    }

    @Override
    public void onMeasureImpl(int widthMeasureSpec, int heightMeasureSpec) {
        if (mIsMusicMediaType) {
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
                        // TODO: remove MCV2 callback
                    }
                } else {
                    if (mSizeType != SIZE_TYPE_EMBEDDED) {
                        mSizeType = SIZE_TYPE_EMBEDDED;
                        inflateMusicView(R.layout.embedded_music);
                        // TODO: remove MCV2 callback
                    }
                }
                mPrevWidth = currWidth;
                mPrevHeight = currHeight;
            }
        }
    }

    ///////////////////////////////////////////////////
    // Implements VideoViewInterface.SurfaceListener
    ///////////////////////////////////////////////////

    /**
     * @hide
     */
    @Override
    @RestrictTo(LIBRARY_GROUP)
    public void onSurfaceCreated(View view, int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated(). mCurrentState=" + mCurrentState
                    + ", mTargetState=" + mTargetState + ", width/height: " + width + "/" + height
                    + ", " + view.toString());
        }
        if (needToStart()) {
            mMediaSession.play();
        }
    }

    /**
     * @hide
     */
    @Override
    @RestrictTo(LIBRARY_GROUP)
    public void onSurfaceDestroyed(View view) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceDestroyed(). mCurrentState=" + mCurrentState
                    + ", mTargetState=" + mTargetState + ", " + view.toString());
        }
    }

    /**
     * @hide
     */
    @Override
    @RestrictTo(LIBRARY_GROUP)
    public void onSurfaceChanged(View view, int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceChanged(). width/height: " + width + "/" + height
                    + ", " + view.toString());
        }
    }

    /**
     * @hide
     */
    @Override
    @RestrictTo(LIBRARY_GROUP)
    public void onSurfaceTakeOverDone(VideoViewInterface view) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceTakeOverDone(). Now current view is: " + view);
        }
        mCurrentView = view;
        if (mViewTypeChangedListener != null) {
            mViewTypeChangedListener.onViewTypeChanged(mInstance, view.getViewType());
        }

        if (needToStart()) {
            mMediaSession.play();
        }
    }

    ///////////////////////////////////////////////////
    // Protected or private methods
    ///////////////////////////////////////////////////
    private void checkMediaSession() {
        if (mMediaSession == null) {
            throw new IllegalStateException("MediaSession instance is not available.");
        }
    }

    private void attachMediaControlView() {
        // Get MediaController from MediaSession and set it inside MediaControlView
        mMediaControlView.setMediaSessionToken(mMediaSession.getToken());

        LayoutParams params =
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mInstance.addView(mMediaControlView, params);
    }

    private boolean isInPlaybackState() {
        return mMediaSession != null
                && mMediaSession.getPlayerState() != BaseMediaPlayer.PLAYER_STATE_ERROR
                && mMediaSession.getPlayerState() != BaseMediaPlayer.PLAYER_STATE_IDLE;
    }

    private boolean needToStart() {
        return (mMediaPlayer != null || mRoutePlayer != null) && isWaitingPlayback();
    }

    private boolean isWaitingPlayback() {
        return mCurrentState != STATE_PLAYING && mTargetState == STATE_PLAYING;
    }

    // Creates a MediaPlayer instance and prepare playback.
    private void openVideo() {
        if (DEBUG) {
            Log.d(TAG, "openVideo()");
        }
        DataSourceDesc dsd = mMediaItem.getDataSourceDesc();
        Uri uri = null;
        if (dsd != null) {
            uri = dsd.getUri();
            resetPlayer();
            if (isRemotePlayback()) {
                // TODO: b/77556429
                mRoutePlayer.openVideo(uri);
                return;
            }
        }

        try {
            if (mMediaPlayer == null) {
                mMediaPlayer = MediaPlayer2.create();
            }
            mSurfaceView.setMediaPlayer(mMediaPlayer);
            mTextureView.setMediaPlayer(mMediaPlayer);
            if (!mCurrentView.assignSurfaceToMediaPlayer(mMediaPlayer)) {
                Log.w(TAG, "failed to assign surface");
            }
            mMediaPlayer.setEventCallback(mCallbackExecutor, mMediaPlayer2Callback);
            mMediaPlayer.setAudioAttributes(mAudioAttributes);

            if (mMediaSession == null) {
                final Context context = mInstance.getContext();
                mMediaSession = new MediaSession2.Builder(context)
                        .setPlayer(mMediaPlayer.getBaseMediaPlayer())
                        .setId("VideoView2_" + mInstance.toString())
                        .setSessionCallback(mCallbackExecutor, new MediaSessionCallback())
                        .build();
            } else {
                mMediaSession.updatePlayer(mMediaPlayer.getBaseMediaPlayer(), null, null);
            }

            final Context context = mInstance.getContext();
            mSubtitleController = new SubtitleController(context);
            mSubtitleController.registerRenderer(new ClosedCaptionRenderer(context));
            mSubtitleController.registerRenderer(new Cea708CaptionRenderer(context));
            mSubtitleController.setAnchor((SubtitleController.Anchor) mSubtitleAnchorView);

            mMediaSession.setPlaylist(mPlayList, null);

            // we don't set the target state here either, but preserve the
            // target state that was there before.
            mCurrentState = STATE_PREPARING;
            mMediaSession.prepare();

            // Save file name as title since the file may not have a title Metadata.
            mTitle = uri != null ? uri.getPath() : null;
            String scheme = uri != null ? uri.getScheme() : null;
            if (scheme != null && scheme.equals("file")) {
                mTitle = uri.getLastPathSegment();
            }

        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "Unable to open content: " + mMediaItem, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mMediaPlayer2Callback.onError(mMediaPlayer, dsd,
                    MediaPlayer2.MEDIA_ERROR_UNKNOWN, MediaPlayer2.MEDIA_ERROR_IO);
        }
    }

    /*
     * Reset the media player in any state
     */
    private void resetPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mTextureView.setMediaPlayer(null);
            mSurfaceView.setMediaPlayer(null);
            mCurrentState = STATE_IDLE;
            mTargetState = STATE_IDLE;
        }
        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    private boolean isRemotePlayback() {
        return mRoutePlayer != null
                && mMediaSession != null && mMediaSession.getVolumeProvider() != null;
    }

    private void selectOrDeselectSubtitle(boolean select) {
        if (!isInPlaybackState()) {
            return;
        }
        if (select) {
            if (mSubtitleTrackIndices.size() > 0) {
                mSelectedSubtitleTrackIndex = mSubtitleTrackIndices.get(0).first;
                mSubtitleController.selectTrack(mSubtitleTrackIndices.get(0).second);
                mMediaPlayer.selectTrack(mSelectedSubtitleTrackIndex);
                mSubtitleAnchorView.setVisibility(View.VISIBLE);
            }
        } else {
            if (mSelectedSubtitleTrackIndex != INVALID_TRACK_INDEX) {
                mMediaPlayer.deselectTrack(mSelectedSubtitleTrackIndex);
                mSelectedSubtitleTrackIndex = INVALID_TRACK_INDEX;
                mSubtitleAnchorView.setVisibility(View.GONE);
            }
        }
    }

    private void extractTracks() {
        List<MediaPlayer2.TrackInfo> trackInfos = mMediaPlayer.getTrackInfo();
        mVideoTrackIndices = new ArrayList<>();
        mAudioTrackIndices = new ArrayList<>();
        mSubtitleTrackIndices = new ArrayList<>();
        mSubtitleController.reset();
        for (int i = 0; i < trackInfos.size(); ++i) {
            int trackType = trackInfos.get(i).getTrackType();
            if (trackType == MediaPlayer2.TrackInfo.MEDIA_TRACK_TYPE_VIDEO) {
                mVideoTrackIndices.add(i);
            } else if (trackType == MediaPlayer2.TrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
                mAudioTrackIndices.add(i);
            } else if (trackType == MediaPlayer2.TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE) {
                SubtitleTrack track = mSubtitleController.addTrack(trackInfos.get(i).getFormat());
                if (track != null) {
                    mSubtitleTrackIndices.add(new Pair<>(i, track));
                }
            }
        }
        // Select first tracks as default
        if (mAudioTrackIndices.size() > 0) {
            mSelectedAudioTrackIndex = 0;
        }
        if (mVideoTrackIndices.size() == 0 && mAudioTrackIndices.size() > 0) {
            mIsMusicMediaType = true;
        }

        Bundle data = new Bundle();
        data.putInt(MediaControlView2.KEY_VIDEO_TRACK_COUNT, mVideoTrackIndices.size());
        data.putInt(MediaControlView2.KEY_AUDIO_TRACK_COUNT, mAudioTrackIndices.size());
        data.putInt(MediaControlView2.KEY_SUBTITLE_TRACK_COUNT, mSubtitleTrackIndices.size());
        if (mSubtitleTrackIndices.size() > 0) {
            selectOrDeselectSubtitle(mSubtitleEnabled);
        }
        mMediaSession.sendCustomCommand(
                new SessionCommand2(MediaControlView2.EVENT_UPDATE_TRACK_STATUS, null), data);
    }

    private void extractMetadata() {
        if (mMediaItem.getMetadata() != null) {
            // If a MediaItem2 instance has its own metadata, then use it.
            // TODO: merge metadata from metadata retriever
            return;
        }

        DataSourceDesc dsd = mMediaItem.getDataSourceDesc();
        Uri uri = dsd != null ? dsd.getUri() : null;
        if (uri == null) {
            // Something wrong.
            return;
        }

        // Save file name as title since the file may not have a title Metadata.
        String scheme = uri.getScheme();
        if (scheme != null && scheme.equals("file")) {
            mTitle = uri.getLastPathSegment();
        } else {
            mTitle = uri.getPath();
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(mInstance.getContext(), uri);
        } catch (IllegalArgumentException e) {
            Log.v(TAG, "Cannot retrieve metadata for this media file.");
            retriever.release();
            return;
        }

        String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        if (title != null) {
            mTitle = title;
        }

        if (!mIsMusicMediaType) {
            retriever.release();
            return;
        }

        // From here, extract audio metadata for Music UI.
        Resources resources = mInstance.getResources();
        mManager = (WindowManager) mInstance.getContext().getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);

        byte[] album = retriever.getEmbeddedPicture();
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
            mMusicAlbumDrawable = resources.getDrawable(R.drawable.ic_default_album_image);
        }

        title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        if (title != null) {
            mMusicTitleText = title;
        } else {
            mMusicTitleText = resources.getString(R.string.mcv2_music_title_unknown_text);
        }

        String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        if (artist != null) {
            mMusicArtistText = artist;
        } else {
            mMusicArtistText = resources.getString(R.string.mcv2_music_artist_unknown_text);
        }

        retriever.release();

        // Display Embedded mode as default
        mInstance.removeView(mSurfaceView);
        mInstance.removeView(mTextureView);
        inflateMusicView(R.layout.embedded_music);
    }

    private void sendMetadata() {
        // Get and set duration and title values as MediaMetadata for MediaControlView2
        MediaMetadata2.Builder builder = new MediaMetadata2.Builder();

        if (mIsMusicMediaType) {
            builder.putString(MediaMetadata2.METADATA_KEY_TITLE, mMusicTitleText);
            builder.putString(MediaMetadata2.METADATA_KEY_ARTIST, mMusicArtistText);
        } else {
            builder.putString(MediaMetadata2.METADATA_KEY_TITLE, mTitle);
        }
        builder.putLong(
                MediaMetadata2.METADATA_KEY_DURATION, mMediaSession.getDuration());
        builder.putString(
                MediaMetadata2.METADATA_KEY_MEDIA_ID, mMediaItem.getMediaId());
        mMediaItem.setMetadata(builder.build());
        mPlayList.set(0, mMediaItem);
        mMediaSession.setPlaylist(mPlayList, null);
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

    MediaPlayer2.EventCallback mMediaPlayer2Callback =
            new MediaPlayer2.EventCallback() {
                @Override
                public void onVideoSizeChanged(
                        MediaPlayer2 mp, DataSourceDesc dsd, int width, int height) {
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

                @Override
                public void onInfo(
                        MediaPlayer2 mp, DataSourceDesc dsd, int what, int extra) {
                    if (DEBUG) {
                        Log.d(TAG, "onInfo()");
                    }
                    if (what == MediaPlayer2.MEDIA_INFO_METADATA_UPDATE) {
                        extractTracks();
                    } else if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                        this.onPrepared(mp, dsd);
                    } else if (what == MediaPlayer2.MEDIA_INFO_PLAYBACK_COMPLETE) {
                        this.onCompletion(mp, dsd);
                    }
                }

                @Override
                public void onError(
                        MediaPlayer2 mp, DataSourceDesc dsd, int frameworkErr, int implErr) {
                    if (DEBUG) {
                        Log.d(TAG, "Error: " + frameworkErr + "," + implErr);
                    }
                    mCurrentState = STATE_ERROR;
                    mTargetState = STATE_ERROR;

                    if (mMediaControlView != null) {
                        mMediaControlView.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onSubtitleData(
                        MediaPlayer2 mp, DataSourceDesc dsd, SubtitleData2 data) {
                    if (DEBUG) {
                        Log.d(TAG, "onSubtitleData(): getTrackIndex: " + data.getTrackIndex()
                                + ", getCurrentPosition: " + mp.getCurrentPosition()
                                + ", getStartTimeUs(): " + data.getStartTimeUs()
                                + ", diff: "
                                + (data.getStartTimeUs() / 1000 - mp.getCurrentPosition())
                                + "ms, getDurationUs(): " + data.getDurationUs());
                    }
                    final int index = data.getTrackIndex();
                    if (index != mSelectedSubtitleTrackIndex) {
                        Log.w(TAG, "onSubtitleData(): getTrackIndex: " + data.getTrackIndex()
                                + ", selected track index: " + mSelectedSubtitleTrackIndex);
                        return;
                    }
                    for (Pair<Integer, SubtitleTrack> p : mSubtitleTrackIndices) {
                        if (p.first == index) {
                            SubtitleTrack track = p.second;
                            track.onData(data);
                        }
                    }
                }

                private void onPrepared(MediaPlayer2 mp, DataSourceDesc dsd) {
                    if (DEBUG) {
                        Log.d(TAG, "OnPreparedListener(): "
                                + ", mCurrentState=" + mCurrentState
                                + ", mTargetState=" + mTargetState);
                    }
                    mCurrentState = STATE_PREPARED;

                    if (mMediaSession != null) {
                        extractTracks();
                        extractMetadata();
                        sendMetadata();
                    }

                    if (mMediaControlView != null) {
                        mMediaControlView.setEnabled(true);
                    }
                    int videoWidth = mp.getVideoWidth();
                    int videoHeight = mp.getVideoHeight();

                    // mSeekWhenPrepared may be changed after seekTo() call
                    long seekToPosition = mSeekWhenPrepared;
                    if (seekToPosition != 0) {
                        mMediaSession.seekTo(seekToPosition);
                    }

                    if (videoWidth != 0 && videoHeight != 0) {
                        if (videoWidth != mVideoWidth || videoHeight != mVideoHeight) {
                            mVideoWidth = videoWidth;
                            mVideoHeight = videoHeight;
                            mInstance.requestLayout();
                        }

                        if (needToStart()) {
                            mMediaSession.play();
                        }

                    } else {
                        // We don't know the video size yet, but should start anyway.
                        // The video size might be reported to us later.
                        if (needToStart()) {
                            mMediaSession.play();
                        }
                    }
                }

                private void onCompletion(MediaPlayer2 mp, DataSourceDesc dsd) {
                    mCurrentState = STATE_PLAYBACK_COMPLETED;
                    mTargetState = STATE_PLAYBACK_COMPLETED;
                }
            };

    class MediaSessionCallback extends MediaSession2.SessionCallback {
        @Override
        public SessionCommandGroup2 onConnect(
                @NonNull MediaSession2 session,
                @NonNull MediaSession2.ControllerInfo controller) {
            SessionCommandGroup2 commands = new SessionCommandGroup2();
            commands.addAllPredefinedCommands();
            commands.addCommand(new SessionCommand2(MediaControlView2.COMMAND_SHOW_SUBTITLE, null));
            commands.addCommand(new SessionCommand2(MediaControlView2.COMMAND_HIDE_SUBTITLE, null));
            commands.addCommand(new SessionCommand2(
                    MediaControlView2.COMMAND_SELECT_AUDIO_TRACK, null));
            commands.addCommand(new SessionCommand2(
                    MediaControlView2.COMMAND_SET_PLAYBACK_SPEED, null));
            commands.addCommand(new SessionCommand2(MediaControlView2.COMMAND_MUTE, null));
            commands.addCommand(new SessionCommand2(MediaControlView2.COMMAND_UNMUTE, null));
            return commands;
        }

        @Override
        public void onCustomCommand(@NonNull MediaSession2 session,
                @NonNull MediaSession2.ControllerInfo controller,
                @NonNull SessionCommand2 customCommand,
                @Nullable Bundle args, @Nullable ResultReceiver cb) {
            if (isRemotePlayback()) {
                // TODO: call mRoutePlayer.onCommand()
                return;
            }
            switch (customCommand.getCustomCommand()) {
                case MediaControlView2.COMMAND_SHOW_SUBTITLE:
                    int subtitleIndex = args != null ? args.getInt(
                            MediaControlView2.KEY_SELECTED_SUBTITLE_INDEX,
                            INVALID_TRACK_INDEX) : INVALID_TRACK_INDEX;
                    if (subtitleIndex != INVALID_TRACK_INDEX) {
                        int subtitleTrackIndex = mSubtitleTrackIndices.get(subtitleIndex).first;
                        if (subtitleTrackIndex != mSelectedSubtitleTrackIndex) {
                            mSelectedSubtitleTrackIndex = subtitleTrackIndex;
                            mInstance.setSubtitleEnabled(true);
                        }
                    }
                    break;
                case MediaControlView2.COMMAND_HIDE_SUBTITLE:
                    mInstance.setSubtitleEnabled(false);
                    break;
                case MediaControlView2.COMMAND_SELECT_AUDIO_TRACK:
                    int audioIndex = (args != null)
                            ? args.getInt(MediaControlView2.KEY_SELECTED_AUDIO_INDEX,
                            INVALID_TRACK_INDEX) : INVALID_TRACK_INDEX;
                    if (audioIndex != INVALID_TRACK_INDEX) {
                        int audioTrackIndex = mAudioTrackIndices.get(audioIndex);
                        if (audioTrackIndex != mSelectedAudioTrackIndex) {
                            mSelectedAudioTrackIndex = audioTrackIndex;
                            mMediaPlayer.selectTrack(mSelectedAudioTrackIndex);
                        }
                    }
                    break;
            }
        }

        @Override
        public boolean onCommandRequest(@NonNull MediaSession2 session,
                @NonNull MediaSession2.ControllerInfo controller,
                @NonNull SessionCommand2 command) {
            switch(command.getCommandCode()) {
                case SessionCommand2.COMMAND_CODE_PLAYBACK_PLAY:
                    mTargetState = STATE_PLAYING;
                    if (!mCurrentView.hasAvailableSurface()) {
                        Log.d(TAG, "surface is not available");
                        return false;
                    }
                    break;
                case SessionCommand2.COMMAND_CODE_PLAYBACK_PAUSE:
                    mTargetState = STATE_PAUSED;
                    break;
                case SessionCommand2.COMMAND_CODE_PLAYBACK_SEEK_TO:
                    mSeekWhenPrepared = 0;
                    break;
            }
            return true;
        }

        @Override
        public void onPlayerStateChanged(@NonNull MediaSession2 session,
                @NonNull BaseMediaPlayer player, @BaseMediaPlayer.PlayerState int state) {
            switch(state) {
                case BaseMediaPlayer.PLAYER_STATE_IDLE:
                    mCurrentState = STATE_IDLE;
                    break;
                case BaseMediaPlayer.PLAYER_STATE_PLAYING:
                    mCurrentState = STATE_PLAYING;
                    break;
                case BaseMediaPlayer.PLAYER_STATE_PAUSED:
                    mCurrentState = STATE_PAUSED;
                    break;
                case BaseMediaPlayer.PLAYER_STATE_ERROR:
                    mCurrentState = STATE_ERROR;
                    break;
            }
        }

        @Override
        public void onMediaPrepared(@NonNull MediaSession2 session,
                @NonNull BaseMediaPlayer player, @NonNull MediaItem2 item) {
            Log.d(TAG, "onMediaPrepared() is called.");
        }
    }
}
