/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation;

/**
 * A marker interface for {@link NavDestination} subclasses that float above the view of other
 * destinations (i.e. {@link androidx.navigation.fragment.DialogFragmentNavigator.Destination}).
 *
 * <p>Destinations that implement this interface will automatically be popped off the back
 * stack when you navigate to a new destination.
 *
 * <p>{@link androidx.navigation.NavController.OnDestinationChangedListener} instances can also
 * customize their behavior based on whether the destination is a FloatingWindow.
 */
public interface FloatingWindow {
}
