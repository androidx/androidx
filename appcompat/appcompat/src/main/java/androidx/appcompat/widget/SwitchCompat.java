/*
 * Copyright (C) 2014 The Android Open Source Project
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

package androidx.appcompat.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.InputFilter;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.util.Property;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.FloatRange;
import androidx.appcompat.R;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.text.AllCapsTransformationMethod;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.TextViewCompat;
import androidx.emoji2.text.EmojiCompat;
import androidx.resourceinspection.annotation.Attribute;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * SwitchCompat is a complete backport of the core {@link Switch} widget that
 * brings the visuals and functionality of the toggle widget to older versions
 * of the Android platform. Unlike other widgets in this package, SwitchCompat
 * is not automatically used in layouts that include the
 * <code>&lt;Switch&gt;</code> element. Instead, you need to explicitly use
 * <code>&lt;androidx.appcompat.widget.SwitchCompat&gt;</code> and the matching
 * attributes in your layouts.
 *
 * <p>The thumb can be tinted with {@link #setThumbTintList(ColorStateList)} and
 * {@link #setThumbTintMode(PorterDuff.Mode)} APIs as well as with the matching
 * XML attributes. The track can be tinted with
 * {@link #setTrackTintList(ColorStateList)} and
 * {@link #setTrackTintMode(PorterDuff.Mode)} APIs as well as with the matching
 * XML attributes.</p>
 *
 * <p>Supported attributes include:</p>
 * <ul>
 *    <li>{@link android.R.attr#textOn}</li>
 *    <li>{@link android.R.attr#textOff}</li>
 *    <li>{@link android.R.attr#switchMinWidth}</li>
 *    <li>{@link android.R.attr#switchPadding}</li>
 *    <li>{@link android.R.attr#switchTextAppearance}</li>
 *    <li>{@link android.R.attr#thumb}</li>
 *    <li>{@link android.R.attr#thumbTextPadding}</li>
 *    <li>{@link android.R.attr#track}</li>
 *    <li>{@link android.R.attr#thumbTint}</li>
 *    <li>{@link android.R.attr#thumbTintMode}</li>
 *    <li>{@link android.R.attr#trackTint}</li>
 *    <li>{@link android.R.attr#trackTintMode}</li>
 * </ul>
 *
 * <p>For more information, see the
 * <a href="{@docRoot}guide/topics/ui/controls/togglebutton.html">
 * Toggle Buttons</a> guide.</p>
 */
public class SwitchCompat extends CompoundButton implements EmojiCompatConfigurationView {
    private static final int THUMB_ANIMATION_DURATION = 250;

    private static final int TOUCH_MODE_IDLE = 0;
    private static final int TOUCH_MODE_DOWN = 1;
    private static final int TOUCH_MODE_DRAGGING = 2;

    // We force the accessibility events to have a class name of Switch, since screen readers
    // already know how to handle their events
    private static final String ACCESSIBILITY_EVENT_CLASS_NAME = "android.widget.Switch";

    // Enum for the "typeface" XML parameter.
    private static final int SANS = 1;
    private static final int SERIF = 2;
    private static final int MONOSPACE = 3;

    private static final Property<SwitchCompat, Float> THUMB_POS =
            new Property<SwitchCompat, Float>(Float.class, "thumbPos") {
                @Override
                public Float get(SwitchCompat object) {
                    return object.mThumbPosition;
                }

                @Override
                public void set(SwitchCompat object, Float value) {
                    object.setThumbPosition(value);
                }
            };

    private Drawable mThumbDrawable;
    private ColorStateList mThumbTintList = null;
    private PorterDuff.Mode mThumbTintMode = null;
    private boolean mHasThumbTint = false;
    private boolean mHasThumbTintMode = false;

    private Drawable mTrackDrawable;
    private ColorStateList mTrackTintList = null;
    private PorterDuff.Mode mTrackTintMode = null;
    private boolean mHasTrackTint = false;
    private boolean mHasTrackTintMode = false;

    private int mThumbTextPadding;
    private int mSwitchMinWidth;
    private int mSwitchPadding;
    private boolean mSplitTrack;
    private CharSequence mTextOn;
    private CharSequence mTextOnTransformed;
    private CharSequence mTextOff;
    private CharSequence mTextOffTransformed;
    private boolean mShowText;

    private int mTouchMode;
    private int mTouchSlop;
    private float mTouchX;
    private float mTouchY;
    private VelocityTracker mVelocityTracker = VelocityTracker.obtain();
    private int mMinFlingVelocity;

    float mThumbPosition;

    /**
     * Width required to draw the switch track and thumb. Includes padding and
     * optical bounds for both the track and thumb.
     */
    private int mSwitchWidth;

    /**
     * Height required to draw the switch track and thumb. Includes padding and
     * optical bounds for both the track and thumb.
     */
    private int mSwitchHeight;

    /**
     * Width of the thumb's content region. Does not include padding or
     * optical bounds.
     */
    private int mThumbWidth;

    /** Left bound for drawing the switch track and thumb. */
    private int mSwitchLeft;

    /** Top bound for drawing the switch track and thumb. */
    private int mSwitchTop;

    /** Right bound for drawing the switch track and thumb. */
    private int mSwitchRight;

    /** Bottom bound for drawing the switch track and thumb. */
    private int mSwitchBottom;

    private boolean mEnforceSwitchWidth = true;

    private final TextPaint mTextPaint;
    private ColorStateList mTextColors;
    private Layout mOnLayout;
    private Layout mOffLayout;
    private @Nullable TransformationMethod mSwitchTransformationMethod;
    ObjectAnimator mPositionAnimator;
    private final AppCompatTextHelper mTextHelper;
    private @NonNull AppCompatEmojiTextHelper mAppCompatEmojiTextHelper;
    private @Nullable EmojiCompatInitCallback mEmojiCompatInitCallback;

    @SuppressWarnings("hiding")
    private final Rect mTempRect = new Rect();

    private static final int[] CHECKED_STATE_SET = {
            android.R.attr.state_checked
    };

    /**
     * Construct a new Switch with default styling.
     *
     * @param context The Context that will determine this widget's theming.
     */
    public SwitchCompat(@NonNull Context context) {
        this(context, null);
    }

    /**
     * Construct a new Switch with default styling, overriding specific style
     * attributes as requested.
     *
     * @param context The Context that will determine this widget's theming.
     * @param attrs Specification of attributes that should deviate from default styling.
     */
    public SwitchCompat(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.switchStyle);
    }

    /**
     * Construct a new Switch with a default style determined by the given theme attribute,
     * overriding specific style attributes as requested.
     *
     * @param context The Context that will determine this widget's theming.
     * @param attrs Specification of attributes that should deviate from the default styling.
     * @param defStyleAttr An attribute in the current theme that contains a
     *        reference to a style resource that supplies default values for
     *        the view. Can be 0 to not look for defaults.
     */
    public SwitchCompat(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        ThemeUtils.checkAppCompatTheme(this, getContext());

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

        final Resources res = getResources();
        mTextPaint.density = res.getDisplayMetrics().density;

        final TintTypedArray a = TintTypedArray.obtainStyledAttributes(context,
                attrs, R.styleable.SwitchCompat, defStyleAttr, 0);
        ViewCompat.saveAttributeDataForStyleable(this,
                context, R.styleable.SwitchCompat, attrs,
                a.getWrappedTypeArray(), defStyleAttr, 0);

        mThumbDrawable = a.getDrawable(R.styleable.SwitchCompat_android_thumb);
        if (mThumbDrawable != null) {
            mThumbDrawable.setCallback(this);
        }
        mTrackDrawable = a.getDrawable(R.styleable.SwitchCompat_track);
        if (mTrackDrawable != null) {
            mTrackDrawable.setCallback(this);
        }
        setTextOnInternal(a.getText(R.styleable.SwitchCompat_android_textOn));
        setTextOffInternal(a.getText(R.styleable.SwitchCompat_android_textOff));
        mShowText = a.getBoolean(R.styleable.SwitchCompat_showText, true);
        mThumbTextPadding = a.getDimensionPixelSize(
                R.styleable.SwitchCompat_thumbTextPadding, 0);
        mSwitchMinWidth = a.getDimensionPixelSize(
                R.styleable.SwitchCompat_switchMinWidth, 0);
        mSwitchPadding = a.getDimensionPixelSize(
                R.styleable.SwitchCompat_switchPadding, 0);
        mSplitTrack = a.getBoolean(R.styleable.SwitchCompat_splitTrack, false);

        ColorStateList thumbTintList = a.getColorStateList(R.styleable.SwitchCompat_thumbTint);
        if (thumbTintList != null) {
            mThumbTintList = thumbTintList;
            mHasThumbTint = true;
        }
        PorterDuff.Mode thumbTintMode = DrawableUtils.parseTintMode(
                a.getInt(R.styleable.SwitchCompat_thumbTintMode, -1), null);
        if (mThumbTintMode != thumbTintMode) {
            mThumbTintMode = thumbTintMode;
            mHasThumbTintMode = true;
        }
        if (mHasThumbTint || mHasThumbTintMode) {
            applyThumbTint();
        }

        ColorStateList trackTintList = a.getColorStateList(R.styleable.SwitchCompat_trackTint);
        if (trackTintList != null) {
            mTrackTintList = trackTintList;
            mHasTrackTint = true;
        }
        PorterDuff.Mode trackTintMode = DrawableUtils.parseTintMode(
                a.getInt(R.styleable.SwitchCompat_trackTintMode, -1), null);
        if (mTrackTintMode != trackTintMode) {
            mTrackTintMode = trackTintMode;
            mHasTrackTintMode = true;
        }
        if (mHasTrackTint || mHasTrackTintMode) {
            applyTrackTint();
        }

        final int appearance = a.getResourceId(
                R.styleable.SwitchCompat_switchTextAppearance, 0);
        if (appearance != 0) {
            setSwitchTextAppearance(context, appearance);
        }

        mTextHelper = new AppCompatTextHelper(this);
        mTextHelper.loadFromAttributes(attrs, defStyleAttr);

        a.recycle();

        final ViewConfiguration config = ViewConfiguration.get(context);
        mTouchSlop = config.getScaledTouchSlop();
        mMinFlingVelocity = config.getScaledMinimumFlingVelocity();

        AppCompatEmojiTextHelper emojiTextViewHelper = getEmojiTextViewHelper();
        emojiTextViewHelper.loadFromAttributes(attrs, defStyleAttr);

        // Refresh display with current params
        refreshDrawableState();
        setChecked(isChecked());
    }

    /**
     * Sets the switch text color, size, style, hint color, and highlight color
     * from the specified TextAppearance resource.
     *
     * @see android.R.attr#switchTextAppearance
     */
    public void setSwitchTextAppearance(Context context, int resid) {
        final TintTypedArray appearance = TintTypedArray.obtainStyledAttributes(context, resid,
                R.styleable.TextAppearance);

        ColorStateList colors;
        int ts;

        colors = appearance.getColorStateList(R.styleable.TextAppearance_android_textColor);
        if (colors != null) {
            mTextColors = colors;
        } else {
            // If no color set in TextAppearance, default to the view's textColor
            mTextColors = getTextColors();
        }

        ts = appearance.getDimensionPixelSize(R.styleable.TextAppearance_android_textSize, 0);
        if (ts != 0) {
            if (ts != mTextPaint.getTextSize()) {
                mTextPaint.setTextSize(ts);
                requestLayout();
            }
        }

        int typefaceIndex, styleIndex;
        typefaceIndex = appearance.getInt(R.styleable.TextAppearance_android_typeface, -1);
        styleIndex = appearance.getInt(R.styleable.TextAppearance_android_textStyle, -1);

        setSwitchTypefaceByIndex(typefaceIndex, styleIndex);

        boolean allCaps = appearance.getBoolean(R.styleable.TextAppearance_textAllCaps, false);
        if (allCaps) {
            mSwitchTransformationMethod = new AllCapsTransformationMethod(getContext());
        } else {
            mSwitchTransformationMethod = null;
        }
        // apply the new transform to current text
        setTextOnInternal(mTextOn);
        setTextOffInternal(mTextOff);

        appearance.recycle();
    }

    private void setSwitchTypefaceByIndex(int typefaceIndex, int styleIndex) {
        Typeface tf = null;
        switch (typefaceIndex) {
            case SANS:
                tf = Typeface.SANS_SERIF;
                break;

            case SERIF:
                tf = Typeface.SERIF;
                break;

            case MONOSPACE:
                tf = Typeface.MONOSPACE;
                break;
        }

        setSwitchTypeface(tf, styleIndex);
    }

    /**
     * Sets the typeface and style in which the text should be displayed on the
     * switch, and turns on the fake bold and italic bits in the Paint if the
     * Typeface that you provided does not have all the bits in the
     * style that you specified.
     */
    public void setSwitchTypeface(Typeface tf, int style) {
        if (style > 0) {
            if (tf == null) {
                tf = Typeface.defaultFromStyle(style);
            } else {
                tf = Typeface.create(tf, style);
            }

            setSwitchTypeface(tf);
            // now compute what (if any) algorithmic styling is needed
            int typefaceStyle = tf != null ? tf.getStyle() : 0;
            int need = style & ~typefaceStyle;
            mTextPaint.setFakeBoldText((need & Typeface.BOLD) != 0);
            mTextPaint.setTextSkewX((need & Typeface.ITALIC) != 0 ? -0.25f : 0);
        } else {
            mTextPaint.setFakeBoldText(false);
            mTextPaint.setTextSkewX(0);
            setSwitchTypeface(tf);
        }
    }

    /**
     * Sets the typeface in which the text should be displayed on the switch.
     * Note that not all Typeface families actually have bold and italic
     * variants, so you may need to use
     * {@link #setSwitchTypeface(Typeface, int)} to get the appearance
     * that you actually want.
     */
    public void setSwitchTypeface(Typeface typeface) {
        if ((mTextPaint.getTypeface() != null && !mTextPaint.getTypeface().equals(typeface))
                || (mTextPaint.getTypeface() == null && typeface != null)) {
            mTextPaint.setTypeface(typeface);

            requestLayout();
            invalidate();
        }
    }

    /**
     * Set the amount of horizontal padding between the switch and the associated text.
     *
     * @param pixels Amount of padding in pixels
     *
     * @see android.R.attr#switchPadding
     */
    public void setSwitchPadding(int pixels) {
        mSwitchPadding = pixels;
        requestLayout();
    }

    /**
     * Get the amount of horizontal padding between the switch and the associated text.
     *
     * @return Amount of padding in pixels
     *
     * @see android.R.attr#switchPadding
     */
    @Attribute("androidx.appcompat:switchPadding")
    public int getSwitchPadding() {
        return mSwitchPadding;
    }

    /**
     * Set the minimum width of the switch in pixels. The switch's width will be the maximum
     * of this value and its measured width as determined by the switch drawables and text used.
     *
     * @param pixels Minimum width of the switch in pixels
     *
     * @see android.R.attr#switchMinWidth
     */
    public void setSwitchMinWidth(int pixels) {
        mSwitchMinWidth = pixels;
        requestLayout();
    }

    /**
     * Get the minimum width of the switch in pixels. The switch's width will be the maximum
     * of this value and its measured width as determined by the switch drawables and text used.
     *
     * @return Minimum width of the switch in pixels
     *
     * @see android.R.attr#switchMinWidth
     */
    @Attribute("androidx.appcompat:switchMinWidth")
    public int getSwitchMinWidth() {
        return mSwitchMinWidth;
    }

    /**
     * Set the horizontal padding around the text drawn on the switch itself.
     *
     * @param pixels Horizontal padding for switch thumb text in pixels
     *
     * @see android.R.attr#thumbTextPadding
     */
    public void setThumbTextPadding(int pixels) {
        mThumbTextPadding = pixels;
        requestLayout();
    }

    /**
     * Get the horizontal padding around the text drawn on the switch itself.
     *
     * @return Horizontal padding for switch thumb text in pixels
     *
     * @see android.R.attr#thumbTextPadding
     */
    @Attribute("androidx.appcompat:thumbTextPadding")
    public int getThumbTextPadding() {
        return mThumbTextPadding;
    }

    /**
     * Set the drawable used for the track that the switch slides within.
     *
     * @param track Track drawable
     *
     * @see android.R.attr#track
     */
    public void setTrackDrawable(Drawable track) {
        if (mTrackDrawable != null) {
            mTrackDrawable.setCallback(null);
        }
        mTrackDrawable = track;
        if (track != null) {
            track.setCallback(this);
        }
        requestLayout();
    }

    /**
     * Set the drawable used for the track that the switch slides within.
     *
     * @param resId Resource ID of a track drawable
     *
     * @see android.R.attr#track
     */
    public void setTrackResource(int resId) {
        setTrackDrawable(AppCompatResources.getDrawable(getContext(), resId));
    }

    /**
     * Get the drawable used for the track that the switch slides within.
     *
     * @return Track drawable
     *
     * @see android.R.attr#track
     */
    @Attribute("androidx.appcompat:track")
    public Drawable getTrackDrawable() {
        return mTrackDrawable;
    }

    /**
     * Applies a tint to the track drawable. Does not modify the current
     * tint mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link #setTrackDrawable(Drawable)} will
     * automatically mutate the drawable and apply the specified tint and tint
     * mode using {@link DrawableCompat#setTintList(Drawable, ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     *
     * @see #getTrackTintList()
     * @see android.R.attr#trackTint
     */
    public void setTrackTintList(@Nullable ColorStateList tint) {
        mTrackTintList = tint;
        mHasTrackTint = true;

        applyTrackTint();
    }

    /**
     * @return the tint applied to the track drawable
     *
     * @see #setTrackTintList(ColorStateList)
     * @see android.R.attr#trackTint
     */
    @Attribute("androidx.appcompat:trackTint")
    public @Nullable ColorStateList getTrackTintList() {
        return mTrackTintList;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setTrackTintList(ColorStateList)} to the track drawable.
     * The default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     *
     * @see #getTrackTintMode()
     * @see android.R.attr#trackTintMode
     */
    public void setTrackTintMode(PorterDuff.@Nullable Mode tintMode) {
        mTrackTintMode = tintMode;
        mHasTrackTintMode = true;

        applyTrackTint();
    }

    /**
     * @return the blending mode used to apply the tint to the track
     *         drawable
     *
     * @see #setTrackTintMode(PorterDuff.Mode)
     * @see android.R.attr#trackTintMode
     */
    @Attribute("androidx.appcompat:trackTintMode")
    public PorterDuff.@Nullable Mode getTrackTintMode() {
        return mTrackTintMode;
    }

    private void applyTrackTint() {
        if (mTrackDrawable != null && (mHasTrackTint || mHasTrackTintMode)) {
            mTrackDrawable = DrawableCompat.wrap(mTrackDrawable).mutate();

            if (mHasTrackTint) {
                DrawableCompat.setTintList(mTrackDrawable, mTrackTintList);
            }

            if (mHasTrackTintMode) {
                DrawableCompat.setTintMode(mTrackDrawable, mTrackTintMode);
            }

            // The drawable (or one of its children) may not have been
            // stateful before applying the tint, so let's try again.
            if (mTrackDrawable.isStateful()) {
                mTrackDrawable.setState(getDrawableState());
            }
        }
    }

    /**
     * Set the drawable used for the switch "thumb" - the piece that the user
     * can physically touch and drag along the track.
     *
     * @param thumb Thumb drawable
     *
     * @see android.R.attr#thumb
     */
    public void setThumbDrawable(Drawable thumb) {
        if (mThumbDrawable != null) {
            mThumbDrawable.setCallback(null);
        }
        mThumbDrawable = thumb;
        if (thumb != null) {
            thumb.setCallback(this);
        }
        requestLayout();
    }

    /**
     * Set the drawable used for the switch "thumb" - the piece that the user
     * can physically touch and drag along the track.
     *
     * @param resId Resource ID of a thumb drawable
     *
     * @see android.R.attr#thumb
     */
    public void setThumbResource(int resId) {
        setThumbDrawable(AppCompatResources.getDrawable(getContext(), resId));
    }

    /**
     * Get the drawable used for the switch "thumb" - the piece that the user
     * can physically touch and drag along the track.
     *
     * @return Thumb drawable
     *
     * @see android.R.attr#thumb
     */
    @Attribute("android:thumb")
    public Drawable getThumbDrawable() {
        return mThumbDrawable;
    }

    /**
     * Applies a tint to the thumb drawable. Does not modify the current
     * tint mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link #setThumbDrawable(Drawable)} will
     * automatically mutate the drawable and apply the specified tint and tint
     * mode using {@link DrawableCompat#setTintList(Drawable, ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     *
     * @see #getThumbTintList()
     * @see android.R.attr#thumbTint
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setThumbTintList(@Nullable ColorStateList tint) {
        mThumbTintList = tint;
        mHasThumbTint = true;

        applyThumbTint();
    }

    /**
     * @return the tint applied to the thumb drawable
     *
     * @see #setThumbTintList(ColorStateList)
     * @see android.R.attr#thumbTint
     */
    @Attribute("androidx.appcompat:thumbTint")
    public @Nullable ColorStateList getThumbTintList() {
        return mThumbTintList;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setThumbTintList(ColorStateList)}} to the thumb drawable.
     * The default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     *
     * @see #getThumbTintMode()
     * @see android.R.attr#thumbTintMode
     * @see Drawable#setTintMode(PorterDuff.Mode)
     */
    public void setThumbTintMode(PorterDuff.@Nullable Mode tintMode) {
        mThumbTintMode = tintMode;
        mHasThumbTintMode = true;

        applyThumbTint();
    }

    /**
     * @return the blending mode used to apply the tint to the thumb
     *         drawable
     *
     * @see #setThumbTintMode(PorterDuff.Mode)
     * @see android.R.attr#thumbTintMode
     */
    @Attribute("androidx.appcompat:thumbTintMode")
    public PorterDuff.@Nullable Mode getThumbTintMode() {
        return mThumbTintMode;
    }

    private void applyThumbTint() {
        if (mThumbDrawable != null && (mHasThumbTint || mHasThumbTintMode)) {
            mThumbDrawable = DrawableCompat.wrap(mThumbDrawable).mutate();

            if (mHasThumbTint) {
                DrawableCompat.setTintList(mThumbDrawable, mThumbTintList);
            }

            if (mHasThumbTintMode) {
                DrawableCompat.setTintMode(mThumbDrawable, mThumbTintMode);
            }

            // The drawable (or one of its children) may not have been
            // stateful before applying the tint, so let's try again.
            if (mThumbDrawable.isStateful()) {
                mThumbDrawable.setState(getDrawableState());
            }
        }
    }

    /**
     * Specifies whether the track should be split by the thumb. When true,
     * the thumb's optical bounds will be clipped out of the track drawable,
     * then the thumb will be drawn into the resulting gap.
     *
     * @param splitTrack Whether the track should be split by the thumb
     *
     * @see android.R.attr#splitTrack
     */
    public void setSplitTrack(boolean splitTrack) {
        mSplitTrack = splitTrack;
        invalidate();
    }

    /**
     * Returns whether the track should be split by the thumb.
     *
     * @see android.R.attr#splitTrack
     */
    @Attribute("androidx.appcompat:splitTrack")
    public boolean getSplitTrack() {
        return mSplitTrack;
    }

    /**
     * Returns the text displayed when the button is in the checked state.
     *
     * @see android.R.attr#textOn
     */
    @Attribute("android:textOn")
    public CharSequence getTextOn() {
        return mTextOn;
    }

    /**
     * Call this whenever setting mTextOn or mTextOnTransformed to ensure we maintain
     * consistent state
     */
    private void setTextOnInternal(CharSequence textOn) {
        mTextOn = textOn;
        mTextOnTransformed = doTransformForOnOffText(textOn);
        mOnLayout = null;
        if (mShowText) {
            setupEmojiCompatLoadCallback();
        }
    }


    /**
     * Sets the text displayed when the button is in the checked state.
     *
     * @see android.R.attr#textOn
     */
    public void setTextOn(CharSequence textOn) {
        setTextOnInternal(textOn);
        requestLayout();
        if (isChecked()) {
            // Default state is derived from on/off-text, so state has to be updated when
            // on/off-text are updated.
            setOnStateDescriptionOnRAndAbove();
        }
    }

    /**
     * Returns the text displayed when the button is not in the checked state.
     *
     * @see android.R.attr#textOff
     */
    @Attribute("android:textOff")
    public CharSequence getTextOff() {
        return mTextOff;
    }

    /**
     * Call this whenever setting mTextOff or mTextOffTransformed to ensure we maintain
     * consistent state
     */
    private void setTextOffInternal(CharSequence textOff) {
        mTextOff = textOff;
        mTextOffTransformed = doTransformForOnOffText(textOff);
        mOffLayout = null;
        if (mShowText) {
            setupEmojiCompatLoadCallback();
        }
    }

    /**
     * Sets the text displayed when the button is not in the checked state.
     *
     * @see android.R.attr#textOff
     */
    public void setTextOff(CharSequence textOff) {
        setTextOffInternal(textOff);
        requestLayout();
        if (!isChecked()) {
            // Default state is derived from on/off-text, so state has to be updated when
            // on/off-text are updated.
            setOffStateDescriptionOnRAndAbove();
        }
    }

    private @Nullable CharSequence doTransformForOnOffText(@Nullable CharSequence onOffText) {
        TransformationMethod transformationMethod =
                getEmojiTextViewHelper().wrapTransformationMethod(mSwitchTransformationMethod);
        return ((transformationMethod != null)
                ? transformationMethod.getTransformation(onOffText, this)
                : onOffText);
    }

    /**
     * Sets whether the on/off text should be displayed.
     *
     * @param showText {@code true} to display on/off text
     *
     * @see android.R.attr#showText
     */
    public void setShowText(boolean showText) {
        if (mShowText != showText) {
            mShowText = showText;
            requestLayout();
            if (showText) {
                setupEmojiCompatLoadCallback();
            }
        }
    }

    /**
     * Indicates whether the on/off text should be displayed.
     *
     * @return {@code true} if the on/off text should be displayed, otherwise
     *     {@code false}
     *
     * @see android.R.attr#showText
     */
    @Attribute("androidx.appcompat:showText")
    public boolean getShowText() {
        return mShowText;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mShowText) {
            if (mOnLayout == null) {
                mOnLayout = makeLayout(mTextOnTransformed);
            }

            if (mOffLayout == null) {
                mOffLayout = makeLayout(mTextOffTransformed);
            }
        }

        final Rect padding = mTempRect;
        final int thumbWidth;
        final int thumbHeight;
        if (mThumbDrawable != null) {
            // Cached thumb width does not include padding.
            mThumbDrawable.getPadding(padding);
            thumbWidth = mThumbDrawable.getIntrinsicWidth() - padding.left - padding.right;
            thumbHeight = mThumbDrawable.getIntrinsicHeight();
        } else {
            thumbWidth = 0;
            thumbHeight = 0;
        }

        final int maxTextWidth;
        if (mShowText) {
            maxTextWidth = Math.max(mOnLayout.getWidth(), mOffLayout.getWidth())
                    + mThumbTextPadding * 2;
        } else {
            maxTextWidth = 0;
        }

        mThumbWidth = Math.max(maxTextWidth, thumbWidth);

        final int trackHeight;
        if (mTrackDrawable != null) {
            mTrackDrawable.getPadding(padding);
            trackHeight = mTrackDrawable.getIntrinsicHeight();
        } else {
            padding.setEmpty();
            trackHeight = 0;
        }

        // Adjust left and right padding to ensure there's enough room for the
        // thumb's padding (when present).
        int paddingLeft = padding.left;
        int paddingRight = padding.right;
        if (mThumbDrawable != null) {
            final Rect inset = DrawableUtils.getOpticalBounds(mThumbDrawable);
            paddingLeft = Math.max(paddingLeft, inset.left);
            paddingRight = Math.max(paddingRight, inset.right);
        }

        final int switchWidth =
                mEnforceSwitchWidth
                        ? Math.max(mSwitchMinWidth, 2 * mThumbWidth + paddingLeft + paddingRight)
                        : mSwitchMinWidth;
        final int switchHeight = Math.max(trackHeight, thumbHeight);
        mSwitchWidth = switchWidth;
        mSwitchHeight = switchHeight;

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int measuredHeight = getMeasuredHeight();
        if (measuredHeight < switchHeight) {
            setMeasuredDimension(getMeasuredWidthAndState(), switchHeight);
        }
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);

        final CharSequence text = isChecked() ? mTextOn : mTextOff;
        if (text != null) {
            event.getText().add(text);
        }
    }

    private Layout makeLayout(CharSequence transformedText) {
        return new StaticLayout(transformedText, mTextPaint,
                transformedText != null
                        ? (int) Math.ceil(Layout.getDesiredWidth(transformedText, mTextPaint)) : 0,
                Layout.Alignment.ALIGN_NORMAL, 1.f, 0, true);
    }

    /**
     * @return true if (x, y) is within the target area of the switch thumb
     */
    private boolean hitThumb(float x, float y) {
        if (mThumbDrawable == null) {
            return false;
        }

        // Relies on mTempRect, MUST be called first!
        final int thumbOffset = getThumbOffset();

        mThumbDrawable.getPadding(mTempRect);
        final int thumbTop = mSwitchTop - mTouchSlop;
        final int thumbLeft = mSwitchLeft + thumbOffset - mTouchSlop;
        final int thumbRight = thumbLeft + mThumbWidth +
                mTempRect.left + mTempRect.right + mTouchSlop;
        final int thumbBottom = mSwitchBottom + mTouchSlop;
        return x > thumbLeft && x < thumbRight && y > thumbTop && y < thumbBottom;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mVelocityTracker.addMovement(ev);
        final int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                if (isEnabled() && hitThumb(x, y)) {
                    mTouchMode = TOUCH_MODE_DOWN;
                    mTouchX = x;
                    mTouchY = y;
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                switch (mTouchMode) {
                    case TOUCH_MODE_IDLE:
                        // Didn't target the thumb, treat normally.
                        break;

                    case TOUCH_MODE_DOWN: {
                        final float x = ev.getX();
                        final float y = ev.getY();
                        if (Math.abs(x - mTouchX) > mTouchSlop ||
                                Math.abs(y - mTouchY) > mTouchSlop) {
                            mTouchMode = TOUCH_MODE_DRAGGING;
                            getParent().requestDisallowInterceptTouchEvent(true);
                            mTouchX = x;
                            mTouchY = y;
                            return true;
                        }
                        break;
                    }

                    case TOUCH_MODE_DRAGGING: {
                        final float x = ev.getX();
                        final int thumbScrollRange = getThumbScrollRange();
                        final float thumbScrollOffset = x - mTouchX;
                        float dPos;
                        if (thumbScrollRange != 0) {
                            dPos = thumbScrollOffset / thumbScrollRange;
                        } else {
                            // If the thumb scroll range is empty, just use the
                            // movement direction to snap on or off.
                            dPos = thumbScrollOffset > 0 ? 1 : -1;
                        }
                        if (ViewUtils.isLayoutRtl(this)) {
                            dPos = -dPos;
                        }
                        final float newPos = constrain(mThumbPosition + dPos, 0, 1);
                        if (newPos != mThumbPosition) {
                            mTouchX = x;
                            setThumbPosition(newPos);
                        }
                        return true;
                    }
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (mTouchMode == TOUCH_MODE_DRAGGING) {
                    stopDrag(ev);
                    // Allow super class to handle pressed state, etc.
                    super.onTouchEvent(ev);
                    return true;
                }
                mTouchMode = TOUCH_MODE_IDLE;
                mVelocityTracker.clear();
                break;
            }
        }

        return super.onTouchEvent(ev);
    }

    private void cancelSuperTouch(MotionEvent ev) {
        MotionEvent cancel = MotionEvent.obtain(ev);
        cancel.setAction(MotionEvent.ACTION_CANCEL);
        super.onTouchEvent(cancel);
        cancel.recycle();
    }

    /**
     * Called from onTouchEvent to end a drag operation.
     *
     * @param ev Event that triggered the end of drag mode - ACTION_UP or ACTION_CANCEL
     */
    private void stopDrag(MotionEvent ev) {
        mTouchMode = TOUCH_MODE_IDLE;

        // Commit the change if the event is up and not canceled and the switch
        // has not been disabled during the drag.
        final boolean commitChange = ev.getAction() == MotionEvent.ACTION_UP && isEnabled();
        final boolean oldState = isChecked();
        final boolean newState;
        if (commitChange) {
            mVelocityTracker.computeCurrentVelocity(1000);
            final float xvel = mVelocityTracker.getXVelocity();
            if (Math.abs(xvel) > mMinFlingVelocity) {
                newState = ViewUtils.isLayoutRtl(this) ? (xvel < 0) : (xvel > 0);
            } else {
                newState = getTargetCheckedState();
            }
        } else {
            newState = oldState;
        }

        if (newState != oldState) {
            playSoundEffect(SoundEffectConstants.CLICK);
        }
        // Always call setChecked so that the thumb is moved back to the correct edge
        setChecked(newState);
        cancelSuperTouch(ev);
    }

    private void animateThumbToCheckedState(final boolean newCheckedState) {
        final float targetPosition = newCheckedState ? 1 : 0;
        mPositionAnimator = ObjectAnimator.ofFloat(this, THUMB_POS, targetPosition);
        mPositionAnimator.setDuration(THUMB_ANIMATION_DURATION);
        mPositionAnimator.setAutoCancel(true);
        mPositionAnimator.start();
    }

    private void cancelPositionAnimator() {
        if (mPositionAnimator != null) {
            mPositionAnimator.cancel();
        }
    }

    private boolean getTargetCheckedState() {
        return mThumbPosition > 0.5f;
    }

    /**
     * @return the current thumb position as a decimal value between 0 (off) and 1 (on).
     */
    @FloatRange(from = 0.0, to = 1.0)
    protected final float getThumbPosition() {
        return mThumbPosition;
    }

    /**
     * Sets the thumb position as a decimal value between 0 (off) and 1 (on).
     *
     * @param position new position between [0,1]
     */
    void setThumbPosition(float position) {
        mThumbPosition = position;
        invalidate();
    }

    @Override
    public void toggle() {
        setChecked(!isChecked());
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);

        // Calling the super method may result in setChecked() getting called
        // recursively with a different value, so load the REAL value...
        checked = isChecked();

        if (checked) {
            setOnStateDescriptionOnRAndAbove();
        } else {
            setOffStateDescriptionOnRAndAbove();
        }

        if (getWindowToken() != null && isLaidOut()) {
            animateThumbToCheckedState(checked);
        } else {
            // Immediately move the thumb to the new position.
            cancelPositionAnimator();
            setThumbPosition(checked ? 1 : 0);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int opticalInsetLeft = 0;
        int opticalInsetRight = 0;
        if (mThumbDrawable != null) {
            final Rect trackPadding = mTempRect;
            if (mTrackDrawable != null) {
                mTrackDrawable.getPadding(trackPadding);
            } else {
                trackPadding.setEmpty();
            }

            final Rect insets = DrawableUtils.getOpticalBounds(mThumbDrawable);
            opticalInsetLeft = Math.max(0, insets.left - trackPadding.left);
            opticalInsetRight = Math.max(0, insets.right - trackPadding.right);
        }

        final int switchRight;
        final int switchLeft;
        if (ViewUtils.isLayoutRtl(this)) {
            switchLeft = getPaddingLeft() + opticalInsetLeft;
            switchRight = switchLeft + mSwitchWidth - opticalInsetLeft - opticalInsetRight;
        } else {
            switchRight = getWidth() - getPaddingRight() - opticalInsetRight;
            switchLeft = switchRight - mSwitchWidth + opticalInsetLeft + opticalInsetRight;
        }

        final int switchTop;
        final int switchBottom;
        switch (getGravity() & Gravity.VERTICAL_GRAVITY_MASK) {
            default:
            case Gravity.TOP:
                switchTop = getPaddingTop();
                switchBottom = switchTop + mSwitchHeight;
                break;

            case Gravity.CENTER_VERTICAL:
                switchTop = (getPaddingTop() + getHeight() - getPaddingBottom()) / 2 -
                        mSwitchHeight / 2;
                switchBottom = switchTop + mSwitchHeight;
                break;

            case Gravity.BOTTOM:
                switchBottom = getHeight() - getPaddingBottom();
                switchTop = switchBottom - mSwitchHeight;
                break;
        }

        mSwitchLeft = switchLeft;
        mSwitchTop = switchTop;
        mSwitchBottom = switchBottom;
        mSwitchRight = switchRight;
    }

    @Override
    public void draw(@NonNull Canvas c) {
        final Rect padding = mTempRect;
        final int switchLeft = mSwitchLeft;
        final int switchTop = mSwitchTop;
        final int switchRight = mSwitchRight;
        final int switchBottom = mSwitchBottom;

        int thumbInitialLeft = switchLeft + getThumbOffset();

        final Rect thumbInsets;
        if (mThumbDrawable != null) {
            thumbInsets = DrawableUtils.getOpticalBounds(mThumbDrawable);
        } else {
            thumbInsets = DrawableUtils.INSETS_NONE;
        }

        // Layout the track.
        if (mTrackDrawable != null) {
            mTrackDrawable.getPadding(padding);

            // Adjust thumb position for track padding.
            thumbInitialLeft += padding.left;

            // If necessary, offset by the optical insets of the thumb asset.
            int trackLeft = switchLeft;
            int trackTop = switchTop;
            int trackRight = switchRight;
            int trackBottom = switchBottom;
            if (thumbInsets != null) {
                if (thumbInsets.left > padding.left) {
                    trackLeft += thumbInsets.left - padding.left;
                }
                if (thumbInsets.top > padding.top) {
                    trackTop += thumbInsets.top - padding.top;
                }
                if (thumbInsets.right > padding.right) {
                    trackRight -= thumbInsets.right - padding.right;
                }
                if (thumbInsets.bottom > padding.bottom) {
                    trackBottom -= thumbInsets.bottom - padding.bottom;
                }
            }
            mTrackDrawable.setBounds(trackLeft, trackTop, trackRight, trackBottom);
        }

        // Layout the thumb.
        if (mThumbDrawable != null) {
            mThumbDrawable.getPadding(padding);

            final int thumbLeft = thumbInitialLeft - padding.left;
            final int thumbRight = thumbInitialLeft + mThumbWidth + padding.right;
            mThumbDrawable.setBounds(thumbLeft, switchTop, thumbRight, switchBottom);

            final Drawable background = getBackground();
            if (background != null) {
                DrawableCompat.setHotspotBounds(background, thumbLeft, switchTop,
                        thumbRight, switchBottom);
            }
        }

        // Draw the background.
        super.draw(c);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final Rect padding = mTempRect;
        final Drawable trackDrawable = mTrackDrawable;
        if (trackDrawable != null) {
            trackDrawable.getPadding(padding);
        } else {
            padding.setEmpty();
        }

        final int switchTop = mSwitchTop;
        final int switchBottom = mSwitchBottom;
        final int switchInnerTop = switchTop + padding.top;
        final int switchInnerBottom = switchBottom - padding.bottom;

        final Drawable thumbDrawable = mThumbDrawable;
        if (trackDrawable != null) {
            if (mSplitTrack && thumbDrawable != null) {
                final Rect insets = DrawableUtils.getOpticalBounds(thumbDrawable);
                thumbDrawable.copyBounds(padding);
                padding.left += insets.left;
                padding.right -= insets.right;

                final int saveCount = canvas.save();
                canvas.clipRect(padding, Region.Op.DIFFERENCE);
                trackDrawable.draw(canvas);
                canvas.restoreToCount(saveCount);
            } else {
                trackDrawable.draw(canvas);
            }
        }

        final int saveCount = canvas.save();

        if (thumbDrawable != null) {
            thumbDrawable.draw(canvas);
        }

        final Layout switchText = getTargetCheckedState() ? mOnLayout : mOffLayout;
        if (switchText != null) {
            final int drawableState[] = getDrawableState();
            if (mTextColors != null) {
                mTextPaint.setColor(mTextColors.getColorForState(drawableState, 0));
            }
            mTextPaint.drawableState = drawableState;

            final int cX;
            if (thumbDrawable != null) {
                final Rect bounds = thumbDrawable.getBounds();
                cX = bounds.left + bounds.right;
            } else {
                cX = getWidth();
            }

            final int left = cX / 2 - switchText.getWidth() / 2;
            final int top = (switchInnerTop + switchInnerBottom) / 2 - switchText.getHeight() / 2;
            canvas.translate(left, top);
            switchText.draw(canvas);
        }

        canvas.restoreToCount(saveCount);
    }

    @Override
    public int getCompoundPaddingLeft() {
        if (!ViewUtils.isLayoutRtl(this)) {
            return super.getCompoundPaddingLeft();
        }
        int padding = super.getCompoundPaddingLeft() + mSwitchWidth;
        if (!TextUtils.isEmpty(getText())) {
            padding += mSwitchPadding;
        }
        return padding;
    }

    @Override
    public int getCompoundPaddingRight() {
        if (ViewUtils.isLayoutRtl(this)) {
            return super.getCompoundPaddingRight();
        }
        int padding = super.getCompoundPaddingRight() + mSwitchWidth;
        if (!TextUtils.isEmpty(getText())) {
            padding += mSwitchPadding;
        }
        return padding;
    }

    /**
     * Translates thumb position to offset according to current RTL setting and
     * thumb scroll range. Accounts for both track and thumb padding.
     *
     * @return thumb offset
     */
    private int getThumbOffset() {
        final float thumbPosition;
        if (ViewUtils.isLayoutRtl(this)) {
            thumbPosition = 1 - mThumbPosition;
        } else {
            thumbPosition = mThumbPosition;
        }
        return (int) (thumbPosition * getThumbScrollRange() + 0.5f);
    }

    private int getThumbScrollRange() {
        if (mTrackDrawable != null) {
            final Rect padding = mTempRect;
            mTrackDrawable.getPadding(padding);

            final Rect insets;
            if (mThumbDrawable != null) {
                insets = DrawableUtils.getOpticalBounds(mThumbDrawable);
            } else {
                insets = DrawableUtils.INSETS_NONE;
            }

            return mSwitchWidth - mThumbWidth - padding.left - padding.right
                    - insets.left - insets.right;
        } else {
            return 0;
        }
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        final int[] state = getDrawableState();
        boolean changed = false;

        final Drawable thumbDrawable = mThumbDrawable;
        if (thumbDrawable != null && thumbDrawable.isStateful()) {
            changed |= thumbDrawable.setState(state);
        }

        final Drawable trackDrawable = mTrackDrawable;
        if (trackDrawable != null && trackDrawable.isStateful()) {
            changed |= trackDrawable.setState(state);
        }

        if (changed) {
            invalidate();
        }
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);
        if (mThumbDrawable != null) {
            DrawableCompat.setHotspot(mThumbDrawable, x, y);
        }

        if (mTrackDrawable != null) {
            DrawableCompat.setHotspot(mTrackDrawable, x, y);
        }
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return super.verifyDrawable(who) || who == mThumbDrawable || who == mTrackDrawable;
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();

        if (mThumbDrawable != null) {
            mThumbDrawable.jumpToCurrentState();
        }

        if (mTrackDrawable != null) {
            mTrackDrawable.jumpToCurrentState();
        }

        if (mPositionAnimator != null && mPositionAnimator.isStarted()) {
            mPositionAnimator.end();
            mPositionAnimator = null;
        }
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(ACCESSIBILITY_EVENT_CLASS_NAME);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(ACCESSIBILITY_EVENT_CLASS_NAME);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            CharSequence switchText = isChecked() ? mTextOn : mTextOff;
            if (!TextUtils.isEmpty(switchText)) {
                CharSequence oldText = info.getText();
                if (TextUtils.isEmpty(oldText)) {
                    info.setText(switchText);
                } else {
                    StringBuilder newText = new StringBuilder();
                    newText.append(oldText).append(' ').append(switchText);
                    info.setText(newText);
                }
            }
        }
    }

    /**
     * See
     * {@link TextViewCompat#setCustomSelectionActionModeCallback(TextView, ActionMode.Callback)}
     */
    @Override
    public void setCustomSelectionActionModeCallback(
            ActionMode.@Nullable Callback actionModeCallback) {
        super.setCustomSelectionActionModeCallback(
                TextViewCompat.wrapCustomSelectionActionModeCallback(this, actionModeCallback));
    }

    @Override
    public ActionMode.@Nullable Callback getCustomSelectionActionModeCallback() {
        return TextViewCompat.unwrapCustomSelectionActionModeCallback(
                super.getCustomSelectionActionModeCallback());
    }

    /**
     * Sets {@code true} to enforce the switch width being at least twice of the thumb width.
     * Otherwise the switch width will be the value set by {@link #setSwitchMinWidth(int)}.
     *
     * The default value is {@code true}.
     */
    protected final void setEnforceSwitchWidth(boolean enforceSwitchWidth) {
        mEnforceSwitchWidth = enforceSwitchWidth;
        invalidate();
    }

    /**
     * Taken from android.util.MathUtils
     */
    private static float constrain(float amount, float low, float high) {
        return amount < low ? low : (amount > high ? high : amount);
    }

    private void setOnStateDescriptionOnRAndAbove() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ViewCompat.setStateDescription(
                    this,
                    mTextOn == null ? getResources().getString(R.string.abc_capital_on) : mTextOn
            );
        }
    }

    private void setOffStateDescriptionOnRAndAbove() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ViewCompat.setStateDescription(
                    this,
                    mTextOff == null ? getResources().getString(R.string.abc_capital_off) : mTextOff
            );
        }
    }

    @Override
    public void setAllCaps(boolean allCaps) {
        super.setAllCaps(allCaps);
        getEmojiTextViewHelper().setAllCaps(allCaps);
    }

    @Override
    public void setFilters(@SuppressWarnings("ArrayReturn") InputFilter @NonNull [] filters) {
        super.setFilters(getEmojiTextViewHelper().getFilters(filters));
    }

    /**
     * This may be called from super constructors.
     */
    private @NonNull AppCompatEmojiTextHelper getEmojiTextViewHelper() {
        //noinspection ConstantConditions
        if (mAppCompatEmojiTextHelper == null) {
            mAppCompatEmojiTextHelper = new AppCompatEmojiTextHelper(this);
        }
        return mAppCompatEmojiTextHelper;
    }

    @Override
    public void setEmojiCompatEnabled(boolean enabled) {
        getEmojiTextViewHelper().setEnabled(enabled);
        // the transformation method may have changed for on/off text so call again
        setTextOnInternal(mTextOn);
        setTextOffInternal(mTextOff);
        requestLayout();
    }

    @Override
    public boolean isEmojiCompatEnabled() {
        return getEmojiTextViewHelper().isEnabled();
    }


    /**
     * Call this before caching the text in mOnLayout or mOffLayout to ensure the layouts get
     * updated when emojicompat loads
     */
    private void setupEmojiCompatLoadCallback() {
        // Note: This is called again from onEmojiCompatInitializedForSwitchText, do not remove
        // null check of mEmojiCompatInitCallback without refactoring.
        if (mEmojiCompatInitCallback != null || !mAppCompatEmojiTextHelper.isEnabled()) {
            return;
        }
        if (EmojiCompat.isConfigured()) {
            EmojiCompat emojiCompat = EmojiCompat.get();
            int loadState = emojiCompat.getLoadState();
            if (loadState == EmojiCompat.LOAD_STATE_DEFAULT
                    || loadState == EmojiCompat.LOAD_STATE_LOADING) {
                // we can eventually load from default and loading
                mEmojiCompatInitCallback = new EmojiCompatInitCallback(this);
                emojiCompat.registerInitCallback(mEmojiCompatInitCallback);
            }
        }
    }

    /**
     * Update cached transformed text in mTextOn and mTextOff
     */
    void onEmojiCompatInitializedForSwitchText() {
        // this is required since we manage our own transformation method in this class during
        // setTextOn and setTextOff

        // if makeLayout, mOnLayout, or mOffLayout are removed, this can likely be removed
        setTextOnInternal(mTextOn);
        setTextOffInternal(mTextOff);
        requestLayout();
    }


    static class EmojiCompatInitCallback extends EmojiCompat.InitCallback {
        private final Reference<SwitchCompat> mOuterWeakRef;

        EmojiCompatInitCallback(SwitchCompat view) {
            mOuterWeakRef = new WeakReference<>(view);
        }


        @Override
        public void onInitialized() {
            SwitchCompat view = mOuterWeakRef.get();
            if (view != null) {
                view.onEmojiCompatInitializedForSwitchText();
            }
        }

        @Override
        public void onFailed(@Nullable Throwable throwable) {
            SwitchCompat view = mOuterWeakRef.get();
            if (view != null) {
                view.onEmojiCompatInitializedForSwitchText();
            }
        }
    }
}
