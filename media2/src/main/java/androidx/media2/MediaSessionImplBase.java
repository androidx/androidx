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

import static androidx.media2.BaseResult.RESULT_CODE_BAD_VALUE;
import static androidx.media2.MediaMetadata.BROWSABLE_TYPE_NONE;
import static androidx.media2.MediaMetadata.METADATA_KEY_BROWSABLE;
import static androidx.media2.MediaMetadata.METADATA_KEY_DURATION;
import static androidx.media2.MediaMetadata.METADATA_KEY_MEDIA_ID;
import static androidx.media2.MediaMetadata.METADATA_KEY_PLAYABLE;
import static androidx.media2.MediaSession.ControllerCb;
import static androidx.media2.MediaSession.ControllerInfo;
import static androidx.media2.MediaSession.SessionCallback;
import static androidx.media2.MediaSession.SessionResult.RESULT_CODE_DISCONNECTED;
import static androidx.media2.MediaSession.SessionResult.RESULT_CODE_INVALID_STATE;
import static androidx.media2.MediaSession.SessionResult.RESULT_CODE_SKIPPED;
import static androidx.media2.MediaSession.SessionResult.RESULT_CODE_SUCCESS;
import static androidx.media2.MediaSession.SessionResult.RESULT_CODE_UNKNOWN_ERROR;
import static androidx.media2.MediaUtils.DIRECT_EXECUTOR;
import static androidx.media2.SessionPlayer.PLAYER_STATE_IDLE;
import static androidx.media2.SessionPlayer.UNKNOWN_TIME;
import static androidx.media2.SessionToken.TYPE_SESSION;

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
import androidx.concurrent.futures.AbstractResolvableFuture;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.ObjectsCompat;
import androidx.media.AudioAttributesCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.VolumeProviderCompat;
import androidx.media2.MediaController.PlaybackInfo;
import androidx.media2.MediaSession.MediaSessionImpl;
import androidx.media2.MediaSession.SessionResult;
import androidx.media2.SequencedFutureManager.SequencedFuture;
import androidx.media2.SessionPlayer.PlayerResult;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

class MediaSessionImplBase implements MediaSessionImpl {
    private static final String DEFAULT_MEDIA_SESSION_TAG_PREFIX = "androidx.media2.session.id";
    private static final String DEFAULT_MEDIA_SESSION_TAG_DELIM = ".";

    static final String TAG = "MSImplBase";
    static final boolean DEBUG = true; //Log.isLoggable(TAG, Log.DEBUG);

    // Note: This checks the uniqueness of a session ID only in single process.
    // When the framework becomes able to check the uniqueness, this logic should be removed.
    @GuardedBy("MediaSessionImplBase.class")
    private static final List<String> SESSION_ID_LIST = new ArrayList<>();

    private static final SessionResult RESULT_WHEN_CLOSED = new SessionResult(RESULT_CODE_SKIPPED);

    private final Context mContext;
    private final HandlerThread mHandlerThread;
    private final Handler mHandler;
    private final MediaSessionCompat mSessionCompat;
    private final MediaSessionStub mSessionStub;
    private final MediaSessionLegacyStub mSessionLegacyStub;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Executor mCallbackExecutor;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final SessionCallback mCallback;
    private final String mSessionId;
    private final SessionToken mSessionToken;
    private final AudioManager mAudioManager;
    private final SessionPlayer.PlayerCallback mPlayerCallback;
    private final MediaSession mInstance;
    private final PendingIntent mSessionActivity;

    final Object mLock = new Object();

    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    PlaybackInfo mPlaybackInfo;

    @GuardedBy("mLock")
    private SessionPlayer mPlayer;
    @GuardedBy("mLock")
    private MediaBrowserServiceCompat mBrowserServiceLegacyStub;

    MediaSessionImplBase(MediaSession instance, Context context, String id, SessionPlayer player,
            PendingIntent sessionActivity, Executor callbackExecutor, SessionCallback callback) {
        mContext = context;
        mInstance = instance;
        mHandlerThread = new HandlerThread("MediaSession_Thread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        mSessionStub = new MediaSessionStub(this);
        mSessionActivity = sessionActivity;

        mCallback = callback;
        mCallbackExecutor = callbackExecutor;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        mPlayerCallback = new SessionPlayerCallback(this);

        synchronized (MediaSessionImplBase.class) {
            if (SESSION_ID_LIST.contains(id)) {
                throw new IllegalArgumentException("Session ID must be unique. ID=" + id);
            }
            SESSION_ID_LIST.add(id);
        }
        mSessionId = id;
        mSessionToken = new SessionToken(new SessionTokenImplBase(Process.myUid(),
                TYPE_SESSION, context.getPackageName(), mSessionStub));
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
    public void updatePlayer(@NonNull SessionPlayer player,
            @Nullable SessionPlayer playlistAgent) {
        // No-op
    }

    // TODO(jaewan): Remove SuppressLint when removing duplication session callback.
    @Override
    @SuppressLint("WrongConstant")
    public void updatePlayer(@NonNull SessionPlayer player) {
        final boolean isPlaybackInfoChanged;

        final SessionPlayer oldPlayer;
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

        if (player instanceof RemoteSessionPlayer) {
            final RemoteSessionPlayer remotePlayer = (RemoteSessionPlayer) player;
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

    @NonNull PlaybackInfo createPlaybackInfo(@NonNull SessionPlayer player,
            AudioAttributesCompat audioAttributes) {
        final AudioAttributesCompat attrs = audioAttributes != null ? audioAttributes :
                player.getAudioAttributes();

        if (!(player instanceof RemoteSessionPlayer)) {
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
            RemoteSessionPlayer remotePlayer = (RemoteSessionPlayer) player;
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
            synchronized (MediaSessionImplBase.class) {
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
    public @NonNull SessionPlayer getPlayer() {
        synchronized (mLock) {
            return mPlayer;
        }
    }

    @Override
    public String getId() {
        return mSessionId;
    }

    @Override
    public @NonNull SessionToken getToken() {
        return mSessionToken;
    }

    @Override
    public @NonNull List<ControllerInfo> getConnectedControllers() {
        List<ControllerInfo> controllers = new ArrayList<>();
        controllers.addAll(mSessionStub.getConnectedControllersManager()
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
        return mSessionStub.getConnectedControllersManager().isConnected(controller)
                || mSessionLegacyStub.getConnectedControllersManager().isConnected(controller);
    }

    @Override
    public ListenableFuture<SessionResult> setCustomLayout(@NonNull ControllerInfo controller,
            @NonNull final List<MediaSession.CommandButton> layout) {
        return dispatchRemoteControllerTask(controller, new RemoteControllerTask() {
            @Override
            public void run(ControllerCb controller, int seq) throws RemoteException {
                controller.setCustomLayout(seq, layout);
            }
        });
    }

    @Override
    public void setAllowedCommands(@NonNull ControllerInfo controller,
            @NonNull final SessionCommandGroup commands) {
        if (mSessionStub.getConnectedControllersManager().isConnected(controller)) {
            mSessionStub.getConnectedControllersManager()
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
    public void broadcastCustomCommand(@NonNull final SessionCommand command,
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
            @NonNull ControllerInfo controller, @NonNull final SessionCommand command,
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
            public ListenableFuture<PlayerResult> run(SessionPlayer player) throws Exception {
                if (player.getPlayerState() != PLAYER_STATE_IDLE) {
                    return player.play();
                }
                final ListenableFuture<PlayerResult> prepareFuture = player.prepare();
                final ListenableFuture<PlayerResult> playFuture = player.play();
                if (prepareFuture == null || playFuture == null) {
                    // Let dispatchPlayerTask() handle such cases.
                    return null;
                }
                return CombinedCommandResultFuture.create(
                        DIRECT_EXECUTOR, prepareFuture, playFuture);
            }
        });
    }

    @Override
    public ListenableFuture<PlayerResult> pause() {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer player) throws Exception {
                return player.pause();
            }
        });
    }

    @Override
    public ListenableFuture<PlayerResult> prepare() {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer player) throws Exception {
                return player.prepare();
            }
        });
    }

    @Override
    public ListenableFuture<PlayerResult> seekTo(final long pos) {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer player) throws Exception {
                return player.seekTo(pos);
            }
        });
    }

    @Override public @SessionPlayer.PlayerState int getPlayerState() {
        return dispatchPlayerTask(new PlayerTask<Integer>() {
            @Override
            public Integer run(SessionPlayer player) throws Exception {
                return player.getPlayerState();
            }
        }, SessionPlayer.PLAYER_STATE_ERROR);
    }

    @Override
    public long getCurrentPosition() {
        return dispatchPlayerTask(new PlayerTask<Long>() {
            @Override
            public Long run(SessionPlayer player) throws Exception {
                if (isInPlaybackState(player)) {
                    return player.getCurrentPosition();
                }
                return null;
            }
        }, SessionPlayer.UNKNOWN_TIME);
    }

    @Override
    public long getDuration() {
        return dispatchPlayerTask(new PlayerTask<Long>() {
            @Override
            public Long run(SessionPlayer player) throws Exception {
                if (isInPlaybackState(player)) {
                    return player.getDuration();
                }
                return null;
            }
        }, SessionPlayer.UNKNOWN_TIME);
    }

    @Override
    public long getBufferedPosition() {
        return dispatchPlayerTask(new PlayerTask<Long>() {
            @Override
            public Long run(SessionPlayer player) throws Exception {
                if (isInPlaybackState(player)) {
                    return player.getBufferedPosition();
                }
                return null;
            }
        }, SessionPlayer.UNKNOWN_TIME);
    }

    @Override
    public @SessionPlayer.BuffState int getBufferingState() {
        return dispatchPlayerTask(new PlayerTask<Integer>() {
            @Override
            public Integer run(SessionPlayer player) throws Exception {
                return player.getBufferingState();
            }
        }, SessionPlayer.BUFFERING_STATE_UNKNOWN);
    }

    @Override
    public float getPlaybackSpeed() {
        return dispatchPlayerTask(new PlayerTask<Float>() {
            @Override
            public Float run(SessionPlayer player) throws Exception {
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
            public ListenableFuture<PlayerResult> run(SessionPlayer player) throws Exception {
                return player.setPlaybackSpeed(speed);
            }
        });
    }

    @Override
    public List<MediaItem> getPlaylist() {
        return dispatchPlayerTask(new PlayerTask<List<MediaItem>>() {
            @Override
            public List<MediaItem> run(SessionPlayer player) throws Exception {
                return player.getPlaylist();
            }
        }, null);
    }

    @Override
    public ListenableFuture<PlayerResult> setPlaylist(final @NonNull List<MediaItem> list,
            final @Nullable MediaMetadata metadata) {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer player) throws Exception {
                if (list == null) {
                    throw new IllegalArgumentException("list shouldn't be null");
                }
                return player.setPlaylist(list, metadata);
            }
        });
    }

    @Override
    public ListenableFuture<PlayerResult> setMediaItem(final @NonNull MediaItem item) {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer player) throws Exception {
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
            public ListenableFuture<PlayerResult> run(SessionPlayer player) throws Exception {
                if (index < 0) {
                    throw new IllegalArgumentException("index shouldn't be negative");
                }
                final List<MediaItem> list = player.getPlaylist();
                if (index >= list.size()) {
                    return PlayerResult.createFuture(RESULT_CODE_BAD_VALUE);
                }
                return player.skipToPlaylistItem(index);
            }
        });
    }

    @Override
    public ListenableFuture<PlayerResult> skipToPreviousItem() {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer player) throws Exception {
                return player.skipToPreviousPlaylistItem();
            }
        });
    }

    @Override
    public ListenableFuture<PlayerResult> skipToNextItem() {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer player) throws Exception {
                return player.skipToNextPlaylistItem();
            }
        });
    }

    @Override
    public MediaMetadata getPlaylistMetadata() {
        return dispatchPlayerTask(new PlayerTask<MediaMetadata>() {
            @Override
            public MediaMetadata run(SessionPlayer player) throws Exception {
                return player.getPlaylistMetadata();
            }
        }, null);
    }

    @Override
    public ListenableFuture<PlayerResult> addPlaylistItem(final int index,
            final @NonNull MediaItem item) {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer player) throws Exception {
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
            public ListenableFuture<PlayerResult> run(SessionPlayer player) throws Exception {
                if (index < 0) {
                    throw new IllegalArgumentException("index shouldn't be negative");
                }
                final List<MediaItem> list = player.getPlaylist();
                if (index >= list.size()) {
                    return PlayerResult.createFuture(RESULT_CODE_BAD_VALUE);
                }
                return player.removePlaylistItem(index);
            }
        });
    }

    @Override
    public ListenableFuture<PlayerResult> replacePlaylistItem(final int index,
            final @NonNull MediaItem item) {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer player) throws Exception {
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
    public MediaItem getCurrentMediaItem() {
        return dispatchPlayerTask(new PlayerTask<MediaItem>() {
            @Override
            public MediaItem run(SessionPlayer player) throws Exception {
                return player.getCurrentMediaItem();
            }
        }, null);
    }

    @Override
    public ListenableFuture<PlayerResult> updatePlaylistMetadata(
            final @Nullable MediaMetadata metadata) {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer player) throws Exception {
                return player.updatePlaylistMetadata(metadata);
            }
        });
    }

    @Override
    public @SessionPlayer.RepeatMode int getRepeatMode() {
        return dispatchPlayerTask(new PlayerTask<Integer>() {
            @Override
            public Integer run(SessionPlayer player) throws Exception {
                return player.getRepeatMode();
            }
        }, SessionPlayer.REPEAT_MODE_NONE);
    }

    @Override
    public ListenableFuture<PlayerResult> setRepeatMode(
            final @SessionPlayer.RepeatMode int repeatMode) {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer player) throws Exception {
                return player.setRepeatMode(repeatMode);
            }
        });
    }

    @Override
    public @SessionPlayer.ShuffleMode int getShuffleMode() {
        return dispatchPlayerTask(new PlayerTask<Integer>() {
            @Override
            public Integer run(SessionPlayer player) throws Exception {
                return player.getShuffleMode();
            }
        }, SessionPlayer.SHUFFLE_MODE_NONE);
    }

    @Override
    public ListenableFuture<PlayerResult> setShuffleMode(
            final @SessionPlayer.ShuffleMode int shuffleMode) {
        return dispatchPlayerTask(new PlayerTask<ListenableFuture<PlayerResult>>() {
            @Override
            public ListenableFuture<PlayerResult> run(SessionPlayer player) throws Exception {
                return player.setShuffleMode(shuffleMode);
            }
        });
    }

    ///////////////////////////////////////////////////
    // package private and private methods
    ///////////////////////////////////////////////////
    @Override
    public @NonNull MediaSession getInstance() {
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
            int state = MediaUtils.convertToPlaybackStateCompatState(getPlayerState(),
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

    MediaBrowserServiceCompat createLegacyBrowserService(Context context, SessionToken token,
            Token sessionToken) {
        return new MediaSessionServiceLegacyStub(context, this, sessionToken);
    }

    @Override
    public void connectFromService(IMediaController caller, String packageName, int pid, int uid) {
        mSessionStub.connect(caller, packageName, pid, uid);
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
    boolean isInPlaybackState(@NonNull SessionPlayer player) {
        return !isClosed()
                && player.getPlayerState() != SessionPlayer.PLAYER_STATE_IDLE
                && player.getPlayerState() != SessionPlayer.PLAYER_STATE_ERROR;
    }

    private @Nullable MediaItem getCurrentMediaItemOrNull() {
        final SessionPlayer player;
        synchronized (mLock) {
            player = mPlayer;
        }
        return player != null ? player.getCurrentMediaItem() : null;
    }

    private @Nullable List<MediaItem> getPlaylistOrNull() {
        final SessionPlayer player;
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
        final SessionPlayer player;
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
    private void notifyPlayerUpdatedNotLocked(SessionPlayer oldPlayer) {
        // Tells the playlist change first, to current item can change be notified with an item
        // within the playlist.
        List<MediaItem> oldPlaylist = oldPlayer.getPlaylist();
        final List<MediaItem> newPlaylist = getPlaylistOrNull();
        if (!ObjectsCompat.equals(oldPlaylist, newPlaylist)) {
            dispatchRemoteControllerCallbackTask(new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlaylistChanged(
                            newPlaylist, getPlaylistMetadata());
                }
            });
        } else {
            MediaMetadata oldMetadata = oldPlayer.getPlaylistMetadata();
            final MediaMetadata newMetadata = getPlaylistMetadata();
            if (!ObjectsCompat.equals(oldMetadata, newMetadata)) {
                dispatchRemoteControllerCallbackTask(new RemoteControllerCallbackTask() {
                    @Override
                    public void run(ControllerCb callback) throws RemoteException {
                        callback.onPlaylistMetadataChanged(newMetadata);
                    }
                });
            }
        }
        MediaItem oldCurrentItem = oldPlayer.getCurrentMediaItem();
        final MediaItem newCurrentItem = getCurrentMediaItemOrNull();
        if (!ObjectsCompat.equals(oldCurrentItem, newCurrentItem)) {
            dispatchRemoteControllerCallbackTask(new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onCurrentMediaItemChanged(newCurrentItem);
                }
            });
        }
        final @SessionPlayer.RepeatMode int repeatMode = getRepeatMode();
        if (oldPlayer.getRepeatMode() != repeatMode) {
            dispatchRemoteControllerCallbackTask(new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onRepeatModeChanged(repeatMode);
                }
            });
        }
        final @SessionPlayer.ShuffleMode int shuffleMode = getShuffleMode();
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
        final MediaItem item = getCurrentMediaItemOrNull();
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
                mSessionStub.getConnectedControllersManager().getConnectedControllers();
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
                    mSessionStub.getConnectedControllersManager()
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
                mSessionStub.getConnectedControllersManager().getConnectedControllers();
        controllers.add(mSessionLegacyStub.getControllersForAll());
        for (int i = 0; i < controllers.size(); i++) {
            ControllerInfo controller = controllers.get(i);
            try {
                final int seq;
                final SequencedFutureManager manager =
                        mSessionStub.getConnectedControllersManager()
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
        // Note: Only removing from MediaSessionStub and ignoring (legacy) stubs would be fine for
        //       now. Because calls to the legacy stubs doesn't throw DeadObjectException.
        mSessionStub.getConnectedControllersManager().removeController(controller);
    }

    ///////////////////////////////////////////////////
    // Inner classes
    ///////////////////////////////////////////////////
    @FunctionalInterface
    interface PlayerTask<T> {
        T run(@NonNull SessionPlayer player) throws Exception;
    }

    @FunctionalInterface
    interface RemoteControllerTask {
        void run(ControllerCb controller, int seq) throws RemoteException;
    }

    @FunctionalInterface
    interface RemoteControllerCallbackTask {
        void run(ControllerCb controller) throws RemoteException;
    }

    private static class SessionPlayerCallback extends SessionPlayer.PlayerCallback {
        private final WeakReference<MediaSessionImplBase> mSession;
        private MediaItem mMediaItem;
        private List<MediaItem> mList;
        private final CurrentMediaItemListener mCurrentItemChangedListener;
        private final PlaylistItemListener mPlaylistItemChangedListener;

        SessionPlayerCallback(MediaSessionImplBase session) {
            mSession = new WeakReference<>(session);
            mCurrentItemChangedListener = new CurrentMediaItemListener(session);
            mPlaylistItemChangedListener = new PlaylistItemListener(session);
        }

        @Override
        public void onCurrentMediaItemChanged(final SessionPlayer player, final MediaItem item) {
            final MediaSessionImplBase session = getSession();
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
        public void onPlayerStateChanged(final SessionPlayer player, final int state) {
            final MediaSessionImplBase session = getSession();
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
        public void onBufferingStateChanged(final SessionPlayer player,
                final MediaItem item, final int state) {
            updateDurationIfNeeded(player, item);
            dispatchRemoteControllerTask(player, new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onBufferingStateChanged(item, state, player.getBufferedPosition());
                }
            });
        }

        @Override
        public void onPlaybackSpeedChanged(final SessionPlayer player, final float speed) {
            dispatchRemoteControllerTask(player, new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlaybackSpeedChanged(SystemClock.elapsedRealtime(),
                            player.getCurrentPosition(), speed);
                }
            });
        }

        @Override
        public void onSeekCompleted(final SessionPlayer player, final long position) {
            dispatchRemoteControllerTask(player, new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onSeekCompleted(SystemClock.elapsedRealtime(),
                            player.getCurrentPosition(), position);
                }
            });
        }

        @Override
        public void onPlaylistChanged(final SessionPlayer player, final List<MediaItem> list,
                final MediaMetadata metadata) {
            final MediaSessionImplBase session = getSession();
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
        public void onPlaylistMetadataChanged(final SessionPlayer player,
                final MediaMetadata metadata) {
            dispatchRemoteControllerTask(player, new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlaylistMetadataChanged(metadata);
                }
            });
        }

        @Override
        public void onRepeatModeChanged(final SessionPlayer player, final int repeatMode) {
            dispatchRemoteControllerTask(player, new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onRepeatModeChanged(repeatMode);
                }
            });
        }

        @Override
        public void onShuffleModeChanged(final SessionPlayer player, final int shuffleMode) {
            dispatchRemoteControllerTask(player, new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onShuffleModeChanged(shuffleMode);
                }
            });
        }

        @Override
        public void onPlaybackCompleted(SessionPlayer player) {
            dispatchRemoteControllerTask(player, new RemoteControllerCallbackTask() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlaybackCompleted();
                }
            });
        }

        @Override
        public void onAudioAttributesChanged(final SessionPlayer player,
                final AudioAttributesCompat attributes) {
            final MediaSessionImplBase session = getSession();
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

        private MediaSessionImplBase getSession() {
            final MediaSessionImplBase session = mSession.get();
            if (session == null && DEBUG) {
                Log.d(TAG, "Session is closed", new IllegalStateException());
            }
            return session;
        }

        private void dispatchRemoteControllerTask(@NonNull SessionPlayer player,
                @NonNull RemoteControllerCallbackTask task) {
            final MediaSessionImplBase session = getSession();
            if (session == null || session.getPlayer() != player || player == null) {
                return;
            }
            session.dispatchRemoteControllerCallbackTask(task);
        }

        private void updateDurationIfNeeded(@NonNull final SessionPlayer player,
                @Nullable final MediaItem item) {
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

            MediaMetadata metadata = item.getMetadata();
            if (metadata != null) {
                if (!metadata.containsKey(METADATA_KEY_DURATION)) {
                    metadata = new MediaMetadata.Builder(metadata).putLong(
                            METADATA_KEY_DURATION, duration).build();
                } else {
                    long durationFromMetadata =
                            metadata.getLong(METADATA_KEY_DURATION);
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
                metadata = new MediaMetadata.Builder()
                        .putLong(METADATA_KEY_DURATION, duration)
                        .putString(METADATA_KEY_MEDIA_ID, item.getMediaId())
                        .putLong(METADATA_KEY_BROWSABLE, BROWSABLE_TYPE_NONE)
                        .putLong(METADATA_KEY_PLAYABLE, 1)
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

    static class CurrentMediaItemListener implements MediaItem.OnMetadataChangedListener {
        private final WeakReference<MediaSessionImplBase> mSession;

        CurrentMediaItemListener(MediaSessionImplBase session) {
            mSession = new WeakReference<>(session);
        }

        @Override
        public void onMetadataChanged(final MediaItem item) {
            final MediaSessionImplBase session = mSession.get();
            if (session == null || item == null) {
                return;
            }
            final MediaItem currentItem = session.getCurrentMediaItem();
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

    static class PlaylistItemListener implements MediaItem.OnMetadataChangedListener {
        private final WeakReference<MediaSessionImplBase> mSession;

        PlaylistItemListener(MediaSessionImplBase session) {
            mSession = new WeakReference<>(session);
        }

        @Override
        public void onMetadataChanged(final MediaItem item) {
            final MediaSessionImplBase session = mSession.get();
            if (session == null || item == null) {
                return;
            }
            final List<MediaItem> list = session.getPlaylist();
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

    static final class CombinedCommandResultFuture<T extends BaseResult>
            extends AbstractResolvableFuture<T> {
        final ListenableFuture<T>[] mFutures;
        AtomicInteger mSuccessCount = new AtomicInteger(0);

        public static <U extends BaseResult> CombinedCommandResultFuture create(
                Executor executor, ListenableFuture<U>... futures) {
            return new CombinedCommandResultFuture<U>(executor, futures);
        }

        private CombinedCommandResultFuture(Executor executor,
                ListenableFuture<T>[] futures) {
            mFutures = futures;
            for (int i = 0; i < mFutures.length; ++i) {
                final int cur = i;
                mFutures[i].addListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            T result = mFutures[cur].get();
                            int resultCode = result.getResultCode();
                            if (resultCode != RESULT_CODE_SUCCESS
                                    && resultCode != RESULT_CODE_SKIPPED) {
                                for (int j = 0; j < mFutures.length; ++j) {
                                    if (!mFutures[j].isCancelled() && !mFutures[j].isDone()
                                            && cur != j) {
                                        mFutures[j].cancel(true);
                                    }
                                }
                                set(result);
                            } else {
                                int cnt = mSuccessCount.incrementAndGet();
                                if (cnt == mFutures.length) {
                                    set(result);
                                }
                            }
                        } catch (Exception e) {
                            for (int j = 0; j < mFutures.length; ++j) {
                                if (!mFutures[j].isCancelled() && !mFutures[j].isDone()
                                        && cur != j) {
                                    mFutures[j].cancel(true);
                                }
                            }
                            setException(e);
                        }
                    }
                }, executor);
            }
        }
    }
}
