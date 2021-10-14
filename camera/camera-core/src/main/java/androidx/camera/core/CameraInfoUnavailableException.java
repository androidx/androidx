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

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

/** An exception thrown when unable to retrieve information about a camera. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class CameraInfoUnavailableException extends Exception {
    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public CameraInfoUnavailableException(String s, Throwable e) {
        super(s, e);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public CameraInfoUnavailableException(String s) {
        super(s);
    }
}
