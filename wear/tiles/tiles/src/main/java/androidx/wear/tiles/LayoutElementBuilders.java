/*
 * Copyright 2021-2022 The Android Open Source Project
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

package androidx.wear.tiles;

import static androidx.wear.protolayout.expression.Preconditions.checkNotNull;

import static java.util.stream.Collectors.toList;

import android.annotation.SuppressLint;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.proto.AlignmentProto;
import androidx.wear.protolayout.proto.DimensionProto;
import androidx.wear.protolayout.proto.FingerprintProto;
import androidx.wear.protolayout.proto.FingerprintProto.TreeFingerprint;
import androidx.wear.protolayout.proto.LayoutElementProto;
import androidx.wear.protolayout.proto.TypesProto;
import androidx.wear.protolayout.protobuf.InvalidProtocolBufferException;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.List;

/**
 * Builders for composable layout elements that can be combined together to create renderable UI
 * layouts.
 *
 * @deprecated Use {@link androidx.wear.protolayout.LayoutElementBuilders} instead.
 */
@Deprecated
public final class LayoutElementBuilders {
    private LayoutElementBuilders() {}

    /** The horizontal alignment of an element within its container. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
        HORIZONTAL_ALIGN_UNDEFINED,
        HORIZONTAL_ALIGN_LEFT,
        HORIZONTAL_ALIGN_CENTER,
        HORIZONTAL_ALIGN_RIGHT,
        HORIZONTAL_ALIGN_START,
        HORIZONTAL_ALIGN_END
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface HorizontalAlignment {}

    /** Horizontal alignment is undefined. */
    public static final int HORIZONTAL_ALIGN_UNDEFINED = 0;

    /** Horizontally align to the left. */
    public static final int HORIZONTAL_ALIGN_LEFT = 1;

    /** Horizontally align to center. */
    public static final int HORIZONTAL_ALIGN_CENTER = 2;

    /** Horizontally align to the right. */
    public static final int HORIZONTAL_ALIGN_RIGHT = 3;

    /** Horizontally align to the content start (left in LTR layouts, right in RTL layouts). */
    public static final int HORIZONTAL_ALIGN_START = 4;

    /** Horizontally align to the content end (right in LTR layouts, left in RTL layouts). */
    public static final int HORIZONTAL_ALIGN_END = 5;

    /** The vertical alignment of an element within its container. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
        VERTICAL_ALIGN_UNDEFINED,
        VERTICAL_ALIGN_TOP,
        VERTICAL_ALIGN_CENTER,
        VERTICAL_ALIGN_BOTTOM
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface VerticalAlignment {}

    /** Vertical alignment is undefined. */
    public static final int VERTICAL_ALIGN_UNDEFINED = 0;

    /** Vertically align to the top. */
    public static final int VERTICAL_ALIGN_TOP = 1;

    /** Vertically align to center. */
    public static final int VERTICAL_ALIGN_CENTER = 2;

    /** Vertically align to the bottom. */
    public static final int VERTICAL_ALIGN_BOTTOM = 3;

    /** The weight to be applied to the font. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({FONT_WEIGHT_UNDEFINED, FONT_WEIGHT_NORMAL, FONT_WEIGHT_MEDIUM, FONT_WEIGHT_BOLD})
    @Retention(RetentionPolicy.SOURCE)
    @OptIn(markerClass = TilesExperimental.class)
    public @interface FontWeight {}

    /** Font weight is undefined. */
    public static final int FONT_WEIGHT_UNDEFINED = 0;

    /** Normal font weight. */
    public static final int FONT_WEIGHT_NORMAL = 400;

    /** Medium font weight. */
    @TilesExperimental public static final int FONT_WEIGHT_MEDIUM = 500;

    /** Bold font weight. */
    public static final int FONT_WEIGHT_BOLD = 700;

    /**
     * The variant of a font. Some renderers may use different fonts for title and body text, which
     * can be selected using this field.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({FONT_VARIANT_UNDEFINED, FONT_VARIANT_TITLE, FONT_VARIANT_BODY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FontVariant {}

    /** Font variant is undefined. */
    public static final int FONT_VARIANT_UNDEFINED = 0;

    /** Font variant suited for title text. */
    public static final int FONT_VARIANT_TITLE = 1;

    /** Font variant suited for body text. */
    public static final int FONT_VARIANT_BODY = 2;

    /**
     * The alignment of a {@link SpanImage} within the line height of the surrounding {@link
     * Spannable}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
        SPAN_VERTICAL_ALIGN_UNDEFINED,
        SPAN_VERTICAL_ALIGN_BOTTOM,
        SPAN_VERTICAL_ALIGN_TEXT_BASELINE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SpanVerticalAlignment {}

    /** Alignment is undefined. */
    public static final int SPAN_VERTICAL_ALIGN_UNDEFINED = 0;

    /**
     * Align to the bottom of the line (descent of the largest text in this line). If there is no
     * text in the line containing this image, this will align to the bottom of the line, where the
     * line height is defined as the height of the largest image in the line.
     */
    public static final int SPAN_VERTICAL_ALIGN_BOTTOM = 1;

    /**
     * Align to the baseline of the text. Note that if the line in the {@link Spannable} which
     * contains this image does not contain any text, the effects of using this alignment are
     * undefined.
     */
    public static final int SPAN_VERTICAL_ALIGN_TEXT_BASELINE = 2;

    /** Alignment of a text element. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({TEXT_ALIGN_UNDEFINED, TEXT_ALIGN_START, TEXT_ALIGN_CENTER, TEXT_ALIGN_END})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TextAlignment {}

    /** Alignment is undefined. */
    public static final int TEXT_ALIGN_UNDEFINED = 0;

    /**
     * Align to the "start" of the {@link androidx.wear.tiles.LayoutElementBuilders.Text} element
     * (left in LTR layouts, right in RTL layouts).
     */
    public static final int TEXT_ALIGN_START = 1;

    /**
     * Align to the center of the {@link androidx.wear.tiles.LayoutElementBuilders.Text} element.
     */
    public static final int TEXT_ALIGN_CENTER = 2;

    /**
     * Align to the "end" of the {@link androidx.wear.tiles.LayoutElementBuilders.Text} element
     * (right in LTR layouts, left in RTL layouts).
     */
    public static final int TEXT_ALIGN_END = 3;

    /** How text that will not fit inside the bounds of a {@link Text} element will be handled. */
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
     * The anchor position of an {@link androidx.wear.tiles.LayoutElementBuilders.Arc}'s elements.
     * This is used to specify how elements added to an {@link
     * androidx.wear.tiles.LayoutElementBuilders.Arc} should be laid out with respect to
     * anchor_angle.
     *
     * <p>As an example, assume that the following diagrams are wrapped to an arc, and each
     * represents an {@link androidx.wear.tiles.LayoutElementBuilders.Arc} element containing a
     * single {@link androidx.wear.tiles.LayoutElementBuilders.Text} element. The {@link
     * androidx.wear.tiles.LayoutElementBuilders.Text} element's anchor_angle is "0" for all cases.
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

    /** An extensible {@code HorizontalAlignment} property. */
    public static final class HorizontalAlignmentProp {
        private final AlignmentProto.HorizontalAlignmentProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        private HorizontalAlignmentProp(
                AlignmentProto.HorizontalAlignmentProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. Intended for testing purposes only. */
        @HorizontalAlignment
        public int getValue() {
            return mImpl.getValue().getNumber();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static HorizontalAlignmentProp fromProto(
                @NonNull AlignmentProto.HorizontalAlignmentProp proto) {
            return new HorizontalAlignmentProp(proto, null);
        }

        @NonNull
        AlignmentProto.HorizontalAlignmentProp toProto() {
            return mImpl;
        }

        /** Builder for {@link HorizontalAlignmentProp} */
        public static final class Builder {
            private final AlignmentProto.HorizontalAlignmentProp.Builder mImpl =
                    AlignmentProto.HorizontalAlignmentProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-384830516);

            public Builder() {}

            /** Sets the value. */
            @NonNull
            public Builder setValue(@HorizontalAlignment int value) {
                mImpl.setValue(AlignmentProto.HorizontalAlignment.forNumber(value));
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public HorizontalAlignmentProp build() {
                return HorizontalAlignmentProp.fromProto(mImpl.build());
            }
        }
    }

    /** An extensible {@code VerticalAlignment} property. */
    public static final class VerticalAlignmentProp {
        private final AlignmentProto.VerticalAlignmentProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        VerticalAlignmentProp(
                AlignmentProto.VerticalAlignmentProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. Intended for testing purposes only. */
        @VerticalAlignment
        public int getValue() {
            return mImpl.getValue().getNumber();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static VerticalAlignmentProp fromProto(
                @NonNull AlignmentProto.VerticalAlignmentProp proto) {
            return new VerticalAlignmentProp(proto, null);
        }

        @NonNull
        AlignmentProto.VerticalAlignmentProp toProto() {
            return mImpl;
        }

        /** Builder for {@link VerticalAlignmentProp} */
        public static final class Builder {
            private final AlignmentProto.VerticalAlignmentProp.Builder mImpl =
                    AlignmentProto.VerticalAlignmentProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1443510393);

            public Builder() {}

            /** Sets the value. */
            @NonNull
            public Builder setValue(@VerticalAlignment int value) {
                mImpl.setValue(AlignmentProto.VerticalAlignment.forNumber(value));
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public VerticalAlignmentProp build() {
                return new VerticalAlignmentProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** An extensible {@code FontWeight} property. */
    public static final class FontWeightProp {
        private final LayoutElementProto.FontWeightProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        FontWeightProp(LayoutElementProto.FontWeightProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. Intended for testing purposes only. */
        @FontWeight
        public int getValue() {
            return mImpl.getValue().getNumber();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static FontWeightProp fromProto(@NonNull LayoutElementProto.FontWeightProp proto) {
            return new FontWeightProp(proto, null);
        }

        @NonNull
        LayoutElementProto.FontWeightProp toProto() {
            return mImpl;
        }

        /** Builder for {@link FontWeightProp} */
        public static final class Builder {
            private final LayoutElementProto.FontWeightProp.Builder mImpl =
                    LayoutElementProto.FontWeightProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1793388920);

            public Builder() {}

            /** Sets the value. */
            @NonNull
            public Builder setValue(@FontWeight int value) {
                mImpl.setValue(LayoutElementProto.FontWeight.forNumber(value));
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public FontWeightProp build() {
                return new FontWeightProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** An extensible {@code FontVariant} property. */
    @TilesExperimental
    public static final class FontVariantProp {
        private final LayoutElementProto.FontVariantProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        FontVariantProp(
                LayoutElementProto.FontVariantProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. Intended for testing purposes only. */
        @FontVariant
        public int getValue() {
            return mImpl.getValue().getNumber();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static FontVariantProp fromProto(@NonNull LayoutElementProto.FontVariantProp proto) {
            return new FontVariantProp(proto, null);
        }

        @NonNull
        LayoutElementProto.FontVariantProp toProto() {
            return mImpl;
        }

        /** Builder for {@link FontVariantProp} */
        public static final class Builder {
            private final LayoutElementProto.FontVariantProp.Builder mImpl =
                    LayoutElementProto.FontVariantProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-293831500);

            public Builder() {}

            /** Sets the value. */
            @NonNull
            public Builder setValue(@FontVariant int value) {
                mImpl.setValue(LayoutElementProto.FontVariant.forNumber(value));
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public FontVariantProp build() {
                return new FontVariantProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** An extensible {@code SpanVerticalAlignment} property. */
    public static final class SpanVerticalAlignmentProp {
        private final LayoutElementProto.SpanVerticalAlignmentProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        SpanVerticalAlignmentProp(
                LayoutElementProto.SpanVerticalAlignmentProp impl,
                @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. Intended for testing purposes only. */
        @SpanVerticalAlignment
        public int getValue() {
            return mImpl.getValue().getNumber();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static SpanVerticalAlignmentProp fromProto(
                @NonNull LayoutElementProto.SpanVerticalAlignmentProp proto) {
            return new SpanVerticalAlignmentProp(proto, null);
        }

        @NonNull
        LayoutElementProto.SpanVerticalAlignmentProp toProto() {
            return mImpl;
        }

        /** Builder for {@link SpanVerticalAlignmentProp} */
        public static final class Builder {
            private final LayoutElementProto.SpanVerticalAlignmentProp.Builder mImpl =
                    LayoutElementProto.SpanVerticalAlignmentProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1008812329);

            public Builder() {}

            /** Sets the value. */
            @NonNull
            public Builder setValue(@SpanVerticalAlignment int value) {
                mImpl.setValue(LayoutElementProto.SpanVerticalAlignment.forNumber(value));
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public SpanVerticalAlignmentProp build() {
                return new SpanVerticalAlignmentProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** The styling of a font (e.g. font size, and metrics). */
    public static final class FontStyle {
        private final LayoutElementProto.FontStyle mImpl;
        @Nullable private final Fingerprint mFingerprint;

        FontStyle(LayoutElementProto.FontStyle impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the size of the font, in scaled pixels (sp). If not specified, defaults to the size
         * of the system's "body" font. Intended for testing purposes only.
         */
        @Nullable
        public DimensionBuilders.SpProp getSize() {
            List<DimensionProto.SpProp> sizes = mImpl.getSizeList();
            if (!sizes.isEmpty()) {
                return DimensionBuilders.SpProp.fromProto(sizes.get(0));
            } else {
                return null;
            }
        }

        /**
         * Gets whether the text should be rendered in a italic typeface. If not specified, defaults
         * to "false". Intended for testing purposes only.
         */
        @Nullable
        public TypeBuilders.BoolProp getItalic() {
            if (mImpl.hasItalic()) {
                return TypeBuilders.BoolProp.fromProto(mImpl.getItalic());
            } else {
                return null;
            }
        }

        /**
         * Gets whether the text should be rendered with an underline. If not specified, defaults to
         * "false". Intended for testing purposes only.
         */
        @Nullable
        public TypeBuilders.BoolProp getUnderline() {
            if (mImpl.hasUnderline()) {
                return TypeBuilders.BoolProp.fromProto(mImpl.getUnderline());
            } else {
                return null;
            }
        }

        /**
         * Gets the text color. If not defined, defaults to white. Intended for testing purposes
         * only.
         */
        @Nullable
        public ColorBuilders.ColorProp getColor() {
            if (mImpl.hasColor()) {
                return ColorBuilders.ColorProp.fromProto(mImpl.getColor());
            } else {
                return null;
            }
        }

        /**
         * Gets the weight of the font. If the provided value is not supported on a platform, the
         * nearest supported value will be used. If not defined, or when set to an invalid value,
         * defaults to "normal". Intended for testing purposes only.
         */
        @Nullable
        public FontWeightProp getWeight() {
            if (mImpl.hasWeight()) {
                return FontWeightProp.fromProto(mImpl.getWeight());
            } else {
                return null;
            }
        }

        /**
         * Gets the text letter-spacing. Positive numbers increase the space between letters while
         * negative numbers tighten the space. If not specified, defaults to 0. Intended for testing
         * purposes only.
         */
        @Nullable
        public DimensionBuilders.EmProp getLetterSpacing() {
            if (mImpl.hasLetterSpacing()) {
                return DimensionBuilders.EmProp.fromProto(mImpl.getLetterSpacing());
            } else {
                return null;
            }
        }

        /**
         * Gets the variant of a font. Some renderers may use different fonts for title and body
         * text, which can be selected using this field. If not specified, defaults to "body".
         * Intended for testing purposes only.
         */
        @TilesExperimental
        @Nullable
        public FontVariantProp getVariant() {
            if (mImpl.hasVariant()) {
                return FontVariantProp.fromProto(mImpl.getVariant());
            } else {
                return null;
            }
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static FontStyle fromProto(@NonNull LayoutElementProto.FontStyle proto) {
            return new FontStyle(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public LayoutElementProto.FontStyle toProto() {
            return mImpl;
        }

        /** Builder for {@link FontStyle} */
        public static final class Builder {
            private final LayoutElementProto.FontStyle.Builder mImpl =
                    LayoutElementProto.FontStyle.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(181264306);

            public Builder() {}

            /**
             * Sets the size of the font, in scaled pixels (sp). If not specified, defaults to the
             * size of the system's "body" font.
             */
            @NonNull
            public Builder setSize(@NonNull DimensionBuilders.SpProp size) {
                mImpl.clearSize();
                mImpl.addSize(size.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(size.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets whether the text should be rendered in a italic typeface. If not specified,
             * defaults to "false".
             */
            @NonNull
            public Builder setItalic(@NonNull TypeBuilders.BoolProp italic) {
                mImpl.setItalic(italic.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(italic.getFingerprint()).aggregateValueAsInt());
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
                mFingerprint.recordPropertyUpdate(2, Boolean.hashCode(italic));
                return this;
            }

            /**
             * Sets whether the text should be rendered with an underline. If not specified,
             * defaults to "false".
             */
            @NonNull
            public Builder setUnderline(@NonNull TypeBuilders.BoolProp underline) {
                mImpl.setUnderline(underline.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(underline.getFingerprint()).aggregateValueAsInt());
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
                mFingerprint.recordPropertyUpdate(3, Boolean.hashCode(underline));
                return this;
            }

            /** Sets the text color. If not defined, defaults to white. */
            @NonNull
            public Builder setColor(@NonNull ColorBuilders.ColorProp color) {
                mImpl.setColor(color.toProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(color.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the weight of the font. If the provided value is not supported on a platform,
             * the nearest supported value will be used. If not defined, or when set to an invalid
             * value, defaults to "normal".
             */
            @NonNull
            public Builder setWeight(@NonNull FontWeightProp weight) {
                mImpl.setWeight(weight.toProto());
                mFingerprint.recordPropertyUpdate(
                        5, checkNotNull(weight.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the weight of the font. If the provided value is not supported on a platform,
             * the nearest supported value will be used. If not defined, or when set to an invalid
             * value, defaults to "normal".
             */
            @NonNull
            public Builder setWeight(@FontWeight int weight) {
                mImpl.setWeight(
                        LayoutElementProto.FontWeightProp.newBuilder()
                                .setValue(LayoutElementProto.FontWeight.forNumber(weight)));
                mFingerprint.recordPropertyUpdate(5, weight);
                return this;
            }

            /**
             * Sets the text letter-spacing. Positive numbers increase the space between letters
             * while negative numbers tighten the space. If not specified, defaults to 0.
             */
            @NonNull
            public Builder setLetterSpacing(@NonNull DimensionBuilders.EmProp letterSpacing) {
                mImpl.setLetterSpacing(letterSpacing.toProto());
                mFingerprint.recordPropertyUpdate(
                        6, checkNotNull(letterSpacing.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the variant of a font. Some renderers may use different fonts for title and body
             * text, which can be selected using this field. If not specified, defaults to "body".
             */
            @TilesExperimental
            @NonNull
            public Builder setVariant(@NonNull FontVariantProp variant) {
                mImpl.setVariant(variant.toProto());
                mFingerprint.recordPropertyUpdate(
                        7, checkNotNull(variant.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the variant of a font. Some renderers may use different fonts for title and body
             * text, which can be selected using this field. If not specified, defaults to "body".
             */
            @TilesExperimental
            @NonNull
            public Builder setVariant(@FontVariant int variant) {
                mImpl.setVariant(
                        LayoutElementProto.FontVariantProp.newBuilder()
                                .setValue(LayoutElementProto.FontVariant.forNumber(variant)));
                mFingerprint.recordPropertyUpdate(7, variant);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public FontStyle build() {
                return new FontStyle(mImpl.build(), mFingerprint);
            }
        }
    }

    /** An extensible {@code TextAlignment} property. */
    public static final class TextAlignmentProp {
        private final AlignmentProto.TextAlignmentProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        TextAlignmentProp(
                AlignmentProto.TextAlignmentProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. Intended for testing purposes only. */
        @TextAlignment
        public int getValue() {
            return mImpl.getValue().getNumber();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static TextAlignmentProp fromProto(@NonNull AlignmentProto.TextAlignmentProp proto) {
            return new TextAlignmentProp(proto, null);
        }

        @NonNull
        AlignmentProto.TextAlignmentProp toProto() {
            return mImpl;
        }

        /** Builder for {@link TextAlignmentProp} */
        public static final class Builder {
            private final AlignmentProto.TextAlignmentProp.Builder mImpl =
                    AlignmentProto.TextAlignmentProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(797507251);

            public Builder() {}

            /** Sets the value. */
            @NonNull
            public Builder setValue(@TextAlignment int value) {
                mImpl.setValue(AlignmentProto.TextAlignment.forNumber(value));
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public TextAlignmentProp build() {
                return new TextAlignmentProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** An extensible {@code TextOverflow} property. */
    public static final class TextOverflowProp {
        private final LayoutElementProto.TextOverflowProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        TextOverflowProp(
                LayoutElementProto.TextOverflowProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. Intended for testing purposes only. */
        @TextOverflow
        public int getValue() {
            return mImpl.getValue().getNumber();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static TextOverflowProp fromProto(@NonNull LayoutElementProto.TextOverflowProp proto) {
            return new TextOverflowProp(proto, null);
        }

        @NonNull
        LayoutElementProto.TextOverflowProp toProto() {
            return mImpl;
        }

        /** Builder for {@link TextOverflowProp} */
        public static final class Builder {
            private final LayoutElementProto.TextOverflowProp.Builder mImpl =
                    LayoutElementProto.TextOverflowProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1183432233);

            public Builder() {}

            /** Sets the value. */
            @NonNull
            public Builder setValue(@TextOverflow int value) {
                mImpl.setValue(LayoutElementProto.TextOverflow.forNumber(value));
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public TextOverflowProp build() {
                return new TextOverflowProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** An extensible {@code ArcAnchorType} property. */
    public static final class ArcAnchorTypeProp {
        private final AlignmentProto.ArcAnchorTypeProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ArcAnchorTypeProp(
                AlignmentProto.ArcAnchorTypeProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. Intended for testing purposes only. */
        @ArcAnchorType
        public int getValue() {
            return mImpl.getValue().getNumber();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static ArcAnchorTypeProp fromProto(@NonNull AlignmentProto.ArcAnchorTypeProp proto) {
            return new ArcAnchorTypeProp(proto, null);
        }

        @NonNull
        AlignmentProto.ArcAnchorTypeProp toProto() {
            return mImpl;
        }

        /** Builder for {@link ArcAnchorTypeProp} */
        public static final class Builder {
            private final AlignmentProto.ArcAnchorTypeProp.Builder mImpl =
                    AlignmentProto.ArcAnchorTypeProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1193249074);

            public Builder() {}

            /** Sets the value. */
            @NonNull
            public Builder setValue(@ArcAnchorType int value) {
                mImpl.setValue(AlignmentProto.ArcAnchorType.forNumber(value));
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public ArcAnchorTypeProp build() {
                return new ArcAnchorTypeProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A text string. */
    public static final class Text implements LayoutElement {
        private final LayoutElementProto.Text mImpl;
        @Nullable private final Fingerprint mFingerprint;

        Text(LayoutElementProto.Text impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the text to render. Intended for testing purposes only. */
        @Nullable
        public TypeBuilders.StringProp getText() {
            if (mImpl.hasText()) {
                return TypeBuilders.StringProp.fromProto(mImpl.getText());
            } else {
                return null;
            }
        }

        /**
         * Gets the style of font to use (size, bold etc). If not specified, defaults to the
         * platform's default body font. Intended for testing purposes only.
         */
        @Nullable
        public FontStyle getFontStyle() {
            if (mImpl.hasFontStyle()) {
                return FontStyle.fromProto(mImpl.getFontStyle());
            } else {
                return null;
            }
        }

        /**
         * Gets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. Intended
         * for testing purposes only.
         */
        @Nullable
        public ModifiersBuilders.Modifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return ModifiersBuilders.Modifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        /**
         * Gets the maximum number of lines that can be represented by the {@link Text} element. If
         * not defined, the {@link Text} element will be treated as a single-line element. Intended
         * for testing purposes only.
         */
        @Nullable
        public TypeBuilders.Int32Prop getMaxLines() {
            if (mImpl.hasMaxLines()) {
                return TypeBuilders.Int32Prop.fromProto(mImpl.getMaxLines());
            } else {
                return null;
            }
        }

        /**
         * Gets alignment of the text within its bounds. Note that a {@link Text} element will size
         * itself to wrap its contents, so this option is meaningless for single-line text (for
         * that, use alignment of the outer container). For multi-line text, however, this will set
         * the alignment of lines relative to the {@link Text} element bounds. If not defined,
         * defaults to TEXT_ALIGN_CENTER. Intended for testing purposes only.
         */
        @Nullable
        public TextAlignmentProp getMultilineAlignment() {
            if (mImpl.hasMultilineAlignment()) {
                return TextAlignmentProp.fromProto(mImpl.getMultilineAlignment());
            } else {
                return null;
            }
        }

        /**
         * Gets how to handle text which overflows the bound of the {@link Text} element. A {@link
         * Text} element will grow as large as possible inside its parent container (while still
         * respecting max_lines); if it cannot grow large enough to render all of its text, the text
         * which cannot fit inside its container will be truncated. If not defined, defaults to
         * TEXT_OVERFLOW_TRUNCATE. Intended for testing purposes only.
         */
        @Nullable
        public TextOverflowProp getOverflow() {
            if (mImpl.hasOverflow()) {
                return TextOverflowProp.fromProto(mImpl.getOverflow());
            } else {
                return null;
            }
        }

        /**
         * Gets the explicit height between lines of text. This is equivalent to the vertical
         * distance between subsequent baselines. If not specified, defaults the font's recommended
         * interline spacing. Intended for testing purposes only.
         */
        @Nullable
        public DimensionBuilders.SpProp getLineHeight() {
            if (mImpl.hasLineHeight()) {
                return DimensionBuilders.SpProp.fromProto(mImpl.getLineHeight());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static Text fromProto(
                @NonNull LayoutElementProto.Text proto, @Nullable Fingerprint fingerprint) {
            return new Text(proto, fingerprint);
        }

        @NonNull
        LayoutElementProto.Text toProto() {
            return mImpl;
        }

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
            private final Fingerprint mFingerprint = new Fingerprint(1976530157);

            public Builder() {}

            /** Sets the text to render. */
            @NonNull
            public Builder setText(@NonNull TypeBuilders.StringProp text) {
                mImpl.setText(text.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(text.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the text to render. */
            @NonNull
            public Builder setText(@NonNull String text) {
                mImpl.mergeText(TypesProto.StringProp.newBuilder().setValue(text).build());
                mFingerprint.recordPropertyUpdate(1, text.hashCode());
                return this;
            }

            /**
             * Sets the style of font to use (size, bold etc). If not specified, defaults to the
             * platform's default body font.
             */
            @NonNull
            public Builder setFontStyle(@NonNull FontStyle fontStyle) {
                mImpl.setFontStyle(fontStyle.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(fontStyle.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. */
            @NonNull
            public Builder setModifiers(@NonNull ModifiersBuilders.Modifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the maximum number of lines that can be represented by the {@link Text} element.
             * If not defined, the {@link Text} element will be treated as a single-line element.
             */
            @NonNull
            public Builder setMaxLines(@NonNull TypeBuilders.Int32Prop maxLines) {
                mImpl.setMaxLines(maxLines.toProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(maxLines.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the maximum number of lines that can be represented by the {@link Text} element.
             * If not defined, the {@link Text} element will be treated as a single-line element.
             */
            @NonNull
            public Builder setMaxLines(@IntRange(from = 1) int maxLines) {
                mImpl.setMaxLines(TypesProto.Int32Prop.newBuilder().setValue(maxLines));
                mFingerprint.recordPropertyUpdate(4, maxLines);
                return this;
            }

            /**
             * Sets alignment of the text within its bounds. Note that a {@link Text} element will
             * size itself to wrap its contents, so this option is meaningless for single-line text
             * (for that, use alignment of the outer container). For multi-line text, however, this
             * will set the alignment of lines relative to the {@link Text} element bounds. If not
             * defined, defaults to TEXT_ALIGN_CENTER.
             */
            @NonNull
            public Builder setMultilineAlignment(@NonNull TextAlignmentProp multilineAlignment) {
                mImpl.setMultilineAlignment(multilineAlignment.toProto());
                mFingerprint.recordPropertyUpdate(
                        5, checkNotNull(multilineAlignment.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets alignment of the text within its bounds. Note that a {@link Text} element will
             * size itself to wrap its contents, so this option is meaningless for single-line text
             * (for that, use alignment of the outer container). For multi-line text, however, this
             * will set the alignment of lines relative to the {@link Text} element bounds. If not
             * defined, defaults to TEXT_ALIGN_CENTER.
             */
            @NonNull
            public Builder setMultilineAlignment(@TextAlignment int multilineAlignment) {
                mImpl.setMultilineAlignment(
                        AlignmentProto.TextAlignmentProp.newBuilder()
                                .setValue(
                                        AlignmentProto.TextAlignment.forNumber(
                                                multilineAlignment)));
                mFingerprint.recordPropertyUpdate(5, multilineAlignment);
                return this;
            }

            /**
             * Sets how to handle text which overflows the bound of the {@link Text} element. A
             * {@link Text} element will grow as large as possible inside its parent container
             * (while still respecting max_lines); if it cannot grow large enough to render all of
             * its text, the text which cannot fit inside its container will be truncated. If not
             * defined, defaults to TEXT_OVERFLOW_TRUNCATE.
             */
            @NonNull
            public Builder setOverflow(@NonNull TextOverflowProp overflow) {
                mImpl.setOverflow(overflow.toProto());
                mFingerprint.recordPropertyUpdate(
                        6, checkNotNull(overflow.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets how to handle text which overflows the bound of the {@link Text} element. A
             * {@link Text} element will grow as large as possible inside its parent container
             * (while still respecting max_lines); if it cannot grow large enough to render all of
             * its text, the text which cannot fit inside its container will be truncated. If not
             * defined, defaults to TEXT_OVERFLOW_TRUNCATE.
             */
            @NonNull
            public Builder setOverflow(@TextOverflow int overflow) {
                mImpl.setOverflow(
                        LayoutElementProto.TextOverflowProp.newBuilder()
                                .setValue(LayoutElementProto.TextOverflow.forNumber(overflow)));
                mFingerprint.recordPropertyUpdate(6, overflow);
                return this;
            }

            /**
             * Sets the explicit height between lines of text. This is equivalent to the vertical
             * distance between subsequent baselines. If not specified, defaults the font's
             * recommended interline spacing.
             */
            @NonNull
            public Builder setLineHeight(@NonNull DimensionBuilders.SpProp lineHeight) {
                mImpl.setLineHeight(lineHeight.toProto());
                mFingerprint.recordPropertyUpdate(
                        7, checkNotNull(lineHeight.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public Text build() {
                return new Text(mImpl.build(), mFingerprint);
            }
        }
    }

    /** An extensible {@code ContentScaleMode} property. */
    public static final class ContentScaleModeProp {
        private final LayoutElementProto.ContentScaleModeProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ContentScaleModeProp(
                LayoutElementProto.ContentScaleModeProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. Intended for testing purposes only. */
        @ContentScaleMode
        public int getValue() {
            return mImpl.getValue().getNumber();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static ContentScaleModeProp fromProto(
                @NonNull LayoutElementProto.ContentScaleModeProp proto) {
            return new ContentScaleModeProp(proto, null);
        }

        @NonNull
        LayoutElementProto.ContentScaleModeProp toProto() {
            return mImpl;
        }

        /** Builder for {@link ContentScaleModeProp} */
        public static final class Builder {
            private final LayoutElementProto.ContentScaleModeProp.Builder mImpl =
                    LayoutElementProto.ContentScaleModeProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-893830536);

            public Builder() {}

            /** Sets the value. */
            @NonNull
            public Builder setValue(@ContentScaleMode int value) {
                mImpl.setValue(LayoutElementProto.ContentScaleMode.forNumber(value));
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public ContentScaleModeProp build() {
                return new ContentScaleModeProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** Filtering parameters used for images. This can be used to apply a color tint to images. */
    public static final class ColorFilter {
        private final LayoutElementProto.ColorFilter mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ColorFilter(LayoutElementProto.ColorFilter impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the tint color to use. If specified, the image will be tinted, using SRC_IN blending
         * (that is, all color information will be stripped from the target image, and only the
         * alpha channel will be blended with the requested color).
         *
         * <p>Note that only Android image resources can be tinted; Inline images will not be
         * tinted, and this property will have no effect. Intended for testing purposes only.
         */
        @Nullable
        public ColorBuilders.ColorProp getTint() {
            if (mImpl.hasTint()) {
                return ColorBuilders.ColorProp.fromProto(mImpl.getTint());
            } else {
                return null;
            }
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static ColorFilter fromProto(@NonNull LayoutElementProto.ColorFilter proto) {
            return new ColorFilter(proto, null);
        }

        @NonNull
        LayoutElementProto.ColorFilter toProto() {
            return mImpl;
        }

        /** Builder for {@link ColorFilter} */
        public static final class Builder {
            private final LayoutElementProto.ColorFilter.Builder mImpl =
                    LayoutElementProto.ColorFilter.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(181311326);

            public Builder() {}

            /**
             * Sets the tint color to use. If specified, the image will be tinted, using SRC_IN
             * blending (that is, all color information will be stripped from the target image, and
             * only the alpha channel will be blended with the requested color).
             *
             * <p>Note that only Android image resources can be tinted; Inline images will not be
             * tinted, and this property will have no effect.
             */
            @NonNull
            public LayoutElementBuilders.ColorFilter.Builder setTint(
                    @NonNull ColorBuilders.ColorProp tint) {
                mImpl.setTint(tint.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(tint.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public ColorFilter build() {
                return new ColorFilter(mImpl.build(), mFingerprint);
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
        @Nullable private final Fingerprint mFingerprint;

        Image(LayoutElementProto.Image impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the resource_id of the image to render. This must exist in the supplied resource
         * bundle. Intended for testing purposes only.
         */
        @Nullable
        public TypeBuilders.StringProp getResourceId() {
            if (mImpl.hasResourceId()) {
                return TypeBuilders.StringProp.fromProto(mImpl.getResourceId());
            } else {
                return null;
            }
        }

        /**
         * Gets the width of this image. If not defined, the image will not be rendered. Intended
         * for testing purposes only.
         */
        @Nullable
        public DimensionBuilders.ImageDimension getWidth() {
            if (mImpl.hasWidth()) {
                return DimensionBuilders.imageDimensionFromProto(mImpl.getWidth());
            } else {
                return null;
            }
        }

        /**
         * Gets the height of this image. If not defined, the image will not be rendered. Intended
         * for testing purposes only.
         */
        @Nullable
        public DimensionBuilders.ImageDimension getHeight() {
            if (mImpl.hasHeight()) {
                return DimensionBuilders.imageDimensionFromProto(mImpl.getHeight());
            } else {
                return null;
            }
        }

        /**
         * Gets how to scale the image resource inside the bounds specified by width/height if its
         * size does not match those bounds. Defaults to CONTENT_SCALE_MODE_FIT. Intended for
         * testing purposes only.
         */
        @Nullable
        public ContentScaleModeProp getContentScaleMode() {
            if (mImpl.hasContentScaleMode()) {
                return ContentScaleModeProp.fromProto(mImpl.getContentScaleMode());
            } else {
                return null;
            }
        }

        /**
         * Gets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. Intended
         * for testing purposes only.
         */
        @Nullable
        public ModifiersBuilders.Modifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return ModifiersBuilders.Modifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        /** Gets filtering parameters for this image. If not specified, defaults to no filtering. */
        @Nullable
        public ColorFilter getColorFilter() {
            if (mImpl.hasColorFilter()) {
                return ColorFilter.fromProto(mImpl.getColorFilter());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static Image fromProto(
                @NonNull LayoutElementProto.Image proto, @Nullable Fingerprint fingerprint) {
            return new Image(proto, fingerprint);
        }

        @NonNull
        LayoutElementProto.Image toProto() {
            return mImpl;
        }

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
            private final Fingerprint mFingerprint = new Fingerprint(-543078544);

            public Builder() {}

            /**
             * Sets the resource_id of the image to render. This must exist in the supplied resource
             * bundle.
             */
            @NonNull
            public Builder setResourceId(@NonNull TypeBuilders.StringProp resourceId) {
                mImpl.setResourceId(resourceId.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(resourceId.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the resource_id of the image to render. This must exist in the supplied resource
             * bundle.
             */
            @NonNull
            public Builder setResourceId(@NonNull String resourceId) {
                mImpl.setResourceId(TypesProto.StringProp.newBuilder().setValue(resourceId));
                mFingerprint.recordPropertyUpdate(1, resourceId.hashCode());
                return this;
            }

            /** Sets the width of this image. If not defined, the image will not be rendered. */
            @NonNull
            public Builder setWidth(@NonNull DimensionBuilders.ImageDimension width) {
                mImpl.setWidth(width.toImageDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(width.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the height of this image. If not defined, the image will not be rendered. */
            @NonNull
            public Builder setHeight(@NonNull DimensionBuilders.ImageDimension height) {
                mImpl.setHeight(height.toImageDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(height.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets how to scale the image resource inside the bounds specified by width/height if
             * its size does not match those bounds. Defaults to CONTENT_SCALE_MODE_FIT.
             */
            @NonNull
            public Builder setContentScaleMode(@NonNull ContentScaleModeProp contentScaleMode) {
                mImpl.setContentScaleMode(contentScaleMode.toProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(contentScaleMode.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets how to scale the image resource inside the bounds specified by width/height if
             * its size does not match those bounds. Defaults to CONTENT_SCALE_MODE_FIT.
             */
            @NonNull
            public Builder setContentScaleMode(@ContentScaleMode int contentScaleMode) {
                mImpl.setContentScaleMode(
                        LayoutElementProto.ContentScaleModeProp.newBuilder()
                                .setValue(
                                        LayoutElementProto.ContentScaleMode.forNumber(
                                                contentScaleMode)));
                mFingerprint.recordPropertyUpdate(4, contentScaleMode);
                return this;
            }

            /** Sets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. */
            @NonNull
            public Builder setModifiers(@NonNull ModifiersBuilders.Modifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        5, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets filtering parameters for this image. If not specified, defaults to no filtering.
             */
            @NonNull
            public Builder setColorFilter(@NonNull ColorFilter colorFilter) {
                mImpl.setColorFilter(colorFilter.toProto());
                mFingerprint.recordPropertyUpdate(
                        6, checkNotNull(colorFilter.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public Image build() {
                return new Image(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A simple spacer, typically used to provide padding between adjacent elements. */
    public static final class Spacer implements LayoutElement {
        private final LayoutElementProto.Spacer mImpl;
        @Nullable private final Fingerprint mFingerprint;

        Spacer(LayoutElementProto.Spacer impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the width of this {@link Spacer}. When this is added as the direct child of an
         * {@link Arc}, this must be specified as an angular dimension, otherwise a linear dimension
         * must be used. If not defined, defaults to 0. Intended for testing purposes only.
         */
        @Nullable
        public DimensionBuilders.SpacerDimension getWidth() {
            if (mImpl.hasWidth()) {
                return DimensionBuilders.spacerDimensionFromProto(mImpl.getWidth());
            } else {
                return null;
            }
        }

        /**
         * Gets the height of this spacer. If not defined, defaults to 0. Intended for testing
         * purposes only.
         */
        @Nullable
        public DimensionBuilders.SpacerDimension getHeight() {
            if (mImpl.hasHeight()) {
                return DimensionBuilders.spacerDimensionFromProto(mImpl.getHeight());
            } else {
                return null;
            }
        }

        /**
         * Gets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. Intended
         * for testing purposes only.
         */
        @Nullable
        public ModifiersBuilders.Modifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return ModifiersBuilders.Modifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static Spacer fromProto(
                @NonNull LayoutElementProto.Spacer proto, @Nullable Fingerprint fingerprint) {
            return new Spacer(proto, fingerprint);
        }

        @NonNull
        LayoutElementProto.Spacer toProto() {
            return mImpl;
        }

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
            private final Fingerprint mFingerprint = new Fingerprint(-1748084575);

            public Builder() {}

            /**
             * Sets the width of this {@link Spacer}. When this is added as the direct child of an
             * {@link Arc}, this must be specified as an angular dimension, otherwise a linear
             * dimension must be used. If not defined, defaults to 0.
             */
            @NonNull
            public Builder setWidth(@NonNull DimensionBuilders.SpacerDimension width) {
                mImpl.setWidth(width.toSpacerDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(width.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the height of this spacer. If not defined, defaults to 0. */
            @NonNull
            public Builder setHeight(@NonNull DimensionBuilders.SpacerDimension height) {
                mImpl.setHeight(height.toSpacerDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(height.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. */
            @NonNull
            public Builder setModifiers(@NonNull ModifiersBuilders.Modifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public Spacer build() {
                return new Spacer(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A container which stacks all of its children on top of one another. This also allows to add a
     * background color, or to have a border around them with some padding.
     */
    public static final class Box implements LayoutElement {
        private final LayoutElementProto.Box mImpl;
        @Nullable private final Fingerprint mFingerprint;

        Box(LayoutElementProto.Box impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the child element(s) to wrap. Intended for testing purposes only. */
        @NonNull
        public List<LayoutElement> getContents() {
            return Collections.unmodifiableList(
                    mImpl.getContentsList().stream()
                            .map(LayoutElementBuilders::layoutElementFromProto)
                            .collect(toList()));
        }

        /**
         * Gets the height of this {@link Box}. If not defined, this will size itself to fit all of
         * its children (i.e. a WrappedDimension). Intended for testing purposes only.
         */
        @Nullable
        public DimensionBuilders.ContainerDimension getHeight() {
            if (mImpl.hasHeight()) {
                return DimensionBuilders.containerDimensionFromProto(mImpl.getHeight());
            } else {
                return null;
            }
        }

        /**
         * Gets the width of this {@link Box}. If not defined, this will size itself to fit all of
         * its children (i.e. a WrappedDimension). Intended for testing purposes only.
         */
        @Nullable
        public DimensionBuilders.ContainerDimension getWidth() {
            if (mImpl.hasWidth()) {
                return DimensionBuilders.containerDimensionFromProto(mImpl.getWidth());
            } else {
                return null;
            }
        }

        /**
         * Gets the horizontal alignment of the element inside this {@link Box}. If not defined,
         * defaults to HORIZONTAL_ALIGN_CENTER. Intended for testing purposes only.
         */
        @Nullable
        public HorizontalAlignmentProp getHorizontalAlignment() {
            if (mImpl.hasHorizontalAlignment()) {
                return HorizontalAlignmentProp.fromProto(mImpl.getHorizontalAlignment());
            } else {
                return null;
            }
        }

        /**
         * Gets the vertical alignment of the element inside this {@link Box}. If not defined,
         * defaults to VERTICAL_ALIGN_CENTER. Intended for testing purposes only.
         */
        @Nullable
        public VerticalAlignmentProp getVerticalAlignment() {
            if (mImpl.hasVerticalAlignment()) {
                return VerticalAlignmentProp.fromProto(mImpl.getVerticalAlignment());
            } else {
                return null;
            }
        }

        /**
         * Gets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. Intended
         * for testing purposes only.
         */
        @Nullable
        public ModifiersBuilders.Modifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return ModifiersBuilders.Modifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static Box fromProto(
                @NonNull LayoutElementProto.Box proto, @Nullable Fingerprint fingerprint) {
            return new Box(proto, fingerprint);
        }

        @NonNull
        LayoutElementProto.Box toProto() {
            return mImpl;
        }

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
            private final Fingerprint mFingerprint = new Fingerprint(-1881256071);

            public Builder() {}

            /** Adds one item to the child element(s) to wrap. */
            @NonNull
            public Builder addContent(@NonNull LayoutElement content) {
                mImpl.addContents(content.toLayoutElementProto());
                mFingerprint.addChildNode(checkNotNull(content.getFingerprint()));
                return this;
            }

            /**
             * Sets the height of this {@link Box}. If not defined, this will size itself to fit all
             * of its children (i.e. a WrappedDimension).
             */
            @NonNull
            public Builder setHeight(@NonNull DimensionBuilders.ContainerDimension height) {
                mImpl.setHeight(height.toContainerDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(height.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the width of this {@link Box}. If not defined, this will size itself to fit all
             * of its children (i.e. a WrappedDimension).
             */
            @NonNull
            public Builder setWidth(@NonNull DimensionBuilders.ContainerDimension width) {
                mImpl.setWidth(width.toContainerDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(width.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the horizontal alignment of the element inside this {@link Box}. If not defined,
             * defaults to HORIZONTAL_ALIGN_CENTER.
             */
            @NonNull
            public Builder setHorizontalAlignment(
                    @NonNull HorizontalAlignmentProp horizontalAlignment) {
                mImpl.setHorizontalAlignment(horizontalAlignment.toProto());
                mFingerprint.recordPropertyUpdate(
                        4,
                        checkNotNull(horizontalAlignment.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the horizontal alignment of the element inside this {@link Box}. If not defined,
             * defaults to HORIZONTAL_ALIGN_CENTER.
             */
            @NonNull
            public Builder setHorizontalAlignment(@HorizontalAlignment int horizontalAlignment) {
                mImpl.setHorizontalAlignment(
                        AlignmentProto.HorizontalAlignmentProp.newBuilder()
                                .setValue(
                                        AlignmentProto.HorizontalAlignment.forNumber(
                                                horizontalAlignment)));
                mFingerprint.recordPropertyUpdate(4, horizontalAlignment);
                return this;
            }

            /**
             * Sets the vertical alignment of the element inside this {@link Box}. If not defined,
             * defaults to VERTICAL_ALIGN_CENTER.
             */
            @NonNull
            public Builder setVerticalAlignment(@NonNull VerticalAlignmentProp verticalAlignment) {
                mImpl.setVerticalAlignment(verticalAlignment.toProto());
                mFingerprint.recordPropertyUpdate(
                        5, checkNotNull(verticalAlignment.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the vertical alignment of the element inside this {@link Box}. If not defined,
             * defaults to VERTICAL_ALIGN_CENTER.
             */
            @NonNull
            public Builder setVerticalAlignment(@VerticalAlignment int verticalAlignment) {
                mImpl.setVerticalAlignment(
                        AlignmentProto.VerticalAlignmentProp.newBuilder()
                                .setValue(
                                        AlignmentProto.VerticalAlignment.forNumber(
                                                verticalAlignment)));
                mFingerprint.recordPropertyUpdate(5, verticalAlignment);
                return this;
            }

            /** Sets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. */
            @NonNull
            public Builder setModifiers(@NonNull ModifiersBuilders.Modifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        6, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public Box build() {
                return new Box(mImpl.build(), mFingerprint);
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
        @Nullable private final Fingerprint mFingerprint;

        SpanText(LayoutElementProto.SpanText impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the text to render. Intended for testing purposes only. */
        @Nullable
        public TypeBuilders.StringProp getText() {
            if (mImpl.hasText()) {
                return TypeBuilders.StringProp.fromProto(mImpl.getText());
            } else {
                return null;
            }
        }

        /**
         * Gets the style of font to use (size, bold etc). If not specified, defaults to the
         * platform's default body font. Intended for testing purposes only.
         */
        @Nullable
        public FontStyle getFontStyle() {
            if (mImpl.hasFontStyle()) {
                return FontStyle.fromProto(mImpl.getFontStyle());
            } else {
                return null;
            }
        }

        /**
         * Gets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. Intended
         * for testing purposes only.
         */
        @Nullable
        public ModifiersBuilders.SpanModifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return ModifiersBuilders.SpanModifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static SpanText fromProto(@NonNull LayoutElementProto.SpanText proto) {
            return new SpanText(proto, null);
        }

        @NonNull
        LayoutElementProto.SpanText toProto() {
            return mImpl;
        }

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
            private final Fingerprint mFingerprint = new Fingerprint(-221774557);

            public Builder() {}

            /** Sets the text to render. */
            @NonNull
            public Builder setText(@NonNull TypeBuilders.StringProp text) {
                mImpl.setText(text.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(text.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the text to render. */
            @NonNull
            public Builder setText(@NonNull String text) {
                mImpl.setText(TypesProto.StringProp.newBuilder().setValue(text));
                mFingerprint.recordPropertyUpdate(1, text.hashCode());
                return this;
            }

            /**
             * Sets the style of font to use (size, bold etc). If not specified, defaults to the
             * platform's default body font.
             */
            @NonNull
            public Builder setFontStyle(@NonNull FontStyle fontStyle) {
                mImpl.setFontStyle(fontStyle.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(fontStyle.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. */
            @NonNull
            public Builder setModifiers(@NonNull ModifiersBuilders.SpanModifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public SpanText build() {
                return new SpanText(mImpl.build(), mFingerprint);
            }
        }
    }

    /** An image which can be added to a {@link Span}. */
    public static final class SpanImage implements Span {
        private final LayoutElementProto.SpanImage mImpl;
        @Nullable private final Fingerprint mFingerprint;

        SpanImage(LayoutElementProto.SpanImage impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the resource_id of the image to render. This must exist in the supplied resource
         * bundle. Intended for testing purposes only.
         */
        @Nullable
        public TypeBuilders.StringProp getResourceId() {
            if (mImpl.hasResourceId()) {
                return TypeBuilders.StringProp.fromProto(mImpl.getResourceId());
            } else {
                return null;
            }
        }

        /**
         * Gets the width of this image. If not defined, the image will not be rendered. Intended
         * for testing purposes only.
         */
        @Nullable
        public DimensionBuilders.DpProp getWidth() {
            if (mImpl.hasWidth()) {
                return DimensionBuilders.DpProp.fromProto(mImpl.getWidth());
            } else {
                return null;
            }
        }

        /**
         * Gets the height of this image. If not defined, the image will not be rendered. Intended
         * for testing purposes only.
         */
        @Nullable
        public DimensionBuilders.DpProp getHeight() {
            if (mImpl.hasHeight()) {
                return DimensionBuilders.DpProp.fromProto(mImpl.getHeight());
            } else {
                return null;
            }
        }

        /**
         * Gets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. Intended
         * for testing purposes only.
         */
        @Nullable
        public ModifiersBuilders.SpanModifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return ModifiersBuilders.SpanModifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        /**
         * Gets alignment of this image within the line height of the surrounding {@link Spannable}.
         * If undefined, defaults to SPAN_VERTICAL_ALIGN_BOTTOM. Intended for testing purposes only.
         */
        @Nullable
        public SpanVerticalAlignmentProp getAlignment() {
            if (mImpl.hasAlignment()) {
                return SpanVerticalAlignmentProp.fromProto(mImpl.getAlignment());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static SpanImage fromProto(@NonNull LayoutElementProto.SpanImage proto) {
            return new SpanImage(proto, null);
        }

        @NonNull
        LayoutElementProto.SpanImage toProto() {
            return mImpl;
        }

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
            private final Fingerprint mFingerprint = new Fingerprint(502289772);

            public Builder() {}

            /**
             * Sets the resource_id of the image to render. This must exist in the supplied resource
             * bundle.
             */
            @NonNull
            public Builder setResourceId(@NonNull TypeBuilders.StringProp resourceId) {
                mImpl.setResourceId(resourceId.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(resourceId.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the resource_id of the image to render. This must exist in the supplied resource
             * bundle.
             */
            @NonNull
            public Builder setResourceId(@NonNull String resourceId) {
                mImpl.setResourceId(TypesProto.StringProp.newBuilder().setValue(resourceId));
                mFingerprint.recordPropertyUpdate(1, resourceId.hashCode());
                return this;
            }

            /** Sets the width of this image. If not defined, the image will not be rendered. */
            @NonNull
            public Builder setWidth(@NonNull DimensionBuilders.DpProp width) {
                mImpl.setWidth(width.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(width.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the height of this image. If not defined, the image will not be rendered. */
            @NonNull
            public Builder setHeight(@NonNull DimensionBuilders.DpProp height) {
                mImpl.setHeight(height.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(height.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. */
            @NonNull
            public Builder setModifiers(@NonNull ModifiersBuilders.SpanModifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets alignment of this image within the line height of the surrounding {@link
             * Spannable}. If undefined, defaults to SPAN_VERTICAL_ALIGN_BOTTOM.
             */
            @NonNull
            public Builder setAlignment(@NonNull SpanVerticalAlignmentProp alignment) {
                mImpl.setAlignment(alignment.toProto());
                mFingerprint.recordPropertyUpdate(
                        5, checkNotNull(alignment.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets alignment of this image within the line height of the surrounding {@link
             * Spannable}. If undefined, defaults to SPAN_VERTICAL_ALIGN_BOTTOM.
             */
            @NonNull
            public Builder setAlignment(@SpanVerticalAlignment int alignment) {
                mImpl.setAlignment(
                        LayoutElementProto.SpanVerticalAlignmentProp.newBuilder()
                                .setValue(
                                        LayoutElementProto.SpanVerticalAlignment.forNumber(
                                                alignment)));
                mFingerprint.recordPropertyUpdate(5, alignment);
                return this;
            }

            @Override
            @NonNull
            public SpanImage build() {
                return new SpanImage(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * Interface defining a single {@link Span}. Each {@link Span} forms part of a larger {@link
     * Spannable} widget. At the moment, the only widgets which can be added to {@link Spannable}
     * containers are {@link SpanText} and {@link SpanImage} elements.
     */
    public interface Span {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        LayoutElementProto.Span toSpanProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        Fingerprint getFingerprint();

        /** Builder to create {@link Span} objects. */
        @SuppressLint("StaticFinalBuilder")
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            Span build();
        }
    }

    /**
     * Return an instance of one of this object's subtypes, from the protocol buffer representation.
     */
    @NonNull
    static Span spanFromProto(@NonNull LayoutElementProto.Span proto) {
        if (proto.hasText()) {
            return SpanText.fromProto(proto.getText());
        }
        if (proto.hasImage()) {
            return SpanImage.fromProto(proto.getImage());
        }
        throw new IllegalStateException("Proto was not a recognised instance of Span");
    }

    /**
     * A container of {@link Span} elements. Currently, this supports {@link SpanImage} and {@link
     * SpanText} elements, where each individual {@link Span} can have different styling applied to
     * it but the resulting text will flow naturally. This allows sections of a paragraph of text to
     * have different styling applied to it, for example, making one or two words bold or italic.
     */
    public static final class Spannable implements LayoutElement {
        private final LayoutElementProto.Spannable mImpl;
        @Nullable private final Fingerprint mFingerprint;

        Spannable(LayoutElementProto.Spannable impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the {@link Span} elements that form this {@link Spannable}. Intended for testing
         * purposes only.
         */
        @NonNull
        public List<Span> getSpans() {
            return Collections.unmodifiableList(
                    mImpl.getSpansList().stream()
                            .map(LayoutElementBuilders::spanFromProto)
                            .collect(toList()));
        }

        /**
         * Gets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. Intended
         * for testing purposes only.
         */
        @Nullable
        public ModifiersBuilders.Modifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return ModifiersBuilders.Modifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        /**
         * Gets the maximum number of lines that can be represented by the {@link Spannable}
         * element. If not defined, the {@link Spannable} element will be treated as a single-line
         * element. Intended for testing purposes only.
         */
        @Nullable
        public TypeBuilders.Int32Prop getMaxLines() {
            if (mImpl.hasMaxLines()) {
                return TypeBuilders.Int32Prop.fromProto(mImpl.getMaxLines());
            } else {
                return null;
            }
        }

        /**
         * Gets alignment of the {@link Spannable} content within its bounds. Note that a {@link
         * Spannable} element will size itself to wrap its contents, so this option is meaningless
         * for single-line content (for that, use alignment of the outer container). For multi-line
         * content, however, this will set the alignment of lines relative to the {@link Spannable}
         * element bounds. If not defined, defaults to TEXT_ALIGN_CENTER. Intended for testing
         * purposes only.
         */
        @Nullable
        public HorizontalAlignmentProp getMultilineAlignment() {
            if (mImpl.hasMultilineAlignment()) {
                return HorizontalAlignmentProp.fromProto(mImpl.getMultilineAlignment());
            } else {
                return null;
            }
        }

        /**
         * Gets how to handle content which overflows the bound of the {@link Spannable} element. A
         * {@link Spannable} element will grow as large as possible inside its parent container
         * (while still respecting max_lines); if it cannot grow large enough to render all of its
         * content, the content which cannot fit inside its container will be truncated. If not
         * defined, defaults to TEXT_OVERFLOW_TRUNCATE. Intended for testing purposes only.
         */
        @Nullable
        public TextOverflowProp getOverflow() {
            if (mImpl.hasOverflow()) {
                return TextOverflowProp.fromProto(mImpl.getOverflow());
            } else {
                return null;
            }
        }

        /**
         * Gets the explicit height between lines of text. This is equivalent to the vertical
         * distance between subsequent baselines. If not specified, defaults the font's recommended
         * interline spacing. Intended for testing purposes only.
         */
        @Nullable
        public DimensionBuilders.SpProp getLineHeight() {
            if (mImpl.hasLineHeight()) {
                return DimensionBuilders.SpProp.fromProto(mImpl.getLineHeight());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static Spannable fromProto(
                @NonNull LayoutElementProto.Spannable proto, @Nullable Fingerprint fingerprint) {
            return new Spannable(proto, fingerprint);
        }

        @NonNull
        LayoutElementProto.Spannable toProto() {
            return mImpl;
        }

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
            private final Fingerprint mFingerprint = new Fingerprint(-1690284372);

            public Builder() {}

            /** Adds one item to the {@link Span} elements that form this {@link Spannable}. */
            @NonNull
            public Builder addSpan(@NonNull Span span) {
                mImpl.addSpans(span.toSpanProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(span.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. */
            @NonNull
            public Builder setModifiers(@NonNull ModifiersBuilders.Modifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the maximum number of lines that can be represented by the {@link Spannable}
             * element. If not defined, the {@link Spannable} element will be treated as a
             * single-line element.
             */
            @NonNull
            public Builder setMaxLines(@NonNull TypeBuilders.Int32Prop maxLines) {
                mImpl.setMaxLines(maxLines.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(maxLines.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the maximum number of lines that can be represented by the {@link Spannable}
             * element. If not defined, the {@link Spannable} element will be treated as a
             * single-line element.
             */
            @NonNull
            public Builder setMaxLines(@IntRange(from = 1) int maxLines) {
                mImpl.setMaxLines(TypesProto.Int32Prop.newBuilder().setValue(maxLines));
                mFingerprint.recordPropertyUpdate(3, maxLines);
                return this;
            }

            /**
             * Sets alignment of the {@link Spannable} content within its bounds. Note that a {@link
             * Spannable} element will size itself to wrap its contents, so this option is
             * meaningless for single-line content (for that, use alignment of the outer container).
             * For multi-line content, however, this will set the alignment of lines relative to the
             * {@link Spannable} element bounds. If not defined, defaults to TEXT_ALIGN_CENTER.
             */
            @NonNull
            public Builder setMultilineAlignment(
                    @NonNull HorizontalAlignmentProp multilineAlignment) {
                mImpl.setMultilineAlignment(multilineAlignment.toProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(multilineAlignment.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets alignment of the {@link Spannable} content within its bounds. Note that a {@link
             * Spannable} element will size itself to wrap its contents, so this option is
             * meaningless for single-line content (for that, use alignment of the outer container).
             * For multi-line content, however, this will set the alignment of lines relative to the
             * {@link Spannable} element bounds. If not defined, defaults to TEXT_ALIGN_CENTER.
             */
            @NonNull
            public Builder setMultilineAlignment(@HorizontalAlignment int multilineAlignment) {
                mImpl.setMultilineAlignment(
                        AlignmentProto.HorizontalAlignmentProp.newBuilder()
                                .setValue(
                                        AlignmentProto.HorizontalAlignment.forNumber(
                                                multilineAlignment)));
                mFingerprint.recordPropertyUpdate(4, multilineAlignment);
                return this;
            }

            /**
             * Sets how to handle content which overflows the bound of the {@link Spannable}
             * element. A {@link Spannable} element will grow as large as possible inside its parent
             * container (while still respecting max_lines); if it cannot grow large enough to
             * render all of its content, the content which cannot fit inside its container will be
             * truncated. If not defined, defaults to TEXT_OVERFLOW_TRUNCATE.
             */
            @NonNull
            public Builder setOverflow(@NonNull TextOverflowProp overflow) {
                mImpl.setOverflow(overflow.toProto());
                mFingerprint.recordPropertyUpdate(
                        5, checkNotNull(overflow.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets how to handle content which overflows the bound of the {@link Spannable}
             * element. A {@link Spannable} element will grow as large as possible inside its parent
             * container (while still respecting max_lines); if it cannot grow large enough to
             * render all of its content, the content which cannot fit inside its container will be
             * truncated. If not defined, defaults to TEXT_OVERFLOW_TRUNCATE.
             */
            @NonNull
            public Builder setOverflow(@TextOverflow int overflow) {
                mImpl.setOverflow(
                        LayoutElementProto.TextOverflowProp.newBuilder()
                                .setValue(LayoutElementProto.TextOverflow.forNumber(overflow)));
                mFingerprint.recordPropertyUpdate(5, overflow);
                return this;
            }

            /**
             * Sets the explicit height between lines of text. This is equivalent to the vertical
             * distance between subsequent baselines. If not specified, defaults the font's
             * recommended interline spacing.
             */
            @NonNull
            public Builder setLineHeight(@NonNull DimensionBuilders.SpProp lineHeight) {
                mImpl.setLineHeight(lineHeight.toProto());
                mFingerprint.recordPropertyUpdate(
                        7, checkNotNull(lineHeight.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public Spannable build() {
                return new Spannable(mImpl.build(), mFingerprint);
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
        @Nullable private final Fingerprint mFingerprint;

        Column(LayoutElementProto.Column impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the list of child elements to place inside this {@link Column}. Intended for testing
         * purposes only.
         */
        @NonNull
        public List<LayoutElement> getContents() {
            return Collections.unmodifiableList(
                    mImpl.getContentsList().stream()
                            .map(LayoutElementBuilders::layoutElementFromProto)
                            .collect(toList()));
        }

        /**
         * Gets the horizontal alignment of elements inside this column, if they are narrower than
         * the resulting width of the column. If not defined, defaults to HORIZONTAL_ALIGN_CENTER.
         * Intended for testing purposes only.
         */
        @Nullable
        public HorizontalAlignmentProp getHorizontalAlignment() {
            if (mImpl.hasHorizontalAlignment()) {
                return HorizontalAlignmentProp.fromProto(mImpl.getHorizontalAlignment());
            } else {
                return null;
            }
        }

        /**
         * Gets the width of this column. If not defined, this will size itself to fit all of its
         * children (i.e. a WrappedDimension). Intended for testing purposes only.
         */
        @Nullable
        public DimensionBuilders.ContainerDimension getWidth() {
            if (mImpl.hasWidth()) {
                return DimensionBuilders.containerDimensionFromProto(mImpl.getWidth());
            } else {
                return null;
            }
        }

        /**
         * Gets the height of this column. If not defined, this will size itself to fit all of its
         * children (i.e. a WrappedDimension). Intended for testing purposes only.
         */
        @Nullable
        public DimensionBuilders.ContainerDimension getHeight() {
            if (mImpl.hasHeight()) {
                return DimensionBuilders.containerDimensionFromProto(mImpl.getHeight());
            } else {
                return null;
            }
        }

        /**
         * Gets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. Intended
         * for testing purposes only.
         */
        @Nullable
        public ModifiersBuilders.Modifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return ModifiersBuilders.Modifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static Column fromProto(
                @NonNull LayoutElementProto.Column proto, @Nullable Fingerprint fingerprint) {
            return new Column(proto, fingerprint);
        }

        @NonNull
        LayoutElementProto.Column toProto() {
            return mImpl;
        }

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
            private final Fingerprint mFingerprint = new Fingerprint(-1411218529);

            public Builder() {}

            /** Adds one item to the list of child elements to place inside this {@link Column}. */
            @NonNull
            public Builder addContent(@NonNull LayoutElement content) {
                mImpl.addContents(content.toLayoutElementProto());
                mFingerprint.addChildNode(checkNotNull(content.getFingerprint()));
                return this;
            }

            /**
             * Sets the horizontal alignment of elements inside this column, if they are narrower
             * than the resulting width of the column. If not defined, defaults to
             * HORIZONTAL_ALIGN_CENTER.
             */
            @NonNull
            public Builder setHorizontalAlignment(
                    @NonNull HorizontalAlignmentProp horizontalAlignment) {
                mImpl.setHorizontalAlignment(horizontalAlignment.toProto());
                mFingerprint.recordPropertyUpdate(
                        2,
                        checkNotNull(horizontalAlignment.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the horizontal alignment of elements inside this column, if they are narrower
             * than the resulting width of the column. If not defined, defaults to
             * HORIZONTAL_ALIGN_CENTER.
             */
            @NonNull
            public Builder setHorizontalAlignment(@HorizontalAlignment int horizontalAlignment) {
                mImpl.setHorizontalAlignment(
                        AlignmentProto.HorizontalAlignmentProp.newBuilder()
                                .setValue(
                                        AlignmentProto.HorizontalAlignment.forNumber(
                                                horizontalAlignment)));
                mFingerprint.recordPropertyUpdate(2, horizontalAlignment);
                return this;
            }

            /**
             * Sets the width of this column. If not defined, this will size itself to fit all of
             * its children (i.e. a WrappedDimension).
             */
            @NonNull
            public Builder setWidth(@NonNull DimensionBuilders.ContainerDimension width) {
                mImpl.setWidth(width.toContainerDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(width.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the height of this column. If not defined, this will size itself to fit all of
             * its children (i.e. a WrappedDimension).
             */
            @NonNull
            public Builder setHeight(@NonNull DimensionBuilders.ContainerDimension height) {
                mImpl.setHeight(height.toContainerDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(height.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. */
            @NonNull
            public Builder setModifiers(@NonNull ModifiersBuilders.Modifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        5, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public Column build() {
                return new Column(mImpl.build(), mFingerprint);
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
        @Nullable private final Fingerprint mFingerprint;

        Row(LayoutElementProto.Row impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the list of child elements to place inside this {@link Row}. Intended for testing
         * purposes only.
         */
        @NonNull
        public List<LayoutElement> getContents() {
            return Collections.unmodifiableList(
                    mImpl.getContentsList().stream()
                            .map(LayoutElementBuilders::layoutElementFromProto)
                            .collect(toList()));
        }

        /**
         * Gets the vertical alignment of elements inside this row, if they are narrower than the
         * resulting height of the row. If not defined, defaults to VERTICAL_ALIGN_CENTER. Intended
         * for testing purposes only.
         */
        @Nullable
        public VerticalAlignmentProp getVerticalAlignment() {
            if (mImpl.hasVerticalAlignment()) {
                return VerticalAlignmentProp.fromProto(mImpl.getVerticalAlignment());
            } else {
                return null;
            }
        }

        /**
         * Gets the width of this row. If not defined, this will size itself to fit all of its
         * children (i.e. a WrappedDimension). Intended for testing purposes only.
         */
        @Nullable
        public DimensionBuilders.ContainerDimension getWidth() {
            if (mImpl.hasWidth()) {
                return DimensionBuilders.containerDimensionFromProto(mImpl.getWidth());
            } else {
                return null;
            }
        }

        /**
         * Gets the height of this row. If not defined, this will size itself to fit all of its
         * children (i.e. a WrappedDimension). Intended for testing purposes only.
         */
        @Nullable
        public DimensionBuilders.ContainerDimension getHeight() {
            if (mImpl.hasHeight()) {
                return DimensionBuilders.containerDimensionFromProto(mImpl.getHeight());
            } else {
                return null;
            }
        }

        /**
         * Gets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. Intended
         * for testing purposes only.
         */
        @Nullable
        public ModifiersBuilders.Modifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return ModifiersBuilders.Modifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static Row fromProto(
                @NonNull LayoutElementProto.Row proto, @Nullable Fingerprint fingerprint) {
            return new Row(proto, fingerprint);
        }

        @NonNull
        LayoutElementProto.Row toProto() {
            return mImpl;
        }

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
            private final Fingerprint mFingerprint = new Fingerprint(1537205448);

            public Builder() {}

            /** Adds one item to the list of child elements to place inside this {@link Row}. */
            @NonNull
            public Builder addContent(@NonNull LayoutElement content) {
                mImpl.addContents(content.toLayoutElementProto());
                mFingerprint.addChildNode(checkNotNull(content.getFingerprint()));
                return this;
            }

            /**
             * Sets the vertical alignment of elements inside this row, if they are narrower than
             * the resulting height of the row. If not defined, defaults to VERTICAL_ALIGN_CENTER.
             */
            @NonNull
            public Builder setVerticalAlignment(@NonNull VerticalAlignmentProp verticalAlignment) {
                mImpl.setVerticalAlignment(verticalAlignment.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(verticalAlignment.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the vertical alignment of elements inside this row, if they are narrower than
             * the resulting height of the row. If not defined, defaults to VERTICAL_ALIGN_CENTER.
             */
            @NonNull
            public Builder setVerticalAlignment(@VerticalAlignment int verticalAlignment) {
                mImpl.setVerticalAlignment(
                        AlignmentProto.VerticalAlignmentProp.newBuilder()
                                .setValue(
                                        AlignmentProto.VerticalAlignment.forNumber(
                                                verticalAlignment)));
                mFingerprint.recordPropertyUpdate(2, verticalAlignment);
                return this;
            }

            /**
             * Sets the width of this row. If not defined, this will size itself to fit all of its
             * children (i.e. a WrappedDimension).
             */
            @NonNull
            public Builder setWidth(@NonNull DimensionBuilders.ContainerDimension width) {
                mImpl.setWidth(width.toContainerDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(width.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the height of this row. If not defined, this will size itself to fit all of its
             * children (i.e. a WrappedDimension).
             */
            @NonNull
            public Builder setHeight(@NonNull DimensionBuilders.ContainerDimension height) {
                mImpl.setHeight(height.toContainerDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(height.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. */
            @NonNull
            public Builder setModifiers(@NonNull ModifiersBuilders.Modifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        5, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public Row build() {
                return new Row(mImpl.build(), mFingerprint);
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
        @Nullable private final Fingerprint mFingerprint;

        Arc(LayoutElementProto.Arc impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets contents of this container. Intended for testing purposes only. */
        @NonNull
        public List<ArcLayoutElement> getContents() {
            return Collections.unmodifiableList(
                    mImpl.getContentsList().stream()
                            .map(LayoutElementBuilders::arcLayoutElementFromProto)
                            .collect(toList()));
        }

        /**
         * Gets the angle for the anchor, used with anchor_type to determine where to draw children.
         * Note that 0 degrees is the 12 o clock position on a device, and the angle sweeps
         * clockwise. If not defined, defaults to 0 degrees.
         *
         * <p>Values do not have to be clamped to the range 0-360; values less than 0 degrees will
         * sweep anti-clockwise (i.e. -90 degrees is equivalent to 270 degrees), and values >360
         * will be be placed at X mod 360 degrees. Intended for testing purposes only.
         */
        @Nullable
        public DimensionBuilders.DegreesProp getAnchorAngle() {
            if (mImpl.hasAnchorAngle()) {
                return DimensionBuilders.DegreesProp.fromProto(mImpl.getAnchorAngle());
            } else {
                return null;
            }
        }

        /**
         * Gets how to align the contents of this container relative to anchor_angle. If not
         * defined, defaults to ARC_ANCHOR_CENTER. Intended for testing purposes only.
         */
        @Nullable
        public ArcAnchorTypeProp getAnchorType() {
            if (mImpl.hasAnchorType()) {
                return ArcAnchorTypeProp.fromProto(mImpl.getAnchorType());
            } else {
                return null;
            }
        }

        /**
         * Gets vertical alignment of elements within the arc. If the {@link Arc}'s thickness is
         * larger than the thickness of the element being drawn, this controls whether the element
         * should be drawn towards the inner or outer edge of the arc, or drawn in the center. If
         * not defined, defaults to VERTICAL_ALIGN_CENTER. Intended for testing purposes only.
         */
        @Nullable
        public VerticalAlignmentProp getVerticalAlign() {
            if (mImpl.hasVerticalAlign()) {
                return VerticalAlignmentProp.fromProto(mImpl.getVerticalAlign());
            } else {
                return null;
            }
        }

        /**
         * Gets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. Intended
         * for testing purposes only.
         */
        @Nullable
        public ModifiersBuilders.Modifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return ModifiersBuilders.Modifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static Arc fromProto(
                @NonNull LayoutElementProto.Arc proto, @Nullable Fingerprint fingerprint) {
            return new Arc(proto, fingerprint);
        }

        @NonNull
        LayoutElementProto.Arc toProto() {
            return mImpl;
        }

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
            private final Fingerprint mFingerprint = new Fingerprint(299028337);

            public Builder() {}

            /** Adds one item to contents of this container. */
            @NonNull
            public Builder addContent(@NonNull ArcLayoutElement content) {
                mImpl.addContents(content.toArcLayoutElementProto());
                mFingerprint.addChildNode(checkNotNull(content.getFingerprint()));
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
            @NonNull
            public Builder setAnchorAngle(@NonNull DimensionBuilders.DegreesProp anchorAngle) {
                mImpl.setAnchorAngle(anchorAngle.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(anchorAngle.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets how to align the contents of this container relative to anchor_angle. If not
             * defined, defaults to ARC_ANCHOR_CENTER.
             */
            @NonNull
            public Builder setAnchorType(@NonNull ArcAnchorTypeProp anchorType) {
                mImpl.setAnchorType(anchorType.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(anchorType.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets how to align the contents of this container relative to anchor_angle. If not
             * defined, defaults to ARC_ANCHOR_CENTER.
             */
            @NonNull
            public Builder setAnchorType(@ArcAnchorType int anchorType) {
                mImpl.setAnchorType(
                        AlignmentProto.ArcAnchorTypeProp.newBuilder()
                                .setValue(AlignmentProto.ArcAnchorType.forNumber(anchorType)));
                mFingerprint.recordPropertyUpdate(3, anchorType);
                return this;
            }

            /**
             * Sets vertical alignment of elements within the arc. If the {@link Arc}'s thickness is
             * larger than the thickness of the element being drawn, this controls whether the
             * element should be drawn towards the inner or outer edge of the arc, or drawn in the
             * center. If not defined, defaults to VERTICAL_ALIGN_CENTER.
             */
            @NonNull
            public Builder setVerticalAlign(@NonNull VerticalAlignmentProp verticalAlign) {
                mImpl.setVerticalAlign(verticalAlign.toProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(verticalAlign.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets vertical alignment of elements within the arc. If the {@link Arc}'s thickness is
             * larger than the thickness of the element being drawn, this controls whether the
             * element should be drawn towards the inner or outer edge of the arc, or drawn in the
             * center. If not defined, defaults to VERTICAL_ALIGN_CENTER.
             */
            @NonNull
            public Builder setVerticalAlign(@VerticalAlignment int verticalAlign) {
                mImpl.setVerticalAlign(
                        AlignmentProto.VerticalAlignmentProp.newBuilder()
                                .setValue(
                                        AlignmentProto.VerticalAlignment.forNumber(verticalAlign)));
                mFingerprint.recordPropertyUpdate(4, verticalAlign);
                return this;
            }

            /** Sets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. */
            @NonNull
            public Builder setModifiers(@NonNull ModifiersBuilders.Modifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        5, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public Arc build() {
                return new Arc(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A text element that can be used in an {@link Arc}. */
    public static final class ArcText implements ArcLayoutElement {
        private final LayoutElementProto.ArcText mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ArcText(LayoutElementProto.ArcText impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the text to render. Intended for testing purposes only. */
        @Nullable
        public TypeBuilders.StringProp getText() {
            if (mImpl.hasText()) {
                return TypeBuilders.StringProp.fromProto(mImpl.getText());
            } else {
                return null;
            }
        }

        /**
         * Gets the style of font to use (size, bold etc). If not specified, defaults to the
         * platform's default body font. Intended for testing purposes only.
         */
        @Nullable
        public FontStyle getFontStyle() {
            if (mImpl.hasFontStyle()) {
                return FontStyle.fromProto(mImpl.getFontStyle());
            } else {
                return null;
            }
        }

        /**
         * Gets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. Intended
         * for testing purposes only.
         */
        @Nullable
        public ModifiersBuilders.ArcModifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return ModifiersBuilders.ArcModifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static ArcText fromProto(@NonNull LayoutElementProto.ArcText proto) {
            return new ArcText(proto, null);
        }

        @NonNull
        LayoutElementProto.ArcText toProto() {
            return mImpl;
        }

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
            private final Fingerprint mFingerprint = new Fingerprint(434391973);

            public Builder() {}

            /** Sets the text to render. */
            @NonNull
            public Builder setText(@NonNull TypeBuilders.StringProp text) {
                mImpl.setText(text.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(text.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the text to render. */
            @NonNull
            public Builder setText(@NonNull String text) {
                mImpl.setText(TypesProto.StringProp.newBuilder().setValue(text));
                mFingerprint.recordPropertyUpdate(1, text.hashCode());
                return this;
            }

            /**
             * Sets the style of font to use (size, bold etc). If not specified, defaults to the
             * platform's default body font.
             */
            @NonNull
            public Builder setFontStyle(@NonNull FontStyle fontStyle) {
                mImpl.setFontStyle(fontStyle.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(fontStyle.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. */
            @NonNull
            public Builder setModifiers(@NonNull ModifiersBuilders.ArcModifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public ArcText build() {
                return new ArcText(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A line that can be used in an {@link Arc} and renders as a round progress bar. */
    public static final class ArcLine implements ArcLayoutElement {
        private final LayoutElementProto.ArcLine mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ArcLine(LayoutElementProto.ArcLine impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the length of this line, in degrees. If not defined, defaults to 0. Intended for
         * testing purposes only.
         */
        @Nullable
        public DimensionBuilders.DegreesProp getLength() {
            if (mImpl.hasLength()) {
                return DimensionBuilders.DegreesProp.fromProto(mImpl.getLength());
            } else {
                return null;
            }
        }

        /**
         * Gets the thickness of this line. If not defined, defaults to 0. Intended for testing
         * purposes only.
         */
        @Nullable
        public DimensionBuilders.DpProp getThickness() {
            if (mImpl.hasThickness()) {
                return DimensionBuilders.DpProp.fromProto(mImpl.getThickness());
            } else {
                return null;
            }
        }

        /** Gets the color of this line. Intended for testing purposes only. */
        @Nullable
        public ColorBuilders.ColorProp getColor() {
            if (mImpl.hasColor()) {
                return ColorBuilders.ColorProp.fromProto(mImpl.getColor());
            } else {
                return null;
            }
        }

        /**
         * Gets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. Intended
         * for testing purposes only.
         */
        @Nullable
        public ModifiersBuilders.ArcModifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return ModifiersBuilders.ArcModifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static ArcLine fromProto(@NonNull LayoutElementProto.ArcLine proto) {
            return new ArcLine(proto, null);
        }

        @NonNull
        LayoutElementProto.ArcLine toProto() {
            return mImpl;
        }

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
            private final Fingerprint mFingerprint = new Fingerprint(-1371793535);

            public Builder() {}

            /** Sets the length of this line, in degrees. If not defined, defaults to 0. */
            @NonNull
            public Builder setLength(@NonNull DimensionBuilders.DegreesProp length) {
                mImpl.setLength(length.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(length.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the thickness of this line. If not defined, defaults to 0. */
            @NonNull
            public Builder setThickness(@NonNull DimensionBuilders.DpProp thickness) {
                mImpl.setThickness(thickness.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(thickness.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the color of this line. */
            @NonNull
            public Builder setColor(@NonNull ColorBuilders.ColorProp color) {
                mImpl.setColor(color.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(color.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. */
            @NonNull
            public Builder setModifiers(@NonNull ModifiersBuilders.ArcModifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public ArcLine build() {
                return new ArcLine(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A simple spacer used to provide padding between adjacent elements in an {@link Arc}. */
    public static final class ArcSpacer implements ArcLayoutElement {
        private final LayoutElementProto.ArcSpacer mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ArcSpacer(LayoutElementProto.ArcSpacer impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the length of this spacer, in degrees. If not defined, defaults to 0. Intended for
         * testing purposes only.
         */
        @Nullable
        public DimensionBuilders.DegreesProp getLength() {
            if (mImpl.hasLength()) {
                return DimensionBuilders.DegreesProp.fromProto(mImpl.getLength());
            } else {
                return null;
            }
        }

        /**
         * Gets the thickness of this spacer, in DP. If not defined, defaults to 0. Intended for
         * testing purposes only.
         */
        @Nullable
        public DimensionBuilders.DpProp getThickness() {
            if (mImpl.hasThickness()) {
                return DimensionBuilders.DpProp.fromProto(mImpl.getThickness());
            } else {
                return null;
            }
        }

        /**
         * Gets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. Intended
         * for testing purposes only.
         */
        @Nullable
        public ModifiersBuilders.ArcModifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return ModifiersBuilders.ArcModifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static ArcSpacer fromProto(@NonNull LayoutElementProto.ArcSpacer proto) {
            return new ArcSpacer(proto, null);
        }

        @NonNull
        LayoutElementProto.ArcSpacer toProto() {
            return mImpl;
        }

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
            private final Fingerprint mFingerprint = new Fingerprint(-179760535);

            public Builder() {}

            /** Sets the length of this spacer, in degrees. If not defined, defaults to 0. */
            @NonNull
            public Builder setLength(@NonNull DimensionBuilders.DegreesProp length) {
                mImpl.setLength(length.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(length.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the thickness of this spacer, in DP. If not defined, defaults to 0. */
            @NonNull
            public Builder setThickness(@NonNull DimensionBuilders.DpProp thickness) {
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(thickness.getFingerprint()).aggregateValueAsInt());
                mImpl.setThickness(thickness.toProto());
                return this;
            }

            /** Sets {@link androidx.wear.tiles.ModifiersBuilders.Modifiers} for this element. */
            @NonNull
            public Builder setModifiers(@NonNull ModifiersBuilders.ArcModifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public ArcSpacer build() {
                return new ArcSpacer(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A container that allows a standard {@link LayoutElement} to be added to an {@link Arc}. */
    public static final class ArcAdapter implements ArcLayoutElement {
        private final LayoutElementProto.ArcAdapter mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ArcAdapter(LayoutElementProto.ArcAdapter impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the element to adapt to an {@link Arc}. Intended for testing purposes only. */
        @Nullable
        public LayoutElement getContent() {
            if (mImpl.hasContent()) {
                return LayoutElementBuilders.layoutElementFromProto(mImpl.getContent());
            } else {
                return null;
            }
        }

        /**
         * Gets whether this adapter's contents should be rotated, according to its position in the
         * arc or not. As an example, assume that an {@link Image} has been added to the arc, and
         * ends up at the 3 o clock position. If rotate_contents = true, the image will be placed at
         * the 3 o clock position, and will be rotated clockwise through 90 degrees. If
         * rotate_contents = false, the image will be placed at the 3 o clock position, but itself
         * will not be rotated. If not defined, defaults to false. Intended for testing purposes
         * only.
         */
        @Nullable
        public TypeBuilders.BoolProp getRotateContents() {
            if (mImpl.hasRotateContents()) {
                return TypeBuilders.BoolProp.fromProto(mImpl.getRotateContents());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static ArcAdapter fromProto(@NonNull LayoutElementProto.ArcAdapter proto) {
            return new ArcAdapter(proto, null);
        }

        @NonNull
        LayoutElementProto.ArcAdapter toProto() {
            return mImpl;
        }

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
            private final Fingerprint mFingerprint = new Fingerprint(1696473935);

            public Builder() {}

            /** Sets the element to adapt to an {@link Arc}. */
            @NonNull
            public Builder setContent(@NonNull LayoutElement content) {
                mImpl.setContent(content.toLayoutElementProto());
                mFingerprint.addChildNode(checkNotNull(content.getFingerprint()));
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
            @NonNull
            public Builder setRotateContents(@NonNull TypeBuilders.BoolProp rotateContents) {
                mImpl.setRotateContents(rotateContents.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(rotateContents.getFingerprint()).aggregateValueAsInt());
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
                mFingerprint.recordPropertyUpdate(2, Boolean.hashCode(rotateContents));
                return this;
            }

            @Override
            @NonNull
            public ArcAdapter build() {
                return new ArcAdapter(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * Interface defining the root of all layout elements. This exists to act as a holder for all of
     * the actual layout elements above.
     */
    public interface LayoutElement {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        LayoutElementProto.LayoutElement toLayoutElementProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        Fingerprint getFingerprint();

        /** Builder to create {@link LayoutElement} objects. */
        @SuppressLint("StaticFinalBuilder")
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            LayoutElement build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static LayoutElement layoutElementFromProto(
            @NonNull LayoutElementProto.LayoutElement proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasColumn()) {
            return Column.fromProto(proto.getColumn(), fingerprint);
        }
        if (proto.hasRow()) {
            return Row.fromProto(proto.getRow(), fingerprint);
        }
        if (proto.hasBox()) {
            return Box.fromProto(proto.getBox(), fingerprint);
        }
        if (proto.hasSpacer()) {
            return Spacer.fromProto(proto.getSpacer(), fingerprint);
        }
        if (proto.hasText()) {
            return Text.fromProto(proto.getText(), fingerprint);
        }
        if (proto.hasImage()) {
            return Image.fromProto(proto.getImage(), fingerprint);
        }
        if (proto.hasArc()) {
            return Arc.fromProto(proto.getArc(), fingerprint);
        }
        if (proto.hasSpannable()) {
            return Spannable.fromProto(proto.getSpannable(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of LayoutElement");
    }

    @NonNull
    static LayoutElement layoutElementFromProto(@NonNull LayoutElementProto.LayoutElement proto) {
        return layoutElementFromProto(proto, null);
    }

    /**
     * Interface defining the root of all elements that can be used in an {@link Arc}. This exists
     * to act as a holder for all of the actual arc layout elements above.
     */
    public interface ArcLayoutElement {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        LayoutElementProto.ArcLayoutElement toArcLayoutElementProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        Fingerprint getFingerprint();

        /** Builder to create {@link ArcLayoutElement} objects. */
        @SuppressLint("StaticFinalBuilder")
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            ArcLayoutElement build();
        }
    }

    @NonNull
    static ArcLayoutElement arcLayoutElementFromProto(
            @NonNull LayoutElementProto.ArcLayoutElement proto) {
        if (proto.hasText()) {
            return ArcText.fromProto(proto.getText());
        }
        if (proto.hasLine()) {
            return ArcLine.fromProto(proto.getLine());
        }
        if (proto.hasSpacer()) {
            return ArcSpacer.fromProto(proto.getSpacer());
        }
        if (proto.hasAdapter()) {
            return ArcAdapter.fromProto(proto.getAdapter());
        }
        throw new IllegalStateException("Proto was not a recognised instance of ArcLayoutElement");
    }

    /** A complete layout. */
    public static final class Layout {
        private final LayoutElementProto.Layout mImpl;

        private Layout(LayoutElementProto.Layout impl) {
            this.mImpl = impl;
        }

        /** Gets the root element in the layout. Intended for testing purposes only. */
        @Nullable
        public LayoutElement getRoot() {
            if (mImpl.hasRoot()) {
                return LayoutElementBuilders.layoutElementFromProto(mImpl.getRoot());
            } else {
                return null;
            }
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Layout fromProto(@NonNull LayoutElementProto.Layout proto) {
            return new Layout(proto);
        }

        /** Creates a {@link Layout} object containing the given layout element. */
        @NonNull
        public static Layout fromLayoutElement(@NonNull LayoutElement layoutElement) {
            return new Builder().setRoot(layoutElement).build();
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public LayoutElementProto.Layout toProto() {
            return mImpl;
        }

        /** Converts to byte array representation. */
        @TilesExperimental
        @NonNull
        public byte[] toByteArray() {
            return mImpl.toByteArray();
        }

        /** Converts from byte array representation. */
        @TilesExperimental
        @Nullable
        public static Layout fromByteArray(@NonNull byte[] byteArray) {
            try {
                return fromProto(LayoutElementProto.Layout.parseFrom(byteArray));
            } catch (InvalidProtocolBufferException e) {
                return null;
            }
        }

        /** Builder for {@link Layout} */
        public static final class Builder {
            private final LayoutElementProto.Layout.Builder mImpl =
                    LayoutElementProto.Layout.newBuilder();

            public Builder() {}

            /** Sets the root element in the layout. */
            @NonNull
            public Builder setRoot(@NonNull LayoutElement root) {
                mImpl.setRoot(root.toLayoutElementProto());
                @Nullable Fingerprint fingerprint = root.getFingerprint();
                if (fingerprint != null) {
                    mImpl.setFingerprint(
                            TreeFingerprint.newBuilder().setRoot(fingerprintToProto(fingerprint)));
                }
                return this;
            }

            private static FingerprintProto.NodeFingerprint fingerprintToProto(
                    Fingerprint fingerprint) {
                FingerprintProto.NodeFingerprint.Builder builder =
                        FingerprintProto.NodeFingerprint.newBuilder();
                if (fingerprint.selfTypeValue() != 0) {
                    builder.setSelfTypeValue(fingerprint.selfTypeValue());
                }
                if (fingerprint.selfPropsValue() != 0) {
                    builder.setSelfPropsValue(fingerprint.selfPropsValue());
                }
                if (fingerprint.childNodesValue() != 0) {
                    builder.setChildNodesValue(fingerprint.childNodesValue());
                }
                for (Fingerprint childNode : fingerprint.childNodes()) {
                    builder.addChildNodes(fingerprintToProto(childNode));
                }
                return builder.build();
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

        private FontStyles() {}

        private static boolean isLargeScreen(
                @NonNull DeviceParametersBuilders.DeviceParameters deviceParameters) {
            return deviceParameters.getScreenWidthDp() >= LARGE_SCREEN_WIDTH_DP;
        }

        /** Font style for large display text. */
        @NonNull
        public static FontStyle.Builder display1(
                @NonNull DeviceParametersBuilders.DeviceParameters deviceParameters) {
            return new FontStyle.Builder()
                    .setWeight(FONT_WEIGHT_BOLD)
                    .setSize(DimensionBuilders.sp(isLargeScreen(deviceParameters) ? 54 : 50));
        }

        /** Font style for medium display text. */
        @NonNull
        public static FontStyle.Builder display2(
                @NonNull DeviceParametersBuilders.DeviceParameters deviceParameters) {
            return new FontStyle.Builder()
                    .setWeight(FONT_WEIGHT_BOLD)
                    .setSize(DimensionBuilders.sp(isLargeScreen(deviceParameters) ? 44 : 40));
        }

        /** Font style for small display text. */
        @NonNull
        public static FontStyle.Builder display3(
                @NonNull DeviceParametersBuilders.DeviceParameters deviceParameters) {
            return new FontStyle.Builder()
                    .setWeight(FONT_WEIGHT_BOLD)
                    .setSize(DimensionBuilders.sp(isLargeScreen(deviceParameters) ? 34 : 30));
        }

        /** Font style for large title text. */
        @NonNull
        public static FontStyle.Builder title1(
                @NonNull DeviceParametersBuilders.DeviceParameters deviceParameters) {
            return new FontStyle.Builder()
                    .setWeight(FONT_WEIGHT_BOLD)
                    .setSize(DimensionBuilders.sp(isLargeScreen(deviceParameters) ? 26 : 24));
        }

        /** Font style for medium title text. */
        @NonNull
        public static FontStyle.Builder title2(
                @NonNull DeviceParametersBuilders.DeviceParameters deviceParameters) {
            return new FontStyle.Builder()
                    .setWeight(FONT_WEIGHT_BOLD)
                    .setSize(DimensionBuilders.sp(isLargeScreen(deviceParameters) ? 22 : 20));
        }

        /** Font style for small title text. */
        @NonNull
        public static FontStyle.Builder title3(
                @NonNull DeviceParametersBuilders.DeviceParameters deviceParameters) {
            return new FontStyle.Builder()
                    .setWeight(FONT_WEIGHT_BOLD)
                    .setSize(DimensionBuilders.sp(isLargeScreen(deviceParameters) ? 18 : 16));
        }

        /** Font style for large body text. */
        @NonNull
        public static FontStyle.Builder body1(
                @NonNull DeviceParametersBuilders.DeviceParameters deviceParameters) {
            return new FontStyle.Builder()
                    .setSize(DimensionBuilders.sp(isLargeScreen(deviceParameters) ? 18 : 16));
        }

        /** Font style for medium body text. */
        @NonNull
        public static FontStyle.Builder body2(
                @NonNull DeviceParametersBuilders.DeviceParameters deviceParameters) {
            return new FontStyle.Builder()
                    .setSize(DimensionBuilders.sp(isLargeScreen(deviceParameters) ? 16 : 14));
        }

        /** Font style for button text. */
        @NonNull
        public static FontStyle.Builder button(
                @NonNull DeviceParametersBuilders.DeviceParameters deviceParameters) {
            return new FontStyle.Builder()
                    .setWeight(FONT_WEIGHT_BOLD)
                    .setSize(DimensionBuilders.sp(isLargeScreen(deviceParameters) ? 16 : 14));
        }

        /** Font style for large caption text. */
        @NonNull
        public static FontStyle.Builder caption1(
                @NonNull DeviceParametersBuilders.DeviceParameters deviceParameters) {
            return new FontStyle.Builder()
                    .setSize(DimensionBuilders.sp(isLargeScreen(deviceParameters) ? 16 : 14));
        }

        /** Font style for medium caption text. */
        @NonNull
        public static FontStyle.Builder caption2(
                @NonNull DeviceParametersBuilders.DeviceParameters deviceParameters) {
            return new FontStyle.Builder()
                    .setSize(DimensionBuilders.sp(isLargeScreen(deviceParameters) ? 14 : 12));
        }
    }
}
