/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout.materialcore;

import static androidx.wear.protolayout.ColorBuilders.argb;
import static androidx.wear.protolayout.DimensionBuilders.dp;
import static androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_START;
import static androidx.wear.protolayout.materialcore.Helper.checkNotNull;
import static androidx.wear.protolayout.materialcore.Helper.checkTag;
import static androidx.wear.protolayout.materialcore.Helper.getMetadataTagName;
import static androidx.wear.protolayout.materialcore.Helper.getTagBytes;
import static androidx.wear.protolayout.materialcore.Helper.radiusOf;

import static java.lang.Math.max;

import android.annotation.SuppressLint;
import android.graphics.Color;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.ColorBuilders.ColorProp;
import androidx.wear.protolayout.DimensionBuilders.ContainerDimension;
import androidx.wear.protolayout.DimensionBuilders.DpProp;
import androidx.wear.protolayout.DimensionBuilders.WrappedDimensionProp;
import androidx.wear.protolayout.LayoutElementBuilders;
import androidx.wear.protolayout.LayoutElementBuilders.Box;
import androidx.wear.protolayout.LayoutElementBuilders.Column;
import androidx.wear.protolayout.LayoutElementBuilders.HorizontalAlignment;
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement;
import androidx.wear.protolayout.LayoutElementBuilders.Row;
import androidx.wear.protolayout.LayoutElementBuilders.Spacer;
import androidx.wear.protolayout.ModifiersBuilders.Background;
import androidx.wear.protolayout.ModifiersBuilders.Clickable;
import androidx.wear.protolayout.ModifiersBuilders.Corner;
import androidx.wear.protolayout.ModifiersBuilders.ElementMetadata;
import androidx.wear.protolayout.ModifiersBuilders.Modifiers;
import androidx.wear.protolayout.ModifiersBuilders.Padding;
import androidx.wear.protolayout.ModifiersBuilders.Semantics;
import androidx.wear.protolayout.TypeBuilders.StringProp;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.proto.LayoutElementProto;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;

/**
 * ProtoLayout core component {@link Chip} that represents clickable object with the text, optional
 * label and optional icon or with custom content. This component is not meant to be used
 * standalone, it's a helper component for the Material library.
 *
 * <p>The Chip is Stadium shape object. The recommended sizes and styles are defined in the public
 * Material library.
 *
 * <p>This Button doesn't have any styling applied, that should be done by the calling library.
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
 */
public class Chip implements LayoutElement {
    /**
     * Tool tag for Metadata in Modifiers, so we know that Box is actually a Chip with only text.
     */
    public static final String METADATA_TAG_TEXT = "TXTCHP";

    /** Tool tag for Metadata in Modifiers, so we know that Box is actually a Chip with icon. */
    public static final String METADATA_TAG_ICON = "ICNCHP";

    /**
     * Tool tag for Metadata in Modifiers, so we know that Box is actually a Chip with custom
     * content.
     */
    public static final String METADATA_TAG_CUSTOM_CONTENT = "CSTCHP";

    private static final int PRIMARY_LABEL_INDEX = 0;
    private static final int SECONDARY_LABEL_INDEX = 1;
    private static final int LABELS_INDEX_NO_ICON = 0;
    private static final int LABELS_INDEX_ICON = 2;

    /** Outer tappable Box. */
    @NonNull private final Box mImpl;

    /** Inner visible Box with all Chip elements. */
    @NonNull private final Box mElement;

    Chip(@NonNull Box impl) {
        mImpl = impl;
        mElement = (Box) impl.getContents().get(0);
    }

    /** Builder class for {@link Chip}. */
    public static final class Builder implements LayoutElement.Builder {
        /** Chip type that has no inner set. */
        public static final int NOT_SET = 0;

        /** Chip type to be used when setting a content which has a text. */
        public static final int TEXT = 1;

        /** Chip type to be used when setting a content which has an icon. */
        public static final int ICON = 2;

        /** Chip type to be used when setting a content which is a custom one. */
        public static final int CUSTOM_CONTENT = 3;

        /** Chip types. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({NOT_SET, TEXT, ICON, CUSTOM_CONTENT})
        public @interface ChipType {}

        @Nullable private LayoutElement mCustomContent;
        @Nullable private LayoutElement mIconContent = null;
        @Nullable private LayoutElement mPrimaryLabelContent = null;
        @Nullable private LayoutElement mSecondaryLabelContent = null;
        @NonNull private final Clickable mClickable;
        @Nullable private StringProp mContentDescription = null;
        @NonNull private ContainerDimension mWidth = dp(0);
        @NonNull private DpProp mHeight = dp(0);
        @NonNull private ColorProp mBackgroundColor = argb(Color.BLACK);
        @HorizontalAlignment private int mHorizontalAlign = HORIZONTAL_ALIGN_START;
        @NonNull private DpProp mHorizontalPadding = dp(0);
        @NonNull private DpProp mIconSpacerWidth = dp(0);
        @NonNull private DpProp mMinTappableSquareLength = dp(0);

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
         * @param clickable Associated {@link Clickable} for click events. When the Chip is clicked
         *     it will fire the associated action.
         */
        public Builder(@NonNull Clickable clickable) {
            mClickable = clickable;
        }

        /** Sets the width of {@link Chip}. If not set, Chip won't be shown. */
        @NonNull
        public Builder setWidth(@NonNull ContainerDimension width) {
            mWidth = width;
            return this;
        }

        /** Sets the height of {@link Chip}. If not set, Chip won't be shown. */
        @NonNull
        public Builder setHeight(@NonNull DpProp height) {
            mHeight = height;
            return this;
        }

        /**
         * Sets the custom content for the {@link Chip}. Any previously added content will be
         * overridden. Provided content should be styled and sized.
         */
        @NonNull
        public Builder setCustomContent(@NonNull LayoutElement content) {
            this.mCustomContent = content;
            this.mPrimaryLabelContent = null;
            this.mSecondaryLabelContent = null;
            this.mIconContent = null;
            return this;
        }

        /** Sets the background colors for the {@link Button}. If not set, black is used. */
        @NonNull
        public Builder setBackgroundColor(@NonNull ColorProp backgroundColor) {
            mBackgroundColor = backgroundColor;
            return this;
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
         * overridden. This should be styled and sized by the caller.
         */
        @NonNull
        public Builder setPrimaryLabelContent(@NonNull LayoutElement primaryLabel) {
            this.mPrimaryLabelContent = primaryLabel;
            this.mCustomContent = null;
            return this;
        }

        /**
         * Sets the secondary label for the {@link Chip}. Any previously added custom content will
         * be overridden. If secondary label is set, primary label must be set too with {@link
         * #setPrimaryLabelContent}. This should be styled and sized by the caller.
         */
        @NonNull
        public Builder setSecondaryLabelContent(@NonNull LayoutElement secondaryLabel) {
            this.mSecondaryLabelContent = secondaryLabel;
            this.mCustomContent = null;
            return this;
        }

        /**
         * Sets the icon for the {@link Chip}. Any previously added custom content will be
         * overridden. If icon is set, primary label must be set too with {@link
         * #setPrimaryLabelContent}. This should be styled and sized by the caller.
         */
        @NonNull
        public Builder setIconContent(@NonNull LayoutElement imageResourceId) {
            this.mIconContent = imageResourceId;
            this.mCustomContent = null;
            return this;
        }

        /**
         * Sets the horizontal alignment in the chip. If not set, {@link
         * HorizontalAlignment#HORIZONTAL_ALIGN_START} will be used.
         */
        @NonNull
        public Builder setHorizontalAlignment(@HorizontalAlignment int horizontalAlignment) {
            mHorizontalAlign = horizontalAlignment;
            return this;
        }

        /** Sets the width of spacer used next to the icon if set. */
        @NonNull
        public Builder setIconSpacerWidth(@NonNull DpProp iconSpacerWidth) {
            mIconSpacerWidth = iconSpacerWidth;
            return this;
        }

        /** Sets the length of minimal tappable square for this chip. */
        @NonNull
        public Builder setMinimalTappableSquareLength(@NonNull DpProp tappableLength) {
            mMinTappableSquareLength = tappableLength;
            return this;
        }

        /** Sets the horizontal padding in the chip. */
        @NonNull
        public Builder setHorizontalPadding(@NonNull DpProp horizontalPadding) {
            this.mHorizontalPadding = horizontalPadding;
            return this;
        }

        /** Constructs and returns {@link Chip} with the provided content and look. */
        @NonNull
        @Override
        public Chip build() {
            Modifiers.Builder modifiers =
                    new Modifiers.Builder()
                            .setPadding(
                                    new Padding.Builder()
                                            .setStart(mHorizontalPadding)
                                            .setEnd(mHorizontalPadding)
                                            .build())
                            .setBackground(
                                    new Background.Builder()
                                            .setColor(mBackgroundColor)
                                            .setCorner(
                                                    new Corner.Builder()
                                                            .setRadius(radiusOf(mHeight))
                                                            .build())
                                            .build());

            Box.Builder visible =
                    new Box.Builder()
                            .setHeight(mHeight)
                            .setWidth(mWidth)
                            .setHorizontalAlignment(mHorizontalAlign)
                            .addContent(getCorrectContent())
                            .setModifiers(modifiers.build());

            Box tappable =
                    new Box.Builder()
                            .setWidth(resolveMinTappableWidth())
                            .setHeight(dp(resolveMinTappableHeight()))
                            .setModifiers(
                                    new Modifiers.Builder()
                                            .setClickable(mClickable)
                                            .setMetadata(getCorrectMetadataTag())
                                            .setSemantics(
                                                    new Semantics.Builder()
                                                            .setContentDescription(
                                                                    getCorrectContentDescription())
                                                            .build())
                                            .build())
                            .addContent(visible.build())
                            .build();

            return new Chip(tappable);
        }

        private ContainerDimension resolveMinTappableWidth() {
            if (mWidth instanceof DpProp) {
                return dp(max(((DpProp) mWidth).getValue(), mMinTappableSquareLength.getValue()));
            } else if (mWidth instanceof WrappedDimensionProp) {
                return new WrappedDimensionProp.Builder()
                        .setMinimumSize(mMinTappableSquareLength)
                        .build();
            } else {
                return mWidth;
            }
        }

        private float resolveMinTappableHeight() {
            return max(mHeight.getValue(), mMinTappableSquareLength.getValue());
        }

        @NonNull
        private StringProp getCorrectContentDescription() {
            if (mContentDescription == null) {
                String staticValue = "";
                if (mPrimaryLabelContent != null) {
                    staticValue += mPrimaryLabelContent;
                }
                if (mSecondaryLabelContent != null) {
                    staticValue += "\n" + mSecondaryLabelContent;
                }
                mContentDescription = new StringProp.Builder(staticValue).build();
            }
            return checkNotNull(mContentDescription);
        }

        private ElementMetadata getCorrectMetadataTag() {
            String tag = METADATA_TAG_TEXT;
            if (mCustomContent != null) {
                tag = METADATA_TAG_CUSTOM_CONTENT;
            } else if (mIconContent != null) {
                tag = METADATA_TAG_ICON;
            }
            return new ElementMetadata.Builder().setTagData(getTagBytes(tag)).build();
        }

        @SuppressLint("CheckResult") // (b/247804720)
        @NonNull
        private LayoutElement getCorrectContent() {
            if (mCustomContent != null) {
                return mCustomContent;
            }

            Column.Builder column =
                    new Column.Builder()
                            .setHorizontalAlignment(HORIZONTAL_ALIGN_START)
                            .addContent(putLayoutInBox(checkNotNull(mPrimaryLabelContent)).build());

            if (mSecondaryLabelContent != null) {
                column.addContent(putLayoutInBox(mSecondaryLabelContent).build());
            }

            Box labels = putLayoutInBox(column.build()).build();
            if (mIconContent == null) {
                return labels;
            } else {
                return new Row.Builder()
                        .addContent(mIconContent)
                        .addContent(
                                new Spacer.Builder()
                                        .setHeight(mHeight)
                                        .setWidth(mIconSpacerWidth)
                                        .build())
                        .addContent(labels)
                        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                        .build();
            }
        }

        private Box.Builder putLayoutInBox(@NonNull LayoutElement element) {
            // Wrapped and centered content are default.
            return new Box.Builder().addContent(element);
        }
    }

    /** Returns the visible height of this Chip. */
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
        return checkNotNull(checkNotNull(mImpl.getModifiers()).getClickable());
    }

    /** Returns background color of this Chip. */
    @NonNull
    public ColorProp getBackgroundColor() {
        return checkNotNull(
                checkNotNull(checkNotNull(mElement.getModifiers()).getBackground()).getColor());
    }

    /** Returns content description of this Chip. */
    @Nullable
    public StringProp getContentDescription() {
        Semantics semantics = checkNotNull(mImpl.getModifiers()).getSemantics();
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

    /** Returns primary label from this Chip if it has been added. Otherwise, it returns null. */
    @Nullable
    public LayoutElement getPrimaryLabelContent() {
        return getPrimaryOrSecondaryLabelContent(PRIMARY_LABEL_INDEX);
    }

    /** Returns secondary label from this Chip if it has been added. Otherwise, it returns null. */
    @Nullable
    public LayoutElement getSecondaryLabelContent() {
        return getPrimaryOrSecondaryLabelContent(SECONDARY_LABEL_INDEX);
    }

    /** Returns icon id from this Chip if it has been added. Otherwise, it returns null. */
    @Nullable
    public LayoutElement getIconContent() {
        if (!getMetadataTag().equals(METADATA_TAG_ICON)) {
            return null;
        }
        return ((Row) mElement.getContents().get(0)).getContents().get(0);
    }

    @Nullable
    private LayoutElement getPrimaryOrSecondaryLabelContent(int index) {
        String metadataTag = getMetadataTag();
        if (metadataTag.equals(METADATA_TAG_CUSTOM_CONTENT)) {
            return null;
        }

        // In any other case, text (either primary or primary + label) must be present.
        Column content;
        if (metadataTag.equals(METADATA_TAG_ICON)) {
            content =
                    (Column)
                            ((Box)
                                            ((Row) mElement.getContents().get(0))
                                                    .getContents()
                                                    .get(LABELS_INDEX_ICON))
                                    .getContents()
                                    .get(0);
        } else {
            content =
                    (Column)
                            ((Box) mElement.getContents().get(0))
                                    .getContents()
                                    .get(LABELS_INDEX_NO_ICON);
        }

        // We need to check this as this can be the case when we called for label, which doesn't
        // exist.
        return index < content.getContents().size()
                ? ((Box) content.getContents().get(index)).getContents().get(0)
                : null;
    }

    /** Returns the horizontal alignment of the content in this Chip. */
    @HorizontalAlignment
    public int getHorizontalAlignment() {
        return checkNotNull(mElement.getHorizontalAlignment()).getValue();
    }

    /** Returns metadata tag set to this Chip. */
    @NonNull
    public String getMetadataTag() {
        return getMetadataTagName(checkNotNull(checkNotNull(mImpl.getModifiers()).getMetadata()));
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

    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return mImpl.toLayoutElementProto();
    }

    @Nullable
    @Override
    public Fingerprint getFingerprint() {
        return mImpl.getFingerprint();
    }
}
