/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.view.platform;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.TextInputSession;
import androidx.compose.remote.core.TextInputSupport;
import androidx.compose.remote.core.operations.layout.managers.CoreText;
import androidx.compose.remote.core.operations.layout.managers.TextInputLayout;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * {@link TextInputSupport} for the View player: overlays an {@link EditText} on the host {@link
 * FrameLayout} over the session bounds (the text component displaying the edited string, or the
 * whole text input layout), configured from the layout's input options and styled from the text
 * component's attributes. Edits are written back into the document on every
 * keystroke (or on the IME action with {@code FLAG_COMMIT_ON_DONE}); the editor is dismissed on the
 * IME action, on focus loss, or when the user touches outside of it.
 */
@RestrictTo(LIBRARY_GROUP)
public class AndroidTextInputSupport implements TextInputSupport {
    private final @NonNull FrameLayout mHost;
    private @Nullable EditText mEditor;
    private @Nullable TextInputSession mSession;
    private boolean mDismissing = false;

    public AndroidTextInputSupport(@NonNull FrameLayout host) {
        mHost = host;
    }

    /** Returns true while an editor is shown. */
    public boolean isEditing() {
        return mSession != null && mSession.isActive();
    }

    @Override
    public void startTextInput(@NonNull TextInputSession session) {
        dismiss(true);
        mSession = session;
        Context context = mHost.getContext();
        TextInputLayout op = session.getLayout();
        EditText editor = new EditText(context);
        mEditor = editor;

        editor.setBackground(null);
        editor.setIncludeFontPadding(false);
        editor.setInputType(inputType(op));
        editor.setImeOptions(imeOptions(op));
        editor.setFilters(inputFilters(op));
        String autofillHint = autofillHint(op);
        if (autofillHint != null) {
            editor.setAutofillHints(autofillHint);
        }
        String hint = session.getHint();
        if (hint != null) {
            editor.setHint(hint);
        }
        applyStyle(editor, session);
        editor.setText(session.getText());
        editor.setSelection(editor.getText().length());

        editor.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

                    @Override
                    public void onTextChanged(CharSequence s, int st, int b, int c) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (mSession == session
                                && session.isActive()
                                && !op.hasInputFlag(TextInputLayout.FLAG_COMMIT_ON_DONE)) {
                            session.setText(s.toString());
                            // Schedule the frame that shows the new value elsewhere in the doc.
                            mHost.invalidate();
                        }
                    }
                });
        boolean singleLine = op.hasInputFlag(TextInputLayout.FLAG_SINGLE_LINE);
        editor.setOnEditorActionListener(
                (v, actionId, event) -> {
                    // A single line field ends on Enter (hardware keyboard or IME action key);
                    // a multi-line field keeps Enter as a newline and only ends on an IME action.
                    boolean enter =
                            singleLine
                                    && event != null
                                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                                    && event.getAction() == KeyEvent.ACTION_DOWN;
                    if (actionId != EditorInfo.IME_ACTION_UNSPECIFIED || enter) {
                        dismiss(true);
                        return true;
                    }
                    return false;
                });
        editor.setOnFocusChangeListener(
                (v, hasFocus) -> {
                    if (!hasFocus && mEditor == v) {
                        dismiss(true);
                    }
                });

        mHost.addView(editor, layoutParams(session));
        editor.requestFocus();
        editor.post(
                () -> {
                    InputMethodManager imm =
                            (InputMethodManager)
                                    context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null && mEditor == editor) {
                        imm.showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
    }

    @Override
    public void updateTextInputBounds(@NonNull TextInputSession session) {
        if (mSession != session || mEditor == null) {
            return;
        }
        mEditor.setLayoutParams(layoutParams(session));
    }

    @Override
    public void stopTextInput(@NonNull TextInputSession session) {
        if (mSession != session) {
            return;
        }
        // The document ends the session itself; just drop the editor.
        removeEditor();
        mSession = null;
    }

    /**
     * Called by the host view on touch down; ends the session when the touch lands outside the
     * editor.
     *
     * @param x touch x in host coordinates
     * @param y touch y in host coordinates
     * @return true if the touch was consumed to dismiss the editor
     */
    public boolean onTouchDown(float x, float y) {
        EditText editor = mEditor;
        if (editor == null || !isEditing()) {
            return false;
        }
        boolean inside =
                x >= editor.getLeft()
                        && x < editor.getRight()
                        && y >= editor.getTop()
                        && y < editor.getBottom();
        if (!inside) {
            dismiss(true);
        }
        return false;
    }

    /** Dismisses the editor and ends the active session, if any, committing the text first. */
    public void stopTextInput() {
        dismiss(true);
    }

    /** Dismisses the editor and ends the session, committing the text first if requested. */
    private void dismiss(boolean commit) {
        if (mDismissing) {
            return;
        }
        mDismissing = true;
        try {
            TextInputSession session = mSession;
            EditText editor = mEditor;
            if (session != null && session.isActive() && editor != null && commit) {
                session.setText(editor.getText().toString());
            }
            removeEditor();
            if (session != null) {
                session.end();
            }
            mSession = null;
            // Ending the session only flags the document as needing a repaint; nothing schedules
            // a frame from an IME callback, so request one to paint the text component the layout
            // hid while the editor was showing (whether or not the text changed).
            mHost.invalidate();
        } finally {
            mDismissing = false;
        }
    }

    private void removeEditor() {
        EditText editor = mEditor;
        mEditor = null;
        if (editor == null) {
            return;
        }
        InputMethodManager imm =
                (InputMethodManager)
                        mHost.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(editor.getWindowToken(), 0);
        }
        editor.setOnFocusChangeListener(null);
        mHost.removeView(editor);
    }

    private static FrameLayout.@NonNull LayoutParams layoutParams(
            @NonNull TextInputSession session) {
        float[] bounds = session.getBounds();
        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        Math.max(1, Math.round(bounds[2])), Math.max(1, Math.round(bounds[3])));
        params.leftMargin = Math.round(bounds[0]);
        params.topMargin = Math.round(bounds[1]);
        return params;
    }

    /**
     * Matches the editor's text style to the text component displaying the edited string, when
     * there is one; the editor keeps its defaults otherwise.
     */
    private static void applyStyle(@NonNull EditText editor, @NonNull TextInputSession session) {
        CoreText text = session.getTextComponent();
        if (text == null) {
            editor.setPadding(0, 0, 0, 0);
            return;
        }
        float fontSizePx = text.getFontSizeValue();
        int color = text.getColorValue();
        float weight = text.getFontWeightValue();
        int align = text.getTextAlignValue();
        editor.setPadding(
                Math.round(text.getPaddingLeft()),
                Math.round(text.getPaddingTop()),
                Math.round(text.getPaddingRight()),
                Math.round(text.getPaddingBottom()));
        if (fontSizePx > 0f) {
            editor.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSizePx);
        }
        editor.setTextColor(color);
        editor.setHintTextColor((color & 0x00FFFFFF) | 0x66000000);
        boolean bold = weight >= 600f;
        boolean italic = text.getFontStyle() == 1;
        int typefaceStyle;
        if (bold && italic) {
            typefaceStyle = Typeface.BOLD_ITALIC;
        } else if (bold) {
            typefaceStyle = Typeface.BOLD;
        } else if (italic) {
            typefaceStyle = Typeface.ITALIC;
        } else {
            typefaceStyle = Typeface.NORMAL;
        }
        editor.setTypeface(Typeface.DEFAULT, typefaceStyle);
        int gravity;
        switch (align) {
            case CoreText.TEXT_ALIGN_CENTER:
                gravity = Gravity.CENTER_HORIZONTAL;
                break;
            case CoreText.TEXT_ALIGN_RIGHT:
            case CoreText.TEXT_ALIGN_END:
                gravity = Gravity.END;
                break;
            default:
                gravity = Gravity.START;
        }
        boolean singleLine = session.getLayout().hasInputFlag(TextInputLayout.FLAG_SINGLE_LINE);
        editor.setGravity(gravity | (singleLine ? Gravity.CENTER_VERTICAL : Gravity.TOP));
    }

    /** Maps the layout's keyboard type and flags to an Android {@link InputType}. */
    static int inputType(@NonNull TextInputLayout op) {
        int type;
        switch (op.getKeyboardType()) {
            case TextInputLayout.KEYBOARD_NUMBER:
                type = InputType.TYPE_CLASS_NUMBER;
                break;
            case TextInputLayout.KEYBOARD_DECIMAL:
                type =
                        InputType.TYPE_CLASS_NUMBER
                                | InputType.TYPE_NUMBER_FLAG_DECIMAL
                                | InputType.TYPE_NUMBER_FLAG_SIGNED;
                break;
            case TextInputLayout.KEYBOARD_NUMBER_PASSWORD:
                type = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD;
                break;
            case TextInputLayout.KEYBOARD_PHONE:
                type = InputType.TYPE_CLASS_PHONE;
                break;
            case TextInputLayout.KEYBOARD_URI:
                type = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI;
                break;
            case TextInputLayout.KEYBOARD_EMAIL:
                type = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
                break;
            case TextInputLayout.KEYBOARD_PASSWORD:
                type = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
                break;
            case TextInputLayout.KEYBOARD_ASCII:
                type = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
                break;
            case TextInputLayout.KEYBOARD_TEXT:
            case TextInputLayout.KEYBOARD_UNSPECIFIED:
            default:
                type = InputType.TYPE_CLASS_TEXT;
        }
        if ((type & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT) {
            if (!op.hasInputFlag(TextInputLayout.FLAG_SINGLE_LINE)) {
                type |= InputType.TYPE_TEXT_FLAG_MULTI_LINE;
            }
            if (op.hasInputFlag(TextInputLayout.FLAG_AUTO_CORRECT)) {
                type |= InputType.TYPE_TEXT_FLAG_AUTO_CORRECT;
            }
            if (op.hasInputFlag(TextInputLayout.FLAG_CAPITALIZE_CHARACTERS)) {
                type |= InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS;
            } else if (op.hasInputFlag(TextInputLayout.FLAG_CAPITALIZE_WORDS)) {
                type |= InputType.TYPE_TEXT_FLAG_CAP_WORDS;
            } else if (op.hasInputFlag(TextInputLayout.FLAG_CAPITALIZE_SENTENCES)) {
                type |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
            }
        }
        return type;
    }

    /** Maps the layout's IME action to {@link EditorInfo} options. */
    static int imeOptions(@NonNull TextInputLayout op) {
        int action;
        switch (op.getImeAction()) {
            case TextInputLayout.IME_ACTION_NONE:
                action = EditorInfo.IME_ACTION_NONE;
                break;
            case TextInputLayout.IME_ACTION_GO:
                action = EditorInfo.IME_ACTION_GO;
                break;
            case TextInputLayout.IME_ACTION_SEARCH:
                action = EditorInfo.IME_ACTION_SEARCH;
                break;
            case TextInputLayout.IME_ACTION_SEND:
                action = EditorInfo.IME_ACTION_SEND;
                break;
            case TextInputLayout.IME_ACTION_PREVIOUS:
                action = EditorInfo.IME_ACTION_PREVIOUS;
                break;
            case TextInputLayout.IME_ACTION_NEXT:
                action = EditorInfo.IME_ACTION_NEXT;
                break;
            case TextInputLayout.IME_ACTION_DONE:
                action = EditorInfo.IME_ACTION_DONE;
                break;
            case TextInputLayout.IME_ACTION_DEFAULT:
            default:
                action =
                        op.hasInputFlag(TextInputLayout.FLAG_SINGLE_LINE)
                                ? EditorInfo.IME_ACTION_DONE
                                : EditorInfo.IME_ACTION_UNSPECIFIED;
        }
        return action | EditorInfo.IME_FLAG_NO_EXTRACT_UI;
    }

    /** Maps the layout's content type to an Android autofill hint, or null for none. */
    static @Nullable String autofillHint(@NonNull TextInputLayout op) {
        switch (op.getContentType()) {
            case TextInputLayout.CONTENT_TYPE_PERSON_NAME:
                return View.AUTOFILL_HINT_NAME;
            case TextInputLayout.CONTENT_TYPE_PERSON_FIRST_NAME:
                return "personGivenName";
            case TextInputLayout.CONTENT_TYPE_PERSON_LAST_NAME:
                return "personFamilyName";
            case TextInputLayout.CONTENT_TYPE_EMAIL_ADDRESS:
                return View.AUTOFILL_HINT_EMAIL_ADDRESS;
            case TextInputLayout.CONTENT_TYPE_PHONE_NUMBER:
                return View.AUTOFILL_HINT_PHONE;
            case TextInputLayout.CONTENT_TYPE_USERNAME:
                return View.AUTOFILL_HINT_USERNAME;
            case TextInputLayout.CONTENT_TYPE_PASSWORD:
                return View.AUTOFILL_HINT_PASSWORD;
            case TextInputLayout.CONTENT_TYPE_NEW_PASSWORD:
                return "newPassword";
            case TextInputLayout.CONTENT_TYPE_ONE_TIME_CODE:
                return "smsOTPCode";
            case TextInputLayout.CONTENT_TYPE_POSTAL_ADDRESS:
                return View.AUTOFILL_HINT_POSTAL_ADDRESS;
            case TextInputLayout.CONTENT_TYPE_POSTAL_CODE:
                return View.AUTOFILL_HINT_POSTAL_CODE;
            case TextInputLayout.CONTENT_TYPE_CREDIT_CARD_NUMBER:
                return View.AUTOFILL_HINT_CREDIT_CARD_NUMBER;
            case TextInputLayout.CONTENT_TYPE_CREDIT_CARD_EXPIRATION_DATE:
                return View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE;
            case TextInputLayout.CONTENT_TYPE_CREDIT_CARD_SECURITY_CODE:
                return View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE;
            default:
                return null;
        }
    }

    /** Builds the {@link InputFilter}s for the layout's max length and character filter. */
    static @NonNull InputFilter[] inputFilters(@NonNull TextInputLayout op) {
        InputFilter charFilter = null;
        switch (op.getFilter()) {
            case TextInputLayout.FILTER_DIGITS:
                charFilter = charsetFilter("0123456789");
                break;
            case TextInputLayout.FILTER_DECIMAL:
                charFilter = charsetFilter("0123456789.,-");
                break;
            case TextInputLayout.FILTER_HEXADECIMAL:
                charFilter = charsetFilter("0123456789abcdefABCDEF");
                break;
            case TextInputLayout.FILTER_ALPHANUMERIC:
                charFilter =
                        (source, start, end, dest, dstart, dend) -> {
                            StringBuilder kept = null;
                            for (int i = start; i < end; i++) {
                                char c = source.charAt(i);
                                if (!Character.isLetterOrDigit(c)) {
                                    if (kept == null) {
                                        kept = new StringBuilder(source.subSequence(start, i));
                                    }
                                } else if (kept != null) {
                                    kept.append(c);
                                }
                            }
                            return kept;
                        };
                break;
            default:
                break;
        }
        int count = (op.getMaxLength() > 0 ? 1 : 0) + (charFilter != null ? 1 : 0);
        InputFilter[] filters = new InputFilter[count];
        int i = 0;
        if (op.getMaxLength() > 0) {
            filters[i++] = new InputFilter.LengthFilter(op.getMaxLength());
        }
        if (charFilter != null) {
            filters[i] = charFilter;
        }
        return filters;
    }

    private static @NonNull InputFilter charsetFilter(@NonNull String allowed) {
        return (source, start, end, dest, dstart, dend) -> {
            StringBuilder kept = null;
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (allowed.indexOf(c) < 0) {
                    if (kept == null) {
                        kept = new StringBuilder(source.subSequence(start, i));
                    }
                } else if (kept != null) {
                    kept.append(c);
                }
            }
            return kept;
        };
    }
}
