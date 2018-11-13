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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.media2.MediaLibraryService.LibraryParams;
import androidx.media2.MediaLibraryService.LibraryResult;
import androidx.media2.MediaLibraryService.MediaLibrarySession;
import androidx.versionedparcelable.NonParcelField;
import androidx.versionedparcelable.ParcelField;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
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
     * If it's successfully completed, {@link BrowserResult#getMediaItem()} will return the library
     * root.
     *
     * @param params library params getting root
     * @see BrowserResult#getMediaItem()
     */
    @NonNull
    public ListenableFuture<BrowserResult> getLibraryRoot(@Nullable final LibraryParams params) {
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
    public @NonNull ListenableFuture<BrowserResult> subscribe(@NonNull String parentId,
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
    public @NonNull ListenableFuture<BrowserResult> unsubscribe(@NonNull String parentId) {
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
     * If it's successfully completed, {@link BrowserResult#getMediaItems()} will return the list
     * of children.
     *
     * @param parentId non-empty parent id for getting the children
     * @param page page number to get the result. Starts from {@code 0}
     * @param pageSize page size. Should be greater or equal to {@code 1}
     * @param params library params
     * @see BrowserResult#getMediaItems()
     */
    public @NonNull ListenableFuture<BrowserResult> getChildren(@NonNull String parentId,
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
     * If it's successfully completed, {@link BrowserResult#getMediaItem()} will return the media
     * item.
     *
     * @param mediaId non-empty media id for specifying the item
     * @see BrowserResult#getMediaItems()
     */
    public @NonNull ListenableFuture<BrowserResult> getItem(@NonNull final String mediaId) {
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
     * Returned {@link BrowserResult} will only tell whether the attemp to search was successful.
     * For getting the search result, waits for
     * {@link BrowserCallback#getSearchResult(String, int, int, LibraryParams)} the search result
     * and calls {@link #getSearchResult(String, int, int, LibraryParams)}} for getting the result.
     *
     * @param query non-empty search query
     * @param params library params
     * @see BrowserCallback#getSearchResult(String, int, int, LibraryParams)
     * @see #getSearchResult(String, int, int, LibraryParams)
     */
    public @NonNull ListenableFuture<BrowserResult> search(@NonNull String query,
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
     * If it's successfully completed, {@link BrowserResult#getMediaItems()} will return the search
     * result.
     *
     * @param query non-empty search query that you've specified with
     *              {@link #search(String, LibraryParams)}.
     * @param page page number to get search result. Starts from {@code 0}
     * @param pageSize page size. Should be greater or equal to {@code 1}
     * @param params library params
     * @see BrowserResult#getMediaItems()
     */
    public @NonNull ListenableFuture<BrowserResult> getSearchResult(final @NonNull String query,
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

    private static ListenableFuture<BrowserResult> createDisconnectedFuture() {
        return BrowserResult.createFutureWithResult(BrowserResult.RESULT_CODE_DISCONNECTED);
    }

    /**
     * Result class to be used with {@link ListenableFuture} for asynchronous calls.
     */
    // Specify full name to workaround build error 'cannot find symbol'
    @androidx.versionedparcelable.VersionedParcelize(isCustom = true)
    public static class BrowserResult extends androidx.versionedparcelable.CustomVersionedParcelable
            implements RemoteResult {
        /**
         * @hide
         */
        @IntDef(flag = false, /*prefix = "RESULT_CODE",*/ value = {
                RESULT_CODE_SUCCESS,
                RESULT_CODE_UNKNOWN_ERROR,
                RESULT_CODE_INVALID_STATE,
                RESULT_CODE_BAD_VALUE,
                RESULT_CODE_PERMISSION_DENIED,
                RESULT_CODE_IO_ERROR,
                RESULT_CODE_SKIPPED,
                RESULT_CODE_DISCONNECTED,
                RESULT_CODE_NOT_SUPPORTED,
                RESULT_CODE_AUTHENTICATION_EXPIRED,
                RESULT_CODE_PREMIUM_ACCOUNT_REQUIRED,
                RESULT_CODE_CONCURRENT_STREAM_LIMIT,
                RESULT_CODE_PARENTAL_CONTROL_RESTRICTED,
                RESULT_CODE_NOT_AVAILABLE_IN_REGION,
                RESULT_CODE_SKIP_LIMIT_REACHED,
                RESULT_CODE_SETUP_REQUIRED})
        @Retention(RetentionPolicy.SOURCE)
        @RestrictTo(LIBRARY_GROUP)
        public @interface ResultCode {}

        @ParcelField(1)
        int mResultCode;
        @ParcelField(2)
        long mCompletionTime;
        @ParcelField(3)
        MediaItem mItem;
        @ParcelField(4)
        LibraryParams mParams;
        // Mark list of media items NonParcelField to send the list through the ParcelImpListSlice.
        @NonParcelField
        List<MediaItem> mItemList;
        @ParcelField(5)
        ParcelImplListSlice mItemListSlice;

        // For versioned parcelable.
        BrowserResult() {
            // no-op
        }

        BrowserResult(@ResultCode int resultCode) {
            this(resultCode, null, null, null);
        }

        BrowserResult(@ResultCode int resultCode, @Nullable MediaItem item,
                @Nullable LibraryParams params) {
            this(resultCode, item, null, params);
        }

        BrowserResult(@ResultCode int resultCode, @Nullable List<MediaItem> items,
                @Nullable LibraryParams params) {
            this(resultCode, null, items, params);
        }

        BrowserResult(@ResultCode int resultCode, @Nullable MediaItem item,
                @Nullable List<MediaItem> items, @Nullable LibraryParams params) {
            this(resultCode, item, items, params, SystemClock.elapsedRealtime());
        }

        BrowserResult(@ResultCode int resultCode, @Nullable MediaItem item,
                @Nullable List<MediaItem> items, @Nullable LibraryParams params,
                long elapsedTime) {
            mResultCode = resultCode;
            mItem = item;
            mItemList = items;
            mParams = params;
            mCompletionTime = elapsedTime;
        }

        static ListenableFuture<BrowserResult> createFutureWithResult(@ResultCode int resultCode) {
            ResolvableFuture<BrowserResult> result = ResolvableFuture.create();
            result.set(new BrowserResult(resultCode));
            return result;
        }

        static BrowserResult from(@Nullable LibraryResult result) {
            if (result == null) {
                return null;
            }
            return new BrowserResult(result.getResultCode(), result.getMediaItem(),
                    result.getMediaItems(), result.getLibraryParams(), result.getCompletionTime());
        }

        /**
         * Gets the result code.
         *
         * @return result code
         * @see #RESULT_CODE_SUCCESS
         * @see #RESULT_CODE_UNKNOWN_ERROR
         * @see #RESULT_CODE_INVALID_STATE
         * @see #RESULT_CODE_BAD_VALUE
         * @see #RESULT_CODE_PERMISSION_DENIED
         * @see #RESULT_CODE_IO_ERROR
         * @see #RESULT_CODE_SKIPPED
         * @see #RESULT_CODE_DISCONNECTED
         * @see #RESULT_CODE_NOT_SUPPORTED
         * @see #RESULT_CODE_AUTHENTICATION_EXPIRED
         * @see #RESULT_CODE_PREMIUM_ACCOUNT_REQUIRED
         * @see #RESULT_CODE_CONCURRENT_STREAM_LIMIT
         * @see #RESULT_CODE_PARENTAL_CONTROL_RESTRICTED
         * @see #RESULT_CODE_NOT_AVAILABLE_IN_REGION
         * @see #RESULT_CODE_SKIP_LIMIT_REACHED
         * @see #RESULT_CODE_SETUP_REQUIRED
         */
        @Override
        public @ResultCode int getResultCode() {
            return mResultCode;
        }

        /**
         * Gets the completion time of the command. Being more specific, it's the same as
         * {@link android.os.SystemClock#elapsedRealtime()} when the command is completed.
         *
         * @return completion time of the command
         */
        @Override
        public long getCompletionTime() {
            return mCompletionTime;
        }

        /**
         * Gets the media item.
         * <p>
         * Can be {@code null} if an error happened or the command doesn't return a media item.
         *
         * @return media item
         * @see MediaBrowser#getLibraryRoot(LibraryParams)
         * @see MediaBrowser#getItem(String)
         */
        @Override
        public @Nullable MediaItem getMediaItem() {
            return mItem;
        }

        /**
         * Gets the list of media item.
         * <p>
         * Can be {@code null} if an error happened or the command doesn't return a list of media
         * items.
         *
         * @return list of media item
         * @see MediaBrowser#getSearchResult(String, int, int, LibraryParams)
         * @see MediaBrowser#getChildren(String, int, int, LibraryParams)
         **/
        public @Nullable List<MediaItem> getMediaItems() {
            return mItemList;
        }

        /**
         * Gets the library params
         *
         * @return library params.
         */
        public @Nullable LibraryParams getLibraryParams() {
            return mParams;
        }

        /**
         * @hide
         * @param isStream
         */
        @RestrictTo(LIBRARY)
        @Override
        public void onPreParceling(boolean isStream) {
            mItemListSlice = MediaUtils.convertMediaItemListToParcelImplListSlice(mItemList);
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        @Override
        public void onPostParceling() {
            mItemList = MediaUtils.convertParcelImplListSliceToMediaItemList(mItemListSlice);
            mItemListSlice = null;
        }
    }

    interface MediaBrowserImpl extends MediaControllerImpl {
        ListenableFuture<BrowserResult> getLibraryRoot(@Nullable LibraryParams rootHints);
        ListenableFuture<BrowserResult> subscribe(@NonNull String parentId,
                @Nullable LibraryParams params);
        ListenableFuture<BrowserResult> unsubscribe(@NonNull String parentId);
        ListenableFuture<BrowserResult> getChildren(@NonNull String parentId, int page,
                int pageSize, @Nullable LibraryParams params);
        ListenableFuture<BrowserResult> getItem(@NonNull String mediaId);
        ListenableFuture<BrowserResult> search(@NonNull String query,
                @Nullable LibraryParams params);
        ListenableFuture<BrowserResult> getSearchResult(@NonNull String query, int page,
                int pageSize, @Nullable LibraryParams params);
    }
}
