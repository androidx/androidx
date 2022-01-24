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
import static androidx.wear.tiles.material.ChipDefaults.LARGE_HEIGHT;
import static androidx.wear.tiles.material.ChipDefaults.LARGE_HORIZONTAL_PADDING;
import static androidx.wear.tiles.material.ChipDefaults.LARGE_PRIMARY;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.ActionBuilders.Action;
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.tiles.DimensionBuilders.ContainerDimension;
import androidx.wear.tiles.DimensionBuilders.DpProp;
import androidx.wear.tiles.LayoutElementBuilders.FontStyles;
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement;
import androidx.wear.tiles.proto.LayoutElementProto;

/**
 * Tiles component {@link TitleChip} that represents clickable object with the text.
 *
 * <p>The Large Chip is Stadium shaped object with a larger height then standard Chip and it will
 * take one line of text of {@link FontStyles#title2} style.
 *
 * <p>The recommended set of {@link ChipColors} styles can be obtained from {@link ChipDefaults},
 * e.g. {@link ChipDefaults#LARGE_PRIMARY} to get a color scheme for a primary {@link TitleChip}
 * which by default will have a solid background of {@link Colors#PRIMARY} and text color of {@link
 * Colors#ON_PRIMARY}.
 */
public class TitleChip implements LayoutElement {
    @NonNull private final Chip mElement;

    TitleChip(@NonNull Chip element) {
        this.mElement = element;
    }

    /** Builder class for {@link TitleChip}. */
    public static final class Builder implements LayoutElement.Builder {
        @NonNull private final String mText;
        @NonNull private final Action mAction;
        @NonNull private final String mClickableId;
        @NonNull private final DeviceParameters mDeviceParameters;
        @NonNull private ChipColors mChipColors = LARGE_PRIMARY;
        private boolean mIsLeftAligned = false;

        // Indicates that the width isn't set, so it will be automatically set by Chip.Builder
        // constructor.
        @Nullable private DpProp mWidth = null;

        /**
         * Creates a builder for the {@link TitleChip} with associated action and the given text
         *
         * @param text The text to be displayed in this large chip. Text will be displayed in 1 line
         *     and truncated if it doesn't fit.
         * @param action Associated Actions for click events. When the LargeChip is clicked it will
         *     fire the associated action.
         * @param clickableId The ID associated with the given action's clickable.
         * @param deviceParameters The device parameters used for styling text.
         */
        @SuppressWarnings("LambdaLast")
        public Builder(
                @NonNull String text,
                @NonNull Action action,
                @NonNull String clickableId,
                @NonNull DeviceParameters deviceParameters) {
            this.mText = text;
            this.mAction = action;
            this.mClickableId = clickableId;
            this.mDeviceParameters = deviceParameters;
        }

        // TODO(b/210846270): Add getChipColors.
        /**
         * Sets the colors for the {@link TitleChip}. If set, {@link
         * ChipColors#getBackgroundColor()} will be used for the background of the button and {@link
         * ChipColors#getContentColor()} for the text. If not set, {@link
         * ChipDefaults#LARGE_PRIMARY} will be used.
         */
        @NonNull
        public Builder setChipColors(@NonNull ChipColors chipColors) {
            mChipColors = chipColors;
            return this;
        }

        // TODO(b/210847875): Add isLeftAligned()
        /**
         * Sets content to be left-aligned in the chip. If {@code false} is passed as parameter,
         * text will be center-aligned.
         */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public Builder setLeftAlign(boolean isLeftAlign) {
            mIsLeftAligned = isLeftAlign;
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
        public Builder setWidth(@Dimension(unit = DP) int width) {
            mWidth = dp(width);
            return this;
        }

        /** Constructs and returns {@link TitleChip} with the provided content and look. */
        @NonNull
        @Override
        public TitleChip build() {
            Chip.Builder chipBuilder =
                    new Chip.Builder(mAction, mClickableId, mDeviceParameters)
                            .setChipColors(mChipColors)
                            .setContentDescription(mText)
                            .setLeftAlign(mIsLeftAligned)
                            .setHeight(LARGE_HEIGHT)
                            .setMaxLines(1)
                            .setHorizontalPadding(LARGE_HORIZONTAL_PADDING)
                            .setPrimaryTextContent(mText)
                            .setPrimaryTextFontStyle(FontStyles.title2(mDeviceParameters).build());

            if (mWidth != null) {
                chipBuilder.setWidth(mWidth);
            }

            return new TitleChip(chipBuilder.build());
        }
    }

    /** Returns height of this Chip. Intended for testing purposes only. */
    @NonNull
    public ContainerDimension getHeight() {
        return mElement.getHeight();
    }

    /** Returns width of this Chip. Intended for testing purposes only. */
    @NonNull
    public ContainerDimension getWidth() {
        return mElement.getWidth();
    }

    /** Returns click event action associated with this Chip. Intended for testing purposes only. */
    @NonNull
    public Action getAction() {
        return mElement.getAction();
    }

    /** Returns chip color of this Chip. Intended for testing purposes only. */
    @NonNull
    public ChipColors getChipColors() {
        return mElement.getChipColors();
    }

    /** Returns content description of this Chip. Intended for testing purposes only. */
    @NonNull
    public String getContentDescription() {
        return mElement.getContentDescription();
    }

    /** Returns content of this Chip. Intended for testing purposes only. */
    @NonNull
    public LayoutElement getContent() {
        return mElement.getContent();
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    @Override
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return mElement.toLayoutElementProto();
    }
}
