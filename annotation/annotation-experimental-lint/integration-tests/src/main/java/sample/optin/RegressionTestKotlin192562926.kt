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
 * Regression test for b/192562926 where the lint check should not flag overrides where there is
 * no dependency on the superclass, e.g. calls to super().
 */
class RegressionTestKotlin192562926 {
    internal fun interface StableInterface {
        @ExperimentalKotlinAnnotation
        fun experimentalMethod()
    }

    /**
     * Unsafe override since super is not called.
     */
    internal class ConcreteStableInterface : StableInterface {
        override fun experimentalMethod() {} // unsafe override
    }

    /**
     * Test different approaches to overriding interface methods.
     */
    fun regressionTestOverrides() {
        val anonymous: StableInterface = object : StableInterface {
            override fun experimentalMethod() {} // unsafe override
        }
        val lambda = StableInterface {} // safe (due to bug in Kotlin compiler)
    }
}
