/*
 * Copyright 2024 The Android Open Source Project
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

import android.widget.Button;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Test;

/** Tests that UiAutomator can find objects and perform operations on Compose views. */
public class ComposeTest extends BaseTest {

    @Test
    public void testEndToEnd() {
        launchTestActivity(ComposeTestActivity.class);

        // Find object using its test tag.
        UiObject2 top = mDevice.findObject(By.res("top-text"));
        assertNotNull("Top text not found", top);

        // Scroll down a container.
        UiObject2 column = mDevice.findObject(By.scrollable(true));
        assertNotNull("Scrollable container not found", column);
        UiObject2 button = column.scrollUntil(Direction.DOWN,
                Until.findObject(By.clazz(Button.class)));
        assertNotNull("Button not found after scrolling", button);

        // Click and wait for change.
        UiObject2 text = mDevice.wait(Until.findObject(By.text("Initial")), TIMEOUT_MS);
        assertNotNull("Bottom text not found", text);
        button.click();
        assertTrue("Text not updated after click",
                text.wait(Until.textEquals("Updated"), TIMEOUT_MS));
    }
}
