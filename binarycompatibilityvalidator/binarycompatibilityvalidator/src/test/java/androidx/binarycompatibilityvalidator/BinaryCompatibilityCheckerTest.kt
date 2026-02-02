/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.binarycompatibilityvalidator

import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.validation.ExperimentalBCVApi
import kotlinx.validation.api.klib.KlibDump
import kotlinx.validation.api.klib.KlibDumpFilters
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader
import org.jetbrains.kotlin.library.abi.LibraryAbiReader
import org.junit.Test

@OptIn(ExperimentalLibraryAbiReader::class, ExperimentalBCVApi::class)
class BinaryCompatibilityCheckerTest {
    private val klibFile by lazy { getJavaResource("collection.klib") }

    @Test
    fun klibDumpIsCompatibleWithItself() {
        val libraryAbi = LibraryAbiReader.readAbiInfo(klibFile, emptyList())
        val dump = KlibDump.fromKlib(klibFile, "linuxX64", KlibDumpFilters {})
        val dumpText =
            StringBuilder().let {
                dump.saveTo(it)
                it.toString()
            }

        val parsedLibraryAbis = KlibDumpParser(dumpText).parse()

        BinaryCompatibilityChecker.checkAllBinariesAreCompatible(
            mapOf("linuxX64" to libraryAbi),
            parsedLibraryAbis
        )
    }

    @Test
    fun makePublicPrivate() {
        val beforeText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        
        """
        val expectedErrorMessages =
            listOf("Removed declaration my.lib/MyClass from androidx:library")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun removedFunction() {
        val beforeText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
            final fun myFun(): kotlin/String // my.lib/MyClass.myFun|myFun(){}[0]
            final val myProperty // my.lib/MyClass.myProperty|{}myProperty[0]
                final fun <get-myProperty>(): kotlin/String // my.lib/MyClass.myProperty.<get-myProperty>|<get-myProperty>(){}[0]
        }
        open annotation class my.lib/MyAnnotation : kotlin/Annotation { // my.lib/MyAnnotation|null[0]
            constructor <init>() // my.lib/MyAnnotation.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
            final val myProperty // my.lib/MyClass.myProperty|{}myProperty[0]
                final fun <get-myProperty>(): kotlin/String // my.lib/MyClass.myProperty.<get-myProperty>|<get-myProperty>(){}[0]
        }
        open annotation class my.lib/MyAnnotation : kotlin/Annotation { // my.lib/MyAnnotation|null[0]
            constructor <init>() // my.lib/MyAnnotation.<init>|<init>(){}[0]
        }
        """
        val expectedErrorMessages = listOf("Removed declaration myFun() from my.lib/MyClass")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun addTypeParameters() {
        val beforeText =
            """
        final class <#A: kotlin/Any?> my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        final class <#A: kotlin/Any?, #B: kotlin/Any?> my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val expectedErrorMessages = listOf("Added typeParameter B to my.lib/MyClass")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun changeTypeParamToReified() {
        val beforeText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
            final inline fun <#A1: kotlin/Any?> myFunction() // my.lib/MyClass.myFunction|myFunction(){0§<kotlin.Any?>}[0]
        }
        """
        val afterText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
            final inline fun <#A1: reified kotlin/Any?> myFunction() // my.lib/MyClass.myFunction|myFunction(){0§<kotlin.Any?>}[0]
        }
        """
        val expectedErrorMessages =
            listOf(
                "isReified changed from false to true for type param A1 on my.lib/MyClass.myFunction"
            )
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun openToNonOpen() {
        val beforeText =
            """
        open class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val expectedErrorMessages = listOf("modality changed from OPEN to FINAL for my.lib/MyClass")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun changeVarianceForTypeParam() {
        val beforeText =
            """
        final class <#A: kotlin/Any?> my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        final class <#A: out kotlin/Any?> my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val expectedErrorMessages =
            listOf("variance changed from INVARIANT to OUT for type param A on")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun changeTypeArg() {
        val beforeText =
            """
        final class my.lib/MySubClass : my.lib/MyClass<kotlin/Int> { // my.lib/MySubClass|null[0]
            constructor <init>() // my.lib/MySubClass.<init>|<init>(){}[0]
        }
        open class <#A: kotlin/Any?> my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        final class my.lib/MySubClass : my.lib/MyClass<kotlin/String> { // my.lib/MySubClass|null[0]
            constructor <init>() // my.lib/MySubClass.<init>|<init>(){}[0]
        }
        open class <#A: kotlin/Any?> my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val expectedErrorMessages =
            listOf(
                "Removed typeArgument kotlin/Int from my.lib/MySubClass",
                "Added typeArgument kotlin/String to my.lib/MySubClass",
            )
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun openValToOpenVar() {
        val beforeText =
            """
        open class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
            open val myConst // my.lib/MyClass.myConst|{}myConst[0]
                open fun <get-myConst>(): kotlin/Int // my.lib/MyClass.myConst.<get-myConst>|<get-myConst>(){}[0]
        }
        """
        val afterText =
            """
        open class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
            open var myConst // my.lib/MyClass.myConst|{}myConst[0]
                open fun <get-myConst>(): kotlin/Int // my.lib/MyClass.myConst.<get-myConst>|<get-myConst>(){}[0]
                open fun <set-myConst>(kotlin/Int) // my.lib/MyClass.myConst.<set-myConst>|<set-myConst>(kotlin.Int){}[0]
        }
        """
        val expectedErrorMessages =
            listOf("kind changed from VAL to VAR for my.lib/MyClass.myConst")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun finalClassToAbstract() {
        val beforeText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        abstract class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val expectedErrorMessages =
            listOf("modality changed from FINAL to ABSTRACT for my.lib/MyClass")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun paramInlineToNoinline() {
        val beforeText =
            """
        final inline fun my.lib/myFun(kotlin/Function0<kotlin/Unit>) // my.lib/myFun|myFun(kotlin.Function0<kotlin.Unit>){}[0]
        """
        val afterText =
            """
        final inline fun my.lib/myFun(noinline kotlin/Function0<kotlin/Unit>) // my.lib/myFun|myFun(kotlin.Function0<kotlin.Unit>){}[0]
        """
        val expectedErrorMessages =
            listOf(
                "isNoinline changed from false to true for parameter kotlin/Function0 of my.lib/myFun"
            )
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun changeSuperclasses() {
        val beforeText =
            """
        abstract interface my.lib/SuperB // my.lib/SuperB|null[0]
        abstract interface my.lib/SuperC // my.lib/SuperC|null[0]
        final class my.lib/MyClass : my.lib/SuperA, my.lib/SuperB { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        open class my.lib/SuperA { // my.lib/SuperA|null[0]
            constructor <init>() // my.lib/SuperA.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        abstract interface my.lib/SuperB // my.lib/SuperB|null[0]
        abstract interface my.lib/SuperC // my.lib/SuperC|null[0]
        final class my.lib/MyClass : my.lib/SuperA, my.lib/SuperC { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        open class my.lib/SuperA { // my.lib/SuperA|null[0]
            constructor <init>() // my.lib/SuperA.<init>|<init>(){}[0]
        }
        """
        val expectedErrorMessages = listOf("Removed superType my.lib/SuperB from my.lib/MyClass")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun changeToAnnotationClass() {
        val beforeText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        open annotation class my.lib/MyClass : kotlin/Annotation { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val expectedErrorMessages =
            listOf("kind changed from CLASS to ANNOTATION_CLASS for my.lib/MyClass")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun changeClassname() {
        val beforeText =
            """
        abstract class my.lib/MyClass { // my.lib/MyClass|null[0]
            abstract fun createString(): kotlin/String // my.lib/MyClass.createString|createString(){}[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        abstract class my.lib/MyClassRenamed { // my.lib/MyClassRenamed|null[0]
            abstract fun createString(): kotlin/String // my.lib/MyClassRenamed.createString|createString(){}[0]
            constructor <init>() // my.lib/MyClassRenamed.<init>|<init>(){}[0]
        }
        """
        val expectedErrorMessages =
            listOf("Removed declaration my.lib/MyClass from androidx:library")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun moveInnerClassOut() {
        val beforeText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
            final class MyInnerClass { // my.lib/MyClass.MyInnerClass|null[0]
                constructor <init>() // my.lib/MyClass.MyInnerClass.<init>|<init>(){}[0]
            }
        }
        """
        val afterText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        final class my.lib/MyInnerClass { // my.lib/MyInnerClass|null[0]
            constructor <init>() // my.lib/MyInnerClass.<init>|<init>(){}[0]
        }
        """
        val expectedErrorMessages = listOf("Removed declaration MyInnerClass from my.lib/MyClass")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun removeProperty() {
        val beforeText =
            """
        final class my.lib/MyClassRenamed { // my.lib/MyClassRenamed|null[0]
            constructor <init>() // my.lib/MyClassRenamed.<init>|<init>(){}[0]
            final val myProperty // my.lib/MyClassRenamed.myProperty|{}myProperty[0]
                final fun <get-myProperty>(): kotlin/String // my.lib/MyClassRenamed.myProperty.<get-myProperty>|<get-myProperty>(){}[0]
        }
        """
        val afterText =
            """
        final class my.lib/MyClassRenamed { // my.lib/MyClassRenamed|null[0]
            constructor <init>() // my.lib/MyClassRenamed.<init>|<init>(){}[0]
        }
        """
        val expectedErrorMessages =
            listOf("Removed declaration myProperty from my.lib/MyClassRenamed")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun changeFromReceiverToFirstParam() {
        val beforeText =
            """
        final fun my.lib/myFun(kotlin/Int): kotlin/Int // my.lib/myFun|myFun(kotlin.Int){}[0]
        """
        val afterText =
            """
        final fun (kotlin/Int).my.lib/myFun(): kotlin/Int // my.lib/myFun|myFun@kotlin.Int(){}[0]
        """
        val expectedErrorMessages =
            listOf("hasExtensionReceiverParameter changed from false to true for my.lib/myFun")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun functionSuspendToNotSuspend() {
        val beforeText =
            """
        final suspend fun my.lib/myFun(): kotlin/Int // my.lib/myFun|myFun(){}[0]
        """
        val afterText =
            """
        final fun my.lib/myFun(): kotlin/Int // my.lib/myFun|myFun(){}[0]
        """
        val expectedErrorMessages = listOf("isSuspend changed from true to false for my.lib/myFun")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun changedReturnType() {
        val beforeText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
            final fun myFun(): kotlin/String // my.lib/MyClass.myFun|myFun(){}[0]
        }
        """
        val afterText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
            final fun myFun(): kotlin/Boolean // my.lib/MyClass.myFun|myFun(){}[0]
        }
        """
        val expectedErrorMessages =
            listOf(
                "Return type changed from kotlin/String to kotlin/Boolean for my.lib/MyClass.myFun"
            )
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun changedReturnTypeNullability() {
        val beforeText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
            final fun myFun(): kotlin/String? // my.lib/MyClass.myFun|myFun(){}[0]
        }
        """
        val afterText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
            final fun myFun(): kotlin/String // my.lib/MyClass.myFun|myFun(){}[0]
        }
        """
        val expectedErrorMessages =
            listOf("Return type nullability did not match for my.lib/MyClass.myFun")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun propertyConstToNonConst() {
        val beforeText =
            """
        final const val my.lib/myConst // my.lib/myConst|{}myConst[0]
            final fun <get-myConst>(): kotlin/Int // my.lib/myConst.<get-myConst>|<get-myConst>(){}[0]
        """
        val afterText =
            """
        final val my.lib/myConst // my.lib/myConst|{}myConst[0]
            final fun <get-myConst>(): kotlin/Int // my.lib/myConst.<get-myConst>|<get-myConst>(){}[0]
        """
        val expectedErrorMessages = listOf("kind changed from CONST_VAL to VAL for my.lib/myConst")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun propertyVarToVal() {
        val beforeText =
            """
        final var my.lib/myVal // my.lib/myVal|{}myVal[0]
            final fun <get-myVal>(): kotlin/Int // my.lib/myVal.<get-myVal>|<get-myVal>(){}[0]
            final fun <set-myVal>(kotlin/Int) // my.lib/myVal.<set-myVal>|<set-myVal>(kotlin.Int){}[0]
        """
        val afterText =
            """
        final val my.lib/myVal // my.lib/myVal|{}myVal[0]
            final fun <get-myVal>(): kotlin/Int // my.lib/myVal.<get-myVal>|<get-myVal>(){}[0]
        """
        val expectedErrorMessages =
            listOf(
                "kind changed from VAR to VAL for my.lib/myVal",
                "removed setter from my.lib/myVal"
            )
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun paramInlineToCrossInline() {
        val beforeText =
            """
        final inline fun my.lib/myFun(noinline kotlin/Function0<kotlin/Unit>) // my.lib/myFun|myFun(kotlin.Function0<kotlin.Unit>){}[0]
        """
        val afterText =
            """
        final inline fun my.lib/myFun(crossinline kotlin/Function0<kotlin/Unit>) // my.lib/myFun|myFun(kotlin.Function0<kotlin.Unit>){}[0]
        """
        val expectedErrorMessages =
            listOf(
                "isNoinline changed from true to false for parameter kotlin/Function0" +
                    " of my.lib/myFun",
                "isCrossinline changed from false to true for parameter kotlin/Function0 of " +
                    "my.lib/myFun"
            )
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun changeTypeParamBounds() {
        val beforeText =
            """
        final class <#A: my.lib/Foo> my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        open class my.lib/Bar { // my.lib/Bar|null[0]
            constructor <init>() // my.lib/Bar.<init>|<init>(){}[0]
        }
        open class my.lib/Foo : my.lib/Bar { // my.lib/Foo|null[0]
            constructor <init>() // my.lib/Foo.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        final class <#A: my.lib/Bar> my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        open class my.lib/Bar { // my.lib/Bar|null[0]
            constructor <init>() // my.lib/Bar.<init>|<init>(){}[0]
        }
        open class my.lib/Foo : my.lib/Bar { // my.lib/Foo|null[0]
            constructor <init>() // my.lib/Foo.<init>|<init>(){}[0]
        }
        """
        val expectedErrorMessages =
            listOf(
                "upper bounds changed from my.lib/Foo to my.lib/Bar type param A on my.lib/MyClass"
            )
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun removeTypeParameters() {
        val beforeText =
            """
        final class <#A: kotlin/Any?, #B: kotlin/Any?> my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        final class <#A: kotlin/Any?> my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val expectedErrorMessages = listOf("Removed typeParameter B from my.lib/MyClass")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun changeToEnumClass() {
        val beforeText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        final enum class my.lib/MyClass : kotlin/Enum<my.lib/MyClass> { // my.lib/MyClass|null[0]
            final fun valueOf(kotlin/String): my.lib/MyClass // my.lib/MyClass.valueOf|valueOf#static(kotlin.String){}[0]
            final fun values(): kotlin/Array<my.lib/MyClass> // my.lib/MyClass.values|values#static(){}[0]
            final val entries // my.lib/MyClass.entries|#static{}entries[0]
                final fun <get-entries>(): kotlin.enums/EnumEntries<my.lib/MyClass> // my.lib/MyClass.entries.<get-entries>|<get-entries>#static(){}[0]
        }
        """
        val expectedErrorMessages =
            listOf(
                "kind changed from CLASS to ENUM_CLASS for my.lib/MyClass",
            )
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun removedEnumEntries() {
        val beforeText =
            """
        final enum class my.lib/MyClass : kotlin/Enum<my.lib/MyClass> { // my.lib/MyClass|null[0]
            enum entry FIRST // my.lib/MyClass.FIRST|null[0]
            enum entry SECOND // my.lib/MyClass.SECOND|null[0]
            final fun valueOf(kotlin/String): my.lib/MyClass // my.lib/MyClass.valueOf|valueOf#static(kotlin.String){}[0]
            final fun values(): kotlin/Array<my.lib/MyClass> // my.lib/MyClass.values|values#static(){}[0]
            final val entries // my.lib/MyClass.entries|#static{}entries[0]
                final fun <get-entries>(): kotlin.enums/EnumEntries<my.lib/MyClass> // my.lib/MyClass.entries.<get-entries>|<get-entries>#static(){}[0]
        }
        """

        val afterText =
            """
        final enum class my.lib/MyClass : kotlin/Enum<my.lib/MyClass> { // my.lib/MyClass|null[0]
            enum entry FIRST // my.lib/MyClass.FIRST|null[0]
            final fun valueOf(kotlin/String): my.lib/MyClass // my.lib/MyClass.valueOf|valueOf#static(kotlin.String){}[0]
            final fun values(): kotlin/Array<my.lib/MyClass> // my.lib/MyClass.values|values#static(){}[0]
            final val entries // my.lib/MyClass.entries|#static{}entries[0]
                final fun <get-entries>(): kotlin.enums/EnumEntries<my.lib/MyClass> // my.lib/MyClass.entries.<get-entries>|<get-entries>#static(){}[0]
        }
        """

        val expectedErrorMessages = listOf("Removed declaration SECOND from my.lib/MyClass")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun nonSealedToSealed() {
        val beforeText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        sealed class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val expectedErrorMessages =
            listOf("modality changed from FINAL to SEALED for my.lib/MyClass")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun propertyInlineToNonInline() {
        val beforeText =
            """
        final val my.lib/name // my.lib/name|@kotlin.Int{}name[0]
            final inline fun (kotlin/Int).<get-name>(): kotlin/String // my.lib/name.<get-name>|<get-name>@kotlin.Int(){}[0]
        """
        val afterText =
            """
        final val my.lib/name // my.lib/name|@kotlin.Int{}name[0]
            final fun (kotlin/Int).<get-name>(): kotlin/String // my.lib/name.<get-name>|<get-name>@kotlin.Int(){}[0]
        """
        val expectedErrorMessages =
            listOf("isInline changed from true to false for my.lib/name.<get-name>")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun removeConcreteImplemantation() {
        val beforeText =
            """
        abstract class my.lib/MyClass : my.lib/MyInterface { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
            open fun createString(): kotlin/String // my.lib/MyClass.createString|createString(){}[0]
        }
        abstract interface my.lib/MyInterface { // my.lib/MyInterface|null[0]
            abstract fun createString(): kotlin/String // my.lib/MyInterface.createString|createString(){}[0]
        }
        """
        val afterText =
            """
        abstract class my.lib/MyClass : my.lib/MyInterface { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        abstract interface my.lib/MyInterface { // my.lib/MyInterface|null[0]
            abstract fun createString(): kotlin/String // my.lib/MyInterface.createString|createString(){}[0]
        }
        """
        val expectedErrorMessages =
            listOf("modality changed from OPEN to ABSTRACT for my.lib/MyInterface.createString")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun functionInlineToNotInline() {
        val beforeText =
            """
        final inline fun my.lib/myFun(): kotlin/Int // my.lib/myFun|myFun(){}[0]
        """
        val afterText =
            """
        final fun my.lib/myFun(): kotlin/Int // my.lib/myFun|myFun(){}[0]
        """
        val expectedErrorMessages = listOf("isInline changed from true to false for my.lib/myFun")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun functionRemoveDefaultFromParam() {
        val beforeText =
            """
        final fun my.lib/myFun(kotlin/Int =...): kotlin/Int // my.lib/myFun|myFun(kotlin.Int){}[0]
        """
        val afterText =
            """
        final fun my.lib/myFun(kotlin/Int): kotlin/Int // my.lib/myFun|myFun(kotlin.Int){}[0]
        """
        val expectedErrorMessages =
            listOf(
                "hasDefaultArg changed from true to false for parameter kotlin/Int of my.lib/myFun"
            )
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun concreteToAbstract() {
        val beforeText =
            """
        abstract class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
            final fun createString(): kotlin/String // my.lib/MyClass.createString|createString(){}[0]
        }
        """
        val afterText =
            """
        abstract class my.lib/MyClass { // my.lib/MyClass|null[0]
            abstract fun createString(): kotlin/String // my.lib/MyClass.createString|createString(){}[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val expectedErrorMessages =
            listOf("modality changed from FINAL to ABSTRACT for my.lib/MyClass.createString")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun paramCrossinlineToNoinline() {
        val beforeText =
            """
        final inline fun my.lib/myFun(crossinline kotlin/Function0<kotlin/Unit>) // my.lib/myFun|myFun(kotlin.Function0<kotlin.Unit>){}[0]
        """
        val afterText =
            """
        final inline fun my.lib/myFun(noinline kotlin/Function0<kotlin/Unit>) // my.lib/myFun|myFun(kotlin.Function0<kotlin.Unit>){}[0]
        """
        val expectedErrorMessages =
            listOf(
                "isNoinline changed from false to true for parameter kotlin/Function0" +
                    " of my.lib/myFun",
                "isCrossinline changed from true to false for parameter kotlin/Function0 of " +
                    "my.lib/myFun"
            )
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun functionParamVarargToList() {
        val beforeText =
            """
        final fun my.lib/myFun(kotlin/IntArray...): kotlin/IntArray // my.lib/myFun|myFun(kotlin.IntArray...){}[0]
        """
        val afterText =
            """
        final fun my.lib/myFun(kotlin/IntArray): kotlin/IntArray // my.lib/myFun|myFun(kotlin.IntArray){}[0]
        """
        val expectedErrorMessages =
            listOf(
                "isVararg changed from true to false for parameter kotlin/IntArray of my.lib/myFun"
            )
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun addValueParam() {
        val beforeText =
            """
        final fun my.lib/myFun(): kotlin/Int // my.lib/myFun|myFun(){}[0]
        """
        val afterText =
            """
        final fun my.lib/myFun(kotlin/Int): kotlin/Int // my.lib/myFun|myFun(kotlin/Int){}[0]
        """
        testBeforeAndAfterIsIncompatible(
            beforeText,
            afterText,
            listOf("Removed declaration my.lib/myFun() from androidx:library")
        )
    }

    @Test
    fun changeClassKindToInterface() {
        val beforeText =
            """
        abstract class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        abstract interface my.lib/MyClass // my.lib/MyClass|null[0]
        """
        val expectedErrorMessages =
            listOf("kind changed from CLASS to INTERFACE for my.lib/MyClass")
        testBeforeAndAfterIsIncompatible(beforeText, afterText, expectedErrorMessages)
    }

    @Test
    fun removeDataClassWithReimplementation() {
        val beforeText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>(kotlin/Int) // my.lib/MyClass.<init>|<init>(kotlin.Int){}[0]
            final fun component1(): kotlin/Int // my.lib/MyClass.component1|component1(){}[0]
            final fun copy(kotlin/Int =...): my.lib/MyClass // my.lib/MyClass.copy|copy(kotlin.Int){}[0]
            final fun equals(kotlin/Any?): kotlin/Boolean // my.lib/MyClass.equals|equals(kotlin.Any?){}[0]
            final fun hashCode(): kotlin/Int // my.lib/MyClass.hashCode|hashCode(){}[0]
            final fun toString(): kotlin/String // my.lib/MyClass.toString|toString(){}[0]
            final val myParam // my.lib/MyClass.myParam|{}myParam[0]
                final fun <get-myParam>(): kotlin/Int // my.lib/MyClass.myParam.<get-myParam>|<get-myParam>(){}[0]
        }
        """
        val afterText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>(kotlin/Int) // my.lib/MyClass.<init>|<init>(kotlin.Int){}[0]
            final fun component1(): kotlin/Int // my.lib/MyClass.component1|component1(){}[0]
            final fun copy(kotlin/Int =...): my.lib/MyClass // my.lib/MyClass.copy|copy(kotlin.Int){}[0]
            final fun equals(kotlin/Any?): kotlin/Boolean // my.lib/MyClass.equals|equals(kotlin.Any?){}[0]
            final fun hashCode(): kotlin/Int // my.lib/MyClass.hashCode|hashCode(){}[0]
            final fun toString(): kotlin/String // my.lib/MyClass.toString|toString(){}[0]
            final val myParam // my.lib/MyClass.myParam|{}myParam[0]
                final fun <get-myParam>(): kotlin/Int // my.lib/MyClass.myParam.<get-myParam>|<get-myParam>(){}[0]
        }
        """
        testBeforeAndAfterIsCompatible(beforeText, afterText)
    }

    @Test
    fun classToDataClass() {
        val beforeText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>(kotlin/Int) // my.lib/MyClass.<init>|<init>(kotlin.Int){}[0]
            final val myParam // my.lib/MyClass.myParam|{}myParam[0]
                final fun <get-myParam>(): kotlin/Int // my.lib/MyClass.myParam.<get-myParam>|<get-myParam>(){}[0]
        }
        """
        val afterText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>(kotlin/Int) // my.lib/MyClass.<init>|<init>(kotlin.Int){}[0]
            final fun component1(): kotlin/Int // my.lib/MyClass.component1|component1(){}[0]
            final fun copy(kotlin/Int =...): my.lib/MyClass // my.lib/MyClass.copy|copy(kotlin.Int){}[0]
            final fun equals(kotlin/Any?): kotlin/Boolean // my.lib/MyClass.equals|equals(kotlin.Any?){}[0]
            final fun hashCode(): kotlin/Int // my.lib/MyClass.hashCode|hashCode(){}[0]
            final fun toString(): kotlin/String // my.lib/MyClass.toString|toString(){}[0]
            final val myParam // my.lib/MyClass.myParam|{}myParam[0]
                final fun <get-myParam>(): kotlin/Int // my.lib/MyClass.myParam.<get-myParam>|<get-myParam>(){}[0]
        }
        """
        testBeforeAndAfterIsCompatible(beforeText, afterText)
    }

    @Test
    fun propertyNonConstToConst() {
        val beforeText =
            """
        final val my.lib/myProp // my.lib/myProp|{}myProp[0]
            final fun <get-myProp>(): kotlin/Int // my.lib/myProp.<get-myProp>|<get-myProp>(){}[0]
        """
        val afterText =
            """
        final const val my.lib/myProp // my.lib/myProp|{}myProp[0]
            final fun <get-myProp>(): kotlin/Int // my.lib/myProp.<get-myProp>|<get-myProp>(){}[0]
        """
        testBeforeAndAfterIsCompatible(beforeText, afterText)
    }

    @Test
    fun propertyRegularToInline() {
        val beforeText =
            """
        final val my.lib/myProp // my.lib/myProp|@kotlin.Int{}myProp[0]
            final fun (kotlin/Int).<get-myProp>(): kotlin/Int // my.lib/myProp.<get-myProp>|<get-myProp>@kotlin.Int(){}[0]
        """
        val afterText =
            """
        final val my.lib/myProp // my.lib/myProp|@kotlin.Int{}myProp[0]
            final inline fun (kotlin/Int).<get-myProp>(): kotlin/Int // my.lib/myProp.<get-myProp>|<get-myProp>@kotlin.Int(){}[0]
        """
        testBeforeAndAfterIsCompatible(beforeText, afterText)
    }

    @Test
    fun sealedToAbstract() {
        val beforeText =
            """
        sealed class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        abstract class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        testBeforeAndAfterIsCompatible(beforeText, afterText)
    }

    @Test
    fun addImplementationToAbstractEntity() {
        val beforeText =
            """
        abstract interface my.lib/MyInterface { // my.lib/MyInterface|null[0]
            abstract fun myFun(): kotlin/Int // my.lib/MyInterface.myFun|myFun(){}[0]
        }
        """
        val afterText =
            """
        abstract interface my.lib/MyInterface { // my.lib/MyInterface|null[0]
            open fun myFun(): kotlin/Int // my.lib/MyInterface.myFun|myFun(){}[0]
        }
        """
        testBeforeAndAfterIsCompatible(beforeText, afterText)
    }

    @Test
    fun changeOrderOfSuperclases() {
        val beforeText =
            """
        abstract interface my.lib/B // my.lib/B|null[0]
        abstract interface my.lib/C // my.lib/C|null[0]
        final class my.lib/A : my.lib/B, my.lib/C { // my.lib/A|null[0]
            constructor <init>() // my.lib/A.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        abstract interface my.lib/B // my.lib/B|null[0]
        abstract interface my.lib/C // my.lib/C|null[0]
        final class my.lib/A : my.lib/B, my.lib/C { // my.lib/A|null[0]
            constructor <init>() // my.lib/A.<init>|<init>(){}[0]
        }
        """
        testBeforeAndAfterIsCompatible(beforeText, afterText)
    }

    @Test
    fun abstractToOpen() {
        val beforeText =
            """
        abstract class my.lib/MyClass { // my.lib/MyClass|null[0]
            abstract fun myFun() // my.lib/MyClass.myFun|myFun(){}[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        open class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
            open fun myFun() // my.lib/MyClass.myFun|myFun(){}[0]
        }
        """
        testBeforeAndAfterIsCompatible(beforeText, afterText)
    }

    @Test
    fun nonOpenEntityOpen() {
        val beforeText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        open class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        testBeforeAndAfterIsCompatible(beforeText, afterText)
    }

    @Test
    fun propertyNonOpenValToVar() {
        val beforeText =
            """
        final val my.lib/myProp // my.lib/myProp|{}myProp[0]
            final fun <get-myProp>(): kotlin/Int // my.lib/myProp.<get-myProp>|<get-myProp>(){}[0]
        """
        val afterText =
            """
        final var my.lib/myProp // my.lib/myProp|{}myProp[0]
            final fun <get-myProp>(): kotlin/Int // my.lib/myProp.<get-myProp>|<get-myProp>(){}[0]
            final fun <set-myProp>(kotlin/Int) // my.lib/myProp.<set-myProp>|<set-myProp>(kotlin.Int){}[0]
        """
        testBeforeAndAfterIsCompatible(beforeText, afterText)
    }

    @Test
    fun functionAddingDefaultToParam() {
        val beforeText =
            """
        final fun my.lib/myFun(kotlin/Int): kotlin/Int // my.lib/myFun|myFun(kotlin.Int){}[0]
        """
        val afterText =
            """
        final fun my.lib/myFun(kotlin/Int =...): kotlin/Int // my.lib/myFun|myFun(kotlin.Int){}[0]
        """
        testBeforeAndAfterIsCompatible(beforeText, afterText)
    }

    @Test
    fun functionRegularToInline() {
        val beforeText =
            """
        final fun my.lib/myFun(): kotlin/Int // my.lib/myFun|myFun(){}[0]
        """
        val afterText =
            """
        final inline fun my.lib/myFun(): kotlin/Int // my.lib/myFun|myFun(){}[0]
        """
        testBeforeAndAfterIsCompatible(beforeText, afterText)
    }

    @Test
    fun addNewEntity() {
        val beforeText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
            final fun myFun(): kotlin/Int // my.lib/MyClass.myFun|myFun(){}[0]
        }
        """
        testBeforeAndAfterIsCompatible(beforeText, afterText)
    }

    @Test
    fun addNewAbstractMethodToClass() {
        val beforeText =
            """
        abstract class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        abstract class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
            abstract fun myFun(): kotlin/Int // my.lib/MyClass.myFun|myFun(){}[0]
        }
        """
        testBeforeAndAfterIsIncompatible(
            beforeText,
            afterText,
            listOf("Added declaration myFun() to my.lib/MyClass")
        )
    }

    @Test
    fun interfaceToFunctionalInterface() {
        val beforeText =
            """
        abstract interface my.lib/MyInterface { // my.lib/MyInterface|null[0]
            abstract fun run() // my.lib/MyInterface.run|run(){}[0]
        }
        """
        val afterText =
            """
        abstract fun interface my.lib/MyInterface { // my.lib/MyInterface|null[0]
            abstract fun run() // my.lib/MyInterface.run|run(){}[0]
        }
        """
        testBeforeAndAfterIsCompatible(beforeText, afterText)
    }

    @Test
    fun interfaceFromFunctionalInterface() {
        val beforeText =
            """
        abstract fun interface my.lib/MyInterface { // my.lib/MyInterface|null[0]
            abstract fun run() // my.lib/MyInterface.run|run(){}[0]
        }
        """
        val afterText =
            """
        abstract interface my.lib/MyInterface { // my.lib/MyInterface|null[0]
            abstract fun run() // my.lib/MyInterface.run|run(){}[0]
        }
        """
        testBeforeAndAfterIsIncompatible(
            beforeText,
            afterText,
            listOf("isFunction changed from true to false for my.lib/MyInterface")
        )
    }

    @Test
    fun classToValueClass() {
        val beforeText =
            """
        final class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        final value class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
            final fun myFun(): kotlin/Int // my.lib/MyClass.myFun|myFun(){}[0]
        }
        """
        testBeforeAndAfterIsIncompatible(
            beforeText,
            afterText,
            listOf("isValue changed from false to true for my.lib/MyClass")
        )
    }

    @Test
    fun sealedToOpen() {
        val beforeText =
            """
        sealed class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        open class my.lib/MyClass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        """
        testBeforeAndAfterIsCompatible(beforeText, afterText)
    }

    @Test
    fun typeParamReifiedToRegular() {
        val beforeText =
            """
        final inline fun <#A: reified kotlin/Any?> my.lib/myFun(#A): #A // my.lib/myFun|myFun(0:0){0§<kotlin.Any?>}[0]
        """
        val afterText =
            """
        final inline fun <#A: kotlin/Any?> my.lib/myFun(#A): #A // my.lib/myFun|myFun(0:0){0§<kotlin.Any?>}[0]
        """
        testBeforeAndAfterIsCompatible(beforeText, afterText)
    }

    @Test
    fun changeSupertypeToCompatibleSupertype() {
        val beforeText =
            """
        final class my.lib/A : my.lib/C { // my.lib/A|null[0]
            constructor <init>() // my.lib/A.<init>|<init>(){}[0]
        }
        open class my.lib/B : my.lib/C { // my.lib/B|null[0]
            constructor <init>() // my.lib/B.<init>|<init>(){}[0]
            open fun myFun(): kotlin/Int // my.lib/B.myFun|myFun(){}[0]
        }
        open class my.lib/C { // my.lib/C|null[0]
            constructor <init>() // my.lib/C.<init>|<init>(){}[0]
            open fun myFun(): kotlin/Int // my.lib/C.myFun|myFun(){}[0]
        }
        """
        val afterText =
            """
        final class my.lib/A : my.lib/B { // my.lib/A|null[0]
            constructor <init>() // my.lib/A.<init>|<init>(){}[0]
        }
        open class my.lib/B : my.lib/C { // my.lib/B|null[0]
            constructor <init>() // my.lib/B.<init>|<init>(){}[0]
            open fun myFun(): kotlin/Int // my.lib/B.myFun|myFun(){}[0]
        }
        open class my.lib/C { // my.lib/C|null[0]
            constructor <init>() // my.lib/C.<init>|<init>(){}[0]
            open fun myFun(): kotlin/Int // my.lib/C.myFun|myFun(){}[0]
        }
        """
        testBeforeAndAfterIsCompatible(beforeText, afterText)
    }

    @Test
    fun changingFromInheritanceDelegation() {
        val beforeText =
            """
        abstract interface my.lib/C // my.lib/C|null[0]
        final class my.lib/A : my.lib/C { // my.lib/A|null[0]
            constructor <init>() // my.lib/A.<init>|<init>(){}[0]
        }
        open class my.lib/B : my.lib/C { // my.lib/B|null[0]
            constructor <init>() // my.lib/B.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        abstract interface my.lib/C // my.lib/C|null[0]
        final class my.lib/A : my.lib/B { // my.lib/A|null[0]
            constructor <init>() // my.lib/A.<init>|<init>(){}[0]
        }
        open class my.lib/B : my.lib/C { // my.lib/B|null[0]
            constructor <init>() // my.lib/B.<init>|<init>(){}[0]
        }
        """
        testBeforeAndAfterIsCompatible(beforeText, afterText)
    }

    @Test
    fun moveMethodToSuperclass() {
        val beforeText =
            """
        final class my.lib/MyClass : my.lib/MySuperclass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
            final fun myFun(): kotlin/Boolean // my.lib/MyClass.myFun|myFun(){}[0]
        }
        open class my.lib/MySuperclass { // my.lib/MySuperclass|null[0]
            constructor <init>() // my.lib/MySuperclass.<init>|<init>(){}[0]
        }
        """
        val afterText =
            """
        final class my.lib/MyClass : my.lib/MySuperclass { // my.lib/MyClass|null[0]
            constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
        }
        open class my.lib/MySuperclass { // my.lib/MySuperclass|null[0]
            constructor <init>() // my.lib/MySuperclass.<init>|<init>(){}[0]
            final fun myFun(): kotlin/Boolean // my.lib/MySuperclass.myFun|myFun(){}[0]
        }
        """
        testBeforeAndAfterIsCompatible(beforeText, afterText)
    }

    @Test
    fun removeOverrideOfConcreteFunction() {
        val beforeText =
            """
        open class my.lib/A { // my.lib/A|null[0]
            constructor <init>() // my.lib/A.<init>|<init>(){}[0]
            open fun myFun(): kotlin/Int // my.lib/A.myFun|myFun(){}[0]
        }
        open class my.lib/B : my.lib/A { // my.lib/B|null[0]
            constructor <init>() // my.lib/B.<init>|<init>(){}[0]
            open fun myFun(): kotlin/Int // my.lib/B.myFun|myFun(){}[0]
        }
        """
        val afterText =
            """
        open class my.lib/A { // my.lib/A|null[0]
            constructor <init>() // my.lib/A.<init>|<init>(){}[0]
            open fun myFun(): kotlin/Int // my.lib/A.myFun|myFun(){}[0]
        }
        open class my.lib/B : my.lib/A { // my.lib/B|null[0]
            constructor <init>() // my.lib/B.<init>|<init>(){}[0]
        }
        """
        testBeforeAndAfterIsCompatible(beforeText, afterText)
    }

    @Test
    fun removedTargets() {
        val beforeText = createDumpText("", listOf("iosX64", "linuxX64"))
        val afterText = createDumpText("", listOf("linuxX64"))
        val beforeLibs = KlibDumpParser(beforeText).parse()
        val afterLibs = KlibDumpParser(afterText).parse()

        val e =
            assertFailsWith<ValidationException> {
                BinaryCompatibilityChecker.checkAllBinariesAreCompatible(afterLibs, beforeLibs)
            }
        assertThat(e.message).contains("Removed targets [iosX64]")
    }

    private fun testBeforeAndAfterIsCompatible(before: String, after: String) {
        runBeforeAndAfter(before, after)
    }

    private fun testBeforeAndAfterIsIncompatible(
        before: String,
        after: String,
        expectedErrors: List<String>
    ) {
        val e = assertFailsWith<ValidationException> { runBeforeAndAfter(before, after) }
        for (error in expectedErrors) assertThat(e.message).contains(error)
    }

    private fun runBeforeAndAfter(before: String, after: String) {
        val beforeText = createDumpText(before)
        val afterText = createDumpText(after)
        val beforeLibs = KlibDumpParser(beforeText).parse()
        val afterLibs = KlibDumpParser(afterText).parse()

        BinaryCompatibilityChecker.checkAllBinariesAreCompatible(afterLibs, beforeLibs)
    }

    private fun createDumpText(
        content: String,
        targets: List<String> = listOf("iosX64", "linuxX64")
    ) =
        """
        // KLib ABI Dump
        // Targets: [${targets.joinToString(", ")}]
        // Rendering settings:
        // - Signature version: 2
        // - Show manifest properties: true
        // - Show declarations: true
        // Library unique name: <androidx:library>
        $content
        """
            .trimIndent()
}
