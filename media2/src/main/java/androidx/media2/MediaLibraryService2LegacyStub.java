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
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.MediaSessionManager.RemoteUserInfo;
import androidx.media2.MediaController2.PlaybackInfo;
import androidx.media2.MediaLibraryService2.LibraryRoot;
import androidx.media2.MediaLibraryService2.MediaLibrarySession.MediaLibrarySessionImpl;
import androidx.media2.MediaSession2.CommandButton;
import androidx.media2.MediaSession2.ControllerInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link MediaBrowserServiceCompat} for interoperability between
 * {@link MediaLibraryService2} and {@link MediaBrowserCompat}.
 */
class MediaLibraryService2LegacyStub extends MediaSessionService2LegacyStub {
    private static final String TAG = "MLS2LegacyStub";
    private static final boolean DEBUG = false;

    private final ControllerInfo mControllersForAll;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final MediaLibrarySessionImpl mLibrarySessionImpl;

    // Note: We'd better not obtain token from the session because it's called inside of the
    // session's constructor and session's token may not be initialized here.
    MediaLibraryService2LegacyStub(Context context, MediaLibrarySessionImpl session,
            MediaSessionCompat.Token token) {
        super(context, session, token);
        mLibrarySessionImpl = session;
        mControllersForAll = new ControllerInfo(new RemoteUserInfo(
                        RemoteUserInfo.LEGACY_CONTROLLER, Process.myPid(), Process.myUid()),
                false /* trusted */,
                new BrowserLegacyCbForAll(this));
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, final Bundle extras) {
        BrowserRoot browserRoot = super.onGetRoot(clientPackageName, clientUid, extras);
        if (browserRoot == null) {
            return null;
        }
        final ControllerInfo controller = getCurrentController();
        if (getConnectedControllersManager().isAllowedCommand(controller,
                SessionCommand2.COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT)) {
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
            LibraryRoot libraryRoot = mLibrarySessionImpl.getCallback().onGetLibraryRoot(
                    mLibrarySessionImpl.getInstance(), controller, extras);
            if (libraryRoot != null) {
                return new BrowserRoot(libraryRoot.getRootId(), libraryRoot.getExtras());
            }
        } else if (DEBUG) {
            Log.d(TAG, "Command MBC.connect from " + controller + " was rejected by "
                    + mLibrarySessionImpl);
        }
        // No library root, but keep browser compat connected to allow getting session.
        return MediaUtils2.sDefaultBrowserRoot;
    }

    @Override
    public void onSubscribe(final String id, final Bundle option) {
        final ControllerInfo controller = getCurrentController();
        mLibrarySessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // Note: If a developer calls notifyChildrenChanged inside, onLoadChildren will be
                // called twice for a single subscription event.
                // TODO(post 1.0): Fix the issue above.
                if (!getConnectedControllersManager().isAllowedCommand(controller,
                        SessionCommand2.COMMAND_CODE_LIBRARY_SUBSCRIBE)) {
                    if (DEBUG) {
                        Log.d(TAG, "Command MBC.subscribe() from " + controller + " was rejected"
                                + " by " + mLibrarySessionImpl);
                    }
                    return;
                }
                mLibrarySessionImpl.getCallback().onSubscribe(mLibrarySessionImpl.getInstance(),
                        controller, id, option);
            }
        });
    }

    @Override
    public void onUnsubscribe(final String id) {
        final ControllerInfo controller = getCurrentController();
        mLibrarySessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (!getConnectedControllersManager().isAllowedCommand(controller,
                        SessionCommand2.COMMAND_CODE_LIBRARY_UNSUBSCRIBE)) {
                    if (DEBUG) {
                        Log.d(TAG, "Command MBC.unsubscribe() from " + controller + " was rejected"
                                + " by " + mLibrarySessionImpl);
                    }
                    return;
                }
                mLibrarySessionImpl.getCallback().onUnsubscribe(mLibrarySessionImpl.getInstance(),
                                controller, id);
            }
        });
    }

    @Override
    public void onLoadChildren(String parentId, Result<List<MediaItem>> result) {
        onLoadChildren(parentId, result, null);
    }

    @Override
    public void onLoadChildren(final String parentId, final Result<List<MediaItem>> result,
            final Bundle options) {
        result.detach();
        final ControllerInfo controller = getCurrentController();
        mLibrarySessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (!getConnectedControllersManager().isAllowedCommand(controller,
                        SessionCommand2.COMMAND_CODE_LIBRARY_GET_CHILDREN)) {
                    if (DEBUG) {
                        Log.d(TAG, "Command MBC.subscribe() from " + controller + " was rejected"
                                + " by " + mLibrarySessionImpl);
                    }
                    result.sendError(null);
                    return;
                }
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
                                0 /* page */, Integer.MAX_VALUE /* pageSize*/,
                                null /* extras */);
                result.sendResult(MediaUtils2.convertToMediaItemList(children));
            }
        });
    }

    @Override
    public void onLoadItem(final String itemId, final Result<MediaItem> result) {
        result.detach();
        final ControllerInfo controller = getCurrentController();
        mLibrarySessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (!getConnectedControllersManager().isAllowedCommand(controller,
                        SessionCommand2.COMMAND_CODE_LIBRARY_GET_ITEM)) {
                    if (DEBUG) {
                        Log.d(TAG, "Command MBC.getItem() from " + controller + " was rejected by "
                                + mLibrarySessionImpl);
                    }
                    result.sendError(null);
                    return;
                }
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
        final ControllerInfo controller = getCurrentController();
        if (!(controller.getControllerCb() instanceof BrowserLegacyCb)) {
            if (DEBUG) {
                throw new IllegalStateException("Callback hasn't registered. Must be a bug.");
            }
            return;
        }
        result.detach();
        mLibrarySessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (!getConnectedControllersManager().isAllowedCommand(controller,
                        SessionCommand2.COMMAND_CODE_LIBRARY_SEARCH)) {
                    if (DEBUG) {
                        Log.d(TAG, "Command MBC.search() from " + controller + " was rejected by "
                                + mLibrarySessionImpl);
                    }
                    result.sendError(null);
                    return;
                }
                BrowserLegacyCb cb = (BrowserLegacyCb) controller.getControllerCb();
                cb.registerSearchRequest(controller, query, extras, result);
                mLibrarySessionImpl.getCallback().onSearch(mLibrarySessionImpl.getInstance(),
                        controller, query, extras);
                // Actual search result will be sent by notifySearchResultChanged().
            }
        });
    }

    @Override
    public void onCustomAction(final String action, final Bundle extras,
            final Result<Bundle> result) {
        if (result != null) {
            result.detach();
        }
        final ControllerInfo controller = getCurrentController();
        mLibrarySessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                SessionCommand2 command = new SessionCommand2(action, null);
                if (!getConnectedControllersManager().isAllowedCommand(controller, command)) {
                    if (DEBUG) {
                        Log.d(TAG, "Command MBC.sendCustomAction(" + command + ") from "
                                + controller + " was rejected by " + mLibrarySessionImpl);
                    }
                    if (result != null) {
                        result.sendError(null);
                    }
                    return;
                }
                CustomActionResultReceiver receiver =
                        result == null ? null : new CustomActionResultReceiver(result);
                mLibrarySessionImpl.getCallback().onCustomCommand(mLibrarySessionImpl.getInstance(),
                        controller, command, extras, receiver);
            }
        });
    }

    @Override
    ControllerInfo createControllerInfo(RemoteUserInfo remoteUserInfo) {
        return new ControllerInfo(remoteUserInfo,
                mManager.isTrustedForMediaControl(remoteUserInfo),
                new BrowserLegacyCb(remoteUserInfo));
    }

    ControllerInfo getControllersForAll() {
        return mControllersForAll;
    }

    private ControllerInfo getCurrentController() {
        return getConnectedControllersManager().getController(getCurrentBrowserInfo());
    }

    private static class CustomActionResultReceiver extends ResultReceiver {

        private Result<Bundle> mResult;

        CustomActionResultReceiver(Result<Bundle> result) {
            super(null);
            mResult = result;
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            mResult.sendResult(resultData);
        }
    }

    private static class SearchRequest {
        public final ControllerInfo mController;
        public final RemoteUserInfo mRemoteUserInfo;
        public final String mQuery;
        public final Bundle mExtras;
        public final Result<List<MediaItem>> mResult;

        SearchRequest(ControllerInfo controller, RemoteUserInfo remoteUserInfo, String query,
                Bundle extras, Result<List<MediaItem>> result) {
            mController = controller;
            mRemoteUserInfo = remoteUserInfo;
            mQuery = query;
            mExtras = extras;
            mResult = result;
        }
    }

    // Base class for MediaBrowserCompat's ControllerCb.
    // This documents
    //   1) Why some APIs does nothing
    //   2) Why some APIs should throw exception when DEBUG is {@code true}.
    private abstract static class BaseBrowserLegacyCb extends MediaSession2.ControllerCb {
        @Override
        final void onCustomLayoutChanged(List<CommandButton> layout) throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onPlaybackInfoChanged(PlaybackInfo info) throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onAllowedCommandsChanged(SessionCommandGroup2 commands) throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onCustomCommand(SessionCommand2 command, Bundle args, ResultReceiver receiver)
                throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onPlayerStateChanged(long eventTimeMs, long positionMs, int playerState)
                throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onPlaybackSpeedChanged(long eventTimeMs, long positionMs, float speed)
                throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onBufferingStateChanged(MediaItem2 item, int bufferingState,
                long bufferedPositionMs) throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onSeekCompleted(long eventTimeMs, long positionMs, long position)
                throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onError(int errorCode, Bundle extras) throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onCurrentMediaItemChanged(MediaItem2 item) throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onPlaylistChanged(List<MediaItem2> playlist, MediaMetadata2 metadata)
                throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onPlaylistMetadataChanged(MediaMetadata2 metadata) throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onShuffleModeChanged(int shuffleMode) throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onRepeatModeChanged(int repeatMode) throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onRoutesInfoChanged(List<Bundle> routes) throws RemoteException {
            // No-op. BrowserCompat doesn't understand Controller features.
        }

        @Override
        final void onDisconnected() throws RemoteException {
            // No-op. BrowserCompat doesn't have concept of receiving release of a session.
        }

        @Override
        final void onGetLibraryRootDone(Bundle rootHints, String rootMediaId, Bundle rootExtra)
                throws RemoteException {
            // Shouldn't be called. If it's called, it's bug.
            // This method in the base class is introduced to internally send return of
            // {@link MediaLibrarySessionCallback#onGetRoot}. However, for BrowserCompat,
            // it should be done by returning {@link MediaLibraryService2LegacyStub#onGetRoot}
            // instead.
            if (DEBUG) {
                throw new RuntimeException("Unexpected API call. onGetRoot() should return result"
                        + " instead of calling this");
            }
        }

        @Override
        final void onGetChildrenDone(String parentId, int page, int pageSize,
                List<MediaItem2> result, Bundle extras) throws RemoteException {
            // Shouldn't be called. If it's called, it's bug.
            // This method in the base class is introduced to internally send return of
            // {@link MediaLibrarySessionCallback#onGetChildren}. However, for BrowserCompat,
            // it should be done by {@link Result#sendResult} from
            // {@link MediaLibraryService2LegacyStub#onLoadChildren} instead.
            if (DEBUG) {
                throw new RuntimeException("Unexpected API call. Send result.sendResult() for"
                        + " sending onLoadChildren() result instead of this");
            }
        }

        @Override
        final void onGetItemDone(String mediaId, MediaItem2 result) throws RemoteException {
            // Shouldn't be called. If it's called, it's bug.
            // This method in the base class is introduced to internally send return of
            // {@link MediaLibrarySessionCallback#onGetItem}. However, for BrowserCompat,
            // it should be done by {@link Result#sendResult} from
            // {@link MediaLibraryService2LegacyStub#onLoadItem} instead.
            if (DEBUG) {
                throw new RuntimeException("Unexpected API call. Use result.sendResult() for"
                        + " sending onLoadItem() result instead of this");
            }
        }

        @Override
        final void onGetSearchResultDone(String query, int page, int pageSize,
                List<MediaItem2> result, Bundle extras) throws RemoteException {
            // Shouldn't be called. If it's called, it's bug.
            // This method in the base class is introduced to internally send return of
            // {@link MediaLibrarySessionCallback#onGetSearchResult}. However, for BrowserCompat,
            // it should be done by {@link Result#sendResult} from
            // {@link MediaLibraryService2LegacyStub#onSearch} instead.
            if (DEBUG) {
                throw new RuntimeException("Unexpected API call. Use result.sendResult() for"
                        + " sending onGetSearchResult() result instead of this");
            }
        }
    }

    private class BrowserLegacyCb extends BaseBrowserLegacyCb {
        private final Object mLock = new Object();
        private final RemoteUserInfo mRemoteUserInfo;

        @GuardedBy("mLock")
        private final List<SearchRequest> mSearchRequests = new ArrayList<>();

        BrowserLegacyCb(RemoteUserInfo remoteUserInfo) {
            mRemoteUserInfo = remoteUserInfo;
        }

        @Override
        void onChildrenChanged(String parentId, int itemCount, Bundle extras)
                throws RemoteException {
            notifyChildrenChanged(mRemoteUserInfo, parentId, extras);
        }

        @Override
        void onSearchResultChanged(String query, int itemCount, Bundle extras)
                throws RemoteException {
            // In MediaLibrarySession/MediaBrowser2, we have two different APIs for getting size of
            // search result (and also starting search) and getting result.
            // However, MediaBrowserService2/MediaBrowserCompat only have one search API for getting
            // search result.
            final List<SearchRequest> searchRequests = new ArrayList<>();
            synchronized (mLock) {
                for (int i = mSearchRequests.size() - 1; i >= 0; i--) {
                    SearchRequest iter = mSearchRequests.get(i);
                    if (iter.mRemoteUserInfo == mRemoteUserInfo && iter.mQuery.equals(query)) {
                        searchRequests.add(iter);
                        mSearchRequests.remove(i);
                    }
                }
                if (searchRequests.size() == 0) {
                    if (DEBUG) {
                        Log.d(TAG, "search() hasn't called by " + mRemoteUserInfo
                                + " with query=" + query);
                    }
                    return;
                }
            }

            mLibrarySessionImpl.getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < searchRequests.size(); i++) {
                        SearchRequest request = searchRequests.get(i);
                        int page = 0;
                        int pageSize = 0;
                        if (request.mExtras != null) {
                            try {
                                request.mExtras.setClassLoader(
                                        mLibrarySessionImpl.getContext().getClassLoader());
                                page = request.mExtras.getInt(MediaBrowserCompat.EXTRA_PAGE, -1);
                                pageSize = request.mExtras
                                        .getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, -1);
                            } catch (BadParcelableException e) {
                                request.mResult.sendResult(null);
                                return;
                            }
                        }
                        if (page < 0 || pageSize < 1) {
                            page = 0;
                            pageSize = Integer.MAX_VALUE;
                        }
                        List<MediaItem2> searchResult = mLibrarySessionImpl.getCallback()
                                .onGetSearchResult(mLibrarySessionImpl.getInstance(),
                                        request.mController, request.mQuery, page, pageSize,
                                        request.mExtras);
                        if (searchResult == null) {
                            request.mResult.sendResult(null);
                            return;
                        }
                        request.mResult
                                .sendResult(MediaUtils2.convertToMediaItemList(searchResult));
                    }
                }
            });
        }

        void registerSearchRequest(ControllerInfo controller, String query, Bundle extras,
                Result<List<MediaItem>> result) {
            synchronized (mLock) {
                mSearchRequests.add(new SearchRequest(controller, controller.getRemoteUserInfo(),
                        query, extras, result));
            }
        }
    }

    /**
     * Intentionally static class to prevent lint warning 'SynteheticAccessor' in constructor.
     */
    private static class BrowserLegacyCbForAll extends BaseBrowserLegacyCb {
        private final MediaBrowserServiceCompat mService;

        BrowserLegacyCbForAll(MediaBrowserServiceCompat service) {
            mService = service;
        }

        @Override
        void onChildrenChanged(String parentId, int itemCount, Bundle extras)
                throws RemoteException {
            // This will trigger {@link MediaLibraryService2LegacyStub#onLoadChildren}.
            if (extras == null) {
                mService.notifyChildrenChanged(parentId);
            } else {
                mService.notifyChildrenChanged(parentId, extras);
            }
        }

        @Override
        void onSearchResultChanged(String query, int itemCount, Bundle extras)
                throws RemoteException {
            // Shouldn't be called. If it's called, it's bug.
            // This method in the base class is introduced to internally send return of
            // {@link MediaLibrarySessionCallback#onSearchResultChanged}. However, for
            // BrowserCompat, it should be done by {@link Result#sendResult} from
            // {@link MediaLibraryService2LegacyStub#onSearch} instead.
            if (DEBUG) {
                throw new RuntimeException("Unexpected API call. Use result.sendResult() for"
                        + " sending onSearchResultChanged() result instead of this");
            }
        }
    }
}
