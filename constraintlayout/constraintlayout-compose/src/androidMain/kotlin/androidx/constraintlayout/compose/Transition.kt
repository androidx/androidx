/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.compose

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.core.parser.CLObject
import androidx.constraintlayout.core.parser.CLParser
import androidx.constraintlayout.core.parser.CLParsingException
import androidx.constraintlayout.core.state.CorePixelDp
import androidx.constraintlayout.core.state.TransitionParser
import org.intellij.lang.annotations.Language

/**
 * Defines interpolation parameters between two [ConstraintSet]s.
 */
@ExperimentalMotionApi
@Immutable
interface Transition {
    fun getStartConstraintSetId(): String
    fun getEndConstraintSetId(): String
}

/**
 * Parses the given JSON5 into a [Transition].
 *
 * See the official [Github Wiki](https://github.com/androidx/constraintlayout/wiki/Compose-MotionLayout-JSON-Syntax#transitions) to learn the syntax.
 */
@SuppressLint("ComposableNaming")
@ExperimentalMotionApi
@Composable
fun Transition(@Language("json5") content: String): Transition? {
    val dpToPixel = with(LocalDensity.current) { 1.dp.toPx() }
    val transition = remember(content) {
        val parsed = try {
            CLParser.parse(content)
        } catch (e: CLParsingException) {
            Log.e("CML", "Error parsing JSON $e")
            null
        }
        if (parsed != null) {
            val pixelDp = CorePixelDp { dpValue -> dpValue * dpToPixel }
            TransitionImpl(parsed, pixelDp)
        } else {
            null
        }
    }
    return transition
}

/**
 * Subclass of [Transition] for internal use.
 *
 * Used to reduced the exposed API from [Transition].
 */
@ExperimentalMotionApi
internal class TransitionImpl(
    private val parsedTransition: CLObject,
    private val pixelDp: CorePixelDp
) : Transition {

    /**
     * Applies all Transition properties to [transition].
     */
    fun applyAllTo(transition: androidx.constraintlayout.core.state.Transition) {
        try {
            TransitionParser.parse(parsedTransition, transition, pixelDp)
        } catch (e: CLParsingException) {
            Log.e("CML", "Error parsing JSON $e")
        }
    }

    /**
     * Applies only the KeyFrame related properties (KeyCycles, KeyAttributes, KeyPositions) to
     * [transition], which effectively sets the respective parameters for each WidgetState.
     */
    fun applyKeyFramesTo(transition: androidx.constraintlayout.core.state.Transition) {
        try {
            TransitionParser.parseKeyFrames(parsedTransition, transition)
        } catch (e: CLParsingException) {
            Log.e("CML", "Error parsing JSON $e")
        }
    }

    override fun getStartConstraintSetId(): String {
        return parsedTransition.getStringOrNull("from") ?: "start"
    }

    override fun getEndConstraintSetId(): String {
        return parsedTransition.getStringOrNull("to") ?: "end"
    }
}