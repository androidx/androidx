/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.example.android.support.text.emoji;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatCheckedTextView;
import androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.AppCompatToggleButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.emoji2.text.EmojiCompat;
import androidx.fragment.app.Fragment;

/**
 * Main fragment.
 */
public class MainFragment extends Fragment {

    // [U+1F469] (WOMAN) + [U+200D] (ZERO WIDTH JOINER) + [U+1F4BB] (PERSONAL COMPUTER)
    private static final String WOMAN_TECHNOLOGIST = "\uD83D\uDC69\u200D\uD83D\uDCBB";

    // [U+1F469] (WOMAN) + [U+200D] (ZERO WIDTH JOINER) + [U+1F3A4] (MICROPHONE)
    private static final String WOMAN_SINGER = "\uD83D\uDC69\u200D\uD83C\uDFA4";

    static final String EMOJI = WOMAN_TECHNOLOGIST + " " + WOMAN_SINGER;
    private static final String[] AUTOCOMPLETE_ITEMS = new String[3];
    static {
        AUTOCOMPLETE_ITEMS[0] = "Woman technologist " + WOMAN_TECHNOLOGIST;
        AUTOCOMPLETE_ITEMS[1] = "Woman singer " + WOMAN_SINGER;
        AUTOCOMPLETE_ITEMS[2] = "Woman";
    };

    private TextView mEmojiTextView;
    private TextView mAppcompatTextView;
    private TextView mEmojiEditText;
    TextView mAppcompatEditText;
    private TextView mEmojiButton;
    private TextView mAppcompatButton;
    TextView mRegularTextView;
    private TextView mCustomTextView;
    private AppCompatToggleButton mAppCompatToggleButton;
    private SwitchCompat mSwitchCompat;
    private AppCompatCheckedTextView mAppCompatCheckedTextView;
    private AppCompatCheckBox mAppCompatCheckBox;
    private AppCompatRadioButton mAppCompatRadioButton;
    AppCompatAutoCompleteTextView mAppCompatAutoCompleteTextView;
    AppCompatMultiAutoCompleteTextView mAppCompatMultiAutoCompleteTextView;

    final Config.Listener mConfigListener = new Config.Listener() {
        @Override
        public void onEmojiCompatUpdated() {
            init();
        }
    };

    static MainFragment newInstance() {
        MainFragment fragment = new MainFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_main, container, false);

        // TextView variant provided by EmojiCompat library
        mEmojiTextView = view.findViewById(R.id.emoji_text_view);
        // TextView from AppCompat
        mAppcompatTextView = view.findViewById(R.id.appcompat_text_view);
        // EditText variant provided by EmojiCompat library
        mEmojiEditText = view.findViewById(R.id.emoji_edit_text);
        // EditText from AppCompat
        mAppcompatEditText = view.findViewById(R.id.appcompat_edit_text);
        // Button variant provided by EmojiCompat library
        mEmojiButton = view.findViewById(R.id.emoji_button);
        // Button from AppCompat
        mAppcompatButton = view.findViewById(R.id.appcompat_button);
        // Regular TextView without EmojiCompat support; you have to manually process the text
        mRegularTextView = view.findViewById(R.id.regular_text_view);
        // Custom TextView
        mCustomTextView = view.findViewById(R.id.emoji_custom_text_view);

        mAppCompatToggleButton = view.findViewById(R.id.appcompat_toggle_button);
        mSwitchCompat = view.findViewById(R.id.switch_compat);
        mAppCompatCheckedTextView = view.findViewById(R.id.appcompat_checked_text_view);
        mAppCompatCheckBox = view.findViewById(R.id.appcompat_checkbox);
        mAppCompatRadioButton = view.findViewById(R.id.appcompat_radiobutton);
        mAppCompatAutoCompleteTextView = view.findViewById(R.id.appcompat_autocomplete_textview);
        mAppCompatMultiAutoCompleteTextView =
                view.findViewById(R.id.appcompat_multiautocomplete_textview);

        final TextView emojiListButton = view.findViewById(R.id.emoji_list_button);
        emojiListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).showAllEmojis();
            }
        });

        init();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Config.get().registerListener(mConfigListener);
    }

    @Override
    public void onStop() {
        Config.get().unregisterListener(mConfigListener);
        super.onStop();
    }

    private void init() {
        mEmojiTextView.setText(getString(R.string.emoji_text_view, EMOJI));
        mAppcompatTextView.setText(getString(R.string.appcompat_text_view, EMOJI));
        mEmojiEditText.setText(getString(R.string.emoji_edit_text, EMOJI));
        mAppcompatEditText.setText(getString(R.string.appcompat_edit_text, EMOJI));

        mEmojiButton.setText(getString(R.string.emoji_button, EMOJI));
        mAppcompatButton.setText(getString(R.string.appcompat_button, EMOJI));
        mRegularTextView.setText(getString(R.string.regular_text_view, EMOJI));
        mAppCompatToggleButton.setTextOn(getString(R.string.toggle_on, EMOJI));
        mAppCompatToggleButton.setTextOff(getString(R.string.toggle_off, EMOJI));
        mSwitchCompat.setText(getString(R.string.switch_compat, EMOJI));
        mSwitchCompat.setTextOn("\uD83C\uDF89");
        mSwitchCompat.setTextOff("⛔");
        mSwitchCompat.setShowText(true);
        mSwitchCompat.setEmojiCompatEnabled(true);
        mAppCompatCheckedTextView.setText(getString(R.string.checked_text_view, EMOJI));
        mAppCompatCheckBox.setText(getString(R.string.check_box, EMOJI));
        mAppCompatRadioButton.setText(getString(R.string.radio_button, EMOJI));

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_dropdown_item_1line, AUTOCOMPLETE_ITEMS);
        mAppCompatAutoCompleteTextView.setAdapter(adapter);
        mAppCompatMultiAutoCompleteTextView.setAdapter(adapter);
        mAppCompatMultiAutoCompleteTextView.setTokenizer(
                new MultiAutoCompleteTextView.CommaTokenizer());

        EmojiCompat.get().registerInitCallback(new EmojiCompat.InitCallback() {
            @Override
            public void onInitialized() {
                final EmojiCompat compat = EmojiCompat.get();
                if (compat.getLoadState() == EmojiCompat.LOAD_STATE_SUCCEEDED) {
                    mRegularTextView.setText(
                            compat.process(getString(R.string.regular_text_view, EMOJI)));


                    mAppcompatEditText.setHint(
                            compat.process(getString(R.string.appcompat_edit_text, EMOJI)));
                    mAppCompatAutoCompleteTextView.setHint(
                            compat.process(getString(R.string.autocomplete_hint, EMOJI)));
                    mAppCompatMultiAutoCompleteTextView.setHint(
                            compat.process(getString(R.string.multiautocomplete_hint, EMOJI)));
                }
            }
        });
        mCustomTextView.setText(getString(R.string.custom_text_view, EMOJI));
    }
}
