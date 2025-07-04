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

package androidx.wear.protolayout.testing

import androidx.wear.protolayout.expression.DynamicDataMap
import androidx.wear.protolayout.expression.MutableDynamicDataMap
import androidx.wear.protolayout.expression.mutableDynamicDataMapOf

/**
 * Context data for performing assertation on a layout, including:
 * * app state data and/or platform data for evaluating dynamic values applied on the elements
 */
public class TestContext internal constructor(injectedData: DynamicDataMap? = null) {
    private val mutableDynamicData: MutableDynamicDataMap = mutableDynamicDataMapOf()
    public val dynamicData: DynamicDataMap
        /**
         * Returns a read-only injected data map, which can contain both app state data and platform
         * data.
         */
        get() = mutableDynamicData

    init {
        injectedData?.let { mutableDynamicData.putAll(it) }
    }

    /** Add injected app state data and/or platform data into the assertion context. */
    internal fun addDynamicData(dynamicData: DynamicDataMap) {
        require(this !== EMPTY_CONTEXT) { "Cannot add dynamic data to the EMPTY_CONTEXT" }
        mutableDynamicData.putAll(dynamicData)
    }

    internal companion object {
        val EMPTY_CONTEXT = TestContext()
    }
}
