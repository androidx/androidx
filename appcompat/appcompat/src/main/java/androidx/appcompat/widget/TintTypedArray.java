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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.DoNotInline;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.StyleableRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;

/**
 * A class that wraps a {@link TypedArray} and provides the same public API
 * surface. The purpose of this class is so that we can intercept calls to new APIs.
 *
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class TintTypedArray {

    private final Context mContext;
    private final TypedArray mWrapped;

    private TypedValue mTypedValue;

    public static TintTypedArray obtainStyledAttributes(Context context, AttributeSet set,
            int[] attrs) {
        return new TintTypedArray(context, context.obtainStyledAttributes(set, attrs));
    }

    public static TintTypedArray obtainStyledAttributes(Context context, AttributeSet set,
            int[] attrs, int defStyleAttr, int defStyleRes) {
        return new TintTypedArray(context,
                context.obtainStyledAttributes(set, attrs, defStyleAttr, defStyleRes));
    }

    public static TintTypedArray obtainStyledAttributes(Context context, int resid, int[] attrs) {
        return new TintTypedArray(context, context.obtainStyledAttributes(resid, attrs));
    }

    private TintTypedArray(Context context, TypedArray array) {
        mContext = context;
        mWrapped = array;
    }

    /**
     * Beware, you very likely do not intend to this method. Proceed with caution.
     */
    public TypedArray getWrappedTypeArray() {
        return mWrapped;
    }

    public Drawable getDrawable(int index) {
        if (mWrapped.hasValue(index)) {
            final int resourceId = mWrapped.getResourceId(index, 0);
            if (resourceId != 0) {
                return AppCompatResources.getDrawable(mContext, resourceId);
            }
        }
        return mWrapped.getDrawable(index);
    }

    public Drawable getDrawableIfKnown(int index) {
        if (mWrapped.hasValue(index)) {
            final int resourceId = mWrapped.getResourceId(index, 0);
            if (resourceId != 0) {
                return AppCompatDrawableManager.get().getDrawable(mContext, resourceId, true);
            }
        }
        return null;
    }

    /**
     * Retrieve the Typeface for the attribute at <var>index</var>.
     * <p>
     * This method will throw an exception if the attribute is defined but is
     * not a font.
     *
     * @param index Index of attribute to retrieve.
     * @param style A style value used for selecting best match font from the list of family. Note
     * that this value will be ignored if the platform supports font family (API 24 or later).
     * @param fontCallback A callback to receive async fetching of this font. If async loading is
     *                     specified in XML, this callback will be triggered.
     *
     * @return Typeface for the attribute, or {@code null} if not defined.
     * @throws RuntimeException if the TypedArray has already been recycled.
     * @throws UnsupportedOperationException if the attribute is defined but is
     *         not a font resource.
     */
    @Nullable
    public Typeface getFont(@StyleableRes int index, int style,
            @Nullable ResourcesCompat.FontCallback fontCallback) {
        final int resourceId = mWrapped.getResourceId(index, 0);
        if (resourceId == 0) {
            return null;
        }
        if (!getString(index).startsWith("res/")) {
            return null;
        }
        if (mTypedValue == null) {
            mTypedValue = new TypedValue();
        }
        return ResourcesCompat.getFont(mContext, resourceId, mTypedValue, style, fontCallback);
    }

    public int length() {
        return mWrapped.length();
    }

    public int getIndexCount() {
        return mWrapped.getIndexCount();
    }

    public int getIndex(int at) {
        return mWrapped.getIndex(at);
    }

    public Resources getResources() {
        return mWrapped.getResources();
    }

    public CharSequence getText(int index) {
        return mWrapped.getText(index);
    }

    public String getString(int index) {
        return mWrapped.getString(index);
    }

    public String getNonResourceString(int index) {
        return mWrapped.getNonResourceString(index);
    }

    public boolean getBoolean(int index, boolean defValue) {
        return mWrapped.getBoolean(index, defValue);
    }

    public int getInt(int index, int defValue) {
        return mWrapped.getInt(index, defValue);
    }

    public float getFloat(int index, float defValue) {
        return mWrapped.getFloat(index, defValue);
    }

    public int getColor(int index, int defValue) {
        return mWrapped.getColor(index, defValue);
    }

    public ColorStateList getColorStateList(int index) {
        if (mWrapped.hasValue(index)) {
            final int resourceId = mWrapped.getResourceId(index, 0);
            if (resourceId != 0) {
                final ColorStateList value =
                        AppCompatResources.getColorStateList(mContext, resourceId);
                if (value != null) {
                    return value;
                }
            }
        }
        return mWrapped.getColorStateList(index);
    }

    public int getInteger(int index, int defValue) {
        return mWrapped.getInteger(index, defValue);
    }

    public float getDimension(int index, float defValue) {
        return mWrapped.getDimension(index, defValue);
    }

    public int getDimensionPixelOffset(int index, int defValue) {
        return mWrapped.getDimensionPixelOffset(index, defValue);
    }

    public int getDimensionPixelSize(int index, int defValue) {
        return mWrapped.getDimensionPixelSize(index, defValue);
    }

    public int getLayoutDimension(int index, String name) {
        return mWrapped.getLayoutDimension(index, name);
    }

    public int getLayoutDimension(int index, int defValue) {
        return mWrapped.getLayoutDimension(index, defValue);
    }

    public float getFraction(int index, int base, int pbase, float defValue) {
        return mWrapped.getFraction(index, base, pbase, defValue);
    }

    public int getResourceId(int index, int defValue) {
        return mWrapped.getResourceId(index, defValue);
    }

    public CharSequence[] getTextArray(int index) {
        return mWrapped.getTextArray(index);
    }

    public boolean getValue(int index, TypedValue outValue) {
        return mWrapped.getValue(index, outValue);
    }

    public int getType(int index) {
        if (Build.VERSION.SDK_INT >= 21) {
            return Api21Impl.getType(mWrapped, index);
        } else {
            if (mTypedValue == null) {
                mTypedValue = new TypedValue();
            }
            mWrapped.getValue(index, mTypedValue);
            return mTypedValue.type;
        }
    }

    public boolean hasValue(int index) {
        return mWrapped.hasValue(index);
    }

    public TypedValue peekValue(int index) {
        return mWrapped.peekValue(index);
    }

    public String getPositionDescription() {
        return mWrapped.getPositionDescription();
    }

    public void recycle() {
        mWrapped.recycle();
    }

    @RequiresApi(21)
    public int getChangingConfigurations() {
        return Api21Impl.getChangingConfigurations(mWrapped);
    }

    @RequiresApi(21)
    static class Api21Impl {
        private Api21Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static int getType(TypedArray typedArray, int index) {
            return typedArray.getType(index);
        }

        @DoNotInline
        static int getChangingConfigurations(TypedArray typedArray) {
            return typedArray.getChangingConfigurations();
        }
    }
}
