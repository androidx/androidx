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
import android.support.v4.app.NotificationBuilderWithBuilderAccessor;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.internal.app.NotificationCompatImpl21;
import android.support.v7.internal.app.NotificationCompatImplBase;

/**
 * An extension of {@link android.support.v4.app.NotificationCompat} which supports
 * {@link android.support.v7.app.NotificationCompat.MediaStyle}. You should start using this variant
 * if you need support for media styled notifications.
 */
public class NotificationCompat extends android.support.v4.app.NotificationCompat {

    private static void addMediaStyleToBuilderLollipop(
            NotificationBuilderWithBuilderAccessor builder, android.support.v4.app.NotificationCompat.Style style) {
        if (style instanceof MediaStyle) {
            MediaStyle mediaStyle = (MediaStyle) style;
            NotificationCompatImpl21.addMediaStyle(builder,
                    mediaStyle.mActionsToShowInCompact,
                    mediaStyle.mToken != null ? mediaStyle.mToken.getToken() : null);
        }
    }

    private static void addMediaStyleToBuilderIcs(NotificationBuilderWithBuilderAccessor builder,
            android.support.v4.app.NotificationCompat.Builder b) {
        if (b.mStyle instanceof MediaStyle) {
            MediaStyle mediaStyle = (MediaStyle) b.mStyle;
            NotificationCompatImplBase.overrideContentView(builder, b.mContext,
                    b.mContentTitle,
                    b.mContentText, b.mContentInfo, b.mNumber, b.mLargeIcon, b.mSubText,
                    b.mUseChronometer, b.mNotification.when, b.mActions,
                    mediaStyle.mActionsToShowInCompact, mediaStyle.mShowCancelButton,
                    mediaStyle.mCancelButtonIntent);
        }
    }

    private static void addBigMediaStyleToBuilderJellybean(Notification n,
            android.support.v4.app.NotificationCompat.Builder b) {
        if (b.mStyle instanceof MediaStyle) {
            MediaStyle mediaStyle = (MediaStyle) b.mStyle;
            NotificationCompatImplBase.overrideBigContentView(n, b.mContext,
                    b.mContentTitle,
                    b.mContentText, b.mContentInfo, b.mNumber, b.mLargeIcon, b.mSubText,
                    b.mUseChronometer, b.mNotification.when, b.mActions,
                    mediaStyle.mShowCancelButton, mediaStyle.mCancelButtonIntent);
        }
    }

    /**
     * See {@link android.support.v4.app.NotificationCompat}. In addition to the builder in v4, this
     * builder also supports {@link MediaStyle}.
     */
    public static class Builder extends android.support.v4.app.NotificationCompat.Builder {

        /**
         * @inheritDoc
         */
        public Builder(Context context) {
            super(context);
        }

        @Override
        protected BuilderExtender getExtender() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return new LollipopExtender();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                return new JellybeanExtender();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                return new IceCreamSandwichExtender();
            } else {
                return super.getExtender();
            }
        }
    }

    private static class IceCreamSandwichExtender extends BuilderExtender {

        @Override
        public Notification build(android.support.v4.app.NotificationCompat.Builder b,
                NotificationBuilderWithBuilderAccessor builder) {
            addMediaStyleToBuilderIcs(builder, b);
            return builder.build();
        }
    }

    private static class JellybeanExtender extends BuilderExtender {

        @Override
        public Notification build(android.support.v4.app.NotificationCompat.Builder b,
                NotificationBuilderWithBuilderAccessor builder) {
            addMediaStyleToBuilderIcs(builder, b);
            Notification n = builder.build();
            addBigMediaStyleToBuilderJellybean(n, b);
            return n;
        }
    }

    private static class LollipopExtender extends BuilderExtender {

        @Override
        public Notification build(android.support.v4.app.NotificationCompat.Builder b,
                NotificationBuilderWithBuilderAccessor builder) {
            addMediaStyleToBuilderLollipop(builder, b.mStyle);
            return builder.build();
        }
    }

    /**
     * Notification style for media playback notifications.
     *
     * In the expanded form, {@link Notification#bigContentView}, up to 5
     * {@link android.support.v4.app.NotificationCompat.Action}s specified with
     * {@link NotificationCompat.Builder#addAction(int, CharSequence, PendingIntent) addAction} will
     * be shown as icon-only pushbuttons, suitable for transport controls. The Bitmap given to
     * {@link NotificationCompat.Builder#setLargeIcon(android.graphics.Bitmap) setLargeIcon()} will
     * be treated as album artwork.
     *
     * Unlike the other styles provided here, MediaStyle can also modify the standard-size
     * {@link Notification#contentView}; by providing action indices to
     * {@link #setShowActionsInCompactView(int...)} you can promote up to 3 actions to be displayed
     * in the standard view alongside the usual content.
     *
     * Notifications created with MediaStyle will have their category set to
     * {@link Notification#CATEGORY_TRANSPORT CATEGORY_TRANSPORT} unless you set a different
     * category using {@link NotificationCompat.Builder#setCategory(String) setCategory()}.
     *
     * Finally, if you attach a {@link android.media.session.MediaSession.Token} using
     * {@link android.support.v7.app.NotificationCompat.MediaStyle#setMediaSession}, the System UI
     * can identify this as a notification representing an active media session and respond
     * accordingly (by showing album artwork in the lockscreen, for example).
     *
     * To use this style with your Notification, feed it to
     * {@link NotificationCompat.Builder#setStyle} like so:
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
     * @see Notification#bigContentView
     */
    public static class MediaStyle extends android.support.v4.app.NotificationCompat.Style {

        int[] mActionsToShowInCompact = null;
        MediaSessionCompat.Token mToken;
        boolean mShowCancelButton;
        PendingIntent mCancelButtonIntent;

        public MediaStyle() {
        }

        public MediaStyle(android.support.v4.app.NotificationCompat.Builder builder) {
            setBuilder(builder);
        }

        /**
         * Request up to 3 actions (by index in the order of addition) to be shown in the compact
         * notification view.
         *
         * @param actions the indices of the actions to show in the compact notification view
         */
        public MediaStyle setShowActionsInCompactView(int...actions) {
            mActionsToShowInCompact = actions;
            return this;
        }

        /**
         * Attach a {@link MediaSessionCompat.Token} to this Notification
         * to provide additional playback information and control to the SystemUI.
         */
        public MediaStyle setMediaSession(MediaSessionCompat.Token token) {
            mToken = token;
            return this;
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
         */
        public MediaStyle setShowCancelButton(boolean show) {
            mShowCancelButton = show;
            return this;
        }

        /**
         * Sets the pending intent to be sent when the cancel button is pressed. See {@link
         * #setShowCancelButton}.
         *
         * @param pendingIntent the intent to be sent when the cancel button is pressed
         */
        public MediaStyle setCancelButtonIntent(PendingIntent pendingIntent) {
            mCancelButtonIntent = pendingIntent;
            return this;
        }
    }
}
