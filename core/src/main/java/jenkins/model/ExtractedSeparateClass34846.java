
/*

   Autogenerated by CAP duplicate fixer v1.1.0 at 13-05-2021 05:51:08.

   Locations of Issue Instances ( File, [StartLine:StartColumn ~ EndLine:EndColumn] ):

   core/src/main/java/hudson/model/ParametersDefinitionProperty.java , [193:9~197:10]
   core/src/main/java/jenkins/model/ParameterizedJobMixIn.java , [217:9~221:10]

*/


package jenkins.model;

import java.io.IOException;
import hudson.model.Queue;
import hudson.model.Queue.Item;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;

public final class ExtractedSeparateClass34846 {
    private ExtractedSeparateClass34846() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
    
    public static void extractedMethod63436(final Queue.Item item, final StaplerResponse rsp, final StaplerRequest req, final int SC_CREATED) throws IOException {
        if (item != null) {
            rsp.sendRedirect(SC_CREATED, req.getContextPath() + '/' + item.getUrl());
        } else {
            rsp.sendRedirect(".");
        }
    }
}
