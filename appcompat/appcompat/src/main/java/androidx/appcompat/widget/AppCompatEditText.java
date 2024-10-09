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
import static androidx.appcompat.widget.AppCompatReceiveContentHelper.maybeHandleDragEventViaPerformReceiveContent;
import static androidx.appcompat.widget.AppCompatReceiveContentHelper.maybeHandleMenuActionViaPerformReceiveContent;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Editable;
import android.text.method.KeyListener;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.DragEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.textclassifier.TextClassifier;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.appcompat.R;
import androidx.core.view.ContentInfoCompat;
import androidx.core.view.OnReceiveContentListener;
import androidx.core.view.OnReceiveContentViewBehavior;
import androidx.core.view.TintableBackgroundView;
import androidx.core.view.ViewCompat;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.widget.TextViewCompat;
import androidx.core.widget.TextViewOnReceiveContentListener;
import androidx.core.widget.TintableCompoundDrawablesView;
import androidx.resourceinspection.annotation.AppCompatShadowedAttributes;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A {@link EditText} which supports compatible features on older versions of the platform,
 * including:
 * <ul>
 *     <li>Allows dynamic tint of its background via the background tint methods in
 *     {@link androidx.core.view.ViewCompat}.</li>
 *     <li>Allows setting of the background tint using {@link R.attr#backgroundTint} and
 *     {@link R.attr#backgroundTintMode}.</li>
 *     <li>Allows setting a custom {@link OnReceiveContentListener listener} to handle
 *     insertion of content (e.g. pasting text or an image from the clipboard). This listener
 *     provides the opportunity to implement app-specific handling such as creating an attachment
 *     when an image is pasted.</li>
 * </ul>
 *
 * <p>This will automatically be used when you use {@link EditText} in your layouts
 * and the top-level activity / dialog is provided by
 * <a href="{@docRoot}topic/libraries/support-library/packages.html#v7-appcompat">appcompat</a>.
 * You should only need to manually use this class when writing custom views.</p>
 */
@AppCompatShadowedAttributes
public class AppCompatEditText extends EditText implements TintableBackgroundView,
        OnReceiveContentViewBehavior, EmojiCompatConfigurationView, TintableCompoundDrawablesView {

    private final AppCompatBackgroundHelper mBackgroundTintHelper;
    private final AppCompatTextHelper mTextHelper;
    private final AppCompatTextClassifierHelper mTextClassifierHelper;
    private final TextViewOnReceiveContentListener mDefaultOnReceiveContentListener;
    private final @NonNull AppCompatEmojiEditTextHelper mAppCompatEmojiEditTextHelper;
    private @Nullable SuperCaller mSuperCaller;

    public AppCompatEditText(@NonNull Context context) {
        this(context, null);
    }

    public AppCompatEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.editTextStyle);
    }

    public AppCompatEditText(
            @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(TintContextWrapper.wrap(context), attrs, defStyleAttr);

        ThemeUtils.checkAppCompatTheme(this, getContext());

        mBackgroundTintHelper = new AppCompatBackgroundHelper(this);
        mBackgroundTintHelper.loadFromAttributes(attrs, defStyleAttr);

        mTextHelper = new AppCompatTextHelper(this);
        mTextHelper.loadFromAttributes(attrs, defStyleAttr);
        mTextHelper.applyCompoundDrawablesTints();

        mTextClassifierHelper = new AppCompatTextClassifierHelper(this);

        mDefaultOnReceiveContentListener = new TextViewOnReceiveContentListener();
        mAppCompatEmojiEditTextHelper = new AppCompatEmojiEditTextHelper(this);
        mAppCompatEmojiEditTextHelper.loadFromAttributes(attrs, defStyleAttr);
        initEmojiKeyListener(mAppCompatEmojiEditTextHelper);
    }

    /**
     * Call from the constructor to safely add KeyListener for emoji2.
     *
     * This will always call super methods to avoid leaking a partially constructed this to
     * overrides of non-final methods.
     *
     * @param appCompatEmojiEditTextHelper emojicompat helper
     */
    void initEmojiKeyListener(AppCompatEmojiEditTextHelper appCompatEmojiEditTextHelper) {
        // setKeyListener will cause a reset both focusable and the inputType to the most basic
        // style for the key listener. Since we're calling this from the View constructor, this
        // will cause both focusable and inputType to reset from the XML attributes.
        // See: b/191061070 and b/188049943 for details
        //
        // We will only reset this during ctor invocation, and default to the platform behavior
        // for later calls to setKeyListener, to emulate the exact behavior that a regular
        // EditText would provide.
        //
        // Since we're calling non-final methods from a ctor (setKeyListener, setRawInputType,
        // setFocusable) move this out of AppCompatEmojiEditTextHelper and into the respective
        // views to ensure we only call the super methods during construction  (b/208480173).
        KeyListener currentKeyListener = getKeyListener();
        if (appCompatEmojiEditTextHelper.isEmojiCapableKeyListener(currentKeyListener)) {
            boolean wasFocusable = super.isFocusable();
            boolean wasClickable = super.isClickable();
            boolean wasLongClickable = super.isLongClickable();
            int inputType = super.getInputType();
            KeyListener wrappedKeyListener = appCompatEmojiEditTextHelper.getKeyListener(
                    currentKeyListener);
            // don't call parent setKeyListener if it's not wrapped
            if (wrappedKeyListener == currentKeyListener) return;
            super.setKeyListener(wrappedKeyListener);
            // reset the input type and focusable attributes after calling setKeyListener
            super.setRawInputType(inputType);
            super.setFocusable(wasFocusable);
            super.setClickable(wasClickable);
            super.setLongClickable(wasLongClickable);
        }
    }

    /**
     * Return the text that the view is displaying. If an editable text has not been set yet, this
     * will return null.
     */
    @Override
    public @Nullable Editable getText() {
        if (Build.VERSION.SDK_INT >= 28) {
            return super.getText();
        }
        // A bug pre-P makes getText() crash if called before the first setText due to a cast, so
        // retrieve the editable text.
        return super.getEditableText();
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
     * {@link androidx.core.view.ViewCompat#setBackgroundTintList(android.view.View, ColorStateList)}
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
     * {@link androidx.core.view.ViewCompat#setBackgroundTintMode(android.view.View, PorterDuff.Mode)}
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
        if (mTextHelper != null) {
            mTextHelper.applyCompoundDrawablesTints();
        }
    }

    @Override
    public void setTextAppearance(Context context, int resId) {
        super.setTextAppearance(context, resId);
        if (mTextHelper != null) {
            mTextHelper.onSetTextAppearance(context, resId);
        }
    }

    /**
     * If a {@link ViewCompat#setOnReceiveContentListener listener is set}, the returned
     * {@link InputConnection} will use it to handle calls to {@link InputConnection#commitContent}.
     *
     * {@inheritDoc}
     */
    @Override
    public @Nullable InputConnection onCreateInputConnection(@NonNull EditorInfo outAttrs) {
        InputConnection ic = super.onCreateInputConnection(outAttrs);
        mTextHelper.populateSurroundingTextIfNeeded(this, ic, outAttrs);
        ic = AppCompatHintHelper.onCreateInputConnection(ic, outAttrs, this);

        // On SDK 30 and below, we manually configure the InputConnection here to use
        // ViewCompat.performReceiveContent. On S and above, the platform's BaseInputConnection
        // implementation calls View.performReceiveContent by default.
        if (ic != null && Build.VERSION.SDK_INT <= 30) {
            String[] mimeTypes = ViewCompat.getOnReceiveContentMimeTypes(this);
            if (mimeTypes != null) {
                EditorInfoCompat.setContentMimeTypes(outAttrs, mimeTypes);
                ic = InputConnectionCompat.createWrapper(this, ic, outAttrs);
            }
        }
        return mAppCompatEmojiEditTextHelper.onCreateInputConnection(ic, outAttrs);
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

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (Build.VERSION.SDK_INT >= 30 && Build.VERSION.SDK_INT < 33) {
            final InputMethodManager imm = (InputMethodManager) getContext().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            // Calling isActive() here implied a checkFocus() call to update the active served
            // view for input method. This is a backport for mServedView was detached, but the
            // next served view gets mistakenly cleared as well.
            // https://android.googlesource.com/platform/frameworks/base/+/734613a500fb
            imm.isActive(this);
        }
    }

    @UiThread
    @RequiresApi(26)
    private @NonNull SuperCaller getSuperCaller() {
        if (mSuperCaller == null) {
            mSuperCaller = new SuperCaller();
        }
        return mSuperCaller;
    }

    /**
     * Sets the {@link TextClassifier} for this TextView.
     */
    @Override
    @RequiresApi(api = 26)
    public void setTextClassifier(@Nullable TextClassifier textClassifier) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P || mTextClassifierHelper == null) {
            getSuperCaller().setTextClassifier(textClassifier);
            return;
        }
        mTextClassifierHelper.setTextClassifier(textClassifier);
    }

    /**
     * Returns the {@link TextClassifier} used by this TextView.
     * If no TextClassifier has been set, this TextView uses the default set by the
     * {@link android.view.textclassifier.TextClassificationManager}.
     */
    @Override
    @RequiresApi(api = 26)
    public @NonNull TextClassifier getTextClassifier() {
        // The null check is necessary because getTextClassifier is called when we are invoking
        // the super class's constructor.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P || mTextClassifierHelper == null) {
            return getSuperCaller().getTextClassifier();
        }
        return mTextClassifierHelper.getTextClassifier();
    }

    @Override
    public boolean onDragEvent(@SuppressWarnings("MissingNullability") DragEvent event) {
        if (maybeHandleDragEventViaPerformReceiveContent(this, event)) {
            return true;
        }
        return super.onDragEvent(event);
    }

    /**
     * If a {@link ViewCompat#setOnReceiveContentListener listener is set}, uses it to execute the
     * "Paste" and "Paste as plain text" menu actions.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean onTextContextMenuItem(int id) {
        if (maybeHandleMenuActionViaPerformReceiveContent(this, id)) {
            return true;
        }
        return super.onTextContextMenuItem(id);
    }

    /**
     * Implements the default behavior for receiving content, which coerces all content to text
     * and inserts into the view.
     *
     * <p>IMPORTANT: This method is provided to enable custom widgets that extend this class
     * to customize the default behavior for receiving content. Apps wishing to provide custom
     * behavior for receiving content should not override this method, but rather should set
     * a listener via {@link ViewCompat#setOnReceiveContentListener}. App code wishing to inject
     * content into this view should not call this method directly, but rather should invoke
     * {@link ViewCompat#performReceiveContent}.
     *
     * @param payload The content to insert and related metadata.
     *
     * @return The portion of the passed-in content that was not handled (may be all, some, or none
     * of the passed-in content).
     */
    @Override
    public @Nullable ContentInfoCompat onReceiveContent(@NonNull ContentInfoCompat payload) {
        return mDefaultOnReceiveContentListener.onReceiveContent(this, payload);
    }

    /**
     * Adds EmojiCompat KeyListener to correctly edit multi-codepoint emoji when they've been
     * converted to spans.
     *
     * {@inheritDoc}
     */
    @Override
    public void setKeyListener(@Nullable KeyListener keyListener) {
        super.setKeyListener(mAppCompatEmojiEditTextHelper.getKeyListener(keyListener));
    }

    @Override
    public void setEmojiCompatEnabled(boolean enabled) {
        mAppCompatEmojiEditTextHelper.setEnabled(enabled);
    }

    @Override
    public boolean isEmojiCompatEnabled() {
        return mAppCompatEmojiEditTextHelper.isEnabled();
    }

    @Override
    public void setCompoundDrawables(@Nullable Drawable left, @Nullable Drawable top,
            @Nullable Drawable right, @Nullable Drawable bottom) {
        super.setCompoundDrawables(left, top, right, bottom);
        if (mTextHelper != null) {
            mTextHelper.onSetCompoundDrawables();
        }
    }

    @Override
    public void setCompoundDrawablesRelative(@Nullable Drawable start, @Nullable Drawable top,
            @Nullable Drawable end, @Nullable Drawable bottom) {
        super.setCompoundDrawablesRelative(start, top, end, bottom);
        if (mTextHelper != null) {
            mTextHelper.onSetCompoundDrawables();
        }
    }

    /**
     * This should be accessed via
     * {@link androidx.core.widget.TextViewCompat#getCompoundDrawableTintList(TextView)}
     *
     * @return the tint applied to the compound drawables
     * @attr ref androidx.appcompat.R.styleable#AppCompatTextView_drawableTint
     * @see #setSupportCompoundDrawablesTintList(ColorStateList)
     *
     */
    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public @Nullable ColorStateList getSupportCompoundDrawablesTintList() {
        return mTextHelper.getCompoundDrawableTintList();
    }

    /**
     * This should be accessed via {@link
     * androidx.core.widget.TextViewCompat#setCompoundDrawableTintList(TextView, ColorStateList)}
     *
     * Applies a tint to the compound drawables. Does not modify the current tint mode, which is
     * {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link #setCompoundDrawables(Drawable, Drawable, Drawable, Drawable)} and
     * related methods will automatically mutate the drawables and apply the specified tint and tint
     * mode using {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param tintList the tint to apply, may be {@code null} to clear tint
     * @attr ref androidx.appcompat.R.styleable#AppCompatTextView_drawableTint
     * @see #getSupportCompoundDrawablesTintList()
     *
     */
    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public void setSupportCompoundDrawablesTintList(@Nullable ColorStateList tintList) {
        mTextHelper.setCompoundDrawableTintList(tintList);
        mTextHelper.applyCompoundDrawablesTints();
    }

    /**
     * This should be accessed via
     * {@link androidx.core.widget.TextViewCompat#getCompoundDrawableTintMode(TextView)}
     *
     * Returns the blending mode used to apply the tint to the compound drawables, if specified.
     *
     * @return the blending mode used to apply the tint to the compound drawables
     * @attr ref androidx.appcompat.R.styleable#AppCompatTextView_drawableTintMode
     * @see #setSupportCompoundDrawablesTintMode(PorterDuff.Mode)
     *
     */
    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public PorterDuff.@Nullable Mode getSupportCompoundDrawablesTintMode() {
        return mTextHelper.getCompoundDrawableTintMode();
    }

    /**
     * This should be accessed via {@link
     * androidx.core.widget.TextViewCompat#setCompoundDrawableTintMode(TextView, PorterDuff.Mode)}
     *
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setSupportCompoundDrawablesTintList(ColorStateList)} to the compound drawables. The
     * default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be {@code null} to clear tint
     * @attr ref androidx.appcompat.R.styleable#AppCompatTextView_drawableTintMode
     * @see #setSupportCompoundDrawablesTintList(ColorStateList)
     *
     */
    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public void setSupportCompoundDrawablesTintMode(PorterDuff.@Nullable Mode tintMode) {
        mTextHelper.setCompoundDrawableTintMode(tintMode);
        mTextHelper.applyCompoundDrawablesTints();
    }

    @RequiresApi(api = 26)
    class SuperCaller {

        public @Nullable TextClassifier getTextClassifier() {
            return AppCompatEditText.super.getTextClassifier();
        }

        public void setTextClassifier(TextClassifier textClassifier) {
            AppCompatEditText.super.setTextClassifier(textClassifier);
        }
    }
}
