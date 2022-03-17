/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.watchface

import android.content.res.Resources
import android.content.res.XmlResourceParser
import androidx.annotation.RestrictTo
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleData
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.data.UserStyleFlavorWireFormat
import androidx.wear.watchface.style.data.UserStyleFlavorsWireFormat
import java.io.IOException
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

/**
 * Represents user specified preset of watch face.
 *
 * @param id An arbitrary string that uniquely identifies a flavor within the set of flavors
 * supported by the watch face.
 * @param style Style info of the flavor represented by [UserStyleData].
 * @param complications Specifies complication data source policy represented by
 * [DefaultComplicationDataSourcePolicy] for each [ComplicationSlot.id] presented in map. For
 * absent complication slots default policies are used.
 */
@WatchFaceFlavorsExperimental
public class UserStyleFlavor(
    public val id: String,
    public val style: UserStyleData,
    public val complications: Map<Int, DefaultComplicationDataSourcePolicy>
) {
    /**
     * Constructs UserStyleFlavor based on [UserStyle] specified.
     */
    constructor(
        id: String,
        style: UserStyle,
        complications: Map<Int, DefaultComplicationDataSourcePolicy>
    ) : this(id, style.toUserStyleData(), complications) {}

    /** @hide */
    @Suppress("ShowingMemberInHiddenClass")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    constructor(wireFormat: UserStyleFlavorWireFormat) : this(
        wireFormat.mId,
        UserStyleData(wireFormat.mStyle),
        wireFormat.mComplications.mapValues { DefaultComplicationDataSourcePolicy(it.value) }
    ) {
    }

    /** @hide */
    @Suppress("ShowingMemberInHiddenClass")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    fun toWireFormat() =
        UserStyleFlavorWireFormat(
            id,
            style.toWireFormat(),
            complications.mapValues { it.value.toWireFormat() }
        )

    override fun toString(): String =
        "UserStyleFlavor[$id: $style, $complications]"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserStyleFlavor

        if (id != other.id) return false
        if (style != other.style) return false
        if (complications != other.complications) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + complications.hashCode()
        return result
    }

    internal companion object {
        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Throws(IOException::class, XmlPullParserException::class)
        fun inflate(
            resources: Resources,
            parser: XmlResourceParser,
            schema: UserStyleSchema
        ): UserStyleFlavor {
            require(parser.name == "UserStyleFlavor") {
                "Expected a UserStyleFlavor node"
            }

            val flavorAttributes = resources.obtainAttributes(parser, R.styleable.UserStyleFlavor)
            val flavorId = flavorAttributes.getString(R.styleable.UserStyleFlavor_id)
            require(flavorId != null) { "UserStyleFlavor must have an id" }

            val userStyle = schema.getDefaultUserStyle().toMutableUserStyle()
            val complications = mutableMapOf<Int, DefaultComplicationDataSourcePolicy>()
            val outerDepth = parser.depth
            var type = parser.next()

            // Parse the UserStyle declaration.
            do {
                if (type == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "StyleOption" -> {
                            val attributes = resources.obtainAttributes(
                                parser,
                                R.styleable.StyleOption
                            )
                            val id = attributes.getString(R.styleable.StyleOption_id)
                            require(id != null) { "StyleOption must have an id" }

                            require(attributes.hasValue(R.styleable.StyleOption_value)) {
                                "value is required for BooleanOption"
                            }
                            val value = attributes.getString(R.styleable.StyleOption_value)

                            val setting = schema[UserStyleSetting.Id(id)]
                            require(setting != null) { "no setting found for id $id" }
                            when (setting) {
                                is UserStyleSetting.BooleanUserStyleSetting -> {
                                    val booleanValue = attributes.getBoolean(
                                        R.styleable.StyleOption_value,
                                        true
                                    )

                                    userStyle[setting] =
                                        UserStyleSetting.BooleanUserStyleSetting
                                            .BooleanOption.from(booleanValue)
                                }
                                is UserStyleSetting.DoubleRangeUserStyleSetting -> {
                                    userStyle[setting] =
                                        UserStyleSetting.DoubleRangeUserStyleSetting
                                            .DoubleRangeOption(value!!.toDouble())
                                }
                                is UserStyleSetting.LongRangeUserStyleSetting -> {
                                    userStyle[setting] =
                                        UserStyleSetting.LongRangeUserStyleSetting
                                            .LongRangeOption(value!!.toLong())
                                }
                                else -> {
                                    userStyle[setting] = setting.getOptionForId(
                                        UserStyleSetting.Option.Id(value!!))
                                }
                            }
                        }
                        "ComplicationPolicy" -> {
                            val attributes = resources.obtainAttributes(
                                parser,
                                R.styleable.ComplicationPolicy
                            )
                            require(attributes.hasValue(R.styleable.ComplicationPolicy_slotId)) {
                                "id is required for ComplicationPolicy"
                            }
                            val id = attributes.getInt(R.styleable.ComplicationPolicy_slotId, 0)

                            val policy =
                                XmlSchemaAndComplicationSlotsDefinition.ComplicationSlotStaticData
                                .inflateDefaultComplicationDataSourcePolicy(
                                    attributes,
                                    R.styleable.ComplicationPolicy_primaryDataSource,
                                    R.styleable.ComplicationPolicy_primaryDataSourceDefaultType,
                                    R.styleable.ComplicationPolicy_secondaryDataSource,
                                    R.styleable.ComplicationPolicy_secondaryDataSourceDefaultType,
                                    R.styleable.ComplicationPolicy_systemDataSourceFallback,
                                    R.styleable
                                        .ComplicationPolicy_systemDataSourceFallbackDefaultType
                                )

                            complications[id] = policy
                        }

                        else -> throw IllegalArgumentException(
                            "Unexpected node ${parser.name} at line ${parser.lineNumber}"
                        )
                    }
                }
                type = parser.next()
            } while (type != XmlPullParser.END_DOCUMENT && parser.depth > outerDepth)

            return UserStyleFlavor(
                flavorId,
                userStyle.toUserStyle().toUserStyleData(),
                complications.toMap())
        }
    }
}

/**
 * Collection of watch face flavors, represented by [UserStyleFlavor] class.
 *
 * @param flavors List of flavors.
 */
@WatchFaceFlavorsExperimental
public class UserStyleFlavors(public val flavors: List<UserStyleFlavor>) {
    /**
     * Constructs empty flavors collection.
     */
    constructor() : this(emptyList()) {}

    /** @hide */
    @Suppress("ShowingMemberInHiddenClass")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    constructor(wireFormat: UserStyleFlavorsWireFormat) : this(
        wireFormat.mFlavors.map { UserStyleFlavor(it) }
    ) {
    }

    /** @hide */
    @Suppress("ShowingMemberInHiddenClass")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    fun toWireFormat() =
        UserStyleFlavorsWireFormat(flavors.map { it.toWireFormat() })

    override fun toString(): String = "$flavors"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserStyleFlavors

        if (flavors != other.flavors) return false

        return true
    }

    override fun hashCode(): Int {
        return flavors.hashCode()
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        @Throws(IOException::class, XmlPullParserException::class)
        fun inflate(
            resources: Resources,
            parser: XmlResourceParser,
            schema: UserStyleSchema
        ): UserStyleFlavors {
            require(parser.name == "UserStyleFlavors") {
                "Expected a UserStyleFlavors node"
            }

            val flavors = ArrayList<UserStyleFlavor>()
            val outerDepth = parser.depth
            var type = parser.next()

            // Parse the UserStyle declaration.
            do {
                if (type == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "UserStyleFlavor" -> flavors.add(
                            UserStyleFlavor.inflate(resources, parser, schema)
                        )

                        else -> throw IllegalArgumentException(
                            "Unexpected node ${parser.name} at line ${parser.lineNumber}"
                        )
                    }
                }
                type = parser.next()
            } while (type != XmlPullParser.END_DOCUMENT && parser.depth > outerDepth)

            return UserStyleFlavors(flavors)
        }
    }
}