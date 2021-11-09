/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.leanback.media;

import androidx.leanback.widget.PlaybackRowPresenter;
import androidx.leanback.widget.Row;

/**
 * Fake PlaybackGlueHost used by test.
 */
public class PlaybackGlueHostImpl extends PlaybackGlueHost {

    protected HostCallback mHostCallback;
    protected Row mRow;
    protected PlaybackRowPresenter mPlaybackRowPresenter;
    protected PlayerCallback mPlayerCallback;

    @Override
    public void setHostCallback(HostCallback callback) {
        mHostCallback = callback;
    }

    void notifyOnStart() {
        if (mHostCallback != null) {
            mHostCallback.onHostStart();
        }
    }

    void notifyOnStop() {
        if (mHostCallback != null) {
            mHostCallback.onHostStop();
        }
    }

    void notifyOnResume() {
        if (mHostCallback != null) {
            mHostCallback.onHostResume();
        }
    }

    void notifyOnPause() {
        if (mHostCallback != null) {
            mHostCallback.onHostPause();
        }
    }

    void notifyOnDestroy() {
        if (mHostCallback != null) {
            mHostCallback.onHostDestroy();
        }
    }

    @Override
    public void setPlaybackRow(Row row) {
        mRow = row;
    }

    @Override
    public void setPlaybackRowPresenter(PlaybackRowPresenter presenter) {
        mPlaybackRowPresenter = presenter;
    }

    @Override
    public PlayerCallback getPlayerCallback() {
        return mPlayerCallback;
    }

    public void setPlayerCallback(PlayerCallback playerCallback) {
        mPlayerCallback = playerCallback;
    }
}
