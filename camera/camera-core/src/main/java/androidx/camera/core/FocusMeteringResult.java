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

package androidx.camera.core;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * Result of the {@link CameraControl#startFocusAndMetering(FocusMeteringAction)}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class FocusMeteringResult {
    private boolean mIsFocusSuccessful;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public static FocusMeteringResult emptyInstance() {
        return new FocusMeteringResult(false);
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public static FocusMeteringResult create(boolean isFocusSuccess) {
        return new FocusMeteringResult(isFocusSuccess);
    }

    private FocusMeteringResult(boolean isFocusSuccess) {
        mIsFocusSuccessful = isFocusSuccess;
    }

    /**
     * Returns if auto focus is successful.
     *
     * <p>If AF is requested in {@link FocusMeteringAction} but current camera does not support
     * AF, it will return true. If AF is not requested, it will return false.
     */
    public boolean isFocusSuccessful() {
        return mIsFocusSuccessful;
    }
}
