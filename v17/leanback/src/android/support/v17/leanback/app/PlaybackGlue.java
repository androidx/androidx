/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.app;

import android.content.Context;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.PlaybackRowPresenter;
import android.support.v17.leanback.widget.Row;
import android.view.View;

/**
 * Base class for {@link PlaybackControlGlue}.
 */
public abstract class PlaybackGlue {
    private final Context mContext;
    private PlaybackGlueHost mPlaybackGlueHost;

    /**
     * Returns true when the media player is ready to start media playback. Subclasses must
     * implement this method correctly.
     */
    public boolean isReadyForPlayback() { return true; }

    /**
     * Interface to allow clients to take action once the video is ready to play.
     */
    public static abstract class PlayerCallback {
        /**
         * This method is fired when the video is ready for playback.
         */
        public abstract void onReadyForPlayback();
    }

    /**
     * Lifecycle callbacks triggered by the host(fragment e.g.) hosting the video controls/surface.
     */
    public static abstract class HostLifecycleCallback {
        /**
         * Callback triggered once the host(fragment) has started.
         */
        public abstract void onHostStart();

        /**
         * Callback triggered once the host(fragment) has finished.
         */
        public abstract void onHostStop();
    }

    /**
     * Sets the {@link PlayerCallback} callback.
     */
    public void setPlayerCallback(MediaPlayerGlue.PlayerCallback mPlayerCallback) {}

    /**
     * Starts the media player.
     */
    public void play() {}

    /**
     * Pauses the media player.
     */
    public void pause() {}

    /**
     * Goes to the next media item.
     */
    public void next() {}

    /**
     * Goes to the previous media item.
     */
    public void previous() {}

    /**
     * This class represents the UI {@link PlaybackFragment} hosting playback controls and
     * defines the interaction between {@link PlaybackGlue} and the host.
     */
    public static class PlaybackGlueHost {

        /**
         * Enables or disables view fading.  If enabled, the view will be faded in when the
         * fragment starts and will fade out after a time period.
         */
        public void setFadingEnabled(boolean enable) {
        }

        /**
         * Sets the {@link android.view.View.OnKeyListener} on the host. This would trigger
         * the listener when a {@link android.view.KeyEvent} is unhandled by the host.
         */
        public void setOnKeyInterceptListener(View.OnKeyListener onKeyListener) {
        }

        /**
         * Sets the {@link android.view.View.OnClickListener} on this fragment.
         */
        public void setOnActionClickedListener(OnActionClickedListener listener) {}

        /**
         * Sets the host {@link HostLifecycleCallback} callback on the host.
         */
        public void setHostLifeCycleCallback(HostLifecycleCallback callback) {
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
    }

    /**
     * Constructor.
     */
    public PlaybackGlue(Context context) {
        this.mContext = context;
    }

    /**
     * Returns the context.
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * This method is used to configure the {@link PlaybackGlueHost} with required listeners
     * and presenters.
     */
    public void setHost(PlaybackGlueHost host) {
        mPlaybackGlueHost = host;
    }

    /**
     * @return Associated {@link PlaybackGlueHost} or null if not attached to host.
     */
    public PlaybackGlueHost getHost() {
        return mPlaybackGlueHost;
    }
}
