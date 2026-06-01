/*
 * Copyright 2026 The Android Open Source Project
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

package androidx

import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import org.junit.Test

class KotlinRunTestReturnResultKotlin {

    @Test
    fun testUnitNotFlaggedUnitReturn() {
        unit()
    }

    @Test fun shorthandRunTest() = runTest {}

    @Test
    fun conventionalRunTest() {
        // Error: Method annotated with @Test has runTest not returned/used
        runTest {}
    }

    @Test
    fun lostNestedRunTest() {
        lostUtilityFun()
        // Error: Function with return type `TestResult` not returned/used
        propagatedUtilityFun()
    }

    fun lostUtilityFun() {
        // Error: Method not annotated with @Test has runTest not returned/used
        runTest {}
    }

    fun propagatedUtilityFun() = runTest {}

    private fun unit() = Unit

    @Test
    fun runTestUsedAnotherWay() {
        runTest {}.use()
        return run { runTest {} }
    }

    private fun TestResult.use() {}
}
