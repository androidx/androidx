/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.slice.widget;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents information associated with a logged event on {@link SliceView}.
 */
public class EventInfo {

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
            ROW_TYPE_SHORTCUT,
            ROW_TYPE_LIST,
            ROW_TYPE_GRID,
            ROW_TYPE_MESSAGING,
            ROW_TYPE_TOGGLE,
            ROW_TYPE_SLIDER,
            ROW_TYPE_PROGRESS,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SliceRowType {}

    /**
     * Indicates the slice is represented as a shortcut.
     */
    public static final int ROW_TYPE_SHORTCUT = -1;
    /**
     * Indicates the row is represented in a list template.
     */
    public static final int ROW_TYPE_LIST = 0;
    /**
     * Indicates the row is represented in a grid template.
     */
    public static final int ROW_TYPE_GRID = 1;
    /**
     * Indicates the row is represented as a messaging template.
     */
    public static final int ROW_TYPE_MESSAGING = 2;
    /**
     * Indicates the row represents a toggleable item.
     */
    public static final int ROW_TYPE_TOGGLE = 3;
    /**
     * Indicates the row represents an range input slider.
     */
    public static final int ROW_TYPE_SLIDER = 4;
    /**
     * Indicates the row represents a progress indicator.
     */
    public static final int ROW_TYPE_PROGRESS = 5;

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
            ACTION_TYPE_TOGGLE, ACTION_TYPE_BUTTON, ACTION_TYPE_SLIDER, ACTION_TYPE_CONTENT,
            ACTION_TYPE_SEE_MORE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SliceActionType{}

    /**
     * Indicates the event was an interaction with a toggle. Check {@link EventInfo#state} to
     * see the new state of the toggle.
     */
    public static final int ACTION_TYPE_TOGGLE = 0;
    /**
     * Indicates the event was an interaction with a button. Check {@link EventInfo#actionPosition}
     * to see where on the card the button is placed.
     */
    public static final int ACTION_TYPE_BUTTON = 1;
    /**
     * Indicates the event was an interaction with a slider. Check {@link EventInfo#state} to
     * see the new state of the slider.
     */
    public static final int ACTION_TYPE_SLIDER = 2;
    /**
     * Indicates the event was a tap on the entire row.
     */
    public static final int ACTION_TYPE_CONTENT = 3;
    /**
     * Indicates the event was a tap on a see more button.
     */
    public static final int ACTION_TYPE_SEE_MORE = 4;

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
            POSITION_START, POSITION_END, POSITION_CELL
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SliceButtonPosition{}

    /**
     * Indicates the event was an interaction with a button positioned at the start of the row.
     */
    public static final int POSITION_START = 0;
    /**
     * Indicates the event was an interaction with a button positioned at the end of the row,
     * potentially grouped with other buttons.
     */
    public static final int POSITION_END = 1;
    /**
     * Indicates the event was an interaction with a button positioned in a grid cell.
     */
    public static final int POSITION_CELL = 2;

    /**
     * Indicates the state of a toggle is off.
     */
    public static final int STATE_OFF = 0;
    /**
     * Indicates the state of a toggle is on.
     */
    public static final int STATE_ON = 1;

    /**
     * The display mode of the slice being interacted with.
     */
    public @SliceView.SliceMode int sliceMode;
    /**
     * The type of action that occurred.
     */
    public @SliceActionType int actionType;
    /**
     * The template type of the row that was interacted with in the slice.
     */
    public @SliceRowType int rowTemplateType;
    /**
     * Index of the row that was interacted with in the slice.
     */
    public int rowIndex;
    /**
     * If multiple buttons are presented in this {@link #actionPosition} on the row, then this is
     * the index of that button that was interacted with. For total number of actions
     * see {@link #actionCount}.
     *
     * <p>If the {@link #actionPosition} is {@link #POSITION_CELL} the button is a cell within
     * a grid, and this index would represent the cell position.</p>
     * <p>If the {@link #actionPosition} is {@link #POSITION_END} there might be other buttons
     * in the end position, and this index would represent the position.</p>
     */
    public int actionIndex;
    /**
     * Total number of actions available in this row of the slice.
     *
     * <p>If the {@link #actionPosition} is {@link #POSITION_CELL} the button is a cell within
     * a grid row, and this is the number of cells in the row.</p>
     * <p>If the {@link #actionPosition} is {@link #POSITION_END} this is the number of buttons
     * in the end position of this row.</p>
     */
    public int actionCount;
    /**
     * Position of the button on the template.
     *
     * {@link #POSITION_START}
     * {@link #POSITION_END}
     * {@link #POSITION_CELL}
     */
    public @SliceButtonPosition int actionPosition;
    /**
     * Represents the state after the event or -1 if not applicable for the event type.
     *
     * <p>For {@link #ACTION_TYPE_TOGGLE} events, the state will be either {@link #STATE_OFF}
     * or {@link #STATE_ON}.</p>
     * <p>For {@link #ACTION_TYPE_SLIDER} events, the state will be a number representing
     * the new position of the slider.</p>
     */
    public int state;

    /**
     * Constructs an event info object with the required information for an event.
     *
     * @param sliceMode The display mode of the slice interacted with.
     * @param actionType The type of action this event represents.
     * @param rowTemplateType The template type of the row interacted with.
     * @param rowIndex The index of the row that was interacted with in the slice.
     */
    public EventInfo(@SliceView.SliceMode int sliceMode, @SliceActionType int actionType,
            @SliceRowType int rowTemplateType, int rowIndex) {
        this.sliceMode = sliceMode;
        this.actionType = actionType;
        this.rowTemplateType = rowTemplateType;
        this.rowIndex = rowIndex;

        this.actionPosition = -1;
        this.actionIndex = -1;
        this.actionCount = -1;
        this.state = -1;
    }

    /**
     * Sets positional information for the event.
     *
     * @param actionPosition The position of the button on the template.
     * @param actionIndex The index of that button that was interacted with.
     * @param actionCount The number of actions available in this group of buttons on the slice.
     */
    public void setPosition(@SliceButtonPosition int actionPosition, int actionIndex,
            int actionCount) {
        this.actionPosition = actionPosition;
        this.actionIndex = actionIndex;
        this.actionCount = actionCount;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("mode=").append(SliceView.modeToString(sliceMode));
        sb.append(", actionType=").append(actionToString(actionType));
        sb.append(", rowTemplateType=").append(rowTypeToString(rowTemplateType));
        sb.append(", rowIndex=").append(rowIndex);
        sb.append(", actionPosition=").append(positionToString(actionPosition));
        sb.append(", actionIndex=").append(actionIndex);
        sb.append(", actionCount=").append(actionCount);
        sb.append(", state=").append(state);
        return sb.toString();
    }

    /**
     * @return String representation of the provided position.
     */
    private static String positionToString(@SliceButtonPosition int position) {
        switch (position) {
            case POSITION_START:
                return "START";
            case POSITION_END:
                return "END";
            case POSITION_CELL:
                return "CELL";
            default:
                return "unknown position: " + position;
        }
    }

    /**
     * @return String representation of the provided action.
     */
    private static String actionToString(@SliceActionType int action) {
        switch (action) {
            case ACTION_TYPE_TOGGLE:
                return "TOGGLE";
            case ACTION_TYPE_BUTTON:
                return "BUTTON";
            case ACTION_TYPE_SLIDER:
                return "SLIDER";
            case ACTION_TYPE_CONTENT:
                return "CONTENT";
            case ACTION_TYPE_SEE_MORE:
                return "SEE MORE";
            default:
                return "unknown action: " + action;
        }
    }

    /**
     * @return String representation of the provided row template type.
     */
    private static String rowTypeToString(@SliceRowType int type) {
        switch (type) {
            case ROW_TYPE_LIST:
                return "LIST";
            case ROW_TYPE_GRID:
                return "GRID";
            case ROW_TYPE_MESSAGING:
                return "MESSAGING";
            case ROW_TYPE_SHORTCUT:
                return "SHORTCUT";
            case ROW_TYPE_TOGGLE:
                return "TOGGLE";
            case ROW_TYPE_SLIDER:
                return "SLIDER";
            case ROW_TYPE_PROGRESS:
                return "PROGRESS";
            default:
                return "unknown row type: " + type;
        }
    }
}
