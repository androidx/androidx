/*
 * Copyright 2022 The Android Open Source Project
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
// Restriction is added to prevent the Kt file showing up in the api file.
@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package androidx.collection

import androidx.annotation.RestrictTo

@Suppress("ACTUAL_WITHOUT_EXPECT", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
// this class is intentionally mapped to java.lang.Cloneable to ensure it doesn't come with a
// clone method.
internal actual typealias CloneableKmp = java.lang.Cloneable
@Suppress("ACTUAL_WITHOUT_EXPECT", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
// this class is intentionally mapped to java.lang.Object because kotlin.Any does not have a
// constructor
public actual typealias JvmCloneableAny = java.lang.Object
