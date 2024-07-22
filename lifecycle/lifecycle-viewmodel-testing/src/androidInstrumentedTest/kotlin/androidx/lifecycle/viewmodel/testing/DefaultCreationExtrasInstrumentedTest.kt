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

package androidx.lifecycle.viewmodel.testing

import android.os.Bundle
import androidx.kruth.assertThat
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider.Companion.VIEW_MODEL_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
internal class DefaultCreationExtrasInstrumentedTest {

    @Test
    fun creationExtras_hasAllExtras() {
        val creationExtras = DefaultCreationExtras()

        assertThat(creationExtras[SAVED_STATE_REGISTRY_OWNER_KEY]).isNotNull()
        assertThat(creationExtras[VIEW_MODEL_STORE_OWNER_KEY]).isNotNull()
        assertThat(creationExtras[DEFAULT_ARGS_KEY]).isNotNull()
        assertThat(creationExtras[DEFAULT_ARGS_KEY]!!.isEmpty()).isTrue()
    }

    @Test
    fun creationExtras_withCustomDefaultArgs() {
        val defaultArgs = Bundle().apply { putString("key", "value") }
        val creationExtras = DefaultCreationExtras(defaultArgs)

        assertThat(creationExtras[DEFAULT_ARGS_KEY]).isEqualTo(defaultArgs)
    }

    @Test
    fun creationExtras_savedStateHandle_isEnabled() {
        val creationExtras = DefaultCreationExtras() as MutableCreationExtras
        creationExtras[VIEW_MODEL_KEY] = "modelKey"

        assertThat(creationExtras.createSavedStateHandle()).isNotNull()
    }
}
