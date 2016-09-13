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

import static android.support.annotation.RestrictTo.Scope.GROUP_ID;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.support.annotation.RestrictTo;
import android.support.v4.app.BundleCompat;
import android.support.v4.app.NotificationBuilderWithBuilderAccessor;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.text.BidiFormatter;
import android.support.v7.appcompat.R;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.widget.RemoteViews;

import java.util.List;

/**
 * An extension of {@link android.support.v4.app.NotificationCompat} which supports
 * {@link android.support.v7.app.NotificationCompat.MediaStyle},
 * {@link android.support.v7.app.NotificationCompat.DecoratedCustomViewStyle},
 * and {@link android.support.v7.app.NotificationCompat.DecoratedMediaCustomViewStyle}.
 * You should start using this variant if you need support any of these styles.
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

    private static void addStyleToBuilderApi24(NotificationBuilderWithBuilderAccessor builder,
            android.support.v4.app.NotificationCompat.Builder b) {
        if (b.mStyle instanceof DecoratedCustomViewStyle) {
            NotificationCompatImpl24.addDecoratedCustomViewStyle(builder);
        } else if (b.mStyle instanceof DecoratedMediaCustomViewStyle) {
            NotificationCompatImpl24.addDecoratedMediaCustomViewStyle(builder);
        } else if (!(b.mStyle instanceof MessagingStyle)) {
            addStyleGetContentViewLollipop(builder, b);
        }
    }

    private static RemoteViews addStyleGetContentViewLollipop(
            NotificationBuilderWithBuilderAccessor builder,
            android.support.v4.app.NotificationCompat.Builder b) {
        if (b.mStyle instanceof MediaStyle) {
            MediaStyle mediaStyle = (MediaStyle) b.mStyle;
            NotificationCompatImpl21.addMediaStyle(builder,
                    mediaStyle.mActionsToShowInCompact,
                    mediaStyle.mToken != null ? mediaStyle.mToken.getToken() : null);

            boolean hasContentView = b.getContentView() != null;
            // If we are on L/M the media notification will only be colored if the expanded version
            // is of media style, so we have to create a custom view for the collapsed version as
            // well in that case.
            boolean isMorL = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    && Build.VERSION.SDK_INT <= Build.VERSION_CODES.M;
            boolean createCustomContent = hasContentView
                    || (isMorL && b.getBigContentView() != null);
            if (b.mStyle instanceof DecoratedMediaCustomViewStyle && createCustomContent) {
                RemoteViews contentViewMedia = NotificationCompatImplBase.overrideContentViewMedia(
                        builder, b.mContext, b.mContentTitle, b.mContentText, b.mContentInfo,
                        b.mNumber, b.mLargeIcon, b.mSubText, b.mUseChronometer,
                        b.getWhenIfShowing(), b.getPriority(), b.mActions,
                        mediaStyle.mActionsToShowInCompact, false /* no cancel button on L */,
                        null /* cancelButtonIntent */, hasContentView /* isDecoratedCustomView */);
                if (hasContentView) {
                    NotificationCompatImplBase.buildIntoRemoteViews(b.mContext, contentViewMedia,
                            b.getContentView());
                }
                setBackgroundColor(b.mContext, contentViewMedia, b.getColor());
                return contentViewMedia;
            }
            return null;
        } else if (b.mStyle instanceof DecoratedCustomViewStyle) {
            return getDecoratedContentView(b);
        }
        return addStyleGetContentViewJellybean(builder, b);
    }

    private static RemoteViews addStyleGetContentViewJellybean(
            NotificationBuilderWithBuilderAccessor builder,
            android.support.v4.app.NotificationCompat.Builder b) {
        if (b.mStyle instanceof MessagingStyle) {
            addMessagingFallBackStyle((MessagingStyle) b.mStyle, builder, b);
        }
        return addStyleGetContentViewIcs(builder, b);
    }

    private static MessagingStyle.Message findLatestIncomingMessage(MessagingStyle style) {
        List<MessagingStyle.Message> messages = style.getMessages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            MessagingStyle.Message m = messages.get(i);
            // Incoming messages have a non-empty sender.
            if (!TextUtils.isEmpty(m.getSender())) {
                return m;
            }
        }
        if (!messages.isEmpty()) {
            // No incoming messages, fall back to outgoing message
            return messages.get(messages.size() - 1);
        }
        return null;
    }

    private static CharSequence makeMessageLine(android.support.v4.app.NotificationCompat.Builder b,
            MessagingStyle style,
            MessagingStyle.Message m) {
        BidiFormatter bidi = BidiFormatter.getInstance();
        SpannableStringBuilder sb = new SpannableStringBuilder();
        boolean afterLollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
        int color = afterLollipop || Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1
                ? Color.BLACK : Color.WHITE;
        CharSequence replyName = m.getSender();
        if (TextUtils.isEmpty(m.getSender())) {
            replyName = style.getUserDisplayName() == null
                    ? "" : style.getUserDisplayName();
            color = afterLollipop && b.getColor() != NotificationCompat.COLOR_DEFAULT
                    ? b.getColor()
                    : color;
        }
        CharSequence senderText = bidi.unicodeWrap(replyName);
        sb.append(senderText);
        sb.setSpan(makeFontColorSpan(color),
                sb.length() - senderText.length(),
                sb.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE /* flags */);
        CharSequence text = m.getText() == null ? "" : m.getText();
        sb.append("  ").append(bidi.unicodeWrap(text));
        return sb;
    }

    private static TextAppearanceSpan makeFontColorSpan(int color) {
        return new TextAppearanceSpan(null, 0, 0, ColorStateList.valueOf(color), null);
    }

    private static void addMessagingFallBackStyle(MessagingStyle style,
            NotificationBuilderWithBuilderAccessor builder,
            android.support.v4.app.NotificationCompat.Builder b) {
        SpannableStringBuilder completeMessage = new SpannableStringBuilder();
        List<MessagingStyle.Message> messages = style.getMessages();
        boolean showNames = style.getConversationTitle() != null
                || hasMessagesWithoutSender(style.getMessages());
        for (int i = messages.size() - 1; i >= 0; i--) {
            MessagingStyle.Message m = messages.get(i);
            CharSequence line;
            line = showNames ? makeMessageLine(b, style, m) : m.getText();
            if (i != messages.size() - 1) {
                completeMessage.insert(0, "\n");
            }
            completeMessage.insert(0, line);
        }
        NotificationCompatImplJellybean.addBigTextStyle(builder, completeMessage);
    }

    private static boolean hasMessagesWithoutSender(
            List<MessagingStyle.Message> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            MessagingStyle.Message m = messages.get(i);
            if (m.getSender() == null) {
                return true;
            }
        }
        return false;
    }

    private static RemoteViews addStyleGetContentViewIcs(
            NotificationBuilderWithBuilderAccessor builder,
            android.support.v4.app.NotificationCompat.Builder b) {
        if (b.mStyle instanceof MediaStyle) {
            MediaStyle mediaStyle = (MediaStyle) b.mStyle;
            boolean isDecorated = b.mStyle instanceof DecoratedMediaCustomViewStyle
                    && b.getContentView() != null;
            RemoteViews contentViewMedia = NotificationCompatImplBase.overrideContentViewMedia(
                    builder, b.mContext, b.mContentTitle, b.mContentText, b.mContentInfo, b.mNumber,
                    b.mLargeIcon, b.mSubText, b.mUseChronometer, b.getWhenIfShowing(),
                    b.getPriority(), b.mActions, mediaStyle.mActionsToShowInCompact,
                    mediaStyle.mShowCancelButton, mediaStyle.mCancelButtonIntent, isDecorated);
            if (isDecorated) {
                NotificationCompatImplBase.buildIntoRemoteViews(b.mContext, contentViewMedia,
                        b.getContentView());
                return contentViewMedia;
            }
        } else if (b.mStyle instanceof DecoratedCustomViewStyle) {
            return getDecoratedContentView(b);
        }
        return null;
    }

    private static void addBigStyleToBuilderJellybean(Notification n,
            android.support.v4.app.NotificationCompat.Builder b) {
        if (b.mStyle instanceof MediaStyle) {
            MediaStyle mediaStyle = (MediaStyle) b.mStyle;
            RemoteViews innerView = b.getBigContentView() != null
                    ? b.getBigContentView()
                    : b.getContentView();
            boolean isDecorated = b.mStyle instanceof DecoratedMediaCustomViewStyle
                    && innerView != null;
            NotificationCompatImplBase.overrideMediaBigContentView(n, b.mContext,
                    b.mContentTitle, b.mContentText, b.mContentInfo, b.mNumber, b.mLargeIcon,
                    b.mSubText, b.mUseChronometer, b.getWhenIfShowing(), b.getPriority(), 0,
                    b.mActions, mediaStyle.mShowCancelButton, mediaStyle.mCancelButtonIntent,
                    isDecorated);
            if (isDecorated) {
                NotificationCompatImplBase.buildIntoRemoteViews(b.mContext, n.bigContentView,
                        innerView);
            }
        } else if (b.mStyle instanceof DecoratedCustomViewStyle) {
            addDecoratedBigStyleToBuilder(n, b);
        }
    }

    private static RemoteViews getDecoratedContentView(
            android.support.v4.app.NotificationCompat.Builder b) {
        if (b.getContentView() == null) {
            // No special content view
            return null;
        }
        RemoteViews remoteViews = NotificationCompatImplBase.applyStandardTemplateWithActions(
                b.mContext, b.mContentTitle, b.mContentText, b.mContentInfo, b.mNumber,
                b.mNotification.icon, b.mLargeIcon, b.mSubText, b.mUseChronometer,
                b.getWhenIfShowing(), b.getPriority(), b.getColor(),
                R.layout.notification_template_custom_big, false /* fitIn1U */, null /* actions */);
        NotificationCompatImplBase.buildIntoRemoteViews(b.mContext, remoteViews,
                b.getContentView());
        return remoteViews;
    }

    private static void addDecoratedBigStyleToBuilder(Notification n,
            android.support.v4.app.NotificationCompat.Builder b) {
        RemoteViews bigContentView = b.getBigContentView();
        RemoteViews innerView = bigContentView != null ? bigContentView : b.getContentView();
        if (innerView == null) {
            // No expandable notification
            return;
        }
        RemoteViews remoteViews = NotificationCompatImplBase.applyStandardTemplateWithActions(
                b.mContext, b.mContentTitle, b.mContentText, b.mContentInfo, b.mNumber,
                n.icon ,b.mLargeIcon, b.mSubText, b.mUseChronometer, b.getWhenIfShowing(),
                b.getPriority(), b.getColor(), R.layout.notification_template_custom_big,
                false /* fitIn1U */, b.mActions);
        NotificationCompatImplBase.buildIntoRemoteViews(b.mContext, remoteViews, innerView);
        n.bigContentView = remoteViews;
    }

    private static void addDecoratedHeadsUpToBuilder(Notification n,
            android.support.v4.app.NotificationCompat.Builder b) {
        RemoteViews headsUp = b.getHeadsUpContentView();
        RemoteViews innerView = headsUp != null ? headsUp : b.getContentView();
        if (headsUp == null) {
            // No expandable notification
            return;
        }
        RemoteViews remoteViews = NotificationCompatImplBase.applyStandardTemplateWithActions(
                b.mContext, b.mContentTitle, b.mContentText, b.mContentInfo, b.mNumber, n.icon,
                b.mLargeIcon, b.mSubText, b.mUseChronometer, b.getWhenIfShowing(), b.getPriority(),
                b.getColor(), R.layout.notification_template_custom_big, false /* fitIn1U */,
                b.mActions);
        NotificationCompatImplBase.buildIntoRemoteViews(b.mContext, remoteViews, innerView);
        n.headsUpContentView = remoteViews;
    }

    private static void addBigStyleToBuilderLollipop(Notification n,
            android.support.v4.app.NotificationCompat.Builder b) {
        RemoteViews innerView = b.getBigContentView() != null
                ? b.getBigContentView()
                : b.getContentView();
        if (b.mStyle instanceof DecoratedMediaCustomViewStyle && innerView != null) {
            NotificationCompatImplBase.overrideMediaBigContentView(n, b.mContext,
                    b.mContentTitle, b.mContentText, b.mContentInfo, b.mNumber, b.mLargeIcon,
                    b.mSubText, b.mUseChronometer, b.getWhenIfShowing(), b.getPriority(), 0,
                    b.mActions, false /* showCancelButton */, null /* cancelButtonIntent */,
                    true /* decoratedCustomView */);
                    NotificationCompatImplBase.buildIntoRemoteViews(b.mContext, n.bigContentView,
                            innerView);
            setBackgroundColor(b.mContext, n.bigContentView, b.getColor());
        } else if (b.mStyle instanceof DecoratedCustomViewStyle) {
            addDecoratedBigStyleToBuilder(n, b);
        }
    }

    private static void setBackgroundColor(Context context, RemoteViews views, int color) {
        if (color == COLOR_DEFAULT) {
            color = context.getResources().getColor(
                    R.color.notification_material_background_media_default_color);
        }
        views.setInt(R.id.status_bar_latest_event_content, "setBackgroundColor", color);
    }

    private static void addHeadsUpToBuilderLollipop(Notification n,
            android.support.v4.app.NotificationCompat.Builder b) {
        RemoteViews innerView = b.getHeadsUpContentView() != null
                ? b.getHeadsUpContentView()
                : b.getContentView();
        if (b.mStyle instanceof DecoratedMediaCustomViewStyle && innerView != null) {
            n.headsUpContentView = NotificationCompatImplBase.generateMediaBigView(b.mContext,
                    b.mContentTitle, b.mContentText, b.mContentInfo, b.mNumber,
                    b.mLargeIcon, b.mSubText, b.mUseChronometer, b.getWhenIfShowing(),
                    b.getPriority(), 0, b.mActions, false /* showCancelButton */,
                    null /* cancelButtonIntent */, true /* decoratedCustomView */);
            NotificationCompatImplBase.buildIntoRemoteViews(b.mContext, n.headsUpContentView,
                    innerView);
            setBackgroundColor(b.mContext, n.headsUpContentView, b.getColor());
        } else if (b.mStyle instanceof DecoratedCustomViewStyle) {
            addDecoratedHeadsUpToBuilder(n, b);
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

        /**
         * @return the text of the notification
         *
         * @hide
         */
        @RestrictTo(GROUP_ID)
        @Override
        protected CharSequence resolveText() {
            if (mStyle instanceof MessagingStyle) {
                MessagingStyle style = (MessagingStyle) mStyle;
                MessagingStyle.Message m = findLatestIncomingMessage(style);
                CharSequence conversationTitle = style.getConversationTitle();
                if (m != null) {
                    return conversationTitle != null ? makeMessageLine(this, style, m)
                            : m.getText();
                }
            }
            return super.resolveText();
        }

        /**
         * @return the title of the notification
         *
         * @hide
         */
        @RestrictTo(GROUP_ID)
        @Override
        protected CharSequence resolveTitle() {
            if (mStyle instanceof MessagingStyle) {
                MessagingStyle style = (MessagingStyle) mStyle;
                MessagingStyle.Message m = findLatestIncomingMessage(style);
                CharSequence conversationTitle = style.getConversationTitle();
                if (conversationTitle != null || m != null) {
                    return conversationTitle != null ? conversationTitle : m.getSender();
                }
            }
            return super.resolveTitle();
        }

        /**
         * @hide
         */
        @RestrictTo(GROUP_ID)
        @Override
        protected BuilderExtender getExtender() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return new Api24Extender();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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

        IceCreamSandwichExtender() {
        }

        @Override
        public Notification build(android.support.v4.app.NotificationCompat.Builder b,
                NotificationBuilderWithBuilderAccessor builder) {
            RemoteViews contentView = addStyleGetContentViewIcs(builder, b);
            Notification n = builder.build();
            // The above call might override decorated content views again, let's make sure it
            // sticks.
            if (contentView != null) {
                n.contentView = contentView;
            } else if (b.getContentView() != null) {
                n.contentView = b.getContentView();
            }
            return n;
        }
    }

    private static class JellybeanExtender extends BuilderExtender {

        JellybeanExtender() {
        }

        @Override
        public Notification build(android.support.v4.app.NotificationCompat.Builder b,
                NotificationBuilderWithBuilderAccessor builder) {
            RemoteViews contentView = addStyleGetContentViewJellybean(builder, b);
            Notification n = builder.build();
            // The above call might override decorated content views again, let's make sure it
            // sticks.
            if (contentView != null) {
                n.contentView = contentView;
            }
            addBigStyleToBuilderJellybean(n, b);
            return n;
        }
    }

    private static class LollipopExtender extends BuilderExtender {

        LollipopExtender() {
        }

        @Override
        public Notification build(android.support.v4.app.NotificationCompat.Builder b,
                NotificationBuilderWithBuilderAccessor builder) {
            RemoteViews contentView = addStyleGetContentViewLollipop(builder, b);
            Notification n = builder.build();
            // The above call might override decorated content views again, let's make sure it
            // sticks.
            if (contentView != null) {
                n.contentView = contentView;
            }
            addBigStyleToBuilderLollipop(n, b);
            addHeadsUpToBuilderLollipop(n, b);
            return n;
        }
    }

    private static class Api24Extender extends BuilderExtender {

        @Override
        public Notification build(android.support.v4.app.NotificationCompat.Builder b,
                NotificationBuilderWithBuilderAccessor builder) {
            addStyleToBuilderApi24(builder, b);
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
     *     .setCustomContentView(contentView);
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
     *     .setCustomContentView(contentView);
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
    }
}
