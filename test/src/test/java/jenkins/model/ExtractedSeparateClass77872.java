
/*

   Autogenerated by CAP duplicate fixer v1.1.0 at 13-05-2021 05:48:04.

   Locations of Issue Instances ( File, [StartLine:StartColumn ~ EndLine:EndColumn] ):

   test/src/test/java/hudson/cli/DeleteBuildsCommandTest.java , [273:9~275:42]
   test/src/test/java/jenkins/model/GlobalBuildDiscarderTest.java , [41:13~43:40]
   test/src/test/java/jenkins/model/GlobalBuildDiscarderTest.java , [92:13~94:41]

*/


package jenkins.model;

import hudson.model.FreeStyleProject;
import org.jvnet.hudson.test.JenkinsRule;

public final class ExtractedSeparateClass77872 {
    private ExtractedSeparateClass77872() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
    
    public static void extractedMethod51196(final JenkinsRule j, final FreeStyleProject project) throws Exception {
        j.buildAndAssertSuccess(project);
        j.buildAndAssertSuccess(project);
        j.buildAndAssertSuccess(project);
    }
}
