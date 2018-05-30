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

package androidx.textclassifier;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.core.util.Preconditions;

/**
 * Provides a {@link androidx.textclassifier.TextClassifier} interface for a
 * {@link android.view.textclassifier.TextClassifier} object.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(28)
public class PlatformTextClassifierWrapper extends TextClassifier {
    private android.view.textclassifier.TextClassifier mPlatformTextClassifier;

    public PlatformTextClassifierWrapper(
            @NonNull android.view.textclassifier.TextClassifier platformTextClassifier) {
        super(new ProxySessionStrategy(platformTextClassifier));
        Preconditions.checkNotNull(platformTextClassifier);
        mPlatformTextClassifier = platformTextClassifier;
    }

    /** @inheritDoc */
    @NonNull
    @WorkerThread
    @Override
    public TextSelection suggestSelection(@NonNull TextSelection.Request request) {
        Preconditions.checkNotNull(request);
        return TextSelection.Convert.fromPlatform(
                mPlatformTextClassifier.suggestSelection(
                        TextSelection.Request.Convert.toPlatform(request)));
    }

    /** @inheritDoc */
    @NonNull
    @WorkerThread
    @Override
    public TextClassification classifyText(@NonNull TextClassification.Request request) {
        Preconditions.checkNotNull(request);
        return TextClassification.Convert.fromPlatform(
                mPlatformTextClassifier.classifyText(
                        TextClassification.Request.Convert.toPlatform(request)));
    }

    /** @inheritDoc */
    @NonNull
    @WorkerThread
    @Override
    public TextLinks generateLinks(@NonNull TextLinks.Request request) {
        Preconditions.checkNotNull(request);
        return TextLinks.Convert.fromPlatform(mPlatformTextClassifier.generateLinks(
                TextLinks.Request.Convert.toPlatform(request)), request.getText());
    }

    /** @inheritDoc */
    @Override
    @WorkerThread
    public int getMaxGenerateLinksTextLength() {
        return mPlatformTextClassifier.getMaxGenerateLinksTextLength();
    }

    /**
     * Delegates session handling to {@link android.view.textclassifier.TextClassifier}.
     */
    private static class ProxySessionStrategy implements SessionStrategy {
        private final android.view.textclassifier.TextClassifier mPlatformTextClassifier;

        ProxySessionStrategy(
                @NonNull android.view.textclassifier.TextClassifier textClassifier) {
            Preconditions.checkNotNull(textClassifier);
            mPlatformTextClassifier = textClassifier;
        }

        @Override
        public void destroy() {
            mPlatformTextClassifier.destroy();
        }

        @Override
        public void reportSelectionEvent(@NonNull SelectionEvent event) {
            Preconditions.checkNotNull(event);
            mPlatformTextClassifier.onSelectionEvent(SelectionEvent.Convert.toPlatform(event));
        }

        @Override
        public boolean isDestroyed() {
            return mPlatformTextClassifier.isDestroyed();
        }
    }
}
