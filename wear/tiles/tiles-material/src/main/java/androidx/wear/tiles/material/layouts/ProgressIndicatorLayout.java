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

import static androidx.wear.tiles.DimensionBuilders.dp;
import static androidx.wear.tiles.DimensionBuilders.expand;
import static androidx.wear.tiles.material.Helper.checkNotNull;
import static androidx.wear.tiles.material.Helper.checkTag;
import static androidx.wear.tiles.material.Helper.getMetadataTagBytes;
import static androidx.wear.tiles.material.Helper.getTagBytes;
import static androidx.wear.tiles.material.Helper.isRoundDevice;
import static androidx.wear.tiles.material.ProgressIndicatorDefaults.DEFAULT_PADDING;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PROGRESS_INDICATOR_LAYOUT_MARGIN_HORIZONTAL_ROUND_DP;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PROGRESS_INDICATOR_LAYOUT_MARGIN_HORIZONTAL_SQUARE_DP;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.tiles.DimensionBuilders.DpProp;
import androidx.wear.tiles.LayoutElementBuilders;
import androidx.wear.tiles.LayoutElementBuilders.Box;
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement;
import androidx.wear.tiles.ModifiersBuilders.ElementMetadata;
import androidx.wear.tiles.ModifiersBuilders.Modifiers;
import androidx.wear.tiles.ModifiersBuilders.Padding;
import androidx.wear.tiles.material.CircularProgressIndicator;
import androidx.wear.tiles.proto.LayoutElementProto;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

/**
 * Tiles layout that represents the suggested layout style for Material Tiles with the progress
 * indicator around the edges of the screen and the given content inside of it and the recommended
 * margin and padding applied.
 *
 * <p>For additional examples and suggested layouts see <a
 * href="/training/wearables/design/tiles-design-system">Tiles Design System</a>.
 */
public class ProgressIndicatorLayout implements LayoutElement {
    /**
     * Prefix tool tag for Metadata in Modifiers, so we know that Box is actually a
     * ProgressIndicatorLayout.
     */
    static final String METADATA_TAG_PREFIX = "PIL_";

    /**
     * Index for byte array that contains bits to check whether the content and indicator are
     * present or not.
     */
    static final int FLAG_INDEX = METADATA_TAG_PREFIX.length();

    /**
     * Base tool tag for Metadata in Modifiers, so we know that Box is actually a
     * ProgressIndicatorLayout and what optional content is added.
     */
    static final byte[] METADATA_TAG_BASE =
            Arrays.copyOf(getTagBytes(METADATA_TAG_PREFIX), FLAG_INDEX + 1);

    /**
     * Bit position in a byte on {@link #FLAG_INDEX} index in metadata byte array to check whether
     * the progress indicator is present or not.
     */
    static final int PROGRESS_INDICATOR_PRESENT = 0x1;
    /**
     * Bit position in a byte on {@link #FLAG_INDEX} index in metadata byte array to check whether
     * the main content is present or not.
     */
    static final int CONTENT_PRESENT = 0x2;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            flag = true,
            value = {PROGRESS_INDICATOR_PRESENT, CONTENT_PRESENT})
    @interface ContentBits {}

    @NonNull private final Box mImpl;
    @NonNull private final List<LayoutElement> mContents;

    ProgressIndicatorLayout(@NonNull Box layoutElement) {
        this.mImpl = layoutElement;
        this.mContents = mImpl.getContents();
    }

    /** Builder class for {@link ProgressIndicatorLayout}. */
    public static final class Builder implements LayoutElement.Builder {
        @NonNull private final DeviceParameters mDeviceParameters;
        @Nullable private LayoutElement mProgressIndicator = null;
        @Nullable private LayoutElement mContent = null;
        private byte mMetadataContentByte = 0;

        /**
         * Creates a builder for the {@link ProgressIndicatorLayout}t. Custom content inside of it
         * can later be set with ({@link #setContent}.
         */
        public Builder(@NonNull DeviceParameters deviceParameters) {
            this.mDeviceParameters = deviceParameters;
        }

        /** Sets the progress indicator which will be around the edges. */
        @NonNull
        public Builder setProgressIndicatorContent(@NonNull LayoutElement progressIndicator) {
            this.mProgressIndicator = progressIndicator;
            mMetadataContentByte = (byte) (mMetadataContentByte | PROGRESS_INDICATOR_PRESENT);
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
         * Constructs and returns {@link ProgressIndicatorLayout} with the provided content and
         * look.
         */
        @NonNull
        @Override
        public ProgressIndicatorLayout build() {
            float thicknessDp =
                    mProgressIndicator instanceof CircularProgressIndicator
                            ? ((CircularProgressIndicator) mProgressIndicator)
                                    .getStrokeWidth()
                                    .getValue()
                            : 0;
            float horizontalPaddingDp =
                    isRoundDevice(mDeviceParameters)
                            ? PROGRESS_INDICATOR_LAYOUT_MARGIN_HORIZONTAL_ROUND_DP
                            : PROGRESS_INDICATOR_LAYOUT_MARGIN_HORIZONTAL_SQUARE_DP;
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

            byte[] metadata = METADATA_TAG_BASE.clone();
            metadata[FLAG_INDEX] = mMetadataContentByte;
            Box.Builder boxBuilder =
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

            if (mContent != null) {
                boxBuilder.addContent(
                        new Box.Builder()
                                .setModifiers(modifiers)
                                .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                                .setHorizontalAlignment(
                                        LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                                .setHeight(mainContentHeight)
                                .setWidth(mainContentWidth)
                                .addContent(mContent)
                                .build());
            }

            if (mProgressIndicator != null) {
                boxBuilder.addContent(mProgressIndicator);
            }

            return new ProgressIndicatorLayout(boxBuilder.build());
        }
    }

    private boolean isElementPresent(@ContentBits int elementBitPosition) {
        return (getMetadataTag()[FLAG_INDEX] & elementBitPosition) == elementBitPosition;
    }

    /** Returns metadata tag set to this ProgressIndicatorLayout. */
    @NonNull
    byte[] getMetadataTag() {
        return getMetadataTagBytes(checkNotNull(mImpl.getModifiers()));
    }

    /** Returns the inner content from this layout. */
    @Nullable
    public LayoutElement getContent() {
        if (isElementPresent(CONTENT_PRESENT)) {
            return ((Box) mContents.get(0)).getContents().get(0);
        }
        return null;
    }

    /** Returns the progress indicator content from this layout. */
    @Nullable
    public LayoutElement getProgressIndicatorContent() {
        if (isElementPresent(PROGRESS_INDICATOR_PRESENT)) {
            return mContents.get(isElementPresent(CONTENT_PRESENT) ? 1 : 0);
        }
        return null;
    }

    /**
     * Returns ProgressIndicatorLayout object from the given LayoutElement if that element can be
     * converted to ProgressIndicatorLayout. Otherwise, returns null.
     */
    @Nullable
    public static ProgressIndicatorLayout fromLayoutElement(@NonNull LayoutElement element) {
        if (!(element instanceof Box)) {
            return null;
        }
        Box boxElement = (Box) element;
        if (!checkTag(boxElement.getModifiers(), METADATA_TAG_PREFIX, METADATA_TAG_BASE)) {
            return null;
        }
        // Now we are sure that this element is a ProgressIndicatorLayout.
        return new ProgressIndicatorLayout(boxElement);
    }

    /** @hide */
    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return mImpl.toLayoutElementProto();
    }
}
