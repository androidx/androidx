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

package androidx.room.integration.kotlintestapp

import org.junit.AssumptionViolatedException

/**
 * Helper class to read test configuration via BuildConfig.
 */
object RoomTestConfig {
    /**
     * true if the test code is built by ksp
     * This can help enable/disable certain tests for ksp
     */
    val isKsp
        get() = BuildConfig.FLAVOR == "withKsp"
}

/**
 * Helper method to check if the test was compiled with KSP, and if not, throw an assumption
 * violation exception to skip the test.
 */
fun assumeKsp() {
    if (!RoomTestConfig.isKsp) {
        throw AssumptionViolatedException("test is supported only in KSP")
    }
}