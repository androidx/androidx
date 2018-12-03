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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.ParcelFileDescriptor;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.media.AudioAttributesCompat;
import androidx.media2.FileMediaItem;
import androidx.media2.MediaItem;
import androidx.media2.SessionToken;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Displays a video file.  VideoView class is a ViewGroup class which is wrapping
 * {@link MediaPlayer} so that developers can easily implement a video rendering application.
 *
 * <p>
 * <em> Data sources that VideoView supports : </em>
 * VideoView can play video files and audio-only files as
 * well. It can load from various sources such as resources or content providers. The supported
 * media file formats are the same as {@link MediaPlayer}.
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
 * <li> {@link VideoView} is integrated with {@link androidx.media2.MediaSession} and so
 * it responses with media key events.
 * </p>
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
 * @attr ref androidx.media2.widget.R.styleable#VideoView_enableControlView
 * @attr ref androidx.media2.widget.R.styleable#VideoView_viewType
 */
public class VideoView extends BaseLayout {
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

    private static final String TAG = "VideoView";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private VideoViewImpl mImpl;

    public VideoView(@NonNull Context context) {
        this(context, null);
    }

    public VideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (DEBUG) {
            Log.d(TAG, "Create VideoViewImplBase");
        }
        mImpl = new VideoViewImplBase();
        mImpl.initialize(this, context, attrs, defStyleAttr);
    }

    /**
     * Sets MediaControlView instance. It will replace the previously assigned MediaControlView
     * instance if any.
     *
     * @param mediaControlView a media control view2 instance.
     * @param intervalMs a time interval in milliseconds until VideoView hides MediaControlView.
     */
    public void setMediaControlView(@NonNull MediaControlView mediaControlView, long intervalMs) {
        mImpl.setMediaControlView(mediaControlView, intervalMs);
    }

    /**
     * Returns MediaControlView instance which is currently attached to VideoView by default or by
     * {@link #setMediaControlView} method.
     */
    @Nullable
    public MediaControlView getMediaControlView() {
        return mImpl.getMediaControlView();
    }

    /**
     * Returns {@link SessionToken} so that developers create their own
     * {@link androidx.media2.MediaController} instance. This method should be called when
     * VideoView is attached to window, or it throws IllegalStateException.
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
     * Sets {@link MediaItem} object to render using VideoView.
     * <p>
     * When the media item is a {@link FileMediaItem}, the {@link ParcelFileDescriptor}
     * in the {@link FileMediaItem} will be closed by the VideoView.
     *
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
        return VideoView.class.getName();
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
