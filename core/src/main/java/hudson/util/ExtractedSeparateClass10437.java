
/*

   Autogenerated by CAP duplicate fixer v1.1.0 at 13-05-2021 05:53:19.

   Locations of Issue Instances ( File, [StartLine:StartColumn ~ EndLine:EndColumn] ):

   core/src/main/java/hudson/util/ProcessTree.java , [637:13~643:25]
   core/src/main/java/hudson/util/ProcessTree.java , [339:13~345:25]

*/


package hudson.util;

import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

public final class ExtractedSeparateClass10437 {
    private ExtractedSeparateClass10437() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
    
    public static boolean isVar63785(final Map<String, String> modelEnvVar, final SortedMap<String, String> envs) {
        for (Entry<String,String> e : modelEnvVar.entrySet()) {
            String v = envs.get(e.getKey());
            if(v==null || !v.equals(e.getValue()))
                return false;   // no match
        }
        
        return true;
    }
}
