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

package sample;

import androidx.annotation.experimental.UseExperimental;

import sample.foo.Bar;
import sample.foo.ExperimentalPackage;

@SuppressWarnings("unused")
class UseJavaPackageFromJava {
    /**
     * Unsafe call into a class within an experimental package.
     */
    void callPackageUnsafe() {
        Bar bar = new Bar();
        bar.baz();
    }

    @ExperimentalPackage
    void callPackageExperimental() {
        Bar bar = new Bar();
        bar.baz();
    }

    @UseExperimental(markerClass = ExperimentalPackage.class)
    void callPackageUseExperimental() {
        Bar bar = new Bar();
        bar.baz();
    }

    void callSelfUnsafe() {
        callPackageUnsafe();
    }

    /**
     * Unsafe call into an experimental method within this class.
     */
    void callSelfExperimental() {
        callPackageExperimental();
    }

    void callSelfUseExperimental() {
        callPackageUseExperimental();
    }
}
