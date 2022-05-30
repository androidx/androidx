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
import static androidx.wear.tiles.material.ChipDefaults.PRIMARY_COLORS;
import static androidx.wear.tiles.material.Helper.checkNotNull;
import static androidx.wear.tiles.material.Helper.checkTag;
import static androidx.wear.tiles.material.Helper.getMetadataTagName;
import static androidx.wear.tiles.material.Helper.getTagBytes;
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
import androidx.wear.tiles.ModifiersBuilders.ElementMetadata;
import androidx.wear.tiles.ModifiersBuilders.Modifiers;
import androidx.wear.tiles.ModifiersBuilders.Padding;
import androidx.wear.tiles.ModifiersBuilders.Semantics;
import androidx.wear.tiles.material.Typography.TypographyName;
import androidx.wear.tiles.proto.LayoutElementProto;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;

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
 * e.g. {@link ChipDefaults#PRIMARY_COLORS} to get a color scheme for a primary {@link Chip}.
 */
public class Chip implements LayoutElement {
    /**
     * Tool tag for Metadata in Modifiers, so we know that Box is actually a Chip with only text.
     */
    static final String METADATA_TAG_TEXT = "TXTCHP";
    /** Tool tag for Metadata in Modifiers, so we know that Box is actually a Chip with icon. */
    static final String METADATA_TAG_ICON = "ICNCHP";
    /**
     * Tool tag for Metadata in Modifiers, so we know that Box is actually a Chip with custom
     * content.
     */
    static final String METADATA_TAG_CUSTOM_CONTENT = "CSTCHP";

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
        @NonNull private ChipColors mChipColors = PRIMARY_COLORS;
        @ChipType private int mType = NOT_SET;
        @HorizontalAlignment private int mHorizontalAlign = HORIZONTAL_ALIGN_START;
        @TypographyName private int mPrimaryTextTypography;
        @NonNull private DpProp mHorizontalPadding = HORIZONTAL_PADDING;
        private boolean mIsScalable = true;
        private int mMaxLines = 0; // 0 indicates that is not set.
        @NonNull private String mMetadataTag = "";

        @NonNull static final Map<Integer, String> TYPE_TO_TAG = new HashMap<>();

        static {
            TYPE_TO_TAG.put(ICON, METADATA_TAG_ICON);
            TYPE_TO_TAG.put(TEXT, METADATA_TAG_TEXT);
            TYPE_TO_TAG.put(CUSTOM_CONTENT, METADATA_TAG_CUSTOM_CONTENT);
        }

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
        public Builder setCustomContent(@NonNull LayoutElement content) {
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
        // There's a getter for primary text - getPrimaryText.
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
        // There are separate getters for primary text and label.
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
        // There are separate getters for primary text and icon.
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
        // There are separate getters for primary text, icon and label.
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
         * ChipColors#getIconColor()} will be used as color for the icon itself. If not set, {@link
         * ChipDefaults#PRIMARY_COLORS} will be used.
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

        /** Used for setting the correct tag in CompactChip and TitleChip. */
        @NonNull
        Builder setMetadataTag(@NonNull String metadataTag) {
            this.mMetadataTag = metadataTag;
            return this;
        }

        /** Constructs and returns {@link Chip} with the provided content and look. */
        @NonNull
        @Override
        public Chip build() {
            assertContentFields();

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
                                            .build())
                            .setMetadata(
                                    new ElementMetadata.Builder()
                                            .setTagData(getCorrectMetadataTag())
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

        private void assertContentFields() {
            if (mType == NOT_SET) {
                throw new IllegalStateException(
                        "No content set. Use setPrimaryTextContent or similar method to add"
                            + " content");
            }
        }

        private byte[] getCorrectMetadataTag() {
            return getTagBytes(
                    mMetadataTag.isEmpty() ? checkNotNull(TYPE_TO_TAG.get(mType)) : mMetadataTag);
        }

        @NonNull
        private LayoutElement getCorrectContent() {
            if (mType == CUSTOM_CONTENT) {
                return checkNotNull(mCustomContent);
            }
            Text mainTextElement =
                    new Text.Builder(mContext, mPrimaryText)
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
                        new Text.Builder(mContext, mLabelText)
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
                                                        .setTint(mChipColors.getIconColor())
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

        if (!getMetadataTag().equals(METADATA_TAG_CUSTOM_CONTENT)) {
            if (getMetadataTag().equals(METADATA_TAG_ICON)) {
                Image icon = checkNotNull(getIconContentObject());
                iconTintColor = checkNotNull(checkNotNull(icon.getColorFilter()).getTint());
            }

            contentColor = checkNotNull(getPrimaryTextContentObject()).getColor();
            Text label = getLabelContentObject();
            if (label != null) {
                secondaryContentColor = label.getColor();
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

    /** Returns content description of this Chip. */
    @Nullable
    public CharSequence getContentDescription() {
        Semantics semantics = checkNotNull(mElement.getModifiers()).getSemantics();
        if (semantics == null) {
            return null;
        }
        return semantics.getContentDescription();
    }

    /** Returns custom content from this Chip if it has been added. Otherwise, it returns null. */
    @Nullable
    public LayoutElement getCustomContent() {
        if (getMetadataTag().equals(METADATA_TAG_CUSTOM_CONTENT)) {
            return checkNotNull(checkNotNull(mElement.getContents()).get(0));
        }
        return null;
    }

    /** Returns primary text from this Chip if it has been added. Otherwise, it returns null. */
    @Nullable
    public String getPrimaryTextContent() {
        Text primaryText = getPrimaryTextContentObject();
        return primaryText != null ? primaryText.getText() : null;
    }

    /** Returns label text from this Chip if it has been added. Otherwise, it returns null. */
    @Nullable
    public String getLabelContent() {
        Text label = getLabelContentObject();
        return label != null ? label.getText() : null;
    }

    /** Returns icon id from this Chip if it has been added. Otherwise, it returns null. */
    @Nullable
    public String getIconContent() {
        Image icon = getIconContentObject();
        return icon != null ? checkNotNull(icon.getResourceId()).getValue() : null;
    }

    @Nullable
    private Text getPrimaryTextContentObject() {
        return getPrimaryOrLabelTextContent(0);
    }

    @Nullable
    private Text getLabelContentObject() {
        return getPrimaryOrLabelTextContent(1);
    }

    @Nullable
    private Image getIconContentObject() {
        if (!getMetadataTag().equals(METADATA_TAG_ICON)) {
            return null;
        }
        return ((Image) ((Row) mElement.getContents().get(0)).getContents().get(0));
    }

    @Nullable
    private Text getPrimaryOrLabelTextContent(int index) {
        String metadataTag = getMetadataTag();
        if (metadataTag.equals(METADATA_TAG_CUSTOM_CONTENT)) {
            return null;
        }
        // In any other case, text (either primary or primary + label) must be present.
        Column content;
        if (metadataTag.equals(METADATA_TAG_ICON)) {
            content =
                    (Column)
                            ((Box) ((Row) mElement.getContents().get(0)).getContents().get(2))
                                    .getContents()
                                    .get(0);
        } else {
            content = (Column) ((Box) mElement.getContents().get(0)).getContents().get(0);
        }

        // We need to check this as this can be the case when we called for label, which doesn't
        // exist.
        return index < content.getContents().size()
                ? Text.fromLayoutElement(
                        ((Box) content.getContents().get(index)).getContents().get(0))
                : null;
    }

    /** Returns the horizontal alignment of the content in this Chip. */
    @HorizontalAlignment
    public int getHorizontalAlignment() {
        return checkNotNull(mElement.getHorizontalAlignment()).getValue();
    }

    /** Returns metadata tag set to this Chip. */
    @NonNull
    String getMetadataTag() {
        return getMetadataTagName(
                checkNotNull(checkNotNull(mElement.getModifiers()).getMetadata()));
    }

    /**
     * Returns Chip object from the given LayoutElement if that element can be converted to Chip.
     * Otherwise, returns null.
     */
    @Nullable
    public static Chip fromLayoutElement(@NonNull LayoutElement element) {
        if (!(element instanceof Box)) {
            return null;
        }
        Box boxElement = (Box) element;
        if (!checkTag(boxElement.getModifiers(), Builder.TYPE_TO_TAG.values())) {
            return null;
        }
        // Now we are sure that this element is a Chip.
        return new Chip(boxElement);
    }

    /** @hide */
    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return mElement.toLayoutElementProto();
    }
}
