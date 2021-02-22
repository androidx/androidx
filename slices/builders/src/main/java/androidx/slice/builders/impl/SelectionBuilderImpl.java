/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.slice.builders.impl;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.slice.Slice;
import androidx.slice.builders.SelectionBuilder;

/**
 * @hide
 */
@RestrictTo(LIBRARY)
@RequiresApi(19)
public abstract class SelectionBuilderImpl extends TemplateBuilderImpl {
    private final SelectionBuilder mSelectionBuilder;

    public SelectionBuilderImpl(Slice.Builder sliceBuilder,
                                SelectionBuilder selectionBuilder) {
        super(sliceBuilder, null);
        mSelectionBuilder = selectionBuilder;
    }

    /**
     * Applies the selection returned by {@link #getSelectionBuilder()} to sliceBuilder.
     * @param sliceBuilder the {@link Slice.Builder} into which the selection will be built
     */
    @Override
    public abstract void apply(@NonNull Slice.Builder sliceBuilder);

    protected SelectionBuilder getSelectionBuilder() {
        return mSelectionBuilder;
    }
}
