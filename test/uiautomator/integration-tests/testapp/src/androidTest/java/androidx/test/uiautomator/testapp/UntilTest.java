/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.test.uiautomator.testapp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for {@link Until}, and related methods like
 * {@link UiDevice#wait(SearchCondition, long)} and
 * {@link UiObject2#wait(UiObject2Condition, long)}.
 */
@LargeTest
public class UntilTest extends BaseTest {

    private static final long DELAY_MS = 1_000;
    private static final long TIMEOUT_MS = 5_000;

    @Test
    public void testGone() {
        launchTestActivity(UntilTestActivity.class);
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "gone_button"));
        BySelector target = By.res(TEST_APP, "gone_target");

        waitForCondition(() -> {
            assertTrue(mDevice.wait(Until.gone(target), TIMEOUT_MS));
        }, button::click);
    }

    @Test
    public void testHasObject() {
        launchTestActivity(UntilTestActivity.class);
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "find_button"));
        BySelector target = By.res(TEST_APP, "find_target");

        waitForCondition(() -> {
            assertTrue(mDevice.wait(Until.hasObject(target), TIMEOUT_MS));
        }, button::click);
    }

    @Test
    public void testFindObject() {
        launchTestActivity(UntilTestActivity.class);
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "find_button"));
        BySelector target = By.res(TEST_APP, "find_target");

        waitForCondition(() -> {
            assertNotNull(mDevice.wait(Until.findObject(target), TIMEOUT_MS));
        }, button::click);
    }

    @Test
    public void testFindObjects() {
        launchTestActivity(UntilTestActivity.class);
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "find_button"));
        BySelector target = By.res(TEST_APP, "find_target");

        waitForCondition(() -> {
            assertNotNull(mDevice.wait(Until.findObjects(target), TIMEOUT_MS));
        }, button::click);
    }

    @Test
    public void testChecked() {
        launchTestActivity(UntilTestActivity.class);
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "checked_button"));
        UiObject2 target = mDevice.findObject(By.res(TEST_APP, "checked_target"));

        waitForCondition(() -> {
            assertTrue(target.wait(Until.checked(true), TIMEOUT_MS));
        }, button::click);
    }

    @Test
    public void testClickable() {
        launchTestActivity(UntilTestActivity.class);
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "clickable_button"));
        UiObject2 target = mDevice.findObject(By.res(TEST_APP, "clickable_target"));

        waitForCondition(() -> {
            assertTrue(target.wait(Until.clickable(true), TIMEOUT_MS));
        }, button::click);
    }

    @Test
    public void testEnabled() {
        launchTestActivity(UntilTestActivity.class);
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "enabled_button"));
        UiObject2 target = mDevice.findObject(By.res(TEST_APP, "enabled_target"));

        waitForCondition(() -> {
            assertTrue(target.wait(Until.enabled(true), TIMEOUT_MS));
        }, button::click);
    }

    @Test
    public void testFocusable() {
        launchTestActivity(UntilTestActivity.class);
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "focusable_button"));
        UiObject2 target = mDevice.findObject(By.res(TEST_APP, "focusable_target"));

        waitForCondition(() -> {
            assertTrue(target.wait(Until.focusable(true), TIMEOUT_MS));
        }, button::click);
    }

    @Test
    public void testFocused() {
        launchTestActivity(UntilTestActivity.class);
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "focused_button"));
        UiObject2 target = mDevice.findObject(By.res(TEST_APP, "focused_target"));

        waitForCondition(() -> {
            assertTrue(target.wait(Until.focused(true), TIMEOUT_MS));
        }, button::click);
    }

    @Test
    public void testLongClickable() {
        launchTestActivity(UntilTestActivity.class);
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "long_clickable_button"));
        UiObject2 target = mDevice.findObject(By.res(TEST_APP, "long_clickable_target"));

        waitForCondition(() -> {
            assertTrue(target.wait(Until.longClickable(true), TIMEOUT_MS));
        }, button::click);
    }

    @Test
    public void testScrollable() {
        launchTestActivity(UntilTestActivity.class);
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "scrollable_button"));
        UiObject2 target = mDevice.findObject(By.res(TEST_APP, "scrollable_target"));

        waitForCondition(() -> {
            assertTrue(target.wait(Until.scrollable(true), TIMEOUT_MS));
        }, button::click);
    }

    @Test
    public void testSelected() {
        launchTestActivity(UntilTestActivity.class);
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "selected_button"));
        UiObject2 target = mDevice.findObject(By.res(TEST_APP, "selected_target"));

        waitForCondition(() -> {
            assertTrue(target.wait(Until.selected(true), TIMEOUT_MS));
        }, button::click);
    }

    @Test
    public void testDescMatches() {
        launchTestActivity(UntilTestActivity.class);
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "desc_button"));
        UiObject2 target = mDevice.findObject(By.res(TEST_APP, "desc_target"));

        waitForCondition(() -> {
            assertTrue(target.wait(Until.descMatches("update.*desc$"), TIMEOUT_MS));
        }, button::click);
    }

    @Test
    public void testDescEquals() {
        launchTestActivity(UntilTestActivity.class);
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "desc_button"));
        UiObject2 target = mDevice.findObject(By.res(TEST_APP, "desc_target"));

        waitForCondition(() -> {
            assertTrue(target.wait(Until.descEquals("updated_desc"), TIMEOUT_MS));
        }, button::click);
    }

    @Test
    public void testDescContains() {
        launchTestActivity(UntilTestActivity.class);
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "desc_button"));
        UiObject2 target = mDevice.findObject(By.res(TEST_APP, "desc_target"));

        waitForCondition(() -> {
            assertTrue(target.wait(Until.descContains("updated"), TIMEOUT_MS));
        }, button::click);
    }

    @Test
    public void testDescStartsWith() {
        launchTestActivity(UntilTestActivity.class);
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "desc_button"));
        UiObject2 target = mDevice.findObject(By.res(TEST_APP, "desc_target"));

        waitForCondition(() -> {
            assertTrue(target.wait(Until.descStartsWith("updated"), TIMEOUT_MS));
        }, button::click);
    }

    @Test
    public void testDescEndsWith() {
        launchTestActivity(UntilTestActivity.class);
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "desc_button"));
        UiObject2 target = mDevice.findObject(By.res(TEST_APP, "desc_target"));

        waitForCondition(() -> {
            assertTrue(target.wait(Until.descEndsWith("ed_desc"), TIMEOUT_MS));
        }, button::click);
    }

    @Test
    public void testTextMatches() {
        launchTestActivity(UntilTestActivity.class);
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "text_button"));
        UiObject2 target = mDevice.findObject(By.res(TEST_APP, "text_target"));

        waitForCondition(() -> {
            assertTrue(target.wait(Until.textMatches("update.*text$"), TIMEOUT_MS));
        }, button::click);
    }

    @Test
    public void testTextNotEquals() {
        launchTestActivity(UntilTestActivity.class);
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "text_button"));
        UiObject2 target = mDevice.findObject(By.res(TEST_APP, "text_target"));

        waitForCondition(() -> {
            assertTrue(target.wait(Until.textNotEquals("initial_text"), TIMEOUT_MS));
        }, button::click);
    }

    @Test
    public void testTextEquals() {
        launchTestActivity(UntilTestActivity.class);
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "text_button"));
        UiObject2 target = mDevice.findObject(By.res(TEST_APP, "text_target"));

        waitForCondition(() -> {
            assertTrue(target.wait(Until.textEquals("updated_text"), TIMEOUT_MS));
        }, button::click);
    }

    @Test
    public void testTextContains() {
        launchTestActivity(UntilTestActivity.class);
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "text_button"));
        UiObject2 target = mDevice.findObject(By.res(TEST_APP, "text_target"));

        waitForCondition(() -> {
            assertTrue(target.wait(Until.textContains("updated"), TIMEOUT_MS));
        }, button::click);
    }

    @Test
    public void testTextStartsWith() {
        launchTestActivity(UntilTestActivity.class);
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "text_button"));
        UiObject2 target = mDevice.findObject(By.res(TEST_APP, "text_target"));

        waitForCondition(() -> {
            assertTrue(target.wait(Until.textStartsWith("updated"), TIMEOUT_MS));
        }, button::click);
    }

    @Test
    public void testTextEndsWith() {
        launchTestActivity(UntilTestActivity.class);
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "text_button"));
        UiObject2 target = mDevice.findObject(By.res(TEST_APP, "text_target"));

        waitForCondition(() -> {
            assertTrue(target.wait(Until.textEndsWith("ed_text"), TIMEOUT_MS));
        }, button::click);
    }

    /** Verifies that a condition is met after the required action is executed. */
    private void waitForCondition(Runnable blockingCondition, Runnable actionToUnblock) {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(0);
        try {
            long startTime = System.currentTimeMillis();
            executorService.schedule(actionToUnblock, DELAY_MS, TimeUnit.MILLISECONDS);
            blockingCondition.run();
            long duration = System.currentTimeMillis() - startTime;
            assertTrue("Condition unblocked before action", DELAY_MS < duration);
        } finally {
            executorService.shutdownNow();
        }
    }
}
