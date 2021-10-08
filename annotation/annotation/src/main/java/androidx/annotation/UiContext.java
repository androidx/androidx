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

package androidx.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Denotes a {@link android.content.Context} that can be used to create UI, meaning that it can
 * provide a {@link android.view.Display} via {@link android.content.Context#getDisplay} and can
 * be used to obtain an instance of a UI-related service, such as
 * {@link android.view.WindowManager}, {@link android.view.LayoutInflater} or
 * {@link android.app.WallpaperManager} via
 * {@link android.content.Context#getSystemService(String)}. A {@link android.content.Context}
 * which is marked as {@link UiContext} implies that the
 * {@link android.content.Context} is also a {@link DisplayContext}.
 * <p>
 * This kind of {@link android.content.Context} is usually an {@link android.app.Activity} or an
 * instance created via {@link android.content.Context#createWindowContext(int, Bundle)}. The
 * {@link android.content.res.Configuration} for these types of Context types is correctly
 * adjusted to the visual bounds of your window so it can be used to get the correct values
 * for {link android.view.WindowMetrics} and other UI related queries.
 * </p>
 * This is a marker annotation and has no specific attributes.
 *
 * @see android.content.Context#getDisplay()
 * @see android.content.Context#getSystemService(String)
 * @see android.content.Context#getSystemService(Class)
 * @see android.content.Context#createWindowContext(int, Bundle)
 * @see DisplayContext
 */
@Retention(SOURCE)
@Target({TYPE, METHOD, PARAMETER, FIELD})
public @interface UiContext {
}
