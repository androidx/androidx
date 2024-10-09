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

package androidx.appcompat.app;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InflateException;
import android.view.View;

import androidx.appcompat.R;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatCheckedTextView;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.AppCompatRatingBar;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.AppCompatToggleButton;
import androidx.appcompat.widget.TintContextWrapper;
import androidx.collection.SimpleArrayMap;
import androidx.core.view.ViewCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This class is used by AppCompat to automatically "substitute" all usages of core Android
 * widgets inflated from layout files by the AppCompat extensions of those widgets.
 *
 * <p>This class two main responsibilities: the first is to 'inject' our tinted views in place of
 * the framework versions in layout inflation; the second is backport the {@code android:theme}
 * functionality for any inflated widgets. This include theme inheritance from its parent.</p>
 *
 * <p>In order to provide your own extensions, follow these steps:
 *    <ul>
 *        <li>Extend this class, or the relevant subclass if you're using the Material
 *        components library</li>
 *        <li>Override one or more of the <code>createXYZ</code> methods</li>
 *        <li>Add the <code>viewInflaterClass</code> attribute on your application theme. The
 *        value of the attribute should be the fully-qualified class name of your custom inflater
 *        class.</li>
 *    </ul>
 * </p>
 */
public class AppCompatViewInflater {

    private static final Class<?>[] sConstructorSignature = new Class<?>[]{
            Context.class, AttributeSet.class};
    private static final int[] sOnClickAttrs = new int[]{android.R.attr.onClick};
    private static final int[] sAccessibilityHeading =
            new int[]{android.R.attr.accessibilityHeading};
    private static final int[] sAccessibilityPaneTitle =
            new int[]{android.R.attr.accessibilityPaneTitle};
    private static final int[] sScreenReaderFocusable =
            new int[]{android.R.attr.screenReaderFocusable};

    private static final String[] sClassPrefixList = {
            "android.widget.",
            "android.view.",
            "android.webkit."
    };

    private static final String LOG_TAG = "AppCompatViewInflater";

    private static final SimpleArrayMap<String, Constructor<? extends View>> sConstructorMap =
            new SimpleArrayMap<>();

    private final Object[] mConstructorArgs = new Object[2];

    /**
     * Creates an AppCompat-compatible widget by automatically "substituting" all usages of core
     * Android widgets with the AppCompat extensions of those widgets.
     * <p>
     * Most developers should not call this method directly. Instead, use the layout inflater
     * provided by {@link AppCompatActivity#getLayoutInflater()} or call
     * {@link AppCompatDelegate#createView(View, String, Context, AttributeSet)}.
     *
     * @param parent the hierarchical parent of the view, if any
     * @param name class name of the view, must be a simple class name like {@code TextView} for
     *             core Android widgets
     * @param context context against which the view should be constructed, may be wrapped for
     *                theming purposes or fully replaced if {@code inheritContext} is {@code true}
     * @param attrs attribute set to pass to the view constructor
     * @param inheritContext {@code true} to construct the view against the {@code parent} view's
     *                       context instead of {@code context}, or {@code false} to use
     *                       {@code context}
     * @param readAndroidTheme {@code true} to parse {@code android:theme} from {@code attrs} and
     *                         wrap the view's construction context if necessary
     * @param readAppTheme {@code true} to parse {@code app:theme} from {@code attrs} and wrap the
     *                     view's construction context if necessary
     * @param wrapContext {@code true} to wrap the view's construction context to provide
     *                    additional resource system feature backports
     * @return an AppCompat-compatible widget
     */
    public final @Nullable View createView(@Nullable View parent, final @NonNull String name,
            @NonNull Context context,
            @NonNull AttributeSet attrs, boolean inheritContext,
            boolean readAndroidTheme, boolean readAppTheme, boolean wrapContext) {
        final Context originalContext = context;

        // We can emulate Lollipop's android:theme attribute propagating down the view hierarchy
        // by using the parent's context
        if (inheritContext && parent != null) {
            context = parent.getContext();
        }
        if (readAndroidTheme || readAppTheme) {
            // We then apply the theme on the context, if specified
            context = themifyContext(context, attrs, readAndroidTheme, readAppTheme);
        }
        if (wrapContext) {
            context = TintContextWrapper.wrap(context);
        }

        View view = null;

        // We need to 'inject' our tint aware Views in place of the standard framework versions
        switch (name) {
            case "TextView":
                view = createTextView(context, attrs);
                verifyNotNull(view, name);
                break;
            case "ImageView":
                view = createImageView(context, attrs);
                verifyNotNull(view, name);
                break;
            case "Button":
                view = createButton(context, attrs);
                verifyNotNull(view, name);
                break;
            case "EditText":
                view = createEditText(context, attrs);
                verifyNotNull(view, name);
                break;
            case "Spinner":
                view = createSpinner(context, attrs);
                verifyNotNull(view, name);
                break;
            case "ImageButton":
                view = createImageButton(context, attrs);
                verifyNotNull(view, name);
                break;
            case "CheckBox":
                view = createCheckBox(context, attrs);
                verifyNotNull(view, name);
                break;
            case "RadioButton":
                view = createRadioButton(context, attrs);
                verifyNotNull(view, name);
                break;
            case "CheckedTextView":
                view = createCheckedTextView(context, attrs);
                verifyNotNull(view, name);
                break;
            case "AutoCompleteTextView":
                view = createAutoCompleteTextView(context, attrs);
                verifyNotNull(view, name);
                break;
            case "MultiAutoCompleteTextView":
                view = createMultiAutoCompleteTextView(context, attrs);
                verifyNotNull(view, name);
                break;
            case "RatingBar":
                view = createRatingBar(context, attrs);
                verifyNotNull(view, name);
                break;
            case "SeekBar":
                view = createSeekBar(context, attrs);
                verifyNotNull(view, name);
                break;
            case "ToggleButton":
                view = createToggleButton(context, attrs);
                verifyNotNull(view, name);
                break;
            default:
                // The fallback that allows extending class to take over view inflation
                // for other tags. Note that we don't check that the result is not-null.
                // That allows the custom inflater path to fall back on the default one
                // later in this method.
                view = createView(context, name, attrs);
        }

        if (view == null && originalContext != context) {
            // If the original context does not equal our themed context, then we need to manually
            // inflate it using the name so that android:theme takes effect.
            view = createViewFromTag(context, name, attrs);
        }

        if (view != null) {
            // If we have created a view, check its android:onClick
            checkOnClickListener(view, attrs);
            backportAccessibilityAttributes(context, view, attrs);
        }

        return view;
    }

    protected @NonNull AppCompatTextView createTextView(Context context, AttributeSet attrs) {
        return new AppCompatTextView(context, attrs);
    }

    protected @NonNull AppCompatImageView createImageView(Context context, AttributeSet attrs) {
        return new AppCompatImageView(context, attrs);
    }

    protected @NonNull AppCompatButton createButton(Context context, AttributeSet attrs) {
        return new AppCompatButton(context, attrs);
    }

    protected @NonNull AppCompatEditText createEditText(Context context, AttributeSet attrs) {
        return new AppCompatEditText(context, attrs);
    }

    protected @NonNull AppCompatSpinner createSpinner(Context context, AttributeSet attrs) {
        return new AppCompatSpinner(context, attrs);
    }

    protected @NonNull AppCompatImageButton createImageButton(Context context, AttributeSet attrs) {
        return new AppCompatImageButton(context, attrs);
    }

    protected @NonNull AppCompatCheckBox createCheckBox(Context context, AttributeSet attrs) {
        return new AppCompatCheckBox(context, attrs);
    }

    protected @NonNull AppCompatRadioButton createRadioButton(Context context, AttributeSet attrs) {
        return new AppCompatRadioButton(context, attrs);
    }

    protected @NonNull AppCompatCheckedTextView createCheckedTextView(Context context,
            AttributeSet attrs) {
        return new AppCompatCheckedTextView(context, attrs);
    }

    protected @NonNull AppCompatAutoCompleteTextView createAutoCompleteTextView(Context context,
            AttributeSet attrs) {
        return new AppCompatAutoCompleteTextView(context, attrs);
    }

    protected @NonNull AppCompatMultiAutoCompleteTextView createMultiAutoCompleteTextView(
            Context context, AttributeSet attrs) {
        return new AppCompatMultiAutoCompleteTextView(context, attrs);
    }

    protected @NonNull AppCompatRatingBar createRatingBar(Context context, AttributeSet attrs) {
        return new AppCompatRatingBar(context, attrs);
    }

    protected @NonNull AppCompatSeekBar createSeekBar(Context context, AttributeSet attrs) {
        return new AppCompatSeekBar(context, attrs);
    }

    protected @NonNull AppCompatToggleButton createToggleButton(Context context,
            AttributeSet attrs) {
        return new AppCompatToggleButton(context, attrs);
    }

    private void verifyNotNull(View view, String name) {
        if (view == null) {
            throw new IllegalStateException(this.getClass().getName()
                    + " asked to inflate view for <" + name + ">, but returned null");
        }
    }

    protected @Nullable View createView(Context context, String name, AttributeSet attrs) {
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
                for (int i = 0; i < sClassPrefixList.length; i++) {
                    final View view = createViewByPrefix(context, name, sClassPrefixList[i]);
                    if (view != null) {
                        return view;
                    }
                }
                return null;
            } else {
                return createViewByPrefix(context, name, null);
            }
        } catch (Exception e) {
            // We do not want to catch these, lets return null and let the actual LayoutInflater
            // try
            return null;
        } finally {
            // Don't retain references on context.
            mConstructorArgs[0] = null;
            mConstructorArgs[1] = null;
        }
    }

    /**
     * android:onClick doesn't handle views with a ContextWrapper context. This method
     * backports new framework functionality to traverse the Context wrappers to find a
     * suitable target.
     */
    private void checkOnClickListener(View view, AttributeSet attrs) {
        final Context context = view.getContext();

        if (!(context instanceof ContextWrapper) || !view.hasOnClickListeners()) {
            // Skip our compat functionality if: the Context isn't a ContextWrapper, or
            // the view doesn't have an OnClickListener (we can only rely on this on API 15+ so
            // always use our compat code on older devices)
            return;
        }

        final TypedArray a = context.obtainStyledAttributes(attrs, sOnClickAttrs);
        final String handlerName = a.getString(0);
        if (handlerName != null) {
            view.setOnClickListener(new DeclaredOnClickListener(view, handlerName));
        }
        a.recycle();
    }

    private View createViewByPrefix(Context context, String name, String prefix)
            throws ClassNotFoundException, InflateException {
        Constructor<? extends View> constructor = sConstructorMap.get(name);

        try {
            if (constructor == null) {
                // Class not found in the cache, see if it's real, and try to add it
                Class<? extends View> clazz = Class.forName(
                        prefix != null ? (prefix + name) : name,
                        false,
                        context.getClassLoader()).asSubclass(View.class);

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

    /**
     * Allows us to emulate the {@code android:theme} attribute for devices before L.
     */
    private static Context themifyContext(Context context, AttributeSet attrs,
            boolean useAndroidTheme, boolean useAppTheme) {
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.View, 0, 0);
        int themeId = 0;
        if (useAndroidTheme) {
            // First try reading android:theme if enabled
            themeId = a.getResourceId(R.styleable.View_android_theme, 0);
        }
        if (useAppTheme && themeId == 0) {
            // ...if that didn't work, try reading app:theme (for legacy reasons) if enabled
            themeId = a.getResourceId(R.styleable.View_theme, 0);

            if (themeId != 0) {
                Log.i(LOG_TAG, "app:theme is now deprecated. "
                        + "Please move to using android:theme instead.");
            }
        }
        a.recycle();

        if (themeId != 0 && (!(context instanceof ContextThemeWrapper)
                || ((ContextThemeWrapper) context).getThemeResId() != themeId)) {
            // If the context isn't a ContextThemeWrapper, or it is but does not have
            // the same theme as we need, wrap it in a new wrapper
            context = new ContextThemeWrapper(context, themeId);
        }
        return context;
    }

    private void backportAccessibilityAttributes(@NonNull Context context, @NonNull View view,
            @NonNull AttributeSet attrs) {
        if (Build.VERSION.SDK_INT > 28) {
            return;
        }

        TypedArray a = context.obtainStyledAttributes(attrs, sAccessibilityHeading);
        if (a.hasValue(0)) {
            ViewCompat.setAccessibilityHeading(view, a.getBoolean(0, false));
        }
        a.recycle();

        a = context.obtainStyledAttributes(attrs, sAccessibilityPaneTitle);
        if (a.hasValue(0)) {
            ViewCompat.setAccessibilityPaneTitle(view, a.getString(0));
        }
        a.recycle();

        a = context.obtainStyledAttributes(attrs, sScreenReaderFocusable);
        if (a.hasValue(0)) {
            ViewCompat.setScreenReaderFocusable(view, a.getBoolean(0, false));
        }
        a.recycle();
    }

    /**
     * An implementation of OnClickListener that attempts to lazily load a
     * named click handling method from a parent or ancestor context.
     */
    private static class DeclaredOnClickListener implements View.OnClickListener {
        private final View mHostView;
        private final String mMethodName;

        private Method mResolvedMethod;
        private Context mResolvedContext;

        public DeclaredOnClickListener(@NonNull View hostView, @NonNull String methodName) {
            mHostView = hostView;
            mMethodName = methodName;
        }

        @Override
        public void onClick(@NonNull View v) {
            if (mResolvedMethod == null) {
                resolveMethod(mHostView.getContext());
            }

            try {
                mResolvedMethod.invoke(mResolvedContext, v);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(
                        "Could not execute non-public method for android:onClick", e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException(
                        "Could not execute method for android:onClick", e);
            }
        }

        private void resolveMethod(@Nullable Context context) {
            while (context != null) {
                try {
                    if (!context.isRestricted()) {
                        final Method method = context.getClass().getMethod(mMethodName, View.class);
                        if (method != null) {
                            mResolvedMethod = method;
                            mResolvedContext = context;
                            return;
                        }
                    }
                } catch (NoSuchMethodException e) {
                    // Failed to find method, keep searching up the hierarchy.
                }

                if (context instanceof ContextWrapper) {
                    context = ((ContextWrapper) context).getBaseContext();
                } else {
                    // Can't search up the hierarchy, null out and fail.
                    context = null;
                }
            }

            final int id = mHostView.getId();
            final String idText = id == View.NO_ID ? "" : " with id '"
                    + mHostView.getContext().getResources().getResourceEntryName(id) + "'";
            throw new IllegalStateException("Could not find method " + mMethodName
                    + "(View) in a parent or ancestor Context for android:onClick "
                    + "attribute defined on view " + mHostView.getClass() + idText);
        }
    }
}
