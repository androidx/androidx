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

package androidx.inspection

import androidx.inspection.rules.JvmtiRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

const val InspectorId = "id1"
const val Method1Signature = "method(ILjava/lang/String;)I"
const val Method2Signature = "method(Ljava/lang/String;I)I"
const val MethodVoidSignature = "methodVoid()V"
const val MethodCharSignature = "methodChar(C)C"
const val MethodBooleanSignature = "methodBoolean(Z)Z"
const val MethodByteSignature = "methodByte(B)B"
const val MethodShortSignature = "methodShort(S)S"
const val MethodIntSignature = "methodInt(I)I"
const val MethodLongSignature = "methodLong(J)J"
const val MethodFloatSignature = "methodFloat(F)F"
const val MethodDoubleSignature = "methodDouble(D)D"
const val MethodStringSignature = "methodString(Ljava/lang/String;)Ljava/lang/String;"

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 28) // Debug.attachJvmtiAgent is not available before API 28
@SmallTest
class DefaultArtToolingTest {
    private var lastInstance: Any? = null
    private var lastArgs: List<Any> = emptyList()
    private var lastResult: Any? = null

    // TODO: "add more testcases similar ArtToolingTest in studio codebase"

    @get:Rule
    val rule = JvmtiRule()

    class Target {
        var v1 = 1
        var v2 = ""
        var next: Target? = null

        fun method(a1: Int, a2: String): Int {
            v1 = a1
            v2 = a2
            return a1 + 1
        }

        fun method(a1: String, a2: Int): Int {
            v1 = a2
            v2 = a1
            return a2 + 1
        }

        fun methodVoid() {}

        fun methodChar(c: Char): Char = c

        fun methodBoolean(b: Boolean): Boolean = b

        fun methodByte(b: Byte): Byte = b

        fun methodShort(s: Short): Short = s

        fun methodInt(i: Int): Int = i

        fun methodLong(l: Long): Long = l

        fun methodFloat(f: Float): Float = f

        fun methodDouble(d: Double): Double = d

        fun methodString(s: String): String = s
    }

    @Test
    fun testHooks() {
        val tooling = DefaultArtTooling(InspectorId)
        setupTargetHooks(tooling)

        val target = Target()
        target.method(5, "Android")
        assertThat(lastInstance).isSameInstanceAs(target)
        assertThat(lastArgs).hasSize(2)
        assertThat(lastArgs[0]).isEqualTo(5)
        assertThat(lastArgs[1]).isEqualTo("Android")
        assertThat(lastResult).isEqualTo(6)
        assertThat(target.v1).isEqualTo(5)
        assertThat(target.v2).isEqualTo("Android")
    }

    @Test
    fun testHooksOfOverloadedMethod() {
        val tooling = DefaultArtTooling(InspectorId)
        setupTargetHooks(tooling)

        val target = Target()
        target.method(5, "Android")
        assertThat(lastInstance).isSameInstanceAs(target)
        assertThat(lastArgs).hasSize(2)
        assertThat(lastArgs[0]).isEqualTo(5)
        assertThat(lastArgs[1]).isEqualTo("Android")
        assertThat(lastResult).isEqualTo(6)
        assertThat(target.v1).isEqualTo(5)
        assertThat(target.v2).isEqualTo("Android")

        target.method("Studio", 7)
        assertThat(lastInstance).isSameInstanceAs(target)
        assertThat(lastArgs).hasSize(2)
        assertThat(lastArgs[0]).isEqualTo("Studio")
        assertThat(lastArgs[1]).isEqualTo(7)
        assertThat(lastResult).isEqualTo(8)
        assertThat(target.v1).isEqualTo(7)
        assertThat(target.v2).isEqualTo("Studio")
    }

    @Test
    fun testDoubleHooks() {
        val tooling = DefaultArtTooling(InspectorId)
        setupTargetHooks(tooling)
        var doubleInstance: Any? = null
        var doubleArgs: List<Any> = emptyList()
        var doubleResult = 0
        tooling.registerEntryHook(Target::class.java, Method1Signature) { thisObject, args ->
            doubleInstance = thisObject
            doubleArgs = args
        }
        tooling.registerExitHook(
            Target::class.java, Method1Signature,
            ArtTooling.ExitHook<Int> { result ->
                doubleResult = result
                result
            })

        val target = Target()
        target.method(5, "Android")
        assertThat(lastInstance).isSameInstanceAs(target)
        assertThat(doubleInstance).isSameInstanceAs(target)
        assertThat(lastArgs).hasSize(2)
        assertThat(lastArgs[0]).isEqualTo(5)
        assertThat(lastArgs[1]).isEqualTo("Android")
        assertThat(doubleArgs).isEqualTo(lastArgs)
        assertThat(lastResult).isEqualTo(6)
        assertThat(doubleResult).isEqualTo(6)
        assertThat(target.v1).isEqualTo(5)
        assertThat(target.v2).isEqualTo("Android")
    }

    @Test
    fun testUnregisterHooks() {
        val tooling = DefaultArtTooling(InspectorId)
        setupTargetHooks(tooling)
        tooling.unregisterHooks()

        val target = Target()
        target.method(5, "Android")
        assertThat(lastInstance).isNull()
        assertThat(lastArgs).isEmpty()
        assertThat(lastResult).isNull()
        assertThat(target.v1).isEqualTo(5)
        assertThat(target.v2).isEqualTo("Android")
    }

    @Test
    fun testExitHooksSupportVoidAndPrimitives() {
        val tooling = DefaultArtTooling(InspectorId)
        setupTargetHooks(tooling)

        val target = Target()
        target.method(5, "Android")
        assertThat(lastResult).isEqualTo(6)

        target.methodVoid()
        assertThat(lastResult).isNull()

        target.methodChar('h')
        assertThat(lastResult).isEqualTo('h')

        target.methodBoolean(true)
        assertThat(lastResult).isEqualTo(true)

        target.methodByte(127)
        assertThat(lastResult).isInstanceOf(java.lang.Byte::class.java)
        assertThat(lastResult).isEqualTo(127)

        target.methodShort(365)
        assertThat(lastResult).isInstanceOf(java.lang.Short::class.java)
        assertThat(lastResult).isEqualTo(365)

        target.methodInt(89)
        assertThat(lastResult).isInstanceOf(java.lang.Integer::class.java)
        assertThat(lastResult).isEqualTo(89)

        target.methodLong(78L)
        assertThat(lastResult).isInstanceOf(java.lang.Long::class.java)
        assertThat(lastResult).isEqualTo(78L)

        target.methodFloat(2.71f)
        assertThat(lastResult).isInstanceOf(java.lang.Float::class.java)
        assertThat(lastResult).isEqualTo(2.71f)

        target.methodDouble(3.14)
        assertThat(lastResult).isInstanceOf(java.lang.Double::class.java)
        assertThat(lastResult).isEqualTo(3.14)

        target.methodString("Hello")
        assertThat(lastResult).isEqualTo("Hello")
    }

    @Test
    fun testFindInstances() {
        val t1 = Target()
        val t2 = Target()
        t1.next = Target()
        t2.next = Target()
        val list = listOf(Target(), Target())

        val tooling = DefaultArtTooling(InspectorId)
        val instances = tooling.findInstances(Target::class.java)
        assertThat(instances).containsExactly(t1, t2, t1.next, t2.next, list[0], list[1])
    }

    private fun setupTargetHooks(tooling: ArtTooling) {
        tooling.registerEntryHook(Target::class.java, Method1Signature) { thisObject, args ->
            lastInstance = thisObject
            lastArgs = args
        }
        tooling.registerEntryHook(Target::class.java, Method2Signature) { thisObject, args ->
            lastInstance = thisObject
            lastArgs = args
        }
        registerExitHook<Int>(tooling, Method1Signature)
        registerExitHook<Int>(tooling, Method2Signature)
        registerExitHook<Void>(tooling, MethodVoidSignature)
        registerExitHook<Char>(tooling, MethodCharSignature)
        registerExitHook<Boolean>(tooling, MethodBooleanSignature)
        registerExitHook<Byte>(tooling, MethodByteSignature)
        registerExitHook<Short>(tooling, MethodShortSignature)
        registerExitHook<Int>(tooling, MethodIntSignature)
        registerExitHook<Long>(tooling, MethodLongSignature)
        registerExitHook<Float>(tooling, MethodFloatSignature)
        registerExitHook<Double>(tooling, MethodDoubleSignature)
        registerExitHook<String>(tooling, MethodStringSignature)
    }

    private fun <T> registerExitHook(tooling: ArtTooling, signature: String) {
        tooling.registerExitHook(
            Target::class.java, signature,
            ArtTooling.ExitHook<T> { result ->
                lastResult = result
                result
            })
    }
}
