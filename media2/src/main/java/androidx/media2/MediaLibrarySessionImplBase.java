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

import static androidx.media2.MediaLibraryService.LibraryResult.RESULT_CODE_SUCCESS;
import static androidx.media2.MediaLibraryService.LibraryResult.RESULT_CODE_UNKNOWN_ERROR;

import android.app.PendingIntent;
import android.content.Context;
import android.os.RemoteException;
import android.support.v4.media.session.MediaSessionCompat.Token;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.collection.ArrayMap;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media2.MediaLibraryService.LibraryParams;
import androidx.media2.MediaLibraryService.LibraryResult;
import androidx.media2.MediaLibraryService.MediaLibrarySession;
import androidx.media2.MediaLibraryService.MediaLibrarySession.MediaLibrarySessionCallback;
import androidx.media2.MediaLibraryService.MediaLibrarySession.MediaLibrarySessionImpl;
import androidx.media2.MediaSession.ControllerCb;
import androidx.media2.MediaSession.ControllerInfo;
import androidx.media2.MediaSession.SessionCallback;
import androidx.versionedparcelable.ParcelImpl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

class MediaLibrarySessionImplBase extends MediaSessionImplBase implements MediaLibrarySessionImpl {
    @GuardedBy("mLock")
    private final ArrayMap<ControllerCb, Set<String>> mSubscriptions = new ArrayMap<>();

    MediaLibrarySessionImplBase(MediaSession instance, Context context, String id,
            SessionPlayer player, PendingIntent sessionActivity, Executor callbackExecutor,
            SessionCallback callback) {
        super(instance, context, id, player, sessionActivity, callbackExecutor, callback);
    }

    @Override
    MediaBrowserServiceCompat createLegacyBrowserService(Context context, SessionToken token,
            Token sessionToken) {
        return new MediaLibraryServiceLegacyStub(context, this, sessionToken);
    }

    @Override
    public MediaLibrarySession getInstance() {
        return (MediaLibrarySession) super.getInstance();
    }

    @Override
    public MediaLibrarySessionCallback getCallback() {
        return (MediaLibrarySessionCallback) super.getCallback();
    }

    @Override
    MediaLibraryServiceLegacyStub getLegacyBrowserService() {
        return (MediaLibraryServiceLegacyStub) super.getLegacyBrowserService();
    }

    @Override
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
    public boolean isConnected(ControllerInfo controller) {
        if (super.isConnected(controller)) {
            return true;
        }
        MediaLibraryServiceLegacyStub legacyStub = getLegacyBrowserService();
        return legacyStub != null
                ? legacyStub.getConnectedControllersManager().isConnected(controller) : false;
    }

    @Override
    public void notifyChildrenChanged(final String parentId, final int itemCount,
            final LibraryParams params) {
        dispatchRemoteControllerCallbackTask(new RemoteControllerCallbackTask() {
            @Override
            public void run(ControllerCb callback) throws RemoteException {
                if (isSubscribed(callback, parentId)) {
                    callback.onChildrenChanged(parentId, itemCount, params);
                }
            }
        });
    }

    @Override
    public void notifyChildrenChanged(final ControllerInfo controller, final String parentId,
            final int itemCount, final LibraryParams params) {
        dispatchRemoteControllerCallbackTask(controller, new RemoteControllerCallbackTask() {
            @Override
            public void run(ControllerCb callback) throws RemoteException {
                if (!isSubscribed(callback, parentId)) {
                    if (DEBUG) {
                        Log.d(TAG, "Skipping notifyChildrenChanged() to " + controller
                                + " because it hasn't subscribed");
                        dumpSubscription();
                    }
                    return;
                }
                callback.onChildrenChanged(parentId, itemCount, params);
            }
        });
    }

    @Override
    public void notifySearchResultChanged(ControllerInfo controller, final String query,
            final int itemCount, final LibraryParams params) {
        dispatchRemoteControllerCallbackTask(controller, new RemoteControllerCallbackTask() {
            @Override
            public void run(ControllerCb callback) throws RemoteException {
                callback.onSearchResultChanged(query, itemCount, params);
            }
        });
    }

    private LibraryResult ensureNonNullResult(LibraryResult returnedResult) {
        if (returnedResult == null) {
            throw new RuntimeException("LibraryResult shouldn't be null");
        }
        return returnedResult;
    }

    private LibraryResult ensureNonNullResultWithValidList(LibraryResult returnedResult,
            int pageSize) {
        returnedResult = ensureNonNullResult(returnedResult);
        if (returnedResult.getResultCode() == RESULT_CODE_SUCCESS) {
            List<MediaItem> items = returnedResult.getMediaItems();

            if (items == null) {
                throw new RuntimeException("List shouldn't be null for the success");
            }
            if (items.size() > pageSize) {
                throw new RuntimeException("List shouldn't contain items more than pageSize"
                        + ", size=" + returnedResult.getMediaItems().size()
                        + ", pageSize" + pageSize);
            }
            for (MediaItem item : items) {
                if (!isValidItem(item)) {
                    return new LibraryResult(RESULT_CODE_UNKNOWN_ERROR);
                }
            }
        }
        return returnedResult;
    }

    private LibraryResult ensureNonNullResultWithValidItem(LibraryResult returnedResult) {
        returnedResult = ensureNonNullResult(returnedResult);
        if (returnedResult.getResultCode() == RESULT_CODE_SUCCESS) {
            if (!isValidItem(returnedResult.getMediaItem())) {
                return new LibraryResult(RESULT_CODE_UNKNOWN_ERROR);
            }
        }
        return returnedResult;
    }

    private boolean isValidItem(MediaItem item) {
        if (item == null) {
            throw new RuntimeException("Item shouldn't be null for the success");
        }
        if (TextUtils.isEmpty(item.getMediaId())) {
            throw new RuntimeException(
                    "Media ID of an item shouldn't be empty for the success");
        }
        MediaMetadata metadata = item.getMetadata();
        if (metadata == null) {
            throw new RuntimeException(
                    "Metadata of an item shouldn't be null for the success");
        }
        if (!metadata.containsKey(MediaMetadata.METADATA_KEY_BROWSABLE)) {
            throw new RuntimeException(
                    "METADATA_KEY_BROWSABLE should be specified in metadata of an item");
        }
        if (!metadata.containsKey(MediaMetadata.METADATA_KEY_PLAYABLE)) {
            throw new RuntimeException(
                    "METADATA_KEY_PLAYABLE should be specified in metadata of an item");
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
    public LibraryResult onGetLibraryRootOnExecutor(ControllerInfo controller,
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
    public LibraryResult onGetItemOnExecutor(ControllerInfo controller, final String mediaId) {
        LibraryResult result = getCallback().onGetItem(getInstance(), controller, mediaId);
        return ensureNonNullResultWithValidItem(result);
    }

    @Override
    public LibraryResult onGetChildrenOnExecutor(ControllerInfo controller, final String parentId,
            final int page, final int pageSize, final LibraryParams params) {
        LibraryResult result = getCallback().onGetChildren(getInstance(),
                controller, parentId, page, pageSize, params);
        return ensureNonNullResultWithValidList(result, pageSize);
    }

    @Override
    public int onSubscribeOnExecutor(ControllerInfo controller, String parentId,
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
        if (resultCode != RESULT_CODE_SUCCESS) {
            synchronized (mLock) {
                mSubscriptions.remove(controller.getControllerCb());
            }
        }
        return resultCode;
    }

    @Override
    public int onUnsubscribeOnExecutor(ControllerInfo controller, String parentId) {
        int resultCode = getCallback().onUnsubscribe(getInstance(), controller, parentId);
        synchronized (mLock) {
            mSubscriptions.remove(controller.getControllerCb());
        }
        return resultCode;
    }

    @Override
    public int onSearchOnExecutor(ControllerInfo controller, String query, LibraryParams params) {
        return getCallback().onSearch(getInstance(), controller, query, params);
    }

    @Override
    public LibraryResult onGetSearchResultOnExecutor(ControllerInfo controller, final String query,
            final int page, final int pageSize, final LibraryParams params) {
        LibraryResult result = getCallback().onGetSearchResult(getInstance(),
                controller, query, page, pageSize, params);
        return ensureNonNullResultWithValidList(result, pageSize);
    }

    @Override
    void dispatchRemoteControllerCallbackTask(RemoteControllerCallbackTask task) {
        super.dispatchRemoteControllerCallbackTask(task);
        MediaLibraryServiceLegacyStub legacyStub = getLegacyBrowserService();
        if (legacyStub != null) {
            dispatchRemoteControllerCallbackTask(legacyStub.getControllersForAll(), task);
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
}
