/*
 * The MIT License
 *
 * Copyright 2015 Jesse Glick.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.util;

import com.google.common.collect.ImmutableSet;
import hudson.FilePath;
import hudson.Functions;
import hudson.Util;
import hudson.model.TaskListener;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.Issue;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

public class VirtualFileTest {

    @Rule public TemporaryFolder tmp = new TemporaryFolder();
    
    @Issue("SECURITY-162")
    @Test public void outsideSymlinks() throws Exception {
        assumeFalse("Symlinks don't work well on Windows", Functions.isWindows());
        File ws = tmp.newFolder("ws");
        FileUtils.write(new File(ws, "safe"), "safe", StandardCharsets.US_ASCII, false);
        Util.createSymlink(ws, "safe", "supported", TaskListener.NULL);
        File other = tmp.newFolder("other");
        FileUtils.write(new File(other, "secret"), "s3cr3t", StandardCharsets.US_ASCII, false);
        Util.createSymlink(ws, "../other/secret", "hack", TaskListener.NULL);
        VirtualFile root = VirtualFile.forFile(ws);
        VirtualFile supported = root.child("supported");
        assertTrue(supported.isFile());
        assertTrue(supported.exists());
        assertEquals("safe", IOUtils.toString(supported.open(), (String) null));
        VirtualFile hack = root.child("hack");
        assertFalse(hack.isFile());
        assertFalse(hack.exists());
        try {
            hack.open();
            fail();
        } catch (FileNotFoundException | NoSuchFileException x) {
            // OK
        }
    }

    @Issue("JENKINS-26810")
    @Test public void mode() throws Exception {
        File f = tmp.newFile();
        VirtualFile vf = VirtualFile.forFile(f);
        FilePath fp = new FilePath(f);
        VirtualFile vfp = VirtualFile.forFilePath(fp);
        assertEquals(modeString(hudson.util.IOUtils.mode(f)), modeString(vf.mode()));
        assertEquals(modeString(hudson.util.IOUtils.mode(f)), modeString(vfp.mode()));
        fp.chmod(0755); // no-op on Windows, but harmless
        assertEquals(modeString(hudson.util.IOUtils.mode(f)), modeString(vf.mode()));
        assertEquals(modeString(hudson.util.IOUtils.mode(f)), modeString(vfp.mode()));
    }
    private static String modeString(int mode) throws IOException {
        return mode == -1 ? "N/A" : PosixFilePermissions.toString(Util.modeToPermissions(mode));
    }

    @Issue("JENKINS-26810")
    @Test public void list() throws Exception {
        File root = tmp.getRoot();
        FilePath rootF = new FilePath(root);
        Set<String> paths = ImmutableSet.of("top.txt", "sub/mid.txt", "sub/subsub/lowest.txt", ".hg/config.txt", "very/deep/path/here");
        for (String path : paths) {
            rootF.child(path).write("", null);
        }
        for (VirtualFile vf : new VirtualFile[] {VirtualFile.forFile(root), VirtualFile.forFilePath(rootF), new Ram(paths.stream().map(p -> "/" + p).collect(Collectors.toSet()), "")}) {
            System.err.println("testing " + vf.getClass().getName());
            assertEquals("[.hg/config.txt, sub/mid.txt, sub/subsub/lowest.txt, top.txt]", new TreeSet<>(vf.list("**/*.txt", null, false)).toString());
            assertEquals("[sub/mid.txt, sub/subsub/lowest.txt, top.txt]", new TreeSet<>(vf.list("**/*.txt", null, true)).toString());
            assertEquals("[.hg/config.txt, sub/mid.txt, sub/subsub/lowest.txt, top.txt, very/deep/path/here]", new TreeSet<>(vf.list("**", null, false)).toString());
            assertEquals("[]", new TreeSet<>(vf.list("", null, false)).toString());
            assertEquals("[sub/mid.txt, sub/subsub/lowest.txt]", new TreeSet<>(vf.list("sub/", null, false)).toString());
            assertEquals("[sub/mid.txt]", new TreeSet<>(vf.list("sub/", "sub/subsub/", false)).toString());
            assertEquals("[sub/mid.txt]", new TreeSet<>(vf.list("sub/", "sub/subsub/**", false)).toString());
            assertEquals("[sub/mid.txt]", new TreeSet<>(vf.list("sub/", "**/subsub/", false)).toString());
            assertEquals("[.hg/config.txt, sub/mid.txt]", new TreeSet<>(vf.list("**/mid*,**/conf*", null, false)).toString());
            assertEquals("[sub/mid.txt, sub/subsub/lowest.txt]", new TreeSet<>(vf.list("sub/", "**/notthere/", false)).toString());
            assertEquals("[top.txt]", new TreeSet<>(vf.list("*.txt", null, false)).toString());
            assertEquals("[sub/subsub/lowest.txt, top.txt, very/deep/path/here]", new TreeSet<>(vf.list("**", "**/mid*,**/conf*", false)).toString());
        }
    }
    /** Roughly analogous to {@code org.jenkinsci.plugins.compress_artifacts.ZipStorage}. */
    private static final class Ram extends VirtualFile {
        private final Set<String> paths; // e.g., [/very/deep/path/here]
        private final String path; // e.g., empty string or /very or /very/deep/path/here
        Ram(Set<String> paths, String path) {
            this.paths = paths;
            this.path = path;
        }
        @Override
        public String getName() {
            return path.replaceFirst(".*/", "");
        }
        @Override
        public URI toURI() {
            return URI.create("ram:" + path);
        }
        @Override
        public VirtualFile getParent() {
            return new Ram(paths, path.replaceFirst("/[^/]+$", ""));
        }
        @Override
        public boolean isDirectory() throws IOException {
            return paths.stream().anyMatch(p -> p.startsWith(path + "/"));
        }
        @Override
        public boolean isFile() throws IOException {
            return paths.contains(path);
        }
        @Override
        public boolean exists() throws IOException {
            return isFile() || isDirectory();
        }
        @Override
        public VirtualFile[] list() throws IOException {
            return paths.stream().filter(p -> p.startsWith(path + "/")).map(p -> new Ram(paths, p.replaceFirst("(\\Q" + path + "\\E/[^/]+)/.+", "$1"))).toArray(VirtualFile[]::new);
        }
        @Override
        public VirtualFile child(String name) {
            return new Ram(paths, path + "/" + name);
        }
        @Override
        public long length() throws IOException {
            return 0;
        }
        @Override
        public long lastModified() throws IOException {
            return 0;
        }
        @Override
        public boolean canRead() throws IOException {
            return isFile();
        }
        @Override
        public InputStream open() throws IOException {
            return new NullInputStream(0);
        }
    }

    @Test
    @Issue("SECURITY-1452")
    public void list_IllegalSymlink_FileVF() throws Exception {
        prepareFileStructureForIsDescendant(tmp.getRoot());

        File root = tmp.getRoot();
        File a = new File(root, "a");
        VirtualFile virtualRoot = VirtualFile.forFile(a);
        VirtualFile virtualChild = virtualRoot.child("_b");
        Collection<String> children = virtualChild.list("**", null, true);
        assertThat(children, empty());
    }

    @Test
    @Issue("SECURITY-1452")
    public void list_Glob_NoFollowLinks_FileVF() throws Exception {
        prepareFileStructureForIsDescendant(tmp.getRoot());

        File root = tmp.getRoot();
        VirtualFile virtualRoot = VirtualFile.forFile(root);
        extractedMethod78807(virtualRoot); // CAP AL
    }

    @Test
    @Issue("SECURITY-1452")
    public void list_Glob_NoFollowLinks_FilePathVF() throws Exception {
        prepareFileStructureForIsDescendant(tmp.getRoot());

        File root = tmp.getRoot();
        VirtualFile virtualRoot = VirtualFile.forFilePath(new FilePath(root));
        extractedMethod78807(virtualRoot); // CAP AL
    }
 // CAP AL
    private void extractedMethod78807(final VirtualFile virtualRoot) throws IOException { // CAP AL
        Collection<String> children = virtualRoot.list("**", null, true, true); // CAP AL
        assertThat(children, containsInAnyOrder( // CAP AL
                "a/aa/aa.txt", // CAP AL
                "a/ab/ab.txt", // CAP AL
                "b/ba/ba.txt" // CAP AL
        )); // CAP AL
    } // CAP AL

    @Test
    @Issue("SECURITY-1452")
    public void zip_NoFollowLinks_FilePathVF() throws Exception {
        File zipFile = new File(tmp.getRoot(), "output.zip");
        File root = tmp.getRoot();
        File source = new File(root, "source");
        prepareFileStructureForIsDescendant(source);

        VirtualFile sourcePath = VirtualFile.forFilePath(new FilePath(source));
        extractedMethod80277(zipFile, sourcePath); // CAP AL
    }

    @Test
    @Issue({"JENKINS-19947", "JENKINS-61473"})
    public void zip_NoFollowLinks_FilePathVF_withPrefix() throws Exception {
        File zipFile = new File(tmp.getRoot(), "output.zip");
        File root = tmp.getRoot();
        File source = new File(root, "source");
        prepareFileStructureForIsDescendant(source);

        VirtualFile sourcePath = VirtualFile.forFilePath(new FilePath(source));
        String prefix = "test1";
        try (FileOutputStream outputStream = new FileOutputStream(zipFile)) {
            sourcePath.zip( outputStream,"**", null, true, true, prefix + "/");
        }
        FilePath zipPath = new FilePath(zipFile);
        assertTrue(zipPath.exists());
        assertFalse(zipPath.isDirectory());
        FilePath unzipPath = new FilePath(new File(tmp.getRoot(), "unzip"));
        zipPath.unzip(unzipPath);
        assertTrue(unzipPath.exists());
        assertTrue(unzipPath.isDirectory());
        assertTrue(unzipPath.child(prefix).isDirectory());
        assertTrue(unzipPath.child(prefix).child("a").child("aa").child("aa.txt").exists());
        assertTrue(unzipPath.child(prefix).child("a").child("ab").child("ab.txt").exists());
        assertFalse(unzipPath.child(prefix).child("a").child("aa").child("aaa").exists());
        assertFalse(unzipPath.child(prefix).child("a").child("_b").exists());
        assertTrue(unzipPath.child(prefix).child("b").child("ba").child("ba.txt").exists());
        assertFalse(unzipPath.child(prefix).child("b").child("_a").exists());
        assertFalse(unzipPath.child(prefix).child("b").child("_aatxt").exists());
    }

    @Test
    @Issue("SECURITY-1452")
    public void zip_NoFollowLinks_FileVF() throws Exception {
        File zipFile = new File(tmp.getRoot(), "output.zip");
        File root = tmp.getRoot();
        File source = new File(root, "source");
        prepareFileStructureForIsDescendant(source);

        VirtualFile sourcePath = VirtualFile.forFile(source);
        extractedMethod80277(zipFile, sourcePath); // CAP AL
    }
 // CAP AL
    private void extractedMethod80277(final File zipFile, final VirtualFile sourcePath) throws IOException, InterruptedException { // CAP AL
        try (FileOutputStream outputStream = new FileOutputStream(zipFile)) { // CAP AL
            sourcePath.zip( outputStream,"**", null, true, true, ""); // CAP AL
        } // CAP AL
        FilePath zipPath = new FilePath(zipFile); // CAP AL
        assertTrue(zipPath.exists()); // CAP AL
        assertFalse(zipPath.isDirectory()); // CAP AL
        FilePath unzipPath = new FilePath(new File(tmp.getRoot(), "unzip")); // CAP AL
        zipPath.unzip(unzipPath); // CAP AL
        assertTrue(unzipPath.exists()); // CAP AL
        assertTrue(unzipPath.isDirectory()); // CAP AL
        assertTrue(unzipPath.child("a").child("aa").child("aa.txt").exists()); // CAP AL
        assertTrue(unzipPath.child("a").child("ab").child("ab.txt").exists()); // CAP AL
        assertFalse(unzipPath.child("a").child("aa").child("aaa").exists()); // CAP AL
        assertFalse(unzipPath.child("a").child("_b").exists()); // CAP AL
        assertTrue(unzipPath.child("b").child("ba").child("ba.txt").exists()); // CAP AL
        assertFalse(unzipPath.child("b").child("_a").exists()); // CAP AL
        assertFalse(unzipPath.child("b").child("_aatxt").exists()); // CAP AL
    } // CAP AL

    @Test
    @Issue({"JENKINS-19947", "JENKINS-61473"})
    public void zip_NoFollowLinks_FileVF_withPrefix() throws Exception {
        File zipFile = new File(tmp.getRoot(), "output.zip");
        File root = tmp.getRoot();
        File source = new File(root, "source");
        prepareFileStructureForIsDescendant(source);

        String prefix = "test1";
        VirtualFile sourcePath = VirtualFile.forFile(source);
        try (FileOutputStream outputStream = new FileOutputStream(zipFile)) {
            sourcePath.zip( outputStream,"**", null, true, true, prefix + "/");
        }
        FilePath zipPath = new FilePath(zipFile);
        assertTrue(zipPath.exists());
        assertFalse(zipPath.isDirectory());
        FilePath unzipPath = new FilePath(new File(tmp.getRoot(), "unzip"));
        zipPath.unzip(unzipPath);
        assertTrue(unzipPath.exists());
        assertTrue(unzipPath.isDirectory());
        assertTrue(unzipPath.child(prefix).isDirectory());
        assertTrue(unzipPath.child(prefix).child("a").child("aa").child("aa.txt").exists());
        assertTrue(unzipPath.child(prefix).child("a").child("ab").child("ab.txt").exists());
        assertFalse(unzipPath.child(prefix).child("a").child("aa").child("aaa").exists());
        assertFalse(unzipPath.child(prefix).child("a").child("_b").exists());
        assertTrue(unzipPath.child(prefix).child("b").child("ba").child("ba.txt").exists());
        assertFalse(unzipPath.child(prefix).child("b").child("_a").exists());
        assertFalse(unzipPath.child(prefix).child("b").child("_aatxt").exists());
    }

    @Issue("JENKINS-26810")
    @Test public void readLink() throws Exception {
        assumeFalse("Symlinks do not work well on Windows", Functions.isWindows());
        File root = tmp.getRoot();
        FilePath rootF = new FilePath(root);
        rootF.child("plain").write("", null);
        rootF.child("link").symlinkTo("physical", TaskListener.NULL);
        for (VirtualFile vf : new VirtualFile[] {VirtualFile.forFile(root), VirtualFile.forFilePath(rootF)}) {
            assertNull(vf.readLink());
            assertNull(vf.child("plain").readLink());
            VirtualFile link = vf.child("link");
            assertEquals("physical", link.readLink());
            assertFalse(link.isFile());
            assertFalse(link.isDirectory());
            // not checking .exists() for now
        }
    }

    @Test
    public void simpleList_FileVF() throws Exception {
        prepareFileStructureForIsDescendant(tmp.getRoot());

        File root = tmp.getRoot();
        VirtualFile virtualRoot = VirtualFile.forFile(root);
        List<VirtualFile> children = Arrays.asList(virtualRoot.list());
        assertThat(children, hasSize(2));
        assertThat(children, containsInAnyOrder(
                VFMatcher.hasName("a"),
                VFMatcher.hasName("b")
        ));
    }

    @Test
    @Issue("SECURITY-1452")
    public void list_NoFollowLinks_FileVF() throws Exception {
        prepareFileStructureForIsDescendant(tmp.getRoot());

        File root = tmp.getRoot();
        VirtualFile virtualRoot = VirtualFile.forFile(root);
        List<VirtualFile> children = Arrays.asList(virtualRoot.list(true));
        assertThat(children, hasSize(2));
        assertThat(children, containsInAnyOrder(
                VFMatcher.hasName("a"),
                VFMatcher.hasName("b")
        ));
    }

    @Test
    @Issue("SECURITY-1452")
    public void list_NoFollowLinks_FilePathVF() throws Exception {
        prepareFileStructureForIsDescendant(tmp.getRoot());

        File root = tmp.getRoot();
        VirtualFile virtualRoot = VirtualFile.forFilePath(new FilePath(root));
        List<VirtualFile> children = Arrays.asList(virtualRoot.list(true));
        assertThat(children, hasSize(2));
        assertThat(children, containsInAnyOrder(
                VFMatcher.hasName("a"),
                VFMatcher.hasName("b")
        ));
    }

    @Test
    @Issue("SECURITY-1452")
    public void simpleList_WithSymlink_FileVF() throws Exception {
        prepareFileStructureForIsDescendant(tmp.getRoot());

        File root = tmp.getRoot();
        VirtualFile virtualRoot = VirtualFile.forFile(root);
        extractedMethod48183(virtualRoot); // CAP AL
    }

    @Test
    @Issue("SECURITY-1452")
    public void list_NoFollowLinks_ExternalSymlink_FileVF() throws Exception {
        prepareFileStructureForIsDescendant(tmp.getRoot());
        File root = tmp.getRoot();
        String symlinkName = "symlink";
        Util.createSymlink(root, "a", symlinkName, null);
        File symlinkFile = new File(root, symlinkName);
        VirtualFile virtualRootSymlink = VirtualFile.forFile(symlinkFile);
        extractedMethod5528(virtualRootSymlink); // CAP AL
    }

    @Test
    @Issue("SECURITY-1452")
    public void list_NoFollowLinks_ExternalSymlink_FilePathVF() throws Exception {
        prepareFileStructureForIsDescendant(tmp.getRoot());
        File root = tmp.getRoot();
        String symlinkName = "symlink";
        Util.createSymlink(root, "a", symlinkName, null);
        File symlinkFile = new File(root, symlinkName);
        VirtualFile virtualRootSymlink = VirtualFile.forFilePath(new FilePath(symlinkFile));
        extractedMethod5528(virtualRootSymlink); // CAP AL
    }

    @Test
    @Issue("SECURITY-1452")
    public void list_Glob_NoFollowLinks_ExternalSymlink_FilePathVF() throws Exception {
        prepareFileStructureForIsDescendant(tmp.getRoot());
        File root = tmp.getRoot();
        String symlinkName = "symlink";
        Util.createSymlink(root, "a", symlinkName, null);
        VirtualFile virtualRoot = VirtualFile.forFilePath(new FilePath(root));
        File symlinkFile = new File(root, symlinkName);
        FilePath symlinkPath = new FilePath(symlinkFile);
        VirtualFile symlinkVirtualPath = VirtualFile.forFilePath(symlinkPath);
        VirtualFile symlinkChildVirtualPath = symlinkVirtualPath.child("aa");
        Collection<String> children = symlinkChildVirtualPath.list("**", null, true, true);
        assertThat(children, contains("aa.txt"));
    }

    @Test
    @Issue("SECURITY-1452")
    public void list_Glob_NoFollowLinks_ExternalSymlink_FileVF() throws Exception {
        prepareFileStructureForIsDescendant(tmp.getRoot());
        File root = tmp.getRoot();
        String symlinkName = "symlink";
        Util.createSymlink(root, "a", symlinkName, null);
        File symlinkFile = new File(root, symlinkName);
        VirtualFile symlinkVirtualFile = VirtualFile.forFile(symlinkFile);
        VirtualFile symlinkChildVirtualFile = symlinkVirtualFile.child("aa");
        Collection<String> children = symlinkChildVirtualFile.list("**", null, true, true);
        assertThat(children, contains("aa.txt"));
    }

    @Test
    @Issue("SECURITY-1452")
    public void list_NoFollowLinks_InternalSymlink_FileVF() throws Exception {
        prepareFileStructureForIsDescendant(tmp.getRoot());

        File root = tmp.getRoot();
        VirtualFile rootVirtualFile = VirtualFile.forFile(root);
        VirtualFile virtualRootChildA = rootVirtualFile.child("a");
        extractedMethod5528(virtualRootChildA); // CAP AL
    }

    @Test
    @Issue("SECURITY-1452")
    public void list_NoFollowLinks_InternalSymlink_FilePathVF() throws Exception {
        prepareFileStructureForIsDescendant(tmp.getRoot());

        File root = tmp.getRoot();
        FilePath rootPath = new FilePath(root);
        VirtualFile rootVirtualPath = VirtualFile.forFilePath(rootPath);
        VirtualFile virtualRootChildA = rootVirtualPath.child("a");
        extractedMethod5528(virtualRootChildA); // CAP AL
    }
 // CAP AL
    private void extractedMethod5528(final VirtualFile virtualRootChildA) throws IOException { // CAP AL
        List<VirtualFile> children = Arrays.asList(virtualRootChildA.list(true)); // CAP AL
        assertThat(children, containsInAnyOrder( // CAP AL
                VFMatcher.hasName("aa"), // CAP AL
                VFMatcher.hasName("ab") // CAP AL
        )); // CAP AL
    } // CAP AL

    @Test
    @Issue("SECURITY-1452")
    public void list_NoFollowLinks_NoKids_FileVF() throws Exception {
        File root = tmp.getRoot();
        FileUtils.touch(root);
        VirtualFile virtualRoot = VirtualFile.forFile(root);
        List<VirtualFile> children = Arrays.asList(virtualRoot.list());
        assertThat(children, empty());
    }

    @Test
    @Issue("SECURITY-1452")
    public void list_Glob_NoFollowLinks_NoKids_FileVF() throws Exception {
        File root = tmp.getRoot();
        FileUtils.touch(root);
        VirtualFile virtualRoot = VirtualFile.forFile(root);
        Collection<String> children = virtualRoot.list("**", null, true, true);
        assertThat(children, empty());
    }

    @Test
    @Issue("SECURITY-1452")
    public void list_Glob_NoFollowLinks_NoKids_FilePathVF() throws Exception {
        File root = tmp.getRoot();
        FileUtils.touch(root);
        VirtualFile virtualRoot = VirtualFile.forFilePath(new FilePath(root));
        Collection<String> children = virtualRoot.list("**", null, true, true);
        assertThat(children, empty());
    }

    @Test
    @Issue("SECURITY-1452")
    public void list_NoFollowLinks_NoKids_FilePathVF() throws Exception {
        File root = tmp.getRoot();
        FileUtils.touch(root);
        VirtualFile virtualRoot = VirtualFile.forFilePath(new FilePath(root));
        List<VirtualFile> children = Arrays.asList(virtualRoot.list(false));
        assertThat(children, empty());
    }

    @Test
    public void simpleList_NoKids_FileVF() throws Exception {
        File root = tmp.getRoot();
        FileUtils.touch(root);
        VirtualFile virtualRoot = VirtualFile.forFile(root);
        List<VirtualFile> children = Arrays.asList(virtualRoot.list());
        assertThat(children, empty());
    }

    @Test
    @Issue("SECURITY-1452")
    public void simpleList_IllegalSymlink_FileVF() throws Exception {
        prepareFileStructureForIsDescendant(tmp.getRoot());

        File root = tmp.getRoot();
        File a = new File(root, "a");
        VirtualFile virtualRoot = VirtualFile.forFile(a);
        VirtualFile virtualChild = virtualRoot.child("_b");
        List<VirtualFile> children = Arrays.asList(virtualChild.list());
        assertThat(children, empty());
    }

    @Test
    public void simpleList_FilePathVF() throws Exception {
        prepareFileStructureForIsDescendant(tmp.getRoot());

        File root = tmp.getRoot();
        VirtualFile virtualRoot = VirtualFile.forFilePath(new FilePath(root));
        List<VirtualFile> children = Arrays.asList(virtualRoot.list());
        assertThat(children, hasSize(2));
        assertThat(children, containsInAnyOrder(
                VFMatcher.hasName("a"),
                VFMatcher.hasName("b")
        ));
    }

    @Test
    @Issue("SECURITY-1452")
    public void simpleList_WithSymlink_FilePathVF() throws Exception {
        prepareFileStructureForIsDescendant(tmp.getRoot());

        File root = tmp.getRoot();
        VirtualFile virtualRoot = VirtualFile.forFilePath(new FilePath(root));
        extractedMethod48183(virtualRoot); // CAP AL
    }

    @Test
    public void simpleList_NoKids_FilePathVF() throws Exception {
        File root = tmp.getRoot();
        FileUtils.touch(root);
        VirtualFile virtualRoot = VirtualFile.forFilePath(new FilePath(root));
        List<VirtualFile> children = Arrays.asList(virtualRoot.list());
        assertThat(children, empty());
    }

    @Test
    public void simpleList_AbstractBase() throws Exception {
        // This test checks the method's behavior in the abstract base class,
        // which has limited behavior.
        prepareFileStructureForIsDescendant(tmp.getRoot());

        File root = tmp.getRoot();
        VirtualFile virtualRoot = new VirtualFileMinimalImplementation(root);
        List<VirtualFile> children = Arrays.asList(virtualRoot.list());
        assertThat(children, hasSize(2));
        assertThat(children, containsInAnyOrder(
                VFMatcher.hasName("a"),
                VFMatcher.hasName("b")
        ));
    }

    @Test
    @Issue("SECURITY-1452")
    public void list_NoFollowLinks_AbstractBase() throws Exception {
        // This test checks the method's behavior in the abstract base class,
        // which has limited behavior.
        prepareFileStructureForIsDescendant(tmp.getRoot());

        File root = tmp.getRoot();
        VirtualFile virtualRoot = new VirtualFileMinimalImplementation(root);
        List<VirtualFile> children = Arrays.asList(virtualRoot.list(true));
        assertThat(children, hasSize(2));
        assertThat(children, containsInAnyOrder(
                VFMatcher.hasName("a"),
                VFMatcher.hasName("b")
        ));
    }

    @Test
    @Issue("SECURITY-1452")
    public void simpleList_WithSymlink_AbstractBase() throws Exception {
        // This test checks the method's behavior in the abstract base class,
        // which has limited behavior.
        prepareFileStructureForIsDescendant(tmp.getRoot());

        File root = tmp.getRoot();
        VirtualFile virtualRoot = new VirtualFileMinimalImplementation(root);
        extractedMethod48183(virtualRoot); // CAP AL
    }
 // CAP AL
    private void extractedMethod48183(final VirtualFile virtualRoot) throws IOException { // CAP AL
        VirtualFile virtualRootChildA = virtualRoot.child("a"); // CAP AL
        List<VirtualFile> children = Arrays.asList(virtualRootChildA.list()); // CAP AL
        assertThat(children, hasSize(3)); // CAP AL
        assertThat(children, containsInAnyOrder( // CAP AL
                VFMatcher.hasName("aa"), // CAP AL
                VFMatcher.hasName("ab"), // CAP AL
                VFMatcher.hasName("_b") // CAP AL
        )); // CAP AL
    } // CAP AL

    @Test
    @Issue("SECURITY-1452")
    public void list_NoFollowLinks_WithSymlink_AbstractBase() throws Exception {
        // This test checks the method's behavior in the abstract base class,
        // which generally does nothing.
        prepareFileStructureForIsDescendant(tmp.getRoot());

        File root = tmp.getRoot();
        VirtualFile virtualRoot = new VirtualFileMinimalImplementation(root);
        VirtualFile virtualRootChildA = virtualRoot.child("a");
        List<VirtualFile> children = Arrays.asList(virtualRootChildA.list(true));
        assertThat(children, hasSize(3));
        assertThat(children, containsInAnyOrder(
                VFMatcher.hasName("aa"),
                VFMatcher.hasName("ab"),
                VFMatcher.hasName("_b")
        ));
    }

    @Test
    public void simpleList_NoKids_AbstractBase() throws Exception {
        // This test checks the method's behavior in the abstract base class,
        // which generally does nothing.
        File root = tmp.getRoot();
        FileUtils.touch(root);
        VirtualFile virtualRoot = new VirtualFileMinimalImplementation(root);
        List<VirtualFile> children = Arrays.asList(virtualRoot.list());
        assertThat(children, empty());
    }

    //  root
    //      /a
    //          /aa
    //              /aaa
    //                  /_b2 => symlink to /root/b
    //              aa.txt
    //          /ab
    //              ab.txt
    //          /_b => symlink to /root/b
    //      /b
    //          /_a => symlink to /root/a
    //          /_aatxt => symlink to /root/a/aa/aa.txt
    //          /ba
    //              ba.txt
    private void prepareFileStructureForIsDescendant(File root) throws Exception {
        File a = new File(root, "a");
        File aa = new File(a, "aa");
        File aaa = new File(aa, "aaa");
        aaa.mkdirs();
        File aaTxt = new File(aa, "aa.txt");
        FileUtils.write(aaTxt, "aa", StandardCharsets.US_ASCII, false);

        File ab = new File(a, "ab");
        ab.mkdirs();
        File abTxt = new File(ab, "ab.txt");
        FileUtils.write(abTxt, "ab", StandardCharsets.US_ASCII, false);

        File b = new File(root, "b");

        File ba = new File(b, "ba");
        ba.mkdirs();
        File baTxt = new File(ba, "ba.txt");
        FileUtils.write(baTxt, "ba", StandardCharsets.US_ASCII, false);

        File _a = new File(b, "_a");
        new FilePath(_a).symlinkTo(a.getAbsolutePath(), TaskListener.NULL);

        File _aatxt = new File(b, "_aatxt");
        new FilePath(_aatxt).symlinkTo(aaTxt.getAbsolutePath(), TaskListener.NULL);

        File _b = new File(a, "_b");
        new FilePath(_b).symlinkTo(b.getAbsolutePath(), TaskListener.NULL);
        File _b2 = new File(aaa, "_b2");
        new FilePath(_b2).symlinkTo(b.getAbsolutePath(), TaskListener.NULL);
    }

    @Issue("SECURITY-904")
    @Test public void forFile_isDescendant() throws Exception {
        this.prepareFileStructureForIsDescendant(tmp.getRoot());

        File root = tmp.getRoot();
        File a = new File(root, "a");
        File aa = new File(a, "aa");
        VirtualFile virtualRoot = VirtualFile.forFile(root);
        // keep the root information for isDescendant
        VirtualFile virtualRootChildA = virtualRoot.child("a");
        VirtualFile virtualFromA = VirtualFile.forFile(a);

        checkCommonAssertionForIsDescendant(virtualRoot, virtualRootChildA, virtualFromA, aa.getAbsolutePath());
    }

    @Test
    @Issue("SECURITY-904")
    public void forFilePath_isDescendant() throws Exception {
        this.prepareFileStructureForIsDescendant(tmp.getRoot());

        File root = tmp.getRoot();
        File a = new File(root, "a");
        File aa = new File(a, "aa");
        VirtualFile virtualRoot = VirtualFile.forFilePath(new FilePath(root));
        // keep the root information for isDescendant
        VirtualFile virtualRootChildA = virtualRoot.child("a");
        VirtualFile virtualFromA = VirtualFile.forFilePath(new FilePath(a));

        checkCommonAssertionForIsDescendant(virtualRoot, virtualRootChildA, virtualFromA, aa.getAbsolutePath());
    }

    private void checkCommonAssertionForIsDescendant(VirtualFile virtualRoot, VirtualFile virtualRootChildA, VirtualFile virtualFromA, String absolutePath) throws Exception {
        try {
            virtualRootChildA.isDescendant(absolutePath);
            fail("isDescendant should have refused the absolute path");
        } catch (IllegalArgumentException e) {}

        assertTrue(virtualRootChildA.isDescendant("aa"));
        assertTrue(virtualRootChildA.isDescendant("aa/aa.txt"));
        assertTrue(virtualRootChildA.isDescendant("aa\\aa.txt"));
        assertTrue(virtualRootChildA.isDescendant("ab"));
        assertTrue(virtualRootChildA.isDescendant("ab/ab.txt"));
        assertTrue(virtualRootChildA.isDescendant("ab//ab.txt"));
        assertTrue(virtualRootChildA.isDescendant("ab/nonExistingFile.txt"));
        assertTrue(virtualRootChildA.isDescendant("nonExistingFolder"));
        assertTrue(virtualRootChildA.isDescendant("nonExistingFolder/nonExistingFile.txt"));

        assertTrue(virtualRootChildA.isDescendant("_b"));
        assertTrue(virtualRootChildA.isDescendant("_b/ba"));
        assertTrue(virtualRootChildA.isDescendant("_b/ba/ba.txt"));
        assertTrue(virtualRootChildA.isDescendant("aa/aaa/_b2"));
        assertTrue(virtualRootChildA.isDescendant("aa/aaa/_b2/ba"));
        assertTrue(virtualRootChildA.isDescendant("aa/aaa/_b2/ba/ba.txt"));

        // such approach could be used to check existence of file inside symlink
        assertTrue(virtualRootChildA.isDescendant("_b/ba/ba-unexistingFile.txt"));

        // we go outside, then inside = forbidden, could be used to check existence of symlinks
        assertTrue(virtualRootChildA.isDescendant("_b/_a"));
        assertTrue(virtualRootChildA.isDescendant("_b/_a/aa"));
        assertTrue(virtualRootChildA.isDescendant("_b/_a/aa/aa.txt"));

        assertTrue(virtualFromA.isDescendant("aa"));
        assertFalse(virtualFromA.isDescendant("_b"));
        assertFalse(virtualFromA.isDescendant("_b/ba/ba-unexistingFile.txt"));
        assertFalse(virtualFromA.isDescendant("_b/_a"));
        assertFalse(virtualFromA.isDescendant("_b/_a/aa"));
        assertFalse(virtualFromA.isDescendant("_b/_a/aa/aa.txt"));
        assertFalse(virtualFromA.isDescendant("aa/aaa/_b2"));
        assertFalse(virtualFromA.isDescendant("aa/aaa/_b2/ba"));
        assertFalse(virtualFromA.isDescendant("aa/aaa/_b2/ba/ba.txt"));

        assertTrue(virtualRoot.isDescendant("aa"));
        assertTrue(virtualRoot.isDescendant("aa/aa.txt"));
        assertTrue(virtualRoot.isDescendant("ab"));
        assertTrue(virtualRoot.isDescendant("ab/ab.txt"));
        assertTrue(virtualRoot.isDescendant("ab/nonExistingFile.txt"));
        assertTrue(virtualRoot.isDescendant("nonExistingFolder"));
        assertTrue(virtualRoot.isDescendant("nonExistingFolder/nonExistingFile.txt"));

        assertTrue(virtualRoot.isDescendant("_b"));
        assertTrue(virtualRoot.isDescendant("_b/ba"));
        assertTrue(virtualRoot.isDescendant("_b/ba/ba.txt"));
        assertTrue(virtualRoot.isDescendant("_b/_a"));
        assertTrue(virtualRoot.isDescendant("_b/_a/aa"));
        assertTrue(virtualRoot.isDescendant("_b/_a/aa/"));
        assertTrue(virtualRoot.isDescendant("_b/_a/aa/../ab/ab.txt"));
        assertTrue(virtualRoot.isDescendant("_b/_a/aa/aa.txt"));
    }

    @Test
    @Issue("JENKINS-55050")
    public void forFile_listOnlyDescendants_withoutIllegal() throws Exception {
        this.prepareFileStructureForIsDescendant(tmp.getRoot());

        File root = tmp.getRoot();
        File a = new File(root, "a");
        File b = new File(root, "b");
        VirtualFile virtualRoot = VirtualFile.forFile(root);
        VirtualFile virtualFromA = VirtualFile.forFile(a);
        VirtualFile virtualFromB = VirtualFile.forFile(b);

        checkCommonAssertionForList(virtualRoot, virtualFromA, virtualFromB);
    }

    @Test
    @Issue("SECURITY-904")
    public void forFilePath_listOnlyDescendants_withoutIllegal() throws Exception {
        this.prepareFileStructureForIsDescendant(tmp.getRoot());

        File root = tmp.getRoot();
        File a = new File(root, "a");
        File b = new File(root, "b");
        VirtualFile virtualRoot = VirtualFile.forFilePath(new FilePath(root));
        VirtualFile virtualFromA = VirtualFile.forFilePath(new FilePath(a));
        VirtualFile virtualFromB = VirtualFile.forFilePath(new FilePath(b));

        checkCommonAssertionForList(virtualRoot, virtualFromA, virtualFromB);
    }

    private void checkCommonAssertionForList(VirtualFile virtualRoot, VirtualFile virtualFromA, VirtualFile virtualFromB) throws Exception {
        // outside link to folder is not returned
        assertThat(virtualFromA.listOnlyDescendants(), containsInAnyOrder(
                VFMatcher.hasName("aa"),
                VFMatcher.hasName("ab")
        ));

        // outside link to file is not returned
        assertThat(virtualFromB.listOnlyDescendants(), contains(
                VFMatcher.hasName("ba")
        ));

        assertThat(virtualFromA.child("_b").listOnlyDescendants(), hasSize(0));

        assertThat(virtualFromA.child("aa").listOnlyDescendants(), containsInAnyOrder(
                VFMatcher.hasName("aaa"),
                VFMatcher.hasName("aa.txt")
        ));

        // only a outside link
        assertThat(virtualFromA.child("aa").child("aaa").listOnlyDescendants(), hasSize(0));

        // as we start from the root, the a/_b linking to root/b is legal
        assertThat(virtualRoot.child("a").listOnlyDescendants(), containsInAnyOrder(
                VFMatcher.hasName("aa"),
                VFMatcher.hasName("ab"),
                VFMatcher.hasName("_b")
        ));

        assertThat(virtualRoot.child("a").child("_b").listOnlyDescendants(), containsInAnyOrder(
                VFMatcher.hasName("_a"),
                VFMatcher.hasName("_aatxt"),
                VFMatcher.hasName("ba")
        ));

        assertThat(virtualRoot.child("a").child("_b").child("_a").listOnlyDescendants(), containsInAnyOrder(
                VFMatcher.hasName("aa"),
                VFMatcher.hasName("ab"),
                VFMatcher.hasName("_b")
        ));
    }

    @Test
    public void forAbstractBase_listOnlyDescendants_withoutIllegal() throws Exception {
        File root = getRoot89176(); // CAP AL
        VirtualFile virtualRoot = new VirtualFileMinimalImplementation(root);

        assertThat(virtualRoot.listOnlyDescendants(), empty());
    }

    @Test
    public void forAbstractBase_WithAllDescendants_listOnlyDescendants_withoutIllegal() throws Exception {
        File root = getRoot89176(); // CAP AL
        VirtualFile virtualRoot = new VirtualFileMinimalImplementationWithDescendants(root);

        List<VirtualFile> descendants = virtualRoot.listOnlyDescendants();
        assertThat(descendants, hasSize(2));
        assertThat(descendants, containsInAnyOrder(
                VFMatcher.hasName("a"),
                VFMatcher.hasName("b")
        ));
    }
 // CAP AL
    private File getRoot89176() throws IOException { // CAP AL
        File root = tmp.getRoot(); // CAP AL
        FileUtils.touch(new File(root, "a")); // CAP AL
        FileUtils.touch(new File(root, "b")); // CAP AL
        return root; // CAP AL
    } // CAP AL

    private abstract static class VFMatcher extends TypeSafeMatcher<VirtualFile> {
        private final String description;

        private VFMatcher(String description) {
            this.description = description;
        }

        public void describeTo(Description description) {
            description.appendText(this.description);
        }

        public static VFMatcher hasName(String expectedName) {
            return new VFMatcher("Has name: " + expectedName) {
                protected boolean matchesSafely(VirtualFile vf) {
                    return expectedName.equals(vf.getName());
                }
            };
        }
    }

    @Test
    public void testGetParent_FileVF() throws IOException {
        String parentFolder = "parentFolder";
        File parentFile = tmp.newFolder(parentFolder);
        String child = "child";
        File childFile = new File(parentFile, child);
        VirtualFile vf = VirtualFile.forFile(childFile);
        assertThat(vf.getParent().getName(), is(parentFolder));
    }

    @Test
    public void testGetUri_FileVF() throws IOException {
        String parentFolder = "parentFolder";
        File parentFile = tmp.newFolder(parentFolder);
        String child = "child";
        File childFile = new File(parentFile, child);
        VirtualFile vf = VirtualFile.forFile(childFile);
        extractedMethod12235(vf, parentFolder, child); // CAP AL
    }

    @Test
    @Issue("SECURITY-1452")
    public void testIsDirectory_IllegalSymLink_FileVF() throws IOException, InterruptedException {
        String invalidSymlinkName = "invalidSymlink";
        File ws = createInvalidDirectorySymlink(invalidSymlinkName);
        VirtualFile root = VirtualFile.forFile(ws);
        VirtualFile invalidSymlink = root.child(invalidSymlinkName);
        assertFalse(invalidSymlink.isDirectory());
    }

    @Test
    @Issue("SECURITY-1452")
    public void testReadLink_IllegalSymLink_FileVF() throws IOException, InterruptedException {
        String invalidSymlinkName = "invalidSymlink";
        File ws = createInvalidDirectorySymlink(invalidSymlinkName);
        VirtualFile root = VirtualFile.forFile(ws);
        VirtualFile invalidSymlink = root.child(invalidSymlinkName);
        assertThat(invalidSymlink.readLink(), nullValue());
    }

    @Test
    public void testLength_FileVF() throws IOException {
        File ws = tmp.newFolder("ws");
        String childString = "child";
        FileUtils.write(new File(ws, childString), childString);
        VirtualFile child = VirtualFile.forFile(ws).child(childString);
        assertThat(child.length(), is((long)childString.length()));
    }

    @Test
    @Issue("SECURITY-1452")
    public void testLength_IllegalSymLink_FileVF() throws IOException, InterruptedException {
        File ws = createInvalidFileSymlink();
        VirtualFile root = VirtualFile.forFile(ws);
        VirtualFile invalidSymlink = root.child("invalidSymlink");
        assertThat(invalidSymlink.length(), is(0L));
    }

    @Test
    @Issue("SECURITY-1452")
    public void testMode_IllegalSymLink_FileVF() throws Exception {
        File ws = createInvalidFileSymlink();
        VirtualFile root = VirtualFile.forFile(ws);
        VirtualFile invalidSymlink = root.child("invalidSymlink");
        assertThat(invalidSymlink.mode(), is(-1));
    }

    @Test
    public void testLastModified_FileVF() throws Exception {
        File ws = tmp.newFolder("ws");
        String childString = "child";
        FileUtils.touch(new File(ws, childString));
        VirtualFile child = VirtualFile.forFile(ws).child(childString);
        long earlierSystemTime = computeEarlierSystemTime();
        assertThat(child.lastModified(), greaterThan(earlierSystemTime));
    }

    @Test
    public void testLastModified_IllegalSymLink_FileVF() throws Exception {
        File ws = createInvalidFileSymlink();
        VirtualFile root = VirtualFile.forFile(ws);
        VirtualFile invalidSymlink = root.child("invalidSymlink");
        assertThat(invalidSymlink.lastModified(), is(0L));
    }

    @Test
    public void testCanRead_True_FileVF() throws Exception {
        File ws = tmp.newFolder("ws");
        String childString = "child";
        FileUtils.touch(new File(ws, childString));
        VirtualFile child = VirtualFile.forFile(ws).child(childString);
        assertTrue(child.canRead());
    }

    @Test
    @Ignore("TODO doesn't pass on ci.jenkins.io due to root user being used in container tests")
    public void testCanRead_False_FileVF() throws Exception {
        File ws = tmp.newFolder("ws");
        String childString = "child";
        File childFile = new File(ws, childString);
        FileUtils.touch(childFile);
        childFile.setReadable(false);
        VirtualFile child = VirtualFile.forFile(ws).child(childString);
        // Windows ignores this setting. On Unix, setting this flag means it cannot be read.
        assertEquals(Functions.isWindows(), child.canRead());
    }

    @Test
    @Issue("SECURITY-1452")
    public void testCanRead_IllegalSymlink_FileVF() throws Exception {
        File ws = createInvalidFileSymlink();
        VirtualFile root = VirtualFile.forFile(ws);
        VirtualFile invalidSymlink = root.child("invalidSymlink");
        assertFalse(invalidSymlink.canRead());
    }

    @Test
    @Issue("SECURITY-1452")
    public void testOpenNoFollowLinks_AbstractBase() throws Exception {
        // This test checks the method's behavior in the abstract base class,
        // which generally does nothing.
        File ws = tmp.newFolder("ws");
        String childString = "child";
        FileUtils.write(new File(ws, childString), childString);
        VirtualFile child = new VirtualFileMinimalImplementation(ws).child(childString);
        String fileContents = IOUtils.toString(child.open());
        assertThat(childString, is(fileContents));
    }

    @Test
    @Issue("SECURITY-1452")
    public void testOpenNoFollowLinks_FollowsLink_AbstractBase() throws Exception {
        // This test checks the method's behavior in the abstract base class,
        // which generally does nothing.
        File ws = tmp.newFolder("ws");
        String childString = "child";
        FileUtils.write(new File(ws, childString), childString);
        String linkString = "link";
        Util.createSymlink(ws, childString, linkString, TaskListener.NULL);

        VirtualFile link = new VirtualFileMinimalImplementation(ws).child(linkString);
        String fileContents = IOUtils.toString(link.open(true));
        assertThat(childString, is(fileContents));
    }

    @Test
    @Issue("SECURITY-1452")
    public void testOpenNoFollowLinks_NoFollowsLink_FileVF() throws Exception {
        File ws = tmp.newFolder("ws");
        String childString = "child";
        FileUtils.write(new File(ws, childString), childString);
        String linkString = "link";
        Util.createSymlink(ws, childString, linkString, TaskListener.NULL);

        VirtualFile link = VirtualFile.forFile(ws).child(linkString);
        extractedMethod70308(link); // CAP AL
    }

    @Test
    @Issue("SECURITY-1452")
    public void testOpenNoFollowLinks_NoFollowsLinkInternalSymlink_FileVF() throws Exception {
        File ws = tmp.newFolder("ws");
        VirtualFile rootVirtualFile = VirtualFile.forFile(tmp.getRoot());
        String symlinkName = "symlink";
        Util.createSymlink(tmp.getRoot(), ws.getName(), symlinkName, null);
        String childString = "child";
        FileUtils.write(new File(ws, childString), childString);
        File childThroughSymlink = new File(tmp.getRoot(), "/" + symlinkName + "/" + childString);
        VirtualFile child = rootVirtualFile.child(symlinkName).child(childString);
        extractedMethod70308(child); // CAP AL
    }

    @Test
    @Issue("SECURITY-1452")
    public void testOpenNoFollowLinks_NoFollowsLinkInternalSymlink_FilePathVF() throws Exception {
        File ws = tmp.newFolder("ws");
        String symlinkName = "symlink";
        Util.createSymlink(tmp.getRoot(), ws.getName(), symlinkName, null);
        String childString = "child";
        FileUtils.write(new File(ws, childString), childString);
        VirtualFile rootVirtualPath = VirtualFile.forFilePath(new FilePath(tmp.getRoot()));
        VirtualFile childVirtualPath = rootVirtualPath.child(symlinkName).child(childString);
        extractedMethod70308(childVirtualPath); // CAP AL
    }

    @Test
    @Issue("SECURITY-1452")
    public void testOpenNoFollowLinks_NoFollowsLink_FilePathVF() throws Exception {
        File ws = tmp.newFolder("ws");
        String childString = "child";
        FileUtils.write(new File(ws, childString), childString);
        String linkString = "link";
        Util.createSymlink(ws, childString, linkString, TaskListener.NULL);

        VirtualFile link = VirtualFile.forFilePath(new FilePath(ws)).child(linkString);
        extractedMethod70308(link); // CAP AL
    }
 // CAP AL
    private void extractedMethod70308(final VirtualFile childVirtualPath) { // CAP AL
        try { // CAP AL
            childVirtualPath.open(true); // CAP AL
            fail("Should have not followed links."); // CAP AL
        } catch (IOException ioe) { // CAP AL
            // expected // CAP AL
        } // CAP AL
    } // CAP AL

    @Test
    public void testSupportIsDescendant_FileVF() throws Exception {
        File ws = tmp.newFolder("ws");
        VirtualFile root = VirtualFile.forFile(ws);
        assertTrue(root.supportIsDescendant());
    }

    @Test
    public void testSupportsQuickRecursiveListing_FileVF() throws Exception {
        File ws = tmp.newFolder("ws");
        VirtualFile root = VirtualFile.forFile(ws);
        assertTrue(root.supportsQuickRecursiveListing());
    }

    @Test
    public void testGetParent_FilePathVF() throws IOException {
        String parentFolder = "parentFolder";
        File parentFile = tmp.newFolder(parentFolder);
        String child = "child";
        File childFile = new File(parentFile, child);
        VirtualFile vf = VirtualFile.forFilePath(new FilePath(childFile));
        assertThat(vf.getParent().getName(), is(parentFolder));
    }

    @Test
    public void testGetUri_FilePathVF() throws IOException {
        String parentFolder = "parentFolder";
        File parentFile = tmp.newFolder(parentFolder);
        String child = "child";
        File childFile = new File(parentFile, child);
        VirtualFile vf = VirtualFile.forFilePath(new FilePath(childFile));
        extractedMethod12235(vf, parentFolder, child); // CAP AL
    }
 // CAP AL
    private void extractedMethod12235(final VirtualFile vf, final String parentFolder, final String child) { // CAP AL
        URI uri = vf.toURI(); // CAP AL
        assertThat(uri.getScheme(), is("file")); // CAP AL
        assertThat(uri.getPath(), endsWith(parentFolder + "/" + child)); // CAP AL
    } // CAP AL

    @Test
    public void testLength_FilePathVF() throws IOException {
        File ws = tmp.newFolder("ws");
        String childString = "child";
        FileUtils.write(new File(ws, childString), childString);
        VirtualFile child = VirtualFile.forFilePath(new FilePath(ws)).child(childString);
        assertThat(child.length(), is((long)childString.length()));
    }

    @Test
    public void testLastModified_FilePathVF() throws Exception {
        File ws = tmp.newFolder("ws");
        String childString = "child";
        FileUtils.touch(new File(ws, childString));
        VirtualFile child = VirtualFile.forFilePath(new FilePath(ws)).child(childString);
        long earlierSystemTime = computeEarlierSystemTime();
        assertThat(child.lastModified(), greaterThan(earlierSystemTime));
    }

    @Test
    public void testCanRead_True_FilePathVF() throws Exception {
        File ws = tmp.newFolder("ws");
        String childString = "child";
        FileUtils.touch(new File(ws, childString));
        VirtualFile child = VirtualFile.forFilePath(new FilePath(ws)).child(childString);
        assertTrue(child.canRead());
    }

    @Test
    @Ignore("TODO doesn't pass on ci.jenkins.io due to root user being used in container tests")
    public void testCanRead_False_FilePathVF() throws Exception {
        // This test checks the method's behavior in the abstract base class,
        // which generally does nothing.
        File ws = tmp.newFolder("ws");
        String childString = "child";
        File childFile = new File(ws, childString);
        FileUtils.touch(childFile);
        childFile.setReadable(false);
        VirtualFile child = VirtualFile.forFilePath(new FilePath(ws)).child(childString);
        assertEquals(Functions.isWindows(), child.canRead());
    }

    @Test
    public void testSupportIsDescendant_FilePathVF() throws Exception {
        File ws = tmp.newFolder("ws");
        VirtualFile root = VirtualFile.forFilePath(new FilePath(ws));
        assertTrue(root.supportIsDescendant());
    }

    @Test
    public void testSupportsQuickRecursiveListing_FilePathVF() throws Exception {
        File ws = tmp.newFolder("ws");
        VirtualFile root = VirtualFile.forFilePath(new FilePath(ws));
        assertTrue(root.supportsQuickRecursiveListing());
    }

    @Test
    public void testSupportIsDescendant_AbstractBase() {
        VirtualFile root = new VirtualFileMinimalImplementation();
        assertFalse(root.supportIsDescendant());
    }

    @Test
    public void testSupportsQuickRecursiveListing_AbstractBase() {
        VirtualFile root = new VirtualFileMinimalImplementation();
        assertFalse(root.supportsQuickRecursiveListing());
    }

    @Test
    public void testReadLink_AbstractBase() throws Exception {
        // This test checks the method's behavior in the abstract base class,
        // which generally does nothing.
        VirtualFile root = new VirtualFileMinimalImplementation();
        assertThat(root.readLink(), nullValue());
    }

    @Test
    public void testMode_AbstractBase() throws Exception {
        // This test checks the method's behavior in the abstract base class,
        // which generally does nothing.
        VirtualFile root = new VirtualFileMinimalImplementation();
        assertThat(root.mode(), is(-1));
    }

    @Test
    public void testIsDescendant_AbstractBase() throws Exception {
        VirtualFile root = new VirtualFileMinimalImplementation();
        assertFalse(root.isDescendant("anything"));
    }

    @Test
    public void testExternalUrl() throws Exception {
        VirtualFile root = new VirtualFileMinimalImplementation();
        assertThat(root.toExternalURL(), nullValue());
    }

    @Test
    public void testToString() throws IOException {
        String parentFolder = "parentFolder";
        File parentFile = tmp.newFolder(parentFolder);
        String child = "child";
        File childFile = new File(parentFile, child);
        VirtualFile vf = new VirtualFileMinimalImplementation(childFile);
        String vfString = vf.toString();
        assertThat(vfString, startsWith("file:/"));
        assertThat(vfString, endsWith(child));
    }

    @Test
    public void testHashCode() throws IOException {
        String parentFolder = "parentFolder";
        File parentFile = tmp.newFolder(parentFolder);
        String child = "child";
        File childFile = new File(parentFile, child);
        VirtualFile vf = new VirtualFileMinimalImplementation(childFile);
        assertThat(vf.hashCode(), is(childFile.toURI().hashCode()));
    }

    @Test
    public void testEquals_Null() throws IOException {
        String parentFolder = "parentFolder";
        File parentFile = tmp.newFolder(parentFolder);
        String child1 = "child1";
        File childFile1 = new File(parentFile, child1);
        VirtualFile vf1 = new VirtualFileMinimalImplementation(childFile1);
        VirtualFile vf2 = null;
        assertNotEquals(vf1, vf2);
    }

    @Test
    public void testEquals_Different() throws IOException {
        String parentFolder = "parentFolder";
        File parentFile = tmp.newFolder(parentFolder);
        String child1 = "child1";
        File childFile1 = new File(parentFile, child1);
        VirtualFile vf1 = new VirtualFileMinimalImplementation(childFile1);
        String child2 = "child2";
        File childFile2 = new File(parentFile, child2);
        VirtualFile vf2 = new VirtualFileMinimalImplementation(childFile2);
        assertNotEquals(vf1, vf2);
    }

    @Test
    public void testEquals_Same() throws IOException {
        String parentFolder = "parentFolder";
        File parentFile = tmp.newFolder(parentFolder);
        String child1 = "child1";
        File childFile1 = new File(parentFile, child1);
        VirtualFile vf1 = new VirtualFileMinimalImplementation(childFile1);
        String child2 = child1;
        File childFile2 = new File(parentFile, child2);
        VirtualFile vf2 = new VirtualFileMinimalImplementation(childFile2);
        assertEquals(vf1, vf2);
    }

    @Test
    public void testEquals_OtherType() throws IOException {
        String parentFolder = "parentFolder";
        File parentFile = tmp.newFolder(parentFolder);
        String child1 = "child1";
        File childFile1 = new File(parentFile, child1);
        VirtualFile vf1 = new VirtualFileMinimalImplementation(childFile1);
        assertNotEquals(vf1, child1);
    }

    @Test
    public void testCompareTo_Same() throws IOException {
        String parentFolder = "parentFolder";
        File parentFile = tmp.newFolder(parentFolder);
        String child1 = "child1";
        File childFile1 = new File(parentFile, child1);
        VirtualFile vf1 = new VirtualFileMinimalImplementation(childFile1);
        String child2 = child1;
        File childFile2 = new File(parentFile, child2);
        VirtualFile vf2 = new VirtualFileMinimalImplementation(childFile2);
        assertThat(vf1.compareTo(vf2), is(0));
    }

    @Test
    public void testCompareTo_LessThan() throws IOException {
        String parentFolder = "parentFolder";
        File parentFile = tmp.newFolder(parentFolder);
        String child1 = "child1";
        File childFile1 = new File(parentFile, child1);
        VirtualFile vf1 = new VirtualFileMinimalImplementation(childFile1);
        String child2 = "child2";
        File childFile2 = new File(parentFile, child2);
        VirtualFile vf2 = new VirtualFileMinimalImplementation(childFile2);
        assertThat(vf1.compareTo(vf2), lessThan(0));
    }

    @Test
    public void testCompareTo_GreaterThan() throws IOException {
        String parentFolder = "parentFolder";
        File parentFile = tmp.newFolder(parentFolder);
        String child1 = "child1";
        File childFile1 = new File(parentFile, child1);
        VirtualFile vf1 = new VirtualFileMinimalImplementation(childFile1);
        String child2 = "child2";
        File childFile2 = new File(parentFile, child2);
        VirtualFile vf2 = new VirtualFileMinimalImplementation(childFile2);
        assertThat(vf2.compareTo(vf1), greaterThan(0));
    }

    @Test
    public void hasSymlink_AbstractBase() throws IOException {
        // This test checks the method's behavior in the abstract base class,
        // which generally does nothing.
        VirtualFile virtualRoot = new VirtualFileMinimalImplementation(tmp.getRoot());
        assertFalse(virtualRoot.hasSymlink(true));
    }

    @Test
    public void hasSymlink_False_FilePathVF() throws IOException {
        VirtualFile virtualRoot = VirtualFile.forFilePath(new FilePath(tmp.getRoot()));
        assertFalse(virtualRoot.hasSymlink(true));
    }

    @Test
    public void hasSymlink_True_FilePathVF() throws IOException, InterruptedException {
        FilePath rootPath = new FilePath(tmp.getRoot());
        FilePath childPath = rootPath.child("child");
        childPath.touch(0);
        FilePath symlinkPath = rootPath.child("symlink");
        symlinkPath.symlinkTo(childPath.getName(), null);
        VirtualFile virtualFile = VirtualFile.forFilePath(symlinkPath);
        assertTrue(virtualFile.hasSymlink(true));
    }

    @Test
    public void hasSymlink_False_FileVF() throws IOException {
        VirtualFile virtualRoot = VirtualFile.forFile(tmp.getRoot());
        assertFalse(virtualRoot.hasSymlink(true));
    }

    @Test
    public void hasSymlink_True_FileVF() throws IOException, InterruptedException {
        FilePath rootPath = new FilePath(tmp.getRoot());
        FilePath childPath = rootPath.child("child");
        childPath.touch(0);
        FilePath symlinkPath = rootPath.child("symlink");
        symlinkPath.symlinkTo(childPath.getName(), null);
        VirtualFile virtualFile = VirtualFile.forFile(new File(symlinkPath.toURI()));
        assertTrue(virtualFile.hasSymlink(true));
    }

    private File createInvalidDirectorySymlink(String invalidSymlinkName) throws IOException, InterruptedException {
        File ws = tmp.newFolder("ws");
        String externalFolderName = "external";
        tmp.newFolder(externalFolderName);
        Util.createSymlink(ws, "../" + externalFolderName, invalidSymlinkName, TaskListener.NULL);
        return ws;
    }

    private File createInvalidFileSymlink() throws IOException, InterruptedException {
        File ws = tmp.newFolder("ws");
        String externalFolderName = "external";
        File externalFile = tmp.newFolder(externalFolderName);
        String childString = "child";
        FileUtils.write(new File(externalFile, childString), childString);
        Util.createSymlink(ws, "../" + externalFolderName, "invalidSymlink", TaskListener.NULL);
        return ws;
    }

    private long computeEarlierSystemTime() {
        long earlierSystemTime = 0L;
        if (Functions.isWindows()) {
            return 0L;
        }
        Date date = new GregorianCalendar(2018, Calendar.JANUARY, 1).getTime();
        return date.getTime();
    }

    private static class VirtualFileMinimalImplementation extends VirtualFile {

        private File file;
        private File root;

        VirtualFileMinimalImplementation() {
        }

        VirtualFileMinimalImplementation(File file) {
            this(file, file);
        }

        VirtualFileMinimalImplementation(File file, File root) {
            this.file = file;
            this.root = root;
        }

        @Nonnull
        @Override
        public String getName() {
            return file.getName();
        }

        @Nonnull
        @Override
        public URI toURI() {
            return file.toURI();
        }

        @Override
        public VirtualFile getParent() {
            return null;
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public boolean isFile() {
            return false;
        }

        @Override
        public boolean exists() {
            return false;
        }

        @Nonnull
        @Override
        public VirtualFile[] list() {
            File[] kids = file.listFiles();
            if (kids == null) {
                return new VirtualFile[0];
            }
            VirtualFile[] vfs = new VirtualFile[kids.length];
            for (int i = 0; i < kids.length; i++) {
                vfs[i] = child(kids[i], root);
            }
            return vfs;
        }

        protected VirtualFile child(File kid, File root) {
            return new VirtualFileMinimalImplementation(kid, root);
        }

        @Nonnull
        @Override
        public VirtualFile child(@Nonnull String name) {
            return child(new File(file, name), root);
        }

        @Override
        public long length() {
            return 0;
        }

        @Override
        public long lastModified() {
            return 0;
        }

        @Override
        public boolean canRead() {
            return false;
        }

        @Override
        public InputStream open() throws IOException {
            return Files.newInputStream(file.toPath());
        }

    }

    private static class VirtualFileMinimalImplementationWithDescendants extends VirtualFileMinimalImplementation {

        public VirtualFileMinimalImplementationWithDescendants(File file) {
            super(file);
        }

        public VirtualFileMinimalImplementationWithDescendants(File file, File root) {
            super(file, root);
        }

        @Override
        public boolean supportIsDescendant() {
            return true;
        }

        @Override
        public boolean isDescendant(String childRelativePath) throws IOException {
            return true;
        }

        @Override
        protected VirtualFile child(File kid, File root) {
            return new VirtualFileMinimalImplementationWithDescendants(kid, root);
        }
    }

}
