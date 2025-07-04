/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.test.uiautomator.watcher

/**
 * Defines a contract for a watcher with a scope. A watcher determines whether a certain UI
 * condition is satisfied and allows to operate on the UI. It can be registered with
 * [androidx.test.uiautomator.UiAutomatorTestScope.watchFor]. [ScopedUiWatcher]s registered in a
 * [androidx.test.uiautomator.UiAutomatorTestScope] are automatically unregistered at the end of the
 * scope.
 */
public interface ScopedUiWatcher<T> {

    /** Whether the dialog is visible. */
    public fun isVisible(): Boolean

    /** A scope for interacting with the dialog. */
    public fun scope(): T
}

/**
 * Defines a registration, returned when registering a watcher. Through this interface a watcher can
 * be unregistered. [WatcherRegistration] extends a [AutoCloseable] so it can be used with [use]
 * api. Unregistering a watcher will ensure its visibility condition is
 */
public interface WatcherRegistration : AutoCloseable {

    /** Unregisters the dialog associated to this registration. */
    public fun unregister()

    public override fun close(): Unit = unregister()
}
