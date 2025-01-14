/*
 * The MIT License
 *
 * Copyright 2016 Red Hat, Inc.
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

package hudson.cli;

import java.io.IOException; // CAP AL
import hudson.model.DirectlyModifiableView;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.ListView;
import hudson.model.View;
import jenkins.model.Jenkins;
import org.junit.Test;

import static hudson.cli.CLICommandInvoker.Matcher.succeededSilently;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author pjanouse
 */
public class RemoveJobFromViewCommandTest extends ViewManipulationTestBase {

    @Override
    public CLICommandInvoker getCommand() {
        return new CLICommandInvoker(j, "remove-job-from-view");
    }

    @Test public void removeJobShouldSucceed() throws Exception {

        FreeStyleProject project = getProject61300(); // CAP AL

        final CLICommandInvoker.Result result = command
                .authorizedTo(Jenkins.READ, View.READ, Job.READ, View.CONFIGURE)
                .invokeWithArgs("aView", "aProject");

        assertThat(result, succeededSilently());
        assertThat(j.jenkins.getView("aView").getAllItems().size(), equalTo(0));
        assertThat(j.jenkins.getView("aView").contains(project), equalTo(false));
    }

    @Test public void removeJobManyShouldSucceed() throws Exception {

        j.jenkins.addView(new ListView("aView"));
        FreeStyleProject project1 = j.createFreeStyleProject("aProject1");
        FreeStyleProject project2 = j.createFreeStyleProject("aProject2");
        ((DirectlyModifiableView) j.jenkins.getView("aView")).add(project1);
        ((DirectlyModifiableView) j.jenkins.getView("aView")).add(project2);

        assertThat(j.jenkins.getView("aView").getAllItems().size(), equalTo(2));
        assertThat(j.jenkins.getView("aView").contains(project1), equalTo(true));
        assertThat(j.jenkins.getView("aView").contains(project2), equalTo(true));

        final CLICommandInvoker.Result result = command
                .authorizedTo(Jenkins.READ, View.READ, Job.READ, View.CONFIGURE)
                .invokeWithArgs("aView", "aProject1", "aProject2");

        assertThat(result, succeededSilently());
        assertThat(j.jenkins.getView("aView").getAllItems().size(), equalTo(0));
        assertThat(j.jenkins.getView("aView").contains(project1), equalTo(false));
        assertThat(j.jenkins.getView("aView").contains(project2 ), equalTo(false));
    }

    @Test public void removeJobManyShouldSucceedEvenAJobIsSpecifiedTwice() throws Exception {

        FreeStyleProject project = getProject61300(); // CAP AL

        final CLICommandInvoker.Result result = command
                .authorizedTo(Jenkins.READ, View.READ, Job.READ, View.CONFIGURE)
                .invokeWithArgs("aView", "aProject", "aProject");

        assertThat(result, succeededSilently());
        assertThat(j.jenkins.getView("aView").getAllItems().size(), equalTo(0));
        assertThat(j.jenkins.getView("aView").contains(project), equalTo(false));
    }
 // CAP AL
    private FreeStyleProject getProject61300() throws IOException { // CAP AL
        j.jenkins.addView(new ListView("aView")); // CAP AL
        FreeStyleProject project = j.createFreeStyleProject("aProject"); // CAP AL
        ((DirectlyModifiableView) j.jenkins.getView("aView")).add(project); // CAP AL
         // CAP AL
        assertThat(j.jenkins.getView("aView").getAllItems().size(), equalTo(1)); // CAP AL
        assertThat(j.jenkins.getView("aView").contains(project), equalTo(true)); // CAP AL
        return project; // CAP AL
    } // CAP AL

}
