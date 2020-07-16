/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.autofill.inline;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.List;

/**
 * Contains all the officially supported Autofill inline suggestion hint constants.
 *
 * <p>Set through the inline suggestion content builder, such as
 * {@link androidx.autofill.inline.v1.InlineSuggestionUi.Content.Builder#setHints(List)}
 * to provide hints about the type of the suggestion content. The remote process that shows the
 * inline suggestion may not have access to the suggestion content, but it can use the
 * associated hints to help rank and evaluate the quality of the suggestion based on user action
 * on them.
 */
@RequiresApi(api = Build.VERSION_CODES.R)
public final class SuggestionHintConstants {
    private SuggestionHintConstants() {}

    /**
     * Hint indicating that the suggestion is from clipboard content.
     */
    public static final String SUGGESTION_HINT_CLIPBOARD_CONTENT = "clipboardContent";

    /**
     * Hint indicating that the suggestion is a smart reply to a conversation.
     */
    public static final String SUGGESTION_HINT_SMART_REPLY = "smartReply";
}
