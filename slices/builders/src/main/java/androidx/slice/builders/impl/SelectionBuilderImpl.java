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

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.slice.Slice;
import androidx.slice.builders.SelectionBuilder;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

/**
 * @hide
 */
@RestrictTo(LIBRARY)
@RequiresApi(19)
public abstract class SelectionBuilderImpl {
    private final Slice.Builder mParentSliceBuilder;
    private final SelectionBuilder mSelectionBuilder;

    public SelectionBuilderImpl(Slice.Builder parentSliceBuilder,
                                SelectionBuilder selectionBuilder) {
        mParentSliceBuilder = parentSliceBuilder;
        mSelectionBuilder = selectionBuilder;
    }

    /**
     * Builds a {@link Slice} representing the selection passed in the constructor.
     *
     * The slice will be built as a sub-slice of the slice being built by the {@link Slice.Builder}
     * passed to the constructor.
     *
     * @return the constructed slice
     */
    public final Slice build() {
        mSelectionBuilder.check();
        final Slice.Builder sliceBuilder = new Slice.Builder(mParentSliceBuilder);
        apply(mSelectionBuilder, sliceBuilder);
        return sliceBuilder.build();
    }

    /**
     * Applies the information in selectionBuilder to the {@link Slice} being built in sliceBuilder.
     *
     * @param selectionBuilder the {@link SelectionBuilder} that contains the selection.
     * @param sliceBuilder the {@link Slice.Builder} into which the {@link Slice} will be built.
     */
    protected abstract void apply(SelectionBuilder selectionBuilder, Slice.Builder sliceBuilder);
}
