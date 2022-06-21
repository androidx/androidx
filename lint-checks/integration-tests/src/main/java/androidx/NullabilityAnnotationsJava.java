/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.sample;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Sample class for testing JetBrains nullability annotations.
 *
 * We are purposely using the non-AndroidX versions of @NotNull and @Nullable in this file for
 * testing the annotations lint check.  @SuppressWarnings is used here to bypass the lint check
 * for this test file.
 */
@SuppressWarnings({"NullabilityAnnotationsDetector", "unused"})
public class NullabilityAnnotationsJava {
    /**
     * Sample method
     * @param arg NotNull arg
     */
    private void method1(@NotNull String arg) {
    }

    /**
     * Sample method
     * @param arg Nullable arg
     */
    private void method2(@Nullable String arg) {
    }
}
