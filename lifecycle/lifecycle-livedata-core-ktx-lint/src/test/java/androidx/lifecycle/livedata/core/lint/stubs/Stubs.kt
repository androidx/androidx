/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.lifecycle.livedata.core.lint.stubs

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.java

private val LIVEDATA = java(
    """
    package androidx.lifecycle;

    public abstract class LiveData<T> {
        protected void postValue(T value) {}
        protected void setValue(T value) {}
        public T getValue() {}
    }
"""
).indented()

private val MUTABLE_LIVEDATA = java(
    """
    package androidx.lifecycle;

    public class MutableLiveData<T> extends LiveData<T> {
        public MutableLiveData() {}
        @Override public void postValue(T value) { super.postValue(value); }
        @Override public void setValue(T value) { super.setValue(value); }
    }
"""
).indented()

internal val STUBS = arrayOf(
    LIVEDATA,
    MUTABLE_LIVEDATA
)
