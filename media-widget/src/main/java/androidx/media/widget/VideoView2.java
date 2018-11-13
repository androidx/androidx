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

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.VideoView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.media.AudioAttributesCompat;
import androidx.media2.MediaItem;
import androidx.media2.SessionToken;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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
 * VideoView's functionality. The main differences are
 * <ul>
 * <li> VideoView2 inherits ViewGroup and renders videos using SurfaceView and TextureView
 * selectively while VideoView inherits SurfaceView class.
 * <li> VideoView2 is integrated with MediaControlView2 and a default MediaControlView2 instance is
 * attached to VideoView2 by default.
 * <li> If a developer wants to attach a custom MediaControlView2,
 * assign the custom media control widget using {@link #setMediaControlView2}.
 * <li> VideoView2 is integrated with MediaSession and so it responses with media key events.
 * A VideoView2 keeps a MediaSession instance internally and connects it to a corresponding
 * MediaControlView2 instance.
 * </p>
 * </ul>
 *
 * <p>
 * <em> Audio focus and audio attributes : </em>
 * VideoView2 requests audio focus with {@link AudioManager#AUDIOFOCUS_GAIN} internally,
 * when playing a media content.
 * The default {@link AudioAttributesCompat} used during playback have a usage of
 * {@link AudioAttributesCompat#USAGE_MEDIA} and a content type of
 * {@link AudioAttributesCompat#CONTENT_TYPE_MOVIE},
 * use {@link #setAudioAttributes(AudioAttributesCompat)} to modify them.
 *
 * <p>
 * Note: VideoView2 does not retain its full state when going into the background. In particular, it
 * does not restore the current play state, play position, selected tracks. Applications should save
 * and restore these on their own in {@link android.app.Activity#onSaveInstanceState} and
 * {@link android.app.Activity#onRestoreInstanceState}.
 * @attr ref androidx.media.widget.R.styleable#VideoView2_enableControlView
 * @attr ref androidx.media.widget.R.styleable#VideoView2_viewType
 */
@TargetApi(Build.VERSION_CODES.P)
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

    private VideoView2Impl mImpl;

    public VideoView2(@NonNull Context context) {
        this(context, null);
    }

    public VideoView2(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoView2(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (DEBUG) {
            Log.d(TAG, "Create VideoView2ImplBase");
        }
        mImpl = new VideoView2ImplBase();
        mImpl.initialize(this, context, attrs, defStyleAttr);
    }

    /**
     * Sets MediaControlView2 instance. It will replace the previously assigned MediaControlView2
     * instance if any.
     *
     * @param mediaControlView a media control view2 instance.
     * @param intervalMs a time interval in milliseconds until VideoView2 hides MediaControlView2.
     */
    public void setMediaControlView2(@NonNull MediaControlView2 mediaControlView, long intervalMs) {
        mImpl.setMediaControlView2(mediaControlView, intervalMs);
    }

    /**
     * Returns MediaControlView2 instance which is currently attached to VideoView2 by default or by
     * {@link #setMediaControlView2} method.
     */
    @Nullable
    public MediaControlView2 getMediaControlView2() {
        return mImpl.getMediaControlView2();
    }

    /**
     * Returns {@link SessionToken} so that developers create their own
     * {@link androidx.media2.MediaController} instance. This method should be called when
     * VideoView2 is attached to window, or it throws IllegalStateException.
     *
     * @throws IllegalStateException if internal MediaSession is not created yet.
     */
    @NonNull
    public SessionToken getSessionToken() {
        return mImpl.getSessionToken();
    }

    /**
     * Sets the {@link AudioAttributesCompat} to be used during the playback of the video.
     *
     * @param attributes non-null <code>AudioAttributesCompat</code>.
     */
    public void setAudioAttributes(@NonNull AudioAttributesCompat attributes) {
        mImpl.setAudioAttributes(attributes);
    }

    /**
     * Sets {@link MediaItem} object to render using VideoView2.
     * @param mediaItem the MediaItem2 to play
     */
    public void setMediaItem(@NonNull MediaItem mediaItem) {
        mImpl.setMediaItem(mediaItem);
    }

    /**
     * Selects which view will be used to render video between SurfaceView and TextureView.
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
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        mImpl.onVisibilityAggregatedImpl(isVisible);
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
}
