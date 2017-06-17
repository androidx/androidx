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

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.support.annotation.RestrictTo;
import android.support.v4.app.BundleCompat;
import android.support.v4.app.NotificationBuilderWithBuilderAccessor;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.appcompat.R;
import android.widget.RemoteViews;

/**
 * An extension of {@link android.support.v4.app.NotificationCompat} which adds additional styles.
 */
public class NotificationCompat extends android.support.v4.app.NotificationCompat {

    /**
     * Extracts a {@link MediaSessionCompat.Token} from the extra values
     * in the {@link MediaStyle} {@link android.app.Notification notification}.
     *
     * @param notification The notification to extract a {@link MediaSessionCompat.Token} from.
     * @return The {@link MediaSessionCompat.Token} in the {@code notification} if it contains,
     *         null otherwise.
     */
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
     * See {@link android.support.v4.app.NotificationCompat}.
     */
    public static class Builder extends android.support.v4.app.NotificationCompat.Builder {

        /**
         * @inheritDoc
         */
        public Builder(Context context) {
            super(context);
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
         * Requests up to 3 actions (by index in the order of addition) to be shown in the compact
         * notification view.
         *
         * @param actions the indices of the actions to show in the compact notification view
         */
        public MediaStyle setShowActionsInCompactView(int...actions) {
            mActionsToShowInCompact = actions;
            return this;
        }

        /**
         * Attaches a {@link MediaSessionCompat.Token} to this Notification
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
            if (Build.VERSION.SDK_INT < 21) {
                mShowCancelButton = show;
            }
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

        /**
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        @Override
        public void apply(NotificationBuilderWithBuilderAccessor builder) {
            if (Build.VERSION.SDK_INT >= 21) {
                NotificationCompatImpl21.addMediaStyle(builder,
                        mActionsToShowInCompact,
                        mToken != null ? mToken.getToken() : null);
            } else if (mShowCancelButton) {
                builder.getBuilder().setOngoing(true);
            }
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        @Override
        public RemoteViews makeContentView(NotificationBuilderWithBuilderAccessor builder) {
            if (Build.VERSION.SDK_INT >= 21) {
                // No custom content view required
                return null;
            }
            overrideContentView(builder, false);
            return null;
        }

        RemoteViews overrideContentView(
                NotificationBuilderWithBuilderAccessor builder, boolean isDecorated) {
            return NotificationCompatImplBase.overrideContentViewMedia(
                    builder, mBuilder.mContext, mBuilder.mContentTitle, mBuilder.mContentText,
                    mBuilder.mContentInfo, mBuilder.mNumber, mBuilder.mLargeIcon,
                    mBuilder.mSubText, mBuilder.mUseChronometer, mBuilder.getWhenIfShowing(),
                    mBuilder.getPriority(), mBuilder.mActions, mActionsToShowInCompact,
                    mShowCancelButton, mCancelButtonIntent, isDecorated);
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        @Override
        public RemoteViews makeBigContentView(NotificationBuilderWithBuilderAccessor builder) {
            if (Build.VERSION.SDK_INT >= 21) {
                // No custom content view required
                return null;
            }
            return generateBigContentView(false);
        }

        RemoteViews generateBigContentView(boolean isDecorated) {
            return NotificationCompatImplBase.generateMediaBigView(
                    mBuilder.mContext, mBuilder.mContentTitle, mBuilder.mContentText,
                    mBuilder.mContentInfo, mBuilder.mNumber, mBuilder.mLargeIcon,
                    mBuilder.mSubText, mBuilder.mUseChronometer,
                    mBuilder.getWhenIfShowing(), mBuilder.getPriority(), 0,
                    mBuilder.mActions, mShowCancelButton, mCancelButtonIntent,
                    isDecorated);
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
     * {@link NotificationCompat.Builder#setStyle(Style)} like so:
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
     */
    public static class DecoratedCustomViewStyle extends Style {

        public DecoratedCustomViewStyle() {
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        @Override
        public void apply(NotificationBuilderWithBuilderAccessor builder) {
            if (Build.VERSION.SDK_INT >= 24) {
                NotificationCompatImpl24.addDecoratedCustomViewStyle(builder);
            }
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
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
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
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
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
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

        private RemoteViews createRemoteViews(RemoteViews innerView, boolean showActions) {
            RemoteViews remoteViews = NotificationCompatImplBase.applyStandardTemplateWithActions(
                    mBuilder.mContext, mBuilder.mContentTitle, mBuilder.mContentText,
                    mBuilder.mContentInfo, mBuilder.mNumber, mBuilder.mNotification.icon,
                    mBuilder.mLargeIcon, mBuilder.mSubText, mBuilder.mUseChronometer,
                    mBuilder.getWhenIfShowing(), mBuilder.getPriority(), mBuilder.getColor(),
                    R.layout.notification_template_custom_big,
                    false /* fitIn1U */, showActions ? mBuilder.mActions : null);
            NotificationCompatImplBase.buildIntoRemoteViews(mBuilder.mContext,
                    remoteViews, innerView);
            return remoteViews;
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
     * {@link NotificationCompat.Builder#setStyle(Style)} like so:
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
     * @see DecoratedCustomViewStyle
     * @see MediaStyle
     */
    public static class DecoratedMediaCustomViewStyle extends MediaStyle {

        public DecoratedMediaCustomViewStyle() {
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        @Override
        public void apply(NotificationBuilderWithBuilderAccessor builder) {
            if (Build.VERSION.SDK_INT >= 24) {
                NotificationCompatImpl24.addDecoratedMediaCustomViewStyle(builder,
                        mActionsToShowInCompact,
                        mToken != null ? mToken.getToken() : null);
            } else {
                super.apply(builder);
            }
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        @Override
        public RemoteViews makeContentView(NotificationBuilderWithBuilderAccessor builder) {
            if (Build.VERSION.SDK_INT >= 24) {
                // No custom content view required
                return null;
            }
            boolean hasContentView = mBuilder.getContentView() != null;
            if (Build.VERSION.SDK_INT >= 21) {
                // If we are on L/M the media notification will only be colored if the expanded
                // version is of media style, so we have to create a custom view for the collapsed
                // version as well in that case.
                boolean createCustomContent = hasContentView
                        || mBuilder.getBigContentView() != null;
                if (createCustomContent) {
                    RemoteViews contentView = overrideContentView(builder, hasContentView);
                    if (hasContentView) {
                        NotificationCompatImplBase.buildIntoRemoteViews(mBuilder.mContext,
                                contentView,
                                mBuilder.getContentView());
                    }
                    setBackgroundColor(contentView);
                    return contentView;
                }
            } else {
                RemoteViews contentViewMedia = overrideContentView(builder, hasContentView);
                if (hasContentView) {
                    NotificationCompatImplBase.buildIntoRemoteViews(mBuilder.mContext,
                            contentViewMedia,
                            mBuilder.getContentView());
                    return contentViewMedia;
                }
            }
            return null;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        @Override
        public RemoteViews makeBigContentView(NotificationBuilderWithBuilderAccessor builder) {
            if (Build.VERSION.SDK_INT >= 24) {
                // No custom big content view required
                return null;
            }
            RemoteViews innerView = mBuilder.getBigContentView() != null
                    ? mBuilder.getBigContentView()
                    : mBuilder.getContentView();
            if (innerView == null) {
                // No expandable notification
                return null;
            }
            RemoteViews bigContentView = generateBigContentView(true);
            NotificationCompatImplBase.buildIntoRemoteViews(mBuilder.mContext,
                    bigContentView,
                    innerView);
            if (Build.VERSION.SDK_INT >= 21) {
                setBackgroundColor(bigContentView);
            }
            return bigContentView;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        @Override
        public RemoteViews makeHeadsUpContentView(NotificationBuilderWithBuilderAccessor builder) {
            if (Build.VERSION.SDK_INT >= 24) {
                // No custom heads up content view required
                return null;
            }
            RemoteViews innerView = mBuilder.getHeadsUpContentView() != null
                    ? mBuilder.getHeadsUpContentView()
                    : mBuilder.getContentView();
            if (innerView == null) {
                // No expandable notification
                return null;
            }
            RemoteViews headsUpContentView = generateBigContentView(true);
            NotificationCompatImplBase.buildIntoRemoteViews(mBuilder.mContext,
                    headsUpContentView,
                    innerView);
            if (Build.VERSION.SDK_INT >= 21) {
                setBackgroundColor(headsUpContentView);
            }
            return headsUpContentView;
        }

        private void setBackgroundColor(RemoteViews views) {
            int color = mBuilder.getColor() != COLOR_DEFAULT
                    ? mBuilder.getColor()
                    : mBuilder.mContext.getResources().getColor(
                        R.color.notification_material_background_media_default_color);
            views.setInt(R.id.status_bar_latest_event_content, "setBackgroundColor", color);
        }
    }
}
