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

package androidx.privacysandbox.tools

import java.util.concurrent.CancellationException

/**
 * Annotated interfaces used by the app to communicate with the SDK in the Privacy Sandbox.
 *
 * Functions in a [PrivacySandboxInterface] annotated interface must obey the following rules:
 * - Functions with return values must suspend
 * - Parameter types may be primitives, [PrivacySandboxValue], [PrivacySandboxCallback],
 *   [PrivacySandboxInterface], or lists of primitives or [PrivacySandboxValue]. Nullable types
 *   are allowed.
 * - Return types may be primitives, [PrivacySandboxValue], [PrivacySandboxInterface], or lists
 *   of primitives or [PrivacySandboxValue]. Nullable types are allowed.
 *
 * Suspend functions operate as follows:
 * - The main thread is used by default
 * - App cancellations are propagated to SDK coroutines
 * - Cancellation exceptions thrown by SDKs are packaged and rethrown as valid
 *   [CancellationException]s
 *
 * Additionally, all exceptions thrown by SDK suspend function implementations are wrapped and
 * rethrown to app developers as `PrivacySandboxException` with a full stack trace. Errors in
 * non-suspend functions will not be rethrown.
 *
 * [PrivacySandboxInterface] annotated interfaces may not extend any interface except for
 * [androidx.privacysandbox.ui.core.SandboxedUiAdapter], which can be used to provide SDK content in
 * an app's UI. These interfaces may also have any other functions that are normally allowed.
 *
 * Usage example:
 * ```
 * @PrivacySandboxInterface
 * interface MyInterface {
 *     suspend fun doSomething(request: Request): Response
 *     suspend fun getMyInterface(input: MyInterface): MyInterface
 *     suspend fun getNullableInterface(input: MySecondInterface): MySecondInterface?
 *     fun setListener(listener: MyCallback)
 *     fun appendValue(x: Int)
 * }
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
public annotation class PrivacySandboxInterface