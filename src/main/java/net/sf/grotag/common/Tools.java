package net.sf.grotag.common;

import java.awt.Component;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

public class Tools {
    public enum DeleteResult {
        DELETED, DID_NOT_EXIST, FAILED
    }

    private static final String LOGGING_PROPERTIES = "logging.properties";
    private static final String DEFAULT_TOKEN_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";

    /**
     * Size of slack in buffer escaped characters get encoded to.
     */
    private static final int ESCAPE_SLACK_COUNT = 16;
    private static final int UNICODE_HEX_DIGIT_COUNT = 4;
    private static final int BUFFER_SIZE = 16384;

    private Map<Character, String> escapeMap;
    private Logger log;

    private static Tools instance;

    public static final synchronized Tools getInstance() {
        if (instance == null) {
            instance = new Tools();
        }
        return instance;
    }

    public BufferedReader createBufferedReader(File file) throws IOException {
        return createBufferedReader(file, null);
    }

    public BufferedReader createBufferedReader(File file, String encoding) throws IOException {
        assert file != null;
        BufferedReader result = null;
        FileInputStream fileInStream = new FileInputStream(file);
        try {
            InputStreamReader inStreamReader = new InputStreamReader(fileInStream, encoding);
            try {
                result = new BufferedReader(inStreamReader);
            } finally {
                if (result == null) {
                    inStreamReader.close();
                }
            }
        } finally {
            if (result == null) {
                fileInStream.close();
            }
        }
        return result;
    }

    public BufferedWriter createBufferedWriter(File file, String encoding) throws IOException {
        BufferedWriter result = null;
        FileOutputStream fileOutStream = new FileOutputStream(file);
        try {
            OutputStreamWriter outStreamWriter = new OutputStreamWriter(fileOutStream, encoding);
            try {
                result = new BufferedWriter(outStreamWriter);
            } finally {
                if (result == null) {
                    outStreamWriter.close();
                }
            }
        } finally {
            if (result == null) {
                fileOutStream.close();
            }
        }

        return result;
    }

    private Tools() {
        // Attempt to setup logging.
        String userDir = System.getProperty("user.dir");
        String userHome = System.getProperty("user.home");
        File[] possibleLoggingSetupFiles = new File[] { new File(userDir, LOGGING_PROPERTIES),
                new File(new File(userHome, ".grotag"), LOGGING_PROPERTIES) };
        boolean loggingSetup = false;
        int fileIndex = 0;

        while (!loggingSetup && (fileIndex < possibleLoggingSetupFiles.length)) {
            File loggingSetupFilePath = possibleLoggingSetupFiles[fileIndex];
            try {
                FileInputStream in = new FileInputStream(loggingSetupFilePath);
                try {
                    LogManager.getLogManager().readConfiguration(in);
                    Logger.getLogger(Tools.class.getName())
                            .info("setup loggers from: \"" + loggingSetupFilePath + "\"");
                } finally {
                    in.close();
                }
            } catch (FileNotFoundException errorToIgnore) {
                // Ignore that optional logging setup file could not be found.
            } catch (IOException error) {
                Logger.getLogger(Tools.class.getName()).severe(
                        "cannot read \"" + loggingSetupFilePath + "" + "\": " + error.getMessage());
            }
            log = Logger.getLogger(Tools.class.getName());
            fileIndex += 1;
        }

        escapeMap = new TreeMap<Character, String>();
        escapeMap.put(Character.valueOf('\"'), "\\\"");
        escapeMap.put(Character.valueOf('\''), "\\\'");
        escapeMap.put(Character.valueOf('\\'), "\\\\");
        escapeMap.put(Character.valueOf('\b'), "\\b");
        escapeMap.put(Character.valueOf('\f'), "\\f");
        escapeMap.put(Character.valueOf('\n'), "\\n");
        escapeMap.put(Character.valueOf('\r'), "\\r");
        escapeMap.put(Character.valueOf('\t'), "\\t");
    }

    private boolean isEscapable(Character some) {
        return escapeMap.containsKey(some);
    }

    /**
     * Attempt to recursively delete all files in <code>dir</code>. Every
     * file or directory that cannot be deleted causes a warning in the log.
     */
    public void attemptToDeleteAll(File folder) {
        assert folder != null;
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();

            // TODO #3: Check if steams make sense here.
            for (int i = 0; i < files.length; i += 1) {
                attemptToDeleteAll(files[i]);
            }
        }
        deleteOrWarn(folder);
    }

    /**
     * Attempt to delete <code>file</code>. If this fails, log a warning.
     */
    public DeleteResult deleteOrWarn(File file) {
        assert file != null;
        DeleteResult result;
        boolean deleted = file.delete();

        if (deleted) {
            result = DeleteResult.DELETED;
            log.fine("deleted \"" + file + "\"");
        } else if (file.exists()) {
            log.warning("cannot delete \"" + file + "\"");
            result = DeleteResult.FAILED;
        } else {
            log.warning("cannot delete non-existent \"" + file + "\"");
            result = DeleteResult.DID_NOT_EXIST;
        }
        return result;
    }

    /**
     * Copy of <code>some</code> with any trailing white space removed.
     */
    public String withoutTrailingWhiteSpace(String some) {
        assert some != null;

        int i = some.length() - 1;
        while ((i >= 0) && (Character.isWhitespace(some.charAt(i)))) {
            i -= 1;
        }
        return some.substring(0, i + 1);
    }

    /**
     * Copy of <code>some</code> with any whitespace removed.
     */
    public String withoutWhiteSpace(String some) {
        assert some != null;
        String result = "";

        // TODO #3: Use streams.
        for (char ch : some.toCharArray()) {
            if (!Character.isWhitespace(ch)) {
                result += ch;
            }
        }
        return result;
    }

    public String[] splitFile(File file) {
        List<String> result = new LinkedList<String>();
        File parentRider = file.getAbsoluteFile();

        while (parentRider.getName().length() > 0) {
            result.add(0, parentRider.getName());
            parentRider = parentRider.getParentFile();
        }
        return result.toArray(new String[] {});
    }

    /**
     * The relative URL to link from <code>linkingFile</code> to
     * <code>targetFile</target>.
     */
    public String getRelativeUrl(File linkingFile, File targetFile) {
        // TODO: Check if there is a standard library function for this by now.
        assert linkingFile != null;
        assert targetFile != null;

        log.fine(sourced(linkingFile));
        log.fine(sourced(targetFile));
        String result = "";
        String[] linkingParts = splitFile(linkingFile);
        String[] targetParts = splitFile(targetFile);
        int commonIndex = 0;
        while ((commonIndex < linkingParts.length) && (commonIndex < targetParts.length)
                && linkingParts[commonIndex].equals(targetParts[commonIndex])) {
            commonIndex += 1;
        }

        for (int parentIndex = commonIndex + 1; parentIndex < linkingParts.length; parentIndex += 1) {
            result += "../";
        }

        boolean isFirstTargetPart = true;
        for (int targetIndex = commonIndex; targetIndex < targetParts.length; targetIndex += 1) {
            if (isFirstTargetPart) {
                isFirstTargetPart = false;
            } else {
                result += "/";
            }
            result += targetParts[targetIndex];
        }

        log.fine("-> " + sourced(result));
        return result;
    }

    /**
     * Same as <code>sourced(String)</code>, but uses absolute file path.
     * 
     * @see #sourced(String)
     */
    public/* @ pure @ */String sourced(/* @ nullable @ */File some) {
        String result;

        if (some == null) {
            result = sourced((String) null);
        } else {
            result = sourced(some.getAbsolutePath());
        }
        return result;
    }

    /**
     * Source code version of <code>some</code> that can be pasted into a Java
     * source. The result is embedded in two quotes, escape characters are
     * rendered where possible. Invisible characters are rendered as unicode
     * escape. The value <code>null</code> results in the the text "null"
     * (without quotes).
     */
    public/* @ pure @ */String sourced(/* @ nullable @ */String some) {
        // Check if there is a standard library function for that or streams are applicable.
        String result;

        if (some == null) {
            result = "null";
        } else {
            StringBuffer buffer = new StringBuffer(some.length() + ESCAPE_SLACK_COUNT);

            buffer.append('\"');
            for (int i = 0; i < some.length(); i += 1) {
                char c = some.charAt(i);
                Character cAsCharacter = Character.valueOf(c);
                String escaped = null;

                if (isEscapable(cAsCharacter)) {
                    escaped = escapeMap.get(cAsCharacter);
                } else if (c < ' ') {
                    escaped = hexString(c, UNICODE_HEX_DIGIT_COUNT, "\\u");
                }
                if (escaped == null) {
                    buffer.append(c);
                } else {
                    buffer.append(escaped);
                }
            }
            buffer.append('\"');
            result = buffer.toString();
        }
        return result;
    }

    /**
     * Source code version of character <code>some</code> that can be pasted
     * into a Java source. The result is embedded in two quotes, escape
     * characters are rendered where possible. Invisible characters are rendered
     * as unicode escape.
     */
    public/* @ pure @ */String sourced(char some) {
        return sourced(Character.toString(some));
    }

    /**
     * Hex representation of <code>value</code>.
     * 
     * @param digits
     *                the number of digits the result should have at least; if
     *                necessary, leading "0"s are prepended
     * @param prefix
     *                the text to be used as the fist few characters of the
     *                result; "0x" if null.
     */
    // @ requires digits > 0;
    // @ requires digits <= MAX_HEX_DIGIT_COUNT;
    public/* @ pure @ */String hexString(long value, int digits, /*
                                                                     * @
                                                                     * nullable @
                                                                     */
    String prefix) {
        String result = Long.toHexString(value);
        String actualPrefix;

        if (prefix == null) {
            actualPrefix = "0x";
        } else {
            actualPrefix = prefix;
        }

        if (result.length() < digits) {
            String zeros = "000000000000000";

            // @ assert zeros.length() == (MAX_HEX_DIGIT_COUNT - 1);
            result = zeros.substring(result.length() - 1, digits - 1) + result;
        }
        result = actualPrefix + result;
        return result;
    }

    /**
     * Hex representation of <code>value</code>, prefixed with "0x".
     * 
     * @param digits
     *                the number of digits the result should have at least; if
     *                necessary, leading "0"s are prepended
     */
    // @ requires digits > 0;
    // @ ensures \result.length() >= (2 + digits);
    public/* @ pure @ */String hexString(long value, int digits) {
        return hexString(value, digits, null);
    }

    public String[] getToken(String line, int startColumn) {
        return getToken(line, startColumn, null);
    }

    public String[] getToken(String line, int startColumn, String continuingChars) {
        assert line != null;
        assert startColumn <= line.length();

        String[] result = null;
        String tokenChars = continuingChars;
        String space = null;
        String token = null;
        int column = startColumn;

        while ((column < line.length()) && (result == null)) {
            // Skip white space
            space = "";
            while ((column < line.length()) && Character.isWhitespace(line.charAt(column))) {
                space += line.charAt(column);
                column += 1;
            }

            if (tokenChars == null) {
                tokenChars = DEFAULT_TOKEN_CHARS;
            }

            if (column < line.length()) {
                char firstChar = line.charAt(column);

                token = "" + firstChar;
                if (tokenChars.indexOf(firstChar) >= 0) {
                    column += 1;
                    while ((column < line.length()) && (tokenChars.indexOf(line.charAt(column)) >= 0)) {
                        token += line.charAt(column);
                        column += 1;
                    }
                }
                result = new String[] { space, token };
            }
        }
        return result;
    }

    /**
     * Same as File.delete(), but throws an IOException if the file can not be
     * deleted.
     */
    public void delete(File file) throws IOException {
        assert file != null;
        if (!file.delete()) {
            throw new IOException("cannot delete file: " + sourced(file));
        }
    }

    /**
     * Same as <code>File.mkdirs()</code> but throws an
     * <code>IOException</code> if the directory does not yet exist and also
     * cannot be created.
     * 
     * @see File#mkdirs()
     */
    public void mkdirs(File folder) throws FileNotFoundException {
        assert folder != null;
        if (!folder.mkdirs() && !folder.exists()) {
            throw new FileNotFoundException("cannot create folder: " + sourced(folder));
        }
    }

    public String getRelativePath(File baseDir, File fileInBaseDir) {
        String basePath = baseDir.getAbsolutePath();
        String filePath = fileInBaseDir.getAbsolutePath();

        assert filePath.startsWith(basePath) : "file " + sourced(filePath) + " must start with " + sourced(baseDir);
        String result = filePath.substring(basePath.length() + 1);

        return result;
    }

    public void copyFile(File source, File target) throws IOException {
        boolean copied = false;

        mkdirs(target.getParentFile());

        InputStream in = new FileInputStream(source);
        try {
            OutputStream out = new FileOutputStream(target);

            try {
                byte[] buffer = new byte[BUFFER_SIZE];

                while (!copied) {
                    int bytesRead = in.read(buffer);

                    if (bytesRead > 0) {
                        out.write(buffer, 0, bytesRead);
                    } else {
                        copied = true;
                    }
                }
            } finally {
                out.close();
                if (!copied) {
                    delete(target);
                }
            }
        } finally {
            in.close();
        }
    }

    /**
     * Copy all data from <code>in</code> to <code>out</code>.
     */
    public void copy(InputStream in, OutputStream out) throws IOException {
        assert in != null;
        assert out != null;

        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;

        do {
            bytesRead = in.read(buffer);
            if (bytesRead > 0) {
                out.write(buffer);
            }
        } while (bytesRead > 0);
    }

    /**
     * Copy all data from <code>in</code> to <code>out</code>, and close
     * both streams.
     */
    public void copyAndClose(InputStream in, OutputStream out) throws IOException {
        assert in != null;
        assert out != null;

        try {
            copy(in, out);
        } finally {
            Exception cause = null;

            try {
                out.close();
            } catch (Exception error) {
                cause = error;
            }
            try {
                in.close();
            } catch (Exception error) {
                if (cause == null) {
                    cause = error;
                }
            }
            if (cause != null) {
                IOException error = new IOException("cannot copy stream");
                error.initCause(cause);
                throw error;
            }
        }
    }

    public String[] getRelativePaths(File baseDir, File[] filesInBaseDir) {
        // TODO #3: Use streams.
        String[] result = new String[filesInBaseDir.length];

        for (int i = 0; i < filesInBaseDir.length; i += 1) {
            result[i] = getRelativePath(baseDir, filesInBaseDir[i]);
        }
        return result;
    }

    /**
     * Get the (lower case) last suffix of name (without the "."), for example:
     * "hugo.tar.gz" yields "gz".
     */
    public String getSuffix(File file) {
        assert file != null;
        return getSuffix(file.getName());
    }

    /**
     * Get the (lower case) last suffix of name (without the "."), for example:
     * "hugo.tar.gz" yields "gz".
     */
    public String getSuffix(String name) {
        assert name != null;
        String result;
        int lastDotIndex = name.lastIndexOf('.');
        int lastSeparatorIndex = name.lastIndexOf(File.separator);

        if ((lastDotIndex < lastSeparatorIndex) || (lastDotIndex == -1)) {
            result = "";
        } else {
            result = name.substring(lastDotIndex + 1).toLowerCase();
        }
        return result;
    }

    public String getWithoutLastSuffix(String fileName) {
        assert fileName != null;
        String result;
        String suffix = getSuffix(fileName);
        int length = fileName.length();

        if (suffix.length() == 0) {
            if ((length > 0) && (fileName.charAt(length - 1) == '.')) {
                result = fileName.substring(0, length - 1);
            } else {
                result = fileName;
            }
        } else {
            result = fileName.substring(0, length - suffix.length() - 1);
        }
        return result;
    }

    private char separatorChar(String possibleSeparatedText) {
        assert possibleSeparatedText != null;
        char result;
        if (possibleSeparatedText.indexOf(';') >= 0) {
            result = ';';
        } else if (possibleSeparatedText.indexOf(',') >= 0) {
            result = ',';
        } else {
            result = 0;
        }
        return result;
    }

    public String cutOffAt(String text, char charToCutOffAt) {
        assert text != null;
        String result;
        int cutOffIndex = text.indexOf(charToCutOffAt);
        if (cutOffIndex >= 0) {
            result = text.substring(0, cutOffIndex);
        } else {
            result = text;
        }
        return result;
    }

    public String[] separated(String possibleSeparatedText) {
        String[] result;
        if (possibleSeparatedText != null) {
            char separator = separatorChar(possibleSeparatedText);
            if (separator != 0) {
                result = possibleSeparatedText.split("" + separator);
            } else {
                result = new String[] { possibleSeparatedText };
            }
            // TODO #3: Use streams.
            for (int i = 0; i < result.length; i += 1) {
                result[i] = result[i].trim();
                // TODO: Remove empty items.
            }
            // Detect if result actually is empty.
            if (result.length == 0) {
                result = null;
            } else if ((result.length == 1) && (result[0].length() == 0)) {
                result = null;
            }
        } else {
            result = null;
        }
        return result;
    }

    public boolean isMacOsX() {
        boolean result;
        String osName = System.getProperty("os.name").toLowerCase();
        result = osName.startsWith("mac os x");
        return result;
    }

    /**
     * This method picks good column sizes. If all column heads are wider than
     * the column's cells' contents, then you can just use
     * column.sizeWidthToFit().
     */
    public void initColumnWidths(JTable table) {
        // TODO #3: Check if streams are useful here.
        assert table != null;
        TableModel tableModel = table.getModel();
        TableColumn columnModel = null;
        Component componet = null;
        int headerWidth = 0;
        int cellWidth = 0;
        TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();

        for (int column = 0; column < table.getColumnCount(); column += 1) {
            columnModel = table.getColumnModel().getColumn(column);

            componet = headerRenderer.getTableCellRendererComponent(null, columnModel.getHeaderValue(), false, false,
                    0, 0);
            headerWidth = componet.getPreferredSize().width;

            int maxCellWidth = 0;

            for (int row = 0; row < table.getRowCount(); row += 1) {
                componet = table.getDefaultRenderer(tableModel.getColumnClass(column)).getTableCellRendererComponent(
                        table, table.getValueAt(row, column), false, false, 0, column);
                cellWidth = componet.getPreferredSize().width;
                if (cellWidth > maxCellWidth) {
                    maxCellWidth = cellWidth;
                }
            }

            columnModel.setPreferredWidth(Math.max(headerWidth, maxCellWidth));
        }
    }

    /**
     * The file name part of <code>url</code>. For example,
     * <code>www.hugo.com/sepp/resi.png</code> yields "resi.png".
     */
    public String getName(URI uri) {
        assert uri != null;
        String result = uri.getPath();
        int indexOfLastSlash = result.lastIndexOf('/');
        if (indexOfLastSlash >= 0) {
            result = result.substring(indexOfLastSlash + 1);
        }
        return result;
    }

    /**
     * Show error <code>message</code> in a dialog, and also log it.
     */
    public void showError(JFrame frame, String message, Throwable error) {
        assert message != null;
        assert error != null;
        log.log(Level.SEVERE, message, error);
        // FIXME: Figure out how to actually print stack trace using log.log.
        error.printStackTrace();

        String fullMessage = message;
        Throwable reason = error;
        while (reason != null) {
            fullMessage += ":\n" + reason.getMessage();
            reason = reason.getCause();
        }

        JOptionPane.showMessageDialog(frame, fullMessage, "Grotag error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Show error <code>message</code> in a dialog, and also log it.
     */
    public void showError(String message, Throwable details) {
        assert message != null;
        assert details != null;
        showError(null, message, details);
    }

    /**
     * Read image resource from "/images"; in case it fails, try the file in
     * "source/images".
     */
    public Image readImageRessource(String imageName) {
        Image result;
        URL imageResourceUrl = getClass().getResource("/images/" + imageName);
        if (imageResourceUrl == null) {
            try {
                imageResourceUrl = new File(new File("source", "images"), imageName).toURL();
            } catch (MalformedURLException error) {
                throw new IllegalStateException("cannot create URL for button image " + sourced(imageName));
            }
        }
        result = Toolkit.getDefaultToolkit().createImage(imageResourceUrl);
        if (result == null) {
            throw new IllegalStateException("cannot read image " + sourced(imageName));
        }
        return result;
    }

    public void setGrotagIcon(JFrame frame) {
        assert frame != null;
        Image logoImage = readImageRessource("grotag.png");
        frame.setIconImage(logoImage);
    }
}
