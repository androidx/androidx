/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.webkit.internal;

import androidx.webkit.WebViewStartupException;

import org.chromium.support_lib_boundary.WebViewProviderFactoryBoundaryInterface.StartupErrorType;
import org.jspecify.annotations.NonNull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WebViewStartupExceptionBuilder implements
        BiConsumer<@StartupErrorType Integer, Object> {
    Integer mErrorCode;
    String mErrorMessage;

    @Override
    public void accept(@StartupErrorType Integer key, Object value) {
        switch (key) {
            case StartupErrorType.CODE:
                mErrorCode = (Integer) value;
                break;
            case StartupErrorType.MESSAGE:
                // We always expect a message to be provided.
                mErrorMessage = (String) value;
                break;
            default:
                if (key < 0) {
                    throw new UnsupportedOperationException(
                            "The current AndroidX version doesn't support this callback value: "
                                    + key);
                }
                // If we get here then it means that there's an optional operation that the
                // current AndroidX version doesn't support and it's safe to ignore.
        }
    }

    /**
     * Based on the data above, formulate the right exception here.
     */
    public static @NonNull WebViewStartupException buildException(
            @NonNull Consumer<BiConsumer<@StartupErrorType Integer, Object>> onFailure) {
        WebViewStartupExceptionBuilder builder = new WebViewStartupExceptionBuilder();
        onFailure.accept(builder);

        if (builder.mErrorCode == null) {
            return new WebViewStartupException(builder.mErrorMessage);
        }

        switch (builder.mErrorCode) {
            default:
                return new WebViewStartupException(builder.mErrorMessage);
        }
    }
}
