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

package androidx.media2.test.service;

import static androidx.media2.test.common.CommonConstants.ACTION_MEDIA2_SESSION;
import static androidx.media2.test.common.CommonConstants.INDEX_FOR_NULL_ITEM;
import static androidx.media2.test.common.CommonConstants.INDEX_FOR_UNKONWN_ITEM;
import static androidx.media2.test.common.CommonConstants.KEY_AUDIO_ATTRIBUTES;
import static androidx.media2.test.common.CommonConstants.KEY_BUFFERED_POSITION;
import static androidx.media2.test.common.CommonConstants.KEY_BUFFERING_STATE;
import static androidx.media2.test.common.CommonConstants.KEY_CURRENT_POSITION;
import static androidx.media2.test.common.CommonConstants.KEY_CURRENT_VOLUME;
import static androidx.media2.test.common.CommonConstants.KEY_MAX_VOLUME;
import static androidx.media2.test.common.CommonConstants.KEY_MEDIA_ITEM;
import static androidx.media2.test.common.CommonConstants.KEY_PLAYBACK_SPEED;
import static androidx.media2.test.common.CommonConstants.KEY_PLAYER_STATE;
import static androidx.media2.test.common.CommonConstants.KEY_PLAYLIST;
import static androidx.media2.test.common.CommonConstants.KEY_PLAYLIST_METADATA;
import static androidx.media2.test.common.CommonConstants.KEY_REPEAT_MODE;
import static androidx.media2.test.common.CommonConstants.KEY_SHUFFLE_MODE;
import static androidx.media2.test.common.CommonConstants.KEY_TRACK_INFO;
import static androidx.media2.test.common.CommonConstants.KEY_VIDEO_SIZE;
import static androidx.media2.test.common.CommonConstants.KEY_VOLUME_CONTROL_TYPE;
import static androidx.media2.test.common.MediaSessionConstants.TEST_CONTROLLER_CALLBACK_SESSION_REJECTS;
import static androidx.media2.test.common.MediaSessionConstants.TEST_GET_SESSION_ACTIVITY;
import static androidx.media2.test.common.MediaSessionConstants.TEST_ON_PLAYLIST_METADATA_CHANGED_SESSION_SET_PLAYLIST;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media.AudioAttributesCompat;
import androidx.media2.common.FileMediaItem;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.MediaParcelUtils;
import androidx.media2.common.ParcelImplListSlice;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.SubtitleData;
import androidx.media2.common.VideoSize;
import androidx.media2.session.MediaSession;
import androidx.media2.session.MediaSession.ControllerInfo;
import androidx.media2.session.SessionCommand;
import androidx.media2.session.SessionCommandGroup;
import androidx.media2.test.common.IRemoteMediaSession;
import androidx.media2.test.common.MockActivity;
import androidx.media2.test.common.TestUtils;
import androidx.media2.test.common.TestUtils.SyncHandler;
import androidx.versionedparcelable.ParcelImpl;
import androidx.versionedparcelable.ParcelUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * A Service that creates {@link MediaSession} and calls its methods according to the client app's
 * requests.
 */
public class MediaSessionProviderService extends Service {
    private static final String TAG = "MediaSessionProviderService";

    Map<String, MediaSession> mSessionMap = new HashMap<>();
    RemoteMediaSessionStub mSessionBinder;

    SyncHandler mHandler;
    Executor mExecutor;

    @Override
    public void onCreate() {
        super.onCreate();
        mSessionBinder = new RemoteMediaSessionStub();
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
        if (ACTION_MEDIA2_SESSION.equals(intent.getAction())) {
            return mSessionBinder;
        }
        return null;
    }

    @Override
    public void onDestroy() {
        for (MediaSession session : mSessionMap.values()) {
            session.close();
        }
    }

    private class RemoteMediaSessionStub extends IRemoteMediaSession.Stub {
        @Override
        public void create(final String sessionId, final Bundle tokenExtras)
                throws RemoteException {
            final MediaSession.Builder builder =
                    new MediaSession.Builder(MediaSessionProviderService.this, new MockPlayer(0))
                            .setId(sessionId);

            if (tokenExtras != null) {
                builder.setExtras(tokenExtras);
            }

            switch (sessionId) {
                case TEST_GET_SESSION_ACTIVITY: {
                    final Intent sessionActivity = new Intent(MediaSessionProviderService.this,
                            MockActivity.class);
                    PendingIntent pendingIntent = PendingIntent.getActivity(
                            MediaSessionProviderService.this,
                            0 /* requestCode */,
                            sessionActivity,
                            Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
                    builder.setSessionActivity(pendingIntent);
                    break;
                }
                case TEST_CONTROLLER_CALLBACK_SESSION_REJECTS: {
                    builder.setSessionCallback(mExecutor, new MediaSession.SessionCallback() {
                        @Override
                        public SessionCommandGroup onConnect(@NonNull MediaSession session,
                                @NonNull MediaSession.ControllerInfo controller) {
                            return null;
                        }
                    });
                    break;
                }
                case TEST_ON_PLAYLIST_METADATA_CHANGED_SESSION_SET_PLAYLIST: {
                    builder.setSessionCallback(mExecutor, new MediaSession.SessionCallback() {
                        @Override
                        public SessionCommandGroup onConnect(@NonNull MediaSession session,
                                @NonNull MediaSession.ControllerInfo controller) {
                            SessionCommandGroup commands = new SessionCommandGroup.Builder()
                                    .addCommand(new SessionCommand(
                                            SessionCommand
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
                        MediaSession session = builder.build();
                        mSessionMap.put(sessionId, session);
                    }
                });
            } catch (InterruptedException ex) {
                Log.e(TAG, "InterruptedException occurred while creating MediaSession", ex);
            }
        }

        ////////////////////////////////////////////////////////////////////////////////
        // MediaSession methods
        ////////////////////////////////////////////////////////////////////////////////

        @Override
        public ParcelImpl getToken(String sessionId) throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            return session != null
                    ? MediaParcelUtils.toParcelable(session.getToken()) : null;
        }

        @Override
        public Bundle getCompatToken(String sessionId) throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            return session.getSessionCompat().getSessionToken().toBundle();
        }

        @Override
        public void updatePlayer(String sessionId, @NonNull Bundle config) throws RemoteException {
            config.setClassLoader(MediaSession.class.getClassLoader());
            if (config != null) {
                config.setClassLoader(MediaSession.class.getClassLoader());
            }
            MediaSession session = mSessionMap.get(sessionId);
            session.updatePlayer(createMockPlayer(config));
        }

        private SessionPlayer createMockPlayer(Bundle config) {
            SessionPlayer player;
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
                localPlayer.mPlaybackSpeed = config.getFloat(KEY_PLAYBACK_SPEED);
                localPlayer.mShuffleMode = config.getInt(KEY_SHUFFLE_MODE);
                localPlayer.mRepeatMode = config.getInt(KEY_REPEAT_MODE);

                ParcelImplListSlice listSlice = config.getParcelable(KEY_PLAYLIST);
                if (listSlice != null) {
                    localPlayer.mPlaylist = MediaTestUtils.convertToMediaItems(listSlice.getList());
                }
                localPlayer.mCurrentMediaItem =
                        MediaTestUtils.convertToMediaItem(config.getParcelable(KEY_MEDIA_ITEM));
                localPlayer.mMetadata = ParcelUtils.getVersionedParcelable(config,
                        KEY_PLAYLIST_METADATA);
                ParcelImpl videoSize = config.getParcelable(KEY_VIDEO_SIZE);
                if (videoSize != null) {
                    localPlayer.mVideoSize = MediaParcelUtils.fromParcelable(videoSize);
                }
                List<SessionPlayer.TrackInfo> trackInfos =
                        ParcelUtils.getVersionedParcelableList(config, KEY_TRACK_INFO);
                localPlayer.mTracks = trackInfos;
                player = localPlayer;
            }
            ParcelImpl attrImpl = config.getParcelable(KEY_AUDIO_ATTRIBUTES);
            if (attrImpl != null) {
                AudioAttributesCompat attr = MediaParcelUtils.fromParcelable(attrImpl);
                if (attr != null) {
                    player.setAudioAttributes(attr);
                }
            }
            return player;
        }

        @Override
        public void broadcastCustomCommand(String sessionId, ParcelImpl command, Bundle args)
                throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            session.broadcastCustomCommand(
                    (SessionCommand) MediaParcelUtils.fromParcelable(command), args);
        }

        @Override
        public void sendCustomCommand(String sessionId, Bundle controller, ParcelImpl command,
                Bundle args) throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            ControllerInfo info = MediaTestUtils.getTestControllerInfo(session);
            session.sendCustomCommand(info,
                    (SessionCommand) MediaParcelUtils.fromParcelable(command),
                    args);
        }

        @Override
        public void close(String sessionId) throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            session.close();
        }

        @Override
        public void setAllowedCommands(String sessionId, Bundle controller, ParcelImpl commands)
                throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            ControllerInfo info = MediaTestUtils.getTestControllerInfo(session);
            session.setAllowedCommands(info,
                    (SessionCommandGroup) MediaParcelUtils.fromParcelable(commands));
        }

        @Override
        public void setCustomLayout(String sessionId, Bundle controller, List<ParcelImpl> layout)
                throws RemoteException {
            if (layout == null) {
                return;
            }
            MediaSession session = mSessionMap.get(sessionId);
            ControllerInfo info = MediaTestUtils.getTestControllerInfo(session);
            List<MediaSession.CommandButton> buttons = new ArrayList<>();
            for (ParcelImpl parcel : layout) {
                if (parcel != null) {
                    buttons.add((MediaSession.CommandButton) ParcelUtils.fromParcelable(parcel));
                }
            }
            session.setCustomLayout(info, buttons);
        }

        ////////////////////////////////////////////////////////////////////////////////
        // MockPlayer methods
        ////////////////////////////////////////////////////////////////////////////////

        @Override
        public void setPlayerState(String sessionId, int state) {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            player.mLastPlayerState = state;
        }

        @Override
        public void setCurrentPosition(String sessionId, long pos) throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            player.mCurrentPosition = pos;
        }

        @Override
        public void setBufferedPosition(String sessionId, long pos) throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            player.mBufferedPosition = pos;
        }

        @Override
        public void setDuration(String sessionId, long duration) throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            player.mDuration = duration;
        }

        @Override
        public void setPlaybackSpeed(String sessionId, float speed) throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            player.mPlaybackSpeed = speed;
        }

        @Override
        public void notifySeekCompleted(String sessionId, long pos) throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            player.notifySeekCompleted(pos);
        }

        @Override
        public void notifyBufferingStateChanged(String sessionId, int itemIndex, int buffState)
                throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            player.notifyBufferingStateChanged(
                    player.getPlaylist().get(itemIndex), buffState);
        }

        @Override
        public void notifyPlayerStateChanged(String sessionId, int state) throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            player.notifyPlayerStateChanged(state);
        }

        @Override
        public void notifyPlaybackSpeedChanged(String sessionId, float speed)
                throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            player.notifyPlaybackSpeedChanged(speed);
        }

        @Override
        public void notifyCurrentMediaItemChanged(String sessionId, int index)
                throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            switch (index) {
                case INDEX_FOR_UNKONWN_ITEM:
                    player.notifyCurrentMediaItemChanged(
                            new FileMediaItem.Builder(ParcelFileDescriptor.adoptFd(-1)).build());
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
        public void notifyAudioAttributesChanged(String sessionId, ParcelImpl attrs)
                throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            player.notifyAudioAttributesChanged(
                    (AudioAttributesCompat) MediaParcelUtils.fromParcelable(attrs));
        }

        @Override
        public void notifyTrackInfoChanged(String sessionId, List<ParcelImpl> trackInfoParcelList)
                throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            List<SessionPlayer.TrackInfo> tracks =
                    MediaParcelUtils.fromParcelableList(trackInfoParcelList);
            player.notifyTracksChanged(tracks);
        }

        @Override
        public void notifyTrackSelected(String sessionId, ParcelImpl trackInfo)
                throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            player.notifyTrackSelected(
                    (SessionPlayer.TrackInfo) MediaParcelUtils.fromParcelable(trackInfo));
        }

        @Override
        public void notifyTrackDeselected(String sessionId, ParcelImpl trackInfo)
                throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            player.notifyTrackDeselected(
                    (SessionPlayer.TrackInfo) MediaParcelUtils.fromParcelable(trackInfo));
        }


        ////////////////////////////////////////////////////////////////////////////////
        // MockPlaylistAgent methods
        ////////////////////////////////////////////////////////////////////////////////

        @Override
        public void setPlaylist(String sessionId, List<ParcelImpl> playlist)
                throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            player.mPlaylist = MediaTestUtils.convertToMediaItems(playlist);
        }

        @Override
        public void setCurrentMediaItemMetadata(String sessionId, ParcelImpl metadata)
                throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            player.mCurrentMediaItem.setMetadata(MediaParcelUtils.fromParcelable(metadata));
        }

        @Override
        public void createAndSetFakePlaylist(String sessionId, int size) throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();

            List<MediaItem> list = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                list.add(new MediaItem.Builder()
                        .setMetadata(new MediaMetadata.Builder()
                                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID,
                                        TestUtils.getMediaIdInFakeList(i)).build())
                        .build());
            }
            player.mPlaylist = list;
        }

        @Override
        public void setPlaylistWithFakeItem(String sessionId, List<ParcelImpl> playlist)
                throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();

            List<MediaItem> list = new ArrayList<>();
            for (ParcelImpl parcel : playlist) {
                MediaItem item = MediaParcelUtils.fromParcelable(parcel);
                list.add(new FileMediaItem.Builder(ParcelFileDescriptor.adoptFd(-1))
                        .setMetadata(item.getMetadata())
                        .build());
            }
            player.mPlaylist = list;
        }

        @Override
        public void setPlaylistMetadata(String sessionId, ParcelImpl metadata)
                throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            player.mMetadata = MediaParcelUtils.fromParcelable(metadata);
        }

        @Override
        public void setPlaylistMetadataWithLargeBitmaps(String sessionId, int count, int width,
                int height) throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            MediaMetadata.Builder builder = new MediaMetadata.Builder();
            for (int i = 0; i < count; i++) {
                builder.putBitmap(TestUtils.getMediaIdInFakeList(i), bitmap);
            }
            player.mMetadata = builder.build();
        }

        @Override
        public void setShuffleMode(String sessionId, int shuffleMode)
                throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            player.mShuffleMode = shuffleMode;
        }

        @Override
        public void setRepeatMode(String sessionId, int repeatMode) throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            player.mRepeatMode = repeatMode;
        }

        @Override
        public void setCurrentMediaItem(String sessionId, int index)
                throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            player.mCurrentMediaItem = player.mPlaylist.get(index);
        }

        @Override
        public void notifyPlaylistChanged(String sessionId) throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            player.notifyPlaylistChanged();
        }

        @Override
        public void notifyPlaylistMetadataChanged(String sessionId) throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            player.notifyPlaylistMetadataChanged();
        }

        @Override
        public void notifyShuffleModeChanged(String sessionId) throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            player.notifyShuffleModeChanged();
        }

        @Override
        public void notifyRepeatModeChanged(String sessionId) throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            player.notifyRepeatModeChanged();
        }

        @Override
        public void notifyPlaybackCompleted(String sessionId) throws RemoteException {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            player.notifyPlaybackCompleted();
        }

        @Override
        public void notifyVideoSizeChanged(String sessionId, ParcelImpl videoSize) {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            VideoSize videoSizeObj = MediaParcelUtils.fromParcelable(videoSize);
            player.notifyVideoSizeChanged(videoSizeObj);
        }

        @Override
        public boolean surfaceExists(String sessionId) {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            return player.surfaceExists();
        }

        @Override
        public void notifySubtitleData(String sessionId, ParcelImpl item, ParcelImpl track,
                ParcelImpl data) {
            MediaSession session = mSessionMap.get(sessionId);
            MockPlayer player = (MockPlayer) session.getPlayer();
            MediaItem itemObj = MediaParcelUtils.fromParcelable(item);
            SessionPlayer.TrackInfo trackObj = MediaParcelUtils.fromParcelable(track);
            SubtitleData dataObj = MediaParcelUtils.fromParcelable(data);
            player.notifySubtitleData(itemObj, trackObj, dataObj);
        }

        @Override
        public void notifyVolumeChanged(String sessionId, int volume) {
            MediaSession session = mSessionMap.get(sessionId);
            MockRemotePlayer player = (MockRemotePlayer) session.getPlayer();
            player.mCurrentVolume = volume;
            player.notifyVolumeChanged();
        }
    }
}
