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

package android.support.v4.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;
import java.util.ArrayList;

/**
 * Helper for accessing features in {@link android.app.Notification}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public class NotificationCompat {
    /**
     * Obsolete flag indicating high-priority notifications; use the priority field instead.
     * 
     * @deprecated Use {@link NotificationCompat.Builder#setPriority(int)} with a positive value.
     */
    public static final int FLAG_HIGH_PRIORITY = 0x00000080;

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

    private static final NotificationCompatImpl IMPL;

    interface NotificationCompatImpl {
        public Notification build(Builder b);
    }

    static class NotificationCompatImplBase implements NotificationCompatImpl {
        public Notification build(Builder b) {
            Notification result = (Notification) b.mNotification;
            result.setLatestEventInfo(b.mContext, b.mContentTitle,
                    b.mContentText, b.mContentIntent);
            // translate high priority requests into legacy flag
            if (b.mPriority > PRIORITY_DEFAULT) {
                result.flags |= FLAG_HIGH_PRIORITY; 
            }
            return result;
        }
    }

    static class NotificationCompatImplHoneycomb implements NotificationCompatImpl {
        public Notification build(Builder b) {
            return NotificationCompatHoneycomb.add(b.mContext, b.mNotification,
                    b.mContentTitle, b.mContentText, b.mContentInfo, b.mTickerView,
                    b.mNumber, b.mContentIntent, b.mFullScreenIntent, b.mLargeIcon);
        }
    }

    static class NotificationCompatImplIceCreamSandwich implements NotificationCompatImpl {
        public Notification build(Builder b) {
            return NotificationCompatIceCreamSandwich.add(b.mContext, b.mNotification,
                    b.mContentTitle, b.mContentText, b.mContentInfo, b.mTickerView,
                    b.mNumber, b.mContentIntent, b.mFullScreenIntent, b.mLargeIcon,
                    b.mProgressMax, b.mProgress, b.mProgressIndeterminate);
        }
    }

    static class NotificationCompatImplJellybean implements NotificationCompatImpl {
        public Notification build(Builder b) {
            NotificationCompatJellybean jbBuilder = new NotificationCompatJellybean(
                    b.mContext, b.mNotification, b.mContentTitle, b.mContentText, b.mContentInfo,
                    b.mTickerView, b.mNumber, b.mContentIntent, b.mFullScreenIntent, b.mLargeIcon,
                    b.mProgressMax, b.mProgress, b.mProgressIndeterminate,
                    b.mUseChronometer, b.mPriority, b.mSubText);
            for (Action action: b.mActions) {
                jbBuilder.addAction(action.icon, action.title, action.actionIntent);
            }
            if (b.mStyle != null) {
                if (b.mStyle instanceof BigTextStyle) {
                    BigTextStyle style = (BigTextStyle) b.mStyle;
                    jbBuilder.addBigTextStyle(style.mBigContentTitle,
                            style.mSummaryTextSet, 
                            style.mSummaryText,
                            style.mBigText);
                } else if (b.mStyle instanceof InboxStyle) {
                    InboxStyle style = (InboxStyle) b.mStyle;
                    jbBuilder.addInboxStyle(style.mBigContentTitle,
                            style.mSummaryTextSet, 
                            style.mSummaryText,
                            style.mTexts);
                } else if (b.mStyle instanceof BigPictureStyle) {
                    BigPictureStyle style = (BigPictureStyle) b.mStyle;
                    jbBuilder.addBigPictureStyle(style.mBigContentTitle,
                            style.mSummaryTextSet, 
                            style.mSummaryText,
                            style.mPicture);
                }
            }
            return(jbBuilder.build());
        }
    }

    static {
        if (Build.VERSION.SDK_INT >= 16) {
            IMPL = new NotificationCompatImplJellybean();
        } else if (Build.VERSION.SDK_INT >= 14) {
            IMPL = new NotificationCompatImplIceCreamSandwich();
        } else if (Build.VERSION.SDK_INT >= 11) {
            IMPL = new NotificationCompatImplHoneycomb();
        } else {
            IMPL = new NotificationCompatImplBase();
        }
    }

    /**
     * Builder class for {@link Notification} objects.  Allows easier control over
     * all the flags, as well as help constructing the typical notification layouts.
     */
    public static class Builder {
        Context mContext;

        CharSequence mContentTitle;
        CharSequence mContentText;
        PendingIntent mContentIntent;
        PendingIntent mFullScreenIntent;
        RemoteViews mTickerView;
        Bitmap mLargeIcon;
        CharSequence mContentInfo;
        int mNumber;
        int mPriority;
        boolean mUseChronometer;
        Style mStyle;
        CharSequence mSubText;
        int mProgressMax;
        int mProgress;
        boolean mProgressIndeterminate;
        ArrayList<Action> mActions = new ArrayList<Action>();

        Notification mNotification = new Notification();

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
         */
        public Builder(Context context) {
            mContext = context;

            // Set defaults to match the defaults of a Notification
            mNotification.when = System.currentTimeMillis();
            mNotification.audioStreamType = Notification.STREAM_DEFAULT;
            mPriority = PRIORITY_DEFAULT;
        }

        /**
         * Set the time that the event occurred.  Notifications in the panel are
         * sorted by this time.
         */
        public Builder setWhen(long when) {
            mNotification.when = when;
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
        public Builder setUsesChronometer(boolean b) {
            mUseChronometer = b;
            return this;
        }

        /**
         * Set the small icon to use in the notification layouts.  Different classes of devices
         * may return different sizes.  See the UX guidelines for more information on how to
         * design these icons.
         *
         * @param icon A resource ID in the application's package of the drawble to use.
         */
        public Builder setSmallIcon(int icon) {
            mNotification.icon = icon;
            return this;
        }

        /**
         * A variant of {@link #setSmallIcon(int) setSmallIcon(int)} that takes an additional
         * level parameter for when the icon is a {@link android.graphics.drawable.LevelListDrawable
         * LevelListDrawable}.
         *
         * @param icon A resource ID in the application's package of the drawble to use.
         * @param level The level to use for the icon.
         *
         * @see android.graphics.drawable.LevelListDrawable
         */
        public Builder setSmallIcon(int icon, int level) {
            mNotification.icon = icon;
            mNotification.iconLevel = level;
            return this;
        }

        /**
         * Set the title (first row) of the notification, in a standard notification.
         */
        public Builder setContentTitle(CharSequence title) {
            mContentTitle = title;
            return this;
        }

        /**
         * Set the text (second row) of the notification, in a standard notification.
         */
        public Builder setContentText(CharSequence text) {
            mContentText = text;
            return this;
        }

        /**
         * Set the third line of text in the platform notification template. 
         * Don't use if you're also using {@link #setProgress(int, int, boolean)};
         * they occupy the same location in the standard template.
         */
        public Builder setSubText(CharSequence text) {
            mSubText = text;
            return this;
        }

        /**
         * Set the large number at the right-hand side of the notification.  This is
         * equivalent to setContentInfo, although it might show the number in a different
         * font size for readability.
         */
        public Builder setNumber(int number) {
            mNumber = number;
            return this;
        }

        /**
         * Set the large text at the right-hand side of the notification.
         */
        public Builder setContentInfo(CharSequence info) {
            mContentInfo = info;
            return this;
        }

        /**
         * Set the progress this notification represents, which may be
         * represented as a {@link android.widget.ProgressBar}.
         */
        public Builder setProgress(int max, int progress, boolean indeterminate) {
            mProgressMax = max;
            mProgress = progress;
            mProgressIndeterminate = indeterminate;
            return this;
        }

        /**
         * Supply a custom RemoteViews to use instead of the standard one.
         */
        public Builder setContent(RemoteViews views) {
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
        public Builder setContentIntent(PendingIntent intent) {
            mContentIntent = intent;
            return this;
        }

        /**
         * Supply a {@link PendingIntent} to send when the notification is cleared by the user
         * directly from the notification panel.  For example, this intent is sent when the user
         * clicks the "Clear all" button, or the individual "X" buttons on notifications.  This
         * intent is not sent when the application calls {@link NotificationManager#cancel
         * NotificationManager.cancel(int)}.
         */
        public Builder setDeleteIntent(PendingIntent intent) {
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
         * @param intent The pending intent to launch.
         * @param highPriority Passing true will cause this notification to be sent
         *          even if other notifications are suppressed.
         */
        public Builder setFullScreenIntent(PendingIntent intent, boolean highPriority) {
            mFullScreenIntent = intent;
            setFlag(FLAG_HIGH_PRIORITY, highPriority);
            return this;
        }

        /**
         * Set the text that is displayed in the status bar when the notification first
         * arrives.
         */
        public Builder setTicker(CharSequence tickerText) {
            mNotification.tickerText = tickerText;
            return this;
        }

        /**
         * Set the text that is displayed in the status bar when the notification first
         * arrives, and also a RemoteViews object that may be displayed instead on some
         * devices.
         */
        public Builder setTicker(CharSequence tickerText, RemoteViews views) {
            mNotification.tickerText = tickerText;
            mTickerView = views;
            return this;
        }

        /**
         * Set the large icon that is shown in the ticker and notification.
         */
        public Builder setLargeIcon(Bitmap icon) {
            mLargeIcon = icon;
            return this;
        }

        /**
         * Set the sound to play.  It will play on the default stream.
         */
        public Builder setSound(Uri sound) {
            mNotification.sound = sound;
            mNotification.audioStreamType = Notification.STREAM_DEFAULT;
            return this;
        }

        /**
         * Set the sound to play.  It will play on the stream you supply.
         *
         * @see #STREAM_DEFAULT
         * @see AudioManager for the <code>STREAM_</code> constants.
         */
        public Builder setSound(Uri sound, int streamType) {
            mNotification.sound = sound;
            mNotification.audioStreamType = streamType;
            return this;
        }

        /**
         * Set the vibration pattern to use.
         *
         * @see android.os.Vibrator for a discussion of the <code>pattern</code>
         * parameter.
         */
        public Builder setVibrate(long[] pattern) {
            mNotification.vibrate = pattern;
            return this;
        }

        /**
         * Set the argb value that you would like the LED on the device to blnk, as well as the
         * rate.  The rate is specified in terms of the number of milliseconds to be on
         * and then the number of milliseconds to be off.
         */
        public Builder setLights(int argb, int onMs, int offMs) {
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
         * <p>Ongoing notifications differ from regular notifications in the following ways:
         * <ul>
         *   <li>Ongoing notifications are sorted above the regular notifications in the
         *   notification panel.</li>
         *   <li>Ongoing notifications do not have an 'X' close button, and are not affected
         *   by the "Clear all" button.
         * </ul>
         */
        public Builder setOngoing(boolean ongoing) {
            setFlag(Notification.FLAG_ONGOING_EVENT, ongoing);
            return this;
        }

        /**
         * Set this flag if you would only like the sound, vibrate
         * and ticker to be played if the notification is not already showing.
         */
        public Builder setOnlyAlertOnce(boolean onlyAlertOnce) {
            setFlag(Notification.FLAG_ONLY_ALERT_ONCE, onlyAlertOnce);
            return this;
        }

        /**
         * Setting this flag will make it so the notification is automatically
         * canceled when the user clicks it in the panel.  The PendingIntent
         * set with {@link #setDeleteIntent} will be broadcast when the notification
         * is canceled.
         */
        public Builder setAutoCancel(boolean autoCancel) {
            setFlag(Notification.FLAG_AUTO_CANCEL, autoCancel);
            return this;
        }

        /**
         * Set the default notification options that will be used.
         * <p>
         * The value should be one or more of the following fields combined with
         * bitwise-or:
         * {@link Notification#DEFAULT_SOUND}, {@link Notification#DEFAULT_VIBRATE},
         * {@link Notification#DEFAULT_LIGHTS}.
         * <p>
         * For all default values, use {@link Notification#DEFAULT_ALL}.
         */
        public Builder setDefaults(int defaults) {
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
         * Priority is an indication of how much of the user's
         * valuable attention should be consumed by this
         * notification. Low-priority notifications may be hidden from
         * the user in certain situations, while the user might be
         * interrupted for a higher-priority notification. The system
         * will make a determination about how to interpret
         * notification priority as described in MUMBLE MUMBLE.
         */
        public Builder setPriority(int pri) {
            mPriority = pri;
            return this;
        }

        /**
         * Add an action to this notification. Actions are typically displayed by
         * the system as a button adjacent to the notification content.
         *
         * @param icon Resource ID of a drawable that represents the action.
         * @param title Text describing the action.
         * @param intent PendingIntent to be fired when the action is invoked.
         */
        public Builder addAction(int icon, CharSequence title, PendingIntent intent) {
            mActions.add(new Action(icon, title, intent));
            return this;
        }

        /**
         * Add a rich notification style to be applied at build time.
         *
         * @param style Object responsible for modifying the notification style.
         */
        public Builder setStyle(Style style) {
            if (mStyle != style) {
                mStyle = style;
                if (mStyle != null) {
                    mStyle.setBuilder(this);
                }
            }
            return this;
        }

        /**
         * @deprecated Use {@link #build()} instead.
         */
        @Deprecated
        public Notification getNotification() {
            return (Notification) IMPL.build(this);
        }

        /**
         * Combine all of the options that have been set and return a new {@link Notification}
         * object.
         */
        public Notification build() {
            return (Notification) IMPL.build(this);
        }
    }

    /**
     * An object that can apply a rich notification style to a {@link Notification.Builder}
     * object.
     */
    public static abstract class Style
    {
        Builder mBuilder;
        CharSequence mBigContentTitle;
        CharSequence mSummaryText;
        boolean mSummaryTextSet = false;

        public void setBuilder(Builder builder) {
            if (mBuilder != builder) {
                mBuilder = builder;
                if (mBuilder != null) {
                    mBuilder.setStyle(this);
                }
            }
        }

        public Notification build() {
            Notification notification = null;
            if (mBuilder != null) {
                notification = mBuilder.build();
            } 
            return notification;
        }
    }

    /**
     * Helper class for generating large-format notifications that include a large image attachment.
     * 
     * This class is a "rebuilder": It attaches to a Builder object and modifies its behavior, like so:
     * <pre class="prettyprint">
     * Notification noti = new Notification.Builder()
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
        Bitmap mPicture;

        public BigPictureStyle() {
        }

        public BigPictureStyle(Builder builder) {
            setBuilder(builder);
        }

        /**
         * Overrides ContentTitle in the big form of the template.
         * This defaults to the value passed to setContentTitle().
         */
        public BigPictureStyle setBigContentTitle(CharSequence title) {
            mBigContentTitle = title;
            return this;
        }

        /**
         * Set the first line of text after the detail section in the big form of the template.
         */
        public BigPictureStyle setSummaryText(CharSequence cs) {
            mSummaryText = cs;
            mSummaryTextSet = true;
            return this;
        }

        /**
         * Provide the bitmap to be used as the payload for the BigPicture notification.
         */
        public BigPictureStyle bigPicture(Bitmap b) {
            mPicture = b;
            return this;
        }
    }

    /**
     * Helper class for generating large-format notifications that include a lot of text.
     * 
     * This class is a "rebuilder": It attaches to a Builder object and modifies its behavior, like so:
     * <pre class="prettyprint">
     * Notification noti = new Notification.Builder()
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
        CharSequence mBigText;

        public BigTextStyle() {
        }

        public BigTextStyle(Builder builder) {
            setBuilder(builder);
        }

        /**
         * Overrides ContentTitle in the big form of the template.
         * This defaults to the value passed to setContentTitle().
         */
        public BigTextStyle setBigContentTitle(CharSequence title) {
            mBigContentTitle = title;
            return this;
        }

        /**
         * Set the first line of text after the detail section in the big form of the template.
         */
        public BigTextStyle setSummaryText(CharSequence cs) {
            mSummaryText = cs;
            mSummaryTextSet = true;
            return this;
        }

        /**
         * Provide the longer text to be displayed in the big form of the
         * template in place of the content text.
         */
        public BigTextStyle bigText(CharSequence cs) {
            mBigText = cs;
            return this;
        }
    }

    /**
     * Helper class for generating large-format notifications that include a list of (up to 5) strings.
     * 
     * This class is a "rebuilder": It attaches to a Builder object and modifies its behavior, like so:
     * <pre class="prettyprint">
     * Notification noti = new Notification.Builder()
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
        ArrayList<CharSequence> mTexts = new ArrayList<CharSequence>();

        public InboxStyle() {
        }

        public InboxStyle(Builder builder) {
            setBuilder(builder);
        }

        /**
         * Overrides ContentTitle in the big form of the template.
         * This defaults to the value passed to setContentTitle().
         */
        public InboxStyle setBigContentTitle(CharSequence title) {
            mBigContentTitle = title;
            return this;
        }

        /**
         * Set the first line of text after the detail section in the big form of the template.
         */
        public InboxStyle setSummaryText(CharSequence cs) {
            mSummaryText = cs;
            mSummaryTextSet = true;
            return this;
        }

        /**
         * Append a line to the digest section of the Inbox notification.
         */
        public InboxStyle addLine(CharSequence cs) {
            mTexts.add(cs);
            return this;
        }
    }

    public static class Action {
        public int icon;
        public CharSequence title;
        public PendingIntent actionIntent;

        public Action(int icon_, CharSequence title_, PendingIntent intent_) {
            this.icon = icon_;
            this.title = title_;
            this.actionIntent = intent_;
        }
    }
}
