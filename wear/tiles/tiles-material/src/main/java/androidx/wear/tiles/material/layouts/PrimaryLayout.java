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
import static androidx.wear.tiles.DimensionBuilders.wrap;
import static androidx.wear.tiles.material.ChipDefaults.COMPACT_HEIGHT;
import static androidx.wear.tiles.material.Helper.checkNotNull;
import static androidx.wear.tiles.material.Helper.isRoundDevice;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_BOTTOM_ROUND_PERCENT;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_BOTTOM_SQUARE_PERCENT;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_HORIZONTAL_ROUND_PERCENT;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_HORIZONTAL_SQUARE_PERCENT;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_TOP_ROUND_PERCENT;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_MARGIN_TOP_SQUARE_PERCENT;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PRIMARY_LAYOUT_SPACER_HEIGHT;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.tiles.DimensionBuilders.DpProp;
import androidx.wear.tiles.LayoutElementBuilders;
import androidx.wear.tiles.LayoutElementBuilders.Box;
import androidx.wear.tiles.LayoutElementBuilders.Column;
import androidx.wear.tiles.LayoutElementBuilders.Layout;
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement;
import androidx.wear.tiles.LayoutElementBuilders.Spacer;
import androidx.wear.tiles.ModifiersBuilders.Modifiers;
import androidx.wear.tiles.ModifiersBuilders.Padding;
import androidx.wear.tiles.TimelineBuilders.Timeline;
import androidx.wear.tiles.TimelineBuilders.TimelineEntry;
import androidx.wear.tiles.proto.LayoutElementProto;

/**
 * Tiles layout that represents a suggested layout style for Material Tiles with the primary
 * (compact) chip at the bottom with the given content in a center and the recommended margin and
 * padding applied.
 */
// TODO(b/215323986): Link visuals.
public class PrimaryLayout implements LayoutElement {
    @NonNull private final LayoutElement mElement;

    PrimaryLayout(@NonNull LayoutElement layoutElement) {
        this.mElement = layoutElement;
    }

    /** Builder class for {@link PrimaryLayout}. */
    public static final class Builder implements LayoutElement.Builder {

        @NonNull private final DeviceParameters mDeviceParameters;
        @Nullable private LayoutElement mPrimaryChip = null;
        @NonNull private LayoutElement mContent = new Box.Builder().build();

        /**
         * Creates a builder for the {@link PrimaryLayout} from the given content. Custom
         * content inside of it can later be set with ({@link #setContent}.
         */
        public Builder(@NonNull DeviceParameters deviceParameters) {
            this.mDeviceParameters = deviceParameters;
        }

        /** Sets the primary compact chip which will be at the bottom. */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        // There is no direct matching getter for this setter as the serialized format of the
        // ProtoLayouts do not allow for a direct reconstruction of the arguments. Instead there are
        // methods to get the contents a whole for rendering.
        public Builder setCompactChipContent(@NonNull LayoutElement compactChip) {
            this.mPrimaryChip = compactChip;
            return this;
        }

        /** Sets the additional content to this layout, above the primary chip. */
        @NonNull
        public Builder setContent(@NonNull LayoutElement content) {
            this.mContent = content;
            return this;
        }

        /** Constructs and returns {@link PrimaryLayout} with the provided content and look. */
        @NonNull
        @Override
        public PrimaryLayout build() {
            float topPadding =
                    mDeviceParameters.getScreenHeightDp()
                            * (isRoundDevice(mDeviceParameters)
                                    ? PRIMARY_LAYOUT_MARGIN_TOP_ROUND_PERCENT
                                    : PRIMARY_LAYOUT_MARGIN_TOP_SQUARE_PERCENT);
            float bottomPadding =
                    mPrimaryChip != null
                            ? (mDeviceParameters.getScreenHeightDp()
                                    * (isRoundDevice(mDeviceParameters)
                                            ? PRIMARY_LAYOUT_MARGIN_BOTTOM_ROUND_PERCENT
                                            : PRIMARY_LAYOUT_MARGIN_BOTTOM_SQUARE_PERCENT))
                            : topPadding;
            float horizontalPadding =
                    mDeviceParameters.getScreenWidthDp()
                            * (isRoundDevice(mDeviceParameters)
                                    ? PRIMARY_LAYOUT_MARGIN_HORIZONTAL_ROUND_PERCENT
                                    : PRIMARY_LAYOUT_MARGIN_HORIZONTAL_SQUARE_PERCENT);

            float primaryChipHeight =
                    mPrimaryChip != null
                            ? (COMPACT_HEIGHT.getValue()
                                    + PRIMARY_LAYOUT_SPACER_HEIGHT.getValue())
                            : 0;

            DpProp mainContentHeight =
                    dp(
                            mDeviceParameters.getScreenHeightDp()
                                    - primaryChipHeight
                                    - bottomPadding
                                    - topPadding);

            Modifiers modifiers =
                    new Modifiers.Builder()
                            .setPadding(
                                    new Padding.Builder()
                                            .setTop(dp(topPadding))
                                            .setBottom(dp(bottomPadding))
                                            .setStart(dp(horizontalPadding))
                                            .setEnd(dp(horizontalPadding))
                                            .build())
                            .build();

            Column.Builder columnBuilder =
                    new Column.Builder()
                            .setModifiers(modifiers)
                            .setWidth(expand())
                            .setHeight(expand())
                            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                            .addContent(
                                    new Box.Builder()
                                            .setVerticalAlignment(
                                                    LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                                            .setHeight(mainContentHeight)
                                            .setWidth(expand())
                                            .addContent(mContent)
                                            .build());

            if (mPrimaryChip != null) {
                columnBuilder
                        .addContent(
                                new Spacer.Builder()
                                        .setHeight(PRIMARY_LAYOUT_SPACER_HEIGHT)
                                        .build())
                        .addContent(
                                new Box.Builder()
                                        .setVerticalAlignment(
                                                LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM)
                                        .setHeight(wrap())
                                        .addContent(mPrimaryChip)
                                        .build());
            }

            LayoutElement.Builder element =
                    new Box.Builder()
                            .setWidth(expand())
                            .setHeight(expand())
                            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM)
                            .addContent(columnBuilder.build());

            return new PrimaryLayout(element.build());
        }
    }

    /** Returns the {@link Layout} object containing this layout template. */
    @NonNull
    public Layout toLayout() {
        return toLayoutBuilder().build();
    }

    /** Returns the {@link Layout.Builder} object containing this layout template. */
    @NonNull
    public Layout.Builder toLayoutBuilder() {
        return new Layout.Builder().setRoot(mElement);
    }

    /** Returns the {@link TimelineEntry.Builder} object containing this layout template. */
    @NonNull
    public TimelineEntry.Builder toTimelineEntryBuilder() {
        return new TimelineEntry.Builder().setLayout(toLayout());
    }

    /** Returns the {@link TimelineEntry} object containing this layout template. */
    @NonNull
    public TimelineEntry toTimelineEntry() {
        return toTimelineEntryBuilder().build();
    }

    /** Returns the {@link Timeline.Builder} object containing this layout template. */
    @NonNull
    public Timeline.Builder toTimelineBuilder() {
        return new Timeline.Builder().addTimelineEntry(toTimelineEntry());
    }

    /** Returns the {@link Timeline} object containing this layout template. */
    @NonNull
    public Timeline toTimeline() {
        return toTimelineBuilder().build();
    }

    @NonNull
    public LayoutElement getContent() {
        return checkNotNull(
                ((Box) ((Column) ((Box) mElement).getContents().get(0)).getContents().get(0))
                        .getContents()
                        .get(0));
    }

    /** @hide */
    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return mElement.toLayoutElementProto();
    }
}
