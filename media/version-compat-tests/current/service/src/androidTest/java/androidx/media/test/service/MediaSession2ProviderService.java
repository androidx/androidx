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

import static androidx.media.test.lib.CommonConstants.ACTION_MEDIA_SESSION2;
import static androidx.media.test.lib.CommonConstants.INDEX_FOR_NULL_DSD;
import static androidx.media.test.lib.CommonConstants.INDEX_FOR_UNKONWN_DSD;
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
import static androidx.media.test.lib.MediaSession2Constants
        .TEST_CONTROLLER_CALLBACK_SESSION_REJECTS;
import static androidx.media.test.lib.MediaSession2Constants.TEST_GET_SESSION_ACTIVITY;
import static androidx.media.test.lib.MediaSession2Constants
        .TEST_ON_PLAYLIST_METADATA_CHANGED_SESSION_SET_PLAYLIST;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.mediacompat.testlib.IRemoteMediaSession2;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.AudioAttributesCompat;
import androidx.media.test.lib.MockActivity;
import androidx.media.test.lib.TestUtils.SyncHandler;
import androidx.media2.DataSourceDesc2;
import androidx.media2.FileDataSourceDesc2;
import androidx.media2.MediaItem2;
import androidx.media2.MediaMetadata2;
import androidx.media2.MediaPlayerConnector;
import androidx.media2.MediaPlaylistAgent;
import androidx.media2.MediaSession2;
import androidx.media2.MediaSession2.ControllerInfo;
import androidx.media2.SessionCommand2;
import androidx.media2.SessionCommandGroup2;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * A Service that creates {@link MediaSession2} and calls its methods according to the client app's
 * requests.
 */
public class MediaSession2ProviderService extends Service {
    private static final String TAG = "MediaSession2ProviderService";

    Map<String, MediaSession2> mSession2Map = new HashMap<>();
    RemoteMediaSession2Stub mSession2Binder;

    SyncHandler mHandler;
    Executor mExecutor;

    @Override
    public void onCreate() {
        super.onCreate();
        mSession2Binder = new RemoteMediaSession2Stub();
        mHandler = new SyncHandler(getMainLooper());
        mExecutor = new Executor() {
            @Override
            public void execute(Runnable command) {
                mHandler.post(command);
            }
        };
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (ACTION_MEDIA_SESSION2.equals(intent.getAction())) {
            return mSession2Binder;
        }
        return null;
    }

    @Override
    public void onDestroy() {
        for (MediaSession2 session2 : mSession2Map.values()) {
            session2.close();
        }
    }

    private class RemoteMediaSession2Stub extends IRemoteMediaSession2.Stub {
        @Override
        public void create(final String sessionId) throws RemoteException {
            final MediaSession2.Builder builder =
                    new MediaSession2.Builder(MediaSession2ProviderService.this)
                            .setId(sessionId)
                            .setPlayer(new MockPlayerConnector(0))
                            .setPlaylistAgent(new MockPlaylistAgent());

            switch (sessionId) {
                case TEST_GET_SESSION_ACTIVITY: {
                    final Intent sessionActivity = new Intent(MediaSession2ProviderService.this,
                            MockActivity.class);
                    PendingIntent pendingIntent = PendingIntent.getActivity(
                            MediaSession2ProviderService.this,
                            0 /* requestCode */,
                            sessionActivity, 0 /* flags */);
                    builder.setSessionActivity(pendingIntent);
                    break;
                }
                case TEST_CONTROLLER_CALLBACK_SESSION_REJECTS: {
                    builder.setSessionCallback(mExecutor, new MediaSession2.SessionCallback() {
                        @Override
                        public SessionCommandGroup2 onConnect(MediaSession2 session,
                                MediaSession2.ControllerInfo controller) {
                            return null;
                        }
                    });
                    break;
                }
                case TEST_ON_PLAYLIST_METADATA_CHANGED_SESSION_SET_PLAYLIST: {
                    builder.setSessionCallback(mExecutor, new MediaSession2.SessionCallback() {
                        @Override
                        public SessionCommandGroup2 onConnect(MediaSession2 session,
                                MediaSession2.ControllerInfo controller) {
                            SessionCommandGroup2 commands = new SessionCommandGroup2.Builder()
                                    .addCommand(new SessionCommand2(
                                            SessionCommand2
                                                    .COMMAND_CODE_PLAYLIST_GET_LIST_METADATA))
                                    .build();
                            return commands;
                        }
                    });
                    break;
                }
            }

            try {
                mHandler.postAndSync(new Runnable() {
                    @Override
                    public void run() {
                        MediaSession2 session2 = builder.build();
                        mSession2Map.put(sessionId, session2);
                    }
                });
            } catch (InterruptedException ex) {
                Log.e(TAG, "InterruptedException occurred while creating MediaSession2", ex);
            }
        }

        ////////////////////////////////////////////////////////////////////////////////
        // MediaSession2 methods
        ////////////////////////////////////////////////////////////////////////////////

        @Override
        public Bundle getToken(String sessionId) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            return session2.getToken().toBundle();
        }

        @Override
        public Bundle getCompatToken(String sessionId) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            return session2.getSessionCompat().getSessionToken().toBundle();
        }

        @Override
        public void updatePlayerConnector(String sessionId, @NonNull Bundle playerConfig,
                @Nullable Bundle agentConfig) throws RemoteException {
            playerConfig.setClassLoader(MediaSession2.class.getClassLoader());
            if (agentConfig != null) {
                agentConfig.setClassLoader(MediaSession2.class.getClassLoader());
            }
            MediaSession2 session2 = mSession2Map.get(sessionId);
            session2.updatePlayerConnector(
                    createMockPlayerConnector(playerConfig),
                    createMockPlaylistAgent(agentConfig));
        }

        private MediaPlayerConnector createMockPlayerConnector(Bundle playerConfig) {
            MediaPlayerConnector playerConnector;
            if (playerConfig.containsKey(KEY_VOLUME_CONTROL_TYPE)) {
                // Remote player
                playerConnector = new MockRemotePlayerConnector(
                        playerConfig.getInt(KEY_VOLUME_CONTROL_TYPE),
                        playerConfig.getInt(KEY_MAX_VOLUME),
                        playerConfig.getInt(KEY_CURRENT_VOLUME));
            } else {
                // Local player
                MockPlayerConnector localPlayer = new MockPlayerConnector(0);
                localPlayer.mLastPlayerState = playerConfig.getInt(KEY_PLAYER_STATE);
                localPlayer.mLastBufferingState = playerConfig.getInt(KEY_BUFFERING_STATE);
                localPlayer.mCurrentPosition = playerConfig.getLong(KEY_CURRENT_POSITION);
                localPlayer.mBufferedPosition = playerConfig.getLong(KEY_BUFFERED_POSITION);
                localPlayer.mPlaybackSpeed = playerConfig.getFloat(KEY_SPEED);
                playerConnector = localPlayer;
            }
            playerConnector.setAudioAttributes(
                    AudioAttributesCompat.fromBundle(
                            playerConfig.getBundle(KEY_AUDIO_ATTRIBUTES)));
            return playerConnector;
        }

        private MediaPlaylistAgent createMockPlaylistAgent(Bundle agentConfig) {
            if (agentConfig == null) {
                return null;
            }
            MockPlaylistAgent agent = new MockPlaylistAgent();
            agent.mPlaylist = MediaTestUtils.playlistFromParcelableList(
                    agentConfig.getParcelableArrayList(KEY_PLAYLIST), false /* createDsd */);
            agent.mCurrentMediaItem = MediaItem2.fromBundle(agentConfig.getBundle(KEY_MEDIA_ITEM));
            agent.mMetadata = MediaMetadata2.fromBundle(agentConfig.getBundle(KEY_METADATA));
            return agent;
        }

        @Override
        public void sendCustomCommand(String sessionId, Bundle command, Bundle args)
                throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            session2.sendCustomCommand(SessionCommand2.fromBundle(command), args);
        }

        @Override
        public void sendCustomCommand2(String sessionId, Bundle controller, Bundle command,
                Bundle args, ResultReceiver receiver) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            ControllerInfo info = MediaTestUtils.getTestControllerInfo(session2);
            session2.sendCustomCommand(info, SessionCommand2.fromBundle(command), args, receiver);
        }

        @Override
        public void close(String sessionId) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            session2.close();
        }

        @Override
        public void notifyError(String sessionId, int errorCode, Bundle extras)
                throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            session2.notifyError(errorCode, extras);
        }

        @Override
        public void setAllowedCommands(String sessionId, Bundle controller, Bundle commands)
                throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            ControllerInfo info = MediaTestUtils.getTestControllerInfo(session2);
            session2.setAllowedCommands(info, SessionCommandGroup2.fromBundle(commands));
        }

        @Override
        public void notifyRoutesInfoChanged(String sessionId, Bundle controller,
                List<Bundle> routes) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            ControllerInfo info = MediaTestUtils.getTestControllerInfo(session2);
            session2.notifyRoutesInfoChanged(info, routes);
        }

        @Override
        public void setCustomLayout(String sessionId, Bundle controller, List<Bundle> layout)
                throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            ControllerInfo info = MediaTestUtils.getTestControllerInfo(session2);
            session2.setCustomLayout(info, MediaTestUtils.buttonListFromBundleList(layout));
        }

        ////////////////////////////////////////////////////////////////////////////////
        // MockPlayerConnector methods
        ////////////////////////////////////////////////////////////////////////////////

        @Override
        public void setPlayerState(String sessionId, int state) {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayerConnector player = (MockPlayerConnector) session2.getPlayerConnector();
            player.mLastPlayerState = state;
        }

        @Override
        public void setCurrentPosition(String sessionId, long pos) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayerConnector player = (MockPlayerConnector) session2.getPlayerConnector();
            player.mCurrentPosition = pos;
        }

        @Override
        public void setBufferedPosition(String sessionId, long pos) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayerConnector player = (MockPlayerConnector) session2.getPlayerConnector();
            player.mBufferedPosition = pos;
        }

        @Override
        public void setDuration(String sessionId, long duration) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayerConnector player = (MockPlayerConnector) session2.getPlayerConnector();
            player.mDuration = duration;
        }

        @Override
        public void setPlaybackSpeed(String sessionId, float speed) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayerConnector player = (MockPlayerConnector) session2.getPlayerConnector();
            player.mPlaybackSpeed = speed;
        }

        @Override
        public void notifySeekCompleted(String sessionId, long pos) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayerConnector player = (MockPlayerConnector) session2.getPlayerConnector();
            player.notifySeekCompleted(pos);
        }

        @Override
        public void notifyBufferingStateChanged(String sessionId, int itemIndex, int buffState)
                throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlaylistAgent agent = (MockPlaylistAgent) session2.getPlaylistAgent();
            MockPlayerConnector player = (MockPlayerConnector) session2.getPlayerConnector();
            player.notifyBufferingStateChanged(
                    agent.getPlaylist().get(itemIndex).getDataSourceDesc(), buffState);
        }

        @Override
        public void notifyPlayerStateChanged(String sessionId, int state) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayerConnector player = (MockPlayerConnector) session2.getPlayerConnector();
            player.notifyPlayerStateChanged(state);
        }

        @Override
        public void notifyPlaybackSpeedChanged(String sessionId, float speed)
                throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayerConnector player = (MockPlayerConnector) session2.getPlayerConnector();
            player.notifyPlaybackSpeedChanged(speed);
        }

        @Override
        public void notifyCurrentDataSourceChanged(String sessionId, int index)
                throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayerConnector player = (MockPlayerConnector) session2.getPlayerConnector();
            MockPlaylistAgent agent = (MockPlaylistAgent) session2.getPlaylistAgent();
            switch (index) {
                case INDEX_FOR_UNKONWN_DSD:
                    player.notifyCurrentDataSourceChanged(
                            new FileDataSourceDesc2.Builder(new FileDescriptor()).build());
                    break;
                case INDEX_FOR_NULL_DSD:
                    player.notifyCurrentDataSourceChanged(null);
                    break;
                default:
                    player.notifyCurrentDataSourceChanged(
                            agent.getPlaylist().get(index).getDataSourceDesc());
                    break;
            }
        }

        @Override
        public void notifyMediaPrepared(String sessionId, int index) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayerConnector player = (MockPlayerConnector) session2.getPlayerConnector();
            MockPlaylistAgent agent = (MockPlaylistAgent) session2.getPlaylistAgent();
            player.notifyMediaPrepared(agent.getPlaylist().get(index).getDataSourceDesc());
        }


        ////////////////////////////////////////////////////////////////////////////////
        // MockPlaylistAgent methods
        ////////////////////////////////////////////////////////////////////////////////

        @Override
        public void setPlaylist(String sessionId, List<Bundle> playlist)
                throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlaylistAgent agent = (MockPlaylistAgent) session2.getPlaylistAgent();

            List<MediaItem2> list = new ArrayList<>();
            for (Bundle bundle : playlist) {
                list.add(MediaItem2.fromBundle(bundle));
            }
            agent.mPlaylist = list;
        }

        @Override
        public void setPlaylistWithDummyDsd(String sessionId, List<Bundle> playlist)
                throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlaylistAgent agent = (MockPlaylistAgent) session2.getPlaylistAgent();

            List<MediaItem2> list = new ArrayList<>();
            for (Bundle bundle : playlist) {
                MediaItem2 item = MediaItem2.fromBundle(bundle);
                list.add(new MediaItem2.Builder(item.getFlags())
                        .setMediaId(item.getMediaId())
                        .setMetadata(item.getMetadata())
                        .setDataSourceDesc(createNewDsd())
                        .build());
            }
            agent.mPlaylist = list;
        }

        @Override
        public void setPlaylistMetadata(String sessionId, Bundle metadata)
                throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlaylistAgent agent = (MockPlaylistAgent) session2.getPlaylistAgent();
            agent.mMetadata = MediaMetadata2.fromBundle(metadata);
        }

        @Override
        public void setShuffleMode(String sessionId, int shuffleMode)
                throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlaylistAgent agent = (MockPlaylistAgent) session2.getPlaylistAgent();
            agent.mShuffleMode = shuffleMode;
        }

        @Override
        public void setRepeatMode(String sessionId, int repeatMode) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlaylistAgent agent = (MockPlaylistAgent) session2.getPlaylistAgent();
            agent.mRepeatMode = repeatMode;
        }

        @Override
        public void setCurrentMediaItem(String sessionId, int index)
                throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlaylistAgent agent = (MockPlaylistAgent) session2.getPlaylistAgent();
            agent.mCurrentMediaItem = agent.mPlaylist.get(index);
        }

        @Override
        public void notifyPlaylistChanged(String sessionId) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlaylistAgent agent = (MockPlaylistAgent) session2.getPlaylistAgent();
            agent.callNotifyPlaylistChanged();
        }

        @Override
        public void notifyPlaylistMetadataChanged(String sessionId) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlaylistAgent agent = (MockPlaylistAgent) session2.getPlaylistAgent();
            agent.callNotifyPlaylistMetadataChanged();
        }

        @Override
        public void notifyShuffleModeChanged(String sessionId) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlaylistAgent agent = (MockPlaylistAgent) session2.getPlaylistAgent();
            agent.callNotifyShuffleModeChanged();
        }

        @Override
        public void notifyRepeatModeChanged(String sessionId) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlaylistAgent agent = (MockPlaylistAgent) session2.getPlaylistAgent();
            agent.callNotifyRepeatModeChanged();
        }
    }

    private DataSourceDesc2 createNewDsd() {
        return new FileDataSourceDesc2.Builder(new FileDescriptor()).build();
    }
}
