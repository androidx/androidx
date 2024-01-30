/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.AnnotatedValue
import androidx.privacysandbox.tools.core.model.ParsedApi
import androidx.privacysandbox.tools.core.model.Type
import androidx.privacysandbox.tools.core.model.Types
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ServerBinderCodeConverterTest {
    private val converter = ServerBinderCodeConverter(
        ParsedApi(
            services = setOf(
                AnnotatedInterface(
                    type = Type(packageName = "com.mysdk", simpleName = "MySdk"),
                )
            ),
            values = setOf(
                AnnotatedValue(
                    type = Type(packageName = "com.mysdk", simpleName = "Value"),
                    properties = listOf()
                )
            ),
            callbacks = setOf(
                AnnotatedInterface(
                    type = Type(packageName = "com.mysdk", simpleName = "Callback"),
                )
            ),
        )
    )

    @Test
    fun convertToBinderCode_primitive() {
        assertThat(
            converter.convertToBinderCode(
                Types.int, expression = "5"
            ).toString()
        ).isEqualTo("5")
    }

    @Test
    fun convertToBinderCode_value() {
        assertThat(
            converter.convertToBinderCode(
                Type(packageName = "com.mysdk", simpleName = "Value"), expression = "value"
            ).toString()
        ).isEqualTo("com.mysdk.ValueConverter(context).toParcelable(value)")
    }

    @Test
    fun convertToBinderCode_callback() {
        assertThat(
            converter.convertToBinderCode(
                Type(packageName = "com.mysdk", simpleName = "Callback"), expression = "callback"
            ).toString()
        ).isEqualTo("com.mysdk.CallbackStubDelegate(callback)")
    }
}
