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

package androidx.core.view.accessibility;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Functional interface used to create a custom accessibility action.
 */
public interface AccessibilityViewCommand {
    /**
     * Performs the action.
     *
     * @return {@code true} if the action was handled, {@code false} otherwise
     *
     * @param view The view to act on
     * @param arguments Optional action arguments
     */
    boolean perform(@NonNull View view,
            @Nullable AccessibilityViewCommand.CommandArguments arguments);

    /**
     * Object containing arguments passed into an {@link AccessibilityViewCommand}
     */
    abstract class CommandArguments {
        Bundle mBundle;

        private static final Bundle sEmptyBundle = new Bundle();

        /**
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public void setBundle(Bundle bundle) {
            mBundle = bundle;
        }
    }

    /**
     * Arguments for {@link AccessibilityNodeInfoCompat.AccessibilityActionCompat
     * #ACTION_NEXT_AT_MOVEMENT_GRANULARITY}
     * and {@link AccessibilityNodeInfoCompat.AccessibilityActionCompat
     * #ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY}
     */
    final class MoveAtGranularityArguments extends CommandArguments {

        /**
         * @see AccessibilityNodeInfoCompat#MOVEMENT_GRANULARITY_CHARACTER
         *  AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER
         * @see AccessibilityNodeInfoCompat#MOVEMENT_GRANULARITY_WORD
         *  AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_WORD
         * @see AccessibilityNodeInfoCompat#MOVEMENT_GRANULARITY_LINE
         *  AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_LINE
         * @see AccessibilityNodeInfoCompat#MOVEMENT_GRANULARITY_PARAGRAPH
         *  AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PARAGRAPH
         * @see AccessibilityNodeInfoCompat#MOVEMENT_GRANULARITY_PAGE
         *  AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PAGE
         * @return Movement granularity to be used when traversing the node text.
         */
        public int getGranularity() {
            return mBundle.getInt(
                    AccessibilityNodeInfoCompat.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT);
        }

        /**
         * @return Whether when moving at granularity to extend the selection or to move it.
         */
        public boolean getExtendSelection() {
            return mBundle.getBoolean(
                    AccessibilityNodeInfoCompat.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN);
        }
    }

    /**
     * Arguments for {@link AccessibilityNodeInfoCompat#ACTION_NEXT_HTML_ELEMENT}
     * and {@link AccessibilityNodeInfoCompat.AccessibilityActionCompat
     * #ACTION_PREVIOUS_HTML_ELEMENT}
     */
    final class MoveHtmlArguments extends CommandArguments {

        /**
         * @return HTML element type, for example BUTTON, INPUT, TABLE, etc.
         */
        public String getHTMLElement() {
            return mBundle.getString(
                    AccessibilityNodeInfoCompat.ACTION_ARGUMENT_HTML_ELEMENT_STRING);
        }
    }

    /**
     * Arguments for {@link AccessibilityNodeInfoCompat.AccessibilityActionCompat
     * #ACTION_SET_SELECTION}
     */
    final class SetSelectionArguments extends CommandArguments {

        /**
         * @return Selection start.
         */
        public int getStart() {
            return mBundle.getInt(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_START_INT);
        }

        /**
         * @return Selection End.
         */
        public int getEnd() {
            return mBundle.getInt(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_END_INT);
        }
    }

    /**
     * Arguments for {@link AccessibilityNodeInfoCompat.AccessibilityActionCompat#ACTION_SET_TEXT}
     */
    final class SetTextArguments extends CommandArguments {

        /**
         * @return The text content to set.
         */
        public CharSequence getText() {
            return mBundle.getCharSequence(
                    AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE);
        }
    }

    /**
     * Arguments for {@link AccessibilityNodeInfoCompat.AccessibilityActionCompat
     * #ACTION_SCROLL_TO_POSITION}
     */
    final class ScrollToPositionArguments extends CommandArguments {

        /**
         * @return The collection row to make visible on screen.
         */
        public int getRow() {
            return mBundle.getInt(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_ROW_INT);
        }

        /**
         * @return The collection column to make visible on screen.
         */
        public int getColumn() {
            return mBundle.getInt(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_COLUMN_INT);
        }
    }

    /**
     * Arguments for {@link AccessibilityNodeInfoCompat.AccessibilityActionCompat
     * #ACTION_SET_PROGRESS}
     */
    final class SetProgressArguments extends CommandArguments {

        /**
         * @return The progress value to set.
         */
        public float getProgress() {
            return mBundle.getFloat(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_PROGRESS_VALUE);
        }
    }

    /**
     * Arguments for {@link AccessibilityNodeInfoCompat.AccessibilityActionCompat
     * #ACTION_MOVE_WINDOW}
     */
    final class MoveWindowArguments extends CommandArguments {

        /**
         * @return The x coordinate to which to move a window.
         */
        public int getX() {
            return mBundle.getInt(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_MOVE_WINDOW_X);
        }

        /**
         * @return The y coordinate to which to move a window.
         */
        public int getY() {
            return mBundle.getInt(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_MOVE_WINDOW_Y);
        }
    }
}
