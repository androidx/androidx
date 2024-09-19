package androidx.glance

import androidx.annotation.RestrictTo

/*
 * Copyright 2021 The Android Open Source Project
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <reified T> GlanceModifier.findModifier(): T? =
    this.foldIn<T?>(null) { acc, cur ->
        if (cur is T) {
            cur
        } else {
            acc
        }
    }

/**
 * Find the last modifier of the given type, and create a new [GlanceModifier] which is equivalent
 * with the previous one, but without any modifiers of specified type.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <reified T> GlanceModifier.extractModifier(): Pair<T?, GlanceModifier> =
    if (any { it is T }) {
        foldIn<Pair<T?, GlanceModifier>>(null to GlanceModifier) { acc, cur ->
            if (cur is T) {
                cur to acc.second
            } else {
                acc.first to acc.second.then(cur)
            }
        }
    } else {
        null to this
    }

/** Returns a GlanceModifier with all elements of type [T] removed. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <reified T> GlanceModifier.removeModifiersOfType(): GlanceModifier where
T : GlanceModifier.Element =
    foldIn(GlanceModifier) { acc: GlanceModifier, cur: GlanceModifier ->
        cur.takeUnless { it is T }?.let { acc.then(cur) } ?: acc
    }
