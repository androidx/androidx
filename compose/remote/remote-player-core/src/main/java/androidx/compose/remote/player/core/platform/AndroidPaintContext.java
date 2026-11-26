/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.compose.remote.player.core.platform;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.compose.remote.core.RemoteComposeState.BITMAP_TEXTURE_ID_OFFSET;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.graphics.ComposePathEffect;
import android.graphics.DashPathEffect;
import android.graphics.DiscretePathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Mesh;
import android.graphics.MeshSpecification;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PathEffect;
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
import android.graphics.SumPathEffect;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import androidx.annotation.DoNotInline;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CustomContext;
import androidx.compose.remote.core.MatrixAccess;
import androidx.compose.remote.core.Paint3DContext;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.ClipPath;
import androidx.compose.remote.core.operations.DrawMesh2D;
import androidx.compose.remote.core.operations.DrawTextOnCircle;
import androidx.compose.remote.core.operations.ShaderData;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.LayoutComponent;
import androidx.compose.remote.core.operations.layout.managers.CoreText;
import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation;
import androidx.compose.remote.core.operations.paint.PaintBundle;
import androidx.compose.remote.core.operations.paint.PaintChanges;
import androidx.compose.remote.core.operations.paint.PaintPathEffects;
import androidx.compose.remote.core.operations.utilities.Mesh2DGenerator;
import androidx.compose.remote.player.core.platform.d3.InfinitePlane;
import androidx.compose.remote.player.core.platform.d3.JavaPaint3DContext;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * An implementation of PaintContext for the Android Canvas. This is used to play the RemoteCompose
 * operations on Android.
 */
@RestrictTo(LIBRARY_GROUP)
public class AndroidPaintContext extends PaintContext implements CustomContext, Paint3DContext {

    private static final Shader.TileMode[] TILE_MODES = Shader.TileMode.values();

    private static Shader.@NonNull TileMode tileModeFromInt(int value) {
        if (value < 0 || value >= TILE_MODES.length) {
            return Shader.TileMode.CLAMP;
        }
        return TILE_MODES[value];
    }

    Paint mPaint = new Paint();
    List<Paint> mPaintList = new ArrayList<>();
    Canvas mCanvas;
    Canvas mMainCanvas = null;
    Rect mTmpRect = new Rect(); // use in calculation of bounds
    RenderNode mNode = null;
    Canvas mPreviousCanvas = null;
    private TypefaceResolver mTypefaceResolver;
    private AndroidCustomContext mCustomSupport = null;

    /**
     * Set the custom support for this platform.
     *
     * @param customSupport the custom support to use
     */

    @Override
    public void setCustomSupport(@NonNull CustomContext customSupport) {
        this.mCustomSupport = (AndroidCustomContext) customSupport;
        mCustomSupport.setCanvas(mCanvas);
        mCustomSupport.setRemoteContext(mContext);
        android.content.Context androidContext =
                (mContext instanceof AndroidRemoteContext)
                        ? ((AndroidRemoteContext) mContext).getAndroidContext()
                        : null;
        if (androidContext != null) {
            mCustomSupport.setContext(androidContext);
        }
    }

    @Override
    public void createCustom(int id, @NonNull String config) {
        if (mCustomSupport == null) {
            throw new RuntimeException("No Custom components supported");
        }
        android.content.Context androidContext =
                (mContext instanceof AndroidRemoteContext)
                        ? ((AndroidRemoteContext) mContext).getAndroidContext()
                        : null;
        if (androidContext != null) {
            mCustomSupport.setContext(androidContext);
            mCustomSupport.createCustom(id, config);
        }
    }

    @Override
    public void configureCustom(int id, int type, @NonNull String value) {
        if (mCustomSupport == null) {
            throw new RuntimeException("No Custom components supported");
        }
        mCustomSupport.configureCustom(id, type, value);
    }

    @Override
    public void configureCustom(int id, int type, int value) {
        if (mCustomSupport == null) {
            throw new RuntimeException("No Custom components supported");
        }
        mCustomSupport.configureCustom(id, type, value);
    }

    @Override
    public void configureCustom(int id, int type, float value) {
        if (mCustomSupport == null) {
            throw new RuntimeException("No Custom components supported");
        }
        mCustomSupport.configureCustom(id, type, value);
    }

    @Override
    public void measureCustom(int id, float @NonNull [] bounds) {
        if (mCustomSupport == null) {
            throw new RuntimeException("No Custom components supported");
        }
        mCustomSupport.measureCustom(id, bounds);
    }

    @Override
    public void layoutCustom(int id, float @NonNull [] bounds) {
        if (mCustomSupport == null) {
            throw new RuntimeException("No Custom components supported");
        }
        mCustomSupport.layoutCustom(id, bounds);
    }

    @Override
    public boolean touchCustom(int id, int type, float x, float y) {
        if (mCustomSupport == null) {
            throw new RuntimeException("No Custom components supported");
        }
        return mCustomSupport.touchCustom(id, type, x, y);
    }

    @Override
    public void drawCustom(int id) {
        flushPending3D();
        if (mCustomSupport == null) {
            throw new RuntimeException("No Custom components supported");
        }
        mCustomSupport.setCanvas(mCanvas);
        mCustomSupport.drawCustom(id);
    }

    public AndroidPaintContext(@NonNull RemoteContext context, @NonNull Canvas canvas) {
        super(context);
        this.mCanvas = canvas;
        this.mTypefaceResolver = new DefaultTypefaceResolver(context);
    }

    public void setTypefaceResolver(@NonNull TypefaceResolver typefaceResolver) {
        this.mTypefaceResolver = typefaceResolver;
    }

    public @NonNull Canvas getCanvas() {
        flushPending3D();
        return mCanvas;
    }

    public void setCanvas(@NonNull Canvas canvas) {
        this.mCanvas = mMainCanvas = canvas;
        if (mCustomSupport != null) {
            mCustomSupport.setCanvas(canvas);
        }
        m3DBlitCount = 0; // a new frame: every blit bitmap is free again
    }

    @Override
    public void save() {
        // No flushPending3D(): saving changes neither the matrix, the clip nor the target, and
        // every call that does change one composites pending 3D first.
        mCanvas.save();
    }

    private final Paint mLayerPaint = new Paint();

    @Override
    public void saveLayer(float x, float y, float width, float height) {
        flushPending3D();
        mLayerPaint.reset();
        mLayerPaint.setAlpha(mPaint.getAlpha());
        mCanvas.saveLayer(x, y, x + width, y + height, mLayerPaint);
        mPaint.setAlpha(255);
    }

    @Override
    public void restore() {
        flushPending3D();
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
        flushPending3D();
        if (mContext.mRemoteComposeState.containsId(imageId)) {
            Bitmap bitmap = (Bitmap) mContext.mRemoteComposeState.getFromId(imageId);
            mCanvas.drawBitmap(
                    bitmap,
                    new Rect(srcLeft, srcTop, srcRight, srcBottom),
                    new Rect(dstLeft, dstTop, dstRight, dstBottom),
                    mPaint);
        }
    }

    @Override
    public void scale(float scaleX, float scaleY) {
        flushPending3D();
        mCanvas.scale(scaleX, scaleY);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void startGraphicsLayer(int w, int h) {
        flushPending3D();
        mNode = new RenderNode("layer");
        mNode.setPosition(0, 0, w, h);
        mPreviousCanvas = mCanvas;
        mCanvas = mNode.beginRecording();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
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
                    mNode.setPivotY((Float) value * mNode.getHeight());
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

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void endGraphicsLayer() {
        flushPending3D();
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
        flushPending3D();
        mCanvas.translate(translateX, translateY);
    }

    @Override
    public void drawArc(
            float left, float top, float right, float bottom, float startAngle, float sweepAngle) {
        flushPending3D();
        mCanvas.drawArc(left, top, right, bottom, startAngle, sweepAngle, false, mPaint);
    }

    @Override
    public void drawSector(
            float left, float top, float right, float bottom, float startAngle, float sweepAngle) {
        flushPending3D();
        mCanvas.drawArc(left, top, right, bottom, startAngle, sweepAngle, true, mPaint);
    }

    @Override
    public void drawBitmap(int id, float left, float top, float right, float bottom) {
        flushPending3D();
        if (mContext.mRemoteComposeState.containsId(id)) {
            Bitmap bitmap = (Bitmap) mContext.mRemoteComposeState.getFromId(id);
            Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            RectF dst = new RectF(left, top, right, bottom);
            mCanvas.drawBitmap(bitmap, src, dst, mPaint);
        }
    }

    @Override
    public void drawCircle(float centerX, float centerY, float radius) {
        flushPending3D();
        mCanvas.drawCircle(centerX, centerY, radius, mPaint);
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2) {
        flushPending3D();
        mCanvas.drawLine(x1, y1, x2, y2, mPaint);
    }

    @Override
    public void drawOval(float left, float top, float right, float bottom) {
        flushPending3D();
        mCanvas.drawOval(left, top, right, bottom, mPaint);
    }

    @Override
    public void drawPath(int id, float start, float end) {
        flushPending3D();
        mCanvas.drawPath(getPath(id, start, end), mPaint);
    }

    @Override
    public void drawRect(float left, float top, float right, float bottom) {
        flushPending3D();
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
        flushPending3D();
        mCanvas.drawRoundRect(left, top, right, bottom, radiusX, radiusY, mPaint);
    }

    @Override
    public void drawTextOnPath(int textId, int pathId, float hOffset, float vOffset) {
        flushPending3D();
        mCanvas.drawTextOnPath(getText(textId), getPath(pathId, 0, 1), hOffset, vOffset, mPaint);
    }

    /**
     * Draws text along a circular arc using Android {@link Path} and {@link Canvas#drawTextOnPath}.
     *
     * <p>Measures text width with the current {@link Paint}, calculates the arc sweep angle based
     * on the final radius ({@code radius + warpRadiusOffset}), adjusts the starting angle for text
     * alignment (START, CENTER, or END), and constructs an arc path using {@link Path#addArc}.
     *
     * <p>Placement specifies the drawing direction: {@code OUTSIDE} places text clockwise along the
     * top of the arc, while {@code INSIDE} places text counter-clockwise along the bottom.
     */
    @Override
    public void drawTextOnCircle(
            int textId,
            float centerX,
            float centerY,
            float radius,
            float startAngle,
            float warpRadiusOffset,
            int alignment,
            int placement) {
        flushPending3D();
        String textToDraw = getText(textId);
        if (textToDraw == null) {
            return;
        }
        float textWidth = mPaint.measureText(textToDraw);
        float finalRadius = radius + warpRadiusOffset;
        if (finalRadius <= 0f) {
            return;
        }
        // OUTSIDE placement draws text clockwise on top of the arc;
        // INSIDE placement draws counterclockwise.
        boolean clockwise = placement == DrawTextOnCircle.Placement.OUTSIDE.ordinal();
        float sweepDegrees = (float) Math.toDegrees(textWidth / finalRadius);
        float finalStartAngle = startAngle;
        if (!clockwise) {
            sweepDegrees = -sweepDegrees;
            if (alignment == DrawTextOnCircle.Alignment.CENTER.ordinal()) {
                finalStartAngle = startAngle + Math.abs(sweepDegrees) / 2f;
            } else if (alignment == DrawTextOnCircle.Alignment.END.ordinal()) {
                finalStartAngle = startAngle + Math.abs(sweepDegrees);
            }
        } else {
            if (alignment == DrawTextOnCircle.Alignment.CENTER.ordinal()) {
                finalStartAngle = startAngle - sweepDegrees / 2f;
            } else if (alignment == DrawTextOnCircle.Alignment.END.ordinal()) {
                finalStartAngle = startAngle - sweepDegrees;
            }
        }

        Path path = new Path();
        path.addArc(
                centerX - finalRadius,
                centerY - finalRadius,
                centerX + finalRadius,
                centerY + finalRadius,
                finalStartAngle,
                sweepDegrees);

        mPaint.setTextAlign(Paint.Align.LEFT);
        mCanvas.drawTextOnPath(textToDraw, path, 0f, 0f, mPaint);
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
    public RcPlatformServices.@Nullable ComputedTextLayout layoutComplexText(
            int textId,
            int start,
            int end,
            int alignment,
            int overflow,
            int maxLines,
            float maxWidth,
            float maxHeight,
            float letterSpacing,
            float lineHeightAdd,
            float lineHeightMultiplier,
            int lineBreakStrategy,
            int hyphenationFrequency,
            int justificationMode,
            boolean underline,
            boolean strikethrough,
            int flags) {
        String str = getText(textId);
        if (str == null) {
            return null;
        }
        if (end == -1 || end > str.length()) {
            end = str.length();
        }

        TextPaint textPaint = new TextPaint();

        boolean useAdvancedFeatures =
                (flags & (PaintContext.TEXT_MEASURE_AUTOSIZE | PaintContext.TEXT_COMPLEX)) != 0;

        if (letterSpacing != 0f) {
            mPaint.setLetterSpacing(letterSpacing);
            useAdvancedFeatures = true;
        }
        if (underline) {
            mPaint.setUnderlineText(underline);
            useAdvancedFeatures = true;
        }
        if (strikethrough) {
            mPaint.setStrikeThruText(strikethrough);
            useAdvancedFeatures = true;
        }

        textPaint.set(mPaint);
        StaticLayout.Builder staticLayoutBuilder =
                StaticLayout.Builder.obtain(str, start, end, textPaint, (int) maxWidth);
        switch (alignment) {
            case CoreText.TEXT_ALIGN_RIGHT:
            case CoreText.TEXT_ALIGN_END:
                staticLayoutBuilder.setAlignment(Layout.Alignment.ALIGN_OPPOSITE);
                break;
            case CoreText.TEXT_ALIGN_CENTER:
                staticLayoutBuilder.setAlignment(Layout.Alignment.ALIGN_CENTER);
                break;
            default:
                staticLayoutBuilder.setAlignment(Layout.Alignment.ALIGN_NORMAL);
        }
        boolean useEllipses = false;
        switch (overflow) {
            case CoreText.OVERFLOW_ELLIPSIS:
                staticLayoutBuilder.setEllipsize(TextUtils.TruncateAt.END);
                useEllipses = true;
                break;
            case CoreText.OVERFLOW_MIDDLE_ELLIPSIS:
                staticLayoutBuilder.setEllipsize(TextUtils.TruncateAt.MIDDLE);
                useEllipses = true;
                break;
            case CoreText.OVERFLOW_START_ELLIPSIS:
                staticLayoutBuilder.setEllipsize(TextUtils.TruncateAt.START);
                useEllipses = true;
                break;
            default:
        }

        if (useEllipses) {
            useAdvancedFeatures = true;
        }

        staticLayoutBuilder.setMaxLines(maxLines);
        staticLayoutBuilder.setIncludePad(false);

        if (lineBreakStrategy > 0) {
            staticLayoutBuilder.setBreakStrategy(lineBreakStrategy);
            useAdvancedFeatures = true;
        }
        if (hyphenationFrequency > 0) {
            staticLayoutBuilder.setHyphenationFrequency(hyphenationFrequency);
            useAdvancedFeatures = true;
        }
        if (justificationMode > 0) {
            staticLayoutBuilder.setJustificationMode(justificationMode);
            useAdvancedFeatures = true;
        }
        if (lineHeightAdd != 0f || lineHeightMultiplier != 1f) {
            staticLayoutBuilder.setLineSpacing(lineHeightAdd, lineHeightMultiplier);
            useAdvancedFeatures = true;
        }

        StaticLayout staticLayout = staticLayoutBuilder.build();
        if (useAdvancedFeatures) {
            Rect bounds = new Rect(0, 0, 0, 0);
            boolean isHyphenatedText = getTightBoundingBox(staticLayout, bounds);
            int visibleLines = staticLayout.getLineCount();
            if (bounds.height() > maxHeight
                    && useEllipses
                    && staticLayout.getLineCount() >= maxLines) {
                // let's measure how many lines we can actually fit
                int lineCount = staticLayout.getLineCount();
                visibleLines = 0;
                for (int i = 0; i < lineCount; i++) {
                    float lineHeight = 0;
                    if (Build.VERSION.SDK_INT // REMOVE IN PLATFORM
                            >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // REMOVE IN PLATFORM
                        lineHeight = staticLayout.getLineBottom(i, true);
                    } else { // REMOVE IN PLATFORM
                        lineHeight = staticLayout.getLineBottom(i); // REMOVE IN PLATFORM
                    } // REMOVE IN PLATFORM
                    if (lineHeight < maxHeight) {
                        visibleLines++;
                    } else {
                        break;
                    }
                }
            }
            return new AndroidComputedTextLayout(
                    staticLayout,
                    bounds.left,
                    bounds.width(),
                    bounds.height(),
                    visibleLines,
                    isHyphenatedText);
        } else {
            return new AndroidComputedTextLayout(
                    staticLayout,
                    staticLayout.getWidth(),
                    staticLayout.getHeight(),
                    staticLayout.getLineCount(),
                    false);
        }
    }

    /** Returns true if a line is hyphenated. */
    public boolean isLineHyphenated(
            @NonNull StaticLayout layout, @NonNull CharSequence originalText, int lineIndex) {
        if (lineIndex >= layout.getLineCount() - 1) {
            return false;
        }
        int lastCharIndexOnLine = layout.getLineEnd(lineIndex) - 1;
        if (lastCharIndexOnLine < 0 || lastCharIndexOnLine + 1 >= originalText.length()) {
            // Empty line, or a line ending at the end of the text (e.g. the line before the
            // empty trailing line a final newline produces): nothing follows the break.
            return false;
        }
        char charBeforeBreak = originalText.charAt(lastCharIndexOnLine);
        char charAfterBreak = originalText.charAt(lastCharIndexOnLine + 1);
        if (!Character.isWhitespace(charBeforeBreak) && !Character.isWhitespace(charAfterBreak)) {
            return true;
        }
        return false;
    }

    /** Returns the bounding box of the static layout */
    public boolean getTightBoundingBox(@NonNull StaticLayout layout, @NonNull Rect bounds) {
        int lineCount = layout.getLineCount();
        if (lineCount == 0) {
            return false;
        }

        boolean isHyphenated = false;

        int top = layout.getLineTop(0);
        int bottom = layout.getLineBottom(lineCount - 1);

        float minLeft = Float.MAX_VALUE;
        float maxRight = 0f;
        for (int i = 0; i < lineCount; i++) {
            float lineLeft = layout.getLineLeft(i);
            float lineRight = layout.getLineRight(i);
            if (lineLeft < minLeft) {
                minLeft = lineLeft;
            }
            if (lineRight > maxRight) {
                maxRight = lineRight;
            }
            isHyphenated |= isLineHyphenated(layout, layout.getText(), i);
        }
        if (minLeft == Float.MAX_VALUE) {
            minLeft = 0f;
        }
        bounds.left = (int) minLeft;
        bounds.top = top;
        bounds.right = (int) Math.ceil(maxRight);
        bounds.bottom = bottom;
        return isHyphenated;
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
        flushPending3D();

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
    public void drawComplexText(
            RcPlatformServices.@Nullable ComputedTextLayout computedTextLayout) {
        flushPending3D();
        if (computedTextLayout == null) {
            return;
        }
        AndroidComputedTextLayout androidLayout = (AndroidComputedTextLayout) computedTextLayout;
        StaticLayout staticLayout = androidLayout.get();
        float left = androidLayout.getLeft();
        if (left != 0f) {
            mCanvas.translate(-left, 0f);
            staticLayout.draw(mCanvas);
            mCanvas.translate(left, 0f);
        } else {
            staticLayout.draw(mCanvas);
        }
    }

    @Override
    public void drawTweenPath(int path1Id, int path2Id, float tween, float start, float end) {
        flushPending3D();
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

    @SuppressLint("ObsoleteSdkInt")
    @RequiresApi(29)
    private static class Api29Impl {
        @DoNotInline
        static void setBlendMode(Paint paint, int mode) {
            paint.setBlendMode(remoteToAndroidBlendMode(mode));
        }

        @DoNotInline
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
    }

    PaintChanges mCachedPaintChanges =
            new PaintChanges() {
                private FontInstance mFontInstance;
                final Matrix mTmpMatrix = new Matrix();
                Typeface mFallbackTypeFace = Typeface.DEFAULT;
                int mFallbackWeight = 400;
                boolean mFallbackItalic = false;
                private String[] mPendingTags;
                private float[] mPendingValues;

                @Override
                public void setTextSize(float size) {
                    mPaint.setTextSize(size);
                }

                @Override
                public void setFallbackTypeFace(int fontType, int weight, boolean italic) {
                    Typeface typeface = Typeface.DEFAULT;
                    switch (fontType) {
                        case PaintBundle.FONT_TYPE_DEFAULT:
                            break;
                        case PaintBundle.FONT_TYPE_SERIF:
                            typeface = Typeface.SERIF;
                            break;
                        case PaintBundle.FONT_TYPE_SANS_SERIF:
                            typeface = Typeface.SANS_SERIF;
                            break;
                        case PaintBundle.FONT_TYPE_MONOSPACE:
                            typeface = Typeface.MONOSPACE;
                    }
                    mFallbackItalic = italic;
                    mFallbackWeight = weight;
                    mFallbackTypeFace = typeface;
                }

                @Override
                @SuppressLint("NewApi")
                public void setTypeFace(int fontType, int weight, boolean italic) {
                    mFontInstance =
                            mTypefaceResolver.resolve(
                                    fontType,
                                    weight,
                                    italic,
                                    mFallbackTypeFace,
                                    mFallbackWeight,
                                    mFallbackItalic);
                    if (mPendingTags != null) {
                        mPaint.setTypeface(
                                mFontInstance.applyVariationSettings(mPendingTags, mPendingValues));
                    } else {
                        mPaint.setTypeface(mFontInstance.getTypeface());
                    }
                    mFontInstance.setOnLoadedListener(() -> mContext.needsRepaint());
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

                /*
                 * @param fontType String to be looked up in system
                 * @param weight the weight of the font
                 * @param italic if the font is italic
                 */
                @Override
                public void setTypeFace(@NonNull String fontType, int weight, boolean italic) {
                    mFontInstance =
                            mTypefaceResolver.resolve(
                                    fontType,
                                    weight,
                                    italic,
                                    mFallbackTypeFace,
                                    mFallbackWeight,
                                    mFallbackItalic);
                    if (mPendingTags != null) {
                        mPaint.setTypeface(
                                mFontInstance.applyVariationSettings(mPendingTags, mPendingValues));
                    } else {
                        mPaint.setTypeface(mFontInstance.getTypeface());
                    }
                    mFontInstance.setOnLoadedListener(() -> mContext.needsRepaint());
                }

                /*
                 * Set the font variation axes
                 *
                 * @param tags tags
                 * @param values values
                 */
                @RequiresApi(api = Build.VERSION_CODES.Q)
                @Override
                public void setFontVariationAxes(@NonNull String[] tags, float @NonNull [] values) {
                    mPendingTags = tags;
                    mPendingValues = values;
                    if (mFontInstance != null) {
                        mPaint.setTypeface(mFontInstance.applyVariationSettings(tags, values));
                    }
                }

                /*
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
                                    bitmap, tileModeFromInt(tileX), tileModeFromInt(tileY));

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
                public void setPathEffect(float @Nullable [] pathEffect) {
                    if (pathEffect == null) {
                        mPaint.setPathEffect(null);
                        return;
                    }
                    PaintPathEffects pe = PaintPathEffects.parse(pathEffect, 0);
                    mPaint.setPathEffect(getPathEffect(pe));
                }

                private PathEffect getPathEffect(PaintPathEffects pe) {
                    if (pe == null) {
                        return null;
                    }
                    int type = pe.getType();
                    PathEffect ret;
                    switch (type) {
                        case PaintPathEffects.DASH:
                            PaintPathEffects.Dash dash = (PaintPathEffects.Dash) pe;
                            ret = new DashPathEffect(dash.mIntervals, dash.mPhase);
                            break;
                        case PaintPathEffects.DISCRETE_PATH:
                            PaintPathEffects.Discrete discrete = (PaintPathEffects.Discrete) pe;
                            ret =
                                    new DiscretePathEffect(
                                            discrete.mSegmentLength, discrete.mDeviation);
                            break;
                        case PaintPathEffects.PATH_DASH:
                            PaintPathEffects.PathDash pathDash = (PaintPathEffects.PathDash) pe;
                            ret =
                                    new PathDashPathEffect(
                                            getPath(pathDash.mShapeId, 0f, 1f),
                                            pathDash.mAdvance,
                                            pathDash.mPhase,
                                            PathDashPathEffect.Style.values()[pathDash.mStyle]);
                            break;
                        case PaintPathEffects.SUM:
                            PaintPathEffects.Sum sum = (PaintPathEffects.Sum) pe;
                            ret =
                                    new SumPathEffect(
                                            getPathEffect(sum.mFirst), getPathEffect(sum.mSecond));
                            break;
                        case PaintPathEffects.COMPOSE:
                            PaintPathEffects.Compose compose = (PaintPathEffects.Compose) pe;
                            ret =
                                    new ComposePathEffect(
                                            getPathEffect(compose.mOuterPE),
                                            getPathEffect(compose.mInnerPE));
                            break;
                        default:
                            ret = null;
                    }
                    return ret;
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
                        if (val.length == 1) {
                            if (Float.isNaN(val[0])) {
                                int varId = Utils.idFromNan(val[0]);
                                float[] values =
                                        mContext.getCollectionsAccess() != null
                                                ? mContext.getCollectionsAccess()
                                                .getDynamicFloats(varId)
                                                : null;
                                if (values != null) {
                                    shader.setFloatUniform(name, values);
                                } else {
                                    shader.setFloatUniform(name, mContext.getFloat(varId));
                                }
                            } else {
                                shader.setFloatUniform(name, val[0]);
                            }
                        } else {
                            float[] evaluated = new float[val.length];
                            for (int k = 0; k < val.length; k++) {
                                evaluated[k] =
                                        Float.isNaN(val[k])
                                                ? mContext.getFloat(Utils.idFromNan(val[k]))
                                                : val[k];
                            }
                            shader.setFloatUniform(name, evaluated);
                        }
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
                        Bitmap bitmap = (Bitmap) mContext.mRemoteComposeState.getFromId(val);
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

                @SuppressLint("ObsoleteSdkInt")
                @Override
                public void setBlendMode(int mode) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // REMOVE IN PLATFORM
                        Api29Impl.setBlendMode(mPaint, mode);
                    } else { // REMOVE IN PLATFORM
                        mPaint.setXfermode(// REMOVE IN PLATFORM
                                new android.graphics.PorterDuffXfermode(// REMOVE IN PLATFORM
                                        remoteToAndroidPorterDuffMode(mode))); // REMOVE IN PLATFORM
                    } // REMOVE IN PLATFORM
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
                                Shader.TileMode.CLAMP, Shader.TileMode.REPEAT,
                                Shader.TileMode.MIRROR
                        };

                @Override
                public void setLinearGradient(
                        int @NonNull [] colors,
                        float @Nullable [] stops,
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
                        float @Nullable [] stops,
                        float centerX,
                        float centerY,
                        float radius,
                        int tileMode) {
                    mPaint.setShader(
                            new RadialGradient(
                                    centerX, centerY, radius, colors, stops, mTileModes[tileMode]));
                }

                @Override
                public void setRadialGradient(
                        int @NonNull [] colors,
                        float @Nullable [] stops,
                        float startX,
                        float startY,
                        float startRadius,
                        float endX,
                        float endY,
                        float endRadius,
                        int tileMode) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // REMOVE IN PLATFORM
                        // unavoidable short term allocation for now but little used API.
                        // Long term could cache the data
                        long[] colorLong = new long[colors.length];
                        for (int i = 0; i < colorLong.length; i++) {
                            colorLong[i] = Color.pack(colors[i]);
                        }
                        mPaint.setShader(
                                new RadialGradient(
                                        startX, startY, startRadius,
                                        endX, endY, endRadius,
                                        colorLong, stops, mTileModes[tileMode]));
                    } else { // REMOVE IN PLATFORM
                        // The two-circle (focal) form needs API 31. Degrade to the end circle
                        // so the document still renders something sensible.
                        mPaint.setShader(// REMOVE IN PLATFORM
                                new RadialGradient(// REMOVE IN PLATFORM
                                        endX, endY, endRadius, // REMOVE IN PLATFORM
                                        colors, stops, mTileModes[tileMode])); // REMOVE IN PLATFORM
                    } // REMOVE IN PLATFORM
                }

                @Override
                public void setSweepGradient(
                        int @NonNull [] colors,
                        float @Nullable [] stops,
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
        flushPending3D();
        if (Float.isNaN(centerX)) {
            mCanvas.scale(scaleX, scaleY);
        } else {
            mCanvas.scale(scaleX, scaleY, centerX, centerY);
        }
    }

    @Override
    public void matrixTranslate(float translateX, float translateY) {
        flushPending3D();
        mCanvas.translate(translateX, translateY);
    }

    @Override
    public void matrixSkew(float skewX, float skewY) {
        flushPending3D();
        mCanvas.skew(skewX, skewY);
    }

    @Override
    public void matrixRotate(float rotate, float pivotX, float pivotY) {
        flushPending3D();
        if (Float.isNaN(pivotX)) {
            mCanvas.rotate(rotate);
        } else {
            mCanvas.rotate(rotate, pivotX, pivotY);
        }
    }

    @Override
    public void matrixSave() {
        // No flushPending3D(), as in save().
        mCanvas.save();
    }

    @Override
    public void matrixRestore() {
        flushPending3D();
        mCanvas.restore();
    }

    @Override
    public void clipRect(float left, float top, float right, float bottom) {
        flushPending3D();
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
        flushPending3D();
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
        flushPending3D();
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
        mContext.mRemoteComposeState.putPathData(out, p);
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

        mContext.mRemoteComposeState.putPath(out, p);
    }

    @Override
    public void reset() {
        // The previous canvas is finished (endFrame3D already composited the frame's 3D): drop
        // anything still deferred rather than drawing it onto that canvas.
        discardPending3D();
        // With out calling setTypeface before or after paint is reset()
        // Variable type fonts corrupt memory resulting in a
        // segmentation violation
        mPaint.setTypeface(Typeface.DEFAULT);
        mPaint.reset();
        releaseOffscreenBitmaps();
    }

    private Path getPath(int path1Id, int path2Id, float tween, float start, float end) {
        return getPath(getPathArray(path1Id, path2Id, tween), start, end);
    }

    private float[] getPathArray(int path1Id, int path2Id, float tween) {
        if (tween == 0.0f) {
            return mContext.mRemoteComposeState.getPathData(path1Id);
        }
        if (tween == 1.0f) {
            return mContext.mRemoteComposeState.getPathData(path2Id);
        }

        float[] data1 = mContext.mRemoteComposeState.getPathData(path1Id);
        float[] data2 = mContext.mRemoteComposeState.getPathData(path2Id);
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
        Path p = (Path) mContext.mRemoteComposeState.getPath(id);
        int w = mContext.mRemoteComposeState.getPathWinding(id);
        if (p != null) {
            return p;
        }
        Path path = new Path();
        float[] pathData = mContext.mRemoteComposeState.getPathData(id);
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
            mContext.mRemoteComposeState.putPath(id, path);
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
        flushPending3D();
        Path path = getPath(pathId, 0, 1);
        PathMeasure measure = new PathMeasure(path, false);
        float len = measure.getLength();
        Matrix matrix = new Matrix();
        measure.getMatrix((len * fraction) % len, matrix, flags);
        mCanvas.concat(matrix);
    }

    /** Geometry for one 2D mesh, already in the shape {@code Canvas.drawVertices} wants. */
    private static final class Mesh2D {
        final int mLayout;
        final int mUCount;
        final int mVCount;
        final float[] mVerts;
        final float[] mUv;
        final int[] mColors;
        final short[] mIndices;

        Mesh2D(
                int layout,
                int uCount,
                int vCount,
                float[] verts,
                float[] uv,
                int[] colors,
                short[] indices) {
            mLayout = layout;
            mUCount = uCount;
            mVCount = vCount;
            mVerts = verts;
            mUv = uv;
            mColors = colors;
            mIndices = indices;
        }
    }

    private final HashMap<Integer, Mesh2D> mMeshCache = new HashMap<>();
    private final HashMap<Integer, BitmapShader> mMeshShaderCache = new HashMap<>();

    @Override
    public void setMesh(
            int meshId,
            int layout,
            int uCount,
            int vCount,
            float @NonNull [] verts,
            float @NonNull [] uv,
            int @NonNull [] colors,
            int @NonNull [] indices) {
        // Android's drawVertices takes short[] indices, so narrow once here rather than per frame.
        short[] shortIndices = new short[indices.length];
        for (int i = 0; i < indices.length; i++) {
            shortIndices[i] = (short) indices[i];
        }
        float[] vertsCopy = verts.length > 0 ? verts.clone() : new float[0];
        float[] uvCopy = uv.length > 0 ? uv.clone() : new float[0];
        int[] colorsCopy = colors.length > 0 ? colors.clone() : new int[0];
        mMeshCache.put(
                meshId,
                new Mesh2D(layout, uCount, vCount, vertsCopy, uvCopy, colorsCopy, shortIndices));
    }

    @Override
    public void drawMesh(int meshId, int blend, int imageId) {
        flushPending3D();
        Mesh2D mesh = mMeshCache.get(meshId);
        if (mesh == null || mesh.mVerts.length == 0 || mesh.mIndices.length == 0) {
            return;
        }

        int vertexCount = mesh.mVerts.length;
        int[] colors = mesh.mColors.length > 0 ? mesh.mColors : null;

        // drawVertices takes no bitmap: the texture comes from the paint's shader, and texs are
        // coordinates in shader space. Passing texs with no shader is meaningless, so an
        // untextured mesh must pass null.
        Shader previousShader = mPaint.getShader();
        boolean shaderInstalled = false;
        float[] texs = null;

        try {
            if (imageId != DrawMesh2D.NO_IMAGE && mesh.mUv.length == mesh.mVerts.length) {
                Bitmap bitmap = (Bitmap) mContext.mRemoteComposeState.getFromId(imageId);
                if (bitmap != null) {
                    BitmapShader shader = mMeshShaderCache.get(imageId);
                    if (shader == null) {
                        shader =
                                new BitmapShader(
                                        bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                        mMeshShaderCache.put(imageId, shader);
                    }
                    mPaint.setShader(shader);
                    shaderInstalled = true;

                    // uv is normalised 0..1 with (0,0) at the top left, and both Android and Skia
                    // want image pixels, so the conversion is a plain multiply with no flip. The
                    // 3D path next door flips v for the GL convention; 2D deliberately does not.
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    texs = new float[mesh.mUv.length];
                    for (int i = 0; i < mesh.mUv.length; i += 2) {
                        texs[i] = mesh.mUv[i] * width;
                        texs[i + 1] = mesh.mUv[i + 1] * height;
                    }
                }
            }

            if (blend == DrawMesh2D.BLEND_COLORS_ONLY) {
                // colours only: ignore any texture and let the vertex colours through
                texs = null;
                if (shaderInstalled) {
                    mPaint.setShader(previousShader);
                    shaderInstalled = false;
                }
            }

            mCanvas.drawVertices(
                    Canvas.VertexMode.TRIANGLES,
                    vertexCount,
                    mesh.mVerts,
                    0,
                    texs,
                    0,
                    colors,
                    0,
                    mesh.mIndices,
                    0,
                    mesh.mIndices.length,
                    mPaint);
        } finally {
            // Paint is canvas state; a backend that borrows it must give it back, including when
            // the draw throws.
            if (shaderInstalled) {
                mPaint.setShader(previousShader);
            }
        }
    }

    @Override
    public void matrixFromMesh(int meshId, float u, float v, int flags) {
        flushPending3D();
        Mesh2D mesh = mMeshCache.get(meshId);
        if (mesh == null) {
            return;
        }
        float[] affine = new float[6];
        if (!Mesh2DGenerator.computeMatrixFromMesh(
                mesh.mLayout, mesh.mUCount, mesh.mVCount, mesh.mVerts, u, v, flags, affine)) {
            return;
        }
        Matrix m = new Matrix();
        // [duX, duY, dvX, dvY, originX, originY] -> the 3x3 Android wants, row major
        m.setValues(
                new float[]{
                        affine[0], affine[2], affine[4], affine[1], affine[3], affine[5], 0f, 0f, 1f
                });
        mCanvas.concat(m);
    }

    java.util.WeakHashMap<Bitmap, Canvas> mCCache = new java.util.WeakHashMap<>();

    @Override
    public void drawToBitmap(int bitmapId, int mode, int color) {
        flushPending3D();
        if (mMainCanvas == null) {
            mMainCanvas = mCanvas;
        }
        if (bitmapId == 0) {
            mCanvas = mMainCanvas;
            return;
        }
        mPaint.setAlpha(255);
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

    /** Logs 3D diagnostics (the engine each scene uses). Off: the checks compile away. */
    private static final boolean DEBUG_3D = false;

    // Engine of the last 3D draw logged in the current scene (-1: none yet), for DEBUG_3D.
    private int mDebug3DEngine = -1;

    private JavaPaint3DContext m3D;
    // True while 3D output waits to be composited onto mCanvas: software-rasterized pixels in
    // the engine's pending rectangle and/or a deferred drawmesh-zbuf batch. A run of 3D draws
    // accumulates, and flushPending3D() composites it once, at the first 2D canvas call,
    // clearDepth3D(), endFrame3D() or draw on another engine that must land over it.
    private boolean m3DPending;
    private Paint m3DBlitPaint;
    // Bitmaps for compositing the software engine's pixels. A recording (hardware) canvas keeps
    // a reference to each bitmap it draws rather than a copy, so each blit of a frame on such a
    // canvas needs its own bitmap: slot i (1, 2, ...) serves the i-th since the canvas was
    // installed, and the pool grows to the most blits a frame makes. Slot 0 serves canvases
    // that composite immediately. A slot is reconfigured to exactly the blitted size and
    // reallocated only when outgrown. At the end of every BLIT_POOL_WINDOW_FRAMES frames,
    // endFrame3D() drops the bitmaps of the slots no frame of those frames used.
    private Bitmap[] m3DBlitBitmaps = new Bitmap[4];
    private int m3DBlitCount; // blits on recording canvases since the canvas was installed
    private int m3DBlitWindowMax; // most blits of one frame in the current trim window
    private int m3DBlitWindowFrames; // frames ended in the current trim window
    /** Frames per trim window of the 3D blit pool (see {@link #trimBlitBitmaps()}). */
    static final int BLIT_POOL_WINDOW_FRAMES = 120;
    private Bitmap m3DTexture;
    private int m3DTextureId;
    // The current texture's ARGB pixels, for the software engine. Reused; grown when outgrown.
    private int[] m3DTexturePixels;
    private JavaPaint3DContext.CanvasMesh m3DCanvasMesh;
    private float[] m3DTexs;
    private Paint m3DVertPaint;
    private Paint m3DTexPaint;
    private Bitmap m3DTexShaderBitmap;
    // Canvas.drawMesh backend (API 34+)
    private ByteBuffer m3DDrawMeshBuf;
    private Object m3DDrawMeshSpec;     // Object to avoid class-verify on API < 34
    private Object m3DDrawMeshRTShader; // RuntimeShader (API 33+), same reason
    private Paint m3DDrawMeshPaint;
    // Infinite plane in the canvas engines: its visible polygon and the grid lines' paint.
    private final float[] m3DPlaneXY = new float[2 * InfinitePlane.MAX_POLYGON_VERTICES];
    private final int[] m3DPlaneColors = new int[InfinitePlane.MAX_POLYGON_VERTICES];
    private Paint m3DPlaneLinePaint;

    // ---- drawmesh-zbuf engine (API 34+) ---------------------------------------------
    // A CPU painter's sort over a run of draws: drawMesh3DZBuf() appends each draw's projected,
    // shaded triangles to the scene arrays, and flushZBufScene() sorts them all back to front
    // and paints them with one Canvas.drawVertices call (Canvas.drawMesh crashed in
    // Mesh::updateSkMesh on some devices). The arrays grow as needed and are reused.
    private float[] mZBufXY = new float[0]; // scene: [x0,y0, x1,y1, ...] in draw order
    private int[] mZBufArgb = new int[0]; // scene: ARGB per vertex
    private float[] mZBufDepth = new float[0]; // scene: window depth per triangle (vertex 0)
    private int mZBufVertexCount; // vertices in the scene arrays
    private long[] mZBufOrder = new long[0]; // sort keys: inverted depth << 32 | triangle
    private float[] mZBufPositions; // the sorted triangles' [x0,y0, x1,y1, ...]
    private int[] mZBufColors; // the sorted triangles' ARGB per vertex
    private Paint mZBufPaint; // paint for the merged drawVertices call

    /** Returns the software 3D engine, creating it (unsized) on first use. */
    private @NonNull JavaPaint3DContext get3D() {
        JavaPaint3DContext ctx = m3D;
        if (ctx == null) {
            ctx = new JavaPaint3DContext();
            m3D = ctx;
        }
        return ctx;
    }

    /**
     * Returns the 3D engine sized to the current viewport, or {@code null} if the viewport is
     * empty. The viewport is the component being painted (the content box of a layout
     * component), so 3D in a canvas smaller than the document is projected into that canvas.
     * With no component, while drawing into an offscreen bitmap, or if the component's area is
     * larger than the canvas's (e.g. a canvas inside a scroller), it is the whole canvas: the
     * engine's color and depth buffers never hold more pixels than the canvas. A component
     * wider or taller than the canvas but with no larger area keeps its own viewport, so its
     * scene stays where the component is.
     * Resizing drops the current scene, so anything pending is composited first.
     */
    private @Nullable JavaPaint3DContext ensureViewport3D() {
        int canvasWidth = mCanvas.getWidth();
        int canvasHeight = mCanvas.getHeight();
        int w = canvasWidth;
        int h = canvasHeight;
        Component c = mContext.mLastComponent;
        boolean offscreen = mNode == null && mMainCanvas != null && mCanvas != mMainCanvas;
        if (c != null && !offscreen) {
            float cw = c.getWidth();
            float ch = c.getHeight();
            if (c instanceof LayoutComponent) {
                LayoutComponent lc = (LayoutComponent) c;
                cw -= lc.getPaddingLeft() + lc.getPaddingRight();
                ch -= lc.getPaddingTop() + lc.getPaddingBottom();
            }
            int vw = Math.round(cw);
            int vh = Math.round(ch);
            if ((long) vw * vh <= (long) canvasWidth * canvasHeight) {
                w = vw;
                h = vh;
            }
        }
        if (w <= 0 || h <= 0) {
            return null;
        }
        JavaPaint3DContext ctx = get3D();
        if (ctx.getWidth() != w || ctx.getHeight() != h || ctx.getColorBuffer() == null) {
            flushPending3D();
            ctx.setSize(w, h);
            if (ctx.getColorBuffer() == null) {
                return null;
            }
        }
        return ctx;
    }

    /**
     * Returns a mutable ARGB_8888 bitmap of exactly {@code w} x {@code h} for the next blit,
     * from {@link #m3DBlitBitmaps}: slot 0 for a canvas that composites immediately, the next
     * unused slot for a recording canvas (growing the pool when a frame has used them all).
     */
    private @NonNull Bitmap obtainBlitBitmap(int w, int h) {
        int slot = mCanvas.isHardwareAccelerated() ? ++m3DBlitCount : 0;
        Bitmap[] pool = m3DBlitBitmaps;
        if (slot >= pool.length) {
            pool = Arrays.copyOf(pool, Math.max(slot + 1, 2 * pool.length));
            m3DBlitBitmaps = pool;
        }
        Bitmap bm = pool[slot];
        if (bm == null || bm.getAllocationByteCount() < 4L * w * h) {
            // Round up to 64 pixels so a slowly growing rectangle doesn't reallocate each frame.
            bm = Bitmap.createBitmap((w + 63) & ~63, (h + 63) & ~63, Bitmap.Config.ARGB_8888);
            pool[slot] = bm;
        }
        if (bm.getWidth() != w || bm.getHeight() != h) {
            bm.reconfigure(w, h, Bitmap.Config.ARGB_8888);
        }
        return bm;
    }

    /**
     * Ends a frame for the blit pool. Every {@link #BLIT_POOL_WINDOW_FRAMES} frames, drops the
     * bitmaps above the most blits any of those frames made, so a burst of blits doesn't keep
     * its bitmaps for good. Never drops slot 0. A recording canvas still holding a dropped
     * bitmap keeps it alive until it is done with it.
     */
    private void trimBlitBitmaps() {
        m3DBlitWindowMax = Math.max(m3DBlitWindowMax, m3DBlitCount);
        if (++m3DBlitWindowFrames < BLIT_POOL_WINDOW_FRAMES) {
            return;
        }
        Bitmap[] pool = m3DBlitBitmaps;
        for (int slot = m3DBlitWindowMax + 1; slot < pool.length; slot++) {
            pool[slot] = null;
        }
        m3DBlitWindowMax = 0;
        m3DBlitWindowFrames = 0;
    }

    /**
     * Draws the engine's pending rectangle at its position, then clears its color (keeping its
     * depth, so later draws of the same scene stay hidden behind earlier ones). Only the
     * rectangle is copied; everything outside it is transparent.
     */
    private void blit3D(@NonNull JavaPaint3DContext ctx) {
        int[] pixels = ctx.getColorBuffer();
        if (pixels == null || !ctx.hasPendingPixels()) {
            return;
        }
        int left = ctx.getPendingLeft();
        int top = ctx.getPendingTop();
        int w = ctx.getPendingRight() - left;
        int h = ctx.getPendingBottom() - top;
        int stride = ctx.getWidth();
        Bitmap bm = obtainBlitBitmap(w, h);
        bm.setPixels(pixels, top * stride + left, stride, 0, 0, w, h);
        Paint p = m3DBlitPaint;
        if (p == null) {
            p = new Paint();
            m3DBlitPaint = p;
        }
        mCanvas.drawBitmap(bm, left, top, p);
        ctx.clearPendingColor();
    }

    /**
     * Composites pending 3D output onto {@link #mCanvas}: the software engine's pixels with one
     * blit, then any deferred drawmesh-zbuf batch. Called before every 2D canvas call that
     * draws or changes the matrix, clip or target, and by {@link #clearDepth3D()} and
     * {@link #endFrame3D()}, so 3D lands under the matrix, clip and layer it was drawn with, in
     * order with the 2D content around it.
     *
     * <p>That order is document order. Draws on different engines land in document order
     * (PROTOCOL_3D.md §4.2, rule 5): a software draw composites a pending zbuf batch before it
     * draws, so pending software pixels always come from draws before the batch's, and a canvas
     * engine drawing straight onto the canvas composites the other engines' output first.
     */
    private void flushPending3D() {
        if (!m3DPending) {
            return;
        }
        m3DPending = false;
        if (m3D != null) {
            blit3D(m3D);
        }
        flushZBufScene();
    }

    /** Drops pending 3D output undrawn and empties the scene, for a finished canvas. */
    private void discardPending3D() {
        m3DPending = false;
        mZBufVertexCount = 0;
        if (m3D != null) {
            m3D.clearDepth3D();
        }
        m3DBlitCount = 0;
    }

    @Override
    public void defineMesh3D(
            int id,
            int @NonNull [] indices,
            float @NonNull [] verts,
            float @Nullable [] normals) {
        get3D().defineMesh3D(id, indices, verts, normals);
    }

    @Override
    public void defineMesh3D(
            int id,
            int @NonNull [] indices,
            float @NonNull [] verts,
            float @Nullable [] normals,
            float @Nullable [] uv) {
        get3D().defineMesh3D(id, indices, verts, normals, uv);
    }

    @Override
    public void defineInfinitePlane3D(int id, int flags, float @NonNull [] params) {
        get3D().defineInfinitePlane3D(id, flags, params);
    }

    @Override
    public void setCamera3D(
            int projection, float @NonNull [] projParams, float @NonNull [] viewParams) {
        get3D().setCamera3D(projection, projParams, viewParams);
    }

    @Override
    public void matrix3DOp(int sub, float @NonNull [] args) {
        get3D().matrix3DOp(sub, args);
    }

    @Override
    public void clearDepth3D() {
        // Composite everything drawn so far (software pixels and any buffered Z-buf meshes)
        // before starting a new scene.
        flushPending3D();
        mDebug3DEngine = -1;
        JavaPaint3DContext ctx = ensureViewport3D();
        if (ctx == null) {
            // Nothing to clear at this size, but the scene still starts over (matrix stack).
            ctx = get3D();
        }
        ctx.clearDepth3D();
    }

    @Override
    public void endFrame() {
        endFrame3D();
    }

    @Override
    public void endFrame3D() {
        // Composite the frame's remaining 3D, then empty the scene so the next frame starts with
        // a clear depth buffer (and an empty matrix stack) even if the document never calls
        // clearDepth3D().
        flushPending3D();
        if (m3D != null) {
            m3D.clearDepth3D();
        }
        trimBlitBitmaps();
    }

    @Override
    public void setLights3D(
            int @NonNull [] types, int @NonNull [] colors, float @NonNull [] params) {
        get3D().setLights3D(types, colors, params);
    }

    @Override
    public void setTexture3D(int bitmapId) {
        JavaPaint3DContext ctx = get3D();
        if (bitmapId == 0) {
            m3DTexture = null;
            m3DTextureId = 0;
            ctx.setTextureData(null, 0, 0);
            return;
        }
        if (bitmapId == m3DTextureId && m3DTexture != null) {
            return; // unchanged; pixels already supplied to the engine
        }
        if (!mContext.mRemoteComposeState.containsId(bitmapId)) {
            return;
        }
        Object obj = mContext.mRemoteComposeState.getFromId(bitmapId);
        if (!(obj instanceof Bitmap)) {
            return;
        }
        Bitmap bm = (Bitmap) obj;
        m3DTexture = bm;
        m3DTextureId = bitmapId;
        // Decode the bitmap to ARGB pixels for the software textured rasterizer, into a reused
        // array: the engine reads the texture only while drawing, never after the next switch.
        int w = bm.getWidth();
        int h = bm.getHeight();
        int[] pixels = m3DTexturePixels;
        if (pixels == null || pixels.length < w * h) {
            pixels = new int[w * h];
            m3DTexturePixels = pixels;
        }
        bm.getPixels(pixels, 0, w, 0, 0, w, h);
        ctx.setTextureData(pixels, w, h);
    }

    @Override
    public void setMaterial3D(float specStrength, float shininess) {
        // The software engine owns the lighting math used by both the software rasterizer and the
        // canvas (drawVertices) vertex colors, so forwarding here glosses both backends.
        get3D().setMaterial3D(specStrength, shininess);
    }

    @Override
    public void setDepthBias3D(float constant, float slope) {
        // Depth bias applies to the depth-buffered software path (the canvas backend is painter-
        // ordered and has no shared depth buffer).
        get3D().setDepthBias3D(constant, slope);
    }

    /**
     * Paints the drawmesh-zbuf scene with a single {@link Canvas#drawVertices} call, clipped to
     * the viewport: all its triangles, sorted back to front by their first vertex's window
     * depth, a painter's sort across the run's draws. {@code drawVertices} rather than
     * {@link Canvas#drawMesh}, which crashed in {@code Mesh::updateSkMesh} on some devices.
     * Allocates nothing once its arrays have grown to the size of the scene.
     */
    private void flushZBufScene() {
        int triangles = mZBufVertexCount / 3;
        mZBufVertexCount = 0;
        if (triangles == 0) {
            return;
        }
        long[] order = mZBufOrder;
        if (order.length < triangles) {
            order = new long[Math.max(triangles, 2 * order.length)];
            mZBufOrder = order;
        }
        sortBackToFront(mZBufDepth, triangles, order);

        // The sorted triangles, in arrays as large as the scene's.
        int vertices = 3 * triangles;
        float[] xy = mZBufPositions;
        if (xy == null || xy.length < 2 * vertices) {
            xy = new float[mZBufXY.length];
            mZBufPositions = xy;
        }
        int[] argb = mZBufColors;
        if (argb == null || argb.length < vertices) {
            argb = new int[mZBufArgb.length];
            mZBufColors = argb;
        }
        float[] sceneXY = mZBufXY;
        int[] sceneArgb = mZBufArgb;
        for (int i = 0; i < triangles; i++) {
            int t = (int) order[i]; // the low 32 bits: the triangle
            System.arraycopy(sceneXY, 6 * t, xy, 6 * i, 6);
            System.arraycopy(sceneArgb, 3 * t, argb, 3 * i, 3);
        }

        if (mZBufPaint == null) {
            mZBufPaint = new Paint();
            mZBufPaint.setAntiAlias(true);
            mZBufPaint.setColor(0xFFFFFFFF); // neutral: per-vertex colors drive the output
        }
        int saveCount = m3D != null ? clipToViewport3D(m3D) : mCanvas.save();
        mCanvas.drawVertices(
                Canvas.VertexMode.TRIANGLES,
                2 * vertices,
                xy, 0,
                null, 0,
                argb, 0,
                null, 0, 0,
                mZBufPaint);
        mCanvas.restoreToCount(saveCount);
    }

    /**
     * Sorts triangles {@code 0} to {@code n - 1} back to front: by decreasing {@code depth},
     * compared as by {@link Float#compare}, and ties in increasing index order. Writes one key
     * per triangle to {@code order}, in sorted order; the low 32 bits of a key are its
     * triangle. ({@link Arrays#sort(long[], int, int)} may allocate a merge buffer for a large
     * input that is nearly sorted already.)
     */
    static void sortBackToFront(float @NonNull [] depth, int n, long @NonNull [] order) {
        for (int t = 0; t < n; t++) {
            int bits = Float.floatToIntBits(depth[t]);
            // As signed ints these compare as Float.compare compares the floats (-0 before +0,
            // NaN last); inverted, they sort far to near, and the index breaks ties.
            int ascending = bits ^ ((bits >> 31) & 0x7FFFFFFF);
            order[t] = ((long) ~ascending << 32) | t;
        }
        Arrays.sort(order, 0, n);
    }

    /**
     * Lazily compiles {@link #m3DDrawMeshSpec} and {@link #m3DDrawMeshRTShader}, for
     * {@link #drawMesh3DCanvasDrawMesh}. (The drawmesh-zbuf engine paints with
     * {@link Canvas#drawVertices} and needs neither.)
     */
    @RequiresApi(34)
    private void ensureDrawMeshSpec() {
        if (m3DDrawMeshSpec != null) return;

        // 24-byte vertex layout: float2 position (0) | float4 color (8).
        m3DDrawMeshSpec = MeshSpecification.make(
                new MeshSpecification.Attribute[]{
                        new MeshSpecification.Attribute(
                                MeshSpecification.TYPE_FLOAT2, 0, "position"),
                        new MeshSpecification.Attribute(
                                MeshSpecification.TYPE_FLOAT4, 8, "color"),
                },
                24,
                new MeshSpecification.Varying[]{
                        new MeshSpecification.Varying(
                                MeshSpecification.TYPE_FLOAT4, "vColor"),
                },
                "Varyings main(const Attributes a) {\n"
                        + "    Varyings v;\n"
                        + "    v.position = a.position;\n"
                        + "    v.vColor   = a.color;\n"
                        + "    return v;\n"
                        + "}\n",
                "float2 main(const Varyings v, out float4 color) {\n"
                        + "    color = float4(0.0, 0.0, 0.0, 1.0);\n"
                        + "    float ri = floor(v.vColor.r * 255.0 + 0.5);\n"
                        + "    float gi = floor(v.vColor.g * 255.0 + 0.5);\n"
                        + "    float bi = floor(v.vColor.b * 255.0 + 0.5);\n"
                        + "    return float2(ri, gi * 256.0 + bi);\n"
                        + "}\n",
                ColorSpace.get(ColorSpace.Named.SRGB),
                MeshSpecification.ALPHA_TYPE_PREMULTIPLIED);

        m3DDrawMeshRTShader = new RuntimeShader(
                "half4 main(float2 coord) {\n"
                        + "    half r = half(coord.x / 255.0);\n"
                        + "    half g = half(floor(coord.y / 256.0) / 255.0);\n"
                        + "    half b = half(mod(coord.y, 256.0) / 255.0);\n"
                        + "    return half4(r, g, b, 1.0);\n"
                        + "}\n");
    }

    /**
     * Appends one mesh draw to the drawmesh-zbuf scene: its projected, shaded triangles and
     * each triangle's first vertex's window depth. {@link #flushPending3D()} paints the scene
     * (at the next 2D canvas call, {@link #clearDepth3D()} or the end of the frame).
     */
    @RequiresApi(34)
    private void drawMesh3DZBuf(
            @NonNull JavaPaint3DContext ctx, int meshId, int mode) {
        JavaPaint3DContext.CanvasMesh mesh = m3DCanvasMesh;
        if (mesh == null) {
            mesh = new JavaPaint3DContext.CanvasMesh();
            m3DCanvasMesh = mesh;
        }
        int triangles = ctx.buildCanvasVertices(meshId, mesh, mode) / 3;
        if (triangles == 0) return;

        int vertices = 3 * triangles;
        int start = mZBufVertexCount;
        int end = start + vertices;
        if (mZBufArgb.length < end) {
            int capacity = Math.max(end, 2 * mZBufArgb.length); // in vertices
            mZBufXY = Arrays.copyOf(mZBufXY, 2 * capacity);
            mZBufArgb = Arrays.copyOf(mZBufArgb, capacity);
            mZBufDepth = Arrays.copyOf(mZBufDepth, capacity / 3);
        }
        System.arraycopy(mesh.mPositions, 0, mZBufXY, 2 * start, 2 * vertices);
        System.arraycopy(mesh.mColors, 0, mZBufArgb, start, vertices);
        float[] vertexDepth = mesh.mDepths;
        float[] depth = mZBufDepth;
        int first = start / 3;
        for (int t = 0; t < triangles; t++) {
            depth[first + t] = vertexDepth[3 * t];
        }
        mZBufVertexCount = end;
        m3DPending = true;
    }

    @Override
    public void drawMesh3D(int meshId, int mode) {
        JavaPaint3DContext ctx = ensureViewport3D();
        if (ctx == null) return;
        ctx.setBaseColorArgb(mPaint.getColor());
        // Bits 1-3 pick the engine. Only the software engine draws wireframe, and it is also the
        // fallback for engines this player doesn't implement (GL, canvas-zbuf, reserved) or
        // can't use on this API level. Higher bits never change the engine. Every engine gets
        // the whole mode: shading, MODE_DOUBLE_SIDED, MODE_FRONT_CW and MODE_TRANSPARENT (the
        // canvas engines blend the paint alpha anyway, in painter's order).
        int engine = (mode >> Paint3DContext.MODE_ENGINE_SHIFT) & Paint3DContext.MODE_ENGINE_MASK;
        if ((mode & Paint3DContext.MODE_WIREFRAME) != 0) {
            engine = Paint3DContext.MODE_BACKEND_SOFTWARE;
        }
        int used; // the engine that draws, for the debug log
        if (engine == Paint3DContext.MODE_BACKEND_CANVAS) {
            used = engine;
            flushPending3D();
            int saveCount = clipToViewport3D(ctx);
            if (!drawPlane3DCanvas(ctx, meshId, mode)) {
                drawMesh3DCanvasVertices(ctx, meshId, mode);
            }
            mCanvas.restoreToCount(saveCount);
        } else if (engine == Paint3DContext.MODE_BACKEND_DRAWMESH_ZBUF
                && Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // REMOVE IN PLATFORM
            used = engine;
            if (ctx.getInfinitePlane(meshId) != null) {
                // Drawn now: over the software pixels drawn before it, composited first
                // (document order), and under the pending sorted batch, an approximation: a
                // floor behind the run's meshes.
                blit3D(ctx);
                int saveCount = clipToViewport3D(ctx);
                drawPlane3DCanvas(ctx, meshId, mode);
                mCanvas.restoreToCount(saveCount);
            } else {
                drawMesh3DZBuf(ctx, meshId, mode);
            }
        } else if (engine == Paint3DContext.MODE_BACKEND_DRAWMESH
                && Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // REMOVE IN PLATFORM
            flushPending3D();
            int saveCount = clipToViewport3D(ctx);
            if (drawPlane3DCanvas(ctx, meshId, mode)) {
                used = Paint3DContext.MODE_BACKEND_CANVAS;
            } else if ((mode & Paint3DContext.MODE_TRANSPARENT) != 0) {
                // The drawMesh shader writes opaque colors: blend through drawVertices instead,
                // which draws the same triangles in the same order.
                used = Paint3DContext.MODE_BACKEND_CANVAS;
                drawMesh3DCanvasVertices(ctx, meshId, mode);
            } else {
                used = engine;
                drawMesh3DCanvasDrawMesh(ctx, meshId, mode);
            }
            mCanvas.restoreToCount(saveCount);
        } else {
            used = Paint3DContext.MODE_BACKEND_SOFTWARE;
            // Software. The pixels stay in the engine until flushPending3D() composites the
            // whole run of 3D draws with a single blit. A pending drawmesh-zbuf batch was drawn
            // before this draw, so it is composited first (document order).
            if (mZBufVertexCount > 0) {
                flushPending3D();
            }
            ctx.drawMesh3D(meshId, mode);
            if (ctx.hasPendingPixels()) {
                m3DPending = true;
            }
        }
        if (DEBUG_3D && used != mDebug3DEngine) {
            mDebug3DEngine = used;
            Utils.log("3D scene: engine " + used + " draws mesh " + meshId + " (mode 0x"
                    + Integer.toHexString(mode) + ")");
        }
    }

    /**
     * Saves {@link #mCanvas} and clips it to the 3D viewport, so the triangles of a canvas
     * engine stay inside the painted component, as the software engine's pixels do.
     *
     * @return the save count to restore
     */
    private int clipToViewport3D(@NonNull JavaPaint3DContext ctx) {
        int saveCount = mCanvas.save();
        mCanvas.clipRect(0f, 0f, ctx.getWidth(), ctx.getHeight());
        return saveCount;
    }

    /**
     * Canvas engines: draws mesh {@code meshId} if it is an infinite plane, as one polygon (the
     * plane ∩ the view frustum, not split; its vertex colors carry the paint alpha, so a
     * transparent plane blends) with the grid lines over it, in call order like the meshes.
     *
     * @return whether {@code meshId} is an infinite plane (drawn, or culled by its mode)
     */
    private boolean drawPlane3DCanvas(@NonNull JavaPaint3DContext ctx, int meshId, int mode) {
        InfinitePlane plane = ctx.getInfinitePlane(meshId);
        if (plane == null) {
            return false;
        }
        int n = ctx.buildCanvasPlane(meshId, mode, m3DPlaneXY, null, m3DPlaneColors);
        if (n < 3) {
            return true;
        }
        mCanvas.drawVertices(Canvas.VertexMode.TRIANGLE_FAN, n * 2, m3DPlaneXY, 0, null, 0,
                m3DPlaneColors, 0, null, 0, 0, vertexPaint3D());
        if (ctx.buildCanvasPlaneGrid(meshId, mode) == 0) {
            return true;
        }
        Paint lp = m3DPlaneLinePaint;
        if (lp == null) {
            lp = new Paint(Paint.ANTI_ALIAS_FLAG);
            lp.setStyle(Paint.Style.STROKE);
            lp.setStrokeCap(Paint.Cap.BUTT);
            m3DPlaneLinePaint = lp;
        }
        lp.setStrokeWidth(plane.getLineWidth());
        for (int level = 0; level < InfinitePlane.GRID_LEVELS; level++) {
            int count = plane.getGridLineCount(level);
            if (count > 0) {
                lp.setColor(plane.getGridLineColor(level));
                mCanvas.drawLines(plane.getGridLines(level), 0, 4 * count, lp);
            }
        }
        return true;
    }

    /** The paint of the canvas engines' drawVertices: a neutral base for the vertex colors. */
    private @NonNull Paint vertexPaint3D() {
        Paint p = m3DVertPaint;
        if (p == null) {
            p = new Paint();
            p.setAntiAlias(true);
            // drawVertices uses the per-vertex colors; keep the paint a neutral opaque base.
            p.setColor(0xFFFFFFFF);
            m3DVertPaint = p;
        }
        return p;
    }

    /**
     * Canvas backend: the engine projects + shades (flat or smooth) the mesh into a
     * painter-sorted screen-space triangle soup, which we hand to {@link Canvas#drawVertices}
     * so the GPU does the fill — no software-rasterized bitmap blit. Visibility relies on the
     * engine's backface culling and back-to-front ordering rather than a per-pixel depth
     * buffer.
     */
    private void drawMesh3DCanvasVertices(
            @NonNull JavaPaint3DContext ctx, int meshId, int mode) {
        JavaPaint3DContext.CanvasMesh mesh = m3DCanvasMesh;
        if (mesh == null) {
            mesh = new JavaPaint3DContext.CanvasMesh();
            m3DCanvasMesh = mesh;
        }
        int vertexCount = ctx.buildCanvasVertices(meshId, mesh, mode);
        if (vertexCount < 3) {
            return;
        }

        Bitmap tex = m3DTexture;
        if (tex != null && mesh.mHasUv) {
            // Textured: a BitmapShader on the paint + per-vertex texture coords (UV scaled to
            // bitmap pixels). The lit vertex colors modulate the sampled texture.
            int bw = tex.getWidth();
            int bh = tex.getHeight();
            if (m3DTexs == null || m3DTexs.length < vertexCount * 2) {
                m3DTexs = new float[vertexCount * 2];
            }
            float[] texs = m3DTexs;
            float[] uvs = mesh.mUvs;
            for (int i = 0; i < vertexCount; i++) {
                texs[i * 2] = uvs[i * 2] * bw;
                // UV v is bottom-up; bitmap rows are top-down, so flip v.
                texs[i * 2 + 1] = (1f - uvs[i * 2 + 1]) * bh;
            }
            Paint tp = m3DTexPaint;
            if (tp == null) {
                tp = new Paint();
                tp.setAntiAlias(true);
                tp.setFilterBitmap(true);
                m3DTexPaint = tp;
            }
            if (m3DTexShaderBitmap != tex) {
                tp.setShader(
                        new BitmapShader(tex, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
                m3DTexShaderBitmap = tex;
            }
            mCanvas.drawVertices(
                    Canvas.VertexMode.TRIANGLES,
                    vertexCount * 2,
                    mesh.mPositions,
                    0,
                    texs,
                    0,
                    mesh.mColors,
                    0,
                    null,
                    0,
                    0,
                    tp);
            return;
        }

        Paint p = vertexPaint3D();
        mCanvas.drawVertices(
                Canvas.VertexMode.TRIANGLES,
                vertexCount * 2,
                mesh.mPositions,
                0,
                null,
                0,
                mesh.mColors,
                0,
                null,
                0,
                0,
                p);
    }

    /**
     * Canvas.drawMesh backend (API 34+): projects + shades the mesh identically to the
     * drawVertices path (CPU Lambert/Gouraud via {@link JavaPaint3DContext#buildCanvasVertices}),
     * then uploads the result as an {@link android.graphics.Mesh} whose AGSL shaders pass
     * CPU-computed per-vertex RGBA colours through to the GPU interpolator.
     *
     * <p>Compared to {@link #drawMesh3DCanvasVertices}: same painter-sort ordering, same CPU
     * shading. The difference is the draw call: {@link Canvas#drawMesh} goes through Skia's
     * programmable-mesh path rather than the older fixed-function {@link Canvas#drawVertices}.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private void drawMesh3DCanvasDrawMesh(
            @NonNull JavaPaint3DContext ctx, int meshId, int mode) {
        JavaPaint3DContext.CanvasMesh mesh = m3DCanvasMesh;
        if (mesh == null) {
            mesh = new JavaPaint3DContext.CanvasMesh();
            m3DCanvasMesh = mesh;
        }
        int vertexCount = ctx.buildCanvasVertices(meshId, mesh, mode);
        if (vertexCount < 3) return;

        // Build an interleaved vertex buffer: float2 position (8 bytes) + float4 RGBA (16 bytes).
        int stride = 24; // bytes per vertex
        int bufBytes = vertexCount * stride;
        if (m3DDrawMeshBuf == null || m3DDrawMeshBuf.capacity() < bufBytes) {
            m3DDrawMeshBuf = ByteBuffer.allocateDirect(bufBytes).order(ByteOrder.nativeOrder());
        }
        ByteBuffer buf = m3DDrawMeshBuf;
        buf.clear();
        float[] pos = mesh.mPositions;
        int[] col = mesh.mColors;
        for (int i = 0; i < vertexCount; i++) {
            buf.putFloat(pos[i * 2]);
            buf.putFloat(pos[i * 2 + 1]);
            int argb = col[i];
            buf.putFloat(((argb >> 16) & 0xFF) * (1f / 255f)); // r
            buf.putFloat(((argb >> 8) & 0xFF) * (1f / 255f)); // g
            buf.putFloat((argb & 0xFF) * (1f / 255f)); // b
            buf.putFloat(((argb >> 24) & 0xFF) * (1f / 255f)); // a
        }
        buf.flip();

        // Lazily compile the MeshSpecification + RuntimeShader.
        ensureDrawMeshSpec();

        MeshSpecification spec = (MeshSpecification) m3DDrawMeshSpec;
        Mesh androidMesh = new Mesh(
                spec,
                Mesh.TRIANGLES,
                buf,
                vertexCount,
                new RectF(0, 0, ctx.getWidth(), ctx.getHeight()));

        Paint p = m3DDrawMeshPaint;
        if (p == null) {
            p = new Paint();
            p.setAntiAlias(true);
            m3DDrawMeshPaint = p;
        }
        p.setShader((RuntimeShader) m3DDrawMeshRTShader);
        mCanvas.drawMesh(androidMesh, BlendMode.SRC_OVER, p);
    }
}
