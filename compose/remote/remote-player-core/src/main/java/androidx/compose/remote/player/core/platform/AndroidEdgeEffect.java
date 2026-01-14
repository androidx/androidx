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

package androidx.compose.remote.player.core.platform;

import android.graphics.Color;
import android.graphics.RecordingCanvas;
import android.graphics.RenderNode;
import android.os.Build;
import android.widget.EdgeEffect;

import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.ScrollingEdgeEffect;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.LayoutComponent;

import org.jspecify.annotations.NonNull;

/**
 * Implement a scrolling edge effect on Android
 */
class AndroidEdgeEffect implements ScrollingEdgeEffect {
    EdgeEffect mEffect;
    int mDirection;

    RenderNode mNode;
    RecordingCanvas mRecordingCanvas;

    AndroidEdgeEffect(EdgeEffect effect, int direction) {
        mEffect = effect;
        mDirection = direction;
    }

    @Override
    public void reset() {
        mEffect.onAbsorb(0);
    }

    @Override
    public void pull(float value, float distance) {
        float delta = value / distance; // normalize
        mEffect.onPull(Math.abs(delta), 0.5f);
    }

    @Override
    public void release() {
        mEffect.onRelease();
        mNode = null;
        mRecordingCanvas = null;
    }

    @Override
    public void setSize(float width, float height) {
        if (mDirection == LEFT || mDirection == RIGHT) {
            mEffect.setSize((int) height, (int) width);
        } else {
            mEffect.setSize((int) width, (int) height);
        }
    }

    @Override
    public void apply(@NonNull PaintContext context, @NonNull Component component,
            float contentDimension, int phase) {
        if (!mEffect.isFinished()) {
            AndroidPaintContext paintContext = (AndroidPaintContext) context;
            if (phase == PRE_DRAW) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // REMOVE IN PLATFORM
                    float distance = mEffect.getDistance() / 10f;
                    if (distance > 0) {
                        switch (mDirection) {
                            case TOP:
                                context.matrixScale(1f, 1f + distance,
                                        component.getWidth() / 2f, 0f);
                                break;
                            case BOTTOM:
                                context.matrixScale(1f, 1f + distance,
                                        component.getWidth() / 2f, contentDimension);
                                break;
                            case LEFT:
                                context.matrixScale(1f + distance, 1f,
                                        0f, component.getHeight() / 2f);
                                break;
                            case RIGHT:
                                context.matrixScale(1f + distance, 1f,
                                        contentDimension, component.getHeight() / 2f);
                                break;
                        }
                        // if we switch to render nodes for scroll areas
                        // we could pass the canvas in paintContext, as EdgeEffect can directly
                        // apply a stretch to the corresponding render node
                        if (mNode == null) {
                            mNode = new RenderNode("empty");
                        }
                        if (mRecordingCanvas == null) {
                            mRecordingCanvas = mNode.beginRecording();
                        }
                        mEffect.draw(mRecordingCanvas);
                        context.needsRepaint();
                    }
                } // REMOVE IN PLATFORM
            } else if (phase == POST_DRAW) { // REMOVE IN PLATFORM
                // BEGIN-REMOVE-IN-PLATFORM
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    mEffect.setColor(Color.BLACK);
                    LayoutComponent layoutComponent = (LayoutComponent) component;
                    float horizontalPadding = layoutComponent.getPaddingLeft()
                            + layoutComponent.getPaddingRight();
                    float verticalPadding = layoutComponent.getPaddingTop()
                            + layoutComponent.getPaddingBottom();
                    switch (mDirection) {
                        case TOP:
                            mEffect.draw(paintContext.mCanvas);
                            break;
                        case BOTTOM:
                            paintContext.save();
                            paintContext.translate(-horizontalPadding, contentDimension);
                            paintContext.matrixRotate(180f, component.getWidth() / 2f, 0f);
                            mEffect.draw(paintContext.mCanvas);
                            paintContext.restore();
                            break;
                        case LEFT:
                            paintContext.save();
                            paintContext.translate(0f, -verticalPadding);
                            paintContext.matrixRotate(270f, Float.NaN, Float.NaN);
                            paintContext.translate(-component.getHeight(), 0f);
                            mEffect.draw(paintContext.mCanvas);
                            paintContext.restore();
                            break;
                        case RIGHT:
                            paintContext.save();
                            paintContext.matrixRotate(90f, Float.NaN, Float.NaN);
                            paintContext.translate(0, -contentDimension);
                            mEffect.draw(paintContext.mCanvas);
                            paintContext.restore();
                            break;
                    }
                    context.needsRepaint();
                }
                // END-REMOVE-IN-PLATFORM
            }
        }
    }
}
