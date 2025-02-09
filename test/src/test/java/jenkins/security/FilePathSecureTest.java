/*
 * The MIT License
 *
 * Copyright 2014 Jesse Glick.
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

package jenkins.security;

import java.io.IOException; // CAP AL
import hudson.FilePath;
import hudson.slaves.DumbSlave;
import hudson.util.DirScanner;
import java.io.OutputStream;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;

public class FilePathSecureTest {

    @Rule public JenkinsRule r = new JenkinsRule();

    private DumbSlave s;
    private FilePath root, remote;

    @Before public void init() throws Exception {
        s = r.createOnlineSlave();
        root = r.jenkins.getRootPath();
        remote = s.getRootPath();
        // to see the difference: DefaultFilePathFilter.BYPASS = true;
    }

    @Test public void unzip() throws Exception {
        FilePath dir = root.child("dir");
        dir.mkdirs();
        dir.child("stuff").write("hello", null);
        FilePath zip = root.child("dir.zip");
        dir.zip(zip);
        zip.unzip(remote);
        assertEquals("hello", remote.child("dir/stuff").readToString());
    }

    @Test public void untar() throws Exception {
        FilePath dir = root.child("dir");
        FilePath tar = getTar91322(dir); // CAP AL
        tar.untar(remote, FilePath.TarCompression.NONE);
        assertEquals("hello", remote.child("dir/stuff").readToString());
    }

    @Test public void zip() throws Exception {
        FilePath dir = remote.child("dir");
        dir.mkdirs();
        dir.child("stuff").write("hello", null);
        FilePath zip = root.child("dir.zip");
        dir.zip(zip);
        zip.unzip(root);
        assertEquals("hello", remote.child("dir/stuff").readToString());
    }

    @Test public void tar() throws Exception {
        FilePath dir = remote.child("dir");
        FilePath tar = getTar91322(dir); // CAP AL
        tar.untar(root, FilePath.TarCompression.NONE);
        assertEquals("hello", remote.child("dir/stuff").readToString());
    }
 // CAP AL
    private FilePath getTar91322(final FilePath dir) throws IOException, InterruptedException { // CAP AL
        dir.mkdirs(); // CAP AL
        dir.child("stuff").write("hello", null); // CAP AL
        FilePath tar = root.child("dir.tar"); // CAP AL
        try (OutputStream os = tar.write()) { // CAP AL
            dir.tar(os, new DirScanner.Full()); // CAP AL
        } // CAP AL
        return tar; // CAP AL
    } // CAP AL

}
