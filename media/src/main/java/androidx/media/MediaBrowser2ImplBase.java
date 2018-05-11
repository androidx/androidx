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

import static androidx.media.SessionCommand2.COMMAND_CODE_LIBRARY_GET_CHILDREN;
import static androidx.media.SessionCommand2.COMMAND_CODE_LIBRARY_GET_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT;
import static androidx.media.SessionCommand2.COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT;
import static androidx.media.SessionCommand2.COMMAND_CODE_LIBRARY_SEARCH;
import static androidx.media.SessionCommand2.COMMAND_CODE_LIBRARY_SUBSCRIBE;
import static androidx.media.SessionCommand2.COMMAND_CODE_LIBRARY_UNSUBSCRIBE;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import androidx.media.MediaBrowser2.BrowserCallback;

import java.util.concurrent.Executor;

/**
 * Base implementation of MediaBrowser2.
 */
class MediaBrowser2ImplBase extends MediaController2ImplBase implements
        MediaBrowser2.SupportLibraryImpl {
    MediaBrowser2ImplBase(Context context, MediaController2 instance, SessionToken2 token,
            Executor executor, BrowserCallback callback) {
        super(context, instance, token, executor, callback);
    }

    @Override
    public BrowserCallback getCallback() {
        return (BrowserCallback) super.getCallback();
    }

    @Override
    public void getLibraryRoot(Bundle rootHints) {
        final IMediaSession2 iSession2 = getSessionInterfaceIfAble(
                COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT);
        if (iSession2 != null) {
            try {
                iSession2.getLibraryRoot(mControllerStub, rootHints);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void subscribe(String parentId, Bundle extras) {
        final IMediaSession2 iSession2 = getSessionInterfaceIfAble(COMMAND_CODE_LIBRARY_SUBSCRIBE);
        if (iSession2 != null) {
            try {
                iSession2.subscribe(mControllerStub, parentId, extras);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void unsubscribe(String parentId) {
        final IMediaSession2 iSession2 = getSessionInterfaceIfAble(
                COMMAND_CODE_LIBRARY_UNSUBSCRIBE);
        if (iSession2 != null) {
            try {
                iSession2.unsubscribe(mControllerStub, parentId);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void getChildren(String parentId, int page, int pageSize, Bundle extras) {
        final IMediaSession2 iSession2 = getSessionInterfaceIfAble(
                COMMAND_CODE_LIBRARY_GET_CHILDREN);
        if (iSession2 != null) {
            try {
                iSession2.getChildren(mControllerStub, parentId, page, pageSize, extras);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void getItem(String mediaId) {
        final IMediaSession2 iSession2 = getSessionInterfaceIfAble(COMMAND_CODE_LIBRARY_GET_ITEM);
        if (iSession2 != null) {
            try {
                iSession2.getItem(mControllerStub, mediaId);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void search(String query, Bundle extras) {
        final IMediaSession2 iSession2 = getSessionInterfaceIfAble(COMMAND_CODE_LIBRARY_SEARCH);
        if (iSession2 != null) {
            try {
                iSession2.search(mControllerStub, query, extras);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @Override
    public void getSearchResult(String query, int page, int pageSize,
            Bundle extras) {
        final IMediaSession2 iSession2 = getSessionInterfaceIfAble(
                COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT);
        if (iSession2 != null) {
            try {
                iSession2.getSearchResult(mControllerStub, query, page, pageSize, extras);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }
}
