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

package androidx.stableaidl.internal;


import com.android.annotations.NonNull;

import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Utility class to ease directory walking and performing actions on files.
 *
 * <p>It's expected for you to use either the static initializer methods or the builder class to
 * create instances of this class.
 *
 * <p>Examples:
 *
 * <pre class='code'>
 *     Path root = FileSystems.getDefault().getPath("/");
 *
 *     // Print out all files under a given path.
 *     DirectoryWalker.builder()
 *         .start(root)
 *         .action((root, path) -> System.out.println(path))
 *         .build()
 *         .walk();
 *
 *     // Print out all .java files under a given path.
 *     DirectoryWalker.builder()
 *         .start(root)
 *         .extensions("java")
 *         .action((root, path) -> System.out.println(path))
 *         .build()
 *         .walk();
 *
 *     // Defer creation of your action class until it is needed. This is useful if the creation
 *     // of your action is expensive (e.g. not a lambda like below, but a bonafide class with
 *     // some non-trivial initialisation), and you expect that no files to be found on the
 *     // directory walk (e.g. they're all filtered out because they don't match the extension
 *     // filter).
 *     DirectoryWalker.builder()
 *         .start(root)
 *         .action(() -> (root, path) -> System.out.println(path))
 *         .build()
 *         .walk();
 * </pre>
 *
 * Cloned from <code>com.android.builder.internal.compiler.DirectoryWalker</code>.
 */
public class DirectoryWalker {

    /** The directory to start walking from. */
    @NonNull private final Path root;

    /**
     * A collection of predicates that, together, decide whether a file should be skipped or acted
     * upon.
     *
     * <p>If a predicate's {@code test()} method returns true, that file will be skipped and the
     * given FileAction will not be run for it.
     */
    @NonNull private final Collection<Predicate<Path>> filters;

    /** The callback to invoke on each file. */
    @NonNull private final FileAction action;

    /**
     * A FileAction represents a unit of work to perform on a file in a directory tree. The {@code
     * call()} method will be called for each file in the tree that do not get filtered out by the
     * list of filters.
     */
    @FunctionalInterface
    public interface FileAction {
        /**
         * Perform work on a file.
         *
         * @param root the directory that this directory walk started at.
         * @param file the current file being acted upon.
         * @throws IOException if anything goes wrong. Implementors of this interface are expected
         *     to either re-wrap their exceptions as IOExceptions, or handle their exceptions
         *     appropriately.
         */
        void call(@NonNull Path root, @NonNull Path file) throws IOException;
    }

    /**
     * A convenience class for creating filters that only allow certain extensions through.
     *
     * <p>Instead of using this class directory, instead use the {@code extensions()} method on the
     * DirectoryWalker.Builder class.
     */
    private static class ExtensionSelector implements Predicate<Path> {
        @NonNull private final Set<String> allowedExtensions;

        /** @param allowedExtensions the extensions that you want to let pass through. */
        public ExtensionSelector(String... allowedExtensions) {
            this.allowedExtensions = Sets.newHashSet(allowedExtensions);
        }

        @Override
        public boolean test(Path path) {
            return allowedExtensions.contains(getExtension(path));
        }

        @NonNull
        private static String getExtension(Path path) {
            return com.google.common.io.Files.getFileExtension(path.toAbsolutePath().toString());
        }
    }

    public static class Builder {
        private Path root;
        @NonNull private final Collection<Predicate<Path>> filters = Lists.newLinkedList();
        private FileAction action;

        private Builder() {}

        /**
         * Set the path to start traversal from. If left unset, the {@code build()} method will
         * throw an exception.
         *
         * @param root path to start traversal from.
         * @return itself.
         */
        @NonNull
        public Builder root(@NonNull Path root) {
            Preconditions.checkArgument(root != null, "cannot pass in a null root directory");
            this.root = root;
            return this;
        }

        /**
         * Adds an extension filter to this DirectoryWalker. Any extensions passed in to this method
         * will be allowed to pass through the filter, so you can use this method to declare what
         * types of file you are interested in acting on.
         *
         * @param extensions the extensions you're interested in.
         * @return itself.
         */
        @NonNull
        public Builder extensions(String... extensions) {
            Preconditions.checkArgument(
                    extensions.length > 0, "cannot pass in an empty array of extensions");

            for (String ext : extensions) {
                Preconditions.checkArgument(ext != null, "cannot pass in a null extension");
                Preconditions.checkArgument(ext.length() > 0, "cannot pass in an empty extension");
            }

            return select(new ExtensionSelector(extensions));
        }

        /**
         * Add an arbitrary filter of type {@code Predicate<Path>}. Filters are run for every single
         * file that is found while doing a walk. If the {@code test()} method in the predicate
         * returns {@code true}, that file will not be passed to the {@code FileAction}.
         *
         * <p>A useful side effect of using filters is that if you filter out all of the encountered
         * files and you're using a supplied action (see {@code action(Supplier<FileAction>}), the
         * supplier's {@code get()} method will not be called.
         *
         * @return itself.
         */
        @NonNull
        public Builder filter(@NonNull Predicate<Path> filter) {
            Preconditions.checkArgument(filter != null, "cannot pass in a null filter");
            filters.add(filter);
            return this;
        }

        /**
         * Adds an arbitrary selector of type {@code Predicate<Path>}. A selector is exactly the
         * same as a filter, except that the condition is negated. So if a selector's {@code test()}
         * method returns {@code false}, that file will not be passed to the {@code FileAction}.
         *
         * <p>See {@code filter()} for more information.
         *
         * @return itself.
         */
        @NonNull
        public Builder select(@NonNull Predicate<Path> selector) {
            return filter((path) -> !selector.test(path));
        }

        /**
         * The action you wish to perform on each file. If left unset, the {@code build()} method
         * will throw an exception.
         *
         * @param action the action you wish to perform.
         * @return itself.
         */
        @NonNull
        public Builder action(@NonNull FileAction action) {
            Preconditions.checkArgument(action != null, "cannot pass in a null action");
            this.action = action;
            return this;
        }

        /**
         * Sometimes it could be quite expensive to initialise a FileAction, and you want to avoid
         * doing it if there's no actual work to do. Passing a {@code Supplier<FileAction>} instead
         * of a {@code FileAction} allows you to do this.
         *
         * <p>The {@code get()} method on the supplier is guaranteed to be called either 0 (if no
         * work needs to be done) or 1 time. It cannot return null (doing so will result in a
         * NullPointerException being thrown).
         *
         * <pre class='code'>
         *     DirectoryWalker.builder()
         *         .root(Paths.get("/"))
         *         .action(() -> new MyExpensiveFileAction())
         *         .build()
         *         .walk();
         * </pre>
         *
         * @return itself.
         */
        @NonNull
        public Builder action(@NonNull Supplier<? extends FileAction> supplier) {
            Preconditions.checkArgument(supplier != null, "cannot pass in a null actionSupplier");

            final Supplier<? extends FileAction> action = Suppliers.memoize(supplier::get);
            this.action =
                    (root, path) -> {
                        FileAction fa = action.get();
                        Preconditions.checkNotNull(fa, "action supplier cannot return null action");
                        fa.call(root, path);
                    };

            return this;
        }

        /**
         * Build an instance of DirectoryWalker. This method will throw an exception if any of the
         * given parameters are incorrect.
         *
         * @return a DirectoryWalker.
         */
        @NonNull
        public DirectoryWalker build() {
            Preconditions.checkArgument(action != null, "action cannot be left unset");
            Preconditions.checkArgument(root != null, "root cannot be left unset");
            return new DirectoryWalker(action, root, Collections.unmodifiableCollection(filters));
        }
    }

    /**
     * If none of the static initiators work for you (e.g. DirectoryWalker.walk), using the builder
     * is the expected way to create custom DirectoryWalkers.
     *
     * @return a new DirectoryWalker.Builder class that you can use to build an instance of
     *     DirectoryWalker.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Triggers a directory walk.
     *
     * <p>This can be called multiple times over the lifetime of the object, but note that each walk
     * will use the same action. If you're modifying state in your action, you need to take this in
     * to account.
     *
     * @throws IOException if anything goes wrong.
     */
    public DirectoryWalker walk() throws IOException {
        // This behaviour is preserved from the previous implementation of this class. Ideally, a
        // non-existent file passed to this class would raise an exception, but in this case we do
        // not.
        if (!Files.exists(root)) {
            return this;
        }

        Set<FileVisitOption> options =
                Sets.newEnumSet(Arrays.asList(FileVisitOption.FOLLOW_LINKS), FileVisitOption.class);

        Files.walkFileTree(
                root,
                options,
                Integer.MAX_VALUE,
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                            throws IOException {
                        if (!shouldSkipPath(path)) {
                            action.call(root, path);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });

        return this;
    }

    private DirectoryWalker(
            @NonNull FileAction action,
            @NonNull Path start,
            @NonNull Collection<Predicate<Path>> filters) {
        this.action = action;
        this.root = start;
        this.filters = filters;
    }

    private boolean shouldSkipPath(Path path) {
        return filters.stream().anyMatch(predicate -> predicate.test(path));
    }
}
