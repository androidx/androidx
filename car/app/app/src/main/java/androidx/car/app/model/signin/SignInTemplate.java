/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.model.signin;

import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_BODY;
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_HEADER;
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_SIMPLE;

import static java.util.Objects.requireNonNull;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.Screen;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarText;
import androidx.car.app.model.ForegroundCarColorSpan;
import androidx.car.app.model.Template;
import androidx.car.app.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A template that can be used to create a sign-in flow.
 *
 * <h4>Template Restrictions</h4>
 *
 * This template's body is only available to the user while the car is parked and does not count
 * against the template quota.
 *
 * @see Screen#onGetTemplate()
 */
@RequiresCarApi(2)
public final class SignInTemplate implements Template {
    /**
     * One of the possible sign in methods that can be set on a {@link SignInTemplate}.
     */
    public interface SignInMethod {
    }

    @Keep
    private final boolean mIsLoading;
    @Keep
    @Nullable
    private final Action mHeaderAction;
    @Keep
    @Nullable
    private final CarText mTitle;
    @Keep
    @Nullable
    private final CarText mInstructions;
    @Keep
    @Nullable
    private final CarText mAdditionalText;
    @Keep
    @Nullable
    private final ActionStrip mActionStrip;
    @Keep
    private final List<Action> mActionList;
    @Keep
    @Nullable
    private final SignInMethod mSignInMethod;

    /**
     * Returns whether the template is loading.
     *
     * @see Builder#setLoading(boolean)
     */
    public boolean isLoading() {
        return mIsLoading;
    }

    /**
     * Returns the title of the template or {@code null} if not set.
     *
     * @see Builder#setTitle(CharSequence)
     */
    @Nullable
    public CarText getTitle() {
        return mTitle;
    }

    /**
     * Returns the {@link Action} that is set to be displayed in the header of the template or
     * {@code null} if not set.
     *
     * @see Builder#setHeaderAction(Action)
     */
    @Nullable
    public Action getHeaderAction() {
        return mHeaderAction;
    }

    /**
     * Returns a text containing instructions on how to sign in or {@code null} if not set.
     *
     * @see Builder#setInstructions(CharSequence)
     */
    @Nullable
    public CarText getInstructions() {
        return mInstructions;
    }

    /**
     * Returns any additional text that needs to be displayed in the template or {@code null} if
     * not set.
     *
     * @see Builder#setAdditionalText(CharSequence)
     */
    @Nullable
    public CarText getAdditionalText() {
        return mAdditionalText;
    }

    /**
     * Returns the {@link ActionStrip} for this template or {@code null} if not set.
     *
     * @see Builder#setActionStrip(ActionStrip)
     */
    @Nullable
    public ActionStrip getActionStrip() {
        return mActionStrip;
    }

    /**
     * Returns the list of {@link Action}s displayed alongside the {@link SignInMethod} in this
     * template.
     *
     * @see Builder#addAction(Action)
     */
    @NonNull
    public List<Action> getActions() {
        return CollectionUtils.emptyIfNull(mActionList);
    }

    /**
     * Returns the sign-in method of this template.
     *
     * @see Builder#Builder(SignInMethod)
     */
    @NonNull
    public SignInMethod getSignInMethod() {
        return requireNonNull(mSignInMethod);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof SignInTemplate)) {
            return false;
        }

        SignInTemplate that = (SignInTemplate) other;
        return mIsLoading == that.mIsLoading
                && Objects.equals(mHeaderAction, that.mHeaderAction)
                && Objects.equals(mTitle, that.mTitle)
                && Objects.equals(mInstructions, that.mInstructions)
                && Objects.equals(mAdditionalText, that.mAdditionalText)
                && Objects.equals(mActionStrip, that.mActionStrip)
                && Objects.equals(mActionList, that.mActionList)
                && Objects.equals(mSignInMethod, that.mSignInMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mIsLoading,
                mHeaderAction,
                mTitle,
                mInstructions,
                mAdditionalText,
                mActionStrip,
                mActionList,
                mSignInMethod);
    }

    @NonNull
    @Override
    public String toString() {
        return "SignInTemplate";
    }

    SignInTemplate(Builder builder) {
        mIsLoading = builder.mIsLoading;
        mHeaderAction = builder.mHeaderAction;
        mTitle = builder.mTitle;
        mInstructions = builder.mInstructions;
        mAdditionalText = builder.mAdditionalText;
        mActionStrip = builder.mActionStrip;
        mActionList = CollectionUtils.unmodifiableCopy(builder.mActionList);
        mSignInMethod = builder.mSignInMethod;
    }

    /** Constructs an empty instance, used by serialization code. */
    private SignInTemplate() {
        mIsLoading = false;
        mHeaderAction = null;
        mTitle = null;
        mInstructions = null;
        mAdditionalText = null;
        mActionStrip = null;
        mActionList = Collections.emptyList();
        mSignInMethod = null;
    }

    /** A builder of {@link SignInTemplate}. */
    @RequiresCarApi(2)
    public static final class Builder {
        boolean mIsLoading;
        final SignInMethod mSignInMethod;
        @Nullable
        Action mHeaderAction;
        @Nullable
        CarText mTitle;
        @Nullable
        CarText mInstructions;
        @Nullable
        CarText mAdditionalText;
        @Nullable
        ActionStrip mActionStrip;
        List<Action> mActionList = new ArrayList<>();

        /**
         * Sets whether the template is in a loading state.
         *
         * <p>If set to {@code true}, the UI will display a loading indicator instead of the
         * {@link SignInMethod}. The caller is expected to call
         * {@link androidx.car.app.Screen#invalidate()} once loading is complete.
         */
        @NonNull
        public SignInTemplate.Builder setLoading(boolean isLoading) {
            mIsLoading = isLoading;
            return this;
        }

        /**
         * Sets the {@link Action} that will be displayed in the header of the template.
         *
         * <p>Unless set with this method, the template will not have a header action.
         *
         * <h4>Requirements</h4>
         *
         * This template only supports either one of {@link Action#APP_ICON} and
         * {@link Action#BACK} as a header {@link Action}.
         *
         * @throws IllegalArgumentException if {@code headerAction} does not meet the template's
         *                                  requirements
         * @throws NullPointerException     if {@code headerAction} is {@code null}
         */
        @NonNull
        public Builder setHeaderAction(@NonNull Action headerAction) {
            ACTIONS_CONSTRAINTS_HEADER.validateOrThrow(
                    Collections.singletonList(requireNonNull(headerAction)));
            mHeaderAction = headerAction;
            return this;
        }

        /**
         * Sets the {@link ActionStrip} for this template.
         *
         * <p>Unless set with this method, the template will not have an action strip.
         *
         * <h4>Requirements</h4>
         *
         * This template allows up to 2 {@link Action}s in its {@link ActionStrip}. Of the 2 allowed
         * {@link Action}s, one of them can contain a title as set via
         * {@link Action.Builder#setTitle}. Otherwise, only {@link Action}s with icons are allowed.
         *
         * @throws IllegalArgumentException if {@code actionStrip} does not meet the requirements
         * @throws NullPointerException     if {@code actionStrip} is {@code null}
         */
        @NonNull
        public Builder setActionStrip(@NonNull ActionStrip actionStrip) {
            ACTIONS_CONSTRAINTS_SIMPLE.validateOrThrow(requireNonNull(actionStrip).getActions());
            mActionStrip = actionStrip;
            return this;
        }

        /**
         * Adds an {@link Action} to display alongside the sign-in content.
         *
         * <p>The action's title color can be customized with {@link ForegroundCarColorSpan}
         * instances, any other spans will be ignored by the host.
         *
         * <h4>Requirements</h4>
         *
         * This template allows up to 2 {@link Action}s in its body, and they must use a
         * {@link androidx.car.app.model.ParkedOnlyOnClickListener}.
         *
         * <p>Each action's title color can be customized with {@link ForegroundCarColorSpan}
         * instances, any other spans will be ignored by the host.
         *
         * @throws NullPointerException  if {@code action} is {@code null}
         * @throws IllegalArgumentException if {@code action} does not meet the requirements
         */
        @NonNull
        public Builder addAction(@NonNull Action action) {
            requireNonNull(action);
            if (!requireNonNull(action.getOnClickDelegate()).isParkedOnly()) {
                throw new IllegalArgumentException("The action must use a "
                        + "ParkedOnlyOnClickListener");
            }

            mActionList.add(action);
            ACTIONS_CONSTRAINTS_BODY.validateOrThrow(mActionList);
            return this;
        }

        /**
         * Sets the title of the template.
         *
         * <p>Unless set with this method, the template will not have a title.
         *
         * <p>Spans are not supported in the input string and will be ignored.
         *
         * @throws NullPointerException if {@code title} is {@code null}
         */
        @NonNull
        public Builder setTitle(@NonNull CharSequence title) {
            mTitle = CarText.create(requireNonNull(title));
            return this;
        }

        /**
         * Sets the text to show as instructions of the template.
         *
         * <p>Unless set with this method, the template will not have instructions.
         *
         * <p>Spans are supported in the input string.
         *
         * @throws NullPointerException if {@code instructions} is {@code null}
         * @see CarText for details on text handling and span support.
         */
        // TODO(b/181569051): document supported span types.
        @NonNull
        public Builder setInstructions(@NonNull CharSequence instructions) {
            mInstructions = CarText.create(requireNonNull(instructions));
            return this;
        }

        /**
         * Sets additional text, such as disclaimers, links to terms of services, to show in the
         * template.
         *
         * <p>Unless set with this method, the template will not have additional text.
         *
         * <p>Spans are supported in the input string.
         *
         * @throws NullPointerException if {@code additionalText} is {@code null}
         * @see CarText
         */
        // TODO(b/181569051): document supported span types.
        @NonNull
        public Builder setAdditionalText(@NonNull CharSequence additionalText) {
            mAdditionalText = CarText.create(requireNonNull(additionalText));
            return this;
        }

        /**
         * Constructs the template defined by this builder.
         *
         * <h4>Requirements</h4>
         *
         * Either a header {@link Action} or the title must be set.
         *
         * @throws IllegalStateException if the template does not have either a title or header
         *                               {@link Action} set
         */
        @NonNull
        public SignInTemplate build() {
            if (CarText.isNullOrEmpty(mTitle) && mHeaderAction == null) {
                throw new IllegalStateException("Either the title or header action must be set");
            }
            return new SignInTemplate(this);
        }

        /**
         * Returns a {@link Builder} instance.
         *
         * @param signInMethod the sign-in method to use in this template
         * @throws NullPointerException if the {@code signInMethod} is {@code null}
         */
        public Builder(@NonNull SignInMethod signInMethod) {
            mSignInMethod = requireNonNull(signInMethod);
        }
    }
}
