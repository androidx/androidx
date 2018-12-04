/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.textclassifier;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;
import androidx.textclassifier.TextClassifier.EntityType;
import androidx.textclassifier.TextClassifier.WidgetType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;

/**
 * A selection event.
 * Specify index parameters as word token indices.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SelectionEvent {
    private static final String EXTRA_ABSOLUTE_START = "extra_absolute_start";
    private static final String EXTRA_ABSOLUTE_END = "extra_absolute_end";
    private static final String EXTRA_ENTITY_TYPE = "extra_entity_type";
    private static final String EXTRA_EVENT_TYPE = "extra_event_type";
    private static final String EXTRA_PACKAGE_NAME = "extra_package_name";
    private static final String EXTRA_WIDGET_TYPE = "extra_widget_type";
    private static final String EXTRA_INVOCATION_METHOD = "extra_invocation_method";
    private static final String EXTRA_WIDGET_VERSION = "extra_widget_version";
    private static final String EXTRA_RESULT_ID = "extra_result_id";
    private static final String EXTRA_EVENT_TIME = "extra_event_time";
    private static final String EXTRA_DURATION_SINCE_SESSION_START =
            "extra_duration_since_session_start";
    private static final String EXTRA_DURATION_SINCE_PREVIOUS_EVENT =
            "extra_duration_since_previous_event";
    private static final String EXTRA_EVENT_INDEX = "extra_event_index";
    private static final String EXTRA_SESSION_ID = "extra_session_id";
    private static final String EXTRA_START = "extra_start";
    private static final String EXTRA_END = "extra_end";
    private static final String EXTRA_SMART_START = "extra_smart_start";
    private static final String EXTRA_SMART_END = "extra_smart_end";

    /** User typed over the selection. */
    public static final int ACTION_OVERTYPE =
            android.view.textclassifier.SelectionEvent.ACTION_OVERTYPE;
    /** User copied the selection. */
    public static final int ACTION_COPY = android.view.textclassifier.SelectionEvent.ACTION_COPY;
    /** User pasted over the selection. */
    public static final int ACTION_PASTE = android.view.textclassifier.SelectionEvent.ACTION_PASTE;
    /** User cut the selection. */
    public static final int ACTION_CUT = android.view.textclassifier.SelectionEvent.ACTION_CUT;
    /** User shared the selection. */
    public static final int ACTION_SHARE = android.view.textclassifier.SelectionEvent.ACTION_SHARE;
    /** User clicked the textAssist menu item. */
    public static final int ACTION_SMART_SHARE =
            android.view.textclassifier.SelectionEvent.ACTION_SMART_SHARE;
    /** User dragged+dropped the selection. */
    public static final int ACTION_DRAG = android.view.textclassifier.SelectionEvent.ACTION_DRAG;
    /** User abandoned the selection. */
    public static final int ACTION_ABANDON =
            android.view.textclassifier.SelectionEvent.ACTION_ABANDON;
    /** User performed an action on the selection. */
    public static final int ACTION_OTHER = android.view.textclassifier.SelectionEvent.ACTION_OTHER;

    // Non-terminal actions.
    /** User activated Select All */
    public static final int ACTION_SELECT_ALL =
            android.view.textclassifier.SelectionEvent.ACTION_SELECT_ALL;
    /** User reset the smart selection. */
    public static final int ACTION_RESET = android.view.textclassifier.SelectionEvent.ACTION_RESET;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ACTION_OVERTYPE, ACTION_COPY, ACTION_PASTE, ACTION_CUT,
            ACTION_SHARE, ACTION_SMART_SHARE, ACTION_DRAG, ACTION_ABANDON,
            ACTION_OTHER, ACTION_SELECT_ALL, ACTION_RESET,
            EVENT_SELECTION_STARTED, EVENT_SELECTION_MODIFIED,
            EVENT_SMART_SELECTION_SINGLE, EVENT_SMART_SELECTION_MULTI,
            EVENT_AUTO_SELECTION})
    // NOTE: EventTypes declared here must be less than 100 to avoid colliding with the
    // ActionTypes declared above.
    public @interface EventType {
        /*
         * Range: 1 -> 99.
         */
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ACTION_OVERTYPE, ACTION_COPY, ACTION_PASTE, ACTION_CUT,
            ACTION_SHARE, ACTION_SMART_SHARE, ACTION_DRAG, ACTION_ABANDON,
            ACTION_OTHER, ACTION_SELECT_ALL, ACTION_RESET})
    // NOTE: ActionType values should not be lower than 100 to avoid colliding with the other
    // EventTypes declared below.
    public @interface ActionType {
        /*
         * Terminal event types range: [100,200).
         * Non-terminal event types range: [200,300).
         */
    }

    /** User started a new selection. */
    public static final int EVENT_SELECTION_STARTED =
            android.view.textclassifier.SelectionEvent.EVENT_SELECTION_STARTED;
    /** User modified an existing selection. */
    public static final int EVENT_SELECTION_MODIFIED =
            android.view.textclassifier.SelectionEvent.EVENT_SELECTION_MODIFIED;
    /** Smart selection triggered for a single token (word). */
    public static final int EVENT_SMART_SELECTION_SINGLE =
            android.view.textclassifier.SelectionEvent.EVENT_SMART_SELECTION_SINGLE;
    /** Smart selection triggered spanning multiple tokens (words). */
    public static final int EVENT_SMART_SELECTION_MULTI =
            android.view.textclassifier.SelectionEvent.EVENT_SMART_SELECTION_MULTI;
    /** Something else other than User or the default TextClassifier triggered a selection. */
    public static final int EVENT_AUTO_SELECTION =
            android.view.textclassifier.SelectionEvent.EVENT_AUTO_SELECTION;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({INVOCATION_MANUAL, INVOCATION_LINK, INVOCATION_UNKNOWN})
    public @interface InvocationMethod {}

    /** Selection was invoked by the user long pressing, double tapping, or dragging to select. */
    public static final int INVOCATION_MANUAL =
            android.view.textclassifier.SelectionEvent.INVOCATION_MANUAL;
    /** Selection was invoked by the user tapping on a link. */
    public static final int INVOCATION_LINK =
            android.view.textclassifier.SelectionEvent.INVOCATION_LINK;
    /** Unknown invocation method */
    public static final int INVOCATION_UNKNOWN =
            android.view.textclassifier.SelectionEvent.INVOCATION_UNKNOWN;

    private static final String NO_SIGNATURE = "";

    private final int mAbsoluteStart;
    private final int mAbsoluteEnd;
    private final @EntityType String mEntityType;

    private @EventType int mEventType;
    private String mPackageName = "";
    private String mWidgetType = TextClassifier.WIDGET_TYPE_UNKNOWN;
    private @InvocationMethod int mInvocationMethod;
    @Nullable private String mWidgetVersion;
    @Nullable private String mResultId;
    private long mEventTime;
    private long mDurationSinceSessionStart;
    private long mDurationSincePreviousEvent;
    private int mEventIndex;
    @Nullable private TextClassificationSessionId mSessionId;
    private int mStart;
    private int mEnd;
    private int mSmartStart;
    private int mSmartEnd;

    // For SL -> Platform conversion.
    @Nullable
    private TextClassification mTextClassification;
    @Nullable
    private TextSelection mTextSelection;

    @SuppressLint("RestrictedApi")
    /* package */ SelectionEvent(
            int start, int end,
            @EventType int eventType, @EntityType String entityType,
            @InvocationMethod int invocationMethod, @Nullable String resultId) {
        Preconditions.checkArgument(end >= start, "end cannot be less than start");
        mAbsoluteStart = start;
        mAbsoluteEnd = end;
        mEventType = eventType;
        mEntityType = Preconditions.checkNotNull(entityType);
        mResultId = resultId;
        mInvocationMethod = invocationMethod;
    }

    private SelectionEvent(int absoluteStart, int absoluteEnd, @EntityType String entityType,
            @EventType int eventType, String packageName, String widgetType,
            @InvocationMethod int invocationMethod, String widgetVersion, String resultId,
            long eventTime, long durationSinceSessionStart, long durationSincePreviousEvent,
            int eventIndex, TextClassificationSessionId sessionId, int start, int end,
            int smartStart, int smartEnd) {
        mAbsoluteStart = absoluteStart;
        mAbsoluteEnd = absoluteEnd;
        mEntityType = entityType;
        mEventType = eventType;
        mPackageName = packageName;
        mWidgetType = widgetType;
        mInvocationMethod = invocationMethod;
        mWidgetVersion = widgetVersion;
        mResultId = resultId;
        mEventTime = eventTime;
        mDurationSinceSessionStart = durationSinceSessionStart;
        mDurationSincePreviousEvent = durationSincePreviousEvent;
        mEventIndex = eventIndex;
        mSessionId = sessionId;
        mStart = start;
        mEnd = end;
        mSmartStart = smartStart;
        mSmartEnd = smartEnd;
    }

    /**
     * Adds this Icon to a Bundle that can be read back with the same parameters
     * to {@link #createFromBundle(Bundle)}.
     */
    @NonNull
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_ABSOLUTE_START, mAbsoluteStart);
        bundle.putInt(EXTRA_ABSOLUTE_END, mAbsoluteEnd);
        bundle.putString(EXTRA_ENTITY_TYPE, mEntityType);
        bundle.putInt(EXTRA_EVENT_TYPE, mEventType);
        bundle.putString(EXTRA_PACKAGE_NAME, mPackageName);
        bundle.putString(EXTRA_WIDGET_TYPE, mWidgetType);
        bundle.putInt(EXTRA_INVOCATION_METHOD, mInvocationMethod);
        bundle.putString(EXTRA_WIDGET_VERSION, mWidgetVersion);
        bundle.putString(EXTRA_RESULT_ID, mResultId);
        bundle.putLong(EXTRA_EVENT_TIME, mEventTime);
        bundle.putLong(EXTRA_DURATION_SINCE_SESSION_START, mDurationSinceSessionStart);
        bundle.putLong(EXTRA_DURATION_SINCE_PREVIOUS_EVENT, mDurationSincePreviousEvent);
        bundle.putInt(EXTRA_EVENT_INDEX, mEventIndex);
        if (mSessionId != null) {
            bundle.putBundle(EXTRA_SESSION_ID, mSessionId.toBundle());
        }
        bundle.putInt(EXTRA_START, mStart);
        bundle.putInt(EXTRA_END, mEnd);
        bundle.putInt(EXTRA_SMART_START, mSmartStart);
        bundle.putInt(EXTRA_SMART_END, mSmartEnd);
        return bundle;
    }

    /**
     * Extracts a {@link SelectionEvent} from a bundle that was added using {@link #toBundle()}.
     */
    @NonNull
    public static SelectionEvent createFromBundle(@NonNull Bundle bundle) {
        final int absoluteStart = bundle.getInt(EXTRA_ABSOLUTE_START);
        final int absoluteEnd = bundle.getInt(EXTRA_ABSOLUTE_END);
        final String entityType = bundle.getString(EXTRA_ENTITY_TYPE);
        final int eventType = bundle.getInt(EXTRA_EVENT_TYPE);
        final String packageName = bundle.getString(EXTRA_PACKAGE_NAME);
        final String widgetType = bundle.getString(EXTRA_WIDGET_TYPE);
        final int invocationMethod = bundle.getInt(EXTRA_INVOCATION_METHOD);
        final String widgetVersion = bundle.getString(EXTRA_WIDGET_VERSION);
        final String resultId = bundle.getString(EXTRA_RESULT_ID);
        final long eventTime = bundle.getLong(EXTRA_EVENT_TIME);
        final long durationSinceSessionStart = bundle.getLong(EXTRA_DURATION_SINCE_SESSION_START);
        final long durationSincePreviousEvent = bundle.getLong(EXTRA_DURATION_SINCE_PREVIOUS_EVENT);
        final int eventIndex = bundle.getInt(EXTRA_EVENT_INDEX);
        final TextClassificationSessionId sessionId = bundle.getBundle(EXTRA_SESSION_ID) == null
                ? null
                : TextClassificationSessionId.createFromBundle(bundle.getBundle(EXTRA_SESSION_ID));
        final int start = bundle.getInt(EXTRA_START);
        final int end = bundle.getInt(EXTRA_END);
        final int smartStart = bundle.getInt(EXTRA_SMART_START);
        final int smartEnd = bundle.getInt(EXTRA_SMART_END);
        return new SelectionEvent(absoluteStart, absoluteEnd, entityType, eventType, packageName,
                widgetType, invocationMethod, widgetVersion, resultId, eventTime,
                durationSinceSessionStart, durationSincePreviousEvent, eventIndex,
                sessionId, start, end, smartStart, smartEnd);
    }

    /**
     * Creates a "selection started" event.
     *
     * @param invocationMethod  the way the selection was triggered
     * @param start  the index of the selected text
     */
    @NonNull
    public static SelectionEvent createSelectionStartedEvent(
            @SelectionEvent.InvocationMethod int invocationMethod, int start) {
        return new SelectionEvent(
                start, start + 1, SelectionEvent.EVENT_SELECTION_STARTED,
                TextClassifier.TYPE_UNKNOWN, invocationMethod, NO_SIGNATURE);
    }

    /**
     * Creates a "selection modified" event.
     * Use when the user modifies the selection.
     *
     * @param start  the start (inclusive) index of the selection
     * @param end  the end (exclusive) index of the selection
     *
     * @throws IllegalArgumentException if end is less than start
     */
    @NonNull
    @SuppressLint("RestrictedApi")
    public static SelectionEvent createSelectionModifiedEvent(int start, int end) {
        Preconditions.checkArgument(end >= start, "end cannot be less than start");
        return new SelectionEvent(
                start, end, SelectionEvent.EVENT_SELECTION_MODIFIED,
                TextClassifier.TYPE_UNKNOWN, INVOCATION_UNKNOWN, NO_SIGNATURE);
    }

    /**
     * Creates a "selection modified" event.
     * Use when the user modifies the selection and the selection's entity type is known.
     *
     * @param start  the start (inclusive) index of the selection
     * @param end  the end (exclusive) index of the selection
     * @param classification  the TextClassification object returned by the text classifier that
     *      classified the selected text
     *
     * @throws IllegalArgumentException if end is less than start
     */
    @NonNull
    @SuppressLint("RestrictedApi")
    public static SelectionEvent createSelectionModifiedEvent(
            int start, int end, @NonNull TextClassification classification) {
        Preconditions.checkArgument(end >= start, "end cannot be less than start");
        Preconditions.checkNotNull(classification);
        final String entityType = classification.getEntityTypeCount() > 0
                ? classification.getEntityType(0)
                : TextClassifier.TYPE_UNKNOWN;

        SelectionEvent selectionEvent = new SelectionEvent(
                start, end, SelectionEvent.EVENT_SELECTION_MODIFIED,
                entityType, INVOCATION_UNKNOWN, classification.getId());
        selectionEvent.mTextClassification = classification;
        return selectionEvent;
    }

    /**
     * Creates a "selection modified" event.
     * Use when a TextClassifier modifies the selection.
     *
     * @param start  the start (inclusive) index of the selection
     * @param end  the end (exclusive) index of the selection
     * @param selection  the TextSelection object returned by the text classifier for the
     *      specified selection
     *
     * @throws IllegalArgumentException if end is less than start
     */
    @NonNull
    @SuppressLint("RestrictedApi")
    public static SelectionEvent createSelectionModifiedEvent(
            int start, int end, @NonNull TextSelection selection) {
        Preconditions.checkArgument(end >= start, "end cannot be less than start");
        Preconditions.checkNotNull(selection);
        final String entityType = selection.getEntityCount() > 0
                ? selection.getEntity(0)
                : TextClassifier.TYPE_UNKNOWN;
        SelectionEvent selectionEvent = new SelectionEvent(
                start, end, SelectionEvent.EVENT_AUTO_SELECTION,
                entityType, INVOCATION_UNKNOWN, selection.getId());
        selectionEvent.mTextSelection = selection;
        return selectionEvent;
    }

    /**
     * Creates an event specifying an action taken on a selection.
     * Use when the user clicks on an action to act on the selected text.
     *
     * @param start  the start (inclusive) index of the selection
     * @param end  the end (exclusive) index of the selection
     * @param actionType  the action that was performed on the selection
     *
     * @throws IllegalArgumentException if end is less than start
     */
    @NonNull
    @SuppressLint("RestrictedApi")
    public static SelectionEvent createSelectionActionEvent(
            int start, int end, @SelectionEvent.ActionType int actionType) {
        Preconditions.checkArgument(end >= start, "end cannot be less than start");
        checkActionType(actionType);
        return new SelectionEvent(
                start, end, actionType, TextClassifier.TYPE_UNKNOWN, INVOCATION_UNKNOWN,
                NO_SIGNATURE);
    }

    /**
     * Creates an event specifying an action taken on a selection.
     * Use when the user clicks on an action to act on the selected text and the selection's
     * entity type is known.
     *
     * @param start  the start (inclusive) index of the selection
     * @param end  the end (exclusive) index of the selection
     * @param actionType  the action that was performed on the selection
     * @param classification  the TextClassification object returned by the text classifier that
     *      classified the selected text
     *
     * @throws IllegalArgumentException if end is less than start
     * @throws IllegalArgumentException If actionType is not a valid SelectionEvent actionType
     */
    @NonNull
    @SuppressLint("RestrictedApi")
    public static SelectionEvent createSelectionActionEvent(
            int start, int end, @SelectionEvent.ActionType int actionType,
            @NonNull TextClassification classification) {
        Preconditions.checkArgument(end >= start, "end cannot be less than start");
        Preconditions.checkNotNull(classification);
        checkActionType(actionType);
        final String entityType = classification.getEntityTypeCount() > 0
                ? classification.getEntityType(0)
                : TextClassifier.TYPE_UNKNOWN;
        SelectionEvent selectionEvent =
                new SelectionEvent(start, end, actionType, entityType, INVOCATION_UNKNOWN,
                        classification.getId());
        selectionEvent.mTextClassification = classification;
        return selectionEvent;
    }

    /**
     * @throws IllegalArgumentException If eventType is not an {@link SelectionEvent.ActionType}
     */
    private static void checkActionType(@SelectionEvent.EventType int eventType)
            throws IllegalArgumentException {
        switch (eventType) {
            case SelectionEvent.ACTION_OVERTYPE:  // fall through
            case SelectionEvent.ACTION_COPY:  // fall through
            case SelectionEvent.ACTION_PASTE:  // fall through
            case SelectionEvent.ACTION_CUT:  // fall through
            case SelectionEvent.ACTION_SHARE:  // fall through
            case SelectionEvent.ACTION_SMART_SHARE:  // fall through
            case SelectionEvent.ACTION_DRAG:  // fall through
            case SelectionEvent.ACTION_ABANDON:  // fall through
            case SelectionEvent.ACTION_SELECT_ALL:  // fall through
            case SelectionEvent.ACTION_RESET:  // fall through
            case SelectionEvent.ACTION_OTHER:  // fall through
                return;
            default:
                throw new IllegalArgumentException(
                        String.format(Locale.US, "%d is not an eventType", eventType));
        }
    }

    int getAbsoluteStart() {
        return mAbsoluteStart;
    }

    int getAbsoluteEnd() {
        return mAbsoluteEnd;
    }

    /**
     * Returns the type of event that was triggered. e.g. {@link #ACTION_COPY}.
     */
    @EventType
    public int getEventType() {
        return mEventType;
    }

    /**
     * Sets the event type.
     */
    void setEventType(@EventType int eventType) {
        mEventType = eventType;
    }

    /**
     * Returns the type of entity that is associated with this event. e.g.
     * {@link android.view.textclassifier.TextClassifier#TYPE_EMAIL}.
     */
    @EntityType
    @NonNull
    public String getEntityType() {
        return mEntityType;
    }

    /**
     * Returns the package name of the app that this event originated in.
     */
    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    /**
     * Returns the type of widget that was involved in triggering this event.
     */
    @WidgetType
    @NonNull
    public String getWidgetType() {
        return mWidgetType;
    }

    /**
     * Returns a string version info for the widget this event was triggered in.
     */
    @Nullable
    public String getWidgetVersion() {
        return mWidgetVersion;
    }

    /**
     * Sets the {@link TextClassificationContext} for this event.
     */
    void setTextClassificationSessionContext(TextClassificationContext context) {
        mPackageName = context.getPackageName();
        mWidgetType = context.getWidgetType();
        mWidgetVersion = context.getWidgetVersion();
    }

    /**
     * Returns the way the selection mode was invoked.
     */
    public @InvocationMethod int getInvocationMethod() {
        return mInvocationMethod;
    }

    /**
     * Sets the invocationMethod for this event.
     */
    void setInvocationMethod(@InvocationMethod int invocationMethod) {
        mInvocationMethod = invocationMethod;
    }

    /**
     * Returns the id of the text classifier result associated with this event.
     */
    @Nullable
    public String getResultId() {
        return mResultId;
    }

    SelectionEvent setResultId(@Nullable String resultId) {
        mResultId = resultId;
        return this;
    }

    /**
     * Returns the time this event was triggered.
     */
    public long getEventTime() {
        return mEventTime;
    }

    SelectionEvent setEventTime(long timeMs) {
        mEventTime = timeMs;
        return this;
    }

    /**
     * Returns the duration in ms between when this event was triggered and when the first event in
     * the selection session was triggered.
     */
    public long getDurationSinceSessionStart() {
        return mDurationSinceSessionStart;
    }

    SelectionEvent setDurationSinceSessionStart(long durationMs) {
        mDurationSinceSessionStart = durationMs;
        return this;
    }

    /**
     * Returns the duration in ms between when this event was triggered and when the previous event
     * in the selection session was triggered.
     */
    public long getDurationSincePreviousEvent() {
        return mDurationSincePreviousEvent;
    }

    SelectionEvent setDurationSincePreviousEvent(long durationMs) {
        this.mDurationSincePreviousEvent = durationMs;
        return this;
    }

    /**
     * Returns the index (e.g. 1st event, 2nd event, etc.) of this event in the selection session.
     */
    public int getEventIndex() {
        return mEventIndex;
    }

    SelectionEvent setEventIndex(int index) {
        mEventIndex = index;
        return this;
    }

    /**
     * Returns the selection session id.
     */
    @Nullable
    public TextClassificationSessionId getSessionId() {
        return mSessionId;
    }

    SelectionEvent setSessionId(TextClassificationSessionId id) {
        mSessionId = id;
        return this;
    }

    /**
     * Returns the start index of this events relative to the index of the start selection
     * event in the selection session.
     */
    public int getStart() {
        return mStart;
    }

    SelectionEvent setStart(int start) {
        mStart = start;
        return this;
    }

    /**
     * Returns the end index of this events relative to the index of the start selection
     * event in the selection session.
     */
    public int getEnd() {
        return mEnd;
    }

    SelectionEvent setEnd(int end) {
        mEnd = end;
        return this;
    }

    /**
     * Returns the start index of this events relative to the index of the smart selection
     * event in the selection session.
     */
    public int getSmartStart() {
        return mSmartStart;
    }

    SelectionEvent setSmartStart(int start) {
        this.mSmartStart = start;
        return this;
    }

    /**
     * Returns the end index of this events relative to the index of the smart selection
     * event in the selection session.
     */
    public int getSmartEnd() {
        return mSmartEnd;
    }

    SelectionEvent setSmartEnd(int end) {
        mSmartEnd = end;
        return this;
    }

    boolean isTerminal() {
        return isTerminal(mEventType);
    }

    /**
     * Returns true if the eventType is a terminal event type. Otherwise returns false.
     * A terminal event is an event that ends a selection interaction.
     */
    public static boolean isTerminal(@EventType int eventType) {
        switch (eventType) {
            case ACTION_OVERTYPE:  // fall through
            case ACTION_COPY:  // fall through
            case ACTION_PASTE:  // fall through
            case ACTION_CUT:  // fall through
            case ACTION_SHARE:  // fall through
            case ACTION_SMART_SHARE:  // fall through
            case ACTION_DRAG:  // fall through
            case ACTION_ABANDON:  // fall through
            case ACTION_OTHER:  // fall through
                return true;
            default:
                return false;
        }
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(mAbsoluteStart, mAbsoluteEnd, mEventType, mEntityType,
                mWidgetVersion, mPackageName, mWidgetType, mInvocationMethod, mResultId,
                mEventTime, mDurationSinceSessionStart, mDurationSincePreviousEvent,
                mEventIndex, mSessionId, mStart, mEnd, mSmartStart, mSmartEnd);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SelectionEvent)) {
            return false;
        }

        final SelectionEvent other = (SelectionEvent) obj;
        return mAbsoluteStart == other.mAbsoluteStart
                && mAbsoluteEnd == other.mAbsoluteEnd
                && mEventType == other.mEventType
                && ObjectsCompat.equals(mEntityType, other.mEntityType)
                && ObjectsCompat.equals(mWidgetVersion, other.mWidgetVersion)
                && ObjectsCompat.equals(mPackageName, other.mPackageName)
                && ObjectsCompat.equals(mWidgetType, other.mWidgetType)
                && mInvocationMethod == other.mInvocationMethod
                && ObjectsCompat.equals(mResultId, other.mResultId)
                && mEventTime == other.mEventTime
                && mDurationSinceSessionStart == other.mDurationSinceSessionStart
                && mDurationSincePreviousEvent == other.mDurationSincePreviousEvent
                && mEventIndex == other.mEventIndex
                && ObjectsCompat.equals(mSessionId, other.mSessionId)
                && mStart == other.mStart
                && mEnd == other.mEnd
                && mSmartStart == other.mSmartStart
                && mSmartEnd == other.mSmartEnd;
    }

    @Override
    public String toString() {
        return String.format(Locale.US,
                "SelectionEvent {absoluteStart=%d, absoluteEnd=%d, eventType=%d, entityType=%s, "
                        + "widgetVersion=%s, packageName=%s, widgetType=%s, invocationMethod=%s, "
                        + "resultId=%s, eventTime=%d, durationSinceSessionStart=%d, "
                        + "durationSincePreviousEvent=%d, eventIndex=%d,"
                        + "sessionId=%s, start=%d, end=%d, smartStart=%d, smartEnd=%d}",
                mAbsoluteStart, mAbsoluteEnd, mEventType, mEntityType,
                mWidgetVersion, mPackageName, mWidgetType, mInvocationMethod,
                mResultId, mEventTime, mDurationSinceSessionStart,
                mDurationSincePreviousEvent, mEventIndex,
                mSessionId, mStart, mEnd, mSmartStart, mSmartEnd);
    }

    /**
     * Converts {@link android.view.textclassifier.TextSelection} to
     * {@link android.view.textclassifier.TextSelection}. It can only convert text selection
     * objects created by those factory methods and without further modification.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresApi(28)
    @NonNull
    // Lint does not know the constants in platform and here are same
    @SuppressLint({"WrongConstant", "RestrictedApi"})
    Object toPlatform(@NonNull Context context) {
        Preconditions.checkNotNull(context);

        if (getEventType() == EVENT_SELECTION_STARTED) {
            return android.view.textclassifier.SelectionEvent.createSelectionStartedEvent(
                    getInvocationMethod(),
                    getAbsoluteStart()
            );
        }
        if (getEventType() == EVENT_AUTO_SELECTION && mTextSelection != null) {
            return android.view.textclassifier.SelectionEvent.createSelectionModifiedEvent(
                    getAbsoluteStart(),
                    getAbsoluteEnd(),
                    (android.view.textclassifier.TextSelection) mTextSelection.toPlatform());
        }
        if (getEventType() == EVENT_SELECTION_MODIFIED) {
            return toPlatformSelectionModifiedEvent(context);
        }
        return toPlatformSelectionActionEvent(context);
    }

    @NonNull
    @RequiresApi(28)
    @SuppressLint("RestrictedApi")
    private android.view.textclassifier.SelectionEvent toPlatformSelectionModifiedEvent(
            @NonNull Context context) {
        Preconditions.checkNotNull(context);

        if (mTextClassification != null) {
            return android.view.textclassifier.SelectionEvent.createSelectionModifiedEvent(
                    getAbsoluteStart(),
                    getAbsoluteEnd(),
                    (android.view.textclassifier.TextClassification)
                            mTextClassification.toPlatform(context)
            );
        }
        return android.view.textclassifier.SelectionEvent.createSelectionModifiedEvent(
                getAbsoluteStart(),
                getAbsoluteEnd()
        );
    }

    @NonNull
    @RequiresApi(28)
    // Lint does not know the constants in platform and here are same
    @SuppressLint({"WrongConstant", "RestrictedApi"})
    private android.view.textclassifier.SelectionEvent toPlatformSelectionActionEvent(
            @NonNull Context context) {
        Preconditions.checkNotNull(context);

        if (mTextClassification != null) {
            return android.view.textclassifier.SelectionEvent.createSelectionActionEvent(
                    getAbsoluteStart(),
                    getAbsoluteEnd(),
                    getEventType(),
                    (android.view.textclassifier.TextClassification)
                            mTextClassification.toPlatform(context)
            );
        }
        return android.view.textclassifier.SelectionEvent.createSelectionActionEvent(
                getAbsoluteStart(),
                getAbsoluteEnd(),
                getEventType()
        );
    }
}
