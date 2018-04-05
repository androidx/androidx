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

import static androidx.media.MediaConstants2.ARGUMENT_ALLOWED_COMMANDS;
import static androidx.media.MediaConstants2.ARGUMENT_ERROR_CODE;
import static androidx.media.MediaConstants2.ARGUMENT_ERROR_EXTRAS;
import static androidx.media.MediaConstants2.ARGUMENT_ICONTROLLER_CALLBACK;
import static androidx.media.MediaConstants2.ARGUMENT_PACKAGE_NAME;
import static androidx.media.MediaConstants2.ARGUMENT_PID;
import static androidx.media.MediaConstants2.ARGUMENT_PLAYBACK_STATE_COMPAT;
import static androidx.media.MediaConstants2.ARGUMENT_PLAYER_STATE;
import static androidx.media.MediaConstants2.ARGUMENT_PLAYLIST;
import static androidx.media.MediaConstants2.ARGUMENT_PLAYLIST_METADATA;
import static androidx.media.MediaConstants2.ARGUMENT_REPEAT_MODE;
import static androidx.media.MediaConstants2.ARGUMENT_SHUFFLE_MODE;
import static androidx.media.MediaConstants2.ARGUMENT_UID;
import static androidx.media.MediaConstants2.CONNECT_RESULT_CONNECTED;
import static androidx.media.MediaConstants2.CONNECT_RESULT_DISCONNECTED;
import static androidx.media.MediaConstants2.CONTROLLER_COMMAND_CONNECT;
import static androidx.media.MediaConstants2.SESSION_EVENT_NOTIFY_ERROR;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_PLAYER_STATE_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_PLAYLIST_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_PLAYLIST_METADATA_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_REPEAT_MODE_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_SHUFFLE_MODE_CHANGED;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.v4.media.session.IMediaControllerCallback;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.media.MediaSession2.ControllerInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@TargetApi(Build.VERSION_CODES.KITKAT)
class MediaSession2StubImplBase extends MediaSessionCompat.Callback {
    private static final String TAG = "MS2StubImplBase";
    private static final boolean DEBUG = true; // TODO: Log.isLoggable(TAG, Log.DEBUG);

    private static final SparseArray<SessionCommand2> sCommandsForOnCommandRequest =
            new SparseArray<>();
    static {
        SessionCommandGroup2 group = new SessionCommandGroup2();
        group.addAllPlaybackCommands();
        group.addAllPlaylistCommands();
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

    MediaSession2StubImplBase(MediaSession2.SupportLibraryImpl session) {
        mSession = session;
        mContext = mSession.getContext();
    }

    @Override
    public void onPrepare() {
        mSession.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (mSession.isClosed()) {
                    return;
                }
                mSession.prepare();
            }
        });
    }

    @Override
    public void onPlay() {
        mSession.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (mSession.isClosed()) {
                    return;
                }
                mSession.play();
            }
        });
    }

    @Override
    public void onPause() {
        mSession.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (mSession.isClosed()) {
                    return;
                }
                mSession.pause();
            }
        });
    }

    @Override
    public void onStop() {
        mSession.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (mSession.isClosed()) {
                    return;
                }
                mSession.reset();
            }
        });
    }

    @Override
    public void onSeekTo(final long pos) {
        mSession.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (mSession.isClosed()) {
                    return;
                }
                mSession.seekTo(pos);
            }
        });
    }

    @Override
    public void onCommand(String command, Bundle extras, final ResultReceiver cb) {
        switch (command) {
            case CONTROLLER_COMMAND_CONNECT:
                connect(extras, cb);
                break;
        }
    }

    void notifyPlayerStateChanged(final int state) {
        notifyAll(new Session2Runnable() {
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                Bundle bundle = new Bundle();
                bundle.putInt(ARGUMENT_PLAYER_STATE, state);
                controller.getControllerBinder().onEvent(
                        SESSION_EVENT_ON_PLAYER_STATE_CHANGED, bundle);
            }
        });
    }

    void notifyError(final int errorCode, final Bundle extras) {
        notifyAll(new Session2Runnable() {
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                Bundle bundle = new Bundle();
                bundle.putInt(ARGUMENT_ERROR_CODE, errorCode);
                bundle.putBundle(ARGUMENT_ERROR_EXTRAS, extras);
                controller.getControllerBinder().onEvent(SESSION_EVENT_NOTIFY_ERROR, bundle);
            }
        });
    }

    void notifyPlaylistChanged(final List<MediaItem2> playlist,
            final MediaMetadata2 metadata) {
        notifyAll(SessionCommand2.COMMAND_CODE_PLAYLIST_GET_LIST, new Session2Runnable() {
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                Bundle bundle = new Bundle();
                bundle.putParcelableArray(ARGUMENT_PLAYLIST,
                        MediaUtils2.toMediaItem2BundleArray(playlist));
                bundle.putBundle(ARGUMENT_PLAYLIST_METADATA,
                        metadata == null ? null : metadata.toBundle());
                controller.getControllerBinder().onEvent(
                        SESSION_EVENT_ON_PLAYLIST_CHANGED, bundle);
            }
        });
    }

    void notifyPlaylistMetadataChanged(final MediaMetadata2 metadata) {
        notifyAll(SessionCommand2.COMMAND_CODE_PLAYLIST_GET_LIST_METADATA, new Session2Runnable() {
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                Bundle bundle = new Bundle();
                bundle.putBundle(ARGUMENT_PLAYLIST_METADATA,
                        metadata == null ? null : metadata.toBundle());
                controller.getControllerBinder().onEvent(
                        SESSION_EVENT_ON_PLAYLIST_METADATA_CHANGED, bundle);
            }
        });
    }

    void notifyRepeatModeChanged(final int repeatMode) {
        notifyAll(new Session2Runnable() {
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                Bundle bundle = new Bundle();
                bundle.putInt(ARGUMENT_REPEAT_MODE, repeatMode);
                controller.getControllerBinder().onEvent(
                        SESSION_EVENT_ON_REPEAT_MODE_CHANGED, bundle);
            }
        });
    }

    void notifyShuffleModeChanged(final int shuffleMode) {
        notifyAll(new Session2Runnable() {
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                Bundle bundle = new Bundle();
                bundle.putInt(ARGUMENT_SHUFFLE_MODE, shuffleMode);
                controller.getControllerBinder().onEvent(
                        SESSION_EVENT_ON_SHUFFLE_MODE_CHANGED, bundle);
            }
        });
    }

    private List<ControllerInfo> getControllers() {
        ArrayList<ControllerInfo> controllers = new ArrayList<>();
        synchronized (mLock) {
            for (int i = 0; i < mControllers.size(); i++) {
                controllers.add(mControllers.valueAt(i));
            }
        }
        return controllers;
    }

    private void notifyAll(@NonNull Session2Runnable runnable) {
        List<ControllerInfo> controllers = getControllers();
        for (int i = 0; i < controllers.size(); i++) {
            notifyInternal(controllers.get(i), runnable);
        }
    }

    private void notifyAll(int commandCode, @NonNull Session2Runnable runnable) {
        List<ControllerInfo> controllers = getControllers();
        for (int i = 0; i < controllers.size(); i++) {
            ControllerInfo controller = controllers.get(i);
            if (isAllowedCommand(controller, commandCode)) {
                notifyInternal(controller, runnable);
            }
        }
    }

    // Do not call this API directly. Use notify() instead.
    private void notifyInternal(@NonNull ControllerInfo controller,
            @NonNull Session2Runnable runnable) {
        if (controller == null || controller.getControllerBinder() == null) {
            return;
        }
        try {
            runnable.run(controller);
        } catch (DeadObjectException e) {
            if (DEBUG) {
                Log.d(TAG, controller.toString() + " is gone", e);
            }
            onControllerClosed(controller.getControllerBinder());
        } catch (RemoteException e) {
            // Currently it's TransactionTooLargeException or DeadSystemException.
            // We'd better to leave log for those cases because
            //   - TransactionTooLargeException means that we may need to fix our code.
            //     (e.g. add pagination or special way to deliver Bitmap)
            //   - DeadSystemException means that errors around it can be ignored.
            Log.w(TAG, "Exception in " + controller.toString(), e);
        }
    }

    private boolean isAllowedCommand(ControllerInfo controller, int commandCode) {
        SessionCommandGroup2 allowedCommands;
        synchronized (mLock) {
            allowedCommands = mAllowedCommandGroupMap.get(controller);
        }
        return allowedCommands != null && allowedCommands.hasCommand(commandCode);
    }

    private void onCommand(@NonNull IBinder caller, final int commandCode,
            @NonNull final Session2Runnable runnable) {
        final ControllerInfo controller;
        synchronized (mLock) {
            controller = mControllers.get(caller);
        }
        if (mSession == null || controller == null) {
            return;
        }
        mSession.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (!MediaSession2StubImplBase.this.isAllowedCommand(controller, commandCode)) {
                    return;
                }
                SessionCommand2 command = sCommandsForOnCommandRequest.get(commandCode);
                if (command != null) {
                    boolean accepted = mSession.getCallback().onCommandRequest(
                            mSession.getInstance(), controller, command);
                    if (!accepted) {
                        // Don't run rejected command.
                        if (DEBUG) {
                            Log.d(TAG, "Command (code=" + commandCode + ") from "
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

    private void onControllerClosed(IMediaControllerCallback iController) {
        ControllerInfo controller;
        synchronized (mLock) {
            controller = mControllers.remove(iController.asBinder());
            if (DEBUG) {
                Log.d(TAG, "releasing " + controller);
            }
        }
        if (controller == null) {
            return;
        }
        final ControllerInfo removedController = controller;
        mSession.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                mSession.getCallback().onDisconnected(mSession.getInstance(), removedController);
            }
        });
    }

    private void connect(Bundle extras, final ResultReceiver cb) {
        IMediaControllerCallback callback = IMediaControllerCallback.Stub.asInterface(
                extras.getBinder(ARGUMENT_ICONTROLLER_CALLBACK));
        String packageName = extras.getString(ARGUMENT_PACKAGE_NAME);
        int uid = extras.getInt(ARGUMENT_UID);
        int pid = extras.getInt(ARGUMENT_PID);
        // TODO: sanity check for packageName, uid, and pid.

        final ControllerInfo controllerInfo =
                new ControllerInfo(mContext, uid, pid, packageName, callback);
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
                    // If connection is accepted, notify the current state to the
                    // controller. It's needed because we cannot call synchronous calls
                    // between session/controller.
                    // Note: We're doing this after the onConnectionChanged(), but there's
                    //       no guarantee that events here are notified after the
                    //       onConnected() because IMediaController2 is oneway (i.e. async
                    //       call) and Stub will use thread poll for incoming calls.
                    final Bundle resultData = new Bundle();
                    resultData.putBundle(ARGUMENT_ALLOWED_COMMANDS,
                            allowedCommands.toBundle());
                    resultData.putInt(ARGUMENT_PLAYER_STATE, mSession.getPlayerState());
                    synchronized (mLock) {
                        resultData.putParcelable(ARGUMENT_PLAYBACK_STATE_COMPAT,
                                mSession.getPlaybackStateCompat());
                        // TODO: Insert MediaMetadataCompat
                    }
                    resultData.putInt(ARGUMENT_REPEAT_MODE, mSession.getRepeatMode());
                    resultData.putInt(ARGUMENT_SHUFFLE_MODE, mSession.getShuffleMode());
                    final List<MediaItem2> playlist = allowedCommands.hasCommand(
                            SessionCommand2.COMMAND_CODE_PLAYLIST_GET_LIST)
                            ? mSession.getPlaylist() : null;
                    if (playlist != null) {
                        resultData.putParcelableArray(ARGUMENT_PLAYLIST,
                                MediaUtils2.toMediaItem2BundleArray(playlist));
                    }

                    // Double check if session is still there, because close() can be
                    // called in another thread.
                    if (mSession.isClosed()) {
                        return;
                    }
                    cb.send(CONNECT_RESULT_CONNECTED, resultData);
                } else {
                    synchronized (mLock) {
                        mConnectingControllers.remove(controllerInfo.getId());
                    }
                    if (DEBUG) {
                        Log.d(TAG, "Rejecting connection, controllerInfo=" + controllerInfo);
                    }
                    cb.send(CONNECT_RESULT_DISCONNECTED, null);
                }
            }
        });
    }

    @FunctionalInterface
    private interface Session2Runnable {
        void run(ControllerInfo controller) throws RemoteException;
    }
}
