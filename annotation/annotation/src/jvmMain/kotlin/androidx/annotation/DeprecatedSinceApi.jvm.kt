/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.annotation

import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.CONSTRUCTOR
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER

/**
 * Denotes that this API is only useful until the given API level; after that, a more suitable
 * platform API is available.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, ANNOTATION_CLASS, CLASS, CONSTRUCTOR)
public annotation class DeprecatedSinceApi(
    /** The API level where it is deprecated. */
    val api: Int,
    /** Suggested replacement. */
    val message: String = ""
)
