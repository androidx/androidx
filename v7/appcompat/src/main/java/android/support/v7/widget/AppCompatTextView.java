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

package android.support.v7.widget;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.view.TintableBackgroundView;
import android.support.v4.widget.AutoSizeableTextView;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.appcompat.R;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * A {@link TextView} which supports compatible features on older versions of the platform,
 * including:
 * <ul>
 *     <li>Allows dynamic tint of its background via the background tint methods in
 *     {@link android.support.v4.view.ViewCompat}.</li>
 *     <li>Allows setting of the background tint using {@link R.attr#backgroundTint} and
 *     {@link R.attr#backgroundTintMode}.</li>
 *     <li>Supports auto-sizing via {@link android.support.v4.widget.TextViewCompat} by allowing
 *     to instruct a {@link TextView} to let the size of the text expand or contract automatically
 *     to fill its layout based on the TextView's characteristics and boundaries. The
 *     style attributes associated with auto-sizing are {@link R.attr#autoSizeTextType},
 *     {@link R.attr#autoSizeMinTextSize}, {@link R.attr#autoSizeMaxTextSize},
 *     {@link R.attr#autoSizeStepGranularity} and {@link R.attr#autoSizePresetSizes}, all of
 *     which work back to
 *     {@link android.os.Build.VERSION_CODES#ICE_CREAM_SANDWICH Ice Cream Sandwich}.</li>
 * </ul>
 *
 * <p>This will automatically be used when you use {@link TextView} in your layouts
 * and the top-level activity / dialog is provided by
 * <a href="{@docRoot}topic/libraries/support-library/packages.html#v7-appcompat">appcompat</a>.
 * You should only need to manually use this class when writing custom views.</p>
 */
public class AppCompatTextView extends TextView implements TintableBackgroundView,
        AutoSizeableTextView {

    private final AppCompatBackgroundHelper mBackgroundTintHelper;
    private final AppCompatTextHelper mTextHelper;

    public AppCompatTextView(Context context) {
        this(context, null);
    }

    public AppCompatTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public AppCompatTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(TintContextWrapper.wrap(context), attrs, defStyleAttr);

        mBackgroundTintHelper = new AppCompatBackgroundHelper(this);
        mBackgroundTintHelper.loadFromAttributes(attrs, defStyleAttr);

        mTextHelper = AppCompatTextHelper.create(this);
        mTextHelper.loadFromAttributes(attrs, defStyleAttr);
        mTextHelper.applyCompoundDrawablesTints();
    }

    @Override
    public void setBackgroundResource(@DrawableRes int resId) {
        super.setBackgroundResource(resId);
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.onSetBackgroundResource(resId);
        }
    }

    @Override
    public void setBackgroundDrawable(Drawable background) {
        super.setBackgroundDrawable(background);
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.onSetBackgroundDrawable(background);
        }
    }

    /**
     * This should be accessed via
     * {@link android.support.v4.view.ViewCompat#setBackgroundTintList(android.view.View, ColorStateList)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void setSupportBackgroundTintList(@Nullable ColorStateList tint) {
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.setSupportBackgroundTintList(tint);
        }
    }

    /**
     * This should be accessed via
     * {@link android.support.v4.view.ViewCompat#getBackgroundTintList(android.view.View)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    @Nullable
    public ColorStateList getSupportBackgroundTintList() {
        return mBackgroundTintHelper != null
                ? mBackgroundTintHelper.getSupportBackgroundTintList() : null;
    }

    /**
     * This should be accessed via
     * {@link android.support.v4.view.ViewCompat#setBackgroundTintMode(android.view.View, PorterDuff.Mode)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void setSupportBackgroundTintMode(@Nullable PorterDuff.Mode tintMode) {
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.setSupportBackgroundTintMode(tintMode);
        }
    }

    /**
     * This should be accessed via
     * {@link android.support.v4.view.ViewCompat#getBackgroundTintMode(android.view.View)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    @Nullable
    public PorterDuff.Mode getSupportBackgroundTintMode() {
        return mBackgroundTintHelper != null
                ? mBackgroundTintHelper.getSupportBackgroundTintMode() : null;
    }

    @Override
    public void setTextAppearance(Context context, int resId) {
        super.setTextAppearance(context, resId);
        if (mTextHelper != null) {
            mTextHelper.onSetTextAppearance(context, resId);
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.applySupportBackgroundTint();
        }
        if (mTextHelper != null) {
            mTextHelper.applyCompoundDrawablesTints();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mTextHelper != null) {
            mTextHelper.onLayout(changed, left, top, right, bottom);
        }
    }

    @Override
    public void setTextSize(int unit, float size) {
        if (PLATFORM_SUPPORTS_AUTOSIZE) {
            super.setTextSize(unit, size);
        } else {
            if (mTextHelper != null) {
                mTextHelper.setTextSize(unit, size);
            }
        }
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        if (mTextHelper != null && !PLATFORM_SUPPORTS_AUTOSIZE && mTextHelper.isAutoSizeEnabled()) {
            mTextHelper.autoSizeText();
        }
    }

    /**
     * This should be accessed via
     * {@link android.support.v4.widget.TextViewCompat#setAutoSizeTextTypeWithDefaults(
     *        TextView, int)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void setAutoSizeTextTypeWithDefaults(
            @TextViewCompat.AutoSizeTextType int autoSizeTextType) {
        if (PLATFORM_SUPPORTS_AUTOSIZE) {
            super.setAutoSizeTextTypeWithDefaults(autoSizeTextType);
        } else {
            if (mTextHelper != null) {
                mTextHelper.setAutoSizeTextTypeWithDefaults(autoSizeTextType);
            }
        }
    }

    /**
     * This should be accessed via
     * {@link android.support.v4.widget.TextViewCompat#setAutoSizeTextTypeUniformWithConfiguration(
     *        TextView, int, int, int, int)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void setAutoSizeTextTypeUniformWithConfiguration(
            int autoSizeMinTextSize,
            int autoSizeMaxTextSize,
            int autoSizeStepGranularity,
            int unit) throws IllegalArgumentException {
        if (PLATFORM_SUPPORTS_AUTOSIZE) {
            super.setAutoSizeTextTypeUniformWithConfiguration(
                    autoSizeMinTextSize, autoSizeMaxTextSize, autoSizeStepGranularity, unit);
        } else {
            if (mTextHelper != null) {
                mTextHelper.setAutoSizeTextTypeUniformWithConfiguration(
                        autoSizeMinTextSize, autoSizeMaxTextSize, autoSizeStepGranularity, unit);
            }
        }
    }

    /**
     * This should be accessed via
     * {@link android.support.v4.widget.TextViewCompat#setAutoSizeTextTypeUniformWithPresetSizes(
     *        TextView, int[], int)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void setAutoSizeTextTypeUniformWithPresetSizes(@NonNull int[] presetSizes, int unit)
            throws IllegalArgumentException {
        if (PLATFORM_SUPPORTS_AUTOSIZE) {
            super.setAutoSizeTextTypeUniformWithPresetSizes(presetSizes, unit);
        } else {
            if (mTextHelper != null) {
                mTextHelper.setAutoSizeTextTypeUniformWithPresetSizes(presetSizes, unit);
            }
        }
    }

    /**
     * This should be accessed via
     * {@link android.support.v4.widget.TextViewCompat#getAutoSizeTextType(TextView)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    @TextViewCompat.AutoSizeTextType
    public int getAutoSizeTextType() {
        if (PLATFORM_SUPPORTS_AUTOSIZE) {
            return super.getAutoSizeTextType() == TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM
                    ? TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM
                    : TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE;
        } else {
            if (mTextHelper != null) {
                return mTextHelper.getAutoSizeTextType();
            }
        }
        return TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE;
    }

    /**
     * This should be accessed via
     * {@link android.support.v4.widget.TextViewCompat#getAutoSizeStepGranularity(TextView)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public int getAutoSizeStepGranularity() {
        if (PLATFORM_SUPPORTS_AUTOSIZE) {
            return super.getAutoSizeStepGranularity();
        } else {
            if (mTextHelper != null) {
                return mTextHelper.getAutoSizeStepGranularity();
            }
        }
        return -1;
    }

    /**
     * This should be accessed via
     * {@link android.support.v4.widget.TextViewCompat#getAutoSizeMinTextSize(TextView)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public int getAutoSizeMinTextSize() {
        if (PLATFORM_SUPPORTS_AUTOSIZE) {
            return super.getAutoSizeMinTextSize();
        } else {
            if (mTextHelper != null) {
                return mTextHelper.getAutoSizeMinTextSize();
            }
        }
        return -1;
    }

    /**
     * This should be accessed via
     * {@link android.support.v4.widget.TextViewCompat#getAutoSizeMaxTextSize(TextView)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public int getAutoSizeMaxTextSize() {
        if (PLATFORM_SUPPORTS_AUTOSIZE) {
            return super.getAutoSizeMaxTextSize();
        } else {
            if (mTextHelper != null) {
                return mTextHelper.getAutoSizeMaxTextSize();
            }
        }
        return -1;
    }

    /**
     * This should be accessed via
     * {@link android.support.v4.widget.TextViewCompat#getAutoSizeTextAvailableSizes(TextView)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public int[] getAutoSizeTextAvailableSizes() {
        if (PLATFORM_SUPPORTS_AUTOSIZE) {
            return super.getAutoSizeTextAvailableSizes();
        } else {
            if (mTextHelper != null) {
                return mTextHelper.getAutoSizeTextAvailableSizes();
            }
        }
        return new int[0];
    }
}
