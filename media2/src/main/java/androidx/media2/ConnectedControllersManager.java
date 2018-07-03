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

import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.collection.ArrayMap;
import androidx.media.MediaSessionManager.RemoteUserInfo;
import androidx.media2.MediaSession2.ControllerInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages connected {@link ControllerInfo}. This is thread-safe.
 */
class ConnectedControllersManager<T> {
    private static final String TAG = "MS2ControllerMgr";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private final ArrayMap<ControllerInfo, SessionCommandGroup2> mAllowedCommandGroupMap =
            new ArrayMap<>();
    @GuardedBy("mLock")
    private final ArrayMap<T, ControllerInfo> mControllers = new ArrayMap<>();
    @GuardedBy("mLock")
    private final ArrayMap<ControllerInfo, T> mKeys = new ArrayMap<>();

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final MediaSession2.SupportLibraryImpl mSessionImpl;

    ConnectedControllersManager(MediaSession2.SupportLibraryImpl session) {
        mSessionImpl = session;
    }

    public void addController(T key, ControllerInfo controller, SessionCommandGroup2 commands) {
        if (key == null || controller == null) {
            if (DEBUG) {
                throw new IllegalArgumentException("key nor controller shouldn't be null");
            }
            return;
        }
        synchronized (mLock) {
            mAllowedCommandGroupMap.put(controller, commands);
            mControllers.put(key, controller);
            mKeys.put(controller, key);
        }
        // TODO: Also notify controller connected.
    }

    public void updateAllowedCommands(ControllerInfo controller, SessionCommandGroup2 commands) {
        synchronized (mLock) {
            if (!mAllowedCommandGroupMap.containsKey(controller)) {
                if (DEBUG) {
                    Log.d(TAG, "Cannot update allowed command for disconnected controller "
                            + controller);
                }
                return;
            }
            mAllowedCommandGroupMap.put(controller, commands);
        }
        // TODO: Also notify allowed command changes here.
    }

    public void removeController(T key) {
        if (key == null) {
            return;
        }
        final ControllerInfo controller;
        synchronized (mLock) {
            controller = mControllers.remove(key);
            mKeys.remove(controller);
            mAllowedCommandGroupMap.remove(controller);
        }
        notifyDisconnected(controller);
    }

    public void removeController(ControllerInfo controller) {
        if (controller == null) {
            return;
        }
        synchronized (mLock) {
            T key = mKeys.remove(controller);
            mControllers.remove(key);
            mAllowedCommandGroupMap.remove(controller);
        }
        notifyDisconnected(controller);
    }

    private void notifyDisconnected(final ControllerInfo controller) {
        if (DEBUG) {
            Log.d(TAG, "Controller " + controller + " is disconnected");
        }
        if (mSessionImpl.isClosed() || controller == null) {
            return;
        }
        mSessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (mSessionImpl.isClosed()) {
                    return;
                }
                mSessionImpl.getCallback().onDisconnected(mSessionImpl.getInstance(),
                        controller);
            }
        });
    }

    public List<ControllerInfo> getConnectedControllers() {
        ArrayList<ControllerInfo> controllers = new ArrayList<>();
        synchronized (mLock) {
            for (int i = 0; i < mControllers.size(); i++) {
                controllers.add(mControllers.valueAt(i));
            }
        }
        return controllers;
    }

    public boolean isConnected(ControllerInfo controller) {
        synchronized (mLock) {
            return mKeys.get(controller) != null;
        }
    }

    public boolean isAllowedCommand(ControllerInfo controller, SessionCommand2 command) {
        SessionCommandGroup2 allowedCommands;
        synchronized (mLock) {
            allowedCommands = mAllowedCommandGroupMap.get(controller);
        }
        return allowedCommands != null && allowedCommands.hasCommand(command);
    }

    public boolean isAllowedCommand(ControllerInfo controller, int commandCode) {
        SessionCommandGroup2 allowedCommands;
        synchronized (mLock) {
            allowedCommands = mAllowedCommandGroupMap.get(controller);
        }
        return allowedCommands != null && allowedCommands.hasCommand(commandCode);
    }

    public ControllerInfo getController(T key) {
        if (key == null) {
            return null;
        }
        synchronized (mLock) {
            if (key instanceof RemoteUserInfo) {
                // Workaround MediaBrowserServiceCompat's issue that that RemoteUserInfo from
                // onGetRoot and other methods are differ even for the same controller.
                // TODO: Remove this workaround
                RemoteUserInfo user = (RemoteUserInfo) key;
                for (int i = 0; i < mControllers.size(); i++) {
                    RemoteUserInfo info = (RemoteUserInfo) mControllers.keyAt(i);
                    if (user.getPackageName().equals(info.getPackageName())
                            && user.getUid() == info.getUid()) {
                        return mControllers.valueAt(i);
                    }
                }
                return null;
            }
            return mControllers.get(key);
        }
    }
}
