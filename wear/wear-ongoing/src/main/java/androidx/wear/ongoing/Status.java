/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.wear.ongoing;

import android.content.Context;
import android.text.SpannableStringBuilder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base class to represent the status of an Ongoing Activity and render it.
 * <p>
 * A status is composed of Parts, and they are joined together with a template.
 * <p>
 * Note that for backwards compatibility reasons the code rendering this status message may not
 * have all of the [Part] classes that are available in later versions of the library.
 * Templates that do not have values for all of the named parts will not be used.
 * The template list will be iterated through looking for the first template with all matching named
 * parts available, this will be selected for rendering the status.
 * <p>
 * To provide for backwards compatibility, you should provide one (or more) fallback templates which
 * use status parts from earlier versions of the API. e.g. TextPart, TimerPart & StopwatchPart
 * <p>
 * The status and part classes here use timestamps for updating the displayed representation of the
 * status, in cases when this is needed (chronometers), as returned by
 * {@link android.os.SystemClock#elapsedRealtime()}
 */
public final class Status implements TimeDependentText {
    @NonNull
    final List<CharSequence> mTemplates;

    @NonNull
    private final Map<String, StatusPart> mParts;

    /**
     * Abstract class to represent An Ongoing activity status or part of it.
     * <p>
     * Parts are used to create complex statuses, that may contain several timers, placeholders for
     * text, etc. They may also be used to convey information to the system about this Ongoing
     * Activity.
     */
    public abstract static class Part implements TimeDependentText {
        // Hide constructor.
        Part() {
        }

        @Nullable
        StatusPart toVersionedParcelable() {
            return null;
        }

        @Nullable
        static Part fromVersionedParcelable(@Nullable StatusPart vp) {
            if (vp == null) {
                return null;
            }
            if (vp instanceof TextStatusPart) {
                return new TextPart((TextStatusPart) vp);
            } else if (vp instanceof TimerStatusPart) {
                TimerStatusPart tsp = (TimerStatusPart) vp;
                return tsp.mCountDown ? new TimerPart(tsp) : new StopwatchPart(tsp);
            } else {
                return null;
            }
        }
    }

    /**
     * An Ongoing activity status (or part of it) representing a plain, static text.
     * <p>
     * Available since wear-ongoing:1.0.0
     */
    public static final class TextPart extends Part {
        @NonNull
        private final TextStatusPart mPart;

        TextPart(@NonNull TextStatusPart part) {
            mPart = part;
        }

        /**
         * Create a Part representing a static text.
         */
        public TextPart(@NonNull String str) {
            mPart = new TextStatusPart(str);
        }

        @Override
        @NonNull
        StatusPart toVersionedParcelable() {
            return mPart;
        }

        /**
         * See {@link TimeDependentText#getText(Context, long)}
         */
        @NonNull
        @Override
        public CharSequence getText(@NonNull Context context, long timeNowMillis) {
            return mPart.getText(context, timeNowMillis);
        }

        /**
         * See {@link TimeDependentText#getNextChangeTimeMillis(long)}
         */
        @Override
        public long getNextChangeTimeMillis(long fromTimeMillis) {
            return mPart.getNextChangeTimeMillis(fromTimeMillis);
        }

        @Override
        public int hashCode() {
            return mPart.hashCode();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof TextPart)) return false;
            return mPart.equals(((TextPart) obj).mPart);
        }
    }

    /**
     * Base class for {@link TimerPart} and {@link StopwatchPart}, defines the getters but can't
     * be created directly, create one of those instead.
     */
    public abstract static class TimerOrStopwatchPart extends Part {
        @NonNull
        private final TimerStatusPart mPart;

        TimerOrStopwatchPart(@NonNull TimerStatusPart part) {
            mPart = part;
        }

        /**
         * @return the time at which this Timer or Stopwatch will display 0, will usually be in the
         * past for a stopwatch and in the future for timers.
         */
        public long getTimeZeroMillis() {
            return mPart.mTimeZeroMillis;
        }

        /**
         * @return {@code false} if this is a stopwatch or {@code true} if this is a timer.
         */
        public boolean isCountDown() {
            return mPart.mCountDown;
        }

        /**
         * Determines if this Timer or Stopwatch is paused. i.e. the display representation will
         * not change over time.
         *
         * @return {@code true} if this is paused, {@code false} if it's running.
         */
        public boolean isPaused() {
            return mPart.isPaused();
        }

        /**
         * @return the timestamp of the time when this was paused. Use
         * {@link #isPaused()} to determine if this is paused or not.
         */
        public long getPausedAtMillis() {
            return mPart.mPausedAtMillis;
        }

        /**
         * Determines if this has a total duration set.
         *
         * @return {@code true} if this the total duration was set, {@code false} if not.
         */
        public boolean hasTotalDuration() {
            return mPart.mTotalDurationMillis >= 0L;
        }

        /**
         * @return the total duration of this timer/stopwatch, if set. Use
         * {@link #hasTotalDuration()} to determine if this has a duration set.
         */
        public long getTotalDurationMillis() {
            return mPart.mTotalDurationMillis;
        }

        @Override
        @NonNull
        StatusPart toVersionedParcelable() {
            return mPart;
        }

        @Override
        public int hashCode() {
            return mPart.hashCode();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof TimerOrStopwatchPart)) return false;
            return mPart.equals(((TimerOrStopwatchPart) obj).mPart);
        }

        /**
         * See {@link TimeDependentText#getText(Context, long)}
         */
        @NonNull
        @Override
        public CharSequence getText(@NonNull Context context, long timeNowMillis) {
            return mPart.getText(context, timeNowMillis);
        }

        /**
         * See {@link TimeDependentText#getNextChangeTimeMillis(long)}
         */
        @Override
        public long getNextChangeTimeMillis(long fromTimeMillis) {
            return mPart.getNextChangeTimeMillis(fromTimeMillis);
        }
    }

    /**
     * An Ongoing activity status (or part of it) representing a timer.
     * <p>
     * Available since wear-ongoing:1.0.0
     */
    public static final class TimerPart extends TimerOrStopwatchPart {
        TimerPart(@NonNull TimerStatusPart part) {
            super(part);
        }

        /**
         * Create a Part representing a timer.
         *
         * @param timeZeroMillis      timestamp of the time at the future in which this Timer
         *                            should display 0.
         * @param pausedAtMillis      timestamp of the time when this timer was paused. Or
         *                            {@code -1L} if this timer is running.
         * @param totalDurationMillis total duration of this timer, useful to display as a
         *                            progress bar or similar.
         */
        public TimerPart(long timeZeroMillis, long pausedAtMillis,
                long totalDurationMillis) {
            super(new TimerStatusPart(
                    timeZeroMillis,
                    /* countDown = */ true,
                    pausedAtMillis,
                    totalDurationMillis
            ));
        }

        /**
         * Create a Part representing a timer.
         *
         * @param timeZeroMillis timestamp of the time at the future in which this Timer
         *                       should display 0.
         * @param pausedAtMillis timestamp of the time when this timer was paused. Or
         *                       {@code -1L} if this timer is running.
         */
        public TimerPart(long timeZeroMillis, long pausedAtMillis) {
            this(timeZeroMillis, pausedAtMillis, TimerStatusPart.LONG_DEFAULT);
        }

        /**
         * Create a Part representing a timer.
         *
         * @param timeZeroMillis timestamp of the time at the future in which this Timer
         *                       should display 0.
         */
        public TimerPart(long timeZeroMillis) {
            this(timeZeroMillis, TimerStatusPart.LONG_DEFAULT);
        }
    }

    /**
     * An Ongoing activity status (or part of it) representing a stopwatch
     * <p>
     * Available since wear-ongoing:1.0.0
     */
    public static final class StopwatchPart extends TimerOrStopwatchPart {
        StopwatchPart(@NonNull TimerStatusPart part) {
            super(part);
        }

        /**
         * Create a Part representing a stopwatch.
         *
         * @param timeZeroMillis      timestamp of the time at which this stopwatch started
         *                            running.
         * @param pausedAtMillis      timestamp of the time when this stopwatch was paused. Or
         *                            {@code -1L} if this stopwatch is running.
         * @param totalDurationMillis total duration of this stopwatch, useful to display as a
         *                            progress bar or similar.
         */
        public StopwatchPart(long timeZeroMillis, long pausedAtMillis, long totalDurationMillis) {
            super(new TimerStatusPart(
                    timeZeroMillis,
                    /* countDown = */ false,
                    pausedAtMillis,
                    totalDurationMillis
            ));
        }

        /**
         * Create a Part representing a stopwatch.
         *
         * @param timeZeroMillis timestamp of the time at which this stopwatch started
         *                       running.
         * @param pausedAtMillis timestamp of the time when this stopwatch was paused. Or
         *                       {@code -1L} if this stopwatch is running.
         */
        public StopwatchPart(long timeZeroMillis, long pausedAtMillis) {
            this(timeZeroMillis, pausedAtMillis, TimerStatusPart.LONG_DEFAULT);
        }


        /**
         * Create a Part representing a stopwatch.
         *
         * @param timeZeroMillis timestamp of the time at which this stopwatch started
         *                       running.
         */
        public StopwatchPart(long timeZeroMillis) {
            this(timeZeroMillis, TimerStatusPart.LONG_DEFAULT);
        }
    }

    // Name of the {@link StatusPart} created when using {@link OngoingActivityStatus.forPart()}
    private static final String DEFAULT_STATUS_PART_NAME = "defaultStatusPartName";

    // Basic constructor used by the Builder
    @VisibleForTesting
    Status(
            @Nullable List<CharSequence> templates,
            @NonNull Map<String, StatusPart> parts
    ) {
        mTemplates = templates;
        mParts = parts;
    }

    OngoingActivityStatus toVersionedParcelable() {
        return new OngoingActivityStatus(mTemplates, mParts);
    }

    static Status fromVersionedParcelable(OngoingActivityStatus vp) {
        return new Status(vp.mTemplates, vp.mParts);
    }

    /**
     * Convenience method for creating a Status with no template and a single Part.
     *
     * @param part The only Part that composes this status.
     * @return A new {@link Status} with just one Part.
     */
    @NonNull
    public static Status forPart(@NonNull Part part) {
        // Create an OngoingActivityStatus using only this part and the default template.
        return new Status.Builder().addPart(DEFAULT_STATUS_PART_NAME, part).build();
    }

    /**
     * Helper to Build OngoingActivityStatus instances.
     *
     * Templates can be specified, to specify how to render the parts and any surrounding
     * text/format.
     * If no template is specified, a default template that concatenates all parts separated
     * by space is used.
     */
    public static final class Builder {
        private List<CharSequence> mTemplates = new ArrayList<>();
        private CharSequence mDefaultTemplate = "";
        private Map<String, StatusPart> mParts = new HashMap<>();

        public Builder() {
        }

        /**
         * Add a template to use for this status. Placeholders can be defined with #name#
         * To produce a '#', use '##' in the template.
         * If multiple templates are specified, the first one (in the order they where added by
         * calling this method) that has all required fields is used.
         * If no template is specified, a default template that concatenates all parts separated
         * by space is used.
         *
         * @param template the template to be added
         * @return this builder, to chain calls.
         */
        @NonNull
        public Builder addTemplate(@NonNull CharSequence template) {
            mTemplates.add(template);
            return this;
        }

        /**
         * Add a part to be inserted in the placeholders.
         *
         * @param name the name of this part. In the template, use this name surrounded by '#'
         *             to reference it, e.g. here "track" and in the template "#track#"
         * @param part The part that will be rendered in the specified position/s in the template.
         * @return this builder, to chain calls.
         */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        // We don't want a getter getParts()
        public Builder addPart(@NonNull String name, @NonNull Part part) {
            mParts.put(name, part.toVersionedParcelable());
            mDefaultTemplate += (mDefaultTemplate.length() > 0 ? " " : "") + "#" + name + "#";
            return this;
        }

        /**
         * Build an OngoingActivityStatus with the given parameters.
         *
         * @return the built OngoingActivityStatus
         */
        @NonNull
        public Status build() {
            List<CharSequence> templates = mTemplates.isEmpty() ? Arrays.asList(mDefaultTemplate)
                    : mTemplates;

            // Verify that the last template can be rendered by every SysUI.
            // Verify that all templates have all required parts.
            Map<String, CharSequence> base = new HashMap<>();
            Map<String, CharSequence> all = new HashMap<>();
            for (Map.Entry<String, StatusPart> me : mParts.entrySet()) {
                if (me.getValue() instanceof TextStatusPart
                        || me.getValue() instanceof TimerStatusPart) {
                    base.put(me.getKey(), "");
                }
                all.put(me.getKey(), "");
            }
            if (processTemplate(templates.get(templates.size() - 1), base) == null) {
                throw new IllegalStateException("For backwards compatibility reasons the last "
                        + "templateThe should only use TextStatusPart & TimerStatusPart");
            }
            for (CharSequence template : templates) {
                if (processTemplate(template, all) == null) {
                    throw new IllegalStateException("The template \"" + template + "\" is missing"
                            + " some parts for rendering.");
                }
            }

            return new Status(templates, mParts);
        }
    }

    /**
     * @return the list of templates that this status has.
     */
    @NonNull
    public List<CharSequence> getTemplates() {
        return mTemplates;
    }

    /**
     * @return the names of the parts provide to this status.
     */
    @NonNull
    public Set<String> getPartNames() {
        return Collections.unmodifiableSet(mParts.keySet());
    }

    /**
     * Returns the value of the part with the given name.
     *
     * @param name the name to lookup.
     * @return the part with the given name, can be null.
     */
    @Nullable
    public Part getPart(@NonNull String name) {
        return Part.fromVersionedParcelable(mParts.get(name));
    }

    /**
     * Process a template and replace placeholders with the provided values.
     * Placeholders are named, delimited by '#'. For example: '#name#'
     * To produce a '#' in the output, use '##' in the template.
     *
     * @param template The template to use as base.
     * @param values   The values to replace the placeholders in the template with.
     * @return The template with the placeholders replaced, or null if the template references a
     * value that it's not present (or null).
     */
    @Nullable
    static CharSequence processTemplate(@NonNull CharSequence template,
            @NonNull Map<String, CharSequence> values) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(template);

        int opening = -1;
        for (int i = 0; i < ssb.length(); i++) {
            if (ssb.charAt(i) == '#') {
                if (opening >= 0) {
                    // Replace '##' with '#'
                    // Replace '#varName#' with the value from the map.
                    CharSequence replaceWith =
                            opening == i - 1 ? "#" :
                                    values.get(ssb.subSequence(opening + 1, i).toString());
                    if (replaceWith == null) {
                        return null;
                    }
                    ssb.replace(opening, i + 1, replaceWith);
                    i = opening + replaceWith.length() - 1;
                    opening = -1;
                } else {
                    opening = i;
                }
            }
        }

        return ssb;
    }

    /**
     * Returns a textual representation of this status at the given time. The first template that
     * has all required information will be used, and each part will be used in their respective
     * placeholder/s.
     *
     * @param context       may be used for internationalization. Only used while this method
     *                      executed.
     * @param timeNowMillis the timestamp of the time we want to display, usually now, as
     * @return the rendered text, for best compatibility, display using a TextView.
     */
    @NonNull
    @Override
    public CharSequence getText(@NonNull Context context, long timeNowMillis) {
        Map<String, CharSequence> texts = new HashMap<>();
        for (Map.Entry<String, StatusPart> me : mParts.entrySet()) {
            CharSequence text = me.getValue().getText(context, timeNowMillis);
            texts.put(me.getKey(), text);
        }

        for (CharSequence template : mTemplates) {
            CharSequence ret = processTemplate(template, texts);
            if (ret != null) {
                return ret;
            }
        }

        return "";
    }

    /**
     * Returns the next time this status could have a different rendering.
     * There is no guarantee that the rendering will change at the returned time (for example, if
     * some information in the status is not rendered).
     *
     * @param fromTimeMillis current time, usually now as returned by
     *                       {@link android.os.SystemClock#elapsedRealtime()}. In most cases
     *                       {@code getText} and {@code getNextChangeTimeMillis} should be called
     *                       with the exact same timestamp, so changes are not missed.
     * @return the next time (counting from fromTimeMillis) that this status may produce a
     * different result when calling getText().
     */
    @Override
    public long getNextChangeTimeMillis(long fromTimeMillis) {
        long ret = Long.MAX_VALUE;
        for (StatusPart part : mParts.values()) {
            ret = Math.min(ret, part.getNextChangeTimeMillis(fromTimeMillis));
        }
        return ret;
    }
}

