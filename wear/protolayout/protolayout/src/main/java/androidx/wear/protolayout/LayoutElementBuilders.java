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

package androidx.wear.protolayout;

import static androidx.annotation.Dimension.DP;
import static androidx.wear.protolayout.DimensionBuilders.degrees;
import static androidx.wear.protolayout.DimensionBuilders.dp;
import static androidx.wear.protolayout.DimensionBuilders.sp;
import static androidx.wear.protolayout.expression.Preconditions.checkNotNull;

import static java.nio.charset.StandardCharsets.US_ASCII;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.Dimension;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.StringDef;
import androidx.annotation.VisibleForTesting;
import androidx.wear.protolayout.ColorBuilders.Brush;
import androidx.wear.protolayout.ColorBuilders.ColorProp;
import androidx.wear.protolayout.ColorBuilders.SweepGradient;
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.protolayout.DimensionBuilders.AngularDimension;
import androidx.wear.protolayout.DimensionBuilders.AngularLayoutConstraint;
import androidx.wear.protolayout.DimensionBuilders.ContainerDimension;
import androidx.wear.protolayout.DimensionBuilders.DegreesProp;
import androidx.wear.protolayout.DimensionBuilders.DpProp;
import androidx.wear.protolayout.DimensionBuilders.EmProp;
import androidx.wear.protolayout.DimensionBuilders.ExtensionDimension;
import androidx.wear.protolayout.DimensionBuilders.HorizontalLayoutConstraint;
import androidx.wear.protolayout.DimensionBuilders.ImageDimension;
import androidx.wear.protolayout.DimensionBuilders.SpProp;
import androidx.wear.protolayout.DimensionBuilders.SpacerDimension;
import androidx.wear.protolayout.DimensionBuilders.VerticalLayoutConstraint;
import androidx.wear.protolayout.ModifiersBuilders.ArcModifiers;
import androidx.wear.protolayout.ModifiersBuilders.Modifiers;
import androidx.wear.protolayout.ModifiersBuilders.Shadow;
import androidx.wear.protolayout.ModifiersBuilders.SpanModifiers;
import androidx.wear.protolayout.ResourceBuilders.ImageResource;
import androidx.wear.protolayout.TypeBuilders.BoolProp;
import androidx.wear.protolayout.TypeBuilders.Int32Prop;
import androidx.wear.protolayout.TypeBuilders.StringLayoutConstraint;
import androidx.wear.protolayout.TypeBuilders.StringProp;
import androidx.wear.protolayout.expression.ExperimentalProtoLayoutExtensionApi;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.expression.ProtoLayoutExperimental;
import androidx.wear.protolayout.expression.RequiresSchemaVersion;
import androidx.wear.protolayout.proto.AlignmentProto;
import androidx.wear.protolayout.proto.ColorProto;
import androidx.wear.protolayout.proto.DimensionProto;
import androidx.wear.protolayout.proto.FingerprintProto;
import androidx.wear.protolayout.proto.FingerprintProto.TreeFingerprint;
import androidx.wear.protolayout.proto.LayoutElementProto;
import androidx.wear.protolayout.protobuf.ByteString;
import androidx.wear.protolayout.protobuf.InvalidProtocolBufferException;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Builders for composable layout elements that can be combined together to create renderable UI
 * layouts.
 */
public final class LayoutElementBuilders {
    private LayoutElementBuilders() {}

    @VisibleForTesting static final String WEIGHT_AXIS_TAG = "wght";
    @VisibleForTesting static final String WIDTH_AXIS_TAG = "wdth";
    @VisibleForTesting static final String ROUNDNESS_AXIS_TAG = "ROND";
    @VisibleForTesting static final String TABULAR_OPTION_TAG = "tnum";

    /** The weight to be applied to the font. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({FONT_WEIGHT_UNDEFINED, FONT_WEIGHT_NORMAL, FONT_WEIGHT_MEDIUM, FONT_WEIGHT_BOLD})
    @Retention(RetentionPolicy.SOURCE)
    @OptIn(markerClass = ProtoLayoutExperimental.class)
    public @interface FontWeight {}

    /** Font weight is undefined. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int FONT_WEIGHT_UNDEFINED = 0;

    /** Normal font weight. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int FONT_WEIGHT_NORMAL = 400;

    /** Medium font weight. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    @ProtoLayoutExperimental
    public static final int FONT_WEIGHT_MEDIUM = 500;

    /** Bold font weight. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int FONT_WEIGHT_BOLD = 700;

    /**
     * The variant of a font. Some renderers may use different fonts for title and body text, which
     * can be selected using this field.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({FONT_VARIANT_UNDEFINED, FONT_VARIANT_TITLE, FONT_VARIANT_BODY, FONT_VARIANT_CUSTOM_1})
    @Retention(RetentionPolicy.SOURCE)
    @OptIn(markerClass = ProtoLayoutExperimental.class)
    public @interface FontVariant {}

    /** Font variant is undefined. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int FONT_VARIANT_UNDEFINED = 0;

    /** Font variant suited for title text. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int FONT_VARIANT_TITLE = 1;

    /** Font variant suited for body text. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int FONT_VARIANT_BODY = 2;

    /**
     * Renderer dependent Font variant. If not supported, will behave similar to {@link
     * #FONT_VARIANT_UNDEFINED}.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final int FONT_VARIANT_CUSTOM_1 = 3;

    /**
     * The alignment of a {@link SpanImage} within the line height of the surrounding {@link
     * Spannable}.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
        SPAN_VERTICAL_ALIGN_UNDEFINED,
        SPAN_VERTICAL_ALIGN_BOTTOM,
        SPAN_VERTICAL_ALIGN_TEXT_BASELINE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SpanVerticalAlignment {}

    /** Alignment is undefined. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int SPAN_VERTICAL_ALIGN_UNDEFINED = 0;

    /**
     * Align to the bottom of the line (descent of the largest text in this line). If there is no
     * text in the line containing this image, this will align to the bottom of the line, where the
     * line height is defined as the height of the largest image in the line.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int SPAN_VERTICAL_ALIGN_BOTTOM = 1;

    /**
     * Align to the baseline of the text. Note that if the line in the {@link Spannable} which
     * contains this image does not contain any text, the effects of using this alignment are
     * undefined.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int SPAN_VERTICAL_ALIGN_TEXT_BASELINE = 2;

    /** How text that will not fit inside the bounds of a {@link Text} element will be handled. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
        TEXT_OVERFLOW_UNDEFINED,
        TEXT_OVERFLOW_TRUNCATE,
        TEXT_OVERFLOW_ELLIPSIZE_END,
        TEXT_OVERFLOW_MARQUEE,
        TEXT_OVERFLOW_ELLIPSIZE
    })
    @Retention(RetentionPolicy.SOURCE)
    @OptIn(markerClass = ProtoLayoutExperimental.class)
    public @interface TextOverflow {}

    /** Overflow behavior is undefined. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int TEXT_OVERFLOW_UNDEFINED = 0;

    /**
     * Truncate the text to fit inside of the {@link Text} element's bounds. If text is truncated,
     * it will be truncated on a word boundary.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int TEXT_OVERFLOW_TRUNCATE = 1;

    /**
     * Truncate the text at the last line defined by {@code setMaxLines} in {@link Text} to fit in
     * the {@link Text} element's bounds, but add an ellipsis (i.e. ...) to the end of the text if
     * it has been truncated. Note that this will not add an ellipsis if the number of lines that
     * fits into the available space is less than the {@code setMaxLines} in {@link Text}.
     *
     * @deprecated Use {@link #TEXT_OVERFLOW_ELLIPSIZE} instead.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    @Deprecated
    public static final int TEXT_OVERFLOW_ELLIPSIZE_END = 2;

    /**
     * Enable marquee animation for texts that don't fit inside the {@link Text} element. This is
     * only applicable for single line texts; if the text has multiple lines, the behavior is
     * equivalent to TEXT_OVERFLOW_TRUNCATE.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    public static final int TEXT_OVERFLOW_MARQUEE = 3;

    /**
     * Truncate the text to fit in the {@link Text} element's parent bounds, but add an ellipsis
     * (i.e. ...) to the end of the text if it has been truncated.
     *
     * <p>Note that, when this is used, the parent of the {@link Text} element this corresponds to
     * shouldn't have its width and height set to wrapped, as it can lead to unexpected results.
     */
    @RequiresSchemaVersion(major = 1, minor = 300)
    public static final int TEXT_OVERFLOW_ELLIPSIZE = 4;

    /**
     * How content which does not match the dimensions of its bounds (e.g. an image resource being
     * drawn inside an {@link Image}) will be resized to fit its bounds.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
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
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int CONTENT_SCALE_MODE_UNDEFINED = 0;

    /**
     * Content will be scaled to fit inside its bounds, proportionally. As an example, If a 10x5
     * image was going to be drawn inside a 50x50 {@link Image} element, the actual image resource
     * would be drawn as a 50x25 image, centered within the 50x50 bounds.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int CONTENT_SCALE_MODE_FIT = 1;

    /**
     * Content will be resized proportionally so it completely fills its bounds, and anything
     * outside of the bounds will be cropped. As an example, if a 10x5 image was going to be drawn
     * inside a 50x50 {@link Image} element, the image resource would be drawn as a 100x50 image,
     * centered within its bounds (and with 25px cropped from both the left and right sides).
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int CONTENT_SCALE_MODE_CROP = 2;

    /**
     * Content will be resized to fill its bounds, without taking into account the aspect ratio. If
     * a 10x5 image was going to be drawn inside a 50x50 {@link Image} element, the image would be
     * drawn as a 50x50 image, stretched vertically.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int CONTENT_SCALE_MODE_FILL_BOUNDS = 3;

    /** Styles to use for path endings. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({STROKE_CAP_UNDEFINED, STROKE_CAP_BUTT, STROKE_CAP_ROUND, STROKE_CAP_SQUARE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface StrokeCap {}

    /** {@code StrokeCap} is undefined. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static final int STROKE_CAP_UNDEFINED = 0;

    /** Begin and end contours with a flat edge and no extension. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static final int STROKE_CAP_BUTT = 1;

    /**
     * Begin and end contours with a semi-circle extension. The extension size is proportional to
     * the thickness of the path.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static final int STROKE_CAP_ROUND = 2;

    /**
     * Begin and end contours with a half square extension. The extension size is proportional to
     * the thickness of the path.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static final int STROKE_CAP_SQUARE = 3;

    /** Direction of drawing for any curved element. */
    @RequiresSchemaVersion(major = 1, minor = 300)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({ARC_DIRECTION_NORMAL, ARC_DIRECTION_CLOCKWISE, ARC_DIRECTION_COUNTER_CLOCKWISE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ArcDirection {}

    /**
     * Draws an element in Clockwise direction for LTR layout direction and Counter Clockwise for
     * RTL.
     */
    @RequiresSchemaVersion(major = 1, minor = 300)
    public static final int ARC_DIRECTION_NORMAL = 0;

    /** Draws an element in Clockwise direction, independently of layout direction. */
    @RequiresSchemaVersion(major = 1, minor = 300)
    public static final int ARC_DIRECTION_CLOCKWISE = 1;

    /** Draws an element in Counter Clockwise direction, independently of layout direction. */
    @RequiresSchemaVersion(major = 1, minor = 300)
    public static final int ARC_DIRECTION_COUNTER_CLOCKWISE = 2;

    /** An extensible {@code FontWeight} property. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class FontWeightProp {
        private final LayoutElementProto.FontWeightProp mImpl;
        private final @Nullable Fingerprint mFingerprint;

        FontWeightProp(LayoutElementProto.FontWeightProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. */
        @FontWeight
        public int getValue() {
            return mImpl.getValue().getNumber();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull FontWeightProp fromProto(
                LayoutElementProto.@NonNull FontWeightProp proto,
                @Nullable Fingerprint fingerprint) {
            return new FontWeightProp(proto, fingerprint);
        }

        static @NonNull FontWeightProp fromProto(LayoutElementProto.@NonNull FontWeightProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull FontWeightProp toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "FontWeightProp{" + "value=" + getValue() + "}";
        }

        /** Builder for {@link FontWeightProp} */
        public static final class Builder {
            private final LayoutElementProto.FontWeightProp.Builder mImpl =
                    LayoutElementProto.FontWeightProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1485961687);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the value. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setValue(@FontWeight int value) {
                mImpl.setValue(LayoutElementProto.FontWeight.forNumber(value));
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull FontWeightProp build() {
                return new FontWeightProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** An extensible {@code FontVariant} property. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    @ProtoLayoutExperimental
    public static final class FontVariantProp {
        private final LayoutElementProto.FontVariantProp mImpl;
        private final @Nullable Fingerprint mFingerprint;

        FontVariantProp(
                LayoutElementProto.FontVariantProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. */
        @FontVariant
        public int getValue() {
            return mImpl.getValue().getNumber();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull FontVariantProp fromProto(
                LayoutElementProto.@NonNull FontVariantProp proto,
                @Nullable Fingerprint fingerprint) {
            return new FontVariantProp(proto, fingerprint);
        }

        static @NonNull FontVariantProp fromProto(
                LayoutElementProto.@NonNull FontVariantProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull FontVariantProp toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "FontVariantProp{" + "value=" + getValue() + "}";
        }

        /** Builder for {@link FontVariantProp} */
        public static final class Builder {
            private final LayoutElementProto.FontVariantProp.Builder mImpl =
                    LayoutElementProto.FontVariantProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-295272526);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the value. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setValue(@FontVariant int value) {
                mImpl.setValue(LayoutElementProto.FontVariant.forNumber(value));
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull FontVariantProp build() {
                return new FontVariantProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** An extensible {@code SpanVerticalAlignment} property. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class SpanVerticalAlignmentProp {
        private final LayoutElementProto.SpanVerticalAlignmentProp mImpl;
        private final @Nullable Fingerprint mFingerprint;

        SpanVerticalAlignmentProp(
                LayoutElementProto.SpanVerticalAlignmentProp impl,
                @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. */
        @SpanVerticalAlignment
        public int getValue() {
            return mImpl.getValue().getNumber();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull SpanVerticalAlignmentProp fromProto(
                LayoutElementProto.@NonNull SpanVerticalAlignmentProp proto,
                @Nullable Fingerprint fingerprint) {
            return new SpanVerticalAlignmentProp(proto, fingerprint);
        }

        static @NonNull SpanVerticalAlignmentProp fromProto(
                LayoutElementProto.@NonNull SpanVerticalAlignmentProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull SpanVerticalAlignmentProp toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "SpanVerticalAlignmentProp{" + "value=" + getValue() + "}";
        }

        /** Builder for {@link SpanVerticalAlignmentProp} */
        public static final class Builder {
            private final LayoutElementProto.SpanVerticalAlignmentProp.Builder mImpl =
                    LayoutElementProto.SpanVerticalAlignmentProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1822193880);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the value. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setValue(@SpanVerticalAlignment int value) {
                mImpl.setValue(LayoutElementProto.SpanVerticalAlignment.forNumber(value));
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull SpanVerticalAlignmentProp build() {
                return new SpanVerticalAlignmentProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** The styling of a font (e.g. font size, and metrics). */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class FontStyle {
        private final LayoutElementProto.FontStyle mImpl;
        private final @Nullable Fingerprint mFingerprint;

        FontStyle(LayoutElementProto.FontStyle impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets whether the text should be rendered in a italic typeface. If not specified, defaults
         * to "false".
         */
        private @Nullable BoolProp isItalic() {
            if (mImpl.hasItalic()) {
                return BoolProp.fromProto(mImpl.getItalic());
            } else {
                return null;
            }
        }

        /**
         * Gets whether the text should be rendered with an underline. If not specified, defaults to
         * "false".
         */
        private @Nullable BoolProp isUnderline() {
            if (mImpl.hasUnderline()) {
                return BoolProp.fromProto(mImpl.getUnderline());
            } else {
                return null;
            }
        }

        /**
         * Gets the text color. If not defined, defaults to white.
         *
         * <p>While this field is statically accessible from 1.0, it's only bindable since version
         * 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
         */
        public @Nullable ColorProp getColor() {
            if (mImpl.hasColor()) {
                return ColorProp.fromProto(mImpl.getColor());
            } else {
                return null;
            }
        }

        /**
         * Gets the weight of the font. If the provided value is not supported on a platform, the
         * nearest supported value will be used. If not defined, or when set to an invalid value,
         * defaults to "normal".
         */
        public @Nullable FontWeightProp getWeight() {
            if (mImpl.hasWeight()) {
                return FontWeightProp.fromProto(mImpl.getWeight());
            } else {
                return null;
            }
        }

        /**
         * Gets the text letter-spacing. Positive numbers increase the space between letters while
         * negative numbers tighten the space. If not specified, defaults to 0.
         */
        public @Nullable EmProp getLetterSpacing() {
            if (mImpl.hasLetterSpacing()) {
                return EmProp.fromProto(mImpl.getLetterSpacing());
            } else {
                return null;
            }
        }

        /**
         * Gets the variant of a font. Some renderers may use different fonts for title and body
         * text, which can be selected using this field. If not specified, defaults to "body".
         */
        @ProtoLayoutExperimental
        public @Nullable FontVariantProp getVariant() {
            if (mImpl.hasVariant()) {
                return FontVariantProp.fromProto(mImpl.getVariant());
            } else {
                return null;
            }
        }

        /**
         * Gets the collection of font settings to be applied.
         *
         * <p>Supported settings depend on the font used and renderer version.
         */
        public @NonNull List<FontSetting> getSettings() {
            List<FontSetting> list = new ArrayList<>();
            for (LayoutElementProto.FontSetting item : mImpl.getSettingsList()) {
                list.add(LayoutElementBuilders.fontSettingFromProto(item));
            }
            return Collections.unmodifiableList(list);
        }

        /**
         * Gets the prioritized collection of font family names describing which font should be used
         * for this {@link FontStyle} and its fallback values if not available. For example,
         * preferring default system variable font with default non variable system font as a
         * fallback.
         */
        public @NonNull List<String> getPreferredFontFamilies() {
            return mImpl.getPreferredFontFamiliesList();
        }

        /**
         * Gets the size of the font, in scaled pixels (sp). If more than one size was originally
         * added, it will return the last one.
         */
        public @Nullable SpProp getSize() {
            List<DimensionProto.SpProp> sizes = mImpl.getSizeList();
            return !sizes.isEmpty() ? SpProp.fromProto(sizes.get(sizes.size() - 1)) : null;
        }

        /** Gets the available sizes of the font, in scaled pixels (sp). */
        @ProtoLayoutExperimental
        public @NonNull List<SpProp> getSizes() {
            List<SpProp> list = new ArrayList<>();
            for (DimensionProto.SpProp item : mImpl.getSizeList()) {
                list.add(SpProp.fromProto(item));
            }
            return Collections.unmodifiableList(list);
        }

        /**
         * Gets whether the text should be rendered in a italic typeface. If not specified, defaults
         * to "false".
         */
        public @Nullable BoolProp getItalic() {
            return isItalic();
        }

        /**
         * Gets whether the text should be rendered with an underline. If not specified, defaults to
         * "false".
         */
        public @Nullable BoolProp getUnderline() {
            return isUnderline();
        }

        /** The recommended font family names to be used within {@link FontStyle}. */
        @RequiresSchemaVersion(major = 1, minor = 400)
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @StringDef(
                value = {ROBOTO_FONT, ROBOTO_FLEX_FONT},
                open = true)
        public @interface FontFamilyName {}

        /**
         * Font family name that uses Roboto font. Supported in renderers supporting 1.4, but the
         * actual availability of this font is dependent on the devices.
         */
        @RequiresSchemaVersion(major = 1, minor = 400)
        public static final String ROBOTO_FONT = "roboto";

        /**
         * Font family name that uses Roboto Flex variable font. Supported in renderers supporting
         * 1.4, but the actual availability of this font is dependent on the devices.
         */
        @RequiresSchemaVersion(major = 1, minor = 400)
        public static final String ROBOTO_FLEX_FONT = "roboto-flex";

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull FontStyle fromProto(
                LayoutElementProto.@NonNull FontStyle proto, @Nullable Fingerprint fingerprint) {
            return new FontStyle(proto, fingerprint);
        }

        static @NonNull FontStyle fromProto(LayoutElementProto.@NonNull FontStyle proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull FontStyle toProto() {
            return mImpl;
        }

        @Override
        @OptIn(markerClass = ProtoLayoutExperimental.class)
        public @NonNull String toString() {
            return "FontStyle{"
                    + "size="
                    + getSize()
                    + ", italic="
                    + getItalic()
                    + ", underline="
                    + getUnderline()
                    + ", color="
                    + getColor()
                    + ", weight="
                    + getWeight()
                    + ", letterSpacing="
                    + getLetterSpacing()
                    + ", variant="
                    + getVariant()
                    + ", settings="
                    + getSettings()
                    + ", preferredFontFamilies="
                    + getPreferredFontFamilies()
                    + "}";
        }

        /** Builder for {@link FontStyle} */
        public static final class Builder {
            private final LayoutElementProto.FontStyle.Builder mImpl =
                    LayoutElementProto.FontStyle.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-374492482);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets whether the text should be rendered in a italic typeface. If not specified,
             * defaults to "false".
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setItalic(@NonNull BoolProp italic) {
                if (italic.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "FontStyle.Builder.setItalic doesn't support dynamic values.");
                }
                mImpl.setItalic(italic.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(italic.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets whether the text should be rendered in a italic typeface. If not specified,
             * defaults to "false".
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            @SuppressLint("MissingGetterMatchingBuilder")
            public @NonNull Builder setItalic(boolean italic) {
                return setItalic(new BoolProp.Builder(italic).build());
            }

            /**
             * Sets whether the text should be rendered with an underline. If not specified,
             * defaults to "false".
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setUnderline(@NonNull BoolProp underline) {
                if (underline.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "FontStyle.Builder.setUnderline doesn't support dynamic values.");
                }
                mImpl.setUnderline(underline.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(underline.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets whether the text should be rendered with an underline. If not specified,
             * defaults to "false".
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            @SuppressLint("MissingGetterMatchingBuilder")
            public @NonNull Builder setUnderline(boolean underline) {
                return setUnderline(new BoolProp.Builder(underline).build());
            }

            /**
             * Sets the text color. If not defined, defaults to white.
             *
             * <p>While this field is statically accessible from 1.0, it's only bindable since
             * version 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setColor(@NonNull ColorProp color) {
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
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setWeight(@NonNull FontWeightProp weight) {
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
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setWeight(@FontWeight int weight) {
                return setWeight(new FontWeightProp.Builder().setValue(weight).build());
            }

            /**
             * Sets the text letter-spacing. Positive numbers increase the space between letters
             * while negative numbers tighten the space. If not specified, defaults to 0.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setLetterSpacing(@NonNull EmProp letterSpacing) {
                mImpl.setLetterSpacing(letterSpacing.toProto());
                mFingerprint.recordPropertyUpdate(
                        6, checkNotNull(letterSpacing.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the variant of a font. Some renderers may use different fonts for title and body
             * text, which can be selected using this field. If not specified, defaults to "body".
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            @ProtoLayoutExperimental
            public @NonNull Builder setVariant(@NonNull FontVariantProp variant) {
                mImpl.setVariant(variant.toProto());
                mFingerprint.recordPropertyUpdate(
                        7, checkNotNull(variant.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the variant of a font. Some renderers may use different fonts for title and body
             * text, which can be selected using this field. If not specified, defaults to "body".
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            @ProtoLayoutExperimental
            public @NonNull Builder setVariant(@FontVariant int variant) {
                mImpl.setVariant(
                        LayoutElementProto.FontVariantProp.newBuilder()
                                .setValue(LayoutElementProto.FontVariant.forNumber(variant)));
                mFingerprint.recordPropertyUpdate(7, variant);
                return this;
            }

            /**
             * Adds one item to the collection of font settings to be applied.
             *
             * <p>Supported settings depend on the font used and renderer version. If this is used
             * with the variable fonts on renderers supporting 1.4, weight and width setting will be
             * always available.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            private @NonNull Builder addSetting(@NonNull FontSetting setting) {
                mImpl.addSettings(setting.toFontSettingProto());
                mFingerprint.recordPropertyUpdate(
                        8, checkNotNull(setting.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Adds one item to the prioritized collection of font family names describing which
             * font should be used for this {@link FontStyle} and its fallback values if not
             * available. For example, preferring default system variable font with default non
             * variable system font as a fallback. If not set, defaults to system font.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            private @NonNull Builder addPreferredFontFamily(@NonNull String preferredFontFamily) {
                mImpl.addPreferredFontFamilies(preferredFontFamily);
                mFingerprint.recordPropertyUpdate(9, preferredFontFamily.hashCode());
                return this;
            }

            @VisibleForTesting static final int TEXT_SIZES_LIMIT = 10;

            /**
             * Sets the available sizes of the font, in scaled pixels (sp). If not specified,
             * defaults to the size of the system's "body" font.
             *
             * <p>If more than one size is specified and this {@link FontStyle} is applied to a
             * {@link Text} element with static text, the text size will be automatically picked
             * from the provided sizes to try to perfectly fit within its parent bounds. In other
             * words, the largest size from the specified preset sizes that can fit the most text
             * within the parent bounds will be used.
             *
             * <p>The specified sizes don't have to be sorted, but they need to contain only
             * positive values. The maximum number of sizes used is limited to 10.
             *
             * <p>Note that, if multiple sizes are set, the parent of the {@link Text} element this
             * corresponds to shouldn't have its width and height set to wrapped, as it can lead to
             * unexpected results.
             *
             * <p>If this {@link FontStyle} is set to any other element besides {@link Text} or that
             * {@link Text} element has dynamic field, only the last added size will be used.
             *
             * <p>Any previously added values with this method or {@link #setSize} will be cleared.
             *
             * <p>While this field is accessible from 1.0 as singular, it only accepts multiple
             * values since version 1.3 and renderers supporting version 1.3 will use the multiple
             * values to automatically scale text. Renderers who don't support this version will use
             * the last size among multiple values.
             *
             * @throws IllegalArgumentException if the number of available sizes is larger than 10
             *     or one of the sizes is not a positive value.
             */
            @RequiresSchemaVersion(major = 1, minor = 300)
            @ProtoLayoutExperimental
            public @NonNull Builder setSizes(
                    @IntRange(from = 1) @Dimension(unit = Dimension.SP) int @NonNull ... sizes) {
                if (sizes.length > TEXT_SIZES_LIMIT) {
                    throw new IllegalArgumentException(
                            "Number of available sizes of the font style can't be larger than 10.");
                }

                mImpl.clearSize();
                for (int size : sizes) {
                    if (size <= 0) {
                        throw new IllegalArgumentException(
                                "Available sizes of the font style must contain only positive "
                                        + "value.");
                    }

                    SpProp spPropSize = sp(size);
                    mImpl.addSize(spPropSize.toProto());
                    mFingerprint.recordPropertyUpdate(
                            1, checkNotNull(spPropSize.getFingerprint()).aggregateValueAsInt());
                }
                return this;
            }

            /**
             * Sets the size of the font, in scaled pixels (sp). If not specified, defaults to the
             * size of the system's "body" font.
             *
             * <p>Any previously added values with this method or {@link #setSizes} will be cleared.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setSize(@NonNull SpProp size) {
                mImpl.clearSize();
                mImpl.addSize(size.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(size.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the preferred font families for this {@link FontStyle}.
             *
             * <p>For example, preferring default system variable font with default non variable
             * system font as a fallback.
             *
             * <p>If the given font family is not available on a device, the fallback values will be
             * attempted to use, in order in which they are given.
             *
             * <p>Renderer support for values outside of the given constants ({@link #ROBOTO_FONT}
             * or {@link #ROBOTO_FLEX_FONT}) is not guaranteed for all devices.
             *
             * <p>If not set, default system font will be used.
             *
             * @param fontFamily preferred font family name to be used if available
             * @param fallbacks the ordered list of fallback font family to attempt to use if the
             *     preferred font family is not available.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public @NonNull Builder setPreferredFontFamilies(
                    @FontFamilyName @NonNull String fontFamily, String @NonNull ... fallbacks) {
                addPreferredFontFamily(fontFamily);
                for (String fallback : fallbacks) {
                    addPreferredFontFamily(fallback);
                }
                return this;
            }

            @VisibleForTesting static final int SETTINGS_LIMIT = 10;

            /**
             * Sets the collection of font settings to be applied. If more than one Setting with the
             * same axis tag is added, the first one will be used.
             *
             * <p>Any previously added settings will be cleared.
             *
             * <p>Supported settings depend on the font used and renderer version. If this is used
             * with the variable fonts on renderers supporting 1.4, {@link FontSetting#weight} and
             * {@link FontSetting#width} setting will always be available.
             *
             * <p>Consider providing a fallback values with {@link #setWeight} for devices that
             * don't support variable fonts. For example, using {@link #FONT_WEIGHT_MEDIUM} for
             * weight axis with value greater or equal to {@code 500}.
             *
             * @throws IllegalArgumentException if the number of the given Setting is larger than
             *     10.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public @NonNull Builder setSettings(FontSetting @NonNull ... settings) {
                if (settings.length > SETTINGS_LIMIT) {
                    throw new IllegalArgumentException(
                            "Number of given FontSetting can't be larger than "
                                    + SETTINGS_LIMIT
                                    + ".");
                }

                // To make sure we only pass in unique ones.
                Set<String> axes = new HashSet<>();

                mImpl.clearSettings();

                for (FontSetting setting : settings) {
                    String settingTag = "";

                    switch (setting.toFontSettingProto().getInnerCase()) {
                        case VARIATION:
                            settingTag = ((FontVariationSetting) setting).getAxisTag();
                            break;
                        case FEATURE:
                            settingTag = ((FontFeatureSetting) setting).getTag();
                            break;
                        case INNER_NOT_SET:
                            break;
                    }

                    if (settingTag.isEmpty() || axes.contains(settingTag)) {
                        // We don't want to add duplicates and will only include the first one.
                        continue;
                    }

                    addSetting(setting);

                    axes.add(settingTag);
                }

                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull FontStyle build() {
                return new FontStyle(mImpl.build(), mFingerprint);
            }
        }
    }

    /** Interface defining a single point of customization in a font. */
    @RequiresSchemaVersion(major = 1, minor = 400)
    public interface FontSetting {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        LayoutElementProto.@NonNull FontSetting toFontSettingProto();

        /**
         * {@link FontSetting} option for custom weight for font. Similar to the {@link
         * FontWeightProp} but it accepts any value. For more information, see <a
         * href="https://fonts.google.com/knowledge/glossary/weight_axis">here</a>.
         *
         * <p>Note that using this {@link FontSetting} will override {@link
         * FontStyle.Builder#setWeight}.
         *
         * @param value weight, usually in 1..1000, but actual range can be smaller, depending on
         *     the font used
         */
        @RequiresSchemaVersion(major = 1, minor = 400)
        static @NonNull FontSetting weight(@IntRange(from = 1, to = 1000) int value) {
            return new FontVariationSetting.Builder(WEIGHT_AXIS_TAG, value).build();
        }

        /**
         * {@link FontSetting} option for custom width for font. For more information, see <a
         * href="https://fonts.google.com/knowledge/glossary/width_axis">here</a>.
         *
         * @param value width, usually in 25..200, but actual range can depend on the font used
         */
        @RequiresSchemaVersion(major = 1, minor = 400)
        static @NonNull FontSetting width(@FloatRange(from = 25, to = 200) float value) {
            return new FontVariationSetting.Builder(WIDTH_AXIS_TAG, value).build();
        }

        /**
         * {@link FontSetting} option for custom roundness for font. For more information, see <a
         * href="https://fonts.google.com/knowledge/glossary/rond_axis">here</a>.
         *
         * @param value roundness, usually in 0..100, but actual range and availability can depend
         *     on the font used
         */
        @RequiresSchemaVersion(major = 1, minor = 400)
        static @NonNull FontSetting roundness(int value) {
            return new FontVariationSetting.Builder(ROUNDNESS_AXIS_TAG, value).build();
        }

        /**
         * {@link FontSetting} option for enabling displaying tabular numerals. In other words, all
         * numeral characters will have the same width. This corresponds to {@code tnum} OpenType
         * feature.
         *
         * <p>This setting's availability is font dependent and may not have effect on all font
         * families, some of them like Roboto automatically space out numeral characters to have the
         * same width, while other characters will have their own width.
         */
        @RequiresSchemaVersion(major = 1, minor = 400)
        static @NonNull FontSetting tabularNum() {
            return new FontFeatureSetting.Builder(TABULAR_OPTION_TAG).build();
        }

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable Fingerprint getFingerprint();

        /** Builder to create {@link FontSetting} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull FontSetting build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static @NonNull FontSetting fontSettingFromProto(
            LayoutElementProto.@NonNull FontSetting proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasVariation()) {
            return FontVariationSetting.fromProto(proto.getVariation(), fingerprint);
        }
        if (proto.hasFeature()) {
            return FontFeatureSetting.fromProto(proto.getFeature(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of FontSetting");
    }

    static @NonNull FontSetting fontSettingFromProto(
            LayoutElementProto.@NonNull FontSetting proto) {
        return fontSettingFromProto(proto, null);
    }

    /** A single point of customization in a font variation, with axis tag and a value for it. */
    @RequiresSchemaVersion(major = 1, minor = 400)
    static final class FontVariationSetting implements FontSetting {
        private final LayoutElementProto.FontVariationSetting mImpl;
        private final @Nullable Fingerprint mFingerprint;

        FontVariationSetting(
                LayoutElementProto.FontVariationSetting impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value for this setting. */
        float getValue() {
            return mImpl.getValue();
        }

        /** Gets the axis tag for this font setting. This represents a 4 ASCII characters tag. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @NonNull String getAxisTag() {
            return new String(ByteBuffer.allocate(4).putInt(mImpl.getAxisTag()).array(), US_ASCII);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            FontVariationSetting that = (FontVariationSetting) o;
            return Objects.equals(getAxisTag(), that.getAxisTag())
                    && Float.compare(getValue(), that.getValue()) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getAxisTag(), getValue());
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull FontVariationSetting fromProto(
                LayoutElementProto.@NonNull FontVariationSetting proto,
                @Nullable Fingerprint fingerprint) {
            return new FontVariationSetting(proto, fingerprint);
        }

        static @NonNull FontVariationSetting fromProto(
                LayoutElementProto.@NonNull FontVariationSetting proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        LayoutElementProto.@NonNull FontVariationSetting toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull FontSetting toFontSettingProto() {
            return LayoutElementProto.FontSetting.newBuilder().setVariation(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "FontVariationSetting{" + "value=" + getValue() + "}";
        }

        /** Builder for {@link FontVariationSetting}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements FontSetting.Builder {
            private final LayoutElementProto.FontVariationSetting.Builder mImpl =
                    LayoutElementProto.FontVariationSetting.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(361361700);

            /** Sets the value for this setting. */
            @RequiresSchemaVersion(major = 1, minor = 400)
            @NonNull Builder setValue(float value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(2, Float.floatToIntBits(value));
                return this;
            }

            /**
             * Creates an instance of {@link Builder}.
             *
             * @param axisTag the axis tag for this font setting. This represents a 4 ASCII
             *     characters tag.
             * @param value the value for this font setting.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public Builder(@NonNull String axisTag, float value) {
                setAxisTag(axisTag);
                setValue(value);
            }

            /**
             * Sets the axis tag for this font setting. This represents a 4 ASCII characters tag.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            @NonNull Builder setAxisTag(@NonNull String axisTag) {
                int axisTagInt = ByteBuffer.wrap(axisTag.getBytes()).getInt();
                mImpl.setAxisTag(axisTagInt);
                mFingerprint.recordPropertyUpdate(1, axisTagInt);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull FontVariationSetting build() {
                return new FontVariationSetting(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A single point of customization in a font feature, with specified tag. */
    @RequiresSchemaVersion(major = 1, minor = 400)
    static final class FontFeatureSetting implements FontSetting {
        private final LayoutElementProto.FontFeatureSetting mImpl;
        private final @Nullable Fingerprint mFingerprint;

        FontFeatureSetting(
                LayoutElementProto.FontFeatureSetting impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the feature tag. This represents a 4 ASCII characters tag. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @NonNull String getTag() {
            return new String(
                    ByteBuffer.allocate(4).putInt(mImpl.getTag()).array(),
                    StandardCharsets.US_ASCII);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            FontFeatureSetting that = (FontFeatureSetting) o;
            return Objects.equals(getTag(), that.getTag());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getTag());
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull FontFeatureSetting fromProto(
                LayoutElementProto.@NonNull FontFeatureSetting proto,
                @Nullable Fingerprint fingerprint) {
            return new FontFeatureSetting(proto, fingerprint);
        }

        static @NonNull FontFeatureSetting fromProto(
                LayoutElementProto.@NonNull FontFeatureSetting proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        LayoutElementProto.@NonNull FontFeatureSetting toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull FontSetting toFontSettingProto() {
            return LayoutElementProto.FontSetting.newBuilder().setFeature(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "FontFeatureSetting";
        }

        /** Builder for {@link FontFeatureSetting}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements FontSetting.Builder {
            private final LayoutElementProto.FontFeatureSetting.Builder mImpl =
                    LayoutElementProto.FontFeatureSetting.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-2557130);

            /**
             * Creates an instance of {@link Builder}.
             *
             * @param tag the tag for this font feature. This represents a 4 ASCII characters tag.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public Builder(@NonNull String tag) {
                setTag(tag);
            }

            /** Sets the feature tag. This represents a 4 ASCII characters tag. */
            @RequiresSchemaVersion(major = 1, minor = 400)
            @NonNull Builder setTag(@NonNull String tag) {
                int tagInt = ByteBuffer.wrap(tag.getBytes()).getInt();
                mImpl.setTag(tagInt);
                mFingerprint.recordPropertyUpdate(1, tagInt);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull FontFeatureSetting build() {
                return new FontFeatureSetting(mImpl.build(), mFingerprint);
            }
        }
    }

    /** An extensible {@code TextOverflow} property. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class TextOverflowProp {
        private final LayoutElementProto.TextOverflowProp mImpl;
        private final @Nullable Fingerprint mFingerprint;

        TextOverflowProp(
                LayoutElementProto.TextOverflowProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. */
        @TextOverflow
        public int getValue() {
            return mImpl.getValue().getNumber();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull TextOverflowProp fromProto(
                LayoutElementProto.@NonNull TextOverflowProp proto,
                @Nullable Fingerprint fingerprint) {
            return new TextOverflowProp(proto, fingerprint);
        }

        static @NonNull TextOverflowProp fromProto(
                LayoutElementProto.@NonNull TextOverflowProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull TextOverflowProp toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "TextOverflowProp{" + "value=" + getValue() + "}";
        }

        /** Builder for {@link TextOverflowProp} */
        public static final class Builder {
            private final LayoutElementProto.TextOverflowProp.Builder mImpl =
                    LayoutElementProto.TextOverflowProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1542057565);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the value. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setValue(@TextOverflow int value) {
                mImpl.setValue(LayoutElementProto.TextOverflow.forNumber(value));
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull TextOverflowProp build() {
                return new TextOverflowProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** Parameters for Marquee animation. Only applies for TEXT_OVERFLOW_MARQUEE. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    static final class MarqueeParameters {
        private final LayoutElementProto.MarqueeParameters mImpl;
        private final @Nullable Fingerprint mFingerprint;

        MarqueeParameters(
                LayoutElementProto.MarqueeParameters impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the number of times to repeat the Marquee animation. Set to -1 to repeat
         * indefinitely. Defaults to repeat indefinitely.
         */
        @ProtoLayoutExperimental
        public int getIterations() {
            return mImpl.getIterations();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull MarqueeParameters fromProto(
                LayoutElementProto.@NonNull MarqueeParameters proto,
                @Nullable Fingerprint fingerprint) {
            return new MarqueeParameters(proto, fingerprint);
        }

        static @NonNull MarqueeParameters fromProto(
                LayoutElementProto.@NonNull MarqueeParameters proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull MarqueeParameters toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "MarqueeParameters{" + "iterations=" + getIterations() + "}";
        }

        /** Builder for {@link MarqueeParameters} */
        public static final class Builder {
            private final LayoutElementProto.MarqueeParameters.Builder mImpl =
                    LayoutElementProto.MarqueeParameters.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1405971293);

            /** Creates an instance of {@link Builder}. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public Builder() {}

            /**
             * Sets the number of times to repeat the Marquee animation. Set to -1 to repeat
             * indefinitely. Defaults to repeat indefinitely.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @ProtoLayoutExperimental
            public @NonNull Builder setIterations(int iterations) {
                mImpl.setIterations(iterations);
                mFingerprint.recordPropertyUpdate(1, iterations);
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull MarqueeParameters build() {
                return new MarqueeParameters(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A text string. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class Text implements LayoutElement {
        private final LayoutElementProto.Text mImpl;
        private final @Nullable Fingerprint mFingerprint;

        Text(LayoutElementProto.Text impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the text to render.
         *
         * <p>While this field is statically accessible from 1.0, it's only bindable since version
         * 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
         */
        public @Nullable StringProp getText() {
            if (mImpl.hasText()) {
                return StringProp.fromProto(mImpl.getText());
            } else {
                return null;
            }
        }

        /**
         * Gets the style of font to use (size, bold etc). If not specified, defaults to the
         * platform's default body font.
         */
        public @Nullable FontStyle getFontStyle() {
            if (mImpl.hasFontStyle()) {
                return FontStyle.fromProto(mImpl.getFontStyle());
            } else {
                return null;
            }
        }

        /** Gets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element. */
        public @Nullable Modifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return Modifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        /**
         * Gets the maximum number of lines that can be represented by the {@link Text} element. If
         * not defined, the {@link Text} element will be treated as a single-line element.
         */
        public @Nullable Int32Prop getMaxLines() {
            if (mImpl.hasMaxLines()) {
                return Int32Prop.fromProto(mImpl.getMaxLines());
            } else {
                return null;
            }
        }

        /**
         * Gets alignment of the text within its bounds. Note that a {@link Text} element will size
         * itself to wrap its contents, so this option is meaningless for single-line text (for
         * that, use alignment of the outer container). For multi-line text, however, this will set
         * the alignment of lines relative to the {@link Text} element bounds. If not defined,
         * defaults to TEXT_ALIGN_CENTER.
         */
        public @Nullable TextAlignmentProp getMultilineAlignment() {
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
         * TEXT_OVERFLOW_TRUNCATE.
         */
        public @Nullable TextOverflowProp getOverflow() {
            if (mImpl.hasOverflow()) {
                return TextOverflowProp.fromProto(mImpl.getOverflow());
            } else {
                return null;
            }
        }

        /**
         * Gets the explicit height between lines of text. This is equivalent to the vertical
         * distance between subsequent baselines. If not specified, defaults the font's recommended
         * interline spacing.
         */
        public @Nullable SpProp getLineHeight() {
            if (mImpl.hasLineHeight()) {
                return SpProp.fromProto(mImpl.getLineHeight());
            } else {
                return null;
            }
        }

        /**
         * Gets the number of times to repeat the Marquee animation. Only applies when overflow is
         * TEXT_OVERFLOW_MARQUEE. Set to -1 to repeat indefinitely. Defaults to repeat indefinitely.
         */
        @ProtoLayoutExperimental
        @IntRange(from = -1)
        public int getMarqueeIterations() {
            return mImpl.getMarqueeParameters().getIterations();
        }

        /**
         * Gets the bounding constraints for the layout affected by the dynamic value from {@link
         * #getText()}.
         */
        public @Nullable StringLayoutConstraint getLayoutConstraintsForDynamicText() {
            if (mImpl.hasText()) {
                return StringLayoutConstraint.fromProto(mImpl.getText());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull Text fromProto(
                LayoutElementProto.@NonNull Text proto, @Nullable Fingerprint fingerprint) {
            return new Text(proto, fingerprint);
        }

        static @NonNull Text fromProto(LayoutElementProto.@NonNull Text proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        LayoutElementProto.@NonNull Text toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull LayoutElement toLayoutElementProto() {
            return LayoutElementProto.LayoutElement.newBuilder().setText(mImpl).build();
        }

        @Override
        @OptIn(markerClass = ProtoLayoutExperimental.class)
        public @NonNull String toString() {
            return "Text{"
                    + "text="
                    + getText()
                    + ", fontStyle="
                    + getFontStyle()
                    + ", modifiers="
                    + getModifiers()
                    + ", maxLines="
                    + getMaxLines()
                    + ", multilineAlignment="
                    + getMultilineAlignment()
                    + ", overflow="
                    + getOverflow()
                    + ", lineHeight="
                    + getLineHeight()
                    + "}";
        }

        /** Builder for {@link Text}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements LayoutElement.Builder {
            private final LayoutElementProto.Text.Builder mImpl =
                    LayoutElementProto.Text.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(814133697);

            /** Creates an instance of {@link Builder}. */
            public Builder() {
                mImpl.setAndroidTextStyle(
                        LayoutElementProto.AndroidTextStyle.newBuilder()
                                .setExcludeFontPadding(true));
            }

            /**
             * Sets the text to render.
             *
             * <p>While this field is statically accessible from 1.0, it's only bindable since
             * version 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setText(@NonNull StringProp text) {
                mImpl.mergeText(text.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(text.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the style of font to use (size, bold etc). If not specified, defaults to the
             * platform's default body font.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setFontStyle(@NonNull FontStyle fontStyle) {
                mImpl.setFontStyle(fontStyle.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(fontStyle.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setModifiers(@NonNull Modifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the maximum number of lines that can be represented by the {@link Text} element.
             * If not defined, the {@link Text} element will be treated as a single-line element.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setMaxLines(@NonNull Int32Prop maxLines) {
                mImpl.setMaxLines(maxLines.toProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(maxLines.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the maximum number of lines that can be represented by the {@link Text} element.
             * If not defined, the {@link Text} element will be treated as a single-line element.
             */
            public @NonNull Builder setMaxLines(@IntRange(from = 1) int maxLines) {
                return setMaxLines(new Int32Prop.Builder().setValue(maxLines).build());
            }

            /**
             * Sets alignment of the text within its bounds. Note that a {@link Text} element will
             * size itself to wrap its contents, so this option is meaningless for single-line text
             * (for that, use alignment of the outer container). For multi-line text, however, this
             * will set the alignment of lines relative to the {@link Text} element bounds. If not
             * defined, defaults to TEXT_ALIGN_CENTER.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setMultilineAlignment(
                    @NonNull TextAlignmentProp multilineAlignment) {
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
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setMultilineAlignment(@TextAlignment int multilineAlignment) {
                return setMultilineAlignment(
                        new TextAlignmentProp.Builder().setValue(multilineAlignment).build());
            }

            /**
             * Sets how to handle text which overflows the bound of the {@link Text} element. A
             * {@link Text} element will grow as large as possible inside its parent container
             * (while still respecting max_lines); if it cannot grow large enough to render all of
             * its text, the text which cannot fit inside its container will be truncated. If not
             * defined, defaults to TEXT_OVERFLOW_TRUNCATE.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setOverflow(@NonNull TextOverflowProp overflow) {
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
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setOverflow(@TextOverflow int overflow) {
                return setOverflow(new TextOverflowProp.Builder().setValue(overflow).build());
            }

            /**
             * Sets the explicit height between lines of text. This is equivalent to the vertical
             * distance between subsequent baselines. If not specified, defaults the font's
             * recommended interline spacing.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setLineHeight(@NonNull SpProp lineHeight) {
                mImpl.setLineHeight(lineHeight.toProto());
                mFingerprint.recordPropertyUpdate(
                        7, checkNotNull(lineHeight.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the number of times to repeat the Marquee animation. Only applies when overflow
             * is TEXT_OVERFLOW_MARQUEE. Set to -1 to repeat indefinitely. Defaults to repeat
             * indefinitely.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @ProtoLayoutExperimental
            public @NonNull Builder setMarqueeIterations(
                    @IntRange(from = -1) int marqueeIterations) {
                mImpl.setMarqueeParameters(
                        LayoutElementProto.MarqueeParameters.newBuilder()
                                .setIterations(marqueeIterations));
                mFingerprint.recordPropertyUpdate(9, marqueeIterations);
                return this;
            }

            /**
             * Sets the bounding constraints for the layout affected by the dynamic value from
             * {@link #setText(StringProp)}}.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setLayoutConstraintsForDynamicText(
                    @NonNull StringLayoutConstraint stringLayoutConstraint) {
                mImpl.mergeText(stringLayoutConstraint.toProto());
                mFingerprint.recordPropertyUpdate(
                        1,
                        checkNotNull(stringLayoutConstraint.getFingerprint())
                                .aggregateValueAsInt());
                return this;
            }

            /** Sets the static text to render. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setText(@NonNull String text) {
                return setText(new StringProp.Builder(text).build());
            }

            @Override
            public @NonNull Text build() {
                return new Text(mImpl.build(), mFingerprint);
            }
        }
    }

    /** An extensible {@code ContentScaleMode} property. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class ContentScaleModeProp {
        private final LayoutElementProto.ContentScaleModeProp mImpl;
        private final @Nullable Fingerprint mFingerprint;

        ContentScaleModeProp(
                LayoutElementProto.ContentScaleModeProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. */
        @ContentScaleMode
        public int getValue() {
            return mImpl.getValue().getNumber();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull ContentScaleModeProp fromProto(
                LayoutElementProto.@NonNull ContentScaleModeProp proto,
                @Nullable Fingerprint fingerprint) {
            return new ContentScaleModeProp(proto, fingerprint);
        }

        static @NonNull ContentScaleModeProp fromProto(
                LayoutElementProto.@NonNull ContentScaleModeProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull ContentScaleModeProp toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "ContentScaleModeProp{" + "value=" + getValue() + "}";
        }

        /** Builder for {@link ContentScaleModeProp} */
        public static final class Builder {
            private final LayoutElementProto.ContentScaleModeProp.Builder mImpl =
                    LayoutElementProto.ContentScaleModeProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1200564005);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the value. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setValue(@ContentScaleMode int value) {
                mImpl.setValue(LayoutElementProto.ContentScaleMode.forNumber(value));
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull ContentScaleModeProp build() {
                return new ContentScaleModeProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** Filtering parameters used for images. This can be used to apply a color tint to images. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class ColorFilter {
        private final LayoutElementProto.ColorFilter mImpl;
        private final @Nullable Fingerprint mFingerprint;

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
         * tinted, and this property will have no effect.
         *
         * <p>While this field is statically accessible from 1.0, it's only bindable since version
         * 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
         */
        public @Nullable ColorProp getTint() {
            if (mImpl.hasTint()) {
                return ColorProp.fromProto(mImpl.getTint());
            } else {
                return null;
            }
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull ColorFilter fromProto(
                LayoutElementProto.@NonNull ColorFilter proto, @Nullable Fingerprint fingerprint) {
            return new ColorFilter(proto, fingerprint);
        }

        static @NonNull ColorFilter fromProto(LayoutElementProto.@NonNull ColorFilter proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull ColorFilter toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "ColorFilter{" + "tint=" + getTint() + "}";
        }

        /** Builder for {@link ColorFilter} */
        public static final class Builder {
            private final LayoutElementProto.ColorFilter.Builder mImpl =
                    LayoutElementProto.ColorFilter.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1912021459);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the tint color to use. If specified, the image will be tinted, using SRC_IN
             * blending (that is, all color information will be stripped from the target image, and
             * only the alpha channel will be blended with the requested color).
             *
             * <p>Note that only Android image resources can be tinted; Inline images will not be
             * tinted, and this property will have no effect.
             *
             * <p>While this field is statically accessible from 1.0, it's only bindable since
             * version 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setTint(@NonNull ColorProp tint) {
                mImpl.setTint(tint.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(tint.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull ColorFilter build() {
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
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class Image implements LayoutElement {
        private final LayoutElementProto.Image mImpl;
        private final @Nullable Fingerprint mFingerprint;

        Image(LayoutElementProto.Image impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the resource_id of the image to render. This must exist in the supplied resource
         * bundle.
         */
        public @Nullable StringProp getResourceId() {
            if (mImpl.hasResourceId()) {
                return StringProp.fromProto(mImpl.getResourceId());
            } else {
                return null;
            }
        }

        /** Gets the width of this image. If not defined, the image will not be rendered. */
        public @Nullable ImageDimension getWidth() {
            if (mImpl.hasWidth()) {
                return DimensionBuilders.imageDimensionFromProto(mImpl.getWidth());
            } else {
                return null;
            }
        }

        /** Gets the height of this image. If not defined, the image will not be rendered. */
        public @Nullable ImageDimension getHeight() {
            if (mImpl.hasHeight()) {
                return DimensionBuilders.imageDimensionFromProto(mImpl.getHeight());
            } else {
                return null;
            }
        }

        /**
         * Gets how to scale the image resource inside the bounds specified by width/height if its
         * size does not match those bounds. Defaults to CONTENT_SCALE_MODE_FIT.
         */
        public @Nullable ContentScaleModeProp getContentScaleMode() {
            if (mImpl.hasContentScaleMode()) {
                return ContentScaleModeProp.fromProto(mImpl.getContentScaleMode());
            } else {
                return null;
            }
        }

        /** Gets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element. */
        public @Nullable Modifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return Modifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        /** Gets filtering parameters for this image. If not specified, defaults to no filtering. */
        public @Nullable ColorFilter getColorFilter() {
            if (mImpl.hasColorFilter()) {
                return ColorFilter.fromProto(mImpl.getColorFilter());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull Image fromProto(
                LayoutElementProto.@NonNull Image proto, @Nullable Fingerprint fingerprint) {
            return new Image(proto, fingerprint);
        }

        static @NonNull Image fromProto(LayoutElementProto.@NonNull Image proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        LayoutElementProto.@NonNull Image toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull LayoutElement toLayoutElementProto() {
            return LayoutElementProto.LayoutElement.newBuilder().setImage(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "Image{"
                    + "resourceId="
                    + getResourceId()
                    + ", width="
                    + getWidth()
                    + ", height="
                    + getHeight()
                    + ", contentScaleMode="
                    + getContentScaleMode()
                    + ", modifiers="
                    + getModifiers()
                    + ", colorFilter="
                    + getColorFilter()
                    + "}";
        }

        /** Builder for {@link Image}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements LayoutElement.Builder {
            private final LayoutElementProto.Image.Builder mImpl =
                    LayoutElementProto.Image.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-48009959);

            /** Sets the width of this image. If not defined, the image will not be rendered. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setWidth(@NonNull ImageDimension width) {
                mImpl.setWidth(width.toImageDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(width.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the height of this image. If not defined, the image will not be rendered. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setHeight(@NonNull ImageDimension height) {
                mImpl.setHeight(height.toImageDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(height.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets how to scale the image resource inside the bounds specified by width/height if
             * its size does not match those bounds. Defaults to CONTENT_SCALE_MODE_FIT.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setContentScaleMode(
                    @NonNull ContentScaleModeProp contentScaleMode) {
                mImpl.setContentScaleMode(contentScaleMode.toProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(contentScaleMode.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets how to scale the image resource inside the bounds specified by width/height if
             * its size does not match those bounds. Defaults to CONTENT_SCALE_MODE_FIT.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setContentScaleMode(@ContentScaleMode int contentScaleMode) {
                return setContentScaleMode(
                        new ContentScaleModeProp.Builder().setValue(contentScaleMode).build());
            }

            /**
             * Sets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setModifiers(@NonNull Modifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        5, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets filtering parameters for this image. If not specified, defaults to no filtering.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setColorFilter(@NonNull ColorFilter colorFilter) {
                mImpl.setColorFilter(colorFilter.toProto());
                mFingerprint.recordPropertyUpdate(
                        6, checkNotNull(colorFilter.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            private @Nullable ProtoLayoutScope mScope;
            private boolean mIsResourceIdApiUsed = false;
            private boolean mIsImageResourceApiUsed = false;

            /**
             * Creates an instance of {@link Builder}.
             *
             * <p>Note that, when using this constructor, it should be paired with {@link
             * #setResourceId}, and resource used for it needs to be manually registered in {@code
             * TileService#onTileResourcesRequest} for Tiles. This constructor can't be mixed with
             * {@link #setImageResource}, otherwise an exception will be thrown.
             *
             * @deprecated Use {@link #Builder(ProtoLayoutScope)} constructor which supports
             *     automatic resource registration, paired with {@link #setImageResource}.
             */
            @Deprecated
            public Builder() {}

            /**
             * Creates an instance of {@link Builder} with automatic resource registration used.
             *
             * <p>Note that, when using this constructor, it should be paired with {@link
             * #setImageResource}. Additionally, {@code Resources} object shouldn't be provided in
             * {@code TileService#onTileResourcesRequest} method for your Tile as resources would be
             * automatically registered. This constructor can't be mixed with {@link
             * #setResourceId}, otherwise an exception will be thrown.
             *
             * <p>When using this constructor and automatic resource registration, there's no need
             * to provide resources version in {@code Tile.Builder.setResourcesVersion}, as {@link
             * ProtoLayoutScope} will handle versioning and changes of the resources automatically.
             * However, setting custom version in {@code Tile.Builder.setResourcesVersion} is still
             * supported.
             */
            public Builder(@NonNull ProtoLayoutScope scope) {
                this.mScope = scope;
            }

            /**
             * Sets the specific resource of the image to render. This method will automatically
             * assign the ID and add the given resource in the resources bundle.
             *
             * @param resource An Image resource, used in the layout in the place of this {@link
             *     Image} element.
             * @throws IllegalStateException if this method is called without {@link
             *     #Builder(ProtoLayoutScope)} or after {@link #setResourceId} was already called.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            @SuppressWarnings("MissingGetterMatchingBuilder")
            public @NonNull Builder setImageResource(@NonNull ImageResource resource) {
                return setImageResource(resource, String.valueOf(resource.hashCode()));
            }

            /**
             * Sets the specific resource of the image to render. This method will automatically add
             * the given resource in the resources bundle.
             *
             * @param resource An Image resource, used in the layout in the place of this {@link
             *     Image} element.
             * @param resourceId The ID of the resource
             * @throws IllegalStateException if this method is called without {@link
             *     #Builder(ProtoLayoutScope)} or after {@link #setResourceId} was already called.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            @SuppressWarnings("MissingGetterMatchingBuilder")
            public @NonNull Builder setImageResource(
                    @NonNull ImageResource resource, @NonNull String resourceId) {
                if (mIsResourceIdApiUsed) {
                    throw new IllegalStateException(
                            "Image.Builder.setImageResource can't be mixed with setResourceId"
                                    + " method.");
                }
                if (mScope == null) {
                    throw new IllegalStateException(
                            "Image.Builder.setImageResource needs to be called with constructor"
                                    + " that accepts ProtoLayoutScope.");
                }
                mIsImageResourceApiUsed = true;
                setResourceIdInternal(new StringProp.Builder(resourceId).build());
                mScope.registerResource(resourceId, resource);
                return this;
            }

            /**
             * Sets the resource_id of the image to render. This must exist in the supplied resource
             * bundle.
             *
             * @throws IllegalStateException if this method is called with {@link
             *     #Builder(ProtoLayoutScope)} or after {@link #setImageResource(ImageResource)} was
             *     already called.
             * @deprecated Use {@link #setImageResource} paired with {@link
             *     #Builder(ProtoLayoutScope)} for automatic resource registration.
             */
            @Deprecated
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setResourceId(@NonNull String resourceId) {
                return setResourceId(new StringProp.Builder(resourceId).build());
            }

            /**
             * Sets the resource_id of the image to render. This must exist in the supplied resource
             * bundle.
             *
             * <p>Note that this field only supports static values.
             *
             * @throws IllegalStateException if this method is called with {@link
             *     #Builder(ProtoLayoutScope)} or after {@link #setImageResource(ImageResource)} was
             *     already called.
             * @deprecated Use {@link #setImageResource} paired with {@link
             *     #Builder(ProtoLayoutScope)} for automatic resource registration.
             */
            @Deprecated
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setResourceId(@NonNull StringProp resourceId) {
                if (mScope != null || mIsImageResourceApiUsed) {
                    throw new IllegalStateException(
                            "Image.Builder.setResourceId can't be mixed with constructor that"
                                    + " accepts ProtoLayoutScope or with setImageResource.");
                }
                mIsResourceIdApiUsed = true;
                return setResourceIdInternal(resourceId);
            }

            /**
             * Sets the resource_id of the image to the proto.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            private @NonNull Builder setResourceIdInternal(@NonNull StringProp resourceId) {
                if (resourceId.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "Image.Builder.setResourceId doesn't support dynamic values.");
                }
                mImpl.setResourceId(resourceId.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(resourceId.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            public @NonNull Image build() {
                return new Image(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A simple spacer, typically used to provide padding between adjacent elements. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class Spacer implements LayoutElement {
        private final LayoutElementProto.Spacer mImpl;
        private final @Nullable Fingerprint mFingerprint;

        Spacer(LayoutElementProto.Spacer impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the width of this {@link Spacer}. When this is added as the direct child of an
         * {@link Arc}, this must be specified as an angular dimension, otherwise a linear dimension
         * must be used. If not defined, defaults to 0.
         *
         * <p>While this field is statically accessible from 1.0, it's only bindable since version
         * 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
         */
        public @Nullable SpacerDimension getWidth() {
            if (mImpl.hasWidth()) {
                return DimensionBuilders.spacerDimensionFromProto(mImpl.getWidth());
            } else {
                return null;
            }
        }

        /**
         * Gets the height of this spacer. If not defined, defaults to 0.
         *
         * <p>While this field is statically accessible from 1.0, it's only bindable since version
         * 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
         */
        public @Nullable SpacerDimension getHeight() {
            if (mImpl.hasHeight()) {
                return DimensionBuilders.spacerDimensionFromProto(mImpl.getHeight());
            } else {
                return null;
            }
        }

        /** Gets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element. */
        public @Nullable Modifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return Modifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        /**
         * Gets the bounding constraints for the layout affected by the dynamic value from {@link
         * #getWidth()}.
         */
        public @Nullable HorizontalLayoutConstraint getLayoutConstraintsForDynamicWidth() {
            if (mImpl.getWidth().hasLinearDimension()) {
                return HorizontalLayoutConstraint.fromProto(mImpl.getWidth().getLinearDimension());
            } else {
                return null;
            }
        }

        /**
         * Gets the bounding constraints for the layout affected by the dynamic value from {@link
         * #getHeight()}.
         */
        public @Nullable VerticalLayoutConstraint getLayoutConstraintsForDynamicHeight() {
            if (mImpl.getHeight().hasLinearDimension()) {
                return VerticalLayoutConstraint.fromProto(mImpl.getHeight().getLinearDimension());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull Spacer fromProto(
                LayoutElementProto.@NonNull Spacer proto, @Nullable Fingerprint fingerprint) {
            return new Spacer(proto, fingerprint);
        }

        static @NonNull Spacer fromProto(LayoutElementProto.@NonNull Spacer proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        LayoutElementProto.@NonNull Spacer toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull LayoutElement toLayoutElementProto() {
            return LayoutElementProto.LayoutElement.newBuilder().setSpacer(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "Spacer{"
                    + "width="
                    + getWidth()
                    + ", height="
                    + getHeight()
                    + ", modifiers="
                    + getModifiers()
                    + "}";
        }

        /** Builder for {@link Spacer}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements LayoutElement.Builder {
            private final LayoutElementProto.Spacer.Builder mImpl =
                    LayoutElementProto.Spacer.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-156449821);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the width of this {@link Spacer}. When this is added as the direct child of an
             * {@link Arc}, this must be specified as an angular dimension, otherwise a linear
             * dimension must be used. If not defined, defaults to 0.
             *
             * <p>While this field is statically accessible from 1.0, it's only bindable since
             * version 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setWidth(@NonNull SpacerDimension width) {
                mImpl.mergeWidth(width.toSpacerDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(width.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the bounding constraints for the layout affected by the dynamic value from
             * {@link #setWidth(SpacerDimension)}. If the {@link SpacerDimension} does not have a
             * dynamic value, this will be ignored.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setLayoutConstraintsForDynamicWidth(
                    @NonNull HorizontalLayoutConstraint horizontalLayoutConstraint) {
                switch (mImpl.getWidth().getInnerCase()) {
                    case INNER_NOT_SET:
                    case LINEAR_DIMENSION:
                        mImpl.mergeWidth(horizontalLayoutConstraint.toSpacerDimensionProto());
                        mFingerprint.recordPropertyUpdate(
                                1,
                                checkNotNull(horizontalLayoutConstraint.getFingerprint())
                                        .aggregateValueAsInt());
                        break;
                    default:
                }
                return this;
            }

            /**
             * Sets the height of this spacer. If not defined, defaults to 0.
             *
             * <p>While this field is statically accessible from 1.0, it's only bindable since
             * version 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setHeight(@NonNull SpacerDimension height) {
                mImpl.setHeight(height.toSpacerDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(height.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the bounding constraints for the layout affected by the dynamic value from
             * {@link #setHeight(SpacerDimension)}. If the {@link SpacerDimension} does not have a
             * dynamic value, this will be ignored.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setLayoutConstraintsForDynamicHeight(
                    @NonNull VerticalLayoutConstraint verticalLayoutConstraint) {
                switch (mImpl.getHeight().getInnerCase()) {
                    case INNER_NOT_SET:
                    case LINEAR_DIMENSION:
                        mImpl.mergeHeight(verticalLayoutConstraint.toSpacerDimensionProto());
                        mFingerprint.recordPropertyUpdate(
                                2,
                                checkNotNull(verticalLayoutConstraint.getFingerprint())
                                        .aggregateValueAsInt());
                        break;
                    default:
                }
                return this;
            }

            /**
             * Sets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setModifiers(@NonNull Modifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            public @NonNull Spacer build() {
                return new Spacer(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A container which stacks all of its children on top of one another. This also allows to add a
     * background color, or to have a border around them with some padding.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class Box implements LayoutElement {
        private final LayoutElementProto.Box mImpl;
        private final @Nullable Fingerprint mFingerprint;

        Box(LayoutElementProto.Box impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the child element(s) to wrap. */
        public @NonNull List<LayoutElement> getContents() {
            List<LayoutElement> list = new ArrayList<>();
            for (LayoutElementProto.LayoutElement item : mImpl.getContentsList()) {
                list.add(LayoutElementBuilders.layoutElementFromProto(item));
            }
            return Collections.unmodifiableList(list);
        }

        /**
         * Gets the height of this {@link Box}. If not defined, this will size itself to fit all of
         * its children (i.e. a WrappedDimension).
         */
        public @Nullable ContainerDimension getHeight() {
            if (mImpl.hasHeight()) {
                return DimensionBuilders.containerDimensionFromProto(mImpl.getHeight());
            } else {
                return null;
            }
        }

        /**
         * Gets the width of this {@link Box}. If not defined, this will size itself to fit all of
         * its children (i.e. a WrappedDimension).
         */
        public @Nullable ContainerDimension getWidth() {
            if (mImpl.hasWidth()) {
                return DimensionBuilders.containerDimensionFromProto(mImpl.getWidth());
            } else {
                return null;
            }
        }

        /**
         * Gets the horizontal alignment of the element inside this {@link Box}. If not defined,
         * defaults to HORIZONTAL_ALIGN_CENTER.
         */
        public @Nullable HorizontalAlignmentProp getHorizontalAlignment() {
            if (mImpl.hasHorizontalAlignment()) {
                return HorizontalAlignmentProp.fromProto(mImpl.getHorizontalAlignment());
            } else {
                return null;
            }
        }

        /**
         * Gets the vertical alignment of the element inside this {@link Box}. If not defined,
         * defaults to VERTICAL_ALIGN_CENTER.
         */
        public @Nullable VerticalAlignmentProp getVerticalAlignment() {
            if (mImpl.hasVerticalAlignment()) {
                return VerticalAlignmentProp.fromProto(mImpl.getVerticalAlignment());
            } else {
                return null;
            }
        }

        /** Gets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element. */
        public @Nullable Modifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return Modifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull Box fromProto(
                LayoutElementProto.@NonNull Box proto, @Nullable Fingerprint fingerprint) {
            return new Box(proto, fingerprint);
        }

        static @NonNull Box fromProto(LayoutElementProto.@NonNull Box proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        LayoutElementProto.@NonNull Box toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull LayoutElement toLayoutElementProto() {
            return LayoutElementProto.LayoutElement.newBuilder().setBox(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "Box{"
                    + "contents="
                    + getContents()
                    + ", height="
                    + getHeight()
                    + ", width="
                    + getWidth()
                    + ", horizontalAlignment="
                    + getHorizontalAlignment()
                    + ", verticalAlignment="
                    + getVerticalAlignment()
                    + ", modifiers="
                    + getModifiers()
                    + "}";
        }

        /** Builder for {@link Box}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements LayoutElement.Builder {
            private final LayoutElementProto.Box.Builder mImpl =
                    LayoutElementProto.Box.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-2113485818);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Adds one item to the child element(s) to wrap. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder addContent(@NonNull LayoutElement content) {
                mImpl.addContents(content.toLayoutElementProto());
                mFingerprint.addChildNode(checkNotNull(content.getFingerprint()));
                return this;
            }

            /**
             * Sets the height of this {@link Box}. If not defined, this will size itself to fit all
             * of its children (i.e. a WrappedDimension).
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setHeight(@NonNull ContainerDimension height) {
                mImpl.setHeight(height.toContainerDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(height.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the width of this {@link Box}. If not defined, this will size itself to fit all
             * of its children (i.e. a WrappedDimension).
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setWidth(@NonNull ContainerDimension width) {
                mImpl.setWidth(width.toContainerDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(width.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the horizontal alignment of the element inside this {@link Box}. If not defined,
             * defaults to HORIZONTAL_ALIGN_CENTER.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setHorizontalAlignment(
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
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setHorizontalAlignment(
                    @HorizontalAlignment int horizontalAlignment) {
                return setHorizontalAlignment(
                        new HorizontalAlignmentProp.Builder()
                                .setValue(horizontalAlignment)
                                .build());
            }

            /**
             * Sets the vertical alignment of the element inside this {@link Box}. If not defined,
             * defaults to VERTICAL_ALIGN_CENTER.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setVerticalAlignment(
                    @NonNull VerticalAlignmentProp verticalAlignment) {
                mImpl.setVerticalAlignment(verticalAlignment.toProto());
                mFingerprint.recordPropertyUpdate(
                        5, checkNotNull(verticalAlignment.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the vertical alignment of the element inside this {@link Box}. If not defined,
             * defaults to VERTICAL_ALIGN_CENTER.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setVerticalAlignment(@VerticalAlignment int verticalAlignment) {
                return setVerticalAlignment(
                        new VerticalAlignmentProp.Builder().setValue(verticalAlignment).build());
            }

            /**
             * Sets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setModifiers(@NonNull Modifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        6, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull Box build() {
                return new Box(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A portion of text which can be added to a {@link Span}. Two different {@link SpanText}
     * elements on the same line will be aligned to the same baseline, regardless of the size of
     * each {@link SpanText}.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class SpanText implements Span {
        private final LayoutElementProto.SpanText mImpl;
        private final @Nullable Fingerprint mFingerprint;

        SpanText(LayoutElementProto.SpanText impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the text to render. */
        public @Nullable StringProp getText() {
            if (mImpl.hasText()) {
                return StringProp.fromProto(mImpl.getText());
            } else {
                return null;
            }
        }

        /**
         * Gets the style of font to use (size, bold etc). If not specified, defaults to the
         * platform's default body font.
         */
        public @Nullable FontStyle getFontStyle() {
            if (mImpl.hasFontStyle()) {
                return FontStyle.fromProto(mImpl.getFontStyle());
            } else {
                return null;
            }
        }

        /** Gets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element. */
        public @Nullable SpanModifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return SpanModifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull SpanText fromProto(
                LayoutElementProto.@NonNull SpanText proto, @Nullable Fingerprint fingerprint) {
            return new SpanText(proto, fingerprint);
        }

        static @NonNull SpanText fromProto(LayoutElementProto.@NonNull SpanText proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        LayoutElementProto.@NonNull SpanText toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull Span toSpanProto() {
            return LayoutElementProto.Span.newBuilder().setText(mImpl).build();
        }

        @Override
        @OptIn(markerClass = ProtoLayoutExperimental.class)
        public @NonNull String toString() {
            return "SpanText{"
                    + "text="
                    + getText()
                    + ", fontStyle="
                    + getFontStyle()
                    + ", modifiers="
                    + getModifiers()
                    + "}";
        }

        /** Builder for {@link SpanText}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements Span.Builder {
            private final LayoutElementProto.SpanText.Builder mImpl =
                    LayoutElementProto.SpanText.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(266451531);

            /** Creates an instance of {@link Builder}. */
            public Builder() {
                mImpl.setAndroidTextStyle(
                        LayoutElementProto.AndroidTextStyle.newBuilder()
                                .setExcludeFontPadding(true));
            }

            /**
             * Sets the text to render.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setText(@NonNull StringProp text) {
                if (text.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "SpanText.Builder.setText doesn't support dynamic values.");
                }
                mImpl.setText(text.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(text.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the text to render. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setText(@NonNull String text) {
                return setText(new StringProp.Builder(text).build());
            }

            /**
             * Sets the style of font to use (size, bold etc). If not specified, defaults to the
             * platform's default body font.
             *
             * <p>DynamicColor is not supported for SpanText.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setFontStyle(@NonNull FontStyle fontStyle) {
                ColorProp colorProp = fontStyle.getColor();
                if (colorProp != null && colorProp.getDynamicValue() != null) {
                    throw new IllegalArgumentException("SpanText does not support DynamicColor.");
                }
                mImpl.setFontStyle(fontStyle.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(fontStyle.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setModifiers(@NonNull SpanModifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull SpanText build() {
                return new SpanText(mImpl.build(), mFingerprint);
            }
        }
    }

    /** An image which can be added to a {@link Span}. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class SpanImage implements Span {
        private final LayoutElementProto.SpanImage mImpl;
        private final @Nullable Fingerprint mFingerprint;

        SpanImage(LayoutElementProto.SpanImage impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the resource_id of the image to render. This must exist in the supplied resource
         * bundle.
         */
        public @Nullable StringProp getResourceId() {
            if (mImpl.hasResourceId()) {
                return StringProp.fromProto(mImpl.getResourceId());
            } else {
                return null;
            }
        }

        /** Gets the width of this image. If not defined, the image will not be rendered. */
        public @Nullable DpProp getWidth() {
            if (mImpl.hasWidth()) {
                return DpProp.fromProto(mImpl.getWidth());
            } else {
                return null;
            }
        }

        /** Gets the height of this image. If not defined, the image will not be rendered. */
        public @Nullable DpProp getHeight() {
            if (mImpl.hasHeight()) {
                return DpProp.fromProto(mImpl.getHeight());
            } else {
                return null;
            }
        }

        /** Gets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element. */
        public @Nullable SpanModifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return SpanModifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        /**
         * Gets alignment of this image within the line height of the surrounding {@link Spannable}.
         * If undefined, defaults to SPAN_VERTICAL_ALIGN_BOTTOM.
         */
        public @Nullable SpanVerticalAlignmentProp getAlignment() {
            if (mImpl.hasAlignment()) {
                return SpanVerticalAlignmentProp.fromProto(mImpl.getAlignment());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull SpanImage fromProto(
                LayoutElementProto.@NonNull SpanImage proto, @Nullable Fingerprint fingerprint) {
            return new SpanImage(proto, fingerprint);
        }

        static @NonNull SpanImage fromProto(LayoutElementProto.@NonNull SpanImage proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        LayoutElementProto.@NonNull SpanImage toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull Span toSpanProto() {
            return LayoutElementProto.Span.newBuilder().setImage(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "SpanImage{"
                    + "resourceId="
                    + getResourceId()
                    + ", width="
                    + getWidth()
                    + ", height="
                    + getHeight()
                    + ", modifiers="
                    + getModifiers()
                    + ", alignment="
                    + getAlignment()
                    + "}";
        }

        /** Builder for {@link SpanImage}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements Span.Builder {
            private final LayoutElementProto.SpanImage.Builder mImpl =
                    LayoutElementProto.SpanImage.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(920832637);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the resource_id of the image to render. This must exist in the supplied resource
             * bundle.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setResourceId(@NonNull StringProp resourceId) {
                if (resourceId.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "SpanImage.Builder.setResourceId doesn't support dynamic values.");
                }
                mImpl.setResourceId(resourceId.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(resourceId.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the resource_id of the image to render. This must exist in the supplied resource
             * bundle.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setResourceId(@NonNull String resourceId) {
                return setResourceId(new StringProp.Builder(resourceId).build());
            }

            /**
             * Sets the width of this image. If not defined, the image will not be rendered.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setWidth(@NonNull DpProp width) {
                if (width.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "SpanImage.Builder.setWidth doesn't support dynamic values.");
                }
                mImpl.setWidth(width.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(width.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the height of this image. If not defined, the image will not be rendered.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setHeight(@NonNull DpProp height) {
                if (height.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "SpanImage.Builder.setHeight doesn't support dynamic values.");
                }
                mImpl.setHeight(height.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(height.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setModifiers(@NonNull SpanModifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets alignment of this image within the line height of the surrounding {@link
             * Spannable}. If undefined, defaults to SPAN_VERTICAL_ALIGN_BOTTOM.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setAlignment(@NonNull SpanVerticalAlignmentProp alignment) {
                mImpl.setAlignment(alignment.toProto());
                mFingerprint.recordPropertyUpdate(
                        5, checkNotNull(alignment.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets alignment of this image within the line height of the surrounding {@link
             * Spannable}. If undefined, defaults to SPAN_VERTICAL_ALIGN_BOTTOM.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setAlignment(@SpanVerticalAlignment int alignment) {
                return setAlignment(
                        new SpanVerticalAlignmentProp.Builder().setValue(alignment).build());
            }

            @Override
            public @NonNull SpanImage build() {
                return new SpanImage(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * Interface defining a single {@link Span}. Each {@link Span} forms part of a larger {@link
     * Spannable} widget. At the moment, the only widgets which can be added to {@link Spannable}
     * containers are {@link SpanText} and {@link SpanImage} elements.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public interface Span {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        LayoutElementProto.@NonNull Span toSpanProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable Fingerprint getFingerprint();

        /** Builder to create {@link Span} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull Span build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static @NonNull Span spanFromProto(
            LayoutElementProto.@NonNull Span proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasText()) {
            return SpanText.fromProto(proto.getText(), fingerprint);
        }
        if (proto.hasImage()) {
            return SpanImage.fromProto(proto.getImage(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of Span");
    }

    static @NonNull Span spanFromProto(LayoutElementProto.@NonNull Span proto) {
        return spanFromProto(proto, null);
    }

    /**
     * A container of {@link Span} elements. Currently, this supports {@link SpanImage} and {@link
     * SpanText} elements, where each individual {@link Span} can have different styling applied to
     * it but the resulting text will flow naturally. This allows sections of a paragraph of text to
     * have different styling applied to it, for example, making one or two words bold or italic.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class Spannable implements LayoutElement {
        private final LayoutElementProto.Spannable mImpl;
        private final @Nullable Fingerprint mFingerprint;

        Spannable(LayoutElementProto.Spannable impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the {@link Span} elements that form this {@link Spannable}. */
        public @NonNull List<Span> getSpans() {
            List<Span> list = new ArrayList<>();
            for (LayoutElementProto.Span item : mImpl.getSpansList()) {
                list.add(LayoutElementBuilders.spanFromProto(item));
            }
            return Collections.unmodifiableList(list);
        }

        /** Gets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element. */
        public @Nullable Modifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return Modifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        /**
         * Gets the maximum number of lines that can be represented by the {@link Spannable}
         * element. If not defined, the {@link Spannable} element will be treated as a single-line
         * element.
         */
        public @Nullable Int32Prop getMaxLines() {
            if (mImpl.hasMaxLines()) {
                return Int32Prop.fromProto(mImpl.getMaxLines());
            } else {
                return null;
            }
        }

        /**
         * Gets alignment of the {@link Spannable} content within its bounds. Note that a {@link
         * Spannable} element will size itself to wrap its contents, so this option is meaningless
         * for single-line content (for that, use alignment of the outer container). For multi-line
         * content, however, this will set the alignment of lines relative to the {@link Spannable}
         * element bounds. If not defined, defaults to TEXT_ALIGN_CENTER.
         */
        public @Nullable HorizontalAlignmentProp getMultilineAlignment() {
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
         * defined, defaults to TEXT_OVERFLOW_TRUNCATE.
         */
        public @Nullable TextOverflowProp getOverflow() {
            if (mImpl.hasOverflow()) {
                return TextOverflowProp.fromProto(mImpl.getOverflow());
            } else {
                return null;
            }
        }

        /**
         * Gets the explicit height between lines of text. This is equivalent to the vertical
         * distance between subsequent baselines. If not specified, defaults the font's recommended
         * interline spacing.
         */
        public @Nullable SpProp getLineHeight() {
            if (mImpl.hasLineHeight()) {
                return SpProp.fromProto(mImpl.getLineHeight());
            } else {
                return null;
            }
        }

        /**
         * Gets the number of times to repeat the Marquee animation. Only applies when overflow is
         * TEXT_OVERFLOW_MARQUEE. Set to -1 to repeat indefinitely. Defaults to repeat indefinitely.
         */
        @ProtoLayoutExperimental
        @IntRange(from = -1)
        public int getMarqueeIterations() {
            return mImpl.getMarqueeParameters().getIterations();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull Spannable fromProto(
                LayoutElementProto.@NonNull Spannable proto, @Nullable Fingerprint fingerprint) {
            return new Spannable(proto, fingerprint);
        }

        static @NonNull Spannable fromProto(LayoutElementProto.@NonNull Spannable proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        LayoutElementProto.@NonNull Spannable toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull LayoutElement toLayoutElementProto() {
            return LayoutElementProto.LayoutElement.newBuilder().setSpannable(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "Spannable{"
                    + "spans="
                    + getSpans()
                    + ", modifiers="
                    + getModifiers()
                    + ", maxLines="
                    + getMaxLines()
                    + ", multilineAlignment="
                    + getMultilineAlignment()
                    + ", overflow="
                    + getOverflow()
                    + ", lineHeight="
                    + getLineHeight()
                    + "}";
        }

        /** Builder for {@link Spannable}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements LayoutElement.Builder {
            private final LayoutElementProto.Spannable.Builder mImpl =
                    LayoutElementProto.Spannable.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1111684471);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Adds one item to the {@link Span} elements that form this {@link Spannable}. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder addSpan(@NonNull Span span) {
                mImpl.addSpans(span.toSpanProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(span.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setModifiers(@NonNull Modifiers modifiers) {
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
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setMaxLines(@NonNull Int32Prop maxLines) {
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
            public @NonNull Builder setMaxLines(@IntRange(from = 1) int maxLines) {
                return setMaxLines(new Int32Prop.Builder().setValue(maxLines).build());
            }

            /**
             * Sets alignment of the {@link Spannable} content within its bounds. Note that a {@link
             * Spannable} element will size itself to wrap its contents, so this option is
             * meaningless for single-line content (for that, use alignment of the outer container).
             * For multi-line content, however, this will set the alignment of lines relative to the
             * {@link Spannable} element bounds. If not defined, defaults to TEXT_ALIGN_CENTER.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setMultilineAlignment(
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
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setMultilineAlignment(
                    @HorizontalAlignment int multilineAlignment) {
                return setMultilineAlignment(
                        new HorizontalAlignmentProp.Builder().setValue(multilineAlignment).build());
            }

            /**
             * Sets how to handle content which overflows the bound of the {@link Spannable}
             * element. A {@link Spannable} element will grow as large as possible inside its parent
             * container (while still respecting max_lines); if it cannot grow large enough to
             * render all of its content, the content which cannot fit inside its container will be
             * truncated. If not defined, defaults to TEXT_OVERFLOW_TRUNCATE.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setOverflow(@NonNull TextOverflowProp overflow) {
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
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setOverflow(@TextOverflow int overflow) {
                return setOverflow(new TextOverflowProp.Builder().setValue(overflow).build());
            }

            /**
             * Sets the explicit height between lines of text. This is equivalent to the vertical
             * distance between subsequent baselines. If not specified, defaults the font's
             * recommended interline spacing.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setLineHeight(@NonNull SpProp lineHeight) {
                mImpl.setLineHeight(lineHeight.toProto());
                mFingerprint.recordPropertyUpdate(
                        7, checkNotNull(lineHeight.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the number of times to repeat the Marquee animation. Only applies when overflow
             * is TEXT_OVERFLOW_MARQUEE. Set to -1 to repeat indefinitely. Defaults to repeat
             * indefinitely.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @ProtoLayoutExperimental
            public @NonNull Builder setMarqueeIterations(
                    @IntRange(from = -1) int marqueeIterations) {
                mImpl.setMarqueeParameters(
                        LayoutElementProto.MarqueeParameters.newBuilder()
                                .setIterations(marqueeIterations));
                mFingerprint.recordPropertyUpdate(8, marqueeIterations);
                return this;
            }

            @Override
            public @NonNull Spannable build() {
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
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class Column implements LayoutElement {
        private final LayoutElementProto.Column mImpl;
        private final @Nullable Fingerprint mFingerprint;

        Column(LayoutElementProto.Column impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the list of child elements to place inside this {@link Column}. */
        public @NonNull List<LayoutElement> getContents() {
            List<LayoutElement> list = new ArrayList<>();
            for (LayoutElementProto.LayoutElement item : mImpl.getContentsList()) {
                list.add(LayoutElementBuilders.layoutElementFromProto(item));
            }
            return Collections.unmodifiableList(list);
        }

        /**
         * Gets the horizontal alignment of elements inside this column, if they are narrower than
         * the resulting width of the column. If not defined, defaults to HORIZONTAL_ALIGN_CENTER.
         */
        public @Nullable HorizontalAlignmentProp getHorizontalAlignment() {
            if (mImpl.hasHorizontalAlignment()) {
                return HorizontalAlignmentProp.fromProto(mImpl.getHorizontalAlignment());
            } else {
                return null;
            }
        }

        /**
         * Gets the width of this column. If not defined, this will size itself to fit all of its
         * children (i.e. a WrappedDimension).
         */
        public @Nullable ContainerDimension getWidth() {
            if (mImpl.hasWidth()) {
                return DimensionBuilders.containerDimensionFromProto(mImpl.getWidth());
            } else {
                return null;
            }
        }

        /**
         * Gets the height of this column. If not defined, this will size itself to fit all of its
         * children (i.e. a WrappedDimension).
         */
        public @Nullable ContainerDimension getHeight() {
            if (mImpl.hasHeight()) {
                return DimensionBuilders.containerDimensionFromProto(mImpl.getHeight());
            } else {
                return null;
            }
        }

        /** Gets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element. */
        public @Nullable Modifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return Modifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull Column fromProto(
                LayoutElementProto.@NonNull Column proto, @Nullable Fingerprint fingerprint) {
            return new Column(proto, fingerprint);
        }

        static @NonNull Column fromProto(LayoutElementProto.@NonNull Column proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        LayoutElementProto.@NonNull Column toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull LayoutElement toLayoutElementProto() {
            return LayoutElementProto.LayoutElement.newBuilder().setColumn(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "Column{"
                    + "contents="
                    + getContents()
                    + ", horizontalAlignment="
                    + getHorizontalAlignment()
                    + ", width="
                    + getWidth()
                    + ", height="
                    + getHeight()
                    + ", modifiers="
                    + getModifiers()
                    + "}";
        }

        /** Builder for {@link Column}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements LayoutElement.Builder {
            private final LayoutElementProto.Column.Builder mImpl =
                    LayoutElementProto.Column.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1676323158);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Adds one item to the list of child elements to place inside this {@link Column}. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder addContent(@NonNull LayoutElement content) {
                mImpl.addContents(content.toLayoutElementProto());
                mFingerprint.addChildNode(checkNotNull(content.getFingerprint()));
                return this;
            }

            /**
             * Sets the horizontal alignment of elements inside this column, if they are narrower
             * than the resulting width of the column. If not defined, defaults to
             * HORIZONTAL_ALIGN_CENTER.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setHorizontalAlignment(
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
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setHorizontalAlignment(
                    @HorizontalAlignment int horizontalAlignment) {
                return setHorizontalAlignment(
                        new HorizontalAlignmentProp.Builder()
                                .setValue(horizontalAlignment)
                                .build());
            }

            /**
             * Sets the width of this column. If not defined, this will size itself to fit all of
             * its children (i.e. a WrappedDimension).
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setWidth(@NonNull ContainerDimension width) {
                mImpl.setWidth(width.toContainerDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(width.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the height of this column. If not defined, this will size itself to fit all of
             * its children (i.e. a WrappedDimension).
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setHeight(@NonNull ContainerDimension height) {
                mImpl.setHeight(height.toContainerDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(height.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setModifiers(@NonNull Modifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        5, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull Column build() {
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
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class Row implements LayoutElement {
        private final LayoutElementProto.Row mImpl;
        private final @Nullable Fingerprint mFingerprint;

        Row(LayoutElementProto.Row impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the list of child elements to place inside this {@link Row}. */
        public @NonNull List<LayoutElement> getContents() {
            List<LayoutElement> list = new ArrayList<>();
            for (LayoutElementProto.LayoutElement item : mImpl.getContentsList()) {
                list.add(LayoutElementBuilders.layoutElementFromProto(item));
            }
            return Collections.unmodifiableList(list);
        }

        /**
         * Gets the vertical alignment of elements inside this row, if they are narrower than the
         * resulting height of the row. If not defined, defaults to VERTICAL_ALIGN_CENTER.
         */
        public @Nullable VerticalAlignmentProp getVerticalAlignment() {
            if (mImpl.hasVerticalAlignment()) {
                return VerticalAlignmentProp.fromProto(mImpl.getVerticalAlignment());
            } else {
                return null;
            }
        }

        /**
         * Gets the width of this row. If not defined, this will size itself to fit all of its
         * children (i.e. a WrappedDimension).
         */
        public @Nullable ContainerDimension getWidth() {
            if (mImpl.hasWidth()) {
                return DimensionBuilders.containerDimensionFromProto(mImpl.getWidth());
            } else {
                return null;
            }
        }

        /**
         * Gets the height of this row. If not defined, this will size itself to fit all of its
         * children (i.e. a WrappedDimension).
         */
        public @Nullable ContainerDimension getHeight() {
            if (mImpl.hasHeight()) {
                return DimensionBuilders.containerDimensionFromProto(mImpl.getHeight());
            } else {
                return null;
            }
        }

        /** Gets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element. */
        public @Nullable Modifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return Modifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull Row fromProto(
                LayoutElementProto.@NonNull Row proto, @Nullable Fingerprint fingerprint) {
            return new Row(proto, fingerprint);
        }

        static @NonNull Row fromProto(LayoutElementProto.@NonNull Row proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        LayoutElementProto.@NonNull Row toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull LayoutElement toLayoutElementProto() {
            return LayoutElementProto.LayoutElement.newBuilder().setRow(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "Row{"
                    + "contents="
                    + getContents()
                    + ", verticalAlignment="
                    + getVerticalAlignment()
                    + ", width="
                    + getWidth()
                    + ", height="
                    + getHeight()
                    + ", modifiers="
                    + getModifiers()
                    + "}";
        }

        /** Builder for {@link Row}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements LayoutElement.Builder {
            private final LayoutElementProto.Row.Builder mImpl =
                    LayoutElementProto.Row.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1279502255);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Adds one item to the list of child elements to place inside this {@link Row}. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder addContent(@NonNull LayoutElement content) {
                mImpl.addContents(content.toLayoutElementProto());
                mFingerprint.addChildNode(checkNotNull(content.getFingerprint()));
                return this;
            }

            /**
             * Sets the vertical alignment of elements inside this row, if they are narrower than
             * the resulting height of the row. If not defined, defaults to VERTICAL_ALIGN_CENTER.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setVerticalAlignment(
                    @NonNull VerticalAlignmentProp verticalAlignment) {
                mImpl.setVerticalAlignment(verticalAlignment.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(verticalAlignment.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the vertical alignment of elements inside this row, if they are narrower than
             * the resulting height of the row. If not defined, defaults to VERTICAL_ALIGN_CENTER.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setVerticalAlignment(@VerticalAlignment int verticalAlignment) {
                return setVerticalAlignment(
                        new VerticalAlignmentProp.Builder().setValue(verticalAlignment).build());
            }

            /**
             * Sets the width of this row. If not defined, this will size itself to fit all of its
             * children (i.e. a WrappedDimension).
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setWidth(@NonNull ContainerDimension width) {
                mImpl.setWidth(width.toContainerDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(width.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the height of this row. If not defined, this will size itself to fit all of its
             * children (i.e. a WrappedDimension).
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setHeight(@NonNull ContainerDimension height) {
                mImpl.setHeight(height.toContainerDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(height.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setModifiers(@NonNull Modifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        5, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull Row build() {
                return new Row(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * An arc container. This container will fill itself to a circle, which fits inside its parent
     * container, and all of its children will be placed on that circle. The fields anchor_angle and
     * anchor_type can be used to specify where to draw children within this circle.
     *
     * <p>Note that when setting padding for the arc, if padding values (top, button, left, and
     * right) are not equal, the largest between them will be used to apply padding uniformly to all
     * sides.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class Arc implements LayoutElement {
        private final LayoutElementProto.Arc mImpl;
        private final @Nullable Fingerprint mFingerprint;

        Arc(LayoutElementProto.Arc impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets contents of this container. */
        public @NonNull List<ArcLayoutElement> getContents() {
            List<ArcLayoutElement> list = new ArrayList<>();
            for (LayoutElementProto.ArcLayoutElement item : mImpl.getContentsList()) {
                list.add(LayoutElementBuilders.arcLayoutElementFromProto(item));
            }
            return Collections.unmodifiableList(list);
        }

        /**
         * Gets the angle for the anchor, used with anchor_type to determine where to draw children.
         * Note that 0 degrees is the 12 o clock position on a device, and the angle sweeps
         * clockwise. If not defined, defaults to 0 degrees.
         *
         * <p>Values do not have to be clamped to the range 0-360; values less than 0 degrees will
         * sweep anti-clockwise (i.e. -90 degrees is equivalent to 270 degrees), and values >360
         * will be be placed at X mod 360 degrees.
         *
         * <p>While this field is statically accessible from 1.0, it's only bindable since version
         * 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
         */
        public @Nullable DegreesProp getAnchorAngle() {
            if (mImpl.hasAnchorAngle()) {
                return DegreesProp.fromProto(mImpl.getAnchorAngle());
            } else {
                return null;
            }
        }

        /**
         * Gets how to align the contents of this container relative to anchor_angle. If not
         * defined, defaults to ARC_ANCHOR_CENTER.
         */
        public @Nullable ArcAnchorTypeProp getAnchorType() {
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
         * not defined, defaults to VERTICAL_ALIGN_CENTER.
         */
        public @Nullable VerticalAlignmentProp getVerticalAlign() {
            if (mImpl.hasVerticalAlign()) {
                return VerticalAlignmentProp.fromProto(mImpl.getVerticalAlign());
            } else {
                return null;
            }
        }

        /** Gets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element. */
        public @Nullable Modifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return Modifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        /** Gets defines the direction in which child elements are laid out. */
        public @Nullable ArcDirectionProp getArcDirection() {
            if (mImpl.hasArcDirection()) {
                return ArcDirectionProp.fromProto(mImpl.getArcDirection());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull Arc fromProto(
                LayoutElementProto.@NonNull Arc proto, @Nullable Fingerprint fingerprint) {
            return new Arc(proto, fingerprint);
        }

        static @NonNull Arc fromProto(LayoutElementProto.@NonNull Arc proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        LayoutElementProto.@NonNull Arc toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull LayoutElement toLayoutElementProto() {
            return LayoutElementProto.LayoutElement.newBuilder().setArc(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "Arc{"
                    + "contents="
                    + getContents()
                    + ", anchorAngle="
                    + getAnchorAngle()
                    + ", anchorType="
                    + getAnchorType()
                    + ", verticalAlign="
                    + getVerticalAlign()
                    + ", modifiers="
                    + getModifiers()
                    + ", arcDirection="
                    + getArcDirection()
                    + "}";
        }

        /** Builder for {@link Arc}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements LayoutElement.Builder {
            private final LayoutElementProto.Arc.Builder mImpl =
                    LayoutElementProto.Arc.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-257261663);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Adds one item to contents of this container. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder addContent(@NonNull ArcLayoutElement content) {
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
             *
             * <p>While this field is statically accessible from 1.0, it's only bindable since
             * version 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setAnchorAngle(@NonNull DegreesProp anchorAngle) {
                mImpl.setAnchorAngle(anchorAngle.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(anchorAngle.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets how to align the contents of this container relative to anchor_angle. If not
             * defined, defaults to ARC_ANCHOR_CENTER.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setAnchorType(@NonNull ArcAnchorTypeProp anchorType) {
                mImpl.setAnchorType(anchorType.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(anchorType.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets how to align the contents of this container relative to anchor_angle. If not
             * defined, defaults to ARC_ANCHOR_CENTER.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setAnchorType(@ArcAnchorType int anchorType) {
                return setAnchorType(new ArcAnchorTypeProp.Builder().setValue(anchorType).build());
            }

            /**
             * Sets vertical alignment of elements within the arc. If the {@link Arc}'s thickness is
             * larger than the thickness of the element being drawn, this controls whether the
             * element should be drawn towards the inner or outer edge of the arc, or drawn in the
             * center. If not defined, defaults to VERTICAL_ALIGN_CENTER.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setVerticalAlign(@NonNull VerticalAlignmentProp verticalAlign) {
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
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setVerticalAlign(@VerticalAlignment int verticalAlign) {
                return setVerticalAlign(
                        new VerticalAlignmentProp.Builder().setValue(verticalAlign).build());
            }

            /**
             * Sets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setModifiers(@NonNull Modifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        5, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the direction in which child elements are laid out. If not set, defaults to
             * ARC_DIRECTION_NORMAL.
             */
            @RequiresSchemaVersion(major = 1, minor = 300)
            public @NonNull Builder setArcDirection(@NonNull ArcDirectionProp arcDirection) {
                mImpl.setArcDirection(arcDirection.toProto());
                mFingerprint.recordPropertyUpdate(
                        7, checkNotNull(arcDirection.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the direction in which child elements are laid out. If not set, defaults to
             * ARC_DIRECTION_NORMAL.
             */
            @RequiresSchemaVersion(major = 1, minor = 300)
            public @NonNull Builder setArcDirection(@ArcDirection int arcDirection) {
                return setArcDirection(
                        new ArcDirectionProp.Builder().setValue(arcDirection).build());
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull Arc build() {
                return new Arc(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A text element that can be used in an {@link Arc}. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class ArcText implements ArcLayoutElement {
        private final LayoutElementProto.ArcText mImpl;
        private final @Nullable Fingerprint mFingerprint;

        ArcText(LayoutElementProto.ArcText impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the text to render. */
        public @Nullable StringProp getText() {
            if (mImpl.hasText()) {
                return StringProp.fromProto(mImpl.getText());
            } else {
                return null;
            }
        }

        /**
         * Gets the style of font to use (size, bold etc). If not specified, defaults to the
         * platform's default body font.
         */
        public @Nullable FontStyle getFontStyle() {
            if (mImpl.hasFontStyle()) {
                return FontStyle.fromProto(mImpl.getFontStyle());
            } else {
                return null;
            }
        }

        /** Gets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element. */
        public @Nullable ArcModifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return ArcModifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        /** Gets defines the direction in which text is drawn. */
        public @Nullable ArcDirectionProp getArcDirection() {
            if (mImpl.hasArcDirection()) {
                return ArcDirectionProp.fromProto(mImpl.getArcDirection());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull ArcText fromProto(
                LayoutElementProto.@NonNull ArcText proto, @Nullable Fingerprint fingerprint) {
            return new ArcText(proto, fingerprint);
        }

        static @NonNull ArcText fromProto(LayoutElementProto.@NonNull ArcText proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        LayoutElementProto.@NonNull ArcText toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull ArcLayoutElement toArcLayoutElementProto() {
            return LayoutElementProto.ArcLayoutElement.newBuilder().setText(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "ArcText{"
                    + "text="
                    + getText()
                    + ", fontStyle="
                    + getFontStyle()
                    + ", modifiers="
                    + getModifiers()
                    + ", arcDirection="
                    + getArcDirection()
                    + "}";
        }

        /** Builder for {@link ArcText}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements ArcLayoutElement.Builder {
            private final LayoutElementProto.ArcText.Builder mImpl =
                    LayoutElementProto.ArcText.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-132896327);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the text to render.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setText(@NonNull StringProp text) {
                if (text.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "ArcText.Builder.setText doesn't support dynamic values.");
                }
                mImpl.setText(text.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(text.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the text to render. */
            public @NonNull Builder setText(@NonNull String text) {
                return setText(new StringProp.Builder(text).build());
            }

            /**
             * Sets the style of font to use (size, bold etc). If not specified, defaults to the
             * platform's default body font.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setFontStyle(@NonNull FontStyle fontStyle) {
                mImpl.setFontStyle(fontStyle.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(fontStyle.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setModifiers(@NonNull ArcModifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the direction in which this text is drawn. If not set, defaults to
             * ARC_DIRECTION_CLOCKWISE.
             */
            @RequiresSchemaVersion(major = 1, minor = 300)
            public @NonNull Builder setArcDirection(@NonNull ArcDirectionProp arcDirection) {
                mImpl.setArcDirection(arcDirection.toProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(arcDirection.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the direction in which this text is drawn. If not set, defaults to
             * ARC_DIRECTION_CLOCKWISE.
             */
            @RequiresSchemaVersion(major = 1, minor = 300)
            public @NonNull Builder setArcDirection(@ArcDirection int arcDirection) {
                return setArcDirection(
                        new ArcDirectionProp.Builder().setValue(arcDirection).build());
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull ArcText build() {
                return new ArcText(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A line that can be used in an {@link Arc} and renders as a round progress bar. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class ArcLine implements ArcLayoutElement {
        private final LayoutElementProto.ArcLine mImpl;
        private final @Nullable Fingerprint mFingerprint;

        ArcLine(LayoutElementProto.ArcLine impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the length of this line, in degrees. If not defined, defaults to 0.
         *
         * <p>While this field is statically accessible from 1.0, it's only bindable since version
         * 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
         */
        public @Nullable DegreesProp getLength() {
            if (mImpl.hasLength()) {
                return DegreesProp.fromProto(mImpl.getLength());
            } else {
                return null;
            }
        }

        /** Gets the thickness of this line. If not defined, defaults to 0. */
        public @Nullable DpProp getThickness() {
            if (mImpl.hasThickness()) {
                return DpProp.fromProto(mImpl.getThickness());
            } else {
                return null;
            }
        }

        /**
         * Gets the color of this line.
         *
         * <p>While this field is statically accessible from 1.0, it's only bindable since version
         * 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
         *
         * <p>If a brush is set, this color will not be used.
         */
        public @Nullable ColorProp getColor() {
            if (mImpl.hasColor()) {
                return ColorProp.fromProto(mImpl.getColor());
            } else {
                return null;
            }
        }

        /**
         * Gets a brush used to draw this line. If set, the brush will be used instead of the color
         * provided in {@code setColor()}.
         */
        public @Nullable Brush getBrush() {
            if (mImpl.hasBrush()) {
                return ColorBuilders.brushFromProto(mImpl.getBrush());
            } else {
                return null;
            }
        }

        /** Gets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element. */
        public @Nullable ArcModifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return ArcModifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        /** Gets the line stroke cap. If not defined, defaults to STROKE_CAP_ROUND. */
        public @Nullable StrokeCapProp getStrokeCap() {
            if (mImpl.hasStrokeCap()) {
                return StrokeCapProp.fromProto(mImpl.getStrokeCap());
            } else {
                return null;
            }
        }

        /** Gets defines the direction in which line drawn. */
        public @Nullable ArcDirectionProp getArcDirection() {
            if (mImpl.hasArcDirection()) {
                return ArcDirectionProp.fromProto(mImpl.getArcDirection());
            } else {
                return null;
            }
        }

        /**
         * Gets the bounding constraints for the layout affected by the dynamic value from {@link
         * #getLength()}.
         */
        public @Nullable AngularLayoutConstraint getLayoutConstraintsForDynamicLength() {
            if (mImpl.hasLength()) {
                return AngularLayoutConstraint.fromProto(mImpl.getLength());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull ArcLine fromProto(
                LayoutElementProto.@NonNull ArcLine proto, @Nullable Fingerprint fingerprint) {
            return new ArcLine(proto, fingerprint);
        }

        static @NonNull ArcLine fromProto(LayoutElementProto.@NonNull ArcLine proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        LayoutElementProto.@NonNull ArcLine toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull ArcLayoutElement toArcLayoutElementProto() {
            return LayoutElementProto.ArcLayoutElement.newBuilder().setLine(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "ArcLine{"
                    + "length="
                    + getLength()
                    + ", thickness="
                    + getThickness()
                    + ", color="
                    + getColor()
                    + ", brush="
                    + getBrush()
                    + ", modifiers="
                    + getModifiers()
                    + ", strokeCap="
                    + getStrokeCap()
                    + ", arcDirection="
                    + getArcDirection()
                    + "}";
        }

        /** Builder for {@link ArcLine}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements ArcLayoutElement.Builder {
            private final LayoutElementProto.ArcLine.Builder mImpl =
                    LayoutElementProto.ArcLine.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(846148011);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the length of this line, in degrees. If not defined, defaults to 0.
             *
             * <p>While this field is statically accessible from 1.0, it's only bindable since
             * version 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setLength(@NonNull DegreesProp length) {
                mImpl.mergeLength(length.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(length.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the bounding constraints for the layout affected by the dynamic value from
             * {@link #setLength(DegreesProp)}.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setLayoutConstraintsForDynamicLength(
                    DimensionBuilders.@NonNull AngularLayoutConstraint angularLayoutConstraint) {
                mImpl.mergeLength(angularLayoutConstraint.toProto());
                mFingerprint.recordPropertyUpdate(
                        1,
                        checkNotNull(angularLayoutConstraint.getFingerprint())
                                .aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the thickness of this line. If not defined, defaults to 0.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setThickness(@NonNull DpProp thickness) {
                if (thickness.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "ArcLine.Builder.setThickness doesn't support dynamic values.");
                }
                mImpl.setThickness(thickness.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(thickness.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the color of this line.
             *
             * <p>While this field is statically accessible from 1.0, it's only bindable since
             * version 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
             *
             * <p>If a brush is set, this color will not be used.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setColor(@NonNull ColorProp color) {
                mImpl.setColor(color.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(color.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets a brush used to draw this line. If set, the brush will be used instead of the
             * color provided in {@code setColor()}.
             *
             * @throws IllegalArgumentException if the brush is not a {@link SweepGradient}.
             */
            @RequiresSchemaVersion(major = 1, minor = 300)
            public @NonNull Builder setBrush(@NonNull Brush brush) {
                if (!(brush instanceof SweepGradient)) {
                    throw new IllegalArgumentException(
                            "Only SweepGradient is supported for ArcLine.");
                }
                mImpl.setBrush(brush.toBrushProto());
                mFingerprint.recordPropertyUpdate(
                        7, checkNotNull(brush.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setModifiers(@NonNull ArcModifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the line stroke cap. If not defined, defaults to STROKE_CAP_ROUND. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setStrokeCap(@NonNull StrokeCapProp strokeCap) {
                mImpl.setStrokeCap(strokeCap.toProto());
                mFingerprint.recordPropertyUpdate(
                        6, checkNotNull(strokeCap.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the direction in which this line is drawn. If not set, defaults to
             * ARC_DIRECTION_CLOCKWISE.
             */
            @RequiresSchemaVersion(major = 1, minor = 300)
            public @NonNull Builder setArcDirection(@NonNull ArcDirectionProp arcDirection) {
                mImpl.setArcDirection(arcDirection.toProto());
                mFingerprint.recordPropertyUpdate(
                        8, checkNotNull(arcDirection.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the direction in which this line is drawn. If not set, defaults to
             * ARC_DIRECTION_CLOCKWISE.
             */
            @RequiresSchemaVersion(major = 1, minor = 300)
            public @NonNull Builder setArcDirection(@ArcDirection int arcDirection) {
                return setArcDirection(
                        new ArcDirectionProp.Builder().setValue(arcDirection).build());
            }

            /** Sets the line stroke cap. If not defined, defaults to STROKE_CAP_ROUND. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setStrokeCap(@StrokeCap int strokeCap) {
                return setStrokeCap(new StrokeCapProp.Builder().setValue(strokeCap).build());
            }

            @Override
            public @NonNull ArcLine build() {
                String onlyOpaqueMsg = "Only opaque colors are supported";
                String alphaChangeMsg =
                        "Any transparent colors will have their alpha component set to 0xFF"
                                + " (opaque).";
                for (ColorProto.ColorStop colorStop :
                        mImpl.getBrush().getSweepGradient().getColorStopsList()) {
                    if (Color.alpha(colorStop.getColor().getArgb()) < 0xFF) {
                        Log.w("ArcLine", onlyOpaqueMsg + " for SweepGradient. " + alphaChangeMsg);
                        break;
                    }
                }
                if (mImpl.getStrokeCap().hasShadow()
                        && Color.alpha(mImpl.getColor().getArgb()) < 0xFF) {
                    Log.w(
                            "ArcLine",
                            onlyOpaqueMsg + " when using StrokeCap Shadow. " + alphaChangeMsg);
                }
                return new ArcLine(mImpl.build(), mFingerprint);
            }
        }
    }

    /** An extensible {@code StrokeCap} property. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static final class StrokeCapProp {
        private final LayoutElementProto.StrokeCapProp mImpl;
        private final @Nullable Fingerprint mFingerprint;

        StrokeCapProp(LayoutElementProto.StrokeCapProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. */
        @StrokeCap
        public int getValue() {
            return mImpl.getValue().getNumber();
        }

        /**
         * Gets the stroke cap's shadow. When set, the stroke cap will be drawn with a shadow, which
         * allows it to be visible on top of other similarly colored elements.
         *
         * <p>Only opaque colors are supported in {@link ArcLine} if a shadow is set. Any
         * transparent colors will have their alpha component set to 0xFF (opaque).
         */
        public @Nullable Shadow getShadow() {
            if (mImpl.hasShadow()) {
                return Shadow.fromProto(mImpl.getShadow());
            } else {
                return null;
            }
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull StrokeCapProp fromProto(
                LayoutElementProto.@NonNull StrokeCapProp proto,
                @Nullable Fingerprint fingerprint) {
            return new StrokeCapProp(proto, fingerprint);
        }

        static @NonNull StrokeCapProp fromProto(LayoutElementProto.@NonNull StrokeCapProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull StrokeCapProp toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "StrokeCapProp{" + "value=" + getValue() + ", shadow=" + getShadow() + "}";
        }

        /** Builder for {@link StrokeCapProp} */
        public static final class Builder {
            private final LayoutElementProto.StrokeCapProp.Builder mImpl =
                    LayoutElementProto.StrokeCapProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-956183418);

            /** Creates an instance of {@link Builder}. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public Builder() {}

            /** Sets the value. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setValue(@StrokeCap int value) {
                mImpl.setValue(LayoutElementProto.StrokeCap.forNumber(value));
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            /**
             * Sets the stroke cap's shadow. When set, the stroke cap will be drawn with a shadow,
             * which allows it to be visible on top of other similarly colored elements.
             *
             * <p>Only opaque colors are supported in {@link ArcLine} if a shadow is set. Any
             * transparent colors will have their alpha component set to 0xFF (opaque).
             */
            @RequiresSchemaVersion(major = 1, minor = 300)
            public @NonNull Builder setShadow(@NonNull Shadow shadow) {
                mImpl.setShadow(shadow.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(shadow.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull StrokeCapProp build() {
                return new StrokeCapProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A dashed arc line. It is an arc line made up of arc line segments separated by gaps, and can
     * be placed in an {@link Arc} container.
     */
    @RequiresSchemaVersion(major = 1, minor = 500)
    public static final class DashedArcLine implements ArcLayoutElement {
        private final LayoutElementProto.DashedArcLine mImpl;
        private final @Nullable Fingerprint mFingerprint;

        DashedArcLine(LayoutElementProto.DashedArcLine impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the length of this line in degrees, including gaps. */
        public @Nullable DegreesProp getLength() {
            if (mImpl.hasLength()) {
                return DegreesProp.fromProto(mImpl.getLength());
            } else {
                return null;
            }
        }

        /** Gets the thickness of this line. */
        @Dimension(unit = DP)
        public float getThickness() {
            if (mImpl.hasThickness()) {
                return DpProp.fromProto(mImpl.getThickness()).getValue();
            } else {
                return 0;
            }
        }

        /** Gets the color of this line. */
        public @Nullable ColorProp getColor() {
            if (mImpl.hasColor()) {
                return ColorProp.fromProto(mImpl.getColor());
            } else {
                return null;
            }
        }

        /** Gets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element. */
        public @Nullable ArcModifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return ArcModifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        /** Gets the direction in which this line is drawn. */
        public @Nullable ArcDirectionProp getArcDirection() {
            if (mImpl.hasArcDirection()) {
                return ArcDirectionProp.fromProto(mImpl.getArcDirection());
            } else {
                return null;
            }
        }

        /** Gets the dashed line pattern which describes how the arc line is segmented by gaps. */
        public @Nullable DashedLinePattern getLinePattern() {
            if (mImpl.hasLinePattern()) {
                return DashedLinePattern.fromProto(mImpl.getLinePattern());
            } else {
                return null;
            }
        }

        /**
         * Gets the bounding constraints for the layout affected by the dynamic value from {@link
         * #getLength()}.
         */
        public @Nullable AngularLayoutConstraint getLayoutConstraintsForDynamicLength() {
            if (mImpl.hasLength()) {
                return AngularLayoutConstraint.fromProto(mImpl.getLength());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull DashedArcLine fromProto(
                LayoutElementProto.@NonNull DashedArcLine proto,
                @Nullable Fingerprint fingerprint) {
            return new DashedArcLine(proto, fingerprint);
        }

        static @NonNull DashedArcLine fromProto(LayoutElementProto.@NonNull DashedArcLine proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        LayoutElementProto.@NonNull DashedArcLine toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull ArcLayoutElement toArcLayoutElementProto() {
            return LayoutElementProto.ArcLayoutElement.newBuilder().setDashedLine(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "DashedArcLine{"
                    + "length="
                    + getLength()
                    + ", thickness="
                    + getThickness()
                    + ", color="
                    + getColor()
                    + ", modifiers="
                    + getModifiers()
                    + ", arcDirection="
                    + getArcDirection()
                    + ", linePattern="
                    + getLinePattern()
                    + "}";
        }

        /** Builder for {@link DashedArcLine}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements ArcLayoutElement.Builder {
            private final LayoutElementProto.DashedArcLine.Builder mImpl =
                    LayoutElementProto.DashedArcLine.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1152963772);

            /** Creates an instance of {@link Builder}. */
            @RequiresSchemaVersion(major = 1, minor = 500)
            public Builder() {}

            /**
             * Sets the length of this line in degrees, including gaps. If not defined, defaults to
             * 0.
             */
            @RequiresSchemaVersion(major = 1, minor = 500)
            public @NonNull Builder setLength(@NonNull DegreesProp length) {
                mImpl.mergeLength(length.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(length.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the thickness of this line. If not defined, defaults to 0. */
            @RequiresSchemaVersion(major = 1, minor = 500)
            public @NonNull Builder setThickness(@Dimension(unit = DP) float thickness) {
                DpProp thicknessProp = dp(thickness);
                mImpl.setThickness(thicknessProp.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(thicknessProp.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the color of this line. */
            @RequiresSchemaVersion(major = 1, minor = 500)
            public @NonNull Builder setColor(@NonNull ColorProp color) {
                mImpl.setColor(color.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(color.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element.
             */
            @RequiresSchemaVersion(major = 1, minor = 500)
            public @NonNull Builder setModifiers(@NonNull ArcModifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the direction in which this line is drawn. If not set, defaults to the {@link
             * Arc} container's direction where it is placed in.
             */
            @RequiresSchemaVersion(major = 1, minor = 500)
            public @NonNull Builder setArcDirection(@NonNull ArcDirectionProp arcDirection) {
                mImpl.setArcDirection(arcDirection.toProto());
                mFingerprint.recordPropertyUpdate(
                        5, checkNotNull(arcDirection.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the direction in which this line is drawn. If not set, defaults to the {@link
             * Arc} container's direction where it is placed in.
             */
            @RequiresSchemaVersion(major = 1, minor = 500)
            public @NonNull Builder setArcDirection(@ArcDirection int arcDirection) {
                return setArcDirection(
                        new ArcDirectionProp.Builder().setValue(arcDirection).build());
            }

            /**
             * Sets the dashed line pattern which describes how the arc line is segmented by gaps.
             */
            @RequiresSchemaVersion(major = 1, minor = 500)
            public @NonNull Builder setLinePattern(@NonNull DashedLinePattern linePattern) {
                mImpl.setLinePattern(linePattern.toProto());
                mFingerprint.recordPropertyUpdate(
                        6, checkNotNull(linePattern.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the bounding constraints for the layout affected by the dynamic value from
             * {@link #setLength(DegreesProp)}.
             */
            @RequiresSchemaVersion(major = 1, minor = 500)
            public @NonNull Builder setLayoutConstraintsForDynamicLength(
                    @NonNull AngularLayoutConstraint angularLayoutConstraint) {
                mImpl.mergeLength(angularLayoutConstraint.toProto());
                mFingerprint.recordPropertyUpdate(
                        1,
                        checkNotNull(angularLayoutConstraint.getFingerprint())
                                .aggregateValueAsInt());
                return this;
            }

            /** Builds an instance with the values accumulated in this Builder. */
            @SuppressLint("ProtoLayoutMinSchema")
            @Override
            public @NonNull DashedArcLine build() {
                return new DashedArcLine(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A dashed line pattern which describes how the dashed arc line is segmented by gaps. It
     * determines the gap size and the gap locations.
     */
    @RequiresSchemaVersion(major = 1, minor = 500)
    public static final class DashedLinePattern {
        private final LayoutElementProto.DashedLinePattern mImpl;
        private final @Nullable Fingerprint mFingerprint;

        DashedLinePattern(
                LayoutElementProto.DashedLinePattern impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the size in dp of the gap between the arc line segments. */
        public @Nullable DpProp getGapSize() {
            if (mImpl.hasGapSize()) {
                return DpProp.fromProto(mImpl.getGapSize());
            } else {
                return null;
            }
        }

        /** Gets the list of each gap's center location in degrees. */
        public @NonNull List<DegreesProp> getGapLocations() {
            List<DegreesProp> list = new ArrayList<>();
            for (DimensionProto.DegreesProp item : mImpl.getGapLocationsList()) {
                list.add(DegreesProp.fromProto(item));
            }
            return Collections.unmodifiableList(list);
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull DashedLinePattern fromProto(
                LayoutElementProto.@NonNull DashedLinePattern proto,
                @Nullable Fingerprint fingerprint) {
            return new DashedLinePattern(proto, fingerprint);
        }

        static @NonNull DashedLinePattern fromProto(
                LayoutElementProto.@NonNull DashedLinePattern proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull DashedLinePattern toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "DashedLinePattern{"
                    + "gapSize="
                    + getGapSize()
                    + ", gapLocations="
                    + getGapLocations()
                    + "}";
        }

        /** Builder for {@link DashedLinePattern} */
        public static final class Builder {
            private final LayoutElementProto.DashedLinePattern.Builder mImpl =
                    LayoutElementProto.DashedLinePattern.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1050989205);

            /** Creates an instance of {@link Builder}. */
            @RequiresSchemaVersion(major = 1, minor = 500)
            public Builder() {}

            /**
             * Sets the size in dp of the gap between the segments. If not defined, defaults to 0.
             */
            @RequiresSchemaVersion(major = 1, minor = 500)
            public @NonNull Builder setGapSize(@Dimension(unit = DP) float gapSizeInDp) {
                DpProp gapSizeProp = dp(gapSizeInDp);
                mImpl.setGapSize(gapSizeProp.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(gapSizeProp.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Adds one item to the list of each gap's center location in degrees.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 500)
            private @NonNull Builder addGapLocation(@NonNull DegreesProp gapLocation) {
                if (gapLocation.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "DashedLinePattern.Builder.addGapLocation doesn't support dynamic "
                                    + "values.");
                }
                mImpl.addGapLocations(gapLocation.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(gapLocation.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the list of each gap's center location in degrees.
             *
             * <p>The interval between any two locations must not be shorter than thickness plus gap
             * size, otherwise the gap is ignored.
             *
             * <p>Note that calling this method will invalidate the previous call of {@link
             * #setGapInterval}
             */
            @RequiresSchemaVersion(major = 1, minor = 500)
            public @NonNull Builder setGapLocations(float @NonNull ... gapLocationsInDegrees) {
                mImpl.clearGapLocations();

                for (float gapLocation : gapLocationsInDegrees) {
                    addGapLocation(degrees(gapLocation));
                }

                return this;
            }

            /**
             * Sets the interval length in degrees between two consecutive gap center locations. The
             * arc line will have arc line segments with equal length.
             *
             * <p>The interval between any two locations must not be shorter than thickness plus gap
             * size, otherwise the gap is ignored.
             *
             * <p>Note that calling this method will remove all the gap locations set previously
             * with {@link #setGapLocations}
             */
            @RequiresSchemaVersion(major = 1, minor = 500)
            @SuppressLint("MissingGetterMatchingBuilder")
            public @NonNull Builder setGapInterval(float gapIntervalInDegrees) {
                mImpl.clearGapLocations();

                float gapLocation = 0;
                while (gapLocation <= 360F) {
                    addGapLocation(degrees(gapLocation));
                    gapLocation += gapIntervalInDegrees;
                }

                return this;
            }

            private static final int GAP_COUNTS_LIMIT = 100;

            /** Builds an instance from accumulated values. */
            public @NonNull DashedLinePattern build() {
                if (mImpl.getGapLocationsList().size() > GAP_COUNTS_LIMIT) {
                    throw new IllegalArgumentException(
                            "Number of gaps can't be larger than " + GAP_COUNTS_LIMIT + ".");
                }
                return new DashedLinePattern(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A simple spacer used to provide padding between adjacent elements in an {@link Arc}. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class ArcSpacer implements ArcLayoutElement {
        private final LayoutElementProto.ArcSpacer mImpl;
        private final @Nullable Fingerprint mFingerprint;

        ArcSpacer(LayoutElementProto.ArcSpacer impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the length of this spacer, in degrees. If not defined, defaults to 0. */
        public @Nullable DegreesProp getLength() {
            if (mImpl.hasLength()) {
                return DegreesProp.fromProto(mImpl.getLength());
            } else {
                return null;
            }
        }

        /** Gets the thickness of this spacer, in DP. If not defined, defaults to 0. */
        public @Nullable DpProp getThickness() {
            if (mImpl.hasThickness()) {
                return DpProp.fromProto(mImpl.getThickness());
            } else {
                return null;
            }
        }

        /** Gets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element. */
        public @Nullable ArcModifiers getModifiers() {
            if (mImpl.hasModifiers()) {
                return ArcModifiers.fromProto(mImpl.getModifiers());
            } else {
                return null;
            }
        }

        /** Gets the length of this spacer. */
        public @Nullable AngularDimension getAngularLength() {
            if (mImpl.hasAngularLength()) {
                return DimensionBuilders.angularDimensionFromProto(mImpl.getAngularLength());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull ArcSpacer fromProto(
                LayoutElementProto.@NonNull ArcSpacer proto, @Nullable Fingerprint fingerprint) {
            return new ArcSpacer(proto, fingerprint);
        }

        static @NonNull ArcSpacer fromProto(LayoutElementProto.@NonNull ArcSpacer proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        LayoutElementProto.@NonNull ArcSpacer toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull ArcLayoutElement toArcLayoutElementProto() {
            return LayoutElementProto.ArcLayoutElement.newBuilder().setSpacer(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "ArcSpacer{"
                    + "length="
                    + getLength()
                    + ", thickness="
                    + getThickness()
                    + ", modifiers="
                    + getModifiers()
                    + ", angularLength="
                    + getAngularLength()
                    + "}";
        }

        /** Builder for {@link ArcSpacer}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements ArcLayoutElement.Builder {
            private final LayoutElementProto.ArcSpacer.Builder mImpl =
                    LayoutElementProto.ArcSpacer.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1076667423);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the length of this spacer, in degrees. If not defined, defaults to 0.
             *
             * <p>This value is ignored when an angular length is provided by calling {@code
             * setAngularLength()}.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setLength(@NonNull DegreesProp length) {
                if (length.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "ArcSpacer.Builder.setLength doesn't support dynamic values.");
                }
                mImpl.setLength(length.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(length.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the thickness of this spacer, in DP. If not defined, defaults to 0.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setThickness(@NonNull DpProp thickness) {
                if (thickness.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "ArcSpacer.Builder.setThickness doesn't support dynamic values.");
                }
                mImpl.setThickness(thickness.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(thickness.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets {@link androidx.wear.protolayout.ModifiersBuilders.Modifiers} for this element.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setModifiers(@NonNull ArcModifiers modifiers) {
                mImpl.setModifiers(modifiers.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(modifiers.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the length of this spacer. If not defined, defaults to 0 degrees. If set, this
             * angular length will be used instead of the value provided in {@code setLength()}.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 500)
            public @NonNull Builder setAngularLength(@NonNull AngularDimension angularLength) {
                DimensionProto.AngularDimension angularDimensionProto =
                        angularLength.toAngularDimensionProto();
                if ((angularDimensionProto.hasDegrees()
                                && angularDimensionProto.getDegrees().hasDynamicValue())
                        || (angularDimensionProto.hasDp()
                                && angularDimensionProto.getDp().hasDynamicValue())) {
                    throw new IllegalArgumentException(
                            "ArcSpacer.Builder.setAngularLength doesn't support dynamic values.");
                }

                mImpl.setAngularLength(angularDimensionProto);
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(angularLength.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull ArcSpacer build() {
                return new ArcSpacer(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A container that allows a standard {@link LayoutElement} to be added to an {@link Arc}. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class ArcAdapter implements ArcLayoutElement {
        private final LayoutElementProto.ArcAdapter mImpl;
        private final @Nullable Fingerprint mFingerprint;

        ArcAdapter(LayoutElementProto.ArcAdapter impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the element to adapt to an {@link Arc}. */
        public @Nullable LayoutElement getContent() {
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
         * will not be rotated. If not defined, defaults to false.
         */
        private @Nullable BoolProp isRotateContents() {
            if (mImpl.hasRotateContents()) {
                return BoolProp.fromProto(mImpl.getRotateContents());
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
         * will not be rotated. If not defined, defaults to false.
         */
        public @Nullable BoolProp getRotateContents() {
            return isRotateContents();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull ArcAdapter fromProto(
                LayoutElementProto.@NonNull ArcAdapter proto, @Nullable Fingerprint fingerprint) {
            return new ArcAdapter(proto, fingerprint);
        }

        static @NonNull ArcAdapter fromProto(LayoutElementProto.@NonNull ArcAdapter proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        LayoutElementProto.@NonNull ArcAdapter toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull ArcLayoutElement toArcLayoutElementProto() {
            return LayoutElementProto.ArcLayoutElement.newBuilder().setAdapter(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "ArcAdapter{"
                    + "content="
                    + getContent()
                    + ", rotateContents="
                    + getRotateContents()
                    + "}";
        }

        /** Builder for {@link ArcAdapter}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements ArcLayoutElement.Builder {
            private final LayoutElementProto.ArcAdapter.Builder mImpl =
                    LayoutElementProto.ArcAdapter.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-176086106);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets whether this adapter's contents should be rotated, according to its position in
             * the arc or not. As an example, assume that an {@link Image} has been added to the
             * arc, and ends up at the 3 o clock position. If rotate_contents = true, the image will
             * be placed at the 3 o clock position, and will be rotated clockwise through 90
             * degrees. If rotate_contents = false, the image will be placed at the 3 o clock
             * position, but itself will not be rotated. If not defined, defaults to false.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setRotateContents(@NonNull BoolProp rotateContents) {
                if (rotateContents.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "ArcAdapter.Builder.setRotateContents doesn't support dynamic values.");
                }
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
            public @NonNull Builder setRotateContents(boolean rotateContents) {
                return setRotateContents(new BoolProp.Builder(rotateContents).build());
            }

            /**
             * Sets the element to adapt to an {@link Arc}.
             *
             * @throws IllegalArgumentException if the provided content has a transformation
             *     modifier.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setContent(@NonNull LayoutElement content) {
                LayoutElementProto.LayoutElement contentProto = content.toLayoutElementProto();
                if (hasTransformation(contentProto)) {
                    throw new IllegalArgumentException(
                            "Transformation modifier is not supported for the layout element inside"
                                    + " an ArcAdapter.");
                }
                mImpl.setContent(contentProto);
                mFingerprint.addChildNode(checkNotNull(content.getFingerprint()));
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull ArcAdapter build() {
                return new ArcAdapter(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * An extensible {@code ArcDirection} property that can be set to any curved element to control
     * the drawing direction.
     */
    @RequiresSchemaVersion(major = 1, minor = 300)
    public static final class ArcDirectionProp {
        private final LayoutElementProto.ArcDirectionProp mImpl;
        private final @Nullable Fingerprint mFingerprint;

        ArcDirectionProp(
                LayoutElementProto.ArcDirectionProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. */
        @ArcDirection
        public int getValue() {
            return mImpl.getValue().getNumber();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull ArcDirectionProp fromProto(
                LayoutElementProto.@NonNull ArcDirectionProp proto,
                @Nullable Fingerprint fingerprint) {
            return new ArcDirectionProp(proto, fingerprint);
        }

        static @NonNull ArcDirectionProp fromProto(
                LayoutElementProto.@NonNull ArcDirectionProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull ArcDirectionProp toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "ArcDirectionProp{" + "value=" + getValue() + "}";
        }

        /** Builder for {@link ArcDirectionProp} */
        public static final class Builder {
            private final LayoutElementProto.ArcDirectionProp.Builder mImpl =
                    LayoutElementProto.ArcDirectionProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-855955608);

            /** Creates an instance of {@link Builder} from the given value. */
            @RequiresSchemaVersion(major = 1, minor = 300)
            public Builder(@ArcDirection int value) {
                setValue(value);
            }

            @RequiresSchemaVersion(major = 1, minor = 300)
            Builder() {}

            /** Sets the arc direction value. */
            @RequiresSchemaVersion(major = 1, minor = 300)
            @NonNull Builder setValue(@ArcDirection int value) {
                mImpl.setValue(LayoutElementProto.ArcDirection.forNumber(value));
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull ArcDirectionProp build() {
                return new ArcDirectionProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A layout element which can be defined by a renderer extension. The payload in this message
     * will be passed verbatim to any registered renderer extension in the renderer. It is then
     * expected that the extension can parse this message, and emit the relevant element.
     *
     * <p>If a renderer extension is not installed, this resource will not render any element,
     * although the specified space will still be occupied. If the payload cannot be parsed by the
     * renderer extension, then still nothing should be rendered, although this behaviour is defined
     * by the renderer extension.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ExperimentalProtoLayoutExtensionApi
    public static final class ExtensionLayoutElement implements LayoutElement {
        private final LayoutElementProto.ExtensionLayoutElement mImpl;
        private final @Nullable Fingerprint mFingerprint;

        ExtensionLayoutElement(
                LayoutElementProto.ExtensionLayoutElement impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the content of the renderer extension element. This can be any data; it is expected
         * that the renderer extension knows how to parse this field.
         */
        public byte @NonNull [] getPayload() {
            return mImpl.getPayload().toByteArray();
        }

        /**
         * Gets the ID of the renderer extension that should be used for rendering this layout
         * element.
         */
        public @NonNull String getExtensionId() {
            return mImpl.getExtensionId();
        }

        /** Gets the width of this element. */
        public @Nullable ExtensionDimension getWidth() {
            if (mImpl.hasWidth()) {
                return DimensionBuilders.extensionDimensionFromProto(mImpl.getWidth());
            } else {
                return null;
            }
        }

        /** Gets the height of this element. */
        public @Nullable ExtensionDimension getHeight() {
            if (mImpl.hasHeight()) {
                return DimensionBuilders.extensionDimensionFromProto(mImpl.getHeight());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull ExtensionLayoutElement fromProto(
                LayoutElementProto.@NonNull ExtensionLayoutElement proto,
                @Nullable Fingerprint fingerprint) {
            return new ExtensionLayoutElement(proto, fingerprint);
        }

        static @NonNull ExtensionLayoutElement fromProto(
                LayoutElementProto.@NonNull ExtensionLayoutElement proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        LayoutElementProto.@NonNull ExtensionLayoutElement toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull LayoutElement toLayoutElementProto() {
            return LayoutElementProto.LayoutElement.newBuilder().setExtension(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "ExtensionLayoutElement{"
                    + "payload="
                    + Arrays.toString(getPayload())
                    + ", extensionId="
                    + getExtensionId()
                    + ", width="
                    + getWidth()
                    + ", height="
                    + getHeight()
                    + "}";
        }

        /** Builder for {@link ExtensionLayoutElement}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements LayoutElement.Builder {
            private final LayoutElementProto.ExtensionLayoutElement.Builder mImpl =
                    LayoutElementProto.ExtensionLayoutElement.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(661980356);

            /** Creates an instance of {@link Builder}. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public Builder() {}

            /**
             * Sets the content of the renderer extension element. This can be any data; it is
             * expected that the renderer extension knows how to parse this field.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setPayload(byte @NonNull [] payload) {
                mImpl.setPayload(ByteString.copyFrom(payload));
                mFingerprint.recordPropertyUpdate(1, Arrays.hashCode(payload));
                return this;
            }

            /**
             * Sets the ID of the renderer extension that should be used for rendering this layout
             * element.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setExtensionId(@NonNull String extensionId) {
                mImpl.setExtensionId(extensionId);
                mFingerprint.recordPropertyUpdate(2, extensionId.hashCode());
                return this;
            }

            /** Sets the width of this element. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setWidth(@NonNull ExtensionDimension width) {
                mImpl.setWidth(width.toExtensionDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(width.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the height of this element. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setHeight(@NonNull ExtensionDimension height) {
                mImpl.setHeight(height.toExtensionDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(height.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull ExtensionLayoutElement build() {
                return new ExtensionLayoutElement(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * Interface defining the root of all layout elements. This exists to act as a holder for all of
     * the actual layout elements above.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public interface LayoutElement {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        LayoutElementProto.@NonNull LayoutElement toLayoutElementProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable Fingerprint getFingerprint();

        /** Builder to create {@link LayoutElement} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull LayoutElement build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @OptIn(markerClass = ExperimentalProtoLayoutExtensionApi.class)
    public static @NonNull LayoutElement layoutElementFromProto(
            LayoutElementProto.@NonNull LayoutElement proto, @Nullable Fingerprint fingerprint) {
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
        if (proto.hasExtension()) {
            return ExtensionLayoutElement.fromProto(proto.getExtension(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of LayoutElement");
    }

    static @NonNull LayoutElement layoutElementFromProto(
            LayoutElementProto.@NonNull LayoutElement proto) {
        return layoutElementFromProto(proto, null);
    }

    /**
     * Interface defining the root of all elements that can be used in an {@link Arc}. This exists
     * to act as a holder for all of the actual arc layout elements above.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public interface ArcLayoutElement {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        LayoutElementProto.@NonNull ArcLayoutElement toArcLayoutElementProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable Fingerprint getFingerprint();

        /** Builder to create {@link ArcLayoutElement} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull ArcLayoutElement build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static @NonNull ArcLayoutElement arcLayoutElementFromProto(
            LayoutElementProto.@NonNull ArcLayoutElement proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasText()) {
            return ArcText.fromProto(proto.getText(), fingerprint);
        }
        if (proto.hasLine()) {
            return ArcLine.fromProto(proto.getLine(), fingerprint);
        }
        if (proto.hasSpacer()) {
            return ArcSpacer.fromProto(proto.getSpacer(), fingerprint);
        }
        if (proto.hasAdapter()) {
            return ArcAdapter.fromProto(proto.getAdapter(), fingerprint);
        }
        if (proto.hasDashedLine()) {
            return DashedArcLine.fromProto(proto.getDashedLine(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of ArcLayoutElement");
    }

    static @NonNull ArcLayoutElement arcLayoutElementFromProto(
            LayoutElementProto.@NonNull ArcLayoutElement proto) {
        return arcLayoutElementFromProto(proto, null);
    }

    /** A complete layout. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class Layout {
        private final LayoutElementProto.Layout mImpl;

        Layout(LayoutElementProto.Layout impl) {
            this.mImpl = impl;
        }

        /** Gets the root element in the layout. */
        public @Nullable LayoutElement getRoot() {
            if (mImpl.hasRoot()) {
                return LayoutElementBuilders.layoutElementFromProto(mImpl.getRoot());
            } else {
                return null;
            }
        }

        /** Creates a {@link Layout} object containing the given layout element. */
        public static @NonNull Layout fromLayoutElement(@NonNull LayoutElement layoutElement) {
            return new Builder().setRoot(layoutElement).build();
        }

        /** Converts to byte array representation. */
        @ProtoLayoutExperimental
        public byte @NonNull [] toByteArray() {
            return mImpl.toByteArray();
        }

        /** Converts from byte array representation. */
        @SuppressWarnings("ProtoParseWithRegistry")
        @ProtoLayoutExperimental
        public static @Nullable Layout fromByteArray(byte @NonNull [] byteArray) {
            try {
                return fromProto(LayoutElementProto.Layout.parseFrom(byteArray));
            } catch (InvalidProtocolBufferException e) {
                return null;
            }
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull Layout fromProto(LayoutElementProto.@NonNull Layout proto) {
            return new Layout(proto);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public LayoutElementProto.@NonNull Layout toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "Layout{" + "root=" + getRoot() + "}";
        }

        /** Builder for {@link Layout} */
        public static final class Builder {
            private final LayoutElementProto.Layout.Builder mImpl =
                    LayoutElementProto.Layout.newBuilder();

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the root element in the layout. */
            public @NonNull Builder setRoot(@NonNull LayoutElement root) {
                mImpl.setRoot(root.toLayoutElementProto());
                Fingerprint fingerprint = root.getFingerprint();
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
            public @NonNull Layout build() {
                return Layout.fromProto(mImpl.build());
            }
        }
    }

    /** The horizontal alignment of an element within its container. */
    @RequiresSchemaVersion(major = 1, minor = 0)
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
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int HORIZONTAL_ALIGN_UNDEFINED = 0;

    /** Horizontally align to the left. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int HORIZONTAL_ALIGN_LEFT = 1;

    /** Horizontally align to center. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int HORIZONTAL_ALIGN_CENTER = 2;

    /** Horizontally align to the right. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int HORIZONTAL_ALIGN_RIGHT = 3;

    /** Horizontally align to the content start (left in LTR layouts, right in RTL layouts). */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int HORIZONTAL_ALIGN_START = 4;

    /** Horizontally align to the content end (right in LTR layouts, left in RTL layouts). */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int HORIZONTAL_ALIGN_END = 5;

    /** The vertical alignment of an element within its container. */
    @RequiresSchemaVersion(major = 1, minor = 0)
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
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int VERTICAL_ALIGN_UNDEFINED = 0;

    /** Vertically align to the top. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int VERTICAL_ALIGN_TOP = 1;

    /** Vertically align to center. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int VERTICAL_ALIGN_CENTER = 2;

    /** Vertically align to the bottom. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int VERTICAL_ALIGN_BOTTOM = 3;

    /** Alignment of a text element. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({TEXT_ALIGN_UNDEFINED, TEXT_ALIGN_START, TEXT_ALIGN_CENTER, TEXT_ALIGN_END})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TextAlignment {}

    /** Alignment is undefined. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int TEXT_ALIGN_UNDEFINED = 0;

    /**
     * Align to the "start" of the {@link androidx.wear.protolayout.LayoutElementBuilders.Text}
     * element (left in LTR layouts, right in RTL layouts).
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int TEXT_ALIGN_START = 1;

    /**
     * Align to the center of the {@link androidx.wear.protolayout.LayoutElementBuilders.Text}
     * element.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int TEXT_ALIGN_CENTER = 2;

    /**
     * Align to the "end" of the {@link androidx.wear.protolayout.LayoutElementBuilders.Text}
     * element (right in LTR layouts, left in RTL layouts).
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int TEXT_ALIGN_END = 3;

    /**
     * The anchor position of an {@link androidx.wear.protolayout.LayoutElementBuilders.Arc}'s
     * elements. This is used to specify how elements added to an {@link
     * androidx.wear.protolayout.LayoutElementBuilders.Arc} should be laid out with respect to
     * anchor_angle.
     *
     * <p>As an example, assume that the following diagrams are wrapped to an arc, and each
     * represents an {@link androidx.wear.protolayout.LayoutElementBuilders.Arc} element containing
     * a single {@link androidx.wear.protolayout.LayoutElementBuilders.Text} element. The {@link
     * androidx.wear.protolayout.LayoutElementBuilders.Text} element's anchor_angle is "0" for all
     * cases.
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
     * }</pre>
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({ARC_ANCHOR_UNDEFINED, ARC_ANCHOR_START, ARC_ANCHOR_CENTER, ARC_ANCHOR_END})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ArcAnchorType {}

    /** Anchor position is undefined. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int ARC_ANCHOR_UNDEFINED = 0;

    /**
     * Anchor at the start of the elements. This will cause elements added to an arc to begin at the
     * given anchor_angle, and sweep around to the right.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int ARC_ANCHOR_START = 1;

    /**
     * Anchor at the center of the elements. This will cause the center of the whole set of elements
     * added to an arc to be pinned at the given anchor_angle.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int ARC_ANCHOR_CENTER = 2;

    /**
     * Anchor at the end of the elements. This will cause the set of elements inside the arc to end
     * at the specified anchor_angle, i.e. all elements should be to the left of anchor_angle.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int ARC_ANCHOR_END = 3;

    /**
     * How to lay out components in a {@link androidx.wear.protolayout.LayoutElementBuilders.Arc}
     * context when they are smaller than their container. This would be similar to {@code
     * HorizontalAlignment} in a {@link androidx.wear.protolayout.LayoutElementBuilders.Box} or
     * {@link androidx.wear.protolayout.LayoutElementBuilders.Column}.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
        ANGULAR_ALIGNMENT_UNDEFINED,
        ANGULAR_ALIGNMENT_START,
        ANGULAR_ALIGNMENT_CENTER,
        ANGULAR_ALIGNMENT_END
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AngularAlignment {}

    /** Angular alignment is undefined. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static final int ANGULAR_ALIGNMENT_UNDEFINED = 0;

    /**
     * Align to the start of the container. As an example, if the container starts at 90 degrees and
     * has 180 degrees of sweep, the element within would draw from 90 degrees, clockwise.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static final int ANGULAR_ALIGNMENT_START = 1;

    /**
     * Align to the center of the container. As an example, if the container starts at 90 degrees,
     * and has 180 degrees of sweep, and the contained element has 90 degrees of sweep, the element
     * would draw between 135 and 225 degrees.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static final int ANGULAR_ALIGNMENT_CENTER = 2;

    /**
     * Align to the end of the container. As an example, if the container starts at 90 degrees and
     * has 180 degrees of sweep, and the contained element has 90 degrees of sweep, the element
     * would draw between 180 and 270 degrees.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static final int ANGULAR_ALIGNMENT_END = 3;

    /** An extensible {@code HorizontalAlignment} property. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class HorizontalAlignmentProp {
        private final AlignmentProto.HorizontalAlignmentProp mImpl;
        private final @Nullable Fingerprint mFingerprint;

        HorizontalAlignmentProp(
                AlignmentProto.HorizontalAlignmentProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. */
        @HorizontalAlignment
        public int getValue() {
            return mImpl.getValue().getNumber();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull HorizontalAlignmentProp fromProto(
                AlignmentProto.@NonNull HorizontalAlignmentProp proto,
                @Nullable Fingerprint fingerprint) {
            return new HorizontalAlignmentProp(proto, fingerprint);
        }

        static @NonNull HorizontalAlignmentProp fromProto(
                AlignmentProto.@NonNull HorizontalAlignmentProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public AlignmentProto.@NonNull HorizontalAlignmentProp toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "HorizontalAlignmentProp{" + "value=" + getValue() + "}";
        }

        /** Builder for {@link HorizontalAlignmentProp} */
        public static final class Builder {
            private final AlignmentProto.HorizontalAlignmentProp.Builder mImpl =
                    AlignmentProto.HorizontalAlignmentProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1412659592);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the value. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setValue(@HorizontalAlignment int value) {
                mImpl.setValue(AlignmentProto.HorizontalAlignment.forNumber(value));
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull HorizontalAlignmentProp build() {
                return new HorizontalAlignmentProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** An extensible {@code VerticalAlignment} property. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class VerticalAlignmentProp {
        private final AlignmentProto.VerticalAlignmentProp mImpl;
        private final @Nullable Fingerprint mFingerprint;

        VerticalAlignmentProp(
                AlignmentProto.VerticalAlignmentProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. */
        @VerticalAlignment
        public int getValue() {
            return mImpl.getValue().getNumber();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull VerticalAlignmentProp fromProto(
                AlignmentProto.@NonNull VerticalAlignmentProp proto,
                @Nullable Fingerprint fingerprint) {
            return new VerticalAlignmentProp(proto, fingerprint);
        }

        static @NonNull VerticalAlignmentProp fromProto(
                AlignmentProto.@NonNull VerticalAlignmentProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public AlignmentProto.@NonNull VerticalAlignmentProp toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "VerticalAlignmentProp{" + "value=" + getValue() + "}";
        }

        /** Builder for {@link VerticalAlignmentProp} */
        public static final class Builder {
            private final AlignmentProto.VerticalAlignmentProp.Builder mImpl =
                    AlignmentProto.VerticalAlignmentProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1488177384);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the value. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setValue(@VerticalAlignment int value) {
                mImpl.setValue(AlignmentProto.VerticalAlignment.forNumber(value));
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull VerticalAlignmentProp build() {
                return new VerticalAlignmentProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** An extensible {@code TextAlignment} property. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class TextAlignmentProp {
        private final AlignmentProto.TextAlignmentProp mImpl;
        private final @Nullable Fingerprint mFingerprint;

        TextAlignmentProp(
                AlignmentProto.TextAlignmentProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. */
        @TextAlignment
        public int getValue() {
            return mImpl.getValue().getNumber();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull TextAlignmentProp fromProto(
                AlignmentProto.@NonNull TextAlignmentProp proto,
                @Nullable Fingerprint fingerprint) {
            return new TextAlignmentProp(proto, fingerprint);
        }

        static @NonNull TextAlignmentProp fromProto(
                AlignmentProto.@NonNull TextAlignmentProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public AlignmentProto.@NonNull TextAlignmentProp toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "TextAlignmentProp{" + "value=" + getValue() + "}";
        }

        /** Builder for {@link TextAlignmentProp} */
        public static final class Builder {
            private final AlignmentProto.TextAlignmentProp.Builder mImpl =
                    AlignmentProto.TextAlignmentProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1800500598);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the value. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setValue(@TextAlignment int value) {
                mImpl.setValue(AlignmentProto.TextAlignment.forNumber(value));
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull TextAlignmentProp build() {
                return new TextAlignmentProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** An extensible {@code ArcAnchorType} property. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class ArcAnchorTypeProp {
        private final AlignmentProto.ArcAnchorTypeProp mImpl;
        private final @Nullable Fingerprint mFingerprint;

        ArcAnchorTypeProp(
                AlignmentProto.ArcAnchorTypeProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. */
        @ArcAnchorType
        public int getValue() {
            return mImpl.getValue().getNumber();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull ArcAnchorTypeProp fromProto(
                AlignmentProto.@NonNull ArcAnchorTypeProp proto,
                @Nullable Fingerprint fingerprint) {
            return new ArcAnchorTypeProp(proto, fingerprint);
        }

        static @NonNull ArcAnchorTypeProp fromProto(
                AlignmentProto.@NonNull ArcAnchorTypeProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public AlignmentProto.@NonNull ArcAnchorTypeProp toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "ArcAnchorTypeProp{" + "value=" + getValue() + "}";
        }

        /** Builder for {@link ArcAnchorTypeProp} */
        public static final class Builder {
            private final AlignmentProto.ArcAnchorTypeProp.Builder mImpl =
                    AlignmentProto.ArcAnchorTypeProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-496387006);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the value. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setValue(@ArcAnchorType int value) {
                mImpl.setValue(AlignmentProto.ArcAnchorType.forNumber(value));
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull ArcAnchorTypeProp build() {
                return new ArcAnchorTypeProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * Font styles, currently set up to match Wear's font styling.
     *
     * @deprecated Use {@link androidx.wear.protolayout.material.Typography} on Material {@link
     *     androidx.wear.protolayout.material.Text} (highly recommended) or make your own {@link
     *     FontStyle}.
     */
    @Deprecated
    public static final class FontStyles {
        private static final int LARGE_SCREEN_WIDTH_DP = 210;

        private static boolean isLargeScreen(@NonNull DeviceParameters deviceParameters) {
            return deviceParameters.getScreenWidthDp() >= LARGE_SCREEN_WIDTH_DP;
        }

        /**
         * Font style for large display text.
         *
         * @deprecated Use {@link androidx.wear.protolayout.material.Typography#TYPOGRAPHY_DISPLAY1}
         *     on Material {@link androidx.wear.protolayout.material.Text}.
         */
        @Deprecated
        public static FontStyle.@NonNull Builder display1(
                @NonNull DeviceParameters deviceParameters) {
            return new FontStyle.Builder()
                    .setWeight(FONT_WEIGHT_BOLD)
                    .setSize(sp(isLargeScreen(deviceParameters) ? 54 : 50));
        }

        /**
         * Font style for medium display text.
         *
         * @deprecated Use {@link androidx.wear.protolayout.material.Typography#TYPOGRAPHY_DISPLAY2}
         *     on Material {@link androidx.wear.protolayout.material.Text}.
         */
        @Deprecated
        public static FontStyle.@NonNull Builder display2(
                @NonNull DeviceParameters deviceParameters) {
            return new FontStyle.Builder()
                    .setWeight(FONT_WEIGHT_BOLD)
                    .setSize(sp(isLargeScreen(deviceParameters) ? 44 : 40));
        }

        /**
         * Font style for small display text.
         *
         * @deprecated Use {@link androidx.wear.protolayout.material.Typography#TYPOGRAPHY_DISPLAY3}
         *     on Material {@link androidx.wear.protolayout.material.Text}.
         */
        @Deprecated
        public static FontStyle.@NonNull Builder display3(
                @NonNull DeviceParameters deviceParameters) {
            return new FontStyle.Builder()
                    .setWeight(FONT_WEIGHT_BOLD)
                    .setSize(sp(isLargeScreen(deviceParameters) ? 34 : 30));
        }

        /**
         * Font style for large title text.
         *
         * @deprecated Use {@link androidx.wear.protolayout.material.Typography#TYPOGRAPHY_TITLE1}
         *     on Material {@link androidx.wear.protolayout.material.Text}.
         */
        @Deprecated
        public static FontStyle.@NonNull Builder title1(
                @NonNull DeviceParameters deviceParameters) {
            return new FontStyle.Builder()
                    .setWeight(FONT_WEIGHT_BOLD)
                    .setSize(sp(isLargeScreen(deviceParameters) ? 26 : 24));
        }

        /**
         * Font style for medium title text.
         *
         * @deprecated Use {@link androidx.wear.protolayout.material.Typography#TYPOGRAPHY_TITLE2}
         *     on Material {@link androidx.wear.protolayout.material.Text}.
         */
        @Deprecated
        public static FontStyle.@NonNull Builder title2(
                @NonNull DeviceParameters deviceParameters) {
            return new FontStyle.Builder()
                    .setWeight(FONT_WEIGHT_BOLD)
                    .setSize(sp(isLargeScreen(deviceParameters) ? 22 : 20));
        }

        /**
         * Font style for small title text.
         *
         * @deprecated Use {@link androidx.wear.protolayout.material.Typography#TYPOGRAPHY_TITLE3}
         *     on Material {@link androidx.wear.protolayout.material.Text}.
         */
        @Deprecated
        public static FontStyle.@NonNull Builder title3(
                @NonNull DeviceParameters deviceParameters) {
            return new FontStyle.Builder()
                    .setWeight(FONT_WEIGHT_BOLD)
                    .setSize(sp(isLargeScreen(deviceParameters) ? 18 : 16));
        }

        /**
         * Font style for large body text.
         *
         * @deprecated Use {@link androidx.wear.protolayout.material.Typography#TYPOGRAPHY_BODY1} on
         *     Material {@link androidx.wear.protolayout.material.Text}.
         */
        @Deprecated
        public static FontStyle.@NonNull Builder body1(@NonNull DeviceParameters deviceParameters) {
            return new FontStyle.Builder().setSize(sp(isLargeScreen(deviceParameters) ? 18 : 16));
        }

        /**
         * Font style for medium body text.
         *
         * @deprecated Use {@link androidx.wear.protolayout.material.Typography#TYPOGRAPHY_BODY2} on
         *     Material {@link androidx.wear.protolayout.material.Text}.
         */
        @Deprecated
        public static FontStyle.@NonNull Builder body2(@NonNull DeviceParameters deviceParameters) {
            return new FontStyle.Builder().setSize(sp(isLargeScreen(deviceParameters) ? 16 : 14));
        }

        /**
         * Font style for button text.
         *
         * @deprecated Use {@link androidx.wear.protolayout.material.Typography#TYPOGRAPHY_BUTTON}
         *     on Material {@link androidx.wear.protolayout.material.Text}.
         */
        @Deprecated
        public static FontStyle.@NonNull Builder button(
                @NonNull DeviceParameters deviceParameters) {
            return new FontStyle.Builder()
                    .setWeight(FONT_WEIGHT_BOLD)
                    .setSize(sp(isLargeScreen(deviceParameters) ? 16 : 14));
        }

        /**
         * Font style for large caption text.
         *
         * @deprecated Use {@link androidx.wear.protolayout.material.Typography#TYPOGRAPHY_CAPTION1}
         *     on Material {@link androidx.wear.protolayout.material.Text}.
         */
        @Deprecated
        public static FontStyle.@NonNull Builder caption1(
                @NonNull DeviceParameters deviceParameters) {
            return new FontStyle.Builder().setSize(sp(isLargeScreen(deviceParameters) ? 16 : 14));
        }

        /**
         * Font style for medium caption text.
         *
         * @deprecated Use {@link androidx.wear.protolayout.material.Typography#TYPOGRAPHY_CAPTION2}
         *     on Material {@link androidx.wear.protolayout.material.Text}.
         */
        @Deprecated
        public static FontStyle.@NonNull Builder caption2(
                @NonNull DeviceParameters deviceParameters) {
            return new FontStyle.Builder().setSize(sp(isLargeScreen(deviceParameters) ? 14 : 12));
        }

        private FontStyles() {}
    }

    /** Checks whether a layout element has a transformation modifier. */
    private static boolean hasTransformation(LayoutElementProto.@NonNull LayoutElement content) {
        switch (content.getInnerCase()) {
            case IMAGE:
                return content.getImage().hasModifiers()
                        && content.getImage().getModifiers().hasTransformation();
            case TEXT:
                return content.getText().hasModifiers()
                        && content.getText().getModifiers().hasTransformation();
            case SPACER:
                return content.getSpacer().hasModifiers()
                        && content.getSpacer().getModifiers().hasTransformation();
            case BOX:
                return content.getBox().hasModifiers()
                        && content.getBox().getModifiers().hasTransformation();
            case ROW:
                return content.getRow().hasModifiers()
                        && content.getRow().getModifiers().hasTransformation();
            case COLUMN:
                return content.getColumn().hasModifiers()
                        && content.getColumn().getModifiers().hasTransformation();
            case SPANNABLE:
                return content.getSpannable().hasModifiers()
                        && content.getSpannable().getModifiers().hasTransformation();
            case ARC:
                return content.getArc().hasModifiers()
                        && content.getArc().getModifiers().hasTransformation();
            case EXTENSION:
                // fall through
            case INNER_NOT_SET:
                return false;
        }
        return false;
    }
}
