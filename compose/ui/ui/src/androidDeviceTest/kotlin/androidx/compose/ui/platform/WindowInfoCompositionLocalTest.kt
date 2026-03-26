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

package androidx.compose.ui.platform

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.view.Display
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.setFocusableContent
import androidx.compose.ui.graphics.toComposeIntRect
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.window.layout.WindowMetricsCalculator
import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class WindowInfoCompositionLocalTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>(StandardTestDispatcher())

    @Test
    fun windowIsFocused_onLaunch() {
        // Arrange.
        lateinit var windowInfo: WindowInfo
        rule.setContent {
            BasicText("Main Window")
            windowInfo = LocalWindowInfo.current
        }

        // Act.
        rule.waitForIdle()

        // Assert.
        rule.waitUntil(5000) { windowInfo.isWindowFocused }
        assertThat(windowInfo.isWindowFocused).isTrue()
    }

    @Test
    fun mainWindowIsNotFocused_whenPopupIsVisible() {
        // Arrange.
        lateinit var mainWindowInfo: WindowInfo
        lateinit var popupWindowInfo: WindowInfo
        val showPopup = mutableStateOf(false)
        rule.setContent {
            BasicText("Main Window")
            mainWindowInfo = LocalWindowInfo.current
            if (showPopup.value) {
                Popup(
                    properties = PopupProperties(focusable = true),
                    onDismissRequest = { showPopup.value = false },
                ) {
                    BasicText("Popup Window")
                    popupWindowInfo = LocalWindowInfo.current
                }
            }
        }

        // Act.
        rule.runOnIdle { showPopup.value = true }

        // Assert.
        rule.waitForIdle()
        rule.waitUntil(5000) { !mainWindowInfo.isWindowFocused && popupWindowInfo.isWindowFocused }
        assertThat(mainWindowInfo.isWindowFocused).isFalse()
        assertThat(popupWindowInfo.isWindowFocused).isTrue()
    }

    @Test
    fun windowIsFocused_whenPopupIsDismissed() {
        // Arrange.
        lateinit var mainWindowInfo: WindowInfo
        lateinit var popupWindowInfo: WindowInfo
        val showPopup = mutableStateOf(false)
        rule.setContent {
            BasicText(text = "Main Window")
            mainWindowInfo = LocalWindowInfo.current
            if (showPopup.value) {
                Popup(
                    properties = PopupProperties(focusable = true),
                    onDismissRequest = { showPopup.value = false },
                ) {
                    BasicText(text = "Popup Window")
                    popupWindowInfo = LocalWindowInfo.current
                }
            }
        }
        rule.runOnIdle { showPopup.value = true }
        rule.waitForIdle()
        rule.waitUntil(5000) { popupWindowInfo.isWindowFocused }
        assertThat(popupWindowInfo.isWindowFocused).isTrue()

        // Act.
        rule.runOnIdle { showPopup.value = false }

        // Assert.
        rule.waitForIdle()
        rule.waitUntil(5000) { mainWindowInfo.isWindowFocused }
        assertThat(mainWindowInfo.isWindowFocused).isTrue()
    }

    @Test
    fun mainWindowIsNotFocused_whenDialogIsVisible() {
        // Arrange.
        lateinit var mainWindowInfo: WindowInfo
        lateinit var dialogWindowInfo: WindowInfo
        val showDialog = mutableStateOf(false)
        rule.setContent {
            BasicText("Main Window")
            mainWindowInfo = LocalWindowInfo.current
            if (showDialog.value) {
                Dialog(onDismissRequest = { showDialog.value = false }) {
                    BasicText("Popup Window")
                    dialogWindowInfo = LocalWindowInfo.current
                }
            }
        }

        // Act.
        rule.runOnIdle { showDialog.value = true }

        // Assert.
        rule.waitForIdle()
        rule.waitUntil(5000) { !mainWindowInfo.isWindowFocused && dialogWindowInfo.isWindowFocused }
        assertThat(mainWindowInfo.isWindowFocused).isFalse()
        assertThat(dialogWindowInfo.isWindowFocused).isTrue()
    }

    @Test
    fun windowIsFocused_whenDialogIsDismissed() {
        // Arrange.
        lateinit var mainWindowInfo: WindowInfo
        lateinit var dialogWindowInfo: WindowInfo
        val showDialog = mutableStateOf(false)
        rule.setContent {
            BasicText(text = "Main Window")
            mainWindowInfo = LocalWindowInfo.current
            if (showDialog.value) {
                Dialog(onDismissRequest = { showDialog.value = false }) {
                    BasicText(text = "Popup Window")
                    dialogWindowInfo = LocalWindowInfo.current
                }
            }
        }
        rule.runOnIdle { showDialog.value = true }
        rule.waitForIdle()
        rule.waitUntil(5000) { dialogWindowInfo.isWindowFocused }
        assertThat(dialogWindowInfo.isWindowFocused).isTrue()
        assertThat(mainWindowInfo.isWindowFocused).isFalse()

        // Act.
        rule.runOnIdle { showDialog.value = false }

        // Assert.
        rule.waitForIdle()
        rule.waitUntil(5000) { mainWindowInfo.isWindowFocused }
        assertThat(mainWindowInfo.isWindowFocused).isTrue()
    }

    @Test
    fun windowInfo_providesKeyModifiers() {
        lateinit var ownerView: View
        var keyModifiers = PointerKeyboardModifiers(0)

        rule.setFocusableContent {
            ownerView = LocalView.current
            keyModifiers = LocalWindowInfo.current.keyboardModifiers
            Box(Modifier.focusTarget())
        }
        assertThat(keyModifiers.packedValue).isEqualTo(0)

        (rule as AndroidComposeTestRule<*, *>).runOnUiThread { ownerView.requestFocus() }

        rule.runOnIdle {
            val ctrlPressed =
                KeyEvent(
                    0,
                    0,
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_CTRL_LEFT,
                    0,
                    KeyEvent.META_CTRL_ON,
                )
            ownerView.dispatchKeyEvent(ctrlPressed)
        }

        rule.waitForIdle()
        assertThat(keyModifiers.packedValue).isEqualTo(KeyEvent.META_CTRL_ON)

        rule.runOnIdle {
            val altAndCtrlPressed =
                KeyEvent(
                    0,
                    0,
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_ALT_LEFT,
                    0,
                    KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON,
                )
            ownerView.dispatchKeyEvent(altAndCtrlPressed)
        }

        rule.waitForIdle()
        assertThat(keyModifiers.packedValue)
            .isEqualTo(KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON)

        rule.runOnIdle {
            val altUnpressed =
                KeyEvent(
                    0,
                    0,
                    KeyEvent.ACTION_UP,
                    KeyEvent.KEYCODE_ALT_LEFT,
                    0,
                    KeyEvent.META_CTRL_ON,
                )
            ownerView.dispatchKeyEvent(altUnpressed)
        }

        rule.waitForIdle()
        assertThat(keyModifiers.packedValue).isEqualTo(KeyEvent.META_CTRL_ON)

        rule.runOnIdle {
            val ctrlUnpressed = KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT, 0, 0)
            ownerView.dispatchKeyEvent(ctrlUnpressed)
        }

        rule.waitForIdle()
        assertThat(keyModifiers.packedValue).isEqualTo(0)
    }

    @Test
    fun windowInfo_containerSize() {
        // Arrange.
        var containerSize = IntSize.Zero
        var containerDpSize = DpSize.Zero
        var recompositions = 0
        var density = Density(1f)
        rule.setContent {
            BasicText("Main Window")
            val windowInfo = LocalWindowInfo.current
            containerSize = windowInfo.containerSize
            containerDpSize = windowInfo.containerDpSize
            density = LocalDensity.current
            recompositions++
        }

        // Act.
        rule.waitForIdle()

        val expectedWindowSize =
            WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(rule.activity)
                .bounds
                .toComposeIntRect()
                .size

        val expectedWindowDpSize = with(density) { expectedWindowSize.toSize().toDpSize() }

        // Assert.
        assertThat(containerSize).isEqualTo(expectedWindowSize)
        assertThat(containerDpSize).isEqualTo(expectedWindowDpSize)
        assertThat(recompositions).isEqualTo(1)
    }

    // Regression test for b/360343819
    @Test
    fun windowInfo_containerSize_viewCreatedWithApplicationContext() {
        // Arrange.
        var containerSize = IntSize.Zero
        var drawCount = 0
        var recompositions = 0
        val activity = rule.activity

        rule.runOnUiThread {
            val composeView =
                ComposeView(activity.applicationContext).apply {
                    setContent {
                        BasicText("Main Window", Modifier.drawBehind { drawCount++ })
                        val windowInfo = LocalWindowInfo.current
                        containerSize = windowInfo.containerSize
                        recompositions++
                    }
                }

            val frameLayout = FrameLayout(activity).apply { addView(composeView) }

            rule.activity.setContentView(frameLayout)
        }

        rule.waitUntil { drawCount == 1 }

        val expectedWindowSize =
            WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(activity)
                .bounds
                .toComposeIntRect()

        // For applicationContext we cannot accurately calculate window size (there will be
        // differences
        // in terms of including / excluding some insets), so just roughly assert we are in the
        // correct range
        val widthRange =
            Range.closed(
                (expectedWindowSize.width * 0.8).roundToInt(),
                (expectedWindowSize.width * 1.2).roundToInt(),
            )
        val heightRange =
            Range.closed(
                (expectedWindowSize.height * 0.8).roundToInt(),
                (expectedWindowSize.height * 1.2).roundToInt(),
            )

        // Assert.
        assertThat(containerSize.width).isIn(widthRange)
        assertThat(containerSize.height).isIn(heightRange)
        assertThat(recompositions).isEqualTo(1)
    }

    // Regression test for b/449198972
    @Test
    fun windowInfo_containerSize_viewCreatedWithWrappedApplicationContext() {
        // Arrange.
        var containerSize = IntSize.Zero
        var drawCount = 0
        var recompositions = 0
        val activity = rule.activity

        rule.runOnUiThread {
            val composeView =
                ComposeView(ContextWrapper(activity.applicationContext)).apply {
                    setContent {
                        BasicText("Main Window", Modifier.drawBehind { drawCount++ })
                        val windowInfo = LocalWindowInfo.current
                        containerSize = windowInfo.containerSize
                        recompositions++
                    }
                }

            val frameLayout = FrameLayout(activity).apply { addView(composeView) }

            rule.activity.setContentView(frameLayout)
        }

        rule.waitUntil { drawCount == 1 }

        val expectedWindowSize =
            WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(activity)
                .bounds
                .toComposeIntRect()

        // For applicationContext we cannot accurately calculate window size (there will be
        // differences
        // in terms of including / excluding some insets), so just roughly assert we are in the
        // correct range
        val widthRange =
            Range.closed(
                (expectedWindowSize.width * 0.8).roundToInt(),
                (expectedWindowSize.width * 1.2).roundToInt(),
            )
        val heightRange =
            Range.closed(
                (expectedWindowSize.height * 0.8).roundToInt(),
                (expectedWindowSize.height * 1.2).roundToInt(),
            )

        // Assert.
        assertThat(containerSize.width).isIn(widthRange)
        assertThat(containerSize.height).isIn(heightRange)
        assertThat(recompositions).isEqualTo(1)
    }

    @Test
    fun windowInfo_containerSize_viewCreatedWithCustomContext() {
        // Arrange.
        var containerSize = IntSize.Zero
        var drawCount = 0
        var recompositions = 0
        val activity = rule.activity

        rule.runOnUiThread {
            val composeView =
                ComposeView(CustomWrappedContext(activity)).apply {
                    setContent {
                        BasicText("Main Window", Modifier.drawBehind { drawCount++ })
                        val windowInfo = LocalWindowInfo.current
                        containerSize = windowInfo.containerSize
                        recompositions++
                    }
                }

            val frameLayout = FrameLayout(activity).apply { addView(composeView) }

            rule.activity.setContentView(frameLayout)
        }

        rule.waitUntil { drawCount == 1 }

        val expectedWindowSize =
            WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(activity)
                .bounds
                .toComposeIntRect()

        // For custom context we cannot accurately calculate window size (there will be
        // differences
        // in terms of including / excluding some insets), so just roughly assert we are in the
        // correct range
        val widthRange =
            Range.closed(
                (expectedWindowSize.width * 0.8).roundToInt(),
                (expectedWindowSize.width * 1.2).roundToInt(),
            )
        val heightRange =
            Range.closed(
                (expectedWindowSize.height * 0.8).roundToInt(),
                (expectedWindowSize.height * 1.2).roundToInt(),
            )

        // Assert.
        assertThat(containerSize.width).isIn(widthRange)
        assertThat(containerSize.height).isIn(heightRange)
        assertThat(recompositions).isEqualTo(1)
    }

    @Test
    fun containerSizeUpdatesWhenDeviceSizeChanges() {
        var containerSize = IntSize.Zero
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            containerSize = LocalWindowInfo.current.containerSize
        }
        @OptIn(ExperimentalComposeViewContextApi::class)
        rule.runOnIdle {
            val composeViewContext = view.findViewTreeComposeViewContext()
            val resources = rule.activity.resources
            val configuration = Configuration(resources.configuration)
            configuration.screenWidthDp = (1000 / resources.displayMetrics.density).roundToInt()
            configuration.screenHeightDp = (2000 / resources.displayMetrics.density).roundToInt()
            configuration.smallestScreenWidthDp = 1000
            composeViewContext?.testWindowSize = IntSize(1000, 2000)
            composeViewContext?.onConfigurationChanged(configuration)
        }

        rule.runOnIdle { assertThat(containerSize).isEqualTo(IntSize(1000, 2000)) }
    }
}

/**
 * Context that wraps another [Activity], without using [android.content.ContextWrapper]. This is a
 * bit strange, but it's not guaranteed that an activity or application will have a valid instance
 * of those inside a wrapped context chain.
 */
@Suppress("DEPRECATION", "NewApi", "UnspecifiedRegisterReceiverFlag")
private class CustomWrappedContext(private val base: Activity) : Context() {
    override fun getAssets(): AssetManager = base.assets

    override fun getResources(): Resources = base.resources

    override fun getPackageManager(): PackageManager = base.packageManager

    override fun getContentResolver(): ContentResolver = base.contentResolver

    override fun getMainLooper(): Looper = base.mainLooper

    override fun getApplicationContext(): Context = base.applicationContext

    override fun setTheme(resid: Int) {
        base.setTheme(resid)
    }

    override fun getTheme(): Resources.Theme = base.theme

    override fun getClassLoader(): ClassLoader = base.classLoader

    override fun getPackageName(): String = base.packageName

    override fun getApplicationInfo(): ApplicationInfo = base.applicationInfo

    override fun getPackageResourcePath(): String = base.packageResourcePath

    override fun getPackageCodePath(): String = base.packageCodePath

    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences? =
        base.getSharedPreferences(name, mode)

    override fun moveSharedPreferencesFrom(sourceContext: Context?, name: String?): Boolean =
        base.moveSharedPreferencesFrom(sourceContext, name)

    override fun deleteSharedPreferences(name: String?): Boolean =
        base.deleteSharedPreferences(name)

    override fun openFileInput(name: String?): FileInputStream? = base.openFileInput(name)

    override fun openFileOutput(name: String?, mode: Int): FileOutputStream? =
        base.openFileOutput(name, mode)

    override fun deleteFile(name: String?): Boolean = base.deleteFile(name)

    override fun getFileStreamPath(name: String?): File? = base.getFileStreamPath(name)

    override fun getDataDir(): File? = base.dataDir

    override fun getFilesDir(): File? = base.filesDir

    override fun getNoBackupFilesDir(): File? = base.noBackupFilesDir

    override fun getExternalFilesDir(type: String?): File? = base.getExternalFilesDir(type)

    override fun getExternalFilesDirs(type: String?): Array<out File?>? =
        base.getExternalFilesDirs(type)

    override fun getObbDir(): File? = base.obbDir

    override fun getObbDirs(): Array<out File?>? = base.obbDirs

    override fun getCacheDir(): File? = base.cacheDir

    override fun getCodeCacheDir(): File? = base.codeCacheDir

    override fun getExternalCacheDir(): File? = base.externalCacheDir

    override fun getExternalCacheDirs(): Array<out File?>? = base.externalCacheDirs

    @Deprecated("Deprecated in Java")
    override fun getExternalMediaDirs(): Array<out File?>? = base.externalMediaDirs

    override fun fileList(): Array<out String?>? = base.fileList()

    override fun getDir(name: String?, mode: Int): File? = base.getDir(name, mode)

    override fun openOrCreateDatabase(
        name: String?,
        mode: Int,
        factory: SQLiteDatabase.CursorFactory?,
    ): SQLiteDatabase? = base.openOrCreateDatabase(name, mode, factory)

    override fun openOrCreateDatabase(
        name: String?,
        mode: Int,
        factory: SQLiteDatabase.CursorFactory?,
        errorHandler: DatabaseErrorHandler?,
    ): SQLiteDatabase? = base.openOrCreateDatabase(name, mode, factory, errorHandler)

    override fun moveDatabaseFrom(sourceContext: Context?, name: String?): Boolean =
        base.moveDatabaseFrom(sourceContext, name)

    override fun deleteDatabase(name: String?): Boolean = base.deleteDatabase(name)

    override fun getDatabasePath(name: String?): File? = base.getDatabasePath(name)

    override fun databaseList(): Array<out String?>? = base.databaseList()

    @Deprecated("Deprecated in Java") override fun getWallpaper(): Drawable? = base.wallpaper

    @Deprecated("Deprecated in Java") override fun peekWallpaper(): Drawable? = base.peekWallpaper()

    @Deprecated("Deprecated in Java")
    override fun getWallpaperDesiredMinimumWidth(): Int = base.wallpaperDesiredMinimumWidth

    @Deprecated("Deprecated in Java")
    override fun getWallpaperDesiredMinimumHeight(): Int = base.wallpaperDesiredMinimumHeight

    @Deprecated("Deprecated in Java")
    override fun setWallpaper(bitmap: Bitmap?) {
        base.setWallpaper(bitmap)
    }

    @Deprecated("Deprecated in Java")
    override fun setWallpaper(data: InputStream?) {
        base.setWallpaper(data)
    }

    @Deprecated("Deprecated in Java")
    override fun clearWallpaper() {
        base.clearWallpaper()
    }

    override fun startActivity(intent: Intent?) {
        base.startActivity(intent)
    }

    override fun startActivity(intent: Intent?, options: Bundle?) {
        base.startActivity(intent, options)
    }

    override fun startActivities(intents: Array<out Intent?>?) {
        base.startActivities(intents)
    }

    override fun startActivities(intents: Array<out Intent?>?, options: Bundle?) {
        base.startActivities(intents, options)
    }

    override fun startIntentSender(
        intent: IntentSender?,
        fillInIntent: Intent?,
        flagsMask: Int,
        flagsValues: Int,
        extraFlags: Int,
    ) {
        base.startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags)
    }

    override fun startIntentSender(
        intent: IntentSender?,
        fillInIntent: Intent?,
        flagsMask: Int,
        flagsValues: Int,
        extraFlags: Int,
        options: Bundle?,
    ) {
        base.startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags, options)
    }

    override fun sendBroadcast(intent: Intent?) {
        base.sendBroadcast(intent)
    }

    override fun sendBroadcast(intent: Intent?, receiverPermission: String?) {
        base.sendBroadcast(intent, receiverPermission)
    }

    override fun sendOrderedBroadcast(intent: Intent?, receiverPermission: String?) {
        base.sendOrderedBroadcast(intent, receiverPermission)
    }

    override fun sendOrderedBroadcast(
        intent: Intent,
        receiverPermission: String?,
        resultReceiver: BroadcastReceiver?,
        scheduler: Handler?,
        initialCode: Int,
        initialData: String?,
        initialExtras: Bundle?,
    ) {
        base.sendOrderedBroadcast(
            intent,
            receiverPermission,
            resultReceiver,
            scheduler,
            initialCode,
            initialData,
            initialExtras,
        )
    }

    override fun sendBroadcastAsUser(intent: Intent?, user: UserHandle?) {
        base.sendBroadcastAsUser(intent, user)
    }

    override fun sendBroadcastAsUser(
        intent: Intent?,
        user: UserHandle?,
        receiverPermission: String?,
    ) {
        base.sendBroadcastAsUser(intent, user, receiverPermission)
    }

    override fun sendOrderedBroadcastAsUser(
        intent: Intent?,
        user: UserHandle?,
        receiverPermission: String?,
        resultReceiver: BroadcastReceiver?,
        scheduler: Handler?,
        initialCode: Int,
        initialData: String?,
        initialExtras: Bundle?,
    ) {
        base.sendOrderedBroadcastAsUser(
            intent,
            user,
            receiverPermission,
            resultReceiver,
            scheduler,
            initialCode,
            initialData,
            initialExtras,
        )
    }

    @Deprecated("Deprecated in Java")
    override fun sendStickyBroadcast(intent: Intent?) {
        base.sendStickyBroadcast(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun sendStickyOrderedBroadcast(
        intent: Intent?,
        resultReceiver: BroadcastReceiver?,
        scheduler: Handler?,
        initialCode: Int,
        initialData: String?,
        initialExtras: Bundle?,
    ) {
        base.sendStickyOrderedBroadcast(
            intent,
            resultReceiver,
            scheduler,
            initialCode,
            initialData,
            initialExtras,
        )
    }

    @Deprecated("Deprecated in Java")
    override fun removeStickyBroadcast(intent: Intent?) {
        base.removeStickyBroadcast(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun sendStickyBroadcastAsUser(intent: Intent?, user: UserHandle?) {
        base.sendStickyBroadcastAsUser(intent, user)
    }

    @Deprecated("Deprecated in Java")
    override fun sendStickyOrderedBroadcastAsUser(
        intent: Intent?,
        user: UserHandle?,
        resultReceiver: BroadcastReceiver?,
        scheduler: Handler?,
        initialCode: Int,
        initialData: String?,
        initialExtras: Bundle?,
    ) {
        base.sendStickyOrderedBroadcastAsUser(
            intent,
            user,
            resultReceiver,
            scheduler,
            initialCode,
            initialData,
            initialExtras,
        )
    }

    @Deprecated("Deprecated in Java")
    override fun removeStickyBroadcastAsUser(intent: Intent?, user: UserHandle?) {
        base.removeStickyBroadcastAsUser(intent, user)
    }

    override fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?): Intent? =
        base.registerReceiver(receiver, filter)

    override fun registerReceiver(
        receiver: BroadcastReceiver?,
        filter: IntentFilter?,
        flags: Int,
    ): Intent? = base.registerReceiver(receiver, filter, flags)

    override fun registerReceiver(
        receiver: BroadcastReceiver?,
        filter: IntentFilter?,
        broadcastPermission: String?,
        scheduler: Handler?,
    ): Intent? = base.registerReceiver(receiver, filter, broadcastPermission, scheduler)

    override fun registerReceiver(
        receiver: BroadcastReceiver?,
        filter: IntentFilter?,
        broadcastPermission: String?,
        scheduler: Handler?,
        flags: Int,
    ): Intent? = base.registerReceiver(receiver, filter, broadcastPermission, scheduler, flags)

    override fun unregisterReceiver(receiver: BroadcastReceiver?) {
        base.unregisterReceiver(receiver)
    }

    override fun startService(service: Intent?): ComponentName? = base.startService(service)

    override fun startForegroundService(service: Intent?): ComponentName? =
        base.startForegroundService(service)

    override fun stopService(service: Intent?): Boolean = base.stopService(service)

    override fun bindService(service: Intent, conn: ServiceConnection, flags: Int): Boolean =
        base.bindService(service, conn, flags)

    override fun unbindService(conn: ServiceConnection) {
        base.unbindService(conn)
    }

    override fun startInstrumentation(
        className: ComponentName,
        profileFile: String?,
        arguments: Bundle?,
    ): Boolean = base.startInstrumentation(className, profileFile, arguments)

    override fun getSystemService(name: String): Any? = base.getSystemService(name)

    override fun getSystemServiceName(serviceClass: Class<*>): String? =
        base.getSystemServiceName(serviceClass)

    override fun checkPermission(permission: String, pid: Int, uid: Int): Int =
        base.checkPermission(permission, pid, uid)

    override fun checkCallingPermission(permission: String): Int =
        base.checkCallingPermission(permission)

    override fun checkCallingOrSelfPermission(permission: String): Int =
        base.checkCallingOrSelfPermission(permission)

    override fun checkSelfPermission(permission: String): Int = base.checkSelfPermission(permission)

    override fun enforcePermission(permission: String, pid: Int, uid: Int, message: String?) {
        base.enforcePermission(permission, pid, uid, message)
    }

    override fun enforceCallingPermission(permission: String, message: String?) {
        base.enforceCallingPermission(permission, message)
    }

    override fun enforceCallingOrSelfPermission(permission: String, message: String?) {
        base.enforceCallingOrSelfPermission(permission, message)
    }

    override fun grantUriPermission(toPackage: String?, uri: Uri?, modeFlags: Int) {
        base.grantUriPermission(toPackage, uri, modeFlags)
    }

    override fun revokeUriPermission(uri: Uri?, modeFlags: Int) {
        base.revokeUriPermission(uri, modeFlags)
    }

    override fun revokeUriPermission(toPackage: String?, uri: Uri?, modeFlags: Int) {
        base.revokeUriPermission(toPackage, uri, modeFlags)
    }

    override fun checkUriPermission(uri: Uri?, pid: Int, uid: Int, modeFlags: Int): Int =
        base.checkUriPermission(uri, pid, uid, modeFlags)

    override fun checkCallingUriPermission(uri: Uri?, modeFlags: Int): Int =
        base.checkCallingUriPermission(uri, modeFlags)

    override fun checkCallingOrSelfUriPermission(uri: Uri?, modeFlags: Int): Int =
        base.checkCallingOrSelfUriPermission(uri, modeFlags)

    override fun checkUriPermission(
        uri: Uri?,
        readPermission: String?,
        writePermission: String?,
        pid: Int,
        uid: Int,
        modeFlags: Int,
    ): Int = base.checkUriPermission(uri, readPermission, writePermission, pid, uid, modeFlags)

    override fun enforceUriPermission(
        uri: Uri?,
        pid: Int,
        uid: Int,
        modeFlags: Int,
        message: String?,
    ) {
        base.enforceUriPermission(uri, pid, uid, modeFlags, message)
    }

    override fun enforceCallingUriPermission(uri: Uri?, modeFlags: Int, message: String?) {
        base.enforceCallingUriPermission(uri, modeFlags, message)
    }

    override fun enforceCallingOrSelfUriPermission(uri: Uri?, modeFlags: Int, message: String?) {
        base.enforceCallingOrSelfUriPermission(uri, modeFlags, message)
    }

    override fun enforceUriPermission(
        uri: Uri?,
        readPermission: String?,
        writePermission: String?,
        pid: Int,
        uid: Int,
        modeFlags: Int,
        message: String?,
    ) {
        base.enforceUriPermission(
            uri,
            readPermission,
            writePermission,
            pid,
            uid,
            modeFlags,
            message,
        )
    }

    override fun createPackageContext(packageName: String?, flags: Int): Context? =
        base.createPackageContext(packageName, flags)

    override fun createContextForSplit(splitName: String?): Context? =
        base.createContextForSplit(splitName)

    override fun createConfigurationContext(overrideConfiguration: Configuration): Context? =
        base.createConfigurationContext(overrideConfiguration)

    override fun createDisplayContext(display: Display): Context? =
        base.createDisplayContext(display)

    override fun createDeviceProtectedStorageContext(): Context? =
        base.createDeviceProtectedStorageContext()

    override fun isDeviceProtectedStorage(): Boolean = base.isDeviceProtectedStorage

    override fun getDeviceId(): Int = base.deviceId

    // Optional overrides

    // This is @hide on 30, so we cannot reference base.isUiContext, but overriding will work. Just
    // return true always, since we are wrapping an Activity anyway (which is a UI context)
    override fun isUiContext(): Boolean = true
}
