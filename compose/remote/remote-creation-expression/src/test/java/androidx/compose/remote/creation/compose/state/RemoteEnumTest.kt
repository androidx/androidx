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
package androidx.compose.remote.creation.compose.state

import androidx.compose.remote.creation.compose.state.RemoteEnum.Companion.createNamedRemoteEnum
import androidx.compose.remote.creation.compose.state.RemoteValue.Domain
import com.google.common.truth.Truth.assertThat
import kotlin.enums.enumEntries
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteEnumTest {
    val context = TestRemoteContext()
    val creationState = testCreationState()

    private enum class Checked {
        Off,
        On,
    }

    private enum class Toggle {
        Off,
        On,
    }

    @Test
    fun ordinal() {
        val b0 = RemoteEnum(Checked.Off)
        val b1 = RemoteEnum(Checked.On)
        val i0 = b0.ordinal
        val i1 = b1.ordinal
        val i0Id = creationState.getId(i0)
        val i1Id = creationState.getId(i1)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(i0Id)).isEqualTo(0)
        assertThat(context.getInteger(i1Id)).isEqualTo(1)
    }

    @Test
    fun toRemoteInt() {
        val b0 = RemoteEnum(Checked.Off)
        val b1 = RemoteEnum(Checked.On)
        val i0 = b0.toRemoteInt { 10.ri }
        val i1 = b1.toRemoteInt { 20.ri }
        val i0Id = creationState.getId(i0)
        val i1Id = creationState.getId(i1)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(i0Id)).isEqualTo(10)
        assertThat(context.getInteger(i1Id)).isEqualTo(20)
    }

    @Test
    fun toRemoteString() {
        val b0 = RemoteEnum(Checked.Off)
        val b1 = RemoteEnum(Checked.On)
        val i0 = b0.toRemoteString { it.name.rs }
        val i1 = b1.toRemoteString()
        val i0Id = creationState.getId(i0)
        val i1Id = creationState.getId(i1)
        makeAndPaintCoreDocument()

        assertThat(context.getText(i0Id)).isEqualTo("Off")
        assertThat(context.getText(i1Id)).isEqualTo("On")
    }

    @Test
    fun constantValue() {
        assertThat(RemoteEnum(Checked.Off).constantValue).isEqualTo(Checked.Off)

        val variableInt = (RemoteTimeVariables.continuousSeconds % 1.rf).toRemoteInt()
        assertThat(RemoteEnum(variableInt, enumEntries<Checked>()).constantValueOrNull).isNull()
    }

    @Test
    fun cacheKeys() {
        val constant = RemoteEnum(Checked.Off)
        assertThat(constant.cacheKey).isEqualTo(RemoteConstantCacheKey(Checked.Off))

        val named = createNamedRemoteEnum<Checked>("test", Checked.Off)
        assertThat(named.cacheKey).isEqualTo(RemoteNamedCacheKey(Domain.User, "test"))
    }

    @Test
    fun toString_constant() {
        val constEnum = RemoteEnum(Checked.Off)
        assertThat(constEnum.toString()).isEqualTo("Off")
    }

    @Test
    fun toString_namedVariable() {
        val namedEnum = createNamedRemoteEnum<Checked>("chk", Checked.Off)
        assertThat(namedEnum.toString()).isEqualTo("user:chk")
        assertThat(namedEnum.toRemoteString().toString()).isEqualTo("user:chk.toRemoteString()")
    }

    @Test
    fun toString_equality() {
        val constEnum = RemoteEnum(Checked.Off)
        val namedEnum = createNamedRemoteEnum<Checked>("chk", Checked.Off)
        val eq = namedEnum.ordinal.isEqualTo(constEnum.ordinal)
        assertThat(eq.toString()).isEqualTo("user:chk.ordinal == 0")
        assertThat(namedEnum.ordinal.isEqualTo(Checked.Off.ordinal.ri).toString())
            .isEqualTo("user:chk.ordinal == 0")
    }

    @Test
    fun toString_mapping() {
        val namedEnum = createNamedRemoteEnum<Checked>("chk", Checked.Off)
        val mappedInt = namedEnum.toRemoteInt { if (it == Checked.Off) 10.ri else 20.ri }
        assertThat(mappedInt.toString())
            .isEqualTo("user:chk.ordinal == 1 ? 20 : (user:chk.ordinal == 0 ? 10 : -1)")
    }

    @Test
    fun equality_sameEnumType_equal() {
        val intVar = RemoteInt.createNamedRemoteInt("state", 0)
        val enum1 = RemoteEnum(intVar, Checked.entries)
        val enum2 = RemoteEnum(intVar, Checked.entries)

        assertThat(enum1).isEqualTo(enum2)
        assertThat(enum1.hashCode()).isEqualTo(enum2.hashCode())
        assertThat(RemoteEnum(Checked.Off)).isEqualTo(RemoteEnum(Checked.Off))
        assertThat(RemoteEnum(Checked.Off).hashCode()).isEqualTo(RemoteEnum(Checked.Off).hashCode())
    }

    @Test
    fun equality_differentEnumTypesOverSameRemoteInt_notEqual() {
        val intVar = RemoteInt.createNamedRemoteInt("state", 0)
        val checkedEnum = RemoteEnum(intVar, Checked.entries)
        val toggleEnum = RemoteEnum(intVar, Toggle.entries)

        assertThat(checkedEnum).isNotEqualTo(toggleEnum)
        assertThat(toggleEnum).isNotEqualTo(checkedEnum)
        assertThat(RemoteEnum(Checked.Off)).isNotEqualTo(RemoteEnum(Toggle.Off))

        val mutableInt = MutableRemoteInt(0)
        val mutableChecked = MutableRemoteEnum(mutableInt, Checked.entries)
        val mutableToggle = MutableRemoteEnum(mutableInt, Toggle.entries)
        assertThat(mutableChecked).isNotEqualTo(mutableToggle)
        assertThat(mutableToggle).isNotEqualTo(mutableChecked)
    }

    @Test
    fun equality_readOnlyAndMutableOverSameMutableInt_notEqual() {
        val mutableInt = MutableRemoteInt(0)
        val readOnlyEnum = RemoteEnum(mutableInt, Checked.entries)
        val mutableEnum = MutableRemoteEnum(mutableInt, Checked.entries)

        assertThat(readOnlyEnum).isNotEqualTo(mutableEnum)
        assertThat(mutableEnum).isNotEqualTo(readOnlyEnum)
    }

    @Test
    fun equality_mutableEnum_sameVsDistinctBackingInt() {
        val mutableInt = MutableRemoteInt(0)
        val wrapper1 = MutableRemoteEnum(mutableInt, Checked.entries)
        val wrapper2 = MutableRemoteEnum(mutableInt, Checked.entries)

        assertThat(wrapper1).isEqualTo(wrapper2)
        assertThat(wrapper1.hashCode()).isEqualTo(wrapper2.hashCode())

        val separate1 = MutableRemoteEnum(Checked.Off)
        val separate2 = MutableRemoteEnum(Checked.Off)
        assertThat(separate1).isNotEqualTo(separate2)
        assertThat(wrapper1).isNotEqualTo(separate1)
    }

    private fun makeAndPaintCoreDocument() = context.loadDocument(creationState)
}
