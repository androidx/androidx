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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Interface for a subject that wants to expose changes to its state with notifications. Clients of
 * these notifications use this interface to register themselves as observers of this subject.
 *
 * <p>
 *
 * @param <T> The type of notifications being sent.
 * <p>Note: the Subject class is encouraged to expose one (or more) {@code Observable<T>} end
 * point(s) by delegation rather than inheritance: <code>
 * class Subject {
 * public Observable&lt;NotificationForProperty1Interface> getObservableProperty1() {...}
 * public Observable&lt;NotificationForProperty2Interface> getObservableProperty2() {...}
 * }
 * </code> in which case clients of property1 of this Subject can become observers like e.g.: <code>
 * Subject subject;
 * Object observerKey = subject.getObservableProperty1().addObserver(new PropertyObserver() {...});
 * ... later
 * subject.getObservableProperty1().removeObserver(observerKey);
 * </code>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface Observable<T> {

    /**
     * Registers a new observer (has to be matched with a later {@link #removeObserver}).
     *
     * @return An object that uniquely identifies this observer, for use in {@link #removeObserver}.
     * Unless explicitly documented otherwise, the key will be the passed observer instance.
     */
    @NonNull
    Object addObserver(T observer);

    /**
     * Removes an observer previously set with {@link #addObserver}. This is not idempotent and some
     * implementations may consider it an error to try to remove an inexistent observer.
     *
     * @param observerKey The object previously returned from a call to {@link #addObserver}.
     */
    void removeObserver(@NonNull Object observerKey);
}
