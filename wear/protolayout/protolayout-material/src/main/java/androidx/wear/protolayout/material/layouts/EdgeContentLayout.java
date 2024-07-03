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
import static androidx.wear.protolayout.material.ProgressIndicatorDefaults.DEFAULT_PADDING;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.DEFAULT_VERTICAL_SPACER_HEIGHT;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.EDGE_CONTENT_LAYOUT_MARGIN_HORIZONTAL_ROUND_DP;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.EDGE_CONTENT_LAYOUT_MARGIN_HORIZONTAL_SQUARE_DP;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.EDGE_CONTENT_LAYOUT_PADDING_ABOVE_MAIN_CONTENT_DP;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.EDGE_CONTENT_LAYOUT_PADDING_BELOW_MAIN_CONTENT_DP;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.EDGE_CONTENT_LAYOUT_RESPONSIVE_MARGIN_HORIZONTAL_PERCENT;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.EDGE_CONTENT_LAYOUT_RESPONSIVE_MARGIN_VERTICAL_PERCENT;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.EDGE_CONTENT_LAYOUT_RESPONSIVE_OUTER_MARGIN_DP;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.EDGE_CONTENT_LAYOUT_RESPONSIVE_PRIMARY_LABEL_SPACING_DP;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.LAYOUTS_LABEL_PADDING_PERCENT;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.insetElementWithPadding;
import static androidx.wear.protolayout.materialcore.Helper.checkNotNull;
import static androidx.wear.protolayout.materialcore.Helper.checkTag;
import static androidx.wear.protolayout.materialcore.Helper.getMetadataTagBytes;
import static androidx.wear.protolayout.materialcore.Helper.getTagBytes;
import static androidx.wear.protolayout.materialcore.Helper.isRoundDevice;

import static java.lang.Math.min;

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
import androidx.wear.protolayout.material.CircularProgressIndicator;
import androidx.wear.protolayout.proto.LayoutElementProto;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

/**
 * ProtoLayout layout that represents the suggested layout style for Material ProtoLayout, which has
 * content around the edge of the screen (e.g. a ProgressIndicator) and the given content inside of
 * it with the recommended margin and padding applied. Optional primary or secondary label can be
 * added above and below the additional content, respectively. Visuals and design samples can be
 * found <a href="https://developer.android.com/design/ui/wear/guides/surfaces/tiles-layouts#layout-templates">here</a>.
 *
 * <p>When accessing the contents of a container for testing, note that this element can't be simply
 * casted back to the original type, i.e.:
 *
 * <pre>{@code
 * EdgeContentLayout ecl = new EdgeContentLayout...
 * Box box = new Box.Builder().addContent(ecl).build();
 *
 * EdgeContentLayout myEcl = (EdgeContentLayout) box.getContents().get(0);
 * }</pre>
 *
 * will fail.
 *
 * <p>To be able to get {@link EdgeContentLayout} object from any layout element, {@link
 * #fromLayoutElement} method should be used, i.e.:
 *
 * <pre>{@code
 * EdgeContentLayout myEcl =
 *   EdgeContentLayout.fromLayoutElement(box.getContents().get(0));
 * }</pre>
 */
public class EdgeContentLayout implements LayoutElement {
    /**
     * Prefix tool tag for Metadata in Modifiers, so we know that Box is actually a
     * EdgeContentLayout.
     */
    static final String METADATA_TAG_PREFIX = "ECL_";

    /**
     * Index for byte array that contains bits to check whether the content and indicator are
     * present or not.
     */
    static final int FLAG_INDEX = METADATA_TAG_PREFIX.length();

    /**
     * Base tool tag for Metadata in Modifiers, so we know that Box is actually a EdgeContentLayout
     * and what optional content is added.
     */
    static final byte[] METADATA_TAG_BASE =
            Arrays.copyOf(getTagBytes(METADATA_TAG_PREFIX), FLAG_INDEX + 1);

    /**
     * Bit position in a byte on {@link #FLAG_INDEX} index in metadata byte array to check whether
     * the edge content is present or not.
     */
    static final int EDGE_CONTENT_PRESENT = 0x1;
    /**
     * Bit position in a byte on {@link #FLAG_INDEX} index in metadata byte array to check whether
     * the primary label is present or not.
     */
    static final int PRIMARY_LABEL_PRESENT = 1 << 1;
    /**
     * Bit position in a byte on {@link #FLAG_INDEX} index in metadata byte array to check whether
     * the secondary label is present or not.
     */
    static final int SECONDARY_LABEL_PRESENT = 1 << 2;
    /**
     * Bit position in a byte on {@link #FLAG_INDEX} index in metadata byte array to check whether
     * the additional content is present or not.
     */
    static final int CONTENT_PRESENT = 1 << 3;

    /**
     * Bit position in a byte on {@link #FLAG_INDEX} index in metadata byte array to check whether
     * the edge content is added before the additional content (0) or after it (1).
     */
    static final int EDGE_CONTENT_POSITION = 1 << 4;

    /**
     * Bit position in a byte on {@link #FLAG_INDEX} index in metadata byte array to check whether
     * the responsive content inset is used or not.
     */
    static final int CONTENT_INSET_USED = 1 << 5;

    @RestrictTo(Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            flag = true,
            value = {
                EDGE_CONTENT_PRESENT,
                PRIMARY_LABEL_PRESENT,
                SECONDARY_LABEL_PRESENT,
                CONTENT_PRESENT,
                EDGE_CONTENT_POSITION,
                CONTENT_INSET_USED
            })
    @interface ContentBits {}

    @NonNull private final Box mImpl;

    EdgeContentLayout(@NonNull Box layoutElement) {
        this.mImpl = layoutElement;
    }

    /** Builder class for {@link EdgeContentLayout}. */
    public static final class Builder implements LayoutElement.Builder {
        @NonNull private final DeviceParameters mDeviceParameters;
        @Nullable private LayoutElement mEdgeContent = null;
        @Nullable private LayoutElement mPrimaryLabelText = null;
        @Nullable private LayoutElement mSecondaryLabelText = null;
        @Nullable private LayoutElement mContent = null;
        private byte mMetadataContentByte = 0;
        // Default for non responsive behaviour is false (for backwards compatibility) and for
        // responsive behaviour, only true is used.
        @Nullable private Boolean mIsEdgeContentBehind = null;
        private boolean mIsResponsiveInsetEnabled = false;
        @Nullable private Float mEdgeContentThickness = null;
        @NonNull
        private DpProp mVerticalSpacerHeight =
                DEFAULT_VERTICAL_SPACER_HEIGHT;

        /**
         * Creates a builder for the {@link EdgeContentLayout}. Custom content inside of it can
         * later be set with ({@link #setContent}.
         *
         * <p>For optimal layouts across different screen sizes and better alignment with UX
         * guidelines, it is highly recommended to call {@link #setResponsiveContentInsetEnabled}.
         */
        public Builder(@NonNull DeviceParameters deviceParameters) {
            this.mDeviceParameters = deviceParameters;
        }

        /**
         * Changes this {@link EdgeContentLayout} to better follow guidelines for type of layout
         * that has content around the edge.
         *
         * <p>These updates include:
         * 1. Using responsive insets for its content primary and secondary label by adding some
         * additional space on the sides of these elements to avoid content going off the screen
         * edge.
         * 2. Changing layout padding to responsive to better follow different screen sizes.
         * 3. Positioning primary label at a fixed place on top of the screen rather than
         * following additional content.
         *
         * <p>It is highly recommended to call this method with {@code true} when using this layout
         * to optimize it for different screen sizes.
         *
         * @throws IllegalStateException if this and
         * {@link #setEdgeContentBehindAllOtherContent(boolean)} are used together.
         */
        @NonNull
        public Builder setResponsiveContentInsetEnabled(boolean enabled) {
            if (mIsEdgeContentBehind != null && !mIsEdgeContentBehind) {
                // We don't allow mixing above content with responsiveness, as content should always
                // be behind.
                throw new IllegalStateException(
                        "Setters setResponsiveContentInsetEnabled and "
                                + "setEdgeContentBehindAllOtherContent can't be used together. "
                                + "Please use only setResponsiveContentInsetEnabled, which will "
                                + "always place the edge content behind other content.");
            }

            this.mIsResponsiveInsetEnabled = enabled;
            mMetadataContentByte =
                    (byte)
                            (enabled
                                    ? (mMetadataContentByte | CONTENT_INSET_USED)
                                    : (mMetadataContentByte & ~CONTENT_INSET_USED));
            return this;
        }

        /**
         * Sets the thickness of the hollow edge content so that other content is correctly placed.
         * In other words, sets the space that should be reserved exclusively for the edge
         * content and not be overdrawn by other inner content.
         *
         * <p>For example, for {@link CircularProgressIndicator} or {@link
         * androidx.wear.protolayout.LayoutElementBuilders.ArcLine} elements, this should be equal
         * to their stroke width/thickness.
         *
         * <p>Note that, calling this method when responsiveness is not set with
         * {@link #setResponsiveContentInsetEnabled}, will be ignored.
         */
        @NonNull
        public Builder setEdgeContentThickness(@Dimension(unit = DP) float thickness) {
            this.mEdgeContentThickness = thickness;
            return this;
        }

        /**
         * Sets the content to be around the edges. This can be {@link CircularProgressIndicator}.
         *
         * <p>If this content is something other that {@link CircularProgressIndicator}, please add
         * its thickness with {@link #setEdgeContentThickness} for best results.
         */
        @NonNull
        public Builder setEdgeContent(@NonNull LayoutElement edgeContent) {
            this.mEdgeContent = edgeContent;
            mMetadataContentByte = (byte) (mMetadataContentByte | EDGE_CONTENT_PRESENT);
            return this;
        }

        /**
         * Sets the content in the primary label slot.
         *
         * <p>Depending on whether {@link #setResponsiveContentInsetEnabled} is set to true or
         * not, this label will be placed as following:
         * - If responsive behaviour is set, label will be above the additional content, on a fixed
         * place to ensure Tiles consistency with other layouts. Additionally, the label will
         * also have an inset to prevent it from going off the screen.
         * - If responsive behaviour is not set or called, label will be above the additional
         * content, centered in the remaining space.
         */
        @NonNull
        public Builder setPrimaryLabelTextContent(@NonNull LayoutElement primaryLabelText) {
            this.mPrimaryLabelText = primaryLabelText;
            mMetadataContentByte = (byte) (mMetadataContentByte | PRIMARY_LABEL_PRESENT);
            return this;
        }

        /**
         * Sets the content in the secondary label slot which will be below the additional content.
         * It is highly recommended to have primary label set when having secondary label.
         *
         * <p>Note that when {@link #setResponsiveContentInsetEnabled} is set to {@code true}, the
         * label will also have an inset to prevent it from going off the screen.
         */
        @NonNull
        public Builder setSecondaryLabelTextContent(@NonNull LayoutElement secondaryLabelText) {
            this.mSecondaryLabelText = secondaryLabelText;
            mMetadataContentByte = (byte) (mMetadataContentByte | SECONDARY_LABEL_PRESENT);
            return this;
        }

        /** Sets the additional content to this layout, inside of the screen. */
        @NonNull
        public Builder setContent(@NonNull LayoutElement content) {
            this.mContent = content;
            mMetadataContentByte = (byte) (mMetadataContentByte | CONTENT_PRESENT);
            return this;
        }

        /**
         * Sets the space size between the additional content and secondary label if there is any.
         * If one of those is not present, spacer is not used. If not set,
         * {@link LayoutDefaults#DEFAULT_VERTICAL_SPACER_HEIGHT} will
         * be used.
         *
         * <p>Note that, this method should be used together with
         * {@link #setResponsiveContentInsetEnabled}, otherwise it will be ignored.
         */
        @NonNull
        public Builder setContentAndSecondaryLabelSpacing(@NonNull DpProp height) {
            this.mVerticalSpacerHeight = height;
            return this;
        }

        /**
         * Sets whether the edge content passed in with {@link #setEdgeContent} should be positioned
         * behind all other content in this layout or above it. If not set, defaults to {@code
         * false}, meaning that the edge content will be placed above all other content.
         *
         * <p>Note that, if {@link #setResponsiveContentInsetEnabled} is set to {@code true}, edge
         * content will always go behind all other content and this method call will throw as those
         * shouldn't be mixed.
         *
         * @throws IllegalStateException if this and {@link #setResponsiveContentInsetEnabled} are
         *     used together.
         */
        @NonNull
        public Builder setEdgeContentBehindAllOtherContent(boolean isBehind) {
            if (mIsResponsiveInsetEnabled && !isBehind) {
                // We don't allow mixing this method with responsiveness.
                throw new IllegalStateException(
                        "Setters setResponsiveContentInsetEnabled and "
                                + "setEdgeContentBehindAllOtherContent can't be used together. "
                                + "Please use only setResponsiveContentInsetEnabled, which will "
                                + "always place the edge content behind other content.");
            }

            this.mIsEdgeContentBehind = isBehind;
            return this;
        }

        /** Constructs and returns {@link EdgeContentLayout} with the provided content and look. */
        @NonNull
        @Override
        public EdgeContentLayout build() {
            if (mIsResponsiveInsetEnabled
                    && mIsEdgeContentBehind != null
                    && !mIsEdgeContentBehind) {
                // We don't allow mixing requesting for edge content to be above with
                // responsiveness.
                throw new IllegalStateException(
                        "Setters setResponsiveContentInsetEnabled and "
                                + "setEdgeContentBehindAllOtherContent can't be used together. "
                                + "Please use only setResponsiveContentInsetEnabled, which will "
                                + "always place the edge content behind other content.");
            }

            return mIsResponsiveInsetEnabled ? responsiveLayoutBuild() : legacyLayoutBuild();
        }

        @NonNull
        private EdgeContentLayout responsiveLayoutBuild() {
            // Calculate what is the inset box max size, i.e., the size that all content can occupy
            // without the edge content.
            // Use provided thickness if set. Otherwise, see if we can get it from
            // CircularProgressIndicator.
            float edgeContentSize = getEdgeContentSize();

            DpProp contentHeight = dp(
                    mDeviceParameters.getScreenWidthDp() - edgeContentSize);
            DpProp contentWidth = dp(
                    mDeviceParameters.getScreenHeightDp() - edgeContentSize);

            float outerMargin =
                    mEdgeContent instanceof CircularProgressIndicator
                            && ((CircularProgressIndicator) mEdgeContent).isOuterMarginApplied()
                            ? 0 // CPI has this margin already.
                            : EDGE_CONTENT_LAYOUT_RESPONSIVE_OUTER_MARGIN_DP;

            // Horizontal and vertical padding added to the inner content.
            float horizontalPaddingDp =
                    EDGE_CONTENT_LAYOUT_RESPONSIVE_MARGIN_HORIZONTAL_PERCENT
                            * mDeviceParameters.getScreenWidthDp();
            float verticalPaddingDp =
                    EDGE_CONTENT_LAYOUT_RESPONSIVE_MARGIN_VERTICAL_PERCENT
                            * mDeviceParameters.getScreenWidthDp();

            // Padding to restrict labels from going off the screen.
            float labelHorizontalPaddingDp =
                    mDeviceParameters.getScreenWidthDp() * LAYOUTS_LABEL_PADDING_PERCENT;

            Modifiers modifiers =
                    new Modifiers.Builder()
                            .setPadding(
                                    new Padding.Builder()
                                            .setRtlAware(true)
                                            .setStart(dp(horizontalPaddingDp))
                                            .setEnd(dp(horizontalPaddingDp))
                                            .setTop(dp(verticalPaddingDp))
                                            .setBottom(dp(verticalPaddingDp))
                                            .build())
                            .build();

            // In this variant, it's always behind so resetting the flag to 0.
            mMetadataContentByte = (byte) (mMetadataContentByte & ~EDGE_CONTENT_POSITION);
            byte[] metadata = METADATA_TAG_BASE.clone();
            metadata[FLAG_INDEX] = mMetadataContentByte;

            Box.Builder layout =
                    new Box.Builder()
                            .setWidth(dp(mDeviceParameters.getScreenWidthDp()))
                            .setHeight(dp(mDeviceParameters.getScreenHeightDp()))
                            .setModifiers(
                                    new Modifiers.Builder()
                                            .setMetadata(
                                                    new ElementMetadata.Builder()
                                                            .setTagData(metadata).build())
                                            .setPadding(
                                                    new Padding.Builder()
                                                            .setAll(dp(outerMargin))
                                                            .setRtlAware(true).build())
                                            .build());

            if (mEdgeContent != null) {
                layout.addContent(mEdgeContent);
            }

            // Contains primary label, additional content and secondary label.
            Column.Builder allInnerContent =
                    new Column.Builder()
                            .setWidth(contentWidth)
                            .setHeight(contentHeight)
                            .setModifiers(modifiers);

            if (mPrimaryLabelText != null) {
                allInnerContent.addContent(
                        insetElementWithPadding(mPrimaryLabelText, labelHorizontalPaddingDp));
                allInnerContent.addContent(
                        new Spacer.Builder()
                                .setHeight(EDGE_CONTENT_LAYOUT_RESPONSIVE_PRIMARY_LABEL_SPACING_DP)
                                .build());
            }

            // Contains additional content and secondary label with wrapped height so it can be put
            // inside of the Box to be centered. This is because primary label stays on top at
            // the fixed place, while this content should be centered in the remaining space.
            Column.Builder contentSecondaryLabel =
                    new Column.Builder().setWidth(expand()).setHeight(wrap());

            if (mContent != null) {
                contentSecondaryLabel.addContent(mContent);
            }

            if (mSecondaryLabelText != null) {
                if (mContent != null) {
                    contentSecondaryLabel.addContent(
                            new Spacer.Builder().setHeight(mVerticalSpacerHeight).build());
                }
                contentSecondaryLabel.addContent(
                        insetElementWithPadding(mSecondaryLabelText, labelHorizontalPaddingDp));
            }

            allInnerContent.addContent(
                    new Box.Builder()
                            .setWidth(expand())
                            .setHeight(expand())
                            .addContent(contentSecondaryLabel.build())
                            .build());

            layout.addContent(allInnerContent.build());

            return new EdgeContentLayout(layout.build());
        }

        private float getEdgeContentSize() {
            float edgeContentThickness =
                    mEdgeContentThickness == null
                            ?
                            // When not set, we try to get the thickness from CPI, otherwise we can
                            // only use 0.
                            (mEdgeContent instanceof CircularProgressIndicator
                                    ? ((CircularProgressIndicator) mEdgeContent)
                                    .getStrokeWidth().getValue()
                                    : 0)
                            : mEdgeContentThickness;
            return 2 * (EDGE_CONTENT_LAYOUT_RESPONSIVE_OUTER_MARGIN_DP + edgeContentThickness);
        }

        @NonNull
        private EdgeContentLayout legacyLayoutBuild() {
            if (mIsEdgeContentBehind == null) {
                mIsEdgeContentBehind = false;
            }
            float thicknessDp =
                    mEdgeContent instanceof CircularProgressIndicator
                            ? ((CircularProgressIndicator) mEdgeContent).getStrokeWidth().getValue()
                            : 0;
            float horizontalPaddingDp =
                    isRoundDevice(mDeviceParameters)
                            ? EDGE_CONTENT_LAYOUT_MARGIN_HORIZONTAL_ROUND_DP
                            : EDGE_CONTENT_LAYOUT_MARGIN_HORIZONTAL_SQUARE_DP;
            float indicatorWidth = 2 * (thicknessDp + DEFAULT_PADDING.getValue());
            float contentHeightDp = mDeviceParameters.getScreenHeightDp() - indicatorWidth;
            float contentWidthDp = mDeviceParameters.getScreenWidthDp() - indicatorWidth;

            DpProp contentHeight = dp(min(contentHeightDp, contentWidthDp));
            DpProp contentWidth = dp(min(contentHeightDp, contentWidthDp));

            Modifiers modifiers =
                    new Modifiers.Builder()
                            .setPadding(
                                    new Padding.Builder()
                                            .setRtlAware(true)
                                            .setStart(dp(horizontalPaddingDp))
                                            .setEnd(dp(horizontalPaddingDp))
                                            .build())
                            .build();

            if (!mIsEdgeContentBehind) {
                // If the edge content is above the additional one, then its index should be 1.
                // Otherwise it's 0.
                mMetadataContentByte = (byte) (mMetadataContentByte | EDGE_CONTENT_POSITION);
            }

            byte[] metadata = METADATA_TAG_BASE.clone();
            metadata[FLAG_INDEX] = mMetadataContentByte;
            Box.Builder mainBoxBuilder =
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
                            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER);

            Column.Builder innerContentBuilder =
                    new Column.Builder()
                            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER);

            if (mPrimaryLabelText != null) {
                innerContentBuilder.addContent(mPrimaryLabelText);
                innerContentBuilder.addContent(
                        new Spacer.Builder()
                                .setHeight(dp(EDGE_CONTENT_LAYOUT_PADDING_ABOVE_MAIN_CONTENT_DP))
                                .build());
            }

            if (mContent != null) {
                innerContentBuilder.addContent(
                        new Box.Builder()
                                .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                                .addContent(mContent)
                                .build());
            }

            if (mSecondaryLabelText != null) {
                innerContentBuilder.addContent(
                        new Spacer.Builder()
                                .setHeight(dp(EDGE_CONTENT_LAYOUT_PADDING_BELOW_MAIN_CONTENT_DP))
                                .build());
                innerContentBuilder.addContent(mSecondaryLabelText);
            }

            Box innerContentBox =
                    new Box.Builder()
                            .setModifiers(modifiers)
                            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                            .setHeight(contentHeight)
                            .setWidth(contentWidth)
                            .addContent(innerContentBuilder.build())
                            .build();

            if (mIsEdgeContentBehind) {
                if (mEdgeContent != null) {
                    mainBoxBuilder.addContent(mEdgeContent);
                }
                mainBoxBuilder.addContent(innerContentBox);
            } else {
                mainBoxBuilder.addContent(innerContentBox);
                if (mEdgeContent != null) {
                    mainBoxBuilder.addContent(mEdgeContent);
                }
            }

            return new EdgeContentLayout(mainBoxBuilder.build());
        }
    }

    private boolean areElementsPresent(@ContentBits int elementFlag) {
        return (getMetadataTag()[FLAG_INDEX] & elementFlag) == elementFlag;
    }

    /** Returns metadata tag set to this EdgeContentLayout. */
    @NonNull
    byte[] getMetadataTag() {
        return getMetadataTagBytes(checkNotNull(checkNotNull(mImpl.getModifiers()).getMetadata()));
    }

    /** Returns the inner content from this layout. */
    @Nullable
    public LayoutElement getContent() {
        if (!areElementsPresent(CONTENT_PRESENT)) {
            return null;
        }
        if (isResponsiveContentInsetEnabled()) {
            return getInnerColumnContentsForResponsive().get(0);
        } else {
            // By tag we know that content exists. It will be at position 0 if there is no primary
            // label, or at position 2 (primary label, spacer - content) otherwise.
            int contentPosition = areElementsPresent(PRIMARY_LABEL_PRESENT) ? 2 : 0;
            Box box = (Box) getInnerContent(contentPosition);
            return box.getContents().get(0);
        }
    }

    /**
     * Returns element from the inner content that is on the given index. It is a callers
     * responsibility to pass in the correct index.
     */
    private LayoutElement getInnerContent(int contentPosition) {
        return getAllContent().getContents().get(contentPosition);
    }

    /** Get the primary label content from this layout. */
    @Nullable
    public LayoutElement getPrimaryLabelTextContent() {
        if (!areElementsPresent(PRIMARY_LABEL_PRESENT)) {
            return null;
        }
        // By tag we know that primary label exists. It will always be at position 0 in the inner
        // content area.
        return isResponsiveContentInsetEnabled()
                ? ((Box) getInnerContent(0)).getContents().get(0)
                : getInnerContent(0);
    }

    /** Get the secondary label content from this layout. */
    @Nullable
    public LayoutElement getSecondaryLabelTextContent() {
        if (!areElementsPresent(SECONDARY_LABEL_PRESENT)) {
            return null;
        }
        if (isResponsiveContentInsetEnabled()) {
            List<LayoutElement> innerColumnContents = getInnerColumnContentsForResponsive();
            return ((Box) innerColumnContents.get(innerColumnContents.size() - 1))
                    .getContents().get(0);
        } else {
            // By tag we know that secondary label exists. It will always be at last position.
            List<LayoutElement> mInnerColumn = getAllContent().getContents();
            return mInnerColumn.get(mInnerColumn.size() - 1);
        }
    }

    /** Get the size of spacing between content and secondary from this layout. */
    @Dimension(unit = Dimension.DP)
    public float getContentAndSecondaryLabelSpacing() {
        if (!isResponsiveContentInsetEnabled()) {
            return DEFAULT_VERTICAL_SPACER_HEIGHT.getValue();
        }

        List<LayoutElement> innerColumnContents = getInnerColumnContentsForResponsive();
        if (areElementsPresent(CONTENT_PRESENT) && areElementsPresent(SECONDARY_LABEL_PRESENT)) {
            LayoutElement element =
                    ((Box) innerColumnContents.get(innerColumnContents.size() - 2))
                            .getContents().get(0);
            if (element instanceof Spacer) {
                SpacerDimension height = ((Spacer) element).getHeight();
                if (height instanceof DpProp) {
                    return ((DpProp) height).getValue();
                }
            }
        }
        return DEFAULT_VERTICAL_SPACER_HEIGHT.getValue();
    }

    /** Returns the edge content from this layout. */
    @Nullable
    public LayoutElement getEdgeContent() {
        return areElementsPresent(EDGE_CONTENT_PRESENT)
                ? mImpl.getContents().get(getEdgeContentPosition()) : null;
    }

    private int getEdgeContentPosition() {
        return isEdgeContentBehindAllOtherContent() ? 0 : 1;
    }

    /** Returns if the edge content has been placed behind the other contents. */
    public boolean isEdgeContentBehindAllOtherContent() {
        return (getMetadataTag()[FLAG_INDEX] & EDGE_CONTENT_POSITION) == 0;
    }

    /** Returns whether the contents from this layout are using responsive inset. */
    public boolean isResponsiveContentInsetEnabled() {
        return areElementsPresent(CONTENT_INSET_USED);
    }

    /** Returns the total size of the edge content including margins. */
    public float getEdgeContentThickness() {
        Column allContent = getAllContent();
        if (mImpl.getWidth() instanceof DpProp && allContent.getWidth() instanceof DpProp) {
            float edgeContentTotalThickness =
                    ((DpProp) mImpl.getWidth()).getValue()
                            - ((DpProp) allContent.getWidth()).getValue();
            return edgeContentTotalThickness / 2 - EDGE_CONTENT_LAYOUT_RESPONSIVE_OUTER_MARGIN_DP;
        }
        return 0;
    }

    /** Returns Column that may contain primary label, additional content and secondary label. */
    private Column getAllContent() {
        int contentIndex = 1 - getEdgeContentPosition();
        return (Column) (isResponsiveContentInsetEnabled()
                ? mImpl.getContents().get(areElementsPresent(EDGE_CONTENT_PRESENT)
                    ? 1 : 0)
                : ((Box) mImpl.getContents().get(contentIndex)).getContents().get(0));
    }

    /**
     * Returns all content inside of the inner Column that may contain additional content, spacer
     * and secondary label.
     */
    private List<LayoutElement> getInnerColumnContentsForResponsive() {
        List<LayoutElement> allContent = getAllContent().getContents();
        Box box = (Box) allContent.get(allContent.size() - 1);
        Column column = (Column) box.getContents().get(0);
        return column.getContents();
    }

    /**
     * Returns EdgeContentLayout object from the given LayoutElement (e.g. one retrieved from a
     * container's content with {@code container.getContents().get(index)}) if that element can be
     * converted to EdgeContentLayout. Otherwise, it will return null.
     */
    @Nullable
    public static EdgeContentLayout fromLayoutElement(@NonNull LayoutElement element) {
        if (element instanceof EdgeContentLayout) {
            return (EdgeContentLayout) element;
        }
        if (!(element instanceof Box)) {
            return null;
        }
        Box boxElement = (Box) element;
        if (!checkTag(boxElement.getModifiers(), METADATA_TAG_PREFIX, METADATA_TAG_BASE)) {
            return null;
        }
        // Now we are sure that this element is a EdgeContentLayout.
        return new EdgeContentLayout(boxElement);
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
