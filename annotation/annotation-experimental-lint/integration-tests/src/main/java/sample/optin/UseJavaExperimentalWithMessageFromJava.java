/*
 * Copyright 2024 The Android Open Source Project
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
 * Tests for calls made to members on an experimental (with message) class.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class UseJavaExperimentalWithMessageFromJava {

    int unsafeExperimentalClassField() {
        AnnotatedJavaClassWithMessage experimentalObject = new AnnotatedJavaClassWithMessage();
        return experimentalObject.field;
    }

    /**
     * Unsafe call into a method on an experimental class.
     */
    int unsafeExperimentalClassMethod() {
        AnnotatedJavaClassWithMessage experimentalObject = new AnnotatedJavaClassWithMessage();
        return experimentalObject.method();
    }

    /**
     * Unsafe call into a static field on an experimental class.
     */
    int unsafeExperimentalClassStaticField() {
        return AnnotatedJavaClassWithMessage.FIELD_STATIC;
    }

    /**
     * Unsafe call into a static method on an experimental class.
     */
    int unsafeExperimentalClassStaticMethod() {
        return AnnotatedJavaClassWithMessage.methodStatic();
    }

    /**
     * Safe call due to propagation of experimental annotation with message.
     */
    @ExperimentalJavaAnnotationWithMessage
    int safePropagateMarker() {
        AnnotatedJavaClassWithMessage experimentalObject = new AnnotatedJavaClassWithMessage();
        return experimentalObject.method();
    }

    /**
     * Safe call due to opting in to experimental annotation with message.
     */
    @OptIn(markerClass = ExperimentalJavaAnnotationWithMessage.class)
    int safeOptInMarker() {
        AnnotatedJavaClassWithMessage experimentalObject = new AnnotatedJavaClassWithMessage();
        return experimentalObject.method();
    }

}
