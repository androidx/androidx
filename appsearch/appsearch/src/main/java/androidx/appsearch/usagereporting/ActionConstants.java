/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.appsearch.usagereporting;

import androidx.annotation.RestrictTo;

/**
 * Wrapper class for action constants.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ActionConstants {
    /**
     * Unknown action type.
     *
     * <p>It is defined for abstract action class and compatibility, so it should not be used in any
     * concrete instances.
     */
    public static final int ACTION_TYPE_UNKNOWN = 0;

    /**
     * Search action type.
     *
     * <!--@exportToFramework:ifJetpack()-->
     * <p>It is the action type for {@link SearchAction}.
     * <!--@exportToFramework:else()-->
     */
    public static final int ACTION_TYPE_SEARCH = 1;

    /**
     * Click action type.
     *
     * <!--@exportToFramework:ifJetpack()-->
     * <p>It is the action type for {@link ClickAction}.
     * <!--@exportToFramework:else()-->
     */
    public static final int ACTION_TYPE_CLICK = 2;

    /**
     * Impression action type.
     *
     * <!--@exportToFramework:ifJetpack()-->
     * <p>It is the action type for {@link ImpressionAction}.
     * <!--@exportToFramework:else()-->
     */
    public static final int ACTION_TYPE_IMPRESSION = 3;

    private ActionConstants() {}
}
