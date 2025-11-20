package com.testdata.oneofserializable

import androidx.appfunctions.`internal`.AppFunctionInventory
import androidx.appfunctions.metadata.AppFunctionAllOfTypeMetadata
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionOneOfTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import androidx.appfunctions.metadata.AppFunctionUnitTypeMetadata
import androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata
import javax.`annotation`.processing.Generated
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map

/**
 * Source Files:
 * OneOfFunctions.....kt
 * OneOfSealedInterface.....kt
 * OneOfSealedClass.....kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$OneOfFunctions_AppFunctionInventory` : AppFunctionInventory {
  override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> = mapOf(
    "com.testdata.oneofserializable.OneOfFunctions#oneOfSealedParam" to ComTestdataOneofserializableOneOfFunctionsOneOfSealedParamMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.oneofserializable.OneOfFunctions#oneOfSealedReturn" to ComTestdataOneofserializableOneOfFunctionsOneOfSealedReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.oneofserializable.OneOfFunctions#oneOfSealedListReturn" to ComTestdataOneofserializableOneOfFunctionsOneOfSealedListReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.oneofserializable.OneOfFunctions#oneOfSealedNestedSerializable" to ComTestdataOneofserializableOneOfFunctionsOneOfSealedNestedSerializableMetadataObject.APP_FUNCTION_METADATA,
  )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ASUBCLASS_OBJECT_DATA_TYPE_PROPERTIES_MAP_INTERFACEPROPERTY:
      AppFunctionStringTypeMetadata = AppFunctionStringTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ASUBCLASS_OBJECT_DATA_TYPE_PROPERTIES_MAP_STR:
      AppFunctionStringTypeMetadata = AppFunctionStringTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ASUBCLASS_OBJECT_DATA_TYPE_PROPERTIES_MAP:
      Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "interfaceProperty" to COM_TESTDATA_ONEOFSERIALIZABLE_ASUBCLASS_OBJECT_DATA_TYPE_PROPERTIES_MAP_INTERFACEPROPERTY,
    "str" to COM_TESTDATA_ONEOFSERIALIZABLE_ASUBCLASS_OBJECT_DATA_TYPE_PROPERTIES_MAP_STR,
  )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ASUBCLASS_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
      List<String> = listOf(
    "interfaceProperty",
    "str",
  )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ASUBCLASS_OBJECT_DATA_TYPE:
      AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
          properties = COM_TESTDATA_ONEOFSERIALIZABLE_ASUBCLASS_OBJECT_DATA_TYPE_PROPERTIES_MAP,
          required = COM_TESTDATA_ONEOFSERIALIZABLE_ASUBCLASS_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
          qualifiedName = "com.testdata.oneofserializable.ASubclass",
          isNullable = true,
          description = ""
      )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_BSUBCLASS_OBJECT_DATA_TYPE_PROPERTIES_MAP_INTERFACEPROPERTY:
      AppFunctionStringTypeMetadata = AppFunctionStringTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_BSUBCLASS_OBJECT_DATA_TYPE_PROPERTIES_MAP_INTEGER:
      AppFunctionIntTypeMetadata = AppFunctionIntTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_BSUBCLASS_OBJECT_DATA_TYPE_PROPERTIES_MAP:
      Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "interfaceProperty" to COM_TESTDATA_ONEOFSERIALIZABLE_BSUBCLASS_OBJECT_DATA_TYPE_PROPERTIES_MAP_INTERFACEPROPERTY,
    "integer" to COM_TESTDATA_ONEOFSERIALIZABLE_BSUBCLASS_OBJECT_DATA_TYPE_PROPERTIES_MAP_INTEGER,
  )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_BSUBCLASS_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
      List<String> = listOf(
    "interfaceProperty",
    "integer",
  )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_BSUBCLASS_OBJECT_DATA_TYPE:
      AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
          properties = COM_TESTDATA_ONEOFSERIALIZABLE_BSUBCLASS_OBJECT_DATA_TYPE_PROPERTIES_MAP,
          required = COM_TESTDATA_ONEOFSERIALIZABLE_BSUBCLASS_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
          qualifiedName = "com.testdata.oneofserializable.BSubclass",
          isNullable = true,
          description = ""
      )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDINTERFACE_ONE_OF_DATA_TYPE_MATCH_ONE_OF_LIST_ITEM_0:
      AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
          referenceDataType = "com.testdata.oneofserializable.ASubclass",
          isNullable = true,
          description = ""
      )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDINTERFACE_ONE_OF_DATA_TYPE_MATCH_ONE_OF_LIST_ITEM_1:
      AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
          referenceDataType = "com.testdata.oneofserializable.BSubclass",
          isNullable = true,
          description = ""
      )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDINTERFACE_ONE_OF_DATA_TYPE_MATCH_ONE_OF_LIST:
      List<AppFunctionDataTypeMetadata> = listOf(
    COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDINTERFACE_ONE_OF_DATA_TYPE_MATCH_ONE_OF_LIST_ITEM_0,
    COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDINTERFACE_ONE_OF_DATA_TYPE_MATCH_ONE_OF_LIST_ITEM_1,
  )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDINTERFACE_ONE_OF_DATA_TYPE:
      AppFunctionOneOfTypeMetadata = AppFunctionOneOfTypeMetadata(
          matchOneOf = COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDINTERFACE_ONE_OF_DATA_TYPE_MATCH_ONE_OF_LIST,
          qualifiedName = "com.testdata.oneofserializable.OneOfSealedInterface",
          isNullable = true,
          description = ""
      )

  private val ANDROIDX_APPFUNCTIONS_APPFUNCTIONTEXTRESOURCE_OBJECT_DATA_TYPE_PROPERTIES_MAP_MIMETYPE:
      AppFunctionStringTypeMetadata = AppFunctionStringTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val ANDROIDX_APPFUNCTIONS_APPFUNCTIONTEXTRESOURCE_OBJECT_DATA_TYPE_PROPERTIES_MAP_CONTENT:
      AppFunctionStringTypeMetadata = AppFunctionStringTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val ANDROIDX_APPFUNCTIONS_APPFUNCTIONTEXTRESOURCE_OBJECT_DATA_TYPE_PROPERTIES_MAP:
      Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "mimeType" to ANDROIDX_APPFUNCTIONS_APPFUNCTIONTEXTRESOURCE_OBJECT_DATA_TYPE_PROPERTIES_MAP_MIMETYPE,
    "content" to ANDROIDX_APPFUNCTIONS_APPFUNCTIONTEXTRESOURCE_OBJECT_DATA_TYPE_PROPERTIES_MAP_CONTENT,
  )

  private val ANDROIDX_APPFUNCTIONS_APPFUNCTIONTEXTRESOURCE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
      List<String> = listOf(
    "mimeType",
    "content",
  )

  private val ANDROIDX_APPFUNCTIONS_APPFUNCTIONTEXTRESOURCE_OBJECT_DATA_TYPE:
      AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
          properties = ANDROIDX_APPFUNCTIONS_APPFUNCTIONTEXTRESOURCE_OBJECT_DATA_TYPE_PROPERTIES_MAP,
          required = ANDROIDX_APPFUNCTIONS_APPFUNCTIONTEXTRESOURCE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
          qualifiedName = "androidx.appfunctions.AppFunctionTextResource",
          isNullable = true,
          description = ""
      )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP_RESOURCES_REFERENCE_ITEM_TYPE:
      AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
          referenceDataType = "androidx.appfunctions.AppFunctionTextResource",
          isNullable = false,
          description = ""
      )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP_RESOURCES:
      AppFunctionArrayTypeMetadata = AppFunctionArrayTypeMetadata(
          itemType = COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP_RESOURCES_REFERENCE_ITEM_TYPE,
          isNullable = false,
          description = ""
      )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP:
      Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "resources" to COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP_RESOURCES,
  )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_REQUIRED_PROPERTIES_LIST:
      List<String> = listOf(
  )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0:
      AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
          properties = COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP,
          required = COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_REQUIRED_PROPERTIES_LIST,
          qualifiedName = "androidx.appfunctions.AppFunctionResourceContainer",
          isNullable = true,
          description = "I am a KDoc for OneOfSealedClass.ASubclass "
      )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_INTERFACEPROPERTY:
      AppFunctionStringTypeMetadata = AppFunctionStringTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_STR:
      AppFunctionStringTypeMetadata = AppFunctionStringTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP:
      Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "interfaceProperty" to COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_INTERFACEPROPERTY,
    "str" to COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_STR,
  )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_REQUIRED_PROPERTIES_LIST:
      List<String> = listOf(
    "interfaceProperty",
    "str",
  )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1:
      AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
          properties = COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP,
          required = COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_REQUIRED_PROPERTIES_LIST,
          qualifiedName = "com.testdata.oneofserializable.OneOfSealedClass${'$'}ASubclass",
          isNullable = true,
          description = "I am a KDoc for OneOfSealedClass.ASubclass "
      )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE_MATCH_ALL_LIST:
      List<AppFunctionDataTypeMetadata> = listOf(
    COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0,
    COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1,
  )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE:
      AppFunctionAllOfTypeMetadata = AppFunctionAllOfTypeMetadata(
          matchAll = COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE_MATCH_ALL_LIST,
          qualifiedName = "com.testdata.oneofserializable.OneOfSealedClass${'$'}ASubclass",
          isNullable = true,
          description = "I am a KDoc for OneOfSealedClass.ASubclass "
      )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_BSUBCLASS_OBJECT_DATA_TYPE_PROPERTIES_MAP_INTERFACEPROPERTY:
      AppFunctionStringTypeMetadata = AppFunctionStringTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_BSUBCLASS_OBJECT_DATA_TYPE_PROPERTIES_MAP_INTEGER:
      AppFunctionIntTypeMetadata = AppFunctionIntTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_BSUBCLASS_OBJECT_DATA_TYPE_PROPERTIES_MAP:
      Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "interfaceProperty" to COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_BSUBCLASS_OBJECT_DATA_TYPE_PROPERTIES_MAP_INTERFACEPROPERTY,
    "integer" to COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_BSUBCLASS_OBJECT_DATA_TYPE_PROPERTIES_MAP_INTEGER,
  )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_BSUBCLASS_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
      List<String> = listOf(
    "interfaceProperty",
    "integer",
  )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_BSUBCLASS_OBJECT_DATA_TYPE:
      AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
          properties = COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_BSUBCLASS_OBJECT_DATA_TYPE_PROPERTIES_MAP,
          required = COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_BSUBCLASS_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
          qualifiedName = "com.testdata.oneofserializable.OneOfSealedClass${'$'}BSubclass",
          isNullable = true,
          description = "I am a KDoc for OneOfSealedClass.BSubclass "
      )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ONE_OF_DATA_TYPE_MATCH_ONE_OF_LIST_ITEM_0:
      AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
          referenceDataType = "com.testdata.oneofserializable.OneOfSealedClass${'$'}ASubclass",
          isNullable = true,
          description = ""
      )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ONE_OF_DATA_TYPE_MATCH_ONE_OF_LIST_ITEM_1:
      AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
          referenceDataType = "com.testdata.oneofserializable.OneOfSealedClass${'$'}BSubclass",
          isNullable = true,
          description = ""
      )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ONE_OF_DATA_TYPE_MATCH_ONE_OF_LIST:
      List<AppFunctionDataTypeMetadata> = listOf(
    COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ONE_OF_DATA_TYPE_MATCH_ONE_OF_LIST_ITEM_0,
    COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ONE_OF_DATA_TYPE_MATCH_ONE_OF_LIST_ITEM_1,
  )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ONE_OF_DATA_TYPE:
      AppFunctionOneOfTypeMetadata = AppFunctionOneOfTypeMetadata(
          matchOneOf = COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ONE_OF_DATA_TYPE_MATCH_ONE_OF_LIST,
          qualifiedName = "com.testdata.oneofserializable.OneOfSealedClass",
          isNullable = true,
          description = "I am a KDoc for OneOfSealedClass "
      )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDNESTEDSERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_SEALEDINTERFACE:
      AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
          referenceDataType = "com.testdata.oneofserializable.OneOfSealedInterface",
          isNullable = false,
          description = ""
      )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDNESTEDSERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP:
      Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "sealedInterface" to COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDNESTEDSERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_SEALEDINTERFACE,
  )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDNESTEDSERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
      List<String> = listOf(
    "sealedInterface",
  )

  private val COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDNESTEDSERIALIZABLE_OBJECT_DATA_TYPE:
      AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
          properties = COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDNESTEDSERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP,
          required = COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDNESTEDSERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
          qualifiedName = "com.testdata.oneofserializable.OneOfSealedNestedSerializable",
          isNullable = true,
          description = ""
      )

  private val componentsMetadataDataTypesMap: Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "com.testdata.oneofserializable.ASubclass" to COM_TESTDATA_ONEOFSERIALIZABLE_ASUBCLASS_OBJECT_DATA_TYPE,
    "com.testdata.oneofserializable.BSubclass" to COM_TESTDATA_ONEOFSERIALIZABLE_BSUBCLASS_OBJECT_DATA_TYPE,
    "com.testdata.oneofserializable.OneOfSealedInterface" to COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDINTERFACE_ONE_OF_DATA_TYPE,
    "androidx.appfunctions.AppFunctionTextResource" to ANDROIDX_APPFUNCTIONS_APPFUNCTIONTEXTRESOURCE_OBJECT_DATA_TYPE,
    "com.testdata.oneofserializable.OneOfSealedClass${'$'}ASubclass" to COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ASUBCLASS_ALL_OF_DATA_TYPE,
    "com.testdata.oneofserializable.OneOfSealedClass${'$'}BSubclass" to COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_BSUBCLASS_OBJECT_DATA_TYPE,
    "com.testdata.oneofserializable.OneOfSealedClass" to COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDCLASS_ONE_OF_DATA_TYPE,
    "com.testdata.oneofserializable.OneOfSealedNestedSerializable" to COM_TESTDATA_ONEOFSERIALIZABLE_ONEOFSEALEDNESTEDSERIALIZABLE_OBJECT_DATA_TYPE,
    )

  override val componentsMetadata: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
          dataTypes = componentsMetadataDataTypesMap
      )

  private object ComTestdataOneofserializableOneOfFunctionsOneOfSealedParamMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_ONEOFSEALEDINTERFACE_REFERENCE_DATA_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.oneofserializable.OneOfSealedInterface",
            isNullable = false,
            description = ""
        )

    private val ONEOFSEALEDINTERFACE_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "oneOfSealedInterface",
            isRequired = true,
            dataType = PARAMETER_METADATA_ONEOFSEALEDINTERFACE_REFERENCE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_ONEOFSEALEDINTERFACELIST_ARRAY_DATA_TYPE_REFERENCE_ITEM_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.oneofserializable.OneOfSealedInterface",
            isNullable = false,
            description = ""
        )

    private val PARAMETER_METADATA_ONEOFSEALEDINTERFACELIST_ARRAY_DATA_TYPE:
        AppFunctionArrayTypeMetadata = AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_ONEOFSEALEDINTERFACELIST_ARRAY_DATA_TYPE_REFERENCE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val ONEOFSEALEDINTERFACELIST_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "oneOfSealedInterfaceList",
            isRequired = true,
            dataType = PARAMETER_METADATA_ONEOFSEALEDINTERFACELIST_ARRAY_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      ONEOFSEALEDINTERFACE_PARAMETER_METADATA,
      ONEOFSEALEDINTERFACELIST_PARAMETER_METADATA,
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionUnitTypeMetadata =
        AppFunctionUnitTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.oneofserializable.OneOfFunctions#oneOfSealedParam",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataOneofserializableOneOfFunctionsOneOfSealedReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val REFERENCE_RESPONSE_VALUE_TYPE: AppFunctionReferenceTypeMetadata =
        AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.oneofserializable.OneOfSealedClass",
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = REFERENCE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.oneofserializable.OneOfFunctions#oneOfSealedReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataOneofserializableOneOfFunctionsOneOfSealedListReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_REFERENCE_ITEM_TYPE: AppFunctionReferenceTypeMetadata =
        AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.oneofserializable.OneOfSealedClass",
            isNullable = false,
            description = ""
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_REFERENCE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.oneofserializable.OneOfFunctions#oneOfSealedListReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataOneofserializableOneOfFunctionsOneOfSealedNestedSerializableMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_ONEOFSEALEDNESTEDSERIALIZABLE_REFERENCE_DATA_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.oneofserializable.OneOfSealedNestedSerializable",
            isNullable = false,
            description = ""
        )

    private val ONEOFSEALEDNESTEDSERIALIZABLE_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "oneOfSealedNestedSerializable",
            isRequired = true,
            dataType = PARAMETER_METADATA_ONEOFSEALEDNESTEDSERIALIZABLE_REFERENCE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      ONEOFSEALEDNESTEDSERIALIZABLE_PARAMETER_METADATA,
    )

    private val REFERENCE_RESPONSE_VALUE_TYPE: AppFunctionReferenceTypeMetadata =
        AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.oneofserializable.OneOfSealedNestedSerializable",
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = REFERENCE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.oneofserializable.OneOfFunctions#oneOfSealedNestedSerializable",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }
}
