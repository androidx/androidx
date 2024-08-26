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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the information of the template of vertical list content view for Biometric Prompt.
 * <p>
 * Here's how you'd set a <code>PromptVerticalListContentView</code> on a Biometric Prompt:
 * <pre class="prettyprint">
 * BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
 *     .setTitle(...)
 *     .setSubTitle(...)
 *     .setContentView(
 *         new PromptVerticalListContentView.Builder()
 *             .setDescription("test description")
 *             .addListItem(new PromptContentItemPlainText("test item 1"))
 *             .addListItem(new PromptContentItemPlainText("test item 2"))
 *             .addListItem(new PromptContentItemBulletedText("test item 3"))
 *             .build()
 *      )
 *     .build();
 * </pre>
 */
public final class PromptVerticalListContentView implements PromptContentView {
    private final List<PromptContentItem> mContentList;
    private final String mDescription;

    private PromptVerticalListContentView(@NonNull List<PromptContentItem> contentList,
            @NonNull String description) {
        mContentList = contentList;
        mDescription = description;
    }

    /**
     * Gets the description for the content view, as set by
     * {@link PromptVerticalListContentView.Builder#setDescription(String)}.
     *
     * @return The description for the content view, or null if the content view has no description.
     */
    @Nullable
    public String getDescription() {
        return mDescription;
    }

    /**
     * Gets the list of items on the content view, as set by
     * {@link PromptVerticalListContentView.Builder#addListItem(PromptContentItem)}.
     *
     * @return The item list on the content view.
     */
    @NonNull
    public List<PromptContentItem> getListItems() {
        return new ArrayList<>(mContentList);
    }

    /**
     * A builder used to set individual options for the {@link PromptVerticalListContentView} class.
     */
    public static final class Builder {
        private final List<PromptContentItem> mContentList = new ArrayList<>();
        private String mDescription;

        /**
         * Optional: Sets a description that will be shown on the content view.
         *
         * @param description The description to display.
         * @return This builder.
         * @throws IllegalArgumentException If description exceeds certain character limit.
         */
        @NonNull
        public Builder setDescription(@NonNull String description) {
            mDescription = description;
            return this;
        }

        /**
         * Optional: Adds a list item in the current row.
         *
         * @param listItem The list item view to display
         * @return This builder.
         * @throws IllegalArgumentException If this list item exceeds certain character limits or
         *                                  the number of list items exceeds certain limit.
         */
        @NonNull
        public Builder addListItem(@NonNull PromptContentItem listItem) {
            mContentList.add(listItem);
            return this;
        }

        /**
         * Optional: Adds a list item in the current row.
         *
         * @param listItem The list item view to display
         * @param index    The position at which to add the item
         * @return This builder.
         * @throws IllegalArgumentException If this list item exceeds certain character limits or
         *                                  the number of list items exceeds certain limit.
         */
        @NonNull
        public Builder addListItem(@NonNull PromptContentItem listItem, int index) {
            mContentList.add(index, listItem);
            return this;
        }


        /**
         * Creates a {@link PromptVerticalListContentView}.
         *
         * @return An instance of {@link PromptVerticalListContentView}.
         */
        @NonNull
        public PromptVerticalListContentView build() {
            return new PromptVerticalListContentView(mContentList, mDescription);
        }
    }
}

