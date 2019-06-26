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

import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media2.session.MediaLibraryService.LibraryParams;

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
         * {@link MediaLibraryService.MediaLibrarySession#notifyChildrenChanged} for the parent.
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
     * Create a {@link MediaBrowser} from the {@link SessionToken}.
     *
     * @param context Context
     * @param token token to connect to
     * @param executor executor to run callbacks on.
     * @param callback controller callback to receive changes in
     */
    MediaBrowser(@NonNull Context context, @NonNull SessionToken token,
            @Nullable Bundle connectionHints, @Nullable Executor executor,
            @Nullable BrowserCallback callback) {
        super(context, token, connectionHints, executor, callback);
    }

    MediaBrowser(@NonNull Context context, @NonNull MediaSessionCompat.Token token,
            @Nullable Bundle connectionHints, @Nullable Executor executor,
            @Nullable BrowserCallback callback) {
        super(context, token, connectionHints, executor, callback);
    }

    @Override
    MediaBrowserImpl createImpl(@NonNull Context context, @NonNull SessionToken token,
            @Nullable Bundle connectionHints) {
        if (token.isLegacySession()) {
            return new MediaBrowserImplLegacy(context, this, token);
        } else {
            return new MediaBrowserImplBase(context, this, token, connectionHints);
        }
    }

    @Override
    MediaBrowserImpl getImpl() {
        return (MediaBrowserImpl) super.getImpl();
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
     * {@link #getChildren(String, int, int, LibraryParams)}
     * to get the items under the parent.
     *
     * @param parentId non-empty parent id
     * @param params library params
     */
    @NonNull
    public ListenableFuture<LibraryResult> subscribe(@NonNull String parentId,
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
    @NonNull
    public ListenableFuture<LibraryResult> unsubscribe(@NonNull String parentId) {
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
    @NonNull
    public ListenableFuture<LibraryResult> getChildren(@NonNull String parentId,
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
    @NonNull
    public ListenableFuture<LibraryResult> getItem(@NonNull final String mediaId) {
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
     * {@link BrowserCallback#getSearchResult(String, int, int, LibraryParams)}
     * the search result
     * and calls {@link #getSearchResult(String, int, int, LibraryParams)}}
     * for getting the result.
     *
     * @param query non-empty search query
     * @param params library params
     * @see BrowserCallback#getSearchResult(String, int, int, LibraryParams)
     * @see #getSearchResult(String, int, int, LibraryParams)
     */
    @NonNull
    public ListenableFuture<LibraryResult> search(@NonNull String query,
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
    @NonNull
    public ListenableFuture<LibraryResult> getSearchResult(@NonNull final String query,
            @IntRange(from = 0) int page, @IntRange(from = 1) int pageSize,
            @Nullable final LibraryParams params) {
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

    void notifyBrowserCallback(final BrowserCallbackRunnable callbackRunnable) {
        if (mCallback != null && mCallbackExecutor != null) {
            mCallbackExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    callbackRunnable.run((BrowserCallback) mCallback);
                }
            });
        }
    }

    interface BrowserCallbackRunnable {
        void run(@NonNull BrowserCallback callback);
    }

    /**
     * Builder for {@link MediaBrowser}.
     * <p>
     * To set the token of the session for the controller to connect to, one of the
     * {@link #setSessionToken(SessionToken)} or
     * {@link #setSessionCompatToken(MediaSessionCompat.Token)} should be called.
     * Otherwise, the {@link #build()} will throw an {@link IllegalArgumentException}.
     * <p>
     * Any incoming event from the {@link MediaSession} will be handled on the callback
     * executor.
     */
    public static final class Builder extends
            BuilderBase<MediaBrowser, MediaBrowser.Builder, BrowserCallback> {
        public Builder(@NonNull Context context) {
            super(context);
        }

        @Override
        @NonNull
        public Builder setSessionToken(@NonNull SessionToken token) {
            return super.setSessionToken(token);
        }

        @Override
        @NonNull
        public Builder setSessionCompatToken(@NonNull MediaSessionCompat.Token compatToken) {
            return super.setSessionCompatToken(compatToken);
        }

        @Override
        @NonNull
        public Builder setControllerCallback(@NonNull Executor executor,
                @NonNull BrowserCallback callback) {
            return super.setControllerCallback(executor, callback);
        }

        @Override
        @NonNull
        public Builder setConnectionHints(@NonNull Bundle connectionHints) {
            return super.setConnectionHints(connectionHints);
        }

        /**
         * Build {@link MediaBrowser}.
         * <p>
         * It will throw an {@link IllegalArgumentException} if both {@link SessionToken} and
         * {@link MediaSessionCompat.Token} are not set.
         *
         * @return a new browser
         */
        @Override
        @NonNull
        public MediaBrowser build() {
            if (mToken == null && mCompatToken == null) {
                throw new IllegalArgumentException("token and compat token shouldn't be both null");
            }
            if (mToken != null) {
                return new MediaBrowser(mContext, mToken, mConnectionHints,
                        mCallbackExecutor, (BrowserCallback) mCallback);
            } else {
                return new MediaBrowser(mContext, mCompatToken, mConnectionHints,
                        mCallbackExecutor, (BrowserCallback) mCallback);
            }
        }
    }

    private static ListenableFuture<LibraryResult> createDisconnectedFuture() {
        return LibraryResult.createFutureWithResult(
                LibraryResult.RESULT_ERROR_SESSION_DISCONNECTED);
    }

    interface MediaBrowserImpl extends MediaControllerImpl {
        ListenableFuture<LibraryResult> getLibraryRoot(
                @Nullable LibraryParams rootHints);
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
