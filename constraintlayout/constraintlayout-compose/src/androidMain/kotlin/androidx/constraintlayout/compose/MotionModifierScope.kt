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

import androidx.constraintlayout.core.parser.CLArray
import androidx.constraintlayout.core.parser.CLObject

/**
 * Define keyframes for the animation caused by [MotionScope.motion].
 *
 * @see [KeyAttributesScope]
 * @see [KeyPositionsScope]
 * @see [KeyCyclesScope]
 */
@ExperimentalMotionApi
class MotionModifierScope internal constructor(
    id: Any
) {
    private val stringId = ConstrainedLayoutReference(id.toString())
    private val containerObject = CLObject(charArrayOf())

    private val keyFramesObject = CLObject(charArrayOf())
    private val keyAttributesArray = CLArray(charArrayOf())
    private val keyPositionsArray = CLArray(charArrayOf())
    private val keyCyclesArray = CLArray(charArrayOf())

    private val onSwipeObject = CLObject(charArrayOf())

    internal fun reset() {
        containerObject.clear()
        keyFramesObject.clear()
        keyAttributesArray.clear()
        onSwipeObject.clear()
    }

    private fun addKeyAttributesIfMissing() {
        containerObject.put("KeyFrames", keyFramesObject)
        keyFramesObject.put("KeyAttributes", keyAttributesArray)
    }

    private fun addKeyPositionsIfMissing() {
        containerObject.put("KeyFrames", keyFramesObject)
        keyFramesObject.put("KeyPositions", keyPositionsArray)
    }

    private fun addKeyCyclesIfMissing() {
        containerObject.put("KeyFrames", keyFramesObject)
        keyFramesObject.put("KeyCycles", keyCyclesArray)
    }

    var motionArc: Arc = Arc.None

    fun keyAttributes(keyAttributesContent: KeyAttributesScope.() -> Unit) {
        val scope = KeyAttributesScope(stringId)
        keyAttributesContent(scope)
        addKeyAttributesIfMissing()
        keyAttributesArray.add(scope.keyFramePropsObject)
    }

    fun keyPositions(keyPositionsContent: KeyPositionsScope.() -> Unit) {
        val scope = KeyPositionsScope(stringId)
        keyPositionsContent(scope)
        addKeyPositionsIfMissing()
        keyPositionsArray.add(scope.keyFramePropsObject)
    }

    fun keyCycles(keyCyclesContent: KeyCyclesScope.() -> Unit) {
        val scope = KeyCyclesScope(stringId)
        keyCyclesContent(scope)
        addKeyCyclesIfMissing()
        keyCyclesArray.add(scope.keyFramePropsObject)
    }

    internal fun getObject(): CLObject {
        containerObject.putString("pathMotionArc", motionArc.name)
        containerObject.putString("from", "start")
        containerObject.putString("to", "end")
        return containerObject
    }
}