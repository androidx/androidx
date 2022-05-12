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
import static androidx.wear.tiles.material.Helper.isRoundDevice;
import static androidx.wear.tiles.material.ProgressIndicatorDefaults.DEFAULT_PADDING;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PROGRESS_INDICATOR_LAYOUT_MARGIN_HORIZONTAL_ROUND_DP;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.PROGRESS_INDICATOR_LAYOUT_MARGIN_HORIZONTAL_SQUARE_DP;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.tiles.DimensionBuilders.DpProp;
import androidx.wear.tiles.LayoutElementBuilders;
import androidx.wear.tiles.LayoutElementBuilders.Box;
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement;
import androidx.wear.tiles.ModifiersBuilders.Modifiers;
import androidx.wear.tiles.ModifiersBuilders.Padding;
import androidx.wear.tiles.material.CircularProgressIndicator;
import androidx.wear.tiles.proto.LayoutElementProto;

import java.util.List;

/**
 * Tiles layout that represents the suggested layout style for Material Tiles with the progress
 * indicator around the edges of the screen and the given content inside of it and the recommended
 * margin and padding applied.
 */
// TODO(b/215323986): Link visuals.
public class ProgressIndicatorLayout implements LayoutElement {
    @NonNull private final Box mElement;

    ProgressIndicatorLayout(@NonNull Box layoutElement) {
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

    /** Returns the inner content from this layout. */
    @Nullable
    public LayoutElement getContent() {
        List<LayoutElement> contents = mElement.getContents();
        if (contents.size() > 0) {
            // If content exists, it will always be the first one in the list. If that element is
            // not Box, than this layout only has indicator, so we'll return null.
            LayoutElement element = contents.get(0);
            if (element instanceof Box) {
                return checkNotNull(((Box) element).getContents().get(0));
            }
        }
        return null;
    }

    /** Returns the progress indicator content from this layout. */
    @Nullable
    public LayoutElement getProgressIndicatorContent() {
        List<LayoutElement> contents = mElement.getContents();
        int size = contents.size();
        if (size > 0) {
            // If progress indicator exists, it will always be the last one in the list and not
            // wrapped in the Box.
            LayoutElement element = contents.get(size - 1);
            if (!(element instanceof Box)) {
                return element;
            }
        }
        return null;
    }

    /** @hide */
    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return mElement.toLayoutElementProto();
    }
}
