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
package androidx.annotation

/**
 * Specifies a code fragment that can be used to suggest a replacement for a method in conjunction
 * with the `ReplaceWith` lint check.
 *
 * The `expression` parameter specified the replacement expression, which is interpreted in the
 * context of the symbol being used and can reference members of the enclosing classes, etc.
 *
 * For method calls, the replacement expression may contain parameter names of the method being
 * replaced, which will be substituted with actual arguments used in the call being replaced:
 * <pre>
 * &#64;ReplaceWith(expression = "event.getActionType(slot)")
 * static int getActionType(AccessibilityEvent event, int slot) { ... }
 * </pre>
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.FIELD,
    AnnotationTarget.CONSTRUCTOR
)
public annotation class ReplaceWith(val expression: String, vararg val imports: String = [])
