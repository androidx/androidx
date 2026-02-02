/*
 * Copyright 2024 The Android Open Source Project
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

@file:Suppress("JUnitMalformedDeclaration", "unused")

package androidx.benchmark.samples

import androidx.annotation.Sampled
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Sampled
fun baselineProfileRuleSample() {
    @RunWith(AndroidJUnit4::class)
    class AppBaselineProfileGenerator {
        @get:Rule val baselineProfileRule = BaselineProfileRule()

        @Test
        fun startup() =
            baselineProfileRule.collect(packageName = "com.example.my.application.id") {
                pressHome()
                // This block defines the scenario for which profiles are captured.
                // Here we are interested in optimizing for app startup, but you
                // could also navigate and scroll through your most important UI.
                startActivityAndWait()
            }
    }
}

@Sampled
fun baselineProfileRuleLibrarySample() {
    @RunWith(AndroidJUnit4::class)
    class LibraryBaselineProfileGenerator {
        @get:Rule val baselineProfileRule = BaselineProfileRule()

        @Test
        fun libraryStartupRules() =
            baselineProfileRule.collect(
                packageName = "com.example.my.application.id",
                filterPredicate = {
                    // Only capture rules in the library's package, excluding test app code
                    // Rules are prefixed by tag characters, followed by JVM method signature,
                    // e.g. `HSPLcom/mylibrary/LibraryClass;-><init>()V`, where `L`
                    // signifies the start of the package/class, and '/' is divider instead of '.'
                    val libPackagePrefix = "com.mylibrary"
                    it.contains("^.*L${libPackagePrefix.replace(".", "/")}".toRegex())
                },
            ) {
                startActivityAndWait()
            }
    }
}
