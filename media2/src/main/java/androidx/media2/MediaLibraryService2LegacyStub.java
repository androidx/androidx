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

import static android.support.v4.media.MediaBrowserCompat.EXTRA_PAGE;
import static android.support.v4.media.MediaBrowserCompat.EXTRA_PAGE_SIZE;

import android.content.Context;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.media.MediaBrowserServiceCompat;
import androidx.media2.MediaLibraryService2.LibraryRoot;
import androidx.media2.MediaLibraryService2.MediaLibrarySession;
import androidx.media2.MediaSession2.ControllerInfo;

import java.util.List;

/**
 * Implementation of {@link MediaBrowserServiceCompat} for interoperability between
 * {@link MediaLibraryService2} and {@link MediaBrowserCompat}.
 */
// TODO: permission check
class MediaLibraryService2LegacyStub extends MediaSessionService2LegacyStub {
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final MediaLibrarySession.SupportLibraryImpl mLibrarySessionImpl;

    // Note: We'd better not obtain token from the session because it's called inside of the
    // session's constructor and session's token may not be initialized here.
    MediaLibraryService2LegacyStub(Context context,
            MediaLibrarySession.SupportLibraryImpl session, MediaSessionCompat.Token token) {
        super(context, session, token);
        mLibrarySessionImpl = session;
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, final Bundle extras) {
        final ControllerInfo controller = getController();
        // Call callbacks directly instead of execute on the executor. Here's the reason.
        // We need to return browser root here. So if we run the callback on the executor, we
        // should wait for the completion.
        // However, we cannot wait if the callback executor is the main executor, which posts
        // the runnable to the main thread's. In that case, since this onGetRoot() always runs
        // on the main thread, the posted runnable for calling onGetLibraryRoot() wouldn't run
        // in here. Even worse, we cannot know whether it would be run on the main thread or
        // not.
        // Because of the reason, just call onGetLibraryRoot() directly here. onGetLibraryRoot()
        // has documentation that it may be called on the main thread.
        BrowserRoot browserRoot = super.onGetRoot(clientPackageName, clientUid, extras);
        if (browserRoot == null) {
            return null;
        }
        LibraryRoot libraryRoot = mLibrarySessionImpl.getCallback().onGetLibraryRoot(
                mLibrarySessionImpl.getInstance(), controller, extras);
        if (libraryRoot == null) {
            // No library root, but keep browser compat connected to allow getting session.
            return MediaUtils2.sDefaultBrowserRoot;
        }
        return new BrowserRoot(libraryRoot.getRootId(), libraryRoot.getExtras());
    }

    @Override
    public void onLoadChildren(String parentId, Result<List<MediaItem>> result) {
        onLoadChildren(parentId, result, null);
    }

    @Override
    public void onLoadChildren(final String parentId, final Result<List<MediaItem>> result,
            final Bundle options) {
        result.detach();
        final ControllerInfo controller = getController();
        mLibrarySessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (options != null) {
                    options.setClassLoader(mLibrarySessionImpl.getContext().getClassLoader());
                    try {
                        int page = options.getInt(EXTRA_PAGE);
                        int pageSize = options.getInt(EXTRA_PAGE_SIZE);
                        if (page > 0 && pageSize > 0) {
                            // Requesting the list of children through pagination.
                            List<MediaItem2> children = mLibrarySessionImpl.getCallback()
                                    .onGetChildren(mLibrarySessionImpl.getInstance(), controller,
                                            parentId, page, pageSize, options);
                            result.sendResult(MediaUtils2.convertToMediaItemList(children));
                            return;
                        }
                        // Cannot distinguish onLoadChildren() why it's called either by
                        // {@link MediaBrowserCompat#subscribe()} or
                        // {@link MediaBrowserServiceCompat#notifyChildrenChanged}.
                    } catch (BadParcelableException e) {
                        // pass-through.
                    }
                }
                // A MediaBrowserCompat called loadChildren with no pagination option.
                List<MediaItem2> children = mLibrarySessionImpl.getCallback()
                        .onGetChildren(mLibrarySessionImpl.getInstance(), controller, parentId,
                                1 /* page */, Integer.MAX_VALUE /* pageSize*/,
                                null /* extras */);
                result.sendResult(MediaUtils2.convertToMediaItemList(children));
            }
        });
    }

    @Override
    public void onLoadItem(final String itemId, final Result<MediaItem> result) {
        result.detach();
        final ControllerInfo controller = getController();
        mLibrarySessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                MediaItem2 item = mLibrarySessionImpl.getCallback().onGetItem(
                        mLibrarySessionImpl.getInstance(), controller, itemId);
                if (item == null) {
                    result.sendResult(null);
                } else {
                    result.sendResult(MediaUtils2.convertToMediaItem(item));
                }
            }
        });
    }

    @Override
    public void onSearch(final String query, final Bundle extras,
            final Result<List<MediaItem>> result) {
        result.detach();
        final ControllerInfo controller = getController();
        extras.setClassLoader(mLibrarySessionImpl.getContext().getClassLoader());
        try {
            final int page = extras.getInt(MediaBrowserCompat.EXTRA_PAGE);
            final int pageSize = extras.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE);
            if (page > 0 && pageSize > 0) {
                mLibrarySessionImpl.getCallbackExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        List<MediaItem2> searchResult = mLibrarySessionImpl.getCallback()
                                .onGetSearchResult(mLibrarySessionImpl.getInstance(), controller,
                                        query, page, pageSize, extras);
                        if (searchResult == null) {
                            result.sendResult(null);
                            return;
                        }
                        result.sendResult(MediaUtils2.convertToMediaItemList(searchResult));
                    }
                });
            } else {
                mLibrarySessionImpl.getCallbackExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        mLibrarySessionImpl.getCallback().onSearch(
                                mLibrarySessionImpl.getInstance(), controller, query, extras);
                    }
                });
            }
        } catch (BadParcelableException e) {
            // Do nothing.
        }
    }

    @Override
    public void onCustomAction(String action, Bundle extras, Result<Bundle> result) {
        // No-op. Library session will handle the custom action.
    }
}