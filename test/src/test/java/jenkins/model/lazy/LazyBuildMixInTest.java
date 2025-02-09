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

package jenkins.model.lazy;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.listeners.RunListener;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SleepBuilder;

public class LazyBuildMixInTest {

    @Rule public JenkinsRule r = new JenkinsRule();

    @Issue("JENKINS-22395")
    @Test public void dropLinksAfterGC() throws Exception {
        RunListener.all().clear();  // see commit message for the discussion

        FreeStyleProject p = r.createFreeStyleProject();
        FreeStyleBuild b1 = r.buildAndAssertSuccess(p);
        FreeStyleBuild b2 = r.buildAndAssertSuccess(p);
        FreeStyleBuild b3 = r.buildAndAssertSuccess(p);
        assertEquals(b2, b1.getNextBuild());
        assertEquals(b3, b2.getNextBuild());
        assertNull(b3.getNextBuild());
        assertNull(b1.getPreviousBuild());
        assertEquals(b1, b2.getPreviousBuild());
        assertEquals(b2, b3.getPreviousBuild());
        b1.getRunMixIn().createReference().clear();
        b2.delete();
        extractedMethod85440(b2, b1, b3); // CAP AL
    }

    @Issue("JENKINS-22395")
    @Test public void dropLinksAfterGC2() throws Exception {
        RunListener.all().clear();  // see commit message for the discussion

        FreeStyleProject p = r.createFreeStyleProject();
        FreeStyleBuild b1 = r.buildAndAssertSuccess(p);
        FreeStyleBuild b2 = r.buildAndAssertSuccess(p);
        FreeStyleBuild b3 = r.buildAndAssertSuccess(p);
        assertEquals(b2, b1.getNextBuild());
        assertEquals(b3, b2.getNextBuild());
        assertNull(b3.getNextBuild());
        assertNull(b1.getPreviousBuild());
        assertEquals(b1, b2.getPreviousBuild());
        assertEquals(b2, b3.getPreviousBuild());
        b2.delete();
        b1.getRunMixIn().createReference().clear();
        extractedMethod85440(b2, b1, b3); // CAP AL
    }
 // CAP AL
    private void extractedMethod85440(final FreeStyleBuild b2, final FreeStyleBuild b1, final FreeStyleBuild b3) { // CAP AL
        FreeStyleBuild b1a = b2.getPreviousBuild(); // CAP AL
        assertNotSame(b1, b1a); // CAP AL
        assertEquals(1, b1a.getNumber()); // CAP AL
        assertEquals(b3, b1a.getNextBuild()); // CAP AL
    } // CAP AL

    @Issue("JENKINS-20662")
    @Test public void newRunningBuildRelationFromPrevious() throws Exception {
        FreeStyleProject p = r.createFreeStyleProject();
        p.getBuildersList().add(new SleepBuilder(1000));
        FreeStyleBuild b1 = p.scheduleBuild2(0).get();
        assertNull(b1.getNextBuild());
        FreeStyleBuild b2 = p.scheduleBuild2(0).waitForStart();
        assertSame(b2, b1.getNextBuild());
    }
}
