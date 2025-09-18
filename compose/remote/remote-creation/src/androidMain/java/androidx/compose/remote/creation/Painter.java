/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.creation;

import android.graphics.BlendMode;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.operations.paint.PaintBundle;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Support RemoteCompose Paint creation */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Painter {
    @NonNull PaintBundle mPaint = new PaintBundle();
    @NonNull RemoteComposeWriter mBuilder;
    public static int FONT_TYPE_DEFAULT = PaintBundle.FONT_TYPE_DEFAULT;
    public static int FONT_TYPE_SANS_SERIF = PaintBundle.FONT_TYPE_SANS_SERIF;
    public static int FONT_TYPE_SERIF = PaintBundle.FONT_TYPE_SERIF;
    public static int FONT_TYPE_MONOSPACE = PaintBundle.FONT_TYPE_MONOSPACE;
    private static final int NORMAL_WEIGHT = 400;

    Painter(@NonNull RemoteComposeWriter builder) {
        mBuilder = builder;
    }

    /** Write the paint to the buffer */
    public void commit() {
        mBuilder.mBuffer.addPaint(mPaint);
        mPaint.reset();
    }

    /**
     * Helper for setFlags(), setting or clearing the ANTI_ALIAS_FLAG bit AntiAliasing smooths out
     * the edges of what is being drawn, but is has no impact on the interior of the shape. See
     * setDither() and setFilterBitmap() to affect how colors are treated.
     *
     * @param aa true to set the antialias bit in the flags, false to clear it
     * @return the painter object
     */
    public @NonNull Painter setAntiAlias(boolean aa) {
        mPaint.setAntiAlias(aa);
        return this;
    }

    /**
     * Set the Color based on Color
     *
     * @param color int color
     * @return the painter object
     */
    public @NonNull Painter setColor(int color) {
        mPaint.setColor(color);
        return this;
    }

    /**
     * Set the Color based on ID
     *
     * @param colorId
     * @return the painter object
     */
    public @NonNull Painter setColorId(int colorId) {
        mPaint.setColorId(colorId);
        return this;
    }

    /**
     * Set the paint's Join.
     *
     * @param join set the paint's Join, used whenever the paint's style is Stroke or StrokeAndFill.
     */
    public @NonNull Painter setStrokeJoin(Paint.@NonNull Join join) {
        mPaint.setStrokeJoin(join.ordinal());
        return this;
    }

    /**
     * Set the width for stroking. Pass 0 to stroke in hairline mode. Hairlines always draws a
     * single pixel independent of the canvas's matrix.
     *
     * @param width set the paint's stroke width, used whenever the paint's style is Stroke or
     *     StrokeAndFill.
     */
    public @NonNull Painter setStrokeWidth(float width) {
        mPaint.setStrokeWidth(width);
        return this;
    }

    /**
     * Set the paint's style, used for controlling how primitives' geometries are interpreted
     * (except for drawBitmap, which always assumes Fill).
     *
     * @param style The new style to set in the paint
     */
    public @NonNull Painter setStyle(Paint.@NonNull Style style) {
        mPaint.setStyle(style.ordinal());
        return this;
    }

    /**
     * Set the paint's Cap.
     *
     * @param cap set the paint's line cap style, used whenever the paint's style is Stroke or
     *     StrokeAndFill.
     */
    public @NonNull Painter setStrokeCap(Paint.@NonNull Cap cap) {
        mPaint.setStrokeCap(cap.ordinal());
        return this;
    }

    /**
     * Set the paint's stroke miter value. This is used to control the behavior of miter joins when
     * the joins angle is sharp. This value must be >= 0.
     *
     * @param miter set the miter limit on the paint, used whenever the paint's style is Stroke or
     *     StrokeAndFill.
     */
    public @NonNull Painter setStrokeMiter(float miter) {
        mPaint.setStrokeMiter(miter);
        return this;
    }

    /**
     * Helper to setColor(), that only assigns the color's alpha value, leaving its r,g,b values
     * unchanged. Results are undefined if the alpha value is outside of the range [0..1.0]
     *
     * @param alpha set the alpha component [0..1.0] of the paint's color.
     */
    public @NonNull Painter setAlpha(float alpha) {
        mPaint.setAlpha((alpha > 2) ? alpha / 255f : alpha);
        return this;
    }

    /**
     * Create a color filter that uses the specified color and Porter-Duff mode.
     *
     * @param color The ARGB source color used with the specified Porter-Duff mode
     * @param mode The porter-duff mode that is applied
     */
    public @NonNull Painter setPorterDuffColorFilter(int color, PorterDuff.@NonNull Mode mode) {
        mPaint.setColorFilter(color, porterDuffInt(mode));
        return this;
    }

    /** clears the color Filter. the same as paint.setColorFilter(null); */
    public @NonNull Painter clearColorFilter() {
        mPaint.clearColorFilter();
        return this;
    }

    /**
     * sets a shader that draws a linear gradient along a line.
     *
     * @param startX The x-coordinate for the start of the gradient line
     * @param startY The y-coordinate for the start of the gradient line
     * @param endX The x-coordinate for the end of the gradient line
     * @param endY The y-coordinate for the end of the gradient line
     * @param colors The sRGB colors to be distributed along the gradient line
     * @param positions May be null. The relative positions [0..1] of each corresponding color in
     *     the colors array. If this is null, the colors are distributed evenly along the gradient
     *     line.
     * @param tile The Shader tiling mode
     */
    public @NonNull Painter setLinearGradient(
            float startX,
            float startY,
            float endX,
            float endY,
            int @NonNull [] colors,
            float @Nullable [] positions,
            Shader.@NonNull TileMode tile) {
        mPaint.setLinearGradient(colors, 0, positions, startX, startY, endX, endY, tile.ordinal());
        return this;
    }

    /**
     * sets a shader that draws a linear gradient along a line.
     *
     * @param startX The x-coordinate for the start of the gradient line
     * @param startY The y-coordinate for the start of the gradient line
     * @param endX The x-coordinate for the end of the gradient line
     * @param endY The y-coordinate for the end of the gradient line
     * @param colors The sRGB colors to be distributed along the gradient line
     * @param mask defines a bit mask of colors that are ids
     * @param positions May be null. The relative positions [0..1] of each corresponding color in
     *     the colors array. If this is null, the colors are distributed evenly along the gradient
     *     line.
     * @param tile The Shader tiling mode
     */
    public @NonNull Painter setLinearGradient(
            float startX,
            float startY,
            float endX,
            float endY,
            int @NonNull [] colors,
            int mask,
            float @NonNull [] positions,
            Shader.@NonNull TileMode tile) {
        mPaint.setLinearGradient(
                colors, mask, positions, startX, startY, endX, endY, tile.ordinal());
        return this;
    }

    /**
     * Sets a shader that draws a radial gradient given the center and radius.
     *
     * @param centerX The x-coordinate of the center of the radius
     * @param centerY The y-coordinate of the center of the radius
     * @param radius Must be positive. The radius of the circle for this gradient.
     * @param colors The sRGB colors to be distributed between the center and edge of the circle
     * @param positions May be <code>null</code>. Valid values are between <code>0.0f</code> and
     *     <code>1.0f</code>. The relative position of each corresponding color in the colors array.
     *     If <code>null</code>, colors are distributed evenly between the center and edge of the
     *     circle.
     * @param tileMode The Shader tiling mode
     */
    public @NonNull Painter setRadialGradient(
            float centerX,
            float centerY,
            float radius,
            int @NonNull [] colors,
            float @Nullable [] positions,
            Shader.@NonNull TileMode tileMode) {
        mPaint.setRadialGradient(
                colors, 0, positions, centerX, centerY, radius, tileMode.ordinal());
        return this;
    }

    /**
     * Sets a shader that draws a radial gradient given the center and radius.
     *
     * @param centerX The x-coordinate of the center of the radius
     * @param centerY The y-coordinate of the center of the radius
     * @param radius Must be positive. The radius of the circle for this gradient.
     * @param colors The sRGB colors to be distributed between the center and edge of the circle
     * @param mask defines a bit mask of colors that are ids
     * @param positions May be <code>null</code>. Valid values are between <code>0.0f</code> and
     *     <code>1.0f</code>. The relative position of each corresponding color in the colors array.
     *     If <code>null</code>, colors are distributed evenly between the center and edge of the
     *     circle.
     * @param tileMode The Shader tiling mode
     */
    public @NonNull Painter setRadialGradient(
            float centerX,
            float centerY,
            float radius,
            int @NonNull [] colors,
            int mask,
            float @Nullable [] positions,
            Shader.@NonNull TileMode tileMode) {
        mPaint.setRadialGradient(
                colors, mask, positions, centerX, centerY, radius, tileMode.ordinal());
        return this;
    }

    /**
     * Set a shader that draws a sweep gradient around a center point.
     *
     * @param centerX The x-coordinate of the center
     * @param centerY The y-coordinate of the center
     * @param colors The sRGB colors to be distributed between around the center. There must be at
     *     least 2 colors in the array.
     * @param positions May be NULL. The relative position of each corresponding color in the colors
     *     array, beginning with 0 and ending with 1.0. If the values are not monotonic, the drawing
     *     may produce unexpected results. If positions is NULL, then the colors are automatically
     *     spaced evenly.
     */
    public @NonNull Painter setSweepGradient(
            float centerX, float centerY, int @NonNull [] colors, float @Nullable [] positions) {
        mPaint.setSweepGradient(colors, 0, positions, centerX, centerY);
        return this;
    }

    /**
     * Set a shader that draws a sweep gradient around a center point.
     *
     * @param centerX The x-coordinate of the center
     * @param centerY The y-coordinate of the center
     * @param colors The sRGB colors to be distributed between around the center. There must be at
     *     least 2 colors in the array.
     * @param mask defines a bit mask of colors that are ids
     * @param positions May be NULL. The relative position of each corresponding color in the colors
     *     array, beginning with 0 and ending with 1.0. If the values are not monotonic, the drawing
     *     may produce unexpected results. If positions is NULL, then the colors are automatically
     *     spaced evenly.
     * @return painter
     */
    public @NonNull Painter setSweepGradient(
            float centerX,
            float centerY,
            int @NonNull [] colors,
            int mask,
            float @Nullable [] positions) {
        mPaint.setSweepGradient(colors, mask, positions, centerX, centerY);
        return this;
    }

    /**
     * Set the shader matrix used by the gradient/sweep shaders
     *
     * @param matrixId shader matrix id
     * @return the painter object
     */
    public @NonNull Painter setShaderMatrix(float matrixId) {
        mPaint.setShaderMatrix(matrixId);
        return this;
    }

    /**
     * Set the paint's text size. This value must be > 0
     *
     * @param size set the paint's text size in pixel units.
     */
    public @NonNull Painter setTextSize(float size) {
        mPaint.setTextSize(size);
        return this;
    }

    /**
     * sets a typeface object that best matches the specified existing typeface and the specified
     * weight and italic style
     *
     * <p>Below are numerical values and corresponding common weight names.
     *
     * <table> <thead>
     * <tr><th>Value</th><th>Common weight name</th></tr> </thead> <tbody>
     * <tr><td>100</td><td>Thin</td></tr>
     * <tr><td>200</td><td>Extra Light</td></tr>
     * <tr><td>300</td><td>Light</td></tr>
     * <tr><td>400</td><td>Normal</td></tr>
     * <tr><td>500</td><td>Medium</td></tr>
     * <tr><td>600</td><td>Semi Bold</td></tr>
     * <tr><td>700</td><td>Bold</td></tr>
     * <tr><td>800</td><td>Extra Bold</td></tr>
     * <tr><td>900</td><td>Black</td></tr> </tbody> </table>
     *
     * @param fontType 0 = default 1 = sans serif 2 = serif 3 = monospace
     * @param weight The desired weight to be drawn.
     * @param italic {@code true} if italic style is desired to be drawn. Otherwise, {@code false}
     */
    public @NonNull Painter setTypeface(int fontType, int weight, boolean italic) {
        mPaint.setTextStyle(fontType, weight, italic);
        return this;
    }

    /**
     * Set a typeface on the paint
     *
     * @param typeface a typeface
     * @return the painter object
     */
    public @NonNull Painter setTypeface(@NonNull String typeface) {
        int fontType = mBuilder.textCreateId(typeface);
        mPaint.setTextStyle(fontType, NORMAL_WEIGHT, false);
        return this;
    }

    /**
     * Set a typeface on the paint
     *
     * @param fontDataId a typeface
     * @return the painter object
     */
    public @NonNull Painter setTypeface(int fontDataId) {
        mPaint.setTextStyle(fontDataId, NORMAL_WEIGHT, false, true);
        return this;
    }

    /**
     * Set a typeface on the paint
     *
     * @param typeface a typeface
     * @return the painter object
     */
    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.P)
    public @NonNull Painter setTypeface(@Nullable Typeface typeface) {
        if (typeface != null) {
            int fontType = PaintBundle.FONT_TYPE_DEFAULT;
            if (typeface.equals(Typeface.SANS_SERIF)) {
                fontType = PaintBundle.FONT_TYPE_SANS_SERIF;
            } else if (typeface.equals(Typeface.SERIF)) {
                fontType = PaintBundle.FONT_TYPE_SERIF;
            } else if (typeface.equals(Typeface.MONOSPACE)) {
                fontType = PaintBundle.FONT_TYPE_MONOSPACE;
            } else if (typeface.equals(Typeface.DEFAULT_BOLD)) {
                fontType = PaintBundle.FONT_TYPE_DEFAULT;
            }
            mPaint.setTextStyle(fontType, typeface.getWeight(), typeface.isItalic());
        } else {
            mPaint.setTextStyle(0, NORMAL_WEIGHT, false);
            // generic font
        }
        return this;
    }

    /**
     * Set filter bitmap
     *
     * @param filter set to false to disable interpolation
     * @return the painter object
     */
    public @NonNull Painter setFilterBitmap(boolean filter) {
        mPaint.setFilterBitmap(filter);
        return this;
    }

    /**
     * Set the blendmode
     *
     * @param blend blend mode
     * @return the painter object
     */
    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.Q)
    public @NonNull Painter setBlendMode(@NonNull BlendMode blend) {
        mPaint.setBlendMode(toBlendMode(blend));
        return this;
    }

    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.Q)
    private int toBlendMode(@Nullable BlendMode blend) {
        if (blend == null) {
            return PaintBundle.BLEND_MODE_NULL;
        }
        switch (blend) {
            case CLEAR:
                return PaintBundle.BLEND_MODE_CLEAR;
            case SRC:
                return PaintBundle.BLEND_MODE_SRC;
            case DST:
                return PaintBundle.BLEND_MODE_DST;
            case SRC_OVER:
                return PaintBundle.BLEND_MODE_SRC_OVER;
            case DST_OVER:
                return PaintBundle.BLEND_MODE_DST_OVER;
            case SRC_IN:
                return PaintBundle.BLEND_MODE_SRC_IN;
            case DST_IN:
                return PaintBundle.BLEND_MODE_DST_IN;
            case SRC_OUT:
                return PaintBundle.BLEND_MODE_SRC_OUT;
            case DST_OUT:
                return PaintBundle.BLEND_MODE_DST_OUT;
            case SRC_ATOP:
                return PaintBundle.BLEND_MODE_SRC_ATOP;
            case DST_ATOP:
                return PaintBundle.BLEND_MODE_DST_ATOP;
            case XOR:
                return PaintBundle.BLEND_MODE_XOR;
            case PLUS:
                return PaintBundle.BLEND_MODE_PLUS;
            case MODULATE:
                return PaintBundle.BLEND_MODE_MODULATE;
            case SCREEN:
                return PaintBundle.BLEND_MODE_SCREEN;
            case OVERLAY:
                return PaintBundle.BLEND_MODE_OVERLAY;
            case DARKEN:
                return PaintBundle.BLEND_MODE_DARKEN;
            case LIGHTEN:
                return PaintBundle.BLEND_MODE_LIGHTEN;
            case COLOR_DODGE:
                return PaintBundle.BLEND_MODE_COLOR_DODGE;
            case COLOR_BURN:
                return PaintBundle.BLEND_MODE_COLOR_BURN;
            case HARD_LIGHT:
                return PaintBundle.BLEND_MODE_HARD_LIGHT;
            case SOFT_LIGHT:
                return PaintBundle.BLEND_MODE_SOFT_LIGHT;
            case DIFFERENCE:
                return PaintBundle.BLEND_MODE_DIFFERENCE;
            case EXCLUSION:
                return PaintBundle.BLEND_MODE_EXCLUSION;
            case MULTIPLY:
                return PaintBundle.BLEND_MODE_MULTIPLY;
            case HUE:
                return PaintBundle.BLEND_MODE_HUE;
            case SATURATION:
                return PaintBundle.BLEND_MODE_SATURATION;
            case COLOR:
                return PaintBundle.BLEND_MODE_COLOR;
            case LUMINOSITY:
                return PaintBundle.BLEND_MODE_LUMINOSITY;
        }
        return PaintBundle.BLEND_MODE_NULL;
    }

    /**
     * Set the shader in the current paint
     *
     * @param id shader id
     * @return the painter object
     */
    public @NonNull Painter setShader(int id) {
        mPaint.setShader(id);
        return this;
    }

    /**
     * Convert PorterDuff.Mode enum to int value
     *
     * @param mode PorterDuff.Mode enum value
     * @return corresponding PaintBundle blend mode int value
     */
    public static int porterDuffInt(PorterDuff.@NonNull Mode mode) {
        switch (mode) {
            case CLEAR:
                return PaintBundle.BLEND_MODE_CLEAR;
            case SRC:
                return PaintBundle.BLEND_MODE_SRC;
            case DST:
                return PaintBundle.BLEND_MODE_DST;
            case DST_OVER:
                return PaintBundle.BLEND_MODE_DST_OVER;
            case SRC_IN:
                return PaintBundle.BLEND_MODE_SRC_IN;
            case DST_IN:
                return PaintBundle.BLEND_MODE_DST_IN;
            case SRC_OUT:
                return PaintBundle.BLEND_MODE_SRC_OUT;
            case DST_OUT:
                return PaintBundle.BLEND_MODE_DST_OUT;
            case SRC_ATOP:
                return PaintBundle.BLEND_MODE_SRC_ATOP;
            case DST_ATOP:
                return PaintBundle.BLEND_MODE_DST_ATOP;
            case XOR:
                return PaintBundle.BLEND_MODE_XOR;
            case DARKEN:
                return PaintBundle.BLEND_MODE_DARKEN;
            case LIGHTEN:
                return PaintBundle.BLEND_MODE_LIGHTEN;
            case MULTIPLY:
                return PaintBundle.BLEND_MODE_MULTIPLY;
            case SCREEN:
                return PaintBundle.BLEND_MODE_SCREEN;
            case ADD:
                return PaintBundle.PORTER_MODE_ADD;
            case OVERLAY:
                return PaintBundle.BLEND_MODE_OVERLAY;
            case SRC_OVER:
            default:
                return PaintBundle.BLEND_MODE_SRC_OVER;
        }
    }

    /**
     * Set the Font axis
     *
     * @param tags array of tag names
     * @param values array of values
     * @return the painter object
     */
    public @NonNull Painter setAxis(String @NonNull [] tags, float @NonNull [] values) {
        int[] tagIds = new int[tags.length];
        for (int i = 0; i < tags.length; i++) {
            tagIds[i] = mBuilder.textCreateId(tags[i]);
        }
        mPaint.setTextAxis(tagIds, values);
        return this;
    }

    /**
     * Set the Font axis
     *
     * @param tagIds array of tag ids
     * @param values array of values
     * @return the painter object
     */
    public @NonNull Painter setAxis(int @NonNull [] tagIds, float @NonNull [] values) {
        mPaint.setTextAxis(tagIds, values);
        return this;
    }

    /**
     * Set the texture shader tileMode are: <br>
     * 0=CLAMP Replicate the edge color<br>
     * 1=REPEAT Repeat the shader's image, <br>
     * 2=MIRROR Repeat the shader's image mirror alternative sections <br>
     * 3=DECAL Clamp to transparent outside image<br>
     *
     * @param texture id of bitmap to use as texture
     * @param tileModeX tile mode for x
     * @param tileModeY tile mode for y
     * @param filterMode filter mode, 0 = no filter, 1 = linear, 2 = nearest
     * @param maxAnisotropy max anisotropy, 0 = no anisotropy
     * @return the painter object
     */
    public @NonNull Painter setTextureShader(
            int texture, short tileModeX, short tileModeY, short filterMode, short maxAnisotropy) {
        mPaint.setTextureShader(texture, tileModeX, tileModeY, filterMode, maxAnisotropy);
        return this;
    }
}
