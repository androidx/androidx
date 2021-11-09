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

package androidx.media2.widget;

import android.net.Uri;

import androidx.annotation.NonNull;

class UriUtil {

    static boolean isFromNetwork(@NonNull Uri uri) {
        String scheme = uri.getScheme();
        if (scheme == null) {
            return false;
        }
        return scheme.equals("http") || scheme.equals("https") || scheme.equals("rtsp");
    }

    private UriUtil() {
    }

}
