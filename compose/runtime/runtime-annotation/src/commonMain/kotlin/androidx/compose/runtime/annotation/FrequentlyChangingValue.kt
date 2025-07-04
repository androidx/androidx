/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.runtime.annotation

/**
 * FrequentlyChangingValue is used to denote properties and functions that return values that change
 * frequently during runtime, and cause recompositions when they are read (for example, if they are
 * backed with snapshot state). Reading these values in composition can cause performance issues due
 * to frequent recompositions - for example reading a list's scroll position. An associated lint
 * check will warn for reads of annotated properties and functions inside composition.
 *
 * To avoid frequent recompositions, instead consider:
 * - Using derivedStateOf to filter state changes based on a provided calculation. For example,
 *   rather than recomposing on every scroll position change, only recomposing if the scroll
 *   position changes from 0 (at the top of the list) to greater than 0 (not at the top of the
 *   list), and vice versa.
 * - Using snapshotFlow to create a flow of changes from a provided state. This can then be
 *   collected inside a LaunchedEffect, and used to make changes without needing to recompose.
 * - When using Compose UI, read this value inside measure / layout / draw, depending on where it is
 *   needed. This will cause invalidation of the corresponding phase, instead of a recomposition.
 *   See developer.android.com for more information on Jetpack Compose phases.
 */
@MustBeDocumented
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.BINARY)
public annotation class FrequentlyChangingValue
