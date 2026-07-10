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

package androidx.webkit;

/**
 * Transition interface for {@link WebViewOutcomeReceiver} to support renaming in client code.
 *
 * @see WebViewOutcomeReceiver
 * @deprecated The {@code OutcomeReceiverCompat} name clashes with the same class in {@code
 * androidx.core}, so the webkit library version was renamed to avoid confusion. Clients should
 * migrate to the new name. This class will only be maintained until the APIs that originally
 * used the name are no longer considered experimental.
 *
 * @param <T> The type of the result that's being sent.
 * @param <E> The type of the {@link Throwable} that contains more information about the error.
 */
@Profile.ExperimentalUrlPrefetch
@Deprecated(forRemoval = true)
public interface OutcomeReceiverCompat<T, E extends Throwable> extends
        WebViewOutcomeReceiver<T, E> {
}
