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

package android.support.wearable.complications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.BadParcelableException
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.watchface.complications.data.ComplicationDisplayPolicies
import androidx.wear.watchface.complications.data.ComplicationDisplayPolicy
import androidx.wear.watchface.complications.data.ComplicationPersistencePolicies
import androidx.wear.watchface.complications.data.ComplicationPersistencePolicy
import androidx.wear.watchface.utility.iconEquals
import androidx.wear.watchface.utility.iconHashCode
import java.io.IOException
import java.io.InvalidObjectException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.Arrays
import java.util.Objects

/**
 * Container for complication data of all types.
 *
 * A [androidx.wear.watchface.complications.ComplicationProviderService] should create instances of
 * this class using [ComplicationData.Builder] and send them to the complication system in response
 * to [androidx.wear.watchface.complications.ComplicationProviderService.onComplicationRequest].
 * Depending on the type of complication data, some fields will be required and some will be
 * optional - see the documentation for each type, and for the builder's set methods, for details.
 *
 * A watch face will receive instances of this class as long as providers are configured.
 *
 * When rendering the complication data for a given time, the watch face should first call
 * [isActiveAt] to determine whether the data is valid at that time. See the documentation for each
 * of the complication types below for details of which fields are expected to be displayed.
 *
 * @hide
 */
@SuppressLint("BanParcelableUsage")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ComplicationData : Parcelable, Serializable {
    /** @hide */
    @IntDef(
        TYPE_EMPTY,
        TYPE_NOT_CONFIGURED,
        TYPE_SHORT_TEXT,
        TYPE_LONG_TEXT,
        TYPE_RANGED_VALUE,
        TYPE_ICON,
        TYPE_SMALL_IMAGE,
        TYPE_LARGE_IMAGE,
        TYPE_NO_PERMISSION,
        TYPE_NO_DATA,
        TYPE_GOAL_PROGRESS,
        TYPE_WEIGHTED_ELEMENTS,
        EXP_TYPE_PROTO_LAYOUT,
        EXP_TYPE_LIST
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ComplicationType

    /** @hide */
    @IntDef(IMAGE_STYLE_PHOTO, IMAGE_STYLE_ICON)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ImageStyle

    /** Returns the type of this complication data. */
    @ComplicationType val type: Int

    private val fields: Bundle

    internal constructor(builder: Builder) {
        type = builder.type
        fields = builder.fields
    }

    internal constructor(type: Int, fields: Bundle) {
        this.type = type
        this.fields = fields
        this.fields.classLoader = javaClass.classLoader
    }

    internal constructor(input: Parcel) {
        type = input.readInt()
        fields =
            input.readBundle(javaClass.classLoader)
                ?: run {
                    Log.w(TAG, "ComplicationData parcel input has null bundle.")
                    Bundle()
                }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private class SerializedForm
    @JvmOverloads
    constructor(private var complicationData: ComplicationData? = null) : Serializable {

        @Throws(IOException::class)
        private fun writeObject(oos: ObjectOutputStream) {
            // Get a copy because it could technically change to null in another thread.
            val complicationData = this.complicationData!!
            oos.writeInt(VERSION_NUMBER)
            val type = complicationData.type
            oos.writeInt(type)
            oos.writeInt(complicationData.persistencePolicy)
            oos.writeInt(complicationData.displayPolicy)
            if (isFieldValidForType(FIELD_LONG_TEXT, type)) {
                oos.writeObject(complicationData.longText)
            }
            if (isFieldValidForType(FIELD_LONG_TITLE, type)) {
                oos.writeObject(complicationData.longTitle)
            }
            if (isFieldValidForType(FIELD_SHORT_TEXT, type)) {
                oos.writeObject(complicationData.shortText)
            }
            if (isFieldValidForType(FIELD_SHORT_TITLE, type)) {
                oos.writeObject(complicationData.shortTitle)
            }
            if (isFieldValidForType(FIELD_CONTENT_DESCRIPTION, type)) {
                oos.writeObject(complicationData.contentDescription)
            }
            if (isFieldValidForType(FIELD_ICON, type)) {
                oos.writeObject(IconSerializableHelper.create(complicationData.icon))
            }
            if (isFieldValidForType(FIELD_ICON_BURN_IN_PROTECTION, type)) {
                oos.writeObject(
                    IconSerializableHelper.create(complicationData.burnInProtectionIcon)
                )
            }
            if (isFieldValidForType(FIELD_SMALL_IMAGE, type)) {
                oos.writeObject(IconSerializableHelper.create(complicationData.smallImage))
            }
            if (isFieldValidForType(FIELD_SMALL_IMAGE_BURN_IN_PROTECTION, type)) {
                oos.writeObject(
                    IconSerializableHelper.create(complicationData.burnInProtectionSmallImage)
                )
            }
            if (isFieldValidForType(FIELD_IMAGE_STYLE, type)) {
                oos.writeInt(complicationData.smallImageStyle)
            }
            if (isFieldValidForType(FIELD_LARGE_IMAGE, type)) {
                oos.writeObject(IconSerializableHelper.create(complicationData.largeImage))
            }
            if (isFieldValidForType(FIELD_VALUE, type)) {
                oos.writeFloat(complicationData.rangedValue)
            }
            if (isFieldValidForType(FIELD_VALUE_EXPRESSION, type)) {
                oos.writeNullable(complicationData.rangedValueExpression) {
                    oos.writeByteArray(it.toDynamicFloatByteArray())
                }
            }
            if (isFieldValidForType(FIELD_VALUE_TYPE, type)) {
                oos.writeInt(complicationData.rangedValueType)
            }
            if (isFieldValidForType(FIELD_MIN_VALUE, type)) {
                oos.writeFloat(complicationData.rangedMinValue)
            }
            if (isFieldValidForType(FIELD_MAX_VALUE, type)) {
                oos.writeFloat(complicationData.rangedMaxValue)
            }
            if (isFieldValidForType(FIELD_TARGET_VALUE, type)) {
                oos.writeFloat(complicationData.targetValue)
            }
            if (isFieldValidForType(FIELD_COLOR_RAMP, type)) {
                oos.writeNullable(complicationData.colorRamp, oos::writeIntArray)
            }
            if (isFieldValidForType(FIELD_COLOR_RAMP_INTERPOLATED, type)) {
                oos.writeNullable(complicationData.isColorRampInterpolated, oos::writeBoolean)
            }
            if (isFieldValidForType(FIELD_ELEMENT_WEIGHTS, type)) {
                oos.writeNullable(complicationData.elementWeights, oos::writeFloatArray)
            }
            if (isFieldValidForType(FIELD_ELEMENT_COLORS, type)) {
                oos.writeNullable(complicationData.elementColors, oos::writeIntArray)
            }
            if (isFieldValidForType(FIELD_ELEMENT_BACKGROUND_COLOR, type)) {
                oos.writeInt(complicationData.elementBackgroundColor)
            }
            if (isFieldValidForType(FIELD_START_TIME, type)) {
                oos.writeLong(complicationData.startDateTimeMillis)
            }
            if (isFieldValidForType(FIELD_END_TIME, type)) {
                oos.writeLong(complicationData.endDateTimeMillis)
            }
            oos.writeInt(complicationData.fields.getInt(EXP_FIELD_LIST_ENTRY_TYPE))
            if (isFieldValidForType(EXP_FIELD_LIST_STYLE_HINT, type)) {
                oos.writeInt(complicationData.listStyleHint)
            }
            if (isFieldValidForType(EXP_FIELD_PROTO_LAYOUT_INTERACTIVE, type)) {
                oos.writeByteArray(complicationData.interactiveLayout ?: byteArrayOf())
            }
            if (isFieldValidForType(EXP_FIELD_PROTO_LAYOUT_AMBIENT, type)) {
                oos.writeByteArray(complicationData.ambientLayout ?: byteArrayOf())
            }
            if (isFieldValidForType(EXP_FIELD_PROTO_LAYOUT_RESOURCES, type)) {
                oos.writeByteArray(complicationData.layoutResources ?: byteArrayOf())
            }
            if (isFieldValidForType(FIELD_DATA_SOURCE, type)) {
                val componentName = complicationData.dataSource
                if (componentName == null) {
                    oos.writeUTF("")
                } else {
                    oos.writeUTF(componentName.flattenToString())
                }
            }

            // TapAction unfortunately can't be serialized, instead we record if we've lost it.
            oos.writeBoolean(
                complicationData.hasTapAction() || complicationData.tapActionLostDueToSerialization
            )
            val start = complicationData.fields.getLong(FIELD_TIMELINE_START_TIME, -1)
            oos.writeLong(start)
            val end = complicationData.fields.getLong(FIELD_TIMELINE_END_TIME, -1)
            oos.writeLong(end)
            oos.writeInt(complicationData.fields.getInt(FIELD_TIMELINE_ENTRY_TYPE))
            oos.writeList(complicationData.listEntries ?: listOf()) {
                SerializedForm(it).writeObject(oos)
            }
            if (isFieldValidForType(FIELD_PLACEHOLDER_FIELDS, type)) {
                oos.writeNullable(complicationData.placeholder) {
                    SerializedForm(it).writeObject(oos)
                }
            }

            // This has to be last, since it's recursive.
            oos.writeList(complicationData.timelineEntries ?: listOf()) {
                SerializedForm(it).writeObject(oos)
            }
        }

        @Throws(IOException::class, ClassNotFoundException::class)
        private fun readObject(ois: ObjectInputStream) {
            val versionNumber = ois.readInt()
            if (versionNumber != VERSION_NUMBER) {
                // Give up if there's a version skew.
                throw IOException("Unsupported serialization version number $versionNumber")
            }
            val type = ois.readInt()
            val fields = Bundle()
            fields.putInt(FIELD_PERSISTENCE_POLICY, ois.readInt())
            fields.putInt(FIELD_DISPLAY_POLICY, ois.readInt())
            if (isFieldValidForType(FIELD_LONG_TEXT, type)) {
                putIfNotNull(fields, FIELD_LONG_TEXT, ois.readObject() as ComplicationText?)
            }
            if (isFieldValidForType(FIELD_LONG_TITLE, type)) {
                putIfNotNull(fields, FIELD_LONG_TITLE, ois.readObject() as ComplicationText?)
            }
            if (isFieldValidForType(FIELD_SHORT_TEXT, type)) {
                putIfNotNull(fields, FIELD_SHORT_TEXT, ois.readObject() as ComplicationText?)
            }
            if (isFieldValidForType(FIELD_SHORT_TITLE, type)) {
                putIfNotNull(fields, FIELD_SHORT_TITLE, ois.readObject() as ComplicationText?)
            }
            if (isFieldValidForType(FIELD_CONTENT_DESCRIPTION, type)) {
                putIfNotNull(
                    fields,
                    FIELD_CONTENT_DESCRIPTION,
                    ois.readObject() as ComplicationText?
                )
            }
            if (isFieldValidForType(FIELD_ICON, type)) {
                putIfNotNull(fields, FIELD_ICON, IconSerializableHelper.read(ois))
            }
            if (isFieldValidForType(FIELD_ICON_BURN_IN_PROTECTION, type)) {
                putIfNotNull(
                    fields,
                    FIELD_ICON_BURN_IN_PROTECTION,
                    IconSerializableHelper.read(ois)
                )
            }
            if (isFieldValidForType(FIELD_SMALL_IMAGE, type)) {
                putIfNotNull(fields, FIELD_SMALL_IMAGE, IconSerializableHelper.read(ois))
            }
            if (isFieldValidForType(FIELD_SMALL_IMAGE_BURN_IN_PROTECTION, type)) {
                putIfNotNull(
                    fields,
                    FIELD_SMALL_IMAGE_BURN_IN_PROTECTION,
                    IconSerializableHelper.read(ois)
                )
            }
            if (isFieldValidForType(FIELD_IMAGE_STYLE, type)) {
                fields.putInt(FIELD_IMAGE_STYLE, ois.readInt())
            }
            if (isFieldValidForType(FIELD_LARGE_IMAGE, type)) {
                fields.putParcelable(FIELD_LARGE_IMAGE, IconSerializableHelper.read(ois))
            }
            if (isFieldValidForType(FIELD_VALUE, type)) {
                fields.putFloat(FIELD_VALUE, ois.readFloat())
            }
            if (isFieldValidForType(FIELD_VALUE_EXPRESSION, type)) {
                ois.readNullable { ois.readByteArray() }
                    ?.let { fields.putByteArray(FIELD_VALUE_EXPRESSION, it) }
            }
            if (isFieldValidForType(FIELD_VALUE_TYPE, type)) {
                fields.putInt(FIELD_VALUE_TYPE, ois.readInt())
            }
            if (isFieldValidForType(FIELD_MIN_VALUE, type)) {
                fields.putFloat(FIELD_MIN_VALUE, ois.readFloat())
            }
            if (isFieldValidForType(FIELD_MAX_VALUE, type)) {
                fields.putFloat(FIELD_MAX_VALUE, ois.readFloat())
            }
            if (isFieldValidForType(FIELD_TARGET_VALUE, type)) {
                fields.putFloat(FIELD_TARGET_VALUE, ois.readFloat())
            }
            if (isFieldValidForType(FIELD_COLOR_RAMP, type)) {
                ois.readNullable { ois.readIntArray() }
                    ?.let { fields.putIntArray(FIELD_COLOR_RAMP, it) }
            }
            if (isFieldValidForType(FIELD_COLOR_RAMP_INTERPOLATED, type)) {
                ois.readNullable { ois.readBoolean() }
                    ?.let { fields.putBoolean(FIELD_COLOR_RAMP_INTERPOLATED, it) }
            }
            if (isFieldValidForType(FIELD_ELEMENT_WEIGHTS, type)) {
                ois.readNullable { ois.readFloatArray() }
                    ?.let { fields.putFloatArray(FIELD_ELEMENT_WEIGHTS, it) }
            }
            if (isFieldValidForType(FIELD_ELEMENT_COLORS, type)) {
                ois.readNullable { ois.readIntArray() }
                    ?.let { fields.putIntArray(FIELD_ELEMENT_COLORS, it) }
            }
            if (isFieldValidForType(FIELD_ELEMENT_BACKGROUND_COLOR, type)) {
                fields.putInt(FIELD_ELEMENT_BACKGROUND_COLOR, ois.readInt())
            }
            if (isFieldValidForType(FIELD_START_TIME, type)) {
                fields.putLong(FIELD_START_TIME, ois.readLong())
            }
            if (isFieldValidForType(FIELD_END_TIME, type)) {
                fields.putLong(FIELD_END_TIME, ois.readLong())
            }
            val listEntryType = ois.readInt()
            if (listEntryType != 0) {
                fields.putInt(EXP_FIELD_LIST_ENTRY_TYPE, listEntryType)
            }
            if (isFieldValidForType(EXP_FIELD_LIST_STYLE_HINT, type)) {
                fields.putInt(EXP_FIELD_LIST_STYLE_HINT, ois.readInt())
            }
            if (isFieldValidForType(EXP_FIELD_PROTO_LAYOUT_INTERACTIVE, type)) {
                val length = ois.readInt()
                if (length > 0) {
                    val protoLayout = ByteArray(length)
                    ois.readFully(protoLayout)
                    fields.putByteArray(EXP_FIELD_PROTO_LAYOUT_INTERACTIVE, protoLayout)
                }
            }
            if (isFieldValidForType(EXP_FIELD_PROTO_LAYOUT_AMBIENT, type)) {
                val length = ois.readInt()
                if (length > 0) {
                    val ambientProtoLayout = ByteArray(length)
                    ois.readFully(ambientProtoLayout)
                    fields.putByteArray(EXP_FIELD_PROTO_LAYOUT_AMBIENT, ambientProtoLayout)
                }
            }
            if (isFieldValidForType(EXP_FIELD_PROTO_LAYOUT_RESOURCES, type)) {
                val length = ois.readInt()
                if (length > 0) {
                    val protoLayoutResources = ByteArray(length)
                    ois.readFully(protoLayoutResources)
                    fields.putByteArray(EXP_FIELD_PROTO_LAYOUT_RESOURCES, protoLayoutResources)
                }
            }
            if (isFieldValidForType(FIELD_DATA_SOURCE, type)) {
                val componentName = ois.readUTF()
                if (componentName.isEmpty()) {
                    fields.remove(FIELD_DATA_SOURCE)
                } else {
                    fields.putParcelable(
                        FIELD_DATA_SOURCE,
                        ComponentName.unflattenFromString(componentName)
                    )
                }
            }
            if (ois.readBoolean()) {
                fields.putBoolean(FIELD_TAP_ACTION_LOST, true)
            }
            val start = ois.readLong()
            if (start != -1L) {
                fields.putLong(FIELD_TIMELINE_START_TIME, start)
            }
            val end = ois.readLong()
            if (end != -1L) {
                fields.putLong(FIELD_TIMELINE_END_TIME, end)
            }
            val timelineEntryType = ois.readInt()
            if (timelineEntryType != 0) {
                fields.putInt(FIELD_TIMELINE_ENTRY_TYPE, timelineEntryType)
            }
            ois.readList { SerializedForm().apply { readObject(ois) } }
                .map { it.complicationData!!.fields }
                .takeIf { it.isNotEmpty() }
                ?.let { fields.putParcelableArray(EXP_FIELD_LIST_ENTRIES, it.toTypedArray()) }
            if (isFieldValidForType(FIELD_PLACEHOLDER_FIELDS, type)) {
                ois.readNullable { SerializedForm().apply { readObject(ois) } }
                    ?.let {
                        fields.putInt(FIELD_PLACEHOLDER_TYPE, it.complicationData!!.type)
                        fields.putBundle(FIELD_PLACEHOLDER_FIELDS, it.complicationData!!.fields)
                    }
            }
            ois.readList { SerializedForm().apply { readObject(ois) } }
                .map { it.complicationData!!.fields }
                .takeIf { it.isNotEmpty() }
                ?.let { fields.putParcelableArray(FIELD_TIMELINE_ENTRIES, it.toTypedArray()) }
            complicationData = ComplicationData(type, fields)
        }

        fun readResolve(): Any = complicationData!!

        companion object {
            private const val VERSION_NUMBER = 20
            internal fun putIfNotNull(fields: Bundle, field: String, value: Parcelable?) {
                if (value != null) {
                    fields.putParcelable(field, value)
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P) fun writeReplace(): Any = SerializedForm(this)

    @Throws(InvalidObjectException::class)
    private fun readObject(@Suppress("UNUSED_PARAMETER") stream: ObjectInputStream) {
        throw InvalidObjectException("Use SerializedForm")
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(type)
        dest.writeBundle(fields)
    }

    /**
     * Returns true if the complication is active and should be displayed at the given time. If this
     * returns false, the complication should not be displayed.
     *
     * This must be checked for any time for which the complication will be displayed.
     */
    fun isActiveAt(dateTimeMillis: Long) =
        (dateTimeMillis >= fields.getLong(FIELD_START_TIME, 0) &&
            dateTimeMillis <= fields.getLong(FIELD_END_TIME, Long.MAX_VALUE))

    /**
     * TapAction unfortunately can't be serialized. Returns true if tapAction has been lost due to
     * serialization (e.g. due to being read from the local cache). The next complication update
     * from the system would replace this with one with a tapAction.
     */
    val tapActionLostDueToSerialization: Boolean
        get() = fields.getBoolean(FIELD_TAP_ACTION_LOST)

    /**
     * For timeline entries. The epoch second at which this timeline entry becomes * valid or `null`
     * if it's not set.
     */
    var timelineStartEpochSecond: Long?
        get() {
            val expiresAt = fields.getLong(FIELD_TIMELINE_START_TIME, -1)
            return if (expiresAt == -1L) {
                null
            } else {
                expiresAt
            }
        }
        set(epochSecond) {
            if (epochSecond == null) {
                fields.remove(FIELD_TIMELINE_START_TIME)
            } else {
                fields.putLong(FIELD_TIMELINE_START_TIME, epochSecond)
            }
        }

    /**
     * For timeline entries. The epoch second at which this timeline entry becomes invalid or `null`
     * if it's not set.
     */
    var timelineEndEpochSecond: Long?
        get() {
            val expiresAt = fields.getLong(FIELD_TIMELINE_END_TIME, -1)
            return if (expiresAt == -1L) {
                null
            } else {
                expiresAt
            }
        }
        set(epochSecond) {
            if (epochSecond == null) {
                fields.remove(FIELD_TIMELINE_END_TIME)
            } else {
                fields.putLong(FIELD_TIMELINE_END_TIME, epochSecond)
            }
        }

    /** The list of [ComplicationData] timeline entries. */
    val timelineEntries: List<ComplicationData>?
        @Suppress("DEPRECATION")
        get() =
            fields.getParcelableArray(FIELD_TIMELINE_ENTRIES)?.map { parcelable ->
                val bundle = parcelable as Bundle
                bundle.classLoader = javaClass.classLoader
                // Use the serialized FIELD_TIMELINE_ENTRY_TYPE or the outer type if it's not there.
                // Usually the timeline entry type will be the same as the outer type, unless an
                // entry contains NoDataComplicationData.
                val type = bundle.getInt(FIELD_TIMELINE_ENTRY_TYPE, type)
                ComplicationData(type, parcelable)
            }

    /** Sets the list of [ComplicationData] timeline entries. */
    fun setTimelineEntryCollection(timelineEntries: Collection<ComplicationData>?) {
        if (timelineEntries == null) {
            fields.remove(FIELD_TIMELINE_ENTRIES)
        } else {
            fields.putParcelableArray(
                FIELD_TIMELINE_ENTRIES,
                timelineEntries
                    .map {
                        // This supports timeline entry of NoDataComplicationData.
                        it.fields.putInt(FIELD_TIMELINE_ENTRY_TYPE, it.type)
                        it.fields
                    }
                    .toTypedArray()
            )
        }
    }

    /** The list of [ComplicationData] entries for a ListComplicationData. */
    val listEntries: List<ComplicationData>?
        @Suppress("deprecation")
        get() =
            fields.getParcelableArray(EXP_FIELD_LIST_ENTRIES)?.map { parcelable ->
                val bundle = parcelable as Bundle
                bundle.classLoader = javaClass.classLoader
                ComplicationData(bundle.getInt(EXP_FIELD_LIST_ENTRY_TYPE), bundle)
            }

    /**
     * The [ComponentName] of the ComplicationDataSourceService that provided this ComplicationData.
     */
    var dataSource: ComponentName?
        // The safer alternative is not available on Wear OS yet.
        get() = getParcelableField(FIELD_DATA_SOURCE)
        set(provider) {
            fields.putParcelable(FIELD_DATA_SOURCE, provider)
        }

    /**
     * Returns true if the ComplicationData contains a ranged value. I.e. if [rangedValue] can
     * succeed.
     */
    fun hasRangedValue(): Boolean = isFieldValidForType(FIELD_VALUE, type)

    /**
     * Returns the *value* field for this complication.
     *
     * Valid only if the type of this complication data is [TYPE_RANGED_VALUE] and
     * [TYPE_GOAL_PROGRESS], otherwise returns zero.
     */
    val rangedValue: Float
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_VALUE, type)
            return fields.getFloat(FIELD_VALUE)
        }

    /**
     * Returns true if the ComplicationData contains a ranged value expression. I.e. if
     * [rangedValueExpression] can succeed.
     */
    fun hasRangedValueExpression(): Boolean =
        isFieldValidForType(FIELD_VALUE_EXPRESSION, type) &&
            fields.containsKey(FIELD_VALUE_EXPRESSION)

    /**
     * Returns the *valueExpression* field for this complication.
     *
     * Valid only if the type of this complication data is [TYPE_RANGED_VALUE] and
     * [TYPE_GOAL_PROGRESS].
     */
    val rangedValueExpression: DynamicFloat?
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_VALUE_EXPRESSION, type)
            return fields.getByteArray(FIELD_VALUE_EXPRESSION)?.let {
                DynamicFloat.fromByteArray(it)
            }
        }

    /**
     * Returns true if the ComplicationData contains a ranged max type. I.e. if [rangedValueType]
     * can succeed.
     */
    fun hasRangedValueType(): Boolean = isFieldValidForType(FIELD_VALUE_TYPE, type)

    /**
     * Returns the *value* field for this complication.
     *
     * Valid only if the type of this complication data is [TYPE_RANGED_VALUE], otherwise returns
     * zero.
     */
    val rangedValueType: Int
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_VALUE_TYPE, type)
            return fields.getInt(FIELD_VALUE_TYPE)
        }

    /**
     * Returns true if the ComplicationData contains a ranged max value. I.e. if [rangedMinValue]
     * can succeed.
     */
    fun hasRangedMinValue(): Boolean = isFieldValidForType(FIELD_MIN_VALUE, type)

    /**
     * Returns the *min value* field for this complication.
     *
     * Valid only if the type of this complication data is [TYPE_RANGED_VALUE], otherwise returns
     * zero.
     */
    val rangedMinValue: Float
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_MIN_VALUE, type)
            return fields.getFloat(FIELD_MIN_VALUE)
        }

    /**
     * Returns true if the ComplicationData contains a ranged max value. I.e. if [rangedMaxValue]
     * can succeed.
     */
    fun hasRangedMaxValue(): Boolean = isFieldValidForType(FIELD_MAX_VALUE, type)

    /**
     * Returns the *max value* field for this complication.
     *
     * Valid only if the type of this complication data is [TYPE_RANGED_VALUE], otherwise returns
     * zero.
     */
    val rangedMaxValue: Float
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_MAX_VALUE, type)
            return fields.getFloat(FIELD_MAX_VALUE)
        }

    /**
     * Returns true if the ComplicationData contains a ranged max value. I.e. if [targetValue] can
     * succeed.
     */
    fun hasTargetValue(): Boolean = isFieldValidForType(FIELD_TARGET_VALUE, type)

    /**
     * Returns the *value* field for this complication.
     *
     * Valid only if the type of this complication data is [TYPE_GOAL_PROGRESS], otherwise returns
     * zero.
     */
    val targetValue: Float
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_TARGET_VALUE, type)
            return fields.getFloat(FIELD_TARGET_VALUE)
        }

    /**
     * Returns the colors for the progress bar.
     *
     * Valid only if the type of this complication data is [TYPE_RANGED_VALUE] or
     * [TYPE_GOAL_PROGRESS].
     */
    @get:ColorInt
    val colorRamp: IntArray?
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_COLOR_RAMP, type)
            return if (fields.containsKey(FIELD_COLOR_RAMP)) {
                fields.getIntArray(FIELD_COLOR_RAMP)
            } else {
                null
            }
        }

    /**
     * Returns either a boolean where: true means the color ramp colors should be smoothly
     * interpolated; false means the color ramp should be rendered in equal sized blocks of solid
     * color; null means this value wasn't set, i.e. the complication is not of type
     * [TYPE_RANGED_VALUE] or [TYPE_GOAL_PROGRESS].
     *
     * Valid only if the type of this complication data is [TYPE_RANGED_VALUE] or
     * [TYPE_GOAL_PROGRESS].
     */
    val isColorRampInterpolated: Boolean?
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_COLOR_RAMP_INTERPOLATED, type)
            return if (fields.containsKey(FIELD_COLOR_RAMP_INTERPOLATED)) {
                fields.getBoolean(FIELD_COLOR_RAMP_INTERPOLATED)
            } else {
                null
            }
        }

    /**
     * Returns true if the ComplicationData contains a short title. I.e. if [shortTitle] can
     * succeed.
     */
    fun hasShortTitle(): Boolean =
        isFieldValidForType(FIELD_SHORT_TITLE, type) && hasParcelableField(FIELD_SHORT_TITLE)

    /**
     * Returns the *short title* field for this complication, or `null` if no value was provided for
     * the field.
     *
     * The value is provided as a [ComplicationText] object, from which the text to display can be
     * obtained for a given point in time.
     *
     * The length of the text, including any time-dependent values at any valid time, is expected to
     * not exceed seven characters. When using this text, the watch face should be able to display
     * any string of up to seven characters (reducing the text size appropriately if the string is
     * very wide). Although not expected, it is possible that strings of more than seven characters
     * might be seen, in which case they may be truncated.
     *
     * Valid only if the type of this complication data is [TYPE_SHORT_TEXT], [TYPE_RANGED_VALUE],
     * or [TYPE_NO_PERMISSION], otherwise returns null.
     */
    val shortTitle: ComplicationText?
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_SHORT_TITLE, type)
            return getParcelableFieldOrWarn<ComplicationText>(FIELD_SHORT_TITLE)
        }

    /**
     * Returns true if the ComplicationData contains short text. I.e. if [shortText] can succeed.
     */
    fun hasShortText(): Boolean =
        isFieldValidForType(FIELD_SHORT_TEXT, type) && hasParcelableField(FIELD_SHORT_TEXT)

    /**
     * Returns the *short text* field for this complication, or `null` if no value was provided for
     * the field.
     *
     * The value is provided as a [ComplicationText] object, from which the text to display can be
     * obtained for a given point in time.
     *
     * The length of the text, including any time-dependent values at any valid time, is expected to
     * not exceed seven characters. When using this text, the watch face should be able to display
     * any string of up to seven characters (reducing the text size appropriately if the string is
     * very wide). Although not expected, it is possible that strings of more than seven characters
     * might be seen, in which case they may be truncated.
     *
     * Valid only if the type of this complication data is [TYPE_SHORT_TEXT], [TYPE_RANGED_VALUE],
     * or [TYPE_NO_PERMISSION], otherwise returns null.
     */
    val shortText: ComplicationText?
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_SHORT_TEXT, type)
            return getParcelableFieldOrWarn<ComplicationText>(FIELD_SHORT_TEXT)
        }

    /**
     * Returns true if the ComplicationData contains a long title. I.e. if [longTitle] can succeed.
     */
    fun hasLongTitle(): Boolean =
        isFieldValidForType(FIELD_LONG_TITLE, type) && hasParcelableField(FIELD_LONG_TITLE)

    /**
     * Returns the *long title* field for this complication, or `null` if no value was provided for
     * the field.
     *
     * The value is provided as a [ComplicationText] object, from which the text to display can be
     * obtained for a given point in time.
     *
     * Valid only if the type of this complication data is [TYPE_LONG_TEXT], otherwise returns null.
     */
    val longTitle: ComplicationText?
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_LONG_TITLE, type)
            return getParcelableFieldOrWarn<ComplicationText>(FIELD_LONG_TITLE)
        }

    /** Returns true if the ComplicationData contains long text. I.e. if [longText] can succeed. */
    fun hasLongText(): Boolean =
        isFieldValidForType(FIELD_LONG_TEXT, type) && hasParcelableField(FIELD_LONG_TEXT)

    /**
     * Returns the *long text* field for this complication.
     *
     * The value is provided as a [ComplicationText] object, from which the text to display can be
     * obtained for a given point in time.
     *
     * Valid only if the type of this complication data is [TYPE_LONG_TEXT], otherwise returns null.
     */
    val longText: ComplicationText?
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_LONG_TEXT, type)
            return getParcelableFieldOrWarn<ComplicationText>(FIELD_LONG_TEXT)
        }

    /** Returns true if the ComplicationData contains an Icon. I.e. if [icon] can succeed. */
    fun hasIcon(): Boolean = isFieldValidForType(FIELD_ICON, type) && hasParcelableField(FIELD_ICON)

    /**
     * Returns the *icon* field for this complication, or `null` if no value was provided for the
     * field. The image returned is expected to be single-color and so may be tinted to whatever
     * color the watch face requires (but note that [android.graphics.drawable.Drawable.mutate]
     * should be called before drawables are tinted).
     *
     * If the device is in ambient mode, and utilises burn-in protection, then the result of
     * [burnInProtectionIcon] must be used instead of this.
     *
     * Valid for the types [TYPE_SHORT_TEXT], [TYPE_LONG_TEXT], [TYPE_RANGED_VALUE], [TYPE_ICON], or
     * [TYPE_NO_PERMISSION], otherwise returns null.
     */
    val icon: Icon?
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_ICON, type)
            return getParcelableFieldOrWarn<Icon>(FIELD_ICON)
        }

    /**
     * Returns true if the ComplicationData contains a burn in protection Icon. I.e. if
     * [burnInProtectionIcon] can succeed.
     */
    fun hasBurnInProtectionIcon(): Boolean =
        isFieldValidForType(FIELD_ICON_BURN_IN_PROTECTION, type) &&
            hasParcelableField(FIELD_ICON_BURN_IN_PROTECTION)

    /**
     * Returns the burn-in protection version of the *icon* field for this complication, or `null`
     * if no such icon was provided. The image returned is expected to be an outline image suitable
     * for use in ambient mode on screens with burn-in protection. The image is also expected to be
     * single-color and so may be tinted to whatever color the watch face requires (but note that
     * [android.graphics.drawable.Drawable.mutate] should be called before drawables are tinted, and
     * that the color used should be suitable for ambient mode with burn-in protection).
     *
     * If the device is in ambient mode, and utilises burn-in protection, then the result of this
     * method must be used instead of the result of [icon].
     *
     * Valid for the types [TYPE_SHORT_TEXT], [TYPE_LONG_TEXT], [TYPE_RANGED_VALUE], [TYPE_ICON], or
     * [TYPE_NO_PERMISSION], otherwise returns null.
     */
    val burnInProtectionIcon: Icon?
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_ICON_BURN_IN_PROTECTION, type)
            return getParcelableFieldOrWarn<Icon>(FIELD_ICON_BURN_IN_PROTECTION)
        }

    /**
     * Returns true if the ComplicationData contains a small image. I.e. if [smallImage] can
     * succeed.
     */
    fun hasSmallImage(): Boolean =
        isFieldValidForType(FIELD_SMALL_IMAGE, type) && hasParcelableField(FIELD_SMALL_IMAGE)

    /**
     * Returns the *small image* field for this complication, or `null` if no value was provided for
     * the field.
     *
     * This may be either a [IMAGE_STYLE_PHOTO] image, which is expected to fill the space
     * available, or an [IMAGE_STYLE_ICON] image, which should be drawn entirely within the space
     * available. Use [smallImageStyle] to determine which of these applies.
     *
     * As this may be any image, it is unlikely to be suitable for display in ambient mode when
     * burn-in protection is enabled, or in low-bit ambient mode, and should not be rendered under
     * these circumstances.
     *
     * Valid for the types [TYPE_LONG_TEXT] and [TYPE_SMALL_IMAGE]. Otherwise returns null.
     */
    val smallImage: Icon?
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_SMALL_IMAGE, type)
            return getParcelableFieldOrWarn<Icon>(FIELD_SMALL_IMAGE)
        }

    /**
     * Returns true if the ComplicationData contains a burn in protection small image. I.e. if
     * [burnInProtectionSmallImage] can succeed.
     *
     * @throws IllegalStateException for invalid types
     */
    fun hasBurnInProtectionSmallImage(): Boolean =
        isFieldValidForType(FIELD_SMALL_IMAGE_BURN_IN_PROTECTION, type) &&
            hasParcelableField(FIELD_SMALL_IMAGE_BURN_IN_PROTECTION)

    /**
     * Returns the burn-in protection version of the *small image* field for this complication, or
     * `null` if no such icon was provided. The image returned is expected to be an outline image
     * suitable for use in ambient mode on screens with burn-in protection. The image is also
     * expected to be single-color and so may be tinted to whatever color the watch face requires
     * (but note that [android.graphics.drawable.Drawable.mutate] should be called before drawables
     * are tinted, and that the color used should be suitable for ambient mode with burn-in
     * protection).
     *
     * If the device is in ambient mode, and utilises burn-in protection, then the result of this
     * method must be used instead of the result of [smallImage].
     *
     * Valid for the types [TYPE_LONG_TEXT] and [TYPE_SMALL_IMAGE]. Otherwise returns null.
     */
    val burnInProtectionSmallImage: Icon?
        get() {
            checkFieldValidForTypeWithoutThrowingException(
                FIELD_SMALL_IMAGE_BURN_IN_PROTECTION,
                type
            )
            return getParcelableFieldOrWarn<Icon>(FIELD_SMALL_IMAGE_BURN_IN_PROTECTION)
        }

    /**
     * Returns the *small image style* field for this complication.
     *
     * The result of this method should be taken in to account when drawing a small image
     * complication.
     *
     * Valid only for types that contain small images, i.e. [TYPE_SMALL_IMAGE] and [TYPE_LONG_TEXT].
     * Otherwise returns zero.
     *
     * @see IMAGE_STYLE_PHOTO which can be cropped but not recolored.
     * @see IMAGE_STYLE_ICON which can be recolored but not cropped.
     */
    @ImageStyle
    val smallImageStyle: Int
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_IMAGE_STYLE, type)
            return fields.getInt(FIELD_IMAGE_STYLE)
        }

    /**
     * Returns true if the ComplicationData contains a large image. I.e. if [largeImage] can
     * succeed.
     */
    fun hasLargeImage(): Boolean =
        isFieldValidForType(FIELD_LARGE_IMAGE, type) && hasParcelableField(FIELD_LARGE_IMAGE)

    /**
     * Returns the *large image* field for this complication. This image is expected to be of a
     * suitable size to fill the screen of the watch.
     *
     * As this may be any image, it is unlikely to be suitable for display in ambient mode when
     * burn-in protection is enabled, or in low-bit ambient mode, and should not be rendered under
     * these circumstances.
     *
     * Valid only if the type of this complication data is [TYPE_LARGE_IMAGE]. Otherwise returns
     * null.
     */
    val largeImage: Icon?
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_LARGE_IMAGE, type)
            return getParcelableFieldOrWarn<Icon>(FIELD_LARGE_IMAGE)
        }

    /**
     * Returns true if the ComplicationData contains a tap action. I.e. if [tapAction] can succeed.
     */
    fun hasTapAction(): Boolean =
        isFieldValidForType(FIELD_TAP_ACTION, type) && hasParcelableField(FIELD_TAP_ACTION)

    /**
     * Returns the *tap action* field for this complication. The result is a [PendingIntent] that
     * should be fired if the complication is tapped on, assuming the complication is tappable, or
     * `null` if no tap action has been specified.
     *
     * Valid for all non-empty types, otherwise returns null.
     */
    val tapAction: PendingIntent?
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_TAP_ACTION, type)
            return getParcelableFieldOrWarn<PendingIntent>(FIELD_TAP_ACTION)
        }

    /**
     * Returns true if the ComplicationData contains a content description. I.e. if
     * [contentDescription] can succeed.
     */
    fun hasContentDescription(): Boolean =
        isFieldValidForType(FIELD_CONTENT_DESCRIPTION, type) &&
            hasParcelableField(FIELD_CONTENT_DESCRIPTION)

    /**
     * Returns the *content description * field for this complication, for screen readers. This
     * usually describes the image, but may also describe the overall complication.
     *
     * Valid for all non-empty types.
     */
    val contentDescription: ComplicationText?
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_CONTENT_DESCRIPTION, type)
            return getParcelableFieldOrWarn<ComplicationText>(FIELD_CONTENT_DESCRIPTION)
        }

    /**
     * Returns the element weights for this complication.
     *
     * Valid only if the type of this complication data is [TYPE_WEIGHTED_ELEMENTS]. Otherwise
     * returns null.
     */
    val elementWeights: FloatArray?
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_ELEMENT_WEIGHTS, type)
            return fields.getFloatArray(FIELD_ELEMENT_WEIGHTS)
        }

    /**
     * Returns the element colors for this complication.
     *
     * Valid only if the type of this complication data is [TYPE_WEIGHTED_ELEMENTS]. Otherwise
     * returns null.
     */
    val elementColors: IntArray?
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_ELEMENT_COLORS, type)
            return fields.getIntArray(FIELD_ELEMENT_COLORS)
        }

    /**
     * Returns the background color to use between elements for this complication.
     *
     * Valid only if the type of this complication data is [TYPE_WEIGHTED_ELEMENTS]. Otherwise
     * returns 0.
     */
    @get:ColorInt
    val elementBackgroundColor: Int
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_ELEMENT_BACKGROUND_COLOR, type)
            return fields.getInt(FIELD_ELEMENT_BACKGROUND_COLOR)
        }

    /** Returns the placeholder ComplicationData if there is one or `null`. */
    val placeholder: ComplicationData?
        get() {
            checkFieldValidForType(FIELD_PLACEHOLDER_FIELDS, type)
            checkFieldValidForType(FIELD_PLACEHOLDER_TYPE, type)
            return if (
                !fields.containsKey(FIELD_PLACEHOLDER_FIELDS) ||
                    !fields.containsKey(FIELD_PLACEHOLDER_TYPE)
            ) {
                null
            } else {
                ComplicationData(
                    fields.getInt(FIELD_PLACEHOLDER_TYPE),
                    fields.getBundle(FIELD_PLACEHOLDER_FIELDS)!!
                )
            }
        }

    /** Returns the bytes of the proto layout. */
    val interactiveLayout: ByteArray?
        get() = fields.getByteArray(EXP_FIELD_PROTO_LAYOUT_INTERACTIVE)

    /**
     * Returns the list style hint.
     *
     * Valid only if the type of this complication data is [EXP_TYPE_LIST]. Otherwise returns zero.
     */
    val listStyleHint: Int
        get() {
            checkFieldValidForType(EXP_FIELD_LIST_STYLE_HINT, type)
            return fields.getInt(EXP_FIELD_LIST_STYLE_HINT)
        }

    /** Returns the bytes of the ambient proto layout. */
    val ambientLayout: ByteArray?
        get() = fields.getByteArray(EXP_FIELD_PROTO_LAYOUT_AMBIENT)

    /** Returns the bytes of the proto layout resources. */
    val layoutResources: ByteArray?
        get() = fields.getByteArray(EXP_FIELD_PROTO_LAYOUT_RESOURCES)

    /** Return's the complication's [ComplicationPersistencePolicies]. */
    @ComplicationPersistencePolicy
    val persistencePolicy: Int
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_PERSISTENCE_POLICY, type)
            return fields.getInt(
                FIELD_PERSISTENCE_POLICY,
                ComplicationPersistencePolicies.CACHING_ALLOWED
            )
        }

    /** Return's the complication's [ComplicationDisplayPolicy]. */
    @ComplicationDisplayPolicy
    val displayPolicy: Int
        get() {
            checkFieldValidForTypeWithoutThrowingException(FIELD_DISPLAY_POLICY, type)
            return fields.getInt(FIELD_DISPLAY_POLICY, ComplicationDisplayPolicies.ALWAYS_DISPLAY)
        }

    /**
     * Returns the start time for this complication data (i.e. the first time at which it should be
     * considered active and displayed), this may be 0. See also [isActiveAt].
     */
    val startDateTimeMillis: Long
        get() = fields.getLong(FIELD_START_TIME, 0)

    /**
     * Returns the end time for this complication data (i.e. the last time at which it should be
     * considered active and displayed), this may be [Long.MAX_VALUE]. See also [isActiveAt].
     */
    val endDateTimeMillis: Long
        get() = fields.getLong(FIELD_END_TIME, Long.MAX_VALUE)

    /**
     * Returns true if the complication data contains at least one text field with a value that may
     * change based on the current time.
     */
    val isTimeDependent: Boolean
        get() =
            isTimeDependentField(FIELD_SHORT_TEXT) ||
                isTimeDependentField(FIELD_SHORT_TITLE) ||
                isTimeDependentField(FIELD_LONG_TEXT) ||
                isTimeDependentField(FIELD_LONG_TITLE)

    private fun isTimeDependentField(field: String): Boolean {
        val text = getParcelableFieldOrWarn<ComplicationText>(field)
        return text != null && text.isTimeDependent
    }

    private fun <T : Parcelable?> getParcelableField(field: String): T? =
        try {
            @Suppress("deprecation") fields.getParcelable<T>(field)
        } catch (e: BadParcelableException) {
            null
        }

    private fun hasParcelableField(field: String) = getParcelableField<Parcelable>(field) != null

    private fun <T : Parcelable?> getParcelableFieldOrWarn(field: String): T? =
        try {
            @Suppress("deprecation") fields.getParcelable<T>(field)
        } catch (e: BadParcelableException) {
            Log.w(
                TAG,
                "Could not unparcel ComplicationData. Provider apps must exclude wearable " +
                    "support complication classes from proguard.",
                e
            )
            null
        }

    override fun toString() =
        if (shouldRedact()) {
            "ComplicationData{mType=$type, mFields=REDACTED}"
        } else {
            toStringNoRedaction()
        }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun toStringNoRedaction() = "ComplicationData{mType=$type, mFields=$fields}"

    override fun equals(other: Any?): Boolean =
        other is ComplicationData &&
            equalsWithoutExpressions(other) &&
            (!isFieldValidForType(FIELD_VALUE, type) || rangedValue == other.rangedValue) &&
            (!isFieldValidForType(FIELD_VALUE_EXPRESSION, type) ||
                rangedValueExpression?.toDynamicFloatByteArray() contentEquals
                    other.rangedValueExpression?.toDynamicFloatByteArray()) &&
            (!isFieldValidForType(FIELD_SHORT_TITLE, type) || shortTitle == other.shortTitle) &&
            (!isFieldValidForType(FIELD_SHORT_TEXT, type) || shortText == other.shortText) &&
            (!isFieldValidForType(FIELD_LONG_TITLE, type) || longTitle == other.longTitle) &&
            (!isFieldValidForType(FIELD_LONG_TEXT, type) || longText == other.longText) &&
            (!isFieldValidForType(FIELD_CONTENT_DESCRIPTION, type) ||
                contentDescription == other.contentDescription) &&
            (!isFieldValidForType(FIELD_PLACEHOLDER_TYPE, type) || placeholder == other.placeholder)

    /** Similar to [equals], but avoids comparing evaluated fields (if expressions exist). */
    infix fun equalsUnevaluated(other: ComplicationData): Boolean =
        equalsWithoutExpressions(other) &&
            if (
                !isFieldValidForType(FIELD_VALUE_EXPRESSION, type) || rangedValueExpression == null
            ) {
                !isFieldValidForType(FIELD_VALUE, type) || rangedValue == other.rangedValue
            } else {
                rangedValueExpression?.toDynamicFloatByteArray() contentEquals
                    other.rangedValueExpression?.toDynamicFloatByteArray()
            } &&
            (!isFieldValidForType(FIELD_SHORT_TITLE, type) ||
                shortTitle equalsUnevaluated other.shortTitle) &&
            (!isFieldValidForType(FIELD_SHORT_TEXT, type) ||
                shortText equalsUnevaluated other.shortText) &&
            (!isFieldValidForType(FIELD_LONG_TITLE, type) ||
                longTitle equalsUnevaluated other.longTitle) &&
            (!isFieldValidForType(FIELD_LONG_TEXT, type) ||
                longText equalsUnevaluated other.longText) &&
            (!isFieldValidForType(FIELD_CONTENT_DESCRIPTION, type) ||
                contentDescription.equalsUnevaluated(other.contentDescription)) &&
            (!isFieldValidForType(FIELD_PLACEHOLDER_TYPE, type) ||
                ((placeholder == null && other.placeholder == null) ||
                    ((placeholder != null && other.placeholder != null) &&
                        placeholder!! equalsUnevaluated other.placeholder!!)))

    private infix fun ComplicationText?.equalsUnevaluated(other: ComplicationText?): Boolean {
        if (this == null && other == null) return true
        if (this == null || other == null) return false
        // Both are non-null.
        if (expression == null) return equals(other)
        return expression?.toDynamicStringByteArray() contentEquals
            other.expression?.toDynamicStringByteArray()
    }

    private fun equalsWithoutExpressions(other: ComplicationData): Boolean =
        this === other ||
            (type == other.type &&
                (!isFieldValidForType(FIELD_TAP_ACTION_LOST, type) ||
                    tapActionLostDueToSerialization == other.tapActionLostDueToSerialization) &&
                (!isFieldValidForType(FIELD_TIMELINE_START_TIME, type) ||
                    timelineStartEpochSecond == other.timelineStartEpochSecond) &&
                (!isFieldValidForType(FIELD_TIMELINE_END_TIME, type) ||
                    timelineEndEpochSecond == other.timelineEndEpochSecond) &&
                (!isFieldValidForType(FIELD_TIMELINE_ENTRIES, type) ||
                    timelineEntries == other.timelineEntries) &&
                (!isFieldValidForType(EXP_FIELD_LIST_ENTRIES, type) ||
                    listEntries == other.listEntries) &&
                (!isFieldValidForType(FIELD_DATA_SOURCE, type) || dataSource == other.dataSource) &&
                (!isFieldValidForType(FIELD_VALUE_TYPE, type) ||
                    rangedValueType == other.rangedValueType) &&
                (!isFieldValidForType(FIELD_MIN_VALUE, type) ||
                    rangedMinValue == other.rangedMinValue) &&
                (!isFieldValidForType(FIELD_MAX_VALUE, type) ||
                    rangedMaxValue == other.rangedMaxValue) &&
                (!isFieldValidForType(FIELD_TARGET_VALUE, type) ||
                    targetValue == other.targetValue) &&
                (!isFieldValidForType(FIELD_COLOR_RAMP, type) ||
                    colorRamp contentEquals other.colorRamp) &&
                (!isFieldValidForType(FIELD_COLOR_RAMP_INTERPOLATED, type) ||
                    isColorRampInterpolated == other.isColorRampInterpolated) &&
                (!isFieldValidForType(FIELD_ICON, type) || icon iconEquals other.icon) &&
                (!isFieldValidForType(FIELD_ICON_BURN_IN_PROTECTION, type) ||
                    burnInProtectionIcon iconEquals other.burnInProtectionIcon) &&
                (!isFieldValidForType(FIELD_SMALL_IMAGE, type) ||
                    smallImage iconEquals other.smallImage) &&
                (!isFieldValidForType(FIELD_SMALL_IMAGE_BURN_IN_PROTECTION, type) ||
                    burnInProtectionSmallImage iconEquals other.burnInProtectionSmallImage) &&
                (!isFieldValidForType(FIELD_IMAGE_STYLE, type) ||
                    smallImageStyle == other.smallImageStyle) &&
                (!isFieldValidForType(FIELD_LARGE_IMAGE, type) ||
                    largeImage iconEquals other.largeImage) &&
                (!isFieldValidForType(FIELD_TAP_ACTION, type) || tapAction == other.tapAction) &&
                (!isFieldValidForType(FIELD_ELEMENT_WEIGHTS, type) ||
                    elementWeights contentEquals other.elementWeights) &&
                (!isFieldValidForType(FIELD_ELEMENT_COLORS, type) ||
                    elementColors contentEquals other.elementColors) &&
                (!isFieldValidForType(FIELD_ELEMENT_BACKGROUND_COLOR, type) ||
                    elementBackgroundColor == other.elementBackgroundColor) &&
                (!isFieldValidForType(EXP_FIELD_PROTO_LAYOUT_INTERACTIVE, type) ||
                    interactiveLayout contentEquals other.interactiveLayout) &&
                (!isFieldValidForType(EXP_FIELD_LIST_STYLE_HINT, type) ||
                    listStyleHint == other.listStyleHint) &&
                (!isFieldValidForType(EXP_FIELD_PROTO_LAYOUT_AMBIENT, type) ||
                    ambientLayout contentEquals other.ambientLayout) &&
                (!isFieldValidForType(EXP_FIELD_PROTO_LAYOUT_RESOURCES, type) ||
                    layoutResources contentEquals other.layoutResources) &&
                (!isFieldValidForType(FIELD_PERSISTENCE_POLICY, type) ||
                    persistencePolicy == other.persistencePolicy) &&
                (!isFieldValidForType(FIELD_DISPLAY_POLICY, type) ||
                    displayPolicy == other.displayPolicy) &&
                (!isFieldValidForType(FIELD_START_TIME, type) ||
                    startDateTimeMillis == other.startDateTimeMillis) &&
                (!isFieldValidForType(FIELD_END_TIME, type) ||
                    endDateTimeMillis == other.endDateTimeMillis))

    override fun hashCode(): Int =
        Objects.hash(
            type,
            if (isFieldValidForType(FIELD_TAP_ACTION_LOST, type)) {
                tapActionLostDueToSerialization
            } else {
                null
            },
            if (isFieldValidForType(FIELD_TIMELINE_START_TIME, type)) {
                timelineStartEpochSecond
            } else {
                null
            },
            if (isFieldValidForType(FIELD_TIMELINE_END_TIME, type)) timelineEndEpochSecond
            else null,
            if (isFieldValidForType(FIELD_TIMELINE_ENTRIES, type)) timelineEntries else null,
            if (isFieldValidForType(EXP_FIELD_LIST_ENTRIES, type)) listEntries else null,
            if (isFieldValidForType(FIELD_DATA_SOURCE, type)) dataSource else null,
            if (isFieldValidForType(FIELD_VALUE, type)) rangedValue else null,
            if (isFieldValidForType(FIELD_VALUE_EXPRESSION, type)) {
                Arrays.hashCode(rangedValueExpression?.toDynamicFloatByteArray())
            } else {
                null
            },
            if (isFieldValidForType(FIELD_VALUE_TYPE, type)) rangedValueType else null,
            if (isFieldValidForType(FIELD_MIN_VALUE, type)) rangedMinValue else null,
            if (isFieldValidForType(FIELD_MAX_VALUE, type)) rangedMaxValue else null,
            if (isFieldValidForType(FIELD_TARGET_VALUE, type)) targetValue else null,
            if (isFieldValidForType(FIELD_COLOR_RAMP, type)) colorRamp.contentHashCode() else null,
            if (isFieldValidForType(FIELD_COLOR_RAMP_INTERPOLATED, type)) {
                isColorRampInterpolated
            } else {
                null
            },
            if (isFieldValidForType(FIELD_SHORT_TITLE, type)) shortTitle else null,
            if (isFieldValidForType(FIELD_SHORT_TEXT, type)) shortText else null,
            if (isFieldValidForType(FIELD_LONG_TITLE, type)) longTitle else null,
            if (isFieldValidForType(FIELD_LONG_TEXT, type)) longText else null,
            if (isFieldValidForType(FIELD_ICON, type)) icon?.iconHashCode() else null,
            if (isFieldValidForType(FIELD_ICON_BURN_IN_PROTECTION, type)) {
                burnInProtectionIcon?.iconHashCode()
            } else {
                null
            },
            if (isFieldValidForType(FIELD_SMALL_IMAGE, type)) smallImage?.iconHashCode() else null,
            if (isFieldValidForType(FIELD_SMALL_IMAGE_BURN_IN_PROTECTION, type)) {
                burnInProtectionSmallImage?.iconHashCode()
            } else {
                null
            },
            if (isFieldValidForType(FIELD_IMAGE_STYLE, type)) smallImageStyle else null,
            if (isFieldValidForType(FIELD_LARGE_IMAGE, type)) largeImage?.iconHashCode() else null,
            if (isFieldValidForType(FIELD_TAP_ACTION, type)) tapAction else null,
            if (isFieldValidForType(FIELD_CONTENT_DESCRIPTION, type)) contentDescription else null,
            if (isFieldValidForType(FIELD_ELEMENT_WEIGHTS, type)) {
                elementWeights.contentHashCode()
            } else {
                null
            },
            if (isFieldValidForType(FIELD_ELEMENT_COLORS, type)) {
                elementColors.contentHashCode()
            } else {
                null
            },
            if (isFieldValidForType(FIELD_ELEMENT_BACKGROUND_COLOR, type)) {
                elementBackgroundColor
            } else {
                null
            },
            if (isFieldValidForType(FIELD_PLACEHOLDER_TYPE, type)) placeholder else null,
            if (isFieldValidForType(EXP_FIELD_PROTO_LAYOUT_INTERACTIVE, type)) {
                interactiveLayout.contentHashCode()
            } else {
                null
            },
            if (isFieldValidForType(EXP_FIELD_LIST_STYLE_HINT, type)) listStyleHint else null,
            if (isFieldValidForType(EXP_FIELD_PROTO_LAYOUT_AMBIENT, type)) {
                ambientLayout.contentHashCode()
            } else {
                null
            },
            if (isFieldValidForType(EXP_FIELD_PROTO_LAYOUT_RESOURCES, type)) {
                layoutResources.contentHashCode()
            } else {
                null
            },
            if (isFieldValidForType(FIELD_PERSISTENCE_POLICY, type)) persistencePolicy else null,
            if (isFieldValidForType(FIELD_DISPLAY_POLICY, type)) displayPolicy else null,
            if (isFieldValidForType(FIELD_START_TIME, type)) startDateTimeMillis else null,
            if (isFieldValidForType(FIELD_END_TIME, type)) endDateTimeMillis else null,
        )

    /** Builder class for [ComplicationData]. */
    class Builder {
        @ComplicationType internal val type: Int

        internal val fields: Bundle

        /** Creates a builder from given [ComplicationData], copying its type and data. */
        constructor(data: ComplicationData) {
            type = data.type
            fields = data.fields.clone() as Bundle
        }

        constructor(@ComplicationType type: Int) {
            this.type = type
            fields = Bundle()
            if (type == TYPE_SMALL_IMAGE || type == TYPE_LONG_TEXT) {
                setSmallImageStyle(IMAGE_STYLE_PHOTO)
            }
        }

        /** Sets the complication's [ComplicationPersistencePolicy]. */
        fun setPersistencePolicy(@ComplicationPersistencePolicy cachePolicy: Int) = apply {
            fields.putInt(FIELD_PERSISTENCE_POLICY, cachePolicy)
        }

        /** Sets the complication's [ComplicationDisplayPolicy]. */
        fun setDisplayPolicy(@ComplicationDisplayPolicy displayPolicy: Int) = apply {
            fields.putInt(FIELD_DISPLAY_POLICY, displayPolicy)
        }

        /**
         * Sets the start time for this complication data. This is optional for any type.
         *
         * The complication data will be considered inactive (i.e. should not be displayed) if the
         * current time is less than the start time. If not specified, the data is considered active
         * for all time up to the end time (or always active if end time is also not specified).
         *
         * Returns this Builder to allow chaining.
         */
        fun setStartDateTimeMillis(startDateTimeMillis: Long) = apply {
            fields.putLong(FIELD_START_TIME, startDateTimeMillis)
        }

        /**
         * Removes the start time for this complication data.
         *
         * Returns this Builder to allow chaining.
         */
        fun clearStartDateTime() = apply { fields.remove(FIELD_START_TIME) }

        /**
         * Sets the end time for this complication data. This is optional for any type.
         *
         * The complication data will be considered inactive (i.e. should not be displayed) if the
         * current time is greater than the end time. If not specified, the data is considered
         * active for all time after the start time (or always active if start time is also not
         * specified).
         *
         * Returns this Builder to allow chaining.
         */
        fun setEndDateTimeMillis(endDateTimeMillis: Long) = apply {
            fields.putLong(FIELD_END_TIME, endDateTimeMillis)
        }

        /**
         * Removes the end time for this complication data.
         *
         * Returns this Builder to allow chaining.
         */
        fun clearEndDateTime() = apply { fields.remove(FIELD_END_TIME) }

        /**
         * Sets the *value* field. This is required for the [TYPE_RANGED_VALUE] type, and the
         * [TYPE_GOAL_PROGRESS] type. For [TYPE_RANGED_VALUE] value must be in the range
         * [min .. max] for [TYPE_GOAL_PROGRESS] value must be >= and may be greater than target
         * value.
         *
         * Both the [TYPE_RANGED_VALUE] complication and the [TYPE_GOAL_PROGRESS] complication
         * visually present a single value, which is usually a percentage. E.g. you have completed
         * 70% of today's target of 10000 steps.
         *
         * Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        fun setRangedValue(value: Float?) = apply { putOrRemoveField(FIELD_VALUE, value) }

        /**
         * Sets the *valueExpression* field. It is evaluated to a value with the same limitations as
         * [setRangedValue].
         *
         * Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        fun setRangedValueExpression(value: DynamicFloat?) = apply {
            putOrRemoveField(FIELD_VALUE_EXPRESSION, value?.toDynamicFloatByteArray())
        }

        /**
         * Sets the *value type* field which provides meta data about the value. This is optional
         * for the [TYPE_RANGED_VALUE] type.
         */
        fun setRangedValueType(valueType: Int) = apply { putIntField(FIELD_VALUE_TYPE, valueType) }

        /**
         * Sets the *min value* field. This is required for the [TYPE_RANGED_VALUE] type, and is not
         * valid for any other type. A [TYPE_RANGED_VALUE] complication visually presents a single
         * value, which is usually a percentage. E.g. you have completed 70% of today's target of
         * 10000 steps.
         *
         * Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        fun setRangedMinValue(minValue: Float) = apply { putFloatField(FIELD_MIN_VALUE, minValue) }

        /**
         * Sets the *max value* field. This is required for the [TYPE_RANGED_VALUE] type, and is not
         * valid for any other type. A [TYPE_RANGED_VALUE] complication visually presents a single
         * value, which is usually a percentage. E.g. you have completed 70% of today's target of
         * 10000 steps.
         *
         * Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        fun setRangedMaxValue(maxValue: Float) = apply { putFloatField(FIELD_MAX_VALUE, maxValue) }

        /**
         * Sets the *targetValue* field. This is required for the [TYPE_GOAL_PROGRESS] type, and is
         * not valid for any other type. A [TYPE_GOAL_PROGRESS] complication visually presents a
         * single value, which is usually a percentage. E.g. you have completed 70% of today's
         * target of 10000 steps.
         *
         * Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        fun setTargetValue(targetValue: Float) = apply {
            putFloatField(FIELD_TARGET_VALUE, targetValue)
        }

        /**
         * Sets the *long title* field. This is optional for the [TYPE_LONG_TEXT] type, and is not
         * valid for any other type.
         *
         * The value must be provided as a [ComplicationText] object, so that time-dependent values
         * may be included.
         *
         * Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        fun setLongTitle(longTitle: ComplicationText?) = apply {
            putOrRemoveField(FIELD_LONG_TITLE, longTitle)
        }

        /**
         * Sets the *long text* field. This is required for the [TYPE_LONG_TEXT] type, and is not
         * valid for any other type.
         *
         * The value must be provided as a [ComplicationText] object, so that time-dependent values
         * may be included.
         *
         * Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        fun setLongText(longText: ComplicationText?) = apply {
            putOrRemoveField(FIELD_LONG_TEXT, longText)
        }

        /**
         * Sets the *short title* field. This is valid for the [TYPE_SHORT_TEXT],
         * [TYPE_RANGED_VALUE], and [TYPE_NO_PERMISSION] types, and is not valid for any other type.
         *
         * The value must be provided as a [ComplicationText] object, so that time-dependent values
         * may be included.
         *
         * The length of the text, including any time-dependent values, should not exceed seven
         * characters. If it does, the text may be truncated by the watch face or might not fit in
         * the complication.
         *
         * Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        fun setShortTitle(shortTitle: ComplicationText?) = apply {
            putOrRemoveField(FIELD_SHORT_TITLE, shortTitle)
        }

        /**
         * Sets the *short text* field. This is required for the [TYPE_SHORT_TEXT] type, is optional
         * for the [TYPE_RANGED_VALUE] and [TYPE_NO_PERMISSION] types, and is not valid for any
         * other type.
         *
         * The value must be provided as a [ComplicationText] object, so that time-dependent values
         * may be included.
         *
         * The length of the text, including any time-dependent values, should not exceed seven
         * characters. If it does, the text may be truncated by the watch face or might not fit in
         * the complication.
         *
         * Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        fun setShortText(shortText: ComplicationText?) = apply {
            putOrRemoveField(FIELD_SHORT_TEXT, shortText)
        }

        /**
         * Sets the *icon* field. This is required for the [TYPE_ICON] type, and is optional for the
         * [TYPE_SHORT_TEXT], [TYPE_LONG_TEXT], [TYPE_RANGED_VALUE], and [TYPE_NO_PERMISSION] types.
         *
         * The provided image must be single-color, so that watch faces can tint it as required.
         *
         * If the icon provided here is not suitable for display in ambient mode with burn-in
         * protection (e.g. if it includes solid blocks of pixels), then a burn-in safe version of
         * the icon must be provided via [setBurnInProtectionIcon].
         *
         * Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        fun setIcon(icon: Icon?) = apply { putOrRemoveField(FIELD_ICON, icon) }

        /**
         * Sets the burn-in protection version of the *icon* field. This should be provided if the
         * *icon* field is provided, unless the main icon is already safe for use with burn-in
         * protection. This icon should have fewer lit pixels, and should use darker colors to
         * prevent LCD burn in issues.
         *
         * The provided image must be single-color, so that watch faces can tint it as required.
         *
         * The provided image must not contain solid blocks of pixels - it should instead be
         * composed of outlines or lines only.
         *
         * If this field is set, the *icon* field must also be set.
         *
         * Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        fun setBurnInProtectionIcon(icon: Icon?) = apply {
            putOrRemoveField(FIELD_ICON_BURN_IN_PROTECTION, icon)
        }

        /**
         * Sets the *small image* field. This is required for the [TYPE_SMALL_IMAGE] type, and is
         * optional for the [TYPE_LONG_TEXT] type.
         *
         * Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        fun setSmallImage(smallImage: Icon?) = apply {
            putOrRemoveField(FIELD_SMALL_IMAGE, smallImage)
        }

        /**
         * Sets the burn-in protection version of the *small image* field. This should be provided
         * if the *small image* field is provided, unless the main small image is already safe for
         * use with burn-in protection.
         *
         * The provided image must not contain solid blocks of pixels - it should instead be
         * composed of outlines or lines only.
         *
         * If this field is set, the *small image* field must also be set.
         *
         * Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        fun setBurnInProtectionSmallImage(smallImage: Icon?) = apply {
            putOrRemoveField(FIELD_SMALL_IMAGE_BURN_IN_PROTECTION, smallImage)
        }

        /**
         * Sets the display style for this complication data. This is valid only for types that
         * contain small images, i.e. [TYPE_SMALL_IMAGE] and [TYPE_LONG_TEXT].
         *
         * This affects how watch faces will draw the image in the complication.
         *
         * If not specified, the default is [IMAGE_STYLE_PHOTO].
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         * @see .IMAGE_STYLE_PHOTO which can be cropped but not recolored.
         * @see .IMAGE_STYLE_ICON which can be recolored but not cropped.
         */
        fun setSmallImageStyle(@ImageStyle imageStyle: Int) = apply {
            putIntField(FIELD_IMAGE_STYLE, imageStyle)
        }

        /**
         * Sets the *large image* field. This is required for the [TYPE_LARGE_IMAGE] type, and is
         * not valid for any other type.
         *
         * The provided image should be suitably sized to fill the screen of the watch.
         *
         * Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        fun setLargeImage(largeImage: Icon?) = apply {
            putOrRemoveField(FIELD_LARGE_IMAGE, largeImage)
        }

        /**
         * Sets the list style hint
         *
         * Valid only if the type of this complication data is [EXP_TYPE_LIST]. Otherwise returns
         * zero.
         */
        fun setListStyleHint(listStyleHint: Int) = apply {
            putIntField(EXP_FIELD_LIST_STYLE_HINT, listStyleHint)
        }

        /**
         * Sets the *tap action* field. This is optional for any non-empty type.
         *
         * The provided [PendingIntent] may be fired if the complication is tapped on. Note that
         * some complications might not be tappable, in which case this field will be ignored.
         *
         * Returns this Builder to allow chaining.
         */
        fun setTapAction(pendingIntent: PendingIntent?) = apply {
            putOrRemoveField(FIELD_TAP_ACTION, pendingIntent)
        }

        /**
         * Sets the *content description* field for accessibility. This is optional for any
         * non-empty type. It is recommended to provide a content description whenever the data
         * includes an image.
         *
         * The provided text will be read aloud by a Text-to-speech converter for users who may be
         * vision-impaired. It will be read aloud in addition to any long, short, or range text in
         * the complication.
         *
         * If using to describe an image/icon that is purely stylistic and doesn't convey any
         * information to the user, you may set the image content description to an empty string
         * ("").
         *
         * Returns this Builder to allow chaining.
         */
        fun setContentDescription(description: ComplicationText?) = apply {
            putOrRemoveField(FIELD_CONTENT_DESCRIPTION, description)
        }

        /**
         * Sets whether or not this ComplicationData has been serialized.
         *
         * Returns this Builder to allow chaining.
         */
        fun setTapActionLostDueToSerialization(tapActionLostDueToSerialization: Boolean) = apply {
            if (tapActionLostDueToSerialization) {
                fields.putBoolean(FIELD_TAP_ACTION_LOST, tapActionLostDueToSerialization)
            }
        }

        /**
         * Sets the placeholder.
         *
         * Returns this Builder to allow chaining.
         */
        fun setPlaceholder(placeholder: ComplicationData?) = apply {
            if (placeholder == null) {
                fields.remove(FIELD_PLACEHOLDER_FIELDS)
                fields.remove(FIELD_PLACEHOLDER_TYPE)
            } else {
                checkFieldValidForType(FIELD_PLACEHOLDER_FIELDS, type)
                fields.putBundle(FIELD_PLACEHOLDER_FIELDS, placeholder.fields)
                putIntField(FIELD_PLACEHOLDER_TYPE, placeholder.type)
            }
        }

        /**
         * Sets the [ComponentName] of the ComplicationDataSourceService that provided this
         * ComplicationData. Generally this field should be set and is only nullable for backwards
         * compatibility.
         *
         * Returns this Builder to allow chaining.
         */
        fun setDataSource(provider: ComponentName?) = apply {
            putOrRemoveField(FIELD_DATA_SOURCE, provider)
        }

        /**
         * Sets the ambient proto layout associated with this complication.
         *
         * Returns this Builder to allow chaining.
         */
        fun setAmbientLayout(ambientProtoLayout: ByteArray) = apply {
            putByteArrayField(EXP_FIELD_PROTO_LAYOUT_AMBIENT, ambientProtoLayout)
        }

        /**
         * Sets the proto layout associated with this complication.
         *
         * Returns this Builder to allow chaining.
         */
        fun setInteractiveLayout(protoLayout: ByteArray) = apply {
            putByteArrayField(EXP_FIELD_PROTO_LAYOUT_INTERACTIVE, protoLayout)
        }

        /**
         * Sets the proto layout resources associated with this complication.
         *
         * Returns this Builder to allow chaining.
         */
        fun setLayoutResources(resources: ByteArray) = apply {
            putByteArrayField(EXP_FIELD_PROTO_LAYOUT_RESOURCES, resources)
        }

        /**
         * Optional. Sets the color the color ramp should be drawn with.
         *
         * Returns this Builder to allow chaining.
         */
        fun setColorRamp(colorRamp: IntArray?) = apply {
            putOrRemoveField(FIELD_COLOR_RAMP, colorRamp)
        }

        /**
         * Optional. Sets whether or not the color ramp should be smoothly shaded or drawn with
         * steps.
         *
         * Returns this Builder to allow chaining.
         */
        fun setColorRampIsSmoothShaded(isSmoothShaded: Boolean?) = apply {
            putOrRemoveField(FIELD_COLOR_RAMP_INTERPOLATED, isSmoothShaded)
        }

        /**
         * Sets the list of [ComplicationData] timeline entries.
         *
         * Returns this Builder to allow chaining.
         */
        fun setListEntryCollection(timelineEntries: Collection<ComplicationData>?) = apply {
            if (timelineEntries == null) {
                fields.remove(EXP_FIELD_LIST_ENTRIES)
            } else {
                fields.putParcelableArray(
                    EXP_FIELD_LIST_ENTRIES,
                    timelineEntries
                        .map { data ->
                            data.fields.putInt(EXP_FIELD_LIST_ENTRY_TYPE, data.type)
                            data.fields
                        }
                        .toTypedArray()
                )
            }
        }

        /**
         * Sets the element weights for this complication.
         *
         * Returns this Builder to allow chaining.
         */
        fun setElementWeights(elementWeights: FloatArray?) = apply {
            putOrRemoveField(FIELD_ELEMENT_WEIGHTS, elementWeights)
        }

        /**
         * Sets the element colors for this complication.
         *
         * Returns this Builder to allow chaining.
         */
        fun setElementColors(elementColors: IntArray?) = apply {
            putOrRemoveField(FIELD_ELEMENT_COLORS, elementColors)
        }

        /**
         * Sets the background color to use between elements for this complication.
         *
         * Returns this Builder to allow chaining.
         */
        fun setElementBackgroundColor(@ColorInt elementBackgroundColor: Int) = apply {
            putOrRemoveField(FIELD_ELEMENT_BACKGROUND_COLOR, elementBackgroundColor)
        }

        /**
         * Constructs and returns [ComplicationData] with the provided fields. All required fields
         * must be populated before this method is called.
         *
         * @throws IllegalStateException if the required fields have not been populated
         */
        fun build(): ComplicationData {
            // Validate.
            for (requiredField in REQUIRED_FIELDS[type]!!) {
                check(fields.containsKey(requiredField)) {
                    "Field $requiredField is required for type $type"
                }
                check(
                    !(fields.containsKey(FIELD_ICON_BURN_IN_PROTECTION) &&
                        !fields.containsKey(FIELD_ICON))
                ) {
                    "Field ICON must be provided when field ICON_BURN_IN_PROTECTION is provided."
                }
                check(
                    !(fields.containsKey(FIELD_SMALL_IMAGE_BURN_IN_PROTECTION) &&
                        !fields.containsKey(FIELD_SMALL_IMAGE))
                ) {
                    "Field SMALL_IMAGE must be provided when field SMALL_IMAGE_BURN_IN_PROTECTION" +
                        " is provided."
                }
            }
            for (requiredOneOfFieldGroup in REQUIRED_ONE_OF_FIELDS[type]!!) {
                check(requiredOneOfFieldGroup.count { fields.containsKey(it) } >= 1) {
                    "One of $requiredOneOfFieldGroup must be provided."
                }
            }
            return ComplicationData(this)
        }

        private fun putIntField(field: String, value: Int) {
            checkFieldValidForType(field, type)
            fields.putInt(field, value)
        }

        private fun putFloatField(field: String, value: Float) {
            checkFieldValidForType(field, type)
            fields.putFloat(field, value)
        }

        private fun putByteArrayField(field: String, value: ByteArray) {
            checkFieldValidForType(field, type)
            fields.putByteArray(field, value)
        }

        /** Sets the field with obj or removes it if null. */
        private fun putOrRemoveField(field: String, obj: Any?) {
            checkFieldValidForType(field, type)
            if (obj == null) {
                fields.remove(field)
                return
            }
            when (obj) {
                is Boolean -> fields.putBoolean(field, obj)
                is Int -> fields.putInt(field, obj)
                is Float -> fields.putFloat(field, obj)
                is String -> fields.putString(field, obj)
                is Parcelable -> fields.putParcelable(field, obj)
                is ByteArray -> fields.putByteArray(field, obj)
                is IntArray -> fields.putIntArray(field, obj)
                is FloatArray -> fields.putFloatArray(field, obj)
                else -> throw IllegalArgumentException("Unexpected object type: " + obj.javaClass)
            }
        }
    }

    companion object {
        private const val TAG = "ComplicationData"
        const val PLACEHOLDER_STRING = "__placeholder__"

        /**
         * Type sent when a complication does not have a provider configured. The system will send
         * data of this type to watch faces when the user has not chosen a provider for an active
         * complication, and the watch face has not set a default provider. Providers cannot send
         * data of this type.
         *
         * No fields may be populated for complication data of this type.
         */
        const val TYPE_NOT_CONFIGURED = 1

        /**
         * Type sent when the user has specified that an active complication should have no
         * provider, i.e. when the user has chosen "Empty" in the provider chooser. Providers cannot
         * send data of this type.
         *
         * No fields may be populated for complication data of this type.
         */
        const val TYPE_EMPTY = 2

        /**
         * Type that can be sent by any provider, regardless of the configured type, when the
         * provider has no data to be displayed. Watch faces may choose whether to render this in
         * some way or leave the slot empty.
         *
         * No fields may be populated for complication data of this type.
         */
        const val TYPE_NO_DATA = 10

        /**
         * Type used for complications where the primary piece of data is a short piece of text
         * (expected to be no more than seven characters in length). The short text may be
         * accompanied by an icon or a short title (or both, but if both are provided then a watch
         * face may choose to display only one).
         *
         * The *short text* field is required for this type, and is expected to always be displayed.
         *
         * The *icon* (and *burnInProtectionIcon*) and *short title* fields are optional for this
         * type. If only one of these is provided, it is expected that it will be displayed. If both
         * are provided, it is expected that one of these will be displayed.
         */
        const val TYPE_SHORT_TEXT = 3

        /**
         * Type used for complications where the primary piece of data is a piece of text. The text
         * may be accompanied by an icon and/or a title.
         *
         * The *long text* field is required for this type, and is expected to always be displayed.
         *
         * The *long title* field is optional for this type. If provided, it is expected that this
         * field will be displayed.
         *
         * The *icon* (and *burnInProtectionIcon*) and *small image* fields are also optional for
         * this type. If provided, at least one of these should be displayed.
         */
        const val TYPE_LONG_TEXT = 4

        /**
         * Type used for complications including a numerical value within a range, such as a
         * percentage. The value may be accompanied by an icon and/or short text and title.
         *
         * The *value*, *min value*, and *max value* fields are required for this type, and the
         * value within the range is expected to always be displayed.
         *
         * The *icon* (and *burnInProtectionIcon*), *short title*, and *short text* fields are
         * optional for this type, but at least one must be defined. The watch face may choose which
         * of these fields to display, if any.
         */
        const val TYPE_RANGED_VALUE = 5

        /**
         * Type used for complications which consist only of a tintable icon.
         *
         * The *icon* field is required for this type, and is expected to always be displayed,
         * unless the device is in ambient mode with burn-in protection enabled, in which case the
         * *burnInProtectionIcon* field should be used instead.
         *
         * The contentDescription field is recommended for this type. Use it to describe what data
         * the icon represents. If the icon is purely stylistic, and does not convey any information
         * to the user, then enter the empty string as the contentDescription.
         *
         * No other fields are valid for this type.
         */
        const val TYPE_ICON = 6

        /**
         * Type used for complications which consist only of a small image.
         *
         * The *small image* field is required for this type, and is expected to always be
         * displayed, unless the device is in ambient mode, in which case either nothing or the
         * *burnInProtectionSmallImage* field may be used instead.
         *
         * The contentDescription field is recommended for this type. Use it to describe what data
         * the image represents. If the image is purely stylistic, and does not convey any
         * information to the user, then enter the empty string as the contentDescription.
         *
         * No other fields are valid for this type.
         */
        const val TYPE_SMALL_IMAGE = 7

        /**
         * Type used for complications which consist only of a large image. A large image here is
         * one that could be used to fill the watch face, for example as the background.
         *
         * The *large image* field is required for this type, and is expected to always be
         * displayed, unless the device is in ambient mode.
         *
         * The contentDescription field is recommended for this type. Use it to describe what data
         * the image represents. If the image is purely stylistic, and does not convey any
         * information to the user, then enter the empty string as the contentDescription.
         *
         * No other fields are valid for this type.
         */
        const val TYPE_LARGE_IMAGE = 8

        /**
         * Type sent by the system when the watch face does not have permission to receive
         * complication data.
         *
         * Fields will be populated to allow the data to be rendered as if it were of
         * [TYPE_SHORT_TEXT] or [TYPE_ICON] for consistency and convenience, but watch faces may
         * render this as they see fit.
         *
         * It is recommended that, where possible, tapping on the complication when in this state
         * should trigger a permission request.
         */
        const val TYPE_NO_PERMISSION = 9

        /**
         * Type used for complications which indicate progress towards a goal. The value may be
         * accompanied by an icon and/or short text and title.
         *
         * The *value*, and *target value* fields are required for this type, and the value is
         * expected to always be displayed. The value must be >= 0 and may be > target value. E.g.
         * 15000 out of a target of 10000 steps.
         *
         * The *icon* (and *burnInProtectionIcon*), *short title*, and *short text* fields are
         * optional for this type, but at least one must be defined. The watch face may choose which
         * of these fields to display, if any.
         */
        const val TYPE_GOAL_PROGRESS = 13

        /**
         * Type used for complications to display a series of weighted values e.g. in a pie chart.
         * The weighted values may be accompanied by an icon and/or short text and title.
         *
         * The *element weights* and *element colors* fields are required for this type, and the
         * value within the range is expected to always be displayed.
         *
         * The *icon* (and *burnInProtectionIcon*), *short title*, and *short text* fields are
         * optional for this type, but at least one must be defined. The watch face may choose which
         * of these fields to display, if any.
         */
        const val TYPE_WEIGHTED_ELEMENTS = 14

        // The following types are experimental, and they have negative IDs.
        /** Type that specifies a proto layout based complication. */
        const val EXP_TYPE_PROTO_LAYOUT = -11

        /** Type that specifies a list of complication values. E.g. to support linear 3. */
        const val EXP_TYPE_LIST = -12

        /**
         * Style for small images which are photos that are expected to fill the space available.
         * Images of this style may be cropped to fit the shape of the complication - in particular,
         * the image may be cropped to a circle. Photos my not be recolored.
         *
         * This is the default value.
         */
        const val IMAGE_STYLE_PHOTO = 1

        /**
         * Style for small images that have a transparent background and are expected to be drawn
         * entirely within the space available, such as a launcher icon. Watch faces may add padding
         * when drawing these images, but should never crop these images. Icons may be recolored to
         * fit the complication style.
         */
        const val IMAGE_STYLE_ICON = 2
        private const val FIELD_COLOR_RAMP = "COLOR_RAMP"
        private const val FIELD_COLOR_RAMP_INTERPOLATED = "COLOR_RAMP_INTERPOLATED"
        private const val FIELD_DATA_SOURCE = "FIELD_DATA_SOURCE"
        private const val FIELD_DISPLAY_POLICY = "DISPLAY_POLICY"
        private const val FIELD_ELEMENT_BACKGROUND_COLOR = "ELEMENT_BACKGROUND_COLOR"
        private const val FIELD_ELEMENT_COLORS = "ELEMENT_COLORS"
        private const val FIELD_ELEMENT_WEIGHTS = "ELEMENT_WEIGHTS"
        private const val FIELD_END_TIME = "END_TIME"
        private const val FIELD_ICON = "ICON"
        private const val FIELD_ICON_BURN_IN_PROTECTION = "ICON_BURN_IN_PROTECTION"
        private const val FIELD_IMAGE_STYLE = "IMAGE_STYLE"
        private const val FIELD_LARGE_IMAGE = "LARGE_IMAGE"
        private const val FIELD_LONG_TITLE = "LONG_TITLE"
        private const val FIELD_LONG_TEXT = "LONG_TEXT"
        private const val FIELD_MAX_VALUE = "MAX_VALUE"
        private const val FIELD_MIN_VALUE = "MIN_VALUE"
        private const val FIELD_PERSISTENCE_POLICY = "PERSISTENCE_POLICY"
        private const val FIELD_PLACEHOLDER_FIELDS = "PLACEHOLDER_FIELDS"
        private const val FIELD_PLACEHOLDER_TYPE = "PLACEHOLDER_TYPE"
        private const val FIELD_SMALL_IMAGE = "SMALL_IMAGE"
        private const val FIELD_SMALL_IMAGE_BURN_IN_PROTECTION = "SMALL_IMAGE_BURN_IN_PROTECTION"
        private const val FIELD_SHORT_TITLE = "SHORT_TITLE"
        private const val FIELD_SHORT_TEXT = "SHORT_TEXT"
        private const val FIELD_START_TIME = "START_TIME"
        private const val FIELD_TAP_ACTION = "TAP_ACTION"
        private const val FIELD_TAP_ACTION_LOST = "FIELD_TAP_ACTION_LOST"
        private const val FIELD_TARGET_VALUE = "TARGET_VALUE"
        private const val FIELD_TIMELINE_START_TIME = "TIMELINE_START_TIME"
        private const val FIELD_TIMELINE_END_TIME = "TIMELINE_END_TIME"
        private const val FIELD_TIMELINE_ENTRIES = "TIMELINE"
        private const val FIELD_TIMELINE_ENTRY_TYPE = "TIMELINE_ENTRY_TYPE"
        private const val FIELD_VALUE = "VALUE"
        private const val FIELD_VALUE_EXPRESSION = "VALUE_EXPRESSION"
        private const val FIELD_VALUE_TYPE = "VALUE_TYPE"

        // Experimental fields, these are subject to change without notice.
        private const val EXP_FIELD_LIST_ENTRIES = "EXP_LIST_ENTRIES"
        private const val EXP_FIELD_LIST_ENTRY_TYPE = "EXP_LIST_ENTRY_TYPE"
        private const val EXP_FIELD_LIST_STYLE_HINT = "EXP_LIST_STYLE_HINT"
        private const val EXP_FIELD_PROTO_LAYOUT_AMBIENT = "EXP_FIELD_PROTO_LAYOUT_AMBIENT"
        private const val EXP_FIELD_PROTO_LAYOUT_INTERACTIVE = "EXP_FIELD_PROTO_LAYOUT_INTERACTIVE"
        private const val EXP_FIELD_PROTO_LAYOUT_RESOURCES = "EXP_FIELD_PROTO_LAYOUT_RESOURCES"

        // Originally it was planned to support both content and image content descriptions.
        private const val FIELD_CONTENT_DESCRIPTION = "IMAGE_CONTENT_DESCRIPTION"

        // The set of valid types.
        private val VALID_TYPES: Set<Int> =
            setOf(
                TYPE_NOT_CONFIGURED,
                TYPE_EMPTY,
                TYPE_SHORT_TEXT,
                TYPE_LONG_TEXT,
                TYPE_RANGED_VALUE,
                TYPE_ICON,
                TYPE_SMALL_IMAGE,
                TYPE_LARGE_IMAGE,
                TYPE_NO_PERMISSION,
                TYPE_NO_DATA,
                EXP_TYPE_PROTO_LAYOUT,
                EXP_TYPE_LIST,
                TYPE_GOAL_PROGRESS,
                TYPE_WEIGHTED_ELEMENTS,
            )

        // Used for validation. REQUIRED_FIELDS[i] is a list containing all the fields which must be
        // populated for @ComplicationType i.
        private val REQUIRED_FIELDS: Map<Int, Set<String>> =
            mapOf(
                TYPE_NOT_CONFIGURED to setOf(),
                TYPE_EMPTY to setOf(),
                TYPE_SHORT_TEXT to setOf(FIELD_SHORT_TEXT),
                TYPE_LONG_TEXT to setOf(FIELD_LONG_TEXT),
                TYPE_RANGED_VALUE to setOf(FIELD_MIN_VALUE, FIELD_MAX_VALUE),
                TYPE_ICON to setOf(FIELD_ICON),
                TYPE_SMALL_IMAGE to setOf(FIELD_SMALL_IMAGE, FIELD_IMAGE_STYLE),
                TYPE_LARGE_IMAGE to setOf(FIELD_LARGE_IMAGE),
                TYPE_NO_PERMISSION to setOf(),
                TYPE_NO_DATA to setOf(),
                EXP_TYPE_PROTO_LAYOUT to
                    setOf(
                        EXP_FIELD_PROTO_LAYOUT_AMBIENT,
                        EXP_FIELD_PROTO_LAYOUT_INTERACTIVE,
                        EXP_FIELD_PROTO_LAYOUT_RESOURCES,
                    ),
                EXP_TYPE_LIST to setOf(EXP_FIELD_LIST_ENTRIES),
                TYPE_GOAL_PROGRESS to setOf(FIELD_TARGET_VALUE),
                TYPE_WEIGHTED_ELEMENTS to
                    setOf(
                        FIELD_ELEMENT_WEIGHTS,
                        FIELD_ELEMENT_COLORS,
                        FIELD_ELEMENT_BACKGROUND_COLOR,
                    ),
            )

        // Used for validation. REQUIRED_ONE_OF_FIELDS[i] is a list of field groups of which at
        // least one field must be populated for @ComplicationType i.
        // If a field is also in REQUIRED_FIELDS[i], it is not required if another field in the one
        // of group is populated.
        private val REQUIRED_ONE_OF_FIELDS: Map<Int, Set<Set<String>>> =
            mapOf(
                TYPE_NOT_CONFIGURED to setOf(),
                TYPE_EMPTY to setOf(),
                TYPE_SHORT_TEXT to setOf(),
                TYPE_LONG_TEXT to setOf(),
                TYPE_RANGED_VALUE to setOf(setOf(FIELD_VALUE, FIELD_VALUE_EXPRESSION)),
                TYPE_ICON to setOf(),
                TYPE_SMALL_IMAGE to setOf(),
                TYPE_LARGE_IMAGE to setOf(),
                TYPE_NO_PERMISSION to setOf(),
                TYPE_NO_DATA to setOf(),
                EXP_TYPE_PROTO_LAYOUT to setOf(),
                EXP_TYPE_LIST to setOf(),
                TYPE_GOAL_PROGRESS to setOf(setOf(FIELD_VALUE, FIELD_VALUE_EXPRESSION)),
                TYPE_WEIGHTED_ELEMENTS to setOf(),
            )

        private val COMMON_OPTIONAL_FIELDS: Array<String> =
            arrayOf(
                FIELD_TAP_ACTION,
                FIELD_CONTENT_DESCRIPTION,
                FIELD_DATA_SOURCE,
                FIELD_PERSISTENCE_POLICY,
                FIELD_DISPLAY_POLICY,
                FIELD_TIMELINE_START_TIME,
                FIELD_TIMELINE_END_TIME,
                FIELD_START_TIME,
                FIELD_END_TIME,
                FIELD_TIMELINE_ENTRIES,
                FIELD_TIMELINE_ENTRY_TYPE,
                // Placeholder or fallback.
                FIELD_PLACEHOLDER_FIELDS,
                FIELD_PLACEHOLDER_TYPE,
            )

        // Used for validation. OPTIONAL_FIELDS[i] is a list containing all the fields which are
        // valid but not required for type i.
        private val OPTIONAL_FIELDS: Map<Int, Set<String>> =
            mapOf(
                TYPE_NOT_CONFIGURED to setOf(),
                TYPE_EMPTY to setOf(),
                TYPE_SHORT_TEXT to
                    setOf(
                        *COMMON_OPTIONAL_FIELDS,
                        FIELD_SHORT_TITLE,
                        FIELD_ICON,
                        FIELD_ICON_BURN_IN_PROTECTION,
                        FIELD_SMALL_IMAGE,
                        FIELD_SMALL_IMAGE_BURN_IN_PROTECTION,
                        FIELD_IMAGE_STYLE,
                    ),
                TYPE_LONG_TEXT to
                    setOf(
                        *COMMON_OPTIONAL_FIELDS,
                        FIELD_LONG_TITLE,
                        FIELD_ICON,
                        FIELD_ICON_BURN_IN_PROTECTION,
                        FIELD_SMALL_IMAGE,
                        FIELD_SMALL_IMAGE_BURN_IN_PROTECTION,
                        FIELD_IMAGE_STYLE,
                    ),
                TYPE_RANGED_VALUE to
                    setOf(
                        *COMMON_OPTIONAL_FIELDS,
                        FIELD_SHORT_TEXT,
                        FIELD_SHORT_TITLE,
                        FIELD_ICON,
                        FIELD_ICON_BURN_IN_PROTECTION,
                        FIELD_SMALL_IMAGE,
                        FIELD_SMALL_IMAGE_BURN_IN_PROTECTION,
                        FIELD_IMAGE_STYLE,
                        FIELD_COLOR_RAMP,
                        FIELD_COLOR_RAMP_INTERPOLATED,
                        FIELD_VALUE_TYPE,
                    ),
                TYPE_ICON to
                    setOf(
                        *COMMON_OPTIONAL_FIELDS,
                        FIELD_ICON_BURN_IN_PROTECTION,
                    ),
                TYPE_SMALL_IMAGE to
                    setOf(
                        *COMMON_OPTIONAL_FIELDS,
                        FIELD_SMALL_IMAGE_BURN_IN_PROTECTION,
                    ),
                TYPE_LARGE_IMAGE to
                    setOf(
                        *COMMON_OPTIONAL_FIELDS,
                    ),
                TYPE_NO_PERMISSION to
                    setOf(
                        *COMMON_OPTIONAL_FIELDS,
                        FIELD_SHORT_TEXT,
                        FIELD_SHORT_TITLE,
                        FIELD_ICON,
                        FIELD_ICON_BURN_IN_PROTECTION,
                        FIELD_SMALL_IMAGE,
                        FIELD_SMALL_IMAGE_BURN_IN_PROTECTION,
                        FIELD_IMAGE_STYLE,
                    ),
                TYPE_NO_DATA to
                    setOf(
                        *COMMON_OPTIONAL_FIELDS,
                        FIELD_COLOR_RAMP,
                        FIELD_COLOR_RAMP_INTERPOLATED,
                        FIELD_ELEMENT_BACKGROUND_COLOR,
                        FIELD_ELEMENT_COLORS,
                        FIELD_ELEMENT_WEIGHTS,
                        FIELD_ICON,
                        FIELD_ICON_BURN_IN_PROTECTION,
                        FIELD_IMAGE_STYLE,
                        FIELD_LARGE_IMAGE,
                        FIELD_LONG_TITLE,
                        FIELD_LONG_TEXT,
                        FIELD_MAX_VALUE,
                        FIELD_MIN_VALUE,
                        FIELD_SMALL_IMAGE,
                        FIELD_SMALL_IMAGE_BURN_IN_PROTECTION,
                        FIELD_SHORT_TITLE,
                        FIELD_SHORT_TEXT,
                        FIELD_TAP_ACTION_LOST,
                        FIELD_TARGET_VALUE,
                        FIELD_VALUE,
                        FIELD_VALUE_EXPRESSION,
                        FIELD_VALUE_TYPE,
                        EXP_FIELD_LIST_ENTRIES,
                        EXP_FIELD_LIST_ENTRY_TYPE,
                        EXP_FIELD_LIST_STYLE_HINT,
                        EXP_FIELD_PROTO_LAYOUT_AMBIENT,
                        EXP_FIELD_PROTO_LAYOUT_INTERACTIVE,
                        EXP_FIELD_PROTO_LAYOUT_RESOURCES,
                    ),
                EXP_TYPE_PROTO_LAYOUT to
                    setOf(
                        *COMMON_OPTIONAL_FIELDS,
                    ),
                EXP_TYPE_LIST to
                    setOf(
                        *COMMON_OPTIONAL_FIELDS,
                        EXP_FIELD_LIST_STYLE_HINT,
                    ),
                TYPE_GOAL_PROGRESS to
                    setOf(
                        *COMMON_OPTIONAL_FIELDS,
                        FIELD_SHORT_TEXT,
                        FIELD_SHORT_TITLE,
                        FIELD_ICON,
                        FIELD_ICON_BURN_IN_PROTECTION,
                        FIELD_SMALL_IMAGE,
                        FIELD_SMALL_IMAGE_BURN_IN_PROTECTION,
                        FIELD_IMAGE_STYLE,
                        FIELD_COLOR_RAMP,
                        FIELD_COLOR_RAMP_INTERPOLATED,
                    ),
                TYPE_WEIGHTED_ELEMENTS to
                    setOf(
                        *COMMON_OPTIONAL_FIELDS,
                        FIELD_SHORT_TEXT,
                        FIELD_SHORT_TITLE,
                        FIELD_ICON,
                        FIELD_ICON_BURN_IN_PROTECTION,
                        FIELD_SMALL_IMAGE,
                        FIELD_SMALL_IMAGE_BURN_IN_PROTECTION,
                        FIELD_IMAGE_STYLE,
                    ),
            )

        @JvmField
        val CREATOR =
            object : Parcelable.Creator<ComplicationData> {
                override fun createFromParcel(source: Parcel) = ComplicationData(source)

                override fun newArray(size: Int): Array<ComplicationData?> = Array(size) { null }
            }

        fun isFieldValidForType(field: String, @ComplicationType type: Int): Boolean {
            return REQUIRED_FIELDS[type]!!.contains(field) ||
                REQUIRED_ONE_OF_FIELDS[type]!!.any { it.contains(field) } ||
                OPTIONAL_FIELDS[type]!!.contains(field)
        }

        private fun isTypeSupported(type: Int) = type in VALID_TYPES

        /**
         * The unparceling logic needs to remain backward compatible. Validates that a value of the
         * given field type can be assigned to the given complication type.
         */
        internal fun checkFieldValidForTypeWithoutThrowingException(
            fieldType: String,
            @ComplicationType complicationType: Int,
        ) {
            if (!isTypeSupported(complicationType)) {
                Log.w(TAG, "Type $complicationType can not be recognized")
                return
            }
            if (!isFieldValidForType(fieldType, complicationType)) {
                Log.d(TAG, "Field $fieldType is not supported for type $complicationType")
            }
        }

        internal fun checkFieldValidForType(field: String, @ComplicationType type: Int) {
            check(isTypeSupported(type)) { "Type $type can not be recognized" }
            check(isFieldValidForType(field, type)) {
                "Field $field is not supported for type $type"
            }
        }

        /** Returns whether or not we should redact complication data in toString(). */
        @JvmStatic fun shouldRedact() = !Log.isLoggable(TAG, Log.DEBUG)

        @JvmStatic
        fun maybeRedact(unredacted: CharSequence?): String =
            if (unredacted == null) "(null)" else maybeRedact(unredacted.toString())

        @JvmSynthetic
        private fun maybeRedact(unredacted: String): String =
            if (!shouldRedact() || unredacted == PLACEHOLDER_STRING) unredacted else "REDACTED"
    }
}

/** Writes a [ByteArray] by writing the size, then the bytes. To be used with [readByteArray]. */
internal fun ObjectOutputStream.writeByteArray(value: ByteArray) {
    writeInt(value.size)
    write(value)
}

/** Reads a [ByteArray] written with [writeByteArray]. */
internal fun ObjectInputStream.readByteArray() = ByteArray(readInt()).also { readFully(it) }

/** Writes an [IntArray] by writing the size, then the bytes. To be used with [readIntArray]. */
internal fun ObjectOutputStream.writeIntArray(value: IntArray) {
    writeInt(value.size)
    value.forEach(this::writeInt)
}

/** Reads an [IntArray] written with [writeIntArray]. */
internal fun ObjectInputStream.readIntArray() =
    IntArray(readInt()).also { for (i in it.indices) it[i] = readInt() }

/** Writes a [FloatArray] by writing the size, then the bytes. To be used with [readFloatArray]. */
internal fun ObjectOutputStream.writeFloatArray(value: FloatArray) {
    writeInt(value.size)
    value.forEach(this::writeFloat)
}

/** Reads a [FloatArray] written with [writeFloatArray]. */
internal fun ObjectInputStream.readFloatArray() =
    FloatArray(readInt()).also { for (i in it.indices) it[i] = readFloat() }

/** Writes a generic [List] by writing the size, then the objects. To be used with [readList]. */
internal fun <T> ObjectOutputStream.writeList(value: List<T>, writer: (T) -> Unit) {
    writeInt(value.size)
    value.forEach(writer)
}

/** Reads a list written with [readList]. */
internal fun <T> ObjectInputStream.readList(reader: () -> T) = List(readInt()) { reader() }

/**
 * Writes a nullable object by writing a boolean, then the object. To be used with [readNullable].
 */
internal fun <T> ObjectOutputStream.writeNullable(value: T?, writer: (T) -> Unit) {
    if (value != null) {
        writeBoolean(true)
        writer(value)
    } else {
        writeBoolean(false)
    }
}

/** Reads a nullable value written with [writeNullable]. */
internal fun <T> ObjectInputStream.readNullable(reader: () -> T): T? =
    if (readBoolean()) reader() else null
