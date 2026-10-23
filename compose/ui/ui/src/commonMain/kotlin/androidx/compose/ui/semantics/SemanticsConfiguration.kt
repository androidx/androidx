/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.semantics

import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet
import androidx.collection.mutableScatterMapOf
import androidx.collection.mutableScatterSetOf
import androidx.compose.ui.platform.ExperimentalComposeUiHostApi
import androidx.compose.ui.platform.simpleIdentityToString

/**
 * Read-only view of semantics properties.
 *
 * [SemanticsConfiguration] implements this interface. [SemanticsNode.unmergedConfig] and
 * [SemanticsNode.mergedConfig] return it so that hosts can read semantics without mutating them.
 */
@ExperimentalComposeUiHostApi
public interface SemanticsPropertyProvider {

    /**
     * Returns the value for [key], or throws [IllegalStateException] if not set.
     *
     * @param key Property key to retrieve.
     * @return Value associated with [key].
     * @throws IllegalStateException if [key] is not present.
     */
    public operator fun <T> get(key: SemanticsPropertyKey<T>): T

    /**
     * Returns the value for [key], or runs [defaultValue] if not set.
     *
     * @param key Property key to retrieve.
     * @param defaultValue Fallback supplier run when [key] is not present.
     * @return Value associated with [key], or the result of [defaultValue].
     */
    public fun <T> getOrElse(key: SemanticsPropertyKey<T>, defaultValue: () -> T): T

    /**
     * Returns the value for [key], or runs [defaultValue] if not set.
     *
     * @param key Property key to retrieve.
     * @param defaultValue Fallback supplier run when [key] is not present.
     * @return Value associated with [key], or the result of [defaultValue].
     */
    public fun <T> getOrElseNullable(key: SemanticsPropertyKey<T>, defaultValue: () -> T?): T?

    /**
     * Returns whether [key] is present in this provider.
     *
     * @param key Property key to check.
     * @return `true` if [key] is present, `false` otherwise.
     */
    public operator fun <T> contains(key: SemanticsPropertyKey<T>): Boolean

    /**
     * Keys in this provider whose [SemanticsPropertyKey.accessibilityExtraKey] is non-null.
     *
     * Hosts expose these values as accessibility extras. Returns an empty set if there are none.
     */
    public val accessibilityExtraKeys: Set<SemanticsPropertyKey<*>>

    /**
     * Whether this configuration declares that it merges the semantics of its descendants
     * (`Modifier.semantics(mergeDescendants = true)`).
     *
     * This is the declared value only. [SemanticsNode.isMergingSemanticsOfDescendants] is `true`
     * only when this value is `true` **and** the node was created with
     * [SemanticsNode.mergingEnabled].
     */
    public val isMergingSemanticsOfDescendants: Boolean

    /**
     * Invokes [action] for each property key and value in this provider.
     *
     * @param action Callback invoked with each [SemanticsPropertyKey] and its associated value.
     */
    public fun forEachProperty(action: (key: SemanticsPropertyKey<*>, value: Any?) -> Unit)

    /**
     * Returns `true` if at least one property in this provider matches [predicate]. Stops at the
     * first match.
     *
     * @param predicate Predicate invoked with each [SemanticsPropertyKey] and its associated value.
     */
    public fun anyProperty(
        predicate: (key: SemanticsPropertyKey<*>, value: Any?) -> Boolean
    ): Boolean
}

/**
 * Describes the semantic information associated with the owning component
 *
 * The information provided in the configuration is used to to generate the semantics tree.
 */
@OptIn(ExperimentalComposeUiHostApi::class)
public class SemanticsConfiguration :
    SemanticsPropertyProvider,
    SemanticsPropertyReceiver,
    Iterable<Map.Entry<SemanticsPropertyKey<*>, Any?>> {

    internal val props: MutableScatterMap<SemanticsPropertyKey<*>, Any?> = mutableScatterMapOf()
    private var mapWrapper: Map<SemanticsPropertyKey<*>, Any?>? = null

    internal var _accessibilityExtraKeys: MutableScatterSet<SemanticsPropertyKey<*>>? = null
    private var setWrapper: Set<SemanticsPropertyKey<*>>? = null

    @ExperimentalComposeUiHostApi
    override val accessibilityExtraKeys: Set<SemanticsPropertyKey<*>>
        get() {
            val extraKeys = _accessibilityExtraKeys ?: return emptySet()
            @Suppress("AsCollectionCall")
            return setWrapper ?: extraKeys.asSet().also { setWrapper = it }
        }

    /**
     * Retrieves the value for the given property, if one has been set. If a value has not been set,
     * throws [IllegalStateException]
     */
    // Unavoidable, guaranteed by [set]
    @Suppress("UNCHECKED_CAST")
    override public operator fun <T> get(key: SemanticsPropertyKey<T>): T {
        return props.getOrElse(key) {
            throw IllegalStateException("Key not present: $key - consider getOrElse or getOrNull")
        } as T
    }

    // Unavoidable, guaranteed by [set]
    @Suppress("UNCHECKED_CAST")
    override public fun <T> getOrElse(key: SemanticsPropertyKey<T>, defaultValue: () -> T): T {
        return props.getOrElse(key, defaultValue) as T
    }

    // Unavoidable, guaranteed by [set]
    @Suppress("UNCHECKED_CAST")
    override public fun <T> getOrElseNullable(
        key: SemanticsPropertyKey<T>,
        defaultValue: () -> T?,
    ): T? {
        return props.getOrElse(key, defaultValue) as T?
    }

    public override fun iterator(): Iterator<Map.Entry<SemanticsPropertyKey<*>, Any?>> {
        @Suppress("AsCollectionCall")
        val mapWrapper = mapWrapper ?: props.asMap().apply { mapWrapper = this }
        return mapWrapper.iterator()
    }

    public override fun <T> set(key: SemanticsPropertyKey<T>, value: T) {
        if (value is AccessibilityAction<*> && contains(key)) {
            val prev = props[key] as AccessibilityAction<*>
            props[key] = AccessibilityAction(value.label ?: prev.label, value.action ?: prev.action)
        } else {
            props[key] = value
        }

        addAccessibilityExtraKey(key)
    }

    private fun addAccessibilityExtraKey(key: SemanticsPropertyKey<*>) {
        if (key.accessibilityExtraKey != null) {
            val extraKeys =
                _accessibilityExtraKeys
                    ?: mutableScatterSetOf<SemanticsPropertyKey<*>>().also {
                        _accessibilityExtraKeys = it
                    }
            extraKeys.add(key)
        }
    }

    /** Invokes [block] for each key in [accessibilityExtraKeys] without allocating. */
    internal inline fun forEachAccessibilityExtraKey(block: (SemanticsPropertyKey<*>) -> Unit) {
        _accessibilityExtraKeys?.forEach(block)
    }

    override public operator fun <T> contains(key: SemanticsPropertyKey<T>): Boolean {
        return props.containsKey(key)
    }

    internal fun containsImportantForAccessibility() = props.any { key, _ ->
        key.isImportantForAccessibility
    }

    /**
     * Whether the semantic information provided by the owning component and all of its descendants
     * should be treated as one logical entity.
     *
     * If set to true, the descendants of the owning component's [SemanticsNode] will merge their
     * semantic information into the [SemanticsNode] representing the owning component.
     */
    override public var isMergingSemanticsOfDescendants: Boolean = false
    public var isClearingSemantics: Boolean = false

    @ExperimentalComposeUiHostApi
    override fun forEachProperty(action: (key: SemanticsPropertyKey<*>, value: Any?) -> Unit) {
        props.forEach(action)
    }

    @ExperimentalComposeUiHostApi
    override fun anyProperty(
        predicate: (key: SemanticsPropertyKey<*>, value: Any?) -> Boolean
    ): Boolean {
        return props.any(predicate)
    }

    // CONFIGURATION COMBINATION LOGIC

    /**
     * Absorb the semantic information from a child SemanticsNode into this configuration.
     *
     * This merges the child's semantic configuration using the `merge()` method defined on the key.
     * This is used when mergeDescendants is specified (for accessibility focusable nodes).
     */
    @Suppress("UNCHECKED_CAST")
    internal fun mergeChild(child: SemanticsConfiguration) {
        child.props.forEach { key, nextValue ->
            val existingValue = props[key]
            val mergeResult = (key as SemanticsPropertyKey<Any?>).merge(existingValue, nextValue)
            if (mergeResult != null) {
                props[key] = mergeResult
                addAccessibilityExtraKey(key)
            }
        }
    }

    /**
     * Absorb the semantic information from a peer modifier into this configuration.
     *
     * This is repeatedly called for each semantics {} modifier on one LayoutNode to collapse them
     * into one SemanticsConfiguration. If a key is already seen and the value is
     * AccessibilityAction, the resulting AccessibilityAction's label/action will be the
     * label/action of the outermost modifier with this key and nonnull label/action, or null if no
     * nonnull label/action is found. If the value is not AccessibilityAction, values with a key
     * already seen are ignored (the semantics value of the outermost modifier with a given
     * semantics key is the one used).
     */
    internal fun collapsePeer(peer: SemanticsConfiguration) {
        if (peer.isMergingSemanticsOfDescendants) {
            isMergingSemanticsOfDescendants = true
        }
        if (peer.isClearingSemantics) {
            isClearingSemantics = true
        }
        peer.props.forEach { key, nextValue ->
            if (!props.contains(key)) {
                props[key] = nextValue
                addAccessibilityExtraKey(key)
            } else if (nextValue is AccessibilityAction<*>) {
                val value = props[key] as AccessibilityAction<*>
                props[key] =
                    AccessibilityAction(
                        value.label ?: nextValue.label,
                        value.action ?: nextValue.action,
                    )
            }
        }
    }

    /** Returns an exact copy of this configuration. */
    public fun copy(): SemanticsConfiguration {
        val copy = SemanticsConfiguration()
        copy.isMergingSemanticsOfDescendants = isMergingSemanticsOfDescendants
        copy.isClearingSemantics = isClearingSemantics
        copy.props.putAll(props)
        _accessibilityExtraKeys?.let {
            copy._accessibilityExtraKeys =
                MutableScatterSet<SemanticsPropertyKey<*>>(it.size).apply { addAll(it) }
        }
        return copy
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SemanticsConfiguration) return false

        if (props != other.props) return false
        if (isMergingSemanticsOfDescendants != other.isMergingSemanticsOfDescendants) return false
        if (isClearingSemantics != other.isClearingSemantics) return false

        return true
    }

    override fun hashCode(): Int {
        var result = props.hashCode()
        result = 31 * result + isMergingSemanticsOfDescendants.hashCode()
        result = 31 * result + isClearingSemantics.hashCode()
        return result
    }

    override fun toString(): String {
        val propsString = StringBuilder()
        var nextSeparator = ""

        if (isMergingSemanticsOfDescendants) {
            propsString.append(nextSeparator)
            propsString.append("mergeDescendants=true")
            nextSeparator = ", "
        }

        if (isClearingSemantics) {
            propsString.append(nextSeparator)
            propsString.append("isClearingSemantics=true")
            nextSeparator = ", "
        }

        props.forEach { key, value ->
            propsString.append(nextSeparator)
            propsString.append(key.name)
            propsString.append(" : ")
            propsString.append(value)
            nextSeparator = ", "
        }
        return "${simpleIdentityToString(this@SemanticsConfiguration, null)}{ $propsString }"
    }
}

private val NullDefault: () -> Nothing? = { null }

/**
 * Returns the value for [key], or `null` if not present.
 *
 * @param key Property key to retrieve.
 * @return Value associated with [key], or `null` if not present.
 */
@ExperimentalComposeUiHostApi
public fun <T> SemanticsPropertyProvider.getOrNull(key: SemanticsPropertyKey<T>): T? {
    return getOrElseNullable(key, NullDefault)
}

/** Retrieves the value for the given property, or `null` if the property is not present. */
@Suppress("UNCHECKED_CAST")
public fun <T> SemanticsConfiguration.getOrNull(key: SemanticsPropertyKey<T>): T? {
    return props.getOrElse(key) { null } as T?
}

private val IsImportantForAccessibilityPredicate: (SemanticsPropertyKey<*>, Any?) -> Boolean =
    { key, _ ->
        key.isImportantForAccessibility
    }

/**
 * Returns `true` if this provider contains at least one key that [other] does not contain.
 *
 * Use this to detect deleted properties between an old and a new configuration of the same node:
 * `oldConfig.hasKeysMissingFrom(newConfig)`. Values are not compared.
 *
 * @param other The provider to check keys against.
 */
@ExperimentalComposeUiHostApi
public fun SemanticsPropertyProvider.hasKeysMissingFrom(other: SemanticsPropertyProvider): Boolean {
    if (this is SemanticsConfiguration && other is SemanticsConfiguration) {
        val otherProps = other.props
        return props.any { key, _ -> !otherProps.containsKey(key) }
    }
    return anyProperty { key, _ -> key !in other }
}

@OptIn(ExperimentalComposeUiHostApi::class)
internal fun SemanticsPropertyProvider.containsImportantForAccessibility(): Boolean {
    return anyProperty(IsImportantForAccessibilityPredicate)
}
