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
import androidx.wear.tiles.builders.ActionBuilders.Action;
import androidx.wear.tiles.builders.ColorBuilders.ColorProp;
import androidx.wear.tiles.builders.DimensionBuilders.ContainerDimension;
import androidx.wear.tiles.builders.DimensionBuilders.DegreesProp;
import androidx.wear.tiles.builders.DimensionBuilders.DpProp;
import androidx.wear.tiles.builders.DimensionBuilders.ImageDimension;
import androidx.wear.tiles.builders.DimensionBuilders.LinearOrAngularDimension;
import androidx.wear.tiles.builders.DimensionBuilders.SpProp;
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

        FontStyle(LayoutElementProto.FontStyle impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Get the protocol buffer representation of this object.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY)
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
             * Sets whether the text should be rendered in a bold typeface. If not specified,
             * defaults to "false".
             *
             * @deprecated Use weight instead.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @Deprecated
            @NonNull
            public Builder setBold(boolean bold) {
                mImpl.setBold(TypesProto.BoolProp.newBuilder().setValue(bold));
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

            /** Builds an instance from accumulated values. */
            @NonNull
            public FontStyle build() {
                return new FontStyle(mImpl.build());
            }
        }
    }

    /** The padding around a {@link Box} element. */
    public static final class Padding {
        private final LayoutElementProto.Padding mImpl;

        Padding(LayoutElementProto.Padding impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Get the protocol buffer representation of this object.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public LayoutElementProto.Padding toProto() {
            return mImpl;
        }

        /** Builder for {@link Padding} */
        public static final class Builder {
            private final LayoutElementProto.Padding.Builder mImpl =
                    LayoutElementProto.Padding.newBuilder();

            Builder() {}

            /**
             * Sets the padding on the end of the content, depending on the layout direction, in DP
             * and the value of "rtl_aware".
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setEnd(@NonNull DpProp end) {
                mImpl.setEnd(end.toProto());
                return this;
            }

            /**
             * Sets the padding on the end of the content, depending on the layout direction, in DP
             * and the value of "rtl_aware".
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setEnd(@NonNull DpProp.Builder endBuilder) {
                mImpl.setEnd(endBuilder.build().toProto());
                return this;
            }

            /**
             * Sets the padding on the start of the content, depending on the layout direction, in
             * DP and the value of "rtl_aware".
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setStart(@NonNull DpProp start) {
                mImpl.setStart(start.toProto());
                return this;
            }

            /**
             * Sets the padding on the start of the content, depending on the layout direction, in
             * DP and the value of "rtl_aware".
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setStart(@NonNull DpProp.Builder startBuilder) {
                mImpl.setStart(startBuilder.build().toProto());
                return this;
            }

            /** Sets the padding at the top, in DP. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setTop(@NonNull DpProp top) {
                mImpl.setTop(top.toProto());
                return this;
            }

            /** Sets the padding at the top, in DP. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setTop(@NonNull DpProp.Builder topBuilder) {
                mImpl.setTop(topBuilder.build().toProto());
                return this;
            }

            /** Sets the padding at the bottom, in DP. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setBottom(@NonNull DpProp bottom) {
                mImpl.setBottom(bottom.toProto());
                return this;
            }

            /** Sets the padding at the bottom, in DP. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setBottom(@NonNull DpProp.Builder bottomBuilder) {
                mImpl.setBottom(bottomBuilder.build().toProto());
                return this;
            }

            /**
             * Sets whether the start/end padding is aware of RTL support. If true, the values for
             * start/end will follow the layout direction (i.e. start will refer to the right hand
             * side of the container if the device is using an RTL locale). If false, start/end will
             * always map to left/right, accordingly.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setRtlAware(boolean rtlAware) {
                mImpl.setRtlAware(TypesProto.BoolProp.newBuilder().setValue(rtlAware));
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Padding build() {
                return new Padding(mImpl.build());
            }
        }
    }

    /** The border around a {@link Box} element. */
    public static final class Border {
        private final LayoutElementProto.Border mImpl;

        Border(LayoutElementProto.Border impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Get the protocol buffer representation of this object.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public LayoutElementProto.Border toProto() {
            return mImpl;
        }

        /** Builder for {@link Border} */
        public static final class Builder {
            private final LayoutElementProto.Border.Builder mImpl =
                    LayoutElementProto.Border.newBuilder();

            Builder() {}

            /** Sets the width of the border, in DP. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setWidth(@NonNull DpProp width) {
                mImpl.setWidth(width.toProto());
                return this;
            }

            /** Sets the width of the border, in DP. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setWidth(@NonNull DpProp.Builder widthBuilder) {
                mImpl.setWidth(widthBuilder.build().toProto());
                return this;
            }

            /** Sets the color of the border. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setColor(@NonNull ColorProp color) {
                mImpl.setColor(color.toProto());
                return this;
            }

            /** Sets the color of the border. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setColor(@NonNull ColorProp.Builder colorBuilder) {
                mImpl.setColor(colorBuilder.build().toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Border build() {
                return new Border(mImpl.build());
            }
        }
    }

    /** The corner of a {@link Box} element. */
    public static final class Corner {
        private final LayoutElementProto.Corner mImpl;

        Corner(LayoutElementProto.Corner impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Get the protocol buffer representation of this object.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public LayoutElementProto.Corner toProto() {
            return mImpl;
        }

        /** Builder for {@link Corner} */
        public static final class Builder {
            private final LayoutElementProto.Corner.Builder mImpl =
                    LayoutElementProto.Corner.newBuilder();

            Builder() {}

            /** Sets the radius of the corner in DP. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setRadius(@NonNull DpProp radius) {
                mImpl.setRadius(radius.toProto());
                return this;
            }

            /** Sets the radius of the corner in DP. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setRadius(@NonNull DpProp.Builder radiusBuilder) {
                mImpl.setRadius(radiusBuilder.build().toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Corner build() {
                return new Corner(mImpl.build());
            }
        }
    }

    /** The style of a {@link Text} element. */
    public static final class TextStyle {
        private final LayoutElementProto.TextStyle mImpl;

        TextStyle(LayoutElementProto.TextStyle impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Get the protocol buffer representation of this object.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public LayoutElementProto.TextStyle toProto() {
            return mImpl;
        }

        /** Builder for {@link TextStyle} */
        public static final class Builder {
            private final LayoutElementProto.TextStyle.Builder mImpl =
                    LayoutElementProto.TextStyle.newBuilder();

            Builder() {}

            /**
             * Sets the text color. If not defined, defaults to white.
             *
             * @deprecated Use color property of {@link FontStyle} instead.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @Deprecated
            @NonNull
            public Builder setColor(@NonNull ColorProp color) {
                mImpl.setColor(color.toProto());
                return this;
            }

            /**
             * Sets the text color. If not defined, defaults to white.
             *
             * @deprecated Use color property of {@link FontStyle} instead.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @Deprecated
            @NonNull
            public Builder setColor(@NonNull ColorProp.Builder colorBuilder) {
                mImpl.setColor(colorBuilder.build().toProto());
                return this;
            }

            /**
             * Sets the maximum number of lines that can be represented by the {@link Text} element.
             * If not defined, the {@link Text} element will be treated as a single-line element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setMaxLines(@IntRange(from = 0) int maxLines) {
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
             * Sets specifies how to handle text which overflows the bound of the {@link Text}
             * element. A {@link Text} element will grow as large as possible inside its parent
             * container (while still respecting max_lines); if it cannot grow large enough to
             * render all of its text, the text which cannot fit inside its container will be
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

            /** Builds an instance from accumulated values. */
            @NonNull
            public TextStyle build() {
                return new TextStyle(mImpl.build());
            }
        }
    }

    /** The style of a {@link Spannable} element. */
    public static final class SpannableStyle {
        private final LayoutElementProto.SpannableStyle mImpl;

        SpannableStyle(LayoutElementProto.SpannableStyle impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Get the protocol buffer representation of this object.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public LayoutElementProto.SpannableStyle toProto() {
            return mImpl;
        }

        /** Builder for {@link SpannableStyle} */
        public static final class Builder {
            private final LayoutElementProto.SpannableStyle.Builder mImpl =
                    LayoutElementProto.SpannableStyle.newBuilder();

            Builder() {}

            /**
             * Sets the maximum number of lines that can be represented by the {@link Spannable}
             * element. If not defined, the {@link Text} element will be treated as a single-line
             * element.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setMaxLines(@IntRange(from = 0) int maxLines) {
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
             * Sets specifies how to handle content which overflows the bound of the {@link
             * Spannable} element. A {@link Spannable} element will grow as large as possible inside
             * its parent container (while still respecting max_lines); if it cannot grow large
             * enough to render all of its content, the content which cannot fit inside its
             * container will be truncated. If not defined, defaults to TEXT_OVERFLOW_TRUNCATE.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setOverflow(@TextOverflow int overflow) {
                mImpl.setOverflow(
                        LayoutElementProto.TextOverflowProp.newBuilder()
                                .setValue(LayoutElementProto.TextOverflow.forNumber(overflow)));
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public SpannableStyle build() {
                return new SpannableStyle(mImpl.build());
            }
        }
    }

    /** The style of a {@link Box}. */
    public static final class BoxStyle {
        private final LayoutElementProto.BoxStyle mImpl;

        BoxStyle(LayoutElementProto.BoxStyle impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Get the protocol buffer representation of this object.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public LayoutElementProto.BoxStyle toProto() {
            return mImpl;
        }

        /** Builder for {@link BoxStyle} */
        public static final class Builder {
            private final LayoutElementProto.BoxStyle.Builder mImpl =
                    LayoutElementProto.BoxStyle.newBuilder();

            Builder() {}

            /**
             * Sets the background color for this {@link Box}. If not defined, defaults to being
             * transparent.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setBackgroundColor(@NonNull ColorProp backgroundColor) {
                mImpl.setBackgroundColor(backgroundColor.toProto());
                return this;
            }

            /**
             * Sets the background color for this {@link Box}. If not defined, defaults to being
             * transparent.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setBackgroundColor(@NonNull ColorProp.Builder backgroundColorBuilder) {
                mImpl.setBackgroundColor(backgroundColorBuilder.build().toProto());
                return this;
            }

            /**
             * Sets an optional padding inside of this {@link Box}. If not defined, {@link Box} will
             * not have any padding.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setPadding(@NonNull Padding padding) {
                mImpl.setPadding(padding.toProto());
                return this;
            }

            /**
             * Sets an optional padding inside of this {@link Box}. If not defined, {@link Box} will
             * not have any padding.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setPadding(@NonNull Padding.Builder paddingBuilder) {
                mImpl.setPadding(paddingBuilder.build().toProto());
                return this;
            }

            /**
             * Sets an optional border for this {@link Box}. If not defined, {@link Box} will not
             * have a border.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setBorder(@NonNull Border border) {
                mImpl.setBorder(border.toProto());
                return this;
            }

            /**
             * Sets an optional border for this {@link Box}. If not defined, {@link Box} will not
             * have a border.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setBorder(@NonNull Border.Builder borderBuilder) {
                mImpl.setBorder(borderBuilder.build().toProto());
                return this;
            }

            /**
             * Sets the corner properties of this {@link Box}. This only affects the drawing of this
             * {@link Box} if either "color" or "border" are also set. If not defined, defaults to
             * having a square corner.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setCorner(@NonNull Corner corner) {
                mImpl.setCorner(corner.toProto());
                return this;
            }

            /**
             * Sets the corner properties of this {@link Box}. This only affects the drawing of this
             * {@link Box} if either "color" or "border" are also set. If not defined, defaults to
             * having a square corner.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setCorner(@NonNull Corner.Builder cornerBuilder) {
                mImpl.setCorner(cornerBuilder.build().toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public BoxStyle build() {
                return new BoxStyle(mImpl.build());
            }
        }
    }

    /** The style of a line. */
    public static final class LineStyle {
        private final LayoutElementProto.LineStyle mImpl;

        LineStyle(LayoutElementProto.LineStyle impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Get the protocol buffer representation of this object.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public LayoutElementProto.LineStyle toProto() {
            return mImpl;
        }

        /** Builder for {@link LineStyle} */
        public static final class Builder {
            private final LayoutElementProto.LineStyle.Builder mImpl =
                    LayoutElementProto.LineStyle.newBuilder();

            Builder() {}

            /** Sets color of this line. If not defined, defaults to white. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setColor(@NonNull ColorProp color) {
                mImpl.setColor(color.toProto());
                return this;
            }

            /** Sets color of this line. If not defined, defaults to white. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setColor(@NonNull ColorProp.Builder colorBuilder) {
                mImpl.setColor(colorBuilder.build().toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public LineStyle build() {
                return new LineStyle(mImpl.build());
            }
        }
    }

    /** A text string. */
    public static final class Text implements Span, LayoutElement {
        private final LayoutElementProto.Text mImpl;

        Text(LayoutElementProto.Text impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        LayoutElementProto.Text toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public LayoutElementProto.Span toSpanProto() {
            return LayoutElementProto.Span.newBuilder().setText(mImpl).build();
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public LayoutElementProto.LayoutElement toLayoutElementProto() {
            return LayoutElementProto.LayoutElement.newBuilder().setText(mImpl).build();
        }

        /** Builder for {@link Text}. */
        public static final class Builder implements Span.Builder, LayoutElement.Builder {
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
             * Sets an optional style for this text string.
             *
             * @deprecated Use {@link Spannable} with {@link SpannableStyle} instead.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @Deprecated
            @NonNull
            public Builder setStyle(@NonNull TextStyle style) {
                mImpl.setStyle(style.toProto());
                return this;
            }

            /**
             * Sets an optional style for this text string.
             *
             * @deprecated Use {@link Spannable} with {@link SpannableStyle} instead.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @Deprecated
            @NonNull
            public Builder setStyle(@NonNull TextStyle.Builder styleBuilder) {
                mImpl.setStyle(styleBuilder.build().toProto());
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

            @Override
            @NonNull
            public Text build() {
                return new Text(mImpl.build());
            }
        }
    }

    /**
     * A holder for an element which can have associated {@link
     * androidx.wear.tiles.builders.ActionBuilders.Action} items for click events. When an element
     * wrapped in a {@link Clickable} is clicked, it will fire the associated action.
     */
    public static final class Clickable implements LayoutElement {
        private final LayoutElementProto.Clickable mImpl;

        Clickable(LayoutElementProto.Clickable impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        LayoutElementProto.Clickable toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public LayoutElementProto.LayoutElement toLayoutElementProto() {
            return LayoutElementProto.LayoutElement.newBuilder().setClickable(mImpl).build();
        }

        /** Builder for {@link Clickable}. */
        public static final class Builder implements LayoutElement.Builder {
            private final LayoutElementProto.Clickable.Builder mImpl =
                    LayoutElementProto.Clickable.newBuilder();

            Builder() {}

            /** Sets the ID associated with this action. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setId(@NonNull String id) {
                mImpl.setId(id);
                return this;
            }

            /** Sets the layout element to attach the action to. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setContent(@NonNull LayoutElement content) {
                mImpl.setContent(content.toLayoutElementProto());
                return this;
            }

            /** Sets the layout element to attach the action to. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setContent(@NonNull LayoutElement.Builder contentBuilder) {
                mImpl.setContent(contentBuilder.build().toLayoutElementProto());
                return this;
            }

            /** Sets the action to perform when "content" is clicked. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setOnClick(@NonNull Action onClick) {
                mImpl.setOnClick(onClick.toActionProto());
                return this;
            }

            /** Sets the action to perform when "content" is clicked. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setOnClick(@NonNull Action.Builder onClickBuilder) {
                mImpl.setOnClick(onClickBuilder.build().toActionProto());
                return this;
            }

            @Override
            @NonNull
            public Clickable build() {
                return new Clickable(mImpl.build());
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

        Image(LayoutElementProto.Image impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        LayoutElementProto.Image toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY)
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

            @Override
            @NonNull
            public Image build() {
                return new Image(mImpl.build());
            }
        }
    }

    /** A simple spacer, typically used to provide padding between adjacent elements. */
    public static final class Spacer implements LayoutElement {
        private final LayoutElementProto.Spacer mImpl;

        Spacer(LayoutElementProto.Spacer impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        LayoutElementProto.Spacer toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY)
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
            public Builder setWidth(@NonNull LinearOrAngularDimension width) {
                mImpl.setWidth(width.toLinearOrAngularDimensionProto());
                return this;
            }

            /**
             * Sets the width of this {@link Spacer}. When this is added as the direct child of an
             * {@link Arc}, this must be specified as an angular dimension, otherwise a linear
             * dimension must be used. If not defined, defaults to 0.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setWidth(@NonNull LinearOrAngularDimension.Builder widthBuilder) {
                mImpl.setWidth(widthBuilder.build().toLinearOrAngularDimensionProto());
                return this;
            }

            /** Sets the height of this spacer. If not defined, defaults to 0. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setHeight(@NonNull DpProp height) {
                mImpl.setHeight(height.toProto());
                return this;
            }

            /** Sets the height of this spacer. If not defined, defaults to 0. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setHeight(@NonNull DpProp.Builder heightBuilder) {
                mImpl.setHeight(heightBuilder.build().toProto());
                return this;
            }

            @Override
            @NonNull
            public Spacer build() {
                return new Spacer(mImpl.build());
            }
        }
    }

    /**
     * A container which stacks all of its children on top of one another. This also allows to add a
     * background color, or to have a border around them with some padding.
     */
    public static final class Box implements LayoutElement {
        private final LayoutElementProto.Box mImpl;

        Box(LayoutElementProto.Box impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        LayoutElementProto.Box toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY)
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

            /** Sets the style of the {@link Box} (padding, background color, border etc). */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setStyle(@NonNull BoxStyle style) {
                mImpl.setStyle(style.toProto());
                return this;
            }

            /** Sets the style of the {@link Box} (padding, background color, border etc). */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setStyle(@NonNull BoxStyle.Builder styleBuilder) {
                mImpl.setStyle(styleBuilder.build().toProto());
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

            @Override
            @NonNull
            public Box build() {
                return new Box(mImpl.build());
            }
        }
    }

    /**
     * Interface defining a single {@link Span}. Each {@link Span} forms part of a larger {@link
     * Spannable} widget. At the moment, the only widgets which can be added to {@link Spannable}
     * containers are {@link Text} elements.
     */
    public interface Span {
        /**
         * Get the protocol buffer representation of this object.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY)
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

        Spannable(LayoutElementProto.Spannable impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        LayoutElementProto.Spannable toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY)
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

            /** Sets the style of this {@link Spannable}. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setStyle(@NonNull SpannableStyle style) {
                mImpl.setStyle(style.toProto());
                return this;
            }

            /** Sets the style of this {@link Spannable}. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setStyle(@NonNull SpannableStyle.Builder styleBuilder) {
                mImpl.setStyle(styleBuilder.build().toProto());
                return this;
            }

            @Override
            @NonNull
            public Spannable build() {
                return new Spannable(mImpl.build());
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

        Column(LayoutElementProto.Column impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        LayoutElementProto.Column toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY)
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

            @Override
            @NonNull
            public Column build() {
                return new Column(mImpl.build());
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

        Row(LayoutElementProto.Row impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        LayoutElementProto.Row toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY)
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

            @Override
            @NonNull
            public Row build() {
                return new Row(mImpl.build());
            }
        }
    }

    /**
     * A wrapper for an element which has a screen reader description associated with it. This
     * should generally be used sparingly, and in most cases should only be applied to the top-level
     * layout element or to Clickables.
     */
    public static final class Audible implements LayoutElement {
        private final LayoutElementProto.Audible mImpl;

        Audible(LayoutElementProto.Audible impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        LayoutElementProto.Audible toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public LayoutElementProto.LayoutElement toLayoutElementProto() {
            return LayoutElementProto.LayoutElement.newBuilder().setAudible(mImpl).build();
        }

        /** Builder for {@link Audible}. */
        public static final class Builder implements LayoutElement.Builder {
            private final LayoutElementProto.Audible.Builder mImpl =
                    LayoutElementProto.Audible.newBuilder();

            Builder() {}

            /** Sets the element to wrap with the screen reader description. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setContent(@NonNull LayoutElement content) {
                mImpl.setContent(content.toLayoutElementProto());
                return this;
            }

            /** Sets the element to wrap with the screen reader description. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setContent(@NonNull LayoutElement.Builder contentBuilder) {
                mImpl.setContent(contentBuilder.build().toLayoutElementProto());
                return this;
            }

            /**
             * Sets the accessibility label associated with this element. This will be dictated when
             * the element is focused by the screen reader.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setAccessibilityLabel(@NonNull String accessibilityLabel) {
                mImpl.setAccessibilityLabel(accessibilityLabel);
                return this;
            }

            @Override
            @NonNull
            public Audible build() {
                return new Audible(mImpl.build());
            }
        }
    }

    /**
     * A line. When added to a normal container, this renders as a horizontal line which can be used
     * to provide a visual break between elements. When added to an arc, it will render as a round
     * progress bar.
     */
    public static final class Line implements LayoutElement {
        private final LayoutElementProto.Line mImpl;

        Line(LayoutElementProto.Line impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        LayoutElementProto.Line toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public LayoutElementProto.LayoutElement toLayoutElementProto() {
            return LayoutElementProto.LayoutElement.newBuilder().setLine(mImpl).build();
        }

        /** Builder for {@link Line}. */
        public static final class Builder implements LayoutElement.Builder {
            private final LayoutElementProto.Line.Builder mImpl =
                    LayoutElementProto.Line.newBuilder();

            Builder() {}

            /**
             * Sets the length of this {@link Line}. When this is added as the direct child of an
             * {@link Arc}, this must be specified as an AngularDimension, otherwise a
             * LinearDimension must be used. If not defined, defaults to 0.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setLength(@NonNull LinearOrAngularDimension length) {
                mImpl.setLength(length.toLinearOrAngularDimensionProto());
                return this;
            }

            /**
             * Sets the length of this {@link Line}. When this is added as the direct child of an
             * {@link Arc}, this must be specified as an AngularDimension, otherwise a
             * LinearDimension must be used. If not defined, defaults to 0.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setLength(@NonNull LinearOrAngularDimension.Builder lengthBuilder) {
                mImpl.setLength(lengthBuilder.build().toLinearOrAngularDimensionProto());
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

            /** Sets the style of this line. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setStyle(@NonNull LineStyle style) {
                mImpl.setStyle(style.toProto());
                return this;
            }

            /** Sets the style of this line. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setStyle(@NonNull LineStyle.Builder styleBuilder) {
                mImpl.setStyle(styleBuilder.build().toProto());
                return this;
            }

            @Override
            @NonNull
            public Line build() {
                return new Line(mImpl.build());
            }
        }
    }

    /**
     * An arc container. This container will fill itself to a circle, which fits inside its parent
     * container, and all of its children will be placed on that circle. The fields anchor_angle and
     * anchor_type can be used to specify where to draw children within this circle.
     *
     * <p>Note that there are two special cases. {@link Text} and {@link Line} elements which are
     * added as direct descendants to an arc will be drawn as a curved widget around the arc, rather
     * than just placed on the arc and drawn normally.
     */
    public static final class Arc implements LayoutElement {
        private final LayoutElementProto.Arc mImpl;

        Arc(LayoutElementProto.Arc impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        LayoutElementProto.Arc toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY)
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
            public Builder addContent(@NonNull LayoutElement content) {
                mImpl.addContents(content.toLayoutElementProto());
                return this;
            }

            /** Adds one item to contents of this container. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder addContent(@NonNull LayoutElement.Builder contentBuilder) {
                mImpl.addContents(contentBuilder.build().toLayoutElementProto());
                return this;
            }

            /**
             * Sets the length of this {@link Arc} as an angle. If not defined, this will size
             * itself to fit all of its children. If defined, this should be a value > 0 degrees.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setLength(@NonNull DegreesProp length) {
                mImpl.setLength(length.toProto());
                return this;
            }

            /**
             * Sets the length of this {@link Arc} as an angle. If not defined, this will size
             * itself to fit all of its children. If defined, this should be a value > 0 degrees.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setLength(@NonNull DegreesProp.Builder lengthBuilder) {
                mImpl.setLength(lengthBuilder.build().toProto());
                return this;
            }

            /**
             * Sets the thickness of this {@link Arc}. If not defined, this will size itself to fit
             * all of its children.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setThickness(@NonNull DpProp thickness) {
                mImpl.setThickness(thickness.toProto());
                return this;
            }

            /**
             * Sets the thickness of this {@link Arc}. If not defined, this will size itself to fit
             * all of its children.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setThickness(@NonNull DpProp.Builder thicknessBuilder) {
                mImpl.setThickness(thicknessBuilder.build().toProto());
                return this;
            }

            /**
             * Sets whether this {@link Arc}'s children should be rotated, according to its position
             * in the arc or not. As an example, assume that an {@link Image} has been added to the
             * arc, and ends up at the 3 o clock position. If rotate_contents = true, the image will
             * be placed at the 3 o clock position, and will be rotated clockwise through 90
             * degrees. If rotate_contents = false, the image will be placed at the 3 o clock
             * position, but itself will not be rotated. If not defined, defaults to true.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setRotateContents(boolean rotateContents) {
                mImpl.setRotateContents(TypesProto.BoolProp.newBuilder().setValue(rotateContents));
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
             * Sets how to align the contents of this container relative to anchor_angle. See the
             * descriptions of options in {@link ArcAnchorType} for more information. If not
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

            @Override
            @NonNull
            public Arc build() {
                return new Arc(mImpl.build());
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
        @RestrictTo(Scope.LIBRARY)
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
        public FontStyle display1() {
            return FontStyle.builder()
                    .setBold(true)
                    .setSize(DimensionBuilders.sp(isLargeScreen() ? 54 : 50))
                    .build();
        }

        /** Font style for medium display text. */
        @NonNull
        public FontStyle display2() {
            return FontStyle.builder()
                    .setBold(true)
                    .setSize(DimensionBuilders.sp(isLargeScreen() ? 44 : 40))
                    .build();
        }

        /** Font style for small display text. */
        @NonNull
        public FontStyle display3() {
            return FontStyle.builder()
                    .setBold(true)
                    .setSize(DimensionBuilders.sp(isLargeScreen() ? 34 : 30))
                    .build();
        }

        /** Font style for large title text. */
        @NonNull
        public FontStyle title1() {
            return FontStyle.builder()
                    .setBold(true)
                    .setSize(DimensionBuilders.sp(isLargeScreen() ? 26 : 24))
                    .build();
        }

        /** Font style for medium title text. */
        @NonNull
        public FontStyle title2() {
            return FontStyle.builder()
                    .setBold(true)
                    .setSize(DimensionBuilders.sp(isLargeScreen() ? 22 : 20))
                    .build();
        }

        /** Font style for small title text. */
        @NonNull
        public FontStyle title3() {
            return FontStyle.builder()
                    .setBold(true)
                    .setSize(DimensionBuilders.sp(isLargeScreen() ? 18 : 16))
                    .build();
        }

        /** Font style for large body text. */
        @NonNull
        public FontStyle body1() {
            return FontStyle.builder()
                    .setSize(DimensionBuilders.sp(isLargeScreen() ? 18 : 16))
                    .build();
        }

        /** Font style for medium body text. */
        @NonNull
        public FontStyle body2() {
            return FontStyle.builder()
                    .setSize(DimensionBuilders.sp(isLargeScreen() ? 16 : 14))
                    .build();
        }

        /** Font style for button text. */
        @NonNull
        public FontStyle button() {
            return FontStyle.builder()
                    .setBold(true)
                    .setSize(DimensionBuilders.sp(isLargeScreen() ? 16 : 14))
                    .build();
        }

        /** Font style for large caption text. */
        @NonNull
        public FontStyle caption1() {
            return FontStyle.builder()
                    .setSize(DimensionBuilders.sp(isLargeScreen() ? 16 : 14))
                    .build();
        }

        /** Font style for medium caption text. */
        @NonNull
        public FontStyle caption2() {
            return FontStyle.builder()
                    .setSize(DimensionBuilders.sp(isLargeScreen() ? 14 : 12))
                    .build();
        }
    }
}
