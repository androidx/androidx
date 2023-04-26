/*
 * Copyright 2023 The Android Open Source Project
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

import static android.os.Build.VERSION.SDK_INT;

import android.content.Context;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provide controls for showing and hiding the IME.
 * <p>
 * This class provides the implementation for {@link WindowInsetsControllerCompat#show(int)} and
 * {@link WindowInsetsControllerCompat#hide(int)} for the {@link WindowInsetsCompat.Type#ime()}.
 * <p>
 * This class only requires a View as a dependency, whereas {@link WindowInsetsControllerCompat}
 * requires a Window for all of its behavior.
 */
public final class SoftwareKeyboardControllerCompat {

    private final Impl mImpl;

    public SoftwareKeyboardControllerCompat(@NonNull View view) {
        if (SDK_INT >= 30) {
            mImpl = new Impl30(view);
        } else if (SDK_INT >= 20) {
            mImpl = new Impl20(view);
        } else {
            mImpl = new Impl();
        }
    }

    @RequiresApi(30)
    @Deprecated
    SoftwareKeyboardControllerCompat(@NonNull WindowInsetsController windowInsetsController) {
        mImpl = new Impl30(windowInsetsController);
    }

    /**
     * Request that the system show a software keyboard.
     * <p>
     * This request is best effort. If the system can currently show a software keyboard, it
     * will be shown. However, there is no guarantee that the system will be able to show a
     * software keyboard. If the system cannot show a software keyboard currently,
     * this call will be silently ignored.
     */
    public void show() {
        mImpl.show();
    }

    /**
     * Hide the software keyboard.
     * <p>
     * This request is best effort, if the system cannot hide the software keyboard this call
     * will silently be ignored.
     */
    public void hide() {
        mImpl.hide();
    }

    private static class Impl {
        Impl() {
            //private
        }

        void show() {
        }

        void hide() {
        }
    }

    @RequiresApi(20)
    private static class Impl20 extends Impl {

        @Nullable
        private final View mView;

        Impl20(@Nullable View view) {
            mView = view;
        }

        @Override
        void show() {
            // We'll try to find an available textView to focus to show the IME
            View view = mView;

            if (view == null) {
                return;
            }

            if (view.isInEditMode() || view.onCheckIsTextEditor()) {
                // The IME needs a text view to be focused to be shown
                // The view given to retrieve this controller is a textView so we can assume
                // that we can focus it in order to show the IME
                view.requestFocus();
            } else {
                view = view.getRootView().findFocus();
            }

            // Fallback on the container view
            if (view == null) {
                view = mView.getRootView().findViewById(android.R.id.content);
            }

            if (view != null && view.hasWindowFocus()) {
                final View finalView = view;
                finalView.post(() -> {
                    InputMethodManager imm =
                            (InputMethodManager) finalView.getContext()
                                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(finalView, 0);

                });
            }
        }

        @Override
        void hide() {
            if (mView != null) {
                ((InputMethodManager) mView.getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(mView.getWindowToken(),
                                0);
            }
        }
    }

    @RequiresApi(30)
    private static class Impl30 extends Impl20 {

        @Nullable
        private View mView;

        @Nullable
        private WindowInsetsController mWindowInsetsController;

        Impl30(@NonNull View view) {
            super(view);
            mView = view;
        }

        Impl30(@Nullable WindowInsetsController windowInsetsController) {
            super(null);
            mWindowInsetsController = windowInsetsController;
        }

        @Override
        void show() {
            if (mView != null && SDK_INT < 33) {
                InputMethodManager imm =
                        (InputMethodManager) mView.getContext()
                                .getSystemService(Context.INPUT_METHOD_SERVICE);

                // This is a strange-looking workaround by making a call and ignoring the result.
                // We don't use the return value here, but isActive() has the side-effect of
                // calling a hidden method checkFocus(), which ensures that the IME state has the
                // correct view in some situations (especially when the focused view changes).
                // This is essentially a backport, since an equivalent checkFocus() call was
                // added in API 32 to improve behavior and an additional change in API 33:
                // https://issuetracker.google.com/issues/189858204
                imm.isActive();
            }
            WindowInsetsController insetsController = null;
            if (mWindowInsetsController != null) {
                insetsController = mWindowInsetsController;
            } else if (mView != null) {
                insetsController = mView.getWindowInsetsController();
            }
            if (insetsController != null) {
                insetsController.show(WindowInsets.Type.ime());
            } else {
                // Couldn't find an insets controller, fallback to old implementation
                super.show();
            }
        }

        @Override
        void hide() {
            WindowInsetsController insetsController = null;
            if (mWindowInsetsController != null) {
                insetsController = mWindowInsetsController;
            } else if (mView != null) {
                insetsController = mView.getWindowInsetsController();
            }
            if (insetsController != null) {
                if (SDK_INT <= 33) {
                    final AtomicBoolean isImeInsetsControllable = new AtomicBoolean(false);
                    final WindowInsetsController.OnControllableInsetsChangedListener listener =
                            (windowInsetsController, typeMask) -> isImeInsetsControllable.set(
                                    (typeMask & WindowInsetsCompat.Type.IME) != 0);
                    // Register the OnControllableInsetsChangedListener would synchronously
                    // callback current controllable insets. Adding the listener here to check if
                    // ime inset is controllable.
                    insetsController.addOnControllableInsetsChangedListener(listener);
                    if (!isImeInsetsControllable.get()) {
                        final InputMethodManager imm = (InputMethodManager) mView.getContext()
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        // This is a backport when the app is in multi-windowing mode, it cannot
                        // control the ime insets. Use the InputMethodManager instead.
                        imm.hideSoftInputFromWindow(mView.getWindowToken(), 0);
                    }
                    insetsController.removeOnControllableInsetsChangedListener(listener);
                }
                insetsController.hide(WindowInsets.Type.ime());
            } else {
                // Couldn't find an insets controller, fallback to old implementation
                super.hide();
            }
        }
    }
}
