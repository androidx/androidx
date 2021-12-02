/*
 * Copyright 2018 The Android Open Source Project
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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.preference

/**
 * Returns the preference with `key`, or `null` if no preference with `key` is found.
 */
public inline operator fun <T : Preference> PreferenceGroup.get(key: CharSequence): T? =
    findPreference(key)

/**
 * Returns the preference at `index`.
 *
 * @throws IndexOutOfBoundsException if index is less than 0 or greater than or equal to the count.
 */
public operator fun PreferenceGroup.get(index: Int): Preference = getPreference(index)

/** Returns `true` if `preference` is found in this preference group. */
public operator fun PreferenceGroup.contains(preference: Preference): Boolean {
    for (index in 0 until preferenceCount) {
        if (getPreference(index) == preference) {
            return true
        }
    }
    return false
}

/** Adds `preference` to this preference group. */
public inline operator fun PreferenceGroup.plusAssign(preference: Preference) {
    addPreference(preference)
}

/** Removes `preference` from this preference group. */
public inline operator fun PreferenceGroup.minusAssign(preference: Preference) {
    removePreference(preference)
}

/** Returns the number of preferences in this preference group. */
public inline val PreferenceGroup.size: Int get() = preferenceCount

/** Returns true if this preference group contains no preferences. */
public inline fun PreferenceGroup.isEmpty(): Boolean = size == 0

/** Returns true if this preference group contains one or more preferences. */
public inline fun PreferenceGroup.isNotEmpty(): Boolean = size != 0

/** Performs the given action on each preference in this preference group. */
public inline fun PreferenceGroup.forEach(action: (preference: Preference) -> Unit) {
    for (index in 0 until preferenceCount) {
        action(get(index))
    }
}

/**
 * Performs the given action on each preference in this preference group,
 * providing its sequential index.
 */
public inline fun PreferenceGroup.forEachIndexed(
    action: (index: Int, preference: Preference) -> Unit
) {
    for (index in 0 until preferenceCount) {
        action(index, get(index))
    }
}

/** Returns a [MutableIterator] over the preferences in this preference group. */
public operator fun PreferenceGroup.iterator(): Iterator<Preference> =
    object : MutableIterator<Preference> {
        private var index = 0
        override fun hasNext() = index < size
        override fun next() = getPreference(index++)
        override fun remove() {
            removePreference(getPreference(--index))
        }
    }

/** Returns a [Sequence] over the preferences in this preference group. */
public val PreferenceGroup.children: Sequence<Preference>
    get() = object : Sequence<Preference> {
        override fun iterator() = this@children.iterator()
    }
