/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.privacysandbox.tools.core.generator

import org.junit.After
import org.junit.Before
import org.junit.Test

// TODO(b/269458005): Remove this test once we no longer need the fallback.
class FrameworkAidlFallbackTest {
    lateinit var frameworkPath: String

    @Before
    fun before() {
        frameworkPath = System.clearProperty("framework_aidl_path")!!
    }

    @After
    fun after() {
        System.setProperty("framework_aidl_path", frameworkPath)
    }

    @Test
    fun aidlTestsWithoutFrameworkPath() {
        AidlCallbackGeneratorTest().generate()
        AidlInterfaceGeneratorTest().generate()
        AidlServiceGeneratorTest().generate()
        AidlValueGeneratorTest().generate()
    }
}
