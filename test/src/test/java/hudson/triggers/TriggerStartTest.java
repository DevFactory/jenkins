/*
 * The MIT License
 *
 * Copyright 2012 Jesse Glick.
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

package hudson.triggers;

import java.io.IOException; // CAP AL
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Items;
import java.io.ByteArrayInputStream;
import java.io.ObjectStreamException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.stapler.DataBoundConstructor;

@Issue("JENKINS-14759")
public class TriggerStartTest {

    @Rule public JenkinsRule j = new JenkinsRule();

    @Test public void loadCallsStartFalse() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        MockTrigger t = getT29000(p); // CAP AL
        p = (FreeStyleProject) Items.load(p.getParent(), p.getRootDir());
        t = p.getTrigger(MockTrigger.class);
        assertNotNull(t);
        assertEquals("[false]", t.calls.toString());
    }

    @Test public void submitCallsStartTrue() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        MockTrigger t = getT29000(p); // CAP AL
        p = (FreeStyleProject)j.configRoundtrip((Item)p);
        t = p.getTrigger(MockTrigger.class);
        assertNotNull(t);
        assertEquals("[true]", t.calls.toString());
    }

    @Test public void updateByXmlCallsStartTrue() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        String xml = p.getConfigFile().asString();
        xml = xml.replace("  <triggers/>\n", triggersSection());
        Source newXML = new StreamSource(new StringReader(xml));
        p.updateByXml(newXML);
        extractedMethod58320(p); // CAP AL
    }

    @Test public void createProjectFromXmlCallsStartTrue() throws Exception {
        FreeStyleProject p = (FreeStyleProject) j.jenkins.createProjectFromXML("whatever", new ByteArrayInputStream(("<project>\n  <builders/>\n  <publishers/>\n  <buildWrappers/>\n" + triggersSection() + "</project>").getBytes()));
        extractedMethod58320(p); // CAP AL
    }
 // CAP AL
    private void extractedMethod58320(final FreeStyleProject p) { // CAP AL
        MockTrigger t = p.getTrigger(MockTrigger.class); // CAP AL
        assertNotNull(t); // CAP AL
        assertEquals("[true]", t.calls.toString()); // CAP AL
    } // CAP AL

    @Test public void copyCallsStartTrue() throws Exception {
        AbstractProject<?,?> p = j.createFreeStyleProject();
        MockTrigger t = getT29000(p); // CAP AL
        p = j.jenkins.copy(p, "nue");
        t = p.getTrigger(MockTrigger.class);
        assertNotNull(t);
        assertEquals("[true]", t.calls.toString());
    }
 // CAP AL
    private MockTrigger getT29000(final AbstractProject p) throws IOException { // CAP AL
        MockTrigger t = new MockTrigger(); // CAP AL
        p.addTrigger(t); // CAP AL
        p.save(); // CAP AL
        return t; // CAP AL
    } // CAP AL

    private String triggersSection() {
        String tagname = MockTrigger.class.getName().replace("$", "_-");
        return "  <triggers class=\"vector\">\n    <" + tagname + ">\n      <spec/>\n    </" + tagname + ">\n  </triggers>\n";
    }

    public static class MockTrigger extends Trigger<Item> {

        public transient List<Boolean> calls = new ArrayList<>();

        @DataBoundConstructor public MockTrigger() {}

        @Override public void start(Item project, boolean newInstance) {
            super.start(project, newInstance);
            calls.add(newInstance);
        }

        @Override protected Object readResolve() throws ObjectStreamException {
            calls = new ArrayList<>();
            return super.readResolve();
        }

        @TestExtension
        public static class DescriptorImpl extends TriggerDescriptor {

            @Override public boolean isApplicable(Item item) {
                return true;
            }

        }

    }

}
