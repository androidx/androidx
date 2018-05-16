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

import static androidx.media.SessionCommand2.COMMAND_CODE_CUSTOM;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.media.MediaController2.PlaybackInfo;
import androidx.media.MediaLibraryService2.MediaLibrarySession;
import androidx.media.MediaLibraryService2.MediaLibrarySession.SupportLibraryImpl;
import androidx.media.MediaSession2.CommandButton;
import androidx.media.MediaSession2.ControllerCb;
import androidx.media.MediaSession2.ControllerInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles incoming commands from {@link MediaController2} and {@link MediaBrowser2}
 * to both {@link MediaSession2} and {@link MediaLibrarySession}.
 * <p>
 * We cannot create a subclass for library service specific function because AIDL doesn't support
 * subclassing and it's generated stub class is an abstract class.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
class MediaSession2Stub extends IMediaSession2.Stub {

    private static final String TAG = "MediaSession2Stub";
    private static final boolean DEBUG = true; //Log.isLoggable(TAG, Log.DEBUG);

    private static final SparseArray<SessionCommand2> sCommandsForOnCommandRequest =
            new SparseArray<>();

    static {
        SessionCommandGroup2 group = new SessionCommandGroup2();
        group.addAllPlaybackCommands();
        group.addAllPlaylistCommands();
        group.addAllVolumeCommands();
        Set<SessionCommand2> commands = group.getCommands();
        for (SessionCommand2 command : commands) {
            sCommandsForOnCommandRequest.append(command.getCommandCode(), command);
        }
    }

    private final Object mLock = new Object();

    final MediaSession2.SupportLibraryImpl mSession;
    final Context mContext;

    @GuardedBy("mLock")
    private final ArrayMap<IBinder, ControllerInfo> mControllers = new ArrayMap<>();
    @GuardedBy("mLock")
    private final Set<IBinder> mConnectingControllers = new HashSet<>();
    @GuardedBy("mLock")
    private final ArrayMap<ControllerInfo, SessionCommandGroup2> mAllowedCommandGroupMap =
            new ArrayMap<>();

    MediaSession2Stub(MediaSession2.SupportLibraryImpl session) {
        mSession = session;
        mContext = mSession.getContext();
    }

    List<ControllerInfo> getConnectedControllers() {
        ArrayList<ControllerInfo> controllers = new ArrayList<>();
        synchronized (mLock) {
            for (int i = 0; i < mControllers.size(); i++) {
                controllers.add(mControllers.valueAt(i));
            }
        }
        return controllers;
    }

    void setAllowedCommands(ControllerInfo controller, final SessionCommandGroup2 commands) {
        synchronized (mLock) {
            mAllowedCommandGroupMap.put(controller, commands);
        }
    }

    private boolean isAllowedCommand(ControllerInfo controller, SessionCommand2 command) {
        SessionCommandGroup2 allowedCommands;
        synchronized (mLock) {
            allowedCommands = mAllowedCommandGroupMap.get(controller);
        }
        return allowedCommands != null && allowedCommands.hasCommand(command);
    }

    private boolean isAllowedCommand(ControllerInfo controller, int commandCode) {
        SessionCommandGroup2 allowedCommands;
        synchronized (mLock) {
            allowedCommands = mAllowedCommandGroupMap.get(controller);
        }
        return allowedCommands != null && allowedCommands.hasCommand(commandCode);
    }

    private void onSessionCommand(@NonNull IMediaController2 caller, final int commandCode,
            @NonNull final SessionRunnable runnable) {
        onSessionCommandInternal(caller, null, commandCode, runnable);
    }

    private void onSessionCommand(@NonNull IMediaController2 caller,
            @NonNull final SessionCommand2 sessionCommand,
            @NonNull final SessionRunnable runnable) {
        onSessionCommandInternal(caller, sessionCommand, COMMAND_CODE_CUSTOM, runnable);
    }

    private void onSessionCommandInternal(@NonNull IMediaController2 caller,
            @Nullable final SessionCommand2 sessionCommand, final int commandCode,
            @NonNull final SessionRunnable runnable) {
        final ControllerInfo controller;
        synchronized (mLock) {
            controller = mControllers.get(caller);
        }
        if (mSession.isClosed() || controller == null) {
            return;
        }
        mSession.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                synchronized (mLock) {
                    if (!mControllers.containsValue(controller)) {
                        return;
                    }
                }
                SessionCommand2 command;
                if (sessionCommand != null) {
                    if (!isAllowedCommand(controller, sessionCommand)) {
                        return;
                    }
                    command = sCommandsForOnCommandRequest.get(sessionCommand.getCommandCode());
                } else {
                    if (!isAllowedCommand(controller, commandCode)) {
                        return;
                    }
                    command = sCommandsForOnCommandRequest.get(commandCode);
                }
                if (command != null) {
                    boolean accepted = mSession.getCallback().onCommandRequest(
                            mSession.getInstance(), controller, command);
                    if (!accepted) {
                        // Don't run rejected command.
                        if (DEBUG) {
                            Log.d(TAG, "Command (" + command + ") from "
                                    + controller + " was rejected by " + mSession);
                        }
                        return;
                    }
                }
                try {
                    runnable.run(controller);
                } catch (RemoteException e) {
                    // Currently it's TransactionTooLargeException or DeadSystemException.
                    // We'd better to leave log for those cases because
                    //   - TransactionTooLargeException means that we may need to fix our code.
                    //     (e.g. add pagination or special way to deliver Bitmap)
                    //   - DeadSystemException means that errors around it can be ignored.
                    Log.w(TAG, "Exception in " + controller.toString(), e);
                }
            }
        });
    }

    private void onBrowserCommand(@NonNull IMediaController2 caller, final int commandCode,
            final @NonNull SessionRunnable runnable) {
        if (!(mSession instanceof MediaLibrarySession.SupportLibraryImpl)) {
            throw new RuntimeException("MediaSession2 cannot handle MediaLibrarySession command");
        }

        onSessionCommandInternal(caller, null, commandCode, runnable);
    }

    void removeControllerInfo(ControllerInfo controller) {
        synchronized (mLock) {
            controller = mControllers.remove(controller.getId());
            if (DEBUG) {
                Log.d(TAG, "releasing " + controller);
            }
        }
    }

    private void releaseController(IMediaController2 iController) {
        final ControllerInfo controller;
        synchronized (mLock) {
            controller = mControllers.remove(iController.asBinder());
            if (DEBUG) {
                Log.d(TAG, "releasing " + controller);
            }
        }
        if (mSession.isClosed() || controller == null) {
            return;
        }
        mSession.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                mSession.getCallback().onDisconnected(mSession.getInstance(), controller);
            }
        });
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // AIDL methods for session overrides
    //////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void connect(final IMediaController2 caller, final String callingPackage)
            throws RuntimeException {
        final ControllerInfo controllerInfo = new ControllerInfo(callingPackage,
                Binder.getCallingPid(), Binder.getCallingUid(), new Controller2Cb(caller));
        mSession.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (mSession.isClosed()) {
                    return;
                }
                synchronized (mLock) {
                    // Keep connecting controllers.
                    // This helps sessions to call APIs in the onConnect()
                    // (e.g. setCustomLayout()) instead of pending them.
                    mConnectingControllers.add(controllerInfo.getId());
                }
                SessionCommandGroup2 allowedCommands = mSession.getCallback().onConnect(
                        mSession.getInstance(), controllerInfo);
                // Don't reject connection for the request from trusted app.
                // Otherwise server will fail to retrieve session's information to dispatch
                // media keys to.
                boolean accept = allowedCommands != null || controllerInfo.isTrusted();
                if (accept) {
                    if (DEBUG) {
                        Log.d(TAG, "Accepting connection, controllerInfo=" + controllerInfo
                                + " allowedCommands=" + allowedCommands);
                    }
                    if (allowedCommands == null) {
                        // For trusted apps, send non-null allowed commands to keep
                        // connection.
                        allowedCommands = new SessionCommandGroup2();
                    }
                    synchronized (mLock) {
                        mConnectingControllers.remove(controllerInfo.getId());
                        mControllers.put(controllerInfo.getId(), controllerInfo);
                        mAllowedCommandGroupMap.put(controllerInfo, allowedCommands);
                    }
                    // If connection is accepted, notify the current state to the controller.
                    // It's needed because we cannot call synchronous calls between
                    // session/controller.
                    // Note: We're doing this after the onConnectionChanged(), but there's no
                    //       guarantee that events here are notified after the onConnected()
                    //       because IMediaController2 is oneway (i.e. async call) and Stub will
                    //       use thread poll for incoming calls.
                    final int playerState = mSession.getPlayerState();
                    final Bundle currentItem = mSession.getCurrentMediaItem() == null ? null
                            : mSession.getCurrentMediaItem().toBundle();
                    final long positionEventTimeMs = SystemClock.elapsedRealtime();
                    final long positionMs = mSession.getCurrentPosition();
                    final float playbackSpeed = mSession.getPlaybackSpeed();
                    final long bufferedPositionMs = mSession.getBufferedPosition();
                    final Bundle playbackInfoBundle = mSession.getPlaybackInfo().toBundle();
                    final int repeatMode = mSession.getRepeatMode();
                    final int shuffleMode = mSession.getShuffleMode();
                    final PendingIntent sessionActivity = mSession.getSessionActivity();
                    final List<MediaItem2> playlist =
                            allowedCommands.hasCommand(
                                    SessionCommand2.COMMAND_CODE_PLAYLIST_GET_LIST)
                                            ? mSession.getPlaylist() : null;
                    final List<Bundle> playlistBundle =
                            MediaUtils2.convertMediaItem2ListToBundleList(playlist);

                    // Double check if session is still there, because close() can be called in
                    // another thread.
                    if (mSession.isClosed()) {
                        return;
                    }
                    try {
                        caller.onConnected(MediaSession2Stub.this, allowedCommands.toBundle(),
                                playerState, currentItem, positionEventTimeMs, positionMs,
                                playbackSpeed, bufferedPositionMs, playbackInfoBundle, repeatMode,
                                shuffleMode, playlistBundle, sessionActivity);
                    } catch (RemoteException e) {
                        // Controller may be died prematurely.
                    }
                } else {
                    synchronized (mLock) {
                        mConnectingControllers.remove(controllerInfo.getId());
                    }
                    if (DEBUG) {
                        Log.d(TAG, "Rejecting connection, controllerInfo=" + controllerInfo);
                    }
                    try {
                        caller.onDisconnected();
                    } catch (RemoteException e) {
                        // Controller may be died prematurely.
                        // Not an issue because we'll ignore it anyway.
                    }
                }
            }
        });
    }

    @Override
    public void release(final IMediaController2 caller) throws RemoteException {
        releaseController(caller);
    }

    @Override
    public void setVolumeTo(final IMediaController2 caller, final int value, final int flags)
            throws RuntimeException {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_VOLUME_SET_VOLUME,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        VolumeProviderCompat volumeProvider = mSession.getVolumeProvider();
                        if (volumeProvider == null) {
                            MediaSessionCompat sessionCompat = mSession.getSessionCompat();
                            if (sessionCompat != null) {
                                sessionCompat.getController().setVolumeTo(value, flags);
                            }
                        } else {
                            volumeProvider.onSetVolumeTo(value);
                        }
                    }
                });
    }

    @Override
    public void adjustVolume(IMediaController2 caller, final int direction, final int flags)
            throws RuntimeException {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_VOLUME_ADJUST_VOLUME,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        VolumeProviderCompat volumeProvider = mSession.getVolumeProvider();
                        if (volumeProvider == null) {
                            MediaSessionCompat sessionCompat = mSession.getSessionCompat();
                            if (sessionCompat != null) {
                                sessionCompat.getController().adjustVolume(
                                        direction, flags);
                            }
                        } else {
                            volumeProvider.onAdjustVolume(direction);
                        }
                    }
                });
    }

    @Override
    public void play(IMediaController2 caller) throws RuntimeException {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYBACK_PLAY,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.play();
                    }
                });
    }

    @Override
    public void pause(IMediaController2 caller) throws RuntimeException {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYBACK_PAUSE,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.pause();
                    }
                });
    }

    @Override
    public void reset(IMediaController2 caller) throws RuntimeException {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYBACK_RESET,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.reset();
                    }
                });
    }

    @Override
    public void prepare(IMediaController2 caller) throws RuntimeException {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYBACK_PREPARE,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.prepare();
                    }
                });
    }

    @Override
    public void fastForward(IMediaController2 caller) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_SESSION_FAST_FORWARD,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getCallback().onFastForward(mSession.getInstance(), controller);
                    }
                });
    }

    @Override
    public void rewind(IMediaController2 caller) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_SESSION_REWIND,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getCallback().onRewind(mSession.getInstance(), controller);
                    }
                });
    }

    @Override
    public void seekTo(IMediaController2 caller, final long pos) throws RuntimeException {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYBACK_SEEK_TO,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.seekTo(pos);
                    }
                });
    }

    @Override
    public void sendCustomCommand(final IMediaController2 caller, final Bundle commandBundle,
            final Bundle args, final ResultReceiver receiver) {
        final SessionCommand2 command = SessionCommand2.fromBundle(commandBundle);
        onSessionCommand(caller, SessionCommand2.fromBundle(commandBundle), new SessionRunnable() {
            @Override
            public void run(final ControllerInfo controller) throws RemoteException {
                mSession.getCallback().onCustomCommand(mSession.getInstance(), controller, command,
                        args, receiver);
            }
        });
    }

    @Override
    public void prepareFromUri(final IMediaController2 caller, final Uri uri,
            final Bundle extras) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_URI,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        if (uri == null) {
                            Log.w(TAG, "prepareFromUri(): Ignoring null uri from " + controller);
                            return;
                        }
                        mSession.getCallback().onPrepareFromUri(mSession.getInstance(), controller,
                                uri, extras);
                    }
                });
    }

    @Override
    public void prepareFromSearch(final IMediaController2 caller, final String query,
            final Bundle extras) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        if (TextUtils.isEmpty(query)) {
                            Log.w(TAG, "prepareFromSearch(): Ignoring empty query from "
                                    + controller);
                            return;
                        }
                        mSession.getCallback().onPrepareFromSearch(mSession.getInstance(),
                                controller, query, extras);
                    }
                });
    }

    @Override
    public void prepareFromMediaId(final IMediaController2 caller, final String mediaId,
            final Bundle extras) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        if (mediaId == null) {
                            Log.w(TAG, "prepareFromMediaId(): Ignoring null mediaId from "
                                    + controller);
                            return;
                        }
                        mSession.getCallback().onPrepareFromMediaId(mSession.getInstance(),
                                controller, mediaId, extras);
                    }
                });
    }

    @Override
    public void playFromUri(final IMediaController2 caller, final Uri uri,
            final Bundle extras) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_URI,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        if (uri == null) {
                            Log.w(TAG, "playFromUri(): Ignoring null uri from " + controller);
                            return;
                        }
                        mSession.getCallback().onPlayFromUri(mSession.getInstance(), controller,
                                uri, extras);
                    }
                });
    }

    @Override
    public void playFromSearch(final IMediaController2 caller, final String query,
            final Bundle extras) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_SEARCH,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        if (TextUtils.isEmpty(query)) {
                            Log.w(TAG, "playFromSearch(): Ignoring empty query from " + controller);
                            return;
                        }
                        mSession.getCallback().onPlayFromSearch(mSession.getInstance(),
                                controller, query, extras);
                    }
                });
    }

    @Override
    public void playFromMediaId(final IMediaController2 caller, final String mediaId,
            final Bundle extras) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        if (mediaId == null) {
                            Log.w(TAG,
                                    "playFromMediaId(): Ignoring null mediaId from " + controller);
                            return;
                        }
                        mSession.getCallback().onPlayFromMediaId(mSession.getInstance(), controller,
                                mediaId, extras);
                    }
                });
    }

    @Override
    public void setRating(final IMediaController2 caller, final String mediaId,
            final Bundle ratingBundle) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_SESSION_SET_RATING,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        if (mediaId == null) {
                            Log.w(TAG, "setRating(): Ignoring null mediaId from " + controller);
                            return;
                        }
                        if (ratingBundle == null) {
                            Log.w(TAG,
                                    "setRating(): Ignoring null ratingBundle from " + controller);
                            return;
                        }
                        Rating2 rating = Rating2.fromBundle(ratingBundle);
                        if (rating == null) {
                            if (ratingBundle == null) {
                                Log.w(TAG, "setRating(): Ignoring null rating from " + controller);
                                return;
                            }
                            return;
                        }
                        mSession.getCallback().onSetRating(mSession.getInstance(), controller,
                                mediaId,
                                rating);
                    }
                });
    }

    @Override
    public void setPlaybackSpeed(IMediaController2 caller, final float speed) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYBACK_SET_SPEED,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getInstance().setPlaybackSpeed(speed);
                    }
                });
    }

    @Override
    public void setPlaylist(final IMediaController2 caller, final List<Bundle> playlist,
            final Bundle metadata) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYLIST_SET_LIST,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        if (playlist == null) {
                            Log.w(TAG, "setPlaylist(): Ignoring null playlist from " + controller);
                            return;
                        }
                        mSession.getInstance().setPlaylist(
                                MediaUtils2.convertBundleListToMediaItem2List(playlist),
                                MediaMetadata2.fromBundle(metadata));
                    }
                });
    }

    @Override
    public void updatePlaylistMetadata(final IMediaController2 caller, final Bundle metadata) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYLIST_SET_LIST_METADATA,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getInstance().updatePlaylistMetadata(
                                MediaMetadata2.fromBundle(metadata));
                    }
                });
    }

    @Override
    public void addPlaylistItem(IMediaController2 caller, final int index, final Bundle mediaItem) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYLIST_ADD_ITEM,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        // Resets the UUID from the incoming media id, so controller may reuse a
                        // media item multiple times for addPlaylistItem.
                        mSession.getInstance().addPlaylistItem(
                                index, MediaItem2.fromBundle(mediaItem, null));
                    }
                });
    }

    @Override
    public void removePlaylistItem(IMediaController2 caller, final Bundle mediaItem) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYLIST_REMOVE_ITEM,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        MediaItem2 item = MediaItem2.fromBundle(mediaItem);
                        // Note: MediaItem2 has hidden UUID to identify it across the processes.
                        mSession.getInstance().removePlaylistItem(item);
                    }
                });
    }

    @Override
    public void replacePlaylistItem(IMediaController2 caller, final int index,
            final Bundle mediaItem) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYLIST_REPLACE_ITEM,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        // Resets the UUID from the incoming media id, so controller may reuse a
                        // media item multiple times for replacePlaylistItem.
                        mSession.getInstance().replacePlaylistItem(
                                index, MediaItem2.fromBundle(mediaItem, null));
                    }
                });
    }

    @Override
    public void skipToPlaylistItem(IMediaController2 caller, final Bundle mediaItem) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_TO_PLAYLIST_ITEM,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        if (mediaItem == null) {
                            Log.w(TAG, "skipToPlaylistItem(): Ignoring null mediaItem from "
                                    + controller);
                        }
                        // Note: MediaItem2 has hidden UUID to identify it across the processes.
                        mSession.getInstance().skipToPlaylistItem(MediaItem2.fromBundle(mediaItem));
                    }
                });
    }

    @Override
    public void skipToPreviousItem(IMediaController2 caller) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_TO_PREV_ITEM,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getInstance().skipToPreviousItem();
                    }
                });
    }

    @Override
    public void skipToNextItem(IMediaController2 caller) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_TO_NEXT_ITEM,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getInstance().skipToNextItem();
                    }
                });
    }

    @Override
    public void setRepeatMode(IMediaController2 caller, final int repeatMode) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYLIST_SET_REPEAT_MODE,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getInstance().setRepeatMode(repeatMode);
                    }
                });
    }

    @Override
    public void setShuffleMode(IMediaController2 caller, final int shuffleMode) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYLIST_SET_SHUFFLE_MODE,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getInstance().setShuffleMode(shuffleMode);
                    }
                });
    }

    @Override
    public void subscribeRoutesInfo(IMediaController2 caller) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_SESSION_SUBSCRIBE_ROUTES_INFO,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getCallback().onSubscribeRoutesInfo(mSession.getInstance(),
                                controller);
                    }
                });
    }

    @Override
    public void unsubscribeRoutesInfo(IMediaController2 caller) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getCallback().onUnsubscribeRoutesInfo(mSession.getInstance(),
                                controller);
                    }
                });
    }

    @Override
    public void selectRoute(IMediaController2 caller, final Bundle route) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getCallback().onSelectRoute(mSession.getInstance(),
                                controller, route);
                    }
                });
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // AIDL methods for LibrarySession overrides
    //////////////////////////////////////////////////////////////////////////////////////////////

    private MediaLibrarySession.SupportLibraryImpl getLibrarySession() {
        if (!(mSession instanceof MediaLibrarySession.SupportLibraryImpl)) {
            throw new RuntimeException("Session cannot be casted to library session");
        }
        return (SupportLibraryImpl) mSession;
    }

    @Override
    public void getLibraryRoot(final IMediaController2 caller, final Bundle rootHints)
            throws RuntimeException {
        onBrowserCommand(caller, SessionCommand2.COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        getLibrarySession().onGetLibraryRootOnExecutor(controller, rootHints);
                    }
                });
    }

    @Override
    public void getItem(final IMediaController2 caller, final String mediaId)
            throws RuntimeException {
        onBrowserCommand(caller, SessionCommand2.COMMAND_CODE_LIBRARY_GET_ITEM,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        if (mediaId == null) {
                            Log.w(TAG, "getItem(): Ignoring null mediaId from " + controller);
                            return;
                        }
                        getLibrarySession().onGetItemOnExecutor(controller, mediaId);
                    }
                });
    }

    @Override
    public void getChildren(final IMediaController2 caller, final String parentId,
            final int page, final int pageSize, final Bundle extras) throws RuntimeException {
        onBrowserCommand(caller, SessionCommand2.COMMAND_CODE_LIBRARY_GET_CHILDREN,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        if (parentId == null) {
                            Log.w(TAG, "getChildren(): Ignoring null parentId from " + controller);
                            return;
                        }
                        if (page < 1 || pageSize < 1) {
                            Log.w(TAG, "getChildren(): Ignoring page nor pageSize less than 1 from "
                                    + controller);
                            return;
                        }
                        getLibrarySession().onGetChildrenOnExecutor(controller, parentId, page,
                                pageSize, extras);
                    }
                });
    }

    @Override
    public void search(IMediaController2 caller, final String query, final Bundle extras) {
        onBrowserCommand(caller, SessionCommand2.COMMAND_CODE_LIBRARY_SEARCH,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        if (TextUtils.isEmpty(query)) {
                            Log.w(TAG, "search(): Ignoring empty query from " + controller);
                            return;
                        }
                        getLibrarySession().onSearchOnExecutor(controller, query, extras);
                    }
                });
    }

    @Override
    public void getSearchResult(final IMediaController2 caller, final String query,
            final int page, final int pageSize, final Bundle extras) {
        onBrowserCommand(caller, SessionCommand2.COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        if (TextUtils.isEmpty(query)) {
                            Log.w(TAG, "getSearchResult(): Ignoring empty query from "
                                    + controller);
                            return;
                        }
                        if (page < 1 || pageSize < 1) {
                            Log.w(TAG, "getSearchResult(): Ignoring page nor pageSize less than 1 "
                                    + " from " + controller);
                            return;
                        }
                        getLibrarySession().onGetSearchResultOnExecutor(controller,
                                query, page, pageSize, extras);
                    }
                });
    }

    @Override
    public void subscribe(final IMediaController2 caller, final String parentId,
            final Bundle option) {
        onBrowserCommand(caller, SessionCommand2.COMMAND_CODE_LIBRARY_SUBSCRIBE,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        if (parentId == null) {
                            Log.w(TAG, "subscribe(): Ignoring null parentId from " + controller);
                            return;
                        }
                        getLibrarySession().onSubscribeOnExecutor(controller, parentId, option);
                    }
                });
    }

    @Override
    public void unsubscribe(final IMediaController2 caller, final String parentId) {
        onBrowserCommand(caller, SessionCommand2.COMMAND_CODE_LIBRARY_UNSUBSCRIBE,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        if (parentId == null) {
                            Log.w(TAG, "unsubscribe(): Ignoring null parentId from " + controller);
                            return;
                        }
                        getLibrarySession().onUnsubscribeOnExecutor(controller, parentId);
                    }
                });
    }

    @FunctionalInterface
    private interface SessionRunnable {
        void run(ControllerInfo controller) throws RemoteException;
    }

    static final class Controller2Cb extends ControllerCb {
        private final IMediaController2 mIControllerCallback;

        Controller2Cb(@NonNull IMediaController2 callback) {
            mIControllerCallback = callback;
        }

        @Override
        @NonNull IBinder getId() {
            return mIControllerCallback.asBinder();
        }

        @Override
        void onCustomLayoutChanged(List<CommandButton> layout) throws RemoteException {
            mIControllerCallback.onCustomLayoutChanged(
                    MediaUtils2.convertCommandButtonListToBundleList(layout));
        }

        @Override
        void onPlaybackInfoChanged(PlaybackInfo info) throws RemoteException {
            mIControllerCallback.onPlaybackInfoChanged(info.toBundle());
        }

        @Override
        void onAllowedCommandsChanged(SessionCommandGroup2 commands) throws RemoteException {
            mIControllerCallback.onAllowedCommandsChanged(commands.toBundle());
        }

        @Override
        void onCustomCommand(SessionCommand2 command, Bundle args, ResultReceiver receiver)
                throws RemoteException {
            mIControllerCallback.onCustomCommand(command.toBundle(), args, receiver);
        }

        @Override
        void onPlayerStateChanged(long eventTimeMs, long positionMs, int playerState)
                throws RemoteException {
            mIControllerCallback.onPlayerStateChanged(eventTimeMs, positionMs, playerState);
        }

        @Override
        void onPlaybackSpeedChanged(long eventTimeMs, long positionMs, float speed)
                throws RemoteException {
            mIControllerCallback.onPlaybackSpeedChanged(eventTimeMs, positionMs, speed);
        }

        @Override
        void onBufferingStateChanged(MediaItem2 item, int state, long bufferedPositionMs)
                throws RemoteException {
            mIControllerCallback.onBufferingStateChanged(
                    item == null ? null : item.toBundle(), state, bufferedPositionMs);
        }

        @Override
        void onSeekCompleted(long eventTimeMs, long positionMs, long seekPositionMs)
                throws RemoteException {
            mIControllerCallback.onSeekCompleted(eventTimeMs, positionMs, seekPositionMs);
        }

        @Override
        void onError(int errorCode, Bundle extras) throws RemoteException {
            mIControllerCallback.onError(errorCode, extras);
        }

        @Override
        void onCurrentMediaItemChanged(MediaItem2 item) throws RemoteException {
            mIControllerCallback.onCurrentMediaItemChanged(item == null ? null : item.toBundle());
        }

        @Override
        void onPlaylistChanged(List<MediaItem2> playlist, MediaMetadata2 metadata)
                throws RemoteException {
            mIControllerCallback.onPlaylistChanged(
                    MediaUtils2.convertMediaItem2ListToBundleList(playlist),
                    metadata == null ? null : metadata.toBundle());
        }

        @Override
        void onPlaylistMetadataChanged(MediaMetadata2 metadata) throws RemoteException {
            mIControllerCallback.onPlaylistMetadataChanged(metadata.toBundle());
        }

        @Override
        void onShuffleModeChanged(int shuffleMode) throws RemoteException {
            mIControllerCallback.onShuffleModeChanged(shuffleMode);
        }

        @Override
        void onRepeatModeChanged(int repeatMode) throws RemoteException {
            mIControllerCallback.onRepeatModeChanged(repeatMode);
        }

        @Override
        void onRoutesInfoChanged(List<Bundle> routes) throws RemoteException {
            mIControllerCallback.onRoutesInfoChanged(routes);
        }

        @Override
        void onGetLibraryRootDone(Bundle rootHints, String rootMediaId, Bundle rootExtra)
                throws RemoteException {
            mIControllerCallback.onGetLibraryRootDone(rootHints, rootMediaId, rootExtra);
        }

        @Override
        void onChildrenChanged(String parentId, int itemCount, Bundle extras)
                throws RemoteException {
            mIControllerCallback.onChildrenChanged(parentId, itemCount, extras);
        }

        @Override
        void onGetChildrenDone(String parentId, int page, int pageSize, List<MediaItem2> result,
                Bundle extras) throws RemoteException {
            List<Bundle> bundleList = MediaUtils2.convertMediaItem2ListToBundleList(result);
            mIControllerCallback.onGetChildrenDone(parentId, page, pageSize, bundleList, extras);
        }

        @Override
        void onGetItemDone(String mediaId, MediaItem2 result) throws RemoteException {
            mIControllerCallback.onGetItemDone(mediaId, result == null ? null : result.toBundle());
        }

        @Override
        void onSearchResultChanged(String query, int itemCount, Bundle extras)
                throws RemoteException {
            mIControllerCallback.onSearchResultChanged(query, itemCount, extras);
        }

        @Override
        void onGetSearchResultDone(String query, int page, int pageSize, List<MediaItem2> result,
                Bundle extras) throws RemoteException {
            List<Bundle> bundleList = MediaUtils2.convertMediaItem2ListToBundleList(result);
            mIControllerCallback.onGetSearchResultDone(query, page, pageSize, bundleList, extras);
        }

        @Override
        void onDisconnected() throws RemoteException {
            mIControllerCallback.onDisconnected();
        }
    }
}
