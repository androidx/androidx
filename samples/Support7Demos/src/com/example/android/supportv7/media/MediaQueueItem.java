/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.supportv7.media;

import android.support.v7.media.MediaItemStatus;
import android.net.Uri;
import android.app.PendingIntent;

/**
 * MediaQueueItem helps keep track of the current status of an media item.
 */
final class MediaQueueItem {
    // immutables
    private final String mSessionId;
    private final String mItemId;
    private final Uri mUri;
    private final PendingIntent mUpdateReceiver;
    // changeable states
    private int mPlaybackState = MediaItemStatus.PLAYBACK_STATE_PENDING;
    private long mContentPosition;
    private long mContentDuration;

    public MediaQueueItem(String qid, String iid, Uri uri, PendingIntent pi) {
        mSessionId = qid;
        mItemId = iid;
        mUri = uri;
        mUpdateReceiver = pi;
    }

    public void setState(int state) {
        mPlaybackState = state;
    }

    public void setContentPosition(long pos) {
        mContentPosition = pos;
    }

    public void setContentDuration(long duration) {
        mContentDuration = duration;
    }

    public String getSessionId() {
        return mSessionId;
    }

    public String getItemId() {
        return mItemId;
    }

    public Uri getUri() {
        return mUri;
    }

    public PendingIntent getUpdateReceiver() {
        return mUpdateReceiver;
    }

    public int getState() {
        return mPlaybackState;
    }

    public long getContentPosition() {
        return mContentPosition;
    }

    public long getContentDuration() {
        return mContentDuration;
    }

    public MediaItemStatus getStatus() {
        return new MediaItemStatus.Builder(mPlaybackState)
            .setContentPosition(mContentPosition)
            .setContentDuration(mContentDuration)
            .build();
    }

    @Override
    public String toString() {
        String state[] = {
            "PENDING",
            "PLAYING",
            "PAUSED",
            "BUFFERING",
            "FINISHED",
            "CANCELED",
            "INVALIDATED",
            "ERROR"
        };
        return "[" + mSessionId + "|" + mItemId + "|"
            + state[mPlaybackState] + "] " + mUri.toString();
    }
}