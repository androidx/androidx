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

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.concurrent.thread
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Element

/**
 * A task that checks out an Android repository with string translations, extracts the translations
 * we're interested in and writes Kotlin source files that provide them.
 */
abstract class UpdateTranslationsTask : DefaultTask() {

    /**
     * The git binary to use.
     */
    @get:Input
    abstract val git: Property<String>
    init {
        @Suppress("LeakingThis")
        git.convention("git")
    }

    /**
     * The URL of the repository to check out.
     */
    @get:Input
    abstract val gitRepo: Property<String>

    /**
     * The root resources directories in the repo.
     *
     * Note that there may be more than one because Android shares resources across modules, and
     * some modules use resources from more than one.
     */
    @get:Input
    abstract val repoResDirectories: ListProperty<String>

    /**
     * The strings to translate.
     *
     * The keys are the names of the Android resources in the XML file, and the values are the names
     * of the Kotlin `Strings` constants.
     */
    @get:Input
    abstract val stringByResourceName: MapProperty<String, String>

    /**
     * The locales to get the translations for, in `language(_region)` format; e.g. "fr-CA" or just
     * "fr".
     *
     * Note that language may not be an empty string; use "en" for the default locale.
     */
    @get:Input
    abstract val locales: ListProperty<String>

    /**
     * The directory where the Kotlin source files are to be written.
     *
     * Note that this directory is deleted first in order to clear translations that are no longer
     * needed.
     */
    @get:InputDirectory
    abstract val targetDirectory: DirectoryProperty

    /**
     * The name of the package of the Kotlin source files to be written.
     */
    @get:Input
    abstract val targetPackageName: Property<String>

    /**
     * The package name of the Kotlin `Strings.kt` file.
     */
    @get:Input
    abstract val kotlinStringsPackageName: Property<String>

    /**
     * Updates the translations.
     */
    @TaskAction
    fun updateTranslations() {
        // A temporary directory where we will clone the repo
        val dir = Files.createTempDirectory("translations").toFile()
        dir.mkdirs()
        dir.deleteOnExit()

        val targetDirectory = targetDirectory.get().asFile
        if (targetDirectory.isDirectory && !targetDirectory.deleteRecursively())
            throw IOException("Unable to delete directory $targetDirectory")

        if (!targetDirectory.isDirectory && !targetDirectory.mkdirs())
            throw IOException("Unable to create directory $targetDirectory")

        // The directory into which the repo will be cloned
        val repoDir = File(dir, gitRepo.get().substringAfterLast('/'))
        repoDir.deleteRecursively()

        // The directories in the repo to check out.
        // For each locale, there could be several values directories.
        val valuesDirsByLocale: Map<Locale, List<String>> = locales.get().associate { localeTag ->
            val locale = Locale.fromTag(localeTag)
            val valuesDirs = repoResDirectories.get().map {
                val resDir = if (it.endsWith("/")) it else "$it/"
                resDir + locale.valuesDirName()
            }
            locale to valuesDirs
        }

        val gitCommand = git.getOrElse("git")

        // Clone the repo, but don't check out any files
        execCommand(dir, gitCommand, "clone", "-n", "--depth=1", "--filter=tree:0", gitRepo.get())

        // Set a sparse checkout to download only the directories we need
        val allValuesDirs = valuesDirsByLocale.values.flatten()
        execCommand(repoDir, gitCommand, "sparse-checkout", "set", "--no-cone",
            *allValuesDirs.toTypedArray())

        // Actually download them
        execCommand(repoDir, gitCommand, "checkout")

        // Write the per-language translation files
        val localesGroupedByLanguage = valuesDirsByLocale.keys.groupBy { it.language }
        for ((language, locales) in localesGroupedByLanguage) {
            writeLanguageFile(
                language = language,
                locales = locales,
                stringByResourceName = stringByResourceName.get(),
                repoDir = repoDir,
                valuesDirsByLocale = valuesDirsByLocale
            )
        }

        // Write the Translations.kt file
        writeTranslationsFile(localesGroupedByLanguage.values.flatten())
    }

    /**
     * An XML document builder we use (and reuse).
     */
    private val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

    /**
     * Writes the file with translations for the given language and locales.
     *
     * We group all locales with the same language into one file.
     *
     * @param language The language name.
     * @param locales The locales to write translations for.
     * @param stringByResourceName Maps Android resource names to the names of our Kotlin `Strings`.
     * @param repoDir The directory on the disk where the repository has been checked out.
     * @param valuesDirsByLocale For each locale, the paths of the corresponding "values"
     * directories in the repository.
     */
    private fun writeLanguageFile(
        language: String,
        locales: List<Locale>,
        stringByResourceName: Map<String, String>,
        repoDir: File,
        valuesDirsByLocale: Map<Locale, List<String>>,
    ) {
        val kotlinFileName = language.replaceFirstChar { it.uppercase() } + ".kt"
        println("Writing $kotlinFileName for locales ${locales.joinToString()}")

        File(targetDirectory.get().asFile, kotlinFileName).bufferedWriter().use {
            it.write(kotlinFilePreamble())
            it.appendLine("import ${kotlinStringsPackageName.get()}.Strings")
            it.appendLine("import ${kotlinStringsPackageName.get()}.Translations")

            for (locale in locales) {
                // Keep track of the strings for which translations were found, to be able to detect
                // missing ones.
                val remainingStrings = stringByResourceName.values.toMutableSet()

                it.appendLine()
                it.appendLine("@Suppress(\"UnusedReceiverParameter\", \"DuplicatedCode\")")
                it.appendLine("internal fun Translations.${locale.translationFunctionName()}() = mapOf(")

                for (valuesDir in valuesDirsByLocale[locale]!!) {
                    val stringsFile = File(File(repoDir, valuesDir), "strings.xml")
                    if (!stringsFile.isFile) {
                        throw IOException("Missing strings.xml file for locale: $locale")
                    }
                    val document = docBuilder.parse(stringsFile)
                    val root = document.documentElement

                    val nodeList = root.childNodes
                    for (i in 0 until nodeList.length) {
                        val node = nodeList.item(i)

                        val element = node as? Element
                        if (element?.tagName == "string") {
                            val name = element.attributes.getNamedItem("name").nodeValue
                            val string = stringByResourceName[name]
                            if (string != null) {
                                val content = element.textContent
                                    .trim()
                                    .removeSurrounding("\"", "\"")
                                    .replace("\$", "\\$")
                                it.appendLine("    Strings.$string to \"$content\",")
                                remainingStrings.remove(string)
                            }
                        }
                    }
                }
                it.appendLine(")")

                if (remainingStrings.isNotEmpty()) {
                    throw IllegalStateException("Missing translations in $locale for ${remainingStrings.joinToString()}")
                }
            }
        }
    }

    /**
     * Writes the `Translations.kt` file which maps all locales to the actual translations.
     */
    private fun writeTranslationsFile(locales: List<Locale>) {
        File(targetDirectory.asFile.get(), "Translations.kt").bufferedWriter().use {
            it.write(kotlinFilePreamble())
            it.appendLine("import ${kotlinStringsPackageName.get()}.Translations")
            it.appendLine()
            it.appendLine("""
                /**
                 * Maps each locale tag we have a translation for to a function that creates the translation.
                 */
                internal val TranslationProviderByLocaleTag = mapOf(
            """.trimIndent())
            for (locale in locales) {
                it.appendLine("    \"${locale.toKotlinTag()}\" to " +
                    "Translations::${locale.translationFunctionName()},")

                val newLanguageCode = OldToNewLanguageCode[locale.language]
                if (newLanguageCode != null) {
                    val newLocale = locale.copy(language = newLanguageCode)
                    it.appendLine("    \"${newLocale.toKotlinTag()}\" to " +
                        "Translations::${locale.translationFunctionName()},")
                }
            }
            it.appendLine(")")
        }
    }

    /**
     * The preamble of any Kotlin source files we write.
     */
    @Suppress("HttpUrlsUsage")
    private fun kotlinFilePreamble() = """
        /*
         * Copyright ${Calendar.getInstance().get(Calendar.YEAR)} The Android Open Source Project
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

        package ${targetPackageName.get()}
        
        
    """.trimIndent()

    /**
     * Executes the given command and waits for it to complete.
     *
     * @param dir The working directory for the command.
     * @param command The command, one argument at a time.
     */
    private fun execCommand(dir: File, vararg command: String) {
        println("[$dir] ${command.joinToString(separator = " ")}")
        val process = ProcessBuilder()
            .directory(dir)
            .command(*command)
            .start()
        thread {
            process.errorReader().forEachLine {
                System.err.println(it)
            }
        }
        thread {
            process.inputReader().forEachLine {
                println(it)
            }
        }
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw IOException("Process exited with code $exitCode")
        }
    }
}

/**
 * Represents a locale.
 *
 * @param language The language. May not be empty; use "en" for the default locale.
 * @param region The region, or an empty string.
 */
private data class Locale(val language: String, val region: String) {
    /**
     * Returns the name of the `values` directory corresponding to this locale.
     */
    fun valuesDirName() = when {
        (language == "en") && (region == "") -> "values"
        region == "" -> "values-$language"
        else -> "values-$language-r$region"
    }

    /**
     * Returns the name of the function to generate that returns the translations for this locale.
     */
    fun translationFunctionName(): String {
        val name = when {
            region == "" -> language
            else -> "$language$region"  // Region is all-caps
        }
        return if (name in KotlinKeywords) "`$name`" else name
    }

    /**
     * Returns the locale tag used in Kotlin that corresponds to this locale.
     */
    fun toKotlinTag() = when {
        (language == "en") && (region == "") -> ""
        region == "" -> language
        else -> "${language}_$region"
    }

    override fun toString(): String = when {
        region == "" -> language
        else -> "${language}_$region"
    }

    companion object {
        fun fromTag(tag: String): Locale {
            val (language, region) = "${tag}_".split("_")
            return Locale(language, region)
        }
    }
}

/**
 * Kotlin keywords which we need to escape if we generate a function with the same name.
 */
private val KotlinKeywords = setOf("as", "in", "is")

/**
 * Maps obsolete language codes to their new versions.
 *
 * The Android repos from which the translations are obtained use some obsolete language codes, but
 * Java (starting with 17) uses the new ones. To make things simpler, we map both codes to the same
 * translation.
 */
private val OldToNewLanguageCode = mapOf(
    "iw" to "he",
    "ji" to "yi",
    "in" to "id"
)