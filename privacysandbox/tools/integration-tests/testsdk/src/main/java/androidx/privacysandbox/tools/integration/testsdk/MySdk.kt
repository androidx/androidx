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

package androidx.privacysandbox.tools.integration.testsdk

import android.content.Context
import androidx.privacysandbox.tools.PrivacySandboxService

@PrivacySandboxService
interface MySdk {
    suspend fun doSum(x: Int, y: Int): Int
}

class MySdkImpl(private val context: Context) : MySdk {
    override suspend fun doSum(x: Int, y: Int): Int {
        return x + y
    }
}
