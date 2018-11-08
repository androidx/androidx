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

package androidx.media.test.client;

import static androidx.media.test.lib.CommonConstants.ACTION_MEDIA_SESSION2;
import static androidx.media.test.lib.CommonConstants.KEY_AUDIO_ATTRIBUTES;
import static androidx.media.test.lib.CommonConstants.KEY_BUFFERED_POSITION;
import static androidx.media.test.lib.CommonConstants.KEY_BUFFERING_STATE;
import static androidx.media.test.lib.CommonConstants.KEY_CURRENT_POSITION;
import static androidx.media.test.lib.CommonConstants.KEY_CURRENT_VOLUME;
import static androidx.media.test.lib.CommonConstants.KEY_MAX_VOLUME;
import static androidx.media.test.lib.CommonConstants.KEY_MEDIA_ITEM;
import static androidx.media.test.lib.CommonConstants.KEY_METADATA;
import static androidx.media.test.lib.CommonConstants.KEY_PLAYER_STATE;
import static androidx.media.test.lib.CommonConstants.KEY_PLAYLIST;
import static androidx.media.test.lib.CommonConstants.KEY_SPEED;
import static androidx.media.test.lib.CommonConstants.KEY_VOLUME_CONTROL_TYPE;
import static androidx.media.test.lib.CommonConstants.MEDIA_SESSION2_PROVIDER_SERVICE;
import static androidx.media.test.lib.TestUtils.TIMEOUT_MS;

import static junit.framework.TestCase.fail;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.mediacompat.testlib.IRemoteMediaSession;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.AudioAttributesCompat;
import androidx.media.test.lib.TestUtils;
import androidx.media2.MediaItem;
import androidx.media2.MediaMetadata;
import androidx.media2.MediaSession;
import androidx.media2.MediaSession.CommandButton;
import androidx.media2.MediaSession.ControllerInfo;
import androidx.media2.MediaUtils;
import androidx.media2.ParcelImplListSlice;
import androidx.media2.SessionCommand;
import androidx.media2.SessionCommandGroup;
import androidx.media2.SessionPlayer;
import androidx.media2.SessionToken;
import androidx.versionedparcelable.ParcelImpl;
import androidx.versionedparcelable.ParcelUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Represents remote {@link MediaSession} in the service app's MediaSessionProviderService.
 * Users can run {@link MediaSession} methods remotely with this object.
 */
public class RemoteMediaSession {
    private static final String TAG = "RemoteMediaSession";

    private final Context mContext;
    private final String mSessionId;

    private ServiceConnection mServiceConnection;
    private IRemoteMediaSession mBinder;
    private RemoteMockPlayer mRemotePlayer;
    private CountDownLatch mCountDownLatch;

    /**
     * Create a {@link MediaSession} in the service app.
     * Should NOT be called in main thread.
     */
    public RemoteMediaSession(@NonNull String sessionId, Context context) {
        mSessionId = sessionId;
        mContext = context;
        mCountDownLatch = new CountDownLatch(1);
        mServiceConnection = new MyServiceConnection();

        if (!connect()) {
            fail("Failed to connect to the MediaSessionProviderService.");
        }
        create();
    }

    public void cleanUp() {
        close();
        disconnect();
    }

    /**
     * Gets {@link RemoteMockPlayer} for interact with the remote MockPlayer.
     * Users can run MockPlayer methods remotely with this object.
     */
    public RemoteMockPlayer getMockPlayer() {
        return mRemotePlayer;
    }

    /**
     * Create a {@link Bundle} which represents a configuration of local
     * {@link SessionPlayer} in order to create a new mock player in the service app.
     * <p>
     * The returned value can be used in {@link #updatePlayer(Bundle)}.
     */
    public static Bundle createMockPlayerConnectorConfig(
            int state, int buffState, long pos, long buffPos, float speed,
            @Nullable AudioAttributesCompat attr) {
        Bundle playerBundle = new Bundle();
        playerBundle.putInt(KEY_PLAYER_STATE, state);
        playerBundle.putInt(KEY_BUFFERING_STATE, buffState);
        playerBundle.putLong(KEY_CURRENT_POSITION, pos);
        playerBundle.putLong(KEY_BUFFERED_POSITION, buffPos);
        playerBundle.putFloat(KEY_SPEED, speed);
        if (attr != null) {
            playerBundle.putBundle(KEY_AUDIO_ATTRIBUTES, attr.toBundle());
        }
        return playerBundle;
    }

    /**
     * Create a {@link Bundle} which represents a configuration of remote
     * {@link SessionPlayer} in order to create a new mock player in the service app.
     * <p>
     * The returned value can be used in {@link #updatePlayer(Bundle)}.
     */
    public static Bundle createMockPlayerConnectorConfig(
            int volumeControlType, int maxVolume, int currentVolume,
            @Nullable AudioAttributesCompat attr) {
        Bundle playerBundle = new Bundle();
        playerBundle.putInt(KEY_VOLUME_CONTROL_TYPE, volumeControlType);
        playerBundle.putInt(KEY_MAX_VOLUME, maxVolume);
        playerBundle.putInt(KEY_CURRENT_VOLUME, currentVolume);
        if (attr != null) {
            playerBundle.putBundle(KEY_AUDIO_ATTRIBUTES, attr.toBundle());
        }
        return playerBundle;
    }

    /**
     * Create a {@link Bundle} which represents a configuration of {@link SessionPlayer}
     * in order to create a new mock playlist agent in the service app.
     * <p>
     * The returned value can be used in {@link #updatePlayer(Bundle)}.
     */
    public static Bundle createMockPlayerConnectorConfig(
            int state, int buffState, long pos, long buffPos, float speed,
            @Nullable AudioAttributesCompat attr, @Nullable List<MediaItem> playlist,
            @Nullable MediaItem currentItem, @Nullable MediaMetadata metadata) {
        Bundle bundle = createMockPlayerConnectorConfig(state, buffState, pos, buffPos, speed,
                attr);
        if (playlist != null) {
            ParcelImplListSlice listSlice = new ParcelImplListSlice(
                    MediaTestUtils.convertToParcelImplList(playlist));
            bundle.putParcelable(KEY_PLAYLIST, listSlice);
        }
        if (currentItem != null) {
            bundle.putParcelable(KEY_MEDIA_ITEM, MediaUtils.toParcelable(currentItem));
        }
        if (metadata != null) {
            ParcelUtils.putVersionedParcelable(bundle, KEY_METADATA, metadata);
        }
        return bundle;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // MediaSession methods
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Gets {@link SessionToken} from the service app.
     * Should be used after the creation of the session through {@link #create()}.
     *
     * @return A {@link SessionToken} object if succeeded, {@code null} if failed.
     */
    public SessionToken getToken() {
        SessionToken token = null;
        try {
            token = MediaUtils.fromParcelable(mBinder.getToken(mSessionId));
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to get session token. sessionId=" + mSessionId);
        }
        return token;
    }

    /**
     * Gets {@link MediaSessionCompat.Token} from the service app.
     * Should be used after the creation of the session through {@link #create()}.
     *
     * @return A {@link SessionToken} object if succeeded, {@code null} if failed.
     */
    public MediaSessionCompat.Token getCompatToken() {
        MediaSessionCompat.Token token = null;
        try {
            Bundle bundle = mBinder.getCompatToken(mSessionId);
            if (bundle != null) {
                bundle.setClassLoader(MediaSession.class.getClassLoader());
            }
            token = MediaSessionCompat.Token.fromBundle(bundle);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to get session compat token. sessionId=" + mSessionId);
        }
        return token;
    }

    public void updatePlayer(@NonNull Bundle config) {
        try {
            mBinder.updatePlayer(mSessionId, config);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call updatePlayerConnector()");
        }
    }

    public void broadcastCustomCommand(@NonNull SessionCommand command, @Nullable Bundle args) {
        try {
            mBinder.broadcastCustomCommand(mSessionId, MediaUtils.toParcelable(command), args);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call broadcastCustomCommand()");
        }
    }

    public void sendCustomCommand(@NonNull ControllerInfo controller,
            @NonNull SessionCommand command, @Nullable Bundle args) {
        try {
            // TODO: ControllerInfo should be handled.
            mBinder.sendCustomCommand(mSessionId, null, MediaUtils.toParcelable(command), args);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call sendCustomCommand2()");
        }
    }

    public void close() {
        try {
            mBinder.close(mSessionId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call close()");
        }
    }

    public void setAllowedCommands(@NonNull ControllerInfo controller,
            @NonNull SessionCommandGroup commands) {
        try {
            // TODO: ControllerInfo should be handled.
            mBinder.setAllowedCommands(mSessionId, null,
                    MediaUtils.toParcelable(commands));
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call setAllowedCommands()");
        }
    }

    public void setCustomLayout(@NonNull ControllerInfo controller,
            @NonNull List<CommandButton> layout) {
        try {
            List<ParcelImpl> parcelList = new ArrayList<>();
            for (CommandButton btn : layout) {
                parcelList.add((ParcelImpl) ParcelUtils.toParcelable(btn));
            }
            // TODO: ControllerInfo should be handled.
            mBinder.setCustomLayout(mSessionId, null, parcelList);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call setCustomLayout()");
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // RemoteMockPlayer methods
    ////////////////////////////////////////////////////////////////////////////////

    public class RemoteMockPlayer {

        public void setPlayerState(int state) {
            try {
                mBinder.setPlayerState(mSessionId, state);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call setCurrentPosition()");
            }
        }

        public void setCurrentPosition(long pos) {
            try {
                mBinder.setCurrentPosition(mSessionId, pos);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call setCurrentPosition()");
            }
        }

        public void setBufferedPosition(long pos) {
            try {
                mBinder.setBufferedPosition(mSessionId, pos);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call setBufferedPosition()");
            }
        }

        public void setDuration(long duration) {
            try {
                mBinder.setDuration(mSessionId, duration);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call setDuration()");
            }
        }

        public void setPlaybackSpeed(float speed) {
            try {
                mBinder.setPlaybackSpeed(mSessionId, speed);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call setPlaybackSpeed()");
            }
        }

        public void notifySeekCompleted(long pos) {
            try {
                mBinder.notifySeekCompleted(mSessionId, pos);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call notifySeekCompleted()");
            }
        }

        public void notifyBufferingStateChanged(int itemIndex, int buffState) {
            try {
                mBinder.notifyBufferingStateChanged(mSessionId, itemIndex, buffState);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call notifyBufferingStateChanged()");
            }
        }

        public void notifyPlayerStateChanged(int state) {
            try {
                mBinder.notifyPlayerStateChanged(mSessionId, state);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call notifyPlayerStateChanged()");
            }
        }

        public void notifyPlaybackSpeedChanged(float speed) {
            try {
                mBinder.notifyPlaybackSpeedChanged(mSessionId, speed);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call notifyPlaybackSpeedChanged()");
            }
        }

        public void notifyCurrentMediaItemChanged(int index) {
            try {
                mBinder.notifyCurrentMediaItemChanged(mSessionId, index);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call notifyCurrentMediaItemChanged()");
            }
        }

        public void notifyAudioAttributesChanged(AudioAttributesCompat attrs) {
            try {
                mBinder.notifyAudioAttributesChanged(mSessionId, attrs.toBundle());
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call notifyAudioAttributesChanged()");
            }
        }

        public void setPlaylist(List<MediaItem> playlist) {
            try {
                mBinder.setPlaylist(
                        mSessionId, MediaTestUtils.convertToParcelImplList(playlist));
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call setPlaylist()");
            }
        }

        /**
         * Service app will automatically create a playlist of size {@param size},
         * and sets the list to the player.
         *
         * Each item's media ID will be {@link TestUtils#getMediaIdInDummyList(int)}.
         */
        public void createAndSetDummyPlaylist(int size) {
            try {
                mBinder.createAndSetDummyPlaylist(mSessionId, size);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call createAndSetDummyPlaylist()");
            }
        }

        public void setPlaylistWithDummyItem(List<MediaItem> playlist) {
            try {
                mBinder.setPlaylistWithDummyItem(
                        mSessionId, MediaTestUtils.convertToParcelImplList(playlist));
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call setPlaylistWithDummyItem()");
            }
        }

        public void setPlaylistMetadata(MediaMetadata metadata) {
            try {
                mBinder.setPlaylistMetadata(mSessionId, MediaUtils.toParcelable(metadata));
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call setPlaylistMetadata()");
            }
        }

        public void setPlaylistMetadataWithLargeBitmaps(int count, int width, int height) {
            try {
                mBinder.setPlaylistMetadataWithLargeBitmaps(mSessionId, count, width, height);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call setPlaylistMetadataWithLargeBitmaps()");
            }
        }

        public void setRepeatMode(int repeatMode) {
            try {
                mBinder.setRepeatMode(mSessionId, repeatMode);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call setRepeatMode()");
            }
        }

        public void setShuffleMode(int shuffleMode) {
            try {
                mBinder.setShuffleMode(mSessionId, shuffleMode);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call setShuffleMode()");
            }
        }

        public void setCurrentMediaItem(int index) {
            try {
                mBinder.setCurrentMediaItem(mSessionId, index);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call setCurrentMediaItem()");
            }
        }

        public void notifyPlaylistChanged() {
            try {
                mBinder.notifyPlaylistChanged(mSessionId);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call notifyPlaylistChanged()");
            }
        }

        public void notifyPlaylistMetadataChanged() {
            try {
                mBinder.notifyPlaylistMetadataChanged(mSessionId);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call notifyPlaylistMetadataChanged()");
            }
        }

        public void notifyShuffleModeChanged() {
            try {
                mBinder.notifyShuffleModeChanged(mSessionId);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call notifyShuffleModeChanged()");
            }
        }

        public void notifyRepeatModeChanged() {
            try {
                mBinder.notifyRepeatModeChanged(mSessionId);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call notifyRepeatModeChanged()");
            }
        }

        public void notifyPlaybackCompleted() {
            try {
                mBinder.notifyPlaybackCompleted(mSessionId);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call notifyRepeatModeChanged()");
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Non-public methods
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Connects to service app's MediaSessionProviderService.
     * Should NOT be called in main thread.
     *
     * @return true if connected successfully, false if failed to connect.
     */
    private boolean connect() {
        final Intent intent = new Intent(ACTION_MEDIA_SESSION2);
        intent.setComponent(MEDIA_SESSION2_PROVIDER_SERVICE);

        boolean bound = false;
        try {
            bound = mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception ex) {
            Log.e(TAG, "Failed binding to the MediaSessionProviderService of the service app");
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
     * Disconnects from service app's MediaSessionProviderService.
     */
    private void disconnect() {
        if (mServiceConnection != null) {
            mContext.unbindService(mServiceConnection);
        }
        mServiceConnection = null;
    }

    /**
     * Create a {@link MediaSession} in the service app.
     * Should be used after successful connection through {@link #connect}.
     */
    private void create() {
        try {
            mBinder.create(mSessionId);
            mRemotePlayer = new RemoteMockPlayer();
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to get session token. sessionId=" + mSessionId);
        }
    }

    class MyServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Connected to service app's MediaSessionProviderService.");
            mBinder = IRemoteMediaSession.Stub.asInterface(service);
            mCountDownLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Disconnected from the service.");
        }
    }
}
