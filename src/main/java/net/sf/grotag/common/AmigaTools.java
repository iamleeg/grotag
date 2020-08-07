package net.sf.grotag.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import net.sf.grotag.common.AmigaPathList.AmigaPathFilePair;

/**
 * Amiga related tools.
 * 
 * @author Thomas Aglassinger
 */
public class AmigaTools {
    /**
     * The character encoding used by text files under AmigaOS. It actually is
     * ISO-8859-1 but cp1252 is compatible to it and also supports the Euro
     * character.
     */
    public static final String ENCODING = "cp1252";

    /**
     * Magic text to detect Amigaguide documents.
     */
    private static final String GUIDE_ID = "@database";

    private static AmigaTools instance;

    private Tools tools;

    private AmigaTools() {
        super();
        tools = Tools.getInstance();
    }

    public static final synchronized AmigaTools getInstance() {
        if (instance == null) {
            instance = new AmigaTools();
        }
        return instance;
    }

    public boolean isAmigaguide(File file) throws IOException {
        boolean result;
        try {
            ensureIsAmigaguide(file);
            result = true;
        } catch (IllegalArgumentException errorToIgnore) {
            result = false;
        }
        return result;
    }

    /**
     * Validate that <code>file</code> starts with <code>@database</code>.
     * @throws IllegalArgumentException
     *                 if the file does not start with the expected header
     */
    public void ensureIsAmigaguide(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        try {
            byte[] first9Bytes = new byte[GUIDE_ID.length()];
            in.read(first9Bytes);
            String id = new String(first9Bytes, AmigaTools.ENCODING);
            if (!id.toLowerCase().equals(GUIDE_ID)) {
                throw new IllegalArgumentException("Amigaguide document must start with " + tools.sourced(GUIDE_ID)
                        + " instead of " + tools.sourced(id) + ": ");
            }
        } finally {
            in.close();
        }
    }

    public File getFileFor(String amigaPath, AmigaPathList amigaPaths) {
        assert amigaPath != null;
        assert amigaPaths != null;
        String currentFolderPath = System.getProperty("user.dir");
        return getFileFor(amigaPath, new File(currentFolderPath), amigaPaths);
    }

    public File getFileFor(String amigaPath, File currentFolder, AmigaPathList amigaPaths) {
        assert amigaPath != null;
        assert currentFolder != null;
        assert amigaPaths != null;

        String result = "";
        int charIndex = 0;

        int colonIndex = amigaPath.indexOf(':');
        if (colonIndex >= 0) {
            // Resolve absolute Amiga path.
            String lowerAmigaPath = amigaPath.toLowerCase();
            int amigaPathIndex = 0;
            boolean pathFound = false;
            while (!pathFound && (amigaPathIndex < amigaPaths.items().size())) {
                AmigaPathFilePair pair = amigaPaths.items().get(amigaPathIndex);
                if (lowerAmigaPath.startsWith(pair.getAmigaPath())) {
                    pathFound = true;
                } else {
                    amigaPathIndex += 1;
                }
            }
            if (pathFound) {
                AmigaPathFilePair pairFound = amigaPaths.items().get(amigaPathIndex);
                File localFolder = pairFound.getLocalFolder();
                if (localFolder != null) {
                    result = localFolder.getAbsolutePath();
                    charIndex = pairFound.getAmigaPath().length();
                } else {
                    pathFound = false;
                    amigaPaths.addUndefined(amigaPath.substring(0, colonIndex));
                }
            }
            if (!pathFound) {
                // Assign unknown Amiga paths to the temporary folder.
                result = System.getProperty("java.io.tmpdir");
                charIndex = colonIndex + 1;
            }
        } else {
            charIndex = 0;
        }

        if (result.length() == 0) {
            result = currentFolder.getAbsolutePath();
        } else {
            result = new File(result).getAbsolutePath();
        }
        result += File.separator;

        // Resolve leading slashes.
        while ((charIndex < amigaPath.length()) && (amigaPath.charAt(charIndex) == '/')) {
            result = new File(result).getParent();
            charIndex += 1;
        }

        // Resolve double slashes within path.
        boolean lastWasSlash = false;
        while (charIndex < amigaPath.length()) {
            char ch = amigaPath.charAt(charIndex);
            if (ch == '/') {
                if (lastWasSlash) {
                    result = new File(result).getParent();
                } else {
                    lastWasSlash = true;
                }
            } else {
                if (lastWasSlash) {
                    result += File.separator;
                    lastWasSlash = false;
                }
                result += ch;
            }
            charIndex += 1;
        }

        // Amiga path terminates with slash.
        if (lastWasSlash) {
            result = new File(result).getParent();
        }

        // TODO: Adjust upper/lower case according to folders and files in local
        // file system.
        return new File(result);
    }

    public String escapedForAmigaguide(String some) {
        // TODO #3: Use streams.
        assert some != null;
        String result = "";
        for (int charIndex = 0; charIndex < some.length(); charIndex += 1) {
            char ch = some.charAt(charIndex);
            if (ch == '\\') {
                result += "\\\\";
            } else if (ch == '@') {
                result += "\\@";
            } else {
                result += ch;
            }
        }
        return result;
    }
}
