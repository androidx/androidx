/*
 * Copyright 2017, The Android Open Source Project
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

package com.example.android.flatfoot.codelab.flatfootcodelab.step_databinding;

import android.databinding.DataBindingUtil;
import android.databinding.ObservableField;
import android.os.Bundle;

import com.android.support.lifecycle.LifecycleActivity;
import com.android.support.lifecycle.ViewModelStore;
import com.example.android.flatfoot.codelab.flatfootcodelab.R;
import com.example.android.flatfoot.codelab.flatfootcodelab.databinding.ChronoActivityDatabindingBinding;

public class ChronoDataBindingActivity extends LifecycleActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ChronoActivityDatabindingBinding viewDataBinding = DataBindingUtil.setContentView(
                this, R.layout.chrono_activity_databinding);

        TimerViewModel chronometerViewModel = ViewModelStore.get(
                this, "ChronometerViewModel", TimerViewModel.class);

        // Subscribe UI to changes on the LiveData object
        ObservableField<Long> elapsedTime = BindingObservableUtil.fromLiveData(
                chronometerViewModel.timer.getLiveElapsedTime(), this);
        viewDataBinding.setElapsedTime(elapsedTime);

        // Make the timer observe this activity's lifecycle
        getLifecycle().addObserver(chronometerViewModel.timer);
    }
}
