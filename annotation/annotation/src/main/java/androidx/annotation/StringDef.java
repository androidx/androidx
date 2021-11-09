/*
 * Copyright (C) 2014 The Android Open Source Project
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

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Denotes that the annotated String element, represents a logical
 * type and that its value should be one of the explicitly named constants.
 * <p>
 * Example:
 * <pre>
 *  &#64;Retention(SOURCE)
 *  &#64;StringDef({
 *    POWER_SERVICE,
 *    WINDOW_SERVICE,
 *    LAYOUT_INFLATER_SERVICE
 *  })
 *  public @interface ServiceName {}
 *  public static final String POWER_SERVICE = "power";
 *  public static final String WINDOW_SERVICE = "window";
 *  public static final String LAYOUT_INFLATER_SERVICE = "layout_inflater";
 *  ...
 *  public abstract Object getSystemService(@ServiceName String name);
 * </pre>
 */
@Retention(SOURCE)
@Target({ANNOTATION_TYPE})
public @interface StringDef {
    /** Defines the allowed constants for this element */
    String[] value() default {};

    /**
     * Whether any other values are allowed. Normally this is
     * not the case, but this allows you to specify a set of
     * expected constants, which helps code completion in the IDE
     * and documentation generation and so on, but without
     * flagging compilation warnings if other values are specified.
     */
    boolean open() default false;
}
