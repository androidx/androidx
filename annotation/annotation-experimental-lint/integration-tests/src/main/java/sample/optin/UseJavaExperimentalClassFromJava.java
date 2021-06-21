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

package sample.optin;

import androidx.annotation.OptIn;

/**
 * Tests for calls made to members on an experimental class.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
class UseJavaExperimentalClassFromJava {

    /**
     * Unsafe call into a field on an experimental class.
     */
    int unsafeExperimentalClassField() {
        AnnotatedJavaClass experimentalObject = new AnnotatedJavaClass();
        return experimentalObject.field;
    }

    /**
     * Unsafe call into a method on an experimental class.
     */
    int unsafeExperimentalClassMethod() {
        AnnotatedJavaClass experimentalObject = new AnnotatedJavaClass();
        return experimentalObject.method();
    }

    /**
     * Unsafe call into a static field on an experimental class.
     */
    int unsafeExperimentalClassStaticField() {
        return AnnotatedJavaClass.FIELD_STATIC;
    }

    /**
     * Unsafe call into a static method on an experimental class.
     */
    int unsafeExperimentalClassStaticMethod() {
        return AnnotatedJavaClass.methodStatic();
    }

    /**
     * Safe call due to propagation of experimental annotation.
     */
    @ExperimentalJavaAnnotation
    int safePropagateMarker() {
        AnnotatedJavaClass experimentalObject = new AnnotatedJavaClass();
        return experimentalObject.method();
    }

    /**
     * Safe call due to opting in to experimental annotation.
     */
    @OptIn(markerClass = ExperimentalJavaAnnotation.class)
    int safeOptInMarker() {
        AnnotatedJavaClass experimentalObject = new AnnotatedJavaClass();
        return experimentalObject.method();
    }
}
