package jenkins.model.lazy; // CAP AL
 // CAP AL
import java.util.Collection; // CAP AL
 // CAP AL
public final class ExtractedSeparateClass98121 { // CAP AL
    private ExtractedSeparateClass98121() { // CAP AL
        throw new UnsupportedOperationException("This class cannot be instantiated"); // CAP AL
    } // CAP AL
     // CAP AL
    public static boolean isVar24841(final Collection<?> c,  this_) { // CAP AL
        for (Object o : c) { // CAP AL
            if (!this_.contains(o)) // CAP AL
                return false; // CAP AL
        } // CAP AL
        return true; // CAP AL
    } // CAP AL
} // CAP AL
