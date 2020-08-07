package net.sf.grotag.parse;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import net.sf.grotag.common.Tools;

/**
 * An input source to read lines from a file.
 * 
 * @author Thomas Aglassinger
 */
public class FileSource extends AbstractSource {
    private File file;

    public FileSource(File newFile) {
        assert newFile != null;
        file = newFile;
    }

    public File getFile() {
        return file;
    }

    @Override
    public BufferedReader createBufferedReader() throws IOException {
        return Tools.getInstance().createBufferedReader(file, "ISO-8859-1");
    }

    @Override
    public String getShortName() {
        return file.getName();
    }

    @Override
    public String getFullName() {
        return file.getAbsolutePath();
    }
}
