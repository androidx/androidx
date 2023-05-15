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
import static androidx.wear.tiles.material.ChipDefaults.COMPACT_HEIGHT_TAPPABLE;
import static androidx.wear.tiles.material.Helper.checkNotNull;
import static androidx.wear.tiles.material.Helper.checkTag;
import static androidx.wear.tiles.material.Helper.getMetadataTagBytes;
import static androidx.wear.tiles.material.Helper.getTagBytes;
import static androidx.wear.tiles.material.Helper.isRoundDevice;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.DEFAULT_VERTICAL_SPACER_HEIGHT;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_CHIP_HORIZONTAL_PADDING_ROUND_DP;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_CHIP_HORIZONTAL_PADDING_SQUARE_DP;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_BOTTOM_ROUND_PERCENT;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_BOTTOM_SQUARE_PERCENT;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_HORIZONTAL_ROUND_PERCENT;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_HORIZONTAL_SQUARE_PERCENT;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_TOP_ROUND_PERCENT;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_TOP_SQUARE_PERCENT;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_PRIMARY_LABEL_SPACER_HEIGHT_ROUND_DP;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_PRIMARY_LABEL_SPACER_HEIGHT_SQUARE_DP;

import android.annotation.SuppressLint;

import androidx.annotation.Dimension;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.proto.LayoutElementProto;
import androidx.wear.tiles.material.CompactChip;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

/**
 * Tiles layout that represents a suggested layout style for Material Tiles with the primary
 * (compact) chip at the bottom with the given content in the center and the recommended margin and
 * padding applied. There is a fixed slot for an optional primary label above or optional secondary
 * label below the main content area.
 *
 * <p>It is highly recommended that main content has max lines between 2 and 4 (dependant on labels
 * present), i.e.: * No labels are present: content with max 4 lines, * 1 label is present: content
 * with max 3 lines, * 2 labels are present: content with max 2 lines.
 *
 * <p>For additional examples and suggested layouts see <a
 * href="/training/wearables/design/tiles-design-system">Tiles Design System</a>.
 *
 * <p>When accessing the contents of a container for testing, note that this element can't be simply
 * casted back to the original type, i.e.:
 *
 * <pre>{@code
 * PrimaryLayout pl = new PrimaryLayout...
 * Box box = new Box.Builder().addContent(pl).build();
 *
 * PrimaryLayout myPl = (PrimaryLayout) box.getContents().get(0);
 * }</pre>
 *
 * will fail.
 *
 * <p>To be able to get {@link PrimaryLayout} object from any layout element, {@link
 * #fromLayoutElement} method should be used, i.e.:
 *
 * <pre>{@code
 * PrimaryLayout myPl = PrimaryLayout.fromLayoutElement(box.getContents().get(0));
 * }</pre>
 *
 * @deprecated Use the new class {@link androidx.wear.protolayout.material.layouts.PrimaryLayout}
 *     which provides the same API and functionality.
 */
@Deprecated
@SuppressWarnings("deprecation")
public class PrimaryLayout implements androidx.wear.tiles.LayoutElementBuilders.LayoutElement {
    /**
     * Prefix tool tag for Metadata in androidx.wear.tiles.ModifiersBuilders.Modifiers, so we know
     * that androidx.wear.tiles.LayoutElementBuilders.Box is actually a PrimaryLayout.
     */
    static final String METADATA_TAG_PREFIX = "PL_";

    /** Index for byte array that contains bits to check whether the contents are present or not. */
    static final int FLAG_INDEX = METADATA_TAG_PREFIX.length();

    /**
     * Base tool tag for Metadata in androidx.wear.tiles.ModifiersBuilders.Modifiers, so we know
     * that androidx.wear.tiles.LayoutElementBuilders.Box is actually a PrimaryLayout and what
     * optional content is added.
     */
    static final byte[] METADATA_TAG_BASE =
            Arrays.copyOf(getTagBytes(METADATA_TAG_PREFIX), FLAG_INDEX + 1);

    /**
     * Bit position in a byte on {@link #FLAG_INDEX} index in metadata byte array to check whether
     * the primary chip is present or not.
     */
    static final int CHIP_PRESENT = 0x1;
    /**
     * Bit position in a byte on {@link #FLAG_INDEX} index in metadata byte array to check whether
     * the primary label is present or not.
     */
    static final int PRIMARY_LABEL_PRESENT = 0x2;
    /**
     * Bit position in a byte on {@link #FLAG_INDEX} index in metadata byte array to check whether
     * the secondary label is present or not.
     */
    static final int SECONDARY_LABEL_PRESENT = 0x4;
    /**
     * Bit position in a byte on {@link #FLAG_INDEX} index in metadata byte array to check whether
     * the content is present or not.
     */
    static final int CONTENT_PRESENT = 0x8;

    /** Position of the primary label in its own inner column if exists. */
    static final int PRIMARY_LABEL_POSITION = 1;
    /** Position of the content in its own inner column. */
    static final int CONTENT_ONLY_POSITION = 0;
    /** Position of the primary chip in main layout column. */
    static final int PRIMARY_CHIP_POSITION = 1;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            flag = true,
            value = {CHIP_PRESENT, PRIMARY_LABEL_PRESENT, SECONDARY_LABEL_PRESENT, CONTENT_PRESENT})
    @interface ContentBits {}

    @NonNull private final androidx.wear.tiles.LayoutElementBuilders.Box mImpl;

    // This contains inner columns and primary chip.
    @NonNull
    private final List<androidx.wear.tiles.LayoutElementBuilders.LayoutElement> mAllContent;
    // This contains optional labels, spacers and main content.
    @NonNull
    private final List<androidx.wear.tiles.LayoutElementBuilders.LayoutElement> mPrimaryLabel;
    // This contains optional labels, spacers and main content.
    @NonNull
    private final List<androidx.wear.tiles.LayoutElementBuilders.LayoutElement>
            mContentAndSecondaryLabel;

    PrimaryLayout(@NonNull androidx.wear.tiles.LayoutElementBuilders.Box layoutElement) {
        this.mImpl = layoutElement;
        this.mAllContent =
                ((androidx.wear.tiles.LayoutElementBuilders.Column)
                                layoutElement.getContents().get(0))
                        .getContents();
        List<androidx.wear.tiles.LayoutElementBuilders.LayoutElement> innerContent =
                ((androidx.wear.tiles.LayoutElementBuilders.Column) mAllContent.get(0))
                        .getContents();
        this.mPrimaryLabel =
                ((androidx.wear.tiles.LayoutElementBuilders.Column) innerContent.get(0))
                        .getContents();
        this.mContentAndSecondaryLabel =
                ((androidx.wear.tiles.LayoutElementBuilders.Column)
                                ((androidx.wear.tiles.LayoutElementBuilders.Box)
                                                innerContent.get(1))
                                        .getContents()
                                        .get(0))
                        .getContents();
    }

    /** Builder class for {@link PrimaryLayout}. */
    public static final class Builder
            implements androidx.wear.tiles.LayoutElementBuilders.LayoutElement.Builder {
        @NonNull
        private final androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
                mDeviceParameters;

        @Nullable
        private androidx.wear.tiles.LayoutElementBuilders.LayoutElement mPrimaryChip = null;

        @Nullable
        private androidx.wear.tiles.LayoutElementBuilders.LayoutElement mPrimaryLabelText = null;

        @Nullable
        private androidx.wear.tiles.LayoutElementBuilders.LayoutElement mSecondaryLabelText = null;

        @NonNull
        private androidx.wear.tiles.LayoutElementBuilders.LayoutElement mContent =
                new androidx.wear.tiles.LayoutElementBuilders.Box.Builder().build();

        @NonNull
        private androidx.wear.tiles.DimensionBuilders.DpProp mVerticalSpacerHeight =
                DEFAULT_VERTICAL_SPACER_HEIGHT;

        private byte mMetadataContentByte = 0;

        /**
         * Creates a builder for the {@link PrimaryLayout} from the given content. Content inside of
         * it can later be set with {@link #setContent}, {@link #setPrimaryChipContent}, {@link
         * #setPrimaryLabelTextContent} and {@link #setSecondaryLabelTextContent}.
         */
        public Builder(
                @NonNull
                        androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
                                deviceParameters) {
            this.mDeviceParameters = deviceParameters;
        }

        /**
         * Sets the element which is in the slot at the bottom of the layout. Note that it is
         * accepted to pass in any {@link androidx.wear.tiles.LayoutElementBuilders.LayoutElement},
         * but it is strongly recommended to add a {@link CompactChip} as the layout is optimized
         * for it.
         */
        @NonNull
        public Builder setPrimaryChipContent(
                @NonNull androidx.wear.tiles.LayoutElementBuilders.LayoutElement compactChip) {
            this.mPrimaryChip = compactChip;
            mMetadataContentByte = (byte) (mMetadataContentByte | CHIP_PRESENT);
            return this;
        }

        /** Sets the content in the primary label slot which will be above the main content. */
        @NonNull
        public Builder setPrimaryLabelTextContent(
                @NonNull androidx.wear.tiles.LayoutElementBuilders.LayoutElement primaryLabelText) {
            this.mPrimaryLabelText = primaryLabelText;
            mMetadataContentByte = (byte) (mMetadataContentByte | PRIMARY_LABEL_PRESENT);
            return this;
        }

        /**
         * Sets the content in the primary label slot which will be below the main content. It is
         * highly recommended to have primary label set when having secondary label.
         */
        @NonNull
        public Builder setSecondaryLabelTextContent(
                @NonNull
                        androidx.wear.tiles.LayoutElementBuilders.LayoutElement
                                secondaryLabelText) {
            this.mSecondaryLabelText = secondaryLabelText;
            mMetadataContentByte = (byte) (mMetadataContentByte | SECONDARY_LABEL_PRESENT);
            return this;
        }

        /**
         * Sets the additional content to this layout, above the primary chip.
         *
         * <p>The content slot will wrap the elements' height, so the height of the given content
         * must be fixed or set to wrap ({@code expand} can't be used).
         *
         * <p>This layout has built-in horizontal margins, so the given content should have width
         * set to {@code expand} to use all the available space, rather than an explicit width which
         * may lead to clipping.
         */
        @NonNull
        public Builder setContent(
                @NonNull androidx.wear.tiles.LayoutElementBuilders.LayoutElement content) {
            this.mContent = content;
            mMetadataContentByte = (byte) (mMetadataContentByte | CONTENT_PRESENT);
            return this;
        }

        /**
         * Sets the vertical spacer height which is used as a space between main content and
         * secondary label if there is any. If not set, {@link
         * LayoutDefaults#DEFAULT_VERTICAL_SPACER_HEIGHT} will be used.
         */
        @NonNull
        // The @Dimension(unit = DP) on dp() is seemingly being ignored, so lint complains that
        // we're passing PX to something expecting DP. Just suppress the warning for now.
        @SuppressLint("ResourceType")
        public Builder setVerticalSpacerHeight(@Dimension(unit = DP) float height) {
            this.mVerticalSpacerHeight = androidx.wear.tiles.DimensionBuilders.dp(height);
            return this;
        }

        /** Constructs and returns {@link PrimaryLayout} with the provided content and look. */
        // The @Dimension(unit = DP) on dp() is seemingly being ignored, so lint complains that
        // we're passing DP to something expecting PX. Just suppress the warning for now.
        @SuppressLint("ResourceType")
        @NonNull
        @Override
        public PrimaryLayout build() {
            float topPadding = getTopPadding();
            float bottomPadding = getBottomPadding();
            float horizontalPadding = getHorizontalPadding();
            float horizontalChipPadding = getChipHorizontalPadding();

            float primaryChipHeight = mPrimaryChip != null ? COMPACT_HEIGHT_TAPPABLE.getValue() : 0;

            androidx.wear.tiles.DimensionBuilders.DpProp mainContentHeight =
                    androidx.wear.tiles.DimensionBuilders.dp(
                            mDeviceParameters.getScreenHeightDp()
                                    - primaryChipHeight
                                    - bottomPadding
                                    - topPadding);

            // Layout organization: column(column(primary label + spacer + (box(column(content +
            // secondary label))) + chip)

            // First column that has all other content and chip.
            androidx.wear.tiles.LayoutElementBuilders.Column.Builder layoutBuilder =
                    new androidx.wear.tiles.LayoutElementBuilders.Column.Builder();

            // Contains primary label, main content and secondary label. Primary label will be
            // wrapped, while other content will be expanded so it can be centered in the remaining
            // space.
            androidx.wear.tiles.LayoutElementBuilders.Column.Builder contentAreaBuilder =
                    new androidx.wear.tiles.LayoutElementBuilders.Column.Builder()
                            .setWidth(androidx.wear.tiles.DimensionBuilders.expand())
                            .setHeight(mainContentHeight)
                            .setHorizontalAlignment(
                                    androidx.wear.tiles.LayoutElementBuilders
                                            .HORIZONTAL_ALIGN_CENTER);

            // Contains main content and secondary label with wrapped height so it can be put inside
            // of the androidx.wear.tiles.LayoutElementBuilders.Box to be centered.
            androidx.wear.tiles.LayoutElementBuilders.Column.Builder contentSecondaryLabelBuilder =
                    new androidx.wear.tiles.LayoutElementBuilders.Column.Builder()
                            .setWidth(androidx.wear.tiles.DimensionBuilders.expand())
                            .setHeight(androidx.wear.tiles.DimensionBuilders.wrap())
                            .setHorizontalAlignment(
                                    androidx.wear.tiles.LayoutElementBuilders
                                            .HORIZONTAL_ALIGN_CENTER);

            // Needs to be in column because of the spacers.
            androidx.wear.tiles.LayoutElementBuilders.Column.Builder primaryLabelBuilder =
                    new androidx.wear.tiles.LayoutElementBuilders.Column.Builder()
                            .setWidth(androidx.wear.tiles.DimensionBuilders.expand())
                            .setHeight(androidx.wear.tiles.DimensionBuilders.wrap());

            if (mPrimaryLabelText != null) {
                primaryLabelBuilder.addContent(
                        new androidx.wear.tiles.LayoutElementBuilders.Spacer.Builder()
                                .setHeight(getPrimaryLabelTopSpacerHeight())
                                .build());
                primaryLabelBuilder.addContent(mPrimaryLabelText);
            }

            contentAreaBuilder.addContent(primaryLabelBuilder.build());

            contentSecondaryLabelBuilder.addContent(
                    new androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
                            .setVerticalAlignment(
                                    androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                            .setWidth(androidx.wear.tiles.DimensionBuilders.expand())
                            .setHeight(androidx.wear.tiles.DimensionBuilders.wrap())
                            .addContent(mContent)
                            .build());

            if (mSecondaryLabelText != null) {
                contentSecondaryLabelBuilder.addContent(
                        new androidx.wear.tiles.LayoutElementBuilders.Spacer.Builder()
                                .setHeight(mVerticalSpacerHeight)
                                .build());
                contentSecondaryLabelBuilder.addContent(mSecondaryLabelText);
            }

            contentAreaBuilder.addContent(
                    new androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
                            .setVerticalAlignment(
                                    androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                            .setWidth(androidx.wear.tiles.DimensionBuilders.expand())
                            .setHeight(androidx.wear.tiles.DimensionBuilders.expand())
                            .addContent(contentSecondaryLabelBuilder.build())
                            .build());

            layoutBuilder
                    .setModifiers(
                            new androidx.wear.tiles.ModifiersBuilders.Modifiers.Builder()
                                    .setPadding(
                                            new androidx.wear.tiles.ModifiersBuilders.Padding
                                                            .Builder()
                                                    .setStart(
                                                            androidx.wear.tiles.DimensionBuilders
                                                                    .dp(horizontalPadding))
                                                    .setEnd(
                                                            androidx.wear.tiles.DimensionBuilders
                                                                    .dp(horizontalPadding))
                                                    .setTop(
                                                            androidx.wear.tiles.DimensionBuilders
                                                                    .dp(topPadding))
                                                    .setBottom(
                                                            androidx.wear.tiles.DimensionBuilders
                                                                    .dp(bottomPadding))
                                                    .build())
                                    .build())
                    .setWidth(androidx.wear.tiles.DimensionBuilders.expand())
                    .setHeight(androidx.wear.tiles.DimensionBuilders.expand())
                    .setHorizontalAlignment(
                            androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER);

            layoutBuilder.addContent(contentAreaBuilder.build());

            if (mPrimaryChip != null) {
                layoutBuilder.addContent(
                        new androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
                                .setVerticalAlignment(
                                        androidx.wear.tiles.LayoutElementBuilders
                                                .VERTICAL_ALIGN_BOTTOM)
                                .setWidth(androidx.wear.tiles.DimensionBuilders.expand())
                                .setHeight(androidx.wear.tiles.DimensionBuilders.wrap())
                                .setModifiers(
                                    new androidx.wear.tiles.ModifiersBuilders.Modifiers.Builder()
                                        .setPadding(
                                            new androidx.wear.tiles.ModifiersBuilders.Padding
                                                .Builder()
                                                .setStart(
                                                    androidx.wear.tiles.DimensionBuilders.dp(
                                                        horizontalChipPadding))
                                                .setEnd(
                                                    androidx.wear.tiles.DimensionBuilders.dp(
                                                        horizontalChipPadding))
                                                .build())
                                        .build())
                                .addContent(mPrimaryChip)
                                .build());
            }

            byte[] metadata = METADATA_TAG_BASE.clone();
            metadata[FLAG_INDEX] = mMetadataContentByte;

            androidx.wear.tiles.LayoutElementBuilders.Box.Builder element =
                    new androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
                            .setWidth(androidx.wear.tiles.DimensionBuilders.expand())
                            .setHeight(androidx.wear.tiles.DimensionBuilders.expand())
                            .setModifiers(
                                    new androidx.wear.tiles.ModifiersBuilders.Modifiers.Builder()
                                            .setMetadata(
                                                    new androidx.wear.tiles.ModifiersBuilders
                                                                    .ElementMetadata.Builder()
                                                            .setTagData(metadata)
                                                            .build())
                                            .build())
                            .setVerticalAlignment(
                                    androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM)
                            .addContent(layoutBuilder.build());

            return new PrimaryLayout(element.build());
        }

        /**
         * Returns the recommended bottom padding, based on percentage values in {@link
         * LayoutDefaults}.
         */
        private float getBottomPadding() {
            return mPrimaryChip != null
                    ? (mDeviceParameters.getScreenHeightDp()
                            * (isRoundDevice(mDeviceParameters)
                                    ? PRIMARY_LAYOUT_MARGIN_BOTTOM_ROUND_PERCENT
                                    : PRIMARY_LAYOUT_MARGIN_BOTTOM_SQUARE_PERCENT))
                    : getTopPadding();
        }

        /**
         * Returns the recommended top padding, based on percentage values in {@link
         * LayoutDefaults}.
         */
        @Dimension(unit = DP)
        private float getTopPadding() {
            return mDeviceParameters.getScreenHeightDp()
                    * (isRoundDevice(mDeviceParameters)
                            ? PRIMARY_LAYOUT_MARGIN_TOP_ROUND_PERCENT
                            : PRIMARY_LAYOUT_MARGIN_TOP_SQUARE_PERCENT);
        }

        /**
         * Returns the recommended horizontal padding, based on percentage values in {@link
         * LayoutDefaults}.
         */
        @Dimension(unit = DP)
        private float getHorizontalPadding() {
            return mDeviceParameters.getScreenWidthDp()
                    * (isRoundDevice(mDeviceParameters)
                            ? PRIMARY_LAYOUT_MARGIN_HORIZONTAL_ROUND_PERCENT
                            : PRIMARY_LAYOUT_MARGIN_HORIZONTAL_SQUARE_PERCENT);
        }

        /**
         * Returns the recommended horizontal padding for primary chip, based on percentage values
         * and DP values in {@link LayoutDefaults}.
         */
        @Dimension(unit = DP)
        private float getChipHorizontalPadding() {
            return isRoundDevice(mDeviceParameters)
                    ? PRIMARY_LAYOUT_CHIP_HORIZONTAL_PADDING_ROUND_DP
                    : PRIMARY_LAYOUT_CHIP_HORIZONTAL_PADDING_SQUARE_DP;
        }

        /** Returns the spacer height to be placed above primary label to accommodate Tile icon. */
        @NonNull
        private androidx.wear.tiles.DimensionBuilders.DpProp getPrimaryLabelTopSpacerHeight() {
            return isRoundDevice(mDeviceParameters)
                    ? PRIMARY_LAYOUT_PRIMARY_LABEL_SPACER_HEIGHT_ROUND_DP
                    : PRIMARY_LAYOUT_PRIMARY_LABEL_SPACER_HEIGHT_SQUARE_DP;
        }
    }

    /** Get the primary label content from this layout. */
    @Nullable
    public androidx.wear.tiles.LayoutElementBuilders.LayoutElement getPrimaryLabelTextContent() {
        if (!areElementsPresent(PRIMARY_LABEL_PRESENT)) {
            return null;
        }
        return mPrimaryLabel.get(PRIMARY_LABEL_POSITION);
    }

    /** Get the secondary label content from this layout. */
    @Nullable
    public androidx.wear.tiles.LayoutElementBuilders.LayoutElement getSecondaryLabelTextContent() {
        if (!areElementsPresent(SECONDARY_LABEL_PRESENT)) {
            return null;
        }
        // By tag we know that secondary label exists. It will always be at last position.
        return mContentAndSecondaryLabel.get(mContentAndSecondaryLabel.size() - 1);
    }

    /** Get the inner content from this layout. */
    @Nullable
    public androidx.wear.tiles.LayoutElementBuilders.LayoutElement getContent() {
        if (!areElementsPresent(CONTENT_PRESENT)) {
            return null;
        }
        return ((androidx.wear.tiles.LayoutElementBuilders.Box)
                        mContentAndSecondaryLabel.get(CONTENT_ONLY_POSITION))
                .getContents()
                .get(0);
    }

    /** Get the primary chip content from this layout. */
    @Nullable
    public androidx.wear.tiles.LayoutElementBuilders.LayoutElement getPrimaryChipContent() {
        if (areElementsPresent(CHIP_PRESENT)) {
            return ((androidx.wear.tiles.LayoutElementBuilders.Box)
                            mAllContent.get(PRIMARY_CHIP_POSITION))
                    .getContents()
                    .get(0);
        }
        return null;
    }

    /** Get the vertical spacer height from this layout. */
    // The @Dimension(unit = DP) on getValue() is seemingly being ignored, so lint complains that
    // we're passing PX to something expecting DP. Just suppress the warning for now.
    @SuppressLint("ResourceType")
    @Dimension(unit = DP)
    public float getVerticalSpacerHeight() {
        if (areElementsPresent(SECONDARY_LABEL_PRESENT)) {
            androidx.wear.tiles.LayoutElementBuilders.LayoutElement element =
                    mContentAndSecondaryLabel.get(CONTENT_ONLY_POSITION + 1);
            if (element instanceof androidx.wear.tiles.LayoutElementBuilders.Spacer) {
                androidx.wear.tiles.DimensionBuilders.SpacerDimension height =
                        ((androidx.wear.tiles.LayoutElementBuilders.Spacer) element).getHeight();
                if (height instanceof androidx.wear.tiles.DimensionBuilders.DpProp) {
                    return ((androidx.wear.tiles.DimensionBuilders.DpProp) height).getValue();
                }
            }
        }
        return DEFAULT_VERTICAL_SPACER_HEIGHT.getValue();
    }

    private boolean areElementsPresent(@ContentBits int elementFlag) {
        return (getMetadataTag()[FLAG_INDEX] & elementFlag) == elementFlag;
    }

    /** Returns metadata tag set to this PrimaryLayout. */
    @NonNull
    byte[] getMetadataTag() {
        return getMetadataTagBytes(checkNotNull(checkNotNull(mImpl.getModifiers()).getMetadata()));
    }

    /**
     * Returns PrimaryLayout object from the given
     * androidx.wear.tiles.LayoutElementBuilders.LayoutElement (e.g. one retrieved from a
     * container's content with {@code container.getContents().get(index)}) if that element can be
     * converted to PrimaryLayout. Otherwise, it will return null.
     */
    @Nullable
    public static PrimaryLayout fromLayoutElement(
            @NonNull androidx.wear.tiles.LayoutElementBuilders.LayoutElement element) {
        if (element instanceof PrimaryLayout) {
            return (PrimaryLayout) element;
        }
        if (!(element instanceof androidx.wear.tiles.LayoutElementBuilders.Box)) {
            return null;
        }
        androidx.wear.tiles.LayoutElementBuilders.Box boxElement =
                (androidx.wear.tiles.LayoutElementBuilders.Box) element;
        if (!checkTag(boxElement.getModifiers(), METADATA_TAG_PREFIX, METADATA_TAG_BASE)) {
            return null;
        }
        // Now we are sure that this element is a PrimaryLayout.
        return new PrimaryLayout(boxElement);
    }

    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return mImpl.toLayoutElementProto();
    }
}
