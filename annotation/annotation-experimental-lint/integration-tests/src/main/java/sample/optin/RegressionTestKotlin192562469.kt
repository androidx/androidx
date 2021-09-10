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
@file:Suppress("unused", "ObjectLiteralToLambda", "UNUSED_VARIABLE")

package sample.optin

/**
 * Regression test for b/192562469 where the lint check does not handle annotation usage in lambdas.
 */
class RegressionTestKotlin192562469 {
    @ExperimentalKotlinAnnotation
    internal fun interface ExperimentalInterface {
        fun experimentalMethod()
    }

    /**
     * Unsafe usage due to implementation of an experimental interface.
     */
    internal class ConcreteExperimentalInterface : ExperimentalInterface { // unsafe
        override fun experimentalMethod() {} // unsafe override
    }

    /**
     * Safe usage due to opt-in.
     */
    @OptIn(ExperimentalKotlinAnnotation::class)
    internal class ConcreteExperimentalInterfaceOptIn : ExperimentalInterface {
        override fun experimentalMethod() {}
    }

    /**
     * Safe usage due to propagation.
     */
    @ExperimentalKotlinAnnotation
    internal class ConcreteExperimentalInterfacePropagate : ExperimentalInterface {
        override fun experimentalMethod() {}
    }

    /**
     * Unsafe implementations of an experimental interface.
     */
    fun regressionTestOverrides() {
        val anonymous: ExperimentalInterface = object : ExperimentalInterface { // unsafe
            override fun experimentalMethod() {} // unsafe override
        }
        val lambda = ExperimentalInterface {} // unsafe
    }

    /**
     * Safe implementations of an experimental interface due to opt-in.
     */
    @OptIn(ExperimentalKotlinAnnotation::class)
    fun regressionTestOverridesOptIn() {
        val anonymous: ExperimentalInterface = object : ExperimentalInterface {
            override fun experimentalMethod() {} // safe
        }
        val lambda = ExperimentalInterface {} // safe
    }

    /**
     * Safe implementations of an experimental interface due to propagation.
     */
    @ExperimentalKotlinAnnotation
    fun regressionTestOverridesPropagate() {
        val anonymous: ExperimentalInterface = object : ExperimentalInterface {
            override fun experimentalMethod() {} // safe
        }
        val lambda = ExperimentalInterface {} // safe
    }
}
