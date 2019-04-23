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

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.URLSpan;

import androidx.core.app.RemoteActionCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.text.util.LinkifyCompat;
import androidx.textclassifier.SelectionEvent;
import androidx.textclassifier.TextClassification;
import androidx.textclassifier.TextClassifier;
import androidx.textclassifier.TextLinks;
import androidx.textclassifier.TextSelection;

import java.util.Collections;
import java.util.regex.Pattern;

/**
 * A simple text classifier that looks for the word "android".
 */
public class SimpleTextClassifier extends TextClassifier {
    private final Context mContext;

    public SimpleTextClassifier(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public TextSelection suggestSelection(TextSelection.Request request) {
        return super.suggestSelection(request);
    }

    @Override
    public TextClassification classifyText(TextClassification.Request request) {
        TextClassification.Builder builder =
                new TextClassification.Builder().setText(request.getText().toString());
        String text = request.getText().toString().substring(
                request.getStartIndex(), request.getEndIndex());
        if ("android".equalsIgnoreCase(text)) {
            builder.setEntityType(TextClassifier.TYPE_URL, 1.0f);
            builder.addAction(new RemoteActionCompat(
                    IconCompat.createWithResource(mContext, R.drawable.android), "View",
                    "View", createPendingIntent()));
        }
        return builder.build();
    }

    @Override
    public TextLinks generateLinks(TextLinks.Request request) {
        String text = request.getText().toString();
        TextLinks.Builder builder = new TextLinks.Builder(text);
        final Spannable spannable = new SpannableString(text);
        if (LinkifyCompat.addLinks(spannable, Pattern.compile("android", Pattern.CASE_INSENSITIVE),
                null, null, (matcher, s) -> "https://www.android.com")) {
            final URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
            for (URLSpan urlSpan : spans) {
                builder.addLink(
                        spannable.getSpanStart(urlSpan),
                        spannable.getSpanEnd(urlSpan),
                        Collections.singletonMap(TextClassifier.TYPE_URL, 1.0f));
            }
        }
        return builder.build();
    }

    @Override
    @SuppressLint("RestrictedApi")
    public void onSelectionEvent(SelectionEvent event) {
        super.onSelectionEvent(event);
    }

    private PendingIntent createPendingIntent() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://www.android.com"));
        return PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
