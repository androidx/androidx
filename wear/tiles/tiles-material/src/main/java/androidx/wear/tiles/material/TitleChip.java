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
import static androidx.wear.tiles.DimensionBuilders.dp;
import static androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER;
import static androidx.wear.tiles.material.ChipDefaults.TITLE_HEIGHT;
import static androidx.wear.tiles.material.ChipDefaults.TITLE_HORIZONTAL_PADDING;
import static androidx.wear.tiles.material.ChipDefaults.TITLE_PRIMARY;

import android.content.Context;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.tiles.DimensionBuilders.ContainerDimension;
import androidx.wear.tiles.DimensionBuilders.DpProp;
import androidx.wear.tiles.LayoutElementBuilders.HorizontalAlignment;
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement;
import androidx.wear.tiles.ModifiersBuilders.Clickable;
import androidx.wear.tiles.proto.LayoutElementProto;

/**
 * Tiles component {@link TitleChip} that represents clickable object with the text.
 *
 * <p>The Title Chip is Stadium shaped object with a larger height then standard Chip and it will
 * take one line of text of {@link Typography#TYPOGRAPHY_TITLE2} style.
 *
 * <p>The recommended set of {@link ChipColors} styles can be obtained from {@link ChipDefaults},
 * e.g. {@link ChipDefaults#TITLE_PRIMARY} to get a color scheme for a primary {@link TitleChip}.
 */
public class TitleChip implements LayoutElement {
    @NonNull private final Chip mElement;

    TitleChip(@NonNull Chip element) {
        this.mElement = element;
    }

    /** Builder class for {@link TitleChip}. */
    public static final class Builder implements LayoutElement.Builder {
        @NonNull private final Context mContext;
        @NonNull private final String mText;
        @NonNull private final Clickable mClickable;
        @NonNull private final DeviceParameters mDeviceParameters;
        @NonNull private ChipColors mChipColors = TITLE_PRIMARY;
        @HorizontalAlignment private int mHorizontalAlign = HORIZONTAL_ALIGN_CENTER;

        // Indicates that the width isn't set, so it will be automatically set by Chip.Builder
        // constructor.
        @Nullable private DpProp mWidth = null;

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
                @NonNull Clickable clickable,
                @NonNull DeviceParameters deviceParameters) {
            this.mContext = context;
            this.mText = text;
            this.mClickable = clickable;
            this.mDeviceParameters = deviceParameters;
        }

        // TODO(b/210846270): Add getChipColors.
        /**
         * Sets the colors for the {@link TitleChip}. If set, {@link
         * ChipColors#getBackgroundColor()} will be used for the background of the button and {@link
         * ChipColors#getContentColor()} for the text. If not set, {@link
         * ChipDefaults#TITLE_PRIMARY} will be used.
         */
        @NonNull
        public Builder setChipColors(@NonNull ChipColors chipColors) {
            mChipColors = chipColors;
            return this;
        }

        /** Sets the horizontal alignment in the chip. If not set, content will be centered. */
        @NonNull
        public Builder setHorizontalAlignment(@HorizontalAlignment int horizontalAlignment) {
            mHorizontalAlign = horizontalAlignment;
            return this;
        }

        /**
         * Sets the width of {@link TitleChip}. If not set, default value will be screen width
         * decreased by {@link ChipDefaults#DEFAULT_MARGIN_PERCENT}.
         */
        @NonNull
        public Builder setWidth(@NonNull DpProp width) {
            mWidth = width;
            return this;
        }

        /**
         * Sets the width of {@link TitleChip}. If not set, default value will be screen width
         * decreased by {@link ChipDefaults#DEFAULT_MARGIN_PERCENT}.
         */
        @NonNull
        public Builder setWidth(@Dimension(unit = DP) float width) {
            mWidth = dp(width);
            return this;
        }

        /** Constructs and returns {@link TitleChip} with the provided content and look. */
        @NonNull
        @Override
        public TitleChip build() {
            Chip.Builder chipBuilder =
                    new Chip.Builder(mContext, mClickable, mDeviceParameters)
                            .setChipColors(mChipColors)
                            .setContentDescription(mText)
                            .setHorizontalAlignment(mHorizontalAlign)
                            .setHeight(TITLE_HEIGHT)
                            .setMaxLines(1)
                            .setHorizontalPadding(TITLE_HORIZONTAL_PADDING)
                            .setPrimaryTextContent(mText)
                            .setPrimaryTextTypography(Typography.TYPOGRAPHY_TITLE2)
                            .setIsPrimaryTextScalable(false);

            if (mWidth != null) {
                chipBuilder.setWidth(mWidth);
            }

            return new TitleChip(chipBuilder.build());
        }
    }

    /** Returns height of this Chip. */
    @NonNull
    public ContainerDimension getHeight() {
        return mElement.getHeight();
    }

    /** Returns width of this Chip. */
    @NonNull
    public ContainerDimension getWidth() {
        return mElement.getWidth();
    }

    /** Returns click event action associated with this Chip. */
    @NonNull
    public Clickable getClickable() {
        return mElement.getClickable();
    }

    /** Returns chip color of this Chip. */
    @NonNull
    public ChipColors getChipColors() {
        return mElement.getChipColors();
    }

    /** Returns content description of this Chip. */
    @NonNull
    public String getContentDescription() {
        return mElement.getContentDescription();
    }

    /** Returns content of this Chip. */
    @NonNull
    public LayoutElement getContent() {
        return mElement.getContent();
    }

    /** Returns the horizontal alignment of the content in this Chip. */
    @HorizontalAlignment
    public int getHorizontalAlignment() {
        return mElement.getHorizontalAlignment();
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    @Override
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return mElement.toLayoutElementProto();
    }
}
