/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.regex.Pattern;

/**
 * The {@link Until} class provides factory methods for constructing common conditions.
 */
public class Until {

    private Until() {
    }

    // Search conditions

    /**
     * Returns a {@link SearchCondition} that is satisfied when no elements matching the selector
     * can be found.
     */
    @NonNull
    public static SearchCondition<Boolean> gone(@NonNull BySelector selector) {
        return new SearchCondition<Boolean>() {
            @Override
            public Boolean apply(Searchable container) {
                return !container.hasObject(selector);
            }

            @NonNull
            @Override
            public String toString() {
                return String.format("SearchCondition[gone=%s]", selector);
            }
        };
    }

    /**
     * Returns a {@link SearchCondition} that is satisfied when at least one element matching the
     * selector can be found.
     */
    @NonNull
    public static SearchCondition<Boolean> hasObject(@NonNull BySelector selector) {
        return new SearchCondition<Boolean>() {
            @Override
            public Boolean apply(Searchable container) {
                return container.hasObject(selector);
            }

            @NonNull
            @Override
            public String toString() {
                return String.format("SearchCondition[hasObject=%s]", selector);
            }
        };
    }

    /**
     * Returns a {@link SearchCondition} that is satisfied when at least one element matching the
     * selector can be found. The condition will return the first matching element.
     */
    @NonNull
    public static SearchCondition<UiObject2> findObject(@NonNull BySelector selector) {
        return new SearchCondition<UiObject2>() {
            @Override
            public UiObject2 apply(Searchable container) {
                return container.findObject(selector);
            }

            @NonNull
            @Override
            public String toString() {
                return String.format("SearchCondition[findObject=%s]", selector);
            }
        };
    }

    /**
     * Returns a {@link SearchCondition} that is satisfied when at least one element matching the
     * selector can be found. The condition will return all matching elements.
     */
    @NonNull
    public static SearchCondition<List<UiObject2>> findObjects(@NonNull BySelector selector) {
        return new SearchCondition<List<UiObject2>>() {
            @Override
            public List<UiObject2> apply(Searchable container) {
                List<UiObject2> ret = container.findObjects(selector);
                return ret.isEmpty() ? null : ret;
            }

            @NonNull
            @Override
            public String toString() {
                return String.format("SearchCondition[findObjects=%s]", selector);
            }
        };
    }


    // UiObject2 conditions

    /**
     * Returns a condition that depends on a {@link UiObject2}'s checkable state.
     *
     * @param isCheckable Whether the object should be checkable to satisfy this condition.
     */
    @NonNull
    public static UiObject2Condition<Boolean> checkable(final boolean isCheckable) {
        return new UiObject2Condition<Boolean>() {
            @Override
            public Boolean apply(UiObject2 object) {
                return object.isCheckable() == isCheckable;
            }

            @NonNull
            @Override
            public String toString() {
                return String.format("UiObject2Condition[checkable=%b]", isCheckable);
            }
        };
    }

    /**
     * Returns a condition that depends on a {@link UiObject2}'s checked state.
     *
     * @param isChecked Whether the object should be checked to satisfy this condition.
     */
    @NonNull
    public static UiObject2Condition<Boolean> checked(final boolean isChecked) {
        return new UiObject2Condition<Boolean>() {
            @Override
            public Boolean apply(UiObject2 object) {
                return object.isChecked() == isChecked;
            }

            @NonNull
            @Override
            public String toString() {
                return String.format("UiObject2Condition[checked=%b]", isChecked);
            }
        };
    }

    /**
     * Returns a condition that depends on a {@link UiObject2}'s clickable state.
     *
     * @param isClickable Whether the object should be clickable to satisfy this condition.
     */
    @NonNull
    public static UiObject2Condition<Boolean> clickable(final boolean isClickable) {
        return new UiObject2Condition<Boolean>() {
            @Override
            public Boolean apply(UiObject2 object) {
                return object.isClickable() == isClickable;
            }

            @NonNull
            @Override
            public String toString() {
                return String.format("UiObject2Condition[clickable=%b]", isClickable);
            }
        };
    }

    /**
     * Returns a condition that depends on a {@link UiObject2}'s enabled state.
     *
     * @param isEnabled Whether the object should be enabled to satisfy this condition.
     */
    @NonNull
    public static UiObject2Condition<Boolean> enabled(final boolean isEnabled) {
        return new UiObject2Condition<Boolean>() {
            @Override
            public Boolean apply(UiObject2 object) {
                return object.isEnabled() == isEnabled;
            }

            @NonNull
            @Override
            public String toString() {
                return String.format("UiObject2Condition[enabled=%b]", isEnabled);
            }
        };
    }

    /**
     * Returns a condition that depends on a {@link UiObject2}'s focusable state.
     *
     * @param isFocusable Whether the object should be focusable to satisfy this condition.
     */
    @NonNull
    public static UiObject2Condition<Boolean> focusable(final boolean isFocusable) {
        return new UiObject2Condition<Boolean>() {
            @Override
            public Boolean apply(UiObject2 object) {
                return object.isFocusable() == isFocusable;
            }

            @NonNull
            @Override
            public String toString() {
                return String.format("UiObject2Condition[focusable=%b]", isFocusable);
            }
        };
    }

    /**
     * Returns a condition that depends on a {@link UiObject2}'s focused state.
     *
     * @param isFocused Whether the object should be focused to satisfy this condition.
     */
    @NonNull
    public static UiObject2Condition<Boolean> focused(final boolean isFocused) {
        return new UiObject2Condition<Boolean>() {
            @Override
            public Boolean apply(UiObject2 object) {
                return object.isFocused() == isFocused;
            }

            @NonNull
            @Override
            public String toString() {
                return String.format("UiObject2Condition[focused=%b]", isFocused);
            }
        };
    }

    /**
     * Returns a condition that depends on a {@link UiObject2}'s long clickable state.
     *
     * @param isLongClickable Whether the object should be long clickable to satisfy this condition.
     */
    @NonNull
    public static UiObject2Condition<Boolean> longClickable(final boolean isLongClickable) {
        return new UiObject2Condition<Boolean>() {
            @Override
            public Boolean apply(UiObject2 object) {
                return object.isLongClickable() == isLongClickable;
            }

            @NonNull
            @Override
            public String toString() {
                return String.format("UiObject2Condition[longClickable=%b]", isLongClickable);
            }
        };
    }

    /**
     * Returns a condition that depends on a {@link UiObject2}'s scrollable state.
     *
     * @param isScrollable Whether the object should be scrollable to satisfy this condition.
     */
    @NonNull
    public static UiObject2Condition<Boolean> scrollable(final boolean isScrollable) {
        return new UiObject2Condition<Boolean>() {
            @Override
            public Boolean apply(UiObject2 object) {
                return object.isScrollable() == isScrollable;
            }

            @NonNull
            @Override
            public String toString() {
                return String.format("UiObject2Condition[scrollable=%b]", isScrollable);
            }
        };
    }

    /**
     * Returns a condition that depends on a {@link UiObject2}'s selected state.
     *
     * @param isSelected Whether the object should be selected to satisfy this condition.
     */
    @NonNull
    public static UiObject2Condition<Boolean> selected(final boolean isSelected) {
        return new UiObject2Condition<Boolean>() {
            @Override
            public Boolean apply(UiObject2 object) {
                return object.isSelected() == isSelected;
            }

            @NonNull
            @Override
            public String toString() {
                return String.format("UiObject2Condition[selected=%b]", isSelected);
            }
        };
    }

    /**
     * Returns a condition that is satisfied when the object's content description matches the given
     * regex.
     */
    @NonNull
    public static UiObject2Condition<Boolean> descMatches(@NonNull Pattern regex) {
        return new UiObject2Condition<Boolean>() {
            @Override
            public Boolean apply(UiObject2 object) {
                String desc = object.getContentDescription();
                return regex.matcher(desc != null ? desc : "").matches();
            }

            @NonNull
            @Override
            public String toString() {
                return String.format("UiObject2Condition[descMatches='%s']", regex);
            }
        };
    }

    /**
     * Returns a condition that is satisfied when the object's content description matches the given
     * regex.
     */
    @NonNull
    public static UiObject2Condition<Boolean> descMatches(@NonNull String regex) {
        return descMatches(Pattern.compile(regex, Pattern.DOTALL));
    }

    /**
     * Returns a condition that is satisfied when the object's content description exactly matches
     * the given string (case-sensitive).
     */
    @NonNull
    public static UiObject2Condition<Boolean> descEquals(@NonNull String contentDescription) {
        return descMatches(Pattern.quote(contentDescription));
    }

    /**
     * Returns a condition that is satisfied when the object's content description contains the
     * given string (case-sensitive).
     */
    @NonNull
    public static UiObject2Condition<Boolean> descContains(@NonNull String substring) {
        return descMatches(RegexHelper.getPatternContains(substring));
    }

    /**
     * Returns a condition that is satisfied when the object's content description starts with the
     * given string (case-sensitive).
     */
    @NonNull
    public static UiObject2Condition<Boolean> descStartsWith(@NonNull String substring) {
        return descMatches(RegexHelper.getPatternStartsWith(substring));
    }

    /**
     * Returns a condition that is satisfied when the object's content description ends with the
     * given string (case-sensitive).
     */
    @NonNull
    public static UiObject2Condition<Boolean> descEndsWith(@NonNull String substring) {
        return descMatches(RegexHelper.getPatternEndsWith(substring));
    }

    /**
     * Returns a condition that is satisfied when the object's text value matches the given regex.
     */
    @NonNull
    public static UiObject2Condition<Boolean> textMatches(@NonNull Pattern regex) {
        return new UiObject2Condition<Boolean>() {
            @Override
            public Boolean apply(UiObject2 object) {
                String text = object.getText();
                return regex.matcher(text != null ? text : "").matches();
            }

            @NonNull
            @Override
            public String toString() {
                return String.format("UiObject2Condition[textMatches='%s']", regex);
            }
        };
    }

    /**
     * Returns a condition that is satisfied when the object's text value matches the given regex.
     */
    @NonNull
    public static UiObject2Condition<Boolean> textMatches(@NonNull String regex) {
        return textMatches(Pattern.compile(regex, Pattern.DOTALL));
    }

    /**
     * Returns a condition that is satisfied when the object's text value does not match the
     * given string.
     */
    @NonNull
    public static UiObject2Condition<Boolean> textNotEquals(@NonNull String text) {
        return new UiObject2Condition<Boolean>() {
            @Override
            public Boolean apply(UiObject2 object) {
                return !text.equals(object.getText());
            }

            @NonNull
            @Override
            public String toString() {
                return String.format("UiObject2Condition[textNotEquals='%s']", text);
            }
        };
    }

    /**
     * Returns a condition that is satisfied when the object's text value exactly matches the given
     * string (case-sensitive).
     */
    @NonNull
    public static UiObject2Condition<Boolean> textEquals(@NonNull String text) {
        return textMatches(Pattern.quote(text));
    }

    /**
     * Returns a condition that is satisfied when the object's text value contains the given string
     * (case-sensitive).
     */
    @NonNull
    public static UiObject2Condition<Boolean> textContains(@NonNull String substring) {
        return textMatches(RegexHelper.getPatternContains(substring));
    }

    /**
     * Returns a condition that is satisfied when the object's text value starts with the given
     * string (case-sensitive).
     */
    @NonNull
    public static UiObject2Condition<Boolean> textStartsWith(@NonNull String substring) {
        return textMatches(RegexHelper.getPatternStartsWith(substring));
    }

    /**
     * Returns a condition that is satisfied when the object's text value ends with the given
     * string (case-sensitive).
     */
    @NonNull
    public static UiObject2Condition<Boolean> textEndsWith(@NonNull String substring) {
        return textMatches(RegexHelper.getPatternEndsWith(substring));
    }


    // Event conditions

    /** Returns a condition that depends on a new window having appeared. */
    @NonNull
    public static EventCondition<Boolean> newWindow() {
        return new EventCondition<Boolean>() {
            private int mMask = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;

            @Override
            public boolean accept(AccessibilityEvent event) {
                mMask &= ~event.getEventType();
                return mMask == 0;
            }

            @Override
            public Boolean getResult() {
                return mMask == 0;
            }

            @NonNull
            @Override
            public String toString() {
                return "EventCondition[newWindow]";
            }
        };
    }

    /**
     * Returns a condition that depends on a scroll having reached the end in the given
     * {@code direction}.
     *
     * @param direction The direction of the scroll.
     */
    @NonNull
    public static EventCondition<Boolean> scrollFinished(@NonNull Direction direction) {
        return new EventCondition<Boolean>() {
            private Boolean mResult = null;

            @Override
            public boolean accept(AccessibilityEvent event) {
                if (event.getEventType() != AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                    return false; // Ignore non-scrolling events.
                }

                if (event.getFromIndex() != -1 && event.getToIndex() != -1 &&
                        event.getItemCount() != -1) {
                    switch (direction) {
                        case UP:
                        case LEFT:
                            mResult = (event.getFromIndex() == 0);
                            break;
                        case DOWN:
                        case RIGHT:
                            mResult = (event.getToIndex() == event.getItemCount() - 1);
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid Direction");
                    }
                } else if (event.getScrollX() != -1 && event.getScrollY() != -1) {
                    switch (direction) {
                        case UP:
                            mResult = (event.getScrollY() == 0);
                            break;
                        case DOWN:
                            mResult = (event.getScrollY() == event.getMaxScrollY());
                            break;
                        case LEFT:
                            mResult = (event.getScrollX() == 0);
                            break;
                        case RIGHT:
                            mResult = (event.getScrollX() == event.getMaxScrollX());
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid Direction");
                    }
                }

                // Keep listening for events until the result is set to true (we reached the end)
                return Boolean.TRUE.equals(mResult);
            }

            @Nullable
            @Override
            public Boolean getResult() {
                return mResult;
            }

            @NonNull
            @Override
            public String toString() {
                return String.format("EventCondition[scrollFinished=%s]", direction.name());
            }
        };
    }
}
