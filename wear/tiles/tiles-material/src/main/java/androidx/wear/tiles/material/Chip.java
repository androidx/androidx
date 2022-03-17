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
import static androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_START;
import static androidx.wear.tiles.material.ChipDefaults.DEFAULT_HEIGHT;
import static androidx.wear.tiles.material.ChipDefaults.DEFAULT_MARGIN_PERCENT;
import static androidx.wear.tiles.material.ChipDefaults.HORIZONTAL_PADDING;
import static androidx.wear.tiles.material.ChipDefaults.ICON_SIZE;
import static androidx.wear.tiles.material.ChipDefaults.ICON_SPACER_WIDTH;
import static androidx.wear.tiles.material.ChipDefaults.PRIMARY;
import static androidx.wear.tiles.material.Helper.checkNotNull;
import static androidx.wear.tiles.material.Helper.radiusOf;

import android.content.Context;

import androidx.annotation.Dimension;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.ColorBuilders.ColorProp;
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.tiles.DimensionBuilders.ContainerDimension;
import androidx.wear.tiles.DimensionBuilders.DpProp;
import androidx.wear.tiles.LayoutElementBuilders;
import androidx.wear.tiles.LayoutElementBuilders.Box;
import androidx.wear.tiles.LayoutElementBuilders.ColorFilter;
import androidx.wear.tiles.LayoutElementBuilders.Column;
import androidx.wear.tiles.LayoutElementBuilders.HorizontalAlignment;
import androidx.wear.tiles.LayoutElementBuilders.Image;
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement;
import androidx.wear.tiles.LayoutElementBuilders.Row;
import androidx.wear.tiles.LayoutElementBuilders.Spacer;
import androidx.wear.tiles.ModifiersBuilders;
import androidx.wear.tiles.ModifiersBuilders.Background;
import androidx.wear.tiles.ModifiersBuilders.Clickable;
import androidx.wear.tiles.ModifiersBuilders.Corner;
import androidx.wear.tiles.ModifiersBuilders.Modifiers;
import androidx.wear.tiles.ModifiersBuilders.Padding;
import androidx.wear.tiles.material.Typography.TypographyName;
import androidx.wear.tiles.proto.LayoutElementProto;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Tiles component {@link Chip} that represents clickable object with the text, optional label and
 * optional icon or with custom content.
 *
 * <p>The Chip is Stadium shape and has a max height designed to take no more than two lines of text
 * of {@link Typography#TYPOGRAPHY_BUTTON} style. The {@link Chip} can have an icon horizontally
 * parallel to the two lines of text. Width of chip can very, and the recommended size is screen
 * dependent with the recommended margin being applied.
 *
 * <p>The recommended set of {@link ChipColors} styles can be obtained from {@link ChipDefaults}.,
 * e.g. {@link ChipDefaults#PRIMARY} to get a color scheme for a primary {@link Chip}.
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

        @NonNull private final Context mContext;
        @Nullable private LayoutElement mCustomContent;
        @NonNull private String mResourceId = "";
        @NonNull private String mPrimaryText = "";
        @Nullable private String mLabelText = null;
        @NonNull private final Clickable mClickable;
        @NonNull private CharSequence mContentDescription = "";
        @NonNull private ContainerDimension mWidth;
        @NonNull private DpProp mHeight = DEFAULT_HEIGHT;
        @NonNull private ChipColors mChipColors = PRIMARY;
        @ChipType private int mType = NOT_SET;
        @HorizontalAlignment private int mHorizontalAlign = HORIZONTAL_ALIGN_START;
        @TypographyName private int mPrimaryTextTypography;
        @NonNull private DpProp mHorizontalPadding = HORIZONTAL_PADDING;
        private boolean mIsScalable = true;

        /**
         * Creates a builder for the {@link Chip} with associated action. It is required to add
         * content later with setters.
         *
         * @param context The application's context.
         * @param clickable Associated {@link Clickable} for click events. When the Chip is clicked
         *     it will fire the associated action.
         * @param deviceParameters The device parameters used to derive defaults for this Chip.
         */
        public Builder(
                @NonNull Context context,
                @NonNull Clickable clickable,
                @NonNull DeviceParameters deviceParameters) {
            mContext = context;
            mClickable = clickable;
            mWidth =
                    dp(
                            (100 - 2 * DEFAULT_MARGIN_PERCENT)
                                    * deviceParameters.getScreenWidthDp()
                                    / 100);
            mPrimaryTextTypography = Typography.TYPOGRAPHY_BUTTON;
        }

        /**
         * Sets the width of {@link Chip}. If not set, default value will be set to fill the screen.
         */
        @NonNull
        public Builder setWidth(@NonNull ContainerDimension width) {
            mWidth = width;
            return this;
        }

        /**
         * Sets the width of {@link TitleChip}. If not set, default value will be set to fill the
         * screen.
         */
        @NonNull
        public Builder setWidth(@Dimension(unit = DP) float width) {
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
        public Builder setContentDescription(@NonNull CharSequence contentDescription) {
            this.mContentDescription = contentDescription;
            return this;
        }

        /**
         * Sets the content of the {@link Chip} to be the given primary text. Any previously added
         * content will be overridden. Primary text can be on 1 or 2 lines, depending on the length.
         */
        // There are multiple methods to set different type of content, but there is general getter
        // getContent that will return LayoutElement set by any of them. b/217197259
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public Builder setPrimaryTextContent(@NonNull String primaryText) {
            this.mPrimaryText = primaryText;
            this.mLabelText = null;
            this.mType = TEXT;
            return this;
        }

        /**
         * Used for creating CompactChip and TitleChip.
         *
         * <p>Sets the font for the primary text and should only be used internally.
         */
        @NonNull
        Builder setPrimaryTextTypography(@TypographyName int typography) {
            this.mPrimaryTextTypography = typography;
            return this;
        }

        /**
         * Used for creating CompactChip and TitleChip.
         *
         * <p>Sets whether the font for the primary text is scalable.
         */
        @NonNull
        Builder setIsPrimaryTextScalable(boolean isScalable) {
            this.mIsScalable = isScalable;
            return this;
        }

        /**
         * Sets the content of the {@link Chip} to be the given primary text and secondary label.
         * Any previously added content will be overridden. Primary text can be shown on 1 line
         * only.
         */
        // There are multiple methods to set different type of content, but there is general getter
        // getContent that will return LayoutElement set by any of them. b/217197259
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
         * chosen alpha channel and not an actual image.
         */
        // There are multiple methods to set different type of content, but there is general getter
        // getContent that will return LayoutElement set by any of them. b/217197259
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
         * channel and not an actual image. Primary text can be shown on 1 line only.
         */
        // There are multiple methods to set different type of content, but there is general getter
        // getContent that will return LayoutElement set by any of them. b/217197259
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

        /**
         * Sets the colors for the {@link Chip}. If set, {@link ChipColors#getBackgroundColor()}
         * will be used for the background of the button, {@link ChipColors#getContentColor()} for
         * main text, {@link ChipColors#getSecondaryContentColor()} for label text and {@link
         * ChipColors#getIconTintColor()} will be used as tint color for the icon itself. If not
         * set, {@link ChipDefaults#PRIMARY} will be used.
         */
        @NonNull
        public Builder setChipColors(@NonNull ChipColors chipColors) {
            mChipColors = chipColors;
            return this;
        }

        /**
         * Sets the horizontal alignment in the chip. It is strongly recommended that the content of
         * the chip is start-aligned if there is more than primary text in it. If not set, {@link
         * HorizontalAlignment#HORIZONTAL_ALIGN_START} will be used.
         */
        @NonNull
        public Builder setHorizontalAlignment(@HorizontalAlignment int horizontalAlignment) {
            mHorizontalAlign = horizontalAlignment;
            return this;
        }

        /** Used for creating CompactChip and TitleChip. */
        @NonNull
        Builder setHorizontalPadding(@NonNull DpProp horizontalPadding) {
            this.mHorizontalPadding = horizontalPadding;
            return this;
        }

        /** Used for creating CompactChip and TitleChip. */
        @NonNull
        Builder setHeight(@NonNull DpProp height) {
            this.mHeight = height;
            return this;
        }

        /** Used for creating CompactChip and TitleChip. */
        @NonNull
        Builder setMaxLines(int maxLines) {
            this.mMaxLines = maxLines;
            return this;
        }

        /** Constructs and returns {@link Chip} with the provided content and look. */
        @NonNull
        @Override
        public Chip build() {
            Modifiers.Builder modifiers =
                    new Modifiers.Builder()
                            .setClickable(mClickable)
                            .setPadding(
                                    new Padding.Builder()
                                            .setStart(mHorizontalPadding)
                                            .setEnd(mHorizontalPadding)
                                            .build())
                            .setBackground(
                                    new Background.Builder()
                                            .setColor(mChipColors.getBackgroundColor())
                                            .setCorner(
                                                    new Corner.Builder()
                                                            .setRadius(radiusOf(mHeight))
                                                            .build())
                                            .build());
            if (mContentDescription.length() > 0) {
                modifiers.setSemantics(
                        new ModifiersBuilders.Semantics.Builder()
                                .setContentDescription(mContentDescription.toString())
                                .build());
            }

            Box.Builder element =
                    new Box.Builder()
                            .setWidth(mWidth)
                            .setHeight(mHeight)
                            .setHorizontalAlignment(mHorizontalAlign)
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
                    new Text.Builder(mContext)
                            .setText(mPrimaryText)
                            .setTypography(mPrimaryTextTypography)
                            .setColor(mChipColors.getContentColor())
                            .setMaxLines(getCorrectMaxLines())
                            .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE_END)
                            .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_START)
                            .setIsScalable(mIsScalable)
                            .build();

            // Placeholder for text.
            Column.Builder column =
                    new Column.Builder()
                            .setHorizontalAlignment(HORIZONTAL_ALIGN_START)
                            .addContent(putLayoutInBox(mainTextElement).build());

            if (mLabelText != null) {
                Text labelTextElement =
                        new Text.Builder(mContext)
                                .setText(mLabelText)
                                .setTypography(Typography.TYPOGRAPHY_CAPTION2)
                                .setColor(mChipColors.getSecondaryContentColor())
                                .setMaxLines(1)
                                .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE_END)
                                .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_START)
                                .build();
                column.addContent(putLayoutInBox(labelTextElement).build());
            }

            Box texts = putLayoutInBox(column.build()).build();
            if (mType == TEXT) {
                return texts;
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
                                        .setWidth(ICON_SPACER_WIDTH)
                                        .build())
                        .addContent(texts)
                        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                        .build();
            }
        }

        private int getCorrectMaxLines() {
            if (mMaxLines > 0) {
                return mMaxLines;
            }
            return mLabelText != null ? 1 : 2;
        }

        private Box.Builder putLayoutInBox(@NonNull LayoutElement element) {
            // Wrapped and centered content are default.
            return new Box.Builder().addContent(element);
        }
    }

    /** Returns height of this Chip. */
    @NonNull
    public ContainerDimension getHeight() {
        return checkNotNull(mElement.getHeight());
    }

    /** Returns width of this Chip. */
    @NonNull
    public ContainerDimension getWidth() {
        return checkNotNull(mElement.getWidth());
    }

    /** Returns click event action associated with this Chip. */
    @NonNull
    public Clickable getClickable() {
        return checkNotNull(checkNotNull(mElement.getModifiers()).getClickable());
    }

    /** Returns background color of this Chip. */
    @NonNull
    private ColorProp getBackgroundColor() {
        return checkNotNull(
                checkNotNull(checkNotNull(mElement.getModifiers()).getBackground()).getColor());
    }

    /** Returns chip colors of this Chip. */
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
        if (content instanceof Box && ((Box) content).getContents().get(0) instanceof Column) {
            Column columnContent = (Column) ((Box) content).getContents().get(0);
            List<LayoutElement> contents = columnContent.getContents();

            if (contents.size() == 1 || contents.size() == 2) {
                // This is potentially our chip and this part contains 1 or 2 lines of text.
                LayoutElement element = contents.get(0);
                if (element instanceof Box
                        && ((Box) element).getContents().get(0)
                                instanceof LayoutElementBuilders.Text) {
                    // To elementary Text class as Material Text when it goes to proto disappears.
                    contentColor =
                            getTextColorFromContent(
                                    (LayoutElementBuilders.Text)
                                            ((Box) element).getContents().get(0));

                    if (contents.size() == 2) {
                        element = contents.get(1);
                        if (element instanceof Box
                                && ((Box) element).getContents().get(0)
                                        instanceof LayoutElementBuilders.Text) {
                            // To elementary Text class as Material Text when it goes to proto
                            // disappears.
                            secondaryContentColor =
                                    getTextColorFromContent(
                                            (LayoutElementBuilders.Text)
                                                    ((Box) element).getContents().get(0));
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

    private ColorProp getTextColorFromContent(LayoutElementBuilders.Text text) {
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

    /** Returns content description of this Chip. */
    @NonNull
    public CharSequence getContentDescription() {
        return checkNotNull(
                checkNotNull(checkNotNull(mElement.getModifiers()).getSemantics())
                        .getContentDescription());
    }

    /** Returns content of this Chip. */
    @NonNull
    public LayoutElement getContent() {
        return checkNotNull(checkNotNull(mElement.getContents()).get(0));
    }

    /** Returns the horizontal alignment of the content in this Chip. */
    @HorizontalAlignment
    public int getHorizontalAlignment() {
        return checkNotNull(mElement.getHorizontalAlignment()).getValue();
    }

    /** @hide */
    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return mElement.toLayoutElementProto();
    }
}
