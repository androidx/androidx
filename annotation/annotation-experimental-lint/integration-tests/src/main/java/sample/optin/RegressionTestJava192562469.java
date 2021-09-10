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
 * Regression test for b/192562469 where the lint check does not handle annotation usage in lambdas.
 */
@SuppressWarnings("unused")
public class RegressionTestJava192562469 {
    @ExperimentalJavaAnnotation
    interface ExperimentalInterface {
        void experimentalMethod();
    }

    /**
     * Unsafe usage due to implementation of an experimental interface.
     */
    static class ConcreteExperimentalInterface implements ExperimentalInterface { // unsafe
        @Override
        public void experimentalMethod() {} // unsafe override
    }

    /**
     * Safe usage due to opt-in.
     */
    @OptIn(markerClass = ExperimentalJavaAnnotation.class)
    static class ConcreteExperimentalInterfaceOptIn implements ExperimentalInterface {
        @Override
        public void experimentalMethod() {} // safe
    }

    /**
     * Safe usage due to propagation.
     */
    @ExperimentalJavaAnnotation
    static class ConcreteExperimentalInterfacePropagate implements ExperimentalInterface {
        @Override
        public void experimentalMethod() {} // safe
    }

    /**
     * Unsafe implementations of an experimental interface.
     */
    void regressionTestOverrides() {
        @SuppressWarnings("Convert2Lambda")
        ExperimentalInterface anonymous = new ExperimentalInterface() { // unsafe
            @Override
            public void experimentalMethod() {} // unsafe override
        };

        ExperimentalInterface lambda = () -> {}; // unsafe
    }

    /**
     * Safe implementations of an experimental interface due to opt-in.
     */
    @OptIn(markerClass = ExperimentalJavaAnnotation.class)
    void regressionTestOverridesOptIn() {
        @SuppressWarnings("Convert2Lambda")
        ExperimentalInterface anonymous = new ExperimentalInterface() { // safe
            @Override
            public void experimentalMethod() {} // safe
        };

        ExperimentalInterface lambda = () -> {}; // safe
    }

    /**
     * Safe implementations of an experimental interface due to propagation.
     */
    @ExperimentalJavaAnnotation
    void regressionTestOverridesPropagate() {
        @SuppressWarnings("Convert2Lambda")
        ExperimentalInterface anonymous = new ExperimentalInterface() { // safe
            @Override
            public void experimentalMethod() {} // safe
        };

        ExperimentalInterface lambda = () -> {}; // safe
    }
}
