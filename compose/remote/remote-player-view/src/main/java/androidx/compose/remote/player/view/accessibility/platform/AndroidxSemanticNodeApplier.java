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
package androidx.compose.remote.player.view.accessibility.platform;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.operations.RootContentBehavior;
import androidx.compose.remote.core.semantics.ScrollableComponent;
import androidx.compose.remote.player.view.accessibility.BaseSemanticNodeApplier;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;

import java.util.List;

@RestrictTo(LIBRARY_GROUP)
public class AndroidxSemanticNodeApplier
        extends BaseSemanticNodeApplier<AccessibilityNodeInfoCompat> {
    private final View mPlayer;

    public AndroidxSemanticNodeApplier(View player) {
        this.mPlayer = player;
    }

    @Override
    protected void setClickable(AccessibilityNodeInfoCompat nodeInfo, boolean clickable) {
        nodeInfo.setClickable(clickable);
        if (clickable) {
            nodeInfo.addAction(AccessibilityActionCompat.ACTION_CLICK);
        } else {
            nodeInfo.removeAction(AccessibilityActionCompat.ACTION_CLICK);
        }
    }

    @Override
    protected void setEnabled(AccessibilityNodeInfoCompat nodeInfo, boolean enabled) {
        nodeInfo.setEnabled(enabled);
    }

    @Override
    protected CharSequence getStateDescription(AccessibilityNodeInfoCompat nodeInfo) {
        return nodeInfo.getStateDescription();
    }

    @Override
    protected void setStateDescription(
            AccessibilityNodeInfoCompat nodeInfo, CharSequence description) {
        nodeInfo.setStateDescription(description);
    }

    @Override
    protected void setRoleDescription(AccessibilityNodeInfoCompat nodeInfo, String description) {
        nodeInfo.setRoleDescription(description);
    }

    @Override
    protected CharSequence getText(AccessibilityNodeInfoCompat nodeInfo) {
        return nodeInfo.getText();
    }

    @Override
    protected void setText(AccessibilityNodeInfoCompat nodeInfo, CharSequence text) {
        nodeInfo.setText(text);
    }

    @Override
    protected CharSequence getContentDescription(AccessibilityNodeInfoCompat nodeInfo) {
        return nodeInfo.getContentDescription();
    }

    @Override
    protected void setContentDescription(
            AccessibilityNodeInfoCompat nodeInfo, CharSequence description) {
        nodeInfo.setContentDescription(description);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void setBoundsInScreen(AccessibilityNodeInfoCompat nodeInfo, Rect bounds) {
        // setBoundsInParent() is a deprecated method, however
        // ExploreByTouchHelper.createNodeForChild() relies on the bounds in parent being set.
        nodeInfo.setBoundsInParent(new Rect(0, 0, 1, 1));
        nodeInfo.setBoundsInScreen(bounds);
    }

    @Override
    protected void setUniqueId(AccessibilityNodeInfoCompat nodeInfo, String id) {
        nodeInfo.setUniqueId(id);
        nodeInfo.setSource(mPlayer, Integer.parseInt(id));
    }

    @Override
    protected void applyScrollable(
            AccessibilityNodeInfoCompat nodeInfo,
            ScrollableComponent.ScrollAxisRange scrollAxis,
            int scrollDirection) {
        nodeInfo.setScrollable(true);
        nodeInfo.addAction(AccessibilityActionCompat.ACTION_SCROLL_TO_POSITION);
        nodeInfo.addAction(AccessibilityActionCompat.ACTION_SET_PROGRESS);

        nodeInfo.setGranularScrollingSupported(true);

        if (scrollAxis.canScrollForward()) {
            nodeInfo.addAction(AccessibilityActionCompat.ACTION_SCROLL_FORWARD);
            if (scrollDirection == RootContentBehavior.SCROLL_VERTICAL) {
                nodeInfo.addAction(AccessibilityActionCompat.ACTION_SCROLL_DOWN);
                nodeInfo.addAction(AccessibilityActionCompat.ACTION_PAGE_DOWN);
            } else if (scrollDirection == RootContentBehavior.SCROLL_HORIZONTAL) {
                // TODO handle RTL
                nodeInfo.addAction(AccessibilityActionCompat.ACTION_SCROLL_RIGHT);
                nodeInfo.addAction(AccessibilityActionCompat.ACTION_PAGE_RIGHT);
            }
        }

        if (scrollAxis.canScrollBackwards()) {
            nodeInfo.addAction(AccessibilityActionCompat.ACTION_SCROLL_BACKWARD);
            if (scrollDirection == RootContentBehavior.SCROLL_VERTICAL) {
                nodeInfo.addAction(AccessibilityActionCompat.ACTION_SCROLL_UP);
                nodeInfo.addAction(AccessibilityActionCompat.ACTION_PAGE_UP);
            } else if (scrollDirection == RootContentBehavior.SCROLL_HORIZONTAL) {
                // TODO handle RTL
                nodeInfo.addAction(AccessibilityActionCompat.ACTION_SCROLL_LEFT);
                nodeInfo.addAction(AccessibilityActionCompat.ACTION_PAGE_LEFT);
            }
        }

        if (scrollDirection == RootContentBehavior.SCROLL_HORIZONTAL) {
            nodeInfo.setCollectionInfo(
                    AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(1, -1, false));
            nodeInfo.setClassName("android.widget.HorizontalScrollView");
        } else {
            nodeInfo.setCollectionInfo(
                    AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(-1, 1, false));
            nodeInfo.setClassName("android.widget.ScrollView");
        }
    }

    @Override
    protected void applyListItem(AccessibilityNodeInfoCompat nodeInfo, int parentId) {
        nodeInfo.addAction(AccessibilityActionCompat.ACTION_SHOW_ON_SCREEN);
        nodeInfo.setScreenReaderFocusable(true);
        nodeInfo.setFocusable(true);
        nodeInfo.setParent(mPlayer, parentId);

        // TODO correct values
        nodeInfo.setCollectionItemInfo(
                AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(1, 1, 0, 1, false));
    }

    @Override
    public void addChildren(AccessibilityNodeInfoCompat nodeInfo, List<Integer> childIds) {
        for (int id : childIds) {
            nodeInfo.addChild(mPlayer, id);
        }
    }
}
