/*
 * Copyright (C) 2012 The Android Open Source Project
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

package androidx.core.app;

import static androidx.annotation.Dimension.DP;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import static java.lang.annotation.RetentionPolicy.SOURCE;
import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.LocusId;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DimenRes;
import androidx.annotation.Dimension;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.R;
import androidx.core.content.ContextCompat;
import androidx.core.content.LocusIdCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.os.BundleCompat;
import androidx.core.text.BidiFormatter;
import androidx.core.view.GravityCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Helper for accessing features in {@link android.app.Notification}.
 */
public class NotificationCompat {
    private static final String TAG = "NotifCompat";

    /**
     * An activity that provides a user interface for adjusting notification preferences for its
     * containing application.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String INTENT_CATEGORY_NOTIFICATION_PREFERENCES =
            "android.intent.category.NOTIFICATION_PREFERENCES";

    /**
     * Optional extra for {@link #INTENT_CATEGORY_NOTIFICATION_PREFERENCES}. If provided, will
     * contain a {@link NotificationChannelCompat#getId() channel id} that can be used to narrow
     * down what settings should be shown in the target app.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_CHANNEL_ID = "android.intent.extra.CHANNEL_ID";

    /**
     * Optional extra for {@link #INTENT_CATEGORY_NOTIFICATION_PREFERENCES}. If provided, will
     * contain a {@link NotificationChannelGroupCompat#getId() group id} that can be used to narrow
     * down what settings should be shown in the target app.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_CHANNEL_GROUP_ID = "android.intent.extra.CHANNEL_GROUP_ID";

    /**
     * Optional extra for {@link #INTENT_CATEGORY_NOTIFICATION_PREFERENCES}. If provided, will
     * contain the tag provided to
     * {@link NotificationManagerCompat#notify(String, int, Notification)}
     * that can be used to narrow down what settings should be shown in the target app.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_NOTIFICATION_TAG = "android.intent.extra.NOTIFICATION_TAG";

    /**
     * Optional extra for {@link #INTENT_CATEGORY_NOTIFICATION_PREFERENCES}. If provided, will
     * contain the id provided to
     * {@link NotificationManagerCompat#notify(String, int, Notification)}
     * that can be used to narrow down what settings should be shown in the target app.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_NOTIFICATION_ID = "android.intent.extra.NOTIFICATION_ID";

    /**
     * Use all default values (where applicable).
     */
    public static final int DEFAULT_ALL = ~0;

    /**
     * Use the default notification sound. This will ignore any sound set using
     * {@link Builder#setSound}
     *
     * <p>
     * A notification that is noisy is more likely to be presented as a heads-up notification,
     * on some platforms.
     * </p>
     *
     * @see Builder#setDefaults
     */
    public static final int DEFAULT_SOUND = 1;

    /**
     * Use the default notification vibrate. This will ignore any vibrate set using
     * {@link Builder#setVibrate}. Using phone vibration requires the
     * {@link android.Manifest.permission#VIBRATE VIBRATE} permission.
     *
     * <p>
     * A notification that vibrates is more likely to be presented as a heads-up notification,
     * on some platforms.
     * </p>
     *
     * @see Builder#setDefaults
     */
    public static final int DEFAULT_VIBRATE = 2;

    /**
     * Use the default notification lights. This will ignore the
     * {@link #FLAG_SHOW_LIGHTS} bit, and values set with {@link Builder#setLights}.
     *
     * @see Builder#setDefaults
     */
    public static final int DEFAULT_LIGHTS = 4;

    /**
     * Use this constant as the value for audioStreamType to request that
     * the default stream type for notifications be used.  Currently the
     * default stream type is {@link AudioManager#STREAM_NOTIFICATION}.
     */
    public static final int STREAM_DEFAULT = -1;
    /**
     * Bit set in the Notification flags field when LEDs should be turned on
     * for this notification.
     */
    public static final int FLAG_SHOW_LIGHTS        = 0x00000001;

    /**
     * Bit set in the Notification flags field if this notification is in
     * reference to something that is ongoing, like a phone call.  It should
     * not be set if this notification is in reference to something that
     * happened at a particular point in time, like a missed phone call.
     */
    public static final int FLAG_ONGOING_EVENT      = 0x00000002;

    /**
     * Bit set in the Notification flags field if
     * the audio will be repeated until the notification is
     * cancelled or the notification window is opened.
     */
    public static final int FLAG_INSISTENT          = 0x00000004;

    /**
     * Bit set in the Notification flags field if the notification's sound,
     * vibrate and ticker should only be played if the notification is not already showing.
     */
    public static final int FLAG_ONLY_ALERT_ONCE    = 0x00000008;

    /**
     * Bit set in the Notification flags field if the notification should be canceled when
     * it is clicked by the user.
     */
    public static final int FLAG_AUTO_CANCEL        = 0x00000010;

    /**
     * Bit set in the Notification flags field if the notification should not be canceled
     * when the user clicks the Clear all button.
     */
    public static final int FLAG_NO_CLEAR           = 0x00000020;

    /**
     * Bit set in the Notification flags field if this notification represents a currently
     * running service.  This will normally be set for you by
     * {@link android.app.Service#startForeground}.
     */
    public static final int FLAG_FOREGROUND_SERVICE = 0x00000040;

    /**
     * Obsolete flag indicating high-priority notifications; use the priority field instead.
     *
     * @deprecated Use {@link NotificationCompat.Builder#setPriority(int)} with a positive value.
     */
    @Deprecated
    public static final int FLAG_HIGH_PRIORITY      = 0x00000080;

    /**
     * Bit set in the Notification flags field if this notification is relevant to the current
     * device only and it is not recommended that it bridge to other devices.
     */
    public static final int FLAG_LOCAL_ONLY         = 0x00000100;

    /**
     * Bit set in the Notification flags field if this notification is the group summary for a
     * group of notifications. Grouped notifications may display in a cluster or stack on devices
     * which support such rendering. Requires a group key also be set using
     * {@link Builder#setGroup}.
     */
    public static final int FLAG_GROUP_SUMMARY      = 0x00000200;

    /**
     * Bit set in the Notification flags field if this notification is showing as a bubble.
     *
     * Applications cannot set this flag directly; they should instead call
     * {@link NotificationCompat.Builder#setBubbleMetadata(BubbleMetadata)} to request that a
     * notification be displayed as a bubble, and then check this flag to see whether that request
     * was honored by the system.
     */
    public static final int FLAG_BUBBLE             = 0x00001000;

    /**
     * Default notification priority for {@link NotificationCompat.Builder#setPriority(int)}.
     * If your application does not prioritize its own notifications,
     * use this value for all notifications.
     */
    public static final int PRIORITY_DEFAULT = 0;

    /**
     * Lower notification priority for {@link NotificationCompat.Builder#setPriority(int)},
     * for items that are less important. The UI may choose to show
     * these items smaller, or at a different position in the list,
     * compared with your app's {@link #PRIORITY_DEFAULT} items.
     */
    public static final int PRIORITY_LOW = -1;

    /**
     * Lowest notification priority for {@link NotificationCompat.Builder#setPriority(int)};
     * these items might not be shown to the user except under
     * special circumstances, such as detailed notification logs.
     */
    public static final int PRIORITY_MIN = -2;

    /**
     * Higher notification priority for {@link NotificationCompat.Builder#setPriority(int)},
     * for more important notifications or alerts. The UI may choose
     * to show these items larger, or at a different position in
     * notification lists, compared with your app's {@link #PRIORITY_DEFAULT} items.
     */
    public static final int PRIORITY_HIGH = 1;

    /**
     * Highest notification priority for {@link NotificationCompat.Builder#setPriority(int)},
     * for your application's most important items that require the user's
     * prompt attention or input.
     */
    public static final int PRIORITY_MAX = 2;

    /**
     * {@link #getExtras extras} key: this is the title of the notification,
     * as supplied to {@link Builder#setContentTitle(CharSequence)}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_TITLE = "android.title";

    /**
     * {@link #getExtras extras} key: this is the title of the notification when shown in expanded
     * form, e.g. as supplied to {@link BigTextStyle#setBigContentTitle(CharSequence)}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_TITLE_BIG = EXTRA_TITLE + ".big";

    /**
     * {@link #getExtras extras} key: this is the main text payload, as supplied to
     * {@link Builder#setContentText(CharSequence)}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_TEXT = "android.text";

    /**
     * {@link #getExtras extras} key: this is a third line of text, as supplied to
     * {@link Builder#setSubText(CharSequence)}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_SUB_TEXT = "android.subText";

    /**
     * {@link #getExtras extras} key: this is the remote input history, as supplied to
     * {@link Builder#setRemoteInputHistory(CharSequence[])}.
     *
     * Apps can fill this through {@link Builder#setRemoteInputHistory(CharSequence[])}
     * with the most recent inputs that have been sent through a {@link RemoteInput} of this
     * Notification and are expected to clear it once the it is no longer relevant (e.g. for chat
     * notifications once the other party has responded).
     *
     * The extra with this key is of type CharSequence[] and contains the most recent entry at
     * the 0 index, the second most recent at the 1 index, etc.
     *
     * @see Builder#setRemoteInputHistory(CharSequence[])
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_REMOTE_INPUT_HISTORY = "android.remoteInputHistory";

    /**
     * {@link #getExtras extras} key: this is a small piece of additional text as supplied to
     * {@link Builder#setContentInfo(CharSequence)}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_INFO_TEXT = "android.infoText";

    /**
     * {@link #getExtras extras} key: this is a line of summary information intended to be shown
     * alongside expanded notifications, as supplied to (e.g.)
     * {@link BigTextStyle#setSummaryText(CharSequence)}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_SUMMARY_TEXT = "android.summaryText";

    /**
     * {@link #getExtras extras} key: this is the longer text shown in the big form of a
     * {@link BigTextStyle} notification, as supplied to
     * {@link BigTextStyle#bigText(CharSequence)}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_BIG_TEXT = "android.bigText";

    /**
     * {@link #getExtras extras} key: very short text summarizing the most critical
     * information contained in the notification.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_SHORT_CRITICAL_TEXT = "android.shortCriticalText";

    /**
     * {@link #getExtras extras} key: If provided, should contain a boolean indicating
     * whether the notification is requesting promoted treatment.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing";

    /**
     * {@link #getExtras extras} key: this is the resource ID of the notification's main small icon,
     * as supplied to {@link Builder#setSmallIcon(int)}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_SMALL_ICON = "android.icon";

    /**
     * {@link #getExtras extras} key: this is a bitmap to be used instead of the small icon when
     * showing the notification payload, as
     * supplied to {@link Builder#setLargeIcon(android.graphics.Bitmap)}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_LARGE_ICON = "android.largeIcon";

    /**
     * {@link #getExtras extras} key: this is a bitmap to be used instead of the one from
     * {@link Builder#setLargeIcon(android.graphics.Bitmap)} when the notification is
     * shown in its expanded form, as supplied to
     * {@link BigPictureStyle#bigLargeIcon(android.graphics.Bitmap)}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_LARGE_ICON_BIG = EXTRA_LARGE_ICON + ".big";

    /**
     * {@link #getExtras extras} key: this is the progress value supplied to
     * {@link Builder#setProgress(int, int, boolean)}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_PROGRESS = "android.progress";

    /**
     * {@link #getExtras extras} key: this is the maximum value supplied to
     * {@link Builder#setProgress(int, int, boolean)}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_PROGRESS_MAX = "android.progressMax";

    /**
     * {@link #getExtras extras} key: whether the progress bar is indeterminate, supplied to
     * {@link Builder#setProgress(int, int, boolean)}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_PROGRESS_INDETERMINATE = "android.progressIndeterminate";

    /**
     * {@link #getExtras extras} key: whether the when field set using {@link Builder#setWhen}
     * should be shown as a count-up timer (specifically a {@link android.widget.Chronometer})
     * instead of a timestamp, as supplied to {@link Builder#setUsesChronometer(boolean)}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_SHOW_CHRONOMETER = "android.showChronometer";

    /**
     * {@link #getExtras extras} key: whether the chronometer set on the notification should count
     * down instead of counting up. Is only relevant if key {@link #EXTRA_SHOW_CHRONOMETER} is
     * present. This extra is a boolean. The default is (@code false).
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_CHRONOMETER_COUNT_DOWN = "android.chronometerCountDown";

    /**
     * {@link #getExtras extras} key: whether the notification should be colorized as
     * supplied to {@link Builder#setColorized(boolean)}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_COLORIZED = "android.colorized";

    /**
     * {@link #getExtras extras} key: whether the when field set using {@link Builder#setWhen}
     * should be shown, as supplied to {@link Builder#setShowWhen(boolean)}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_SHOW_WHEN = "android.showWhen";

    /**
     * {@link #getExtras extras} key: this is a bitmap to be shown in {@link BigPictureStyle}
     * expanded notifications, supplied to
     * {@link BigPictureStyle#bigPicture(android.graphics.Bitmap)}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_PICTURE = "android.picture";

    /**
     * {@link #getExtras extras} key: this is an {@link Icon} of an image to be
     * shown in {@link Notification.BigPictureStyle} expanded notifications, supplied to
     * {@link BigPictureStyle#bigPicture(Icon)}.
     */
    @SuppressLint("ActionValue") // Field & value copied from android.app.Notification
    public static final String EXTRA_PICTURE_ICON = "android.pictureIcon";

    /**
     * {@link #getExtras extras} key: this is a content description of the big picture supplied from
     * {@link BigPictureStyle#bigPicture(Bitmap)}, supplied to
     * {@link BigPictureStyle#setContentDescription(CharSequence)}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_PICTURE_CONTENT_DESCRIPTION =
            "android.pictureContentDescription";

    /**
     * {@link #getExtras extras} key: this is a boolean to indicate that the
     * {@link BigPictureStyle#bigPicture(Bitmap) big picture} is to be shown in the collapsed state
     * of a {@link BigPictureStyle} notification.  This will replace a
     * {@link Builder#setLargeIcon(Bitmap) large icon} in that state if one was provided.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_SHOW_BIG_PICTURE_WHEN_COLLAPSED =
            "android.showBigPictureWhenCollapsed";

    /**
     * {@link #getExtras extras} key: An array of CharSequences to show in {@link InboxStyle}
     * expanded notifications, each of which was supplied to
     * {@link InboxStyle#addLine(CharSequence)}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_TEXT_LINES = "android.textLines";

    /**
     * {@link #getExtras extras} key: A string representing the name of the specific
     * {@link android.app.Notification.Style} used to create this notification.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_TEMPLATE = "android.template";

    /**
     * {@link #getExtras extras} key: A string representing the name of the specific
     * {@link NotificationCompat.Style} used to create this notification.
     */
    public static final String EXTRA_COMPAT_TEMPLATE = "androidx.core.app.extra.COMPAT_TEMPLATE";

    /**
     * {@link #getExtras extras} key: A String array containing the people that this
     * notification relates to, each of which was supplied to
     * {@link Builder#addPerson(String)}.
     *
     * @deprecated the actual objects are now in {@link #EXTRA_PEOPLE_LIST}
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    @Deprecated
    public static final String EXTRA_PEOPLE = "android.people";

    /**
     * {@link #getExtras extras} key: : An arrayList of {@link Person} objects containing the
     * people that this notification relates to, each of which was supplied to
     * {@link Builder#addPerson(Person)}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_PEOPLE_LIST = "android.people.list";

    /**
     * {@link #getExtras extras} key: A
     * {@link android.content.ContentUris content URI} pointing to an image that can be displayed
     * in the background when the notification is selected. The URI must point to an image stream
     * suitable for passing into
     * {@link android.graphics.BitmapFactory#decodeStream(java.io.InputStream)
     * BitmapFactory.decodeStream}; all other content types will be ignored. The content provider
     * URI used for this purpose must require no permissions to read the image data.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_BACKGROUND_IMAGE_URI = "android.backgroundImageUri";

    /**
     * Notification key: A
     * {@link android.media.session.MediaSession.Token} associated with a
     * {@link android.app.Notification.MediaStyle} notification.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_MEDIA_SESSION = "android.mediaSession";

    /**
     * {@link #getExtras extras} key: the indices of actions to be shown in the compact view,
     * as supplied to (e.g.) {@link Notification.MediaStyle#setShowActionsInCompactView(int...)}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_COMPACT_ACTIONS = "android.compactActions";

    /**
     * {@link #getExtras extras} key: the username to be displayed for all messages sent by the
     * user including direct replies {@link MessagingStyle} notification.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_SELF_DISPLAY_NAME = "android.selfDisplayName";

    /**
     * {@link #getExtras extras} key: the person to display for all messages sent by the user,
     * including direct replies to {@link MessagingStyle} notifications.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_MESSAGING_STYLE_USER = "android.messagingStyleUser";

    /**
     * {@link #getExtras extras} key: a {@link String} to be displayed as the title to a
     * conversation represented by a {@link MessagingStyle}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_CONVERSATION_TITLE = "android.conversationTitle";

    /**
     * {@link #getExtras extras} key: an array of {@link MessagingStyle.Message}
     * bundles provided by a {@link android.app.Notification.MessagingStyle} notification.
     * This extra is a parcelable array of {@link Bundle} objects.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_MESSAGES = "android.messages";

    /**
     * {@link #getExtras extras} key: an array of {@link MessagingStyle#addHistoricMessage historic}
     * {@link MessagingStyle.Message} bundles provided by a {@link MessagingStyle} notification.
     * This extra is a parcelable array of {@link Bundle} objects.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_HISTORIC_MESSAGES = "android.messages.historic";

    /**
     * {@link #getExtras extras} key: whether the {@link NotificationCompat.MessagingStyle}
     * notification represents a group conversation.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_IS_GROUP_CONVERSATION = "android.isGroupConversation";

    /**
     * {@link #getExtras extras} key: the type of call represented by the
     * {@link android.app.Notification.CallStyle} notification. This extra is an int.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_CALL_TYPE = "android.callType";

    /**
     * {@link #getExtras extras} key: whether the  {@link android.app.Notification.CallStyle} notification
     * is for a call that will activate video when answered. This extra is a boolean.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_CALL_IS_VIDEO = "android.callIsVideo";

    /**
     * {@link #getExtras extras} key: the person to be displayed as calling for the
     * {@link android.app.Notification.CallStyle} notification. This extra is a {@link Person}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_CALL_PERSON = "android.callPerson";

    /**
     * {@link #getExtras extras} key: the person to be displayed as calling for the
     * {@link android.app.Notification.CallStyle} notification, for Android versions before the
     * {@link Person} class was introduced. This extra is a {@link Bundle} representing a
     * {@link Person}.
     */
    @SuppressLint("ActionValue")
    public static final String EXTRA_CALL_PERSON_COMPAT = "android.callPersonCompat";

    /**
     * {@link #getExtras extras} key: the icon to be displayed as a verification status of the
     * caller on a {@link android.app.Notification.CallStyle} notification. This extra is an
     * {@link Icon}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_VERIFICATION_ICON = "android.verificationIcon";

    /**
     * {@link #getExtras extras} key: the icon to be displayed as a verification status of the
     * caller on a {@link android.app.Notification.CallStyle} notification, for Android versions
     * before the {@link Icon} class was introduced. This extra is an {@link Bundle} representing an
     * {@link Icon}.
     */
    @SuppressLint("ActionValue")
    public static final String EXTRA_VERIFICATION_ICON_COMPAT = "android.verificationIconCompat";

    /**
     * {@link #getExtras extras} key: the text to be displayed as a verification status of the
     * caller on a {@link android.app.Notification.CallStyle} notification. This extra is a
     * {@link CharSequence}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_VERIFICATION_TEXT = "android.verificationText";

    /**
     * {@link #getExtras extras} key: the intent to be sent when the users answers a
     * {@link android.app.Notification.CallStyle} notification. This extra is a
     * {@link PendingIntent}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_ANSWER_INTENT = "android.answerIntent";

    /**
     * {@link #getExtras extras} key: the intent to be sent when the users declines a
     * {@link android.app.Notification.CallStyle} notification. This extra is a
     * {@link PendingIntent}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_DECLINE_INTENT = "android.declineIntent";

    /**
     * {@link #getExtras extras} key: the intent to be sent when the users hangs up a
     * {@link android.app.Notification.CallStyle} notification. This extra is a
     * {@link PendingIntent}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_HANG_UP_INTENT = "android.hangUpIntent";

    /**
     * {@link #getExtras extras} key: the color used as a hint for the Answer action button of a
     * {@link android.app.Notification.CallStyle} notification. This extra is a {@link ColorInt}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_ANSWER_COLOR = "android.answerColor";

    /**
     * {@link #getExtras extras} key: the color used as a hint for the Decline or Hang Up action button of a
     * {@link android.app.Notification.CallStyle} notification. This extra is a {@link ColorInt}.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_DECLINE_COLOR = "android.declineColor";

    /**
     * Key for compat's {@link MessagingStyle#getConversationTitle()}. This allows backwards support
     * for conversation titles as SDK < P uses the title to denote group status. This hidden title
     * doesn't appear in the notification shade.
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_HIDDEN_CONVERSATION_TITLE = "android.hiddenConversationTitle";

    /**
     * Keys into the {@link #getExtras} Bundle: the audio contents of this notification.
     *
     * This is for use when rendering the notification on an audio-focused interface;
     * the audio contents are a complete sound sample that contains the contents/body of the
     * notification. This may be used in substitute of a Text-to-Speech reading of the
     * notification. For example if the notification represents a voice message this should point
     * to the audio of that message.
     *
     * The data stored under this key should be a String representation of a Uri that contains the
     * audio contents in one of the following formats: WAV, PCM 16-bit, AMR-WB.
     *
     * This extra is unnecessary if you are using {@code MessagingStyle} since each {@code Message}
     * has a field for holding data URI. That field can be used for audio.
     * See {@code Message#setData}.
     *
     * Example usage:
     * <pre>
     * {@code
     * NotificationCompat.Builder myBuilder = (build your Notification as normal);
     * myBuilder.getExtras().putString(EXTRA_AUDIO_CONTENTS_URI, myAudioUri.toString());
     * }
     * </pre>
     */
    @SuppressLint("ActionValue")  // Field & value copied from android.app.Notification
    public static final String EXTRA_AUDIO_CONTENTS_URI = "android.audioContents";

    /**
     * {@link #extras} key: an arraylist of {@link android.app.Notification.ProgressStyle.Segment}
     * bundles provided by a
     * {@link android.app.Notification.ProgressStyle} notification as supplied to
     * {@link ProgressStyle#setProgressSegments}
     * or {@link ProgressStyle#addProgressSegment(ProgressStyle.Segment)}.
     * This extra is a parcelable array list of bundles.
     */
    @SuppressLint("ActionValue")
    public static final String EXTRA_PROGRESS_SEGMENTS = "android.progressSegments";

    /**
     * {@link #extras} key: an arraylist of {@link ProgressStyle.Point}
     * bundles provided by a
     * {@link android.app.Notification.ProgressStyle} notification as supplied to
     * {@link ProgressStyle#setProgressPoints}
     * or {@link ProgressStyle#addProgressPoint(ProgressStyle.Point)}.
     * This extra is a parcelable array list of bundles.
     */
    @SuppressLint("ActionValue")
    public static final String EXTRA_PROGRESS_POINTS = "android.progressPoints";

    /**
     * {@link #extras} key: whether the progress bar should be styled by its progress as
     * supplied to {@link ProgressStyle#setStyledByProgress}.
     * This extra is a boolean.
     */
    @SuppressLint("ActionValue")
    public static final String EXTRA_STYLED_BY_PROGRESS = "android.styledByProgress";

    /**
     * {@link #extras} key: this is an {@link IconCompat} of an image to be
     * shown as progress bar progress tracker icon in {@link ProgressStyle}, supplied to
     *{@link ProgressStyle#setProgressTrackerIcon(IconCompat)}.
     */
    @SuppressLint("ActionValue")
    public static final String EXTRA_PROGRESS_TRACKER_ICON = "android.progressTrackerIcon";

    /**
     * {@link #extras} key: this is an {@link IconCompat} of an image to be
     * shown at the beginning of the progress bar in {@link ProgressStyle}, supplied to
     *{@link ProgressStyle#setProgressStartIcon(IconCompat)}.
     */
    @SuppressLint("ActionValue")
    public static final String EXTRA_PROGRESS_START_ICON = "android.progressStartIcon";

    /**
     * {@link #extras} key: this is an {@link IconCompat} of an image to be
     * shown at the end of the progress bar in {@link ProgressStyle}, supplied to
     *{@link ProgressStyle#setProgressEndIcon(IconCompat)}.
     */
    @SuppressLint("ActionValue")
    public static final String EXTRA_PROGRESS_END_ICON = "android.progressEndIcon";

    /**
     * Value of {@link Notification#color} equal to 0 (also known as
     * {@link android.graphics.Color#TRANSPARENT Color.TRANSPARENT}),
     * telling the system not to decorate this notification with any special color but instead use
     * default colors when presenting this notification.
     */
    @ColorInt
    public static final int COLOR_DEFAULT = Color.TRANSPARENT;

    /**
     * Maximum number of (generic) action buttons in a notification (contextual action buttons are
     * handled separately).
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static final int MAX_ACTION_BUTTONS = 3;

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef({AudioManager.STREAM_VOICE_CALL, AudioManager.STREAM_SYSTEM, AudioManager.STREAM_RING,
            AudioManager.STREAM_MUSIC, AudioManager.STREAM_ALARM, AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_DTMF, AudioManager.STREAM_ACCESSIBILITY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface StreamType {}

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Retention(SOURCE)
    @IntDef({VISIBILITY_PUBLIC, VISIBILITY_PRIVATE, VISIBILITY_SECRET})
    public @interface NotificationVisibility {}
    /**
     * Notification visibility: Show this notification in its entirety on all lockscreens.
     *
     * {@see android.app.Notification#visibility}
     */
    public static final int VISIBILITY_PUBLIC = Notification.VISIBILITY_PUBLIC;

    /**
     * Notification visibility: Show this notification on all lockscreens, but conceal sensitive or
     * private information on secure lockscreens.
     *
     * {@see android.app.Notification#visibility}
     */
    public static final int VISIBILITY_PRIVATE = Notification.VISIBILITY_PRIVATE;

    /**
     * Notification visibility: Do not reveal any part of this notification on a secure lockscreen.
     *
     * {@see android.app.Notification#visibility}
     */
    public static final int VISIBILITY_SECRET = Notification.VISIBILITY_SECRET;

    /**
     * Notification category: incoming call (voice or video) or similar synchronous communication request.
     */
    public static final String CATEGORY_CALL = Notification.CATEGORY_CALL;

    /**
     * Notification category: map turn-by-turn navigation.
     */
    public static final String CATEGORY_NAVIGATION = Notification.CATEGORY_NAVIGATION;

    /**
     * Notification category: incoming direct message (SMS, instant message, etc.).
     */
    public static final String CATEGORY_MESSAGE = Notification.CATEGORY_MESSAGE;

    /**
     * Notification category: asynchronous bulk message (email).
     */
    public static final String CATEGORY_EMAIL = Notification.CATEGORY_EMAIL;

    /**
     * Notification category: calendar event.
     */
    public static final String CATEGORY_EVENT = Notification.CATEGORY_EVENT;

    /**
     * Notification category: promotion or advertisement.
     */
    public static final String CATEGORY_PROMO = Notification.CATEGORY_PROMO;

    /**
     * Notification category: alarm or timer.
     */
    public static final String CATEGORY_ALARM = Notification.CATEGORY_ALARM;

    /**
     * Notification category: progress of a long-running background operation.
     */
    public static final String CATEGORY_PROGRESS = Notification.CATEGORY_PROGRESS;

    /**
     * Notification category: social network or sharing update.
     */
    public static final String CATEGORY_SOCIAL = Notification.CATEGORY_SOCIAL;

    /**
     * Notification category: error in background operation or authentication status.
     */
    public static final String CATEGORY_ERROR = Notification.CATEGORY_ERROR;

    /**
     * Notification category: media transport control for playback.
     */
    public static final String CATEGORY_TRANSPORT = Notification.CATEGORY_TRANSPORT;

    /**
     * Notification category: system or device status update.  Reserved for system use.
     */
    public static final String CATEGORY_SYSTEM = Notification.CATEGORY_SYSTEM;

    /**
     * Notification category: indication of running background service.
     */
    public static final String CATEGORY_SERVICE = Notification.CATEGORY_SERVICE;

    /**
     * Notification category: user-scheduled reminder.
     */
    public static final String CATEGORY_REMINDER = Notification.CATEGORY_REMINDER;

    /**
     * Notification category: a specific, timely recommendation for a single thing.
     * For example, a news app might want to recommend a news story it believes the user will
     * want to read next.
     */
    public static final String CATEGORY_RECOMMENDATION =
            Notification.CATEGORY_RECOMMENDATION;

    /**
     * Notification category: ongoing information about device or contextual status.
     */
    public static final String CATEGORY_STATUS = Notification.CATEGORY_STATUS;

    /**
     * Notification category: tracking a user's workout.
     */
    public static final String CATEGORY_WORKOUT = "workout";

    /**
     * Notification category: temporarily sharing location.
     */
    public static final String CATEGORY_LOCATION_SHARING = "location_sharing";

    /**
     * Notification category: running stopwatch.
     */
    public static final String CATEGORY_STOPWATCH = "stopwatch";

    /**
     * Notification category: missed call.
     */
    public static final String CATEGORY_MISSED_CALL = "missed_call";

    /**
     * Notification category: voicemail.
     */
    public static final String CATEGORY_VOICEMAIL = "voicemail";

    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef({BADGE_ICON_NONE, BADGE_ICON_SMALL, BADGE_ICON_LARGE})
    public @interface BadgeIconType {}
    /**
     * If this notification is being shown as a badge, always show as a number.
     */
    public static final int BADGE_ICON_NONE = Notification.BADGE_ICON_NONE;

    /**
     * If this notification is being shown as a badge, use the icon provided to
     * {@link Builder#setSmallIcon(int)} to represent this notification.
     */
    public static final int BADGE_ICON_SMALL = Notification.BADGE_ICON_SMALL;

    /**
     * If this notification is being shown as a badge, use the icon provided to
     * {@link Builder#setLargeIcon(Bitmap)} to represent this notification.
     */
    public static final int BADGE_ICON_LARGE = Notification.BADGE_ICON_LARGE;

    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef({GROUP_ALERT_ALL, GROUP_ALERT_SUMMARY, GROUP_ALERT_CHILDREN})
    public @interface GroupAlertBehavior {}

    /**
     * Constant for {@link Builder#setGroupAlertBehavior(int)}, meaning that all notifications in a
     * group with sound or vibration ought to make sound or vibrate (respectively), so this
     * notification will not be muted when it is in a group.
     */
    public static final int GROUP_ALERT_ALL = Notification.GROUP_ALERT_ALL;

    /**
     * Constant for {@link Builder#setGroupAlertBehavior(int)}, meaning that all children
     * notification in a group should be silenced (no sound or vibration) even if they would
     * otherwise make sound or vibrate. Use this constant to mute this notification if this
     * notification is a group child. This must be applied to all children notifications you want
     * to mute.
     *
     * <p> For example, you might want to use this constant if you post a number of children
     * notifications at once (say, after a periodic sync), and only need to notify the user
     * audibly once.
     */
    public static final int GROUP_ALERT_SUMMARY = Notification.GROUP_ALERT_SUMMARY;

    /**
     * Constant for {@link Builder#setGroupAlertBehavior(int)}, meaning that the summary
     * notification in a group should be silenced (no sound or vibration) even if they would
     * otherwise make sound or vibrate. Use this constant
     * to mute this notification if this notification is a group summary.
     *
     * <p>For example, you might want to use this constant if only the children notifications
     * in your group have content and the summary is only used to visually group notifications
     * rather than to alert the user that new information is available.
     */
    public static final int GROUP_ALERT_CHILDREN = Notification.GROUP_ALERT_CHILDREN;

    /**
     * Constant for the {@link Builder#setGroup(String) group key} that's added to notifications
     * that are not already grouped when {@link Builder#setNotificationSilent()} is used when
     * {@link Build.VERSION#SDK_INT} is >= {@link Build.VERSION_CODES#O}.
     */
    public static final String GROUP_KEY_SILENT = "silent";

    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef({FOREGROUND_SERVICE_DEFAULT,
            FOREGROUND_SERVICE_IMMEDIATE,
            FOREGROUND_SERVICE_DEFERRED})
    public @interface ServiceNotificationBehavior {}

    /**
     * Constant for {@link Builder#setForegroundServiceBehavior(int)}. In Android 12 or later,
     * if the Notification associated with starting a foreground service has been
     * built using setForegroundServiceBehavior() with this behavior, display of
     * the notification will often be suppressed for a short time to avoid visual
     * disturbances to the user.
     *
     * @see NotificationCompat.Builder#setForegroundServiceBehavior(int)
     * @see #FOREGROUND_SERVICE_IMMEDIATE
     * @see #FOREGROUND_SERVICE_DEFERRED
     */
    public static final int FOREGROUND_SERVICE_DEFAULT =
            Notification.FOREGROUND_SERVICE_DEFAULT;

    /**
     * Constant for {@link Builder#setForegroundServiceBehavior(int)}. In Android 12 or later,
     * if the Notification associated with starting a foreground service has been
     * built using setForegroundServiceBehavior() with this behavior, display of
     * the notification will be immediate even if the default behavior would be
     * to defer visibility for a short time.
     *
     * @see NotificationCompat.Builder#setForegroundServiceBehavior(int)
     * @see #FOREGROUND_SERVICE_DEFAULT
     * @see #FOREGROUND_SERVICE_DEFERRED
     */
    public static final int FOREGROUND_SERVICE_IMMEDIATE =
            Notification.FOREGROUND_SERVICE_IMMEDIATE;

    /**
     * Constant for {@link Builder#setForegroundServiceBehavior(int)}. In Android 12 or later,
     * if the Notification associated with starting a foreground service has been
     * built using setForegroundServiceBehavior() with this behavior, display of
     * the notification will usually be suppressed for a short time to avoid visual
     * disturbances to the user.
     *
     * @see NotificationCompat.Builder#setForegroundServiceBehavior(int)
     * @see #FOREGROUND_SERVICE_DEFAULT
     * @see #FOREGROUND_SERVICE_IMMEDIATE
     */
    public static final int FOREGROUND_SERVICE_DEFERRED =
            Notification.FOREGROUND_SERVICE_DEFERRED;

    /**
     * Builder class for {@link NotificationCompat} objects.  Allows easier control over
     * all the flags, as well as help constructing the typical notification layouts.
     * <p>
     * On platform versions that don't offer expanded notifications, methods that depend on
     * expanded notifications have no effect.
     * </p>
     * <p>
     * For example, action buttons won't appear on platforms prior to Android 4.1. Action
     * buttons depend on expanded notifications, which are only available in Android 4.1
     * and later.
     * <p>
     * For this reason, you should always ensure that UI controls in a notification are also
     * available in an {@link android.app.Activity} in your app, and you should always start that
     * {@link android.app.Activity} when users click the notification. To do this, use the
     * {@link NotificationCompat.Builder#setContentIntent setContentIntent()}
     * method.
     * </p>
     *
     */
    public static class Builder {
        /**
         * Maximum length of CharSequences accepted by Builder and friends.
         *
         * <p>
         * Avoids spamming the system with overly large strings such as full e-mails.
         */
        private static final int MAX_CHARSEQUENCE_LENGTH = 5 * 1024;

        // All these variables are declared public/hidden so they can be accessed by a builder
        // extender.

        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public Context mContext;

        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public ArrayList<Action> mActions = new ArrayList<>();

        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public @NonNull ArrayList<Person> mPersonList = new ArrayList<>();

        // Invisible actions are stored in the CarExtender bundle without actually being owned by
        // CarExtender. This is to comply with an optimization of the Android OS which removes
        // Actions from the Notification if there are no listeners for those Actions.
        ArrayList<Action> mInvisibleActions = new ArrayList<>();

        CharSequence mContentTitle;
        CharSequence mContentText;
        @Nullable String mShortCriticalText;
        PendingIntent mContentIntent;
        PendingIntent mFullScreenIntent;
        RemoteViews mTickerView;
        IconCompat mLargeIcon;
        CharSequence mContentInfo;
        int mNumber;
        int mPriority;
        boolean mShowWhen = true;
        boolean mUseChronometer;
        boolean mChronometerCountDown;
        Style mStyle;
        CharSequence mSubText;
        CharSequence mSettingsText;
        CharSequence[] mRemoteInputHistory;
        int mProgressMax;
        int mProgress;
        boolean mProgressIndeterminate;
        String mGroupKey;
        boolean mGroupSummary;
        String mSortKey;
        boolean mLocalOnly = false;
        boolean mColorized;
        boolean mColorizedSet;
        String mCategory;
        Bundle mExtras;
        int mColor = COLOR_DEFAULT;
        @NotificationVisibility int mVisibility = VISIBILITY_PRIVATE;
        Notification mPublicVersion;
        RemoteViews mContentView;
        RemoteViews mBigContentView;
        RemoteViews mHeadsUpContentView;
        String mChannelId;
        int mBadgeIcon = BADGE_ICON_NONE;
        String mShortcutId;
        LocusIdCompat mLocusId;
        long mTimeout;
        @GroupAlertBehavior int mGroupAlertBehavior = GROUP_ALERT_ALL;
        @ServiceNotificationBehavior int mFgsDeferBehavior = FOREGROUND_SERVICE_DEFAULT;
        boolean mAllowSystemGeneratedContextualActions;
        BubbleMetadata mBubbleMetadata;
        Notification mNotification = new Notification();
        boolean mSilent;
        Object mSmallIcon; // Icon

        /**
         * @deprecated This field was not meant to be public.
         */
        @Deprecated
        public ArrayList<String> mPeople;

        /**
         * Creates a NotificationCompat.Builder which can be used to build a notification that is
         * equivalent to the given one, such that updates can be made to an existing notification
         * with the NotificationCompat.Builder API.
         */
        @SuppressWarnings("deprecation")
        public Builder(@NonNull Context context,
                @NonNull Notification notification) {
            this(context, getChannelId(notification));
            final Bundle extras = notification.extras;
            final Style style = Style.extractStyleFromNotification(notification);
            this.setContentTitle(NotificationCompat.getContentTitle(notification))
                    .setContentText(NotificationCompat.getContentText(notification))
                    .setContentInfo(NotificationCompat.getContentInfo(notification))
                    .setSubText(NotificationCompat.getSubText(notification))
                    .setSettingsText(NotificationCompat.getSettingsText(notification))
                    .setStyle(style)
                    .setGroup(NotificationCompat.getGroup(notification))
                    .setGroupSummary(NotificationCompat.isGroupSummary(notification))
                    .setLocusId(NotificationCompat.getLocusId(notification))
                    .setWhen(notification.when)
                    .setShowWhen(NotificationCompat.getShowWhen(notification))
                    .setUsesChronometer(NotificationCompat.getUsesChronometer(notification))
                    .setAutoCancel(NotificationCompat.getAutoCancel(notification))
                    .setOnlyAlertOnce(NotificationCompat.getOnlyAlertOnce(notification))
                    .setOngoing(NotificationCompat.getOngoing(notification))
                    .setLocalOnly(NotificationCompat.getLocalOnly(notification))
                    .setLargeIcon(notification.largeIcon)
                    .setBadgeIconType(NotificationCompat.getBadgeIconType(notification))
                    .setCategory(NotificationCompat.getCategory(notification))
                    .setBubbleMetadata(NotificationCompat.getBubbleMetadata(notification))
                    .setNumber(notification.number)
                    .setTicker(notification.tickerText)
                    .setContentIntent(notification.contentIntent)
                    .setDeleteIntent(notification.deleteIntent)
                    .setFullScreenIntent(notification.fullScreenIntent,
                            NotificationCompat.getHighPriority(notification))
                    .setSound(notification.sound, notification.audioStreamType)
                    .setSilent(NotificationCompat.isSilent(notification))
                    .setVibrate(notification.vibrate)
                    .setLights(notification.ledARGB, notification.ledOnMS, notification.ledOffMS)
                    .setDefaults(notification.defaults)
                    .setPriority(notification.priority)
                    .setColor(NotificationCompat.getColor(notification))
                    .setVisibility(NotificationCompat.getVisibility(notification))
                    .setPublicVersion(NotificationCompat.getPublicVersion(notification))
                    .setSortKey(NotificationCompat.getSortKey(notification))
                    .setTimeoutAfter(getTimeoutAfter(notification))
                    .setShortcutId(getShortcutId(notification))
                    .setProgress(extras.getInt(EXTRA_PROGRESS_MAX), extras.getInt(EXTRA_PROGRESS),
                            extras.getBoolean(EXTRA_PROGRESS_INDETERMINATE))
                    .setAllowSystemGeneratedContextualActions(NotificationCompat
                            .getAllowSystemGeneratedContextualActions(notification))
                    .setSmallIcon(notification.icon, notification.iconLevel)
                    .addExtras(getExtrasWithoutDuplicateData(notification, style));

            // TODO: Copy custom RemoteViews from the Notification.

            // Avoid the setter which requires wrapping/unwrapping IconCompat and extra null checks
            this.mSmallIcon = notification.getSmallIcon();
            Icon largeIcon = notification.getLargeIcon();
            if (largeIcon != null) {
                this.mLargeIcon = IconCompat.createFromIcon(largeIcon);
            }

            // Add actions from the notification.
            if (notification.actions != null && notification.actions.length != 0) {
                for (Notification.Action action : notification.actions) {
                    this.addAction(Action.Builder.fromAndroidAction(action).build());
                }
            }
            // Add invisible actions from the notification.
            List<Action> invisibleActions =
                    NotificationCompat.getInvisibleActions(notification);
            if (!invisibleActions.isEmpty()) {
                for (Action invisibleAction : invisibleActions) {
                    this.addInvisibleAction(invisibleAction);
                }
            }

            // Add legacy people.  On 28+ this would be empty unless the app used addPerson(String).
            String[] people = notification.extras.getStringArray(EXTRA_PEOPLE);
            if (people != null && people.length != 0) {
                for (String person : people) {
                    this.addPerson(person);
                }
            }
            // Add modern Person list
            if (Build.VERSION.SDK_INT >= 28) {
                ArrayList<android.app.Person> peopleList =
                        notification.extras.getParcelableArrayList(EXTRA_PEOPLE_LIST);
                if (peopleList != null && !peopleList.isEmpty()) {
                    for (android.app.Person person : peopleList) {
                        this.addPerson(Person.fromAndroidPerson(person));
                    }
                }
            }

            // These setters have side effects even when the default value is set, so they should
            // only be called if there is a value in the notification extras to be set
            if (Build.VERSION.SDK_INT >= 24) {
                if (extras.containsKey(EXTRA_CHRONOMETER_COUNT_DOWN)) {
                    this.setChronometerCountDown(
                            extras.getBoolean(EXTRA_CHRONOMETER_COUNT_DOWN));
                }
            }
            if (Build.VERSION.SDK_INT >= 26) {
                if (extras.containsKey(EXTRA_COLORIZED)) {
                    this.setColorized(extras.getBoolean(EXTRA_COLORIZED));
                }
            }
            if (Build.VERSION.SDK_INT >= 36) {
                if (extras.containsKey(EXTRA_SHORT_CRITICAL_TEXT)) {
                    this.setShortCriticalText(extras.getString(EXTRA_SHORT_CRITICAL_TEXT));
                }
            }
        }

        /** Remove all extras which have been parsed by the rest of the copy process */
        private static @Nullable Bundle getExtrasWithoutDuplicateData(
                @NonNull Notification notification, @Nullable Style style) {
            if (notification.extras == null) {
                return null;
            }
            Bundle newExtras = new Bundle(notification.extras);

            // Remove keys which are elsewhere copied from the notification to the builder
            newExtras.remove(EXTRA_TITLE);
            newExtras.remove(EXTRA_TEXT);
            newExtras.remove(EXTRA_INFO_TEXT);
            newExtras.remove(EXTRA_SUB_TEXT);
            if (Build.VERSION.SDK_INT >= 36) {
                newExtras.remove(EXTRA_SHORT_CRITICAL_TEXT);
            }
            newExtras.remove(EXTRA_CHANNEL_ID);
            newExtras.remove(EXTRA_CHANNEL_GROUP_ID);
            newExtras.remove(EXTRA_SHOW_WHEN);
            newExtras.remove(EXTRA_PROGRESS);
            newExtras.remove(EXTRA_PROGRESS_MAX);
            newExtras.remove(EXTRA_PROGRESS_INDETERMINATE);
            newExtras.remove(EXTRA_CHRONOMETER_COUNT_DOWN);
            newExtras.remove(EXTRA_COLORIZED);
            newExtras.remove(EXTRA_PEOPLE_LIST);
            newExtras.remove(EXTRA_PEOPLE);
            newExtras.remove(NotificationCompatExtras.EXTRA_SORT_KEY);
            newExtras.remove(NotificationCompatExtras.EXTRA_GROUP_KEY);
            newExtras.remove(NotificationCompatExtras.EXTRA_GROUP_SUMMARY);
            newExtras.remove(NotificationCompatExtras.EXTRA_LOCAL_ONLY);
            newExtras.remove(NotificationCompatExtras.EXTRA_ACTION_EXTRAS);

            // Remove the nested EXTRA_INVISIBLE_ACTIONS from the EXTRA_CAR_EXTENDER
            Bundle carExtenderExtras = newExtras.getBundle(CarExtender.EXTRA_CAR_EXTENDER);
            if (carExtenderExtras != null) {
                carExtenderExtras = new Bundle(carExtenderExtras);
                carExtenderExtras.remove(CarExtender.EXTRA_INVISIBLE_ACTIONS);
                newExtras.putBundle(CarExtender.EXTRA_CAR_EXTENDER, carExtenderExtras);
            }

            // Remove keys used by the style which was successfully extracted from the notification
            if (style != null) {
                style.clearCompatExtraKeys(newExtras);
            }
            return newExtras;
        }

        /**
         * Constructor.
         *
         * Automatically sets the when field to {@link System#currentTimeMillis()
         * System.currentTimeMillis()} and the audio stream to the
         * {@link Notification#STREAM_DEFAULT}.
         *
         * @param context A {@link Context} that will be used to construct the
         *      RemoteViews. The Context will not be held past the lifetime of this
         *      Builder object.
         * @param channelId The constructed Notification will be posted on this
         *      NotificationChannel.
         */
        @SuppressWarnings("deprecation")
        public Builder(@NonNull Context context, @NonNull String channelId) {
            mContext = context;
            mChannelId = channelId;
            // Set defaults to match the defaults of a Notification
            mNotification.when = System.currentTimeMillis();
            mNotification.audioStreamType = Notification.STREAM_DEFAULT;
            mPriority = PRIORITY_DEFAULT;
            mPeople = new ArrayList<>();
            mAllowSystemGeneratedContextualActions = true;
        }

        /**
         * @deprecated use {@code Builder(Context, String)} instead. All posted notifications must
         * specify a NotificationChannel ID.
         */
        @Deprecated
        public Builder(@NonNull Context context) {
            this(context, (String) null);
        }

        /**
         * Set the time that the event occurred.  Notifications in the panel are
         * sorted by this time.
         */
        public @NonNull Builder setWhen(long when) {
            mNotification.when = when;
            return this;
        }

        /**
         * Control whether the timestamp set with {@link #setWhen(long) setWhen} is shown
         * in the content view. The default is {@code true}.
         */
        public @NonNull Builder setShowWhen(boolean show) {
            mShowWhen = show;
            return this;
        }

        /**
         * Set the small icon to use in the notification layouts.  Different classes of devices
         * may return different sizes.  See the UX guidelines for more information on how to
         * design these icons.
         *
         * @param icon The small Icon object to use
         */
        public @NonNull Builder setSmallIcon(@NonNull IconCompat icon) {
            this.mSmallIcon = icon.toIcon(mContext);
            return this;
        }

        /**
         * Show the {@link Notification#when} field as a stopwatch.
         *
         * Instead of presenting <code>when</code> as a timestamp, the notification will show an
         * automatically updating display of the minutes and seconds since <code>when</code>.
         *
         * Useful when showing an elapsed time (like an ongoing phone call).
         *
         * @see android.widget.Chronometer
         * @see Notification#when
         */
        public @NonNull Builder setUsesChronometer(boolean b) {
            mUseChronometer = b;
            return this;
        }

        /**
         * Sets the Chronometer to count down instead of counting up.
         *
         * This is only relevant if setUsesChronometer(boolean) has been set to true. If it
         * isn't set the chronometer will count up.
         *
         * @see android.widget.Chronometer
         */
        @RequiresApi(24)
        public @NonNull Builder setChronometerCountDown(boolean countsDown) {
            mChronometerCountDown = countsDown;
            getExtras().putBoolean(EXTRA_CHRONOMETER_COUNT_DOWN, countsDown);
            return this;
        }

        /**
         * Set the small icon to use in the notification layouts.  Different classes of devices
         * may return different sizes.  See the UX guidelines for more information on how to
         * design these icons.
         *
         * @param icon A resource ID in the application's package of the drawable to use.
         */
        public @NonNull Builder setSmallIcon(int icon) {
            mNotification.icon = icon;
            return this;
        }

        /**
         * A variant of {@link #setSmallIcon(int) setSmallIcon(int)} that takes an additional
         * level parameter for when the icon is a {@link android.graphics.drawable.LevelListDrawable
         * LevelListDrawable}.
         *
         * @param icon A resource ID in the application's package of the drawable to use.
         * @param level The level to use for the icon.
         *
         * @see android.graphics.drawable.LevelListDrawable
         */
        public @NonNull Builder setSmallIcon(int icon, int level) {
            mNotification.icon = icon;
            mNotification.iconLevel = level;
            return this;
        }

        /**
         * Silences this instance of the notification, regardless of the sounds or vibrations set
         * on the notification or notification channel.
         *
         * @deprecated use {@link #setSilent(boolean)}
         */
        @Deprecated
        public @NonNull Builder setNotificationSilent() {
            mSilent = true;
            return this;
        }

        /**
         * If {@code true}, silences this instance of the notification, regardless of the sounds or
         * vibrations set on the notification or notification channel. If {@code false}, then the
         * normal sound and vibration logic applies. Defaults to {@code false}.
         */
        public @NonNull Builder setSilent(boolean silent) {
            mSilent = silent;
            return this;
        }

        /**
         * Set the title (first row) of the notification, in a standard notification.
         */
        public @NonNull Builder setContentTitle(@Nullable CharSequence title) {
            mContentTitle = limitCharSequenceLength(title);
            return this;
        }

        /**
         * Set the text (second row) of the notification, in a standard notification.
         */
        public @NonNull Builder setContentText(@Nullable CharSequence text) {
            mContentText = limitCharSequenceLength(text);
            return this;
        }

        /**
         * This provides some additional information that is displayed in the notification. No
         * guarantees are given where exactly it is displayed.
         *
         * <p>This information should only be provided if it provides an essential
         * benefit to the understanding of the notification. The more text you provide the
         * less readable it becomes. For example, an email client should only provide the account
         * name here if more than one email account has been added.</p>
         *
         * <p>As of {@link android.os.Build.VERSION_CODES#N} this information is displayed in the
         * notification header area.</p>
         *
         * <p>On Android versions before {@link android.os.Build.VERSION_CODES#N}
         * this will be shown in the third line of text in the platform notification template.
         * You should not be using {@link #setProgress(int, int, boolean)} at the
         * same time on those versions; they occupy the same place.
         * </p>
         */
        public @NonNull Builder setSubText(@Nullable CharSequence text) {
            mSubText = limitCharSequenceLength(text);
            return this;
        }

        /**
         * Provides text that will appear as a link to your application's settings.
         *
         * <p>This text does not appear within notification {@link Style templates} but may
         * appear when the user uses an affordance to learn more about the notification.
         * Additionally, this text will not appear unless you provide a valid link target by
         * handling {@link #INTENT_CATEGORY_NOTIFICATION_PREFERENCES}.
         *
         * <p>This text is meant to be concise description about what the user can customize
         * when they click on this link. The recommended maximum length is 40 characters.
         *
         * <p>Prior to {@link Build.VERSION_CODES#O} this field has no effect.
         */
        public @NonNull Builder setSettingsText(@Nullable CharSequence text) {
            mSettingsText = limitCharSequenceLength(text);
            return this;
        }

        /**
         * Set the remote input history.
         *
         * This should be set to the most recent inputs that have been sent
         * through a {@link RemoteInput} of this Notification and cleared once the it is no
         * longer relevant (e.g. for chat notifications once the other party has responded).
         *
         * The most recent input must be stored at the 0 index, the second most recent at the
         * 1 index, etc. Note that the system will limit both how far back the inputs will be shown
         * and how much of each individual input is shown.
         *
         * <p>Note: The reply text will only be shown on notifications that have least one action
         * with a {@code RemoteInput}.</p>
         */
        public @NonNull Builder setRemoteInputHistory(CharSequence @Nullable [] text) {
            mRemoteInputHistory = text;
            return this;
        }

        /**
         * Sets the number of items this notification represents.
         *
         * On the latest platforms, this may be displayed as a badge count for Launchers that
         * support badging. Prior to {@link android.os.Build.VERSION_CODES#O} it could be
         * shown in the header. And prior to {@link android.os.Build.VERSION_CODES#N} this was
         * shown in the notification on the right side.
         */
        public @NonNull Builder setNumber(int number) {
            mNumber = number;
            return this;
        }

        /**
         * A small piece of additional information pertaining to this notification.
         *
         * Where this text is displayed varies between platform versions.
         *
         * Use {@link #setSubText(CharSequence)} instead to set a text in the header.
         * For legacy apps targeting a version below {@link android.os.Build.VERSION_CODES#N} this
         * field will still show up, but the subtext will take precedence.
         */
        public @NonNull Builder setContentInfo(@Nullable CharSequence info) {
            mContentInfo = limitCharSequenceLength(info);
            return this;
        }

        /**
         * Sets a very short string summarizing the most critical information contained in the
         * notification. Suggested max length is 7 characters, and there is no guarantee how much or
         * how little of this text will be shown.
         */
        @NonNull
        public Builder setShortCriticalText(@Nullable String shortCriticalText) {
            mShortCriticalText = shortCriticalText;
            if (Build.VERSION.SDK_INT < 36) {
                getExtras().putString(EXTRA_SHORT_CRITICAL_TEXT, shortCriticalText);
            }
            return this;
        }

        /**
         * Set whether this notification is requesting to be a promoted ongoing notification.
         *
         * <p>This is the first requirement of {@link Notification#hasPromotableCharacteristics()}.
         */
        @SuppressWarnings("MissingGetterMatchingBuilder")
        @NonNull
        public Builder setRequestPromotedOngoing(boolean requestPromotedOngoing) {
            getExtras().putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, requestPromotedOngoing);
            return this;
        }

        /**
         * Set the progress this notification represents, which may be
         * represented as a {@link android.widget.ProgressBar}.
         */
        public @NonNull Builder setProgress(int max, int progress, boolean indeterminate) {
            mProgressMax = max;
            mProgress = progress;
            mProgressIndeterminate = indeterminate;
            return this;
        }

        /**
         * Supply a custom RemoteViews to use instead of the standard one.
         */
        public @NonNull Builder setContent(@Nullable RemoteViews views) {
            mNotification.contentView = views;
            return this;
        }

        /**
         * Supply a {@link PendingIntent} to send when the notification is clicked.
         * If you do not supply an intent, you can now add PendingIntents to individual
         * views to be launched when clicked by calling {@link RemoteViews#setOnClickPendingIntent
         * RemoteViews.setOnClickPendingIntent(int,PendingIntent)}.  Be sure to
         * read {@link Notification#contentIntent Notification.contentIntent} for
         * how to correctly use this.
         */
        public @NonNull Builder setContentIntent(@Nullable PendingIntent intent) {
            mContentIntent = intent;
            return this;
        }

        /**
         * Supply a {@link PendingIntent} to send when the notification is cleared by the user
         * directly from the notification panel.  For example, this intent is sent when the user
         * clicks the "Clear all" button, or the individual "X" buttons on notifications.  This
         * intent is not sent when the application calls
         * {@link android.app.NotificationManager#cancel NotificationManager.cancel(int)}.
         */
        public @NonNull Builder setDeleteIntent(@Nullable PendingIntent intent) {
            mNotification.deleteIntent = intent;
            return this;
        }

        /**
         * An intent to launch instead of posting the notification to the status bar.
         * Only for use with extremely high-priority notifications demanding the user's
         * <strong>immediate</strong> attention, such as an incoming phone call or
         * alarm clock that the user has explicitly set to a particular time.
         * If this facility is used for something else, please give the user an option
         * to turn it off and use a normal notification, as this can be extremely
         * disruptive.
         *
         * <p>
         * On some platforms, the system UI may choose to display a heads-up notification,
         * instead of launching this intent, while the user is using the device.
         * </p>
         *
         * @param intent The pending intent to launch.
         * @param highPriority Passing true will cause this notification to be sent
         *          even if other notifications are suppressed.
         */
        @SuppressWarnings("deprecation")
        public @NonNull Builder setFullScreenIntent(@Nullable PendingIntent intent,
                boolean highPriority) {
            mFullScreenIntent = intent;
            setFlag(FLAG_HIGH_PRIORITY, highPriority);
            return this;
        }

        /**
         * Sets the "ticker" text which is sent to accessibility services. Prior to
         * {@link Build.VERSION_CODES#LOLLIPOP}, sets the text that is displayed in the status bar
         * when the notification first arrives.
         */
        public @NonNull Builder setTicker(@Nullable CharSequence tickerText) {
            mNotification.tickerText = limitCharSequenceLength(tickerText);
            return this;
        }

        /**
         * Sets the "ticker" text which is sent to accessibility services. Prior to
         * {@link Build.VERSION_CODES#LOLLIPOP}, sets the text that is displayed in the status bar
         * when the notification first arrives, and also a RemoteViews object that may be displayed
         * instead on some devices.
         *
         * @deprecated use {@link #setTicker(CharSequence)}
         */
        @Deprecated
        public @NonNull Builder setTicker(@Nullable CharSequence tickerText,
                @Nullable RemoteViews views) {
            mNotification.tickerText = limitCharSequenceLength(tickerText);
            mTickerView = views;
            return this;
        }

        /**
         * Sets the large icon that is shown in the notification. Icons will be scaled on versions
         * before API 27. Starting in API 27, the framework does this automatically.
         */
        public @NonNull Builder setLargeIcon(@Nullable Bitmap icon) {
            mLargeIcon = icon == null ? null : IconCompat.createWithBitmap(
             reduceLargeIconSize(mContext, icon));
            return this;
        }

        /**
         * Sets the large icon that is shown in the notification. Starting in API 27, the framework
         * scales icons automatically. Before API 27, for safety, {@code #reduceLargeIconSize}
         * should be called on bitmaps before putting them in an {@code Icon} and passing them
         * into this function.
         */
        public @NonNull Builder setLargeIcon(@Nullable Icon icon) {
            mLargeIcon = icon == null ? null : IconCompat.createFromIcon(icon);
            return this;
        }

        /**
         * Set the sound to play.  It will play on the default stream.
         *
         * <p>
         * On some platforms, a notification that is noisy is more likely to be presented
         * as a heads-up notification.
         * </p>
         *
         * <p>On platforms {@link Build.VERSION_CODES#O} and above this value is ignored in favor
         * of the value set on the {@link #setChannelId(String) notification's channel}. On older
         * platforms, this value is still used, so it is still required for apps supporting
         * those platforms.</p>
         *
         * @see NotificationChannelCompat.Builder#setSound(Uri, AudioAttributes)
         */
        public @NonNull Builder setSound(@Nullable Uri sound) {
            mNotification.sound = sound;
            mNotification.audioStreamType = Notification.STREAM_DEFAULT;
            AudioAttributes.Builder builder = new AudioAttributes.Builder();
            builder = builder.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION);
            builder = builder.setUsage(AudioAttributes.USAGE_NOTIFICATION);
            mNotification.audioAttributes = builder.build();
            return this;
        }

        /**
         * Set the sound to play.  It will play on the stream you supply.
         *
         * <p>
         * On some platforms, a notification that is noisy is more likely to be presented
         * as a heads-up notification.
         * </p>
         *
         * <p>On platforms {@link Build.VERSION_CODES#O} and above this value is ignored in favor
         * of the value set on the {@link #setChannelId(String) notification's channel}. On older
         * platforms, this value is still used, so it is still required for apps supporting
         * those platforms.</p>
         *
         * @see NotificationChannelCompat.Builder#setSound(Uri, AudioAttributes)
         * @see Notification#STREAM_DEFAULT
         * @see AudioManager for the <code>STREAM_</code> constants.
         */
        public @NonNull Builder setSound(@Nullable Uri sound, @StreamType int streamType) {
            mNotification.sound = sound;
            mNotification.audioStreamType = streamType;
            AudioAttributes.Builder builder = new AudioAttributes.Builder();
            builder = builder.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION);
            builder = builder.setLegacyStreamType(streamType);
            mNotification.audioAttributes = builder.build();
            return this;
        }

        /**
         * Set the vibration pattern to use.
         *
         * <p>
         * On some platforms, a notification that vibrates is more likely to be presented
         * as a heads-up notification.
         * </p>
         *
         * <p>On platforms {@link Build.VERSION_CODES#O} and above this value is ignored in favor
         * of the value set on the {@link #setChannelId(String) notification's channel}. On older
         * platforms, this value is still used, so it is still required for apps supporting
         * those platforms.</p>
         *
         * @see NotificationChannelCompat.Builder#setVibrationEnabled(boolean)
         * @see NotificationChannelCompat.Builder#setVibrationPattern(long[])
         * @see android.os.Vibrator for a discussion of the <code>pattern</code>
         * parameter.
         */
        public @NonNull Builder setVibrate(long @Nullable [] pattern) {
            mNotification.vibrate = pattern;
            return this;
        }

        /**
         * Set the argb value that you would like the LED on the device to blink, as well as the
         * rate.  The rate is specified in terms of the number of milliseconds to be on
         * and then the number of milliseconds to be off.
         *
         * <p>On platforms {@link Build.VERSION_CODES#O} and above this value is ignored in favor
         * of the value set on the {@link #setChannelId(String) notification's channel}. On older
         * platforms, this value is still used, so it is still required for apps supporting
         * those platforms.</p>
         *
         * @see NotificationChannelCompat.Builder#setLightsEnabled(boolean)
         * @see NotificationChannelCompat.Builder#setLightColor(int)
         */
        public @NonNull Builder setLights(@ColorInt int argb, int onMs, int offMs) {
            mNotification.ledARGB = argb;
            mNotification.ledOnMS = onMs;
            mNotification.ledOffMS = offMs;
            boolean showLights = mNotification.ledOnMS != 0 && mNotification.ledOffMS != 0;
            mNotification.flags = (mNotification.flags & ~Notification.FLAG_SHOW_LIGHTS) |
                    (showLights ? Notification.FLAG_SHOW_LIGHTS : 0);
            return this;
        }

        /**
         * Set whether this is an ongoing notification.
         *
         * Ongoing notifications cannot be dismissed by the user, so your application or service
         * must take care of canceling them.
         *
         * They are typically used to indicate a background task that the user is actively engaged
         * with (e.g., playing music) or is pending in some way and therefore occupying the device
         * (e.g., a file download, sync operation, active network connection).
         *
         * @see Notification#FLAG_ONGOING_EVENT
         */
        public @NonNull Builder setOngoing(boolean ongoing) {
            setFlag(Notification.FLAG_ONGOING_EVENT, ongoing);
            return this;
        }

        /**
         * Set whether this notification should be colorized. When set, the color set with
         * {@link #setColor(int)} will be used as the background color of this notification.
         * <p>
         * This should only be used for high priority ongoing tasks like navigation, an ongoing
         * call, or other similarly high-priority events for the user.
         * <p>
         * For most styles, the coloring will only be applied if the notification is for a
         * foreground service notification.
         * <p>
         * However, for MediaStyle and DecoratedMediaCustomViewStyle notifications
         * that have a media session attached there is no such requirement.
         * <p>
         * Calling this method on any version prior to {@link android.os.Build.VERSION_CODES#O} will
         * not have an effect on the notification and it won't be colorized.
         *
         * @see #setColor(int)
         */
        public @NonNull Builder setColorized(boolean colorize) {
            mColorized = colorize;
            mColorizedSet = true;
            return this;
        }

        /**
         * Set this flag if you would only like the sound, vibrate
         * and ticker to be played if the notification is not already showing.
         */
        public @NonNull Builder setOnlyAlertOnce(boolean onlyAlertOnce) {
            setFlag(Notification.FLAG_ONLY_ALERT_ONCE, onlyAlertOnce);
            return this;
        }

        /**
         * Setting this flag will make it so the notification is automatically
         * canceled when the user clicks it in the panel.
         */
        public @NonNull Builder setAutoCancel(boolean autoCancel) {
            setFlag(Notification.FLAG_AUTO_CANCEL, autoCancel);
            return this;
        }

        /**
         * Set whether or not this notification is only relevant to the current device.
         *
         * <p>Some notifications can be bridged to other devices for remote display.
         * This hint can be set to recommend this notification not be bridged.
         */
        public @NonNull Builder setLocalOnly(boolean b) {
            mLocalOnly = b;
            return this;
        }

        /**
         * Set the notification category.
         *
         * <p>Must be one of the predefined notification categories (see the <code>CATEGORY_*</code>
         * constants in {@link Notification}) that best describes this notification.
         * May be used by the system for ranking and filtering.
         */
        public @NonNull Builder setCategory(@Nullable String category) {
            mCategory = category;
            return this;
        }

        // TODO (b/149433438) support person field

        /**
         * Set the default notification options that will be used.
         * <p>
         * The value should be one or more of the following fields combined with
         * bitwise-or:
         * {@link Notification#DEFAULT_SOUND}, {@link Notification#DEFAULT_VIBRATE},
         * {@link Notification#DEFAULT_LIGHTS}.
         * <p>
         * For all default values, use {@link Notification#DEFAULT_ALL}.
         *
         * <p>On platforms {@link Build.VERSION_CODES#O} and above this value is ignored in favor
         * of the values set on the {@link #setChannelId(String) notification's channel}. On older
         * platforms, this value is still used, so it is still required for apps supporting
         * those platforms.</p>
         *
         * @see NotificationChannelCompat.Builder#setVibrationEnabled(boolean)
         * @see NotificationChannelCompat.Builder#setLightsEnabled(boolean)
         */
        public @NonNull Builder setDefaults(int defaults) {
            mNotification.defaults = defaults;
            if ((defaults & Notification.DEFAULT_LIGHTS) != 0) {
                mNotification.flags |= Notification.FLAG_SHOW_LIGHTS;
            }
            return this;
        }

        private void setFlag(int mask, boolean value) {
            if (value) {
                mNotification.flags |= mask;
            } else {
                mNotification.flags &= ~mask;
            }
        }

        /**
         * Set the relative priority for this notification.
         *
         * <p>Priority is an indication of how much of the user's valuable attention should be
         * consumed by this notification. Low-priority notifications may be hidden from
         * the user in certain situations, while the user might be interrupted for a
         * higher-priority notification. The system sets a notification's priority based on
         * various factors including the setPriority value. The effect may differ slightly on
         * different platforms.
         *
         * <p>On platforms {@link Build.VERSION_CODES#O} and above this value is ignored in favor
         * of the importance value set on the {@link #setChannelId(String) notification's channel}.
         * On older platforms, this value is still used, so it is still required for apps
         * supporting those platforms.</p>
         *
         * @see NotificationChannelCompat.Builder#setImportance(int)
         *
         * @param pri Relative priority for this notification. Must be one of
         *     the priority constants defined by {@link NotificationCompat}.
         *     Acceptable values range from {@link NotificationCompat#PRIORITY_MIN} (-2) to
         *     {@link NotificationCompat#PRIORITY_MAX} (2).
         */
        public @NonNull Builder setPriority(int pri) {
            mPriority = pri;
            return this;
        }

        /**
         * Add a person that is relevant to this notification.
         *
         * <P>
         * Depending on user preferences, this annotation may allow the notification to pass
         * through interruption filters, and to appear more prominently in the user interface.
         * </P>
         *
         * <P>
         * The person should be specified by the {@code String} representation of a
         * {@link android.provider.ContactsContract.Contacts#CONTENT_LOOKUP_URI}.
         * </P>
         *
         * <P>The system will also attempt to resolve {@code mailto:} and {@code tel:} schema
         * URIs.  The path part of these URIs must exist in the contacts database, in the
         * appropriate column, or the reference will be discarded as invalid. Telephone schema
         * URIs will be resolved by {@link android.provider.ContactsContract.PhoneLookup}.
         * </P>
         *
         * @param uri A URI for the person.
         * @see Notification#EXTRA_PEOPLE
         * @deprecated use {@link #addPerson(Person)}
         */
        @Deprecated
        public @NonNull Builder addPerson(@Nullable String uri) {
            if (uri != null && !uri.isEmpty()) {
                mPeople.add(uri);
            }
            return this;
        }

        /**
         * Add a person that is relevant to this notification.
         *
         * <P>
         * Depending on user preferences, this annotation may allow the notification to pass
         * through interruption filters, if this notification is of category {@link #CATEGORY_CALL}
         * or {@link #CATEGORY_MESSAGE}. The addition of people may also cause this notification to
         * appear more prominently in the user interface.
         * </P>
         *
         * <P>
         * A person should usually contain a uri in order to benefit from the ranking boost.
         * However, even if no uri is provided, it's beneficial to provide other people in the
         * notification, such that listeners and voice only devices can announce and handle them
         * properly.
         * </P>
         *
         * @param person the person to add.
         * @see #EXTRA_PEOPLE_LIST
         */
        public @NonNull Builder addPerson(final @Nullable Person person) {
            if (person != null) {
                mPersonList.add(person);
            }
            return this;
        }

        /**
         * Clear any people added via either {@link #addPerson(Person)} or
         * {@link #addPerson(String)}
         */
        public @NonNull Builder clearPeople() {
            mPersonList.clear();
            mPeople.clear();
            return this;
        }

        /**
         * Set this notification to be part of a group of notifications sharing the same key.
         * Grouped notifications may display in a cluster or stack on devices which
         * support such rendering.
         *
         * <p>To make this notification the summary for its group, also call
         * {@link #setGroupSummary}. A sort order can be specified for group members by using
         * {@link #setSortKey}.
         * @param groupKey The group key of the group.
         * @return this object for method chaining
         */
        public @NonNull Builder setGroup(@Nullable String groupKey) {
            mGroupKey = groupKey;
            return this;
        }

        /**
         * Set this notification to be the group summary for a group of notifications.
         * Grouped notifications may display in a cluster or stack on devices which
         * support such rendering. Requires a group key also be set using {@link #setGroup}.
         * @param isGroupSummary Whether this notification should be a group summary.
         * @return this object for method chaining
         */
        public @NonNull Builder setGroupSummary(boolean isGroupSummary) {
            mGroupSummary = isGroupSummary;
            return this;
        }

        /**
         * Set a sort key that orders this notification among other notifications from the
         * same package. This can be useful if an external sort was already applied and an app
         * would like to preserve this. Notifications will be sorted lexicographically using this
         * value, although providing different priorities in addition to providing sort key may
         * cause this value to be ignored.
         *
         * <p>This sort key can also be used to order members of a notification group. See
         * {@link Builder#setGroup}.
         *
         * @see String#compareTo(String)
         */
        public @NonNull Builder setSortKey(@Nullable String sortKey) {
            mSortKey = sortKey;
            return this;
        }

        /**
         * Merge additional metadata into this notification.
         *
         * <p>Values within the Bundle will replace existing extras values in this Builder.
         *
         * @see Notification#extras
         */
        public @NonNull Builder addExtras(@Nullable Bundle extras) {
            if (extras != null) {
                if (mExtras == null) {
                    mExtras = new Bundle(extras);
                } else {
                    mExtras.putAll(extras);
                }
            }
            return this;
        }

        /**
         * Set metadata for this notification.
         *
         * <p>A reference to the Bundle is held for the lifetime of this Builder, and the Bundle's
         * current contents are copied into the Notification each time {@link #build()} is
         * called.
         *
         * <p>Replaces any existing extras values with those from the provided Bundle.
         * Use {@link #addExtras} to merge in metadata instead.
         *
         * @see Notification#extras
         */
        public @NonNull Builder setExtras(@Nullable Bundle extras) {
            mExtras = extras;
            return this;
        }

        /**
         * Get the current metadata Bundle used by this notification Builder.
         *
         * <p>The returned Bundle is shared with this Builder.
         *
         * <p>The current contents of this Bundle are copied into the Notification each time
         * {@link #build()} is called.
         *
         * @see Notification#extras
         */
        public @NonNull Bundle getExtras() {
            if (mExtras == null) {
                mExtras = new Bundle();
            }
            return mExtras;
        }

        /**
         * Add an action to this notification. Actions are typically displayed by
         * the system as a button adjacent to the notification content.
         * <br>
         * Action buttons won't appear on platforms prior to Android 4.1. Action
         * buttons depend on expanded notifications, which are only available in Android 4.1
         * and later. To ensure that an action button's functionality is always available, first
         * implement the functionality in the {@link android.app.Activity} that starts when a user
         * clicks the  notification (see {@link #setContentIntent setContentIntent()}), and then
         * enhance the notification by implementing the same functionality with
         * {@link #addAction addAction()}.
         *
         * @param icon Resource ID of a drawable that represents the action.
         * @param title Text describing the action.
         * @param intent {@link android.app.PendingIntent} to be fired when the action is invoked.
         */
        public @NonNull Builder addAction(int icon, @Nullable CharSequence title,
                @Nullable PendingIntent intent) {
            mActions.add(new Action(icon, title, intent));
            return this;
        }

        /**
         * Add an action to this notification. Actions are typically displayed by
         * the system as a button adjacent to the notification content.
         * <br>
         * Action buttons won't appear on platforms prior to Android 4.1. Action
         * buttons depend on expanded notifications, which are only available in Android 4.1
         * and later. To ensure that an action button's functionality is always available, first
         * implement the functionality in the {@link android.app.Activity} that starts when a user
         * clicks the  notification (see {@link #setContentIntent setContentIntent()}), and then
         * enhance the notification by implementing the same functionality with
         * {@link #addAction addAction()}.
         *
         * @param action The action to add.
         */
        public @NonNull Builder addAction(@Nullable Action action) {
            if (action != null) {
                mActions.add(action);
            }
            return this;
        }

        /**
         * Clear any actions added via {@link #addAction}
         */
        public @NonNull Builder clearActions() {
            mActions.clear();
            return this;
        }

        /**
         * Add an invisible action to this notification. Invisible actions are never displayed by
         * the system, but can be retrieved and used by other application listening to
         * system notifications. Invisible actions are supported from Android 4.4.4 (API 20) and can
         * be retrieved using {@link NotificationCompat#getInvisibleActions(Notification)}.
         *
         * @param icon Resource ID of a drawable that represents the action.
         * @param title Text describing the action.
         * @param intent {@link android.app.PendingIntent} to be fired when the action is invoked.
         */
        public @NonNull Builder addInvisibleAction(int icon, @Nullable CharSequence title,
                @Nullable PendingIntent intent) {
            mInvisibleActions.add(new Action(icon, title, intent));
            return this;
        }

        /**
         * Add an invisible action to this notification. Invisible actions are never displayed by
         * the system, but can be retrieved and used by other application listening to
         * system notifications. Invisible actions are supported from Android 4.4.4 (API 20) and can
         * be retrieved using {@link NotificationCompat#getInvisibleActions(Notification)}.
         *
         * @param action The action to add.
         */
        public @NonNull Builder addInvisibleAction(@Nullable Action action) {
            if (action != null) {
                mInvisibleActions.add(action);
            }
            return this;
        }

        /**
         * Clear any invisible actions added via {@link #addInvisibleAction}
         */
        public @NonNull Builder clearInvisibleActions() {
            mInvisibleActions.clear();
            // NOTE: Building a notification actually mutates the extras on the builder, so we
            //  have to clear out those changes in order for the function to really work.
            Bundle carExtenderBundle = mExtras.getBundle(CarExtender.EXTRA_CAR_EXTENDER);
            if (carExtenderBundle != null) {
                carExtenderBundle = new Bundle(carExtenderBundle);
                carExtenderBundle.remove(CarExtender.EXTRA_INVISIBLE_ACTIONS);
                mExtras.putBundle(CarExtender.EXTRA_CAR_EXTENDER, carExtenderBundle);
            }
            return this;
        }

        /**
         * Add a rich notification style to be applied at build time.
         * <br>
         * If the platform does not provide rich notification styles, this method has no effect. The
         * user will always see the normal notification style.
         *
         * @param style Object responsible for modifying the notification style.
         */
        public @NonNull Builder setStyle(@Nullable Style style) {
            if (mStyle != style) {
                mStyle = style;
                if (mStyle != null) {
                    mStyle.setBuilder(this);
                }
            }
            return this;
        }

        /**
         * Sets {@link Notification#color}.
         *
         * @param argb The accent color to use
         *
         * @return The same Builder.
         */
        public @NonNull Builder setColor(@ColorInt int argb) {
            mColor = argb;
            return this;
        }

        /**
         * Sets {@link Notification#visibility}.
         *
         * @param visibility One of {@link Notification#VISIBILITY_PRIVATE} (the default),
         *                   {@link Notification#VISIBILITY_PUBLIC}, or
         *                   {@link Notification#VISIBILITY_SECRET}.
         */
        public @NonNull Builder setVisibility(@NotificationVisibility int visibility) {
            mVisibility = visibility;
            return this;
        }

        /**
         * Supply a replacement Notification whose contents should be shown in insecure contexts
         * (i.e. atop the secure lockscreen). See {@link Notification#visibility} and
         * {@link #VISIBILITY_PUBLIC}.
         *
         * @param n A replacement notification, presumably with some or all info redacted.
         * @return The same Builder.
         */
        public @NonNull Builder setPublicVersion(@Nullable Notification n) {
            mPublicVersion = n;
            return this;
        }

        /**
         * @return whether the builder's createContentView() and peer methods should use the
         * user-supplied RemoteViews.  This will be true unless the style is a 'decorated' style,
         * because those styles are defined to wrap that RemoteViews object.
         */
        private boolean useExistingRemoteView() {
            return mStyle == null || !mStyle.displayCustomViewInline();
        }

        /**
         * Construct a RemoteViews for the final notification layout.
         */
        @SuppressLint("BuilderSetStyle")  // This API is copied from Notification.Builder
        public @Nullable RemoteViews createContentView() {
            // If the user setCustomContentView(), return it if appropriate for the style.
            if (mContentView != null && useExistingRemoteView()) {
                return mContentView;
            }
            // If there's a NotificationCompat.Style, and that Style produces a content view,
            // then that content view is a backport of the Style's appearance to an old platform
            // version, and it should be returned.
            NotificationCompatBuilder compatBuilder = new NotificationCompatBuilder(this);
            if (mStyle != null) {
                RemoteViews styleView = mStyle.makeContentView(compatBuilder);
                if (styleView != null) {
                    return styleView;
                }
            }
            Notification notification = compatBuilder.build();
            if (Build.VERSION.SDK_INT >= 24) {
                // On N and newer, do some magic and delegate to the Platform's implementation
                return Api24Impl.createContentView(Api24Impl.recoverBuilder(mContext,
                        notification));
            } else {
                // Before N, delegate to the deprecated field on the built notification
                return notification.contentView;
            }
        }

        /**
         * Construct a RemoteViews for the final big notification layout.
         */
        @SuppressLint("BuilderSetStyle")  // This API is copied from Notification.Builder
        public @Nullable RemoteViews createBigContentView() {
            // If the user setCustomBigContentView(), return it if appropriate for the style.
            if (mBigContentView != null && useExistingRemoteView()) {
                return mBigContentView;
            }
            // If there's a NotificationCompat.Style, and that Style produces a content view,
            // then that content view is a backport of the Style's appearance to an old platform
            // version, and it should be returned.
            NotificationCompatBuilder compatBuilder = new NotificationCompatBuilder(this);
            if (mStyle != null) {
                RemoteViews styleView = mStyle.makeBigContentView(compatBuilder);
                if (styleView != null) {
                    return styleView;
                }
            }
            Notification notification = compatBuilder.build();
            if (Build.VERSION.SDK_INT >= 24) {
                // On N and newer, do some magic and delegate to the Platform's implementation
                return Api24Impl.createBigContentView(Api24Impl.recoverBuilder(mContext,
                        notification));
            } else {
                // Before N, delegate to the deprecated field on the built notification
                return notification.bigContentView;
            }
        }

        /**
         * Construct a RemoteViews for the final heads-up notification layout.
         */
        @SuppressLint("BuilderSetStyle")  // This API is copied from Notification.Builder
        public @Nullable RemoteViews createHeadsUpContentView() {
            // If the user setCustomHeadsUpContentView(), return it if appropriate for the style.
            if (mHeadsUpContentView != null && useExistingRemoteView()) {
                return mHeadsUpContentView;
            }
            // If there's a NotificationCompat.Style, and that Style produces a content view,
            // then that content view is a backport of the Style's appearance to an old platform
            // version, and it should be returned.
            NotificationCompatBuilder compatBuilder = new NotificationCompatBuilder(this);
            if (mStyle != null) {
                RemoteViews styleView = mStyle.makeHeadsUpContentView(compatBuilder);
                if (styleView != null) {
                    return styleView;
                }
            }
            Notification notification = compatBuilder.build();
            if (Build.VERSION.SDK_INT >= 24) {
                // On N and newer, do some magic and delegate to the Platform's implementation
                return Api24Impl.createHeadsUpContentView(Api24Impl.recoverBuilder(mContext,
                        notification));
            } else {
                // Before N, delegate to the deprecated field on the built notification
                return notification.headsUpContentView;
            }
        }

        /**
         * Supply custom RemoteViews to use instead of the platform template.
         *
         * This will override the layout that would otherwise be constructed by this Builder
         * object.
         */
        public @NonNull Builder setCustomContentView(@Nullable RemoteViews contentView) {
            mContentView = contentView;
            return this;
        }

        /**
         * Supply custom RemoteViews to use instead of the platform template in the expanded form.
         *
         * This will override the expanded layout that would otherwise be constructed by this
         * Builder object.
         *
         * No-op on versions prior to {@link android.os.Build.VERSION_CODES#JELLY_BEAN}.
         */
        public @NonNull Builder setCustomBigContentView(@Nullable RemoteViews contentView) {
            mBigContentView = contentView;
            return this;
        }

        /**
         * Supply custom RemoteViews to use instead of the platform template in the heads up dialog.
         *
         * This will override the heads-up layout that would otherwise be constructed by this
         * Builder object.
         *
         * No-op on versions prior to {@link android.os.Build.VERSION_CODES#LOLLIPOP}.
         */
        public @NonNull Builder setCustomHeadsUpContentView(@Nullable RemoteViews contentView) {
            mHeadsUpContentView = contentView;
            return this;
        }

        /**
         * Specifies the channel the notification should be delivered on.
         *
         * No-op on versions prior to {@link android.os.Build.VERSION_CODES#O}.
         */
        public @NonNull Builder setChannelId(@NonNull String channelId) {
            mChannelId = channelId;
            return this;
        }

        /**
         * Specifies the time at which this notification should be canceled, if it is not already
         * canceled.
         *
         * No-op on versions prior to {@link android.os.Build.VERSION_CODES#O}.
         */
        public @NonNull Builder setTimeoutAfter(long durationMs) {
            mTimeout = durationMs;
            return this;
        }

        /**
         * From Android 11, messaging notifications (those that use {@link MessagingStyle}) that
         * use this method to link to a published long-lived sharing shortcut may appear in a
         * dedicated Conversation section of the shade and may show configuration options that
         * are unique to conversations. This behavior should be reserved for person to person(s)
         * conversations where there is a likely social obligation for an individual to respond.
         * <p>
         * For example, the following are some examples of notifications that belong in the
         * conversation space:
         * <ul>
         * <li>1:1 conversations between two individuals</li>
         * <li>Group conversations between individuals where everyone can contribute</li>
         * </ul>
         * And the following are some examples of notifications that do not belong in the
         * conversation space:
         * <ul>
         * <li>Advertisements from a bot (even if personal and contextualized)</li>
         * <li>Engagement notifications from a bot</li>
         * <li>Directional conversations where there is an active speaker and many passive
         * individuals</li>
         * <li>Stream / posting updates from other individuals</li>
         * <li>Email, document comments, or other conversation types that are not real-time</li>
         * </ul>
         * </p>
         *
         * <p>
         * Additionally, this method can be used for all types of notifications to mark this
         * notification as duplicative of a Launcher shortcut. Launchers that show badges or
         * notification content may then suppress the shortcut in favor of the content of this
         * notification.
         * <p>
         * If this notification has {@link BubbleMetadata} attached that was created with
         * a shortcutId a check will be performed to ensure the shortcutId supplied to bubble
         * metadata matches the shortcutId set here, if one was set. If the shortcutId's were
         * specified but do not match, an exception is thrown.
         *
         * @param shortcutId the {@link ShortcutInfoCompat#getId() id} of the shortcut this
         *                   notification is linked to
         *
         * @see BubbleMetadata.Builder#Builder()
         */
        public @NonNull Builder setShortcutId(@Nullable String shortcutId) {
            mShortcutId = shortcutId;
            return this;
        }

        /**
         * Populates this notification with given {@link ShortcutInfoCompat}.
         *
         * <p>Sets {@link androidx.core.content.pm.ShortcutInfoCompat#getId() shortcutId} based on
         * the given shortcut. In addition, it also sets {@link LocusIdCompat locusId} and
         * {@link #setContentTitle(CharSequence) contentTitle} if they were empty.
         *
         */
        public @NonNull Builder setShortcutInfo(final @Nullable ShortcutInfoCompat shortcutInfo) {
            // TODO: b/156784300 add check to filter long-lived and sharing shortcut
            if (shortcutInfo == null) {
                return this;
            }
            mShortcutId = shortcutInfo.getId();
            if (mLocusId == null) {
                if (shortcutInfo.getLocusId() != null) {
                    mLocusId = shortcutInfo.getLocusId();
                } else if (shortcutInfo.getId() != null) {
                    mLocusId = new LocusIdCompat(shortcutInfo.getId());
                }
            }
            if (mContentTitle == null) {
                setContentTitle(shortcutInfo.getShortLabel());
            }
            // TODO: b/156784300 include person info
            return this;
        }

        /**
         * Sets the {@link LocusIdCompat} associated with this notification.
         *
         * <p>This method should be called when the {@link LocusIdCompat} is used in other places
         * (such as {@link androidx.core.content.pm.ShortcutInfoCompat} and
         * {@link android.view.contentcapture.ContentCaptureContext}) so the device's intelligence
         * services can correlate them.
         */
        public @NonNull Builder setLocusId(final @Nullable LocusIdCompat locusId) {
            mLocusId = locusId;
            return this;
        }

        /**
         * Sets which icon to display as a badge for this notification.
         *
         * <p>Must be one of {@link #BADGE_ICON_NONE}, {@link #BADGE_ICON_SMALL},
         * {@link #BADGE_ICON_LARGE}.
         *
         * <p><strong>Note:</strong> This value might be ignored, for launchers that don't support
         * badge icons.
         */
        public @NonNull Builder setBadgeIconType(@BadgeIconType int icon) {
            mBadgeIcon = icon;
            return this;
        }

        /**
         * Sets the group alert behavior for this notification. Use this method to mute this
         * notification if alerts for this notification's group should be handled by a different
         * notification. This is only applicable for notifications that belong to a
         * {@link #setGroup(String) group}. This must be called on all notifications you want to
         * mute. For example, if you want only the summary of your group to make noise, all
         * children in the group should have the group alert behavior {@link #GROUP_ALERT_SUMMARY}.
         *
         * <p> The default value is {@link #GROUP_ALERT_ALL}.</p>
         */
        public @NonNull Builder setGroupAlertBehavior(@GroupAlertBehavior int groupAlertBehavior) {
            mGroupAlertBehavior = groupAlertBehavior;
            return this;
        }

        /**
         * Specify a desired visibility policy for a Notification associated with a
         * foreground service.  The default value is {@link #FOREGROUND_SERVICE_DEFAULT},
         * meaning the system can choose to defer visibility of the notification for
         * a short time after the service is started.  Pass
         * {@link NotificationCompat#FOREGROUND_SERVICE_IMMEDIATE FOREGROUND_SERVICE_IMMEDIATE}
         * to this method in order to guarantee that visibility is never deferred.  Pass
         * {@link NotificationCompat#FOREGROUND_SERVICE_DEFERRED FOREGROUND_SERVICE_DEFERRED}
         * to request that visibility is deferred whenever possible.
         *
         * <p class="note">Note that deferred visibility is not guaranteed.  There
         * may be some circumstances under which the system will show the foreground
         * service's associated Notification immediately even when the app has used
         * this method to explicitly request deferred display.</p>
         *
         * This method has no effect when running on versions prior to
         * {@link android.os.Build.VERSION_CODES#S}.
         */
        @SuppressWarnings("MissingGetterMatchingBuilder") // no underlying getter in platform API
        public @NonNull Builder setForegroundServiceBehavior(
                @ServiceNotificationBehavior int behavior) {
            mFgsDeferBehavior = behavior;
            return this;
        }

        /**
         * Sets the {@link BubbleMetadata} that will be used to display app content in a floating
         * window over the existing foreground activity.
         *
         * <p>This data will be ignored unless the notification is posted to a channel that
         * allows {@link android.app.NotificationChannel#canBubble() bubbles}.</p>
         *
         * <p>Notifications allowed to bubble that have valid bubble metadata will display in
         * collapsed state outside of the notification shade on unlocked devices. When a user
         * interacts with the collapsed state, the bubble intent will be invoked and displayed.</p>
         */
        public @NonNull Builder setBubbleMetadata(@Nullable BubbleMetadata data) {
            mBubbleMetadata = data;
            return this;
        }

        /**
         * Apply an extender to this notification builder. Extenders may be used to add
         * metadata or change options on this builder.
         */
        public @NonNull Builder extend(@NonNull Extender extender) {
            extender.extend(this);
            return this;
        }

        /**
         * Determines whether the platform can generate contextual actions for a notification.
         * By default this is true.
         */
        public @NonNull Builder setAllowSystemGeneratedContextualActions(boolean allowed) {
            mAllowSystemGeneratedContextualActions = allowed;
            return this;
        }

        /**
         * @deprecated Use {@link #build()} instead.
         */
        @Deprecated
        public @NonNull Notification getNotification() {
            return build();
        }

        /**
         * Combine all of the options that have been set and return a new {@link Notification}
         * object.
         */
        public @NonNull Notification build() {
            return new NotificationCompatBuilder(this).build();
        }

        protected static @Nullable CharSequence limitCharSequenceLength(@Nullable CharSequence cs) {
            if (cs == null) return cs;
            if (cs.length() > MAX_CHARSEQUENCE_LENGTH) {
                cs = cs.subSequence(0, MAX_CHARSEQUENCE_LENGTH);
            }
            return cs;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public RemoteViews getContentView() {
            return mContentView;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public RemoteViews getBigContentView() {
            return mBigContentView;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public RemoteViews getHeadsUpContentView() {
            return mHeadsUpContentView;
        }

        /**
         * return when if it is showing or 0 otherwise
         *
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public long getWhenIfShowing() {
            return mShowWhen ? mNotification.when : 0;
        }

        /**
         * @return the priority set on the notification
         *
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public int getPriority() {
            return mPriority;
        }

        /**
         * @return the foreground service behavior defined for the notification
         *
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public int getForegroundServiceBehavior() {
            return mFgsDeferBehavior;
        }

        /**
         * @return the color of the notification
         *
         */
        @ColorInt
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public int getColor() {
            return mColor;
        }

        /**
         * @return the {@link BubbleMetadata} of the notification
         *
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public @Nullable BubbleMetadata getBubbleMetadata() {
            return mBubbleMetadata;
        }

        /**
         * A class for wrapping calls to {@link Notification.Builder} methods which
         * were added in API 24; these calls must be wrapped to avoid performance issues.
         * See the UnsafeNewApiCall lint rule for more details.
         */
        @RequiresApi(24)
        static class Api24Impl {
            private Api24Impl() { }

            static Notification.Builder recoverBuilder(Context context, Notification n) {
                return Notification.Builder.recoverBuilder(context, n);
            }

            static RemoteViews createContentView(Notification.Builder builder) {
                return builder.createContentView();
            }

            static RemoteViews createHeadsUpContentView(Notification.Builder builder) {
                return builder.createHeadsUpContentView();
            }

            static RemoteViews createBigContentView(Notification.Builder builder) {
                return builder.createHeadsUpContentView();
            }
        }
    }

    /**
     * An object that can apply a rich notification style to a {@link Notification.Builder}
     * object.
     * <br>
     * If the platform does not provide rich notification styles, methods in this class have no
     * effect.
     */
    public static abstract class Style {
        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        protected Builder mBuilder;
        CharSequence mBigContentTitle;
        CharSequence mSummaryText;
        boolean mSummaryTextSet = false;

        /**
         * Link this rich notification style with a notification builder.
         */
        public void setBuilder(@Nullable Builder builder) {
            if (mBuilder != builder) {
                mBuilder = builder;
                if (mBuilder != null) {
                    mBuilder.setStyle(this);
                }
            }
        }

        /**
         * If this Style object has been set on a notification builder, this method will build
         * that notification and return it.  Otherwise, it will return {@code null}.
         */
        public @Nullable Notification build() {
            Notification notification = null;
            if (mBuilder != null) {
                notification = mBuilder.build();
            }
            return notification;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        protected @Nullable String getClassName() {
            // We can't crash for apps that write their own subclasses, so we return null
            return null;
        }

        /**
         * Applies the compat style data to the framework {@link Notification} in a backwards
         * compatible way. All other data should be stored within the Notification's extras.
         *
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public void apply(NotificationBuilderWithBuilderAccessor builder) {
        }

        /**
         * @return Whether custom content views are displayed inline in the style
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public boolean displayCustomViewInline() {
            return false;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public RemoteViews makeContentView(NotificationBuilderWithBuilderAccessor builder) {
            return null;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public RemoteViews makeBigContentView(NotificationBuilderWithBuilderAccessor builder) {
            return null;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public RemoteViews makeHeadsUpContentView(NotificationBuilderWithBuilderAccessor builder) {
            return null;
        }

        /**
         * This is called with the extras of the framework {@link Notification} during the
         * {@link Builder#build()} process, after <code>apply()</code> has been called.  This means
         * that you only need to add data which won't be populated by the framework Notification
         * which was built so far.
         *
         * Moreover, recovering builders and styles is only supported at API 19 and above, no
         * implementation is required for current BigTextStyle, BigPictureStyle, or InboxStyle.
         *
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public void addCompatExtras(@NonNull Bundle extras) {
            if (mSummaryTextSet) {
                extras.putCharSequence(EXTRA_SUMMARY_TEXT, mSummaryText);
            }
            if (mBigContentTitle != null) {
                extras.putCharSequence(EXTRA_TITLE_BIG, mBigContentTitle);
            }
            String className = getClassName();
            if (className != null) {
                extras.putString(EXTRA_COMPAT_TEMPLATE, className);
            }
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        protected void restoreFromCompatExtras(@NonNull Bundle extras) {
            if (extras.containsKey(EXTRA_SUMMARY_TEXT)) {
                mSummaryText = extras.getCharSequence(EXTRA_SUMMARY_TEXT);
                mSummaryTextSet = true;
            }
            mBigContentTitle = extras.getCharSequence(EXTRA_TITLE_BIG);
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        protected void clearCompatExtraKeys(@NonNull Bundle extras) {
            extras.remove(EXTRA_SUMMARY_TEXT);
            extras.remove(EXTRA_TITLE_BIG);
            extras.remove(EXTRA_COMPAT_TEMPLATE);
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public static @Nullable Style extractStyleFromNotification(
                @NonNull Notification notification) {
            Bundle extras = NotificationCompat.getExtras(notification);
            if (extras == null) {
                return null;
            }
            return constructStyleForExtras(extras);
        }

        private static @Nullable Style constructCompatStyleByPlatformName(
                @Nullable String platformTemplateClass) {
            if (platformTemplateClass == null) {
                return null;
            }
            if (platformTemplateClass.equals(Notification.BigPictureStyle.class.getName())) {
                return new BigPictureStyle();
            }
            if (platformTemplateClass.equals(Notification.BigTextStyle.class.getName())) {
                return new BigTextStyle();
            }
            if (platformTemplateClass.equals(Notification.InboxStyle.class.getName())) {
                return new InboxStyle();
            }
            if (Build.VERSION.SDK_INT >= 36) {
                if (platformTemplateClass.equals(Notification.ProgressStyle.class.getName())) {
                    return new ProgressStyle();
                }
            }

            if (Build.VERSION.SDK_INT >= 24) {
                if (platformTemplateClass.equals(Notification.MessagingStyle.class.getName())) {
                    return new MessagingStyle();
                }
                if (platformTemplateClass.equals(
                        Notification.DecoratedCustomViewStyle.class.getName())) {
                    return new DecoratedCustomViewStyle();
                }
            }
            return null;
        }

        static @Nullable Style constructCompatStyleByName(@Nullable String templateClass) {
            if (templateClass != null) {
                switch (templateClass) {
                    case BigTextStyle.TEMPLATE_CLASS_NAME:
                        return new BigTextStyle();
                    case BigPictureStyle.TEMPLATE_CLASS_NAME:
                        return new BigPictureStyle();
                    case InboxStyle.TEMPLATE_CLASS_NAME:
                        return new InboxStyle();
                    case DecoratedCustomViewStyle.TEMPLATE_CLASS_NAME:
                        return new DecoratedCustomViewStyle();
                    case MessagingStyle.TEMPLATE_CLASS_NAME:
                        return new MessagingStyle();
                    case CallStyle.TEMPLATE_CLASS_NAME:
                        return new CallStyle();
                    case ProgressStyle.TEMPLATE_CLASS_NAME:
                        return new ProgressStyle();
                }
            }
            return null;
        }

        static @Nullable Style constructCompatStyleForBundle(@NonNull Bundle extras) {
            // If the compat template name provided in the bundle can be resolved to a class, use
            // that style class.
            Style style = constructCompatStyleByName(extras.getString(EXTRA_COMPAT_TEMPLATE));
            if (style != null) {
                return style;
            }
            // Check for some specific extras which indicate the particular style that was used.
            // Start with MessagingStyle which this library (since before EXTRA_COMPAT_TEMPLATE)
            // creates as Notification.BigTextStyle, and thus both fields are contained.
            if (extras.containsKey(EXTRA_SELF_DISPLAY_NAME)
                    || extras.containsKey(EXTRA_MESSAGING_STYLE_USER)) {
                return new MessagingStyle();
            } else if (extras.containsKey(EXTRA_PICTURE)
                    || extras.containsKey(EXTRA_PICTURE_ICON)) {
                return new BigPictureStyle();
            } else if (extras.containsKey(EXTRA_BIG_TEXT)) {
                return new BigTextStyle();
            } else if (extras.containsKey(EXTRA_TEXT_LINES)) {
                return new InboxStyle();
            } else if (extras.containsKey(EXTRA_CALL_TYPE)) {
                return new CallStyle();
            } else if (extras.containsKey(EXTRA_PROGRESS_SEGMENTS)
                    || extras.containsKey(EXTRA_PROGRESS_POINTS)) {
                return new ProgressStyle();
            }
            // If individual extras do not help identify the style, use the framework style name.
            return constructCompatStyleByPlatformName(extras.getString(EXTRA_TEMPLATE));
        }

        static @Nullable Style constructStyleForExtras(@NonNull Bundle extras) {
            final Style style = constructCompatStyleForBundle(extras);
            if (style == null) {
                return null;
            }
            try {
                style.restoreFromCompatExtras(extras);
                return style;
            } catch (ClassCastException e) {
                return null;
            }
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public @NonNull RemoteViews applyStandardTemplate(boolean showSmallIcon,
                int resId, boolean fitIn1U) {
            Resources res = mBuilder.mContext.getResources();
            RemoteViews contentView = new RemoteViews(mBuilder.mContext.getPackageName(), resId);
            boolean showLine3 = false;
            boolean showLine2 = false;

            if (mBuilder.mLargeIcon != null) {
                // On versions before Jellybean, the large icon was shown by SystemUI, so we need
                // to hide it here.
                contentView.setViewVisibility(R.id.icon, View.VISIBLE);
                contentView.setImageViewBitmap(R.id.icon,
                            createColoredBitmap(mBuilder.mLargeIcon, Color.TRANSPARENT));
                if (showSmallIcon && mBuilder.mNotification.icon != 0) {
                    int backgroundSize = res.getDimensionPixelSize(
                            R.dimen.notification_right_icon_size);
                    int iconSize = backgroundSize - res.getDimensionPixelSize(
                            R.dimen.notification_small_icon_background_padding) * 2;
                    Bitmap smallBit = createIconWithBackground(
                            mBuilder.mNotification.icon,
                            backgroundSize,
                            iconSize,
                            mBuilder.getColor());
                    contentView.setImageViewBitmap(R.id.right_icon, smallBit);
                    contentView.setViewVisibility(R.id.right_icon, View.VISIBLE);
                }
            } else if (showSmallIcon && mBuilder.mNotification.icon != 0) { // small icon at left
                contentView.setViewVisibility(R.id.icon, View.VISIBLE);
                int backgroundSize = res.getDimensionPixelSize(
                        R.dimen.notification_large_icon_width)
                        - res.getDimensionPixelSize(R.dimen.notification_big_circle_margin);
                int iconSize = res.getDimensionPixelSize(
                        R.dimen.notification_small_icon_size_as_large);
                Bitmap smallBit = createIconWithBackground(
                        mBuilder.mNotification.icon,
                        backgroundSize,
                        iconSize,
                        mBuilder.getColor());
                contentView.setImageViewBitmap(R.id.icon, smallBit);
            }
            if (mBuilder.mContentTitle != null) {
                contentView.setTextViewText(R.id.title, mBuilder.mContentTitle);
            }
            if (mBuilder.mContentText != null) {
                contentView.setTextViewText(R.id.text, mBuilder.mContentText);
                showLine3 = true;
            }
            // If there is a large icon we have a right side
            boolean hasRightSide = false;
            if (mBuilder.mContentInfo != null) {
                contentView.setTextViewText(R.id.info, mBuilder.mContentInfo);
                contentView.setViewVisibility(R.id.info, View.VISIBLE);
                showLine3 = true;
                hasRightSide = true;
            } else if (mBuilder.mNumber > 0) {
                final int tooBig = res.getInteger(
                        R.integer.status_bar_notification_info_maxnum);
                if (mBuilder.mNumber > tooBig) {
                    contentView.setTextViewText(R.id.info, ((Resources) res).getString(
                            R.string.status_bar_notification_info_overflow));
                } else {
                    NumberFormat f = NumberFormat.getIntegerInstance();
                    contentView.setTextViewText(R.id.info, f.format(mBuilder.mNumber));
                }
                contentView.setViewVisibility(R.id.info, View.VISIBLE);
                showLine3 = true;
                hasRightSide = true;
            } else {
                contentView.setViewVisibility(R.id.info, View.GONE);
            }

            // Need to show three lines?
            if (mBuilder.mSubText != null) {
                contentView.setTextViewText(R.id.text, mBuilder.mSubText);
                if (mBuilder.mContentText != null) {
                    contentView.setTextViewText(R.id.text2, mBuilder.mContentText);
                    contentView.setViewVisibility(R.id.text2, View.VISIBLE);
                    showLine2 = true;
                } else {
                    contentView.setViewVisibility(R.id.text2, View.GONE);
                }
            }

            // RemoteViews.setViewPadding and RemoteViews.setTextViewTextSize is not available on
            // ICS-
            if (showLine2) {
                if (fitIn1U) {
                    // need to shrink all the type to make sure everything fits
                    final float subTextSize = res.getDimensionPixelSize(
                            R.dimen.notification_subtext_size);
                    contentView.setTextViewTextSize(R.id.text, TypedValue.COMPLEX_UNIT_PX,
                            subTextSize);
                }
                // vertical centering
                contentView.setViewPadding(R.id.line1, 0, 0, 0, 0);
            }

            if (mBuilder.getWhenIfShowing() != 0) {
                if (mBuilder.mUseChronometer) {
                    contentView.setViewVisibility(R.id.chronometer, View.VISIBLE);
                    contentView.setLong(R.id.chronometer, "setBase",
                            mBuilder.getWhenIfShowing()
                                    + (SystemClock.elapsedRealtime() - System.currentTimeMillis()));
                    contentView.setBoolean(R.id.chronometer, "setStarted", true);
                    if (mBuilder.mChronometerCountDown && Build.VERSION.SDK_INT >= 24) {
                        Api24Impl.setChronometerCountDown(contentView, R.id.chronometer,
                                mBuilder.mChronometerCountDown);
                    }
                } else {
                    contentView.setViewVisibility(R.id.time, View.VISIBLE);
                    contentView.setLong(R.id.time, "setTime", mBuilder.getWhenIfShowing());
                }
                hasRightSide = true;
            }
            contentView.setViewVisibility(R.id.right_side, hasRightSide ? View.VISIBLE : View.GONE);
            contentView.setViewVisibility(R.id.line3, showLine3 ? View.VISIBLE : View.GONE);
            return contentView;
        }

        /**
         * Create a bitmap using the given icon together with a color filter created from the given
         * color.
         *
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public Bitmap createColoredBitmap(int iconId, int color) {
            return createColoredBitmap(iconId, color, 0 /* size */);
        }

        /**
         * Create a bitmap using the given icon together with a color filter created from the given
         * color.
         */
        Bitmap createColoredBitmap(@NonNull IconCompat icon, int color) {
            return createColoredBitmap(icon, color, 0 /* size */);
        }

        private Bitmap createColoredBitmap(int iconId, int color, int size) {
            return createColoredBitmap(IconCompat.createWithResource(mBuilder.mContext, iconId),
                    color, size);
        }

        private Bitmap createColoredBitmap(@NonNull IconCompat icon, int color, int size) {
            Drawable drawable = icon.loadDrawable(mBuilder.mContext);
            int width = size == 0 ? drawable.getIntrinsicWidth() : size;
            int height = size == 0 ? drawable.getIntrinsicHeight() : size;
            Bitmap resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            drawable.setBounds(0, 0, width, height);
            if (color != 0) {
                drawable.mutate().setColorFilter(
                        new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
            }
            Canvas canvas = new Canvas(resultBitmap);
            drawable.draw(canvas);
            return resultBitmap;
        }

        private Bitmap createIconWithBackground(int iconId, int size,
                int iconSize, int color) {
            Bitmap coloredBitmap = createColoredBitmap(R.drawable.notification_icon_background,
                    color == NotificationCompat.COLOR_DEFAULT ? 0 : color, size);
            Canvas canvas = new Canvas(coloredBitmap);
            Drawable icon = mBuilder.mContext.getResources().getDrawable(iconId).mutate();
            icon.setFilterBitmap(true);
            int inset = (size - iconSize) / 2;
            icon.setBounds(inset, inset, iconSize + inset, iconSize + inset);
            icon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP));
            icon.draw(canvas);
            return coloredBitmap;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public void buildIntoRemoteViews(RemoteViews outerView,
                RemoteViews innerView) {
            // this needs to be done fore the other calls, since otherwise we might hide the wrong
            // things if our ids collide.
            hideNormalContent(outerView);
            outerView.removeAllViews(R.id.notification_main_column);
            outerView.addView(R.id.notification_main_column, innerView.clone());
            outerView.setViewVisibility(R.id.notification_main_column, View.VISIBLE);
            // Adjust padding depending on font size.
            int top = calculateTopPadding();
            outerView.setViewPadding(R.id.notification_main_column_container, 0, top, 0, 0);
        }

        private void hideNormalContent(RemoteViews outerView) {
            outerView.setViewVisibility(R.id.title, View.GONE);
            outerView.setViewVisibility(R.id.text2, View.GONE);
            outerView.setViewVisibility(R.id.text, View.GONE);
        }

        private int calculateTopPadding() {
            Resources resources = mBuilder.mContext.getResources();
            int padding = resources.getDimensionPixelSize(R.dimen.notification_top_pad);
            int largePadding = resources.getDimensionPixelSize(
                    R.dimen.notification_top_pad_large_text);
            float fontScale = resources.getConfiguration().fontScale;
            float largeFactor = (constrain(fontScale, 1.0f, 1.3f) - 1f) / (1.3f - 1f);

            // Linearly interpolate the padding between large and normal with the font scale ranging
            // from 1f to LARGE_TEXT_SCALE
            return Math.round((1 - largeFactor) * padding + largeFactor * largePadding);
        }

        private static float constrain(float amount, float low, float high) {
            return amount < low ? low : (amount > high ? high : amount);
        }

        /**
         * A class for wrapping calls to {@link NotificationCompat.Style} methods which
         * were added in API 24; these calls must be wrapped to avoid performance issues.
         * See the UnsafeNewApiCall lint rule for more details.
         */
        @RequiresApi(24)
        static class Api24Impl {
            private Api24Impl() { }

            static void setChronometerCountDown(RemoteViews remoteViews, int viewId,
                    boolean isCountDown) {
                remoteViews.setChronometerCountDown(viewId, isCountDown);
            }

        }
    }

    /**
     * Helper class for generating large-format notifications that include a large image attachment.
     * <br>
     * If the platform does not provide large-format notifications, this method has no effect. The
     * user will always see the normal notification view.
     * <br>
     * This class is a "rebuilder": It attaches to a Builder object and modifies its behavior, like so:
     * <pre class="prettyprint">
     * Notification notification = new NotificationCompat.Builder(mContext)
     *     .setContentTitle(&quot;New photo from &quot; + sender.toString())
     *     .setContentText(subject)
     *     .setSmallIcon(R.drawable.new_post)
     *     .setLargeIcon(aBitmap)
     *     .setStyle(new Notification.BigPictureStyle()
     *         .bigPicture(aBigBitmap))
     *     .build();
     * </pre>
     *
     * @see Notification#bigContentView
     */
    public static class BigPictureStyle extends Style {

        private static final String TEMPLATE_CLASS_NAME =
                "androidx.core.app.NotificationCompat$BigPictureStyle";

        private IconCompat mPictureIcon;
        private IconCompat mBigLargeIcon;
        private boolean mBigLargeIconSet;
        private CharSequence mPictureContentDescription;
        private boolean mShowBigPictureWhenCollapsed;

        public BigPictureStyle() {
        }

        public BigPictureStyle(@Nullable Builder builder) {
            setBuilder(builder);
        }

        /**
         * Overrides ContentTitle in the big form of the template.
         * This defaults to the value passed to setContentTitle().
         */
        public @NonNull BigPictureStyle setBigContentTitle(@Nullable CharSequence title) {
            mBigContentTitle = Builder.limitCharSequenceLength(title);
            return this;
        }

        /**
         * Set the first line of text after the detail section in the big form of the template.
         */
        public @NonNull BigPictureStyle setSummaryText(@Nullable CharSequence cs) {
            mSummaryText = Builder.limitCharSequenceLength(cs);
            mSummaryTextSet = true;
            return this;
        }

        /**
         * Set the content description of the big picture.
         */
        @RequiresApi(31)
        public @NonNull BigPictureStyle setContentDescription(
                @Nullable CharSequence contentDescription) {
            mPictureContentDescription = contentDescription;
            return this;
        }

        /**
         * Provide the bitmap to be used as the payload for the BigPicture notification.
         */
        public @NonNull BigPictureStyle bigPicture(@Nullable Bitmap b) {
            mPictureIcon = b == null ? null : IconCompat.createWithBitmap(b);
            return this;
        }

        /**
         * Provide an icon to be used as the payload for the BigPicture notification.
         * Note that certain features (like animated Icons) may not work on all versions.
         */
        @RequiresApi(31)
        public @NonNull BigPictureStyle bigPicture(@Nullable Icon i) {
            mPictureIcon = IconCompat.createFromIcon(i);
            return this;
        }

        /**
         * When set, the {@link #bigPicture(Bitmap) big picture} of this style will be promoted and
         * shown in place of the {@link Builder#setLargeIcon(Bitmap) large icon} in the collapsed
         * state of this notification.
         */
        @RequiresApi(31)
        public @NonNull BigPictureStyle showBigPictureWhenCollapsed(boolean show) {
            mShowBigPictureWhenCollapsed = show;
            return this;
        }

        /**
         * Override the large icon when the big notification is shown.
         */
        public @NonNull BigPictureStyle bigLargeIcon(@Nullable Bitmap b) {
            mBigLargeIcon = b == null ? null : IconCompat.createWithBitmap(b);
            mBigLargeIconSet = true;
            return this;
        }

        /**
         * Override the large icon when the big notification is shown.
         */
        public @NonNull BigPictureStyle bigLargeIcon(@Nullable Icon i) {
            mBigLargeIcon = i == null ? null : IconCompat.createFromIcon(i);
            mBigLargeIconSet = true;
            return this;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        protected @NonNull String getClassName() {
            return TEMPLATE_CLASS_NAME;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        public void apply(NotificationBuilderWithBuilderAccessor builder) {
            Notification.Builder builder1 = builder.getBuilder();
            Notification.BigPictureStyle bigPictureStyle =
                    new Notification.BigPictureStyle(builder1);
            Notification.BigPictureStyle style =
                    bigPictureStyle.setBigContentTitle(mBigContentTitle);
            if (mPictureIcon != null) {
                // Attempts to set the icon for BigPictureStyle; prefers using data as Icon,
                // with a fallback to store the Bitmap if Icon is not supported directly.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Context context = null;
                    if (builder instanceof NotificationCompatBuilder) {
                        context = ((NotificationCompatBuilder) builder).getContext();
                    }
                    Api31Impl.setBigPicture(style, mPictureIcon.toIcon(context));
                } else if (mPictureIcon.getType() == IconCompat.TYPE_BITMAP) {
                    Bitmap b = mPictureIcon.getBitmap();
                    style = style.bigPicture(b);
                }
            }
            // Attempts to set the big large icon for BigPictureStyle.
            if (mBigLargeIconSet) {
                if (mBigLargeIcon == null) {
                    style.bigLargeIcon((Bitmap) null);
                } else {
                    Context context = null;
                    if (builder instanceof NotificationCompatBuilder) {
                        context = ((NotificationCompatBuilder) builder).getContext();
                    }
                    style.bigLargeIcon(mBigLargeIcon.toIcon(context));
                }
            }
            if (mSummaryTextSet) {
                style.setSummaryText(mSummaryText);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Api31Impl.showBigPictureWhenCollapsed(style, mShowBigPictureWhenCollapsed);
                Api31Impl.setContentDescription(style, mPictureContentDescription);
            }
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        @SuppressWarnings("deprecation")
        protected void restoreFromCompatExtras(@NonNull Bundle extras) {
            super.restoreFromCompatExtras(extras);

            if (extras.containsKey(EXTRA_LARGE_ICON_BIG)) {
                mBigLargeIcon = asIconCompat(extras.getParcelable(EXTRA_LARGE_ICON_BIG));
                mBigLargeIconSet = true;
            }
            mPictureIcon = getPictureIcon(extras);
            mShowBigPictureWhenCollapsed = extras.getBoolean(EXTRA_SHOW_BIG_PICTURE_WHEN_COLLAPSED);
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public static @Nullable IconCompat getPictureIcon(@Nullable Bundle extras) {
            if (extras == null) return null;
            // When this style adds a picture, we only add one of the keys.  If both were added,
            // it would most likely be a legacy app trying to override the picture in some way.
            // Because of that case it's better to give precedence to the legacy field.
            Parcelable bitmapPicture = extras.getParcelable(EXTRA_PICTURE);
            if (bitmapPicture != null) {
                return asIconCompat(bitmapPicture);
            } else {
                return asIconCompat(extras.getParcelable(EXTRA_PICTURE_ICON));
            }
        }

        private static @Nullable IconCompat asIconCompat(@Nullable Parcelable bitmapOrIcon) {
            if (bitmapOrIcon != null) {
                if (bitmapOrIcon instanceof Icon) {
                    return IconCompat.createFromIcon((Icon) bitmapOrIcon);
                }
                if (bitmapOrIcon instanceof Bitmap) {
                    return IconCompat.createWithBitmap((Bitmap) bitmapOrIcon);
                }
            }
            return null;
        }

        /**
         */
        @Override
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        protected void clearCompatExtraKeys(@NonNull Bundle extras) {
            super.clearCompatExtraKeys(extras);
            extras.remove(EXTRA_LARGE_ICON_BIG);
            extras.remove(EXTRA_PICTURE);
            extras.remove(EXTRA_PICTURE_ICON);
            extras.remove(EXTRA_SHOW_BIG_PICTURE_WHEN_COLLAPSED);
        }

        /**
         * A class for wrapping calls to {@link Notification.BigPictureStyle} methods which
         * were added in API 31; these calls must be wrapped to avoid performance issues.
         * See the UnsafeNewApiCall lint rule for more details.
         */
        @RequiresApi(31)
        private static class Api31Impl {
            private Api31Impl() {
            }

            /**
             * Calls {@link Notification.BigPictureStyle#showBigPictureWhenCollapsed(boolean)}
             */
            @RequiresApi(31)
            static void showBigPictureWhenCollapsed(Notification.BigPictureStyle style,
                    boolean show) {
                style.showBigPictureWhenCollapsed(show);
            }

            /**
             * Calls {@link Notification.BigPictureStyle#setContentDescription(CharSequence)}
             */
            @RequiresApi(31)
            static void setContentDescription(Notification.BigPictureStyle style,
                    CharSequence contentDescription) {
                style.setContentDescription(contentDescription);
            }

            /**
             * Calls {@link Notification.BigPictureStyle#bigPicture(Icon)}
             */
            @RequiresApi(31)
            static void setBigPicture(Notification.BigPictureStyle style, Icon icon) {
                style.bigPicture(icon);
            }
        }
    }

    /**
     * Helper class for generating large-format notifications that include a lot of text.
     *
     * <br>
     * If the platform does not provide large-format notifications, this method has no effect. The
     * user will always see the normal notification view.
     * <br>
     * This class is a "rebuilder": It attaches to a Builder object and modifies its behavior, like so:
     * <pre class="prettyprint">
     * Notification notification = new Notification.Builder(mContext)
     *     .setContentTitle(&quot;New mail from &quot; + sender.toString())
     *     .setContentText(subject)
     *     .setSmallIcon(R.drawable.new_mail)
     *     .setLargeIcon(aBitmap)
     *     .setStyle(new Notification.BigTextStyle()
     *         .bigText(aVeryLongString))
     *     .build();
     * </pre>
     *
     * @see Notification#bigContentView
     */
    public static class BigTextStyle extends Style {

        private static final String TEMPLATE_CLASS_NAME =
                "androidx.core.app.NotificationCompat$BigTextStyle";

        private CharSequence mBigText;

        public BigTextStyle() {
        }

        public BigTextStyle(@Nullable Builder builder) {
            setBuilder(builder);
        }

        /**
         * Overrides ContentTitle in the big form of the template.
         * This defaults to the value passed to setContentTitle().
         */
        public @NonNull BigTextStyle setBigContentTitle(@Nullable CharSequence title) {
            mBigContentTitle = Builder.limitCharSequenceLength(title);
            return this;
        }

        /**
         * Set the first line of text after the detail section in the big form of the template.
         */
        public @NonNull BigTextStyle setSummaryText(@Nullable CharSequence cs) {
            mSummaryText = Builder.limitCharSequenceLength(cs);
            mSummaryTextSet = true;
            return this;
        }

        /**
         * Provide the longer text to be displayed in the big form of the
         * template in place of the content text.
         */
        public @NonNull BigTextStyle bigText(@Nullable CharSequence cs) {
            mBigText = Builder.limitCharSequenceLength(cs);
            return this;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        protected @NonNull String getClassName() {
            return TEMPLATE_CLASS_NAME;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        public void apply(NotificationBuilderWithBuilderAccessor builder) {
            Notification.Builder builder1 = builder.getBuilder();
            Notification.BigTextStyle style =
                    new Notification.BigTextStyle(builder1);
            style = style.setBigContentTitle(mBigContentTitle);
            style = style.bigText(mBigText);
            if (mSummaryTextSet) {
                style.setSummaryText(mSummaryText);
            }
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        protected void restoreFromCompatExtras(@NonNull Bundle extras) {
            super.restoreFromCompatExtras(extras);

            mBigText = extras.getCharSequence(EXTRA_BIG_TEXT);
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        public void addCompatExtras(@NonNull Bundle extras) {
            super.addCompatExtras(extras);
        }

        /**
         */
        @Override
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        protected void clearCompatExtraKeys(@NonNull Bundle extras) {
            super.clearCompatExtraKeys(extras);
            extras.remove(EXTRA_BIG_TEXT);
        }
    }

    /**
     * Helper class for generating large-format notifications that include multiple back-and-forth
     * messages of varying types between any number of people.
     *
     * <br>
     * In order to get a backwards compatible behavior, the app needs to use the v7 version of the
     * notification builder together with this style, otherwise the user will see the normal
     * notification view.
     *
     * <br>
     * Use {@link MessagingStyle#setConversationTitle(CharSequence)} to set a conversation title for
     * group chats with more than two people. This could be the user-created name of the group or,
     * if it doesn't have a specific name, a list of the participants in the conversation. Do not
     * set a conversation title for one-on-one chats, since platforms use the existence of this
     * field as a hint that the conversation is a group.
     *
     * <br>
     * This class is a "rebuilder": It attaches to a Builder object and modifies its behavior, like
     * so:
     * <pre class="prettyprint">
     *
     * Notification notification = new Notification.Builder()
     *     .setContentTitle(&quot;2 new messages with &quot; + sender.toString())
     *     .setContentText(subject)
     *     .setSmallIcon(R.drawable.new_message)
     *     .setLargeIcon(aBitmap)
     *     .setStyle(new Notification.MessagingStyle(resources.getString(R.string.reply_name))
     *         .addMessage(messages[0].getText(), messages[0].getTime(), messages[0].getSender())
     *         .addMessage(messages[1].getText(), messages[1].getTime(), messages[1].getSender()))
     *     .build();
     * </pre>
     */
    public static class MessagingStyle extends Style {

        private static final String TEMPLATE_CLASS_NAME =
                "androidx.core.app.NotificationCompat$MessagingStyle";

        /**
         * The maximum number of messages that will be retained in the Notification itself (the
         * number displayed is up to the platform).
         */
        public static final int MAXIMUM_RETAINED_MESSAGES = 25;

        private final List<Message> mMessages = new ArrayList<>();
        private final List<Message> mHistoricMessages = new ArrayList<>();
        private Person mUser;
        private @Nullable CharSequence mConversationTitle;
        private @Nullable Boolean mIsGroupConversation;

        /** Package private empty constructor for {@link Style#restoreFromCompatExtras(Bundle)}. */
        MessagingStyle() {}

        /**
         * @param userDisplayName Required - the name to be displayed for any replies sent by the
         * user before the posting app reposts the notification with those messages after they've
         * been actually sent and in previous messages sent by the user added in
         * {@link #addMessage(Message)}
         * @deprecated Use {@code #MessagingStyle(Person)} instead.
         */
        @Deprecated
        public MessagingStyle(@NonNull CharSequence userDisplayName) {
            mUser = new Person.Builder().setName(userDisplayName).build();
        }

        /**
         * Creates a new {@link MessagingStyle} object. Note that {@link Person} must have a
         * non-empty name.
         *
         * @param user This {@link Person}'s name will be shown when this app's notification is
         * being replied to. It's used temporarily so the app has time to process the send request
         * and repost the notification with updates to the conversation.
         */
        public MessagingStyle(@NonNull Person user) {
            if (TextUtils.isEmpty(user.getName())) {
                throw new IllegalArgumentException("User's name must not be empty.");
            }
            mUser = user;
        }

        /**
         * Returns the name to be displayed for any replies sent by the user.
         *
         * @deprecated Use {@link #getUser()} instead.
         */
        @Deprecated
        public @Nullable CharSequence getUserDisplayName() {
            return mUser.getName();
        }

        /** Returns the person to be used for any replies sent by the user. */
        public @NonNull Person getUser() {
            return mUser;
        }

        /**
         * Sets the title to be displayed on this conversation. May be set to {@code null}.
         *
         * <p>This API's behavior was changed in SDK version {@link Build.VERSION_CODES#P}. If your
         * application's target version is less than {@link Build.VERSION_CODES#P}, setting a
         * conversation title to a non-null value will make {@link #isGroupConversation()} return
         * {@code true} and passing {@code null} will make it return {@code false}. This behavior
         * can be overridden by calling {@link #setGroupConversation(boolean)} regardless of SDK
         * version. In {@link Build.VERSION_CODES#P} and above, this method does not affect group
         * conversation settings.
         *
         * @param conversationTitle Title displayed for this conversation
         * @return this object for method chaining
         */
        public @NonNull MessagingStyle setConversationTitle(
                @Nullable CharSequence conversationTitle) {
            mConversationTitle = conversationTitle;
            return this;
        }

        /**
         * Return the title to be displayed on this conversation. Can be {@code null}.
         */
        public @Nullable CharSequence getConversationTitle() {
            return mConversationTitle;
        }

        /**
         * Adds a message for display by this notification. Convenience call for a simple
         * {@link Message} in {@link #addMessage(Message)}
         * @param text A {@link CharSequence} to be displayed as the message content
         * @param timestamp Time at which the message arrived in ms since Unix epoch
         * @param sender A {@link CharSequence} to be used for displaying the name of the
         * sender. Should be <code>null</code> for messages by the current user, in which case
         * the platform will insert {@link #getUserDisplayName()}.
         * Should be unique amongst all individuals in the conversation, and should be
         * consistent during re-posts of the notification.
         *
         * @see Message#Message(CharSequence, long, CharSequence)
         *
         * @return this object for method chaining
         *
         * @deprecated Use {@link #addMessage(CharSequence, long, Person)} or
         * {@link #addMessage(Message)}
         */
        @Deprecated
        public @NonNull MessagingStyle addMessage(@Nullable CharSequence text, long timestamp,
                @Nullable CharSequence sender) {
            mMessages.add(
                    new Message(text, timestamp, new Person.Builder().setName(sender).build()));
            if (mMessages.size() > MAXIMUM_RETAINED_MESSAGES) {
                mMessages.remove(0);
            }
            return this;
        }

        /**
         * Adds a message for display by this notification. Convenience call for
         * {@link #addMessage(Message)}.
         *
         * @see Message#Message(CharSequence, long, Person)
         *
         * @return this for method chaining
         */
        public @NonNull MessagingStyle addMessage(@Nullable CharSequence text, long timestamp,
                @Nullable Person person) {
            addMessage(new Message(text, timestamp, person));
            return this;
        }

        /**
         * Adds a {@link Message} for display in this notification.
         *
         * @param message The {@link Message} to be displayed
         *
         * @return this object for method chaining
         */
        public @NonNull MessagingStyle addMessage(@Nullable Message message) {
            if (message != null) {
                mMessages.add(message);
                if (mMessages.size() > MAXIMUM_RETAINED_MESSAGES) {
                    mMessages.remove(0);
                }
            }
            return this;
        }

        /**
         * Adds a {@link Message} for historic context in this notification.
         *
         * <p>Messages should be added as historic if they are not the main subject of the
         * notification but may give context to a conversation. The system may choose to present
         * them only when relevant, e.g. when replying to a message through a {@link RemoteInput}.
         *
         * <p>The messages should be added in chronologic order, i.e. the oldest first,
         * the newest last.
         *
         * @param message The historic {@link Message} to be added
         * @return this object for method chaining
         */
        public @NonNull MessagingStyle addHistoricMessage(@Nullable Message message) {
            if (message != null) {
                mHistoricMessages.add(message);
                if (mHistoricMessages.size() > MAXIMUM_RETAINED_MESSAGES) {
                    mHistoricMessages.remove(0);
                }
            }
            return this;
        }

        /**
         * Gets the list of {@code Message} objects that represent the notification.
         */
        public @NonNull List<Message> getMessages() {
            return mMessages;
        }

        /**
         * Gets the list of historic {@code Message}s in the notification.
         */
        public @NonNull List<Message> getHistoricMessages() {
            return mHistoricMessages;
        }

        /**
         * Sets whether this conversation notification represents a group. An app should set
         * isGroupConversation {@code true} to mark that the conversation involves multiple people.
         *
         * <p>Group conversation notifications may display additional group-related context not
         * present in non-group notifications.
         *
         * @param isGroupConversation {@code true} if the conversation represents a group,
         * {@code false} otherwise.
         * @return this object for method chaining
         *
         * @see #isGroupConversation()
         */
        public @NonNull MessagingStyle setGroupConversation(boolean isGroupConversation) {
            mIsGroupConversation = isGroupConversation;
            return this;
        }

        /**
         * Returns {@code true} if this notification represents a group conversation, otherwise
         * {@code false}.
         *
         * <p> If the application that generated this {@link MessagingStyle} targets an SDK version
         * less than {@link Build.VERSION_CODES#P} and {@link #setGroupConversation(boolean)}
         * was not called, this method becomes dependent on whether or not the conversation title is
         * set; returning {@code true} if the conversation title is a non-null value, or
         * {@code false} otherwise. This is to maintain backwards compatibility. Regardless, {@link
         * #setGroupConversation(boolean)} has precedence over this legacy behavior. From
         * {@link Build.VERSION_CODES#P} forward, {@link #setConversationTitle(CharSequence)} has
         * no affect on group conversation status.
         *
         * @see #setConversationTitle(CharSequence)
         */
        public boolean isGroupConversation() {
            // When target SDK version is < P and the app didn't explicitly set isGroupConversation,
            // a non-null conversation title dictates if this is a group conversation.
            if (mBuilder != null
                    && mBuilder.mContext.getApplicationInfo().targetSdkVersion
                    < Build.VERSION_CODES.P
                    && mIsGroupConversation == null) {
                return mConversationTitle != null;
            }

            // Default to false if not set.
            return (mIsGroupConversation != null) ? mIsGroupConversation : false;
        }

        /**
         * Retrieves a {@link MessagingStyle} from a {@link Notification}, enabling an application
         * that has set a {@link MessagingStyle} using {@link NotificationCompat} or
         * {@link Notification.Builder} to send messaging information to another
         * application using {@link NotificationCompat}, regardless of the API level of the system.
         *
         * @return {@code null} if there is no {@link MessagingStyle} set, or if the SDK version is
         * &lt; {@code 16} (JellyBean).
         */
        public static @Nullable MessagingStyle extractMessagingStyleFromNotification(
                @NonNull Notification notification) {
            Style style = Style.extractStyleFromNotification(notification);
            if (style instanceof MessagingStyle) {
                return (MessagingStyle) style;
            }
            return null;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        protected @NonNull String getClassName() {
            return TEMPLATE_CLASS_NAME;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        public void apply(NotificationBuilderWithBuilderAccessor builder) {
            // This is called because we need to apply legacy logic before writing MessagingInfo
            // data into the bundle. This does nothing in >= P, but in < P this will apply the
            // correct group conversation status to new fields which will then be decoded properly
            // by #extractMessagingStyleFromNotification.
            setGroupConversation(isGroupConversation());

            if (Build.VERSION.SDK_INT >= 24) {
                Object frameworkStyle;
                if (Build.VERSION.SDK_INT >= 28) {
                    frameworkStyle = Api28Impl.createMessagingStyle(mUser.toAndroidPerson());
                } else {
                    frameworkStyle =
                            Api24Impl.createMessagingStyle(
                                    mUser.getName());
                }

                for (Message message : mMessages) {
                    Api24Impl.addMessage((Notification.MessagingStyle) frameworkStyle,
                            message.toAndroidMessage());
                }

                if (Build.VERSION.SDK_INT >= 26) {
                    for (Message historicMessage : mHistoricMessages) {
                        Api26Impl.addHistoricMessage((Notification.MessagingStyle) frameworkStyle,
                                historicMessage.toAndroidMessage());
                    }
                }

                // In SDK < 28, base Android will assume a MessagingStyle notification is a group
                // chat if the conversation title is set. In compat, this isn't the case as we've
                // introduced #setGroupConversation. When we apply these settings to base Android
                // notifications, we should only set base Android's MessagingStyle conversation
                // title if it's a group conversation OR SDK >= 28. Otherwise we set the
                // Notification content title so Android won't think it's a group conversation.
                if (mIsGroupConversation || Build.VERSION.SDK_INT >= 28) {
                    // If group or non-legacy, set MessagingStyle#mConversationTitle.
                    Api24Impl.setConversationTitle((Notification.MessagingStyle) frameworkStyle,
                            mConversationTitle);
                }

                // For SDK >= 28, we can simply denote the group conversation status regardless of
                // if we set the conversation title or not.
                if (Build.VERSION.SDK_INT >= 28) {
                    Api28Impl.setGroupConversation((Notification.MessagingStyle) frameworkStyle,
                            mIsGroupConversation);
                }
                Notification.Builder builder1 = builder.getBuilder();
                ((Notification.Style) frameworkStyle).setBuilder(builder1);
            } else {
                Message latestIncomingMessage = findLatestIncomingMessage();
                // Set the title
                if (mConversationTitle != null && mIsGroupConversation) {
                    builder.getBuilder().setContentTitle(mConversationTitle);
                } else if (latestIncomingMessage != null) {
                    builder.getBuilder().setContentTitle("");
                    if (latestIncomingMessage.getPerson() != null) {
                        builder.getBuilder().setContentTitle(
                                latestIncomingMessage.getPerson().getName());
                    }
                }
                // Set the text
                if (latestIncomingMessage != null) {
                    builder.getBuilder().setContentText(mConversationTitle != null
                            ? makeMessageLine(latestIncomingMessage)
                            : latestIncomingMessage.getText());
                }
                // Build a fallback BigTextStyle for API 16-23 devices
                SpannableStringBuilder completeMessage = new SpannableStringBuilder();
                boolean showNames = mConversationTitle != null
                        || hasMessagesWithoutSender();
                for (int i = mMessages.size() - 1; i >= 0; i--) {
                    Message message = mMessages.get(i);
                    CharSequence line;
                    line = showNames ? makeMessageLine(message) : message.getText();
                    if (i != mMessages.size() - 1) {
                        completeMessage.insert(0, "\n");
                    }
                    completeMessage.insert(0, line);
                }
                Notification.Builder builder1 = builder.getBuilder();
                Notification.BigTextStyle style =
                        new Notification.BigTextStyle(builder1);
                style = style.setBigContentTitle(null);
                style.bigText(completeMessage);
            }
        }

        private @Nullable Message findLatestIncomingMessage() {
            for (int i = mMessages.size() - 1; i >= 0; i--) {
                Message message = mMessages.get(i);
                // Incoming messages have a non-empty sender.
                if (message.getPerson() != null
                        && !TextUtils.isEmpty(message.getPerson().getName())) {
                    return message;
                }
            }
            if (!mMessages.isEmpty()) {
                // No incoming messages, fall back to outgoing message
                return mMessages.get(mMessages.size() - 1);
            }
            return null;
        }

        private boolean hasMessagesWithoutSender() {
            for (int i = mMessages.size() - 1; i >= 0; i--) {
                Message message = mMessages.get(i);
                if (message.getPerson() != null && message.getPerson().getName() == null) {
                    return true;
                }
            }
            return false;
        }

        private CharSequence makeMessageLine(@NonNull Message message) {
            BidiFormatter bidi = BidiFormatter.getInstance();
            SpannableStringBuilder sb = new SpannableStringBuilder();
            int color = Color.BLACK;
            CharSequence replyName =
                    message.getPerson() == null ? "" : message.getPerson().getName();
            if (TextUtils.isEmpty(replyName)) {
                replyName = mUser.getName();
                color = mBuilder.getColor() != NotificationCompat.COLOR_DEFAULT
                        ? mBuilder.getColor()
                        : color;
            }
            CharSequence senderText = bidi.unicodeWrap(replyName);
            sb.append(senderText);
            sb.setSpan(makeFontColorSpan(color),
                    sb.length() - senderText.length(),
                    sb.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE /* flags */);
            CharSequence text = message.getText() == null ? "" : message.getText();
            sb.append("  ").append(bidi.unicodeWrap(text));
            return sb;
        }

        private @NonNull TextAppearanceSpan makeFontColorSpan(int color) {
            return new TextAppearanceSpan(null, 0, 0, ColorStateList.valueOf(color), null);
        }

        @Override
        public void addCompatExtras(@NonNull Bundle extras) {
            super.addCompatExtras(extras);
            extras.putCharSequence(EXTRA_SELF_DISPLAY_NAME, mUser.getName());
            extras.putBundle(EXTRA_MESSAGING_STYLE_USER, mUser.toBundle());

            extras.putCharSequence(EXTRA_HIDDEN_CONVERSATION_TITLE, mConversationTitle);
            if (mConversationTitle != null && mIsGroupConversation) {
                extras.putCharSequence(EXTRA_CONVERSATION_TITLE, mConversationTitle);
            }
            if (!mMessages.isEmpty()) {
                extras.putParcelableArray(EXTRA_MESSAGES,
                        Message.getBundleArrayForMessages(mMessages));
            }
            if (!mHistoricMessages.isEmpty()) {
                extras.putParcelableArray(EXTRA_HISTORIC_MESSAGES,
                        Message.getBundleArrayForMessages(mHistoricMessages));
            }
            if (mIsGroupConversation != null) {
                extras.putBoolean(EXTRA_IS_GROUP_CONVERSATION, mIsGroupConversation);
            }
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        @SuppressWarnings("deprecation")
        protected void restoreFromCompatExtras(@NonNull Bundle extras) {
            super.restoreFromCompatExtras(extras);
            mMessages.clear();
            // Call to #restore requires that there either be a display name OR a user.
            if (extras.containsKey(EXTRA_MESSAGING_STYLE_USER)) {
                // New path simply unpacks Person, but checks if there's a valid name.
                mUser = Person.fromBundle(extras.getBundle(EXTRA_MESSAGING_STYLE_USER));
            } else {
                // Legacy extra simply builds Person with a name.
                mUser = new Person.Builder()
                        .setName(extras.getString(EXTRA_SELF_DISPLAY_NAME))
                        .build();
            }

            mConversationTitle = extras.getCharSequence(EXTRA_CONVERSATION_TITLE);
            if (mConversationTitle == null) {
                mConversationTitle = extras.getCharSequence(EXTRA_HIDDEN_CONVERSATION_TITLE);
            }
            Parcelable[] messages = extras.getParcelableArray(EXTRA_MESSAGES);
            if (messages != null) {
                mMessages.addAll(Message.getMessagesFromBundleArray(messages));
            }
            Parcelable[] historicMessages = extras.getParcelableArray(EXTRA_HISTORIC_MESSAGES);
            if (historicMessages != null) {
                mHistoricMessages.addAll(Message.getMessagesFromBundleArray(historicMessages));
            }
            if (extras.containsKey(EXTRA_IS_GROUP_CONVERSATION)) {
                mIsGroupConversation = extras.getBoolean(EXTRA_IS_GROUP_CONVERSATION);
            }
        }

        /**
         */
        @Override
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        protected void clearCompatExtraKeys(@NonNull Bundle extras) {
            super.clearCompatExtraKeys(extras);
            extras.remove(EXTRA_MESSAGING_STYLE_USER);
            extras.remove(EXTRA_SELF_DISPLAY_NAME);
            extras.remove(EXTRA_CONVERSATION_TITLE);
            extras.remove(EXTRA_HIDDEN_CONVERSATION_TITLE);
            extras.remove(EXTRA_MESSAGES);
            extras.remove(EXTRA_HISTORIC_MESSAGES);
            extras.remove(EXTRA_IS_GROUP_CONVERSATION);
        }

        public static final class Message {
            static final String KEY_TEXT = "text";
            static final String KEY_TIMESTAMP = "time";
            static final String KEY_SENDER = "sender";
            static final String KEY_DATA_MIME_TYPE = "type";
            static final String KEY_DATA_URI= "uri";
            static final String KEY_EXTRAS_BUNDLE = "extras";
            static final String KEY_PERSON = "person";
            static final String KEY_NOTIFICATION_PERSON = "sender_person";

            private final CharSequence mText;
            private final long mTimestamp;
            private final @Nullable Person mPerson;

            private Bundle mExtras = new Bundle();
            private @Nullable String mDataMimeType;
            private @Nullable Uri mDataUri;

            /**
             * Creates a new {@link Message} with the given text, timestamp, and sender.
             *
             * @param text A {@link CharSequence} to be displayed as the message content
             * @param timestamp Time at which the message arrived in ms since Unix epoch
             * @param person A {@link Person} whose {@link Person#getName()} value is used as the
             * display name for the sender. This should be {@code null} for messages by the current
             * user, in which case, the platform will insert
             * {@link MessagingStyle#getUserDisplayName()}. A {@link Person}'s key should be
             * consistent during re-posts of the notification.
             */
            public Message(@Nullable CharSequence text, long timestamp, @Nullable Person person) {
                mText = text;
                mTimestamp = timestamp;
                mPerson = person;
            }

            /**
             * Constructor
             *
             * @param text A {@link CharSequence} to be displayed as the message content
             * @param timestamp Time at which the message arrived in ms since Unix epoch
             * @param sender A {@link CharSequence} to be used for displaying the name of the
             * sender. Should be <code>null</code> for messages by the current user, in which case
             * the platform will insert {@link MessagingStyle#getUserDisplayName()}.
             * Should be unique amongst all individuals in the conversation, and should be
             * consistent during re-posts of the notification.
             *
             * @deprecated Use the alternative constructor instead.
             */
            @Deprecated
            public Message(@Nullable CharSequence text, long timestamp,
                    @Nullable CharSequence sender) {
                this(text, timestamp, new Person.Builder().setName(sender).build());
            }

            /**
             * Sets a binary blob of data and an associated MIME type for a message. In the case
             * where the platform doesn't support the MIME type, the original text provided in the
             * constructor will be used.
             *
             * @param dataMimeType The MIME type of the content. See
             * {@link android.graphics.ImageDecoder#isMimeTypeSupported(String)}
             * for a list of supported image MIME types.
             * @param dataUri The uri containing the content whose type is given by the MIME type.
             * <p class="note">
             * Notification Listeners including the System UI need permission to access the
             * data the Uri points to. The recommended ways to do this are:
             * <ol>
             *   <li>Store the data in your own ContentProvider, making sure that other apps have
             *       the correct permission to access your provider. The preferred mechanism for
             *       providing access is to use per-URI permissions which are temporary and only
             *       grant access to the receiving application. An easy way to create a
             *       ContentProvider like this is to use the FileProvider helper class.</li>
             *   <li>Use the system MediaStore. The MediaStore is primarily aimed at video, audio
             *       and image MIME types, however beginning with Android 3.0 (API level 11) it can
             *       also store non-media types (see MediaStore.Files for more info). Files can be
             *       inserted into the MediaStore using scanFile() after which a content:// style
             *       Uri suitable for sharing is passed to the provided onScanCompleted() callback.
             *       Note that once added to the system MediaStore the content is accessible to any
             *       app on the device.</li>
             * </ol>
             *
             * @return this object for method chaining
             */
            public @NonNull Message setData(@Nullable String dataMimeType, @Nullable Uri dataUri) {
                mDataMimeType = dataMimeType;
                mDataUri = dataUri;
                return this;
            }

            /**
             * Get the text to be used for this message, or the fallback text if a type and content
             * Uri have been set
             */
            public @Nullable CharSequence getText() {
                return mText;
            }

            /** Get the time at which this message arrived in ms since Unix epoch. */
            public long getTimestamp() {
                return mTimestamp;
            }

            /** Get the extras Bundle for this message. */
            public @NonNull Bundle getExtras() {
                return mExtras;
            }

            /**
             * Get the text used to display the contact's name in the messaging experience
             *
             * @deprecated Use {@link #getPerson()}
             */
            @Deprecated
            public @Nullable CharSequence getSender() {
                return mPerson == null ? null : mPerson.getName();
            }

            /** Returns the {@link Person} sender of this message. */
            public @Nullable Person getPerson() {
                return mPerson;
            }

            /** Get the MIME type of the data pointed to by the URI. */
            public @Nullable String getDataMimeType() {
                return mDataMimeType;
            }

            /**
             * Get the the Uri pointing to the content of the message. Can be null, in which case
             * {@see #getText()} is used.
             */
            public @Nullable Uri getDataUri() {
                return mDataUri;
            }

            private @NonNull Bundle toBundle() {
                Bundle bundle = new Bundle();
                if (mText != null) {
                    bundle.putCharSequence(KEY_TEXT, mText);
                }
                bundle.putLong(KEY_TIMESTAMP, mTimestamp);
                if (mPerson != null) {
                    // We must add both as Frameworks depends on this extra directly in order to
                    // render properly.
                    bundle.putCharSequence(KEY_SENDER, mPerson.getName());

                    // Write person to native notification
                    if (Build.VERSION.SDK_INT >= 28) {
                        bundle.putParcelable(KEY_NOTIFICATION_PERSON,
                                Api28Impl.castToParcelable(mPerson.toAndroidPerson()));
                    } else {
                        bundle.putBundle(KEY_PERSON, mPerson.toBundle());
                    }
                }
                if (mDataMimeType != null) {
                    bundle.putString(KEY_DATA_MIME_TYPE, mDataMimeType);
                }
                if (mDataUri != null) {
                    bundle.putParcelable(KEY_DATA_URI, mDataUri);
                }
                if (mExtras != null) {
                    bundle.putBundle(KEY_EXTRAS_BUNDLE, mExtras);
                }
                return bundle;
            }

            static Bundle @NonNull [] getBundleArrayForMessages(@NonNull List<Message> messages) {
                Bundle[] bundles = new Bundle[messages.size()];
                final int N = messages.size();
                for (int i = 0; i < N; i++) {
                    bundles[i] = messages.get(i).toBundle();
                }
                return bundles;
            }

            static @NonNull List<Message> getMessagesFromBundleArray(
                    Parcelable @NonNull [] bundles) {
                List<Message> messages = new ArrayList<>(bundles.length);
                for (int i = 0; i < bundles.length; i++) {
                    if (bundles[i] instanceof Bundle) {
                        Message message = getMessageFromBundle((Bundle)bundles[i]);
                        if (message != null) {
                            messages.add(message);
                        }
                    }
                }
                return messages;
            }

            @SuppressWarnings("deprecation")
            static @Nullable Message getMessageFromBundle(@NonNull Bundle bundle) {
                try {
                    if (!bundle.containsKey(KEY_TEXT) || !bundle.containsKey(KEY_TIMESTAMP)) {
                        return null;
                    }

                    Person person = null;
                    if (bundle.containsKey(KEY_PERSON)) {
                        // Person written via compat
                        person = Person.fromBundle(bundle.getBundle(KEY_PERSON));
                    } else if (bundle.containsKey(KEY_NOTIFICATION_PERSON)
                            && Build.VERSION.SDK_INT >= 28) {
                        // Person written via non-compat, or >= P
                        person = Person.fromAndroidPerson(
                                (android.app.Person) bundle.getParcelable(KEY_NOTIFICATION_PERSON));
                    } else if (bundle.containsKey(KEY_SENDER)) {
                        // Legacy person
                        person = new Person.Builder()
                                .setName(bundle.getCharSequence(KEY_SENDER))
                                .build();
                    }

                    Message message = new Message(
                            bundle.getCharSequence(KEY_TEXT),
                            bundle.getLong(KEY_TIMESTAMP),
                            person);

                    if (bundle.containsKey(KEY_DATA_MIME_TYPE)
                            && bundle.containsKey(KEY_DATA_URI)) {
                        message.setData(bundle.getString(KEY_DATA_MIME_TYPE),
                                (Uri) bundle.getParcelable(KEY_DATA_URI));
                    }
                    if (bundle.containsKey(KEY_EXTRAS_BUNDLE)) {
                        message.getExtras().putAll(bundle.getBundle(KEY_EXTRAS_BUNDLE));
                    }
                    return message;
                } catch (ClassCastException e) {
                    return null;
                }
            }

            /**
             * Converts this compat {@link Message} to the base Android framework
             * {@link Notification.MessagingStyle.Message}.
             */
            @RestrictTo(LIBRARY_GROUP_PREFIX)
            @RequiresApi(24)
            Notification.MessagingStyle.@NonNull Message toAndroidMessage() {
                Notification.MessagingStyle.Message frameworkMessage;
                Person person = getPerson();
                // Use Person for P and above
                if (Build.VERSION.SDK_INT >= 28) {
                    frameworkMessage = Api28Impl.createMessage(getText(), getTimestamp(),
                            person == null ? null : person.toAndroidPerson());
                } else {
                    frameworkMessage = Api24Impl.createMessage(getText(), getTimestamp(),
                            person == null ? null : person.getName());
                }

                if (getDataMimeType() != null) {
                    Api24Impl.setData(frameworkMessage, getDataMimeType(), getDataUri());
                }
                return frameworkMessage;
            }

            /**
             * A class for wrapping calls to {@link Notification.MessagingStyle.Message} methods
             * which were added in API 24; these calls must be wrapped to avoid performance issues.
             * See the UnsafeNewApiCall lint rule for more details.
             */
            @RequiresApi(24)
            static class Api24Impl {
                private Api24Impl() {
                    // This class is not instantiable.
                }

                static Notification.MessagingStyle.Message createMessage(CharSequence text,
                        long timestamp, CharSequence sender) {
                    return new Notification.MessagingStyle.Message(text, timestamp, sender);
                }

                static Notification.MessagingStyle.Message setData(
                        Notification.MessagingStyle.Message message, String dataMimeType,
                        Uri dataUri) {
                    return message.setData(dataMimeType, dataUri);
                }
            }

            /**
             * A class for wrapping calls to {@link Notification.MessagingStyle.Message} methods
             * which were added in API 28; these calls must be wrapped to avoid performance issues.
             * See the UnsafeNewApiCall lint rule for more details.
             */
            @RequiresApi(28)
            static class Api28Impl {
                private Api28Impl() {
                    // This class is not instantiable.
                }

                static Notification.MessagingStyle.Message createMessage(CharSequence text,
                        long timestamp, android.app.Person sender) {
                    return new Notification.MessagingStyle.Message(text, timestamp, sender);
                }

                static Parcelable castToParcelable(android.app.Person person) {
                    return person;
                }
            }
        }

        /**
         * A class for wrapping calls to {@link Notification.MessagingStyle} methods which
         * were added in API 24; these calls must be wrapped to avoid performance issues.
         * See the UnsafeNewApiCall lint rule for more details.
         */
        @RequiresApi(24)
        static class Api24Impl {
            private Api24Impl() { }

            static Notification.MessagingStyle createMessagingStyle(CharSequence userDisplayName) {
                return new Notification.MessagingStyle(userDisplayName);
            }

            static Notification.MessagingStyle addMessage(
                    Notification.MessagingStyle messagingStyle,
                    Notification.MessagingStyle.Message message) {
                return messagingStyle.addMessage(message);
            }

            static Notification.MessagingStyle setConversationTitle(
                    Notification.MessagingStyle messagingStyle, CharSequence conversationTitle) {
                return messagingStyle.setConversationTitle(conversationTitle);
            }
        }

        /**
         * A class for wrapping calls to {@link Notification.MessagingStyle} methods which
         * were added in API 26; these calls must be wrapped to avoid performance issues.
         * See the UnsafeNewApiCall lint rule for more details.
         */
        @RequiresApi(26)
        static class Api26Impl {
            private Api26Impl() { }

            static Notification.MessagingStyle addHistoricMessage(
                    Notification.MessagingStyle messagingStyle,
                    Notification.MessagingStyle.Message message) {
                return messagingStyle.addHistoricMessage(message);
            }

        }

        /**
         * A class for wrapping calls to {@link Notification.MessagingStyle} methods which
         * were added in API 28; these calls must be wrapped to avoid performance issues.
         * See the UnsafeNewApiCall lint rule for more details.
         */
        @RequiresApi(28)
        static class Api28Impl {
            private Api28Impl() { }

            static Notification.MessagingStyle createMessagingStyle(android.app.Person user) {
                return new Notification.MessagingStyle(user);
            }

            static Notification.MessagingStyle setGroupConversation(
                    Notification.MessagingStyle messagingStyle, boolean isGroupConversation) {
                return messagingStyle.setGroupConversation(isGroupConversation);
            }
        }
    }

    /**
     * Helper class for generating large-format notifications that include a caller and required
     * actions, and indicate an incoming call.
     * <br>
     * If the platform does not provide large-format notifications, this method has no effect. The
     * user will always see the normal notification view.
     * <br>
     * This class is a "rebuilder": It attaches to a Builder object and modifies its behavior,
     * like so:
     * <pre class="prettyprint">
     * Notification notification = new NotificationCompat.Builder(mContext)
     *     .setSmallIcon(R.drawable.new_post)
     *     .setStyle(
     *             new Notification.CallStyle.forIncomingCall(caller, declineIntent, answerIntent))
     *     .build();
     * </pre>
     * <br>
     * Note that for CallStyle Notifications on API versions before 31 to be ranked as they are
     * in API versions 31+, they must be associated with a foreground service. Additionally,
     * CallStyle Notifications on API versions before 31 can achieve the similar ranking by marking
     * the Notification as colorized, using {@link Builder#setColorized(boolean)}.
     */
    public static class CallStyle extends Style {

        private static final String TEMPLATE_CLASS_NAME =
                "androidx.core.app.NotificationCompat$CallStyle";

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({
                CALL_TYPE_UNKNOWN,
                CALL_TYPE_INCOMING,
                CALL_TYPE_ONGOING,
                CALL_TYPE_SCREENING
        })
        public @interface CallType {
        }

        ;

        // TODO: Replace these with the real CALL_TYPE constants, once they are available.
        /**
         * Unknown call type.
         *
         * See {@link #EXTRA_CALL_TYPE}.
         */
        public static final int CALL_TYPE_UNKNOWN = 0;

        /**
         * Call type for incoming calls.
         *
         * See {@link #EXTRA_CALL_TYPE}.
         */
        public static final int CALL_TYPE_INCOMING = 1;
        /**
         * Call type for ongoing calls.
         *
         * See {@link #EXTRA_CALL_TYPE}.
         */
        public static final int CALL_TYPE_ONGOING = 2;
        /**
         * Call type for calls that are being screened.
         *
         * See {@link #EXTRA_CALL_TYPE}.
         */
        public static final int CALL_TYPE_SCREENING = 3;

        /**
         * This is a key used privately on the action.extras to give spacing priority
         * to the required call actions
         */
        private static final String KEY_ACTION_PRIORITY = "key_action_priority";

        private int mCallType;
        private Person mPerson;
        private PendingIntent mAnswerIntent;
        private PendingIntent mDeclineIntent;
        private PendingIntent mHangUpIntent;
        private boolean mIsVideo;
        private Integer mAnswerButtonColor;
        private Integer mDeclineButtonColor;
        private IconCompat mVerificationIcon;
        private CharSequence mVerificationText;

        public CallStyle() {
        }

        /**
         * Creates a CallStyle linked to a notification builder.
         *
         * @param builder       the notification builder to link
         */
        public CallStyle(@Nullable Builder builder) {
            setBuilder(builder);
        }

        /**
         * Creates a CallStyle for an incoming call.
         * This notification will have a decline and an answer action, will allow a single
         * custom {@link Builder#addAction(Action) action}, and will have a default
         * {@link Builder#setContentText(CharSequence) content text} for an incoming call.
         *
         * @param person        the person displayed as the caller
         *                      the person also needs to have a non-empty name associated with it
         * @param declineIntent the intent to be sent when the user taps the decline action
         * @param answerIntent  the intent to be sent when the user taps the answer action
         */
        public static @NonNull CallStyle forIncomingCall(@NonNull Person person,
                @NonNull PendingIntent declineIntent, @NonNull PendingIntent answerIntent) {
            return new CallStyle(CALL_TYPE_INCOMING, person,
                    null /* hangUpIntent */,
                    requireNonNull(declineIntent, "declineIntent is required"),
                    requireNonNull(answerIntent, "answerIntent is required")
            );
        }

        /**
         * Creates a CallStyle for an ongoing call.
         * This notification will have a hang up action, will allow up to two
         * custom {@link Builder#addAction(Action) actions}, and will have a default
         * {@link Builder#setContentText(CharSequence) content text} for an ongoing call.
         *
         * @param person       the person displayed as being on the other end of the call
         *                     the person also needs to have a non-empty name associated with it
         * @param hangUpIntent the intent to be sent when the user taps the hang up action
         */
        public static @NonNull CallStyle forOngoingCall(@NonNull Person person,
                @NonNull PendingIntent hangUpIntent) {
            return new CallStyle(CALL_TYPE_ONGOING, person,
                    requireNonNull(hangUpIntent, "hangUpIntent is required"),
                    null /* declineIntent */,
                    null /* answerIntent */
            );
        }

        /**
         * Creates a CallStyle for a call that is being screened.
         * This notification will have a hang up and an answer action, will allow a single
         * custom {@link Builder#addAction(Action) action}, and will have a default
         * {@link Builder#setContentText(CharSequence) content text} for a call that is being
         * screened.
         *
         * @param person       the person displayed as the caller
         *                     the person also needs to have a non-empty name associated with it
         * @param hangUpIntent the intent to be sent when the user taps the hang up action
         * @param answerIntent the intent to be sent when the user taps the answer action
         */
        public static @NonNull CallStyle forScreeningCall(@NonNull Person person,
                @NonNull PendingIntent hangUpIntent, @NonNull PendingIntent answerIntent) {
            return new CallStyle(CALL_TYPE_SCREENING, person,
                    requireNonNull(hangUpIntent, "hangUpIntent is required"),
                    null /* declineIntent */,
                    requireNonNull(answerIntent, "answerIntent is required")
            );
        }

        /**
         * @param callType      the type of the call (for example, CALL_TYPE_INCOMING)
         * @param person        the person displayed for the incoming call
         *                      the user also needs to have a non-empty name associated with it
         * @param hangUpIntent  the intent to be sent when the user taps the hang up action
         * @param declineIntent the intent to be sent when the user taps the decline action
         * @param answerIntent  the intent to be sent when the user taps the answer action
         */
        private CallStyle(@CallType int callType, @NonNull Person person,
                @Nullable PendingIntent hangUpIntent, @Nullable PendingIntent declineIntent,
                @Nullable PendingIntent answerIntent) {
            if (person == null || TextUtils.isEmpty(person.getName())) {
                throw new IllegalArgumentException("person must have a non-empty a name");
            }
            mCallType = callType;
            mPerson = person;
            mAnswerIntent = answerIntent;
            mDeclineIntent = declineIntent;
            mHangUpIntent = hangUpIntent;
        }

        /**
         * Sets whether the call is a video call, which may affect the icons or text used on the
         * required action buttons.
         */
        public @NonNull CallStyle setIsVideo(boolean isVideo) {
            mIsVideo = isVideo;
            return this;
        }

        /**
         * Sets an optional icon to be displayed with {@link #setVerificationText(CharSequence)
         * text} as a verification status of the caller.
         */
        public @NonNull CallStyle setVerificationIcon(@Nullable Icon verificationIcon) {
            mVerificationIcon = verificationIcon == null ? null :
                    IconCompat.createFromIcon(verificationIcon);
            return this;
        }

        /**
         * Sets an optional icon to be displayed with {@link #setVerificationText(CharSequence)
         * text} as a verification status of the caller.
         */
        public @NonNull CallStyle setVerificationIcon(@Nullable Bitmap verificationIcon) {
            mVerificationIcon = IconCompat.createWithBitmap(verificationIcon);
            return this;
        }

        /**
         * Sets optional text to be displayed with an {@link #setVerificationIcon(Icon) icon}
         * as a verification status of the caller.
         */
        public @NonNull CallStyle setVerificationText(@Nullable CharSequence verificationText) {
            mVerificationText = verificationText;
            return this;
        }

        /**
         * Sets an optional color to be used as a hint for the Answer action button's color.
         * The system may change this color to ensure sufficient contrast with the background.
         * The system may choose to disregard this hint if the notification is not colorized.
         */
        public @NonNull CallStyle setAnswerButtonColorHint(@ColorInt int color) {
            mAnswerButtonColor = color;
            return this;
        }

        /**
         * Sets an optional color to be used as a hint for the Decline or Hang Up action button's
         * color.
         * The system may change this color to ensure sufficient contrast with the background.
         * The system may choose to disregard this hint if the notification is not colorized.
         */
        public @NonNull CallStyle setDeclineButtonColorHint(@ColorInt int color) {
            mDeclineButtonColor = color;
            return this;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        public boolean displayCustomViewInline() {
            // This is a lie; True is returned to make sure that the custom view is not used
            // instead of the template, but it will not actually be included.
            return true;
        }


        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        protected void restoreFromCompatExtras(@NonNull Bundle extras) {
            super.restoreFromCompatExtras(extras);

            mCallType = extras.getInt(EXTRA_CALL_TYPE);
            mIsVideo = extras.getBoolean(EXTRA_CALL_IS_VIDEO);
            if (Build.VERSION.SDK_INT >= 28
                    && extras.containsKey(EXTRA_CALL_PERSON)) {
                mPerson = Person.fromAndroidPerson(
                        (android.app.Person)
                                extras.getParcelable(EXTRA_CALL_PERSON));
            } else if (extras.containsKey(EXTRA_CALL_PERSON_COMPAT)) {
                mPerson = Person.fromBundle(extras.getBundle(EXTRA_CALL_PERSON_COMPAT));
            }
            if (extras.containsKey(EXTRA_VERIFICATION_ICON)) {
                mVerificationIcon = IconCompat.createFromIcon((Icon) extras.getParcelable(
                        EXTRA_VERIFICATION_ICON));
            } else if (extras.containsKey(EXTRA_VERIFICATION_ICON_COMPAT)) {
                mVerificationIcon =
                        IconCompat.createFromBundle(
                                extras.getBundle(EXTRA_VERIFICATION_ICON_COMPAT));
            }
            mVerificationText = extras.getCharSequence(EXTRA_VERIFICATION_TEXT);
            mAnswerIntent = (PendingIntent) extras.getParcelable(EXTRA_ANSWER_INTENT);
            mDeclineIntent = (PendingIntent) extras.getParcelable(EXTRA_DECLINE_INTENT);
            mHangUpIntent = (PendingIntent) extras.getParcelable(EXTRA_HANG_UP_INTENT);
            mAnswerButtonColor = extras.containsKey(EXTRA_ANSWER_COLOR)
                    ? extras.getInt(EXTRA_ANSWER_COLOR) : null;
            mDeclineButtonColor = extras.containsKey(EXTRA_DECLINE_COLOR)
                    ? extras.getInt(EXTRA_DECLINE_COLOR) : null;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        public void addCompatExtras(@NonNull Bundle extras) {
            super.addCompatExtras(extras);
            // Reminder: this method only needs to add fields which are not added by the platform
            // builder (and only needs to work at all for API 19+).

            extras.putInt(EXTRA_CALL_TYPE, mCallType);
            extras.putBoolean(EXTRA_CALL_IS_VIDEO, mIsVideo);
            if (mPerson != null) {
                if (Build.VERSION.SDK_INT >= 28) {
                    extras.putParcelable(EXTRA_CALL_PERSON,
                            Api28Impl.castToParcelable(mPerson.toAndroidPerson()));
                } else {
                    extras.putParcelable(EXTRA_CALL_PERSON_COMPAT, mPerson.toBundle());
                }
            }
            if (mVerificationIcon != null) {
                extras.putParcelable(EXTRA_VERIFICATION_ICON, mVerificationIcon.toIcon(mBuilder.mContext));
            }
            extras.putCharSequence(EXTRA_VERIFICATION_TEXT, mVerificationText);
            extras.putParcelable(EXTRA_ANSWER_INTENT, mAnswerIntent);
            extras.putParcelable(EXTRA_DECLINE_INTENT, mDeclineIntent);
            extras.putParcelable(EXTRA_HANG_UP_INTENT, mHangUpIntent);
            if (mAnswerButtonColor != null) {
                extras.putInt(EXTRA_ANSWER_COLOR, mAnswerButtonColor);
            }
            if (mDeclineButtonColor != null) {
                extras.putInt(EXTRA_DECLINE_COLOR, mDeclineButtonColor);
            }
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        protected @NonNull String getClassName() {
            return TEMPLATE_CLASS_NAME;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        public void apply(NotificationBuilderWithBuilderAccessor builderAccessor) {
            if (Build.VERSION.SDK_INT >= 31) {
                Notification.CallStyle style = null;
                switch (mCallType) {
                    case CALL_TYPE_INCOMING:
                        style = Api31Impl.forIncomingCall(mPerson.toAndroidPerson(),
                                mDeclineIntent, mAnswerIntent);
                        break;
                    case CALL_TYPE_ONGOING:
                        style = Api31Impl.forOngoingCall(mPerson.toAndroidPerson(),
                                mHangUpIntent);
                        break;
                    case CALL_TYPE_SCREENING:
                        style = Api31Impl.forScreeningCall(mPerson.toAndroidPerson(),
                                mHangUpIntent, mAnswerIntent);
                        break;
                    default:
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG,
                                    "Unrecognized call type in CallStyle: " + String.valueOf(
                                            mCallType));
                        }
                }
                if (style != null) {
                    style.setBuilder(builderAccessor.getBuilder());
                    if (mAnswerButtonColor != null) {
                        Api31Impl.setAnswerButtonColorHint(style, mAnswerButtonColor);
                    }
                    if (mDeclineButtonColor != null) {
                        Api31Impl.setDeclineButtonColorHint(style, mDeclineButtonColor);
                    }
                    Api31Impl.setVerificationText(style, mVerificationText);
                    if (mVerificationIcon != null) {
                        Api31Impl.setVerificationIcon(style,
                                mVerificationIcon.toIcon(mBuilder.mContext));
                    }
                    Api31Impl.setIsVideo(style, mIsVideo);
                }
            } else {
                // For versions before CallStyle existed, we fallback to an unstyled
                // notification, and modify the builder directly to set the relevant fields.
                // Fields not settable in the builder are added separately as part of the
                // RemoteView.
                Notification.Builder builder = builderAccessor.getBuilder();

                // Sets the notification title to the caller name.
                CharSequence title = mPerson != null ? mPerson.getName() : null;
                builder.setContentTitle(title);

                // Sets the text of the notification either to EXTRA_TEXT, or (if not set),
                // uses the default call notification text.
                CharSequence text =
                        mBuilder.mExtras != null && mBuilder.mExtras.containsKey(EXTRA_TEXT)
                                ? mBuilder.mExtras.getCharSequence(EXTRA_TEXT) : null;
                if (text == null) {
                    text = getDefaultText();
                }
                builder.setContentText(text);

                // Adds person information to the notification.
                if (mPerson != null) {
                    // Adds the caller icon, if available.
                    if (mPerson.getIcon() != null) {
                        builder.setLargeIcon(mPerson.getIcon().toIcon(mBuilder.mContext));
                    }

                    // Adds the caller person as being relevant to this notification.
                    if (Build.VERSION.SDK_INT >= 28) {
                        Api28Impl.addPerson(builder, mPerson.toAndroidPerson());
                    } else {
                        builder.addPerson(mPerson.getUri());
                    }
                }

                // Sets the category of the notification to CATEGORY_CALL; if the notification
                // has this set and is also from the default phone app, it will be ranked in the
                // shade similarly to how CallStyle notifications are ranked in API 31+.
                builder.setCategory(NotificationCompat.CATEGORY_CALL);
            }
        }

        /**
         * Provides the default text for a CallStyle notification. Corresponds to Notification
         * .CallStyle
         */
        private @Nullable String getDefaultText() {
            switch (mCallType) {
                case CALL_TYPE_INCOMING:
                    return mBuilder.mContext.getResources().getString(
                            R.string.call_notification_incoming_text);
                case CALL_TYPE_ONGOING:
                    return mBuilder.mContext.getResources().getString(
                            R.string.call_notification_ongoing_text);
                case CALL_TYPE_SCREENING:
                    return mBuilder.mContext.getResources().getString(
                            R.string.call_notification_screening_text);
            }
            return null;
        }

        private @NonNull Action makeNegativeAction() {
            int icon = R.drawable.ic_call_decline;
            if (mDeclineIntent == null) {
                return makeAction(icon, R.string.call_notification_hang_up_action,
                        mDeclineButtonColor,
                        R.color.call_notification_decline_color,
                        mHangUpIntent);
            } else {
                return makeAction(icon, R.string.call_notification_decline_action,
                        mDeclineButtonColor,
                        R.color.call_notification_decline_color,
                        mDeclineIntent);
            }
        }

        private @Nullable Action makeAnswerAction() {
            int videoIcon = R.drawable.ic_call_answer_video;
            int icon = R.drawable.ic_call_answer;

            return mAnswerIntent == null ? null : makeAction(
                    mIsVideo ? videoIcon : icon,
                    mIsVideo ? R.string.call_notification_answer_video_action
                            : R.string.call_notification_answer_action,
                    mAnswerButtonColor, R.color.call_notification_answer_color,
                    mAnswerIntent);
        }

        private @NonNull Action makeAction(int icon, int title, Integer colorInt,
                int defaultColorRes, PendingIntent intent) {
            if (colorInt == null) {
                colorInt = ContextCompat.getColor(mBuilder.mContext, defaultColorRes);
            }
            SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
            stringBuilder.append(mBuilder.mContext.getResources().getString(title));
            stringBuilder.setSpan(new ForegroundColorSpan(colorInt), 0, stringBuilder.length(),
                    SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE);

            Action action = new Action.Builder(
                    IconCompat.createWithResource(mBuilder.mContext, icon), stringBuilder,
                    intent).build();
            action.getExtras().putBoolean(KEY_ACTION_PRIORITY, true);
            return action;
        }

        private boolean isActionAddedByCallStyle(Action action) {
            // This is an internal extra added by the style to these actions. If an app were to add
            // this extra to the action themselves, the action would be dropped.  :shrug:
            return action != null && action.getExtras().getBoolean(KEY_ACTION_PRIORITY);
        }

        /**
         * Gets the actions list for the call with the answer/decline/hangUp actions inserted in
         * the correct place.  This returns the correct result even if the system actions have
         * already been added, and even if more actions were added since then.
         *
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public @NonNull ArrayList<Action> getActionsListWithSystemActions() {
            // Define the system actions we expect to see.
            final Action firstAction = makeNegativeAction();
            final Action lastAction = makeAnswerAction();

            // Start creating the result list.
            int nonContextualActionSlotsRemaining = MAX_ACTION_BUTTONS;
            ArrayList<Action> resultActions = new ArrayList<>(MAX_ACTION_BUTTONS);
            if (nonContextualActionSlotsRemaining > 0) {
                resultActions.add(firstAction);
                --nonContextualActionSlotsRemaining;
            }

            // Copy actions into the new list, correcting system actions.
            List<Action> existingActions = mBuilder.mActions;
            if (existingActions != null) {
                for (Action action : existingActions) {
                    if (action.isContextual()) {
                        // Always include all contextual actions
                        resultActions.add(action);
                    } else if (isActionAddedByCallStyle(action)) {
                        // Drops any old versions of system actions.
                    } else {
                        // Copies non-contextual actions; decrement the remaining action slots.
                        resultActions.add(action);
                        --nonContextualActionSlotsRemaining;
                    }
                    // If there's exactly one action slot left, fill it with the lastAction.
                    if (lastAction != null && nonContextualActionSlotsRemaining == 1) {
                        resultActions.add(lastAction);
                        --nonContextualActionSlotsRemaining;
                    }
                }
            }
            // If there are any action slots left, the lastAction still needs to be added.
            if (lastAction != null && nonContextualActionSlotsRemaining >= 1) {
                resultActions.add(lastAction);
            }
            return resultActions;
        }

        /**
         * A class for wrapping calls to {@link Notification.CallStyle} methods which
         * were added in API 28; these calls must be wrapped to avoid performance issues.
         * See the UnsafeNewApiCall lint rule for more details.
         */
        @RequiresApi(28)
        static class Api28Impl {
            private Api28Impl() {
            }

            static Notification.Builder addPerson(Notification.Builder builder,
                    android.app.Person person) {
                return builder.addPerson(person);
            }

            static Parcelable castToParcelable(android.app.Person person) {
                return person;
            }
        }

        /**
         * A class for wrapping calls to {@link Notification.CallStyle} methods which
         * were added in API 31; these calls must be wrapped to avoid performance issues.
         * See the UnsafeNewApiCall lint rule for more details.
         */
        @RequiresApi(31)
        static class Api31Impl {
            private Api31Impl() {
            }

            static Notification.CallStyle forIncomingCall(android.app.@NonNull Person person,
                    @NonNull PendingIntent declineIntent, @NonNull PendingIntent answerIntent) {
                return Notification.CallStyle.forIncomingCall(person, declineIntent, answerIntent);
            }

            static Notification.CallStyle forOngoingCall(android.app.@NonNull Person person,
                    @NonNull PendingIntent hangUpIntent) {
                return Notification.CallStyle.forOngoingCall(person, hangUpIntent);
            }

            static Notification.CallStyle forScreeningCall(android.app.@NonNull Person person,
                    @NonNull PendingIntent hangUpIntent, @NonNull PendingIntent answerIntent) {
                return Notification.CallStyle.forScreeningCall(person, hangUpIntent, answerIntent);
            }

            static Notification.CallStyle setIsVideo(Notification.CallStyle callStyle,
                    boolean isVideo) {
                return callStyle.setIsVideo(isVideo);
            }

            static Notification.CallStyle setVerificationIcon(Notification.CallStyle callStyle,
                    @Nullable Icon verificationIcon) {
                return callStyle.setVerificationIcon(verificationIcon);
            }

            static Notification.CallStyle setVerificationText(Notification.CallStyle callStyle,
                    @Nullable CharSequence verificationText) {
                return callStyle.setVerificationText(verificationText);
            }

            static Notification.CallStyle setAnswerButtonColorHint(Notification.CallStyle callStyle,
                    @ColorInt int color) {
                return callStyle.setAnswerButtonColorHint(color);
            }

            static Notification.CallStyle setDeclineButtonColorHint(
                    Notification.CallStyle callStyle, @ColorInt int color) {
                return callStyle.setDeclineButtonColorHint(color);
            }

            static Notification.Action.Builder setAuthenticationRequired(
                    Notification.Action.Builder actionBuilder, boolean authenticationRequired) {
                return actionBuilder.setAuthenticationRequired(authenticationRequired);
            }
        }
    }

    /**
     * Helper class for generating large-format notifications that include a list of (up to 5) strings.
     *
     * <br>
     * If the platform does not provide large-format notifications, this method has no effect. The
     * user will always see the normal notification view.
     * <br>
     * This class is a "rebuilder": It attaches to a Builder object and modifies its behavior, like so:
     * <pre class="prettyprint">
     * Notification notification = new Notification.Builder()
     *     .setContentTitle(&quot;5 New mails from &quot; + sender.toString())
     *     .setContentText(subject)
     *     .setSmallIcon(R.drawable.new_mail)
     *     .setLargeIcon(aBitmap)
     *     .setStyle(new Notification.InboxStyle()
     *         .addLine(str1)
     *         .addLine(str2)
     *         .setContentTitle(&quot;&quot;)
     *         .setSummaryText(&quot;+3 more&quot;))
     *     .build();
     * </pre>
     *
     * @see Notification#bigContentView
     */
    public static class InboxStyle extends Style {

        private static final String TEMPLATE_CLASS_NAME =
                "androidx.core.app.NotificationCompat$InboxStyle";

        private ArrayList<CharSequence> mTexts = new ArrayList<>();

        public InboxStyle() {
        }

        public InboxStyle(@Nullable Builder builder) {
            setBuilder(builder);
        }

        /**
         * Overrides ContentTitle in the big form of the template.
         * This defaults to the value passed to setContentTitle().
         */
        public @NonNull InboxStyle setBigContentTitle(@Nullable CharSequence title) {
            mBigContentTitle = Builder.limitCharSequenceLength(title);
            return this;
        }

        /**
         * Set the first line of text after the detail section in the big form of the template.
         */
        public @NonNull InboxStyle setSummaryText(@Nullable CharSequence cs) {
            mSummaryText = Builder.limitCharSequenceLength(cs);
            mSummaryTextSet = true;
            return this;
        }

        /**
         * Append a line to the digest section of the Inbox notification.
         */
        public @NonNull InboxStyle addLine(@Nullable CharSequence cs) {
            if (cs != null) {
                mTexts.add(Builder.limitCharSequenceLength(cs));
            }
            return this;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        protected @NonNull String getClassName() {
            return TEMPLATE_CLASS_NAME;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        public void apply(NotificationBuilderWithBuilderAccessor builder) {
            Notification.Builder builder1 = builder.getBuilder();
            Notification.InboxStyle style = new Notification.InboxStyle(builder1);
            style = style.setBigContentTitle(mBigContentTitle);
            if (mSummaryTextSet) {
                style.setSummaryText(mSummaryText);
            }
            for (CharSequence text: mTexts) {
                style.addLine(text);
            }
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        protected void restoreFromCompatExtras(@NonNull Bundle extras) {
            super.restoreFromCompatExtras(extras);
            mTexts.clear();

            if (extras.containsKey(EXTRA_TEXT_LINES)) {
                Collections.addAll(mTexts, extras.getCharSequenceArray(EXTRA_TEXT_LINES));
            }
        }

        /**
         */
        @Override
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        protected void clearCompatExtraKeys(@NonNull Bundle extras) {
            super.clearCompatExtraKeys(extras);
            extras.remove(EXTRA_TEXT_LINES);
        }
    }

    /**
     * Helper class for generating large-format notifications that display progress to the user
     * with a highly customizable progress bar with segments, points, a custom tracker icon,
     * and custom icons at the start and end of the progress bar.
     * <br>
     * If the platform does not provide large-format notifications, this method has no effect. The
     * user will always see the normal notification view.
     * <br>
     * This class is a "rebuilder": It attaches to a Builder object and modifies its behavior,
     * like so:
     * <pre class="prettyprint">
     * new NotificationCompat.Builder(context)
     *   .setSmallIcon(R.drawable.ic_notification)
     *   .setColor(Color.GREEN)
     *   .setColorized(true)
     *   .setContentTitle("Arrive 10:08 AM").
     *   .setContentText("Dominique Ansel Bakery Soho")
     *   .addAction(new NotificationCompat.Action("Exit navigation",...))
     *   .setStyle(new NotificationCompat.ProgressStyle()
     *       .setStyledByProgress(false)
     *       .setProgress(456)
     *       .setProgressTrackerIcon(IconCompat.createWithResource(R.drawable.ic_driving_tracker))
     *       .addProgressSegment(new Segment(41).setColor(Color.BLACK))
     *       .addProgressSegment(new Segment(552).setColor(Color.YELLOW))
     *       .addProgressSegment(new Segment(253).setColor(Color.YELLOW))
     *       .addProgressSegment(new Segment(94).setColor(Color.BLUE))
     *       .addProgressPoint(new Point(60).setColor(Color.RED))
     *       .addProgressPoint(new Point(560).setColor(Color.YELLOW))
     *   )
     * </pre>
     * <p>
     * NOTE: ProgressStyle Notifications are supported on Android 36 and above. If the SDK version
     * is below 36, the ProgressStyle will fall back to the default notification style.
     * </p>
     */
    public static class ProgressStyle extends Style {
        private static final String KEY_ELEMENT_ID = "id";
        private static final String KEY_ELEMENT_COLOR = "colorInt";
        private static final String KEY_SEGMENT_LENGTH = "length";
        private static final String KEY_POINT_POSITION = "position";

        private static final String TEMPLATE_CLASS_NAME =
                "androidx.core.app.NotificationCompat$ProgressStyle";

        private static final int DEFAULT_PROGRESS_MAX = 100;

        private List<Segment> mProgressSegments = new ArrayList<>();
        private List<Point> mProgressPoints = new ArrayList<>();

        private int mProgress = 0;

        private boolean mIndeterminate;

        private boolean mIsStyledByProgress = true;

        @Nullable
        private IconCompat mTrackerIcon;
        @Nullable
        private IconCompat mStartIcon;
        @Nullable
        private IconCompat mEndIcon;

        /**
         * Gets the segments that define the background layer of the progress bar.
         *
         * If no segments are provided, the progress bar will be rendered with a single segment
         * with length 100 and default color.
         *
         * @see #setProgressSegments
         * @see #addProgressSegment
         * @see Segment
         */
        public @NonNull List<Segment> getProgressSegments() {
            return mProgressSegments;
        }

        /**
         * Sets or replaces the segments of the progress bar.
         *
         * Segments allow for creating progress bars with multiple colors or sections
         * to represent different stages or categories of progress.
         * For example, Traffic conditions along a navigation journey.
         * @see Segment
         */
        public @NonNull ProgressStyle setProgressSegments(@NonNull List<Segment> progressSegments) {
            if (mProgressSegments == null) {
                mProgressSegments = new ArrayList<>();
            }
            mProgressSegments.clear();
            for (Segment segment : progressSegments) {
                addProgressSegment(segment);
            }
            return this;
        }

        /**
         * Appends a segment to the end of the progress bar.
         *
         * Segments allow for creating progress bars with multiple colors or sections
         * to represent different stages or categories of progress.
         * For example, Traffic conditions along a navigation journey.
         * @see Segment
         */
        public @NonNull ProgressStyle addProgressSegment(@NonNull Segment segment) {
            if (mProgressSegments == null) {
                mProgressSegments = new ArrayList<>();
            }
            if (segment.getLength() > 0) {
                mProgressSegments.add(segment);
            }
            return this;
        }

        /**
         * Gets the points that are displayed on the progress bar.
         *.
         * @see #setProgressPoints
         * @see #addProgressPoint
         * @see Point
         */
        public @NonNull List<Point> getProgressPoints() {
            return mProgressPoints;
        }

        /**
         * Replaces all the progress points.
         *
         * Points within a progress bar are used to visualize distinct stages or milestones.
         * For example, you might use points to mark stops in a multi-stop
         * navigation journey, where each point represents a destination.
         * @see Point
         */
        public @NonNull ProgressStyle setProgressPoints(@NonNull List<Point> points) {
            if (mProgressPoints == null) {
                mProgressPoints = new ArrayList<>();
            }
            mProgressPoints.clear();

            for (Point point : points) {
                addProgressPoint(point);
            }
            return this;
        }

        /**
         * Adds another point.
         *
         * Points within a progress bar are used to visualize distinct stages or milestones.
         *
         * For example, you might use points to mark stops in a multi-stop
         * navigation journey, where each point represents a destination.
         *
         * Points can be added in any order, as their
         * position within the progress bar is determined by their individual
         * {@link Point#getPosition()}.
         * @see Point
         */
        public @NonNull ProgressStyle addProgressPoint(@NonNull Point point) {
            if (mProgressPoints == null) {
                mProgressPoints = new ArrayList<>();
            }
            if (point.getPosition() > 0) {
                mProgressPoints.add(point);
            }
            return this;
        }

        /**
         * Gets the progress value of the progress bar.
         * @see #setProgress
         */
        @IntRange(from = 0)
        public int getProgress() {
            return mProgress;
        }

        /**
         * Specifies the progress (in the same units as {@link Segment#getLength()})
         * of the tracker along the length of the bar.
         *
         * The max progress value is the sum of all Segment lengths.
         * The default value is 0.
         */
        public @NonNull ProgressStyle setProgress(@IntRange(from = 0) int progress) {
            mProgress = progress;
            return this;
        }

        /**
         * Gets the sum of the lengths of all Segments in the style, which
         * defines the maximum progress. Defaults to 100 when segments are omitted.
         */
        @IntRange(from = 0)
        public int getProgressMax() {
            final List<Segment> progressSegment = mProgressSegments;
            if (progressSegment == null || progressSegment.isEmpty()) {
                return DEFAULT_PROGRESS_MAX;
            } else {
                int progressMax = 0;
                int validSegmentCount = 0;
                for (int i = 0; i < progressSegment.size(); i++) {
                    int segmentLength = progressSegment.get(i).getLength();
                    if (segmentLength > 0) {
                        try {
                            progressMax = Math.addExact(progressMax, segmentLength);
                            validSegmentCount++;
                        } catch (ArithmeticException e) {
                            return DEFAULT_PROGRESS_MAX;
                        }
                    }
                }

                if (validSegmentCount == 0) {
                    return DEFAULT_PROGRESS_MAX;
                }

                return progressMax;
            }

        }

        /**
         * Get indeterminate value of the progress bar.
         * @see #setProgressIndeterminate
         */
        public boolean isProgressIndeterminate() {
            return mIndeterminate;
        }

        /**
         * Used to indicate an initialization state without a known progress amount.
         * When specified, the following fields are ignored:
         * @see #setProgress
         * @see #setProgressSegments
         * @see #setProgressPoints
         * @see #setProgressTrackerIcon
         * @see #setStyledByProgress
         *
         * If the app provides exactly one Segment, that segment's color will be
         * used to style the indeterminate bar.
         */
        public @NonNull ProgressStyle setProgressIndeterminate(boolean indeterminate) {
            mIndeterminate = indeterminate;
            return this;
        }

        /**
         * Gets whether the progress bar's style is based on its progress.
         * @see #setStyledByProgress
         */
        public boolean isStyledByProgress() {
            return mIsStyledByProgress;
        }

        /**
         * Indicates whether the segments and points will be styled differently
         * based on whether they are behind or ahead of the current progress.
         * When true, segments appearing ahead of the current progress will be given a
         * slightly different appearance to indicate that it is part of the progress bar
         * that is not "filled".
         * When false, all segments will be given the filled appearance, and it will be
         * the app's responsibility to use #setProgressTrackerIcon or segment colors
         * to make the current progress clear to the user.
         * the default value is true.
         */
        public @NonNull ProgressStyle setStyledByProgress(boolean enabled) {
            mIsStyledByProgress = enabled;
            return this;
        }


        /**
         * Gets the progress tracker icon for the progress bar.
         * @see #setProgressTrackerIcon
         */
        public @Nullable IconCompat getProgressTrackerIcon() {
            return mTrackerIcon;
        }

        /**
         * An optional icon that can appear as an overlay on the bar at the point of
         * current progress.
         * Aspect ratio may be anywhere from 2:1 to 1:2; content outside that
         * aspect ratio range will be cropped.
         * This icon will be mirrored in RTL.
         */
        public @NonNull ProgressStyle setProgressTrackerIcon(@Nullable IconCompat trackerIcon) {
            mTrackerIcon = trackerIcon;
            return this;
        }

        /**
         * Gets the progress bar start icon.
         * @see #setProgressStartIcon
         */
        public @Nullable IconCompat getProgressStartIcon() {
            return mStartIcon;
        }

        /**
         * An optional square icon that appears at the start of the progress bar.
         * This icon will be cropped to its central square.
         * This icon will NOT be mirrored in RTL layouts.
         */
        public @NonNull ProgressStyle setProgressStartIcon(@Nullable IconCompat startIcon) {
            mStartIcon = startIcon;
            return this;
        }

        /**
         * Gets the progress bar end icon.
         * @see #setProgressEndIcon
         */
        public @Nullable IconCompat getProgressEndIcon() {
            return mEndIcon;
        }

        /**
         * An optional square icon that appears at the end of the progress bar.
         * This icon will be cropped to its central square.
         * This icon will NOT be mirrored in RTL layouts.
         */
        public @NonNull ProgressStyle setProgressEndIcon(@Nullable IconCompat endIcon) {
            mEndIcon = endIcon;
            return this;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        public boolean displayCustomViewInline() {
            // This is a lie; True is returned for progress notifications to make sure
            // that the custom view is not used instead of the template, but it will not
            // actually be included.
            return true;
        }

        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        protected @Nullable String getClassName() {
            return TEMPLATE_CLASS_NAME;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        public void apply(NotificationBuilderWithBuilderAccessor builder) {
            final Notification.Builder builder1 = builder.getBuilder();
            if (Build.VERSION.SDK_INT >= 36) {
                Context context = null;
                if (builder instanceof NotificationCompatBuilder) {
                    context = ((NotificationCompatBuilder) builder).getContext();
                }

                final Notification.ProgressStyle progressStyle = new Notification.ProgressStyle();
                Api36Impl.setStyledByProgress(progressStyle, mIsStyledByProgress);
                Api36Impl.setProgress(progressStyle, mProgress);
                Api36Impl.setProgressIndeterminate(progressStyle, mIndeterminate);

                Icon startIcon = null;
                if (mStartIcon != null) {
                    startIcon = mStartIcon.toIcon(context);
                }
                Api36Impl.setProgressStartIcon(progressStyle, startIcon);

                Icon endIcon = null;
                if (mEndIcon != null) {
                    endIcon = mEndIcon.toIcon(context);
                }
                Api36Impl.setProgressEndIcon(progressStyle, endIcon);

                Icon trackerIcon = null;
                if (mTrackerIcon != null) {
                    trackerIcon = mTrackerIcon.toIcon(context);
                }
                Api36Impl.setProgressTrackerIcon(progressStyle, trackerIcon);

                Api36Impl.setProgressPoints(progressStyle, mProgressPoints);
                Api36Impl.setProgressSegments(progressStyle, mProgressSegments);

                builder1.setStyle(progressStyle);
            } else {
                final int progressMax = getProgressMax();
                builder1.setProgress(
                        progressMax,
                        Math.min(mProgress, progressMax),
                        mIndeterminate);
            }
        }

        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        public void addCompatExtras(@NonNull Bundle extras) {
            super.addCompatExtras(extras);

            if (Build.VERSION.SDK_INT < 36) {
                extras.putParcelableArrayList(EXTRA_PROGRESS_SEGMENTS,
                        getProgressSegmentsAsBundleList(mProgressSegments));
                extras.putParcelableArrayList(EXTRA_PROGRESS_POINTS,
                        getProgressPointsAsBundleList(mProgressPoints));

                extras.putInt(EXTRA_PROGRESS, mProgress);
                extras.putBoolean(EXTRA_PROGRESS_INDETERMINATE, mIndeterminate);
                extras.putInt(EXTRA_PROGRESS_MAX, getProgressMax());
                extras.putBoolean(EXTRA_STYLED_BY_PROGRESS, mIsStyledByProgress);
                Context context = null;
                if (mBuilder != null) {
                    context = mBuilder.mContext;
                }

                if (context == null) {
                    return;
                }

                if (mTrackerIcon != null) {
                    extras.putParcelable(EXTRA_PROGRESS_TRACKER_ICON, mTrackerIcon.toIcon(context));
                } else {
                    extras.remove(EXTRA_PROGRESS_TRACKER_ICON);
                }

                if (mStartIcon != null) {
                    extras.putParcelable(EXTRA_PROGRESS_START_ICON, mStartIcon.toIcon(context));
                } else {
                    extras.remove(EXTRA_PROGRESS_START_ICON);
                }

                if (mEndIcon != null) {
                    extras.putParcelable(EXTRA_PROGRESS_END_ICON, mEndIcon.toIcon(context));
                } else {
                    extras.remove(EXTRA_PROGRESS_END_ICON);
                }
            }
        }

        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        protected void restoreFromCompatExtras(@NonNull Bundle extras) {
            super.restoreFromCompatExtras(extras);

            mProgressSegments = getProgressSegmentsFromBundleList(
                    BundleCompat.getParcelableArrayList(
                            extras, EXTRA_PROGRESS_SEGMENTS, Bundle.class));
            mProgress = extras.getInt(EXTRA_PROGRESS, 0);
            mIndeterminate = extras.getBoolean(EXTRA_PROGRESS_INDETERMINATE, false);
            mIsStyledByProgress = extras.getBoolean(EXTRA_STYLED_BY_PROGRESS, true);
            mProgressPoints = getProgressPointsFromBundleList(
                    BundleCompat.getParcelableArrayList(
                            extras, EXTRA_PROGRESS_POINTS, Bundle.class));
            mTrackerIcon = asIconCompat(
                   BundleCompat.getParcelable(extras, EXTRA_PROGRESS_TRACKER_ICON, Icon.class));
            mStartIcon = asIconCompat(
                   BundleCompat.getParcelable(extras, EXTRA_PROGRESS_START_ICON, Icon.class));
            mEndIcon = asIconCompat(
                   BundleCompat.getParcelable(extras, EXTRA_PROGRESS_END_ICON, Icon.class));
        }

        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        protected void clearCompatExtraKeys(@NonNull Bundle extras) {
            super.clearCompatExtraKeys(extras);
            extras.remove(EXTRA_PROGRESS_SEGMENTS);
            extras.remove(EXTRA_PROGRESS);
            extras.remove(EXTRA_STYLED_BY_PROGRESS);
            extras.remove(EXTRA_PROGRESS_TRACKER_ICON);
            extras.remove(EXTRA_PROGRESS_START_ICON);
            extras.remove(EXTRA_PROGRESS_END_ICON);
            extras.remove(EXTRA_PROGRESS_POINTS);
            extras.remove(EXTRA_PROGRESS_INDETERMINATE);
        }

        private static @Nullable IconCompat asIconCompat(@Nullable Parcelable bitmapOrIcon) {
            if (bitmapOrIcon != null) {
                if (bitmapOrIcon instanceof Icon) {
                    return IconCompat.createFromIcon((Icon) bitmapOrIcon);
                }
                if (bitmapOrIcon instanceof Bitmap) {
                    return IconCompat.createWithBitmap((Bitmap) bitmapOrIcon);
                }
            }
            return null;
        }

        private static @NonNull List<Segment> getProgressSegmentsFromBundleList(
                @Nullable List<Bundle> segmentBundleList) {
            final ArrayList<Segment> segments = new ArrayList<>();
            if (segmentBundleList != null && !segmentBundleList.isEmpty()) {
                for (int i = 0; i < segmentBundleList.size(); i++) {
                    final Bundle segmentBundle = segmentBundleList.get(i);
                    final int length = segmentBundle.getInt(KEY_SEGMENT_LENGTH);
                    if (length <= 0) {
                        continue;
                    }

                    final int id = segmentBundle.getInt(KEY_ELEMENT_ID);
                    final int color = segmentBundle.getInt(KEY_ELEMENT_COLOR,
                            Notification.COLOR_DEFAULT);
                    final Segment segment = new Segment(length)
                            .setId(id).setColor(color);

                    segments.add(segment);
                }
            }

            return segments;
        }

        private static @NonNull ArrayList<Bundle> getProgressPointsAsBundleList(
                @Nullable List<Point> progressPoints) {
            final ArrayList<Bundle> points = new ArrayList<>();
            if (progressPoints != null && !progressPoints.isEmpty()) {
                for (int i = 0; i < progressPoints.size(); i++) {
                    final Point point = progressPoints.get(i);
                    if (point.getPosition() < 0) {
                        continue;
                    }

                    final Bundle bundle = new Bundle();
                    bundle.putInt(KEY_POINT_POSITION, point.getPosition());
                    bundle.putInt(KEY_ELEMENT_ID, point.getId());
                    bundle.putInt(KEY_ELEMENT_COLOR, point.getColor());

                    points.add(bundle);
                }
            }

            return points;
        }

        private static @NonNull List<Point> getProgressPointsFromBundleList(
                @Nullable List<Bundle> pointBundleList) {
            final ArrayList<Point> points = new ArrayList<>();

            if (pointBundleList != null && !pointBundleList.isEmpty()) {
                for (int i = 0; i < pointBundleList.size(); i++) {
                    final Bundle pointBundle = pointBundleList.get(i);
                    final int position = pointBundle.getInt(KEY_POINT_POSITION);
                    if (position < 0) {
                        continue;
                    }
                    final int id = pointBundle.getInt(KEY_ELEMENT_ID);
                    final int color = pointBundle.getInt(KEY_ELEMENT_COLOR,
                            Notification.COLOR_DEFAULT);
                    final Point point = new Point(position).setId(id).setColor(color);
                    points.add(point);
                }
            }

            return points;
        }

        private static @NonNull ArrayList<Bundle> getProgressSegmentsAsBundleList(
                @Nullable List<Segment> progressSegments) {
            final ArrayList<Bundle> segments = new ArrayList<>();
            if (progressSegments != null && !progressSegments.isEmpty()) {
                for (int i = 0; i < progressSegments.size(); i++) {
                    final Segment segment = progressSegments.get(i);
                    if (segment.getLength() <= 0) {
                        continue;
                    }

                    final Bundle bundle = new Bundle();
                    bundle.putInt(KEY_SEGMENT_LENGTH, segment.getLength());
                    bundle.putInt(KEY_ELEMENT_ID, segment.getId());
                    bundle.putInt(KEY_ELEMENT_COLOR, segment.getColor());

                    segments.add(bundle);
                }
            }

            return segments;
        }

        /**
         * A segment of the progress bar, which defines its length and color.
         * Segments allow for creating progress bars with multiple colors or sections
         * to represent different stages or categories of progress.
         * For example, Traffic conditions along a navigation journey.
         */
        public static final class Segment {
            private int mLength;
            private int mId = 0;
            @ColorInt
            private int mColor = NotificationCompat.COLOR_DEFAULT;

            /**
             * Create a segment with a non-zero length.
             * @param length
             * See {@link #getLength}
             */
            public Segment(@IntRange(from = 1) int length) {
                mLength = length;
            }

            /**
             * The length of this Segment within the progress bar.
             * This value has no units, it is just relative to the length of other segments,
             * and the value provided to {@link ProgressStyle#setProgress}.
             */
            @IntRange(from = 1)
            public int getLength() {
                return mLength;
            }

            /**
             * Gets the id of this Segment.
             *
             * @see #setId
             */
            public int getId() {
                return mId;
            }

            /**
             * Optional ID used to uniquely identify the element across updates.
             * The default is 0.
             */
            public @NonNull Segment setId(int id) {
                mId = id;
                return this;
            }

            /**
             * Returns the color of this Segment.
             *
             * @see #setColor
             * @see #COLOR_DEFAULT for the default visual behavior when it is not set.
             */
            @ColorInt
            public int getColor() {
                return mColor;
            }

            /**
             * Optional color of this Segment
             */
            public @NonNull Segment setColor(@ColorInt int color) {
                mColor = color;
                return this;
            }
        }

        /**
         * A point within the progress bar, defining its position and color.
         * Points within a progress bar are used to visualize distinct stages or milestones.
         * For example, you might use points to mark stops in a multi-stop
         * navigation journey, where each point represents a destination.
         */
        public static final class Point {

            private int mPosition;
            private int mId = 0;
            @ColorInt
            private int mColor = NotificationCompat.COLOR_DEFAULT;

            /**
             * Create a point element.
             * The position of this point on the progress bar
             * relative to {@link ProgressStyle#getProgressMax}
             * @param position
             * See {@link #getPosition}
             */
            public Point(@IntRange(from = 1) int position) {
                mPosition = position;
            }

            /**
             * Gets the position of this Point.
             * The position of this point on the progress bar
             * relative to {@link ProgressStyle#getProgressMax}.
             */
            @IntRange(from = 1)
            public int getPosition() {
                return mPosition;
            }


            /**
             * Optional ID used to uniquely identify the element across updates.
             */
            public int getId() {
                return mId;
            }

            /**
             * Optional ID used to uniquely identify the element across updates.
             * The default is 0.
             */
            public @NonNull Point setId(int id) {
                mId = id;
                return this;
            }

            /**
             * Returns the color of this Segment.
             *
             * @see #setColor
             * @see #COLOR_DEFAULT for the default visual behavior when it is not set.
             */
            @ColorInt
            public int getColor() {
                return mColor;
            }

            /**
             * Optional color of this Segment
             */
            public @NonNull Point setColor(@ColorInt int color) {
                mColor = color;
                return this;
            }
        }

        /**
         * A class for wrapping calls to {@link Notification.ProgressStyle} methods which
         * were added in API 36.
         * See the UnsafeNewApiCall lint rule for more details.
         */
        @RequiresApi(36)
        private static final class Api36Impl {
            private Api36Impl() {
            }

            @RequiresApi(36)
            static void setStyledByProgress(
                    Notification.ProgressStyle progressStyle,
                    boolean isStyledByProgress) {
                progressStyle.setStyledByProgress(isStyledByProgress);
            }

            @RequiresApi(36)
            static void setProgress(
                    Notification.ProgressStyle progressStyle,
                    int progress) {
                progressStyle.setProgress(progress);
            }

            @RequiresApi(36)
            static void setProgressIndeterminate(
                    Notification.ProgressStyle progressStyle,
                    boolean indeterminate
            ) {
                progressStyle.setProgressIndeterminate(indeterminate);

            }

            @RequiresApi(36)
            static void setProgressStartIcon(
                    Notification.ProgressStyle progressStyle,
                    @Nullable Icon startIcon) {
                progressStyle.setProgressStartIcon(startIcon);
            }

            @RequiresApi(36)
            static void setProgressEndIcon(
                    Notification.ProgressStyle progressStyle,
                    @Nullable Icon endIcon) {
                progressStyle.setProgressEndIcon(endIcon);
            }

            @RequiresApi(36)
            static void setProgressTrackerIcon(
                    Notification.ProgressStyle progressStyle,
                    @Nullable Icon trackerIcon) {
                progressStyle.setProgressTrackerIcon(trackerIcon);
            }

            @RequiresApi(36)
            static void setProgressPoints(
                    Notification.ProgressStyle progressStyle,
                    @NonNull List<Point> progressPoints) {
                for (Point point : progressPoints) {
                    progressStyle.addProgressPoint(
                            new Notification.ProgressStyle.Point(point.getPosition())
                                    .setColor(point.getColor())
                                    .setId(point.getId()));
                }
            }

            @RequiresApi(36)
            static void setProgressSegments(
                    Notification.ProgressStyle progressStyle,
                    @NonNull List<Segment> progressSegments) {
                for (Segment segment : progressSegments) {
                    progressStyle.addProgressSegment(
                            new Notification.ProgressStyle.Segment(segment.getLength())
                                    .setColor(segment.getColor())
                                    .setId(segment.getId()));
                }

            }
        }
    }

    /**
     * Notification style for custom views that are decorated by the system.
     *
     * <p>Instead of providing a notification that is completely custom, a developer can set this
     * style and still obtain system decorations like the notification header with the expand
     * affordance and actions.
     *
     * <p>Use {@link Builder#setCustomContentView(RemoteViews)},
     * {@link Builder#setCustomBigContentView(RemoteViews)} and
     * {@link Builder#setCustomHeadsUpContentView(RemoteViews)} to set the
     * corresponding custom views to display.
     *
     * <p>To use this style with your Notification, feed it to
     * {@link Builder#setStyle(Style)} like so:
     * <pre class="prettyprint">
     * Notification noti = new NotificationCompat.Builder()
     *     .setSmallIcon(R.drawable.ic_stat_player)
     *     .setLargeIcon(albumArtBitmap))
     *     .setCustomContentView(contentView)
     *     .setStyle(<b>new NotificationCompat.DecoratedCustomViewStyle()</b>)
     *     .build();
     * </pre>
     *
     * <p>If you are using this style, consider using the corresponding styles like
     * {@link R.style#TextAppearance_Compat_Notification} or
     * {@link R.style#TextAppearance_Compat_Notification_Title} in
     * your custom views in order to get the correct styling on each platform version.
     */
    public static class DecoratedCustomViewStyle extends Style {

        private static final String TEMPLATE_CLASS_NAME =
                "androidx.core.app.NotificationCompat$DecoratedCustomViewStyle";

        private static final int MAX_ACTION_BUTTONS = 3;

        public DecoratedCustomViewStyle() {
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        protected @NonNull String getClassName() {
            return TEMPLATE_CLASS_NAME;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        public boolean displayCustomViewInline() {
            return true;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        public void apply(NotificationBuilderWithBuilderAccessor builder) {
            if (Build.VERSION.SDK_INT >= 24) {
                Notification.Builder builder1 = builder.getBuilder();
                builder1.setStyle(Api24Impl.createDecoratedCustomViewStyle());

            }
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        public RemoteViews makeContentView(NotificationBuilderWithBuilderAccessor builder) {
            if (Build.VERSION.SDK_INT >= 24) {
                // No custom content view required
                return null;
            }
            if (mBuilder.getContentView() == null) {
                // No special content view
                return null;
            }
            return createRemoteViews(mBuilder.getContentView(), false);
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        public RemoteViews makeBigContentView(NotificationBuilderWithBuilderAccessor builder) {
            if (Build.VERSION.SDK_INT >= 24) {
                // No custom big content view required
                return null;
            }
            RemoteViews bigContentView = mBuilder.getBigContentView();
            RemoteViews innerView = bigContentView != null
                    ? bigContentView
                    : mBuilder.getContentView();
            if (innerView == null) {
                // No expandable notification
                return null;
            }
            return createRemoteViews(innerView, true);
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @Override
        public RemoteViews makeHeadsUpContentView(NotificationBuilderWithBuilderAccessor builder) {
            if (Build.VERSION.SDK_INT >= 24) {
                // No custom heads up content view required
                return null;
            }
            RemoteViews headsUp = mBuilder.getHeadsUpContentView();
            RemoteViews innerView = headsUp != null ? headsUp : mBuilder.getContentView();
            if (headsUp == null) {
                // No expandable notification
                return null;
            }
            return createRemoteViews(innerView, true);
        }


        /**
         * A helper method to get texts from a {@link Notification}'s custom content view made by
         * either
         * {@link Builder#setCustomBigContentView(RemoteViews)},
         * {@link Builder#setCustomContentView(RemoteViews)} or
         * {@link Builder#setCustomHeadsUpContentView(RemoteViews)}.
         *
         * Note that this method will not look for {@link Notification#publicVersion} made by
         * {@link Builder#setPublicVersion(Notification)}.
         *
         * @param context A {@link Context} that will be used to inflate the content view from
         *                the notification.
         * @param notification The notification from which to get texts from its content view.
         * @return A list of text from the notification custom content view made by the above
         * method. Note that the method only returns a list of text from one of the custom view
         * as the above when it set, meaning when multiple custom content views has set in a
         * notification, the returned list will base on the detail of custom content and usage as
         * the priority: First is {@link Notification#bigContentView}, then is
         * {@link Notification#contentView} when no big content view has set, or
         * {@link Notification#headsUpContentView} when set. Otherwise, returns the empty list.
         */
        @SuppressWarnings("MixedMutabilityReturnType")
        @RequiresApi(24)
        public static @NonNull List<CharSequence> getTextsFromContentView(@NonNull Context context,
                @NonNull Notification notification) {
            final String styleClassName = notification.extras.getString(EXTRA_TEMPLATE);
            if (!Notification.DecoratedCustomViewStyle.class.getName().equals(styleClassName)) {
                return Collections.emptyList();
            }

            if (notification.contentView == null && notification.bigContentView == null
                    && notification.headsUpContentView == null) {
                return Collections.emptyList();
            }

            RemoteViews contentView = notification.bigContentView != null
                    ? notification.bigContentView : notification.contentView != null
                    ? notification.contentView : notification.headsUpContentView;
            final String packageName = contentView.getPackage();
            ApplicationInfo applicationInfo;
            Context packageContext;
            try {
                packageContext = context.createPackageContext(packageName, 0);
                applicationInfo = context.getPackageManager().getApplicationInfo(packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
            packageContext.setTheme(applicationInfo.theme);
            View contentLayout = contentView.apply(packageContext, null);

            final ArrayList<CharSequence> texts = new ArrayList<>();
            getTextsFromViewTraversal(contentLayout, texts);

            return texts;
        }

        private static void getTextsFromViewTraversal(View v, ArrayList<CharSequence> outTexts) {
            if (!(v instanceof ViewGroup)) {
                return;
            }
            for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) {
                View child = ((ViewGroup) v).getChildAt(i);
                if (child instanceof TextView) {
                    CharSequence text = ((TextView) child).getText();
                    if (text != null && text.length() > 0) {
                        outTexts.add(text);
                    }
                }
                if (child instanceof ViewGroup) {
                    getTextsFromViewTraversal(child, outTexts);
                }
            }
        }

        private RemoteViews createRemoteViews(RemoteViews innerView, boolean showActions) {
            RemoteViews remoteViews = applyStandardTemplate(true /* showSmallIcon */,
                    R.layout.notification_template_custom_big, false /* fitIn1U */);
            remoteViews.removeAllViews(R.id.actions);
            boolean actionsVisible = false;

            // In the UI contextual actions appear separately from the standard actions, so we
            // filter them out here.
            List<Action> nonContextualActions =
                    getNonContextualActions(mBuilder.mActions);

            if (showActions && nonContextualActions != null) {
                int numActions = Math.min(nonContextualActions.size(), MAX_ACTION_BUTTONS);
                if (numActions > 0) {
                    actionsVisible = true;
                    for (int i = 0; i < numActions; i++) {
                        final RemoteViews button =
                                generateActionButton(nonContextualActions.get(i));
                        remoteViews.addView(R.id.actions, button);
                    }
                }
            }
            int actionVisibility = actionsVisible ? View.VISIBLE : View.GONE;
            remoteViews.setViewVisibility(R.id.actions, actionVisibility);
            remoteViews.setViewVisibility(R.id.action_divider, actionVisibility);
            buildIntoRemoteViews(remoteViews, innerView);
            return remoteViews;
        }

        private static List<Action> getNonContextualActions(
                List<Action> actions) {
            if (actions == null) return null;
            List<Action> nonContextualActions = new ArrayList<>();
            for (Action action : actions) {
                if (!action.isContextual()) {
                    nonContextualActions.add(action);
                }
            }
            return nonContextualActions;
        }

        private RemoteViews generateActionButton(Action action) {
            final boolean tombstone = (action.actionIntent == null);
            RemoteViews button = new RemoteViews(mBuilder.mContext.getPackageName(),
                    tombstone ? R.layout.notification_action_tombstone
                            : R.layout.notification_action);
            IconCompat icon = action.getIconCompat();
            if (icon != null) {
                button.setImageViewBitmap(R.id.action_image,
                        createColoredBitmap(icon, R.color.notification_action_color_filter));
            }
            button.setTextViewText(R.id.action_text, action.title);
            if (!tombstone) {
                button.setOnClickPendingIntent(R.id.action_container, action.actionIntent);
            }
            button.setContentDescription(R.id.action_container, action.title);
            return button;
        }

        /**
         * A class for wrapping calls to {@link Notification.DecoratedCustomViewStyle} methods which
         * were added in API 24; these calls must be wrapped to avoid performance issues.
         * See the UnsafeNewApiCall lint rule for more details.
         */
        @RequiresApi(24)
        static class Api24Impl {
            private Api24Impl() { }

            static Notification.Style createDecoratedCustomViewStyle() {
                return new Notification.DecoratedCustomViewStyle();
            }

        }
    }

    /**
     * Structure to encapsulate a named action that can be shown as part of this notification.
     * It must include an icon, a label, and a {@link PendingIntent} to be fired when the action is
     * selected by the user. Action buttons won't appear on platforms prior to Android
     * {@link android.os.Build.VERSION_CODES#JELLY_BEAN}.
     * <p>
     * As of Android {@link android.os.Build.VERSION_CODES#N},
     * action button icons will not be displayed on action buttons, but are still required and
     * are available to
     * {@link android.service.notification.NotificationListenerService notification listeners},
     * which may display them in other contexts, for example on a wearable device.
     * <p>
     * Apps should use {@link NotificationCompat.Builder#addAction(int, CharSequence, PendingIntent)}
     * or {@link NotificationCompat.Builder#addAction(NotificationCompat.Action)}
     * to attach actions.
     */
    public static class Action {
        /**
         * {@link SemanticAction}: No semantic action defined.
         */
        public static final int SEMANTIC_ACTION_NONE = 0;

        /**
         * {@link SemanticAction}: Reply to a conversation, chat, group, or wherever replies
         * may be appropriate.
         */
        public static final int SEMANTIC_ACTION_REPLY = 1;

        /**
         * {@link SemanticAction}: Mark content as read.
         */
        public static final int SEMANTIC_ACTION_MARK_AS_READ = 2;

        /**
         * {@link SemanticAction}: Mark content as unread.
         */
        public static final int SEMANTIC_ACTION_MARK_AS_UNREAD = 3;

        /**
         * {@link SemanticAction}: Delete the content associated with the notification. This
         * could mean deleting an email, message, etc.
         */
        public static final int SEMANTIC_ACTION_DELETE = 4;

        /**
         * {@link SemanticAction}: Archive the content associated with the notification. This
         * could mean archiving an email, message, etc.
         */
        public static final int SEMANTIC_ACTION_ARCHIVE = 5;

        /**
         * {@link SemanticAction}: Mute the content associated with the notification. This could
         * mean silencing a conversation or currently playing media.
         */
        public static final int SEMANTIC_ACTION_MUTE = 6;

        /**
         * {@link SemanticAction}: Unmute the content associated with the notification. This could
         * mean un-silencing a conversation or currently playing media.
         */
        public static final int SEMANTIC_ACTION_UNMUTE = 7;

        /**
         * {@link SemanticAction}: Mark content with a thumbs up.
         */
        public static final int SEMANTIC_ACTION_THUMBS_UP = 8;

        /**
         * {@link SemanticAction}: Mark content with a thumbs down.
         */
        public static final int SEMANTIC_ACTION_THUMBS_DOWN = 9;

        /**
         * {@link SemanticAction}: Call a contact, group, etc.
         */
        public static final int SEMANTIC_ACTION_CALL = 10;

        static final String EXTRA_SHOWS_USER_INTERFACE =
                "android.support.action.showsUserInterface";

        static final String EXTRA_SEMANTIC_ACTION = "android.support.action.semanticAction";

        final Bundle mExtras;
        private @Nullable IconCompat mIcon;
        private final RemoteInput[] mRemoteInputs;

        /**
         * Holds {@link RemoteInput}s that only accept data, meaning
         * {@link RemoteInput#getAllowFreeFormInput} is false, {@link RemoteInput#getChoices}
         * is null or empty, and {@link RemoteInput#getAllowedDataTypes is non-null and not
         * empty. These {@link RemoteInput}s will be ignored by devices that do not
         * support non-text-based {@link RemoteInput}s. See {@link Builder#build}.
         *
         * You can test if a RemoteInput matches these constraints using
         * {@link RemoteInput#isDataOnly}.
         */
        private final RemoteInput[] mDataOnlyRemoteInputs;

        private boolean mAllowGeneratedReplies;
        boolean mShowsUserInterface = true;

        private final @SemanticAction int mSemanticAction;
        private final boolean mIsContextual;

        /**
         * Small icon representing the action.
         *
         * @deprecated Use {@link #getIconCompat()} instead.
         */
        @Deprecated
        public int icon;
        /**
         * Title of the action.
         */
        public @Nullable CharSequence title;
        /**
         * Intent to send when the user invokes this action. May be null, in which case the action
         * may be rendered in a disabled presentation.
         */
        public @Nullable PendingIntent actionIntent;

        private boolean mAuthenticationRequired;

        public Action(int icon, @Nullable CharSequence title, @Nullable PendingIntent intent) {
            this(icon == 0 ? null : IconCompat.createWithResource(null, "", icon), title, intent);
        }

        /**
         * <strong>Note:</strong> For devices running an Android version strictly lower than API
         * level 23 this constructor only supports resource-ID based IconCompat objects.
         */
        public Action(@Nullable IconCompat icon, @Nullable CharSequence title,
                @Nullable PendingIntent intent) {
            this(icon, title, intent, new Bundle(), null, null, true, SEMANTIC_ACTION_NONE, true,
                    false /* isContextual */, false /* authRequired */);
        }

        Action(int icon, @Nullable CharSequence title, @Nullable PendingIntent intent,
                @Nullable Bundle extras, RemoteInput @Nullable [] remoteInputs,
                RemoteInput @Nullable [] dataOnlyRemoteInputs, boolean allowGeneratedReplies,
                @SemanticAction int semanticAction, boolean showsUserInterface,
                boolean isContextual, boolean requireAuth) {
            this(icon == 0 ? null : IconCompat.createWithResource(null, "", icon), title,
                    intent, extras, remoteInputs, dataOnlyRemoteInputs, allowGeneratedReplies,
                    semanticAction, showsUserInterface, isContextual, requireAuth);
        }

        // Package private access to avoid adding a SyntheticAccessor for the Action.Builder class.
        @SuppressWarnings("deprecation")
        Action(@Nullable IconCompat icon, @Nullable CharSequence title,
                @Nullable PendingIntent intent, @Nullable Bundle extras,
                RemoteInput @Nullable [] remoteInputs,
                RemoteInput @Nullable [] dataOnlyRemoteInputs, boolean allowGeneratedReplies,
                @SemanticAction int semanticAction, boolean showsUserInterface,
                boolean isContextual, boolean requireAuth) {
            this.mIcon = icon;
            if (icon != null && icon.getType() == IconCompat.TYPE_RESOURCE) {
                this.icon = icon.getResId();
            }
            this.title = NotificationCompat.Builder.limitCharSequenceLength(title);
            this.actionIntent = intent;
            this.mExtras = extras != null ? extras : new Bundle();
            this.mRemoteInputs = remoteInputs;
            this.mDataOnlyRemoteInputs = dataOnlyRemoteInputs;
            this.mAllowGeneratedReplies = allowGeneratedReplies;
            this.mSemanticAction = semanticAction;
            this.mShowsUserInterface = showsUserInterface;
            this.mIsContextual = isContextual;
            this.mAuthenticationRequired = requireAuth;
        }

        /**
         * @deprecated use {@link #getIconCompat()} instead.
         */
        @SuppressWarnings("deprecation")
        @Deprecated
        public int getIcon() {
            return icon;
        }

        /**
         * Return the icon associated with this Action.
         */
        @SuppressWarnings("deprecation")
        public @Nullable IconCompat getIconCompat() {
            if (mIcon == null && icon != 0) {
                mIcon = IconCompat.createWithResource(null, "", icon);
            }
            return mIcon;
        }

        public @Nullable CharSequence getTitle() {
            return title;
        }

        public @Nullable PendingIntent getActionIntent() {
            return actionIntent;
        }

        /**
         * Get additional metadata carried around with this Action.
         */
        public @NonNull Bundle getExtras() {
            return mExtras;
        }

        /**
         * Return whether the platform should automatically generate possible replies for this
         * {@link Action}
         */
        public boolean getAllowGeneratedReplies() {
            return mAllowGeneratedReplies;
        }

        /**
         * Returns whether the OS should only send this action's {@link PendingIntent} on an
         * unlocked device.
         *
         * If the device is locked when the action is invoked, the OS should show the keyguard and
         * require successful authentication before invoking the intent.
         */
        public boolean isAuthenticationRequired() {
            return mAuthenticationRequired;
        }

        /**
         * Get the list of inputs to be collected from the user when this action is sent.
         * May return null if no remote inputs were added. Only returns inputs which accept
         * a text input. For inputs which only accept data use {@link #getDataOnlyRemoteInputs}.
         */
        public RemoteInput @Nullable [] getRemoteInputs() {
            return mRemoteInputs;
        }

        /**
         * Returns the {@link SemanticAction} associated with this {@link Action}. A
         * {@link SemanticAction} denotes what an {@link Action}'s {@link PendingIntent} will do
         * (eg. reply, mark as read, delete, etc).
         *
         * @see SemanticAction
         */
        public @SemanticAction int getSemanticAction() {
            return mSemanticAction;
        }

        /**
         * Returns whether this is a contextual Action, i.e. whether the action is dependent on the
         * notification message body. An example of a contextual action could be an action opening a
         * map application with an address shown in the notification.
         */
        public boolean isContextual() {
            return mIsContextual;
        }

        /**
         * Get the list of inputs to be collected from the user that ONLY accept data when this
         * action is sent. These remote inputs are guaranteed to return true on a call to
         * {@link RemoteInput#isDataOnly}.
         *
         * <p>May return null if no data-only remote inputs were added.
         *
         * <p>This method exists so that legacy RemoteInput collectors that pre-date the addition
         * of non-textual RemoteInputs do not access these remote inputs.
         */
        public RemoteInput @Nullable [] getDataOnlyRemoteInputs() {
            return mDataOnlyRemoteInputs;
        }

        /**
         * Return whether or not triggering this {@link Action}'s {@link PendingIntent} will open a
         * user interface.
         */
        public boolean getShowsUserInterface() {
            return mShowsUserInterface;
        }

        /**
         * Builder class for {@link Action} objects.
         */
        public static final class Builder {
            private final IconCompat mIcon;
            private final CharSequence mTitle;
            private final PendingIntent mIntent;
            private boolean mAllowGeneratedReplies = true;
            private final Bundle mExtras;
            private ArrayList<RemoteInput> mRemoteInputs;
            private @SemanticAction int mSemanticAction;
            private boolean mShowsUserInterface = true;
            private boolean mIsContextual;
            private boolean mAuthenticationRequired;

            /**
             * Creates a {@link Builder} from an {@link android.app.Notification.Action}.
             *
             */
            @RestrictTo(LIBRARY_GROUP_PREFIX)
            public static @NonNull Builder fromAndroidAction(Notification.@NonNull Action action) {
                final Builder builder;
                if (action.getIcon() != null) {
                    IconCompat iconCompat = IconCompat.createFromIconOrNullIfZeroResId(
                            action.getIcon());
                    builder = new NotificationCompat.Action.Builder(iconCompat, action.title,
                            action.actionIntent);
                } else {
                    builder = new NotificationCompat.Action.Builder(action.icon, action.title,
                            action.actionIntent);
                }
                android.app.RemoteInput[] remoteInputs = action.getRemoteInputs();
                if (remoteInputs != null && remoteInputs.length != 0) {
                    for (android.app.RemoteInput remoteInput : remoteInputs) {
                        builder.addRemoteInput(RemoteInput.fromPlatform(remoteInput));
                    }
                }
                if (Build.VERSION.SDK_INT >= 24) {
                    builder.mAllowGeneratedReplies = Api24Impl.getAllowGeneratedReplies(action);
                }
                if (Build.VERSION.SDK_INT >= 28) {
                    builder.setSemanticAction(Api28Impl.getSemanticAction(action));
                }
                if (Build.VERSION.SDK_INT >= 29) {
                    builder.setContextual(Api29Impl.isContextual(action));
                }
                if (Build.VERSION.SDK_INT >= 31) {
                    builder.setAuthenticationRequired(Api31Impl.isAuthenticationRequired(action));
                }
                builder.addExtras(action.getExtras());
                return builder;
            }

            /**
             * Construct a new builder for {@link Action} object.
             *
             * <p><strong>Note:</strong> For devices running an Android version strictly lower than
             * API level 23 this constructor only supports resource-ID based IconCompat objects.
             * @param icon icon to show for this action
             * @param title the title of the action
             * @param intent the {@link PendingIntent} to fire when users trigger this action
             */
            public Builder(@Nullable IconCompat icon, @Nullable CharSequence title,
                    @Nullable PendingIntent intent) {
                this(icon, title, intent, new Bundle(), null, true, SEMANTIC_ACTION_NONE, true,
                        false /* isContextual */, false /* authRequired */);
            }

            /**
             * Construct a new builder for {@link Action} object.
             * @param icon icon to show for this action
             * @param title the title of the action
             * @param intent the {@link PendingIntent} to fire when users trigger this action
             */
            public Builder(int icon, @Nullable CharSequence title, @Nullable PendingIntent intent) {
                this(icon == 0 ? null : IconCompat.createWithResource(null, "", icon), title,
                        intent,
                        new Bundle(),
                        null,
                        true,
                        SEMANTIC_ACTION_NONE,
                        true,
                        false /* isContextual */, false /* authRequired */);
            }

            /**
             * Construct a new builder for {@link Action} object using the fields from an
             * {@link Action}.
             * @param action the action to read fields from.
             */
            public Builder(@NonNull Action action) {
                this(action.getIconCompat(), action.title, action.actionIntent,
                        new Bundle(action.mExtras),
                        action.getRemoteInputs(), action.getAllowGeneratedReplies(),
                        action.getSemanticAction(), action.mShowsUserInterface,
                        action.isContextual(), action.isAuthenticationRequired());
            }

            private Builder(@Nullable IconCompat icon, @Nullable CharSequence title,
                    @Nullable PendingIntent intent, @NonNull Bundle extras,
                    RemoteInput @Nullable [] remoteInputs, boolean allowGeneratedReplies,
                    @SemanticAction int semanticAction, boolean showsUserInterface,
                    boolean isContextual, boolean authRequired) {
                mIcon = icon;
                mTitle = NotificationCompat.Builder.limitCharSequenceLength(title);
                mIntent = intent;
                mExtras = extras;
                mRemoteInputs = remoteInputs == null ? null : new ArrayList<>(
                        Arrays.asList(remoteInputs));
                mAllowGeneratedReplies = allowGeneratedReplies;
                mSemanticAction = semanticAction;
                mShowsUserInterface = showsUserInterface;
                mIsContextual = isContextual;
                mAuthenticationRequired = authRequired;
            }

            /**
             * Merge additional metadata into this builder.
             *
             * <p>Values within the Bundle will replace existing extras values in this Builder.
             *
             * @see NotificationCompat.Action#getExtras
             */
            public @NonNull Builder addExtras(@Nullable Bundle extras) {
                if (extras != null) {
                    mExtras.putAll(extras);
                }
                return this;
            }

            /**
             * Get the metadata Bundle used by this Builder.
             *
             * <p>The returned Bundle is shared with this Builder.
             */
            public @NonNull Bundle getExtras() {
                return mExtras;
            }

            /**
             * Add an input to be collected from the user when this action is sent.
             * Response values can be retrieved from the fired intent by using the
             * {@link RemoteInput#getResultsFromIntent} function.
             * @param remoteInput a {@link RemoteInput} to add to the action
             * @return this object for method chaining
             */
            public @NonNull Builder addRemoteInput(@Nullable RemoteInput remoteInput) {
                if (mRemoteInputs == null) {
                    mRemoteInputs = new ArrayList<>();
                }
                if (remoteInput != null) {
                    mRemoteInputs.add(remoteInput);
                }
                return this;
            }

            /**
             * Set whether the platform should automatically generate possible replies to add to
             * {@link RemoteInput#getChoices()}. If the {@link Action} doesn't have a
             * {@link RemoteInput}, this has no effect.
             * @param allowGeneratedReplies {@code true} to allow generated replies, {@code false}
             * otherwise
             * @return this object for method chaining
             * The default value is {@code true}
             */
            public @NonNull Builder setAllowGeneratedReplies(boolean allowGeneratedReplies) {
                mAllowGeneratedReplies = allowGeneratedReplies;
                return this;
            }

            /**
             * Sets the {@link SemanticAction} for this {@link Action}. A {@link SemanticAction}
             * denotes what an {@link Action}'s {@link PendingIntent} will do (eg. reply, mark
             * as read, delete, etc).
             * @param semanticAction a {@link SemanticAction} defined within {@link Action} with
             * {@code SEMANTIC_ACTION_} prefixes
             * @return this object for method chaining
             */
            public @NonNull Builder setSemanticAction(@SemanticAction int semanticAction) {
                mSemanticAction = semanticAction;
                return this;
            }

            /**
             * Sets whether this {@link Action} is a contextual action, i.e. whether the action is
             * dependent on the notification message body. An example of a contextual action could
             * be an action opening a map application with an address shown in the notification.
             */
            public @NonNull Builder setContextual(boolean isContextual) {
                mIsContextual = isContextual;
                return this;
            }

            /**
             * From API 31, sets whether the OS should only send this action's {@link PendingIntent}
             * on an unlocked device.
             *
             * If this is true and the device is locked when the action is invoked, the OS will
             * show the keyguard and require successful authentication before invoking the intent.
             * If this is false and the device is locked, the OS will decide whether authentication
             * should be required.
             */
            public @NonNull Builder setAuthenticationRequired(boolean authenticationRequired) {
                mAuthenticationRequired = authenticationRequired;
                return this;
            }

            /**
             * Set whether or not this {@link Action}'s {@link PendingIntent} will open a user
             * interface.
             * @param showsUserInterface {@code true} if this {@link Action}'s {@link PendingIntent}
             * will open a user interface, otherwise {@code false}
             * @return this object for method chaining
             * The default value is {@code true}
             */
            public @NonNull Builder setShowsUserInterface(boolean showsUserInterface) {
                mShowsUserInterface = showsUserInterface;
                return this;
            }

            /**
             * Apply an extender to this action builder. Extenders may be used to add
             * metadata or change options on this builder.
             */
            public @NonNull Builder extend(@NonNull Extender extender) {
                extender.extend(this);
                return this;
            }

            /**
             * Throws an NPE if we are building a contextual action missing one of the fields
             * necessary to display the action.
             */
            private void checkContextualActionNullFields() {
                if (!mIsContextual) return;

                if (mIntent == null) {
                    throw new NullPointerException(
                            "Contextual Actions must contain a valid PendingIntent");
                }
            }

            /**
             * Combine all of the options that have been set and return a new {@link Action}
             * object.
             * @return the built action
             * @throws NullPointerException if this is a contextual Action and its Intent is
             * null.
             */
            public @NonNull Action build() {
                checkContextualActionNullFields();

                List<RemoteInput> dataOnlyInputs = new ArrayList<>();
                List<RemoteInput> textInputs = new ArrayList<>();
                if (mRemoteInputs != null) {
                    for (RemoteInput input : mRemoteInputs) {
                        if (input.isDataOnly()) {
                            dataOnlyInputs.add(input);
                        } else {
                            textInputs.add(input);
                        }
                    }
                }
                RemoteInput[] dataOnlyInputsArr = dataOnlyInputs.isEmpty()
                        ? null : dataOnlyInputs.toArray(new RemoteInput[dataOnlyInputs.size()]);
                RemoteInput[] textInputsArr = textInputs.isEmpty()
                        ? null : textInputs.toArray(new RemoteInput[textInputs.size()]);
                return new Action(mIcon, mTitle, mIntent, mExtras, textInputsArr,
                        dataOnlyInputsArr, mAllowGeneratedReplies, mSemanticAction,
                        mShowsUserInterface, mIsContextual, mAuthenticationRequired);
            }

            /**
             * A class for wrapping calls to {@link Notification.Action.Builder} methods which
             * were added in API 24; these calls must be wrapped to avoid performance issues.
             * See the UnsafeNewApiCall lint rule for more details.
             */
            @RequiresApi(24)
            static class Api24Impl {
                private Api24Impl() { }

                static boolean getAllowGeneratedReplies(Notification.Action action) {
                    return action.getAllowGeneratedReplies();
                }
            }

            /**
             * A class for wrapping calls to {@link Notification.Action.Builder} methods which
             * were added in API 28; these calls must be wrapped to avoid performance issues.
             * See the UnsafeNewApiCall lint rule for more details.
             */
            @RequiresApi(28)
            static class Api28Impl {
                private Api28Impl() { }

                static int getSemanticAction(Notification.Action action) {
                    return action.getSemanticAction();
                }
            }

            /**
             * A class for wrapping calls to {@link Notification.Action.Builder} methods which
             * were added in API 29; these calls must be wrapped to avoid performance issues.
             * See the UnsafeNewApiCall lint rule for more details.
             */
            @RequiresApi(29)
            static class Api29Impl {
                private Api29Impl() { }

                static boolean isContextual(Notification.Action action) {
                    return action.isContextual();
                }
            }

            /**
             * A class for wrapping calls to {@link Notification.Action.Builder} methods which
             * were added in API 31; these calls must be wrapped to avoid performance issues.
             * See the UnsafeNewApiCall lint rule for more details.
             */
            @RequiresApi(31)
            static class Api31Impl {
                private Api31Impl() { }

                static boolean isAuthenticationRequired(Notification.Action action) {
                    return action.isAuthenticationRequired();
                }
            }

        }


        /**
         * Extender interface for use with {@link Builder#extend}. Extenders may be used to add
         * metadata or change options on an action builder.
         */
        public interface Extender {
            /**
             * Apply this extender to a notification action builder.
             * @param builder the builder to be modified.
             * @return the build object for chaining.
             */
            @NonNull Builder extend(@NonNull Builder builder);
        }

        /**
         * Wearable extender for notification actions. To add extensions to an action,
         * create a new {@link NotificationCompat.Action.WearableExtender} object using
         * the {@code WearableExtender()} constructor and apply it to a
         * {@link NotificationCompat.Action.Builder} using
         * {@link NotificationCompat.Action.Builder#extend}.
         *
         * <pre class="prettyprint">
         * NotificationCompat.Action action = new NotificationCompat.Action.Builder(
         *         R.drawable.archive_all, "Archive all", actionIntent)
         *         .extend(new NotificationCompat.Action.WearableExtender()
         *                 .setAvailableOffline(false))
         *         .build();</pre>
         */
        public static final class WearableExtender implements Extender {
            /** Notification action extra which contains wearable extensions */
            private static final String EXTRA_WEARABLE_EXTENSIONS = "android.wearable.EXTENSIONS";

            // Keys within EXTRA_WEARABLE_EXTENSIONS for wearable options.
            private static final String KEY_FLAGS = "flags";
            private static final String KEY_IN_PROGRESS_LABEL = "inProgressLabel";
            private static final String KEY_CONFIRM_LABEL = "confirmLabel";
            private static final String KEY_CANCEL_LABEL = "cancelLabel";

            // Flags bitwise-ored to mFlags
            private static final int FLAG_AVAILABLE_OFFLINE = 0x1;
            private static final int FLAG_HINT_LAUNCHES_ACTIVITY = 1 << 1;
            private static final int FLAG_HINT_DISPLAY_INLINE = 1 << 2;

            // Default value for flags integer
            private static final int DEFAULT_FLAGS = FLAG_AVAILABLE_OFFLINE;

            private int mFlags = DEFAULT_FLAGS;

            private CharSequence mInProgressLabel;
            private CharSequence mConfirmLabel;
            private CharSequence mCancelLabel;

            /**
             * Create a {@link NotificationCompat.Action.WearableExtender} with default
             * options.
             */
            public WearableExtender() {
            }

            /**
             * Create a {@link NotificationCompat.Action.WearableExtender} by reading
             * wearable options present in an existing notification action.
             * @param action the notification action to inspect.
             */
            public WearableExtender(@NonNull Action action) {
                Bundle wearableBundle = action.getExtras().getBundle(EXTRA_WEARABLE_EXTENSIONS);
                if (wearableBundle != null) {
                    mFlags = wearableBundle.getInt(KEY_FLAGS, DEFAULT_FLAGS);
                    mInProgressLabel = wearableBundle.getCharSequence(KEY_IN_PROGRESS_LABEL);
                    mConfirmLabel = wearableBundle.getCharSequence(KEY_CONFIRM_LABEL);
                    mCancelLabel = wearableBundle.getCharSequence(KEY_CANCEL_LABEL);
                }
            }

            /**
             * Apply wearable extensions to a notification action that is being built. This is
             * typically called by the {@link NotificationCompat.Action.Builder#extend}
             * method of {@link NotificationCompat.Action.Builder}.
             */
            @Override
            public Action.@NonNull Builder extend(Action.@NonNull Builder builder) {
                Bundle wearableBundle = new Bundle();

                if (mFlags != DEFAULT_FLAGS) {
                    wearableBundle.putInt(KEY_FLAGS, mFlags);
                }
                if (mInProgressLabel != null) {
                    wearableBundle.putCharSequence(KEY_IN_PROGRESS_LABEL, mInProgressLabel);
                }
                if (mConfirmLabel != null) {
                    wearableBundle.putCharSequence(KEY_CONFIRM_LABEL, mConfirmLabel);
                }
                if (mCancelLabel != null) {
                    wearableBundle.putCharSequence(KEY_CANCEL_LABEL, mCancelLabel);
                }

                builder.getExtras().putBundle(EXTRA_WEARABLE_EXTENSIONS, wearableBundle);
                return builder;
            }

            @Override
            public @NonNull WearableExtender clone() {
                WearableExtender that = new WearableExtender();
                that.mFlags = this.mFlags;
                that.mInProgressLabel = this.mInProgressLabel;
                that.mConfirmLabel = this.mConfirmLabel;
                that.mCancelLabel = this.mCancelLabel;
                return that;
            }

            /**
             * Set whether this action is available when the wearable device is not connected to
             * a companion device. The user can still trigger this action when the wearable device
             * is offline, but a visual hint will indicate that the action may not be available.
             * Defaults to true.
             */
            public @NonNull WearableExtender setAvailableOffline(boolean availableOffline) {
                setFlag(FLAG_AVAILABLE_OFFLINE, availableOffline);
                return this;
            }

            /**
             * Get whether this action is available when the wearable device is not connected to
             * a companion device. The user can still trigger this action when the wearable device
             * is offline, but a visual hint will indicate that the action may not be available.
             * Defaults to true.
             */
            public boolean isAvailableOffline() {
                return (mFlags & FLAG_AVAILABLE_OFFLINE) != 0;
            }

            private void setFlag(int mask, boolean value) {
                if (value) {
                    mFlags |= mask;
                } else {
                    mFlags &= ~mask;
                }
            }

            /**
             * Set a label to display while the wearable is preparing to automatically execute the
             * action. This is usually a 'ing' verb ending in ellipsis like "Sending..."
             *
             * @param label the label to display while the action is being prepared to execute
             * @return this object for method chaining
             *
             * @deprecated This method has no effect starting with Wear 2.0.
             */
            @Deprecated
            public @NonNull WearableExtender setInProgressLabel(@Nullable CharSequence label) {
                mInProgressLabel = label;
                return this;
            }

            /**
             * Get the label to display while the wearable is preparing to automatically execute
             * the action. This is usually a 'ing' verb ending in ellipsis like "Sending..."
             *
             * @return the label to display while the action is being prepared to execute
             *
             * @deprecated This method has no effect starting with Wear 2.0.
             */
            @Deprecated
            public @Nullable CharSequence getInProgressLabel() {
                return mInProgressLabel;
            }

            /**
             * Set a label to display to confirm that the action should be executed.
             * This is usually an imperative verb like "Send".
             *
             * @param label the label to confirm the action should be executed
             * @return this object for method chaining
             *
             * @deprecated This method has no effect starting with Wear 2.0.
             */
            @Deprecated
            public @NonNull WearableExtender setConfirmLabel(@Nullable CharSequence label) {
                mConfirmLabel = label;
                return this;
            }

            /**
             * Get the label to display to confirm that the action should be executed.
             * This is usually an imperative verb like "Send".
             *
             * @return the label to confirm the action should be executed
             *
             * @deprecated This method has no effect starting with Wear 2.0.
             */
            @Deprecated
            public @Nullable CharSequence getConfirmLabel() {
                return mConfirmLabel;
            }

            /**
             * Set a label to display to cancel the action.
             * This is usually an imperative verb, like "Cancel".
             *
             * @param label the label to display to cancel the action
             * @return this object for method chaining
             *
             * @deprecated This method has no effect starting with Wear 2.0.
             */
            @Deprecated
            public @NonNull WearableExtender setCancelLabel(@Nullable CharSequence label) {
                mCancelLabel = label;
                return this;
            }

            /**
             * Get the label to display to cancel the action.
             * This is usually an imperative verb like "Cancel".
             *
             * @return the label to display to cancel the action
             *
             * @deprecated This method has no effect starting with Wear 2.0.
             */
            @Deprecated
            public @Nullable CharSequence getCancelLabel() {
                return mCancelLabel;
            }

            /**
             * Set a hint that this Action will launch an {@link Activity} directly, telling the
             * platform that it can generate the appropriate transitions.
             * @param hintLaunchesActivity {@code true} if the content intent will launch
             * an activity and transitions should be generated, false otherwise.
             * @return this object for method chaining
             */
            public @NonNull WearableExtender setHintLaunchesActivity(
                    boolean hintLaunchesActivity) {
                setFlag(FLAG_HINT_LAUNCHES_ACTIVITY, hintLaunchesActivity);
                return this;
            }

            /**
             * Get a hint that this Action will launch an {@link Activity} directly, telling the
             * platform that it can generate the appropriate transitions
             * @return {@code true} if the content intent will launch an activity and transitions
             * should be generated, false otherwise. The default value is {@code false} if this was
             * never set.
             */
            public boolean getHintLaunchesActivity() {
                return (mFlags & FLAG_HINT_LAUNCHES_ACTIVITY) != 0;
            }

            /**
             * Set a hint that this Action should be displayed inline - i.e. it will have a visual
             * representation directly on the notification surface in addition to the expanded
             * Notification
             *
             * @param hintDisplayInline {@code true} if action should be displayed inline, false
             *        otherwise
             * @return this object for method chaining
             */
            public @NonNull WearableExtender setHintDisplayActionInline(
                    boolean hintDisplayInline) {
                setFlag(FLAG_HINT_DISPLAY_INLINE, hintDisplayInline);
                return this;
            }

            /**
             * Get a hint that this Action should be displayed inline - i.e. it should have a
             * visual representation directly on the notification surface in addition to the
             * expanded Notification
             *
             * @return {@code true} if the Action should be displayed inline, {@code false}
             *         otherwise. The default value is {@code false} if this was never set.
             */
            public boolean getHintDisplayActionInline() {
                return (mFlags & FLAG_HINT_DISPLAY_INLINE) != 0;
            }
        }

        /**
         * Provides meaning to an {@link Action} that hints at what the associated
         * {@link PendingIntent} will do. For example, an {@link Action} with a
         * {@link PendingIntent} that replies to a text message notification may have the
         * {@link #SEMANTIC_ACTION_REPLY} {@link SemanticAction} set within it.
         */
        @IntDef({
                SEMANTIC_ACTION_NONE,
                SEMANTIC_ACTION_REPLY,
                SEMANTIC_ACTION_MARK_AS_READ,
                SEMANTIC_ACTION_MARK_AS_UNREAD,
                SEMANTIC_ACTION_DELETE,
                SEMANTIC_ACTION_ARCHIVE,
                SEMANTIC_ACTION_MUTE,
                SEMANTIC_ACTION_UNMUTE,
                SEMANTIC_ACTION_THUMBS_UP,
                SEMANTIC_ACTION_THUMBS_DOWN,
                SEMANTIC_ACTION_CALL
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface SemanticAction {}
    }


    /**
     * Extender interface for use with {@link Builder#extend}. Extenders may be used to add
     * metadata or change options on a notification builder.
     */
    public interface Extender {
        /**
         * Apply this extender to a notification builder.
         * @param builder the builder to be modified.
         * @return the build object for chaining.
         */
        @NonNull Builder extend(@NonNull Builder builder);
    }

    /**
     * Helper class to add wearable extensions to notifications.
     * <p class="note"> See
     * <a href="{@docRoot}training/wearables/notifications">Creating Notifications
     * for Android Wear</a> for more information on how to use this class.
     * <p>
     * To create a notification with wearable extensions:
     * <ol>
     *   <li>Create a {@link NotificationCompat.Builder}, setting any desired
     *   properties.</li>
     *   <li>Create a {@link NotificationCompat.WearableExtender}.</li>
     *   <li>Set wearable-specific properties using the
     *   {@code add} and {@code set} methods of {@link NotificationCompat.WearableExtender}.</li>
     *   <li>Call {@link NotificationCompat.Builder#extend} to apply the extensions to a
     *   notification.</li>
     *   <li>Post the notification to the notification
     *   system with the {@code NotificationManagerCompat.notify(...)} methods
     *   and not the {@code NotificationManager.notify(...)} methods.</li>
     * </ol>
     *
     * <pre class="prettyprint">
     * Notification notification = new NotificationCompat.Builder(mContext)
     *         .setContentTitle(&quot;New mail from &quot; + sender.toString())
     *         .setContentText(subject)
     *         .setSmallIcon(R.drawable.new_mail)
     *         .extend(new NotificationCompat.WearableExtender()
     *                 .setContentIcon(R.drawable.new_mail))
     *         .build();
     * NotificationManagerCompat.from(mContext).notify(0, notification);</pre>
     *
     * <p>Wearable extensions can be accessed on an existing notification by using the
     * {@code WearableExtender(Notification)} constructor,
     * and then using the {@code get} methods to access values.
     *
     * <pre class="prettyprint">
     * NotificationCompat.WearableExtender wearableExtender =
     *         new NotificationCompat.WearableExtender(notification);
     * List&lt;Notification&gt; pages = wearableExtender.getPages();</pre>
     */
    public static final class WearableExtender implements Extender {
        /**
         * Sentinel value for an action index that is unset.
         */
        public static final int UNSET_ACTION_INDEX = -1;

        /**
         * Size value for use with {@link #setCustomSizePreset} to show this notification with
         * default sizing.
         * <p>For custom display notifications created using {@link #setDisplayIntent},
         * the default is {@link #SIZE_MEDIUM}. All other notifications size automatically based
         * on their content.
         *
         * @deprecated Display intents are no longer supported.
         */
        @Deprecated
        public static final int SIZE_DEFAULT = 0;

        /**
         * Size value for use with {@link #setCustomSizePreset} to show this notification
         * with an extra small size.
         * <p>This value is only applicable for custom display notifications created using
         * {@link #setDisplayIntent}.
         *
         * @deprecated Display intents are no longer supported.
         */
        @Deprecated
        public static final int SIZE_XSMALL = 1;

        /**
         * Size value for use with {@link #setCustomSizePreset} to show this notification
         * with a small size.
         * <p>This value is only applicable for custom display notifications created using
         * {@link #setDisplayIntent}.
         *
         * @deprecated Display intents are no longer supported.
         */
        @Deprecated
        public static final int SIZE_SMALL = 2;

        /**
         * Size value for use with {@link #setCustomSizePreset} to show this notification
         * with a medium size.
         * <p>This value is only applicable for custom display notifications created using
         * {@link #setDisplayIntent}.
         *
         * @deprecated Display intents are no longer supported.
         */
        @Deprecated
        public static final int SIZE_MEDIUM = 3;

        /**
         * Size value for use with {@link #setCustomSizePreset} to show this notification
         * with a large size.
         * <p>This value is only applicable for custom display notifications created using
         * {@link #setDisplayIntent}.
         *
         * @deprecated Display intents are no longer supported.
         */
        @Deprecated
        public static final int SIZE_LARGE = 4;

        /**
         * Size value for use with {@link #setCustomSizePreset} to show this notification
         * full screen.
         * <p>This value is only applicable for custom display notifications created using
         * {@link #setDisplayIntent}.
         *
         * @deprecated Display intents are no longer supported.
         */
        @Deprecated
        public static final int SIZE_FULL_SCREEN = 5;

        /**
         * Sentinel value for use with {@link #setHintScreenTimeout} to keep the screen on for a
         * short amount of time when this notification is displayed on the screen. This
         * is the default value.
         *
         * @deprecated This feature is no longer supported.
         */
        @Deprecated
        public static final int SCREEN_TIMEOUT_SHORT = 0;

        /**
         * Sentinel value for use with {@link #setHintScreenTimeout} to keep the screen on
         * for a longer amount of time when this notification is displayed on the screen.
         * @deprecated This feature is no longer supported.
         */
        @Deprecated
        public static final int SCREEN_TIMEOUT_LONG = -1;

        /** Notification extra which contains wearable extensions */
        private static final String EXTRA_WEARABLE_EXTENSIONS = "android.wearable.EXTENSIONS";

        // Keys within EXTRA_WEARABLE_EXTENSIONS for wearable options.
        private static final String KEY_ACTIONS = "actions";
        private static final String KEY_FLAGS = "flags";
        private static final String KEY_DISPLAY_INTENT = "displayIntent";
        private static final String KEY_PAGES = "pages";
        private static final String KEY_BACKGROUND = "background";
        private static final String KEY_CONTENT_ICON = "contentIcon";
        private static final String KEY_CONTENT_ICON_GRAVITY = "contentIconGravity";
        private static final String KEY_CONTENT_ACTION_INDEX = "contentActionIndex";
        private static final String KEY_CUSTOM_SIZE_PRESET = "customSizePreset";
        private static final String KEY_CUSTOM_CONTENT_HEIGHT = "customContentHeight";
        private static final String KEY_GRAVITY = "gravity";
        private static final String KEY_HINT_SCREEN_TIMEOUT = "hintScreenTimeout";
        private static final String KEY_DISMISSAL_ID = "dismissalId";
        private static final String KEY_BRIDGE_TAG = "bridgeTag";

        // Flags bitwise-ored to mFlags
        private static final int FLAG_CONTENT_INTENT_AVAILABLE_OFFLINE = 0x1;
        private static final int FLAG_HINT_HIDE_ICON = 1 << 1;
        private static final int FLAG_HINT_SHOW_BACKGROUND_ONLY = 1 << 2;
        private static final int FLAG_START_SCROLL_BOTTOM = 1 << 3;
        private static final int FLAG_HINT_AVOID_BACKGROUND_CLIPPING = 1 << 4;
        private static final int FLAG_BIG_PICTURE_AMBIENT = 1 << 5;
        private static final int FLAG_HINT_CONTENT_INTENT_LAUNCHES_ACTIVITY = 1 << 6;

        // Default value for flags integer
        private static final int DEFAULT_FLAGS = FLAG_CONTENT_INTENT_AVAILABLE_OFFLINE;

        private static final int DEFAULT_CONTENT_ICON_GRAVITY = GravityCompat.END;
        private static final int DEFAULT_GRAVITY = Gravity.BOTTOM;

        private ArrayList<Action> mActions = new ArrayList<>();
        private int mFlags = DEFAULT_FLAGS;
        private PendingIntent mDisplayIntent;
        private ArrayList<Notification> mPages = new ArrayList<>();
        private Bitmap mBackground;
        private int mContentIcon;
        private int mContentIconGravity = DEFAULT_CONTENT_ICON_GRAVITY;
        private int mContentActionIndex = UNSET_ACTION_INDEX;
        @SuppressWarnings("deprecation")
        private int mCustomSizePreset = SIZE_DEFAULT;
        private int mCustomContentHeight;
        private int mGravity = DEFAULT_GRAVITY;
        private int mHintScreenTimeout;
        private String mDismissalId;
        private String mBridgeTag;

        /**
         * Create a {@link NotificationCompat.WearableExtender} with default
         * options.
         */
        public WearableExtender() {
        }

        @SuppressWarnings("deprecation")
        public WearableExtender(@NonNull Notification notification) {
            Bundle extras = getExtras(notification);
            Bundle wearableBundle = extras != null ? extras.getBundle(EXTRA_WEARABLE_EXTENSIONS)
                    : null;
            if (wearableBundle != null) {
                final ArrayList<Parcelable> parcelables =
                        wearableBundle.getParcelableArrayList(KEY_ACTIONS);
                if (parcelables != null) {
                    Action[] actions = new Action[parcelables.size()];
                    for (int i = 0; i < actions.length; i++) {
                        actions[i] = getActionCompatFromAction(
                                (Notification.Action) parcelables.get(i));
                    }
                    Collections.addAll(mActions, (Action[]) actions);
                }

                mFlags = wearableBundle.getInt(KEY_FLAGS, DEFAULT_FLAGS);
                mDisplayIntent = wearableBundle.getParcelable(KEY_DISPLAY_INTENT);

                Notification[] pages = getNotificationArrayFromBundle(
                        wearableBundle, KEY_PAGES);
                if (pages != null) {
                    Collections.addAll(mPages, pages);
                }

                mBackground = wearableBundle.getParcelable(KEY_BACKGROUND);
                mContentIcon = wearableBundle.getInt(KEY_CONTENT_ICON);
                mContentIconGravity = wearableBundle.getInt(KEY_CONTENT_ICON_GRAVITY,
                        DEFAULT_CONTENT_ICON_GRAVITY);
                mContentActionIndex = wearableBundle.getInt(KEY_CONTENT_ACTION_INDEX,
                        UNSET_ACTION_INDEX);
                mCustomSizePreset = wearableBundle.getInt(KEY_CUSTOM_SIZE_PRESET,
                        SIZE_DEFAULT);
                mCustomContentHeight = wearableBundle.getInt(KEY_CUSTOM_CONTENT_HEIGHT);
                mGravity = wearableBundle.getInt(KEY_GRAVITY, DEFAULT_GRAVITY);
                mHintScreenTimeout = wearableBundle.getInt(KEY_HINT_SCREEN_TIMEOUT);
                mDismissalId = wearableBundle.getString(KEY_DISMISSAL_ID);
                mBridgeTag = wearableBundle.getString(KEY_BRIDGE_TAG);
            }
        }

        /**
         * Apply wearable extensions to a notification that is being built. This is typically
         * called by the {@link NotificationCompat.Builder#extend} method of
         * {@link NotificationCompat.Builder}.
         */
        @SuppressWarnings("deprecation")
        @Override
        public NotificationCompat.@NonNull Builder extend(
                NotificationCompat.@NonNull Builder builder) {
            Bundle wearableBundle = new Bundle();

            if (!mActions.isEmpty()) {
                ArrayList<Parcelable> parcelables = new ArrayList<>(mActions.size());
                for (Action action : mActions) {
                    parcelables.add(
                            WearableExtender.getActionFromActionCompat(action));
                }
                wearableBundle.putParcelableArrayList(KEY_ACTIONS, parcelables);
            }
            if (mFlags != DEFAULT_FLAGS) {
                wearableBundle.putInt(KEY_FLAGS, mFlags);
            }
            if (mDisplayIntent != null) {
                wearableBundle.putParcelable(KEY_DISPLAY_INTENT, mDisplayIntent);
            }
            if (!mPages.isEmpty()) {
                wearableBundle.putParcelableArray(KEY_PAGES, mPages.toArray(
                        new Notification[mPages.size()]));
            }
            if (mBackground != null) {
                wearableBundle.putParcelable(KEY_BACKGROUND, mBackground);
            }
            if (mContentIcon != 0) {
                wearableBundle.putInt(KEY_CONTENT_ICON, mContentIcon);
            }
            if (mContentIconGravity != DEFAULT_CONTENT_ICON_GRAVITY) {
                wearableBundle.putInt(KEY_CONTENT_ICON_GRAVITY, mContentIconGravity);
            }
            if (mContentActionIndex != UNSET_ACTION_INDEX) {
                wearableBundle.putInt(KEY_CONTENT_ACTION_INDEX,
                        mContentActionIndex);
            }
            if (mCustomSizePreset != SIZE_DEFAULT) {
                wearableBundle.putInt(KEY_CUSTOM_SIZE_PRESET, mCustomSizePreset);
            }
            if (mCustomContentHeight != 0) {
                wearableBundle.putInt(KEY_CUSTOM_CONTENT_HEIGHT, mCustomContentHeight);
            }
            if (mGravity != DEFAULT_GRAVITY) {
                wearableBundle.putInt(KEY_GRAVITY, mGravity);
            }
            if (mHintScreenTimeout != 0) {
                wearableBundle.putInt(KEY_HINT_SCREEN_TIMEOUT, mHintScreenTimeout);
            }
            if (mDismissalId != null) {
                wearableBundle.putString(KEY_DISMISSAL_ID, mDismissalId);
            }
            if (mBridgeTag != null) {
                wearableBundle.putString(KEY_BRIDGE_TAG, mBridgeTag);
            }

            builder.getExtras().putBundle(EXTRA_WEARABLE_EXTENSIONS, wearableBundle);
            return builder;
        }

        private static Notification.Action getActionFromActionCompat(Action actionCompat) {
            IconCompat iconCompat = actionCompat.getIconCompat();
            Notification.Action.Builder actionBuilder = new Notification.Action.Builder(
                    iconCompat == null ? null : iconCompat.toIcon(), actionCompat.getTitle(),
                    actionCompat.getActionIntent());
            Bundle actionExtras;
            if (actionCompat.getExtras() != null) {
                actionExtras = new Bundle(actionCompat.getExtras());
            } else {
                actionExtras = new Bundle();
            }
            actionExtras.putBoolean(NotificationCompatJellybean.EXTRA_ALLOW_GENERATED_REPLIES,
                    actionCompat.getAllowGeneratedReplies());
            if (Build.VERSION.SDK_INT >= 24) {
                Api24Impl.setAllowGeneratedReplies(actionBuilder,
                        actionCompat.getAllowGeneratedReplies());
            }
            if (Build.VERSION.SDK_INT >= 31) {
                Api31Impl.setAuthenticationRequired(actionBuilder,
                        actionCompat.isAuthenticationRequired());
            }
            actionBuilder.addExtras(actionExtras);
            RemoteInput[] remoteInputCompats = actionCompat.getRemoteInputs();
            if (remoteInputCompats != null) {
                android.app.RemoteInput[] remoteInputs = RemoteInput.fromCompat(remoteInputCompats);
                for (android.app.RemoteInput remoteInput : remoteInputs) {
                    actionBuilder.addRemoteInput(remoteInput);
                }
            }
            return actionBuilder.build();
        }

        @Override
        public @NonNull WearableExtender clone() {
            WearableExtender that = new WearableExtender();
            that.mActions = new ArrayList<>(this.mActions);
            that.mFlags = this.mFlags;
            that.mDisplayIntent = this.mDisplayIntent;
            that.mPages = new ArrayList<>(this.mPages);
            that.mBackground = this.mBackground;
            that.mContentIcon = this.mContentIcon;
            that.mContentIconGravity = this.mContentIconGravity;
            that.mContentActionIndex = this.mContentActionIndex;
            that.mCustomSizePreset = this.mCustomSizePreset;
            that.mCustomContentHeight = this.mCustomContentHeight;
            that.mGravity = this.mGravity;
            that.mHintScreenTimeout = this.mHintScreenTimeout;
            that.mDismissalId = this.mDismissalId;
            that.mBridgeTag = this.mBridgeTag;
            return that;
        }

        /**
         * Add a wearable action to this notification.
         *
         * <p>When wearable actions are added using this method, the set of actions that
         * show on a wearable device splits from devices that only show actions added
         * using {@link NotificationCompat.Builder#addAction}. This allows for customization
         * of which actions display on different devices.
         *
         * @param action the action to add to this notification
         * @return this object for method chaining
         * @see NotificationCompat.Action
         */
        public @NonNull WearableExtender addAction(@NonNull Action action) {
            mActions.add(action);
            return this;
        }

        /**
         * Adds wearable actions to this notification.
         *
         * <p>When wearable actions are added using this method, the set of actions that
         * show on a wearable device splits from devices that only show actions added
         * using {@link NotificationCompat.Builder#addAction}. This allows for customization
         * of which actions display on different devices.
         *
         * @param actions the actions to add to this notification
         * @return this object for method chaining
         * @see NotificationCompat.Action
         */
        public @NonNull WearableExtender addActions(@NonNull List<Action> actions) {
            mActions.addAll(actions);
            return this;
        }

        /**
         * Clear all wearable actions present on this builder.
         * @return this object for method chaining.
         * @see #addAction
         */
        public @NonNull WearableExtender clearActions() {
            mActions.clear();
            return this;
        }

        /**
         * Get the wearable actions present on this notification.
         */
        public @NonNull List<Action> getActions() {
            return mActions;
        }

        /**
         * Set an intent to launch inside of an activity view when displaying
         * this notification. The {@link PendingIntent} provided should be for an activity.
         *
         * <pre class="prettyprint">
         * Intent displayIntent = new Intent(context, MyDisplayActivity.class);
         * PendingIntent displayPendingIntent = PendingIntent.getActivity(context,
         *         0, displayIntent, PendingIntent.FLAG_UPDATE_CURRENT);
         * Notification notification = new NotificationCompat.Builder(context)
         *         .extend(new NotificationCompat.WearableExtender()
         *                 .setDisplayIntent(displayPendingIntent)
         *                 .setCustomSizePreset(NotificationCompat.WearableExtender.SIZE_MEDIUM))
         *         .build();</pre>
         *
         * <p>The activity to launch needs to allow embedding, must be exported, and
         * should have an empty task affinity. It is also recommended to use the device
         * default light theme.
         *
         * <p>Example AndroidManifest.xml entry:
         * <pre class="prettyprint">
         * &lt;activity android:name=&quot;com.example.MyDisplayActivity&quot;
         *     android:exported=&quot;true&quot;
         *     android:allowEmbedded=&quot;true&quot;
         *     android:taskAffinity=&quot;&quot;
         *     android:theme=&quot;@android:style/Theme.DeviceDefault.Light&quot; /&gt;</pre>
         *
         * @param intent the {@link PendingIntent} for an activity
         * @return this object for method chaining
         * @see NotificationCompat.WearableExtender#getDisplayIntent
         * @deprecated Display intents are no longer supported.
         */
        @Deprecated
        public @NonNull WearableExtender setDisplayIntent(@Nullable PendingIntent intent) {
            mDisplayIntent = intent;
            return this;
        }

        /**
         * Get the intent to launch inside of an activity view when displaying this
         * notification. This {@code PendingIntent} should be for an activity.
         *
         * @deprecated Display intents are no longer supported.
         */
        @Deprecated
        public @Nullable PendingIntent getDisplayIntent() {
            return mDisplayIntent;
        }

        /**
         * Add an additional page of content to display with this notification. The current
         * notification forms the first page, and pages added using this function form
         * subsequent pages. This field can be used to separate a notification into multiple
         * sections.
         *
         * @param page the notification to add as another page
         * @return this object for method chaining
         * @see NotificationCompat.WearableExtender#getPages
         * @deprecated Multiple content pages are no longer supported.
         */
        @Deprecated
        public @NonNull WearableExtender addPage(@NonNull Notification page) {
            mPages.add(page);
            return this;
        }

        /**
         * Add additional pages of content to display with this notification. The current
         * notification forms the first page, and pages added using this function form
         * subsequent pages. This field can be used to separate a notification into multiple
         * sections.
         *
         * @param pages a list of notifications
         * @return this object for method chaining
         * @see NotificationCompat.WearableExtender#getPages
         * @deprecated Multiple content pages are no longer supported.
         */
        @Deprecated
        public @NonNull WearableExtender addPages(@NonNull List<Notification> pages) {
            mPages.addAll(pages);
            return this;
        }

        /**
         * Clear all additional pages present on this builder.
         * @return this object for method chaining.
         * @see #addPage
         * @deprecated Multiple content pages are no longer supported.
         */
        @Deprecated
        public @NonNull WearableExtender clearPages() {
            mPages.clear();
            return this;
        }

        /**
         * Get the array of additional pages of content for displaying this notification. The
         * current notification forms the first page, and elements within this array form
         * subsequent pages. This field can be used to separate a notification into multiple
         * sections.
         * @return the pages for this notification
         * @deprecated Multiple content pages are no longer supported.
         */
        @Deprecated
        public @NonNull List<Notification> getPages() {
            return mPages;
        }

        /**
         * Set a background image to be displayed behind the notification content.
         * Contrary to the {@link NotificationCompat.BigPictureStyle}, this background
         * will work with any notification style.
         *
         * @param background the background bitmap
         * @return this object for method chaining
         * @see NotificationCompat.WearableExtender#getBackground
         * @deprecated Background images are no longer supported.
         */
        @Deprecated
        public @NonNull WearableExtender setBackground(@Nullable Bitmap background) {
            mBackground = background;
            return this;
        }

        /**
         * Get a background image to be displayed behind the notification content.
         * Contrary to the {@link NotificationCompat.BigPictureStyle}, this background
         * will work with any notification style.
         *
         * @return the background image
         * @see NotificationCompat.WearableExtender#setBackground
         * @deprecated Background images are no longer supported.
         */
        @Deprecated
        public @Nullable Bitmap getBackground() {
            return mBackground;
        }

        /**
         * Set an icon that goes with the content of this notification.
         *
         * @deprecated This method has no effect starting with Wear 2.0.
         */
        @Deprecated
        public @NonNull WearableExtender setContentIcon(int icon) {
            mContentIcon = icon;
            return this;
        }

        /**
         * Get an icon that goes with the content of this notification.
         *
         * @deprecated This method has no effect starting with Wear 2.0.
         */
        @Deprecated
        public int getContentIcon() {
            return mContentIcon;
        }

        /**
         * Set the gravity that the content icon should have within the notification display.
         * Supported values include {@link android.view.Gravity#START} and
         * {@link android.view.Gravity#END}. The default value is {@link android.view.Gravity#END}.
         * @see #setContentIcon
         *
         * @deprecated This method has no effect starting with Wear 2.0.
         */
        @Deprecated
        public @NonNull WearableExtender setContentIconGravity(int contentIconGravity) {
            mContentIconGravity = contentIconGravity;
            return this;
        }

        /**
         * Get the gravity that the content icon should have within the notification display.
         * Supported values include {@link android.view.Gravity#START} and
         * {@link android.view.Gravity#END}. The default value is {@link android.view.Gravity#END}.
         * @see #getContentIcon
         *
         * @deprecated This method has no effect starting with Wear 2.0.
         */
        @Deprecated
        public int getContentIconGravity() {
            return mContentIconGravity;
        }

        /**
         * Set an action from this notification's actions as the primary action. If the action has a
         * {@link RemoteInput} associated with it, shortcuts to the options for that input are shown
         * directly on the notification.
         *
         * @param actionIndex The index of the primary action.
         *                    If wearable actions were added to the main notification, this index
         *                    will apply to that list, otherwise it will apply to the regular
         *                    actions list.
         */
        public @NonNull WearableExtender setContentAction(int actionIndex) {
            mContentActionIndex = actionIndex;
            return this;
        }

        /**
         * Get the index of the notification action, if any, that was specified as the primary
         * action.
         *
         * <p>If wearable specific actions were added to the main notification, this index will
         * apply to that list, otherwise it will apply to the regular actions list.
         *
         * @return the action index or {@link #UNSET_ACTION_INDEX} if no action was selected.
         */
        public int getContentAction() {
            return mContentActionIndex;
        }

        /**
         * Set the gravity that this notification should have within the available viewport space.
         * Supported values include {@link android.view.Gravity#TOP},
         * {@link android.view.Gravity#CENTER_VERTICAL} and {@link android.view.Gravity#BOTTOM}.
         * The default value is {@link android.view.Gravity#BOTTOM}.
         *
         * @deprecated This method has no effect starting with Wear 2.0.
         */
        @Deprecated
        public @NonNull WearableExtender setGravity(int gravity) {
            mGravity = gravity;
            return this;
        }

        /**
         * Get the gravity that this notification should have within the available viewport space.
         * Supported values include {@link android.view.Gravity#TOP},
         * {@link android.view.Gravity#CENTER_VERTICAL} and {@link android.view.Gravity#BOTTOM}.
         * The default value is {@link android.view.Gravity#BOTTOM}.
         *
         * @deprecated This method has no effect starting with Wear 2.0.
         */
        @Deprecated
        public int getGravity() {
            return mGravity;
        }

        /**
         * Set the custom size preset for the display of this notification out of the available
         * presets found in {@link NotificationCompat.WearableExtender}, e.g.
         * {@link #SIZE_LARGE}.
         * <p>Some custom size presets are only applicable for custom display notifications created
         * using {@link NotificationCompat.WearableExtender#setDisplayIntent}. Check the
         * documentation for the preset in question. See also
         * {@link #setCustomContentHeight} and {@link #getCustomSizePreset}.
         *
         * @deprecated This method has no effect starting with Wear 2.0.
         */
        @Deprecated
        public @NonNull WearableExtender setCustomSizePreset(int sizePreset) {
            mCustomSizePreset = sizePreset;
            return this;
        }

        /**
         * Get the custom size preset for the display of this notification out of the available
         * presets found in {@link NotificationCompat.WearableExtender}, e.g.
         * {@link #SIZE_LARGE}.
         * <p>Some custom size presets are only applicable for custom display notifications created
         * using {@link #setDisplayIntent}. Check the documentation for the preset in question.
         * See also {@link #setCustomContentHeight} and {@link #setCustomSizePreset}.
         *
         * @deprecated This method has no effect starting with Wear 2.0.
         */
        @Deprecated
        public int getCustomSizePreset() {
            return mCustomSizePreset;
        }

        /**
         * Set the custom height in pixels for the display of this notification's content.
         * <p>This option is only available for custom display notifications created
         * using {@link NotificationCompat.WearableExtender#setDisplayIntent}. See also
         * {@link NotificationCompat.WearableExtender#setCustomSizePreset} and
         * {@link #getCustomContentHeight}.
         *
         * @deprecated This method has no effect starting with Wear 2.0.
         */
        @Deprecated
        public @NonNull WearableExtender setCustomContentHeight(int height) {
            mCustomContentHeight = height;
            return this;
        }

        /**
         * Get the custom height in pixels for the display of this notification's content.
         * <p>This option is only available for custom display notifications created
         * using {@link #setDisplayIntent}. See also {@link #setCustomSizePreset} and
         * {@link #setCustomContentHeight}.
         *
         * @deprecated This method has no effect starting with Wear 2.0.
         */
        @Deprecated
        public int getCustomContentHeight() {
            return mCustomContentHeight;
        }

        /**
         * Set whether the scrolling position for the contents of this notification should start
         * at the bottom of the contents instead of the top when the contents are too long to
         * display within the screen.  Default is false (start scroll at the top).
         */
        public @NonNull WearableExtender setStartScrollBottom(boolean startScrollBottom) {
            setFlag(FLAG_START_SCROLL_BOTTOM, startScrollBottom);
            return this;
        }

        /**
         * Get whether the scrolling position for the contents of this notification should start
         * at the bottom of the contents instead of the top when the contents are too long to
         * display within the screen. Default is false (start scroll at the top).
         */
        public boolean getStartScrollBottom() {
            return (mFlags & FLAG_START_SCROLL_BOTTOM) != 0;
        }

        /**
         * Set whether the content intent is available when the wearable device is not connected
         * to a companion device.  The user can still trigger this intent when the wearable device
         * is offline, but a visual hint will indicate that the content intent may not be available.
         * Defaults to true.
         */
        public @NonNull WearableExtender setContentIntentAvailableOffline(
                boolean contentIntentAvailableOffline) {
            setFlag(FLAG_CONTENT_INTENT_AVAILABLE_OFFLINE, contentIntentAvailableOffline);
            return this;
        }

        /**
         * Get whether the content intent is available when the wearable device is not connected
         * to a companion device.  The user can still trigger this intent when the wearable device
         * is offline, but a visual hint will indicate that the content intent may not be available.
         * Defaults to true.
         */
        public boolean getContentIntentAvailableOffline() {
            return (mFlags & FLAG_CONTENT_INTENT_AVAILABLE_OFFLINE) != 0;
        }

        /**
         * Set a hint that this notification's icon should not be displayed.
         * @param hintHideIcon {@code true} to hide the icon, {@code false} otherwise.
         * @return this object for method chaining
         *
         * @deprecated This method has no effect starting with Wear 2.0.
         */
        @Deprecated
        public @NonNull WearableExtender setHintHideIcon(boolean hintHideIcon) {
            setFlag(FLAG_HINT_HIDE_ICON, hintHideIcon);
            return this;
        }

        /**
         * Get a hint that this notification's icon should not be displayed.
         * @return {@code true} if this icon should not be displayed, false otherwise.
         * The default value is {@code false} if this was never set.
         *
         * @deprecated This method has no effect starting with Wear 2.0.
         */
        @Deprecated
        public boolean getHintHideIcon() {
            return (mFlags & FLAG_HINT_HIDE_ICON) != 0;
        }

        /**
         * Set a visual hint that only the background image of this notification should be
         * displayed, and other semantic content should be hidden. This hint is only applicable
         * to sub-pages added using {@link #addPage}.
         *
         * @deprecated This method has no effect starting with Wear 2.0.
         */
        @Deprecated
        public @NonNull WearableExtender setHintShowBackgroundOnly(boolean hintShowBackgroundOnly) {
            setFlag(FLAG_HINT_SHOW_BACKGROUND_ONLY, hintShowBackgroundOnly);
            return this;
        }

        /**
         * Get a visual hint that only the background image of this notification should be
         * displayed, and other semantic content should be hidden. This hint is only applicable
         * to sub-pages added using {@link NotificationCompat.WearableExtender#addPage}.
         *
         * @deprecated This method has no effect starting with Wear 2.0.
         */
        @Deprecated
        public boolean getHintShowBackgroundOnly() {
            return (mFlags & FLAG_HINT_SHOW_BACKGROUND_ONLY) != 0;
        }

        /**
         * Set a hint that this notification's background should not be clipped if possible,
         * and should instead be resized to fully display on the screen, retaining the aspect
         * ratio of the image. This can be useful for images like barcodes or qr codes.
         * @param hintAvoidBackgroundClipping {@code true} to avoid clipping if possible.
         * @return this object for method chaining
         *
         * @deprecated This method has no effect starting with Wear 2.0.
         */
        @Deprecated
        public @NonNull WearableExtender setHintAvoidBackgroundClipping(
                boolean hintAvoidBackgroundClipping) {
            setFlag(FLAG_HINT_AVOID_BACKGROUND_CLIPPING, hintAvoidBackgroundClipping);
            return this;
        }

        /**
         * Get a hint that this notification's background should not be clipped if possible,
         * and should instead be resized to fully display on the screen, retaining the aspect
         * ratio of the image. This can be useful for images like barcodes or qr codes.
         * @return {@code true} if it's ok if the background is clipped on the screen, false
         * otherwise. The default value is {@code false} if this was never set.
         *
         * @deprecated This method has no effect starting with Wear 2.0.
         */
        @Deprecated
        public boolean getHintAvoidBackgroundClipping() {
            return (mFlags & FLAG_HINT_AVOID_BACKGROUND_CLIPPING) != 0;
        }

        /**
         * Set a hint that the screen should remain on for at least this duration when
         * this notification is displayed on the screen.
         * @param timeout The requested screen timeout in milliseconds. Can also be either
         *     {@link #SCREEN_TIMEOUT_SHORT} or {@link #SCREEN_TIMEOUT_LONG}.
         * @return this object for method chaining
         *
         * @deprecated This method has no effect.
         */
        @Deprecated
        public @NonNull WearableExtender setHintScreenTimeout(int timeout) {
            mHintScreenTimeout = timeout;
            return this;
        }

        /**
         * Get the duration, in milliseconds, that the screen should remain on for
         * when this notification is displayed.
         * @return the duration in milliseconds if > 0, or either one of the sentinel values
         *     {@link #SCREEN_TIMEOUT_SHORT} or {@link #SCREEN_TIMEOUT_LONG}.
         *
         * @deprecated This method has no effect starting with Wear 2.0.
         */
        @Deprecated
        public int getHintScreenTimeout() {
            return mHintScreenTimeout;
        }

        /**
         * Set a hint that this notification's {@link BigPictureStyle} (if present) should be
         * converted to low-bit and displayed in ambient mode, especially useful for barcodes and
         * qr codes, as well as other simple black-and-white tickets.
         * @param hintAmbientBigPicture {@code true} to enable converstion and ambient.
         * @return this object for method chaining
         * @deprecated This feature is no longer supported.
         */
        @Deprecated
        public @NonNull WearableExtender setHintAmbientBigPicture(boolean hintAmbientBigPicture) {
            setFlag(FLAG_BIG_PICTURE_AMBIENT, hintAmbientBigPicture);
            return this;
        }

        /**
         * Get a hint that this notification's {@link BigPictureStyle} (if present) should be
         * converted to low-bit and displayed in ambient mode, especially useful for barcodes and
         * qr codes, as well as other simple black-and-white tickets.
         * @return {@code true} if it should be displayed in ambient, false otherwise
         * otherwise. The default value is {@code false} if this was never set.
         * @deprecated This feature is no longer supported.
         */
        @Deprecated
        public boolean getHintAmbientBigPicture() {
            return (mFlags & FLAG_BIG_PICTURE_AMBIENT) != 0;
        }

        /**
         * Set a hint that this notification's content intent will launch an {@link Activity}
         * directly, telling the platform that it can generate the appropriate transitions.
         * @param hintContentIntentLaunchesActivity {@code true} if the content intent will launch
         * an activity and transitions should be generated, false otherwise.
         * @return this object for method chaining
         */
        public @NonNull WearableExtender setHintContentIntentLaunchesActivity(
                boolean hintContentIntentLaunchesActivity) {
            setFlag(FLAG_HINT_CONTENT_INTENT_LAUNCHES_ACTIVITY, hintContentIntentLaunchesActivity);
            return this;
        }

        /**
         * Get a hint that this notification's content intent will launch an {@link Activity}
         * directly, telling the platform that it can generate the appropriate transitions
         * @return {@code true} if the content intent will launch an activity and transitions should
         * be generated, false otherwise. The default value is {@code false} if this was never set.
         */
        public boolean getHintContentIntentLaunchesActivity() {
            return (mFlags & FLAG_HINT_CONTENT_INTENT_LAUNCHES_ACTIVITY) != 0;
        }

        /**
         * Sets the dismissal id for this notification. If a notification is posted with a
         * dismissal id, then when that notification is canceled, notifications on other wearables
         * and the paired Android phone having that same dismissal id will also be canceled. See
         * <a href="{@docRoot}training/wearables/notifications/bridger">Adding Wearable Features to
         * Notifications</a> for more information.
         * @param dismissalId the dismissal id of the notification.
         * @return this object for method chaining
         */
        public @NonNull WearableExtender setDismissalId(@Nullable String dismissalId) {
            mDismissalId = dismissalId;
            return this;
        }

        /**
         * Returns the dismissal id of the notification.
         * @return the dismissal id of the notification or null if it has not been set.
         */
        public @Nullable String getDismissalId() {
            return mDismissalId;
        }

        /**
         * Sets a bridge tag for this notification. A bridge tag can be set for notifications
         * posted from a phone to provide finer-grained control on what notifications are bridged
         * to wearables. See <a href="{@docRoot}training/wearables/notifications/bridger">Adding
         * Wearable Features to Notifications</a> for more information.
         * @param bridgeTag the bridge tag of the notification.
         * @return this object for method chaining
         */
        public @NonNull WearableExtender setBridgeTag(@Nullable String bridgeTag) {
            mBridgeTag = bridgeTag;
            return this;
        }

        /**
         * Returns the bridge tag of the notification.
         * @return the bridge tag or null if not present.
         */
        public @Nullable String getBridgeTag() {
            return mBridgeTag;
        }

        private void setFlag(int mask, boolean value) {
            if (value) {
                mFlags |= mask;
            } else {
                mFlags &= ~mask;
            }
        }

        /**
         * A class for wrapping calls to {@link Notification.WearableExtender} methods which
         * were added in API 24; these calls must be wrapped to avoid performance issues.
         * See the UnsafeNewApiCall lint rule for more details.
         */
        @RequiresApi(24)
        static class Api24Impl {
            private Api24Impl() { }

            static Notification.Action.Builder setAllowGeneratedReplies(
                    Notification.Action.Builder builder, boolean allowGeneratedReplies) {
                return builder.setAllowGeneratedReplies(allowGeneratedReplies);
            }
        }

        /**
         * A class for wrapping calls to {@link Notification.WearableExtender} methods which
         * were added in API 31; these calls must be wrapped to avoid performance issues.
         * See the UnsafeNewApiCall lint rule for more details.
         */
        @RequiresApi(31)
        static class Api31Impl {
            private Api31Impl() { }

            static Notification.Action.Builder setAuthenticationRequired(
                    Notification.Action.Builder builder, boolean authenticationRequired) {
                return builder.setAuthenticationRequired(authenticationRequired);
            }
        }
    }

    /**
     * <p>Helper class to add Android Auto extensions to notifications. To create a notification
     * with car extensions:
     *
     * <ol>
     *  <li>Create an {@link NotificationCompat.Builder}, setting any desired
     *  properties.</li>
     *  <li>Create a {@link CarExtender}.</li>
     *  <li>Set car-specific properties using the {@code add} and {@code set} methods of
     *  {@link CarExtender}.</li>
     *  <li>Call {@link androidx.core.app.NotificationCompat.Builder#extend(NotificationCompat.Extender)}
     *  to apply the extensions to a notification.</li>
     *  <li>Post the notification to the notification system with the
     *  {@code NotificationManagerCompat.notify(...)} methods and not the
     *  {@code NotificationManager.notify(...)} methods.</li>
     * </ol>
     *
     * <pre class="prettyprint">
     * Notification notification = new NotificationCompat.Builder(context)
     *         ...
     *         .extend(new CarExtender()
     *                 .set*(...))
     *         .build();
     * </pre>
     *
     * <p>Car extensions can be accessed on an existing notification by using the
     * {@code CarExtender(Notification)} constructor, and then using the {@code get} methods
     * to access values.
     */
    public static final class CarExtender implements Extender {
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        static final String EXTRA_CAR_EXTENDER = "android.car.EXTENSIONS";
        private static final String EXTRA_LARGE_ICON = "large_icon";
        private static final String EXTRA_CONVERSATION = "car_conversation";
        private static final String EXTRA_COLOR = "app_color";
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        static final String EXTRA_INVISIBLE_ACTIONS = "invisible_actions";

        private static final String KEY_AUTHOR = "author";
        private static final String KEY_TEXT = "text";
        private static final String KEY_MESSAGES = "messages";
        private static final String KEY_REMOTE_INPUT = "remote_input";
        private static final String KEY_ON_REPLY = "on_reply";
        private static final String KEY_ON_READ = "on_read";
        private static final String KEY_PARTICIPANTS = "participants";
        private static final String KEY_TIMESTAMP = "timestamp";

        private Bitmap mLargeIcon;
        private UnreadConversation mUnreadConversation;
        private int mColor = NotificationCompat.COLOR_DEFAULT;

        /**
         * Create a {@link CarExtender} with default options.
         */
        public CarExtender() {
        }

        /**
         * Create a {@link CarExtender} from the CarExtender options of an existing Notification.
         *
         * @param notification The notification from which to copy options.
         */
        @SuppressWarnings("deprecation")
        public CarExtender(@NonNull Notification notification) {
            Bundle carBundle = getExtras(notification) == null
                    ? null : getExtras(notification).getBundle(EXTRA_CAR_EXTENDER);
            if (carBundle != null) {
                mLargeIcon = carBundle.getParcelable(EXTRA_LARGE_ICON);
                mColor = carBundle.getInt(EXTRA_COLOR, NotificationCompat.COLOR_DEFAULT);

                Bundle b = carBundle.getBundle(EXTRA_CONVERSATION);
                mUnreadConversation = getUnreadConversationFromBundle(b);
            }
        }

        @SuppressWarnings("deprecation")
        private static UnreadConversation getUnreadConversationFromBundle(@Nullable Bundle b) {
            if (b == null) {
                return null;
            }
            Parcelable[] parcelableMessages = b.getParcelableArray(KEY_MESSAGES);
            String[] messages = null;
            if (parcelableMessages != null) {
                String[] tmp = new String[parcelableMessages.length];
                boolean success = true;
                for (int i = 0; i < tmp.length; i++) {
                    if (!(parcelableMessages[i] instanceof Bundle)) {
                        success = false;
                        break;
                    }
                    tmp[i] = ((Bundle) parcelableMessages[i]).getString(KEY_TEXT);
                    if (tmp[i] == null) {
                        success = false;
                        break;
                    }
                }
                if (success) {
                    messages = tmp;
                } else {
                    return null;
                }
            }

            PendingIntent onRead = b.getParcelable(KEY_ON_READ);
            PendingIntent onReply = b.getParcelable(KEY_ON_REPLY);

            android.app.RemoteInput remoteInput = b.getParcelable(KEY_REMOTE_INPUT);

            String[] participants = b.getStringArray(KEY_PARTICIPANTS);
            if (participants == null || participants.length != 1) {
                return null;
            }

            RemoteInput remoteInputCompat = remoteInput != null
                    ? new RemoteInput(remoteInput.getResultKey(),
                    remoteInput.getLabel(),
                    remoteInput.getChoices(),
                    remoteInput.getAllowFreeFormInput(),
                    Build.VERSION.SDK_INT >= 29
                            ? Api29Impl.getEditChoicesBeforeSending(remoteInput)
                            : RemoteInput.EDIT_CHOICES_BEFORE_SENDING_AUTO,
                    remoteInput.getExtras(),
                    null /* allowedDataTypes */)
                    : null;

            return new UnreadConversation(messages, remoteInputCompat, onReply,
                    onRead, participants, b.getLong(KEY_TIMESTAMP));
        }

        private static Bundle getBundleForUnreadConversation(@NonNull UnreadConversation uc) {
            Bundle b = new Bundle();
            String author = null;
            if (uc.getParticipants() != null && uc.getParticipants().length > 1) {
                author = uc.getParticipants()[0];
            }
            Parcelable[] messages = new Parcelable[uc.getMessages().length];
            for (int i = 0; i < messages.length; i++) {
                Bundle m = new Bundle();
                m.putString(KEY_TEXT, uc.getMessages()[i]);
                m.putString(KEY_AUTHOR, author);
                messages[i] = m;
            }
            b.putParcelableArray(KEY_MESSAGES, messages);
            RemoteInput remoteInputCompat = uc.getRemoteInput();
            if (remoteInputCompat != null) {
                android.app.RemoteInput.Builder builder = new android.app.RemoteInput.Builder(
                        remoteInputCompat.getResultKey());
                builder.setLabel(remoteInputCompat.getLabel());
                builder.setChoices(remoteInputCompat.getChoices());
                builder.setAllowFreeFormInput(remoteInputCompat.getAllowFreeFormInput());
                builder.addExtras(remoteInputCompat.getExtras());

                android.app.RemoteInput remoteInput = builder.build();
                b.putParcelable(KEY_REMOTE_INPUT, remoteInput);
            }
            b.putParcelable(KEY_ON_REPLY, uc.getReplyPendingIntent());
            b.putParcelable(KEY_ON_READ, uc.getReadPendingIntent());
            b.putStringArray(KEY_PARTICIPANTS, uc.getParticipants());
            b.putLong(KEY_TIMESTAMP, uc.getLatestTimestamp());
            return b;
        }

        /**
         * Apply car extensions to a notification that is being built. This is typically called by
         * the {@link androidx.core.app.NotificationCompat.Builder#extend(NotificationCompat.Extender)}
         * method of {@link NotificationCompat.Builder}.
         */
        @Override
        public NotificationCompat.@NonNull Builder extend(
                NotificationCompat.@NonNull Builder builder) {
            Bundle carExtensions = new Bundle();

            if (mLargeIcon != null) {
                carExtensions.putParcelable(EXTRA_LARGE_ICON, mLargeIcon);
            }
            if (mColor != NotificationCompat.COLOR_DEFAULT) {
                carExtensions.putInt(EXTRA_COLOR, mColor);
            }

            if (mUnreadConversation != null) {
                Bundle b = getBundleForUnreadConversation(mUnreadConversation);
                carExtensions.putBundle(EXTRA_CONVERSATION, b);
            }

            builder.getExtras().putBundle(EXTRA_CAR_EXTENDER, carExtensions);
            return builder;
        }

        /**
         * Sets the accent color to use when Android Auto presents the notification.
         *
         * Android Auto uses the color set with {@link androidx.core.app.NotificationCompat.Builder#setColor(int)}
         * to accent the displayed notification. However, not all colors are acceptable in an
         * automotive setting. This method can be used to override the color provided in the
         * notification in such a situation.
         */
        public @NonNull CarExtender setColor(@ColorInt int color) {
            mColor = color;
            return this;
        }

        /**
         * Gets the accent color.
         *
         * @see #setColor
         */
        @ColorInt
        public int getColor() {
            return mColor;
        }

        /**
         * Sets the large icon of the car notification.
         *
         * If no large icon is set in the extender, Android Auto will display the icon
         * specified by {@link androidx.core.app.NotificationCompat.Builder#setLargeIcon(android.graphics.Bitmap)}
         *
         * @param largeIcon The large icon to use in the car notification.
         * @return This object for method chaining.
         */
        public @NonNull CarExtender setLargeIcon(@Nullable Bitmap largeIcon) {
            mLargeIcon = largeIcon;
            return this;
        }

        /**
         * Gets the large icon used in this car notification, or null if no icon has been set.
         *
         * @return The large icon for the car notification.
         * @see CarExtender#setLargeIcon
         */
        public @Nullable Bitmap getLargeIcon() {
            return mLargeIcon;
        }

        /**
         * Sets the unread conversation in a message notification.
         *
         * @param unreadConversation The unread part of the conversation this notification conveys.
         * @return This object for method chaining.
         *
         * @deprecated {@link UnreadConversation} is no longer supported. Use {@link MessagingStyle}
         * instead.
         */
        @Deprecated
        public @NonNull CarExtender setUnreadConversation(
                @Nullable UnreadConversation unreadConversation) {
            mUnreadConversation = unreadConversation;
            return this;
        }

        /**
         * Returns the unread conversation conveyed by this notification.
         * @see #setUnreadConversation(UnreadConversation)
         *
         * @deprecated {@link UnreadConversation} is no longer supported. Use {@link MessagingStyle}
         * instead.
         */
        @Deprecated
        public @Nullable UnreadConversation getUnreadConversation() {
            return mUnreadConversation;
        }

        /**
         * A class which holds the unread messages from a conversation.
         *
         * @deprecated {@link UnreadConversation} is no longer supported. Use {@link MessagingStyle}
         * instead.
         */
        @Deprecated
        public static class UnreadConversation {
            private final String[] mMessages;
            private final RemoteInput mRemoteInput;
            private final PendingIntent mReplyPendingIntent;
            private final PendingIntent mReadPendingIntent;
            private final String[] mParticipants;
            private final long mLatestTimestamp;

            UnreadConversation(String @Nullable [] messages, @Nullable RemoteInput remoteInput,
                    @Nullable PendingIntent replyPendingIntent,
                    @Nullable PendingIntent readPendingIntent,
                    String @Nullable [] participants, long latestTimestamp) {
                mMessages = messages;
                mRemoteInput = remoteInput;
                mReadPendingIntent = readPendingIntent;
                mReplyPendingIntent = replyPendingIntent;
                mParticipants = participants;
                mLatestTimestamp = latestTimestamp;
            }

            /**
             * Gets the list of messages conveyed by this notification.
             */
            public String @Nullable [] getMessages() {
                return mMessages;
            }

            /**
             * Gets the remote input that will be used to convey the response to a message list, or
             * null if no such remote input exists.
             */
            public @Nullable RemoteInput getRemoteInput() {
                return mRemoteInput;
            }

            /**
             * Gets the pending intent that will be triggered when the user replies to this
             * notification.
             */
            public @Nullable PendingIntent getReplyPendingIntent() {
                return mReplyPendingIntent;
            }

            /**
             * Gets the pending intent that Android Auto will send after it reads aloud all messages
             * in this object's message list.
             */
            public @Nullable PendingIntent getReadPendingIntent() {
                return mReadPendingIntent;
            }

            /**
             * Gets the participants in the conversation.
             */
            public String @Nullable [] getParticipants() {
                return mParticipants;
            }

            /**
             * Gets the firs participant in the conversation.
             */
            public @Nullable String getParticipant() {
                return mParticipants.length > 0 ? mParticipants[0] : null;
            }

            /**
             * Gets the timestamp of the conversation.
             */
            public long getLatestTimestamp() {
                return mLatestTimestamp;
            }

            /**
             * Builder class for {@link CarExtender.UnreadConversation} objects.
             */
            public static class Builder {
                private final List<String> mMessages = new ArrayList<>();
                private final String mParticipant;
                private RemoteInput mRemoteInput;
                private PendingIntent mReadPendingIntent;
                private PendingIntent mReplyPendingIntent;
                private long mLatestTimestamp;

                /**
                 * Constructs a new builder for {@link CarExtender.UnreadConversation}.
                 *
                 * @param name The name of the other participant in the conversation.
                 */
                public Builder(@NonNull String name) {
                    mParticipant = name;
                }

                /**
                 * Appends a new unread message to the list of messages for this conversation.
                 *
                 * The messages should be added from oldest to newest.
                 *
                 * @param message The text of the new unread message.
                 * @return This object for method chaining.
                 */
                public @NonNull Builder addMessage(@Nullable String message) {
                    if (message != null) {
                        mMessages.add(message);
                    }
                    return this;
                }

                /**
                 * Sets the pending intent and remote input which will convey the reply to this
                 * notification.
                 *
                 * @param pendingIntent The pending intent which will be triggered on a reply.
                 * @param remoteInput The remote input parcelable which will carry the reply.
                 * @return This object for method chaining.
                 *
                 * @see CarExtender.UnreadConversation#getRemoteInput
                 * @see CarExtender.UnreadConversation#getReplyPendingIntent
                 */
                public @NonNull Builder setReplyAction(@Nullable PendingIntent pendingIntent,
                        @Nullable RemoteInput remoteInput) {
                    mRemoteInput = remoteInput;
                    mReplyPendingIntent = pendingIntent;

                    return this;
                }

                /**
                 * Sets the pending intent that will be sent once the messages in this notification
                 * are read.
                 *
                 * @param pendingIntent The pending intent to use.
                 * @return This object for method chaining.
                 */
                public @NonNull Builder setReadPendingIntent(
                        @Nullable PendingIntent pendingIntent) {
                    mReadPendingIntent = pendingIntent;
                    return this;
                }

                /**
                 * Sets the timestamp of the most recent message in an unread conversation.
                 *
                 * If a messaging notification has been posted by your application and has not
                 * yet been cancelled, posting a later notification with the same id and tag
                 * but without a newer timestamp may result in Android Auto not displaying a
                 * heads up notification for the later notification.
                 *
                 * @param timestamp The timestamp of the most recent message in the conversation.
                 * @return This object for method chaining.
                 */
                public @NonNull Builder setLatestTimestamp(long timestamp) {
                    mLatestTimestamp = timestamp;
                    return this;
                }

                /**
                 * Builds a new unread conversation object.
                 *
                 * @return The new unread conversation object.
                 */
                public @NonNull UnreadConversation build() {
                    String[] messages = mMessages.toArray(new String[mMessages.size()]);
                    String[] participants = { mParticipant };
                    return new UnreadConversation(messages, mRemoteInput, mReplyPendingIntent,
                            mReadPendingIntent, participants, mLatestTimestamp);
                }
            }
        }

        /**
         * A class for wrapping calls to {@link Notification.CarExtender} methods which
         * were added in API 29; these calls must be wrapped to avoid performance issues.
         * See the UnsafeNewApiCall lint rule for more details.
         */
        @RequiresApi(29)
        static class Api29Impl {
            private Api29Impl() { }

            static int getEditChoicesBeforeSending(android.app.RemoteInput remoteInput) {
                return remoteInput.getEditChoicesBeforeSending();
            }

        }
    }

    /**
     * Helper class to add Android TV extensions to notifications.
     * <p>
     * To create a notification with a TV extension:
     * <ol>
     *  <li>Create an {@link NotificationCompat.Builder}, setting any desired properties.
     *  <li>Create a {@link TvExtender}.
     *  <li>Set TV-specific properties using the {@code set} methods of
     *  {@link TvExtender}.
     *  <li>Call {@link NotificationCompat.Builder#extend(NotificationCompat.Extender)}
     *  to apply the extension to a notification.
     * </ol>
     *
     * <pre class="prettyprint">
     * Notification notification = new NotificationCompat.Builder(context)
     *         ...
     *         .extend(new TvExtender()
     *                 .setChannelId("channel id"))
     *         .build();
     * NotificationManagerCompat.from(mContext).notify(0, notification);
     * </pre>
     *
     * <p>TV extensions can be accessed on an existing notification by using the
     * {@code TvExtender(Notification)} constructor, and then using the {@code get} methods
     * to access values.
     *
     * <p>Note that prior to {@link Build.VERSION_CODES#O} this field has no effect.
     */
    public static final class TvExtender implements Extender {
        private static final String TAG = "TvExtender";

        @RestrictTo(LIBRARY_GROUP_PREFIX)
        static final String EXTRA_TV_EXTENDER = "android.tv.EXTENSIONS";

        @RestrictTo(LIBRARY_GROUP_PREFIX)
        private static final String EXTRA_FLAGS = "flags";

        static final String EXTRA_CONTENT_INTENT = "content_intent";
        static final String EXTRA_DELETE_INTENT = "delete_intent";
        static final String EXTRA_CHANNEL_ID = "channel_id";
        static final String EXTRA_SUPPRESS_SHOW_OVER_APPS = "suppressShowOverApps";

        // Flags bitwise-ored to mFlags
        private static final int FLAG_AVAILABLE_ON_TV = 0x1;

        private int mFlags;
        private String mChannelId;
        private PendingIntent mContentIntent;
        private PendingIntent mDeleteIntent;
        private boolean mSuppressShowOverApps;

        /**
         * Create a {@link TvExtender} with default options.
         */
        public TvExtender() {
            mFlags = FLAG_AVAILABLE_ON_TV;
        }

        /**
         * Create a {@link TvExtender} from the TvExtender options of an existing Notification.
         *
         * @param notif The notification from which to copy options.
         */
        public TvExtender(@NonNull Notification notif) {
            // TvExtender was introduced in API level 26; note that before API level 26, the extras
            // added by TvExtender are not expected to be used; thus, we avoid setting them to save
            // memory.
            if (Build.VERSION.SDK_INT < 26) {
                return;
            }

            Bundle tvBundle = notif.extras == null
                    ? null : notif.extras.getBundle(EXTRA_TV_EXTENDER);
            if (tvBundle != null) {
                mFlags = tvBundle.getInt(EXTRA_FLAGS);
                mChannelId = tvBundle.getString(EXTRA_CHANNEL_ID);
                mSuppressShowOverApps = tvBundle.getBoolean(EXTRA_SUPPRESS_SHOW_OVER_APPS);
                mContentIntent = (PendingIntent) tvBundle.getParcelable(EXTRA_CONTENT_INTENT);
                mDeleteIntent = (PendingIntent) tvBundle.getParcelable(EXTRA_DELETE_INTENT);
            }
        }

        /**
         * Apply a TV extension to a notification that is being built. This is typically called by
         * the
         * {@link androidx.core.app.NotificationCompat.Builder#extend(NotificationCompat.Extender)}
         * method of {@link NotificationCompat.Builder}.
         */
        @Override
        public NotificationCompat.@NonNull Builder extend(
                NotificationCompat.@NonNull Builder builder) {
            // TvExtender was introduced in API level 26; note that before API level 26, the extras
            // added by TvExtender are not expected to be used; thus, we avoid setting them to save
            // memory.
            if (Build.VERSION.SDK_INT < 26) {
                return builder;
            }

            Bundle tvExtensions = new Bundle();

            tvExtensions.putInt(EXTRA_FLAGS, mFlags);
            tvExtensions.putString(EXTRA_CHANNEL_ID, mChannelId);
            tvExtensions.putBoolean(EXTRA_SUPPRESS_SHOW_OVER_APPS, mSuppressShowOverApps);
            if (mContentIntent != null) {
                tvExtensions.putParcelable(EXTRA_CONTENT_INTENT, mContentIntent);
            }

            if (mDeleteIntent != null) {
                tvExtensions.putParcelable(EXTRA_DELETE_INTENT, mDeleteIntent);
            }

            // Extras was added in API level 19.
            builder.getExtras().putBundle(EXTRA_TV_EXTENDER, tvExtensions);
            return builder;
        }

        /**
         * Returns true if this notification should be shown on TV. This method return true
         * if the notification was extended with a TvExtender.
         */
        public boolean isAvailableOnTv() {
            return (mFlags & FLAG_AVAILABLE_ON_TV) != 0;
        }

        /**
         * Specifies the channel the notification should be delivered on when shown on TV.
         * It can be different from the channel that the notification is delivered to when
         * posting on a non-TV device.
         *
         * @param channelId The channelId to use in the tv notification.
         * @return The object for method chaining.
         */
        public @NonNull TvExtender setChannelId(@Nullable String channelId) {
            mChannelId = channelId;
            return this;
        }

        /**
         * Returns the id of the channel this notification posts to on TV.
         */
        public @Nullable String getChannelId() {
            return mChannelId;
        }

        /**
         * Supplies a {@link PendingIntent} to be sent when the notification is selected on TV.
         * If provided, it is used instead of the content intent specified
         * at the level of Notification.
         */
        public @NonNull TvExtender setContentIntent(@Nullable PendingIntent intent) {
            mContentIntent = intent;
            return this;
        }

        /**
         * Returns the TV-specific content intent.  If this method returns null, the
         * main content intent on the notification should be used.
         *
         * @see {@link Notification#contentIntent}
         */
        public @Nullable PendingIntent getContentIntent() {
            return mContentIntent;
        }

        /**
         * Supplies a {@link PendingIntent} to send when the notification is cleared explicitly
         * by the user on TV.  If provided, it is used instead of the delete intent specified
         * at the level of Notification.
         */
        public @NonNull TvExtender setDeleteIntent(@Nullable PendingIntent intent) {
            mDeleteIntent = intent;
            return this;
        }

        /**
         * Returns the TV-specific delete intent.  If this method returns null, the
         * main delete intent on the notification should be used.
         *
         * @see {@link Notification#deleteIntent}
         */
        public @Nullable PendingIntent getDeleteIntent() {
            return mDeleteIntent;
        }

        /**
         * Specifies whether this notification should suppress showing a message over top of apps
         * outside of the launcher.
         */
        public @NonNull TvExtender setSuppressShowOverApps(boolean suppress) {
            mSuppressShowOverApps = suppress;
            return this;
        }

        /**
         * Returns true if this notification should not show messages over top of apps
         * outside of the launcher.
         */
        public boolean isSuppressShowOverApps() {
            return mSuppressShowOverApps;
        }
    }

    /**
     * Encapsulates the information needed to display a notification as a bubble.
     *
     * <p>A bubble is used to display app content in a floating window over the existing
     * foreground activity. A bubble has a collapsed state represented by an icon,
     * {@link BubbleMetadata.Builder#setIcon(IconCompat)} and an expanded state which is populated
     * via {@link BubbleMetadata.Builder#setIntent(PendingIntent)}.</p>
     *
     * <b>Notifications with a valid and allowed bubble will display in collapsed state
     * outside of the notification shade on unlocked devices. When a user interacts with the
     * collapsed bubble, the bubble intent will be invoked and displayed.</b>
     *
     * @see NotificationCompat.Builder#setBubbleMetadata(BubbleMetadata)
     */
    public static final class BubbleMetadata {

        private PendingIntent mPendingIntent;
        private PendingIntent mDeleteIntent;
        private IconCompat mIcon;
        private int mDesiredHeight;
        @DimenRes private int mDesiredHeightResId;
        private int mFlags;
        private String mShortcutId;

        /**
         * If set and the app creating the bubble is in the foreground, the bubble will be posted
         * in its expanded state, with the contents of {@link #getIntent()} in a floating window.
         *
         * <p>If the app creating the bubble is not in the foreground this flag has no effect.</p>
         *
         * <p>Generally this flag should only be set if the user has performed an action to request
         * or create a bubble.</p>
         */
        private static final int FLAG_AUTO_EXPAND_BUBBLE = 0x00000001;

        /**
         * Indicates whether the notification associated with the bubble is being visually
         * suppressed from the notification shade. When <code>true</code> the notification is
         * hidden, when <code>false</code> the notification shows as normal.
         *
         * <p>Apps sending bubbles may set this flag so that the bubble is posted <b>without</b>
         * the associated notification in the notification shade.</p>
         *
         * <p>Apps sending bubbles can only apply this flag when the app is in the foreground,
         * otherwise the flag is not respected. The app is considered foreground if it is visible
         * and on the screen, note that a foreground service does not qualify.</p>
         *
         * <p>Generally this flag should only be set by the app if the user has performed an
         * action to request or create a bubble, or if the user has seen the content in the
         * notification and the notification is no longer relevant. </p>
         *
         * <p>The system will also update this flag with <code>true</code> to hide the notification
         * from the user once the bubble has been expanded. </p>
         */
        private static final int FLAG_SUPPRESS_NOTIFICATION = 0x00000002;

        private BubbleMetadata(@Nullable PendingIntent expandIntent,
                @Nullable PendingIntent deleteIntent,
                @Nullable IconCompat icon, int height, @DimenRes int heightResId, int flags,
                @Nullable String shortcutId) {
            mPendingIntent = expandIntent;
            mIcon = icon;
            mDesiredHeight = height;
            mDesiredHeightResId = heightResId;
            mDeleteIntent = deleteIntent;
            mFlags = flags;
            mShortcutId = shortcutId;
        }

        /**
         * @return the pending intent used to populate the floating window for this bubble, or
         * null if this bubble is created via {@link Builder#Builder(String)}.
         */
        @SuppressLint("InvalidNullConversion")
        public @Nullable PendingIntent getIntent() {
            return mPendingIntent;
        }

        /**
         * @return the shortcut id used for this bubble if created via
         * {@link Builder#Builder(String)} or null if created via
         * {@link Builder#Builder(PendingIntent, IconCompat)}.
         */
        public @Nullable String getShortcutId() {
            return mShortcutId;
        }

        /**
         * @return the pending intent to send when the bubble is dismissed by a user, if one exists.
         */
        public @Nullable PendingIntent getDeleteIntent() {
            return mDeleteIntent;
        }

        /**
         * @return the icon that will be displayed for this bubble when it is collapsed, or null
         * if the bubble is created via {@link Builder#Builder(String)}.
         */
        @SuppressLint("InvalidNullConversion")
        public @Nullable IconCompat getIcon() {
            return mIcon;
        }

        /**
         * @return the ideal height, in DPs, for the floating window that app content defined by
         * {@link #getIntent()} for this bubble. A value of 0 indicates a desired height has not
         * been set.
         */
        @Dimension(unit = DP)
        public int getDesiredHeight() {
            return mDesiredHeight;
        }

        /**
         * @return the resId of ideal height for the floating window that app content defined by
         * {@link #getIntent()} for this bubble. A value of 0 indicates a res value has not
         * been provided for the desired height.
         */
        @DimenRes
        public int getDesiredHeightResId() {
            return mDesiredHeightResId;
        }

        /**
         * @return whether this bubble should auto expand when it is posted.
         *
         * @see BubbleMetadata.Builder#setAutoExpandBubble(boolean)
         */
        public boolean getAutoExpandBubble() {
            return (mFlags & FLAG_AUTO_EXPAND_BUBBLE) != 0;
        }

        /**
         * @return whether this bubble should suppress the notification when it is posted.
         *
         * @see BubbleMetadata.Builder#setSuppressNotification(boolean)
         */
        public boolean isNotificationSuppressed() {
            return (mFlags & FLAG_SUPPRESS_NOTIFICATION) != 0;
        }

        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public void setFlags(int flags) {
            mFlags = flags;
        }

        /**
         * Converts a {@link NotificationCompat.BubbleMetadata} to a platform-level
         * {@link Notification.BubbleMetadata}.
         *
         * @param compatMetadata the NotificationCompat.BubbleMetadata to convert
         * @return a {@link Notification.BubbleMetadata} containing the same data if compatMetadata
         * is non-null, otherwise null.
         */
        public static android.app.Notification.@Nullable BubbleMetadata toPlatform(
                @Nullable BubbleMetadata compatMetadata) {
            if (compatMetadata == null) {
                return null;
            }
            if (Build.VERSION.SDK_INT >= 30) {
                return Api30Impl.toPlatform(compatMetadata);
            } else if (Build.VERSION.SDK_INT == 29) {
                return Api29Impl.toPlatform(compatMetadata);
            }
            return null;
        }

        /**
         * Converts a platform-level {@link Notification.BubbleMetadata} to a
         * {@link NotificationCompat.BubbleMetadata}.
         *
         * @param platformMetadata the {@link Notification.BubbleMetadata} to convert
         * @return a {@link NotificationCompat.BubbleMetadata} containing the same data if
         * platformMetadata is non-null, otherwise null.
         */
        public static @Nullable BubbleMetadata fromPlatform(
                android.app.Notification.@Nullable BubbleMetadata platformMetadata) {
            if (platformMetadata == null) {
                return null;
            }
            if (Build.VERSION.SDK_INT >= 30) {
                return Api30Impl.fromPlatform(platformMetadata);
            } else if (Build.VERSION.SDK_INT == 29) {
                return Api29Impl.fromPlatform(platformMetadata);
            }
            return null;
        }

        /**
         * Builder to construct a {@link BubbleMetadata} object.
         */
        public static final class Builder {

            private PendingIntent mPendingIntent;
            private IconCompat mIcon;
            private int mDesiredHeight;
            @DimenRes private int mDesiredHeightResId;
            private int mFlags;
            private PendingIntent mDeleteIntent;
            private String mShortcutId;

            /**
             * @deprecated use {@link #Builder(String)} for a bubble created via a
             * {@link ShortcutInfoCompat} or {@link #Builder(PendingIntent, IconCompat)}
             * for a bubble created via a {@link PendingIntent}.
             */
            @Deprecated
            public Builder() {
            }

            /**
             * Creates a {@link BubbleMetadata.Builder} based on a {@link ShortcutInfoCompat}. To
             * create a shortcut bubble, ensure that the shortcut associated with the provided
             * {@param shortcutId} is published as a dynamic shortcut that was built with
             * {@link ShortcutInfoCompat.Builder#setLongLived(boolean)} being true, otherwise your
             * notification will not be able to bubble.
             *
             * <p>The shortcut icon will be used to represent the bubble when it is collapsed.</p>
             *
             * <p>The shortcut activity will be used when the bubble is expanded. This will display
             * the shortcut activity in a floating window over the existing foreground activity.</p>
             *
             * <p>If the shortcut has not been published when the bubble notification is sent,
             * no bubble will be produced. If the shortcut is deleted while the bubble is active,
             * the bubble will be removed.</p>
             *
             * @throws NullPointerException if shortcutId is null.
             * @see ShortcutInfoCompat
             * @see ShortcutInfoCompat.Builder#setLongLived(boolean)
             * @see android.content.pm.ShortcutManager#addDynamicShortcuts(List)
             */
            @RequiresApi(30)
            public Builder(@NonNull String shortcutId) {
                if (TextUtils.isEmpty(shortcutId)) {
                    throw new NullPointerException("Bubble requires a non-null shortcut id");
                }
                mShortcutId = shortcutId;
            }

            /**
             * Creates a {@link BubbleMetadata.Builder} based on the provided intent and icon.
             *
             * <p>The icon will be used to represent the bubble when it is collapsed. An icon
             * should be representative of the content within the bubble. If your app produces
             * multiple bubbles, the icon should be unique for each of them.</p>
             *
             * <p>The intent that will be used when the bubble is expanded. This will display the
             * app content in a floating window over the existing foreground activity. The intent
             * should point to a resizable activity. </p>
             *
             * @throws NullPointerException if intent is null.
             * @throws NullPointerException if icon is null.
             */
            public Builder(@NonNull PendingIntent intent, @NonNull IconCompat icon) {
                if (intent == null) {
                    throw new NullPointerException("Bubble requires non-null pending intent");
                }
                if (icon == null) {
                    throw new NullPointerException("Bubbles require non-null icon");
                }
                mPendingIntent = intent;
                mIcon = icon;
            }

            /**
             * Sets the intent that will be used when the bubble is expanded. This will display the
             * app content in a floating window over the existing foreground activity.
             *
             * @throws NullPointerException  if intent is null.
             * @throws IllegalStateException if this builder was created via
             *                               {@link #Builder(String)}.
             */
            public BubbleMetadata.@NonNull Builder setIntent(@NonNull PendingIntent intent) {
                if (mShortcutId != null) {
                    throw new IllegalStateException("Created as a shortcut bubble, cannot set a "
                            + "PendingIntent. Consider using "
                            + "BubbleMetadata.Builder(PendingIntent,Icon) instead.");
                }
                if (intent == null) {
                    throw new NullPointerException("Bubble requires non-null pending intent");
                }
                mPendingIntent = intent;
                return this;
            }
            /**
             * Sets the icon for the bubble. Can only be used if the bubble was created
             * via {@link #Builder(PendingIntent, IconCompat)}.
             *
             * <p>The icon will be used to represent the bubble when it is collapsed. An icon
             * should be representative of the content within the bubble. If your app produces
             * multiple bubbles, the icon should be unique for each of them.</p>
             *
             * <p>It is recommended to use an {@link Icon} of type {@link Icon#TYPE_URI}
             * or {@link Icon#TYPE_URI_ADAPTIVE_BITMAP}</p>
             *
             * @throws NullPointerException  if icon is null.
             * @throws IllegalStateException if this builder was created via
             *                               {@link #Builder(String)}.
             */
            public BubbleMetadata.@NonNull Builder setIcon(@NonNull IconCompat icon) {
                if (mShortcutId != null) {
                    throw new IllegalStateException("Created as a shortcut bubble, cannot set an "
                            + "Icon. Consider using "
                            + "BubbleMetadata.Builder(PendingIntent,Icon) instead.");
                }
                if (icon == null) {
                    throw new NullPointerException("Bubbles require non-null icon");
                }
                mIcon = icon;
                return this;
            }

            /**
             * Sets the desired height in DPs for the app content defined by
             * {@link #setIntent(PendingIntent)}, this height may not be respected if there is not
             * enough space on the screen or if the provided height is too small to be useful.
             * <p>
             * If {@link #setDesiredHeightResId(int)} was previously called on this builder, the
             * previous value set will be cleared after calling this method, and this value will
             * be used instead.
             */
            public BubbleMetadata.@NonNull Builder setDesiredHeight(
                    @Dimension(unit = DP) int height) {
                mDesiredHeight = Math.max(height, 0);
                mDesiredHeightResId = 0;
                return this;
            }

            /**
             * Sets the desired height via resId for the app content defined by
             * {@link #setIntent(PendingIntent)}, this height may not be respected if there is not
             * enough space on the screen or if the provided height is too small to be useful.
             * <p>
             * If {@link #setDesiredHeight(int)} was previously called on this builder, the
             * previous value set will be cleared after calling this method, and this value will
             * be used instead.
             */
            public BubbleMetadata.@NonNull Builder setDesiredHeightResId(
                    @DimenRes int heightResId) {
                mDesiredHeightResId = heightResId;
                mDesiredHeight = 0;
                return this;
            }

            /**
             * If set and the app creating the bubble is in the foreground, the bubble will be
             * posted in its expanded state, with the contents of {@link #getIntent()} in a
             * floating window.
             *
             * <p>If the app creating the bubble is not in the foreground this flag has no effect.
             * </p>
             *
             * <p>Generally this flag should only be set if the user has performed an action to
             * request or create a bubble.</p>
             */
            public BubbleMetadata.@NonNull Builder setAutoExpandBubble(boolean shouldExpand) {
                setFlag(FLAG_AUTO_EXPAND_BUBBLE, shouldExpand);
                return this;
            }

            /**
             * If set and the app posting the bubble is in the foreground, the bubble will be
             * posted <b>without</b> the associated notification in the notification shade.
             *
             * <p>If the app posting the bubble is not in the foreground this flag has no effect.
             * </p>
             *
             * <p>Generally this flag should only be set if the user has performed an action to
             * request or create a bubble, or if the user has seen the content in the notification
             * and the notification is no longer relevant.</p>
             */
            public BubbleMetadata.@NonNull Builder setSuppressNotification(
                    boolean shouldSuppressNotif) {
                setFlag(FLAG_SUPPRESS_NOTIFICATION, shouldSuppressNotif);
                return this;
            }

            /**
             * Sets an optional intent to send when this bubble is explicitly removed by the user.
             */
            public BubbleMetadata.@NonNull Builder setDeleteIntent(
                    @Nullable PendingIntent deleteIntent) {
                mDeleteIntent = deleteIntent;
                return this;
            }

            /**
             * Creates the {@link BubbleMetadata} defined by this builder.
             * <p>Will throw {@link NullPointerException} if required fields have not been set
             * on this builder.</p>
             */
            public @NonNull BubbleMetadata build() {
                if (mShortcutId == null && mPendingIntent == null) {
                    throw new NullPointerException(
                            "Must supply pending intent or shortcut to bubble");
                }
                if (mShortcutId == null && mIcon == null) {
                    throw new NullPointerException(
                            "Must supply an icon or shortcut for the bubble");
                }
                BubbleMetadata data = new BubbleMetadata(mPendingIntent, mDeleteIntent,
                        mIcon, mDesiredHeight, mDesiredHeightResId, mFlags, mShortcutId);
                data.setFlags(mFlags);
                return data;
            }

            private BubbleMetadata.@NonNull Builder setFlag(int mask, boolean value) {
                if (value) {
                    mFlags |= mask;
                } else {
                    mFlags &= ~mask;
                }
                return this;
            }
        }

        @RequiresApi(29)
        private static class Api29Impl {

            private Api29Impl() {
            }

            /**
             * Converts a {@link NotificationCompat.BubbleMetadata} to a platform-level
             * {@link Notification.BubbleMetadata}.
             *
             * @param compatMetadata the NotificationCompat.BubbleMetadata to convert
             * @return a {@link Notification.BubbleMetadata} containing the same data if
             * compatMetadata is non-null, otherwise null.
             */
            @RequiresApi(29)
            static android.app.Notification.@Nullable BubbleMetadata toPlatform(
                    @Nullable BubbleMetadata compatMetadata) {
                if (compatMetadata == null) {
                    return null;
                }
                if (compatMetadata.getIntent() == null) {
                    // If intent is null, it is shortcut bubble which is not supported in API 29
                    return null;
                }

                android.app.Notification.BubbleMetadata.Builder platformMetadataBuilder =
                        new android.app.Notification.BubbleMetadata.Builder()
                                .setIcon(compatMetadata.getIcon().toIcon())
                                .setIntent(compatMetadata.getIntent())
                                .setDeleteIntent(compatMetadata.getDeleteIntent())
                                .setAutoExpandBubble(compatMetadata.getAutoExpandBubble())
                                .setSuppressNotification(compatMetadata.isNotificationSuppressed());

                if (compatMetadata.getDesiredHeight() != 0) {
                    platformMetadataBuilder.setDesiredHeight(compatMetadata.getDesiredHeight());
                }

                if (compatMetadata.getDesiredHeightResId() != 0) {
                    platformMetadataBuilder.setDesiredHeightResId(
                            compatMetadata.getDesiredHeightResId());
                }

                return platformMetadataBuilder.build();
            }

            /**
             * Converts a platform-level {@link Notification.BubbleMetadata} to a
             * {@link NotificationCompat.BubbleMetadata}.
             *
             * @param platformMetadata the {@link Notification.BubbleMetadata} to convert
             * @return a {@link NotificationCompat.BubbleMetadata} containing the same data if
             * platformMetadata is non-null, otherwise null.
             */
            @RequiresApi(29)
            static @Nullable BubbleMetadata fromPlatform(
                    android.app.Notification.@Nullable BubbleMetadata platformMetadata) {
                if (platformMetadata == null) {
                    return null;
                }
                if (platformMetadata.getIntent() == null) {
                    // If intent is null, it is shortcut bubble which is not supported in API 29
                    return null;
                }
                BubbleMetadata.Builder compatBuilder =
                        new BubbleMetadata.Builder(platformMetadata.getIntent(),
                                IconCompat.createFromIcon(platformMetadata.getIcon()))
                                .setAutoExpandBubble(platformMetadata.getAutoExpandBubble())
                                .setDeleteIntent(platformMetadata.getDeleteIntent())
                                .setSuppressNotification(
                                        platformMetadata.isNotificationSuppressed());

                if (platformMetadata.getDesiredHeight() != 0) {
                    compatBuilder.setDesiredHeight(platformMetadata.getDesiredHeight());
                }

                if (platformMetadata.getDesiredHeightResId() != 0) {
                    compatBuilder.setDesiredHeightResId(platformMetadata.getDesiredHeightResId());
                }

                return compatBuilder.build();
            }
        }

        @RequiresApi(30)
        private static class Api30Impl {

            private Api30Impl() {
            }

            /**
             * Converts a {@link NotificationCompat.BubbleMetadata} to a platform-level
             * {@link Notification.BubbleMetadata}.
             *
             * @param compatMetadata the NotificationCompat.BubbleMetadata to convert
             * @return a {@link Notification.BubbleMetadata} containing the same data if
             * compatMetadata is non-null, otherwise null.
             */
            @RequiresApi(30)
            static android.app.Notification.@Nullable BubbleMetadata toPlatform(
                    @Nullable BubbleMetadata compatMetadata) {
                if (compatMetadata == null) {
                    return null;
                }

                android.app.Notification.BubbleMetadata.Builder platformMetadataBuilder = null;
                if (compatMetadata.getShortcutId() != null) {
                    platformMetadataBuilder = new android.app.Notification.BubbleMetadata.Builder(
                            compatMetadata.getShortcutId());
                } else {
                    platformMetadataBuilder =
                            new android.app.Notification.BubbleMetadata.Builder(
                                    compatMetadata.getIntent(), compatMetadata.getIcon().toIcon());
                }
                platformMetadataBuilder
                        .setDeleteIntent(compatMetadata.getDeleteIntent())
                        .setAutoExpandBubble(compatMetadata.getAutoExpandBubble())
                        .setSuppressNotification(compatMetadata.isNotificationSuppressed());

                if (compatMetadata.getDesiredHeight() != 0) {
                    platformMetadataBuilder.setDesiredHeight(compatMetadata.getDesiredHeight());
                }

                if (compatMetadata.getDesiredHeightResId() != 0) {
                    platformMetadataBuilder.setDesiredHeightResId(
                            compatMetadata.getDesiredHeightResId());
                }

                return platformMetadataBuilder.build();
            }

            /**
             * Converts a platform-level {@link Notification.BubbleMetadata} to a
             * {@link NotificationCompat.BubbleMetadata}.
             *
             * @param platformMetadata the {@link Notification.BubbleMetadata} to convert
             * @return a {@link NotificationCompat.BubbleMetadata} containing the same data if
             * platformMetadata is non-null, otherwise null.
             */
            @RequiresApi(30)
            static @Nullable BubbleMetadata fromPlatform(
                    android.app.Notification.@Nullable BubbleMetadata platformMetadata) {
                if (platformMetadata == null) {
                    return null;
                }

                BubbleMetadata.Builder compatBuilder = null;
                if (platformMetadata.getShortcutId() != null) {
                    compatBuilder = new BubbleMetadata.Builder(platformMetadata.getShortcutId());
                } else {
                    compatBuilder = new BubbleMetadata.Builder(platformMetadata.getIntent(),
                            IconCompat.createFromIcon(platformMetadata.getIcon()));
                }
                compatBuilder
                        .setAutoExpandBubble(platformMetadata.getAutoExpandBubble())
                        .setDeleteIntent(platformMetadata.getDeleteIntent())
                        .setSuppressNotification(
                                platformMetadata.isNotificationSuppressed());

                if (platformMetadata.getDesiredHeight() != 0) {
                    compatBuilder.setDesiredHeight(platformMetadata.getDesiredHeight());
                }

                if (platformMetadata.getDesiredHeightResId() != 0) {
                    compatBuilder.setDesiredHeightResId(platformMetadata.getDesiredHeightResId());
                }

                return compatBuilder.build();
            }
        }
    }

    /**
     * Get an array of Notification objects from a parcelable array bundle field.
     * Update the bundle to have a typed array so fetches in the future don't need
     * to do an array copy.
     */
    @SuppressWarnings("deprecation")
    static Notification @NonNull [] getNotificationArrayFromBundle(@NonNull Bundle bundle,
            @NonNull String key) {
        Parcelable[] array = bundle.getParcelableArray(key);
        if (array instanceof Notification[] || array == null) {
            return (Notification[]) array;
        }
        Notification[] typedArray = new Notification[array.length];
        for (int i = 0; i < array.length; i++) {
            typedArray[i] = (Notification) array[i];
        }
        bundle.putParcelableArray(key, typedArray);
        return typedArray;
    }

    /**
     * Gets the {@link Notification#extras} field from a notification in a backwards
     * compatible manner. Extras field was supported from JellyBean (Api level 16)
     * forwards. This function will return {@code null} on older api levels.
     * @deprecated Call {@link Notification#extras} directly.
     */
    @Deprecated
    @androidx.annotation.ReplaceWith(expression = "notification.extras")
    public static @Nullable Bundle getExtras(@NonNull Notification notification) {
        return notification.extras;
    }

    /**
     * Get the number of actions in this notification in a backwards compatible
     * manner. Actions were supported from JellyBean (Api level 16) forwards.
     */
    public static int getActionCount(@NonNull Notification notification) {
        return notification.actions != null ? notification.actions.length : 0;
    }

    /**
     * Get an action on this notification in a backwards compatible
     * manner. Actions were supported from JellyBean (Api level 16) forwards.
     * @param notification The notification to inspect.
     * @param actionIndex The index of the action to retrieve.
     */
    @SuppressWarnings("deprecation")
    public static @Nullable Action getAction(@NonNull Notification notification, int actionIndex) {
        return getActionCompatFromAction(notification.actions[actionIndex]);
    }

    /**
     * Get the {@link BubbleMetadata} for a notification that will be used to display app content in
     * a floating window over the existing foreground activity.
     *
     * @param notification the notification to inspect
     *
     * @return the BubbleMetadata if available and set, otherwise null
     */
    public static @Nullable BubbleMetadata getBubbleMetadata(@NonNull Notification notification) {
        if (Build.VERSION.SDK_INT >= 29) {
            return BubbleMetadata.fromPlatform(Api29Impl.getBubbleMetadata(notification));
        } else {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    static @NonNull Action getActionCompatFromAction(Notification.@NonNull Action action) {
        final RemoteInput[] remoteInputs;
        final android.app.RemoteInput[] srcArray = action.getRemoteInputs();
        if (srcArray == null) {
            remoteInputs = null;
        } else {
            remoteInputs = new RemoteInput[srcArray.length];
            for (int i = 0; i < srcArray.length; i++) {
                android.app.RemoteInput src = srcArray[i];
                remoteInputs[i] = new RemoteInput(
                        src.getResultKey(),
                        src.getLabel(),
                        src.getChoices(),
                        src.getAllowFreeFormInput(),
                        Build.VERSION.SDK_INT >= 29
                                ? Api29Impl.getEditChoicesBeforeSending(src)
                                : RemoteInput.EDIT_CHOICES_BEFORE_SENDING_AUTO,
                        src.getExtras(),
                        null);
            }
        }

        final boolean allowGeneratedReplies;
        if (Build.VERSION.SDK_INT >= 24) {
            allowGeneratedReplies = action.getExtras().getBoolean(
                    NotificationCompatJellybean.EXTRA_ALLOW_GENERATED_REPLIES)
                    || Api24Impl.getAllowGeneratedReplies(action);
        } else {
            allowGeneratedReplies = action.getExtras().getBoolean(
                    NotificationCompatJellybean.EXTRA_ALLOW_GENERATED_REPLIES);
        }

        final boolean showsUserInterface =
                action.getExtras().getBoolean(Action.EXTRA_SHOWS_USER_INTERFACE, true);

        final @Action.SemanticAction int semanticAction;
        if (Build.VERSION.SDK_INT >= 28) {
            semanticAction = Api28Impl.getSemanticAction(action);
        } else {
            semanticAction = action.getExtras().getInt(
                    Action.EXTRA_SEMANTIC_ACTION, Action.SEMANTIC_ACTION_NONE);
        }

        final boolean isContextual = Build.VERSION.SDK_INT >= 29 ? Api29Impl.isContextual(action)
                : false;

        final boolean authRequired =
                Build.VERSION.SDK_INT >= 31 ? Api31Impl.isAuthenticationRequired(action) : false;

        if (action.getIcon() == null && action.icon != 0) {
            return new Action(action.icon, action.title, action.actionIntent,
                    action.getExtras(), remoteInputs, null,
                    allowGeneratedReplies, semanticAction, showsUserInterface, isContextual,
                    authRequired);
        }
        IconCompat icon = action.getIcon() == null ? null
                : IconCompat.createFromIconOrNullIfZeroResId(action.getIcon());
        return new Action(icon, action.title, action.actionIntent, action.getExtras(),
                remoteInputs, null, allowGeneratedReplies, semanticAction,
                showsUserInterface, isContextual, authRequired);
    }

    /** Returns the invisible actions contained within the given notification. */
    public static @NonNull List<Action> getInvisibleActions(@NonNull Notification notification) {
        ArrayList<Action> result = new ArrayList<>();
        Bundle carExtenderBundle =
                notification.extras.getBundle(CarExtender.EXTRA_CAR_EXTENDER);
        if (carExtenderBundle == null) {
            return result;
        }

        Bundle listBundle = carExtenderBundle.getBundle(CarExtender.EXTRA_INVISIBLE_ACTIONS);
        if (listBundle != null) {
            for (int i = 0; i < listBundle.size(); i++) {
                result.add(NotificationCompatJellybean.getActionFromBundle(
                        listBundle.getBundle(Integer.toString(i))));
            }
        }
        return result;
    }

    /**
     * Returns the people in the notification.
     * On platforms which do not have the {@link android.app.Person} class, the
     * {@link Person} objects will contain only the URI from {@link Builder#addPerson(String)}.
     */
    @SuppressWarnings("deprecation")
    public static @NonNull List<Person> getPeople(@NonNull Notification notification) {
        ArrayList<Person> result = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 28) {
            final ArrayList<android.app.Person> peopleList =
                    notification.extras.getParcelableArrayList(EXTRA_PEOPLE_LIST);
            if (peopleList != null && !peopleList.isEmpty()) {
                for (android.app.Person person : peopleList) {
                    result.add(Person.fromAndroidPerson(person));
                }
            }
        } else {
            final String[] peopleArray = notification.extras.getStringArray(EXTRA_PEOPLE);
            if (peopleArray != null && peopleArray.length != 0) {
                for (String personUri : peopleArray) {
                    result.add(new Person.Builder().setUri(personUri).build());
                }
            }
        }
        return result;
    }

    /** Returns the content title provided to {@link Builder#setContentTitle(CharSequence)}. */
    public static @Nullable CharSequence getContentTitle(@NonNull Notification notification) {
        return notification.extras.getCharSequence(EXTRA_TITLE);
    }

    /** Returns the content text provided to {@link Builder#setContentText(CharSequence)}. */
    public static @Nullable CharSequence getContentText(@NonNull Notification notification) {
        return notification.extras.getCharSequence(EXTRA_TEXT);
    }

    /** Returns the content info provided to {@link Builder#setContentInfo(CharSequence)}. */
    public static @Nullable CharSequence getContentInfo(@NonNull Notification notification) {
        return notification.extras.getCharSequence(EXTRA_INFO_TEXT);
    }

    /** Returns the sub text provided to {@link Builder#setSubText(CharSequence)}. */
    public static @Nullable CharSequence getSubText(@NonNull Notification notification) {
        return notification.extras.getCharSequence(EXTRA_SUB_TEXT);
    }

    /**
     * Returns the very short text summarizing the most critical information contained in the
     * notification, or null if this field was not set.
     */
    public static @Nullable String getShortCriticalText(@NonNull Notification notification) {
        return notification.extras.getString(EXTRA_SHORT_CRITICAL_TEXT);
    }

    /**
     * Returns whether this notification has requested to be a promoted ongoing notification.
     */
    public static boolean isRequestPromotedOngoing(@NonNull Notification notification) {
        return notification.extras.getBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, false);
    }

    /**
     * Get the category of this notification in a backwards compatible
     * manner.
     * @param notification The notification to inspect.
     */
    public static @Nullable String getCategory(@NonNull Notification notification) {
        return notification.category;
    }

    /**
     * Get whether or not this notification is only relevant to the current device.
     *
     * <p>Some notifications can be bridged to other devices for remote display.
     * If this hint is set, it is recommend that this notification not be bridged.
     */
    public static boolean getLocalOnly(@NonNull Notification notification) {
        return (notification.flags & Notification.FLAG_LOCAL_ONLY) != 0;
    }

    /**
     * Get the key used to group this notification into a cluster or stack
     * with other notifications on devices which support such rendering.
     */
    public static @Nullable String getGroup(@NonNull Notification notification) {
        return notification.getGroup();
    }

    /** Get the value provided to {@link Builder#setShowWhen(boolean)} */
    public static boolean getShowWhen(@NonNull Notification notification) {
        // NOTE: This field can be set since API 17, but is impossible to extract from the
        // constructed Notification until API 19 when it was moved to the extras.
        return notification.extras.getBoolean(EXTRA_SHOW_WHEN);
    }

    /** Get the value provided to {@link Builder#setUsesChronometer(boolean)} */
    public static boolean getUsesChronometer(@NonNull Notification notification) {
        // NOTE: This field can be set since API 16, but is impossible to extract from the
        // constructed Notification until API 19 when it was moved to the extras.
        return notification.extras.getBoolean(EXTRA_SHOW_CHRONOMETER);
    }

    /** Get the value provided to {@link Builder#setOnlyAlertOnce(boolean)} */
    public static boolean getOnlyAlertOnce(@NonNull Notification notification) {
        return (notification.flags & Notification.FLAG_ONLY_ALERT_ONCE) != 0;
    }

    /** Get the value provided to {@link Builder#setAutoCancel(boolean)} */
    public static boolean getAutoCancel(@NonNull Notification notification) {
        return (notification.flags & Notification.FLAG_AUTO_CANCEL) != 0;
    }

    /** Get the value provided to {@link Builder#setOngoing(boolean)} */
    public static boolean getOngoing(@NonNull Notification notification) {
        return (notification.flags & Notification.FLAG_ONGOING_EVENT) != 0;
    }

    /** Get the value provided to {@link Builder#setColor(int)} */
    public static int getColor(@NonNull Notification notification) {
        return notification.color;
    }

    /** Get the value provided to {@link Builder#setVisibility(int)} */
    public static @NotificationVisibility int getVisibility(@NonNull Notification notification) {
        return notification.visibility;
    }

    /** Get the value provided to {@link Builder#setVisibility(int)} */
    public static @Nullable Notification getPublicVersion(@NonNull Notification notification) {
        return notification.publicVersion;
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    static boolean getHighPriority(@NonNull Notification notification) {
        return (notification.flags & Notification.FLAG_HIGH_PRIORITY) != 0;
    }

    /**
     * Get whether this notification to be the group summary for a group of notifications.
     * Grouped notifications may display in a cluster or stack on devices which
     * support such rendering. Requires a group key also be set using {@link Builder#setGroup}.
     * @return Whether this notification is a group summary.
     */
    public static boolean isGroupSummary(@NonNull Notification notification) {
        return (notification.flags & Notification.FLAG_GROUP_SUMMARY) != 0;
    }

    /**
     * Get a sort key that orders this notification among other notifications from the
     * same package. This can be useful if an external sort was already applied and an app
     * would like to preserve this. Notifications will be sorted lexicographically using this
     * value, although providing different priorities in addition to providing sort key may
     * cause this value to be ignored.
     *
     * <p>This sort key can also be used to order members of a notification group. See
     * {@link Builder#setGroup}.
     *
     * @see String#compareTo(String)
     */
    public static @Nullable String getSortKey(@NonNull Notification notification) {
        return notification.getSortKey();
    }

    /**
     * @return the ID of the channel this notification posts to.
     */
    public static @Nullable String getChannelId(@NonNull Notification notification) {
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.getChannelId(notification);
        } else {
            return null;
        }
    }

    /**
     * Returns the time at which this notification should be canceled by the system, if it's not
     * canceled already.
     */
    public static long getTimeoutAfter(@NonNull Notification notification) {
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.getTimeoutAfter(notification);
        } else {
            return 0;
        }
    }

    /**
     * Returns what icon should be shown for this notification if it is being displayed in a
     * Launcher that supports badging. Will be one of {@link #BADGE_ICON_NONE},
     * {@link #BADGE_ICON_SMALL}, or {@link #BADGE_ICON_LARGE}.
     */
    public static int getBadgeIconType(@NonNull Notification notification) {
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.getBadgeIconType(notification);
        } else {
            return BADGE_ICON_NONE;
        }
    }

    /**
     * Returns the {@link ShortcutInfoCompat#getId() id} that this
     * notification supersedes, if any.
     */
    public static @Nullable String getShortcutId(@NonNull Notification notification) {
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.getShortcutId(notification);
        } else {
            return null;
        }
    }

    /**
     * Returns the settings text provided to {@link Builder#setSettingsText(CharSequence)}.
     */
    public static @Nullable CharSequence getSettingsText(@NonNull Notification notification) {
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.getSettingsText(notification);
        } else {
            return null;
        }
    }

    /**
     * Gets the {@link LocusIdCompat} associated with this notification.
     *
     * <p>Used by the Android system to correlate objects (such as
     * {@link androidx.core.content.pm.ShortcutInfoCompat} and
     * {@link android.view.contentcapture.ContentCaptureContext}).
     */
    public static @Nullable LocusIdCompat getLocusId(@NonNull Notification notification) {
        if (Build.VERSION.SDK_INT >= 29) {
            LocusId locusId = Api29Impl.getLocusId(notification);
            return locusId == null ? null : LocusIdCompat.toLocusIdCompat(locusId);
        } else {
            return null;
        }
    }

    /**
     * Returns which type of notifications in a group are responsible for audibly alerting the
     * user. See {@link #GROUP_ALERT_ALL}, {@link #GROUP_ALERT_CHILDREN},
     * {@link #GROUP_ALERT_SUMMARY}.
     */
    @GroupAlertBehavior
    public static int getGroupAlertBehavior(@NonNull Notification notification) {
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.getGroupAlertBehavior(notification);
        } else {
            return GROUP_ALERT_ALL;
        }
    }

    /**
     * Returns whether the platform is allowed (by the app developer) to generate contextual actions
     * for this notification.
     */
    public static boolean getAllowSystemGeneratedContextualActions(
            @NonNull Notification notification) {
        if (Build.VERSION.SDK_INT >= 29) {
            return Api29Impl.getAllowSystemGeneratedContextualActions(notification);
        } else {
            return false;
        }
    }

    /**
     * Reduces the size of a provided {@code icon} if it's larger than the maximum allowed
     * for a notification large icon; returns the resized icon. Note that the framework does this
     * scaling automatically starting from API 27.
     */
    public static @Nullable Bitmap reduceLargeIconSize(@NonNull Context context,
            @Nullable Bitmap icon) {
        if (icon == null || Build.VERSION.SDK_INT >= 27) {
            return icon;
        }

        Resources res = context.getResources();
        int maxWidth =
                res.getDimensionPixelSize(R.dimen.compat_notification_large_icon_max_width);
        int maxHeight =
                res.getDimensionPixelSize(R.dimen.compat_notification_large_icon_max_height);
        if (icon.getWidth() <= maxWidth && icon.getHeight() <= maxHeight) {
            return icon;
        }

        double scale = Math.min(
                maxWidth / (double) Math.max(1, icon.getWidth()),
                maxHeight / (double) Math.max(1, icon.getHeight()));
        return Bitmap.createScaledBitmap(
                icon,
                (int) Math.ceil(icon.getWidth() * scale),
                (int) Math.ceil(icon.getHeight() * scale),
                true /* filtered */);
    }

    /**
     * Returns whether the notification has any promotable characteristics.
     *
     * <p>This is a wrapper around {@link Notification#hasPromotableCharacteristics()}.
     */
    public static boolean hasPromotableCharacteristics(@NonNull Notification notification) {
        if (Build.VERSION.SDK_INT >= 36) {
            return Api36Impl.hasPromotableCharacteristics(notification);
        } else {
            return false;
        }
    }

    static boolean isSilent(@NonNull Notification notification) {
        if (Build.VERSION.SDK_INT >= 26) {
            if (NotificationCompat.GROUP_KEY_SILENT.equals(notification.getGroup())) {
                return true;
            }

            if (isGroupSummary(notification)) {
                return getGroupAlertBehavior(notification) == GROUP_ALERT_CHILDREN;
            } else {
                return getGroupAlertBehavior(notification) == GROUP_ALERT_SUMMARY;
            }
        } else {
            return ((notification.defaults & DEFAULT_SOUND) == 0
                    && (notification.defaults & DEFAULT_VIBRATE) == 0
                    && notification.vibrate == null
                    && notification.sound == null);
        }
    }

    /** @deprecated This type should not be instantiated as it contains only static methods. */
    @Deprecated
    @SuppressWarnings("PrivateConstructorForUtilityClass")
    public NotificationCompat() {
    }

    /**
     * A class for wrapping calls to {@link Notification} methods which
     * were added in API 24; these calls must be wrapped to avoid performance issues.
     * See the UnsafeNewApiCall lint rule for more details.
     */
    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() { }

        static boolean getAllowGeneratedReplies(Notification.Action action) {
            return action.getAllowGeneratedReplies();
        }

    }

    /**
     * A class for wrapping calls to {@link Notification} methods which
     * were added in API 26; these calls must be wrapped to avoid performance issues.
     * See the UnsafeNewApiCall lint rule for more details.
     */
    @RequiresApi(26)
    static class Api26Impl {
        private Api26Impl() { }

        static int getGroupAlertBehavior(Notification notification) {
            return notification.getGroupAlertBehavior();
        }

        static CharSequence getSettingsText(Notification notification) {
            return notification.getSettingsText();
        }

        static String getShortcutId(Notification notification) {
            return notification.getShortcutId();
        }

        static int getBadgeIconType(Notification notification) {
            return notification.getBadgeIconType();
        }

        static long getTimeoutAfter(Notification notification) {
            return notification.getTimeoutAfter();
        }

        static String getChannelId(Notification notification) {
            return notification.getChannelId();
        }
    }

    /**
     * A class for wrapping calls to {@link Notification} methods which
     * were added in API 28; these calls must be wrapped to avoid performance issues.
     * See the UnsafeNewApiCall lint rule for more details.
     */
    @RequiresApi(28)
    static class Api28Impl {
        private Api28Impl() { }

        static int getSemanticAction(Notification.Action action) {
            return action.getSemanticAction();
        }
    }

    /**
     * A class for wrapping calls to {@link Notification} methods which
     * were added in API 29; these calls must be wrapped to avoid performance issues.
     * See the UnsafeNewApiCall lint rule for more details.
     */
    @RequiresApi(29)
    static class Api29Impl {
        private Api29Impl() { }

        static boolean getAllowSystemGeneratedContextualActions(Notification notification) {
            return notification.getAllowSystemGeneratedContextualActions();
        }

        static LocusId getLocusId(Notification notification) {
            return notification.getLocusId();
        }

        static boolean isContextual(Notification.Action action) {
            return action.isContextual();
        }

        static int getEditChoicesBeforeSending(android.app.RemoteInput remoteInput) {
            return remoteInput.getEditChoicesBeforeSending();
        }

        static Notification.BubbleMetadata getBubbleMetadata(Notification notification) {
            return notification.getBubbleMetadata();
        }
    }

    /**
     * A class for wrapping calls to {@link Notification} methods which
     * were added in API 31; these calls must be wrapped to avoid performance issues.
     * See the UnsafeNewApiCall lint rule for more details.
     */
    @RequiresApi(31)
    static class Api31Impl {
        private Api31Impl() { }

        static boolean isAuthenticationRequired(Notification.Action action) {
            return action.isAuthenticationRequired();
        }

    }

    /**
     * A class for wrapping calls to {@link Notification} methods which
     * were added in API 36; these calls must be wrapped to avoid performance issues.
     * See the UnsafeNewApiCall lint rule for more details.
     */
    @RequiresApi(36)
    static class Api36Impl {
        private Api36Impl() { }

        static boolean hasPromotableCharacteristics(@NonNull Notification notification) {
            return notification.hasPromotableCharacteristics();
        }

    }
}
