/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.foundation.diagnostics

/**
 * Debugging message displayed like a property.
 *
 * ## Sample code
 *
 * The following two properties are better expressed using this
 * [MessageProperty] class, rather than [StringProperty], as the intent is to
 * show a message with property style display rather than to describe the value
 * of an actual property of the object:
 *
 * ```dart
 * new MessageProperty('table size', '$columns\u00D7$rows')
 * ```
 * ```dart
 * new MessageProperty('usefulness ratio', 'no metrics collected yet (never painted)')
 * ```
 *
 * On the other hand, [StringProperty] is better suited when the property has a
 * concrete value that is a string:
 *
 * ```dart
 * new StringProperty('name', _name)
 * ```
 *
 * See also:
 *
 *  * [DiagnosticsNode.message], which serves the same role for messages
 *    without a clear property name.
 *  * [StringProperty], which is a better fit for properties with string values.

 * Create a diagnostics property that displays a message.
 *
 * Messages have no concrete [value] (so [value] will return null). The
 * message is stored as the description.
 *
 * The [name], `message`, and [level] arguments must not be null.
 */
class MessageProperty(
    name: String,
    message: String,
    level: DiagnosticLevel = DiagnosticLevel.info
) : DiagnosticsProperty<Void>(
        name, null,
        description = message,
        level = level)
