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

package androidx.concurrent.futures;


import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * An AndroidX version of Guava's {@code SettableFuture}.
 * <p>
 * A {@link ListenableFuture} whose result can be set by a {@link #set(Object)}, {@link
 * #setException(Throwable)} or {@link #setFuture(ListenableFuture)} call. It can also, like any
 * other {@code Future}, be {@linkplain #cancel cancelled}.
 * <p>
 * If your needs are more complex than {@code ResolvableFuture} supports, use {@link
 * AbstractResolvableFuture}, which offers an extensible version of the API.
 *
 * @hide
 * @author Sven Mawson
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public final class ResolvableFuture<V> extends AbstractResolvableFuture<V> {
    /**
     * Creates a new {@code ResolvableFuture} that can be completed or cancelled by a later method
     * call.
     */
    public static <V> ResolvableFuture<V> create() {
        return new ResolvableFuture<>();
    }

    @Override
    public boolean set(@Nullable V value) {
        return super.set(value);
    }

    @Override
    public boolean setException(Throwable throwable) {
        return super.setException(throwable);
    }

    @Override
    public boolean setFuture(ListenableFuture<? extends V> future) {
        return super.setFuture(future);
    }

    private ResolvableFuture() {
    }
}

