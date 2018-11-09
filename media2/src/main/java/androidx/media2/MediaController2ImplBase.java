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

import static androidx.media2.MediaController2.ControllerResult.RESULT_CODE_DISCONNECTED;
import static androidx.media2.MediaController2.ControllerResult.RESULT_CODE_PERMISSION_DENIED;
import static androidx.media2.MediaController2.ControllerResult.RESULT_CODE_SKIPPED;
import static androidx.media2.MediaController2.ControllerResult.RESULT_CODE_UNKNOWN_ERROR;
import static androidx.media2.MediaMetadata2.METADATA_KEY_DURATION;
import static androidx.media2.SessionCommand2.COMMAND_CODE_CUSTOM;
import static androidx.media2.SessionCommand2.COMMAND_CODE_PLAYER_ADD_PLAYLIST_ITEM;
import static androidx.media2.SessionCommand2.COMMAND_CODE_PLAYER_PAUSE;
import static androidx.media2.SessionCommand2.COMMAND_CODE_PLAYER_PLAY;
import static androidx.media2.SessionCommand2.COMMAND_CODE_PLAYER_PREPARE;
import static androidx.media2.SessionCommand2.COMMAND_CODE_PLAYER_REMOVE_PLAYLIST_ITEM;
import static androidx.media2.SessionCommand2.COMMAND_CODE_PLAYER_REPLACE_PLAYLIST_ITEM;
import static androidx.media2.SessionCommand2.COMMAND_CODE_PLAYER_SEEK_TO;
import static androidx.media2.SessionCommand2.COMMAND_CODE_PLAYER_SET_MEDIA_ITEM;
import static androidx.media2.SessionCommand2.COMMAND_CODE_PLAYER_SET_PLAYLIST;
import static androidx.media2.SessionCommand2.COMMAND_CODE_PLAYER_SET_REPEAT_MODE;
import static androidx.media2.SessionCommand2.COMMAND_CODE_PLAYER_SET_SHUFFLE_MODE;
import static androidx.media2.SessionCommand2.COMMAND_CODE_PLAYER_SET_SPEED;
import static androidx.media2.SessionCommand2.COMMAND_CODE_PLAYER_SKIP_TO_NEXT_PLAYLIST_ITEM;
import static androidx.media2.SessionCommand2.COMMAND_CODE_PLAYER_SKIP_TO_PLAYLIST_ITEM;
import static androidx.media2.SessionCommand2.COMMAND_CODE_PLAYER_SKIP_TO_PREVIOUS_PLAYLIST_ITEM;
import static androidx.media2.SessionCommand2.COMMAND_CODE_PLAYER_UPDATE_LIST_METADATA;
import static androidx.media2.SessionCommand2.COMMAND_CODE_SESSION_FAST_FORWARD;
import static androidx.media2.SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID;
import static androidx.media2.SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_SEARCH;
import static androidx.media2.SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_URI;
import static androidx.media2.SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID;
import static androidx.media2.SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH;
import static androidx.media2.SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_URI;
import static androidx.media2.SessionCommand2.COMMAND_CODE_SESSION_REWIND;
import static androidx.media2.SessionCommand2.COMMAND_CODE_SESSION_SELECT_ROUTE;
import static androidx.media2.SessionCommand2.COMMAND_CODE_SESSION_SET_RATING;
import static androidx.media2.SessionCommand2.COMMAND_CODE_SESSION_SKIP_BACKWARD;
import static androidx.media2.SessionCommand2.COMMAND_CODE_SESSION_SKIP_FORWARD;
import static androidx.media2.SessionCommand2.COMMAND_CODE_SESSION_SUBSCRIBE_ROUTES_INFO;
import static androidx.media2.SessionCommand2.COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO;
import static androidx.media2.SessionCommand2.COMMAND_CODE_VOLUME_ADJUST_VOLUME;
import static androidx.media2.SessionCommand2.COMMAND_CODE_VOLUME_SET_VOLUME;
import static androidx.media2.SessionPlayer2.BUFFERING_STATE_UNKNOWN;
import static androidx.media2.SessionPlayer2.UNKNOWN_TIME;
import static androidx.media2.SessionToken2.TYPE_SESSION;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.media.MediaBrowserCompat;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media2.MediaController2.ControllerCallback;
import androidx.media2.MediaController2.ControllerResult;
import androidx.media2.MediaController2.MediaController2Impl;
import androidx.media2.MediaController2.PlaybackInfo;
import androidx.media2.MediaController2.VolumeDirection;
import androidx.media2.MediaController2.VolumeFlags;
import androidx.media2.SequencedFutureManager.SequencedFuture;
import androidx.media2.SessionCommand2.CommandCode;
import androidx.media2.SessionPlayer2.RepeatMode;
import androidx.media2.SessionPlayer2.ShuffleMode;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.Executor;

class MediaController2ImplBase implements MediaController2Impl {
    private static final boolean THROW_EXCEPTION_FOR_NULL_RESULT = true;
    private static final ControllerResult RESULT_WHEN_CLOSED =
            new ControllerResult(RESULT_CODE_SKIPPED);

    static final String TAG = "MC2ImplBase";
    static final boolean DEBUG = true; //Log.isLoggable(TAG, Log.DEBUG);

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final MediaController2 mInstance;
    private final Context mContext;
    private final Object mLock = new Object();

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final SessionToken2 mToken;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ControllerCallback mCallback;
    private final Executor mCallbackExecutor;
    private final IBinder.DeathRecipient mDeathRecipient;
    final SequencedFutureManager mSequencedFutureManager;
    final MediaController2Stub mControllerStub;

    @GuardedBy("mLock")
    private SessionToken2 mConnectedToken;
    @GuardedBy("mLock")
    private SessionServiceConnection mServiceConnection;
    @GuardedBy("mLock")
    private boolean mIsReleased;
    @GuardedBy("mLock")
    private List<MediaItem2> mPlaylist;
    @GuardedBy("mLock")
    private MediaMetadata2 mPlaylistMetadata;
    @GuardedBy("mLock")
    private @RepeatMode int mRepeatMode;
    @GuardedBy("mLock")
    private @ShuffleMode int mShuffleMode;
    @GuardedBy("mLock")
    private int mPlayerState;
    @GuardedBy("mLock")
    private long mPositionEventTimeMs;
    @GuardedBy("mLock")
    private long mPositionMs;
    @GuardedBy("mLock")
    private float mPlaybackSpeed;
    @GuardedBy("mLock")
    private MediaItem2 mCurrentMediaItem;
    @GuardedBy("mLock")
    private int mBufferingState;
    @GuardedBy("mLock")
    private long mBufferedPositionMs;
    @GuardedBy("mLock")
    private PlaybackInfo mPlaybackInfo;
    @GuardedBy("mLock")
    private PendingIntent mSessionActivity;
    @GuardedBy("mLock")
    private SessionCommandGroup2 mAllowedCommands;

    // Assignment should be used with the lock hold, but should be used without a lock to prevent
    // potential deadlock.
    @GuardedBy("mLock")
    private volatile IMediaSession2 mISession2;

    MediaController2ImplBase(Context context, MediaController2 instance, SessionToken2 token,
            Executor executor, ControllerCallback callback) {
        mInstance = instance;
        if (context == null) {
            throw new IllegalArgumentException("context shouldn't be null");
        }
        if (token == null) {
            throw new IllegalArgumentException("token shouldn't be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback shouldn't be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("executor shouldn't be null");
        }
        mContext = context;
        mSequencedFutureManager = new SequencedFutureManager();
        mControllerStub = new MediaController2Stub(this, mSequencedFutureManager);
        mToken = token;
        mCallback = callback;
        mCallbackExecutor = executor;
        mDeathRecipient = new IBinder.DeathRecipient() {
            @Override
            public void binderDied() {
                mInstance.close();
            }
        };

        IMediaSession2 iSession2 = IMediaSession2.Stub.asInterface((IBinder) mToken.getBinder());
        if (mToken.getType() == SessionToken2.TYPE_SESSION) {
            // Session
            mServiceConnection = null;
            connectToSession();
        } else {
            mServiceConnection = new SessionServiceConnection();
            connectToService();
        }
    }

    @Override
    public void close() {
        if (DEBUG) {
            Log.d(TAG, "release from " + mToken);
        }
        final IMediaSession2 iSession2;
        synchronized (mLock) {
            iSession2 = mISession2;
            if (mIsReleased) {
                // Prevent re-enterance from the ControllerCallback.onDisconnected()
                return;
            }
            mIsReleased = true;
            if (mServiceConnection != null) {
                mContext.unbindService(mServiceConnection);
                mServiceConnection = null;
            }
            mISession2 = null;
            mControllerStub.destroy();
        }
        if (iSession2 != null) {
            int seq = mSequencedFutureManager.obtainNextSequenceNumber();
            try {
                iSession2.asBinder().unlinkToDeath(mDeathRecipient, 0);
                iSession2.release(mControllerStub, seq);
            } catch (RemoteException e) {
                // No-op.
            }
        }
        mSequencedFutureManager.close();
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mCallback.onDisconnected(mInstance);
            }
        });
    }

    @Override
    public SessionToken2 getConnectedSessionToken() {
        synchronized (mLock) {
            return isConnected() ? mConnectedToken : null;
        }
    }

    @Override
    public boolean isConnected() {
        synchronized (mLock) {
            return mISession2 != null;
        }
    }

    @FunctionalInterface
    private interface RemoteSessionTask {
        void run(IMediaSession2 iSession2, int seq) throws RemoteException;
    }

    private ListenableFuture<ControllerResult> dispatchRemoteSessionTask(int commandCode,
            RemoteSessionTask task) {
        return dispatchRemoteSessionTaskInternal(commandCode, null, task);
    }

    private ListenableFuture<ControllerResult> dispatchRemoteSessionTask(
            SessionCommand2 sessionCommand, RemoteSessionTask task) {
        return dispatchRemoteSessionTaskInternal(COMMAND_CODE_CUSTOM, sessionCommand, task);
    }

    private ListenableFuture<ControllerResult> dispatchRemoteSessionTaskInternal(int commandCode,
            SessionCommand2 sessionCommand, RemoteSessionTask task) {
        final IMediaSession2 iSession2 = sessionCommand != null
                ? getSessionInterfaceIfAble(sessionCommand)
                : getSessionInterfaceIfAble(commandCode);
        if (iSession2 != null) {
            final SequencedFuture<ControllerResult> result =
                    mSequencedFutureManager.createSequencedFuture(RESULT_WHEN_CLOSED);
            try {
                task.run(iSession2, result.getSequenceNumber());
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
                result.set(new ControllerResult(RESULT_CODE_DISCONNECTED));
            }
            return result;
        } else {
            // Don't create Future with SequencedFutureManager.
            // Otherwise session would receive discontinued sequence number, and it would make
            // future work item 'keeping call sequence when session execute commands' impossible.
            return ControllerResult.createFutureWithResult(RESULT_CODE_PERMISSION_DENIED);
        }
    }

    @Override
    public ListenableFuture<ControllerResult> play() {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_PLAY, new RemoteSessionTask() {
            @Override
            public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                iSession2.play(mControllerStub, seq);
            }
        });
    }

    @Override
    public ListenableFuture<ControllerResult> pause() {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_PAUSE, new RemoteSessionTask() {
            @Override
            public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                iSession2.pause(mControllerStub, seq);
            }
        });
    }

    @Override
    public ListenableFuture<ControllerResult> prepare() {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_PREPARE, new RemoteSessionTask() {
            @Override
            public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                iSession2.prepare(mControllerStub, seq);
            }
        });
    }

    @Override
    public ListenableFuture<ControllerResult> fastForward() {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_FAST_FORWARD,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                        iSession2.fastForward(mControllerStub, seq);
                    }
                });
    }

    @Override
    public ListenableFuture<ControllerResult> rewind() {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_REWIND, new RemoteSessionTask() {
            @Override
            public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                iSession2.rewind(mControllerStub, seq);
            }
        });
    }

    @Override
    public ListenableFuture<ControllerResult> skipForward() {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_SKIP_FORWARD,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                        iSession2.skipForward(mControllerStub, seq);
                    }
                });
    }

    @Override
    public ListenableFuture<ControllerResult> skipBackward() {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_SKIP_BACKWARD,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                        iSession2.skipBackward(mControllerStub, seq);
                    }
                });
    }

    @Override
    public ListenableFuture<ControllerResult> seekTo(final long pos) {
        if (pos < 0) {
            throw new IllegalArgumentException("position shouldn't be negative");
        }
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_SEEK_TO, new RemoteSessionTask() {
            @Override
            public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                iSession2.seekTo(mControllerStub, seq, pos);
            }
        });
    }

    @Override
    public ListenableFuture<ControllerResult> playFromMediaId(final @NonNull String mediaId,
            final @Nullable Bundle extras) {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                        iSession2.playFromMediaId(mControllerStub, seq, mediaId, extras);
                    }
                });
    }

    @Override
    public ListenableFuture<ControllerResult> playFromSearch(final @NonNull String query,
            final @Nullable Bundle extras) {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_PLAY_FROM_SEARCH,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                        iSession2.playFromSearch(mControllerStub, seq, query, extras);
                    }
                });
    }

    @Override
    public ListenableFuture<ControllerResult> playFromUri(final @NonNull Uri uri,
            final @NonNull Bundle extras) {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_PLAY_FROM_URI,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                        iSession2.playFromUri(mControllerStub, seq, uri, extras);
                    }
                });
    }

    @Override
    public ListenableFuture<ControllerResult> prepareFromMediaId(final @NonNull String mediaId,
            final @Nullable Bundle extras) {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                        iSession2.prepareFromMediaId(mControllerStub, seq, mediaId, extras);
                    }
                });
    }

    @Override
    public ListenableFuture<ControllerResult> prepareFromSearch(final @NonNull String query,
            final @Nullable Bundle extras) {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                        iSession2.prepareFromSearch(mControllerStub, seq, query, extras);
                    }
                });
    }

    @Override
    public ListenableFuture<ControllerResult> prepareFromUri(final @NonNull Uri uri,
            final @Nullable Bundle extras) {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_PREPARE_FROM_URI,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                        iSession2.prepareFromUri(mControllerStub, seq, uri, extras);
                    }
                });
    }

    @Override
    public ListenableFuture<ControllerResult> setVolumeTo(final int value,
            final @VolumeFlags int flags) {
        return dispatchRemoteSessionTask(COMMAND_CODE_VOLUME_SET_VOLUME, new RemoteSessionTask() {
            @Override
            public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                iSession2.setVolumeTo(mControllerStub, seq, value, flags);
            }
        });
    }

    @Override
    public ListenableFuture<ControllerResult> adjustVolume(final @VolumeDirection int direction,
            final @VolumeFlags int flags) {
        return dispatchRemoteSessionTask(COMMAND_CODE_VOLUME_ADJUST_VOLUME,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                        iSession2.adjustVolume(mControllerStub, seq, direction, flags);
                    }
                });
    }

    @Override
    public PendingIntent getSessionActivity() {
        synchronized (mLock) {
            return mSessionActivity;
        }
    }

    @Override
    public int getPlayerState() {
        synchronized (mLock) {
            return mPlayerState;
        }
    }

    @Override
    public long getDuration() {
        synchronized (mLock) {
            MediaMetadata2 metadata = mCurrentMediaItem == null ? null
                    : mCurrentMediaItem.getMetadata();
            if (metadata != null && metadata.containsKey(METADATA_KEY_DURATION)) {
                return metadata.getLong(METADATA_KEY_DURATION);
            }
        }
        return SessionPlayer2.UNKNOWN_TIME;
    }

    @Override
    public long getCurrentPosition() {
        synchronized (mLock) {
            if (mISession2 == null) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return UNKNOWN_TIME;
            }
            if (mPlayerState == SessionPlayer2.PLAYER_STATE_PLAYING) {
                long timeDiff = (mInstance.mTimeDiff != null) ? mInstance.mTimeDiff
                        : SystemClock.elapsedRealtime() - mPositionEventTimeMs;
                long expectedPosition = mPositionMs + (long) (mPlaybackSpeed * timeDiff);
                return Math.max(0, expectedPosition);
            }
            return mPositionMs;
        }
    }

    @Override
    public float getPlaybackSpeed() {
        synchronized (mLock) {
            if (mISession2 == null) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return 0f;
            }
            return mPlaybackSpeed;
        }
    }

    @Override
    public ListenableFuture<ControllerResult> setPlaybackSpeed(final float speed) {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_SET_SPEED, new RemoteSessionTask() {
            @Override
            public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                iSession2.setPlaybackSpeed(mControllerStub, seq, speed);
            }
        });
    }

    @Override
    public @SessionPlayer2.BuffState int getBufferingState() {
        synchronized (mLock) {
            if (mISession2 == null) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return BUFFERING_STATE_UNKNOWN;
            }
            return mBufferingState;
        }
    }

    @Override
    public long getBufferedPosition() {
        synchronized (mLock) {
            if (mISession2 == null) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return UNKNOWN_TIME;
            }
            return mBufferedPositionMs;
        }
    }

    @Override
    public PlaybackInfo getPlaybackInfo() {
        synchronized (mLock) {
            return mPlaybackInfo;
        }
    }

    @Override
    public ListenableFuture<ControllerResult> setRating(final @NonNull String mediaId,
            final @NonNull Rating2 rating) {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_SET_RATING, new RemoteSessionTask() {
            @Override
            public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                iSession2.setRating(mControllerStub, seq, mediaId,
                        MediaUtils2.toParcelable(rating));
            }
        });
    }

    @Override
    public ListenableFuture<ControllerResult> sendCustomCommand(
            final @NonNull SessionCommand2 command, final @Nullable Bundle args) {
        return dispatchRemoteSessionTask(command, new RemoteSessionTask() {
            @Override
            public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                iSession2.onCustomCommand(mControllerStub, seq, MediaUtils2.toParcelable(command),
                        args);
            }
        });
    }

    @Override
    public List<MediaItem2> getPlaylist() {
        synchronized (mLock) {
            return mPlaylist;
        }
    }

    @Override
    public ListenableFuture<ControllerResult> setPlaylist(final @NonNull List<String> list,
            final @Nullable MediaMetadata2 metadata) {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_SET_PLAYLIST, new RemoteSessionTask() {
            @Override
            public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                iSession2.setPlaylist(mControllerStub, seq, list,
                        MediaUtils2.toParcelable(metadata));
            }
        });
    }

    @Override
    public ListenableFuture<ControllerResult> setMediaItem(final String mediaId) {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_SET_MEDIA_ITEM,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                        iSession2.setMediaItem(mControllerStub, seq, mediaId);
                    }
                });
    }

    @Override
    public ListenableFuture<ControllerResult> updatePlaylistMetadata(
            final @Nullable MediaMetadata2 metadata) {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_UPDATE_LIST_METADATA,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                        iSession2.updatePlaylistMetadata(mControllerStub, seq,
                                MediaUtils2.toParcelable(metadata));
                    }
                });
    }

    @Override
    public MediaMetadata2 getPlaylistMetadata() {
        synchronized (mLock) {
            return mPlaylistMetadata;
        }
    }

    @Override
    public ListenableFuture<ControllerResult> addPlaylistItem(final int index,
            final @NonNull String mediaId) {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_ADD_PLAYLIST_ITEM,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                        iSession2.addPlaylistItem(mControllerStub, seq, index, mediaId);
                    }
                });
    }

    @Override
    public ListenableFuture<ControllerResult> removePlaylistItem(final @NonNull int index) {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_REMOVE_PLAYLIST_ITEM,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                        iSession2.removePlaylistItem(mControllerStub, seq, index);
                    }
                });
    }

    @Override
    public ListenableFuture<ControllerResult> replacePlaylistItem(final int index,
            final @NonNull String mediaId) {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_REPLACE_PLAYLIST_ITEM,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                        iSession2.replacePlaylistItem(mControllerStub, seq, index, mediaId);
                    }
                });
    }

    @Override
    public MediaItem2 getCurrentMediaItem() {
        synchronized (mLock) {
            return mCurrentMediaItem;
        }
    }

    @Override
    public ListenableFuture<ControllerResult> skipToPreviousItem() {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_SKIP_TO_PREVIOUS_PLAYLIST_ITEM,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                        iSession2.skipToPreviousItem(mControllerStub, seq);
                    }
                });
    }

    @Override
    public ListenableFuture<ControllerResult> skipToNextItem() {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_SKIP_TO_NEXT_PLAYLIST_ITEM,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                        iSession2.skipToNextItem(mControllerStub, seq);
                    }
                });
    }

    @Override
    public ListenableFuture<ControllerResult> skipToPlaylistItem(final @NonNull int index) {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_SKIP_TO_PLAYLIST_ITEM,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                        iSession2.skipToPlaylistItem(mControllerStub, seq, index);
                    }
                });
    }

    @Override
    public int getRepeatMode() {
        synchronized (mLock) {
            return mRepeatMode;
        }
    }

    @Override
    public ListenableFuture<ControllerResult> setRepeatMode(final int repeatMode) {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_SET_REPEAT_MODE,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                        iSession2.setRepeatMode(mControllerStub, seq, repeatMode);
                    }
                });
    }

    @Override
    public int getShuffleMode() {
        synchronized (mLock) {
            return mShuffleMode;
        }
    }

    @Override
    public ListenableFuture<ControllerResult> setShuffleMode(final int shuffleMode) {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_SET_SHUFFLE_MODE,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                        iSession2.setShuffleMode(mControllerStub, seq, shuffleMode);
                    }
                });
    }

    @Override
    public ListenableFuture<ControllerResult> subscribeRoutesInfo() {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_SUBSCRIBE_ROUTES_INFO,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                        iSession2.subscribeRoutesInfo(mControllerStub, seq);
                    }
                });
    }

    @Override
    public ListenableFuture<ControllerResult> unsubscribeRoutesInfo() {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                        iSession2.unsubscribeRoutesInfo(mControllerStub, seq);
                    }
                });
    }

    @Override
    public ListenableFuture<ControllerResult> selectRoute(final @NonNull Bundle route) {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_SELECT_ROUTE,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession2 iSession2, int seq) throws RemoteException {
                        iSession2.selectRoute(mControllerStub, seq, route);
                    }
                });
    }

    @Override
    public @NonNull Context getContext() {
        return mContext;
    }

    @Override
    public @NonNull ControllerCallback getCallback() {
        return mCallback;
    }

    @Override
    public @NonNull Executor getCallbackExecutor() {
        return mCallbackExecutor;
    }

    @Override
    public @Nullable MediaBrowserCompat getBrowserCompat() {
        return null;
    }

    @Override
    public @NonNull MediaController2 getInstance() {
        return mInstance;
    }

    private void connectToService() {
        // Service. Needs to get fresh binder whenever connection is needed.
        final Intent intent = new Intent(MediaSessionService2.SERVICE_INTERFACE);
        intent.setClassName(mToken.getPackageName(), mToken.getServiceName());

        // Use bindService() instead of startForegroundService() to start session service for three
        // reasons.
        // 1. Prevent session service owner's stopSelf() from destroying service.
        //    With the startForegroundService(), service's call of stopSelf() will trigger immediate
        //    onDestroy() calls on the main thread even when onConnect() is running in another
        //    thread.
        // 2. Minimize APIs for developers to take care about.
        //    With bindService(), developers only need to take care about Service.onBind()
        //    but Service.onStartCommand() should be also taken care about with the
        //    startForegroundService().
        // 3. Future support for UI-less playback
        //    If a service wants to keep running, it should be either foreground service or
        //    bound service. But there had been request for the feature for system apps
        //    and using bindService() will be better fit with it.
        synchronized (mLock) {
            boolean result = mContext.bindService(
                    intent, mServiceConnection, Context.BIND_AUTO_CREATE);
            if (!result) {
                Log.w(TAG, "bind to " + mToken + " failed");
            } else if (DEBUG) {
                Log.d(TAG, "bind to " + mToken + " succeeded");
            }
        }
    }

    private void connectToSession() {
        IMediaSession2 iSession2 = IMediaSession2.Stub.asInterface((IBinder) mToken.getBinder());
        int seq = mSequencedFutureManager.obtainNextSequenceNumber();
        try {
            iSession2.connect(mControllerStub, seq, mContext.getPackageName());
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to call connection request. Framework will retry"
                    + " automatically");
        }
    }

    // Returns session interface if the controller can send the command.
    IMediaSession2 getSessionInterfaceIfAble(@CommandCode int commandCode) {
        synchronized (mLock) {
            if (!mAllowedCommands.hasCommand(commandCode)) {
                // Cannot send because isn't allowed to.
                Log.w(TAG, "Controller isn't allowed to call command, commandCode="
                        + commandCode);
                return null;
            }
            return mISession2;
        }
    }

    // Returns session binder if the controller can send the command.
    IMediaSession2 getSessionInterfaceIfAble(SessionCommand2 command) {
        synchronized (mLock) {
            if (!mAllowedCommands.hasCommand(command)) {
                Log.w(TAG, "Controller isn't allowed to call command, command=" + command);
                return null;
            }
            return mISession2;
        }
    }

    void notifyCurrentMediaItemChanged(final MediaItem2 item) {
        synchronized (mLock) {
            mCurrentMediaItem = item;
        }
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (!mInstance.isConnected()) {
                    return;
                }
                mCallback.onCurrentMediaItemChanged(mInstance, item);
            }
        });

    }

    void notifyPlayerStateChanges(long eventTimeMs, long positionMs, final int state) {
        synchronized (mLock) {
            mPositionEventTimeMs = eventTimeMs;
            mPositionMs = positionMs;
            mPlayerState = state;
        }
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (!mInstance.isConnected()) {
                    return;
                }
                mCallback.onPlayerStateChanged(mInstance, state);
            }
        });
    }

    void notifyPlaybackSpeedChanges(long eventTimeMs, long positionMs, final float speed) {
        synchronized (mLock) {
            mPositionEventTimeMs = eventTimeMs;
            mPositionMs = positionMs;
            mPlaybackSpeed = speed;
        }
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (!mInstance.isConnected()) {
                    return;
                }
                mCallback.onPlaybackSpeedChanged(mInstance, speed);
            }
        });
    }

    void notifyBufferingStateChanged(final MediaItem2 item, final int state,
            long bufferedPositionMs) {
        synchronized (mLock) {
            mBufferingState = state;
            mBufferedPositionMs = bufferedPositionMs;
        }
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (!mInstance.isConnected()) {
                    return;
                }
                mCallback.onBufferingStateChanged(mInstance, item, state);
            }
        });
    }

    void notifyPlaylistChanges(final List<MediaItem2> playlist, final MediaMetadata2 metadata) {
        synchronized (mLock) {
            mPlaylist = playlist;
            mPlaylistMetadata = metadata;
        }
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (!mInstance.isConnected()) {
                    return;
                }
                mCallback.onPlaylistChanged(mInstance, playlist, metadata);
            }
        });
    }

    void notifyPlaylistMetadataChanges(final MediaMetadata2 metadata) {
        synchronized (mLock) {
            mPlaylistMetadata = metadata;
        }
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (!mInstance.isConnected()) {
                    return;
                }
                mCallback.onPlaylistMetadataChanged(mInstance, metadata);
            }
        });
    }

    void notifyPlaybackInfoChanges(final PlaybackInfo info) {
        synchronized (mLock) {
            mPlaybackInfo = info;
        }
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (!mInstance.isConnected()) {
                    return;
                }
                mCallback.onPlaybackInfoChanged(mInstance, info);
            }
        });
    }

    void notifyRepeatModeChanges(final int repeatMode) {
        synchronized (mLock) {
            mRepeatMode = repeatMode;
        }
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (!mInstance.isConnected()) {
                    return;
                }
                mCallback.onRepeatModeChanged(mInstance, repeatMode);
            }
        });
    }

    void notifyShuffleModeChanges(final int shuffleMode) {
        synchronized (mLock) {
            mShuffleMode = shuffleMode;
        }
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (!mInstance.isConnected()) {
                    return;
                }
                mCallback.onShuffleModeChanged(mInstance, shuffleMode);
            }
        });
    }

    void notifyPlaybackCompleted() {
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (!mInstance.isConnected()) {
                    return;
                }
                mCallback.onPlaybackCompleted(mInstance);
            }
        });
    }

    void notifySeekCompleted(long eventTimeMs, long positionMs, final long seekPositionMs) {
        synchronized (mLock) {
            mPositionEventTimeMs = eventTimeMs;
            mPositionMs = positionMs;
        }

        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (!mInstance.isConnected()) {
                    return;
                }
                mCallback.onSeekCompleted(mInstance, seekPositionMs);
            }
        });
    }

    void notifyRoutesInfoChanged(final List<Bundle> routes) {
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (!mInstance.isConnected()) {
                    return;
                }
                mCallback.onRoutesInfoChanged(mInstance, routes);
            }
        });
    }

    // Should be used without a lock to prevent potential deadlock.
    void onConnectedNotLocked(IMediaSession2 sessionBinder,
            final SessionCommandGroup2 allowedCommands,
            final int playerState,
            final MediaItem2 currentMediaItem,
            final long positionEventTimeMs,
            final long positionMs,
            final float playbackSpeed,
            final long bufferedPositionMs,
            final PlaybackInfo info,
            final int repeatMode,
            final int shuffleMode,
            final List<MediaItem2> playlist,
            final PendingIntent sessionActivity) {
        if (DEBUG) {
            Log.d(TAG, "onConnectedNotLocked sessionBinder=" + sessionBinder
                    + ", allowedCommands=" + allowedCommands);
        }
        // 'close' is used in try-finally
        boolean close = false;
        try {
            if (sessionBinder == null || allowedCommands == null) {
                // Connection rejected.
                close = true;
                return;
            }
            synchronized (mLock) {
                if (mIsReleased) {
                    return;
                }
                if (mISession2 != null) {
                    Log.e(TAG, "Cannot be notified about the connection result many times."
                            + " Probably a bug or malicious app.");
                    close = true;
                    return;
                }
                mAllowedCommands = allowedCommands;
                mPlayerState = playerState;
                mCurrentMediaItem = currentMediaItem;
                mPositionEventTimeMs = positionEventTimeMs;
                mPositionMs = positionMs;
                mPlaybackSpeed = playbackSpeed;
                mBufferedPositionMs = bufferedPositionMs;
                mPlaybackInfo = info;
                mRepeatMode = repeatMode;
                mShuffleMode = shuffleMode;
                mPlaylist = playlist;
                mSessionActivity = sessionActivity;
                mISession2 = sessionBinder;
                try {
                    // Implementation for the local binder is no-op,
                    // so can be used without worrying about deadlock.
                    mISession2.asBinder().linkToDeath(mDeathRecipient, 0);
                } catch (RemoteException e) {
                    if (DEBUG) {
                        Log.d(TAG, "Session died too early.", e);
                    }
                    close = true;
                    return;
                }
                mConnectedToken = new SessionToken2(new SessionToken2ImplBase(
                        mToken.getUid(), TYPE_SESSION, mToken.getPackageName(), sessionBinder));
            }
            mCallbackExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mCallback.onConnected(mInstance, allowedCommands);
                }
            });
        } finally {
            if (close) {
                // Trick to call release() without holding the lock, to prevent potential deadlock
                // with the developer's custom lock within the ControllerCallback.onDisconnected().
                mInstance.close();
            }
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void sendControllerResult(int seq, @NonNull ControllerResult result) {
        final IMediaSession2 iSession2;
        synchronized (mLock) {
            iSession2 = mISession2;
        }
        if (iSession2 == null) {
            return;
        }
        try {
            iSession2.onControllerResult(mControllerStub, seq,
                    MediaUtils2.toParcelable(result));
        } catch (RemoteException e) {
            Log.w(TAG, "Error in sending");
        }
    }

    void onCustomCommand(final int seq, final SessionCommand2 command, final Bundle args) {
        if (DEBUG) {
            Log.d(TAG, "onCustomCommand cmd=" + command.getCustomCommand());
        }
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                ControllerResult result = mCallback.onCustomCommand(mInstance, command, args);
                if (result == null) {
                    if (THROW_EXCEPTION_FOR_NULL_RESULT) {
                        throw new RuntimeException("ControllerCallback#onCustomCommand() has"
                                + " returned null, command=" + command.getCustomCommand());
                    } else {
                        result = new ControllerResult(RESULT_CODE_UNKNOWN_ERROR);
                    }
                }
                sendControllerResult(seq, result);
            }
        });
    }

    void onAllowedCommandsChanged(final SessionCommandGroup2 commands) {
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mCallback.onAllowedCommandsChanged(mInstance, commands);
            }
        });
    }

    void onSetCustomLayout(final int seq, final List<MediaSession2.CommandButton> layout) {
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                int resultCode = mCallback.onSetCustomLayout(mInstance, layout);
                ControllerResult result = new ControllerResult(resultCode);
                sendControllerResult(seq, result);
            }
        });
    }

    // This will be called on the main thread.
    private class SessionServiceConnection implements ServiceConnection {
        SessionServiceConnection() {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // Note that it's always main-thread.
            if (DEBUG) {
                Log.d(TAG, "onServiceConnected " + name + " " + this);
            }
            // Sanity check
            if (!mToken.getPackageName().equals(name.getPackageName())) {
                Log.wtf(TAG, "Expected connection to " + mToken.getPackageName() + " but is"
                        + " connected to " + name);
                return;
            }
            IMediaSessionService2 iService = IMediaSessionService2.Stub.asInterface(service);
            if (iService == null) {
                Log.wtf(TAG, "Service interface is missing.");
                return;
            }
            try {
                iService.connect(mControllerStub, getContext().getPackageName());
            } catch (RemoteException e) {
                Log.w(TAG, "Service " + name + " has died prematurely");
                mInstance.close();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Temporal lose of the binding because of the service crash. System will automatically
            // rebind, so just no-op.
            if (DEBUG) {
                Log.w(TAG, "Session service " + name + " is disconnected.");
            }
        }

        @Override
        public void onBindingDied(ComponentName name) {
            // Permanent lose of the binding because of the service package update or removed.
            // This SessionServiceRecord will be removed accordingly, but forget session binder here
            // for sure.
            close();
        }
    }
}
