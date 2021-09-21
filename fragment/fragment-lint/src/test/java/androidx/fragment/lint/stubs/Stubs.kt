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

private val COMPONENT_ACTIVITY = java(
    """
    package androidx.activity;

    import androidx.core.view.MenuHost;
    import androidx.core.view.MenuProvider;
    import androidx.lifecycle.Lifecycle;
    import androidx.lifecycle.LifecycleOwner;

    public class ComponentActivity implements MenuHost {
        public void addMenuProvider(@NonNull MenuProvider provider, @NonNull LifecycleOwner owner) {

        }

        public void addMenuProvider(@NonNull MenuProvider provider, @NonNull LifecycleOwner owner,
            @NonNull Lifecycle.State state) { }
    }
    """
)

private val FRAGMENT = java(
    """
    package androidx.fragment.app;

    import androidx.activity.ComponentActivity;
    import androidx.core.view.MenuProvider;
    import androidx.lifecycle.Lifecycle;
    import androidx.lifecycle.LifecycleOwner;

    public class Fragment implements LifecycleOwner, MenuProvider {
        public LifecycleOwner getViewLifecycleOwner() {}
        public Lifecycle getLifecycle() {}
        public ComponentActivity requireActivity() {
            return ComponentActivity();
        }
    }
    """
)

private val FRAGMENT_MANAGER = java(
    """
    package androidx.fragment.app;

    public class FragmentManager {
        public FragmentTransaction beginTransaction() { }
    }
    """
)

private val FRAGMENT_TRANSACTION = java(
    """
    package androidx.fragment.app;

    public class FragmentTransaction {
        public FragmentTransaction attach(Fragment fragment) { }
        public FragmentTransaction detach(Fragment fragment) { }
        public int commit() { }
        public int commitAllowingStateLoss() { }
        public int commitNow() { }
        public int commitNowAllowingStateLoss() { }
    }
    """
)

internal val DIALOG_FRAGMENT = java(
    """
    package androidx.fragment.app;

    public class DialogFragment extends Fragment { }
"""
)

internal val ALERT_DIALOG = java(
    """
    package androidx.appcompat.app;

    public class AlertDialog { }
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

private val MENU_PROVIDER = java(
    """
    package androidx.core.view;

    import androidx.annotation.NonNull;

    public interface MenuProvider { }
    """
)

private val MENU_HOST = java(
    """
    package androidx.core.view;

    import androidx.annotation.NonNull;
    import androidx.lifecycle.Lifecycle;
    import androidx.lifecycle.LifecycleOwner;

    public interface MenuHost {
        void addMenuProvider(@NonNull MenuProvider provider, @NonNull LifecycleOwner owner);

        void addMenuProvider(@NonNull MenuProvider provider, @NonNull LifecycleOwner owner,
            @NonNull Lifecycle.State state);
    }
    """
)

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
        package androidx.lifecycle

        public fun LifecycleOwner.repeatOnLifecycle(
            state: Lifecycle.State,
            block: suspend CoroutineScope.() -> Unit
        ) { }
    """
).indented().within("src")

// stubs for testing fragment transaction stubs
internal val FRAGMENT_TRANSACTION_STUBS = arrayOf(
    COMPONENT_ACTIVITY,
    FRAGMENT,
    FRAGMENT_MANAGER,
    FRAGMENT_TRANSACTION,
    LIFECYCLE,
    LIFECYCLE_OWNER,
    MENU_HOST,
    MENU_PROVIDER
)

// stubs for testing calls to LiveData.observe calls
internal val LIVEDATA_STUBS = arrayOf(
    COMPONENT_ACTIVITY,
    FRAGMENT,
    DIALOG_FRAGMENT,
    LIFECYCLE,
    LIFECYCLE_OWNER,
    LIVEDATA,
    MUTABLE_LIVEDATA,
    OBSERVER,
    LIVEDATA_OBSERVE_EXTENSION,
    MENU_HOST,
    MENU_PROVIDER
)

// stubs for testing calls to OnBackPressedDispatcher.addCallback calls
internal val BACK_CALLBACK_STUBS = arrayOf(
    COMPONENT_ACTIVITY,
    BACK_PRESSED_CALLBACK,
    BACK_PRESSED_DISPATCHER,
    FRAGMENT,
    LIFECYCLE,
    LIFECYCLE_OWNER,
    MENU_HOST,
    MENU_PROVIDER
)

// stubs for testing calls to LifecycleOwner.repeatOnLifecycle
internal val REPEAT_ON_LIFECYCLE_STUBS = arrayOf(
    COMPONENT_ACTIVITY,
    REPEAT_ON_LIFECYCLE,
    DIALOG_FRAGMENT,
    FRAGMENT,
    COROUTINES,
    LIFECYCLE,
    LIFECYCLE_OWNER,
    MENU_HOST,
    MENU_PROVIDER
)

// stubs for testing calls to MenuHost.addMenuProvider calls
internal val ADD_MENU_PROVIDER_STUBS = arrayOf(
    COMPONENT_ACTIVITY,
    FRAGMENT,
    LIFECYCLE,
    LIFECYCLE_OWNER,
    MENU_HOST,
    MENU_PROVIDER
)
