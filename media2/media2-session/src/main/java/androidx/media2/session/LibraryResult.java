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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.os.SystemClock;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.media2.common.MediaItem;
import androidx.media2.common.ParcelImplListSlice;
import androidx.versionedparcelable.CustomVersionedParcelable;
import androidx.versionedparcelable.NonParcelField;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelize;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Result class to be used with {@link ListenableFuture} for asynchronous calls between {@link
 * MediaLibraryService.MediaLibrarySession} and {@link MediaBrowser}.
 *
 * @deprecated androidx.media2 is deprecated. Please migrate to <a
 *     href="https://developer.android.com/guide/topics/media/media3">androidx.media3</a>.
 */
@Deprecated
@VersionedParcelize(isCustom = true)
public class LibraryResult extends CustomVersionedParcelable implements RemoteResult {
    /**
     */
    @IntDef(flag = false, /*prefix = "RESULT_CODE",*/ value = {
            RESULT_SUCCESS,
            RESULT_ERROR_UNKNOWN,
            RESULT_ERROR_INVALID_STATE,
            RESULT_ERROR_BAD_VALUE,
            RESULT_ERROR_PERMISSION_DENIED,
            RESULT_ERROR_IO,
            RESULT_INFO_SKIPPED,
            RESULT_ERROR_SESSION_DISCONNECTED,
            RESULT_ERROR_NOT_SUPPORTED,
            RESULT_ERROR_SESSION_AUTHENTICATION_EXPIRED,
            RESULT_ERROR_SESSION_PREMIUM_ACCOUNT_REQUIRED,
            RESULT_ERROR_SESSION_CONCURRENT_STREAM_LIMIT,
            RESULT_ERROR_SESSION_PARENTAL_CONTROL_RESTRICTED,
            RESULT_ERROR_SESSION_NOT_AVAILABLE_IN_REGION,
            RESULT_ERROR_SESSION_SKIP_LIMIT_REACHED,
            RESULT_ERROR_SESSION_SETUP_REQUIRED})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    public @interface ResultCode {}

    @ParcelField(1)
    int mResultCode;
    @ParcelField(2)
    long mCompletionTime;
    // Parceled via mParcelableItem.
    @NonParcelField
    MediaItem mItem;
    // For parceling mItem. Should be only used by onPreParceling() and onPostParceling().
    @ParcelField(3)
    MediaItem mParcelableItem;
    @ParcelField(4)
    MediaLibraryService.LibraryParams mParams;
    // Parceled via mItemListSlice
    @NonParcelField
    List<MediaItem> mItemList;
    // For parceling mItemList. Should be only used by onPreParceling() and onPostParceling().
    @ParcelField(5)
    ParcelImplListSlice mItemListSlice;

    // WARNING: Adding a new ParcelField may break old library users (b/152830728)

    // For versioned parcelable
    LibraryResult() {
        // no-op.
    }

    /**
     * Constructor only with the result code.
     * <p>
     * For success, use other constructor that you can also return the result.
     *
     * @param resultCode result code
     */
    public LibraryResult(@ResultCode int resultCode) {
        this(resultCode, null, null, null);
    }

    /**
     * Constructor with the result code and a media item.
     *
     * @param resultCode result code
     * @param item a media item. Can be {@code null} for error
     * @param params optional library params to describe the returned media item
     */
    public LibraryResult(@ResultCode int resultCode, @Nullable MediaItem item,
            @Nullable MediaLibraryService.LibraryParams params) {
        this(resultCode, item, null, params);
    }

    /**
     * Constructor with the result code and a list of media items.
     *
     * @param resultCode result code
     * @param items list of media items. Can be {@code null} for error
     * @param params optional library params to describe the returned list of media items.
     */
    public LibraryResult(@ResultCode int resultCode, @Nullable List<MediaItem> items,
            @Nullable MediaLibraryService.LibraryParams params) {
        this(resultCode, null, items, params);
    }

    private LibraryResult(@ResultCode int resultCode, @Nullable MediaItem item,
            @Nullable List<MediaItem> items, @Nullable MediaLibraryService.LibraryParams params) {
        mResultCode = resultCode;
        mCompletionTime = SystemClock.elapsedRealtime();
        mItem = item;
        mItemList = items;
        mParams = params;
    }

    static ListenableFuture<LibraryResult> createFutureWithResult(@ResultCode int resultCode) {
        ResolvableFuture<LibraryResult> result = ResolvableFuture.create();
        result.set(new LibraryResult(resultCode));
        return result;
    }

    /**
     * Gets the result code.
     *
     * @return result code
     * @see #RESULT_SUCCESS
     * @see #RESULT_ERROR_UNKNOWN
     * @see #RESULT_ERROR_INVALID_STATE
     * @see #RESULT_ERROR_BAD_VALUE
     * @see #RESULT_ERROR_PERMISSION_DENIED
     * @see #RESULT_ERROR_IO
     * @see #RESULT_INFO_SKIPPED
     * @see #RESULT_ERROR_SESSION_DISCONNECTED
     * @see #RESULT_ERROR_NOT_SUPPORTED
     * @see #RESULT_ERROR_SESSION_AUTHENTICATION_EXPIRED
     * @see #RESULT_ERROR_SESSION_PREMIUM_ACCOUNT_REQUIRED
     * @see #RESULT_ERROR_SESSION_CONCURRENT_STREAM_LIMIT
     * @see #RESULT_ERROR_SESSION_PARENTAL_CONTROL_RESTRICTED
     * @see #RESULT_ERROR_SESSION_NOT_AVAILABLE_IN_REGION
     * @see #RESULT_ERROR_SESSION_SKIP_LIMIT_REACHED
     * @see #RESULT_ERROR_SESSION_SETUP_REQUIRED
     */
    @Override
    @ResultCode
    public int getResultCode() {
        return mResultCode;
    }

    /**
     * Gets the completion time of the command. Being more specific, it's the same as
     * {@link android.os.SystemClock#elapsedRealtime()} when the command completed.
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
     * @see MediaBrowser#getLibraryRoot(MediaLibraryService.LibraryParams)
     * @see MediaBrowser#getItem(String)
     */
    @Override
    @Nullable
    public MediaItem getMediaItem() {
        return mItem;
    }

    /**
     * Gets the list of media item.
     * <p>
     * Can be {@code null} if an error happened or the command doesn't return a list of media
     * items.
     *
     * @return list of media item
     * @see MediaBrowser#getSearchResult(String, int, int, MediaLibraryService.LibraryParams)
     * @see MediaBrowser#getChildren(String, int, int, MediaLibraryService.LibraryParams)
     */
    @Nullable
    public List<MediaItem> getMediaItems() {
        return mItemList;
    }

    /**
     * Gets the library params
     *
     * @return library params.
     */
    @Nullable
    public MediaLibraryService.LibraryParams getLibraryParams() {
        return mParams;
    }

    /**
     */
    @RestrictTo(LIBRARY)
    @Override
    @SuppressWarnings("SynchronizeOnNonFinalField") // mItem and mItemList are effectively final.
    public void onPreParceling(boolean isStream) {
        if (mItem != null) {
            synchronized (mItem) {
                if (mParcelableItem == null) {
                    mParcelableItem = MediaUtils.upcastForPreparceling(mItem);
                }
            }
        }
        if (mItemList != null) {
            synchronized (mItemList) {
                if (mItemListSlice == null) {
                    mItemListSlice = MediaUtils.convertMediaItemListToParcelImplListSlice(
                            mItemList);
                }
            }
        }
    }

    /**
     */
    @RestrictTo(LIBRARY)
    @Override
    public void onPostParceling() {
        mItem = mParcelableItem;
        mItemList = MediaUtils.convertParcelImplListSliceToMediaItemList(mItemListSlice);
    }
}
