/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.core.operations.layout.managers;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.LayoutComponent;
import androidx.compose.remote.core.operations.layout.modifiers.CollapsiblePriorityModifierOperation;

import java.util.ArrayList;

/** Utility class to manage collapsible priorities on components */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CollapsiblePriority {

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    private CollapsiblePriority() {}

    /**
     * Returns the priority of a child component
     *
     * @param c the child component
     * @return priority value, or 0f if not found
     */
    static float getPriority(Component c, int orientation) {
        if (c instanceof LayoutComponent) {
            LayoutComponent lc = (LayoutComponent) c;
            CollapsiblePriorityModifierOperation priority =
                    lc.selfOrModifier(CollapsiblePriorityModifierOperation.class);
            if (priority != null && priority.getOrientation() == orientation) {
                return priority.getPriority();
            }
        }
        return Float.MAX_VALUE;
    }

    /**
     * Returns the component that should stay last (the one with the highest priority number).
     *
     * @param components the list of components
     * @param orientation the orientation (HORIZONTAL or VERTICAL)
     * @return the last standing component, or the first one if same priority
     */
    static Component findLastStanding(ArrayList<Component> components, int orientation) {
        if (components.isEmpty()) {
            return null;
        }
        Component best = components.get(0);
        float maxPriority = getPriority(best, orientation);
        for (int i = 1; i < components.size(); i++) {
            Component c = components.get(i);
            float p = getPriority(c, orientation);
            if (p > maxPriority) {
                maxPriority = p;
                best = c;
            }
        }
        return best;
    }

    /**
     * Allocate and return a sorted array of components by their priorities
     *
     * @param components the children components
     * @return list of components sorted by their priority in decreasing order
     */
    static ArrayList<Component> sortWithPriorities(
            ArrayList<Component> components, int orientation) {
        ArrayList<Component> sorted = new ArrayList<>(components);
        sorted.sort(
                (t1, t2) -> {
                    float p1 = getPriority(t1, orientation);
                    float p2 = getPriority(t2, orientation);
                    return (int) (p2 - p1);
                });
        return sorted;
    }
}
