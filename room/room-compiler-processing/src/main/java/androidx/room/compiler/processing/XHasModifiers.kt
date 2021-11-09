/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.compiler.processing

/**
 * Common interface for elements which might have modifiers (e.g. field, method, class)
 */
interface XHasModifiers {
    /**
     * Returns `true` if this element is public (has public modifier in Java or not marked as
     * private / internal in Kotlin).
     */
    fun isPublic(): Boolean

    /**
     * Returns `true` if this element has protected modifier.
     */
    fun isProtected(): Boolean

    /**
     * Returns `true` if this element is declared as abstract.
     */
    fun isAbstract(): Boolean

    /**
     * Returns `true` if this element has private modifier.
     */
    fun isPrivate(): Boolean

    /**
     * Returns `true` if this element has static modifier.
     */
    fun isStatic(): Boolean

    /**
     * Returns `true` if this element has transient modifier.
     */
    fun isTransient(): Boolean

    /**
     * Returns `true` if this element is final and cannot be overridden.
     */
    fun isFinal(): Boolean
}
