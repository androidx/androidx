package androidx.wear.ongoingactivity

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Icon
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.content.LocusIdCompat

/**
 * Main class to access the Ongoing Activities API.
 *
 * It's created with the [Builder]. After it's created (and before building and
 * posting the [Notification]) [apply] needs to be called:
 * ```
 * val builder = NotificationCompat.Builder(context)....
 *
 * val ongoingActivity = OngoingActivity.Builder(context, notificationId, builder)....
 * ongoingActivity.apply(context)
 *
 * notificationManager.notify(notificationId, builder.build())
 * ```
 *
 * Afterward, [update] can be used to update the status.
 *
 * If saving the [OngoingActivity] instance is not convenient, it can be recovered (after the
 * notification is posted) with [OngoingActivity.fromExistingOngoingActivity]
 */
public class OngoingActivity internal constructor (
    private val notificationId: Int,
    private val notificationBuilder: NotificationCompat.Builder,
    private val data: OngoingActivityData
) {
    /**
     * Construct a new empty [Builder], associated with the given notification.
     *
     * @param context to be used during the life of this [Builder], will NOT pass a reference
     * into the built [OngoingActivity]
     * @param notificationId id that will be used to post the notification associated with this
     * Ongoing Activity
     * @param notificationBuilder builder for the notification associated with this Ongoing Activity
     */
    public class Builder(
        private val context: Context,
        private val notificationId: Int,
        private val notificationBuilder: NotificationCompat.Builder
    ) {
        private val data = OngoingActivityData()

        /**
         * Set the animated icon that can be used on some surfaces to represent this
         * [OngoingActivity]. For example, in the WatchFace.
         * Should be white with a transparent background, preferably an AnimatedVectorDrawable.
         */
        public fun setAnimatedIcon(animatedIcon: Icon): Builder = apply {
            data.animatedIcon = animatedIcon
        }
        /**
         * Set the animated icon that can be used on some surfaces to represent this
         * [OngoingActivity]. For example, in the WatchFace.
         * Should be white with a transparent background, preferably an AnimatedVectorDrawable.
         */
        public fun setAnimatedIcon(@DrawableRes animatedIcon: Int): Builder = apply {
            data.animatedIcon = Icon.createWithResource(context, animatedIcon)
        }

        /**
         * Set the animated icon that can be used on some surfaces to represent this
         * [OngoingActivity], for example in the WatchFace in ambient mode.
         * Should be white with a transparent background, preferably an VectorDrawable.
         */
        public fun setStaticIcon(staticIcon: Icon): Builder = apply {
            data.staticIcon = staticIcon
        }
        /**
         * Set the animated icon that can be used on some surfaces to represent this
         * [OngoingActivity], for example in the WatchFace in ambient mode.
         * Should be white with a transparent background, preferably an VectorDrawable.
         */
        public fun setStaticIcon(@DrawableRes staticIcon: Int): Builder = apply {
            data.staticIcon = Icon.createWithResource(context, staticIcon)
        }

        /**
         * Set the initial status of this ongoing activity, the status may be displayed on the UI to
         * show progress of the Ongoing Activity.
         */
        public fun setStatus(status: OngoingActivityStatus): Builder = apply {
            data.status = status
        }

        /**
         * Set the intent to be used to go back to the activity when the user interacts with the
         * Ongoing Activity in other surfaces (for example, taps the Icon on the WatchFace)
         */
        public fun setTouchIntent(touchIntent: PendingIntent): Builder = apply {
            data.touchIntent = touchIntent
        }

        /**
         * Set the corresponding LocusId of this [OngoingActivity], this will be used by the
         * launcher to identify the corresponding launcher item and display it accordingly.
         */
        public fun setLocusId(locusId: LocusIdCompat): Builder = apply { data.locusId = locusId }

        /**
         * Give an id to this [OngoingActivity], as a way to reference it in
         * [fromExistingOngoingActivity]
         */
        public fun setOngoingActivityId(id: Int): Builder = apply { data.ongoingActivityId = id }

        /**
         * Combine all options provided and return a new [OngoingActivity] object.
         */
        public fun build(): OngoingActivity = OngoingActivity(
            notificationId,
            notificationBuilder,
            data
        )
    }

    /**
     * Notify the system that this activity should be shown as an Ongoing Activity.
     *
     * This will modify the notification builder associated with this Ongoing Activity, so needs
     * to be called before building and posting that notification.
     *
     * @param context May be used to access system services. A reference will not be kept after
     * this call returns.
     */
    @Suppress("UNUSED_PARAMETER")
    public fun apply(context: Context) {
        data.extend(notificationBuilder)
    }

    /**
     * Update the status of this Ongoing Activity.
     *
     * Note that this may post the notification updated with the new information.
     *
     * @param context May be used to access system services. A reference will not be kept after
     * this call returns.
     * @param status The new status of this Ongoing Activity.
     */
    public fun update(context: Context, status: OngoingActivityStatus) {
        data.status = status
        val notification = data.extendAndBuild(notificationBuilder)

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(notificationId, notification)
    }

    public companion object {
        /**
         * Convenience method for clients that don’t want to / can’t store the OngoingActivity
         * instance.
         *
         * @param context May be used to access system services. A reference will not be kept after
         * this call returns.
         * @param filter used to find the required [OngoingActivity].
         * @return the Ongoing Activity or null if not found
         */
        @JvmStatic
        public fun fromExistingOngoingActivity(
            context: Context,
            filter: ((OngoingActivityData) -> Boolean)
        ): OngoingActivity? =
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .activeNotifications
                .mapNotNull {
                    val data = OngoingActivityData.create(it.notification)
                    if (data != null && filter(data)) {
                        OngoingActivity(
                            it.id,
                            NotificationCompat.Builder(context, it.notification),
                            data
                        )
                    } else {
                        null
                    }
                }.firstOrNull()

        /**
         * Convenience method for clients that don’t want to / can’t store the OngoingActivity
         * instance.
         *
         * Note that if there is more than one Ongoing Activity active you have not guarantee
         * over which one you get, you need to use one of the other variations of this method.
         * @param context May be used to access system services. A reference will not be kept after
         * this call returns.
         * @return the Ongoing Activity or null if not found
         */
        @JvmStatic
        public fun fromExistingOngoingActivity(context: Context): OngoingActivity? =
            fromExistingOngoingActivity(context) { _ -> true }

        /**
         * Convenience method for clients that don’t want to / can’t store the OngoingActivity
         * instance.
         *
         * @param context May be used to access system services. A reference will not be kept after
         * this call returns.
         * @param ongoingActivityId the id of the Ongoing Activity to retrieve, set in
         * [OngoingActivity.Builder.setOngoingActivityId]
         * @return the Ongoing Activity or null if not found
         */
        @JvmStatic
        public fun fromExistingOngoingActivity(context: Context, ongoingActivityId: Int):
            OngoingActivity? = fromExistingOngoingActivity(context) { oae ->
                oae.ongoingActivityId == ongoingActivityId
            }
    }
}
