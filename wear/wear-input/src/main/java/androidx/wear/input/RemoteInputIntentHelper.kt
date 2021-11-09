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
package androidx.wear.input

import android.app.RemoteInput
import android.content.Intent
import android.os.Build
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi

/**
 * Helper functions for supporting remote inputs through starting an [android.content.Intent].
 *
 *
 * The following example prompts the user to provide input for one `RemoteInput` by
 * starting an input activity.
 *
 * ```
 * public const val KEY_QUICK_REPLY_TEXT: String = "quick_reply";
 * val remoteInputs: List<RemoteInput> = listOf(
 *     new RemoteInput.Builder(KEY_QUICK_REPLY_TEXT).setLabel("Quick reply").build()
 * );
 * val intent: Intent = createActionRemoteInputIntent();
 * putRemoteInputsExtra(intent, remoteInputs)
 * startActivityForResult(intent);
 * ```
 *
 * The intent returned via [android.app.Activity.onActivityResult] will contain the input results if
 * collected, for example:
 *
 * ```
 * override fun onActivityResult(requestCode: Int, resultCode: Int, intentResults: Intent?) {
 *     val results: Bundle = RemoteInput.getResultsFromIntent(intentResults)
 *     val quickReplyResult: CharSequence? = results.getCharSequence(KEY_QUICK_REPLY_TEXT)
 * }
 * ```
 *
 * More information about accessing these results can be found in [RemoteInput].
 */
@RequiresApi(Build.VERSION_CODES.N)
public class RemoteInputIntentHelper private constructor() {
    public companion object {
        private const val ACTION_REMOTE_INPUT: String =
            "android.support.wearable.input.action.REMOTE_INPUT"

        private const val EXTRA_REMOTE_INPUTS: String =
            "android.support.wearable.input.extra.REMOTE_INPUTS"

        private const val EXTRA_TITLE: String = "android.support.wearable.input.extra.TITLE"

        private const val EXTRA_CANCEL_LABEL: String =
            "android.support.wearable.input.extra.CANCEL_LABEL"

        private const val EXTRA_CONFIRM_LABEL: String =
            "android.support.wearable.input.extra.CONFIRM_LABEL"

        private const val EXTRA_IN_PROGRESS_LABEL: String =
            "android.support.wearable.input.extra.IN_PROGRESS_LABEL"

        private const val EXTRA_SMART_REPLY_CONTEXT: String =
            "android.support.wearable.input.extra.SMART_REPLY_CONTEXT"

        /**
         * Create an intent with action for remote input. This intent can be used to start an
         * activity that will prompt the user for input. With the other helpers in this class to
         * specify the intent extras, we can configure the behaviour of the input activity, such as
         * specifying input be collected from a user by populating with an array of [RemoteInput]
         * with [putRemoteInputsExtra].
         *
         * @return The created intent with action for remote input.
         */
        @JvmStatic
        @NonNull
        public fun createActionRemoteInputIntent(): Intent = Intent(ACTION_REMOTE_INPUT)

        /**
         * Checks whether the action of the given intent is for remote input.
         */
        @JvmStatic
        public fun isActionRemoteInput(intent: Intent): Boolean =
            intent.action == ACTION_REMOTE_INPUT

        /**
         * Returns the array of [RemoteInput] from the given [Intent] that specifies inputs to be
         * collected from a user. Should be used with [Intent] created with [
         * .createActionRemoteInputIntent].
         *
         * @param intent The intent with given data.
         * @return The array of [RemoteInput] previously added with [putRemoteInputsExtra] or null
         * which means no user input required.
         */
        @JvmStatic
        @Nullable
        public fun getRemoteInputsExtra(intent: Intent): List<RemoteInput>? =
            intent.getParcelableArrayExtra(EXTRA_REMOTE_INPUTS)?.map { it as RemoteInput }

        /**
         * Checks whether the given [Intent] has extra for the array of [RemoteInput].
         */
        @JvmStatic
        public fun hasRemoteInputsExtra(intent: Intent): Boolean =
            intent.hasExtra(EXTRA_REMOTE_INPUTS)

        /**
         * Adds the array of [RemoteInput] to the given [Intent] that specifies inputs collected
         * from a user. Should be used with [Intent] created with [createActionRemoteInputIntent].
         *
         * @param intent The intent with given data.
         * @param remoteInputs The array of [RemoteInput] to be added.
         */
        @JvmStatic
        @NonNull
        public fun putRemoteInputsExtra(
            intent: Intent,
            remoteInputs: List<RemoteInput>
        ): Intent = intent.putExtra(EXTRA_REMOTE_INPUTS, remoteInputs.toTypedArray())

        /**
         * Returns the [CharSequence] from the given [Intent] that specifies what is displayed on
         * top of the confirmation screen to describe the action.
         *
         * @param intent The intent with given data.
         * @return The CharSequence previously added with [putTitleExtra] or null if no value is
         * found.
         */
        @JvmStatic
        @Nullable
        public fun getTitleExtra(intent: Intent): CharSequence? =
            intent.getCharSequenceExtra(EXTRA_TITLE)

        /**
         * Adds the [CharSequence] to the given [Intent] that specifies what is displayed on top of
         * the confirmation screen to describe the action like "SMS" or "Email".
         *
         * @param intent The intent with given data.
         * @param title The CharSequence to be added.
         * @return The given intent.
         */
        @JvmStatic
        @NonNull
        public fun putTitleExtra(intent: Intent, title: CharSequence): Intent =
            intent.putExtra(EXTRA_TITLE, title)

        /**
         * Returns the [CharSequence] from the given [Intent] that specifies what is displayed to
         * cancel the action.
         *
         * @param intent The intent with given data.
         * @return The CharSequence previously added with [putCancelLabelExtra] or null if no value
         * is found.
         */
        @JvmStatic
        @Nullable
        public fun getCancelLabelExtra(intent: Intent): CharSequence? =
            intent.getCharSequenceExtra(EXTRA_CANCEL_LABEL)

        /**
         * Adds the [CharSequence] to the given [Intent] that specifies what is displayed to cancel
         * the action. This is usually an imperative verb, like "Cancel". Defaults to Cancel.
         *
         * @param intent The intent with given data.
         * @param label The CharSequence to be added.
         * @return The given intent.
         */
        @JvmStatic
        @NonNull
        public fun putCancelLabelExtra(intent: Intent, label: CharSequence): Intent =
            intent.putExtra(EXTRA_CANCEL_LABEL, label)

        /**
         * Returns the [CharSequence] from the given [Intent] that specifies what is displayed to
         * confirm that the action should be executed.
         *
         * @param intent The intent with given data.
         * @return The CharSequence previously added with [putConfirmLabelExtra] or null if no value
         * is found.
         */
        @JvmStatic
        @Nullable
        public fun getConfirmLabelExtra(intent: Intent): CharSequence? =
            intent.getCharSequenceExtra(EXTRA_CONFIRM_LABEL)

        /**
         * Adds the [CharSequence] to the given [Intent] that specifies what is displayed to confirm
         * that the action should be executed. This is usually an imperative verb like "Send".
         * Defaults to "Send".
         *
         * @param intent The intent with given data.
         * @param label The CharSequence to be added.
         * @return The given intent.
         */
        @JvmStatic
        @NonNull
        public fun putConfirmLabelExtra(intent: Intent, label: CharSequence): Intent =
            intent.putExtra(EXTRA_CONFIRM_LABEL, label)

        /**
         * Returns the [CharSequence] from the given [Intent] that specifies what is displayed while
         * the wearable is preparing to automatically execute the action.
         *
         * @param intent The intent with given data.
         * @return The CharSequence previously added with [putInProgressLabelExtra] or null if no
         * value is found.
         */
        @JvmStatic
        @Nullable
        public fun getInProgressLabelExtra(intent: Intent): CharSequence? =
            intent.getCharSequenceExtra(EXTRA_IN_PROGRESS_LABEL)

        /**
         * Adds the [CharSequence] to the given [Intent] that specifies what is displayed while the
         * wearable is preparing to automatically execute the action. This is usually a 'ing'
         * verb ending in ellipsis like "Sending...". Defaults to "Sending...".
         *
         * @param intent The intent with given data.
         * @param label The CharSequence to be added.
         * @return The given intent.
         */
        @JvmStatic
        @NonNull
        public fun putInProgressLabelExtra(intent: Intent, label: CharSequence): Intent =
            intent.putExtra(EXTRA_IN_PROGRESS_LABEL, label)

        /**
         * Returns the array of [CharSequence] from the given [Intent] that provides context for
         * creating Smart Reply choices within a RemoteInput session.
         *
         * @param intent The intent with given data.
         * @return The array of [CharSequence] previously added with [putSmartReplyContextExtra] or
         * [putSmartReplyContextExtra] or null if no value is found.
         */
        @JvmStatic
        @Nullable
        public fun getSmartReplyContextExtra(intent: Intent): List<CharSequence>? =
            intent.getCharSequenceArrayListExtra(EXTRA_SMART_REPLY_CONTEXT)

        /**
         * Adds the array of [CharSequence] to the given [Intent] that provides context for
         * creating Smart Reply choices within a RemoteInput session. The context should be
         * incoming chat messages that a user will reply to using RemoteInput. Only incoming
         * messages (messages from other users) should be passed via this extra.
         *
         * The messages should be in the order that they were received with the newest messages
         * at the highest index of the CharSequence[]. For example, a possible value for this
         * extra would be: ["hey", "where are you?"]. In this case, "where are you?" was the most
         * recently received message.
         *
         * Passing a chat context into RemoteInput using this method does not guarantee that Smart
         * Reply choices will be shown to a user.
         *
         * @param intent The intent with given data.
         * @param smartReplyContext The string to be added.
         * @return The given intent.
         */
        @JvmStatic
        @NonNull
        public fun putSmartReplyContextExtra(
            intent: Intent,
            smartReplyContext: List<CharSequence>
        ): Intent = intent.putExtra(EXTRA_SMART_REPLY_CONTEXT, ArrayList(smartReplyContext))
    }
}