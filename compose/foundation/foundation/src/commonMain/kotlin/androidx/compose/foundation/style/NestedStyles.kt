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

package androidx.compose.foundation.style

/**
 * A key used to identify a style that is being defined by a parent style or applied by a nested
 * styleable composable.
 *
 * @param label diagnostic label for debugging; not used for key identity
 * @see NestedStyleProviderStyleScope.provideNestedStyle
 */
@ExperimentalFoundationStyleApi
public class NestedStyleKey(private val label: String) {
    override fun toString(): String =
        if (label.isNotEmpty()) "${super.toString()}(label = $label)" else super.toString()
}

/**
 * A scope that allows providing a nested style that will be read and used by a nested styleable
 * composable function.
 *
 * [NestedStyleProviderStyleScope] is implemented by [CommonStyleScope], but a [CustomStyle] may
 * choose to prevent arbitrary keys from being provided by defining a style scope that excludes this
 * interface.
 */
@ExperimentalFoundationStyleApi
public interface NestedStyleProviderStyleScope {
    /**
     * Provides [style] as a nested style to the nested composable identified by the provided [key].
     * The [style] is intended to be applied as the style for a nested styleable composable using
     * [applyNestedStyle][NestedStyleConsumerStyleScope.applyNestedStyle].
     *
     * @param key the key that identifies which nested composable this style is applied to
     * @param style the style to be applied to the nested composable
     * @see NestedStyleConsumerStyleScope.applyNestedStyle
     */
    public fun provideNestedStyle(key: NestedStyleKey, style: CommonStyle)
}

/** A scope that allows applying a style defined in a parent style. */
@ExperimentalFoundationStyleApi
public interface NestedStyleConsumerStyleScope {
    /**
     * Applies a style with the given [key] defined in a parent style. If no style is provided by
     * the parent for the given [key], calling this is equivalent to an empty style.
     *
     * @param key the key used to look up the style to apply
     * @see NestedStyleProviderStyleScope.provideNestedStyle
     */
    public fun applyNestedStyle(key: NestedStyleKey)
}
