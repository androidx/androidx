/*
 * Copyright 2019 The Android Open Source Project
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

import sample.optin.foo.AnnotatedJavaPackage;

/**
 * Tests for calls made on classes within an experimental package.
 */
@SuppressWarnings("unused")
class UseJavaPackageFromJava {

    /**
     * Unsafe call into a method on a class within an experimental package.
     */
    void unsafeMethodInExperimentalPackage() {
        AnnotatedJavaPackage experimentalObject = new AnnotatedJavaPackage();
        experimentalObject.method();
    }

    /**
     * Safe call due to propagation of experimental marker.
     */
    @ExperimentalJavaAnnotation
    void safePropagateMarker() {
        AnnotatedJavaPackage experimentalObject = new AnnotatedJavaPackage();
        experimentalObject.method();
    }

    /**
     * Safe call due to opt-in to experimental marker.
     */
    @OptIn(markerClass = ExperimentalJavaAnnotation.class)
    void safeOptInMarker() {
        AnnotatedJavaPackage experimentalObject = new AnnotatedJavaPackage();
        experimentalObject.method();
    }

    /**
     * Unsafe call into a method with an unsafe call. This should not be flagged, as the
     * called method itself is not experimental.
     */
    void unsafeSelfExperimental() {
        unsafeMethodInExperimentalPackage();
    }

    /**
     * Unsafe call into an experimental method within this class.
     */
    void unsafeSelfPropagateMarker() {
        safePropagateMarker();
    }

    /**
     * Safe call into an opted-in method within this class.
     */
    void safeSelfOptInMarker() {
        safeOptInMarker();
    }
}
