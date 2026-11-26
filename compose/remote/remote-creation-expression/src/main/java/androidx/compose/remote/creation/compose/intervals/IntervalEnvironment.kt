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

package androidx.compose.remote.creation.compose.intervals

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.state.RemoteNamedCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateIdKey
import androidx.compose.remote.creation.compose.state.RemoteValue
import androidx.compose.remote.document.intervals.ColorInterval
import androidx.compose.remote.document.intervals.FloatInterval
import androidx.compose.remote.document.intervals.IntInterval
import androidx.compose.remote.document.intervals.IntervalComparison
import androidx.compose.remote.document.intervals.SlotInterval
import androidx.compose.remote.document.intervals.StringSummary

/**
 * Abstraction supplying static domain bounds for leaf [RemoteStateCacheKey]s (wire IDs, named
 * variables, array dereferences, and path-sensitive key overrides) to
 * [RemoteValueIntervalEvaluator].
 *
 * Standalone callers can use [MapIntervalEnvironment]; document-level analyzers can implement this
 * interface directly over their document slot tables without duplicating expression evaluation
 * logic.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface IntervalEnvironment {
    /** Returns the static float interval for a wire variable/context [id]. */
    public fun floatForStateId(id: Int): FloatInterval =
        SlotInterval.systemGlobalOrNull(id)?.toFloatInterval() ?: FloatInterval.UNBOUNDED

    /** Returns the static integer interval for a wire variable/context [id]. */
    public fun intForStateId(id: Int): IntInterval =
        SlotInterval.systemGlobalOrNull(id)?.toIntInterval() ?: IntInterval.UNBOUNDED

    /** Returns the static string summary for a wire string variable [id]. */
    public fun stringForStateId(id: Int): StringSummary = StringSummary.UNKNOWN

    /** Returns the static color interval for a wire color variable [id]. */
    public fun colorForStateId(id: Int): ColorInterval = ColorInterval.UNKNOWN

    /** Returns the static float interval for a named variable [key]. */
    public fun floatForNamed(key: RemoteNamedCacheKey): FloatInterval = FloatInterval.UNBOUNDED

    /** Returns the static integer interval for a named variable [key]. */
    public fun intForNamed(key: RemoteNamedCacheKey): IntInterval = IntInterval.UNBOUNDED

    /** Returns the static string summary for a named variable [key]. */
    public fun stringForNamed(key: RemoteNamedCacheKey): StringSummary = StringSummary.UNKNOWN

    /** Returns the static color interval for a named variable [key]. */
    public fun colorForNamed(key: RemoteNamedCacheKey): ColorInterval = ColorInterval.UNKNOWN

    /** Returns the static float interval for indexing wire float array [arrayId] at [index]. */
    public fun floatArrayElement(arrayId: Int, index: FloatInterval): FloatInterval =
        FloatInterval.UNBOUNDED

    /**
     * Returns `true` if a [RemoteStateIdKey] referencing [id] is eligible for point-constant
     * folding (e.g., `false` if [id] belongs to an output namespace or carries an animation spec).
     */
    public fun canFoldStateId(id: Int): Boolean = true

    /** Returns any path-sensitive float override installed for [key], or `null` if none. */
    public fun floatOverride(key: RemoteStateCacheKey): FloatInterval? = null

    /** Returns any path-sensitive integer override installed for [key], or `null` if none. */
    public fun intOverride(key: RemoteStateCacheKey): IntInterval? = null

    /**
     * Returns a derived [IntervalEnvironment] in which [key] is narrowed to [interval] (intersected
     * with any existing float bound or override on [key]).
     */
    public fun withFloatNarrowing(
        key: RemoteStateCacheKey,
        interval: FloatInterval,
    ): IntervalEnvironment {
        val existing = existingFloatBoundOrNull(key)
        val narrowed = if (existing != null) existing.intersect(interval) else interval
        if (narrowed.isEmpty || narrowed == existing) return this
        return OverlaidIntervalEnvironment(this, key, floatInterval = narrowed)
    }

    /**
     * Returns a derived [IntervalEnvironment] in which [key] is narrowed to [interval] (intersected
     * with any existing integer bound or override on [key]).
     */
    public fun withIntNarrowing(
        key: RemoteStateCacheKey,
        interval: IntInterval,
    ): IntervalEnvironment {
        val existing = existingIntBoundOrNull(key)
        val narrowed = if (existing != null) existing.intersect(interval) else interval
        if (narrowed.isEmpty || narrowed == existing) return this
        return OverlaidIntervalEnvironment(this, key, intInterval = narrowed)
    }

    /**
     * Returns a derived [IntervalEnvironment] refined by assuming the boolean expression [key]
     * evaluates to [holds].
     */
    public fun refine(key: RemoteStateCacheKey, holds: Boolean): IntervalEnvironment =
        refineEnvironmentByBooleanKey(this, key, holds)

    /**
     * Returns a derived [IntervalEnvironment] refined by assuming `lhs <comparison> rhs` holds in
     * either the integer ([asInt] = `true`) or float ([asInt] = `false`) domain.
     */
    public fun narrowComparison(
        comparison: IntervalComparison,
        lhs: RemoteStateCacheKey,
        rhs: RemoteStateCacheKey,
        asInt: Boolean = false,
    ): IntervalEnvironment =
        if (asInt) {
            narrowIntOperands(this, comparison, lhs, rhs)
        } else {
            narrowFloatOperands(this, comparison, lhs, rhs)
        }
}

internal fun IntervalEnvironment.existingFloatBoundOrNull(
    key: RemoteStateCacheKey
): FloatInterval? =
    floatOverride(key)
        ?: when (key) {
            is RemoteNamedCacheKey -> floatForNamed(key)
            is RemoteStateIdKey -> floatForStateId(key.id)
            else -> null
        }

internal fun IntervalEnvironment.existingIntBoundOrNull(key: RemoteStateCacheKey): IntInterval? =
    intOverride(key)
        ?: when (key) {
            is RemoteNamedCacheKey -> intForNamed(key)
            is RemoteStateIdKey -> intForStateId(key.id)
            else -> null
        }

/**
 * Immutable map-backed [IntervalEnvironment] for standalone expression interval analysis and
 * optimization without requiring a full document snapshot.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MapIntervalEnvironment
@JvmOverloads
constructor(
    public val canvasWidth: Float? = null,
    public val canvasHeight: Float? = null,
    private val stateIdFloats: Map<Int, FloatInterval> = emptyMap(),
    private val stateIdInts: Map<Int, IntInterval> = emptyMap(),
    private val stateIdStrings: Map<Int, StringSummary> = emptyMap(),
    private val stateIdColors: Map<Int, ColorInterval> = emptyMap(),
    private val namedFloats: Map<String, FloatInterval> = emptyMap(),
    private val namedInts: Map<String, IntInterval> = emptyMap(),
    private val namedStrings: Map<String, StringSummary> = emptyMap(),
    private val namedColors: Map<String, ColorInterval> = emptyMap(),
    private val keyFloatOverrides: Map<RemoteStateCacheKey, FloatInterval> = emptyMap(),
    private val keyIntOverrides: Map<RemoteStateCacheKey, IntInterval> = emptyMap(),
    private val nonFoldableStateIds: Set<Int> = emptySet(),
) : IntervalEnvironment {

    override fun floatForStateId(id: Int): FloatInterval =
        stateIdFloats[id]
            ?: stateIdInts[id]?.toFloatInterval()
            ?: SlotInterval.systemGlobalOrNull(id, canvasWidth, canvasHeight)?.toFloatInterval()
            ?: FloatInterval.UNBOUNDED

    override fun intForStateId(id: Int): IntInterval =
        stateIdInts[id]
            ?: stateIdFloats[id]?.let { SlotInterval.fromFloat(it).toIntInterval() }
            ?: SlotInterval.systemGlobalOrNull(id, canvasWidth, canvasHeight)?.toIntInterval()
            ?: IntInterval.UNBOUNDED

    override fun stringForStateId(id: Int): StringSummary =
        stateIdStrings[id] ?: StringSummary.UNKNOWN

    override fun colorForStateId(id: Int): ColorInterval =
        stateIdColors[id] ?: ColorInterval.UNKNOWN

    override fun floatForNamed(key: RemoteNamedCacheKey): FloatInterval {
        val wireName = key.wireName
        return namedFloats[wireName]
            ?: namedInts[wireName]?.toFloatInterval()
            ?: FloatInterval.UNBOUNDED
    }

    override fun intForNamed(key: RemoteNamedCacheKey): IntInterval {
        val wireName = key.wireName
        return namedInts[wireName]
            ?: namedFloats[wireName]?.let { SlotInterval.fromFloat(it).toIntInterval() }
            ?: IntInterval.UNBOUNDED
    }

    override fun stringForNamed(key: RemoteNamedCacheKey): StringSummary =
        namedStrings[key.wireName] ?: StringSummary.UNKNOWN

    override fun colorForNamed(key: RemoteNamedCacheKey): ColorInterval =
        namedColors[key.wireName] ?: ColorInterval.UNKNOWN

    override fun canFoldStateId(id: Int): Boolean = id !in nonFoldableStateIds

    override fun floatOverride(key: RemoteStateCacheKey): FloatInterval? = keyFloatOverrides[key]

    override fun intOverride(key: RemoteStateCacheKey): IntInterval? = keyIntOverrides[key]

    override fun withFloatNarrowing(
        key: RemoteStateCacheKey,
        interval: FloatInterval,
    ): MapIntervalEnvironment {
        val existing = existingFloatBoundOrNull(key)
        val narrowed = if (existing != null) existing.intersect(interval) else interval
        if (narrowed.isEmpty || narrowed == existing) return this
        val updated = LinkedHashMap(keyFloatOverrides)
        updated[key] = narrowed
        return copy(keyFloatOverrides = updated)
    }

    override fun withIntNarrowing(
        key: RemoteStateCacheKey,
        interval: IntInterval,
    ): MapIntervalEnvironment {
        val existing = existingIntBoundOrNull(key)
        val narrowed = if (existing != null) existing.intersect(interval) else interval
        if (narrowed.isEmpty || narrowed == existing) return this
        val updated = LinkedHashMap(keyIntOverrides)
        updated[key] = narrowed
        return copy(keyIntOverrides = updated)
    }

    /** Returns a copy binding named float variable `domain.prefixed(name)` to [interval]. */
    @JvmOverloads
    public fun withNamedFloat(
        name: String,
        interval: FloatInterval,
        domain: RemoteValue.Domain = RemoteValue.Domain.User,
    ): MapIntervalEnvironment {
        val updated = LinkedHashMap(namedFloats)
        updated[domain.prefixed(name)] = interval
        return copy(namedFloats = updated)
    }

    /** Returns a copy binding named integer variable `domain.prefixed(name)` to [interval]. */
    @JvmOverloads
    public fun withNamedInt(
        name: String,
        interval: IntInterval,
        domain: RemoteValue.Domain = RemoteValue.Domain.User,
    ): MapIntervalEnvironment {
        val updated = LinkedHashMap(namedInts)
        updated[domain.prefixed(name)] = interval
        return copy(namedInts = updated)
    }

    /** Returns a copy binding named string variable `domain.prefixed(name)` to [summary]. */
    @JvmOverloads
    public fun withNamedString(
        name: String,
        summary: StringSummary,
        domain: RemoteValue.Domain = RemoteValue.Domain.User,
    ): MapIntervalEnvironment {
        val updated = LinkedHashMap(namedStrings)
        updated[domain.prefixed(name)] = summary
        return copy(namedStrings = updated)
    }

    /** Returns a copy binding named color variable `domain.prefixed(name)` to [interval]. */
    @JvmOverloads
    public fun withNamedColor(
        name: String,
        interval: ColorInterval,
        domain: RemoteValue.Domain = RemoteValue.Domain.User,
    ): MapIntervalEnvironment {
        val updated = LinkedHashMap(namedColors)
        updated[domain.prefixed(name)] = interval
        return copy(namedColors = updated)
    }

    /** Returns a copy binding wire variable [id] to float [interval]. */
    public fun withStateIdFloat(id: Int, interval: FloatInterval): MapIntervalEnvironment {
        val updated = LinkedHashMap(stateIdFloats)
        updated[id] = interval
        return copy(stateIdFloats = updated)
    }

    /** Returns a copy binding wire variable [id] to integer [interval]. */
    public fun withStateIdInt(id: Int, interval: IntInterval): MapIntervalEnvironment {
        val updated = LinkedHashMap(stateIdInts)
        updated[id] = interval
        return copy(stateIdInts = updated)
    }

    /** Returns a copy marking wire variable [id] as ineligible for constant folding. */
    public fun withNonFoldableStateId(id: Int): MapIntervalEnvironment {
        val updated = LinkedHashSet(nonFoldableStateIds)
        updated.add(id)
        return copy(nonFoldableStateIds = updated)
    }

    private fun copy(
        stateIdFloats: Map<Int, FloatInterval> = this.stateIdFloats,
        stateIdInts: Map<Int, IntInterval> = this.stateIdInts,
        stateIdStrings: Map<Int, StringSummary> = this.stateIdStrings,
        stateIdColors: Map<Int, ColorInterval> = this.stateIdColors,
        namedFloats: Map<String, FloatInterval> = this.namedFloats,
        namedInts: Map<String, IntInterval> = this.namedInts,
        namedStrings: Map<String, StringSummary> = this.namedStrings,
        namedColors: Map<String, ColorInterval> = this.namedColors,
        keyFloatOverrides: Map<RemoteStateCacheKey, FloatInterval> = this.keyFloatOverrides,
        keyIntOverrides: Map<RemoteStateCacheKey, IntInterval> = this.keyIntOverrides,
        nonFoldableStateIds: Set<Int> = this.nonFoldableStateIds,
    ): MapIntervalEnvironment =
        MapIntervalEnvironment(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            stateIdFloats = stateIdFloats,
            stateIdInts = stateIdInts,
            stateIdStrings = stateIdStrings,
            stateIdColors = stateIdColors,
            namedFloats = namedFloats,
            namedInts = namedInts,
            namedStrings = namedStrings,
            namedColors = namedColors,
            keyFloatOverrides = keyFloatOverrides,
            keyIntOverrides = keyIntOverrides,
            nonFoldableStateIds = nonFoldableStateIds,
        )

    public companion object {
        /** Empty environment providing only built-in system global bounds. */
        public val EMPTY: MapIntervalEnvironment = MapIntervalEnvironment()
    }
}

private class OverlaidIntervalEnvironment(
    private val base: IntervalEnvironment,
    private val key: RemoteStateCacheKey,
    private val floatInterval: FloatInterval? = null,
    private val intInterval: IntInterval? = null,
) : IntervalEnvironment {
    override fun floatForStateId(id: Int): FloatInterval = base.floatForStateId(id)

    override fun intForStateId(id: Int): IntInterval = base.intForStateId(id)

    override fun stringForStateId(id: Int): StringSummary = base.stringForStateId(id)

    override fun colorForStateId(id: Int): ColorInterval = base.colorForStateId(id)

    override fun floatForNamed(key: RemoteNamedCacheKey): FloatInterval = base.floatForNamed(key)

    override fun intForNamed(key: RemoteNamedCacheKey): IntInterval = base.intForNamed(key)

    override fun stringForNamed(key: RemoteNamedCacheKey): StringSummary = base.stringForNamed(key)

    override fun colorForNamed(key: RemoteNamedCacheKey): ColorInterval = base.colorForNamed(key)

    override fun floatArrayElement(arrayId: Int, index: FloatInterval): FloatInterval =
        base.floatArrayElement(arrayId, index)

    override fun canFoldStateId(id: Int): Boolean = base.canFoldStateId(id)

    override fun floatOverride(key: RemoteStateCacheKey): FloatInterval? {
        if (key != this.key || floatInterval == null) return base.floatOverride(key)
        val baseBound = base.existingFloatBoundOrNull(key)
        return if (baseBound != null) {
            baseBound.intersect(floatInterval).takeIf { !it.isEmpty } ?: baseBound
        } else {
            floatInterval
        }
    }

    override fun intOverride(key: RemoteStateCacheKey): IntInterval? {
        if (key != this.key || intInterval == null) return base.intOverride(key)
        val baseBound = base.existingIntBoundOrNull(key)
        return if (baseBound != null) {
            baseBound.intersect(intInterval).takeIf { !it.isEmpty } ?: baseBound
        } else {
            intInterval
        }
    }
}
