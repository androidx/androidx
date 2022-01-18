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

import static androidx.wear.tiles.material.ChipDefaults.COMPACT_HEIGHT;
import static androidx.wear.tiles.material.ChipDefaults.COMPACT_HORIZONTAL_PADDING;
import static androidx.wear.tiles.material.ChipDefaults.COMPACT_PRIMARY;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.ActionBuilders.Action;
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.tiles.DimensionBuilders.ContainerDimension;
import androidx.wear.tiles.DimensionBuilders.WrappedDimensionProp;
import androidx.wear.tiles.LayoutElementBuilders.FontStyles;
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement;
import androidx.wear.tiles.proto.LayoutElementProto;

/**
 * Tiles component {@link CompactChip} that represents clickable object with the text.
 *
 * <p>The Chip is Stadium shape and has a max height designed to take no more than one line of text
 * of {@link FontStyles#caption1} style. Width of the chip is adjustable to the text size.
 *
 * <p>The recommended set of {@link ChipColors} styles can be obtained from {@link ChipDefaults}.,
 * e.g. {@link ChipDefaults#COMPACT_PRIMARY} to get a color scheme for a primary {@link CompactChip}
 * which by default will have a solid background of {@link Colors#PRIMARY} and text color of {@link
 * Colors#ON_PRIMARY}.
 */
public class CompactChip implements LayoutElement {
    @NonNull private final Chip mElement;

    CompactChip(@NonNull Chip element) {
        this.mElement = element;
    }

    /** Builder class for {@link androidx.wear.tiles.material.CompactChip}. */
    public static final class Builder implements LayoutElement.Builder {
        @NonNull private final String mText;
        @NonNull private final Action mAction;
        @NonNull private final String mClickableId;
        @NonNull private final DeviceParameters mDeviceParameters;
        @NonNull private ChipColors mChipColors = COMPACT_PRIMARY;

        /**
         * Creates a builder for the {@link CompactChip} with associated action and the given text
         *
         * @param text The text to be displayed in this compact chip. It shouldn't contain more than
         *     9 characters. Any extra characters will be deleted.
         * @param action Associated Actions for click events. When the CompactChip is clicked it
         *     will fire the associated action.
         * @param clickableId The ID associated with the given action's clickable.
         * @param deviceParameters The device parameters used for styling text.
         */
        @SuppressWarnings("LambdaLast")
        public Builder(
                @NonNull String text,
                @NonNull Action action,
                @NonNull String clickableId,
                @NonNull DeviceParameters deviceParameters) {
            this.mText = text.substring(0, Math.min(text.length(), 9));
            this.mAction = action;
            this.mClickableId = clickableId;
            this.mDeviceParameters = deviceParameters;
        }

        /**
         * Sets the colors for the {@link CompactChip}. If set, {@link
         * ChipColors#getBackgroundColor()} will be used for the background of the button and {@link
         * ChipColors#getBackgroundColor()} for the text. If not set, {@link
         * ChipDefaults#COMPACT_PRIMARY} will be used.
         */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public Builder setChipColors(@NonNull ChipColors chipColors) {
            mChipColors = chipColors;
            return this;
        }

        /** Constructs and returns {@link CompactChip} with the provided content and look. */
        @NonNull
        @Override
        public CompactChip build() {
            Chip.Builder chipBuilder =
                    new Chip.Builder(mAction, mClickableId, mDeviceParameters)
                            .setChipColors(mChipColors)
                            .setContentDescription(mText)
                            .setLeftAlign(false) // centered
                            .setWidth(new WrappedDimensionProp.Builder().build())
                            .setHeight(COMPACT_HEIGHT)
                            .setHorizontalPadding(COMPACT_HORIZONTAL_PADDING)
                            .setPrimaryTextContent(mText)
                            .setPrimaryTextFontStyle(
                                    FontStyles.caption1(mDeviceParameters).build());

            return new CompactChip(chipBuilder.build());
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
