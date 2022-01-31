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
import static androidx.wear.tiles.material.ProgressIndicatorDefaults.DEFAULT_PADDING;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PROGRESS_INDICATOR_LAYOUT_MARGIN_HORIZONTAL_ROUND_DP;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PROGRESS_INDICATOR_LAYOUT_MARGIN_HORIZONTAL_SQUARE_DP;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.DeviceParametersBuilders;
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.tiles.DimensionBuilders.DpProp;
import androidx.wear.tiles.LayoutElementBuilders;
import androidx.wear.tiles.LayoutElementBuilders.Box;
import androidx.wear.tiles.LayoutElementBuilders.Layout;
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement;
import androidx.wear.tiles.ModifiersBuilders.Modifiers;
import androidx.wear.tiles.ModifiersBuilders.Padding;
import androidx.wear.tiles.TimelineBuilders.Timeline;
import androidx.wear.tiles.TimelineBuilders.TimelineEntry;
import androidx.wear.tiles.material.CircularProgressIndicator;
import androidx.wear.tiles.proto.LayoutElementProto;

/**
 * Tiles layout that represents the suggested layout style for Material Tiles with the progress
 * indicator around the edges of the screen and the given content inside of it and the recommended
 * margin and padding applied.
 */
// TODO(b/215323986)
public class ProgressIndicatorLayout implements LayoutElement {
    @NonNull private final LayoutElement mElement;

    ProgressIndicatorLayout(@NonNull LayoutElement layoutElement) {
        this.mElement = layoutElement;
    }

    /** Builder class for {@link ProgressIndicatorLayout}. */
    public static final class Builder implements LayoutElement.Builder {
        @NonNull private final DeviceParameters mDeviceParameters;
        @Nullable private LayoutElement mProgressIndicator = null;
        @Nullable private LayoutElement mContent = null;

        /**
         * Creates a builder for the {@link ProgressIndicatorLayout}t. Custom content inside of it
         * can later be set with ({@link #setContent}.
         */
        public Builder(@NonNull DeviceParameters deviceParameters) {
            this.mDeviceParameters = deviceParameters;
        }

        /** Sets the progress indicator which will be around the edges. */
        @NonNull
        // There is no direct matching getter for this setter as the serialized format of the
        // ProtoLayouts do not allow for a direct reconstruction of the arguments. Instead there are
        // methods to get the contents a whole for rendering.
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public Builder setProgressIndicatorContent(@NonNull LayoutElement progressIndicator) {
            this.mProgressIndicator = progressIndicator;
            return this;
        }

        /** Sets the additional content to this layout, inside of the screen. */
        @NonNull
        public Builder setContent(@NonNull LayoutElement content) {
            this.mContent = content;
            return this;
        }

        private boolean isRoundDevice() {
            return mDeviceParameters.getScreenShape()
                    == DeviceParametersBuilders.SCREEN_SHAPE_ROUND;
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
                    isRoundDevice()
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

            Box.Builder boxBuilder =
                    new Box.Builder()
                            .setWidth(expand())
                            .setHeight(expand())
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

    /** Get the inner content from this layout. */
    @NonNull
    public LayoutElement getContent() {
        return checkNotNull(((Box) ((Box) mElement).getContents().get(0))).getContents().get(0);
    }

    /** @hide */
    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return mElement.toLayoutElementProto();
    }
}
