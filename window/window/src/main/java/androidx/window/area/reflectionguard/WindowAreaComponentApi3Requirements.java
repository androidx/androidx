/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.window.area.reflectionguard;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.window.extensions.area.ExtensionWindowAreaPresentation;
import androidx.window.extensions.area.ExtensionWindowAreaStatus;
import androidx.window.extensions.area.WindowAreaComponent;
import androidx.window.extensions.core.util.function.Consumer;


/**
 * This file defines the Vendor API Level 3 Requirements for WindowAreaComponent. This is used
 * in the client library to perform reflection guard to ensure that the OEM extension implementation
 * is complete.
 *
 * @see WindowAreaComponent
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface WindowAreaComponentApi3Requirements extends WindowAreaComponentApi2Requirements {

    /** @see WindowAreaComponent#addRearDisplayPresentationStatusListener */
    void addRearDisplayPresentationStatusListener(
            @NonNull Consumer<ExtensionWindowAreaStatus> consumer);

    /** @see WindowAreaComponent#removeRearDisplayPresentationStatusListener */
    void removeRearDisplayPresentationStatusListener(
            @NonNull Consumer<ExtensionWindowAreaStatus> consumer);

    /** @see WindowAreaComponent#startRearDisplayPresentationSession */
    void startRearDisplayPresentationSession(@NonNull Activity activity,
            @NonNull Consumer<Integer> consumer);

    /** @see WindowAreaComponent#endRearDisplayPresentationSession */
    void endRearDisplayPresentationSession();

    /** @see WindowAreaComponent#getRearDisplayPresentation */
    @Nullable
    ExtensionWindowAreaPresentation getRearDisplayPresentation();
}
