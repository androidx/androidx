/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.a2ui.compose.ui.catalog

import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.model.schema.A2uiAnySchema
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchemaKeyword
import androidx.a2ui.model.schema.A2uiStringSchema
import androidx.a2ui.model.schema.commontypes.A2uiAccessibilityAttributesSchema
import androidx.a2ui.model.schema.commontypes.A2uiDataBindingSchema
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiBasicCatalogV1IconTest {

    @Test
    fun interfaceDefaults_haveExpectedValues() {
        val iconComponent =
            object : A2uiBasicCatalogV1.Icon {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    source: A2uiBasicCatalogV1.Icon.Source,
                    accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
                    modifier: Modifier,
                ) {}
            }

        assertThat(iconComponent.name).isEqualTo("Icon")
        assertThat(iconComponent.description)
            .isEqualTo("Displays an icon from a predefined set of icons or an SVG path.")
        assertThat(iconComponent.properties)
            .containsExactly(
                A2uiBasicCatalogV1.WeightProperty,
                A2uiBasicCatalogV1.Icon.NameProperty,
                A2uiBasicCatalogV1.Icon.AccessibilityProperty,
            )
            .inOrder()
    }

    @Test
    fun companionProperties_haveExpectedSchema() {
        assertThat(A2uiBasicCatalogV1.Icon.NameProperty.key).isEqualTo("name")
        assertThat(A2uiBasicCatalogV1.Icon.NameProperty.isRequired).isTrue()
        val schema = assertIs<A2uiAnySchema>(A2uiBasicCatalogV1.Icon.NameProperty.schema)
        assertThat(schema.description).isEqualTo("The name of the icon to display.")
        assertThat(schema.keywords)
            .contains(
                A2uiSchemaKeyword.OneOf(
                    listOf(
                        A2uiStringSchema(
                            keywords =
                                listOf(
                                    A2uiSchemaKeyword.Enum(
                                        A2uiBasicCatalogV1.Icon.BuiltIn.entries.map { it.value }
                                    )
                                )
                        ),
                        A2uiObjectSchema(
                            properties = mapOf("svgPath" to A2uiStringSchema.INSTANCE),
                            required = setOf("svgPath"),
                            isAdditionalPropertiesAllowed = false,
                        ),
                        A2uiDataBindingSchema.DEFAULT_INSTANCE,
                    )
                )
            )

        assertThat(A2uiBasicCatalogV1.Icon.AccessibilityProperty.key).isEqualTo("accessibility")
        assertThat(A2uiBasicCatalogV1.Icon.AccessibilityProperty.isRequired).isFalse()
        assertThat(A2uiBasicCatalogV1.Icon.AccessibilityProperty.schema)
            .isEqualTo(A2uiAccessibilityAttributesSchema.DEFAULT_INSTANCE)
    }

    @Test
    fun source_subtypes_arePolymorphicAndDoNotEqualEachOther() {
        val builtIn: A2uiBasicCatalogV1.Icon.Source = A2uiBasicCatalogV1.Icon.BuiltIn.Add
        val svgPath: A2uiBasicCatalogV1.Icon.Source = A2uiBasicCatalogV1.Icon.SvgPath("add")
        val unrecognized: A2uiBasicCatalogV1.Icon.Source =
            A2uiBasicCatalogV1.Icon.Unrecognized("add")

        assertIs<A2uiBasicCatalogV1.Icon.BuiltIn>(builtIn)
        assertIs<A2uiBasicCatalogV1.Icon.SvgPath>(svgPath)
        assertIs<A2uiBasicCatalogV1.Icon.Unrecognized>(unrecognized)

        assertThat(builtIn).isNotEqualTo(svgPath)
        assertThat(builtIn).isNotEqualTo(unrecognized)
        assertThat(svgPath).isNotEqualTo(unrecognized)
    }

    @Test
    fun builtIn_values_matchSpecificationStrings() {
        val expected =
            mapOf(
                A2uiBasicCatalogV1.Icon.BuiltIn.AccountCircle to "accountCircle",
                A2uiBasicCatalogV1.Icon.BuiltIn.Add to "add",
                A2uiBasicCatalogV1.Icon.BuiltIn.ArrowBack to "arrowBack",
                A2uiBasicCatalogV1.Icon.BuiltIn.ArrowForward to "arrowForward",
                A2uiBasicCatalogV1.Icon.BuiltIn.AttachFile to "attachFile",
                A2uiBasicCatalogV1.Icon.BuiltIn.CalendarToday to "calendarToday",
                A2uiBasicCatalogV1.Icon.BuiltIn.Call to "call",
                A2uiBasicCatalogV1.Icon.BuiltIn.Camera to "camera",
                A2uiBasicCatalogV1.Icon.BuiltIn.Check to "check",
                A2uiBasicCatalogV1.Icon.BuiltIn.Close to "close",
                A2uiBasicCatalogV1.Icon.BuiltIn.Delete to "delete",
                A2uiBasicCatalogV1.Icon.BuiltIn.Download to "download",
                A2uiBasicCatalogV1.Icon.BuiltIn.Edit to "edit",
                A2uiBasicCatalogV1.Icon.BuiltIn.Error to "error",
                A2uiBasicCatalogV1.Icon.BuiltIn.Event to "event",
                A2uiBasicCatalogV1.Icon.BuiltIn.FastForward to "fastForward",
                A2uiBasicCatalogV1.Icon.BuiltIn.Favorite to "favorite",
                A2uiBasicCatalogV1.Icon.BuiltIn.FavoriteOff to "favoriteOff",
                A2uiBasicCatalogV1.Icon.BuiltIn.Folder to "folder",
                A2uiBasicCatalogV1.Icon.BuiltIn.Help to "help",
                A2uiBasicCatalogV1.Icon.BuiltIn.Home to "home",
                A2uiBasicCatalogV1.Icon.BuiltIn.Info to "info",
                A2uiBasicCatalogV1.Icon.BuiltIn.LocationOn to "locationOn",
                A2uiBasicCatalogV1.Icon.BuiltIn.Lock to "lock",
                A2uiBasicCatalogV1.Icon.BuiltIn.LockOpen to "lockOpen",
                A2uiBasicCatalogV1.Icon.BuiltIn.Mail to "mail",
                A2uiBasicCatalogV1.Icon.BuiltIn.Menu to "menu",
                A2uiBasicCatalogV1.Icon.BuiltIn.MoreHoriz to "moreHoriz",
                A2uiBasicCatalogV1.Icon.BuiltIn.MoreVert to "moreVert",
                A2uiBasicCatalogV1.Icon.BuiltIn.Notifications to "notifications",
                A2uiBasicCatalogV1.Icon.BuiltIn.NotificationsOff to "notificationsOff",
                A2uiBasicCatalogV1.Icon.BuiltIn.Pause to "pause",
                A2uiBasicCatalogV1.Icon.BuiltIn.Payment to "payment",
                A2uiBasicCatalogV1.Icon.BuiltIn.Person to "person",
                A2uiBasicCatalogV1.Icon.BuiltIn.Phone to "phone",
                A2uiBasicCatalogV1.Icon.BuiltIn.Photo to "photo",
                A2uiBasicCatalogV1.Icon.BuiltIn.Play to "play",
                A2uiBasicCatalogV1.Icon.BuiltIn.Print to "print",
                A2uiBasicCatalogV1.Icon.BuiltIn.Refresh to "refresh",
                A2uiBasicCatalogV1.Icon.BuiltIn.Rewind to "rewind",
                A2uiBasicCatalogV1.Icon.BuiltIn.Search to "search",
                A2uiBasicCatalogV1.Icon.BuiltIn.Send to "send",
                A2uiBasicCatalogV1.Icon.BuiltIn.Settings to "settings",
                A2uiBasicCatalogV1.Icon.BuiltIn.Share to "share",
                A2uiBasicCatalogV1.Icon.BuiltIn.ShoppingCart to "shoppingCart",
                A2uiBasicCatalogV1.Icon.BuiltIn.SkipNext to "skipNext",
                A2uiBasicCatalogV1.Icon.BuiltIn.SkipPrevious to "skipPrevious",
                A2uiBasicCatalogV1.Icon.BuiltIn.Star to "star",
                A2uiBasicCatalogV1.Icon.BuiltIn.StarHalf to "starHalf",
                A2uiBasicCatalogV1.Icon.BuiltIn.StarOff to "starOff",
                A2uiBasicCatalogV1.Icon.BuiltIn.Stop to "stop",
                A2uiBasicCatalogV1.Icon.BuiltIn.Upload to "upload",
                A2uiBasicCatalogV1.Icon.BuiltIn.Visibility to "visibility",
                A2uiBasicCatalogV1.Icon.BuiltIn.VisibilityOff to "visibilityOff",
                A2uiBasicCatalogV1.Icon.BuiltIn.VolumeDown to "volumeDown",
                A2uiBasicCatalogV1.Icon.BuiltIn.VolumeMute to "volumeMute",
                A2uiBasicCatalogV1.Icon.BuiltIn.VolumeOff to "volumeOff",
                A2uiBasicCatalogV1.Icon.BuiltIn.VolumeUp to "volumeUp",
                A2uiBasicCatalogV1.Icon.BuiltIn.Warning to "warning",
            )

        assertThat(A2uiBasicCatalogV1.Icon.BuiltIn.entries).containsExactlyElementsIn(expected.keys)
        for ((enumValue, expectedString) in expected) {
            assertThat(enumValue.value).isEqualTo(expectedString)
        }
    }

    @Test
    fun builtIn_entries_haveUniqueValuesAndValidFormat() {
        val values = A2uiBasicCatalogV1.Icon.BuiltIn.entries.map { it.value }

        assertThat(values).hasSize(59)
        assertThat(values.distinct()).hasSize(59)

        val camelCaseRegex = Regex("^[a-z][a-zA-Z0-9]*$")
        for (entry in A2uiBasicCatalogV1.Icon.BuiltIn.entries) {
            assertThat(entry.value).matches(camelCaseRegex.toPattern())
        }
    }

    @Test
    fun builtIn_fromValue_validStrings_returnsCorrespondingBuiltIn() {
        for (entry in A2uiBasicCatalogV1.Icon.BuiltIn.entries) {
            assertThat(A2uiBasicCatalogV1.Icon.BuiltIn.fromValue(entry.value)).isEqualTo(entry)
        }
    }

    @Test
    fun builtIn_fromValue_invalidOrEmptyString_returnsNull() {
        assertThat(A2uiBasicCatalogV1.Icon.BuiltIn.fromValue("nonexistentIcon")).isNull()
        assertThat(A2uiBasicCatalogV1.Icon.BuiltIn.fromValue("")).isNull()
        assertThat(A2uiBasicCatalogV1.Icon.BuiltIn.fromValue("ACCOUNT_CIRCLE")).isNull()
        assertThat(A2uiBasicCatalogV1.Icon.BuiltIn.fromValue("account_circle")).isNull()
    }

    @Test
    fun svgPath_propertiesAndEquality() {
        val path1 = A2uiBasicCatalogV1.Icon.SvgPath("M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z")
        val path2 = A2uiBasicCatalogV1.Icon.SvgPath("M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z")
        val path3 = A2uiBasicCatalogV1.Icon.SvgPath("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10")
        val emptyPath = A2uiBasicCatalogV1.Icon.SvgPath("")

        assertThat(path1.svgPath).isEqualTo("M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z")
        assertThat(emptyPath.svgPath).isEmpty()

        assertThat(path1).isEqualTo(path2)
        assertThat(path1.hashCode()).isEqualTo(path2.hashCode())
        assertThat(path1).isNotEqualTo(path3)
        assertThat(path1).isNotEqualTo(emptyPath)
        assertThat(path1).isNotEqualTo("M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z")

        assertThat(path1.toString())
            .isEqualTo("SvgPath(svgPath='M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z')")
    }

    @Test
    fun svgPath_specialCharactersAndNullHandling() {
        val complexPath = "M 10 80 Q 52.5 10, 95 80 T 180 80\nZ"
        val svgPath = A2uiBasicCatalogV1.Icon.SvgPath(complexPath)
        assertThat(svgPath.svgPath).isEqualTo(complexPath)
        assertThat(svgPath).isNotEqualTo(null)
    }

    @Test
    fun unrecognized_propertiesAndEquality() {
        val unrec1 = A2uiBasicCatalogV1.Icon.Unrecognized("unknown_icon")
        val unrec2 = A2uiBasicCatalogV1.Icon.Unrecognized("unknown_icon")
        val unrec3 = A2uiBasicCatalogV1.Icon.Unrecognized("other_icon")
        val emptyUnrec = A2uiBasicCatalogV1.Icon.Unrecognized("")

        assertThat(unrec1.name).isEqualTo("unknown_icon")
        assertThat(emptyUnrec.name).isEmpty()

        assertThat(unrec1).isEqualTo(unrec2)
        assertThat(unrec1.hashCode()).isEqualTo(unrec2.hashCode())
        assertThat(unrec1).isNotEqualTo(unrec3)
        assertThat(unrec1).isNotEqualTo(emptyUnrec)
        assertThat(unrec1).isNotEqualTo("unknown_icon")

        assertThat(unrec1.toString()).isEqualTo("Unrecognized(name='unknown_icon')")
    }

    @Test
    fun unrecognized_specialCharactersAndNullHandling() {
        val customToken = "custom-icon_name 🚀"
        val unrecognized = A2uiBasicCatalogV1.Icon.Unrecognized(customToken)
        assertThat(unrecognized.name).isEqualTo(customToken)
        assertThat(unrecognized).isNotEqualTo(null)
        assertThat(unrecognized.toString()).isEqualTo("Unrecognized(name='custom-icon_name 🚀')")
    }

    @Test
    fun accessibilityAttributes_propertiesAndEquality() {
        val attr1 =
            A2uiBasicCatalogV1.AccessibilityAttributes(label = "Label", description = "Desc")
        val attr2 =
            A2uiBasicCatalogV1.AccessibilityAttributes(label = "Label", description = "Desc")
        val attr3 =
            A2uiBasicCatalogV1.AccessibilityAttributes(label = "Other", description = "Desc")
        val emptyAttr = A2uiBasicCatalogV1.AccessibilityAttributes()

        assertThat(attr1.label).isEqualTo("Label")
        assertThat(attr1.description).isEqualTo("Desc")
        assertThat(emptyAttr.label).isNull()
        assertThat(emptyAttr.description).isNull()

        assertThat(attr1).isEqualTo(attr2)
        assertThat(attr1.hashCode()).isEqualTo(attr2.hashCode())
        assertThat(emptyAttr.hashCode()).isEqualTo(0)
        assertThat(attr1).isNotEqualTo(attr3)
        assertThat(attr1).isNotEqualTo(emptyAttr)
        assertThat(attr1).isNotEqualTo("Label")

        assertThat(attr1.toString())
            .isEqualTo("AccessibilityAttributes(label=Label, description=Desc)")
    }

    @Test
    fun accessibilityAttributes_partialPropertiesAndInequality() {
        val labelOnly = A2uiBasicCatalogV1.AccessibilityAttributes(label = "Label")
        val descOnly = A2uiBasicCatalogV1.AccessibilityAttributes(description = "Desc")
        val full1 =
            A2uiBasicCatalogV1.AccessibilityAttributes(label = "Label", description = "Desc1")
        val full2 =
            A2uiBasicCatalogV1.AccessibilityAttributes(label = "Label", description = "Desc2")

        assertThat(labelOnly.label).isEqualTo("Label")
        assertThat(labelOnly.description).isNull()
        assertThat(descOnly.label).isNull()
        assertThat(descOnly.description).isEqualTo("Desc")

        assertThat(full1).isNotEqualTo(full2)
        assertThat(labelOnly).isNotEqualTo(descOnly)
        assertThat(labelOnly).isNotEqualTo(null)
        assertThat(labelOnly).isNotEqualTo("Label")
        assertThat(labelOnly.hashCode()).isNotEqualTo(descOnly.hashCode())
    }
}
