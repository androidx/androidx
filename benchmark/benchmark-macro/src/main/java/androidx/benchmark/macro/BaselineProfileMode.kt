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

package androidx.benchmark.macro

/**
 * Choice of how the Baseline Profile in a target application should be included or ignored during pre-compilation.
 */
enum class BaselineProfileMode {
    /**
     * Require the BaselineProfile methods/classes from the target app to be pre-compiled.
     *
     * If the ProfileInstaller library or Baseline Profile isn't present in the target app, an
     * exception will be thrown at compilation time.
     */
    Require,

    /**
     * Include the BaselineProfile methods/classes from the target app into the compilation step if
     * a Baseline Profile and the ProfileInstaller library are both present in the target.
     *
     * This is the same as [Require], except it logs instead of throwing when the
     * Baseline Profile or ProfileInstaller library aren't present in the target application.
     */
    UseIfAvailable,

    /**
     * Do not include the Baseline Profile, if present, in the compilation of the target app.
     */
    Disable
}
