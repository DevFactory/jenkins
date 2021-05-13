
/*

   Autogenerated by CAP duplicate fixer v1.1.0 at 13-05-2021 05:53:48.

   Locations of Issue Instances ( File, [StartLine:StartColumn ~ EndLine:EndColumn] ):

   test/src/test/java/hudson/cli/QuietDownCommandTest.java , [468:9~472:43]
   test/src/test/java/hudson/cli/CancelQuietDownCommandTest.java , [122:9~126:43]
   test/src/test/java/hudson/cli/QuietDownCommandTest.java , [423:9~427:43]
   test/src/test/java/hudson/cli/QuietDownCommandTest.java , [291:9~295:43]
   test/src/test/java/hudson/cli/QuietDownCommandTest.java , [246:9~250:43]
   test/src/test/java/hudson/cli/QuietDownCommandTest.java , [337:9~341:43]
   test/src/test/java/hudson/cli/QuietDownCommandTest.java , [383:9~387:43]
   test/src/test/java/hudson/cli/QuietDownCommandTest.java , [202:9~206:43]
   test/src/test/java/hudson/cli/CancelQuietDownCommandTest.java , [158:9~162:43]
   test/src/test/java/hudson/cli/OnlineNodeCommandTest.java , [212:9~216:43]
   test/src/test/java/hudson/cli/OfflineNodeCommandTest.java , [260:9~264:43]
   test/src/test/java/hudson/cli/OfflineNodeCommandTest.java , [289:9~293:43]

*/


package hudson.cli;

import java.util.concurrent.Future;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.OneShotEvent;
import org.jvnet.hudson.test.JenkinsRule;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public final class ExtractedSeparateClass71350 {
    private ExtractedSeparateClass71350() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
    
    public static void extractedMethod90380(final OneShotEvent finish, final Future<FreeStyleBuild> build, final JenkinsRule j, final FreeStyleProject project) throws Exception {
        finish.signal();
        build.get();
        assertThat(((FreeStyleProject) j.jenkins.getItem("aProject")).getBuilds(), hasSize(1));
        assertThat(project.isBuilding(), equalTo(false));
        j.assertBuildStatusSuccess(build);
    }
}
