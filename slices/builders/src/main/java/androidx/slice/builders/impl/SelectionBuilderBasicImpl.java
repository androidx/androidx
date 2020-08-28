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

import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.SUBTYPE_CONTENT_DESCRIPTION;
import static android.app.slice.Slice.SUBTYPE_LAYOUT_DIRECTION;

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
public class SelectionBuilderBasicImpl extends SelectionBuilderImpl {
    public SelectionBuilderBasicImpl(Slice.Builder sliceBuilder,
                                     SelectionBuilder selectionBuilder) {
        super(sliceBuilder, selectionBuilder);
    }

    @Override
    public void apply(@NonNull Slice.Builder sliceBuilder) {
        final SelectionBuilder selectionBuilder = getSelectionBuilder();

        selectionBuilder.check();

        // TODO: This should ideally be in ListBuilder, not here.
        sliceBuilder.addHints(HINT_LIST_ITEM);

        selectionBuilder.getPrimaryAction().setPrimaryAction(sliceBuilder);

        if (selectionBuilder.getTitle() != null) {
            sliceBuilder.addText(selectionBuilder.getTitle(), null, HINT_TITLE);
        }

        if (selectionBuilder.getSubtitle() != null) {
            sliceBuilder.addText(selectionBuilder.getSubtitle(), null);
        }

        if (selectionBuilder.getContentDescription() != null) {
            sliceBuilder.addText(selectionBuilder.getContentDescription(),
                    SUBTYPE_CONTENT_DESCRIPTION);
        }

        if (selectionBuilder.getLayoutDirection() != -1) {
            sliceBuilder.addInt(selectionBuilder.getLayoutDirection(), SUBTYPE_LAYOUT_DIRECTION);
        }
    }
}
