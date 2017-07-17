/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v7.app;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.support.v4.app.BundleCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.widget.RemoteViews;

/**
 * An extension of {@link android.support.v4.app.NotificationCompat} which adds additional styles.
 * @deprecated Use {@link android.support.v4.app.NotificationCompat}.
 */
@Deprecated
public class NotificationCompat extends android.support.v4.app.NotificationCompat {

    /**
     * @deprecated Use the static classes in {@link android.support.v4.app.NotificationCompat}.
     */
    @Deprecated
    public NotificationCompat() {
    }

    /**
     * Extracts a {@link MediaSessionCompat.Token} from the extra values
     * in the {@link MediaStyle} {@link android.app.Notification notification}.
     *
     * @param notification The notification to extract a {@link MediaSessionCompat.Token} from.
     * @return The {@link MediaSessionCompat.Token} in the {@code notification} if it contains,
     *         null otherwise.
     * @deprecated Use {@link android.support.v4.media.app.NotificationCompat.MediaStyle
     * #getMediaSession(Notification)}.
     */
    @Deprecated
    public static MediaSessionCompat.Token getMediaSession(Notification notification) {
        Bundle extras = getExtras(notification);
        if (extras != null) {
            if (Build.VERSION.SDK_INT >= 21) {
                Object tokenInner = extras.getParcelable(EXTRA_MEDIA_SESSION);
                if (tokenInner != null) {
                    return MediaSessionCompat.Token.fromToken(tokenInner);
                }
            } else {
                IBinder tokenInner = BundleCompat.getBinder(extras, EXTRA_MEDIA_SESSION);
                if (tokenInner != null) {
                    Parcel p = Parcel.obtain();
                    p.writeStrongBinder(tokenInner);
                    p.setDataPosition(0);
                    MediaSessionCompat.Token token =
                            MediaSessionCompat.Token.CREATOR.createFromParcel(p);
                    p.recycle();
                    return token;
                }
            }
        }
        return null;
    }

    /**
     * @deprecated All {@link android.support.v4.app.NotificationCompat.Style styles} can now be
     * used with {@link android.support.v4.app.NotificationCompat.Builder}.
     */
    @Deprecated
    public static class Builder extends android.support.v4.app.NotificationCompat.Builder {

        /**
         * @inheritDoc
         * @deprecated Use {@link android.support.v4.app.NotificationCompat.Builder
         * #NotificationCompat.Builder(Context, String)}
         */
        @Deprecated
        public Builder(Context context) {
            super(context);
        }
    }

    /**
     * Notification style for media playback notifications.
     *
     * In the expanded form, {@link Notification#bigContentView}, up to 5
     * {@link android.support.v4.app.NotificationCompat.Action}s specified with
     * {@link android.support.v4.app.NotificationCompat.Builder#addAction(int, CharSequence,
     * PendingIntent) addAction} will be shown as icon-only pushbuttons, suitable for transport
     * controls. The Bitmap given to
     * {@link android.support.v4.app.NotificationCompat.Builder#setLargeIcon(
     * android.graphics.Bitmap) setLargeIcon()} will
     * be treated as album artwork.
     *
     * Unlike the other styles provided here, MediaStyle can also modify the standard-size
     * {@link Notification#contentView}; by providing action indices to
     * {@link #setShowActionsInCompactView(int...)} you can promote up to 3 actions to be displayed
     * in the standard view alongside the usual content.
     *
     * Notifications created with MediaStyle will have their category set to
     * {@link Notification#CATEGORY_TRANSPORT CATEGORY_TRANSPORT} unless you set a different
     * category using {@link android.support.v4.app.NotificationCompat.Builder#setCategory(String)
     * setCategory()}.
     *
     * Finally, if you attach a {@link android.media.session.MediaSession.Token} using
     * {@link android.support.v7.app.NotificationCompat.MediaStyle#setMediaSession}, the System UI
     * can identify this as a notification representing an active media session and respond
     * accordingly (by showing album artwork in the lockscreen, for example).
     *
     * To use this style with your Notification, feed it to
     * {@link android.support.v4.app.NotificationCompat.Builder#setStyle} like so:
     * <pre class="prettyprint">
     * Notification noti = new NotificationCompat.Builder()
     *     .setSmallIcon(R.drawable.ic_stat_player)
     *     .setContentTitle(&quot;Track title&quot;)
     *     .setContentText(&quot;Artist - Album&quot;)
     *     .setLargeIcon(albumArtBitmap))
     *     .setStyle(<b>new NotificationCompat.MediaStyle()</b>
     *         .setMediaSession(mySession))
     *     .build();
     * </pre>
     *
     * @deprecated Use {@link android.support.v4.media.app.NotificationCompat.MediaStyle}.
     */
    @Deprecated
    public static class MediaStyle extends
            android.support.v4.media.app.NotificationCompat.MediaStyle {

        /**
         * @deprecated Use {@link android.support.v4.media.app.NotificationCompat.MediaStyle
         * #MediaStyle()}
         */
        @Deprecated
        public MediaStyle() {
            super();
        }

        /**
         * @deprecated Use {@link android.support.v4.media.app.NotificationCompat.MediaStyle
         * #MediaStyle(android.support.v4.app.NotificationCompat.Builder)}
         */
        @Deprecated
        public MediaStyle(android.support.v4.app.NotificationCompat.Builder builder) {
            super(builder);
        }

        /**
         * Requests up to 3 actions (by index in the order of addition) to be shown in the compact
         * notification view.
         *
         * @param actions the indices of the actions to show in the compact notification view
         *
         * @deprecated Use {@link android.support.v4.media.app.NotificationCompat.MediaStyle
         * #setShowActionsInCompactView(int...)}
         */
        @Deprecated
        @Override
        public MediaStyle setShowActionsInCompactView(int...actions) {
            return (MediaStyle) super.setShowActionsInCompactView(actions);
        }

        /**
         * Attaches a {@link MediaSessionCompat.Token} to this Notification
         * to provide additional playback information and control to the SystemUI.
         *
         * @deprecated Use {@link android.support.v4.media.app.NotificationCompat.MediaStyle
         * #setMediaSession(MediaSessionCompat.Token)}
         */
        @Deprecated
        @Override
        public MediaStyle setMediaSession(MediaSessionCompat.Token token) {
            return (MediaStyle) super.setMediaSession(token);
        }

        /**
         * Sets whether a cancel button at the top right should be shown in the notification on
         * platforms before Lollipop.
         *
         * <p>Prior to Lollipop, there was a bug in the framework which prevented the developer to
         * make a notification dismissable again after having used the same notification as the
         * ongoing notification for a foreground service. When the notification was posted by
         * {@link android.app.Service#startForeground}, but then the service exited foreground mode
         * via {@link android.app.Service#stopForeground}, without removing the notification, the
         * notification stayed ongoing, and thus not dismissable.
         *
         * <p>This is a common scenario for media notifications, as this is exactly the service
         * lifecycle that happens when playing/pausing media. Thus, a workaround is provided by the
         * support library: Instead of making the notification ongoing depending on the playback
         * state, the support library provides the ability to add an explicit cancel button to the
         * notification.
         *
         * <p>Note that the notification is enforced to be ongoing if a cancel button is shown to
         * provide a consistent user experience.
         *
         * <p>Also note that this method is a no-op when running on Lollipop and later.
         *
         * @param show whether to show a cancel button
         *
         * @deprecated Use {@link android.support.v4.media.app.NotificationCompat.MediaStyle
         * #setShowCancelButton(boolean)}
         */
        @Deprecated
        @Override
        public MediaStyle setShowCancelButton(boolean show) {
            return (MediaStyle) super.setShowCancelButton(show);
        }

        /**
         * Sets the pending intent to be sent when the cancel button is pressed. See {@link
         * #setShowCancelButton}.
         *
         * @param pendingIntent the intent to be sent when the cancel button is pressed
         *
         * @deprecated Use {@link android.support.v4.media.app.NotificationCompat.MediaStyle
         * #setCancelButtonIntent(PendingIntent)}
         */
        @Deprecated
        @Override
        public MediaStyle setCancelButtonIntent(PendingIntent pendingIntent) {
            return (MediaStyle) super.setCancelButtonIntent(pendingIntent);
        }
    }


    /**
     * Notification style for custom views that are decorated by the system.
     *
     * <p>Instead of providing a notification that is completely custom, a developer can set this
     * style and still obtain system decorations like the notification header with the expand
     * affordance and actions.
     *
     * <p>Use {@link android.app.Notification.Builder#setCustomContentView(RemoteViews)},
     * {@link android.app.Notification.Builder#setCustomBigContentView(RemoteViews)} and
     * {@link android.app.Notification.Builder#setCustomHeadsUpContentView(RemoteViews)} to set the
     * corresponding custom views to display.
     *
     * <p>To use this style with your Notification, feed it to
     * {@link android.support.v4.app.NotificationCompat.Builder#setStyle(Style)} like so:
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
     * {@link android.support.v7.appcompat.R.style#TextAppearance_AppCompat_Notification} or
     * {@link android.support.v7.appcompat.R.style#TextAppearance_AppCompat_Notification_Title} in
     * your custom views in order to get the correct styling on each platform version.
     *
     * @deprecated Use {@link android.support.v4.app.NotificationCompat.DecoratedCustomViewStyle}
     * and {@link android.support.compat.R.style#TextAppearance_Compat_Notification} or
     * {@link android.support.compat.R.style#TextAppearance_Compat_Notification_Title}.
     */
    @Deprecated
    public static class DecoratedCustomViewStyle extends
            android.support.v4.app.NotificationCompat.DecoratedCustomViewStyle {

        /**
         * @deprecated Use
         * {@link android.support.v4.app.NotificationCompat.DecoratedCustomViewStyle
         * #DecoratedCustomViewStyle()}.
         */
        @Deprecated
        public DecoratedCustomViewStyle() {
            super();
        }
    }

    /**
     * Notification style for media custom views that are decorated by the system.
     *
     * <p>Instead of providing a media notification that is completely custom, a developer can set
     * this style and still obtain system decorations like the notification header with the expand
     * affordance and actions.
     *
     * <p>Use {@link android.app.Notification.Builder#setCustomContentView(RemoteViews)},
     * {@link android.app.Notification.Builder#setCustomBigContentView(RemoteViews)} and
     * {@link android.app.Notification.Builder#setCustomHeadsUpContentView(RemoteViews)} to set the
     * corresponding custom views to display.
     *
     * <p>To use this style with your Notification, feed it to
     * {@link android.support.v4.app.NotificationCompat.Builder#setStyle(Style)} like so:
     * <pre class="prettyprint">
     * Notification noti = new Notification.Builder()
     *     .setSmallIcon(R.drawable.ic_stat_player)
     *     .setLargeIcon(albumArtBitmap))
     *     .setCustomContentView(contentView)
     *     .setStyle(<b>new NotificationCompat.DecoratedMediaCustomViewStyle()</b>
     *          .setMediaSession(mySession))
     *     .build();
     * </pre>
     *
     * <p>If you are using this style, consider using the corresponding styles like
     * {@link android.support.v7.appcompat.R.style#TextAppearance_AppCompat_Notification_Media} or
     * {@link
     * android.support.v7.appcompat.R.style#TextAppearance_AppCompat_Notification_Title_Media} in
     * your custom views in order to get the correct styling on each platform version.
     *
     * @see android.support.v4.app.NotificationCompat.DecoratedCustomViewStyle
     * @see android.support.v4.media.app.NotificationCompat.MediaStyle
     *
     * @deprecated Use
     * {@link android.support.v4.media.app.NotificationCompat.DecoratedMediaCustomViewStyle} and
     * {@link android.support.mediacompat.R.style#TextAppearance_Compat_Notification_Media} and
     * {@link android.support.mediacompat.R.style#TextAppearance_Compat_Notification_Title_Media}.
     */
    @Deprecated
    public static class DecoratedMediaCustomViewStyle extends
            android.support.v4.media.app.NotificationCompat.DecoratedMediaCustomViewStyle {

        /**
         * @deprecated Use
         * {@link android.support.v4.media.app.NotificationCompat.DecoratedMediaCustomViewStyle
         * #DecoratedMediaCustomViewStyle()}.
         */
        @Deprecated
        public DecoratedMediaCustomViewStyle() {
            super();
        }
    }
}
