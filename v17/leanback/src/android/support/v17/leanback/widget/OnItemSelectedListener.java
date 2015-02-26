/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

/**
 * Interface for receiving notification when a row or item becomes selected.
 *
 * @deprecated Use {@link OnItemViewSelectedListener}
 */
public interface OnItemSelectedListener {
    /**
     * Called when the a row or a new item becomes selected.  The concept of current selection
     * is different than focus.  Row or item can be selected even they don't have focus.
     * Having the concept of selection will allow developer to switch background to selected
     * item or selected row when user selects rows outside row UI (e.g. headers left of
     * rows).
     * <p>
     * For a none {@link ListRow} case,  parameter item is always null.  Event is fired when
     * selection changes between rows, regardless if row view has focus or not.
     * <p>
     * For a {@link ListRow} case, parameter item can be null if the list row is empty.
     * </p>
     * <p>
     * In the case of a grid, the row parameter is always null.
     * </p>
     * <li>
     * Row has focus: event is fired when focus changes between child of the row.
     * </li>
     * <li>
     * None of the row has focus: the event is fired with the current selected row and last
     * focused item in the row.
     * </li>
     *
     * @param item The item that is currently selected.
     * @param row The row that is currently selected.
     */
    public void onItemSelected(Object item, Row row);
}
