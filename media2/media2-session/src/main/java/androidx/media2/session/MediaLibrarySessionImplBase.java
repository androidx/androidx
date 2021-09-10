/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.session;

import static androidx.media2.session.LibraryResult.RESULT_ERROR_UNKNOWN;
import static androidx.media2.session.LibraryResult.RESULT_SUCCESS;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.session.MediaSessionCompat.Token;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.SessionPlayer;
import androidx.media2.session.MediaLibraryService.LibraryParams;
import androidx.media2.session.MediaLibraryService.MediaLibrarySession;
import androidx.media2.session.MediaSession.ControllerCb;
import androidx.media2.session.MediaSession.ControllerInfo;
import androidx.versionedparcelable.ParcelImpl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

class MediaLibrarySessionImplBase extends MediaSessionImplBase implements
        MediaLibrarySession.MediaLibrarySessionImpl {
    private final boolean mThrowsWhenInvalidReturn;
    @GuardedBy("mLock")
    private final ArrayMap<ControllerCb, Set<String>> mSubscriptions = new ArrayMap<>();

    MediaLibrarySessionImplBase(MediaSession instance, Context context, String id,
            SessionPlayer player, PendingIntent sessionActivity, Executor callbackExecutor,
            MediaSession.SessionCallback callback, Bundle tokenExtras,
            boolean throwsWhenInvalidReturn) {
        super(instance, context, id, player, sessionActivity, callbackExecutor, callback,
                tokenExtras);
        mThrowsWhenInvalidReturn = throwsWhenInvalidReturn;
    }

    @Override
    MediaBrowserServiceCompat createLegacyBrowserServiceLocked(Context context, SessionToken token,
            Token sessionToken) {
        return new MediaLibraryServiceLegacyStub(context, this, sessionToken);
    }

    @Override
    @NonNull
    public MediaLibrarySession getInstance() {
        return (MediaLibrarySession) super.getInstance();
    }

    @Override
    public MediaLibrarySession.MediaLibrarySessionCallback getCallback() {
        return (MediaLibrarySession.MediaLibrarySessionCallback) super.getCallback();
    }

    @Override
    MediaLibraryServiceLegacyStub getLegacyBrowserService() {
        return (MediaLibraryServiceLegacyStub) super.getLegacyBrowserService();
    }

    @Override
    @NonNull
    public List<ControllerInfo> getConnectedControllers() {
        List<ControllerInfo> list = super.getConnectedControllers();
        MediaLibraryServiceLegacyStub legacyStub = getLegacyBrowserService();
        if (legacyStub != null) {
            list.addAll(legacyStub.getConnectedControllersManager()
                    .getConnectedControllers());
        }
        return list;
    }

    @Override
    public boolean isConnected(@NonNull ControllerInfo controller) {
        if (super.isConnected(controller)) {
            return true;
        }
        MediaLibraryServiceLegacyStub legacyStub = getLegacyBrowserService();
        return legacyStub != null
                ? legacyStub.getConnectedControllersManager().isConnected(controller) : false;
    }

    @Override
    public void notifyChildrenChanged(@NonNull final String parentId, final int itemCount,
            final LibraryParams params) {
        dispatchRemoteControllerTaskWithoutReturn(new RemoteControllerTask() {
            @Override
            public void run(ControllerCb callback, int seq) throws RemoteException {
                if (isSubscribed(callback, parentId)) {
                    callback.onChildrenChanged(seq, parentId, itemCount, params);
                }
            }
        });
    }

    @Override
    public void notifyChildrenChanged(@NonNull final ControllerInfo controller,
            @NonNull final String parentId, final int itemCount, final LibraryParams params) {
        dispatchRemoteControllerTaskWithoutReturn(controller, new RemoteControllerTask() {
            @Override
            public void run(ControllerCb callback, int seq) throws RemoteException {
                if (!isSubscribed(callback, parentId)) {
                    if (DEBUG) {
                        Log.d(TAG, "Skipping notifyChildrenChanged() to " + controller
                                + " because it hasn't subscribed");
                        dumpSubscription();
                    }
                    return;
                }
                callback.onChildrenChanged(seq, parentId, itemCount, params);
            }
        });
    }

    @Override
    public void notifySearchResultChanged(@NonNull ControllerInfo controller,
            @NonNull final String query, final int itemCount, final LibraryParams params) {
        dispatchRemoteControllerTaskWithoutReturn(controller, new RemoteControllerTask() {
            @Override
            public void run(ControllerCb callback, int seq) throws RemoteException {
                callback.onSearchResultChanged(seq, query, itemCount, params);
            }
        });
    }

    private LibraryResult ensureNonNullResult(LibraryResult returnedResult) {
        if (returnedResult == null) {
            handleError("LibraryResult shouldn't be null");
            returnedResult = new LibraryResult(RESULT_ERROR_UNKNOWN);
        }
        return returnedResult;
    }

    private LibraryResult ensureNonNullResultWithValidList(LibraryResult returnedResult,
            int pageSize) {
        returnedResult = ensureNonNullResult(returnedResult);
        if (returnedResult.getResultCode() == RESULT_SUCCESS) {
            List<MediaItem> items = returnedResult.getMediaItems();

            if (items == null) {
                handleError("List shouldn't be null for the success");
                return new LibraryResult(RESULT_ERROR_UNKNOWN);
            }
            if (items.size() > pageSize) {
                handleError("List shouldn't contain items more than pageSize"
                        + ", size=" + returnedResult.getMediaItems().size()
                        + ", pageSize" + pageSize);
                return new LibraryResult(RESULT_ERROR_UNKNOWN);
            }
            for (MediaItem item : items) {
                if (!isValidItem(item)) {
                    return new LibraryResult(RESULT_ERROR_UNKNOWN);
                }
            }
        }
        return returnedResult;
    }

    private LibraryResult ensureNonNullResultWithValidItem(LibraryResult returnedResult) {
        returnedResult = ensureNonNullResult(returnedResult);
        if (returnedResult.getResultCode() == RESULT_SUCCESS) {
            if (!isValidItem(returnedResult.getMediaItem())) {
                return new LibraryResult(RESULT_ERROR_UNKNOWN);
            }
        }
        return returnedResult;
    }

    private boolean isValidItem(MediaItem item) {
        if (item == null) {
            handleError("Item shouldn't be null for the success");
            return false;
        }
        if (TextUtils.isEmpty(item.getMediaId())) {
            handleError(
                    "Media ID of an item shouldn't be empty for the success");
            return false;
        }
        MediaMetadata metadata = item.getMetadata();
        if (metadata == null) {
            handleError(
                    "Metadata of an item shouldn't be null for the success");
            return false;
        }
        if (!metadata.containsKey(MediaMetadata.METADATA_KEY_BROWSABLE)) {
            handleError(
                    "METADATA_KEY_BROWSABLE should be specified in metadata of an item");
            return false;
        }
        if (!metadata.containsKey(MediaMetadata.METADATA_KEY_PLAYABLE)) {
            handleError(
                    "METADATA_KEY_PLAYABLE should be specified in metadata of an item");
            return false;
        }
        return true;
    }

    /**
     * Called by {@link MediaSessionStub#getLibraryRoot(IMediaController, int, ParcelImpl)}.
     *
     * @param controller
     * @param params
     */
    @Override
    public LibraryResult onGetLibraryRootOnExecutor(@NonNull ControllerInfo controller,
            final LibraryParams params) {
        LibraryResult result = getCallback().onGetLibraryRoot(getInstance(), controller, params);
        return ensureNonNullResultWithValidItem(result);
    }

    /**
     * Called by {@link MediaSessionStub#getItem(IMediaController, int, String)}.
     *
     * @param controller
     * @param mediaId
     */
    @Override
    public LibraryResult onGetItemOnExecutor(@NonNull ControllerInfo controller,
            @NonNull final String mediaId) {
        LibraryResult result = getCallback().onGetItem(getInstance(), controller, mediaId);
        return ensureNonNullResultWithValidItem(result);
    }

    @Override
    public LibraryResult onGetChildrenOnExecutor(@NonNull ControllerInfo controller,
            @NonNull final String parentId, final int page, final int pageSize,
            final LibraryParams params) {
        LibraryResult result = getCallback().onGetChildren(getInstance(),
                controller, parentId, page, pageSize, params);
        return ensureNonNullResultWithValidList(result, pageSize);
    }

    @Override
    public int onSubscribeOnExecutor(@NonNull ControllerInfo controller, @NonNull String parentId,
            LibraryParams params) {
        synchronized (mLock) {
            Set<String> subscription = mSubscriptions.get(controller.getControllerCb());
            if (subscription == null) {
                subscription = new HashSet<>();
                mSubscriptions.put(controller.getControllerCb(), subscription);
            }
            subscription.add(parentId);
        }
        // Call callbacks after adding it to the subscription list because library session may want
        // to call notifyChildrenChanged() in the callback.
        int resultCode = getCallback().onSubscribe(getInstance(), controller, parentId, params);

        // When error happens, remove from the subscription list.
        if (resultCode != RESULT_SUCCESS) {
            synchronized (mLock) {
                mSubscriptions.remove(controller.getControllerCb());
            }
        }
        return resultCode;
    }

    @Override
    public int onUnsubscribeOnExecutor(@NonNull ControllerInfo controller,
            @NonNull String parentId) {
        int resultCode = getCallback().onUnsubscribe(getInstance(), controller, parentId);
        synchronized (mLock) {
            mSubscriptions.remove(controller.getControllerCb());
        }
        return resultCode;
    }

    @Override
    public int onSearchOnExecutor(@NonNull ControllerInfo controller, @NonNull String query,
            LibraryParams params) {
        return getCallback().onSearch(getInstance(), controller, query, params);
    }

    @Override
    public LibraryResult onGetSearchResultOnExecutor(@NonNull ControllerInfo controller,
            @NonNull final String query, final int page, final int pageSize,
            final LibraryParams params) {
        LibraryResult result = getCallback().onGetSearchResult(getInstance(),
                controller, query, page, pageSize, params);
        return ensureNonNullResultWithValidList(result, pageSize);
    }

    @Override
    void dispatchRemoteControllerTaskWithoutReturn(@NonNull RemoteControllerTask task) {
        super.dispatchRemoteControllerTaskWithoutReturn(task);
        MediaLibraryServiceLegacyStub legacyStub = getLegacyBrowserService();
        if (legacyStub != null) {
            try {
                task.run(legacyStub.getBrowserLegacyCbForBroadcast(), /* seq= */ 0);
            } catch (RemoteException e) {
                Log.e(TAG, "Exception in using media1 API", e);
            }
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean isSubscribed(ControllerCb callback, String parentId) {
        synchronized (mLock) {
            Set<String> subscriptions = mSubscriptions.get(callback);
            if (subscriptions == null || !subscriptions.contains(parentId)) {
                return false;
            }
        }
        return true;
    }

    // Debug only
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void dumpSubscription() {
        if (!DEBUG) {
            return;
        }
        synchronized (mLock) {
            Log.d(TAG, "Dumping subscription, controller sz=" + mSubscriptions.size());
            for (int i = 0; i < mSubscriptions.size(); i++) {
                Log.d(TAG, "  controller " + mSubscriptions.valueAt(i));
                for (String parentId : mSubscriptions.valueAt(i)) {
                    Log.d(TAG, "  - " + parentId);
                }
            }
        }
    }

    private void handleError(@NonNull String message) {
        if (mThrowsWhenInvalidReturn) {
            throw new RuntimeException(message);
        } else {
            Log.e(TAG, message);
        }
    }
}
