/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.v4.media.app;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static android.support.v4.app.NotificationCompat.COLOR_DEFAULT;

import android.app.Notification;
import android.app.PendingIntent;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.mediacompat.R;
import android.support.v4.app.BundleCompat;
import android.support.v4.app.NotificationBuilderWithBuilderAccessor;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.View;
import android.widget.RemoteViews;

/**
 * Class containing media specfic {@link android.support.v4.app.NotificationCompat.Style styles}
 * that you can use with {@link android.support.v4.app.NotificationCompat.Builder#setStyle}.
 */
public class NotificationCompat {

    private NotificationCompat() {
    }

    /**
     * Notification style for media playback notifications.
     *
     * In the expanded form, up to 5
     * {@link android.support.v4.app.NotificationCompat.Action actions} specified with
     * {@link android.support.v4.app.NotificationCompat.Builder
     * #addAction(int, CharSequence, PendingIntent) addAction} will be shown as icon-only
     * pushbuttons, suitable for transport controls. The Bitmap given to
     * {@link android.support.v4.app.NotificationCompat.Builder
     * #setLargeIcon(android.graphics.Bitmap) setLargeIcon()} will
     * be treated as album artwork.
     *
     * Unlike the other styles provided here, MediaStyle can also modify the standard-size
     * content view; by providing action indices to
     * {@link #setShowActionsInCompactView(int...)} you can promote up to 3 actions to be displayed
     * in the standard view alongside the usual content.
     *
     * Notifications created with MediaStyle will have their category set to
     * {@link android.support.v4.app.NotificationCompat#CATEGORY_TRANSPORT CATEGORY_TRANSPORT}
     * unless you set a different category using
     * {@link android.support.v4.app.NotificationCompat.Builder#setCategory(String)
     * setCategory()}.
     *
     * Finally, if you attach a {@link MediaSession.Token} using
     * {@link android.support.v4.media.app.NotificationCompat.MediaStyle#setMediaSession}, the
     * System UI can identify this as a notification representing an active media session and
     * respond accordingly (by showing album artwork in the lockscreen, for example).
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
     * @see Notification#bigContentView
     */
    public static class MediaStyle extends android.support.v4.app.NotificationCompat.Style {

        /**
         * Extracts a {@link MediaSessionCompat.Token} from the extra values
         * in the {@link MediaStyle} {@link Notification notification}.
         *
         * @param notification The notification to extract a {@link MediaSessionCompat.Token} from.
         * @return The {@link MediaSessionCompat.Token} in the {@code notification} if it contains,
         *         null otherwise.
         */
        public static MediaSessionCompat.Token getMediaSession(Notification notification) {
            Bundle extras = android.support.v4.app.NotificationCompat.getExtras(notification);
            if (extras != null) {
                if (Build.VERSION.SDK_INT >= 21) {
                    Object tokenInner = extras.getParcelable(
                            android.support.v4.app.NotificationCompat.EXTRA_MEDIA_SESSION);
                    if (tokenInner != null) {
                        return MediaSessionCompat.Token.fromToken(tokenInner);
                    }
                } else {
                    IBinder tokenInner = BundleCompat.getBinder(extras,
                            android.support.v4.app.NotificationCompat.EXTRA_MEDIA_SESSION);
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

        private static final int MAX_MEDIA_BUTTONS_IN_COMPACT = 3;
        private static final int MAX_MEDIA_BUTTONS = 5;

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
                builder.getBuilder().setStyle(
                        fillInMediaStyle(new Notification.MediaStyle()));
            } else if (mShowCancelButton) {
                builder.getBuilder().setOngoing(true);
            }
        }

        @RequiresApi(21)
        Notification.MediaStyle fillInMediaStyle(Notification.MediaStyle style) {
            if (mActionsToShowInCompact != null) {
                style.setShowActionsInCompactView(mActionsToShowInCompact);
            }
            if (mToken != null) {
                style.setMediaSession((MediaSession.Token) mToken.getToken());
            }
            return style;
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
            return generateContentView();
        }

        RemoteViews generateContentView() {
            RemoteViews view = applyStandardTemplate(false /* showSmallIcon */,
                    getContentViewLayoutResource(), true /* fitIn1U */);

            final int numActions = mBuilder.mActions.size();
            final int numActionsInCompact = mActionsToShowInCompact == null
                    ? 0
                    : Math.min(mActionsToShowInCompact.length, MAX_MEDIA_BUTTONS_IN_COMPACT);
            view.removeAllViews(R.id.media_actions);
            if (numActionsInCompact > 0) {
                for (int i = 0; i < numActionsInCompact; i++) {
                    if (i >= numActions) {
                        throw new IllegalArgumentException(String.format(
                                "setShowActionsInCompactView: action %d out of bounds (max %d)",
                                i, numActions - 1));
                    }

                    final android.support.v4.app.NotificationCompat.Action action =
                            mBuilder.mActions.get(mActionsToShowInCompact[i]);
                    final RemoteViews button = generateMediaActionButton(action);
                    view.addView(R.id.media_actions, button);
                }
            }
            if (mShowCancelButton) {
                view.setViewVisibility(R.id.end_padder, View.GONE);
                view.setViewVisibility(R.id.cancel_action, View.VISIBLE);
                view.setOnClickPendingIntent(R.id.cancel_action, mCancelButtonIntent);
                view.setInt(R.id.cancel_action, "setAlpha", mBuilder.mContext
                        .getResources().getInteger(R.integer.cancel_button_image_alpha));
            } else {
                view.setViewVisibility(R.id.end_padder, View.VISIBLE);
                view.setViewVisibility(R.id.cancel_action, View.GONE);
            }
            return view;
        }

        private RemoteViews generateMediaActionButton(
                android.support.v4.app.NotificationCompat.Action action) {
            final boolean tombstone = (action.getActionIntent() == null);
            RemoteViews button = new RemoteViews(mBuilder.mContext.getPackageName(),
                    R.layout.notification_media_action);
            button.setImageViewResource(R.id.action0, action.getIcon());
            if (!tombstone) {
                button.setOnClickPendingIntent(R.id.action0, action.getActionIntent());
            }
            if (Build.VERSION.SDK_INT >= 15) {
                button.setContentDescription(R.id.action0, action.getTitle());
            }
            return button;
        }

        int getContentViewLayoutResource() {
            return R.layout.notification_template_media;
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
            return generateBigContentView();
        }

        RemoteViews generateBigContentView() {
            final int actionCount = Math.min(mBuilder.mActions.size(), MAX_MEDIA_BUTTONS);
            RemoteViews big = applyStandardTemplate(false /* showSmallIcon */,
                    getBigContentViewLayoutResource(actionCount), false /* fitIn1U */);

            big.removeAllViews(R.id.media_actions);
            if (actionCount > 0) {
                for (int i = 0; i < actionCount; i++) {
                    final RemoteViews button = generateMediaActionButton(mBuilder.mActions.get(i));
                    big.addView(R.id.media_actions, button);
                }
            }
            if (mShowCancelButton) {
                big.setViewVisibility(R.id.cancel_action, View.VISIBLE);
                big.setInt(R.id.cancel_action, "setAlpha", mBuilder.mContext
                        .getResources().getInteger(R.integer.cancel_button_image_alpha));
                big.setOnClickPendingIntent(R.id.cancel_action, mCancelButtonIntent);
            } else {
                big.setViewVisibility(R.id.cancel_action, View.GONE);
            }
            return big;
        }

        int getBigContentViewLayoutResource(int actionCount) {
            return actionCount <= 3
                    ? R.layout.notification_template_big_media_narrow
                    : R.layout.notification_template_big_media;
        }
    }

    /**
     * Notification style for media custom views that are decorated by the system.
     *
     * <p>Instead of providing a media notification that is completely custom, a developer can set
     * this style and still obtain system decorations like the notification header with the expand
     * affordance and actions.
     *
     * <p>Use {@link android.support.v4.app.NotificationCompat.Builder
     * #setCustomContentView(RemoteViews)},
     * {@link android.support.v4.app.NotificationCompat.Builder
     * #setCustomBigContentView(RemoteViews)} and
     * {@link android.support.v4.app.NotificationCompat.Builder
     * #setCustomHeadsUpContentView(RemoteViews)} to set the
     * corresponding custom views to display.
     *
     * <p>To use this style with your Notification, feed it to
     * {@link android.support.v4.app.NotificationCompat.Builder
     * #setStyle(android.support.v4.app.NotificationCompat.Style)} like so:
     * <pre class="prettyprint">
     * Notification noti = new NotificationCompat.Builder()
     *     .setSmallIcon(R.drawable.ic_stat_player)
     *     .setLargeIcon(albumArtBitmap))
     *     .setCustomContentView(contentView)
     *     .setStyle(<b>new NotificationCompat.DecoratedMediaCustomViewStyle()</b>
     *          .setMediaSession(mySession))
     *     .build();
     * </pre>
     *
     * <p>If you are using this style, consider using the corresponding styles like
     * {@link android.support.mediacompat.R.style#TextAppearance_Compat_Notification_Media} or
     * {@link
     * android.support.mediacompat.R.style#TextAppearance_Compat_Notification_Title_Media} in
     * your custom views in order to get the correct styling on each platform version.
     *
     * @see android.support.v4.app.NotificationCompat.DecoratedCustomViewStyle
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
                builder.getBuilder().setStyle(
                        fillInMediaStyle(new Notification.DecoratedMediaCustomViewStyle()));
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
                    RemoteViews contentView = generateContentView();
                    if (hasContentView) {
                        buildIntoRemoteViews(contentView, mBuilder.getContentView());
                    }
                    setBackgroundColor(contentView);
                    return contentView;
                }
            } else {
                RemoteViews contentView = generateContentView();
                if (hasContentView) {
                    buildIntoRemoteViews(contentView, mBuilder.getContentView());
                    return contentView;
                }
            }
            return null;
        }

        @Override
        int getContentViewLayoutResource() {
            return mBuilder.getContentView() != null
                    ? R.layout.notification_template_media_custom
                    : super.getContentViewLayoutResource();
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
            RemoteViews bigContentView = generateBigContentView();
            buildIntoRemoteViews(bigContentView, innerView);
            if (Build.VERSION.SDK_INT >= 21) {
                setBackgroundColor(bigContentView);
            }
            return bigContentView;
        }

        @Override
        int getBigContentViewLayoutResource(int actionCount) {
            return actionCount <= 3
                    ? R.layout.notification_template_big_media_narrow_custom
                    : R.layout.notification_template_big_media_custom;
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
            RemoteViews headsUpContentView = generateBigContentView();
            buildIntoRemoteViews(headsUpContentView, innerView);
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
