/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.textclassifier;

import androidx.textclassifier.ITextClassificationCallback;
import androidx.textclassifier.ITextLinksCallback;
import androidx.textclassifier.ITextSelectionCallback;

/**
 * TextClassifierService binder interface.
 * See TextClassifier for interface documentation.
 * {@hide}
 */
oneway interface ITextClassifierService {

    void onSuggestSelection(
            in Bundle sessionId,
            in Bundle request,
            in ITextSelectionCallback callback) = 0;

    void onClassifyText(
            in Bundle sessionId,
            in Bundle request,
            in ITextClassificationCallback callback) = 1;

    void onGenerateLinks(
            in Bundle sessionId,
            in Bundle request,
            in ITextLinksCallback callback) = 2;

    void onSelectionEvent(
            in Bundle sessionId,
            in Bundle event) = 3;
}