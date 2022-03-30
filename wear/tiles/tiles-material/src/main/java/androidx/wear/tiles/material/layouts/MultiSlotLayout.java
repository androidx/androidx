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
import static androidx.wear.tiles.material.layouts.LayoutDefaults.DEFAULT_VERTICAL_SPACER_HEIGHT;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.MULTI_SLOT_LAYOUT_HORIZONTAL_SPACER_WIDTH;

import android.annotation.SuppressLint;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters;
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
 * Opinionated Tiles layout style with optional primary and secondary Labels on rows 1 and 3, row 2
 * is a row of horizontally aligned and spaced slots (for icons or other small content). Followed by
 * a 4th row that contains a primary (compact) chip.
 *
 * <p>Recommended number of added slots is 1 to 3. Their width will be scaled to fit and have the
 * same value, with the {@link LayoutDefaults#MULTI_SLOT_LAYOUT_HORIZONTAL_SPACER_WIDTH} space
 * between.
 */
// TODO(b/215323986): Link visuals.
public class MultiSlotLayout implements LayoutElement {
    @NonNull private final PrimaryLayout mElement;

    MultiSlotLayout(@NonNull PrimaryLayout mElement) {
        this.mElement = mElement;
    }

    /** Builder class for {@link MultiSlotLayout}. */
    public static final class Builder implements LayoutElement.Builder {
        @NonNull private final DeviceParameters mDeviceParameters;
        @Nullable private LayoutElement mPrimaryChip = null;
        @Nullable private LayoutElement mPrimaryLabelText = null;
        @Nullable private LayoutElement mSecondaryLabelText = null;
        @NonNull private final List<LayoutElement> mSlotsContent = new ArrayList<>();
        @NonNull private DpProp mHorizontalSpacerWidth = MULTI_SLOT_LAYOUT_HORIZONTAL_SPACER_WIDTH;
        @NonNull private DpProp mVerticalSpacerHeight = DEFAULT_VERTICAL_SPACER_HEIGHT;

        /**
         * Creates a builder for the {@link MultiSlotLayout}. Content inside of it can later be set
         * with {@link #addSlotContent}, {@link #setPrimaryChipContent}, {@link
         * #setPrimaryLabelTextContent} and {@link #setSecondaryLabelTextContent}.
         */
        public Builder(@NonNull DeviceParameters deviceParameters) {
            this.mDeviceParameters = deviceParameters;
        }

        /** Sets the primary compact chip which will be at the bottom. */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        // There is no direct matching getter for this setter as the serialized format of the
        // ProtoLayouts do not allow for a direct reconstruction of the arguments. Instead there are
        // methods to get the contents a whole for rendering.
        public Builder setPrimaryChipContent(@NonNull LayoutElement primaryChip) {
            this.mPrimaryChip = primaryChip;
            return this;
        }

        /** Sets the primary label text which will be above the slots. */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        // There is no direct matching getter for this setter as the serialized format of the
        // ProtoLayouts do not allow for a direct reconstruction of the arguments. Instead there are
        // methods to get the contents a whole for rendering.
        public Builder setPrimaryLabelTextContent(@NonNull LayoutElement primaryLabelText) {
            this.mPrimaryLabelText = primaryLabelText;
            return this;
        }

        /** Sets the secondary label text which will be below the slots. */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        // There is no direct matching getter for this setter as the serialized format of the
        // ProtoLayouts do not allow for a direct reconstruction of the arguments. Instead there are
        // methods to get the contents a whole for rendering.
        public Builder setSecondaryLabelTextContent(@NonNull LayoutElement secondaryLabelText) {
            this.mSecondaryLabelText = secondaryLabelText;
            return this;
        }

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

        /**
         * Sets the vertical spacer height which is used as a space between all slots and primary or
         * secondary label if there is any. If not set, {@link
         * LayoutDefaults#DEFAULT_VERTICAL_SPACER_HEIGHT} will be used.
         */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        // There is no direct matching getter for this setter as the serialized format of the
        // ProtoLayouts do not allow for a direct reconstruction of the arguments. Instead there are
        // methods to get the contents a whole for rendering.
        public Builder setVerticalSpacerHeight(@Dimension(unit = DP) float height) {
            this.mVerticalSpacerHeight = dp(height);
            return this;
        }

        @NonNull
        @Override
        // The @Dimension(unit = DP) on mVerticalSpacerHeight.getValue() is seemingly being ignored,
        // so lint complains that we're passing PX to something expecting DP. Just suppress the
        // warning for now.
        @SuppressLint("ResourceType")
        public MultiSlotLayout build() {
            PrimaryLayout.Builder layoutBuilder = new PrimaryLayout.Builder(mDeviceParameters);
            layoutBuilder.setVerticalSpacerHeight(mVerticalSpacerHeight.getValue());

            if (mPrimaryChip != null) {
                layoutBuilder.setPrimaryChipContent(mPrimaryChip);
            }

            if (mPrimaryLabelText != null) {
                layoutBuilder.setPrimaryLabelTextContent(mPrimaryLabelText);
            }

            if (mSecondaryLabelText != null) {
                layoutBuilder.setSecondaryLabelTextContent(mSecondaryLabelText);
            }

            if (!mSlotsContent.isEmpty()) {
                float horizontalPadding = layoutBuilder.getHorizontalPadding();
                DpProp rowWidth = dp(mDeviceParameters.getScreenWidthDp() - horizontalPadding * 2);
                Row.Builder rowBuilder =
                        new Row.Builder()
                                .setHeight(expand())
                                .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                                .setWidth(rowWidth);

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

                layoutBuilder.setContent(rowBuilder.build());
            }

            return new MultiSlotLayout(layoutBuilder.build());
        }
    }

    /** @hide */
    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return mElement.toLayoutElementProto();
    }
}
