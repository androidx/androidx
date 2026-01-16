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

package androidx.xr.runtime

/**
 * Marks XrDevice lifecycle APIs that are experimental and likely to change or be removed in the
 * future.
 *
 * Any usage of a declaration annotated with `@ExperimentalXrDeviceLifecycleApi` must be accepted
 * either by annotating that usage with `@OptIn(ExperimentalXrDeviceLifecycleApi::class)` or by
 * propagating the annotation to the containing declaration.
 */
@RequiresOptIn(message = "This is an experimental API. It may be changed or removed in the future.")
@Retention(AnnotationRetention.BINARY)
public annotation class ExperimentalXrDeviceLifecycleApi()
