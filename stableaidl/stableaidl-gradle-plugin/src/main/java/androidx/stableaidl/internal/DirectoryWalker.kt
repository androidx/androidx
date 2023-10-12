/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.stableaidl.internal

import androidx.stableaidl.internal.DirectoryWalker.FileAction
import com.google.common.base.Preconditions
import com.google.common.base.Suppliers
import com.google.common.collect.Lists
import com.google.common.collect.Sets
import com.google.common.io.Files
import java.io.IOException
import java.nio.file.FileVisitOption
import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.Collections
import java.util.function.Predicate
import java.util.function.Supplier

/**
 * Utility class to ease directory walking and performing actions on files.
 *
 *
 * It's expected for you to use either the static initializer methods or the builder class to
 * create instances of this class.
 *
 *
 * Examples:
 *
 * <pre class='code'>
 * Path root = FileSystems.getDefault().getPath("/");
 *
 * // Print out all files under a given path.
 * DirectoryWalker.builder()
 * .start(root)
 * .action((root, path) -> System.out.println(path))
 * .build()
 * .walk();
 *
 * // Print out all .java files under a given path.
 * DirectoryWalker.builder()
 * .start(root)
 * .extensions("java")
 * .action((root, path) -> System.out.println(path))
 * .build()
 * .walk();
 *
 * // Defer creation of your action class until it is needed. This is useful if the creation
 * // of your action is expensive (e.g. not a lambda like below, but a bona fide class with
 * // some non-trivial initialisation), and you expect that no files to be found on the
 * // directory walk (e.g. they're all filtered out because they don't match the extension
 * // filter).
 * DirectoryWalker.builder()
 * .start(root)
 * .action(() -> (root, path) -> System.out.println(path))
 * .build()
 * .walk();
 * </pre>
 *
 * Cloned from `com.android.builder.internal.compiler.DirectoryWalker`.
 */
class DirectoryWalker internal constructor(
    /** The callback to invoke on each file.  */
    private val action: FileAction,
    /** The directory to start walking from.  */
    private val root: Path,
    /**
     * A collection of predicates that, together, decide whether a file should be skipped or acted
     * upon.
     *
     *
     * If a predicate's `test()` method returns true, that file will be skipped and the
     * given FileAction will not be run for it.
     */
    private val filters: Collection<Predicate<Path>>
) {
    /**
     * A FileAction represents a unit of work to perform on a file in a directory tree. The `call()` method will be called for each file in the tree that do not get filtered out by the
     * list of filters.
     */
    fun interface FileAction {
        /**
         * Perform work on a file.
         *
         * @param root the directory that this directory walk started at.
         * @param file the current file being acted upon.
         * @throws IOException if anything goes wrong. Implementors of this interface are expected
         * to either re-wrap their exceptions as IOExceptions, or handle their exceptions
         * appropriately.
         */
        @Throws(IOException::class)
        fun call(root: Path, file: Path)
    }

    /**
     * A convenience class for creating filters that only allow certain extensions through.
     *
     *
     * Instead of using this class directory, instead use the `extensions()` method on the
     * DirectoryWalker.Builder class.
     *
     * @param allowedExtensions the extensions that you want to let pass through.
     */
    private class ExtensionSelector(allowedExtensions: Set<String>) : Predicate<Path> {
        private val allowedExtensions: Set<String>

        init {
            this.allowedExtensions = Sets.newHashSet<String>(allowedExtensions)
        }

        override fun test(path: Path): Boolean {
            return allowedExtensions.contains(getExtension(path))
        }

        companion object {
            internal fun getExtension(path: Path): String {
                return Files.getFileExtension(path.toAbsolutePath().toString())
            }
        }
    }

    class Builder {
        private var root: Path? = null
        private val filters: MutableCollection<Predicate<Path>> = Lists.newLinkedList()
        private var action: FileAction? = null

        /**
         * Set the path to start traversal from. If left unset, the `build()` method will
         * throw an exception.
         *
         * @param root path to start traversal from.
         * @return itself.
         */
        fun root(root: Path): Builder {
            this.root = root
            return this
        }

        /**
         * Adds an extension filter to this DirectoryWalker. Any extensions passed in to this method
         * will be allowed to pass through the filter, so you can use this method to declare what
         * types of file you are interested in acting on.
         *
         * @param extensions the extensions you're interested in.
         * @return itself.
         */
        fun extensions(vararg extensions: String): Builder {
            return extensions(setOf(*extensions))
        }

        /**
         * Adds an extension filter to this DirectoryWalker. Any extensions passed in to this method
         * will be allowed to pass through the filter, so you can use this method to declare what
         * types of file you are interested in acting on.
         *
         * @param extensions the extensions you're interested in.
         * @return itself.
         */
        private fun extensions(extensions: Set<String>): Builder {
            Preconditions.checkArgument(
                extensions.isNotEmpty(), "cannot pass in an empty array of extensions"
            )
            for (ext in extensions) {
                Preconditions.checkArgument(ext.isNotEmpty(), "cannot pass in an empty extension")
            }
            return select(ExtensionSelector(extensions))
        }

        /**
         * Add an arbitrary filter of type `Predicate<Path>`. Filters are run for every single
         * file that is found while doing a walk. If the `test()` method in the predicate
         * returns `true`, that file will not be passed to the `FileAction`.
         *
         *
         * A useful side effect of using filters is that if you filter out all of the encountered
         * files and you're using a supplied action (see `action(Supplier<FileAction>`), the
         * supplier's `get()` method will not be called.
         *
         * @return itself.
         */
        private fun filter(filter: Predicate<Path>): Builder {
            filters.add(filter)
            return this
        }

        /**
         * Adds an arbitrary selector of type `Predicate<Path>`. A selector is exactly the
         * same as a filter, except that the condition is negated. So if a selector's `test()`
         * method returns `false`, that file will not be passed to the `FileAction`.
         *
         *
         * See `filter()` for more information.
         *
         * @return itself.
         */
        private fun select(selector: Predicate<Path>): Builder {
            return filter { path: Path -> !selector.test(path) }
        }

        /**
         * The action you wish to perform on each file. If left unset, the `build()` method
         * will throw an exception.
         *
         * @param action the action you wish to perform.
         * @return itself.
         */
        fun action(action: FileAction): Builder {
            this.action = action
            return this
        }

        /**
         * Sometimes it could be quite expensive to initialise a FileAction, and you want to avoid
         * doing it if there's no actual work to do. Passing a `Supplier<FileAction>` instead
         * of a `FileAction` allows you to do this.
         *
         *
         * The `get()` method on the supplier is guaranteed to be called either 0 (if no
         * work needs to be done) or 1 time. It cannot return null (doing so will result in a
         * NullPointerException being thrown).
         *
         * <pre class='code'>
         * DirectoryWalker.builder()
         * .root(Paths.get("/"))
         * .action(() -> new MyExpensiveFileAction())
         * .build()
         * .walk();
        </pre> *
         *
         * @return itself.
         */
        fun action(supplier: Supplier<out FileAction>): Builder {
            val action: Supplier<out FileAction> = Suppliers.memoize { supplier.get() }
            this.action = FileAction { root: Path, path: Path ->
                action.get().call(root, path)
            }
            return this
        }

        /**
         * Build an instance of DirectoryWalker. This method will throw an exception if any of the
         * given parameters are incorrect.
         *
         * @return a DirectoryWalker.
         */
        fun build(): DirectoryWalker {
            Preconditions.checkArgument(action != null, "action cannot be left unset")
            Preconditions.checkArgument(root != null, "root cannot be left unset")
            return DirectoryWalker(action!!, root!!, Collections.unmodifiableCollection(filters))
        }
    }

    /**
     * Triggers a directory walk.
     *
     *
     * This can be called multiple times over the lifetime of the object, but note that each walk
     * will use the same action. If you're modifying state in your action, you need to take this in
     * to account.
     *
     * @throws IOException if anything goes wrong.
     */
    @Throws(IOException::class)
    fun walk(): DirectoryWalker {
        // This behaviour is preserved from the previous implementation of this class. Ideally, a
        // non-existent file passed to this class would raise an exception, but in this case we do
        // not.
        if (!java.nio.file.Files.exists(root)) {
            return this
        }
        val options: Set<FileVisitOption> = Sets.newEnumSet(
            listOf(FileVisitOption.FOLLOW_LINKS),
            FileVisitOption::class.java
        )
        java.nio.file.Files.walkFileTree(
            root,
            options, Int.MAX_VALUE,
            object : SimpleFileVisitor<Path>() {
                @Throws(IOException::class)
                override fun visitFile(path: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (!shouldSkipPath(path)) {
                        action.call(root, path)
                    }
                    return FileVisitResult.CONTINUE
                }
            })
        return this
    }

    internal fun shouldSkipPath(path: Path): Boolean {
        return filters.stream().anyMatch { predicate: Predicate<Path> -> predicate.test(path) }
    }

    companion object {
        /**
         * If none of the static initiators work for you (e.g. DirectoryWalker.walk), using the builder
         * is the expected way to create custom DirectoryWalkers.
         *
         * @return a new DirectoryWalker.Builder class that you can use to build an instance of
         * DirectoryWalker.
         */
        fun builder(): Builder {
            return Builder()
        }
    }
}
