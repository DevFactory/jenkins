package hudson.util; // CAP AL
 // CAP AL
public final class ExtractedSeparateClass63480 { // CAP AL
    private ExtractedSeparateClass63480() { // CAP AL
        throw new UnsupportedOperationException("This class cannot be instantiated"); // CAP AL
    } // CAP AL
     // CAP AL
    public static int getVar68451(final int i, final boolean IS_LITTLE_ENDIAN) { // CAP AL
        if(IS_LITTLE_ENDIAN) // CAP AL
            return (i<<24) |((i<<8) & 0x00FF0000) | ((i>>8) & 0x0000FF00) | (i>>>24); // CAP AL
        else // CAP AL
            return i; // CAP AL
    } // CAP AL
} // CAP AL
