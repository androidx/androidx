package com.testdata.oneofserializable

import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$NestedOneOfSerializableWithinSerializableFactory` : AppFunctionSerializableFactory<NestedOneOfSerializableWithinSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): NestedOneOfSerializableWithinSerializable {
    val appFunctionDataWithSpec = getAppFunctionDataWithSpec(appFunctionData = appFunctionData, qualifiedName = "com.testdata.oneofserializable.NestedOneOfSerializableWithinSerializable")
    val oneOfSealedInterfaceFactory = `$OneOfSealedInterfaceFactory`()
    val oneOfSealedClassFactory = `$OneOfSealedClassFactory`()

    val str = checkNotNull(appFunctionDataWithSpec.getStringOrNull("str"))
    val oneOfSerializableData = checkNotNull(appFunctionDataWithSpec.getAppFunctionData("oneOfSerializable"))
    val oneOfSerializable = oneOfSealedInterfaceFactory.fromAppFunctionData(oneOfSerializableData)
    val oneOfSerializableListData = checkNotNull(appFunctionDataWithSpec.getAppFunctionDataList("oneOfSerializableList"))
    val oneOfSerializableList = oneOfSerializableListData.map { data ->
      oneOfSealedClassFactory.fromAppFunctionData(data)
    }

    val resultNestedOneOfSerializableWithinSerializable = NestedOneOfSerializableWithinSerializable(str, oneOfSerializable, oneOfSerializableList)
    return resultNestedOneOfSerializableWithinSerializable
  }

  override fun toAppFunctionData(appFunctionSerializable: NestedOneOfSerializableWithinSerializable): AppFunctionData {
    val nestedOneOfSerializableWithinSerializable_appFunctionSerializable = appFunctionSerializable
    val oneOfSealedInterfaceFactory = `$OneOfSealedInterfaceFactory`()
    val oneOfSealedClassFactory = `$OneOfSealedClassFactory`()

    val builder = getAppFunctionDataBuilder("com.testdata.oneofserializable.NestedOneOfSerializableWithinSerializable")
    val str = nestedOneOfSerializableWithinSerializable_appFunctionSerializable.str
    builder.setString("str", str)
    val oneOfSerializable = nestedOneOfSerializableWithinSerializable_appFunctionSerializable.oneOfSerializable
    builder.setAppFunctionData("oneOfSerializable", oneOfSealedInterfaceFactory.toAppFunctionData(oneOfSerializable))
    val oneOfSerializableList = nestedOneOfSerializableWithinSerializable_appFunctionSerializable.oneOfSerializableList
    builder.setAppFunctionDataList("oneOfSerializableList", oneOfSerializableList.map{ oneOfSealedClass ->
      oneOfSealedClassFactory.toAppFunctionData(oneOfSealedClass)
    })

    return builder.build()
  }
}
