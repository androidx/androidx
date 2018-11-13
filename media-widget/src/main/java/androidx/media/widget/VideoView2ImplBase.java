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

import static androidx.media2.MediaSession2.SessionResult.RESULT_CODE_INVALID_STATE;
import static androidx.media2.MediaSession2.SessionResult.RESULT_CODE_SUCCESS;

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
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
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
import androidx.core.content.ContextCompat;
import androidx.media.AudioAttributesCompat;
import androidx.media2.FileMediaItem2;
import androidx.media2.MediaItem2;
import androidx.media2.MediaMetadata2;
import androidx.media2.MediaPlayer;
import androidx.media2.MediaPlayer2;
import androidx.media2.MediaSession2;
import androidx.media2.RemoteSessionPlayer2;
import androidx.media2.SessionCommand2;
import androidx.media2.SessionCommandGroup2;
import androidx.media2.SessionPlayer2;
import androidx.media2.SessionToken2;
import androidx.media2.SubtitleData2;
import androidx.media2.UriMediaItem2;
import androidx.media2.subtitle.Cea708CaptionRenderer;
import androidx.media2.subtitle.ClosedCaptionRenderer;
import androidx.media2.subtitle.SubtitleController;
import androidx.media2.subtitle.SubtitleTrack;
import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;
import androidx.palette.graphics.Palette;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Base implementation of VideoView2.
 */
@RequiresApi(28)
class VideoView2ImplBase implements VideoView2Impl, VideoViewInterface.SurfaceListener {
    private static final String TAG = "VideoView2ImplBase";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

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

    private static final String SUBTITLE_TRACK_LANG_UNDEFINED = "und";

    private AudioAttributesCompat mAudioAttributes;

    private VideoView2.OnViewTypeChangedListener mViewTypeChangedListener;

    VideoViewInterface mCurrentView;
    VideoViewInterface mTargetView;
    private VideoTextureView mTextureView;
    private VideoSurfaceView mSurfaceView;

    VideoView2Player mMediaPlayer;
    MediaItem2 mMediaItem;
    MediaControlView2 mMediaControlView;
    MediaSession2 mMediaSession;
    private String mTitle;
    Executor mCallbackExecutor;

    private WindowManager mManager;
    View mCurrentMusicView;
    View mMusicFullLandscapeView;
    View mMusicFullPortraitView;
    View mMusicEmbeddedView;
    private Drawable mMusicAlbumDrawable;
    private String mMusicArtistText;
    boolean mIsMusicMediaType;
    private int mPrevWidth;
    int mDominantColor;
    private int mSizeType;

    int mTargetState = STATE_IDLE;
    int mCurrentState = STATE_IDLE;
    long mSeekWhenPrepared;  // recording the seek position while preparing

    int mVideoWidth;
    int mVideoHeight;

    private ArrayList<Integer> mVideoTrackIndices;
    ArrayList<Integer> mAudioTrackIndices;
    SparseArray<SubtitleTrack> mSubtitleTracks;
    private SubtitleController mSubtitleController;

    // selected audio/subtitle track index as MediaPlayer returns
    int mSelectedAudioTrackIndex;
    int mSelectedSubtitleTrackIndex;

    private SubtitleAnchorView mSubtitleAnchorView;

    VideoView2 mInstance;

    private MediaRouter mMediaRouter;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaRouteSelector mRouteSelector;
    MediaRouter.RouteInfo mRoute;
    RoutePlayer2 mRoutePlayer;

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
                mRoutePlayer = new RoutePlayer2(mInstance.getContext(), mRouteSelector, route);
                // TODO: Replace with MediaSession2#setPlaylist once b/110811730 is fixed.
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
                if (currentState == SessionPlayer2.PLAYER_STATE_PLAYING) {
                    mMediaSession.getPlayer().play();
                }
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

        mCallbackExecutor = ContextCompat.getMainExecutor(context);

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

        LayoutInflater inflater = (LayoutInflater) mInstance.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMusicFullLandscapeView = inflater.inflate(R.layout.full_landscape_music, null);
        mMusicFullPortraitView = inflater.inflate(R.layout.full_portrait_music, null);
        mMusicEmbeddedView = inflater.inflate(R.layout.embedded_music, null);

        boolean enableControlView = (attrs == null) || attrs.getAttributeBooleanValue(
                "http://schemas.android.com/apk/res-auto",
                "enableControlView", true);
        if (enableControlView) {
            mMediaControlView = new MediaControlView2(context);
        }

        // Choose surface view by default
        int viewType = (attrs == null) ? VideoView2.VIEW_TYPE_SURFACEVIEW
                : attrs.getAttributeIntValue(
                "http://schemas.android.com/apk/res-auto",
                "viewType", VideoView2.VIEW_TYPE_SURFACEVIEW);
        if (viewType == VideoView2.VIEW_TYPE_SURFACEVIEW) {
            if (DEBUG) {
                Log.d(TAG, "viewType attribute is surfaceView.");
            }
            mTextureView.setVisibility(View.GONE);
            mSurfaceView.setVisibility(View.VISIBLE);
            mCurrentView = mSurfaceView;
        } else if (viewType == VideoView2.VIEW_TYPE_TEXTUREVIEW) {
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
     * Sets MediaControlView2 instance. It will replace the previously assigned MediaControlView2
     * instance if any.
     *
     * @param mediaControlView a media control view2 instance.
     * @param intervalMs a time interval in milliseconds until VideoView2 hides MediaControlView2.
     */
    @Override
    public void setMediaControlView2(@NonNull MediaControlView2 mediaControlView, long intervalMs) {
        mMediaControlView = mediaControlView;
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
     * Returns {@link SessionToken2} so that developers create their own
     * {@link androidx.media2.MediaController2} instance. This method should be called when
     * VideoView2 is attached to window or after {@link #setMediaItem2} is called.
     *
     * @throws IllegalStateException if internal MediaSession is not created yet.
     */
    @Override
    @NonNull
    public SessionToken2 getMediaSessionToken2() {
        if (mMediaSession == null) {
            throw new IllegalStateException("MediaSession2 instance is not available.");
        }
        return mMediaSession.getToken();
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
     * Sets {@link MediaItem2} object to render using VideoView2.
     * @param mediaItem the MediaItem2 to play
     */
    @Override
    public void setMediaItem2(@NonNull MediaItem2 mediaItem) {
        mSeekWhenPrepared = 0;
        mMediaItem = mediaItem;
        openVideo();
    }

    /**
     * Selects which view will be used to render video between SurfaceView and TextureView.
     *
     * @param viewType the view type to render video
     * <ul>
     * <li>{@link VideoView2#VIEW_TYPE_SURFACEVIEW}
     * <li>{@link VideoView2#VIEW_TYPE_TEXTUREVIEW}
     * </ul>
     */
    @Override
    public void setViewType(@VideoView2.ViewType int viewType) {
        if (viewType == mTargetView.getViewType()) {
            Log.d(TAG, "setViewType with the same type (" + viewType + ") is ignored.");
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

        mTargetView = targetView;
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
     */
    @Override
    public void setOnViewTypeChangedListener(VideoView2.OnViewTypeChangedListener l) {
        mViewTypeChangedListener = l;
    }

    @Override
    public void onAttachedToWindowImpl() {
        // Note: MediaPlayer2 and MediaSession2 instances are created in onAttachedToWindow()
        // and closed in onDetachedFromWindow().
        if (mMediaPlayer == null) {
            mMediaPlayer = new VideoView2Player(mInstance.getContext());

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
        mMediaRouter = MediaRouter.getInstance(mInstance.getContext());
        // TODO: Revisit once ag/4207152 is merged.
        mMediaRouter.setMediaSessionCompat(mMediaSession.getSessionCompat());
        mMediaRouter.addCallback(mRouteSelector, mRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    @Override
    public void onDetachedFromWindowImpl() {
        try {
            mMediaPlayer.close();
        } catch (Exception e) {
        }
        mMediaSession.close();
        mMediaPlayer = null;
        mMediaSession = null;
    }

    @Override
    public void onVisibilityAggregatedImpl(boolean isVisible) {
        if (isMediaPrepared()) {
            if (!isVisible && mCurrentState == STATE_PLAYING) {
                mMediaSession.getPlayer().pause();
            } else if (isVisible && mTargetState == STATE_PLAYING) {
                mMediaSession.getPlayer().play();
            }
        }
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
            if (mPrevWidth != currWidth) {
                Point screenSize = new Point();
                mManager.getDefaultDisplay().getSize(screenSize);
                int screenWidth = screenSize.x;
                if (currWidth == screenWidth) {
                    int orientation = retrieveOrientation();
                    if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                        updateCurrentMusicView(mMusicFullLandscapeView);
                    } else {
                        updateCurrentMusicView(mMusicFullPortraitView);
                    }

                    if (mSizeType != SIZE_TYPE_FULL) {
                        mSizeType = SIZE_TYPE_FULL;
                    }
                } else {
                    if (mSizeType != SIZE_TYPE_EMBEDDED) {
                        mSizeType = SIZE_TYPE_EMBEDDED;
                        updateCurrentMusicView(mMusicEmbeddedView);
                    }
                }
                mPrevWidth = currWidth;
            }
        }
    }

    ///////////////////////////////////////////////////
    // Implements VideoViewInterface.SurfaceListener
    ///////////////////////////////////////////////////

    @Override
    public void onSurfaceCreated(View view, int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated(). mCurrentState=" + mCurrentState
                    + ", mTargetState=" + mTargetState + ", width/height: " + width + "/" + height
                    + ", " + view.toString());
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
        if (mCurrentState != STATE_PLAYING) {
            mMediaSession.getPlayer().seekTo(mMediaSession.getPlayer().getCurrentPosition());
        }
        if (view != mCurrentView) {
            mCurrentView = view;
            if (mViewTypeChangedListener != null) {
                mViewTypeChangedListener.onViewTypeChanged(mInstance, view.getViewType());
            }
        }

        if (needToStart()) {
            mMediaSession.getPlayer().play();
        }
    }

    ///////////////////////////////////////////////////
    // Protected or private methods
    ///////////////////////////////////////////////////
    private void attachMediaControlView() {
        // Get MediaController from MediaSession and set it inside MediaControlView
        mMediaControlView.setMediaSessionToken2(mMediaSession.getToken());

        LayoutParams params =
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mInstance.addView(mMediaControlView, params);
    }

    void ensureSessionWithPlayer(SessionPlayer2 player) {
        if (mMediaSession != null) {
            SessionPlayer2 oldPlayer = mMediaSession.getPlayer();
            if (oldPlayer == player) {
                return;
            }
            oldPlayer.unregisterPlayerCallback(mMediaPlayerCallback);
            mMediaSession.updatePlayer(player);
        } else {
            final Context context = mInstance.getContext();
            mMediaSession = new MediaSession2.Builder(context, player)
                    .setId("VideoView2_" + mInstance.toString())
                    .setSessionCallback(mCallbackExecutor, new MediaSessionCallback())
                    .build();
        }
        player.registerPlayerCallback(mCallbackExecutor, mMediaPlayerCallback);
    }

    private boolean isMediaPrepared() {
        return mMediaSession != null
                && mMediaSession.getPlayer().getPlayerState() != SessionPlayer2.PLAYER_STATE_ERROR
                && mMediaSession.getPlayer().getPlayerState() != SessionPlayer2.PLAYER_STATE_IDLE;
    }

    boolean needToStart() {
        return (mMediaPlayer != null || mRoutePlayer != null) && isWaitingPlayback();
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
                mMediaPlayer = new VideoView2Player(mInstance.getContext());
            }
            mSurfaceView.setMediaPlayer(mMediaPlayer);
            mTextureView.setMediaPlayer(mMediaPlayer);
            if (!mCurrentView.assignSurfaceToMediaPlayer(mMediaPlayer)) {
                Log.w(TAG, "failed to assign surface");
            }
            mMediaPlayer.setAudioAttributes(mAudioAttributes);

            ensureSessionWithPlayer(mMediaPlayer);
            mMediaPlayer.setMediaItem(mMediaItem);

            final Context context = mInstance.getContext();
            mSubtitleController = new SubtitleController(context);
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
            mSelectedSubtitleTrackIndex = INVALID_TRACK_INDEX;
            mSelectedAudioTrackIndex = INVALID_TRACK_INDEX;
        }
        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    boolean isRemotePlayback() {
        return mRoutePlayer != null
                && mMediaSession != null
                && (mMediaSession.getPlayer() instanceof RemoteSessionPlayer2);
    }

    void selectSubtitleTrack(int trackIndex) {
        if (!isMediaPrepared()) {
            return;
        }
        SubtitleTrack track = mSubtitleTracks.get(trackIndex);
        if (track != null) {
            mMediaPlayer.selectTrack(trackIndex);
            mSubtitleController.selectTrack(track);
            mSelectedSubtitleTrackIndex = trackIndex;
            mSubtitleAnchorView.setVisibility(View.VISIBLE);

            Bundle data = new Bundle();
            data.putInt(MediaControlView2.KEY_SELECTED_SUBTITLE_INDEX,
                    mSubtitleTracks.indexOfKey(trackIndex));
            mMediaSession.broadcastCustomCommand(
                    new SessionCommand2(MediaControlView2.EVENT_UPDATE_SUBTITLE_SELECTED, null),
                    data);
        }
    }

    void deselectSubtitleTrack() {
        if (!isMediaPrepared() || mSelectedSubtitleTrackIndex == INVALID_TRACK_INDEX) {
            return;
        }
        mMediaPlayer.deselectTrack(mSelectedSubtitleTrackIndex);
        mSelectedSubtitleTrackIndex = INVALID_TRACK_INDEX;
        mSubtitleAnchorView.setVisibility(View.GONE);

        mMediaSession.broadcastCustomCommand(
                new SessionCommand2(MediaControlView2.EVENT_UPDATE_SUBTITLE_DESELECTED, null),
                null);
    }

    // TODO: move this method inside callback to make sure it runs inside the callback thread.
    Bundle extractTrackInfoData() {
        List<MediaPlayer.TrackInfo> trackInfos = mMediaPlayer.getTrackInfo();
        mVideoTrackIndices = new ArrayList<>();
        mAudioTrackIndices = new ArrayList<>();
        mSubtitleTracks = new SparseArray<>();
        ArrayList<String> subtitleTracksLanguageList = new ArrayList<>();
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
                    mSubtitleTracks.put(i, track);
                    String language =
                            (trackInfos.get(i).getLanguage().equals(SUBTITLE_TRACK_LANG_UNDEFINED))
                                    ? "" : trackInfos.get(i).getLanguage();
                    subtitleTracksLanguageList.add(language);
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
        data.putInt(MediaControlView2.KEY_SUBTITLE_TRACK_COUNT, mSubtitleTracks.size());
        data.putStringArrayList(MediaControlView2.KEY_SUBTITLE_TRACK_LANGUAGE_LIST,
                subtitleTracksLanguageList);
        return data;
    }

    // TODO: move this method inside callback to make sure it runs inside the callback thread.
    MediaMetadata2 extractMetadata() {
        MediaMetadataRetriever retriever = null;
        String path = "";
        try {
            if (mMediaItem == null) {
                return null;
            } else if (mMediaItem instanceof UriMediaItem2) {
                Uri uri = ((UriMediaItem2) mMediaItem).getUri();

                // Save file name as title since the file may not have a title Metadata.
                String scheme = uri.getScheme();
                if (scheme != null) {
                    if (scheme.equals("file")) {
                        path = uri.getLastPathSegment();
                    } else if (scheme.equals("http") || scheme.equals("https")) {
                        path = uri.getPath();
                    }
                }
                retriever = new MediaMetadataRetriever();
                retriever.setDataSource(mInstance.getContext(), uri);
            } else if (mMediaItem instanceof FileMediaItem2) {
                retriever = new MediaMetadataRetriever();
                retriever.setDataSource(
                        ((FileMediaItem2) mMediaItem).getFileDescriptor(),
                        ((FileMediaItem2) mMediaItem).getFileDescriptorOffset(),
                        ((FileMediaItem2) mMediaItem).getFileDescriptorLength());
            }
        } catch (IllegalArgumentException e) {
            Log.v(TAG, "Cannot retrieve metadata for this media file.");
            retriever = null;
        }

        MediaMetadata2 metadata = mMediaItem.getMetadata();
        if (!mIsMusicMediaType) {
            mTitle = extractString(metadata, MediaMetadata2.METADATA_KEY_TITLE, retriever,
                    MediaMetadataRetriever.METADATA_KEY_TITLE, path);
        } else {
            Resources resources = mInstance.getResources();
            mManager = (WindowManager) mInstance.getContext().getApplicationContext()
                    .getSystemService(Context.WINDOW_SERVICE);

            mTitle = extractString(metadata, MediaMetadata2.METADATA_KEY_TITLE, retriever,
                    MediaMetadataRetriever.METADATA_KEY_TITLE,
                    resources.getString(R.string.mcv2_music_title_unknown_text));
            mMusicArtistText = extractString(metadata, MediaMetadata2.METADATA_KEY_ARTIST,
                    retriever, MediaMetadataRetriever.METADATA_KEY_ARTIST,
                    resources.getString(R.string.mcv2_music_artist_unknown_text));
            mMusicAlbumDrawable = extractAlbumArt(metadata, retriever,
                    resources.getDrawable(R.drawable.ic_default_album_image));
        }

        if (retriever != null) {
            retriever.release();
        }

        // Update Music View to reflect the new metadata
        if (mIsMusicMediaType) {
            mInstance.removeView(mSurfaceView);
            mInstance.removeView(mTextureView);
            updateCurrentMusicView(mMusicEmbeddedView);
        }

        // Set duration and title values as MediaMetadata2 for MediaControlView2
        MediaMetadata2.Builder builder = new MediaMetadata2.Builder();

        if (mIsMusicMediaType) {
            builder.putString(MediaMetadata2.METADATA_KEY_ARTIST, mMusicArtistText);
        }
        builder.putString(MediaMetadata2.METADATA_KEY_TITLE, mTitle);
        builder.putLong(
                MediaMetadata2.METADATA_KEY_DURATION, mMediaSession.getPlayer().getDuration());
        builder.putString(
                MediaMetadata2.METADATA_KEY_MEDIA_ID, mMediaItem.getMediaId());
        builder.putLong(MediaMetadata2.METADATA_KEY_BROWSABLE, MediaMetadata2.BROWSABLE_TYPE_NONE);
        builder.putLong(MediaMetadata2.METADATA_KEY_PLAYABLE, 1);
        return builder.build();
    }

    // TODO: move this method inside callback to make sure it runs inside the callback thread.
    private String extractString(MediaMetadata2 metadata, String stringKey,
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

    // TODO: move this method inside callback to make sure it runs inside the callback thread.
    private Drawable extractAlbumArt(MediaMetadata2 metadata, MediaMetadataRetriever retriever,
            Drawable defaultDrawable) {
        Bitmap bitmap = null;

        if (metadata != null && metadata.containsKey(MediaMetadata2.METADATA_KEY_ALBUM_ART)) {
            bitmap = metadata.getBitmap(MediaMetadata2.METADATA_KEY_ALBUM_ART);
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
                    if (mCurrentMusicView != null) {
                        mCurrentMusicView.setBackgroundColor(mDominantColor);
                    }
                }
            });
            return new BitmapDrawable(bitmap);
        }
        return defaultDrawable;
    }

    private int retrieveOrientation() {
        DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
        int width = dm.widthPixels;
        int height = dm.heightPixels;

        return (height > width)
                ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    }

    private void updateCurrentMusicView(View newMusicView) {
        newMusicView.setBackgroundColor(mDominantColor);

        ImageView albumView = newMusicView.findViewById(R.id.album);
        if (albumView != null) {
            albumView.setImageDrawable(mMusicAlbumDrawable);
        }

        TextView titleView = newMusicView.findViewById(R.id.title);
        if (titleView != null) {
            titleView.setText(mTitle);
        }

        TextView artistView = newMusicView.findViewById(R.id.artist);
        if (artistView != null) {
            artistView.setText(mMusicArtistText);
        }

        mInstance.removeView(mCurrentMusicView);
        mInstance.addView(newMusicView, 0);
        mCurrentMusicView = newMusicView;
    }

    MediaPlayer.PlayerCallback mMediaPlayerCallback =
            new MediaPlayer.PlayerCallback() {
                @Override
                public void onVideoSizeChanged(
                        MediaPlayer mp, MediaItem2 dsd, int width, int height) {
                    if (DEBUG) {
                        Log.d(TAG, "onVideoSizeChanged(): size: " + width + "/" + height);
                    }
                    if (mp != mMediaPlayer) {
                        if (DEBUG) {
                            Log.w(TAG, "onVideoSizeChanged() is ignored. mp is already gone.");
                        }
                        return;
                    }
                    mVideoWidth = width;
                    mVideoHeight = height;
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
                        MediaPlayer mp, MediaItem2 dsd, int what, int extra) {
                    if (DEBUG) {
                        Log.d(TAG, "onInfo()");
                    }
                    if (mp != mMediaPlayer) {
                        if (DEBUG) {
                            Log.w(TAG, "onInfo() is ignored. mp is already gone.");
                        }
                        return;
                    }
                    if (what == MediaPlayer2.MEDIA_INFO_METADATA_UPDATE) {
                        Bundle data = extractTrackInfoData();
                        if (data != null) {
                            mMediaSession.broadcastCustomCommand(
                                    new SessionCommand2(MediaControlView2.EVENT_UPDATE_TRACK_STATUS,
                                            null), data);
                        }
                    }
                }

                @Override
                public void onError(
                        MediaPlayer mp, MediaItem2 dsd, int frameworkErr, int implErr) {
                    if (DEBUG) {
                        Log.d(TAG, "Error: " + frameworkErr + "," + implErr);
                    }
                    if (mp != mMediaPlayer) {
                        if (DEBUG) {
                            Log.w(TAG, "onError() is ignored. mp is already gone.");
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
                        MediaPlayer mp, MediaItem2 dsd, SubtitleData2 data) {
                    if (DEBUG) {
                        Log.d(TAG, "onSubtitleData(): getTrackIndex: " + data.getTrackIndex()
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
                    final int index = data.getTrackIndex();
                    if (index != mSelectedSubtitleTrackIndex) {
                        return;
                    }
                    SubtitleTrack track = mSubtitleTracks.get(index);
                    if (track != null) {
                        track.onData(data);
                    }
                }

                @Override
                public void onPlayerStateChanged(@NonNull SessionPlayer2 player,
                        @SessionPlayer2.PlayerState int state) {
                    switch (state) {
                        case SessionPlayer2.PLAYER_STATE_IDLE:
                            mCurrentState = STATE_IDLE;
                            break;
                        case SessionPlayer2.PLAYER_STATE_PLAYING:
                            mCurrentState = STATE_PLAYING;
                            break;
                        case SessionPlayer2.PLAYER_STATE_PAUSED:
                            if (mCurrentState == STATE_PREPARING) {
                                onPrepared(player);
                            }
                            mCurrentState = STATE_PAUSED;
                            break;
                        case SessionPlayer2.PLAYER_STATE_ERROR:
                            mCurrentState = STATE_ERROR;
                            break;
                    }
                }

                private void onPrepared(SessionPlayer2 player) {
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
                                    new SessionCommand2(MediaControlView2.EVENT_UPDATE_TRACK_STATUS,
                                            null), data);
                        }

                        MediaMetadata2 metadata = extractMetadata();
                        if (metadata != null) {
                            mMediaItem.setMetadata(metadata);
                        }
                    }

                    if (mMediaControlView != null) {
                        mMediaControlView.setEnabled(true);

                        Uri uri = (mMediaItem instanceof UriMediaItem2)
                                ? ((UriMediaItem2) mMediaItem).getUri() : null;
                        if (uri != null) {
                            String scheme = uri.getScheme();
                            if (scheme != null) {
                                if (scheme.equals("file")) {
                                    mMediaControlView.setRouteSelector(null);
                                } else if (scheme.equals("http") || scheme.equals("https")) {
                                    mMediaControlView.setRouteSelector(mRouteSelector);
                                }
                            }
                        }
                    }

                    // mSeekWhenPrepared may be changed after seekTo() call
                    long seekToPosition = mSeekWhenPrepared;
                    if (seekToPosition != 0) {
                        mMediaSession.getPlayer().seekTo(seekToPosition);
                    }

                    if (player instanceof VideoView2Player) {
                        int videoWidth = ((VideoView2Player) player).getVideoWidth();
                        int videoHeight = ((VideoView2Player) player).getVideoHeight();

                        if (videoWidth != 0 && videoHeight != 0) {
                            if (videoWidth != mVideoWidth || videoHeight != mVideoHeight) {
                                mVideoWidth = videoWidth;
                                mVideoHeight = videoHeight;
                                mInstance.requestLayout();
                            }
                        }
                        if (needToStart()) {
                            mMediaSession.getPlayer().play();
                        }
                    }
                }

                private void onCompletion(MediaPlayer mp, MediaItem2 dsd) {
                    mCurrentState = STATE_PLAYBACK_COMPLETED;
                    mTargetState = STATE_PLAYBACK_COMPLETED;
                }
            };

    class MediaSessionCallback extends MediaSession2.SessionCallback {
        @Override
        public SessionCommandGroup2 onConnect(
                @NonNull MediaSession2 session,
                @NonNull MediaSession2.ControllerInfo controller) {
            if (session != mMediaSession) {
                if (DEBUG) {
                    Log.w(TAG, "onConnect() is ignored. session is already gone.");
                }
            }
            SessionCommandGroup2.Builder commandsBuilder = new SessionCommandGroup2.Builder()
                    .addCommand(SessionCommand2.COMMAND_CODE_PLAYER_PAUSE)
                    .addCommand(SessionCommand2.COMMAND_CODE_PLAYER_PLAY)
                    .addCommand(SessionCommand2.COMMAND_CODE_PLAYER_PREPARE)
                    .addCommand(SessionCommand2.COMMAND_CODE_PLAYER_SET_SPEED)
                    .addCommand(SessionCommand2.COMMAND_CODE_SESSION_FAST_FORWARD)
                    .addCommand(SessionCommand2.COMMAND_CODE_SESSION_REWIND)
                    .addCommand(SessionCommand2.COMMAND_CODE_PLAYER_SEEK_TO)
                    .addCommand(SessionCommand2.COMMAND_CODE_VOLUME_SET_VOLUME)
                    .addCommand(SessionCommand2.COMMAND_CODE_VOLUME_ADJUST_VOLUME)
                    .addCommand(SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_URI)
                    .addCommand(SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_URI)
                    .addCommand(SessionCommand2.COMMAND_CODE_PLAYER_GET_PLAYLIST)
                    .addCommand(SessionCommand2.COMMAND_CODE_PLAYER_GET_PLAYLIST_METADATA)
                    .addCommand(new SessionCommand2(
                            MediaControlView2.COMMAND_SELECT_AUDIO_TRACK, null))
                    .addCommand(new SessionCommand2(
                            MediaControlView2.COMMAND_SHOW_SUBTITLE, null))
                    .addCommand(new SessionCommand2(
                            MediaControlView2.COMMAND_HIDE_SUBTITLE, null));
            return commandsBuilder.build();
        }

        @Override
        public MediaSession2.SessionResult onCustomCommand(@NonNull MediaSession2 session,
                @NonNull MediaSession2.ControllerInfo controller,
                @NonNull SessionCommand2 customCommand, @Nullable Bundle args) {
            if (session != mMediaSession) {
                if (DEBUG) {
                    Log.w(TAG, "onCustomCommand() is ignored. session is already gone.");
                }
            }
            if (isRemotePlayback()) {
                // TODO: call mRoutePlayer.onCommand()
                return new MediaSession2.SessionResult(RESULT_CODE_SUCCESS, null);
            }
            switch (customCommand.getCustomCommand()) {
                case MediaControlView2.COMMAND_SHOW_SUBTITLE:
                    int subtitleIndex = args != null ? args.getInt(
                            MediaControlView2.KEY_SELECTED_SUBTITLE_INDEX,
                            INVALID_TRACK_INDEX) : INVALID_TRACK_INDEX;
                    if (subtitleIndex != INVALID_TRACK_INDEX) {
                        int subtitleTrackIndex = mSubtitleTracks.keyAt(subtitleIndex);
                        if (subtitleTrackIndex != mSelectedSubtitleTrackIndex) {
                            selectSubtitleTrack(subtitleTrackIndex);
                        }
                    }
                    break;
                case MediaControlView2.COMMAND_HIDE_SUBTITLE:
                    deselectSubtitleTrack();
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
            return new MediaSession2.SessionResult(RESULT_CODE_SUCCESS, null);
        }

        @Override
        public int onCommandRequest(@NonNull MediaSession2 session,
                @NonNull MediaSession2.ControllerInfo controller,
                @NonNull SessionCommand2 command) {
            if (session != mMediaSession) {
                if (DEBUG) {
                    Log.w(TAG, "onCommandRequest() is ignored. session is already gone.");
                }
            }
            switch (command.getCommandCode()) {
                case SessionCommand2.COMMAND_CODE_PLAYER_PLAY:
                    mTargetState = STATE_PLAYING;
                    if (!mCurrentView.hasAvailableSurface() && !mIsMusicMediaType) {
                        Log.d(TAG, "surface is not available");
                        return RESULT_CODE_INVALID_STATE;
                    }
                    break;
                case SessionCommand2.COMMAND_CODE_PLAYER_PAUSE:
                    mTargetState = STATE_PAUSED;
                    break;
                case SessionCommand2.COMMAND_CODE_PLAYER_SEEK_TO:
                    mSeekWhenPrepared = 0;
                    break;
            }
            return RESULT_CODE_SUCCESS;
        }
    }
}
