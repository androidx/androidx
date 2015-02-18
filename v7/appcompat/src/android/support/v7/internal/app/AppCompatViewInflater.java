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

package android.support.v7.internal.app;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatMultiAutoCompleteTextView;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatRatingBar;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.internal.widget.ViewUtils;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.view.InflateException;
import android.view.View;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is responsible for manually inflating our tinted widgets which are used on devices
 * running {@link android.os.Build.VERSION_CODES#KITKAT KITKAT} or below. As such, this class
 * should only be used when running on those devices.
 * <p>This class two main responsibilities: the first is to 'inject' our tinted views in place of
 * the framework versions in layout inflation; the second is backport the {@code android:theme}
 * functionality for any inflated widgets. This include theme inheritance from it's parent.
 *
 * @hide
 */
public class AppCompatViewInflater {

    static final Class<?>[] sConstructorSignature = new Class[] {
            Context.class, AttributeSet.class};

    private static final Map<String, Constructor<? extends View>> sConstructorMap = new HashMap<>();

    private final Context mContext;
    private final Object[] mConstructorArgs = new Object[2];

    public AppCompatViewInflater(Context context) {
        mContext = context;
    }

    public final View createView(View parent, final String name, @NonNull Context context,
            @NonNull AttributeSet attrs, boolean inheritContext, boolean themeContext) {
        final Context originalContext = context;

        // We can emulate Lollipop's android:theme attribute propagating down the view hierarchy
        // by using the parent's context
        if (inheritContext && parent != null) {
            context = parent.getContext();
        }
        if (themeContext) {
            // We then apply the theme on the context, if specified
            context = ViewUtils.themifyContext(context, attrs, true, true);
        }

        // We need to 'inject' our tint aware Views in place of the standard framework versions
        switch (name) {
            case "EditText":
                return new AppCompatEditText(context, attrs);
            case "Spinner":
                return new AppCompatSpinner(context, attrs);
            case "CheckBox":
                return new AppCompatCheckBox(context, attrs);
            case "RadioButton":
                return new AppCompatRadioButton(context, attrs);
            case "CheckedTextView":
                return new AppCompatCheckedTextView(context, attrs);
            case "AutoCompleteTextView":
                return new AppCompatAutoCompleteTextView(context, attrs);
            case "MultiAutoCompleteTextView":
                return new AppCompatMultiAutoCompleteTextView(context, attrs);
            case "RatingBar":
                return new AppCompatRatingBar(context, attrs);
            case "Button":
                return new AppCompatButton(context, attrs);
            case "TextView":
                return new AppCompatTextView(context, attrs);
        }

        if (originalContext != context) {
            // If the original context does not equal our themed context, then we need to manually
            // inflate it using the name so that app:theme takes effect.
            return createViewFromTag(context, name, attrs);
        }

        return null;
    }

    private View createViewFromTag(Context context, String name, AttributeSet attrs) {
        if (name.equals("view")) {
            name = attrs.getAttributeValue(null, "class");
        }

        try {
            mConstructorArgs[0] = context;
            mConstructorArgs[1] = attrs;

            if (-1 == name.indexOf('.')) {
                // try the android.widget prefix first...
                return createView(name, "android.widget.");
            } else {
                return createView(name, null);
            }
        } catch (Exception e) {
            // We do not want to catch these, lets return null and let the actual LayoutInflater
            // try
            return null;
        } finally {
            // Don't retain static reference on context.
            mConstructorArgs[0] = null;
            mConstructorArgs[1] = null;
        }
    }

    private View createView(String name, String prefix)
            throws ClassNotFoundException, InflateException {
        Constructor<? extends View> constructor = sConstructorMap.get(name);

        try {
            if (constructor == null) {
                // Class not found in the cache, see if it's real, and try to add it
                Class<? extends View> clazz = mContext.getClassLoader().loadClass(
                        prefix != null ? (prefix + name) : name).asSubclass(View.class);

                constructor = clazz.getConstructor(sConstructorSignature);
                sConstructorMap.put(name, constructor);
            }
            constructor.setAccessible(true);
            return constructor.newInstance(mConstructorArgs);
        } catch (Exception e) {
            // We do not want to catch these, lets return null and let the actual LayoutInflater
            // try
            return null;
        }
    }

}
