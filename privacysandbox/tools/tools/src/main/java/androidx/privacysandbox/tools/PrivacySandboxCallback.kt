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

/**
 * Annotated callbacks that can be passed to an SDK running in the Privacy Sandbox.
 *
 * These can be used to provide the SDK with a channel to invoke app code, e.g. listeners. They
 * should be public interfaces that only declare functions without implementation, and they may not
 * extend any other interface. Callbacks run in the main thread by default.
 *
 * The allowed types are the same as for [PrivacySandboxInterface], except that functions may not
 * return a value.
 *
 * Usage example:
 * ```
 * @PrivacySandboxCallback
 * interface MyCallback {
 *     fun onComplete(response: Response)
 *     fun onClick(x: Int, y: Int)
 *     fun onCompleteInterface(myInterface: MyInterface)
 * }
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
public annotation class PrivacySandboxCallback