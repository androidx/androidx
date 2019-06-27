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

package sample

import androidx.annotation.experimental.UseExperimental

import sample.foo.Bar
import sample.foo.ExperimentalPackage

@Suppress("unused")
class UseJavaPackageFromKt {
    /**
     * Unsafe call into a class within an experimental package.
     */
    fun callPackageUnsafe() {
        val bar = Bar()
        bar.baz()
    }

    @ExperimentalPackage
    fun callPackageExperimental() {
        val bar = Bar()
        bar.baz()
    }

    @UseExperimental(markerClass = ExperimentalPackage::class)
    fun callPackageUseExperimental() {
        val bar = Bar()
        bar.baz()
    }

    fun callSelfUnsafe() {
        callPackageUnsafe()
    }

    /**
     * Unsafe call into an experimental method within this class.
     */
    fun callSelfExperimental() {
        callPackageExperimental()
    }

    fun callSelfUseExperimental() {
        callPackageUseExperimental()
    }
}