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

package androidx.media.test.service;

import static androidx.media.test.lib.CommonConstants.ACTION_MEDIA_CONTROLLER2;
import static androidx.media.test.lib.CommonConstants.MEDIA_CONTROLLER2_PROVIDER_SERVICE;
import static androidx.media.test.lib.TestUtils.TIMEOUT_MS;

import static junit.framework.TestCase.fail;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.mediacompat.testlib.IRemoteMediaController2;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.test.lib.TestUtils;
import androidx.media2.MediaController2;
import androidx.media2.MediaMetadata2;
import androidx.media2.MediaUtils2;
import androidx.media2.Rating2;
import androidx.media2.SessionCommand2;
import androidx.media2.SessionToken2;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Represents remote {@link MediaController2} the client app's MediaController2ProviderService.
 * Users can run {@link MediaController2} methods remotely with this object.
 */
public class RemoteMediaController2 {
    static final String TAG = "RemoteMediaController2";

    final String mControllerId;
    final Context mContext;
    final CountDownLatch mCountDownLatch;

    ServiceConnection mServiceConnection;
    IRemoteMediaController2 mBinder;

    /**
     * Create a {@link MediaController2} in the client app.
     * Should NOT be called main thread.
     *
     * @param waitForConnection true if the remote controller needs to wait for the connection,
     *                          false otherwise.
     */
    public RemoteMediaController2(Context context, SessionToken2 token, boolean waitForConnection) {
        mContext = context;
        mControllerId = UUID.randomUUID().toString();
        mCountDownLatch = new CountDownLatch(1);
        mServiceConnection = new MyServiceConnection();
        if (!connect()) {
            fail("Failed to connect to the MediaController2ProviderService.");
        }
        create(token, waitForConnection);
    }

    public void cleanUp() {
        close();
        disconnect();
    }

    ////////////////////////////////////////////////////////////////////////////////
    // MediaController2 methods
    ////////////////////////////////////////////////////////////////////////////////

    public SessionToken2 getConnectedSessionToken() {
        try {
            return MediaUtils2.fromParcelable(mBinder.getConnectedSessionToken(mControllerId));
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call getConnectedSessionToken()");
            return null;
        }
    }

    public void play() {
        try {
            mBinder.play(mControllerId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call play()");
        }
    }

    public void pause() {
        try {
            mBinder.pause(mControllerId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call pause()");
        }
    }

    public void prepare() {
        try {
            mBinder.prepare(mControllerId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call prepare()");
        }
    }

    public void seekTo(long pos) {
        try {
            mBinder.seekTo(mControllerId, pos);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call seekTo()");
        }
    }

    public void setPlaybackSpeed(float speed) {
        try {
            mBinder.setPlaybackSpeed(mControllerId, speed);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call setPlaybackSpeed()");
        }
    }

    public void setPlaylist(@NonNull List<String> list, @Nullable MediaMetadata2 metadata) {
        try {
            mBinder.setPlaylist(mControllerId, list, MediaUtils2.toParcelable(metadata));
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call setPlaylist()");
        }
    }

    /**
     * Client app will automatically create a playlist of size {@param size},
     * and call MediaController2#setPlaylist() with the list.
     *
     * Each item's media ID will be {@link TestUtils#getMediaIdInDummyList(int)}.
     */
    public void createAndSetDummyPlaylist(int size, @Nullable MediaMetadata2 metadata) {
        try {
            mBinder.createAndSetDummyPlaylist(mControllerId, size,
                    MediaUtils2.toParcelable(metadata));
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call createAndSetDummyPlaylist()");
        }
    }

    public void setMediaItem(@NonNull String mediaId) {
        try {
            mBinder.setMediaItem(mControllerId, mediaId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call setMediaItem()");
        }
    }

    public void updatePlaylistMetadata(@Nullable MediaMetadata2 metadata) {
        try {
            mBinder.updatePlaylistMetadata(mControllerId, MediaUtils2.toParcelable(metadata));
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call updatePlaylistMetadata()");
        }
    }

    public void addPlaylistItem(int index, @NonNull String mediaId) {
        try {
            mBinder.addPlaylistItem(mControllerId, index, mediaId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call addPlaylistItem()");
        }
    }

    public void removePlaylistItem(int index) {
        try {
            mBinder.removePlaylistItem(mControllerId, index);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call removePlaylistItem()");
        }
    }

    public void replacePlaylistItem(int index, @NonNull String media) {
        try {
            mBinder.replacePlaylistItem(mControllerId, index, media);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call replacePlaylistItem()");
        }
    }

    public void skipToPreviousItem() {
        try {
            mBinder.skipToPreviousItem(mControllerId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call skipToPreviousPlaylistItem()");
        }
    }

    public void skipToNextItem() {
        try {
            mBinder.skipToNextItem(mControllerId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call skipToNextPlaylistItem()");
        }
    }

    public void skipToPlaylistItem(int index) {
        try {
            mBinder.skipToPlaylistItem(mControllerId, index);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call skipToPlaylistItem()");
        }
    }

    public void setShuffleMode(int shuffleMode) {
        try {
            mBinder.setShuffleMode(mControllerId, shuffleMode);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call setShuffleMode()");
        }
    }

    public void setRepeatMode(int repeatMode) {
        try {
            mBinder.setRepeatMode(mControllerId, repeatMode);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call setRepeatMode()");
        }
    }

    public void setVolumeTo(int value, int flags) {
        try {
            mBinder.setVolumeTo(mControllerId, value, flags);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call setVolumeTo()");
        }
    }

    public void adjustVolume(int direction, int flags) {
        try {
            mBinder.adjustVolume(mControllerId, direction, flags);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call adjustVolume()");
        }
    }

    public void sendCustomCommand(@NonNull SessionCommand2 command, @Nullable Bundle args) {
        try {
            mBinder.sendCustomCommand(mControllerId, command.toBundle(), args);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call sendCustomCommand()");
        }
    }

    public void fastForward() {
        try {
            mBinder.fastForward(mControllerId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call fastForward()");
        }
    }

    public void rewind() {
        try {
            mBinder.rewind(mControllerId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call rewind()");
        }
    }

    public void skipForward() {
        try {
            mBinder.skipForward(mControllerId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call skipForward()");
        }
    }

    public void skipBackward() {
        try {
            mBinder.skipBackward(mControllerId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call skipBackward()");
        }
    }

    public void playFromMediaId(@NonNull String mediaId, @Nullable Bundle extras) {
        try {
            mBinder.playFromMediaId(mControllerId, mediaId, extras);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call playFromMediaId()");
        }
    }

    public void playFromSearch(@NonNull String query, @Nullable Bundle extras) {
        try {
            mBinder.playFromSearch(mControllerId, query, extras);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call playFromSearch()");
        }
    }

    public void playFromUri(@NonNull Uri uri, @Nullable Bundle extras) {
        try {
            mBinder.playFromUri(mControllerId, uri, extras);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call playFromUri()");
        }
    }

    public void prepareFromMediaId(@NonNull String mediaId, @Nullable Bundle extras) {
        try {
            mBinder.prepareFromMediaId(mControllerId, mediaId, extras);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call prepareFromMediaId()");
        }
    }

    public void prepareFromSearch(@NonNull String query, @Nullable Bundle extras) {
        try {
            mBinder.prepareFromSearch(mControllerId, query, extras);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call prepareFromSearch()");
        }
    }

    public void prepareFromUri(@NonNull Uri uri, @Nullable Bundle extras) {
        try {
            mBinder.prepareFromUri(mControllerId, uri, extras);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call prepareFromUri()");
        }
    }

    public void setRating(@NonNull String mediaId, @NonNull Rating2 rating) {
        try {
            mBinder.setRating(mControllerId, mediaId, MediaUtils2.toParcelable(rating));
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call setRating()");
        }
    }

    public void subscribeRoutesInfo() {
        try {
            mBinder.subscribeRoutesInfo(mControllerId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call subscribeRoutesInfo()");
        }
    }

    public void unsubscribeRoutesInfo() {
        try {
            mBinder.unsubscribeRoutesInfo(mControllerId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call unsubscribeRoutesInfo()");
        }
    }

    public void selectRoute(@NonNull Bundle route) {
        try {
            mBinder.selectRoute(mControllerId, route);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call selectRoute()");
        }
    }

    public void close() {
        try {
            mBinder.close(mControllerId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call close()");
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Non-public methods
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Connects to client app's MediaController2ProviderService.
     * Should NOT be called main thread.
     *
     * @return true if connected successfully, false if failed to connect.
     */
    private boolean connect() {
        final Intent intent = new Intent(ACTION_MEDIA_CONTROLLER2);
        intent.setComponent(MEDIA_CONTROLLER2_PROVIDER_SERVICE);

        boolean bound = false;
        try {
            bound = mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to bind to the MediaController2ProviderService.");
        }

        if (bound) {
            try {
                mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Log.e(TAG, "InterruptedException while waiting for onServiceConnected.", ex);
            }
        }
        return mBinder != null;
    }

    /**
     * Disconnects from client app's MediaController2ProviderService.
     */
    private void disconnect() {
        if (mServiceConnection != null) {
            mContext.unbindService(mServiceConnection);
            mServiceConnection = null;
        }
    }

    /**
     * Create a {@link MediaController2} in the client app.
     * Should be used after successful connection through {@link #connect()}.
     *
     * @param waitForConnection true if this method needs to wait for the connection,
     *                          false otherwise.
     */
    void create(SessionToken2 token, boolean waitForConnection) {
        try {
            mBinder.create(false /* isBrowser */, mControllerId,
                    MediaUtils2.toParcelable(token), waitForConnection);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to create default controller with given token.");
        }
    }

    class MyServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Connected to client app's MediaController2ProviderService.");
            mBinder = IRemoteMediaController2.Stub.asInterface(service);
            mCountDownLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Disconnected from client app's MediaController2ProviderService.");
        }
    }
}
