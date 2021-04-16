package hudson.util; // CAP AL
 // CAP AL
import java.io.ByteArrayOutputStream; // CAP AL
import java.util.logging.Level; // CAP AL
import java.util.logging.Logger; // CAP AL
 // CAP AL
public final class ExtractedSeparateClass70328 { // CAP AL
    private ExtractedSeparateClass70328() { // CAP AL
        throw new UnsupportedOperationException("This class cannot be instantiated"); // CAP AL
    } // CAP AL
     // CAP AL
    public static String getLine2318(final ByteArrayOutputStream buf, final String prefix, final Level FINEST, final Logger LOGGER) { // CAP AL
        String line = buf.toString(); // CAP AL
        if(LOGGER.isLoggable(FINEST)) // CAP AL
            LOGGER.finest(prefix+" was "+line); // CAP AL
        return line; // CAP AL
    } // CAP AL
} // CAP AL
