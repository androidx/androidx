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

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

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
 *   <li>Create a {@link CarAppExtender.Builder} with {@link CarAppExtender#builder()}.
 *   <li>Set car-specific properties using the {@code set} methods of {@link
 *       CarAppExtender.Builder}.
 *   <li>Create a {@link CarAppExtender} by calling {@link Builder#build()}.
 *   <li>Call {@link NotificationCompat.Builder#extend} to apply the extensions to a notification.
 *   <li>Post the notification to the notification system with the {@code
 *       NotificationManagerCompat.notify(...)} methods and not the {@code
 *       NotificationManager.notify(...)} methods.
 * </ol>
 *
 * <pre class="prettyprint">
 * Notification notification = new NotificationCompat.Builder(context)
 *         ...
 *         .extend(CarAppExtender.builder()
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
 * NotificationCompat.Builder#setCategory(NotificationCompat.CATEGORY_NAVIGATION)}. The car screen
 * UI is affected in the following ways:
 *
 * <ul>
 *   <li>The same heads-up-notification (HUN) behavior as regular notifications.
 *   <li>A rail widget at the bottom of the screen will show when the navigation app is in the
 *       background.
 * </ul>
 *
 * Note that frequent HUNs distract the driver. The recommended practice is to update the TBT
 * notification regularly on distance changes, which updates the rail widget, but call {@code
 * NotificationCompat.Builder#setOnlyAlertOnce(true)} unless there is a significant navigation turn
 * event.
 */
public class CarAppExtender implements NotificationCompat.Extender {
    private static final String EXTRA_CAR_EXTENDER = "android.car.EXTENSIONS";
    private static final String EXTRA_IS_EXTENDED = "android.car.app.EXTENDED";
    private static final String EXTRA_CONTENT_TITLE = "content_title";
    private static final String EXTRA_CONTENT_TEXT = "content_text";
    private static final String EXTRA_SMALL_RES_ID = "small_res_id";
    private static final String EXTRA_LARGE_BITMAP = "large_bitmap";
    private static final String EXTRA_CONTENT_INTENT = "content_intent";
    private static final String EXTRA_DELETE_INTENT = "delete_intent";
    private static final String EXTRA_ACTIONS = "actions";
    private static final String EXTRA_IMPORTANCE = "importance";

    private boolean mIsExtended;
    @Nullable
    private CharSequence mContentTitle;
    @Nullable
    private CharSequence mContentText;
    private int mSmallIconResId;
    @Nullable
    private Bitmap mLargeIconBitmap;
    @Nullable
    private PendingIntent mContentIntent;
    @Nullable
    private PendingIntent mDeleteIntent;
    private ArrayList<Action> mActions;
    private int mImportance;

    /** Creates a {@link CarAppExtender.Builder}. */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a {@link CarAppExtender} from the {@link CarAppExtender} of an existing notification.
     */
    public CarAppExtender(@NonNull Notification notification) {
        Bundle extras = NotificationCompat.getExtras(notification);
        if (extras == null) {
            return;
        }

        Bundle carBundle = extras.getBundle(EXTRA_CAR_EXTENDER);
        if (carBundle == null) {
            return;
        }

        mIsExtended = carBundle.getBoolean(EXTRA_IS_EXTENDED);
        mContentTitle = carBundle.getCharSequence(EXTRA_CONTENT_TITLE);
        mContentText = carBundle.getCharSequence(EXTRA_CONTENT_TEXT);
        mSmallIconResId = carBundle.getInt(EXTRA_SMALL_RES_ID);
        mLargeIconBitmap = carBundle.getParcelable(EXTRA_LARGE_BITMAP);
        mContentIntent = carBundle.getParcelable(EXTRA_CONTENT_INTENT);
        mDeleteIntent = carBundle.getParcelable(EXTRA_DELETE_INTENT);
        ArrayList<Action> actions = carBundle.getParcelableArrayList(EXTRA_ACTIONS);
        this.mActions = actions == null ? new ArrayList<>() : actions;
        mImportance =
                carBundle.getInt(EXTRA_IMPORTANCE,
                        NotificationManagerCompat.IMPORTANCE_UNSPECIFIED);
    }

    private CarAppExtender(Builder builder) {
        this.mContentTitle = builder.mContentTitle;
        this.mContentText = builder.mContentText;
        this.mSmallIconResId = builder.mSmallIconResId;
        this.mLargeIconBitmap = builder.mLargeIconBitmap;
        this.mContentIntent = builder.mContentIntent;
        this.mDeleteIntent = builder.mDeleteIntent;
        this.mActions = builder.mActions;
        this.mImportance = builder.mImportance;
    }

    /**
     * Applies car extensions to a notification that is being built. This is typically called by
     * {@link NotificationCompat.Builder#extend(NotificationCompat.Extender)}.
     *
     * @throws NullPointerException if {@code builder} is {@code null}.
     */
    @NonNull
    @Override
    public NotificationCompat.Builder extend(@NonNull NotificationCompat.Builder builder) {
        requireNonNull(builder);
        Bundle carExtensions = new Bundle();

        carExtensions.putBoolean(EXTRA_IS_EXTENDED, true);

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

        if (!mActions.isEmpty()) {
            carExtensions.putParcelableArrayList(EXTRA_ACTIONS, mActions);
        }

        carExtensions.putInt(EXTRA_IMPORTANCE, mImportance);

        builder.getExtras().putBundle(EXTRA_CAR_EXTENDER, carExtensions);
        return builder;
    }

    /**
     * Returns {@code true} if the notification was extended with {@link CarAppExtender}, {@code
     * false} otherwise.
     */
    public boolean isExtended() {
        return mIsExtended;
    }

    /**
     * Returns {@code true} if the notification was extended with {@link CarAppExtender}, {@code
     * false} otherwise.
     *
     * @throws NullPointerException if {@code notification} is {@code null}.
     */
    public static boolean isExtended(@NonNull Notification notification) {
        Bundle extras = NotificationCompat.getExtras(requireNonNull(notification));
        if (extras == null) {
            return false;
        }

        extras = extras.getBundle(EXTRA_CAR_EXTENDER);
        return extras != null && extras.getBoolean(EXTRA_IS_EXTENDED);
    }

    /**
     * Gets the content title for the notification.
     *
     * @see Builder#setContentTitle
     */
    @Nullable
    public CharSequence getContentTitle() {
        return mContentTitle;
    }

    /**
     * Returns the content text of the notification.
     *
     * @see Builder#setContentText
     */
    @Nullable
    public CharSequence getContentText() {
        return mContentText;
    }

    /**
     * Gets the resource ID of the small icon drawable to use.
     *
     * @see Builder#setSmallIcon(int)
     */
    public int getSmallIconResId() {
        return mSmallIconResId;
    }

    /**
     * Gets the large icon bitmap to display in the notification.
     *
     * @see Builder#setLargeIcon(Bitmap)
     */
    @Nullable
    public Bitmap getLargeIconBitmap() {
        return mLargeIconBitmap;
    }

    /**
     * Gets the {@link PendingIntent} to send when the notification is clicked in the car, or {@code
     * null} if one is not set.
     *
     * @see Builder#setContentIntent(PendingIntent)
     */
    @Nullable
    public PendingIntent getContentIntent() {
        return mContentIntent;
    }

    /**
     * Gets the {@link PendingIntent} to send when the notification is cleared by the user, or
     * {@code null} if one is not set.
     *
     * @see Builder#setDeleteIntent(PendingIntent)
     */
    @Nullable
    public PendingIntent getDeleteIntent() {
        return mDeleteIntent;
    }

    /**
     * Gets the list of {@link Action} present on this car notification.
     *
     * @see Builder#addAction(int, CharSequence, PendingIntent)
     */
    @NonNull
    public List<Action> getActions() {
        return mActions;
    }

    /**
     * Gets the importance of the notification in the car screen.
     *
     * @see Builder#setImportance(int)
     */
    public int getImportance() {
        return mImportance;
    }

    /** A builder of {@link CarAppExtender}. */
    public static final class Builder {
        @Nullable
        private CharSequence mContentTitle;
        @Nullable
        private CharSequence mContentText;
        private int mSmallIconResId;
        @Nullable
        private Bitmap mLargeIconBitmap;
        @Nullable
        private PendingIntent mContentIntent;
        @Nullable
        private PendingIntent mDeleteIntent;
        private final ArrayList<Action> mActions = new ArrayList<>();
        private int mImportance = NotificationManagerCompat.IMPORTANCE_UNSPECIFIED;

        /**
         * Sets the title of the notification in the car screen, or {@code null} to not override the
         * notification title.
         *
         * <p>This will be the most prominently displayed text in the car notification.
         *
         * <p>This method is equivalent to
         * {@link NotificationCompat.Builder#setContentTitle(CharSequence)} for the car
         * screen.
         */
        @NonNull
        public Builder setContentTitle(@Nullable CharSequence contentTitle) {
            this.mContentTitle = contentTitle;
            return this;
        }

        /**
         * Sets the content text of the notification in the car screen, or {@code null} to not
         * override the content text.
         *
         * <p>This method is equivalent to
         * {@link NotificationCompat.Builder#setContentText(CharSequence)} for the car screen.
         *
         * @param contentText override for the notification's content text. If set to an empty
         *                    string, it will be treated as if there is no context text.
         * @throws NullPointerException if {@code contentText} is {@code null}
         */
        @NonNull
        public Builder setContentText(@Nullable CharSequence contentText) {
            this.mContentText = contentText;
            return this;
        }

        /**
         * Sets the small icon of the notification in the car screen, or
         * {@link Resources#ID_NULL} to not override the notification small icon.
         *
         * <p>This is used as the primary icon to represent the notification.
         *
         * <p>This method is equivalent to {@link NotificationCompat.Builder#setSmallIcon(int)} for
         * the car screen.
         */
        // TODO(rampara): Revisit small icon getter API
        @SuppressWarnings("MissingGetterMatchingBuilder")
        @NonNull
        public Builder setSmallIcon(int iconResId) {
            this.mSmallIconResId = iconResId;
            return this;
        }

        /**
         * Sets the large icon of the notification in the car screen, or {@code null} to not
         * override the large icon of the notification.
         *
         * <p>This is used as the secondary icon to represent the notification in the notification
         * center.
         *
         * <p>This method is equivalent to {@link NotificationCompat.Builder#setLargeIcon(Bitmap)}
         * for the car screen.
         *
         * <p>The large icon will be shown in the notification badge. If the large icon is not
         * set in the {@link CarAppExtender} or the notification, the small icon will show instead.
         */
        // TODO(rampara): Revisit small icon getter API
        @SuppressWarnings("MissingGetterMatchingBuilder")
        @NonNull
        public Builder setLargeIcon(@Nullable Bitmap bitmap) {
            this.mLargeIconBitmap = bitmap;
            return this;
        }

        /**
         * Supplies a {@link PendingIntent} to send when the notification is clicked in the car.
         *
         * <p>If set to {@code null}, the notification's content intent will be used.
         *
         * <p>In the case of navigation notifications in the rail widget, this intent will be
         * sent when the user taps on the rail widget.
         *
         * <p>This method is equivalent to
         * {@link NotificationCompat.Builder#setContentIntent(PendingIntent)} for the car screen.
         *
         * @param contentIntent override for the notification's content intent.
         */
        @NonNull
        public Builder setContentIntent(@Nullable PendingIntent contentIntent) {
            this.mContentIntent = contentIntent;
            return this;
        }

        /**
         * Supplies a {@link PendingIntent} to send when the user clears the notification by either
         * using the "clear all" functionality in the notification center, or tapping the individual
         * "close" buttons on the notification in the car screen.
         *
         * <p>If set to {@code null}, the notification's content intent will be used.
         *
         * <p>This method is equivalent to
         * {@link NotificationCompat.Builder#setDeleteIntent(PendingIntent)} for the car screen.
         *
         * @param deleteIntent override for the notification's delete intent.
         */
        @NonNull
        public Builder setDeleteIntent(@Nullable PendingIntent deleteIntent) {
            this.mDeleteIntent = deleteIntent;
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
         *               display the
         *               actions properly, a valid resource id for the icon must be provided.
         * @param title  text describing the action.
         * @param intent {@link PendingIntent} to send when the action is invoked. In the case of
         *               navigation notifications in the rail widget, this intent will be sent
         *               when the user taps on the action icon in the rail
         *               widget.
         * @throws NullPointerException if {@code title} is {@code null}.
         * @throws NullPointerException if {@code intent} is {@code null}.
         */
        // TODO(rampara): Update to remove use of deprecated Action API
        @SuppressWarnings("deprecation")
        @NonNull
        public Builder addAction(
                @DrawableRes int icon, @NonNull CharSequence title, @NonNull PendingIntent intent) {
            this.mActions.add(new Action(icon, requireNonNull(title), requireNonNull(intent)));
            return this;
        }

        /** Clears any actions that may have been added with {@link #addAction} up to this point. */
        @NonNull
        public Builder clearActions() {
            this.mActions.clear();
            return this;
        }

        /**
         * Sets the importance of the notification in the car screen.
         *
         * <p>The default value is {@link NotificationManagerCompat#IMPORTANCE_UNSPECIFIED}.
         *
         * <p>The importance is used to determine whether the notification will show as a HUN on
         * the car screen. See the class description for more details.
         *
         * <p>See {@link NotificationManagerCompat} for all supported importance
         * values.
         */
        @NonNull
        public Builder setImportance(int importance) {
            this.mImportance = importance;
            return this;
        }

        /**
         * Constructs the {@link CarAppExtender} defined by this builder.
         */
        @NonNull
        public CarAppExtender build() {
            return new CarAppExtender(this);
        }
    }
}
