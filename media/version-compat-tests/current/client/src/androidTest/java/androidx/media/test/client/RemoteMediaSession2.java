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
import static androidx.media.test.lib.CommonConstants.REMOTE_MEDIA_SESSION2_SERVICE;
import static androidx.media.test.lib.TestUtils.WAIT_TIME_MS;

import static junit.framework.TestCase.fail;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.mediacompat.testlib.IRemoteMediaSession2;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.test.lib.MediaSession2Constants;
import androidx.media2.MediaItem2;
import androidx.media2.MediaMetadata2;
import androidx.media2.MediaSession2;
import androidx.media2.MediaSession2.CommandButton;
import androidx.media2.MediaSession2.ControllerInfo;
import androidx.media2.SessionCommand2;
import androidx.media2.SessionCommandGroup2;
import androidx.media2.SessionToken2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Represents remote {@link MediaSession2} in the service app's RemoteMediaSession2Service.
 * Users can run {@link MediaSession2} methods remotely with this object.
 */
public class RemoteMediaSession2 {
    private static final String TAG = "RemoteMediaSession2";

    private final Context mContext;
    private final String mSessionId;

    private ServiceConnection mServiceConnection;
    private IRemoteMediaSession2 mBinder;
    private RemoteMockPlayer mRemotePlayer;
    private RemoteMockPlaylistAgent mRemotePlaylistAgent;
    private CountDownLatch mCountDownLatch;

    /**
     * Create a {@link MediaSession2} in the service app.
     * Should NOT be called in main thread.
     */
    public RemoteMediaSession2(@NonNull String sessionId, Context context) {
        mSessionId = sessionId;
        mContext = context;
        mCountDownLatch = new CountDownLatch(1);
        mServiceConnection = new MyServiceConnection();

        if (!connect()) {
            fail("Failed to connect to the RemoteMediaSession2Service.");
        }
        create();
    }

    public void cleanUp() {
        close();
        disconnect();
    }

    /**
     * Run a test-specific custom command in the service app.
     *
     * @param command Pre-defined command code.
     *                One of the constants in {@link MediaSession2Constants}.
     * @param args A {@link Bundle} which contains pre-defined arguments.
     */
    public void runCustomTestCommands(int command, @Nullable Bundle args) {
        try {
            mBinder.runCustomTestCommands(mSessionId, command, args);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call runCustomTestCommands(). command=" + command
                    + " , args=" + args);
        }
    }

    /**
     * Gets {@link RemoteMockPlayer} for interact with the remote MockPlayer.
     * Users can run MockPlayer methods remotely with this object.
     */
    public RemoteMockPlayer getMockPlayer() {
        return mRemotePlayer;
    }

    /**
     * Gets {@link RemoteMockPlaylistAgent} for interact with the remote MockPlaylistAgent.
     * Users can run MockPlaylistAgent methods remotely with this object.
     */
    public RemoteMockPlaylistAgent getMockPlaylistAgent() {
        return mRemotePlaylistAgent;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // MediaSession2 methods
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Gets {@link SessionToken2} from the service app.
     * Should be used after the creation of the session through {@link #create()}.
     *
     * @return A {@link SessionToken2} object if succeeded, {@code null} if failed.
     */
    public SessionToken2 getToken() {
        SessionToken2 token = null;
        try {
            Bundle bundle = mBinder.getToken(mSessionId);
            if (bundle != null) {
                bundle.setClassLoader(MediaSession2.class.getClassLoader());
            }
            token = SessionToken2.fromBundle(bundle);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to get session token. sessionId=" + mSessionId);
        }
        return token;
    }

    public void sendCustomCommand(@NonNull SessionCommand2 command, @Nullable Bundle args) {
        try {
            mBinder.sendCustomCommand(mSessionId, command.toBundle(), args);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call sendCustomCommand()");
        }
    }

    public void sendCustomCommand(@NonNull ControllerInfo controller,
            @NonNull SessionCommand2 command, @Nullable Bundle args,
            @Nullable ResultReceiver receiver) {
        try {
            // TODO: ControllerInfo should be handled.
            mBinder.sendCustomCommand2(mSessionId, null, command.toBundle(), args, receiver);
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

    public void notifyError(@MediaSession2.ErrorCode int errorCode, @Nullable Bundle extras) {
        try {
            mBinder.notifyError(mSessionId, errorCode, extras);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call notifyError()");
        }
    }

    public void setAllowedCommands(@NonNull ControllerInfo controller,
            @NonNull SessionCommandGroup2 commands) {
        try {
            // TODO: ControllerInfo should be handled.
            mBinder.setAllowedCommands(mSessionId, null, commands.toBundle());
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call setAllowedCommands()");
        }
    }

    public void notifyRoutesInfoChanged(@NonNull ControllerInfo controller,
            @Nullable List<Bundle> routes) {
        try {
            // TODO: ControllerInfo should be handled.
            mBinder.notifyRoutesInfoChanged(mSessionId, null, routes);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call notifyRoutesInfoChanged()");
        }
    }

    public void setCustomLayout(@NonNull ControllerInfo controller,
            @NonNull List<CommandButton> layout) {
        try {
            List<Bundle> bundleList = new ArrayList<>();
            for (CommandButton btn : layout) {
                bundleList.add(btn.toBundle());
            }
            // TODO: ControllerInfo should be handled.
            mBinder.setCustomLayout(mSessionId, null, bundleList);
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

        public void notifyCurrentDataSourceChanged(int index) {
            try {
                mBinder.notifyCurrentDataSourceChanged(mSessionId, index);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call notifyCurrentDataSourceChanged()");
            }
        }

        public void notifyMediaPrepared(int index) {
            try {
                mBinder.notifyMediaPrepared(mSessionId, index);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call notifyMediaPrepared()");
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // MockPlaylistAgent methods
    ////////////////////////////////////////////////////////////////////////////////

    public class RemoteMockPlaylistAgent {

        public void setPlaylist(List<MediaItem2> playlist) {
            try {
                mBinder.setPlaylist(
                        mSessionId, MediaTestUtils.mediaItem2ListToBundleList(playlist));
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call setPlaylist()");
            }
        }

        public void setPlaylistNewDsd(List<MediaItem2> playlist) {
            try {
                mBinder.setPlaylistWithNewDsd(
                        mSessionId, MediaTestUtils.mediaItem2ListToBundleList(playlist));
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call setPlaylistNewDsd()");
            }
        }

        public void setPlaylistMetadata(MediaMetadata2 metadata) {
            try {
                mBinder.setPlaylistMetadata(mSessionId, metadata.toBundle());
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call setPlaylistMetadata()");
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
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Non-public methods
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Connects to service app's RemoteMediaSession2Service.
     * Should NOT be called in main thread.
     *
     * @return true if connected successfully, false if failed to connect.
     */
    private boolean connect() {
        final Intent intent = new Intent(ACTION_MEDIA_SESSION2);
        intent.setComponent(REMOTE_MEDIA_SESSION2_SERVICE);

        boolean bound = false;
        try {
            bound = mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception ex) {
            Log.e(TAG, "Failed binding to the RemoteMediaSession2Service of the service app");
        }

        if (bound) {
            try {
                mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Log.e(TAG, "InterruptedException while waiting for onServiceConnected.", ex);
            }
        }
        return mBinder != null;
    }

    /**
     * Disconnects from service app's RemoteMediaSession2Service.
     */
    private void disconnect() {
        if (mServiceConnection != null) {
            mContext.unbindService(mServiceConnection);
        }
        mServiceConnection = null;
    }

    /**
     * Create a {@link MediaSession2} in the service app.
     * Should be used after successful connection through {@link #connect}.
     */
    private void create() {
        try {
            mBinder.create(mSessionId);
            mRemotePlayer = new RemoteMockPlayer();
            mRemotePlaylistAgent = new RemoteMockPlaylistAgent();
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to get session token. sessionId=" + mSessionId);
        }
    }

    class MyServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Connected to service app's RemoteMediaSession2Service.");
            mBinder = IRemoteMediaSession2.Stub.asInterface(service);
            mCountDownLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Disconnected from the service.");
        }
    }
}
