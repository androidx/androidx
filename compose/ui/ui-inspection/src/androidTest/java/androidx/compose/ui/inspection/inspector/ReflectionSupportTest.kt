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

package androidx.compose.ui.inspection.inspector

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Test

interface I1

interface I2

interface I3

interface I4

interface I5

open class A : I1

open class B : A(), I2

open class C : B(), I3

open class D : C(), I1, I4, I5

val TOP_C1 = Size(width = 5.7f, height = -12.0f)
val TOP_C2 = "A package constant"

class WithCompanion {
    companion object {
        val C1 = Size(width = 3.5f, height = 5.0f)
        val C2 = 17
    }
}

object AsObject {
    val C1 = Offset(x = 1.0f, y = 10.0f)
    val C2 = "SomeConstant"
}

@Suppress("unused")
open class SomeClass {
    val c1 = Offset(x = 1.0f, y = 10.0f)
    var c2 = Offset(x = 0f, y = 0f)
    lateinit var c3: String
    open val c4 = "SomeValue"
}

class ReflectionSupportTest {

    @Test
    fun testSuper() {
        assertThat(D::class.java.allSuperclasses)
            .containsExactly(
                A::class.java,
                B::class.java,
                C::class.java,
                D::class.java,
                I1::class.java,
                I2::class.java,
                I3::class.java,
                I4::class.java,
                I5::class.java,
            )
    }

    @Test
    fun testCompanionObject() {
        val support = ReflectionSupport(InlineClassConverter())
        val companionObject = support.objectInstance(WithCompanion::class.java)
        val members = support.declaredMemberProperties(companionObject!!.javaClass)
        val c1 = members.first()
        val c2 = members.last()
        assertThat(c1.name).isEqualTo("C1")
        assertThat(c2.name).isEqualTo("C2")
        assertThat(support.valueOf(c1, companionObject)).isEqualTo(WithCompanion.C1)
        assertThat(support.valueOf(c2, companionObject)).isEqualTo(WithCompanion.C2)
    }

    @Test
    fun testObject() {
        val support = ReflectionSupport(InlineClassConverter())
        val members = support.declaredMemberProperties(AsObject::class.java)
        val c1 = members.first()
        val c2 = members.last()
        assertThat(c1.name).isEqualTo("C1")
        assertThat(c2.name).isEqualTo("C2")
        assertThat(support.valueOf(c1, AsObject)).isEqualTo(AsObject.C1)
        assertThat(support.valueOf(c2, AsObject)).isEqualTo(AsObject.C2)
    }

    @Test
    fun testPackageMembers() {
        val support = ReflectionSupport(InlineClassConverter())
        val packageClass =
            Class.forName("androidx.compose.ui.inspection.inspector.ReflectionSupportTestKt")
        val members = support.declaredPackageProperties(packageClass)
        val c1 = members.first()
        val c2 = members.last()
        assertThat(c1.name).isEqualTo("TOP_C1")
        assertThat(c2.name).isEqualTo("TOP_C2")
        assertThat(support.valueOf(c1, null)).isEqualTo(TOP_C1)
        assertThat(support.valueOf(c2, null)).isEqualTo(TOP_C2)
    }

    @Test
    fun testPropertyAttributes() {
        val support = ReflectionSupport(InlineClassConverter())
        val members = support.declaredMemberProperties(SomeClass::class.java).toList()
        val c1 = members[0]
        val c2 = members[1]
        val c3 = members[2]
        val c4 = members[3]

        assertThat(c1.name).isEqualTo("c1")
        assertThat(c1.isVar).isFalse()
        assertThat(c1.isFinal).isTrue()
        assertThat(c1.isLateInit).isFalse()

        assertThat(c2.name).isEqualTo("c2")
        assertThat(c2.isVar).isTrue()
        assertThat(c2.isFinal).isTrue()
        assertThat(c2.isLateInit).isFalse()

        assertThat(c3.name).isEqualTo("c3")
        assertThat(c3.isVar).isTrue()
        assertThat(c3.isFinal).isTrue()
        assertThat(c3.isLateInit).isTrue()

        assertThat(c4.name).isEqualTo("c4")
        assertThat(c4.isVar).isFalse()
        assertThat(c4.isFinal).isFalse()
        assertThat(c4.isLateInit).isFalse()
    }
}
