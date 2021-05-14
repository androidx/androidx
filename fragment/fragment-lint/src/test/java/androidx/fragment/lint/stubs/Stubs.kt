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
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFiles

private val BACK_PRESSED_CALLBACK = java(
    """
    package androidx.activity;

    public abstract class OnBackPressedCallback {}
"""
)

private val BACK_PRESSED_DISPATCHER = java(
    """
    package androidx.activity;

    import androidx.lifecycle.LifecycleOwner;

    public final class OnBackPressedDispatcher {
        public void addCallback(LifecycleOwner owner, OnBackPressedCallback callback) {}
    }
"""
)

private val FRAGMENT = java(
    """
    package androidx.fragment.app;

    import androidx.lifecycle.Lifecycle;
    import androidx.lifecycle.LifecycleOwner;

    public class Fragment implements LifecycleOwner {
        public LifecycleOwner getViewLifecycleOwner() {}
        public LifecycleOwner getLifecycleOwner() {}
        public Lifecycle getLifecycle() {}
    }
    """
)

private val DIALOG_FRAGMENT = java(
    """
    package androidx.fragment.app;

    public class DialogFragment extends Fragment { }
"""
)

private val LIFECYCLE_OWNER = java(
    """
    package androidx.lifecycle;

    public interface LifecycleOwner {
        Lifecycle getLifecycle();
    }
"""
)

private val LIVEDATA = java(
    """
    package androidx.lifecycle;

    public abstract class LiveData<T> {
        public void observe(LifecycleOwner owner, Observer<? super T> observer) {}
    }
"""
)

private val MUTABLE_LIVEDATA = java(
    """
    package androidx.lifecycle;

    import androidx.fragment.app.Fragment;

    public class MutableLiveData<T> extends LiveData<T> {
        public void observe(Fragment fragment,  Observer<? super T> observer, Boolean bool) {}
    }
"""
)

private val OBSERVER = java(
    """
    package androidx.lifecycle;

    public interface Observer<T> {}
"""
)

private val LIFECYCLE = TestFiles.kt(
    "androidx/lifecycle/Lifecycle.kt",
    """
        package androidx.lifecycle;

        abstract class Lifecycle {
            enum class State { CREATED, STARTED }
            fun isAtLeast(state: State): Boolean {
                return true
            }
        }
    """
).indented().within("src")

private val LIVEDATA_OBSERVE_EXTENSION = kotlin(
    "androidx/lifecycle/LiveDataKt.kt",
    """
    package androidx.lifecycle

    import kotlin.jvm.functions.Function1

    object LiveDataKt {
        fun observe<T>(
            liveData: LiveData<T>,
            owner: LifecycleOwner,
            onChanged: Function1<T, Unit>) {

        }
    }
"""
).indented().within("src")

private val COROUTINES = TestFiles.kt(
    "kotlinx/coroutines/GlobalScope.kt",
    """
        package kotlinx.coroutines;

        import kotlinx.coroutines.CoroutineScope

        interface CoroutineScope {}

        object GlobalScope {
            fun launch(block: suspend () -> Unit) {}
        }

    """
).indented().within("src")

private val REPEAT_ON_LIFECYCLE = TestFiles.kt(
    "androidx/lifecycle/RepeatOnLifecycle.kt",
    """
        package androidx.lifecycle;

        import androidx.lifecycle.Lifecycle
        import androidx.lifecycle.LifecycleOwner
        import kotlinx.coroutines.CoroutineScope

        public suspend fun Lifecycle.repeatOnLifecycle(
            state: Lifecycle.State,
            block: suspend CoroutineScope.() -> Unit
        ) {
            throw Error()
        }

        public suspend fun LifecycleOwner.repeatOnLifecycle(
            state: Lifecycle.State,
            block: suspend CoroutineScope.() -> Unit
        ) {
            throw Error()
        }
    """
).indented().within("src")

// stubs for testing calls to LiveData.observe calls
internal val LIVEDATA_STUBS = arrayOf(
    FRAGMENT,
    DIALOG_FRAGMENT,
    LIFECYCLE,
    LIFECYCLE_OWNER,
    LIVEDATA,
    MUTABLE_LIVEDATA,
    OBSERVER,
    LIVEDATA_OBSERVE_EXTENSION
)

// stubs for testing calls to OnBackPressedDispatcher.addCallback calls
internal val BACK_CALLBACK_STUBS = arrayOf(
    BACK_PRESSED_CALLBACK,
    BACK_PRESSED_DISPATCHER,
    FRAGMENT,
    LIFECYCLE,
    LIFECYCLE_OWNER
)

// stubs for testing calls to LifecycleOwner.repeatOnLifecycle
internal val REPEAT_ON_LIFECYCLE_STUBS = arrayOf(
    REPEAT_ON_LIFECYCLE,
    DIALOG_FRAGMENT,
    FRAGMENT,
    COROUTINES,
    LIFECYCLE,
    LIFECYCLE_OWNER
)
