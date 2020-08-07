package net.sf.grotag.parse;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

/**
 * An input source to read lines from a String.
 * 
 * @author Thomas Aglassinger
 */
public class StringSource extends AbstractSource {
    private String fullName;
    private String shortName;
    private String text;

    public StringSource(String newFullName, String newText) {
        assert newFullName != null;
        assert newFullName.length() > 0;
        assert newText != null;

        fullName = newFullName;
        shortName = new File(fullName).getName();
        text = newText;
    }

    @Override
    public BufferedReader createBufferedReader() throws IOException {
        return new BufferedReader(new StringReader(text));
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    /**
     * Short version of <code>getFullName()</code> with only the part after
     * the last <code>File.separator</code>.
     * 
     * @see File#separator
     */
    @Override
    public String getShortName() {
        return shortName;
    }
}
