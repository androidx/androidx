/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import android.graphics.Rect;

/** Listener called when focus scan has completed. */
public interface OnFocusListener {
    /** Callback when focus has been locked. */
    void onFocusLocked(Rect afRect);

    /** Callback when unable to acquire focus. */
    void onFocusUnableToLock(Rect afRect);

    /** Callback when timeout is reached and af state haven't settled. */
    void onFocusTimedOut(Rect afRect);
}
