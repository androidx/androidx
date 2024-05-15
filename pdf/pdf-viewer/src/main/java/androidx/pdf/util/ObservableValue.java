/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Interface for an observable property value, which can be exposed by a Subject. Clients can read
 * the value, or get notified whenever the value changes.
 *
 * @param <T> The type of the value.
 * <p>Example: <code>
 * class Subject {
 * public ObservableValue&lt;String&gt; getObservableProperty1() {...}
 * public ObservableValue&lt;Integer&gt; getObservableProperty2() {...}
 * }
 * </code> in which case clients of property1 of this Subject can become observers like e.g.: <code>
 * Subject subject;
 * String currentValue = subject.getObservableProperty1().get();
 * Object observerKey = subject.getObservableProperty1().addObserver(new ValueObserver() {...});
 * ... later
 * subject.getObservableProperty1().removeObserver(observerKey);
 * </code>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface ObservableValue<T> extends Observable<ObservableValue.ValueObserver<T>> {

    /**
     * Reads the value.
     *
     * @return the value, supposedly immutable (if T is a mutable object, it's not meant to be
     * mutated by clients).
     */
    @Nullable
    T get();

    /**
     * Interface to be provided by observers of this value.
     *
     * @param <T> type of value
     */
    interface ValueObserver<T> {

        /** Signals a new value has been recorded. */
        void onChange(@Nullable T oldValue, @Nullable T newValue);
    }
}
