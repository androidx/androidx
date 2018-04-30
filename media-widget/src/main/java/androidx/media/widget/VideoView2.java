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
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.VideoView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.media.AudioAttributesCompat;
import androidx.media.DataSourceDesc;
import androidx.media.MediaItem2;
import androidx.media.MediaMetadata2;
import androidx.media.SessionToken2;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Displays a video file.  VideoView2 class is a ViewGroup class which is wrapping
 * {@link MediaPlayer} so that developers can easily implement a video rendering application.
 *
 * <p>
 * <em> Data sources that VideoView2 supports : </em>
 * VideoView2 can play video files and audio-only files as
 * well. It can load from various sources such as resources or content providers. The supported
 * media file formats are the same as {@link MediaPlayer}.
 *
 * <p>
 * <em> View type can be selected : </em>
 * VideoView2 can render videos on top of TextureView as well as
 * SurfaceView selectively. The default is SurfaceView and it can be changed using
 * {@link #setViewType(int)} method. Using SurfaceView is recommended in most cases for saving
 * battery. TextureView might be preferred for supporting various UIs such as animation and
 * translucency.
 *
 * <p>
 * <em> Differences between {@link VideoView} class : </em>
 * VideoView2 covers and inherits the most of
 * VideoView's functionalities. The main differences are
 * <ul>
 * <li> VideoView2 inherits ViewGroup and renders videos using SurfaceView and TextureView
 * selectively while VideoView inherits SurfaceView class.
 * <li> VideoView2 is integrated with MediaControlView2 and a default MediaControlView2 instance is
 * attached to VideoView2 by default.
 * <li> If a developer wants to attach a customed MediaControlView2,
 * assign the customed media control widget using {@link #setMediaControlView2}.
 * <li> VideoView2 is integrated with MediaSession and so it responses with media key events.
 * A VideoView2 keeps a MediaSession instance internally and connects it to a corresponding
 * MediaControlView2 instance.
 * </p>
 * </ul>
 *
 * <p>
 * <em> Audio focus and audio attributes : </em>
 * By default, VideoView2 requests audio focus with
 * {@link AudioManager#AUDIOFOCUS_GAIN}. Use {@link #setAudioFocusRequest(int)} to change this
 * behavior. The default {@link AudioAttributes} used during playback have a usage of
 * {@link AudioAttributes#USAGE_MEDIA} and a content type of
 * {@link AudioAttributes#CONTENT_TYPE_MOVIE}, use {@link #setAudioAttributes(AudioAttributes)} to
 * modify them.
 *
 * <p>
 * Note: VideoView2 does not retain its full state when going into the background. In particular, it
 * does not restore the current play state, play position, selected tracks. Applications should save
 * and restore these on their own in {@link android.app.Activity#onSaveInstanceState} and
 * {@link android.app.Activity#onRestoreInstanceState}.
 */
@RequiresApi(21) // TODO correct minSdk API use incompatibilities and remove before release.
public class VideoView2 extends BaseLayout {
    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({
            VIEW_TYPE_TEXTUREVIEW,
            VIEW_TYPE_SURFACEVIEW
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ViewType {}

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

    private static final String TAG = "VideoView2";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private static final boolean USE_MP2 = Log.isLoggable("VV2MP2", Log.DEBUG);

    private VideoView2Impl mImpl;

    public VideoView2(@NonNull Context context) {
        this(context, null);
    }

    public VideoView2(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoView2(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            if (USE_MP2) {
                Log.d(TAG, "Create VideoView2ImplBase");
                mImpl = new VideoView2ImplBase();
            } else {
                Log.d(TAG, "Create VideoView2ImplApi28WithMp1");
                mImpl = new VideoView2ImplApi28WithMp1();
            }
        } else {
            Log.d(TAG, "Create VideoView2ImplBaseWithMp1");
            mImpl = new VideoView2ImplBaseWithMp1();
        }
        mImpl.initialize(this, context, attrs, defStyleAttr);
    }

    /**
     * Sets MediaControlView2 instance. It will replace the previously assigned MediaControlView2
     * instance if any.
     *
     * @param mediaControlView a media control view2 instance.
     * @param intervalMs a time interval in milliseconds until VideoView2 hides MediaControlView2.
     */
    public void setMediaControlView2(MediaControlView2 mediaControlView, long intervalMs) {
        mImpl.setMediaControlView2(mediaControlView, intervalMs);
    }

    /**
     * Returns MediaControlView2 instance which is currently attached to VideoView2 by default or by
     * {@link #setMediaControlView2} method.
     */
    public MediaControlView2 getMediaControlView2() {
        return mImpl.getMediaControlView2();
    }

    /**
     * Sets MediaMetadata2 instance. It will replace the previously assigned MediaMetadata2 instance
     * if any.
     *
     * @param metadata a MediaMetadata2 instance.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void setMediaMetadata(MediaMetadata2 metadata) {
      //mProvider.setMediaMetadata_impl(metadata);
    }

    /**
     * Returns MediaMetadata2 instance which is retrieved from MediaPlayer inside VideoView2 by
     * default or by {@link #setMediaMetadata} method.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public MediaMetadata2 getMediaMetadata() {
        return mImpl.getMediaMetadata();
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
    public MediaControllerCompat getMediaController() {
        return mImpl.getMediaController();
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
    public SessionToken2 getMediaSessionToken() {
        return null;
    }

    /**
     * Shows or hides closed caption or subtitles if there is any.
     * The first subtitle track will be chosen if there multiple subtitle tracks exist.
     * Default behavior of VideoView2 is not showing subtitle.
     * @param enable shows closed caption or subtitles if this value is true, or hides.
     */
    public void setSubtitleEnabled(boolean enable) {
        mImpl.setSubtitleEnabled(enable);
    }

    /**
     * Returns true if showing subtitle feature is enabled or returns false.
     * Although there is no subtitle track or closed caption, it can return true, if the feature
     * has been enabled by {@link #setSubtitleEnabled}.
     */
    public boolean isSubtitleEnabled() {
        return mImpl.isSubtitleEnabled();
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
    public void setSpeed(float speed) {
        mImpl.setSpeed(speed);
    }

    /**
     * Returns playback speed.
     *
     * It returns the same value that has been set by {@link #setSpeed}, if it was available value.
     * If {@link #setSpeed} has not been called before, then the normal speed 1.0f will be returned.
     */
    public float getSpeed() {
        return mImpl.getSpeed();
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
    public void setAudioFocusRequest(int focusGain) {
        mImpl.setAudioFocusRequest(focusGain);
    }

    /**
     * Sets the {@link AudioAttributes} to be used during the playback of the video.
     *
     * @param attributes non-null <code>AudioAttributes</code>.
     */
    public void setAudioAttributes(@NonNull AudioAttributes attributes) {
        mImpl.setAudioAttributes(AudioAttributesCompat.wrap(attributes));
    }

    /**
     * Sets the {@link AudioAttributesCompat} to be used during the playback of the video.
     *
     * @param attributes non-null <code>AudioAttributesCompat</code>.
     *
     * @hide TODO unhide and remove setAudioAttributes with framework attributes
     */
    @RestrictTo(LIBRARY_GROUP)
    public void setAudioAttributes(@NonNull AudioAttributesCompat attributes) {
        mImpl.setAudioAttributes(attributes);
    }

    /**
     * Sets video path.
     *
     * @param path the path of the video.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void setVideoPath(String path) {
        mImpl.setVideoUri(Uri.parse(path));
    }

    /**
     * Sets video URI.
     *
     * @param uri the URI of the video.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void setVideoUri(Uri uri) {
        mImpl.setVideoUri(uri, null);
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
    public void setVideoUri(Uri uri, @Nullable Map<String, String> headers) {
        mImpl.setVideoUri(uri, headers);
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
    public void setMediaItem(@NonNull MediaItem2 mediaItem) {
    }

    /**
     * Sets {@link DataSourceDesc} object to render using VideoView2.
     * @param dataSource the {@link DataSourceDesc} object to play.
     * @see #setMediaItem
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void setDataSource(@NonNull DataSourceDesc dataSource) {
    }

    /**
     * Selects which view will be used to render video between SurfacView and TextureView.
     *
     * @param viewType the view type to render video
     * <ul>
     * <li>{@link #VIEW_TYPE_SURFACEVIEW}
     * <li>{@link #VIEW_TYPE_TEXTUREVIEW}
     * </ul>
     */
    public void setViewType(@ViewType int viewType) {
        mImpl.setViewType(viewType);
    }

    /**
     * Returns view type.
     *
     * @return view type. See {@see setViewType}.
     */
    @ViewType
    public int getViewType() {
        return mImpl.getViewType();
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
    public void setCustomActions(List<PlaybackStateCompat.CustomAction> actionList,
            Executor executor, OnCustomActionListener listener) {
        mImpl.setCustomActions(actionList, executor, listener);
    }

    /**
     * Registers a callback to be invoked when a view type change is done.
     * {@see #setViewType(int)}
     * @param l The callback that will be run
     * @hide
     */
    @VisibleForTesting
    @RestrictTo(LIBRARY_GROUP)
    public void setOnViewTypeChangedListener(OnViewTypeChangedListener l) {
        mImpl.setOnViewTypeChangedListener(l);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mImpl.onAttachedToWindowImpl();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mImpl.onDetachedFromWindowImpl();
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return VideoView2.class.getName();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mImpl.onTouchEventImpl(ev);
        return super.onTouchEvent(ev);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        mImpl.onTrackballEventImpl(ev);
        return super.onTrackballEvent(ev);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mImpl.onMeasureImpl(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * Interface definition of a callback to be invoked when the view type has been changed.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
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
        void onViewTypeChanged(View view, @ViewType int viewType);
    }

    /**
     * Interface definition of a callback to be invoked to inform that a custom action is performed.
     * @hide  TODO remove
     */
    @RestrictTo(LIBRARY_GROUP)
    public interface OnCustomActionListener {
        /**
         * Called to indicate that a custom action is performed.
         *
         * @param action The action that was originally sent in the
         *               {@link PlaybackStateCompat.CustomAction}.
         * @param extras Optional extras.
         */
        void onCustomAction(String action, Bundle extras);
    }
}
