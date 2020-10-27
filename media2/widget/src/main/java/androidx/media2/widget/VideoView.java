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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.media2.common.BaseResult;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.SessionPlayer.TrackInfo;
import androidx.media2.common.SubtitleData;
import androidx.media2.common.VideoSize;
import androidx.media2.session.MediaController;
import androidx.media2.session.MediaSession;
import androidx.palette.graphics.Palette;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A high level view for media playback that can be integrated with either a {@link SessionPlayer}
 * or a {@link MediaController}. Developers can easily implement a video rendering application
 * using this class. By default, a {@link MediaControlView} is attached so the playback
 * control buttons are displayed on top of VideoView.
 * <p>
 * Contents:
 * <ol>
 *     <li><a href="UseCases">Using VideoView with SessionPlayer or MediaController</a>
 *     <li><a href="UseWithMCV">Using VideoView with MediaControlView</a>
 *     <li><a href="ViewType">Choosing a view type</a>
 *     <li><a href="LegacyVideoView">Comparison with android.widget.VideoView</a>
 *     <li><a href="DisplayMetadata">Displaying Metadata</a>
 * </ol>
 *
 * <h3 id="UseCases">Using VideoView with SessionPlayer or MediaController</h3>
 * <ul>
 *     <li> For simple use cases that do not require communication with a {@link MediaSession},
 *     apps need to create a player instance that extends {@link SessionPlayer} (e.g.
 *     {@link androidx.media2.player.MediaPlayer}) and link it to this view by calling
 *     {@link #setPlayer}.
 *     <li> For more advanced use cases that require a {@link MediaSession} (e.g. handling media
 *     key events, integrating with other MediaSession apps as Assistant), apps need to create
 *     a {@link MediaController} that's attached to the {@link MediaSession} and link it to this
 *     view by calling {@link #setMediaController}.
 * </ul>
 *
 * <h3 id="UseWithMCV">Using VideoView with MediaControlView</h3>
 * {@link VideoView} is working with {@link MediaControlView} and a MediaControlView
 * instance is attached to VideoView by default.
 * <p>
 * If you want to attach a custom {@link MediaControlView}, assign the custom media
 * control widget using {@link #setMediaControlView}.
 * <p>
 * If you don't want to use {@link MediaControlView}, set
 * the VideoView attribute {@link androidx.media2.widget.R.attr#enableControlView} to false.
 *
 * <h3 id="ViewType">Choosing a view type</h3>
 * VideoView can render videos on a TextureView or SurfaceView. The
 * default is SurfaceView which can be changed by using the {@link #setViewType(int)} method or
 * by setting the {@link androidx.media2.widget.R.attr#viewType} attribute in the layout file.
 * <p> SurfaceView is recommended in most cases for saving battery life.
 * TextureView might be preferred for supporting various UIs such as animation and translucency.
 *
 * <h3 id="LegacyVideoView">Comparison with android.widget.VideoView</h3>
 * These are the main differences between the media2 VideoView widget and the older android widget:
 * <ul>
 * <li>
 *     {@link android.widget.VideoView android.widget.VideoView} creates a
 *     {@link android.media.MediaPlayer} instance internally and wraps playback APIs around it.
 *     <p>
 *     {@link VideoView androidx.media2.widget.VideoView} does not create a player instance
 *     internally. Instead, either a {@link SessionPlayer} or a {@link MediaController} instance
 *     should be created externally and link to {@link VideoView} using
 *     {@link #setPlayer(SessionPlayer)} or {@link #setMediaController(MediaController)},
 *     respectively.
 * <li>
 *     {@link android.widget.VideoView android.widget.VideoView} inherits from the SurfaceView
 *     class.
 *     <p>
 *     {@link VideoView androidx.media2.widget.VideoView} inherits from ViewGroup and can render
 *     videos using SurfaceView or TextureView, depending on your choice.
 * <li>
 *     A {@link VideoView} can respond to media key events if you call {@link #setMediaController}
 *     to link it to a {@link MediaController} that's connected to an active {@link MediaSession}.
 * </ul>
 *
 * <h3 id="DisplayMetadata">Displaying Metadata</h3>
 * When you play music only (sound with no video), VideoView can display album art and other
 * metadata by calling {@link MediaItem#setMetadata(MediaMetadata)}.
 * The following table shows the metadata displayed by the VideoView, and the default values
 * assigned if the keys are not set:
 * <table>
 *     <tr><th>Key</th><th>Default</th></tr>
 *     <tr><td>{@link MediaMetadata#METADATA_KEY_TITLE}</td>
 *     <td>{@link androidx.media2.widget.R.string#mcv2_music_title_unknown_text}</td></tr>
 *     <tr><td>{@link MediaMetadata#METADATA_KEY_ARTIST}</td>
 *     <td>{@link androidx.media2.widget.R.string#mcv2_music_artist_unknown_text}</td></tr>
 *     <tr><td>{@link MediaMetadata#METADATA_KEY_ALBUM_ART}</td>
 *     <td>{@link androidx.media2.widget.R.drawable#media2_widget_ic_default_album_image}</td></tr>
 *     </table>
 * <p>
 * Note: VideoView does not retain its full state when going into the background. In particular, it
 * does not save, and does not restore the current play state, play position, selected tracks.
 * Applications should save and restore these on their own in
 * {@link android.app.Activity#onSaveInstanceState} and
 * {@link android.app.Activity#onRestoreInstanceState}.
 * <p> Attributes :
 * <ul>
 *     <li> {@link androidx.media2.widget.R.attr#enableControlView}
 *     <li> {@link androidx.media2.widget.R.attr#viewType}
 * </ul>
 * <p> Example of attributes for a VideoView with TextureView and no attached control view:
 * <pre> {@code
 *  <androidx.media2.widget.VideoView
 *      android:id="@+id/video_view"
 *      widget:enableControlView="false"
 *      widget:viewType="textureView"
 *  />}</pre>
 *
 * @see MediaControlView
 * @see SessionPlayer
 * @see MediaController
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

    VideoView.OnViewTypeChangedListener mViewTypeChangedListener;

    VideoViewInterface mCurrentView;
    VideoViewInterface mTargetView;
    VideoTextureView mTextureView;
    VideoSurfaceView mSurfaceView;

    PlayerWrapper mPlayer;
    MediaControlView mMediaControlView;

    MusicView mMusicView;

    SelectiveLayout.LayoutParams mSelectiveLayoutParams;

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
        public void onSurfaceCreated(@NonNull View view, int width, int height) {
            if (DEBUG) {
                Log.d(TAG, "onSurfaceCreated()"
                        + ", width/height: " + width + "/" + height
                        + ", " + view.toString());
            }
            if (view == mTargetView && VideoView.this.isAggregatedVisible()) {
                mTargetView.assignSurfaceToPlayerWrapper(mPlayer);
            }
        }

        @Override
        public void onSurfaceDestroyed(@NonNull View view) {
            if (DEBUG) {
                Log.d(TAG, "onSurfaceDestroyed(). " + view.toString());
            }
        }

        @Override
        public void onSurfaceChanged(@NonNull View view, int width, int height) {
            if (DEBUG) {
                Log.d(TAG, "onSurfaceChanged(). width/height: " + width + "/" + height
                        + ", " + view.toString());
            }
        }

        @Override
        public void onSurfaceTakeOverDone(@NonNull VideoViewInterface view) {
            if (view != mTargetView) {
                Log.w(TAG, "onSurfaceTakeOverDone(). view is not targetView. ignore.: " + view);
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

        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();

        mTextureView = new VideoTextureView(context);
        mSurfaceView = new VideoSurfaceView(context);
        mTextureView.setSurfaceListener(mSurfaceListener);
        mSurfaceView.setSurfaceListener(mSurfaceListener);

        addView(mTextureView);
        addView(mSurfaceView);

        mSelectiveLayoutParams = new SelectiveLayout.LayoutParams();
        mSelectiveLayoutParams.forceMatchParent = true;

        mSubtitleAnchorView = new SubtitleAnchorView(context);
        mSubtitleAnchorView.setBackgroundColor(0);
        addView(mSubtitleAnchorView, mSelectiveLayoutParams);

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
                for (Map.Entry<TrackInfo, SubtitleTrack> pair : mSubtitleTracks.entrySet()) {
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
        mSubtitleController.registerRenderer(new Cea608CaptionRenderer(context));
        mSubtitleController.registerRenderer(new Cea708CaptionRenderer(context));
        mSubtitleController.setAnchor(mSubtitleAnchorView);

        mMusicView = new MusicView(context);
        mMusicView.setVisibility(View.GONE);
        addView(mMusicView, mSelectiveLayoutParams);

        boolean enableControlView = (attrs == null) || attrs.getAttributeBooleanValue(
                "http://schemas.android.com/apk/res-auto",
                "enableControlView", true);
        if (enableControlView) {
            mMediaControlView = new MediaControlView(context);
            mMediaControlView.setAttachedToVideoView(true);
            addView(mMediaControlView, mSelectiveLayoutParams);
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
     * Sets {@link MediaController} to display media content.
     * Setting a {@link MediaController} will unset any {@link MediaController} or
     * {@link SessionPlayer} that was previously set.
     * <p>
     * If VideoView has a {@link MediaControlView} instance, this controller will also be set to it.
     * <p>
     * Calling this method will automatically set VideoView's surface to {@link MediaController}
     * by calling {@link MediaController#setSurface(Surface)}. If the {@link MediaController} is
     * connected to a {@link MediaSession} and that {@link MediaSession} is associated with a
     * {@link SessionPlayer}, VideoView's surface will be set to that {@link SessionPlayer}.
     *
     * @param controller the controller
     * @see #setPlayer
     */
    public void setMediaController(@NonNull MediaController controller) {
        if (controller == null) {
            throw new NullPointerException("controller must not be null");
        }
        if (mPlayer != null) {
            mPlayer.detachCallback();
        }
        mPlayer = new PlayerWrapper(controller, ContextCompat.getMainExecutor(getContext()),
                new PlayerCallback());
        if (ViewCompat.isAttachedToWindow(this)) {
            mPlayer.attachCallback();
        }
        if (this.isAggregatedVisible()) {
            mTargetView.assignSurfaceToPlayerWrapper(mPlayer);
        } else {
            resetPlayerSurfaceWithNullAsync();
        }

        if (mMediaControlView != null) {
            mMediaControlView.setMediaControllerInternal(controller);
        }
    }


    /**
     * Sets {@link SessionPlayer} to display media content.
     * Setting a SessionPlayer will unset any MediaController or SessionPlayer that was previously
     * set.
     * <p>
     * If VideoView has a {@link MediaControlView} instance, this player will also be set to it.
     * <p>
     * Calling this method will automatically set VideoView's surface to {@link SessionPlayer}
     * by calling {@link SessionPlayer#setSurface(Surface)}.
     *
     * @param player the player
     * @see #setMediaController
     */
    public void setPlayer(@NonNull SessionPlayer player) {
        if (player == null) {
            throw new NullPointerException("player must not be null");
        }
        if (mPlayer != null) {
            mPlayer.detachCallback();
        }
        mPlayer = new PlayerWrapper(player, ContextCompat.getMainExecutor(getContext()),
                new PlayerCallback());
        if (ViewCompat.isAttachedToWindow(this)) {
            mPlayer.attachCallback();
        }
        if (this.isAggregatedVisible()) {
            mTargetView.assignSurfaceToPlayerWrapper(mPlayer);
        } else {
            resetPlayerSurfaceWithNullAsync();
        }

        if (mMediaControlView != null) {
            mMediaControlView.setPlayerInternal(player);
        }
    }

    /**
     * Sets {@link MediaControlView} instance. It will replace the previously assigned
     * {@link MediaControlView} instance if any.
     * <p>
     * If a {@link MediaController} or a {@link SessionPlayer} instance has been set to
     * {@link VideoView}, the same instance will be set to {@link MediaControlView}.
     *
     * @param mediaControlView a {@link MediaControlView} instance.
     * @param intervalMs time interval in milliseconds until {@link MediaControlView} transitions
     *                   into a different mode. -1 can be set to disable all UI transitions. See
     *                   {@link MediaControlView} Javadoc Section "UI transitions" for details.
     */
    public void setMediaControlView(@NonNull MediaControlView mediaControlView, long intervalMs) {
        if (mMediaControlView != null) {
            removeView(mMediaControlView);
            mMediaControlView.setAttachedToVideoView(false);
        }
        addView(mediaControlView, mSelectiveLayoutParams);
        mediaControlView.setAttachedToVideoView(true);

        mMediaControlView = mediaControlView;
        mMediaControlView.setDelayedAnimationInterval(intervalMs);

        if (mPlayer != null) {
            if (mPlayer.mController != null) {
                mMediaControlView.setMediaControllerInternal(mPlayer.mController);
            } else if (mPlayer.mPlayer != null) {
                mMediaControlView.setPlayerInternal(mPlayer.mPlayer);
            }
        }
    }

    /**
     * Returns {@link MediaControlView} instance which is currently attached to VideoView by default
     * or by {@link #setMediaControlView} method.
     */
    @Nullable
    public MediaControlView getMediaControlView() {
        return mMediaControlView;
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
        if (this.isAggregatedVisible()) {
            targetView.assignSurfaceToPlayerWrapper(mPlayer);
        }
        ((View) targetView).setVisibility(View.VISIBLE);
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
        if (mPlayer != null) {
            mPlayer.attachCallback();
        }
    }

    @Override
    void onVisibilityAggregatedCompat(boolean isVisible) {
        super.onVisibilityAggregatedCompat(isVisible);
        if (mPlayer == null) {
            return;
        }

        if (isVisible) {
            mTargetView.assignSurfaceToPlayerWrapper(mPlayer);
        } else {
            if (mPlayer == null || mPlayer.hasDisconnectedController()) {
                Log.w(TAG, "Surface is being destroyed, but player will not be informed "
                        + "as the associated media controller is disconnected.");
                return;
            }
            resetPlayerSurfaceWithNull();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mPlayer != null) {
            mPlayer.detachCallback();
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        // Class name may be obfuscated by Proguard. Hardcode the string for accessibility usage.
        return "androidx.media2.widget.VideoView";
    }

    ///////////////////////////////////////////////////
    // Protected or private methods
    ///////////////////////////////////////////////////
    boolean isMediaPrepared() {
        return mPlayer != null
                && mPlayer.getPlayerState() != SessionPlayer.PLAYER_STATE_ERROR
                && mPlayer.getPlayerState() != SessionPlayer.PLAYER_STATE_IDLE;
    }

    boolean hasActualVideo() {
        if (mVideoTrackCount > 0) {
            return true;
        }
        VideoSize videoSize = mPlayer.getVideoSize();
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

    void updateTracks(PlayerWrapper player, List<TrackInfo> trackInfos) {
        mSubtitleTracks = new LinkedHashMap<>();
        mVideoTrackCount = 0;
        mAudioTrackCount = 0;
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
        mSelectedSubtitleTrackInfo = player.getSelectedTrack(TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE);
    }

    void updateMusicView(MediaItem item) {
        boolean shouldShowMusicView = item != null && isCurrentItemMusic();
        if (shouldShowMusicView) {
            mMusicView.setVisibility(View.VISIBLE);

            MediaMetadata metadata = item.getMetadata();
            Resources resources = getResources();

            Drawable albumDrawable = getAlbumArt(metadata,
                    ContextCompat.getDrawable(
                            getContext(), R.drawable.media2_widget_ic_default_album_image));
            String title = getString(metadata, MediaMetadata.METADATA_KEY_TITLE,
                    resources.getString(R.string.mcv2_music_title_unknown_text));
            String artist = getString(metadata, MediaMetadata.METADATA_KEY_ARTIST,
                    resources.getString(R.string.mcv2_music_artist_unknown_text));

            mMusicView.setAlbumDrawable(albumDrawable);
            mMusicView.setTitleText(title);
            mMusicView.setArtistText(artist);
        } else {
            mMusicView.setVisibility(View.GONE);
            mMusicView.setAlbumDrawable(null);
            mMusicView.setTitleText(null);
            mMusicView.setArtistText(null);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void resetPlayerSurfaceWithNull() {
        try {
            int resultCode = mPlayer.setSurface(null).get(100, TimeUnit.MILLISECONDS)
                    .getResultCode();
            if (resultCode != BaseResult.RESULT_SUCCESS) {
                Log.e(TAG, "calling setSurface(null) was not "
                        + "successful. ResultCode: " + resultCode);
            }
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            Log.e(TAG, "calling setSurface(null) was not successful.", e);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void resetPlayerSurfaceWithNullAsync() {
        ListenableFuture<? extends BaseResult> future = mPlayer.setSurface(null);
        future.addListener(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            int resultCode = future.get().getResultCode();
                            if (resultCode != BaseResult.RESULT_SUCCESS) {
                                Log.e(TAG, "calling setSurface(null) was not "
                                        + "successful. ResultCode: " + resultCode);
                            }
                        } catch (ExecutionException | InterruptedException e) {
                            Log.e(TAG, "calling setSurface(null) was not successful.", e);
                        }
                    }
                }, ContextCompat.getMainExecutor(getContext()));
    }

    private Drawable getAlbumArt(@NonNull MediaMetadata metadata, Drawable defaultDrawable) {
        Drawable drawable = defaultDrawable;
        Bitmap bitmap = null;

        if (metadata != null && metadata.containsKey(MediaMetadata.METADATA_KEY_ALBUM_ART)) {
            bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        }
        if (bitmap != null) {
            Palette.Builder builder = Palette.from(bitmap);
            builder.generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    int dominantColor = palette.getDominantColor(0);
                    mMusicView.setBackgroundColor(dominantColor);
                }
            });
            drawable = new BitmapDrawable(getResources(), bitmap);
        } else {
            mMusicView.setBackgroundColor(ContextCompat.getColor(getContext(),
                    R.color.media2_widget_music_view_default_background));
        }
        return drawable;
    }

    private String getString(@NonNull MediaMetadata metadata, String stringKey,
            String defaultValue) {
        String value = (metadata == null) ? defaultValue : metadata.getString(stringKey);
        return value == null ? defaultValue : value;
    }

    class PlayerCallback extends PlayerWrapper.PlayerCallback {
        @Override
        void onConnected(@NonNull PlayerWrapper player) {
            if (DEBUG) {
                Log.d(TAG, "onConnected()");
            }
            if (shouldIgnoreCallback(player)) return;
            if (VideoView.this.isAggregatedVisible()) {
                mTargetView.assignSurfaceToPlayerWrapper(mPlayer);
            }
        }

        @Override
        void onVideoSizeChanged(@NonNull PlayerWrapper player, @NonNull VideoSize videoSize) {
            if (DEBUG) {
                Log.d(TAG, "onVideoSizeChanged(): size: " + videoSize);
            }
            if (shouldIgnoreCallback(player)) return;
            if (mVideoTrackCount == 0 && videoSize.getHeight() > 0 && videoSize.getWidth() > 0) {
                if (isMediaPrepared()) {
                    List<TrackInfo> trackInfos = player.getTracks();
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
        void onSubtitleData(@NonNull PlayerWrapper player, @NonNull MediaItem item,
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
            if (shouldIgnoreCallback(player)) return;
            if (!track.equals(mSelectedSubtitleTrackInfo)) {
                return;
            }
            SubtitleTrack subtitleTrack = mSubtitleTracks.get(track);
            if (subtitleTrack != null) {
                subtitleTrack.onData(data);
            }
        }

        @Override
        void onPlayerStateChanged(@NonNull PlayerWrapper player, int state) {
            if (DEBUG) {
                Log.d(TAG, "onPlayerStateChanged(): state: " + state);
            }
            if (shouldIgnoreCallback(player)) return;
            if (state == SessionPlayer.PLAYER_STATE_ERROR) {
                // TODO: Show error state (b/123498635)
            }
        }

        @Override
        void onCurrentMediaItemChanged(@NonNull PlayerWrapper player, @Nullable MediaItem item) {
            if (DEBUG) {
                Log.d(TAG, "onCurrentMediaItemChanged(): MediaItem: " + item);
            }
            if (shouldIgnoreCallback(player)) return;

            updateMusicView(item);
        }

        @Override
        void onTracksChanged(@NonNull PlayerWrapper player, @NonNull List<TrackInfo> tracks) {
            if (DEBUG) {
                Log.d(TAG, "onTrackInfoChanged(): tracks: " + tracks);
            }
            if (shouldIgnoreCallback(player)) return;
            updateTracks(player, tracks);
            updateMusicView(player.getCurrentMediaItem());
        }

        @Override
        void onTrackSelected(@NonNull PlayerWrapper player, @NonNull TrackInfo trackInfo) {
            if (DEBUG) {
                Log.d(TAG, "onTrackSelected(): selected track: " + trackInfo);
            }
            if (shouldIgnoreCallback(player)) return;
            SubtitleTrack subtitleTrack = mSubtitleTracks.get(trackInfo);
            if (subtitleTrack != null) {
                mSubtitleController.selectTrack(subtitleTrack);
            }
        }

        @Override
        void onTrackDeselected(@NonNull PlayerWrapper player, @NonNull TrackInfo trackInfo) {
            if (DEBUG) {
                Log.d(TAG, "onTrackDeselected(): deselected track: " + trackInfo);
            }
            if (shouldIgnoreCallback(player)) return;
            SubtitleTrack subtitleTrack = mSubtitleTracks.get(trackInfo);
            if (subtitleTrack != null) {
                mSubtitleController.selectTrack(null);
            }
        }

        private boolean shouldIgnoreCallback(@NonNull PlayerWrapper player) {
            if (player != mPlayer) {
                if (DEBUG) {
                    try {
                        final String methodName =
                                new Throwable().getStackTrace()[1].getMethodName();
                        Log.w(TAG, methodName + " should be ignored. player is already gone.");
                    } catch (IndexOutOfBoundsException e) {
                        Log.w(TAG, "A PlayerCallback should be ignored. player is already gone.");
                    }
                }
                return true;
            }
            return false;
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
