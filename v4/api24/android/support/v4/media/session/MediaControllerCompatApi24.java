/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.v4.media.session;

import android.media.session.MediaController;
import android.net.Uri;
import android.os.Bundle;

class MediaControllerCompatApi24 {

    public static class TransportControls extends MediaControllerCompatApi23.TransportControls {
        public static void prepare(Object controlsObj) {
            ((MediaController.TransportControls) controlsObj).prepare();
        }

        public static void prepareFromMediaId(Object controlsObj, String mediaId, Bundle extras) {
            ((MediaController.TransportControls) controlsObj).prepareFromMediaId(mediaId, extras);
        }

        public static void prepareFromSearch(Object controlsObj, String query, Bundle extras) {
            ((MediaController.TransportControls) controlsObj).prepareFromSearch(query, extras);
        }

        public static void prepareFromUri(Object controlsObj, Uri uri, Bundle extras) {
            ((MediaController.TransportControls) controlsObj).prepareFromUri(uri, extras);
        }
    }
}
