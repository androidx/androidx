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

package androidx.appfunctions

import androidx.annotation.RestrictTo

// TODO(b/403525399): Defines all compatible overrides
/**
 * Annotates an interface class to be used as a base for [AppFunctionSerializable] implementations.
 *
 * The AppFunction compiler plugin enforces property compatibility between this interface/class and
 * any [AppFunctionSerializable] class that implements or extends it.
 *
 * Take a base interface like Note as example:
 * ```
 * @AppFunctionSerializableInterface
 * public interface Note {
 *   public val title: String
 *   public val content: String
 *
 *   public val attachments: List<Attachment>
 *       get() = emptyList()
 * }
 * ```
 *
 * If an app attempts to implement this base interface, it must respect the original property status
 *
 * ```
 * @AppFunctionSerializable
 * public class MyNote(
 *   override val title: String,
 *   override val content: String,
 *   // attachments is optional in base schema, therefore, must stay as optional
 *   override val attachments: List<Attachment> = emptyList()
 * ): Note
 * ```
 *
 * Attempting to have an incompatible override would result in a compiler error:
 * ```
 * @AppFunctionSerializable
 * public class MyNote(
 *   override val title: String,
 *   override val content: String,
 *   // This would cause a compiler error because it is not required instead of optional
 *   override val attachments: List<Attachment>
 * ): Note
 * ```
 *
 * @see [AppFunctionSerializable]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
public annotation class AppFunctionSerializableInterface
