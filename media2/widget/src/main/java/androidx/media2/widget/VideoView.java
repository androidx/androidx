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

package androidx.media2.widget;

import static androidx.media2.session.SessionResult.RESULT_ERROR_INVALID_STATE;
import static androidx.media2.session.SessionResult.RESULT_SUCCESS;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.media.AudioAttributesCompat;
import androidx.media2.common.FileMediaItem;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.UriMediaItem;
import androidx.media2.player.MediaPlayer;
import androidx.media2.player.MediaPlayer.TrackInfo;
import androidx.media2.player.SubtitleData;
import androidx.media2.player.VideoSize;
import androidx.media2.player.subtitle.Cea708CaptionRenderer;
import androidx.media2.player.subtitle.ClosedCaptionRenderer;
import androidx.media2.player.subtitle.SubtitleController;
import androidx.media2.player.subtitle.SubtitleTrack;
import androidx.media2.session.MediaController;
import androidx.media2.session.MediaSession;
import androidx.media2.session.RemoteSessionPlayer;
import androidx.media2.session.SessionCommand;
import androidx.media2.session.SessionCommandGroup;
import androidx.media2.session.SessionResult;
import androidx.media2.session.SessionToken;
import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;
import androidx.palette.graphics.Palette;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;

/**
 * Displays a video file.  VideoView class is a ViewGroup class which is wrapping
 * {@link MediaPlayer} so that developers can easily implement a video rendering application.
 *
 * <p>
 * <em> Data sources that VideoView supports : </em>
 * VideoView can play video files and audio-only files as well. It can load from various sources
 * such as resources or content providers. The supported media file formats are the same as
 * {@link android.media.MediaPlayer}.
 *
 * <p>
 * <em> View type can be selected : </em>
 * VideoView can render videos on top of TextureView as well as
 * SurfaceView selectively. The default is SurfaceView and it can be changed using
 * {@link #setViewType(int)} method. Using SurfaceView is recommended in most cases for saving
 * battery. TextureView might be preferred for supporting various UIs such as animation and
 * translucency.
 *
 * <p>
 * <em> Differences between {@link android.widget.VideoView} class : </em>
 * {@link VideoView} covers and inherits the most of
 * {@link android.widget.VideoView}'s functionality. The main differences are
 * <ul>
 * <li> {@link VideoView} inherits ViewGroup and renders videos using SurfaceView and TextureView
 * selectively while {@link android.widget.VideoView} inherits SurfaceView class.
 * <li> {@link VideoView} is integrated with {@link MediaControlView} and
 * a default MediaControlView instance is attached to this VideoView by default.
 * <li> If a developer wants to attach a custom MediaControlView,
 * assign the custom media control widget using {@link #setMediaControlView}.
 * <li> {@link VideoView} is integrated with {@link MediaSession} and so
 * it responses with media key events.
 * </ul>
 *
 * <p>
 * <em> Audio focus and audio attributes : </em>
 * VideoView requests audio focus with {@link AudioManager#AUDIOFOCUS_GAIN} internally,
 * when playing a media content.
 * The default {@link AudioAttributesCompat} used during playback have a usage of
 * {@link AudioAttributesCompat#USAGE_MEDIA} and a content type of
 * {@link AudioAttributesCompat#CONTENT_TYPE_MOVIE},
 * use {@link #setAudioAttributes(AudioAttributesCompat)} to modify them.
 *
 * <p>
 * Note: VideoView does not retain its full state when going into the background. In particular, it
 * does not restore the current play state, play position, selected tracks. Applications should save
 * and restore these on their own in {@link android.app.Activity#onSaveInstanceState} and
 * {@link android.app.Activity#onRestoreInstanceState}.
 * {@link androidx.media2.widget.R.attr#enableControlView}
 * {@link androidx.media2.widget.R.attr#viewType}
 */
public class VideoView extends SelectiveLayout {
    @IntDef({
            VIEW_TYPE_TEXTUREVIEW,
            VIEW_TYPE_SURFACEVIEW
    })
    @Retention(RetentionPolicy.SOURCE)
    /* package */ @interface ViewType {}

    /**
     * Indicates video is rendering on SurfaceView.
     *
     * @see #setViewType
     */
    public static final int VIEW_TYPE_SURFACEVIEW = 0;

    /**
     * Indicates video is rendering on TextureView.
     *
     * @see #setViewType
     */
    public static final int VIEW_TYPE_TEXTUREVIEW = 1;

    private static final String TAG = "VideoView";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    private static final int INVALID_TRACK_INDEX = -1;

    private AudioAttributesCompat mAudioAttributes;

    VideoView.OnViewTypeChangedListener mViewTypeChangedListener;

    VideoViewInterface mCurrentView;
    VideoViewInterface mTargetView;
    VideoTextureView mTextureView;
    VideoSurfaceView mSurfaceView;

    MediaPlayer mMediaPlayer;
    MediaItem mMediaItem;
    MediaControlView mMediaControlView;
    MediaSession mMediaSession;
    String mTitle;
    Executor mCallbackExecutor;

    MusicView mMusicView;
    Drawable mMusicAlbumDrawable;
    String mMusicArtistText;

    int mDominantColor;

    int mTargetState = STATE_IDLE;
    int mCurrentState = STATE_IDLE;

    private int mVideoTrackCount;
    List<TrackInfo> mAudioTrackInfos;
    Map<TrackInfo, SubtitleTrack> mSubtitleTracks;
    private SubtitleController mSubtitleController;

    // selected audio/subtitle track info as MediaPlayer returns
    TrackInfo mSelectedAudioTrackInfo;
    TrackInfo mSelectedSubtitleTrackInfo;

    SubtitleAnchorView mSubtitleAnchorView;

    private MediaRouter mMediaRouter;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
            MediaRouteSelector mRouteSelector;
    MediaRouter.RouteInfo mRoute;
    RoutePlayer mRoutePlayer;

    private final MediaRouter.Callback mRouterCallback = new MediaRouter.Callback() {
        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            if (route.supportsControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)) {
                // Save local playback state and position
                int localPlaybackState = mCurrentState;
                long localPlaybackPosition = (mMediaSession == null)
                        ? 0 : mMediaSession.getPlayer().getCurrentPosition();

                // Update player
                resetPlayer();
                mRoute = route;
                mRoutePlayer = new RoutePlayer(getContext(), mRouteSelector, route);
                // TODO: Replace with MediaSession#setPlaylist once b/110811730 is fixed.
                mRoutePlayer.setMediaItem(mMediaItem);
                mRoutePlayer.setCurrentPosition(localPlaybackPosition);
                ensureSessionWithPlayer(mRoutePlayer);
                if (localPlaybackState == STATE_PLAYING) {
                    mMediaSession.getPlayer().play();
                }
            }
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route, int reason) {
            long currentPosition = 0;
            int currentState = 0;
            if (mRoute != null && mRoutePlayer != null) {
                currentPosition = mRoutePlayer.getCurrentPosition();
                currentState = mRoutePlayer.getPlayerState();
                mRoutePlayer.close();
                mRoutePlayer = null;
            }
            if (mRoute == route) {
                mRoute = null;
            }
            if (reason != MediaRouter.UNSELECT_REASON_ROUTE_CHANGED) {
                openVideo();
                mMediaSession.getPlayer().seekTo(currentPosition);
                if (currentState == SessionPlayer.PLAYER_STATE_PLAYING) {
                    mMediaSession.getPlayer().play();
                }
            }
        }
    };

    private final VideoViewInterface.SurfaceListener mSurfaceListener =
            new VideoViewInterface.SurfaceListener() {
        @Override
        public void onSurfaceCreated(View view, int width, int height) {
            if (DEBUG) {
                Log.d(TAG, "onSurfaceCreated(). mCurrentState=" + mCurrentState
                        + ", mTargetState=" + mTargetState
                        + ", width/height: " + width + "/" + height
                        + ", " + view.toString());
            }
            if (view == mTargetView) {
                ((VideoViewInterface) view).takeOver();
            }
            if (needToStart()) {
                mMediaSession.getPlayer().play();
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
        public void onSurfaceTakeOverDone(VideoViewInterface view) {
            if (view != mTargetView) {
                if (DEBUG) {
                    Log.d(TAG, "onSurfaceTakeOverDone(). view is not targetView. ignore.: " + view);
                }
                return;
            }
            if (DEBUG) {
                Log.d(TAG, "onSurfaceTakeOverDone(). Now current view is: " + view);
            }
            if (view != mCurrentView) {
                ((View) mCurrentView).setVisibility(View.GONE);
                mCurrentView = view;
                if (mViewTypeChangedListener != null) {
                    mViewTypeChangedListener.onViewTypeChanged(VideoView.this, view.getViewType());
                }
            }

            if (needToStart()) {
                mMediaSession.getPlayer().play();
            }
        }
    };

    public VideoView(@NonNull Context context) {
        this(context, null);
    }

    public VideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs);
    }

    private void initialize(Context context, @Nullable AttributeSet attrs) {
        mSelectedSubtitleTrackInfo = null;

        mAudioAttributes = new AudioAttributesCompat.Builder()
                .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                .setContentType(AudioAttributesCompat.CONTENT_TYPE_MOVIE).build();

        mCallbackExecutor = ContextCompat.getMainExecutor(context);

        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();

        mTextureView = new VideoTextureView(context);
        mSurfaceView = new VideoSurfaceView(context);
        mTextureView.setSurfaceListener(mSurfaceListener);
        mSurfaceView.setSurfaceListener(mSurfaceListener);

        addView(mTextureView);
        addView(mSurfaceView);

        SelectiveLayout.LayoutParams params = new SelectiveLayout.LayoutParams();
        params.forceMatchParent = true;

        mSubtitleAnchorView = new SubtitleAnchorView(context);
        mSubtitleAnchorView.setBackgroundColor(0);
        addView(mSubtitleAnchorView, params);

        mMusicView = new MusicView(context);
        mMusicView.setVisibility(View.GONE);
        addView(mMusicView, params);

        boolean enableControlView = (attrs == null) || attrs.getAttributeBooleanValue(
                "http://schemas.android.com/apk/res-auto",
                "enableControlView", true);
        if (enableControlView) {
            mMediaControlView = new MediaControlView(context);
        }

        // Choose surface view by default
        int viewType = (attrs == null) ? VideoView.VIEW_TYPE_SURFACEVIEW
                : attrs.getAttributeIntValue(
                        "http://schemas.android.com/apk/res-auto",
                        "viewType", VideoView.VIEW_TYPE_SURFACEVIEW);
        if (viewType == VideoView.VIEW_TYPE_SURFACEVIEW) {
            if (DEBUG) {
                Log.d(TAG, "viewType attribute is surfaceView.");
            }
            mTextureView.setVisibility(View.GONE);
            mSurfaceView.setVisibility(View.VISIBLE);
            mCurrentView = mSurfaceView;
        } else if (viewType == VideoView.VIEW_TYPE_TEXTUREVIEW) {
            if (DEBUG) {
                Log.d(TAG, "viewType attribute is textureView.");
            }
            mTextureView.setVisibility(View.VISIBLE);
            mSurfaceView.setVisibility(View.GONE);
            mCurrentView = mTextureView;
        }
        mTargetView = mCurrentView;

        MediaRouteSelector.Builder builder = new MediaRouteSelector.Builder();
        builder.addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        builder.addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO);
        builder.addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO);
        mRouteSelector = builder.build();
    }

    /**
     * Sets MediaControlView instance. It will replace the previously assigned MediaControlView
     * instance if any.
     *
     * @param mediaControlView a media control view instance.
     * @param intervalMs a time interval in milliseconds until VideoView hides MediaControlView.
     */
    public void setMediaControlView(@NonNull MediaControlView mediaControlView, long intervalMs) {
        mMediaControlView = mediaControlView;
        mMediaControlView.setDelayedAnimationInterval(intervalMs);

        if (isAttachedToWindow()) {
            attachMediaControlView();
        }
    }

    /**
     * Returns MediaControlView instance which is currently attached to VideoView by default or by
     * {@link #setMediaControlView} method.
     */
    @Nullable
    public MediaControlView getMediaControlView() {
        return mMediaControlView;
    }

    /**
     * Returns {@link SessionToken} so that developers create their own
     * {@link MediaController} instance. This method should be called when
     * VideoView is attached to window, or it throws IllegalStateException.
     *
     * @throws IllegalStateException if internal MediaSession is not created yet.
     */
    @NonNull
    public SessionToken getSessionToken() {
        if (mMediaSession == null) {
            throw new IllegalStateException("MediaSession instance is not available.");
        }
        return mMediaSession.getToken();
    }

    /**
     * Sets the {@link AudioAttributesCompat} to be used during the playback of the video.
     *
     * @param attributes non-null <code>AudioAttributesCompat</code>.
     */
    public void setAudioAttributes(@NonNull AudioAttributesCompat attributes) {
        if (attributes == null) {
            throw new IllegalArgumentException("Illegal null AudioAttributes");
        }
        mAudioAttributes = attributes;
    }

    /**
     * Sets {@link MediaItem} object to render using VideoView.
     * <p>
     * When the media item is a {@link FileMediaItem}, the {@link ParcelFileDescriptor}
     * in the {@link FileMediaItem} will be closed by the VideoView.
     *
     * @param mediaItem the MediaItem to play
     */
    public void setMediaItem(@NonNull MediaItem mediaItem) {
        if (mMediaItem instanceof FileMediaItem) {
            ((FileMediaItem) mMediaItem).decreaseRefCount();
        }
        mMediaItem = mediaItem;
        if (mMediaItem instanceof FileMediaItem) {
            ((FileMediaItem) mMediaItem).increaseRefCount();
        }
        openVideo();
    }

    /**
     * Selects which view will be used to render video between SurfaceView and TextureView.
     * <p>
     * Note: There are two known issues on API level 28+ devices.
     * <ul>
     * <li> When changing view type to SurfaceView from TextureView in "paused" playback state,
     * a blank screen can be shown.
     * <li> When changing view type to TextureView from SurfaceView repeatedly in "paused" playback
     * state, the lastly rendered frame on TextureView can be shown.
     * </ul>
     * @param viewType the view type to render video
     * <ul>
     * <li>{@link #VIEW_TYPE_SURFACEVIEW}
     * <li>{@link #VIEW_TYPE_TEXTUREVIEW}
     * </ul>
     */
    public void setViewType(@ViewType int viewType) {
        if (viewType == mTargetView.getViewType()) {
            Log.d(TAG, "setViewType with the same type (" + viewType + ") is ignored.");
            return;
        }
        VideoViewInterface targetView;
        if (viewType == VideoView.VIEW_TYPE_TEXTUREVIEW) {
            Log.d(TAG, "switching to TextureView");
            targetView = mTextureView;
        } else if (viewType == VideoView.VIEW_TYPE_SURFACEVIEW) {
            Log.d(TAG, "switching to SurfaceView");
            targetView = mSurfaceView;
        } else {
            throw new IllegalArgumentException("Unknown view type: " + viewType);
        }

        mTargetView = targetView;
        ((View) targetView).setVisibility(View.VISIBLE);
        targetView.takeOver();
        requestLayout();
    }

    /**
     * Returns view type.
     *
     * @return view type. See {@see setViewType}.
     */
    @ViewType
    public int getViewType() {
        return mCurrentView.getViewType();
    }

    /**
     * Sets a listener to be called when a view type change is done.
     *
     * @see #setViewType(int)
     *
     * @param listener The listener to be called. A value of <code>null</code> removes any existing
     * listener.
     */
    public void setOnViewTypeChangedListener(@Nullable OnViewTypeChangedListener listener) {
        mViewTypeChangedListener = listener;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Note: MediaPlayer and MediaSession instances are created in onAttachedToWindow()
        // and closed in onDetachedFromWindow().
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer(getContext());

            mSurfaceView.setMediaPlayer(mMediaPlayer);
            mTextureView.setMediaPlayer(mMediaPlayer);
            mCurrentView.assignSurfaceToMediaPlayer(mMediaPlayer);

            if (mMediaSession != null) {
                mMediaSession.updatePlayer(mMediaPlayer);
            }
        } else {
            if (!mCurrentView.assignSurfaceToMediaPlayer(mMediaPlayer)) {
                Log.w(TAG, "failed to assign surface");
            }
        }

        ensureSessionWithPlayer(mMediaPlayer);

        attachMediaControlView();
        mMediaRouter = MediaRouter.getInstance(getContext());
        // TODO: Revisit once ag/4207152 is merged.
        mMediaRouter.setMediaSessionCompat(mMediaSession.getSessionCompat());
        mMediaRouter.addCallback(mRouteSelector, mRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        try {
            mMediaPlayer.close();
        } catch (Exception e) {
            Log.e(TAG, "Encountered an exception while performing MediaPlayer.close()", e);
        }
        mMediaSession.close();
        mMediaPlayer = null;
        mMediaSession = null;
        if (mMediaItem != null && mMediaItem instanceof FileMediaItem) {
            ((FileMediaItem) mMediaItem).decreaseRefCount();
        }
        mMediaItem = null;
    }

    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);

        if (isMediaPrepared()) {
            if (!isVisible && mCurrentState == STATE_PLAYING) {
                mMediaSession.getPlayer().pause();
            } else if (isVisible && mTargetState == STATE_PLAYING) {
                mMediaSession.getPlayer().play();
            }
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        // Class name may be obfuscated by Proguard. Hardcode the string for accessibility usage.
        return "androidx.media2.widget.VideoView";
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (DEBUG) {
            Log.d(TAG, "onTouchEvent(). mCurrentState=" + mCurrentState
                    + ", mTargetState=" + mTargetState);
        }
        return super.onTouchEvent(ev);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        if (DEBUG) {
            Log.d(TAG, "onTrackBallEvent(). mCurrentState=" + mCurrentState
                    + ", mTargetState=" + mTargetState);
        }
        return super.onTrackballEvent(ev);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    ///////////////////////////////////////////////////
    // Protected or private methods
    ///////////////////////////////////////////////////
    private void attachMediaControlView() {
        if (mMediaControlView == null) return;

        // Get MediaController from MediaSession and set it inside MediaControlView
        mMediaControlView.setSessionToken(mMediaSession.getToken());

        SelectiveLayout.LayoutParams params = new SelectiveLayout.LayoutParams();
        params.forceMatchParent = true;
        addView(mMediaControlView, params);
    }

    void ensureSessionWithPlayer(SessionPlayer player) {
        if (mMediaSession != null) {
            SessionPlayer oldPlayer = mMediaSession.getPlayer();
            if (oldPlayer == player) {
                return;
            }
            oldPlayer.unregisterPlayerCallback(mMediaPlayerCallback);
            mMediaSession.updatePlayer(player);
        } else {
            final Context context = getContext();
            mMediaSession = new MediaSession.Builder(context, player)
                    .setId("VideoView_" + toString())
                    .setSessionCallback(mCallbackExecutor, new MediaSessionCallback())
                    .build();
        }
        player.registerPlayerCallback(mCallbackExecutor, mMediaPlayerCallback);
    }

    boolean isMediaPrepared() {
        return mMediaSession != null
                && mMediaSession.getPlayer().getPlayerState() != SessionPlayer.PLAYER_STATE_ERROR
                && mMediaSession.getPlayer().getPlayerState() != SessionPlayer.PLAYER_STATE_IDLE;
    }

    boolean needToStart() {
        return mMediaSession != null
                && (mMediaPlayer != null || mRoutePlayer != null) && isWaitingPlayback();
    }

    private boolean isWaitingPlayback() {
        return mCurrentState != STATE_PLAYING && mTargetState == STATE_PLAYING;
    }

    // Creates a MediaPlayer instance and prepare media item.
    void openVideo() {
        if (DEBUG) {
            Log.d(TAG, "openVideo()");
        }
        if (mMediaItem != null) {
            resetPlayer();
            if (isRemotePlayback()) {
                mRoutePlayer.setMediaItem(mMediaItem);
                return;
            }
        }

        try {
            if (mMediaPlayer == null) {
                mMediaPlayer = new MediaPlayer(getContext());
            }
            mSurfaceView.setMediaPlayer(mMediaPlayer);
            mTextureView.setMediaPlayer(mMediaPlayer);
            if (!mCurrentView.assignSurfaceToMediaPlayer(mMediaPlayer)) {
                Log.w(TAG, "failed to assign surface");
            }
            mMediaPlayer.setAudioAttributes(mAudioAttributes);

            ensureSessionWithPlayer(mMediaPlayer);
            mMediaPlayer.setMediaItem(mMediaItem);

            final Context context = getContext();
            SubtitleController.Listener listener = new SubtitleController.Listener() {
                @Override
                public void onSubtitleTrackSelected(SubtitleTrack track) {
                    if (track == null) {
                        mMediaPlayer.deselectTrack(mSelectedSubtitleTrackInfo);
                        mSelectedSubtitleTrackInfo = null;
                        mSubtitleAnchorView.setVisibility(View.GONE);

                        mMediaSession.broadcastCustomCommand(new SessionCommand(
                                MediaControlView.EVENT_UPDATE_SUBTITLE_DESELECTED, null), null);
                        return;
                    }
                    TrackInfo info = null;
                    int indexInSubtitleTrackList = 0;
                    for (Entry<TrackInfo, SubtitleTrack> pair : mSubtitleTracks.entrySet()) {
                        if (pair.getValue() == track) {
                            info = pair.getKey();
                            break;
                        }
                        indexInSubtitleTrackList++;
                    }
                    if (info != null) {
                        mMediaPlayer.selectTrack(info);
                        mSelectedSubtitleTrackInfo = info;
                        mSubtitleAnchorView.setVisibility(View.VISIBLE);

                        Bundle data = new Bundle();
                        data.putInt(MediaControlView.KEY_SELECTED_SUBTITLE_INDEX,
                                indexInSubtitleTrackList);
                        mMediaSession.broadcastCustomCommand(new SessionCommand(
                                MediaControlView.EVENT_UPDATE_SUBTITLE_SELECTED, null), data);
                    }
                }
            };
            mSubtitleController = new SubtitleController(context, null, listener);
            mSubtitleController.registerRenderer(new ClosedCaptionRenderer(context));
            mSubtitleController.registerRenderer(new Cea708CaptionRenderer(context));
            mSubtitleController.setAnchor(mSubtitleAnchorView);

            // we don't set the target state here either, but preserve the
            // target state that was there before.
            mCurrentState = STATE_PREPARING;
            mMediaSession.getPlayer().prepare();
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "Unable to open content: " + mMediaItem, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
        }
    }

    /*
     * Reset the media player in any state
     */
    void resetPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mTextureView.setMediaPlayer(null);
            mSurfaceView.setMediaPlayer(null);
            mCurrentState = STATE_IDLE;
            mTargetState = STATE_IDLE;
            mSelectedSubtitleTrackInfo = null;
            mSelectedAudioTrackInfo = null;
        }
    }

    boolean isRemotePlayback() {
        return mRoutePlayer != null
                && mMediaSession != null
                && (mMediaSession.getPlayer() instanceof RemoteSessionPlayer);
    }

    void selectSubtitleTrack(TrackInfo trackInfo) {
        if (!isMediaPrepared()) {
            return;
        }
        SubtitleTrack track = mSubtitleTracks.get(trackInfo);
        if (track != null) {
            mSubtitleController.selectTrack(track);
        }
    }

    void deselectSubtitleTrack() {
        if (!isMediaPrepared() || mSelectedSubtitleTrackInfo == null) {
            return;
        }
        mSubtitleController.selectTrack(null);
    }

    // TODO: move this method inside callback to make sure it runs inside the callback thread.
    Bundle extractTrackInfoData() {
        List<MediaPlayer.TrackInfo> trackInfos = mMediaPlayer.getTrackInfo();
        mVideoTrackCount = 0;
        mAudioTrackInfos = new ArrayList<>();
        mSubtitleTracks = new LinkedHashMap<>();
        ArrayList<String> subtitleTracksLanguageList = new ArrayList<>();
        TrackInfo selectedSubtitleTrackInfo = mSelectedSubtitleTrackInfo;
        mSubtitleController.reset();
        for (int i = 0; i < trackInfos.size(); ++i) {
            final TrackInfo trackInfo = trackInfos.get(i);
            int trackType = trackInfo.getTrackType();
            if (trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_VIDEO) {
                mVideoTrackCount++;
            } else if (trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
                mAudioTrackInfos.add(trackInfo);
            } else if (trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE) {
                SubtitleTrack track = mSubtitleController.addTrack(trackInfo.getFormat());
                if (track != null) {
                    mSubtitleTracks.put(trackInfo, track);
                    String language = trackInfo.getLanguage().getISO3Language();
                    subtitleTracksLanguageList.add(language);
                }
            }
        }
        // Select first tracks as default
        if (mAudioTrackInfos.size() > 0) {
            mSelectedAudioTrackInfo = mAudioTrackInfos.get(0);
        }
        // Re-select originally selected subtitle track since SubtitleController has been reset.
        if (selectedSubtitleTrackInfo != null) {
            selectSubtitleTrack(selectedSubtitleTrackInfo);
        }

        Bundle data = new Bundle();
        data.putInt(MediaControlView.KEY_VIDEO_TRACK_COUNT, mVideoTrackCount);
        data.putInt(MediaControlView.KEY_AUDIO_TRACK_COUNT, mAudioTrackInfos.size());
        data.putStringArrayList(MediaControlView.KEY_SUBTITLE_TRACK_LANGUAGE_LIST,
                subtitleTracksLanguageList);
        return data;
    }

    boolean isCurrentItemMusic() {
        return mVideoTrackCount == 0 && mAudioTrackInfos != null && mAudioTrackInfos.size() > 0;
    }

    void updateMusicView() {
        mMusicView.setBackgroundColor(mDominantColor);
        mMusicView.setAlbumDrawable(mMusicAlbumDrawable);
        mMusicView.setTitleText(mTitle);
        mMusicView.setArtistText(mMusicArtistText);
    }

    @SuppressLint("SyntheticAccessor")
    MediaPlayer.PlayerCallback mMediaPlayerCallback =
            new MediaPlayer.PlayerCallback() {
                @Override
                public void onVideoSizeChanged(
                        @NonNull MediaPlayer mp, @NonNull MediaItem dsd, @NonNull VideoSize size) {
                    if (DEBUG) {
                        Log.d(TAG, "onVideoSizeChanged(): size: " + size.getWidth() + "/"
                                + size.getHeight());
                    }
                    if (mp != mMediaPlayer) {
                        if (DEBUG) {
                            Log.w(TAG, "onVideoSizeChanged() is ignored. mp is already gone.");
                        }
                        return;
                    }
                    if (dsd != mMediaItem) {
                        if (DEBUG) {
                            Log.w(TAG, "onVideoSizeChanged() is ignored. Media item is changed.");
                        }
                        return;
                    }
                    mTextureView.forceLayout();
                    mSurfaceView.forceLayout();
                    requestLayout();
                }

                @Override
                public void onInfo(
                        @NonNull MediaPlayer mp, @NonNull MediaItem dsd, int what, int extra) {
                    if (DEBUG) {
                        Log.d(TAG, "onInfo()");
                    }
                    if (mp != mMediaPlayer) {
                        if (DEBUG) {
                            Log.w(TAG, "onInfo() is ignored. mp is already gone.");
                        }
                        return;
                    }
                    if (dsd != mMediaItem) {
                        if (DEBUG) {
                            Log.w(TAG, "onInfo() is ignored. Media item is changed.");
                        }
                        return;
                    }
                    if (what == MediaPlayer.MEDIA_INFO_METADATA_UPDATE) {
                        Bundle data = extractTrackInfoData();
                        if (data != null) {
                            mMediaSession.broadcastCustomCommand(
                                    new SessionCommand(MediaControlView.EVENT_UPDATE_TRACK_STATUS,
                                            null), data);
                        }
                    }
                }

                @Override
                public void onError(
                        @NonNull MediaPlayer mp, @NonNull MediaItem dsd, int frameworkErr,
                        int implErr) {
                    if (DEBUG) {
                        Log.d(TAG, "Error: " + frameworkErr + "," + implErr);
                    }
                    if (mp != mMediaPlayer) {
                        if (DEBUG) {
                            Log.w(TAG, "onError() is ignored. mp is already gone.");
                        }
                        return;
                    }
                    if (dsd != mMediaItem) {
                        if (DEBUG) {
                            Log.w(TAG, "onError() is ignored. Media item is changed.");
                        }
                        return;
                    }
                    if (mCurrentState != STATE_ERROR) {
                        mCurrentState = STATE_ERROR;
                        mTargetState = STATE_ERROR;
                    }
                }

                @Override
                public void onSubtitleData(
                        @NonNull MediaPlayer mp, @NonNull MediaItem dsd,
                        @NonNull SubtitleData data) {
                    final TrackInfo trackInfo = data.getTrackInfo();
                    if (DEBUG) {
                        Log.d(TAG, "onSubtitleData():"
                                + " getTrackInfo: " + trackInfo
                                + ", getCurrentPosition: " + mp.getCurrentPosition()
                                + ", getStartTimeUs(): " + data.getStartTimeUs()
                                + ", diff: "
                                + (data.getStartTimeUs() / 1000 - mp.getCurrentPosition())
                                + "ms, getDurationUs(): " + data.getDurationUs());
                    }
                    if (mp != mMediaPlayer) {
                        if (DEBUG) {
                            Log.w(TAG, "onSubtitleData() is ignored. mp is already gone.");
                        }
                        return;
                    }
                    if (dsd != mMediaItem) {
                        if (DEBUG) {
                            Log.w(TAG, "onSubtitleData() is ignored. Media item is changed.");
                        }
                        return;
                    }
                    if (!trackInfo.equals(mSelectedSubtitleTrackInfo)) {
                        return;
                    }
                    SubtitleTrack track = mSubtitleTracks.get(trackInfo);
                    if (track != null) {
                        track.onData(data);
                    }
                }

                @Override
                public void onPlayerStateChanged(@NonNull SessionPlayer player,
                        @SessionPlayer.PlayerState int state) {
                    switch (state) {
                        case SessionPlayer.PLAYER_STATE_IDLE:
                            mCurrentState = STATE_IDLE;
                            break;
                        case SessionPlayer.PLAYER_STATE_PLAYING:
                            mCurrentState = STATE_PLAYING;
                            break;
                        case SessionPlayer.PLAYER_STATE_PAUSED:
                            if (mCurrentState == STATE_PREPARING) {
                                onPrepared(player);
                            }
                            mCurrentState = STATE_PAUSED;
                            break;
                        case SessionPlayer.PLAYER_STATE_ERROR:
                            mCurrentState = STATE_ERROR;
                            break;
                    }
                }

                @Override
                public void onPlaybackCompleted(@NonNull SessionPlayer player) {
                    if (player != mMediaPlayer) {
                        Log.d(TAG, "onPlaybackCompleted() is ignored. player is already gone.");
                    }
                    mCurrentState = STATE_PLAYBACK_COMPLETED;
                    mTargetState = STATE_PLAYBACK_COMPLETED;
                }

                private void onPrepared(SessionPlayer player) {
                    if (DEBUG) {
                        Log.d(TAG, "OnPreparedListener(): "
                                + ", mCurrentState=" + mCurrentState
                                + ", mTargetState=" + mTargetState);
                    }
                    mCurrentState = STATE_PREPARED;

                    if (mMediaSession != null) {
                        Bundle data = extractTrackInfoData();
                        if (data != null) {
                            mMediaSession.broadcastCustomCommand(
                                    new SessionCommand(MediaControlView.EVENT_UPDATE_TRACK_STATUS,
                                            null), data);
                        }

                        // Run extractMetadata() in another thread to prevent StrictMode violation.
                        // extractMetadata() contains file IO indirectly,
                        // via MediaMetadataRetriever.
                        boolean isMusic = isCurrentItemMusic();
                        MetadataExtractTask task = new MetadataExtractTask(mMediaItem, isMusic,
                                getContext());
                        task.execute();
                    }

                    if (mMediaControlView != null) {
                        Uri uri = (mMediaItem instanceof UriMediaItem)
                                ? ((UriMediaItem) mMediaItem).getUri() : null;
                        if (uri != null && UriUtil.isFromNetwork(uri)) {
                            mMediaControlView.setRouteSelector(mRouteSelector);
                        } else {
                            mMediaControlView.setRouteSelector(null);
                        }
                    }

                    if (player instanceof MediaPlayer) {
                        if (needToStart()) {
                            mMediaSession.getPlayer().play();
                        }
                    }
                }
            };

    class MediaSessionCallback extends MediaSession.SessionCallback {
        @Override
        public SessionCommandGroup onConnect(
                @NonNull MediaSession session,
                @NonNull MediaSession.ControllerInfo controller) {
            if (session != mMediaSession) {
                if (DEBUG) {
                    Log.w(TAG, "onConnect() is ignored. session is already gone.");
                }
            }
            SessionCommandGroup.Builder commandsBuilder = new SessionCommandGroup.Builder()
                    .addCommand(new SessionCommand(SessionCommand.COMMAND_CODE_PLAYER_PAUSE))
                    .addCommand(new SessionCommand(SessionCommand.COMMAND_CODE_PLAYER_PLAY))
                    .addCommand(new SessionCommand(SessionCommand.COMMAND_CODE_PLAYER_PREPARE))
                    .addCommand(new SessionCommand(SessionCommand.COMMAND_CODE_PLAYER_SET_SPEED))
                    .addCommand(new SessionCommand(
                            SessionCommand.COMMAND_CODE_SESSION_FAST_FORWARD))
                    .addCommand(new SessionCommand(SessionCommand.COMMAND_CODE_SESSION_REWIND))
                    .addCommand(new SessionCommand(SessionCommand.COMMAND_CODE_PLAYER_SEEK_TO))
                    .addCommand(new SessionCommand(SessionCommand.COMMAND_CODE_VOLUME_SET_VOLUME))
                    .addCommand(new SessionCommand(
                            SessionCommand.COMMAND_CODE_VOLUME_ADJUST_VOLUME))
                    .addCommand(new SessionCommand(
                            SessionCommand.COMMAND_CODE_SESSION_PLAY_FROM_URI))
                    .addCommand(new SessionCommand(
                            SessionCommand.COMMAND_CODE_SESSION_PREPARE_FROM_URI))
                    .addCommand(new SessionCommand(SessionCommand.COMMAND_CODE_PLAYER_GET_PLAYLIST))
                    .addCommand(new SessionCommand(
                            SessionCommand.COMMAND_CODE_PLAYER_GET_PLAYLIST_METADATA))
                    .addCommand(new SessionCommand(
                            MediaControlView.COMMAND_SELECT_AUDIO_TRACK, null))
                    .addCommand(new SessionCommand(
                            MediaControlView.COMMAND_SHOW_SUBTITLE, null))
                    .addCommand(new SessionCommand(
                            MediaControlView.COMMAND_HIDE_SUBTITLE, null));
            return commandsBuilder.build();
        }

        @Override
        public void onPostConnect(@NonNull MediaSession session,
                @NonNull MediaSession.ControllerInfo controller) {
            if (session != mMediaSession) {
                if (DEBUG) {
                    Log.w(TAG, "onPostConnect() is ignored. session is already gone.");
                }
            }
            if (isMediaPrepared()) {
                Bundle data = extractTrackInfoData();
                if (data != null) {
                    mMediaSession.broadcastCustomCommand(new SessionCommand(
                            MediaControlView.EVENT_UPDATE_TRACK_STATUS, null), data);
                }
            }
        }

        @Override
        @NonNull
        public SessionResult onCustomCommand(@NonNull MediaSession session,
                @NonNull MediaSession.ControllerInfo controller,
                @NonNull SessionCommand command, @Nullable Bundle args) {
            if (session != mMediaSession) {
                if (DEBUG) {
                    Log.w(TAG, "onCustomCommand() is ignored. session is already gone.");
                }
            }
            if (isRemotePlayback()) {
                // TODO: call mRoutePlayer.onCommand()
                return new SessionResult(RESULT_SUCCESS, null);
            }
            switch (command.getCustomAction()) {
                case MediaControlView.COMMAND_SHOW_SUBTITLE:
                    int indexInSubtitleTrackList = args != null ? args.getInt(
                            MediaControlView.KEY_SELECTED_SUBTITLE_INDEX,
                            INVALID_TRACK_INDEX) : INVALID_TRACK_INDEX;
                    if (indexInSubtitleTrackList != INVALID_TRACK_INDEX) {
                        final List<TrackInfo> subtitleTracks =
                                new ArrayList<>(mSubtitleTracks.keySet());
                        TrackInfo subtitleTrack = subtitleTracks.get(indexInSubtitleTrackList);
                        if (!subtitleTrack.equals(mSelectedSubtitleTrackInfo)) {
                            selectSubtitleTrack(subtitleTrack);
                        }
                    }
                    break;
                case MediaControlView.COMMAND_HIDE_SUBTITLE:
                    deselectSubtitleTrack();
                    break;
                case MediaControlView.COMMAND_SELECT_AUDIO_TRACK:
                    int audioIndex = (args != null)
                            ? args.getInt(MediaControlView.KEY_SELECTED_AUDIO_INDEX,
                            INVALID_TRACK_INDEX) : INVALID_TRACK_INDEX;
                    if (audioIndex != INVALID_TRACK_INDEX) {
                        TrackInfo audioTrackInfo = mAudioTrackInfos.get(audioIndex);
                        if (!audioTrackInfo.equals(mSelectedAudioTrackInfo)) {
                            mSelectedAudioTrackInfo = audioTrackInfo;
                            mMediaPlayer.selectTrack(mSelectedAudioTrackInfo);
                        }
                    }
                    break;
            }
            return new SessionResult(RESULT_SUCCESS, null);
        }

        @Override
        public int onCommandRequest(@NonNull MediaSession session,
                @NonNull MediaSession.ControllerInfo controller,
                @NonNull SessionCommand command) {
            if (session != mMediaSession) {
                if (DEBUG) {
                    Log.w(TAG, "onCommandRequest() is ignored. session is already gone.");
                }
            }
            switch (command.getCommandCode()) {
                case SessionCommand.COMMAND_CODE_PLAYER_PLAY:
                    mTargetState = STATE_PLAYING;
                    if (!mCurrentView.hasAvailableSurface() && !isCurrentItemMusic()) {
                        Log.d(TAG, "surface is not available");
                        return RESULT_ERROR_INVALID_STATE;
                    }
                    break;
                case SessionCommand.COMMAND_CODE_PLAYER_PAUSE:
                    mTargetState = STATE_PAUSED;
                    break;
            }
            return RESULT_SUCCESS;
        }
    }

    private class MetadataExtractTask extends AsyncTask<Void, Void, MediaMetadata> {
        private MediaItem mItem;
        private boolean mIsMusic;
        private Context mContext;

        MetadataExtractTask(MediaItem mediaItem, boolean isMusic, Context context) {
            mItem = mediaItem;
            mIsMusic = isMusic;
            mContext = context;
        }

        @Override
        protected MediaMetadata doInBackground(Void... params) {
            return extractMetadata(mItem, mIsMusic);
        }

        @Override
        @SuppressLint("SyntheticAccessor")
        protected void onPostExecute(MediaMetadata metadata) {
            if (metadata != null) {
                mItem.setMetadata(metadata);
            }

            if (mIsMusic && mMediaItem == mItem) {
                // Update Music View to reflect the new metadata
                mMusicView.setVisibility(View.VISIBLE);
                updateMusicView();
            } else {
                mMusicView.setVisibility(View.GONE);
            }
        }

        MediaMetadata extractMetadata(MediaItem mediaItem, boolean isMusic) {
            MediaMetadataRetriever retriever = null;
            String path = "";
            try {
                if (mediaItem == null) {
                    return null;
                } else if (mediaItem instanceof UriMediaItem) {
                    Uri uri = ((UriMediaItem) mediaItem).getUri();

                    // Save file name as title since the file may not have a title Metadata.
                    if ("file".equals(uri.getScheme())) {
                        path = uri.getLastPathSegment();
                    } else {
                        path = uri.toString();
                    }
                    retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(mContext, uri);
                } else if (mediaItem instanceof FileMediaItem) {
                    retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(
                            ((FileMediaItem) mediaItem).getParcelFileDescriptor()
                                    .getFileDescriptor(),
                            ((FileMediaItem) mediaItem).getFileDescriptorOffset(),
                            ((FileMediaItem) mediaItem).getFileDescriptorLength());
                }
            } catch (IllegalArgumentException e) {
                Log.v(TAG, "Cannot retrieve metadata for this media file.");
                retriever = null;
            }

            MediaMetadata metadata = mediaItem.getMetadata();

            // Do not extract metadata of a media item which is not the current item.
            if (mediaItem != mMediaItem) {
                if (retriever != null) {
                    retriever.release();
                }
                return null;
            }
            if (!isMusic) {
                mTitle = extractString(metadata,
                        MediaMetadata.METADATA_KEY_TITLE, retriever,
                        MediaMetadataRetriever.METADATA_KEY_TITLE, path);
            } else {
                Resources resources = getResources();
                mTitle = extractString(metadata,
                        MediaMetadata.METADATA_KEY_TITLE, retriever,
                        MediaMetadataRetriever.METADATA_KEY_TITLE,
                        resources.getString(R.string.mcv2_music_title_unknown_text));
                mMusicArtistText = extractString(metadata,
                        MediaMetadata.METADATA_KEY_ARTIST,
                        retriever,
                        MediaMetadataRetriever.METADATA_KEY_ARTIST,
                        resources.getString(R.string.mcv2_music_artist_unknown_text));
                mMusicAlbumDrawable = extractAlbumArt(metadata, retriever,
                        resources.getDrawable(R.drawable.ic_default_album_image));
            }

            if (retriever != null) {
                retriever.release();
            }

            // Set duration and title values as MediaMetadata for MediaControlView
            MediaMetadata.Builder builder = new MediaMetadata.Builder();

            if (isMusic) {
                builder.putString(MediaMetadata.METADATA_KEY_ARTIST, mMusicArtistText);
            }
            builder.putString(MediaMetadata.METADATA_KEY_TITLE, mTitle);
            builder.putLong(
                    MediaMetadata.METADATA_KEY_DURATION, mMediaSession.getPlayer().getDuration());
            builder.putString(
                    MediaMetadata.METADATA_KEY_MEDIA_ID, mediaItem.getMediaId());
            builder.putLong(MediaMetadata.METADATA_KEY_PLAYABLE, 1);
            return builder.build();
        }

        private String extractString(MediaMetadata metadata, String stringKey,
                MediaMetadataRetriever retriever, int intKey, String defaultValue) {
            String value = null;

            if (metadata != null) {
                value = metadata.getString(stringKey);
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
            if (retriever != null) {
                value = retriever.extractMetadata(intKey);
            }
            return value == null ? defaultValue : value;
        }

        private Drawable extractAlbumArt(MediaMetadata metadata, MediaMetadataRetriever retriever,
                Drawable defaultDrawable) {
            Bitmap bitmap = null;

            if (metadata != null && metadata.containsKey(MediaMetadata.METADATA_KEY_ALBUM_ART)) {
                bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            } else if (retriever != null) {
                byte[] album = retriever.getEmbeddedPicture();
                if (album != null) {
                    bitmap = BitmapFactory.decodeByteArray(album, 0, album.length);
                }
            }
            if (bitmap != null) {
                Palette.Builder builder = Palette.from(bitmap);
                builder.generate(new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        mDominantColor = palette.getDominantColor(0);
                        mMusicView.setBackgroundColor(mDominantColor);
                    }
                });
                return new BitmapDrawable(getResources(), bitmap);
            }
            return defaultDrawable;
        }
    }

    /**
     * Interface definition of a callback to be invoked when the view type has been changed.
     */
    public interface OnViewTypeChangedListener {
        /**
         * Called when the view type has been changed.
         * @see #setViewType(int)
         * @param view the View whose view type is changed
         * @param viewType
         * <ul>
         * <li>{@link #VIEW_TYPE_SURFACEVIEW}
         * <li>{@link #VIEW_TYPE_TEXTUREVIEW}
         * </ul>
         */
        void onViewTypeChanged(@NonNull View view, @ViewType int viewType);
    }
}
