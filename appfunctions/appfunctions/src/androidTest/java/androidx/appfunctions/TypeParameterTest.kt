/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appfunctions.internal.AppFunctionSerializableFactory
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class TypeParameterTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun primitiveTypeParameter_getSet_success() {
        val builder = AppFunctionData.Builder("")
        val intParam =
            AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(Int::class.java)
        val stringParam =
            AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(String::class.java)
        val longParam =
            AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(Long::class.java)
        val doubleParam =
            AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(Double::class.java)
        val floatParam =
            AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(Float::class.java)
        val booleanParam =
            AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(Boolean::class.java)
        // Array types
        val intArrayParam =
            AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(
                IntArray::class.java
            )
        val longArrayParam =
            AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(
                LongArray::class.java
            )
        val booleanArrayParam =
            AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(
                BooleanArray::class.java
            )
        val doubleArrayParam =
            AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(
                DoubleArray::class.java
            )
        val floatArrayParam =
            AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(
                FloatArray::class.java
            )
        val byteArrayParam =
            AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(
                ByteArray::class.java
            )
        val pendingIntentParam =
            AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(
                PendingIntent::class.java
            )
        val pendingIntent =
            PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)

        // Set values
        intParam.setValueInAppFunctionData(builder, "intKey", 42)
        stringParam.setValueInAppFunctionData(builder, "stringKey", "test")
        longParam.setValueInAppFunctionData(builder, "longKey", 42L)
        doubleParam.setValueInAppFunctionData(builder, "doubleKey", 42.0)
        floatParam.setValueInAppFunctionData(builder, "floatKey", 42.0f)
        booleanParam.setValueInAppFunctionData(builder, "boolKey", true)
        // Set array values
        intArrayParam.setValueInAppFunctionData(builder, "intArrayKey", intArrayOf(1, 2, 3))
        longArrayParam.setValueInAppFunctionData(
            builder,
            "longArrayKey",
            longArrayOf(10L, 20L, 30L),
        )
        booleanArrayParam.setValueInAppFunctionData(
            builder,
            "booleanArrayKey",
            booleanArrayOf(true, false, true),
        )
        doubleArrayParam.setValueInAppFunctionData(
            builder,
            "doubleArrayKey",
            doubleArrayOf(1.0, 2.5, 3.14),
        )
        floatArrayParam.setValueInAppFunctionData(
            builder,
            "floatArrayKey",
            floatArrayOf(0.5f, 1.5f, 2.5f),
        )
        byteArrayParam.setValueInAppFunctionData(
            builder,
            "byteArrayKey",
            byteArrayOf(0x1, 0x2, 0x3),
        )
        pendingIntentParam.setValueInAppFunctionData(builder, "pendingIntentKey", pendingIntent)

        val afd = builder.build()

        // Assert values
        assertThat(intParam.getFromAppFunctionData(afd, "intKey")).isEqualTo(42)
        assertThat(stringParam.getFromAppFunctionData(afd, "stringKey")).isEqualTo("test")
        assertThat(longParam.getFromAppFunctionData(afd, "longKey")).isEqualTo(42L)
        assertThat(doubleParam.getFromAppFunctionData(afd, "doubleKey")).isEqualTo(42.0)
        assertThat(floatParam.getFromAppFunctionData(afd, "floatKey")).isEqualTo(42.0f)
        assertThat(booleanParam.getFromAppFunctionData(afd, "boolKey")).isTrue()
        // Assert array values
        assertThat(intArrayParam.getFromAppFunctionData(afd, "intArrayKey"))
            .asList()
            .containsExactly(1, 2, 3)
            .inOrder()
        assertThat(longArrayParam.getFromAppFunctionData(afd, "longArrayKey"))
            .asList()
            .containsExactly(10L, 20L, 30L)
            .inOrder()
        assertThat(booleanArrayParam.getFromAppFunctionData(afd, "booleanArrayKey"))
            .asList()
            .containsExactly(true, false, true)
            .inOrder()
        assertThat(doubleArrayParam.getFromAppFunctionData(afd, "doubleArrayKey").asList())
            .containsExactly(1.0, 2.5, 3.14)
            .inOrder()
        assertThat(floatArrayParam.getFromAppFunctionData(afd, "floatArrayKey").asList())
            .containsExactly(0.5f, 1.5f, 2.5f)
            .inOrder()
        assertThat(byteArrayParam.getFromAppFunctionData(afd, "byteArrayKey"))
            .asList()
            .containsExactly(0x1.toByte(), 0x2.toByte(), 0x3.toByte())
            .inOrder()
        assertThat(pendingIntentParam.getFromAppFunctionData(afd, "pendingIntentKey"))
            .isEqualTo(pendingIntent)
    }

    @Test
    fun primitiveListTypeParameter_getSet_success() {
        val builder = AppFunctionData.Builder("")
        val stringListParam =
            AppFunctionSerializableFactory.TypeParameter.PrimitiveListTypeParameter<
                String,
                List<String>,
            >(
                String::class.java
            )
        val stringList = listOf("1", "2", "3")
        val pendingIntentParam =
            AppFunctionSerializableFactory.TypeParameter.PrimitiveListTypeParameter<
                PendingIntent,
                List<PendingIntent>,
            >(
                PendingIntent::class.java
            )
        val pendingIntentList =
            listOf(PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE))

        stringListParam.setValueInAppFunctionData(builder, "stringListKey", stringList)
        pendingIntentParam.setValueInAppFunctionData(
            builder,
            "pendingIntentListKey",
            pendingIntentList,
        )

        val afd = builder.build()
        assertThat(stringListParam.getFromAppFunctionData(afd, "stringListKey"))
            .containsExactlyElementsIn(stringList)
        assertThat(pendingIntentParam.getFromAppFunctionData(afd, "pendingIntentListKey"))
            .containsExactlyElementsIn(pendingIntentList)
    }

    @Test
    fun serializableTypeParameter_getSet_success() {
        val builder = AppFunctionData.Builder("")
        val param =
            AppFunctionSerializableFactory.TypeParameter.SerializableTypeParameter(
                Attachment::class.java,
                `$AttachmentFactory`(),
            )

        param.setValueInAppFunctionData(builder, "serializableKey", Attachment("uri"))

        assertThat(param.getFromAppFunctionData(builder.build(), "serializableKey"))
            .isEqualTo(Attachment("uri"))
    }

    @Test
    fun serializableListTypeParameter_getSet_success() {
        val builder = AppFunctionData.Builder("")
        val param =
            AppFunctionSerializableFactory.TypeParameter.SerializableListTypeParameter<
                Attachment,
                List<Attachment>,
            >(
                Attachment::class.java,
                `$AttachmentFactory`(),
            )

        param.setValueInAppFunctionData(builder, "serializableListKey", listOf(Attachment("uri")))

        assertThat(param.getFromAppFunctionData(builder.build(), "serializableListKey"))
            .containsExactly(Attachment("uri"))
    }

    @Test
    fun primitiveTypeParameter_setValueInAppFunctionBuilder_illegalType() {
        val builder = AppFunctionData.Builder("")
        val param =
            AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(Map::class.java)

        assertFailsWith<IllegalStateException> {
            param.setValueInAppFunctionData(builder, "mapKey", mapOf("key" to "value"))
        }
    }

    @Test
    fun primitiveListTypeParameter_setValueInAppFunctionBuilder_illegalType() {
        val builder = AppFunctionData.Builder("")
        val param =
            AppFunctionSerializableFactory.TypeParameter.PrimitiveListTypeParameter<Int, List<Int>>(
                Int::class.java
            )

        assertFailsWith<IllegalStateException> {
            param.setValueInAppFunctionData(builder, "illegal", listOf(10))
        }
    }

    @Test
    fun primitiveTypeParameter_getFromAppFunctionData_illegalType() {
        val afd = AppFunctionData.Builder("").build()
        val param =
            AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(Map::class.java)

        assertFailsWith<IllegalStateException> { param.getFromAppFunctionData(afd, "") }
    }

    @Test
    fun primitiveListTypeParameter_getFromAppFunctionData_illegalType() {
        val afd = AppFunctionData.Builder("").build()
        val param =
            AppFunctionSerializableFactory.TypeParameter.PrimitiveListTypeParameter<Int, List<Int>>(
                Int::class.java
            )

        assertFailsWith<IllegalStateException> { param.getFromAppFunctionData(afd, "") }
    }
}
