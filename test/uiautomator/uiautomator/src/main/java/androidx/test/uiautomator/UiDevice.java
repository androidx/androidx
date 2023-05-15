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

package androidx.test.uiautomator;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.app.Service;
import android.app.UiAutomation;
import android.app.UiAutomation.AccessibilityEventFilter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * UiDevice provides access to state information about the device.
 * You can also use this class to simulate user actions on the device,
 * such as pressing the d-pad or pressing the Home and Menu buttons.
 */
public class UiDevice implements Searchable {
    private static final String TAG = UiDevice.class.getSimpleName();

    // Use a short timeout after HOME or BACK key presses, as no events might be generated if
    // already on the home page or if there is nothing to go back to.
    private static final long KEY_PRESS_EVENT_TIMEOUT = 1_000; // ms
    private static final long ROTATION_TIMEOUT = 1_000; // ms

    // Singleton instance.
    private static UiDevice sInstance;

    private final Instrumentation mInstrumentation;
    private final QueryController mQueryController;
    private final InteractionController mInteractionController;
    private final DisplayManager mDisplayManager;
    private final WaitMixin<UiDevice> mWaitMixin = new WaitMixin<>(this);

    // Track accessibility service flags to determine when the underlying connection has changed.
    private int mCachedServiceFlags = -1;
    private boolean mCompressed = false;

    // Lazily created UI context per display, used to access UI components/configurations.
    private final Map<Integer, Context> mUiContexts = new HashMap<>();

    // Track registered UiWatchers, and whether currently in a UiWatcher execution.
    private final Map<String, UiWatcher> mWatchers = new LinkedHashMap<>();
    private final List<String> mWatchersTriggers = new ArrayList<>();
    private boolean mInWatcherContext = false;

    /** Private constructor. Clients should use {@link UiDevice#getInstance(Instrumentation)}. */
    UiDevice(Instrumentation instrumentation) {
        mInstrumentation = instrumentation;
        mQueryController = new QueryController(this);
        mInteractionController = new InteractionController(this);
        mDisplayManager = (DisplayManager) instrumentation.getContext().getSystemService(
                Service.DISPLAY_SERVICE);
    }

    boolean isInWatcherContext() {
        return mInWatcherContext;
    }

    /**
     * Returns a UiObject which represents a view that matches the specified selector criteria.
     *
     * @param selector
     * @return UiObject object
     */
    @NonNull
    public UiObject findObject(@NonNull UiSelector selector) {
        return new UiObject(this, selector);
    }

    /** Returns whether there is a match for the given {@code selector} criteria. */
    @Override
    public boolean hasObject(@NonNull BySelector selector) {
        AccessibilityNodeInfo node = ByMatcher.findMatch(this, selector, getWindowRoots());
        if (node != null) {
            node.recycle();
            return true;
        }
        return false;
    }

    /**
     * Returns the first object to match the {@code selector} criteria,
     * or null if no matching objects are found.
     */
    @Override
    @SuppressLint("UnknownNullness") // Avoid unnecessary null checks from nullable testing APIs.
    public UiObject2 findObject(@NonNull BySelector selector) {
        AccessibilityNodeInfo node = ByMatcher.findMatch(this, selector, getWindowRoots());
        if (node == null) {
            Log.d(TAG, String.format("Node not found with selector: %s.", selector));
            return null;
        }
        return new UiObject2(this, selector, node);
    }

    /** Returns all objects that match the {@code selector} criteria. */
    @Override
    @NonNull
    public List<UiObject2> findObjects(@NonNull BySelector selector) {
        List<UiObject2> ret = new ArrayList<>();
        for (AccessibilityNodeInfo node : ByMatcher.findMatches(this, selector, getWindowRoots())) {
            ret.add(new UiObject2(this, selector, node));
        }

        return ret;
    }


    /**
     * Waits for given the {@code condition} to be met.
     *
     * @param condition The {@link Condition} to evaluate.
     * @param timeout Maximum amount of time to wait in milliseconds.
     * @return The final result returned by the {@code condition}, or null if the {@code condition}
     * was not met before the {@code timeout}.
     */
    public <U> U wait(@NonNull Condition<? super UiDevice, U> condition, long timeout) {
        Log.d(TAG, String.format("Waiting %dms for %s.", timeout, condition));
        return mWaitMixin.wait(condition, timeout);
    }

    /**
     * Performs the provided {@code action} and waits for the {@code condition} to be met.
     *
     * @param action The {@link Runnable} action to perform.
     * @param condition The {@link EventCondition} to evaluate.
     * @param timeout Maximum amount of time to wait in milliseconds.
     * @return The final result returned by the condition.
     */
    public <U> U performActionAndWait(@NonNull Runnable action,
            @NonNull EventCondition<U> condition, long timeout) {
        AccessibilityEvent event = null;
        Log.d(TAG, String.format("Performing action %s and waiting %dms for %s.", action, timeout,
                condition));
        try {
            event = getUiAutomation().executeAndWaitForEvent(
                    action, condition, timeout);
        } catch (TimeoutException e) {
            // Ignore
            Log.w(TAG, String.format("Timed out waiting %dms on the condition.", timeout), e);
        }

        if (event != null) {
            event.recycle();
        }

        return condition.getResult();
    }

    /**
     * Enables or disables layout hierarchy compression.
     *
     * If compression is enabled, the layout hierarchy derived from the Acessibility
     * framework will only contain nodes that are important for uiautomator
     * testing. Any unnecessary surrounding layout nodes that make viewing
     * and searching the hierarchy inefficient are removed.
     *
     * @param compressed true to enable compression; else, false to disable
     * @deprecated Typo in function name, should use {@link #setCompressedLayoutHierarchy(boolean)}
     * instead.
     */
    @Deprecated
    public void setCompressedLayoutHeirarchy(boolean compressed) {
        this.setCompressedLayoutHierarchy(compressed);
    }

    /**
     * Enables or disables layout hierarchy compression.
     *
     * If compression is enabled, the layout hierarchy derived from the Accessibility
     * framework will only contain nodes that are important for uiautomator
     * testing. Any unnecessary surrounding layout nodes that make viewing
     * and searching the hierarchy inefficient are removed.
     *
     * @param compressed true to enable compression; else, false to disable
     */
    public void setCompressedLayoutHierarchy(boolean compressed) {
        mCompressed = compressed;
        mCachedServiceFlags = -1; // Reset cached accessibility service flags to force an update.
    }

    /**
     * Retrieves a singleton instance of UiDevice
     *
     * @deprecated Should use {@link #getInstance(Instrumentation)} instead. This version hides
     * UiDevice's dependency on having an Instrumentation reference and is prone to misuse.
     * @return UiDevice instance
     */
    @Deprecated
    @NonNull
    public static UiDevice getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("UiDevice singleton not initialized");
        }
        return sInstance;
    }

    /**
     * Retrieves a singleton instance of UiDevice
     *
     * @return UiDevice instance
     */
    @NonNull
    public static UiDevice getInstance(@NonNull Instrumentation instrumentation) {
        if (sInstance == null) {
            sInstance = new UiDevice(instrumentation);
        }
        return sInstance;
    }

    /**
     * Returns the display size in dp (device-independent pixel)
     *
     * The returned display size is adjusted per screen rotation. Also this will return the actual
     * size of the screen, rather than adjusted per system decorations (like status bar).
     *
     * @return a Point containing the display size in dp
     */
    @NonNull
    public Point getDisplaySizeDp() {
        Display display = getDefaultDisplay();
        Point p = new Point();
        display.getRealSize(p);
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        float dpx = p.x / metrics.density;
        float dpy = p.y / metrics.density;
        p.x = Math.round(dpx);
        p.y = Math.round(dpy);
        return p;
    }

    /**
     * Retrieves the product name of the device.
     *
     * This method provides information on what type of device the test is running on. This value is
     * the same as returned by invoking #adb shell getprop ro.product.name.
     *
     * @return product name of the device
     */
    @NonNull
    public String getProductName() {
        return Build.PRODUCT;
    }

    /**
     * Retrieves the text from the last UI traversal event received.
     *
     * You can use this method to read the contents in a WebView container
     * because the accessibility framework fires events
     * as each text is highlighted. You can write a test to perform
     * directional arrow presses to focus on different elements inside a WebView,
     * and call this method to get the text from each traversed element.
     * If you are testing a view container that can return a reference to a
     * Document Object Model (DOM) object, your test should use the view's
     * DOM instead.
     *
     * @return text of the last traversal event, else return an empty string
     */
    @SuppressLint("UnknownNullness") // Avoid unnecessary null checks from nullable testing APIs.
    public String getLastTraversedText() {
        return getQueryController().getLastTraversedText();
    }

    /**
     * Clears the text from the last UI traversal event.
     * See {@link #getLastTraversedText()}.
     */
    public void clearLastTraversedText() {
        Log.d(TAG, "Clearing last traversed text.");
        getQueryController().clearLastTraversedText();
    }

    /**
     * Simulates a short press on the MENU button.
     * @return true if successful, else return false
     */
    public boolean pressMenu() {
        waitForIdle();
        Log.d(TAG, "Pressing menu button.");
        return getInteractionController().sendKeyAndWaitForEvent(
                KeyEvent.KEYCODE_MENU, 0, AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                KEY_PRESS_EVENT_TIMEOUT);
    }

    /**
     * Simulates a short press on the BACK button.
     * @return true if successful, else return false
     */
    public boolean pressBack() {
        waitForIdle();
        Log.d(TAG, "Pressing back button.");
        return getInteractionController().sendKeyAndWaitForEvent(
                KeyEvent.KEYCODE_BACK, 0, AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                KEY_PRESS_EVENT_TIMEOUT);
    }

    /**
     * Simulates a short press on the HOME button.
     * @return true if successful, else return false
     */
    public boolean pressHome() {
        waitForIdle();
        Log.d(TAG, "Pressing home button.");
        return getInteractionController().sendKeyAndWaitForEvent(
                KeyEvent.KEYCODE_HOME, 0, AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                KEY_PRESS_EVENT_TIMEOUT);
    }

    /**
     * Simulates a short press on the SEARCH button.
     * @return true if successful, else return false
     */
    public boolean pressSearch() {
        return pressKeyCode(KeyEvent.KEYCODE_SEARCH);
    }

    /**
     * Simulates a short press on the CENTER button.
     * @return true if successful, else return false
     */
    public boolean pressDPadCenter() {
        return pressKeyCode(KeyEvent.KEYCODE_DPAD_CENTER);
    }

    /**
     * Simulates a short press on the DOWN button.
     * @return true if successful, else return false
     */
    public boolean pressDPadDown() {
        return pressKeyCode(KeyEvent.KEYCODE_DPAD_DOWN);
    }

    /**
     * Simulates a short press on the UP button.
     * @return true if successful, else return false
     */
    public boolean pressDPadUp() {
        return pressKeyCode(KeyEvent.KEYCODE_DPAD_UP);
    }

    /**
     * Simulates a short press on the LEFT button.
     * @return true if successful, else return false
     */
    public boolean pressDPadLeft() {
        return pressKeyCode(KeyEvent.KEYCODE_DPAD_LEFT);
    }

    /**
     * Simulates a short press on the RIGHT button.
     * @return true if successful, else return false
     */
    public boolean pressDPadRight() {
        return pressKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT);
    }

    /**
     * Simulates a short press on the DELETE key.
     * @return true if successful, else return false
     */
    public boolean pressDelete() {
        return pressKeyCode(KeyEvent.KEYCODE_DEL);
    }

    /**
     * Simulates a short press on the ENTER key.
     * @return true if successful, else return false
     */
    public boolean pressEnter() {
        return pressKeyCode(KeyEvent.KEYCODE_ENTER);
    }

    /**
     * Simulates a short press using a key code.
     *
     * See {@link KeyEvent}
     * @return true if successful, else return false
     */
    public boolean pressKeyCode(int keyCode) {
        return pressKeyCode(keyCode, 0);
    }

    /**
     * Simulates a short press using a key code.
     *
     * See {@link KeyEvent}.
     * @param keyCode the key code of the event.
     * @param metaState an integer in which each bit set to 1 represents a pressed meta key
     * @return true if successful, else return false
     */
    public boolean pressKeyCode(int keyCode, int metaState) {
        return pressKeyCodes(new int[]{keyCode}, metaState);
    }

    /**
     * Presses one or more keys.
     * <br/>
     * For example, you can simulate taking a screenshot on the device by pressing both the
     * power and volume down keys.
     * <pre>{@code pressKeyCodes(new int[]{KeyEvent.KEYCODE_POWER, KeyEvent.KEYCODE_VOLUME_DOWN})}
     * </pre>
     *
     * @see KeyEvent
     * @param keyCodes array of key codes.
     * @return true if successful, else return false
     */
    public boolean pressKeyCodes(@NonNull int[] keyCodes) {
        return pressKeyCodes(keyCodes, 0);
    }

    /**
     * Presses one or more keys.
     * <br/>
     * For example, you can simulate taking a screenshot on the device by pressing both the
     * power and volume down keys.
     * <pre>{@code pressKeyCodes(new int[]{KeyEvent.KEYCODE_POWER, KeyEvent.KEYCODE_VOLUME_DOWN})}
     * </pre>
     *
     * @see KeyEvent
     * @param keyCodes array of key codes.
     * @param metaState an integer in which each bit set to 1 represents a pressed meta key
     * @return true if successful, else return false
     */
    public boolean pressKeyCodes(@NonNull int[] keyCodes, int metaState) {
        waitForIdle();
        Log.d(TAG, String.format("Pressing keycodes %s with modifier %d.",
                Arrays.toString(keyCodes),
                metaState));
        return getInteractionController().sendKeys(keyCodes, metaState);
    }

    /**
     * Simulates a short press on the Recent Apps button.
     *
     * @return true if successful, else return false
     * @throws RemoteException
     */
    public boolean pressRecentApps() throws RemoteException {
        waitForIdle();
        Log.d(TAG, "Pressing recent apps button.");
        return getInteractionController().toggleRecentApps();
    }

    /**
     * Opens the notification shade.
     *
     * @return true if successful, else return false
     */
    public boolean openNotification() {
        waitForIdle();
        Log.d(TAG, "Opening notification.");
        return  getInteractionController().openNotification();
    }

    /**
     * Opens the Quick Settings shade.
     *
     * @return true if successful, else return false
     */
    public boolean openQuickSettings() {
        waitForIdle();
        Log.d(TAG, "Opening quick settings.");
        return getInteractionController().openQuickSettings();
    }

    /**
     * Gets the width of the display, in pixels. The width and height details
     * are reported based on the current orientation of the display.
     * @return width in pixels or zero on failure
     */
    public int getDisplayWidth() {
        Display display = getDefaultDisplay();
        Point p = new Point();
        display.getRealSize(p);
        return p.x;
    }

    /**
     * Gets the height of the display, in pixels. The size is adjusted based
     * on the current orientation of the display.
     * @return height in pixels or zero on failure
     */
    public int getDisplayHeight() {
        Display display = getDefaultDisplay();
        Point p = new Point();
        display.getRealSize(p);
        return p.y;
    }

    /**
     * Perform a click at arbitrary coordinates specified by the user
     *
     * @param x coordinate
     * @param y coordinate
     * @return true if the click succeeded else false
     */
    public boolean click(int x, int y) {
        if (x >= getDisplayWidth() || y >= getDisplayHeight()) {
            Log.w(TAG, String.format("Cannot click. Point (%d, %d) is outside display (%d, %d).",
                    x, y, getDisplayWidth(), getDisplayHeight()));
            return false;
        }
        Log.d(TAG, String.format("Clicking on (%d, %d).", x, y));
        return getInteractionController().clickNoSync(x, y);
    }

    /**
     * Performs a swipe from one coordinate to another using the number of steps
     * to determine smoothness and speed. Each step execution is throttled to 5ms
     * per step. So for a 100 steps, the swipe will take about 1/2 second to complete.
     *
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     * @param steps is the number of move steps sent to the system
     * @return false if the operation fails or the coordinates are invalid
     */
    public boolean swipe(int startX, int startY, int endX, int endY, int steps) {
        Log.d(TAG, String.format("Swiping from (%d, %d) to (%d, %d) in %d steps.", startX, startY,
                endX, endY, steps));
        return getInteractionController()
                .swipe(startX, startY, endX, endY, steps);
    }

    /**
     * Performs a swipe from one coordinate to another coordinate. You can control
     * the smoothness and speed of the swipe by specifying the number of steps.
     * Each step execution is throttled to 5 milliseconds per step, so for a 100
     * steps, the swipe will take around 0.5 seconds to complete.
     *
     * @param startX X-axis value for the starting coordinate
     * @param startY Y-axis value for the starting coordinate
     * @param endX X-axis value for the ending coordinate
     * @param endY Y-axis value for the ending coordinate
     * @param steps is the number of steps for the swipe action
     * @return true if swipe is performed, false if the operation fails
     * or the coordinates are invalid
     */
    public boolean drag(int startX, int startY, int endX, int endY, int steps) {
        Log.d(TAG, String.format("Dragging from (%d, %d) to (%d, %d) in %d steps.", startX, startY,
                endX, endY, steps));
        return getInteractionController()
                .swipe(startX, startY, endX, endY, steps, true);
    }

    /**
     * Performs a swipe between points in the Point array. Each step execution is throttled
     * to 5ms per step. So for a 100 steps, the swipe will take about 1/2 second to complete
     *
     * @param segments is Point array containing at least one Point object
     * @param segmentSteps steps to inject between two Points
     * @return true on success
     */
    public boolean swipe(@NonNull Point[] segments, int segmentSteps) {
        Log.d(TAG, String.format("Swiping between %s in %d steps.", Arrays.toString(segments),
                segmentSteps * (segments.length - 1)));
        return getInteractionController().swipe(segments, segmentSteps);
    }

    /**
     * Waits for the current application to idle.
     * Default wait timeout is 10 seconds
     */
    public void waitForIdle() {
        getQueryController().waitForIdle();
    }

    /**
     * Waits for the current application to idle.
     * @param timeout in milliseconds
     */
    public void waitForIdle(long timeout) {
        getQueryController().waitForIdle(timeout);
    }

    /**
     * Retrieves the last activity to report accessibility events.
     * @deprecated The results returned should be considered unreliable
     * @return String name of activity
     */
    @Deprecated
    @SuppressLint("UnknownNullness") // Avoid unnecessary null checks from nullable testing APIs.
    public String getCurrentActivityName() {
        return getQueryController().getCurrentActivityName();
    }

    /**
     * Retrieves the name of the last package to report accessibility events.
     * @return String name of package
     */
    @SuppressLint("UnknownNullness") // Avoid unnecessary null checks from nullable testing APIs.
    public String getCurrentPackageName() {
        return getQueryController().getCurrentPackageName();
    }

    /**
     * Registers a {@link UiWatcher} to run automatically when the testing framework is unable to
     * find a match using a {@link UiSelector}. See {@link #runWatchers()}
     *
     * @param name to register the UiWatcher
     * @param watcher {@link UiWatcher}
     */
    public void registerWatcher(@Nullable String name, @Nullable UiWatcher watcher) {
        Log.d(TAG, String.format("Registering watcher %s.", name));
        if (mInWatcherContext) {
            throw new IllegalStateException("Cannot register new watcher from within another");
        }
        mWatchers.put(name, watcher);
    }

    /**
     * Removes a previously registered {@link UiWatcher}.
     *
     * See {@link #registerWatcher(String, UiWatcher)}
     * @param name used to register the UiWatcher
     */
    public void removeWatcher(@Nullable String name) {
        Log.d(TAG, String.format("Removing watcher %s.", name));
        if (mInWatcherContext) {
            throw new IllegalStateException("Cannot remove a watcher from within another");
        }
        mWatchers.remove(name);
    }

    /**
     * This method forces all registered watchers to run.
     * See {@link #registerWatcher(String, UiWatcher)}
     */
    public void runWatchers() {
        if (mInWatcherContext) {
            return;
        }

        for (String watcherName : mWatchers.keySet()) {
            UiWatcher watcher = mWatchers.get(watcherName);
            if (watcher != null) {
                try {
                    mInWatcherContext = true;
                    if (watcher.checkForCondition()) {
                        setWatcherTriggered(watcherName);
                    }
                } catch (Exception e) {
                    Log.e(TAG, String.format("Failed to execute watcher %s.", watcherName), e);
                } finally {
                    mInWatcherContext = false;
                }
            }
        }
    }

    /**
     * Resets a {@link UiWatcher} that has been triggered.
     * If a UiWatcher runs and its {@link UiWatcher#checkForCondition()} call
     * returned <code>true</code>, then the UiWatcher is considered triggered.
     * See {@link #registerWatcher(String, UiWatcher)}
     */
    public void resetWatcherTriggers() {
        Log.d(TAG, "Resetting all watchers.");
        mWatchersTriggers.clear();
    }

    /**
     * Checks if a specific registered  {@link UiWatcher} has triggered.
     * See {@link #registerWatcher(String, UiWatcher)}. If a UiWatcher runs and its
     * {@link UiWatcher#checkForCondition()} call returned <code>true</code>, then
     * the UiWatcher is considered triggered. This is helpful if a watcher is detecting errors
     * from ANR or crash dialogs and the test needs to know if a UiWatcher has been triggered.
     *
     * @param watcherName
     * @return true if triggered else false
     */
    public boolean hasWatcherTriggered(@Nullable String watcherName) {
        return mWatchersTriggers.contains(watcherName);
    }

    /**
     * Checks if any registered {@link UiWatcher} have triggered.
     *
     * See {@link #registerWatcher(String, UiWatcher)}
     * See {@link #hasWatcherTriggered(String)}
     */
    public boolean hasAnyWatcherTriggered() {
        return mWatchersTriggers.size() > 0;
    }

    /**
     * Used internally by this class to set a {@link UiWatcher} state as triggered.
     * @param watcherName
     */
    private void setWatcherTriggered(String watcherName) {
        if (!hasWatcherTriggered(watcherName)) {
            mWatchersTriggers.add(watcherName);
        }
    }

    /**
     * @return true if device is in its natural orientation (0 or 180 degrees)
     */
    public boolean isNaturalOrientation() {
        int ret = getDisplayRotation();
        return ret == UiAutomation.ROTATION_FREEZE_0 ||
                ret == UiAutomation.ROTATION_FREEZE_180;
    }

    /**
     * @return the current rotation of the display, as defined in {@link Surface}
     */
    public int getDisplayRotation() {
        waitForIdle();
        return getDefaultDisplay().getRotation();
    }

    /**
     * Freezes the device rotation at its current state.
     * @throws RemoteException never
     */
    public void freezeRotation() throws RemoteException {
        Log.d(TAG, "Freezing rotation.");
        getUiAutomation().setRotation(UiAutomation.ROTATION_FREEZE_CURRENT);
    }

    /**
     * Un-freezes the device rotation allowing its contents to rotate with the device physical
     * rotation. During testing, it is best to keep the device frozen in a specific orientation.
     * @throws RemoteException never
     */
    public void unfreezeRotation() throws RemoteException {
        Log.d(TAG, "Unfreezing rotation.");
        getUiAutomation().setRotation(UiAutomation.ROTATION_UNFREEZE);
    }

    /**
     * Orients the device to the left and freezes rotation. Use {@link #unfreezeRotation()} to
     * un-freeze the rotation.
     * <p>Note: This rotation is relative to the natural orientation which depends on the device
     * type (e.g. phone vs. tablet). Consider using {@link #setOrientationPortrait()} and
     * {@link #setOrientationLandscape()}.
     * @throws RemoteException never
     */
    public void setOrientationLeft() throws RemoteException {
        Log.d(TAG, "Setting orientation to left.");
        rotate(UiAutomation.ROTATION_FREEZE_90);
    }

    /**
     * Orients the device to the right and freezes rotation. Use {@link #unfreezeRotation()} to
     * un-freeze the rotation.
     * <p>Note: This rotation is relative to the natural orientation which depends on the device
     * type (e.g. phone vs. tablet). Consider using {@link #setOrientationPortrait()} and
     * {@link #setOrientationLandscape()}.
     * @throws RemoteException never
     */
    public void setOrientationRight() throws RemoteException {
        Log.d(TAG, "Setting orientation to right.");
        rotate(UiAutomation.ROTATION_FREEZE_270);
    }

    /**
     * Orients the device to its natural orientation (0 or 180 degrees) and freezes rotation. Use
     * {@link #unfreezeRotation()} to un-freeze the rotation.
     * <p>Note: The natural orientation depends on the device type (e.g. phone vs. tablet).
     * Consider using {@link #setOrientationPortrait()} and {@link #setOrientationLandscape()}.
     * @throws RemoteException never
     */
    public void setOrientationNatural() throws RemoteException {
        Log.d(TAG, "Setting orientation to natural.");
        rotate(UiAutomation.ROTATION_FREEZE_0);
    }

    /**
     * Orients the device to its portrait orientation (height > width) and freezes rotation. Use
     * {@link #unfreezeRotation()} to un-freeze the rotation.
     * @throws RemoteException never
     */
    public void setOrientationPortrait() throws RemoteException {
        Log.d(TAG, "Setting orientation to portrait.");
        if (getDisplayHeight() > getDisplayWidth()) {
            freezeRotation(); // Already in portrait orientation.
        } else if (isNaturalOrientation()) {
            rotate(UiAutomation.ROTATION_FREEZE_90);
        } else {
            rotate(UiAutomation.ROTATION_FREEZE_0);
        }
    }

    /**
     * Orients the device to its landscape orientation (width > height) and freezes rotation. Use
     * {@link #unfreezeRotation()} to un-freeze the rotation.
     * @throws RemoteException never
     */
    public void setOrientationLandscape() throws RemoteException {
        Log.d(TAG, "Setting orientation to landscape.");
        if (getDisplayWidth() > getDisplayHeight()) {
            freezeRotation(); // Already in landscape orientation.
        } else if (isNaturalOrientation()) {
            rotate(UiAutomation.ROTATION_FREEZE_90);
        } else {
            rotate(UiAutomation.ROTATION_FREEZE_0);
        }
    }

    // Rotates the device and waits for the rotation to be detected.
    private void rotate(int rotation) {
        getUiAutomation().setRotation(rotation);
        Condition<UiDevice, Boolean> rotationCondition = new Condition<UiDevice, Boolean>() {
            @Override
            public Boolean apply(UiDevice device) {
                return device.getDisplayRotation() == rotation;
            }

            @NonNull
            @Override
            public String toString() {
                return String.format("Condition[displayRotation=%d]", rotation);
            }
        };
        if (!wait(rotationCondition, ROTATION_TIMEOUT)) {
            Log.w(TAG, String.format("Didn't detect rotation within %dms.", ROTATION_TIMEOUT));
        }
    }

    /**
     * This method simulates pressing the power button if the screen is OFF else
     * it does nothing if the screen is already ON.
     *
     * If the screen was OFF and it just got turned ON, this method will insert a 500ms delay
     * to allow the device time to wake up and accept input.
     * @throws RemoteException
     */
    public void wakeUp() throws RemoteException {
        Log.d(TAG, "Turning on screen.");
        if(getInteractionController().wakeDevice()) {
            // sync delay to allow the window manager to start accepting input
            // after the device is awakened.
            SystemClock.sleep(500);
        }
    }

    /**
     * Checks the power manager if the screen is ON.
     *
     * @return true if the screen is ON else false
     * @throws RemoteException
     */
    public boolean isScreenOn() throws RemoteException {
        return getInteractionController().isScreenOn();
    }

    /**
     * This method simply presses the power button if the screen is ON else
     * it does nothing if the screen is already OFF.
     *
     * @throws RemoteException
     */
    public void sleep() throws RemoteException {
        Log.d(TAG, "Turning off screen.");
        getInteractionController().sleepDevice();
    }

    /**
     * Helper method used for debugging to dump the current window's layout hierarchy.
     * Relative file paths are stored the application's internal private storage location.
     *
     * @param fileName
     * @deprecated Use {@link UiDevice#dumpWindowHierarchy(File)} or
     *     {@link UiDevice#dumpWindowHierarchy(OutputStream)} instead.
     */
    @Deprecated
    public void dumpWindowHierarchy(@NonNull String fileName) {

        File dumpFile = new File(fileName);
        if (!dumpFile.isAbsolute()) {
            dumpFile = mInstrumentation.getContext().getFileStreamPath(fileName);
        }
        try {
            dumpWindowHierarchy(dumpFile);
        } catch (IOException e) {
            // Ignore to preserve existing behavior. Ugh.
        }
    }

    /**
     * Dump the current window hierarchy to a {@link java.io.File}.
     *
     * @param dest The file in which to store the window hierarchy information.
     * @throws IOException
     */
    public void dumpWindowHierarchy(@NonNull File dest) throws IOException {
        try (OutputStream stream = new BufferedOutputStream(new FileOutputStream(dest))) {
            dumpWindowHierarchy(stream);
        }
    }

    /**
     * Dump the current window hierarchy to an {@link java.io.OutputStream}.
     *
     * @param out The output stream that the window hierarchy information is written to.
     * @throws IOException
     */
    public void dumpWindowHierarchy(@NonNull OutputStream out) throws IOException {
        AccessibilityNodeInfoDumper.dumpWindowHierarchy(this, out);
    }

    /**
     * Waits for a window content update event to occur.
     *
     * If a package name for the window is specified, but the current window
     * does not have the same package name, the function returns immediately.
     *
     * @param packageName the specified window package name (can be <code>null</code>).
     *        If <code>null</code>, a window update from any front-end window will end the wait
     * @param timeout the timeout for the wait
     *
     * @return true if a window update occurred, false if timeout has elapsed or if the current
     *         window does not have the specified package name
     */
    public boolean waitForWindowUpdate(@Nullable String packageName, long timeout) {
        if (packageName != null) {
            if (!packageName.equals(getCurrentPackageName())) {
                Log.w(TAG, String.format("Skipping wait as package %s does not match current "
                        + "window %s.", packageName, getCurrentPackageName()));
                return false;
            }
        }
        Runnable emptyRunnable = () -> {};
        AccessibilityEventFilter checkWindowUpdate = t -> {
            if (t.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                return packageName == null || (t.getPackageName() != null
                        && packageName.contentEquals(t.getPackageName()));
            }
            return false;
        };
        Log.d(TAG, String.format("Waiting %dms for window update of package %s.", timeout,
                packageName));
        try {
            getUiAutomation().executeAndWaitForEvent(emptyRunnable, checkWindowUpdate, timeout);
        } catch (TimeoutException e) {
            Log.w(TAG, String.format("Timed out waiting %dms on window update.", timeout), e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Failed to wait for window update.", e);
            return false;
        }
        return true;
    }

    /**
     * Take a screenshot of current window and store it as PNG
     *
     * Default scale of 1.0f (original size) and 90% quality is used
     * The screenshot is adjusted per screen rotation
     *
     * @param storePath where the PNG should be written to
     * @return true if screen shot is created successfully, false otherwise
     */
    public boolean takeScreenshot(@NonNull File storePath) {
        return takeScreenshot(storePath, 1.0f, 90);
    }

    /**
     * Take a screenshot of current window and store it as PNG
     *
     * The screenshot is adjusted per screen rotation
     *
     * @param storePath where the PNG should be written to
     * @param scale scale the screenshot down if needed; 1.0f for original size
     * @param quality quality of the PNG compression; range: 0-100
     * @return true if screen shot is created successfully, false otherwise
     */
    public boolean takeScreenshot(@NonNull File storePath, float scale, int quality) {
        Log.d(TAG, String.format("Taking screenshot (scale=%f, quality=%d) and storing at %s.",
                scale, quality, storePath));
        Bitmap screenshot = getUiAutomation().takeScreenshot();
        if (screenshot == null) {
            Log.w(TAG, "Failed to take screenshot.");
            return false;
        }
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(storePath))) {
            screenshot = Bitmap.createScaledBitmap(screenshot,
                    Math.round(scale * screenshot.getWidth()),
                    Math.round(scale * screenshot.getHeight()), false);
            screenshot.compress(Bitmap.CompressFormat.PNG, quality, bos);
            bos.flush();
            return true;
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to save screenshot.", ioe);
            return false;
        } finally {
            screenshot.recycle();
        }
    }

    /**
     * Retrieves the default launcher package name.
     *
     * <p>As of Android 11 (API level 30), apps must declare the packages and intents they intend
     * to query. To use this method, an app will need to include the following in its manifest:
     * <pre>{@code
     * <queries>
     *   <intent>
     *     <action android:name="android.intent.action.MAIN"/>
     *     <category android:name="android.intent.category.HOME"/>
     *   </intent>
     * </queries>
     * }</pre>
     *
     * @return package name of the default launcher
     */
    @SuppressLint("UnknownNullness") // Avoid unnecessary null checks from nullable testing APIs.
    public String getLauncherPackageName() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        PackageManager pm = mInstrumentation.getContext().getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo.activityInfo.packageName;
    }

    /**
     * Executes a shell command using shell user identity, and return the standard output in string.
     * <p>
     * Calling function with large amount of output will have memory impacts, and the function call
     * will block if the command executed is blocking.
     * <p>Note: calling this function requires API level 21 or above
     * @param cmd the command to run
     * @return the standard output of the command
     * @throws IOException
     * @hide
     */
    @RequiresApi(21)
    @NonNull
    public String executeShellCommand(@NonNull String cmd) throws IOException {
        Log.d(TAG, String.format("Executing shell command: %s", cmd));
        try (ParcelFileDescriptor pfd = Api21Impl.executeShellCommand(getUiAutomation(), cmd);
             FileInputStream fis = new ParcelFileDescriptor.AutoCloseInputStream(pfd)) {
            byte[] buf = new byte[512];
            int bytesRead;
            StringBuilder stdout = new StringBuilder();
            while ((bytesRead = fis.read(buf)) != -1) {
                stdout.append(new String(buf, 0, bytesRead));
            }
            return stdout.toString();
        }
    }

    private Display getDefaultDisplay() {
        return mDisplayManager.getDisplay(Display.DEFAULT_DISPLAY);
    }

    @RequiresApi(21)
    private List<AccessibilityWindowInfo> getWindows(UiAutomation uiAutomation) {
        // Support multi-display searches for API level 30 and up.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final List<AccessibilityWindowInfo> windowList = new ArrayList<>();
            final SparseArray<List<AccessibilityWindowInfo>> allWindows =
                    Api30Impl.getWindowsOnAllDisplays(uiAutomation);
            for (int index = 0; index < allWindows.size(); index++) {
                windowList.addAll(allWindows.valueAt(index));
            }
            return windowList;
        }
        return Api21Impl.getWindows(uiAutomation);
    }

    /** Returns a list containing the root {@link AccessibilityNodeInfo}s for each active window */
    AccessibilityNodeInfo[] getWindowRoots() {
        waitForIdle();

        Set<AccessibilityNodeInfo> roots = new HashSet<>();
        UiAutomation uiAutomation = getUiAutomation();

        // Ensure the active window root is included.
        AccessibilityNodeInfo activeRoot = uiAutomation.getRootInActiveWindow();
        if (activeRoot != null) {
            roots.add(activeRoot);
        } else {
            Log.w(TAG, "Active window root not found.");
        }
        // Support multi-window searches for API level 21 and up.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (final AccessibilityWindowInfo window : getWindows(uiAutomation)) {
                final AccessibilityNodeInfo root = Api21Impl.getRoot(window);
                if (root == null) {
                    Log.w(TAG, "Skipping null root node for window: " + window);
                    continue;
                }
                roots.add(root);
            }
        }
        return roots.toArray(new AccessibilityNodeInfo[0]);
    }

    Instrumentation getInstrumentation() {
        return mInstrumentation;
    }

    Context getUiContext(int displayId) {
        Context context = mUiContexts.get(displayId);
        if (context == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Display display = mDisplayManager.getDisplay(displayId);
                context = Api31Impl.createWindowContext(mInstrumentation.getContext(), display);
            } else {
                context = mInstrumentation.getContext();
            }
            mUiContexts.put(displayId, context);
        }
        return context;
    }

    UiAutomation getUiAutomation() {
        UiAutomation uiAutomation;
        int flags = Configurator.getInstance().getUiAutomationFlags();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uiAutomation = Api24Impl.getUiAutomation(getInstrumentation(), flags);
        } else {
            if (flags != Configurator.DEFAULT_UIAUTOMATION_FLAGS) {
                Log.w(TAG, "UiAutomation flags not supported prior to API 24");
            }
            uiAutomation = getInstrumentation().getUiAutomation();
        }

        // Verify and update the accessibility service flags if necessary. These might get reset
        // if the underlying UiAutomationConnection is recreated.
        AccessibilityServiceInfo serviceInfo = uiAutomation.getServiceInfo();
        if (serviceInfo == null) {
            Log.w(TAG, "Cannot verify accessibility service flags. "
                    + "Multi-window support (searching non-active windows) may be disabled.");
        } else if (serviceInfo.flags != mCachedServiceFlags) {
            // Enable multi-window support for API 21+.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                serviceInfo.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            }
            // Enable or disable hierarchy compression.
            if (mCompressed) {
                serviceInfo.flags &= ~AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
            } else {
                serviceInfo.flags |= AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
            }
            Log.d(TAG,
                    String.format("Setting accessibility service flags: %d", serviceInfo.flags));
            uiAutomation.setServiceInfo(serviceInfo);
            mCachedServiceFlags = serviceInfo.flags;
        }

        return uiAutomation;
    }

    QueryController getQueryController() {
        return mQueryController;
    }

    InteractionController getInteractionController() {
        return mInteractionController;
    }

    @RequiresApi(21)
    static class Api21Impl {
        private Api21Impl() {
        }

        @DoNotInline
        static ParcelFileDescriptor executeShellCommand(UiAutomation uiAutomation, String command) {
            return uiAutomation.executeShellCommand(command);
        }

        @DoNotInline
        static List<AccessibilityWindowInfo> getWindows(UiAutomation uiAutomation) {
            return uiAutomation.getWindows();
        }

        @DoNotInline
        static AccessibilityNodeInfo getRoot(AccessibilityWindowInfo accessibilityWindowInfo) {
            return accessibilityWindowInfo.getRoot();
        }
    }

    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() {
        }

        @DoNotInline
        static UiAutomation getUiAutomation(Instrumentation instrumentation, int flags) {
            return instrumentation.getUiAutomation(flags);
        }
    }

    @RequiresApi(30)
    static class Api30Impl {
        private Api30Impl() {
        }

        @DoNotInline
        static SparseArray<List<AccessibilityWindowInfo>> getWindowsOnAllDisplays(
                UiAutomation uiAutomation) {
            return uiAutomation.getWindowsOnAllDisplays();
        }
    }

    @RequiresApi(31)
    static class Api31Impl {
        private Api31Impl() {
        }

        @DoNotInline
        static Context createWindowContext(Context context, Display display) {
            return context.createWindowContext(display,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, null);
        }
    }
}
