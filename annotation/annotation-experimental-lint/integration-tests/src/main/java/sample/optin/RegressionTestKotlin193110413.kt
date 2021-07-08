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
@file:Suppress("unused")

package sample.optin

/**
 * Regression test for b/193110413 where the lint check does not handle nested annotations.
 *
 * This uses the Kotlin compiler's enforcement and is guaranteed to be correct.
 */
class RegressionTestKotlin193110413 {
    @ExperimentalKotlinAnnotation
    internal interface ExperimentalInterface {
        fun experimentalMethod()
        fun anotherExperimentalMethod()

        fun defaultExperimentalMethod() {
            // Stub!
        }
    }

    /**
     * Safe usage due to opting in to the experimental annotation.
     */
    @OptIn(ExperimentalKotlinAnnotation::class)
    internal class Foo : ExperimentalInterface {
        @ExperimentalKotlinAnnotation
        override fun experimentalMethod() {
            // Stub!
        }

        override fun anotherExperimentalMethod() {
            // Stub!
        }

        fun stableClassLevelOptIn() {
            // Stub!
        }
    }

    /**
     * Safe usage due to propagating the experimental annotation.
     */
    @ExperimentalKotlinAnnotation
    internal class Bar : ExperimentalInterface {
        override fun experimentalMethod() {
            // Stub!
        }

        override fun anotherExperimentalMethod() {
            // Stub!
        }

        @OptIn(ExperimentalKotlinAnnotation::class)
        fun stableMethodLevelOptIn() {
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
    fun regressionTestMixedStability() {
        val foo = Foo() // safe
        foo.stableClassLevelOptIn() // safe
        foo.anotherExperimentalMethod() // safe
        foo.defaultExperimentalMethod() // safe
        foo.experimentalMethod() // unsafe
        val bar = Bar() // unsafe
        bar.stableMethodLevelOptIn() // unsafe due to class reference
        bar.experimentalMethod() // unsafe
    }
}