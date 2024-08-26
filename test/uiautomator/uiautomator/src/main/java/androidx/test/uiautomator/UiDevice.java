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

import android.accessibilityservice.AccessibilityService;
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

import androidx.annotation.Discouraged;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RequiresApi;
import androidx.test.uiautomator.util.Traces;
import androidx.test.uiautomator.util.Traces.Section;

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

    static final String TAG = UiDevice.class.getSimpleName();

    private static final int MAX_UIAUTOMATION_RETRY = 3;
    private static final int UIAUTOMATION_RETRY_INTERVAL = 500; // ms
    // Workaround for stale accessibility cache issues: duration after which the a11y service flags
    // should be reset (when fetching a UiAutomation instance) to periodically invalidate the cache.
    private static final long SERVICE_FLAGS_TIMEOUT = 2_000; // ms

    // Use a short timeout after HOME or BACK key presses, as no events might be generated if
    // already on the home page or if there is nothing to go back to.
    private static final long KEY_PRESS_EVENT_TIMEOUT = 1_000; // ms
    private static final long ROTATION_TIMEOUT = 2_000; // ms

    // Singleton instance.
    private static UiDevice sInstance;

    private final Instrumentation mInstrumentation;
    private final QueryController mQueryController;
    private final InteractionController mInteractionController;
    private final DisplayManager mDisplayManager;
    private final WaitMixin<UiDevice> mWaitMixin = new WaitMixin<>(this);

    // Track accessibility service flags to determine when the underlying connection has changed.
    private int mCachedServiceFlags = -1;
    private long mLastServiceFlagsTime = -1;
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
        Log.d(TAG, String.format("Searching for node with selector: %s.", selector));
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
        Log.d(TAG, String.format("Retrieving node with selector: %s.", selector));
        AccessibilityNodeInfo node = ByMatcher.findMatch(this, selector, getWindowRoots());
        if (node == null) {
            Log.d(TAG, String.format("Node not found with selector: %s.", selector));
            return null;
        }
        return UiObject2.create(this, selector, node);
    }

    /** Returns all objects that match the {@code selector} criteria. */
    @Override
    @NonNull
    public List<UiObject2> findObjects(@NonNull BySelector selector) {
        Log.d(TAG, String.format("Retrieving nodes with selector: %s.", selector));
        List<UiObject2> ret = new ArrayList<>();
        for (AccessibilityNodeInfo node : ByMatcher.findMatches(this, selector, getWindowRoots())) {
            UiObject2 object = UiObject2.create(this, selector, node);
            if (object != null) {
                ret.add(object);
            }
        }
        return ret;
    }


    /**
     * Waits for given the {@code condition} to be met.
     *
     * @param condition The {@link SearchCondition} to evaluate.
     * @param timeout Maximum amount of time to wait in milliseconds.
     * @return The final result returned by the {@code condition}, or null if the {@code condition}
     * was not met before the {@code timeout}.
     */
    public <U> U wait(@NonNull SearchCondition<U> condition, long timeout) {
        return wait((Condition<? super UiDevice, U>) condition, timeout);
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
        try (Section ignored = Traces.trace("UiDevice#wait")) {
            Log.d(TAG, String.format("Waiting %dms for %s.", timeout, condition));
            return mWaitMixin.wait(condition, timeout);
        }
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
        try (Section ignored = Traces.trace("UiDevice#performActionAndWait")) {
            AccessibilityEvent event = null;
            Log.d(TAG, String.format("Performing action %s and waiting %dms for %s.", action,
                    timeout, condition));
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
     * Retrieves a singleton instance of UiDevice. A new instance will be created if
     * instrumentation is also new.
     *
     * @return UiDevice instance
     */
    @NonNull
    public static UiDevice getInstance(@NonNull Instrumentation instrumentation) {
        if (sInstance == null || !instrumentation.equals(sInstance.mInstrumentation)) {
            Log.i(TAG, String.format("Creating a new instance, old instance exists: %b",
                    (sInstance != null)));
            sInstance = new UiDevice(instrumentation);
        }
        return sInstance;
    }

    /**
     * Returns the default display size in dp (device-independent pixel).
     * <p>The returned display size is adjusted per screen rotation. Also this will return the
     * actual size of the screen, rather than adjusted per system decorations (like status bar).
     *
     * @see DisplayMetrics#density
     * @return a Point containing the display size in dp
     */
    @NonNull
    public Point getDisplaySizeDp() {
        Point p = getDisplaySize(Display.DEFAULT_DISPLAY);
        Context context = getUiContext(Display.DEFAULT_DISPLAY);
        int densityDpi = context.getResources().getConfiguration().densityDpi;
        float density = (float) densityDpi / DisplayMetrics.DENSITY_DEFAULT;
        return new Point(Math.round(p.x / density), Math.round(p.y / density));
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
     * Presses one or more keys. Keys that change meta state are supported, and will apply their
     * meta state to following keys.
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
     * Presses one or more keys. Keys that change meta state are supported, and will apply their
     * meta state to following keys.
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
     * @throws RemoteException never
     */
    public boolean pressRecentApps() throws RemoteException {
        waitForIdle();
        Log.d(TAG, "Pressing recent apps button.");
        return getUiAutomation().performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
    }

    /**
     * Opens the notification shade.
     *
     * @return true if successful, else return false
     */
    public boolean openNotification() {
        waitForIdle();
        Log.d(TAG, "Opening notification.");
        return getUiAutomation().performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
    }

    /**
     * Opens the Quick Settings shade.
     *
     * @return true if successful, else return false
     */
    public boolean openQuickSettings() {
        waitForIdle();
        Log.d(TAG, "Opening quick settings.");
        return getUiAutomation().performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS);
    }

    /**
     * Gets the width of the default display, in pixels. The size is adjusted based on the
     * current orientation of the display.
     *
     * @return width in pixels
     */
    public @Px int getDisplayWidth() {
        return getDisplayWidth(Display.DEFAULT_DISPLAY);
    }

    /**
     * Gets the width of the display with {@code displayId}, in pixels. The size is adjusted
     * based on the current orientation of the display.
     *
     * @param displayId the display ID. Use {@link Display#getDisplayId()} to get the ID.
     * @return width in pixels
     * @throws IllegalArgumentException when the display with {@code displayId} is not accessible.
     */
    public @Px int getDisplayWidth(int displayId) {
        return getDisplaySize(displayId).x;
    }

    /**
     * Gets the height of the default display, in pixels. The size is adjusted based on the
     * current orientation of the display.
     *
     * @return height in pixels
     */
    public @Px int getDisplayHeight() {
        return getDisplayHeight(Display.DEFAULT_DISPLAY);
    }

    /**
     * Gets the height of the display with {@code displayId}, in pixels. The size is adjusted
     * based on the current orientation of the display.
     *
     * @param displayId the display ID. Use {@link Display#getDisplayId()} to get the ID.
     * @return height in pixels
     * @throws IllegalArgumentException when the display with {@code displayId} is not accessible.
     */
    public @Px int getDisplayHeight(int displayId) {
        return getDisplaySize(displayId).y;
    }

    /**
     * Perform a click at arbitrary coordinates on the default display specified by the user.
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
     * Performs a swipe from one coordinate to another on the default display using the number of
     * steps to determine smoothness and speed. Each step execution is throttled to 5ms per step.
     * So for a 100 steps, the swipe will take about 1/2 second to complete.
     *
     * @param startX X-axis value for the starting coordinate
     * @param startY Y-axis value for the starting coordinate
     * @param endX X-axis value for the ending coordinate
     * @param endY Y-axis value for the ending coordinate
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
     * Performs a swipe from one coordinate to another coordinate on the default display. You can
     * control the smoothness and speed of the swipe by specifying the number of steps. Each step
     * execution is throttled to 5 milliseconds per step, so for a 100 steps, the swipe will take
     * around 0.5 seconds to complete.
     *
     * @param startX X-axis value for the starting coordinate
     * @param startY Y-axis value for the starting coordinate
     * @param endX X-axis value for the ending coordinate
     * @param endY Y-axis value for the ending coordinate
     * @param steps is the number of steps for the swipe action
     * @return true if swipe is performed, false if the operation fails or the coordinates are
     * invalid
     */
    public boolean drag(int startX, int startY, int endX, int endY, int steps) {
        Log.d(TAG, String.format("Dragging from (%d, %d) to (%d, %d) in %d steps.", startX, startY,
                endX, endY, steps));
        return getInteractionController()
                .swipe(startX, startY, endX, endY, steps, true);
    }

    /**
     * Performs a swipe between points in the Point array on the default display. Each step
     * execution is throttled to 5ms per step. So for a 100 steps, the swipe will take about 1/2
     * second to complete.
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
        try (Section ignored = Traces.trace("UiDevice#waitForIdle")) {
            getQueryController().waitForIdle();
        }
    }

    /**
     * Waits for the current application to idle.
     * @param timeout in milliseconds
     */
    public void waitForIdle(long timeout) {
        try (Section ignored = Traces.trace("UiDevice#waitForIdle")) {
            getQueryController().waitForIdle(timeout);
        }
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
     * @return true if default display is in its natural or flipped (180 degrees) orientation
     */
    public boolean isNaturalOrientation() {
        return isNaturalOrientation(Display.DEFAULT_DISPLAY);
    }

    /**
     * @return true if display with {@code displayId} is in its natural or flipped (180 degrees)
     * orientation
     * @see Display#getDisplayId()
     * @throws IllegalArgumentException when the display with {@code displayId} is not accessible.
     */
    private boolean isNaturalOrientation(int displayId) {
        int ret = getDisplayRotation(displayId);
        return ret == UiAutomation.ROTATION_FREEZE_0
                || ret == UiAutomation.ROTATION_FREEZE_180;
    }

    /**
     * @return the current rotation of the default display
     * @see Display#getRotation()
     */
    public int getDisplayRotation() {
        return getDisplayRotation(Display.DEFAULT_DISPLAY);
    }

    /**
     * @return the current rotation of the display with {@code displayId}
     * @see Display#getDisplayId()
     * @see Display#getRotation()
     * @throws IllegalArgumentException when the display with {@code displayId} is not accessible.
     */
    public int getDisplayRotation(int displayId) {
        waitForIdle();
        Display display = getDisplayById(displayId);
        if (display == null) {
            throw new IllegalArgumentException(String.format("Display %d not found or not "
                    + "accessible", displayId));
        }
        return display.getRotation();
    }

    /**
     * Freezes the default display rotation at its current state.
     * @throws RemoteException never
     */
    public void freezeRotation() throws RemoteException {
        Log.d(TAG, "Freezing rotation.");
        getUiAutomation().setRotation(UiAutomation.ROTATION_FREEZE_CURRENT);
    }

    /**
     * Freezes the rotation of the display with {@code displayId} at its current state.
     * <p>Note: Only works on Android API level 30 (R) or above, where multi-display is
     * officially supported.
     * @see Display#getDisplayId()
     * @throws IllegalArgumentException when the display with {@code displayId} is not accessible.
     */
    @RequiresApi(30)
    public void freezeRotation(int displayId) {
        Log.d(TAG, String.format("Freezing rotation on display %d.", displayId));
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                executeShellCommand(String.format("cmd window user-rotation -d %d lock",
                        displayId));
            } else {
                int rotation = getDisplayRotation(displayId);
                executeShellCommand(String.format("cmd window set-user-rotation lock -d %d %d",
                        displayId, rotation));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Un-freezes the default display rotation allowing its contents to rotate with its physical
     * rotation. During testing, it is best to keep the default display frozen in a specific
     * orientation.
     * <p>Note: Need to wait a short period for the rotation animation to complete before
     * performing another operation.
     * @throws RemoteException never
     */
    public void unfreezeRotation() throws RemoteException {
        Log.d(TAG, "Unfreezing rotation.");
        getUiAutomation().setRotation(UiAutomation.ROTATION_UNFREEZE);
    }

    /**
     * Un-freezes the rotation of the display with {@code displayId} allowing its contents to
     * rotate with its physical rotation. During testing, it is best to keep the display frozen
     * in a specific orientation.
     * <p>Note: Need to wait a short period for the rotation animation to complete before
     * performing another operation.
     * <p>Note: Some secondary displays don't have rotation sensors and therefore won't respond
     * to this method.
     * <p>Note: Only works on Android API level 30 (R) or above, where multi-display is
     * officially supported.
     * @see Display#getDisplayId()
     */
    @RequiresApi(30)
    public void unfreezeRotation(int displayId) {
        Log.d(TAG, String.format("Unfreezing rotation on display %d.", displayId));
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                executeShellCommand(String.format("cmd window user-rotation -d %d free",
                        displayId));
            } else {
                executeShellCommand(String.format("cmd window set-user-rotation free -d %d",
                        displayId));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Orients the default display to the left and freezes rotation. Use
     * {@link #unfreezeRotation()} to un-freeze the rotation.
     * <p>Note: This rotation is relative to the natural orientation which depends on the device
     * type (e.g. phone vs. tablet). Consider using {@link #setOrientationPortrait()} and
     * {@link #setOrientationLandscape()}.
     * @throws RemoteException never
     */
    public void setOrientationLeft() throws RemoteException {
        Log.d(TAG, "Setting orientation to left.");
        rotateWithUiAutomation(UiAutomation.ROTATION_FREEZE_90);
    }

    /**
     * Orients the display with {@code displayId} to the left and freezes rotation. Use
     * {@link #unfreezeRotation()} to un-freeze the rotation.
     * <p>Note: This rotation is relative to the natural orientation which depends on the device
     * type (e.g. phone vs. tablet). Consider using {@link #setOrientationPortrait()} and
     * {@link #setOrientationLandscape()}.
     * <p>Note: Only works on Android API level 30 (R) or above, where multi-display is
     * officially supported.
     * @see Display#getDisplayId()
     * @throws IllegalArgumentException when the display with {@code displayId} is not accessible.
     */
    @RequiresApi(30)
    public void setOrientationLeft(int displayId) {
        Log.d(TAG, String.format("Setting orientation to left on display %d.", displayId));
        rotateWithCommand(Surface.ROTATION_90, displayId);
    }

    /**
     * Orients the default display to the right and freezes rotation. Use
     * {@link #unfreezeRotation()} to un-freeze the rotation.
     * <p>Note: This rotation is relative to the natural orientation which depends on the device
     * type (e.g. phone vs. tablet). Consider using {@link #setOrientationPortrait()} and
     * {@link #setOrientationLandscape()}.
     * @throws RemoteException never
     */
    public void setOrientationRight() throws RemoteException {
        Log.d(TAG, "Setting orientation to right.");
        rotateWithUiAutomation(UiAutomation.ROTATION_FREEZE_270);
    }

    /**
     * Orients the display with {@code displayId} to the right and freezes rotation. Use
     * {@link #unfreezeRotation()} to un-freeze the rotation.
     * <p>Note: This rotation is relative to the natural orientation which depends on the device
     * type (e.g. phone vs. tablet). Consider using {@link #setOrientationPortrait()} and
     * {@link #setOrientationLandscape()}.
     * <p>Note: Only works on Android API level 30 (R) or above, where multi-display is
     * officially supported.
     * @see Display#getDisplayId()
     * @throws IllegalArgumentException when the display with {@code displayId} is not accessible.
     */
    @RequiresApi(30)
    public void setOrientationRight(int displayId) {
        Log.d(TAG, String.format("Setting orientation to right on display %d.", displayId));
        rotateWithCommand(Surface.ROTATION_270, displayId);
    }

    /**
     * Orients the default display to its natural orientation and freezes rotation. Use
     * {@link #unfreezeRotation()} to un-freeze the rotation.
     * <p>Note: The natural orientation depends on the device type (e.g. phone vs. tablet).
     * Consider using {@link #setOrientationPortrait()} and {@link #setOrientationLandscape()}.
     * @throws RemoteException never
     */
    public void setOrientationNatural() throws RemoteException {
        Log.d(TAG, "Setting orientation to natural.");
        rotateWithUiAutomation(UiAutomation.ROTATION_FREEZE_0);
    }

    /**
     * Orients the display with {@code displayId} to its natural orientation and freezes rotation
     * . Use {@link #unfreezeRotation()} to un-freeze the rotation.
     * <p>Note: The natural orientation depends on the device type (e.g. phone vs. tablet).
     * Consider using {@link #setOrientationPortrait()} and {@link #setOrientationLandscape()}.
     * <p>Note: Only works on Android API level 30 (R) or above, where multi-display is
     * officially supported.
     * @see Display#getDisplayId()
     * @throws IllegalArgumentException when the display with {@code displayId} is not accessible.
     */
    @RequiresApi(30)
    public void setOrientationNatural(int displayId) {
        Log.d(TAG, String.format("Setting orientation to natural on display %d.", displayId));
        rotateWithCommand(Surface.ROTATION_0, displayId);
    }

    /**
     * Orients the default display to its portrait orientation (height >= width) and freezes
     * rotation. Use {@link #unfreezeRotation()} to un-freeze the rotation.
     * @throws RemoteException never
     */
    public void setOrientationPortrait() throws RemoteException {
        Log.d(TAG, "Setting orientation to portrait.");
        if (getDisplayHeight() >= getDisplayWidth()) {
            freezeRotation(); // Already in portrait orientation.
        } else if (isNaturalOrientation()) {
            rotateWithUiAutomation(UiAutomation.ROTATION_FREEZE_90);
        } else {
            rotateWithUiAutomation(UiAutomation.ROTATION_FREEZE_0);
        }
    }

    /**
     * Orients the display with {@code displayId} to its portrait orientation (height >= width) and
     * freezes rotation. Use {@link #unfreezeRotation()} to un-freeze the rotation.
     * <p>Note: Only works on Android API level 30 (R) or above, where multi-display is
     * officially supported.
     * @see Display#getDisplayId()
     * @throws IllegalArgumentException when the display with {@code displayId} is not accessible.
     */
    @RequiresApi(30)
    public void setOrientationPortrait(int displayId) {
        Log.d(TAG, String.format("Setting orientation to portrait on display %d.", displayId));
        if (getDisplayHeight(displayId) >= getDisplayWidth(displayId)) {
            freezeRotation(displayId); // Already in portrait orientation.
        } else if (isNaturalOrientation(displayId)) {
            rotateWithCommand(Surface.ROTATION_90, displayId);
        } else {
            rotateWithCommand(Surface.ROTATION_0, displayId);
        }
    }

    /**
     * Orients the default display to its landscape orientation (width >= height) and freezes
     * rotation. Use {@link #unfreezeRotation()} to un-freeze the rotation.
     * @throws RemoteException never
     */
    public void setOrientationLandscape() throws RemoteException {
        Log.d(TAG, "Setting orientation to landscape.");
        if (getDisplayWidth() >= getDisplayHeight()) {
            freezeRotation(); // Already in landscape orientation.
        } else if (isNaturalOrientation()) {
            rotateWithUiAutomation(UiAutomation.ROTATION_FREEZE_90);
        } else {
            rotateWithUiAutomation(UiAutomation.ROTATION_FREEZE_0);
        }
    }

    /**
     * Orients the display with {@code displayId} to its landscape orientation (width >= height) and
     * freezes rotation. Use {@link #unfreezeRotation()} to un-freeze the rotation.
     * <p>Note: Only works on Android API level 30 (R) or above, where multi-display is
     * officially supported.
     * @see Display#getDisplayId()
     * @throws IllegalArgumentException when the display with {@code displayId} is not accessible.
     */
    @RequiresApi(30)
    public void setOrientationLandscape(int displayId) {
        Log.d(TAG, String.format("Setting orientation to landscape on display %d.", displayId));
        if (getDisplayWidth(displayId) >= getDisplayHeight(displayId)) {
            freezeRotation(displayId); // Already in landscape orientation.
        } else if (isNaturalOrientation(displayId)) {
            rotateWithCommand(Surface.ROTATION_90, displayId);
        } else {
            rotateWithCommand(Surface.ROTATION_0, displayId);
        }
    }

    /** Rotates the default display using UiAutomation and waits for the rotation to be detected. */
    private void rotateWithUiAutomation(int rotation) {
        getUiAutomation().setRotation(rotation);
        waitRotationComplete(rotation, Display.DEFAULT_DISPLAY);
    }

    /**
     * Rotates the display using shell command and waits for the rotation to be detected.
     *
     * @throws IllegalArgumentException when the display with {@code displayId} is not accessible.
     */
    @RequiresApi(30)
    private void rotateWithCommand(int rotation, int displayId) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                executeShellCommand(String.format("cmd window user-rotation -d %d lock %d",
                        displayId, rotation));
            } else {
                executeShellCommand(String.format("cmd window set-user-rotation lock -d %d %d",
                        displayId, rotation));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        waitRotationComplete(rotation, displayId);
    }

    /**
     * Waits for the display with {@code displayId} to be in {@code rotation}.
     *
     * @throws IllegalArgumentException when the display with {@code displayId} is not accessible.
     */
    private void waitRotationComplete(int rotation, int displayId) {
        Condition<UiDevice, Boolean> rotationCondition = new Condition<UiDevice, Boolean>() {
            @Override
            public Boolean apply(UiDevice device) {
                return device.getDisplayRotation(displayId) == rotation;
            }

            @NonNull
            @Override
            public String toString() {
                return String.format("Condition[displayRotation=%d, displayId=%d]", rotation,
                        displayId);
            }
        };
        if (!wait(rotationCondition, ROTATION_TIMEOUT)) {
            Log.w(TAG, String.format("Didn't detect rotation within %dms.", ROTATION_TIMEOUT));
        }
    }

    /**
     * This method simulates pressing the power button if the default display is OFF, else it does
     * nothing if the default display is already ON.
     * <p>If the default display was OFF and it just got turned ON, this method will insert a 500ms
     * delay for the device to wake up and accept input.
     *
     * @throws RemoteException
     */
    public void wakeUp() throws RemoteException {
        Log.d(TAG, "Turning on screen.");
        if(getInteractionController().wakeDevice()) {
            // Sync delay to allow the window manager to start accepting input after the device
            // is awakened.
            SystemClock.sleep(500);
        }
    }

    /**
     * Checks the power manager if the default display is ON.
     *
     * @return true if the screen is ON else false
     * @throws RemoteException
     */
    public boolean isScreenOn() throws RemoteException {
        return getInteractionController().isScreenOn();
    }

    /**
     * This method simply presses the power button if the default display is ON, else it does
     * nothing if the default display is already OFF.
     *
     * @throws RemoteException
     */
    public void sleep() throws RemoteException {
        Log.d(TAG, "Turning off screen.");
        getInteractionController().sleepDevice();
    }

    /**
     * Dumps every window's layout hierarchy to a file in XML format.
     *
     * @param fileName The file path in which to store the window hierarchy information. Relative
     *                file paths are stored the application's internal private storage location.
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
     * Dumps every window's layout hierarchy to a {@link java.io.File} in XML format.
     *
     * @param dest The file in which to store the window hierarchy information.
     * @throws IOException if an I/O error occurs
     */
    public void dumpWindowHierarchy(@NonNull File dest) throws IOException {
        Log.d(TAG, String.format("Dumping window hierarchy to %s.", dest));
        try (OutputStream stream = new BufferedOutputStream(new FileOutputStream(dest))) {
            AccessibilityNodeInfoDumper.dumpWindowHierarchy(this, stream);
        }
    }

    /**
     * Dumps every window's layout hierarchy to an {@link java.io.OutputStream} in XML format.
     *
     * @param out The output stream that the window hierarchy information is written to.
     * @throws IOException if an I/O error occurs
     */
    public void dumpWindowHierarchy(@NonNull OutputStream out) throws IOException {
        Log.d(TAG, String.format("Dumping window hierarchy to %s.", out));
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
        try (Section ignored = Traces.trace("UiDevice#waitForWindowUpdate")) {
            if (packageName != null) {
                if (!packageName.equals(getCurrentPackageName())) {
                    Log.w(TAG, String.format("Skipping wait as package %s does not match current "
                            + "window %s.", packageName, getCurrentPackageName()));
                    return false;
                }
            }
            Runnable emptyRunnable = () -> {
            };
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
     *
     * @param cmd the command to run
     * @return the standard output of the command
     * @throws IOException if an I/O error occurs while reading output
     */
    @Discouraged(message = "Can be useful for simple commands, but lacks support for proper error"
            + " handling, input data, or complex commands (quotes, pipes) that can be obtained "
            + "from UiAutomation#executeShellCommandRwe or similar utilities.")
    @NonNull
    public String executeShellCommand(@NonNull String cmd) throws IOException {
        Log.d(TAG, String.format("Executing shell command: %s", cmd));
        try (ParcelFileDescriptor pfd = getUiAutomation().executeShellCommand(cmd);
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

    /**
     * Gets the display with {@code displayId}. The display may be null because it may be a private
     * virtual display, for example.
     */
    @Nullable
    Display getDisplayById(int displayId) {
        return mDisplayManager.getDisplay(displayId);
    }

    /**
     * Gets the size of the display with {@code displayId}, in pixels. The size is adjusted based
     * on the current orientation of the display.
     *
     * @see Display#getRealSize(Point)
     * @throws IllegalArgumentException when the display with {@code displayId} is not accessible.
     */
    Point getDisplaySize(int displayId) {
        Point p = new Point();
        Display display = getDisplayById(displayId);
        if (display == null) {
            throw new IllegalArgumentException(String.format("Display %d not found or not "
                    + "accessible", displayId));
        }
        display.getRealSize(p);
        return p;
    }

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
        return uiAutomation.getWindows();
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
        // Add all windows to support multi-window/display searches.
        for (final AccessibilityWindowInfo window : getWindows(uiAutomation)) {
            final AccessibilityNodeInfo root = window.getRoot();
            if (root == null) {
                Log.w(TAG, "Skipping null root node for window: " + window);
                continue;
            }
            roots.add(root);
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
                final Display display = getDisplayById(displayId);
                if (display != null) {
                    context = Api31Impl.createWindowContext(mInstrumentation.getContext(), display);
                } else {
                    // The display may be null because it may be private display, for example. In
                    // such a case, use the instrumentation's context instead.
                    context = mInstrumentation.getContext();
                }
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
            uiAutomation = Api24Impl.getUiAutomationWithRetry(getInstrumentation(), flags);
        } else {
            if (flags != Configurator.DEFAULT_UIAUTOMATION_FLAGS) {
                Log.w(TAG, "UiAutomation flags not supported prior to API 24");
            }
            uiAutomation = getInstrumentation().getUiAutomation();
        }

        if (uiAutomation == null) {
            throw new NullPointerException("Got null UiAutomation from instrumentation.");
        }

        // Verify and update the accessibility service flags if necessary. These might get reset
        // if the underlying UiAutomationConnection is recreated.
        AccessibilityServiceInfo serviceInfo = uiAutomation.getServiceInfo();
        if (serviceInfo == null) {
            Log.w(TAG, "Cannot verify accessibility service flags. "
                    + "Multi-window support (searching non-active windows) may be disabled.");
            return uiAutomation;
        }

        boolean serviceFlagsChanged = serviceInfo.flags != mCachedServiceFlags;
        if (serviceFlagsChanged
                || SystemClock.uptimeMillis() - mLastServiceFlagsTime > SERVICE_FLAGS_TIMEOUT) {
            // Enable multi-window support.
            serviceInfo.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            // Enable or disable hierarchy compression.
            if (mCompressed) {
                serviceInfo.flags &= ~AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
            } else {
                serviceInfo.flags |= AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
            }

            if (serviceFlagsChanged) {
                Log.d(TAG, String.format("Setting accessibility service flags: %d",
                        serviceInfo.flags));
            }
            uiAutomation.setServiceInfo(serviceInfo);
            mCachedServiceFlags = serviceInfo.flags;
            mLastServiceFlagsTime = SystemClock.uptimeMillis();
        }

        return uiAutomation;
    }

    QueryController getQueryController() {
        return mQueryController;
    }

    InteractionController getInteractionController() {
        return mInteractionController;
    }

    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() {
        }

        static UiAutomation getUiAutomationWithRetry(Instrumentation instrumentation, int flags) {
            UiAutomation uiAutomation = null;
            for (int i = 0; i < MAX_UIAUTOMATION_RETRY; i++) {
                uiAutomation = instrumentation.getUiAutomation(flags);
                if (uiAutomation != null) {
                    break;
                }
                if (i < MAX_UIAUTOMATION_RETRY - 1) {
                    Log.e(TAG, "Got null UiAutomation from instrumentation - Retrying...");
                    SystemClock.sleep(UIAUTOMATION_RETRY_INTERVAL);
                }
            }
            return uiAutomation;
        }
    }

    @RequiresApi(30)
    static class Api30Impl {
        private Api30Impl() {
        }

        static SparseArray<List<AccessibilityWindowInfo>> getWindowsOnAllDisplays(
                UiAutomation uiAutomation) {
            return uiAutomation.getWindowsOnAllDisplays();
        }
    }

    @RequiresApi(31)
    static class Api31Impl {
        private Api31Impl() {
        }

        static Context createWindowContext(Context context, Display display) {
            return context.createWindowContext(display,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, null);
        }
    }
}
