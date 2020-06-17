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

package androidx.wear.complications.rendering;

import android.annotation.SuppressLint;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.wearable.complications.ComplicationData;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.wear.complications.ComplicationHelperActivity;
import androidx.wear.complications.rendering.ComplicationRenderer.OnInvalidateListener;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
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
@SuppressLint("BanParcelableUsage")
public final class ComplicationDrawable extends Drawable implements Parcelable {

    private static final String FIELD_ACTIVE_STYLE_BUILDER = "active_style_builder";
    private static final String FIELD_AMBIENT_STYLE_BUILDER = "ambient_style_builder";
    private static final String FIELD_NO_DATA_TEXT = "no_data_text";
    private static final String FIELD_HIGHLIGHT_DURATION = "highlight_duration";
    private static final String FIELD_RANGED_VALUE_PROGRESS_HIDDEN = "ranged_value_progress_hidden";
    private static final String FIELD_BOUNDS = "bounds";

    @NonNull
    public static final Creator<ComplicationDrawable> CREATOR =
            new Creator<ComplicationDrawable>() {
                @Override
                public ComplicationDrawable createFromParcel(Parcel source) {
                    return new ComplicationDrawable(source);
                }

                @Override
                public ComplicationDrawable[] newArray(int size) {
                    return new ComplicationDrawable[size];
                }
            };

    /**
     * Constants used to define border styles for complications.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({BORDER_STYLE_NONE, BORDER_STYLE_SOLID, BORDER_STYLE_DASHED})
    @SuppressWarnings("PublicTypedef")
    public @interface BorderStyle {}

    /** Style where the borders are not drawn. */
    public static final int BORDER_STYLE_NONE = 0;

    /** Style where the borders are drawn without any gap. */
    public static final int BORDER_STYLE_SOLID = 1;
    /**
     * Style where the borders are drawn as dashed lines. If this is set as current border style,
     * dash width and dash gap should also be set via {@link #setBorderDashWidthActive(int)}, {@link
     * #setBorderDashGapActive(int)}, {@link #setBorderDashWidthAmbient(int)}, {@link
     * #setBorderDashGapAmbient(int)} or XML attributes, or default values will be used.
     */
    public static final int BORDER_STYLE_DASHED = 2;

    private Context mContext;
    private ComplicationRenderer mComplicationRenderer;

    private final ComplicationStyle.Builder mActiveStyleBuilder;
    private final ComplicationStyle.Builder mAmbientStyleBuilder;

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
    private boolean mIsStyleUpToDate;
    private boolean mRangedValueProgressHidden;

    private boolean mIsInflatedFromXml;
    private boolean mAlreadyStyled;

    /** Default constructor. */
    public ComplicationDrawable() {
        mActiveStyleBuilder = new ComplicationStyle.Builder();
        mAmbientStyleBuilder = new ComplicationStyle.Builder();
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
        mActiveStyleBuilder = new ComplicationStyle.Builder(drawable.mActiveStyleBuilder);
        mAmbientStyleBuilder = new ComplicationStyle.Builder(drawable.mAmbientStyleBuilder);
        mNoDataText = drawable.mNoDataText.subSequence(0, drawable.mNoDataText.length());
        mHighlightDuration = drawable.mHighlightDuration;
        mRangedValueProgressHidden = drawable.mRangedValueProgressHidden;
        setBounds(drawable.getBounds());

        mAlreadyStyled = true;
    }

    ComplicationDrawable(@NonNull Parcel in) {
        Bundle bundle = in.readBundle(getClass().getClassLoader());

        mActiveStyleBuilder = bundle.getParcelable(FIELD_ACTIVE_STYLE_BUILDER);
        mAmbientStyleBuilder = bundle.getParcelable(FIELD_AMBIENT_STYLE_BUILDER);
        mNoDataText = bundle.getCharSequence(FIELD_NO_DATA_TEXT);
        mHighlightDuration = bundle.getLong(FIELD_HIGHLIGHT_DURATION);
        mRangedValueProgressHidden = bundle.getBoolean(FIELD_RANGED_VALUE_PROGRESS_HIDDEN);
        setBounds(bundle.<Rect>getParcelable(FIELD_BOUNDS));

        mAlreadyStyled = true;
    }

    /**
     * Creates a ComplicationDrawable from a resource.
     *
     * @param context The {@link Context} to load the resource from
     * @param id The id of the resource to load
     * @return The {@link ComplicationDrawable} loaded from the specified resource id or null if it
     *     doesn't exist.
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

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Bundle bundle = new Bundle();

        bundle.putParcelable(FIELD_ACTIVE_STYLE_BUILDER, mActiveStyleBuilder);
        bundle.putParcelable(FIELD_AMBIENT_STYLE_BUILDER, mAmbientStyleBuilder);
        bundle.putCharSequence(FIELD_NO_DATA_TEXT, mNoDataText);
        bundle.putLong(FIELD_HIGHLIGHT_DURATION, mHighlightDuration);
        bundle.putBoolean(FIELD_RANGED_VALUE_PROGRESS_HIDDEN, mRangedValueProgressHidden);
        bundle.putParcelable(FIELD_BOUNDS, getBounds());

        dest.writeBundle(bundle);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** Sets the style to default values using resources. */
    private static void setStyleToDefaultValues(
            ComplicationStyle.Builder styleBuilder, Resources r) {
        styleBuilder.setBackgroundColor(
                r.getColor(R.color.complicationDrawable_backgroundColor, null));
        styleBuilder.setTextColor(r.getColor(R.color.complicationDrawable_textColor, null));
        styleBuilder.setTitleColor(r.getColor(R.color.complicationDrawable_titleColor, null));
        styleBuilder.setTextTypeface(
                Typeface.create(
                        r.getString(R.string.complicationDrawable_textTypeface), Typeface.NORMAL));
        styleBuilder.setTitleTypeface(
                Typeface.create(
                        r.getString(R.string.complicationDrawable_titleTypeface), Typeface.NORMAL));
        styleBuilder.setTextSize(r.getDimensionPixelSize(R.dimen.complicationDrawable_textSize));
        styleBuilder.setTitleSize(r.getDimensionPixelSize(R.dimen.complicationDrawable_titleSize));
        styleBuilder.setIconColor(r.getColor(R.color.complicationDrawable_iconColor, null));
        styleBuilder.setBorderColor(r.getColor(R.color.complicationDrawable_borderColor, null));
        styleBuilder.setBorderWidth(
                r.getDimensionPixelSize(R.dimen.complicationDrawable_borderWidth));
        styleBuilder.setBorderRadius(
                r.getDimensionPixelSize(R.dimen.complicationDrawable_borderRadius));
        styleBuilder.setBorderStyle(r.getInteger(R.integer.complicationDrawable_borderStyle));
        styleBuilder.setBorderDashWidth(
                r.getDimensionPixelSize(R.dimen.complicationDrawable_borderDashWidth));
        styleBuilder.setBorderDashGap(
                r.getDimensionPixelSize(R.dimen.complicationDrawable_borderDashGap));
        styleBuilder.setRangedValueRingWidth(
                r.getDimensionPixelSize(R.dimen.complicationDrawable_rangedValueRingWidth));
        styleBuilder.setRangedValuePrimaryColor(
                r.getColor(R.color.complicationDrawable_rangedValuePrimaryColor, null));
        styleBuilder.setRangedValueSecondaryColor(
                r.getColor(R.color.complicationDrawable_rangedValueSecondaryColor, null));
        styleBuilder.setHighlightColor(
                r.getColor(R.color.complicationDrawable_highlightColor, null));
    }

    /**
     * Sets the context used to render the complication. If a context is not set,
     * ComplicationDrawable will throw an {@link IllegalStateException} if one of {@link
     * #draw(Canvas)}, {@link #draw(Canvas, long)}, {@link #setBounds(Rect)}, or {@link
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
            setStyleToDefaultValues(mActiveStyleBuilder, context.getResources());
            setStyleToDefaultValues(mAmbientStyleBuilder, context.getResources());
        }

        if (!mAlreadyStyled) {
            mHighlightDuration = context.getResources()
                    .getInteger(R.integer.complicationDrawable_highlightDurationMs);
        }

        mComplicationRenderer =
                new ComplicationRenderer(
                        mContext, mActiveStyleBuilder.build(), mAmbientStyleBuilder.build());
        mComplicationRenderer.setOnInvalidateListener(mRendererInvalidateListener);
        if (mNoDataText == null) {
            setNoDataText(context.getString(R.string.complicationDrawable_noDataText));
        } else {
            mComplicationRenderer.setNoDataText(mNoDataText);
        }
        mComplicationRenderer.setRangedValueProgressHidden(mRangedValueProgressHidden);
        mComplicationRenderer.setBounds(getBounds());
        mIsStyleUpToDate = true;
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
        ComplicationStyle.Builder currentBuilder = getComplicationStyleBuilder(isAmbient);
        if (a.hasValue(R.styleable.ComplicationDrawable_backgroundColor)) {
            currentBuilder.setBackgroundColor(
                    a.getColor(
                            R.styleable.ComplicationDrawable_backgroundColor,
                            r.getColor(R.color.complicationDrawable_backgroundColor, null)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_backgroundDrawable)) {
            currentBuilder.setBackgroundDrawable(
                    a.getDrawable(R.styleable.ComplicationDrawable_backgroundDrawable));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_textColor)) {
            currentBuilder.setTextColor(
                    a.getColor(
                            R.styleable.ComplicationDrawable_textColor,
                            r.getColor(R.color.complicationDrawable_textColor, null)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_titleColor)) {
            currentBuilder.setTitleColor(
                    a.getColor(
                            R.styleable.ComplicationDrawable_titleColor,
                            r.getColor(R.color.complicationDrawable_titleColor, null)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_textTypeface)) {
            currentBuilder.setTextTypeface(
                    Typeface.create(
                            a.getString(R.styleable.ComplicationDrawable_textTypeface),
                            Typeface.NORMAL));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_titleTypeface)) {
            currentBuilder.setTitleTypeface(
                    Typeface.create(
                            a.getString(R.styleable.ComplicationDrawable_titleTypeface),
                            Typeface.NORMAL));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_textSize)) {
            currentBuilder.setTextSize(
                    a.getDimensionPixelSize(
                            R.styleable.ComplicationDrawable_textSize,
                            r.getDimensionPixelSize(R.dimen.complicationDrawable_textSize)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_titleSize)) {
            currentBuilder.setTitleSize(
                    a.getDimensionPixelSize(
                            R.styleable.ComplicationDrawable_titleSize,
                            r.getDimensionPixelSize(R.dimen.complicationDrawable_titleSize)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_iconColor)) {
            currentBuilder.setIconColor(
                    a.getColor(
                            R.styleable.ComplicationDrawable_iconColor,
                            r.getColor(R.color.complicationDrawable_iconColor, null)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_borderColor)) {
            currentBuilder.setBorderColor(
                    a.getColor(
                            R.styleable.ComplicationDrawable_borderColor,
                            r.getColor(R.color.complicationDrawable_borderColor, null)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_borderRadius)) {
            currentBuilder.setBorderRadius(
                    a.getDimensionPixelSize(
                            R.styleable.ComplicationDrawable_borderRadius,
                            r.getDimensionPixelSize(R.dimen.complicationDrawable_borderRadius)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_borderStyle)) {
            currentBuilder.setBorderStyle(
                    a.getInt(
                            R.styleable.ComplicationDrawable_borderStyle,
                            r.getInteger(R.integer.complicationDrawable_borderStyle)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_borderDashWidth)) {
            currentBuilder.setBorderDashWidth(
                    a.getDimensionPixelSize(
                            R.styleable.ComplicationDrawable_borderDashWidth,
                            r.getDimensionPixelSize(R.dimen.complicationDrawable_borderDashWidth)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_borderDashGap)) {
            currentBuilder.setBorderDashGap(
                    a.getDimensionPixelSize(
                            R.styleable.ComplicationDrawable_borderDashGap,
                            r.getDimensionPixelSize(R.dimen.complicationDrawable_borderDashGap)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_borderWidth)) {
            currentBuilder.setBorderWidth(
                    a.getDimensionPixelSize(
                            R.styleable.ComplicationDrawable_borderWidth,
                            r.getDimensionPixelSize(R.dimen.complicationDrawable_borderWidth)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_rangedValueRingWidth)) {
            currentBuilder.setRangedValueRingWidth(
                    a.getDimensionPixelSize(
                            R.styleable.ComplicationDrawable_rangedValueRingWidth,
                            r.getDimensionPixelSize(
                                    R.dimen.complicationDrawable_rangedValueRingWidth)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_rangedValuePrimaryColor)) {
            currentBuilder.setRangedValuePrimaryColor(
                    a.getColor(
                            R.styleable.ComplicationDrawable_rangedValuePrimaryColor,
                            r.getColor(
                                    R.color.complicationDrawable_rangedValuePrimaryColor, null)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_rangedValueSecondaryColor)) {
            currentBuilder.setRangedValueSecondaryColor(
                    a.getColor(
                            R.styleable.ComplicationDrawable_rangedValueSecondaryColor,
                            r.getColor(
                                    R.color.complicationDrawable_rangedValueSecondaryColor, null)));
        }
        if (a.hasValue(R.styleable.ComplicationDrawable_highlightColor)) {
            currentBuilder.setHighlightColor(
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
     * @param r Resources used to resolve attribute values
     * @param parser XML parser from which to inflate this ComplicationDrawable
     * @param attrs Base set of attribute values
     * @param theme Ignored by ComplicationDrawable
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
        setStyleToDefaultValues(mActiveStyleBuilder, r);
        setStyleToDefaultValues(mAmbientStyleBuilder, r);
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
        mIsStyleUpToDate = false;
    }

    /**
     * Draws the complication into bounds set via {@link #setBounds(Rect)} for the given time.
     * Calling this method is equivalent to calling {@link #setCurrentTimeMillis(long)} followed by
     * {@link #draw(Canvas)}, so it will update the last known time and any future calls to {@link
     * #draw(Canvas)} will use the time passed to this method.
     *
     * @param canvas Canvas for the complication to be drawn onto
     * @param currentTimeMillis The time complication is drawn at in milliseconds
     */
    public void draw(@NonNull Canvas canvas, long currentTimeMillis) {
        assertInitialized();
        setCurrentTimeMillis(currentTimeMillis);
        draw(canvas);
    }

    /**
     * Draws the complication for the last known time. Last known time is derived from either {@link
     * ComplicationDrawable#draw(Canvas, long)} or {@link
     * ComplicationDrawable#setCurrentTimeMillis(long)} depending on which was called most recently.
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
     * Does nothing. Use {@link #setImageColorFilterActive(ColorFilter)} or {@link
     * #setImageColorFilterAmbient(ColorFilter)} instead to apply color filter to small and large
     * images.
     */
    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        // No op.
    }

    /**
     *  @inheritDoc
     *  @deprecated This method is no longer used in graphics optimizations
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
     *     otherwise
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_rangedValueProgressHidden
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
     * will be drawn when {@link #draw(Canvas)} or {@link #draw(Canvas, long)} is called.
     */
    public void setComplicationData(@Nullable ComplicationData complicationData) {
        assertInitialized();
        mComplicationRenderer.setComplicationData(complicationData);
    }

    /** Sets whether the complication should be rendered in ambient mode. */
    public void setInAmbientMode(boolean inAmbientMode) {
        mInAmbientMode = inAmbientMode;
    }

    /**
     * Sets whether the complication, when rendering in ambient mode, should apply a style suitable
     * low bit ambient mode.
     */
    public void setLowBitAmbient(boolean lowBitAmbient) {
        mLowBitAmbient = lowBitAmbient;
    }

    /**
     * Sets whether the complication, when rendering in ambient mode, should apply a style suitable
     * for display on devices with burn in protection.
     */
    public void setBurnInProtection(boolean burnInProtection) {
        mBurnInProtection = burnInProtection;
    }

    /**
     * Sets the current time. This will be used to render {@link ComplicationData} with time
     * dependent text.
     *
     * @param currentTimeMillis time in milliseconds
     */
    public void setCurrentTimeMillis(long currentTimeMillis) {
        mCurrentTimeMillis = currentTimeMillis;
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
     * Sets the background color used in active mode.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_backgroundColor
     */
    public void setBackgroundColorActive(int backgroundColor) {
        getComplicationStyleBuilder(false).setBackgroundColor(backgroundColor);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the background drawable used in active mode.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_backgroundDrawable
     */
    public void setBackgroundDrawableActive(@Nullable Drawable drawable) {
        getComplicationStyleBuilder(false).setBackgroundDrawable(drawable);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the text color used in active mode. Text color is used for rendering short text and long
     * text fields.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_textColor
     */
    public void setTextColorActive(int textColor) {
        getComplicationStyleBuilder(false).setTextColor(textColor);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the title color used in active mode. Title color is used for rendering short title and
     * long title fields.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_titleColor
     */
    public void setTitleColorActive(int titleColor) {
        getComplicationStyleBuilder(false).setTitleColor(titleColor);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the color filter used in active mode when rendering large images and small images with
     * style {@link ComplicationData#IMAGE_STYLE_PHOTO}.
     */
    public void setImageColorFilterActive(@Nullable ColorFilter colorFilter) {
        getComplicationStyleBuilder(false).setColorFilter(colorFilter);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the icon color used for tinting icons in active mode.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_iconColor
     */
    public void setIconColorActive(int iconColor) {
        getComplicationStyleBuilder(false).setIconColor(iconColor);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the typeface used in active mode when rendering short text and long text fields.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_textTypeface
     */
    public void setTextTypefaceActive(@Nullable Typeface textTypeface) {
        getComplicationStyleBuilder(false).setTextTypeface(textTypeface);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the typeface used in active mode when rendering short text and long text fields.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_titleTypeface
     */
    public void setTitleTypefaceActive(@Nullable Typeface titleTypeface) {
        getComplicationStyleBuilder(false).setTitleTypeface(titleTypeface);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the text size used in active mode when rendering short text and long text fields.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_textSize
     */
    public void setTextSizeActive(int textSize) {
        getComplicationStyleBuilder(false).setTextSize(textSize);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the text size used in active mode when rendering short title and long title fields.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_titleSize
     */
    public void setTitleSizeActive(int titleSize) {
        getComplicationStyleBuilder(false).setTitleSize(titleSize);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the border color used in active mode.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_borderColor
     */
    public void setBorderColorActive(int borderColor) {
        getComplicationStyleBuilder(false).setBorderColor(borderColor);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the border style used in active mode. It should be one of {@link #BORDER_STYLE_NONE},
     * {@link #BORDER_STYLE_SOLID}, or {@link #BORDER_STYLE_DASHED}.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_borderStyle
     * @see #BORDER_STYLE_NONE
     * @see #BORDER_STYLE_SOLID
     * @see #BORDER_STYLE_DASHED
     */
    public void setBorderStyleActive(@BorderStyle int borderStyle) {
        getComplicationStyleBuilder(false).setBorderStyle(borderStyle);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the dash width used in active mode when drawing borders with style {@link
     * #BORDER_STYLE_DASHED}.
     *
     * @param borderDashWidth Dash width in pixels
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_borderDashWidth
     */
    public void setBorderDashWidthActive(int borderDashWidth) {
        getComplicationStyleBuilder(false).setBorderDashWidth(borderDashWidth);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the dash gap used in active mode when drawing borders with style {@link
     * #BORDER_STYLE_DASHED}.
     *
     * @param borderDashGap Dash gap in pixels
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_borderDashGap
     */
    public void setBorderDashGapActive(int borderDashGap) {
        getComplicationStyleBuilder(false).setBorderDashGap(borderDashGap);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the border radius to be applied to the corners of the bounds of the complication in
     * active mode. Border radius will be limited to the half of width or height, depending on which
     * one is smaller.
     *
     * @param borderRadius Border radius in pixels
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_borderRadius
     */
    public void setBorderRadiusActive(int borderRadius) {
        getComplicationStyleBuilder(false).setBorderRadius(borderRadius);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the border width for active mode.
     *
     * @param borderWidth Border width in pixels
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_borderWidth
     */
    public void setBorderWidthActive(int borderWidth) {
        getComplicationStyleBuilder(false).setBorderWidth(borderWidth);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the stroke width used in active mode when rendering the ranged value indicator.
     *
     * @param rangedValueRingWidth Ring width in pixels
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_rangedValueRingWidth
     */
    public void setRangedValueRingWidthActive(int rangedValueRingWidth) {
        getComplicationStyleBuilder(false).setRangedValueRingWidth(rangedValueRingWidth);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the main color for the ranged value indicator in active mode.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_rangedValuePrimaryColor
     */
    public void setRangedValuePrimaryColorActive(int rangedValuePrimaryColor) {
        getComplicationStyleBuilder(false).setRangedValuePrimaryColor(rangedValuePrimaryColor);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the secondary color for the ranged value indicator in active mode.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_rangedValueSecondaryColor
     */
    public void setRangedValueSecondaryColorActive(int rangedValueSecondaryColor) {
        getComplicationStyleBuilder(false).setRangedValueSecondaryColor(rangedValueSecondaryColor);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the highlight color used in active mode, which is applied when {@link #setHighlighted}
     * is called.
     *
     * @param highlightColor Highlight color
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_highlightColor
     */
    public void setHighlightColorActive(int highlightColor) {
        getComplicationStyleBuilder(false).setHighlightColor(highlightColor);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the background color used in ambient mode.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_backgroundColor
     */
    public void setBackgroundColorAmbient(int backgroundColor) {
        getComplicationStyleBuilder(true).setBackgroundColor(backgroundColor);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the background drawable used in ambient mode.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_backgroundDrawable
     */
    public void setBackgroundDrawableAmbient(@Nullable Drawable drawable) {
        getComplicationStyleBuilder(true).setBackgroundDrawable(drawable);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the text color used in ambient mode. Text color is used for rendering short text and
     * long text fields.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_textColor
     */
    public void setTextColorAmbient(int textColor) {
        getComplicationStyleBuilder(true).setTextColor(textColor);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the title color used in ambient mode. Title color is used for rendering short title and
     * long title fields.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_titleColor
     */
    public void setTitleColorAmbient(int titleColor) {
        getComplicationStyleBuilder(true).setTitleColor(titleColor);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the color filter used in ambient mode when rendering large images and small images with
     * style {@link ComplicationData#IMAGE_STYLE_PHOTO}.
     */
    public void setImageColorFilterAmbient(@Nullable ColorFilter colorFilter) {
        getComplicationStyleBuilder(true).setColorFilter(colorFilter);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the icon color used for tinting icons in ambient mode.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_iconColor
     */
    public void setIconColorAmbient(int iconColor) {
        getComplicationStyleBuilder(true).setIconColor(iconColor);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the typeface used in ambient mode when rendering short text and long text fields.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_textTypeface
     */
    public void setTextTypefaceAmbient(@Nullable Typeface textTypeface) {
        getComplicationStyleBuilder(true).setTextTypeface(textTypeface);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the typeface used in ambient mode when rendering short text and long text fields.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_titleTypeface
     */
    public void setTitleTypefaceAmbient(@Nullable Typeface titleTypeface) {
        getComplicationStyleBuilder(true).setTitleTypeface(titleTypeface);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the text size used in ambient mode when rendering short text and long text fields.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_textSize
     */
    public void setTextSizeAmbient(int textSize) {
        getComplicationStyleBuilder(true).setTextSize(textSize);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the text size used in ambient mode when rendering short title and long title fields.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_titleSize
     */
    public void setTitleSizeAmbient(int titleSize) {
        getComplicationStyleBuilder(true).setTitleSize(titleSize);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the border color used in ambient mode.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_borderColor
     */
    public void setBorderColorAmbient(int borderColor) {
        getComplicationStyleBuilder(true).setBorderColor(borderColor);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the border style used in ambient mode. It should be one of {@link #BORDER_STYLE_NONE},
     * {@link #BORDER_STYLE_SOLID}, or {@link #BORDER_STYLE_DASHED}.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_borderStyle
     * @see #BORDER_STYLE_NONE
     * @see #BORDER_STYLE_SOLID
     * @see #BORDER_STYLE_DASHED
     */
    public void setBorderStyleAmbient(@BorderStyle int borderStyle) {
        getComplicationStyleBuilder(true).setBorderStyle(borderStyle);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the dash width used in ambient mode when drawing borders with style {@link
     * #BORDER_STYLE_DASHED}.
     *
     * @param borderDashWidth Dash width in pixels
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_borderDashWidth
     */
    public void setBorderDashWidthAmbient(int borderDashWidth) {
        getComplicationStyleBuilder(true).setBorderDashWidth(borderDashWidth);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the dash gap used in ambient mode when drawing borders with style {@link
     * #BORDER_STYLE_DASHED}.
     *
     * @param borderDashGap Dash gap in pixels
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_borderDashGap
     */
    public void setBorderDashGapAmbient(int borderDashGap) {
        getComplicationStyleBuilder(true).setBorderDashGap(borderDashGap);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the border radius to be applied to the corners of the bounds of the complication in
     * ambient mode. Border radius will be limited to the half of width or height, depending on
     * which one is smaller.
     *
     * @param borderRadius Border radius in pixels
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_borderRadius
     */
    public void setBorderRadiusAmbient(int borderRadius) {
        getComplicationStyleBuilder(true).setBorderRadius(borderRadius);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the border width for ambient mode.
     *
     * @param borderWidth Border width in pixels
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_borderWidth
     */
    public void setBorderWidthAmbient(int borderWidth) {
        getComplicationStyleBuilder(true).setBorderWidth(borderWidth);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the stroke width used in ambient mode when rendering the ranged value indicator.
     *
     * @param rangedValueRingWidth Ring width in pixels
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_rangedValueRingWidth
     */
    public void setRangedValueRingWidthAmbient(int rangedValueRingWidth) {
        getComplicationStyleBuilder(true).setRangedValueRingWidth(rangedValueRingWidth);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the main color for the ranged value indicator in ambient mode.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_rangedValuePrimaryColor
     */
    public void setRangedValuePrimaryColorAmbient(int rangedValuePrimaryColor) {
        getComplicationStyleBuilder(true).setRangedValuePrimaryColor(rangedValuePrimaryColor);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the secondary color for the ranged value indicator in ambient mode.
     *
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_rangedValueSecondaryColor
     */
    public void setRangedValueSecondaryColorAmbient(int rangedValueSecondaryColor) {
        getComplicationStyleBuilder(true).setRangedValueSecondaryColor(rangedValueSecondaryColor);
        mIsStyleUpToDate = false;
    }

    /**
     * Sets the highlight color used in ambient mode, which is applied when {@link
     * #setHighlighted} is called.
     *
     * @param highlightColor Highlight color
     * @attr ref android.support.wearable.R.styleable#ComplicationDrawable_highlightColor
     */
    public void setHighlightColorAmbient(int highlightColor) {
        getComplicationStyleBuilder(true).setHighlightColor(highlightColor);
        mIsStyleUpToDate = false;
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
     * instance of androidx.wear.watchface.WatchFaceService.
     *
     * @param x X coordinate of the tap relative to screen origin
     * @param y Y coordinate of the tap relative to screen origin
     * @return {@code true} if the action was successful, {@code false} if complication data is not
     *     set, the complication has no tap action, the tap action (i.e. {@link
     *     android.app.PendingIntent}) is cancelled, or the given x and y are not inside the
     *     complication bounds.
     */
    public boolean onTap(int x, int y) {
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

    /** Returns {@code true} if the complication is highlighted. */
    public boolean getHighlighted() {
        return mHighlighted;
    }

    /**
     * Sets the duration for the complication to stay highlighted after calling the {@link
     * #onTap(int, int)} method. Default value is 300 milliseconds. Setting highlight duration to 0
     * disables highlighting.
     *
     * @param highlightDurationMillis highlight duration in milliseconds
     */
    public void setHighlightDuration(long highlightDurationMillis) {
        if (highlightDurationMillis < 0) {
            throw new IllegalArgumentException("Highlight duration should be non-negative.");
        }
        mHighlightDuration = highlightDurationMillis;
    }

    /** Returns the highlight duration. */
    public long getHighlightDuration() {
        return mHighlightDuration;
    }

    private ComplicationStyle.Builder getComplicationStyleBuilder(boolean isAmbient) {
        return isAmbient ? mAmbientStyleBuilder : mActiveStyleBuilder;
    }

    /** Builds styles and syncs them with the complication renderer. */
    private void updateStyleIfRequired() {
        if (!mIsStyleUpToDate) {
            mComplicationRenderer.updateStyle(
                    mActiveStyleBuilder.build(), mAmbientStyleBuilder.build());
            mIsStyleUpToDate = true;
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
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public ComplicationStyle getActiveStyle() {
        return mActiveStyleBuilder.build();
    }

    /** Returns complication style for ambient mode. */
    @NonNull
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public ComplicationStyle getAmbientStyle() {
        return mAmbientStyleBuilder.build();
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
