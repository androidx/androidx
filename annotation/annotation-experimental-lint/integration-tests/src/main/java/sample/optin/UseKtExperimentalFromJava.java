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

@SuppressWarnings({"unused", "WeakerAccess"})
class UseKtExperimentalFromJava {

    /**
     * Unsafe call into an experimental class.
     */
    int unsafeExperimentalClassField() {
        AnnotatedKotlinClass experimentalObject = new AnnotatedKotlinClass();
        return experimentalObject.method();
    }

    /**
     * Safe call due to propagation of experimental annotation.
     */
    @ExperimentalKotlinAnnotation
    int safePropagateMarker() {
        AnnotatedKotlinClass experimentalObject = new AnnotatedKotlinClass();
        return experimentalObject.method();
    }

    /**
     * Safe call due to opting in to experimental annotation.
     */
    @OptIn(markerClass = ExperimentalKotlinAnnotation.class)
    int safeOptInMarker() {
        AnnotatedKotlinClass experimentalObject = new AnnotatedKotlinClass();
        return experimentalObject.method();
    }

    /**
     * Unsafe call into multiple experimental classes.
     */
    @ExperimentalKotlinAnnotation
    int unsafeMultipleExperimentalClasses() {
        AnnotatedKotlinClass experimentalObject = new AnnotatedKotlinClass();
        return experimentalObject.method() + AnnotatedKotlinClass2.fieldStatic;
    }

    /**
     * Safe call due to propagation of both annotations.
     */
    @ExperimentalKotlinAnnotation
    @ExperimentalKotlinAnnotation2
    int safePropagateMultipleMarkers() {
        AnnotatedKotlinClass experimentalObject = new AnnotatedKotlinClass();
        return experimentalObject.method() + AnnotatedKotlinClass2.fieldStatic;
    }

    /**
     * Safe call due to opt-in of one annotation and propagation of another.
     */
    @OptIn(markerClass = ExperimentalKotlinAnnotation.class)
    @ExperimentalKotlinAnnotation2
    int safePropagateAndOptInMarkers() {
        AnnotatedKotlinClass experimentalObject = new AnnotatedKotlinClass();
        return experimentalObject.method() + AnnotatedKotlinClass2.fieldStatic;
    }

    /**
     * Safe call due to opt-in of both annotations.
     */
    @OptIn(markerClass = {
            ExperimentalKotlinAnnotation.class,
            ExperimentalKotlinAnnotation2.class
    })
    int safeOptInMultipleMarkers() {
        AnnotatedKotlinClass experimentalObject = new AnnotatedKotlinClass();
        return experimentalObject.method() + AnnotatedKotlinClass2.fieldStatic;
    }

    /**
     * Unsafe calls into static methods.
     *
     * Regression test for issue reported in b/140637106, which passes here but fails in Studio.
     */
    void regressionTestStaticUsage() {
        AnnotatedKotlinMembers.methodStatic();
        AnnotatedKotlinMembers.Companion.methodStatic();
    }

    /**
     * Unsafe calls into methods without intermediate variable.
     *
     * Regression test for issue reported in b/140637106, which passes here but fails in Studio.
     */
    void regressionTestInlineUsage() {
        new AnnotatedKotlinMembers().method();
        new AnnotatedKotlinMembers().methodWithJavaMarker();
    }

    /**
     * Safe usage due to opting in to experimental annotation.
     */
    @OptIn(markerClass = ExperimentalKotlinAnnotation.class)
    static class ExtendsAnnotatedKotlinClass extends AnnotatedKotlinClass {}

    /**
     * Safe usage due to opting in to experimental annotation.
     */
    @kotlin.OptIn(markerClass = ExperimentalKotlinAnnotation.class)
    static class ExtendsAnnotatedKotlinClass2 extends AnnotatedKotlinClass {}
}
