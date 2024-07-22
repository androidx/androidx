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

package androidx.biometric;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.concurrent.Executor;

/**
 * Utility class for creating and converting between different types of prompt content view that may
 * be used internally by {@link BiometricPrompt}
 */
class PromptContentViewUtils {
    // Prevent instantiation.
    private PromptContentViewUtils() {
    }

    /**
     * Wraps a prompt content view to be passed to {@link BiometricPrompt}.
     *
     * @param contentView               An instance of {@link PromptContentView}.
     * @param executor                  An executor for the more options button callback.
     * @param moreOptionsButtonListener A listener for the more options button press event.
     * @return An equivalent prompt content view that is compatible with
     * {@link android.hardware.biometrics.PromptContentView}.
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Nullable
    static android.hardware.biometrics.PromptContentView wrapForBiometricPrompt(
            @Nullable PromptContentView contentView, @NonNull Executor executor,
            @NonNull DialogInterface.OnClickListener moreOptionsButtonListener) {

        if (contentView == null) {
            return null;
        }

        // Prompt content view is only supported on API 35 and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            if (contentView instanceof PromptVerticalListContentView) {
                return Api35Impl.createPromptVerticalListContentView(
                        (PromptVerticalListContentView) contentView);
            } else if (contentView instanceof PromptContentViewWithMoreOptionsButton) {
                return Api35Impl.createPromptContentViewWithMoreOptionsButton(
                        (PromptContentViewWithMoreOptionsButton) contentView, executor,
                        moreOptionsButtonListener);
            }
        }

        return null;
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 15.0 (API 35).
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @SuppressLint("MissingPermission")
    private static class Api35Impl {
        // Prevent instantiation.
        private Api35Impl() {
        }

        /**
         * Creates an instance of the framework class
         * {@link android.hardware.biometrics.PromptVerticalListContentView} from the given
         * content view.
         *
         * @param contentView The prompt content view to be wrapped.
         * @return An instance of {@link android.hardware.biometrics.PromptVerticalListContentView}.
         */
        @NonNull
        static android.hardware.biometrics.PromptContentView createPromptVerticalListContentView(
                @NonNull PromptVerticalListContentView contentView) {
            android.hardware.biometrics.PromptVerticalListContentView.Builder
                    contentViewBuilder =
                    new android.hardware.biometrics.PromptVerticalListContentView.Builder();
            if (contentView.getDescription() != null) {
                contentViewBuilder.setDescription(contentView.getDescription());
            }
            contentView.getListItems().forEach(
                    it -> {
                        if (it instanceof PromptContentItemPlainText) {
                            contentViewBuilder.addListItem(
                                    new android.hardware.biometrics.PromptContentItemPlainText(
                                            ((PromptContentItemPlainText) it).getText()));
                        } else if (it instanceof PromptContentItemBulletedText) {
                            contentViewBuilder.addListItem(
                                    new android.hardware.biometrics.PromptContentItemBulletedText(
                                            ((PromptContentItemBulletedText) it).getText()));
                        }
                    });
            return contentViewBuilder.build();
        }

        /**
         * Creates an instance of the framework class
         * {@link android.hardware.biometrics.PromptContentViewWithMoreOptionsButton} from the
         * given content view.
         *
         * @param contentView               The prompt content view to be wrapped.
         * @param executor                  An executor for the more options button callback.
         * @param moreOptionsButtonListener A listener for the more options button press event.
         * @return An instance of
         * {@link android.hardware.biometrics.PromptContentViewWithMoreOptionsButton}.
         */
        @NonNull
        static android.hardware.biometrics.PromptContentView
                createPromptContentViewWithMoreOptionsButton(
                        @NonNull PromptContentViewWithMoreOptionsButton contentView,
                        @NonNull Executor executor,
                        @NonNull DialogInterface.OnClickListener moreOptionsButtonListener) {
            android.hardware.biometrics.PromptContentViewWithMoreOptionsButton.Builder
                    contentViewBuilder =
                    new android.hardware.biometrics.PromptContentViewWithMoreOptionsButton
                            .Builder();
            if (contentView.getDescription() != null) {
                contentViewBuilder.setDescription(contentView.getDescription());
            }
            contentViewBuilder.setMoreOptionsButtonListener(executor, moreOptionsButtonListener);
            return contentViewBuilder.build();
        }
    }
}
