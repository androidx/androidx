/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.os.Build;
import android.os.Parcelable;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityRecord;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Helper for accessing {@link AccessibilityRecord}.
 */
public class AccessibilityRecordCompat {
    private final AccessibilityRecord mRecord;

    /**
     * @deprecated This is not type safe. If you want to modify an
     * {@link AccessibilityEvent}'s properties defined in
     * {@link android.view.accessibility.AccessibilityRecord} use
     * {@link AccessibilityEventCompat#asRecord(AccessibilityEvent)}. This method will be removed
     * in a subsequent release of the support library.
     */
    @Deprecated
    public AccessibilityRecordCompat(Object record) {
        mRecord = (AccessibilityRecord) record;
    }

    /**
     * @return The wrapped implementation.
     *
     * @deprecated This method will be removed in a subsequent release of
     * the support library.
     */
    @Deprecated
    public Object getImpl() {
        return mRecord;
    }

    /**
     * Returns a cached instance if such is available or a new one is
     * instantiated. The instance is initialized with data from the
     * given record.
     *
     * @return An instance.
     *
     * @deprecated Use {@link AccessibilityRecord#obtain(AccessibilityRecord)} directly.
     */
    @Deprecated
    public static AccessibilityRecordCompat obtain(AccessibilityRecordCompat record) {
        return new AccessibilityRecordCompat(AccessibilityRecord.obtain(record.mRecord));
    }

    /**
     * Returns a cached instance if such is available or a new one is
     * instantiated.
     *
     * @return An instance.
     *
     * @deprecated Use {@link AccessibilityRecord#obtain()} directly.
     */
    @Deprecated
    public static AccessibilityRecordCompat obtain() {
        return new AccessibilityRecordCompat(AccessibilityRecord.obtain());
    }

    /**
     * Sets the event source.
     *
     * @param source The source.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     *
     * @deprecated Use {@link AccessibilityRecord#setSource(View)} directly.
     */
    @Deprecated
    public void setSource(View source) {
        mRecord.setSource(source);
    }

    /**
     * Sets the source to be a virtual descendant of the given <code>root</code>.
     * If <code>virtualDescendantId</code> equals to {@link View#NO_ID} the root
     * is set as the source.
     * <p>
     * A virtual descendant is an imaginary View that is reported as a part of the view
     * hierarchy for accessibility purposes. This enables custom views that draw complex
     * content to report them selves as a tree of virtual views, thus conveying their
     * logical structure.
     * </p>
     *
     * @param root The root of the virtual subtree.
     * @param virtualDescendantId The id of the virtual descendant.
     *
     * @deprecated Use {@link #setSource(AccessibilityRecord, View, int)} instead.
     */
    @Deprecated
    public void setSource(View root, int virtualDescendantId) {
        AccessibilityRecordCompat.setSource(mRecord, root, virtualDescendantId);
    }

    /**
     * Sets the source to be a virtual descendant of the given <code>root</code>.
     * If <code>virtualDescendantId</code> equals to {@link View#NO_ID} the root
     * is set as the source.
     * <p>
     * A virtual descendant is an imaginary View that is reported as a part of the view
     * hierarchy for accessibility purposes. This enables custom views that draw complex
     * content to report them selves as a tree of virtual views, thus conveying their
     * logical structure.
     * </p>
     *
     * @param record The {@link AccessibilityRecord} instance to use.
     * @param root The root of the virtual subtree.
     * @param virtualDescendantId The id of the virtual descendant.
     */
    public static void setSource(@NonNull AccessibilityRecord record, View root,
            int virtualDescendantId) {
        if (Build.VERSION.SDK_INT >= 16) {
            record.setSource(root, virtualDescendantId);
        }
    }

    /**
     * Gets the {@link android.view.accessibility.AccessibilityNodeInfo} of
     * the event source.
     * <p>
     * <strong>Note:</strong> It is a client responsibility to recycle the
     * received info by calling
     * {@link android.view.accessibility.AccessibilityNodeInfo#recycle()
     * AccessibilityNodeInfo#recycle()} to avoid creating of multiple instances.
     *</p>
     *
     * @return The info of the source.
     *
     * @deprecated Use {@link AccessibilityRecord#getSource()} directly.
     */
    @Deprecated
    public AccessibilityNodeInfoCompat getSource() {
        return AccessibilityNodeInfoCompat.wrapNonNullInstance(mRecord.getSource());
    }

    /**
     * Gets the id of the window from which the event comes from.
     *
     * @return The window id.
     *
     * @deprecated Use {@link AccessibilityRecord#getWindowId()} directly.
     */
    @Deprecated
    public int getWindowId() {
        return mRecord.getWindowId();
    }

    /**
     * Gets if the source is checked.
     *
     * @return True if the view is checked, false otherwise.
     *
     * @deprecated Use {@link AccessibilityRecord#isChecked()} directly.
     */
    @Deprecated
    public boolean isChecked() {
        return mRecord.isChecked();
    }

    /**
     * Sets if the source is checked.
     *
     * @param isChecked True if the view is checked, false otherwise.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     *
     * @deprecated Use {@link AccessibilityRecord#setChecked(boolean)} directly.
     */
    @Deprecated
    public void setChecked(boolean isChecked) {
        mRecord.setChecked(isChecked);
    }

    /**
     * Gets if the source is enabled.
     *
     * @return True if the view is enabled, false otherwise.
     *
     * @deprecated Use {@link AccessibilityRecord#isEnabled()} directly.
     */
    @Deprecated
    public boolean isEnabled() {
        return mRecord.isEnabled();
    }

    /**
     * Sets if the source is enabled.
     *
     * @param isEnabled True if the view is enabled, false otherwise.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     *
     * @deprecated Use {@link AccessibilityRecord#isEnabled()} directly.
     */
    @Deprecated
    public void setEnabled(boolean isEnabled) {
        mRecord.setEnabled(isEnabled);
    }

    /**
     * Gets if the source is a password field.
     *
     * @return True if the view is a password field, false otherwise.
     *
     * @deprecated Use {@link AccessibilityRecord#isPassword()} directly.
     */
    @Deprecated
    public boolean isPassword() {
        return mRecord.isPassword();
    }

    /**
     * Sets if the source is a password field.
     *
     * @param isPassword True if the view is a password field, false otherwise.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     *
     * @deprecated Use {@link AccessibilityRecord#setPassword(boolean)} directly.
     */
    @Deprecated
    public void setPassword(boolean isPassword) {
        mRecord.setPassword(isPassword);
    }

    /**
     * Gets if the source is taking the entire screen.
     *
     * @return True if the source is full screen, false otherwise.
     *
     * @deprecated Use {@link AccessibilityRecord#isFullScreen()} directly.
     */
    @Deprecated
    public boolean isFullScreen() {
        return mRecord.isFullScreen();
    }

    /**
     * Sets if the source is taking the entire screen.
     *
     * @param isFullScreen True if the source is full screen, false otherwise.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     *
     * @deprecated Use {@link AccessibilityRecord#setFullScreen(boolean)} directly.
     */
    @Deprecated
    public void setFullScreen(boolean isFullScreen) {
        mRecord.setFullScreen(isFullScreen);
    }

    /**
     * Gets if the source is scrollable.
     *
     * @return True if the source is scrollable, false otherwise.
     *
     * @deprecated Use {@link AccessibilityRecord#isScrollable()} directly.
     */
    @Deprecated
    public boolean isScrollable() {
        return mRecord.isScrollable();
    }

    /**
     * Sets if the source is scrollable.
     *
     * @param scrollable True if the source is scrollable, false otherwise.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     *
     * @deprecated Use {@link AccessibilityRecord#setScrollable(boolean)} directly.
     */
    @Deprecated
    public void setScrollable(boolean scrollable) {
        mRecord.setScrollable(scrollable);
    }

    /**
     * Gets the number of items that can be visited.
     *
     * @return The number of items.
     *
     * @deprecated Use {@link AccessibilityRecord#getItemCount()} directly.
     */
    @Deprecated
    public int getItemCount() {
        return mRecord.getItemCount();
    }

    /**
     * Sets the number of items that can be visited.
     *
     * @param itemCount The number of items.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     *
     * @deprecated Use {@link AccessibilityRecord#setItemCount(int)} directly.
     */
    @Deprecated
    public void setItemCount(int itemCount) {
        mRecord.setItemCount(itemCount);
    }

    /**
     * Gets the index of the source in the list of items the can be visited.
     *
     * @return The current item index.
     *
     * @deprecated Use {@link AccessibilityRecord#getCurrentItemIndex()} directly.
     */
    @Deprecated
    public int getCurrentItemIndex() {
        return mRecord.getCurrentItemIndex();
    }

    /**
     * Sets the index of the source in the list of items that can be visited.
     *
     * @param currentItemIndex The current item index.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     *
     * @deprecated Use {@link AccessibilityRecord#setCurrentItemIndex(int)} directly.
     */
    @Deprecated
    public void setCurrentItemIndex(int currentItemIndex) {
        mRecord.setCurrentItemIndex(currentItemIndex);
    }

    /**
     * Gets the index of the first character of the changed sequence,
     * or the beginning of a text selection or the index of the first
     * visible item when scrolling.
     *
     * @return The index of the first character or selection
     *        start or the first visible item.
     *
     * @deprecated Use {@link AccessibilityRecord#getFromIndex()} directly.
     */
    @Deprecated
    public int getFromIndex() {
        return mRecord.getFromIndex();
    }

    /**
     * Sets the index of the first character of the changed sequence
     * or the beginning of a text selection or the index of the first
     * visible item when scrolling.
     *
     * @param fromIndex The index of the first character or selection
     *        start or the first visible item.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     *
     * @deprecated Use {@link AccessibilityRecord#setFromIndex(int)} directly.
     */
    @Deprecated
    public void setFromIndex(int fromIndex) {
        mRecord.setFromIndex(fromIndex);
    }

    /**
     * Gets the index of text selection end or the index of the last
     * visible item when scrolling.
     *
     * @return The index of selection end or last item index.
     *
     * @deprecated Use {@link AccessibilityRecord#getToIndex()} directly.
     */
    @Deprecated
    public int getToIndex() {
        return mRecord.getToIndex();
    }

    /**
     * Sets the index of text selection end or the index of the last
     * visible item when scrolling.
     *
     * @param toIndex The index of selection end or last item index.
     *
     * @deprecated Use {@link AccessibilityRecord#setToIndex(int)} directly.
     */
    @Deprecated
    public void setToIndex(int toIndex) {
        mRecord.setToIndex(toIndex);
    }

    /**
     * Gets the scroll offset of the source left edge in pixels.
     *
     * @return The scroll.
     *
     * @deprecated Use {@link AccessibilityRecord#getScrollX()} directly.
     */
    @Deprecated
    public int getScrollX() {
        return mRecord.getScrollX();
    }

    /**
     * Sets the scroll offset of the source left edge in pixels.
     *
     * @param scrollX The scroll.
     *
     * @deprecated Use {@link AccessibilityRecord#setScrollX(int)} directly.
     */
    @Deprecated
    public void setScrollX(int scrollX) {
        mRecord.setScrollX(scrollX);
    }

    /**
     * Gets the scroll offset of the source top edge in pixels.
     *
     * @return The scroll.
     *
     * @deprecated Use {@link AccessibilityRecord#getScrollY()} directly.
     */
    @Deprecated
    public int getScrollY() {
        return mRecord.getScrollY();
    }

    /**
     * Sets the scroll offset of the source top edge in pixels.
     *
     * @param scrollY The scroll.
     *
     * @deprecated Use {@link AccessibilityRecord#setScrollY(int)} directly.
     */
    @Deprecated
    public void setScrollY(int scrollY) {
        mRecord.setScrollY(scrollY);
    }

    /**
     * Gets the max scroll offset of the source left edge in pixels.
     *
     * @return The max scroll.
     *
     * @deprecated Use {@link #getMaxScrollX(AccessibilityRecord)} instead.
     */
    @Deprecated
    public int getMaxScrollX() {
        return AccessibilityRecordCompat.getMaxScrollX(mRecord);
    }

    /**
     * Gets the max scroll offset of the source left edge in pixels.
     *
     * @param record The {@link AccessibilityRecord} instance to use.
     * @return The max scroll.
     */
    public static int getMaxScrollX(AccessibilityRecord record) {
        if (Build.VERSION.SDK_INT >= 15) {
            return record.getMaxScrollX();
        } else {
            return 0;
        }
    }

    /**
     * Sets the max scroll offset of the source left edge in pixels.
     *
     * @param maxScrollX The max scroll.
     *
     * @deprecated Use {@link #setMaxScrollX(AccessibilityRecord, int)} instead.
     */
    @Deprecated
    public void setMaxScrollX(int maxScrollX) {
        AccessibilityRecordCompat.setMaxScrollX(mRecord, maxScrollX);
    }

    /**
     * Sets the max scroll offset of the source left edge in pixels.
     *
     * @param record The {@link AccessibilityRecord} instance to use.
     * @param maxScrollX The max scroll.
     */
    public static void setMaxScrollX(AccessibilityRecord record, int maxScrollX) {
        if (Build.VERSION.SDK_INT >= 15) {
            record.setMaxScrollX(maxScrollX);
        }
    }

    /**
     * Gets the max scroll offset of the source top edge in pixels.
     *
     * @return The max scroll.
     *
     * @deprecated Use {@link #getMaxScrollY(AccessibilityRecord)} instead.
     */
    @Deprecated
    public int getMaxScrollY() {
        return AccessibilityRecordCompat.getMaxScrollY(mRecord);
    }

    /**
     * Gets the max scroll offset of the source top edge in pixels.
     *
     * @param record The {@link AccessibilityRecord} instance to use.
     * @return The max scroll.
     */
    public static int getMaxScrollY(AccessibilityRecord record) {
        if (Build.VERSION.SDK_INT >= 15) {
            return record.getMaxScrollY();
        } else {
            return 0;
        }
    }

    /**
     * Sets the max scroll offset of the source top edge in pixels.
     *
     * @param maxScrollY The max scroll.
     *
     * @deprecated Use {@link #setMaxScrollY(AccessibilityRecord, int)} instead.
     */
    @Deprecated
    public void setMaxScrollY(int maxScrollY) {
        AccessibilityRecordCompat.setMaxScrollY(mRecord, maxScrollY);
    }

    /**
     * Sets the max scroll offset of the source top edge in pixels.
     *
     * @param record The {@link AccessibilityRecord} instance to use.
     * @param maxScrollY The max scroll.
     */
    public static void setMaxScrollY(AccessibilityRecord record, int maxScrollY) {
        if (Build.VERSION.SDK_INT >= 15) {
            record.setMaxScrollY(maxScrollY);
        }
    }

    /**
     * Gets the number of added characters.
     *
     * @return The number of added characters.
     *
     * @deprecated Use {@link AccessibilityRecord#getAddedCount()} directly.
     */
    @Deprecated
    public int getAddedCount() {
        return mRecord.getAddedCount();
    }

    /**
     * Sets the number of added characters.
     *
     * @param addedCount The number of added characters.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     *
     * @deprecated Use {@link AccessibilityRecord#setAddedCount(int)} directly.
     */
    @Deprecated
    public void setAddedCount(int addedCount) {
        mRecord.setAddedCount(addedCount);
    }

    /**
     * Gets the number of removed characters.
     *
     * @return The number of removed characters.
     *
     * @deprecated Use {@link AccessibilityRecord#getRemovedCount()} directly.
     */
    @Deprecated
    public int getRemovedCount() {
        return mRecord.getRemovedCount();
    }

    /**
     * Sets the number of removed characters.
     *
     * @param removedCount The number of removed characters.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     *
     * @deprecated Use {@link AccessibilityRecord#setRemovedCount(int)} directly.
     */
    @Deprecated
    public void setRemovedCount(int removedCount) {
        mRecord.setRemovedCount(removedCount);
    }

    /**
     * Gets the class name of the source.
     *
     * @return The class name.
     *
     * @deprecated Use {@link AccessibilityRecord#getClassName()} directly.
     */
    @Deprecated
    public CharSequence getClassName() {
        return mRecord.getClassName();
    }

    /**
     * Sets the class name of the source.
     *
     * @param className The lass name.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     *
     * @deprecated Use {@link AccessibilityRecord#setClassName(CharSequence)} directly.
     */
    @Deprecated
    public void setClassName(CharSequence className) {
        mRecord.setClassName(className);
    }

    /**
     * Gets the text of the event. The index in the list represents the priority
     * of the text. Specifically, the lower the index the higher the priority.
     *
     * @return The text.
     *
     * @deprecated Use {@link AccessibilityRecord#getText()} directly.
     */
    @Deprecated
    public List<CharSequence> getText() {
        return mRecord.getText();
    }

    /**
     * Sets the text before a change.
     *
     * @return The text before the change.
     *
     * @deprecated Use {@link AccessibilityRecord#getBeforeText()} directly.
     */
    @Deprecated
    public CharSequence getBeforeText() {
        return mRecord.getBeforeText();
    }

    /**
     * Sets the text before a change.
     *
     * @param beforeText The text before the change.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     *
     * @deprecated Use {@link AccessibilityRecord#setBeforeText(CharSequence)} directly.
     */
    @Deprecated
    public void setBeforeText(CharSequence beforeText) {
        mRecord.setBeforeText(beforeText);
    }

    /**
     * Gets the description of the source.
     *
     * @return The description.
     *
     * @deprecated Use {@link AccessibilityRecord#getContentDescription()} directly.
     */
    @Deprecated
    public CharSequence getContentDescription() {
        return mRecord.getContentDescription();
    }

    /**
     * Sets the description of the source.
     *
     * @param contentDescription The description.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     *
     * @deprecated Use {@link AccessibilityRecord#setContentDescription(CharSequence)} directly.
     */
    @Deprecated
    public void setContentDescription(CharSequence contentDescription) {
        mRecord.setContentDescription(contentDescription);
    }

    /**
     * Gets the {@link Parcelable} data.
     *
     * @return The parcelable data.
     *
     * @deprecated Use {@link AccessibilityRecord#getParcelableData()} directly.
     */
    @Deprecated
    public Parcelable getParcelableData() {
        return mRecord.getParcelableData();
    }

    /**
     * Sets the {@link Parcelable} data of the event.
     *
     * @param parcelableData The parcelable data.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     *
     * @deprecated Use {@link AccessibilityRecord#setParcelableData(Parcelable)} directly.
     */
    @Deprecated
    public void setParcelableData(Parcelable parcelableData) {
        mRecord.setParcelableData(parcelableData);
    }

    /**
     * Return an instance back to be reused.
     * <p>
     * <strong>Note:</strong> You must not touch the object after calling this
     * function.
     * </p>
     *
     * @throws IllegalStateException If the record is already recycled.
     *
     * @deprecated Use {@link AccessibilityRecord#recycle()} directly.
     */
    @Deprecated
    public void recycle() {
        mRecord.recycle();
    }

    /**
     * @deprecated Use {@link AccessibilityRecord#hashCode()} directly.
     */
    @Deprecated
    @Override
    public int hashCode() {
        return (mRecord == null) ? 0 : mRecord.hashCode();
    }

    /**
     * @deprecated Use {@link AccessibilityRecord} directly.
     */
    @Deprecated
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AccessibilityRecordCompat other = (AccessibilityRecordCompat) obj;
        if (mRecord == null) {
            if (other.mRecord != null) {
                return false;
            }
        } else if (!mRecord.equals(other.mRecord)) {
            return false;
        }
        return true;
    }
}
