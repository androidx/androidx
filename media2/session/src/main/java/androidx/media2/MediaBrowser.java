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

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media2.MediaLibraryService.LibraryParams;
import androidx.media2.MediaLibraryService.MediaLibrarySession;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * Browses media content offered by a {@link MediaLibraryService}.
 */
public class MediaBrowser extends MediaController {
    static final String TAG = "MediaBrowser";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    /**
     * Callback to listen events from {@link MediaLibraryService}.
     */
    public static class BrowserCallback extends MediaController.ControllerCallback {
        /**
         * Called when there's change in the parent's children after you've subscribed to the parent
         * with {@link #subscribe}.
         * <p>
         * This API is called when the library service called
         * {@link MediaLibrarySession#notifyChildrenChanged} for the parent.
         *
         * @param browser the browser for this event
         * @param parentId non-empty parent id that you've specified with
         *                 {@link #subscribe(String, LibraryParams)}
         * @param itemCount number of children
         * @param params library params from the library service. Can be differ from params
         *               that you've specified with {@link #subscribe(String, LibraryParams)}.
         */
        public void onChildrenChanged(@NonNull MediaBrowser browser, @NonNull String parentId,
                @IntRange(from = 0) int itemCount, @Nullable LibraryParams params) { }

        /**
         * Called when there's change in the search result requested by the previous
         * {@link MediaBrowser#search(String, LibraryParams)}.
         *
         * @param browser the browser for this event
         * @param query non-empty search query that you've specified with
         *              {@link #search(String, LibraryParams)}
         * @param itemCount The item count for the search result
         * @param params library params from the library service. Can be differ from params
         *               that you've specified with {@link #search(String, LibraryParams)}.
         */
        public void onSearchResultChanged(@NonNull MediaBrowser browser, @NonNull String query,
                @IntRange(from = 0) int itemCount, @Nullable LibraryParams params) { }
    }

    /**
     * Create a {@link MediaBrowser} from the {@link SessionToken}. Detailed behavior differs
     * according to the type of the token as follows.
     * <p>
     * <ol>
     * <li>Connected to a {@link SessionToken#TYPE_SESSION} token
     * <p>
     * The browser connects to the specified session directly. It's recommended when you're sure
     * which session to control, or a you've got token directly from the session app.
     * <p>
     * This can be used only when the session for the token is running. Once the session is closed,
     * the token becomes unusable.</li>
     * <li>Connected to a {@link SessionToken#TYPE_SESSION_SERVICE} or
     * {@link SessionToken#TYPE_LIBRARY_SERVICE}
     * <p>The browser connects to the session provided by the
     * {@link MediaSessionService#onGetPrimarySession()}. It's up to the service's decision which
     * session would be returned for the connection. Use the {@link #getConnectedSessionToken()} to
     * know the connected session.
     * <p>
     * This can be used regardless of the session app is running or not. The browser would bind
     * to the service while connected to wake up and keep the service process running.</li>
     * </ol>
     *
     * @param context Context
     * @param token token to connect to
     * @param executor executor to run callbacks on.
     * @param callback browser callback to receive changes in
     * @see MediaSessionService#onGetPrimarySession()
     * @see #getConnectedSessionToken()
     */
    public MediaBrowser(@NonNull Context context, @NonNull SessionToken token,
            @NonNull /*@CallbackExecutor*/ Executor executor, @NonNull BrowserCallback callback) {
        super(context, token, executor, callback);
    }

    @Override
    MediaBrowserImpl createImpl(@NonNull Context context, @NonNull SessionToken token,
            @NonNull Executor executor, @NonNull MediaController.ControllerCallback callback) {
        if (token.isLegacySession()) {
            return new MediaBrowserImplLegacy(
                    context, this, token, executor, (BrowserCallback) callback);
        } else {
            return new MediaBrowserImplBase(
                    context, this, token, executor, (BrowserCallback) callback);
        }
    }

    @Override
    MediaBrowserImpl getImpl() {
        return (MediaBrowserImpl) super.getImpl();
    }

    @Override
    BrowserCallback getCallback() {
        return (BrowserCallback) super.getCallback();
    }

    /**
     * Gets the library root.
     * <p>
     * If it's successfully completed, {@link LibraryResult#getMediaItem()} will return the library
     * root.
     *
     * @param params library params getting root
     * @see LibraryResult#getMediaItem()
     */
    @NonNull
    public ListenableFuture<LibraryResult> getLibraryRoot(@Nullable final LibraryParams params) {
        if (isConnected()) {
            return getImpl().getLibraryRoot(params);
        }
        return createDisconnectedFuture();
    }

    /**
     * Subscribes to a parent id for the change in its children. When there's a change,
     * {@link BrowserCallback#onChildrenChanged(MediaBrowser, String, int, LibraryParams)} will be
     * called with the library params. You should call
     * {@link #getChildren(String, int, int, LibraryParams)} to get the items under the parent.
     *
     * @param parentId non-empty parent id
     * @param params library params
     */
    public @NonNull ListenableFuture<LibraryResult> subscribe(@NonNull String parentId,
            @Nullable LibraryParams params) {
        if (TextUtils.isEmpty(parentId)) {
            throw new IllegalArgumentException("parentId shouldn't be empty");
        }
        if (isConnected()) {
            return getImpl().subscribe(parentId, params);
        }
        return createDisconnectedFuture();
    }

    /**
     * Unsubscribes for changes to the children of the parent, which was previously subscribed with
     * {@link #subscribe(String, LibraryParams)}.
     * <p>
     * This unsubscribes all previous subscription with the parent id, regardless of the library
     * param that was previously sent to the library service.
     *
     * @param parentId non-empty parent id
     */
    public @NonNull ListenableFuture<LibraryResult> unsubscribe(@NonNull String parentId) {
        if (TextUtils.isEmpty(parentId)) {
            throw new IllegalArgumentException("parentId shouldn't be empty");
        }
        if (isConnected()) {
            return getImpl().unsubscribe(parentId);
        }
        return createDisconnectedFuture();
    }

    /**
     * Gets the list of children under the parent.
     * <p>
     * If it's successfully completed, {@link LibraryResult#getMediaItems()} will return the list
     * of children.
     *
     * @param parentId non-empty parent id for getting the children
     * @param page page number to get the result. Starts from {@code 0}
     * @param pageSize page size. Should be greater or equal to {@code 1}
     * @param params library params
     * @see LibraryResult#getMediaItems()
     */
    public @NonNull ListenableFuture<LibraryResult> getChildren(@NonNull String parentId,
            @IntRange(from = 0) int page, @IntRange(from = 1) int pageSize,
            @Nullable LibraryParams params) {
        if (TextUtils.isEmpty(parentId)) {
            throw new IllegalArgumentException("parentId shouldn't be empty");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page shouldn't be negative");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize shouldn't be less than 1");
        }
        if (isConnected()) {
            return getImpl().getChildren(parentId, page, pageSize, params);
        }
        return createDisconnectedFuture();
    }

    /**
     * Gets the media item with the given media id.
     * <p>
     * If it's successfully completed, {@link LibraryResult#getMediaItem()} will return the media
     * item.
     *
     * @param mediaId non-empty media id for specifying the item
     * @see LibraryResult#getMediaItem()
     */
    public @NonNull ListenableFuture<LibraryResult> getItem(@NonNull final String mediaId) {
        if (TextUtils.isEmpty(mediaId)) {
            throw new IllegalArgumentException("mediaId shouldn't be empty");
        }
        if (isConnected()) {
            return getImpl().getItem(mediaId);
        }
        return createDisconnectedFuture();
    }

    /**
     * Sends a search request to the library service.
     * <p>
     * Returned {@link LibraryResult} will only tell whether the attemp to search was successful.
     * For getting the search result, waits for
     * {@link BrowserCallback#getSearchResult(String, int, int, LibraryParams)} the search result
     * and calls {@link #getSearchResult(String, int, int, LibraryParams)}} for getting the result.
     *
     * @param query non-empty search query
     * @param params library params
     * @see BrowserCallback#getSearchResult(String, int, int, LibraryParams)
     * @see #getSearchResult(String, int, int, LibraryParams)
     */
    public @NonNull ListenableFuture<LibraryResult> search(@NonNull String query,
            @Nullable LibraryParams params) {
        if (TextUtils.isEmpty(query)) {
            throw new IllegalArgumentException("query shouldn't be empty");
        }
        if (isConnected()) {
            return getImpl().search(query, params);
        }
        return createDisconnectedFuture();
    }

    /**
     * Gets the search result from lhe library service.
     * <p>
     * If it's successfully completed, {@link LibraryResult#getMediaItems()} will return the search
     * result.
     *
     * @param query non-empty search query that you've specified with
     *              {@link #search(String, LibraryParams)}.
     * @param page page number to get search result. Starts from {@code 0}
     * @param pageSize page size. Should be greater or equal to {@code 1}
     * @param params library params
     * @see LibraryResult#getMediaItems()
     */
    public @NonNull ListenableFuture<LibraryResult> getSearchResult(final @NonNull String query,
            @IntRange(from = 0) int page, @IntRange(from = 1) int pageSize,
            final @Nullable LibraryParams params) {
        if (TextUtils.isEmpty(query)) {
            throw new IllegalArgumentException("query shouldn't be empty");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page shouldn't be negative");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize shouldn't be less than 1");
        }
        if (isConnected()) {
            return getImpl().getSearchResult(query, page, pageSize, params);
        }
        return createDisconnectedFuture();
    }

    private static ListenableFuture<LibraryResult> createDisconnectedFuture() {
        return LibraryResult.createFutureWithResult(
                LibraryResult.RESULT_ERROR_SESSION_DISCONNECTED);
    }

    interface MediaBrowserImpl extends MediaControllerImpl {
        ListenableFuture<LibraryResult> getLibraryRoot(@Nullable LibraryParams rootHints);
        ListenableFuture<LibraryResult> subscribe(@NonNull String parentId,
                @Nullable LibraryParams params);
        ListenableFuture<LibraryResult> unsubscribe(@NonNull String parentId);
        ListenableFuture<LibraryResult> getChildren(@NonNull String parentId, int page,
                int pageSize, @Nullable LibraryParams params);
        ListenableFuture<LibraryResult> getItem(@NonNull String mediaId);
        ListenableFuture<LibraryResult> search(@NonNull String query,
                @Nullable LibraryParams params);
        ListenableFuture<LibraryResult> getSearchResult(@NonNull String query, int page,
                int pageSize, @Nullable LibraryParams params);
    }
}
