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

package androidx.media.test.service;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media2.MediaBrowser2;
import androidx.media2.SessionToken2;

/**
 * Represents remote {@link MediaBrowser2} the client app's MediaController2Service.
 * Users can run {@link MediaBrowser2} methods remotely with this object.
 */
public class RemoteMediaBrowser2 extends RemoteMediaController2 {

    /**
     * Create a {@link MediaBrowser2} in the client app.
     * Should NOT be called main thread.
     *
     * @param waitForConnection true if the remote browser needs to wait for the connection,
     *                          false otherwise.
     */
    public RemoteMediaBrowser2(Context context, SessionToken2 token, boolean waitForConnection) {
        super(context, token, waitForConnection);
    }

    /**
     * {@link MediaBrowser2} methods.
     */

    public void subscribe(@NonNull String parentId, @Nullable Bundle extras) {
        try {
            mBinder.subscribe(mControllerId, parentId, extras);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call subscribe()");
        }
    }

    public void unsubscribe(@NonNull String parentId) {
        try {
            mBinder.unsubscribe(mControllerId, parentId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call unsubscribe()");
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Non-public methods
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a {@link MediaBrowser2} in the client app.
     * Should be used after successful connection through {@link #connect()}.
     *
     * @param waitForConnection true if this method needs to wait for the connection,
     *                          false otherwise.
     */
    void create(SessionToken2 token, boolean waitForConnection) {
        try {
            mBinder.create(true /* isBrowser */, mControllerId, token.toBundle(),
                    waitForConnection);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to create default browser with given token.");
        }
    }
}
