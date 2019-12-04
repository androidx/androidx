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

package androidx.message.browser;

import android.os.BadParcelableException;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

final class MessageUtils {
    static final String TAG = "MessageUtils";
    static final int LIBRARY_VERSION = 1;

    /**
     * Tries to unparcel the given {@link Bundle} with the application class loader and
     * returns {@code null} if a {@link BadParcelableException} is thrown while unparcelling,
     * otherwise the given bundle in which the application class loader is set.
     */
    @Nullable
    static Bundle unparcelWithClassLoader(@Nullable Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        bundle.setClassLoader(MessageUtils.class.getClassLoader());
        try {
            bundle.isEmpty(); // to call unparcel()
            return bundle;
        } catch (BadParcelableException e) {
            // The exception details will be logged by Parcel class.
            Log.e(TAG, "Could not unparcel the data.");
            return null;
        }
    }

    private MessageUtils() {
    }
}
