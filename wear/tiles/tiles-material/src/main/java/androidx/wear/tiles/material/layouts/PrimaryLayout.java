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
import static androidx.wear.tiles.DimensionBuilders.wrap;
import static androidx.wear.tiles.material.ChipDefaults.COMPACT_HEIGHT;
import static androidx.wear.tiles.material.Helper.isRoundDevice;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.DEFAULT_VERTICAL_SPACER_HEIGHT;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_BOTTOM_ROUND_PERCENT;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_BOTTOM_SQUARE_PERCENT;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_HORIZONTAL_ROUND_PERCENT;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_HORIZONTAL_SQUARE_PERCENT;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_TOP_ROUND_PERCENT;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_TOP_SQUARE_PERCENT;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_SPACER_HEIGHT;

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
import androidx.wear.tiles.LayoutElementBuilders.Column;
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement;
import androidx.wear.tiles.LayoutElementBuilders.Spacer;
import androidx.wear.tiles.ModifiersBuilders.Modifiers;
import androidx.wear.tiles.ModifiersBuilders.Padding;
import androidx.wear.tiles.material.CompactChip;
import androidx.wear.tiles.proto.LayoutElementProto;

/**
 * Tiles layout that represents a suggested layout style for Material Tiles with the primary
 * (compact) chip at the bottom with the given content in the center and the recommended margin and
 * padding applied.
 */
// TODO(b/215323986): Link visuals.
public class PrimaryLayout implements LayoutElement {
    @NonNull private final LayoutElement mElement;

    PrimaryLayout(@NonNull LayoutElement layoutElement) {
        this.mElement = layoutElement;
    }

    /** Builder class for {@link PrimaryLayout}. */
    public static final class Builder implements LayoutElement.Builder {

        @NonNull private final DeviceParameters mDeviceParameters;
        @Nullable private LayoutElement mPrimaryChip = null;
        @Nullable private LayoutElement mPrimaryLabelText = null;
        @Nullable private LayoutElement mSecondaryLabelText = null;
        @NonNull private LayoutElement mContent = new Box.Builder().build();
        @NonNull private DpProp mVerticalSpacerHeight = DEFAULT_VERTICAL_SPACER_HEIGHT;

        /**
         * Creates a builder for the {@link PrimaryLayout} from the given content. Content inside of
         * it can later be set with {@link #setContent}, {@link #setPrimaryChipContent}, {@link
         * #setPrimaryLabelTextContent} and {@link #setSecondaryLabelTextContent}.
         */
        public Builder(@NonNull DeviceParameters deviceParameters) {
            this.mDeviceParameters = deviceParameters;
        }

        /**
         * Sets the element which is in the slot at the bottom of the layout. Note that it is
         * accepted to pass in any {@link LayoutElement}, but it is strongly recommended to add a
         * {@link CompactChip} as the layout is optimized for it.
         */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        // There is no direct matching getter for this setter as the serialized format of the
        // ProtoLayouts do not allow for a direct reconstruction of the arguments. Instead there are
        // methods to get the contents a whole for rendering.
        public Builder setPrimaryChipContent(@NonNull LayoutElement compactChip) {
            this.mPrimaryChip = compactChip;
            return this;
        }

        /** Sets the content in the primary label slot which will be above the main content. */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        // There is no direct matching getter for this setter as the serialized format of the
        // ProtoLayouts do not allow for a direct reconstruction of the arguments. Instead there are
        // methods to get the contents a whole for rendering.
        public Builder setPrimaryLabelTextContent(@NonNull LayoutElement primaryLabelText) {
            this.mPrimaryLabelText = primaryLabelText;
            return this;
        }

        /**
         * Sets the content in the primary label slot which will be below the main content. It is
         * highly recommended to have primary label set when having secondary label.
         */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        // There is no direct matching getter for this setter as the serialized format of the
        // ProtoLayouts do not allow for a direct reconstruction of the arguments. Instead there are
        // methods to get the contents a whole for rendering.
        public Builder setSecondaryLabelTextContent(@NonNull LayoutElement secondaryLabelText) {
            this.mSecondaryLabelText = secondaryLabelText;
            return this;
        }

        /** Sets the additional content to this layout, above the primary chip. */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        // TODO(b/221427609): Add getter - there is a problem when serializing this to the
        // ProtoLayouts as we can't easily reconstruct the position of the actual content vs
        // additional labels.
        public Builder setContent(@NonNull LayoutElement content) {
            this.mContent = content;
            return this;
        }

        /**
         * Sets the vertical spacer height which is used as a space between main content and primary
         * or secondary label if there is any. If not set, {@link
         * LayoutDefaults#DEFAULT_VERTICAL_SPACER_HEIGHT} will be used.
         */
        @NonNull
        // The @Dimension(unit = DP) on dp() is seemingly being ignored, so lint complains that
        // we're passing PX to something expecting DP. Just suppress the warning for now.
        @SuppressLint("ResourceType")
        @SuppressWarnings("MissingGetterMatchingBuilder")
        // There is no direct matching getter for this setter as the serialized format of the
        // ProtoLayouts do not allow for a direct reconstruction of the arguments. Instead there are
        // methods to get the contents a whole for rendering.
        public Builder setVerticalSpacerHeight(@Dimension(unit = DP) float height) {
            this.mVerticalSpacerHeight = dp(height);
            return this;
        }

        /** Constructs and returns {@link PrimaryLayout} with the provided content and look. */
        @NonNull
        @Override
        public PrimaryLayout build() {
            float topPadding =
                    mDeviceParameters.getScreenHeightDp()
                            * (isRoundDevice(mDeviceParameters)
                                    ? PRIMARY_LAYOUT_MARGIN_TOP_ROUND_PERCENT
                                    : PRIMARY_LAYOUT_MARGIN_TOP_SQUARE_PERCENT);
            float bottomPadding =
                    mPrimaryChip != null
                            ? (mDeviceParameters.getScreenHeightDp()
                                    * (isRoundDevice(mDeviceParameters)
                                            ? PRIMARY_LAYOUT_MARGIN_BOTTOM_ROUND_PERCENT
                                            : PRIMARY_LAYOUT_MARGIN_BOTTOM_SQUARE_PERCENT))
                            : topPadding;
            float horizontalPadding = getHorizontalPadding();

            float primaryChipHeight =
                    mPrimaryChip != null
                            ? (COMPACT_HEIGHT.getValue() + PRIMARY_LAYOUT_SPACER_HEIGHT.getValue())
                            : 0;

            DpProp mainContentHeight =
                    dp(
                            mDeviceParameters.getScreenHeightDp()
                                    - primaryChipHeight
                                    - bottomPadding
                                    - topPadding);

            Modifiers modifiers =
                    new Modifiers.Builder()
                            .setPadding(
                                    new Padding.Builder()
                                            .setTop(dp(topPadding))
                                            .setBottom(dp(bottomPadding))
                                            .setStart(dp(horizontalPadding))
                                            .setEnd(dp(horizontalPadding))
                                            .build())
                            .build();

            Column.Builder innerContentBuilder =
                    new Column.Builder()
                            .setWidth(expand())
                            .setHeight(mainContentHeight)
                            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER);

            if (mPrimaryLabelText != null) {
                innerContentBuilder.addContent(mPrimaryLabelText);
                innerContentBuilder.addContent(
                        new Spacer.Builder().setHeight(mVerticalSpacerHeight).build());
            }

            innerContentBuilder.addContent(
                    new Box.Builder()
                            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                            .setHeight(expand())
                            .setWidth(expand())
                            .addContent(mContent)
                            .build());

            if (mSecondaryLabelText != null) {
                innerContentBuilder.addContent(
                        new Spacer.Builder().setHeight(mVerticalSpacerHeight).build());
                innerContentBuilder.addContent(mSecondaryLabelText);
            }

            Column.Builder layoutBuilder =
                    new Column.Builder()
                            .setModifiers(modifiers)
                            .setWidth(expand())
                            .setHeight(expand())
                            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER);

            layoutBuilder.addContent(innerContentBuilder.build());

            if (mPrimaryChip != null) {
                layoutBuilder
                        .addContent(
                                new Spacer.Builder()
                                        .setHeight(PRIMARY_LAYOUT_SPACER_HEIGHT)
                                        .build())
                        .addContent(
                                new Box.Builder()
                                        .setVerticalAlignment(
                                                LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM)
                                        .setHeight(wrap())
                                        .addContent(mPrimaryChip)
                                        .build());
            }

            LayoutElement.Builder element =
                    new Box.Builder()
                            .setWidth(expand())
                            .setHeight(expand())
                            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM)
                            .addContent(layoutBuilder.build());

            return new PrimaryLayout(element.build());
        }

        /**
         * Returns the recommended horizontal padding, based on percentage values in {@link
         * LayoutDefaults}.
         */
        float getHorizontalPadding() {
            return mDeviceParameters.getScreenWidthDp()
                    * (isRoundDevice(mDeviceParameters)
                            ? PRIMARY_LAYOUT_MARGIN_HORIZONTAL_ROUND_PERCENT
                            : PRIMARY_LAYOUT_MARGIN_HORIZONTAL_SQUARE_PERCENT);
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
