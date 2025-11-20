package com.testdata.oneofserializable

import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import java.lang.IllegalArgumentException
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$OneOfSealedClassFactory` : AppFunctionSerializableFactory<OneOfSealedClass> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): OneOfSealedClass = when(appFunctionData.qualifiedName) {
    OneOfSealedClass.ASubclass::class.java.name ->  {
      val oneOfSealedClassASubclassFactory = `$OneOfSealedClass$ASubclassFactory`()
      oneOfSealedClassASubclassFactory.fromAppFunctionData(appFunctionData)}
    OneOfSealedClass.BSubclass::class.java.name ->  {
      val oneOfSealedClassBSubclassFactory = `$OneOfSealedClass$BSubclassFactory`()
      oneOfSealedClassBSubclassFactory.fromAppFunctionData(appFunctionData)}
    else -> throw IllegalArgumentException("""Unknown qualifiedName: ${appFunctionData.qualifiedName}""")
  }

  override fun toAppFunctionData(appFunctionSerializable: OneOfSealedClass): AppFunctionData = when(appFunctionSerializable) {
    is OneOfSealedClass.ASubclass ->  {
      val oneOfSealedClassASubclassFactory = `$OneOfSealedClass$ASubclassFactory`()
      oneOfSealedClassASubclassFactory.toAppFunctionData(appFunctionSerializable)}
    is OneOfSealedClass.BSubclass ->  {
      val oneOfSealedClassBSubclassFactory = `$OneOfSealedClass$BSubclassFactory`()
      oneOfSealedClassBSubclassFactory.toAppFunctionData(appFunctionSerializable)}
  }
}
