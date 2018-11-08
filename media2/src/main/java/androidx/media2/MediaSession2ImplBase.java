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

package androidx.media2;

import static androidx.media2.BaseResult2.RESULT_CODE_BAD_VALUE;
import static androidx.media2.MediaSession2.ControllerCb;
import static androidx.media2.MediaSession2.ControllerInfo;
import static androidx.media2.MediaSession2.SessionCallback;
import static androidx.media2.MediaSession2.SessionResult.RESULT_CODE_DISCONNECTED;
import static androidx.media2.MediaSession2.SessionResult.RESULT_CODE_INVALID_STATE;
import static androidx.media2.MediaSession2.SessionResult.RESULT_CODE_SKIPPED;
import static androidx.media2.MediaSession2.SessionResult.RESULT_CODE_SUCCESS;
import static androidx.media2.MediaSession2.SessionResult.RESULT_CODE_UNKNOWN_ERROR;
import static androidx.media2.MediaUtils2.DIRECT_EXECUTOR;
import static androidx.media2.SessionPlayer2.PLAYER_STATE_IDLE;
import static androidx.media2.SessionPlayer2.UNKNOWN_TIME;
import static androidx.media2.SessionToken2.TYPE_SESSION;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.MediaSessionCompat.Token;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.ObjectsCompat;
import androidx.media.AudioAttributesCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.VolumeProviderCompat;
import androidx.media2.MediaController2.PlaybackInfo;
import androidx.media2.MediaSession2.MediaSession2Impl;
import androidx.media2.MediaSession2.SessionResult;
import androidx.media2.SequencedFutureManager.SequencedFuture;
import androidx.media2.SessionPlayer2.PlayerResult;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

class MediaSession2ImplBase implements MediaSession2Impl {
    private static final String DEFAULT_MEDIA_SESSION_TAG_PREFIX = "android.media.session2.id";
    private static final String DEFAULT_MEDIA_SESSION_TAG_DELIM = ".";

    static final String TAG = "MS2ImplBase";
    static final boolean DEBUG = true; //Log.isLoggable(TAG, Log.DEBUG);

    // Note: This checks the uniqueness of a session ID only in single process.
    // When the framework becomes able to check the uniqueness, this logic should be removed.
    @GuardedBy("MediaSession2ImplBase.class")
    private static final List<String> SESSION_ID_LIST = new ArrayList<>();

    private static final SessionResult RESULT_WHEN_CLOSED = new SessionResult(RESULT_CODE_SKIPPED);

    private final Context mContext;
    private final HandlerThread mHandlerThread;
    private final Handler mHandler;
    private final MediaSessionCompat mSessionCompat;
    private final MediaSession2Stub mSession2Stub;
    private final MediaSessionLegacyStub mSessionLegacyStub;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Executor mCallbackExecutor;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final SessionCallback mCallback;
    private final String mSessionId;
    private final SessionToken2 mSessionToken;
    private final AudioManager mAudioManager;
    private final SessionPlayer2.PlayerCallback mPlayerCallback;
    private final MediaSession2 mInstance;
    private final PendingIntent mSessionActivity;

    final Object mLock = new Object();

    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    PlaybackInfo mPlaybackInfo;

    @GuardedBy("mLock")
    private SessionPlayer2 mPlayer;
    @GuardedBy("mLock")
    private MediaBrowserServiceCompat mBrowserServiceLegacyStub;

    MediaSession2ImplBase(MediaSession2 instance, Context context, String id, SessionPlayer2 player,
            PendingIntent sessionActivity, Executor callbackExecutor, SessionCallback callback) {
        mContext = context;
        mInstance = instance;
        mHandlerThread = new HandlerThread("MediaController2_Thread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        mSession2Stub = new MediaSession2Stub(this);
        mSessionActivity = sessionActivity;

        mCallback = callback;
        mCallbackExecutor = callbackExecutor;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        mPlayerCallback = new SessionPlayerCallback(this);

        synchronized (MediaSession2ImplBase.class) {
            if (SESSION_ID_LIST.contains(id)) {
                throw new IllegalArgumentException("Session ID must be unique. ID=" + id);
            }
            SESSION_ID_LIST.add(id);
        }
        mSessionId = id;
        mSessionToken = new SessionToken2(new SessionToken2ImplBase(Process.myUid(),
                TYPE_SESSION, context.getPackageName(), mSession2Stub));
        String sessionCompatId = TextUtils.join(DEFAULT_MEDIA_SESSION_TAG_DELIM,
                new String[] {DEFAULT_MEDIA_SESSION_TAG_PREFIX, id});

        mSessionCompat = new MediaSessionCompat(context, sessionCompatId, mSessionToken);
        // NOTE: mSessionLegacyStub should be created after mSessionCompat created.
        mSessionLegacyStub = new MediaSessionLegacyStub(this);

        mSessionCompat.setSessionActivity(sessionActivity);
        mSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS);

        updatePlayer(player);
        // Do followings at the last moment. Otherwise commands through framework would be sent to
        // this session while initializing, and end up with unexpected situation.
        mSessionCompat.setCallback(mSessionLegacyStub, mHandler);
        mSessionCompat.setActive(true);
    }

    @Override
    public void updatePlayer(@NonNull SessionPlayer2 player,
            @Nullable SessionPlayer2 playlistAgent) {
        // No-op
    }

    // TODO(jaewan): Remove SuppressLint when removing duplication session callback.
    @Override
    @SuppressLint("WrongConstant")
    public void updatePlayer(@NonNull SessionPlayer2 player) {
        final boolean isPlaybackInfoChanged;

        final SessionPlayer2 oldPlayer;
        final PlaybackInfo info = createPlaybackInfo(player, null);

        synchronized (mLock) {
            isPlaybackInfoChanged = !info.equals(mPlaybackInfo);

            oldPlayer = mPlayer;
            mPlayer = player;
            mPlaybackInfo = info;

            if (oldPlayer != mPlayer) {
                if (oldPlayer != null) {
                    oldPlayer.unregisterPlayerCallback(mPlayerCallback);
                }
                mPlayer.registerPlayerCallback(mCallbackExecutor, mPlayerCallback);
            }
        }

        if (oldPlayer == null) {
            // updatePlayerConnector() is called inside of the constructor.
            // There's no connected controllers at this moment, so just initialize session compat's
            // playback state. Otherwise, framework doesn't know whether this is ready to receive
            // media key event.
            mSessionCompat.setPlaybackState(createPlaybackStateCompat());
        } else {
            if (player != oldPlayer) {
                final int state = getPlayerState();
                mCallbackExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onPlayerStateChanged(getInstance(), state);
                    }
                });
                notifyPlayerUpdatedNotLocked(oldPlayer);
            }
            if (isPlaybackInfoChanged) {
                notifyPlaybackInfoChangedNotLocked(info);
            }
        }

        if (player instanceof RemoteSessionPlayer2) {
            final RemoteSessionPlayer2 remotePlayer = (RemoteSessionPlayer2) player;
            VolumeProviderCompat volumeProvider =
                    new VolumeProviderCompat(remotePlayer.getVolumeControlType(),
                            remotePlayer.getMaxVolume(),
                            remotePlayer.getVolume()) {
                        @Override
                        public void onSetVolumeTo(int volume) {
                            remotePlayer.setVolume(volume);
                        }

                        @Override
                        public void onAdjustVolume(int direction) {
                            remotePlayer.adjustVolume(direction);
                        }
                    };
            mSessionCompat.setPlaybackToRemote(volumeProvider);
        } else {
            int stream = getLegacyStreamType(player.getAudioAttributes());
            mSessionCompat.setPlaybackToLocal(stream);
        }
    }

    @NonNull PlaybackInfo createPlaybackInfo(@NonNull SessionPlayer2 player,
            AudioAttributesCompat audioAttributes) {
        final AudioAttributesCompat attrs = audioAttributes != null ? audioAttributes :
                player.getAudioAttributes();

        if (!(player instanceof RemoteSessionPlayer2)) {
            int stream = getLegacyStreamType(attrs);
            int controlType = VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
            if (Build.VERSION.SDK_INT >= 21 && mAudioManager.isVolumeFixed()) {
                controlType = VolumeProviderCompat.VOLUME_CONTROL_FIXED;
            }
            return PlaybackInfo.createPlaybackInfo(
                    PlaybackInfo.PLAYBACK_TYPE_LOCAL,
                    attrs,
                    controlType,
                    mAudioManager.getStreamMaxVolume(stream),
                    mAudioManager.getStreamVolume(stream));
        } else {
            RemoteSessionPlayer2 remotePlayer = (RemoteSessionPlayer2) player;
            return PlaybackInfo.createPlaybackInfo(
                    PlaybackInfo.PLAYBACK_TYPE_REMOTE,
                    attrs,
                    remotePlayer.getVolumeControlType(),
                    remotePlayer.getMaxVolume(),
                    remotePlayer.getVolume());
        }
    }

    private int getLegacyStreamType(@Nullable AudioAttributesCompat attrs) {
        int stream;
        if (attrs == null) {
            stream = AudioManager.STREAM_MUSIC;
        } else {
            stream = attrs.getLegacyStreamType();
            if (stream == AudioManager.USE_DEFAULT_STREAM_TYPE) {
                // Usually, AudioAttributesCompat#getLegacyStreamType() does not return
                // USE_DEFAULT_STREAM_TYPE unless the developer sets it with
                // AudioAttributesCompat.Builder#setLegacyStreamType().
                // But for safety, let's convert USE_DEFAULT_STREAM_TYPE to STREAM_MUSIC here.
                stream = AudioManager.STREAM_MUSIC;
            }
        }
        return stream;
    }

    @Override
    public void close() {
        synchronized (mLock) {
            if (isClosed()) {
                return;
            }
            if (DEBUG) {
                Log.d(TAG, "Closing session, id=" + getId() + ", token=" + getToken());
            }
            synchronized (MediaSession2ImplBase.class) {
                SESSION_ID_LIST.remove(mSessionId);
            }
            mPlayer.unregisterPlayerCallback(mPlayerCallback);
            mSessionCompat.release();
            mCallback.onSessionClosed(mInstance);
            dispatchRemoteControllerCallbackTask(new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onDisconnected();
                }
            });
            mHandler.removeCallbacksAndMessages(null);
            if (mHandlerThread.isAlive()) {
                if (Build.VERSION.SDK_INT >= 18) {
                    mHandlerThread.quitSafely();
                } else {
                    mHandlerThread.quit();
                }
            }
        }
    }

    @Override
    public @NonNull SessionPlayer2 getPlayer() {
        synchronized (mLock) {
            return mPlayer;
        }
    }

    @Override
    public String getId() {
        return mSessionId;
    }

    @Override
    public @NonNull SessionToken2 getToken() {
        return mSessionToken;
    }

    @Override
    public @NonNull List<ControllerInfo> getConnectedControllers() {
        List<ControllerInfo> controllers = new ArrayList<>();
        controllers.addAll(mSession2Stub.getConnectedControllersManager()
                .getConnectedControllers());
        controllers.addAll(mSessionLegacyStub.getConnectedControllersManager()
                .getConnectedControllers());
        return controllers;
    }

    @Override
    public boolean isConnected(ControllerInfo controller) {
        if (controller == null) {
            return false;
        }
        if (controller.equals(mSessionLegacyStub.getControllersForAll())) {
            return true;
        }
        return mSession2Stub.getConnectedControllersManager().isConnected(controller)
                || mSessionLegacyStub.getConnectedControllersManager().isConnected(controller);
    }

    @Override
    public ListenableFuture<SessionResult> setCustomLayout(@NonNull ControllerInfo controller,
            @NonNull final List<MediaSession2.CommandButton> layout) {
        return dispatchRemoteControllerTask(controller, new RemoteControllerTask() {
            @Override
            public void run(ControllerCb controller, int seq) throws RemoteException {
                controller.setCustomLayout(seq, layout);
            }
        });
    }

    @Override
    public void setAllowedCommands(@NonNull ControllerInfo controller,
            @NonNull final SessionCommandGroup2 commands) {
        if (mSession2Stub.getConnectedControllersManager().isConnected(controller)) {
            mSession2Stub.getConnectedControllersManager()
                    .updateAllowedCommands(controller, commands);
            dispatchRemoteControllerCallbackTask(controller, new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onAllowedCommandsChanged(commands);
                }
            });
        } else {
            mSessionLegacyStub.getConnectedControllersManager()
                    .updateAllowedCommands(controller, commands);
        }
    }

    @Override
    public void broadcastCustomCommand(@NonNull final SessionCommand2 command,
            @Nullable final Bundle args) {
        dispatchRemoteControllerTask(new RemoteControllerTask() {
            @Override
            public void run(ControllerCb controller, int seq) throws RemoteException {
                controller.sendCustomCommand(seq, command, args);
            }
        });
    }

    @Override
    public ListenableFuture<SessionResult> sendCustomCommand(
            @NonNull ControllerInfo controller, @NonNull final SessionCommand2 command,
            @Nullable final Bundle args) {
        return dispatchRemoteControllerTask(controller, new RemoteControllerTask() {
            @Override
            public void run(ControllerCb controller, int seq) throws RemoteException {
                controller.sendCustomCommand(seq, command, args);
            }
        });
    }

    @Override
    public ListenableFuture<PlayerResult> play() {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer2 player) throws Exception {
                if (player.getPlayerState() != PLAYER_STATE_IDLE) {
                    return player.play();
                }
                final ListenableFuture<PlayerResult> prepareFuture = player.prepare();
                final ListenableFuture<PlayerResult> playFuture = player.play();
                if (prepareFuture == null || playFuture == null) {
                    // Let dispatchPlayerTask() handle such cases.
                    return null;
                }
                return MediaPlayer.CombindedCommandResultFuture.create(DIRECT_EXECUTOR,
                        prepareFuture, playFuture);
            }
        });
    }

    @Override
    public ListenableFuture<PlayerResult> pause() {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer2 player) throws Exception {
                return player.pause();
            }
        });
    }

    @Override
    public ListenableFuture<PlayerResult> prepare() {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer2 player) throws Exception {
                return player.prepare();
            }
        });
    }

    @Override
    public ListenableFuture<PlayerResult> seekTo(final long pos) {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer2 player) throws Exception {
                return player.seekTo(pos);
            }
        });
    }

    @Override
    public void notifyRoutesInfoChanged(@NonNull ControllerInfo controller,
            @Nullable final List<Bundle> routes) {
        dispatchRemoteControllerCallbackTask(controller, new RemoteControllerCallbackTask() {
            @Override
            public void run(ControllerCb callback) throws RemoteException {
                callback.onRoutesInfoChanged(routes);
            }
        });
    }

    @Override public @SessionPlayer2.PlayerState int getPlayerState() {
        return dispatchPlayerTask(new PlayerTask<Integer>() {
            @Override
            public Integer run(SessionPlayer2 player) throws Exception {
                return player.getPlayerState();
            }
        }, SessionPlayer2.PLAYER_STATE_ERROR);
    }

    @Override
    public long getCurrentPosition() {
        return dispatchPlayerTask(new PlayerTask<Long>() {
            @Override
            public Long run(SessionPlayer2 player) throws Exception {
                if (isInPlaybackState(player)) {
                    return player.getCurrentPosition();
                }
                return null;
            }
        }, SessionPlayer2.UNKNOWN_TIME);
    }

    @Override
    public long getDuration() {
        return dispatchPlayerTask(new PlayerTask<Long>() {
            @Override
            public Long run(SessionPlayer2 player) throws Exception {
                if (isInPlaybackState(player)) {
                    return player.getDuration();
                }
                return null;
            }
        }, SessionPlayer2.UNKNOWN_TIME);
    }

    @Override
    public long getBufferedPosition() {
        return dispatchPlayerTask(new PlayerTask<Long>() {
            @Override
            public Long run(SessionPlayer2 player) throws Exception {
                if (isInPlaybackState(player)) {
                    return player.getBufferedPosition();
                }
                return null;
            }
        }, SessionPlayer2.UNKNOWN_TIME);
    }

    @Override
    public @SessionPlayer2.BuffState int getBufferingState() {
        return dispatchPlayerTask(new PlayerTask<Integer>() {
            @Override
            public Integer run(SessionPlayer2 player) throws Exception {
                return player.getBufferingState();
            }
        }, SessionPlayer2.BUFFERING_STATE_UNKNOWN);
    }

    @Override
    public float getPlaybackSpeed() {
        return dispatchPlayerTask(new PlayerTask<Float>() {
            @Override
            public Float run(SessionPlayer2 player) throws Exception {
                if (isInPlaybackState(player)) {
                    return player.getPlaybackSpeed();
                }
                return null;
            }
        }, 1.0f);
    }

    @Override
    public ListenableFuture<PlayerResult> setPlaybackSpeed(final float speed) {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer2 player) throws Exception {
                return player.setPlaybackSpeed(speed);
            }
        });
    }

    @Override
    public List<MediaItem2> getPlaylist() {
        return dispatchPlayerTask(new PlayerTask<List<MediaItem2>>() {
            @Override
            public List<MediaItem2> run(SessionPlayer2 player) throws Exception {
                return player.getPlaylist();
            }
        }, null);
    }

    @Override
    public ListenableFuture<PlayerResult> setPlaylist(final @NonNull List<MediaItem2> list,
            final @Nullable MediaMetadata2 metadata) {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer2 player) throws Exception {
                if (list == null) {
                    throw new IllegalArgumentException("list shouldn't be null");
                }
                return player.setPlaylist(list, metadata);
            }
        });
    }

    @Override
    public ListenableFuture<PlayerResult> setMediaItem(final @NonNull MediaItem2 item) {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer2 player) throws Exception {
                if (item == null) {
                    throw new IllegalArgumentException("item shouldn't be null");
                }
                return player.setMediaItem(item);
            }
        });
    }

    @Override
    public ListenableFuture<PlayerResult> skipToPlaylistItem(final int index) {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer2 player) throws Exception {
                if (index < 0) {
                    throw new IllegalArgumentException("index shouldn't be negative");
                }
                final List<MediaItem2> list = player.getPlaylist();
                if (index >= list.size()) {
                    return PlayerResult.createFuture(RESULT_CODE_BAD_VALUE);
                }
                return player.skipToPlaylistItem(list.get(index));
            }
        });
    }

    @Override
    public ListenableFuture<PlayerResult> skipToPreviousItem() {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer2 player) throws Exception {
                return player.skipToPreviousPlaylistItem();
            }
        });
    }

    @Override
    public ListenableFuture<PlayerResult> skipToNextItem() {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer2 player) throws Exception {
                return player.skipToNextPlaylistItem();
            }
        });
    }

    @Override
    public MediaMetadata2 getPlaylistMetadata() {
        return dispatchPlayerTask(new PlayerTask<MediaMetadata2>() {
            @Override
            public MediaMetadata2 run(SessionPlayer2 player) throws Exception {
                return player.getPlaylistMetadata();
            }
        }, null);
    }

    @Override
    public ListenableFuture<PlayerResult> addPlaylistItem(final int index,
            final @NonNull MediaItem2 item) {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer2 player) throws Exception {
                if (index < 0) {
                    throw new IllegalArgumentException("index shouldn't be negative");
                }
                if (item == null) {
                    throw new IllegalArgumentException("item shouldn't be null");
                }
                return player.addPlaylistItem(index, item);
            }
        });
    }

    @Override
    public ListenableFuture<PlayerResult> removePlaylistItem(final int index) {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer2 player) throws Exception {
                if (index < 0) {
                    throw new IllegalArgumentException("index shouldn't be negative");
                }
                final List<MediaItem2> list = player.getPlaylist();
                if (index >= list.size()) {
                    return PlayerResult.createFuture(RESULT_CODE_BAD_VALUE);
                }
                return player.removePlaylistItem(list.get(index));
            }
        });
    }

    @Override
    public ListenableFuture<PlayerResult> replacePlaylistItem(final int index,
            final @NonNull MediaItem2 item) {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer2 player) throws Exception {
                if (index < 0) {
                    throw new IllegalArgumentException("index shouldn't be negative");
                }
                if (item == null) {
                    throw new IllegalArgumentException("item shouldn't be null");
                }
                return player.replacePlaylistItem(index, item);
            }
        });

    }

    @Override
    public MediaItem2 getCurrentMediaItem() {
        return dispatchPlayerTask(new PlayerTask<MediaItem2>() {
            @Override
            public MediaItem2 run(SessionPlayer2 player) throws Exception {
                return player.getCurrentMediaItem();
            }
        }, null);
    }

    @Override
    public ListenableFuture<PlayerResult> updatePlaylistMetadata(
            final @Nullable MediaMetadata2 metadata) {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer2 player) throws Exception {
                return player.updatePlaylistMetadata(metadata);
            }
        });
    }

    @Override
    public @SessionPlayer2.RepeatMode int getRepeatMode() {
        return dispatchPlayerTask(new PlayerTask<Integer>() {
            @Override
            public Integer run(SessionPlayer2 player) throws Exception {
                return player.getRepeatMode();
            }
        }, SessionPlayer2.REPEAT_MODE_NONE);
    }

    @Override
    public ListenableFuture<PlayerResult> setRepeatMode(
            final @SessionPlayer2.RepeatMode int repeatMode) {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer2 player) throws Exception {
                return player.setRepeatMode(repeatMode);
            }
        });
    }

    @Override
    public @SessionPlayer2.ShuffleMode int getShuffleMode() {
        return dispatchPlayerTask(new PlayerTask<Integer>() {
            @Override
            public Integer run(SessionPlayer2 player) throws Exception {
                return player.getShuffleMode();
            }
        }, SessionPlayer2.SHUFFLE_MODE_NONE);
    }

    @Override
    public ListenableFuture<PlayerResult> setShuffleMode(
            final @SessionPlayer2.ShuffleMode int shuffleMode) {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer2 player) throws Exception {
                return player.setShuffleMode(shuffleMode);
            }
        });
    }

    ///////////////////////////////////////////////////
    // package private and private methods
    ///////////////////////////////////////////////////
    @Override
    public @NonNull MediaSession2 getInstance() {
        return mInstance;
    }

    @Override
    public Context getContext() {
        return mContext;
    }

    @Override
    public Executor getCallbackExecutor() {
        return mCallbackExecutor;
    }

    @Override
    public SessionCallback getCallback() {
        return mCallback;
    }

    @Override
    public MediaSessionCompat getSessionCompat() {
        return mSessionCompat;
    }

    @Override
    public boolean isClosed() {
        return !mHandlerThread.isAlive();
    }

    @Override
    public PlaybackStateCompat createPlaybackStateCompat() {
        synchronized (mLock) {
            int state = MediaUtils2.convertToPlaybackStateCompatState(getPlayerState(),
                    getBufferingState());
            long allActions = PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_PAUSE
                    | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_REWIND
                    | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    | PlaybackStateCompat.ACTION_FAST_FORWARD
                    | PlaybackStateCompat.ACTION_SET_RATING
                    | PlaybackStateCompat.ACTION_SEEK_TO | PlaybackStateCompat.ACTION_PLAY_PAUSE
                    | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                    | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                    | PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
                    | PlaybackStateCompat.ACTION_PLAY_FROM_URI | PlaybackStateCompat.ACTION_PREPARE
                    | PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID
                    | PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH
                    | PlaybackStateCompat.ACTION_PREPARE_FROM_URI
                    | PlaybackStateCompat.ACTION_SET_REPEAT_MODE
                    | PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE
                    | PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED;
            return new PlaybackStateCompat.Builder()
                    .setState(state, getCurrentPosition(), getPlaybackSpeed(),
                            SystemClock.elapsedRealtime())
                    .setActions(allActions)
                    .setBufferedPosition(getBufferedPosition())
                    .build();
        }
    }

    @Override
    public PlaybackInfo getPlaybackInfo() {
        synchronized (mLock) {
            return mPlaybackInfo;
        }
    }

    @Override
    public PendingIntent getSessionActivity() {
        return mSessionActivity;
    }

    MediaBrowserServiceCompat createLegacyBrowserService(Context context, SessionToken2 token,
            Token sessionToken) {
        return new MediaSessionService2LegacyStub(context, this, sessionToken);
    }

    @Override
    public void connectFromService(IMediaController2 caller, String packageName, int pid, int uid) {
        mSession2Stub.connect(caller, packageName, pid, uid);
    }

    /**
     * Gets the service binder from the MediaBrowserServiceCompat. Should be only called by the
     * thread with a Looper.
     *
     * @return
     */
    @Override
    public IBinder getLegacyBrowserServiceBinder() {
        MediaBrowserServiceCompat legacyStub;
        synchronized (mLock) {
            if (mBrowserServiceLegacyStub == null) {
                mBrowserServiceLegacyStub = createLegacyBrowserService(mContext, mSessionToken,
                        mSessionCompat.getSessionToken());
            }
            legacyStub = mBrowserServiceLegacyStub;
        }
        Intent intent = new Intent(MediaBrowserServiceCompat.SERVICE_INTERFACE);
        return legacyStub.onBind(intent);
    }

    MediaBrowserServiceCompat getLegacyBrowserService() {
        synchronized (mLock) {
            return mBrowserServiceLegacyStub;
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean isInPlaybackState(@NonNull SessionPlayer2 player) {
        return !isClosed()
                && player.getPlayerState() != SessionPlayer2.PLAYER_STATE_IDLE
                && player.getPlayerState() != SessionPlayer2.PLAYER_STATE_ERROR;
    }

    private @Nullable MediaItem2 getCurrentMediaItemOrNull() {
        final SessionPlayer2 player;
        synchronized (mLock) {
            player = mPlayer;
        }
        return player != null ? player.getCurrentMediaItem() : null;
    }

    private @Nullable List<MediaItem2> getPlaylistOrNull() {
        final SessionPlayer2 player;
        synchronized (mLock) {
            player = mPlayer;
        }
        return player != null ? player.getPlaylist() : null;
    }

    private ListenableFuture<PlayerResult> dispatchPlayerTask(
            @NonNull PlayerTask<ListenableFuture<PlayerResult>> command) {
        ResolvableFuture<PlayerResult> result = ResolvableFuture.create();
        result.set(new PlayerResult(RESULT_CODE_INVALID_STATE, null));
        return dispatchPlayerTask(command, result);
    }

    private <T> T dispatchPlayerTask(@NonNull PlayerTask<T> command, T defaultResult) {
        final SessionPlayer2 player;
        synchronized (mLock) {
            player = mPlayer;
        }
        try {
            if (!isClosed()) {
                T result = command.run(player);
                if (result != null) {
                    return result;
                }
            } else if (DEBUG) {
                Log.d(TAG, "API calls after the close()", new IllegalStateException());
            }
        } catch (Exception e) {
        }
        return defaultResult;
    }

    // TODO(jaewan): Remove SuppressLint when removing duplication session callback.
    @SuppressLint("WrongConstant")
    private void notifyPlayerUpdatedNotLocked(SessionPlayer2 oldPlayer) {
        // Tells the playlist change first, to current item can change be notified with an item
        // within the playlist.
        List<MediaItem2> oldPlaylist = oldPlayer.getPlaylist();
        final List<MediaItem2> newPlaylist = getPlaylistOrNull();
        if (!ObjectsCompat.equals(oldPlaylist, newPlaylist)) {
            dispatchRemoteControllerCallbackTask(new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlaylistChanged(
                            newPlaylist, getPlaylistMetadata());
                }
            });
        } else {
            MediaMetadata2 oldMetadata = oldPlayer.getPlaylistMetadata();
            final MediaMetadata2 newMetadata = getPlaylistMetadata();
            if (!ObjectsCompat.equals(oldMetadata, newMetadata)) {
                dispatchRemoteControllerCallbackTask(new RemoteControllerCallbackTask() {
                    @Override
                    public void run(ControllerCb callback) throws RemoteException {
                        callback.onPlaylistMetadataChanged(newMetadata);
                    }
                });
            }
        }
        MediaItem2 oldCurrentItem = oldPlayer.getCurrentMediaItem();
        final MediaItem2 newCurrentItem = getCurrentMediaItemOrNull();
        if (!ObjectsCompat.equals(oldCurrentItem, newCurrentItem)) {
            dispatchRemoteControllerCallbackTask(new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onCurrentMediaItemChanged(newCurrentItem);
                }
            });
        }
        final @SessionPlayer2.RepeatMode int repeatMode = getRepeatMode();
        if (oldPlayer.getRepeatMode() != repeatMode) {
            dispatchRemoteControllerCallbackTask(new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onRepeatModeChanged(repeatMode);
                }
            });
        }
        final @SessionPlayer2.ShuffleMode int shuffleMode = getShuffleMode();
        if (oldPlayer.getShuffleMode() != shuffleMode) {
            dispatchRemoteControllerCallbackTask(new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onShuffleModeChanged(shuffleMode);
                }
            });
        }

        // Always forcefully send the player state and buffered state to send the current position
        // and buffered position.
        final long currentTimeMs = SystemClock.elapsedRealtime();
        final long positionMs = getCurrentPosition();
        final int playerState = getPlayerState();
        dispatchRemoteControllerCallbackTask(new RemoteControllerCallbackTask() {
            @Override
            public void run(ControllerCb callback) throws RemoteException {
                callback.onPlayerStateChanged(currentTimeMs, positionMs, playerState);
            }
        });
        final MediaItem2 item = getCurrentMediaItemOrNull();
        if (item != null) {
            final int bufferingState = getBufferingState();
            final long bufferedPositionMs = getBufferedPosition();
            dispatchRemoteControllerCallbackTask(new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onBufferingStateChanged(item, bufferingState, bufferedPositionMs);
                }
            });
        }
        final float speed = getPlaybackSpeed();
        if (speed != oldPlayer.getPlaybackSpeed()) {
            dispatchRemoteControllerCallbackTask(new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlaybackSpeedChanged(currentTimeMs, positionMs, speed);
                }
            });
        }
        // Note: AudioInfo is updated outside of this API.
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void notifyPlaybackInfoChangedNotLocked(final PlaybackInfo info) {
        dispatchRemoteControllerCallbackTask(new RemoteControllerCallbackTask() {
            @Override
            public void run(ControllerCb callback) throws RemoteException {
                callback.onPlaybackInfoChanged(info);
            }
        });
    }

    void dispatchRemoteControllerCallbackTask(@NonNull ControllerInfo controller,
            @NonNull RemoteControllerCallbackTask task) {
        if (!isConnected(controller)) {
            // Do not send command to an unconnected controller.
            return;
        }
        try {
            task.run(controller.getControllerCb());
        } catch (DeadObjectException e) {
            onDeadObjectException(controller, e);
        } catch (RemoteException e) {
            // Currently it's TransactionTooLargeException or DeadSystemException.
            // We'd better to leave log for those cases because
            //   - TransactionTooLargeException means that we may need to fix our code.
            //     (e.g. add pagination or special way to deliver Bitmap)
            //   - DeadSystemException means that errors around it can be ignored.
            Log.w(TAG, "Exception in " + controller.toString(), e);
        }
    }

    void dispatchRemoteControllerCallbackTask(@NonNull RemoteControllerCallbackTask task) {
        List<ControllerInfo> controllers =
                mSession2Stub.getConnectedControllersManager().getConnectedControllers();
        controllers.add(mSessionLegacyStub.getControllersForAll());
        for (int i = 0; i < controllers.size(); i++) {
            dispatchRemoteControllerCallbackTask(controllers.get(i), task);
        }
    }

    private ListenableFuture<SessionResult> dispatchRemoteControllerTask(
            @NonNull ControllerInfo controller, @NonNull RemoteControllerTask task) {
        if (!isConnected(controller)) {
            return SessionResult.createFutureWithResult(RESULT_CODE_DISCONNECTED);
        }
        try {
            final ListenableFuture<SessionResult> future;
            final int seq;
            final SequencedFutureManager manager =
                    mSession2Stub.getConnectedControllersManager()
                            .getSequencedFutureManager(controller);
            if (manager != null) {
                future = manager.createSequencedFuture(RESULT_WHEN_CLOSED);
                seq = ((SequencedFuture<SessionResult>) future).getSequenceNumber();
            } else {
                // Can be null in two cases. Use the 0 as sequence number in both cases because
                //     Case 1) Controller is from the legacy stub
                //             -> Sequence number isn't needed, so 0 is OK
                //     Case 2) Controller is removed after the connection check above
                //             -> Call will fail below or ignored by the controller, so 0 is OK.
                seq = 0;
                future = SessionResult.createFutureWithResult(RESULT_CODE_SUCCESS);
            }
            task.run(controller.getControllerCb(), seq);
            return future;
        } catch (DeadObjectException e) {
            onDeadObjectException(controller, e);
            return SessionResult.createFutureWithResult(RESULT_CODE_DISCONNECTED);
        } catch (RemoteException e) {
            // Currently it's TransactionTooLargeException or DeadSystemException.
            // We'd better to leave log for those cases because
            //   - TransactionTooLargeException means that we may need to fix our code.
            //     (e.g. add pagination or special way to deliver Bitmap)
            //   - DeadSystemException means that errors around it can be ignored.
            Log.w(TAG, "Exception in " + controller.toString(), e);
        }
        return SessionResult.createFutureWithResult(RESULT_CODE_UNKNOWN_ERROR);
    }

    void dispatchRemoteControllerTask(@NonNull RemoteControllerTask task) {
        List<ControllerInfo> controllers =
                mSession2Stub.getConnectedControllersManager().getConnectedControllers();
        controllers.add(mSessionLegacyStub.getControllersForAll());
        for (int i = 0; i < controllers.size(); i++) {
            ControllerInfo controller = controllers.get(i);
            try {
                final int seq;
                final SequencedFutureManager manager =
                        mSession2Stub.getConnectedControllersManager()
                                .getSequencedFutureManager(controller);
                if (manager != null) {
                    seq = manager.obtainNextSequenceNumber();
                } else {
                    // Can be null in two cases. Use the 0 as sequence number in both cases because
                    //     Case 1) Controller is from the legacy stub
                    //             -> Sequence number isn't needed, so 0 is OK
                    //     Case 2) Controller is removed after the connection check above
                    //             -> Call will fail below or ignored by the controller, so 0 is OK.
                    seq = 0;
                }
                task.run(controller.getControllerCb(), seq);
            } catch (DeadObjectException e) {
                onDeadObjectException(controller, e);
            } catch (RemoteException e) {
                // Currently it's TransactionTooLargeException or DeadSystemException.
                // We'd better to leave log for those cases because
                //   - TransactionTooLargeException means that we may need to fix our code.
                //     (e.g. add pagination or special way to deliver Bitmap)
                //   - DeadSystemException means that errors around it can be ignored.
                Log.w(TAG, "Exception in " + controller.toString(), e);
            }
        }
    }

    /**
     * Removes controller. Call this when DeadObjectException is happened with binder call.
     */
    private void onDeadObjectException(ControllerInfo controller, DeadObjectException e) {
        if (DEBUG) {
            Log.d(TAG, controller.toString() + " is gone", e);
        }
        // Note: Only removing from MediaSession2Stub and ignoring (legacy) stubs would be fine for
        //       now. Because calls to the legacy stubs doesn't throw DeadObjectException.
        mSession2Stub.getConnectedControllersManager().removeController(controller);
    }

    ///////////////////////////////////////////////////
    // Inner classes
    ///////////////////////////////////////////////////
    @FunctionalInterface
    interface PlayerTask<T> {
        T run(@NonNull SessionPlayer2 player) throws Exception;
    }

    @FunctionalInterface
    interface RemoteControllerTask {
        void run(ControllerCb controller, int seq) throws RemoteException;
    }

    @FunctionalInterface
    interface RemoteControllerCallbackTask {
        void run(ControllerCb controller) throws RemoteException;
    }

    private static class SessionPlayerCallback extends SessionPlayer2.PlayerCallback {
        private final WeakReference<MediaSession2ImplBase> mSession;
        private MediaItem2 mMediaItem;
        private List<MediaItem2> mList;
        private final CurrentMediaItemListener mCurrentItemChangedListener;
        private final PlaylistItemListener mPlaylistItemChangedListener;

        SessionPlayerCallback(MediaSession2ImplBase session) {
            mSession = new WeakReference<>(session);
            mCurrentItemChangedListener = new CurrentMediaItemListener(session);
            mPlaylistItemChangedListener = new PlaylistItemListener(session);
        }

        @Override
        public void onCurrentMediaItemChanged(final SessionPlayer2 player, final MediaItem2 item) {
            final MediaSession2ImplBase session = getSession();
            if (session == null || session.getPlayer() != player || player == null) {
                return;
            }
            synchronized (session.mLock) {
                if (mMediaItem != null) {
                    mMediaItem.removeOnMetadataChangedListener(mCurrentItemChangedListener);
                }
                if (item != null)  {
                    item.addOnMetadataChangedListener(session.mCallbackExecutor,
                            mCurrentItemChangedListener);
                }
                mMediaItem = item;
            }

            // Note: No sanity check whether the item is in the playlist.
            updateDurationIfNeeded(player, item);
            session.dispatchRemoteControllerCallbackTask(new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onCurrentMediaItemChanged(item);
                }
            });
        }

        @Override
        public void onPlayerStateChanged(final SessionPlayer2 player, final int state) {
            final MediaSession2ImplBase session = getSession();
            if (session == null || session.getPlayer() != player || player == null) {
                return;
            }
            session.getCallback().onPlayerStateChanged(session.getInstance(), state);
            updateDurationIfNeeded(player, player.getCurrentMediaItem());
            session.dispatchRemoteControllerCallbackTask(new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlayerStateChanged(SystemClock.elapsedRealtime(),
                            player.getCurrentPosition(), state);
                }
            });
        }

        @Override
        public void onBufferingStateChanged(final SessionPlayer2 player,
                final MediaItem2 item, final int state) {
            updateDurationIfNeeded(player, item);
            dispatchRemoteControllerTask(player, new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onBufferingStateChanged(item, state, player.getBufferedPosition());
                }
            });
        }

        @Override
        public void onPlaybackSpeedChanged(final SessionPlayer2 player, final float speed) {
            dispatchRemoteControllerTask(player, new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlaybackSpeedChanged(SystemClock.elapsedRealtime(),
                            player.getCurrentPosition(), speed);
                }
            });
        }

        @Override
        public void onSeekCompleted(final SessionPlayer2 player, final long position) {
            dispatchRemoteControllerTask(player, new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onSeekCompleted(SystemClock.elapsedRealtime(),
                            player.getCurrentPosition(), position);
                }
            });
        }

        @Override
        public void onPlaylistChanged(final SessionPlayer2 player, final List<MediaItem2> list,
                final MediaMetadata2 metadata) {
            final MediaSession2ImplBase session = getSession();
            if (session == null || session.getPlayer() != player || player == null) {
                return;
            }
            synchronized (session.mLock) {
                if (mList != null) {
                    for (int i = 0; i < mList.size(); i++) {
                        mList.get(i).removeOnMetadataChangedListener(mPlaylistItemChangedListener);
                    }
                }
                if (list != null) {
                    for (int i = 0; i < list.size(); i++) {
                        list.get(i).addOnMetadataChangedListener(session.mCallbackExecutor,
                                mPlaylistItemChangedListener);
                    }
                }
                mList = list;
            }

            dispatchRemoteControllerTask(player, new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlaylistChanged(list, metadata);
                }
            });
        }

        @Override
        public void onPlaylistMetadataChanged(final SessionPlayer2 player,
                final MediaMetadata2 metadata) {
            dispatchRemoteControllerTask(player, new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlaylistMetadataChanged(metadata);
                }
            });
        }

        @Override
        public void onRepeatModeChanged(final SessionPlayer2 player, final int repeatMode) {
            dispatchRemoteControllerTask(player, new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onRepeatModeChanged(repeatMode);
                }
            });
        }

        @Override
        public void onShuffleModeChanged(final SessionPlayer2 player, final int shuffleMode) {
            dispatchRemoteControllerTask(player, new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onShuffleModeChanged(shuffleMode);
                }
            });
        }

        @Override
        public void onPlaybackCompleted(SessionPlayer2 player) {
            dispatchRemoteControllerTask(player, new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlaybackCompleted();
                }
            });
        }

        @Override
        public void onAudioAttributesChanged(final SessionPlayer2 player,
                final AudioAttributesCompat attributes) {
            final MediaSession2ImplBase session = getSession();
            if (session == null || session.getPlayer() != player || player == null) {
                return;
            }
            PlaybackInfo newInfo = session.createPlaybackInfo(player, attributes);
            PlaybackInfo oldInfo;
            synchronized (session.mLock) {
                oldInfo = session.mPlaybackInfo;
                session.mPlaybackInfo = newInfo;
            }
            if (!ObjectsCompat.equals(newInfo, oldInfo)) {
                session.notifyPlaybackInfoChangedNotLocked(newInfo);
            }
        }

        private MediaSession2ImplBase getSession() {
            final MediaSession2ImplBase session = mSession.get();
            if (session == null && DEBUG) {
                Log.d(TAG, "Session is closed", new IllegalStateException());
            }
            return session;
        }

        private void dispatchRemoteControllerTask(@NonNull SessionPlayer2 player,
                @NonNull RemoteControllerCallbackTask task) {
            final MediaSession2ImplBase session = getSession();
            if (session == null || session.getPlayer() != player || player == null) {
                return;
            }
            session.dispatchRemoteControllerCallbackTask(task);
        }

        private void updateDurationIfNeeded(@NonNull final SessionPlayer2 player,
                @Nullable final MediaItem2 item) {
            if (item == null) {
                return;
            }
            if (!item.equals(player.getCurrentMediaItem())) {
                return;
            }
            final long duration = player.getDuration();
            if (duration <= 0 || duration == UNKNOWN_TIME) {
                return;
            }

            MediaMetadata2 metadata = item.getMetadata();
            if (metadata != null) {
                if (!metadata.containsKey(MediaMetadata2.METADATA_KEY_DURATION)) {
                    metadata = new MediaMetadata2.Builder(metadata).putLong(
                            MediaMetadata2.METADATA_KEY_DURATION, duration).build();
                } else {
                    long durationFromMetadata =
                            metadata.getLong(MediaMetadata2.METADATA_KEY_DURATION);
                    if (duration == durationFromMetadata) {
                        return;
                    }
                    // Warns developers about the mismatch. Don't log media item here to keep
                    // metadata secure.
                    Log.w(TAG, "duration mismatch for an item."
                            + " duration from player=" + duration
                            + " duration from metadata=" + durationFromMetadata
                            + ". May be a timing issue?");
                    // Trust duration in the metadata set by developer.
                    // In theory, duration may differ if the current item has been
                    // changed before the getDuration(). So it's better not touch
                    // duration set by developer.
                }
            } else {
                metadata = new MediaMetadata2.Builder()
                        .putLong(MediaMetadata2.METADATA_KEY_DURATION, duration)
                        .putString(MediaMetadata2.METADATA_KEY_MEDIA_ID,
                                item.getMediaId())
                        .putLong(MediaMetadata2.METADATA_KEY_BROWSABLE,
                                MediaMetadata2.BROWSABLE_TYPE_NONE)
                        .putLong(MediaMetadata2.METADATA_KEY_PLAYABLE, 1)
                        .build();
            }
            if (metadata != null) {
                item.setMetadata(metadata);
                dispatchRemoteControllerTask(player, new RemoteControllerCallbackTask() {
                    @Override
                    public void run(ControllerCb callback) throws RemoteException {
                        callback.onPlaylistChanged(
                                player.getPlaylist(), player.getPlaylistMetadata());
                    }
                });
            }
        }
    }

    static class CurrentMediaItemListener implements MediaItem2.OnMetadataChangedListener {
        private final WeakReference<MediaSession2ImplBase> mSession;

        CurrentMediaItemListener(MediaSession2ImplBase session) {
            mSession = new WeakReference<>(session);
        }

        @Override
        public void onMetadataChanged(final MediaItem2 item) {
            final MediaSession2ImplBase session = mSession.get();
            if (session == null || item == null) {
                return;
            }
            final MediaItem2 currentItem = session.getCurrentMediaItem();
            if (currentItem != null && item.equals(currentItem)) {
                session.dispatchRemoteControllerCallbackTask(new RemoteControllerCallbackTask() {
                    @Override
                    public void run(ControllerCb callback) throws RemoteException {
                        callback.onCurrentMediaItemChanged(item);
                    }
                });
            }
        }
    }
    static class PlaylistItemListener implements MediaItem2.OnMetadataChangedListener {
        private final WeakReference<MediaSession2ImplBase> mSession;

        PlaylistItemListener(MediaSession2ImplBase session) {
            mSession = new WeakReference<>(session);
        }

        @Override
        public void onMetadataChanged(final MediaItem2 item) {
            final MediaSession2ImplBase session = mSession.get();
            if (session == null || item == null) {
                return;
            }
            final List<MediaItem2> list = session.getPlaylist();
            if (list == null) {
                return;
            }
            for (int i = 0; i < list.size(); i++) {
                if (item.equals(list.get(i))) {
                    session.dispatchRemoteControllerCallbackTask(
                            new RemoteControllerCallbackTask() {
                                @Override
                                public void run(ControllerCb callback) throws RemoteException {
                                    callback.onPlaylistChanged(list, session.getPlaylistMetadata());
                                }
                            });
                    return;
                }
            }
        }
    }
}
