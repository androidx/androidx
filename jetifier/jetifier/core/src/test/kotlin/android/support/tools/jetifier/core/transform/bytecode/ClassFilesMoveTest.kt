package android.support.tools.jetifier.core.transform.bytecode

import android.support.tools.jetifier.core.Processor
import android.support.tools.jetifier.core.archive.Archive
import android.support.tools.jetifier.core.archive.ArchiveFile
import android.support.tools.jetifier.core.archive.ArchiveItemVisitor
import android.support.tools.jetifier.core.config.Config
import android.support.tools.jetifier.core.map.TypesMap
import android.support.tools.jetifier.core.rules.JavaType
import android.support.tools.jetifier.core.rules.RewriteRule
import android.support.tools.jetifier.core.transform.proguard.ProGuardTypesMap
import com.google.common.truth.Truth
import org.junit.Test
import java.io.File

/**
 * Tests that individual files were moved properly due to their owner types rewrites.
 */
class ClassFilesMoveTest {

    companion object {
        private val TEST_CONFIG = Config(
            restrictToPackagePrefixes = listOf("android/support"),
            rewriteRules = listOf(
                RewriteRule("android/support/annotation/(.*)", "ignore"),
                RewriteRule("android/support/v7/preference/R(.*)", "ignore"),
                RewriteRule("android/support/v4/(.*)", "ignore")
            ),
            pomRewriteRules = emptyList(),
            typesMap = TypesMap(mapOf(
                "android/support/v7/preference/Preference"
                    to "androidx/support/preference/Preference",
                "android/support/v7/preference/Preference\$1"
                    to "androidx/support/preference/Preference\$1",
                "android/support/v7/preference/TwoStatePreference"
                    to "androidx/support/preference/TwoStatePreference",
                "android/support/v7/preference/PreferenceGroup"
                    to "androidx/support/preference/PreferenceGroup",
                "android/support/v7/preference/Preference\$OnPreferenceChangeListener"
                    to "androidx/support/preference/Preference\$OnPreferenceChangeListener",
                "android/support/v7/preference/Preference\$OnPreferenceClickListener"
                    to "androidx/support/preference/Preference\$OnPreferenceClickListener",
                "android/support/v7/preference/Preference\$OnPreferenceChangeInternalListener"
                    to "androidx/support/preference/Preference\$OnPreferenceChangeInternalListener",
                "android/support/v7/preference/PreferenceManager\$OnPreferenceTreeClickListener"
                    to "androidx/support/preference/PreferenceManager\$OnPreferenceTreeClickLisnr",
                "android/support/v7/preference/PreferenceViewHolder"
                    to "androidx/support/preference/PreferenceViewHolder",
                "android/support/v7/preference/PreferenceManager"
                    to "androidx/support/preference/PreferenceManager",
                "android/support/v14/preference/SwitchPreference"
                    to "androidx/support/preference/SwitchPreference",
                "android/support/v14/preference/SwitchPreference\$1"
                    to "androidx/support/preference/SwitchPreference\$1",
                "android/support/v14/preference/SwitchPreference\$Listener"
                    to "androidx/support/preference/SwitchPreference\$Listener",
                "android/support/v7/preference/PreferenceDataStore"
                    to "androidx/support/preference/PreferenceDataStore",
                "android/support/v7/preference/Preference\$BaseSavedState"
                    to "androidx/support/preference/Preference\$BaseSavedState"
            ).map { JavaType(it.key) to JavaType(it.value) }.toMap()),
            proGuardMap = ProGuardTypesMap.EMPTY
        )
    }

    /**
     * Tests that after rewrite of a input archive the internal classes are properly moved to new
     * locations (based on the rewrite rules) which is compared with the expected archive.
     *
     * Note: The expected archive does not contain rewritten classes - they were only manually
     * moved. Which is fine because this test validates only files locations.
     *
     * Note: This runs in support library rewrite mode which allows to move classes around.
     */
    @Test fun fileMove_forwardRewrite_shouldMoveFilesProperly() {
        val inputZipPath = "/fileRenameTest/inputTestLib.zip"
        val expectedZipPath = "/fileRenameTest/expectedTestLib.zip"

        val processor = Processor.createProcessor(TEST_CONFIG, rewritingSupportLib = true)
        val inputFile = File(javaClass.getResource(inputZipPath).file)

        val tempDir = createTempDir()
        val result = processor.transform(setOf(inputFile), tempDir.toPath())

        Truth.assertThat(result.filesToAdd).hasSize(1)
        testArchivesAreSame(result.filesToAdd.first(),
            File(javaClass.getResource(expectedZipPath).file))

        tempDir.delete()
    }

    /**
     * Does exactly the same as [fileMove_forwardRewrite_nestedArchive_shouldMoveFilesProperly] but
     * the files are in a nested archive e.g. archive.zip/classes.jar/some files.
     */
    @Test fun fileMove_forwardRewrite_nestedArchive_shouldMoveFilesProperly() {
        val inputZipPath = "/fileRenameTest/inputTestLibNested.zip"
        val expectedZipPath = "/fileRenameTest/expectedTestLibNested.zip"

        val processor = Processor.createProcessor(TEST_CONFIG, rewritingSupportLib = true)
        val inputFile = File(javaClass.getResource(inputZipPath).file)

        val tempDir = createTempDir()
        val result = processor.transform(setOf(inputFile), tempDir.toPath())

        Truth.assertThat(result.filesToAdd).hasSize(1)
        testArchivesAreSame(result.filesToAdd.first(),
            File(javaClass.getResource(expectedZipPath).file))

        tempDir.delete()
    }

    /**
     * Rewrites the input archive and then applies reversed mode to rewrite it back. The final
     * produced archive has to have the same directory structure as the input one.
     *
     * Note: This runs in support library rewrite mode which allows to move classes around.
     */
    @Test fun fileMove_forwardRewrite_backwardsRewrite_shouldKeepFilesProperly() {
        val inputZipPath = "/fileRenameTest/inputTestLib.zip"

        // Transform forward
        val processor = Processor.createProcessor(TEST_CONFIG,
            rewritingSupportLib = true)
        val inputFile = File(javaClass.getResource(inputZipPath).file)
        val tempDir = createTempDir()
        val result = processor.transform(setOf(inputFile), tempDir.toPath())

        // Take previous result & reverse it
        val processor2 = Processor.createProcessor(TEST_CONFIG,
            rewritingSupportLib = true,
            reversedMode = true)
        val result2 = processor2.transform(setOf(result.filesToAdd.first()), tempDir.toPath())

        testArchivesAreSame(result2.filesToAdd.first(),
            File(javaClass.getResource(inputZipPath).file))

        tempDir.delete()
    }

    /**
     * Runs the rewrite but with support library rewrite mode off which means that none of the files
     * should be moved.
     */
    @Test fun fileMove_forwardRewrite_noSupportLibMode_noFilesMove() {
        val inputZipPath = "/fileRenameTest/inputTestLib.zip"

        val processor = Processor.createProcessor(TEST_CONFIG, rewritingSupportLib = false)
        val inputFile = File(javaClass.getResource(inputZipPath).file)

        val tempDir = createTempDir()
        val result = processor.transform(setOf(inputFile), tempDir.toPath())

        Truth.assertThat(result.filesToAdd).hasSize(1)
        testArchivesAreSame(result.filesToAdd.first(),
            File(javaClass.getResource(inputZipPath).file))

        tempDir.delete()
    }

    fun testArchivesAreSame(givenZip: File, expectedZip: File) {
        testArchivesAreSame(Archive.Builder.extract(givenZip), Archive.Builder.extract(expectedZip))
    }

    fun testArchivesAreSame(givenZip: Archive, expectedZip: Archive) {
        val givenFiles = ArchiveBrowser.grabAllPathsIn(givenZip)
        val expectedFiles = ArchiveBrowser.grabAllPathsIn(expectedZip)
        Truth.assertThat(givenFiles).containsExactlyElementsIn(expectedFiles)
    }

    /**
     * Just a helper utility to get all file paths in the archive.
     */
    class ArchiveBrowser : ArchiveItemVisitor {

        companion object {
            fun grabAllPathsIn(archive: Archive): MutableSet<String> {
                val grabber = ArchiveBrowser()
                archive.accept(grabber)
                return grabber.allPaths
            }
        }

        val allPaths = mutableSetOf<String>()

        override fun visit(archiveFile: ArchiveFile) {
            allPaths.add(archiveFile.relativePath.toString())
            println("Visited ${archiveFile.relativePath}")
        }

        override fun visit(archive: Archive) {
            archive.files.forEach { it.accept(this) }
        }
    }
}