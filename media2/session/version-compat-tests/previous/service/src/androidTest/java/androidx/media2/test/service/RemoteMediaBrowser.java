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

package androidx.media2.test.service;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media2.common.MediaParcelUtils;
import androidx.media2.session.MediaBrowser;
import androidx.media2.session.MediaLibraryService.LibraryParams;
import androidx.media2.session.SessionToken;

/**
 * Represents remote {@link MediaBrowser} the client app's MediaControllerService.
 * Users can run {@link MediaBrowser} methods remotely with this object.
 */
public class RemoteMediaBrowser extends RemoteMediaController {

    /**
     * Create a {@link MediaBrowser} in the client app.
     * Should NOT be called main thread.
     *
     * @param waitForConnection true if the remote browser needs to wait for the connection,
     *                          false otherwise.
     * @param connectionHints connection hints
     */
    public RemoteMediaBrowser(Context context, SessionToken token, boolean waitForConnection,
            Bundle connectionHints) {
        super(context, token, connectionHints, waitForConnection);
    }

    /**
     * {@link MediaBrowser} methods.
     */

    public void getLibraryRoot(@Nullable LibraryParams params) {
        try {
            mBinder.getLibraryRoot(mControllerId, MediaParcelUtils.toParcelable(params));
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call getLibraryRoot()");
        }
    }

    public void subscribe(@NonNull String parentId, @Nullable LibraryParams params) {
        try {
            mBinder.subscribe(mControllerId, parentId, MediaParcelUtils.toParcelable(params));
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

    public void getChildren(@NonNull String parentId, int page, int pageSize,
            @Nullable LibraryParams params) {
        try {
            mBinder.getChildren(mControllerId, parentId, page, pageSize,
                    MediaParcelUtils.toParcelable(params));
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call getChildren()");
        }
    }

    public void getItem(@NonNull String mediaId) {
        try {
            mBinder.getItem(mControllerId, mediaId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call getItem()");
        }
    }

    public void search(@NonNull String query, @Nullable LibraryParams params) {
        try {
            mBinder.search(mControllerId, query, MediaParcelUtils.toParcelable(params));
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call search()");
        }
    }

    public void getSearchResult(@NonNull String query, int page, int pageSize,
            @Nullable LibraryParams params) {
        try {
            mBinder.getSearchResult(mControllerId, query, page, pageSize,
                    MediaParcelUtils.toParcelable(params));
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call getSearchResult()");
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Non-public methods
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a {@link MediaBrowser} in the client app.
     * Should be used after successful connection through {@link #connect()}.
     *
     * @param connectionHints connection hints
     * @param waitForConnection true if this method needs to wait for the connection,
     */
    void create(SessionToken token, Bundle connectionHints, boolean waitForConnection) {
        try {
            mBinder.create(true /* isBrowser */, mControllerId,
                    MediaParcelUtils.toParcelable(token), connectionHints, waitForConnection);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to create default browser with given token.");
        }
    }
}