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

package androidx.privacysandbox.sdkruntime.client

import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfig
import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfigsHolder
import androidx.test.core.app.ApplicationProvider
import java.io.FileNotFoundException

/**
 * Holds information about all TestSDKs.
 */
internal object TestSdkConfigs {

    private val ALL_CONFIGS: LocalSdkConfigsHolder by lazy {
        LocalSdkConfigsHolder.load(ApplicationProvider.getApplicationContext())
    }

    /**
     * Minimal TestSDK which built with HEAD version of sdkruntime-core library.
     */
    val CURRENT: LocalSdkConfig by lazy {
        forSdkName("current")
    }

    /**
     * Same as [CURRENT] but also has optional fields set, such as:
     * 1) [LocalSdkConfig.javaResourcesRoot]
     * 2) [LocalSdkConfig.resourceRemapping]
     */
    val CURRENT_WITH_RESOURCES: LocalSdkConfig by lazy {
        forSdkName("currentWithResources")
    }

    /**
     * Return LocalSdkConfig for TestSDK.
     * TestSDK should be registered in RuntimeEnabledSdkTable.xml with package name:
     * "androidx.privacysandbox.sdkruntime.testsdk.[testSdkName]"
     */
    fun forSdkName(testSdkName: String): LocalSdkConfig {
        val sdkPackageName = "androidx.privacysandbox.sdkruntime.testsdk.$testSdkName"
        return ALL_CONFIGS
            .getSdkConfig(sdkPackageName)
            ?: throw FileNotFoundException("Can't find LocalSdkConfig for $sdkPackageName")
    }
}
