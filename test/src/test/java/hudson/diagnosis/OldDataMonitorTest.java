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

package hudson.diagnosis;

import hudson.model.Actionable; // CAP AL
import hudson.XmlFile;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.InvisibleAction;
import hudson.model.Saveable;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import jenkins.model.lazy.BuildReference;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MemoryAssert;
import org.jvnet.hudson.test.recipes.LocalData;
import org.kohsuke.stapler.Stapler;

public class OldDataMonitorTest {

    static {
        // To make memory run faster:
        System.setProperty(BuildReference.DefaultHolderFactory.MODE_PROPERTY, "weak");
    }

    @Rule public JenkinsRule r = new JenkinsRule();

    @Ignore("constantly failing on CI builders, makes problems for memory()")
    @Issue("JENKINS-19544")
    @LocalData
    @Test public void robustness() {
        OldDataMonitor odm = OldDataMonitor.get(r.jenkins);
        FreeStyleProject p = r.jenkins.getItemByFullName("busted", FreeStyleProject.class);
        assertNotNull(p);
        /*
        System.err.println(p.getActions());
        for (Map.Entry<Saveable,OldDataMonitor.VersionRange> entry : odm.getData().entrySet()) {
            System.err.println(entry.getKey());
            System.err.println(entry.getValue());
            System.err.println(entry.getValue().extra);
        }
        */
        extractedMethod33537(p, odm); // CAP AL
        // did not manage to save p, but at least we are not holding onto a reference to it anymore
    }

    @Issue("JENKINS-19544")
    @Test public void memory() throws Exception {
        FreeStyleProject p = r.createFreeStyleProject("p");
        FreeStyleBuild b = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
        b.addAction(new BadAction2());
        b.save();
        r.jenkins.getQueue().clearLeftItems();
        p._getRuns().purgeCache();
        b = p.getBuildByNumber(1);
        assertEquals(Collections.singleton(b), OldDataMonitor.get(r.jenkins).getData().keySet());
        WeakReference<?> ref = new WeakReference<Object>(b);
        b = null;
        MemoryAssert.assertGC(ref, true);
    }

    /**
     * Note that this doesn't actually run slowly, it just ensures that
     * the {@link OldDataMonitor#changeListener's onChange()} can complete
     * while {@link OldDataMonitor#doDiscard(org.kohsuke.stapler.StaplerRequest, org.kohsuke.stapler.StaplerResponse)}
     * is still running.
     *
     */
    // Test timeout indicates JENKINS-24763 exists
    @Issue("JENKINS-24763")
    @Test public void slowDiscard() throws InterruptedException, IOException, ExecutionException {
        final OldDataMonitor oldDataMonitor = OldDataMonitor.get(r.jenkins);
        final CountDownLatch ensureEntry= new CountDownLatch(1);
        final CountDownLatch preventExit = new CountDownLatch(1);
        Saveable slowSavable = new Saveable() {
            @Override
            public void save() {
                try {
                    ensureEntry.countDown();
                    preventExit.await();
                } catch (InterruptedException e) {
                }
            }
        };

        OldDataMonitor.report(slowSavable,(String)null);
        ExecutorService executors = Executors.newSingleThreadExecutor();

        Future<Void> discardFuture = executors.submit(new Callable<Void>() {
            @Override
            public Void call() {
                oldDataMonitor.doDiscard(Stapler.getCurrentRequest(), Stapler.getCurrentResponse());
                return null;
            }
        });

        ensureEntry.await();
        // test will hang here due to JENKINS-24763
        File xml = File.createTempFile("OldDataMonitorTest.slowDiscard", "xml");
        xml.deleteOnExit();
        OldDataMonitor.changeListener
                .onChange(new Saveable() {public void save() {}},
                        new XmlFile(xml));

        preventExit.countDown();
        discardFuture.get();

    }

    @Issue("JENKINS-26718")
    @Test public void unlocatableRun() throws Exception {
        OldDataMonitor odm = OldDataMonitor.get(r.jenkins);
        FreeStyleProject p = r.createFreeStyleProject();
        FreeStyleBuild build = r.buildAndAssertSuccess(p);
        p.delete();
        OldDataMonitor.report(build, (String) null);

        extractedMethod33537(build, odm); // CAP AL

    }
 // CAP AL
    private void extractedMethod33537(final Actionable build, final OldDataMonitor odm) { // CAP AL
        assertEquals(Collections.singleton(build), odm.getData().keySet()); // CAP AL
        odm.doDiscard(null, null); // CAP AL
        assertEquals(Collections.emptySet(), odm.getData().keySet()); // CAP AL
    } // CAP AL

    public static final class BadAction extends InvisibleAction {
        private Object writeReplace() {
            throw new IllegalStateException("broken");
        }
    }

    public static final class BadAction2 extends InvisibleAction {
        private Object readResolve() {
            throw new IllegalStateException("broken");
        }
    }

}
