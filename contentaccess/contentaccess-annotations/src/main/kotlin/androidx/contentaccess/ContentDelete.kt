
/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.contentaccess

/**
 * Annotates a method that deletes one or more entries into a content provider.
 *
 * @property selection The conditions for deleting a row (e.g column1 = :value).
 * This applies if the method is not being passed the entity, otherwise this field is ignored and
 * we deleted the passed in entity(s).
 *
 * @property uri The string representation of the uri to delete from, if empty then uses the
 * entity's uri, if existing.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class ContentDelete(
    val selection: String,
    val uri: String
)
