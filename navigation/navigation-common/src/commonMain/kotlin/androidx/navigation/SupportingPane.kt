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

package androidx.navigation

/**
 * A marker interface for [NavDestination] subclasses that sit alongside the view of other
 * destinations.
 *
 * Supporting pane destinations have the same lifecycle as the other visible destinations (e.g., a
 * non-SupportingPane destination will continue to be resumed when a supporting pane is added to the
 * back stack).
 *
 * [androidx.navigation.NavController.OnDestinationChangedListener] instances can also customize
 * their behavior based on whether the destination is a SupportingPane.
 */
public interface SupportingPane
