/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.supportv7.app;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.MultiAutoCompleteTextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.android.supportv7.Cheeses;
import com.example.android.supportv7.R;

/**
 * This demonstrates the styled text input widgets in AppCompat, such as
 * {@link android.widget.EditText}, {@link android.widget.AutoCompleteTextView} and
 * {@link android.widget.MultiAutoCompleteTextView}.
 */
public class AppCompatWidgetsTextInput extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appcompat_widgets_text_input);

        // Fetch the AutoCompleteTextView and set an adapter
        AutoCompleteTextView actv = findViewById(
                R.id.widgets_autocompletetextview);
        actv.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, Cheeses.sCheeseStrings));

        // Fetch the MultiAutoCompleteTextView and set an adapter and Tokenizer
        MultiAutoCompleteTextView mactv = findViewById(
                R.id.widgets_multiautocompletetextview);
        mactv.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        mactv.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, Cheeses.sCheeseStrings));
    }

}
