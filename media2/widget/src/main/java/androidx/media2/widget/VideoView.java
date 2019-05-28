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
import androidx.media2.common.SessionPlayer.TrackInfo;
import androidx.media2.common.SubtitleData;
import androidx.media2.common.UriMediaItem;
import androidx.media2.common.VideoSize;
import androidx.media2.player.MediaPlayer;
import androidx.media2.player.subtitle.Cea708CaptionRenderer;
import androidx.media2.player.subtitle.ClosedCaptionRenderer;
import androidx.media2.player.subtitle.SubtitleController;
import androidx.media2.player.subtitle.SubtitleTrack;
import androidx.media2.session.MediaController;
import androidx.media2.session.MediaSession;
import androidx.media2.session.SessionCommand;
import androidx.media2.session.SessionCommandGroup;
import androidx.media2.session.SessionToken;
import androidx.palette.graphics.Palette;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
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
    MediaController mMediaController;
    String mTitle;
    Executor mCallbackExecutor;

    MusicView mMusicView;
    Drawable mMusicAlbumDrawable;
    String mMusicArtistText;

    int mDominantColor;

    int mTargetState = STATE_IDLE;
    int mCurrentState = STATE_IDLE;

    int mVideoTrackCount;
    int mAudioTrackCount;
    Map<TrackInfo, SubtitleTrack> mSubtitleTracks;
    SubtitleController mSubtitleController;

    // selected subtitle track info as MediaPlayer returns
    TrackInfo mSelectedSubtitleTrackInfo;

    SubtitleAnchorView mSubtitleAnchorView;

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
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mMediaController.close();
        mMediaController = null;
        mMediaSession.close();
        mMediaSession = null;
        try {
            mMediaPlayer.close();
        } catch (Exception e) {
            Log.e(TAG, "Encountered an exception while performing MediaPlayer.close()", e);
        }
        mMediaPlayer = null;
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

        if (mMediaController == null) {
            throw new IllegalStateException("It can't be called when mMediaController is null");
        }
        mMediaControlView.setMediaController(mMediaController);

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
            oldPlayer.unregisterPlayerCallback(mPlayerCallback);
            mMediaSession.updatePlayer(player);
        } else {
            final Context context = getContext();
            mMediaSession = new MediaSession.Builder(context, player)
                    .setId("VideoView_" + toString())
                    .setSessionCallback(mCallbackExecutor, new MediaSessionCallback())
                    .build();
            mMediaController = new MediaController.Builder(context)
                    .setSessionToken(mMediaSession.getToken())
                    .build();
        }
        player.registerPlayerCallback(mCallbackExecutor, mPlayerCallback);
    }

    boolean isMediaPrepared() {
        return mMediaSession != null
                && mMediaSession.getPlayer().getPlayerState() != SessionPlayer.PLAYER_STATE_ERROR
                && mMediaSession.getPlayer().getPlayerState() != SessionPlayer.PLAYER_STATE_IDLE;
    }

    boolean needToStart() {
        return mMediaSession != null
                && mMediaPlayer != null && isWaitingPlayback();
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
                    // Track deselected
                    if (track == null) {
                        mSelectedSubtitleTrackInfo = null;
                        mSubtitleAnchorView.setVisibility(View.GONE);
                        return;
                    }

                    // Track selected
                    TrackInfo info = null;
                    for (Entry<TrackInfo, SubtitleTrack> pair : mSubtitleTracks.entrySet()) {
                        if (pair.getValue() == track) {
                            info = pair.getKey();
                            break;
                        }
                    }
                    if (info != null) {
                        mSelectedSubtitleTrackInfo = info;
                        mSubtitleAnchorView.setVisibility(View.VISIBLE);
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
        }
    }

    boolean hasActualVideo() {
        if (mVideoTrackCount > 0) {
            return true;
        }
        VideoSize videoSize = mMediaPlayer.getVideoSizeInternal();
        if (videoSize.getHeight() > 0 && videoSize.getWidth() > 0) {
            Log.w(TAG, "video track count is zero, but it renders video. size: "
                    + videoSize.getWidth() + "/" + videoSize.getHeight());
            return true;
        }
        return false;
    }

    boolean isCurrentItemMusic() {
        return !hasActualVideo() && mAudioTrackCount > 0;
    }

    void updateMusicView() {
        mMusicView.setBackgroundColor(mDominantColor);
        mMusicView.setAlbumDrawable(mMusicAlbumDrawable);
        mMusicView.setTitleText(mTitle);
        mMusicView.setArtistText(mMusicArtistText);
    }

    void updateTracks(SessionPlayer player, List<TrackInfo> trackInfos) {
        mSubtitleTracks = new LinkedHashMap<>();
        for (int i = 0; i < trackInfos.size(); i++) {
            TrackInfo trackInfo = trackInfos.get(i);
            int trackType = trackInfos.get(i).getTrackType();
            if (trackType == TrackInfo.MEDIA_TRACK_TYPE_VIDEO) {
                mVideoTrackCount++;
            } else if (trackType == TrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
                mAudioTrackCount++;
            } else if (trackType == TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE) {
                SubtitleTrack track = mSubtitleController.addTrack(trackInfo.getFormat());
                if (track != null) {
                    mSubtitleTracks.put(trackInfo, track);
                }
            }
        }
        mSelectedSubtitleTrackInfo = player.getSelectedTrackInternal(
                TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE);
    }

    private SessionPlayer.PlayerCallback mPlayerCallback = new SessionPlayer.PlayerCallback() {
        @Override
        public void onVideoSizeChangedInternal(@NonNull SessionPlayer player,
                @NonNull MediaItem item, @NonNull VideoSize size) {
            if (DEBUG) {
                Log.d(TAG, "onVideoSizeChanged(): size: " + size);
            }
            if (player != mMediaPlayer) {
                if (DEBUG) {
                    Log.w(TAG, "onVideoSizeChanged() is ignored. player is already gone.");
                }
                return;
            }
            if (item != mMediaItem) {
                if (DEBUG) {
                    Log.w(TAG, "onVideoSizeChanged() is ignored. Media item is changed.");
                }
                return;
            }
            if (mVideoTrackCount == 0 && size.getHeight() > 0 && size.getWidth() > 0) {
                if (isMediaPrepared()) {
                    List<TrackInfo> trackInfos = player.getTrackInfoInternal();
                    if (trackInfos != null) {
                        updateTracks(player, trackInfos);
                    }
                }
            }
            mTextureView.forceLayout();
            mSurfaceView.forceLayout();
            requestLayout();
        }

        @Override
        public void onSubtitleData(@NonNull SessionPlayer player, @NonNull MediaItem item,
                @NonNull TrackInfo track, @NonNull SubtitleData data) {
            if (DEBUG) {
                Log.d(TAG, "onSubtitleData():"
                        + " TrackInfo: " + track
                        + ", getCurrentPosition: " + player.getCurrentPosition()
                        + ", getStartTimeUs(): " + data.getStartTimeUs()
                        + ", diff: "
                        + (data.getStartTimeUs() / 1000 - player.getCurrentPosition())
                        + "ms, getDurationUs(): " + data.getDurationUs());
            }
            if (player != mMediaPlayer) {
                if (DEBUG) {
                    Log.w(TAG, "onSubtitleData() is ignored. player is already gone.");
                }
                return;
            }
            if (item != mMediaItem) {
                if (DEBUG) {
                    Log.w(TAG, "onSubtitleData() is ignored. Media item is changed.");
                }
                return;
            }
            if (!track.equals(mSelectedSubtitleTrackInfo)) {
                return;
            }
            SubtitleTrack subtitleTrack = mSubtitleTracks.get(track);
            if (subtitleTrack != null) {
                subtitleTrack.onData(data);
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
                    mTargetState = STATE_ERROR;
                    // TODO: Show error state (b/123498635)
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

        @Override
        public void onTrackInfoChanged(@NonNull SessionPlayer player,
                @NonNull List<TrackInfo> trackInfos) {
            updateTracks(player, trackInfos);

            // TODO: Remove extracting metadata (b/133283493)
            // Run extractMetadata() in another thread to prevent StrictMode violation.
            // extractMetadata() contains file IO indirectly,
            // via MediaMetadataRetriever.
            boolean isMusic = isCurrentItemMusic();
            MetadataExtractTask task = new MetadataExtractTask(mMediaItem, isMusic,
                    getContext());
            task.execute();
        }

        @Override
        public void onTrackSelected(@NonNull SessionPlayer player, @NonNull TrackInfo trackInfo) {
            SubtitleTrack subtitleTrack = mSubtitleTracks.get(trackInfo);
            if (subtitleTrack != null) {
                mSubtitleController.selectTrack(subtitleTrack);
            }
        }

        @Override
        public void onTrackDeselected(@NonNull SessionPlayer player, @NonNull TrackInfo trackInfo) {
            SubtitleTrack subtitleTrack = mSubtitleTracks.get(trackInfo);
            if (subtitleTrack != null) {
                mSubtitleController.selectTrack(null);
            }
        }

        private void onPrepared(SessionPlayer player) {
            if (DEBUG) {
                Log.d(TAG, "onPrepared(): "
                        + ", mCurrentState=" + mCurrentState
                        + ", mTargetState=" + mTargetState);
            }
            mCurrentState = STATE_PREPARED;

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
                            SessionCommand.COMMAND_CODE_PLAYER_SELECT_TRACK))
                    .addCommand(new SessionCommand(
                            SessionCommand.COMMAND_CODE_PLAYER_DESELECT_TRACK));
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
            if (mMediaPlayer != null) {
                builder.putLong(
                        MediaMetadata.METADATA_KEY_DURATION, mMediaPlayer.getDuration());
            }
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
