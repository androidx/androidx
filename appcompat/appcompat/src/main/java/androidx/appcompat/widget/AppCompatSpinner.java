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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.StyleableRes;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.R;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.view.menu.ShowableListMenu;
import androidx.core.util.ObjectsCompat;
import androidx.core.view.TintableBackgroundView;
import androidx.resourceinspection.annotation.AppCompatShadowedAttributes;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A {@link Spinner} which supports compatible features on older versions of the platform,
 * including:
 * <ul>
 *     <li>Allows dynamic tint of its background via the background tint methods in
 *     {@link androidx.core.view.ViewCompat}.</li>
 *     <li>Allows setting of the background tint using {@link R.attr#buttonTint} and
 *     {@link R.attr#buttonTintMode}.</li>
 *     <li>Setting the popup theme using {@link R.attr#popupTheme}.</li>
 * </ul>
 *
 * <p>This will automatically be used when you use {@link Spinner} in your layouts.
 * You should only need to manually use this class when writing custom views.</p>
 */
@AppCompatShadowedAttributes
public class AppCompatSpinner extends Spinner implements TintableBackgroundView {

    @SuppressLint("ResourceType")
    @StyleableRes
    private static final int[] ATTRS_ANDROID_SPINNERMODE = {android.R.attr.spinnerMode};

    private static final int MAX_ITEMS_MEASURED = 15;

    private static final String TAG = "AppCompatSpinner";

    private static final int MODE_DIALOG = 0;
    private static final int MODE_DROPDOWN = 1;
    private static final int MODE_THEME = -1;

    private final AppCompatBackgroundHelper mBackgroundTintHelper;

    /** Context used to inflate the popup window or dialog. */
    private final Context mPopupContext;

    /** Forwarding listener used to implement drag-to-open. */
    private ForwardingListener mForwardingListener;

    /** Temporary holder for setAdapter() calls from the super constructor. */
    private SpinnerAdapter mTempAdapter;

    private final boolean mPopupSet;

    private SpinnerPopup mPopup;

    int mDropDownWidth;

    final Rect mTempRect = new Rect();

    /**
     * Construct a new spinner with the given context's theme.
     *
     * @param context The Context the view is running in, through which it can
     *                access the current theme, resources, etc.
     */
    public AppCompatSpinner(
            @NonNull Context context) {
        this(context, null);
    }

    /**
     * Construct a new spinner with the given context's theme and the supplied
     * mode of displaying choices. <code>mode</code> may be one of
     * {@link #MODE_DIALOG} or {@link #MODE_DROPDOWN}.
     *
     * @param context The Context the view is running in, through which it can
     *                access the current theme, resources, etc.
     * @param mode    Constant describing how the user will select choices from the spinner.
     * @see #MODE_DIALOG
     * @see #MODE_DROPDOWN
     */
    public AppCompatSpinner(
            @NonNull Context context, int mode) {
        this(context, null, R.attr.spinnerStyle, mode);
    }

    /**
     * Construct a new spinner with the given context's theme and the supplied attribute set.
     *
     * @param context The Context the view is running in, through which it can
     *                access the current theme, resources, etc.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public AppCompatSpinner(
            @NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.spinnerStyle);
    }

    /**
     * Construct a new spinner with the given context's theme, the supplied attribute set,
     * and default style attribute.
     *
     * @param context      The Context the view is running in, through which it can
     *                     access the current theme, resources, etc.
     * @param attrs        The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr An attribute in the current theme that contains a
     *                     reference to a style resource that supplies default values for
     *                     the view. Can be 0 to not look for defaults.
     */
    public AppCompatSpinner(
            @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, MODE_THEME);
    }

    /**
     * Construct a new spinner with the given context's theme, the supplied attribute set,
     * and default style. <code>mode</code> may be one of {@link #MODE_DIALOG} or
     * {@link #MODE_DROPDOWN} and determines how the user will select choices from the spinner.
     *
     * @param context      The Context the view is running in, through which it can
     *                     access the current theme, resources, etc.
     * @param attrs        The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr An attribute in the current theme that contains a
     *                     reference to a style resource that supplies default values for
     *                     the view. Can be 0 to not look for defaults.
     * @param mode         Constant describing how the user will select choices from the spinner.
     * @see #MODE_DIALOG
     * @see #MODE_DROPDOWN
     */
    public AppCompatSpinner(
            @NonNull Context context,  @Nullable AttributeSet attrs, int defStyleAttr, int mode) {
        this(context, attrs, defStyleAttr, mode, null);
    }


    /**
     * Constructs a new spinner with the given context's theme, the supplied
     * attribute set, default styles, popup mode (one of {@link #MODE_DIALOG}
     * or {@link #MODE_DROPDOWN}), and the context against which the popup
     * should be inflated.
     *
     * @param context      The context against which the view is inflated, which
     *                     provides access to the current theme, resources, etc.
     * @param attrs        The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr An attribute in the current theme that contains a
     *                     reference to a style resource that supplies default
     *                     values for the view. Can be 0 to not look for
     *                     defaults.
     * @param mode         Constant describing how the user will select choices from
     *                     the spinner.
     * @param popupTheme   The theme against which the dialog or dropdown popup
     *                     should be inflated. May be {@code null} to use the
     *                     view theme. If set, this will override any value
     *                     specified by
     *                     {@link R.styleable#Spinner_popupTheme}.
     * @see #MODE_DIALOG
     * @see #MODE_DROPDOWN
     */
    public AppCompatSpinner(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr, int mode, Resources.Theme popupTheme) {
        super(context, attrs, defStyleAttr);

        ThemeUtils.checkAppCompatTheme(this, getContext());

        TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs,
                R.styleable.Spinner, defStyleAttr, 0);

        mBackgroundTintHelper = new AppCompatBackgroundHelper(this);

        if (popupTheme != null) {
            mPopupContext = new ContextThemeWrapper(context, popupTheme);
        } else {
            final int popupThemeResId = a.getResourceId(R.styleable.Spinner_popupTheme, 0);
            if (popupThemeResId != 0) {
                mPopupContext = new ContextThemeWrapper(context, popupThemeResId);
            } else {
                mPopupContext = context;
            }
        }

        if (mode == MODE_THEME) {
            TypedArray aa = null;
            try {
                aa = context.obtainStyledAttributes(attrs, ATTRS_ANDROID_SPINNERMODE,
                        defStyleAttr, 0);
                if (aa.hasValue(0)) {
                    mode = aa.getInt(0, MODE_DIALOG);
                }
            } catch (Exception e) {
                Log.i(TAG, "Could not read android:spinnerMode", e);
            } finally {
                if (aa != null) {
                    aa.recycle();
                }
            }
        }

        switch (mode) {
            case MODE_DIALOG: {
                mPopup = new AppCompatSpinner.DialogPopup();
                mPopup.setPromptText(a.getString(R.styleable.Spinner_android_prompt));
                break;
            }
            case MODE_DROPDOWN: {
                final DropdownPopup popup = new DropdownPopup(mPopupContext, attrs, defStyleAttr);
                final TintTypedArray pa = TintTypedArray.obtainStyledAttributes(
                        mPopupContext, attrs, R.styleable.Spinner, defStyleAttr, 0);
                mDropDownWidth = pa.getLayoutDimension(R.styleable.Spinner_android_dropDownWidth,
                        LayoutParams.WRAP_CONTENT);
                popup.setBackgroundDrawable(
                        pa.getDrawable(R.styleable.Spinner_android_popupBackground));
                popup.setPromptText(a.getString(R.styleable.Spinner_android_prompt));
                pa.recycle();

                mPopup = popup;
                mForwardingListener = new ForwardingListener(this) {
                    @Override
                    public ShowableListMenu getPopup() {
                        return popup;
                    }

                    @Override
                    public boolean onForwardingStarted() {
                        if (!getInternalPopup().isShowing()) {
                            showPopup();
                        }
                        return true;
                    }
                };
            }
        }

        final CharSequence[] entries = a.getTextArray(R.styleable.Spinner_android_entries);
        if (entries != null) {
            final ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                    context, android.R.layout.simple_spinner_item, entries);
            adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
            setAdapter(adapter);
        }

        a.recycle();

        mPopupSet = true;

        // Base constructors can call setAdapter before we initialize mPopup.
        // Finish setting things up if this happened.
        if (mTempAdapter != null) {
            setAdapter(mTempAdapter);
            mTempAdapter = null;
        }

        mBackgroundTintHelper.loadFromAttributes(attrs, defStyleAttr);
    }

    /**
     * @return the context used to inflate the Spinner's popup or dialog window
     */
    @Override
    public Context getPopupContext() {
        return mPopupContext;
    }

    @Override
    public void setPopupBackgroundDrawable(Drawable background) {
        if (mPopup != null) {
            mPopup.setBackgroundDrawable(background);
        } else {
            super.setPopupBackgroundDrawable(background);
        }
    }

    @Override
    public void setPopupBackgroundResource(@DrawableRes int resId) {
        setPopupBackgroundDrawable(AppCompatResources.getDrawable(getPopupContext(), resId));
    }

    @Override
    public Drawable getPopupBackground() {
        if (mPopup != null) {
            return mPopup.getBackground();
        } else {
            return super.getPopupBackground();
        }
    }

    @Override
    public void setDropDownVerticalOffset(int pixels) {
        if (mPopup != null) {
            mPopup.setVerticalOffset(pixels);
        } else {
            super.setDropDownVerticalOffset(pixels);
        }
    }

    @Override
    public int getDropDownVerticalOffset() {
        if (mPopup != null) {
            return mPopup.getVerticalOffset();
        } else {
            return super.getDropDownVerticalOffset();
        }
    }

    @Override
    public void setDropDownHorizontalOffset(int pixels) {
        if (mPopup != null) {
            mPopup.setHorizontalOriginalOffset(pixels);
            mPopup.setHorizontalOffset(pixels);
        } else {
            super.setDropDownHorizontalOffset(pixels);
        }
    }

    /**
     * Get the configured horizontal offset in pixels for the spinner's popup window of choices.
     * Only valid in {@link #MODE_DROPDOWN}; other modes will return 0.
     *
     * @return Horizontal offset in pixels
     */
    @Override
    public int getDropDownHorizontalOffset() {
        if (mPopup != null) {
            return mPopup.getHorizontalOffset();
        } else {
            return super.getDropDownHorizontalOffset();
        }
    }

    @Override
    public void setDropDownWidth(int pixels) {
        if (mPopup != null) {
            mDropDownWidth = pixels;
        } else {
            super.setDropDownWidth(pixels);
        }
    }

    @Override
    public int getDropDownWidth() {
        if (mPopup != null) {
            return mDropDownWidth;
        } else {
            return super.getDropDownWidth();
        }
    }

    @Override
    public void setAdapter(SpinnerAdapter adapter) {
        // The super constructor may call setAdapter before we're prepared.
        // Postpone doing anything until we've finished construction.
        if (!mPopupSet) {
            mTempAdapter = adapter;
            return;
        }

        super.setAdapter(adapter);

        if (mPopup != null) {
            final Context popupContext = mPopupContext == null ? getContext() : mPopupContext;
            mPopup.setAdapter(new DropDownAdapter(adapter, popupContext.getTheme()));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mPopup != null && mPopup.isShowing()) {
            mPopup.dismiss();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mForwardingListener != null && mForwardingListener.onTouch(this, event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mPopup != null && MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST) {
            final int measuredWidth = getMeasuredWidth();
            setMeasuredDimension(Math.min(Math.max(measuredWidth,
                                    compatMeasureContentWidth(getAdapter(), getBackground())),
                            MeasureSpec.getSize(widthMeasureSpec)),
                    getMeasuredHeight());
        }
    }

    @Override
    public boolean performClick() {
        if (mPopup != null) {
            // If we have a popup, show it if needed, or just consume the click...
            if (!mPopup.isShowing()) {
                showPopup();
            }
            return true;
        }

        // Else let the platform handle the click
        return super.performClick();
    }

    @Override
    public void setPrompt(CharSequence prompt) {
        if (mPopup != null) {
            mPopup.setPromptText(prompt);
        } else {
            super.setPrompt(prompt);
        }
    }

    @Override
    public CharSequence getPrompt() {
        return mPopup != null ? mPopup.getHintText() : super.getPrompt();
    }

    @Override
    public void setBackgroundResource(@DrawableRes int resId) {
        super.setBackgroundResource(resId);
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.onSetBackgroundResource(resId);
        }
    }

    @Override
    public void setBackgroundDrawable(@Nullable Drawable background) {
        super.setBackgroundDrawable(background);
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.onSetBackgroundDrawable(background);
        }
    }

    /**
     * This should be accessed via
     * {@link androidx.core.view.ViewCompat#setBackgroundTintList(android.view.View,
     * ColorStateList)}
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public void setSupportBackgroundTintList(@Nullable ColorStateList tint) {
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.setSupportBackgroundTintList(tint);
        }
    }

    /**
     * This should be accessed via
     * {@link androidx.core.view.ViewCompat#getBackgroundTintList(android.view.View)}
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public @Nullable ColorStateList getSupportBackgroundTintList() {
        return mBackgroundTintHelper != null
                ? mBackgroundTintHelper.getSupportBackgroundTintList() : null;
    }

    /**
     * This should be accessed via
     * {@link androidx.core.view.ViewCompat#setBackgroundTintMode(android.view.View,
     * PorterDuff.Mode)}
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public void setSupportBackgroundTintMode(PorterDuff.@Nullable Mode tintMode) {
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.setSupportBackgroundTintMode(tintMode);
        }
    }

    /**
     * This should be accessed via
     * {@link androidx.core.view.ViewCompat#getBackgroundTintMode(android.view.View)}
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public PorterDuff.@Nullable Mode getSupportBackgroundTintMode() {
        return mBackgroundTintHelper != null
                ? mBackgroundTintHelper.getSupportBackgroundTintMode() : null;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.applySupportBackgroundTint();
        }
    }

    int compatMeasureContentWidth(SpinnerAdapter adapter, Drawable background) {
        if (adapter == null) {
            return 0;
        }

        int width = 0;
        View itemView = null;
        int itemType = 0;
        final int widthMeasureSpec =
                MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.UNSPECIFIED);
        final int heightMeasureSpec =
                MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.UNSPECIFIED);

        // Make sure the number of items we'll measure is capped. If it's a huge data set
        // with wildly varying sizes, oh well.
        int start = Math.max(0, getSelectedItemPosition());
        final int end = Math.min(adapter.getCount(), start + MAX_ITEMS_MEASURED);
        final int count = end - start;
        start = Math.max(0, start - (MAX_ITEMS_MEASURED - count));
        for (int i = start; i < end; i++) {
            final int positionType = adapter.getItemViewType(i);
            if (positionType != itemType) {
                itemType = positionType;
                itemView = null;
            }
            itemView = adapter.getView(i, itemView, this);
            if (itemView.getLayoutParams() == null) {
                itemView.setLayoutParams(new LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT));
            }
            itemView.measure(widthMeasureSpec, heightMeasureSpec);
            width = Math.max(width, itemView.getMeasuredWidth());
        }

        // Add background padding to measured width
        if (background != null) {
            background.getPadding(mTempRect);
            width += mTempRect.left + mTempRect.right;
        }

        return width;
    }

    @VisibleForTesting
    final SpinnerPopup getInternalPopup() {
        return mPopup;
    }

    void showPopup() {
        mPopup.show(getTextDirection(), getTextAlignment());
    }


    @Override
    public Parcelable onSaveInstanceState() {
        final AppCompatSpinner.SavedState ss =
                new AppCompatSpinner.SavedState(super.onSaveInstanceState());
        ss.mShowDropdown = mPopup != null && mPopup.isShowing();
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        AppCompatSpinner.SavedState ss = (AppCompatSpinner.SavedState) state;

        super.onRestoreInstanceState(ss.getSuperState());

        if (ss.mShowDropdown) {
            ViewTreeObserver vto = getViewTreeObserver();
            if (vto != null) {
                final OnGlobalLayoutListener listener = new OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (!getInternalPopup().isShowing()) {
                            showPopup();
                        }
                        final ViewTreeObserver vto = getViewTreeObserver();
                        if (vto != null) {
                            vto.removeOnGlobalLayoutListener(this);
                        }
                    }
                };
                vto.addOnGlobalLayoutListener(listener);
            }
        }
    }

    static class SavedState extends BaseSavedState {
        boolean mShowDropdown;

        SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState(Parcel in) {
            super(in);
            mShowDropdown = in.readByte() != 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeByte((byte) (mShowDropdown ? 1 : 0));
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    /**
     * <p>Wrapper class for an Adapter. Transforms the embedded Adapter instance
     * into a ListAdapter.</p>
     */
    private static class DropDownAdapter implements ListAdapter, SpinnerAdapter {

        private SpinnerAdapter mAdapter;

        private ListAdapter mListAdapter;

        /**
         * Creates a new ListAdapter wrapper for the specified adapter.
         *
         * @param adapter       the SpinnerAdapter to transform into a ListAdapter
         * @param dropDownTheme the theme against which to inflate drop-down
         *                      views, may be {@null} to use default theme
         */
        public DropDownAdapter(@Nullable SpinnerAdapter adapter,
                Resources.@Nullable Theme dropDownTheme) {
            mAdapter = adapter;

            if (adapter instanceof ListAdapter) {
                mListAdapter = (ListAdapter) adapter;
            }

            if (dropDownTheme != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                         && adapter instanceof android.widget.ThemedSpinnerAdapter) {
                    final android.widget.ThemedSpinnerAdapter themedAdapter =
                            (android.widget.ThemedSpinnerAdapter) adapter;
                    Api23Impl.setDropDownViewTheme(themedAdapter, dropDownTheme);
                } else if (adapter instanceof ThemedSpinnerAdapter) {
                    final ThemedSpinnerAdapter themedAdapter = (ThemedSpinnerAdapter) adapter;
                    if (themedAdapter.getDropDownViewTheme() == null) {
                        themedAdapter.setDropDownViewTheme(dropDownTheme);
                    }
                }
            }
        }

        @Override
        public int getCount() {
            return mAdapter == null ? 0 : mAdapter.getCount();
        }

        @Override
        public Object getItem(int position) {
            return mAdapter == null ? null : mAdapter.getItem(position);
        }

        @Override
        public long getItemId(int position) {
            return mAdapter == null ? -1 : mAdapter.getItemId(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getDropDownView(position, convertView, parent);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return (mAdapter == null) ? null
                    : mAdapter.getDropDownView(position, convertView, parent);
        }

        @Override
        public boolean hasStableIds() {
            return mAdapter != null && mAdapter.hasStableIds();
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            if (mAdapter != null) {
                mAdapter.registerDataSetObserver(observer);
            }
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            if (mAdapter != null) {
                mAdapter.unregisterDataSetObserver(observer);
            }
        }

        /**
         * If the wrapped SpinnerAdapter is also a ListAdapter, delegate this call.
         * Otherwise, return true.
         */
        @Override
        public boolean areAllItemsEnabled() {
            final ListAdapter adapter = mListAdapter;
            if (adapter != null) {
                return adapter.areAllItemsEnabled();
            } else {
                return true;
            }
        }

        /**
         * If the wrapped SpinnerAdapter is also a ListAdapter, delegate this call.
         * Otherwise, return true.
         */
        @Override
        public boolean isEnabled(int position) {
            final ListAdapter adapter = mListAdapter;
            if (adapter != null) {
                return adapter.isEnabled(position);
            } else {
                return true;
            }
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return getCount() == 0;
        }
    }

    /**
     * Implements some sort of popup selection interface for selecting a spinner option.
     * Allows for different spinner modes.
     */
    @VisibleForTesting
    interface SpinnerPopup {
        void setAdapter(ListAdapter adapter);

        /**
         * Show the popup
         */
        void show(int textDirection, int textAlignment);

        /**
         * Dismiss the popup
         */
        void dismiss();

        /**
         * @return true if the popup is showing, false otherwise.
         */
        boolean isShowing();

        /**
         * Set hint text to be displayed to the user. This should provide
         * a description of the choice being made.
         * @param hintText Hint text to set.
         */
        void setPromptText(CharSequence hintText);
        CharSequence getHintText();

        void setBackgroundDrawable(Drawable bg);
        void setVerticalOffset(int px);
        void setHorizontalOffset(int px);
        void setHorizontalOriginalOffset(int px);
        int getHorizontalOriginalOffset();
        Drawable getBackground();
        int getVerticalOffset();
        int getHorizontalOffset();
    }

    @VisibleForTesting
    class DialogPopup implements SpinnerPopup, DialogInterface.OnClickListener {
        @VisibleForTesting
        AlertDialog mPopup;
        private ListAdapter mListAdapter;
        private CharSequence mPrompt;

        @Override
        public void dismiss() {
            if (mPopup != null) {
                mPopup.dismiss();
                mPopup = null;
            }
        }

        @Override
        public boolean isShowing() {
            return mPopup != null ? mPopup.isShowing() : false;
        }

        @Override
        public void setAdapter(ListAdapter adapter) {
            mListAdapter = adapter;
        }

        @Override
        public void setPromptText(CharSequence hintText) {
            mPrompt = hintText;
        }

        @Override
        public CharSequence getHintText() {
            return mPrompt;
        }

        @Override
        public void show(int textDirection, int textAlignment) {
            if (mListAdapter == null) {
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getPopupContext());
            if (mPrompt != null) {
                builder.setTitle(mPrompt);
            }
            mPopup = builder.setSingleChoiceItems(mListAdapter,
                    getSelectedItemPosition(), this).create();
            final ListView listView = mPopup.getListView();
            listView.setTextDirection(textDirection);
            listView.setTextAlignment(textAlignment);
            mPopup.show();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            setSelection(which);
            if (getOnItemClickListener() != null) {
                performItemClick(null, which, mListAdapter.getItemId(which));
            }
            dismiss();
        }

        @Override
        public void setBackgroundDrawable(Drawable bg) {
            Log.e(TAG, "Cannot set popup background for MODE_DIALOG, ignoring");
        }

        @Override
        public void setVerticalOffset(int px) {
            Log.e(TAG, "Cannot set vertical offset for MODE_DIALOG, ignoring");
        }

        @Override
        public void setHorizontalOffset(int px) {
            Log.e(TAG, "Cannot set horizontal offset for MODE_DIALOG, ignoring");
        }

        @Override
        public Drawable getBackground() {
            return null;
        }

        @Override
        public int getVerticalOffset() {
            return 0;
        }

        @Override
        public int getHorizontalOffset() {
            return 0;
        }

        @Override
        public void setHorizontalOriginalOffset(int px) {
            Log.e(TAG, "Cannot set horizontal (original) offset for MODE_DIALOG, ignoring");
        }

        @Override
        public int getHorizontalOriginalOffset() {
            return 0;
        }
    }

    @VisibleForTesting
    class DropdownPopup extends ListPopupWindow implements SpinnerPopup {
        private CharSequence mHintText;
        ListAdapter mAdapter;
        private final Rect mVisibleRect = new Rect();
        private int mOriginalHorizontalOffset;

        public DropdownPopup(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);

            setAnchorView(AppCompatSpinner.this);
            setModal(true);
            setPromptPosition(POSITION_PROMPT_ABOVE);

            setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    AppCompatSpinner.this.setSelection(position);
                    if (getOnItemClickListener() != null) {
                        AppCompatSpinner.this
                                .performItemClick(v, position, mAdapter.getItemId(position));
                    }
                    dismiss();
                }
            });
        }

        @Override
        public void setAdapter(ListAdapter adapter) {
            super.setAdapter(adapter);
            mAdapter = adapter;
        }

        @Override
        public CharSequence getHintText() {
            return mHintText;
        }

        @Override
        public void setPromptText(CharSequence hintText) {
            // Hint text is ignored for dropdowns, but maintain it here.
            mHintText = hintText;
        }

        void computeContentWidth() {
            final Drawable background = getBackground();
            int hOffset = 0;
            if (background != null) {
                background.getPadding(mTempRect);
                hOffset = ViewUtils.isLayoutRtl(AppCompatSpinner.this) ? mTempRect.right
                        : -mTempRect.left;
            } else {
                mTempRect.left = mTempRect.right = 0;
            }

            final int spinnerPaddingLeft = AppCompatSpinner.this.getPaddingLeft();
            final int spinnerPaddingRight = AppCompatSpinner.this.getPaddingRight();
            final int spinnerWidth = AppCompatSpinner.this.getWidth();
            if (mDropDownWidth == WRAP_CONTENT) {
                int contentWidth = compatMeasureContentWidth(
                        (SpinnerAdapter) mAdapter, getBackground());
                final int contentWidthLimit = getContext().getResources()
                        .getDisplayMetrics().widthPixels - mTempRect.left - mTempRect.right;
                if (contentWidth > contentWidthLimit) {
                    contentWidth = contentWidthLimit;
                }
                setContentWidth(Math.max(
                        contentWidth, spinnerWidth - spinnerPaddingLeft - spinnerPaddingRight));
            } else if (mDropDownWidth == MATCH_PARENT) {
                setContentWidth(spinnerWidth - spinnerPaddingLeft - spinnerPaddingRight);
            } else {
                setContentWidth(mDropDownWidth);
            }
            if (ViewUtils.isLayoutRtl(AppCompatSpinner.this)) {
                hOffset += spinnerWidth - spinnerPaddingRight - getWidth()
                        - getHorizontalOriginalOffset();
            } else {
                hOffset += spinnerPaddingLeft + getHorizontalOriginalOffset();
            }
            setHorizontalOffset(hOffset);
        }

        @Override
        public void show(int textDirection, int textAlignment) {
            final boolean wasShowing = isShowing();

            computeContentWidth();

            setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);
            super.show();
            final ListView listView = getListView();
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listView.setTextDirection(textDirection);
            listView.setTextAlignment(textAlignment);
            setSelection(AppCompatSpinner.this.getSelectedItemPosition());

            if (wasShowing) {
                // Skip setting up the layout/dismiss listener below. If we were previously
                // showing it will still stick around.
                return;
            }

            // Make sure we hide if our anchor goes away.
            // TODO: This might be appropriate to push all the way down to PopupWindow,
            // but it may have other side effects to investigate first. (Text editing handles, etc.)
            final ViewTreeObserver vto = getViewTreeObserver();
            if (vto != null) {
                final ViewTreeObserver.OnGlobalLayoutListener layoutListener
                        = new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (!isVisibleToUser(AppCompatSpinner.this)) {
                            dismiss();
                        } else {
                            computeContentWidth();

                            // Use super.show here to update; we don't want to move the selected
                            // position or adjust other things that would be reset otherwise.
                            DropdownPopup.super.show();
                        }
                    }
                };
                vto.addOnGlobalLayoutListener(layoutListener);
                setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        final ViewTreeObserver vto = getViewTreeObserver();
                        if (vto != null) {
                            vto.removeGlobalOnLayoutListener(layoutListener);
                        }
                    }
                });
            }
        }

        /**
         * Simplified version of the the hidden View.isVisibleToUser()
         */
        boolean isVisibleToUser(View view) {
            return view.isAttachedToWindow() && view.getGlobalVisibleRect(mVisibleRect);
        }

        @Override
        public void setHorizontalOriginalOffset(int px) {
            mOriginalHorizontalOffset = px;
        }

        @Override
        public int getHorizontalOriginalOffset() {
            return mOriginalHorizontalOffset;
        }
    }

    @RequiresApi(23)
    private static final class Api23Impl {
        private Api23Impl() {
            // This class is not instantiable.
        }

        static void setDropDownViewTheme(
                android.widget.@NonNull ThemedSpinnerAdapter themedSpinnerAdapter,
                Resources.@Nullable Theme theme
        ) {
            if (!ObjectsCompat.equals(themedSpinnerAdapter.getDropDownViewTheme(), theme)) {
                themedSpinnerAdapter.setDropDownViewTheme(theme);
            }
        }
    }
}
