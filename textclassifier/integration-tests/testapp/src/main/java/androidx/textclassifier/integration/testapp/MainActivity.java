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

package androidx.textclassifier.integration.testapp;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.textclassifier.SmartLinkify;
import androidx.textclassifier.SmartLinkifyParams;
import androidx.textclassifier.TextClassificationManager;
import androidx.textclassifier.TextClassifier;

/**
 * Main activity.
 */
public class MainActivity extends AppCompatActivity {
    private static final int DEFAULT = 0;
    private static final int CUSTOM = 1;

    private TextClassifier mTextClassifier;

    private EditText mInput;

    private TextView mStatusTextView;

    private TextClassificationManager mTextClassificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTextClassificationManager = TextClassificationManager.of(this);
        mTextClassifier = mTextClassificationManager.getTextClassifier();

        setContentView(R.layout.activity_main);
        mInput = findViewById(R.id.textView_input);
        mStatusTextView = findViewById(R.id.textView_tc);
        findViewById(R.id.button_generate_links).setOnClickListener(v -> smartLinkify());
        findViewById(R.id.get_tc).setOnClickListener(view -> {
            mTextClassifier = mTextClassificationManager.getTextClassifier();
            updateStatusText();
        });

        updateStatusText();
        setupSpinner();
    }

    private void setupSpinner() {
        Spinner spinner = findViewById(R.id.textclassifier_spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                if (pos == DEFAULT) {
                    mTextClassificationManager.setTextClassifier(null);
                } else {
                    mTextClassificationManager.setTextClassifier(
                            new SimpleTextClassifier(MainActivity.this));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void updateStatusText() {
        mStatusTextView.setText(mTextClassifier.getClass().getName());
    }

    private void smartLinkify() {
        SmartLinkifyParams smartLinkifyParams = new SmartLinkifyParams.Builder().build();
        SmartLinkify.addLinksAsync(mInput, smartLinkifyParams);
    }
}
