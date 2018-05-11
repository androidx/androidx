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

import android.content.Intent;
import android.os.IBinder;

import androidx.media.MediaLibraryService2.MediaLibrarySession;

/**
 * Implementation of {@link MediaLibraryService2}.
 */
class MediaLibraryService2ImplBase extends MediaSessionService2ImplBase {
    @Override
    public IBinder onBind(Intent intent) {
        switch (intent.getAction()) {
            case MediaLibraryService2.SERVICE_INTERFACE:
                return getSession().getSessionBinder();
            case MediaBrowserServiceCompat.SERVICE_INTERFACE:
                return getSession().getImpl().getLegacySessionBinder();
        }
        return super.onBind(intent);
    }

    @Override
    public MediaLibrarySession getSession() {
        return (MediaLibrarySession) super.getSession();
    }

    @Override
    public int getSessionType() {
        return SessionToken2.TYPE_LIBRARY_SERVICE;
    }
}
