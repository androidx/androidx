/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.content.pm;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.List;

/**
 * Defines a listener for {@link ShortcutInfoCompat} changes in {@link ShortcutManagerCompat}. This
 * class is no-op as is and may be overridden to provide the required functionality.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public abstract class ShortcutInfoChangeListener {
    @AnyThread
    public void onShortcutAdded(@NonNull List<ShortcutInfoCompat> shortcuts) {}

    @AnyThread
    public void onShortcutUpdated(@NonNull List<ShortcutInfoCompat> shortcuts) {}

    @AnyThread
    public void onShortcutRemoved(@NonNull List<String> shortcutIds) {}

    @AnyThread
    public void onAllShortcutsRemoved() {}

    @AnyThread
    public void onShortcutUsageReported(@NonNull List<String> shortcutIds) {}
}
