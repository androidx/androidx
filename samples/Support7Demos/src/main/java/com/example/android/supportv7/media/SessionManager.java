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

import android.app.PendingIntent;
import android.net.Uri;
import android.util.Log;

import androidx.mediarouter.media.MediaItemStatus;
import androidx.mediarouter.media.MediaSessionStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * SessionManager manages a media session as a queue. It supports common
 * queuing behaviors such as enqueue/remove of media items, pause/resume/stop,
 * etc.
 *
 * Actual playback of a single media item is abstracted into a Player interface,
 * and is handled outside this class.
 */
public class SessionManager implements Player.Callback {
    private static final String TAG = "SessionManager";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private String mName;
    private int mSessionId;
    private int mItemId;
    private boolean mPaused;
    private boolean mSessionValid;
    private Player mPlayer;
    private Callback mCallback;
    private List<PlaylistItem> mPlaylist = new ArrayList<PlaylistItem>();

    public SessionManager(String name) {
        mName = name;
    }

    public boolean isPaused() {
        return hasSession() && mPaused;
    }

    public boolean hasSession() {
        return mSessionValid;
    }

    public String getSessionId() {
        return mSessionValid ? Integer.toString(mSessionId) : null;
    }

    public PlaylistItem getCurrentItem() {
        return mPlaylist.isEmpty() ? null : mPlaylist.get(0);
    }

    // Returns the cached playlist (note this is not responsible for updating it)
    public List<PlaylistItem> getPlaylist() {
        return mPlaylist;
    }

    // Updates the playlist asynchronously, calls onPlaylistReady() when finished.
    public void updateStatus() {
        if (DEBUG) {
            log("updateStatus");
        }
        checkPlayer();
        // update the statistics first, so that the stats string is valid when
        // onPlaylistReady() gets called in the end
        mPlayer.takeSnapshot();

        if (mPlaylist.isEmpty()) {
            // If queue is empty, don't forget to call onPlaylistReady()!
            onPlaylistReady();
        } else if (mPlayer.isQueuingSupported()) {
            // If player supports queuing, get status of each item. Player is
            // responsible to call onPlaylistReady() after last getStatus().
            // (update=1 requires player to callback onPlaylistReady())
            for (int i = 0; i < mPlaylist.size(); i++) {
                PlaylistItem item = mPlaylist.get(i);
                mPlayer.getStatus(item, (i == mPlaylist.size() - 1) /* update */);
            }
        } else {
            // Otherwise, only need to get status for current item. Player is
            // responsible to call onPlaylistReady() when finished.
            mPlayer.getStatus(getCurrentItem(), true /* update */);
        }
    }

    public PlaylistItem add(String title, Uri uri, String mime) {
        return add(title, uri, mime, null);
    }

    public PlaylistItem add(String title, Uri uri, String mime, PendingIntent receiver) {
        if (DEBUG) {
            log("add: title=" + title + ", uri=" + uri + ", receiver=" + receiver);
        }
        // create new session if needed
        startSession();
        checkPlayerAndSession();

        // append new item with initial status PLAYBACK_STATE_PENDING
        PlaylistItem item = new PlaylistItem(Integer.toString(mSessionId),
                Integer.toString(mItemId), title, uri, mime, receiver);
        mPlaylist.add(item);
        mItemId++;

        // if player supports queuing, enqueue the item now
        if (mPlayer.isQueuingSupported()) {
            mPlayer.enqueue(item);
        }
        updatePlaybackState();
        return item;
    }

    public PlaylistItem remove(String iid) {
        if (DEBUG) {
            log("remove: iid=" + iid);
        }
        checkPlayerAndSession();
        return removeItem(iid, MediaItemStatus.PLAYBACK_STATE_CANCELED);
    }

    public PlaylistItem seek(String iid, long pos) {
        if (DEBUG) {
            log("seek: iid=" + iid +", pos=" + pos);
        }
        checkPlayerAndSession();
        // seeking on pending items are not yet supported
        checkItemCurrent(iid);

        PlaylistItem item = getCurrentItem();
        if (pos != item.getPosition()) {
            item.setPosition(pos);
            if (item.getState() == MediaItemStatus.PLAYBACK_STATE_PLAYING
                    || item.getState() == MediaItemStatus.PLAYBACK_STATE_PAUSED) {
                mPlayer.seek(item);
            }
        }
        return item;
    }

    public PlaylistItem getStatus(String iid) {
        checkPlayerAndSession();

        // This should only be called for local player. Remote player is
        // asynchronous, need to use updateStatus() instead.
        if (mPlayer.isRemotePlayback()) {
            throw new IllegalStateException(
                    "getStatus should not be called on remote player!");
        }

        for (PlaylistItem item : mPlaylist) {
            if (item.getItemId().equals(iid)) {
                if (item == getCurrentItem()) {
                    mPlayer.getStatus(item, false);
                }
                return item;
            }
        }
        return null;
    }

    public void pause() {
        if (DEBUG) {
            log("pause");
        }
        if (!mSessionValid) {
            return;
        }
        checkPlayer();
        mPaused = true;
        updatePlaybackState();
    }

    public void resume() {
        if (DEBUG) {
            log("resume");
        }
        if (!mSessionValid) {
            return;
        }
        checkPlayer();
        mPaused = false;
        updatePlaybackState();
    }

    public void stop() {
        if (DEBUG) {
            log("stop");
        }
        if (!mSessionValid) {
            return;
        }
        checkPlayer();
        mPlayer.stop();
        mPlaylist.clear();
        mPaused = false;
        updateStatus();
    }

    public String startSession() {
        if (!mSessionValid) {
            mSessionId++;
            mItemId = 0;
            mPaused = false;
            mSessionValid = true;
            return Integer.toString(mSessionId);
        }
        return null;
    }

    public boolean endSession() {
        if (mSessionValid) {
            mSessionValid = false;
            return true;
        }
        return false;
    }

    MediaSessionStatus getSessionStatus(String sid) {
        int sessionState = (sid != null && sid.equals(mSessionId)) ?
                MediaSessionStatus.SESSION_STATE_ACTIVE :
                    MediaSessionStatus.SESSION_STATE_INVALIDATED;

        return new MediaSessionStatus.Builder(sessionState)
                .setQueuePaused(mPaused)
                .build();
    }

    // Suspend the playback manager. Put the current item back into PENDING
    // state, and remember the current playback position. Called when switching
    // to a different player (route).
    public void suspend(long pos) {
        for (PlaylistItem item : mPlaylist) {
            item.setRemoteItemId(null);
            item.setDuration(0);
        }
        PlaylistItem item = getCurrentItem();
        if (DEBUG) {
            log("suspend: item=" + item + ", pos=" + pos);
        }
        if (item != null) {
            if (item.getState() == MediaItemStatus.PLAYBACK_STATE_PLAYING
                    || item.getState() == MediaItemStatus.PLAYBACK_STATE_PAUSED) {
                item.setState(MediaItemStatus.PLAYBACK_STATE_PENDING);
                item.setPosition(pos);
            }
        }
    }

    // Unsuspend the playback manager. Restart playback on new player (route).
    // This will resume playback of current item. Furthermore, if the new player
    // supports queuing, playlist will be re-established on the remote player.
    public void unsuspend() {
        if (DEBUG) {
            log("unsuspend");
        }
        if (mPlayer.isQueuingSupported()) {
            for (PlaylistItem item : mPlaylist) {
                mPlayer.enqueue(item);
            }
        }
        updatePlaybackState();
    }

    // Player.Callback
    @Override
    public void onError() {
        finishItem(true);
    }

    @Override
    public void onCompletion() {
        finishItem(false);
    }

    @Override
    public void onPlaylistChanged() {
        // Playlist has changed, update the cached playlist
        updateStatus();
    }

    @Override
    public void onPlaylistReady() {
        // Notify activity to update Ui
        if (mCallback != null) {
            mCallback.onStatusChanged();
        }
    }

    private void log(String message) {
        Log.d(TAG, mName + ": " + message);
    }

    private void checkPlayer() {
        if (mPlayer == null) {
            throw new IllegalStateException("Player not set!");
        }
    }

    private void checkSession() {
        if (!mSessionValid) {
            throw new IllegalStateException("Session not set!");
        }
    }

    private void checkPlayerAndSession() {
        checkPlayer();
        checkSession();
    }

    private void checkItemCurrent(String iid) {
        PlaylistItem item = getCurrentItem();
        if (item == null || !item.getItemId().equals(iid)) {
            throw new IllegalArgumentException("Item is not current!");
        }
    }

    private void updatePlaybackState() {
        PlaylistItem item = getCurrentItem();
        if (item != null) {
            if (item.getState() == MediaItemStatus.PLAYBACK_STATE_PENDING) {
                item.setState(mPaused ? MediaItemStatus.PLAYBACK_STATE_PAUSED
                        : MediaItemStatus.PLAYBACK_STATE_PLAYING);
                if (!mPlayer.isQueuingSupported()) {
                    mPlayer.play(item);
                }
            } else if (mPaused && item.getState() == MediaItemStatus.PLAYBACK_STATE_PLAYING) {
                mPlayer.pause();
                item.setState(MediaItemStatus.PLAYBACK_STATE_PAUSED);
            } else if (!mPaused && item.getState() == MediaItemStatus.PLAYBACK_STATE_PAUSED) {
                mPlayer.resume();
                item.setState(MediaItemStatus.PLAYBACK_STATE_PLAYING);
            }
            // notify client that item playback status has changed
            if (mCallback != null) {
                mCallback.onItemChanged(item);
            }
        } else {
            mPlayer.initMediaSession();
        }
        updateStatus();
    }

    private PlaylistItem removeItem(String iid, int state) {
        checkPlayerAndSession();
        List<PlaylistItem> queue =
                new ArrayList<PlaylistItem>(mPlaylist.size());
        PlaylistItem found = null;
        for (PlaylistItem item : mPlaylist) {
            if (iid.equals(item.getItemId())) {
                if (mPlayer.isQueuingSupported()) {
                    mPlayer.remove(item.getRemoteItemId());
                } else if (item.getState() == MediaItemStatus.PLAYBACK_STATE_PLAYING
                        || item.getState() == MediaItemStatus.PLAYBACK_STATE_PAUSED){
                    mPlayer.stop();
                }
                item.setState(state);
                found = item;
                // notify client that item is now removed
                if (mCallback != null) {
                    mCallback.onItemChanged(found);
                }
            } else {
                queue.add(item);
            }
        }
        if (found != null) {
            mPlaylist = queue;
            updatePlaybackState();
        } else {
            log("item not found");
        }
        return found;
    }

    private void finishItem(boolean error) {
        PlaylistItem item = getCurrentItem();
        if (item != null) {
            removeItem(item.getItemId(), error ?
                    MediaItemStatus.PLAYBACK_STATE_ERROR :
                        MediaItemStatus.PLAYBACK_STATE_FINISHED);
            updateStatus();
        }
    }

    // set the Player that this playback manager will interact with
    public void setPlayer(Player player) {
        mPlayer = player;
        checkPlayer();
        mPlayer.setCallback(this);
    }

    // provide a callback interface to tell the UI when significant state changes occur
    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    @Override
    public String toString() {
        String result = "Media Queue: ";
        if (!mPlaylist.isEmpty()) {
            for (PlaylistItem item : mPlaylist) {
                result += "\n" + item.toString();
            }
        } else {
            result += "<empty>";
        }
        return result;
    }

    public interface Callback {
        void onStatusChanged();
        void onItemChanged(PlaylistItem item);
    }
}
