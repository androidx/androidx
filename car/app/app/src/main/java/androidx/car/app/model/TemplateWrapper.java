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

package androidx.car.app.model;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A wrapper for mapping a {@link Template} with a unique ID used for implementing task flow
 * restrictions.
 *
 * <p>This is what is sent to the host, so that the host can determine whether the template is a new
 * template (e.g. a step counts toward the task limit), or an existing template update (e.g. a
 * refresh that does not count towards the task limit), by checking whether the ID have changed.
 */
public final class TemplateWrapper {
    @Keep
    private Template mTemplate;
    @Keep
    private String mId;
    @Keep
    private List<TemplateInfo> mTemplateInfoForScreenStack = new ArrayList<>();

    /** The current step in a task that the template is in. For internal, host-side use only. */
    private int mCurrentTaskStep;

    /**
     * Whether the template wrapper is a refresh of the current template. For internal, host-side
     * use only.
     */
    private boolean mIsRefresh;

    /**
     * Creates a {@link TemplateWrapper} instance with the given {@link Template}.
     *
     * <p>The host will treat the {@link Template} as a new task step, unless it determines through
     * its internal logic that the {@link Template} is a refresh of the existing view, in which case
     * the task step will remain the same.
     */
    @NonNull
    public static TemplateWrapper wrap(@NonNull Template template) {
        // Assign a random ID to the template. This should be unique so that the host knows the
        // template
        // is a new step. We are not using hashCode() here as we might override template's hash
        // codes in
        // the future.
        //
        // Note: There is a chance of collision here, in which case the host will reset the
        // task step to the value of a previous template that has the colliding ID. The chance of
        // this
        // happening should be negligible given we are dealing with a very small number of
        // templates in
        // the stack.
        return wrap(template, createRandomId());
    }

    /**
     * Creates a {@link TemplateWrapper} instance with the given {@link Template} and ID.
     *
     * <p>The ID is primarily used to inform the host that the given {@link Template} shares the
     * same
     * ID as a previously sent {@link Template}, even though their contents differ. In such
     * cases, the
     * host will reset the task step to where the previous {@link Template} was.
     *
     * <p>For example, the client sends Template A (task step 1), then move forwards a screen and
     * sends Template B (task step 2). Now the client pops the screen and sends Template C. By
     * assigning the ID of Template A to Template C, the client library informs the host that it
     * is a back operation and the task step should be set to 1 again.
     */
    @NonNull
    public static TemplateWrapper wrap(@NonNull Template template, @NonNull String id) {
        return new TemplateWrapper(requireNonNull(template), requireNonNull(id));
    }

    /** Returns the wrapped {@link Template}. */
    @NonNull
    public Template getTemplate() {
        return requireNonNull(mTemplate);
    }

    /** Returns the ID associated with the wrapped {@link Template}. */
    @NonNull
    public String getId() {
        return mId;
    }

    /**
     * Sets the {@link TemplateInfo} of each of the last known templates for each of the screens in
     * the stack managed by the screen manager.
     *
     * @hide
     * @see #getTemplateInfosForScreenStack
     */
    @RestrictTo(LIBRARY)
    public void setTemplateInfosForScreenStack(
            @NonNull List<TemplateInfo> templateInfoForScreenStack) {
        this.mTemplateInfoForScreenStack = templateInfoForScreenStack;
    }

    /**
     * Returns a {@link TemplateInfo} for the last returned template for each of the screens in the
     * screen stack managed by the screen manager.
     *
     * <p>The return values are in order, where position 0 is the top of the stack, and position
     * n is
     * the bottom of the stack given n screens on the stack.
     */
    @Nullable
    public List<TemplateInfo> getTemplateInfosForScreenStack() {
        return mTemplateInfoForScreenStack;
    }

    /**
     * Retrieves the current task step that the template is in. For internal, host-side use only.
     */
    public int getCurrentTaskStep() {
        return mCurrentTaskStep;
    }

    /**
     * Sets the current task step that the template is in. For internal, host-side use only.
     */
    public void setCurrentTaskStep(int currentTaskStep) {
        this.mCurrentTaskStep = currentTaskStep;
    }

    /** Sets whether the template is a refresh of the current template. */
    public void setRefresh(boolean isRefresh) {
        this.mIsRefresh = isRefresh;
    }

    /** Returns {@code true} if the template is a refresh for the previous template. */
    public boolean isRefresh() {
        return mIsRefresh;
    }

    /**
     * Updates the {@link Template} this {@link TemplateWrapper} instance wraps. For internal,
     * host-side use only.
     */
    public void setTemplate(@NonNull Template template) {
        this.mTemplate = template;
    }

    /**
     * Updates the ID associated with the wrapped {@link Template}. For internal, host-side use
     * only.
     */
    public void setId(@NonNull String id) {
        this.mId = id;
    }

    /** Creates a copy of the given {@link TemplateWrapper}. */
    @NonNull
    public static TemplateWrapper copyOf(@NonNull TemplateWrapper source) {
        TemplateWrapper destination = TemplateWrapper.wrap(source.getTemplate(), source.getId());
        destination.setRefresh(source.isRefresh());
        destination.setCurrentTaskStep(source.getCurrentTaskStep());
        List<TemplateInfo> templateInfos = source.getTemplateInfosForScreenStack();
        if (templateInfos != null) {
            destination.setTemplateInfosForScreenStack(templateInfos);
        }
        return destination;
    }

    @NonNull
    @Override
    public String toString() {
        return "[template: " + mTemplate + ", ID: " + mId + "]";
    }

    private TemplateWrapper(Template template, String id) {
        this.mTemplate = template;
        this.mId = id;
    }

    private TemplateWrapper() {
        mTemplate = null;
        mId = "";
    }

    private static String createRandomId() {
        return UUID.randomUUID().toString();
    }
}
