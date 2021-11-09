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

import static androidx.media2.session.LibraryResult.RESULT_ERROR_PERMISSION_DENIED;
import static androidx.media2.session.LibraryResult.RESULT_ERROR_SESSION_DISCONNECTED;
import static androidx.media2.session.LibraryResult.RESULT_INFO_SKIPPED;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_LIBRARY_GET_CHILDREN;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_LIBRARY_GET_ITEM;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_LIBRARY_SEARCH;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_LIBRARY_SUBSCRIBE;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_LIBRARY_UNSUBSCRIBE;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media2.common.MediaParcelUtils;
import androidx.media2.session.MediaBrowser.BrowserCallback;
import androidx.media2.session.MediaBrowser.BrowserCallbackRunnable;
import androidx.media2.session.MediaLibraryService.LibraryParams;
import androidx.media2.session.SequencedFutureManager.SequencedFuture;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Base implementation of MediaBrowser.
 */
class MediaBrowserImplBase extends MediaControllerImplBase implements
        MediaBrowser.MediaBrowserImpl {
    private static final LibraryResult RESULT_WHEN_CLOSED =
            new LibraryResult(RESULT_INFO_SKIPPED);

    MediaBrowserImplBase(Context context, MediaController instance, SessionToken token,
            @Nullable Bundle connectionHints) {
        super(context, instance, token, connectionHints);
    }

    @NonNull
    MediaBrowser getMediaBrowser() {
        return (MediaBrowser) mInstance;
    }

    @Override
    public ListenableFuture<LibraryResult> getLibraryRoot(final LibraryParams params) {
        return dispatchRemoteLibrarySessionTask(COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT,
                new RemoteLibrarySessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.getLibraryRoot(mControllerStub, seq,
                                MediaParcelUtils.toParcelable(params));
                    }
                });
    }

    @Override
    public ListenableFuture<LibraryResult> subscribe(final String parentId,
            final LibraryParams params) {
        return dispatchRemoteLibrarySessionTask(COMMAND_CODE_LIBRARY_SUBSCRIBE,
                new RemoteLibrarySessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.subscribe(mControllerStub, seq, parentId,
                                MediaParcelUtils.toParcelable(params));
                    }
                });
    }

    @Override
    public ListenableFuture<LibraryResult> unsubscribe(final String parentId) {
        return dispatchRemoteLibrarySessionTask(COMMAND_CODE_LIBRARY_UNSUBSCRIBE,
                new RemoteLibrarySessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.unsubscribe(mControllerStub, seq, parentId);
                    }
                });
    }

    @Override
    public ListenableFuture<LibraryResult> getChildren(final String parentId, final int page,
            final int pageSize, final LibraryParams params) {
        return dispatchRemoteLibrarySessionTask(COMMAND_CODE_LIBRARY_GET_CHILDREN,
                new RemoteLibrarySessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.getChildren(mControllerStub, seq, parentId, page, pageSize,
                                MediaParcelUtils.toParcelable(params));
                    }
                });
    }

    @Override
    public ListenableFuture<LibraryResult> getItem(final String mediaId) {
        return dispatchRemoteLibrarySessionTask(COMMAND_CODE_LIBRARY_GET_ITEM,
                new RemoteLibrarySessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.getItem(mControllerStub, seq, mediaId);
                    }
                });
    }

    @Override
    public ListenableFuture<LibraryResult> search(final String query, final LibraryParams params) {
        return dispatchRemoteLibrarySessionTask(COMMAND_CODE_LIBRARY_SEARCH,
                new RemoteLibrarySessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.search(mControllerStub, seq, query,
                                MediaParcelUtils.toParcelable(params));
                    }
                });
    }

    @Override
    public ListenableFuture<LibraryResult> getSearchResult(final String query, final int page,
            final int pageSize, final LibraryParams params) {
        return dispatchRemoteLibrarySessionTask(
                COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT,
                new RemoteLibrarySessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.getSearchResult(mControllerStub, seq, query, page, pageSize,
                                MediaParcelUtils.toParcelable(params));
                    }
                });
    }

    void notifySearchResultChanged(final String query, final int itemCount,
            final LibraryParams libraryParams) {
        getMediaBrowser().notifyBrowserCallback(new BrowserCallbackRunnable() {
            @Override
            public void run(@NonNull BrowserCallback callback) {
                callback.onSearchResultChanged(getMediaBrowser(), query, itemCount, libraryParams);
            }
        });
    }

    void notifyChildrenChanged(final String parentId, final int itemCount,
            final LibraryParams libraryParams) {
        getMediaBrowser().notifyBrowserCallback(new BrowserCallbackRunnable() {
            @Override
            public void run(@NonNull BrowserCallback callback) {
                callback.onChildrenChanged(getMediaBrowser(), parentId, itemCount, libraryParams);
            }
        });
    }

    private ListenableFuture<LibraryResult> dispatchRemoteLibrarySessionTask(int commandCode,
            RemoteLibrarySessionTask task) {
        final IMediaSession iSession = getSessionInterfaceIfAble(commandCode);
        if (iSession != null) {
            final SequencedFuture<LibraryResult> result =
                    mSequencedFutureManager.createSequencedFuture(RESULT_WHEN_CLOSED);
            try {
                task.run(iSession, result.getSequenceNumber());
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
                result.set(new LibraryResult(RESULT_ERROR_SESSION_DISCONNECTED));
            }
            return result;
        } else {
            // Don't create Future with SequencedFutureManager.
            // Otherwise session would receive discontinued sequence number, and it would make
            // future work item 'keeping call sequence when session execute commands' impossible.
            return LibraryResult.createFutureWithResult(RESULT_ERROR_PERMISSION_DENIED);
        }
    }

    @FunctionalInterface
    private interface RemoteLibrarySessionTask {
        void run(IMediaSession iSession, int seq) throws RemoteException;
    }
}
