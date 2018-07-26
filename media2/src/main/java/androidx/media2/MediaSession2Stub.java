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

import static androidx.media2.SessionCommand2.COMMAND_CODE_CUSTOM;
import static androidx.media2.SessionCommand2.COMMAND_VERSION_CURRENT;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelUuid;
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
import androidx.media.MediaSessionManager;
import androidx.media.MediaSessionManager.RemoteUserInfo;
import androidx.media2.MediaController2.PlaybackInfo;
import androidx.media2.MediaLibraryService2.MediaLibrarySession;
import androidx.media2.MediaLibraryService2.MediaLibrarySession.MediaLibrarySessionImpl;
import androidx.media2.MediaSession2.CommandButton;
import androidx.media2.MediaSession2.ControllerCb;
import androidx.media2.MediaSession2.ControllerInfo;
import androidx.media2.MediaSession2.MediaSession2Impl;
import androidx.versionedparcelable.ParcelImpl;
import androidx.versionedparcelable.ParcelUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final SparseArray<SessionCommand2> sCommandsForOnCommandRequest =
            new SparseArray<>();

    static {
        SessionCommandGroup2 group = new SessionCommandGroup2.Builder()
                .addAllPlaybackCommands(COMMAND_VERSION_CURRENT)
                .addAllPlaylistCommands(COMMAND_VERSION_CURRENT)
                .addAllVolumeCommands(COMMAND_VERSION_CURRENT)
                .build();
        Set<SessionCommand2> commands = group.getCommands();
        for (SessionCommand2 command : commands) {
            sCommandsForOnCommandRequest.append(command.getCommandCode(), command);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ConnectedControllersManager<IBinder> mConnectedControllersManager;

    final Object mLock = new Object();

    final MediaSession2Impl mSessionImpl;
    final Context mContext;
    final MediaSessionManager mSessionManager;

    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Set<IBinder> mConnectingControllers = new HashSet<>();

    MediaSession2Stub(MediaSession2Impl sessionImpl) {
        mSessionImpl = sessionImpl;
        mContext = mSessionImpl.getContext();
        mSessionManager = MediaSessionManager.getSessionManager(mContext);
        mConnectedControllersManager = new ConnectedControllersManager<>(sessionImpl);
    }

    ConnectedControllersManager<IBinder> getConnectedControllersManager() {
        return mConnectedControllersManager;
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
        final ControllerInfo controller = mConnectedControllersManager.getController(
                caller == null ? null : caller.asBinder());
        if (mSessionImpl.isClosed() || controller == null) {
            return;
        }
        mSessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (!mConnectedControllersManager.isConnected(controller)) {
                    return;
                }
                SessionCommand2 command;
                if (sessionCommand != null) {
                    if (!mConnectedControllersManager.isAllowedCommand(
                            controller, sessionCommand)) {
                        return;
                    }
                    command = sCommandsForOnCommandRequest.get(sessionCommand.getCommandCode());
                } else {
                    if (!mConnectedControllersManager.isAllowedCommand(controller, commandCode)) {
                        return;
                    }
                    command = sCommandsForOnCommandRequest.get(commandCode);
                }
                if (command != null) {
                    boolean accepted = mSessionImpl.getCallback().onCommandRequest(
                            mSessionImpl.getInstance(), controller, command);
                    if (!accepted) {
                        // Don't run rejected command.
                        if (DEBUG) {
                            Log.d(TAG, "Command (" + command + ") from "
                                    + controller + " was rejected by " + mSessionImpl);
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
        if (!(mSessionImpl instanceof MediaLibrarySessionImpl)) {
            throw new RuntimeException("MediaSession2 cannot handle MediaLibrarySession command");
        }

        onSessionCommandInternal(caller, null, commandCode, runnable);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // AIDL methods for session overrides
    //////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void connect(final IMediaController2 caller, final String callingPackage)
            throws RuntimeException {
        RemoteUserInfo remoteUserInfo = new RemoteUserInfo(
                callingPackage, Binder.getCallingPid(), Binder.getCallingUid());
        final ControllerInfo controllerInfo = new ControllerInfo(remoteUserInfo,
                mSessionManager.isTrustedForMediaControl(remoteUserInfo),
                new Controller2Cb(caller));
        mSessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (mSessionImpl.isClosed()) {
                    return;
                }
                final IBinder callbackBinder = ((Controller2Cb) controllerInfo.getControllerCb())
                        .getCallbackBinder();
                synchronized (mLock) {
                    // Keep connecting controllers.
                    // This helps sessions to call APIs in the onConnect()
                    // (e.g. setCustomLayout()) instead of pending them.
                    mConnectingControllers.add(callbackBinder);
                }
                SessionCommandGroup2 allowedCommands = mSessionImpl.getCallback().onConnect(
                        mSessionImpl.getInstance(), controllerInfo);
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
                        mConnectingControllers.remove(callbackBinder);
                        mConnectedControllersManager.addController(
                                callbackBinder, controllerInfo, allowedCommands);
                    }
                    // If connection is accepted, notify the current state to the controller.
                    // It's needed because we cannot call synchronous calls between
                    // session/controller.
                    // Note: We're doing this after the onConnectionChanged(), but there's no
                    //       guarantee that events here are notified after the onConnected()
                    //       because IMediaController2 is oneway (i.e. async call) and Stub will
                    //       use thread poll for incoming calls.
                    final int playerState = mSessionImpl.getPlayerState();
                    final ParcelImpl currentItem = (ParcelImpl) ParcelUtils.toParcelable(
                            mSessionImpl.getCurrentMediaItem());
                    final long positionEventTimeMs = SystemClock.elapsedRealtime();
                    final long positionMs = mSessionImpl.getCurrentPosition();
                    final float playbackSpeed = mSessionImpl.getPlaybackSpeed();
                    final long bufferedPositionMs = mSessionImpl.getBufferedPosition();
                    final ParcelImpl playbackInfo =
                            (ParcelImpl) ParcelUtils.toParcelable(mSessionImpl.getPlaybackInfo());
                    final int repeatMode = mSessionImpl.getRepeatMode();
                    final int shuffleMode = mSessionImpl.getShuffleMode();
                    final PendingIntent sessionActivity = mSessionImpl.getSessionActivity();
                    final List<MediaItem2> playlist =
                            allowedCommands.hasCommand(
                                    SessionCommand2.COMMAND_CODE_PLAYLIST_GET_LIST)
                                            ? mSessionImpl.getPlaylist() : null;
                    final List<ParcelImpl> playlistParcel =
                            MediaUtils2.convertMediaItem2ListToParcelImplList(playlist);

                    // Double check if session is still there, because close() can be called in
                    // another thread.
                    if (mSessionImpl.isClosed()) {
                        return;
                    }
                    try {
                        caller.onConnected(MediaSession2Stub.this,
                                (ParcelImpl) ParcelUtils.toParcelable(allowedCommands),
                                playerState, currentItem, positionEventTimeMs, positionMs,
                                playbackSpeed, bufferedPositionMs, playbackInfo, repeatMode,
                                shuffleMode, playlistParcel, sessionActivity);
                    } catch (RemoteException e) {
                        // Controller may be died prematurely.
                    }
                } else {
                    synchronized (mLock) {
                        mConnectingControllers.remove(callbackBinder);
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
        mConnectedControllersManager.removeController(caller == null ? null : caller.asBinder());
    }

    @Override
    public void setVolumeTo(final IMediaController2 caller, final int value, final int flags)
            throws RuntimeException {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_VOLUME_SET_VOLUME,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        MediaSessionCompat sessionCompat = mSessionImpl.getSessionCompat();
                        if (sessionCompat != null) {
                            sessionCompat.getController().setVolumeTo(value, flags);
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
                        MediaSessionCompat sessionCompat = mSessionImpl.getSessionCompat();
                        if (sessionCompat != null) {
                            sessionCompat.getController().adjustVolume(direction, flags);
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
                        mSessionImpl.play();
                    }
                });
    }

    @Override
    public void pause(IMediaController2 caller) throws RuntimeException {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYBACK_PAUSE,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.pause();
                    }
                });
    }

    @Override
    public void reset(IMediaController2 caller) throws RuntimeException {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYBACK_RESET,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.reset();
                    }
                });
    }

    @Override
    public void prepare(IMediaController2 caller) throws RuntimeException {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYBACK_PREPARE,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.prepare();
                    }
                });
    }

    @Override
    public void fastForward(IMediaController2 caller) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_SESSION_FAST_FORWARD,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.getCallback().onFastForward(
                                mSessionImpl.getInstance(), controller);
                    }
                });
    }

    @Override
    public void rewind(IMediaController2 caller) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_SESSION_REWIND,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.getCallback().onRewind(mSessionImpl.getInstance(), controller);
                    }
                });
    }

    @Override
    public void seekTo(IMediaController2 caller, final long pos) throws RuntimeException {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYBACK_SEEK_TO,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.seekTo(pos);
                    }
                });
    }

    @Override
    public void sendCustomCommand(final IMediaController2 caller, final ParcelImpl command,
            final Bundle args, final ResultReceiver receiver) {
        final SessionCommand2 sessionCommand = ParcelUtils.fromParcelable(command);
        onSessionCommand(caller, sessionCommand, new SessionRunnable() {
            @Override
            public void run(final ControllerInfo controller) throws RemoteException {
                mSessionImpl.getCallback().onCustomCommand(mSessionImpl.getInstance(), controller,
                        sessionCommand, args, receiver);
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
                        mSessionImpl.getCallback().onPrepareFromUri(
                                mSessionImpl.getInstance(), controller, uri, extras);
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
                        mSessionImpl.getCallback().onPrepareFromSearch(mSessionImpl.getInstance(),
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
                        mSessionImpl.getCallback().onPrepareFromMediaId(mSessionImpl.getInstance(),
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
                        mSessionImpl.getCallback().onPlayFromUri(
                                mSessionImpl.getInstance(), controller, uri, extras);
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
                        mSessionImpl.getCallback().onPlayFromSearch(mSessionImpl.getInstance(),
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
                        mSessionImpl.getCallback().onPlayFromMediaId(
                                mSessionImpl.getInstance(), controller, mediaId, extras);
                    }
                });
    }

    @Override
    public void setRating(final IMediaController2 caller, final String mediaId,
            final ParcelImpl rating) {
        final Rating2 rating2 = ParcelUtils.fromParcelable(rating);
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_SESSION_SET_RATING,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        if (mediaId == null) {
                            Log.w(TAG, "setRating(): Ignoring null mediaId from " + controller);
                            return;
                        }
                        if (rating2 == null) {
                            Log.w(TAG,
                                    "setRating(): Ignoring null ratingBundle from " + controller);
                            return;
                        }
                        mSessionImpl.getCallback().onSetRating(
                                mSessionImpl.getInstance(), controller, mediaId, rating2);
                    }
                });
    }

    @Override
    public void setPlaybackSpeed(IMediaController2 caller, final float speed) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYBACK_SET_SPEED,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.getInstance().setPlaybackSpeed(speed);
                    }
                });
    }

    @Override
    public void setPlaylist(final IMediaController2 caller, final List<ParcelImpl> playlist,
            final Bundle metadata) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYLIST_SET_LIST,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        if (playlist == null) {
                            Log.w(TAG, "setPlaylist(): Ignoring null playlist from " + controller);
                            return;
                        }
                        mSessionImpl.getInstance().setPlaylist(
                                MediaUtils2.convertParcelImplListToMediaItem2List(playlist),
                                MediaMetadata2.fromBundle(metadata));
                    }
                });
    }

    @Override
    public void updatePlaylistMetadata(final IMediaController2 caller, final Bundle metadata) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYLIST_UPDATE_LIST_METADATA,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.getInstance().updatePlaylistMetadata(
                                MediaMetadata2.fromBundle(metadata));
                    }
                });
    }

    @Override
    public void addPlaylistItem(IMediaController2 caller, final int index,
            final ParcelImpl mediaItem) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYLIST_ADD_ITEM,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        MediaItem2 item = ParcelUtils.fromParcelable(mediaItem);
                        // Resets the UUID from the incoming media id, so controller may reuse a
                        // media item multiple times for addPlaylistItem.
                        item.mParcelUuid = new ParcelUuid(UUID.randomUUID());
                        mSessionImpl.getInstance().addPlaylistItem(index, item);
                    }
                });
    }

    @Override
    public void removePlaylistItem(IMediaController2 caller, final ParcelImpl mediaItem) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYLIST_REMOVE_ITEM,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        MediaItem2 item = ParcelUtils.fromParcelable(mediaItem);
                        // Note: MediaItem2 has hidden UUID to identify it across the processes.
                        mSessionImpl.getInstance().removePlaylistItem(item);
                    }
                });
    }

    @Override
    public void replacePlaylistItem(IMediaController2 caller, final int index,
            final ParcelImpl mediaItem) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYLIST_REPLACE_ITEM,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        MediaItem2 item = ParcelUtils.fromParcelable(mediaItem);
                        // Resets the UUID from the incoming media id, so controller may reuse a
                        // media item multiple times for replacePlaylistItem.
                        item.mParcelUuid = new ParcelUuid(UUID.randomUUID());
                        mSessionImpl.getInstance().replacePlaylistItem(index, item);
                    }
                });
    }

    @Override
    public void skipToPlaylistItem(IMediaController2 caller, final ParcelImpl mediaItem) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_TO_PLAYLIST_ITEM,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        if (mediaItem == null) {
                            Log.w(TAG, "skipToPlaylistItem(): Ignoring null mediaItem from "
                                    + controller);
                        }
                        // Note: MediaItem2 has hidden UUID to identify it across the processes.
                        mSessionImpl.getInstance().skipToPlaylistItem(
                                (MediaItem2) ParcelUtils.fromParcelable(mediaItem));
                    }
                });
    }

    @Override
    public void skipToPreviousItem(IMediaController2 caller) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_TO_PREV_ITEM,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.getInstance().skipToPreviousItem();
                    }
                });
    }

    @Override
    public void skipToNextItem(IMediaController2 caller) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_TO_NEXT_ITEM,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.getInstance().skipToNextItem();
                    }
                });
    }

    @Override
    public void setRepeatMode(IMediaController2 caller, final int repeatMode) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYLIST_SET_REPEAT_MODE,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.getInstance().setRepeatMode(repeatMode);
                    }
                });
    }

    @Override
    public void setShuffleMode(IMediaController2 caller, final int shuffleMode) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_PLAYLIST_SET_SHUFFLE_MODE,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.getInstance().setShuffleMode(shuffleMode);
                    }
                });
    }

    @Override
    public void subscribeRoutesInfo(IMediaController2 caller) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_SESSION_SUBSCRIBE_ROUTES_INFO,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.getCallback().onSubscribeRoutesInfo(
                                mSessionImpl.getInstance(), controller);
                    }
                });
    }

    @Override
    public void unsubscribeRoutesInfo(IMediaController2 caller) {
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.getCallback().onUnsubscribeRoutesInfo(
                                mSessionImpl.getInstance(), controller);
                    }
                });
    }

    @Override
    public void selectRoute(IMediaController2 caller, final Bundle route) {
        if (MediaUtils2.isUnparcelableBundle(route)) {
            throw new RuntimeException("Unexpected route bundle: " + route);
        }
        onSessionCommand(caller, SessionCommand2.COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.getCallback().onSelectRoute(mSessionImpl.getInstance(),
                                controller, route);
                    }
                });
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // AIDL methods for LibrarySession overrides
    //////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaLibrarySessionImpl getLibrarySession() {
        if (!(mSessionImpl instanceof MediaLibrarySessionImpl)) {
            throw new RuntimeException("Session cannot be casted to library session");
        }
        return (MediaLibrarySessionImpl) mSessionImpl;
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
                        if (page < 0) {
                            Log.w(TAG, "getChildren(): Ignoring negative page from " + controller);
                            return;
                        }
                        if (pageSize < 1) {
                            Log.w(TAG, "getChildren(): Ignoring pageSize less than 1 from "
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
                        if (page < 0) {
                            Log.w(TAG, "getSearchResult(): Ignoring negative page from "
                                    + controller);
                            return;
                        }
                        if (pageSize < 1) {
                            Log.w(TAG, "getSearchResult(): Ignoring pageSize less than 1 from "
                                    + controller);
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

    final class Controller2Cb extends ControllerCb {
        private final IMediaController2 mIControllerCallback;

        Controller2Cb(@NonNull IMediaController2 callback) {
            mIControllerCallback = callback;
        }

        @NonNull IBinder getCallbackBinder() {
            return mIControllerCallback.asBinder();
        }

        @Override
        void onCustomLayoutChanged(List<CommandButton> layout) throws RemoteException {
            mIControllerCallback.onCustomLayoutChanged(
                    MediaUtils2.convertCommandButtonListToParcelImplList(layout));
        }

        @Override
        void onPlaybackInfoChanged(PlaybackInfo info) throws RemoteException {
            mIControllerCallback.onPlaybackInfoChanged((ParcelImpl) ParcelUtils.toParcelable(info));
        }

        @Override
        void onAllowedCommandsChanged(SessionCommandGroup2 commands) throws RemoteException {
            mIControllerCallback.onAllowedCommandsChanged(
                    (ParcelImpl) ParcelUtils.toParcelable(commands));
        }

        @Override
        void onCustomCommand(SessionCommand2 command, Bundle args, ResultReceiver receiver)
                throws RemoteException {
            mIControllerCallback.onCustomCommand((ParcelImpl) ParcelUtils.toParcelable(command),
                    args, receiver);
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
        void onBufferingStateChanged(MediaItem2 item, int bufferingState, long bufferedPositionMs)
                throws RemoteException {
            mIControllerCallback.onBufferingStateChanged(
                    (ParcelImpl) ParcelUtils.toParcelable(item),
                    bufferingState, bufferedPositionMs);
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
            mIControllerCallback.onCurrentMediaItemChanged(
                    (ParcelImpl) ParcelUtils.toParcelable(item));
        }

        @Override
        void onPlaylistChanged(List<MediaItem2> playlist, MediaMetadata2 metadata)
                throws RemoteException {
            ControllerInfo controller = mConnectedControllersManager.getController(
                    getCallbackBinder());
            if (mConnectedControllersManager.isAllowedCommand(controller,
                    SessionCommand2.COMMAND_CODE_PLAYLIST_GET_LIST)) {
                mIControllerCallback.onPlaylistChanged(
                        MediaUtils2.convertMediaItem2ListToParcelImplList(playlist),
                        metadata == null ? null : metadata.toBundle());
            } else if (mConnectedControllersManager.isAllowedCommand(controller,
                    SessionCommand2.COMMAND_CODE_PLAYLIST_GET_LIST_METADATA)) {
                mIControllerCallback.onPlaylistMetadataChanged(metadata.toBundle());
            }
        }

        @Override
        void onPlaylistMetadataChanged(MediaMetadata2 metadata) throws RemoteException {
            ControllerInfo controller = mConnectedControllersManager.getController(
                    getCallbackBinder());
            if (mConnectedControllersManager.isAllowedCommand(controller,
                    SessionCommand2.COMMAND_CODE_PLAYLIST_GET_LIST_METADATA)) {
                mIControllerCallback.onPlaylistMetadataChanged(metadata.toBundle());
            }
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
            List<ParcelImpl> parcelList = MediaUtils2.convertMediaItem2ListToParcelImplList(result);
            mIControllerCallback.onGetChildrenDone(parentId, page, pageSize, parcelList, extras);
        }

        @Override
        void onGetItemDone(String mediaId, MediaItem2 result) throws RemoteException {
            mIControllerCallback.onGetItemDone(mediaId,
                    (ParcelImpl) ParcelUtils.toParcelable(result));
        }

        @Override
        void onSearchResultChanged(String query, int itemCount, Bundle extras)
                throws RemoteException {
            mIControllerCallback.onSearchResultChanged(query, itemCount, extras);
        }

        @Override
        void onGetSearchResultDone(String query, int page, int pageSize, List<MediaItem2> result,
                Bundle extras) throws RemoteException {
            List<ParcelImpl> parcelList = MediaUtils2.convertMediaItem2ListToParcelImplList(result);
            mIControllerCallback.onGetSearchResultDone(query, page, pageSize, parcelList, extras);
        }

        @Override
        void onDisconnected() throws RemoteException {
            mIControllerCallback.onDisconnected();
        }
    }
}
