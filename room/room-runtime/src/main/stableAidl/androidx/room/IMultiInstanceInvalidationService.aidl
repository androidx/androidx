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

import androidx.room.IMultiInstanceInvalidationCallback;

/**
 * RPC Service that controls interaction about multi-instance invalidation.
 *
 * @hide
 */
interface IMultiInstanceInvalidationService {

    /**
     * Registers a new {@link IMultiInstanceInvalidationCallback} as a client of this service.
     *
     * @param callback The RPC callback.
     * @param name The name of the database file as it is passed to {@link RoomDatabase.Builder}.
     * @return A new client ID. The client needs to hold on to this ID and pass it to the service
     *         for subsequent calls.
     */
    int registerCallback(IMultiInstanceInvalidationCallback callback, String name);

    /**
     * Unregisters the specified {@link IMultiInstanceInvalidationCallback} from this service.
     * <p>
     * Clients might die without explicitly calling this method. In that case, the service should
     * handle the clean up.
     *
     * @param callback The RPC callback.
     * @param clientId The client ID returned from {@link #registerCallback}.
     */
    void unregisterCallback(IMultiInstanceInvalidationCallback callback, int clientId);

    /**
     * Broadcasts invalidation of database tables to other clients registered to this service.
     * <p>
     * The broadcast is delivered to {@link IMultiInstanceInvalidationCallback#onInvalidation} of
     * the registered clients. The client calling this method will not receive its own broadcast.
     * Clients that are associated with a different database file will not be notified.
     *
     * @param clientId The client ID returned from {@link #registerCallback}.
     * @param tables The names of invalidated tables.
     */
    oneway void broadcastInvalidation(int clientId, in String[] tables);

}
