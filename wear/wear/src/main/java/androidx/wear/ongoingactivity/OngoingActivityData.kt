package androidx.wear.ongoingactivity

import android.app.Notification
import android.app.PendingIntent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.app.NotificationCompat
import androidx.core.content.LocusIdCompat

/**
 * This class is used internally by the library to represent the data of an OngoingActivity.
 */
public class OngoingActivityData {
    /** See [OngoingActivity.Builder.setAnimatedIcon] */
    public var animatedIcon: Icon? = null

    /** See [OngoingActivity.Builder.setStaticIcon] */
    public var staticIcon: Icon? = null

    /** See [OngoingActivity.Builder.setStatus] */
    public var status: OngoingActivityStatus? = null

    /** See [OngoingActivity.Builder.setTouchIntent] */
    public var touchIntent: PendingIntent? = null

    /** See [OngoingActivity.Builder.setLocusId] */
    public var locusId: LocusIdCompat? = null

    /** See [OngoingActivity.Builder.setOngoingActivityId] */
    public var ongoingActivityId: Int = DEFAULT_ID

    public constructor() {
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public fun extend(builder: NotificationCompat.Builder): NotificationCompat.Builder =
        builder.apply {
            val bundle = Bundle()
            animatedIcon?.let { bundle.putParcelable(KEY_ANIMATED_ICON, it) }
            staticIcon?.let { bundle.putParcelable(KEY_STATIC_ICON, it) }
            status?.let { it.extend(bundle) }
            touchIntent?.let { bundle.putParcelable(KEY_TOUCH_INTENT, it) }
            locusId?.let { builder.setLocusId(locusId) }
            if (ongoingActivityId != DEFAULT_ID) {
                bundle.putInt(KEY_ID, ongoingActivityId)
            }
            extras.putBundle(EXTRA_ONGOING_ACTIVITY, bundle)
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public fun extendAndBuild(builder: NotificationCompat.Builder): Notification {
        return extend(builder).build().apply {
            // TODO(http://b/169394642): Undo this if/when the bug is fixed.
            extras.putBundle(
                EXTRA_ONGOING_ACTIVITY,
                builder.extras.getBundle(EXTRA_ONGOING_ACTIVITY)
            )
        }
    }

    public companion object {
        /**
         * Internal version that deserializes the [OngoingActivityData] from a notification.
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public fun createInternal(notification: Notification): OngoingActivityData? =
            (notification.extras[EXTRA_ONGOING_ACTIVITY] as? Bundle)?.let { bundle ->
                OngoingActivityData().apply {
                    animatedIcon = bundle[KEY_ANIMATED_ICON] as? Icon
                    staticIcon = bundle[KEY_STATIC_ICON] as? Icon
                    status = OngoingActivityStatus.create(bundle)
                    touchIntent = bundle[KEY_TOUCH_INTENT] as? PendingIntent
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        locusId = Api29Impl.getLocusId(notification)
                    }
                    ongoingActivityId = bundle.getInt(KEY_ID, DEFAULT_ID)
                }
            }

        /**
         * Deserializes the [OngoingActivityData] from a notification.
         *
         * Applies defaults from the notification for information not provided as part of the
         * [OngoingActivity].
         *
         * @param notification the notification that may contain information about a Ongoing
         * Activity.
         * @return the data, or null of the notification doesn't contain Ongoing Activity data.
         */
        @JvmStatic
        public fun create(notification: Notification): OngoingActivityData? =
            createInternal(notification)?.apply {
                if (animatedIcon == null) {
                    animatedIcon = notification.smallIcon
                }
                if (staticIcon == null) {
                    staticIcon = notification.smallIcon
                }
                if (status == null) {
                    status = notification.extras.getString(Notification.EXTRA_TEXT)?.let {
                        TextOngoingActivityStatus(it)
                    }
                }
                if (touchIntent == null) {
                    touchIntent = notification.contentIntent
                }
            }
    }

    // Inner class required to avoid VFY errors during class init.
    @RequiresApi(29)
    private object Api29Impl {
        @JvmStatic
        fun getLocusId(notification: Notification): LocusIdCompat? =
            notification.locusId?.let { LocusIdCompat.toLocusIdCompat(it) }
    }
}

/** Notification action extra which contains ongoing activity extensions  */
private const val EXTRA_ONGOING_ACTIVITY = "android.wearable.ongoingactivities.EXTENSIONS"

// Keys within EXTRA_ONGOING_ACTIVITY_EXTENDER for ongoing activities options.
private const val KEY_ANIMATED_ICON = "animatedIcon"
private const val KEY_STATIC_ICON = "staticIcon"
private const val KEY_TOUCH_INTENT = "touchIntent"
private const val KEY_ID = "id"

private const val DEFAULT_ID = -1