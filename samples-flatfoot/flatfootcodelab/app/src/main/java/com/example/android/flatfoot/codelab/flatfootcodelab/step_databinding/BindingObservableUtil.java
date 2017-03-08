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


import android.databinding.ObservableField;
import android.support.annotation.Nullable;

import com.android.support.lifecycle.LifecycleProvider;
import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.Observer;

/**
 * Binds a {@link LiveData} object to an {@link ObservableField}.
 */
public class BindingObservableUtil {

    public static <T> ObservableField<T> fromLiveData(LiveData<T> liveData,
                                                      LifecycleProvider lifecycleProvider) {

        final ObservableField<T> field = new ObservableField<>();

        liveData.observe(lifecycleProvider, new Observer<T>() {
            @Override
            public void onChanged(@Nullable T t) {
                field.set(t);
            }
        });

        return field;
    }
}