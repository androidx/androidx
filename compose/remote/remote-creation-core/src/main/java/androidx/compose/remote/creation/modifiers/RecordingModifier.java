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
package androidx.compose.remote.creation.modifiers;

import androidx.compose.remote.core.RemoteComposeBuffer;
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.actions.Action;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Represent a list of modifiers */
public class RecordingModifier {

    @NonNull List<Element> mList = new ArrayList<>();

    int mId = -1;

    float mSpacedBy = 0f;

    /**
     * Add a wrap content size modifier
     *
     * @return
     */
    public @NonNull RecordingModifier wrapContentSize() {
        setWidthModifier(DimensionModifierOperation.Type.WRAP, 0);
        setHeightModifier(DimensionModifierOperation.Type.WRAP, 0);
        return this;
    }

    /**
     * Write the modifier to the buffer
     *
     * @param buffer
     */
    public void write(@NonNull RemoteComposeBuffer buffer) {
        // nothing
    }

    /**
     * Set the component Id
     *
     * @param id
     * @return
     */
    public @NonNull RecordingModifier componentId(int id) {
        mId = id;
        return this;
    }

    /**
     * Add a visibility modifier
     *
     * @param id the id of the RemoteInt representing the runtime visibility
     * @return
     */
    public @NonNull RecordingModifier visibility(int id) {
        mList.add(new VisibilityModifier(id));
        return this;
    }

    public int getComponentId() {
        return mId;
    }

    /**
     * Add a spacedBy value
     *
     * @param value
     * @return
     */
    public @NonNull RecordingModifier spacedBy(float value) {
        mSpacedBy = value;
        return this;
    }

    private @NonNull WidthModifier findWidthModifier() {
        for (int i = 0; i < mList.size(); i++) {
            RecordingModifier.Element m = mList.get(i);
            if (m instanceof WidthModifier) {
                return (WidthModifier) m;
            }
        }
        return null;
    }

    private void setWidthModifier(DimensionModifierOperation.@NonNull Type type, float value) {
        WidthModifier wm = findWidthModifier();
        if (wm == null) {
            mList.add(new WidthModifier(type, value));
        } else {
            wm.update(type, value);
        }
    }

    private @NonNull HeightModifier findHeightModifier() {
        for (int i = 0; i < mList.size(); i++) {
            RecordingModifier.Element m = mList.get(i);
            if (m instanceof HeightModifier) {
                return (HeightModifier) m;
            }
        }
        return null;
    }

    private void setHeightModifier(DimensionModifierOperation.@NonNull Type type, float value) {
        HeightModifier hm = findHeightModifier();
        if (hm == null) {
            mList.add(new HeightModifier(type, value));
        } else {
            hm.update(type, value);
        }
    }

    /**
     * Add a horizontal weight modifier
     *
     * @param value
     * @return
     */
    public @NonNull RecordingModifier horizontalWeight(float value) {
        setWidthModifier(DimensionModifierOperation.Type.WEIGHT, value);
        return this;
    }

    /**
     * Add a vertical weight modifier
     *
     * @param value
     * @return
     */
    public @NonNull RecordingModifier verticalWeight(float value) {
        setHeightModifier(DimensionModifierOperation.Type.WEIGHT, value);
        return this;
    }

    /**
     * Add min/max constraints on the horizontal dimension
     *
     * @param min minimum dimension, of -1f if not applied
     * @param max maximum dimension, of -1f if not applied
     * @return
     */
    public @NonNull RecordingModifier widthIn(float min, float max) {
        then(new WidthInModifier(min, max));
        return this;
    }

    /**
     * Add min/max constraints on the vertical dimension
     *
     * @param min minimum dimension, of -1f if not applied
     * @param max maximum dimension, of -1f if not applied
     * @return
     */
    public @NonNull RecordingModifier heightIn(float min, float max) {
        then(new HeightInModifier(min, max));
        return this;
    }

    /**
     * Add a background modifier (flat color background)
     *
     * @param color color of the background
     * @return
     */
    public @NonNull RecordingModifier background(int color) {
        mList.add(new SolidBackgroundModifier(color));
        return this;
    }

    /**
     * Add a collapsible priority. Only valid within a Collapsible layout.
     *
     * @param orientation HORIZONTAL or VERTICAL
     * @param priority a float representing a priority (lower priority get collapsed first)
     * @return
     */
    public @NonNull RecordingModifier collapsiblePriority(int orientation, float priority) {
        mList.add(new CollapsiblePriorityModifier(orientation, priority));
        return this;
    }

    /**
     * Add a padding modifier
     *
     * @param padding
     * @return
     */
    public @NonNull RecordingModifier padding(int padding) {
        mList.add(new PaddingModifier(padding, padding, padding, padding));
        return this;
    }

    /**
     * Add a padding modifier
     *
     * @param start
     * @param top
     * @param end
     * @param bottom
     * @return
     */
    public @NonNull RecordingModifier padding(int start, int top, int end, int bottom) {
        mList.add(new PaddingModifier(start, top, end, bottom));
        return this;
    }

    /**
     * Add a fixed width modifier
     *
     * @param value
     * @return
     */
    public @NonNull RecordingModifier width(int value) {
        setWidthModifier(DimensionModifierOperation.Type.EXACT, value);
        return this;
    }

    /**
     * Add a fixed width modifier
     *
     * @param value
     * @return
     */
    public @NonNull RecordingModifier width(float value) {
        setWidthModifier(DimensionModifierOperation.Type.EXACT, value);
        return this;
    }

    /**
     * Add a fixed height modifier
     *
     * @param value
     * @return
     */
    public @NonNull RecordingModifier height(int value) {
        setHeightModifier(DimensionModifierOperation.Type.EXACT, value);
        return this;
    }

    /**
     * Add a fixed height modifier
     *
     * @param value
     * @return
     */
    public @NonNull RecordingModifier height(float value) {
        setHeightModifier(DimensionModifierOperation.Type.EXACT, value);
        return this;
    }

    /**
     * Add a fixed size modifier
     *
     * @param value
     * @return
     */
    public @NonNull RecordingModifier size(int value) {
        return width(value).height(value);
    }

    /**
     * Add a fixed size modifier
     *
     * @param width
     * @param height
     * @return
     */
    public @NonNull RecordingModifier size(int width, int height) {
        return width(width).height(height);
    }

    /**
     * Add a width modifier to fill the parent width
     *
     * @return
     */
    public @NonNull RecordingModifier fillMaxWidth() {
        setWidthModifier(DimensionModifierOperation.Type.FILL, Float.NaN);
        return this;
    }

    /**
     * Add a height modifier to fill the parent height
     *
     * @return
     */
    public @NonNull RecordingModifier fillMaxHeight() {
        setHeightModifier(DimensionModifierOperation.Type.FILL, Float.NaN);
        return this;
    }

    /**
     * Add a size modifier to fill the parent
     *
     * @return
     */
    public @NonNull RecordingModifier fillMaxSize() {
        return fillMaxWidth().fillMaxHeight();
    }

    /**
     * Return the spacedBy value if set, NaN otherwise
     *
     * @return
     */
    public float getSpacedBy() {
        return mSpacedBy;
    }

    /**
     * return the horizontal weight if set, NaN otherwise
     *
     * @return
     */
    public float getHorizontalWeight() {
        WidthModifier wm = findWidthModifier();
        if (wm != null && wm.getType() == DimensionModifierOperation.Type.WEIGHT) {
            return wm.getValue();
        }
        return Float.NaN;
    }

    /**
     * return the vertical weight if set, NaN otherwise
     *
     * @return
     */
    public float getVerticalWeight() {
        HeightModifier hm = findHeightModifier();
        if (hm != null && hm.getType() == DimensionModifierOperation.Type.WEIGHT) {
            return hm.getValue();
        }
        return Float.NaN;
    }

    /**
     * return true if there is a fillMaxWidth modifier
     *
     * @return
     */
    public boolean getFillMaxWidth() {
        WidthModifier wm = findWidthModifier();
        if (wm != null) {
            return wm.getType() == DimensionModifierOperation.Type.FILL;
        }
        return false;
    }

    /**
     * return true if there is a fillMaxHeight modifier
     *
     * @return
     */
    public boolean getFillMaxHeight() {
        HeightModifier hm = findHeightModifier();
        if (hm != null) {
            return hm.getType() == DimensionModifierOperation.Type.FILL;
        }
        return false;
    }

    public @NonNull List<RecordingModifier.Element> getList() {
        return mList;
    }

    /**
     * Add a clip modifier
     *
     * @param shape
     * @return
     */
    public @NonNull RecordingModifier clip(@NonNull Shape shape) {
        mList.add(new ClipModifier(shape));
        return this;
    }

    /**
     * Add a border modifier
     *
     * @param width
     * @param roundedCorner
     * @param color
     * @param shape
     * @return
     */
    public @NonNull RecordingModifier border(
            float width, float roundedCorner, int color, int shape) {
        mList.add(new BorderModifier(width, roundedCorner, color, shape));
        return this;
    }

    /**
     * Adds a click modifier
     *
     * @param actions list of actions to execute on click
     * @return
     */
    public @NonNull RecordingModifier onClick(Action @NonNull ... actions) {
        mList.add(new ClickActionModifier(Arrays.asList(actions)));
        return this;
    }

    /**
     * Adds a touchDownmodifier
     *
     * @param actions list of actions to execute on touch down
     * @return
     */
    public @NonNull RecordingModifier onTouchDown(Action @NonNull ... actions) {
        mList.add(new TouchActionModifier(TouchActionModifier.DOWN, Arrays.asList(actions)));
        return this;
    }

    /**
     * Adds a touchUp modifier
     *
     * @param actions list of actions to execute on touch up
     * @return
     */
    public @NonNull RecordingModifier onTouchUp(Action @NonNull ... actions) {
        mList.add(new TouchActionModifier(TouchActionModifier.UP, Arrays.asList(actions)));
        return this;
    }

    /**
     * Adds a touchCancel modifier
     *
     * @param actions list of actions to execute on cancel
     * @return
     */
    public @NonNull RecordingModifier onTouchCancel(Action @NonNull ... actions) {
        mList.add(new TouchActionModifier(TouchActionModifier.CANCEL, Arrays.asList(actions)));
        return this;
    }

    /**
     * Adds an element
     *
     * @param existing
     * @return
     */
    public @NonNull RecordingModifier then(@NonNull RecordingModifier existing) {
        mList.addAll(existing.mList);
        return this;
    }

    /**
     * Adds an element
     *
     * @param existing
     * @return
     */
    public @NonNull RecordingModifier then(RecordingModifier.@NonNull Element existing) {
        mList.add(existing);
        return this;
    }

    @Override
    public @NonNull String toString() {
        return "RecordingModifier{mList=" + mList + '}';
    }

    /**
     * Finds the element of type T
     *
     * @param type
     * @return
     * @param <T>
     */
    public @Nullable <T extends RecordingModifier.Element> T find(@NonNull Class<T> type) {
        for (@NonNull Element element : mList) {
            if (type.isInstance(element)) {
                return type.cast(element);
            }
        }

        return null;
    }

    public interface Element {
        /**
         * Write the element to the buffer
         *
         * @param writer
         */
        void write(@NonNull RemoteComposeWriter writer);
    }
}
