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

package androidx.lifecycle.viewmodel.internal

import androidx.kruth.assertThat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.test.Test
import kotlin.test.fail

class DefaultViewModelProviderFactoryTest {

    @Test
    fun create_throwsUnsupportedOperationException() {
        val modelClass = ViewModel::class
        try {
            DefaultViewModelProviderFactory.create(modelClass, CreationExtras.Empty)
            fail("Expected `UnsupportedOperationException` but no exception has been throw.")
        } catch (e: UnsupportedOperationException) {
            assertThat(e).hasCauseThat().isNull()
            assertThat(e).hasMessageThat().contains(
                "`Factory.create(String, CreationExtras)` is not implemented. You may need to " +
                    "override the method and provide a custom implementation. Note that using " +
                    "`Factory.create(String)` is not supported and considered an error."
            )
        }
    }
}
