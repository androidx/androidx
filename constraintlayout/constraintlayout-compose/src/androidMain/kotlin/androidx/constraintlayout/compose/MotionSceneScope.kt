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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.constraintlayout.core.parser.CLObject

private const val UNDEFINED_NAME_PREFIX = "androidx.constraintlayout"

/**
 * Returns a [MotionScene] instance defined by [motionSceneContent].
 *
 * @see MotionSceneScope
 * @see TransitionScope
 * @see ConstraintSetScope
 */
fun MotionScene(
    motionSceneContent: MotionSceneScope.() -> Unit
): MotionScene {
    val scope = MotionSceneScope().apply(motionSceneContent)
    return MotionSceneDslImpl(
        constraintSetsByName = scope.constraintSetsByName,
        transitionsByName = scope.transitionsByName
    )
}

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MotionSceneDslImpl

        if (constraintSetsByName != other.constraintSetsByName) return false
        if (transitionsByName != other.transitionsByName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = constraintSetsByName.hashCode()
        result = 31 * result + transitionsByName.hashCode()
        return result
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
class MotionSceneScope internal constructor() {
    /**
     * Count of generated ConstraintSet & Transition names.
     */
    private var generatedCount = 0

    /**
     * Count of generated ConstraintLayoutReference IDs.
     */
    private var generatedIdCount = 0

    /**
     * Returns a new unique name. Should be used when the user does not provide a specific name
     * for their ConstraintSets/Transitions.
     */
    private fun nextName() = UNDEFINED_NAME_PREFIX + generatedCount++

    private fun nextId() = UNDEFINED_NAME_PREFIX + "id${generatedIdCount++}"

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
        transition(from, to, "default", transitionContent)
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
            constraintSet = DslConstraintSet(
                description = constraintSetContent,
                extendFrom = extendConstraintSet?.let { constraintSetsByName[it.name] }
            ),
            name = name
        )
    }

    /**
     * Adds a [Transition] defined by [transitionContent]. A [name] may be provided and it can
     * be used on MotionLayout calls that request a Transition name.
     *
     * Where [from] and [to] are the ConstraintSets handled by it.
     */
    fun transition(
        from: ConstraintSetRef,
        to: ConstraintSetRef,
        name: String? = null,
        transitionContent: TransitionScope.() -> Unit
    ) {
        val transitionName = name ?: nextName()
        transitionsByName[transitionName] = TransitionImpl(
            parsedTransition = TransitionScope(
                from = from.name,
                to = to.name
            ).apply(transitionContent).getObject()
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
        constraintSet: ConstraintSet,
        name: String? = null
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
        transition: Transition,
        name: String? = null
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
     * Convenient way to create multiple [ConstrainedLayoutReference] with one statement, the [ids]
     * provided should match Composables within ConstraintLayout using [androidx.compose.ui.Modifier.layoutId].
     *
     * Example:
     * ```
     * val (box, text, button) = createRefsFor("box", "text", "button")
     * ```
     * Note that the number of ids should match the number of variables assigned.
     *
     * &nbsp;
     *
     * To create a singular [ConstrainedLayoutReference] see [createRefFor].
     */
    fun createRefsFor(vararg ids: Any): ConstrainedLayoutReferences =
        ConstrainedLayoutReferences(arrayOf(*ids))

    inner class ConstrainedLayoutReferences internal constructor(
        private val ids: Array<Any>
    ) {
        operator fun component1(): ConstrainedLayoutReference =
            ConstrainedLayoutReference(ids.getOrElse(0) { nextId() })

        operator fun component2(): ConstrainedLayoutReference =
            createRefFor(ids.getOrElse(1) { nextId() })

        operator fun component3(): ConstrainedLayoutReference =
            createRefFor(ids.getOrElse(2) { nextId() })

        operator fun component4(): ConstrainedLayoutReference =
            createRefFor(ids.getOrElse(3) { nextId() })

        operator fun component5(): ConstrainedLayoutReference =
            createRefFor(ids.getOrElse(4) { nextId() })

        operator fun component6(): ConstrainedLayoutReference =
            createRefFor(ids.getOrElse(5) { nextId() })

        operator fun component7(): ConstrainedLayoutReference =
            createRefFor(ids.getOrElse(6) { nextId() })

        operator fun component8(): ConstrainedLayoutReference =
            createRefFor(ids.getOrElse(7) { nextId() })

        operator fun component9(): ConstrainedLayoutReference =
            createRefFor(ids.getOrElse(8) { nextId() })

        operator fun component10(): ConstrainedLayoutReference =
            createRefFor(ids.getOrElse(9) { nextId() })

        operator fun component11(): ConstrainedLayoutReference =
            createRefFor(ids.getOrElse(10) { nextId() })

        operator fun component12(): ConstrainedLayoutReference =
            createRefFor(ids.getOrElse(11) { nextId() })

        operator fun component13(): ConstrainedLayoutReference =
            createRefFor(ids.getOrElse(12) { nextId() })

        operator fun component14(): ConstrainedLayoutReference =
            createRefFor(ids.getOrElse(13) { nextId() })

        operator fun component15(): ConstrainedLayoutReference =
            createRefFor(ids.getOrElse(14) { nextId() })

        operator fun component16(): ConstrainedLayoutReference =
            createRefFor(ids.getOrElse(15) { nextId() })
    }

    /**
     * Declare a custom Float [value] addressed by [name].
     */
    fun ConstrainScope.customFloat(name: String, value: Float) {
        if (!containerObject.has("custom")) {
            containerObject.put("custom", CLObject(charArrayOf()))
        }
        val customPropsObject = containerObject.getObjectOrNull("custom") ?: return
        customPropsObject.putNumber(name, value)
    }

    /**
     * Declare a custom Color [value] addressed by [name].
     */
    fun ConstrainScope.customColor(name: String, value: Color) {
        if (!containerObject.has("custom")) {
            containerObject.put("custom", CLObject(charArrayOf()))
        }
        val customPropsObject = containerObject.getObjectOrNull("custom") ?: return
        customPropsObject.putString(name, value.toJsonHexString())
    }

    /**
     * Declare a custom Int [value] addressed by [name].
     */
    fun ConstrainScope.customInt(name: String, value: Int) {
        customFloat(name, value.toFloat())
    }

    /**
     * Declare a custom Dp [value] addressed by [name].
     */
    fun ConstrainScope.customDistance(name: String, value: Dp) {
        customFloat(name, value.value)
    }

    /**
     * Declare a custom TextUnit [value] addressed by [name].
     */
    fun ConstrainScope.customFontSize(name: String, value: TextUnit) {
        customFloat(name, value.value)
    }

    /**
     * Custom staggered weight. When set, MotionLayout will use these values instead of the default
     * way of calculating the weight, ignoring those with a `Float.NaN` value.
     *
     * &nbsp;
     *
     * The value is `Float.NaN` by default. Note that when all widgets are set to `Float.NaN`,
     * MotionLayout will use the default way of calculating the weight.
     *
     * @see TransitionScope.staggered
     */
    var ConstrainScope.staggeredWeight: Float
        get() {
            if (!this.containerObject.has("motion")) {
                return Float.NaN
            }
            val motionObject = this.containerObject.getObject("motion")
            return motionObject.getFloatOrNaN("stagger")
        }
        set(value) {
            with(this) {
                setMotionProperty("stagger", value)
            }
        }

    private fun ConstrainScope.setMotionProperty(name: String, value: Float) {
        if (!this.containerObject.has("motion")) {
            containerObject.put("motion", CLObject(charArrayOf()))
        }
        val motionPropsObject = containerObject.getObjectOrNull("motion") ?: return
        motionPropsObject.putNumber(name, value)
    }

    /**
     * Sets the custom Float [value] at the frame of the current [KeyAttributeScope].
     */
    fun KeyAttributeScope.customFloat(name: String, value: Float) {
        customPropertiesValue[name] = value
    }

    /**
     * Sets the custom Color [value] at the frame of the current [KeyAttributeScope].
     */
    fun KeyAttributeScope.customColor(name: String, value: Color) {
        // Colors must be in the following format: "#AARRGGBB"
        customPropertiesValue[name] = value.toJsonHexString()
    }

    /**
     * Sets the custom Int [value] at the frame of the current [KeyAttributeScope].
     */
    fun KeyAttributeScope.customInt(name: String, value: Int) {
        customPropertiesValue[name] = value
    }

    /**
     * Sets the custom Dp [value] at the frame of the current [KeyAttributeScope].
     */
    fun KeyAttributeScope.customDistance(name: String, value: Dp) {
        customPropertiesValue[name] = value.value
    }

    /**
     * Sets the custom TextUnit [value] at the frame of the current [KeyAttributeScope].
     */
    fun KeyAttributeScope.customFontSize(name: String, value: TextUnit) {
        customPropertiesValue[name] = value.value
    }

    private fun Color.toJsonHexString() =
        "#${this.toArgb().toUInt().toString(16)}"
}

data class ConstraintSetRef internal constructor(
    internal val name: String
)