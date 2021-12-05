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
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseArray;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.collection.ArraySet;
import androidx.core.graphics.drawable.IconCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link Notification.Builder} that works in a backwards compatible way.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
class NotificationCompatBuilder implements NotificationBuilderWithBuilderAccessor {
    private final Context mContext;
    private final Notification.Builder mBuilder;
    private final NotificationCompat.Builder mBuilderCompat;

    // @RequiresApi(16) - uncomment when lint bug is fixed.
    private RemoteViews mContentView;
    // @RequiresApi(16) - uncomment when lint bug is fixed.
    private RemoteViews mBigContentView;
    // @RequiresApi(16) - uncomment when lint bug is fixed.
    private final List<Bundle> mActionExtrasList = new ArrayList<>();
    // @RequiresApi(16) - uncomment when lint bug is fixed.
    private final Bundle mExtras = new Bundle();
    // @RequiresApi(20) - uncomment when lint bug is fixed.
    private int mGroupAlertBehavior;
    // @RequiresApi(21) - uncomment when lint bug is fixed.
    private RemoteViews mHeadsUpContentView;

    @SuppressWarnings("deprecation")
    NotificationCompatBuilder(NotificationCompat.Builder b) {
        mBuilderCompat = b;
        mContext = b.mContext;
        if (Build.VERSION.SDK_INT >= 26) {
            mBuilder = new Notification.Builder(b.mContext, b.mChannelId);
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
                .setLargeIcon(b.mLargeIcon)
                .setNumber(b.mNumber)
                .setProgress(b.mProgressMax, b.mProgress, b.mProgressIndeterminate);
        if (Build.VERSION.SDK_INT < 21) {
            mBuilder.setSound(n.sound, n.audioStreamType);
        }
        if (Build.VERSION.SDK_INT >= 16) {
            mBuilder.setSubText(b.mSubText)
                    .setUsesChronometer(b.mUseChronometer)
                    .setPriority(b.mPriority);
            for (NotificationCompat.Action action : b.mActions) {
                addAction(action);
            }

            if (b.mExtras != null) {
                mExtras.putAll(b.mExtras);
            }
            if (Build.VERSION.SDK_INT < 20) {
                if (b.mLocalOnly) {
                    mExtras.putBoolean(NotificationCompatExtras.EXTRA_LOCAL_ONLY, true);
                }
                if (b.mGroupKey != null) {
                    mExtras.putString(NotificationCompatExtras.EXTRA_GROUP_KEY, b.mGroupKey);
                    if (b.mGroupSummary) {
                        mExtras.putBoolean(NotificationCompatExtras.EXTRA_GROUP_SUMMARY, true);
                    } else {
                        mExtras.putBoolean(
                                NotificationManagerCompat.EXTRA_USE_SIDE_CHANNEL, true);
                    }
                }
                if (b.mSortKey != null) {
                    mExtras.putString(NotificationCompatExtras.EXTRA_SORT_KEY, b.mSortKey);
                }
            }

            mContentView = b.mContentView;
            mBigContentView = b.mBigContentView;
        }
        if (Build.VERSION.SDK_INT >= 17) {
            mBuilder.setShowWhen(b.mShowWhen);
        }
        if (Build.VERSION.SDK_INT >= 19) {
            if (Build.VERSION.SDK_INT < 21) {
                final List<String> people = combineLists(getPeople(b.mPersonList), b.mPeople);
                if (people != null && !people.isEmpty()) {
                    mExtras.putStringArray(Notification.EXTRA_PEOPLE,
                            people.toArray(new String[people.size()]));
                }
            }
        }
        if (Build.VERSION.SDK_INT >= 20) {
            mBuilder.setLocalOnly(b.mLocalOnly)
                    .setGroup(b.mGroupKey)
                    .setGroupSummary(b.mGroupSummary)
                    .setSortKey(b.mSortKey);

            mGroupAlertBehavior = b.mGroupAlertBehavior;
        }
        if (Build.VERSION.SDK_INT >= 21) {
            mBuilder.setCategory(b.mCategory)
                    .setColor(b.mColor)
                    .setVisibility(b.mVisibility)
                    .setPublicVersion(b.mPublicVersion)
                    .setSound(n.sound, n.audioAttributes);


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
        }
        if (Build.VERSION.SDK_INT >= 23) {
            if (b.mSmallIcon != null) {
                mBuilder.setSmallIcon(b.mSmallIcon);
            }
        }
        if (Build.VERSION.SDK_INT >= 24) {
            mBuilder.setExtras(b.mExtras)
                    .setRemoteInputHistory(b.mRemoteInputHistory);
            if (b.mContentView != null) {
                mBuilder.setCustomContentView(b.mContentView);
            }
            if (b.mBigContentView != null) {
                mBuilder.setCustomBigContentView(b.mBigContentView);
            }
            if (b.mHeadsUpContentView != null) {
                mBuilder.setCustomHeadsUpContentView(b.mHeadsUpContentView);
            }
        }
        if (Build.VERSION.SDK_INT >= 26) {
            mBuilder.setBadgeIconType(b.mBadgeIcon)
                    .setSettingsText(b.mSettingsText)
                    .setShortcutId(b.mShortcutId)
                    .setTimeoutAfter(b.mTimeout)
                    .setGroupAlertBehavior(b.mGroupAlertBehavior);
            if (b.mColorizedSet) {
                mBuilder.setColorized(b.mColorized);
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
                mBuilder.addPerson(p.toAndroidPerson());
            }
        }
        if (Build.VERSION.SDK_INT >= 29) {
            mBuilder.setAllowSystemGeneratedContextualActions(
                    b.mAllowSystemGeneratedContextualActions);
            // TODO: Consider roundtripping NotificationCompat.BubbleMetadata on pre-Q platforms.
            mBuilder.setBubbleMetadata(
                    NotificationCompat.BubbleMetadata.toPlatform(b.mBubbleMetadata));
            if (b.mLocusId != null) {
                mBuilder.setLocusId(b.mLocusId.toLocusId());
            }
        }
        if (Build.VERSION.SDK_INT >= 31) {
            if (b.mFgsDeferBehavior != NotificationCompat.FOREGROUND_SERVICE_DEFAULT) {
                mBuilder.setForegroundServiceBehavior(b.mFgsDeferBehavior);
            }
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
                mBuilder.setGroupAlertBehavior(mGroupAlertBehavior);
            }
        }
    }

    @Nullable
    private static List<String> combineLists(@Nullable final List<String> first,
            @Nullable final List<String> second) {
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

    @Nullable
    private static List<String> getPeople(@Nullable final List<Person> people) {
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
        if (Build.VERSION.SDK_INT >= 16 && style != null) {
            RemoteViews styleBigContentView = style.makeBigContentView(this);
            if (styleBigContentView != null) {
                n.bigContentView = styleBigContentView;
            }
        }
        if (Build.VERSION.SDK_INT >= 21 && style != null) {
            RemoteViews styleHeadsUpContentView =
                    mBuilderCompat.mStyle.makeHeadsUpContentView(this);
            if (styleHeadsUpContentView != null) {
                n.headsUpContentView = styleHeadsUpContentView;
            }
        }

        if (Build.VERSION.SDK_INT >= 16 && style != null) {
            Bundle extras = NotificationCompat.getExtras(n);
            if (extras != null) {
                style.addCompatExtras(extras);
            }
        }

        return n;
    }

    private void addAction(NotificationCompat.Action action) {
        if (Build.VERSION.SDK_INT >= 20) {
            Notification.Action.Builder actionBuilder;
            IconCompat iconCompat = action.getIconCompat();
            if (Build.VERSION.SDK_INT >= 23) {
                actionBuilder = new Notification.Action.Builder(
                        iconCompat != null ? iconCompat.toIcon() : null,
                        action.getTitle(),
                        action.getActionIntent());
            } else {
                actionBuilder = new Notification.Action.Builder(
                        iconCompat != null ? iconCompat.getResId() : 0,
                        action.getTitle(),
                        action.getActionIntent());
            }
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
                actionBuilder.setAllowGeneratedReplies(action.getAllowGeneratedReplies());
            }

            actionExtras.putInt(NotificationCompat.Action.EXTRA_SEMANTIC_ACTION,
                    action.getSemanticAction());
            if (Build.VERSION.SDK_INT >= 28) {
                actionBuilder.setSemanticAction(action.getSemanticAction());
            }

            if (Build.VERSION.SDK_INT >= 29) {
                actionBuilder.setContextual(action.isContextual());
            }

            if (Build.VERSION.SDK_INT >= 31) {
                actionBuilder.setAuthenticationRequired(action.isAuthenticationRequired());
            }

            actionExtras.putBoolean(NotificationCompat.Action.EXTRA_SHOWS_USER_INTERFACE,
                    action.getShowsUserInterface());
            actionBuilder.addExtras(actionExtras);
            mBuilder.addAction(actionBuilder.build());
        } else if (Build.VERSION.SDK_INT >= 16) {
            mActionExtrasList.add(
                    NotificationCompatJellybean.writeActionAndGetExtras(mBuilder, action));
        }
    }

    @SuppressWarnings("deprecation")
    protected Notification buildInternal() {
        if (Build.VERSION.SDK_INT >= 26) {
            return mBuilder.build();
        } else if (Build.VERSION.SDK_INT >= 24) {
            Notification notification =  mBuilder.build();

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
        } else if (Build.VERSION.SDK_INT >= 21) {
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
        } else if (Build.VERSION.SDK_INT >= 20) {
            mBuilder.setExtras(mExtras);
            Notification notification = mBuilder.build();
            if (mContentView != null) {
                notification.contentView = mContentView;
            }
            if (mBigContentView != null) {
                notification.bigContentView = mBigContentView;
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
        } else if (Build.VERSION.SDK_INT >= 19) {
            SparseArray<Bundle> actionExtrasMap =
                    NotificationCompatJellybean.buildActionExtrasMap(mActionExtrasList);
            if (actionExtrasMap != null) {
                // Add the action extras sparse array if any action was added with extras.
                mExtras.putSparseParcelableArray(
                        NotificationCompatExtras.EXTRA_ACTION_EXTRAS, actionExtrasMap);
            }
            mBuilder.setExtras(mExtras);
            Notification notification = mBuilder.build();
            if (mContentView != null) {
                notification.contentView = mContentView;
            }
            if (mBigContentView != null) {
                notification.bigContentView = mBigContentView;
            }
            return notification;
        } else if (Build.VERSION.SDK_INT >= 16) {
            Notification notification = mBuilder.build();
            // Merge in developer provided extras, but let the values already set
            // for keys take precedence.
            Bundle extras = NotificationCompat.getExtras(notification);
            Bundle mergeBundle = new Bundle(mExtras);
            for (String key : mExtras.keySet()) {
                if (extras.containsKey(key)) {
                    mergeBundle.remove(key);
                }
            }
            extras.putAll(mergeBundle);
            SparseArray<Bundle> actionExtrasMap =
                    NotificationCompatJellybean.buildActionExtrasMap(mActionExtrasList);
            if (actionExtrasMap != null) {
                // Add the action extras sparse array if any action was added with extras.
                NotificationCompat.getExtras(notification).putSparseParcelableArray(
                        NotificationCompatExtras.EXTRA_ACTION_EXTRAS, actionExtrasMap);
            }
            if (mContentView != null) {
                notification.contentView = mContentView;
            }
            if (mBigContentView != null) {
                notification.bigContentView = mBigContentView;
            }
            return notification;
        } else {
            return mBuilder.getNotification();
        }
    }

    private void removeSoundAndVibration(Notification notification) {
        notification.sound = null;
        notification.vibrate = null;
        notification.defaults &= ~DEFAULT_SOUND;
        notification.defaults &= ~DEFAULT_VIBRATE;
    }
}
