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
import static androidx.core.widget.RichContentReceiverCompat.FLAG_CONVERT_TO_PLAIN_TEXT;
import static androidx.core.widget.RichContentReceiverCompat.SOURCE_CLIPBOARD;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.textclassifier.TextClassifier;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appcompat.R;
import androidx.core.view.TintableBackgroundView;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.widget.RichContentReceiverCompat;
import androidx.core.widget.TextViewCompat;

/**
 * A {@link EditText} which supports compatible features on older versions of the platform,
 * including:
 * <ul>
 *     <li>Allows dynamic tint of its background via the background tint methods in
 *     {@link androidx.core.view.ViewCompat}.</li>
 *     <li>Allows setting of the background tint using {@link R.attr#backgroundTint} and
 *     {@link R.attr#backgroundTintMode}.</li>
 *     <li>Allows setting a custom {@link RichContentReceiverCompat receiver callback} in order to
 *     handle insertion of content (e.g. pasting text or an image from the clipboard). This callback
 *     provides the opportunity to implement app-specific handling such as creating an attachment
 *     when an image is pasted.</li>
 * </ul>
 *
 * <p>This will automatically be used when you use {@link EditText} in your layouts
 * and the top-level activity / dialog is provided by
 * <a href="{@docRoot}topic/libraries/support-library/packages.html#v7-appcompat">appcompat</a>.
 * You should only need to manually use this class when writing custom views.</p>
 */
public class AppCompatEditText extends EditText implements TintableBackgroundView {

    private final AppCompatBackgroundHelper mBackgroundTintHelper;
    private final AppCompatTextHelper mTextHelper;
    private final AppCompatTextClassifierHelper mTextClassifierHelper;
    @Nullable
    private RichContentReceiverCompat<TextView> mRichContentReceiverCompat;

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
    }

    /**
     * Return the text that the view is displaying. If an editable text has not been set yet, this
     * will return null.
     */
    @Override
    @Nullable public Editable getText() {
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
     * @hide
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
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    @Nullable
    public ColorStateList getSupportBackgroundTintList() {
        return mBackgroundTintHelper != null
                ? mBackgroundTintHelper.getSupportBackgroundTintList() : null;
    }

    /**
     * This should be accessed via
     * {@link androidx.core.view.ViewCompat#setBackgroundTintMode(android.view.View, PorterDuff.Mode)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public void setSupportBackgroundTintMode(@Nullable PorterDuff.Mode tintMode) {
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.setSupportBackgroundTintMode(tintMode);
        }
    }

    /**
     * This should be accessed via
     * {@link androidx.core.view.ViewCompat#getBackgroundTintMode(android.view.View)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
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
     * If a {@link #setRichContentReceiverCompat receiver callback} is set, the returned
     * {@link InputConnection} will use it to handle calls to {@link InputConnection#commitContent}.
     *
     * {@inheritDoc}
     */
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection ic = super.onCreateInputConnection(outAttrs);
        ic = AppCompatHintHelper.onCreateInputConnection(ic, outAttrs, this);
        if (ic != null && mRichContentReceiverCompat != null) {
            mRichContentReceiverCompat.populateEditorInfoContentMimeTypes(ic, outAttrs);
            InputConnectionCompat.OnCommitContentListener callback =
                    mRichContentReceiverCompat.buildOnCommitContentListener(this);
            ic = InputConnectionCompat.createWrapper(ic, outAttrs, callback);
        }
        return ic;
    }

    /**
     * See
     * {@link TextViewCompat#setCustomSelectionActionModeCallback(TextView, ActionMode.Callback)}
     */
    @Override
    public void setCustomSelectionActionModeCallback(ActionMode.Callback actionModeCallback) {
        super.setCustomSelectionActionModeCallback(TextViewCompat
                .wrapCustomSelectionActionModeCallback(this, actionModeCallback));
    }

    /**
     * Sets the {@link TextClassifier} for this TextView.
     */
    @Override
    @RequiresApi(api = 26)
    public void setTextClassifier(@Nullable TextClassifier textClassifier) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P || mTextClassifierHelper == null) {
            super.setTextClassifier(textClassifier);
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
    @NonNull
    @RequiresApi(api = 26)
    public TextClassifier getTextClassifier() {
        // The null check is necessary because getTextClassifier is called when we are invoking
        // the super class's constructor.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P || mTextClassifierHelper == null) {
            return super.getTextClassifier();
        }
        return mTextClassifierHelper.getTextClassifier();
    }

    /**
     * If a {@link #setRichContentReceiverCompat receiver callback} is set, uses it to execute the
     * "Paste" and "Paste as plain text" menu actions.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean onTextContextMenuItem(int id) {
        if (mRichContentReceiverCompat == null) {
            return super.onTextContextMenuItem(id);
        }
        if (id == android.R.id.paste || id == android.R.id.pasteAsPlainText) {
            ClipboardManager cm = (ClipboardManager) getContext().getSystemService(
                    Context.CLIPBOARD_SERVICE);
            ClipData clip = cm == null ? null : cm.getPrimaryClip();
            if (clip != null) {
                int flags = (id == android.R.id.paste) ? 0 : FLAG_CONVERT_TO_PLAIN_TEXT;
                mRichContentReceiverCompat.onReceive(this, clip, SOURCE_CLIPBOARD, flags);
            }
            return true;
        }
        return super.onTextContextMenuItem(id);
    }

    /**
     * Returns the callback that handles insertion of content into this view (e.g. pasting from
     * the clipboard). See {@link #setRichContentReceiverCompat} for more info.
     *
     * @return The callback that this view is using to handle insertion of content. Returns
     * {@code null} if no callback is configured, in which case the platform behavior of the
     * {@link EditText} component will be used for content insertion.
     */
    @Nullable
    public RichContentReceiverCompat<TextView> getRichContentReceiverCompat() {
        return mRichContentReceiverCompat;
    }

    /**
     * Sets the callback to handle insertion of content into this view.
     *
     * <p>"Content" and "rich content" here refers to both text and non-text: plain text, styled
     * text, HTML, images, videos, audio files, etc. The callback configured here should typically
     * extend from {@link androidx.core.widget.TextViewRichContentReceiverCompat} to provide
     * consistent behavior for text content.
     *
     * <p>This callback will be invoked for the following scenarios:
     * <ol>
     *     <li>Paste from the clipboard (e.g. "Paste" or "Paste as plain text" action in the
     *     insertion/selection menu)
     *     <li>Content insertion from the keyboard ({@link InputConnection#commitContent})
     * </ol>
     *
     * @param receiver The callback to use. This can be {@code null} to clear any previously set
     *                 callback (the platform behavior of the {@link EditText} component will then
     *                 be used).
     */
    public void setRichContentReceiverCompat(
            @Nullable RichContentReceiverCompat<TextView> receiver) {
        mRichContentReceiverCompat = receiver;
    }
}
