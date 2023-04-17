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

import static androidx.wear.tiles.material.Helper.checkNotNull;
import static androidx.wear.tiles.material.Helper.checkTag;
import static androidx.wear.tiles.material.Helper.getMetadataTagName;
import static androidx.wear.tiles.material.Helper.getTagBytes;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.MULTI_BUTTON_1_SIZE;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.MULTI_BUTTON_2_SIZE;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.MULTI_BUTTON_3_PLUS_SIZE;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.MULTI_BUTTON_MAX_NUMBER;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.MULTI_BUTTON_SPACER_HEIGHT;
import static androidx.wear.tiles.material.layouts.LayoutDefaults.MULTI_BUTTON_SPACER_WIDTH;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.proto.LayoutElementProto;
import androidx.wear.tiles.material.Button;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * Opinionated Tiles layout, that can contain between 1 and {@link
 * LayoutDefaults#MULTI_BUTTON_MAX_NUMBER} number of buttons arranged inline with the Material
 * guidelines. Can be used as a content passed in to the {@link PrimaryLayout}, but if there is
 * {@link LayoutDefaults#MULTI_BUTTON_MAX_NUMBER} buttons it should be used on its own.
 *
 * <p>For additional examples and suggested layouts see <a
 * href="/training/wearables/design/tiles-design-system">Tiles Design System</a>.
 *
 * <p>When accessing the contents of a container for testing, note that this element can't be simply
 * casted back to the original type, i.e.:
 *
 * <pre>{@code
 * MultiButtonLayout mbl = new MultiButtonLayout...
 * Box box = new Box.Builder().addContent(mbl).build();
 *
 * MultiButtonLayout myMbl = (MultiButtonLayout) box.getContents().get(0);
 * }</pre>
 *
 * will fail.
 *
 * <p>To be able to get {@link MultiButtonLayout} object from any layout element, {@link
 * #fromLayoutElement} method should be used, i.e.:
 *
 * <pre>{@code
 * MultiButtonLayout myMbl = MultiButtonLayout.fromLayoutElement(box.getContents().get(0));
 * }</pre>
 *
 * @deprecated Use the new class {@link
 *     androidx.wear.protolayout.material.layouts.MultiButtonLayout} which provides the same API and
 *     functionality.
 */
@Deprecated
@SuppressWarnings("deprecation")
public class MultiButtonLayout implements androidx.wear.tiles.LayoutElementBuilders.LayoutElement {
    /**
     * Tool tag for Metadata in androidx.wear.tiles.ModifiersBuilders.Modifiers, so we know that
     * androidx.wear.tiles.LayoutElementBuilders.Box is actually a MultiButtonLayout.
     */
    static final String METADATA_TAG = "MBL";

    /** Button distribution where the first row has more buttons than other rows. */
    public static final int FIVE_BUTTON_DISTRIBUTION_TOP_HEAVY = 1;

    /** Button distribution where the last row has more buttons than other rows. */
    public static final int FIVE_BUTTON_DISTRIBUTION_BOTTOM_HEAVY = 2;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FIVE_BUTTON_DISTRIBUTION_TOP_HEAVY, FIVE_BUTTON_DISTRIBUTION_BOTTOM_HEAVY})
    public @interface ButtonDistribution {}

    @NonNull private final androidx.wear.tiles.LayoutElementBuilders.Box mElement;

    MultiButtonLayout(@NonNull androidx.wear.tiles.LayoutElementBuilders.Box mElement) {
        this.mElement = mElement;
    }

    /** Builder class for {@link MultiButtonLayout}. */
    public static final class Builder
            implements androidx.wear.tiles.LayoutElementBuilders.LayoutElement.Builder {
        @NonNull
        private final List<androidx.wear.tiles.LayoutElementBuilders.LayoutElement>
                mButtonsContent = new ArrayList<>();

        private @ButtonDistribution int mFiveButtonDistribution =
                FIVE_BUTTON_DISTRIBUTION_BOTTOM_HEAVY;

        /**
         * Creates a builder for the {@link MultiButtonLayout}. Content inside of it can later be
         * added with {@link #addButtonContent}.
         */
        public Builder() {}

        /**
         * Add one new button to the layout. Note that it is accepted to pass in any {@link
         * androidx.wear.tiles.LayoutElementBuilders.LayoutElement}, but it is strongly recommended
         * to add a {@link Button} as the layout is optimized for it. Any button added after {@link
         * LayoutDefaults#MULTI_BUTTON_MAX_NUMBER} is reached will be discarded.
         */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        // There is no direct matching getter for this setter, but there is a getter that gets all
        // added buttons.
        public Builder addButtonContent(
                @NonNull androidx.wear.tiles.LayoutElementBuilders.LayoutElement buttonContent) {
            mButtonsContent.add(buttonContent);
            return this;
        }

        /**
         * Sets the button distribution for this layout. Button distribution is used in case when
         * there is 5 buttons in the layout to determine whether the 3 buttons row is at the top or
         * bottom.
         */
        @NonNull
        public Builder setFiveButtonDistribution(@ButtonDistribution int fiveButtonDistribution) {
            this.mFiveButtonDistribution = fiveButtonDistribution;
            return this;
        }

        /** Constructs and returns {@link MultiButtonLayout} with the provided content and look. */
        @NonNull
        @Override
        public MultiButtonLayout build() {
            int buttonNum = mButtonsContent.size();
            if (buttonNum > MULTI_BUTTON_MAX_NUMBER) {
                throw new IllegalArgumentException(
                        "Too many buttons are added. Maximum number is "
                                + MULTI_BUTTON_MAX_NUMBER
                                + ".");
            }

            androidx.wear.tiles.LayoutElementBuilders.LayoutElement buttons =
                    buildButtons(buttonNum);
            androidx.wear.tiles.LayoutElementBuilders.Box.Builder elementBuilder =
                    new androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
                            .setModifiers(
                                    new androidx.wear.tiles.ModifiersBuilders.Modifiers.Builder()
                                            .setMetadata(
                                                    new androidx.wear.tiles.ModifiersBuilders
                                                                    .ElementMetadata.Builder()
                                                            .setTagData(getTagBytes(METADATA_TAG))
                                                            .build())
                                            .build())
                            .addContent(buttons);

            return new MultiButtonLayout(elementBuilder.build());
        }

        @NonNull
        private androidx.wear.tiles.LayoutElementBuilders.LayoutElement buildButtons(
                int buttonNum) {
            switch (buttonNum) {
                case 1:
                    return wrapButton(mButtonsContent.get(0), MULTI_BUTTON_1_SIZE);
                case 2:
                    return build2ButtonRow(
                            mButtonsContent.get(0), mButtonsContent.get(1), MULTI_BUTTON_2_SIZE);
                case 3:
                    return build3ButtonRow(
                            mButtonsContent.get(0), mButtonsContent.get(1), mButtonsContent.get(2));
                case 4:
                    return new androidx.wear.tiles.LayoutElementBuilders.Column.Builder()
                            .addContent(
                                    build2ButtonRow(
                                            mButtonsContent.get(0),
                                            mButtonsContent.get(1),
                                            MULTI_BUTTON_3_PLUS_SIZE))
                            .addContent(buildVerticalSpacer())
                            .addContent(
                                    build2ButtonRow(
                                            mButtonsContent.get(2),
                                            mButtonsContent.get(3),
                                            MULTI_BUTTON_3_PLUS_SIZE))
                            .build();
                case 5:
                    return new androidx.wear.tiles.LayoutElementBuilders.Column.Builder()
                            .addContent(
                                    mFiveButtonDistribution == FIVE_BUTTON_DISTRIBUTION_TOP_HEAVY
                                            ? build3ButtonRow(
                                                    mButtonsContent.get(0),
                                                    mButtonsContent.get(1),
                                                    mButtonsContent.get(2))
                                            : build2ButtonRow(
                                                    mButtonsContent.get(0),
                                                    mButtonsContent.get(1),
                                                    MULTI_BUTTON_3_PLUS_SIZE))
                            .addContent(buildVerticalSpacer())
                            .addContent(
                                    mFiveButtonDistribution == FIVE_BUTTON_DISTRIBUTION_TOP_HEAVY
                                            ? build2ButtonRow(
                                                    mButtonsContent.get(3),
                                                    mButtonsContent.get(4),
                                                    MULTI_BUTTON_3_PLUS_SIZE)
                                            : build3ButtonRow(
                                                    mButtonsContent.get(2),
                                                    mButtonsContent.get(3),
                                                    mButtonsContent.get(4)))
                            .build();
                case 6:
                    return new androidx.wear.tiles.LayoutElementBuilders.Column.Builder()
                            .addContent(
                                    build3ButtonRow(
                                            mButtonsContent.get(0),
                                            mButtonsContent.get(1),
                                            mButtonsContent.get(2)))
                            .addContent(buildVerticalSpacer())
                            .addContent(
                                    build3ButtonRow(
                                            mButtonsContent.get(3),
                                            mButtonsContent.get(4),
                                            mButtonsContent.get(5)))
                            .build();
                case 7:
                    return new androidx.wear.tiles.LayoutElementBuilders.Column.Builder()
                            .addContent(
                                    build2ButtonRow(
                                            mButtonsContent.get(0),
                                            mButtonsContent.get(1),
                                            MULTI_BUTTON_3_PLUS_SIZE))
                            .addContent(buildVerticalSpacer())
                            .addContent(
                                    build3ButtonRow(
                                            mButtonsContent.get(2),
                                            mButtonsContent.get(3),
                                            mButtonsContent.get(4)))
                            .addContent(buildVerticalSpacer())
                            .addContent(
                                    build2ButtonRow(
                                            mButtonsContent.get(5),
                                            mButtonsContent.get(6),
                                            MULTI_BUTTON_3_PLUS_SIZE))
                            .build();
            }
            // This shouldn't happen, but return an empty
            // androidx.wear.tiles.LayoutElementBuilders.Box instead of having this method nullable
            // and checks above.
            return new androidx.wear.tiles.LayoutElementBuilders.Box.Builder().build();
        }

        @NonNull
        private androidx.wear.tiles.LayoutElementBuilders.Row build3ButtonRow(
                @NonNull androidx.wear.tiles.LayoutElementBuilders.LayoutElement button1,
                @NonNull androidx.wear.tiles.LayoutElementBuilders.LayoutElement button2,
                @NonNull androidx.wear.tiles.LayoutElementBuilders.LayoutElement button3) {
            return new androidx.wear.tiles.LayoutElementBuilders.Row.Builder()
                    .setWidth(androidx.wear.tiles.DimensionBuilders.wrap())
                    .setHeight(androidx.wear.tiles.DimensionBuilders.wrap())
                    .addContent(wrapButton(button1, MULTI_BUTTON_3_PLUS_SIZE))
                    .addContent(buildHorizontalSpacer())
                    .addContent(wrapButton(button2, MULTI_BUTTON_3_PLUS_SIZE))
                    .addContent(buildHorizontalSpacer())
                    .addContent(wrapButton(button3, MULTI_BUTTON_3_PLUS_SIZE))
                    .build();
        }

        @NonNull
        private androidx.wear.tiles.LayoutElementBuilders.Row build2ButtonRow(
                @NonNull androidx.wear.tiles.LayoutElementBuilders.LayoutElement button1,
                @NonNull androidx.wear.tiles.LayoutElementBuilders.LayoutElement button2,
                @NonNull androidx.wear.tiles.DimensionBuilders.DpProp size) {
            return new androidx.wear.tiles.LayoutElementBuilders.Row.Builder()
                    .setWidth(androidx.wear.tiles.DimensionBuilders.wrap())
                    .setHeight(androidx.wear.tiles.DimensionBuilders.wrap())
                    .addContent(wrapButton(button1, size))
                    .addContent(buildHorizontalSpacer())
                    .addContent(wrapButton(button2, size))
                    .build();
        }

        @NonNull
        private androidx.wear.tiles.LayoutElementBuilders.Spacer buildHorizontalSpacer() {
            return new androidx.wear.tiles.LayoutElementBuilders.Spacer.Builder()
                    .setWidth(MULTI_BUTTON_SPACER_WIDTH)
                    .build();
        }

        @NonNull
        private androidx.wear.tiles.LayoutElementBuilders.Spacer buildVerticalSpacer() {
            return new androidx.wear.tiles.LayoutElementBuilders.Spacer.Builder()
                    .setHeight(MULTI_BUTTON_SPACER_HEIGHT)
                    .build();
        }

        @NonNull
        private androidx.wear.tiles.LayoutElementBuilders.Box wrapButton(
                @NonNull androidx.wear.tiles.LayoutElementBuilders.LayoutElement button,
                @NonNull androidx.wear.tiles.DimensionBuilders.DpProp size) {
            return new androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
                    .setWidth(size)
                    .setHeight(size)
                    .addContent(button)
                    .build();
        }
    }

    /** Gets the content from this layout, containing all buttons that were added. */
    @NonNull
    public List<androidx.wear.tiles.LayoutElementBuilders.LayoutElement> getButtonContents() {
        List<androidx.wear.tiles.LayoutElementBuilders.LayoutElement> buttons = new ArrayList<>();
        List<androidx.wear.tiles.LayoutElementBuilders.LayoutElement> contents =
                mElement.getContents();
        if (contents.isEmpty()) {
            return buttons;
        }
        androidx.wear.tiles.LayoutElementBuilders.LayoutElement innerContent = contents.get(0);
        if (innerContent instanceof androidx.wear.tiles.LayoutElementBuilders.Column) {
            for (androidx.wear.tiles.LayoutElementBuilders.LayoutElement row :
                    ((androidx.wear.tiles.LayoutElementBuilders.Column) innerContent)
                            .getContents()) {
                if (row instanceof androidx.wear.tiles.LayoutElementBuilders.Row) {
                    buttons.addAll(
                            getButtonsFromRow((androidx.wear.tiles.LayoutElementBuilders.Row) row));
                }
            }
        } else if (innerContent instanceof androidx.wear.tiles.LayoutElementBuilders.Row) {
            return getButtonsFromRow((androidx.wear.tiles.LayoutElementBuilders.Row) innerContent);
        } else if (innerContent instanceof androidx.wear.tiles.LayoutElementBuilders.Box) {
            buttons.add(
                    ((androidx.wear.tiles.LayoutElementBuilders.Box) innerContent)
                            .getContents()
                            .get(0));
        }

        return buttons;
    }

    /** Returns metadata tag set to this MultiButtonLayouts. */
    @NonNull
    String getMetadataTag() {
        return getMetadataTagName(
                checkNotNull(checkNotNull(mElement.getModifiers()).getMetadata()));
    }

    /**
     * Gets the button distribution from this layout for the case when there is 5 buttons in the
     * layout. If there is more or less buttons than 5, default {@link
     * #FIVE_BUTTON_DISTRIBUTION_BOTTOM_HEAVY} will be returned.
     */
    public int getFiveButtonDistribution() {
        List<androidx.wear.tiles.LayoutElementBuilders.LayoutElement> contents =
                mElement.getContents();
        if (getButtonContents().size() != 5) {
            return FIVE_BUTTON_DISTRIBUTION_BOTTOM_HEAVY;
        }
        androidx.wear.tiles.LayoutElementBuilders.LayoutElement innerContent = contents.get(0);
        if (innerContent instanceof androidx.wear.tiles.LayoutElementBuilders.Column
                && ((androidx.wear.tiles.LayoutElementBuilders.Column) innerContent)
                                .getContents()
                                .size()
                        == 3) {
            // 1st and 3rd row are buttons. Check whether the first row has 5 (3 buttons + 2 spacer)
            // - top heavy or 3 (2 buttons + spacer) - bottom heavy elements.
            androidx.wear.tiles.LayoutElementBuilders.LayoutElement firstElement =
                    ((androidx.wear.tiles.LayoutElementBuilders.Column) innerContent)
                            .getContents()
                            .get(0);
            if (firstElement instanceof androidx.wear.tiles.LayoutElementBuilders.Row
                    && ((androidx.wear.tiles.LayoutElementBuilders.Row) firstElement)
                                    .getContents()
                                    .size()
                            == 5) {
                return FIVE_BUTTON_DISTRIBUTION_TOP_HEAVY;
            }
        }
        return FIVE_BUTTON_DISTRIBUTION_BOTTOM_HEAVY;
    }

    private List<androidx.wear.tiles.LayoutElementBuilders.LayoutElement> getButtonsFromRow(
            androidx.wear.tiles.LayoutElementBuilders.Row row) {
        List<androidx.wear.tiles.LayoutElementBuilders.LayoutElement> buttons = new ArrayList<>();
        for (androidx.wear.tiles.LayoutElementBuilders.LayoutElement element : row.getContents()) {
            if (element instanceof androidx.wear.tiles.LayoutElementBuilders.Box) {
                buttons.add(
                        ((androidx.wear.tiles.LayoutElementBuilders.Box) element)
                                .getContents()
                                .get(0));
            }
        }
        return buttons;
    }

    /**
     * Returns MultiButtonLayout object from the given
     * androidx.wear.tiles.LayoutElementBuilders.LayoutElement (e.g. one retrieved from a
     * container's content with {@code container.getContents().get(index)}) if that element can be
     * converted to MultiButtonLayout. Otherwise, it will return null.
     */
    @Nullable
    public static MultiButtonLayout fromLayoutElement(
            @NonNull androidx.wear.tiles.LayoutElementBuilders.LayoutElement element) {
        if (element instanceof MultiButtonLayout) {
            return (MultiButtonLayout) element;
        }
        if (!(element instanceof androidx.wear.tiles.LayoutElementBuilders.Box)) {
            return null;
        }
        androidx.wear.tiles.LayoutElementBuilders.Box boxElement =
                (androidx.wear.tiles.LayoutElementBuilders.Box) element;
        if (!checkTag(boxElement.getModifiers(), METADATA_TAG)) {
            return null;
        }
        // Now we are sure that this element is a MultiButtonLayout.
        return new MultiButtonLayout(boxElement);
    }

    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return mElement.toLayoutElementProto();
    }
}
