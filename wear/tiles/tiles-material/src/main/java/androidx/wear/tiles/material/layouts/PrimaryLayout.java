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

import android.annotation.SuppressLint;

import androidx.annotation.Dimension;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.tiles.DimensionBuilders.DpProp;
import androidx.wear.tiles.DimensionBuilders.SpacerDimension;
import androidx.wear.tiles.LayoutElementBuilders;
import androidx.wear.tiles.LayoutElementBuilders.Box;
import androidx.wear.tiles.LayoutElementBuilders.Column;
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement;
import androidx.wear.tiles.LayoutElementBuilders.Spacer;
import androidx.wear.tiles.ModifiersBuilders.ElementMetadata;
import androidx.wear.tiles.ModifiersBuilders.Modifiers;
import androidx.wear.tiles.ModifiersBuilders.Padding;
import androidx.wear.tiles.material.CompactChip;
import androidx.wear.tiles.proto.LayoutElementProto;

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
 */
public class PrimaryLayout implements LayoutElement {
    /**
     * Prefix tool tag for Metadata in Modifiers, so we know that Box is actually a PrimaryLayout.
     */
    static final String METADATA_TAG_PREFIX = "PL_";

    /** Index for byte array that contains bits to check whether the contents are present or not. */
    static final int FLAG_INDEX = METADATA_TAG_PREFIX.length();

    /**
     * Base tool tag for Metadata in Modifiers, so we know that Box is actually a PrimaryLayout and
     * what optional content is added.
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

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            flag = true,
            value = {CHIP_PRESENT, PRIMARY_LABEL_PRESENT, SECONDARY_LABEL_PRESENT, CONTENT_PRESENT})
    @interface ContentBits {}

    @NonNull private final Box mImpl;

    // This contains inner columns and primary chip.
    @NonNull private final List<LayoutElement> mAllContent;

    // This contains optional labels, spacers and main content.
    @NonNull private final List<LayoutElement> mInnerColumn;

    PrimaryLayout(@NonNull Box layoutElement) {
        this.mImpl = layoutElement;
        this.mAllContent = ((Column) layoutElement.getContents().get(0)).getContents();
        this.mInnerColumn = ((Column) mAllContent.get(0)).getContents();
    }

    /** Builder class for {@link PrimaryLayout}. */
    public static final class Builder implements LayoutElement.Builder {
        @NonNull private final DeviceParameters mDeviceParameters;
        @Nullable private LayoutElement mPrimaryChip = null;
        @Nullable private LayoutElement mPrimaryLabelText = null;
        @Nullable private LayoutElement mSecondaryLabelText = null;
        @NonNull private LayoutElement mContent = new Box.Builder().build();
        @NonNull private DpProp mVerticalSpacerHeight = DEFAULT_VERTICAL_SPACER_HEIGHT;
        private byte mMetadataContentByte = 0;

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
        public Builder setPrimaryChipContent(@NonNull LayoutElement compactChip) {
            this.mPrimaryChip = compactChip;
            mMetadataContentByte = (byte) (mMetadataContentByte | CHIP_PRESENT);
            return this;
        }

        /** Sets the content in the primary label slot which will be above the main content. */
        @NonNull
        public Builder setPrimaryLabelTextContent(@NonNull LayoutElement primaryLabelText) {
            this.mPrimaryLabelText = primaryLabelText;
            mMetadataContentByte = (byte) (mMetadataContentByte | PRIMARY_LABEL_PRESENT);
            return this;
        }

        /**
         * Sets the content in the primary label slot which will be below the main content. It is
         * highly recommended to have primary label set when having secondary label.
         */
        @NonNull
        public Builder setSecondaryLabelTextContent(@NonNull LayoutElement secondaryLabelText) {
            this.mSecondaryLabelText = secondaryLabelText;
            mMetadataContentByte = (byte) (mMetadataContentByte | SECONDARY_LABEL_PRESENT);
            return this;
        }

        /** Sets the additional content to this layout, above the primary chip. */
        @NonNull
        public Builder setContent(@NonNull LayoutElement content) {
            this.mContent = content;
            mMetadataContentByte = (byte) (mMetadataContentByte | CONTENT_PRESENT);
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
        public Builder setVerticalSpacerHeight(@Dimension(unit = DP) float height) {
            this.mVerticalSpacerHeight = dp(height);
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

            DpProp mainContentHeight =
                    dp(
                            mDeviceParameters.getScreenHeightDp()
                                    - primaryChipHeight
                                    - bottomPadding
                                    - topPadding);

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
                            .setModifiers(
                                    new Modifiers.Builder()
                                            .setPadding(
                                                    new Padding.Builder()
                                                            .setStart(dp(horizontalPadding))
                                                            .setEnd(dp(horizontalPadding))
                                                            .setTop(dp(topPadding))
                                                            .setBottom(dp(bottomPadding))
                                                            .build())
                                            .build())
                            .setWidth(expand())
                            .setHeight(expand())
                            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER);

            layoutBuilder.addContent(innerContentBuilder.build());

            if (mPrimaryChip != null) {
                layoutBuilder.addContent(
                        new Box.Builder()
                                .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM)
                                .setWidth(expand())
                                .setHeight(wrap())
                                .setModifiers(
                                        new Modifiers.Builder()
                                                .setPadding(
                                                        new Padding.Builder()
                                                                .setStart(dp(horizontalChipPadding))
                                                                .setEnd(dp(horizontalChipPadding))
                                                                .build())
                                                .build())
                                .addContent(mPrimaryChip)
                                .build());
            }

            byte[] metadata = METADATA_TAG_BASE.clone();
            metadata[FLAG_INDEX] = mMetadataContentByte;

            Box.Builder element =
                    new Box.Builder()
                            .setWidth(expand())
                            .setHeight(expand())
                            .setModifiers(
                                    new Modifiers.Builder()
                                            .setMetadata(
                                                    new ElementMetadata.Builder()
                                                            .setTagData(metadata)
                                                            .build())
                                            .build())
                            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM)
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
    }

    /** Get the primary label content from this layout. */
    @Nullable
    public LayoutElement getPrimaryLabelTextContent() {
        if (!areElementsPresent(PRIMARY_LABEL_PRESENT)) {
            return null;
        }
        // By tag we know that primary label exists. It will always be at position 0.
        return mInnerColumn.get(0);
    }

    /** Get the secondary label content from this layout. */
    @Nullable
    public LayoutElement getSecondaryLabelTextContent() {
        if (!areElementsPresent(SECONDARY_LABEL_PRESENT)) {
            return null;
        }
        // By tag we know that secondary label exists. It will always be at last position.
        return mInnerColumn.get(mInnerColumn.size() - 1);
    }

    /** Get the inner content from this layout. */
    @Nullable
    public LayoutElement getContent() {
        if (!areElementsPresent(CONTENT_PRESENT)) {
            return null;
        }
        // By tag we know that content exists. It will be at position 0 if there is no primary
        // label, or at position 2 (primary label, spacer - content) otherwise.
        int contentPosition = areElementsPresent(PRIMARY_LABEL_PRESENT) ? 2 : 0;
        return ((Box) mInnerColumn.get(contentPosition)).getContents().get(0);
    }

    /** Get the primary chip content from this layout. */
    @Nullable
    public LayoutElement getPrimaryChipContent() {
        if (areElementsPresent(CHIP_PRESENT)) {
            return ((Box) mAllContent.get(1)).getContents().get(0);
        }
        return null;
    }

    /** Get the vertical spacer height from this layout. */
    // The @Dimension(unit = DP) on getValue() is seemingly being ignored, so lint complains that
    // we're passing PX to something expecting DP. Just suppress the warning for now.
    @SuppressLint("ResourceType")
    @Dimension(unit = DP)
    public float getVerticalSpacerHeight() {
        // We don't need special cases for primary or secondary label - if primary label is present,
        // then the first spacer is at the position 1 and we can get height from it. However, if the
        // primary label is not present, the spacer will be between content and secondary label (if
        // there is secondary label) so its position is again 1.
        if (areElementsPresent(PRIMARY_LABEL_PRESENT)
                || areElementsPresent(SECONDARY_LABEL_PRESENT)) {
            LayoutElement element = mInnerColumn.get(1);
            if (element instanceof Spacer) {
                SpacerDimension height = ((Spacer) element).getHeight();
                if (height instanceof DpProp) {
                    return ((DpProp) height).getValue();
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
     * Returns PrimaryLayout object from the given LayoutElement (e.g. one retrieved from a
     * container's content with {@code container.getContents().get(index)}) if that element can be
     * converted to PrimaryLayout. Otherwise, it will return null.
     */
    @Nullable
    public static PrimaryLayout fromLayoutElement(@NonNull LayoutElement element) {
        if (element instanceof PrimaryLayout) {
            return (PrimaryLayout) element;
        }
        if (!(element instanceof Box)) {
            return null;
        }
        Box boxElement = (Box) element;
        if (!checkTag(boxElement.getModifiers(), METADATA_TAG_PREFIX, METADATA_TAG_BASE)) {
            return null;
        }
        // Now we are sure that this element is a PrimaryLayout.
        return new PrimaryLayout(boxElement);
    }

    /** @hide */
    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return mImpl.toLayoutElementProto();
    }
}
