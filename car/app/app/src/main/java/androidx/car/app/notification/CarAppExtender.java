/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app.notification;

import static java.util.Objects.requireNonNull;

import android.app.Notification;
import android.app.Notification.Action;
import android.app.PendingIntent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.car.app.model.CarColor;
import androidx.car.app.serialization.Bundler;
import androidx.car.app.serialization.BundlerException;
import androidx.car.app.utils.CollectionUtils;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to add car app extensions to notifications.
 *
 * <p>By default, notifications in a car screen have the properties provided by
 * {@link NotificationCompat.Builder}. This helper class provides methods to
 * override those properties for the car screen. However, notifications only show up in the car
 * screen if it is extended with {@link CarAppExtender}, even if the extender does not override any
 * properties. To create a notification with car extensions:
 *
 * <ol>
 *   <li>Create a {@link NotificationCompat.Builder}, setting any desired properties.
 *
 *   <li>Create a {@link CarAppExtender.Builder}.
 *
 *   <li>Set car-specific properties using the {@code set} methods of {@link
 *       CarAppExtender.Builder}.
 *
 *   <li>Create a {@link CarAppExtender} by calling {@link Builder#build()}.
 *
 *   <li>Call {@link NotificationCompat.Builder#extend} to apply the extensions to a notification.
 *
 *   <li>Post the notification to the notification system with the {@code
 *       CarNotificationManager.notify(...)} methods. Do not use the {@code
 *       NotificationManager.notify(...)}, nor the NotificationManagerCompat.notify(...)}  methods.
 * </ol>
 *
 * <pre class="prettyprint">
 * Notification notification = new NotificationCompat.Builder(context)
 *         ...
 *         .extend(new CarAppExtender.Builder()
 *                 .set*(...)
 *                 .build())
 *         .build();
 * </pre>
 *
 * <p>Car extensions can be accessed on an existing notification by using the {@code
 * CarAppExtender(Notification)} constructor, and then using the {@code get} methods to access
 * values.
 *
 * <p>The car screen UI is affected by the notification channel importance (Android O and above) or
 * notification priority (below Android O) in the following ways:
 *
 * <ul>
 *   <li>A heads-up-notification (HUN) will show if the importance is set to
 *   {@link NotificationManagerCompat#IMPORTANCE_HIGH}, or the priority is set
 *       to {@link NotificationCompat#PRIORITY_HIGH} or above.
 *
 *   <li>The notification center icon, which opens a screen with all posted notifications when
 *       tapped, will show a badge for a new notification if the importance is set to
 *       {@link NotificationManagerCompat#IMPORTANCE_DEFAULT} or above, or the
 *       priority is set to {@link NotificationCompat#PRIORITY_DEFAULT} or above.
 *   <li>The notification entry will show in the notification center for all priority levels.
 * </ul>
 *
 * Calling {@link Builder#setImportance(int)} will override the importance for the notification in
 * the car screen.
 *
 * <p>Calling {@code NotificationCompat.Builder#setOnlyAlertOnce(true)} will alert a high-priority
 * notification only once in the HUN. Updating the same notification will not trigger another HUN
 * event.
 *
 * <h4>Navigation</h4>
 *
 * <p>For a navigation app's turn-by-turn (TBT) notifications, which update the same notification
 * frequently with navigation information, the notification UI has a slightly different behavior.
 * The app can post a TBT notification by calling {@code
 * NotificationCompat.Builder#setOngoing(true)} and {@code
 * NotificationCompat.Builder#setCategory(NotificationCompat.CATEGORY_NAVIGATION)}.
 * <p>TBT notifications behave the same as regular notifications with the following
 * exceptions:
 *
 * <ul>
 *     <li>The notification will not be displayed if the navigation app is not the currently active
 *     navigation app, or if the app is already displaying routing information in the navigation
 *     template.
 *
 *     <li>The heads-up-notification (HUN) can be customized with a background color through
 *     {@link Builder#setColor}.
 *
 *     <li>The notification will not be displayed in the notification center.
 * </ul>
 *
 * <p>In addition to that, the information in the navigation notification will be displayed in the
 * rail widget at the bottom of the screen when the app is in the background.
 *
 * <p>Note that frequent HUNs distract the driver. The recommended practice is to update the TBT
 * notification regularly on distance changes, which updates the rail widget, but call {@code
 * NotificationCompat.Builder#setOnlyAlertOnce(true)} unless there is a significant navigation turn
 * event.
 */
public final class CarAppExtender implements NotificationCompat.Extender {
    private static final String TAG = "CarAppExtender";

    private static final String EXTRA_CAR_EXTENDER = "androidx.car.app.EXTENSIONS";
    private static final String EXTRA_CONTENT_TITLE = "content_title";
    private static final String EXTRA_CONTENT_TEXT = "content_text";
    private static final String EXTRA_SMALL_RES_ID = "small_res_id";
    private static final String EXTRA_LARGE_BITMAP = "large_bitmap";
    private static final String EXTRA_CONTENT_INTENT = "content_intent";
    private static final String EXTRA_DELETE_INTENT = "delete_intent";
    private static final String EXTRA_ACTIONS = "actions";
    private static final String EXTRA_IMPORTANCE = "importance";
    private static final String EXTRA_COLOR = "color";
    private static final String EXTRA_CHANNEL_ID = "channel_id";

    private @Nullable CharSequence mContentTitle;
    private @Nullable CharSequence mContentText;
    private int mSmallIconResId;
    private @Nullable Bitmap mLargeIconBitmap;
    private @Nullable PendingIntent mContentIntent;
    private @Nullable PendingIntent mDeleteIntent;
    private @Nullable ArrayList<Action> mActions;
    private int mImportance;
    private @Nullable CarColor mColor;
    private @Nullable String mChannelId;

    /**
     * Creates a {@link CarAppExtender} from the {@link CarAppExtender} of an existing notification.
     */
    @SuppressWarnings("deprecation")
    public CarAppExtender(@NonNull Notification notification) {
        Bundle extras = NotificationCompat.getExtras(notification);
        if (extras == null) {
            return;
        }

        Bundle carBundle = extras.getBundle(EXTRA_CAR_EXTENDER);
        if (carBundle == null) {
            return;
        }

        mContentTitle = carBundle.getCharSequence(EXTRA_CONTENT_TITLE);
        mContentText = carBundle.getCharSequence(EXTRA_CONTENT_TEXT);
        mSmallIconResId = carBundle.getInt(EXTRA_SMALL_RES_ID);
        mLargeIconBitmap = carBundle.getParcelable(EXTRA_LARGE_BITMAP);
        mContentIntent = carBundle.getParcelable(EXTRA_CONTENT_INTENT);
        mDeleteIntent = carBundle.getParcelable(EXTRA_DELETE_INTENT);
        ArrayList<Action> actions = carBundle.getParcelableArrayList(EXTRA_ACTIONS);
        mActions = actions == null ? new ArrayList<>() : actions;
        mImportance =
                carBundle.getInt(EXTRA_IMPORTANCE,
                        NotificationManagerCompat.IMPORTANCE_UNSPECIFIED);

        Bundle colorBundle = carBundle.getBundle(EXTRA_COLOR);
        if (colorBundle != null) {
            try {
                mColor = (CarColor) Bundler.fromBundle(colorBundle);
            } catch (BundlerException e) {
                Log.e(TAG, "Failed to deserialize the notification color", e);
            }
        }

        mChannelId = carBundle.getString(EXTRA_CHANNEL_ID);
    }

    CarAppExtender(Builder builder) {
        mContentTitle = builder.mContentTitle;
        mContentText = builder.mContentText;
        mSmallIconResId = builder.mSmallIconResId;
        mLargeIconBitmap = builder.mLargeIconBitmap;
        mContentIntent = builder.mContentIntent;
        mDeleteIntent = builder.mDeleteIntent;
        mActions = builder.mActions;
        mImportance = builder.mImportance;
        mColor = builder.mColor;
        mChannelId = builder.mChannelId;
    }

    /**
     * Applies car extensions to a notification that is being built.
     *
     * <p>This is automatically called when the style is applied to the builder via {@link
     * NotificationCompat.Builder#extend(NotificationCompat.Extender)} so this method does not need
     * to be manually called.
     *
     * @throws NullPointerException if {@code builder} is {@code null}
     */
    @Override
    public NotificationCompat.@NonNull Builder extend(NotificationCompat.@NonNull Builder builder) {
        requireNonNull(builder);
        Bundle carExtensions = createExtrasBundle();
        builder.getExtras().putBundle(EXTRA_CAR_EXTENDER, carExtensions);
        return builder;
    }

    /**
     * Applies car extensions to a notification that is being built.
     *
     * <p>For the most part, developers should be building notifications via {@link
     * NotificationCompat.Builder} and not {@link Notification.Builder}; however, there may be
     * reasons to not use the compat version, so this non-compat method is provided for convenience
     * in those situations.
     *
     * @throws NullPointerException if {@code builder} is {@code null}
     */
    public Notification.@NonNull Builder extend(Notification.@NonNull Builder builder) {
        requireNonNull(builder);
        Bundle carExtensions = createExtrasBundle();
        builder.getExtras().putBundle(EXTRA_CAR_EXTENDER, carExtensions);
        return builder;
    }

    @NonNull
    private Bundle createExtrasBundle() {
        Bundle carExtensions = new Bundle();

        if (mContentTitle != null) {
            carExtensions.putCharSequence(EXTRA_CONTENT_TITLE, mContentTitle);
        }

        if (mContentText != null) {
            carExtensions.putCharSequence(EXTRA_CONTENT_TEXT, mContentText);
        }

        if (mSmallIconResId != Resources.ID_NULL) {
            carExtensions.putInt(EXTRA_SMALL_RES_ID, mSmallIconResId);
        }

        if (mLargeIconBitmap != null) {
            carExtensions.putParcelable(EXTRA_LARGE_BITMAP, mLargeIconBitmap);
        }

        if (mContentIntent != null) {
            carExtensions.putParcelable(EXTRA_CONTENT_INTENT, mContentIntent);
        }

        if (mDeleteIntent != null) {
            carExtensions.putParcelable(EXTRA_DELETE_INTENT, mDeleteIntent);
        }

        if (mActions != null && !mActions.isEmpty()) {
            carExtensions.putParcelableArrayList(EXTRA_ACTIONS, mActions);
        }

        carExtensions.putInt(EXTRA_IMPORTANCE, mImportance);

        if (mColor != null) {
            try {
                Bundle bundle = Bundler.toBundle(mColor);
                carExtensions.putBundle(EXTRA_COLOR, bundle);
            } catch (BundlerException e) {
                Log.e(TAG, "Failed to serialize the notification color", e);
            }
        }

        if (mChannelId != null) {
            carExtensions.putString(EXTRA_CHANNEL_ID, mChannelId);
        }

        return carExtensions;
    }

    /**
     * Returns whether the given notification was extended with {@link CarAppExtender}.
     *
     * @throws NullPointerException if {@code notification} is {@code null}
     */
    public static boolean isExtended(@NonNull Notification notification) {
        Bundle extras = NotificationCompat.getExtras(requireNonNull(notification));
        if (extras == null) {
            return false;
        }

        return extras.getBundle(EXTRA_CAR_EXTENDER) != null;
    }

    /**
     * Returns the content title for the notification or {@code null} if not set.
     *
     * @see Builder#setContentTitle
     */
    public @Nullable CharSequence getContentTitle() {
        return mContentTitle;
    }

    /**
     * Returns the content text of the notification or {@code null} if not set.
     *
     * @see Builder#setContentText
     */
    public @Nullable CharSequence getContentText() {
        return mContentText;
    }

    /**
     * Returns the resource ID of the small icon drawable to use.
     *
     * @see Builder#setSmallIcon(int)
     */
    @DrawableRes
    public int getSmallIcon() {
        return mSmallIconResId;
    }

    /**
     * Returns the large icon bitmap to display in the notification or {@code null} if not set.
     *
     * @see Builder#setLargeIcon(Bitmap)
     */
    public @Nullable Bitmap getLargeIcon() {
        return mLargeIconBitmap;
    }

    /**
     * Returns  the {@link PendingIntent} to send when the notification is clicked in the car or
     * {@code null} if not set.
     *
     * @see Builder#setContentIntent(PendingIntent)
     */
    public @Nullable PendingIntent getContentIntent() {
        return mContentIntent;
    }

    /**
     * Returns the {@link PendingIntent} to send when the notification is cleared by the user or
     * {@code null} if not set.
     *
     * @see Builder#setDeleteIntent(PendingIntent)
     */
    public @Nullable PendingIntent getDeleteIntent() {
        return mDeleteIntent;
    }

    /**
     * Returns the list of {@link Action} present on this car notification.
     *
     * @see Builder#addAction(int, CharSequence, PendingIntent)
     */
    public @NonNull List<Action> getActions() {
        return CollectionUtils.emptyIfNull(mActions);
    }

    /**
     * Returns the importance of the notification in the car screen.
     *
     * @see Builder#setImportance(int)
     */
    public int getImportance() {
        return mImportance;
    }

    /**
     * Returns the background color of the notification or {@code null} if a default color is to
     * be used.
     *
     * @see Builder#setColor(CarColor)
     */
    public @Nullable CarColor getColor() {
        return mColor;
    }

    /**
     * Returns the channel id of the notification channel to use in the car.
     *
     * @see Builder#setChannelId(String)
     */
    public @Nullable String getChannelId() {
        return mChannelId;
    }

    /** A builder of {@link CarAppExtender}. */
    public static final class Builder {
        @Nullable CharSequence mContentTitle;
        @Nullable CharSequence mContentText;
        int mSmallIconResId;
        @Nullable Bitmap mLargeIconBitmap;
        @Nullable PendingIntent mContentIntent;
        @Nullable PendingIntent mDeleteIntent;
        final ArrayList<Action> mActions = new ArrayList<>();
        int mImportance = NotificationManagerCompat.IMPORTANCE_UNSPECIFIED;
        @Nullable CarColor mColor;
        @Nullable String mChannelId;

        /**
         * Sets the title of the notification in the car screen.
         *
         * <p>This will be the most prominently displayed text in the car notification.
         *
         * <p>This method is equivalent to
         * {@link NotificationCompat.Builder#setContentTitle(CharSequence)} for the car
         * screen.
         *
         * <p>Spans are not supported in the input string and will be ignored.
         *
         * @throws NullPointerException if {@code contentTitle} is {@code null}
         */
        public @NonNull Builder setContentTitle(@NonNull CharSequence contentTitle) {
            mContentTitle = requireNonNull(contentTitle);
            return this;
        }

        /**
         * Sets the content text of the notification in the car screen.
         *
         * <p>This method is equivalent to
         * {@link NotificationCompat.Builder#setContentText(CharSequence)} for the car screen.
         *
         * <p>Spans are not supported in the input string and will be ignored.
         *
         * @param contentText override for the notification's content text. If set to an empty
         *                    string, it will be treated as if there is no context text
         * @throws NullPointerException if {@code contentText} is {@code null}
         */
        public @NonNull Builder setContentText(@NonNull CharSequence contentText) {
            mContentText = requireNonNull(contentText);
            return this;
        }

        /**
         * Sets the small icon of the notification in the car screen.
         *
         * <p>This is used as the primary icon to represent the notification.
         *
         * <p>This method is equivalent to {@link NotificationCompat.Builder#setSmallIcon(int)} for
         * the car screen.
         */
        public @NonNull Builder setSmallIcon(int iconResId) {
            mSmallIconResId = iconResId;
            return this;
        }

        /**
         * Sets the large icon of the notification in the car screen.
         *
         * <p>This is used as the secondary icon to represent the notification in the notification
         * center.
         *
         * <p>This method is equivalent to {@link NotificationCompat.Builder#setLargeIcon(Bitmap)}
         * for the car screen.
         *
         * <p>The large icon will be shown in the notification badge. If the large icon is not
         * set in the {@link CarAppExtender} or the notification, the small icon will show instead.
         *
         * @throws NullPointerException if {@code bitmap} is {@code null}
         */
        public @NonNull Builder setLargeIcon(@NonNull Bitmap bitmap) {
            mLargeIconBitmap = requireNonNull(bitmap);
            return this;
        }

        /**
         * Supplies a {@link PendingIntent} to send when the notification is clicked in the car.
         *
         * <p>If not set, the notification's content intent will be used.
         *
         * <p>In the case of navigation notifications in the rail widget, this intent will be
         * sent when the user taps on the rail widget.
         *
         * <p>This method is equivalent to
         * {@link NotificationCompat.Builder#setContentIntent(PendingIntent)} for the car screen.
         *
         * @param contentIntent override for the notification's content intent.
         * @throws NullPointerException if {@code contentIntent} is {@code null}
         */
        public @NonNull Builder setContentIntent(@NonNull PendingIntent contentIntent) {
            mContentIntent = requireNonNull(contentIntent);
            return this;
        }

        /**
         * Supplies a {@link PendingIntent} to send when the user clears the notification by either
         * using the "clear all" functionality in the notification center, or tapping the individual
         * "close" buttons on the notification in the car screen.
         *
         * <p>If not set, the notification's content intent will be used.
         *
         * <p>This method is equivalent to
         * {@link NotificationCompat.Builder#setDeleteIntent(PendingIntent)} for the car screen.
         *
         * @param deleteIntent override for the notification's delete intent
         * @throws NullPointerException if {@code deleteIntent} is {@code null}
         */
        public @NonNull Builder setDeleteIntent(@NonNull PendingIntent deleteIntent) {
            mDeleteIntent = requireNonNull(deleteIntent);
            return this;
        }

        /**
         * Adds an action to this notification.
         *
         * <p>Actions are typically displayed by the system as a button adjacent to the notification
         * content.
         *
         * <p>A notification may offer up to 2 actions. The system may not display some actions
         * in the compact notification UI (e.g. heads-up-notifications).
         *
         * <p>If one or more action is added with this method, any action added by
         * {@link NotificationCompat.Builder#addAction(int, CharSequence, PendingIntent)} will be
         * ignored.
         *
         * <p>This method is equivalent to
         * {@link NotificationCompat.Builder#addAction(int, CharSequence, PendingIntent)} for the
         * car screen.
         *
         * @param icon   resource ID of a drawable that represents the action. In order to
         *               display the actions properly, a valid resource id for the icon must be
         *               provided
         * @param title  text describing the action
         * @param intent {@link PendingIntent} to send when the action is invoked. In the case of
         *               navigation notifications in the rail widget, this intent will be sent
         *               when the user taps on the action icon in the rail
         *               widget
         * @throws NullPointerException if {@code title} or {@code intent} are {@code null}
         */
        @SuppressWarnings("deprecation")
        public @NonNull Builder addAction(
                @DrawableRes int icon, @NonNull CharSequence title, @NonNull PendingIntent intent) {
            mActions.add(new Action(icon, requireNonNull(title), requireNonNull(intent)));
            return this;
        }

        /**
         * For Android Auto only, sets the importance of the notification in the car screen.
         *
         * <p>The default value is {@link NotificationManagerCompat#IMPORTANCE_UNSPECIFIED},
         * and will not be used to override.
         *
         * <p>The importance is used to determine whether the notification will show as a HUN on
         * the car screen. See the class description for more details.
         *
         * <p>See {@link NotificationManagerCompat} for all supported importance values.
         *
         * @see #setChannelId(String)
         */
        public @NonNull Builder setImportance(int importance) {
            mImportance = importance;
            return this;
        }

        /**
         * Sets the background color of the notification in the car screen.
         *
         * <p>This method is equivalent to {@link NotificationCompat.Builder#setColor(int)} for
         * the car screen.
         *
         * <p>This color is only used for navigation notifications. See the "Navigation" section
         * of {@link CarAppExtender} for more details.
         *
         * @throws NullPointerException if {@code color} is {@code null}
         */
        public @NonNull Builder setColor(@NonNull CarColor color) {
            mColor = requireNonNull(color);
            return this;
        }

        /**
         * For Android Automotive OS only, sets the channel id of the notification channel to be
         * used in the car.
         *
         * <p>This is used in the case where your notification is to have a different importance
         * in the car then it does on the phone.
         *
         * <p>It is used for the same purposes you'd use {@link #setImportance(int)} for
         * Auto.
         *
         * @see #setImportance(int)
         */
        public @NonNull Builder setChannelId(@NonNull String channelId) {
            mChannelId = channelId;
            return this;
        }

        /**
         * Constructs the {@link CarAppExtender} defined by this builder.
         */
        public @NonNull CarAppExtender build() {
            return new CarAppExtender(this);
        }

        /** Creates an empty {@link Builder} instance. */
        public Builder() {
        }
    }
}
