/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.subspace.layout

/**
 * An Entity is the underlying API that Compose XR emits and controls to render spatial content.
 * This opaque wrapper has been extracted to avoid exposing the underlying entity type where Entity
 * appears in the API. It is not expected to be used or manipulated outside Compose XR's subspace
 * module.
 */
@PublishedApi internal interface OpaqueEntity
