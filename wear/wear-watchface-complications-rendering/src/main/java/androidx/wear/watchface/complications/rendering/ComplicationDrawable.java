/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface.complications.rendering;

import android.app.PendingIntent.CanceledException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.wearable.complications.ComplicationData;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.VisibleForTesting;
import androidx.wear.complications.ComplicationHelperActivity;
import androidx.wear.watchface.complications.rendering.ComplicationRenderer.OnInvalidateListener;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Objects;

/**
 * A styleable drawable object that draws complications. You can create a ComplicationDrawable from
 * XML inflation or by using one of the constructor methods.
 *
 * <h3>Constructing a ComplicationDrawable</h3>
 *
 * <p>To construct a ComplicationDrawable programmatically, use the {@link
 * #ComplicationDrawable(Context)} constructor. Afterwards, styling attributes you want to modify
 * can be set via set methods.
 *
 * <pre>
 * public void onCreate(SurfaceHolder holder) {
 *   ...
 *   ComplicationDrawable complicationDrawable = new ComplicationDrawable(WatchFaceService.this);
 *   complicationDrawable.setBackgroundColorActive(backgroundColor);
 *   complicationDrawable.setTextColorActive(textColor);
 *   ...
 * }</pre>
 *
 * <h3>Constructing a ComplicationDrawable from XML</h3>
 *
 * <p>Constructing a ComplicationDrawable from an XML file makes it easier to modify multiple
 * styling attributes at once without calling any set methods. You may also use different XML files
 * to switch between different styles your watch face supports.
 *
 * <p>To construct a ComplicationDrawable from a drawable XML file, you may create an XML file in
 * your project's {@code res/drawable} folder. A ComplicationDrawable with red text and white title
 * in active mode, and white text and white title in ambient mode would look like this:
 *
 * <pre>
 * &lt;?xml version="1.0" encoding="utf-8"?&gt;
 * &lt;android.support.wearable.complications.rendering.ComplicationDrawable
 *   xmlns:app="http://schemas.android.com/apk/res-auto"
 *   app:textColor="#FFFF0000"
 *   app:titleColor="#FFFFFFFF"&gt;
 *   &lt;ambient
 *     app:textColor="#FFFFFFFF" /&gt;
 * &lt;/android.support.wearable.complications.rendering.ComplicationDrawable&gt;
 * </pre>
 *
 * <p>A top-level {@code drawable} tag with the {@code class} attribute may also be used to
 * construct a ComplicationDrawable from an XML file:
 *
 * <pre>
 * &lt;?xml version="1.0" encoding="utf-8"?&gt;
 * &lt;drawable
 *   class="android.support.wearable.complications.rendering.ComplicationDrawable"
 *   xmlns:app="http://schemas.android.com/apk/res-auto"
 *   app:textColor="#FFFF0000"
 *   app:titleColor="#FFFFFFFF"&gt;
 *   &lt;ambient
 *     app:textColor="#FFFFFFFF" /&gt;
 * &lt;/drawable&gt;</pre>
 *
 * <p>To inflate a ComplicationDrawable from XML file, use the {@link #getDrawable(Context, int)}
 * method. ComplicationDrawable needs access to the current context in order to style and draw
 * the complication.
 *
 * <pre>
 * public void onCreate(SurfaceHolder holder) {
 *   ...
 *   ComplicationDrawable complicationDrawable = (ComplicationDrawable)
 *       getDrawable(R.drawable.complication);
 *   complicationDrawable.setContext(WatchFaceService.this);
 *   ...
 * }</pre>
 *
 * <h4>Syntax:</h4>
 *
 * <pre>
 * &lt;?xml version="1.0" encoding="utf-8"?&gt;
 * &lt;android.support.wearable.complications.rendering.ComplicationDrawable
 *   xmlns:app="http://schemas.android.com/apk/res-auto"
 *   app:backgroundColor="color"
 *   app:backgroundDrawable="drawable"
 *   app:borderColor="color"
 *   app:borderDashGap="dimension"
 *   app:borderDashWidth="dimension"
 *   app:borderRadius="dimension"
 *   app:borderStyle="none|solid|dashed"
 *   app:borderWidth="dimension"
 *   app:highlightColor="color"
 *   app:iconColor="color"
 *   app:rangedValuePrimaryColor="color"
 *   app:rangedValueProgressHidden="boolean"
 *   app:rangedValueRingWidth="dimension"
 *   app:rangedValueSecondaryColor="color"
 *   app:textColor="color"
 *   app:textSize="dimension"
 *   app:textTypeface="string"
 *   app:titleColor="color"
 *   app:titleSize="dimension"
 *   app:titleTypeface="string"&gt;
 *   &lt;ambient
 *     app:backgroundColor="color"
 *     app:backgroundDrawable="drawable"
 *     app:borderColor="color"
 *     app:borderDashGap="dimension"
 *     app:borderDashWidth="dimension"
 *     app:borderRadius="dimension"
 *     app:borderStyle="none|solid|dashed"
 *     app:borderWidth="dimension"
 *     app:highlightColor="color"
 *     app:iconColor="color"
 *     app:rangedValuePrimaryColor="color"
 *     app:rangedValueRingWidth="dimension"
 *     app:rangedValueSecondaryColor="color"
 *     app:textColor="color"
 *     app:textSize="dimension"
 *     app:textTypeface="string"
 *     app:titleColor="color"
 *     app:titleSize="dimension"
 *     app:titleTypeface="string" /&gt;
 * &lt;/android.support.wearable.complications.rendering.ComplicationDrawable&gt;
 * </pre>
 *
 * <p>Attributes of the top-level tag apply to both active and ambient modes while attributes of the
 * inner {@code ambient} tag only apply to ambient mode. As an exception, top-level only {@code
 * rangedValueProgressHidden} attribute applies to both modes, and cannot be overridden in ambient
 * mode. To hide ranged value in only one of the active or ambient modes, you may consider setting
 * {@code rangedValuePrimaryColor} and {@code rangedValueSecondaryColor} to {@link
 * android.graphics.Color#TRANSPARENT} instead.
 *
 * <h3>Drawing a ComplicationDrawable</h3>
 *
 * <p>Depending on the size and shape of the bounds, the layout of the complication may change. For
 * instance, a short text complication with an icon that is drawn on square bounds would draw the
 * icon above the short text, but a short text complication with an icon that is drawn on wide
 * rectangular bounds might draw the icon to the left of the short text instead.
 */
public final class ComplicationDrawable extends Drawable {

    private Context mContext;
    private ComplicationRenderer mComplicationRenderer;

    private final ComplicationStyle mActiveStyle;
    private final ComplicationStyle mAmbientStyle;

    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

    private final Runnable mUnhighlightRunnable =
            () -> {
                setHighlighted(false);
                invalidateSelf();
            };

    private final OnInvalidateListener mRendererInvalidateListener = () -> invalidateSelf();

    private CharSequence mNoDataText;
    private long mHighlightDuration;
    private long mCurrentTimeMillis;
    private boolean mInAmbientMode;
    private boolean mLowBitAmbient;
    private boolean mBurnInProtection;
    private boolean mHighlighted;
    private boolean mRangedValueProgressHidden;

    private boolean mIsInflatedFromXml;
    private boolean mAlreadyStyled;

    /** Default constructor. */
    public ComplicationDrawable() {
        mActiveStyle = new ComplicationStyle();
        mAmbientStyle = new ComplicationStyle();
    }

    /**
     * Creates a ComplicationDrawable using the given context. If this constructor is used, calling
     * {@link #setContext(Context)} may not be necessary.
     */
    public ComplicationDrawable(@NonNull Context context) {
        this();
        setContext(context);
    }

    public ComplicationDrawable(@NonNull ComplicationDrawable drawable) {
        mActiveStyle = new ComplicationStyle(drawable.mActiveStyle);
        mAmbientStyle = new ComplicationStyle(drawable.mAmbientStyle);
        mNoDataText = drawable.mNoDataText.subSequence(0, drawable.mNoDataText.length());
        mHighlightDuration = drawable.mHighlightDuration;
        mCurrentTimeMillis = drawable.mCurrentTimeMillis;
        setBounds(drawable.getBounds());

        mInAmbientMode = drawable.mInAmbientMode;
        mLowBitAmbient = drawable.mLowBitAmbient;
        mBurnInProtection = drawable.mBurnInProtection;
        mHighlighted = false;
        mRangedValueProgressHidden = drawable.mRangedValueProgressHidden;
        mIsInflatedFromXml = drawable.mIsInflatedFromXml;
        mAlreadyStyled = true;
    }

    /**
     * Creates a ComplicationDrawable from a resource.
     *
     * @param context The {@link Context} to load the resource from
     * @param id      The id of the resource to load
     * @return The {@link ComplicationDrawable} loaded from the specified resource id or null if it
     * doesn't exist.
     */
    @Nullable
    public static ComplicationDrawable getDrawable(@NonNull Context context, int id) {
        if (context == null) {
            throw new IllegalArgumentException("Argument \"context\" should not be null.");
        }
        ComplicationDrawable drawable = (ComplicationDrawable) context.getDrawable(id);
        if (drawable == null) {
            return null;
        }

        drawable.setContext(context);
        return drawable;
    }

    /** Sets the style to default values using resources. */
    private static void setStyleToDefaultValues(ComplicationStyle style, Resources r) {
        style.setBackgroundColor(
                r.getColor(R.color.complicationDrawable_backgroundColor, null));
        style.setTextColor(r.getColor(R.color.complicationDrawable_textColor, null));
        style.setTitleColor(r.getColor(R.color.complicationDrawable_titleColor, null));
        style.setTextTypeface(
                Typeface.create(
                        r.getString(R.string.complicationDrawable_textTypeface), Typeface.NORMAL));
        style.setTitleTypeface(
                Typeface.create(
                        r.getString(R.string.complicationDrawable_titleTypeface), Typeface.NORMAL));
        style.setTextSize(r.getDimensionPixelSize(R.dimen.complicationDrawable_textSize));
        style.setTitleSize(r.getDimensionPixelSize(R.dimen.complicationDrawable_titleSize));
        style.setIconColor(r.getColor(R.color.complicationDrawable_iconColor, null));
        style.setBorderColor(r.getColor(R.color.complicationDrawable_borderColor, null));
        style.setBorderWidth(r.getDimensionPixelSize(R.dimen.complicationDrawable_borderWidth));
        style.setBorderRadius(r.getDimensionPixelSize(R.dimen.complicationDrawable_borderRadius));
        style.setBorderStyle(r.getInteger(R.integer.complicationDrawable_borderStyle));
        style.setBorderDashWidth(
                r.getDimensionPixelSize(R.dimen.complicationDrawable_borderDashWidth));
        style.setBorderDashGap(r.getDimensionPixelSize(R.dimen.complicationDrawable_borderDashGap));
        style.setRangedValueRingWidth(
                r.getDimensionPixelSize(R.dimen.complicationDrawable_rangedValueRingWidth));
        style.setRangedValuePrimaryColor(
                r.getColor(R.color.complicationDrawable_rangedValuePrimaryColor, null));
        style.setRangedValueSecondaryColor(
                r.getColor(R.color.complicationDrawable_rangedValueSecondaryColor, null));
        style.setHighlightColor(r.getColor(R.color.complicationDrawable_highlightColor, null));
    }

    /**
     * Sets the context used to render the complication. If a context is not set,
     * ComplicationDrawable will throw an {@link IllegalStateException} if one of
     * {@link #draw(Canvas)}, {@link #setBounds(Rect)}, or {@link
     * #setComplicationData(ComplicationData)} is called.
     *
     * <p>While this can be called from any context, ideally, a
     * androidx.wear.watchface.WatchFaceService object should be passed here to allow creating
     * permission dialogs by the {@link #onTap(int, int)} method, in case current watch face
     * doesn't have the permission to receive complication data.
     *
     * <p>If this ComplicationDrawable is retrieved using {@link Resources#getDrawable(int, Theme)},
     * this method must be called before calling any of the methods mentioned above.
     *
     * <p>If this ComplicationDrawable is not inflated from an XML file, this method will reset the
     * style to match the default values, so if {@link #ComplicationDrawable()} is used to construct
     * a ComplicationDrawable, this method should be called right after.
     */
    public void setContext(@NonNull Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Argument \"context\" should not be null.");
        }
        if (Objects.equals(context, mContext)) {
            return;
        }
        mContext = context;

        if (!mIsInflatedFromXml && !mAlreadyStyled) {
            setStyleToDefaultValues(mActiveStyle, context.getResources());
            setStyleToDefaultValues(mAmbientStyle, context.getResources());
        }

        if (!mAlreadyStyled) {
            mHighlightDuration = context.getResources()
                    .getInteger(R.integer.complicationDrawable_highlightDurationMs);
        }

        mComplicationRenderer = new ComplicationRenderer(mContext, mActiveStyle, mAmbientStyle);
        mComplicationRenderer.setOnInvalidateListener(mRendererInvalidateListener);
        if (mNoDataText == null) {
            setNoDataText(context.getString(R.string.complicationDrawable_noDataText));
        } else {
            mComplicationRenderer.setNoDataText(mNoDataText);
        }
        mComplicationRenderer.setRangedValueProgressHidden(mRangedValueProgressHidden);
        mComplicationRenderer.setBounds(getBounds());
    }

    /**
     * Returns the {@link Context} used to render the complication.
     */
    @Nullable public Context getContext() {
        return mContext;
    }

    private void inflateAttributes(Resources r, XmlPullParser parser) {
        TypedArray a =
                r.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.ComplicationDrawable);
        setRangedValueProgressHidden(
                a.getBoolean(R.styleable.ComplicationDrawable_rangedValueProgressHidden, false));
        a.recycle();
    }

    private void inflateStyle(boolean isAmbient, Resources r, XmlPullParser parser) {
        TypedArray a =
                r.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.ComplicationDrawable);
        ComplicationStyle complicationStyle = isAmbient ? mAmbientStyle : mActiveStyle;
        if (a.hasValue(R.styleable.ComplicationDrawable_backgroundColor)) {
            complicationStyle.setBackgroundColor(
                    a.getColor(
                            R.styleable.ComplicationDrawable_backgroundColor,
                            r.getColor(R.color.complicationDrawable_backgroundColor, null)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_backgroundDrawable)) {
            complicationStyle.setBackgroundDrawable(
                    a.getDrawable(R.styleable.ComplicationDrawable_backgroundDrawable));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_textColor)) {
            complicationStyle.setTextColor(
                    a.getColor(
                            R.styleable.ComplicationDrawable_textColor,
                            r.getColor(R.color.complicationDrawable_textColor, null)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_titleColor)) {
            complicationStyle.setTitleColor(
                    a.getColor(
                            R.styleable.ComplicationDrawable_titleColor,
                            r.getColor(R.color.complicationDrawable_titleColor, null)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_textTypeface)) {
            complicationStyle.setTextTypeface(
                    Typeface.create(
                            a.getString(R.styleable.ComplicationDrawable_textTypeface),
                            Typeface.NORMAL));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_titleTypeface)) {
            complicationStyle.setTitleTypeface(
                    Typeface.create(
                            a.getString(R.styleable.ComplicationDrawable_titleTypeface),
                            Typeface.NORMAL));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_textSize)) {
            complicationStyle.setTextSize(
                    a.getDimensionPixelSize(
                            R.styleable.ComplicationDrawable_textSize,
                            r.getDimensionPixelSize(R.dimen.complicationDrawable_textSize)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_titleSize)) {
            complicationStyle.setTitleSize(
                    a.getDimensionPixelSize(
                            R.styleable.ComplicationDrawable_titleSize,
                            r.getDimensionPixelSize(R.dimen.complicationDrawable_titleSize)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_iconColor)) {
            complicationStyle.setIconColor(
                    a.getColor(
                            R.styleable.ComplicationDrawable_iconColor,
                            r.getColor(R.color.complicationDrawable_iconColor, null)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_borderColor)) {
            complicationStyle.setBorderColor(
                    a.getColor(
                            R.styleable.ComplicationDrawable_borderColor,
                            r.getColor(R.color.complicationDrawable_borderColor, null)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_borderRadius)) {
            complicationStyle.setBorderRadius(
                    a.getDimensionPixelSize(
                            R.styleable.ComplicationDrawable_borderRadius,
                            r.getDimensionPixelSize(R.dimen.complicationDrawable_borderRadius)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_borderStyle)) {
            complicationStyle.setBorderStyle(
                    a.getInt(
                            R.styleable.ComplicationDrawable_borderStyle,
                            r.getInteger(R.integer.complicationDrawable_borderStyle)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_borderDashWidth)) {
            complicationStyle.setBorderDashWidth(
                    a.getDimensionPixelSize(
                            R.styleable.ComplicationDrawable_borderDashWidth,
                            r.getDimensionPixelSize(R.dimen.complicationDrawable_borderDashWidth)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_borderDashGap)) {
            complicationStyle.setBorderDashGap(
                    a.getDimensionPixelSize(
                            R.styleable.ComplicationDrawable_borderDashGap,
                            r.getDimensionPixelSize(R.dimen.complicationDrawable_borderDashGap)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_borderWidth)) {
            complicationStyle.setBorderWidth(
                    a.getDimensionPixelSize(
                            R.styleable.ComplicationDrawable_borderWidth,
                            r.getDimensionPixelSize(R.dimen.complicationDrawable_borderWidth)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_rangedValueRingWidth)) {
            complicationStyle.setRangedValueRingWidth(
                    a.getDimensionPixelSize(
                            R.styleable.ComplicationDrawable_rangedValueRingWidth,
                            r.getDimensionPixelSize(
                                    R.dimen.complicationDrawable_rangedValueRingWidth)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_rangedValuePrimaryColor)) {
            complicationStyle.setRangedValuePrimaryColor(
                    a.getColor(
                            R.styleable.ComplicationDrawable_rangedValuePrimaryColor,
                            r.getColor(
                                    R.color.complicationDrawable_rangedValuePrimaryColor, null)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_rangedValueSecondaryColor)) {
            complicationStyle.setRangedValueSecondaryColor(
                    a.getColor(
                            R.styleable.ComplicationDrawable_rangedValueSecondaryColor,
                            r.getColor(
                                    R.color.complicationDrawable_rangedValueSecondaryColor, null)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_highlightColor)) {
            complicationStyle.setHighlightColor(
                    a.getColor(
                            R.styleable.ComplicationDrawable_highlightColor,
                            r.getColor(R.color.complicationDrawable_highlightColor, null)));
        }
        a.recycle();
    }

    /**
     * Inflates this ComplicationDrawable from an XML resource. This can't be called more than once
     * for each ComplicationDrawable. Note that framework may have called this once to create the
     * ComplicationDrawable instance from an XML resource.
     *
     * @param r      Resources used to resolve attribute values
     * @param parser XML parser from which to inflate this ComplicationDrawable
     * @param attrs  Base set of attribute values
     * @param theme  Ignored by ComplicationDrawable
     */
    @Override
    public void inflate(@NonNull Resources r, @NonNull XmlPullParser parser,
            @NonNull AttributeSet attrs, @Nullable Theme theme)
            throws XmlPullParserException, IOException {
        if (mIsInflatedFromXml) {
            throw new IllegalStateException("inflate may be called once only.");
        }
        mIsInflatedFromXml = true;
        int type;
        final int outerDepth = parser.getDepth();
        // Inflate attributes always shared between active and ambient mode
        inflateAttributes(r, parser);
        // Reset both style builders to default values
        setStyleToDefaultValues(mActiveStyle, r);
        setStyleToDefaultValues(mAmbientStyle, r);
        // Attributes of the outer tag applies to both active and ambient styles
        inflateStyle(false, r, parser);
        inflateStyle(true, r, parser);
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            // Attributes of inner <ambient> tag applies to ambient style only
            final String name = parser.getName();
            if (TextUtils.equals(name, "ambient")) {
                inflateStyle(true, r, parser);
            } else {
                Log.w(
                        "ComplicationDrawable",
                        "Unknown element: " + name + " for ComplicationDrawable " + this);
            }
        }
    }

    /**
     * Draws the complication for the last known time. Last known time is derived from
     * ComplicationDrawable#setCurrentTimeMillis(long)}.
     *
     * @param canvas Canvas for the complication to be drawn onto
     */
    @Override
    public void draw(@NonNull Canvas canvas) {
        assertInitialized();
        updateStyleIfRequired();
        mComplicationRenderer.draw(
                canvas,
                mCurrentTimeMillis,
                mInAmbientMode,
                mLowBitAmbient,
                mBurnInProtection,
                mHighlighted);
    }

    /** Does nothing. */
    @Override
    public void setAlpha(int alpha) {
        // No op.
    }

    /**
     * Does nothing. Use {@link ComplicationStyle#setImageColorFilter(ColorFilter)} instead to apply
     * color filter to small and large images.
     */
    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        // No op.
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated This method is no longer used in graphics optimizations
     */
    @Override
    @Deprecated
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        if (mComplicationRenderer != null) {
            mComplicationRenderer.setBounds(bounds);
        }
    }

    /**
     * Sets the text to be rendered when {@link ComplicationData} is of type {@link
     * ComplicationData#TYPE_NO_DATA}. If {@code noDataText} is null, an empty text will be
     * rendered.
     */
    public void setNoDataText(@Nullable CharSequence noDataText) {
        if (noDataText == null) {
            mNoDataText = "";
        } else {
            mNoDataText = noDataText.subSequence(0, noDataText.length());
        }
        if (mComplicationRenderer != null) {
            mComplicationRenderer.setNoDataText(mNoDataText);
        }
    }

    /**
     * Sets if the ranged value progress should be hidden when {@link ComplicationData} is of type
     * {@link ComplicationData#TYPE_RANGED_VALUE}.
     *
     * @param rangedValueProgressHidden {@code true} if progress should be hidden, {@code false}
     *                                  otherwise
     * @attr ref androidx.wear.watchface.complications.rendering.R
     *     .styleable#ComplicationDrawable_rangedValueProgressHidden
     */
    public void setRangedValueProgressHidden(boolean rangedValueProgressHidden) {
        mRangedValueProgressHidden = rangedValueProgressHidden;
        if (mComplicationRenderer != null) {
            mComplicationRenderer.setRangedValueProgressHidden(rangedValueProgressHidden);
        }
    }

    /** Returns {@code true} if the ranged value progress is hidden, {@code false} otherwise. */
    public boolean isRangedValueProgressHidden() {
        return mRangedValueProgressHidden;
    }

    /**
     * Sets the complication data to be drawn. If {@code complicationData} is {@code null}, nothing
     * will be drawn when {@link #draw(Canvas)} is called.
     */
    public void setComplicationData(@Nullable ComplicationData complicationData) {
        assertInitialized();
        mComplicationRenderer.setComplicationData(complicationData);
    }

    /**
     * Returns the {@link ComplicationData} to be drawn by this ComplicationDrawable.
     */
    @Nullable
    public ComplicationData getComplicationData() {
        return mComplicationRenderer.getComplicationData();
    }

    /** Sets whether the complication should be rendered in ambient mode. */
    public void setInAmbientMode(boolean inAmbientMode) {
        mInAmbientMode = inAmbientMode;
    }

    /** Returns whether the complication is rendered in ambient mode. */
    public boolean isInAmbientMode() {
        return mInAmbientMode;
    }

    /**
     * Sets whether the complication, when rendering in ambient mode, should apply a style suitable
     * for low bit ambient mode.
     */
    public void setLowBitAmbient(boolean lowBitAmbient) {
        mLowBitAmbient = lowBitAmbient;
    }

    /**
     * Returns whether the complication, when rendering in ambient mode, should apply a style
     * suitable for low bit ambient mode.
     */
    public boolean isLowBitAmbient() {
        return mLowBitAmbient;
    }

    /**
     * Sets whether the complication, when rendering in ambient mode, should apply a style suitable
     * for display on devices with burn in protection.
     */
    public void setBurnInProtection(boolean burnInProtection) {
        mBurnInProtection = burnInProtection;
    }

    /**
     * Whether the complication, when rendering in ambient mode, should apply a style suitable for
     * display on devices with burn in protection.
     */
    public boolean isBurnInProtectionOn() {
        return mBurnInProtection;
    }

    /**
     * Sets the current time in mulliseconds since the epoch. This will be used to render
     * {@link ComplicationData} with time dependent text.
     *
     * @param currentTimeMillis time in milliseconds since the epoch
     */
    public void setCurrentTimeMillis(long currentTimeMillis) {
        mCurrentTimeMillis = currentTimeMillis;
    }

    /**
     * Returns the time in milliseconds since the epoch used for rendering {@link ComplicationData}
     * with time dependent text.
     */
    public long getCurrentTimeMillis() {
        return mCurrentTimeMillis;
    }

    /**
     * Sets whether the complication is currently highlighted. This may be called by a watch face
     * when a complication is tapped.
     *
     * <p>If watch face is in ambient mode, highlight will not be visible even if this is set to
     * {@code true}, because it may cause burn-in or power inefficiency.
     */
    public void setHighlighted(boolean isHighlighted) {
        mHighlighted = isHighlighted;
    }

    /**
     * Returns whether the complication is currently highlighted.
     */
    public boolean isHighlighted() {
        return mHighlighted;
    }

    /**
     * Sends the tap action for the complication if tap coordinates are inside the complication
     * bounds.
     *
     * <p>This method will also highlight the complication. The highlight duration is 300
     * milliseconds by default but can be modified using the {@link #setHighlightDuration(long)}
     * method.
     *
     * <p>If {@link ComplicationData} has the type {@link ComplicationData#TYPE_NO_PERMISSION}, this
     * method will launch an intent to request complication permission for the watch face. This will
     * only work if the context set by {@link #getDrawable} or the constructor is an
     * instance of WatchFaceService.
     *
     * @param x X coordinate of the tap relative to screen origin
     * @param y Y coordinate of the tap relative to screen origin
     * @return {@code true} if the action was successful, {@code false} if complication data is not
     * set, the complication has no tap action, the tap action (i.e. {@link
     * android.app.PendingIntent}) is cancelled, or the given x and y are not inside the
     * complication bounds.
     */
    public boolean onTap(@Px int x, @Px int y) {
        if (mComplicationRenderer == null) {
            return false;
        }
        ComplicationData data = mComplicationRenderer.getComplicationData();
        if (data == null) {
            return false;
        }
        if (!data.hasTapAction() && data.getType() != ComplicationData.TYPE_NO_PERMISSION) {
            return false;
        }
        if (!getBounds().contains(x, y)) {
            return false;
        }
        if (data.getType() == ComplicationData.TYPE_NO_PERMISSION) {
            // Check if mContext is an instance of WatchFaceService. We can't use the standard
            // instanceof operator because WatchFaceService is defined in library which depends on
            // this one, hence the reflection hack.
            try {
                if (Class.forName("androidx.wear.watchface.WatchFaceService")
                        .isInstance(mContext)) {
                    mContext.startActivity(
                            ComplicationHelperActivity.createPermissionRequestHelperIntent(
                                    mContext,
                                    new ComponentName(mContext, mContext.getClass()))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } else {
                    return false;
                }
            } catch (ClassNotFoundException e) {
                // If watchFaceServiceClass class isn't found we know mContext can't be an instance
                // of WatchFaceService.
                return false;
            }
        } else {
            try {
                data.getTapAction().send();
            } catch (CanceledException e) {
                return false;
            }
        }
        if (getHighlightDuration() > 0) {
            setHighlighted(true);
            invalidateSelf();
            mMainThreadHandler.removeCallbacks(mUnhighlightRunnable);
            mMainThreadHandler.postDelayed(mUnhighlightRunnable, getHighlightDuration());
        }
        return true;
    }

    /**
     * Sets the duration for the complication to stay highlighted after calling the {@link
     * #onTap(int, int)} method. Default value is 300 milliseconds. Setting highlight duration to 0
     * disables highlighting.
     *
     * @param highlightDurationMillis highlight duration in milliseconds
     */
    public void setHighlightDuration(@IntRange(from = 0) long highlightDurationMillis) {
        if (highlightDurationMillis < 0) {
            throw new IllegalArgumentException("Highlight duration should be non-negative.");
        }
        mHighlightDuration = highlightDurationMillis;
    }

    /** Returns the highlight duration. */
    public long getHighlightDuration() {
        return mHighlightDuration;
    }

    /** Builds styles and syncs them with the complication renderer. */
    void updateStyleIfRequired() {
        if (mActiveStyle.isDirty() || mAmbientStyle.isDirty()) {
            mComplicationRenderer.updateStyle(mActiveStyle, mAmbientStyle);
            mActiveStyle.clearDirtyFlag();
            mAmbientStyle.clearDirtyFlag();
        }
    }

    /**
     * Throws an exception if the context is not set. This method should be called if any of the
     * member methods do a context-dependent job.
     */
    private void assertInitialized() {
        if (mContext == null) {
            throw new IllegalStateException(
                    "ComplicationDrawable does not have a context. Use setContext(Context) to set"
                            + " it first.");
        }
    }

    /** Returns complication style for active mode. */
    @NonNull
    public ComplicationStyle getActiveStyle() {
        return mActiveStyle;
    }

    /** Returns complication style for ambient mode. */
    @NonNull
    public ComplicationStyle getAmbientStyle() {
        return mAmbientStyle;
    }

    /** Returns complication renderer. */
    @Nullable
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public ComplicationRenderer getComplicationRenderer() {
        return mComplicationRenderer;
    }

    @Nullable
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public CharSequence getNoDataText() {
        return mNoDataText;
    }
}
