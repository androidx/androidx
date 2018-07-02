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

import static androidx.media2.MediaMetadata2.METADATA_KEY_DISPLAY_TITLE;
import static androidx.media2.MediaMetadata2.METADATA_KEY_TITLE;
import static androidx.media2.SessionCommand2.COMMAND_CODE_CUSTOM;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.media.MediaSessionManager;
import androidx.media.MediaSessionManager.RemoteUserInfo;
import androidx.media2.MediaController2.PlaybackInfo;
import androidx.media2.MediaSession2.CommandButton;
import androidx.media2.MediaSession2.ControllerCb;
import androidx.media2.MediaSession2.ControllerInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@TargetApi(Build.VERSION_CODES.KITKAT)
// Getting the commands from MediaControllerCompat'
class MediaSessionLegacyStub extends MediaSessionCompat.Callback {

    private static final String TAG = "MediaSessionLegacyStub";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final SparseArray<SessionCommand2> sCommandsForOnCommandRequest =
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

    final Object mLock = new Object();

    final MediaSession2.SupportLibraryImpl mSession;
    final MediaSessionManager mSessionManager;
    final Context mContext;
    final ControllerInfo mControllerInfoForAll;

    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ArrayMap<RemoteUserInfo, ControllerInfo> mControllers = new ArrayMap<>();
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Set<IBinder> mConnectingControllers = new HashSet<>();
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ArrayMap<ControllerInfo, SessionCommandGroup2> mAllowedCommandGroupMap =
            new ArrayMap<>();

    MediaSessionLegacyStub(MediaSession2.SupportLibraryImpl session) {
        mSession = session;
        mContext = mSession.getContext();
        mSessionManager = MediaSessionManager.getSessionManager(mContext);
        mControllerInfoForAll = new ControllerInfo(
                new RemoteUserInfo(
                        RemoteUserInfo.LEGACY_CONTROLLER, Process.myPid(), Process.myUid()),
                false /* trusted */,
                new ControllerLegacyCbForAll());
    }

    @Override
    public void onCommand(@NonNull String command, @Nullable Bundle args,
            @Nullable ResultReceiver cb) {
    }

    @Override
    public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
        return false;
    }

    @Override
    public void onPrepare() {
        onSessionCommand(SessionCommand2.COMMAND_CODE_PLAYBACK_PREPARE, new SessionRunnable() {
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                mSession.prepare();
            }
        });
    }

    @Override
    public void onPrepareFromMediaId(final String mediaId, final Bundle extras) {
        onSessionCommand(SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getCallback().onPrepareFromMediaId(mSession.getInstance(),
                                controller, mediaId, extras);
                    }
                });
    }

    @Override
    public void onPrepareFromSearch(final String query, final Bundle extras) {
        onSessionCommand(SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getCallback().onPrepareFromSearch(mSession.getInstance(),
                                controller, query, extras);
                    }
                });
    }

    @Override
    public void onPrepareFromUri(final Uri uri, final Bundle extras) {
        onSessionCommand(SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_URI,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getCallback().onPrepareFromUri(mSession.getInstance(),
                                controller, uri, extras);
                    }
                });
    }

    @Override
    public void onPlay() {
        onSessionCommand(SessionCommand2.COMMAND_CODE_PLAYBACK_PLAY, new SessionRunnable() {
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                mSession.play();
            }
        });
    }

    @Override
    public void onPlayFromMediaId(final String mediaId, final Bundle extras) {
        onSessionCommand(SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getCallback().onPlayFromMediaId(mSession.getInstance(),
                                controller, mediaId, extras);
                    }
                });
    }

    @Override
    public void onPlayFromSearch(final String query, final Bundle extras) {
        onSessionCommand(SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getCallback().onPlayFromSearch(mSession.getInstance(),
                                controller, query, extras);
                    }
                });
    }

    @Override
    public void onPlayFromUri(final Uri uri, final Bundle extras) {
        onSessionCommand(SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_URI,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getCallback().onPlayFromUri(mSession.getInstance(),
                                controller, uri, extras);
                    }
                });
    }

    @Override
    public void onPause() {
        onSessionCommand(SessionCommand2.COMMAND_CODE_PLAYBACK_PAUSE, new SessionRunnable() {
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                mSession.pause();
            }
        });
    }

    @Override
    public void onStop() {
        onSessionCommand(SessionCommand2.COMMAND_CODE_PLAYBACK_RESET, new SessionRunnable() {
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                mSession.reset();
            }
        });
    }

    @Override
    public void onSeekTo(final long pos) {
        onSessionCommand(SessionCommand2.COMMAND_CODE_PLAYBACK_SEEK_TO, new SessionRunnable() {
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                mSession.seekTo(pos);
            }
        });
    }

    @Override
    public void onSkipToNext() {
    }

    @Override
    public void onSkipToPrevious() {
    }

    @Override
    public void onSkipToQueueItem(long id) {
    }

    @Override
    public void onFastForward() {
    }

    @Override
    public void onRewind() {
    }

    @Override
    public void onSetRating(@NonNull RatingCompat rating) {
    }

    @Override
    public void onCustomAction(@NonNull String action, @Nullable Bundle extras) {
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

    ControllerInfo getControllersForAll() {
        return mControllerInfoForAll;
    }

    void setAllowedCommands(ControllerInfo controller, final SessionCommandGroup2 commands) {
        synchronized (mLock) {
            mAllowedCommandGroupMap.put(controller, commands);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean isAllowedCommand(ControllerInfo controller, SessionCommand2 command) {
        SessionCommandGroup2 allowedCommands;
        synchronized (mLock) {
            allowedCommands = mAllowedCommandGroupMap.get(controller);
        }
        return allowedCommands != null && allowedCommands.hasCommand(command);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean isAllowedCommand(ControllerInfo controller, int commandCode) {
        SessionCommandGroup2 allowedCommands;
        synchronized (mLock) {
            allowedCommands = mAllowedCommandGroupMap.get(controller);
        }
        return allowedCommands != null && allowedCommands.hasCommand(commandCode);
    }

    private void onSessionCommand(final int commandCode, @NonNull final SessionRunnable runnable) {
        onSessionCommandInternal(null, commandCode, runnable);
    }

    private void onSessionCommand(@NonNull final SessionCommand2 sessionCommand,
            @NonNull final SessionRunnable runnable) {
        onSessionCommandInternal(sessionCommand, COMMAND_CODE_CUSTOM, runnable);
    }

    private void onSessionCommandInternal(@Nullable final SessionCommand2 sessionCommand,
            final int commandCode, @NonNull final SessionRunnable runnable) {
        if (mSession.isClosed()) {
            return;
        }
        RemoteUserInfo remoteUserInfo = mSession.getSessionCompat().getCurrentControllerInfo();
        final ControllerInfo controller;
        synchronized (mLock) {
            if (remoteUserInfo == null) {
                controller = null;
            } else if (mControllers.containsKey(remoteUserInfo)) {
                controller = mControllers.get(remoteUserInfo);
            } else {
                controller = new ControllerInfo(
                        remoteUserInfo,
                        mSessionManager.isTrustedForMediaControl(remoteUserInfo),
                        new ControllerLegacyCb(remoteUserInfo));
                connect(controller);
            }
        }

        mSession.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                synchronized (mLock) {
                    if (controller != null && !mControllers.containsValue(controller)) {
                        return;
                    }
                }

                SessionCommand2 command;
                if (sessionCommand == null) {
                    if (!isAllowedCommand(controller, commandCode)) {
                        return;
                    }
                    command = sCommandsForOnCommandRequest.get(commandCode);
                } else {
                    if (!isAllowedCommand(controller, sessionCommand)) {
                        return;
                    }
                    command = sessionCommand;
                }

                if (command == null) {
                    return;
                }
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

//    private void onCommand2(@NonNull IBinder caller, final int commandCode,
//            @NonNull final SessionRunnable runnable) {
//        onCommand2Internal(caller, null, commandCode, runnable);
//    }
//
//    private void onCommand2(@NonNull IBinder caller,
//            @NonNull final SessionCommand2 sessionCommand,
//            @NonNull final SessionRunnable runnable) {
//        onCommand2Internal(caller, sessionCommand, COMMGAND_CODE_CUSTOM, runnable);
//    }

//    private void onCommand2Internal(@NonNull IBinder caller,
//            @Nullable final SessionCommand2 sessionCommand, final int commandCode,
//            @NonNull final SessionRunnable runnable) {
//        final ControllerInfo controller;
//        synchronized (mLock) {
//            controller = mControllers.get(caller);
//        }
//        if (mSession == null || controller == null) {
//            return;
//        }
//        mSession.getCallbackExecutor().execute(new Runnable() {
//            @Override
//            public void run() {
//                SessionCommand2 command;
//                if (sessionCommand != null) {
//                    if (!isAllowedCommand(controller, sessionCommand)) {
//                        return;
//                    }
//                    command = sCommandsForOnCommandRequest.get(sessionCommand.getCommandCode());
//                } else {
//                    if (!isAllowedCommand(controller, commandCode)) {
//                        return;
//                    }
//                    command = sCommandsForOnCommandRequest.get(commandCode);
//                }
//                if (command != null) {
//                    boolean accepted = mSession.getCallback().onCommandRequest(
//                            mSession.getInstance(), controller, command);
//                    if (!accepted) {
//                        // Don't run rejected command.
//                        if (DEBUG) {
//                            Log.d(TAG, "Command (" + command + ") from "
//                                    + controller + " was rejected by " + mSession);
//                        }
//                        return;
//                    }
//                }
//                try {
//                    runnable.run(controller);
//                } catch (RemoteException e) {
//                    // Currently it's TransactionTooLargeException or DeadSystemException.
//                    // We'd better to leave log for those cases because
//                    //   - TransactionTooLargeException means that we may need to fix our code.
//                    //     (e.g. add pagination or special way to deliver Bitmap)
//                    //   - DeadSystemException means that errors around it can be ignored.
//                    Log.w(TAG, "Exception in " + controller.toString(), e);
//                }
//            }
//        });
//    }

//    void removeControllerInfo(ControllerInfo controller) {
//        synchronized (mLock) {
//            controller = mControllers.remove(controller.getId());
//            if (DEBUG) {
//                Log.d(TAG, "releasing " + controller);
//            }
//        }
//    }
//
//    private ControllerInfo createControllerInfo(Context context, Bundle extras) {
//        IMediaControllerCallback callback = IMediaControllerCallback.Stub.asInterface(
//                BundleCompat.getBinder(extras, ARGUMENT_ICONTROLLER_CALLBACK));
//        String packageName = extras.getString(ARGUMENT_PACKAGE_NAME);
//        int uid = extras.getInt(ARGUMENT_UID);
//        int pid = extras.getInt(ARGUMENT_PID);
//        MediaSessionManager sessionManager = MediaSessionManager.getSessionManager(context);
//        RemoteUserInfo remoteUserInfo = new RemoteUserInfo(packageName, pid, uid);
//        return new ControllerInfo(remoteUserInfo,
//                sessionManager.isTrustedForMediaControl(remoteUserInfo),
//                new ControllerLegacyCb(callback));
//    }
//
    private void connect(final ControllerInfo controller) {
        mSession.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (mSession.isClosed()) {
                    return;
                }
                SessionCommandGroup2 allowedCommands = mSession.getCallback().onConnect(
                        mSession.getInstance(), controller);
                if (allowedCommands == null) {
                    try {
                        controller.getControllerCb().onDisconnected();
                    } catch (RemoteException ex) {
                        // Controller may have died prematurely.
                    }
                    return;
                }
                synchronized (mLock) {
                    mControllers.put(controller.getRemoteUserInfo(), controller);
                    mAllowedCommandGroupMap.put(controller, allowedCommands);
                }
            }
        });
    }

//    private void disconnect(Context context, Bundle extras) {
//        final ControllerInfo controllerInfo = createControllerInfo(context, extras);
//        mSession.getCallbackExecutor().execute(new Runnable() {
//            @Override
//            public void run() {
//                if (mSession.isClosed()) {
//                    return;
//                }
//                mSession.getCallback().onDisconnected(mSession.getInstance(), controllerInfo);
//            }
//        });
//    }

    @FunctionalInterface
    private interface SessionRunnable {
        void run(ControllerInfo controller) throws RemoteException;
    }

    @SuppressWarnings("ClassCanBeStatic")
    final class ControllerLegacyCb extends ControllerCb {
        private final RemoteUserInfo mRemoteUserInfo;

        ControllerLegacyCb(RemoteUserInfo remoteUserInfo) {
            mRemoteUserInfo = remoteUserInfo;
        }

        @Override
        void onCustomLayoutChanged(List<CommandButton> layout) throws RemoteException {
//            Bundle bundle = new Bundle();
//            bundle.putParcelableArray(ARGUMENT_COMMAND_BUTTONS,
//                    MediaUtils2.convertCommandButtonListToParcelableArray(layout));
//            mIControllerCallback.onEvent(SESSION_EVENT_SET_CUSTOM_LAYOUT, bundle);
        }

        @Override
        void onPlaybackInfoChanged(PlaybackInfo info) throws RemoteException {
//            Bundle bundle = new Bundle();
//            bundle.putBundle(ARGUMENT_PLAYBACK_INFO, info.toBundle());
//            mIControllerCallback.onEvent(SESSION_EVENT_ON_PLAYBACK_INFO_CHANGED, bundle);

        }

        @Override
        void onAllowedCommandsChanged(SessionCommandGroup2 commands) throws RemoteException {
//            Bundle bundle = new Bundle();
//            bundle.putBundle(ARGUMENT_ALLOWED_COMMANDS, commands.toBundle());
//            mIControllerCallback.onEvent(SESSION_EVENT_ON_ALLOWED_COMMANDS_CHANGED, bundle);
        }

        @Override
        void onCustomCommand(SessionCommand2 command, Bundle args, ResultReceiver receiver)
                throws RemoteException {
//            Bundle bundle = new Bundle();
//            bundle.putBundle(ARGUMENT_CUSTOM_COMMAND, command.toBundle());
//            bundle.putBundle(ARGUMENT_ARGUMENTS, args);
//            bundle.putParcelable(ARGUMENT_RESULT_RECEIVER, receiver);
//            mIControllerCallback.onEvent(SESSION_EVENT_SEND_CUSTOM_COMMAND, bundle);
        }

        @Override
        void onPlayerStateChanged(long eventTimeMs, long positionMs, int playerState)
                throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onPlaybackSpeedChanged(long eventTimeMs, long positionMs, float speed)
                throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onBufferingStateChanged(MediaItem2 item, int bufferingState, long bufferedPositionMs)
                throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onSeekCompleted(long eventTimeMs, long positionMs, long position)
                throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onError(int errorCode, Bundle extras) throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onCurrentMediaItemChanged(MediaItem2 item) throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onPlaylistChanged(List<MediaItem2> playlist, MediaMetadata2 metadata)
                throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onPlaylistMetadataChanged(MediaMetadata2 metadata) throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onShuffleModeChanged(int shuffleMode) throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onRepeatModeChanged(int repeatMode) throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onRoutesInfoChanged(List<Bundle> routes) throws RemoteException {
//            Bundle bundle = null;
//            if (routes != null) {
//                bundle = new Bundle();
//                bundle.putParcelableArray(ARGUMENT_ROUTE_BUNDLE, routes.toArray(new Bundle[0]));
//            }
//            mIControllerCallback.onEvent(SESSION_EVENT_ON_ROUTES_INFO_CHANGED, bundle);
        }

        @Override
        void onGetLibraryRootDone(Bundle rootHints, String rootMediaId, Bundle rootExtra)
                throws RemoteException {
            // no-op
        }

        @Override
        void onChildrenChanged(String parentId, int itemCount, Bundle extras)
                throws RemoteException {
            // no-op
        }

        @Override
        void onGetChildrenDone(String parentId, int page, int pageSize, List<MediaItem2> result,
                Bundle extras) throws RemoteException {
            // no-op
        }

        @Override
        void onGetItemDone(String mediaId, MediaItem2 result) throws RemoteException {
            // no-op
        }

        @Override
        void onSearchResultChanged(String query, int itemCount, Bundle extras)
                throws RemoteException {
            // no-op
        }

        @Override
        void onGetSearchResultDone(String query, int page, int pageSize, List<MediaItem2> result,
                Bundle extras) throws RemoteException {
            // no-op
        }

        @Override
        void onDisconnected() throws RemoteException {
            // TODO: Find a way to disconnect a specific controller in MediaSessionCompat.
        }
    }

    final class ControllerLegacyCbForAll extends ControllerCb {
        ControllerLegacyCbForAll() {
        }

        @Override
        void onCustomLayoutChanged(List<CommandButton> layout) throws RemoteException {
//            Bundle bundle = new Bundle();
//            bundle.putParcelableArray(ARGUMENT_COMMAND_BUTTONS,
//                    MediaUtils2.convertCommandButtonListToParcelableArray(layout));
//            mIControllerCallback.onEvent(SESSION_EVENT_SET_CUSTOM_LAYOUT, bundle);
        }

        @Override
        void onPlaybackInfoChanged(PlaybackInfo info) throws RemoteException {
//            Bundle bundle = new Bundle();
//            bundle.putBundle(ARGUMENT_PLAYBACK_INFO, info.toBundle());
//            mIControllerCallback.onEvent(SESSION_EVENT_ON_PLAYBACK_INFO_CHANGED, bundle);
        }

        @Override
        void onAllowedCommandsChanged(SessionCommandGroup2 commands) throws RemoteException {
//            Bundle bundle = new Bundle();
//            bundle.putBundle(ARGUMENT_ALLOWED_COMMANDS, commands.toBundle());
//            mIControllerCallback.onEvent(SESSION_EVENT_ON_ALLOWED_COMMANDS_CHANGED, bundle);
        }

        @Override
        void onCustomCommand(SessionCommand2 command, Bundle args, ResultReceiver receiver)
                throws RemoteException {
//            Bundle bundle = new Bundle();
//            bundle.putBundle(ARGUMENT_CUSTOM_COMMAND, command.toBundle());
//            bundle.putBundle(ARGUMENT_ARGUMENTS, args);
//            bundle.putParcelable(ARGUMENT_RESULT_RECEIVER, receiver);
//            mIControllerCallback.onEvent(SESSION_EVENT_SEND_CUSTOM_COMMAND, bundle);
        }

        @Override
        void onPlayerStateChanged(long eventTimeMs, long positionMs, int playerState)
                throws RemoteException {
            // Note: This method does not use any of the given arguments.
            mSession.getSessionCompat().setPlaybackState(mSession.createPlaybackStateCompat());
        }

        @Override
        void onPlaybackSpeedChanged(long eventTimeMs, long positionMs, float speed)
                throws RemoteException {
            // Note: This method does not use any of the given arguments.
            mSession.getSessionCompat().setPlaybackState(mSession.createPlaybackStateCompat());
        }

        @Override
        void onBufferingStateChanged(MediaItem2 item, int bufferingState, long bufferedPositionMs)
                throws RemoteException {
            // Note: This method does not use any of the given arguments.
            mSession.getSessionCompat().setPlaybackState(mSession.createPlaybackStateCompat());
        }

        @Override
        void onSeekCompleted(long eventTimeMs, long positionMs, long position)
                throws RemoteException {
            // Note: This method does not use any of the given arguments.
            mSession.getSessionCompat().setPlaybackState(mSession.createPlaybackStateCompat());
        }

        @Override
        void onError(int errorCode, Bundle extras) throws RemoteException {
            PlaybackStateCompat stateWithoutError = mSession.createPlaybackStateCompat();
            // We don't set the state here as PlaybackStateCompat#STATE_ERROR, since
            // MediaSession2#notifyError() does not affect the player state.
            // This prevents MediaControllerCompat from remaining long time in error state.
            PlaybackStateCompat stateWithError = new PlaybackStateCompat.Builder(stateWithoutError)
                    .setErrorMessage(errorCode, "")
                    .setExtras(extras)
                    .build();
            mSession.getSessionCompat().setPlaybackState(stateWithError);
        }

        @Override
        void onCurrentMediaItemChanged(MediaItem2 item) throws RemoteException {
            mSession.getSessionCompat().setMetadata(item == null ? null
                    : MediaUtils2.convertToMediaMetadataCompat(item.getMetadata()));
        }

        @Override
        void onPlaylistChanged(List<MediaItem2> playlist, MediaMetadata2 metadata)
                throws RemoteException {
            mSession.getSessionCompat().setQueue(MediaUtils2.convertToQueueItemList(playlist));
            onPlaylistMetadataChanged(metadata);
        }

        @Override
        void onPlaylistMetadataChanged(MediaMetadata2 metadata) throws RemoteException {
            // Since there is no 'queue metadata', only set title of the queue.
            CharSequence oldTitle = mSession.getSessionCompat().getController().getQueueTitle();
            CharSequence newTitle = null;

            if (metadata != null) {
                newTitle = metadata.getText(METADATA_KEY_DISPLAY_TITLE);
                if (newTitle == null) {
                    newTitle = metadata.getText(METADATA_KEY_TITLE);
                }
            }

            if (!TextUtils.equals(oldTitle, newTitle)) {
                mSession.getSessionCompat().setQueueTitle(newTitle);
            }
        }

        @Override
        void onShuffleModeChanged(int shuffleMode) throws RemoteException {
            mSession.getSessionCompat().setShuffleMode(shuffleMode);
        }

        @Override
        void onRepeatModeChanged(int repeatMode) throws RemoteException {
            mSession.getSessionCompat().setRepeatMode(repeatMode);
        }

        @Override
        void onRoutesInfoChanged(List<Bundle> routes) throws RemoteException {
//            Bundle bundle = null;
//            if (routes != null) {
//                bundle = new Bundle();
//                bundle.putParcelableArray(ARGUMENT_ROUTE_BUNDLE, routes.toArray(new Bundle[0]));
//            }
//            mIControllerCallback.onEvent(SESSION_EVENT_ON_ROUTES_INFO_CHANGED, bundle);
        }

        @Override
        void onGetLibraryRootDone(Bundle rootHints, String rootMediaId, Bundle rootExtra)
                throws RemoteException {
            // no-op
        }

        @Override
        void onChildrenChanged(String parentId, int itemCount, Bundle extras)
                throws RemoteException {
            // no-op
        }

        @Override
        void onGetChildrenDone(String parentId, int page, int pageSize, List<MediaItem2> result,
                Bundle extras) throws RemoteException {
            // no-op
        }

        @Override
        void onGetItemDone(String mediaId, MediaItem2 result) throws RemoteException {
            // no-op
        }

        @Override
        void onSearchResultChanged(String query, int itemCount, Bundle extras)
                throws RemoteException {
            // no-op
        }

        @Override
        void onGetSearchResultDone(String query, int page, int pageSize, List<MediaItem2> result,
                Bundle extras) throws RemoteException {
            // no-op
        }

        @Override
        void onDisconnected() throws RemoteException {
            mSession.getSessionCompat().release();
        }
    }
}
