package androidx.wear.ongoing

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
    private val BasicStatus = androidx.wear.ongoing.Status.forPart(
        Status.TextPart(
            "Basic Status"
        )
    )
    private val NotificationId = 4321
    private val ChannelId = "ChannelId"
    private val Title = "AppTitle"

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
    fun testMinimalOngoingActivity() {
        val builder = NotificationCompat.Builder(context, ChannelId)
        val oa = OngoingActivity.Builder(context, 1, builder)
            .setStaticIcon(StaticIconResourceId)
            .setTouchIntent(PendingIntentValue)
            .build()
        oa.apply(context)

        val notification = builder.build()

        // check that the Notification contains the information needed.
        val received = SerializationHelper.create(notification)!!
        assertEquals(StaticIconResourceId, received.staticIcon.resId)
        assertEquals(PendingIntentValue, received.touchIntent)
        // Also ensure that unset fields are null
        assertNull(received.animatedIcon)
        assertNull(received.locusId)
        assertNull(received.status)
    }

    @Test
    fun testOngoingActivityApply() {
        val builder = NotificationCompat.Builder(context, ChannelId)
        val oa = OngoingActivity.Builder(context, 1, builder)
            .setAnimatedIcon(AnimatedIconResourceId)
            .setStaticIcon(StaticIconResourceId)
            .setLocusId(LocusIdValue)
            .setOngoingActivityId(OaId)
            .setStatus(BasicStatus)
            .setTouchIntent(PendingIntentValue)
            .setTitle(Title)
            .build()
        oa.apply(context)

        val notification = builder.build()

        // check that the Notification contains the information needed.
        val received = SerializationHelper.create(notification)!!
        assertEquals(AnimatedIconResourceId, received.animatedIcon!!.resId)
        assertEquals(StaticIconResourceId, received.staticIcon.resId)
        assertEquals(LocusIdValue, received.locusId)
        assertEquals(OaId, received.ongoingActivityId)
        // TODO(ssancho): check status
        assertEquals(PendingIntentValue, received.touchIntent)
        assertEquals(Title, received.title)
    }

    @Test
    fun testOngoingActivityUpdate() {
        val builder = NotificationCompat.Builder(context, ChannelId)
        val oa = OngoingActivity.Builder(context, NotificationId, builder)
            .setAnimatedIcon(AnimatedIconResourceId)
            .setStaticIcon(StaticIconResourceId)
            .setLocusId(LocusIdValue)
            .setOngoingActivityId(OaId)
            .setStatus(BasicStatus)
            .setTouchIntent(PendingIntentValue)
            .setTitle(Title)
            .build()
        oa.apply(context)
        notificationManager.notify(NotificationId, builder.build())

        // After posting, send an update.
        val newStatus = androidx.wear.ongoing.Status.forPart(
            Status.StopwatchPart(
                12345
            )
        )
        oa.update(context, newStatus)

        // Get the notification and check that the status, and only the status has been updated.
        val notifications = notificationManager.activeNotifications
        assertEquals(1, notifications.size)

        val received = SerializationHelper.create(notifications[0].notification)!!
        assertEquals(AnimatedIconResourceId, received.animatedIcon!!.resId)
        assertEquals(StaticIconResourceId, received.staticIcon.resId)
        assertEquals(LocusIdValue, received.locusId)
        assertEquals(OaId, received.ongoingActivityId)
        // TODO(ssancho): check status
        assertEquals(PendingIntentValue, received.touchIntent)
        assertEquals(Title, received.title)

        notificationManager.cancel(NotificationId)
    }

    @Test
    fun testCreateFromExistingOngoingActivityNull() {
        // Nothing to see here.
        assertNull(OngoingActivity.recoverOngoingActivity(context))

        // Same, with a normal notification posted.
        val builder = NotificationCompat.Builder(context, ChannelId)
        notificationManager.notify(NotificationId, builder.build())
        assertNull(OngoingActivity.recoverOngoingActivity(context))

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
                .setStatus(Status.forPart(Status.TextPart("Ongoing Activity")))
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
            OngoingActivity.recoverOngoingActivity(context, i)!!
                .update(context, Status.forPart(Status.TextPart(status)))
        }
        assertEquals(n, statuses.size) // Just in case.

        // Get status from notifications.
        val notificationStatuses = notificationManager.activeNotifications.mapNotNull { sbn ->
            SerializationHelper.create(sbn.notification)?.status?.getText(context, 0).toString()
        }.toSet()

        // Check.
        assertEquals(statuses, notificationStatuses)

        // Clean up.
        notificationManager.cancelAll()
    }

    @Test
    fun testBlackBoxCopy() {
        val builder = NotificationCompat.Builder(context, ChannelId)
        val oa = OngoingActivity.Builder(context, 1, builder)
            .setAnimatedIcon(AnimatedIconResourceId)
            .setStaticIcon(StaticIconResourceId)
            .setLocusId(LocusIdValue)
            .setOngoingActivityId(OaId)
            .setStatus(BasicStatus)
            .setTouchIntent(PendingIntentValue)
            .setTitle(Title)
            .build()
        oa.apply(context)
        val notification = builder.build()

        // Copy the data.
        val newBundle = Bundle()
        SerializationHelper.copy(notification.extras, newBundle)

        // check that the information was copied.
        val received = SerializationHelper.create(newBundle)!!
        assertEquals(AnimatedIconResourceId, received.animatedIcon!!.resId)
        assertEquals(StaticIconResourceId, received.staticIcon.resId)
        assertEquals(LocusIdValue, received.locusId)
        assertEquals(OaId, received.ongoingActivityId)
        // TODO(ssancho): check status
        assertEquals(PendingIntentValue, received.touchIntent)
        assertEquals(Title, received.title)
    }

    @Test
    fun testCreateFromExistingOngoingActivityUpdate() {
        val builder = NotificationCompat.Builder(context, ChannelId)
        val oa = OngoingActivity.Builder(context, NotificationId, builder)
            .setAnimatedIcon(AnimatedIconResourceId)
            .setStaticIcon(StaticIconResourceId)
            .setLocusId(LocusIdValue)
            .setOngoingActivityId(OaId)
            .setStatus(BasicStatus)
            .setTouchIntent(PendingIntentValue)
            .build()
        oa.apply(context)
        notificationManager.notify(NotificationId, builder.build())

        // After posting, send an update.
        val newStatus = androidx.wear.ongoing.Status.forPart(
            Status.StopwatchPart(
                12345
            )
        )
        OngoingActivity.recoverOngoingActivity(context)!!.update(context, newStatus)

        // Get the notification and check that the status, and only the status has been updated.
        val notifications = notificationManager.activeNotifications
        assertEquals(1, notifications.size)

        val received = SerializationHelper.create(notifications[0].notification)!!
        assertEquals(AnimatedIconResourceId, received.animatedIcon!!.resId)
        assertEquals(StaticIconResourceId, received.staticIcon.resId)
        assertEquals(LocusIdValue, received.locusId)
        assertEquals(OaId, received.ongoingActivityId)
        // TODO(ssancho): check status
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
        val received = SerializationHelper.create(notifications[0].notification)!!
        assertNull(received.animatedIcon)
        assertEquals(StaticIconResourceId, received.staticIcon.resId)
        assertEquals(contentText, received.status!!.getText(context, 0).toString())
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
            .setStatus(BasicStatus)
            .setTouchIntent(newPendingIntentValue)
            .build()
        oa.apply(context)
        notificationManager.notify(NotificationId, builder.build())

        val notifications = notificationManager.activeNotifications
        assertEquals(1, notifications.size)
        val received = SerializationHelper.create(notifications[0].notification)!!
        assertEquals(newAnimatedIconResourceId, received.animatedIcon!!.resId)
        assertEquals(newStaticIconResourceId, received.staticIcon.resId)
        // TODO(ssancho): check status
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

        assertFalse(SerializationHelper.hasOngoingActivity(notification))

        OngoingActivity.Builder(context, NotificationId, builder)
            .build()
            .apply(context)

        notification = builder.build()
        assertTrue(SerializationHelper.hasOngoingActivity(notification))
    }

    @Test
    fun testTagSupport() {
        val tag = "TAG"
        val builder1 = NotificationCompat.Builder(context, ChannelId)
        val oa1 = OngoingActivity.Builder(context, NotificationId, builder1)
            .setStaticIcon(StaticIconResourceId)
            .setStatus(Status.forPart(Status.TextPart("status1")))
            .setOngoingActivityId(1)
            .setTouchIntent(PendingIntentValue)
            .build()
        oa1.apply(context)
        notificationManager.notify(NotificationId, builder1.build())

        val builder2 = NotificationCompat.Builder(context, ChannelId)
        val oa2 = OngoingActivity.Builder(context, tag, NotificationId, builder2)
            .setStaticIcon(StaticIconResourceId)
            .setStatus(Status.forPart(Status.TextPart("status2")))
            .setOngoingActivityId(2)
            .setTouchIntent(PendingIntentValue)
            .build()
        oa2.apply(context)
        notificationManager.notify(tag, NotificationId, builder2.build())

        assertEquals(2, notificationManager.activeNotifications.size)

        // After posting, send an update to the second OA and check the statuses.
        val newStatus2 = androidx.wear.ongoing.Status.forPart(
            Status.TextPart(
                "update2"
            )
        )
        OngoingActivity.recoverOngoingActivity(context, 2)?.update(context, newStatus2)

        assertEquals("status1, update2", getStatuses())

        // Update the first OA, and check the statuses.
        val newStatus1 = androidx.wear.ongoing.Status.forPart(
            Status.TextPart(
                "updated-one"
            )
        )
        oa1.update(context, newStatus1)

        assertEquals("updated-one, update2", getStatuses())

        // Clean up.
        notificationManager.cancel(NotificationId)
        notificationManager.cancel(tag, NotificationId)

        assertEquals(0, notificationManager.activeNotifications.size)
    }

    private fun getStatuses(): String =
        notificationManager.activeNotifications
            .mapNotNull { SerializationHelper.create(it.notification) }
            .sortedBy { it.ongoingActivityId }
            .joinToString { it.status?.getText(context, 0L).toString() }
}
