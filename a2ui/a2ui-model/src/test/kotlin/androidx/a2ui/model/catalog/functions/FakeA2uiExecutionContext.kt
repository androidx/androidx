/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.a2ui.model.catalog.functions

import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.protocol.A2uiExecutionContext

internal object FakeA2uiExecutionContext : A2uiExecutionContext {
    override fun evaluatePayload(dataPath: A2uiDataPath, payload: Any?): Any? = null

    override fun executeFunction(name: String, args: Map<String, Any>): Any? = null

    override fun resolveValue(path: A2uiDataPath): Any? = null
}

internal fun A2uiFunction.execute(args: Map<String, Any>): Any? {
    return this.execute(args, FakeA2uiExecutionContext)
}
