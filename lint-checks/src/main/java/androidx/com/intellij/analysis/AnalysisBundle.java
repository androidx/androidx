/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.com.intellij.analysis;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

/**
 * Modifications:
 * - Stripped down to a stub implementation of message() to bypass resource bundling.
 */
@SuppressWarnings("UnresolvedPropertyKey")
public final class AnalysisBundle extends DynamicBundle {
    @NonNls public static final String BUNDLE = "messages.AnalysisBundle";

    private AnalysisBundle() {
        super(BUNDLE);
    }

    @NotNull
    public static @Nls String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key,
            @NotNull Object... params) {
        StringBuilder builder = new StringBuilder(key);
        builder.append(":");
        for (Object param : params) {
            builder.append(param.toString());
            builder.append(", ");
        }
        return builder.toString();
    }
}
