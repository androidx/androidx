package com.testdata

import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata
import androidx.appfunctions.service.`internal`.AppFunctionInventory
import javax.`annotation`.processing.Generated
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map

/**
 * Source Files:
 * AppFunctionWithInputFromDifferentPackage.....kt
 * DiffPackageSchemas.....kt
 * DiffPackageSerializable.....kt
 * AnotherDiffPackageSerializable.....kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$AppFunctionWithInputFromDifferentPackage_AppFunctionInventory` : AppFunctionInventory {
  override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> = mapOf(
    "com.testdata.AppFunctionWithInputFromDifferentPackage#functionWithDiffPackageInput" to ComTestdataAppFunctionWithInputFromDifferentPackageFunctionWithDiffPackageInputMetadataObject.APP_FUNCTION_METADATA,
  )

  private object ComTestdataAppFunctionWithInputFromDifferentPackageFunctionWithDiffPackageInputMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "diff_package_schemas_category", name="diffPackageInputSchemas", version=1)

    private val PARAMETER_METADATA_INPUT_REFERENCE_DATA_TYPE: AppFunctionReferenceTypeMetadata =
        AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.differentPackage.DiffPackageSerializable",
            isNullable = false,
            description = ""
        )

    private val INPUT_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "input",
            isRequired = true,
            dataType = PARAMETER_METADATA_INPUT_REFERENCE_DATA_TYPE
        )

    private val PARAMETER_METADATA_INPUTLIST_ARRAY_DATA_TYPE_REFERENCE_ITEM_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.anotherDifferentPackage.AnotherDiffPackageSerializable",
            isNullable = false,
            description = ""
        )

    private val PARAMETER_METADATA_INPUTLIST_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_INPUTLIST_ARRAY_DATA_TYPE_REFERENCE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val INPUTLIST_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "inputList",
            isRequired = true,
            dataType = PARAMETER_METADATA_INPUTLIST_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      INPUT_PARAMETER_METADATA,
      INPUTLIST_PARAMETER_METADATA,
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 0,
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE
        )

    private val COM_TESTDATA_DIFFERENTPACKAGE_DIFFPACKAGESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LONGPARAM:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 6,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_DIFFERENTPACKAGE_DIFFPACKAGESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "longParam" to COM_TESTDATA_DIFFERENTPACKAGE_DIFFPACKAGESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LONGPARAM,
    )

    private val COM_TESTDATA_DIFFERENTPACKAGE_DIFFPACKAGESERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "longParam",
    )

    private val COM_TESTDATA_DIFFERENTPACKAGE_DIFFPACKAGESERIALIZABLE_OBJECT_DATA_TYPE:
        AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_DIFFERENTPACKAGE_DIFFPACKAGESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP,
            required = COM_TESTDATA_DIFFERENTPACKAGE_DIFFPACKAGESERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.differentPackage.DiffPackageSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_ANOTHERDIFFERENTPACKAGE_ANOTHERDIFFPACKAGESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_STRINGPARAM:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_ANOTHERDIFFERENTPACKAGE_ANOTHERDIFFPACKAGESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "stringParam" to COM_TESTDATA_ANOTHERDIFFERENTPACKAGE_ANOTHERDIFFPACKAGESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_STRINGPARAM,
    )

    private val COM_TESTDATA_ANOTHERDIFFERENTPACKAGE_ANOTHERDIFFPACKAGESERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "stringParam",
    )

    private val COM_TESTDATA_ANOTHERDIFFERENTPACKAGE_ANOTHERDIFFPACKAGESERIALIZABLE_OBJECT_DATA_TYPE:
        AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_ANOTHERDIFFERENTPACKAGE_ANOTHERDIFFPACKAGESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP,
            required = COM_TESTDATA_ANOTHERDIFFERENTPACKAGE_ANOTHERDIFFPACKAGESERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.anotherDifferentPackage.AnotherDiffPackageSerializable",
            isNullable = true,
            description = ""
        )

    private val COMPONENTS_METADATA_DATA_TYPES_MAP: Map<String, AppFunctionDataTypeMetadata> =
        mapOf(
      "com.testdata.differentPackage.DiffPackageSerializable" to COM_TESTDATA_DIFFERENTPACKAGE_DIFFPACKAGESERIALIZABLE_OBJECT_DATA_TYPE,
      "com.testdata.anotherDifferentPackage.AnotherDiffPackageSerializable" to COM_TESTDATA_ANOTHERDIFFERENTPACKAGE_ANOTHERDIFFPACKAGESERIALIZABLE_OBJECT_DATA_TYPE,
      )

    private val COMPONENTS_METADATA: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
            dataTypes = COMPONENTS_METADATA_DATA_TYPES_MAP
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.AppFunctionWithInputFromDifferentPackage#functionWithDiffPackageInput",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }
}
