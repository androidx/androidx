package com.testdata.oneofserializable

import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import java.lang.IllegalArgumentException
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$NonSealedOneOfSerializableFactory` : AppFunctionSerializableFactory<NonSealedOneOfSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): NonSealedOneOfSerializable = when(appFunctionData.qualifiedName) {
    NonSealedOneOfSerializable.ASubclass::class.java.name ->  {
      val nonSealedOneOfSerializableASubclassFactory = `$NonSealedOneOfSerializable$ASubclassFactory`()
      nonSealedOneOfSerializableASubclassFactory.fromAppFunctionData(appFunctionData)}
    NonSealedOneOfSerializable.BSubclass::class.java.name ->  {
      val nonSealedOneOfSerializableBSubclassFactory = `$NonSealedOneOfSerializable$BSubclassFactory`()
      nonSealedOneOfSerializableBSubclassFactory.fromAppFunctionData(appFunctionData)}
    else -> throw IllegalArgumentException("""Unknown qualifiedName: ${appFunctionData.qualifiedName}""")
  }

  override fun toAppFunctionData(appFunctionSerializable: NonSealedOneOfSerializable): AppFunctionData = when(appFunctionSerializable) {
    is NonSealedOneOfSerializable.ASubclass ->  {
      val nonSealedOneOfSerializableASubclassFactory = `$NonSealedOneOfSerializable$ASubclassFactory`()
      nonSealedOneOfSerializableASubclassFactory.toAppFunctionData(appFunctionSerializable)}
    is NonSealedOneOfSerializable.BSubclass ->  {
      val nonSealedOneOfSerializableBSubclassFactory = `$NonSealedOneOfSerializable$BSubclassFactory`()
      nonSealedOneOfSerializableBSubclassFactory.toAppFunctionData(appFunctionSerializable)}
    else -> throw IllegalArgumentException("""Unknown class type for OneOfType: ${appFunctionSerializable::class.java}""")
  }
}
