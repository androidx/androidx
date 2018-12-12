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

import static android.app.slice.Slice.HINT_SELECTED;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.SUBTYPE_CONTENT_DESCRIPTION;
import static android.app.slice.Slice.SUBTYPE_LAYOUT_DIRECTION;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.slice.core.SliceHints.HINT_SELECTION_OPTION_VALUE;
import static androidx.slice.core.SliceHints.SUBTYPE_SELECTION_OPTION_KEY;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.util.Pair;
import androidx.slice.Slice;
import androidx.slice.builders.SelectionBuilder;

import java.util.List;

/**
 * @hide
 */
@RestrictTo(LIBRARY)
@RequiresApi(19)
public class SelectionBuilderV1Impl extends SelectionBuilderImpl {
    public SelectionBuilderV1Impl(Slice.Builder parentSliceBuilder,
                                  SelectionBuilder selectionBuilder) {
        super(parentSliceBuilder, selectionBuilder);
    }

    @Override
    public void apply(Slice.Builder sliceBuilder) {
        final SelectionBuilder selectionBuilder = getSelectionBuilder();

        selectionBuilder.check();

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

        final List<Pair<String, CharSequence>> options = selectionBuilder.getOptions();
        for (Pair<String, CharSequence> option : options) {
            final Slice.Builder optionSubSliceBuilder = new Slice.Builder(sliceBuilder);
            if (option.first.equals(selectionBuilder.getSelectedOption())) {
                optionSubSliceBuilder.addHints(HINT_SELECTED);
            }
            optionSubSliceBuilder.addText(option.first, SUBTYPE_SELECTION_OPTION_KEY);
            optionSubSliceBuilder.addText(option.second, null, HINT_SELECTION_OPTION_VALUE);
            sliceBuilder.addSubSlice(optionSubSliceBuilder.build());
        }
    }
}
