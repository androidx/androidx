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

package android.support.v4.view.accessibility;

import android.os.Build;
import android.os.Parcelable;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import java.util.Collections;
import java.util.List;

/**
 * Helper for accessing {@link android.view.accessibility.AccessibilityRecord}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public class AccessibilityRecordCompat {

    static interface AccessibilityRecordImpl {
        public Object obtain();
        public Object obtain(Object record);
        public void setSource(Object record, View source);
        public void setSource(Object record, View root, int virtualDescendantId);
        public AccessibilityNodeInfoCompat getSource(Object record);
        public int getWindowId(Object record);
        public boolean isChecked(Object record);
        public void setChecked(Object record, boolean isChecked);
        public boolean isEnabled(Object record);
        public void setEnabled(Object record, boolean isEnabled);
        public boolean isPassword(Object record);
        public void setPassword(Object record, boolean isPassword);
        public boolean isFullScreen(Object record);
        public void setFullScreen(Object record, boolean isFullScreen);
        public boolean isScrollable(Object record);
        public void setScrollable(Object record, boolean scrollable);
        public int getItemCount(Object record);
        public void setItemCount(Object record, int itemCount);
        public int getCurrentItemIndex(Object record);
        public void setCurrentItemIndex(Object record, int currentItemIndex);
        public int getFromIndex(Object record);
        public void setFromIndex(Object record, int fromIndex);
        public int getToIndex(Object record);
        public void setToIndex(Object record, int toIndex);
        public int getScrollX(Object record);
        public void setScrollX(Object record, int scrollX);
        public int getScrollY(Object record);
        public void setScrollY(Object record, int scrollY);
        public int getMaxScrollX(Object record);
        public void setMaxScrollX(Object record, int maxScrollX);
        public int getMaxScrollY(Object record);
        public void setMaxScrollY(Object record, int maxScrollY);
        public int getAddedCount(Object record);
        public void setAddedCount(Object record, int addedCount);
        public int getRemovedCount(Object record);
        public void setRemovedCount(Object record, int removedCount);
        public CharSequence getClassName(Object record);
        public void setClassName(Object record, CharSequence className);
        public List<CharSequence> getText(Object record);
        public CharSequence getBeforeText(Object record);
        public void setBeforeText(Object record, CharSequence beforeText);
        public CharSequence getContentDescription(Object record);
        public void setContentDescription(Object record, CharSequence contentDescription);
        public Parcelable getParcelableData(Object record);
        public void setParcelableData(Object record, Parcelable parcelableData);
        public void recycle(Object record);
    }

    static class AccessibilityRecordStubImpl implements AccessibilityRecordImpl {
        public Object obtain() {
            return null;
        }

        public Object obtain(Object record) {
            return null;
        }

        public int getAddedCount(Object record) {
            return 0;
        }

        public CharSequence getBeforeText(Object record) {
            return null;
        }

        public CharSequence getClassName(Object record) {
            return null;
        }

        public CharSequence getContentDescription(Object record) {
            return null;
        }

        public int getCurrentItemIndex(Object record) {
            return 0;
        }

        public int getFromIndex(Object record) {
            return 0;
        }

        public int getItemCount(Object record) {
            return 0;
        }

        public int getMaxScrollX(Object record) {
            return 0;
        }

        public int getMaxScrollY(Object record) {
            return 0;
        }

        public Parcelable getParcelableData(Object record) {
            return null;
        }

        public int getRemovedCount(Object record) {
            return 0;
        }

        public int getScrollX(Object record) {
            return 0;
        }

        public int getScrollY(Object record) {
            return 0;
        }

        public AccessibilityNodeInfoCompat getSource(Object record) {
            return null;
        }

        public List<CharSequence> getText(Object record) {
            return Collections.emptyList();
        }

        public int getToIndex(Object record) {
            return 0;
        }

        public int getWindowId(Object record) {
            return 0;
        }

        public boolean isChecked(Object record) {
            return false;
        }

        public boolean isEnabled(Object record) {
            return false;
        }

        public boolean isFullScreen(Object record) {
            return false;
        }

        public boolean isPassword(Object record) {
            return false;
        }

        public boolean isScrollable(Object record) {
            return false;
        }

        public void recycle(Object record) {

        }

        public void setAddedCount(Object record, int addedCount) {

        }

        public void setBeforeText(Object record, CharSequence beforeText) {

        }

        public void setChecked(Object record, boolean isChecked) {

        }

        public void setClassName(Object record, CharSequence className) {

        }

        public void setContentDescription(Object record, CharSequence contentDescription) {

        }

        public void setCurrentItemIndex(Object record, int currentItemIndex) {

        }

        public void setEnabled(Object record, boolean isEnabled) {

        }

        public void setFromIndex(Object record, int fromIndex) {

        }

        public void setFullScreen(Object record, boolean isFullScreen) {

        }

        public void setItemCount(Object record, int itemCount) {

        }

        public void setMaxScrollX(Object record, int maxScrollX) {

        }

        public void setMaxScrollY(Object record, int maxScrollY) {

        }

        public void setParcelableData(Object record, Parcelable parcelableData) {

        }

        public void setPassword(Object record, boolean isPassword) {

        }

        public void setRemovedCount(Object record, int removedCount) {

        }

        public void setScrollX(Object record, int scrollX) {

        }

        public void setScrollY(Object record, int scrollY) {

        }

        public void setScrollable(Object record, boolean scrollable) {

        }

        public void setSource(Object record, View source) {

        }

        public void setSource(Object record, View root, int virtualDescendantId) {

        }

        public void setToIndex(Object record, int toIndex) {

        }
    }

    static class AccessibilityRecordIcsImpl extends AccessibilityRecordStubImpl {
        @Override
        public Object obtain() {
            return AccessibilityRecordCompatIcs.obtain();
        }

        @Override
        public Object obtain(Object record) {
            return AccessibilityRecordCompatIcs.obtain(record);
        }

        @Override
        public int getAddedCount(Object record) {
            return AccessibilityRecordCompatIcs.getAddedCount(record);
        }

        @Override
        public CharSequence getBeforeText(Object record) {
            return AccessibilityRecordCompatIcs.getBeforeText(record);
        }

        @Override
        public CharSequence getClassName(Object record) {
            return AccessibilityRecordCompatIcs.getClassName(record);
        }

        @Override
        public CharSequence getContentDescription(Object record) {
            return AccessibilityRecordCompatIcs.getContentDescription(record);
        }

        @Override
        public int getCurrentItemIndex(Object record) {
            return AccessibilityRecordCompatIcs.getCurrentItemIndex(record);
        }

        @Override
        public int getFromIndex(Object record) {
            return AccessibilityRecordCompatIcs.getFromIndex(record);
        }

        @Override
        public int getItemCount(Object record) {
            return AccessibilityRecordCompatIcs.getItemCount(record);
        }

        @Override
        public Parcelable getParcelableData(Object record) {
            return AccessibilityRecordCompatIcs.getParcelableData(record);
        }

        @Override
        public int getRemovedCount(Object record) {
            return AccessibilityRecordCompatIcs.getRemovedCount(record);
        }

        @Override
        public int getScrollX(Object record) {
            return AccessibilityRecordCompatIcs.getScrollX(record);
        }

        @Override
        public int getScrollY(Object record) {
            return AccessibilityRecordCompatIcs.getScrollY(record);
        }

        @Override
        public AccessibilityNodeInfoCompat getSource(Object record) {
            return AccessibilityNodeInfoCompat.wrapNonNullInstance(
                    AccessibilityRecordCompatIcs.getSource(record));
        }

        @Override
        public List<CharSequence> getText(Object record) {
            return AccessibilityRecordCompatIcs.getText(record);
        }

        @Override
        public int getToIndex(Object record) {
            return AccessibilityRecordCompatIcs.getToIndex(record);
        }

        @Override
        public int getWindowId(Object record) {
            return AccessibilityRecordCompatIcs.getWindowId(record);
        }

        @Override
        public boolean isChecked(Object record) {
            return AccessibilityRecordCompatIcs.isChecked(record);
        }

        @Override
        public boolean isEnabled(Object record) {
            return AccessibilityRecordCompatIcs.isEnabled(record);
        }

        @Override
        public boolean isFullScreen(Object record) {
            return AccessibilityRecordCompatIcs.isFullScreen(record);
        }

        @Override
        public boolean isPassword(Object record) {
            return AccessibilityRecordCompatIcs.isPassword(record);
        }

        @Override
        public boolean isScrollable(Object record) {
            return AccessibilityRecordCompatIcs.isScrollable(record);
        }

        @Override
        public void recycle(Object record) {
            AccessibilityRecordCompatIcs.recycle(record);
        }

        @Override
        public void setAddedCount(Object record, int addedCount) {
            AccessibilityRecordCompatIcs.setAddedCount(record, addedCount);
        }

        @Override
        public void setBeforeText(Object record, CharSequence beforeText) {
            AccessibilityRecordCompatIcs.setBeforeText(record, beforeText);
        }

        @Override
        public void setChecked(Object record, boolean isChecked) {
            AccessibilityRecordCompatIcs.setChecked(record, isChecked);
        }

        @Override
        public void setClassName(Object record, CharSequence className) {
            AccessibilityRecordCompatIcs.setClassName(record, className);
        }

        @Override
        public void setContentDescription(Object record, CharSequence contentDescription) {
            AccessibilityRecordCompatIcs.setContentDescription(record, contentDescription);
        }

        @Override
        public void setCurrentItemIndex(Object record, int currentItemIndex) {
            AccessibilityRecordCompatIcs.setCurrentItemIndex(record, currentItemIndex);
        }

        @Override
        public void setEnabled(Object record, boolean isEnabled) {
            AccessibilityRecordCompatIcs.setEnabled(record, isEnabled);
        }

        @Override
        public void setFromIndex(Object record, int fromIndex) {
            AccessibilityRecordCompatIcs.setFromIndex(record, fromIndex);
        }

        @Override
        public void setFullScreen(Object record, boolean isFullScreen) {
            AccessibilityRecordCompatIcs.setFullScreen(record, isFullScreen);
        }

        @Override
        public void setItemCount(Object record, int itemCount) {
            AccessibilityRecordCompatIcs.setItemCount(record, itemCount);
        }

        @Override
        public void setParcelableData(Object record, Parcelable parcelableData) {
            AccessibilityRecordCompatIcs.setParcelableData(record, parcelableData);
        }

        @Override
        public void setPassword(Object record, boolean isPassword) {
            AccessibilityRecordCompatIcs.setPassword(record, isPassword);
        }

        @Override
        public void setRemovedCount(Object record, int removedCount) {
            AccessibilityRecordCompatIcs.setRemovedCount(record, removedCount);
        }

        @Override
        public void setScrollX(Object record, int scrollX) {
            AccessibilityRecordCompatIcs.setScrollX(record, scrollX);
        }

        @Override
        public void setScrollY(Object record, int scrollY) {
            AccessibilityRecordCompatIcs.setScrollY(record, scrollY);
        }

        @Override
        public void setScrollable(Object record, boolean scrollable) {
            AccessibilityRecordCompatIcs.setScrollable(record, scrollable);
        }

        @Override
        public void setSource(Object record, View source) {
            AccessibilityRecordCompatIcs.setSource(record, source);
        }

        @Override
        public void setToIndex(Object record, int toIndex) {
            AccessibilityRecordCompatIcs.setToIndex(record, toIndex);
        }
    }

    static class AccessibilityRecordIcsMr1Impl extends AccessibilityRecordIcsImpl {
        @Override
        public int getMaxScrollX(Object record) {
            return AccessibilityRecordCompatIcsMr1.getMaxScrollX(record);
        }

        @Override
        public int getMaxScrollY(Object record) {
            return AccessibilityRecordCompatIcsMr1.getMaxScrollY(record);
        }

        @Override
        public void setMaxScrollX(Object record, int maxScrollX) {
            AccessibilityRecordCompatIcsMr1.setMaxScrollX(record, maxScrollX);
        }

        @Override
        public void setMaxScrollY(Object record, int maxScrollY) {
            AccessibilityRecordCompatIcsMr1.setMaxScrollY(record, maxScrollY);
        }
    }

    static class AccessibilityRecordJellyBeanImpl extends AccessibilityRecordIcsMr1Impl {
        @Override
        public void setSource(Object record, View root, int virtualDescendantId) {
            AccessibilityRecordCompatJellyBean.setSource(record, root, virtualDescendantId);
        }
    }

    static {
        if (Build.VERSION.SDK_INT >= 16) { // JellyBean
            IMPL = new AccessibilityRecordJellyBeanImpl();
        } else if (Build.VERSION.SDK_INT >= 15) {  // ICS MR1
            IMPL = new AccessibilityRecordIcsMr1Impl();
        } else if (Build.VERSION.SDK_INT >= 14) { // ICS
            IMPL = new AccessibilityRecordIcsImpl();
        } else {
            IMPL = new AccessibilityRecordStubImpl();
        }
    }

    private static final AccessibilityRecordImpl IMPL;

    private final Object mRecord;

    /**
     * @deprecated This is not type safe. If you want to modify an
     * {@link AccessibilityEvent}'s properties defined in
     * {@link android.view.accessibility.AccessibilityRecord} use
     * {@link AccessibilityEventCompat#asRecord(AccessibilityEvent)}. This method will be removed
     * in a subsequent release of the support library.
     */
    @Deprecated
    public AccessibilityRecordCompat(Object record) {
        mRecord = record;
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
     */
    public static AccessibilityRecordCompat obtain(AccessibilityRecordCompat record) {
       return new AccessibilityRecordCompat(IMPL.obtain(record.mRecord));
    }

    /**
     * Returns a cached instance if such is available or a new one is
     * instantiated.
     *
     * @return An instance.
     */
    public static AccessibilityRecordCompat obtain() {
        return new AccessibilityRecordCompat(IMPL.obtain());
    }

    /**
     * Sets the event source.
     *
     * @param source The source.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setSource(View source) {
        IMPL.setSource(mRecord, source);
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
     */
    public void setSource(View root, int virtualDescendantId) {
        IMPL.setSource(mRecord, root, virtualDescendantId);
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
     */
    public AccessibilityNodeInfoCompat getSource() {
        return IMPL.getSource(mRecord);
    }

    /**
     * Gets the id of the window from which the event comes from.
     *
     * @return The window id.
     */
    public int getWindowId() {
        return IMPL.getWindowId(mRecord);
    }

    /**
     * Gets if the source is checked.
     *
     * @return True if the view is checked, false otherwise.
     */
    public boolean isChecked() {
        return IMPL.isChecked(mRecord);
    }

    /**
     * Sets if the source is checked.
     *
     * @param isChecked True if the view is checked, false otherwise.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setChecked(boolean isChecked) {
        IMPL.setChecked(mRecord, isChecked);
    }

    /**
     * Gets if the source is enabled.
     *
     * @return True if the view is enabled, false otherwise.
     */
    public boolean isEnabled() {
        return IMPL.isEnabled(mRecord);
    }

    /**
     * Sets if the source is enabled.
     *
     * @param isEnabled True if the view is enabled, false otherwise.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setEnabled(boolean isEnabled) {
        IMPL.setEnabled(mRecord, isEnabled);
    }

    /**
     * Gets if the source is a password field.
     *
     * @return True if the view is a password field, false otherwise.
     */
    public boolean isPassword() {
        return IMPL.isPassword(mRecord);
    }

    /**
     * Sets if the source is a password field.
     *
     * @param isPassword True if the view is a password field, false otherwise.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setPassword(boolean isPassword) {
        IMPL.setPassword(mRecord, isPassword);
    }

    /**
     * Gets if the source is taking the entire screen.
     *
     * @return True if the source is full screen, false otherwise.
     */
    public boolean isFullScreen() {
        return IMPL.isFullScreen(mRecord);
    }

    /**
     * Sets if the source is taking the entire screen.
     *
     * @param isFullScreen True if the source is full screen, false otherwise.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setFullScreen(boolean isFullScreen) {
        IMPL.setFullScreen(mRecord, isFullScreen);
    }

    /**
     * Gets if the source is scrollable.
     *
     * @return True if the source is scrollable, false otherwise.
     */
    public boolean isScrollable() {
        return IMPL.isScrollable(mRecord);
    }

    /**
     * Sets if the source is scrollable.
     *
     * @param scrollable True if the source is scrollable, false otherwise.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setScrollable(boolean scrollable) {
        IMPL.setScrollable(mRecord, scrollable);
    }

    /**
     * Gets the number of items that can be visited.
     *
     * @return The number of items.
     */
    public int getItemCount() {
        return IMPL.getItemCount(mRecord);
    }

    /**
     * Sets the number of items that can be visited.
     *
     * @param itemCount The number of items.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setItemCount(int itemCount) {
        IMPL.setItemCount(mRecord, itemCount);
    }

    /**
     * Gets the index of the source in the list of items the can be visited.
     *
     * @return The current item index.
     */
    public int getCurrentItemIndex() {
        return IMPL.getCurrentItemIndex(mRecord);
    }

    /**
     * Sets the index of the source in the list of items that can be visited.
     *
     * @param currentItemIndex The current item index.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setCurrentItemIndex(int currentItemIndex) {
        IMPL.setCurrentItemIndex(mRecord, currentItemIndex);
    }

    /**
     * Gets the index of the first character of the changed sequence,
     * or the beginning of a text selection or the index of the first
     * visible item when scrolling.
     *
     * @return The index of the first character or selection
     *        start or the first visible item.
     */
    public int getFromIndex() {
        return IMPL.getFromIndex(mRecord);
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
     */
    public void setFromIndex(int fromIndex) {
        IMPL.setFromIndex(mRecord, fromIndex);
    }

    /**
     * Gets the index of text selection end or the index of the last
     * visible item when scrolling.
     *
     * @return The index of selection end or last item index.
     */
    public int getToIndex() {
        return IMPL.getToIndex(mRecord);
    }

    /**
     * Sets the index of text selection end or the index of the last
     * visible item when scrolling.
     *
     * @param toIndex The index of selection end or last item index.
     */
    public void setToIndex(int toIndex) {
        IMPL.setToIndex(mRecord, toIndex);
    }

    /**
     * Gets the scroll offset of the source left edge in pixels.
     *
     * @return The scroll.
     */
    public int getScrollX() {
        return IMPL.getScrollX(mRecord);
    }

    /**
     * Sets the scroll offset of the source left edge in pixels.
     *
     * @param scrollX The scroll.
     */
    public void setScrollX(int scrollX) {
        IMPL.setScrollX(mRecord, scrollX);
    }

    /**
     * Gets the scroll offset of the source top edge in pixels.
     *
     * @return The scroll.
     */
    public int getScrollY() {
        return IMPL.getScrollY(mRecord);
    }

    /**
     * Sets the scroll offset of the source top edge in pixels.
     *
     * @param scrollY The scroll.
     */
    public void setScrollY(int scrollY) {
        IMPL.setScrollY(mRecord, scrollY);
    }

    /**
     * Gets the max scroll offset of the source left edge in pixels.
     *
     * @return The max scroll.
     */
    public int getMaxScrollX() {
        return IMPL.getMaxScrollX(mRecord);
    }
    /**
     * Sets the max scroll offset of the source left edge in pixels.
     *
     * @param maxScrollX The max scroll.
     */
    public void setMaxScrollX(int maxScrollX) {
        IMPL.setMaxScrollX(mRecord, maxScrollX);
    }

    /**
     * Gets the max scroll offset of the source top edge in pixels.
     *
     * @return The max scroll.
     */
    public int getMaxScrollY() {
        return IMPL.getMaxScrollY(mRecord);
    }

    /**
     * Sets the max scroll offset of the source top edge in pixels.
     *
     * @param maxScrollY The max scroll.
     */
    public void setMaxScrollY(int maxScrollY) {
        IMPL.setMaxScrollY(mRecord, maxScrollY);
    }

    /**
     * Gets the number of added characters.
     *
     * @return The number of added characters.
     */
    public int getAddedCount() {
        return IMPL.getAddedCount(mRecord);
    }

    /**
     * Sets the number of added characters.
     *
     * @param addedCount The number of added characters.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setAddedCount(int addedCount) {
        IMPL.setAddedCount(mRecord, addedCount);
    }

    /**
     * Gets the number of removed characters.
     *
     * @return The number of removed characters.
     */
    public int getRemovedCount() {
        return IMPL.getRemovedCount(mRecord);
    }

    /**
     * Sets the number of removed characters.
     *
     * @param removedCount The number of removed characters.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setRemovedCount(int removedCount) {
        IMPL.setRemovedCount(mRecord, removedCount);
    }

    /**
     * Gets the class name of the source.
     *
     * @return The class name.
     */
    public CharSequence getClassName() {
        return IMPL.getClassName(mRecord);
    }

    /**
     * Sets the class name of the source.
     *
     * @param className The lass name.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setClassName(CharSequence className) {
        IMPL.setClassName(mRecord, className);
    }

    /**
     * Gets the text of the event. The index in the list represents the priority
     * of the text. Specifically, the lower the index the higher the priority.
     *
     * @return The text.
     */
    public List<CharSequence> getText() {
        return IMPL.getText(mRecord);
    }

    /**
     * Sets the text before a change.
     *
     * @return The text before the change.
     */
    public CharSequence getBeforeText() {
        return IMPL.getBeforeText(mRecord);
    }

    /**
     * Sets the text before a change.
     *
     * @param beforeText The text before the change.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setBeforeText(CharSequence beforeText) {
        IMPL.setBeforeText(mRecord, beforeText);
    }

    /**
     * Gets the description of the source.
     *
     * @return The description.
     */
    public CharSequence getContentDescription() {
        return IMPL.getContentDescription(mRecord);
    }

    /**
     * Sets the description of the source.
     *
     * @param contentDescription The description.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setContentDescription(CharSequence contentDescription) {
        IMPL.setContentDescription(mRecord, contentDescription);
    }

    /**
     * Gets the {@link Parcelable} data.
     *
     * @return The parcelable data.
     */
    public Parcelable getParcelableData() {
        return IMPL.getParcelableData(mRecord);
    }

    /**
     * Sets the {@link Parcelable} data of the event.
     *
     * @param parcelableData The parcelable data.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setParcelableData(Parcelable parcelableData) {
        IMPL.setParcelableData(mRecord, parcelableData);
    }

    /**
     * Return an instance back to be reused.
     * <p>
     * <strong>Note:</strong> You must not touch the object after calling this
     * function.
     * </p>
     *
     * @throws IllegalStateException If the record is already recycled.
     */
    public void recycle() {
        IMPL.recycle(mRecord);
    }

    @Override
    public int hashCode() {
        return (mRecord == null) ? 0 : mRecord.hashCode();
    }


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
