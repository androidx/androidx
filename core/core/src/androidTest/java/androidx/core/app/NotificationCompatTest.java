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
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.BaseInstrumentationTestCase;
import android.widget.RemoteViews;

import androidx.collection.ArraySet;
import androidx.core.R;
import androidx.core.app.NotificationCompat.MessagingStyle.Message;
import androidx.core.app.NotificationCompat.Style;
import androidx.core.content.LocusIdCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.jetbrains.annotations.NotNull;
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

    @SdkSuppress(minSdkVersion = 19)
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

    @SdkSuppress(minSdkVersion = 19)
    @Test
    public void testContentTitle() {
        Notification n = new NotificationCompat.Builder(mContext)
                .setContentTitle("testContentTitle")
                .build();
        assertEquals("testContentTitle", NotificationCompat.getContentTitle(n));
    }

    @SdkSuppress(minSdkVersion = 19)
    @Test
    public void testContentText() {
        Notification n = new NotificationCompat.Builder(mContext)
                .setContentText("testContentText")
                .build();
        assertEquals("testContentText", NotificationCompat.getContentText(n));
    }

    @SdkSuppress(minSdkVersion = 19)
    @Test
    public void testContentInfo() {
        Notification n = new NotificationCompat.Builder(mContext)
                .setContentInfo("testContentInfo")
                .build();
        assertEquals("testContentInfo", NotificationCompat.getContentInfo(n));
    }

    @SdkSuppress(minSdkVersion = 19)
    @Test
    public void testSubText() {
        Notification n = new NotificationCompat.Builder(mContext)
                .setSubText("testSubText")
                .build();
        assertEquals("testSubText", NotificationCompat.getSubText(n));
    }

    @FlakyTest(bugId = 190533219)
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
    public void testInvisibleActions() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        Notification nWith = builder.addInvisibleAction(0, "testAction", null)
                .build();
        List<NotificationCompat.Action> actions = NotificationCompat.getInvisibleActions(nWith);

        if (Build.VERSION.SDK_INT < 21) {
            assertEquals(0, actions.size());
            return;
        }

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

        // test true
        Notification nTrue = builder.setShowWhen(true).build();
        if (Build.VERSION.SDK_INT >= 19) {
            assertTrue(NotificationCompat.getShowWhen(nTrue));
        }

        // test false
        Notification nFalse = builder.setShowWhen(false).build();
        if (Build.VERSION.SDK_INT >= 19) {
            assertFalse(NotificationCompat.getShowWhen(nFalse));
        }
    }

    @Test
    public void testUsesChronometer() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);

        // test true
        Notification nTrue = builder.setUsesChronometer(true).build();
        if (Build.VERSION.SDK_INT >= 19) {
            assertTrue(NotificationCompat.getUsesChronometer(nTrue));
        }

        // test false
        Notification nFalse = builder.setUsesChronometer(false).build();
        if (Build.VERSION.SDK_INT >= 19) {
            assertFalse(NotificationCompat.getUsesChronometer(nFalse));
        }
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
        if (Build.VERSION.SDK_INT >= 21) {
            assertEquals(Color.GREEN, NotificationCompat.getColor(notification));
        } else {
            assertEquals(Color.TRANSPARENT, NotificationCompat.getColor(notification));
        }
    }

    @Test
    public void testVisibility() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);

        Notification n = builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC).build();
        if (Build.VERSION.SDK_INT >= 21) {
            assertEquals(NotificationCompat.VISIBILITY_PUBLIC,
                    NotificationCompat.getVisibility(n));
        } else {
            assertEquals(NotificationCompat.VISIBILITY_PRIVATE,
                    NotificationCompat.getVisibility(n));
        }
    }

    @Test
    public void testPublicVersion() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        Notification pub = builder.setContentTitle("public title").build();
        Notification priv = builder.setContentTitle("private title").setPublicVersion(pub).build();

        if (Build.VERSION.SDK_INT >= 21) {
            assertNull(NotificationCompat.getPublicVersion(pub));
            assertNotSame(pub, NotificationCompat.getPublicVersion(priv));
            assertNotificationEquals(pub, NotificationCompat.getPublicVersion(priv));
        } else {
            assertNull(NotificationCompat.getPublicVersion(pub));
            assertNull(NotificationCompat.getPublicVersion(priv));
        }
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
    @SdkSuppress(minSdkVersion = 19)
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

    @NotNull
    private List<Class<? extends Style>> getStyleSubclasses() {
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

    @SdkSuppress(minSdkVersion = 19)
    @Test
    public void testBuilderFromNotification_fromMinimalPlatform() {
        Notification original = new Notification.Builder(mContext).build();
        Notification recovered = new NotificationCompat.Builder(mContext, original).build();
        assertEquals(original.toString(), recovered.toString());
    }

    @SdkSuppress(minSdkVersion = 19)
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

    @SdkSuppress(minSdkVersion = 19)
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
        assertThat(layoutName).startsWith("android:layout/notification_template_");

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
            assertThat(layoutName).startsWith("android:layout/notification_template_");
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

        // Before Jellybean, there was no big view; expect null
        if (Build.VERSION.SDK_INT < 16) {
            assertNull(builder.createHeadsUpContentView());
            return;
        }

        // Expect the standard big notification template
        RemoteViews standardView = builder.createBigContentView();
        assertNotNull(standardView);
        String layoutName = mContext.getResources().getResourceName(standardView.getLayoutId());
        assertThat(layoutName).startsWith("android:layout/notification_template_");

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
            assertThat(layoutName).startsWith("android:layout/notification_template_");
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

        // Before Lollipop, there was no heads up view; expect null
        if (Build.VERSION.SDK_INT < 21) {
            assertNull(builder.createHeadsUpContentView());
            return;
        }

        // Expect the standard big notification template (yes, heads up defaults to big template)
        RemoteViews standardView = builder.createHeadsUpContentView();
        assertNotNull(standardView);
        String layoutName = mContext.getResources().getResourceName(standardView.getLayoutId());
        assertThat(layoutName).startsWith("android:layout/notification_template_");

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
            assertThat(layoutName).startsWith("android:layout/notification_template_");
        } else {
            // AndroidX is providing a decorated style not available on these platforms natively
            String packageName = mContext.getPackageName();
            assertEquals(packageName + ":layout/notification_template_custom_big", layoutName);
        }
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
    public void testNotificationActionBuilder_assignsColorized() throws Throwable {
        Notification n = newNotificationBuilder().setColorized(true).build();
        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(Boolean.TRUE, n.extras.get(EXTRA_COLORIZED));
        }
    }

    @Test
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

    @SdkSuppress(minSdkVersion = 20)
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

    @SdkSuppress(minSdkVersion = 20)
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

    @SdkSuppress(minSdkVersion = 20)
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

    @SdkSuppress(minSdkVersion = 17)
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

    @SdkSuppress(minSdkVersion = 17)
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

    @SdkSuppress(minSdkVersion = 17)
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

    @SdkSuppress(minSdkVersion = 17)
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
    @SdkSuppress(minSdkVersion = 23)
    public void testNotificationSmallIcon() {
        IconCompat icon = IconCompat.createWithResource(mContext,
                R.drawable.notification_action_background);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);

        builder.setSmallIcon(icon);

        Notification notification = builder.build();

        assertEquals(icon.toIcon(mContext).toString(), notification.getSmallIcon().toString());
    }

    @SdkSuppress(maxSdkVersion = 16)
    @SmallTest
    @Test
    public void testNotificationWearableExtenderAction_noActions()
            throws Throwable {
        NotificationCompat.Action a = newActionBuilder()
                .setAllowGeneratedReplies(true).build();
        NotificationCompat.WearableExtender extender = new NotificationCompat.WearableExtender()
                .addAction(a);
        Notification notification = newNotificationBuilder().extend(extender).build();
        assertTrue(new NotificationCompat.WearableExtender(notification).getActions().size() == 0);
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

        if (Build.VERSION.SDK_INT >= 20 && !(Build.VERSION.SDK_INT >= 26)) {
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
        } else if (Build.VERSION.SDK_INT < 20) {
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

        if (Build.VERSION.SDK_INT >= 20 && !(Build.VERSION.SDK_INT >= 26)) {
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
        } else if (Build.VERSION.SDK_INT >= 20) {
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
        if (Build.VERSION.SDK_INT >= 20) {
            assertEquals(groupKey, nChild.getGroup());
            assertEquals(groupKey, nSummary.getGroup());
        }
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

        if (Build.VERSION.SDK_INT >= 20 && !(Build.VERSION.SDK_INT >= 26)) {
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
    @SdkSuppress(minSdkVersion = 21)
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

    @Test
    @SdkSuppress(maxSdkVersion = 20)
    public void testHasStreamTypePre21() throws Throwable {
        Notification n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setSound(Uri.EMPTY, 34)
                .build();
        assertEquals(34, n.audioStreamType);
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

    @SdkSuppress(minSdkVersion = 16)
    @Test
    public void testBigPictureStyle_withNullBigLargeIcon() {
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.notification_bg_low_pressed);
        Notification n = new NotificationCompat.Builder(mContext, "channelId")
                .setSmallIcon(1)
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(null)
                        .setBigContentTitle("Big Content Title")
                        .setSummaryText("Summary Text"))
                .build();
        // Extras are not populated before KITKAT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Bundle extras = NotificationCompat.getExtras(n);
            assertNotNull(extras);
            assertTrue(extras.containsKey(NotificationCompat.EXTRA_LARGE_ICON_BIG));
            assertNull(extras.get(NotificationCompat.EXTRA_LARGE_ICON_BIG));
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

    @SdkSuppress(minSdkVersion = 19)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            assertSame(Icon.class, firstBuiltIcon.getClass());
            assertEquals(Icon.TYPE_BITMAP, ((Icon) firstBuiltIcon).getType());
        } else {
            assertSame(Bitmap.class, firstBuiltIcon.getClass());
        }

        Style style = Style.extractStyleFromNotification(n);
        assertNotNull(style);
        assertSame(NotificationCompat.BigPictureStyle.class, style.getClass());
        n = new NotificationCompat.Builder(mContext, "channelId")
                .setSmallIcon(1)
                .setStyle(style)
                .build();
        Parcelable rebuiltIcon = n.extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            assertSame(Icon.class, rebuiltIcon.getClass());
            assertEquals(Icon.TYPE_BITMAP, ((Icon) rebuiltIcon).getType());
        } else {
            assertSame(Bitmap.class, rebuiltIcon.getClass());
        }
    }

    @SdkSuppress(minSdkVersion = 23)
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

    @SdkSuppress(minSdkVersion = 16)
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

    @SdkSuppress(minSdkVersion = 16)
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

    @SdkSuppress(minSdkVersion = 16)
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

    @SdkSuppress(minSdkVersion = 16)
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

    @SdkSuppress(minSdkVersion = 16)
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

    @SdkSuppress(minSdkVersion = 16)
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

    @SdkSuppress(minSdkVersion = 16)
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

    @SdkSuppress(minSdkVersion = 16)
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

    @SdkSuppress(minSdkVersion = 16)
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

    @SdkSuppress(minSdkVersion = 19, maxSdkVersion = 27)
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
    @Test
    public void testMessagingStyle_apply_writesMessagePerson() {
        Notification msNotification = newMsNotification(true, true);

        Bundle[] messagesBundle =
                (Bundle[]) msNotification.extras.getParcelableArray(Notification.EXTRA_MESSAGES);
        assertEquals(2, messagesBundle.length);
        assertTrue(messagesBundle[0].containsKey(Message.KEY_NOTIFICATION_PERSON));
    }

    @SdkSuppress(minSdkVersion = 24, maxSdkVersion = 27)
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
    @SdkSuppress(minSdkVersion = 20)
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
    @SdkSuppress(minSdkVersion = 21)
    public void getInvisibleActions() {
        Notification notification =
                newNotificationBuilder().addInvisibleAction(TEST_INVISIBLE_ACTION).build();
        verifyInvisibleActionExists(notification);
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public void getInvisibleActions_withCarExtender() {
        NotificationCompat.CarExtender carExtender = new NotificationCompat.CarExtender();
        Notification notification = newNotificationBuilder()
                .addInvisibleAction(TEST_INVISIBLE_ACTION)
                .extend(carExtender)
                .build();
        verifyInvisibleActionExists(notification);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
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
        if (Build.VERSION.SDK_INT >= 19) {
            assertNull(notificationWithoutPeople.extras.get(NotificationCompat.EXTRA_PEOPLE));
            assertNull(notificationWithoutPeople.extras.get(NotificationCompat.EXTRA_PEOPLE_LIST));
        }

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
        } else if (Build.VERSION.SDK_INT >= 19) {
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
        } else if (Build.VERSION.SDK_INT >= 19) {
            // On older platforms, the name is converted into a URI
            expected.add("null\tname:test name");
            expected.add("null\tname:test name 2");
            expected.add("null\ttest:selfUri");
        }
        assertEquals(expected, people);
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
