/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.tiles.material;

import static androidx.annotation.Dimension.DP;
import static androidx.wear.tiles.material.ChipDefaults.TITLE_HEIGHT;
import static androidx.wear.tiles.material.ChipDefaults.TITLE_HORIZONTAL_PADDING;
import static androidx.wear.tiles.material.ChipDefaults.TITLE_PRIMARY_COLORS;
import static androidx.wear.tiles.material.Helper.checkNotNull;
import static androidx.wear.tiles.material.Helper.checkTag;

import android.content.Context;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.proto.LayoutElementProto;

/**
 * Tiles component {@link TitleChip} that represents clickable object with the text.
 *
 * <p>The Title Chip is Stadium shaped object with a larger height then standard Chip and it will
 * take one line of text of {@link Typography#TYPOGRAPHY_TITLE2} style.
 *
 * <p>The recommended set of {@link ChipColors} styles can be obtained from {@link ChipDefaults},
 * e.g. {@link ChipDefaults#TITLE_PRIMARY_COLORS} to get a color scheme for a primary {@link
 * TitleChip}.
 *
 * <p>When accessing the contents of a container for testing, note that this element can't be simply
 * casted back to the original type, i.e.:
 *
 * <pre>{@code
 * TitleChip chip = new TitleChip...
 * Box box = new Box.Builder().addContent(chip).build();
 *
 * TitleChip myChip = (TitleChip) box.getContents().get(0);
 * }</pre>
 *
 * will fail.
 *
 * <p>To be able to get {@link TitleChip} object from any layout element, {@link #fromLayoutElement}
 * method should be used, i.e.:
 *
 * <pre>{@code
 * TitleChip myChip = TitleChip.fromLayoutElement(box.getContents().get(0));
 * }</pre>
 *
 * @see androidx.wear.tiles.material.layouts.PrimaryLayout.Builder#setContent if this TitleChip is
 *     used inside of {@link androidx.wear.tiles.material.layouts.PrimaryLayout}.
 * @deprecated Use the new class {@link androidx.wear.protolayout.material.TitleChip} which provides
 *     the same API and functionality.
 */
@Deprecated
@SuppressWarnings("deprecation")
public class TitleChip implements androidx.wear.tiles.LayoutElementBuilders.LayoutElement {
    /**
     * Tool tag for Metadata in androidx.wear.tiles.ModifiersBuilders.Modifiers, so we know that
     * androidx.wear.tiles.LayoutElementBuilders.Box is actually a TitleChip.
     */
    static final String METADATA_TAG = "TTLCHP";

    @NonNull private final Chip mElement;

    TitleChip(@NonNull Chip element) {
        this.mElement = element;
    }

    /** Builder class for {@link TitleChip}. */
    public static final class Builder
            implements androidx.wear.tiles.LayoutElementBuilders.LayoutElement.Builder {
        @NonNull private final Context mContext;
        @NonNull private final String mText;
        @NonNull private final androidx.wear.tiles.ModifiersBuilders.Clickable mClickable;

        @NonNull
        private final androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
                mDeviceParameters;

        @NonNull private ChipColors mChipColors = TITLE_PRIMARY_COLORS;

        @androidx.wear.tiles.LayoutElementBuilders.HorizontalAlignment
        private int mHorizontalAlign =
                androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER;

        // Indicates that the width isn't set, so it will be automatically set by Chip.Builder
        // constructor.
        @Nullable private androidx.wear.tiles.DimensionBuilders.ContainerDimension mWidth = null;

        /**
         * Creates a builder for the {@link TitleChip} with associated action and the given text
         *
         * @param context The application's context.
         * @param text The text to be displayed in this title chip. Text will be displayed in 1 line
         *     and truncated if it doesn't fit.
         * @param clickable Associated {@link Clickable} for click events. When the TitleChip is
         *     clicked it will fire the associated action.
         * @param deviceParameters The device parameters used for styling text.
         */
        public Builder(
                @NonNull Context context,
                @NonNull String text,
                @NonNull androidx.wear.tiles.ModifiersBuilders.Clickable clickable,
                @NonNull
                        androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
                                deviceParameters) {
            this.mContext = context;
            this.mText = text;
            this.mClickable = clickable;
            this.mDeviceParameters = deviceParameters;
        }

        /**
         * Sets the colors for the {@link TitleChip}. If set, {@link
         * ChipColors#getBackgroundColor()} will be used for the background of the button and {@link
         * ChipColors#getContentColor()} for the text. If not set, {@link
         * ChipDefaults#TITLE_PRIMARY_COLORS} will be used.
         */
        @NonNull
        public Builder setChipColors(@NonNull ChipColors chipColors) {
            mChipColors = chipColors;
            return this;
        }

        /** Sets the horizontal alignment in the chip. If not set, content will be centered. */
        @NonNull
        public Builder setHorizontalAlignment(
                @androidx.wear.tiles.LayoutElementBuilders.HorizontalAlignment
                        int horizontalAlignment) {
            mHorizontalAlign = horizontalAlignment;
            return this;
        }

        /**
         * Sets the width of {@link TitleChip}. If not set, default value will be set to fill the
         * screen.
         */
        @NonNull
        public Builder setWidth(
                @NonNull androidx.wear.tiles.DimensionBuilders.ContainerDimension width) {
            mWidth = width;
            return this;
        }

        /**
         * Sets the width of {@link TitleChip}. If not set, default value will be set to fill the
         * screen.
         */
        @NonNull
        public Builder setWidth(@Dimension(unit = DP) float width) {
            mWidth = androidx.wear.tiles.DimensionBuilders.dp(width);
            return this;
        }

        /** Constructs and returns {@link TitleChip} with the provided content and look. */
        @NonNull
        @Override
        public TitleChip build() {
            Chip.Builder chipBuilder =
                    new Chip.Builder(mContext, mClickable, mDeviceParameters)
                            .setMetadataTag(METADATA_TAG)
                            .setChipColors(mChipColors)
                            .setContentDescription(mText)
                            .setHorizontalAlignment(mHorizontalAlign)
                            .setHeight(TITLE_HEIGHT)
                            .setMaxLines(1)
                            .setHorizontalPadding(TITLE_HORIZONTAL_PADDING)
                            .setPrimaryLabelContent(mText)
                            .setPrimaryLabelTypography(Typography.TYPOGRAPHY_TITLE2)
                            .setIsPrimaryLabelScalable(false);

            if (mWidth != null) {
                chipBuilder.setWidth(mWidth);
            }

            return new TitleChip(chipBuilder.build());
        }
    }

    /** Returns width of this Chip. */
    @NonNull
    public androidx.wear.tiles.DimensionBuilders.ContainerDimension getWidth() {
        return mElement.getWidth();
    }

    /** Returns click event action associated with this Chip. */
    @NonNull
    public androidx.wear.tiles.ModifiersBuilders.Clickable getClickable() {
        return mElement.getClickable();
    }

    /** Returns chip color of this Chip. */
    @NonNull
    public ChipColors getChipColors() {
        return mElement.getChipColors();
    }

    /** Returns text content of this Chip. */
    @NonNull
    public String getText() {
        return checkNotNull(mElement.getPrimaryLabelContent());
    }

    /** Returns the horizontal alignment of the content in this Chip. */
    @androidx.wear.tiles.LayoutElementBuilders.HorizontalAlignment
    public int getHorizontalAlignment() {
        return mElement.getHorizontalAlignment();
    }

    /** Returns metadata tag set to this TitleChip, which should be {@link #METADATA_TAG}. */
    @NonNull
    String getMetadataTag() {
        return mElement.getMetadataTag();
    }

    /**
     * Returns TitleChip object from the given
     * androidx.wear.tiles.LayoutElementBuilders.LayoutElement (e.g. one retrieved from a
     * container's content with {@code container.getContents().get(index)}) if that element can be
     * converted to TitleChip. Otherwise, it will return null.
     */
    @Nullable
    public static TitleChip fromLayoutElement(
            @NonNull androidx.wear.tiles.LayoutElementBuilders.LayoutElement element) {
        if (element instanceof TitleChip) {
            return (TitleChip) element;
        }
        if (!(element instanceof androidx.wear.tiles.LayoutElementBuilders.Box)) {
            return null;
        }
        androidx.wear.tiles.LayoutElementBuilders.Box boxElement =
                (androidx.wear.tiles.LayoutElementBuilders.Box) element;
        if (!checkTag(boxElement.getModifiers(), METADATA_TAG)) {
            return null;
        }
        // Now we are sure that this element is a TitleChip.
        return new TitleChip(new Chip(boxElement));
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    @Override
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return mElement.toLayoutElementProto();
    }
}
