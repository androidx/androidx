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
 * Regression test for b/193110413 where the lint check does not handle nested annotations.
 */
@SuppressWarnings("unused")
public class RegressionTestJava193110413 {

    @ExperimentalJavaAnnotation
    interface ExperimentalInterface {
        void experimentalMethod();
        void anotherExperimentalMethod();

        default void defaultExperimentalMethod() {
            // Stub!
        }
    }

    /**
     * Safe usage due to opting in to the experimental annotation.
     */
    @OptIn(markerClass = ExperimentalJavaAnnotation.class)
    static class Foo implements ExperimentalInterface {

        @ExperimentalJavaAnnotation
        @Override
        public void experimentalMethod() {
            // Stub!
        }

        @Override
        public void anotherExperimentalMethod() {
            // Stub!
        }

        public void stableClassLevelOptIn() {
            // Stub!
        }
    }

    /**
     * Safe usage due to propagating the experimental annotation.
     */
    @ExperimentalJavaAnnotation
    static class Bar implements ExperimentalInterface {

        @Override
        public void experimentalMethod() {
            // Stub!
        }

        @Override
        public void anotherExperimentalMethod() {
            // Stub!
        }

        @OptIn(markerClass = ExperimentalJavaAnnotation.class)
        public void stableMethodLevelOptIn() {
            // Stub!
        }
    }

    /**
     * Unsafe call to an experimental method where the containing class has opted-in to an
     * unstable interface, thus the constructor and stable method calls are safe.
     *
     * The expected behavior has been verified against the Kotlin compiler's implementation of
     * opt-in.
     */
    void regressionTestMixedStability() {
        Foo foo = new Foo(); // safe
        foo.stableClassLevelOptIn(); // safe
        foo.anotherExperimentalMethod(); // safe
        foo.defaultExperimentalMethod(); // unsafe in Java but safe in Kotlin
        foo.experimentalMethod(); // unsafe

        Bar bar = new Bar(); // unsafe
        bar.stableMethodLevelOptIn(); // unsafe due to experimental class scope
        bar.experimentalMethod(); // unsafe
    }
}
