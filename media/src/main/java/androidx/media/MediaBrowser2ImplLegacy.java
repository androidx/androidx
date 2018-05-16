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

import static androidx.media.MediaConstants2.ARGUMENT_EXTRAS;

import android.content.Context;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.ItemCallback;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowser2.BrowserCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Implementation of MediaBrowser2 with the {@link MediaBrowserCompat} for legacy support.
 */
class MediaBrowser2ImplLegacy extends MediaController2ImplLegacy
        implements MediaBrowser2.SupportLibraryImpl {
    public static final String EXTRA_ITEM_COUNT = "android.media.browse.extra.ITEM_COUNT";

    @GuardedBy("mLock")
    private final HashMap<Bundle, MediaBrowserCompat> mBrowserCompats = new HashMap<>();
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
    public void getLibraryRoot(@Nullable final Bundle extras) {
        final MediaBrowserCompat browser = getBrowserCompat(extras);
        if (browser != null) {
            // Already connected with the given extras.
            getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    getCallback().onGetLibraryRootDone(getInstance(), extras,
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

    @Override
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
        browser.subscribe(parentId, options, callback);
    }

    @Override
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

    @Override
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

    @Override
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
                        getCallback().onGetItemDone(getInstance(), mediaId,
                                MediaUtils2.convertToMediaItem2(item));
                    }
                });
            }

            @Override
            public void onError(String itemId) {
                getCallbackExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        getCallback().onGetItemDone(getInstance(), mediaId, null);
                    }
                });
            }
        });
    }

    @Override
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
                                getInstance(), query, items.size(), extras);
                    }
                });
            }

            @Override
            public void onError(final String query, final Bundle extras) {
                // Currently no way to tell failures in MediaBrowser2#search().
            }
        });
    }

    @Override
    public void getSearchResult(final @NonNull String query, final int page, final int pageSize,
            final @Nullable Bundle extras) {
        MediaBrowserCompat browser = getBrowserCompat();
        if (browser == null) {
            return;
        }
        Bundle options = MediaUtils2.createBundle(extras);
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
                        getCallback().onGetSearchResultDone(
                                getInstance(), query, page, pageSize, item2List, extras);
                    }
                });
            }

            @Override
            public void onError(final String query, final Bundle extrasSent) {
                getCallbackExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        getCallback().onGetSearchResultDone(
                                getInstance(), query, page, pageSize, null, extras);
                    }
                });
            }
        });
    }

    @Override
    public BrowserCallback getCallback() {
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
                    getCallback().onGetLibraryRootDone(getInstance(),
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
                    getCallback().onChildrenChanged(getInstance(), parentId, itemCount,
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
                    items.add(MediaUtils2.convertToMediaItem2(children.get(i)));
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
                    getCallback().onGetChildrenDone(getInstance(), parentId, mPage, mPageSize,
                            items, extras);
                    browser.unsubscribe(mParentId, GetChildrenCallback.this);
                }
            });
        }
    }
}
