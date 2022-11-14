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

package androidx.room.compiler.codegen

import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.javapoet.JClassName
import org.junit.Test

class XTypeNameTest {

    @Test
    fun equality() {
        assertThat(
            XTypeName(
                java = JClassName.INT.box(),
                kotlin = INT
            )
        ).isEqualTo(
            XTypeName(
                java = JClassName.INT.box(),
                kotlin = INT
            )
        )

        assertThat(
            XTypeName(
                java = JClassName.INT.box(),
                kotlin = INT
            )
        ).isNotEqualTo(
            XTypeName(
                java = JClassName.INT.box(),
                kotlin = SHORT
            )
        )

        assertThat(
            XTypeName(
                java = JClassName.INT.box(),
                kotlin = SHORT
            )
        ).isNotEqualTo(
            XTypeName(
                java = JClassName.INT.box(),
                kotlin = INT
            )
        )
    }

    @Test
    fun equality_kotlinUnavailable() {
        assertThat(
            XTypeName(
                java = JClassName.INT.box(),
                kotlin = INT
            )
        ).isEqualTo(
            XTypeName(
                java = JClassName.INT.box(),
                kotlin = XTypeName.UNAVAILABLE_KTYPE_NAME
            )
        )

        assertThat(
            XTypeName(
                java = JClassName.INT.box(),
                kotlin = XTypeName.UNAVAILABLE_KTYPE_NAME
            )
        ).isEqualTo(
            XTypeName(
                java = JClassName.INT.box(),
                kotlin = INT
            )
        )

        assertThat(
            XTypeName(
                java = JClassName.SHORT.box(),
                kotlin = SHORT
            )
        ).isNotEqualTo(
            XTypeName(
                java = JClassName.INT.box(),
                kotlin = XTypeName.UNAVAILABLE_KTYPE_NAME
            )
        )

        assertThat(
            XTypeName(
                java = JClassName.INT.box(),
                kotlin = XTypeName.UNAVAILABLE_KTYPE_NAME
            )
        ).isNotEqualTo(
            XTypeName(
                java = JClassName.SHORT.box(),
                kotlin = SHORT
            )
        )
    }

    @Test
    fun hashCode_kotlinUnavailable() {
        val expectedClass = XClassName.get("foo", "Bar")
        assertThat(
            XTypeName(
                java = JClassName.get("foo", "Bar"),
                kotlin = XTypeName.UNAVAILABLE_KTYPE_NAME
            ).hashCode()
        ).isEqualTo(expectedClass.hashCode())
    }

    @Test
    fun rawType() {
        val expectedRawClass = XClassName.get("foo", "Bar")
        assertThat(expectedRawClass.parametrizedBy(String::class.asClassName()).rawTypeName)
            .isEqualTo(expectedRawClass)
    }
}