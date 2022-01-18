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
import static androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_LEFT;
import static androidx.wear.tiles.material.ChipDefaults.DEFAULT_HEIGHT;
import static androidx.wear.tiles.material.ChipDefaults.DEFAULT_MARGIN_PERCENT;
import static androidx.wear.tiles.material.ChipDefaults.HORIZONTAL_PADDING;
import static androidx.wear.tiles.material.ChipDefaults.ICON_SIZE;
import static androidx.wear.tiles.material.ChipDefaults.PRIMARY;
import static androidx.wear.tiles.material.ChipDefaults.VERTICAL_PADDING;
import static androidx.wear.tiles.material.Helper.checkNotNull;
import static androidx.wear.tiles.material.Helper.radiusOf;

import androidx.annotation.Dimension;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.ActionBuilders.Action;
import androidx.wear.tiles.ColorBuilders.ColorProp;
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.tiles.DimensionBuilders.ContainerDimension;
import androidx.wear.tiles.DimensionBuilders.DpProp;
import androidx.wear.tiles.LayoutElementBuilders;
import androidx.wear.tiles.LayoutElementBuilders.Box;
import androidx.wear.tiles.LayoutElementBuilders.ColorFilter;
import androidx.wear.tiles.LayoutElementBuilders.Column;
import androidx.wear.tiles.LayoutElementBuilders.FontStyle;
import androidx.wear.tiles.LayoutElementBuilders.FontStyles;
import androidx.wear.tiles.LayoutElementBuilders.HorizontalAlignment;
import androidx.wear.tiles.LayoutElementBuilders.Image;
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement;
import androidx.wear.tiles.LayoutElementBuilders.Row;
import androidx.wear.tiles.LayoutElementBuilders.Spacer;
import androidx.wear.tiles.LayoutElementBuilders.Text;
import androidx.wear.tiles.ModifiersBuilders;
import androidx.wear.tiles.ModifiersBuilders.Background;
import androidx.wear.tiles.ModifiersBuilders.Clickable;
import androidx.wear.tiles.ModifiersBuilders.Corner;
import androidx.wear.tiles.ModifiersBuilders.Modifiers;
import androidx.wear.tiles.ModifiersBuilders.Padding;
import androidx.wear.tiles.proto.LayoutElementProto;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Tiles component {@link Chip} that represents clickable object with the text, optional label and
 * optional icon or with custom content.
 *
 * <p>The Chip is Stadium shape and has a max height designed to take no more than two lines of text
 * of {@link FontStyles#button} style. The {@link Chip} can have an icon horizontally parallel to
 * the two lines of text. Width of chip can very, and the recommended size is screen dependent with
 * the recommended margin being defined in {@link ChipDefaults#DEFAULT_MARGIN_PERCENT} which is set
 * by default.
 *
 * <p>The recommended set of {@link ChipColors} styles can be obtained from {@link ChipDefaults}.,
 * e.g. {@link ChipDefaults#PRIMARY} to get a color scheme for a primary {@link Chip} which by
 * default will have a solid background of {@link Colors#PRIMARY} and content color of {@link
 * Colors#ON_PRIMARY}.
 */
public class Chip implements LayoutElement {
    @NonNull private final Box mElement;

    Chip(@NonNull Box element) {
        mElement = element;
    }

    /** Builder class for {@link androidx.wear.tiles.material.Chip}. */
    public static final class Builder implements LayoutElement.Builder {
        private static final int NOT_SET = 0;
        private static final int TEXT = 1;
        private static final int ICON = 2;
        private static final int CUSTOM_CONTENT = 3;
        private int mMaxLines = 0; // 0 indicates that is not set.

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({NOT_SET, TEXT, ICON, CUSTOM_CONTENT})
        @interface ChipType {}

        @Nullable private LayoutElement mCustomContent;
        @NonNull private String mResourceId = "";
        @NonNull private String mPrimaryText = "";
        @Nullable private String mLabelText = null;
        @NonNull private final Action mAction;
        @NonNull private final String mClickableId;
        @NonNull private final DeviceParameters mDeviceParameters;
        @NonNull private String mContentDescription = "";
        @NonNull private ContainerDimension mWidth;
        @NonNull private DpProp mHeight = DEFAULT_HEIGHT;
        @NonNull private ChipColors mChipColors = PRIMARY;
        private @ChipType int mType = NOT_SET;
        private boolean mIsLeftAligned = false;
        @NonNull private FontStyle mPrimaryTextFont;
        @NonNull private DpProp mHorizontalPadding = HORIZONTAL_PADDING;
        @NonNull private DpProp mVerticalPadding = VERTICAL_PADDING;

        /**
         * Creates a builder for the {@link Chip} with associated action. It is required to add
         * content later with setters.
         *
         * @param action Associated Actions for click events. When the Chip is clicked it will fire
         *     the associated action.
         * @param clickableId The ID associated with the given action's clickable.
         * @param deviceParameters The device parameters used for styling text.
         */
        @SuppressWarnings("LambdaLast")
        public Builder(
                @NonNull Action action,
                @NonNull String clickableId,
                @NonNull DeviceParameters deviceParameters) {
            mAction = action;
            mClickableId = clickableId;
            mDeviceParameters = deviceParameters;
            mWidth =
                    dp(
                            (100 - 2 * DEFAULT_MARGIN_PERCENT)
                                    * deviceParameters.getScreenWidthDp()
                                    / 100);
            mPrimaryTextFont = FontStyles.button(deviceParameters).build();
        }

        /**
         * Sets the width of {@link Chip}. If not set, default value will be screen width decreased
         * by {@link ChipDefaults#DEFAULT_MARGIN_PERCENT}.
         */
        @NonNull
        public Builder setWidth(@NonNull DpProp width) {
            mWidth = width;
            return this;
        }

        /**
         * Sets the width of {@link Chip}. If not set, default value will be screen width decreased
         * by {@link ChipDefaults#DEFAULT_MARGIN_PERCENT}.
         */
        @NonNull
        public Builder setWidth(@Dimension(unit = DP) int width) {
            mWidth = dp(width);
            return this;
        }

        /**
         * Sets the custom content for the {@link Chip}. Any previously added content will be
         * overridden.
         */
        @NonNull
        public Builder setContent(@NonNull LayoutElement content) {
            this.mCustomContent = content;
            this.mType = CUSTOM_CONTENT;
            return this;
        }

        /**
         * Sets the content description for the {@link Chip}. It is highly recommended to provide
         * this for chip containing icon.
         */
        @NonNull
        public Builder setContentDescription(@NonNull String contentDescription) {
            this.mContentDescription = contentDescription;
            return this;
        }

        /**
         * Sets the content of the {@link Chip} to be the given primary text. Any previously added
         * content will be overridden. Primary text can be on 1 or 2 lines, depending on the length.
         */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public Builder setPrimaryTextContent(@NonNull String primaryText) {
            this.mPrimaryText = primaryText;
            this.mLabelText = null;
            this.mType = TEXT;
            return this;
        }

        /**
         * Used for creating CompactChip and LargeChip.
         *
         * <p>Sets the content of the {@link Chip} to be the given primary text. Any previously
         * added content will be overridden. Primary text can be on 1 or 2 lines, depending on the
         * length.
         */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        Builder setPrimaryTextFontStyle(@NonNull FontStyle fontStyle) {
            this.mPrimaryTextFont = fontStyle;
            return this;
        }

        /**
         * Sets the content of the {@link Chip} to be the given primary text and secondary label.
         * Any previously added content will be overridden. Primary text can be shown on 1 line
         * only. This content will be left aligned inside the chip.
         */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public Builder setPrimaryTextLabelContent(
                @NonNull String primaryText, @NonNull String label) {
            this.mPrimaryText = primaryText;
            this.mLabelText = label;
            this.mType = TEXT;
            return this;
        }

        /**
         * Sets the content of the {@link Chip} to be the given primary text with an icon and
         * secondary label. Any previously added content will be overridden. Provided icon will be
         * tinted to the given content color from {@link ChipColors}. This icon should be image with
         * chosen alpha channel and not an actual image. This content will be left-aligned inside
         * the chip.
         */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public Builder setPrimaryTextIconContent(
                @NonNull String primaryText, @NonNull String resourceId) {
            this.mPrimaryText = primaryText;
            this.mResourceId = resourceId;
            this.mLabelText = null;
            this.mType = ICON;
            return this;
        }

        /**
         * Sets the content of the {@link Chip} to be the given primary text with an icon. Any
         * previously added content will be overridden. Provided icon will be tinted to the given
         * content color from {@link ChipColors}. This icon should be image with chosen alpha
         * channel and not an actual image. Primary text can be shown on 1 line only. This content
         * will be left-aligned inside the chip.
         */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public Builder setPrimaryTextLabelIconContent(
                @NonNull String primaryText, @NonNull String label, @NonNull String resourceId) {
            this.mPrimaryText = primaryText;
            this.mLabelText = label;
            this.mResourceId = resourceId;
            this.mType = ICON;
            return this;
        }

        // TODO(b/210846270): Add getChipColors.
        /**
         * Sets the colors for the {@link Chip}. If set, {@link ChipColors#getBackgroundColor()}
         * will be used for the background of the button, {@link ChipColors#getContentColor()} for
         * main text, {@link ChipColors#getSecondaryContentColor()} for label text and {@link
         * ChipColors#getIconTintColor()} will be used as tint color for the icon itself. If not
         * set, {@link ChipDefaults#PRIMARY} will be used.
         */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public Builder setChipColors(@NonNull ChipColors chipColors) {
            mChipColors = chipColors;
            return this;
        }

        // TODO(b/207350548): In RTL mode, should icon still be on the left.
        // TODO(b/210847875): Add isLeftAligned()
        /**
         * Sets content to be left-aligned in the chip in cases where the custom content is set or
         * in case where there is only primary text without label or an icon. If label or icon is
         * added, left align will be automatically applied. If {@code false} is passed as parameter,
         * custom content or primary text (in case when there's no icon or label) will be
         * center-aligned.
         */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public Builder setLeftAlign(boolean isLeftAlign) {
            mIsLeftAligned = isLeftAlign;
            return this;
        }

        /** Used for creating CompactChip and LargeChip. */
        @NonNull
        Builder setHorizontalPadding(@NonNull DpProp horizontalPadding) {
            this.mHorizontalPadding = horizontalPadding;
            return this;
        }

        /** Used for creating CompactChip and LargeChip. */
        @NonNull
        Builder setVerticalPadding(@NonNull DpProp verticalPadding) {
            this.mVerticalPadding = verticalPadding;
            return this;
        }

        /** Used for creating CompactChip and LargeChip. */
        @NonNull
        Builder setHeight(@NonNull DpProp height) {
            this.mHeight = height;
            return this;
        }

        /** Used for creating CompactChip and LargeChip. */
        @NonNull
        Builder setWidth(@NonNull ContainerDimension width) {
            this.mWidth = width;
            return this;
        }

        /** Used for creating CompactChip and LargeChip. */
        @NonNull
        Builder setMaxLines(int maxLines) {
            this.mMaxLines = maxLines;
            return this;
        }

        private @HorizontalAlignment int getCorrectAlignment() {
            if (mType == CUSTOM_CONTENT) {
                return mIsLeftAligned ? HORIZONTAL_ALIGN_LEFT : HORIZONTAL_ALIGN_CENTER;
            }
            if (!mIsLeftAligned && mLabelText == null && mType == TEXT) {
                return HORIZONTAL_ALIGN_CENTER;
            }
            return HORIZONTAL_ALIGN_LEFT;
        }

        /** Constructs and returns {@link Chip} with the provided content and look. */
        @NonNull
        @Override
        public Chip build() {
            Modifiers.Builder modifiers =
                    new Modifiers.Builder()
                            .setClickable(
                                    new Clickable.Builder()
                                            .setId(mClickableId)
                                            .setOnClick(mAction)
                                            .build())
                            .setPadding(
                                    new Padding.Builder()
                                            .setStart(mHorizontalPadding)
                                            .setEnd(mHorizontalPadding)
                                            .setBottom(mVerticalPadding)
                                            .setTop(mVerticalPadding)
                                            .build())
                            .setBackground(
                                    new Background.Builder()
                                            .setColor(mChipColors.getBackgroundColor())
                                            .setCorner(
                                                    new Corner.Builder()
                                                            .setRadius(radiusOf(mHeight))
                                                            .build())
                                            .build());
            if (!mContentDescription.isEmpty()) {
                modifiers.setSemantics(
                        new ModifiersBuilders.Semantics.Builder()
                                .setContentDescription(mContentDescription)
                                .build());
            }

            Box.Builder element =
                    new Box.Builder()
                            .setWidth(mWidth)
                            .setHeight(mHeight)
                            .setHorizontalAlignment(getCorrectAlignment())
                            .addContent(getCorrectContent())
                            .setModifiers(modifiers.build());

            return new Chip(element.build());
        }

        @NonNull
        private LayoutElement getCorrectContent() {
            if (mType == NOT_SET) {
                throw new IllegalStateException(
                        "No content set. Use setPrimaryTextContent or similar method to add"
                            + " content");
            }
            if (mType == CUSTOM_CONTENT) {
                return checkNotNull(mCustomContent);
            }
            Text mainTextElement =
                    new Text.Builder()
                            .setText(mPrimaryText)
                            .setFontStyle(customizeFontStyle())
                            .setMaxLines(getCorrectMaxLines())
                            .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_TRUNCATE)
                            .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_START)
                            .build();

            // Placeholder for text.
            Column.Builder column =
                    new Column.Builder()
                            .setHorizontalAlignment(HORIZONTAL_ALIGN_LEFT)
                            .addContent(mainTextElement);
            if (mLabelText != null) {
                Text labelTextElement =
                        new Text.Builder()
                                .setText(mLabelText)
                                .setFontStyle(
                                        FontStyles.caption2(mDeviceParameters)
                                                .setColor(mChipColors.getSecondaryContentColor())
                                                .build())
                                .setMaxLines(1)
                                .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_TRUNCATE)
                                .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_START)
                                .build();
                column.addContent(labelTextElement);
            }

            if (mType == TEXT) {
                return column.build();
            } else {
                return new Row.Builder()
                        .addContent(
                                new Image.Builder()
                                        .setResourceId(mResourceId)
                                        .setWidth(ICON_SIZE)
                                        .setHeight(ICON_SIZE)
                                        .setColorFilter(
                                                new ColorFilter.Builder()
                                                        .setTint(mChipColors.getIconTintColor())
                                                        .build())
                                        .build())
                        .addContent(
                                new Spacer.Builder()
                                        .setHeight(mHeight)
                                        .setWidth(VERTICAL_PADDING)
                                        .build())
                        .addContent(column.build())
                        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                        .build();
            }
        }

        private FontStyle customizeFontStyle() {
            return FontStyle.fromProto(
                    mPrimaryTextFont.toProto().toBuilder()
                            .setColor(mChipColors.getContentColor().toProto())
                            .build());
        }

        private int getCorrectMaxLines() {
            if (mMaxLines > 0) {
                return mMaxLines;
            }
            return mLabelText != null ? 1 : 2;
        }
    }

    /** Returns height of this Chip. Intended for testing purposes only. */
    @NonNull
    public ContainerDimension getHeight() {
        return checkNotNull(mElement.getHeight());
    }

    /** Returns width of this Chip. Intended for testing purposes only. */
    @NonNull
    public ContainerDimension getWidth() {
        return checkNotNull(mElement.getWidth());
    }

    /** Returns click event action associated with this Chip. Intended for testing purposes only. */
    @NonNull
    public Action getAction() {
        return checkNotNull(
                checkNotNull(checkNotNull(mElement.getModifiers()).getClickable()).getOnClick());
    }

    /** Returns background color of this Chip. Intended for testing purposes only. */
    @NonNull
    private ColorProp getBackgroundColor() {
        return checkNotNull(
                checkNotNull(checkNotNull(mElement.getModifiers()).getBackground()).getColor());
    }

    /** Returns chip colors of this Chip. Intended for testing purposes only. */
    @NonNull
    public ChipColors getChipColors() {
        ColorProp backgroundColor = getBackgroundColor();
        ColorProp contentColor = null;
        ColorProp secondaryContentColor = null;
        ColorProp iconTintColor = null;

        LayoutElement content = getContent();
        if (content instanceof Row) {
            Row rowContent = (Row) content;
            List<LayoutElement> contents = rowContent.getContents();
            if (contents.size() == 3) {
                // This is potentially our built chip with icon and column that has text. Column
                // part will be extracted in the next condition.
                LayoutElement element = contents.get(0);
                if (element instanceof Image) {
                    // Extract icon tint color and prepare content for the second condition. If it
                    // is our chip, it will be a Column.
                    iconTintColor = getIconTintColorFromContent((Image) element);
                    content = contents.get(2);
                }
            }
        }
        if (content instanceof Column) {
            Column columnContent = (Column) content;
            List<LayoutElement> contents = columnContent.getContents();

            if (contents.size() == 1 || contents.size() == 2) {
                // This is potentially our chip and this part contains 1 or 2 lines of text.
                LayoutElement element = contents.get(0);
                if (element instanceof Text) {
                    contentColor = getTextColorFromContent((Text) element);

                    if (contents.size() == 2) {
                        element = contents.get(1);
                        if (element instanceof Text) {
                            secondaryContentColor = getTextColorFromContent((Text) element);
                        }
                    }
                }
            }
        }

        // Populate other colors if they are not found.
        if (contentColor == null) {
            contentColor = new ColorProp.Builder().build();
        }
        if (secondaryContentColor == null) {
            secondaryContentColor = contentColor;
        }
        if (iconTintColor == null) {
            iconTintColor = contentColor;
        }

        return new ChipColors(backgroundColor, iconTintColor, contentColor, secondaryContentColor);
    }

    private ColorProp getTextColorFromContent(Text text) {
        ColorProp color = new ColorProp.Builder().build();
        if (text.getFontStyle() != null && text.getFontStyle().getColor() != null) {
            color = checkNotNull(checkNotNull(text.getFontStyle()).getColor());
        }
        return color;
    }

    private ColorProp getIconTintColorFromContent(Image image) {
        ColorProp color = new ColorProp.Builder().build();
        if (image.getColorFilter() != null && image.getColorFilter().getTint() != null) {
            color = checkNotNull(checkNotNull(image.getColorFilter()).getTint());
        }
        return color;
    }

    /** Returns content description of this Chip. Intended for testing purposes only. */
    @NonNull
    public String getContentDescription() {
        return checkNotNull(
                checkNotNull(checkNotNull(mElement.getModifiers()).getSemantics())
                        .getContentDescription());
    }

    /** Returns content of this Chip. Intended for testing purposes only. */
    @NonNull
    public LayoutElement getContent() {
        return checkNotNull(checkNotNull(mElement.getContents()).get(0));
    }

    /** @hide */
    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return mElement.toLayoutElementProto();
    }
}
