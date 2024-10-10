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

package androidx.car.app;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.car.app.utils.LogTags.TAG;

import static java.util.Objects.requireNonNull;

import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.car.app.model.Template;
import androidx.car.app.model.TemplateInfo;
import androidx.car.app.model.TemplateWrapper;
import androidx.car.app.utils.ThreadUtils;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A Screen has a {@link Lifecycle} and provides the mechanism for the app to send {@link Template}s
 * to display when the Screen is visible. Screen instances can also be pushed and popped to and from
 * a Screen stack, which ensures they adhere to the template flow restrictions (see {@link
 * #onGetTemplate} for more details on template flow).
 *
 * <p>The Screen class can be used to manage individual units of business logic within a car app. A
 * Screen is closely tied to the {@link CarAppService} it is a part of, and cannot be used without
 * it. Though Screen defines its own lifecycle (see {@link #getLifecycle}), that lifecycle is
 * dependent on its {@link CarAppService}: if the car app service is stopped, no screens inside of
 * it can be started; when the car app service is destroyed, all screens will be destroyed.
 *
 * <p>Screen objects are not thread safe and all calls should be made from the same thread.
 */
// This lint warning is triggered because this has a finish() API. Suppress because we are not
// actually cleaning any held resources in that method.
@SuppressWarnings("NotCloseable")
public abstract class Screen implements LifecycleOwner {
    private final CarContext mCarContext;

    @SuppressWarnings({"assignment.type.incompatible", "argument.type.incompatible"})
    private final LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);

    private OnScreenResultListener mOnScreenResultListener = (obj) -> {
    };

    private @Nullable Object mResult;

    private @Nullable String mMarker;

    /**
     * A reference to the last template returned by this screen, or {@code null} if one has not been
     * returned yet.
     */
    private @Nullable TemplateWrapper mTemplateWrapper;

    /**
     * Whether to set the ID of the last template in the next template to be returned.
     *
     * @see #onGetTemplate
     */
    private boolean mUseLastTemplateId;

    protected Screen(@NonNull CarContext carContext) {
        mCarContext = requireNonNull(carContext);
    }

    /**
     * Requests the current template to be invalidated, which eventually triggers a call to {@link
     * #onGetTemplate} to get the new template to display.
     *
     * <p>If the current {@link State} of this screen is not at least {@link State#STARTED}, then a
     * call to this method will have no effect.
     *
     * <p>After the call to invalidate is made, subsequent calls have no effect until the new
     * template is returned by {@link #onGetTemplate}.
     *
     * <p>To avoid race conditions with calls to {@link #onGetTemplate} you should call this method
     * with the main thread.
     *
     * @throws HostException if the remote call fails
     */
    public final void invalidate() {
        if (getLifecycle().getCurrentState().isAtLeast(State.STARTED)) {
            mCarContext.getCarService(AppManager.class).invalidate();
        }
    }

    /**
     * Removes this screen from the stack, which will move its lifecycle state down to {@link
     * State#DESTROYED}.
     *
     * <p>Call when your screen is done and should be removed from the stack.
     *
     * <p>If this screen is the only one in the stack, it will not be finished.
     */
    public final void finish() {
        mCarContext.getCarService(ScreenManager.class).remove(this);
    }

    /**
     * Sets the {@code result} that will be sent to the {@link OnScreenResultListener} that was
     * given when pushing this screen onto the stack using {@link ScreenManager#pushForResult}.
     *
     * <p>Only the final {@code result} set will be sent.
     *
     * <p>The {@code result} will be propagated when this screen is being destroyed. This can be due
     * to being removed from the stack or explicitly calling {@link #finish}.
     *
     * @param result the value to send to the {@link OnScreenResultListener} that was given when
     *               pushing this screen onto the stack using {@link ScreenManager#pushForResult}
     */
    public void setResult(@Nullable Object result) {
        mResult = result;
    }

    /**
     * Returns the result set via {@link #setResult}, or {@code null} if none is set.
     *
     */
    @RestrictTo(LIBRARY_GROUP)
    public @Nullable Object getResultInternal() {
        return mResult;
    }

    /**
     * Updates the marker for this screen.
     *
     * <p>Set the {@code marker} to {@code null} to clear it.
     *
     * <p>This is used for setting a marker to where you can jump back to by calling {@link
     * ScreenManager#popTo}.
     */
    public void setMarker(@Nullable String marker) {
        mMarker = marker;
    }

    /**
     * Retrieves the {@code marker} that has been set for this screen, or {@code null} if one has
     * not been set.
     *
     * @see #setMarker
     */
    public @Nullable String getMarker() {
        return mMarker;
    }

    /**
     * Returns this screen's lifecycle.
     *
     * <p>Here are some ways you can use a Screen's {@link Lifecycle}:
     *
     * <ul>
     *   <li>Observe its {@link Lifecycle} by calling {@link Lifecycle#addObserver}. You can use the
     *       {@link androidx.lifecycle.LifecycleObserver} to take specific actions whenever the
     *       screen
     *       receives different {@link Event}s.
     *   <li>Use this Screen to observe {@link androidx.lifecycle.LiveData}s that may drive the
     *       backing data for your templates.
     * </ul>
     *
     * <p>What each {@link Event} means for a screen:
     *
     * <dl>
     *   <dt>{@link Event#ON_CREATE}
     *   <dd>The screen is in the process of being pushed to the screen stack, it is valid, but
     *       contents from it are not yet visible in the car screen. You should get a callback to
     *       {@link #onGetTemplate} at a point after this call.
     *       This is where you can make decision on whether this {@link Screen} is still
     *       relevant, and if you choose to not return a {@link Template} from this
     *       {@link Screen} call {@link #finish()}.
     *   <dt>{@link Event#ON_START}
     *   <dd>The template returned from this screen is visible in the car screen.
     *   <dt>{@link Event#ON_RESUME}
     *   <dd>The user can now interact with the template returned from this screen.
     *   <dt>{@link Event#ON_PAUSE}
     *   <dd>The user can no longer interact with this screen's template.
     *   <dt>{@link Event#ON_STOP}
     *   <dd>The template returned from this screen is no longer visible.
     *   <dt>{@link Event#ON_DESTROY}
     *   <dd>This screen is no longer valid and is removed from the screen stack.
     * </dl>
     *
     * <p>Listeners that are added in {@link Event#ON_START}, should be removed in {@link
     * Event#ON_STOP}.
     *
     * <p>Similarly, listeners that are added in {@link Event#ON_CREATE} should be removed in {@link
     * Event#ON_DESTROY}.
     *
     * @see androidx.lifecycle.LifecycleObserver
     */
    @Override
    public final @NonNull Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    /** Returns the {@link CarContext} of the {@link CarAppService}. */
    public final @NonNull CarContext getCarContext() {
        return mCarContext;
    }

    /** Returns the {@link ScreenManager} to use for pushing/removing screens. */
    public final @NonNull ScreenManager getScreenManager() {
        return mCarContext.getCarService(ScreenManager.class);
    }

    /**
     * Returns the {@link Template} to present in the car screen.
     *
     * <p>This method is invoked whenever a new template is needed, for example, the first time the
     * screen is created, or when the UI is invalidated through a call to {@link #invalidate}.
     *
     * <h4>Throttling of UI updates</h4>
     *
     * To minimize user distraction while driving, the host will throttle template updates to the
     * car screen. When the app invalidates multiple times in a short period, the host will call
     * this method for each call, but it may not update the actual car screen right away. This will
     * ensure there are not excessive UI changes in a short period of time.
     *
     * <p>For example, if the app sends the host two or more templates within a period of time
     * shorter than the throttle period, only the last template will be displayed on the car screen.
     *
     * <h4>Template Restrictions</h4>
     *
     * The host limits the number of templates to display for a given task to a maximum of 5, of
     * which the last template of the 5 must be one of the following types:
     *
     * <ul>
     *   <li>{@link androidx.car.app.navigation.model.NavigationTemplate}
     *   <li>{@link androidx.car.app.model.PaneTemplate}
     *   <li>{@link androidx.car.app.model.MessageTemplate}
     * </ul>
     *
     * <p><b>If the 5 template quota is exhausted and the app attempts to send a new template, the
     * host will display an error message to the user before closing the app.</b> Note that this
     * limit applies to the number of templates, and not the number of screen instances in the
     * stack. For example, if while in screen A an app sends 2 templates, and then pushes screen
     * B, it can now send 3 more templates. Alternatively, if each screen is structured to send a
     * single template, then the app can push 5 {@link Screen} instances onto the
     * {@link ScreenManager} stack.
     *
     * <p>There are special cases to these restrictions: template refreshes, back and reset
     * operations.
     *
     * <h5>Template Refreshes</h5>
     *
     * Certain content updates are not counted towards the template limit. In general, as long as an
     * app returns a template that is of the same type and contains the same main content as the
     * previous template, the new template will not be counted against the quota. For example,
     * updating the toggle state of a row in a {@link androidx.car.app.model.ListTemplate} does
     * not count against the quota. See the documentation of individual {@link Template} classes
     * to learn more about what types of content updates can be considered a refresh.
     *
     * <h5>Back Operations</h5>
     *
     * To enable sub-flows within a task, the host detects when an app is popping a {@link Screen}
     * from the {@link ScreenManager}'s stack, and updates the remaining quota based on the
     * number of templates that the app is going backwards by.
     *
     * <p>For example, if while in screen A, the app sends 2 templates and then pushes screen B and
     * sends 2 more templates, then the app has 1 quota remaining. If the app now pops back to
     * screen A, the host will reset the quota to 3, because the app has gone backwards by 2
     * templates.
     *
     * <p>Note that when popping back to a {@link Screen}, an app must send a {@link Template}
     * that is of the same type as the one last sent by that screen. Sending any other template
     * types would cause an error. However, as long as the type remains the same during a back
     * operation, an app can freely modify the contents of the template without affecting the quota.
     *
     * <h5>Reset Operations</h5>
     *
     * Certain {@link Template} classes have special semantics that signify the end of a task. For
     * example, the {@link androidx.car.app.navigation.model.NavigationTemplate} is a template
     * that is expected to stay on the screen and be refreshed with new turn-by-turn instructions
     * for the user's consumption. Upon reaching one of these templates, the host will reset the
     * template quota, treating that template as if it is the first step of a new task, thus
     * allowing the app to begin a new task. See the documentation of individual {@link Template}
     * classes to see which ones trigger a reset on the host.
     *
     * <p>If the host receives an {@link android.content.Intent} to start the car app from a
     * notification action or from the launcher, the quota will also be reset. This mechanism allows
     * an app to begin a new task flow from notifications, and it holds true even if an app is
     * already bound and in the foreground.
     *
     * <p>See {@link androidx.car.app.notification.CarAppExtender} for details on notifications.
     */
    public abstract @NonNull Template onGetTemplate();

    /** Sets a {@link OnScreenResultListener} for this {@link Screen}. */
    void setOnScreenResultListener(OnScreenResultListener onScreenResultListener) {
        mOnScreenResultListener = onScreenResultListener;
    }

    /**
     * Dispatches lifecycle event for {@code event} on the main thread.
     *
     */
    @RestrictTo(LIBRARY_GROUP)
    // Restrict to testing library
    public void dispatchLifecycleEvent(@NonNull Event event) {
        ThreadUtils.runOnMain(
                () -> {
                    State currentState = mLifecycleRegistry.getCurrentState();
                    // Avoid handling further events if the screen is already marked as destroyed.
                    if (!currentState.isAtLeast(State.INITIALIZED)) {
                        return;
                    }

                    if (event == Event.ON_DESTROY) {
                        mOnScreenResultListener.onScreenResult(mResult);
                    }

                    mLifecycleRegistry.handleLifecycleEvent(event);
                });
    }

    /**
     * Calls {@link #onGetTemplate} to get the next {@link Template} for the screen and returns it
     * wrapped in a {@link TemplateWrapper}.
     *
     * <p>The {@link TemplateWrapper} attaches a unique ID to the wrapped template, which is used
     * for implementing flow restrictions. The host keeps track of these IDs to detect push, pop, or
     * refresh operations and handle the different cases accordingly. For example, when more than
     * a max limit of templates are pushed, the host may return an error.
     *
     * <p>If {@link #setUseLastTemplateId} is called first, this method will produce a wrapper
     * that is stamped with the same ID as the last template returned by this screen. This is
     * used to identify back (stack pop) operations.
     */
    @NonNull TemplateWrapper getTemplateWrapper() {
        Template template = onGetTemplate();

        TemplateWrapper wrapper;
        if (mUseLastTemplateId && mTemplateWrapper != null) {
            wrapper =
                    TemplateWrapper.wrap(
                            template, getLastTemplateInfo(mTemplateWrapper).getTemplateId());
        } else {
            wrapper = TemplateWrapper.wrap(template);
        }
        mUseLastTemplateId = false;

        mTemplateWrapper = wrapper;

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Returning " + template + " from screen " + this);
        }
        return wrapper;
    }

    /**
     * Returns the information for the template that was last returned by this screen.
     *
     * <p>If no templates have been returned from this screen yet, this will call
     * {@link #onGetTemplate} to retrieve the {@link Template} and generate an info for it. This is
     * used in the case where multiple screens are added before a {@link #onGetTemplate} method is
     * dispatched to the top screen, allowing to notify the host of the current stack of template
     * ids known to the client.
     */
    @NonNull TemplateInfo getLastTemplateInfo() {
        if (mTemplateWrapper == null) {
            mTemplateWrapper = TemplateWrapper.wrap(onGetTemplate());
        }
        return new TemplateInfo(mTemplateWrapper.getTemplate().getClass(),
                mTemplateWrapper.getId());
    }

    private static @NonNull TemplateInfo getLastTemplateInfo(TemplateWrapper lastTemplateWrapper) {
        return new TemplateInfo(lastTemplateWrapper.getTemplate().getClass(),
                lastTemplateWrapper.getId());
    }

    /**
     * Denotes whether the next {@link Template} retrieved via {@link #onGetTemplate} should reuse
     * the ID of the last {@link Template}.
     *
     * <p>When this is set to {@code true}, the host will considered the next template sent to be a
     * back operation, and will attempt to find the previous template that shares the same ID and
     * reset the task step to that point in time.
     */
    void setUseLastTemplateId(boolean useLastTemplateId) {
        mUseLastTemplateId = useLastTemplateId;
    }
}
