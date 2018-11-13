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
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.media.MediaSessionManager.RemoteUserInfo;
import androidx.media2.MediaSession.ControllerInfo;
import androidx.media2.MediaSession.MediaSessionImpl;
import androidx.media2.SessionCommand.CommandCode;

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
    private final ArrayMap<ControllerInfo, SessionCommandGroup> mAllowedCommandGroupMap =
            new ArrayMap<>();
    @GuardedBy("mLock")
    private final ArrayMap<ControllerInfo, SequencedFutureManager>
            mControllerToSequencedFutureManager = new ArrayMap<>();
    @GuardedBy("mLock")
    private final ArrayMap<T, ControllerInfo> mControllers = new ArrayMap<>();
    @GuardedBy("mLock")
    private final ArrayMap<ControllerInfo, T> mKeys = new ArrayMap<>();

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final MediaSessionImpl mSessionImpl;

    ConnectedControllersManager(MediaSessionImpl session) {
        mSessionImpl = session;
    }

    public void addController(T key, ControllerInfo controller, SessionCommandGroup commands) {
        if (key == null || controller == null) {
            if (DEBUG) {
                throw new IllegalArgumentException("key nor controller shouldn't be null");
            }
            return;
        }
        synchronized (mLock) {
            mAllowedCommandGroupMap.put(controller, commands);
            mControllerToSequencedFutureManager.put(controller, new SequencedFutureManager());
            mControllers.put(key, controller);
            mKeys.put(controller, key);
        }
        // TODO: Also notify controller connected.
    }

    public void updateAllowedCommands(ControllerInfo controller, SessionCommandGroup commands) {
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
        final SequencedFutureManager manager;
        synchronized (mLock) {
            controller = mControllers.remove(key);
            mKeys.remove(controller);
            mAllowedCommandGroupMap.remove(controller);
            manager = mControllerToSequencedFutureManager.remove(controller);
        }
        if (manager != null) {
            manager.close();
        }
        notifyDisconnected(controller);
    }

    public void removeController(ControllerInfo controller) {
        if (controller == null) {
            return;
        }
        final SequencedFutureManager manager;
        synchronized (mLock) {
            T key = mKeys.remove(controller);
            mControllers.remove(key);
            mAllowedCommandGroupMap.remove(controller);
            manager = mControllerToSequencedFutureManager.remove(controller);
        }
        if (manager != null) {
            manager.close();
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
            return controller != null && mKeys.get(controller) != null;
        }
    }

    /**
     * Gets the sequenced future manager.
     *
     * @param controller controller
     * @return sequenced future manager. Can be {@code null} if the controller was null or
     *         disconencted.
     */
    public @Nullable SequencedFutureManager getSequencedFutureManager(
            @Nullable ControllerInfo controller) {
        if (controller == null) {
            return null;
        }
        synchronized (mLock) {
            return isConnected(controller)
                    ? mControllerToSequencedFutureManager.get(controller) : null;
        }
    }

    /**
     * Gets the sequenced future manager.
     *
     * @param key key
     * @return sequenced future manager. Can be {@code null} if the controller was null or
     *         disconencted.
     */
    public @Nullable SequencedFutureManager getSequencedFutureManager(@Nullable T key) {
        synchronized (mLock) {
            return getSequencedFutureManager(getController(key));
        }
    }

    public boolean isAllowedCommand(ControllerInfo controller, SessionCommand command) {
        SessionCommandGroup allowedCommands;
        synchronized (mLock) {
            allowedCommands = mAllowedCommandGroupMap.get(controller);
        }
        return allowedCommands != null && allowedCommands.hasCommand(command);
    }

    public boolean isAllowedCommand(ControllerInfo controller, @CommandCode int commandCode) {
        SessionCommandGroup allowedCommands;
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
