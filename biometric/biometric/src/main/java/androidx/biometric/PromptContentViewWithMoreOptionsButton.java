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

import static android.Manifest.permission.SET_BIOMETRIC_DIALOG_ADVANCED;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

/**
 * Contains the information of the template of content view with a more options button for
 * Biometric Prompt.
 * <p>
 * This button should be used to provide more options for sign in or other purposes, such as when a
 * user needs to select between multiple app-specific accounts or profiles that are available for
 * sign in.
 * <p>
 * Apps should avoid using this when possible because it will create additional steps that the user
 * must navigate through - clicking the more options button will dismiss the prompt, provide the app
 * an opportunity to ask the user for the correct option, and finally allow the app to decide how to
 * proceed once selected.
 *
 * <p>
 * Here's how you'd set a <code>PromptContentViewWithMoreOptionsButton</code> on a Biometric
 * Prompt:
 * <pre class="prettyprint">
 * BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
 *     .setTitle(...)
 *     .setSubTitle(...)
 *     .setContentView(
 *         new PromptContentViewWithMoreOptionsButton.Builder()
 *             .setDescription("test description")
 *             .build()
 *      )
 *     .build();
 * </pre>
 */
public final class PromptContentViewWithMoreOptionsButton implements PromptContentView {
    private final String mDescription;

    private PromptContentViewWithMoreOptionsButton(@NonNull String description) {
        mDescription = description;
    }

    /**
     * Gets the description for the content view, as set by
     * {@link PromptContentViewWithMoreOptionsButton.Builder#setDescription(String)}.
     *
     * @return The description for the content view, or null if the content view has no description.
     */
    @RequiresPermission(SET_BIOMETRIC_DIALOG_ADVANCED)
    @Nullable
    public String getDescription() {
        return mDescription;
    }

    /**
     * A builder used to set individual options for the
     * {@link PromptContentViewWithMoreOptionsButton} class.
     */
    public static final class Builder {
        private String mDescription;

        /**
         * Optional: Sets a description that will be shown on the content view.
         *
         * @param description The description to display.
         * @return This builder.
         * @throws IllegalArgumentException If description exceeds certain character limit.
         */
        @RequiresPermission(SET_BIOMETRIC_DIALOG_ADVANCED)
        @NonNull
        public Builder setDescription(@NonNull String description) {
            mDescription = description;
            return this;
        }

        /**
         * Creates a {@link PromptContentViewWithMoreOptionsButton}.
         *
         * @return An instance of {@link PromptContentViewWithMoreOptionsButton}.
         */
        @RequiresPermission(SET_BIOMETRIC_DIALOG_ADVANCED)
        @NonNull
        public PromptContentViewWithMoreOptionsButton build() {
            return new PromptContentViewWithMoreOptionsButton(mDescription);
        }
    }
}
