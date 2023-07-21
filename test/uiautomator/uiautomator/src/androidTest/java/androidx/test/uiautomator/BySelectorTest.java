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

package androidx.test.uiautomator;

import static org.junit.Assert.assertThrows;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.regex.Pattern;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class BySelectorTest {

    private static final BySelector SELECTOR = By.res("id");

    @Test
    public void testClazz_nullValue() {
        assertThrows(NullPointerException.class, () -> By.clazz((String) null));
        assertThrows(NullPointerException.class, () -> By.clazz(null, "className"));
        assertThrows(NullPointerException.class, () -> By.clazz("packageName", null));
        assertThrows(NullPointerException.class, () -> By.clazz((Class) null));
        assertThrows(NullPointerException.class, () -> By.clazz((Pattern) null));
    }

    @Test(expected = IllegalStateException.class)
    public void testClazz_alreadyDefined() {
        By.clazz("first").clazz("second");
    }

    @Test
    public void testDesc_nullValue() {
        assertThrows(NullPointerException.class, () -> By.desc((String) null));
        assertThrows(NullPointerException.class, () -> By.descContains(null));
        assertThrows(NullPointerException.class, () -> By.descStartsWith(null));
        assertThrows(NullPointerException.class, () -> By.descEndsWith(null));
        assertThrows(NullPointerException.class, () -> By.desc((Pattern) null));
    }

    @Test(expected = IllegalStateException.class)
    public void testDesc_alreadyDefined() {
        By.desc("first").descStartsWith("second");
    }

    @Test
    public void testPkg_nullValue() {
        assertThrows(NullPointerException.class, () -> By.pkg((String) null));
        assertThrows(NullPointerException.class, () -> By.pkg((Pattern) null));
    }

    @Test(expected = IllegalStateException.class)
    public void testPkg_alreadyDefined() {
        By.pkg("first").pkg("second");
    }

    @Test
    public void testRes_nullValue() {
        assertThrows(NullPointerException.class, () -> By.res((String) null));
        assertThrows(NullPointerException.class, () -> By.res(null, "resourceId"));
        assertThrows(NullPointerException.class, () -> By.res("resourcePackage", null));
        assertThrows(NullPointerException.class, () -> By.res((Pattern) null));
    }

    @Test(expected = IllegalStateException.class)
    public void testRes_alreadyDefined() {
        By.res("first").res("second");
    }

    @Test
    public void testText_nullValue() {
        assertThrows(NullPointerException.class, () -> By.text((String) null));
        assertThrows(NullPointerException.class, () -> By.textContains(null));
        assertThrows(NullPointerException.class, () -> By.textStartsWith(null));
        assertThrows(NullPointerException.class, () -> By.textEndsWith(null));
        assertThrows(NullPointerException.class, () -> By.text((Pattern) null));
    }

    @Test(expected = IllegalStateException.class)
    public void testText_alreadyDefined() {
        By.text("first").textStartsWith("second");
    }

    @Test(expected = IllegalStateException.class)
    public void testCheckable_alreadyDefined() {
        By.checkable(true).checkable(false);
    }

    @Test(expected = IllegalStateException.class)
    public void testChecked_alreadyDefined() {
        By.checked(true).checked(false);
    }

    @Test(expected = IllegalStateException.class)
    public void testClickable_alreadyDefined() {
        By.clickable(true).clickable(false);
    }

    @Test(expected = IllegalStateException.class)
    public void testEnabled_alreadyDefined() {
        By.enabled(true).enabled(false);
    }

    @Test(expected = IllegalStateException.class)
    public void testFocusable_alreadyDefined() {
        By.focusable(true).focusable(false);
    }

    @Test(expected = IllegalStateException.class)
    public void testFocused_alreadyDefined() {
        By.focused(true).focused(false);
    }

    @Test(expected = IllegalStateException.class)
    public void testLongClickable_alreadyDefined() {
        By.longClickable(true).longClickable(false);
    }

    @Test(expected = IllegalStateException.class)
    public void testScrollable_alreadyDefined() {
        By.scrollable(true).scrollable(false);
    }

    @Test(expected = IllegalStateException.class)
    public void testSelected_alreadyDefined() {
        By.selected(true).selected(false);
    }

    @Test(expected = IllegalStateException.class)
    public void testDepth_alreadyDefined() {
        By.depth(1).depth(2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDepth_negativeValue() {
        By.depth(-1);
    }

    @Test(expected = NullPointerException.class)
    public void testHasParent_nullValue() {
        By.hasParent(null);
    }

    @Test(expected = IllegalStateException.class)
    public void testHasParent_alreadyDefined() {
        By.hasParent(SELECTOR).hasParent(SELECTOR);
    }

    @Test
    public void testHasAncestor_nullValue() {
        assertThrows(NullPointerException.class, () -> By.hasAncestor(null));
        assertThrows(NullPointerException.class, () -> By.hasAncestor(null, 1));
    }

    @Test
    public void testHasAncestor_alreadyDefined() {
        assertThrows(IllegalStateException.class,
                () -> By.hasAncestor(SELECTOR).hasAncestor(SELECTOR));
        assertThrows(IllegalStateException.class,
                () -> By.hasAncestor(SELECTOR).hasAncestor(SELECTOR, 1));
    }

    @Test(expected = NullPointerException.class)
    public void testHasChild_nullValue() {
        By.hasChild(null);
    }

    @Test
    public void testHasChild_nestedAncestor() {
        assertThrows(IllegalArgumentException.class, () -> By.hasChild(By.hasParent(SELECTOR)));
        assertThrows(IllegalArgumentException.class, () -> By.hasChild(By.hasAncestor(SELECTOR)));
    }

    @Test
    public void testHasDescendant_nullValue() {
        assertThrows(NullPointerException.class, () -> By.hasDescendant(null));
        assertThrows(NullPointerException.class, () -> By.hasDescendant(null, 0));
    }

    @Test
    public void testHasDescendant_nestedAncestor() {
        assertThrows(IllegalArgumentException.class,
                () -> By.hasDescendant(By.hasParent(SELECTOR)));
        assertThrows(IllegalArgumentException.class,
                () -> By.hasDescendant(By.hasAncestor(SELECTOR)));
    }
}
