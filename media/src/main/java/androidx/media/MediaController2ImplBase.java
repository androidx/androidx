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

package androidx.media;

import static androidx.media.BaseMediaPlayer.BUFFERING_STATE_UNKNOWN;
import static androidx.media.BaseMediaPlayer.UNKNOWN_TIME;
import static androidx.media.MediaMetadata2.METADATA_KEY_DURATION;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYBACK_PAUSE;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYBACK_PLAY;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYBACK_PREPARE;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYBACK_RESET;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYBACK_SEEK_TO;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYBACK_SET_SPEED;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_ADD_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_REMOVE_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_REPLACE_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SET_LIST;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SET_LIST_METADATA;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SET_REPEAT_MODE;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SET_SHUFFLE_MODE;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_TO_NEXT_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_TO_PLAYLIST_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_TO_PREV_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_FAST_FORWARD;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_SEARCH;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_URI;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_URI;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_REWIND;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_SELECT_ROUTE;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_SUBSCRIBE_ROUTES_INFO;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO;
import static androidx.media.SessionCommand2.COMMAND_CODE_VOLUME_ADJUST_VOLUME;
import static androidx.media.SessionCommand2.COMMAND_CODE_VOLUME_SET_VOLUME;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.v4.media.MediaBrowserCompat;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaController2.ControllerCallback;
import androidx.media.MediaController2.PlaybackInfo;
import androidx.media.MediaController2.VolumeDirection;
import androidx.media.MediaController2.VolumeFlags;
import androidx.media.MediaPlaylistAgent.RepeatMode;
import androidx.media.MediaPlaylistAgent.ShuffleMode;

import java.util.List;
import java.util.concurrent.Executor;

class MediaController2ImplBase implements MediaController2.SupportLibraryImpl {
    private static final String TAG = "MC2ImplBase";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final MediaController2 mInstance;
    private final Context mContext;
    private final Object mLock = new Object();

    private final MediaController2Stub mControllerStub;
    private final SessionToken2 mToken;
    private final ControllerCallback mCallback;
    private final Executor mCallbackExecutor;
    private final IBinder.DeathRecipient mDeathRecipient;

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
        mControllerStub = new MediaController2Stub(this);
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
            connectToSession(iSession2);
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
            try {
                iSession2.asBinder().unlinkToDeath(mDeathRecipient, 0);
                iSession2.release(mControllerStub);
            } catch (RemoteException e) {
                // No-op.
            }
        }
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mCallback.onDisconnected(mInstance);
            }
        });
    }

    @Override
    public SessionToken2 getSessionToken() {
        return mToken;
    }

    @Override
    public boolean isConnected() {
        synchronized (mLock) {
            return mISession2 != null;
        }
    }

    @Override
    public void play() {
        final IMediaSession2 iSession2 = getSessionInterfaceIfAble(COMMAND_CODE_PLAYBACK_PLAY);
        if (iSession2 != null) {
            try {
                iSession2.play(mControllerStub);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void pause() {
        final IMediaSession2 iSession2 = getSessionInterfaceIfAble(COMMAND_CODE_PLAYBACK_PAUSE);
        if (iSession2 != null) {
            try {
                iSession2.pause(mControllerStub);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void reset() {
        final IMediaSession2 iSession2 = getSessionInterfaceIfAble(COMMAND_CODE_PLAYBACK_RESET);
        if (iSession2 != null) {
            try {
                iSession2.reset(mControllerStub);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void prepare() {
        final IMediaSession2 iSession2 = getSessionInterfaceIfAble(COMMAND_CODE_PLAYBACK_PREPARE);
        if (iSession2 != null) {
            try {
                iSession2.prepare(mControllerStub);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void fastForward() {
        final IMediaSession2 iSession2 = getSessionInterfaceIfAble(
                COMMAND_CODE_SESSION_FAST_FORWARD);
        if (iSession2 != null) {
            try {
                iSession2.fastForward(mControllerStub);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void rewind() {
        final IMediaSession2 iSession2 = getSessionInterfaceIfAble(COMMAND_CODE_SESSION_REWIND);
        if (iSession2 != null) {
            try {
                iSession2.rewind(mControllerStub);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void seekTo(long pos) {
        if (pos < 0) {
            throw new IllegalArgumentException("position shouldn't be negative");
        }
        final IMediaSession2 iSession2 = getSessionInterfaceIfAble(COMMAND_CODE_PLAYBACK_SEEK_TO);
        if (iSession2 != null) {
            try {
                iSession2.seekTo(mControllerStub, pos);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void skipForward() {
        // To match with KEYCODE_MEDIA_SKIP_FORWARD
    }

    @Override
    public void skipBackward() {
        // To match with KEYCODE_MEDIA_SKIP_BACKWARD
    }

    @Override
    public void playFromMediaId(@NonNull String mediaId, @Nullable Bundle extras) {
        final IMediaSession2 iSession2 = getSessionInterfaceIfAble(
                COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID);
        if (iSession2 != null) {
            try {
                iSession2.playFromMediaId(mControllerStub, mediaId, extras);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void playFromSearch(@NonNull String query, @Nullable Bundle extras) {
        final IMediaSession2 iSession2 =
                getSessionInterfaceIfAble(COMMAND_CODE_SESSION_PLAY_FROM_SEARCH);
        if (iSession2 != null) {
            try {
                iSession2.playFromSearch(mControllerStub, query, extras);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void playFromUri(Uri uri, Bundle extras) {
        final IMediaSession2 iSession2 =
                getSessionInterfaceIfAble(COMMAND_CODE_SESSION_PLAY_FROM_URI);
        if (iSession2 != null) {
            try {
                iSession2.playFromUri(mControllerStub, uri, extras);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void prepareFromMediaId(@NonNull String mediaId, @Nullable Bundle extras) {
        final IMediaSession2 iSession2 = getSessionInterfaceIfAble(
                COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID);
        if (iSession2 != null) {
            try {
                iSession2.prepareFromMediaId(mControllerStub, mediaId, extras);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void prepareFromSearch(@NonNull String query, @Nullable Bundle extras) {
        final IMediaSession2 iSession2 = getSessionInterfaceIfAble(
                COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH);
        if (iSession2 != null) {
            try {
                iSession2.prepareFromSearch(mControllerStub, query, extras);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void prepareFromUri(@NonNull Uri uri, @Nullable Bundle extras) {
        final IMediaSession2 iSession2 =
                getSessionInterfaceIfAble(COMMAND_CODE_SESSION_PREPARE_FROM_URI);
        if (iSession2 != null) {
            try {
                iSession2.prepareFromUri(mControllerStub, uri, extras);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void setVolumeTo(int value, @VolumeFlags int flags) {
        final IMediaSession2 iSession2 = getSessionInterfaceIfAble(COMMAND_CODE_VOLUME_SET_VOLUME);
        if (iSession2 != null) {
            try {
                iSession2.setVolumeTo(mControllerStub, value, flags);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void adjustVolume(@VolumeDirection int direction, @VolumeFlags int flags) {
        final IMediaSession2 iSession2 =
                getSessionInterfaceIfAble(COMMAND_CODE_VOLUME_ADJUST_VOLUME);
        if (iSession2 != null) {
            try {
                iSession2.adjustVolume(mControllerStub, direction, flags);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
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
            MediaMetadata2 metadata = mCurrentMediaItem.getMetadata();
            if (metadata != null && metadata.containsKey(METADATA_KEY_DURATION)) {
                return metadata.getLong(METADATA_KEY_DURATION);
            }
        }
        return BaseMediaPlayer.UNKNOWN_TIME;
    }

    @Override
    public long getCurrentPosition() {
        synchronized (mLock) {
            if (mISession2 == null) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return UNKNOWN_TIME;
            }
            long timeDiff = (mInstance.mTimeDiff != null) ? mInstance.mTimeDiff
                    : SystemClock.elapsedRealtime() - mPositionEventTimeMs;
            long expectedPosition = mPositionMs + (long) (mPlaybackSpeed * timeDiff);
            return Math.max(0, expectedPosition);
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
    public void setPlaybackSpeed(float speed) {
        synchronized (mLock) {
            final IMediaSession2 iSession2 =
                    getSessionInterfaceIfAble(COMMAND_CODE_PLAYBACK_SET_SPEED);
            if (iSession2 != null) {
                try {
                    iSession2.setPlaybackSpeed(mControllerStub, speed);
                } catch (RemoteException e) {
                    Log.w(TAG, "Cannot connect to the service or the session is gone", e);
                }
            }
        }
    }

    @Override
    public @BaseMediaPlayer.BuffState int getBufferingState() {
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
    public void setRating(@NonNull String mediaId, @NonNull Rating2 rating) {
        final IMediaSession2 iSession2;
        synchronized (mLock) {
            iSession2 = mISession2;
        }
        if (iSession2 != null) {
            try {
                iSession2.setRating(mControllerStub, mediaId, rating.toBundle());
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void sendCustomCommand(@NonNull SessionCommand2 command, Bundle args,
            @Nullable ResultReceiver cb) {
        final IMediaSession2 iSession2 = getSessionInterfaceIfAble(command);
        if (iSession2 != null) {
            try {
                iSession2.sendCustomCommand(mControllerStub, command.toBundle(), args, cb);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public List<MediaItem2> getPlaylist() {
        synchronized (mLock) {
            return mPlaylist;
        }
    }

    @Override
    public void setPlaylist(@NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata) {
        final IMediaSession2 iSession2 = getSessionInterfaceIfAble(COMMAND_CODE_PLAYLIST_SET_LIST);
        if (iSession2 != null) {
            try {
                iSession2.setPlaylist(mControllerStub, MediaUtils2.toMediaItem2BundleList(list),
                        (metadata == null) ? null : metadata.toBundle());
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void updatePlaylistMetadata(@Nullable MediaMetadata2 metadata) {
        final IMediaSession2 iSession2 = getSessionInterfaceIfAble(
                COMMAND_CODE_PLAYLIST_SET_LIST_METADATA);
        if (iSession2 != null) {
            try {
                iSession2.updatePlaylistMetadata(mControllerStub,
                        (metadata == null) ? null : metadata.toBundle());
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public MediaMetadata2 getPlaylistMetadata() {
        synchronized (mLock) {
            return mPlaylistMetadata;
        }
    }

    @Override
    public void addPlaylistItem(int index, @NonNull MediaItem2 item) {
        final IMediaSession2 iSession2 = getSessionInterfaceIfAble(COMMAND_CODE_PLAYLIST_ADD_ITEM);
        if (iSession2 != null) {
            try {
                iSession2.addPlaylistItem(mControllerStub, index, item.toBundle());
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void removePlaylistItem(@NonNull MediaItem2 item) {
        final IMediaSession2 iSession2 =
                getSessionInterfaceIfAble(COMMAND_CODE_PLAYLIST_REMOVE_ITEM);
        if (iSession2 != null) {
            try {
                iSession2.removePlaylistItem(mControllerStub, item.toBundle());
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void replacePlaylistItem(int index, @NonNull MediaItem2 item) {
        final IMediaSession2 iSession2 =
                getSessionInterfaceIfAble(COMMAND_CODE_PLAYLIST_REPLACE_ITEM);
        if (iSession2 != null) {
            try {
                iSession2.replacePlaylistItem(mControllerStub, index, item.toBundle());
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public MediaItem2 getCurrentMediaItem() {
        synchronized (mLock) {
            return mCurrentMediaItem;
        }
    }

    @Override
    public void skipToPreviousItem() {
        final IMediaSession2 iSession2 =
                getSessionInterfaceIfAble(COMMAND_CODE_PLAYLIST_SKIP_TO_PREV_ITEM);
        synchronized (mLock) {
            if (iSession2 != null) {
                try {
                    iSession2.skipToPreviousItem(mControllerStub);
                } catch (RemoteException e) {
                    Log.w(TAG, "Cannot connect to the service or the session is gone", e);
                }
            }
        }
    }

    @Override
    public void skipToNextItem() {
        final IMediaSession2 iSession2 =
                getSessionInterfaceIfAble(COMMAND_CODE_PLAYLIST_SKIP_TO_NEXT_ITEM);
        synchronized (mLock) {
            if (iSession2 != null) {
                try {
                    mISession2.skipToNextItem(mControllerStub);
                } catch (RemoteException e) {
                    Log.w(TAG, "Cannot connect to the service or the session is gone", e);
                }
            }
        }
    }

    @Override
    public void skipToPlaylistItem(@NonNull MediaItem2 item) {
        final IMediaSession2 iSession2 =
                getSessionInterfaceIfAble(COMMAND_CODE_PLAYLIST_SKIP_TO_PLAYLIST_ITEM);
        synchronized (mLock) {
            if (iSession2 != null) {
                try {
                    mISession2.skipToPlaylistItem(mControllerStub, item.toBundle());
                } catch (RemoteException e) {
                    Log.w(TAG, "Cannot connect to the service or the session is gone", e);
                }
            }
        }
    }

    @Override
    public int getRepeatMode() {
        synchronized (mLock) {
            return mRepeatMode;
        }
    }

    @Override
    public void setRepeatMode(int repeatMode) {
        final IMediaSession2 iSession2 =
                getSessionInterfaceIfAble(COMMAND_CODE_PLAYLIST_SET_REPEAT_MODE);
        if (iSession2 != null) {
            try {
                iSession2.setRepeatMode(mControllerStub, repeatMode);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public int getShuffleMode() {
        synchronized (mLock) {
            return mShuffleMode;
        }
    }

    @Override
    public void setShuffleMode(int shuffleMode) {
        final IMediaSession2 iSession2 =
                getSessionInterfaceIfAble(COMMAND_CODE_PLAYLIST_SET_SHUFFLE_MODE);
        if (iSession2 != null) {
            try {
                iSession2.setShuffleMode(mControllerStub, shuffleMode);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void subscribeRoutesInfo() {
        final IMediaSession2 iSession2 =
                getSessionInterfaceIfAble(COMMAND_CODE_SESSION_SUBSCRIBE_ROUTES_INFO);
        if (iSession2 != null) {
            try {
                iSession2.subscribeRoutesInfo(mControllerStub);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void unsubscribeRoutesInfo() {
        final IMediaSession2 iSession2 =
                getSessionInterfaceIfAble(COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO);
        if (iSession2 != null) {
            try {
                iSession2.unsubscribeRoutesInfo(mControllerStub);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void selectRoute(@NonNull Bundle route) {
        final IMediaSession2 iSession2 =
                getSessionInterfaceIfAble(COMMAND_CODE_SESSION_SELECT_ROUTE);
        if (iSession2 != null) {
            try {
                iSession2.selectRoute(mControllerStub, route);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
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
        //    bounded service. But there had been request for the feature for system apps
        //    and using bindService() will be better fit with it.
        synchronized (mLock) {
            boolean result = mContext.bindService(
                    intent, mServiceConnection, Context.BIND_AUTO_CREATE);
            if (!result) {
                Log.w(TAG, "bind to " + mToken + " failed");
            } else if (DEBUG) {
                Log.d(TAG, "bind to " + mToken + " success");
            }
        }
    }

    private void connectToSession(IMediaSession2 sessionBinder) {
        try {
            sessionBinder.connect(mControllerStub, mContext.getPackageName());
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to call connection request. Framework will retry"
                    + " automatically");
        }
    }

    // Returns session interface if the controller can send the command.
    IMediaSession2 getSessionInterfaceIfAble(int commandCode) {
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

    void notifyError(final int errorCode, final Bundle extras) {
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (!mInstance.isConnected()) {
                    return;
                }
                mCallback.onError(mInstance, errorCode, extras);
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
            }
            mCallbackExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    // Note: We may trigger ControllerCallbacks with the initial values
                    // But it's hard to define the order of the controller callbacks
                    // Only notify about the
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

    void onCustomCommand(final SessionCommand2 command, final Bundle args,
            final ResultReceiver receiver) {
        if (DEBUG) {
            Log.d(TAG, "onCustomCommand cmd=" + command);
        }
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mCallback.onCustomCommand(mInstance, command, args, receiver);
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

    void onCustomLayoutChanged(final List<MediaSession2.CommandButton> layout) {
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mCallback.onCustomLayoutChanged(mInstance, layout);
            }
        });
    }

    // This will be called on the main thread.
    private class SessionServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // Note that it's always main-thread.
            if (DEBUG) {
                Log.d(TAG, "onServiceConnected " + name + " " + this);
            }
            // Sanity check
            if (!mToken.getPackageName().equals(name.getPackageName())) {
                Log.wtf(TAG, name + " was connected, but expected pkg="
                        + mToken.getPackageName() + " with id=" + mToken.getId());
                return;
            }
            connectToSession(IMediaSession2.Stub.asInterface(service));
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