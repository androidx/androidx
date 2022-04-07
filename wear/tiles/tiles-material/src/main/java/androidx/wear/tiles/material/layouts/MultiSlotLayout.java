/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.tiles.material.layouts;

import static androidx.annotation.Dimension.DP;
import static androidx.wear.tiles.DimensionBuilders.dp;
import static androidx.wear.tiles.DimensionBuilders.expand;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.MULTI_SLOT_LAYOUT_HORIZONTAL_SPACER_WIDTH;

import android.annotation.SuppressLint;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.DimensionBuilders.DpProp;
import androidx.wear.tiles.LayoutElementBuilders;
import androidx.wear.tiles.LayoutElementBuilders.Box;
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement;
import androidx.wear.tiles.LayoutElementBuilders.Row;
import androidx.wear.tiles.LayoutElementBuilders.Spacer;
import androidx.wear.tiles.proto.LayoutElementProto;

import java.util.ArrayList;
import java.util.List;

/**
 * Opinionated Tiles layout, row like style with horizontally aligned and spaced slots (for icons or
 * other small content). Should be used as a content passed in to the {@link PrimaryLayout}.
 *
 * <p>Recommended number of added slots is 1 to 3. Their width will be scaled to fit and have the
 * same value, with the {@link LayoutDefaults#MULTI_SLOT_LAYOUT_HORIZONTAL_SPACER_WIDTH} space
 * between.
 */
// TODO(b/215323986): Link visuals.
public class MultiSlotLayout implements LayoutElement {
    @NonNull private final Row mElement;

    MultiSlotLayout(@NonNull Row mElement) {
        this.mElement = mElement;
    }

    /** Builder class for {@link MultiSlotLayout}. */
    public static final class Builder implements LayoutElement.Builder {

        @NonNull private final List<LayoutElement> mSlotsContent = new ArrayList<>();
        @NonNull private DpProp mHorizontalSpacerWidth = MULTI_SLOT_LAYOUT_HORIZONTAL_SPACER_WIDTH;

        /**
         * Creates a builder for the {@link MultiSlotLayout}. Content inside of it can later be
         * added with {@link #addSlotContent}.
         */
        public Builder() {}

        /** Add one new slot to the layout with the given content inside. */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        // There is no direct matching getter for this setter as the serialized format of the
        // ProtoLayouts do not allow for a direct reconstruction of the arguments. b/221427609
        public Builder addSlotContent(@NonNull LayoutElement slotContent) {
            mSlotsContent.add(slotContent);
            return this;
        }

        /**
         * Sets the horizontal spacer width which is used as a space between slots if there is more
         * than one slot. If not set, {@link
         * LayoutDefaults#MULTI_SLOT_LAYOUT_HORIZONTAL_SPACER_WIDTH} will be used.
         */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        // There is no direct matching getter for this setter as the serialized format of the
        // ProtoLayouts do not allow for a direct reconstruction of the arguments. Instead there are
        // methods to get the contents a whole for rendering.
        public Builder setHorizontalSpacerWidth(@Dimension(unit = DP) float width) {
            this.mHorizontalSpacerWidth = dp(width);
            return this;
        }

        @NonNull
        @Override
        // The @Dimension(unit = DP) on mVerticalSpacerHeight.getValue() is seemingly being ignored,
        // so lint complains that we're passing PX to something expecting DP. Just suppress the
        // warning for now.
        @SuppressLint("ResourceType")
        public MultiSlotLayout build() {
            Row.Builder rowBuilder =
                    new Row.Builder()
                            .setHeight(expand())
                            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                            .setWidth(expand());
            if (!mSlotsContent.isEmpty()) {

                boolean isFirst = true;
                for (LayoutElement slot : mSlotsContent) {
                    if (!isFirst) {
                        rowBuilder.addContent(
                                new Spacer.Builder().setWidth(mHorizontalSpacerWidth).build());
                    } else {
                        isFirst = false;
                    }
                    rowBuilder.addContent(
                            new Box.Builder()
                                    .setWidth(expand())
                                    .setHeight(expand())
                                    .addContent(slot)
                                    .build());
                }
            }

            return new MultiSlotLayout(rowBuilder.build());
        }
    }

    /** Gets the content from this layout, containing all slots that were added. */
    @NonNull
    public List<LayoutElement> getSlotContents() {
        List<LayoutElement> slots = new ArrayList<>();
        for (LayoutElement slot : mElement.getContents()) {
            if (slot instanceof Box) {
                slots.add(((Box) slot).getContents().get(0));
            }
        }
        return slots;
    }

    /** @hide */
    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return mElement.toLayoutElementProto();
    }
}
