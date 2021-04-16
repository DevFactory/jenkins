package hudson.util; // CAP AL
 // CAP AL
import java.util.Map; // CAP AL
import java.util.Map.Entry; // CAP AL
import java.util.SortedMap; // CAP AL
 // CAP AL
public final class ExtractedSeparateClass37470 { // CAP AL
    private ExtractedSeparateClass37470() { // CAP AL
        throw new UnsupportedOperationException("This class cannot be instantiated"); // CAP AL
    } // CAP AL
     // CAP AL
    public static boolean isVar76057(final Map<String, String> modelEnvVar, final SortedMap<String, String> envs) { // CAP AL
        for (Entry<String,String> e : modelEnvVar.entrySet()) { // CAP AL
            String v = envs.get(e.getKey()); // CAP AL
            if(v==null || !v.equals(e.getValue())) // CAP AL
                return false;   // no match // CAP AL
        } // CAP AL
         // CAP AL
        return true; // CAP AL
    } // CAP AL
} // CAP AL
