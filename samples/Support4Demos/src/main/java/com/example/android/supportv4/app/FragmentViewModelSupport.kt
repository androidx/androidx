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

package com.example.android.supportv4.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.observe
import androidx.lifecycle.viewModelScope
import com.example.android.supportv4.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FragmentViewModelSupport : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // First time init, create the UI.
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().add(
                android.R.id.content,
                UiFragment()
            ).commit()
        }
    }

    /**
     * This is a fragment showing UI that will be updated from work done
     * in the ProgressViewModel.
     */
    class UiFragment : Fragment(R.layout.fragment_view_model) {

        private val progressViewModel: ProgressViewModel by viewModels()

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            view.findViewById<Button>(R.id.restart).setOnClickListener {
                progressViewModel.restart()
            }

            val progresssBar = view.findViewById<ProgressBar>(R.id.progress_horizontal)
            progressViewModel.max = progresssBar.max
            progressViewModel.progressLiveData.observe(viewLifecycleOwner) { progress ->
                progresssBar.progress = progress
            }
        }
    }

    /**
     * This is the ViewModel implementation that will be retained across
     * activity instances.  It represents some ongoing work, here a Job
     * we have that sits around incrementing a progress indicator.
     */
    class ProgressViewModel : ViewModel() {
        private var progress = 0
        private val _progressLiveData = MutableLiveData<Int>(progress)
        val progressLiveData: LiveData<Int> get() = _progressLiveData

        var max = 10000

        init {
            // Using viewModelScope ensures that the Job is automatically cancelled
            // when the ViewModel is cleared
            viewModelScope.launch {
                while (true) {
                    if (progress < max) {
                        _progressLiveData.value = progress++
                    }
                    delay(50)
                }
            }
        }

        fun restart() {
            progress = 0
        }
    }
}
