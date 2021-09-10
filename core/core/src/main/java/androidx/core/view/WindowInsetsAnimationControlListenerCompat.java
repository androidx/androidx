/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.core.view;

import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsCompat.Type;
import androidx.core.view.WindowInsetsCompat.Type.InsetsType;

/**
 * Listener that encapsulates a request to
 * {@link WindowInsetsControllerCompat#controlWindowInsetsAnimation}.
 *
 * <p>
 * Insets can be controlled with the supplied {@link WindowInsetsAnimationControllerCompat} from
 * {@link #onReady} until either {@link #onFinished} or {@link #onCancelled}.
 *
 * <p>
 * Once the control over insets is finished or cancelled, it will not be regained until a new
 * request to {@link WindowInsetsControllerCompat#controlWindowInsetsAnimation} is made.
 *
 * <p>
 * The request to control insets can fail immediately. In that case {@link #onCancelled} will be
 * invoked without a preceding {@link #onReady}.
 *
 * @see WindowInsetsControllerCompat#controlWindowInsetsAnimation
 */
public interface WindowInsetsAnimationControlListenerCompat {

    /**
     * Called when the animation is ready to be controlled. This may be delayed when the IME needs
     * to redraw because of an {@link EditorInfo} change, or when the window is starting up.
     *
     * @param controller The controller to control the inset animation.
     * @param types      The {@link Type}s it was able to gain control over. Note that this
     *                   may be different than the types passed into
     *                   {@link WindowInsetsControllerCompat#controlWindowInsetsAnimation} in
     *                   case the window wasn't able to gain the controls because it wasn't the
     *                   IME target or not currently the window that's controlling the system bars.
     * @see WindowInsetsAnimationControllerCompat#isReady
     */
    void onReady(@NonNull WindowInsetsAnimationControllerCompat controller, @InsetsType int types);

    /**
     * Called when the request for control over the insets has
     * {@link WindowInsetsAnimationControllerCompat#finish finished}.
     * <p>
     * Once this callback is invoked, the supplied {@link WindowInsetsAnimationControllerCompat}
     * is no longer {@link WindowInsetsAnimationControllerCompat#isReady() ready}.
     * <p>
     * Control will not be regained until a new request
     * to {@link WindowInsetsControllerCompat#controlWindowInsetsAnimation} is made.
     *
     * @param controller the controller which has finished.
     * @see WindowInsetsAnimationControllerCompat#isFinished
     */
    void onFinished(@NonNull WindowInsetsAnimationControllerCompat controller);

    /**
     * Called when the request for control over the insets has been cancelled, either
     * because the {@link android.os.CancellationSignal} associated with the
     * {@link WindowInsetsControllerCompat#controlWindowInsetsAnimation request} has been
     * invoked, or the window has lost control over the insets (e.g. because it lost focus).
     * <p>
     * Once this callback is invoked, the supplied {@link WindowInsetsAnimationControllerCompat}
     * is no longer {@link WindowInsetsAnimationControllerCompat#isReady() ready}.
     * <p>
     * Control will not be regained until a new request
     * to {@link WindowInsetsControllerCompat#controlWindowInsetsAnimation} is made.
     *
     * @param controller the controller which has been cancelled, or null if the request
     *                   was cancelled before {@link #onReady} was invoked.
     * @see WindowInsetsAnimationControllerCompat#isCancelled
     */
    void onCancelled(@Nullable WindowInsetsAnimationControllerCompat controller);
}
