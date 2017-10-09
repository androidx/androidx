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

package com.example.android.leanback;

import android.app.Activity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

/**
 * Host PlaybackFragment and provide PIP events.
 */
public class PlaybackTransportControlActivity extends Activity {
    private List<PictureInPictureListener> mListeners = new ArrayList<>();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playback_transportcontrol_activity);
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        for (PictureInPictureListener listener : mListeners) {
            listener.onPictureInPictureModeChanged(isInPictureInPictureMode);
        }
    }

    /**
     * Register a PIP listener.
     */
    public void registerPictureInPictureListener(PictureInPictureListener listener) {
        mListeners.add(listener);
    }

    /**
     * Unregister a PIP listener.
     */
    public void unregisterPictureInPictureListener(PictureInPictureListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Interface of PIP event on Activity.
     */
    public interface PictureInPictureListener {
        /**
         * Called when Activity's PIP mode is changed.
         */
        void onPictureInPictureModeChanged(boolean isInPictureInPictureMode);
    }
}
