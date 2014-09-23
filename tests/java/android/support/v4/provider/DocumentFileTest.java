/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v4.provider;

import android.content.ContentResolver;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Environment;
import android.os.FileUtils;
import android.os.SystemClock;
import android.support.v4.provider.DocumentFile;
import android.test.AndroidTestCase;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Tests for {@link DocumentFile}
 */
public class DocumentFileTest extends AndroidTestCase {

    private Uri treeUri;

    private File root;
    private File rootFoo;
    private File rootMeow;
    private File rootMeowCat;
    private File rootMeowDog;
    private File rootMeowBar;

    private static final String FOO = "foo.randomext";
    private static final String MEOW = "meow";
    private static final String CAT = "cat.jpg";
    private static final String DOG = "DOG.PDF";
    private static final String BAR = "bar.png";

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        final ContentResolver resolver = getContext().getContentResolver();
        final List<UriPermission> perms = resolver.getPersistedUriPermissions();

        if (perms.isEmpty()) {
            throw new RuntimeException(
                    "Failed to find outstanding grant; did you run the activity first?");
        } else {
            treeUri = perms.get(0).getUri();
        }

        root = Environment.getExternalStorageDirectory();
        rootFoo = new File(root, FOO);
        rootMeow = new File(root, MEOW);
        rootMeowCat = new File(rootMeow, CAT);
        rootMeowDog = new File(rootMeow, DOG);
        rootMeowBar = new File(rootMeow, BAR);

        resetRoot();
    }

    private void resetRoot() throws Exception {
        final File tmp = new File(root, "bark.pdf");
        FileUtils.deleteContents(tmp);
        tmp.delete();

        FileUtils.deleteContents(rootMeow);
        rootMeow.mkdir();
        rootMeowBar.mkdir();

        writeInt(rootFoo, 12);
        writeInt(rootMeowCat, 24);
        writeInt(rootMeowDog, 48);
    }

    private interface DocumentTest {
        public void exec(DocumentFile doc) throws Exception;
    }

    public void testSimple() throws Exception {
        final DocumentTest test = new DocumentTest() {
            @Override
            public void exec(DocumentFile doc) throws Exception {
                resetRoot();
                assertTrue("isDirectory", doc.isDirectory());
                assertFalse("isFile", doc.isFile());
                assertTrue("canRead", doc.canRead());
                assertTrue("canWrite", doc.canWrite());
                assertTrue("exists", doc.exists());
            }
        };

        test.exec(DocumentFile.fromFile(root));
        test.exec(DocumentFile.fromTreeUri(getContext(), treeUri));
    }

    public void testTraverse() throws Exception {
        final DocumentTest test = new DocumentTest() {
            @Override
            public void exec(DocumentFile doc) throws Exception {
                resetRoot();

                // Root needs to at least contain our test file and dir
                final DocumentFile foo = doc.findFile(FOO);
                final DocumentFile meow = doc.findFile(MEOW);
                assertTrue("isFile", foo.isFile());
                assertTrue("isDirectory", meow.isDirectory());

                // Traverse inside, and expect to find exact number of items
                DocumentFile[] docs = meow.listFiles();
                assertEquals("length", 3, docs.length);

                final DocumentFile cat = meow.findFile(CAT);
                final DocumentFile dog = meow.findFile(DOG);
                final DocumentFile bar = meow.findFile(BAR);
                assertTrue("isFile", cat.isFile());
                assertTrue("isFile", dog.isFile());
                assertTrue("isDirectory", bar.isDirectory());

                // Empty directory is empty
                assertEquals("length", 0, bar.listFiles().length);
            }
        };

        test.exec(DocumentFile.fromFile(root));
        test.exec(DocumentFile.fromTreeUri(getContext(), treeUri));
    }

    public void testReadAndWrite() throws Exception {
        final DocumentTest test = new DocumentTest() {
            @Override
            public void exec(DocumentFile doc) throws Exception {
                resetRoot();

                final DocumentFile foo = doc.findFile(FOO);
                assertEquals("file", 12, readInt(rootFoo));
                assertEquals("uri", 12, readInt(foo.getUri()));

                // Underlying storage may not have sub-second resolution, so
                // wait a few seconds.
                SystemClock.sleep(2000);

                // Ensure provider write makes its way to disk
                final long beforeTime = foo.lastModified();
                writeInt(foo.getUri(), 13);
                final long afterTime = foo.lastModified();

                assertEquals("file", 13, readInt(rootFoo));
                assertEquals("uri", 13, readInt(foo.getUri()));

                // Make sure we kicked time forward
                assertTrue("lastModified", afterTime > beforeTime);
            }
        };

        test.exec(DocumentFile.fromFile(root));
        test.exec(DocumentFile.fromTreeUri(getContext(), treeUri));
    }

    public void testMimes() throws Exception {
        final DocumentTest test = new DocumentTest() {
            @Override
            public void exec(DocumentFile doc) throws Exception {
                resetRoot();

                final DocumentFile foo = doc.findFile(FOO);
                final DocumentFile meow = doc.findFile(MEOW);
                final DocumentFile cat = meow.findFile(CAT);
                final DocumentFile dog = meow.findFile(DOG);
                final DocumentFile bar = meow.findFile(BAR);

                assertEquals(null, doc.getType());
                assertEquals("application/octet-stream", foo.getType());
                assertEquals(null, meow.getType());
                assertEquals("image/jpeg", cat.getType());
                assertEquals("application/pdf", dog.getType());
                assertEquals(null, bar.getType());
            }
        };

        test.exec(DocumentFile.fromFile(root));
        test.exec(DocumentFile.fromTreeUri(getContext(), treeUri));
    }

    public void testCreate() throws Exception {
        final DocumentTest test = new DocumentTest() {
            @Override
            public void exec(DocumentFile doc) throws Exception {
                resetRoot();

                final DocumentFile meow = doc.findFile(MEOW);
                assertEquals("length", 3, meow.listFiles().length);

                // Create file with MIME
                final DocumentFile newFile = meow.createFile("text/plain", "My New File");
                assertEquals("My New File.txt", newFile.getName());
                assertEquals("text/plain", newFile.getType());
                assertTrue("isFile", newFile.isFile());
                assertFalse("isDirectory", newFile.isDirectory());

                assertEquals("length", 0, newFile.length());
                writeInt(newFile.getUri(), 0);
                assertEquals("length", 4, newFile.length());

                // Create raw file
                final DocumentFile newRaw = meow.createFile("application/octet-stream",
                        "myrawfile");
                assertEquals("myrawfile", newRaw.getName());
                assertEquals("application/octet-stream", newRaw.getType());
                assertTrue("isFile", newRaw.isFile());
                assertFalse("isDirectory", newRaw.isDirectory());

                // Create directory
                final DocumentFile newDir = meow.createDirectory("My New Directory.png");
                assertEquals("My New Directory.png", newDir.getName());
                assertFalse("isFile", newDir.isFile());
                assertTrue("isDirectory", newDir.isDirectory());
                assertEquals("length", 0, newDir.listFiles().length);

                // And overall dir grew
                assertEquals("length", 6, meow.listFiles().length);
            }
        };

        test.exec(DocumentFile.fromFile(root));
        test.exec(DocumentFile.fromTreeUri(getContext(), treeUri));
    }

    public void testDelete() throws Exception {
        final DocumentTest test = new DocumentTest() {
            @Override
            public void exec(DocumentFile doc) throws Exception {
                resetRoot();

                final DocumentFile meow = doc.findFile(MEOW);
                final DocumentFile cat = meow.findFile(CAT);
                final DocumentFile dog = meow.findFile(DOG);

                // Delete single file
                assertTrue(cat.delete());
                assertNull("cat", meow.findFile(CAT));

                // Other file still exists
                assertTrue("exists", dog.exists());

                // Delete entire tree
                assertTrue(meow.delete());
                assertNull("meow", doc.findFile(MEOW));

                // Nuking tree deleted other file
                assertFalse("exists", dog.exists());
            }
        };

        test.exec(DocumentFile.fromFile(root));
        test.exec(DocumentFile.fromTreeUri(getContext(), treeUri));
    }

    public void testRename() throws Exception {
        final DocumentTest test = new DocumentTest() {
            @Override
            public void exec(DocumentFile doc) throws Exception {
                resetRoot();

                DocumentFile meow = doc.findFile(MEOW);
                DocumentFile cat = meow.findFile(CAT);
                DocumentFile dog = meow.findFile(DOG);
                assertTrue(dog.exists());

                // Rename a file
                assertEquals("cat.jpg", cat.getName());
                assertEquals("image/jpeg", cat.getType());

                assertTrue(cat.renameTo("music.aAc"));
                assertEquals("music.aAc", cat.getName());
                assertEquals("audio/aac", cat.getType());

                // Rename a directory
                assertEquals("meow", meow.getName());
                assertEquals(null, meow.getType());
                assertTrue(meow.isDirectory());
                assertEquals(3, meow.listFiles().length);

                assertTrue(meow.renameTo("bark.pdf"));
                assertEquals("bark.pdf", meow.getName());
                assertEquals(null, meow.getType());
                assertTrue(meow.isDirectory());
                assertEquals(3, meow.listFiles().length);

                // Current implementation of ExternalStorageProvider invalidates
                // all children documents when directory is renamed.
                assertFalse(dog.exists());

                // But we can find it again
                dog = meow.findFile(DOG);
                assertTrue(dog.exists());
            }
        };

        test.exec(DocumentFile.fromFile(root));
        test.exec(DocumentFile.fromTreeUri(getContext(), treeUri));
    }

    private void writeInt(Uri uri, int value) throws IOException {
        final DataOutputStream os = new DataOutputStream(
                getContext().getContentResolver().openOutputStream(uri));
        try {
            os.writeInt(value);
        } finally {
            os.close();
        }
    }

    private static void writeInt(File file, int value) throws IOException {
        final DataOutputStream os = new DataOutputStream(new FileOutputStream(file));
        try {
            os.writeInt(value);
        } finally {
            os.close();
        }
    }

    private int readInt(Uri uri) throws IOException {
        final DataInputStream is = new DataInputStream(
                getContext().getContentResolver().openInputStream(uri));
        try {
            return is.readInt();
        } finally {
            is.close();
        }
    }

    private static int readInt(File file) throws IOException {
        final DataInputStream is = new DataInputStream(new FileInputStream(file));
        try {
            return is.readInt();
        } finally {
            is.close();
        }
    }
}
