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

package sample.optin.foo;

import androidx.annotation.OptIn;

import sample.optin.ExperimentalJavaAnnotation;

/**
 * Regression test for b/218798815 where the lint check yields false positives on usages within an
 * annotated package.
 */
@SuppressWarnings("unused")
public class RegressionTestJava218798815 {

    /**
     * Safe call into a method on a class within the same experimental package.
     */
    void safeMethodInExperimentalPackage() {
        AnnotatedJavaPackage experimentalObject = new AnnotatedJavaPackage();
        experimentalObject.method();
    }

    /**
     * Safe call with redundant propagation of experimental marker.
     */
    @ExperimentalJavaAnnotation
    void safePropagateMarker() {
        AnnotatedJavaPackage experimentalObject = new AnnotatedJavaPackage();
        experimentalObject.method();
    }

    /**
     * Safe call with unnecessary and invalid opt-in to experimental marker.
     */
    @OptIn(markerClass = ExperimentalJavaAnnotation.class)
    void safeOptInMarker() {
        AnnotatedJavaPackage experimentalObject = new AnnotatedJavaPackage();
        experimentalObject.method();
    }

    /**
     * Safe call into a method with an safe call. This should not be flagged, as the
     * called method itself is not experimental.
     */
    void safeSelfExperimental() {
        safeMethodInExperimentalPackage();
    }

    /**
     * Safe call into a redundantly-annotated experimental method within the same experimetnal
     * package.
     */
    void safeSelfPropagateMarker() {
        safePropagateMarker();
    }
}
