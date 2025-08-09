/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static androidx.core.app.NotificationCompat.DEFAULT_ALL;
import static androidx.core.app.NotificationCompat.DEFAULT_LIGHTS;
import static androidx.core.app.NotificationCompat.DEFAULT_SOUND;
import static androidx.core.app.NotificationCompat.DEFAULT_VIBRATE;
import static androidx.core.app.NotificationCompat.EXTRA_COMPAT_TEMPLATE;
import static androidx.core.app.NotificationCompat.GROUP_ALERT_ALL;
import static androidx.core.app.NotificationCompat.GROUP_ALERT_CHILDREN;
import static androidx.core.app.NotificationCompat.GROUP_ALERT_SUMMARY;
import static androidx.core.app.NotificationCompat.GROUP_KEY_SILENT;
import static androidx.core.app.NotificationTester.assertNotificationEquals;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.BaseInstrumentationTestCase;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.widget.RemoteViews;

import androidx.collection.ArraySet;
import androidx.core.R;
import androidx.core.app.NotificationCompat.MessagingStyle.Message;
import androidx.core.app.NotificationCompat.Style;
import androidx.core.content.ContextCompat;
import androidx.core.content.LocusIdCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.os.BundleCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import com.google.common.collect.Lists;

import org.jspecify.annotations.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class NotificationCompatTest extends BaseInstrumentationTestCase<TestActivity> {
    private static final String TEXT_RESULT_KEY = "text";
    private static final String DATA_RESULT_KEY = "data";
    private static final String EXTRA_COLORIZED = "android.colorized";

    Context mContext;

    public NotificationCompatTest() {
        super(TestActivity.class);
    }

    @Before
    public void setup() {
        mContext = mActivityTestRule.getActivity();
    }

    @Test
    public void testBadgeIcon() throws Throwable {
        int badgeIcon = NotificationCompat.BADGE_ICON_SMALL;
        Notification n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setBadgeIconType(badgeIcon)
                .build();
        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(badgeIcon, NotificationCompat.getBadgeIconType(n));
        } else {
            assertEquals(NotificationCompat.BADGE_ICON_NONE,
                    NotificationCompat.getBadgeIconType(n));
        }
    }

    @Test
    public void testTimeout() throws Throwable {
        long timeout = 23552;
        Notification n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setTimeoutAfter(timeout)
                .build();
        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(timeout, NotificationCompat.getTimeoutAfter(n));
        } else {
            assertEquals(0, NotificationCompat.getTimeoutAfter(n));
        }
    }

    @Test
    public void testShortcutId() throws Throwable {
        String shortcutId = "fgdfg";
        Notification n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setShortcutId(shortcutId)
                .build();
        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(shortcutId, NotificationCompat.getShortcutId(n));
        } else {
            assertEquals(null, NotificationCompat.getShortcutId(n));
        }
    }

    @Test
    public void testShortcutInfo() {
        final String shortcutId = "my-shortcut";
        final String locusId = "locus-id";
        final String title = "title";
        final ShortcutInfoCompat shortcutInfo =
                new ShortcutInfoCompat.Builder(mContext, shortcutId)
                        .setIntent(new Intent())
                        .setLocusId(new LocusIdCompat(locusId))
                        .setShortLabel(title)
                        .build();
        final Notification n =
                new NotificationCompat.Builder(mContext).setShortcutInfo(shortcutInfo).build();
        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(shortcutId, NotificationCompat.getShortcutId(n));
        } else {
            assertEquals(null, NotificationCompat.getShortcutId(n));
        }
        if (Build.VERSION.SDK_INT >= 29) {
            assertNotNull(n.getLocusId());
            assertEquals(locusId, n.getLocusId().getId());
        }
        assertEquals(title, NotificationCompat.getContentTitle(n));
    }

    @Test
    public void testLocusId() throws Throwable {
        final LocusIdCompat locusId = new LocusIdCompat("Chat_A_B");
        Notification n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setLocusId(locusId)
                .build();
        if (Build.VERSION.SDK_INT >= 29) {
            assertEquals(locusId, NotificationCompat.getLocusId(n));
        } else {
            assertEquals(null, NotificationCompat.getLocusId(n));
        }
    }

    @Test
    public void testSettingsText() {
        String settingsText = "testSettingsText";
        Notification n = new NotificationCompat.Builder(mContext)
                .setSettingsText(settingsText)
                .build();
        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(settingsText, NotificationCompat.getSettingsText(n));
        } else {
            assertEquals(null, NotificationCompat.getSettingsText(n));
        }
    }

    @Test
    public void testContentTitle() {
        Notification n = new NotificationCompat.Builder(mContext)
                .setContentTitle("testContentTitle")
                .build();
        assertEquals("testContentTitle", NotificationCompat.getContentTitle(n));
    }

    @Test
    public void testContentText() {
        Notification n = new NotificationCompat.Builder(mContext)
                .setContentText("testContentText")
                .build();
        assertEquals("testContentText", NotificationCompat.getContentText(n));
    }

    @Test
    public void testContentInfo() {
        Notification n = new NotificationCompat.Builder(mContext)
                .setContentInfo("testContentInfo")
                .build();
        assertEquals("testContentInfo", NotificationCompat.getContentInfo(n));
    }

    @Test
    public void testSubText() {
        Notification n = new NotificationCompat.Builder(mContext)
                .setSubText("testSubText")
                .build();
        assertEquals("testSubText", NotificationCompat.getSubText(n));
    }

    @Test
    public void testActions() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        Notification nWith = builder.addAction(0, "testAction", null).build();
        assertEquals(1, NotificationCompat.getActionCount(nWith));
        NotificationCompat.Action action = NotificationCompat.getAction(nWith, 0);
        assertNotNull(action);
        assertEquals("testAction", action.getTitle());

        Notification nWithout = builder.clearActions().build();
        assertEquals(0, NotificationCompat.getActionCount(nWithout));

        // Validate that the clear did not mutate the first notification
        assertEquals(1, NotificationCompat.getActionCount(nWith));
    }

    @Test
    public void testGetActions() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        builder.addAction(0, "testAction", null);
        builder.addAction(0, "testAction2", null);

        List<NotificationCompat.Action> actions = builder.mActions;

        assertEquals(2, actions.size());
        assertEquals("testAction2", actions.get(1).getTitle());
    }

    @Test
    public void testInvisibleActions() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        Notification nWith = builder.addInvisibleAction(0, "testAction", null)
                .build();
        List<NotificationCompat.Action> actions = NotificationCompat.getInvisibleActions(nWith);

        assertEquals(1, actions.size());
        assertEquals("testAction", actions.get(0).getTitle());

        Notification nWithout = builder.clearInvisibleActions().build();
        assertEquals(Collections.EMPTY_LIST, NotificationCompat.getInvisibleActions(nWithout));

        // Validate that the clear did not mutate the first notification
        assertEquals(1, NotificationCompat.getInvisibleActions(nWith).size());
    }

    @Test
    public void testShowWhen() {
        // NOTE: It's very difficult to unit test the built notification on JellyBean because
        // there was no extras field and the only affect is that the RemoteViews object has some
        // different internal state.  However, this unit test still validates that the
        // notification is built successfully (without throwing an exception).
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);

        Notification nDefault = builder.build();
        assertThat(NotificationCompat.getShowWhen(nDefault)).isTrue();

        // test true
        Notification nTrue = builder.setShowWhen(true).build();
        assertTrue(NotificationCompat.getShowWhen(nTrue));

        // test false
        Notification nFalse = builder.setShowWhen(false).build();
        assertFalse(NotificationCompat.getShowWhen(nFalse));
    }

    @Test
    public void testUsesChronometer() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);

        // test true
        Notification nTrue = builder.setUsesChronometer(true).build();
        assertTrue(NotificationCompat.getUsesChronometer(nTrue));

        // test false
        Notification nFalse = builder.setUsesChronometer(false).build();
        assertFalse(NotificationCompat.getUsesChronometer(nFalse));
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    public void testSetChronometerCountDown() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, "channelId");
        builder.setUsesChronometer(true);

        assertTrue(builder.mUseChronometer);

        builder.setChronometerCountDown(true);
        Notification n = builder.build();

        assertEquals(Boolean.TRUE, n.extras.get(NotificationCompat.EXTRA_CHRONOMETER_COUNT_DOWN));
    }

    @Test
    public void testOnlyAlertOnce() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);

        // test true
        Notification nTrue = builder.setOnlyAlertOnce(true).build();
        assertTrue(NotificationCompat.getOnlyAlertOnce(nTrue));

        // test false
        Notification nFalse = builder.setOnlyAlertOnce(false).build();
        assertFalse(NotificationCompat.getOnlyAlertOnce(nFalse));
    }

    @Test
    public void testAutoCancel() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);

        // test true
        Notification nTrue = builder.setAutoCancel(true).build();
        assertTrue(NotificationCompat.getAutoCancel(nTrue));

        // test false
        Notification nFalse = builder.setAutoCancel(false).build();
        assertFalse(NotificationCompat.getAutoCancel(nFalse));
    }

    @Test
    public void testOngoing() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);

        // test true
        Notification nTrue = builder.setOngoing(true).build();
        assertTrue(NotificationCompat.getOngoing(nTrue));

        // test false
        Notification nFalse = builder.setOngoing(false).build();
        assertFalse(NotificationCompat.getOngoing(nFalse));
    }

    @Test
    public void testColor() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);

        Notification notification = builder.setColor(Color.GREEN).build();
        assertEquals(Color.GREEN, NotificationCompat.getColor(notification));
    }

    @Test
    public void setShortCriticalText() {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext,
                "test_channel");
        final String shortCriticalText = "shortCriticalText";
        builder.setShortCriticalText(shortCriticalText);

        Notification notification = builder.build();
        assertThat(NotificationCompat.getShortCriticalText(notification))
            .isEqualTo(shortCriticalText);

        notification = builder.setShortCriticalText(null).build();
        assertThat(NotificationCompat.getShortCriticalText(notification)).isNull();
    }

    @Test
    public void setRequestPromotedOngoing() {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext,
                "test_channel");

        Notification notification = builder.build();
        // default is false.
        assertThat(NotificationCompat.isRequestPromotedOngoing(notification)).isFalse();
        // set to true.
        notification = builder.setRequestPromotedOngoing(true).build();
        assertThat(NotificationCompat.isRequestPromotedOngoing(notification)).isTrue();
    }

    @Test
    public void testVisibility() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);

        Notification n = builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC).build();
        assertEquals(NotificationCompat.VISIBILITY_PUBLIC,
                NotificationCompat.getVisibility(n));
    }

    @Test
    public void testPublicVersion() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        Notification pub = builder.setContentTitle("public title").build();
        Notification priv = builder.setContentTitle("private title").setPublicVersion(pub).build();

        assertNull(NotificationCompat.getPublicVersion(pub));
        assertNotSame(pub, NotificationCompat.getPublicVersion(priv));
        assertNotificationEquals(pub, NotificationCompat.getPublicVersion(priv));
    }

    /**
     * Validate that all concrete Style subclasses have a TEMPLATE_CLASS_NAME constant which is
     * the non-obfuscated class name.
     */
    @Test
    public void testStyle_templateClassNameField() throws Exception {
        for (Class<? extends Style> styleSubclass : getStyleSubclasses()) {
            Field field = styleSubclass.getDeclaredField("TEMPLATE_CLASS_NAME");
            field.setAccessible(true);
            assertEquals(styleSubclass.getName(), field.get(null));
        }
    }

    /**
     * Validate that all concrete Style subclasses override getClassName() to correctly return
     * the non-obfuscated class name.
     */
    @Test
    public void testStyle_getClassName() throws Exception {
        for (Class<? extends Style> styleSubclass : getStyleSubclasses()) {
            Constructor<? extends Style> ctor = styleSubclass.getDeclaredConstructor();
            ctor.setAccessible(true);
            Style style = ctor.newInstance();
            assertEquals(styleSubclass.getName(), style.getClassName());
        }
    }

    /**
     * Validate that getCompatStyleClass returns the subclass for all concrete Style subclasses.
     */
    @Test
    public void testStyle_getCompatStyleClass() throws Exception {
        for (Class<? extends Style> styleSubclass : getStyleSubclasses()) {
            assertIsStyle(styleSubclass, Style.constructCompatStyleByName(styleSubclass.getName()));
        }
    }

    /**
     * Validate that constructStyleForExtras can reinflate any default-constructed Style class.
     */
    @Test
    public void testStyle_constructStyleForExtras() throws Exception {
        for (Class<? extends Style> styleSubclass : getStyleSubclasses()) {
            final Style original;
            if (styleSubclass == NotificationCompat.MessagingStyle.class) {
                original = new NotificationCompat.MessagingStyle("Person's Name");
            } else {
                Constructor<? extends Style> ctor = styleSubclass.getDeclaredConstructor();
                ctor.setAccessible(true);
                original = ctor.newInstance();
            }
            Bundle bundle = new Bundle();
            original.addCompatExtras(bundle);
            Style result = Style.constructStyleForExtras(bundle);
            assertIsStyle(styleSubclass, result);
        }
    }

    /**
     * Validate that recovering the compat builder from a notification will correctly recover the
     * original style.
     */
    @Test
    public void testStyle_recoveredCorrectly() throws Exception {
        for (Class<? extends Style> styleSubclass : getStyleSubclasses()) {
            final Style original;
            if (styleSubclass == NotificationCompat.MessagingStyle.class) {
                original = new NotificationCompat.MessagingStyle("Person's Name");
            } else {
                Constructor<? extends Style> ctor = styleSubclass.getDeclaredConstructor();
                ctor.setAccessible(true);
                original = ctor.newInstance();
            }
            Notification n = new NotificationCompat.Builder(mContext).setStyle(original).build();
            Style result = new NotificationCompat.Builder(mContext, n).mStyle;
            assertIsStyle(styleSubclass, result);

            if (Build.VERSION.SDK_INT < 24
                    && styleSubclass == NotificationCompat.DecoratedCustomViewStyle.class) {
                // This compat style adds nothing to the bundle other than its name, and is thus
                // impossible to recover on platforms without this style in the framework.
                continue;
            }

            // Validate that this still works even if the template name is missing.
            // This is a rough test of compatibility with old versions of the support library.
            n.extras.remove(EXTRA_COMPAT_TEMPLATE);
            result = new NotificationCompat.Builder(mContext, n).mStyle;
            assertIsStyle(styleSubclass, result);
        }
    }

    static void assertIsStyle(Class<? extends Style> styleSubclass, Style style) {
        assertNotNull("Expected: " + styleSubclass, style);
        assertSame(styleSubclass, style.getClass());
    }

    private @NonNull List<Class<? extends Style>> getStyleSubclasses() {
        List<Class<? extends Style>> styleSubclasses = new ArrayList<>();
        for (Class<?> candidate : NotificationCompat.class.getClasses()) {
            try {
                if (Modifier.isAbstract(candidate.getModifiers())) {
                    continue;
                }
                styleSubclasses.add(candidate.asSubclass(Style.class));
            } catch (ClassCastException ex) {
                continue;
            }
        }
        assertFalse(styleSubclasses.isEmpty());
        return styleSubclasses;
    }

    @Test
    public void testBuilderFromNotification_fromMinimalPlatform() {
        Notification original = new Notification.Builder(mContext).build();
        Notification recovered = new NotificationCompat.Builder(mContext, original).build();
        assertEquals(original.toString(), recovered.toString());
    }

    @Test
    public void testBuilderFromNotification_fromSimpleCompat() {
        Notification original = new NotificationCompat.Builder(mContext, "channelId")
                .setContentTitle("contentTitle")
                .setContentText("contentText")
                .setNumber(3)
                .build();
        Notification recovered = new NotificationCompat.Builder(mContext, original).build();
        assertNotificationEquals(original, recovered);
    }

    @Test
    public void testBuilderFromNotification_withSmallResIdAndLargeBitmap() {
        int smallIcon = R.drawable.ic_call_answer;
        Bitmap largeIcon = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.notification_bg);
        Notification original = new NotificationCompat.Builder(mContext, "channelId")
                .setSmallIcon(smallIcon)
                .setLargeIcon(largeIcon)
                .build();

        Notification recovered = new NotificationCompat.Builder(mContext, original).build();

        assertThat(recovered.icon).isEqualTo(smallIcon);
        assertThat(recovered.largeIcon).isEqualTo(largeIcon);
    }

    @Test
    public void testBuilderFromNotification_withSmallAndLargeIcons() {
        IconCompat smallIcon = IconCompat.createWithResource(mContext, R.drawable.ic_call_answer);
        Icon largeIcon = Icon.createWithResource(mContext, R.drawable.notification_bg);
        Notification original = new NotificationCompat.Builder(mContext, "channelId")
                .setSmallIcon(smallIcon)
                .setLargeIcon(largeIcon)
                .build();

        Notification recovered = new NotificationCompat.Builder(mContext, original).build();

        // Icon doesn't implement equals(), and instances are not identical due to conversion
        // between Icon & IconCompat, so compare string representation instead.
        assertThat(recovered.getSmallIcon().toString()).isEqualTo(
                smallIcon.toIcon(mContext).toString());
        assertThat(recovered.getLargeIcon().toString()).isEqualTo(largeIcon.toString());
    }

    @Test
    public void testBuilderFromNotification_fromMessagingStyledCompat() {
        Person person1 = new Person.Builder()
                .setName("personName1")
                .setBot(true)
                .setImportant(true)
                .build();
        Person person2 = new Person.Builder()
                .setName("personName2")
                .setBot(true)
                .setImportant(true)
                .build();
        Bundle testBundle = new Bundle();
        testBundle.putString("testExtraKey", "testExtraValue");
        Notification original = new NotificationCompat.Builder(mContext, "channelId")
                .setContentTitle("contentTitle")
                .setContentText("contentText")
                .setContentInfo("contentInfo")
                .setSubText("subText")
                .setNumber(3)
                .setProgress(10, 1, false)
                .setSettingsText("settingsText")
                .setLocalOnly(true)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.MessagingStyle(person1)
                        .addMessage("Message 1", 123, person1)
                        .addMessage("Message 2", 234, person2))
                .addPerson(person1)
                .addPerson(person2)
                .addExtras(testBundle)
                .build();
        Notification recovered = new NotificationCompat.Builder(mContext, original).build();
        assertNotificationEquals(original, recovered);
    }

    @FlakyTest(bugId = 190533219)
    @Test
    public void testNotificationBuilder_foregroundServiceBehavior() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, "channelId");
        assertEquals(NotificationCompat.FOREGROUND_SERVICE_DEFAULT,
                builder.getForegroundServiceBehavior());
        builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);
        assertEquals(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE,
                builder.getForegroundServiceBehavior());

        // TODO: validate the built Notifications once there's testing API there
    }

    @Test
    public void testNotificationBuilder_createContentView() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, "channelId");
        assertNull(builder.getContentView());

        // Expect the standard notification template
        RemoteViews standardView = builder.createContentView();
        assertNotNull(standardView);
        String layoutName = mContext.getResources().getResourceName(standardView.getLayoutId());
        assertThat(layoutName).startsWith("android:layout/notification_");

        // If we set a custom view, it should be returned if there's no style
        RemoteViews customRemoteViews = new RemoteViews(mContext.getPackageName(),
                R.layout.notification_action);
        builder.setCustomContentView(customRemoteViews);

        assertSame(customRemoteViews, builder.getContentView());
        assertSame(customRemoteViews, builder.createContentView());

        // The custom view we set should be returned with this style
        builder.setStyle(new NotificationCompat.BigPictureStyle());

        assertSame(customRemoteViews, builder.getContentView());
        assertSame(customRemoteViews, builder.createContentView());

        // The custom view we set should be WRAPPED with decorated style
        builder.setStyle(new NotificationCompat.DecoratedCustomViewStyle());

        assertSame(customRemoteViews, builder.getContentView());
        RemoteViews decoratedCustomView = builder.createContentView();
        assertNotNull(decoratedCustomView);
        assertNotSame(customRemoteViews, decoratedCustomView);
        layoutName = mContext.getResources().getResourceName(decoratedCustomView.getLayoutId());
        if (Build.VERSION.SDK_INT >= 24) {
            assertThat(layoutName).startsWith("android:layout/notification_");
        } else {
            // AndroidX is providing a decorated style not available on these platforms natively
            // NOTE: this is the 'big' one because androidx has only one template, but hides
            // actions in this view.
            String packageName = mContext.getPackageName();
            assertEquals(packageName + ":layout/notification_template_custom_big", layoutName);
        }
    }

    @Test
    public void testNotificationBuilder_createBigContentView() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, "channelId");
        assertNull(builder.getBigContentView());

        // NOTE: starting in S, the bigContentView will exist even without actions.
        // Once we have a VERSION_CODE for S, this *might* be worth asserting.
        // assertNull(builder.createBigContentView());

        // Add an action so that we start getting the view
        builder.addAction(new NotificationCompat.Action(null, "action", null));

        // Expect the standard big notification template
        RemoteViews standardView = builder.createBigContentView();
        assertNotNull(standardView);
        String layoutName = mContext.getResources().getResourceName(standardView.getLayoutId());
        assertThat(layoutName).startsWith("android:layout/notification_");

        // If we set a custom view, it should be returned if there's no style
        RemoteViews customRemoteViews = new RemoteViews(mContext.getPackageName(),
                R.layout.notification_action);
        builder.setCustomBigContentView(customRemoteViews);

        assertSame(customRemoteViews, builder.getBigContentView());
        assertSame(customRemoteViews, builder.createBigContentView());

        // The custom view we set should be returned with this style
        builder.setStyle(new NotificationCompat.BigPictureStyle());

        assertSame(customRemoteViews, builder.getBigContentView());
        assertSame(customRemoteViews, builder.createBigContentView());

        // The custom view we set should be WRAPPED with decorated style
        builder.setStyle(new NotificationCompat.DecoratedCustomViewStyle());

        assertSame(customRemoteViews, builder.getBigContentView());
        RemoteViews decoratedCustomView = builder.createBigContentView();
        assertNotNull(decoratedCustomView);
        assertNotSame(customRemoteViews, decoratedCustomView);
        layoutName = mContext.getResources().getResourceName(decoratedCustomView.getLayoutId());
        if (Build.VERSION.SDK_INT >= 24) {
            assertThat(layoutName).startsWith("android:layout/notification_");
        } else {
            // AndroidX is providing a decorated style not available on these platforms natively
            String packageName = mContext.getPackageName();
            assertEquals(packageName + ":layout/notification_template_custom_big", layoutName);
        }
    }

    @Test
    public void testNotificationBuilder_createHeadsUpContentView() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, "channelId");
        assertNull(builder.getHeadsUpContentView());

        // The view will be null if there are no actions
        assertNull(builder.createHeadsUpContentView());

        // Add an action so that we start getting the view
        builder.addAction(new NotificationCompat.Action(null, "action", null));

        // Expect the standard big notification template (yes, heads up defaults to big template)
        RemoteViews standardView = builder.createHeadsUpContentView();
        assertNotNull(standardView);
        String layoutName = mContext.getResources().getResourceName(standardView.getLayoutId());
        assertThat(layoutName).startsWith("android:layout/notification_");

        // If we set a custom view, it should be returned if there's no style
        RemoteViews customRemoteViews = new RemoteViews(mContext.getPackageName(),
                R.layout.notification_action);
        builder.setCustomHeadsUpContentView(customRemoteViews);

        assertSame(customRemoteViews, builder.getHeadsUpContentView());
        assertSame(customRemoteViews, builder.createHeadsUpContentView());

        // The custom view we set should be returned with this style
        builder.setStyle(new NotificationCompat.BigPictureStyle());

        assertSame(customRemoteViews, builder.getHeadsUpContentView());
        assertSame(customRemoteViews, builder.createHeadsUpContentView());

        // The custom view we set should be WRAPPED with decorated style
        builder.setStyle(new NotificationCompat.DecoratedCustomViewStyle());

        assertSame(customRemoteViews, builder.getHeadsUpContentView());
        RemoteViews decoratedCustomView = builder.createHeadsUpContentView();
        assertNotNull(decoratedCustomView);
        assertNotSame(customRemoteViews, decoratedCustomView);
        layoutName = mContext.getResources().getResourceName(decoratedCustomView.getLayoutId());
        if (Build.VERSION.SDK_INT >= 24) {
            assertThat(layoutName).startsWith("android:layout/notification_");
        } else {
            // AndroidX is providing a decorated style not available on these platforms natively
            String packageName = mContext.getPackageName();
            assertEquals(packageName + ":layout/notification_template_custom_big", layoutName);
        }
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    public void testGetTextsFromContentView() {
        Notification n1 = new NotificationCompat.Builder(mContext, "channelId")
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .build();
        List<CharSequence> texts = NotificationCompat.DecoratedCustomViewStyle
                .getTextsFromContentView(mContext, n1);
        assertThat(texts).isEmpty();

        Notification n2 = new NotificationCompat.Builder(mContext, "channelId")
                .setStyle(new NotificationCompat.BigTextStyle())
                .build();
        texts = NotificationCompat.DecoratedCustomViewStyle.getTextsFromContentView(mContext, n2);
        assertThat(texts).isEmpty();

        Notification n3 = new NotificationCompat.Builder(mContext, "channelId")
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(new RemoteViews(mContext.getPackageName(),
                        androidx.core.test.R.layout.notification_custom_content_view))
                .build();
        texts = NotificationCompat.DecoratedCustomViewStyle.getTextsFromContentView(mContext, n3);
        assertThat(texts).isNotEmpty();
        assertTrue(texts.containsAll(List.of("Test title", "Test info")));

        Notification n4 = new NotificationCompat.Builder(mContext, "channelId")
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomBigContentView(new RemoteViews(mContext.getPackageName(),
                        androidx.core.test.R.layout.notification_custom_content_view))
                .build();
        texts = NotificationCompat.DecoratedCustomViewStyle.getTextsFromContentView(mContext, n4);
        assertThat(texts).isNotEmpty();
        assertTrue(texts.containsAll(List.of("Test title", "Test info")));

        Notification n5 = new NotificationCompat.Builder(mContext, "channelId")
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomHeadsUpContentView(new RemoteViews(mContext.getPackageName(),
                        androidx.core.test.R.layout.notification_custom_content_view))
                .build();
        texts = NotificationCompat.DecoratedCustomViewStyle.getTextsFromContentView(mContext, n5);
        assertThat(texts).isNotEmpty();
        assertTrue(texts.containsAll(List.of("Test title", "Test info")));
    }

    @Test
    public void testNotificationChannel() throws Throwable {
        String channelId = "new ID";
        Notification n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setChannelId(channelId)
                .build();
        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(channelId, NotificationCompat.getChannelId(n));
        } else {
            assertNull(NotificationCompat.getChannelId(n));
        }
    }

    @Test
    public void testNotificationChannel_assignedFromBuilder() throws Throwable {
        String channelId = "new ID";
        Notification n = new NotificationCompat.Builder(mActivityTestRule.getActivity(), channelId)
                .build();
        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(channelId, NotificationCompat.getChannelId(n));
        } else {
            assertNull(NotificationCompat.getChannelId(n));
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testNotificationActionBuilder_assignsColorized() throws Throwable {
        Notification n = newNotificationBuilder().setColorized(true).build();
        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(Boolean.TRUE, n.extras.get(EXTRA_COLORIZED));
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testNotificationActionBuilder_unassignesColorized() throws Throwable {
        Notification n = newNotificationBuilder().setColorized(false).build();
        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(Boolean.FALSE, n.extras.get(EXTRA_COLORIZED));
        }
    }

    @Test
    public void testNotificationActionBuilder_doesntAssignColorized() throws Throwable {
        Notification n = newNotificationBuilder().build();
        if (Build.VERSION.SDK_INT >= 26) {
            assertFalse(n.extras.containsKey(EXTRA_COLORIZED));
        }
    }

    @Test
    public void testNotificationActionBuilder_copiesIcon() {
        NotificationCompat.Action a = new NotificationCompat.Action.Builder(
                R.drawable.notification_action_background, "title", null).build();
        assertEquals(R.drawable.notification_action_background, a.getIconCompat().getResId());

        NotificationCompat.Action aCopy = new NotificationCompat.Action.Builder(a).build();

        assertEquals(R.drawable.notification_action_background, aCopy.getIconCompat().getResId());
    }

    @Test
    public void testNotificationActionBuilder_copiesRemoteInputs() throws Throwable {
        NotificationCompat.Action a = newActionBuilder()
                .addRemoteInput(new RemoteInput("a", "b", null, false,
                        RemoteInput.EDIT_CHOICES_BEFORE_SENDING_AUTO, null, null)).build();

        NotificationCompat.Action aCopy = new NotificationCompat.Action.Builder(a).build();

        assertSame(a.getRemoteInputs()[0], aCopy.getRemoteInputs()[0]);
    }

    @Test
    public void testNotificationActionBuilder_copiesAllowGeneratedReplies() throws Throwable {
        NotificationCompat.Action a = newActionBuilder()
                .setAllowGeneratedReplies(true).build();

        NotificationCompat.Action aCopy = new NotificationCompat.Action.Builder(a).build();

        assertEquals(a.getAllowGeneratedReplies(), aCopy.getAllowGeneratedReplies());
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    public void testFrameworkNotificationActionBuilder_setAllowGeneratedRepliesTrue()
            throws Throwable {
        Notification notif = new Notification.Builder(mContext)
                .addAction(new Notification.Action.Builder(0, "title", null)
                        .setAllowGeneratedReplies(true).build()).build();
        NotificationCompat.Action action = NotificationCompat.getAction(notif, 0);
        assertTrue(action.getAllowGeneratedReplies());
    }

    @Test
    public void testNotificationActionBuilder_defaultAllowGeneratedRepliesTrue() throws Throwable {
        NotificationCompat.Action a = newActionBuilder().build();

        assertTrue(a.getAllowGeneratedReplies());
    }

    @Test
    public void testNotificationActionBuilder_defaultShowsUserInterfaceTrue() {
        NotificationCompat.Action action = newActionBuilder().build();

        assertTrue(action.getShowsUserInterface());
    }

    @Test
    public void testNotificationAction_defaultAllowGeneratedRepliesTrue() throws Throwable {
        NotificationCompat.Action a = new NotificationCompat.Action(0, null, null);

        assertTrue(a.getAllowGeneratedReplies());
    }

    @Test
    public void testNotificationAction_defaultShowsUserInterfaceTrue() {
        NotificationCompat.Action action = new NotificationCompat.Action(0, null, null);

        assertTrue(action.getShowsUserInterface());
    }

    @Test
    public void testNotificationActionBuilder_setAllowGeneratedRepliesFalse() throws Throwable {
        NotificationCompat.Action a = newActionBuilder()
                .setAllowGeneratedReplies(false).build();

        assertFalse(a.getAllowGeneratedReplies());
    }

    @Test
    public void testNotificationAction_setShowsUserInterfaceFalse() {
        NotificationCompat.Action action = newActionBuilder()
                .setShowsUserInterface(false).build();

        assertFalse(action.getShowsUserInterface());
    }

    @Test
    public void testGetActionCompatFromAction_sameIconResourceId() {
        NotificationCompat.Action action = new NotificationCompat.Action.Builder(
                R.drawable.notification_bg, "title", null).build();
        assertEquals(R.drawable.notification_bg, action.getIconCompat().getResId());
        Notification notification = newNotificationBuilder().addAction(action).build();

        NotificationCompat.Action result =
                NotificationCompat.getActionCompatFromAction(notification.actions[0]);

        assertEquals(R.drawable.notification_bg, result.getIconCompat().getResId());
    }

    @Test
    public void testGetActionCompatFromAction_showsUserInterface() {
        NotificationCompat.Action action = newActionBuilder()
                .setShowsUserInterface(false).build();
        Notification notification = newNotificationBuilder().addAction(action).build();
        NotificationCompat.Action result =
                NotificationCompat.getActionCompatFromAction(notification.actions[0]);

        assertFalse(result.getExtras().getBoolean(
                NotificationCompat.Action.EXTRA_SHOWS_USER_INTERFACE, true));
        assertFalse(result.getShowsUserInterface());
    }

    @Test
    public void testGetActionCompatFromAction_withRemoteInputs_doesntCrash() {
        NotificationCompat.Action action = newActionBuilder()
                .addRemoteInput(new RemoteInput(
                        "a",
                        "b",
                        null /* choices */,
                        false /* allowFreeFormTextInput */,
                        RemoteInput.EDIT_CHOICES_BEFORE_SENDING_AUTO,
                        null /* extras */,
                        null /* allowedDataTypes */)).build();
        Notification notification = newNotificationBuilder().addAction(action).build();

        NotificationCompat.Action result =
                NotificationCompat.getActionCompatFromAction(notification.actions[0]);

        assertEquals(1, result.getRemoteInputs().length);
    }

    @Test
    public void testNotificationWearableExtenderAction_noIcon() throws Throwable {
        NotificationCompat.Action a = new NotificationCompat.Action.Builder(0, "title", null)
                .build();
        NotificationCompat.WearableExtender extender = new NotificationCompat.WearableExtender()
                .addAction(a);
        Notification notification = newNotificationBuilder().extend(extender).build();
        NotificationCompat.Action actualAction =
                new NotificationCompat.WearableExtender(notification).getActions().get(0);
        assertNull(actualAction.getIconCompat());
    }

    @Test
    public void testNotificationWearableExtenderAction_drawableIcon() throws Throwable {
        NotificationCompat.Action a =
                new NotificationCompat.Action.Builder(android.R.drawable.ic_delete, "title", null)
                        .build();
        NotificationCompat.WearableExtender extender = new NotificationCompat.WearableExtender()
                .addAction(a);
        Notification notification = newNotificationBuilder().extend(extender).build();
        NotificationCompat.Action actualAction =
                new NotificationCompat.WearableExtender(notification).getActions().get(0);
        assertEquals(android.R.drawable.ic_delete, actualAction.getIconCompat().getResId());
    }

    @Test
    public void testNotificationWearableExtenderAction_setAllowGeneratedRepliesTrue()
            throws Throwable {
        NotificationCompat.Action a = newActionBuilder()
                .setAllowGeneratedReplies(true).build();
        NotificationCompat.WearableExtender extender = new NotificationCompat.WearableExtender()
                .addAction(a);
        Notification notification = newNotificationBuilder().extend(extender).build();
        assertTrue(new NotificationCompat.WearableExtender(notification).getActions().get(0)
                .getAllowGeneratedReplies());
    }

    @Test
    public void testNotificationWearableExtenderAction_setAllowGeneratedRepliesFalse()
            throws Throwable {
        NotificationCompat.Action a = newActionBuilder()
                .setAllowGeneratedReplies(false).build();
        NotificationCompat.WearableExtender extender = new NotificationCompat.WearableExtender()
                .addAction(a);
        Notification notification = newNotificationBuilder().extend(extender).build();
        assertFalse(new NotificationCompat.WearableExtender(notification).getActions().get(0)
                .getAllowGeneratedReplies());
    }

    @Test
    public void testNotificationSmallIcon() {
        IconCompat icon = IconCompat.createWithResource(mContext,
                R.drawable.notification_action_background);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);

        builder.setSmallIcon(icon);

        Notification notification = builder.build();

        assertEquals(icon.toIcon(mContext).toString(), notification.getSmallIcon().toString());
    }

    @Test
    public void testNotificationActionBuilder_setDataOnlyRemoteInput() throws Throwable {
        NotificationCompat.Action a = newActionBuilder()
                .addRemoteInput(newDataOnlyRemoteInput()).build();
        RemoteInput[] textInputs = a.getRemoteInputs();
        assertTrue(textInputs == null || textInputs.length == 0);
        verifyRemoteInputArrayHasSingleResult(a.getDataOnlyRemoteInputs(), DATA_RESULT_KEY);
    }

    @Test
    public void testNotificationActionBuilder_setTextAndDataOnlyRemoteInput() throws Throwable {
        NotificationCompat.Action a = newActionBuilder()
                .addRemoteInput(newDataOnlyRemoteInput())
                .addRemoteInput(newTextRemoteInput())
                .build();

        verifyRemoteInputArrayHasSingleResult(a.getRemoteInputs(), TEXT_RESULT_KEY);
        verifyRemoteInputArrayHasSingleResult(a.getDataOnlyRemoteInputs(), DATA_RESULT_KEY);
    }

    @Test
    public void testMessage_setAndGetExtras() throws Throwable {
        String extraKey = "extra_key";
        CharSequence extraValue = "extra_value";
        Message m =
                new Message("text", 0 /*timestamp */, "sender");
        m.getExtras().putCharSequence(extraKey, extraValue);
        assertEquals(extraValue, m.getExtras().getCharSequence(extraKey));

        ArrayList<Message> messages = new ArrayList<>(1);
        messages.add(m);
        Bundle[] bundleArray =
                Message.getBundleArrayForMessages(messages);
        assertEquals(1, bundleArray.length);
        Message fromBundle =
                Message.getMessageFromBundle(bundleArray[0]);
        assertEquals(extraValue, fromBundle.getExtras().getCharSequence(extraKey));
    }

    @Test
    public void testGetGroupAlertBehavior() throws Throwable {
        Notification n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setGroupAlertBehavior(GROUP_ALERT_CHILDREN)
                .build();
        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(GROUP_ALERT_CHILDREN, NotificationCompat.getGroupAlertBehavior(n));
        } else {
            assertEquals(GROUP_ALERT_ALL, NotificationCompat.getGroupAlertBehavior(n));
        }
    }

    @Test
    public void testGroupAlertBehavior_mutesGroupNotifications() throws Throwable {
        // valid between api 20, when groups were added, and api 25, the last to use sound
        // and vibration from the notification itself

        Notification n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setGroupAlertBehavior(GROUP_ALERT_CHILDREN)
                .setVibrate(new long[]{235})
                .setSound(Uri.EMPTY)
                .setDefaults(DEFAULT_ALL)
                .setGroup("grouped")
                .setGroupSummary(true)
                .build();

        Notification n2 = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                .setVibrate(new long[]{235})
                .setSound(Uri.EMPTY)
                .setDefaults(DEFAULT_ALL)
                .setGroup("grouped")
                .setGroupSummary(false)
                .build();

        if (Build.VERSION.SDK_INT < 26) {
            assertNull(n.sound);
            assertNull(n.vibrate);
            assertTrue((n.defaults & DEFAULT_LIGHTS) != 0);
            assertTrue((n.defaults & DEFAULT_SOUND) == 0);
            assertTrue((n.defaults & DEFAULT_VIBRATE) == 0);

            assertNull(n2.sound);
            assertNull(n2.vibrate);
            assertTrue((n2.defaults & DEFAULT_LIGHTS) != 0);
            assertTrue((n2.defaults & DEFAULT_SOUND) == 0);
            assertTrue((n2.defaults & DEFAULT_VIBRATE) == 0);
        }
    }

    @Test
    public void testSetNotification_setLargeIconNull() {
        Notification n = new NotificationCompat.Builder(mContext, "channelId")
                .setSmallIcon(1)
                .setLargeIcon((Bitmap) null)
                .build();

        Bundle extras = NotificationCompat.getExtras(n);
        assertNotNull(extras);
        if (Build.VERSION.SDK_INT == 23) {
            assertFalse(extras.containsKey(NotificationCompat.EXTRA_LARGE_ICON));
        } else {
            assertTrue(extras.containsKey(NotificationCompat.EXTRA_LARGE_ICON));
            assertNull(extras.get(NotificationCompat.EXTRA_LARGE_ICON));
        }

        assertNull(n.getLargeIcon());
    }

    @Test
    public void testSetNotification_setLargeIconBitmap() {
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.notification_bg_low_pressed);
        Notification n = new NotificationCompat.Builder(mContext, "channelId")
                .setSmallIcon(1)
                .setLargeIcon(bitmap)
                .build();

        // Extras are not populated before API 19.
        Bundle extras = NotificationCompat.getExtras(n);
        assertNotNull(extras);
        assertTrue(extras.containsKey(NotificationCompat.EXTRA_LARGE_ICON));
        assertNotNull(extras.get(NotificationCompat.EXTRA_LARGE_ICON));
        assertNotNull(n.getLargeIcon());
    }

    @Test
    public void testSetNotification_setLargeIconNullIcon() {
        Notification n = new NotificationCompat.Builder(mContext, "channelId")
                .setSmallIcon(1)
                .setLargeIcon((Icon) null)
                .build();

        assertNull(n.getLargeIcon());

        Bundle extras = NotificationCompat.getExtras(n);
        assertNotNull(extras);
        // Prior to API version 24, EXTRA_LARGE_ICON was not set if largeIcon was set to null.
        // Starting in version 24, EXTRA_LARGE_ICON is set, but its value is null.
        if (Build.VERSION.SDK_INT == 23) {
            assertFalse(extras.containsKey(NotificationCompat.EXTRA_LARGE_ICON));
        } else {
            assertTrue(extras.containsKey(NotificationCompat.EXTRA_LARGE_ICON));
            assertNull(extras.get(NotificationCompat.EXTRA_LARGE_ICON));
        }

    }

    @Test
    public void testSetNotification_setLargeIconIcon() {
        IconCompat iconCompat = IconCompat.createWithResource(mContext,
                R.drawable.notification_bg_low_pressed);
        Icon icon = iconCompat.toIcon(mContext);

        Notification n = new NotificationCompat.Builder(mContext, "channelId")
                .setSmallIcon(1)
                .setLargeIcon(icon)
                .build();

        Bundle extras = NotificationCompat.getExtras(n);
        assertNotNull(extras);
        assertTrue(extras.containsKey(NotificationCompat.EXTRA_LARGE_ICON));
        assertNotNull(extras.get(NotificationCompat.EXTRA_LARGE_ICON));
        if (Build.VERSION.SDK_INT >= 28) {
            assertEquals(n.getLargeIcon().getResId(), icon.getResId());
            Icon recoveredIcon = extras.getParcelable(NotificationCompat.EXTRA_LARGE_ICON);
            assertEquals(icon.getResId(), recoveredIcon.getResId());
        }
    }

    @SdkSuppress(maxSdkVersion = 26)
    @Test
    public void testSetNotification_setLargeIconBitmapScales() {
        // Original icon is 860x860
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.notification_oversize_large_icon_bg);

        Notification n = new NotificationCompat.Builder(mContext, "channelId")
                .setSmallIcon(1)
                .setLargeIcon(bitmap)
                .build();

        Icon recoveredIcon = n.getLargeIcon();
        Drawable drawable = recoveredIcon.loadDrawable(mContext);
        // Scale has reduced its height and width.
        assertTrue(drawable.getIntrinsicHeight() < 860);
        assertTrue(drawable.getIntrinsicWidth() < 860);
    }

    @Test
    public void testReduceLargeIconSize_nullIcon() {
        assertNull(NotificationCompat.reduceLargeIconSize(mContext, null));
    }

    @SdkSuppress(minSdkVersion = 27)
    @Test
    public void testReduceLargeIconSize_doesNotResizeInModernVersions() {
        // We expect the function to do nothing for API 27 and higher, where scaling is not needed.
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.notification_oversize_large_icon_bg);
        assertEquals(bitmap, NotificationCompat.reduceLargeIconSize(mContext, bitmap));
    }

    @SdkSuppress(maxSdkVersion = 26)
    @Test
    public void testReduceLargeIconSize() {
        // Original icon is 860x860; set inScaled to false to validate the unscaled size.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.notification_oversize_large_icon_bg, options);

        assertEquals(860, bitmap.getWidth());
        assertEquals(860, bitmap.getHeight());

        // In the case that the bitmap is larger than the max allowable width or height, we expect
        // reduceLargeIconSize scales it down.
        // Because each device sets this differently, we only want to test the expectation that
        // the size is reduced on the devices where it's appropriate.
        int maxWidth =
                mContext.getResources().getDimensionPixelSize(
                        R.dimen.compat_notification_large_icon_max_width);
        int maxHeight =
                mContext.getResources().getDimensionPixelSize(
                        R.dimen.compat_notification_large_icon_max_height);
        if (maxWidth < 860 || maxHeight < 860) {
            // We don't check the exact size because it varies based on the device scaling factor.
            Bitmap newBitmap = NotificationCompat.reduceLargeIconSize(mContext, bitmap);
            assertTrue(newBitmap.getWidth() < 860);
            assertTrue(newBitmap.getHeight() < 860);
        }
    }

    @Test
    public void testSetNotificationSilent() {

        Notification nSummary = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setVibrate(new long[]{235})
                .setSound(Uri.EMPTY)
                .setDefaults(DEFAULT_ALL)
                .setGroupSummary(true)
                .setTicker("summary")
                .setSilent(true)
                .build();

        Notification nChild = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setVibrate(new long[]{235})
                .setSound(Uri.EMPTY)
                .setDefaults(DEFAULT_ALL)
                .setGroupSummary(false)
                .setTicker("child")
                .setSilent(true)
                .build();

        Notification nNoisy = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setVibrate(new long[]{235})
                .setSound(Uri.EMPTY)
                .setDefaults(DEFAULT_ALL)
                .setTicker("noisy")
                .setNotificationSilent()
                .setSilent(false)
                .build();

        if (Build.VERSION.SDK_INT < 26) {
            assertNull(nSummary.sound);
            assertNull(nSummary.vibrate);
            assertTrue((nSummary.defaults & DEFAULT_LIGHTS) != 0);
            assertTrue((nSummary.defaults & DEFAULT_SOUND) == 0);
            assertTrue((nSummary.defaults & DEFAULT_VIBRATE) == 0);

            assertNull(nChild.sound);
            assertNull(nChild.vibrate);
            assertTrue((nChild.defaults & DEFAULT_LIGHTS) != 0);
            assertTrue((nChild.defaults & DEFAULT_SOUND) == 0);
            assertTrue((nChild.defaults & DEFAULT_VIBRATE) == 0);

            assertNotNull(nNoisy.sound);
            assertNotNull(nNoisy.vibrate);
            assertTrue((nNoisy.defaults & DEFAULT_LIGHTS) != 0);
            assertTrue((nNoisy.defaults & DEFAULT_SOUND) != 0);
            assertTrue((nNoisy.defaults & DEFAULT_VIBRATE) != 0);
        }

        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(GROUP_ALERT_SUMMARY, nChild.getGroupAlertBehavior());
            assertEquals(GROUP_ALERT_CHILDREN, nSummary.getGroupAlertBehavior());
            assertEquals(GROUP_ALERT_ALL, nNoisy.getGroupAlertBehavior());
            assertEquals(GROUP_KEY_SILENT, nChild.getGroup());
            assertEquals(GROUP_KEY_SILENT, nSummary.getGroup());
            assertNull(nNoisy.getGroup());
        } else {
            assertNull(nChild.getGroup());
            assertNull(nSummary.getGroup());
            assertNull(nNoisy.getGroup());
        }
    }

    @Test
    public void testSetNotificationSilent_doesNotOverrideGroup() throws Throwable {
        final String groupKey = "grouped";

        Notification nSummary = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setVibrate(new long[]{235})
                .setSound(Uri.EMPTY)
                .setDefaults(DEFAULT_ALL)
                .setGroupSummary(true)
                .setGroup(groupKey)
                .setTicker("summary")
                .setNotificationSilent()
                .build();

        Notification nChild = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setVibrate(new long[]{235})
                .setSound(Uri.EMPTY)
                .setDefaults(DEFAULT_ALL)
                .setGroupSummary(false)
                .setGroup(groupKey)
                .setTicker("child")
                .setNotificationSilent()
                .build();

        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(GROUP_ALERT_SUMMARY, nChild.getGroupAlertBehavior());
            assertEquals(GROUP_ALERT_CHILDREN, nSummary.getGroupAlertBehavior());
        }
        assertEquals(groupKey, nChild.getGroup());
        assertEquals(groupKey, nSummary.getGroup());
    }

    @Test
    public void testSetNotificationSilent_notSilenced() throws Throwable {

        Notification nSummary = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setVibrate(new long[]{235})
                .setSound(Uri.EMPTY)
                .setDefaults(DEFAULT_ALL)
                .setGroup("grouped")
                .setGroupSummary(true)
                .build();

        Notification nChild = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setVibrate(new long[]{235})
                .setSound(Uri.EMPTY)
                .setDefaults(DEFAULT_ALL)
                .setGroup("grouped")
                .setGroupSummary(false)
                .build();

        assertNotNull(nSummary.sound);
        assertNotNull(nSummary.vibrate);
        assertTrue((nSummary.defaults & DEFAULT_LIGHTS) != 0);
        assertTrue((nSummary.defaults & DEFAULT_SOUND) != 0);
        assertTrue((nSummary.defaults & DEFAULT_VIBRATE) != 0);

        assertNotNull(nChild.sound);
        assertNotNull(nChild.vibrate);
        assertTrue((nChild.defaults & DEFAULT_LIGHTS) != 0);
        assertTrue((nChild.defaults & DEFAULT_SOUND) != 0);
        assertTrue((nChild.defaults & DEFAULT_VIBRATE) != 0);

        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(GROUP_ALERT_ALL, nChild.getGroupAlertBehavior());
            assertEquals(GROUP_ALERT_ALL, nSummary.getGroupAlertBehavior());
        }
    }

    @Test
    public void testGroupAlertBehavior_doesNotMuteIncorrectGroupNotifications() throws Throwable {
        Notification n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                .setVibrate(new long[]{235})
                .setSound(Uri.EMPTY)
                .setDefaults(DEFAULT_ALL)
                .setGroup("grouped")
                .setGroupSummary(true)
                .build();

        Notification n2 = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setGroupAlertBehavior(GROUP_ALERT_CHILDREN)
                .setVibrate(new long[]{235})
                .setSound(Uri.EMPTY)
                .setDefaults(DEFAULT_ALL)
                .setGroup("grouped")
                .setGroupSummary(false)
                .build();

        Notification n3 = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setVibrate(new long[]{235})
                .setSound(Uri.EMPTY)
                .setDefaults(DEFAULT_ALL)
                .setGroup("grouped")
                .setGroupSummary(false)
                .build();

        if (Build.VERSION.SDK_INT < 26) {
            assertNotNull(n.sound);
            assertNotNull(n.vibrate);
            assertTrue((n.defaults & DEFAULT_LIGHTS) != 0);
            assertTrue((n.defaults & DEFAULT_SOUND) != 0);
            assertTrue((n.defaults & DEFAULT_VIBRATE) != 0);

            assertNotNull(n2.sound);
            assertNotNull(n2.vibrate);
            assertTrue((n2.defaults & DEFAULT_LIGHTS) != 0);
            assertTrue((n2.defaults & DEFAULT_SOUND) != 0);
            assertTrue((n2.defaults & DEFAULT_VIBRATE) != 0);

            assertNotNull(n3.sound);
            assertNotNull(n3.vibrate);
            assertTrue((n3.defaults & DEFAULT_LIGHTS) != 0);
            assertTrue((n3.defaults & DEFAULT_SOUND) != 0);
            assertTrue((n3.defaults & DEFAULT_VIBRATE) != 0);
        }
    }

    @Test
    public void testGroupAlertBehavior_doesNotMuteNonGroupNotifications() throws Throwable {
        Notification n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setGroupAlertBehavior(GROUP_ALERT_CHILDREN)
                .setVibrate(new long[]{235})
                .setSound(Uri.EMPTY)
                .setDefaults(DEFAULT_ALL)
                .setGroup(null)
                .setGroupSummary(false)
                .build();
        if (!(Build.VERSION.SDK_INT >= 26)) {
            assertNotNull(n.sound);
            assertNotNull(n.vibrate);
            assertTrue((n.defaults & DEFAULT_LIGHTS) != 0);
            assertTrue((n.defaults & DEFAULT_SOUND) != 0);
            assertTrue((n.defaults & DEFAULT_VIBRATE) != 0);
        }
    }

    @Test
    public void testHasAudioAttributesFrom21() throws Throwable {
        Notification n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setSound(Uri.EMPTY)
                .build();
        assertNotNull(n.audioAttributes);
        assertEquals(-1, n.audioStreamType);
        assertEquals(Uri.EMPTY, n.sound);

        n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setSound(Uri.EMPTY, AudioManager.STREAM_RING)
                .build();
        assertNotNull(n.audioAttributes);
        assertEquals(AudioAttributes.USAGE_NOTIFICATION_RINGTONE, n.audioAttributes.getUsage());
        assertEquals(-1, n.audioStreamType);
        assertEquals(Uri.EMPTY, n.sound);
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testClearAlertingFieldsIfUsingChannels() throws Throwable {
        long[] vibration = new long[]{100};

        // stripped if using channels
        Notification n = new NotificationCompat.Builder(mActivityTestRule.getActivity(), "test")
                .setSound(Uri.EMPTY)
                .setDefaults(Notification.DEFAULT_ALL)
                .setVibrate(vibration)
                .setChronometerCountDown(true)
                .setLights(Color.BLUE, 100, 100)
                .build();
        assertNull(n.sound);
        assertEquals(0, n.defaults);
        assertNull(n.vibrate);
        assertEquals(0, n.ledARGB);
        assertEquals(0, n.ledOnMS);
        assertEquals(0, n.ledOffMS);

        // left intact if not using channels
        n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setSound(Uri.EMPTY)
                .setDefaults(Notification.DEFAULT_ALL)
                .setVibrate(vibration)
                .setLights(Color.BLUE, 100, 100)
                .build();
        assertEquals(Uri.EMPTY, n.sound);
        assertNotNull(n.audioAttributes);
        assertEquals(Notification.DEFAULT_ALL, n.defaults);
        assertEquals(vibration, n.vibrate);
        assertEquals(Color.BLUE, n.ledARGB);
        assertEquals(100, n.ledOnMS);
        assertEquals(100, n.ledOffMS);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testBigPictureStyle_withNullBigLargeIcon() {
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.notification_bg_low_pressed);
        Notification n = new NotificationCompat.Builder(mContext, "channelId")
                .setSmallIcon(1)
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon((Bitmap) null)
                        .setBigContentTitle("Big Content Title")
                        .setSummaryText("Summary Text"))
                .build();
        Bundle extras = NotificationCompat.getExtras(n);
        assertNotNull(extras);
        assertTrue(extras.containsKey(NotificationCompat.EXTRA_LARGE_ICON_BIG));
        assertNull(extras.get(NotificationCompat.EXTRA_LARGE_ICON_BIG));
    }

    @Test
    public void testBigPictureStyle_withIconNullBigLargeIcon() {
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.notification_bg_low_pressed);
        Notification n = new NotificationCompat.Builder(mContext, "channelId")
                .setSmallIcon(1)
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon((Icon) null)
                        .setBigContentTitle("Big Content Title")
                        .setSummaryText("Summary Text"))
                .build();
        Bundle extras = NotificationCompat.getExtras(n);
        assertNotNull(extras);
        assertTrue(extras.containsKey(NotificationCompat.EXTRA_LARGE_ICON_BIG));
        assertNull(extras.get(NotificationCompat.EXTRA_LARGE_ICON_BIG));
    }

    @Test
    public void testBigPictureStyle_withIconBigLargeIcon() {
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.notification_bg_low_pressed);
        IconCompat iconCompat = IconCompat.createWithResource(mContext, R.drawable.ic_call_decline);
        Icon icon = iconCompat.toIcon(mContext);

        Notification n = new NotificationCompat.Builder(mContext, "channelId")
                .setSmallIcon(1)
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(icon)
                        .setBigContentTitle("Big Content Title")
                        .setSummaryText("Summary Text"))
                .build();
        Bundle extras = NotificationCompat.getExtras(n);
        assertNotNull(extras);
        assertTrue(extras.containsKey(NotificationCompat.EXTRA_LARGE_ICON_BIG));
        assertNotNull(extras.get(NotificationCompat.EXTRA_LARGE_ICON_BIG));

        Icon recoveredIcon = extras.getParcelable(NotificationCompat.EXTRA_LARGE_ICON_BIG);
        if (Build.VERSION.SDK_INT >= 28) {
            assertEquals(icon.getResId(), recoveredIcon.getResId());
        }
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    public void testBigPictureStyle_encodesAndRecoversSetContentDescription() {
        String contentDesc = "content!";
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.notification_bg_low_pressed);
        Notification n = new NotificationCompat.Builder(mContext, "channelId")
                .setSmallIcon(1)
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(bitmap)
                        .setContentDescription(contentDesc)
                        .setBigContentTitle("Big Content Title")
                        .setSummaryText("Summary Text"))
                .build();
        assertEquals(contentDesc,
                n.extras.getCharSequence(Notification.EXTRA_PICTURE_CONTENT_DESCRIPTION));
        Notification recovered = Notification.Builder.recoverBuilder(mContext, n).build();
        assertEquals(contentDesc,
                recovered.extras.getCharSequence(Notification.EXTRA_PICTURE_CONTENT_DESCRIPTION));
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    public void testBigPictureStyle_encodesAndRecoversShowBigPictureWhenCollapsed() {
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.notification_bg_low_pressed);
        Notification n = new NotificationCompat.Builder(mContext, "channelId")
                .setSmallIcon(1)
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(bitmap)
                        .showBigPictureWhenCollapsed(true)
                        .setBigContentTitle("Big Content Title")
                        .setSummaryText("Summary Text"))
                .build();
        assertTrue(n.extras.getBoolean(Notification.EXTRA_SHOW_BIG_PICTURE_WHEN_COLLAPSED));
        Notification recovered = Notification.Builder.recoverBuilder(mContext, n).build();
        assertTrue(recovered.extras.getBoolean(Notification.EXTRA_SHOW_BIG_PICTURE_WHEN_COLLAPSED));
    }

    @SdkSuppress(minSdkVersion = 24)
    @SuppressWarnings("deprecation")
    @Test
    public void testBigPictureStyle_isRecovered() {
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.notification_bg_low_pressed);
        Notification n = new NotificationCompat.Builder(mContext, "channelId")
                .setSmallIcon(1)
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(bitmap)
                        .setBigContentTitle("Big Content Title")
                        .setSummaryText("Summary Text"))
                .build();
        Notification.Builder builder = Notification.Builder.recoverBuilder(mContext, n);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Notification.Style style = builder.getStyle();
            assertNotNull(style);
            assertSame(Notification.BigPictureStyle.class, style.getClass());
        }
        builder.getExtras().remove(Notification.EXTRA_LARGE_ICON_BIG);
        Icon icon = builder.build().extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG);
        assertNotNull(icon);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testBigPictureStyle_recoverStyleWithBitmap() {
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.notification_bg_low_pressed);
        Notification n = new Notification.Builder(mContext)
                .setSmallIcon(1)
                .setStyle(new Notification.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(bitmap)
                        .setBigContentTitle("Big Content Title")
                        .setSummaryText("Summary Text"))
                .build();
        Parcelable firstBuiltIcon = n.extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG);
        assertSame(Icon.class, firstBuiltIcon.getClass());
        assertEquals(Icon.TYPE_BITMAP, ((Icon) firstBuiltIcon).getType());

        Style style = Style.extractStyleFromNotification(n);
        assertNotNull(style);
        assertSame(NotificationCompat.BigPictureStyle.class, style.getClass());
        n = new NotificationCompat.Builder(mContext, "channelId")
                .setSmallIcon(1)
                .setStyle(style)
                .build();
        Parcelable rebuiltIcon = n.extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG);
        assertSame(Icon.class, rebuiltIcon.getClass());
        assertEquals(Icon.TYPE_BITMAP, ((Icon) rebuiltIcon).getType());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testBigPictureStyle_recoverStyleWithResIcon() {
        Notification n = new Notification.Builder(mContext)
                .setSmallIcon(1)
                .setStyle(new Notification.BigPictureStyle()
                        .bigLargeIcon(Icon.createWithResource(mContext,
                                R.drawable.notification_template_icon_bg)))
                .build();
        Icon firstBuiltIcon = n.extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG);
        assertEquals(Icon.TYPE_RESOURCE, firstBuiltIcon.getType());

        Style style = Style.extractStyleFromNotification(n);
        assertNotNull(style);
        assertSame(NotificationCompat.BigPictureStyle.class, style.getClass());
        n = new NotificationCompat.Builder(mContext, "channelId")
                .setSmallIcon(1)
                .setStyle(style)
                .build();
        Icon rebuiltIcon = n.extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG);
        assertEquals(Icon.TYPE_RESOURCE, rebuiltIcon.getType());
    }

    @SdkSuppress(maxSdkVersion = 30)
    @Test
    // maxSdkVersion selected because EXTRA_PICTURE_ICON not set before 31.
    public void testBigPictureStyle_bigPictureWithBitmap_legacy() {

        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.notification_bg_low_pressed);
        Notification n = new NotificationCompat.Builder(mContext, "channelId")
                .setSmallIcon(1)
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap))
                .build();

        Bundle extras = NotificationCompat.getExtras(n);
        assertNotNull(extras);
        // Because the bigPicture was a bitmap, it'll be stored in EXTRA_PICTURE.
        assertTrue(extras.containsKey(NotificationCompat.EXTRA_PICTURE));
        assertNotNull(extras.get(NotificationCompat.EXTRA_PICTURE));
        // EXTRA_PICTURE_ICON was not set before 31.
        assertFalse(extras.containsKey(NotificationCompat.EXTRA_PICTURE_ICON));

        Parcelable firstBuiltIcon = n.extras.getParcelable(NotificationCompat.EXTRA_PICTURE);
        // Checks that the data is unparcled as a bitmap. Because the original data was a
        // bitmap, this ensures maximal backwards compatibility for existing listeners expecting
        // a bitmap from extras.
        assertSame(Bitmap.class, firstBuiltIcon.getClass());

        // Ensures that the icon's EXTRA_PICTURE gets rebuilt from style.
        Style style = Style.extractStyleFromNotification(n);
        assertNotNull(style);
        assertSame(NotificationCompat.BigPictureStyle.class, style.getClass());
        n = new NotificationCompat.Builder(mContext, "channelId")
                .setSmallIcon(1)
                .setStyle(style)
                .build();
        Parcelable rebuiltIcon = n.extras.getParcelable(NotificationCompat.EXTRA_PICTURE);
        assertSame(Bitmap.class, rebuiltIcon.getClass());

    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    // minSdkVersion selected because EXTRA_PICTURE_ICON not set before 31.
    public void testBigPictureStyle_bigPictureWithBitmap() {

        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.notification_bg_low_pressed);
        Notification n = new NotificationCompat.Builder(mContext, "channelId")
                .setSmallIcon(1)
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap))
                .build();

        Bundle extras = NotificationCompat.getExtras(n);
        assertNotNull(extras);
        // Because the bigPicture was a bitmap, it'll be stored in EXTRA_PICTURE.
        assertTrue(extras.containsKey(NotificationCompat.EXTRA_PICTURE));
        assertNotNull(extras.get(NotificationCompat.EXTRA_PICTURE));
        // EXTRA_PICTURE_ICON began being set in 31.
        // However, we avoid storing both the Icon version and the Bitmap
        // version in extras, so the stored value is null.
        assertTrue(extras.containsKey(NotificationCompat.EXTRA_PICTURE_ICON));
        assertNull(extras.get(NotificationCompat.EXTRA_PICTURE_ICON));

        Parcelable firstBuiltIcon = n.extras.getParcelable(NotificationCompat.EXTRA_PICTURE);
        // Checks that the data is unparcled as a bitmap. Because the original data was a
        // bitmap, this ensures maximal backwards compatibility for existing listeners expecting
        // a bitmap from extras.
        assertSame(Bitmap.class, firstBuiltIcon.getClass());

        // Ensures that the icon's EXTRA_PICTURE gets rebuilt from style.
        Style style = Style.extractStyleFromNotification(n);
        assertNotNull(style);
        assertSame(NotificationCompat.BigPictureStyle.class, style.getClass());
        n = new NotificationCompat.Builder(mContext, "channelId")
                .setSmallIcon(1)
                .setStyle(style)
                .build();
        Parcelable rebuiltIcon = n.extras.getParcelable(NotificationCompat.EXTRA_PICTURE);
        assertSame(Bitmap.class, rebuiltIcon.getClass());

    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    // minSdkVersion selected because setting BigPicture from Icon added in 31.
    public void testBigPictureStyle_bigPictureWithIcon() {
        Notification n = new NotificationCompat.Builder(mContext)
                .setSmallIcon(1)
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(Icon.createWithResource(mContext,
                                R.drawable.notification_template_icon_bg)))
                .build();

        Bundle extras = NotificationCompat.getExtras(n);
        assertNotNull(extras);
        // Because the notification was created with a non-bitmap bigPicture, it will be
        // stored in EXTRA_PICTURE_ICON.
        assertTrue(extras.containsKey(NotificationCompat.EXTRA_PICTURE_ICON));
        assertNotNull(extras.get(NotificationCompat.EXTRA_PICTURE_ICON));
        // We avoid storing both the Icon version and the Bitmap version in extras.
        assertTrue(extras.containsKey(NotificationCompat.EXTRA_PICTURE));
        assertNull(extras.get(NotificationCompat.EXTRA_PICTURE));

        Icon firstBuiltIcon = n.extras.getParcelable(NotificationCompat.EXTRA_PICTURE_ICON);
        assertEquals(Icon.TYPE_RESOURCE, firstBuiltIcon.getType());

        // Ensures that the icon's EXTRA_PICTURE gets rebuilt.
        Style style = Style.extractStyleFromNotification(n);
        assertNotNull(style);
        assertSame(NotificationCompat.BigPictureStyle.class, style.getClass());
        n = new NotificationCompat.Builder(mContext, "channelId")
                .setSmallIcon(1)
                .setStyle(style)
                .build();

        Icon rebuiltIcon = n.extras.getParcelable(NotificationCompat.EXTRA_PICTURE_ICON);
        assertEquals(Icon.TYPE_RESOURCE, rebuiltIcon.getType());
    }

    @Test
    public void testMessagingStyle_nullPerson() {
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle("self name");
        messagingStyle.addMessage("text", 200, (Person) null);

        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(messagingStyle)
                .build();

        List<Message> result = NotificationCompat.MessagingStyle
                .extractMessagingStyleFromNotification(notification)
                .getMessages();

        assertEquals(1, result.size());
        assertEquals("text", result.get(0).getText());
        assertEquals(200, result.get(0).getTimestamp());
        assertNull(result.get(0).getPerson());
        assertNull(result.get(0).getSender());
    }

    @Test
    public void testMessagingStyle_message() {
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle("self name");
        Person person = new Person.Builder().setName("test name").setKey("key").build();
        Person person2 = new Person.Builder()
                .setName("test name 2").setKey("key 2").setImportant(true).build();
        messagingStyle.addMessage("text", 200, person);
        messagingStyle.addMessage("text2", 300, person2);

        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(messagingStyle)
                .build();

        List<Message> result = NotificationCompat.MessagingStyle
                .extractMessagingStyleFromNotification(notification)
                .getMessages();

        assertEquals(2, result.size());
        assertEquals("text", result.get(0).getText());
        assertEquals(200, result.get(0).getTimestamp());
        assertEquals("test name", result.get(0).getPerson().getName());
        assertEquals("key", result.get(0).getPerson().getKey());
        assertEquals("text2", result.get(1).getText());
        assertEquals(300, result.get(1).getTimestamp());
        assertEquals("test name 2", result.get(1).getPerson().getName());
        assertEquals("key 2", result.get(1).getPerson().getKey());
        assertTrue(result.get(1).getPerson().isImportant());
    }

    @Test
    public void testMessagingStyle_historicMessage() {
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle("self name");
        Person person = new Person.Builder().setName("test name").setKey("key").build();
        Person person2 = new Person.Builder()
                .setName("test name 2").setKey("key 2").setImportant(true).build();
        messagingStyle.addHistoricMessage(new Message("text", 200, person));
        messagingStyle.addHistoricMessage(new Message("text2", 300, person2));

        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(messagingStyle)
                .build();

        List<Message> result = NotificationCompat.MessagingStyle
                .extractMessagingStyleFromNotification(notification)
                .getHistoricMessages();

        assertEquals(2, result.size());
        assertEquals("text", result.get(0).getText());
        assertEquals(200, result.get(0).getTimestamp());
        assertEquals("test name", result.get(0).getPerson().getName());
        assertEquals("key", result.get(0).getPerson().getKey());
        assertEquals("text2", result.get(1).getText());
        assertEquals(300, result.get(1).getTimestamp());
        assertEquals("test name 2", result.get(1).getPerson().getName());
        assertEquals("key 2", result.get(1).getPerson().getKey());
        assertTrue(result.get(1).getPerson().isImportant());
    }

    @Test
    public void testMessagingStyle_requiresNonEmptyUserName() {
        try {
            new NotificationCompat.MessagingStyle(new Person.Builder().build());
            fail("Expected IllegalArgumentException about a non-empty user name.");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testMessagingStyle_isGroupConversation() {
        mContext.getApplicationInfo().targetSdkVersion = Build.VERSION_CODES.P;
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle(
                        new Person.Builder().setName("self name").build())
                        .setGroupConversation(true)
                        .setConversationTitle("test conversation title");
        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(messagingStyle)
                .build();

        NotificationCompat.MessagingStyle result =
                NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(notification);

        assertTrue(result.isGroupConversation());
    }

    @Test
    public void testMessagingStyle_isGroupConversation_noConversationTitle() {
        mContext.getApplicationInfo().targetSdkVersion = Build.VERSION_CODES.P;
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle(
                        new Person.Builder().setName("self name").build())
                        .setGroupConversation(true)
                        .setConversationTitle(null);
        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(messagingStyle)
                .build();

        NotificationCompat.MessagingStyle result =
                NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(notification);

        assertTrue(result.isGroupConversation());
    }

    @Test
    public void testMessagingStyle_isGroupConversation_withConversationTitle_legacy() {
        // In legacy (version < P), isGroupConversation is controlled by conversationTitle.
        mContext.getApplicationInfo().targetSdkVersion = Build.VERSION_CODES.O;
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle(
                        new Person.Builder().setName("self name").build())
                        .setConversationTitle("test conversation title");
        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(messagingStyle)
                .build();

        NotificationCompat.MessagingStyle result =
                NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(notification);

        assertTrue(result.isGroupConversation());
    }

    @Test
    public void testMessagingStyle_isGroupConversation_withoutConversationTitle_legacy() {
        // In legacy (version < P), isGroupConversation is controlled by conversationTitle.
        mContext.getApplicationInfo().targetSdkVersion = Build.VERSION_CODES.O;
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle(
                        new Person.Builder().setName("self name").build())
                        .setConversationTitle(null);
        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(messagingStyle)
                .build();

        NotificationCompat.MessagingStyle result =
                NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(notification);

        assertFalse(result.isGroupConversation());
    }

    @Test
    public void testMessagingStyle_isGroupConversation_withConversationTitle_legacyWithOverride() {
        // #setGroupConversation should always take precedence over legacy behavior, so a non-null
        // title shouldn't affect #isGroupConversation.
        mContext.getApplicationInfo().targetSdkVersion = Build.VERSION_CODES.O;
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle(
                        new Person.Builder().setName("self name").build())
                        .setGroupConversation(false)
                        .setConversationTitle("test conversation title");
        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(messagingStyle)
                .build();

        NotificationCompat.MessagingStyle result =
                NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(notification);

        assertFalse(result.isGroupConversation());
    }

    @Test
    public void testMessagingStyle_isGroupConversation_withoutTitle_legacyWithOverride() {
        // #setGroupConversation should always take precedence over legacy behavior, so a null
        // title shouldn't affect #isGroupConversation.
        mContext.getApplicationInfo().targetSdkVersion = Build.VERSION_CODES.O;
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle(
                        new Person.Builder().setName("self name").build())
                        .setGroupConversation(true)
                        .setConversationTitle(null);
        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(messagingStyle)
                .build();

        NotificationCompat.MessagingStyle result =
                NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(notification);

        assertTrue(result.isGroupConversation());
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    public void testMessagingStyle_applyNoTitleAndNotGroup() {
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle(
                        new Person.Builder().setName("self name").build())
                        .setGroupConversation(false)
                        .addMessage(
                                new Message(
                                        "body",
                                        1,
                                        new Person.Builder().setName("example name").build()))
                        .addMessage(new Message("body 2", 2, (Person) null));

        Notification resultNotification = new NotificationCompat.Builder(mContext, "test id")
                .setStyle(messagingStyle)
                .build();
        NotificationCompat.MessagingStyle resultCompatMessagingStyle =
                NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(resultNotification);

        // SDK >= 28 applies no title when none is provided to MessagingStyle.
        assertNull(resultCompatMessagingStyle.getConversationTitle());
        assertFalse(resultCompatMessagingStyle.isGroupConversation());
    }

    @SdkSuppress(minSdkVersion = 24, maxSdkVersion = 27)
    @Test
    public void testMessagingStyle_applyNoTitleAndNotGroup_legacy() {
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle(
                        new Person.Builder().setName("self name").build())
                        .setGroupConversation(false)
                        .addMessage(
                                new Message(
                                        "body",
                                        1,
                                        new Person.Builder().setName("example name").build()))
                        .addMessage(new Message("body 2", 2, (Person) null));

        Notification resultNotification = new NotificationCompat.Builder(mContext, "test id")
                .setStyle(messagingStyle)
                .build();
        NotificationCompat.MessagingStyle resultCompatMessagingStyle =
                NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(resultNotification);

        // SDK [24, 27] applies first incoming message sender name as Notification content title.
        assertEquals("example name", NotificationCompat.getContentTitle(resultNotification));
        assertNull(resultCompatMessagingStyle.getConversationTitle());
        assertFalse(resultCompatMessagingStyle.isGroupConversation());
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    public void testMessagingStyle_applyConversationTitleAndNotGroup() {
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle(
                        new Person.Builder().setName("self name").build())
                        .setGroupConversation(false)
                        .setConversationTitle("test title");

        Notification resultNotification = new NotificationCompat.Builder(mContext, "test id")
                .setStyle(messagingStyle)
                .build();
        NotificationCompat.MessagingStyle resultMessagingStyle =
                NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(resultNotification);

        // SDK >= 28 applies provided title to MessagingStyle.
        assertEquals("test title", resultMessagingStyle.getConversationTitle());
        assertFalse(resultMessagingStyle.isGroupConversation());
    }

    @SdkSuppress(maxSdkVersion = 27)
    @Test
    public void testMessagingStyle_applyConversationTitleAndNotGroup_legacy() {
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle(
                        new Person.Builder().setName("self name").build())
                        .setGroupConversation(false)
                        .setConversationTitle("test title");

        Notification resultNotification = new NotificationCompat.Builder(mContext, "test id")
                .setStyle(messagingStyle)
                .build();
        NotificationCompat.MessagingStyle resultMessagingStyle =
                NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(resultNotification);

        // SDK <= 27 applies MessagingStyle title as Notification content title.
        assertEquals("test title", resultMessagingStyle.getConversationTitle());
        assertFalse(resultMessagingStyle.isGroupConversation());
    }

    @SdkSuppress(minSdkVersion = 28)
    @SuppressWarnings("deprecation")
    @Test
    public void testMessagingStyle_apply_writesMessagePerson() {
        Notification msNotification = newMsNotification(true, true);

        Bundle[] messagesBundle =
                (Bundle[]) msNotification.extras.getParcelableArray(Notification.EXTRA_MESSAGES);
        assertEquals(2, messagesBundle.length);
        assertTrue(messagesBundle[0].containsKey(Message.KEY_NOTIFICATION_PERSON));
    }

    @SdkSuppress(minSdkVersion = 24, maxSdkVersion = 27)
    @SuppressWarnings("deprecation")
    @Test
    public void testMessagingStyle_apply_writesMessagePerson_legacy() {
        Notification msNotification = newMsNotification(true, true);

        Bundle[] messagesBundle =
                (Bundle[]) msNotification.extras.getParcelableArray(Notification.EXTRA_MESSAGES);
        assertEquals(2, messagesBundle.length);
        assertTrue(messagesBundle[0].containsKey(Message.KEY_PERSON));
    }

    @Test
    public void testMessagingStyle_restoreFromCompatExtras() {
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle(
                        new Person.Builder().setName("test name").build())
                        .setGroupConversation(true);
        Bundle bundle = new Bundle();
        messagingStyle.addCompatExtras(bundle);

        NotificationCompat.MessagingStyle resultMessagingStyle =
                new NotificationCompat.MessagingStyle(new Person.Builder().setName("temp").build());
        resultMessagingStyle.restoreFromCompatExtras(bundle);

        assertTrue(resultMessagingStyle.isGroupConversation());
        assertEquals("test name", resultMessagingStyle.getUser().getName());
    }

    @Test
    public void testMessagingStyleMessage_bundle_legacySender() {
        Bundle legacyBundle = new Bundle();
        legacyBundle.putCharSequence(Message.KEY_TEXT, "message");
        legacyBundle.putLong(Message.KEY_TIMESTAMP, 100);
        legacyBundle.putCharSequence(Message.KEY_SENDER, "sender");

        Message result = Message.getMessageFromBundle(legacyBundle);
        assertEquals("sender", result.getPerson().getName());
    }

    @Test
    public void testCallStyle_className() {
        NotificationCompat.CallStyle callStyle = new NotificationCompat.CallStyle();
        assertEquals("androidx.core.app.NotificationCompat$CallStyle", callStyle.getClassName());
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    public void testCallStyle_callStyleIncomingSetsIntents() {
        PendingIntent answerIntent = createIntent("answer");
        PendingIntent declineIntent = createIntent("decline");
        Person person = new Person.Builder().setName("test name").build();
        NotificationCompat.CallStyle callStyle = NotificationCompat.CallStyle.forIncomingCall(
                person, declineIntent, answerIntent);

        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(callStyle)
                .build();

        // Test extras values. This is equivalent to creating a new NotificationCompat.Builder,
        // and checking the values in it (because those are created via restoreFromCompatExtras).
        Bundle extras = notification.extras;
        assertEquals(NotificationCompat.CallStyle.CALL_TYPE_INCOMING,
                extras.getInt(NotificationCompat.EXTRA_CALL_TYPE));
        assertEquals("test name", ((android.app.Person) extras.getParcelable(
                NotificationCompat.EXTRA_CALL_PERSON)).getName());

        assertTrue(extras.containsKey(NotificationCompat.EXTRA_ANSWER_INTENT));
        assertEquals(answerIntent,
                (PendingIntent) extras.getParcelable(NotificationCompat.EXTRA_ANSWER_INTENT));
        assertTrue(extras.containsKey(NotificationCompat.EXTRA_DECLINE_INTENT));
        assertEquals(declineIntent,
                ((PendingIntent) extras.getParcelable(NotificationCompat.EXTRA_DECLINE_INTENT)));

        // Create a new NotificationCompat Builder object based on the notification.
        // This allows us to inspect various fields, including actions.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, notification);
        assertEquals(2, builder.mActions.size());
        assertEquals(mContext.getString(R.string.call_notification_decline_action),
                builder.mActions.get(0).getTitle().toString());
        assertEquals(mContext.getString(R.string.call_notification_answer_action),
                builder.mActions.get(1).getTitle().toString());
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    public void testCallStyle_callStyleOngoingSetsIntents() {
        PendingIntent hangupIntent = createIntent("hangup");
        Person person = new Person.Builder().setName("test name").build();
        NotificationCompat.CallStyle callStyle = NotificationCompat.CallStyle.forOngoingCall(
                person, hangupIntent);

        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(callStyle)
                .build();

        // Test extras values. This is equivalent to creating a new NotificationCompat.Builder,
        // and checking the values in it (because those are created via restoreFromCompatExtras).
        Bundle extras = notification.extras;
        assertEquals(NotificationCompat.CallStyle.CALL_TYPE_ONGOING,
                extras.getInt(NotificationCompat.EXTRA_CALL_TYPE));
        assertEquals("test name", ((android.app.Person) extras.getParcelable(
                NotificationCompat.EXTRA_CALL_PERSON)).getName());

        assertTrue(extras.containsKey(NotificationCompat.EXTRA_HANG_UP_INTENT));
        assertEquals(hangupIntent,
                ((PendingIntent) extras.getParcelable(NotificationCompat.EXTRA_HANG_UP_INTENT)));

        // Create a new NotificationCompat Builder object based on the notification.
        // This allows us to inspect various fields, including actions.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, notification);
        assertEquals(1, builder.mActions.size());
        assertEquals(mContext.getString(R.string.call_notification_hang_up_action),
                builder.mActions.get(0).getTitle().toString());
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    public void testCallStyle_callStyleScreeningSetsIntents() {
        PendingIntent answerIntent = createIntent("answer");
        PendingIntent hangupIntent = createIntent("hangup");
        Person person = new Person.Builder().setName("test name").build();
        NotificationCompat.CallStyle callStyle = NotificationCompat.CallStyle.forScreeningCall(
                person, hangupIntent, answerIntent);

        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(callStyle)
                .build();

        // Test extras values. This is equivalent to creating a new NotificationCompat.Builder,
        // and checking the values in it (because those are created via restoreFromCompatExtras).
        Bundle extras = notification.extras;
        assertEquals(NotificationCompat.CallStyle.CALL_TYPE_SCREENING,
                extras.getInt(NotificationCompat.EXTRA_CALL_TYPE));
        assertEquals("test name", ((android.app.Person) extras.getParcelable(
                NotificationCompat.EXTRA_CALL_PERSON)).getName());

        assertTrue(extras.containsKey(NotificationCompat.EXTRA_ANSWER_INTENT));
        assertEquals(answerIntent,
                ((PendingIntent) extras.getParcelable(NotificationCompat.EXTRA_ANSWER_INTENT)));
        assertTrue(extras.containsKey(NotificationCompat.EXTRA_HANG_UP_INTENT));
        assertEquals(hangupIntent,
                ((PendingIntent) extras.getParcelable(NotificationCompat.EXTRA_HANG_UP_INTENT)));

        // Create a new NotificationCompat Builder object based on the notification.
        // This allows us to inspect various fields, including actions.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, notification);
        assertEquals(2, builder.mActions.size());
        assertEquals(mContext.getString(R.string.call_notification_hang_up_action),
                builder.mActions.get(0).getTitle().toString());
        assertEquals(mContext.getString(R.string.call_notification_answer_action),
                builder.mActions.get(1).getTitle().toString());
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    public void testCallStyle_callStyleSetAdditionalFields() {
        PendingIntent answerIntent = createIntent("answer");
        PendingIntent declineIntent = createIntent("decline");
        Person person = new Person.Builder().setName("test name").build();
        NotificationCompat.CallStyle callStyle = NotificationCompat.CallStyle.forIncomingCall(
                person, declineIntent, answerIntent);

        IconCompat icon = IconCompat.createWithResource(mContext, R.drawable.ic_call_decline);
        assertNotNull(icon);

        callStyle.setAnswerButtonColorHint(Color.BLUE);
        callStyle.setDeclineButtonColorHint(Color.MAGENTA);
        callStyle.setVerificationText("Verified");
        callStyle.setVerificationIcon(icon.toIcon(mContext));
        callStyle.setIsVideo(true);

        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(callStyle)
                .build();

        // Test extras values. This is equivalent to creating a new NotificationCompat.Builder,
        // and checking the values in it (because those are created via restoreFromCompatExtras).
        Bundle extras = notification.extras;

        assertEquals(Color.BLUE, extras.getInt(NotificationCompat.EXTRA_ANSWER_COLOR));
        assertEquals(Color.MAGENTA, extras.getInt(NotificationCompat.EXTRA_DECLINE_COLOR));
        assertEquals("Verified",
                extras.getCharSequence(NotificationCompat.EXTRA_VERIFICATION_TEXT));
        assertTrue(extras.containsKey(NotificationCompat.EXTRA_VERIFICATION_ICON));
        assertEquals(((Icon) extras.getParcelable(
                        NotificationCompat.EXTRA_VERIFICATION_ICON)).getResId(),
                icon.toIcon(mContext).getResId());
        assertTrue(extras.getBoolean(NotificationCompat.EXTRA_CALL_IS_VIDEO));
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    public void testCallStyle_callStyleNullAdditionalFields() {
        PendingIntent answerIntent = createIntent("answer");
        PendingIntent declineIntent = createIntent("decline");
        Person person = new Person.Builder().setName("test name").build();
        NotificationCompat.CallStyle callStyle = NotificationCompat.CallStyle.forIncomingCall(
                person, declineIntent, answerIntent);

        callStyle.setVerificationText(null);
        callStyle.setVerificationIcon((Icon) null);
        callStyle.setIsVideo(false);

        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(callStyle)
                .build();

        // Test extras values. This is equivalent to creating a new NotificationCompat.Builder,
        // and checking the values in it (because those are created via restoreFromCompatExtras).
        Bundle extras = notification.extras;

        assertEquals(0, extras.getInt(NotificationCompat.EXTRA_ANSWER_COLOR));
        assertEquals(0, extras.getInt(NotificationCompat.EXTRA_DECLINE_COLOR));
        assertNull(extras.getCharSequence(NotificationCompat.EXTRA_VERIFICATION_TEXT));
        assertNull(extras.getParcelable(NotificationCompat.EXTRA_VERIFICATION_ICON));
        assertFalse(extras.getBoolean(NotificationCompat.EXTRA_CALL_IS_VIDEO));
    }

    @Test
    public void testCallStyle_preservesOneCustomAction() {
        PendingIntent hangupIntent = createIntent("hangup");
        Person person = new Person.Builder().setName("test name").build();
        NotificationCompat.CallStyle callStyle = NotificationCompat.CallStyle.forOngoingCall(
                person, hangupIntent);
        NotificationCompat.Action customAction = new NotificationCompat.Action.Builder(
                IconCompat.createWithResource(mContext, R.drawable.notification_bg),
                "Custom!", null).build();

        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .addAction(customAction)
                .setStyle(callStyle)
                .build();

        // Actions as Hang up + Custom (fits on all Android versions).
        assertThat(notification.actions).hasLength(2);

        // But order is different per Android version. CallStyle was introduced in SDK 31 and
        // placed custom actions first. A QPR of SDK 33 switched to system actions first. Pre-31
        // there is no native CallStyle, so the Compat version uses the newer ordering there too.
        List<String> actionTitles = Lists.transform(Arrays.asList(notification.actions),
                a -> a.title.toString());
        if (Build.VERSION.SDK_INT < 31 || Build.VERSION.SDK_INT >= 34) {
            assertThat(actionTitles).containsExactly(
                            mContext.getString(R.string.call_notification_hang_up_action),
                            customAction.title.toString())
                    .inOrder();
        } else if (Build.VERSION.SDK_INT >= 31 && Build.VERSION.SDK_INT <= 32) {
            assertThat(actionTitles).containsExactly(
                            customAction.title.toString(),
                            mContext.getString(R.string.call_notification_hang_up_action))
                    .inOrder();
        } else if (Build.VERSION.SDK_INT == 33) {
            // Could be either, so check presence but not ordering.
            assertThat(actionTitles).containsExactly(
                            mContext.getString(R.string.call_notification_hang_up_action),
                            customAction.title.toString());
        } else {
            throw new AssertionError("All SDK_INT values are covered!");
        }
    }

    @Test
    public void testCallStyle_preservesTwoCustomActions() {
        PendingIntent hangupIntent = createIntent("hangup");
        Person person = new Person.Builder().setName("test name").build();
        NotificationCompat.CallStyle callStyle = NotificationCompat.CallStyle.forOngoingCall(
                person, hangupIntent);
        NotificationCompat.Action customAction1 = new NotificationCompat.Action.Builder(
                IconCompat.createWithResource(mContext, R.drawable.notification_bg),
                "Custom 1!", null).build();
        NotificationCompat.Action customAction2 = new NotificationCompat.Action.Builder(
                IconCompat.createWithResource(mContext, R.drawable.notification_bg),
                "Custom 2!", null).build();

        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .addAction(customAction1)
                .addAction(customAction2)
                .setStyle(callStyle)
                .build();

        // Actions as Hang up + Custom (fits on all Android versions).
        assertThat(notification.actions).hasLength(3);

        // But order is different per Android version. CallStyle was introduced in SDK 31 and
        // placed custom actions first. A QPR of SDK 33 switched to system actions first. Pre-31
        // there is no native CallStyle, so the Compat version uses the newer ordering there too.
        List<String> actionTitles = Lists.transform(Arrays.asList(notification.actions),
                a -> a.title.toString());
        if (Build.VERSION.SDK_INT < 31 || Build.VERSION.SDK_INT >= 34) {
            assertThat(actionTitles).containsExactly(
                            mContext.getString(R.string.call_notification_hang_up_action),
                            customAction1.title.toString(),
                            customAction2.title.toString())
                    .inOrder();
        } else if (Build.VERSION.SDK_INT >= 31 && Build.VERSION.SDK_INT <= 32) {
            assertThat(actionTitles).containsExactly(
                            customAction1.title.toString(),
                            customAction2.title.toString(),
                            mContext.getString(R.string.call_notification_hang_up_action))
                    .inOrder();
        } else if (Build.VERSION.SDK_INT == 33) {
            // Could be either, so check presence but not ordering.
            assertThat(actionTitles).containsExactly(
                            mContext.getString(R.string.call_notification_hang_up_action),
                            customAction1.title.toString(),
                            customAction2.title.toString());
        } else {
            throw new AssertionError("All SDK_INT values are covered!");
        }
    }

    @Test
    public void testCallStyle_getActionsListWithSystemAndContextualActionsForIncoming() {
        PendingIntent positiveIntent = createIntent("answerIntent");
        PendingIntent negativeIntent = createIntent("declineIntent");
        Person person = new Person.Builder().setName("test name").build();
        NotificationCompat.CallStyle callStyle = NotificationCompat.CallStyle.forIncomingCall(
                person, negativeIntent, positiveIntent);

        // Create three "system" actions. The second action is marked as Contextual.
        NotificationCompat.Action fooAction = new NotificationCompat.Action(0, "foo",
                createIntent("foo"));
        NotificationCompat.Action barAction = new NotificationCompat.Action(0, "bar",
                createIntent("bar"), null, null, null, false, 0, false, /*isContextual=*/true,
                false);
        NotificationCompat.Action bazAction = new NotificationCompat.Action(0, "baz",
                createIntent("baz"));

        // The style must be attached to a builder for the context to be initialized.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .addAction(fooAction)
                .addAction(barAction)
                .addAction(bazAction)
                .setStyle(callStyle);

        ArrayList<NotificationCompat.Action> resultActions =
                callStyle.getActionsListWithSystemActions();
        // Note that contextual actions do not count toward the MAX_ACTION_BUTTON limit.
        // Thus the resulting number of actions can be more than MAX_ACTION_BUTTON.
        assertEquals(5, resultActions.size());

        // The negative action always gets placed first.
        assertEquals(mContext.getString(R.string.call_notification_decline_action),
                resultActions.get(0).getTitle().toString());
        assertEquals(negativeIntent, resultActions.get(0).getActionIntent());
        resultActions.get(0).getIconCompat();

        assertEquals("foo", resultActions.get(1).getTitle().toString());

        assertEquals(mContext.getString(R.string.call_notification_answer_action),
                resultActions.get(2).getTitle().toString());
        assertEquals(positiveIntent, resultActions.get(2).getActionIntent());

        // The "bar" action is Contextual, so it is always included, beyond the limit of 3.
        assertEquals("bar", resultActions.get(3).getTitle().toString());
        // even non-contextual actions are included, beyond the visible limit of 3.
        assertEquals("baz", resultActions.get(4).getTitle().toString());
    }

    @Test
    public void testCallStyle_getActionsListWithSystemAndContextualActionsForOngoing() {
        PendingIntent negativeIntent = createIntent("hang up");
        Person person = new Person.Builder().setName("test name").build();
        NotificationCompat.CallStyle callStyle = NotificationCompat.CallStyle.forOngoingCall(
                person, negativeIntent);

        // Create three "system" actions. The second action is marked as Contextual.
        NotificationCompat.Action fooAction = new NotificationCompat.Action(0, "foo",
                createIntent("foo"));
        NotificationCompat.Action barAction = new NotificationCompat.Action(0, "bar",
                createIntent("bar"), null, null, null, false, 0, false, /*isContextual=*/true,
                false);
        NotificationCompat.Action bazAction = new NotificationCompat.Action(0, "baz",
                createIntent("baz"));
        NotificationCompat.Action bbqAction = new NotificationCompat.Action(0, "bbq",
                createIntent("bbq"));

        // The style must be attached to a builder for the context to be initialized.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .addAction(fooAction)
                .addAction(barAction)
                .addAction(bazAction)
                .addAction(bbqAction)
                .setStyle(callStyle);

        ArrayList<NotificationCompat.Action> resultActions =
                callStyle.getActionsListWithSystemActions();
        assertEquals(5, resultActions.size());

        // The negative Intent is always placed first.
        assertEquals(mContext.getString(R.string.call_notification_hang_up_action),
                resultActions.get(0).getTitle().toString());
        assertEquals(negativeIntent, resultActions.get(0).getActionIntent());
        assertEquals("foo", resultActions.get(1).getTitle().toString());
        assertEquals("bar", resultActions.get(2).getTitle().toString());
        // even non-contextual actions are included, beyond the visible limit of 3.
        assertEquals("baz", resultActions.get(3).getTitle().toString());
        assertEquals("bbq", resultActions.get(4).getTitle().toString());
    }

    @Test
    public void testCallStyle_getActionsListWithSystemAndContextualActionsForScreening() {
        PendingIntent positiveIntent = createIntent("answer");
        PendingIntent negativeIntent = createIntent("hangup");
        Person person = new Person.Builder().setName("test name").build();
        NotificationCompat.CallStyle callStyle = NotificationCompat.CallStyle.forScreeningCall(
                person, negativeIntent, positiveIntent);

        // Create three "system" actions. The second action is marked as Contextual.
        NotificationCompat.Action fooAction = new NotificationCompat.Action(0, "foo",
                createIntent("foo"));
        NotificationCompat.Action barAction = new NotificationCompat.Action(0, "bar",
                createIntent("bar"), null, null, null, false, 0, false, /*isContextual=*/true,
                false);
        NotificationCompat.Action bazAction = new NotificationCompat.Action(0, "baz",
                createIntent("baz"));

        // The style must be attached to a builder for the context to be initialized.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .addAction(fooAction)
                .addAction(barAction)
                .addAction(bazAction)
                .setStyle(callStyle);

        ArrayList<NotificationCompat.Action> resultActions =
                callStyle.getActionsListWithSystemActions();
        assertEquals(5, resultActions.size());

        // The negative intent is always placed first.
        assertEquals(mContext.getString(R.string.call_notification_hang_up_action),
                resultActions.get(0).getTitle().toString());
        assertEquals(negativeIntent, resultActions.get(0).getActionIntent());

        assertEquals("foo", resultActions.get(1).getTitle().toString());

        assertEquals(mContext.getString(R.string.call_notification_answer_action),
                resultActions.get(2).getTitle().toString());
        assertEquals(positiveIntent, resultActions.get(2).getActionIntent());

        // contextual actions are always included, beyond the visible limit of 3.
        assertEquals("bar", resultActions.get(3).getTitle().toString());
        // even non-contextual actions are included, beyond the visible limit of 3.
        assertEquals("baz", resultActions.get(4).getTitle().toString());
    }

    @Test
    public void testCallStyle_getActionsListForIncomingVideo() {
        PendingIntent positiveIntent = createIntent("answerIntent");
        PendingIntent negativeIntent = createIntent("declineIntent");
        Person person = new Person.Builder().setName("test name").build();
        NotificationCompat.CallStyle callStyle = NotificationCompat.CallStyle.forIncomingCall(
                person, negativeIntent, positiveIntent).setIsVideo(true);

        // The style must be attached to a builder for the context to be initialized.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(callStyle);

        ArrayList<NotificationCompat.Action> resultActions =
                callStyle.getActionsListWithSystemActions();
        assertEquals(2, resultActions.size());

        assertEquals(mContext.getString(R.string.call_notification_decline_action),
                resultActions.get(0).getTitle().toString());
        assertEquals(negativeIntent, resultActions.get(0).getActionIntent());
        resultActions.get(0).getIconCompat();

        assertEquals(mContext.getString(R.string.call_notification_answer_video_action),
                resultActions.get(1).getTitle().toString());
        assertEquals(positiveIntent, resultActions.get(1).getActionIntent());
        assertEquals(R.drawable.ic_call_answer_video,
                resultActions.get(1).getIconCompat().getResId());
    }

    @SdkSuppress(maxSdkVersion = 30)
    @Test
    public void testCallStyle_callStyleLegacyNotificationExtraText() {
        PendingIntent answerIntent = createIntent("answer");
        PendingIntent declineIntent = createIntent("decline");
        Person callerPerson = new Person.Builder()
                .setName("test name")
                .build();
        NotificationCompat.CallStyle callStyle = NotificationCompat.CallStyle.forIncomingCall(
                callerPerson, declineIntent, answerIntent);

        Bundle inputBundle = new Bundle();
        inputBundle.putCharSequence(NotificationCompat.EXTRA_TEXT, "Supplemental Text");

        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(callStyle)
                .setExtras(inputBundle)
                .build();

        // Test extras values. This is equivalent to creating a new NotificationCompat.Builder,
        // and checking the values in it (because those are created via restoreFromCompatExtras).
        Bundle extras = notification.extras;

        // Checks that the notification text is set to the EXTRA_TEXT value. 11 >=
        assertEquals("Supplemental Text",
                extras.getCharSequence(NotificationCompat.EXTRA_TEXT));
    }

    @SdkSuppress(maxSdkVersion = 30)
    @Test
    public void testCallStyle_callStyleLegacyNotificationVerificationInfo() {
        PendingIntent answerIntent = createIntent("answer");
        PendingIntent declineIntent = createIntent("decline");
        Person callerPerson = new Person.Builder().setName("test name").build();
        NotificationCompat.CallStyle callStyle = NotificationCompat.CallStyle.forIncomingCall(
                callerPerson, declineIntent, answerIntent);

        // Use a bitmap to test that setVerificationIcon can accept a bitmap.
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.notification_bg_low_pressed);
        callStyle.setVerificationText("Verified");
        callStyle.setVerificationIcon(bitmap);

        Bundle inputBundle = new Bundle();
        NotificationCompat.Builder originalBuilder =
                new NotificationCompat.Builder(mContext, "test id")
                        .setSmallIcon(1)
                        .setContentTitle("test title")
                        .setExtras(inputBundle)
                        .setStyle(callStyle);

        Notification notification = originalBuilder.build();
        // Test extras values. This is equivalent to creating a new NotificationCompat.Builder,
        // and checking the values in it (because those are created via restoreFromCompatExtras).
        // Extras are only available in API 19 and above, hence the minSdkVersion of this test.
        Bundle extras = notification.extras;

        assertEquals("Verified",
                extras.getCharSequence(NotificationCompat.EXTRA_VERIFICATION_TEXT));
        // Unfortunately there is no easy way to directly compare the bitmaps of two icons,
        // so we settle for checking that it's the right size via the toString method.
        assertTrue(extras.containsKey(NotificationCompat.EXTRA_VERIFICATION_ICON));
        assertEquals(extras.getParcelable(NotificationCompat.EXTRA_VERIFICATION_ICON).toString(),
                IconCompat.createWithBitmap(bitmap).toIcon(mContext).toString());
    }

    @SdkSuppress(maxSdkVersion = 30)
    @Test
    public void testCallStyle_callStyleLegacyNotificationPersonInfo() {
        PendingIntent answerIntent = createIntent("answer");
        PendingIntent declineIntent = createIntent("decline");
        Person callerPerson = new Person.Builder().setName("test name").build();
        NotificationCompat.CallStyle callStyle = NotificationCompat.CallStyle.forIncomingCall(
                callerPerson, declineIntent, answerIntent);

        NotificationCompat.Builder originalBuilder =
                new NotificationCompat.Builder(mContext, "test id")
                        .setSmallIcon(1)
                        .setContentTitle("test title")
                        .setStyle(callStyle);

        Notification notification = originalBuilder.build();
        // Test extras values. This is equivalent to creating a new NotificationCompat.Builder,
        // and checking the values in it (because those are created via restoreFromCompatExtras).
        // Extras are only available in API 19 and above, hence the minSdkVersion of this test.
        Bundle extras = notification.extras;

        if (Build.VERSION.SDK_INT >= 28) {
            assertEquals(callerPerson.getName(),
                    Person.fromAndroidPerson((android.app.Person) extras.getParcelable(
                            NotificationCompat.EXTRA_CALL_PERSON)).getName());
        } else {
            assertEquals(callerPerson.getName(),
                    Person.fromBundle(extras.getBundle(
                            NotificationCompat.EXTRA_CALL_PERSON_COMPAT)).getName());
        }
    }

    @SdkSuppress(maxSdkVersion = 30)
    @Test
    public void testCallStyle_callStyleLegacyNotificationIncoming() {
        // Create a placeholder icon for use with the person.
        IconCompat personIcon = IconCompat.createWithResource(mContext,
                R.drawable.ic_call_answer_video_low);

        PendingIntent answerIntent = createIntent("answer");
        PendingIntent declineIntent = createIntent("decline");
        Person callerPerson = new Person.Builder()
                .setName("test name")
                .setIcon(personIcon)
                .setUri("personUri")
                .build();
        NotificationCompat.CallStyle callStyle = NotificationCompat.CallStyle.forIncomingCall(
                callerPerson, declineIntent, answerIntent);

        callStyle.setAnswerButtonColorHint(Color.BLUE);
        callStyle.setDeclineButtonColorHint(Color.MAGENTA);
        callStyle.setIsVideo(false);

        NotificationCompat.Builder originalBuilder =
                new NotificationCompat.Builder(mContext, "test id")
                        .setSmallIcon(1)
                        .setContentTitle("test title")
                        .setStyle(callStyle);

        Notification notification = originalBuilder.build();

        // Test extras values. This is equivalent to creating a new NotificationCompat.Builder,
        // and checking the values in it (because those are created via restoreFromCompatExtras).
        Bundle extras = notification.extras;

        // Checks that the notification title is set to the caller name. 11 >=
        assertEquals("test name", extras.getCharSequence(NotificationCompat.EXTRA_TITLE));
        // Checks that the notification text is set to the default text (since EXTRA_TEXT isn't
        // set).
        assertEquals(
                mContext.getResources().getString(R.string.call_notification_incoming_text),
                extras.getCharSequence(NotificationCompat.EXTRA_TEXT));

        // Create a new NotificationCompat Builder object based on the notification.
        // This allows us to inspect various fields, including actions.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext,
                notification);
        // For versions above 11, the "Person" name from the style is applied to the title.
        assertEquals("test name", builder.mContentTitle);

        assertNotNull(notification.actions);
        assertEquals(2, notification.actions.length);

        // Check that the decline action has the suggested color and title.
        assertEquals(mContext.getString(R.string.call_notification_decline_action),
                notification.actions[0].title.toString());
        assertEquals(Color.MAGENTA,
                ((SpannableStringBuilder) notification.actions[0].title).getSpans(0,
                        notification.actions[0].title.length(),
                        ForegroundColorSpan.class)[0].getForegroundColor());

        // Check that the answer action has the suggested color and title.
        assertEquals(mContext.getString(R.string.call_notification_answer_action),
                notification.actions[1].title.toString());
        assertEquals(Color.BLUE,
                ((SpannableStringBuilder) notification.actions[1].title).getSpans(0,
                        notification.actions[1].title.length(),
                        ForegroundColorSpan.class)[0].getForegroundColor());

        assertEquals(personIcon.toString(), notification.getLargeIcon().toString());
    }

    @SdkSuppress(maxSdkVersion = 30)
    @Test
    public void testCallStyle_callStyleLegacyNotificationIncomingVideo() {
        PendingIntent answerIntent = createIntent("answer");
        PendingIntent declineIntent = createIntent("decline");
        Person callerPerson = new Person.Builder().setName("test name").build();
        NotificationCompat.CallStyle callStyle = NotificationCompat.CallStyle.forIncomingCall(
                callerPerson, declineIntent, answerIntent);

        callStyle.setAnswerButtonColorHint(Color.BLUE);
        callStyle.setDeclineButtonColorHint(Color.MAGENTA);
        callStyle.setIsVideo(true);

        NotificationCompat.Builder originalBuilder =
                new NotificationCompat.Builder(mContext, "test id")
                        .setSmallIcon(1)
                        .setContentTitle("test title")
                        .setStyle(callStyle);
        Notification notification = originalBuilder.build();

        // Checks in this section check values in the Notification's extras, which were unavailable
        // prior to API 19.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext,
                notification);

        Bundle extras = builder.getExtras();
        assertTrue(extras.getBoolean(NotificationCompat.EXTRA_CALL_IS_VIDEO));

        assertNotNull(notification.actions);
        assertEquals(2, notification.actions.length);

        // Check that the answer action has the suggested color and title.
        assertEquals(mContext.getString(R.string.call_notification_answer_video_action),
                notification.actions[1].title.toString());
        assertEquals(Color.BLUE,
                ((SpannableStringBuilder) notification.actions[1].title).getSpans(0,
                        notification.actions[1].title.length(),
                        ForegroundColorSpan.class)[0].getForegroundColor());
    }

    @SdkSuppress(maxSdkVersion = 30)
    @Test
    public void testCallStyle_callStyleLegacyNotificationOngoing() {
        // Create a placeholder icon for use with the person.
        IconCompat personIcon = IconCompat.createWithResource(mContext,
                R.drawable.ic_call_answer_video_low);

        PendingIntent hangupIntent = createIntent("hangup");
        Person callerPerson = new Person.Builder()
                .setName("test name")
                .setIcon(personIcon)
                .setUri("personUri")
                .build();
        NotificationCompat.CallStyle callStyle = NotificationCompat.CallStyle.forOngoingCall(
                callerPerson, hangupIntent);

        callStyle.setAnswerButtonColorHint(Color.BLUE);
        callStyle.setDeclineButtonColorHint(Color.MAGENTA);

        NotificationCompat.Builder originalBuilder =
                new NotificationCompat.Builder(mContext, "test id")
                        .setSmallIcon(1)
                        .setContentTitle("test title")
                        .setStyle(callStyle);
        Notification notification = originalBuilder.build();

        // Checks in this section check values in the Notification's extras, which were unavailable
        // prior to API 19.
        // Test extras values. This is equivalent to creating a new NotificationCompat.Builder,
        // and checking the values in it (as those are created via restoreFromCompatExtras).
        Bundle extras = notification.extras;

        // Checks that the notification title is set to the caller name. 11 >=
        assertEquals("test name", extras.getCharSequence(NotificationCompat.EXTRA_TITLE));

        // Checks that the notification text is set to the default text (since EXTRA_TEXT isn't
        // set). 11 >=
        assertEquals(mContext.getResources().getString(R.string.call_notification_ongoing_text),
                extras.getCharSequence(NotificationCompat.EXTRA_TEXT));

        // Create a new NotificationCompat Builder object based on the notification.
        // This allows us to inspect various fields, including actions.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext,
                notification);

        // For versions above 11, the "Person" name from the style is applied to the title.
        assertEquals("test name", builder.mContentTitle);

        assertNotNull(notification.actions);
        assertEquals(1, notification.actions.length);

        // Check that the hangup action has the suggested color and title.
        assertEquals(mContext.getString(R.string.call_notification_hang_up_action),
                notification.actions[0].title.toString());
        assertEquals(Color.MAGENTA,
                ((SpannableStringBuilder) notification.actions[0].title).getSpans(0,
                        notification.actions[0].title.length(),
                        ForegroundColorSpan.class)[0].getForegroundColor());

        assertEquals(personIcon.toString(), notification.getLargeIcon().toString());
    }

    @SdkSuppress(maxSdkVersion = 30)
    @Test
    public void testCallStyle_callStyleLegacyNotificationScreening() {
        // Create a placeholder icon for use with the person.
        IconCompat personIcon = IconCompat.createWithResource(mContext,
                R.drawable.ic_call_answer_video_low);

        PendingIntent answerIntent = createIntent("answer");
        PendingIntent hangupIntent = createIntent("hangup");
        Person callerPerson = new Person.Builder()
                .setName("test name")
                .setIcon(personIcon)
                .setUri("personUri")
                .build();
        NotificationCompat.CallStyle callStyle = NotificationCompat.CallStyle.forScreeningCall(
                callerPerson, hangupIntent, answerIntent);

        NotificationCompat.Builder originalBuilder =
                new NotificationCompat.Builder(mContext, "test id")
                        .setSmallIcon(1)
                        .setContentTitle("test title")
                        .setStyle(callStyle);
        Notification notification = originalBuilder.build();

        // Checks in this section check values in the Notification's extras, which were unavailable
        // prior to API 19.
        // Test extras values. This is equivalent to creating a new NotificationCompat.Builder,
        // and checking the values in it (as those are created via restoreFromCompatExtras).
        Bundle extras = notification.extras;

        // Checks that the notification title is set to the caller name. 11 >=
        assertEquals("test name", extras.getCharSequence(NotificationCompat.EXTRA_TITLE));

        // Checks that the notification text is set to the default text (since EXTRA_TEXT isn't
        // set). 11 >=
        assertEquals(
                mContext.getResources().getString(R.string.call_notification_screening_text),
                extras.getCharSequence(NotificationCompat.EXTRA_TEXT));

        // Create a new NotificationCompat Builder object based on the notification.
        // This allows us to inspect various fields, including actions.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext,
                notification);

        // For versions above 11, the "Person" name from the style is applied to the title.
        assertEquals("test name", builder.mContentTitle);

        assertNotNull(notification.actions);
        assertEquals(2, notification.actions.length);

        // Check that the decline action has the suggested color and title.
        assertEquals(mContext.getString(R.string.call_notification_hang_up_action),
                notification.actions[0].title.toString());
        assertEquals(ContextCompat.getColor(mContext, R.color.call_notification_decline_color),
                ((SpannableStringBuilder) notification.actions[0].title).getSpans(0,
                        notification.actions[0].title.length(),
                        ForegroundColorSpan.class)[0].getForegroundColor());

        // Check that the answer action has the suggested color and title.
        assertEquals(mContext.getString(R.string.call_notification_answer_action),
                notification.actions[1].title.toString());
        assertEquals(ContextCompat.getColor(mContext, R.color.call_notification_answer_color),
                ((SpannableStringBuilder) notification.actions[1].title).getSpans(0,
                        notification.actions[1].title.length(),
                        ForegroundColorSpan.class)[0].getForegroundColor());

        assertEquals(personIcon.toString(), notification.getLargeIcon().toString());
    }

    @SdkSuppress(maxSdkVersion = 27)
    @Test
    public void testCallStyle_callStyleLegacyNotificationImportantPeopleUri() {
        PendingIntent answerIntent = createIntent("answer");
        PendingIntent declineIntent = createIntent("decline");
        Person callerPerson = new Person.Builder()
                .setName("test name")
                .setUri("personUri")
                .build();
        NotificationCompat.CallStyle callStyle = NotificationCompat.CallStyle.forIncomingCall(
                callerPerson, declineIntent, answerIntent);

        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(callStyle)
                .build();

        // Create a new NotificationCompat Builder object based on the notification.
        // This allows us to inspect various fields, including actions.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, notification);

        // For API versions 21 through 28, we check the URI is in the mPeople set of string URIs.
        assertTrue(builder.mPeople.contains(callerPerson.getUri()));
    }

    @SdkSuppress(minSdkVersion = 28, maxSdkVersion = 30)
    @Test
    public void testCallStyle_callStyleLegacyNotificationImportantPeople() {
        // Create a placeholder icon for use with the person.
        IconCompat personIcon = IconCompat.createWithResource(mContext,
                R.drawable.ic_call_answer_video);

        PendingIntent answerIntent = createIntent("answer");
        PendingIntent declineIntent = createIntent("decline");
        Person callerPerson = new Person.Builder()
                .setName("test name")
                .setIcon(personIcon)
                .setUri("personUri")
                .build();
        NotificationCompat.CallStyle callStyle = NotificationCompat.CallStyle.forIncomingCall(
                callerPerson, declineIntent, answerIntent);

        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(callStyle)
                .build();

        // Create a new NotificationCompat Builder object based on the notification.
        // This allows us to inspect various fields, including actions.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, notification);

        // For API versions after 28, the person object is added as relevant to the notification.
        boolean foundPerson = false;
        for (Person person : builder.mPersonList) {
            foundPerson |= person.getUri() == callerPerson.getUri();
        }
        assertTrue(foundPerson);
    }

    @SdkSuppress(maxSdkVersion = 30)
    @Test
    public void testCallStyle_callStyleLegacySetsCategory() {
        PendingIntent answerIntent = createIntent("answer");
        PendingIntent declineIntent = createIntent("decline");
        Person callerPerson = new Person.Builder().setName("test name").build();
        NotificationCompat.CallStyle callStyle = NotificationCompat.CallStyle.forIncomingCall(
                callerPerson, declineIntent, answerIntent);

        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(callStyle)
                .build();

        assertEquals(NotificationCompat.CATEGORY_CALL, notification.category);
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    public void testBuilderFromNotification_fromCallStyleCompat() {
        // Create the NotificationCompat CallStyle for an Incoming Call.
        PendingIntent answerIntent = createIntent("answer");
        PendingIntent declineIntent = createIntent("decline");
        NotificationCompat.CallStyle callStyle = NotificationCompat.CallStyle.forIncomingCall(
                new Person.Builder().setName(
                        "test name").build(), answerIntent, declineIntent);

        Bundle testBundle = new Bundle();
        testBundle.putString("testExtraKey", "testExtraValue");

        Notification expectedNotification = new NotificationCompat.Builder(mContext, "test id")
                .setContentTitle("test name")
                .setContentText("contentText")
                .setContentInfo("contentInfo")
                .setSubText("subText")
                .setNumber(1)
                .setProgress(10, 1, false)
                .setSettingsText("settingsText")
                .setLocalOnly(true)
                .setAutoCancel(true)
                .setStyle(callStyle)
                .addExtras(testBundle)
                .build();
        Notification recoveredNotification = new NotificationCompat.Builder(mContext,
                expectedNotification).build();
        assertNotificationEquals(expectedNotification, recoveredNotification);
    }

    private PendingIntent createIntent(String actionName) {
        return PendingIntent.getActivity(mContext, 0, new Intent(actionName),
                PendingIntent.FLAG_IMMUTABLE);
    }

    @Test
    public void action_builder_hasDefault() {
        NotificationCompat.Action action =
                newActionBuilder().build();
        assertEquals(NotificationCompat.Action.SEMANTIC_ACTION_NONE, action.getSemanticAction());
    }

    @Test
    public void action_builder_setSemanticAction() {
        NotificationCompat.Action action =
                newActionBuilder()
                        .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                        .build();
        assertEquals(NotificationCompat.Action.SEMANTIC_ACTION_REPLY, action.getSemanticAction());
    }

    @Test
    public void action_builder_fromAndroidActionAllowsZeroResIcon() {
        // Create action with a 0 resId Icon.
        Notification.Action action = new Notification.Action.Builder(0, "title", null)
                .build();
        // Create a Compat Action Builder from the Android Action.
        NotificationCompat.Action compatAction =
                NotificationCompat.Action.Builder.fromAndroidAction(action).build();
        // The zero res icon is not set, but the action is created.
        assertNull(compatAction.getIconCompat());
        assertEquals("title", compatAction.getTitle());
    }

    @Test
    public void action_semanticAction_toAndFromNotification() {
        NotificationCompat.Action action =
                newActionBuilder()
                        .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                        .build();
        Notification notification = newNotificationBuilder().addAction(action).build();
        NotificationCompat.Action result = NotificationCompat.getAction(notification, 0);

        assertEquals(NotificationCompat.Action.SEMANTIC_ACTION_REPLY, result.getSemanticAction());
    }

    private static final NotificationCompat.Action TEST_INVISIBLE_ACTION =
            newActionBuilder()
                    .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MUTE)
                    .setShowsUserInterface(false)
                    .build();

    @Test
    public void getInvisibleActions() {
        Notification notification =
                newNotificationBuilder().addInvisibleAction(TEST_INVISIBLE_ACTION).build();
        verifyInvisibleActionExists(notification);
    }

    @Test
    public void getInvisibleActions_withCarExtender() {
        NotificationCompat.CarExtender carExtender = new NotificationCompat.CarExtender();
        Notification notification = newNotificationBuilder()
                .addInvisibleAction(TEST_INVISIBLE_ACTION)
                .extend(carExtender)
                .build();
        verifyInvisibleActionExists(notification);
    }

    @Test
    public void getContentTitle() {
        Notification notification = new NotificationCompat.Builder(mContext, "test channel")
                .setContentTitle("example title")
                .build();

        assertEquals("example title", NotificationCompat.getContentTitle(notification));
    }

    @Test
    public void action_builder_defaultNotContextual() {
        NotificationCompat.Action action = newActionBuilder().build();
        assertFalse(action.isContextual());
    }

    @Test
    public void action_builder_setContextual() {
        // Without a PendingIntent the Action.Builder class throws an NPE when building a contextual
        // action.
        PendingIntent pendingIntent = PendingIntent.getActivity(
                mContext, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Action action =
                new NotificationCompat.Action.Builder(0, "Test Title", pendingIntent)
                        .setContextual(true)
                        .build();
        assertTrue(action.isContextual());
    }

    @Test
    public void action_builder_contextual_invalidIntentCausesNpe() {
        NotificationCompat.Action.Builder builder = newActionBuilder().setContextual(true);
        try {
            builder.build();
            fail("Creating a contextual Action with a null PendingIntent should cause a "
                    + " NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 28) // TODO(gsennton): this test only applies to Q+ devices.
    public void action_contextual_toAndFromNotification() {
        if (Build.VERSION.SDK_INT < 29) return;
        // Without a PendingIntent the Action.Builder class throws an NPE when building a contextual
        // action.
        PendingIntent pendingIntent = PendingIntent.getActivity(
                mContext, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Action action = new NotificationCompat.Action.Builder(
                R.drawable.notification_bg, "Test Title", pendingIntent)
                .setContextual(true)
                .build();
        Notification notification = newNotificationBuilder().addAction(action).build();
        NotificationCompat.Action result = NotificationCompat.getAction(notification, 0);

        assertTrue(result.isContextual());
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P) // TODO(gsennton): This only works on Q+
    public void getAllowSystemGeneratedContextualActions_trueByDefault() {
        if (Build.VERSION.SDK_INT < 29) return;
        Notification notification =
                new NotificationCompat.Builder(mContext, "test channel").build();
        assertTrue(NotificationCompat.getAllowSystemGeneratedContextualActions(notification));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P) // TODO(gsennton): This only works on Q+
    public void getAllowSystemGeneratedContextualActions() {
        if (Build.VERSION.SDK_INT < 29) return;
        Notification notification = new NotificationCompat.Builder(mContext, "test channel")
                .setAllowSystemGeneratedContextualActions(false)
                .build();
        assertFalse(NotificationCompat.getAllowSystemGeneratedContextualActions(notification));
    }

    @Test
    public void action_builder_defaultNoAuthRequired() {
        NotificationCompat.Action action = newActionBuilder().build();
        assertFalse(action.isAuthenticationRequired());
    }

    @Test
    public void action_builder_setAuthRequired() {
        PendingIntent pendingIntent = PendingIntent.getActivity(
                mContext, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Action action =
                new NotificationCompat.Action.Builder(0, "Test Title", pendingIntent)
                        .setAuthenticationRequired(true)
                        .build();
        assertTrue(action.isAuthenticationRequired());
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    public void action_authRequired_toAndFromNotification() {
        if (Build.VERSION.SDK_INT < 31) return;
        PendingIntent pendingIntent = PendingIntent.getActivity(
                mContext, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Action action = new NotificationCompat.Action.Builder(
                R.drawable.notification_bg, "Test Title", pendingIntent)
                .setAuthenticationRequired(true)
                .build();
        Notification notification = newNotificationBuilder().addAction(action).build();
        NotificationCompat.Action result = NotificationCompat.getAction(notification, 0);

        assertTrue(result.isAuthenticationRequired());
    }

    @Test
    public void tvExtenderSetGetChannelId() {
        NotificationCompat.TvExtender tvExtender = new NotificationCompat.TvExtender();
        tvExtender.setChannelId("My cool channel");
        assertEquals("My cool channel", tvExtender.getChannelId());
    }

    @Test
    public void tvExtenderSetGetContentIntent() {
        NotificationCompat.TvExtender tvExtender = new NotificationCompat.TvExtender();
        PendingIntent intent =
                PendingIntent.getActivity(mContext, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE);
        tvExtender.setContentIntent(intent);
        assertEquals(intent, tvExtender.getContentIntent());
    }

    @Test
    public void tvExtenderSetGetDeleteIntent() {
        NotificationCompat.TvExtender tvExtender = new NotificationCompat.TvExtender();
        PendingIntent intent =
                PendingIntent.getActivity(mContext, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE);
        tvExtender.setDeleteIntent(intent);
        assertEquals(intent, tvExtender.getDeleteIntent());
    }

    @Test
    public void tvExtenderSetGetSuppressShowOverApps() {
        NotificationCompat.TvExtender tvExtender = new NotificationCompat.TvExtender();
        tvExtender.setSuppressShowOverApps(true);
        assertTrue(tvExtender.isSuppressShowOverApps());
        tvExtender.setSuppressShowOverApps(false);
        assertFalse(tvExtender.isSuppressShowOverApps());
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    public void tvExtenderSetsExtras() {
        PendingIntent contentIntent = createIntent("content");
        PendingIntent deleteIntent = createIntent("delete");

        NotificationCompat.TvExtender tvExtender = new NotificationCompat.TvExtender()
                .setChannelId("My cool channel")
                .setContentIntent(contentIntent)
                .setDeleteIntent(deleteIntent)
                .setSuppressShowOverApps(true);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext,
                "test channel")
                .setSmallIcon(0)
                .setContentTitle("title")
                .setContentText("text");

        builder = tvExtender.extend(builder);
        Notification notification = builder.build();

        Bundle tvExtensions =
                notification.extras.getBundle(NotificationCompat.TvExtender.EXTRA_TV_EXTENDER);
        assertNotNull(tvExtensions);
        assertEquals("My cool channel",
                tvExtensions.getString(NotificationCompat.TvExtender.EXTRA_CHANNEL_ID));
        assertEquals(contentIntent,
                ((PendingIntent) tvExtensions.getParcelable(
                        NotificationCompat.TvExtender.EXTRA_CONTENT_INTENT)));
        assertEquals(deleteIntent,
                ((PendingIntent) tvExtensions.getParcelable(
                        NotificationCompat.TvExtender.EXTRA_DELETE_INTENT)));
        assertTrue(tvExtensions.getBoolean(
                NotificationCompat.TvExtender.EXTRA_SUPPRESS_SHOW_OVER_APPS));
        assertTrue(tvExtender.isAvailableOnTv());
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    public void tvExtenderFromNotification() {
        // Use TvExtender to set all the information in the Notification.
        // Note that this relies on the tvExtenderSetsExtras to assure us of correctness.
        PendingIntent contentIntent = createIntent("content");
        PendingIntent deleteIntent = createIntent("delete");

        NotificationCompat.TvExtender tvExtender = new NotificationCompat.TvExtender()
                .setChannelId("My cool channel")
                .setContentIntent(contentIntent)
                .setDeleteIntent(deleteIntent)
                .setSuppressShowOverApps(true);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext,
                "test channel")
                .setSmallIcon(0)
                .setContentTitle("title")
                .setContentText("text");

        builder = tvExtender.extend(builder);
        Notification notification = builder.build();

        // Recovers the tvExtender values from an already-extended Notification.
        NotificationCompat.TvExtender recoveredExtender =
                new NotificationCompat.TvExtender(notification);
        assertEquals("My cool channel", recoveredExtender.getChannelId());
        assertEquals(contentIntent, recoveredExtender.getContentIntent());
        assertEquals(deleteIntent, recoveredExtender.getDeleteIntent());
        assertTrue(recoveredExtender.isSuppressShowOverApps());
        assertTrue(recoveredExtender.isAvailableOnTv());
    }

    @Test
    public void emptyTvExtender() {
        NotificationCompat.TvExtender tvExtender = new NotificationCompat.TvExtender();
        assertTrue(tvExtender.isAvailableOnTv());
    }

    @Test
    public void setBubbleMetadataIntent() {
        IconCompat icon = IconCompat.createWithAdaptiveBitmap(BitmapFactory.decodeResource(
                mContext.getResources(),
                R.drawable.notification_bg_normal));

        PendingIntent intent =
                PendingIntent.getActivity(mContext, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE);

        PendingIntent deleteIntent =
                PendingIntent.getActivity(mContext, 1, new Intent(), PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.BubbleMetadata originalBubble =
                new NotificationCompat.BubbleMetadata.Builder(intent, icon)
                        .setAutoExpandBubble(true)
                        .setDeleteIntent(deleteIntent)
                        .setDesiredHeight(600)
                        .setSuppressNotification(true)
                        .build();

        Notification notification = new NotificationCompat.Builder(mContext, "test channel")
                .setBubbleMetadata(originalBubble)
                .build();

        NotificationCompat.BubbleMetadata roundtripBubble =
                NotificationCompat.getBubbleMetadata(notification);

        // Bubbles are only supported on Q and above; on P and earlier, simply verify that the above
        // code does not crash.
        if (Build.VERSION.SDK_INT < 29) {
            return;
        }

        assertNotNull(roundtripBubble);
        assertEquals(originalBubble.getIntent(), roundtripBubble.getIntent());
        assertNotNull(originalBubble.getIcon());
        assertEquals(originalBubble.getIcon().getType(), roundtripBubble.getIcon().getType());

        assertEquals(originalBubble.getAutoExpandBubble(), roundtripBubble.getAutoExpandBubble());
        assertEquals(originalBubble.getDeleteIntent(), roundtripBubble.getDeleteIntent());
        assertEquals(originalBubble.getDesiredHeight(), roundtripBubble.getDesiredHeight());
        assertEquals(
                originalBubble.isNotificationSuppressed(),
                roundtripBubble.isNotificationSuppressed());
    }

    @Test
    public void setBubbleMetadataShortcut() {
        String shortcutId = "someShortcut";
        PendingIntent deleteIntent =
                PendingIntent.getActivity(mContext, 1, new Intent(), PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.BubbleMetadata originalBubble =
                new NotificationCompat.BubbleMetadata.Builder(shortcutId)
                        .setAutoExpandBubble(true)
                        .setDeleteIntent(deleteIntent)
                        .setDesiredHeight(600)
                        .setSuppressNotification(true)
                        .build();

        Notification notification = new NotificationCompat.Builder(mContext, "test channel")
                .setBubbleMetadata(originalBubble)
                .build();

        NotificationCompat.BubbleMetadata roundtripBubble =
                NotificationCompat.getBubbleMetadata(notification);

        if (Build.VERSION.SDK_INT < 30) {
            // Shortcut bubbles are only supported on 30+ so it's null on earlier SDKs.
            assertNull(roundtripBubble);
            return;
        }

        assertNotNull(roundtripBubble);

        assertEquals(shortcutId, roundtripBubble.getShortcutId());
        // These should be null if it's a shortcut
        assertEquals(null, roundtripBubble.getIntent());
        assertEquals(null, roundtripBubble.getIcon());

        assertEquals(originalBubble.getAutoExpandBubble(), roundtripBubble.getAutoExpandBubble());
        assertEquals(originalBubble.getDeleteIntent(), roundtripBubble.getDeleteIntent());
        assertEquals(originalBubble.getDesiredHeight(), roundtripBubble.getDesiredHeight());
        assertEquals(
                originalBubble.isNotificationSuppressed(),
                roundtripBubble.isNotificationSuppressed());
    }

    @Test
    public void setBubbleMetadataDesiredHeightResId() {
        IconCompat icon = IconCompat.createWithAdaptiveBitmap(BitmapFactory.decodeResource(
                mContext.getResources(),
                R.drawable.notification_bg_normal));

        PendingIntent intent =
                PendingIntent.getActivity(mContext, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.BubbleMetadata originalBubble =
                new NotificationCompat.BubbleMetadata.Builder()
                        .setDesiredHeightResId(R.dimen.compat_notification_large_icon_max_height)
                        .setIcon(icon)
                        .setIntent(intent)
                        .build();

        Notification notification = new NotificationCompat.Builder(mContext, "test channel")
                .setBubbleMetadata(originalBubble)
                .build();

        NotificationCompat.BubbleMetadata roundtripBubble =
                NotificationCompat.getBubbleMetadata(notification);

        // Bubbles are only supported on Q and above; on P and earlier, simply verify that the above
        // code does not crash.
        if (Build.VERSION.SDK_INT < 29) {
            return;
        }

        // TODO: Check notification itself.

        assertNotNull(roundtripBubble);

        assertEquals(
                originalBubble.getDesiredHeightResId(),
                roundtripBubble.getDesiredHeightResId());
    }

    @Test
    public void setBubbleMetadataToNull() {
        Notification notification = new NotificationCompat.Builder(mContext, "test channel")
                .setBubbleMetadata(null)
                .build();

        assertNull(NotificationCompat.getBubbleMetadata(notification));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testPeopleField() {
        final Person person1 = new Person.Builder().setName("test name").setKey("key").build();
        final Person person2 = new Person.Builder()
                .setName("test name 2").setKey("key 2").setImportant(true).build();

        final NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext, "test channel")
                        .addPerson("test:selfUri")
                        .addPerson(person1)
                        .addPerson(person2);

        final Notification notification = builder.build();

        // before testing the notification we've built with people, test the clearPeople() method
        final Notification notificationWithoutPeople = builder.clearPeople().build();
        assertNull(notificationWithoutPeople.extras.get(NotificationCompat.EXTRA_PEOPLE));
        assertNull(notificationWithoutPeople.extras.get(NotificationCompat.EXTRA_PEOPLE_LIST));

        if (Build.VERSION.SDK_INT >= 29) {
            assertNull(notificationWithoutPeople.extras.get(NotificationCompat.EXTRA_PEOPLE));
            final ArrayList<android.app.Person> peopleList =
                    notification.extras.getParcelableArrayList(Notification.EXTRA_PEOPLE_LIST);
            final ArraySet<android.app.Person> people = new ArraySet<>(peopleList);
            final ArraySet<android.app.Person> expected = new ArraySet<>();
            expected.add(new Person.Builder().setUri("test:selfUri").build().toAndroidPerson());
            expected.add(person1.toAndroidPerson());
            expected.add(person2.toAndroidPerson());
            assertEquals(expected, people);
        } else if (Build.VERSION.SDK_INT >= 28) {
            assertNull(notificationWithoutPeople.extras.get(NotificationCompat.EXTRA_PEOPLE));
            // Person#equals is not implemented in API 28, so comparing uri manually
            final ArrayList<android.app.Person> peopleList =
                    notification.extras.getParcelableArrayList(Notification.EXTRA_PEOPLE_LIST);
            final ArraySet<String> people = new ArraySet<>();
            for (android.app.Person p : peopleList) {
                people.add(p.getName() + "\t" + p.getUri());
            }
            final ArraySet<String> expected = new ArraySet<>();
            expected.add("null\ttest:selfUri");
            expected.add("test name\tnull");
            expected.add("test name 2\tnull");
            assertEquals(expected, people);
        } else {
            assertNull(notificationWithoutPeople.extras.get(NotificationCompat.EXTRA_PEOPLE_LIST));
            final String[] peopleArray =
                    notification.extras.getStringArray(Notification.EXTRA_PEOPLE);
            if (peopleArray == null) {
                throw new IllegalStateException("Notification.EXTRA_PEOPLE is null");
            }
            final List<String> peopleList = Arrays.asList(peopleArray);
            final ArraySet<String> people = new ArraySet<>(peopleList);
            final ArraySet<String> expected = new ArraySet<>();
            expected.add("name:test name");
            expected.add("name:test name 2");
            expected.add("test:selfUri");
            assertEquals(expected, people);
        }

        // Test the getter as well
        final ArraySet<String> people = new ArraySet<>();
        for (Person person : NotificationCompat.getPeople(notification)) {
            people.add(person.getName() + "\t" + person.getUri());
        }
        final ArraySet<String> expected = new ArraySet<>();
        if (Build.VERSION.SDK_INT >= 28) {
            expected.add("test name\tnull");
            expected.add("test name 2\tnull");
            expected.add("null\ttest:selfUri");
        } else {
            // On older platforms, the name is converted into a URI
            expected.add("null\tname:test name");
            expected.add("null\tname:test name 2");
            expected.add("null\ttest:selfUri");
        }
        assertEquals(expected, people);
    }

    @Test
    public void testProgressStyle_className() {
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();
        assertThat(progressStyle.getClassName()).isEqualTo(
                "androidx.core.app.NotificationCompat$ProgressStyle");
    }

    @Test
    public void testProgressStyle_getAndSetProgressSegments() {
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();

        final List<NotificationCompat.ProgressStyle.Segment> segments = List.of(
                new NotificationCompat.ProgressStyle.Segment(20).setColor(0xFF00FF00).setId(1),
                new NotificationCompat.ProgressStyle.Segment(30).setColor(0xFFFF0000).setId(2));

        progressStyle.setProgressSegments(segments);
        assertThat(progressStyle.getProgressSegments()).isEqualTo(segments);
    }

    @Test
    public void testProgressStyle_addProgressSegments() {
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();

        final NotificationCompat.ProgressStyle.Segment segment1 =
                new NotificationCompat.ProgressStyle.Segment(25).setColor(0xFF0000FF).setId(3);
        final NotificationCompat.ProgressStyle.Segment segment2 =
                new NotificationCompat.ProgressStyle.Segment(45).setColor(0xFFFFFF00).setId(4);

        progressStyle.addProgressSegment(segment1);
        progressStyle.addProgressSegment(segment2);

        final List<NotificationCompat.ProgressStyle.Segment> segments =
                progressStyle.getProgressSegments();
        assertThat(segments.size()).isEqualTo(2);
        assertThat(segment1).isEqualTo(segments.get(0));
        assertThat(segment2).isEqualTo(segments.get(1));
    }

    @Test
    public void testProgressStyle_addSegment_ignoresNegativeLength() {
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();

        progressStyle.addProgressSegment(new NotificationCompat.ProgressStyle.Segment(-10));
        assertThat(progressStyle.getProgressSegments()).isEmpty();
    }

    @Test
    public void testProgressStyle_addSegment_ignoresZeroLength() {
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();

        progressStyle.addProgressSegment(new NotificationCompat.ProgressStyle.Segment(0));
        assertThat(progressStyle.getProgressSegments()).isEmpty();
    }

    @Test
    public void testProgressStyle_setSegments_ignoresNegativeLength() {
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();

        progressStyle.setProgressSegments(
                List.of(new NotificationCompat.ProgressStyle.Segment(-10)));
        assertThat(progressStyle.getProgressSegments()).isEmpty();
    }

    @Test
    public void testProgressStyle_setSegments_ignoresZeroLength() {
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();

        progressStyle.setProgressSegments(List.of(new NotificationCompat.ProgressStyle.Segment(0)));
        assertThat(progressStyle.getProgressSegments()).isEmpty();
    }

    @Test
    public void testProgressStyle_getAndSetProgressPoints() {
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();

        final List<NotificationCompat.ProgressStyle.Point> points = List.of(
                new NotificationCompat.ProgressStyle.Point(10).setColor(0xFF00FF00).setId(5),
                new NotificationCompat.ProgressStyle.Point(50).setColor(0xFFFF0000).setId(6));
        progressStyle.setProgressPoints(points);

        assertThat(progressStyle.getProgressPoints()).isEqualTo(points);
    }

    @Test
    public void testProgressStyle_addProgressPoints() {
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();

        final NotificationCompat.ProgressStyle.Point point1 =
                new NotificationCompat.ProgressStyle.Point(20).setColor(0xFF0000FF).setId(7);
        final NotificationCompat.ProgressStyle.Point point2 =
                new NotificationCompat.ProgressStyle.Point(80).setColor(0xFFFFFF00).setId(8);

        progressStyle.addProgressPoint(point1);
        progressStyle.addProgressPoint(point2);

        final List<NotificationCompat.ProgressStyle.Point> points =
                progressStyle.getProgressPoints();
        assertThat(points.size()).isEqualTo(2);
        assertThat(points.get(0)).isEqualTo(point1);
        assertThat(points.get(1)).isEqualTo(point2);
    }

    @Test
    public void testProgressStyle_addPoint_ignoresNegativePosition() {
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();

        progressStyle.addProgressPoint(new NotificationCompat.ProgressStyle.Point(-5));

        assertThat(progressStyle.getProgressPoints()).isEmpty();
    }

    @Test
    public void testProgressStyle_addPoint_ignoresZeroPosition() {
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();

        progressStyle.addProgressPoint(new NotificationCompat.ProgressStyle.Point(0));

        assertThat(progressStyle.getProgressPoints()).isEmpty();
    }

    @Test
    public void testProgressStyle_getAndSetProgress() {
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();

        progressStyle.setProgress(75);

        assertThat(progressStyle.getProgress()).isEqualTo(75);
    }

    @Test
    public void testProgressStyle_getProgressMax_noSegments() {
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();
        assertEquals(100, progressStyle.getProgressMax());
    }

    @Test
    public void testProgressStyle_getProgressMax_withSegments() {
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();

        progressStyle.addProgressSegment(new NotificationCompat.ProgressStyle.Segment(20));
        progressStyle.addProgressSegment(new NotificationCompat.ProgressStyle.Segment(30));
        progressStyle.addProgressSegment(new NotificationCompat.ProgressStyle.Segment(80));

        assertThat(progressStyle.getProgressMax()).isEqualTo(130);
    }

    @Test
    public void testProgressStyle_getProgressMax_zeroLength() {
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();

        progressStyle.addProgressSegment(new NotificationCompat.ProgressStyle.Segment(20));
        progressStyle.addProgressSegment(new NotificationCompat.ProgressStyle.Segment(0));
        progressStyle.addProgressSegment(new NotificationCompat.ProgressStyle.Segment(30));

        assertThat(progressStyle.getProgressMax()).isEqualTo(50);
    }

    @Test
    public void testProgressStyle_getProgressMax_someNegativeLength() {
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();

        progressStyle.addProgressSegment(new NotificationCompat.ProgressStyle.Segment(20));
        progressStyle.addProgressSegment(new NotificationCompat.ProgressStyle.Segment(-10));
        progressStyle.addProgressSegment(new NotificationCompat.ProgressStyle.Segment(30));

        assertThat(progressStyle.getProgressMax()).isEqualTo(50);
    }

    @Test
    public void testProgressStyle_getProgressMax_overflow() {
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();

        progressStyle.addProgressSegment(
                new NotificationCompat.ProgressStyle.Segment(Integer.MAX_VALUE));
        progressStyle.addProgressSegment(new NotificationCompat.ProgressStyle.Segment(1));

        assertThat(progressStyle.getProgressMax()).isEqualTo(100);
    }

    @Test
    public void testProgressStyle_getAndSetProgressIndeterminate() {
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();
        progressStyle.setProgressIndeterminate(true);
        assertThat(progressStyle.isProgressIndeterminate()).isTrue();
        progressStyle.setProgressIndeterminate(false);
        assertThat(progressStyle.isProgressIndeterminate()).isFalse();
    }

    @Test
    public void testProgressStyle_getAndSetStyledByProgress() {
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();
        progressStyle.setStyledByProgress(false);
        assertThat(progressStyle.isStyledByProgress()).isFalse();
        progressStyle.setStyledByProgress(true);
        assertThat(progressStyle.isStyledByProgress()).isTrue();
    }

    @Test
    public void testProgressStyle_getAndSetProgressTrackerIcon() {
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();
        final IconCompat icon = IconCompat.createWithAdaptiveBitmap(BitmapFactory.decodeResource(
                mContext.getResources(),
                R.drawable.notification_bg_normal));
        progressStyle.setProgressTrackerIcon(icon);
        assertThat(progressStyle.getProgressTrackerIcon()).isEqualTo(icon);
    }

    @Test
    public void testProgressStyle_getAndSetProgressStartIcon() {
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();
        final IconCompat icon = IconCompat.createWithAdaptiveBitmap(BitmapFactory.decodeResource(
                mContext.getResources(),
                R.drawable.notification_bg_normal));
        progressStyle.setProgressStartIcon(icon);
        assertThat(progressStyle.getProgressStartIcon()).isEqualTo(icon);
    }

    @Test
    public void testProgressStyle_getAndSetProgressEndIcon() {
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();
        final IconCompat icon = IconCompat.createWithAdaptiveBitmap(BitmapFactory.decodeResource(
                mContext.getResources(),
                R.drawable.notification_bg_normal));
        progressStyle.setProgressEndIcon(icon);
        assertThat(progressStyle.getProgressEndIcon()).isEqualTo(icon);
    }

    @Test
    public void testProgressStyle_displayCustomViewInline() {
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();
        assertTrue(progressStyle.displayCustomViewInline());
    }

    @Test
    public void testProgressStyle_restoreFromCompatExtras() {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext,
                "test_channel");
        final NotificationCompat.ProgressStyle progressStyle =
                new NotificationCompat.ProgressStyle();
        builder.setStyle(progressStyle);

        // Set all the fields of ProgressStyle
        progressStyle.setStyledByProgress(false);
        progressStyle.setProgress(75);
        progressStyle.setProgressIndeterminate(true);

        final List<NotificationCompat.ProgressStyle.Segment> segments = List.of(
                new NotificationCompat.ProgressStyle.Segment(20).setColor(0xFF00FF00).setId(1),
                new NotificationCompat.ProgressStyle.Segment(80).setColor(0xFFFF0000).setId(2)
        );
        progressStyle.setProgressSegments(segments);

        final List<NotificationCompat.ProgressStyle.Point> points = List.of(
                new NotificationCompat.ProgressStyle.Point(10).setColor(0xFF0000FF).setId(3),
                new NotificationCompat.ProgressStyle.Point(50).setColor(0xFFFFFF00).setId(4)
        );
        progressStyle.setProgressPoints(points);

        final IconCompat trackerIconCompat = IconCompat.createWithResource(mContext,
                android.R.drawable.ic_menu_compass);
        progressStyle.setProgressTrackerIcon(trackerIconCompat);

        final IconCompat startIconCompat = IconCompat.createWithResource(mContext,
                android.R.drawable.ic_menu_call);
        progressStyle.setProgressStartIcon(startIconCompat);

        final IconCompat endIconCompat = IconCompat.createWithResource(mContext,
                android.R.drawable.ic_menu_info_details);
        progressStyle.setProgressEndIcon(endIconCompat);

        final Notification n = builder.build();
        final Bundle extras = NotificationCompat.getExtras(n);

        assertThat(extras.getBoolean(NotificationCompat.EXTRA_STYLED_BY_PROGRESS)).isFalse();
        assertThat(extras.getInt(NotificationCompat.EXTRA_PROGRESS)).isEqualTo(75);
        assertThat(extras.getInt(NotificationCompat.EXTRA_PROGRESS_MAX)).isEqualTo(100);
        assertThat(extras.getBoolean(NotificationCompat.EXTRA_PROGRESS_INDETERMINATE)).isTrue();

        final List<Bundle> segmentList = BundleCompat.getParcelableArrayList(extras,
                NotificationCompat.EXTRA_PROGRESS_SEGMENTS, Bundle.class);
        assertThat(segmentList.size()).isEqualTo(2);
        final Bundle segment1 = segmentList.get(0);
        assertThat(segment1.getInt("id")).isEqualTo(1);
        assertThat(segment1.getInt("length")).isEqualTo(20);
        assertThat(segment1.getInt("colorInt")).isEqualTo(0xFF00FF00);
        final Bundle segment2 = segmentList.get(1);
        assertThat(segment2.getInt("id")).isEqualTo(2);
        assertThat(segment2.getInt("length")).isEqualTo(80);
        assertThat(segment2.getInt("colorInt")).isEqualTo(0xFFFF0000);

        final List<Bundle> pointList = BundleCompat.getParcelableArrayList(extras,
                NotificationCompat.EXTRA_PROGRESS_POINTS, Bundle.class);
        assertThat(pointList.size()).isEqualTo(2);
        final Bundle point1 = pointList.get(0);
        assertThat(point1.getInt("id")).isEqualTo(3);
        assertThat(point1.getInt("position")).isEqualTo(10);
        assertThat(point1.getInt("colorInt")).isEqualTo(0xFF0000FF);
        final Bundle point2 = pointList.get(1);
        assertThat(point2.getInt("id")).isEqualTo(4);
        assertThat(point2.getInt("position")).isEqualTo(50);
        assertThat(point2.getInt("colorInt")).isEqualTo(0xFFFFFF00);

        assertThat(extras.containsKey(NotificationCompat.EXTRA_PROGRESS_TRACKER_ICON)).isTrue();
        assertThat(extras.containsKey(NotificationCompat.EXTRA_PROGRESS_START_ICON)).isTrue();
        assertThat(extras.containsKey(NotificationCompat.EXTRA_PROGRESS_END_ICON)).isTrue();
    }

    // Add the @Test annotation to enable this test. This test is disabled by default as it's not a
    // unit test. This will simply create 4 MessagingStyle notifications so a developer may see what
    // the end result will look like on a physical device (or emulator).
    public void makeMessagingStyleNotifications() {
        NotificationManager notificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel("id", "name", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Lol a description");
            channel.enableLights(true);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(2, newMsNotification(false, false));
        notificationManager.notify(3, newMsNotification(false, true));
        notificationManager.notify(4, newMsNotification(true, false));
        notificationManager.notify(5, newMsNotification(true, true));
    }

    private Notification newMsNotification(boolean isGroup, boolean hasTitle) {
        IconCompat testIcon =
                IconCompat.createWithBitmap(
                        BitmapFactory.decodeResource(
                                mContext.getResources(),
                                R.drawable.notification_bg_normal));
        NotificationCompat.MessagingStyle ms = new NotificationCompat.MessagingStyle(
                new Person.Builder().setName("Me").setIcon(testIcon).build());
        String message = "compat. isGroup? " + Boolean.toString(isGroup)
                + "; hasTitle? " + Boolean.toString(hasTitle);
        ms.addMessage(new Message(
                message, 40, new Person.Builder().setName("John").setIcon(testIcon).build()));
        ms.addMessage(new Message("Heyo", 41, (Person) null));
        ms.setGroupConversation(isGroup);
        ms.setConversationTitle(hasTitle ? "My Conversation Title" : null);

        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new NotificationCompat.Builder(mContext, "id");
        } else {
            builder = new NotificationCompat.Builder(mContext);
        }
        builder.setSmallIcon(R.drawable.notification_bg_normal);
        builder.setStyle(ms);
        return builder.build();
    }

    private static void verifyInvisibleActionExists(Notification notification) {
        List<NotificationCompat.Action> result =
                NotificationCompat.getInvisibleActions(notification);
        assertTrue("Expecting 1 result, got " + result.size(), result.size() == 1);
        NotificationCompat.Action resultAction = result.get(0);
        assertEquals(resultAction.getIcon(), TEST_INVISIBLE_ACTION.getIcon());
        assertEquals(resultAction.getTitle(), TEST_INVISIBLE_ACTION.getTitle());
        assertEquals(
                resultAction.getShowsUserInterface(),
                TEST_INVISIBLE_ACTION.getShowsUserInterface());
        assertEquals(resultAction.getSemanticAction(), TEST_INVISIBLE_ACTION.getSemanticAction());
    }

    private static RemoteInput newDataOnlyRemoteInput() {
        return new RemoteInput.Builder(DATA_RESULT_KEY)
                .setAllowFreeFormInput(false)
                .setAllowDataType("mimeType", true)
                .build();
    }

    private static RemoteInput newTextRemoteInput() {
        return new RemoteInput.Builder(TEXT_RESULT_KEY).build();  // allowFreeForm defaults to true
    }

    private static void verifyRemoteInputArrayHasSingleResult(
            RemoteInput[] remoteInputs, String expectedResultKey) {
        assertTrue(remoteInputs != null && remoteInputs.length == 1);
        assertEquals(expectedResultKey, remoteInputs[0].getResultKey());
    }

    private static NotificationCompat.Action.Builder newActionBuilder() {
        return new NotificationCompat.Action.Builder(0, "title", null);
    }

    private NotificationCompat.Builder newNotificationBuilder() {
        return new NotificationCompat.Builder(mContext)
                .setSmallIcon(0)
                .setContentTitle("title")
                .setContentText("text");
    }
}
