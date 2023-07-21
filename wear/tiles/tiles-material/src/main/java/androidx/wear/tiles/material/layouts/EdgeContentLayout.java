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

import static androidx.wear.tiles.material.Helper.checkNotNull;
import static androidx.wear.tiles.material.Helper.checkTag;
import static androidx.wear.tiles.material.Helper.getMetadataTagBytes;
import static androidx.wear.tiles.material.Helper.getTagBytes;
import static androidx.wear.tiles.material.Helper.isRoundDevice;
import static androidx.wear.tiles.material.ProgressIndicatorDefaults.DEFAULT_PADDING;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.EDGE_CONTENT_LAYOUT_MARGIN_HORIZONTAL_ROUND_DP;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.EDGE_CONTENT_LAYOUT_MARGIN_HORIZONTAL_SQUARE_DP;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.EDGE_CONTENT_LAYOUT_PADDING_ABOVE_MAIN_CONTENT_DP;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.EDGE_CONTENT_LAYOUT_PADDING_BELOW_MAIN_CONTENT_DP;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.proto.LayoutElementProto;
import androidx.wear.tiles.material.CircularProgressIndicator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

/**
 * Tiles layout that represents the suggested layout style for Material Tiles, which has content
 * around the edge of the screen (e.g. a ProgressIndicator) and the given content inside of it with
 * the recommended margin and padding applied. Optional primary or secondary label can be added
 * above and below the main content, respectively.
 *
 * <p>For additional examples and suggested layouts see <a
 * href="/training/wearables/design/tiles-design-system">Tiles Design System</a>.
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
 *
 * @deprecated Use the new class {@link
 *     androidx.wear.protolayout.material.layouts.EdgeContentLayout} which provides the same API and
 *     functionality.
 */
@Deprecated
@SuppressWarnings("deprecation")
public class EdgeContentLayout implements androidx.wear.tiles.LayoutElementBuilders.LayoutElement {
    /**
     * Prefix tool tag for Metadata in androidx.wear.tiles.ModifiersBuilders.Modifiers, so we know
     * that androidx.wear.tiles.LayoutElementBuilders.Box is actually a EdgeContentLayout.
     */
    static final String METADATA_TAG_PREFIX = "ECL_";

    /**
     * Index for byte array that contains bits to check whether the content and indicator are
     * present or not.
     */
    static final int FLAG_INDEX = METADATA_TAG_PREFIX.length();

    /**
     * Base tool tag for Metadata in androidx.wear.tiles.ModifiersBuilders.Modifiers, so we know
     * that androidx.wear.tiles.LayoutElementBuilders.Box is actually a EdgeContentLayout and what
     * optional content is added.
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
    static final int PRIMARY_LABEL_PRESENT = 0x2;
    /**
     * Bit position in a byte on {@link #FLAG_INDEX} index in metadata byte array to check whether
     * the secondary label is present or not.
     */
    static final int SECONDARY_LABEL_PRESENT = 0x4;
    /**
     * Bit position in a byte on {@link #FLAG_INDEX} index in metadata byte array to check whether
     * the main content is present or not.
     */
    static final int CONTENT_PRESENT = 0x8;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            flag = true,
            value = {
                EDGE_CONTENT_PRESENT,
                PRIMARY_LABEL_PRESENT,
                CONTENT_PRESENT,
                SECONDARY_LABEL_PRESENT
            })
    @interface ContentBits {}

    @NonNull private final androidx.wear.tiles.LayoutElementBuilders.Box mImpl;

    // This contains inner columns and edge content.
    @NonNull private final List<androidx.wear.tiles.LayoutElementBuilders.LayoutElement> mContents;

    // This contains optional labels, spacers and main content.
    @NonNull
    private final List<androidx.wear.tiles.LayoutElementBuilders.LayoutElement> mInnerColumn;

    EdgeContentLayout(@NonNull androidx.wear.tiles.LayoutElementBuilders.Box layoutElement) {
        this.mImpl = layoutElement;
        this.mContents = mImpl.getContents();
        this.mInnerColumn =
                ((androidx.wear.tiles.LayoutElementBuilders.Column)
                                ((androidx.wear.tiles.LayoutElementBuilders.Box) mContents.get(0))
                                        .getContents()
                                        .get(0))
                        .getContents();
    }

    /** Builder class for {@link EdgeContentLayout}. */
    public static final class Builder
            implements androidx.wear.tiles.LayoutElementBuilders.LayoutElement.Builder {
        @NonNull
        private final androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
                mDeviceParameters;

        @Nullable
        private androidx.wear.tiles.LayoutElementBuilders.LayoutElement mEdgeContent = null;

        @Nullable
        private androidx.wear.tiles.LayoutElementBuilders.LayoutElement mPrimaryLabelText = null;

        @Nullable
        private androidx.wear.tiles.LayoutElementBuilders.LayoutElement mSecondaryLabelText = null;

        @Nullable private androidx.wear.tiles.LayoutElementBuilders.LayoutElement mContent = null;
        private byte mMetadataContentByte = 0;

        /**
         * Creates a builder for the {@link EdgeContentLayout}t. Custom content inside of it can
         * later be set with ({@link #setContent}.
         */
        public Builder(
                @NonNull
                        androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
                                deviceParameters) {
            this.mDeviceParameters = deviceParameters;
        }

        /**
         * Sets the content to be around the edges. This can be {@link CircularProgressIndicator}.
         */
        @NonNull
        public Builder setEdgeContent(
                @NonNull androidx.wear.tiles.LayoutElementBuilders.LayoutElement edgeContent) {
            this.mEdgeContent = edgeContent;
            mMetadataContentByte = (byte) (mMetadataContentByte | EDGE_CONTENT_PRESENT);
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
         * Sets the content in the secondary label slot which will be below the main content. It is
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

        /** Sets the additional content to this layout, inside of the screen. */
        @NonNull
        public Builder setContent(
                @NonNull androidx.wear.tiles.LayoutElementBuilders.LayoutElement content) {
            this.mContent = content;
            mMetadataContentByte = (byte) (mMetadataContentByte | CONTENT_PRESENT);
            return this;
        }

        /** Constructs and returns {@link EdgeContentLayout} with the provided content and look. */
        @NonNull
        @Override
        public EdgeContentLayout build() {
            float thicknessDp =
                    mEdgeContent instanceof CircularProgressIndicator
                            ? ((CircularProgressIndicator) mEdgeContent).getStrokeWidth().getValue()
                            : 0;
            float horizontalPaddingDp =
                    isRoundDevice(mDeviceParameters)
                            ? EDGE_CONTENT_LAYOUT_MARGIN_HORIZONTAL_ROUND_DP
                            : EDGE_CONTENT_LAYOUT_MARGIN_HORIZONTAL_SQUARE_DP;
            float indicatorWidth = 2 * (thicknessDp + DEFAULT_PADDING.getValue());
            float mainContentHeightDp = mDeviceParameters.getScreenHeightDp() - indicatorWidth;
            float mainContentWidthDp = mDeviceParameters.getScreenWidthDp() - indicatorWidth;

            androidx.wear.tiles.DimensionBuilders.DpProp mainContentHeight =
                    androidx.wear.tiles.DimensionBuilders.dp(
                            Math.min(mainContentHeightDp, mainContentWidthDp));
            androidx.wear.tiles.DimensionBuilders.DpProp mainContentWidth =
                    androidx.wear.tiles.DimensionBuilders.dp(
                            Math.min(mainContentHeightDp, mainContentWidthDp));

            androidx.wear.tiles.ModifiersBuilders.Modifiers modifiers =
                    new androidx.wear.tiles.ModifiersBuilders.Modifiers.Builder()
                            .setPadding(
                                    new androidx.wear.tiles.ModifiersBuilders.Padding.Builder()
                                            .setStart(
                                                    androidx.wear.tiles.DimensionBuilders.dp(
                                                            horizontalPaddingDp))
                                            .setEnd(
                                                    androidx.wear.tiles.DimensionBuilders.dp(
                                                            horizontalPaddingDp))
                                            .build())
                            .build();

            byte[] metadata = METADATA_TAG_BASE.clone();
            metadata[FLAG_INDEX] = mMetadataContentByte;
            androidx.wear.tiles.LayoutElementBuilders.Box.Builder mainBoxBuilder =
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
                            .setHorizontalAlignment(
                                    androidx.wear.tiles.LayoutElementBuilders
                                            .HORIZONTAL_ALIGN_CENTER)
                            .setVerticalAlignment(
                                    androidx.wear.tiles.LayoutElementBuilders
                                            .VERTICAL_ALIGN_CENTER);

            androidx.wear.tiles.LayoutElementBuilders.Column.Builder innerContentBuilder =
                    new androidx.wear.tiles.LayoutElementBuilders.Column.Builder()
                            .setHorizontalAlignment(
                                    androidx.wear.tiles.LayoutElementBuilders
                                            .HORIZONTAL_ALIGN_CENTER);

            if (mPrimaryLabelText != null) {
                innerContentBuilder.addContent(mPrimaryLabelText);
                innerContentBuilder.addContent(
                        new androidx.wear.tiles.LayoutElementBuilders.Spacer.Builder()
                                .setHeight(
                                        androidx.wear.tiles.DimensionBuilders.dp(
                                                EDGE_CONTENT_LAYOUT_PADDING_ABOVE_MAIN_CONTENT_DP))
                                .build());
            }

            if (mContent != null) {
                innerContentBuilder.addContent(
                        new androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
                                .setVerticalAlignment(
                                        androidx.wear.tiles.LayoutElementBuilders
                                                .VERTICAL_ALIGN_CENTER)
                                .addContent(mContent)
                                .build());
            }

            if (mSecondaryLabelText != null) {
                innerContentBuilder.addContent(
                        new androidx.wear.tiles.LayoutElementBuilders.Spacer.Builder()
                                .setHeight(
                                        androidx.wear.tiles.DimensionBuilders.dp(
                                                EDGE_CONTENT_LAYOUT_PADDING_BELOW_MAIN_CONTENT_DP))
                                .build());
                innerContentBuilder.addContent(mSecondaryLabelText);
            }

            mainBoxBuilder.addContent(
                    new androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
                            .setModifiers(modifiers)
                            .setVerticalAlignment(
                                    androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                            .setHorizontalAlignment(
                                    androidx.wear.tiles.LayoutElementBuilders
                                            .HORIZONTAL_ALIGN_CENTER)
                            .setHeight(mainContentHeight)
                            .setWidth(mainContentWidth)
                            .addContent(innerContentBuilder.build())
                            .build());

            if (mEdgeContent != null) {
                mainBoxBuilder.addContent(mEdgeContent);
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
    public androidx.wear.tiles.LayoutElementBuilders.LayoutElement getContent() {
        if (!areElementsPresent(CONTENT_PRESENT)) {
            return null;
        }
        // By tag we know that content exists. It will be at position 0 if there is no primary
        // label, or at position 2 (primary label, spacer - content) otherwise.
        int contentPosition = areElementsPresent(PRIMARY_LABEL_PRESENT) ? 2 : 0;
        return ((androidx.wear.tiles.LayoutElementBuilders.Box) mInnerColumn.get(contentPosition))
                .getContents()
                .get(0);
    }

    /** Get the primary label content from this layout. */
    @Nullable
    public androidx.wear.tiles.LayoutElementBuilders.LayoutElement getPrimaryLabelTextContent() {
        if (!areElementsPresent(PRIMARY_LABEL_PRESENT)) {
            return null;
        }
        // By tag we know that primary label exists. It will always be at position 0.
        return mInnerColumn.get(0);
    }

    /** Get the secondary label content from this layout. */
    @Nullable
    public androidx.wear.tiles.LayoutElementBuilders.LayoutElement getSecondaryLabelTextContent() {
        if (!areElementsPresent(SECONDARY_LABEL_PRESENT)) {
            return null;
        }
        // By tag we know that secondary label exists. It will always be at last position.
        return mInnerColumn.get(mInnerColumn.size() - 1);
    }

    /** Returns the edge content from this layout. */
    @Nullable
    public androidx.wear.tiles.LayoutElementBuilders.LayoutElement getEdgeContent() {
        if (areElementsPresent(EDGE_CONTENT_PRESENT)) {
            return mContents.get(1);
        }
        return null;
    }

    /**
     * Returns EdgeContentLayout object from the given
     * androidx.wear.tiles.LayoutElementBuilders.LayoutElement (e.g. one retrieved from a
     * container's content with {@code container.getContents().get(index)}) if that element can be
     * converted to EdgeContentLayout. Otherwise, it will return null.
     */
    @Nullable
    public static EdgeContentLayout fromLayoutElement(
            @NonNull androidx.wear.tiles.LayoutElementBuilders.LayoutElement element) {
        if (element instanceof EdgeContentLayout) {
            return (EdgeContentLayout) element;
        }
        if (!(element instanceof androidx.wear.tiles.LayoutElementBuilders.Box)) {
            return null;
        }
        androidx.wear.tiles.LayoutElementBuilders.Box boxElement =
                (androidx.wear.tiles.LayoutElementBuilders.Box) element;
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
}
