/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.core.app;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.collection.SimpleArrayMap;
import androidx.core.view.KeyEventDispatcher;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.ReportFragment;

/**
 * Base class for activities that enables composition of higher level components.
 * <p>
 * Rather than all functionality being built directly into this class, only the minimal set of
 * lower level building blocks are included. Higher level components can then be used as needed
 * without enforcing a deep Activity class hierarchy or strong coupling between components.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class ComponentActivity extends Activity implements
        LifecycleOwner,
        KeyEventDispatcher.Component {
    /**
     * Storage for {@link ExtraData} instances.
     *
     * <p>Note that these objects are not retained across configuration changes</p>
     */
    @SuppressWarnings("deprecation")
    private SimpleArrayMap<Class<? extends ExtraData>, ExtraData> mExtraDataMap =
            new SimpleArrayMap<>();
    /**
     * This is only used for apps that have not switched to Fragments 1.1.0, where this
     * behavior is provided by <code>androidx.activity.ComponentActivity</code>.
     */
    private LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);

    /**
     * Store an instance of {@link ExtraData} for later retrieval by class name
     * via {@link #getExtraData}.
     *
     * <p>Note that these objects are not retained across configuration changes</p>
     *
     * @see #getExtraData
     * @hide
     * @deprecated Use {@link View#setTag(int, Object)} with the window's decor view.
     */
    @SuppressWarnings("deprecation")
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Deprecated
    public void putExtraData(ExtraData extraData) {
        mExtraDataMap.put(extraData.getClass(), extraData);
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ReportFragment.injectIfNeededIn(this);
    }

    @CallSuper
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        mLifecycleRegistry.markState(Lifecycle.State.CREATED);
        super.onSaveInstanceState(outState);
    }

    /**
     * Retrieves a previously set {@link ExtraData} by class name.
     *
     * @see #putExtraData
     * @hide
     * @deprecated Use {@link View#getTag(int)} with the window's decor view.
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @SuppressWarnings({"unchecked", "deprecation"})
    @Deprecated
    public <T extends ExtraData> T getExtraData(Class<T> extraDataClass) {
        return (T) mExtraDataMap.get(extraDataClass);
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    /**
     * @hide
     * @param event
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public boolean superDispatchKeyEvent(@NonNull KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        View decor = getWindow().getDecorView();
        if (decor != null && KeyEventDispatcher.dispatchBeforeHierarchy(decor, event)) {
            return true;
        }
        return super.dispatchKeyShortcutEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        View decor = getWindow().getDecorView();
        if (decor != null && KeyEventDispatcher.dispatchBeforeHierarchy(decor, event)) {
            return true;
        }
        return KeyEventDispatcher.dispatchKeyEvent(this, decor, this, event);
    }

    /**
     * Checks if the internal state should be dump, as some special args are handled by
     * {@link Activity} itself.
     *
     * <p>Subclasses implementing
     * {@link Activity#dump(String, java.io.FileDescriptor, java.io.PrintWriter, String[])} should
     * typically start with:
     *
     * <pre>
     * {@literal @}Override
     * public void dump({@literal @}NonNull String prefix, {@literal @}Nullable FileDescriptor fd,
     *        {@literal @}NonNull PrintWriter writer, {@literal @}Nullable String[] args) {
     *    super.dump(prefix, fd, writer, args);
     *
     *    if (!shouldDumpInternalState(args)) {
     *        return;
     *    }
     *    // dump internal starte
     * }
     * </pre>
     */
    protected final boolean shouldDumpInternalState(@Nullable String[] args) {
        return !shouldSkipDump(args);
    }

    private static boolean shouldSkipDump(@Nullable String[] args) {
        if (args != null && args.length > 0) {
            // NOTE: values below are hardcoded on framework's Activity (like dumpInner())
            switch (args[0]) {
                case "--autofill":
                    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
                case "--contentcapture":
                    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
                case "--translation":
                    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
            }
        }
        return false;
    }

    /**
     * @hide
     * @deprecated Store the object you want to save directly by using
     * {@link View#setTag(int, Object)} with the window's decor view.
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Deprecated
    public static class ExtraData {
    }
}
