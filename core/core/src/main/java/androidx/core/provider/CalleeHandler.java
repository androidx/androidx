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

package androidx.core.provider;


import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

class CalleeHandler {
    private CalleeHandler() { }

    /**
     * Utility function to create a handler for a callee, when no Handler is provided.
     * If the current Thread has a Looper defined uses the current Thread
     * looper. Otherwise uses main Looper for the as the Handler.
     */
    @NonNull
    static Handler create() {
        final Handler handler;
        if (Looper.myLooper() == null) {
            handler = new Handler(Looper.getMainLooper());
        } else {
            handler = new Handler();
        }
        return handler;
    }
}
