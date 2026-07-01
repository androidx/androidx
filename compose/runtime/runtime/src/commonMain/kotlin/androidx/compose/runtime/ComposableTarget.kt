/*
 * Copyright 2021 The Android Open Source Project
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

/**
 * This annotation may be applied to a [Composable] function one or more times to declare that the
 * function expects an applier whose name is the [applier] argument of any of the [ComposableTarget]
 * annotations marking the function.
 *
 * Beware that only version 2.4.20 and newer of the Compose compiler plugin correctly acknowledge
 * multiple applications of this annotation on a single function. Older versions of the Compose
 * compiler plugin will only acknowledge the [ComposableTarget] annotation on a function that comes
 * earliest in the file.
 *
 * The [applier] name can be an arbitrary string but is expected to be the fully qualified name of a
 * class that is annotated by [ComposableTargetMarker]. The [applier] name is used in diagnostic
 * messages unless it refers to a class annotated by [ComposableTargetMarker], then the
 * corresponding [ComposableTargetMarker.description] is used instead.
 *
 * The Compose compiler plugin can, in most cases, infer the necessary [ComposableTarget]
 * annotations, or equivalent pair of [ComposableInferredTarget] and
 * [ComposableInferredTargetConstraints] annotations, to apply to a composable function. Some
 * insight into how this inference works is provided below.
 *
 * If it has been declared or inferred that a composable function expects an applier with precisely
 * one allowed name, then that function is considered to be in the group of functions corresponding
 * to that name. If a composable function calls another composable function, then both must be of
 * the same group of composable functions. This means that if it has been determined that a called
 * function is part of a certain group, then the function that calls it must also be in the same
 * group. If a function calls another function of a different group, then the Compose compiler
 * plugin will generate a diagnostic message describing which group was received and which group was
 * expected.
 *
 * If it has been declared or inferred that a composable function expects an applier whose name is
 * in a set of allowed names, then that function is considered to be constrained to that set of
 * names. As mentioned above, if a composable function calls another composable function then both
 * must be of the same group of composable functions. This means that if it has been determined that
 * a called function is constrained to a set of names, then the function that calls it must be 1)
 * part of the group of functions corresponding to one of the names in that set, or 2) constrained
 * to a set of names that has at least one name in common with that set. If the calling function
 * does not fit in either of those two categories, then the Compose compiler plugin will generate a
 * diagnostic message describing the incompatibility between the appliers expected by the two
 * functions.
 *
 * If an [Applier] is supplied to a composable function at runtime that the function did not expect,
 * an error will be reported. This annotation, and the corresponding validation performed by the
 * Compose compiler plugin, can detect incompatibilities at compile time, and issue a diagnostic
 * message when calling a [Composable] function will result in the [Applier] check failing. This
 * makes it possible to eliminate the possibility of encountering runtime [Applier]
 * incompatibilities.
 *
 * The Compose compiler plugin can infer the necessary annotations to apply to a composable function
 * in most cases. However, there are certain categories of functions that need to be annotated
 * explicitly by the user to indicate that they are part of a certain group or constrained to a
 * certain set of names. They are listed below:
 * - [Composable] functions that call [ComposeNode] directly
 * - Abstract methods, such as interface functions (which do not contain a body from which the
 *   plugin can infer the necessary annotations)
 * - [Composable] lambdas used in sub-composition
 * - [Composable] lambdas that are stored in class fields or global variables
 *
 * Functions in the above categories that are not explicitly annotated will be ignored by the
 * Compose compiler plugin, and diagnostics will not be emitted when those functions are called
 * incorrectly.
 *
 * @param applier The applier name used during composable call checking. This can be an arbitrary
 *   string value but is expected to be a fully qualified name of a class that is marked with
 *   [ComposableTargetMarker].
 */
@Repeatable
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FILE,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER,
)
public annotation class ComposableTarget(val applier: String)
