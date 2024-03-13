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
package androidx.browser.customtabs;

import androidx.annotation.RequiresOptIn;

/**
 * Denotes that the annotated method uses the experimental {@link CustomTabsSession#PendingSession}
 * class.
 * <p> The PendingSession is a class to be used instead of {@link CustomTabsSession} when a Custom
 * Tab is launched before a service connection is established. Users may create a new pending
 * session, and later convert it to a standard session using
 * {@link CustomTabsClient#attachSession()} which associates the pending session with the service
 * and turn it into a {@link CustomTabsSession}.
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
public @interface ExperimentalPendingSession {}
