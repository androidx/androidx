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

package androidx.compose.remote.creation.compose.layout

import androidx.compose.runtime.ComposableTargetMarker

/**
 * An annotation that can be used to mark a composable function as being expected to be used in a
 * composable function that is also marked or inferred to be marked as a [RemoteComposable].
 *
 * Content marked with [RemoteComposable] will be recorded into a Remote Compose document during the
 * recording phase. This document can then be transmitted and played back by a Remote Compose
 * Player, potentially on a different device or process.
 */
@Retention(AnnotationRetention.BINARY)
@ComposableTargetMarker(description = "RemoteCompose Composable")
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER,
)
public annotation class RemoteComposable
