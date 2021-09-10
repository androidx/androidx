/*
 * Copyright 2021 The Android Open Source Project
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

package androidx;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Sample class used to verify that ConcurrentHashMap lint check is running.
 */
@SuppressWarnings("unused")
public class Sample {

    /**
     * This function does not specify the nullability of its return type.
     * Lint should catch this and report an error.
     * If Lint does not catch this, then Lint's intrinsic checks are not running
     */
    public static Sample confirmIntrinisicLintChecksRun() {
        return null;
    }

    /**
     * This function uses a disallowed annotation
     * Lint should catch this and report an error.
     * If Lint does not catch this, then our AndroidX-specific checks are not running
     */
    public static void confirmCustomAndroidXChecksRun(ConcurrentHashMap m) {
    }

    private Sample() {
    }
}
