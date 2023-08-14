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

import static androidx.annotation.Dimension.DP;
import static androidx.wear.protolayout.DimensionBuilders.dp;
import static androidx.wear.protolayout.DimensionBuilders.wrap;
import static androidx.wear.protolayout.material.layouts.LayoutDefaults.MULTI_SLOT_LAYOUT_HORIZONTAL_SPACER_WIDTH;
import static androidx.wear.protolayout.materialcore.Helper.checkNotNull;
import static androidx.wear.protolayout.materialcore.Helper.checkTag;
import static androidx.wear.protolayout.materialcore.Helper.getMetadataTagName;
import static androidx.wear.protolayout.materialcore.Helper.getTagBytes;

import android.annotation.SuppressLint;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.DimensionBuilders.DpProp;
import androidx.wear.protolayout.DimensionBuilders.SpacerDimension;
import androidx.wear.protolayout.LayoutElementBuilders;
import androidx.wear.protolayout.LayoutElementBuilders.Box;
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement;
import androidx.wear.protolayout.LayoutElementBuilders.Row;
import androidx.wear.protolayout.LayoutElementBuilders.Spacer;
import androidx.wear.protolayout.ModifiersBuilders.ElementMetadata;
import androidx.wear.protolayout.ModifiersBuilders.Modifiers;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.proto.LayoutElementProto;

import java.util.ArrayList;
import java.util.List;

/**
 * Opinionated ProtoLayout layout, row like style with horizontally aligned and spaced slots (for
 * icons or other small content). Should be used as a content passed in to the {@link
 * PrimaryLayout}.
 *
 * <p>Recommended number of added slots is 1 to 3. Their width will be the width of an element
 * passed in, with the {@link LayoutDefaults#MULTI_SLOT_LAYOUT_HORIZONTAL_SPACER_WIDTH} space
 * between.
 *
 * <p>When accessing the contents of a container for testing, note that this element can't be simply
 * casted back to the original type, i.e.:
 *
 * <pre>{@code
 * MultiSlotLayout msl = new MultiSlotLayout...
 * Box box = new Box.Builder().addContent(msl).build();
 *
 * MultiSlotLayout myMsl = (MultiSlotLayout) box.getContents().get(0);
 * }</pre>
 *
 * will fail.
 *
 * <p>To be able to get {@link MultiSlotLayout} object from any layout element, {@link
 * #fromLayoutElement} method should be used, i.e.:
 *
 * <pre>{@code
 * MultiSlotLayout myMsl = MultiSlotLayout.fromLayoutElement(box.getContents().get(0));
 * }</pre>
 */
// TODO(b/274916652): Link visuals once they are available.
public class MultiSlotLayout implements LayoutElement {
    /** Tool tag for Metadata in Modifiers, so we know that Row is actually a MultiSlotLayout. */
    static final String METADATA_TAG = "MSL";

    @NonNull private final Row mElement;

    MultiSlotLayout(@NonNull Row mElement) {
        this.mElement = mElement;
    }

    /** Builder class for {@link MultiSlotLayout}. */
    public static final class Builder implements LayoutElement.Builder {

        @NonNull private final List<LayoutElement> mSlotsContent = new ArrayList<>();
        @NonNull private DpProp mHorizontalSpacerWidth = MULTI_SLOT_LAYOUT_HORIZONTAL_SPACER_WIDTH;

        /**
         * Creates a builder for the {@link MultiSlotLayout}. Content inside of it can later be
         * added with {@link #addSlotContent}.
         */
        public Builder() {}

        /** Add one new slot to the layout with the given content inside. */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        // There is no direct matching getter for this setter, but there is a getter that gets all
        // added slots.
        public Builder addSlotContent(@NonNull LayoutElement slotContent) {
            mSlotsContent.add(slotContent);
            return this;
        }

        /**
         * Sets the horizontal spacer width which is used as a space between slots if there is more
         * than one slot. If not set, {@link
         * LayoutDefaults#MULTI_SLOT_LAYOUT_HORIZONTAL_SPACER_WIDTH} will be used.
         */
        @NonNull
        public Builder setHorizontalSpacerWidth(@Dimension(unit = DP) float width) {
            this.mHorizontalSpacerWidth = dp(width);
            return this;
        }

        /** Constructs and returns {@link MultiSlotLayout} with the provided content and look. */
        @NonNull
        @Override
        // The @Dimension(unit = DP) on mVerticalSpacerHeight.getValue() is seemingly being ignored,
        // so lint complains that we're passing PX to something expecting DP. Just suppress the
        // warning for now.
        @SuppressLint("ResourceType")
        public MultiSlotLayout build() {
            Row.Builder rowBuilder =
                    new Row.Builder()
                            .setHeight(wrap())
                            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                            .setWidth(wrap())
                            .setModifiers(
                                    new Modifiers.Builder()
                                            .setMetadata(
                                                    new ElementMetadata.Builder()
                                                            .setTagData(getTagBytes(METADATA_TAG))
                                                            .build())
                                            .build());
            if (!mSlotsContent.isEmpty()) {

                boolean isFirst = true;
                for (LayoutElement slot : mSlotsContent) {
                    if (!isFirst) {
                        rowBuilder.addContent(
                                new Spacer.Builder().setWidth(mHorizontalSpacerWidth).build());
                    } else {
                        isFirst = false;
                    }
                    rowBuilder.addContent(
                            new Box.Builder()
                                    .setWidth(wrap())
                                    .setHeight(wrap())
                                    .addContent(slot)
                                    .build());
                }
            }

            return new MultiSlotLayout(rowBuilder.build());
        }
    }

    /** Gets the content from this layout, containing all slots that were added. */
    @NonNull
    public List<LayoutElement> getSlotContents() {
        List<LayoutElement> slots = new ArrayList<>();
        for (LayoutElement slot : mElement.getContents()) {
            if (slot instanceof Box) {
                slots.add(((Box) slot).getContents().get(0));
            }
        }
        return slots;
    }

    /** Gets the width of horizontal spacer that is between slots. */
    // The @Dimension(unit = DP) on getLinearDimension.getValue() is seemingly being ignored, so
    // lint complains that we're passing PX to something expecting DP. Just suppress the warning for
    // now.
    @SuppressLint("ResourceType")
    @Dimension(unit = DP)
    public float getHorizontalSpacerWidth() {
        for (LayoutElement slot : mElement.getContents()) {
            if (slot instanceof Spacer) {
                SpacerDimension width = ((Spacer) slot).getWidth();
                if (width instanceof DpProp) {
                    return ((DpProp) width).getValue();
                }
            }
        }
        return LayoutDefaults.MULTI_SLOT_LAYOUT_HORIZONTAL_SPACER_WIDTH.getValue();
    }

    /** Returns metadata tag set to this MultiSlotLayout. */
    @NonNull
    String getMetadataTag() {
        return getMetadataTagName(
                checkNotNull(checkNotNull(mElement.getModifiers()).getMetadata()));
    }

    /**
     * Returns MultiSlotLayout object from the given LayoutElement (e.g. one retrieved from a
     * container's content with {@code container.getContents().get(index)}) if that element can be
     * converted to MultiSlotLayout. Otherwise, it will return null.
     */
    @Nullable
    public static MultiSlotLayout fromLayoutElement(@NonNull LayoutElement element) {
        if (element instanceof MultiSlotLayout) {
            return (MultiSlotLayout) element;
        }
        if (!(element instanceof Row)) {
            return null;
        }
        Row rowElement = (Row) element;
        if (!checkTag(rowElement.getModifiers(), METADATA_TAG)) {
            return null;
        }
        // Now we are sure that this element is a MultiSlotLayout.
        return new MultiSlotLayout(rowElement);
    }

    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return mElement.toLayoutElementProto();
    }

    @Nullable
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public Fingerprint getFingerprint() {
        return mElement.getFingerprint();
    }
}
