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

/** Entry point for an SDK service running in the Privacy Sandbox.
 *
 * There must be exactly one interface annotated with @PrivacySandboxService in your SDK module.
 * This will be the first point of communication once the app has successfully loaded your SDK
 * in the Privacy Sandbox.
 *
 * On the SDK side, the tools will generate a class called `AbstractSandboxedSdkProviderCompat`,
 * containing an abstract factory method to create this service. This must be implemented by SDK
 * developers, returning an implementation of the [PrivacySandboxService] annotated interface. This
 * implementation should then be named in the SDK's `build.gradle` as the
 * `compatSdkProviderClassName`.
 *
 * For example:
 * ```
 * package com.example.mysdk
 *
 * class MySdkSandboxedSdkProvider : AbstractSandboxedSdkProviderCompat() {
 *     override fun createMySdk(context: Context): MySdk = MySdkImpl(context)
 * }
 * ```
 *
 * On the app side, the tools will generate a factory class for the service containing a static
 * function (prefixed with "wrapTo") which should be used to convert an IBinder to the
 * [PrivacySandboxService] annotated interface.
 *
 * For example:
 * ```
 * val sandboxManagerCompat = SdkSandboxManagerCompat.from(this)
 * val sandboxedSdk = sandboxManagerCompat.loadSdk("com.example.mysdk", Bundle.EMPTY)
 * val mySdk = MySdkFactory.wrapToMySdk(sandboxedSdk.getInterface()!!)
 * ```
 *
 * Like [PrivacySandboxInterface], interfaces annotated with [PrivacySandboxService] have
 * restrictions on allowed methods and types. See [PrivacySandboxInterface] documentation for
 * details.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
public annotation class PrivacySandboxService