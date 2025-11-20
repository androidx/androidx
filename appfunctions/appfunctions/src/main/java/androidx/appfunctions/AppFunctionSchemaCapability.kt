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
import androidx.annotation.RestrictTo.Scope

/**
 * Marks a schema interface to be implemented by an [androidx.appfunctions.AppFunctionSerializable]
 * class as having a capability.
 *
 * Use of this annotation indicates that when a class implements this capability, its schema and the
 * capability's schema are treated as distinct outputs.
 *
 * This allows a serializable class to expose both its data and additional capabilities with
 * separate schemas. Example:
 * ```
 * @AppFunctionSchemaCapability
 * public interface OpenIntent {
 *     public val intentToOpen: PendingIntent
 * }
 *
 * @AppFunctionSerializable
 * data class NoteWithOpenIntent(
 *     override val intentToOpen,
 *     val title: String) : OpenIntent {}
 * ```
 *
 * In this case, `NoteWithOpenIntent` would have two distinct schema representations: one for its
 * data (`title`) and another for its `OpenIntent` capability. This is makes the schema interface
 * composable.
 */
@RestrictTo(Scope.LIBRARY_GROUP)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
public annotation class AppFunctionSchemaCapability()
