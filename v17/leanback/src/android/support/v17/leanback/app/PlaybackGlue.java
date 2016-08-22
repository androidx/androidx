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
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.view.InputEvent;
import android.view.View;

/**
 * Base class for {@link PlaybackControlGlue}.
 */
public abstract class PlaybackGlue {
    private final Context mContext;
    protected PlaybackGlueHost mPlaybackGlueHost;

    /**
     * This defines the interaction between this class and the fragment hosting the playback
     * controls.
     */
    public static class PlaybackGlueHost {

        public void setFadingEnabled(boolean enable) {
        }

        public void setOnKeyInterceptListener(View.OnKeyListener onKeyListener) {
        }

        public void setOnActionClickedListener(OnActionClickedListener listener) {}
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
     * Sets the {@link PlaybackGlueHost}.
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
