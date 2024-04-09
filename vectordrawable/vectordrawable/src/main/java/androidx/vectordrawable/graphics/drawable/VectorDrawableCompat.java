/*
 * Copyright (C) 2015 The Android Open Source Project
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

package androidx.vectordrawable.graphics.drawable;

import static android.graphics.Color.TRANSPARENT;
import static android.graphics.Color.alpha;
import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.graphics.Paint.Cap;
import static android.graphics.Paint.Join;
import static android.graphics.Paint.Style.FILL;
import static android.graphics.Paint.Style.STROKE;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;
import androidx.core.content.res.ComplexColorCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.content.res.TypedArrayUtils;
import androidx.core.graphics.PathParser;
import androidx.core.graphics.drawable.DrawableCompat;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;

/**
 * For API 24 and above, this class delegates to the framework's {@link VectorDrawable}.
 * For older API version, this class lets you create a drawable based on an XML vector graphic.
 * <p/>
 * You can always create a VectorDrawableCompat object and use it as a Drawable by the Java API.
 * In order to refer to VectorDrawableCompat inside a XML file, you can use app:srcCompat attribute
 * in AppCompat library's ImageButton or ImageView.
 * <p/>
 * <strong>Note:</strong> To optimize for the re-drawing performance, one bitmap cache is created
 * for each VectorDrawableCompat. Therefore, referring to the same VectorDrawableCompat means
 * sharing the same bitmap cache. If these references don't agree upon on the same size, the bitmap
 * will be recreated and redrawn every time size is changed. In other words, if a VectorDrawable is
 * used for different sizes, it is more efficient to create multiple VectorDrawables, one for each
 * size.
 * <p/>
 * VectorDrawableCompat can be defined in an XML file with the <code>&lt;vector></code> element.
 * <p/>
 * The VectorDrawableCompat has the following elements:
 * <p/>
 * <dl>
 * <dt><code>&lt;vector></code></dt>
 * <dd>Used to define a vector drawable
 * <dl>
 * <dt><code>android:name</code></dt>
 * <dd>Defines the name of this vector drawable.</dd>
 * <dt><code>android:width</code></dt>
 * <dd>Used to define the intrinsic width of the drawable.
 * This support all the dimension units, normally specified with dp.</dd>
 * <dt><code>android:height</code></dt>
 * <dd>Used to define the intrinsic height the drawable.
 * This support all the dimension units, normally specified with dp.</dd>
 * <dt><code>android:viewportWidth</code></dt>
 * <dd>Used to define the width of the viewport space. Viewport is basically
 * the virtual canvas where the paths are drawn on.</dd>
 * <dt><code>android:viewportHeight</code></dt>
 * <dd>Used to define the height of the viewport space. Viewport is basically
 * the virtual canvas where the paths are drawn on.</dd>
 * <dt><code>android:tint</code></dt>
 * <dd>The color to apply to the drawable as a tint. By default, no tint is applied.</dd>
 * <dt><code>android:tintMode</code></dt>
 * <dd>The Porter-Duff blending mode for the tint color. Default is src_in.</dd>
 * <dt><code>android:autoMirrored</code></dt>
 * <dd>Indicates if the drawable needs to be mirrored when its layout direction is
 * RTL (right-to-left). Default is false.</dd>
 * <dt><code>android:alpha</code></dt>
 * <dd>The opacity of this drawable. Default is 1.</dd>
 * </dl></dd>
 * </dl>
 *
 * <dl>
 * <dt><code>&lt;group></code></dt>
 * <dd>Defines a group of paths or subgroups, plus transformation information.
 * The transformations are defined in the same coordinates as the viewport.
 * And the transformations are applied in the order of scale, rotate then translate.
 * <dl>
 * <dt><code>android:name</code></dt>
 * <dd>Defines the name of the group.</dd>
 * <dt><code>android:rotation</code></dt>
 * <dd>The degrees of rotation of the group. Default is 0.</dd>
 * <dt><code>android:pivotX</code></dt>
 * <dd>The X coordinate of the pivot for the scale and rotation of the group.
 * This is defined in the viewport space. Default is 0.</dd>
 * <dt><code>android:pivotY</code></dt>
 * <dd>The Y coordinate of the pivot for the scale and rotation of the group.
 * This is defined in the viewport space. Default is 0.</dd>
 * <dt><code>android:scaleX</code></dt>
 * <dd>The amount of scale on the X Coordinate. Default is 1.</dd>
 * <dt><code>android:scaleY</code></dt>
 * <dd>The amount of scale on the Y coordinate. Default is 1.</dd>
 * <dt><code>android:translateX</code></dt>
 * <dd>The amount of translation on the X coordinate.
 * This is defined in the viewport space. Default is 0.</dd>
 * <dt><code>android:translateY</code></dt>
 * <dd>The amount of translation on the Y coordinate.
 * This is defined in the viewport space. Default is 0.</dd>
 * </dl></dd>
 * </dl>
 *
 * <dl>
 * <dt><code>&lt;path></code></dt>
 * <dd>Defines paths to be drawn.
 * <dl>
 * <dt><code>android:name</code></dt>
 * <dd>Defines the name of the path.</dd>
 * <dt><code>android:pathData</code></dt>
 * <dd>Defines path data using exactly same format as "d" attribute
 * in the SVG's path data. This is defined in the viewport space.</dd>
 * <dt><code>android:fillColor</code></dt>
 * <dd>Specifies the color used to fill the path.
 * If this property is animated, any value set by the animation will override the original value.
 * No path fill is drawn if this property is not specified.</dd>
 * <dt><code>android:strokeColor</code></dt>
 * <dd>Specifies the color used to draw the path outline.
 * If this property is animated, any value set by the animation will override the original value.
 * No path outline is drawn if this property is not specified.</dd>
 * <dt><code>android:strokeWidth</code></dt>
 * <dd>The width a path stroke. Default is 0.</dd>
 * <dt><code>android:strokeAlpha</code></dt>
 * <dd>The opacity of a path stroke. Default is 1.</dd>
 * <dt><code>android:fillAlpha</code></dt>
 * <dd>The opacity to fill the path with. Default is 1.</dd>
 * <dt><code>android:trimPathStart</code></dt>
 * <dd>The fraction of the path to trim from the start, in the range from 0 to 1. Default is 0.</dd>
 * <dt><code>android:trimPathEnd</code></dt>
 * <dd>The fraction of the path to trim from the end, in the range from 0 to 1. Default is 1.</dd>
 * <dt><code>android:trimPathOffset</code></dt>
 * <dd>Shift trim region (allows showed region to include the start and end), in the range
 * from 0 to 1. Default is 0.</dd>
 * <dt><code>android:strokeLineCap</code></dt>
 * <dd>Sets the linecap for a stroked path: butt, round, square. Default is butt.</dd>
 * <dt><code>android:strokeLineJoin</code></dt>
 * <dd>Sets the lineJoin for a stroked path: miter,round,bevel. Default is miter.</dd>
 * <dt><code>android:strokeMiterLimit</code></dt>
 * <dd>Sets the Miter limit for a stroked path. Default is 4.</dd>
 * <dt><code>android:fillType</code></dt>
 * <dd>Sets the fillType for a path. The types can be either "evenOdd" or "nonZero". They behave the
 * same as SVG's "fill-rule" properties. Default is nonZero. For more details, see
 * <a href="https://www.w3.org/TR/SVG/painting.html#FillRuleProperty">FillRuleProperty</a></dd>
 * </dl></dd>
 * </dl>
 *
 * <dl>
 * <dt><code>&lt;clip-path></code></dt>
 * <dd>Defines path to be the current clip. Note that the clip path only apply to
 * the current group and its children.
 * <dl>
 * <dt><code>android:name</code></dt>
 * <dd>Defines the name of the clip path.</dd>
 * <dt><code>android:pathData</code></dt>
 * <dd>Defines clip path using the same format as "d" attribute
 * in the SVG's path data.</dd>
 * </dl></dd>
 * </dl>
 * <p/>
 *
 * <h4>Gradient support</h4>
 * We support 3 types of gradients: {@link android.graphics.LinearGradient},
 * {@link android.graphics.RadialGradient}, or {@link android.graphics.SweepGradient}.
 * <p/>
 * And we support all of 3 types of tile modes {@link android.graphics.Shader.TileMode}:
 * CLAMP, REPEAT, MIRROR.
 * <p/>
 * Note that different attributes are relevant for different types of gradient.
 * <table border="2" align="center" cellpadding="5">
 *     <thead>
 *         <tr>
 *             <th>LinearGradient</th>
 *             <th>RadialGradient</th>
 *             <th>SweepGradient</th>
 *         </tr>
 *     </thead>
 *     <tr>
 *         <td>startColor</td>
 *         <td>startColor</td>
 *         <td>startColor</td>
 *     </tr>
 *     <tr>
 *         <td>centerColor</td>
 *         <td>centerColor</td>
 *         <td>centerColor</td>
 *     </tr>
 *     <tr>
 *         <td>endColor</td>
 *         <td>endColor</td>
 *         <td>endColor</td>
 *     </tr>
 *     <tr>
 *         <td>type</td>
 *         <td>type</td>
 *         <td>type</td>
 *     </tr>
 *     <tr>
 *         <td>tileMode</td>
 *         <td>tileMode</td>
 *         <td>tileMode</td>
 *     </tr>
 *     <tr>
 *         <td>startX</td>
 *         <td>centerX</td>
 *         <td>centerX</td>
 *     </tr>
 *     <tr>
 *         <td>startY</td>
 *         <td>centerY</td>
 *         <td>centerY</td>
 *     </tr>
 *     <tr>
 *         <td>endX</td>
 *         <td>gradientRadius</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>endY</td>
 *         <td></td>
 *         <td></td>
 *     </tr>
 * </table>
 * <p/>
 * Also note that if any color item is defined, then
 * startColor, centerColor and endColor will be ignored.
 * <p/>
 * Note that theme attributes in XML file are supported through
 * <code>{@link #inflate(Resources, XmlPullParser, AttributeSet, Theme)}</code>.
 */
public class VectorDrawableCompat extends VectorDrawableCommon {
    static final String LOGTAG = "VectorDrawableCompat";

    static final PorterDuff.Mode DEFAULT_TINT_MODE = PorterDuff.Mode.SRC_IN;

    private static final String SHAPE_CLIP_PATH = "clip-path";
    private static final String SHAPE_GROUP = "group";
    private static final String SHAPE_PATH = "path";

    private static final int LINECAP_BUTT = 0;
    private static final int LINECAP_ROUND = 1;
    private static final int LINECAP_SQUARE = 2;

    private static final int LINEJOIN_MITER = 0;
    private static final int LINEJOIN_ROUND = 1;
    private static final int LINEJOIN_BEVEL = 2;

    // Cap the bitmap size, such that it won't hurt the performance too much
    // and it won't crash due to a very large scale.
    // The drawable will look blurry above this size.
    private static final int MAX_CACHED_BITMAP_SIZE = 2048;

    private static final boolean DBG_VECTOR_DRAWABLE = false;

    private VectorDrawableCompatState mVectorState;

    private PorterDuffColorFilter mTintFilter;
    private ColorFilter mColorFilter;

    private boolean mMutated;

    // AnimatedVectorDrawable needs to turn off the cache all the time, otherwise,
    // caching the bitmap by default is allowed.
    private boolean mAllowCaching = true;

    // Temp variable, only for saving "new" operation at the draw() time.
    private final float[] mTmpFloats = new float[9];
    private final Matrix mTmpMatrix = new Matrix();
    private final Rect mTmpBounds = new Rect();

    VectorDrawableCompat() {
        mVectorState = new VectorDrawableCompatState();
    }

    VectorDrawableCompat(@NonNull VectorDrawableCompatState state) {
        mVectorState = state;
        mTintFilter = updateTintFilter(mTintFilter, state.mTint, state.mTintMode);
    }

    @NonNull
    @Override
    public Drawable mutate() {
        if (mDelegateDrawable != null) {
            mDelegateDrawable.mutate();
            return this;
        }

        if (!mMutated && super.mutate() == this) {
            mVectorState = new VectorDrawableCompatState(mVectorState);
            mMutated = true;
        }
        return this;
    }

    Object getTargetByName(String name) {
        return mVectorState.mVPathRenderer.mVGTargetsMap.get(name);
    }

    @NonNull
    @Override
    public ConstantState getConstantState() {
        if (mDelegateDrawable != null && Build.VERSION.SDK_INT >= 24) {
            // Such that the configuration can be refreshed.
            return new VectorDrawableDelegateState(mDelegateDrawable.getConstantState());
        }
        mVectorState.mChangingConfigurations = getChangingConfigurations();
        return mVectorState;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mDelegateDrawable != null) {
            mDelegateDrawable.draw(canvas);
            return;
        }
        // We will offset the bounds for drawBitmap, so copyBounds() here instead
        // of getBounds().
        copyBounds(mTmpBounds);
        if (mTmpBounds.width() <= 0 || mTmpBounds.height() <= 0) {
            // Nothing to draw
            return;
        }

        // Color filters always override tint filters.
        final ColorFilter colorFilter = (mColorFilter == null ? mTintFilter : mColorFilter);

        // The imageView can scale the canvas in different ways, in order to
        // avoid blurry scaling, we have to draw into a bitmap with exact pixel
        // size first. This bitmap size is determined by the bounds and the
        // canvas scale.
        canvas.getMatrix(mTmpMatrix);
        mTmpMatrix.getValues(mTmpFloats);
        float canvasScaleX = Math.abs(mTmpFloats[Matrix.MSCALE_X]);
        float canvasScaleY = Math.abs(mTmpFloats[Matrix.MSCALE_Y]);

        float canvasSkewX = Math.abs(mTmpFloats[Matrix.MSKEW_X]);
        float canvasSkewY = Math.abs(mTmpFloats[Matrix.MSKEW_Y]);

        // When there is any rotation / skew, then the scale value is not valid.
        if (canvasSkewX != 0 || canvasSkewY != 0) {
            canvasScaleX = 1.0f;
            canvasScaleY = 1.0f;
        }

        int scaledWidth = (int) (mTmpBounds.width() * canvasScaleX);
        int scaledHeight = (int) (mTmpBounds.height() * canvasScaleY);
        scaledWidth = Math.min(MAX_CACHED_BITMAP_SIZE, scaledWidth);
        scaledHeight = Math.min(MAX_CACHED_BITMAP_SIZE, scaledHeight);

        if (scaledWidth <= 0 || scaledHeight <= 0) {
            return;
        }

        final int saveCount = canvas.save();
        canvas.translate(mTmpBounds.left, mTmpBounds.top);

        // Handle RTL mirroring.
        final boolean needMirroring = needMirroring();
        if (needMirroring) {
            canvas.translate(mTmpBounds.width(), 0);
            canvas.scale(-1.0f, 1.0f);
        }

        // At this point, canvas has been translated to the right position.
        // And we use this bound for the destination rect for the drawBitmap, so
        // we offset to (0, 0);
        mTmpBounds.offsetTo(0, 0);

        mVectorState.createCachedBitmapIfNeeded(scaledWidth, scaledHeight);
        if (!mAllowCaching) {
            mVectorState.updateCachedBitmap(scaledWidth, scaledHeight);
        } else {
            if (!mVectorState.canReuseCache()) {
                mVectorState.updateCachedBitmap(scaledWidth, scaledHeight);
                mVectorState.updateCacheStates();
            }
        }
        mVectorState.drawCachedBitmapWithRootAlpha(canvas, colorFilter, mTmpBounds);
        canvas.restoreToCount(saveCount);
    }

    @Override
    public int getAlpha() {
        if (mDelegateDrawable != null) {
            return DrawableCompat.getAlpha(mDelegateDrawable);
        }

        return mVectorState.mVPathRenderer.getRootAlpha();
    }

    @Override
    public void setAlpha(int alpha) {
        if (mDelegateDrawable != null) {
            mDelegateDrawable.setAlpha(alpha);
            return;
        }

        if (mVectorState.mVPathRenderer.getRootAlpha() != alpha) {
            mVectorState.mVPathRenderer.setRootAlpha(alpha);
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        if (mDelegateDrawable != null) {
            mDelegateDrawable.setColorFilter(colorFilter);
            return;
        }

        mColorFilter = colorFilter;
        invalidateSelf();
    }

    @Nullable
    @Override
    public ColorFilter getColorFilter() {
        if (mDelegateDrawable != null) {
            return DrawableCompat.getColorFilter(mDelegateDrawable);
        }
        return mColorFilter;
    }

    /**
     * Ensures the tint filter is consistent with the current tint color and
     * mode.
     */
    @SuppressWarnings("unused")
    PorterDuffColorFilter updateTintFilter(PorterDuffColorFilter tintFilter, ColorStateList tint,
                                           PorterDuff.Mode tintMode) {
        if (tint == null || tintMode == null) {
            return null;
        }
        // setMode, setColor of PorterDuffColorFilter are not public method in SDK v7.
        // Therefore we create a new one all the time here. Don't expect this is called often.
        final int color = tint.getColorForState(getState(), TRANSPARENT);
        return new PorterDuffColorFilter(color, tintMode);
    }

    @Override
    public void setTint(int tint) {
        if (mDelegateDrawable != null) {
            DrawableCompat.setTint(mDelegateDrawable, tint);
            return;
        }

        setTintList(ColorStateList.valueOf(tint));
    }

    @Override
    public void setTintList(@Nullable ColorStateList tint) {
        if (mDelegateDrawable != null) {
            DrawableCompat.setTintList(mDelegateDrawable, tint);
            return;
        }

        final VectorDrawableCompatState state = mVectorState;
        if (state.mTint != tint) {
            state.mTint = tint;
            mTintFilter = updateTintFilter(mTintFilter, tint, state.mTintMode);
            invalidateSelf();
        }
    }

    @Override
    public void setTintMode(@Nullable Mode tintMode) {
        if (mDelegateDrawable != null) {
            DrawableCompat.setTintMode(mDelegateDrawable, tintMode);
            return;
        }

        final VectorDrawableCompatState state = mVectorState;
        if (state.mTintMode != tintMode) {
            state.mTintMode = tintMode;
            mTintFilter = updateTintFilter(mTintFilter, state.mTint, tintMode);
            invalidateSelf();
        }
    }

    @Override
    public boolean isStateful() {
        if (mDelegateDrawable != null) {
            return mDelegateDrawable.isStateful();
        }

        return super.isStateful() || (mVectorState != null
                && (mVectorState.isStateful()
                || (mVectorState.mTint != null && mVectorState.mTint.isStateful())));
    }

    @Override
    protected boolean onStateChange(int[] stateSet) {
        if (mDelegateDrawable != null) {
            return mDelegateDrawable.setState(stateSet);
        }

        boolean changed = false;
        final VectorDrawableCompatState state = mVectorState;
        if (state.mTint != null && state.mTintMode != null) {
            mTintFilter = updateTintFilter(mTintFilter, state.mTint, state.mTintMode);
            invalidateSelf();
            changed = true;
        }
        if (state.isStateful() && state.onStateChanged(stateSet)) {
            invalidateSelf();
            changed = true;
        }
        return changed;
    }

    @Override
    public int getOpacity() {
        if (mDelegateDrawable != null) {
            return mDelegateDrawable.getOpacity();
        }

        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        if (mDelegateDrawable != null) {
            return mDelegateDrawable.getIntrinsicWidth();
        }

        return (int) mVectorState.mVPathRenderer.mBaseWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        if (mDelegateDrawable != null) {
            return mDelegateDrawable.getIntrinsicHeight();
        }

        return (int) mVectorState.mVPathRenderer.mBaseHeight;
    }

    // Don't support re-applying themes. The initial theme loading is working.
    @Override
    public boolean canApplyTheme() {
        if (mDelegateDrawable != null) {
            DrawableCompat.canApplyTheme(mDelegateDrawable);
        }

        return false;
    }

    @Override
    public boolean isAutoMirrored() {
        if (mDelegateDrawable != null) {
            return DrawableCompat.isAutoMirrored(mDelegateDrawable);
        }
        return mVectorState.mAutoMirrored;
    }

    @Override
    public void setAutoMirrored(boolean mirrored) {
        if (mDelegateDrawable != null) {
            DrawableCompat.setAutoMirrored(mDelegateDrawable, mirrored);
            return;
        }
        mVectorState.mAutoMirrored = mirrored;
    }
    /**
     * The size of a pixel when scaled from the intrinsic dimension to the viewport dimension. This
     * is used to calculate the path animation accuracy.
     *
     */
    @SuppressWarnings("unused")
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public float getPixelSize() {
        if (mVectorState == null || mVectorState.mVPathRenderer == null
                || mVectorState.mVPathRenderer.mBaseWidth == 0
                || mVectorState.mVPathRenderer.mBaseHeight == 0
                || mVectorState.mVPathRenderer.mViewportHeight == 0
                || mVectorState.mVPathRenderer.mViewportWidth == 0) {
            return 1; // fall back to 1:1 pixel mapping.
        }
        float intrinsicWidth = mVectorState.mVPathRenderer.mBaseWidth;
        float intrinsicHeight = mVectorState.mVPathRenderer.mBaseHeight;
        float viewportWidth = mVectorState.mVPathRenderer.mViewportWidth;
        float viewportHeight = mVectorState.mVPathRenderer.mViewportHeight;
        float scaleX = viewportWidth / intrinsicWidth;
        float scaleY = viewportHeight / intrinsicHeight;
        return Math.min(scaleX, scaleY);
    }

    /**
     * Create a VectorDrawableCompat object.
     *
     * @param res   the resources.
     * @param resId the resource ID for VectorDrawableCompat object.
     * @param theme the theme of this vector drawable, it can be null.
     * @return a new VectorDrawableCompat or null if parsing error is found.
     */
    @Nullable
    public static VectorDrawableCompat create(@NonNull Resources res, @DrawableRes int resId,
                                              @Nullable Theme theme) {
        if (Build.VERSION.SDK_INT >= 24) {
            final VectorDrawableCompat drawable = new VectorDrawableCompat();
            drawable.mDelegateDrawable = ResourcesCompat.getDrawable(res, resId, theme);
            return drawable;
        }
        return createWithoutDelegate(res, resId, theme);
    }

    static VectorDrawableCompat createWithoutDelegate(
            @NonNull Resources res,
            @DrawableRes int resId,
            @Nullable Theme theme
    ) {
        try {
            @SuppressLint("ResourceType") final XmlPullParser parser = res.getXml(resId);
            final AttributeSet attrs = Xml.asAttributeSet(parser);
            int type;
            //noinspection StatementWithEmptyBody
            while ((type = parser.next()) != XmlPullParser.START_TAG
                    && type != XmlPullParser.END_DOCUMENT) {
                // Empty loop
            }
            if (type != XmlPullParser.START_TAG) {
                throw new XmlPullParserException("No start tag found");
            }
            return createFromXmlInner(res, parser, attrs, theme);
        } catch (XmlPullParserException e) {
            Log.e(LOGTAG, "parser error", e);
        } catch (IOException e) {
            Log.e(LOGTAG, "parser error", e);
        }
        return null;
    }

    /**
     * Create a VectorDrawableCompat from inside an XML document using an optional
     * {@link Theme}. Called on a parser positioned at a tag in an XML
     * document, tries to create a Drawable from that tag. Returns {@code null}
     * if the tag is not a valid drawable.
     */
    @NonNull
    public static VectorDrawableCompat createFromXmlInner(@NonNull Resources r,
            @NonNull XmlPullParser parser, @NonNull AttributeSet attrs, @Nullable Theme theme)
            throws XmlPullParserException, IOException {
        final VectorDrawableCompat drawable = new VectorDrawableCompat();
        drawable.inflate(r, parser, attrs, theme);
        return drawable;
    }

    static int applyAlpha(int color, float alpha) {
        int alphaBytes = alpha(color);
        color &= 0x00FFFFFF;
        color |= ((int) (alphaBytes * alpha)) << 24;
        return color;
    }

    @Override
    public void inflate(@NonNull Resources res, @NonNull XmlPullParser parser,
            @NonNull AttributeSet attrs) throws XmlPullParserException, IOException {
        if (mDelegateDrawable != null) {
            mDelegateDrawable.inflate(res, parser, attrs);
            return;
        }

        inflate(res, parser, attrs, null);
    }

    @Override
    public void inflate(@NonNull Resources res, @NonNull XmlPullParser parser,
            @NonNull AttributeSet attrs, @Nullable Theme theme)
            throws XmlPullParserException, IOException {
        if (mDelegateDrawable != null) {
            DrawableCompat.inflate(mDelegateDrawable, res, parser, attrs, theme);
            return;
        }

        final VectorDrawableCompatState state = mVectorState;
        state.mVPathRenderer = new VPathRenderer();

        final TypedArray a = TypedArrayUtils.obtainAttributes(res, theme, attrs,
                AndroidResources.STYLEABLE_VECTOR_DRAWABLE_TYPE_ARRAY);

        updateStateFromTypedArray(a, parser, theme);
        a.recycle();
        state.mChangingConfigurations = getChangingConfigurations();
        state.mCacheDirty = true;
        inflateInternal(res, parser, attrs, theme);

        mTintFilter = updateTintFilter(mTintFilter, state.mTint, state.mTintMode);
    }


    /**
     * Parses a {@link android.graphics.PorterDuff.Mode} from a tintMode
     * attribute's enum value.
     */
    @SuppressWarnings("SameParameterValue")
    private static PorterDuff.Mode parseTintModeCompat(int value, Mode defaultMode) {
        switch (value) {
            case 3:
                return Mode.SRC_OVER;
            case 5:
                return Mode.SRC_IN;
            case 9:
                return Mode.SRC_ATOP;
            case 14:
                return Mode.MULTIPLY;
            case 15:
                return Mode.SCREEN;
            case 16:
                return Mode.ADD;
            default:
                return defaultMode;
        }
    }

    private void updateStateFromTypedArray(TypedArray a, XmlPullParser parser, Theme theme)
            throws XmlPullParserException {
        final VectorDrawableCompatState state = mVectorState;
        final VPathRenderer pathRenderer = state.mVPathRenderer;

        // Account for any configuration changes.
        // state.mChangingConfigurations |= Utils.getChangingConfigurations(a);

        final int mode = TypedArrayUtils.getNamedInt(a, parser, "tintMode",
                AndroidResources.STYLEABLE_VECTOR_DRAWABLE_TINT_MODE, -1);
        state.mTintMode = parseTintModeCompat(mode, Mode.SRC_IN);

        final ColorStateList tint =
                TypedArrayUtils.getNamedColorStateList(a, parser, theme, "tint",
                        AndroidResources.STYLEABLE_VECTOR_DRAWABLE_TINT);
        if (tint != null) {
            state.mTint = tint;
        }

        state.mAutoMirrored = TypedArrayUtils.getNamedBoolean(a, parser, "autoMirrored",
                AndroidResources.STYLEABLE_VECTOR_DRAWABLE_AUTO_MIRRORED, state.mAutoMirrored);

        pathRenderer.mViewportWidth = TypedArrayUtils.getNamedFloat(a, parser, "viewportWidth",
                AndroidResources.STYLEABLE_VECTOR_DRAWABLE_VIEWPORT_WIDTH,
                pathRenderer.mViewportWidth);

        pathRenderer.mViewportHeight = TypedArrayUtils.getNamedFloat(a, parser, "viewportHeight",
                AndroidResources.STYLEABLE_VECTOR_DRAWABLE_VIEWPORT_HEIGHT,
                pathRenderer.mViewportHeight);

        if (pathRenderer.mViewportWidth <= 0) {
            throw new XmlPullParserException(a.getPositionDescription()
                    + "<vector> tag requires viewportWidth > 0");
        } else if (pathRenderer.mViewportHeight <= 0) {
            throw new XmlPullParserException(a.getPositionDescription()
                    + "<vector> tag requires viewportHeight > 0");
        }

        pathRenderer.mBaseWidth = a.getDimension(
                AndroidResources.STYLEABLE_VECTOR_DRAWABLE_WIDTH, pathRenderer.mBaseWidth);
        pathRenderer.mBaseHeight = a.getDimension(
                AndroidResources.STYLEABLE_VECTOR_DRAWABLE_HEIGHT, pathRenderer.mBaseHeight);
        if (pathRenderer.mBaseWidth <= 0) {
            throw new XmlPullParserException(a.getPositionDescription()
                    + "<vector> tag requires width > 0");
        } else if (pathRenderer.mBaseHeight <= 0) {
            throw new XmlPullParserException(a.getPositionDescription()
                    + "<vector> tag requires height > 0");
        }

        // shown up from API 11.
        final float alphaInFloat = TypedArrayUtils.getNamedFloat(a, parser, "alpha",
                AndroidResources.STYLEABLE_VECTOR_DRAWABLE_ALPHA, pathRenderer.getAlpha());
        pathRenderer.setAlpha(alphaInFloat);

        final String name = a.getString(AndroidResources.STYLEABLE_VECTOR_DRAWABLE_NAME);
        if (name != null) {
            pathRenderer.mRootName = name;
            pathRenderer.mVGTargetsMap.put(name, pathRenderer);
        }
    }

    private void inflateInternal(Resources res, XmlPullParser parser, AttributeSet attrs,
                                 Theme theme) throws XmlPullParserException, IOException {
        final VectorDrawableCompatState state = mVectorState;
        final VPathRenderer pathRenderer = state.mVPathRenderer;
        boolean noPathTag = true;

        // Use a stack to help to build the group tree.
        // The top of the stack is always the current group.
        final ArrayDeque<VGroup> groupStack = new ArrayDeque<>();
        groupStack.push(pathRenderer.mRootGroup);

        int eventType = parser.getEventType();
        final int innerDepth = parser.getDepth() + 1;

        // Parse everything until the end of the vector element.
        while (eventType != XmlPullParser.END_DOCUMENT
                && (parser.getDepth() >= innerDepth || eventType != XmlPullParser.END_TAG)) {
            if (eventType == XmlPullParser.START_TAG) {
                final String tagName = parser.getName();
                final VGroup currentGroup = groupStack.peek();
                if (currentGroup != null) {
                    if (SHAPE_PATH.equals(tagName)) {
                        final VFullPath path = new VFullPath();
                        path.inflate(res, attrs, theme, parser);
                        currentGroup.mChildren.add(path);
                        if (path.getPathName() != null) {
                            pathRenderer.mVGTargetsMap.put(path.getPathName(), path);
                        }
                        noPathTag = false;
                        state.mChangingConfigurations |= path.mChangingConfigurations;
                    } else if (SHAPE_CLIP_PATH.equals(tagName)) {
                        final VClipPath path = new VClipPath();
                        path.inflate(res, attrs, theme, parser);
                        currentGroup.mChildren.add(path);
                        if (path.getPathName() != null) {
                            pathRenderer.mVGTargetsMap.put(path.getPathName(), path);
                        }
                        state.mChangingConfigurations |= path.mChangingConfigurations;
                    } else if (SHAPE_GROUP.equals(tagName)) {
                        VGroup newChildGroup = new VGroup();
                        newChildGroup.inflate(res, attrs, theme, parser);
                        currentGroup.mChildren.add(newChildGroup);
                        groupStack.push(newChildGroup);
                        if (newChildGroup.getGroupName() != null) {
                            pathRenderer.mVGTargetsMap.put(newChildGroup.getGroupName(),
                                    newChildGroup);
                        }
                        state.mChangingConfigurations |= newChildGroup.mChangingConfigurations;
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                final String tagName = parser.getName();
                if (SHAPE_GROUP.equals(tagName)) {
                    groupStack.pop();
                }
            }
            eventType = parser.next();
        }

        // Print the tree out for debug.
        if (DBG_VECTOR_DRAWABLE) {
            printGroupTree(pathRenderer.mRootGroup, 0);
        }

        if (noPathTag) {
            throw new XmlPullParserException("no " + SHAPE_PATH + " defined");
        }
    }

    private void printGroupTree(VGroup currentGroup, int level) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < level; i++) {
            indent.append("    ");
        }
        // Print the current node
        Log.v(LOGTAG, indent + "current group is :" + currentGroup.getGroupName()
                + " rotation is " + currentGroup.mRotate);
        Log.v(LOGTAG, indent + "matrix is :" + currentGroup.getLocalMatrix().toString());
        // Then print all the children groups
        for (int i = 0; i < currentGroup.mChildren.size(); i++) {
            VObject child = currentGroup.mChildren.get(i);
            if (child instanceof VGroup) {
                printGroupTree((VGroup) child, level + 1);
            } else {
                ((VPath) child).printVPath(level + 1);
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    void setAllowCaching(boolean allowCaching) {
        mAllowCaching = allowCaching;
    }

    // We don't support RTL auto mirroring since the getLayoutDirection() is for API 17+.
    private boolean needMirroring() {
        return isAutoMirrored()
                && DrawableCompat.getLayoutDirection(this) == View.LAYOUT_DIRECTION_RTL;
    }

    // Extra override functions for delegation for SDK >= 7.
    @Override
    protected void onBoundsChange(Rect bounds) {
        if (mDelegateDrawable != null) {
            mDelegateDrawable.setBounds(bounds);
        }
    }

    @Override
    public int getChangingConfigurations() {
        if (mDelegateDrawable != null) {
            return mDelegateDrawable.getChangingConfigurations();
        }
        return super.getChangingConfigurations() | mVectorState.getChangingConfigurations();
    }

    @Override
    public void invalidateSelf() {
        if (mDelegateDrawable != null) {
            mDelegateDrawable.invalidateSelf();
            return;
        }
        super.invalidateSelf();
    }

    @Override
    public void scheduleSelf(@NonNull Runnable what, long when) {
        if (mDelegateDrawable != null) {
            mDelegateDrawable.scheduleSelf(what, when);
            return;
        }
        super.scheduleSelf(what, when);
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        if (mDelegateDrawable != null) {
            return mDelegateDrawable.setVisible(visible, restart);
        }
        return super.setVisible(visible, restart);
    }

    @Override
    public void unscheduleSelf(@NonNull Runnable what) {
        if (mDelegateDrawable != null) {
            mDelegateDrawable.unscheduleSelf(what);
            return;
        }
        super.unscheduleSelf(what);
    }

    /**
     * Constant state for delegating the creating drawable job for SDK >= 24.
     * Instead of creating a VectorDrawable, create a VectorDrawableCompat instance which contains
     * a delegated VectorDrawable instance.
     */
    @RequiresApi(24)
    private static class VectorDrawableDelegateState extends ConstantState {
        private final ConstantState mDelegateState;

        VectorDrawableDelegateState(ConstantState state) {
            mDelegateState = state;
        }

        @Override
        public Drawable newDrawable() {
            VectorDrawableCompat drawableCompat = new VectorDrawableCompat();
            drawableCompat.mDelegateDrawable = (VectorDrawable) mDelegateState.newDrawable();
            return drawableCompat;
        }

        @Override
        public Drawable newDrawable(Resources res) {
            VectorDrawableCompat drawableCompat = new VectorDrawableCompat();
            drawableCompat.mDelegateDrawable = (VectorDrawable) mDelegateState.newDrawable(res);
            return drawableCompat;
        }

        @Override
        public Drawable newDrawable(Resources res, Theme theme) {
            VectorDrawableCompat drawableCompat = new VectorDrawableCompat();
            drawableCompat.mDelegateDrawable =
                    (VectorDrawable) mDelegateState.newDrawable(res, theme);
            return drawableCompat;
        }

        @Override
        public boolean canApplyTheme() {
            return mDelegateState.canApplyTheme();
        }

        @Override
        public int getChangingConfigurations() {
            return mDelegateState.getChangingConfigurations();
        }
    }

    private static class VectorDrawableCompatState extends ConstantState {
        int mChangingConfigurations;
        VPathRenderer mVPathRenderer;
        ColorStateList mTint = null;
        Mode mTintMode = DEFAULT_TINT_MODE;
        boolean mAutoMirrored;

        // Cached fields, don't copy on mutate.
        Bitmap mCachedBitmap;
        @SuppressWarnings("unused")
        int[] mCachedThemeAttrs;
        ColorStateList mCachedTint;
        Mode mCachedTintMode;
        int mCachedRootAlpha;
        boolean mCachedAutoMirrored;
        boolean mCacheDirty;

        /**
         * Temporary paint object used to draw cached bitmaps.
         */
        Paint mTempPaint;

        // Deep copy for mutate() or implicitly mutate.
        @SuppressWarnings("CopyConstructorMissesField") // Intentional, see field comments.
        VectorDrawableCompatState(VectorDrawableCompatState copy) {
            if (copy != null) {
                mChangingConfigurations = copy.mChangingConfigurations;
                mVPathRenderer = new VPathRenderer(copy.mVPathRenderer);
                if (copy.mVPathRenderer.mFillPaint != null) {
                    mVPathRenderer.mFillPaint = new Paint(copy.mVPathRenderer.mFillPaint);
                }
                if (copy.mVPathRenderer.mStrokePaint != null) {
                    mVPathRenderer.mStrokePaint = new Paint(copy.mVPathRenderer.mStrokePaint);
                }
                mTint = copy.mTint;
                mTintMode = copy.mTintMode;
                mAutoMirrored = copy.mAutoMirrored;
            }
        }

        public void drawCachedBitmapWithRootAlpha(Canvas canvas, ColorFilter filter,
                                                  Rect originalBounds) {
            // The bitmap's size is the same as the bounds.
            final Paint p = getPaint(filter);
            canvas.drawBitmap(mCachedBitmap, null, originalBounds, p);
        }

        public boolean hasTranslucentRoot() {
            return mVPathRenderer.getRootAlpha() < 255;
        }

        /**
         * @return null when there is no need for alpha paint.
         */
        public Paint getPaint(ColorFilter filter) {
            if (!hasTranslucentRoot() && filter == null) {
                return null;
            }

            if (mTempPaint == null) {
                mTempPaint = new Paint();
                mTempPaint.setFilterBitmap(true);
            }
            mTempPaint.setAlpha(mVPathRenderer.getRootAlpha());
            mTempPaint.setColorFilter(filter);
            return mTempPaint;
        }

        public void updateCachedBitmap(int width, int height) {
            mCachedBitmap.eraseColor(TRANSPARENT);
            Canvas tmpCanvas = new Canvas(mCachedBitmap);
            mVPathRenderer.draw(tmpCanvas, width, height, null);
        }

        public void createCachedBitmapIfNeeded(int width, int height) {
            if (mCachedBitmap == null || !canReuseBitmap(width, height)) {
                mCachedBitmap = Bitmap.createBitmap(width, height,
                        Bitmap.Config.ARGB_8888);
                mCacheDirty = true;
            }

        }

        public boolean canReuseBitmap(int width, int height) {
            return width == mCachedBitmap.getWidth()
                    && height == mCachedBitmap.getHeight();
        }

        public boolean canReuseCache() {
            return !mCacheDirty
                    && mCachedTint == mTint
                    && mCachedTintMode == mTintMode
                    && mCachedAutoMirrored == mAutoMirrored
                    && mCachedRootAlpha == mVPathRenderer.getRootAlpha();
        }

        public void updateCacheStates() {
            // Use shallow copy here and shallow comparison in canReuseCache(),
            // likely hit cache miss more, but practically not much difference.
            mCachedTint = mTint;
            mCachedTintMode = mTintMode;
            mCachedRootAlpha = mVPathRenderer.getRootAlpha();
            mCachedAutoMirrored = mAutoMirrored;
            mCacheDirty = false;
        }

        VectorDrawableCompatState() {
            mVPathRenderer = new VPathRenderer();
        }

        @NonNull
        @Override
        public Drawable newDrawable() {
            return new VectorDrawableCompat(this);
        }

        @NonNull
        @Override
        public Drawable newDrawable(Resources res) {
            return new VectorDrawableCompat(this);
        }

        @Override
        public int getChangingConfigurations() {
            return mChangingConfigurations;
        }

        public boolean isStateful() {
            return mVPathRenderer.isStateful();
        }

        public boolean onStateChanged(int[] stateSet) {
            final boolean changed = mVPathRenderer.onStateChanged(stateSet);
            mCacheDirty |= changed;
            return changed;
        }
    }

    private static class VPathRenderer {
        /* Right now the internal data structure is organized as a tree.
         * Each node can be a group node, or a path.
         * A group node can have groups or paths as children, but a path node has
         * no children.
         * One example can be:
         *                 Root Group
         *                /    |     \
         *           Group    Path    Group
         *          /     \             |
         *         Path   Path         Path
         *
         */
        // Variables that only used temporarily inside the draw() call, so there
        // is no need for deep copying.
        private final Path mPath;
        private final Path mRenderPath;
        private static final Matrix IDENTITY_MATRIX = new Matrix();
        private final Matrix mFinalPathMatrix = new Matrix();

        Paint mStrokePaint;
        Paint mFillPaint;
        private PathMeasure mPathMeasure;

        /////////////////////////////////////////////////////
        // Variables below need to be copied (deep copy if applicable) for mutation.
        private int mChangingConfigurations;
        final VGroup mRootGroup;
        float mBaseWidth = 0;
        float mBaseHeight = 0;
        float mViewportWidth = 0;
        float mViewportHeight = 0;
        int mRootAlpha = 0xFF;
        String mRootName = null;
        Boolean mIsStateful = null;

        final ArrayMap<String, Object> mVGTargetsMap = new ArrayMap<>();

        VPathRenderer() {
            mRootGroup = new VGroup();
            mPath = new Path();
            mRenderPath = new Path();
        }

        public void setRootAlpha(int alpha) {
            mRootAlpha = alpha;
        }

        public int getRootAlpha() {
            return mRootAlpha;
        }

        // setAlpha() and getAlpha() are used mostly for animation purpose, since
        // Animator like to use alpha from 0 to 1.
        public void setAlpha(float alpha) {
            setRootAlpha((int) (alpha * 255));
        }

        @SuppressWarnings("unused")
        public float getAlpha() {
            return getRootAlpha() / 255.0f;
        }

        @SuppressWarnings("CopyConstructorMissesField")
        VPathRenderer(VPathRenderer copy) {
            mRootGroup = new VGroup(copy.mRootGroup, mVGTargetsMap);
            mPath = new Path(copy.mPath);
            mRenderPath = new Path(copy.mRenderPath);
            mBaseWidth = copy.mBaseWidth;
            mBaseHeight = copy.mBaseHeight;
            mViewportWidth = copy.mViewportWidth;
            mViewportHeight = copy.mViewportHeight;
            mChangingConfigurations = copy.mChangingConfigurations;
            mRootAlpha = copy.mRootAlpha;
            mRootName = copy.mRootName;
            if (copy.mRootName != null) {
                mVGTargetsMap.put(copy.mRootName, this);
            }
            mIsStateful = copy.mIsStateful;
        }

        private void drawGroupTree(VGroup currentGroup, Matrix currentMatrix,
                                   Canvas canvas, int w, int h, ColorFilter filter) {
            // Calculate current group's matrix by preConcat the parent's and
            // and the current one on the top of the stack.
            // Basically the Mfinal = Mviewport * M0 * M1 * M2;
            // Mi the local matrix at level i of the group tree.
            currentGroup.mStackedMatrix.set(currentMatrix);

            currentGroup.mStackedMatrix.preConcat(currentGroup.mLocalMatrix);

            // Save the current clip information, which is local to this group.
            canvas.save();

            // Draw the group tree in the same order as the XML file.
            for (int i = 0; i < currentGroup.mChildren.size(); i++) {
                VObject child = currentGroup.mChildren.get(i);
                if (child instanceof VGroup) {
                    VGroup childGroup = (VGroup) child;
                    drawGroupTree(childGroup, currentGroup.mStackedMatrix,
                            canvas, w, h, filter);
                } else if (child instanceof VPath) {
                    VPath childPath = (VPath) child;
                    drawPath(currentGroup, childPath, canvas, w, h, filter);
                }
            }

            canvas.restore();
        }

        public void draw(Canvas canvas, int w, int h, ColorFilter filter) {
            // Traverse the tree in pre-order to draw.
            drawGroupTree(mRootGroup, IDENTITY_MATRIX, canvas, w, h, filter);
        }

        private void drawPath(VGroup vGroup, VPath vPath, Canvas canvas, int w, int h,
                              ColorFilter filter) {
            final float scaleX = w / mViewportWidth;
            final float scaleY = h / mViewportHeight;
            final float minScale = Math.min(scaleX, scaleY);
            final Matrix groupStackedMatrix = vGroup.mStackedMatrix;

            mFinalPathMatrix.set(groupStackedMatrix);
            mFinalPathMatrix.postScale(scaleX, scaleY);


            final float matrixScale = getMatrixScale(groupStackedMatrix);
            if (matrixScale == 0) {
                // When either x or y is scaled to 0, we don't need to draw anything.
                return;
            }
            vPath.toPath(mPath);
            final Path path = mPath;

            mRenderPath.reset();

            if (vPath.isClipPath()) {
                mRenderPath.setFillType(vPath.mFillRule == 0 ? Path.FillType.WINDING
                        : Path.FillType.EVEN_ODD);
                mRenderPath.addPath(path, mFinalPathMatrix);
                canvas.clipPath(mRenderPath);
            } else {
                VFullPath fullPath = (VFullPath) vPath;
                if (fullPath.mTrimPathStart != 0.0f || fullPath.mTrimPathEnd != 1.0f) {
                    float start = (fullPath.mTrimPathStart + fullPath.mTrimPathOffset) % 1.0f;
                    float end = (fullPath.mTrimPathEnd + fullPath.mTrimPathOffset) % 1.0f;

                    if (mPathMeasure == null) {
                        mPathMeasure = new PathMeasure();
                    }
                    mPathMeasure.setPath(mPath, false);

                    float len = mPathMeasure.getLength();
                    start = start * len;
                    end = end * len;
                    path.reset();
                    if (start > end) {
                        mPathMeasure.getSegment(start, len, path, true);
                        mPathMeasure.getSegment(0f, end, path, true);
                    } else {
                        mPathMeasure.getSegment(start, end, path, true);
                    }
                    path.rLineTo(0, 0); // fix bug in measure
                }
                mRenderPath.addPath(path, mFinalPathMatrix);

                if (fullPath.mFillColor.willDraw()) {
                    final ComplexColorCompat fill = fullPath.mFillColor;
                    if (mFillPaint == null) {
                        mFillPaint = new Paint(ANTI_ALIAS_FLAG);
                        mFillPaint.setStyle(FILL);
                    }

                    final Paint fillPaint = mFillPaint;
                    if (fill.isGradient()) {
                        final Shader shader = fill.getShader();
                        // isGradient() implies non-null shader
                        //noinspection ConstantConditions
                        shader.setLocalMatrix(mFinalPathMatrix);
                        fillPaint.setShader(shader);
                        fillPaint.setAlpha(Math.round(fullPath.mFillAlpha * 255f));
                    } else {
                        fillPaint.setShader(null);
                        fillPaint.setAlpha(255);
                        fillPaint.setColor(applyAlpha(fill.getColor(), fullPath.mFillAlpha));
                    }
                    fillPaint.setColorFilter(filter);
                    mRenderPath.setFillType(fullPath.mFillRule == 0 ? Path.FillType.WINDING
                            : Path.FillType.EVEN_ODD);
                    canvas.drawPath(mRenderPath, fillPaint);
                }

                if (fullPath.mStrokeColor.willDraw()) {
                    final ComplexColorCompat strokeColor = fullPath.mStrokeColor;
                    if (mStrokePaint == null) {
                        mStrokePaint = new Paint(ANTI_ALIAS_FLAG);
                        mStrokePaint.setStyle(STROKE);
                    }

                    final Paint strokePaint = mStrokePaint;
                    if (fullPath.mStrokeLineJoin != null) {
                        strokePaint.setStrokeJoin(fullPath.mStrokeLineJoin);
                    }

                    if (fullPath.mStrokeLineCap != null) {
                        strokePaint.setStrokeCap(fullPath.mStrokeLineCap);
                    }

                    strokePaint.setStrokeMiter(fullPath.mStrokeMiterlimit);
                    if (strokeColor.isGradient()) {
                        final Shader shader = strokeColor.getShader();
                        // isGradient() implies non-null shader
                        //noinspection ConstantConditions
                        shader.setLocalMatrix(mFinalPathMatrix);
                        strokePaint.setShader(shader);
                        strokePaint.setAlpha(Math.round(fullPath.mStrokeAlpha * 255f));
                    } else {
                        strokePaint.setShader(null);
                        strokePaint.setAlpha(255);
                        strokePaint.setColor(applyAlpha(strokeColor.getColor(),
                                fullPath.mStrokeAlpha));
                    }
                    strokePaint.setColorFilter(filter);
                    final float finalStrokeScale = minScale * matrixScale;
                    strokePaint.setStrokeWidth(fullPath.mStrokeWidth * finalStrokeScale);
                    canvas.drawPath(mRenderPath, strokePaint);
                }
            }
        }

        private static float cross(float v1x, float v1y, float v2x, float v2y) {
            return v1x * v2y - v1y * v2x;
        }

        private float getMatrixScale(Matrix groupStackedMatrix) {
            // Given unit vectors A = (0, 1) and B = (1, 0).
            // After matrix mapping, we got A' and B'. Let theta = the angel b/t A' and B'.
            // Therefore, the final scale we want is min(|A'| * sin(theta), |B'| * sin(theta)),
            // which is (|A'| * |B'| * sin(theta)) / max (|A'|, |B'|);
            // If  max (|A'|, |B'|) = 0, that means either x or y has a scale of 0.
            //
            // For non-skew case, which is most of the cases, matrix scale is computing exactly the
            // scale on x and y axis, and take the minimal of these two.
            // For skew case, an unit square will mapped to a parallelogram. And this function will
            // return the minimal height of the 2 bases.
            float[] unitVectors = new float[]{0, 1, 1, 0};
            groupStackedMatrix.mapVectors(unitVectors);
            float scaleX = (float) Math.hypot(unitVectors[0], unitVectors[1]);
            float scaleY = (float) Math.hypot(unitVectors[2], unitVectors[3]);
            float crossProduct = cross(unitVectors[0], unitVectors[1], unitVectors[2],
                    unitVectors[3]);
            float maxScale = Math.max(scaleX, scaleY);

            float matrixScale = 0;
            if (maxScale > 0) {
                matrixScale = Math.abs(crossProduct) / maxScale;
            }
            if (DBG_VECTOR_DRAWABLE) {
                Log.d(LOGTAG, "Scale x " + scaleX + " y " + scaleY + " final " + matrixScale);
            }
            return matrixScale;
        }

        public boolean isStateful() {
            if (mIsStateful == null) {
                mIsStateful = mRootGroup.isStateful();
            }
            return mIsStateful;
        }

        public boolean onStateChanged(int[] stateSet) {
            return mRootGroup.onStateChanged(stateSet);
        }
    }

    private abstract static class VObject {

        /**
         * @return {@code true} if this {@code VObject} changes based on state, {@code false}
         * otherwise.
         */
        public boolean isStateful() {
            return false;
        }

        /**
         * @return {@code true} if the state change has caused the appearance of this
         * {@code VObject} to change (that is, it needs to be redrawn), otherwise {@code false}.
         */
        public boolean onStateChanged(int[] stateSet) {
            return false;
        }
    }

    private static class VGroup extends VObject {
        // mStackedMatrix is only used temporarily when drawing, it combines all
        // the parents' local matrices with the current one.
        final Matrix mStackedMatrix = new Matrix();

        /////////////////////////////////////////////////////
        // Variables below need to be copied (deep copy if applicable) for mutation.
        final ArrayList<VObject> mChildren = new ArrayList<>();

        float mRotate = 0;
        private float mPivotX = 0;
        private float mPivotY = 0;
        private float mScaleX = 1;
        private float mScaleY = 1;
        private float mTranslateX = 0;
        private float mTranslateY = 0;

        // mLocalMatrix is updated based on the update of transformation information,
        // either parsed from the XML or by animation.
        final Matrix mLocalMatrix = new Matrix();
        int mChangingConfigurations;
        private int[] mThemeAttrs;
        private String mGroupName = null;

        VGroup(VGroup copy, ArrayMap<String, Object> targetsMap) {
            mRotate = copy.mRotate;
            mPivotX = copy.mPivotX;
            mPivotY = copy.mPivotY;
            mScaleX = copy.mScaleX;
            mScaleY = copy.mScaleY;
            mTranslateX = copy.mTranslateX;
            mTranslateY = copy.mTranslateY;
            mThemeAttrs = copy.mThemeAttrs;
            mGroupName = copy.mGroupName;
            mChangingConfigurations = copy.mChangingConfigurations;
            if (mGroupName != null) {
                targetsMap.put(mGroupName, this);
            }

            mLocalMatrix.set(copy.mLocalMatrix);

            final ArrayList<VObject> children = copy.mChildren;
            for (int i = 0; i < children.size(); i++) {
                Object copyChild = children.get(i);
                if (copyChild instanceof VGroup) {
                    VGroup copyGroup = (VGroup) copyChild;
                    mChildren.add(new VGroup(copyGroup, targetsMap));
                } else {
                    VPath newPath;
                    if (copyChild instanceof VFullPath) {
                        newPath = new VFullPath((VFullPath) copyChild);
                    } else if (copyChild instanceof VClipPath) {
                        newPath = new VClipPath((VClipPath) copyChild);
                    } else {
                        throw new IllegalStateException("Unknown object in the tree!");
                    }
                    mChildren.add(newPath);
                    if (newPath.mPathName != null) {
                        targetsMap.put(newPath.mPathName, newPath);
                    }
                }
            }
        }

        VGroup() {
        }

        public String getGroupName() {
            return mGroupName;
        }

        public Matrix getLocalMatrix() {
            return mLocalMatrix;
        }

        public void inflate(Resources res, AttributeSet attrs, Theme theme, XmlPullParser parser) {
            final TypedArray a = TypedArrayUtils.obtainAttributes(res, theme, attrs,
                    AndroidResources.STYLEABLE_VECTOR_DRAWABLE_GROUP);
            updateStateFromTypedArray(a, parser);
            a.recycle();
        }

        private void updateStateFromTypedArray(TypedArray a, XmlPullParser parser) {
            // Account for any configuration changes.
            // mChangingConfigurations |= Utils.getChangingConfigurations(a);

            // Extract the theme attributes, if any.
            mThemeAttrs = null; // TODO TINT THEME Not supported yet a.extractThemeAttrs();

            // This is added in API 11
            mRotate = TypedArrayUtils.getNamedFloat(a, parser, "rotation",
                    AndroidResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_ROTATION, mRotate);

            mPivotX = a.getFloat(AndroidResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_PIVOT_X, mPivotX);
            mPivotY = a.getFloat(AndroidResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_PIVOT_Y, mPivotY);

            // This is added in API 11
            mScaleX = TypedArrayUtils.getNamedFloat(a, parser, "scaleX",
                    AndroidResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_SCALE_X, mScaleX);

            // This is added in API 11
            mScaleY = TypedArrayUtils.getNamedFloat(a, parser, "scaleY",
                    AndroidResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_SCALE_Y, mScaleY);

            mTranslateX = TypedArrayUtils.getNamedFloat(a, parser, "translateX",
                    AndroidResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_TRANSLATE_X, mTranslateX);
            mTranslateY = TypedArrayUtils.getNamedFloat(a, parser, "translateY",
                    AndroidResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_TRANSLATE_Y, mTranslateY);

            final String groupName =
                    a.getString(AndroidResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_NAME);
            if (groupName != null) {
                mGroupName = groupName;
            }

            updateLocalMatrix();
        }

        private void updateLocalMatrix() {
            // The order we apply is the same as the
            // RenderNode.cpp::applyViewPropertyTransforms().
            mLocalMatrix.reset();
            mLocalMatrix.postTranslate(-mPivotX, -mPivotY);
            mLocalMatrix.postScale(mScaleX, mScaleY);
            mLocalMatrix.postRotate(mRotate, 0, 0);
            mLocalMatrix.postTranslate(mTranslateX + mPivotX, mTranslateY + mPivotY);
        }

        /* Setters and Getters, used by animator from AnimatedVectorDrawable. */
        @SuppressWarnings("unused")
        public float getRotation() {
            return mRotate;
        }

        @SuppressWarnings("unused")
        public void setRotation(float rotation) {
            if (rotation != mRotate) {
                mRotate = rotation;
                updateLocalMatrix();
            }
        }

        @SuppressWarnings("unused")
        public float getPivotX() {
            return mPivotX;
        }

        @SuppressWarnings("unused")
        public void setPivotX(float pivotX) {
            if (pivotX != mPivotX) {
                mPivotX = pivotX;
                updateLocalMatrix();
            }
        }

        @SuppressWarnings("unused")
        public float getPivotY() {
            return mPivotY;
        }

        @SuppressWarnings("unused")
        public void setPivotY(float pivotY) {
            if (pivotY != mPivotY) {
                mPivotY = pivotY;
                updateLocalMatrix();
            }
        }

        @SuppressWarnings("unused")
        public float getScaleX() {
            return mScaleX;
        }

        @SuppressWarnings("unused")
        public void setScaleX(float scaleX) {
            if (scaleX != mScaleX) {
                mScaleX = scaleX;
                updateLocalMatrix();
            }
        }

        @SuppressWarnings("unused")
        public float getScaleY() {
            return mScaleY;
        }

        @SuppressWarnings("unused")
        public void setScaleY(float scaleY) {
            if (scaleY != mScaleY) {
                mScaleY = scaleY;
                updateLocalMatrix();
            }
        }

        @SuppressWarnings("unused")
        public float getTranslateX() {
            return mTranslateX;
        }

        @SuppressWarnings("unused")
        public void setTranslateX(float translateX) {
            if (translateX != mTranslateX) {
                mTranslateX = translateX;
                updateLocalMatrix();
            }
        }

        @SuppressWarnings("unused")
        public float getTranslateY() {
            return mTranslateY;
        }

        @SuppressWarnings("unused")
        public void setTranslateY(float translateY) {
            if (translateY != mTranslateY) {
                mTranslateY = translateY;
                updateLocalMatrix();
            }
        }

        @Override
        public boolean isStateful() {
            for (int i = 0; i < mChildren.size(); i++) {
                if (mChildren.get(i).isStateful()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onStateChanged(int[] stateSet) {
            boolean changed = false;
            for (int i = 0; i < mChildren.size(); i++) {
                changed |= mChildren.get(i).onStateChanged(stateSet);
            }
            return changed;
        }
    }

    /**
     * Common Path information for clip path and normal path.
     */
    private abstract static class VPath extends VObject {
        protected static final int FILL_TYPE_WINDING = 0;
        protected PathParser.PathDataNode[] mNodes = null;
        String mPathName;
        // Default fill rule is winding, or as known as "non-zero".
        int mFillRule = FILL_TYPE_WINDING;
        int mChangingConfigurations;

        VPath() {
            // Empty constructor.
        }

        public void printVPath(int level) {
            StringBuilder indent = new StringBuilder();
            for (int i = 0; i < level; i++) {
                indent.append("    ");
            }
            Log.v(LOGTAG, indent + "current path is :" + mPathName
                    + " pathData is " + nodesToString(mNodes));

        }

        public String nodesToString(PathParser.PathDataNode[] nodes) {
            StringBuilder result = new StringBuilder(" ");
            for (PathParser.PathDataNode node : nodes) {
                result.append(node.getType()).append(":");
                float[] params = node.getParams();
                for (float param : params) {
                    result.append(param).append(",");
                }
            }
            return result.toString();
        }

        @SuppressWarnings("CopyConstructorMissesField")
        VPath(VPath copy) {
            mPathName = copy.mPathName;
            mChangingConfigurations = copy.mChangingConfigurations;
            mNodes = PathParser.deepCopyNodes(copy.mNodes);
        }

        public void toPath(Path path) {
            path.reset();
            if (mNodes != null) {
                PathParser.nodesToPath(mNodes, path);
            }
        }

        public String getPathName() {
            return mPathName;
        }

        @SuppressWarnings("unused")
        public boolean canApplyTheme() {
            return false;
        }

        @SuppressWarnings("unused")
        public void applyTheme(Theme t) {
        }

        public boolean isClipPath() {
            return false;
        }

        /* Setters and Getters, used by animator from AnimatedVectorDrawable. */
        @SuppressWarnings("unused")
        public PathParser.PathDataNode[] getPathData() {
            return mNodes;
        }

        @SuppressWarnings("unused")
        public void setPathData(PathParser.PathDataNode[] nodes) {
            if (!PathParser.canMorph(mNodes, nodes)) {
                // This should not happen in the middle of animation.
                mNodes = PathParser.deepCopyNodes(nodes);
            } else {
                PathParser.updateNodes(mNodes, nodes);
            }
        }
    }

    /**
     * Clip path, which only has name and pathData.
     */
    private static class VClipPath extends VPath {
        VClipPath() {
            // Empty constructor.
        }

        VClipPath(VClipPath copy) {
            super(copy);
        }

        public void inflate(Resources r, AttributeSet attrs, Theme theme, XmlPullParser parser) {
            // TODO TINT THEME Not supported yet
            final boolean hasPathData = TypedArrayUtils.hasAttribute(parser, "pathData");
            if (!hasPathData) {
                return;
            }
            final TypedArray a = TypedArrayUtils.obtainAttributes(r, theme, attrs,
                    AndroidResources.STYLEABLE_VECTOR_DRAWABLE_CLIP_PATH);
            updateStateFromTypedArray(a, parser);
            a.recycle();
        }

        private void updateStateFromTypedArray(TypedArray a, XmlPullParser parser) {
            // Account for any configuration changes.
            // mChangingConfigurations |= Utils.getChangingConfigurations(a);;

            final String pathName =
                    a.getString(AndroidResources.STYLEABLE_VECTOR_DRAWABLE_CLIP_PATH_NAME);
            if (pathName != null) {
                mPathName = pathName;
            }

            final String pathData =
                    a.getString(AndroidResources.STYLEABLE_VECTOR_DRAWABLE_CLIP_PATH_PATH_DATA);
            if (pathData != null) {
                mNodes = PathParser.createNodesFromPathData(pathData);
            }
            mFillRule = TypedArrayUtils.getNamedInt(a, parser, "fillType",
                    AndroidResources.STYLEABLE_VECTOR_DRAWABLE_CLIP_PATH_FILLTYPE,
                    FILL_TYPE_WINDING);
        }

        @Override
        public boolean isClipPath() {
            return true;
        }
    }

    /**
     * Normal path, which contains all the fill / paint information.
     */
    private static class VFullPath extends VPath {
        /////////////////////////////////////////////////////
        // Variables below need to be copied (deep copy if applicable) for mutation.
        private int[] mThemeAttrs;
        ComplexColorCompat mStrokeColor;
        float mStrokeWidth = 0;

        ComplexColorCompat mFillColor;
        float mStrokeAlpha = 1.0f;
        float mFillAlpha = 1.0f;
        float mTrimPathStart = 0;
        float mTrimPathEnd = 1;
        float mTrimPathOffset = 0;

        Cap mStrokeLineCap = Cap.BUTT;
        Join mStrokeLineJoin = Join.MITER;
        float mStrokeMiterlimit = 4;

        VFullPath() {
            // Empty constructor.
        }

        VFullPath(VFullPath copy) {
            super(copy);
            mThemeAttrs = copy.mThemeAttrs;

            mStrokeColor = copy.mStrokeColor;
            mStrokeWidth = copy.mStrokeWidth;
            mStrokeAlpha = copy.mStrokeAlpha;
            mFillColor = copy.mFillColor;
            mFillRule = copy.mFillRule;
            mFillAlpha = copy.mFillAlpha;
            mTrimPathStart = copy.mTrimPathStart;
            mTrimPathEnd = copy.mTrimPathEnd;
            mTrimPathOffset = copy.mTrimPathOffset;

            mStrokeLineCap = copy.mStrokeLineCap;
            mStrokeLineJoin = copy.mStrokeLineJoin;
            mStrokeMiterlimit = copy.mStrokeMiterlimit;
        }

        private Cap getStrokeLineCap(int id, Cap defValue) {
            switch (id) {
                case LINECAP_BUTT:
                    return Cap.BUTT;
                case LINECAP_ROUND:
                    return Cap.ROUND;
                case LINECAP_SQUARE:
                    return Cap.SQUARE;
                default:
                    return defValue;
            }
        }

        private Join getStrokeLineJoin(int id, Join defValue) {
            switch (id) {
                case LINEJOIN_MITER:
                    return Join.MITER;
                case LINEJOIN_ROUND:
                    return Join.ROUND;
                case LINEJOIN_BEVEL:
                    return Join.BEVEL;
                default:
                    return defValue;
            }
        }

        @Override
        public boolean canApplyTheme() {
            return mThemeAttrs != null;
        }

        public void inflate(Resources r, AttributeSet attrs, Theme theme, XmlPullParser parser) {
            final TypedArray a = TypedArrayUtils.obtainAttributes(r, theme, attrs,
                    AndroidResources.STYLEABLE_VECTOR_DRAWABLE_PATH);
            updateStateFromTypedArray(a, parser, theme);
            a.recycle();
        }

        private void updateStateFromTypedArray(TypedArray a, XmlPullParser parser, Theme theme) {
            // Account for any configuration changes.
            // mChangingConfigurations |= Utils.getChangingConfigurations(a);

            // Extract the theme attributes, if any.
            mThemeAttrs = null; // TODO TINT THEME Not supported yet a.extractThemeAttrs();

            // In order to work around the conflicting id issue, we need to double check the
            // existence of the attribute.
            // B/c if the attribute existed in the compiled XML, then calling TypedArray will be
            // safe since the framework will look up in the XML first.
            // Note that each getAttributeValue take roughly 0.03ms, it is a price we have to pay.
            final boolean hasPathData = TypedArrayUtils.hasAttribute(parser, "pathData");
            if (!hasPathData) {
                // If there is no pathData in the <path> tag, then this is an empty path,
                // nothing need to be drawn.
                return;
            }

            final String pathName = a.getString(
                    AndroidResources.STYLEABLE_VECTOR_DRAWABLE_PATH_NAME);
            if (pathName != null) {
                mPathName = pathName;
            }
            final String pathData =
                    a.getString(AndroidResources.STYLEABLE_VECTOR_DRAWABLE_PATH_PATH_DATA);
            if (pathData != null) {
                mNodes = PathParser.createNodesFromPathData(pathData);
            }

            mFillColor = TypedArrayUtils.getNamedComplexColor(a, parser, theme, "fillColor",
                    AndroidResources.STYLEABLE_VECTOR_DRAWABLE_PATH_FILL_COLOR, TRANSPARENT);
            mFillAlpha = TypedArrayUtils.getNamedFloat(a, parser, "fillAlpha",
                    AndroidResources.STYLEABLE_VECTOR_DRAWABLE_PATH_FILL_ALPHA, mFillAlpha);
            final int lineCap = TypedArrayUtils.getNamedInt(a, parser, "strokeLineCap",
                    AndroidResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_LINE_CAP, -1);
            mStrokeLineCap = getStrokeLineCap(lineCap, mStrokeLineCap);
            final int lineJoin = TypedArrayUtils.getNamedInt(a, parser, "strokeLineJoin",
                    AndroidResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_LINE_JOIN, -1);
            mStrokeLineJoin = getStrokeLineJoin(lineJoin, mStrokeLineJoin);
            mStrokeMiterlimit = TypedArrayUtils.getNamedFloat(a, parser, "strokeMiterLimit",
                    AndroidResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_MITER_LIMIT,
                    mStrokeMiterlimit);
            mStrokeColor = TypedArrayUtils.getNamedComplexColor(a, parser, theme, "strokeColor",
                    AndroidResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_COLOR, TRANSPARENT);
            mStrokeAlpha = TypedArrayUtils.getNamedFloat(a, parser, "strokeAlpha",
                    AndroidResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_ALPHA, mStrokeAlpha);
            mStrokeWidth = TypedArrayUtils.getNamedFloat(a, parser, "strokeWidth",
                    AndroidResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_WIDTH, mStrokeWidth);
            mTrimPathEnd = TypedArrayUtils.getNamedFloat(a, parser, "trimPathEnd",
                    AndroidResources.STYLEABLE_VECTOR_DRAWABLE_PATH_TRIM_PATH_END, mTrimPathEnd);
            mTrimPathOffset = TypedArrayUtils.getNamedFloat(a, parser, "trimPathOffset",
                    AndroidResources.STYLEABLE_VECTOR_DRAWABLE_PATH_TRIM_PATH_OFFSET,
                    mTrimPathOffset);
            mTrimPathStart = TypedArrayUtils.getNamedFloat(a, parser, "trimPathStart",
                    AndroidResources.STYLEABLE_VECTOR_DRAWABLE_PATH_TRIM_PATH_START,
                    mTrimPathStart);
            mFillRule = TypedArrayUtils.getNamedInt(a, parser, "fillType",
                    AndroidResources.STYLEABLE_VECTOR_DRAWABLE_PATH_TRIM_PATH_FILLTYPE,
                    mFillRule);
        }

        @Override
        public boolean isStateful() {
            return mFillColor.isStateful() || mStrokeColor.isStateful();
        }

        @Override
        public boolean onStateChanged(int[] stateSet) {
            boolean changed = mFillColor.onStateChanged(stateSet);
            changed |= mStrokeColor.onStateChanged(stateSet);
            return changed;
        }

        @Override
        public void applyTheme(Theme t) {
            /*
             * TODO TINT THEME Not supported yet final TypedArray a =
             * t.resolveAttributes(mThemeAttrs, styleable_VectorDrawablePath);
             * updateStateFromTypedArray(a); a.recycle();
             */
        }

        /* Setters and Getters, used by animator from AnimatedVectorDrawable. */
        @SuppressWarnings("unused")
        @ColorInt
        int getStrokeColor() {
            return mStrokeColor.getColor();
        }

        @SuppressWarnings("unused")
        void setStrokeColor(int strokeColor) {
            mStrokeColor.setColor(strokeColor);
        }

        @SuppressWarnings("unused")
        float getStrokeWidth() {
            return mStrokeWidth;
        }

        @SuppressWarnings("unused")
        void setStrokeWidth(float strokeWidth) {
            mStrokeWidth = strokeWidth;
        }

        @SuppressWarnings("unused")
        float getStrokeAlpha() {
            return mStrokeAlpha;
        }

        @SuppressWarnings("unused")
        void setStrokeAlpha(float strokeAlpha) {
            mStrokeAlpha = strokeAlpha;
        }

        @SuppressWarnings("unused")
        @ColorInt
        int getFillColor() {
            return mFillColor.getColor();
        }

        @SuppressWarnings("unused")
        void setFillColor(int fillColor) {
            mFillColor.setColor(fillColor);
        }

        @SuppressWarnings("unused")
        float getFillAlpha() {
            return mFillAlpha;
        }

        @SuppressWarnings("unused")
        void setFillAlpha(float fillAlpha) {
            mFillAlpha = fillAlpha;
        }

        @SuppressWarnings("unused")
        float getTrimPathStart() {
            return mTrimPathStart;
        }

        @SuppressWarnings("unused")
        void setTrimPathStart(float trimPathStart) {
            mTrimPathStart = trimPathStart;
        }

        @SuppressWarnings("unused")
        float getTrimPathEnd() {
            return mTrimPathEnd;
        }

        @SuppressWarnings("unused")
        void setTrimPathEnd(float trimPathEnd) {
            mTrimPathEnd = trimPathEnd;
        }

        @SuppressWarnings("unused")
        float getTrimPathOffset() {
            return mTrimPathOffset;
        }

        @SuppressWarnings("unused")
        void setTrimPathOffset(float trimPathOffset) {
            mTrimPathOffset = trimPathOffset;
        }
    }
}
