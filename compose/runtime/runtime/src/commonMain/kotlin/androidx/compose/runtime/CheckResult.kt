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

package androidx.compose.runtime

// TODO: It's not required anymore, but was declared as public previously.
//  `metalava` won't detect removing this API because typealias doesn't
//  introduce a new type in runtime.
@Deprecated(
    message = "It was never intended to be public",
    replaceWith = ReplaceWith("androidx.annotation.CheckResult"),
)
public typealias CheckResult = androidx.annotation.CheckResult
