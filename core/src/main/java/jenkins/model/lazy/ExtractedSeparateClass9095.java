package jenkins.model.lazy; // CAP AL
 // CAP AL
import java.util.Collection; // CAP AL
 // CAP AL
public final class ExtractedSeparateClass9095 { // CAP AL
    private ExtractedSeparateClass9095() { // CAP AL
        throw new UnsupportedOperationException("This class cannot be instantiated"); // CAP AL
    } // CAP AL
     // CAP AL
    public static boolean isB18590(final Collection<?> c,  this_) { // CAP AL
        boolean b=false; // CAP AL
        for (Object o : c) { // CAP AL
            b|=this_.remove(o); // CAP AL
        } // CAP AL
        return b; // CAP AL
    } // CAP AL
} // CAP AL
