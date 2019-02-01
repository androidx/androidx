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

import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.media.MediaBrowserServiceCompat;
import androidx.media.MediaSessionManager;
import androidx.media.MediaSessionManager.RemoteUserInfo;
import androidx.media2.MediaSession.ControllerInfo;
import androidx.media2.MediaSession.MediaSessionImpl;

import java.util.List;

/**
 * Implementation of {@link MediaBrowserServiceCompat} for interoperability between
 * {@link MediaLibraryService} and {@link android.support.v4.media.MediaBrowserCompat}.
 */
class MediaSessionServiceLegacyStub extends MediaBrowserServiceCompat {
    private final MediaSessionImpl mSessionImpl;
    private final ConnectedControllersManager<RemoteUserInfo> mConnectedControllersManager;

    final MediaSessionManager mManager;

    MediaSessionServiceLegacyStub(Context context, MediaSessionImpl sessionImpl,
            MediaSessionCompat.Token token) {
        super();
        attachToBaseContext(context);
        onCreate();
        setSessionToken(token);
        mManager = MediaSessionManager.getSessionManager(context);
        mSessionImpl = sessionImpl;
        mConnectedControllersManager = new ConnectedControllersManager<>(sessionImpl);
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        RemoteUserInfo info = getCurrentBrowserInfo();
        final ControllerInfo controller = createControllerInfo(info);
        // Call callbacks directly instead of execute on the executor. Here's the reason.
        // We need to return browser root here. So if we run the callback on the executor, we
        // should wait for the completion.
        // However, we cannot wait if the callback executor is the main executor, which posts
        // the runnable to the main thread's. In that case, since this onGetRoot() always runs
        // on the main thread, the posted runnable for calling onConnect() wouldn't run
        // in here. Even worse, we cannot know whether it would be run on the main thread or
        // not.
        // Because of the reason, just call onConnect() directly here. onConnect() has documentation
        // that it may be called on the main thread.
        SessionCommandGroup connectResult = mSessionImpl.getCallback().onConnect(
                mSessionImpl.getInstance(), controller);
        if (connectResult == null) {
            return null;
        }
        mConnectedControllersManager.addController(info, controller, connectResult);
        // No library root, but keep browser compat connected to allow getting session.
        return MediaUtils.sDefaultBrowserRoot;
    }

    @Override
    public void onLoadChildren(String parentId, Result<List<MediaItem>> result) {
        result.sendResult(null);
    }

    ControllerInfo createControllerInfo(RemoteUserInfo info) {
        return new ControllerInfo(info, mManager.isTrustedForMediaControl(info), null);
    }

    ConnectedControllersManager<RemoteUserInfo> getConnectedControllersManager() {
        return mConnectedControllersManager;
    }
}
