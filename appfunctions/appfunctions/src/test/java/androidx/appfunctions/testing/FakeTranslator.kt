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

package androidx.appfunctions.testing

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.internal.Translator

internal class FakeTranslator : Translator {
    var upgradeRequestCalled = false
    var upgradeResponseCalled = false
    var downgradeRequestCalled = false
    var downgradeResponseCalled = false

    override fun upgradeRequest(request: AppFunctionData): AppFunctionData {
        upgradeRequestCalled = true
        return request
    }

    override fun upgradeResponse(response: AppFunctionData): AppFunctionData {
        upgradeResponseCalled = true
        return response
    }

    override fun downgradeRequest(request: AppFunctionData): AppFunctionData {
        downgradeRequestCalled = true
        return request
    }

    override fun downgradeResponse(response: AppFunctionData): AppFunctionData {
        downgradeResponseCalled = true
        return response
    }
}
