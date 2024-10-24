/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.services.client.impl.ipc.internal;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import org.jspecify.annotations.Nullable;

/**
 * Unique key to hold listener reference.
 *
 */
@RestrictTo(Scope.LIBRARY)
public final class ListenerKey {
    private final Object mListenerKey;

    public ListenerKey(Object listenerKey) {
        this.mListenerKey = listenerKey;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ListenerKey)) {
            return false;
        }

        ListenerKey that = (ListenerKey) o;
        return mListenerKey.equals(that);
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(mListenerKey);
    }

    @Override
    public String toString() {
        return String.valueOf(mListenerKey);
    }
}
