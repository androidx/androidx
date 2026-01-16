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

package androidx.compose.foundation.layout

@RequiresOptIn(
    "This FlexBox API is experimental and is likely to change or be removed in the future.\n" +
        "This API is experimental because it introduces a new category of layout concepts to the Compose Foundation library. \n" +
        "It requires a period of user validation to ensure the API surface is intuitive and flexible enough to cover the intended use cases before being stabilized. (https://issuetracker.google.com/475491619)"
)
@Retention(AnnotationRetention.BINARY)
public annotation class ExperimentalFlexBoxApi
