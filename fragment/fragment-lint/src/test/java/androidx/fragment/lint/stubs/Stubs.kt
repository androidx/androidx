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

package androidx.fragment.lint.stubs

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.java

private val FRAGMENT = java("""
    package androidx.fragment.app;

    import androidx.lifecycle.LifecycleOwner;

    public class Fragment {
        public LifecycleOwner getViewLifecycleOwner() {}
    }
""")

private val LIFECYCLE_OWNER = java("""
    package androidx.lifecycle;

    public interface LifecycleOwner {}
""")

private val LIVEDATA = java("""
    package androidx.lifecycle;

    public abstract class LiveData<T> {
        public void observe(LifecycleOwner owner, Observer<? super T> observer) {}
    }
""")

private val MUTABLE_LIVEDATA = java("""
    package androidx.lifecycle;

    import androidx.fragment.app.Fragment;

    public class MutableLiveData<T> extends LiveData<T> {
        public void observe(Fragment fragment,  Observer<? super T> observer, Boolean bool) {}
    }
""")

private val OBSERVER = java("""
    package androidx.lifecycle;

    public interface Observer<T> {}
""")

internal val STUBS = arrayOf(
    FRAGMENT,
    LIVEDATA,
    MUTABLE_LIVEDATA,
    OBSERVER,
    LIFECYCLE_OWNER
)
