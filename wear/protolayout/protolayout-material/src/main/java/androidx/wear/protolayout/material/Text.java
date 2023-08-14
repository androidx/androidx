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

package androidx.wear.protolayout.material;

import static androidx.wear.protolayout.ColorBuilders.argb;
import static androidx.wear.protolayout.LayoutElementBuilders.TEXT_ALIGN_CENTER;
import static androidx.wear.protolayout.LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE_END;
import static androidx.wear.protolayout.material.Typography.TYPOGRAPHY_DISPLAY1;
import static androidx.wear.protolayout.material.Typography.getFontStyleBuilder;
import static androidx.wear.protolayout.material.Typography.getLineHeightForTypography;
import static androidx.wear.protolayout.materialcore.Helper.checkNotNull;
import static androidx.wear.protolayout.materialcore.Helper.staticString;

import android.content.Context;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.ColorBuilders.ColorProp;
import androidx.wear.protolayout.LayoutElementBuilders;
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle;
import androidx.wear.protolayout.LayoutElementBuilders.FontWeight;
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement;
import androidx.wear.protolayout.LayoutElementBuilders.TextAlignment;
import androidx.wear.protolayout.LayoutElementBuilders.TextOverflow;
import androidx.wear.protolayout.ModifiersBuilders.Modifiers;
import androidx.wear.protolayout.TypeBuilders.StringLayoutConstraint;
import androidx.wear.protolayout.TypeBuilders.StringProp;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.expression.ProtoLayoutExperimental;
import androidx.wear.protolayout.material.Typography.TypographyName;
import androidx.wear.protolayout.proto.LayoutElementProto;

/**
 * ProtoLayout component {@link Text} that represents text object holding any information.
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

    @NonNull private final LayoutElementBuilders.Text mText;

    Text(@NonNull LayoutElementBuilders.Text mText) {
        this.mText = mText;
    }

    /** Builder class for {@link Text}. */
    public static final class Builder implements LayoutElement.Builder {
        @NonNull private final Context mContext;
        @NonNull private ColorProp mColor = argb(Colors.DEFAULT.getOnPrimary());
        private @TypographyName int mTypographyName = TYPOGRAPHY_DISPLAY1;
        private boolean mItalic = false;
        private boolean mUnderline = false;
        private boolean mIsScalable = true;
        @Nullable private Integer mCustomWeight = null;

        @NonNull
        private final LayoutElementBuilders.Text.Builder mElementBuilder =
                new LayoutElementBuilders.Text.Builder()
                        .setMaxLines(1)
                        .setMultilineAlignment(TEXT_ALIGN_CENTER)
                        .setOverflow(TEXT_OVERFLOW_ELLIPSIZE_END);

        /**
         * Creates a builder for a {@link Text} component with static text.
         *
         * @param context The application's context.
         * @param text The text content for this component.
         */
        public Builder(@NonNull Context context, @NonNull String text) {
            mContext = context;
            mElementBuilder.setText(staticString(text));
        }

        /**
         * Creates a builder for a {@link Text}.
         *
         * <p>While this component is statically accessible from 1.0, it's only bindable since
         * version 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
         *
         * @param context The application's context.
         * @param text The text content for this component.
         * @param stringLayoutConstraint Layout constraints used to correctly measure Text view size
         *     and align text when {@code text} has dynamic value.
         */
        public Builder(
                @NonNull Context context,
                @NonNull StringProp text,
                @NonNull StringLayoutConstraint stringLayoutConstraint) {
            mContext = context;
            mElementBuilder.setText(text);
            mElementBuilder.setLayoutConstraintsForDynamicText(stringLayoutConstraint);
        }

        /**
         * Sets the typography for the {@link Text}. If not set, {@link
         * Typography#TYPOGRAPHY_DISPLAY1} will be used.
         */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        // There is getFontStyle matching getter for this setter as the serialized format of the
        // ProtoLayout do not allow for a direct reconstruction of the all arguments, but it has
        // FontStyle object of that text.
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
            this.mElementBuilder.setMaxLines(maxLines);
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
            this.mElementBuilder.setMultilineAlignment(multilineAlignment);
            return this;
        }

        /** Sets the modifiers of text. */
        @NonNull
        public Builder setModifiers(@NonNull Modifiers modifiers) {
            this.mElementBuilder.setModifiers(modifiers);
            return this;
        }

        /**
         * Sets the overflow for text. If not set, {@link TextAlignment#TEXT_OVERFLOW_ELLIPSIZE_END}
         * will be used.
         */
        @NonNull
        public Builder setOverflow(@TextOverflow int overflow) {
            this.mElementBuilder.setOverflow(overflow);
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

        /**
         * Sets whether the {@link Text} excludes extra top and bottom padding above the normal
         * ascent and descent. The default is false.
         */
        // TODO(b/252767963): Coordinate the transition of the default from false->true along with
        // other impacted UI Libraries - needs care as will have an impact on layout and needs to be
        // communicated clearly.
        @NonNull
        @ProtoLayoutExperimental
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public Builder setExcludeFontPadding(boolean excludeFontPadding) {
            this.mElementBuilder.setAndroidTextStyle(
                    new LayoutElementBuilders.AndroidTextStyle.Builder()
                            .setExcludeFontPadding(excludeFontPadding)
                            .build());
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
            mElementBuilder
                    .setFontStyle(fontStyleBuilder.build())
                    .setLineHeight(getLineHeightForTypography(mTypographyName));
            return new Text(mElementBuilder.build());
        }
    }

    /** Returns the text of this Text element. */
    @NonNull
    public StringProp getText() {
        return checkNotNull(mText.getText());
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

    /**
     * Returns whether the Text has extra top and bottom padding above the normal ascent and descent
     * excluded.
     */
    @ProtoLayoutExperimental
    public boolean hasExcludeFontPadding() {
        return checkNotNull(mText.getAndroidTextStyle()).getExcludeFontPadding();
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
        // We don't need to check the tag as LayoutElement.Text will have the same fields as our
        // getters even if it's not Material.
        return new Text(textElement);
    }

    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return mText.toLayoutElementProto();
    }

    @Nullable
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public Fingerprint getFingerprint() {
        return mText.getFingerprint();
    }
}
