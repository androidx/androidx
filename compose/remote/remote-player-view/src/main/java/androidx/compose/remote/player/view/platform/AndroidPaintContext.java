/*
 * Copyright (C) 2023 The Android Open Source Project
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
package androidx.compose.remote.player.view.platform;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.compose.remote.core.RemoteComposeState.BITMAP_TEXTURE_ID_OFFSET;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.graphics.fonts.Font;
import android.graphics.fonts.FontFamily;
import android.graphics.fonts.FontStyle;
import android.graphics.fonts.FontVariationAxis;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.MatrixAccess;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.Platform;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.ClipPath;
import androidx.compose.remote.core.operations.ShaderData;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.managers.TextLayout;
import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation;
import androidx.compose.remote.core.operations.paint.PaintBundle;
import androidx.compose.remote.core.operations.paint.PaintChanges;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * An implementation of PaintContext for the Android Canvas. This is used to play the RemoteCompose
 * operations on Android.
 */
@RestrictTo(LIBRARY_GROUP)
public class AndroidPaintContext extends PaintContext {
    private static final String SYSTEM_FONTS_PATH = "/system/fonts/";
    Paint mPaint = new Paint();
    List<Paint> mPaintList = new ArrayList<>();
    Canvas mCanvas;
    Canvas mMainCanvas = null;
    Rect mTmpRect = new Rect(); // use in calculation of bounds
    RenderNode mNode = null;
    Canvas mPreviousCanvas = null;

    public AndroidPaintContext(RemoteContext context, Canvas canvas) {
        super(context);
        this.mCanvas = canvas;
    }

    public Canvas getCanvas() {
        return mCanvas;
    }

    public void setCanvas(Canvas canvas) {
        this.mCanvas = mMainCanvas = canvas;
    }

    @Override
    public void save() {
        mCanvas.save();
    }

    @Override
    public void saveLayer(float x, float y, float width, float height) {
        mCanvas.saveLayer(x, y, x + width, y + height, mPaint);
    }

    @Override
    public void restore() {
        mCanvas.restore();
    }

    /**
     * Draw an image onto the canvas
     *
     * @param imageId   the id of the image
     * @param srcLeft   left coordinate of the source area
     * @param srcTop    top coordinate of the source area
     * @param srcRight  right coordinate of the source area
     * @param srcBottom bottom coordinate of the source area
     * @param dstLeft   left coordinate of the destination area
     * @param dstTop    top coordinate of the destination area
     * @param dstRight  right coordinate of the destination area
     * @param dstBottom bottom coordinate of the destination area
     * @param cdId      the id of the content description
     */
    @Override
    public void drawBitmap(
            int imageId,
            int srcLeft,
            int srcTop,
            int srcRight,
            int srcBottom,
            int dstLeft,
            int dstTop,
            int dstRight,
            int dstBottom,
            int cdId) {
        AndroidRemoteContext androidContext = (AndroidRemoteContext) mContext;
        if (androidContext.mRemoteComposeState.containsId(imageId)) {
            Bitmap bitmap = (Bitmap) androidContext.mRemoteComposeState.getFromId(imageId);
            mCanvas.drawBitmap(
                    bitmap,
                    new Rect(srcLeft, srcTop, srcRight, srcBottom),
                    new Rect(dstLeft, dstTop, dstRight, dstBottom),
                    mPaint);
        }
    }

    @Override
    public void scale(float scaleX, float scaleY) {
        mCanvas.scale(scaleX, scaleY);
    }

    @Override
    public void startGraphicsLayer(int w, int h) {
        mNode = new RenderNode("layer");
        mNode.setPosition(0, 0, w, h);
        mPreviousCanvas = mCanvas;
        mCanvas = mNode.beginRecording();
    }

    @Override
    public void setGraphicsLayer(@NonNull HashMap<Integer, Object> attributes) {
        if (mNode == null) {
            return;
        }
        boolean hasBlurEffect = false;
        boolean hasOutline = false;
        for (Integer key : attributes.keySet()) {
            Object value = attributes.get(key);
            switch (key) {
                case GraphicsLayerModifierOperation.SCALE_X:
                    mNode.setScaleX((Float) value);
                    break;
                case GraphicsLayerModifierOperation.SCALE_Y:
                    mNode.setScaleY((Float) value);
                    break;
                case GraphicsLayerModifierOperation.ROTATION_X:
                    mNode.setRotationX((Float) value);
                    break;
                case GraphicsLayerModifierOperation.ROTATION_Y:
                    mNode.setRotationY((Float) value);
                    break;
                case GraphicsLayerModifierOperation.ROTATION_Z:
                    mNode.setRotationZ((Float) value);
                    break;
                case GraphicsLayerModifierOperation.TRANSFORM_ORIGIN_X:
                    mNode.setPivotX((Float) value * mNode.getWidth());
                    break;
                case GraphicsLayerModifierOperation.TRANSFORM_ORIGIN_Y:
                    mNode.setPivotY((Float) value * mNode.getWidth());
                    break;
                case GraphicsLayerModifierOperation.TRANSLATION_X:
                    mNode.setTranslationX((Float) value);
                    break;
                case GraphicsLayerModifierOperation.TRANSLATION_Y:
                    mNode.setTranslationY((Float) value);
                    break;
                case GraphicsLayerModifierOperation.TRANSLATION_Z:
                    mNode.setTranslationZ((Float) value);
                    break;
                case GraphicsLayerModifierOperation.SHAPE:
                    hasOutline = true;
                    break;
                case GraphicsLayerModifierOperation.SHADOW_ELEVATION:
                    mNode.setElevation((Float) value);
                    break;
                case GraphicsLayerModifierOperation.ALPHA:
                    mNode.setAlpha((Float) value);
                    break;
                case GraphicsLayerModifierOperation.CAMERA_DISTANCE:
                    mNode.setCameraDistance((Float) value);
                    break;
                case GraphicsLayerModifierOperation.SPOT_SHADOW_COLOR:
                    mNode.setSpotShadowColor((Integer) value);
                    break;
                case GraphicsLayerModifierOperation.AMBIENT_SHADOW_COLOR:
                    mNode.setAmbientShadowColor((Integer) value);
                    break;
                case GraphicsLayerModifierOperation.HAS_BLUR:
                    hasBlurEffect = ((Integer) value) != 0;
                    break;
            }
        }
        if (hasOutline) {
            Outline outline = new Outline();
            outline.setAlpha(1f);
            Object oShape = attributes.get(GraphicsLayerModifierOperation.SHAPE);
            if (oShape != null) {
                Object oShapeRadius = attributes.get(GraphicsLayerModifierOperation.SHAPE_RADIUS);
                int type = (Integer) oShape;
                if (type == GraphicsLayerModifierOperation.SHAPE_RECT) {
                    outline.setRect(0, 0, mNode.getWidth(), mNode.getHeight());
                } else if (type == GraphicsLayerModifierOperation.SHAPE_ROUND_RECT) {
                    if (oShapeRadius != null) {
                        float radius = (Float) oShapeRadius;
                        outline.setRoundRect(
                                new Rect(0, 0, mNode.getWidth(), mNode.getHeight()), radius);
                    } else {
                        outline.setRect(0, 0, mNode.getWidth(), mNode.getHeight());
                    }
                } else if (type == GraphicsLayerModifierOperation.SHAPE_CIRCLE) {
                    float radius = Math.min(mNode.getWidth(), mNode.getHeight()) / 2f;
                    outline.setRoundRect(
                            new Rect(0, 0, mNode.getWidth(), mNode.getHeight()), radius);
                }
            }
            mNode.setOutline(outline);
        }
        if (hasBlurEffect) {
            Object oBlurRadiusX = attributes.get(GraphicsLayerModifierOperation.BLUR_RADIUS_X);
            float blurRadiusX = 0f;
            if (oBlurRadiusX != null) {
                blurRadiusX = (Float) oBlurRadiusX;
            }
            Object oBlurRadiusY = attributes.get(GraphicsLayerModifierOperation.BLUR_RADIUS_Y);
            float blurRadiusY = 0f;
            if (oBlurRadiusY != null) {
                blurRadiusY = (Float) oBlurRadiusY;
            }
            int blurTileMode = 0;
            Object oBlurTileMode = attributes.get(GraphicsLayerModifierOperation.BLUR_TILE_MODE);
            if (oBlurTileMode != null) {
                blurTileMode = (Integer) oBlurTileMode;
            }
            Shader.TileMode tileMode = Shader.TileMode.CLAMP;
            switch (blurTileMode) {
                case GraphicsLayerModifierOperation.TILE_MODE_CLAMP:
                    tileMode = Shader.TileMode.CLAMP;
                    break;
                case GraphicsLayerModifierOperation.TILE_MODE_DECAL:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // REMOVE IN PLATFORM
                        tileMode = Shader.TileMode.DECAL;
                    } // REMOVE IN PLATFORM
                    break;
                case GraphicsLayerModifierOperation.TILE_MODE_MIRROR:
                    tileMode = Shader.TileMode.MIRROR;
                    break;
                case GraphicsLayerModifierOperation.TILE_MODE_REPEATED:
                    tileMode = Shader.TileMode.REPEAT;
                    break;
            }
            if (android.os.Build.VERSION.SDK_INT // REMOVE IN PLATFORM
                    >= android.os.Build.VERSION_CODES.S) { // REMOVE IN PLATFORM
                RenderEffect effect =
                        RenderEffect.createBlurEffect(blurRadiusX, blurRadiusY, tileMode);
                mNode.setRenderEffect(effect);
            } // REMOVE IN PLATFORM
        }
    }

    @Override
    public void endGraphicsLayer() {
        mNode.endRecording();
        mCanvas = mPreviousCanvas;
        if (mCanvas.isHardwareAccelerated()) {
            mCanvas.enableZ();
            mCanvas.drawRenderNode(mNode);
            mCanvas.disableZ();
        }
        // node.discardDisplayList();
        mNode = null;
    }

    @Override
    public void translate(float translateX, float translateY) {
        mCanvas.translate(translateX, translateY);
    }

    @Override
    public void drawArc(
            float left, float top, float right, float bottom, float startAngle, float sweepAngle) {
        mCanvas.drawArc(left, top, right, bottom, startAngle, sweepAngle, false, mPaint);
    }

    @Override
    public void drawSector(
            float left, float top, float right, float bottom, float startAngle, float sweepAngle) {
        mCanvas.drawArc(left, top, right, bottom, startAngle, sweepAngle, true, mPaint);
    }

    @Override
    public void drawBitmap(int id, float left, float top, float right, float bottom) {
        AndroidRemoteContext androidContext = (AndroidRemoteContext) mContext;
        if (androidContext.mRemoteComposeState.containsId(id)) {
            Bitmap bitmap = (Bitmap) androidContext.mRemoteComposeState.getFromId(id);
            Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            RectF dst = new RectF(left, top, right, bottom);
            mCanvas.drawBitmap(bitmap, src, dst, mPaint);
        }
    }

    @Override
    public void drawCircle(float centerX, float centerY, float radius) {
        mCanvas.drawCircle(centerX, centerY, radius, mPaint);
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2) {
        mCanvas.drawLine(x1, y1, x2, y2, mPaint);
    }

    @Override
    public void drawOval(float left, float top, float right, float bottom) {
        mCanvas.drawOval(left, top, right, bottom, mPaint);
    }

    @Override
    public void drawPath(int id, float start, float end) {
        mCanvas.drawPath(getPath(id, start, end), mPaint);
    }

    @Override
    public void drawRect(float left, float top, float right, float bottom) {
        mCanvas.drawRect(left, top, right, bottom, mPaint);
    }

    @Override
    public void savePaint() {
        mPaintList.add(new Paint(mPaint));
    }

    @Override
    public void restorePaint() {
        mPaint = mPaintList.remove(mPaintList.size() - 1);
    }

    @Override
    public void replacePaint(@NonNull PaintBundle paintBundle) {
        mPaint.reset();
        applyPaint(paintBundle);
    }

    @Override
    public void drawRoundRect(
            float left, float top, float right, float bottom, float radiusX, float radiusY) {
        mCanvas.drawRoundRect(left, top, right, bottom, radiusX, radiusY, mPaint);
    }

    @Override
    public void drawTextOnPath(int textId, int pathId, float hOffset, float vOffset) {
        mCanvas.drawTextOnPath(getText(textId), getPath(pathId, 0, 1), hOffset, vOffset, mPaint);
    }

    private Paint.FontMetrics mCachedFontMetrics;

    @Override
    public void getTextBounds(int textId, int start, int end, int flags, float @NonNull [] bounds) {
        String str = getText(textId);
        if (str == null) {
            bounds[0] = 0f;
            bounds[1] = 0f;
            bounds[2] = 0f;
            bounds[3] = 0f;
            return;
        }
        if (end == -1 || end > str.length()) {
            end = str.length();
        }

        if (mCachedFontMetrics == null) {
            mCachedFontMetrics = mPaint.getFontMetrics();
        }
        mPaint.getFontMetrics(mCachedFontMetrics);
        mPaint.getTextBounds(str, start, end, mTmpRect);
        if ((flags & PaintContext.TEXT_MEASURE_SPACES) != 0) {
            bounds[0] = 0f;
            bounds[2] = mPaint.measureText(str, start, end);
        } else {
            bounds[0] = mTmpRect.left;
            if ((flags & PaintContext.TEXT_MEASURE_MONOSPACE_WIDTH) != 0) {
                bounds[2] = mPaint.measureText(str, start, end) - mTmpRect.left;
            } else {
                bounds[2] = mTmpRect.right;
            }
        }

        if ((flags & PaintContext.TEXT_MEASURE_FONT_HEIGHT) != 0) {
            bounds[1] = Math.round(mCachedFontMetrics.ascent);
            bounds[3] = Math.round(mCachedFontMetrics.descent);
        } else {
            bounds[1] = mTmpRect.top;
            bounds[3] = mTmpRect.bottom;
        }
    }

    @Override
    public Platform.@Nullable ComputedTextLayout layoutComplexText(
            int textId,
            int start,
            int end,
            int alignment,
            int overflow,
            int maxLines,
            float maxWidth,
            int flags) {
        String str = getText(textId);
        if (str == null) {
            return null;
        }
        if (end == -1 || end > str.length()) {
            end = str.length();
        }

        TextPaint textPaint = new TextPaint();
        textPaint.set(mPaint);
        StaticLayout.Builder staticLayoutBuilder =
                StaticLayout.Builder.obtain(str, start, end, textPaint, (int) maxWidth);
        switch (alignment) {
            case TextLayout.TEXT_ALIGN_RIGHT:
            case TextLayout.TEXT_ALIGN_END:
                staticLayoutBuilder.setAlignment(Layout.Alignment.ALIGN_OPPOSITE);
                break;
            case TextLayout.TEXT_ALIGN_CENTER:
                staticLayoutBuilder.setAlignment(Layout.Alignment.ALIGN_CENTER);
                break;
            default:
                staticLayoutBuilder.setAlignment(Layout.Alignment.ALIGN_NORMAL);
        }
        switch (overflow) {
            case TextLayout.OVERFLOW_ELLIPSIS:
                staticLayoutBuilder.setEllipsize(TextUtils.TruncateAt.END);
                break;
            case TextLayout.OVERFLOW_MIDDLE_ELLIPSIS:
                staticLayoutBuilder.setEllipsize(TextUtils.TruncateAt.MIDDLE);
                break;
            case TextLayout.OVERFLOW_START_ELLIPSIS:
                staticLayoutBuilder.setEllipsize(TextUtils.TruncateAt.START);
                break;
            default:
        }
        staticLayoutBuilder.setMaxLines(maxLines);
        staticLayoutBuilder.setIncludePad(false);

        StaticLayout staticLayout = staticLayoutBuilder.build();
        return new AndroidComputedTextLayout(
                staticLayout, staticLayout.getWidth(), staticLayout.getHeight());
    }

    @Override
    public void drawTextRun(
            int textId,
            int start,
            int end,
            int contextStart,
            int contextEnd,
            float x,
            float y,
            boolean rtl) {

        String textToPaint = getText(textId);
        if (textToPaint == null) {
            return;
        }
        if (end == -1) {
            if (start != 0) {
                textToPaint = textToPaint.substring(start);
            }
        } else if (end > textToPaint.length()) {
            textToPaint = textToPaint.substring(start);
        } else {
            textToPaint = textToPaint.substring(start, end);
        }

        mCanvas.drawText(textToPaint, x, y, mPaint);
    }

    @Override
    public void drawComplexText(Platform.@Nullable ComputedTextLayout computedTextLayout) {
        if (computedTextLayout == null) {
            return;
        }
        StaticLayout staticLayout = ((AndroidComputedTextLayout) computedTextLayout).get();
        staticLayout.draw(mCanvas);
    }

    @Override
    public void drawTweenPath(int path1Id, int path2Id, float tween, float start, float end) {
        mCanvas.drawPath(getPath(path1Id, path2Id, tween, start, end), mPaint);
    }

    private static PorterDuff.Mode remoteToAndroidPorterDuffMode(int mode) {
        switch (mode) {
            case PaintBundle.BLEND_MODE_CLEAR:
                return PorterDuff.Mode.CLEAR;
            case PaintBundle.BLEND_MODE_SRC:
                return PorterDuff.Mode.SRC;
            case PaintBundle.BLEND_MODE_DST:
                return PorterDuff.Mode.DST;
            case PaintBundle.BLEND_MODE_SRC_OVER:
                return PorterDuff.Mode.SRC_OVER;
            case PaintBundle.BLEND_MODE_DST_OVER:
                return PorterDuff.Mode.DST_OVER;
            case PaintBundle.BLEND_MODE_SRC_IN:
                return PorterDuff.Mode.SRC_IN;
            case PaintBundle.BLEND_MODE_DST_IN:
                return PorterDuff.Mode.DST_IN;
            case PaintBundle.BLEND_MODE_SRC_OUT:
                return PorterDuff.Mode.SRC_OUT;
            case PaintBundle.BLEND_MODE_DST_OUT:
                return PorterDuff.Mode.DST_OUT;
            case PaintBundle.BLEND_MODE_SRC_ATOP:
                return PorterDuff.Mode.SRC_ATOP;
            case PaintBundle.BLEND_MODE_DST_ATOP:
                return PorterDuff.Mode.DST_ATOP;
            case PaintBundle.BLEND_MODE_XOR:
                return PorterDuff.Mode.XOR;
            case PaintBundle.BLEND_MODE_SCREEN:
                return PorterDuff.Mode.SCREEN;
            case PaintBundle.BLEND_MODE_OVERLAY:
                return PorterDuff.Mode.OVERLAY;
            case PaintBundle.BLEND_MODE_DARKEN:
                return PorterDuff.Mode.DARKEN;
            case PaintBundle.BLEND_MODE_LIGHTEN:
                return PorterDuff.Mode.LIGHTEN;
            case PaintBundle.BLEND_MODE_MULTIPLY:
                return PorterDuff.Mode.MULTIPLY;
            case PaintBundle.PORTER_MODE_ADD:
                return PorterDuff.Mode.ADD;
        }
        return PorterDuff.Mode.SRC_OVER;
    }

    private static BlendMode remoteToAndroidBlendMode(int mode) {
        switch (mode) {
            case PaintBundle.BLEND_MODE_CLEAR:
                return BlendMode.CLEAR;
            case PaintBundle.BLEND_MODE_SRC:
                return BlendMode.SRC;
            case PaintBundle.BLEND_MODE_DST:
                return BlendMode.DST;
            case PaintBundle.BLEND_MODE_SRC_OVER:
                return BlendMode.SRC_OVER;
            case PaintBundle.BLEND_MODE_DST_OVER:
                return BlendMode.DST_OVER;
            case PaintBundle.BLEND_MODE_SRC_IN:
                return BlendMode.SRC_IN;
            case PaintBundle.BLEND_MODE_DST_IN:
                return BlendMode.DST_IN;
            case PaintBundle.BLEND_MODE_SRC_OUT:
                return BlendMode.SRC_OUT;
            case PaintBundle.BLEND_MODE_DST_OUT:
                return BlendMode.DST_OUT;
            case PaintBundle.BLEND_MODE_SRC_ATOP:
                return BlendMode.SRC_ATOP;
            case PaintBundle.BLEND_MODE_DST_ATOP:
                return BlendMode.DST_ATOP;
            case PaintBundle.BLEND_MODE_XOR:
                return BlendMode.XOR;
            case PaintBundle.BLEND_MODE_PLUS:
                return BlendMode.PLUS;
            case PaintBundle.BLEND_MODE_MODULATE:
                return BlendMode.MODULATE;
            case PaintBundle.BLEND_MODE_SCREEN:
                return BlendMode.SCREEN;
            case PaintBundle.BLEND_MODE_OVERLAY:
                return BlendMode.OVERLAY;
            case PaintBundle.BLEND_MODE_DARKEN:
                return BlendMode.DARKEN;
            case PaintBundle.BLEND_MODE_LIGHTEN:
                return BlendMode.LIGHTEN;
            case PaintBundle.BLEND_MODE_COLOR_DODGE:
                return BlendMode.COLOR_DODGE;
            case PaintBundle.BLEND_MODE_COLOR_BURN:
                return BlendMode.COLOR_BURN;
            case PaintBundle.BLEND_MODE_HARD_LIGHT:
                return BlendMode.HARD_LIGHT;
            case PaintBundle.BLEND_MODE_SOFT_LIGHT:
                return BlendMode.SOFT_LIGHT;
            case PaintBundle.BLEND_MODE_DIFFERENCE:
                return BlendMode.DIFFERENCE;
            case PaintBundle.BLEND_MODE_EXCLUSION:
                return BlendMode.EXCLUSION;
            case PaintBundle.BLEND_MODE_MULTIPLY:
                return BlendMode.MULTIPLY;
            case PaintBundle.BLEND_MODE_HUE:
                return BlendMode.HUE;
            case PaintBundle.BLEND_MODE_SATURATION:
                return BlendMode.SATURATION;
            case PaintBundle.BLEND_MODE_COLOR:
                return BlendMode.COLOR;
            case PaintBundle.BLEND_MODE_LUMINOSITY:
                return BlendMode.LUMINOSITY;
            case PaintBundle.BLEND_MODE_NULL:
                return null;
        }
        return null;
    }

    PaintChanges mCachedPaintChanges =
            new PaintChanges() {
                private Font.Builder mFontBuilder;
                final Matrix mTmpMatrix = new Matrix();

                @Override
                public void setTextSize(float size) {
                    mPaint.setTextSize(size);
                }

                @Override
                public void setTypeFace(int fontType, int weight, boolean italic) {
                    switch (fontType) {
                        case PaintBundle.FONT_TYPE_DEFAULT:
                            if (weight == 400 && !italic) { // for normal case
                                mPaint.setTypeface(Typeface.DEFAULT);
                            } else {
                                mPaint.setTypeface(
                                        Typeface.create(Typeface.DEFAULT, weight, italic));
                            }
                            break;
                        case PaintBundle.FONT_TYPE_SERIF:
                            if (weight == 400 && !italic) { // for normal case
                                mPaint.setTypeface(Typeface.SERIF);
                            } else {
                                mPaint.setTypeface(Typeface.create(Typeface.SERIF, weight, italic));
                            }
                            break;
                        case PaintBundle.FONT_TYPE_SANS_SERIF:
                            if (weight == 400 && !italic) { //  for normal case
                                mPaint.setTypeface(Typeface.SANS_SERIF);
                            } else {
                                mPaint.setTypeface(
                                        Typeface.create(Typeface.SANS_SERIF, weight, italic));
                            }
                            break;
                        case PaintBundle.FONT_TYPE_MONOSPACE:
                            if (weight == 400 && !italic) { //  for normal case
                                mPaint.setTypeface(Typeface.MONOSPACE);
                            } else {
                                mPaint.setTypeface(
                                        Typeface.create(Typeface.MONOSPACE, weight, italic));
                            }

                            break;
                        default: // font data
                            RemoteContext.FontInfo fi =
                                    (RemoteContext.FontInfo) mContext.getObject(fontType);
                            Font.Builder builder = (Font.Builder) fi.fontBuilder;
                            if (builder == null) {
                                fi.fontBuilder =
                                        builder = createFontBuilder(fi.mFontData, weight, italic);
                            }
                            mFontBuilder = builder;
                            setAxis(null);

                            break;
                    }
                }

                @Override
                public void setShaderMatrix(float matrixId) {
                    int id = Utils.idFromNan(matrixId);
                    if (id == 0) {
                        mPaint.getShader().setLocalMatrix(null);
                        return;
                    }
                    MatrixAccess matAccess = (MatrixAccess) mContext.getObject(id);
                    mTmpMatrix.setValues(MatrixAccess.to3x3(matAccess.get()));
                    Shader s = mPaint.getShader();
                    s.setLocalMatrix(mTmpMatrix);
                }

                /**
                 * @param fontType String to be looked up in system
                 * @param weight the weight of the font
                 * @param italic if the font is italic
                 */
                @Override
                public void setTypeFace(@NonNull String fontType, int weight, boolean italic) {
                    String path = getFontPath(fontType);
                    mFontBuilder = new Font.Builder(new File(path));
                    mFontBuilder.setWeight(weight);
                    mFontBuilder.setSlant(
                            italic ? FontStyle.FONT_SLANT_ITALIC : FontStyle.FONT_SLANT_UPRIGHT);
                    setAxis(null);
                }

                private Font.Builder createFontBuilder(byte[] data, int weight, boolean italic) {
                    ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);

                    // 2. Put the fontBytes into the direct buffer.
                    buffer.put(data);
                    buffer.rewind();
                    mFontBuilder = new Font.Builder(buffer);
                    mFontBuilder.setWeight(weight);
                    mFontBuilder.setSlant(
                            italic ? FontStyle.FONT_SLANT_ITALIC : FontStyle.FONT_SLANT_UPRIGHT);
                    setAxis(null);
                    return mFontBuilder;
                }

                private void setAxis(FontVariationAxis[] axis) {
                    Font font = null;
                    try {
                        if (axis != null) {
                            mFontBuilder.setFontVariationSettings(axis);
                        }
                        font = mFontBuilder.build();
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }

                    FontFamily.Builder fontFamilyBuilder = new FontFamily.Builder(font);
                    FontFamily fontFamily = fontFamilyBuilder.build();
                    Typeface typeface =
                            new Typeface.CustomFallbackBuilder(fontFamily)
                                    .setSystemFallback("sans-serif")
                                    .build();
                    mPaint.setTypeface(typeface);
                }

                private String getFontPath(String fontName) {
                    File fontsDir = new File(SYSTEM_FONTS_PATH);
                    if (!fontsDir.exists() || !fontsDir.isDirectory()) {
                        System.err.println("System fonts directory not found");
                        return null;
                    }

                    File[] fontFiles = fontsDir.listFiles();
                    if (fontFiles == null) {
                        System.err.println("Unable to list font files");
                        return null;
                    }
                    fontName = fontName.toLowerCase(Locale.ROOT);
                    for (File fontFile : fontFiles) {
                        if (fontFile.getName().toLowerCase(Locale.ROOT).contains(fontName)) {
                            return fontFile.getAbsolutePath();
                        }
                    }
                    System.err.println("font \"" + fontName + "\" not found");
                    return null;
                }

                /**
                 * Set the font variation axes
                 *
                 * @param tags tags
                 * @param values values
                 */
                @Override
                public void setFontVariationAxes(@NonNull String[] tags, float @NonNull [] values) {
                    FontVariationAxis[] axes = new FontVariationAxis[tags.length];
                    for (int i = 0; i < tags.length; i++) {
                        axes[i] = new FontVariationAxis(tags[i], values[i]);
                    }
                    setAxis(axes);
                }

                /**
                 * Set the texture shader
                 *
                 * @param bitmapId the id of the bitmap to use
                 * @param tileX The tiling mode for x to draw the bitmap in.
                 * @param tileY The tiling mode for y to draw the bitmap in.
                 * @param filterMode the filter mode to be used when sampling from this shader.
                 * @param maxAnisotropy The Anisotropy value to use for filtering. Must be greater
                 *     than 0.
                 */
                @Override
                public void setTextureShader(
                        int bitmapId,
                        short tileX,
                        short tileY,
                        short filterMode,
                        short maxAnisotropy) {
                    BitmapShader shader =
                            (BitmapShader)
                                    mContext.mRemoteComposeState.getFromId(
                                            bitmapId + BITMAP_TEXTURE_ID_OFFSET);
                    if (shader != null) {
                        mPaint.setShader(shader);
                        return;
                    }
                    Bitmap bitmap = (Bitmap) mContext.mRemoteComposeState.getFromId(bitmapId);
                    if (bitmap == null) {
                        return;
                    }
                    shader =
                            new BitmapShader(
                                    bitmap,
                                    Shader.TileMode.values()[tileX],
                                    Shader.TileMode.values()[tileY]);

                    if (Build.VERSION.SDK_INT // REMOVE IN PLATFORM
                            >= Build.VERSION_CODES.TIRAMISU) { // REMOVE IN PLATFORM
                        if (filterMode > 0) {
                            shader.setFilterMode(filterMode);
                        }

                        if (Build.VERSION.SDK_INT // REMOVE IN PLATFORM
                                >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // REMOVE IN PLATFORM
                            if (maxAnisotropy > 0) {
                                shader.setMaxAnisotropy(maxAnisotropy);
                            }
                        } // REMOVE IN PLATFORM
                    } // REMOVE IN PLATFORM

                    mPaint.setShader(shader);
                }

                @Override
                public void setStrokeWidth(float width) {
                    mPaint.setStrokeWidth(width);
                }

                @Override
                public void setColor(int color) {
                    mPaint.setColor(color);
                }

                @Override
                public void setStrokeCap(int cap) {
                    mPaint.setStrokeCap(Paint.Cap.values()[cap]);
                }

                @Override
                public void setStyle(int style) {
                    mPaint.setStyle(Paint.Style.values()[style]);
                }

                @SuppressLint("NewApi")
                @Override
                public void setShader(int shaderId) {
                    // TODO this stuff should check the shader creation
                    if (shaderId == 0) {
                        mPaint.setShader(null);
                        return;
                    }
                    ShaderData data = getShaderData(shaderId);
                    if (data == null) {
                        return;
                    }
                    RuntimeShader shader = new RuntimeShader(getText(data.getShaderTextId()));
                    String[] names = data.getUniformFloatNames();
                    for (int i = 0; i < names.length; i++) {
                        String name = names[i];
                        float[] val = data.getUniformFloats(name);
                        shader.setFloatUniform(name, val);
                    }
                    names = data.getUniformIntegerNames();
                    for (int i = 0; i < names.length; i++) {
                        String name = names[i];
                        int[] val = data.getUniformInts(name);
                        shader.setIntUniform(name, val);
                    }
                    names = data.getUniformBitmapNames();
                    for (int i = 0; i < names.length; i++) {
                        String name = names[i];
                        int val = data.getUniformBitmapId(name);
                        AndroidRemoteContext androidContext = (AndroidRemoteContext) mContext;
                        Bitmap bitmap = (Bitmap) androidContext.mRemoteComposeState.getFromId(val);
                        BitmapShader bitmapShader =
                                new BitmapShader(
                                        bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                        shader.setInputShader(name, bitmapShader);
                    }
                    mPaint.setShader(shader);
                }

                @Override
                public void setImageFilterQuality(int quality) {
                    mPaint.setFilterBitmap(quality == 1);
                }

                @Override
                public void setBlendMode(int mode) {
                    mPaint.setBlendMode(remoteToAndroidBlendMode(mode));
                }

                @Override
                public void setAlpha(float a) {
                    mPaint.setAlpha((int) (255 * a));
                }

                @Override
                public void setStrokeMiter(float miter) {
                    mPaint.setStrokeMiter(miter);
                }

                @Override
                public void setStrokeJoin(int join) {
                    mPaint.setStrokeJoin(Paint.Join.values()[join]);
                }

                @Override
                public void setFilterBitmap(boolean filter) {
                    mPaint.setFilterBitmap(filter);
                }

                @Override
                public void setAntiAlias(boolean aa) {
                    mPaint.setAntiAlias(aa);
                }

                @Override
                public void clear(long mask) {
                    if ((mask & (1L << PaintBundle.COLOR_FILTER)) != 0) {
                        mPaint.setColorFilter(null);
                    }
                }

                Shader.TileMode[] mTileModes =
                        new Shader.TileMode[]{
                                Shader.TileMode.CLAMP,
                                Shader.TileMode.REPEAT,
                                Shader.TileMode.MIRROR
                        };

                @Override
                public void setLinearGradient(
                        int @NonNull [] colors,
                        float @NonNull [] stops,
                        float startX,
                        float startY,
                        float endX,
                        float endY,
                        int tileMode) {
                    mPaint.setShader(
                            new LinearGradient(
                                    startX,
                                    startY,
                                    endX,
                                    endY,
                                    colors,
                                    stops,
                                    mTileModes[tileMode]));
                }

                @Override
                public void setRadialGradient(
                        int @NonNull [] colors,
                        float @NonNull [] stops,
                        float centerX,
                        float centerY,
                        float radius,
                        int tileMode) {
                    mPaint.setShader(
                            new RadialGradient(
                                    centerX, centerY, radius, colors, stops, mTileModes[tileMode]));
                }

                @Override
                public void setSweepGradient(
                        int @NonNull [] colors,
                        float @NonNull [] stops,
                        float centerX,
                        float centerY) {
                    mPaint.setShader(new SweepGradient(centerX, centerY, colors, stops));
                }

                @Override
                public void setColorFilter(int color, int mode) {
                    PorterDuff.Mode pmode = remoteToAndroidPorterDuffMode(mode);
                    if (pmode != null) {
                        mPaint.setColorFilter(new PorterDuffColorFilter(color, pmode));
                    }
                }
            };

    /**
     * This applies paint changes to the current paint
     *
     * @param paintData the list change to the paint
     */
    @Override
    public void applyPaint(@NonNull PaintBundle paintData) {
        paintData.applyPaintChange(this, mCachedPaintChanges);
    }

    @Override
    public void matrixScale(float scaleX, float scaleY, float centerX, float centerY) {
        if (Float.isNaN(centerX)) {
            mCanvas.scale(scaleX, scaleY);
        } else {
            mCanvas.scale(scaleX, scaleY, centerX, centerY);
        }
    }

    @Override
    public void matrixTranslate(float translateX, float translateY) {
        mCanvas.translate(translateX, translateY);
    }

    @Override
    public void matrixSkew(float skewX, float skewY) {
        mCanvas.skew(skewX, skewY);
    }

    @Override
    public void matrixRotate(float rotate, float pivotX, float pivotY) {
        if (Float.isNaN(pivotX)) {
            mCanvas.rotate(rotate);
        } else {
            mCanvas.rotate(rotate, pivotX, pivotY);
        }
    }

    @Override
    public void matrixSave() {
        mCanvas.save();
    }

    @Override
    public void matrixRestore() {
        mCanvas.restore();
    }

    @Override
    public void clipRect(float left, float top, float right, float bottom) {
        mCanvas.clipRect(left, top, right, bottom);
    }

    @Override
    public void roundedClipRect(
            float width,
            float height,
            float topStart,
            float topEnd,
            float bottomStart,
            float bottomEnd) {
        Path roundedPath = new Path();
        float[] radii =
                new float[]{
                        topStart,
                        topStart,
                        topEnd,
                        topEnd,
                        bottomEnd,
                        bottomEnd,
                        bottomStart,
                        bottomStart
                };

        roundedPath.addRoundRect(0f, 0f, width, height, radii, android.graphics.Path.Direction.CW);
        mCanvas.clipPath(roundedPath);
    }

    @Override
    public void clipPath(int pathId, int regionOp) {
        Path path = getPath(pathId, 0, 1);
        if (regionOp == ClipPath.DIFFERENCE) {
            mCanvas.clipOutPath(path); // DIFFERENCE
        } else {
            mCanvas.clipPath(path); // INTERSECT
        }
    }

    @Override
    public void tweenPath(int out, int path1, int path2, float tween) {
        float[] p = getPathArray(path1, path2, tween);
        AndroidRemoteContext androidContext = (AndroidRemoteContext) mContext;
        androidContext.mRemoteComposeState.putPathData(out, p);
    }

    @Override
    public void combinePath(int out, int path1, int path2, byte operation) {
        Path p1 = getPath(path1, 0, 1);
        Path p2 = getPath(path2, 0, 1);
        Path.Op[] op = {
                Path.Op.DIFFERENCE,
                Path.Op.INTERSECT,
                Path.Op.REVERSE_DIFFERENCE,
                Path.Op.UNION,
                Path.Op.XOR,
        };
        Path p = new Path(p1);
        p.op(p2, op[operation]);

        AndroidRemoteContext androidContext = (AndroidRemoteContext) mContext;
        androidContext.mRemoteComposeState.putPath(out, p);
    }

    @Override
    public void reset() {
        // With out calling setTypeface before or after paint is reset()
        // Variable type fonts corrupt memory resulting in a
        // segmentation violation
        mPaint.setTypeface(Typeface.DEFAULT);
        mPaint.reset();
    }

    private Path getPath(int path1Id, int path2Id, float tween, float start, float end) {
        return getPath(getPathArray(path1Id, path2Id, tween), start, end);
    }

    private float[] getPathArray(int path1Id, int path2Id, float tween) {
        AndroidRemoteContext androidContext = (AndroidRemoteContext) mContext;
        if (tween == 0.0f) {
            return androidContext.mRemoteComposeState.getPathData(path1Id);
        }
        if (tween == 1.0f) {
            return androidContext.mRemoteComposeState.getPathData(path2Id);
        }

        float[] data1 = androidContext.mRemoteComposeState.getPathData(path1Id);
        float[] data2 = androidContext.mRemoteComposeState.getPathData(path2Id);
        float[] tmp = new float[data2.length];
        for (int i = 0; i < tmp.length; i++) {
            if (Float.isNaN(data1[i]) || Float.isNaN(data2[i])) {
                tmp[i] = data1[i];
            } else {
                tmp[i] = (data2[i] - data1[i]) * tween + data1[i];
            }
        }
        return tmp;
    }

    private Path getPath(float[] tmp, float start, float end) {
        Path path = new Path();
        FloatsToPath.genPath(path, tmp, start, end);
        return path;
    }

    private Path getPath(int id, float start, float end) {
        AndroidRemoteContext androidContext = (AndroidRemoteContext) mContext;
        Path p = (Path) androidContext.mRemoteComposeState.getPath(id);
        int w = androidContext.mRemoteComposeState.getPathWinding(id);
        if (p != null) {
            return p;
        }
        Path path = new Path();
        float[] pathData = androidContext.mRemoteComposeState.getPathData(id);
        if (pathData != null) {
            FloatsToPath.genPath(path, pathData, start, end);
            switch (w) {
                case 1:
                    path.setFillType(Path.FillType.EVEN_ODD);
                    break;
                case 2:
                    path.setFillType(Path.FillType.INVERSE_EVEN_ODD);
                    break;
                case 3:
                    path.setFillType(Path.FillType.INVERSE_WINDING);
                    break;
            }
            androidContext.mRemoteComposeState.putPath(id, path);
        }

        return path;
    }

    @Override
    public @Nullable String getText(int id) {
        return (String) mContext.mRemoteComposeState.getFromId(id);
    }

    private ShaderData getShaderData(int id) {
        return (ShaderData) mContext.mRemoteComposeState.getFromId(id);
    }

    @Override
    public void matrixFromPath(int pathId, float fraction, float vOffset, int flags) {
        Path path = getPath(pathId, 0, 1);
        PathMeasure measure = new PathMeasure(path, false);
        float len = measure.getLength();
        Matrix matrix = new Matrix();
        measure.getMatrix((len * fraction) % len, matrix, flags);
        mCanvas.concat(matrix);
    }

    HashMap<Bitmap, Canvas> mCCache = new HashMap<>();

    @Override
    public void drawToBitmap(int bitmapId, int mode, int color) {
        if (mMainCanvas == null) {
            mMainCanvas = mCanvas;
        }
        if (bitmapId == 0) {
            mCanvas = mMainCanvas;
            return;
        }
        Bitmap bitmap = (Bitmap) mContext.mRemoteComposeState.getFromId(bitmapId);
        Objects.requireNonNull(bitmap);
        if (mCCache.containsKey(bitmap)) {
            mCanvas = Objects.requireNonNull(mCCache.get(bitmap));

            if ((mode & 1) == 0) {
                bitmap.eraseColor(color);
            }
            return;
        }
        mCanvas = new Canvas(bitmap);
        if ((mode & 1) == 0) {
            bitmap.eraseColor(color);
        }
        mCCache.put(bitmap, mCanvas);
    }
}
