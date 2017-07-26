/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static android.support.mediacompat.testlib.MediaBrowserConstants.EXTRAS_KEY;
import static android.support.mediacompat.testlib.MediaBrowserConstants.EXTRAS_VALUE;
import static android.support.mediacompat.testlib.MediaBrowserConstants.MEDIA_ID_ROOT;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.session.MediaSessionCompat;

import java.util.List;

/**
 * Stub implementation of {@link android.support.v4.media.MediaBrowserServiceCompat}.
 */
public class StubMediaBrowserServiceCompat extends MediaBrowserServiceCompat {

    public static MediaSessionCompat sSession;
    private Bundle mExtras;

    @Override
    public void onCreate() {
        super.onCreate();
        sSession = new MediaSessionCompat(this, "StubMediaBrowserServiceCompat");
        setSessionToken(sSession.getSessionToken());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sSession.release();
        sSession = null;
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        mExtras = new Bundle();
        mExtras.putString(EXTRAS_KEY, EXTRAS_VALUE);
        return new BrowserRoot(MEDIA_ID_ROOT, mExtras);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaItem>> result) {
        // Will be implemented.
    }
}
