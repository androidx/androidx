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

import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.textclassifier.MainThreadExecutor;
import androidx.textclassifier.TextClassificationManager;
import androidx.textclassifier.TextClassifier;
import androidx.textclassifier.TextLinks;
import androidx.textclassifier.TextLinksParams;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Main activity.
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class MainActivity extends AppCompatActivity {
    private static final int DEFAULT = 0;

    private static final Executor sWorkerThreadExecutor = Executors.newSingleThreadExecutor();
    private static final Executor sMainThreadExecutor = new MainThreadExecutor();

    private TextView mInput;
    private TextView mStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        Uncomment this to use the experimental toolbar.
//        ToolbarController.setFloatingToolbarFactory(FloatingToolbar::new);

        setContentView(R.layout.activity_main);
        mInput = findViewById(R.id.textView_input);
        setLinkMovementMethod(mInput);
        mStatusTextView = findViewById(R.id.textView_tc);
        findViewById(R.id.button_generate_links).setOnClickListener(v -> linkifyAsync(mInput));
        updateStatusText();
        setupTextClassifierSpinner();
    }

    private void setupTextClassifierSpinner() {
        Spinner spinner = findViewById(R.id.textclassifier_spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                if (pos == DEFAULT) {
                    setTextClassifier(null);
                } else {
                    setTextClassifier(new SimpleTextClassifier(MainActivity.this));
                }
                updateStatusText();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    private void updateStatusText() {
        mStatusTextView.setText(getTextClassifier().getClass().getName());
    }

    private void clearSpans(TextView textView) {
        CharSequence text = textView.getText();
        if (!(text instanceof Spannable)) {
            return;
        }
        Spannable spannable = (Spannable) text;
        TextLinks.TextLinkSpan[] spans =
                spannable.getSpans(0, spannable.length(), TextLinks.TextLinkSpan.class);
        for (TextLinks.TextLinkSpan span : spans) {
            spannable.removeSpan(span);
        }
    }

    private void linkifyAsync(TextView textView) {
        clearSpans(textView);
        sWorkerThreadExecutor.execute(() -> {
            TextLinks.Request request = new TextLinks.Request.Builder(textView.getText()).build();
            TextLinks textLinks = getTextClassifier().generateLinks(request);
            sMainThreadExecutor.execute(() ->
                    textLinks.apply(
                            (Spannable) textView.getText(),
                            getTextClassifier(),
                            TextLinksParams.DEFAULT_PARAMS));
        });
    }

    private TextClassifier getTextClassifier() {
        return TextClassificationManager.of(this).getTextClassifier();
    }

    private void setTextClassifier(TextClassifier textClassifier) {
        TextClassificationManager.of(this).setTextClassifier(textClassifier);
    }

    private void setLinkMovementMethod(TextView textView) {
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
