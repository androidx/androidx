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

package androidx.wear.tiles.builders;

import android.annotation.SuppressLint;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.builders.ColorBuilders.ColorProp;
import androidx.wear.tiles.builders.DimensionBuilders.ContainerDimension;
import androidx.wear.tiles.builders.DimensionBuilders.DegreesProp;
import androidx.wear.tiles.builders.DimensionBuilders.DpProp;
import androidx.wear.tiles.builders.DimensionBuilders.EmProp;
import androidx.wear.tiles.builders.DimensionBuilders.ImageDimension;
import androidx.wear.tiles.builders.DimensionBuilders.SpProp;
import androidx.wear.tiles.builders.DimensionBuilders.SpacerDimension;
import androidx.wear.tiles.builders.ModifiersBuilders.ArcModifiers;
import androidx.wear.tiles.builders.ModifiersBuilders.Modifiers;
import androidx.wear.tiles.builders.ModifiersBuilders.SpanModifiers;
import androidx.wear.tiles.proto.LayoutElementProto;
import androidx.wear.tiles.proto.TypesProto;
import androidx.wear.tiles.readers.DeviceParametersReaders.DeviceParameters;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Builders for composable layout elements that can be combined together to create renderable UI
 * layouts.
 */
public final class LayoutElementBuilders {
    private LayoutElementBuilders() {}

    /**
     * The horizontal alignment of an element within its container.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({HALIGN_UNDEFINED, HALIGN_LEFT, HALIGN_CENTER, HALIGN_RIGHT, HALIGN_START, HALIGN_END})
    @Retention(RetentionPolicy.SOURCE)
    public @interface HorizontalAlignment {}

    /** Horizontal alignment is undefined. */
    public static final int HALIGN_UNDEFINED = 0;

    /** Horizontally align to the left. */
    public static final int HALIGN_LEFT = 1;

    /** Horizontally align to center. */
    public static final int HALIGN_CENTER = 2;

    /** Horizontally align to the right. */
    public static final int HALIGN_RIGHT = 3;

    /** Horizontally align to the content start (left in LTR layouts, right in RTL layouts). */
    public static final int HALIGN_START = 4;

    /** Horizontally align to the content end (right in LTR layouts, left in RTL layouts). */
    public static final int HALIGN_END = 5;

    /**
     * The vertical alignment of an element within its container.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({VALIGN_UNDEFINED, VALIGN_TOP, VALIGN_CENTER, VALIGN_BOTTOM})
    @Retention(RetentionPolicy.SOURCE)
    public @interface VerticalAlignment {}

    /** Vertical alignment is undefined. */
    public static final int VALIGN_UNDEFINED = 0;

    /** Vertically align to the top. */
    public static final int VALIGN_TOP = 1;

    /** Vertically align to center. */
    public static final int VALIGN_CENTER = 2;

    /** Vertically align to the bottom. */
    public static final int VALIGN_BOTTOM = 3;

    /**
     * The weight to be applied to the font.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({FONT_WEIGHT_UNDEFINED, FONT_WEIGHT_NORMAL, FONT_WEIGHT_BOLD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FontWeight {}

    /** Font weight is undefined. */
    public static final int FONT_WEIGHT_UNDEFINED = 0;

    /** Normal font weight. */
    public static final int FONT_WEIGHT_NORMAL = 400;

    /** Bold font weight. */
    public static final int FONT_WEIGHT_BOLD = 700;

    /**
     * Alignment of a text element.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({TEXT_ALIGN_UNDEFINED, TEXT_ALIGN_START, TEXT_ALIGN_CENTER, TEXT_ALIGN_END})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TextAlignment {}

    /** Alignment is undefined. */
    public static final int TEXT_ALIGN_UNDEFINED = 0;

    /**
     * Align to the "start" of the {@link Text} element (left in LTR layouts, right in RTL layouts).
     */
    public static final int TEXT_ALIGN_START = 1;

    /** Align to the center of the {@link Text} element. */
    public static final int TEXT_ALIGN_CENTER = 2;

    /**
     * Align to the "end" of the {@link Text} element (right in LTR layouts, left in RTL layouts).
     */
    public static final int TEXT_ALIGN_END = 3;

    /**
     * How text that will not fit inside the bounds of a {@link Text} element will be handled.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({TEXT_OVERFLOW_UNDEFINED, TEXT_OVERFLOW_TRUNCATE, TEXT_OVERFLOW_ELLIPSIZE_END})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TextOverflow {}

    /** Overflow behavior is undefined. */
    public static final int TEXT_OVERFLOW_UNDEFINED = 0;

    /**
     * Truncate the text to fit inside of the {@link Text} element's bounds. If text is truncated,
     * it will be truncated on a word boundary.
     */
    public static final int TEXT_OVERFLOW_TRUNCATE = 1;

    /**
     * Truncate the text to fit in the {@link Text} element's bounds, but add an ellipsis (i.e. ...)
     * to the end of the text if it has been truncated.
     */
    public static final int TEXT_OVERFLOW_ELLIPSIZE_END = 2;

    /**
     * The anchor position of an {@link Arc}'s elements. This is used to specify how elements added
     * to an {@link Arc} should be laid out with respect to anchor_angle.
     *
     * <p>As an example, assume that the following diagrams are wrapped to an arc, and each
     * represents an {@link Arc} element containing a single {@link Text} element. The {@link Text}
     * element's anchor_angle is "0" for all cases.
     *
     * <pre>{@code
     * ARC_ANCHOR_START:
     * -180                                0                                    180
     *                                     Hello World!
     *
     *
     * ARC_ANCHOR_CENTER:
     * -180                                0                                    180
     *                                Hello World!
     *
     * ARC_ANCHOR_END:
     * -180                                0                                    180
     *                          Hello World!
     *
     * }</pre>
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({ARC_ANCHOR_UNDEFINED, ARC_ANCHOR_START, ARC_ANCHOR_CENTER, ARC_ANCHOR_END})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ArcAnchorType {}

    /** Anchor position is undefined. */
    public static final int ARC_ANCHOR_UNDEFINED = 0;

    /**
     * Anchor at the start of the elements. This will cause elements added to an arc to begin at the
     * given anchor_angle, and sweep around to the right.
     */
    public static final int ARC_ANCHOR_START = 1;

    /**
     * Anchor at the center of the elements. This will cause the center of the whole set of elements
     * added to an arc to be pinned at the given anchor_angle.
     */
    public static final int ARC_ANCHOR_CENTER = 2;

    /**
     * Anchor at the end of the elements. This will cause the set of elements inside the arc to end
     * at the specified anchor_angle, i.e. all elements should be to the left of anchor_angle.
     */
    public static final int ARC_ANCHOR_END = 3;

    /**
     * How content which does not match the dimensions of its bounds (e.g. an image resource being
     * drawn inside an {@link Image}) will be resized to fit its bounds.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
        CONTENT_SCALE_MODE_UNDEFINED,
        CONTENT_SCALE_MODE_FIT,
        CONTENT_SCALE_MODE_CROP,
        CONTENT_SCALE_MODE_FILL_BOUNDS
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ContentScaleMode {}

    /** Content scaling is undefined. */
    public static final int CONTENT_SCALE_MODE_UNDEFINED = 0;

    /**
     * Content will be scaled to fit inside its bounds, proportionally. As an example, If a 10x5
     * image was going to be drawn inside a 50x50 {@link Image} element, the actual image resource
     * would be drawn as a 50x25 image, centered within the 50x50 bounds.
     */
    public static final int CONTENT_SCALE_MODE_FIT = 1;

    /**
     * Content will be resized proportionally so it completely fills its bounds, and anything
     * outside of the bounds will be cropped. As an example, if a 10x5 image was going to be drawn
     * inside a 50x50 {@link Image} element, the image resource would be drawn as a 100x50 image,
     * centered within its bounds (and with 25px cropped from both the left and right sides).
     */
    public static final int CONTENT_SCALE_MODE_CROP = 2;

    /**
     * Content will be resized to fill its bounds, without taking into account the aspect ratio. If
     * a 10x5 image was going to be drawn inside a 50x50 {@link Image} element, the image would be
     * drawn as a 50x50 image, stretched vertically.
     */
    public static final int CONTENT_SCALE_MODE_FILL_BOUNDS = 3;

    /** The styling of a font (e.g. font size, and metrics). */
    public static final class FontStyle {
        private final LayoutElementProto.FontStyle mImpl;

        private FontStyle(LayoutElementProto.FontStyle impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static FontStyle fromProto(@NonNull LayoutElementProto.FontStyle proto) {
            return new FontStyle(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public LayoutElementProto.FontStyle toProto() {
            return mImpl;
        }

        /** Builder for {@link FontStyle} */
        public static final class Builder {
            private final LayoutElementProto.FontStyle.Builder mImpl =
                    LayoutElementProto.FontStyle.newBuilder();

            Builder() {}

            /**
             * Sets the size of the font, in scaled pixels (sp). If not specified, defaults to the
             * size of the system's "body" font.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setSize(@NonNull SpProp size) {
                mImpl.setSize(size.toProto());
                return this;
            }

            /**
             * Sets the size of the font, in scaled pixels (sp). If not specified, defaults to the
             * size of the system's "body" font.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setSize(@NonNull SpProp.Builder sizeBuilder) {
                mImpl.setSize(sizeBuilder.build().toProto());
                return this;
            }

            /**
             * Sets whether the text should be rendered in a italic typeface. If not specified,
             * defaults to "false".
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setItalic(boolean italic) {
                mImpl.setItalic(TypesProto.BoolProp.newBuilder().setValue(italic));
                return this;
            }

            /**
             * Sets whether the text should be rendered with an underline. If not specified,
             * defaults to "false".
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setUnderline(boolean underline) {
                mImpl.setUnderline(TypesProto.BoolProp.newBuilder().setValue(underline));
                return this;
            }

            /** Sets the text color. If not defined, defaults to white. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setColor(@NonNull ColorProp color) {
                mImpl.setColor(color.toProto());
                return this;
            }

            /** Sets the text color. If not defined, defaults to white. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setColor(@NonNull ColorProp.Builder colorBuilder) {
                mImpl.setColor(colorBuilder.build().toProto());
                return this;
            }

            /**
             * Sets the weight of the font. If the provided value is not supported on a platform,
             * the nearest supported value will be used. If not defined, or when set to an invalid
             * value, defaults to "normal".
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setWeight(@FontWeight int weight) {
                mImpl.setWeight(
                        LayoutElementProto.FontWeightProp.newBuilder()
                                .setValue(LayoutElementProto.FontWeight.forNumber(weight)));
                return this;
            }

            /**
             * Sets the text letter-spacing. Positive numbers increase the space between letters
             * while negative numbers tighten the space. If not specified, defaults to 0.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setLetterSpacing(@NonNull EmProp letterSpacing) {
                mImpl.setLetterSpacing(letterSpacing.toProto());
                return this;
            }

            /**
             * Sets the text letter-spacing. Positive numbers increase the space between letters
             * while negative numbers tighten the space. If not specified, defaults to 0.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setLetterSpacing(@NonNull EmProp.Builder letterSpacingBuilder) {
                mImpl.setLetterSpacing(letterSpacingBuilder.build().toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public FontStyle build() {
                return FontStyle.fromProto(mImpl.build());
            }
        }
    }

    /** A text string. */
    public static final class Text implements LayoutElement {
        private final LayoutElementProto.Text mImpl;

        private Text(LayoutElementProto.Text impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Text fromProto(@NonNull LayoutElementProto.Text proto) {
            return new Text(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        LayoutElementProto.Text toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public LayoutElementProto.LayoutElement toLayoutElementProto() {
            return LayoutElementProto.LayoutElement.newBuilder().setText(mImpl).build();
        }

        /** Builder for {@link Text}. */
        public static final class Builder implements LayoutElement.Builder {
            private final LayoutElementProto.Text.Builder mImpl =
                    LayoutElementProto.Text.newBuilder();

            Builder() {}

            /** Sets the text to render. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setText(@NonNull String text) {
                mImpl.setText(TypesProto.StringProp.newBuilder().setValue(text));
                return this;
            }

            /**
             * Sets the style of font to use (size, bold etc). If not specified, defaults to the
             * platform's default body font.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setFontStyle(@NonNull FontStyle fontStyle) {
                mImpl.setFontStyle(fontStyle.toProto());
                return this;
            }

            /**
             * Sets the style of font to use (size, bold etc). If not specified, defaults to the
             * platform's default body font.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setFontStyle(@NonNull FontStyle.Builder fontStyleBuilder) {
                mImpl.setFontStyle(fontStyleBuilder.build().toProto());
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull Modifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull Modifiers.Builder modifiersBuilder) {
                mImpl.setModifiers(modifiersBuilder.build().toProto());
                return this;
            }

            /**
             * Sets the maximum number of lines that can be represented by the {@link Text} element.
             * If not defined, the {@link Text} element will be treated as a single-line element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setMaxLines(@IntRange(from = 1) int maxLines) {
                mImpl.setMaxLines(TypesProto.Int32Prop.newBuilder().setValue(maxLines));
                return this;
            }

            /**
             * Sets alignment of the text within its bounds. Note that a {@link Text} element will
             * size itself to wrap its contents, so this option is meaningless for single-line text
             * (for that, use alignment of the outer container). For multi-line text, however, this
             * will set the alignment of lines relative to the {@link Text} element bounds. If not
             * defined, defaults to TEXT_ALIGN_CENTER.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setMultilineAlignment(@TextAlignment int multilineAlignment) {
                mImpl.setMultilineAlignment(
                        LayoutElementProto.TextAlignmentProp.newBuilder()
                                .setValue(
                                        LayoutElementProto.TextAlignment.forNumber(
                                                multilineAlignment)));
                return this;
            }

            /**
             * Sets how to handle text which overflows the bound of the {@link Text} element. A
             * {@link Text} element will grow as large as possible inside its parent container
             * (while still respecting max_lines); if it cannot grow large enough to render all of
             * its text, the text which cannot fit inside its container will be truncated. If not
             * defined, defaults to TEXT_OVERFLOW_TRUNCATE.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setOverflow(@TextOverflow int overflow) {
                mImpl.setOverflow(
                        LayoutElementProto.TextOverflowProp.newBuilder()
                                .setValue(LayoutElementProto.TextOverflow.forNumber(overflow)));
                return this;
            }

            /**
             * Sets the explicit height between lines of text. This is equivalent to the vertical
             * distance between subsequent baselines. If not specified, defaults the font's
             * recommended interline spacing.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setLineHeight(@NonNull SpProp lineHeight) {
                mImpl.setLineHeight(lineHeight.toProto());
                return this;
            }

            /**
             * Sets the explicit height between lines of text. This is equivalent to the vertical
             * distance between subsequent baselines. If not specified, defaults the font's
             * recommended interline spacing.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setLineHeight(@NonNull SpProp.Builder lineHeightBuilder) {
                mImpl.setLineHeight(lineHeightBuilder.build().toProto());
                return this;
            }

            @Override
            @NonNull
            public Text build() {
                return Text.fromProto(mImpl.build());
            }
        }
    }

    /**
     * An image.
     *
     * <p>Images used in this element must exist in the resource bundle that corresponds to this
     * layout. Images must have their dimension specified, and will be rendered at this width and
     * height, regardless of their native dimension.
     */
    public static final class Image implements LayoutElement {
        private final LayoutElementProto.Image mImpl;

        private Image(LayoutElementProto.Image impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Image fromProto(@NonNull LayoutElementProto.Image proto) {
            return new Image(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        LayoutElementProto.Image toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public LayoutElementProto.LayoutElement toLayoutElementProto() {
            return LayoutElementProto.LayoutElement.newBuilder().setImage(mImpl).build();
        }

        /** Builder for {@link Image}. */
        public static final class Builder implements LayoutElement.Builder {
            private final LayoutElementProto.Image.Builder mImpl =
                    LayoutElementProto.Image.newBuilder();

            Builder() {}

            /**
             * Sets the resource_id of the image to render. This must exist in the supplied resource
             * bundle.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setResourceId(@NonNull String resourceId) {
                mImpl.setResourceId(TypesProto.StringProp.newBuilder().setValue(resourceId));
                return this;
            }

            /** Sets the width of this image. If not defined, the image will not be rendered. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setWidth(@NonNull ImageDimension width) {
                mImpl.setWidth(width.toImageDimensionProto());
                return this;
            }

            /** Sets the width of this image. If not defined, the image will not be rendered. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setWidth(@NonNull ImageDimension.Builder widthBuilder) {
                mImpl.setWidth(widthBuilder.build().toImageDimensionProto());
                return this;
            }

            /** Sets the height of this image. If not defined, the image will not be rendered. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setHeight(@NonNull ImageDimension height) {
                mImpl.setHeight(height.toImageDimensionProto());
                return this;
            }

            /** Sets the height of this image. If not defined, the image will not be rendered. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setHeight(@NonNull ImageDimension.Builder heightBuilder) {
                mImpl.setHeight(heightBuilder.build().toImageDimensionProto());
                return this;
            }

            /**
             * Sets how to scale the image resource inside the bounds specified by width/height if
             * its size does not match those bounds. Defaults to CONTENT_SCALE_MODE_FIT.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setContentScaleMode(@ContentScaleMode int contentScaleMode) {
                mImpl.setContentScaleMode(
                        LayoutElementProto.ContentScaleModeProp.newBuilder()
                                .setValue(
                                        LayoutElementProto.ContentScaleMode.forNumber(
                                                contentScaleMode)));
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull Modifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull Modifiers.Builder modifiersBuilder) {
                mImpl.setModifiers(modifiersBuilder.build().toProto());
                return this;
            }

            @Override
            @NonNull
            public Image build() {
                return Image.fromProto(mImpl.build());
            }
        }
    }

    /** A simple spacer, typically used to provide padding between adjacent elements. */
    public static final class Spacer implements LayoutElement {
        private final LayoutElementProto.Spacer mImpl;

        private Spacer(LayoutElementProto.Spacer impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Spacer fromProto(@NonNull LayoutElementProto.Spacer proto) {
            return new Spacer(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        LayoutElementProto.Spacer toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public LayoutElementProto.LayoutElement toLayoutElementProto() {
            return LayoutElementProto.LayoutElement.newBuilder().setSpacer(mImpl).build();
        }

        /** Builder for {@link Spacer}. */
        public static final class Builder implements LayoutElement.Builder {
            private final LayoutElementProto.Spacer.Builder mImpl =
                    LayoutElementProto.Spacer.newBuilder();

            Builder() {}

            /**
             * Sets the width of this {@link Spacer}. When this is added as the direct child of an
             * {@link Arc}, this must be specified as an angular dimension, otherwise a linear
             * dimension must be used. If not defined, defaults to 0.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setWidth(@NonNull SpacerDimension width) {
                mImpl.setWidth(width.toSpacerDimensionProto());
                return this;
            }

            /**
             * Sets the width of this {@link Spacer}. When this is added as the direct child of an
             * {@link Arc}, this must be specified as an angular dimension, otherwise a linear
             * dimension must be used. If not defined, defaults to 0.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setWidth(@NonNull SpacerDimension.Builder widthBuilder) {
                mImpl.setWidth(widthBuilder.build().toSpacerDimensionProto());
                return this;
            }

            /** Sets the height of this spacer. If not defined, defaults to 0. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setHeight(@NonNull SpacerDimension height) {
                mImpl.setHeight(height.toSpacerDimensionProto());
                return this;
            }

            /** Sets the height of this spacer. If not defined, defaults to 0. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setHeight(@NonNull SpacerDimension.Builder heightBuilder) {
                mImpl.setHeight(heightBuilder.build().toSpacerDimensionProto());
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull Modifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull Modifiers.Builder modifiersBuilder) {
                mImpl.setModifiers(modifiersBuilder.build().toProto());
                return this;
            }

            @Override
            @NonNull
            public Spacer build() {
                return Spacer.fromProto(mImpl.build());
            }
        }
    }

    /**
     * A container which stacks all of its children on top of one another. This also allows to add a
     * background color, or to have a border around them with some padding.
     */
    public static final class Box implements LayoutElement {
        private final LayoutElementProto.Box mImpl;

        private Box(LayoutElementProto.Box impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Box fromProto(@NonNull LayoutElementProto.Box proto) {
            return new Box(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        LayoutElementProto.Box toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public LayoutElementProto.LayoutElement toLayoutElementProto() {
            return LayoutElementProto.LayoutElement.newBuilder().setBox(mImpl).build();
        }

        /** Builder for {@link Box}. */
        public static final class Builder implements LayoutElement.Builder {
            private final LayoutElementProto.Box.Builder mImpl =
                    LayoutElementProto.Box.newBuilder();

            Builder() {}

            /** Adds one item to the child element(s) to wrap. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder addContent(@NonNull LayoutElement content) {
                mImpl.addContents(content.toLayoutElementProto());
                return this;
            }

            /** Adds one item to the child element(s) to wrap. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder addContent(@NonNull LayoutElement.Builder contentBuilder) {
                mImpl.addContents(contentBuilder.build().toLayoutElementProto());
                return this;
            }

            /**
             * Sets the height of this {@link Box}. If not defined, this will size itself to fit all
             * of its children (i.e. a WrappedDimension).
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setHeight(@NonNull ContainerDimension height) {
                mImpl.setHeight(height.toContainerDimensionProto());
                return this;
            }

            /**
             * Sets the height of this {@link Box}. If not defined, this will size itself to fit all
             * of its children (i.e. a WrappedDimension).
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setHeight(@NonNull ContainerDimension.Builder heightBuilder) {
                mImpl.setHeight(heightBuilder.build().toContainerDimensionProto());
                return this;
            }

            /**
             * Sets the width of this {@link Box}. If not defined, this will size itself to fit all
             * of its children (i.e. a WrappedDimension).
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setWidth(@NonNull ContainerDimension width) {
                mImpl.setWidth(width.toContainerDimensionProto());
                return this;
            }

            /**
             * Sets the width of this {@link Box}. If not defined, this will size itself to fit all
             * of its children (i.e. a WrappedDimension).
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setWidth(@NonNull ContainerDimension.Builder widthBuilder) {
                mImpl.setWidth(widthBuilder.build().toContainerDimensionProto());
                return this;
            }

            /**
             * Sets the horizontal alignment of the element inside this {@link Box}. If not defined,
             * defaults to HALIGN_CENTER.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setHorizontalAlignment(@HorizontalAlignment int horizontalAlignment) {
                mImpl.setHorizontalAlignment(
                        LayoutElementProto.HorizontalAlignmentProp.newBuilder()
                                .setValue(
                                        LayoutElementProto.HorizontalAlignment.forNumber(
                                                horizontalAlignment)));
                return this;
            }

            /**
             * Sets the vertical alignment of the element inside this {@link Box}. If not defined,
             * defaults to VALIGN_CENTER.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setVerticalAlignment(@VerticalAlignment int verticalAlignment) {
                mImpl.setVerticalAlignment(
                        LayoutElementProto.VerticalAlignmentProp.newBuilder()
                                .setValue(
                                        LayoutElementProto.VerticalAlignment.forNumber(
                                                verticalAlignment)));
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull Modifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull Modifiers.Builder modifiersBuilder) {
                mImpl.setModifiers(modifiersBuilder.build().toProto());
                return this;
            }

            @Override
            @NonNull
            public Box build() {
                return Box.fromProto(mImpl.build());
            }
        }
    }

    /**
     * A portion of text which can be added to a {@link Span}. Two different {@link SpanText}
     * elements on the same line will be aligned to the same baseline, regardless of the size of
     * each {@link SpanText}.
     */
    public static final class SpanText implements Span {
        private final LayoutElementProto.SpanText mImpl;

        private SpanText(LayoutElementProto.SpanText impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static SpanText fromProto(@NonNull LayoutElementProto.SpanText proto) {
            return new SpanText(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        LayoutElementProto.SpanText toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public LayoutElementProto.Span toSpanProto() {
            return LayoutElementProto.Span.newBuilder().setText(mImpl).build();
        }

        /** Builder for {@link SpanText}. */
        public static final class Builder implements Span.Builder {
            private final LayoutElementProto.SpanText.Builder mImpl =
                    LayoutElementProto.SpanText.newBuilder();

            Builder() {}

            /** Sets the text to render. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setText(@NonNull String text) {
                mImpl.setText(TypesProto.StringProp.newBuilder().setValue(text));
                return this;
            }

            /**
             * Sets the style of font to use (size, bold etc). If not specified, defaults to the
             * platform's default body font.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setFontStyle(@NonNull FontStyle fontStyle) {
                mImpl.setFontStyle(fontStyle.toProto());
                return this;
            }

            /**
             * Sets the style of font to use (size, bold etc). If not specified, defaults to the
             * platform's default body font.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setFontStyle(@NonNull FontStyle.Builder fontStyleBuilder) {
                mImpl.setFontStyle(fontStyleBuilder.build().toProto());
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull SpanModifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull SpanModifiers.Builder modifiersBuilder) {
                mImpl.setModifiers(modifiersBuilder.build().toProto());
                return this;
            }

            @Override
            @NonNull
            public SpanText build() {
                return SpanText.fromProto(mImpl.build());
            }
        }
    }

    /** An image which can be added to a {@link Span}. */
    public static final class SpanImage implements Span {
        private final LayoutElementProto.SpanImage mImpl;

        private SpanImage(LayoutElementProto.SpanImage impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static SpanImage fromProto(@NonNull LayoutElementProto.SpanImage proto) {
            return new SpanImage(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        LayoutElementProto.SpanImage toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public LayoutElementProto.Span toSpanProto() {
            return LayoutElementProto.Span.newBuilder().setImage(mImpl).build();
        }

        /** Builder for {@link SpanImage}. */
        public static final class Builder implements Span.Builder {
            private final LayoutElementProto.SpanImage.Builder mImpl =
                    LayoutElementProto.SpanImage.newBuilder();

            Builder() {}

            /**
             * Sets the resource_id of the image to render. This must exist in the supplied resource
             * bundle.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setResourceId(@NonNull String resourceId) {
                mImpl.setResourceId(TypesProto.StringProp.newBuilder().setValue(resourceId));
                return this;
            }

            /** Sets the width of this image. If not defined, the image will not be rendered. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setWidth(@NonNull DpProp width) {
                mImpl.setWidth(width.toProto());
                return this;
            }

            /** Sets the width of this image. If not defined, the image will not be rendered. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setWidth(@NonNull DpProp.Builder widthBuilder) {
                mImpl.setWidth(widthBuilder.build().toProto());
                return this;
            }

            /** Sets the height of this image. If not defined, the image will not be rendered. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setHeight(@NonNull DpProp height) {
                mImpl.setHeight(height.toProto());
                return this;
            }

            /** Sets the height of this image. If not defined, the image will not be rendered. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setHeight(@NonNull DpProp.Builder heightBuilder) {
                mImpl.setHeight(heightBuilder.build().toProto());
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull SpanModifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull SpanModifiers.Builder modifiersBuilder) {
                mImpl.setModifiers(modifiersBuilder.build().toProto());
                return this;
            }

            @Override
            @NonNull
            public SpanImage build() {
                return SpanImage.fromProto(mImpl.build());
            }
        }
    }

    /**
     * Interface defining a single {@link Span}. Each {@link Span} forms part of a larger {@link
     * Spannable} widget. At the moment, the only widgets which can be added to {@link Spannable}
     * containers are {@link SpanText} and {@link SpanImage} elements.
     */
    public interface Span {
        /**
         * Get the protocol buffer representation of this object.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        LayoutElementProto.Span toSpanProto();

        /** Builder to create {@link Span} objects. */
        @SuppressLint("StaticFinalBuilder")
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            Span build();
        }
    }

    /**
     * A container of {@link Span} elements. Currently, this only supports {@link Text} elements,
     * where each individual {@link Span} can have different styling applied to it but the resulting
     * text will flow naturally. This allows sections of a paragraph of text to have different
     * styling applied to it, for example, making one or two words bold or italic.
     */
    public static final class Spannable implements LayoutElement {
        private final LayoutElementProto.Spannable mImpl;

        private Spannable(LayoutElementProto.Spannable impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Spannable fromProto(@NonNull LayoutElementProto.Spannable proto) {
            return new Spannable(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        LayoutElementProto.Spannable toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public LayoutElementProto.LayoutElement toLayoutElementProto() {
            return LayoutElementProto.LayoutElement.newBuilder().setSpannable(mImpl).build();
        }

        /** Builder for {@link Spannable}. */
        public static final class Builder implements LayoutElement.Builder {
            private final LayoutElementProto.Spannable.Builder mImpl =
                    LayoutElementProto.Spannable.newBuilder();

            Builder() {}

            /** Adds one item to the {@link Span} elements that form this {@link Spannable}. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder addSpan(@NonNull Span span) {
                mImpl.addSpans(span.toSpanProto());
                return this;
            }

            /** Adds one item to the {@link Span} elements that form this {@link Spannable}. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder addSpan(@NonNull Span.Builder spanBuilder) {
                mImpl.addSpans(spanBuilder.build().toSpanProto());
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull Modifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull Modifiers.Builder modifiersBuilder) {
                mImpl.setModifiers(modifiersBuilder.build().toProto());
                return this;
            }

            /**
             * Sets the maximum number of lines that can be represented by the {@link Spannable}
             * element. If not defined, the {@link Spannable} element will be treated as a
             * single-line element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setMaxLines(@IntRange(from = 1) int maxLines) {
                mImpl.setMaxLines(TypesProto.Int32Prop.newBuilder().setValue(maxLines));
                return this;
            }

            /**
             * Sets alignment of the {@link Spannable} content within its bounds. Note that a {@link
             * Spannable} element will size itself to wrap its contents, so this option is
             * meaningless for single-line content (for that, use alignment of the outer container).
             * For multi-line content, however, this will set the alignment of lines relative to the
             * {@link Spannable} element bounds. If not defined, defaults to TEXT_ALIGN_CENTER.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setMultilineAlignment(@HorizontalAlignment int multilineAlignment) {
                mImpl.setMultilineAlignment(
                        LayoutElementProto.HorizontalAlignmentProp.newBuilder()
                                .setValue(
                                        LayoutElementProto.HorizontalAlignment.forNumber(
                                                multilineAlignment)));
                return this;
            }

            /**
             * Sets how to handle content which overflows the bound of the {@link Spannable}
             * element. A {@link Spannable} element will grow as large as possible inside its parent
             * container (while still respecting max_lines); if it cannot grow large enough to
             * render all of its content, the content which cannot fit inside its container will be
             * truncated. If not defined, defaults to TEXT_OVERFLOW_TRUNCATE.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setOverflow(@TextOverflow int overflow) {
                mImpl.setOverflow(
                        LayoutElementProto.TextOverflowProp.newBuilder()
                                .setValue(LayoutElementProto.TextOverflow.forNumber(overflow)));
                return this;
            }

            /**
             * Sets extra spacing to add between each line. This will apply to all spans regardless
             * of their font size. This is in addition to original line heights. Note that this
             * won't add any additional space before the first line or after the last line. The
             * default value is zero and negative values will decrease the interline spacing.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setLineSpacing(@NonNull SpProp lineSpacing) {
                mImpl.setLineSpacing(lineSpacing.toProto());
                return this;
            }

            /**
             * Sets extra spacing to add between each line. This will apply to all spans regardless
             * of their font size. This is in addition to original line heights. Note that this
             * won't add any additional space before the first line or after the last line. The
             * default value is zero and negative values will decrease the interline spacing.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setLineSpacing(@NonNull SpProp.Builder lineSpacingBuilder) {
                mImpl.setLineSpacing(lineSpacingBuilder.build().toProto());
                return this;
            }

            @Override
            @NonNull
            public Spannable build() {
                return Spannable.fromProto(mImpl.build());
            }
        }
    }

    /**
     * A column of elements. Each child element will be laid out vertically, one after another (i.e.
     * stacking down). This element will size itself to the smallest size required to hold all of
     * its children (e.g. if it contains three elements sized 10x10, 20x20 and 30x30, the resulting
     * column will be 30x60).
     *
     * <p>If specified, horizontal_alignment can be used to control the gravity inside the
     * container, affecting the horizontal placement of children whose width are smaller than the
     * resulting column width.
     */
    public static final class Column implements LayoutElement {
        private final LayoutElementProto.Column mImpl;

        private Column(LayoutElementProto.Column impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Column fromProto(@NonNull LayoutElementProto.Column proto) {
            return new Column(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        LayoutElementProto.Column toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public LayoutElementProto.LayoutElement toLayoutElementProto() {
            return LayoutElementProto.LayoutElement.newBuilder().setColumn(mImpl).build();
        }

        /** Builder for {@link Column}. */
        public static final class Builder implements LayoutElement.Builder {
            private final LayoutElementProto.Column.Builder mImpl =
                    LayoutElementProto.Column.newBuilder();

            Builder() {}

            /** Adds one item to the list of child elements to place inside this {@link Column}. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder addContent(@NonNull LayoutElement content) {
                mImpl.addContents(content.toLayoutElementProto());
                return this;
            }

            /** Adds one item to the list of child elements to place inside this {@link Column}. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder addContent(@NonNull LayoutElement.Builder contentBuilder) {
                mImpl.addContents(contentBuilder.build().toLayoutElementProto());
                return this;
            }

            /**
             * Sets the horizontal alignment of elements inside this column, if they are narrower
             * than the resulting width of the column. If not defined, defaults to HALIGN_CENTER.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setHorizontalAlignment(@HorizontalAlignment int horizontalAlignment) {
                mImpl.setHorizontalAlignment(
                        LayoutElementProto.HorizontalAlignmentProp.newBuilder()
                                .setValue(
                                        LayoutElementProto.HorizontalAlignment.forNumber(
                                                horizontalAlignment)));
                return this;
            }

            /**
             * Sets the width of this column. If not defined, this will size itself to fit all of
             * its children (i.e. a WrappedDimension).
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setWidth(@NonNull ContainerDimension width) {
                mImpl.setWidth(width.toContainerDimensionProto());
                return this;
            }

            /**
             * Sets the width of this column. If not defined, this will size itself to fit all of
             * its children (i.e. a WrappedDimension).
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setWidth(@NonNull ContainerDimension.Builder widthBuilder) {
                mImpl.setWidth(widthBuilder.build().toContainerDimensionProto());
                return this;
            }

            /**
             * Sets the height of this column. If not defined, this will size itself to fit all of
             * its children (i.e. a WrappedDimension).
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setHeight(@NonNull ContainerDimension height) {
                mImpl.setHeight(height.toContainerDimensionProto());
                return this;
            }

            /**
             * Sets the height of this column. If not defined, this will size itself to fit all of
             * its children (i.e. a WrappedDimension).
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setHeight(@NonNull ContainerDimension.Builder heightBuilder) {
                mImpl.setHeight(heightBuilder.build().toContainerDimensionProto());
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull Modifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull Modifiers.Builder modifiersBuilder) {
                mImpl.setModifiers(modifiersBuilder.build().toProto());
                return this;
            }

            @Override
            @NonNull
            public Column build() {
                return Column.fromProto(mImpl.build());
            }
        }
    }

    /**
     * A row of elements. Each child will be laid out horizontally, one after another (i.e. stacking
     * to the right). This element will size itself to the smallest size required to hold all of its
     * children (e.g. if it contains three elements sized 10x10, 20x20 and 30x30, the resulting row
     * will be 60x30).
     *
     * <p>If specified, vertical_alignment can be used to control the gravity inside the container,
     * affecting the vertical placement of children whose width are smaller than the resulting row
     * height.
     */
    public static final class Row implements LayoutElement {
        private final LayoutElementProto.Row mImpl;

        private Row(LayoutElementProto.Row impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Row fromProto(@NonNull LayoutElementProto.Row proto) {
            return new Row(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        LayoutElementProto.Row toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public LayoutElementProto.LayoutElement toLayoutElementProto() {
            return LayoutElementProto.LayoutElement.newBuilder().setRow(mImpl).build();
        }

        /** Builder for {@link Row}. */
        public static final class Builder implements LayoutElement.Builder {
            private final LayoutElementProto.Row.Builder mImpl =
                    LayoutElementProto.Row.newBuilder();

            Builder() {}

            /** Adds one item to the list of child elements to place inside this {@link Row}. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder addContent(@NonNull LayoutElement content) {
                mImpl.addContents(content.toLayoutElementProto());
                return this;
            }

            /** Adds one item to the list of child elements to place inside this {@link Row}. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder addContent(@NonNull LayoutElement.Builder contentBuilder) {
                mImpl.addContents(contentBuilder.build().toLayoutElementProto());
                return this;
            }

            /**
             * Sets the vertical alignment of elements inside this row, if they are narrower than
             * the resulting height of the row. If not defined, defaults to VALIGN_CENTER.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setVerticalAlignment(@VerticalAlignment int verticalAlignment) {
                mImpl.setVerticalAlignment(
                        LayoutElementProto.VerticalAlignmentProp.newBuilder()
                                .setValue(
                                        LayoutElementProto.VerticalAlignment.forNumber(
                                                verticalAlignment)));
                return this;
            }

            /**
             * Sets the width of this row. If not defined, this will size itself to fit all of its
             * children (i.e. a WrappedDimension).
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setWidth(@NonNull ContainerDimension width) {
                mImpl.setWidth(width.toContainerDimensionProto());
                return this;
            }

            /**
             * Sets the width of this row. If not defined, this will size itself to fit all of its
             * children (i.e. a WrappedDimension).
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setWidth(@NonNull ContainerDimension.Builder widthBuilder) {
                mImpl.setWidth(widthBuilder.build().toContainerDimensionProto());
                return this;
            }

            /**
             * Sets the height of this row. If not defined, this will size itself to fit all of its
             * children (i.e. a WrappedDimension).
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setHeight(@NonNull ContainerDimension height) {
                mImpl.setHeight(height.toContainerDimensionProto());
                return this;
            }

            /**
             * Sets the height of this row. If not defined, this will size itself to fit all of its
             * children (i.e. a WrappedDimension).
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setHeight(@NonNull ContainerDimension.Builder heightBuilder) {
                mImpl.setHeight(heightBuilder.build().toContainerDimensionProto());
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull Modifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull Modifiers.Builder modifiersBuilder) {
                mImpl.setModifiers(modifiersBuilder.build().toProto());
                return this;
            }

            @Override
            @NonNull
            public Row build() {
                return Row.fromProto(mImpl.build());
            }
        }
    }

    /**
     * An arc container. This container will fill itself to a circle, which fits inside its parent
     * container, and all of its children will be placed on that circle. The fields anchor_angle and
     * anchor_type can be used to specify where to draw children within this circle.
     */
    public static final class Arc implements LayoutElement {
        private final LayoutElementProto.Arc mImpl;

        private Arc(LayoutElementProto.Arc impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Arc fromProto(@NonNull LayoutElementProto.Arc proto) {
            return new Arc(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        LayoutElementProto.Arc toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public LayoutElementProto.LayoutElement toLayoutElementProto() {
            return LayoutElementProto.LayoutElement.newBuilder().setArc(mImpl).build();
        }

        /** Builder for {@link Arc}. */
        public static final class Builder implements LayoutElement.Builder {
            private final LayoutElementProto.Arc.Builder mImpl =
                    LayoutElementProto.Arc.newBuilder();

            Builder() {}

            /** Adds one item to contents of this container. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder addContent(@NonNull ArcLayoutElement content) {
                mImpl.addContents(content.toArcLayoutElementProto());
                return this;
            }

            /** Adds one item to contents of this container. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder addContent(@NonNull ArcLayoutElement.Builder contentBuilder) {
                mImpl.addContents(contentBuilder.build().toArcLayoutElementProto());
                return this;
            }

            /**
             * Sets the angle for the anchor, used with anchor_type to determine where to draw
             * children. Note that 0 degrees is the 12 o clock position on a device, and the angle
             * sweeps clockwise. If not defined, defaults to 0 degrees.
             *
             * <p>Values do not have to be clamped to the range 0-360; values less than 0 degrees
             * will sweep anti-clockwise (i.e. -90 degrees is equivalent to 270 degrees), and values
             * >360 will be be placed at X mod 360 degrees.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setAnchorAngle(@NonNull DegreesProp anchorAngle) {
                mImpl.setAnchorAngle(anchorAngle.toProto());
                return this;
            }

            /**
             * Sets the angle for the anchor, used with anchor_type to determine where to draw
             * children. Note that 0 degrees is the 12 o clock position on a device, and the angle
             * sweeps clockwise. If not defined, defaults to 0 degrees.
             *
             * <p>Values do not have to be clamped to the range 0-360; values less than 0 degrees
             * will sweep anti-clockwise (i.e. -90 degrees is equivalent to 270 degrees), and values
             * >360 will be be placed at X mod 360 degrees.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setAnchorAngle(@NonNull DegreesProp.Builder anchorAngleBuilder) {
                mImpl.setAnchorAngle(anchorAngleBuilder.build().toProto());
                return this;
            }

            /**
             * Sets how to align the contents of this container relative to anchor_angle. If not
             * defined, defaults to ARC_ANCHOR_CENTER.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setAnchorType(@ArcAnchorType int anchorType) {
                mImpl.setAnchorType(
                        LayoutElementProto.ArcAnchorTypeProp.newBuilder()
                                .setValue(LayoutElementProto.ArcAnchorType.forNumber(anchorType)));
                return this;
            }

            /**
             * Sets vertical alignment of elements within the arc. If the {@link Arc}'s thickness is
             * larger than the thickness of the element being drawn, this controls whether the
             * element should be drawn towards the inner or outer edge of the arc, or drawn in the
             * center. If not defined, defaults to VALIGN_CENTER.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setVerticalAlign(@VerticalAlignment int verticalAlign) {
                mImpl.setVerticalAlign(
                        LayoutElementProto.VerticalAlignmentProp.newBuilder()
                                .setValue(
                                        LayoutElementProto.VerticalAlignment.forNumber(
                                                verticalAlign)));
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull Modifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull Modifiers.Builder modifiersBuilder) {
                mImpl.setModifiers(modifiersBuilder.build().toProto());
                return this;
            }

            @Override
            @NonNull
            public Arc build() {
                return Arc.fromProto(mImpl.build());
            }
        }
    }

    /** A text element that can be used in an {@link Arc}. */
    public static final class ArcText implements ArcLayoutElement {
        private final LayoutElementProto.ArcText mImpl;

        private ArcText(LayoutElementProto.ArcText impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ArcText fromProto(@NonNull LayoutElementProto.ArcText proto) {
            return new ArcText(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        LayoutElementProto.ArcText toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public LayoutElementProto.ArcLayoutElement toArcLayoutElementProto() {
            return LayoutElementProto.ArcLayoutElement.newBuilder().setText(mImpl).build();
        }

        /** Builder for {@link ArcText}. */
        public static final class Builder implements ArcLayoutElement.Builder {
            private final LayoutElementProto.ArcText.Builder mImpl =
                    LayoutElementProto.ArcText.newBuilder();

            Builder() {}

            /** Sets the text to render. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setText(@NonNull String text) {
                mImpl.setText(TypesProto.StringProp.newBuilder().setValue(text));
                return this;
            }

            /**
             * Sets the style of font to use (size, bold etc). If not specified, defaults to the
             * platform's default body font.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setFontStyle(@NonNull FontStyle fontStyle) {
                mImpl.setFontStyle(fontStyle.toProto());
                return this;
            }

            /**
             * Sets the style of font to use (size, bold etc). If not specified, defaults to the
             * platform's default body font.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setFontStyle(@NonNull FontStyle.Builder fontStyleBuilder) {
                mImpl.setFontStyle(fontStyleBuilder.build().toProto());
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull ArcModifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull ArcModifiers.Builder modifiersBuilder) {
                mImpl.setModifiers(modifiersBuilder.build().toProto());
                return this;
            }

            @Override
            @NonNull
            public ArcText build() {
                return ArcText.fromProto(mImpl.build());
            }
        }
    }

    /** A line that can be used in an {@link Arc} and renders as a round progress bar. */
    public static final class ArcLine implements ArcLayoutElement {
        private final LayoutElementProto.ArcLine mImpl;

        private ArcLine(LayoutElementProto.ArcLine impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ArcLine fromProto(@NonNull LayoutElementProto.ArcLine proto) {
            return new ArcLine(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        LayoutElementProto.ArcLine toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public LayoutElementProto.ArcLayoutElement toArcLayoutElementProto() {
            return LayoutElementProto.ArcLayoutElement.newBuilder().setLine(mImpl).build();
        }

        /** Builder for {@link ArcLine}. */
        public static final class Builder implements ArcLayoutElement.Builder {
            private final LayoutElementProto.ArcLine.Builder mImpl =
                    LayoutElementProto.ArcLine.newBuilder();

            Builder() {}

            /** Sets the length of this line, in degrees. If not defined, defaults to 0. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setLength(@NonNull DegreesProp length) {
                mImpl.setLength(length.toProto());
                return this;
            }

            /** Sets the length of this line, in degrees. If not defined, defaults to 0. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setLength(@NonNull DegreesProp.Builder lengthBuilder) {
                mImpl.setLength(lengthBuilder.build().toProto());
                return this;
            }

            /** Sets the thickness of this line. If not defined, defaults to 0. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setThickness(@NonNull DpProp thickness) {
                mImpl.setThickness(thickness.toProto());
                return this;
            }

            /** Sets the thickness of this line. If not defined, defaults to 0. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setThickness(@NonNull DpProp.Builder thicknessBuilder) {
                mImpl.setThickness(thicknessBuilder.build().toProto());
                return this;
            }

            /** Sets the color of this line. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setColor(@NonNull ColorProp color) {
                mImpl.setColor(color.toProto());
                return this;
            }

            /** Sets the color of this line. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setColor(@NonNull ColorProp.Builder colorBuilder) {
                mImpl.setColor(colorBuilder.build().toProto());
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull ArcModifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull ArcModifiers.Builder modifiersBuilder) {
                mImpl.setModifiers(modifiersBuilder.build().toProto());
                return this;
            }

            @Override
            @NonNull
            public ArcLine build() {
                return ArcLine.fromProto(mImpl.build());
            }
        }
    }

    /** A simple spacer used to provide padding between adjacent elements in an {@link Arc}. */
    public static final class ArcSpacer implements ArcLayoutElement {
        private final LayoutElementProto.ArcSpacer mImpl;

        private ArcSpacer(LayoutElementProto.ArcSpacer impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ArcSpacer fromProto(@NonNull LayoutElementProto.ArcSpacer proto) {
            return new ArcSpacer(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        LayoutElementProto.ArcSpacer toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public LayoutElementProto.ArcLayoutElement toArcLayoutElementProto() {
            return LayoutElementProto.ArcLayoutElement.newBuilder().setSpacer(mImpl).build();
        }

        /** Builder for {@link ArcSpacer}. */
        public static final class Builder implements ArcLayoutElement.Builder {
            private final LayoutElementProto.ArcSpacer.Builder mImpl =
                    LayoutElementProto.ArcSpacer.newBuilder();

            Builder() {}

            /** Sets the length of this spacer, in degrees. If not defined, defaults to 0. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setLength(@NonNull DegreesProp length) {
                mImpl.setLength(length.toProto());
                return this;
            }

            /** Sets the length of this spacer, in degrees. If not defined, defaults to 0. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setLength(@NonNull DegreesProp.Builder lengthBuilder) {
                mImpl.setLength(lengthBuilder.build().toProto());
                return this;
            }

            /** Sets the thickness of this spacer, in DP. If not defined, defaults to 0. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setThickness(@NonNull DpProp thickness) {
                mImpl.setThickness(thickness.toProto());
                return this;
            }

            /** Sets the thickness of this spacer, in DP. If not defined, defaults to 0. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setThickness(@NonNull DpProp.Builder thicknessBuilder) {
                mImpl.setThickness(thicknessBuilder.build().toProto());
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull ArcModifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                return this;
            }

            /**
             * Sets {@link androidx.wear.tiles.builders.ModifiersBuilders.Modifiers} for this
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setModifiers(@NonNull ArcModifiers.Builder modifiersBuilder) {
                mImpl.setModifiers(modifiersBuilder.build().toProto());
                return this;
            }

            @Override
            @NonNull
            public ArcSpacer build() {
                return ArcSpacer.fromProto(mImpl.build());
            }
        }
    }

    /** A container that allows a standard {@link LayoutElement} to be added to an {@link Arc}. */
    public static final class ArcAdapter implements ArcLayoutElement {
        private final LayoutElementProto.ArcAdapter mImpl;

        private ArcAdapter(LayoutElementProto.ArcAdapter impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ArcAdapter fromProto(@NonNull LayoutElementProto.ArcAdapter proto) {
            return new ArcAdapter(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        LayoutElementProto.ArcAdapter toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public LayoutElementProto.ArcLayoutElement toArcLayoutElementProto() {
            return LayoutElementProto.ArcLayoutElement.newBuilder().setAdapter(mImpl).build();
        }

        /** Builder for {@link ArcAdapter}. */
        public static final class Builder implements ArcLayoutElement.Builder {
            private final LayoutElementProto.ArcAdapter.Builder mImpl =
                    LayoutElementProto.ArcAdapter.newBuilder();

            Builder() {}

            /** Sets the element to adapt to an {@link Arc}. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setContent(@NonNull LayoutElement content) {
                mImpl.setContent(content.toLayoutElementProto());
                return this;
            }

            /** Sets the element to adapt to an {@link Arc}. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setContent(@NonNull LayoutElement.Builder contentBuilder) {
                mImpl.setContent(contentBuilder.build().toLayoutElementProto());
                return this;
            }

            /**
             * Sets whether this adapter's contents should be rotated, according to its position in
             * the arc or not. As an example, assume that an {@link Image} has been added to the
             * arc, and ends up at the 3 o clock position. If rotate_contents = true, the image will
             * be placed at the 3 o clock position, and will be rotated clockwise through 90
             * degrees. If rotate_contents = false, the image will be placed at the 3 o clock
             * position, but itself will not be rotated. If not defined, defaults to false.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setRotateContents(boolean rotateContents) {
                mImpl.setRotateContents(TypesProto.BoolProp.newBuilder().setValue(rotateContents));
                return this;
            }

            @Override
            @NonNull
            public ArcAdapter build() {
                return ArcAdapter.fromProto(mImpl.build());
            }
        }
    }

    /**
     * Interface defining the root of all layout elements. This exists to act as a holder for all of
     * the actual layout elements above.
     */
    public interface LayoutElement {
        /**
         * Get the protocol buffer representation of this object.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        LayoutElementProto.LayoutElement toLayoutElementProto();

        /** Builder to create {@link LayoutElement} objects. */
        @SuppressLint("StaticFinalBuilder")
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            LayoutElement build();
        }
    }

    /**
     * Interface defining the root of all elements that can be used in an {@link Arc}. This exists
     * to act as a holder for all of the actual arc layout elements above.
     */
    public interface ArcLayoutElement {
        /**
         * Get the protocol buffer representation of this object.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        LayoutElementProto.ArcLayoutElement toArcLayoutElementProto();

        /** Builder to create {@link ArcLayoutElement} objects. */
        @SuppressLint("StaticFinalBuilder")
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            ArcLayoutElement build();
        }
    }

    /** A complete layout. */
    public static final class Layout {
        private final LayoutElementProto.Layout mImpl;

        private Layout(LayoutElementProto.Layout impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Layout fromProto(@NonNull LayoutElementProto.Layout proto) {
            return new Layout(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public LayoutElementProto.Layout toProto() {
            return mImpl;
        }

        /** Builder for {@link Layout} */
        public static final class Builder {
            private final LayoutElementProto.Layout.Builder mImpl =
                    LayoutElementProto.Layout.newBuilder();

            Builder() {}

            /** Sets the root element in the layout. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setRoot(@NonNull LayoutElement root) {
                mImpl.setRoot(root.toLayoutElementProto());
                return this;
            }

            /** Sets the root element in the layout. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setRoot(@NonNull LayoutElement.Builder rootBuilder) {
                mImpl.setRoot(rootBuilder.build().toLayoutElementProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Layout build() {
                return Layout.fromProto(mImpl.build());
            }
        }
    }

    /** Font styles, currently set up to match Wear's font styling. */
    public static class FontStyles {
        private static final int LARGE_SCREEN_WIDTH_DP = 210;

        private final int mScreenWidthDp;

        private FontStyles(int screenWidthDp) {
            this.mScreenWidthDp = screenWidthDp;
        }

        private boolean isLargeScreen() {
            return mScreenWidthDp >= LARGE_SCREEN_WIDTH_DP;
        }

        /**
         * Create a FontStyles instance, using the given device parameters to determine font sizes.
         */
        @NonNull
        public static FontStyles withDeviceParameters(@NonNull DeviceParameters deviceParameters) {
            return new FontStyles(deviceParameters.getScreenWidthDp());
        }

        /** Font style for large display text. */
        @NonNull
        public FontStyle.Builder display1() {
            return FontStyle.builder()
                    .setWeight(FONT_WEIGHT_BOLD)
                    .setSize(DimensionBuilders.sp(isLargeScreen() ? 54 : 50));
        }

        /** Font style for medium display text. */
        @NonNull
        public FontStyle.Builder display2() {
            return FontStyle.builder()
                    .setWeight(FONT_WEIGHT_BOLD)
                    .setSize(DimensionBuilders.sp(isLargeScreen() ? 44 : 40));
        }

        /** Font style for small display text. */
        @NonNull
        public FontStyle.Builder display3() {
            return FontStyle.builder()
                    .setWeight(FONT_WEIGHT_BOLD)
                    .setSize(DimensionBuilders.sp(isLargeScreen() ? 34 : 30));
        }

        /** Font style for large title text. */
        @NonNull
        public FontStyle.Builder title1() {
            return FontStyle.builder()
                    .setWeight(FONT_WEIGHT_BOLD)
                    .setSize(DimensionBuilders.sp(isLargeScreen() ? 26 : 24));
        }

        /** Font style for medium title text. */
        @NonNull
        public FontStyle.Builder title2() {
            return FontStyle.builder()
                    .setWeight(FONT_WEIGHT_BOLD)
                    .setSize(DimensionBuilders.sp(isLargeScreen() ? 22 : 20));
        }

        /** Font style for small title text. */
        @NonNull
        public FontStyle.Builder title3() {
            return FontStyle.builder()
                    .setWeight(FONT_WEIGHT_BOLD)
                    .setSize(DimensionBuilders.sp(isLargeScreen() ? 18 : 16));
        }

        /** Font style for large body text. */
        @NonNull
        public FontStyle.Builder body1() {
            return FontStyle.builder().setSize(DimensionBuilders.sp(isLargeScreen() ? 18 : 16));
        }

        /** Font style for medium body text. */
        @NonNull
        public FontStyle.Builder body2() {
            return FontStyle.builder().setSize(DimensionBuilders.sp(isLargeScreen() ? 16 : 14));
        }

        /** Font style for button text. */
        @NonNull
        public FontStyle.Builder button() {
            return FontStyle.builder()
                    .setWeight(FONT_WEIGHT_BOLD)
                    .setSize(DimensionBuilders.sp(isLargeScreen() ? 16 : 14));
        }

        /** Font style for large caption text. */
        @NonNull
        public FontStyle.Builder caption1() {
            return FontStyle.builder().setSize(DimensionBuilders.sp(isLargeScreen() ? 16 : 14));
        }

        /** Font style for medium caption text. */
        @NonNull
        public FontStyle.Builder caption2() {
            return FontStyle.builder().setSize(DimensionBuilders.sp(isLargeScreen() ? 14 : 12));
        }
    }
}
