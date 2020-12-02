package androidx.wear.ongoingactivity

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.LocusIdCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(PatchedRobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Build.VERSION_CODES.Q])
open class OngoingActivityTest {
    private val AnimatedIconResourceId = 123
    private val StaticIconResourceId = 456
    private val LocusIdValue = LocusIdCompat("TestLocusId")
    private val OaId = 123456
    private val Status = TextOngoingActivityStatus("Basic Status")
    private val NotificationId = 4321
    private val ChannelId = "ChannelId"

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var PendingIntentValue: PendingIntent

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager
        PendingIntentValue = PendingIntent.getBroadcast(context, 0, Intent(), 0)
    }

    @Test
    fun testOngoingActivityApply() {
        val builder = NotificationCompat.Builder(context, ChannelId)
        val oa = OngoingActivity.Builder(context, 1, builder)
            .setAnimatedIcon(AnimatedIconResourceId)
            .setStaticIcon(StaticIconResourceId)
            .setLocusId(LocusIdValue)
            .setOngoingActivityId(OaId)
            .setStatus(Status)
            .setTouchIntent(PendingIntentValue)
            .build()
        oa.apply(context)

        val notification = builder.build()

        // check that the Notification contains the information needed.
        val received = OngoingActivityData.create(notification)!!
        assertEquals(AnimatedIconResourceId, received.animatedIcon!!.resId)
        assertEquals(StaticIconResourceId, received.staticIcon.resId)
        assertEquals(LocusIdValue, received.locusId)
        assertEquals(OaId, received.ongoingActivityId)
        assertEquals(Status, received.status)
        assertEquals(PendingIntentValue, received.touchIntent)
    }

    @Test
    fun testOngoingActivityUpdate() {
        val builder = NotificationCompat.Builder(context, ChannelId)
        val oa = OngoingActivity.Builder(context, NotificationId, builder)
            .setAnimatedIcon(AnimatedIconResourceId)
            .setStaticIcon(StaticIconResourceId)
            .setLocusId(LocusIdValue)
            .setOngoingActivityId(OaId)
            .setStatus(Status)
            .setTouchIntent(PendingIntentValue)
            .build()
        oa.apply(context)
        notificationManager.notify(NotificationId, builder.build())

        // After posting, send an update.
        val newStatus = TimerOngoingActivityStatus(12345)
        oa.update(context, newStatus)

        // Get the notification and check that the status, and only the status has been updated.
        val notifications = notificationManager.activeNotifications
        notifications.forEach { n -> println(n) }
        assertEquals(1, notifications.size)

        val received = OngoingActivityData.create(notifications[0].notification)!!
        assertEquals(AnimatedIconResourceId, received.animatedIcon!!.resId)
        assertEquals(StaticIconResourceId, received.staticIcon.resId)
        assertEquals(LocusIdValue, received.locusId)
        assertEquals(OaId, received.ongoingActivityId)
        assertEquals(newStatus, received.status)
        assertEquals(PendingIntentValue, received.touchIntent)

        notificationManager.cancel(NotificationId)
    }

    @Test
    fun testCreateFromExistingOngoingActivityNull() {
        // Nothing to see here.
        assertNull(OngoingActivity.fromExistingOngoingActivity(context))

        // Same, with a normal notification posted.
        val builder = NotificationCompat.Builder(context, ChannelId)
        notificationManager.notify(NotificationId, builder.build())
        assertNull(OngoingActivity.fromExistingOngoingActivity(context))

        // Clean up.
        notificationManager.cancel(NotificationId)
    }

    @Test
    fun testCreateFromExistingOngoingActivityFilter() {
        val n = 10
        // Create n Ongoing Notification & Activities.
        for (i in 1..n) {
            val builder = NotificationCompat.Builder(context, ChannelId)
            OngoingActivity.Builder(context, NotificationId + i, builder)
                .setStatus(TextOngoingActivityStatus("Ongoing Activity"))
                .setOngoingActivityId(i)
                .setStaticIcon(StaticIconResourceId)
                .setTouchIntent(PendingIntentValue)
                .build()
                .apply(context)

            notificationManager.notify(NotificationId + i, builder.build())
        }

        var statuses = mutableSetOf<String>()
        // Update them.
        for (i in 1..n) {
            val status = "New Status $i"
            statuses.add(status)
            OngoingActivity.fromExistingOngoingActivity(context, i)!!
                .update(context, TextOngoingActivityStatus(status))
        }
        assertEquals(n, statuses.size) // Just in case.

        // Get status from notifications.
        val notificationStatuses = notificationManager.activeNotifications.mapNotNull { sbn ->
            OngoingActivityData.create(sbn.notification)?.status?.getText(context, 0)
        }.toSet()

        // Check.
        assertEquals(statuses, notificationStatuses)

        // Clean up.
        notificationManager.cancelAll()
    }

    @Test
    fun testCreateFromExistingOngoingActivityUpdate() {
        val builder = NotificationCompat.Builder(context, ChannelId)
        val oa = OngoingActivity.Builder(context, NotificationId, builder)
            .setAnimatedIcon(AnimatedIconResourceId)
            .setStaticIcon(StaticIconResourceId)
            .setLocusId(LocusIdValue)
            .setOngoingActivityId(OaId)
            .setStatus(Status)
            .setTouchIntent(PendingIntentValue)
            .build()
        oa.apply(context)
        notificationManager.notify(NotificationId, builder.build())

        // After posting, send an update.
        val newStatus = TimerOngoingActivityStatus(12345)
        OngoingActivity.fromExistingOngoingActivity(context)!!.update(context, newStatus)

        // Get the notification and check that the status, and only the status has been updated.
        val notifications = notificationManager.activeNotifications
        assertEquals(1, notifications.size)

        val received = OngoingActivityData.create(notifications[0].notification)!!
        assertEquals(AnimatedIconResourceId, received.animatedIcon!!.resId)
        assertEquals(StaticIconResourceId, received.staticIcon.resId)
        assertEquals(LocusIdValue, received.locusId)
        assertEquals(OaId, received.ongoingActivityId)
        assertEquals(newStatus, received.status)
        assertEquals(PendingIntentValue, received.touchIntent)

        // Clean up.
        notificationManager.cancel(NotificationId)
    }

    @Test
    fun testDefaultsApplied() {
        // Test that icons, status and intent are taken from the notification when not specified in
        // the OngoingActivity.
        val contentText = "Default Content"
        val builder = NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(StaticIconResourceId)
            .setContentIntent(PendingIntentValue)
            .setContentText(contentText)
        val oa = OngoingActivity.Builder(context, NotificationId, builder).build()
        oa.apply(context)
        notificationManager.notify(NotificationId, builder.build())

        val notifications = notificationManager.activeNotifications
        assertEquals(1, notifications.size)
        val received = OngoingActivityData.create(notifications[0].notification)!!
        assertNull(received.animatedIcon)
        assertEquals(StaticIconResourceId, received.staticIcon.resId)
        assertEquals(contentText, received.status!!.getText(context, 0))
        assertEquals(PendingIntentValue, received.touchIntent)

        // Clean up.
        notificationManager.cancel(NotificationId)
    }

    @Test
    fun testDefaultsOverridden() {
        // Test that icons, status and intent are taken from the OngoingActivity when specified both
        // in the notification and OngoingActivity.
        val contentText = "Default Content"
        val builder = NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(StaticIconResourceId)
            .setContentIntent(PendingIntentValue)
            .setContentText(contentText)

        val newStaticIconResourceId = StaticIconResourceId + 1
        val newAnimatedIconResourceId = StaticIconResourceId + 2
        val newPendingIntentValue = PendingIntent.getBroadcast(context, 1, Intent(), 0)
        val oa = OngoingActivity.Builder(context, NotificationId, builder)
            .setAnimatedIcon(newAnimatedIconResourceId)
            .setStaticIcon(newStaticIconResourceId)
            .setStatus(Status)
            .setTouchIntent(newPendingIntentValue)
            .build()
        oa.apply(context)
        notificationManager.notify(NotificationId, builder.build())

        val notifications = notificationManager.activeNotifications
        assertEquals(1, notifications.size)
        val received = OngoingActivityData.create(notifications[0].notification)!!
        assertEquals(newAnimatedIconResourceId, received.animatedIcon!!.resId)
        assertEquals(newStaticIconResourceId, received.staticIcon.resId)
        assertEquals(Status, received.status)
        assertEquals(newPendingIntentValue, received.touchIntent)

        // Clean up.
        notificationManager.cancel(NotificationId)
    }

    @Test
    fun testHasOngoingActivity() {
        val builder = NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(StaticIconResourceId)
            .setContentIntent(PendingIntentValue)
            .setContentText("Text")
        var notification = builder.build()

        assertFalse(OngoingActivityData.hasOngoingActivity(notification))

        OngoingActivity.Builder(context, NotificationId, builder)
            .build()
            .apply(context)

        notification = builder.build()
        assertTrue(OngoingActivityData.hasOngoingActivity(notification))
    }
}
