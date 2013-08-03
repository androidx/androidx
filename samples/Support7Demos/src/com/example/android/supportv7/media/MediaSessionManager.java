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

import java.util.List;
import java.util.ArrayList;
import android.util.Log;
import android.net.Uri;
import android.app.PendingIntent;
import android.support.v7.media.MediaItemStatus;

/**
 * MediaSessionManager manages a media session as a queue. It supports common
 * queuing behaviors such as enqueue/remove of media items, pause/resume/stop,
 * etc.
 *
 * Actual playback of a single media item is abstracted into a set of
 * callbacks MediaSessionManager.Callback, and is handled outside this class.
 */
public class MediaSessionManager {
    private static final String TAG = "MediaSessionManager";

    private String mSessionId;
    private String mItemId;
    private String mCurItemId;
    private boolean mIsPlaying = true;
    private Callback mCallback;
    private List<MediaQueueItem> mQueue = new ArrayList<MediaQueueItem>();

    public MediaSessionManager() {
    }

    // Queue item (this maps to the ENQUEUE in the API which queues the item)
    public MediaQueueItem enqueue(String sid, Uri uri, PendingIntent receiver) {
        // fail if queue id is invalid
        if (sid != null && !sid.equals(mSessionId)) {
            Log.d(TAG, "invalid session id, mSessionId="+mSessionId+", sid="+sid);
            return null;
        }

        // if queue id is unspecified, invalidate current queue
        if (sid == null) {
            invalidate();
        }

        mQueue.add(new MediaQueueItem(mSessionId, mItemId, uri, receiver));

        if (updatePlaybackState()) {
            MediaQueueItem item = findItem(mItemId);
            mItemId = inc(mItemId);
            if (item == null) {
                Log.d(TAG, "item not found after it's added");
            }
            return item;
        }

        removeItem(mItemId, MediaItemStatus.PLAYBACK_STATE_ERROR);
        return null;
    }

    public MediaQueueItem remove(String sid, String iid) {
        if (sid == null || !sid.equals(mSessionId)) {
            return null;
        }
        return removeItem(iid, MediaItemStatus.PLAYBACK_STATE_CANCELED);
    }

    // handles ERROR / COMPLETION
    public MediaQueueItem finish(boolean error) {
        return removeItem(mCurItemId, error ? MediaItemStatus.PLAYBACK_STATE_ERROR :
                MediaItemStatus.PLAYBACK_STATE_FINISHED);
    }

    public MediaQueueItem seek(String sid, String iid, long pos) {
        if (sid == null || !sid.equals(mSessionId)) {
            return null;
        }
        for (int i = 0; i < mQueue.size(); i++) {
            MediaQueueItem item = mQueue.get(i);
            if (iid.equals(item.getItemId())) {
                if (pos != item.getContentPosition()) {
                    item.setContentPosition(pos);
                    if (item.getState() == MediaItemStatus.PLAYBACK_STATE_PLAYING
                            || item.getState() == MediaItemStatus.PLAYBACK_STATE_PAUSED) {
                        if (mCallback != null) {
                            mCallback.onSeek(pos);
                        }
                    }
                }
                return item;
            }
        }
        return null;
    }

    public MediaQueueItem getCurrentItem() {
        return getStatus(mSessionId, mCurItemId);
    }

    public MediaQueueItem getStatus(String sid, String iid) {
        if (sid == null || !sid.equals(mSessionId)) {
            return null;
        }
        for (int i = 0; i < mQueue.size(); i++) {
            MediaQueueItem item = mQueue.get(i);
            if (iid.equals(item.getItemId())) {
                if (item.getState() == MediaItemStatus.PLAYBACK_STATE_PLAYING
                        || item.getState() == MediaItemStatus.PLAYBACK_STATE_PAUSED) {
                    if (mCallback != null) {
                        mCallback.onGetStatus(item);
                    }
                }
                return item;
            }
        }
        return null;
    }

    public boolean pause(String sid) {
        if (sid == null || !sid.equals(mSessionId)) {
            return false;
        }
        mIsPlaying = false;
        return updatePlaybackState();
    }

    public boolean resume(String sid) {
        if (sid == null || !sid.equals(mSessionId)) {
            return false;
        }
        mIsPlaying = true;
        return updatePlaybackState();
    }

    public boolean stop(String sid) {
        if (sid == null || !sid.equals(mSessionId)) {
            return false;
        }
        clear();
        return true;
    }

    public void setCallback(Callback cb) {
        mCallback = cb;
    }

    @Override
    public String toString() {
        String result = "Media Queue: ";
        if (!mQueue.isEmpty()) {
            for (MediaQueueItem item : mQueue) {
                result += "\n" + item.toString();
            }
        } else {
            result += "<empty>";
        }
        return result;
    }

    private String inc(String id) {
        return (id == null) ? "0" : Integer.toString(Integer.parseInt(id)+1);
    }

    // play the item at queue head
    private void play() {
        MediaQueueItem item = mQueue.get(0);
        if (item.getState() == MediaItemStatus.PLAYBACK_STATE_PENDING
                || item.getState() == MediaItemStatus.PLAYBACK_STATE_PAUSED) {
            mCurItemId = item.getItemId();
            if (mCallback != null) {
                if (item.getState() == MediaItemStatus.PLAYBACK_STATE_PENDING) {
                    mCallback.onNewItem(item.getUri());
                }
                mCallback.onStart();
            }
            item.setState(MediaItemStatus.PLAYBACK_STATE_PLAYING);
        }
    }

    // stop the currently playing item
    private void stop() {
        MediaQueueItem item = mQueue.get(0);
        if (item.getState() == MediaItemStatus.PLAYBACK_STATE_PLAYING
                || item.getState() == MediaItemStatus.PLAYBACK_STATE_PAUSED) {
            if (mCallback != null) {
                mCallback.onStop();
            }
            item.setState(MediaItemStatus.PLAYBACK_STATE_FINISHED);
        }
    }

    // pause the currently playing item
    private void pause() {
        MediaQueueItem item = mQueue.get(0);
        if (item.getState() == MediaItemStatus.PLAYBACK_STATE_PENDING
                || item.getState() == MediaItemStatus.PLAYBACK_STATE_PLAYING) {
            if (mCallback != null) {
                if (item.getState() == MediaItemStatus.PLAYBACK_STATE_PENDING) {
                    mCallback.onNewItem(item.getUri());
                } else if (item.getState() == MediaItemStatus.PLAYBACK_STATE_PLAYING) {
                    mCallback.onPause();
                }
            }
            item.setState(MediaItemStatus.PLAYBACK_STATE_PAUSED);
        }
    }

    private void clear() {
        if (mQueue.size() > 0) {
            stop();
            mQueue.clear();
        }
    }

    private void invalidate() {
        clear();
        mSessionId = inc(mSessionId);
        mItemId = "0";
        mIsPlaying = true;
    }

    private boolean updatePlaybackState() {
        if (mQueue.isEmpty()) {
            return true;
        }

        if (mIsPlaying) {
            play();
        } else {
            pause();
        }
        return true;
    }

    private MediaQueueItem findItem(String iid) {
        for (MediaQueueItem item : mQueue) {
            if (iid.equals(item.getItemId())) {
                return item;
            }
        }
        return null;
    }

    private MediaQueueItem removeItem(String iid, int state) {
        List<MediaQueueItem> queue =
                new ArrayList<MediaQueueItem>(mQueue.size());
        MediaQueueItem found = null;
        for (MediaQueueItem item : mQueue) {
            if (iid.equals(item.getItemId())) {
                if (item.getState() == MediaItemStatus.PLAYBACK_STATE_PLAYING
                        || item.getState() == MediaItemStatus.PLAYBACK_STATE_PAUSED) {
                    stop();
                }
                item.setState(state);
                found = item;
            } else {
                queue.add(item);
            }
        }
        if (found != null) {
            mQueue = queue;
            updatePlaybackState();
        }
        return found;
    }

    public interface Callback {
        public void onStart();
        public void onPause();
        public void onStop();
        public void onSeek(long pos);
        public void onGetStatus(MediaQueueItem item);
        public void onNewItem(Uri uri);
    }
}
