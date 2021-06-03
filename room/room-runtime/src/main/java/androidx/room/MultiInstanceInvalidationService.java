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

package androidx.room;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.HashMap;

/**
 * A {@link Service} for remote invalidation among multiple {@link InvalidationTracker} instances.
 * This service runs in the main app process. All the instances of {@link InvalidationTracker}
 * (potentially in other processes) has to connect to this service.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class MultiInstanceInvalidationService extends Service {

    // synthetic access
    @SuppressWarnings("WeakerAccess")
    int mMaxClientId = 0;

    // synthetic access
    @SuppressWarnings("WeakerAccess")
    final HashMap<Integer, String> mClientNames = new HashMap<>();

    // synthetic access
    @SuppressWarnings("WeakerAccess")
    final RemoteCallbackList<IMultiInstanceInvalidationCallback> mCallbackList =
            new RemoteCallbackList<IMultiInstanceInvalidationCallback>() {
                @Override
                public void onCallbackDied(IMultiInstanceInvalidationCallback callback,
                        Object cookie) {
                    mClientNames.remove((int) cookie);
                }
            };

    private final IMultiInstanceInvalidationService.Stub mBinder =
            new IMultiInstanceInvalidationService.Stub() {

                // Assigns a client ID to the client.
                @Override
                public int registerCallback(IMultiInstanceInvalidationCallback callback,
                        String name) {
                    if (name == null) {
                        return 0;
                    }
                    synchronized (mCallbackList) {
                        int clientId = ++mMaxClientId;
                        // Use the client ID as the RemoteCallbackList cookie.
                        if (mCallbackList.register(callback, clientId)) {
                            mClientNames.put(clientId, name);
                            return clientId;
                        } else {
                            --mMaxClientId;
                            return 0;
                        }
                    }
                }

                // Explicitly removes the client.
                // The client can die without calling this. In that case, mCallbackList
                // .onCallbackDied() can take care of removal.
                @Override
                public void unregisterCallback(IMultiInstanceInvalidationCallback callback,
                        int clientId) {
                    synchronized (mCallbackList) {
                        mCallbackList.unregister(callback);
                        mClientNames.remove(clientId);
                    }
                }

                // Broadcasts table invalidation to other instances of the same database file.
                // The broadcast is not sent to the caller itself.
                @Override
                public void broadcastInvalidation(int clientId, String[] tables) {
                    synchronized (mCallbackList) {
                        String name = mClientNames.get(clientId);
                        if (name == null) {
                            Log.w(Room.LOG_TAG, "Remote invalidation client ID not registered");
                            return;
                        }
                        int count = mCallbackList.beginBroadcast();
                        try {
                            for (int i = 0; i < count; i++) {
                                int targetClientId = (int) mCallbackList.getBroadcastCookie(i);
                                String targetName = mClientNames.get(targetClientId);
                                if (clientId == targetClientId // This is the caller itself.
                                        || !name.equals(targetName)) { // Not the same file.
                                    continue;
                                }
                                try {
                                    IMultiInstanceInvalidationCallback callback =
                                            mCallbackList.getBroadcastItem(i);
                                    callback.onInvalidation(tables);
                                } catch (RemoteException e) {
                                    Log.w(Room.LOG_TAG, "Error invoking a remote callback", e);
                                }
                            }
                        } finally {
                            mCallbackList.finishBroadcast();
                        }
                    }
                }
            };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
