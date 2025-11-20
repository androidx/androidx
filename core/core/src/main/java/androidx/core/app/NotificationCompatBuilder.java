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

package androidx.core.app;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;
import static androidx.core.app.NotificationCompat.DEFAULT_SOUND;
import static androidx.core.app.NotificationCompat.DEFAULT_VIBRATE;
import static androidx.core.app.NotificationCompat.FLAG_GROUP_SUMMARY;
import static androidx.core.app.NotificationCompat.GROUP_ALERT_ALL;
import static androidx.core.app.NotificationCompat.GROUP_ALERT_CHILDREN;
import static androidx.core.app.NotificationCompat.GROUP_ALERT_SUMMARY;

import android.app.Notification;
import android.content.Context;
import android.content.LocusId;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.RemoteViews;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.collection.ArraySet;
import androidx.core.graphics.drawable.IconCompat;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link Notification.Builder} that works in a backwards compatible way.
 *
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
class NotificationCompatBuilder implements NotificationBuilderWithBuilderAccessor {
    private final Context mContext;
    private final Notification.Builder mBuilder;
    private final NotificationCompat.Builder mBuilderCompat;

    private RemoteViews mContentView;
    private RemoteViews mBigContentView;
    private final Bundle mExtras = new Bundle();
    private int mGroupAlertBehavior;
    private RemoteViews mHeadsUpContentView;

    @SuppressWarnings({"deprecation", "MissingPermission"})
    NotificationCompatBuilder(NotificationCompat.Builder b) {
        mBuilderCompat = b;
        mContext = b.mContext;
        if (Build.VERSION.SDK_INT >= 26) {
            mBuilder = Api26Impl.createBuilder(b.mContext, b.mChannelId);
        } else {
            mBuilder = new Notification.Builder(b.mContext);
        }
        Notification n = b.mNotification;
        mBuilder.setWhen(n.when)
                .setSmallIcon(n.icon, n.iconLevel)
                .setContent(n.contentView)
                .setTicker(n.tickerText, b.mTickerView)
                .setVibrate(n.vibrate)
                .setLights(n.ledARGB, n.ledOnMS, n.ledOffMS)
                .setOngoing((n.flags & Notification.FLAG_ONGOING_EVENT) != 0)
                .setOnlyAlertOnce((n.flags & Notification.FLAG_ONLY_ALERT_ONCE) != 0)
                .setAutoCancel((n.flags & Notification.FLAG_AUTO_CANCEL) != 0)
                .setDefaults(n.defaults)
                .setContentTitle(b.mContentTitle)
                .setContentText(b.mContentText)
                .setContentInfo(b.mContentInfo)
                .setContentIntent(b.mContentIntent)
                .setDeleteIntent(n.deleteIntent)
                .setFullScreenIntent(b.mFullScreenIntent,
                        (n.flags & Notification.FLAG_HIGH_PRIORITY) != 0)
                .setNumber(b.mNumber)
                .setProgress(b.mProgressMax, b.mProgress, b.mProgressIndeterminate);
        mBuilder.setLargeIcon(b.mLargeIcon == null ? null : b.mLargeIcon.toIcon(mContext));

        Notification.Builder builder = mBuilder.setSubText(b.mSubText);
        Notification.Builder builder1 = builder.setUsesChronometer(b.mUseChronometer);
        builder1.setPriority(b.mPriority);

        // CallStyle notifications add special actions in pre-specified positions, in addition
        // to any provided custom actions. Because there's no way to remove Actions once they're
        // added to Notification.Builder in Versions < 24, we add them here where we have
        // access to NotificationCompatBuilder, rather than in CallStyle.apply where we have
        // to add to the Notification.Builder directly.
        if (b.mStyle instanceof NotificationCompat.CallStyle) {
            // Retrieves call style actions, including contextual and system actions.
            List<NotificationCompat.Action> actionsList =
                    ((NotificationCompat.CallStyle) b.mStyle).getActionsListWithSystemActions();
            // Adds the actions to the builder in the proper order.
            for (NotificationCompat.Action action : actionsList) {
                addAction(action);
            }
        } else {
            for (NotificationCompat.Action action : b.mActions) {
                addAction(action);
            }
        }

        if (b.mExtras != null) {
            mExtras.putAll(b.mExtras);
        }

        mContentView = b.mContentView;
        mBigContentView = b.mBigContentView;
        mBuilder.setShowWhen(b.mShowWhen);
        mBuilder.setLocalOnly(b.mLocalOnly);
        mBuilder.setGroup(b.mGroupKey);
        mBuilder.setSortKey(b.mSortKey);
        mBuilder.setGroupSummary(b.mGroupSummary);
        mGroupAlertBehavior = b.mGroupAlertBehavior;
        mBuilder.setCategory(b.mCategory);
        mBuilder.setColor(b.mColor);
        mBuilder.setVisibility(b.mVisibility);
        mBuilder.setPublicVersion(b.mPublicVersion);
        mBuilder.setSound(n.sound, n.audioAttributes);

        final List<String> people;
        if (Build.VERSION.SDK_INT < 28) {
            people = combineLists(getPeople(b.mPersonList), b.mPeople);
        } else {
            people = b.mPeople;
        }
        if (people != null && !people.isEmpty()) {
            for (String person : people) {
                mBuilder.addPerson(person);
            }
        }

        mHeadsUpContentView = b.mHeadsUpContentView;

        if (b.mInvisibleActions.size() > 0) {
            // Invisible actions should be stored in the extender so we need to check if one
            // exists already.
            Bundle carExtenderBundle =
                    b.getExtras().getBundle(NotificationCompat.CarExtender.EXTRA_CAR_EXTENDER);
            if (carExtenderBundle == null) {
                carExtenderBundle = new Bundle();
            }
            Bundle extenderBundleCopy = new Bundle(carExtenderBundle);
            Bundle listBundle = new Bundle();
            for (int i = 0; i < b.mInvisibleActions.size(); i++) {
                listBundle.putBundle(
                        Integer.toString(i),
                        NotificationCompatJellybean.getBundleForAction(
                                b.mInvisibleActions.get(i)));
            }
            carExtenderBundle.putBundle(
                    NotificationCompat.CarExtender.EXTRA_INVISIBLE_ACTIONS, listBundle);
            extenderBundleCopy.putBundle(
                    NotificationCompat.CarExtender.EXTRA_INVISIBLE_ACTIONS, listBundle);
            b.getExtras().putBundle(
                    NotificationCompat.CarExtender.EXTRA_CAR_EXTENDER, carExtenderBundle);
            mExtras.putBundle(
                    NotificationCompat.CarExtender.EXTRA_CAR_EXTENDER, extenderBundleCopy);
        }
        if (b.mSmallIcon != null) {
            mBuilder.setSmallIcon((Icon) b.mSmallIcon);
        }
        if (Build.VERSION.SDK_INT >= 24) {
            mBuilder.setExtras(b.mExtras);
            Api24Impl.setRemoteInputHistory(mBuilder, b.mRemoteInputHistory);
            if (b.mContentView != null) {
                Api24Impl.setCustomContentView(mBuilder, b.mContentView);
            }
            if (b.mBigContentView != null) {
                Api24Impl.setCustomBigContentView(mBuilder, b.mBigContentView);
            }
            if (b.mHeadsUpContentView != null) {
                Api24Impl.setCustomHeadsUpContentView(mBuilder, b.mHeadsUpContentView);
            }
        }

        if (Build.VERSION.SDK_INT >= 26) {
            Api26Impl.setBadgeIconType(mBuilder, b.mBadgeIcon);
            Api26Impl.setSettingsText(mBuilder, b.mSettingsText);
            Api26Impl.setShortcutId(mBuilder, b.mShortcutId);
            Api26Impl.setTimeoutAfter(mBuilder, b.mTimeout);
            Api26Impl.setGroupAlertBehavior(mBuilder, b.mGroupAlertBehavior);
            if (b.mColorizedSet) {
                Api26Impl.setColorized(mBuilder, b.mColorized);
            }

            if (!TextUtils.isEmpty(b.mChannelId)) {
                mBuilder.setSound(null)
                        .setDefaults(0)
                        .setLights(0, 0, 0)
                        .setVibrate(null);
            }
        }
        if (Build.VERSION.SDK_INT >= 28) {
            for (Person p : b.mPersonList) {
                Api28Impl.addPerson(mBuilder, p.toAndroidPerson());
            }
        }
        if (Build.VERSION.SDK_INT >= 29) {
            Api29Impl.setAllowSystemGeneratedContextualActions(mBuilder,
                    b.mAllowSystemGeneratedContextualActions);
            // TODO: Consider roundtripping NotificationCompat.BubbleMetadata on pre-Q platforms.
            Api29Impl.setBubbleMetadata(mBuilder,
                    NotificationCompat.BubbleMetadata.toPlatform(b.mBubbleMetadata));
            if (b.mLocusId != null) {
                Api29Impl.setLocusId(mBuilder, b.mLocusId.toLocusId());
            }
        }
        if (Build.VERSION.SDK_INT >= 31) {
            if (b.mFgsDeferBehavior != NotificationCompat.FOREGROUND_SERVICE_DEFAULT) {
                Api31Impl.setForegroundServiceBehavior(mBuilder, b.mFgsDeferBehavior);
            }
        }

        if (Build.VERSION.SDK_INT >= 36) {
            Api36Impl.setShortCriticalText(mBuilder, b.mShortCriticalText);
        }

        if (b.mSilent) {
            if (mBuilderCompat.mGroupSummary) {
                mGroupAlertBehavior = GROUP_ALERT_CHILDREN;
            } else {
                mGroupAlertBehavior = GROUP_ALERT_SUMMARY;
            }

            mBuilder.setVibrate(null);
            mBuilder.setSound(null);
            n.defaults &= ~DEFAULT_SOUND;
            n.defaults &= ~DEFAULT_VIBRATE;
            mBuilder.setDefaults(n.defaults);

            if (Build.VERSION.SDK_INT >= 26) {
                if (TextUtils.isEmpty(mBuilderCompat.mGroupKey)) {
                    mBuilder.setGroup(NotificationCompat.GROUP_KEY_SILENT);
                }
                Api26Impl.setGroupAlertBehavior(mBuilder, mGroupAlertBehavior);
            }
        }
    }

    private static @Nullable List<String> combineLists(final @Nullable List<String> first,
            final @Nullable List<String> second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        final ArraySet<String> people = new ArraySet<>(first.size() + second.size());
        people.addAll(first);
        people.addAll(second);
        return new ArrayList<>(people);
    }

    private static @Nullable List<String> getPeople(final @Nullable List<Person> people) {
        if (people == null) {
            return null;
        }
        final ArrayList<String> result = new ArrayList<>(people.size());
        for (Person person : people) {
            result.add(person.resolveToLegacyUri());
        }
        return result;
    }

    @Override
    public Notification.Builder getBuilder() {
        return mBuilder;
    }

    Context getContext() {
        return mContext;
    }

    public Notification build() {
        final NotificationCompat.Style style = mBuilderCompat.mStyle;
        if (style != null) {
            style.apply(this);
        }

        RemoteViews styleContentView = style != null
                ? style.makeContentView(this)
                : null;
        Notification n = buildInternal();
        if (styleContentView != null) {
            n.contentView = styleContentView;
        } else if (mBuilderCompat.mContentView != null) {
            n.contentView = mBuilderCompat.mContentView;
        }
        if (style != null) {
            RemoteViews styleBigContentView = style.makeBigContentView(this);
            if (styleBigContentView != null) {
                n.bigContentView = styleBigContentView;
            }
        }
        if (style != null) {
            RemoteViews styleHeadsUpContentView =
                    mBuilderCompat.mStyle.makeHeadsUpContentView(this);
            if (styleHeadsUpContentView != null) {
                n.headsUpContentView = styleHeadsUpContentView;
            }
        }

        if (style != null) {
            Bundle extras = NotificationCompat.getExtras(n);
            if (extras != null) {
                style.addCompatExtras(extras);
            }
        }

        return n;
    }

    private void addAction(NotificationCompat.Action action) {
        IconCompat iconCompat = action.getIconCompat();
        Notification.Action.Builder actionBuilder = new Notification.Action.Builder(
                iconCompat != null ? iconCompat.toIcon() : null, action.getTitle(),
                action.getActionIntent());
        if (action.getRemoteInputs() != null) {
            for (android.app.RemoteInput remoteInput : RemoteInput.fromCompat(
                    action.getRemoteInputs())) {
                actionBuilder.addRemoteInput(remoteInput);
            }
        }
        Bundle actionExtras;
        if (action.getExtras() != null) {
            actionExtras = new Bundle(action.getExtras());
        } else {
            actionExtras = new Bundle();
        }
        actionExtras.putBoolean(NotificationCompatJellybean.EXTRA_ALLOW_GENERATED_REPLIES,
                action.getAllowGeneratedReplies());
        if (Build.VERSION.SDK_INT >= 24) {
            Api24Impl.setAllowGeneratedReplies(actionBuilder,
                    action.getAllowGeneratedReplies());
        }

        actionExtras.putInt(NotificationCompat.Action.EXTRA_SEMANTIC_ACTION,
                action.getSemanticAction());
        if (Build.VERSION.SDK_INT >= 28) {
            Api28Impl.setSemanticAction(actionBuilder, action.getSemanticAction());
        }

        if (Build.VERSION.SDK_INT >= 29) {
            Api29Impl.setContextual(actionBuilder, action.isContextual());
        }

        if (Build.VERSION.SDK_INT >= 31) {
            Api31Impl.setAuthenticationRequired(actionBuilder,
                    action.isAuthenticationRequired());
        }

        actionExtras.putBoolean(NotificationCompat.Action.EXTRA_SHOWS_USER_INTERFACE,
                action.getShowsUserInterface());
        actionBuilder.addExtras(actionExtras);
        mBuilder.addAction(actionBuilder.build());
    }

    @SuppressWarnings("deprecation")
    protected Notification buildInternal() {
        if (Build.VERSION.SDK_INT >= 26) {
            return mBuilder.build();
        } else if (Build.VERSION.SDK_INT >= 24) {
            Notification notification = mBuilder.build();

            if (mGroupAlertBehavior != GROUP_ALERT_ALL) {
                // if is summary and only children should alert
                if (notification.getGroup() != null
                        && (notification.flags & FLAG_GROUP_SUMMARY) != 0
                        && mGroupAlertBehavior == GROUP_ALERT_CHILDREN) {
                    removeSoundAndVibration(notification);
                }
                // if is group child and only summary should alert
                if (notification.getGroup() != null
                        && (notification.flags & FLAG_GROUP_SUMMARY) == 0
                        && mGroupAlertBehavior == GROUP_ALERT_SUMMARY) {
                    removeSoundAndVibration(notification);
                }
            }

            return notification;
        } else {
            mBuilder.setExtras(mExtras);
            Notification notification = mBuilder.build();
            if (mContentView != null) {
                notification.contentView = mContentView;
            }
            if (mBigContentView != null) {
                notification.bigContentView = mBigContentView;
            }
            if (mHeadsUpContentView != null) {
                notification.headsUpContentView = mHeadsUpContentView;
            }

            if (mGroupAlertBehavior != GROUP_ALERT_ALL) {
                // if is summary and only children should alert
                if (notification.getGroup() != null
                        && (notification.flags & FLAG_GROUP_SUMMARY) != 0
                        && mGroupAlertBehavior == GROUP_ALERT_CHILDREN) {
                    removeSoundAndVibration(notification);
                }
                // if is group child and only summary should alert
                if (notification.getGroup() != null
                        && (notification.flags & FLAG_GROUP_SUMMARY) == 0
                        && mGroupAlertBehavior == GROUP_ALERT_SUMMARY) {
                    removeSoundAndVibration(notification);
                }
            }
            return notification;
        }
    }

    private void removeSoundAndVibration(Notification notification) {
        notification.sound = null;
        notification.vibrate = null;
        notification.defaults &= ~DEFAULT_SOUND;
        notification.defaults &= ~DEFAULT_VIBRATE;
    }

    /**
     * A class for wrapping calls to {@link NotificationCompatBuilder} methods which
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

        static Notification.Builder setRemoteInputHistory(Notification.Builder builder,
                CharSequence[] text) {
            return builder.setRemoteInputHistory(text);
        }

        static Notification.Builder setCustomContentView(Notification.Builder builder,
                RemoteViews contentView) {
            return builder.setCustomContentView(contentView);
        }

        static Notification.Builder setCustomBigContentView(Notification.Builder builder,
                RemoteViews contentView) {
            return builder.setCustomBigContentView(contentView);
        }

        static Notification.Builder setCustomHeadsUpContentView(Notification.Builder builder,
                RemoteViews contentView) {
            return builder.setCustomHeadsUpContentView(contentView);
        }
    }

    /**
     * A class for wrapping calls to {@link NotificationCompatBuilder} methods which
     * were added in API 26; these calls must be wrapped to avoid performance issues.
     * See the UnsafeNewApiCall lint rule for more details.
     */
    @RequiresApi(26)
    static class Api26Impl {
        private Api26Impl() { }

        static Notification.Builder createBuilder(Context context, String channelId) {
            return new Notification.Builder(context, channelId);
        }

        static Notification.Builder setGroupAlertBehavior(Notification.Builder builder,
                int groupAlertBehavior) {
            return builder.setGroupAlertBehavior(groupAlertBehavior);
        }

        static Notification.Builder setColorized(Notification.Builder builder, boolean colorize) {
            return builder.setColorized(colorize);
        }

        static Notification.Builder setBadgeIconType(Notification.Builder builder, int icon) {
            return builder.setBadgeIconType(icon);
        }

        static Notification.Builder setSettingsText(Notification.Builder builder,
                CharSequence text) {
            return builder.setSettingsText(text);
        }

        static Notification.Builder setShortcutId(Notification.Builder builder, String shortcutId) {
            return builder.setShortcutId(shortcutId);
        }

        static Notification.Builder setTimeoutAfter(Notification.Builder builder, long durationMs) {
            return builder.setTimeoutAfter(durationMs);
        }
    }

    /**
     * A class for wrapping calls to {@link NotificationCompatBuilder} methods which
     * were added in API 28; these calls must be wrapped to avoid performance issues.
     * See the UnsafeNewApiCall lint rule for more details.
     */
    @RequiresApi(28)
    static class Api28Impl {
        private Api28Impl() {
        }

        static Notification.Action.Builder setSemanticAction(Notification.Action.Builder builder,
                int semanticAction) {
            return builder.setSemanticAction(semanticAction);
        }

        static Notification.Builder addPerson(Notification.Builder builder,
                android.app.Person person) {
            return builder.addPerson(person);
        }
    }

    /**
     * A class for wrapping calls to {@link NotificationCompatBuilder} methods which
     * were added in API 29; these calls must be wrapped to avoid performance issues.
     * See the UnsafeNewApiCall lint rule for more details.
     */
    @RequiresApi(29)
    static class Api29Impl {
        private Api29Impl() { }

        static Notification.Action.Builder setContextual(Notification.Action.Builder builder,
                boolean isContextual) {
            return builder.setContextual(isContextual);
        }

        static Notification.Builder setLocusId(Notification.Builder builder,
                Object locusId /* LocusId */) {
            return builder.setLocusId((LocusId) locusId);
        }

        static Notification.Builder setBubbleMetadata(Notification.Builder builder,
                Notification.BubbleMetadata data) {
            return builder.setBubbleMetadata(data);
        }

        static Notification.Builder setAllowSystemGeneratedContextualActions(
                Notification.Builder builder, boolean allowed) {
            return builder.setAllowSystemGeneratedContextualActions(allowed);
        }
    }

    /**
     * A class for wrapping calls to {@link NotificationCompatBuilder} methods which
     * were added in API 31; these calls must be wrapped to avoid performance issues.
     * See the UnsafeNewApiCall lint rule for more details.
     */
    @RequiresApi(31)
    static class Api31Impl {
        private Api31Impl() {
        }

        static Notification.Action.Builder setAuthenticationRequired(
                Notification.Action.Builder builder, boolean authenticationRequired) {
            return builder.setAuthenticationRequired(authenticationRequired);
        }

        static Notification.Builder setForegroundServiceBehavior(Notification.Builder builder,
                int behavior) {
            return builder.setForegroundServiceBehavior(behavior);
        }
    }

    /**
     * A class for wrapping calls to {@link NotificationCompatBuilder} methods which
     * were added in API 36; these calls must be wrapped to avoid performance issues.
     * See the UnsafeNewApiCall lint rule for more details.
     */
    @RequiresApi(36)
    static final class Api36Impl {
        private Api36Impl() {
        }

        static Notification.Builder setShortCriticalText(
                Notification.Builder builder, String shortCriticalText) {
            return builder.setShortCriticalText(shortCriticalText);
        }
    }
}
