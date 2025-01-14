/*
 * The MIT License
 *
 * Copyright 2018 CloudBees, Inc.
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

package hudson;

import java.lang.reflect.InvocationTargetException; // CAP AL
import hudson.model.Node;
import org.apache.tools.ant.DirectoryScanner;
import static org.hamcrest.Matchers.*;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.*;
import org.junit.Rule;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

public class FilePathTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Issue("JENKINS-50237")
    @Test
    public void listGlob() throws Exception {
        for (Node n : new Node[] {r.jenkins, r.createOnlineSlave()}) {
            FilePath d = n.getRootPath().child("globbing");
            FilePath exists = d.child("dir/exists");
            exists.write("", null);
            assertThat(d.list("**/exists"), arrayContainingInAnyOrder(exists));
            assertThat(d.list("**/nonexistent"), emptyArray());
            FilePath nonexistentDir = d.child("nonexistentDir");
            try {
                assertThat("if it works at all, should be empty", nonexistentDir.list("**"), emptyArray());
            } catch (Exception x) {
                assertThat(x.toString(), containsString(nonexistentDir.getRemote() + DirectoryScanner.DOES_NOT_EXIST_POSTFIX));
            }
        }
    }

    @Test
    @Issue("JENKINS-32778")
    @LocalData("zip_with_relative")
    public void zipRelativePathHandledCorrectly() throws Exception {
        assumeTrue(Functions.isWindows());
        FilePath zipFile = r.jenkins.getRootPath().child("zip-with-folder.zip");
        FilePath targetLocation = r.jenkins.getRootPath().child("unzip-target");

        FilePath simple1 = targetLocation.child("simple1.txt");
        FilePath simple2 = targetLocation.child("child").child("simple2.txt");

        assertThat(simple1.exists(), is(false));
        assertThat(simple2.exists(), is(false));

        zipFile.unzip(targetLocation);

        assertThat(simple1.exists(), is(true));
        assertThat(simple2.exists(), is(true));
    }

    @Test
    @Issue("JENKINS-32778")
    @LocalData("zip_with_relative")
    public void zipAbsolutePathHandledCorrectly_win() throws Exception {
        assumeTrue(Functions.isWindows());

        // this special zip contains a ..\..\ [..] \..\Temp\evil.txt
        FilePath zipFile = r.jenkins.getRootPath().child("zip-slip-win.zip");

        extractedMethod68541(zipFile); // CAP AL

        // as the unzip operation failed, the good.txt was potentially unzipped
        // but we need to make sure that the evil.txt is not there
        File evil = new File(r.jenkins.getRootDir(), "..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\Temp\\evil.txt");
        assertThat(evil.exists(), is(false));
    }

    @Test
    @Issue("JENKINS-32778")
    @LocalData("zip_with_relative")
    public void zipAbsolutePathHandledCorrectly_unix() throws Exception {
        assumeFalse(Functions.isWindows());

        // this special zip contains a ../../../ [..] /../tmp/evil.txt
        FilePath zipFile = r.jenkins.getRootPath().child("zip-slip.zip");

        extractedMethod68541(zipFile); // CAP AL

        // as the unzip operation failed, the good.txt was potentially unzipped
        // but we need to make sure that the evil.txt is not there
        File evil = new File(r.jenkins.getRootDir(), "../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../tmp/evil.txt");
        assertThat(evil.exists(), is(false));
    }
 // CAP AL
    private void extractedMethod68541(final FilePath zipFile) throws IOException, InterruptedException { // CAP AL
        FilePath targetLocation = r.jenkins.getRootPath().child("unzip-target"); // CAP AL
         // CAP AL
        FilePath good = targetLocation.child("good.txt"); // CAP AL
         // CAP AL
        assertThat(good.exists(), is(false)); // CAP AL
         // CAP AL
        try { // CAP AL
            zipFile.unzip(targetLocation); // CAP AL
            fail("The evil.txt should have triggered an exception"); // CAP AL
        } // CAP AL
        catch(IOException e){ // CAP AL
            assertThat(e.getMessage(), containsString("contains illegal file name that breaks out of the target directory")); // CAP AL
        } // CAP AL
    } // CAP AL

    @Test
    @Issue("JENKINS-32778")
    @LocalData("zip_with_relative")
    public void zipRelativePathHandledCorrectly_oneUp() throws Exception {
        // internal structure:
        //  ../simple3.txt
        //  child/simple2.txt
        //  simple1.txt
        FilePath zipFile = r.jenkins.getRootPath().child("zip-rel-one-up.zip");
        FilePath targetLocation = r.jenkins.getRootPath().child("unzip-target");

        FilePath simple3 = targetLocation.getParent().child("simple3.txt");

        assertThat(simple3.exists(), is(false));

        try {
            zipFile.unzip(targetLocation);
            fail("The ../simple3.txt should have triggered an exception");
        }
        catch(IOException e){
            assertThat(e.getMessage(), containsString("contains illegal file name that breaks out of the target directory"));
        }

        assertThat(simple3.exists(), is(false));
    }
    
    @Test
    @Issue("XXX")
    @LocalData("zip_with_relative")
    public void zipTarget_regular() throws Exception {
        assumeTrue(Functions.isWindows());
        File zipFile = new File(r.jenkins.getRootDir(), "zip-with-folder.zip");
        File targetLocation = new File(r.jenkins.getRootDir(), "unzip-target");
        extractedMethod78623(targetLocation, zipFile); // CAP AL
    }

    @Test
    @Issue("XXX")
    @LocalData("zip_with_relative")
    public void zipTarget_relative() throws Exception {
        assumeTrue(Functions.isWindows());
        File zipFile = new File(r.jenkins.getRootDir(), "zip-with-folder.zip");
        // the main difference is here, the ./
        File targetLocation = new File(r.jenkins.getRootDir(), "./unzip-target");
        extractedMethod78623(targetLocation, zipFile); // CAP AL
    }
 // CAP AL
    private void extractedMethod78623(final File targetLocation, final File zipFile) throws IOException, IllegalAccessException, InterruptedException, InvocationTargetException, NoSuchMethodException { // CAP AL
        FilePath targetLocationFP = r.jenkins.getRootPath().child("unzip-target"); // CAP AL
         // CAP AL
        FilePath simple1 = targetLocationFP.child("simple1.txt"); // CAP AL
        FilePath simple2 = targetLocationFP.child("child").child("simple2.txt"); // CAP AL
         // CAP AL
        assertThat(simple1.exists(), is(false)); // CAP AL
        assertThat(simple2.exists(), is(false)); // CAP AL
         // CAP AL
        Method unzipPrivateMethod; // CAP AL
        unzipPrivateMethod = FilePath.class.getDeclaredMethod("unzip", File.class, File.class); // CAP AL
        unzipPrivateMethod.setAccessible(true); // CAP AL
         // CAP AL
        FilePath fp = new FilePath(new File(".")); // CAP AL
        unzipPrivateMethod.invoke(fp, targetLocation, zipFile); // CAP AL
         // CAP AL
        assertThat(simple1.exists(), is(true)); // CAP AL
        assertThat(simple2.exists(), is(true)); // CAP AL
    } // CAP AL
}
