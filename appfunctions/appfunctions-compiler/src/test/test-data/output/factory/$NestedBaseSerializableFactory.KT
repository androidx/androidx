package com.testdata

import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionDataSpec
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$NestedBaseSerializableFactory` : AppFunctionSerializableFactory<NestedBaseSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): NestedBaseSerializable {
    val appFunctionDataWithSpec = getAppFunctionDataWithSpec(appFunctionData = appFunctionData, qualifiedName = "com.testdata.NestedBaseSerializable")
    val nestedBaseSerializableFactory = `$NestedBaseSerializableFactory`()

    val stringBaseName = checkNotNull(appFunctionDataWithSpec.getStringOrNull("stringBaseName"))
    val nestedBaseData = appFunctionDataWithSpec.getAppFunctionData("nestedBase")
    var nestedBase: NestedBaseSerializable? = null
    if (nestedBaseData != null) {
      nestedBase = nestedBaseSerializableFactory.fromAppFunctionData(nestedBaseData)
    }

    val resultNestedBaseSerializable = NestedBaseSerializable(stringBaseName, nestedBase)
    return resultNestedBaseSerializable
  }

  override fun toAppFunctionData(spec: AppFunctionDataSpec?, appFunctionSerializable: NestedBaseSerializable): AppFunctionData {
    val nestedBaseSerializable_appFunctionSerializable = appFunctionSerializable
    val nestedBaseSerializableFactory = `$NestedBaseSerializableFactory`()

    val builder = getAppFunctionDataBuilder(spec, "com.testdata.NestedBaseSerializable")
    val stringBaseName = nestedBaseSerializable_appFunctionSerializable.stringBaseName
    builder.setString("stringBaseName", stringBaseName)
    val nestedBase = nestedBaseSerializable_appFunctionSerializable.nestedBase
    if (nestedBase != null) {
      builder.setAppFunctionData(
        "nestedBase",
        nestedBaseSerializableFactory.toAppFunctionData(
          spec?.getPropertyObjectSpec("nestedBase", checkNotNull(nestedBase::class.java.canonicalName)),
          nestedBase
        )
      )
    }

    return builder.build()
  }
}
