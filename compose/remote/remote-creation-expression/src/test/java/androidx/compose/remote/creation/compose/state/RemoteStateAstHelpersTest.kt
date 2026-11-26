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

package androidx.compose.remote.creation.compose.state

import androidx.compose.remote.core.operations.Utils
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteStateAstHelpersTest {

    private enum class SampleEnum {
        A,
        B,
    }

    private enum class OtherEnum {
        X,
        Y,
    }

    private val x = RemoteFloat.createNamedRemoteFloat("x", 1f)
    private val n = RemoteInt.createNamedRemoteInt("n", 1)
    private val s = RemoteString.createNamedRemoteString("s", "abc")
    private val b = RemoteBoolean.createNamedRemoteBoolean("b", true)
    private val c = RemoteColor.createNamedRemoteColor("c", 0xFFFF0000.toInt())

    @Test
    fun valueKind_classifiesAllRemoteValueDomains() {
        assertThat(1f.rf.valueKind).isEqualTo(RemoteValueKind.FLOAT)
        assertThat(1.ri.valueKind).isEqualTo(RemoteValueKind.INT)
        assertThat(RemoteBoolean(true).valueKind).isEqualTo(RemoteValueKind.BOOLEAN)
        assertThat("hi".rs.valueKind).isEqualTo(RemoteValueKind.STRING)
        assertThat(RemoteColor(0xFF00FF00.toInt()).valueKind).isEqualTo(RemoteValueKind.COLOR)
        assertThat(RemoteDp(4f.rf).valueKind).isEqualTo(RemoteValueKind.DP)
        assertThat(RemoteTextUnit(14f).valueKind).isEqualTo(RemoteValueKind.TEXT_UNIT)
        assertThat(RemoteLong(5L).valueKind).isEqualTo(RemoteValueKind.LONG)
        assertThat(RemoteImageBitmap.createForId(7).valueKind)
            .isEqualTo(RemoteValueKind.IMAGE_BITMAP)
        assertThat(RemoteFloatArray(listOf(1f.rf)).valueKind).isEqualTo(RemoteValueKind.FLOAT_ARRAY)
        assertThat(RemoteMutableFloatArray(2).valueKind).isEqualTo(RemoteValueKind.FLOAT_ARRAY)
        assertThat(RemoteIntArray(listOf(1.ri)).valueKind).isEqualTo(RemoteValueKind.INT_ARRAY)
        assertThat(RemoteStringArray(listOf("a".rs)).valueKind)
            .isEqualTo(RemoteValueKind.STRING_ARRAY)
        assertThat(RemoteMatrix3x3.createIdentity().valueKind).isEqualTo(RemoteValueKind.MATRIX)
        assertThat(RemoteEnum(SampleEnum.A).valueKind).isEqualTo(RemoteValueKind.ENUM)

        assertThat(RemoteValueKind.INT.isIntEncoded).isTrue()
        assertThat(RemoteValueKind.BOOLEAN.isIntEncoded).isTrue()
        assertThat(RemoteValueKind.LONG.isIntEncoded).isFalse()
        assertThat(RemoteValueKind.FLOAT.isIntEncoded).isFalse()

        assertThat(RemoteValueKind.INT.isIntegral).isTrue()
        assertThat(RemoteValueKind.BOOLEAN.isIntegral).isTrue()
        assertThat(RemoteValueKind.LONG.isIntegral).isTrue()
        assertThat(RemoteValueKind.FLOAT.isIntegral).isFalse()
        assertThat(RemoteValueKind.STRING.isIntegral).isFalse()
    }

    @Test
    fun namedCacheKeyWireName_formatsAllDomains() {
        assertThat(RemoteNamedCacheKey(RemoteValue.Domain.User, "count").wireName)
            .isEqualTo("USER:count")
        assertThat(RemoteNamedCacheKey(RemoteValue.Domain.System, "theme").wireName)
            .isEqualTo("SYSTEM:theme")
        assertThat(RemoteNamedCacheKey(RemoteValue.Domain.None, "raw").wireName).isEqualTo("raw")
        assertThat(RemoteNamedCacheKey(RemoteValue.Domain("HOME"), "temp").wireName)
            .isEqualTo("HOME:temp")
    }

    @Test
    fun getValueKind_classifiesAttachedDetachedAndCallbackKeys() {
        val intSum = n + 2.ri
        val intToFloat = intSum.toRemoteFloat()
        val intToString = intSum.toRemoteString()
        val floatToInt = x.toRemoteInt()
        val cmpLt = x.isLessThan(5f.rf)
        val strLen = s.length
        val strIsEmpty = s.isEmpty
        val boolSelInt = b.select(1.ri, 2.ri)
        val boolSelFloat = b.select(1f.rf, 2f.rf)
        val b2 = RemoteBoolean.createNamedRemoteBoolean("b2", false)
        val boolSelBool = b.select(b2, false.rb)
        val intArrGet = RemoteIntArray(listOf(1.ri, 2.ri))[n]

        // Attached live keys and constants
        assertThat(RemoteConstantCacheKey(5).getValueKind().isIntEncoded).isTrue()
        assertThat(RemoteConstantCacheKey(5.toShort()).getValueKind().isIntEncoded).isTrue()
        assertThat(RemoteConstantCacheKey(5.toByte()).getValueKind().isIntEncoded).isTrue()
        assertThat(RemoteConstantCacheKey(5L).getValueKind().isIntEncoded).isFalse()
        assertThat(RemoteLong(5L).cacheKey.getValueKind().isIntEncoded).isFalse()
        assertThat(RemoteConstantCacheKey(5f).getValueKind().isIntEncoded).isFalse()
        assertThat(intSum.cacheKey.getValueKind().isIntEncoded).isTrue()
        assertThat(intToFloat.cacheKey.getValueKind().isIntEncoded).isFalse()
        assertThat(intToString.cacheKey.getValueKind().isIntEncoded).isFalse()
        assertThat(floatToInt.cacheKey.getValueKind().isIntEncoded).isTrue()
        assertThat(cmpLt.cacheKey.getValueKind().isIntEncoded).isTrue()
        assertThat(strLen.cacheKey.getValueKind().isIntEncoded).isTrue()
        assertThat(strIsEmpty.cacheKey.getValueKind().isIntEncoded).isTrue()
        assertThat(boolSelInt.cacheKey.getValueKind().isIntEncoded).isTrue()
        assertThat(boolSelFloat.cacheKey.getValueKind().isIntEncoded).isFalse()
        assertThat(boolSelBool.cacheKey.getValueKind().isIntEncoded).isTrue()
        assertThat(intArrGet.cacheKey.getValueKind().isIntEncoded).isTrue()

        // Detached operation keys (getState() == null) exercise RemoteOperation.resultKind
        fun detached(key: RemoteStateCacheKey): RemoteOperationCacheKey =
            RemoteOperationCacheKey.create((key as RemoteOperationCacheKey).op)

        assertThat(detached(intSum.cacheKey).getValueKind()).isEqualTo(RemoteValueKind.INT)
        assertThat(detached(intToFloat.cacheKey).getValueKind()).isEqualTo(RemoteValueKind.FLOAT)
        assertThat(detached(intToString.cacheKey).getValueKind()).isEqualTo(RemoteValueKind.STRING)
        assertThat(detached(floatToInt.cacheKey).getValueKind()).isEqualTo(RemoteValueKind.INT)
        assertThat(detached(cmpLt.cacheKey).getValueKind()).isEqualTo(RemoteValueKind.BOOLEAN)
        assertThat(cmpLt.cacheKey.getValueKind()).isEqualTo(RemoteValueKind.BOOLEAN)
        assertThat(detached(strLen.cacheKey).getValueKind()).isEqualTo(RemoteValueKind.INT)
        assertThat(detached(strIsEmpty.cacheKey).getValueKind()).isEqualTo(RemoteValueKind.BOOLEAN)
        assertThat(detached(boolSelInt.cacheKey).getValueKind()).isEqualTo(RemoteValueKind.INT)
        assertThat(detached(boolSelFloat.cacheKey).getValueKind()).isEqualTo(RemoteValueKind.FLOAT)
        assertThat(detached(boolSelBool.cacheKey).getValueKind()).isEqualTo(RemoteValueKind.BOOLEAN)
        assertThat(detached(intArrGet.cacheKey).getValueKind()).isEqualTo(RemoteValueKind.INT)
        assertThat(detached(c.red.cacheKey).getValueKind()).isEqualTo(RemoteValueKind.FLOAT)
        assertThat(detached(lerp(c, RemoteColor(0xFF0000FF.toInt()), x).cacheKey).getValueKind())
            .isEqualTo(RemoteValueKind.COLOR)
        assertThat(detached(RemoteDp(x).cacheKey).getValueKind()).isEqualTo(RemoteValueKind.DP)
        assertThat(detached(RemoteTextUnit(x).cacheKey).getValueKind())
            .isEqualTo(RemoteValueKind.TEXT_UNIT)
        assertThat(
                detached((RemoteLong(1L) + RemoteLong.createNamedRemoteLong("l", 2L)).cacheKey)
                    .getValueKind()
            )
            .isEqualTo(RemoteValueKind.LONG)
        assertThat(detached(RemoteMatrix3x3.createRotate(x).cacheKey).getValueKind())
            .isEqualTo(RemoteValueKind.MATRIX)
        assertThat(detached(RemoteImageBitmap.createForId(107).width.cacheKey).getValueKind())
            .isEqualTo(RemoteValueKind.FLOAT)
        assertThat(RemoteColorConstantCacheKey(0xFF112233.toInt()).getValueKind())
            .isEqualTo(RemoteValueKind.COLOR)
        assertThat(RemoteStateArrayKey(4).getValueKind()).isEqualTo(RemoteValueKind.FLOAT_ARRAY)

        // Detached state ID and named keys delegate to caller predicates
        val stateIdKey = RemoteStateIdKey(42)
        assertThat(stateIdKey.getValueKind()).isEqualTo(RemoteValueKind.OTHER)
        assertThat(
                stateIdKey.getValueKind(
                    stateIdKind = { if (it == 42) RemoteValueKind.COLOR else RemoteValueKind.OTHER }
                )
            )
            .isEqualTo(RemoteValueKind.COLOR)
        assertThat(
                stateIdKey
                    .getValueKind(
                        stateIdKind = {
                            if (it == 42) RemoteValueKind.INT else RemoteValueKind.OTHER
                        }
                    )
                    .isIntEncoded
            )
            .isTrue()

        val namedKey = RemoteNamedCacheKey(RemoteValue.Domain.User, "idx")
        assertThat(namedKey.getValueKind()).isEqualTo(RemoteValueKind.OTHER)
        assertThat(
                namedKey.getValueKind(
                    namedKeyKind = {
                        if (it.wireName == "USER:idx") RemoteValueKind.STRING
                        else RemoteValueKind.OTHER
                    }
                )
            )
            .isEqualTo(RemoteValueKind.STRING)
        assertThat(
                namedKey
                    .getValueKind(
                        namedKeyKind = {
                            if (it.wireName == "USER:idx") RemoteValueKind.INT
                            else RemoteValueKind.OTHER
                        }
                    )
                    .isIntEncoded
            )
            .isTrue()
    }

    @Test
    fun textUnitOperationKey_resultKindMatchesRuntimeValueKind() {
        fun detached(key: RemoteStateCacheKey): RemoteOperationCacheKey =
            RemoteOperationCacheKey.create((key as RemoteOperationCacheKey).op)

        // Outer RemoteTextUnit uses TextUnitOperationKey.Sp -> TEXT_UNIT
        val dpToRsp = RemoteDp(x).toRsp()
        val spKey = dpToRsp.cacheKey as RemoteOperationCacheKey
        assertThat(spKey.op.name).isEqualTo("Sp")
        assertThat(dpToRsp.valueKind).isEqualTo(RemoteValueKind.TEXT_UNIT)
        assertThat(spKey.getValueKind()).isEqualTo(dpToRsp.valueKind)
        assertThat(detached(spKey).getValueKind()).isEqualTo(dpToRsp.valueKind)

        // RemoteDp.toRsp() creates an inner RemoteFloat scalar keyed by TextUnitOperationKey.DpToSp
        // -> FLOAT (NOT TEXT_UNIT; RemoteTextUnit wraps it via Sp).
        val dpToSpKey = dpToRsp.value.cacheKey as RemoteOperationCacheKey
        assertThat(dpToSpKey.op.name).isEqualTo("DpToSp")
        assertThat(dpToRsp.value).isInstanceOf(RemoteFloat::class.java)
        assertThat(dpToRsp.value.valueKind).isEqualTo(RemoteValueKind.FLOAT)
        assertThat(dpToSpKey.getValueKind()).isEqualTo(dpToRsp.value.valueKind)
        assertThat(detached(dpToSpKey).getValueKind()).isEqualTo(dpToRsp.value.valueKind)

        // toPx() produces a RemoteFloat keyed by TextUnitOperationKey.ToPx -> FLOAT
        val px = dpToRsp.toPx()
        val toPxKey = px.cacheKey as RemoteOperationCacheKey
        assertThat(toPxKey.op.name).isEqualTo("ToPx")
        assertThat(px.valueKind).isEqualTo(RemoteValueKind.FLOAT)
        assertThat(toPxKey.getValueKind()).isEqualTo(px.valueKind)
        assertThat(detached(toPxKey).getValueKind()).isEqualTo(px.valueKind)
    }

    @Test
    fun requiresConstantForOperandIndexAndCanFailOnOperandIndexMetadata() {
        val toStrOp =
            (x.toRemoteStringOptions(2, 1, 0).cacheKey as RemoteOperationCacheKey).op
                as RemoteOperation
        assertThat(toStrOp.requiresConstantForOperandIndex(0)).isFalse()
        assertThat(toStrOp.requiresConstantForOperandIndex(1)).isTrue()
        assertThat(toStrOp.requiresConstantForOperandIndex(2)).isTrue()
        assertThat(toStrOp.requiresConstantForOperandIndex(3)).isTrue()

        val refOp =
            (x.createReference(forceRemote = true).cacheKey as RemoteOperationCacheKey).op
                as RemoteOperation
        assertThat(refOp.requiresConstantForOperandIndex(0)).isFalse()
        assertThat(refOp.requiresConstantForOperandIndex(1)).isTrue()

        val animOp =
            (animatedRemoteFloat(x, floatArrayOf(0.5f)).cacheKey as RemoteOperationCacheKey).op
                as RemoteOperation
        assertThat(animOp.requiresConstantForOperandIndex(0)).isFalse()
        assertThat(animOp.requiresConstantForOperandIndex(1)).isTrue()

        val ahsvOp =
            (RemoteColor.fromAHSV(200, x, 1f.rf, 1f.rf).cacheKey as RemoteOperationCacheKey).op
                as RemoteOperation
        assertThat(ahsvOp.requiresConstantForOperandIndex(0)).isTrue()
        assertThat(ahsvOp.requiresConstantForOperandIndex(1)).isFalse()

        val compOp = (c.red.cacheKey as RemoteOperationCacheKey).op as RemoteOperation
        assertThat(compOp.requiresConstantForOperandIndex(0)).isFalse()
        assertThat(compOp.requiresConstantForOperandIndex(1)).isTrue()

        val tweenIntOp =
            (lerp(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), x).cacheKey as RemoteOperationCacheKey).op
                as RemoteOperation
        assertThat(tweenIntOp.requiresConstantForOperandIndex(0)).isTrue()
        assertThat(tweenIntOp.requiresConstantForOperandIndex(1)).isTrue()
        assertThat(tweenIntOp.requiresConstantForOperandIndex(2)).isFalse()

        val intDivOp = ((n / 2.ri).cacheKey as RemoteOperationCacheKey).op as RemoteOperation
        assertThat(intDivOp.canFailOnOperandIndex(0)).isFalse()
        assertThat(intDivOp.canFailOnOperandIndex(1)).isTrue()

        val subOp = (s.substring(n, 2.ri).cacheKey as RemoteOperationCacheKey).op as RemoteOperation
        assertThat(subOp.canFailOnOperandIndex(0)).isFalse()
        assertThat(subOp.canFailOnOperandIndex(1)).isTrue()
        assertThat(subOp.canFailOnOperandIndex(2)).isTrue()

        val enumToStrOp =
            (RemoteEnum(n, SampleEnum.entries).toRemoteString().cacheKey as RemoteOperationCacheKey)
                .op as RemoteOperation
        assertThat(enumToStrOp.canFailOnOperandIndex(0)).isTrue()
        assertThat(enumToStrOp.canFailOnOperandIndex(1)).isFalse()

        val floatArrGetOp =
            (RemoteFloatArray(listOf(1f.rf, 2f.rf))[n].cacheKey as RemoteOperationCacheKey).op
                as RemoteOperation
        assertThat(floatArrGetOp.canFailOnOperandIndex(1)).isTrue()
    }

    @Test
    fun determinismAndCanReplaceWithConstant() {
        assertThat((x + 1f.rf).cacheKey.isDeterministic()).isTrue()
        assertThat((x + 1f.rf).cacheKey.canConstantFold()).isTrue()

        assertThat(rand().cacheKey.isDeterministic()).isFalse()
        assertThat(rand().cacheKey.canConstantFold()).isFalse()
        assertThat(randRange(1f.rf, 5f.rf).cacheKey.isDeterministic()).isFalse()

        val animated = animatedRemoteFloat(x, floatArrayOf(0.5f))
        assertThat(animated.cacheKey.isDeterministic()).isFalse()
        assertThat(animated.cacheKey.canConstantFold()).isFalse()

        // forceRemote = true reference is deterministic (x == x holds) but must NOT constant-fold.
        val forcedRef = 7f.rf.createReference(forceRemote = true)
        assertThat(forcedRef.cacheKey.isDeterministic()).isTrue()
        assertThat(forcedRef.cacheKey.canConstantFold()).isFalse()

        assertThat(RemoteStateInstanceKey().canConstantFold()).isFalse()
        assertThat(RemoteStateArrayKey(4).canConstantFold()).isFalse()

        // Unkeyed RemoteFloat lambda is non-deterministic and non-foldable
        val unkeyedLambda = RemoteFloat { 1f.rf }
        assertThat(unkeyedLambda.cacheKey.isDeterministic()).isFalse()
        assertThat(unkeyedLambda.cacheKey.canConstantFold()).isFalse()
        assertThat((unkeyedLambda + 2f.rf).cacheKey.isDeterministic()).isFalse()
        assertThat((unkeyedLambda + 2f.rf).cacheKey.canConstantFold()).isFalse()

        // requireFiniteFloats rejects expressions that may produce Infinity or NaN
        val divByZero = x / 0f.rf
        assertThat(divByZero.cacheKey.isDeterministic()).isTrue()
        assertThat(divByZero.cacheKey.isDeterministic(requireFiniteFloats = true)).isFalse()

        // Deep DAG queries are O(1) via precomputed expressionFlags without recursion depth cliff
        var deepNamed: RemoteStateCacheKey = x.cacheKey
        repeat(2000) {
            deepNamed =
                RemoteOperationCacheKey(
                    RemoteFloatOperationKey.Plus,
                    listOf(deepNamed, RemoteConstantCacheKey(1f)),
                )
        }
        assertThat(deepNamed.isDeterministic()).isTrue()
        assertThat(deepNamed.canConstantFold()).isTrue()
        assertThat(
                (deepNamed as BaseRemoteStateCacheKey).canConstantFold(
                    maxDepth = 1,
                    canFoldStateId = { false },
                )
            )
            .isTrue()

        // State-ID predicate checks traverse branches containing RemoteStateIdKey up to maxDepth
        val idFloat = RemoteFloat.createForId(Utils.asNan(42))
        val deepWithId = sin(cos(abs(idFloat)))
        val deepWithIdKey = deepWithId.cacheKey as BaseRemoteStateCacheKey
        assertThat(deepWithIdKey.canConstantFold()).isTrue()
        assertThat(deepWithIdKey.canConstantFold(maxDepth = 1, canFoldStateId = { it == 42 }))
            .isFalse()
        assertThat(deepWithIdKey.canConstantFold(maxDepth = 4, canFoldStateId = { it == 42 }))
            .isTrue()
        assertThat(deepWithIdKey.canConstantFold(maxDepth = 4, canFoldStateId = { it < 42 }))
            .isFalse()

        val stateIdKey = RemoteStateIdKey(42)
        assertThat(stateIdKey.canConstantFold(canFoldStateId = { it < 42 })).isFalse()
        assertThat(stateIdKey.canConstantFold(canFoldStateId = { it == 42 })).isTrue()

        // Shared diamond DAG visits each reachable node at most once (O(N), not O(2^depth))
        var diamond: RemoteStateCacheKey = idFloat.cacheKey
        repeat(16) {
            diamond =
                RemoteOperationCacheKey(RemoteFloatOperationKey.Plus, listOf(diamond, diamond))
        }
        var stateIdChecks = 0
        assertThat(
                (diamond as BaseRemoteStateCacheKey).canConstantFold(
                    maxDepth = 16,
                    canFoldStateId = {
                        stateIdChecks++
                        it == 42
                    },
                )
            )
            .isTrue()
        assertThat(stateIdChecks).isEqualTo(1)
    }

    @Test
    fun getOrCreateValueOrNull_handlesConstantsIdsAndUnsupportedValues() {
        assertThat(RemoteConstantCacheKey(3.5f).getOrCreateValueOrNull()?.constantValueOrNull)
            .isEqualTo(3.5f)
        val byteVal = RemoteConstantCacheKey(5.toByte()).getOrCreateValueOrNull()
        assertThat(byteVal).isInstanceOf(RemoteInt::class.java)
        assertThat(byteVal?.constantValueOrNull).isEqualTo(5)
        val shortVal = RemoteConstantCacheKey(12.toShort()).getOrCreateValueOrNull()
        assertThat(shortVal).isInstanceOf(RemoteInt::class.java)
        assertThat(shortVal?.constantValueOrNull).isEqualTo(12)
        assertThat(RemoteConstantCacheKey(SampleEnum.A).getOrCreateValueOrNull()).isNull()
        assertThat(
                RemoteColorConstantCacheKey(0xFF112233.toInt())
                    .getOrCreateValueOrNull()
                    ?.constantValueOrNull
            )
            .isEqualTo(0xFF112233.toInt())
        assertThat(RemoteStateIdKey(42).getOrCreateValueOrNull())
            .isInstanceOf(RemoteFloat::class.java)
        assertThat(
                RemoteStateIdKey(42).getOrCreateValueOrNull {
                    RemoteInt.createForId(0x100000000L + it)
                }
            )
            .isInstanceOf(RemoteInt::class.java)
        assertThat(RemoteStateIdKey(42).getOrCreateValueOrNull { null }).isNull()
    }

    private enum class UnsupportedReconstructOp : RemoteOperation {
        DefaultOnly;

        override fun toDebugString(args: List<RemoteStateCacheKey>): String = "defaultOnly()"
    }

    @Test
    fun reconstructOrNull_handlesBooleanUnwrappingConstantBoolArgsAndErrors() {
        val addKey = (x + 1f.rf).cacheKey as RemoteOperationCacheKey
        assertThat(addKey.reconstructOrNull(listOf(2f.rf, 3f.rf))?.constantValueOrNull)
            .isEqualTo(5f)
        assertThat(addKey.reconstructOrNull(listOf("bad".rs, "args".rs))).isNull()
        assertThat(addKey.reconstructOrNull(emptyList())).isNull()

        val enumToStrKey =
            RemoteEnum(n, SampleEnum.entries).toRemoteString().cacheKey as RemoteOperationCacheKey
        assertThat(
                enumToStrKey.reconstructOrNull(listOf("bad".rs, RemoteStringArray(listOf("A".rs))))
            )
            .isNull()

        // First-class RemoteBoolean and RemoteEnum operations reconstruct directly
        val selectKey = b.select(10f.rf, 20f.rf).cacheKey as RemoteOperationCacheKey
        val selected = selectKey.reconstructOrNull(listOf(RemoteBoolean(true), 10f.rf, 20f.rf))
        assertThat(selected?.constantValueOrNull).isEqualTo(10f)
        val andKey = (b and false.rb).cacheKey as RemoteOperationCacheKey
        assertThat(andKey.reconstructOrNull(listOf(true.rb, true.rb))?.constantValueOrNull)
            .isEqualTo(true)
        val toIntKey = b.toRemoteInt().cacheKey as RemoteOperationCacheKey
        assertThat(toIntKey.reconstructOrNull(listOf(true.rb))?.constantValueOrNull).isEqualTo(1)
        val wrappedEnumKey =
            RemoteEnum(n + 1.ri, SampleEnum.entries).cacheKey as RemoteOperationCacheKey
        assertThat(wrappedEnumKey.reconstructOrNull(listOf(1.ri))?.constantValueOrNull)
            .isEqualTo(SampleEnum.B)

        // Constant-required RemoteBoolean operand (Reference forceRemote) stays a literal Boolean
        val refKey = x.createReference(forceRemote = true).cacheKey as RemoteOperationCacheKey
        val reconstructedRef = refKey.reconstructOrNull(listOf(x, RemoteBoolean(true)))
        assertThat(reconstructedRef).isInstanceOf(RemoteFloat::class.java)
        assertThat(reconstructedRef!!.cacheKey.canConstantFold()).isFalse()

        // Runtime exceptions (e.g., UnsupportedOperationException or IndexOutOfBoundsException)
        // return null
        val unsupportedKey = RemoteOperationCacheKey.create(UnsupportedReconstructOp.DefaultOnly)
        assertThat(unsupportedKey.reconstructOrNull(emptyList())).isNull()
        val subKey = s.substring(n, 2.ri).cacheKey as RemoteOperationCacheKey
        assertThat(subKey.reconstructOrNull(listOf("hi".rs, 1.ri, 99.ri))).isNull()

        // adaptToType wraps reconstructed RemoteInt comparison result back into RemoteBoolean
        val cmp = x.isLessThan(5f.rf)
        val cmpKey = cmp.cacheKey as RemoteOperationCacheKey
        val reconstructedBool = cmp.adaptToType(cmpKey.reconstructOrNull(listOf(1f.rf, 5f.rf))!!)
        assertThat(reconstructedBool).isInstanceOf(RemoteBoolean::class.java)
        assertThat(reconstructedBool.constantValueOrNull).isEqualTo(true)
    }

    @Test
    fun createForIdOrNull_synthesizesMatchingIdBackedValues() {
        val id = 123
        assertThat(x.createForIdOrNull(id)).isInstanceOf(RemoteFloat::class.java)
        assertThat(x.createForIdOrNull(id)?.cacheKey).isEqualTo(RemoteStateIdKey(id))
        assertThat(n.createForIdOrNull(id)).isInstanceOf(RemoteInt::class.java)
        assertThat(n.createForIdOrNull(id)?.cacheKey).isEqualTo(RemoteStateIdKey(id))
        assertThat(b.createForIdOrNull(id)).isInstanceOf(RemoteBoolean::class.java)
        assertThat(b.createForIdOrNull(id)?.cacheKey).isEqualTo(RemoteStateIdKey(id))
        assertThat(s.createForIdOrNull(id)).isInstanceOf(RemoteString::class.java)
        assertThat(s.createForIdOrNull(id)?.cacheKey).isEqualTo(RemoteStateIdKey(id))
        assertThat(c.createForIdOrNull(id)).isInstanceOf(RemoteColor::class.java)
        assertThat(c.createForIdOrNull(id)?.cacheKey).isEqualTo(RemoteStateIdKey(id))
        assertThat(RemoteDp(1f.rf).createForIdOrNull(id)).isInstanceOf(RemoteDp::class.java)
        assertThat(RemoteLong(1L).createForIdOrNull(id)).isInstanceOf(RemoteLong::class.java)
        assertThat(RemoteImageBitmap.createForId(1).createForIdOrNull(id))
            .isInstanceOf(RemoteImageBitmap::class.java)
        assertThat(RemoteFloatArray(listOf(1f.rf)).createForIdOrNull(id))
            .isInstanceOf(RemoteFloatArray::class.java)
        assertThat(RemoteIntArray(listOf(1.ri)).createForIdOrNull(id))
            .isInstanceOf(RemoteIntArray::class.java)
        assertThat(RemoteStringArray(listOf("a".rs)).createForIdOrNull(id))
            .isInstanceOf(RemoteStringArray::class.java)
        assertThat(RemoteEnum(SampleEnum.A).createForIdOrNull(id))
            .isInstanceOf(RemoteEnum::class.java)
        assertThat(RemoteMatrix3x3.createIdentity().createForIdOrNull(id)).isNull()
    }

    @Test
    fun fromRemoteIntAndAdaptToType() {
        assertThat(RemoteBoolean.fromRemoteInt(1.ri).constantValueOrNull).isTrue()
        assertThat(RemoteBoolean.fromRemoteInt(0.ri).constantValueOrNull).isFalse()
        assertThat(RemoteBoolean.fromRemoteInt(n).constantValueOrNull).isNull()

        assertThat(x.adaptToType(5.ri)).isInstanceOf(RemoteFloat::class.java)
        assertThat(n.adaptToType(5f.rf)).isInstanceOf(RemoteInt::class.java)
        assertThat(b.adaptToType(1.ri).constantValueOrNull).isEqualTo(true)
        assertThat(b.adaptToType(0.5f.rf).constantValueOrNull).isEqualTo(false)
        assertThat(b.adaptToType(1.5f.rf).constantValueOrNull).isEqualTo(true)

        val mutableFloat = MutableRemoteFloat(1f)
        assertThat(mutableFloat.adaptToType(2f.rf)).isSameInstanceAs(mutableFloat)
        val otherMutableFloat = MutableRemoteFloat(2f)
        assertThat(mutableFloat.adaptToType(otherMutableFloat)).isSameInstanceAs(otherMutableFloat)

        // RemoteLong matches across different concrete implementations (RemoteLongImpl vs
        // MutableRemoteLongImpl), while MutableRemoteLong rejects immutable RemoteLong
        val immutableLong = RemoteLong(1L)
        val mutableLong = MutableRemoteLong(2L)
        assertThat(immutableLong.adaptToType(mutableLong)).isSameInstanceAs(mutableLong)
        assertThat(mutableLong.adaptToType(immutableLong)).isSameInstanceAs(mutableLong)
        val otherMutableLong = MutableRemoteLong(3L)
        assertThat(mutableLong.adaptToType(otherMutableLong)).isSameInstanceAs(otherMutableLong)

        // RemoteEnum matches same enumEntries, rejects different enum types, and adapts valid
        // RemoteInt ordinals while rejecting out-of-range constant ordinals
        val enumA = RemoteEnum(SampleEnum.A)
        val enumB = RemoteEnum(SampleEnum.B)
        val otherEnum = RemoteEnum(OtherEnum.X)
        assertThat(enumA.adaptToType(enumB)).isSameInstanceAs(enumB)
        assertThat(enumA.adaptToType(otherEnum)).isSameInstanceAs(enumA)
        val adaptedEnum = enumA.adaptToType(1.ri)
        assertThat(adaptedEnum).isInstanceOf(RemoteEnum::class.java)
        assertThat(adaptedEnum.constantValueOrNull).isEqualTo(SampleEnum.B)
        assertThat(enumA.adaptToType(99.ri)).isSameInstanceAs(enumA)
        assertThat(enumA.adaptToType((-1).ri)).isSameInstanceAs(enumA)

        val mutableEnumA = MutableRemoteEnum(SampleEnum.A)
        val mutableEnumB = MutableRemoteEnum(SampleEnum.B)
        val mutableOtherEnum = MutableRemoteEnum(OtherEnum.X)
        assertThat(mutableEnumA.adaptToType(enumB)).isSameInstanceAs(mutableEnumA)
        assertThat(mutableEnumA.adaptToType(mutableEnumB)).isSameInstanceAs(mutableEnumB)
        assertThat(mutableEnumA.adaptToType(mutableOtherEnum)).isSameInstanceAs(mutableEnumA)
    }
}
