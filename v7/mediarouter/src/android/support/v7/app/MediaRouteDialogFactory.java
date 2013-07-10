/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v7.app;

/**
 * The media route dialog factory is responsible for creating the media route
 * chooser and controller dialogs as needed.
 * <p>
 * The application can customize the dialogs by providing a subclass of the
 * dialog factory to the {@link MediaRouteButton} using the
 * {@link MediaRouteButton#setDialogFactory setDialogFactory} method.
 * </p>
 */
public class MediaRouteDialogFactory {
    private static final MediaRouteDialogFactory sDefault = new MediaRouteDialogFactory();

    /**
     * Creates a default media route dialog factory.
     */
    public MediaRouteDialogFactory() {
    }

    /**
     * Gets the default factory instance.
     *
     * @return The default media route dialog factory, never null.
     */
    public static MediaRouteDialogFactory getDefault() {
        return sDefault;
    }

    /**
     * Called when the chooser dialog is being opened and it is time to create the fragment.
     * <p>
     * Subclasses may override this method to create a customized fragment.
     * </p>
     *
     * @return The media route chooser dialog fragment, must not be null.
     */
    public MediaRouteChooserDialogFragment onCreateChooserDialogFragment() {
        return new MediaRouteChooserDialogFragment();
    }

    /**
     * Called when the controller dialog is being opened and it is time to create the fragment.
     * <p>
     * Subclasses may override this method to create a customized fragment.
     * </p>
     *
     * @return The media route controller dialog fragment, must not be null.
     */
    public MediaRouteControllerDialogFragment onCreateControllerDialogFragment() {
        return new MediaRouteControllerDialogFragment();
    }
}
