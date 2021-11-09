/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.media2.player;

import android.os.PersistableBundle;

import androidx.annotation.DoNotInline;
import androidx.annotation.RequiresApi;

final class PersistableBundleHelper {

    @RequiresApi(21)
    static final class Api21Impl {

        @DoNotInline
        static PersistableBundle createInstance() {
            return new PersistableBundle();
        }

        @DoNotInline
        static void putLong(PersistableBundle bundle, String key, Long value) {
            bundle.putLong(key, value);
        }

        @DoNotInline
        static void putString(PersistableBundle bundle, String key, String value) {
            bundle.putString(key, value);
        }

        private Api21Impl() {}
    }

    private PersistableBundleHelper() {}
}
