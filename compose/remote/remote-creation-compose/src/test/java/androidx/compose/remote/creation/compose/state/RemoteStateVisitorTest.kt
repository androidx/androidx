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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteStateVisitorTest {

    private fun <S : BaseRemoteState<*>> S.replaceNamedVariables(
        replacements: Map<out BaseRemoteState<*>, BaseRemoteState<*>>
    ): S {
        if (replacements.isEmpty()) return this

        val unwrappedReplacements =
            replacements.map { (target, rep) ->
                val unwrappedTarget =
                    when (target) {
                        is RemoteBoolean -> target.intValue
                        is RemoteDp -> target.value
                        is RemoteEnum<*> -> target.intValue
                        else -> target
                    }
                val unwrappedRep =
                    when (rep) {
                        is RemoteBoolean -> rep.intValue
                        is RemoteDp -> rep.value
                        is RemoteEnum<*> -> rep.intValue
                        else -> rep
                    }
                unwrappedTarget to unwrappedRep
            }

        return transform { node ->
            for ((target, replacement) in unwrappedReplacements) {
                if (node.isStructurallyEqual(target)) {
                    return@transform replacement
                }
            }
            null
        }
    }

    @Test
    fun testPureInteractiveAlpha_evaluatesToConstantsInBothModes() {
        val isInteractive = RemoteBoolean.createNamedRemoteBoolean("interactive_state", true)
        // Alpha: 255 in interactive, 0 in ambient
        val alpha: RemoteFloat = isInteractive.select(255f.rf, 0f.rf)

        // 1. Ambient mode: interactive_state = false
        val ambientAlpha = alpha.replaceNamedVariables(mapOf(isInteractive to false.rb))
        assertThat(ambientAlpha.hasConstantValue).isTrue()
        assertThat(ambientAlpha.constantValue).isEqualTo(0f)

        // 2. Interactive mode: interactive_state = true
        val interactiveAlpha = alpha.replaceNamedVariables(mapOf(isInteractive to true.rb))
        assertThat(interactiveAlpha.hasConstantValue).isTrue()
        assertThat(interactiveAlpha.constantValue).isEqualTo(255f)
    }

    @Test
    fun testPureAmbientAlpha_evaluatesToConstantsInBothModes() {
        val isInteractive = RemoteBoolean.createNamedRemoteBoolean("interactive_state", true)
        // Alpha: 0 in interactive, 255 in ambient (e.g. bg_aod_6d2c)
        val alpha: RemoteFloat = isInteractive.select(0f.rf, 255f.rf)

        // 1. Ambient mode: interactive_state = false
        val ambientAlpha = alpha.replaceNamedVariables(mapOf(isInteractive to false.rb))
        assertThat(ambientAlpha.hasConstantValue).isTrue()
        assertThat(ambientAlpha.constantValue).isEqualTo(255f)

        // 2. Interactive mode: interactive_state = true
        val interactiveAlpha = alpha.replaceNamedVariables(mapOf(isInteractive to true.rb))
        assertThat(interactiveAlpha.hasConstantValue).isTrue()
        assertThat(interactiveAlpha.constantValue).isEqualTo(0f)
    }

    @Test
    fun testDynamicInteractiveAlpha_evaluatesToZeroInAmbient_andDynamicInInteractive() {
        val isInteractive = RemoteBoolean.createNamedRemoteBoolean("interactive_state", true)
        val dynamicHour = RemoteFloat.createNamedRemoteFloat("HOUR_0_23_MINUTE", 10f)
        // Alpha: dynamic in interactive, 0 in ambient (e.g. bg_night_2194)
        val alpha: RemoteFloat = isInteractive.select(dynamicHour * 10f.rf, 0f.rf)

        // 1. Ambient mode: must fold to constant 0
        val ambientAlpha = alpha.replaceNamedVariables(mapOf(isInteractive to false.rb))
        assertThat(ambientAlpha.hasConstantValue).isTrue()
        assertThat(ambientAlpha.constantValue).isEqualTo(0f)

        // 2. Interactive mode: must NOT fold to constant (remains dynamic)
        val interactiveAlpha = alpha.replaceNamedVariables(mapOf(isInteractive to true.rb))
        assertThat(interactiveAlpha.hasConstantValue).isFalse()
    }

    @Test
    fun testMultipliedExpression_foldsToZeroWhenAnyFactorIsZero() {
        val isInteractive = RemoteBoolean.createNamedRemoteBoolean("interactive_state", true)
        val dynamicBattery = RemoteFloat.createNamedRemoteFloat("BATTERY_PERCENT", 50f)
        val interactiveAlpha = isInteractive.select(1f.rf, 0f.rf)

        // Group alpha multiplied with dynamic child alpha: (IS_INTERACTIVE ? 1 : 0) *
        // dynamicBattery
        val composedAlpha: RemoteFloat = interactiveAlpha * dynamicBattery

        // In ambient mode (interactive_state = false), 0 * dynamicBattery must fold to constant 0
        val ambientResult = composedAlpha.replaceNamedVariables(mapOf(isInteractive to false.rb))
        assertThat(ambientResult.hasConstantValue).isTrue()
        assertThat(ambientResult.constantValue).isEqualTo(0f)

        // In interactive mode (interactive_state = true), 1 * dynamicBattery remains dynamic
        val interactiveResult = composedAlpha.replaceNamedVariables(mapOf(isInteractive to true.rb))
        assertThat(interactiveResult.hasConstantValue).isFalse()
    }

    @Test
    fun testNumericStateReplacement_withRemoteIntOrRemoteFloat() {
        // Also verify when interactive_state is passed as an Int/Float (0 or 1)
        val isInteractive = RemoteFloat.createNamedRemoteFloat("interactive_state", 1f)
        val alpha = selectIfGt(isInteractive, 0f.rf, 255f.rf, 0f.rf)

        val ambientAlpha = alpha.replaceNamedVariables(mapOf(isInteractive to 0f.rf))
        assertThat(ambientAlpha.hasConstantValue).isTrue()
        assertThat(ambientAlpha.constantValue).isEqualTo(0f)

        val interactiveAlpha = alpha.replaceNamedVariables(mapOf(isInteractive to 1f.rf))
        assertThat(interactiveAlpha.hasConstantValue).isTrue()
        assertThat(interactiveAlpha.constantValue).isEqualTo(255f)
    }

    @Test
    fun testArithmeticExpressionFolding_withIntAndFloat() {
        val namedInt = RemoteInt.createNamedRemoteInt("step_count", 100)
        val expr = (namedInt.toRemoteFloat() * 2f.rf) + 50f.rf

        val replaced = expr.replaceNamedVariables(mapOf(namedInt to 500.ri))
        assertThat(replaced.hasConstantValue).isTrue()
        assertThat(replaced.constantValue).isEqualTo(1050f)
    }

    @Test
    fun testRemoteIntReplacement() {
        val namedInt = RemoteInt.createNamedRemoteInt("raw_value", 10)
        val expr = (namedInt + 5.ri) * 2.ri

        val replaced = expr.replaceNamedVariables(mapOf(namedInt to 15.ri))
        assertThat(replaced.hasConstantValue).isTrue()
        assertThat(replaced.constantValue).isEqualTo(40)
    }

    @Test
    fun testRemoteBooleanReplacement() {
        val namedBool = RemoteBoolean.createNamedRemoteBoolean("flag", false)
        val inverted = !namedBool

        val replacedTrue = inverted.replaceNamedVariables(mapOf(namedBool to true.rb))
        assertThat(replacedTrue.hasConstantValue).isTrue()
        assertThat(replacedTrue.constantValue).isFalse()

        val replacedFalse = inverted.replaceNamedVariables(mapOf(namedBool to false.rb))
        assertThat(replacedFalse.hasConstantValue).isTrue()
        assertThat(replacedFalse.constantValue).isTrue()
    }

    @Test
    fun testRemoteDpReplacement() {
        val namedDp = RemoteDp.createNamedRemoteDp("padding_val", 10.dp)
        val dpVal = namedDp + 5.rdp

        val replaced = dpVal.replaceNamedVariables(mapOf(namedDp to 20.rdp))
        assertThat(replaced.hasConstantValue).isTrue()
        assertThat(replaced.constantValue).isEqualTo(25.dp)
    }

    @Test
    fun testRemoteStringReplacement() {
        val namedStr = RemoteString.createNamedRemoteString("user_name", "Alice")
        val greeting = "Hello, ".rs + namedStr

        val replaced = greeting.replaceNamedVariables(mapOf(namedStr to "Bob".rs))
        assertThat(replaced.hasConstantValue).isTrue()
        assertThat(replaced.constantValue).isEqualTo("Hello, Bob")
    }

    @Test
    fun testRemoteColorReplacement() {
        val isWarning = RemoteBoolean.createNamedRemoteBoolean("is_warning", false)
        val color = isWarning.select(Color.Red.rc, Color.Green.rc)

        val replacedWarning = color.replaceNamedVariables(mapOf(isWarning to true.rb))
        assertThat(replacedWarning.hasConstantValue).isTrue()
        assertThat(replacedWarning.constantValue).isEqualTo(Color.Red)

        val replacedNormal = color.replaceNamedVariables(mapOf(isWarning to false.rb))
        assertThat(replacedNormal.hasConstantValue).isTrue()
        assertThat(replacedNormal.constantValue).isEqualTo(Color.Green)
    }

    @Test
    fun testNoOp_whenReplacementsEmptyOrNoMatch() {
        val namedFloat = RemoteFloat.createNamedRemoteFloat("alpha", 1f)
        val unrelated = RemoteFloat.createNamedRemoteFloat("unrelated", 0f)
        val expr = namedFloat * 2f.rf

        val same1 = expr.replaceNamedVariables(emptyMap())
        assertThat(same1).isSameInstanceAs(expr)

        val same2 = expr.replaceNamedVariables(mapOf(unrelated to 0f.rf))
        assertThat(same2).isSameInstanceAs(expr)
    }

    @Test
    fun testDomainPrefixedMatching() {
        val namedFloat =
            RemoteFloat.createNamedRemoteFloat(
                name = "custom_var",
                defaultValue = 10f,
                domain = RemoteState.Domain.System,
            )
        val expr = namedFloat + 5f.rf

        val sameNamedFloat =
            RemoteFloat.createNamedRemoteFloat(
                name = "custom_var",
                defaultValue = 10f,
                domain = RemoteState.Domain.System,
            )
        val replaced = expr.replaceNamedVariables(mapOf(sameNamedFloat to 20f.rf))
        assertThat(replaced.hasConstantValue).isTrue()
        assertThat(replaced.constantValue).isEqualTo(25f)
    }

    @Test
    fun testDAGSharedSubExpression_isTransformedOnce() {
        val isInteractive = RemoteBoolean.createNamedRemoteBoolean("interactive_state", true)
        val sharedBranch = isInteractive.select(10f.rf, 0f.rf)

        // Diamond DAG: sharedBranch + sharedBranch
        val diamond = sharedBranch + sharedBranch

        val result = diamond.replaceNamedVariables(mapOf(isInteractive to true.rb))
        assertThat(result.hasConstantValue).isTrue()
        assertThat(result.constantValue).isEqualTo(20f)
    }

    @Test
    fun testRemoteLongReplacement() {
        val namedLong = RemoteLong.createNamedRemoteLong("start_time", 1000L)
        val replaced = namedLong.replaceNamedVariables(mapOf(namedLong to RemoteLong(2000L)))
        assertThat(replaced.hasConstantValue).isTrue()
        assertThat(replaced.constantValue).isEqualTo(2000L)
    }

    @Test
    fun testRemoteLongArithmeticReplacement() {
        val namedLong = RemoteLong.createNamedRemoteLong("start_time", 1000L)
        val totalTime = namedLong + RemoteLong(500L)
        val replaced = totalTime.replaceNamedVariables(mapOf(namedLong to RemoteLong(2000L)))
        assertThat(replaced.hasConstantValue).isTrue()
        assertThat(replaced.constantValue).isEqualTo(2500L)
    }

    @Test
    fun testSelectIfLt_withFloatAndInt() {
        val condFloat = RemoteFloat.createNamedRemoteFloat("cond_f", 1f)
        val selectedFloat = selectIfLt(condFloat, 5f.rf, 100f.rf, 200f.rf)
        val replacedFloat = selectedFloat.replaceNamedVariables(mapOf(condFloat to 10f.rf))
        assertThat(replacedFloat.hasConstantValue).isTrue()
        assertThat(replacedFloat.constantValue).isEqualTo(200f)

        val condInt = RemoteInt.createNamedRemoteInt("cond_i", 1)
        val selectedInt = selectIfLt(condInt, 5.ri, 10.ri, 20.ri)
        val replacedInt = selectedInt.replaceNamedVariables(mapOf(condInt to 10.ri))
        assertThat(replacedInt.hasConstantValue).isTrue()
        assertThat(replacedInt.constantValue).isEqualTo(20)
    }

    @Test
    fun testCustomVisitor_collectsNamedVariables() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 1f)
        val b = RemoteFloat.createNamedRemoteFloat("b", 2f)
        val c = RemoteFloat.createNamedRemoteFloat("c", 3f)
        val expr = (a + b) * c

        val names =
            expr.accept { key, _, visitedArgs ->
                val fromArgs = visitedArgs.flatten().toSet()
                if (key is RemoteNamedCacheKey) fromArgs + key.name else fromArgs
            }
        assertThat(names).containsExactly("a", "b", "c")
    }

    @Test
    fun testCustomRewriter_scalesConstants() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 1f)
        val expr = a + 10f.rf

        val result =
            expr.transform { node ->
                val value = node.constantValueOrNull
                if (value is Float) RemoteFloat(value * 2f) else null
            }
        val evaluated = result.replaceNamedVariables(mapOf(a to 5f.rf))
        assertThat(evaluated.hasConstantValue).isTrue()
        assertThat(evaluated.constantValue).isEqualTo(25f)
    }

    @Test
    fun testDynamicToDynamicReplacement() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 1f)
        val b = RemoteFloat.createNamedRemoteFloat("b", 2f)
        val expr = a + 5f.rf

        // Replace 'a' with 'b * 2'
        val dynamicReplaced = expr.replaceNamedVariables(mapOf(a to (b * 2f.rf)))
        assertThat(dynamicReplaced.hasConstantValue).isFalse()

        // Now replace 'b' with 10
        val finalResult = dynamicReplaced.replaceNamedVariables(mapOf(b to 10f.rf))
        assertThat(finalResult.hasConstantValue).isTrue()
        assertThat(finalResult.constantValue).isEqualTo(25f)
    }

    @Test
    fun testAnimatedRemoteFloat_preservesAnimationWhenVariablesReplaced() {
        val namedInput = RemoteFloat.createNamedRemoteFloat("progress_val", 0f)
        val animSpec = floatArrayOf(0f, 1f, 2f)
        val animated = AnimatedRemoteFloat(namedInput, animSpec)

        val replaced = animated.replaceNamedVariables(mapOf(namedInput to 5f.rf))
        assertThat(replaced).isInstanceOf(AnimatedRemoteFloat::class.java)
        val replacedAnim = replaced as AnimatedRemoteFloat
        assertThat(replacedAnim.input.hasConstantValue).isTrue()
        assertThat(replacedAnim.input.constantValue).isEqualTo(5f)
        assertThat(replacedAnim.anim).isEqualTo(animSpec)
    }

    @Test
    fun testAnimatedRemoteFloat_preservesIdentityWhenNoReplacements() {
        val namedInput = RemoteFloat.createNamedRemoteFloat("progress_val", 0f)
        val unrelated = RemoteFloat.createNamedRemoteFloat("unrelated", 10f)
        val animSpec = floatArrayOf(0f, 1f, 2f)
        val animated = AnimatedRemoteFloat(namedInput, animSpec)

        val unchanged = animated.replaceNamedVariables(emptyMap())
        assertThat(unchanged).isSameInstanceAs(animated)

        val unmatching = animated.replaceNamedVariables(mapOf(unrelated to 10f.rf))
        assertThat(unmatching).isSameInstanceAs(animated)
    }

    @Test
    fun testCubicEasing_foldsWhenAllInputsConstant() {
        val progress = RemoteFloat.createNamedRemoteFloat("progress", 0.5f)
        val cubic = cubicEasing(0.25f.rf, 0.1f.rf, 0.25f.rf, 1.0f.rf, progress)

        val replaced = cubic.replaceNamedVariables(mapOf(progress to 0.5f.rf))
        assertThat(replaced.hasConstantValue).isTrue()
    }

    @Test
    fun testCubicEasing_preservesOperationWhenInputRemainsDynamic() {
        val progress = RemoteFloat.createNamedRemoteFloat("progress", 0.5f)
        val dynamicOther = RemoteFloat.createNamedRemoteFloat("dynamic_other", 0.8f)
        val cubic = cubicEasing(0.25f.rf, 0.1f.rf, 0.25f.rf, 1.0f.rf, progress)

        val replaced = cubic.replaceNamedVariables(mapOf(progress to dynamicOther))
        assertThat(replaced.hasConstantValue).isFalse()
        assertThat(replaced.cacheKey).isInstanceOf(RemoteOperationCacheKey::class.java)
        assertThat((replaced.cacheKey as RemoteOperationCacheKey).op)
            .isEqualTo(RemoteFloat.OperationKey.Cubic)
    }

    @Test
    fun testEvalSpline_preservesOperationWhenInputRemainsDynamic() {
        val progress = RemoteFloat.createNamedRemoteFloat("progress", 0.5f)
        val dynamicOther = RemoteFloat.createNamedRemoteFloat("dynamic_other", 0.8f)
        val controlPoints = RemoteFloatArray(listOf(0f.rf, 10f.rf, 20f.rf))
        val spline = evalSpline(controlPoints, loop = false, progress = progress)
        val splineLoop = evalSpline(controlPoints, loop = true, progress = progress)

        val replacedSpline = spline.replaceNamedVariables(mapOf(progress to dynamicOther))
        assertThat(replacedSpline.hasConstantValue).isFalse()
        assertThat(replacedSpline.cacheKey).isInstanceOf(RemoteOperationCacheKey::class.java)
        assertThat((replacedSpline.cacheKey as RemoteOperationCacheKey).op)
            .isEqualTo(RemoteFloat.OperationKey.Spline)

        val replacedSplineLoop = splineLoop.replaceNamedVariables(mapOf(progress to dynamicOther))
        assertThat(replacedSplineLoop.hasConstantValue).isFalse()
        assertThat(replacedSplineLoop.cacheKey).isInstanceOf(RemoteOperationCacheKey::class.java)
        assertThat((replacedSplineLoop.cacheKey as RemoteOperationCacheKey).op)
            .isEqualTo(RemoteFloat.OperationKey.SplineLoop)
    }

    @Test
    fun testTweenInt_withOneOrBothColorsReplaced() {
        val tweenProgress = RemoteFloat.createNamedRemoteFloat("tween_p", 0.5f)
        val tweenColor = tween(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), tweenProgress)

        // 1. Replace only 'from' with a RemoteColor
        val resultFrom =
            tweenColor.transform { node ->
                if (node.constantValueOrNull == 0xFFFF0000.toInt()) {
                    Color.Blue.rc
                } else {
                    null
                }
            }
        assertThat(resultFrom).isNotNull()

        // 2. Replace only 'to' with a RemoteColor
        val resultTo =
            tweenColor.transform { node ->
                if (node.constantValueOrNull == 0xFF00FF00.toInt()) {
                    Color.Yellow.rc
                } else {
                    null
                }
            }
        assertThat(resultTo).isNotNull()

        // 3. Replace progress and evaluate
        val evaluated = resultFrom.replaceNamedVariables(mapOf(tweenProgress to 0f.rf))
        assertThat(evaluated.hasConstantValue).isTrue()
        assertThat(evaluated.constantValue).isEqualTo(Color.Blue)
    }

    @Test
    fun testTweenInt_withDynamicRemoteInt_throwsDescriptiveError() {
        val tweenProgress = RemoteFloat.createNamedRemoteFloat("tween_p", 0.5f)
        val tweenColor = tween(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), tweenProgress)
        val dynamicInt = RemoteInt.createNamedRemoteInt("dynamic_color", 0)

        var exceptionThrown = false
        try {
            tweenColor.transform { node ->
                if (node.constantValueOrNull == 0xFFFF0000.toInt()) {
                    dynamicInt
                } else {
                    null
                }
            }
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
            assertThat(e.message).contains("Dynamic RemoteInt cannot be used as a color")
        }
        assertThat(exceptionThrown).isTrue()
    }

    @Test
    fun testIsStructurallyEqual_constants() {
        val f1 = 5f.rf
        val f2 = 5f.rf
        val f3 = 10f.rf

        assertThat(f1.isStructurallyEqual(f2)).isTrue()
        assertThat(f1.isStructurallyEqual(f3)).isFalse()
        assertThat(f1.isStructurallyEqual(f1)).isTrue()
        assertThat(f1.isStructurallyEqual(null)).isFalse()

        val i1 = 5.ri
        val i2 = 5.ri
        val i3 = 10.ri
        assertThat(i1.isStructurallyEqual(i2)).isTrue()
        assertThat(i1.isStructurallyEqual(i3)).isFalse()
        assertThat(i1.isStructurallyEqual(f1)).isFalse()

        val s1 = "hello".rs
        val s2 = "hello".rs
        val s3 = "world".rs
        assertThat(s1.isStructurallyEqual(s2)).isTrue()
        assertThat(s1.isStructurallyEqual(s3)).isFalse()

        val b1 = true.rb
        val b2 = true.rb
        val b3 = false.rb
        assertThat(b1.isStructurallyEqual(b2)).isTrue()
        assertThat(b1.isStructurallyEqual(b3)).isFalse()
    }

    @Test
    fun testIsStructurallyEqual_dynamicExpressions() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 1f)
        val b = RemoteFloat.createNamedRemoteFloat("b", 2f)
        val c = RemoteFloat.createNamedRemoteFloat("c", 3f)

        val expr1 = (a + b) * 10f.rf
        val expr2 = (a + b) * 10f.rf
        val expr3 = (a + c) * 10f.rf
        val expr4 = (a + b) * 20f.rf

        assertThat(expr1.isStructurallyEqual(expr2)).isTrue()
        assertThat(expr1.isStructurallyEqual(expr3)).isFalse()
        assertThat(expr1.isStructurallyEqual(expr4)).isFalse()

        // Constant vs dynamic
        val constExpr = 30f.rf
        assertThat(expr1.isStructurallyEqual(constExpr)).isFalse()
        assertThat(constExpr.isStructurallyEqual(expr1)).isFalse()
    }

    @Test
    fun testSelectBoolean_reconstruction() {
        val cond = RemoteBoolean.createNamedRemoteBoolean("cond", true)
        val selected = cond.select(true.rb, false.rb)

        val replaced = selected.replaceNamedVariables(mapOf(cond to false.rb))
        assertThat(replaced.hasConstantValue).isTrue()
        assertThat(replaced.constantValue).isFalse()

        // When branches remain dynamic
        val b1 = RemoteBoolean.createNamedRemoteBoolean("b1", true)
        val b2 = RemoteBoolean.createNamedRemoteBoolean("b2", false)
        val selectedDynamic = cond.select(b1, b2)
        val replacedDynamic = selectedDynamic.replaceNamedVariables(mapOf(cond to false.rb))
        assertThat(replacedDynamic.hasConstantValue).isFalse()
        assertThat(replacedDynamic.isStructurallyEqual(b2)).isTrue()
    }

    @Test
    fun testToDp_reconstruction() {
        val floatPx = RemoteFloat.createNamedRemoteFloat("px", 100f)
        val dpVal = floatPx.toRemoteDp()

        val dynamicOther = RemoteFloat.createNamedRemoteFloat("px2", 200f)
        val replaced = dpVal.replaceNamedVariables(mapOf(floatPx to dynamicOther))
        assertThat(replaced).isInstanceOf(RemoteDp::class.java)
        assertThat(replaced.value.cacheKey).isInstanceOf(RemoteOperationCacheKey::class.java)
        assertThat((replaced.value.cacheKey as RemoteOperationCacheKey).op)
            .isEqualTo(RemoteDp.OperationKey.ToDp)
    }

    @Test
    fun testColorComponent_reconstruction() {
        val namedColor = RemoteColor.createNamedRemoteColor("color", Color.Red)
        val alpha = namedColor.alpha

        val replaced = alpha.replaceNamedVariables(mapOf(namedColor to Color.Blue.rc))
        assertThat(replaced.hasConstantValue).isTrue()
        assertThat(replaced.constantValue).isEqualTo(1.0f)
    }

    @Test
    fun testSubstring1Arg_reconstruction() {
        val namedStr = RemoteString.createNamedRemoteString("s", "HelloWorld")
        val sub = namedStr.substring(5.ri)

        val replaced = sub.replaceNamedVariables(mapOf(namedStr to "HelloAndroid".rs))
        assertThat(replaced.hasConstantValue).isTrue()
        assertThat(replaced.constantValue).isEqualTo("Android")
    }

    @Test
    fun testFloatArray_reconstruction() {
        val namedFloat = RemoteFloat.createNamedRemoteFloat("f", 10f)
        val array = RemoteFloatArray(listOf(1f.rf, namedFloat, 3f.rf))
        val index = RemoteInt.createNamedRemoteInt("idx", 0)
        val element = array[index]

        val replaced = element.replaceNamedVariables(mapOf(index to 1.ri, namedFloat to 20f.rf))
        assertThat(replaced.hasConstantValue).isTrue()
        assertThat(replaced.constantValue).isEqualTo(20f)
    }

    @Test
    fun testRemoteMatrix3x3_reconstruction() {
        val angle = RemoteFloat.createNamedRemoteFloat("angle", 45f)
        val matrix = RemoteMatrix3x3.createRotate(angle)

        val replaced = matrix.replaceNamedVariables(mapOf(angle to 90f.rf))
        assertThat(replaced).isInstanceOf(RemoteMatrix3x3::class.java)
        assertThat(replaced.cacheKey).isInstanceOf(RemoteOperationCacheKey::class.java)
        assertThat((replaced.cacheKey as RemoteOperationCacheKey).op)
            .isEqualTo(RemoteMatrix3x3.OperationKey.ROTATE)
    }

    enum class TestEnum {
        First,
        Second,
    }

    @Test
    fun testRemoteEnumToString_reconstruction() {
        val namedInt = RemoteInt.createNamedRemoteInt("enum_ord", 0)
        val entries = kotlin.enums.enumEntries<TestEnum>()
        val remoteEnum = RemoteEnum(namedInt, entries)
        val str = remoteEnum.toRemoteString()

        val replaced =
            str.replaceNamedVariables(mapOf(remoteEnum to RemoteEnum(TestEnum.Second, entries)))
        assertThat(replaced.hasConstantValue).isTrue()
        assertThat(replaced.constantValue).isEqualTo("Second")
    }

    @Test
    fun testFromAHSV_reconstruction() {
        val h = RemoteFloat.createNamedRemoteFloat("h", 0.5f)
        val color = RemoteColor.fromAHSV(255, h, 1f.rf, 1f.rf)

        val replaced = color.replaceNamedVariables(mapOf(h to 0.25f.rf))
        assertThat(replaced.hasConstantValue).isTrue()
    }

    @Test
    fun testRemoteMutableFloatArray_transform() {
        val mutableArray = RemoteMutableFloatArray(10)
        val index = RemoteInt.createNamedRemoteInt("idx", 0)
        val element = mutableArray[index]

        val replaced = element.replaceNamedVariables(mapOf(index to 1.ri))
        assertThat(replaced).isInstanceOf(RemoteFloat::class.java)
    }
}
