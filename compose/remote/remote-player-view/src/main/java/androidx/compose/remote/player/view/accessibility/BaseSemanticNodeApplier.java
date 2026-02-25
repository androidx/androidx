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

import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.LayoutComponent;
import androidx.compose.remote.core.semantics.AccessibilitySemantics;
import androidx.compose.remote.core.semantics.AccessibleComponent;
import androidx.compose.remote.core.semantics.CoreSemantics;
import androidx.compose.remote.core.semantics.ScrollableComponent;
import androidx.compose.remote.core.semantics.ScrollableComponent.ScrollAxisRange;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Base class for applying semantic information to a node.
 *
 * <p>This class provides common functionality for applying semantic information extracted from
 * Compose UI components to a node representation used for accessibility purposes. It handles
 * applying properties like content description, text, role, clickability, and bounds.
 *
 * <p>Subclasses are responsible for implementing methods to actually set these properties on the
 * specific node type they handle.
 *
 * @param <N> The type of node this applier works with.
 */
@RestrictTo(LIBRARY_GROUP)
public abstract class BaseSemanticNodeApplier<N> implements SemanticNodeApplier<N> {
    private static final String LOG_TAG = "RemoteCompose";

    @Override
    public void applyComponent(
            @NonNull RemoteComposeDocumentAccessibility remoteComposeAccessibility,
            @NonNull N nodeInfo, @NonNull Component component,
            @NonNull List<AccessibilitySemantics> semantics,
            @Nullable Integer parentId) {
        setBoundsInParentOrScreen(nodeInfo, component, parentId);

        setUniqueId(nodeInfo, String.valueOf(component.getComponentId()));

        if (component instanceof AccessibleComponent) {
            applyContentDescription(((AccessibleComponent) component).getContentDescriptionId(),
                    nodeInfo, remoteComposeAccessibility);

            applyText(((AccessibleComponent) component).getTextId(), nodeInfo,
                    remoteComposeAccessibility);

            applyRole(((AccessibleComponent) component).getRole(), nodeInfo);
        }

        applySemantics(remoteComposeAccessibility, nodeInfo, semantics);

        if (getText(nodeInfo) == null && getContentDescription(nodeInfo) == null) {
            setContentDescription(nodeInfo, "");
        }

        if (component.getParent() instanceof LayoutComponent) {
            LayoutComponent parent = (LayoutComponent) component.getParent();
            ScrollableComponent scrollable = parent.selfOrModifier(ScrollableComponent.class);

            if (scrollable != null) {
                applyListItem(nodeInfo, parent.getComponentId());
            }
        }
    }

    protected abstract void setBoundsInParentOrScreen(@NonNull N nodeInfo,
            @NonNull Component component,
            @Nullable Integer parentId);

    protected void applySemantics(
            @NonNull RemoteComposeDocumentAccessibility remoteComposeAccessibility,
            @NonNull N nodeInfo, @NonNull List<AccessibilitySemantics> semantics) {
        for (AccessibilitySemantics semantic : semantics) {
            if (semantic.isInterestingForSemantics()) {
                if (semantic instanceof CoreSemantics) {
                    CoreSemantics coreSemantics = (CoreSemantics) semantic;
                    applyCoreSemantics(remoteComposeAccessibility, nodeInfo, coreSemantics);
                } else if (semantic instanceof AccessibleComponent) {
                    AccessibleComponent accessibleComponent = (AccessibleComponent) semantic;
                    if (accessibleComponent.isClickable()) {
                        setClickable(nodeInfo, true);
                    }

                    if (accessibleComponent.getContentDescriptionId() != null) {
                        applyContentDescription(accessibleComponent.getContentDescriptionId(),
                                nodeInfo, remoteComposeAccessibility);
                    }

                    if (accessibleComponent.getTextId() != null) {
                        applyText(accessibleComponent.getTextId(), nodeInfo,
                                remoteComposeAccessibility);
                    }

                    applyRole(accessibleComponent.getRole(), nodeInfo);
                } else if (semantic instanceof ScrollableComponent) {
                    ScrollableComponent scrollableSemantic = (ScrollableComponent) semantic;

                    if (scrollableSemantic.supportsScrollByOffset()) {
                        ScrollAxisRange scrollAxis = scrollableSemantic.getScrollAxisRange();
                        if (scrollAxis != null) {
                            applyScrollable(nodeInfo, scrollAxis,
                                    scrollableSemantic.scrollDirection());
                        }
                    }
                } else {
                    Log.w(LOG_TAG, "Unknown semantic: " + semantic);
                }
            }
        }
    }

    protected void applyCoreSemantics(
            @NonNull RemoteComposeDocumentAccessibility remoteComposeAccessibility,
            @NonNull N nodeInfo, @NonNull CoreSemantics coreSemantics) {
        applyContentDescription(coreSemantics.getContentDescriptionId(), nodeInfo,
                remoteComposeAccessibility);

        applyRole(coreSemantics.getRole(), nodeInfo);

        applyText(coreSemantics.getTextId(), nodeInfo, remoteComposeAccessibility);

        applyStateDescription(coreSemantics.getStateDescriptionId(), nodeInfo,
                remoteComposeAccessibility);

        if (!coreSemantics.mEnabled) {
            setEnabled(nodeInfo, false);
        }
    }

    protected void applyStateDescription(@Nullable Integer stateDescriptionId, @NonNull N nodeInfo,
            @NonNull RemoteComposeDocumentAccessibility remoteComposeAccessibility) {
        if (stateDescriptionId != null) {
            setStateDescription(nodeInfo, appendNullable(getStateDescription(nodeInfo),
                    remoteComposeAccessibility.stringValue(stateDescriptionId)));
        }
    }

    protected void applyRole(AccessibleComponent.@Nullable Role role, @NonNull N nodeInfo) {
        String description = role != null ? role.getDescription() : null;
        if (description != null) {
            setRoleDescription(nodeInfo, description);
        }
    }

    protected void applyText(@Nullable Integer textId, @NonNull N nodeInfo,
            @NonNull RemoteComposeDocumentAccessibility remoteComposeAccessibility) {
        if (textId != null) {
            String value = remoteComposeAccessibility.stringValue(textId);
            setText(nodeInfo, appendNullable(getText(nodeInfo), value));
        }
    }

    protected void applyContentDescription(@Nullable Integer contentDescriptionId,
            @NonNull N nodeInfo,
            @NonNull RemoteComposeDocumentAccessibility remoteComposeAccessibility) {
        if (contentDescriptionId != null) {
            setContentDescription(nodeInfo, appendNullable(getContentDescription(nodeInfo),
                    remoteComposeAccessibility.stringValue(contentDescriptionId)));
        }
    }

    private @Nullable CharSequence appendNullable(@Nullable CharSequence contentDescription,
            @Nullable String value) {
        if (contentDescription == null) {
            return value;
        } else if (value == null) {
            return contentDescription;
        } else {
            return contentDescription + " " + value;
        }
    }

    protected abstract void setClickable(@NonNull N nodeInfo, boolean b);

    protected abstract void setEnabled(@NonNull N nodeInfo, boolean b);

    protected abstract @Nullable CharSequence getStateDescription(@NonNull N nodeInfo);

    protected abstract void setStateDescription(@NonNull N nodeInfo,
            @NonNull CharSequence charSequence);

    protected abstract void setRoleDescription(@NonNull N nodeInfo, @NonNull String description);

    protected abstract @Nullable CharSequence getText(@NonNull N nodeInfo);

    protected abstract void setText(@NonNull N nodeInfo, @NonNull CharSequence charSequence);

    protected abstract @Nullable CharSequence getContentDescription(@NonNull N nodeInfo);

    protected abstract void setContentDescription(@NonNull N nodeInfo,
            @Nullable CharSequence charSequence);

    protected abstract void setUniqueId(@NonNull N nodeInfo, @NonNull String s);

    protected abstract void applyScrollable(@NonNull N nodeInfo,
            @NonNull ScrollAxisRange scrollAxis, int scrollDirection);

    protected abstract void applyListItem(@NonNull N nodeInfo, int parentId);
}
