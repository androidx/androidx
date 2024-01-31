/*
 * Copyright 2024 The Android Open Source Project
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
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.EDGE_CONTENT_LAYOUT2_MARGIN_HORIZONTAL_PERCENT;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.EDGE_CONTENT_LAYOUT2_MARGIN_VERTICAL_PERCENT;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.EDGE_CONTENT_LAYOUT2_OUTER_MARGIN_DP;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.EDGE_CONTENT_LAYOUT2_CONTENT_AND_SECONDARY_LABEL_SPACING_DP;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.LAYOUTS_LABEL_PADDING_PERCENT;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.insetElementWithPadding;
import static androidx.wear.protolayout.materialcore.Helper.checkNotNull;
import static androidx.wear.protolayout.materialcore.Helper.checkTag;
import static androidx.wear.protolayout.materialcore.Helper.getMetadataTagBytes;
import static androidx.wear.protolayout.materialcore.Helper.getTagBytes;

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
 * content around the edge of the screen (e.g. a CircularProgressIndicator) and the given content
 * inside of it with the recommended margin and padding applied. Optional primary or secondary label
 * can be added above and below the additional content, respectively.
 *
 * <p>When accessing the contents of a container for testing, note that this element can't be simply
 * casted back to the original type, i.e.:
 *
 * <pre>{@code
 * EdgeContentLayout2 ecl = new EdgeContentLayout2...
 * Box box = new Box.Builder().addContent(ecl).build();
 *
 * EdgeContentLayout2 myEcl = (EdgeContentLayout2) box.getContents().get(0);
 * }</pre>
 *
 * will fail.
 *
 * <p>To be able to get {@link EdgeContentLayout2} object from any layout element, {@link
 * #fromLayoutElement} method should be used, i.e.:
 *
 * <pre>{@code
 * EdgeContentLayout2 myEcl =
 *   EdgeContentLayout.fromLayoutElement(box.getContents().get(0));
 * }</pre>
 */
// TODO(b/274916652): Link visuals once they are available.
public final class EdgeContentLayout2 implements LayoutElement {
    /**
     * Prefix tool tag for Metadata in Modifiers, so we know that Box is actually a
     * EdgeContentLayout.
     */
    static final String METADATA_TAG_PREFIX = "ECL2_";

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

    @RestrictTo(Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            flag = true,
            value = {
                    EDGE_CONTENT_PRESENT,
                    PRIMARY_LABEL_PRESENT,
                    SECONDARY_LABEL_PRESENT,
                    CONTENT_PRESENT
            })
    @interface ContentBits {}

    // Top level impl element.
    @NonNull private final Box mImpl;

    EdgeContentLayout2(@NonNull Box layoutElement) {
        this.mImpl = layoutElement;
    }

    /** Builder class for {@link EdgeContentLayout2}. */
    public static final class Builder implements LayoutElement.Builder {
        @NonNull private final DeviceParameters mDeviceParameters;
        @Nullable private LayoutElement mEdgeContent = null;
        @Nullable private LayoutElement mPrimaryLabel = null;
        @Nullable private LayoutElement mSecondaryLabel = null;
        @Nullable private LayoutElement mContent = null;

        @NonNull
        private DpProp mVerticalSpacerHeight =
                EDGE_CONTENT_LAYOUT2_CONTENT_AND_SECONDARY_LABEL_SPACING_DP;

        private byte mMetadataContentByte = 0;
        @Nullable private Float mEdgeContentThickness = null;

        /**
         * Creates a builder for the {@link EdgeContentLayout2}. Custom content inside of it can
         * later be set with {@link #setEdgeContent}, {@link #setContent},
         * {@link #setPrimaryLabelContent} and {@link #setSecondaryLabelContent}.
         */
        public Builder(@NonNull DeviceParameters deviceParameters) {
            this.mDeviceParameters = deviceParameters;
        }

        /**
         * Sets the content to be around the edges. This can be {@link CircularProgressIndicator},
         * custom {@link androidx.wear.protolayout.LayoutElementBuilders.Arc}, image or other
         * element.
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
         * Sets the thickness of the hollow edge content so that other content is correctly placed.
         * In other words, sets the space that should be reserved exclusively for the edge
         * content and not be overdrawn by other inner content.
         *
         * <p>For example, for {@link CircularProgressIndicator} or {@link
         * androidx.wear.protolayout.LayoutElementBuilders.ArcLine} elements, this should be equal
         * to their stroke width/thickness.
         */
        @NonNull
        public Builder setEdgeContentThickness(@Dimension(unit = DP) float thickness) {
            this.mEdgeContentThickness = thickness;
            return this;
        }

        /**
         * Sets the content in the primary label slot which will be above the additional content,
         * on a fixed place to ensure Tiles consistency with other layouts.
         *
         * <p>The label will also have an inset to prevent it from going off the screen.
         */
        @NonNull
        public Builder setPrimaryLabelContent(@NonNull LayoutElement primaryLabel) {
            this.mPrimaryLabel = primaryLabel;
            mMetadataContentByte = (byte) (mMetadataContentByte | PRIMARY_LABEL_PRESENT);
            return this;
        }

        /**
         * Sets the content in the secondary label slot which will be below the additional content.
         * It is highly recommended to have primary label set when having secondary label.
         *
         * <p>The label will also have an inset to prevent it from going off the screen.
         */
        @NonNull
        public Builder setSecondaryLabelContent(@NonNull LayoutElement secondaryLabel) {
            this.mSecondaryLabel = secondaryLabel;
            mMetadataContentByte = (byte) (mMetadataContentByte | SECONDARY_LABEL_PRESENT);
            return this;
        }

        /**
         * Sets the additional content to center of this layout, inside of the edge content and
         * between labels if present.
         */
        @NonNull
        public Builder setContent(@NonNull LayoutElement content) {
            this.mContent = content;
            mMetadataContentByte = (byte) (mMetadataContentByte | CONTENT_PRESENT);
            return this;
        }

        /**
         * Sets the size of space between an additional content and secondary label if there is
         * any. If one of those is not present, spacer is not used. If not set,
         * {@link LayoutDefaults#EDGE_CONTENT_LAYOUT2_CONTENT_AND_SECONDARY_LABEL_SPACING_DP} will be
         * used.
         */
        @NonNull
        public Builder setContentAndSecondaryLabelSpacing(@NonNull DpProp height) {
            this.mVerticalSpacerHeight = height;
            return this;
        }

        /** Constructs and returns {@link EdgeContentLayout2} with the provided content and look. */
        @SuppressLint("CheckResult") // (b/247804720)
        @NonNull
        @Override
        public EdgeContentLayout2 build() {
            // Calculate what is the inset box max size, i.e., the size that all content can occupy
            // without the edge content.
            // Use provided thickness if set. Otherwise, see if we can get it from
            // CircularProgressIndicator.
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
            float edgeContentSize = 2
                    * (EDGE_CONTENT_LAYOUT2_OUTER_MARGIN_DP + edgeContentThickness);

            DpProp contentHeight = dp(
                    mDeviceParameters.getScreenWidthDp() - edgeContentSize);
            DpProp contentWidth = dp(
                    mDeviceParameters.getScreenHeightDp() - edgeContentSize);

            // TODO(b/321681652): Confirm with the UX if we can put 6dp as outer margin so it
            //  matches CPI.
            float outerMargin =
                    mEdgeContent instanceof CircularProgressIndicator
                            ? EDGE_CONTENT_LAYOUT2_OUTER_MARGIN_DP - DEFAULT_PADDING.getValue()
                            : EDGE_CONTENT_LAYOUT2_OUTER_MARGIN_DP;

            // Horizontal and vertical padding added to the inner content.
            float horizontalPaddingDp =
                    EDGE_CONTENT_LAYOUT2_MARGIN_HORIZONTAL_PERCENT
                            * mDeviceParameters.getScreenWidthDp();
            float verticalPaddingDp =
                    EDGE_CONTENT_LAYOUT2_MARGIN_VERTICAL_PERCENT
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

            if (mPrimaryLabel != null) {
                allInnerContent.addContent(
                        insetElementWithPadding(mPrimaryLabel, labelHorizontalPaddingDp));
                // TODO(b/321681652): Confirm with the UX that we don't need a space after this
                //  abel.
            }

            // Contains additional content and secondary label with wrapped height so it can be put
            // inside of the Box to be centered. This is because primary label stays on top at
            // the fixed place, while this content should be centered in the remaining space.
            Column.Builder contentSecondaryLabel =
                    new Column.Builder().setWidth(expand()).setHeight(wrap());

            if (mContent != null) {
                contentSecondaryLabel.addContent(mContent);
            }

            if (mSecondaryLabel != null) {
                if (mContent != null) {
                    contentSecondaryLabel.addContent(
                            new Spacer.Builder().setHeight(mVerticalSpacerHeight).build());
                }
                contentSecondaryLabel.addContent(
                        insetElementWithPadding(mSecondaryLabel, labelHorizontalPaddingDp));
            }

            allInnerContent.addContent(
                    new Box.Builder()
                            .setWidth(expand())
                            .setHeight(expand())
                            .addContent(contentSecondaryLabel.build())
                            .build());

            layout.addContent(allInnerContent.build());

            return new EdgeContentLayout2(layout.build());
        }
    }

    private boolean areElementsPresent(@ContentBits int elementFlag) {
        return (getMetadataTag()[FLAG_INDEX] & elementFlag) == elementFlag;
    }

    /** Returns Column that may contain primary label, additional content and secondary label. */
    private Column getAllContent() {
        return (Column) mImpl.getContents().get(areElementsPresent(EDGE_CONTENT_PRESENT) ? 1 : 0);
    }

    /**
     * Returns all content inside of the inner Column that may contain additional content, spacer
     * and secondary label.
     */
    private List<LayoutElement> getInnerColumnContents() {
        return ((Column)
                ((Box)
                        getAllContent()
                                .getContents()
                                .get(areElementsPresent(PRIMARY_LABEL_PRESENT) ? 1 : 0))
                        .getContents()
                        .get(0))
                .getContents();
    }

    /** Returns metadata tag set to this EdgeContentLayout. */
    @NonNull
    byte[] getMetadataTag() {
        return getMetadataTagBytes(checkNotNull(checkNotNull(mImpl.getModifiers()).getMetadata()));
    }

    /** Returns the inner content from this layout. */
    @Nullable
    public LayoutElement getContent() {
        return areElementsPresent(CONTENT_PRESENT) ? getInnerColumnContents().get(0) : null;
    }

    /** Get the primary label content from this layout. */
    @Nullable
    public LayoutElement getPrimaryLabelContent() {
        return areElementsPresent(PRIMARY_LABEL_PRESENT)
                ? ((Box) getAllContent().getContents().get(0)).getContents().get(0)
                : null;
    }

    /** Get the secondary label content from this layout. */
    @Nullable
    public LayoutElement getSecondaryLabelContent() {
        List<LayoutElement> innerColumnContents = getInnerColumnContents();
        return areElementsPresent(SECONDARY_LABEL_PRESENT)
                ? ((Box) innerColumnContents.get(innerColumnContents.size() - 1))
                    .getContents().get(0)
                : null;
    }

    /** Get the size of spacing between content and secondary from this layout. */
    @Dimension(unit = Dimension.DP)
    public float getContentAndSecondaryLabelSpacing() {
        List<LayoutElement> innerColumnContents = getInnerColumnContents();
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
        return EDGE_CONTENT_LAYOUT2_CONTENT_AND_SECONDARY_LABEL_SPACING_DP.getValue();
    }

    /** Returns the edge content from this layout. */
    @Nullable
    public LayoutElement getEdgeContent() {
        return areElementsPresent(EDGE_CONTENT_PRESENT) ? mImpl.getContents().get(0) : null;
    }

    /** Returns the total size of the edge content including margins. */
    public float getEdgeContentThickness() {
        Column allContent = getAllContent();
        if (mImpl.getWidth() instanceof DpProp && allContent.getWidth() instanceof DpProp) {
            float edgeContentTotalThickness =
                    ((DpProp) mImpl.getWidth()).getValue()
                            - ((DpProp) allContent.getWidth()).getValue();
            return edgeContentTotalThickness / 2 - EDGE_CONTENT_LAYOUT2_OUTER_MARGIN_DP;
        }
        return 0;
    }

    /**
     * Returns EdgeContentLayout object from the given LayoutElement (e.g. one retrieved from a
     * container's content with {@code container.getContents().get(index)}) if that element can be
     * converted to EdgeContentLayout. Otherwise, it will return null.
     */
    @Nullable
    public static EdgeContentLayout2 fromLayoutElement(@NonNull LayoutElement element) {
        if (element instanceof EdgeContentLayout2) {
            return (EdgeContentLayout2) element;
        }
        if (!(element instanceof Box)) {
            return null;
        }
        Box boxElement = (Box) element;
        if (!checkTag(boxElement.getModifiers(), METADATA_TAG_PREFIX, METADATA_TAG_BASE)) {
            return null;
        }
        // Now we are sure that this element is a EdgeContentLayout.
        return new EdgeContentLayout2(boxElement);
    }

    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return mImpl.toLayoutElementProto();
    }

    @Nullable
    @Override
    public Fingerprint getFingerprint() {
        return mImpl.getFingerprint();
    }
}
