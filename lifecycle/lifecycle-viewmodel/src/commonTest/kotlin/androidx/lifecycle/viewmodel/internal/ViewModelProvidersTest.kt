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
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import kotlin.test.Test

class ViewModelProvidersTest {

    @Test
    fun getDefaultFactory_ownerWithNoFactory_returnsDefault() {
        val owner = TestViewModelStoreOwner()
        val factory = ViewModelProviders.getDefaultFactory(owner)
        assertThat(factory).isEqualTo(DefaultViewModelProviderFactory)
    }

    @Test
    fun getDefaultFactory_ownerWithFactory_returnsExtras() {
        val customFactory = object : ViewModelProvider.Factory {}
        val owner = TestViewModelStoreOwnerWithDefaults(
            defaultViewModelProviderFactory = customFactory,
        )

        val factory = ViewModelProviders.getDefaultFactory(owner)

        assertThat(factory).isEqualTo(customFactory)
    }

    @Test
    fun getDefaultCreationExtras_ownerWithNoExtras_returnsDefault() {
        val owner = TestViewModelStoreOwner()
        val extras = ViewModelProviders.getDefaultCreationExtras(owner)
        assertThat(extras).isEqualTo(CreationExtras.Empty)
    }

    @Test
    fun getDefaultCreationExtras_ownerWithExtras_returnsExtras() {
        val customExtras = MutableCreationExtras()
        val owner = TestViewModelStoreOwnerWithDefaults(
            defaultViewModelCreationExtras = customExtras,
        )

        val extras = ViewModelProviders.getDefaultCreationExtras(owner)

        assertThat(extras).isEqualTo(customExtras)
    }

    private class TestViewModelStoreOwner(
        override val viewModelStore: ViewModelStore = ViewModelStore(),
    ) : ViewModelStoreOwner

    private class TestViewModelStoreOwnerWithDefaults(
        override val viewModelStore: ViewModelStore = ViewModelStore(),
        override val defaultViewModelProviderFactory: ViewModelProvider.Factory =
            DefaultViewModelProviderFactory,
        override val defaultViewModelCreationExtras: CreationExtras = CreationExtras.Empty,
    ) : ViewModelStoreOwner, HasDefaultViewModelProviderFactory
}
