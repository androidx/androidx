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

import androidx.privacysandbox.tools.core.generator.poet.AidlMethodSpec
import androidx.privacysandbox.tools.core.generator.poet.AidlParameterSpec
import androidx.privacysandbox.tools.core.generator.poet.AidlTypeKind
import androidx.privacysandbox.tools.core.generator.poet.AidlTypeSpec
import androidx.privacysandbox.tools.core.model.Type
import com.google.common.hash.Hashing
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AidlMethodSpecTest {

    @Test
    fun automaticallyCreatesTransactionId() {
        val name = "methodName"
        val parameters =
            listOf(
                AidlParameterSpec(
                    "foo",
                    AidlTypeSpec(
                        Type("", "boolean"),
                        AidlTypeKind.PRIMITIVE,
                        isList = true,
                    )
                ),
                AidlParameterSpec(
                    "bar",
                    AidlTypeSpec(
                        Type("com.mysdk", "IListStringTransactionCallback"),
                        AidlTypeKind.INTERFACE
                    )
                ),
            )
        val method = AidlMethodSpec(name, parameters)

        val expectedSignature = "methodName(boolean[],com.mysdk.IListStringTransactionCallback)"
        val expectedHash =
            Hashing.farmHashFingerprint64()
                .hashString(expectedSignature, Charsets.UTF_8)
                .asLong()
                .toULong()
        val expectedTransactionId = expectedHash % (0xffff9bUL - 100UL - 65535UL)

        Truth.assertThat(method.transactionId).isEqualTo(expectedTransactionId.toInt())
    }
}
