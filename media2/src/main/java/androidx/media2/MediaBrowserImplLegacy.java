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

import static androidx.media2.MediaBrowser.BrowserResult.RESULT_CODE_BAD_VALUE;
import static androidx.media2.MediaBrowser.BrowserResult.RESULT_CODE_DISCONNECTED;
import static androidx.media2.MediaBrowser.BrowserResult.RESULT_CODE_SUCCESS;
import static androidx.media2.MediaBrowser.BrowserResult.RESULT_CODE_UNKNOWN_ERROR;
import static androidx.media2.MediaMetadata.BROWSABLE_TYPE_MIXED;
import static androidx.media2.MediaMetadata.METADATA_KEY_BROWSABLE;
import static androidx.media2.MediaMetadata.METADATA_KEY_MEDIA_ID;
import static androidx.media2.MediaMetadata.METADATA_KEY_PLAYABLE;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.ItemCallback;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.media2.MediaBrowser.BrowserCallback;
import androidx.media2.MediaBrowser.BrowserResult;
import androidx.media2.MediaBrowser.MediaBrowserImpl;
import androidx.media2.MediaLibraryService.LibraryParams;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Implementation of MediaBrowser with the {@link MediaBrowserCompat} for legacy support.
 */
class MediaBrowserImplLegacy extends MediaControllerImplLegacy implements MediaBrowserImpl {
    private static final String TAG = "MB2ImplLegacy";

    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final HashMap<LibraryParams, MediaBrowserCompat> mBrowserCompats = new HashMap<>();
    @GuardedBy("mLock")
    private final HashMap<String, List<SubscribeCallback>> mSubscribeCallbacks = new HashMap<>();

    MediaBrowserImplLegacy(@NonNull Context context, MediaBrowser instance,
            @NonNull SessionToken token, @NonNull /*@CallbackExecutor*/ Executor executor,
            @NonNull BrowserCallback callback) {
        super(context, instance, token, executor, callback);
    }

    @Override
    public MediaBrowser getInstance() {
        return (MediaBrowser) super.getInstance();
    }

    @Override
    public void close() {
        synchronized (mLock) {
            for (MediaBrowserCompat browserCompat : mBrowserCompats.values()) {
                browserCompat.disconnect();
            }
            mBrowserCompats.clear();
            // Ensure that ControllerCallback#onDisconnected() is called by super.close().
            super.close();
        }
    }

    @Override
    public ListenableFuture<BrowserResult> getLibraryRoot(@Nullable final LibraryParams params) {
        final ResolvableFuture<BrowserResult> result = ResolvableFuture.create();
        final MediaBrowserCompat browserCompat = getBrowserCompat(params);
        if (browserCompat != null) {
            // Already connected with the given extras.
            result.set(new BrowserResult(RESULT_CODE_SUCCESS, createRootMediaItem(browserCompat),
                    null));
        } else {
            getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    // Do this on the callback executor to set the looper of MediaBrowserCompat's
                    // callback handler to this looper.
                    Bundle rootHints = MediaUtils.convertToRootHints(params);
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
        MediaBrowserCompat browserCompat = getBrowserCompat();
        if (browserCompat == null) {
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
        browserCompat.subscribe(parentId, getExtras(params), callback);

        // No way to get result. Just return success.
        return BrowserResult.createFutureWithResult(BrowserResult.RESULT_CODE_SUCCESS);
    }

    @Override
    public ListenableFuture<BrowserResult> unsubscribe(@NonNull String parentId) {
        MediaBrowserCompat browserCompat = getBrowserCompat();
        if (browserCompat == null) {
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
                browserCompat.unsubscribe(parentId, list.get(i));
            }
        }

        // No way to get result. Just return success.
        return BrowserResult.createFutureWithResult(BrowserResult.RESULT_CODE_SUCCESS);
    }

    @Override
    public ListenableFuture<BrowserResult> getChildren(@NonNull String parentId, int page,
            int pageSize, @Nullable LibraryParams params) {
        MediaBrowserCompat browserCompat = getBrowserCompat();
        if (browserCompat == null) {
            return BrowserResult.createFutureWithResult(RESULT_CODE_DISCONNECTED);
        }

        final ResolvableFuture<BrowserResult> future = ResolvableFuture.create();
        Bundle options = createBundle(params);
        options.putInt(MediaBrowserCompat.EXTRA_PAGE, page);
        options.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, pageSize);
        browserCompat.subscribe(parentId, options, new GetChildrenCallback(future, parentId));
        return future;
    }

    @Override
    public ListenableFuture<BrowserResult> getItem(@NonNull final String mediaId) {
        MediaBrowserCompat browserCompat = getBrowserCompat();
        if (browserCompat == null) {
            return BrowserResult.createFutureWithResult(RESULT_CODE_DISCONNECTED);
        }
        final ResolvableFuture<BrowserResult> result = ResolvableFuture.create();
        browserCompat.getItem(mediaId, new ItemCallback() {
            @Override
            public void onItemLoaded(final MediaBrowserCompat.MediaItem item) {
                getCallbackExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (item != null) {
                            result.set(new BrowserResult(RESULT_CODE_SUCCESS,
                                    MediaUtils.convertToMediaItem(item), null));
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
        MediaBrowserCompat browserCompat = getBrowserCompat();
        if (browserCompat == null) {
            return BrowserResult.createFutureWithResult(RESULT_CODE_DISCONNECTED);
        }
        browserCompat.search(query, getExtras(params), new MediaBrowserCompat.SearchCallback() {
            @Override
            public void onSearchResult(final String query, final Bundle extras,
                    final List<MediaBrowserCompat.MediaItem> items) {
                getCallbackExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        // Set extra null here, because 'extra' have different meanings between old
                        // API and new API as follows.
                        // - Old API: Extra/Option specified with search().
                        // - New API: Extra from MediaLibraryService to MediaBrowser
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
                        // - New API: Extra from MediaLibraryService to MediaBrowser
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
        MediaBrowserCompat browserCompat = getBrowserCompat();
        if (browserCompat == null) {
            return BrowserResult.createFutureWithResult(RESULT_CODE_DISCONNECTED);
        }

        final ResolvableFuture<BrowserResult> future = ResolvableFuture.create();
        Bundle options = createBundle(param);
        options.putInt(MediaBrowserCompat.EXTRA_PAGE, page);
        options.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, pageSize);
        browserCompat.search(query, options, new MediaBrowserCompat.SearchCallback() {
            @Override
            public void onSearchResult(final String query, final Bundle extrasSent,
                    final List<MediaBrowserCompat.MediaItem> items) {
                getCallbackExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        List<MediaItem> item2List =
                                MediaUtils.convertMediaItemListToMediaItemList(items);
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
    MediaItem createRootMediaItem(@NonNull MediaBrowserCompat browserCompat) {
        // TODO: Query again with getMediaItem() to get real media item.
        MediaMetadata metadata = new MediaMetadata.Builder()
                .putString(METADATA_KEY_MEDIA_ID, browserCompat.getRoot())
                .putLong(METADATA_KEY_BROWSABLE, BROWSABLE_TYPE_MIXED)
                .putLong(METADATA_KEY_PLAYABLE, 0)
                .setExtras(browserCompat.getExtras())
                .build();
        return new MediaItem.Builder().setMetadata(metadata).build();
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
            MediaBrowserCompat browserCompat;
            synchronized (mLock) {
                browserCompat = mBrowserCompats.get(mParams);
            }
            if (browserCompat == null) {
                // Shouldn't be happen. Internal error?
                mResult.set(new BrowserResult(RESULT_CODE_UNKNOWN_ERROR));
            } else {
                mResult.set(new BrowserResult(RESULT_CODE_SUCCESS,
                        createRootMediaItem(browserCompat),
                        MediaUtils.convertToLibraryParams(mContext, browserCompat.getExtras())));
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
        public void onChildrenLoaded(String parentId, List<MediaBrowserCompat.MediaItem> children) {
            onChildrenLoaded(parentId, children, null);
        }

        @Override
        public void onChildrenLoaded(final String parentId,
                List<MediaBrowserCompat.MediaItem> children, final Bundle options) {
            if (TextUtils.isEmpty(parentId)) {
                Log.w(TAG, "SubscribeCallback.onChildrenLoaded(): Ignoring empty parentId");
                return;
            }
            final MediaBrowserCompat browserCompat = getBrowserCompat();
            if (browserCompat == null) {
                // Browser is closed.
                return;
            }
            final int itemCount;
            if (children != null) {
                itemCount = children.size();
            } else {
                // Currently no way to tell failures in MediaBrowser#subscribe().
                return;
            }

            final LibraryParams params = MediaUtils.convertToLibraryParams(mContext,
                    browserCompat.getNotifyChildrenChangedOptions());
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
        public void onChildrenLoaded(String parentId, List<MediaBrowserCompat.MediaItem> children) {
            onChildrenLoaded(parentId, children, null);
        }

        @Override
        public void onChildrenLoaded(final String parentId,
                List<MediaBrowserCompat.MediaItem> children, Bundle options) {
            if (TextUtils.isEmpty(parentId)) {
                Log.w(TAG, "GetChildrenCallback.onChildrenLoaded(): Ignoring empty parentId");
                return;
            }
            MediaBrowserCompat browserCompat = getBrowserCompat();
            if (browserCompat == null) {
                mFuture.set(new BrowserResult(RESULT_CODE_DISCONNECTED));
                return;
            }
            browserCompat.unsubscribe(mParentId, GetChildrenCallback.this);

            final List<MediaItem> items = new ArrayList<>();
            if (children == null) {
                // list are non-Null, so it must be internal error.
                mFuture.set(new BrowserResult(RESULT_CODE_UNKNOWN_ERROR));
            } else {
                for (int i = 0; i < children.size(); i++) {
                    items.add(MediaUtils.convertToMediaItem(children.get(i)));
                }
                // Don't set extra here, because 'extra' have different meanings between old
                // API and new API as follows.
                // - Old API: Extra/Option specified with subscribe().
                // - New API: Extra from MediaLibraryService to MediaBrowser
                mFuture.set(new BrowserResult(RESULT_CODE_SUCCESS, items, null));
            }
        }
    }
}
