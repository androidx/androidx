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
import static androidx.media.test.lib.CommonConstants.INDEX_FOR_NULL_ITEM;
import static androidx.media.test.lib.CommonConstants.INDEX_FOR_UNKONWN_ITEM;
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
import android.support.mediacompat.testlib.IRemoteMediaSession2;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media.AudioAttributesCompat;
import androidx.media.test.lib.MockActivity;
import androidx.media.test.lib.TestUtils;
import androidx.media.test.lib.TestUtils.SyncHandler;
import androidx.media2.FileMediaItem2;
import androidx.media2.MediaItem2;
import androidx.media2.MediaMetadata2;
import androidx.media2.MediaSession2;
import androidx.media2.MediaSession2.ControllerInfo;
import androidx.media2.SessionCommand2;
import androidx.media2.SessionCommandGroup2;
import androidx.media2.SessionPlayer2;
import androidx.versionedparcelable.ParcelImpl;
import androidx.versionedparcelable.ParcelUtils;

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
                    new MediaSession2.Builder(MediaSession2ProviderService.this, new MockPlayer(0))
                            .setId(sessionId);

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
                                                    .COMMAND_CODE_PLAYER_GET_PLAYLIST_METADATA))
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
        public ParcelImpl getToken(String sessionId) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            return session2 != null
                    ? (ParcelImpl) ParcelUtils.toParcelable(session2.getToken()) : null;
        }

        @Override
        public Bundle getCompatToken(String sessionId) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            return session2.getSessionCompat().getSessionToken().toBundle();
        }

        @Override
        public void updatePlayer(String sessionId, @NonNull Bundle config) throws RemoteException {
            config.setClassLoader(MediaSession2.class.getClassLoader());
            if (config != null) {
                config.setClassLoader(MediaSession2.class.getClassLoader());
            }
            MediaSession2 session2 = mSession2Map.get(sessionId);
            session2.updatePlayer(createMockPlayer(config));
        }

        private SessionPlayer2 createMockPlayer(Bundle config) {
            SessionPlayer2 player;
            if (config.containsKey(KEY_VOLUME_CONTROL_TYPE)) {
                // Remote player
                player = new MockRemotePlayer(
                        config.getInt(KEY_VOLUME_CONTROL_TYPE),
                        config.getInt(KEY_MAX_VOLUME),
                        config.getInt(KEY_CURRENT_VOLUME));
            } else {
                // Local player
                MockPlayer localPlayer = new MockPlayer(0);
                localPlayer.mLastPlayerState = config.getInt(KEY_PLAYER_STATE);
                localPlayer.mLastBufferingState = config.getInt(KEY_BUFFERING_STATE);
                localPlayer.mCurrentPosition = config.getLong(KEY_CURRENT_POSITION);
                localPlayer.mBufferedPosition = config.getLong(KEY_BUFFERED_POSITION);
                localPlayer.mPlaybackSpeed = config.getFloat(KEY_SPEED);

                localPlayer.mPlaylist = MediaTestUtils.playlistFromParcelableList(
                        config.getParcelableArrayList(KEY_PLAYLIST), false /* createItem */);
                localPlayer.mCurrentMediaItem = MediaItem2.fromBundle(
                        config.getBundle(KEY_MEDIA_ITEM));
                localPlayer.mMetadata = ParcelUtils.getVersionedParcelable(config, KEY_METADATA);
                player = localPlayer;
            }
            player.setAudioAttributes(
                    AudioAttributesCompat.fromBundle(
                            config.getBundle(KEY_AUDIO_ATTRIBUTES)));
            return player;
        }

        @Override
        public void broadcastCustomCommand(String sessionId, Bundle command, Bundle args)
                throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            session2.broadcastCustomCommand(SessionCommand2.fromBundle(command), args);
        }

        @Override
        public void sendCustomCommand(String sessionId, Bundle controller, Bundle command,
                Bundle args) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            ControllerInfo info = MediaTestUtils.getTestControllerInfo(session2);
            session2.sendCustomCommand(info, SessionCommand2.fromBundle(command), args);
        }

        @Override
        public void close(String sessionId) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            session2.close();
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
        // MockPlayer methods
        ////////////////////////////////////////////////////////////////////////////////

        @Override
        public void setPlayerState(String sessionId, int state) {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayer player = (MockPlayer) session2.getPlayer();
            player.mLastPlayerState = state;
        }

        @Override
        public void setCurrentPosition(String sessionId, long pos) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayer player = (MockPlayer) session2.getPlayer();
            player.mCurrentPosition = pos;
        }

        @Override
        public void setBufferedPosition(String sessionId, long pos) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayer player = (MockPlayer) session2.getPlayer();
            player.mBufferedPosition = pos;
        }

        @Override
        public void setDuration(String sessionId, long duration) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayer player = (MockPlayer) session2.getPlayer();
            player.mDuration = duration;
        }

        @Override
        public void setPlaybackSpeed(String sessionId, float speed) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayer player = (MockPlayer) session2.getPlayer();
            player.mPlaybackSpeed = speed;
        }

        @Override
        public void notifySeekCompleted(String sessionId, long pos) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayer player = (MockPlayer) session2.getPlayer();
            player.notifySeekCompleted(pos);
        }

        @Override
        public void notifyBufferingStateChanged(String sessionId, int itemIndex, int buffState)
                throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayer player = (MockPlayer) session2.getPlayer();
            player.notifyBufferingStateChanged(
                    player.getPlaylist().get(itemIndex), buffState);
        }

        @Override
        public void notifyPlayerStateChanged(String sessionId, int state) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayer player = (MockPlayer) session2.getPlayer();
            player.notifyPlayerStateChanged(state);
        }

        @Override
        public void notifyPlaybackSpeedChanged(String sessionId, float speed)
                throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayer player = (MockPlayer) session2.getPlayer();
            player.notifyPlaybackSpeedChanged(speed);
        }

        @Override
        public void notifyCurrentMediaItemChanged(String sessionId, int index)
                throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayer player = (MockPlayer) session2.getPlayer();
            switch (index) {
                case INDEX_FOR_UNKONWN_ITEM:
                    player.notifyCurrentMediaItemChanged(
                            new FileMediaItem2.Builder(new FileDescriptor()).build());
                    break;
                case INDEX_FOR_NULL_ITEM:
                    player.notifyCurrentMediaItemChanged(null);
                    break;
                default:
                    player.notifyCurrentMediaItemChanged(
                            player.getPlaylist().get(index));
                    break;
            }
        }

        @Override
        public void notifyAudioAttributesChanged(String sessionId, Bundle attrs)
                throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayer player = (MockPlayer) session2.getPlayer();
            player.notifyAudioAttributesChanged(AudioAttributesCompat.fromBundle(attrs));
        }

        ////////////////////////////////////////////////////////////////////////////////
        // MockPlaylistAgent methods
        ////////////////////////////////////////////////////////////////////////////////

        @Override
        public void setPlaylist(String sessionId, List<Bundle> playlist)
                throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayer player = (MockPlayer) session2.getPlayer();

            List<MediaItem2> list = new ArrayList<>();
            for (Bundle bundle : playlist) {
                list.add(MediaItem2.fromBundle(bundle));
            }
            player.mPlaylist = list;
        }

        @Override
        public void createAndSetDummyPlaylist(String sessionId, int size) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayer player = (MockPlayer) session2.getPlayer();

            List<MediaItem2> list = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                list.add(new MediaItem2.Builder()
                        .setMetadata(new MediaMetadata2.Builder()
                                .putString(MediaMetadata2.METADATA_KEY_MEDIA_ID,
                                        TestUtils.getMediaIdInDummyList(i)).build())
                        .build());
            }
            player.mPlaylist = list;
        }

        @Override
        public void setPlaylistWithDummyItem(String sessionId, List<Bundle> playlist)
                throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayer player = (MockPlayer) session2.getPlayer();

            List<MediaItem2> list = new ArrayList<>();
            for (Bundle bundle : playlist) {
                MediaItem2 item = MediaItem2.fromBundle(bundle);
                list.add(new FileMediaItem2.Builder(new FileDescriptor())
                        .setMetadata(item.getMetadata())
                        .build());
            }
            player.mPlaylist = list;
        }

        @Override
        public void setPlaylistMetadata(String sessionId, Bundle metadata)
                throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayer player = (MockPlayer) session2.getPlayer();
            player.mMetadata = MediaMetadata2.fromBundle(metadata);
        }

        @Override
        public void setShuffleMode(String sessionId, int shuffleMode)
                throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayer player = (MockPlayer) session2.getPlayer();
            player.mShuffleMode = shuffleMode;
        }

        @Override
        public void setRepeatMode(String sessionId, int repeatMode) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayer player = (MockPlayer) session2.getPlayer();
            player.mRepeatMode = repeatMode;
        }

        @Override
        public void setCurrentMediaItem(String sessionId, int index)
                throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayer player = (MockPlayer) session2.getPlayer();
            player.mCurrentMediaItem = player.mPlaylist.get(index);
        }

        @Override
        public void notifyPlaylistChanged(String sessionId) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayer player = (MockPlayer) session2.getPlayer();
            player.notifyPlaylistChanged();
        }

        @Override
        public void notifyPlaylistMetadataChanged(String sessionId) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayer player = (MockPlayer) session2.getPlayer();
            player.notifyPlaylistMetadataChanged();
        }

        @Override
        public void notifyShuffleModeChanged(String sessionId) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayer player = (MockPlayer) session2.getPlayer();
            player.notifyShuffleModeChanged();
        }

        @Override
        public void notifyRepeatModeChanged(String sessionId) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayer player = (MockPlayer) session2.getPlayer();
            player.notifyRepeatModeChanged();
        }

        @Override
        public void notifyPlaybackCompleted(String sessionId) throws RemoteException {
            MediaSession2 session2 = mSession2Map.get(sessionId);
            MockPlayer player = (MockPlayer) session2.getPlayer();
            player.notifyPlaybackCompleted();
        }
    }
}
