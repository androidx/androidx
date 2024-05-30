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

package sample.optin;

import android.annotation.SuppressLint;

/** @noinspection unused*/
@SuppressLint("UnknownNullness")
public class RegressionTestJava313686921 {
    @ExperimentalJavaAnnotation
    public @interface AnnotatedJavaAnnotation {}

    /**
     * Unsafe usage due to the experimental annotation on the annotation
     */
    public Object unsafeAnnotatedAnnotationUsageOnParam(@AnnotatedJavaAnnotation Object param) {
        return param;
    }

    /**
     * Unsafe usage due to the experimental annotation on the annotation
     */
    @AnnotatedJavaAnnotation
    public Object unsafeAnnotatedAnnotationUsageOnMethod(Object param) {
        return param;
    }

    void usage() {
        unsafeAnnotatedAnnotationUsageOnMethod("param");
    }

    @AnnotatedJavaAnnotation
    static class UnsafeAnnotatedAnnotationUsageOnClass {}
}
