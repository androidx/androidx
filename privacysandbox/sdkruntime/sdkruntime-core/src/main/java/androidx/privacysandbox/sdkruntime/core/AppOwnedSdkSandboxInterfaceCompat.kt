/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.privacysandbox.sdkruntime.core

import android.app.sdksandbox.AppOwnedSdkSandboxInterface
import android.os.IBinder
import android.os.ext.SdkExtensions.AD_SERVICES
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP

/**
 * Represents a channel for an SDK in the sandbox process to interact with the app.
 *
 * The SDK and the app can agree on a binder interface to be implemented by the app and shared
 * via an object of [AppOwnedSdkSandboxInterfaceCompat].
 *
 * The app registers the AppOwnedSdkSandboxInterfaces using SdkSandboxManagerCompat.
 * The SDK can then query the list of registered interfaces using SdkSandboxControllerCompat.
 *
 * Once SDK has the [AppOwnedSdkSandboxInterfaceCompat] it wants to communicate with, it will have
 * to cast the binder object from [getInterface] to the prearranged interface before initiating
 * the communication.
 */
class AppOwnedSdkSandboxInterfaceCompat(
    private val name: String,
    private val version: Long,
    private val binder: IBinder
) {

    /**
     * Creates AppOwnedSdkSandboxInterfaceCompat from existing [AppOwnedSdkSandboxInterface] object.
     *
     * @param appOwnedSdkInterface source platform object.
     */
    @RequiresExtension(extension = AD_SERVICES, version = 8)
    @RestrictTo(LIBRARY_GROUP)
    constructor(appOwnedSdkInterface: AppOwnedSdkSandboxInterface) : this(
        name = appOwnedSdkInterface.getName(),
        version = appOwnedSdkInterface.getVersion(),
        binder = appOwnedSdkInterface.getInterface(),
    )

    /**
     * Returns the name used to register the [AppOwnedSdkSandboxInterfaceCompat].
     *
     * App can register only one interface of given name.
     */
    fun getName(): String = name

    /**
     * Returns the version used to register the [AppOwnedSdkSandboxInterfaceCompat].
     *
     * A version may be chosen by an app, and used to communicate any updates the app makes to
     * this implementation.
     */
    fun getVersion(): Long = version

    /**
     * Returns binder object associated with [AppOwnedSdkSandboxInterfaceCompat].
     *
     * The SDK and the app can agree on a binder interface to be implemented by the app and
     * shared via this object.
     *
     * The SDK in the sandbox will have to cast the binder object received from this method to
     * the agreed upon interface before using it.
     */
    fun getInterface(): IBinder = binder

    /**
     * Create [AppOwnedSdkSandboxInterface] from compat object.
     *
     * @return Platform AppOwnedSdkSandboxInterface
     */
    @RequiresExtension(extension = AD_SERVICES, version = 8)
    @RestrictTo(LIBRARY_GROUP)
    fun toAppOwnedSdkSandboxInterface() = AppOwnedSdkSandboxInterface(name, version, binder)
}
