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

package androidx.wear.protolayout.material;

import static androidx.annotation.Dimension.DP;
import static androidx.wear.protolayout.DimensionBuilders.dp;
import static androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER;
import static androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_START;
import static androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_UNDEFINED;
import static androidx.wear.protolayout.material.ChipDefaults.DEFAULT_HEIGHT;
import static androidx.wear.protolayout.material.ChipDefaults.DEFAULT_MARGIN_PERCENT;
import static androidx.wear.protolayout.material.ChipDefaults.HORIZONTAL_PADDING;
import static androidx.wear.protolayout.material.ChipDefaults.ICON_SIZE;
import static androidx.wear.protolayout.material.ChipDefaults.ICON_SPACER_WIDTH;
import static androidx.wear.protolayout.material.ChipDefaults.PRIMARY_COLORS;
import static androidx.wear.protolayout.materialcore.Chip.METADATA_TAG_CUSTOM_CONTENT;
import static androidx.wear.protolayout.materialcore.Chip.METADATA_TAG_ICON;
import static androidx.wear.protolayout.materialcore.Helper.checkNotNull;
import static androidx.wear.protolayout.materialcore.Helper.staticString;

import android.content.Context;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.ColorBuilders.ColorProp;
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.protolayout.DimensionBuilders.ContainerDimension;
import androidx.wear.protolayout.DimensionBuilders.DpProp;
import androidx.wear.protolayout.LayoutElementBuilders;
import androidx.wear.protolayout.LayoutElementBuilders.ColorFilter;
import androidx.wear.protolayout.LayoutElementBuilders.HorizontalAlignment;
import androidx.wear.protolayout.LayoutElementBuilders.Image;
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement;
import androidx.wear.protolayout.ModifiersBuilders.Clickable;
import androidx.wear.protolayout.TypeBuilders.StringProp;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.expression.ProtoLayoutExperimental;
import androidx.wear.protolayout.material.Typography.TypographyName;
import androidx.wear.protolayout.proto.LayoutElementProto;

/**
 * ProtoLayout component {@link Chip} that represents clickable object with the text, optional label
 * and optional icon or with custom content.
 *
 * <p>The Chip is Stadium shape that has a max height designed to take no more than two lines of
 * text of {@link Typography#TYPOGRAPHY_BUTTON} style and with minimum tap target to meet
 * accessibility requirements. The {@link Chip} can have an icon horizontally parallel to the two
 * lines of text. Width of chip can very, and the recommended size is screen dependent with the
 * recommended margin being applied.
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
 * @see androidx.wear.protolayout.material.layouts.PrimaryLayout.Builder#setContent if this Chip is
 *     used inside of {@link androidx.wear.protolayout.material.layouts.PrimaryLayout}.
 */
public class Chip implements LayoutElement {
    @NonNull private final androidx.wear.protolayout.materialcore.Chip mElement;

    Chip(@NonNull androidx.wear.protolayout.materialcore.Chip element) {
        mElement = element;
    }

    /** Builder class for {@link androidx.wear.protolayout.material.Chip}. */
    public static final class Builder implements LayoutElement.Builder {
        @NonNull private final Context mContext;
        @Nullable private LayoutElement mCustomContent;
        @Nullable private String mImageResourceId = null;
        @Nullable private String mPrimaryLabel = null;
        @Nullable private String mSecondaryLabel = null;
        @Nullable private StringProp mContentDescription = null;
        @NonNull private ChipColors mChipColors = PRIMARY_COLORS;
        @NonNull private DpProp mIconSize = ICON_SIZE;
        @HorizontalAlignment private int mHorizontalAlign = HORIZONTAL_ALIGN_UNDEFINED;
        @TypographyName private int mPrimaryLabelTypography;
        private boolean mIsScalable = true;
        private boolean mIsFontPaddingExcluded = false;
        private int mMaxLines = 0; // 0 indicates that is not set.
        @NonNull private final androidx.wear.protolayout.materialcore.Chip.Builder mCoreBuilder;

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
            float width =
                    (100 - 2 * DEFAULT_MARGIN_PERCENT) * deviceParameters.getScreenWidthDp() / 100;
            mPrimaryLabelTypography = Typography.TYPOGRAPHY_BUTTON;
            mCoreBuilder = new androidx.wear.protolayout.materialcore.Chip.Builder(clickable);
            mCoreBuilder.setWidth(dp(width));
            mCoreBuilder.setHorizontalPadding(HORIZONTAL_PADDING);
            mCoreBuilder.setHeight(DEFAULT_HEIGHT);
            mCoreBuilder.setBackgroundColor(mChipColors.getBackgroundColor());
            mCoreBuilder.setMinimalTappableSquareLength(ChipDefaults.MIN_TAPPABLE_SQUARE_LENGTH);
            mCoreBuilder.setIconSpacerWidth(ICON_SPACER_WIDTH);
        }

        /**
         * Sets the width of {@link Chip}. If not set, default value will be set to fill the screen.
         */
        @NonNull
        public Builder setWidth(@NonNull ContainerDimension width) {
            mCoreBuilder.setWidth(width);
            return this;
        }

        /**
         * Sets the width of {@link TitleChip}. If not set, default value will be set to fill the
         * screen.
         */
        @NonNull
        public Builder setWidth(@Dimension(unit = DP) float width) {
            return setWidth(dp(width));
        }

        /**
         * Sets the custom content for the {@link Chip}. Any previously added content will be
         * overridden.
         */
        @NonNull
        public Builder setCustomContent(@NonNull LayoutElement content) {
            this.mCustomContent = content;
            this.mPrimaryLabel = null;
            this.mSecondaryLabel = null;
            this.mImageResourceId = null;
            return this;
        }

        /**
         * Sets the static content description for the {@link Chip}. It is highly recommended to
         * provide this for chip containing icon.
         */
        @NonNull
        public Builder setContentDescription(@NonNull CharSequence contentDescription) {
            return setContentDescription(staticString(contentDescription.toString()));
        }

        /**
         * Sets the content description for the {@link Chip}. It is highly recommended to provide
         * this for chip containing icon.
         *
         * <p>While this field is statically accessible from 1.0, it's only bindable since version
         * 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
         */
        @NonNull
        public Builder setContentDescription(@NonNull StringProp contentDescription) {
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
         * Used for creating {@code CompactChip} and {@code TitleChip}.
         *
         * <p>Sets the font for the primary label and should only be used internally.
         */
        @NonNull
        Builder setPrimaryLabelTypography(@TypographyName int typography) {
            this.mPrimaryLabelTypography = typography;
            return this;
        }

        /**
         * Used for creating {@code CompactChip} and {@code TitleChip}.
         *
         * <p>Sets the icon size and should only be used internally.
         */
        @NonNull
        Builder setIconSize(@NonNull DpProp size) {
            this.mIconSize = size;
            return this;
        }

        /**
         * Used for creating {@code CompactChip} and {@code TitleChip}.
         *
         * <p>Sets whether the font for the primary label is scalable.
         */
        @NonNull
        Builder setIsPrimaryLabelScalable(boolean isScalable) {
            this.mIsScalable = isScalable;
            return this;
        }

        /**
         * Sets whether the font padding for the primary label is excluded.
         *
         * <p>It should be used for creating {@code CompactChip} and {@code TitleChip} to make the
         * label vertically aligned. Shouldn't be used if there is anything else in chip besides
         * primary label.
         *
         * @see Text.Builder#setExcludeFontPadding
         */
        @NonNull
        @ProtoLayoutExperimental
        @SuppressWarnings("MissingGetterMatchingBuilder")
        Builder setPrimaryLabelExcludeFontPadding(boolean excluded) {
            this.mIsFontPaddingExcluded = excluded;
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
            mCoreBuilder.setBackgroundColor(chipColors.getBackgroundColor());
            return this;
        }

        /**
         * Sets the horizontal alignment in the chip. It is strongly recommended that the content of
         * the chip is start-aligned if there is more than primary text in it. By default, {@link
         * HorizontalAlignment#HORIZONTAL_ALIGN_CENTER} will be used when only a primary label is
         * present. Otherwise {@link HorizontalAlignment#HORIZONTAL_ALIGN_START} will be used.
         */
        @NonNull
        public Builder setHorizontalAlignment(@HorizontalAlignment int horizontalAlignment) {
            mHorizontalAlign = horizontalAlignment;
            return this;
        }

        /** Used for creating {@code CompactChip} and {@code TitleChip}. */
        @NonNull
        Builder setHorizontalPadding(@NonNull DpProp horizontalPadding) {
            mCoreBuilder.setHorizontalPadding(horizontalPadding);
            return this;
        }

        /** Used for creating {@code CompactChip} and {@code TitleChip}. */
        @NonNull
        Builder setHeight(@NonNull DpProp height) {
            mCoreBuilder.setHeight(height);
            return this;
        }

        /** Used for creating {@code CompactChip} and {@code TitleChip}. */
        @NonNull
        Builder setMaxLines(int maxLines) {
            this.mMaxLines = maxLines;
            return this;
        }

        /** Constructs and returns {@link Chip} with the provided content and look. */
        @NonNull
        @Override
        public Chip build() {
            mCoreBuilder.setContentDescription(getCorrectContentDescription());
            mCoreBuilder.setHorizontalAlignment(getCorrectHorizontalAlignment());

            if (mCustomContent != null) {
                mCoreBuilder.setCustomContent(mCustomContent);
            } else {
                setCorrectContent();
            }

            return new Chip(mCoreBuilder.build());
        }

        @NonNull
        private StringProp getCorrectContentDescription() {
            if (mContentDescription == null) {
                String staticValue = "";
                if (mPrimaryLabel != null) {
                    staticValue += mPrimaryLabel;
                }
                if (mSecondaryLabel != null) {
                    staticValue += "\n" + mSecondaryLabel;
                }
                mContentDescription = new StringProp.Builder(staticValue).build();
            }
            return checkNotNull(mContentDescription);
        }

        @HorizontalAlignment
        private int getCorrectHorizontalAlignment() {
            if (mHorizontalAlign != HORIZONTAL_ALIGN_UNDEFINED) {
                return mHorizontalAlign;
            }
            if (mPrimaryLabel != null && mSecondaryLabel == null && mImageResourceId == null) {
                return HORIZONTAL_ALIGN_CENTER;
            } else {
                return HORIZONTAL_ALIGN_START;
            }
        }

        @OptIn(markerClass = ProtoLayoutExperimental.class)
        private void setCorrectContent() {
            Text mainTextElement =
                    new Text.Builder(mContext, checkNotNull(mPrimaryLabel))
                            .setTypography(mPrimaryLabelTypography)
                            .setColor(mChipColors.getContentColor())
                            .setMaxLines(getCorrectMaxLines())
                            .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE_END)
                            .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_START)
                            .setIsScalable(mIsScalable)
                            .setExcludeFontPadding(mIsFontPaddingExcluded)
                            .build();

            mCoreBuilder.setPrimaryLabelContent(mainTextElement);

            if (mSecondaryLabel != null) {
                Text labelTextElement =
                        new Text.Builder(mContext, mSecondaryLabel)
                                .setTypography(Typography.TYPOGRAPHY_CAPTION2)
                                .setColor(mChipColors.getSecondaryContentColor())
                                .setMaxLines(1)
                                .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE_END)
                                .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_START)
                                .build();
                mCoreBuilder.setSecondaryLabelContent(labelTextElement);
            }

            if (mImageResourceId != null) {
                Image icon =
                        new Image.Builder()
                                .setResourceId(mImageResourceId)
                                .setWidth(mIconSize)
                                .setHeight(mIconSize)
                                .setColorFilter(
                                        new ColorFilter.Builder()
                                                .setTint(mChipColors.getIconColor())
                                                .build())
                                .build();
                mCoreBuilder.setIconContent(icon);
            }
        }

        private int getCorrectMaxLines() {
            if (mMaxLines > 0) {
                return mMaxLines;
            }
            return mSecondaryLabel != null ? 1 : 2;
        }
    }

    /** Returns the visible height of this Chip. */
    @NonNull
    public ContainerDimension getHeight() {
        return mElement.getHeight();
    }

    /** Returns width of this Chip. */
    @NonNull
    public ContainerDimension getWidth() {
        return mElement.getWidth();
    }

    /** Returns click event action associated with this Chip. */
    @NonNull
    public Clickable getClickable() {
        return mElement.getClickable();
    }

    /** Returns chip colors of this Chip. */
    @NonNull
    public ChipColors getChipColors() {
        ColorProp backgroundColor = mElement.getBackgroundColor();
        ColorProp contentColor = null;
        ColorProp secondaryContentColor = null;
        ColorProp iconTintColor = null;

        if (!getMetadataTag().equals(METADATA_TAG_CUSTOM_CONTENT)) {
            if (getMetadataTag().equals(METADATA_TAG_ICON)) {
                Image icon = checkNotNull(getIconContentObject());
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
            contentColor = new ColorProp.Builder(0).build();
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
    public StringProp getContentDescription() {
        return mElement.getContentDescription();
    }

    /** Returns custom content from this Chip if it has been added. Otherwise, it returns null. */
    @Nullable
    public LayoutElement getCustomContent() {
        return mElement.getCustomContent();
    }

    /** Returns primary label from this Chip if it has been added. Otherwise, it returns null. */
    @Nullable
    public String getPrimaryLabelContent() {
        Text primaryLabel = getPrimaryLabelContentObject();
        return primaryLabel != null ? primaryLabel.getText().getValue() : null;
    }

    /** Returns secondary label from this Chip if it has been added. Otherwise, it returns null. */
    @Nullable
    public String getSecondaryLabelContent() {
        Text secondaryLabel = getSecondaryLabelContentObject();
        return secondaryLabel != null ? secondaryLabel.getText().getValue() : null;
    }

    /** Returns icon id from this Chip if it has been added. Otherwise, it returns null. */
    @Nullable
    public String getIconContent() {
        Image icon = getIconContentObject();
        return icon != null ? checkNotNull(icon.getResourceId()).getValue() : null;
    }

    @Nullable
    private Text getPrimaryLabelContentObject() {
        LayoutElement content = mElement.getPrimaryLabelContent();
        if (content != null) {
            return Text.fromLayoutElement(content);
        }
        return null;
    }

    @Nullable
    private Text getSecondaryLabelContentObject() {
        LayoutElement content = mElement.getSecondaryLabelContent();
        if (content != null) {
            return Text.fromLayoutElement(content);
        }
        return null;
    }

    @Nullable
    private Image getIconContentObject() {
        LayoutElement content = mElement.getIconContent();
        return content instanceof Image ? (Image) content : null;
    }

    /** Returns the horizontal alignment of the content in this Chip. */
    @HorizontalAlignment
    public int getHorizontalAlignment() {
        return mElement.getHorizontalAlignment();
    }

    /** Returns metadata tag set to this Chip. */
    @NonNull
    String getMetadataTag() {
        return mElement.getMetadataTag();
    }

    /** Returns whether the font padding for the primary label is excluded. */
    @ProtoLayoutExperimental
    boolean hasPrimaryLabelExcludeFontPadding() {
        Text primaryLabel = getPrimaryLabelContentObject();
        return primaryLabel != null && primaryLabel.hasExcludeFontPadding();
    }

    /**
     * Returns Chip object from the given LayoutElement (e.g. one retrieved from a container's
     * content with {@code container.getContents().get(index)}) if that element can be converted to
     * Chip. Otherwise, it will return null.
     */
    @Nullable
    public static Chip fromLayoutElement(@NonNull LayoutElement element) {
        if (element instanceof Chip) {
            return (Chip) element;
        }
        androidx.wear.protolayout.materialcore.Chip coreChip =
                androidx.wear.protolayout.materialcore.Chip.fromLayoutElement(element);
        return coreChip == null ? null : new Chip(coreChip);
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
