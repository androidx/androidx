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

package androidx.media;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.media.MediaConstants2.ARGUMENT_EXTRAS;
import static androidx.media.MediaConstants2.ARGUMENT_PAGE;
import static androidx.media.MediaConstants2.ARGUMENT_PAGE_SIZE;

import android.content.Context;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.ItemCallback;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.media.MediaLibraryService2.MediaLibrarySession;
import androidx.media.MediaSession2.ControllerInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Browses media content offered by a {@link MediaLibraryService2}.
 */
public class MediaBrowser2 extends MediaController2 {
    static final String TAG = "MediaBrowser2";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final String EXTRA_ITEM_COUNT = "android.media.browse.extra.ITEM_COUNT";

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final String MEDIA_BROWSER2_SUBSCRIBE = "androidx.media.MEDIA_BROWSER2_SUBSCRIBE";

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private final HashMap<Bundle, MediaBrowserCompat> mBrowserCompats = new HashMap<>();
    @GuardedBy("mLock")
    private final HashMap<String, List<SubscribeCallback>> mSubscribeCallbacks = new HashMap<>();

    /**
     * Callback to listen events from {@link MediaLibraryService2}.
     */
    public static class BrowserCallback extends MediaController2.ControllerCallback {
        /**
         * Called with the result of {@link #getLibraryRoot(Bundle)}.
         * <p>
         * {@code rootMediaId} and {@code rootExtra} can be {@code null} if the library root isn't
         * available.
         *
         * @param browser the browser for this event
         * @param rootHints rootHints that you previously requested.
         * @param rootMediaId media id of the library root. Can be {@code null}
         * @param rootExtra extra of the library root. Can be {@code null}
         */
        public void onGetLibraryRootDone(@NonNull MediaBrowser2 browser, @Nullable Bundle rootHints,
                @Nullable String rootMediaId, @Nullable Bundle rootExtra) { }

        /**
         * Called when there's change in the parent's children.
         * <p>
         * This API is called when the library service called
         * {@link MediaLibrarySession#notifyChildrenChanged(ControllerInfo, String, int, Bundle)} or
         * {@link MediaLibrarySession#notifyChildrenChanged(String, int, Bundle)} for the parent.
         *
         * @param browser the browser for this event
         * @param parentId parent id that you've specified with {@link #subscribe(String, Bundle)}
         * @param itemCount number of children
         * @param extras extra bundle from the library service. Can be differ from extras that
         *               you've specified with {@link #subscribe(String, Bundle)}.
         */
        public void onChildrenChanged(@NonNull MediaBrowser2 browser, @NonNull String parentId,
                int itemCount, @Nullable Bundle extras) { }

        /**
         * Called when the list of items has been returned by the library service for the previous
         * {@link MediaBrowser2#getChildren(String, int, int, Bundle)}.
         *
         * @param browser the browser for this event
         * @param parentId parent id
         * @param page page number that you've specified with
         *             {@link #getChildren(String, int, int, Bundle)}
         * @param pageSize page size that you've specified with
         *                 {@link #getChildren(String, int, int, Bundle)}
         * @param result result. Can be {@code null}
         * @param extras extra bundle from the library service
         */
        public void onGetChildrenDone(@NonNull MediaBrowser2 browser, @NonNull String parentId,
                int page, int pageSize, @Nullable List<MediaItem2> result,
                @Nullable Bundle extras) { }

        /**
         * Called when the item has been returned by the library service for the previous
         * {@link MediaBrowser2#getItem(String)} call.
         * <p>
         * Result can be null if there had been error.
         *
         * @param browser the browser for this event
         * @param mediaId media id
         * @param result result. Can be {@code null}
         */
        public void onGetItemDone(@NonNull MediaBrowser2 browser, @NonNull String mediaId,
                @Nullable MediaItem2 result) { }

        /**
         * Called when there's change in the search result requested by the previous
         * {@link MediaBrowser2#search(String, Bundle)}.
         *
         * @param browser the browser for this event
         * @param query search query that you've specified with {@link #search(String, Bundle)}
         * @param itemCount The item count for the search result
         * @param extras extra bundle from the library service
         */
        public void onSearchResultChanged(@NonNull MediaBrowser2 browser, @NonNull String query,
                int itemCount, @Nullable Bundle extras) { }

        /**
         * Called when the search result has been returned by the library service for the previous
         * {@link MediaBrowser2#getSearchResult(String, int, int, Bundle)}.
         * <p>
         * Result can be null if there had been error.
         *
         * @param browser the browser for this event
         * @param query search query that you've specified with
         *              {@link #getSearchResult(String, int, int, Bundle)}
         * @param page page number that you've specified with
         *             {@link #getSearchResult(String, int, int, Bundle)}
         * @param pageSize page size that you've specified with
         *                 {@link #getSearchResult(String, int, int, Bundle)}
         * @param result result. Can be {@code null}.
         * @param extras extra bundle from the library service
         */
        public void onGetSearchResultDone(@NonNull MediaBrowser2 browser, @NonNull String query,
                int page, int pageSize, @Nullable List<MediaItem2> result,
                @Nullable Bundle extras) { }
    }

    public MediaBrowser2(@NonNull Context context, @NonNull SessionToken2 token,
            @NonNull /*@CallbackExecutor*/ Executor executor, @NonNull BrowserCallback callback) {
        super(context, token, executor, callback);
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

    /**
     * Get the library root. Result would be sent back asynchronously with the
     * {@link BrowserCallback#onGetLibraryRootDone(MediaBrowser2, Bundle, String, Bundle)}.
     *
     * @param extras extras for getting root
     * @see BrowserCallback#onGetLibraryRootDone(MediaBrowser2, Bundle, String, Bundle)
     */
    public void getLibraryRoot(@Nullable final Bundle extras) {
        final MediaBrowserCompat browser = getBrowserCompat(extras);
        if (browser != null) {
            // Already connected with the given extras.
            getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    getCallback().onGetLibraryRootDone(MediaBrowser2.this, extras,
                            browser.getRoot(), browser.getExtras());
                }
            });
        } else {
            getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    // Do this on the callback executor to set the looper of MediaBrowserCompat's
                    // callback handler to this looper.
                    MediaBrowserCompat newBrowser = new MediaBrowserCompat(getContext(),
                            getSessionToken().getComponentName(),
                            new GetLibraryRootCallback(extras), extras);
                    synchronized (mLock) {
                        mBrowserCompats.put(extras, newBrowser);
                    }
                    newBrowser.connect();
                }
            });
        }
    }

    /**
     * Subscribe to a parent id for the change in its children. When there's a change,
     * {@link BrowserCallback#onChildrenChanged(MediaBrowser2, String, int, Bundle)} will be called
     * with the bundle that you've specified. You should call
     * {@link #getChildren(String, int, int, Bundle)} to get the actual contents for the parent.
     *
     * @param parentId parent id
     * @param extras extra bundle
     */
    public void subscribe(@NonNull String parentId, @Nullable Bundle extras) {
        if (parentId == null) {
            throw new IllegalArgumentException("parentId shouldn't be null");
        }
        MediaBrowserCompat browser = getBrowserCompat();
        if (browser == null) {
            return;
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

        Bundle options = new Bundle();
        options.putBundle(ARGUMENT_EXTRAS, extras);
        options.putBoolean(MEDIA_BROWSER2_SUBSCRIBE, true);
        browser.subscribe(parentId, options, callback);
    }

    /**
     * Unsubscribe for changes to the children of the parent, which was previously subscribed with
     * {@link #subscribe(String, Bundle)}.
     * <p>
     * This unsubscribes all previous subscription with the parent id, regardless of the extra
     * that was previously sent to the library service.
     *
     * @param parentId parent id
     */
    public void unsubscribe(@NonNull String parentId) {
        if (parentId == null) {
            throw new IllegalArgumentException("parentId shouldn't be null");
        }
        MediaBrowserCompat browser = getBrowserCompat();
        if (browser == null) {
            return;
        }
        // Note: don't use MediaBrowserCompat#unsubscribe(String) here, to keep the subscription
        // callback for getChildren.
        synchronized (mLock) {
            List<SubscribeCallback> list = mSubscribeCallbacks.get(parentId);
            if (list == null) {
                return;
            }
            for (int i = 0; i < list.size(); i++) {
                browser.unsubscribe(parentId, list.get(i));
            }
        }
    }

    /**
     * Get list of children under the parent. Result would be sent back asynchronously with the
     * {@link BrowserCallback#onGetChildrenDone(MediaBrowser2, String, int, int, List, Bundle)}.
     *
     * @param parentId parent id for getting the children.
     * @param page page number to get the result. Starts from {@code 1}
     * @param pageSize page size. Should be greater or equal to {@code 1}
     * @param extras extra bundle
     */
    public void getChildren(@NonNull String parentId, int page, int pageSize,
            @Nullable Bundle extras) {
        if (parentId == null) {
            throw new IllegalArgumentException("parentId shouldn't be null");
        }
        if (page < 1 || pageSize < 1) {
            throw new IllegalArgumentException("Neither page nor pageSize should be less than 1");
        }
        MediaBrowserCompat browser = getBrowserCompat();
        if (browser == null) {
            return;
        }

        Bundle options = MediaUtils2.createBundle(extras);
        options.putInt(MediaBrowserCompat.EXTRA_PAGE, page);
        options.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, pageSize);
        browser.subscribe(parentId, options, new GetChildrenCallback(parentId, page, pageSize));
    }

    /**
     * Get the media item with the given media id. Result would be sent back asynchronously with the
     * {@link BrowserCallback#onGetItemDone(MediaBrowser2, String, MediaItem2)}.
     *
     * @param mediaId media id for specifying the item
     */
    public void getItem(@NonNull final String mediaId) {
        MediaBrowserCompat browser = getBrowserCompat();
        if (browser == null) {
            return;
        }
        browser.getItem(mediaId, new ItemCallback() {
            @Override
            public void onItemLoaded(final MediaItem item) {
                getCallbackExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        getCallback().onGetItemDone(MediaBrowser2.this, mediaId,
                                MediaUtils2.createMediaItem2(item));
                    }
                });
            }

            @Override
            public void onError(String itemId) {
                getCallbackExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        getCallback().onGetItemDone(MediaBrowser2.this, mediaId, null);
                    }
                });
            }
        });
    }

    /**
     * Send a search request to the library service. When the search result is changed,
     * {@link BrowserCallback#onSearchResultChanged(MediaBrowser2, String, int, Bundle)} will be
     * called. You should call {@link #getSearchResult(String, int, int, Bundle)} to get the actual
     * search result.
     *
     * @param query search query. Should not be an empty string.
     * @param extras extra bundle
     */
    public void search(@NonNull String query, @Nullable Bundle extras) {
        MediaBrowserCompat browser = getBrowserCompat();
        if (browser == null) {
            return;
        }
        browser.search(query, extras, new MediaBrowserCompat.SearchCallback() {
            @Override
            public void onSearchResult(final String query, final Bundle extras,
                    final List<MediaItem> items) {
                getCallbackExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        getCallback().onSearchResultChanged(
                                MediaBrowser2.this, query, items.size(), extras);
                    }
                });
            }

            @Override
            public void onError(final String query, final Bundle extras) {
                // Currently no way to tell failures in MediaBrowser2#search().
            }
        });
    }

    /**
     * Get the search result from lhe library service. Result would be sent back asynchronously with
     * the
     * {@link BrowserCallback#onGetSearchResultDone(MediaBrowser2, String, int, int, List, Bundle)}.
     *
     * @param query search query that you've specified with {@link #search(String, Bundle)}
     * @param page page number to get search result. Starts from {@code 1}
     * @param pageSize page size. Should be greater or equal to {@code 1}
     * @param extras extra bundle
     */
    public void getSearchResult(final @NonNull String query, final int page, final int pageSize,
            final @Nullable Bundle extras) {
        MediaBrowserCompat browser = getBrowserCompat();
        if (browser == null) {
            return;
        }
        Bundle options = MediaUtils2.createBundle(extras);
        options.putInt(ARGUMENT_PAGE, page);
        options.putInt(ARGUMENT_PAGE_SIZE, pageSize);
        browser.search(query, options, new MediaBrowserCompat.SearchCallback() {
            @Override
            public void onSearchResult(final String query, final Bundle extrasSent,
                    final List<MediaItem> items) {
                getCallbackExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        List<MediaItem2> item2List = MediaUtils2.toMediaItem2List(items);
                        getCallback().onGetSearchResultDone(
                                MediaBrowser2.this, query, page, pageSize, item2List, extras);
                    }
                });
            }

            @Override
            public void onError(final String query, final Bundle extrasSent) {
                getCallbackExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        getCallback().onGetSearchResultDone(
                                MediaBrowser2.this, query, page, pageSize, null, extras);
                    }
                });
            }
        });
    }

    @Override
    BrowserCallback getCallback() {
        return (BrowserCallback) super.getCallback();
    }

    private MediaBrowserCompat getBrowserCompat(Bundle extras) {
        synchronized (mLock) {
            return mBrowserCompats.get(extras);
        }
    }

    private Bundle getExtrasWithoutPagination(Bundle extras) {
        if (extras == null) {
            return null;
        }
        extras.setClassLoader(getContext().getClassLoader());
        try {
            extras.remove(MediaBrowserCompat.EXTRA_PAGE);
            extras.remove(MediaBrowserCompat.EXTRA_PAGE_SIZE);
        } catch (BadParcelableException e) {
            // Pass through...
        }
        return extras;
    }

    private class GetLibraryRootCallback extends MediaBrowserCompat.ConnectionCallback {
        private final Bundle mExtras;

        GetLibraryRootCallback(Bundle extras) {
            super();
            mExtras = extras;
        }

        @Override
        public void onConnected() {
            getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    MediaBrowserCompat browser;
                    synchronized (mLock) {
                        browser = mBrowserCompats.get(mExtras);
                    }
                    if (browser == null) {
                        // Shouldn't be happen.
                        return;
                    }
                    getCallback().onGetLibraryRootDone(MediaBrowser2.this,
                            mExtras, browser.getRoot(), browser.getExtras());
                }
            });
        }

        @Override
        public void onConnectionSuspended() {
            close();
        }

        @Override
        public void onConnectionFailed() {
            close();
        }
    }

    private class SubscribeCallback extends SubscriptionCallback {
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
            final int itemCount;
            if (options != null && options.containsKey(EXTRA_ITEM_COUNT)) {
                itemCount = options.getInt(EXTRA_ITEM_COUNT);
            } else if (children != null) {
                itemCount = children.size();
            } else {
                // Currently no way to tell failures in MediaBrowser2#subscribe().
                return;
            }

            final Bundle notifyChildrenChangedOptions =
                    getBrowserCompat().getNotifyChildrenChangedOptions();
            getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    getCallback().onChildrenChanged(MediaBrowser2.this, parentId, itemCount,
                            notifyChildrenChangedOptions);
                }
            });
        }
    }

    private class GetChildrenCallback extends SubscriptionCallback {
        private final String mParentId;
        private final int mPage;
        private final int mPageSize;

        GetChildrenCallback(String parentId, int page, int pageSize) {
            super();
            mParentId = parentId;
            mPage = page;
            mPageSize = pageSize;
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
                Bundle options) {
            final List<MediaItem2> items;
            if (children == null) {
                items = null;
            } else {
                items = new ArrayList<>();
                for (int i = 0; i < children.size(); i++) {
                    items.add(MediaUtils2.createMediaItem2(children.get(i)));
                }
            }
            final Bundle extras = getExtrasWithoutPagination(options);
            getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    MediaBrowserCompat browser = getBrowserCompat();
                    if (browser == null) {
                        return;
                    }
                    getCallback().onGetChildrenDone(MediaBrowser2.this, parentId, mPage, mPageSize,
                            items, extras);
                    browser.unsubscribe(mParentId, GetChildrenCallback.this);
                }
            });
        }
    }
}
