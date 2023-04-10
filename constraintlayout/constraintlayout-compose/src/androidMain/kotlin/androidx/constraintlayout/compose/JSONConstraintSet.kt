/*
 * Copyright (C) 2021 The Android Open Source Project
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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.layout.Measurable
import androidx.constraintlayout.core.parser.CLKey
import androidx.constraintlayout.core.parser.CLParser
import androidx.constraintlayout.core.parser.CLParsingException
import androidx.constraintlayout.core.state.ConstraintSetParser
import androidx.constraintlayout.core.state.Transition
import org.intellij.lang.annotations.Language

@Immutable
internal class JSONConstraintSet(
    @Language("json5") content: String,
    @Language("json5") overrideVariables: String? = null,
    override val extendFrom: ConstraintSet? = null
) : EditableJSONLayout(content), DerivedConstraintSet {
    private val overridedVariables = HashMap<String, Float>()
    private val overrideVariables = overrideVariables
    private var _isDirty = true

    init {
        initialization()
    }

    override fun equals(other: Any?): Boolean {
        if (other is JSONConstraintSet) {
            return this.getCurrentContent() == other.getCurrentContent()
        }
        return false
    }

    override fun isDirty(measurables: List<Measurable>): Boolean {
        return _isDirty
    }

    // Only called by MotionLayout in MotionMeasurer
    override fun applyTo(transition: Transition, type: Int) {
        val layoutVariables = ConstraintSetParser.LayoutVariables()
        applyLayoutVariables(layoutVariables)
        ConstraintSetParser.parseJSON(getCurrentContent(), transition, type)
    }

    fun emitDesignElements(designElements: ArrayList<ConstraintSetParser.DesignElement>) {
        try {
            designElements.clear()
            ConstraintSetParser.parseDesignElementsJSON(getCurrentContent(), designElements)
        } catch (e: Exception) {
            // nothing (content might be invalid, sent by live edit)
        }
    }

    // Called by both MotionLayout & ConstraintLayout measurers
    override fun applyToState(state: State) {
        val layoutVariables = ConstraintSetParser.LayoutVariables()
        applyLayoutVariables(layoutVariables)
        // TODO: Need to better handle half parsed JSON and/or incorrect states.
        try {
            ConstraintSetParser.parseJSON(getCurrentContent(), state, layoutVariables)
            _isDirty = false
        } catch (e: Exception) {
            // nothing (content might be invalid, sent by live edit)
            _isDirty = true
        }
    }

    override fun onNewContent(content: String) {
        super.onNewContent(content)
        _isDirty = true
    }

    override fun override(name: String, value: Float): ConstraintSet {
        overridedVariables[name] = value
        return this
    }

    private fun applyLayoutVariables(layoutVariables: ConstraintSetParser.LayoutVariables) {
        if (overrideVariables != null) {
            try {
                val variables = CLParser.parse(overrideVariables)
                for (i in 0 until variables.size()) {
                    val key = variables[i] as CLKey
                    val variable = key.value.float
                    // TODO: allow arbitrary override, not just float values
                    layoutVariables.putOverride(key.content(), variable)
                }
            } catch (e: CLParsingException) {
                System.err.println("exception: " + e)
            }
        }
        for (name in overridedVariables.keys) {
            layoutVariables.putOverride(name, overridedVariables[name]!!)
        }
    }

    override fun resetForcedProgress() {
        // Nothing for ConstraintSet
    }

    override fun getForcedProgress(): Float {
        // Nothing for ConstraintSet
        return 0f
    }

    override fun onNewProgress(progress: Float) {
        // Nothing for ConstraintSet
    }
}