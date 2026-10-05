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

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

/**
 * Result of the {@link CameraControl#startFocusAndMetering(FocusMeteringAction)}.
 */
public final class FocusMeteringResult {
    private boolean mIsFocusSuccessful;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static @NonNull FocusMeteringResult emptyInstance() {
        return new FocusMeteringResult(false);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static @NonNull FocusMeteringResult create(boolean isFocusSuccess) {
        return new FocusMeteringResult(isFocusSuccess);
    }

    private FocusMeteringResult(boolean isFocusSuccess) {
        mIsFocusSuccessful = isFocusSuccess;
    }

    /**
     * Returns whether auto focus is successful.
     *
     * <p>Auto focus is successful if focus is locked when the {@link FocusMeteringAction}
     * completes, for example when the autofocus scan triggered for the action's AF
     * {@link MeteringPoint}s converges. If the camera applies the AF points but doesn't support
     * triggering an autofocus scan, auto focus is also considered successful.
     *
     * <p>Auto focus is not successful if, for example, the action has no AF points, the camera
     * doesn't support AF regions, {@link FocusMeteringAction#FLAG_AF} is excluded from the locking
     * mode and focus isn't already locked, or autofocus fails or doesn't complete in time.
     *
     * @return {@code true} if auto focus is successful, {@code false} otherwise.
     */
    public boolean isFocusSuccessful() {
        return mIsFocusSuccessful;
    }
}
