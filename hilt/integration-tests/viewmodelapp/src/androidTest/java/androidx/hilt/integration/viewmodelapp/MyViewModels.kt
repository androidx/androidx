/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("DEPRECATION")

package androidx.hilt.integration.viewmodelapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

class MyAndroidViewModel(app: Application) : AndroidViewModel(app)

class MyViewModel() : ViewModel()

@Suppress("UNUSED_PARAMETER")
@HiltViewModel
class MyInjectedViewModel @Inject constructor(foo: Foo) : ViewModel()

@Suppress("UNUSED_PARAMETER")
@HiltViewModel(assistedFactory = MyAssistedInjectedViewModel.Factory::class)
class MyAssistedInjectedViewModel @AssistedInject constructor(foo: Foo, @Assisted val bar: Int) :
    ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(bar: Int): MyAssistedInjectedViewModel
    }
}

object TopClass {
    @Suppress("UNUSED_PARAMETER")
    @HiltViewModel
    class MyNestedInjectedViewModel @Inject constructor(foo: Foo) : ViewModel()
}
