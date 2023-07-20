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

package androidx.compose.ui.platform.coreshims;

import android.view.autofill.AutofillId;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * Helper for accessing features in {@link AutofillId}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AutofillIdCompat {
    // Only guaranteed to be non-null on SDK_INT >= 26.
    private final Object mWrappedObj;

    @RequiresApi(26)
    private AutofillIdCompat(@NonNull AutofillId obj) {
        mWrappedObj = obj;
    }

    /**
     * Provides a backward-compatible wrapper for {@link AutofillId}.
     * <p>
     * This method is not supported on devices running SDK < 26 since the platform
     * class will not be available.
     *
     * @param autofillId platform class to wrap
     * @return wrapped class
     */
    @RequiresApi(26)
    @NonNull
    public static AutofillIdCompat toAutofillIdCompat(@NonNull AutofillId autofillId) {
        return new AutofillIdCompat(autofillId);
    }

    /**
     * Provides the {@link AutofillId} represented by this object.
     * <p>
     * This method is not supported on devices running SDK < 26 since the platform
     * class will not be available.
     *
     * @return platform class object
     * @see AutofillIdCompat#toAutofillIdCompat(AutofillId)
     */
    @RequiresApi(26)
    @NonNull
    public AutofillId toAutofillId() {
        return (AutofillId) mWrappedObj;
    }
}
