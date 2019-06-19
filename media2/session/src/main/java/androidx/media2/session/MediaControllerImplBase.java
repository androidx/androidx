/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.session;

import static androidx.media2.common.MediaMetadata.METADATA_KEY_DURATION;
import static androidx.media2.common.SessionPlayer.BUFFERING_STATE_UNKNOWN;
import static androidx.media2.common.SessionPlayer.UNKNOWN_TIME;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_CUSTOM;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_ADD_PLAYLIST_ITEM;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_DESELECT_TRACK;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_PAUSE;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_PLAY;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_PREPARE;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_REMOVE_PLAYLIST_ITEM;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_REPLACE_PLAYLIST_ITEM;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_SEEK_TO;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_SELECT_TRACK;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_SET_MEDIA_ITEM;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_SET_PLAYLIST;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_SET_REPEAT_MODE;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_SET_SHUFFLE_MODE;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_SET_SPEED;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_SET_SURFACE;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_SKIP_TO_NEXT_PLAYLIST_ITEM;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_SKIP_TO_PLAYLIST_ITEM;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_SKIP_TO_PREVIOUS_PLAYLIST_ITEM;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_UPDATE_LIST_METADATA;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_FAST_FORWARD;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_PLAY_FROM_SEARCH;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_PLAY_FROM_URI;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_PREPARE_FROM_URI;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_REWIND;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_SET_RATING;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_SKIP_BACKWARD;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_SKIP_FORWARD;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_VOLUME_ADJUST_VOLUME;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_VOLUME_SET_VOLUME;
import static androidx.media2.session.SessionResult.RESULT_ERROR_PERMISSION_DENIED;
import static androidx.media2.session.SessionResult.RESULT_ERROR_SESSION_DISCONNECTED;
import static androidx.media2.session.SessionResult.RESULT_ERROR_UNKNOWN;
import static androidx.media2.session.SessionResult.RESULT_INFO_SKIPPED;
import static androidx.media2.session.SessionToken.TYPE_SESSION;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.media.MediaBrowserCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.MediaParcelUtils;
import androidx.media2.common.Rating;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.SessionPlayer.RepeatMode;
import androidx.media2.common.SessionPlayer.ShuffleMode;
import androidx.media2.common.SessionPlayer.TrackInfo;
import androidx.media2.common.SubtitleData;
import androidx.media2.common.VideoSize;
import androidx.media2.session.MediaController.ControllerCallback;
import androidx.media2.session.MediaController.ControllerCallbackRunnable;
import androidx.media2.session.MediaController.MediaControllerImpl;
import androidx.media2.session.MediaController.PlaybackInfo;
import androidx.media2.session.MediaController.VolumeDirection;
import androidx.media2.session.MediaController.VolumeFlags;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.List;

class MediaControllerImplBase implements MediaControllerImpl {
    private static final boolean THROW_EXCEPTION_FOR_NULL_RESULT = true;
    private static final SessionResult RESULT_WHEN_CLOSED =
            new SessionResult(RESULT_INFO_SKIPPED);

    static final String TAG = "MC2ImplBase";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final MediaController mInstance;
    private final Context mContext;
    private final Object mLock = new Object();

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final SessionToken mToken;
    private final IBinder.DeathRecipient mDeathRecipient;
    final SequencedFutureManager mSequencedFutureManager;
    final MediaControllerStub mControllerStub;

    @GuardedBy("mLock")
    private SessionToken mConnectedToken;
    @GuardedBy("mLock")
    private SessionServiceConnection mServiceConnection;
    @GuardedBy("mLock")
    private boolean mIsReleased;
    @GuardedBy("mLock")
    private List<MediaItem> mPlaylist;
    @GuardedBy("mLock")
    private MediaMetadata mPlaylistMetadata;
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
    private MediaItem mCurrentMediaItem;
    @GuardedBy("mLock")
    private int mCurrentMediaItemIndex = -1;
    @GuardedBy("mLock")
    private int mPreviousMediaItemIndex = -1;
    @GuardedBy("mLock")
    private int mNextMediaItemIndex = -1;
    @GuardedBy("mLock")
    private int mBufferingState;
    @GuardedBy("mLock")
    private long mBufferedPositionMs;
    @GuardedBy("mLock")
    private PlaybackInfo mPlaybackInfo;
    @GuardedBy("mLock")
    private PendingIntent mSessionActivity;
    @GuardedBy("mLock")
    private SessionCommandGroup mAllowedCommands;
    @GuardedBy("mLock")
    private VideoSize mVideoSize = new VideoSize(0, 0);
    @GuardedBy("mLock")
    private List<TrackInfo> mTrackInfos = Collections.emptyList();
    @GuardedBy("mLock")
    private SparseArray<TrackInfo> mSelectedTracks = new SparseArray<>();

    // Assignment should be used with the lock hold, but should be used without a lock to prevent
    // potential deadlock.
    @GuardedBy("mLock")
    private volatile IMediaSession mISession;

    MediaControllerImplBase(Context context, MediaController instance, SessionToken token,
            @Nullable Bundle connectionHints) {
        mInstance = instance;
        if (context == null) {
            throw new NullPointerException("context shouldn't be null");
        }
        if (token == null) {
            throw new NullPointerException("token shouldn't be null");
        }
        mContext = context;
        mSequencedFutureManager = new SequencedFutureManager();
        mControllerStub = new MediaControllerStub(this, mSequencedFutureManager);
        mToken = token;
        mDeathRecipient = new IBinder.DeathRecipient() {
            @Override
            public void binderDied() {
                mInstance.close();
            }
        };

        boolean connectionRequested;
        if (mToken.getType() == TYPE_SESSION) {
            // Session
            mServiceConnection = null;
            connectionRequested = requestConnectToSession(connectionHints);
        } else {
            mServiceConnection = new SessionServiceConnection(connectionHints);
            connectionRequested = requestConnectToService();
        }
        if (!connectionRequested) {
            mInstance.close();
        }
    }

    @Override
    public void close() {
        if (DEBUG) {
            Log.d(TAG, "release from " + mToken);
        }
        final IMediaSession iSession;
        synchronized (mLock) {
            iSession = mISession;
            if (mIsReleased) {
                // Prevent re-enterance from the ControllerCallback.onDisconnected()
                return;
            }
            mIsReleased = true;
            if (mServiceConnection != null) {
                mContext.unbindService(mServiceConnection);
                mServiceConnection = null;
            }
            mISession = null;
            mControllerStub.destroy();
        }
        if (iSession != null) {
            int seq = mSequencedFutureManager.obtainNextSequenceNumber();
            try {
                iSession.asBinder().unlinkToDeath(mDeathRecipient, 0);
                iSession.release(mControllerStub, seq);
            } catch (RemoteException e) {
                // No-op.
            }
        }
        mSequencedFutureManager.close();
        mInstance.notifyControllerCallback(new ControllerCallbackRunnable() {
            @Override
            public void run(@NonNull ControllerCallback callback) {
                callback.onDisconnected(mInstance);
            }
        });
    }

    @Override
    public SessionToken getConnectedToken() {
        synchronized (mLock) {
            return isConnected() ? mConnectedToken : null;
        }
    }

    @Override
    public boolean isConnected() {
        synchronized (mLock) {
            return mISession != null;
        }
    }

    @FunctionalInterface
    private interface RemoteSessionTask {
        void run(IMediaSession iSession, int seq) throws RemoteException;
    }

    private ListenableFuture<SessionResult> dispatchRemoteSessionTask(int commandCode,
            RemoteSessionTask task) {
        return dispatchRemoteSessionTaskInternal(commandCode, null, task);
    }

    private ListenableFuture<SessionResult> dispatchRemoteSessionTask(
            SessionCommand sessionCommand, RemoteSessionTask task) {
        return dispatchRemoteSessionTaskInternal(COMMAND_CODE_CUSTOM, sessionCommand, task);
    }

    private ListenableFuture<SessionResult> dispatchRemoteSessionTaskInternal(int commandCode,
            SessionCommand sessionCommand, RemoteSessionTask task) {
        final IMediaSession iSession = sessionCommand != null
                ? getSessionInterfaceIfAble(sessionCommand)
                : getSessionInterfaceIfAble(commandCode);
        if (iSession != null) {
            final SequencedFutureManager.SequencedFuture<SessionResult> result =
                    mSequencedFutureManager.createSequencedFuture(RESULT_WHEN_CLOSED);
            try {
                task.run(iSession, result.getSequenceNumber());
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
                result.set(new SessionResult(RESULT_ERROR_SESSION_DISCONNECTED));
            }
            return result;
        } else {
            // Don't create Future with SequencedFutureManager.
            // Otherwise session would receive discontinued sequence number, and it would make
            // future work item 'keeping call sequence when session execute commands' impossible.
            return SessionResult.createFutureWithResult(RESULT_ERROR_PERMISSION_DENIED);
        }
    }

    @Override
    public ListenableFuture<SessionResult> play() {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_PLAY, new RemoteSessionTask() {
            @Override
            public void run(IMediaSession iSession, int seq) throws RemoteException {
                iSession.play(mControllerStub, seq);
            }
        });
    }

    @Override
    public ListenableFuture<SessionResult> pause() {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_PAUSE, new RemoteSessionTask() {
            @Override
            public void run(IMediaSession iSession, int seq) throws RemoteException {
                iSession.pause(mControllerStub, seq);
            }
        });
    }

    @Override
    public ListenableFuture<SessionResult> prepare() {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_PREPARE, new RemoteSessionTask() {
            @Override
            public void run(IMediaSession iSession, int seq) throws RemoteException {
                iSession.prepare(mControllerStub, seq);
            }
        });
    }

    @Override
    public ListenableFuture<SessionResult> fastForward() {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_FAST_FORWARD,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.fastForward(mControllerStub, seq);
                    }
                });
    }

    @Override
    public ListenableFuture<SessionResult> rewind() {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_REWIND, new RemoteSessionTask() {
            @Override
            public void run(IMediaSession iSession, int seq) throws RemoteException {
                iSession.rewind(mControllerStub, seq);
            }
        });
    }

    @Override
    public ListenableFuture<SessionResult> skipForward() {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_SKIP_FORWARD,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.skipForward(mControllerStub, seq);
                    }
                });
    }

    @Override
    public ListenableFuture<SessionResult> skipBackward() {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_SKIP_BACKWARD,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.skipBackward(mControllerStub, seq);
                    }
                });
    }

    @Override
    public ListenableFuture<SessionResult> seekTo(final long pos) {
        if (pos < 0) {
            throw new IllegalArgumentException("position shouldn't be negative");
        }
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_SEEK_TO, new RemoteSessionTask() {
            @Override
            public void run(IMediaSession iSession, int seq) throws RemoteException {
                iSession.seekTo(mControllerStub, seq, pos);
            }
        });
    }

    @Override
    public ListenableFuture<SessionResult> playFromMediaId(@NonNull final String mediaId,
            @Nullable final Bundle extras) {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.playFromMediaId(mControllerStub, seq, mediaId, extras);
                    }
                });
    }

    @Override
    public ListenableFuture<SessionResult> playFromSearch(@NonNull final String query,
            @Nullable final Bundle extras) {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_PLAY_FROM_SEARCH,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.playFromSearch(mControllerStub, seq, query, extras);
                    }
                });
    }

    @Override
    public ListenableFuture<SessionResult> playFromUri(@NonNull final Uri uri,
            @Nullable final Bundle extras) {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_PLAY_FROM_URI,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.playFromUri(mControllerStub, seq, uri, extras);
                    }
                });
    }

    @Override
    public ListenableFuture<SessionResult> prepareFromMediaId(@NonNull final String mediaId,
            @Nullable final Bundle extras) {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.prepareFromMediaId(mControllerStub, seq, mediaId, extras);
                    }
                });
    }

    @Override
    public ListenableFuture<SessionResult> prepareFromSearch(@NonNull final String query,
            @Nullable final Bundle extras) {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.prepareFromSearch(mControllerStub, seq, query, extras);
                    }
                });
    }

    @Override
    public ListenableFuture<SessionResult> prepareFromUri(@NonNull final Uri uri,
            @Nullable final Bundle extras) {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_PREPARE_FROM_URI,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.prepareFromUri(mControllerStub, seq, uri, extras);
                    }
                });
    }

    @Override
    public ListenableFuture<SessionResult> setVolumeTo(final int value,
            final @VolumeFlags int flags) {
        return dispatchRemoteSessionTask(COMMAND_CODE_VOLUME_SET_VOLUME, new RemoteSessionTask() {
            @Override
            public void run(IMediaSession iSession, int seq) throws RemoteException {
                iSession.setVolumeTo(mControllerStub, seq, value, flags);
            }
        });
    }

    @Override
    public ListenableFuture<SessionResult> adjustVolume(final @VolumeDirection int direction,
            final @VolumeFlags int flags) {
        return dispatchRemoteSessionTask(COMMAND_CODE_VOLUME_ADJUST_VOLUME,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.adjustVolume(mControllerStub, seq, direction, flags);
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
            MediaMetadata metadata = mCurrentMediaItem == null ? null
                    : mCurrentMediaItem.getMetadata();
            if (metadata != null && metadata.containsKey(METADATA_KEY_DURATION)) {
                return metadata.getLong(METADATA_KEY_DURATION);
            }
        }
        return SessionPlayer.UNKNOWN_TIME;
    }

    @Override
    public long getCurrentPosition() {
        synchronized (mLock) {
            if (mISession == null) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return UNKNOWN_TIME;
            }
            if (mPlayerState == SessionPlayer.PLAYER_STATE_PLAYING
                    && mBufferingState != SessionPlayer.BUFFERING_STATE_BUFFERING_AND_STARVED) {
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
            if (mISession == null) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return 0f;
            }
            return mPlaybackSpeed;
        }
    }

    @Override
    public ListenableFuture<SessionResult> setPlaybackSpeed(final float speed) {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_SET_SPEED, new RemoteSessionTask() {
            @Override
            public void run(IMediaSession iSession, int seq) throws RemoteException {
                iSession.setPlaybackSpeed(mControllerStub, seq, speed);
            }
        });
    }

    @Override
    @SessionPlayer.BuffState
    public int getBufferingState() {
        synchronized (mLock) {
            if (mISession == null) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return BUFFERING_STATE_UNKNOWN;
            }
            return mBufferingState;
        }
    }

    @Override
    public long getBufferedPosition() {
        synchronized (mLock) {
            if (mISession == null) {
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
    public ListenableFuture<SessionResult> setRating(@NonNull final String mediaId,
            @NonNull final Rating rating) {
        return dispatchRemoteSessionTask(COMMAND_CODE_SESSION_SET_RATING, new RemoteSessionTask() {
            @Override
            public void run(IMediaSession iSession, int seq) throws RemoteException {
                iSession.setRating(mControllerStub, seq, mediaId,
                        MediaParcelUtils.toParcelable(rating));
            }
        });
    }

    @Override
    public ListenableFuture<SessionResult> sendCustomCommand(
            @NonNull final SessionCommand command, @Nullable final Bundle args) {
        return dispatchRemoteSessionTask(command, new RemoteSessionTask() {
            @Override
            public void run(IMediaSession iSession, int seq) throws RemoteException {
                iSession.onCustomCommand(mControllerStub, seq,
                        MediaParcelUtils.toParcelable(command), args);
            }
        });
    }

    @Override
    public List<MediaItem> getPlaylist() {
        synchronized (mLock) {
            return mPlaylist;
        }
    }

    @Override
    public ListenableFuture<SessionResult> setPlaylist(@NonNull final List<String> list,
            @Nullable final MediaMetadata metadata) {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_SET_PLAYLIST, new RemoteSessionTask() {
            @Override
            public void run(IMediaSession iSession, int seq) throws RemoteException {
                iSession.setPlaylist(mControllerStub, seq, list,
                        MediaParcelUtils.toParcelable(metadata));
            }
        });
    }

    @Override
    public ListenableFuture<SessionResult> setMediaItem(@NonNull final String mediaId) {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_SET_MEDIA_ITEM,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.setMediaItem(mControllerStub, seq, mediaId);
                    }
                });
    }

    @Override
    public ListenableFuture<SessionResult> updatePlaylistMetadata(
            @Nullable final MediaMetadata metadata) {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_UPDATE_LIST_METADATA,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.updatePlaylistMetadata(mControllerStub, seq,
                                MediaParcelUtils.toParcelable(metadata));
                    }
                });
    }

    @Override
    public MediaMetadata getPlaylistMetadata() {
        synchronized (mLock) {
            return mPlaylistMetadata;
        }
    }

    @Override
    public ListenableFuture<SessionResult> addPlaylistItem(final int index,
            @NonNull final String mediaId) {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_ADD_PLAYLIST_ITEM,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.addPlaylistItem(mControllerStub, seq, index, mediaId);
                    }
                });
    }

    @Override
    public ListenableFuture<SessionResult> removePlaylistItem(final int index) {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_REMOVE_PLAYLIST_ITEM,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.removePlaylistItem(mControllerStub, seq, index);
                    }
                });
    }

    @Override
    public ListenableFuture<SessionResult> replacePlaylistItem(final int index,
            @NonNull final String mediaId) {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_REPLACE_PLAYLIST_ITEM,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.replacePlaylistItem(mControllerStub, seq, index, mediaId);
                    }
                });
    }

    @Override
    public MediaItem getCurrentMediaItem() {
        synchronized (mLock) {
            return mCurrentMediaItem;
        }
    }

    @Override
    public int getCurrentMediaItemIndex() {
        synchronized (mLock) {
            return mCurrentMediaItemIndex;
        }
    }

    @Override
    public int getPreviousMediaItemIndex() {
        synchronized (mLock) {
            return mPreviousMediaItemIndex;
        }
    }

    @Override
    public int getNextMediaItemIndex() {
        synchronized (mLock) {
            return mNextMediaItemIndex;
        }
    }

    @Override
    public ListenableFuture<SessionResult> skipToPreviousItem() {
        return dispatchRemoteSessionTask(
                COMMAND_CODE_PLAYER_SKIP_TO_PREVIOUS_PLAYLIST_ITEM,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.skipToPreviousItem(mControllerStub, seq);
                    }
                });
    }

    @Override
    public ListenableFuture<SessionResult> skipToNextItem() {
        return dispatchRemoteSessionTask(
                COMMAND_CODE_PLAYER_SKIP_TO_NEXT_PLAYLIST_ITEM,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.skipToNextItem(mControllerStub, seq);
                    }
                });
    }

    @Override
    public ListenableFuture<SessionResult> skipToPlaylistItem(final int index) {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_SKIP_TO_PLAYLIST_ITEM,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.skipToPlaylistItem(mControllerStub, seq, index);
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
    public ListenableFuture<SessionResult> setRepeatMode(final int repeatMode) {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_SET_REPEAT_MODE,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.setRepeatMode(mControllerStub, seq, repeatMode);
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
    public ListenableFuture<SessionResult> setShuffleMode(final int shuffleMode) {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_SET_SHUFFLE_MODE,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.setShuffleMode(mControllerStub, seq, shuffleMode);
                    }
                });
    }

    @Override
    @NonNull
    public List<SessionPlayer.TrackInfo> getTrackInfo() {
        synchronized (mLock) {
            return mTrackInfos;
        }
    }

    @Override
    @NonNull
    public ListenableFuture<SessionResult> selectTrack(
            @NonNull final SessionPlayer.TrackInfo trackInfo) {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_SELECT_TRACK,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.selectTrack(mControllerStub, seq,
                                MediaParcelUtils.toParcelable(trackInfo));
                    }
                });
    }

    @Override
    @NonNull
    public ListenableFuture<SessionResult> deselectTrack(
            @NonNull final SessionPlayer.TrackInfo trackInfo) {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_DESELECT_TRACK,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.deselectTrack(mControllerStub, seq,
                                MediaParcelUtils.toParcelable(trackInfo));
                    }
                });
    }

    @Override
    @Nullable
    public TrackInfo getSelectedTrack(int trackType) {
        synchronized (mLock) {
            return mSelectedTracks.get(trackType);
        }
    }

    @Override
    @NonNull
    public VideoSize getVideoSize() {
        synchronized (mLock) {
            return mVideoSize;
        }
    }

    @Override
    public ListenableFuture<SessionResult> setSurface(@Nullable final Surface surface) {
        return dispatchRemoteSessionTask(COMMAND_CODE_PLAYER_SET_SURFACE,
                new RemoteSessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.setSurface(mControllerStub, seq, surface);
                    }
                });
    }

    @Override
    public SessionCommandGroup getAllowedCommands() {
        synchronized (mLock) {
            if (mISession == null) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return null;
            }
            return mAllowedCommands;
        }
    }

    @Override
    @NonNull
    public Context getContext() {
        return mContext;
    }

    @Override
    @Nullable
    public MediaBrowserCompat getBrowserCompat() {
        return null;
    }

    private boolean requestConnectToService() {
        // Service. Needs to get fresh binder whenever connection is needed.
        final Intent intent = new Intent(MediaSessionService.SERVICE_INTERFACE);
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
                return false;
            }
        }
        if (DEBUG) {
            Log.d(TAG, "bind to " + mToken + " succeeded");
        }
        return true;
    }

    private boolean requestConnectToSession(@Nullable Bundle connectionHints) {
        IMediaSession iSession = IMediaSession.Stub.asInterface((IBinder) mToken.getBinder());
        int seq = mSequencedFutureManager.obtainNextSequenceNumber();
        ConnectionRequest request =
                new ConnectionRequest(mContext.getPackageName(), Process.myPid(), connectionHints);
        try {
            iSession.connect(mControllerStub, seq, MediaParcelUtils.toParcelable(request));
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to call connection request.", e);
            return false;
        }
        return true;
    }

    // Returns session interface if the controller can send the command.
    IMediaSession getSessionInterfaceIfAble(@SessionCommand.CommandCode int commandCode) {
        synchronized (mLock) {
            if (!mAllowedCommands.hasCommand(commandCode)) {
                // Cannot send because isn't allowed to.
                Log.w(TAG, "Controller isn't allowed to call command, commandCode="
                        + commandCode);
                return null;
            }
            return mISession;
        }
    }

    // Returns session binder if the controller can send the command.
    IMediaSession getSessionInterfaceIfAble(SessionCommand command) {
        synchronized (mLock) {
            if (!mAllowedCommands.hasCommand(command)) {
                Log.w(TAG, "Controller isn't allowed to call command, command=" + command);
                return null;
            }
            return mISession;
        }
    }

    void notifyCurrentMediaItemChanged(final MediaItem item, int currentMediaItemIndex,
            int previousMediaItemIndex, int nextMediaItemIndex) {
        synchronized (mLock) {
            mCurrentMediaItem = item;
            mCurrentMediaItemIndex = currentMediaItemIndex;
            mPreviousMediaItemIndex = previousMediaItemIndex;
            mNextMediaItemIndex = nextMediaItemIndex;
            if (mPlaylist != null && currentMediaItemIndex >= 0
                    && currentMediaItemIndex < mPlaylist.size()) {
                mPlaylist.set(currentMediaItemIndex, item);
            }
        }
        mInstance.notifyControllerCallback(new ControllerCallbackRunnable() {
            @Override
            public void run(@NonNull ControllerCallback callback) {
                if (!mInstance.isConnected()) {
                    return;
                }
                callback.onCurrentMediaItemChanged(mInstance, item);
            }
        });
    }

    void notifyPlayerStateChanges(long eventTimeMs, long positionMs, final int state) {
        synchronized (mLock) {
            mPositionEventTimeMs = eventTimeMs;
            mPositionMs = positionMs;
            mPlayerState = state;
        }
        mInstance.notifyControllerCallback(new ControllerCallbackRunnable() {
            @Override
            public void run(@NonNull ControllerCallback callback) {
                if (!mInstance.isConnected()) {
                    return;
                }
                callback.onPlayerStateChanged(mInstance, state);
            }
        });
    }

    void notifyPlaybackSpeedChanges(long eventTimeMs, long positionMs, final float speed) {
        synchronized (mLock) {
            mPositionEventTimeMs = eventTimeMs;
            mPositionMs = positionMs;
            mPlaybackSpeed = speed;
        }
        mInstance.notifyControllerCallback(new ControllerCallbackRunnable() {
            @Override
            public void run(@NonNull ControllerCallback callback) {
                if (!mInstance.isConnected()) {
                    return;
                }
                callback.onPlaybackSpeedChanged(mInstance, speed);
            }
        });
    }

    void notifyBufferingStateChanged(final MediaItem item, final int state,
            long bufferedPositionMs, long eventTimeMs, long positionMs) {
        synchronized (mLock) {
            mBufferingState = state;
            mBufferedPositionMs = bufferedPositionMs;
            mPositionEventTimeMs = eventTimeMs;
            mPositionMs = positionMs;
        }
        mInstance.notifyControllerCallback(new ControllerCallbackRunnable() {
            @Override
            public void run(@NonNull ControllerCallback callback) {
                if (!mInstance.isConnected()) {
                    return;
                }
                callback.onBufferingStateChanged(mInstance, item, state);
            }
        });
    }

    void notifyPlaylistChanges(final List<MediaItem> playlist, final MediaMetadata metadata,
            int currentMediaItemIndex, int previousMediaItemIndex, int nextMediaItemIndex) {
        synchronized (mLock) {
            mPlaylist = playlist;
            mPlaylistMetadata = metadata;
            mCurrentMediaItemIndex = currentMediaItemIndex;
            mPreviousMediaItemIndex = previousMediaItemIndex;
            mNextMediaItemIndex = nextMediaItemIndex;
            if (currentMediaItemIndex >= 0 && currentMediaItemIndex < playlist.size()) {
                mCurrentMediaItem = playlist.get(currentMediaItemIndex);
            }
        }
        mInstance.notifyControllerCallback(new ControllerCallbackRunnable() {
            @Override
            public void run(@NonNull ControllerCallback callback) {
                if (!mInstance.isConnected()) {
                    return;
                }
                callback.onPlaylistChanged(mInstance, playlist, metadata);
            }
        });
    }

    void notifyPlaylistMetadataChanges(final MediaMetadata metadata) {
        synchronized (mLock) {
            mPlaylistMetadata = metadata;
        }
        mInstance.notifyControllerCallback(new ControllerCallbackRunnable() {
            @Override
            public void run(@NonNull ControllerCallback callback) {
                if (!mInstance.isConnected()) {
                    return;
                }
                callback.onPlaylistMetadataChanged(mInstance, metadata);
            }
        });
    }

    void notifyPlaybackInfoChanges(final PlaybackInfo info) {
        synchronized (mLock) {
            mPlaybackInfo = info;
        }
        mInstance.notifyControllerCallback(new ControllerCallbackRunnable() {
            @Override
            public void run(@NonNull ControllerCallback callback) {
                if (!mInstance.isConnected()) {
                    return;
                }
                callback.onPlaybackInfoChanged(mInstance, info);
            }
        });
    }

    void notifyRepeatModeChanges(final int repeatMode, int currentMediaItemIndex,
            int previousMediaItemIndex, int nextMediaItemIndex) {
        synchronized (mLock) {
            mRepeatMode = repeatMode;
            mCurrentMediaItemIndex = currentMediaItemIndex;
            mPreviousMediaItemIndex = previousMediaItemIndex;
            mNextMediaItemIndex = nextMediaItemIndex;
        }
        mInstance.notifyControllerCallback(new ControllerCallbackRunnable() {
            @Override
            public void run(@NonNull ControllerCallback callback) {
                if (!mInstance.isConnected()) {
                    return;
                }
                callback.onRepeatModeChanged(mInstance, repeatMode);
            }
        });
    }

    void notifyShuffleModeChanges(final int shuffleMode, int currentMediaItemIndex,
            int previousMediaItemIndex, int nextMediaItemIndex) {
        synchronized (mLock) {
            mShuffleMode = shuffleMode;
            mCurrentMediaItemIndex = currentMediaItemIndex;
            mPreviousMediaItemIndex = previousMediaItemIndex;
            mNextMediaItemIndex = nextMediaItemIndex;
        }
        mInstance.notifyControllerCallback(new ControllerCallbackRunnable() {
            @Override
            public void run(@NonNull ControllerCallback callback) {
                if (!mInstance.isConnected()) {
                    return;
                }
                callback.onShuffleModeChanged(mInstance, shuffleMode);
            }
        });
    }

    void notifyPlaybackCompleted() {
        mInstance.notifyControllerCallback(new ControllerCallbackRunnable() {
            @Override
            public void run(@NonNull ControllerCallback callback) {
                if (!mInstance.isConnected()) {
                    return;
                }
                callback.onPlaybackCompleted(mInstance);
            }
        });
    }

    void notifySeekCompleted(long eventTimeMs, long positionMs, final long seekPositionMs) {
        synchronized (mLock) {
            mPositionEventTimeMs = eventTimeMs;
            mPositionMs = positionMs;
        }

        mInstance.notifyControllerCallback(new ControllerCallbackRunnable() {
            @Override
            public void run(@NonNull ControllerCallback callback) {
                if (!mInstance.isConnected()) {
                    return;
                }
                callback.onSeekCompleted(mInstance, seekPositionMs);
            }
        });
    }

    void notifyVideoSizeChanged(final MediaItem item, final VideoSize videoSize) {
        synchronized (mLock) {
            mVideoSize = videoSize;
        }
        mInstance.notifyControllerCallback(new ControllerCallbackRunnable() {
            @Override
            public void run(@NonNull ControllerCallback callback) {
                if (!mInstance.isConnected()) {
                    return;
                }
                callback.onVideoSizeChanged(mInstance, item, videoSize);
            }
        });
    }

    void notifyTrackInfoChanged(final int seq, final List<TrackInfo> trackInfos,
            TrackInfo selectedVideoTrack, TrackInfo selectedAudioTrack,
            TrackInfo selectedSubtitleTrack, TrackInfo selectedMetadataTrack) {
        synchronized (mLock) {
            mTrackInfos = trackInfos;
            // Update selected tracks
            mSelectedTracks.put(TrackInfo.MEDIA_TRACK_TYPE_VIDEO, selectedVideoTrack);
            mSelectedTracks.put(TrackInfo.MEDIA_TRACK_TYPE_AUDIO, selectedAudioTrack);
            mSelectedTracks.put(TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE, selectedSubtitleTrack);
            mSelectedTracks.put(TrackInfo.MEDIA_TRACK_TYPE_METADATA, selectedMetadataTrack);
        }

        mInstance.notifyControllerCallback(new ControllerCallbackRunnable() {
            @Override
            public void run(@NonNull ControllerCallback callback) {
                if (!mInstance.isConnected()) {
                    return;
                }
                callback.onTrackInfoChanged(mInstance, trackInfos);
            }
        });
    }

    void notifyTrackSelected(final int seq, final TrackInfo trackInfo) {
        synchronized (mLock) {
            mSelectedTracks.put(trackInfo.getTrackType(), trackInfo);
        }
        mInstance.notifyControllerCallback(new ControllerCallbackRunnable() {
            @Override
            public void run(@NonNull ControllerCallback callback) {
                if (!mInstance.isConnected()) {
                    return;
                }
                callback.onTrackSelected(mInstance, trackInfo);
            }
        });
    }

    void notifyTrackDeselected(final int seq, final TrackInfo trackInfo) {
        synchronized (mLock) {
            mSelectedTracks.remove(trackInfo.getTrackType());
        }
        mInstance.notifyControllerCallback(new ControllerCallbackRunnable() {
            @Override
            public void run(@NonNull ControllerCallback callback) {
                if (!mInstance.isConnected()) {
                    return;
                }
                callback.onTrackDeselected(mInstance, trackInfo);
            }
        });
    }

    void notifySubtitleData(final MediaItem item, final TrackInfo track, final SubtitleData data) {
        mInstance.notifyControllerCallback(new ControllerCallbackRunnable() {
            @Override
            public void run(@NonNull ControllerCallback callback) {
                if (!mInstance.isConnected()) {
                    return;
                }
                callback.onSubtitleData(mInstance, item, track, data);
            }
        });
    }

    // Should be used without a lock to prevent potential deadlock.
    void onConnectedNotLocked(IMediaSession sessionBinder,
            final SessionCommandGroup allowedCommands,
            final int playerState,
            final MediaItem currentMediaItem,
            final long positionEventTimeMs,
            final long positionMs,
            final float playbackSpeed,
            final long bufferedPositionMs,
            final PlaybackInfo info,
            final int repeatMode,
            final int shuffleMode,
            final List<MediaItem> playlist,
            final PendingIntent sessionActivity,
            final int currentMediaItemIndex,
            final int previousMediaItemIndex,
            final int nextMediaItemIndex,
            final Bundle tokenExtras,
            final VideoSize videoSize,
            final List<TrackInfo> trackInfos,
            final TrackInfo selectedVideoTrack,
            final TrackInfo selectedAudioTrack,
            final TrackInfo selectedSubtitleTrack,
            final TrackInfo selectedMetadataTrack) {
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
                if (mISession != null) {
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
                mISession = sessionBinder;
                mCurrentMediaItemIndex = currentMediaItemIndex;
                mPreviousMediaItemIndex = previousMediaItemIndex;
                mNextMediaItemIndex = nextMediaItemIndex;
                mVideoSize = videoSize;
                mTrackInfos = trackInfos;
                mSelectedTracks.put(TrackInfo.MEDIA_TRACK_TYPE_VIDEO, selectedVideoTrack);
                mSelectedTracks.put(TrackInfo.MEDIA_TRACK_TYPE_AUDIO, selectedAudioTrack);
                mSelectedTracks.put(TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE, selectedSubtitleTrack);
                mSelectedTracks.put(TrackInfo.MEDIA_TRACK_TYPE_METADATA, selectedMetadataTrack);
                try {
                    // Implementation for the local binder is no-op,
                    // so can be used without worrying about deadlock.
                    mISession.asBinder().linkToDeath(mDeathRecipient, 0);
                } catch (RemoteException e) {
                    if (DEBUG) {
                        Log.d(TAG, "Session died too early.", e);
                    }
                    close = true;
                    return;
                }
                mConnectedToken = new SessionToken(new SessionTokenImplBase(
                        mToken.getUid(), TYPE_SESSION, mToken.getPackageName(), sessionBinder,
                        tokenExtras));
            }
            mInstance.notifyControllerCallback(new ControllerCallbackRunnable() {
                @Override
                public void run(@NonNull ControllerCallback callback) {
                    callback.onConnected(mInstance, allowedCommands);
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
    void sendControllerResult(int seq, @NonNull SessionResult result) {
        final IMediaSession iSession;
        synchronized (mLock) {
            iSession = mISession;
        }
        if (iSession == null) {
            return;
        }
        try {
            iSession.onControllerResult(mControllerStub, seq,
                    MediaParcelUtils.toParcelable(result));
        } catch (RemoteException e) {
            Log.w(TAG, "Error in sending");
        }
    }

    void onCustomCommand(final int seq, final SessionCommand command, final Bundle args) {
        if (DEBUG) {
            Log.d(TAG, "onCustomCommand cmd=" + command.getCustomAction());
        }
        mInstance.notifyControllerCallback(new ControllerCallbackRunnable() {
            @Override
            public void run(@NonNull ControllerCallback callback) {
                SessionResult result = callback.onCustomCommand(mInstance, command, args);
                if (result == null) {
                    if (THROW_EXCEPTION_FOR_NULL_RESULT) {
                        throw new RuntimeException("ControllerCallback#onCustomCommand() has"
                                + " returned null, command=" + command.getCustomAction());
                    } else {
                        result = new SessionResult(RESULT_ERROR_UNKNOWN);
                    }
                }
                sendControllerResult(seq, result);
            }
        });
    }

    void onAllowedCommandsChanged(final SessionCommandGroup commands) {
        synchronized (mLock) {
            mAllowedCommands = commands;
        }
        mInstance.notifyControllerCallback(new ControllerCallbackRunnable() {
            @Override
            public void run(@NonNull ControllerCallback callback) {
                callback.onAllowedCommandsChanged(mInstance, commands);
            }
        });
    }

    void onSetCustomLayout(final int seq, final List<MediaSession.CommandButton> layout) {
        mInstance.notifyControllerCallback(new ControllerCallbackRunnable() {
            @Override
            public void run(@NonNull ControllerCallback callback) {
                int resultCode = callback.onSetCustomLayout(mInstance, layout);
                SessionResult result = new SessionResult(resultCode);
                sendControllerResult(seq, result);
            }
        });
    }

    // This will be called on the main thread.
    private class SessionServiceConnection implements ServiceConnection {
        private final Bundle mConnectionHints;

        SessionServiceConnection(@Nullable Bundle connectionHints) {
            mConnectionHints = connectionHints;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            boolean connectionRequested = false;
            try {
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
                IMediaSessionService iService = IMediaSessionService.Stub.asInterface(service);
                if (iService == null) {
                    Log.wtf(TAG, "Service interface is missing.");
                    return;
                }
                ConnectionRequest request = new ConnectionRequest(getContext().getPackageName(),
                        Process.myPid(), mConnectionHints);
                iService.connect(mControllerStub, MediaParcelUtils.toParcelable(request));
                connectionRequested = true;
            } catch (RemoteException e) {
                Log.w(TAG, "Service " + name + " has died prematurely");
            } finally {
                if (!connectionRequested) {
                    mInstance.close();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Temporal lose of the binding because of the service crash. System will automatically
            // rebind, but we'd better to close() here. Otherwise ControllerCallback#onConnected()
            // would be called multiple times, and the controller would be connected to the
            // different session everytime.
            if (DEBUG) {
                Log.w(TAG, "Session service " + name + " is disconnected.");
            }
            mInstance.close();
        }

        @Override
        public void onBindingDied(ComponentName name) {
            // Permanent lose of the binding because of the service package update or removed.
            // This SessionServiceRecord will be removed accordingly, but forget session binder here
            // for sure.
            mInstance.close();
        }
    }
}
