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

import static androidx.media2.MediaBrowser2.BrowserResult.RESULT_CODE_BAD_VALUE;
import static androidx.media2.MediaBrowser2.BrowserResult.RESULT_CODE_DISCONNECTED;
import static androidx.media2.MediaBrowser2.BrowserResult.RESULT_CODE_SUCCESS;
import static androidx.media2.MediaBrowser2.BrowserResult.RESULT_CODE_UNKNOWN_ERROR;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.ItemCallback;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.media2.MediaBrowser2.BrowserCallback;
import androidx.media2.MediaBrowser2.BrowserResult;
import androidx.media2.MediaBrowser2.MediaBrowser2Impl;
import androidx.media2.MediaLibraryService2.LibraryParams;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Implementation of MediaBrowser2 with the {@link MediaBrowserCompat} for legacy support.
 */
class MediaBrowser2ImplLegacy extends MediaController2ImplLegacy implements MediaBrowser2Impl {
    private static final String TAG = "MB2ImplLegacy";

    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final HashMap<LibraryParams, MediaBrowserCompat> mBrowserCompats = new HashMap<>();
    @GuardedBy("mLock")
    private final HashMap<String, List<SubscribeCallback>> mSubscribeCallbacks = new HashMap<>();

    MediaBrowser2ImplLegacy(@NonNull Context context, MediaBrowser2 instance,
            @NonNull SessionToken2 token, @NonNull /*@CallbackExecutor*/ Executor executor,
            @NonNull BrowserCallback callback) {
        super(context, instance, token, executor, callback);
    }

    @Override
    public MediaBrowser2 getInstance() {
        return (MediaBrowser2) super.getInstance();
    }

    @Override
    public void close() {
        synchronized (mLock) {
            for (MediaBrowserCompat browser : mBrowserCompats.values()) {
                browser.disconnect();
            }
            mBrowserCompats.clear();
            // Ensure that ControllerCallback#onDisconnected() is called by super.close().
            super.close();
        }
    }

    @Override
    public ListenableFuture<BrowserResult> getLibraryRoot(@Nullable final LibraryParams params) {
        final ResolvableFuture<BrowserResult> result = ResolvableFuture.create();
        final MediaBrowserCompat browser = getBrowserCompat(params);
        if (browser != null) {
            // Already connected with the given extras.
            result.set(new BrowserResult(RESULT_CODE_SUCCESS, createRootMediaItem(browser), null));
        } else {
            getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    // Do this on the callback executor to set the looper of MediaBrowserCompat's
                    // callback handler to this looper.
                    Bundle rootHints = MediaUtils2.convertToRootHints(params);
                    MediaBrowserCompat newBrowser = new MediaBrowserCompat(getContext(),
                            getConnectedSessionToken().getComponentName(),
                            new GetLibraryRootCallback(result, params), rootHints);
                    synchronized (mLock) {
                        mBrowserCompats.put(params, newBrowser);
                    }
                    newBrowser.connect();
                }
            });
        }
        return result;
    }

    @Override
    public ListenableFuture<BrowserResult> subscribe(@NonNull String parentId,
            @Nullable LibraryParams params) {
        MediaBrowserCompat browser = getBrowserCompat();
        if (browser == null) {
            return BrowserResult.createFutureWithResult(RESULT_CODE_DISCONNECTED);
        }
        SubscribeCallback callback = new SubscribeCallback();
        synchronized (mLock) {
            List<SubscribeCallback> list = mSubscribeCallbacks.get(parentId);
            if (list == null) {
                list = new ArrayList<>();
                mSubscribeCallbacks.put(parentId, list);
            }
            list.add(callback);
        }
        browser.subscribe(parentId, getExtras(params), callback);

        // No way to get result. Just return success.
        return BrowserResult.createFutureWithResult(BrowserResult.RESULT_CODE_SUCCESS);
    }

    @Override
    public ListenableFuture<BrowserResult> unsubscribe(@NonNull String parentId) {
        MediaBrowserCompat browser = getBrowserCompat();
        if (browser == null) {
            return BrowserResult.createFutureWithResult(RESULT_CODE_DISCONNECTED);
        }
        // Note: don't use MediaBrowserCompat#unsubscribe(String) here, to keep the subscription
        // callback for getChildren.
        synchronized (mLock) {
            List<SubscribeCallback> list = mSubscribeCallbacks.get(parentId);
            if (list == null) {
                return BrowserResult.createFutureWithResult(RESULT_CODE_BAD_VALUE);
            }
            for (int i = 0; i < list.size(); i++) {
                browser.unsubscribe(parentId, list.get(i));
            }
        }

        // No way to get result. Just return success.
        return BrowserResult.createFutureWithResult(BrowserResult.RESULT_CODE_SUCCESS);
    }

    @Override
    public ListenableFuture<BrowserResult> getChildren(@NonNull String parentId, int page,
            int pageSize, @Nullable LibraryParams params) {
        MediaBrowserCompat browser = getBrowserCompat();
        if (browser == null) {
            return BrowserResult.createFutureWithResult(RESULT_CODE_DISCONNECTED);
        }

        final ResolvableFuture<BrowserResult> future = ResolvableFuture.create();
        Bundle options = createBundle(params);
        options.putInt(MediaBrowserCompat.EXTRA_PAGE, page);
        options.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, pageSize);
        browser.subscribe(parentId, options, new GetChildrenCallback(future, parentId));
        return future;
    }

    @Override
    public ListenableFuture<BrowserResult> getItem(@NonNull final String mediaId) {
        MediaBrowserCompat browser = getBrowserCompat();
        if (browser == null) {
            return BrowserResult.createFutureWithResult(RESULT_CODE_DISCONNECTED);
        }
        final ResolvableFuture<BrowserResult> result = ResolvableFuture.create();
        browser.getItem(mediaId, new ItemCallback() {
            @Override
            public void onItemLoaded(final MediaItem item) {
                getCallbackExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (item != null) {
                            result.set(new BrowserResult(RESULT_CODE_SUCCESS,
                                    MediaUtils2.convertToMediaItem2(item), null));
                        } else {
                            result.set(new BrowserResult(RESULT_CODE_BAD_VALUE));
                        }
                    }
                });
            }

            @Override
            public void onError(String itemId) {
                getCallbackExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        result.set(new BrowserResult(RESULT_CODE_UNKNOWN_ERROR));
                    }
                });
            }
        });
        return result;
    }

    @Override
    public ListenableFuture<BrowserResult> search(@NonNull String query,
            @Nullable LibraryParams params) {
        MediaBrowserCompat browser = getBrowserCompat();
        if (browser == null) {
            return BrowserResult.createFutureWithResult(RESULT_CODE_DISCONNECTED);
        }
        browser.search(query, getExtras(params), new MediaBrowserCompat.SearchCallback() {
            @Override
            public void onSearchResult(final String query, final Bundle extras,
                    final List<MediaItem> items) {
                getCallbackExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        // Set extra null here, because 'extra' have different meanings between old
                        // API and new API as follows.
                        // - Old API: Extra/Option specified with search().
                        // - New API: Extra from MediaLibraryService2 to MediaBrowser2
                        // TODO(Post-P): Cache search result for later getSearchResult() calls.
                        getCallback().onSearchResultChanged(
                                getInstance(), query, items.size(), null);
                    }
                });
            }

            @Override
            public void onError(final String query, final Bundle extras) {
                getCallbackExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        // Set extra null here, because 'extra' have different meanings between old
                        // API and new API as follows.
                        // - Old API: Extra/Option specified with search().
                        // - New API: Extra from MediaLibraryService2 to MediaBrowser2
                        getCallback().onSearchResultChanged(
                                getInstance(), query, 0, null);
                    }
                });
            }
        });
        // No way to get result. Just return success.
        return BrowserResult.createFutureWithResult(RESULT_CODE_SUCCESS);
    }

    @Override
    public ListenableFuture<BrowserResult> getSearchResult(final @NonNull String query,
            final int page, final int pageSize, final @Nullable LibraryParams param) {
        MediaBrowserCompat browser = getBrowserCompat();
        if (browser == null) {
            return BrowserResult.createFutureWithResult(RESULT_CODE_DISCONNECTED);
        }

        final ResolvableFuture<BrowserResult> future = ResolvableFuture.create();
        Bundle options = createBundle(param);
        options.putInt(MediaBrowserCompat.EXTRA_PAGE, page);
        options.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, pageSize);
        browser.search(query, options, new MediaBrowserCompat.SearchCallback() {
            @Override
            public void onSearchResult(final String query, final Bundle extrasSent,
                    final List<MediaItem> items) {
                getCallbackExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        List<MediaItem2> item2List =
                                MediaUtils2.convertMediaItemListToMediaItem2List(items);
                        future.set(new BrowserResult(RESULT_CODE_SUCCESS, item2List, null));
                    }
                });
            }

            @Override
            public void onError(final String query, final Bundle extrasSent) {
                getCallbackExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        future.set(new BrowserResult(RESULT_CODE_UNKNOWN_ERROR));
                    }
                });
            }
        });
        return future;
    }

    @Override
    public BrowserCallback getCallback() {
        return (BrowserCallback) super.getCallback();
    }

    private MediaBrowserCompat getBrowserCompat(LibraryParams extras) {
        synchronized (mLock) {
            return mBrowserCompats.get(extras);
        }
    }

    private static Bundle createBundle(@Nullable LibraryParams params) {
        return params == null || params.getExtras() == null
                ? new Bundle() : new Bundle(params.getExtras());
    }

    private static Bundle getExtras(@Nullable LibraryParams params) {
        return params != null ? params.getExtras() : null;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaItem2 createRootMediaItem(@NonNull MediaBrowserCompat browser) {
        // TODO: Query again with getMediaItem() to get real media item.
        MediaMetadata2 metadata = new MediaMetadata2.Builder()
                .putString(MediaMetadata2.METADATA_KEY_MEDIA_ID, browser.getRoot())
                .putLong(MediaMetadata2.METADATA_KEY_BROWSABLE, MediaMetadata2.BROWSABLE_TYPE_MIXED)
                .putLong(MediaMetadata2.METADATA_KEY_PLAYABLE, 0)
                .setExtras(browser.getExtras())
                .build();
        return new MediaItem2.Builder().setMetadata(metadata).build();
    }

    private class GetLibraryRootCallback extends MediaBrowserCompat.ConnectionCallback {
        final ResolvableFuture<BrowserResult> mResult;
        final LibraryParams mParams;

        GetLibraryRootCallback(ResolvableFuture<BrowserResult> result, LibraryParams params) {
            super();
            mResult = result;
            mParams = params;
        }

        @Override
        public void onConnected() {
            MediaBrowserCompat browser;
            synchronized (mLock) {
                browser = mBrowserCompats.get(mParams);
            }
            if (browser == null) {
                // Shouldn't be happen. Internal error?
                mResult.set(new BrowserResult(RESULT_CODE_UNKNOWN_ERROR));
            } else {
                mResult.set(new BrowserResult(RESULT_CODE_SUCCESS, createRootMediaItem(browser),
                        MediaUtils2.convertToLibraryParams(mContext, browser.getExtras())));
            }
        }

        @Override
        public void onConnectionSuspended() {
            onConnectionFailed();
        }

        @Override
        public void onConnectionFailed() {
            // Unknown extra field.
            mResult.set(new BrowserResult(RESULT_CODE_BAD_VALUE));
            close();
        }
    }

    private class SubscribeCallback extends SubscriptionCallback {
        SubscribeCallback() {
        }

        @Override
        public void onError(String parentId) {
            onChildrenLoaded(parentId, null, null);
        }

        @Override
        public void onError(String parentId, Bundle options) {
            onChildrenLoaded(parentId, null, options);
        }

        @Override
        public void onChildrenLoaded(String parentId, List<MediaItem> children) {
            onChildrenLoaded(parentId, children, null);
        }

        @Override
        public void onChildrenLoaded(final String parentId, List<MediaItem> children,
                final Bundle options) {
            if (TextUtils.isEmpty(parentId)) {
                Log.w(TAG, "SubscribeCallback.onChildrenLoaded(): Ignoring empty parentId");
                return;
            }
            final MediaBrowserCompat browser = getBrowserCompat();
            if (browser == null) {
                // Browser is closed.
                return;
            }
            final int itemCount;
            if (children != null) {
                itemCount = children.size();
            } else {
                // Currently no way to tell failures in MediaBrowser2#subscribe().
                return;
            }

            final LibraryParams params = MediaUtils2.convertToLibraryParams(mContext,
                    browser.getNotifyChildrenChangedOptions());
            getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    // TODO(Post-P): Cache children result for later getChildren() calls.
                    getCallback().onChildrenChanged(getInstance(), parentId, itemCount, params);
                }
            });
        }
    }

    private class GetChildrenCallback extends SubscriptionCallback {
        final ResolvableFuture<BrowserResult> mFuture;
        final String mParentId;

        GetChildrenCallback(ResolvableFuture<BrowserResult> future, String parentId) {
            super();
            mFuture = future;
            mParentId = parentId;
        }

        @Override
        public void onError(String parentId) {
            mFuture.set(new BrowserResult(RESULT_CODE_UNKNOWN_ERROR));
        }

        @Override
        public void onError(String parentId, Bundle options) {
            mFuture.set(new BrowserResult(RESULT_CODE_UNKNOWN_ERROR));
        }

        @Override
        public void onChildrenLoaded(String parentId, List<MediaItem> children) {
            onChildrenLoaded(parentId, children, null);
        }

        @Override
        public void onChildrenLoaded(final String parentId, List<MediaItem> children,
                Bundle options) {
            if (TextUtils.isEmpty(parentId)) {
                Log.w(TAG, "GetChildrenCallback.onChildrenLoaded(): Ignoring empty parentId");
                return;
            }
            MediaBrowserCompat browser = getBrowserCompat();
            if (browser == null) {
                mFuture.set(new BrowserResult(RESULT_CODE_DISCONNECTED));
                return;
            }
            browser.unsubscribe(mParentId, GetChildrenCallback.this);

            final List<MediaItem2> items = new ArrayList<>();
            if (children == null) {
                // list are non-Null, so it must be internal error.
                mFuture.set(new BrowserResult(RESULT_CODE_UNKNOWN_ERROR));
            } else {
                for (int i = 0; i < children.size(); i++) {
                    items.add(MediaUtils2.convertToMediaItem2(children.get(i)));
                }
                // Don't set extra here, because 'extra' have different meanings between old
                // API and new API as follows.
                // - Old API: Extra/Option specified with subscribe().
                // - New API: Extra from MediaLibraryService2 to MediaBrowser2
                mFuture.set(new BrowserResult(RESULT_CODE_SUCCESS, items, null));
            }
        }
    }
}
