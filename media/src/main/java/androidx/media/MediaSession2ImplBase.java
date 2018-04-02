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

import static androidx.media.MediaPlayerBase.BUFFERING_STATE_UNKNOWN;
import static androidx.media.MediaSession2.ControllerInfo;
import static androidx.media.MediaSession2.ErrorCode;
import static androidx.media.MediaSession2.OnDataSourceMissingHelper;
import static androidx.media.MediaSession2.SessionCallback;
import static androidx.media.SessionToken2.TYPE_LIBRARY_SERVICE;
import static androidx.media.SessionToken2.TYPE_SESSION;
import static androidx.media.SessionToken2.TYPE_SESSION_SERVICE;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.os.ResultReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@TargetApi(Build.VERSION_CODES.KITKAT)
class MediaSession2ImplBase extends MediaSession2.SupportLibraryImpl {
    static final String TAG = "MS2ImplBase";
    static final boolean DEBUG = true; // TODO: Log.isLoggable(TAG, Log.DEBUG);

    private final Object mLock = new Object();

    private final Context mContext;
    private final HandlerThread mHandlerThread;
    private final Handler mHandler;
    private final MediaSessionCompat mSessionCompat;
    private final MediaSession2StubImplBase mSession2Stub;
    private final String mId;
    private final Executor mCallbackExecutor;
    private final SessionCallback mCallback;
    private final SessionToken2 mSessionToken;
    private final AudioManager mAudioManager;
    private final MediaPlayerBase.PlayerEventCallback mPlayerEventCallback;
    private final MediaPlaylistAgent.PlaylistEventCallback mPlaylistEventCallback;

    private WeakReference<MediaSession2> mInstance;

    @GuardedBy("mLock")
    private MediaPlayerBase mPlayer;
    @GuardedBy("mLock")
    private MediaPlaylistAgent mPlaylistAgent;
    @GuardedBy("mLock")
    private SessionPlaylistAgentImplBase mSessionPlaylistAgent;
    @GuardedBy("mLock")
    private VolumeProviderCompat mVolumeProvider;
    @GuardedBy("mLock")
    private OnDataSourceMissingHelper mDsmHelper;
    @GuardedBy("mLock")
    private PlaybackStateCompat mPlaybackStateCompat;
    @GuardedBy("mLock")
    private final ArrayMap<IBinder, MediaSession2.ControllerInfo> mControllers = new ArrayMap<>();
    @GuardedBy("mLock")
    private final Set<IBinder> mConnectingControllers = new HashSet<>();
    @GuardedBy("mLock")
    private final ArrayMap<MediaSession2.ControllerInfo, SessionCommandGroup2>
            mAllowedCommandGroupMap = new ArrayMap<>();

    MediaSession2ImplBase(Context context, MediaSessionCompat sessionCompat, String id,
            MediaPlayerBase player, MediaPlaylistAgent playlistAgent,
            VolumeProviderCompat volumeProvider, PendingIntent sessionActivity,
            Executor callbackExecutor, SessionCallback callback) {
        mContext = context;
        mHandlerThread = new HandlerThread("MediaController2_Thread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        mSessionCompat = sessionCompat;
        mSession2Stub = new MediaSession2StubImplBase(this);
        mSessionCompat.setCallback(mSession2Stub, mHandler);
        mSessionCompat.setSessionActivity(sessionActivity);

        mId = id;
        mCallback = callback;
        mCallbackExecutor = callbackExecutor;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // TODO: Set callback values properly
        mPlayerEventCallback = null;
        mPlaylistEventCallback = null;

        // Infer type from the id and package name.
        String libraryService = getServiceName(context, MediaLibraryService2.SERVICE_INTERFACE, id);
        String sessionService = getServiceName(context, MediaSessionService2.SERVICE_INTERFACE, id);
        if (sessionService != null && libraryService != null) {
            throw new IllegalArgumentException("Ambiguous session type. Multiple"
                    + " session services define the same id=" + id);
        } else if (libraryService != null) {
            mSessionToken = new SessionToken2(Process.myUid(), TYPE_LIBRARY_SERVICE,
                    context.getPackageName(), libraryService, id, mSessionCompat.getSessionToken());
        } else if (sessionService != null) {
            mSessionToken = new SessionToken2(Process.myUid(), TYPE_SESSION_SERVICE,
                    context.getPackageName(), sessionService, id, mSessionCompat.getSessionToken());
        } else {
            mSessionToken = new SessionToken2(Process.myUid(), TYPE_SESSION,
                    context.getPackageName(), null, id, mSessionCompat.getSessionToken());
        }
        updatePlayer(player, playlistAgent, volumeProvider);
    }

    @Override
    public void updatePlayer(@NonNull MediaPlayerBase player,
            @Nullable MediaPlaylistAgent playlistAgent,
            @Nullable VolumeProviderCompat volumeProvider) {
        if (player == null) {
            throw new IllegalArgumentException("player shouldn't be null");
        }
        final MediaPlayerBase oldPlayer;
        final MediaPlaylistAgent oldAgent;
        synchronized (mLock) {
            oldPlayer = mPlayer;
            oldAgent = mPlaylistAgent;
            mPlayer = player;
            if (playlistAgent == null) {
                mSessionPlaylistAgent = new SessionPlaylistAgentImplBase(this, mPlayer);
                if (mDsmHelper != null) {
                    mSessionPlaylistAgent.setOnDataSourceMissingHelper(mDsmHelper);
                }
                playlistAgent = mSessionPlaylistAgent;
            }
            mPlaylistAgent = playlistAgent;
            mVolumeProvider = volumeProvider;
        }
        if (player != oldPlayer) {
            player.registerPlayerEventCallback(mCallbackExecutor, mPlayerEventCallback);
            if (oldPlayer != null) {
                // Warning: Poorly implement player may ignore this
                oldPlayer.unregisterPlayerEventCallback(mPlayerEventCallback);
            }
        }
        if (playlistAgent != oldAgent) {
            playlistAgent.registerPlaylistEventCallback(mCallbackExecutor, mPlaylistEventCallback);
            if (oldAgent != null) {
                // Warning: Poorly implement player may ignore this
                oldAgent.unregisterPlaylistEventCallback(mPlaylistEventCallback);
            }
        }

        if (oldPlayer != null) {
            // TODO: implement
            //mSessionStub.notifyPlaybackInfoChanged(info);
            //notifyPlayerUpdatedNotLocked(oldPlayer);
        }
        // TODO(jaewan): Repeat the same thing for the playlist agent.
    }

    @Override
    public void close() {
        synchronized (mLock) {
            mHandler.removeCallbacksAndMessages(null);
            if (mHandlerThread.isAlive()) {
                mHandlerThread.quitSafely();
            }
        }
    }

    @Override
    public @NonNull MediaPlayerBase getPlayer() {
        synchronized (mLock) {
            return mPlayer;
        }
    }

    @Override
    public @NonNull MediaPlaylistAgent getPlaylistAgent() {
        synchronized (mLock) {
            return mPlaylistAgent;
        }
    }

    @Override
    public @Nullable VolumeProviderCompat getVolumeProvider() {
        synchronized (mLock) {
            return mVolumeProvider;
        }
    }

    @Override
    public @NonNull SessionToken2 getToken() {
        return mSessionToken;
    }

    @Override
    public @NonNull List<MediaSession2.ControllerInfo> getConnectedControllers() {
        ArrayList<MediaSession2.ControllerInfo> controllers = new ArrayList<>();
        synchronized (mLock) {
            for (int i = 0; i < mControllers.size(); i++) {
                controllers.add(mControllers.valueAt(i));
            }
        }
        return controllers;
    }

    @Override
    public void setAudioFocusRequest(@Nullable AudioFocusRequest afr) {
        // TODO(jaewan): implement this (b/72529899)
        // mProvider.setAudioFocusRequest_impl(focusGain);
    }

    @Override
    public void setCustomLayout(@NonNull ControllerInfo controller,
            @NonNull List<MediaSession2.CommandButton> layout) {
        //mProvider.setCustomLayout_impl(controller, layout);
    }

    @Override
    public void setAllowedCommands(@NonNull ControllerInfo controller,
            @NonNull SessionCommandGroup2 commands) {
        //mProvider.setAllowedCommands_impl(controller, commands);
    }

    @Override
    public void sendCustomCommand(@NonNull SessionCommand2 command, @Nullable Bundle args) {
        //mProvider.sendCustomCommand_impl(command, args);
    }

    @Override
    public void sendCustomCommand(@NonNull ControllerInfo controller,
            @NonNull SessionCommand2 command, @Nullable Bundle args,
            @Nullable ResultReceiver receiver) {
        // Equivalent to the MediaController.sendCustomCommand(Action action, ResultReceiver r);
        //mProvider.sendCustomCommand_impl(controller, command, args, receiver);
    }

    @Override
    public void play() {
        final MediaPlayerBase player;
        synchronized (mLock) {
            player = mPlayer;
        }
        if (player != null) {
            player.play();
        }
    }

    @Override
    public void pause() {
        //mProvider.pause_impl();
    }

    @Override
    public void reset() {
        //mProvider.stop_impl();
    }

    @Override
    public void prepare() {
        //mProvider.prepare_impl();
    }

    @Override
    public void seekTo(long pos) {
        //mProvider.seekTo_impl(pos);
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
    public void notifyError(@ErrorCode int errorCode, @Nullable Bundle extras) {
        //mProvider.notifyError_impl(errorCode, extras);
    }

    @Override
    public @MediaPlayerBase.PlayerState int getPlayerState() {
        MediaPlayerBase player;
        synchronized (mLock) {
            player = mPlayer;
        }
        if (player != null) {
            return player.getPlayerState();
        } else if (DEBUG) {
            Log.d(TAG, "API calls after the close()", new IllegalStateException());
        }
        return MediaPlayerBase.PLAYER_STATE_ERROR;
    }

    @Override
    public long getCurrentPosition() {
        //return mProvider.getPosition_impl();
        return 0L;
    }

    @Override
    public long getDuration() {
        // TODO: implement
        return 0;
    }

    @Override
    public long getBufferedPosition() {
        //return mProvider.getBufferedPosition_impl();
        return 0L;
    }

    @Override
    public @MediaPlayerBase.BuffState int getBufferingState() {
        // TODO(jaewan): Implement this
        return BUFFERING_STATE_UNKNOWN;
    }

    @Override
    public float getPlaybackSpeed() {
        // TODO(jaewan): implement this (b/74093080)
        return -1;
    }

    @Override
    public void setPlaybackSpeed(float speed) {
        // TODO(jaewan): implement this (b/74093080)
    }

    @Override
    public void setOnDataSourceMissingHelper(
            @NonNull OnDataSourceMissingHelper helper) {
        if (helper == null) {
            throw new IllegalArgumentException("helper shouldn't be null");
        }
        synchronized (mLock) {
            mDsmHelper = helper;
            if (mSessionPlaylistAgent != null) {
                mSessionPlaylistAgent.setOnDataSourceMissingHelper(helper);
            }
        }
    }

    @Override
    public void clearOnDataSourceMissingHelper() {
        synchronized (mLock) {
            mDsmHelper = null;
            if (mSessionPlaylistAgent != null) {
                mSessionPlaylistAgent.clearOnDataSourceMissingHelper();
            }
        }
    }

    @Override
    public List<MediaItem2> getPlaylist() {
        //return mProvider.getPlaylist_impl();
        return null;
    }

    @Override
    public void setPlaylist(@NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata) {
        //mProvider.setPlaylist_impl(list, metadata);
    }

    @Override
    public void skipToPlaylistItem(@NonNull MediaItem2 item) {
        //mProvider.skipToPlaylistItem_impl(item);
    }

    @Override
    public void skipToPreviousItem() {
        //mProvider.skipToPreviousItem_impl();
    }

    @Override
    public void skipToNextItem() {
        //mProvider.skipToNextItem_impl();
    }

    @Override
    public MediaMetadata2 getPlaylistMetadata() {
        //return mProvider.getPlaylistMetadata_impl();
        return null;
    }

    @Override
    public void addPlaylistItem(int index, @NonNull MediaItem2 item) {
        //mProvider.addPlaylistItem_impl(index, item);
    }

    @Override
    public void removePlaylistItem(@NonNull MediaItem2 item) {
        //mProvider.removePlaylistItem_impl(item);
    }

    @Override
    public void replacePlaylistItem(int index, @NonNull MediaItem2 item) {
        //mProvider.replacePlaylistItem_impl(index, item);
    }

    @Override
    public MediaItem2 getCurrentMediaItem() {
        // TODO(jaewan): Rename provider, and implement (b/74316764)
        //return mProvider.getCurrentPlaylistItem_impl();
        return null;
    }

    @Override
    public void updatePlaylistMetadata(@Nullable MediaMetadata2 metadata) {
        //mProvider.updatePlaylistMetadata_impl(metadata);
    }

    @Override
    public @MediaPlaylistAgent.RepeatMode int getRepeatMode() {
        //return mProvider.getRepeatMode_impl();
        return MediaPlaylistAgent.REPEAT_MODE_NONE;
    }

    @Override
    public void setRepeatMode(@MediaPlaylistAgent.RepeatMode int repeatMode) {
        //mProvider.setRepeatMode_impl(repeatMode);
    }

    @Override
    public @MediaPlaylistAgent.ShuffleMode int getShuffleMode() {
        //return mProvider.getShuffleMode_impl();
        return MediaPlaylistAgent.SHUFFLE_MODE_NONE;
    }

    @Override
    public void setShuffleMode(int shuffleMode) {
        //mProvider.setShuffleMode_impl(shuffleMode);
    }

    ///////////////////////////////////////////////////
    // package private and private methods
    ///////////////////////////////////////////////////

    @Override
    void setInstance(MediaSession2 session) {
        mInstance = new WeakReference<>(session);

    }

    @Override
    MediaSession2 getInstance() {
        return mInstance.get();
    }

    @Override
    Context getContext() {
        return mContext;
    }

    @Override
    Executor getCallbackExecutor() {
        return mCallbackExecutor;
    }

    @Override
    SessionCallback getCallback() {
        return mCallback;
    }

    @Override
    boolean isClosed() {
        return !mHandlerThread.isAlive();
    }

    @Override
    PlaybackStateCompat getPlaybackStateCompat() {
        synchronized (mLock) {
            return mPlaybackStateCompat;
        }
    }

    private static String getServiceName(Context context, String serviceAction, String id) {
        PackageManager manager = context.getPackageManager();
        Intent serviceIntent = new Intent(serviceAction);
        serviceIntent.setPackage(context.getPackageName());
        List<ResolveInfo> services = manager.queryIntentServices(serviceIntent,
                PackageManager.GET_META_DATA);
        String serviceName = null;
        if (services != null) {
            for (int i = 0; i < services.size(); i++) {
                String serviceId = SessionToken2.getSessionId(services.get(i));
                if (serviceId != null && TextUtils.equals(id, serviceId)) {
                    if (services.get(i).serviceInfo == null) {
                        continue;
                    }
                    if (serviceName != null) {
                        throw new IllegalArgumentException("Ambiguous session type. Multiple"
                                + " session services define the same id=" + id);
                    }
                    serviceName = services.get(i).serviceInfo.name;
                }
            }
        }
        return serviceName;
    }

    ///////////////////////////////////////////////////
    // Inner classes
    ///////////////////////////////////////////////////

    abstract static class BuilderBase
            <T extends MediaSession2, C extends SessionCallback> {
        final Context mContext;
        MediaPlayerBase mPlayer;
        String mId;
        Executor mCallbackExecutor;
        C mCallback;
        MediaPlaylistAgent mPlaylistAgent;
        VolumeProviderCompat mVolumeProvider;
        PendingIntent mSessionActivity;

        BuilderBase(Context context) {
            if (context == null) {
                throw new IllegalArgumentException("context shouldn't be null");
            }
            mContext = context;
            // Ensure MediaSessionCompat non-null or empty
            mId = TAG;
        }

        void setPlayer(@NonNull MediaPlayerBase player) {
            if (player == null) {
                throw new IllegalArgumentException("player shouldn't be null");
            }
            mPlayer = player;
        }

        void setPlaylistAgent(@NonNull MediaPlaylistAgent playlistAgent) {
            if (playlistAgent == null) {
                throw new IllegalArgumentException("playlistAgent shouldn't be null");
            }
            mPlaylistAgent = playlistAgent;
        }

        void setVolumeProvider(@Nullable VolumeProviderCompat volumeProvider) {
            mVolumeProvider = volumeProvider;
        }

        void setSessionActivity(@Nullable PendingIntent pi) {
            mSessionActivity = pi;
        }

        void setId(@NonNull String id) {
            if (id == null) {
                throw new IllegalArgumentException("id shouldn't be null");
            }
            mId = id;
        }

        void setSessionCallback(@NonNull Executor executor, @NonNull C callback) {
            if (executor == null) {
                throw new IllegalArgumentException("executor shouldn't be null");
            }
            if (callback == null) {
                throw new IllegalArgumentException("callback shouldn't be null");
            }
            mCallbackExecutor = executor;
            mCallback = callback;
        }

        abstract @NonNull T build();
    }

    static final class Builder extends
            BuilderBase<MediaSession2, MediaSession2.SessionCallback> {
        Builder(Context context) {
            super(context);
        }

        @Override
        public @NonNull MediaSession2 build() {
            if (mCallbackExecutor == null) {
                mCallbackExecutor = new MainHandlerExecutor(mContext);
            }
            if (mCallback == null) {
                mCallback = new SessionCallback() {};
            }
            return new MediaSession2(new MediaSession2ImplBase(mContext,
                    new MediaSessionCompat(mContext, mId), mId, mPlayer, mPlaylistAgent,
                    mVolumeProvider, mSessionActivity, mCallbackExecutor, mCallback));
        }
    }

    static class MainHandlerExecutor implements Executor {
        private final Handler mHandler;

        MainHandlerExecutor(Context context) {
            mHandler = new Handler(context.getMainLooper());
        }

        @Override
        public void execute(Runnable command) {
            if (!mHandler.post(command)) {
                throw new RejectedExecutionException(mHandler + " is shutting down");
            }
        }
    }
}
