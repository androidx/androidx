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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.core.state.CorePixelDp

private const val UNDEFINED_NAME_PREFIX = "androidx.constraintlayout"

/**
 * Returns a [MotionScene] instance defined by [motionSceneContent].
 *
 * @see MotionSceneScope
 * @see TransitionScope
 * @see ConstraintSetScope
 */
@ExperimentalMotionApi
@Composable
fun MotionScene(
    motionSceneContent: MotionSceneScope.() -> Unit
): MotionScene {
    // TODO: Add a state listener
    val dpToPixel = with(LocalDensity.current) { CorePixelDp { 1.dp.toPx() } }
    val scope = remember { MotionSceneScope(dpToPixel) }
    scope.reset()
    scope.motionSceneContent()
    return remember {
        // Clone the elements to avoid issues with async mutability
        MotionSceneDslImpl(
            constraintSetsByName = scope.constraintSetsByName.toMap(),
            transitionsByName = scope.transitionsByName.toMap()
        )
    }
}

@ExperimentalMotionApi
internal class MotionSceneDslImpl(
    private val constraintSetsByName: Map<String, ConstraintSet>,
    private val transitionsByName: Map<String, Transition>
) : MotionScene {
    override fun setTransitionContent(elementName: String?, toJSON: String?) {
        // Do Nothing
    }

    override fun getConstraintSet(ext: String?): String {
        // Do nothing
        return ""
    }

    override fun setConstraintSetContent(csName: String?, toJSON: String?) {
        // Do nothing
        // TODO: Consider overwriting ConstraintSet instance
    }

    override fun setDebugName(name: String?) {
        // Do nothing
    }

    override fun getTransition(str: String?): String {
        // Do nothing
        return ""
    }

    override fun getConstraintSet(index: Int): String {
        // Do nothing
        return ""
    }

    override fun getConstraintSetInstance(name: String): ConstraintSet? {
        return constraintSetsByName[name]
    }

    override fun getTransitionInstance(name: String): Transition? {
        return transitionsByName[name]
    }
}

/**
 * Scope used by the MotionScene DSL.
 *
 * &nbsp;
 *
 * Define new [ConstraintSet]s and [Transition]s within this scope using [constraintSet] and
 * [transition] respectively.
 *
 * Alternatively, you may add existing objects to this scope using [addConstraintSet] and
 * [addTransition].
 *
 * &nbsp;
 *
 * The [defaultTransition] **should always be set**. It defines the initial state of the layout and
 * works as a fallback for undefined `from -> to` transitions.
 */
@ExperimentalMotionApi
class MotionSceneScope internal constructor(private val dpToPixel: CorePixelDp) {
    /**
     * Count of generated ConstraintSet & Transition names.
     */
    private var generatedCount = 0

    /**
     * Returns a new unique name. Should be used when the user does not provide a specific name
     * for their ConstraintSets/Transitions.
     */
    private fun nextName() = UNDEFINED_NAME_PREFIX + generatedCount++

    internal var constraintSetsByName = HashMap<String, ConstraintSet>()
    internal var transitionsByName = HashMap<String, Transition>()

    internal fun reset() {
        generatedCount = 0
        constraintSetsByName.clear()
        transitionsByName.clear()
    }

    /**
     * Defines the default [Transition], where the [from] and [to] [ConstraintSet]s will be the
     * initial start and end states of the layout.
     *
     * The default [Transition] will also be applied when the combination of `from` and `to`
     * ConstraintSets was not defined by a [transition] call.
     *
     * This [Transition] is required to initialize [MotionLayout].
     */
    fun defaultTransition(
        from: ConstraintSetRef,
        to: ConstraintSetRef,
        transitionContent: TransitionScope.() -> Unit = { }
    ) {
        transition("default", from, to, transitionContent)
    }

    /**
     * Creates a [ConstraintSet] that extends the changes applied by [extendConstraintSet] (if not
     * null).
     *
     * A [name] may be provided and it can be used on MotionLayout calls that request a
     * ConstraintSet name.
     *
     * Returns a [ConstraintSetRef] object representing this ConstraintSet, which may be used as a
     * parameter of [transition].
     */
    fun constraintSet(
        name: String? = null,
        extendConstraintSet: ConstraintSetRef? = null,
        constraintSetContent: ConstraintSetScope.() -> Unit
    ): ConstraintSetRef {
        return addConstraintSet(
            name = name,
            constraintSet = DslConstraintSet(
                description = constraintSetContent,
                extendFrom = extendConstraintSet?.let { constraintSetsByName[it.name] }
            )
        )
    }

    /**
     * Adds a [Transition] defined by [transitionContent]. A [name] may be provided and it can
     * be used on MotionLayout calls that request a Transition name.
     *
     * Where [from] and [to] are the ConstraintSets handled by it.
     */
    fun transition(
        name: String? = null,
        from: ConstraintSetRef,
        to: ConstraintSetRef,
        transitionContent: TransitionScope.() -> Unit
    ) {
        val transitionName = name ?: nextName()
        transitionsByName[transitionName] = TransitionImpl(
            parsedTransition = TransitionScope(
                from = from.name,
                to = to.name
            ).apply(transitionContent).getObject(),
            pixelDp = dpToPixel
        )
    }

    /**
     * Adds an existing [ConstraintSet] object to the scope of this MotionScene. A [name] may be
     * provided and it can be used on MotionLayout calls that request a ConstraintSet name.
     *
     * Returns a [ConstraintSetRef] object representing the added [constraintSet], which may be used
     * as a parameter of [transition].
     */
    fun addConstraintSet(
        name: String? = null,
        constraintSet: ConstraintSet
    ): ConstraintSetRef {
        val cSetName = name ?: nextName()
        constraintSetsByName[cSetName] = constraintSet
        return ConstraintSetRef(cSetName)
    }

    /**
     * Adds an existing [Transition] object to the scope of this MotionScene. A [name] may be
     * provided and it can be used on MotionLayout calls that request a Transition name.
     *
     * The [ConstraintSet]s referenced by the transition must match the name of a [ConstraintSet]
     * added within this scope.
     *
     * @see [constraintSet]
     * @see [addConstraintSet]
     */
    fun addTransition(
        name: String? = null,
        transition: Transition
    ) {
        val transitionName = name ?: nextName()
        transitionsByName[transitionName] = transition
    }

    /**
     * Creates one [ConstrainedLayoutReference] corresponding to the [ConstraintLayout] element
     * with [id].
     */
    fun createRefFor(id: Any): ConstrainedLayoutReference = ConstrainedLayoutReference(id)

    /**
     * Declare a custom Float [value] addressed by [name].
     */
    fun ConstrainScope.customFloat(name: String, value: Float) {
        tasks.add { state ->
            state.constraints(id).addCustomFloat(name, value)
        }
    }

    /**
     * Declare a custom Color [value] addressed by [name].
     */
    fun ConstrainScope.customColor(name: String, value: Color) {
        tasks.add { state ->
            state.constraints(id).addCustomColor(name, value.toArgb())
        }
    }
}

data class ConstraintSetRef internal constructor(
    internal val name: String
)