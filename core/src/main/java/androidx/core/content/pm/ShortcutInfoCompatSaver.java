/*
 * Copyright 2018 The Android Open Source Project
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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import androidx.annotation.AnyThread;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.concurrent.futures.ResolvableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines APIs to access and update a persistable list of {@link ShortcutInfoCompat}. This class
 * is no-op as is and may be overridden to provide the required functionality.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class ShortcutInfoCompatSaver {
    @AnyThread
    public ListenableFuture<Void> addShortcuts(List<ShortcutInfoCompat> shortcuts) {
        final ResolvableFuture<Void> result = ResolvableFuture.create();
        result.set(null);
        return result;
    }

    @AnyThread
    public ListenableFuture<Void> removeShortcuts(List<String> shortcutIds) {
        final ResolvableFuture<Void> result = ResolvableFuture.create();
        result.set(null);
        return result;
    }

    @AnyThread
    public ListenableFuture<Void> removeAllShortcuts() {
        final ResolvableFuture<Void> result = ResolvableFuture.create();
        result.set(null);
        return result;
    }

    @WorkerThread
    public List<ShortcutInfoCompat> getShortcuts() throws Exception {
        return new ArrayList<>();
    }
}
