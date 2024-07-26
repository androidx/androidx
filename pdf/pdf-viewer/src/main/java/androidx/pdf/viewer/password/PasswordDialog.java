/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.viewer.password;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.fragment.app.DialogFragment;
import androidx.pdf.R;
import androidx.pdf.util.Accessibility;

/**
 * Dialog for querying password for a protected file. The dialog has 2 buttons:
 * <ul>
 * <li>Exit, exits the application,
 * <li>Open, tries to open the document with the given password. If this is not successful, the
 *     dialog stays up, and offers to try again (the controller should call {@link #retry}).
 *     If successful, the controller should call {@link #dismiss}.
 * </ul>
 * <p>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("deprecation")
public abstract class PasswordDialog extends DialogFragment {

    private int mTextDefaultColor;
    private int mBlueColor;
    private int mTextErrorColor;
    private AlertDialog mPasswordDialog;

    private boolean mIncorrect;
    private boolean mFinishOnCancel;

    /**
     * @param finishOnCancel being true indicates that the activity will be killed when the user
     *                       presses the cancel button on this dialog.
     */
    public void setFinishOnCancel(boolean finishOnCancel) {
        this.mFinishOnCancel = finishOnCancel;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_password, null);
        builder.setTitle(R.string.title_dialog_password)
                .setView(view)
                .setPositiveButton(R.string.button_open, null)
                .setNegativeButton(R.string.button_cancel, null);
        final AlertDialog dialog = builder.create();

        dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        final EditText passwordField = (EditText) view.findViewById(R.id.password);
        setupPasswordField(passwordField);

        // Hijack the positive button to NOT dismiss the dialog immediately.
        dialog.setOnShowListener(
                new OnShowListener() {
                    @Override
                    public void onShow(DialogInterface useless) {
                        // TODO: Track password prompt displayed.
                        final Button open = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        final Button exit = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

                        open.setOnClickListener(
                                new OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        sendPassword(passwordField);
                                        // TODO: Track password prompt opened.
                                    }
                                });

                        exit.setOnClickListener(
                                new OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        dialog.cancel();
                                        // TODO: Track password prompt exit.
                                    }
                                });

                        // Clear red patches on new text
                        passwordField.addTextChangedListener(
                                new TextWatcher() {
                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before,
                                            int count) {
                                        if (mIncorrect) {
                                            clearIncorrect();
                                        }
                                    }

                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start,
                                            int count, int after) {
                                    }

                                    @Override
                                    public void afterTextChanged(Editable s) {
                                    }
                                });
                    }
                });

        mPasswordDialog = dialog;
        return dialog;
    }

    private void setupPasswordField(final EditText passwordField) {
        passwordField.setFocusable(true);
        passwordField.requestFocus();
        // Do not expand the text field to full screen when in landscape.
        passwordField.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);

        // Set the open button text with title case.
        String openText = getResources().getString(R.string.button_open);
        passwordField.setImeActionLabel(openText, EditorInfo.IME_ACTION_DONE);

        // Handle 'Enter'
        passwordField.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    sendPassword(passwordField);
                    return true;
                }
                return false;
            }
        });

        // Handle soft keyboard "Done" button.
        passwordField.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    sendPassword(passwordField);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mTextDefaultColor = getResources().getColor(R.color.text_default);
        mTextErrorColor = getResources().getColor(R.color.text_error);
        mBlueColor = getResources().getColor(R.color.google_blue);

        EditText textField = (EditText) getDialog().findViewById(R.id.password);
        textField.getBackground().setColorFilter(mBlueColor, PorterDuff.Mode.SRC_ATOP);

        mPasswordDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(mBlueColor);
        mPasswordDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(mBlueColor);

        showSoftKeyboard(textField);
    }

    private void showSoftKeyboard(View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager)
                    getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        if (mFinishOnCancel) {
            getActivity().finish();
        } else {
            dismiss();
            showErrorOnDialogCancel();
        }
    }

    /** Set the password input by the user. */
    public abstract void sendPassword(@NonNull EditText textField);

    /** Show error when user cancels password prompt dialog. */
    public abstract void showErrorOnDialogCancel();

    /** The given password didn't work, perhaps try again? */
    public void retry() {
        // TODO: Track incorrect password input.
        mIncorrect = true;
        EditText textField = (EditText) getDialog().findViewById(R.id.password);
        textField.selectAll();

        swapBackground(textField, false);
        textField.getBackground().setColorFilter(mTextErrorColor, PorterDuff.Mode.SRC_ATOP);

        TextView label = (TextView) getDialog().findViewById(R.id.label);
        label.setText(R.string.label_password_incorrect);
        label.setTextColor(mTextErrorColor);

        Accessibility.get().announce(getActivity(), getDialog().getCurrentFocus(),
                R.string.desc_password_incorrect_message);

        getDialog().findViewById(R.id.password_alert).setVisibility(View.VISIBLE);
    }

    private void clearIncorrect() {
        mIncorrect = false;
        TextView label = (TextView) getDialog().findViewById(R.id.label);
        label.setText(R.string.label_password_first);
        label.setTextColor(mTextDefaultColor);

        EditText textField = (EditText) getDialog().findViewById(R.id.password);
        swapBackground(textField, true);

        getDialog().findViewById(R.id.password_alert).setVisibility(View.GONE);
    }

    private void swapBackground(EditText textField, boolean reverse) {
        if (!reverse) {
            textField.setBackground(
                    getResources().getDrawable(R.drawable.drag_indicator));
        } else {
            EditText sample = new EditText(getActivity());
            textField.setBackground(sample.getBackground());
        }
    }
}
