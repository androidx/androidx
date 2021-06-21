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
 * Tests for calls involving multiple experimental markers.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
class UseJavaExperimentalMultipleMarkersFromJava {

    /**
     * Unsafe call into multiple experimental classes.
     */
    @ExperimentalJavaAnnotation
    int unsafeMultipleExperimentalClasses() {
        AnnotatedJavaClass experimentalObject = new AnnotatedJavaClass();
        AnnotatedJavaClass2 experimentalObject2 = new AnnotatedJavaClass2();
        return experimentalObject.method() + experimentalObject2.field;
    }

    /**
     * Safe call due to propagation of both annotations.
     */
    @ExperimentalJavaAnnotation
    @ExperimentalJavaAnnotation2
    int safePropagateMultipleMarkers() {
        AnnotatedJavaClass experimentalObject = new AnnotatedJavaClass();
        AnnotatedJavaClass2 experimentalObject2 = new AnnotatedJavaClass2();
        return experimentalObject.method() + experimentalObject2.field;
    }

    /**
     * Safe call due to opt-in of one annotation and propagation of another.
     */
    @OptIn(markerClass = ExperimentalJavaAnnotation.class)
    @ExperimentalJavaAnnotation2
    int safePropagateAndOptInMarkers() {
        AnnotatedJavaClass experimentalObject = new AnnotatedJavaClass();
        AnnotatedJavaClass2 experimentalObject2 = new AnnotatedJavaClass2();
        return experimentalObject.method() + experimentalObject2.field;
    }

    /**
     * Safe call due to opt-in of both annotations.
     */
    @OptIn(markerClass = { ExperimentalJavaAnnotation.class, ExperimentalJavaAnnotation2.class })
    int safeOptInMultipleMarkers() {
        AnnotatedJavaClass experimentalObject = new AnnotatedJavaClass();
        AnnotatedJavaClass2 experimentalObject2 = new AnnotatedJavaClass2();
        return experimentalObject.method() + experimentalObject2.field;
    }
}
