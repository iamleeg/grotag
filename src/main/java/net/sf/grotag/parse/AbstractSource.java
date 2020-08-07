package net.sf.grotag.parse;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * An input source from which lines can be read.
 * 
 * @author Thomas Aglassinger
 */
public abstract class AbstractSource {
    /**
     * Full name of the source, describing it in a way that it can not be mixed
     * with other sources.
     */
    abstract public String getFullName();

    /**
     * Short name of the source, which can be used to identify it within a set
     * of related sources. This name is not guaranteed to be unique.
     */
    abstract public String getShortName();

    /**
     * Create a Reader for the source.
     */
    abstract public BufferedReader createBufferedReader() throws IOException;
}
