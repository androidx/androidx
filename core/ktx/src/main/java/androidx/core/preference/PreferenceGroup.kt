/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("NOTHING_TO_INLINE", "DeprecatedCallableAddReplaceWith", "DEPRECATION")

package androidx.core.preference

import android.preference.Preference
import android.preference.PreferenceGroup

/**
 * Returns the preference with `key`.
 *
 * @throws NullPointerException if no preference is found with that key.
 */
@Deprecated("Use Jetpack preference library", level = DeprecationLevel.ERROR)
inline operator fun PreferenceGroup.get(key: CharSequence): Preference = findPreference(key)

/**
 * Returns the preference at `index`.
 *
 * @throws IndexOutOfBoundsException if index is less than 0 or greater than or equal to the count.
 */
@Deprecated("Use Jetpack preference library", level = DeprecationLevel.ERROR)
operator fun PreferenceGroup.get(index: Int): Preference = getPreference(index)
        ?: throw IndexOutOfBoundsException("Index: $index, Size: $preferenceCount")

/** Returns `true` if `preference` is found in this preference group. */
@Deprecated("Use Jetpack preference library", level = DeprecationLevel.ERROR)
operator fun PreferenceGroup.contains(preference: Preference): Boolean {
    for (index in 0 until preferenceCount) {
        if (getPreference(index) == preference) {
            return true
        }
    }
    return false
}

/** Adds `preference` to this preference group. */
@Deprecated("Use Jetpack preference library", level = DeprecationLevel.ERROR)
inline operator fun PreferenceGroup.plusAssign(preference: Preference) {
    addPreference(preference)
}

/** Removes `preference` from this preference group. */
@Deprecated("Use Jetpack preference library", level = DeprecationLevel.ERROR)
inline operator fun PreferenceGroup.minusAssign(preference: Preference) {
    removePreference(preference)
}

/** Returns the number of preferences in this preference group. */
@Deprecated("Use Jetpack preference library", level = DeprecationLevel.ERROR)
inline val PreferenceGroup.size: Int get() = preferenceCount

/** Returns true if this preference group contains no preferences. */
@Deprecated("Use Jetpack preference library", level = DeprecationLevel.ERROR)
inline fun PreferenceGroup.isEmpty(): Boolean = preferenceCount == 0

/** Returns true if this preference group contains one or more preferences. */
@Deprecated("Use Jetpack preference library", level = DeprecationLevel.ERROR)
inline fun PreferenceGroup.isNotEmpty(): Boolean = preferenceCount != 0

/** Performs the given action on each preference in this preference group. */
@Deprecated("Use Jetpack preference library", level = DeprecationLevel.ERROR)
inline fun PreferenceGroup.forEach(action: (preference: Preference) -> Unit) {
    for (index in 0 until preferenceCount) {
        action(getPreference(index))
    }
}

/** Performs the given action on each preference in this preference group, providing its sequential index. */
@Deprecated("Use Jetpack preference library", level = DeprecationLevel.ERROR)
inline fun PreferenceGroup.forEachIndexed(action: (index: Int, preference: Preference) -> Unit) {
    for (index in 0 until preferenceCount) {
        action(index, getPreference(index))
    }
}

/** Returns a [MutableIterator] over the preferences in this preference group. */
@Deprecated("Use Jetpack preference library", level = DeprecationLevel.ERROR)
operator fun PreferenceGroup.iterator(): Iterator<Preference> = PreferenceIterator(this)

/** Returns a [Sequence] over the preferences in this preference group. */
@Deprecated("Use Jetpack preference library", level = DeprecationLevel.ERROR)
val PreferenceGroup.children: Sequence<Preference>
    get() = object : Sequence<Preference> {
        override fun iterator() = PreferenceIterator(this@children)
    }

private class PreferenceIterator(val group: PreferenceGroup) : MutableIterator<Preference> {
    private var index = 0
    override fun hasNext() = index < group.preferenceCount
    override fun next() = group.getPreference(index++) ?: throw IndexOutOfBoundsException()
    override fun remove() {
        group.removePreference(group.getPreference(--index))
    }
}
