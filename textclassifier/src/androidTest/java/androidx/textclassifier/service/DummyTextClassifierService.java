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

package androidx.textclassifier.service;

import android.content.ComponentName;
import android.content.Context;
import android.os.CancellationSignal;

import androidx.textclassifier.RemoteServiceTextClassifierTest;
import androidx.textclassifier.SelectionEvent;
import androidx.textclassifier.TextClassification;
import androidx.textclassifier.TextClassificationSessionId;
import androidx.textclassifier.TextClassifierService;
import androidx.textclassifier.TextLinks;
import androidx.textclassifier.TextSelection;

/**
 * Used by {@link RemoteServiceTextClassifierTest} to verify the request handling
 * from end to end.
 */
public class DummyTextClassifierService extends TextClassifierService {
    private static SelectionEvent sSelectionEvent;
    private static TextSelection sTextSelection;
    private static TextLinks sTextLinks;
    private static TextClassification sTextClassification;

    @Override
    public void onSuggestSelection(TextClassificationSessionId sessionId,
            TextSelection.Request request, CancellationSignal cancellationSignal,
            TextClassifierService.Callback<TextSelection> callback) {
        callback.onSuccess(sTextSelection);
    }

    @Override
    public void onClassifyText(TextClassificationSessionId sessionId,
            TextClassification.Request request, CancellationSignal cancellationSignal,
            Callback<TextClassification> callback) {
        callback.onSuccess(sTextClassification);
    }

    @Override
    public void onGenerateLinks(TextClassificationSessionId sessionId, TextLinks.Request request,
            CancellationSignal cancellationSignal,
            Callback<TextLinks> callback) {
        callback.onSuccess(sTextLinks);
    }

    @Override
    public void onSelectionEvent(TextClassificationSessionId sessionId, SelectionEvent event) {
        sSelectionEvent = event;
    }

    public static SelectionEvent getLastReportedSelectionEvent() {
        return sSelectionEvent;
    }

    public static void setTextSelection(TextSelection textSelection) {
        sTextSelection = textSelection;
    }

    public static void setTextClassification(TextClassification textClassification) {
        sTextClassification = textClassification;
    }

    public static void setTextLinks(TextLinks textLinks) {
        sTextLinks = textLinks;
    }

    public static void cleanup() {
        sTextLinks = null;
        sTextClassification = null;
        sTextSelection = null;
        sSelectionEvent = null;
    }


    public static ComponentName getComponent(Context context) {
        return new ComponentName(context, DummyTextClassifierService.class);
    }
}
