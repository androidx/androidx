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

package androidx.wear.protolayout.material.layouts;

import static androidx.annotation.Dimension.DP;
import static androidx.wear.protolayout.DimensionBuilders.dp;
import static androidx.wear.protolayout.DimensionBuilders.expand;
import static androidx.wear.protolayout.DimensionBuilders.wrap;
import static androidx.wear.protolayout.material.ChipDefaults.MIN_TAPPABLE_SQUARE_LENGTH;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.DEFAULT_VERTICAL_SPACER_HEIGHT;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_CHIP_HORIZONTAL_PADDING_ROUND_DP;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_CHIP_HORIZONTAL_PADDING_SQUARE_DP;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_BOTTOM_ROUND_PERCENT;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_BOTTOM_SQUARE_PERCENT;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_HORIZONTAL_ROUND_PERCENT;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_HORIZONTAL_SQUARE_PERCENT;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_TOP_ROUND_PERCENT;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_TOP_SQUARE_PERCENT;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_PRIMARY_LABEL_SPACER_HEIGHT_ROUND_DP;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_PRIMARY_LABEL_SPACER_HEIGHT_SQUARE_DP;
import static androidx.wear.protolayout.materialcore.Helper.checkNotNull;
import static androidx.wear.protolayout.materialcore.Helper.checkTag;
import static androidx.wear.protolayout.materialcore.Helper.getMetadataTagBytes;
import static androidx.wear.protolayout.materialcore.Helper.getTagBytes;
import static androidx.wear.protolayout.materialcore.Helper.isRoundDevice;

import android.annotation.SuppressLint;

import androidx.annotation.Dimension;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.protolayout.DimensionBuilders.DpProp;
import androidx.wear.protolayout.DimensionBuilders.SpacerDimension;
import androidx.wear.protolayout.LayoutElementBuilders;
import androidx.wear.protolayout.LayoutElementBuilders.Box;
import androidx.wear.protolayout.LayoutElementBuilders.Column;
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement;
import androidx.wear.protolayout.LayoutElementBuilders.Spacer;
import androidx.wear.protolayout.ModifiersBuilders.ElementMetadata;
import androidx.wear.protolayout.ModifiersBuilders.Modifiers;
import androidx.wear.protolayout.ModifiersBuilders.Padding;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.material.CompactChip;
import androidx.wear.protolayout.proto.LayoutElementProto;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

/**
 * ProtoLayout layout that represents a suggested layout style for Material ProtoLayout with the
 * primary (compact) chip at the bottom with the given content in the center and the recommended
 * margin and padding applied. There is a fixed slot for an optional primary label above or optional
 * secondary label below the main content area.
 *
 * <p>It is highly recommended that main content has max lines between 2 and 4 (dependant on labels
 * present), i.e.: * No labels are present: content with max 4 lines, * 1 label is present: content
 * with max 3 lines, * 2 labels are present: content with max 2 lines.
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
// TODO(b/274916652): Link visuals once they are available.
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

    /** Position of the primary label in its own inner column if exists. */
    static final int PRIMARY_LABEL_POSITION = 1;
    /** Position of the content in its own inner column. */
    static final int CONTENT_ONLY_POSITION = 0;
    /** Position of the primary chip in main layout column. */
    static final int PRIMARY_CHIP_POSITION = 1;

    @RestrictTo(Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            flag = true,
            value = {CHIP_PRESENT, PRIMARY_LABEL_PRESENT, SECONDARY_LABEL_PRESENT, CONTENT_PRESENT})
    @interface ContentBits {}

    @NonNull private final Box mImpl;

    // This contains inner columns and primary chip.
    @NonNull private final List<LayoutElement> mAllContent;
    // This contains optional labels, spacers and main content.
    @NonNull private final List<LayoutElement> mPrimaryLabel;
    // This contains optional labels, spacers and main content.
    @NonNull private final List<LayoutElement> mContentAndSecondaryLabel;

    PrimaryLayout(@NonNull Box layoutElement) {
        this.mImpl = layoutElement;
        this.mAllContent = ((Column) layoutElement.getContents().get(0)).getContents();
        List<LayoutElement> innerContent = ((Column) mAllContent.get(0)).getContents();
        this.mPrimaryLabel = ((Column) innerContent.get(0)).getContents();
        this.mContentAndSecondaryLabel =
                ((Column) ((Box) innerContent.get(1)).getContents().get(0)).getContents();
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
        public Builder setContent(@NonNull LayoutElement content) {
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

            float primaryChipHeight =
                    mPrimaryChip != null ? MIN_TAPPABLE_SQUARE_LENGTH.getValue() : 0;

            DpProp mainContentHeight =
                    dp(
                            mDeviceParameters.getScreenHeightDp()
                                    - primaryChipHeight
                                    - bottomPadding
                                    - topPadding);

            // Layout organization: column(column(primary label + spacer + (box(column(content +
            // secondary label))) + chip)

            // First column that has all other content and chip.
            Column.Builder layoutBuilder = new Column.Builder();

            // Contains primary label, main content and secondary label. Primary label will be
            // wrapped, while other content will be expanded so it can be centered in the remaining
            // space.
            Column.Builder contentAreaBuilder =
                    new Column.Builder()
                            .setWidth(expand())
                            .setHeight(mainContentHeight)
                            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER);

            // Contains main content and secondary label with wrapped height so it can be put inside
            // of the Box to be centered.
            Column.Builder contentSecondaryLabelBuilder =
                    new Column.Builder()
                            .setWidth(expand())
                            .setHeight(wrap())
                            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER);

            // Needs to be in column because of the spacers.
            Column.Builder primaryLabelBuilder =
                    new Column.Builder().setWidth(expand()).setHeight(wrap());

            if (mPrimaryLabelText != null) {
                primaryLabelBuilder.addContent(
                        new Spacer.Builder().setHeight(getPrimaryLabelTopSpacerHeight()).build());
                primaryLabelBuilder.addContent(mPrimaryLabelText);
            }

            contentAreaBuilder.addContent(primaryLabelBuilder.build());

            contentSecondaryLabelBuilder.addContent(
                    new Box.Builder()
                            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                            .setWidth(expand())
                            .setHeight(wrap())
                            .addContent(mContent)
                            .build());

            if (mSecondaryLabelText != null) {
                contentSecondaryLabelBuilder.addContent(
                        new Spacer.Builder().setHeight(mVerticalSpacerHeight).build());
                contentSecondaryLabelBuilder.addContent(mSecondaryLabelText);
            }

            contentAreaBuilder.addContent(
                    new Box.Builder()
                            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                            .setWidth(expand())
                            .setHeight(expand())
                            .addContent(contentSecondaryLabelBuilder.build())
                            .build());

            layoutBuilder
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

            layoutBuilder.addContent(contentAreaBuilder.build());

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

        /** Returns the spacer height to be placed above primary label to accommodate Tile icon. */
        @NonNull
        private DpProp getPrimaryLabelTopSpacerHeight() {
            return isRoundDevice(mDeviceParameters)
                    ? PRIMARY_LAYOUT_PRIMARY_LABEL_SPACER_HEIGHT_ROUND_DP
                    : PRIMARY_LAYOUT_PRIMARY_LABEL_SPACER_HEIGHT_SQUARE_DP;
        }
    }

    /** Get the primary label content from this layout. */
    @Nullable
    public LayoutElement getPrimaryLabelTextContent() {
        if (!areElementsPresent(PRIMARY_LABEL_PRESENT)) {
            return null;
        }
        return mPrimaryLabel.get(PRIMARY_LABEL_POSITION);
    }

    /** Get the secondary label content from this layout. */
    @Nullable
    public LayoutElement getSecondaryLabelTextContent() {
        if (!areElementsPresent(SECONDARY_LABEL_PRESENT)) {
            return null;
        }
        // By tag we know that secondary label exists. It will always be at last position.
        return mContentAndSecondaryLabel.get(mContentAndSecondaryLabel.size() - 1);
    }

    /** Get the inner content from this layout. */
    @Nullable
    public LayoutElement getContent() {
        if (!areElementsPresent(CONTENT_PRESENT)) {
            return null;
        }
        return ((Box) mContentAndSecondaryLabel.get(CONTENT_ONLY_POSITION)).getContents().get(0);
    }

    /** Get the primary chip content from this layout. */
    @Nullable
    public LayoutElement getPrimaryChipContent() {
        if (areElementsPresent(CHIP_PRESENT)) {
            return ((Box) mAllContent.get(PRIMARY_CHIP_POSITION)).getContents().get(0);
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
            LayoutElement element = mContentAndSecondaryLabel.get(CONTENT_ONLY_POSITION + 1);
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

    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return mImpl.toLayoutElementProto();
    }

    @Nullable
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public Fingerprint getFingerprint() {
        return mImpl.getFingerprint();
    }
}
