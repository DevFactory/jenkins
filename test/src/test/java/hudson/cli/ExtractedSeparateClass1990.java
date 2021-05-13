
/*

   Autogenerated by CAP duplicate fixer v1.1.0 at 13-05-2021 05:07:15.

   Locations of Issue Instances ( File, [StartLine:StartColumn ~ EndLine:EndColumn] ):

   test/src/test/java/hudson/cli/OfflineNodeCommandTest.java , [434:9~440:74]
   test/src/test/java/hudson/cli/OfflineNodeCommandTest.java , [411:9~417:74]
   test/src/test/java/hudson/cli/OfflineNodeCommandTest.java , [359:9~365:74]
   test/src/test/java/hudson/cli/OfflineNodeCommandTest.java , [385:9~391:74]
   test/src/test/java/hudson/cli/DisconnectNodeCommandTest.java , [221:9~227:74]
   test/src/test/java/hudson/cli/DisconnectNodeCommandTest.java , [247:9~253:74]

*/


package hudson.cli;

import hudson.slaves.DumbSlave;
import org.jvnet.hudson.test.JenkinsRule;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public final class ExtractedSeparateClass1990 {
    private ExtractedSeparateClass1990() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
    
    public static DumbSlave getSlave268922(final JenkinsRule j, final DumbSlave slave1) throws Exception {
        DumbSlave slave2 = j.createSlave("aNode2", "", null);
        slave1.toComputer().waitUntilOnline();
        assertThat(slave1.toComputer().isOnline(), equalTo(true));
        assertThat(slave1.toComputer().getOfflineCause(), equalTo(null));
        slave2.toComputer().waitUntilOnline();
        assertThat(slave2.toComputer().isOnline(), equalTo(true));
        assertThat(slave2.toComputer().getOfflineCause(), equalTo(null));
        return slave2;
    }
}
