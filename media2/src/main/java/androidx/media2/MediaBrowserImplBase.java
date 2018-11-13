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

import static androidx.media2.MediaBrowser.BrowserResult.RESULT_CODE_DISCONNECTED;
import static androidx.media2.MediaBrowser.BrowserResult.RESULT_CODE_PERMISSION_DENIED;
import static androidx.media2.MediaBrowser.BrowserResult.RESULT_CODE_SKIPPED;
import static androidx.media2.SessionCommand.COMMAND_CODE_LIBRARY_GET_CHILDREN;
import static androidx.media2.SessionCommand.COMMAND_CODE_LIBRARY_GET_ITEM;
import static androidx.media2.SessionCommand.COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT;
import static androidx.media2.SessionCommand.COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT;
import static androidx.media2.SessionCommand.COMMAND_CODE_LIBRARY_SEARCH;
import static androidx.media2.SessionCommand.COMMAND_CODE_LIBRARY_SUBSCRIBE;
import static androidx.media2.SessionCommand.COMMAND_CODE_LIBRARY_UNSUBSCRIBE;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import androidx.media2.MediaBrowser.BrowserCallback;
import androidx.media2.MediaBrowser.BrowserResult;
import androidx.media2.MediaLibraryService.LibraryParams;
import androidx.media2.SequencedFutureManager.SequencedFuture;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * Base implementation of MediaBrowser.
 */
class MediaBrowserImplBase extends MediaControllerImplBase implements
        MediaBrowser.MediaBrowserImpl {
    private static final BrowserResult RESULT_WHEN_CLOSED =
            new BrowserResult(RESULT_CODE_SKIPPED);

    MediaBrowserImplBase(Context context, MediaController instance, SessionToken token,
            Executor executor, BrowserCallback callback) {
        super(context, instance, token, executor, callback);
    }

    @Override
    public BrowserCallback getCallback() {
        return (BrowserCallback) super.getCallback();
    }

    @Override
    public ListenableFuture<BrowserResult> getLibraryRoot(final LibraryParams params) {
        return dispatchRemoteLibrarySessionTask(COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT,
                new RemoteLibrarySessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.getLibraryRoot(mControllerStub, seq,
                                MediaUtils.toParcelable(params));
                    }
                });
    }

    @Override
    public ListenableFuture<BrowserResult> subscribe(final String parentId,
            final LibraryParams params) {
        return dispatchRemoteLibrarySessionTask(COMMAND_CODE_LIBRARY_SUBSCRIBE,
                new RemoteLibrarySessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.subscribe(mControllerStub, seq, parentId,
                                MediaUtils.toParcelable(params));
                    }
                });
    }

    @Override
    public ListenableFuture<BrowserResult> unsubscribe(final String parentId) {
        return dispatchRemoteLibrarySessionTask(COMMAND_CODE_LIBRARY_UNSUBSCRIBE,
                new RemoteLibrarySessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.unsubscribe(mControllerStub, seq, parentId);
                    }
                });
    }

    @Override
    public ListenableFuture<BrowserResult> getChildren(final String parentId, final int page,
            final int pageSize, final LibraryParams params) {
        return dispatchRemoteLibrarySessionTask(COMMAND_CODE_LIBRARY_GET_CHILDREN,
                new RemoteLibrarySessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.getChildren(mControllerStub, seq, parentId, page, pageSize,
                                MediaUtils.toParcelable(params));
                    }
                });
    }

    @Override
    public ListenableFuture<BrowserResult> getItem(final String mediaId) {
        return dispatchRemoteLibrarySessionTask(COMMAND_CODE_LIBRARY_GET_ITEM,
                new RemoteLibrarySessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.getItem(mControllerStub, seq, mediaId);
                    }
                });
    }

    @Override
    public ListenableFuture<BrowserResult> search(final String query, final LibraryParams params) {
        return dispatchRemoteLibrarySessionTask(COMMAND_CODE_LIBRARY_SEARCH,
                new RemoteLibrarySessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.search(mControllerStub, seq, query,
                                MediaUtils.toParcelable(params));
                    }
                });
    }

    @Override
    public ListenableFuture<BrowserResult> getSearchResult(final String query, final int page,
            final int pageSize, final LibraryParams params) {
        return dispatchRemoteLibrarySessionTask(COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT,
                new RemoteLibrarySessionTask() {
                    @Override
                    public void run(IMediaSession iSession, int seq) throws RemoteException {
                        iSession.getSearchResult(mControllerStub, seq, query, page, pageSize,
                                MediaUtils.toParcelable(params));
                    }
                });
    }

    @FunctionalInterface
    private interface RemoteLibrarySessionTask {
        void run(IMediaSession iSession, int seq) throws RemoteException;
    }

    private ListenableFuture<BrowserResult> dispatchRemoteLibrarySessionTask(int commandCode,
            RemoteLibrarySessionTask task) {
        final IMediaSession iSession = getSessionInterfaceIfAble(commandCode);
        if (iSession != null) {
            final SequencedFuture<BrowserResult> result =
                    mSequencedFutureManager.createSequencedFuture(RESULT_WHEN_CLOSED);
            try {
                task.run(iSession, result.getSequenceNumber());
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
                result.set(new BrowserResult(RESULT_CODE_DISCONNECTED));
            }
            return result;
        } else {
            // Don't create Future with SequencedFutureManager.
            // Otherwise session would receive discontinued sequence number, and it would make
            // future work item 'keeping call sequence when session execute commands' impossible.
            return BrowserResult.createFutureWithResult(RESULT_CODE_PERMISSION_DENIED);
        }
    }
}
