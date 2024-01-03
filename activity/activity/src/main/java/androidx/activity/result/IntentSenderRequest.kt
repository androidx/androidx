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
package androidx.activity.result

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.IntDef

/**
 * A request for a
 * [androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult]
 * Activity Contract.
 */
@SuppressLint("BanParcelableUsage")
class IntentSenderRequest internal constructor(
    /**
     * The intentSender from this IntentSenderRequest.
     */
    val intentSender: IntentSender,
    /**
     * The intent from this IntentSender request. If non-null, this will be provided as the
     * intent parameter to IntentSender#sendIntent.
     */
    val fillInIntent: Intent? = null,
    /**
     * The flag mask from this IntentSender request.
     */
    val flagsMask: Int = 0,
    /**
     * The flag values from this IntentSender request.
     */
    val flagsValues: Int = 0,
) : Parcelable {

    @Suppress("DEPRECATION")
    internal constructor(parcel: Parcel) : this(
        parcel.readParcelable(IntentSender::class.java.classLoader)!!,
        parcel.readParcelable(Intent::class.java.classLoader),
        parcel.readInt(),
        parcel.readInt()
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(intentSender, flags)
        dest.writeParcelable(fillInIntent, flags)
        dest.writeInt(flagsMask)
        dest.writeInt(flagsValues)
    }

    /**
     * A builder for constructing [IntentSenderRequest] instances.
     */
    class Builder(private val intentSender: IntentSender) {
        private var fillInIntent: Intent? = null
        private var flagsMask = 0
        private var flagsValues = 0

        /**
         * Convenience constructor that takes an [PendingIntent] and uses
         * its [IntentSender].
         *
         * @param pendingIntent the pendingIntent containing with the intentSender to go in the
         * IntentSenderRequest.
         */
        constructor(pendingIntent: PendingIntent) : this(pendingIntent.intentSender)

        @IntDef(
            flag = true,
            value = [
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                Intent.FLAG_FROM_BACKGROUND,
                Intent.FLAG_DEBUG_LOG_RESOLUTION,
                Intent.FLAG_EXCLUDE_STOPPED_PACKAGES,
                Intent.FLAG_INCLUDE_STOPPED_PACKAGES,
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
                Intent.FLAG_ACTIVITY_MATCH_EXTERNAL,
                Intent.FLAG_ACTIVITY_NO_HISTORY,
                Intent.FLAG_ACTIVITY_SINGLE_TOP,
                Intent.FLAG_ACTIVITY_NEW_TASK,
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
                Intent.FLAG_ACTIVITY_CLEAR_TOP,
                Intent.FLAG_ACTIVITY_FORWARD_RESULT,
                Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP,
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
                Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT,
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED,
                Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY,
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT,
                Intent.FLAG_ACTIVITY_NO_USER_ACTION,
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
                Intent.FLAG_ACTIVITY_NO_ANIMATION,
                Intent.FLAG_ACTIVITY_CLEAR_TASK,
                Intent.FLAG_ACTIVITY_TASK_ON_HOME,
                Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS,
                Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
            ]
        )
        @Retention(AnnotationRetention.SOURCE)
        private annotation class Flag

        /**
         * Set the intent for the [IntentSenderRequest].
         *
         * @param fillInIntent intent to go in the IntentSenderRequest. If non-null, this
         * will be provided as the intent parameter to IntentSender#sendIntent.
         * @return This builder.
         */
        fun setFillInIntent(fillInIntent: Intent?): Builder {
            this.fillInIntent = fillInIntent
            return this
        }

        /**
         * Set the flag mask and flag values for the [IntentSenderRequest].
         *
         * @param values flagValues to go in the IntentSenderRequest. Desired values for any bits
         * set in flagsMask
         * @param mask mask to go in the IntentSenderRequest. Intent flags in the original
         * IntentSender that you would like to change.
         *
         * @return This builder.
         */
        fun setFlags(@Flag values: Int, mask: Int): Builder {
            flagsValues = values
            flagsMask = mask
            return this
        }

        /**
         * Build the IntentSenderRequest specified by this builder.
         *
         * @return the newly constructed IntentSenderRequest.
         */
        fun build(): IntentSenderRequest {
            return IntentSenderRequest(intentSender, fillInIntent, flagsMask, flagsValues)
        }
    }

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR: Parcelable.Creator<IntentSenderRequest> =
            object : Parcelable.Creator<IntentSenderRequest> {
                override fun createFromParcel(inParcel: Parcel): IntentSenderRequest {
                    return IntentSenderRequest(inParcel)
                }

                override fun newArray(size: Int): Array<IntentSenderRequest?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
