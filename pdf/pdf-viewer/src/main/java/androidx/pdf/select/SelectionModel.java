/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.select;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.pdf.models.SelectionBoundary;
import androidx.pdf.util.ObservableValue;
import androidx.pdf.util.Observables;
import androidx.pdf.util.Observables.ExposedValue;

/**
 * Stores data relevant to the current selection.
 *
 * @param <S> Type for the model
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class SelectionModel<S> {

    protected final ExposedValue<S> mSelection =
            Observables.newExposedValueWithInitialValue(null);

    /**
     *
     */
    @NonNull
    public ObservableValue<S> selection() {
        return mSelection;
    }

    /**
     *
     */
    @NonNull
    public abstract String getText();

    /** Synchronous update - the exact selection is already known. */
    public void setSelection(S newSelection) {
        mSelection.set(newSelection);
    }

    /**
     *
     */
    public void updateSelectionAsync(@NonNull SelectionBoundary start,
            @NonNull SelectionBoundary stop) {
        throw new UnsupportedOperationException("No support for updating selection");
    }
}
