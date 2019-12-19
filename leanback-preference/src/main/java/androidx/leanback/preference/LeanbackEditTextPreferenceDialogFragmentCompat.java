/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.leanback.preference;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.DialogPreference;
import androidx.preference.EditTextPreference;

/**
 * Implemented a dialog to input text.
 */
public class LeanbackEditTextPreferenceDialogFragmentCompat extends
        LeanbackPreferenceDialogFragmentCompat {

    public static final String EXTRA_INPUT_TYPE = "input_type";
    public static final String EXTRA_IME_OPTIONS = "ime_option";

    private static final int DEFAULT_INPUT_TYPE = InputType.TYPE_CLASS_TEXT;
    private static final int DEFAULT_IME_OPTIONS = EditorInfo.IME_ACTION_GO;

    private static final String SAVE_STATE_TITLE = "LeanbackEditPreferenceDialog.title";
    private static final String SAVE_STATE_MESSAGE = "LeanbackEditPreferenceDialog.message";
    private static final String SAVE_STATE_TEXT = "LeanbackEditPreferenceDialog.text";
    private static final String SAVE_STATE_INPUT_TYPE = "LeanbackEditPreferenceDialog.inputType";
    private static final String SAVE_STATE_IME_OPTIONS = "LeanbackEditPreferenceDialog.imeOptions";

    private CharSequence mDialogTitle;
    private CharSequence mDialogMessage;
    private CharSequence mText;
    private int mImeOptions;
    private int mInputType;

    /**
     * Create a new LeanbackListPreferenceDialogFragmentCompat.
     * @param key The key of {@link EditTextPreference} it will be created from.
     * @return A new LeanbackEditTextPreferenceDialogFragmentCompat to display.
     */
    public static LeanbackEditTextPreferenceDialogFragmentCompat newInstance(String key) {
        final Bundle args = new Bundle(1);
        args.putString(ARG_KEY, key);

        final LeanbackEditTextPreferenceDialogFragmentCompat
                fragment = new LeanbackEditTextPreferenceDialogFragmentCompat();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            final DialogPreference preference = getPreference();
            mDialogTitle = preference.getDialogTitle();
            mDialogMessage = preference.getDialogMessage();
            if (preference instanceof EditTextPreference) {
                mDialogTitle = preference.getDialogTitle();
                mDialogMessage = preference.getDialogMessage();
                mText = ((EditTextPreference) preference).getText();
                mInputType = preference.getExtras().getInt(EXTRA_INPUT_TYPE, DEFAULT_INPUT_TYPE);
                mImeOptions = preference.getExtras().getInt(EXTRA_IME_OPTIONS, DEFAULT_IME_OPTIONS);
            } else {
                throw new IllegalArgumentException("Preference must be a EditTextPreference");
            }
        } else {
            mDialogTitle = savedInstanceState.getCharSequence(SAVE_STATE_TITLE);
            mDialogMessage = savedInstanceState.getCharSequence(SAVE_STATE_MESSAGE);
            mText = savedInstanceState.getCharSequence(SAVE_STATE_TEXT);
            mInputType = savedInstanceState.getInt(SAVE_STATE_INPUT_TYPE, DEFAULT_INPUT_TYPE);
            mImeOptions = savedInstanceState.getInt(SAVE_STATE_IME_OPTIONS, DEFAULT_IME_OPTIONS);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(SAVE_STATE_TITLE, mDialogTitle);
        outState.putCharSequence(SAVE_STATE_MESSAGE, mDialogMessage);
        outState.putCharSequence(SAVE_STATE_TEXT, mText);
        outState.putInt(SAVE_STATE_INPUT_TYPE, mInputType);
        outState.putInt(SAVE_STATE_IME_OPTIONS, mImeOptions);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        TypedValue tv = new TypedValue();
        getActivity().getTheme().resolveAttribute(
                androidx.preference.R.attr.preferenceTheme, tv, true);
        int theme = tv.resourceId;
        if (theme == 0) {
            // Fallback to default theme.
            theme = R.style.PreferenceThemeOverlayLeanback;
        }
        Context styledContext = new ContextThemeWrapper(getActivity(), theme);
        LayoutInflater styledInflater = inflater.cloneInContext(styledContext);
        View view = styledInflater.inflate(R.layout.leanback_edit_preference_fragment,
                container, false);

        if (!TextUtils.isEmpty(mDialogTitle)) {
            TextView titleView = (TextView) view.findViewById(R.id.decor_title);
            titleView.setText(mDialogTitle);
        }

        if (!TextUtils.isEmpty(mDialogMessage)) {
            TextView messageView = (TextView) view.findViewById(android.R.id.message);
            messageView.setVisibility(View.VISIBLE);
            messageView.setText(mDialogMessage);
        }

        EditText editText = view.findViewById(android.R.id.edit);
        editText.setInputType(mInputType);
        editText.setImeOptions(mImeOptions);
        if (!TextUtils.isEmpty(mText)) {
            editText.setText(mText);
        }
        editText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_GO
                        || actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_NEXT
                        || actionId == EditorInfo.IME_ACTION_SEND) {
                    InputMethodManager imm = (InputMethodManager)
                            getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
                    EditTextPreference preference = (EditTextPreference) getPreference();
                    preference.setText(textView.getText().toString());
                    getFragmentManager().popBackStack();
                    return true;
                }
                return false;
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        EditText editText = getView().findViewById(android.R.id.edit);
        InputMethodManager imm = (InputMethodManager)
                getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        editText.requestFocus();
        imm.showSoftInput(editText, 0);
    }
}
