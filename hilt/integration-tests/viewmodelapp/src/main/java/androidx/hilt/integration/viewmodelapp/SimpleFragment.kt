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

package androidx.hilt.integration.viewmodelapp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.hilt.FragmentViewModelFactory
import androidx.lifecycle.hilt.ViewModelFactory
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SimpleFragment : Fragment() {
    // TODO(danysantiago): Should be declared in Hilt gen class
    @Inject
    @FragmentViewModelFactory
    lateinit var viewModelFactory: ViewModelFactory

    val simpleViewModel by viewModels<SimpleViewModel>()
    val activitySimpleViewModel by activityViewModels<SimpleViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("SimpleFragment", simpleViewModel.hi())
        Log.d("SimpleFragment", activitySimpleViewModel.hi())
    }

    // TODO(danysantiago): Should be overridden by Hilt gen class
    override fun getDefaultViewModelProviderFactory() = viewModelFactory
}