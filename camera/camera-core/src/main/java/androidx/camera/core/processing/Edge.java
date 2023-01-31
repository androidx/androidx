/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.core.processing;

import static kotlin.jvm.internal.Intrinsics.checkNotNull;

import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

/**
 * A {@link Consumer} that can be listened to by another {@link Consumer}.
 *
 * <p>This is a publisher/subscriber mechanism that follows the pattern of the {@link Surface}
 * API, where the upstream pipeline publishes data to the class by calling {@link #accept}, which
 * will be automatically sent to the downstream pipeline registered via {@link #setListener}.
 *
 * @param <T> the data type.
 */
public class Edge<T> implements Consumer<T> {

    private Consumer<T> mListener;

    @Override
    public void accept(@NonNull T t) {
        checkNotNull(mListener, "Listener is not set.");
        mListener.accept(t);
    }

    /**
     * Sets a listener that will get data updates.
     */
    public void setListener(@NonNull Consumer<T> listener) {
        mListener = listener;
    }
}
