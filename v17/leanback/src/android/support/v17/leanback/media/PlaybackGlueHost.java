/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.v17.leanback.media;

import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.PlaybackRowPresenter;
import android.support.v17.leanback.widget.Row;
import android.view.View;

/**
 * This class represents the UI (e.g. Fragment/Activity) hosting playback controls and
 * defines the interaction between {@link PlaybackGlue} and the host.
 * PlaybackGlueHost provides the following functions:
 * <li>Render UI of PlaybackGlue: {@link #setPlaybackRow(Row)},
 * {@link #setPlaybackRowPresenter(PlaybackRowPresenter)}.
 * </li>
 * <li>Callback for fragment/activity onStart/onStop: {@link #setHostCallback(HostCallback)}.
 * </li>
 * <li>Auto fade out controls after a short period: {@link #setFadingEnabled(boolean)}.
 * </li>
 * <li>Key listener and ActionListener. {@link #setOnKeyInterceptListener(View.OnKeyListener)},
 * {@link #setOnActionClickedListener(OnActionClickedListener)}.
 * </li>
 *
 * Subclass of PlaybackGlueHost may implement optional interface e.g. {@link SurfaceHolderGlueHost}
 * to provide SurfaceView. These optional interface should be used during
 * {@link PlaybackGlue#setHost(PlaybackGlueHost)}.
 */
public abstract class PlaybackGlueHost {
    PlaybackGlue mGlue;

    /**
     * Callbacks triggered by the host(e.g. fragment) hosting the video controls/surface.
     *
     * @see #setHostCallback(HostCallback)
     */
    public abstract static class HostCallback {
        /**
         * Callback triggered once the host(fragment) has started.
         */
        public void onHostStart() {
        }

        /**
         * Callback triggered once the host(fragment) has stopped.
         */
        public void onHostStop() {
        }

        /**
         * Callback triggered once the host(fragment) has paused.
         */
        public void onHostPause() {
        }

        /**
         * Callback triggered once the host(fragment) has resumed.
         */
        public void onHostResume() {
        }
    }

    /**
     * Enables or disables view fading.  If enabled, the view will be faded in when the
     * fragment starts and will fade out after a time period.
     */
    public void setFadingEnabled(boolean enable) {
    }

    /**
     * Fade out views immediately.
     */
    public void fadeOut() {
    }

    /**
     * Sets the {@link android.view.View.OnKeyListener} on the host. This would trigger
     * the listener when a {@link android.view.KeyEvent} is unhandled by the host.
     */
    public void setOnKeyInterceptListener(View.OnKeyListener onKeyListener) {
    }

    /**
     * Sets the {@link View.OnClickListener} on this fragment.
     */
    public void setOnActionClickedListener(OnActionClickedListener listener) {}

    /**
     * Sets the host {@link HostCallback} callback on the host. This method should only be called
     * by {@link PlaybackGlue}. App should not directly call this method, app should override
     * {@link PlaybackGlue#onHostStart()} etc.
     */
    public void setHostCallback(HostCallback callback) {
    }

    /**
     * Notifies host about a change so it can update the view.
     */
    public void notifyPlaybackRowChanged() {}

    /**
     * Sets {@link PlaybackRowPresenter} for rendering the playback controls.
     */
    public void setPlaybackRowPresenter(PlaybackRowPresenter presenter) {}

    /**
     * Sets the {@link Row} that represents the information on control items that needs
     * to be rendered.
     */
    public void setPlaybackRow(Row row) {}

    final void attachToGlue(PlaybackGlue glue) {
        if (mGlue != null) {
            mGlue.onDetachedFromHost();
        }
        mGlue = glue;
        if (mGlue != null) {
            mGlue.onAttachedToHost(this);
        }
    }

}
