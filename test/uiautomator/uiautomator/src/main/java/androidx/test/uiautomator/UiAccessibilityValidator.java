/*
 * Copyright 2025 The Android Open Source Project
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

import android.view.accessibility.AccessibilityNodeInfo;

import org.jspecify.annotations.NonNull;

/**
 * A validator that runs during test actions to check the accessibility of the UI.
 *
 * <p>This interface can be implemented to provide custom accessibility checks. The {@link
 * #validate(AccessibilityNodeInfo)} method is called by {@link UiDevice} and {@link UiObject2}
 * before performing any action on a UI element, such as clicking or scrolling. The method is
 * provided with the {@link AccessibilityNodeInfo} of the element that is being interacted with.
 *
 * <p>Implementations of this interface may throw exceptions if an accessibility issue is found,
 * which will cause the test to fail.
 *
 * <p>To use a custom validator, set it on the {@link Configurator} instance using {@link
 * Configurator#addUiAccessibilityValidator(UiAccessibilityValidator)}.
 */
public interface UiAccessibilityValidator {
    /**
     * Runs a check on the given {@link AccessibilityNodeInfo}.
     *
     * <p>This method is called before any action is performed on the UI element represented by the
     * given {@code node}. Implementations may check the node for any accessibility issues and
     * throw an exception if any are found.
     *
     * @param node The {@link AccessibilityNodeInfo} of the UI element to validate.
     */
    void validate(@NonNull AccessibilityNodeInfo node);
}
