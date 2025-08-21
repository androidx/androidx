/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.player.view.accessibility;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.RestrictTo;

import android.graphics.PointF;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.RemoteContextActions;
import androidx.compose.remote.core.operations.layout.ClickModifierOperation;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.LayoutComponent;
import androidx.compose.remote.core.operations.layout.RootLayoutComponent;
import androidx.compose.remote.core.operations.layout.modifiers.ComponentModifiers;
import androidx.compose.remote.core.operations.layout.modifiers.ModifierOperation;
import androidx.compose.remote.core.semantics.AccessibilitySemantics;
import androidx.compose.remote.core.semantics.AccessibleComponent;
import androidx.compose.remote.core.semantics.CoreSemantics;
import androidx.compose.remote.core.semantics.ScrollableComponent.ScrollDirection;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Java Player implementation of the {@link RemoteComposeDocumentAccessibility} interface. Each item
 * in the semantic tree is a {@link Component} from the remote Compose UI. Each Component can have a
 * list of modifiers that must be tagged with {@link AccessibilitySemantics} either incidentally
 * (see {@link ClickModifierOperation}) or explicitly (see {@link CoreSemantics}).
 */
@RestrictTo(LIBRARY_GROUP)
public class CoreDocumentAccessibility implements RemoteComposeDocumentAccessibility {
    private final CoreDocument mDocument;
    private final RemoteContextActions mRemoteContextActions;

    public CoreDocumentAccessibility(
            CoreDocument document, RemoteContextActions mRemoteContextActions) {
        this.mDocument = document;
        this.mRemoteContextActions = mRemoteContextActions;
    }

    @Nullable
    @Override
    public Integer getComponentIdAt(PointF point) {
        return RootId;
    }

    @Override
    public @Nullable Component findComponentById(int id) {
        RootLayoutComponent root = mDocument.getRootLayoutComponent();

        if (root == null || id == -1) {
            return root;
        }

        return componentStream(root)
                .filter(op -> op.getComponentId() == id)
                .findFirst()
                .orElse(null);
    }

    @Override
    public CoreSemantics.Mode mergeMode(Component component) {
        if (!(component instanceof LayoutComponent)) {
            return CoreSemantics.Mode.SET;
        }

        CoreSemantics.Mode result = CoreSemantics.Mode.SET;

        for (ModifierOperation modifier :
                ((LayoutComponent) component).getComponentModifiers().getList()) {
            if (modifier instanceof AccessibleComponent) {
                AccessibleComponent semantics = (AccessibleComponent) modifier;

                if (semantics.getMode().ordinal() > result.ordinal()) {
                    result = semantics.getMode();
                }
            }
        }

        return result;
    }

    /**
     * Performs the given accessibility action on the specified component.
     *
     * @param component The component on which to perform the action.
     * @param action The accessibility action to perform.
     * @param arguments Optional arguments for the action.
     * @return True if the action was successfully performed, false otherwise.
     */
    public boolean performAction(Component component, int action, Bundle arguments) {
        boolean needsRepaint = true;

        try {
            if (isClickAction(action)) {
                return performClick(component);
            } else if (isScrollForwardAction(action)) {
                return scrollDirection(component, ScrollDirection.FORWARD);
            } else if (isScrollBackwardAction(action)) {
                return scrollDirection(component, ScrollDirection.BACKWARD);
            } else if (isShowOnScreenAction(action)) {
                return showOnScreen(component);
            } else {
                needsRepaint = false;
                return false;
            }
        } finally {
            if (needsRepaint) {
                mDocument.needsRepaint();
            }
        }
    }

    private static boolean isShowOnScreenAction(int action) {
        return action == android.R.id.accessibilityActionShowOnScreen;
    }

    private static boolean isScrollBackwardAction(int action) {
        return action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                || action == android.R.id.accessibilityActionScrollUp
                || action == android.R.id.accessibilityActionScrollLeft;
    }

    private static boolean isScrollForwardAction(int action) {
        return action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                || action == android.R.id.accessibilityActionScrollDown
                || action == android.R.id.accessibilityActionScrollRight;
    }

    private static boolean isClickAction(int action) {
        return action == AccessibilityNodeInfo.ACTION_CLICK;
    }

    private boolean showOnScreen(Component component) {
        return mRemoteContextActions.showOnScreen(component);
    }

    /**
     * scroll content by the given offset
     *
     * @param component
     * @param pixels
     * @return
     */
    public int scrollByOffset(@NonNull Component component, int pixels) {
        return mRemoteContextActions.scrollByOffset(component, pixels);
    }

    /**
     * scroll content in a given direction
     *
     * @param component
     * @param direction
     * @return
     */
    public boolean scrollDirection(@NonNull Component component, ScrollDirection direction) {
        return mRemoteContextActions.scrollDirection(component, direction);
    }

    /**
     * Perform a click on the given component
     *
     * @param component
     * @return
     */
    public boolean performClick(@NonNull Component component) {
        return mRemoteContextActions.performClick(mDocument, component, "");
    }

    @Nullable
    @Override
    public String stringValue(int id) {
        Object value = mDocument.getRemoteComposeState().getFromId(id);

        return value != null ? String.valueOf(value) : null;
    }

    @Override
    public List<AccessibilitySemantics> semanticModifiersForComponent(Component component) {
        if (!(component instanceof LayoutComponent)) {
            return Collections.emptyList();
        }

        List<ModifierOperation> modifiers =
                ((LayoutComponent) component).getComponentModifiers().getList();

        return modifiers.stream()
                .filter(
                        it ->
                                it instanceof AccessibilitySemantics
                                        && ((AccessibilitySemantics) it)
                                                .isInterestingForSemantics())
                .map(i -> (AccessibilitySemantics) i)
                .collect(Collectors.toList());
    }

    @Override
    public List<Integer> semanticallyRelevantChildComponents(
            Component component, boolean useUnmergedTree) {
        if (!component.isVisible()) {
            return Collections.emptyList();
        }

        CoreSemantics.Mode mergeMode = mergeMode(component);
        if (mergeMode == CoreSemantics.Mode.CLEAR_AND_SET
                || (!useUnmergedTree && mergeMode == CoreSemantics.Mode.MERGE)) {
            return Collections.emptyList();
        }

        ArrayList<Integer> result = new ArrayList<>();

        for (Operation child : component.mList) {
            if (child instanceof Component) {
                if (isInteresting((Component) child)) {
                    result.add(((Component) child).getComponentId());
                } else {
                    result.addAll(
                            semanticallyRelevantChildComponents(
                                    (Component) child, useUnmergedTree));
                }
            }
        }

        return result;
    }

    static Stream<Component> componentStream(Component root) {
        return Stream.concat(
                Stream.of(root),
                root.mList.stream()
                        .flatMap(
                                op -> {
                                    if (op instanceof Component) {
                                        return componentStream((Component) op);
                                    } else {
                                        return Stream.empty();
                                    }
                                }));
    }

    static Stream<ModifierOperation> modifiersStream(Component component) {
        return component.mList.stream()
                .filter(it -> it instanceof ComponentModifiers)
                .flatMap(it -> ((ComponentModifiers) it).getList().stream());
    }

    static boolean isInteresting(Component component) {
        if (!component.isVisible()) {
            return false;
        }

        return isContainerWithSemantics(component)
                || modifiersStream(component)
                        .anyMatch(CoreDocumentAccessibility::isModifierWithSemantics);
    }

    static boolean isModifierWithSemantics(ModifierOperation modifier) {
        return modifier instanceof AccessibilitySemantics
                && ((AccessibilitySemantics) modifier).isInterestingForSemantics();
    }

    static boolean isContainerWithSemantics(Component component) {
        if (component instanceof AccessibilitySemantics) {
            return ((AccessibilitySemantics) component).isInterestingForSemantics();
        }

        if (!(component instanceof LayoutComponent)) {
            return false;
        }

        return ((LayoutComponent) component)
                .getComponentModifiers().getList().stream()
                        .anyMatch(CoreDocumentAccessibility::isModifierWithSemantics);
    }
}
