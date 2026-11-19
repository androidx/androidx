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

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.state.RemoteDp.Companion.createNamedRemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteDpTest {
    private val density = 2f
    private val context = TestRemoteContext()
    private lateinit var creationState: RemoteComposeCreationState

    @Before
    fun setUp() {
        context.density = density
        creationState = testCreationState(remoteDensity = RemoteDensity(density.rf, 1f.rf))
    }

    private fun <T> initialise(block: (RemoteComposeCreationState) -> T): T {
        val result = block(creationState)
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            initializeContext(context)
            context.loadFloat(RemoteContext.ID_DENSITY, this@RemoteDpTest.density)
            for (op in operations) {
                if (op is VariableSupport) {
                    op.updateVariables(context)
                }
                op.apply(context)
            }
        }
        return result
    }

    @Test
    fun constructor_createsCorrectly() {
        val floatValue = 10.5f
        val remoteFloat = RemoteFloat(floatValue)
        val remoteFloatDp = RemoteDp(remoteFloat)

        assertThat(remoteFloatDp.value).isEqualTo(remoteFloat)
        assertThat(remoteFloatDp.constantValue).isEqualTo(floatValue)
    }

    @Test
    fun constructor_extensionFunction() {
        val intValue = 10
        val rdpValue = intValue.rdp
        assertThat(rdpValue.constantValue).isEqualTo(intValue)
    }

    @Test
    fun newInstance_hasSameFloatValueAsOriginalRemoteFloat() {
        val floatValue = 10.5f

        val (resultId, resultDpId) =
            initialise {
                val remoteFloat = RemoteFloat(floatValue)
                val remoteFloatDp = RemoteDp(remoteFloat)
                val resultId = it.getId(remoteFloat)
                val resultDpId = it.getId(remoteFloatDp.value)
                Pair(resultId, resultDpId)
            }
        assertThat(context.getFloat(resultId)).isEqualTo(floatValue)
        assertThat(context.getFloat(resultDpId)).isEqualTo(floatValue)
    }

    @Test
    fun newInstance_hasSameIdFromOriginalRemoteFloat() {
        val floatValue = 10.5f

        val (resultId, resultDpId) =
            initialise {
                val remoteFloat = RemoteFloat(floatValue)
                val remoteFloatDp = RemoteDp(remoteFloat)

                val resultId = it.getId(remoteFloat)
                val resultDpId = it.getId(remoteFloatDp.value)
                Pair(resultId, resultDpId)
            }

        assertThat(resultId).isEqualTo(resultDpId)
    }

    @Test
    fun toPx_hasDifferentFloatValueAsOriginalRemoteFloat() {
        val floatValue = 10.5f

        val (resultDpId, resultPxId, resultPxId2) =
            initialise {
                val remoteFloatDp = RemoteDp(floatValue.rf)
                val remoteFloatPx = remoteFloatDp.toPx(it.remoteDensity)
                val remoteFloatPx2 = remoteFloatDp.toPx()

                val resultDpId = it.getId(remoteFloatDp.value)
                val resultPxId = it.getId(remoteFloatPx)
                val resultPxId2 = it.getId(remoteFloatPx2)
                Triple(resultDpId, resultPxId, resultPxId2)
            }

        assertThat(context.getFloat(resultDpId)).isEqualTo(floatValue)
        assertThat(context.getFloat(resultPxId)).isEqualTo(floatValue * density)
        assertThat(context.getFloat(resultPxId2)).isEqualTo(floatValue * density)
    }

    @Test
    fun toRemoteDp_hasDifferentFloatValueAsOriginalRemoteFloat() {
        val pxValue = 20f

        val (resultPxId, resultDpId) =
            initialise {
                val remoteFloatPx = pxValue.rf
                val remoteFloatDp = remoteFloatPx.toRemoteDp()

                val resultPxId = it.getId(remoteFloatPx)
                val resultDpId = it.getId(remoteFloatDp.value)
                Pair(resultPxId, resultDpId)
            }

        assertThat(context.getFloat(resultPxId)).isEqualTo(pxValue)
        assertThat(context.getFloat(resultDpId)).isEqualTo(pxValue / density)
    }

    @Test
    fun remoteConfiguration_dpVariants_divideByHostDensity() {
        val (widthDpId, heightDpId, fontSizeDpId) =
            initialise {
                Triple(
                    it.getId(RemoteConfiguration.windowWidth.value),
                    it.getId(RemoteConfiguration.windowHeight.value),
                    it.getId(RemoteConfiguration.fontSize.value),
                )
            }

        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            initializeContext(context)
            context.loadFloat(RemoteContext.ID_DENSITY, 3f)
            context.overrideFloat(RemoteContext.ID_WINDOW_WIDTH, 600f)
            context.overrideFloat(RemoteContext.ID_WINDOW_HEIGHT, 900f)
            context.loadFloat(RemoteContext.ID_FONT_SIZE, 42f)
            for (op in operations) {
                if (op is VariableSupport) {
                    op.updateVariables(context)
                }
                op.apply(context)
            }
        }

        assertThat(context.getFloat(widthDpId)).isEqualTo(200f)
        assertThat(context.getFloat(heightDpId)).isEqualTo(300f)
        assertThat(context.getFloat(fontSizeDpId)).isEqualTo(14f)
    }

    @Ignore("Fails because toRemoteDp creates non-constant expression")
    @Test
    fun toRemoteDp_resolvesAsConstant_whenValueAndDensityAreConstants() {
        val pxValue = 20f

        val resultDpId = initialise {
            val remoteFloatPx = pxValue.rf
            val remoteFloatDp = remoteFloatPx.toRemoteDp()
            it.getFloatId(remoteFloatDp)
        }

        assertThat(resultDpId).isEqualTo(pxValue)
    }

    @Test
    fun toPx_remoteFloatHasDifferentIdFromOriginal() {
        val floatValue = 10.5f

        val (resultDpId, resultPxId) =
            initialise {
                val remoteFloatDp = RemoteDp(floatValue.rf)
                val remoteFloatPx = remoteFloatDp.toPx(it.remoteDensity)

                val resultDpId = it.getId(remoteFloatDp.value)
                val resultPxId = it.getId(remoteFloatPx)
                Pair(resultDpId, resultPxId)
            }

        assertThat(resultDpId).isNotEqualTo(resultPxId)
    }

    @Test
    fun remoteDp_cacheKey() {
        val dp1 = 10.rdp
        val dp2 = 10.rdp
        assertThat(dp1.cacheKey).isNotNull()
        assertThat(dp1.cacheKey).isEqualTo(dp2.cacheKey)
    }

    @Test
    fun cacheKeyDoesNotCollideWithRemoteFloat_constant_floatFirst() {
        val remoteFloat = 10f.rf
        val remoteDp = remoteFloat.asRemoteDp()

        assertThat(remoteDp.cacheKey).isNotEqualTo(remoteFloat.cacheKey)
        assertThat(remoteDp).isNotEqualTo(remoteFloat)

        val (floatId, dpPxId) =
            initialise {
                val fId = it.getId(remoteFloat)
                val dId = it.getId(remoteDp)
                Pair(fId, dId)
            }

        assertThat(dpPxId).isNotEqualTo(floatId)
        assertThat(context.getFloat(floatId)).isEqualTo(10f)
        assertThat(context.getFloat(dpPxId)).isEqualTo(10f * density)
    }

    @Test
    fun cacheKeyDoesNotCollideWithRemoteFloat_constant_dpFirst() {
        val remoteDp = 10.rdp
        val remoteFloat = 10f.rf

        val (dpPxId, floatId) =
            initialise {
                val dId = it.getId(remoteDp)
                val fId = it.getId(remoteFloat)
                Pair(dId, fId)
            }

        assertThat(dpPxId).isNotEqualTo(floatId)
        assertThat(context.getFloat(dpPxId)).isEqualTo(10f * density)
        assertThat(context.getFloat(floatId)).isEqualTo(10f)
    }

    @Test
    fun cacheKeyDoesNotCollideWithRemoteFloat_namedVariable() {
        val remoteFloat = createNamedRemoteFloat("val", 10f)
        val remoteDp = remoteFloat.asRemoteDp()

        assertThat(remoteDp.cacheKey).isNotEqualTo(remoteFloat.cacheKey)

        val (floatId, dpPxId) =
            initialise {
                val fId = it.getId(remoteFloat)
                val dId = it.getId(remoteDp)
                Pair(fId, dId)
            }

        assertThat(dpPxId).isNotEqualTo(floatId)
        assertThat(context.getFloat(floatId)).isEqualTo(10f)
        assertThat(context.getFloat(dpPxId)).isEqualTo(10f * density)
    }

    @Test
    fun transform_identity_preservesRemoteDpAndInnerFloat() {
        val remoteFloat = createNamedRemoteFloat("val", 10f)
        val expr = RemoteDp(remoteFloat * 2f)

        val transformed = expr.transform { null }
        assertThat(transformed).isEqualTo(expr)

        val (floatId, dpPxId) =
            initialise {
                val fId = it.getId(remoteFloat)
                val dId = it.getId(transformed)
                Pair(fId, dId)
            }

        assertThat(context.getFloat(floatId)).isEqualTo(10f)
        assertThat(context.getFloat(dpPxId)).isEqualTo(20f * density)
    }

    @Test
    fun toString_constant() {
        val dp = 10.5f.rdp
        assertThat(dp.toString()).isEqualTo("10.5.dp")
    }

    @Test
    fun toString_toDp() {
        val dpExpr = createNamedRemoteFloat("pxVal", 0f).toRemoteDp()
        assertThat(dpExpr.toString()).isEqualTo("user:pxVal.toDp()")
    }

    @Test
    fun toString_contextVariable() {
        val continuousSecFloat = RemoteTimeVariables.continuousSeconds
        val dpExpr = continuousSecFloat.toRemoteDp()
        assertThat(dpExpr.toString()).isEqualTo("context:continuous_sec.toDp()")
    }

    @Test
    fun plus_constant() {
        val resultId = initialise {
            val dp1 = 10.rdp
            val dp2 = 5.rdp
            val result = dp1 + dp2
            it.getId(result.value)
        }
        assertThat(context.getFloat(resultId)).isEqualTo(15f)
    }

    @Test
    fun plus_variable() {
        val resultId = initialise {
            val dp1 = createNamedRemoteDp("dp1", 10f)
            val dp2 = createNamedRemoteDp("dp2", 5f)
            val result = dp1 + dp2
            it.getId(result.value)
        }
        assertThat(context.getFloat(resultId)).isEqualTo(15f)
    }

    @Test
    fun minus_variable() {
        val resultId = initialise {
            val dp1 = createNamedRemoteDp("dp1", 10f)
            val dp2 = createNamedRemoteDp("dp2", 5f)
            val result = dp1 - dp2
            it.getId(result.value)
        }
        assertThat(context.getFloat(resultId)).isEqualTo(5f)
    }

    @Test
    fun unaryMinus_variable() {
        val resultId = initialise {
            val dp1 = createNamedRemoteDp("dp1", 10f)
            val result = -dp1
            it.getId(result.value)
        }
        assertThat(context.getFloat(resultId)).isEqualTo(-10f)
    }

    @Test
    fun div_float_variable() {
        val resultId = initialise {
            val dp1 = createNamedRemoteDp("dp1", 10f)
            val result = dp1 / 2f
            it.getId(result.value)
        }
        assertThat(context.getFloat(resultId)).isEqualTo(5f)
    }

    @Test
    fun div_remoteDp_variable() {
        val resultId = initialise {
            val dp1 = createNamedRemoteDp("dp1", 10f)
            val dp2 = createNamedRemoteDp("dp2", 2f)
            val result = dp1 / dp2
            it.getId(result)
        }
        assertThat(context.getFloat(resultId)).isEqualTo(5f)
    }

    @Test
    fun times_float_variable() {
        val resultId = initialise {
            val dp1 = createNamedRemoteDp("dp1", 10f)
            val result = dp1 * 2f
            it.getId(result.value)
        }
        assertThat(context.getFloat(resultId)).isEqualTo(20f)
    }

    @Test
    fun min_remoteDp_variable() {
        val resultId = initialise {
            val dp1 = createNamedRemoteDp("dp1", 10f)
            val dp2 = createNamedRemoteDp("dp2", 5f)
            val result = min(dp1, dp2)
            it.getId(result.value)
        }
        assertThat(context.getFloat(resultId)).isEqualTo(5f)
    }

    @Test
    fun max_remoteDp_variable() {
        val resultId = initialise {
            val dp1 = createNamedRemoteDp("dp1", 10f)
            val dp2 = createNamedRemoteDp("dp2", 5f)
            val result = max(dp1, dp2)
            it.getId(result.value)
        }
        assertThat(context.getFloat(resultId)).isEqualTo(10f)
    }

    @Test
    fun compare_isLessThan() {
        val resultId = initialise {
            val dp1 = createNamedRemoteDp("dp1", 5f)
            val dp2 = createNamedRemoteDp("dp2", 10f)
            val result = dp1.isLessThan(dp2)
            it.getId(result)
        }
        assertThat(context.getInteger(resultId)).isEqualTo(1)
    }

    @Test
    fun compare_isLessThan_false() {
        val resultId = initialise {
            val dp1 = createNamedRemoteDp("dp1", 10f)
            val dp2 = createNamedRemoteDp("dp2", 5f)
            val result = dp1.isLessThan(dp2)
            it.getId(result)
        }
        assertThat(context.getInteger(resultId)).isEqualTo(0)
    }

    @Test
    fun compare_isLessThanOrEqualTo() {
        val resultId = initialise {
            val dp1 = createNamedRemoteDp("dp1", 5f)
            val dp2 = createNamedRemoteDp("dp2", 5f)
            val result = dp1.isLessThanOrEqualTo(dp2)
            it.getId(result)
        }
        assertThat(context.getInteger(resultId)).isEqualTo(1)
    }

    @Test
    fun compare_isGreaterThan() {
        val resultId = initialise {
            val dp1 = createNamedRemoteDp("dp1", 10f)
            val dp2 = createNamedRemoteDp("dp2", 5f)
            val result = dp1.isGreaterThan(dp2)
            it.getId(result)
        }
        assertThat(context.getInteger(resultId)).isEqualTo(1)
    }

    @Test
    fun compare_isGreaterThanOrEqualTo() {
        val resultId = initialise {
            val dp1 = createNamedRemoteDp("dp1", 10f)
            val dp2 = createNamedRemoteDp("dp2", 10f)
            val result = dp1.isGreaterThanOrEqualTo(dp2)
            it.getId(result)
        }
        assertThat(context.getInteger(resultId)).isEqualTo(1)
    }

    @Test
    fun compare_isEqualTo() {
        val resultId = initialise {
            val dp1 = createNamedRemoteDp("dp1", 10f)
            val dp2 = createNamedRemoteDp("dp2", 10f)
            val result = dp1.isEqualTo(dp2)
            it.getId(result)
        }
        assertThat(context.getInteger(resultId)).isEqualTo(1)
    }

    @Test
    fun compare_isNotEqualTo() {
        val resultId = initialise {
            val dp1 = createNamedRemoteDp("dp1", 10f)
            val dp2 = createNamedRemoteDp("dp2", 5f)
            val result = dp1.isNotEqualTo(dp2)
            it.getId(result)
        }
        assertThat(context.getInteger(resultId)).isEqualTo(1)
    }

    @Test
    fun createNamedRemoteDp_withLambda_hasSameValueAsLiteral() {
        val dpValue = 48.rdp

        val (literalDpId, namedDpId, namedDpFromFloatId, namedPxId) =
            initialise {
                val namedDp = createNamedRemoteDp("testDp") { dpValue }
                val namedDpFromFloat = createNamedRemoteDp("testDp2", 48f)
                val namedPx = namedDp.toPx()

                val literalDpId = it.getId(dpValue.value)
                val namedDpId = it.getId(namedDp.value)
                val namedDpFromFloatId = it.getId(namedDpFromFloat.value)
                val namedPxId = it.getId(namedPx)
                listOf(literalDpId, namedDpId, namedDpFromFloatId, namedPxId)
            }

        assertThat(context.getFloat(literalDpId)).isEqualTo(48f)
        assertThat(context.getFloat(namedDpId)).isEqualTo(48f)
        assertThat(context.getFloat(namedDpFromFloatId)).isEqualTo(48f)
        assertThat(context.getFloat(namedPxId)).isEqualTo(48f * density)
    }

    @Test
    fun createNamedRemoteDp_withLambda_underHostDensity_retainsDpValue() {
        val (namedDpId, namedPxId) =
            initialise { creationState ->
                val namedDp = createNamedRemoteDp("hostDp") { 48.rdp }
                val namedPx = namedDp.toPx(RemoteDensity.Host)
                val namedDpId = creationState.getId(namedDp.value)
                val namedPxId = creationState.getId(namedPx)
                Pair(namedDpId, namedPxId)
            }

        assertThat(context.getFloat(namedDpId)).isEqualTo(48f)
        assertThat(context.getFloat(namedPxId)).isEqualTo(48f * density)
    }

    @Test
    fun createNamedRemoteDp_withLambda_dynamicExpression() {
        val resultId = initialise {
            val base = createNamedRemoteDp("base", 20f)
            val derived = createNamedRemoteDp("derived") { base + 10.rdp }
            it.getId(derived.value)
        }

        assertThat(context.getFloat(resultId)).isEqualTo(30f)
    }
}
