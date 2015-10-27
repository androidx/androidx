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

package android.support.v7.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.view.TintableBackgroundView;
import android.support.v4.view.ViewCompat;
import android.support.v7.appcompat.R;
import android.support.v7.view.ContextThemeWrapper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;


/**
 * A {@link Spinner} which supports compatible features on older version of the platform,
 * including:
 * <ul>
 * <li>Allows dynamic tint of it background via the background tint methods in
 * {@link android.support.v4.view.ViewCompat}.</li>
 * <li>Allows setting of the background tint using {@link R.attr#backgroundTint} and
 * {@link R.attr#backgroundTintMode}.</li>
 * <li>Allows setting of the popups theme using {@link R.attr#popupTheme}.</li>
 * </ul>
 *
 * <p>This will automatically be used when you use {@link Spinner} in your layouts.
 * You should only need to manually use this class when writing custom views.</p>
 */
public class AppCompatSpinner extends Spinner implements TintableBackgroundView {

    private static final boolean IS_AT_LEAST_M = Build.VERSION.SDK_INT >= 23;
    private static final boolean IS_AT_LEAST_JB = Build.VERSION.SDK_INT >= 16;

    private static final int[] ATTRS_ANDROID_SPINNERMODE = {android.R.attr.spinnerMode};

    private static final int MAX_ITEMS_MEASURED = 15;

    private static final String TAG = "AppCompatSpinner";

    private static final int MODE_DIALOG = 0;
    private static final int MODE_DROPDOWN = 1;
    private static final int MODE_THEME = -1;

    private AppCompatDrawableManager mDrawableManager;

    private AppCompatBackgroundHelper mBackgroundTintHelper;

    /** Context used to inflate the popup window or dialog. */
    private Context mPopupContext;

    /** Forwarding listener used to implement drag-to-open. */
    private ListPopupWindow.ForwardingListener mForwardingListener;

    /** Temporary holder for setAdapter() calls from the super constructor. */
    private SpinnerAdapter mTempAdapter;

    private boolean mPopupSet;

    private DropdownPopup mPopup;

    private int mDropDownWidth;

    private final Rect mTempRect = new Rect();

    /**
     * Construct a new spinner with the given context's theme.
     *
     * @param context The Context the view is running in, through which it can
     *                access the current theme, resources, etc.
     */
    public AppCompatSpinner(Context context) {
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
    public AppCompatSpinner(Context context, int mode) {
        this(context, null, R.attr.spinnerStyle, mode);
    }

    /**
     * Construct a new spinner with the given context's theme and the supplied attribute set.
     *
     * @param context The Context the view is running in, through which it can
     *                access the current theme, resources, etc.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public AppCompatSpinner(Context context, AttributeSet attrs) {
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
    public AppCompatSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
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
    public AppCompatSpinner(Context context, AttributeSet attrs, int defStyleAttr, int mode) {
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
    public AppCompatSpinner(Context context, AttributeSet attrs, int defStyleAttr, int mode,
            Resources.Theme popupTheme) {
        super(context, attrs, defStyleAttr);

        TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs,
                R.styleable.Spinner, defStyleAttr, 0);

        mDrawableManager = AppCompatDrawableManager.get();
        mBackgroundTintHelper = new AppCompatBackgroundHelper(this, mDrawableManager);

        if (popupTheme != null) {
            mPopupContext = new ContextThemeWrapper(context, popupTheme);
        } else {
            final int popupThemeResId = a.getResourceId(R.styleable.Spinner_popupTheme, 0);
            if (popupThemeResId != 0) {
                mPopupContext = new ContextThemeWrapper(context, popupThemeResId);
            } else {
                // If we're running on a < M device, we'll use the current context and still handle
                // any dropdown popup
                mPopupContext = !IS_AT_LEAST_M ? context : null;
            }
        }

        if (mPopupContext != null) {
            if (mode == MODE_THEME) {
                if (Build.VERSION.SDK_INT >= 11) {
                    // If we're running on API v11+ we will try and read android:spinnerMode
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
                } else {
                    // Else, we use a default mode of dropdown
                    mode = MODE_DROPDOWN;
                }
            }

            if (mode == MODE_DROPDOWN) {
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
                mForwardingListener = new ListPopupWindow.ForwardingListener(this) {
                    @Override
                    public ListPopupWindow getPopup() {
                        return popup;
                    }

                    @Override
                    public boolean onForwardingStarted() {
                        if (!mPopup.isShowing()) {
                            mPopup.show();
                        }
                        return true;
                    }
                };
            }
        }

        final CharSequence[] entries = a.getTextArray(R.styleable.Spinner_android_entries);
        if (entries != null) {
            final ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(context,
                    R.layout.support_simple_spinner_dropdown_item, entries);
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
    public Context getPopupContext() {
        if (mPopup != null) {
            return mPopupContext;
        } else if (IS_AT_LEAST_M) {
            return super.getPopupContext();
        }
        return null;
    }

    public void setPopupBackgroundDrawable(Drawable background) {
        if (mPopup != null) {
            mPopup.setBackgroundDrawable(background);
        } else if (IS_AT_LEAST_JB) {
            super.setPopupBackgroundDrawable(background);
        }
    }

    public void setPopupBackgroundResource(@DrawableRes int resId) {
        setPopupBackgroundDrawable(getPopupContext().getDrawable(resId));
    }

    public Drawable getPopupBackground() {
        if (mPopup != null) {
            return mPopup.getBackground();
        } else if (IS_AT_LEAST_JB) {
            return super.getPopupBackground();
        }
        return null;
    }

    public void setDropDownVerticalOffset(int pixels) {
        if (mPopup != null) {
            mPopup.setVerticalOffset(pixels);
        } else if (IS_AT_LEAST_JB) {
            super.setDropDownVerticalOffset(pixels);
        }
    }

    public int getDropDownVerticalOffset() {
        if (mPopup != null) {
            return mPopup.getVerticalOffset();
        } else if (IS_AT_LEAST_JB) {
            return super.getDropDownVerticalOffset();
        }
        return 0;
    }

    public void setDropDownHorizontalOffset(int pixels) {
        if (mPopup != null) {
            mPopup.setHorizontalOffset(pixels);
        } else if (IS_AT_LEAST_JB) {
            super.setDropDownHorizontalOffset(pixels);
        }
    }

    /**
     * Get the configured horizontal offset in pixels for the spinner's popup window of choices.
     * Only valid in {@link #MODE_DROPDOWN}; other modes will return 0.
     *
     * @return Horizontal offset in pixels
     */
    public int getDropDownHorizontalOffset() {
        if (mPopup != null) {
            return mPopup.getHorizontalOffset();
        } else if (IS_AT_LEAST_JB) {
            return super.getDropDownHorizontalOffset();
        }
        return 0;
    }

    public void setDropDownWidth(int pixels) {
        if (mPopup != null) {
            mDropDownWidth = pixels;
        } else if (IS_AT_LEAST_JB) {
            super.setDropDownWidth(pixels);
        }
    }

    public int getDropDownWidth() {
        if (mPopup != null) {
            return mDropDownWidth;
        } else if (IS_AT_LEAST_JB) {
            return super.getDropDownWidth();
        }
        return 0;
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
        if (mPopup != null && !mPopup.isShowing()) {
            mPopup.show();
            return true;
        }
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
    public void setBackgroundDrawable(Drawable background) {
        super.setBackgroundDrawable(background);
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.onSetBackgroundDrawable(background);
        }
    }

    /**
     * This should be accessed via
     * {@link android.support.v4.view.ViewCompat#setBackgroundTintList(android.view.View,
     * ColorStateList)}
     *
     * @hide
     */
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
    @Override
    @Nullable
    public ColorStateList getSupportBackgroundTintList() {
        return mBackgroundTintHelper != null
                ? mBackgroundTintHelper.getSupportBackgroundTintList() : null;
    }

    /**
     * This should be accessed via
     * {@link android.support.v4.view.ViewCompat#setBackgroundTintMode(android.view.View,
     * PorterDuff.Mode)}
     *
     * @hide
     */
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
    @Override
    @Nullable
    public PorterDuff.Mode getSupportBackgroundTintMode() {
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

    private int compatMeasureContentWidth(SpinnerAdapter adapter, Drawable background) {
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
                @Nullable Resources.Theme dropDownTheme) {
            mAdapter = adapter;

            if (adapter instanceof ListAdapter) {
                mListAdapter = (ListAdapter) adapter;
            }

            if (dropDownTheme != null) {
                 if (IS_AT_LEAST_M && adapter instanceof android.widget.ThemedSpinnerAdapter) {
                    final android.widget.ThemedSpinnerAdapter themedAdapter =
                            (android.widget.ThemedSpinnerAdapter) adapter;
                    if (themedAdapter.getDropDownViewTheme() != dropDownTheme) {
                        themedAdapter.setDropDownViewTheme(dropDownTheme);
                    }
                } else if (adapter instanceof ThemedSpinnerAdapter) {
                    final ThemedSpinnerAdapter themedAdapter = (ThemedSpinnerAdapter) adapter;
                    if (themedAdapter.getDropDownViewTheme() == null) {
                        themedAdapter.setDropDownViewTheme(dropDownTheme);
                    }
                }
            }
        }

        public int getCount() {
            return mAdapter == null ? 0 : mAdapter.getCount();
        }

        public Object getItem(int position) {
            return mAdapter == null ? null : mAdapter.getItem(position);
        }

        public long getItemId(int position) {
            return mAdapter == null ? -1 : mAdapter.getItemId(position);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            return getDropDownView(position, convertView, parent);
        }

        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return (mAdapter == null) ? null
                    : mAdapter.getDropDownView(position, convertView, parent);
        }

        public boolean hasStableIds() {
            return mAdapter != null && mAdapter.hasStableIds();
        }

        public void registerDataSetObserver(DataSetObserver observer) {
            if (mAdapter != null) {
                mAdapter.registerDataSetObserver(observer);
            }
        }

        public void unregisterDataSetObserver(DataSetObserver observer) {
            if (mAdapter != null) {
                mAdapter.unregisterDataSetObserver(observer);
            }
        }

        /**
         * If the wrapped SpinnerAdapter is also a ListAdapter, delegate this call.
         * Otherwise, return true.
         */
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
        public boolean isEnabled(int position) {
            final ListAdapter adapter = mListAdapter;
            if (adapter != null) {
                return adapter.isEnabled(position);
            } else {
                return true;
            }
        }

        public int getItemViewType(int position) {
            return 0;
        }

        public int getViewTypeCount() {
            return 1;
        }

        public boolean isEmpty() {
            return getCount() == 0;
        }
    }

    private class DropdownPopup extends ListPopupWindow {
        private CharSequence mHintText;
        private ListAdapter mAdapter;
        private final Rect mVisibleRect = new Rect();

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

        public CharSequence getHintText() {
            return mHintText;
        }

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
                hOffset += spinnerWidth - spinnerPaddingRight - getWidth();
            } else {
                hOffset += spinnerPaddingLeft;
            }
            setHorizontalOffset(hOffset);
        }

        public void show() {
            final boolean wasShowing = isShowing();

            computeContentWidth();

            setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);
            super.show();
            final ListView listView = getListView();
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
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
        private boolean isVisibleToUser(View view) {
            return ViewCompat.isAttachedToWindow(view) && view.getGlobalVisibleRect(mVisibleRect);
        }
    }
}
