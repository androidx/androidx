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

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.AudioAttributesCompat;
import androidx.media2.MediaItem2;
import androidx.media2.SessionToken2;

/**
 * Interface for impl classes.
 */
interface VideoView2Impl {
    void initialize(
            VideoView2 instance, Context context,
            @Nullable AttributeSet attrs, int defStyleAttr);

    /**
     * Sets MediaControlView2 instance. It will replace the previously assigned MediaControlView2
     * instance if any.
     *
     * @param mediaControlView a media control view2 instance.
     * @param intervalMs a time interval in milliseconds until VideoView2 hides MediaControlView2.
     */
    void setMediaControlView2(@NonNull MediaControlView2 mediaControlView, long intervalMs);

    /**
     * Returns MediaControlView2 instance which is currently attached to VideoView2 by default or by
     * {@link #setMediaControlView2} method.
     */
    MediaControlView2 getMediaControlView2();

    /**
     * Returns {@link SessionToken2} so that developers create their own
     * {@link androidx.media2.MediaController2} instance. This method should be called when
     * VideoView2 is attached to window, or it throws IllegalStateException.
     *
     * @throws IllegalStateException if internal MediaSession is not created yet.
     */
    @NonNull
    SessionToken2 getMediaSessionToken2();

    /**
     * Sets the {@link AudioAttributesCompat} to be used during the playback of the video.
     *
     * @param attributes non-null <code>AudioAttributesCompat</code>.
     */
    void setAudioAttributes(@NonNull AudioAttributesCompat attributes);

    /**
     * Sets {@link MediaItem2} object to render using VideoView2.
     *
     * @param mediaItem the MediaItem2 to play
     */
    void setMediaItem2(@NonNull MediaItem2 mediaItem);

    /**
     * Selects which view will be used to render video between SurfaceView and TextureView.
     *
     * @param viewType the view type to render video
     * <ul>
     * <li>{@link VideoView2#VIEW_TYPE_SURFACEVIEW}
     * <li>{@link VideoView2#VIEW_TYPE_TEXTUREVIEW}
     * </ul>
     */
    void setViewType(@VideoView2.ViewType int viewType);

    /**
     * Returns view type.
     *
     * @return view type. See {@see setViewType}.
     */
    @VideoView2.ViewType
    int getViewType();

    /**
     * Registers a callback to be invoked when a view type change is done.
     * {@see #setViewType(int)}
     * @param l The callback that will be run
     */
    void setOnViewTypeChangedListener(VideoView2.OnViewTypeChangedListener l);

    void onAttachedToWindowImpl();

    void onDetachedFromWindowImpl();

    void onVisibilityAggregatedImpl(boolean isVisible);

    void onTouchEventImpl(MotionEvent ev);

    void onTrackballEventImpl(MotionEvent ev);

    void onMeasureImpl(int widthMeasureSpec, int heightMeasureSpec);
}
