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
package androidx.core.content;

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static android.content.Context.ACCOUNT_SERVICE;
import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.APPWIDGET_SERVICE;
import static android.content.Context.APP_OPS_SERVICE;
import static android.content.Context.AUDIO_SERVICE;
import static android.content.Context.BATTERY_SERVICE;
import static android.content.Context.BLUETOOTH_SERVICE;
import static android.content.Context.CAMERA_SERVICE;
import static android.content.Context.CAPTIONING_SERVICE;
import static android.content.Context.CLIPBOARD_SERVICE;
import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.CONSUMER_IR_SERVICE;
import static android.content.Context.DEVICE_POLICY_SERVICE;
import static android.content.Context.DISPLAY_SERVICE;
import static android.content.Context.DOWNLOAD_SERVICE;
import static android.content.Context.DROPBOX_SERVICE;
import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.content.Context.INPUT_SERVICE;
import static android.content.Context.JOB_SCHEDULER_SERVICE;
import static android.content.Context.KEYGUARD_SERVICE;
import static android.content.Context.LAUNCHER_APPS_SERVICE;
import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static android.content.Context.LOCATION_SERVICE;
import static android.content.Context.MEDIA_PROJECTION_SERVICE;
import static android.content.Context.MEDIA_ROUTER_SERVICE;
import static android.content.Context.MEDIA_SESSION_SERVICE;
import static android.content.Context.NFC_SERVICE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.NSD_SERVICE;
import static android.content.Context.POWER_SERVICE;
import static android.content.Context.PRINT_SERVICE;
import static android.content.Context.RESTRICTIONS_SERVICE;
import static android.content.Context.SEARCH_SERVICE;
import static android.content.Context.SENSOR_SERVICE;
import static android.content.Context.STORAGE_SERVICE;
import static android.content.Context.TELECOM_SERVICE;
import static android.content.Context.TELEPHONY_SERVICE;
import static android.content.Context.TELEPHONY_SUBSCRIPTION_SERVICE;
import static android.content.Context.TEXT_SERVICES_MANAGER_SERVICE;
import static android.content.Context.TV_INPUT_SERVICE;
import static android.content.Context.UI_MODE_SERVICE;
import static android.content.Context.USAGE_STATS_SERVICE;
import static android.content.Context.USB_SERVICE;
import static android.content.Context.USER_SERVICE;
import static android.content.Context.VIBRATOR_SERVICE;
import static android.content.Context.WALLPAPER_SERVICE;
import static android.content.Context.WIFI_P2P_SERVICE;
import static android.content.Context.WIFI_SERVICE;
import static android.content.Context.WINDOW_SERVICE;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.DownloadManager;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.app.UiModeManager;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.app.job.JobScheduler;
import android.app.usage.UsageStatsManager;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.RestrictionsManager;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.hardware.ConsumerIrManager;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.hardware.usb.UsbManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.media.projection.MediaProjectionManager;
import android.media.session.MediaSessionManager;
import android.media.tv.TvInputManager;
import android.net.ConnectivityManager;
import android.net.nsd.NsdManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.nfc.NfcManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.DropBoxManager;
import android.os.PowerManager;
import android.os.UserManager;
import android.os.Vibrator;
import android.os.storage.StorageManager;
import android.print.PrintManager;
import android.support.v4.BaseInstrumentationTestCase;
import android.support.v4.ThemedYellowActivity;
import android.support.v4.testutils.TestUtils;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.CaptioningManager;
import android.view.inputmethod.InputMethodManager;
import android.view.textservice.TextServicesManager;

import androidx.annotation.OptIn;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.hardware.display.DisplayManagerCompat;
import androidx.core.os.BuildCompat;
import androidx.core.test.R;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

@LargeTest
public class ContextCompatTest extends BaseInstrumentationTestCase<ThemedYellowActivity> {
    private Context mContext;
    private IntentFilter mTestFilter = new IntentFilter();
    private String mPermission;
    private BroadcastReceiver mTestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };
    public ContextCompatTest() {
        super(ThemedYellowActivity.class);
    }

    @Before
    public void setup() {
        mContext = mActivityTestRule.getActivity();
        mPermission = mContext.getPackageName() + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION";
    }

    @Test
    public void getSystemServiceName() {
        assertEquals(ACCESSIBILITY_SERVICE,
                ContextCompat.getSystemServiceName(mContext, AccessibilityManager.class));
        assertEquals(ACCOUNT_SERVICE,
                ContextCompat.getSystemServiceName(mContext, AccountManager.class));
        assertEquals(ACTIVITY_SERVICE,
                ContextCompat.getSystemServiceName(mContext, ActivityManager.class));
        assertEquals(ALARM_SERVICE,
                ContextCompat.getSystemServiceName(mContext, AlarmManager.class));
        assertEquals(AUDIO_SERVICE,
                ContextCompat.getSystemServiceName(mContext, AudioManager.class));
        assertEquals(CLIPBOARD_SERVICE,
                ContextCompat.getSystemServiceName(mContext, ClipboardManager.class));
        assertEquals(CONNECTIVITY_SERVICE,
                ContextCompat.getSystemServiceName(mContext, ConnectivityManager.class));
        assertEquals(DEVICE_POLICY_SERVICE,
                ContextCompat.getSystemServiceName(mContext, DevicePolicyManager.class));
        assertEquals(DOWNLOAD_SERVICE,
                ContextCompat.getSystemServiceName(mContext, DownloadManager.class));
        assertEquals(DROPBOX_SERVICE,
                ContextCompat.getSystemServiceName(mContext, DropBoxManager.class));
        assertEquals(INPUT_METHOD_SERVICE,
                ContextCompat.getSystemServiceName(mContext, InputMethodManager.class));
        assertEquals(KEYGUARD_SERVICE,
                ContextCompat.getSystemServiceName(mContext, KeyguardManager.class));
        assertEquals(LAYOUT_INFLATER_SERVICE,
                ContextCompat.getSystemServiceName(mContext, LayoutInflater.class));
        assertEquals(LOCATION_SERVICE,
                ContextCompat.getSystemServiceName(mContext, LocationManager.class));
        assertEquals(NFC_SERVICE,
                ContextCompat.getSystemServiceName(mContext, NfcManager.class));
        assertEquals(NOTIFICATION_SERVICE,
                ContextCompat.getSystemServiceName(mContext, NotificationManager.class));
        assertEquals(POWER_SERVICE,
                ContextCompat.getSystemServiceName(mContext, PowerManager.class));
        assertEquals(SEARCH_SERVICE,
                ContextCompat.getSystemServiceName(mContext, SearchManager.class));
        assertEquals(SENSOR_SERVICE,
                ContextCompat.getSystemServiceName(mContext, SensorManager.class));
        assertEquals(STORAGE_SERVICE,
                ContextCompat.getSystemServiceName(mContext, StorageManager.class));
        assertEquals(TELEPHONY_SERVICE,
                ContextCompat.getSystemServiceName(mContext, TelephonyManager.class));
        assertEquals(TEXT_SERVICES_MANAGER_SERVICE,
                ContextCompat.getSystemServiceName(mContext, TextServicesManager.class));
        assertEquals(UI_MODE_SERVICE,
                ContextCompat.getSystemServiceName(mContext, UiModeManager.class));
        assertEquals(USB_SERVICE,
                ContextCompat.getSystemServiceName(mContext, UsbManager.class));
        assertEquals(VIBRATOR_SERVICE,
                ContextCompat.getSystemServiceName(mContext, Vibrator.class));
        assertEquals(WALLPAPER_SERVICE,
                ContextCompat.getSystemServiceName(mContext, WallpaperManager.class));
        assertEquals(WIFI_P2P_SERVICE,
                ContextCompat.getSystemServiceName(mContext, WifiP2pManager.class));
        assertEquals(WIFI_SERVICE,
                ContextCompat.getSystemServiceName(mContext, WifiManager.class));
        assertEquals(WINDOW_SERVICE,
                ContextCompat.getSystemServiceName(mContext, WindowManager.class));
    }

    @Test
    @SdkSuppress(minSdkVersion = 16)
    public void getSystemServiceNameApi16() {
        assertEquals(INPUT_SERVICE,
                ContextCompat.getSystemServiceName(mContext, InputManager.class));
        assertEquals(MEDIA_ROUTER_SERVICE,
                ContextCompat.getSystemServiceName(mContext, MediaRouter.class));
        assertEquals(NSD_SERVICE,
                ContextCompat.getSystemServiceName(mContext, NsdManager.class));
    }

    @Test
    @SdkSuppress(minSdkVersion = 17)
    public void getSystemServiceNameApi17() {
        assertEquals(DISPLAY_SERVICE,
                ContextCompat.getSystemServiceName(mContext, DisplayManager.class));
        assertEquals(USER_SERVICE,
                ContextCompat.getSystemServiceName(mContext, UserManager.class));
    }

    @Test
    @SdkSuppress(minSdkVersion = 18)
    public void getSystemServiceNameApi18() {
        assertEquals(BLUETOOTH_SERVICE,
                ContextCompat.getSystemServiceName(mContext, BluetoothManager.class));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void getSystemServiceNameApi19() {
        assertEquals(APP_OPS_SERVICE,
                ContextCompat.getSystemServiceName(mContext, AppOpsManager.class));
        assertEquals(CAPTIONING_SERVICE,
                ContextCompat.getSystemServiceName(mContext, CaptioningManager.class));
        assertEquals(CONSUMER_IR_SERVICE,
                ContextCompat.getSystemServiceName(mContext, ConsumerIrManager.class));
        assertEquals(PRINT_SERVICE,
                ContextCompat.getSystemServiceName(mContext, PrintManager.class));
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public void getSystemServiceNameApi21() {
        assertEquals(APPWIDGET_SERVICE,
                ContextCompat.getSystemServiceName(mContext, AppWidgetManager.class));
        assertEquals(BATTERY_SERVICE,
                ContextCompat.getSystemServiceName(mContext, BatteryManager.class));
        assertEquals(CAMERA_SERVICE,
                ContextCompat.getSystemServiceName(mContext, CameraManager.class));
        assertEquals(JOB_SCHEDULER_SERVICE,
                ContextCompat.getSystemServiceName(mContext, JobScheduler.class));
        assertEquals(LAUNCHER_APPS_SERVICE,
                ContextCompat.getSystemServiceName(mContext, LauncherApps.class));
        assertEquals(MEDIA_PROJECTION_SERVICE,
                ContextCompat.getSystemServiceName(mContext, MediaProjectionManager.class));
        assertEquals(MEDIA_SESSION_SERVICE,
                ContextCompat.getSystemServiceName(mContext, MediaSessionManager.class));
        assertEquals(RESTRICTIONS_SERVICE,
                ContextCompat.getSystemServiceName(mContext, RestrictionsManager.class));
        assertEquals(TELECOM_SERVICE,
                ContextCompat.getSystemServiceName(mContext, TelecomManager.class));
        assertEquals(TV_INPUT_SERVICE,
                ContextCompat.getSystemServiceName(mContext, TvInputManager.class));
    }

    @Test
    @SdkSuppress(minSdkVersion = 22)
    public void getSystemServiceNameApi22() {
        assertEquals(TELEPHONY_SUBSCRIPTION_SERVICE,
                ContextCompat.getSystemServiceName(mContext, SubscriptionManager.class));
        assertEquals(USAGE_STATS_SERVICE,
                ContextCompat.getSystemServiceName(mContext, UsageStatsManager.class));
    }

    @Test
    public void getSystemServiceNameUnknown() {
        assertNull(ContextCompat.getSystemServiceName(mContext, String.class));
    }

    @Test
    @SdkSuppress(minSdkVersion = 23)
    public void getSystemServiceNameCallsRealMethodOn23() {
        // We explicitly test for platform delegation on API 23+ because the compat implementation
        // only handles pre-23 service types.

        final AtomicBoolean called = new AtomicBoolean();
        Context c = new ContextWrapper(mContext) {
            @Override
            public String getSystemServiceName(Class<?> serviceClass) {
                called.set(true);
                return super.getSystemServiceName(serviceClass);
            }
        };

        String serviceName = ContextCompat.getSystemServiceName(c, LayoutInflater.class);
        assertEquals(LAYOUT_INFLATER_SERVICE, serviceName);
        assertTrue(called.get());
    }

    @Test
    public void getSystemService() {
        LayoutInflater inflater = ContextCompat.getSystemService(mContext, LayoutInflater.class);
        assertNotNull(inflater);
    }

    @Test
    public void getSystemServiceUnknown() {
        assertNull(ContextCompat.getSystemService(mContext, String.class));
    }

    @Test
    @SdkSuppress(minSdkVersion = 23)
    public void getSystemServiceCallsRealMethodOn23() {
        // We explicitly test for platform delegation on API 23+ because the compat implementation
        // only handles pre-23 service types.

        final AtomicBoolean called = new AtomicBoolean();
        Context c = new ContextWrapper(mContext) {
            // Note: we're still checking the name lookup here because the non-name method is
            // final. It delegates to this function, however, which, while an implementation detail,
            // is the only way (at present) to validate behavior.
            @Override
            public String getSystemServiceName(Class<?> serviceClass) {
                called.set(true);
                return super.getSystemServiceName(serviceClass);
            }
        };

        LayoutInflater inflater = ContextCompat.getSystemService(c, LayoutInflater.class);
        assertNotNull(inflater);
        assertTrue(called.get());
    }

    @Test
    public void testGetAttributionTag() throws Throwable {
        assertEquals("Unattributed context", null, ContextCompat.getAttributionTag(mContext));

        if (Build.VERSION.SDK_INT >= 30) {
            // The following test is only expected to pass on v30+ devices
            Context attributed = mContext.createAttributionContext("test");
            assertEquals("Attributed context", "test", ContextCompat.getAttributionTag(attributed));
        }
    }

    @Test
    public void testGetColor() throws Throwable {
        assertEquals("Unthemed color load", 0xFFFF8090,
                ContextCompat.getColor(mContext, R.color.text_color));

        if (Build.VERSION.SDK_INT >= 23) {
            // The following test is only expected to pass on v23+ devices. The result of
            // calling theme-aware getColor() in pre-v23 is undefined.
            assertEquals("Themed yellow color load",
                    ContextCompat.getColor(mContext, R.color.simple_themed_selector),
                    0xFFF0B000);
        }
    }

    @Test
    public void testGetColorStateList() throws Throwable {
        ColorStateList unthemedColorStateList =
                ContextCompat.getColorStateList(mContext, R.color.complex_unthemed_selector);
        assertEquals("Unthemed color state list load: default", 0xFF70A0C0,
                unthemedColorStateList.getDefaultColor());
        assertEquals("Unthemed color state list load: focused", 0xFF70B0F0,
                unthemedColorStateList.getColorForState(
                        new int[]{android.R.attr.state_focused}, 0));
        assertEquals("Unthemed color state list load: pressed", 0xFF6080B0,
                unthemedColorStateList.getColorForState(
                        new int[]{android.R.attr.state_pressed}, 0));

        if (Build.VERSION.SDK_INT >= 23) {
            // The following tests are only expected to pass on v23+ devices. The result of
            // calling theme-aware getColorStateList() in pre-v23 is undefined.
            ColorStateList themedYellowColorStateList =
                    ContextCompat.getColorStateList(mContext, R.color.complex_themed_selector);
            assertEquals("Themed yellow color state list load: default", 0xFFF0B000,
                    themedYellowColorStateList.getDefaultColor());
            assertEquals("Themed yellow color state list load: focused", 0xFFF0A020,
                    themedYellowColorStateList.getColorForState(
                            new int[]{android.R.attr.state_focused}, 0));
            assertEquals("Themed yellow color state list load: pressed", 0xFFE0A040,
                    themedYellowColorStateList.getColorForState(
                            new int[]{android.R.attr.state_pressed}, 0));
        }
    }

    @Test
    public void testGetDrawable() throws Throwable {
        Drawable unthemedDrawable =
                ContextCompat.getDrawable(mContext, R.drawable.test_drawable_red);
        TestUtils.assertAllPixelsOfColor("Unthemed drawable load",
                unthemedDrawable, mContext.getResources().getColor(R.color.test_red));

        if (Build.VERSION.SDK_INT >= 23) {
            // The following test is only expected to pass on v23+ devices. The result of
            // calling theme-aware getDrawable() in pre-v23 is undefined.
            Drawable themedYellowDrawable =
                    ContextCompat.getDrawable(mContext, R.drawable.themed_drawable);
            TestUtils.assertAllPixelsOfColor("Themed yellow drawable load",
                    themedYellowDrawable, 0xFFF0B000);
        }
    }

    @Test
    public void testDrawableConfigurationWorkaround() throws Throwable {
        final int expectedWidth = scaleFromDensity(7, DisplayMetrics.DENSITY_LOW,
                mContext.getResources().getDisplayMetrics().densityDpi);

        // Ensure we retrieve the correct drawable configuration. Specifically,
        // this tests a workaround for a bug in drawable configuration that
        // exists on API < 16 for references to drawables.
        Drawable referencedDrawable = ContextCompat.getDrawable(mContext,
                R.drawable.aliased_drawable);
        assertEquals("Drawable configuration does not match DisplayMetrics",
                expectedWidth, referencedDrawable.getIntrinsicWidth());
    }

    private static int scaleFromDensity(int size, int sdensity, int tdensity) {
        if (sdensity == tdensity) {
            return size;
        }

        // Scale by tdensity / sdensity, rounding up.
        return ((size * tdensity) + (sdensity >> 1)) / sdensity;
    }

    @Test
    public void testRegisterReceiver_noExportStateFlagThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> ContextCompat.registerReceiver(mContext,
                mTestReceiver, mTestFilter, 0));
    }

    @Test
    public void testRegisterReceiver_specifyBothExportStateFlagsThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> ContextCompat.registerReceiver(mContext,
                mTestReceiver, mTestFilter,
                ContextCompat.RECEIVER_EXPORTED | ContextCompat.RECEIVER_NOT_EXPORTED));
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    public void testRegisterReceiverApi33() {
        Context spyContext = spy(mContext);

        ContextCompat.registerReceiver(spyContext, mTestReceiver, mTestFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
        verify(spyContext).registerReceiver(eq(mTestReceiver), eq(mTestFilter), eq(null),
                any(), eq(ContextCompat.RECEIVER_NOT_EXPORTED));

        ContextCompat.registerReceiver(spyContext, mTestReceiver, mTestFilter,
                ContextCompat.RECEIVER_EXPORTED);
        verify(spyContext).registerReceiver(eq(mTestReceiver), eq(mTestFilter), eq(null), any(),
                eq(ContextCompat.RECEIVER_EXPORTED));
    }

    @Test
    @SdkSuppress(minSdkVersion = 26, maxSdkVersion = 32)
    public void testRegisterReceiverApi26() {
        Context spyContext = spy(mContext);

        ContextCompat.registerReceiver(spyContext, mTestReceiver, mTestFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
        verify(spyContext).registerReceiver(eq(mTestReceiver), eq(mTestFilter),
                eq(mPermission), any());

        ContextCompat.registerReceiver(spyContext, mTestReceiver, mTestFilter,
                ContextCompat.RECEIVER_EXPORTED);
        verify(spyContext).registerReceiver(eq(mTestReceiver), eq(mTestFilter), eq(null), any(),
                eq(0));

    }

    @Test
    @SdkSuppress(minSdkVersion = 29, maxSdkVersion = 32)
    public void testRegisterReceiverPermissionNotGrantedApi26() {
        InstrumentationRegistry
                .getInstrumentation().getUiAutomation().adoptShellPermissionIdentity();
        assertThrows(RuntimeException.class,
                () -> ContextCompat.registerReceiver(mContext,
                        mTestReceiver, mTestFilter, ContextCompat.RECEIVER_NOT_EXPORTED));
    }

    @Test
    @SdkSuppress(maxSdkVersion = 25)
    public void testRegisterReceiver() {
        Context spyContext = spy(mContext);

        ContextCompat.registerReceiver(spyContext, mTestReceiver, mTestFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
        verify(spyContext).registerReceiver(eq(mTestReceiver), eq(mTestFilter), eq(mPermission),
                any());

        ContextCompat.registerReceiver(spyContext, mTestReceiver, mTestFilter,
                ContextCompat.RECEIVER_EXPORTED);
        verify(spyContext).registerReceiver(eq(mTestReceiver), eq(mTestFilter), eq(null), any());
    }

    @Test
    public void testRegisterReceiver_mergedPermission_hasSignatureProtectionLevel()
            throws Exception {
        // Packages registering unexported runtime receivers on pre-T devices will have their
        // receiver protected by a permission; to ensure other apps on the device cannot send a
        // broadcast to these receivers, the permission must be declared with the signature
        // protectionLevel.
        PermissionInfo permissionInfo =
                mContext.getPackageManager().getPermissionInfo(mPermission, 0);

        assertEquals("The permission guarding unexported runtime receivers must have the "
                + "signature protectionLevel.", PermissionInfo.PROTECTION_SIGNATURE,
                permissionInfo.protectionLevel);
    }

    @Test(expected = NullPointerException.class)
    public void testCheckSelfPermissionNull() {
        ContextCompat.checkSelfPermission(mContext, null);
    }

    @Test
    public void testCheckSelfPermission() {
        assertEquals("Vibrate permission granted", PackageManager.PERMISSION_GRANTED,
                ContextCompat.checkSelfPermission(mContext,
                        android.Manifest.permission.VIBRATE));
        assertEquals("Wake lock permission granted", PackageManager.PERMISSION_GRANTED,
                ContextCompat.checkSelfPermission(mContext,
                        android.Manifest.permission.WAKE_LOCK));

        // The following permissions (normal and dangerous) are expected to be denied as they are
        // not declared in our manifest.
        assertEquals("Access network state permission denied", PackageManager.PERMISSION_DENIED,
                ContextCompat.checkSelfPermission(mContext,
                        android.Manifest.permission.ACCESS_NETWORK_STATE));
        assertEquals("Bluetooth permission denied", PackageManager.PERMISSION_DENIED,
                ContextCompat.checkSelfPermission(mContext,
                        android.Manifest.permission.BLUETOOTH));
        assertEquals("Call phone permission denied", PackageManager.PERMISSION_DENIED,
                ContextCompat.checkSelfPermission(mContext,
                        android.Manifest.permission.CALL_PHONE));
        assertEquals("Delete packages permission denied", PackageManager.PERMISSION_DENIED,
                ContextCompat.checkSelfPermission(mContext,
                        android.Manifest.permission.DELETE_PACKAGES));
    }

    @Test
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    public void testCheckSelfPermissionNotificationPermission() {
        if (BuildCompat.isAtLeastT()) {
            assertEquals(
                    mContext.checkCallingPermission(Manifest.permission.POST_NOTIFICATIONS),
                    ContextCompat.checkSelfPermission(
                            mContext,
                            Manifest.permission.POST_NOTIFICATIONS));
        } else {
            assertEquals("Notification permission allowed by default on devices <= SDK 32",
                    NotificationManagerCompat.from(mContext).areNotificationsEnabled()
                            ? PackageManager.PERMISSION_GRANTED : PackageManager.PERMISSION_DENIED,
                    ContextCompat.checkSelfPermission(mContext,
                            Manifest.permission.POST_NOTIFICATIONS));
        }
    }

    @Test
    public void testGetDisplayFromActivity() {
        final Display actualDisplay = ContextCompat.getDisplay(mContext);
        if (Build.VERSION.SDK_INT >= 30) {
            assertEquals(mContext.getDisplay(), actualDisplay);
        } else {
            final WindowManager windowManager =
                    (WindowManager) mContext.getSystemService(WINDOW_SERVICE);
            assertEquals(actualDisplay, windowManager.getDefaultDisplay());
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 17)
    public void testGetDisplayFromDisplayContext() {
        final DisplayManagerCompat displayManagerCompat = DisplayManagerCompat
                .getInstance(mContext);
        final Display defaultDisplay =  displayManagerCompat.getDisplay(Display.DEFAULT_DISPLAY);
        final Context displayContext = mContext.createDisplayContext(defaultDisplay);

        assertEquals(ContextCompat.getDisplay(displayContext), defaultDisplay);
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    public void testGetDisplayFromWindowContext() {
        final Context windowContext = mContext.createWindowContext(TYPE_APPLICATION_OVERLAY, null);

        assertEquals(ContextCompat.getDisplay(windowContext), windowContext.getDisplay());
    }

    @Test
    public void testGetDisplayFromApplication() {
        final Context applicationContext = ApplicationProvider.getApplicationContext();
        final Context spyContext = spy(applicationContext);
        final Display actualDisplay = ContextCompat.getDisplay(spyContext);

        if (Build.VERSION.SDK_INT >= 30) {
            verify(spyContext).getSystemService(eq(DisplayManager.class));

            final Display defaultDisplay = DisplayManagerCompat.getInstance(spyContext)
                    .getDisplay(Display.DEFAULT_DISPLAY);
            assertEquals(defaultDisplay, actualDisplay);
        } else {
            final WindowManager windowManager =
                    (WindowManager) spyContext.getSystemService(WINDOW_SERVICE);
            // Don't verify if the returned display is the same instance because Application is
            // not a DisplayContext and the framework always create a fallback Display for
            // the Context that not associated with a Display.
            assertEquals(windowManager.getDefaultDisplay().getDisplayId(),
                    actualDisplay.getDisplayId());
        }
    }
}
