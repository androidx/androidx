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
import android.widget.TextView;

import androidx.emoji.text.EmojiCompat;
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

    private TextView mEmojiTextView;
    private TextView mEmojiEditText;
    private TextView mEmojiButton;
    private TextView mRegularTextView;
    private TextView mCustomTextView;

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
        // EditText variant provided by EmojiCompat library
        mEmojiEditText = view.findViewById(R.id.emoji_edit_text);
        // Button variant provided by EmojiCompat library
        mEmojiButton = view.findViewById(R.id.emoji_button);
        // Regular TextView without EmojiCompat support; you have to manually process the text
        mRegularTextView = view.findViewById(R.id.regular_text_view);
        // Custom TextView
        mCustomTextView = view.findViewById(R.id.emoji_custom_text_view);

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
        mEmojiEditText.setText(getString(R.string.emoji_edit_text, EMOJI));
        mEmojiButton.setText(getString(R.string.emoji_button, EMOJI));
        mRegularTextView.setText(getString(R.string.regular_text_view, EMOJI));
        EmojiCompat.get().registerInitCallback(new EmojiCompat.InitCallback() {
            @Override
            public void onInitialized() {
                final EmojiCompat compat = EmojiCompat.get();
                mRegularTextView.setText(
                        compat.process(getString(R.string.regular_text_view, EMOJI)));
            }
        });
        mCustomTextView.setText(getString(R.string.custom_text_view, EMOJI));
    }
}
