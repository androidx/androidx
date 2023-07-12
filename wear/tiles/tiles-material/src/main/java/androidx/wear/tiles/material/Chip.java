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
import androidx.wear.protolayout.proto.LayoutElementProto;
import androidx.wear.tiles.material.Typography.TypographyName;

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
 *
 * <p>When accessing the contents of a container for testing, note that this element can't be simply
 * casted back to the original type, i.e.:
 *
 * <pre>{@code
 * Chip chip = new Chip...
 * Box box = new Box.Builder().addContent(chip).build();
 *
 * Chip myChip = (Chip) box.getContents().get(0);
 * }</pre>
 *
 * will fail.
 *
 * <p>To be able to get {@link Chip} object from any layout element, {@link #fromLayoutElement}
 * method should be used, i.e.:
 *
 * <pre>{@code
 * Chip myChip = Chip.fromLayoutElement(box.getContents().get(0));
 * }</pre>
 *
 * @see androidx.wear.tiles.material.layouts.PrimaryLayout.Builder#setContent if this Chip is used
 *     inside of {@link androidx.wear.tiles.material.layouts.PrimaryLayout}.
 * @deprecated Use the new class {@link androidx.wear.protolayout.material.Chip} which provides the
 *     same API and functionality.
 */
@Deprecated
@SuppressWarnings("deprecation")
public class Chip implements androidx.wear.tiles.LayoutElementBuilders.LayoutElement {
    /**
     * Tool tag for Metadata in androidx.wear.tiles.ModifiersBuilders.Modifiers, so we know that
     * androidx.wear.tiles.LayoutElementBuilders.Box is actually a Chip with only text.
     */
    static final String METADATA_TAG_TEXT = "TXTCHP";
    /**
     * Tool tag for Metadata in androidx.wear.tiles.ModifiersBuilders.Modifiers, so we know that
     * androidx.wear.tiles.LayoutElementBuilders.Box is actually a Chip with icon.
     */
    static final String METADATA_TAG_ICON = "ICNCHP";
    /**
     * Tool tag for Metadata in androidx.wear.tiles.ModifiersBuilders.Modifiers, so we know that
     * androidx.wear.tiles.LayoutElementBuilders.Box is actually a Chip with custom content.
     */
    static final String METADATA_TAG_CUSTOM_CONTENT = "CSTCHP";

    @NonNull private final androidx.wear.tiles.LayoutElementBuilders.Box mElement;

    Chip(@NonNull androidx.wear.tiles.LayoutElementBuilders.Box element) {
        mElement = element;
    }
    /** Builder class for {@link androidx.wear.tiles.material.Chip}. */
    public static final class Builder
            implements androidx.wear.tiles.LayoutElementBuilders.LayoutElement.Builder {
        private static final int NOT_SET = 0;
        private static final int TEXT = 1;
        private static final int ICON = 2;
        private static final int CUSTOM_CONTENT = 3;

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({NOT_SET, TEXT, ICON, CUSTOM_CONTENT})
        @interface ChipType {}

        @NonNull private final Context mContext;
        @Nullable private androidx.wear.tiles.LayoutElementBuilders.LayoutElement mCustomContent;
        @Nullable private String mImageResourceId = null;
        @Nullable private String mPrimaryLabel = null;
        @Nullable private String mSecondaryLabel = null;
        @NonNull private final androidx.wear.tiles.ModifiersBuilders.Clickable mClickable;
        @NonNull private CharSequence mContentDescription = "";
        @NonNull private androidx.wear.tiles.DimensionBuilders.ContainerDimension mWidth;
        @NonNull private androidx.wear.tiles.DimensionBuilders.DpProp mHeight = DEFAULT_HEIGHT;
        @NonNull private ChipColors mChipColors = PRIMARY_COLORS;

        @androidx.wear.tiles.LayoutElementBuilders.HorizontalAlignment
        private int mHorizontalAlign =
                androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_UNDEFINED;

        @TypographyName private int mPrimaryLabelTypography;

        @NonNull
        private androidx.wear.tiles.DimensionBuilders.DpProp mHorizontalPadding =
                HORIZONTAL_PADDING;

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
                @NonNull androidx.wear.tiles.ModifiersBuilders.Clickable clickable,
                @NonNull
                        androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
                                deviceParameters) {
            mContext = context;
            mClickable = clickable;
            mWidth =
                    androidx.wear.tiles.DimensionBuilders.dp(
                            (100 - 2 * DEFAULT_MARGIN_PERCENT)
                                    * deviceParameters.getScreenWidthDp()
                                    / 100);
            mPrimaryLabelTypography = Typography.TYPOGRAPHY_BUTTON;
        }

        /**
         * Sets the width of {@link Chip}. If not set, default value will be set to fill the screen.
         */
        @NonNull
        public Builder setWidth(
                @NonNull androidx.wear.tiles.DimensionBuilders.ContainerDimension width) {
            mWidth = width;
            return this;
        }

        /**
         * Sets the width of {@link TitleChip}. If not set, default value will be set to fill the
         * screen.
         */
        @NonNull
        public Builder setWidth(@Dimension(unit = DP) float width) {
            mWidth = androidx.wear.tiles.DimensionBuilders.dp(width);
            return this;
        }

        /**
         * Sets the custom content for the {@link Chip}. Any previously added content will be
         * overridden.
         */
        @NonNull
        public Builder setCustomContent(
                @NonNull androidx.wear.tiles.LayoutElementBuilders.LayoutElement content) {
            this.mCustomContent = content;
            this.mPrimaryLabel = "";
            this.mSecondaryLabel = "";
            this.mImageResourceId = "";
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
         * Sets the primary label for the {@link Chip}. Any previously added custom content will be
         * overridden. Primary label can be on 1 or 2 lines, depending on the length and existence
         * of secondary label.
         */
        @NonNull
        public Builder setPrimaryLabelContent(@NonNull String primaryLabel) {
            this.mPrimaryLabel = primaryLabel;
            this.mCustomContent = null;
            return this;
        }

        /**
         * Used for creating CompactChip and TitleChip.
         *
         * <p>Sets the font for the primary label and should only be used internally.
         */
        @NonNull
        Builder setPrimaryLabelTypography(@TypographyName int typography) {
            this.mPrimaryLabelTypography = typography;
            return this;
        }

        /**
         * Used for creating CompactChip and TitleChip.
         *
         * <p>Sets whether the font for the primary label is scalable.
         */
        @NonNull
        Builder setIsPrimaryLabelScalable(boolean isScalable) {
            this.mIsScalable = isScalable;
            return this;
        }

        /**
         * Sets the secondary label for the {@link Chip}. Any previously added custom content will
         * be overridden. If secondary label is set, primary label must be set too with {@link
         * #setPrimaryLabelContent}.
         */
        @NonNull
        public Builder setSecondaryLabelContent(@NonNull String secondaryLabel) {
            this.mSecondaryLabel = secondaryLabel;
            this.mCustomContent = null;
            return this;
        }

        /**
         * Sets the icon for the {@link Chip}. Any previously added custom content will be
         * overridden. Provided icon will be tinted to the given content color from {@link
         * ChipColors}. This icon should be image with chosen alpha channel and not an actual image.
         * If icon is set, primary label must be set too with {@link #setPrimaryLabelContent}.
         */
        @NonNull
        public Builder setIconContent(@NonNull String imageResourceId) {
            this.mImageResourceId = imageResourceId;
            this.mCustomContent = null;
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
         * the chip is start-aligned if there is more than primary text in it. By default, {@link
         * androidx.wear.tiles.LayoutElementBuilders.HorizontalAlignment#HORIZONTAL_ALIGN_CENTER}
         * will be used when only a primary label is present. Otherwise {@link
         * androidx.wear.tiles.LayoutElementBuilders.HorizontalAlignment#HORIZONTAL_ALIGN_START}
         * will be used.
         */
        @NonNull
        public Builder setHorizontalAlignment(
                @androidx.wear.tiles.LayoutElementBuilders.HorizontalAlignment
                        int horizontalAlignment) {
            mHorizontalAlign = horizontalAlignment;
            return this;
        }

        /** Used for creating CompactChip and TitleChip. */
        @NonNull
        Builder setHorizontalPadding(
                @NonNull androidx.wear.tiles.DimensionBuilders.DpProp horizontalPadding) {
            this.mHorizontalPadding = horizontalPadding;
            return this;
        }

        /** Used for creating CompactChip and TitleChip. */
        @NonNull
        Builder setHeight(@NonNull androidx.wear.tiles.DimensionBuilders.DpProp height) {
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
            androidx.wear.tiles.ModifiersBuilders.Modifiers.Builder modifiers =
                    new androidx.wear.tiles.ModifiersBuilders.Modifiers.Builder()
                            .setClickable(mClickable)
                            .setPadding(
                                    new androidx.wear.tiles.ModifiersBuilders.Padding.Builder()
                                            .setStart(mHorizontalPadding)
                                            .setEnd(mHorizontalPadding)
                                            .build())
                            .setBackground(
                                    new androidx.wear.tiles.ModifiersBuilders.Background.Builder()
                                            .setColor(mChipColors.getBackgroundColor())
                                            .setCorner(
                                                    new androidx.wear.tiles.ModifiersBuilders.Corner
                                                                    .Builder()
                                                            .setRadius(radiusOf(mHeight))
                                                            .build())
                                            .build())
                            .setMetadata(
                                    new androidx.wear.tiles.ModifiersBuilders.ElementMetadata
                                                    .Builder()
                                            .setTagData(getTagBytes(getCorrectMetadataTag()))
                                            .build())
                            .setSemantics(
                                    new androidx.wear.tiles.ModifiersBuilders.Semantics.Builder()
                                            .setContentDescription(getCorrectContentDescription())
                                            .build());

            androidx.wear.tiles.LayoutElementBuilders.Box.Builder element =
                    new androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
                            .setWidth(mWidth)
                            .setHeight(mHeight)
                            .setHorizontalAlignment(getCorrectHorizontalAlignment())
                            .addContent(getCorrectContent())
                            .setModifiers(modifiers.build());

            return new Chip(element.build());
        }

        @NonNull
        private String getCorrectContentDescription() {
            if (mContentDescription.length() == 0) {
                mContentDescription = "";
                if (mPrimaryLabel != null) {
                    mContentDescription += mPrimaryLabel;
                }
                if (mSecondaryLabel != null) {
                    mContentDescription += "\n" + mSecondaryLabel;
                }
            }
            return mContentDescription.toString();
        }

        @androidx.wear.tiles.LayoutElementBuilders.HorizontalAlignment
        private int getCorrectHorizontalAlignment() {
            if (mHorizontalAlign
                    != androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_UNDEFINED) {
                return mHorizontalAlign;
            }
            if (mPrimaryLabel != null && mSecondaryLabel == null && mImageResourceId == null) {
                return androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER;
            } else {
                return androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_START;
            }
        }

        private String getCorrectMetadataTag() {
            if (!mMetadataTag.isEmpty()) {
                return mMetadataTag;
            }
            if (mCustomContent != null) {
                return METADATA_TAG_CUSTOM_CONTENT;
            }
            if (mImageResourceId != null) {
                return METADATA_TAG_ICON;
            }
            return METADATA_TAG_TEXT;
        }

        @NonNull
        private androidx.wear.tiles.LayoutElementBuilders.LayoutElement getCorrectContent() {
            if (mCustomContent != null) {
                return mCustomContent;
            }

            Text mainTextElement =
                    new Text.Builder(mContext, checkNotNull(mPrimaryLabel))
                            .setTypography(mPrimaryLabelTypography)
                            .setColor(mChipColors.getContentColor())
                            .setMaxLines(getCorrectMaxLines())
                            .setOverflow(
                                    androidx.wear.tiles.LayoutElementBuilders
                                            .TEXT_OVERFLOW_ELLIPSIZE_END)
                            .setMultilineAlignment(
                                    androidx.wear.tiles.LayoutElementBuilders.TEXT_ALIGN_START)
                            .setIsScalable(mIsScalable)
                            .build();

            // Placeholder for text.
            androidx.wear.tiles.LayoutElementBuilders.Column.Builder column =
                    new androidx.wear.tiles.LayoutElementBuilders.Column.Builder()
                            .setHorizontalAlignment(
                                    androidx.wear.tiles.LayoutElementBuilders
                                            .HORIZONTAL_ALIGN_START)
                            .addContent(putLayoutInBox(mainTextElement).build());

            if (mSecondaryLabel != null) {
                Text labelTextElement =
                        new Text.Builder(mContext, mSecondaryLabel)
                                .setTypography(Typography.TYPOGRAPHY_CAPTION2)
                                .setColor(mChipColors.getSecondaryContentColor())
                                .setMaxLines(1)
                                .setOverflow(
                                        androidx.wear.tiles.LayoutElementBuilders
                                                .TEXT_OVERFLOW_ELLIPSIZE_END)
                                .setMultilineAlignment(
                                        androidx.wear.tiles.LayoutElementBuilders.TEXT_ALIGN_START)
                                .build();
                column.addContent(putLayoutInBox(labelTextElement).build());
            }

            androidx.wear.tiles.LayoutElementBuilders.Box texts =
                    putLayoutInBox(column.build()).build();
            if (mImageResourceId == null) {
                return texts;
            } else {
                return new androidx.wear.tiles.LayoutElementBuilders.Row.Builder()
                        .addContent(
                                new androidx.wear.tiles.LayoutElementBuilders.Image.Builder()
                                        .setResourceId(mImageResourceId)
                                        .setWidth(ICON_SIZE)
                                        .setHeight(ICON_SIZE)
                                        .setColorFilter(
                                                new androidx.wear.tiles.LayoutElementBuilders
                                                                .ColorFilter.Builder()
                                                        .setTint(mChipColors.getIconColor())
                                                        .build())
                                        .build())
                        .addContent(
                                new androidx.wear.tiles.LayoutElementBuilders.Spacer.Builder()
                                        .setHeight(mHeight)
                                        .setWidth(ICON_SPACER_WIDTH)
                                        .build())
                        .addContent(texts)
                        .setVerticalAlignment(
                                androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                        .build();
            }
        }

        private int getCorrectMaxLines() {
            if (mMaxLines > 0) {
                return mMaxLines;
            }
            return mSecondaryLabel != null ? 1 : 2;
        }

        private androidx.wear.tiles.LayoutElementBuilders.Box.Builder putLayoutInBox(
                @NonNull androidx.wear.tiles.LayoutElementBuilders.LayoutElement element) {
            // Wrapped and centered content are default.
            return new androidx.wear.tiles.LayoutElementBuilders.Box.Builder().addContent(element);
        }
    }

    /** Returns height of this Chip. */
    @NonNull
    public androidx.wear.tiles.DimensionBuilders.ContainerDimension getHeight() {
        return checkNotNull(mElement.getHeight());
    }

    /** Returns width of this Chip. */
    @NonNull
    public androidx.wear.tiles.DimensionBuilders.ContainerDimension getWidth() {
        return checkNotNull(mElement.getWidth());
    }

    /** Returns click event action associated with this Chip. */
    @NonNull
    public androidx.wear.tiles.ModifiersBuilders.Clickable getClickable() {
        return checkNotNull(checkNotNull(mElement.getModifiers()).getClickable());
    }

    /** Returns background color of this Chip. */
    @NonNull
    private androidx.wear.tiles.ColorBuilders.ColorProp getBackgroundColor() {
        return checkNotNull(
                checkNotNull(checkNotNull(mElement.getModifiers()).getBackground()).getColor());
    }

    /** Returns chip colors of this Chip. */
    @NonNull
    public ChipColors getChipColors() {
        androidx.wear.tiles.ColorBuilders.ColorProp backgroundColor = getBackgroundColor();
        androidx.wear.tiles.ColorBuilders.ColorProp contentColor = null;
        androidx.wear.tiles.ColorBuilders.ColorProp secondaryContentColor = null;
        androidx.wear.tiles.ColorBuilders.ColorProp iconTintColor = null;

        if (!getMetadataTag().equals(METADATA_TAG_CUSTOM_CONTENT)) {
            if (getMetadataTag().equals(METADATA_TAG_ICON)) {
                androidx.wear.tiles.LayoutElementBuilders.Image icon =
                        checkNotNull(getIconContentObject());
                iconTintColor = checkNotNull(checkNotNull(icon.getColorFilter()).getTint());
            }

            contentColor = checkNotNull(getPrimaryLabelContentObject()).getColor();
            Text label = getSecondaryLabelContentObject();
            if (label != null) {
                secondaryContentColor = label.getColor();
            }
        }

        // Populate other colors if they are not found.
        if (contentColor == null) {
            contentColor = new androidx.wear.tiles.ColorBuilders.ColorProp.Builder().build();
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
        androidx.wear.tiles.ModifiersBuilders.Semantics semantics =
                checkNotNull(mElement.getModifiers()).getSemantics();
        if (semantics == null) {
            return null;
        }
        return semantics.getContentDescription();
    }

    /** Returns custom content from this Chip if it has been added. Otherwise, it returns null. */
    @Nullable
    public androidx.wear.tiles.LayoutElementBuilders.LayoutElement getCustomContent() {
        if (getMetadataTag().equals(METADATA_TAG_CUSTOM_CONTENT)) {
            return checkNotNull(checkNotNull(mElement.getContents()).get(0));
        }
        return null;
    }

    /** Returns primary label from this Chip if it has been added. Otherwise, it returns null. */
    @Nullable
    public String getPrimaryLabelContent() {
        Text primaryLabel = getPrimaryLabelContentObject();
        return primaryLabel != null ? primaryLabel.getText() : null;
    }

    /** Returns secondary label from this Chip if it has been added. Otherwise, it returns null. */
    @Nullable
    public String getSecondaryLabelContent() {
        Text label = getSecondaryLabelContentObject();
        return label != null ? label.getText() : null;
    }

    /** Returns icon id from this Chip if it has been added. Otherwise, it returns null. */
    @Nullable
    public String getIconContent() {
        androidx.wear.tiles.LayoutElementBuilders.Image icon = getIconContentObject();
        return icon != null ? checkNotNull(icon.getResourceId()).getValue() : null;
    }

    @Nullable
    private Text getPrimaryLabelContentObject() {
        return getPrimaryOrSecondaryLabelContent(0);
    }

    @Nullable
    private Text getSecondaryLabelContentObject() {
        return getPrimaryOrSecondaryLabelContent(1);
    }

    @Nullable
    private androidx.wear.tiles.LayoutElementBuilders.Image getIconContentObject() {
        if (!getMetadataTag().equals(METADATA_TAG_ICON)) {
            return null;
        }
        return ((androidx.wear.tiles.LayoutElementBuilders.Image)
                ((androidx.wear.tiles.LayoutElementBuilders.Row) mElement.getContents().get(0))
                        .getContents()
                        .get(0));
    }

    @Nullable
    private Text getPrimaryOrSecondaryLabelContent(int index) {
        String metadataTag = getMetadataTag();
        if (metadataTag.equals(METADATA_TAG_CUSTOM_CONTENT)) {
            return null;
        }
        // In any other case, text (either primary or primary + label) must be present.
        androidx.wear.tiles.LayoutElementBuilders.Column content;
        if (metadataTag.equals(METADATA_TAG_ICON)) {
            content =
                    (androidx.wear.tiles.LayoutElementBuilders.Column)
                            ((androidx.wear.tiles.LayoutElementBuilders.Box)
                                            ((androidx.wear.tiles.LayoutElementBuilders.Row)
                                                            mElement.getContents().get(0))
                                                    .getContents()
                                                    .get(2))
                                    .getContents()
                                    .get(0);
        } else {
            content =
                    (androidx.wear.tiles.LayoutElementBuilders.Column)
                            ((androidx.wear.tiles.LayoutElementBuilders.Box)
                                            mElement.getContents().get(0))
                                    .getContents()
                                    .get(0);
        }

        // We need to check this as this can be the case when we called for label, which doesn't
        // exist.
        return index < content.getContents().size()
                ? Text.fromLayoutElement(
                        ((androidx.wear.tiles.LayoutElementBuilders.Box)
                                        content.getContents().get(index))
                                .getContents()
                                .get(0))
                : null;
    }

    /** Returns the horizontal alignment of the content in this Chip. */
    @androidx.wear.tiles.LayoutElementBuilders.HorizontalAlignment
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
     * Returns Chip object from the given androidx.wear.tiles.LayoutElementBuilders.LayoutElement
     * (e.g. one retrieved from a container's content with {@code
     * container.getContents().get(index)}) if that element can be converted to Chip. Otherwise, it
     * will return null.
     */
    @Nullable
    public static Chip fromLayoutElement(
            @NonNull androidx.wear.tiles.LayoutElementBuilders.LayoutElement element) {
        if (element instanceof Chip) {
            return (Chip) element;
        }
        if (!(element instanceof androidx.wear.tiles.LayoutElementBuilders.Box)) {
            return null;
        }
        androidx.wear.tiles.LayoutElementBuilders.Box boxElement =
                (androidx.wear.tiles.LayoutElementBuilders.Box) element;
        if (!checkTag(boxElement.getModifiers(), Builder.TYPE_TO_TAG.values())) {
            return null;
        }
        // Now we are sure that this element is a Chip.
        return new Chip(boxElement);
    }

    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return mElement.toLayoutElementProto();
    }
}
