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

package androidx.car.app.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Any class annotated with this marker is part of the protocol layer for remote host rendering and
 * can be sent between the client and the host through serialization.
 * Changes to these classes must take forward and backward compatibility into account.
 *
 * <p>Newer apps should be able to work with older hosts, if the functionality they use can be
 * emulated using older APIs or if they don't use newer features. The {@link RequiresCarApi}
 * annotation details on required versioning for compatibility for classes and methods.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PARAMETER})
public @interface CarProtocol {
}
