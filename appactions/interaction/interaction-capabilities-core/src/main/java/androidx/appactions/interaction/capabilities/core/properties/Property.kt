/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core.properties

/**
 * Configure parameters for the capability such as providing possible values of some type, or
 * marking a parameter as required for execution.
 */
class Property<T>
internal constructor(
    /** Possible values which can fill the slot. */
    private val possibleValueSupplier: () -> List<T> = { emptyList() },
    /**
     * Indicates that a value for this property is required to be present for fulfillment. The
     * default value is false.
     */
    @get:JvmName("isRequiredForExecution")
    val isRequiredForExecution: Boolean = false,
    /**
     * Indicates that the value for this parameter must be one of [Property.possibleValues].
     * The default value is false.
     *
     * <p>If true, then the assistant should not trigger this capability if there is no match for
     * this parameter.
     */
    @get:JvmName("shouldMatchPossibleValues")
    val shouldMatchPossibleValues: Boolean = false,
    /**
     * If false, the {@code Capability} will be rejected by assistant if corresponding param is set
     * in Argument. Also, the value of |isRequired| and |entityMatchRequired| will be ignored by
     * assistant. Default value is true and can be set false with static method
     * [Property.unsupported].
     */
    @get:JvmName("isSupported")
    val isSupported: Boolean = true,
) {
    /**
     * The current list of possible values for this parameter, can change over time if this property
     * was constructed with a possible values supplier.
     */
    val possibleValues: List<T>
        get() = possibleValueSupplier()

    /** Creates a property using a static list of possible values. */
    @JvmOverloads
    constructor(
        possibleValues: List<T> = emptyList(),
        isRequiredForExecution: Boolean = false,
        shouldMatchPossibleValues: Boolean = false,
    ) : this({ possibleValues }, isRequiredForExecution, shouldMatchPossibleValues, true)

    /**
     * Creates a property using a supplier of possible values. This allows for inventory to be
     * dynamic, i.e. change every time the assistant reads the list of app-supported
     * capabilities. With background service execution, the supplier may be called once on every
     * request to the capability.
     */
    @JvmOverloads
    constructor(
        possibleValueSupplier: () -> List<T>,
        isRequiredForExecution: Boolean = false,
        shouldMatchPossibleValues: Boolean = false,
    ) : this(possibleValueSupplier, isRequiredForExecution, shouldMatchPossibleValues, true)

    companion object {
        /**
         * Sets the parameter as unsupported. Declares that the app does not support this particular
         * intent parameter, so it should not be sent to the app.
         */
        @JvmStatic
        fun <T> unsupported(): Property<T> {
            return Property(
                possibleValueSupplier = { emptyList() },
                isRequiredForExecution = false,
                shouldMatchPossibleValues = false,
                isSupported = false,
            )
        }
    }
}
