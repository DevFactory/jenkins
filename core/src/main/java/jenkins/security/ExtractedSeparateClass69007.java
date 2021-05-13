
/*

   Autogenerated by CAP duplicate fixer v1.1.0 at 13-05-2021 05:01:36.

   Locations of Issue Instances ( File, [StartLine:StartColumn ~ EndLine:EndColumn] ):

   core/src/main/java/jenkins/security/ImpersonatingExecutorService.java , [64:9~71:11]
   core/src/main/java/jenkins/security/ImpersonatingScheduledExecutorService.java , [63:9~70:11]

*/


package jenkins.security;

import hudson.security.ACL;
import hudson.security.ACLContext;
import org.springframework.security.core.Authentication;

public final class ExtractedSeparateClass69007 {
    private ExtractedSeparateClass69007() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
    
    public static Runnable getVar33747(final Runnable r) {
        Authentication authentication = null;
        return new Runnable() {
            @Override
            public void run() {
                try (ACLContext ctxt = ACL.as2(authentication)) {
                    r.run();
                }
            }
        };
    }
}
