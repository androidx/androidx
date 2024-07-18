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

import static androidx.wear.protolayout.DimensionBuilders.dp;
import static androidx.wear.protolayout.DimensionBuilders.expand;
import static androidx.wear.protolayout.material.ProgressIndicatorDefaults.DEFAULT_PADDING;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.EDGE_CONTENT_LAYOUT_MARGIN_HORIZONTAL_ROUND_DP;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.EDGE_CONTENT_LAYOUT_MARGIN_HORIZONTAL_SQUARE_DP;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.EDGE_CONTENT_LAYOUT_PADDING_ABOVE_MAIN_CONTENT_DP;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.EDGE_CONTENT_LAYOUT_PADDING_BELOW_MAIN_CONTENT_DP;
import static androidx.wear.protolayout.materialcore.Helper.checkNotNull;
import static androidx.wear.protolayout.materialcore.Helper.checkTag;
import static androidx.wear.protolayout.materialcore.Helper.getMetadataTagBytes;
import static androidx.wear.protolayout.materialcore.Helper.getTagBytes;
import static androidx.wear.protolayout.materialcore.Helper.isRoundDevice;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.protolayout.DimensionBuilders.DpProp;
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
 * added above and below the main content, respectively.
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
// TODO(b/274916652): Link visuals once they are available.
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

    /**
     * Bit position in a byte on {@link #FLAG_INDEX} index in metadata byte array to check whether
     * the edge content is added before the main content (0) or after it (1).
     */
    static final int EDGE_CONTENT_POSITION = 0x10;

    @RestrictTo(Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            flag = true,
            value = {
                EDGE_CONTENT_PRESENT,
                PRIMARY_LABEL_PRESENT,
                SECONDARY_LABEL_PRESENT,
                CONTENT_PRESENT,
                EDGE_CONTENT_POSITION
            })
    @interface ContentBits {}

    @NonNull private final Box mImpl;

    // This contains optional labels, spacers and main content.
    @NonNull private final List<LayoutElement> mInnerColumn;

    // This contains edge content;
    @Nullable private final LayoutElement mEdgeContent;
    private final boolean mIsEdgeContentBehind;

    EdgeContentLayout(@NonNull Box layoutElement) {
        this.mImpl = layoutElement;
        // This contains inner columns and edge content.
        List<LayoutElement> contents = mImpl.getContents();
        int edgeContentIndex = (getMetadataTag()[FLAG_INDEX] & EDGE_CONTENT_POSITION) == 0 ? 0 : 1;
        int contentIndex = 1 - edgeContentIndex;
        this.mInnerColumn =
                ((Column) ((Box) contents.get(contentIndex)).getContents().get(0)).getContents();
        this.mEdgeContent =
                areElementsPresent(EDGE_CONTENT_PRESENT) ? contents.get(edgeContentIndex) : null;
        mIsEdgeContentBehind = edgeContentIndex == 0;
    }

    /** Builder class for {@link EdgeContentLayout}. */
    public static final class Builder implements LayoutElement.Builder {
        @NonNull private final DeviceParameters mDeviceParameters;
        @Nullable private LayoutElement mEdgeContent = null;
        @Nullable private LayoutElement mPrimaryLabelText = null;
        @Nullable private LayoutElement mSecondaryLabelText = null;
        @Nullable private LayoutElement mContent = null;
        private byte mMetadataContentByte = 0;
        private boolean mIsEdgeContentBehind = false;

        /**
         * Creates a builder for the {@link EdgeContentLayout}t. Custom content inside of it can
         * later be set with ({@link #setContent}.
         */
        public Builder(@NonNull DeviceParameters deviceParameters) {
            this.mDeviceParameters = deviceParameters;
        }

        /**
         * Sets the content to be around the edges. This can be {@link CircularProgressIndicator}.
         */
        @NonNull
        public Builder setEdgeContent(@NonNull LayoutElement edgeContent) {
            this.mEdgeContent = edgeContent;
            mMetadataContentByte = (byte) (mMetadataContentByte | EDGE_CONTENT_PRESENT);
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
         * Sets the content in the secondary label slot which will be below the main content. It is
         * highly recommended to have primary label set when having secondary label.
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
         * Sets whether the edge content passed in with {@link #setEdgeContent} should be positioned
         * behind all other content in this layout or above it. If not set, defaults to {@code
         * false}, meaning that the edge content will be placed above all other content.
         */
        @NonNull
        public Builder setEdgeContentBehindAllOtherContent(boolean isBehind) {
            this.mIsEdgeContentBehind = isBehind;
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

            DpProp mainContentHeight = dp(Math.min(mainContentHeightDp, mainContentWidthDp));
            DpProp mainContentWidth = dp(Math.min(mainContentHeightDp, mainContentWidthDp));

            Modifiers modifiers =
                    new Modifiers.Builder()
                            .setPadding(
                                    new Padding.Builder()
                                            .setStart(dp(horizontalPaddingDp))
                                            .setEnd(dp(horizontalPaddingDp))
                                            .build())
                            .build();

            if (!mIsEdgeContentBehind) {
                // If the edge content is above the main one, then its index should be 1.
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
                            .setHeight(mainContentHeight)
                            .setWidth(mainContentWidth)
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
        // By tag we know that content exists. It will be at position 0 if there is no primary
        // label, or at position 2 (primary label, spacer - content) otherwise.
        int contentPosition = areElementsPresent(PRIMARY_LABEL_PRESENT) ? 2 : 0;
        return ((Box) mInnerColumn.get(contentPosition)).getContents().get(0);
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

    /** Returns the edge content from this layout. */
    @Nullable
    public LayoutElement getEdgeContent() {
        return mEdgeContent;
    }

    /** Returns if the edge content has been placed behind the other contents. */
    public boolean isEdgeContentBehindAllOtherContent() {
        return mIsEdgeContentBehind;
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
