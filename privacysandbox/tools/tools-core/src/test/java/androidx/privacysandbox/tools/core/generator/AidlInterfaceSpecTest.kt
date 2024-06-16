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

import androidx.privacysandbox.tools.core.generator.poet.AidlInterfaceSpec
import androidx.privacysandbox.tools.core.generator.poet.AidlMethodSpec
import androidx.privacysandbox.tools.core.model.Type
import androidx.testutils.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AidlInterfaceSpecTest {

    @Test
    fun transactionIdConflict_throws() {
        val methodA = AidlMethodSpec("methodA", listOf(), transactionId = 5)
        val methodB = AidlMethodSpec("methodB", listOf(), transactionId = 5)
        val interfaceBuilder = AidlInterfaceSpec.Builder(Type("com.mysdk", "MySdk"))

        interfaceBuilder.addMethod(methodA)
        val thrown = assertThrows<IllegalStateException> { interfaceBuilder.addMethod(methodB) }
        thrown.hasMessageThat().contains("'methodA'")
        thrown.hasMessageThat().contains("'methodB'")
        thrown.hasMessageThat().contains("'MySdk'")
    }
}
