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

package androidx.wear.tiles.material;

import static androidx.wear.tiles.ColorBuilders.argb;
import static androidx.wear.tiles.LayoutElementBuilders.TEXT_ALIGN_CENTER;
import static androidx.wear.tiles.LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE_END;
import static androidx.wear.tiles.material.Helper.checkNotNull;
import static androidx.wear.tiles.material.Helper.checkTag;
import static androidx.wear.tiles.material.Helper.getMetadataTagName;
import static androidx.wear.tiles.material.Helper.getTagBytes;
import static androidx.wear.tiles.material.Typography.TYPOGRAPHY_DISPLAY1;
import static androidx.wear.tiles.material.Typography.getFontStyleBuilder;
import static androidx.wear.tiles.material.Typography.getLineHeightForTypography;

import android.content.Context;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.ColorBuilders.ColorProp;
import androidx.wear.tiles.LayoutElementBuilders;
import androidx.wear.tiles.LayoutElementBuilders.FontStyle;
import androidx.wear.tiles.LayoutElementBuilders.FontWeight;
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement;
import androidx.wear.tiles.LayoutElementBuilders.TextAlignment;
import androidx.wear.tiles.LayoutElementBuilders.TextOverflow;
import androidx.wear.tiles.ModifiersBuilders.ElementMetadata;
import androidx.wear.tiles.ModifiersBuilders.Modifiers;
import androidx.wear.tiles.material.Typography.TypographyName;
import androidx.wear.tiles.proto.LayoutElementProto;
import androidx.wear.tiles.proto.ModifiersProto;

/**
 * Tiles component {@link Text} that represents text object holding any information.
 *
 * <p>There are pre-built typography styles that can be obtained from constants in {@link
 * FontStyle}.
 *
 * <p>When accessing the contents of a container for testing, note that this element can't be simply
 * casted back to the original type, i.e.:
 *
 * <pre>{@code
 * Text text = new Text...
 * Box box = new Box.Builder().addContent(text).build();
 *
 * Text myText = (Text) box.getContents().get(0);
 * }</pre>
 *
 * will fail.
 *
 * <p>To be able to get {@link Text} object from any layout element, {@link #fromLayoutElement}
 * method should be used, i.e.:
 *
 * <pre>{@code
 * Text myText = Text.fromLayoutElement(box.getContents().get(0));
 * }</pre>
 */
public class Text implements LayoutElement {
    /** Tool tag for Metadata in Modifiers, so we know that Text is actually a Material Text. */
    static final String METADATA_TAG = "TXT";

    @NonNull private final LayoutElementBuilders.Text mText;

    Text(@NonNull LayoutElementBuilders.Text mText) {
        this.mText = mText;
    }

    /** Builder class for {@link Text}. */
    public static final class Builder implements LayoutElement.Builder {
        @NonNull private final Context mContext;
        @NonNull private String mTextContent = "";
        @NonNull private ColorProp mColor = argb(Colors.DEFAULT.getOnPrimary());
        private @TypographyName int mTypographyName = TYPOGRAPHY_DISPLAY1;
        private boolean mItalic = false;
        private int mMaxLines = 1;
        private boolean mUnderline = false;
        @TextAlignment private int mMultilineAlignment = TEXT_ALIGN_CENTER;
        @NonNull private Modifiers mModifiers = new Modifiers.Builder().build();
        private @TextOverflow int mOverflow = TEXT_OVERFLOW_ELLIPSIZE_END;
        private boolean mIsScalable = true;
        @Nullable private Integer mCustomWeight = null;

        /**
         * Creates a builder for {@link Text}.
         *
         * @param context The application's context.
         * @param text The text content for this component.
         */
        public Builder(@NonNull Context context, @NonNull String text) {
            mContext = context;
            mTextContent = text;
        }

        /**
         * Sets the typography for the {@link Text}. If not set, {@link
         * Typography#TYPOGRAPHY_DISPLAY1} will be used.
         */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        // There is getFontStyle matching getter for this setter as the serialized format of the
        // Tiles do not allow for a direct reconstruction of the all arguments, but it has FontStyle
        // object of that text.
        public Builder setTypography(@TypographyName int typography) {
            this.mTypographyName = typography;
            return this;
        }

        /**
         * Sets whether the text size will change if user has changed the default font size. If not
         * set, true will be used.
         */
        Builder setIsScalable(boolean isScalable) {
            this.mIsScalable = isScalable;
            return this;
        }

        /**
         * Sets the color for the {@link Text}. If not set, onPrimary color from the {@link
         * Colors#DEFAULT} will be used.
         */
        @NonNull
        public Builder setColor(@NonNull ColorProp color) {
            this.mColor = color;
            return this;
        }

        /** Sets the text to be italic. If not set, false will be used. */
        @NonNull
        public Builder setItalic(boolean italic) {
            this.mItalic = italic;
            return this;
        }

        /** Sets the text to be underlined. If not set, false will be used. */
        @NonNull
        public Builder setUnderline(boolean underline) {
            this.mUnderline = underline;
            return this;
        }

        /** Sets the maximum lines of text. If not set, 1 will be used. */
        @NonNull
        public Builder setMaxLines(@IntRange(from = 1) int maxLines) {
            this.mMaxLines = maxLines;
            return this;
        }

        /**
         * Sets the multiline alignment for text within bounds of the Text element. Note that this
         * option has no effect for single line of text, and for that, alignment on the outer
         * container should be used. If not set, {@link TextAlignment#TEXT_ALIGN_CENTER} will be
         * used.
         */
        @NonNull
        public Builder setMultilineAlignment(@TextAlignment int multilineAlignment) {
            this.mMultilineAlignment = multilineAlignment;
            return this;
        }

        /** Sets the modifiers of text. */
        @NonNull
        public Builder setModifiers(@NonNull Modifiers modifiers) {
            this.mModifiers = modifiers;
            return this;
        }

        /**
         * Sets the overflow for text. If not set, {@link TextAlignment#TEXT_OVERFLOW_ELLIPSIZE_END}
         * will be used.
         */
        @NonNull
        public Builder setOverflow(@TextOverflow int overflow) {
            this.mOverflow = overflow;
            return this;
        }

        /**
         * Sets the weight of the font. If not set, default weight for the chosen Typography will be
         * used.
         */
        @NonNull
        public Builder setWeight(@FontWeight int weight) {
            this.mCustomWeight = weight;
            return this;
        }

        /** Constructs and returns {@link Text} with the provided content and look. */
        @NonNull
        @Override
        public Text build() {
            FontStyle.Builder fontStyleBuilder =
                    getFontStyleBuilder(mTypographyName, mContext, mIsScalable)
                            .setColor(mColor)
                            .setItalic(mItalic)
                            .setUnderline(mUnderline);
            if (mCustomWeight != null) {
                fontStyleBuilder.setWeight(mCustomWeight);
            }

            LayoutElementBuilders.Text.Builder text =
                    new LayoutElementBuilders.Text.Builder()
                            .setText(mTextContent)
                            .setFontStyle(fontStyleBuilder.build())
                            .setLineHeight(getLineHeightForTypography(mTypographyName))
                            .setMaxLines(mMaxLines)
                            .setMultilineAlignment(mMultilineAlignment)
                            .setModifiers(addTagToModifiers(mModifiers))
                            .setOverflow(mOverflow);
            return new Text(text.build());
        }

        @NonNull
        static Modifiers addTagToModifiers(Modifiers modifiers) {
            return Modifiers.fromProto(
                    ModifiersProto.Modifiers.newBuilder(modifiers.toProto())
                            .setMetadata(
                                    new ElementMetadata.Builder()
                                            .setTagData(getTagBytes(METADATA_TAG))
                                            .build()
                                            .toProto())
                            .build());
        }
    }

    /** Returns the text of this Text element. */
    @NonNull
    public String getText() {
        return checkNotNull(checkNotNull(mText.getText()).getValue());
    }

    /** Returns the color of this Text element. */
    @NonNull
    public ColorProp getColor() {
        return checkNotNull(checkNotNull(mText.getFontStyle()).getColor());
    }

    /** Returns the font style of this Text element. */
    @NonNull
    public FontStyle getFontStyle() {
        return checkNotNull(mText.getFontStyle());
    }

    /** Returns the line height of this Text element. */
    public float getLineHeight() {
        return checkNotNull(mText.getLineHeight()).getValue();
    }

    /** Returns the max lines of text of this Text element. */
    public int getMaxLines() {
        return checkNotNull(mText.getMaxLines()).getValue();
    }

    /** Returns the multiline alignment of this Text element. */
    @TextAlignment
    public int getMultilineAlignment() {
        return checkNotNull(mText.getMultilineAlignment()).getValue();
    }

    /** Returns the modifiers of this Text element. */
    @NonNull
    public Modifiers getModifiers() {
        return checkNotNull(mText.getModifiers());
    }

    /** Returns the overflow of this Text element. */
    @TextOverflow
    public int getOverflow() {
        return checkNotNull(mText.getOverflow()).getValue();
    }

    /** Returns the overflow of this Text element. */
    @FontWeight
    public int getWeight() {
        return checkNotNull(checkNotNull(mText.getFontStyle()).getWeight()).getValue();
    }

    /** Returns whether the Text is in italic. */
    public boolean isItalic() {
        return checkNotNull(checkNotNull(mText.getFontStyle()).getItalic()).getValue();
    }

    /** Returns whether the Text is underlined. */
    public boolean isUnderline() {
        return checkNotNull(checkNotNull(mText.getFontStyle()).getUnderline()).getValue();
    }

    /** Returns metadata tag set to this Text, which should be {@link #METADATA_TAG}. */
    @NonNull
    String getMetadataTag() {
        return getMetadataTagName(checkNotNull(checkNotNull(getModifiers()).getMetadata()));
    }

    /**
     * Returns Material Text object from the given LayoutElement (e.g. one retrieved from a
     * container's content with {@code container.getContents().get(index)}) if that element can be
     * converted to Material Text. Otherwise, it will return null.
     */
    @Nullable
    public static Text fromLayoutElement(@NonNull LayoutElement element) {
        if (element instanceof Text) {
            return (Text) element;
        }
        if (!(element instanceof LayoutElementBuilders.Text)) {
            return null;
        }
        LayoutElementBuilders.Text textElement = (LayoutElementBuilders.Text) element;
        if (!checkTag(textElement.getModifiers(), METADATA_TAG)) {
            return null;
        }
        // Now we are sure that this element is a Material Text.
        return new Text(textElement);
    }

    /** @hide */
    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return mText.toLayoutElementProto();
    }
}
