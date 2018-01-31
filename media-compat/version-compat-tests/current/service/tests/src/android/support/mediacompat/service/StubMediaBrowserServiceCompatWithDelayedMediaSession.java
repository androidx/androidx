/*
 * Copyright 2017 The Android Open Source Project
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

package android.support.mediacompat.service;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.session.MediaSessionCompat;

import java.util.List;

/**
 * Stub implementation of {@link MediaBrowserServiceCompat}.
 * This implementation does not call
 * {@link MediaBrowserServiceCompat#setSessionToken(MediaSessionCompat.Token)} in its
 * {@link android.app.Service#onCreate}.
 */
public class StubMediaBrowserServiceCompatWithDelayedMediaSession extends
        MediaBrowserServiceCompat {

    static StubMediaBrowserServiceCompatWithDelayedMediaSession sInstance;
    private MediaSessionCompat mSession;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        mSession = new MediaSessionCompat(
                this, "StubMediaBrowserServiceCompatWithDelayedMediaSession");
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName,
            int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot("StubRootId", null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId,
            @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.detach();
    }

    public void callSetSessionToken() {
        setSessionToken(mSession.getSessionToken());
    }
}
