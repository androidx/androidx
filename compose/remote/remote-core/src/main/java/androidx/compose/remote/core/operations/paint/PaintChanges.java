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
package androidx.compose.remote.core.operations.paint;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Interface to a paint object For more details see Android Paint */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface PaintChanges {

    // MASK to be set/cleared
    int CLEAR_TEXT_SIZE = 1 << (PaintBundle.TEXT_SIZE - 1);
    int CLEAR_TEXT_STYLE = 1 << (PaintBundle.TYPEFACE - 1);
    int CLEAR_COLOR = 1 << (PaintBundle.COLOR - 1);
    int CLEAR_STROKE_WIDTH = 1 << (PaintBundle.STROKE_WIDTH - 1);
    int CLEAR_STROKE_MITER = 1 << (PaintBundle.STROKE_MITER - 1);
    int CLEAR_CAP = 1 << (PaintBundle.STROKE_CAP - 1);
    int CLEAR_STYLE = 1 << (PaintBundle.STYLE - 1);
    int CLEAR_SHADER = 1 << (PaintBundle.SHADER - 1);
    int CLEAR_IMAGE_FILTER_QUALITY = 1 << (PaintBundle.IMAGE_FILTER_QUALITY - 1);
    int CLEAR_RADIENT = 1 << (PaintBundle.GRADIENT - 1);
    int CLEAR_ALPHA = 1 << (PaintBundle.ALPHA - 1);
    int CLEAR_COLOR_FILTER = 1 << (PaintBundle.COLOR_FILTER - 1);
    int VALID_BITS = 0x1FFF; // only the first 13 bit are valid now

    /**
     * Set the size of text
     *
     * @param size
     */
    void setTextSize(float size);

    /**
     * Set the width of lines
     *
     * @param width
     */
    void setStrokeWidth(float width);

    /**
     * Set the color to use
     *
     * @param color
     */
    void setColor(int color);

    /**
     * Set the Stroke Cap
     *
     * @param cap
     */
    void setStrokeCap(int cap);

    /**
     * Set the Stroke style FILL and/or STROKE
     *
     * @param style
     */
    void setStyle(int style);

    /**
     * Set the id of the shader to use
     *
     * @param shader
     */
    void setShader(int shader);

    /**
     * Set the way image is interpolated
     *
     * @param quality
     */
    void setImageFilterQuality(int quality);

    /**
     * Set the alpha to draw under
     *
     * @param a
     */
    void setAlpha(float a);

    /**
     * Set the Stroke Miter
     *
     * @param miter
     */
    void setStrokeMiter(float miter);

    /**
     * Set the Stroke Join
     *
     * @param join
     */
    void setStrokeJoin(int join);

    /**
     * Should bitmaps be interpolated
     *
     * @param filter
     */
    void setFilterBitmap(boolean filter);

    /**
     * Set the blend mode can be porterduff + others
     *
     * @param mode
     */
    void setBlendMode(int mode);

    /**
     * Set the AntiAlias. Typically true Set to off when you need pixilated look (e.g. QR codes)
     *
     * @param aa
     */
    void setAntiAlias(boolean aa);

    /**
     * Clear some sub set of the settings
     *
     * @param mask
     */
    void clear(long mask);

    /**
     * Set a linear gradient fill
     *
     * @param colorsArray
     * @param stopsArray // todo: standardize naming
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     * @param tileMode
     */
    void setLinearGradient(
            int @NonNull [] colorsArray,
            float @Nullable [] stopsArray,
            float startX,
            float startY,
            float endX,
            float endY,
            int tileMode);

    /**
     * Set a radial gradient fill
     *
     * @param colorsArray
     * @param stopsArray // todo: standardize naming
     * @param centerX
     * @param centerY
     * @param radius
     * @param tileMode
     */
    void setRadialGradient(
            int @NonNull [] colorsArray,
            float @Nullable [] stopsArray,
            float centerX,
            float centerY,
            float radius,
            int tileMode);

    /**
     * Set a sweep gradient fill
     *
     * @param colorsArray
     * @param stopsArray // todo: standardize naming to either "positions" or "stops"
     * @param centerX
     * @param centerY
     */
    void setSweepGradient(
            int @NonNull [] colorsArray,
            float @Nullable [] stopsArray,
            float centerX,
            float centerY);

    /**
     * Set Color filter mod
     *
     * @param color
     * @param mode
     */
    void setColorFilter(int color, int mode);

    /**
     * Set TypeFace 0,1,2
     *
     * @param fontType the type of font 0,1,or 2
     * @param weight the weight of the font
     * @param italic if the font is italic
     */
    void setTypeFace(int fontType, int weight, boolean italic);

    /**
     * Set the shader matrix
     *
     * @param matrixId the id of the matrix
     */
    void setShaderMatrix(float matrixId);

    /**
     * @param fontType String to be looked up in system
     * @param weight the weight of the font
     * @param italic if the font is italic
     */
    void setTypeFace(@NonNull String fontType, int weight, boolean italic);

    /**
     * Set the font variation axes
     *
     * @param tags tags
     * @param values values
     */
    void setFontVariationAxes(@NonNull String[] tags, float @NonNull [] values);

    /**
     * Set the texture shader
     *
     * @param bitmapId the id of the bitmap to use
     * @param tileX The tiling mode for x to draw the bitmap in.
     * @param tileY The tiling mode for y to draw the bitmap in.
     * @param filterMode the filter mode to be used when sampling from this shader.
     * @param maxAnisotropy The Anisotropy value to use for filtering. Must be greater than 0.
     */
    void setTextureShader(
            int bitmapId, short tileX, short tileY, short filterMode, short maxAnisotropy);
}
