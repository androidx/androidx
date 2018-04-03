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
import static androidx.media.MediaConstants2.ARGUMENT_ICONTROLLER_CALLBACK;
import static androidx.media.MediaConstants2.ARGUMENT_PACKAGE_NAME;
import static androidx.media.MediaConstants2.ARGUMENT_PID;
import static androidx.media.MediaConstants2.ARGUMENT_PLAYBACK_STATE_COMPAT;
import static androidx.media.MediaConstants2.ARGUMENT_PLAYER_STATE;
import static androidx.media.MediaConstants2.ARGUMENT_PLAYLIST;
import static androidx.media.MediaConstants2.ARGUMENT_REPEAT_MODE;
import static androidx.media.MediaConstants2.ARGUMENT_SHUFFLE_MODE;
import static androidx.media.MediaConstants2.ARGUMENT_UID;
import static androidx.media.MediaConstants2.CONNECT_RESULT_CONNECTED;
import static androidx.media.MediaConstants2.CONNECT_RESULT_DISCONNECTED;
import static androidx.media.MediaConstants2.CUSTOM_COMMAND_CONNECT;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.v4.media.session.IMediaControllerCallback;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.media.MediaSession2.ControllerInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@TargetApi(Build.VERSION_CODES.KITKAT)
class MediaSession2StubImplBase extends MediaSessionCompat.Callback {
    private static final String TAG = "MS2StubImplBase";
    private static final boolean DEBUG = true; // TODO: Log.isLoggable(TAG, Log.DEBUG);

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
    public void onCommand(String command, Bundle extras, final ResultReceiver cb) {
        if (CUSTOM_COMMAND_CONNECT.equals(command)) {
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
                            mControllers.put(controllerInfo.getId(),  controllerInfo);
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
                            List<Bundle> playlistBundle = new ArrayList<>();
                            // TODO(jaewan): Find a way to avoid concurrent modification exception.
                            for (int i = 0; i < playlist.size(); i++) {
                                final MediaItem2 item = playlist.get(i);
                                if (item != null) {
                                    final Bundle itemBundle = item.toBundle();
                                    if (itemBundle != null) {
                                        playlistBundle.add(itemBundle);
                                    }
                                }
                            }
                            resultData.putParcelableArray(ARGUMENT_PLAYLIST,
                                    (Bundle[]) playlistBundle.toArray());
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
    }
}
