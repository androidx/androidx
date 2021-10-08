/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.input

import android.app.RemoteInput
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.annotation.VisibleForTesting

/**
 * Extender for Wear-specific extras for a [RemoteInput] instance.
 *
 * For example, to create a RemoteInput that will allow free form input (e.g. voice input on Wear),
 * but not show the Draw Emoji option:
 *
 * @sample androidx.wear.input.samples.extenderSample
 */
public class WearableRemoteInputExtender(private var remoteInput: RemoteInput.Builder) {
    private var extras = Bundle()

    /**
     * Adding extra to a [RemoteInput] for allowing or disallowing showing emoji-only options (e.g.
     * the Draw Emoji option).
     *
     * If set to false, the Draw Emoji option will not be shown. If set to true or not set, the
     * Draw Emoji option will be shown as long as the [RemoteInput] allows free form input.
     *
     * @param emojisAllowed Whether the emoji-only options is shown. If not set, it will be allowed.
     */
    public fun setEmojisAllowed(emojisAllowed: Boolean): WearableRemoteInputExtender =
        apply { extras.putBoolean(EXTRA_DISALLOW_EMOJI, !emojisAllowed) }

    /**
     * Adding specified input action type to a [RemoteInput] to modify the action type of the
     * RemoteInput session (e.g. "send" or "search"). The default action type is "send."
     *
     * @param imeActionType Action type to be set on RemoteInput session. Should be one of the
     * following values: [EditorInfo.IME_ACTION_SEND], [EditorInfo.IME_ACTION_SEARCH],
     * [EditorInfo.IME_ACTION_DONE], [EditorInfo.IME_ACTION_GO]. If not, send action will be set.
     */
    public fun setInputActionType(imeActionType: Int): WearableRemoteInputExtender = apply {
        extras.putInt(
            EXTRA_INPUT_ACTION_TYPE, getInputActionTypeForImeActionType(imeActionType)
        )
    }

    /**
     * Returns the [RemoteInput.Builder] with set options.
     */
    public fun get(): RemoteInput.Builder = remoteInput.addExtras(extras)

    @VisibleForTesting
    internal companion object {
        /**
         * Key for a boolean extra that can be added to a [RemoteInput] to cause emoji-only
         * options (e.g. the Draw Emoji option) to not be shown.
         *
         * If this extra has value true, the Draw Emoji option will not be shown. If this extra
         * is not present or has any other value, the Draw Emoji option will be shown as long as
         * the [RemoteInput] allows free form input.
         */
        @VisibleForTesting
        internal const val EXTRA_DISALLOW_EMOJI =
            "android.support.wearable.input.extra.DISALLOW_EMOJI"

        /**
         * Key for an integer extra that can be added to a [RemoteInput] to modify the action
         * type of the RemoteInput session (e.g. "send" or "search"). The default action type is
         * "send."
         */
        @VisibleForTesting
        internal const val EXTRA_INPUT_ACTION_TYPE =
            "android.support.wearable.input.extra.INPUT_ACTION_TYPE"

        /** Send action type. */
        @VisibleForTesting
        internal const val INPUT_ACTION_TYPE_SEND = 0

        /** Search action type. */
        @VisibleForTesting
        internal const val INPUT_ACTION_TYPE_SEARCH = 1

        /** Done action type. */
        @VisibleForTesting
        internal const val INPUT_ACTION_TYPE_DONE = 2

        /** Go action type. */
        @VisibleForTesting
        internal const val INPUT_ACTION_TYPE_GO = 3

        internal fun getInputActionTypeForImeActionType(imeActionType: Int) = when (imeActionType) {
            EditorInfo.IME_ACTION_SEND -> INPUT_ACTION_TYPE_SEND
            EditorInfo.IME_ACTION_SEARCH -> INPUT_ACTION_TYPE_SEARCH
            EditorInfo.IME_ACTION_DONE -> INPUT_ACTION_TYPE_DONE
            EditorInfo.IME_ACTION_GO -> INPUT_ACTION_TYPE_GO
            // If any other action is passed which is not support on Wear OS, use the default
            // send action.
            else -> INPUT_ACTION_TYPE_SEND
        }
    }
}

public fun RemoteInput.Builder.wearableExtender(block: WearableRemoteInputExtender.() -> Unit):
    RemoteInput.Builder =
        WearableRemoteInputExtender(this).apply { block() }.get()