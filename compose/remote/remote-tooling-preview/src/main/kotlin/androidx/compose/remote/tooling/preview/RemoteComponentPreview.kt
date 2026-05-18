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

package androidx.compose.remote.tooling.preview

import androidx.annotation.RestrictTo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper

/**
 * A [Preview] that can be applied
 * to @[RemoteComposable][androidx.compose.remote.creation.compose.layout.RemoteComposable] methods
 * to show them in the Android Studio preview.
 *
 * This preview wraps the content and does not allow preview overrides, making it suitable for
 * previewing individual components rather than full device screens.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Preview
@PreviewWrapper(RemotePreviewWrapper::class)
@Target(AnnotationTarget.FUNCTION)
public annotation class RemoteComponentPreview
