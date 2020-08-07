package net.sf.grotag.guide;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.grotag.common.Tools;

/**
 * Information about an Amigaguide <code>@database</code>.
 * 
 * @author Thomas Aglassinger
 */
public class DatabaseInfo extends AbstractInfo {
    private static final String GUIDE_SUFFIX = ".guide";
    private static final String[] POSSIBLE_COPYRIGHT_YEAR_PATTERNS = new String[] { "\\d+\\s*-\\s*\\d+\\s", "\\d+\\s" };

    private String author;
    private String copyright;
    private String copyrightHolder;
    private String copyrightYear;
    private Tools tools;
    private String version;

    protected DatabaseInfo(String newName) {
        super(newName);

        tools = Tools.getInstance();

        if (getName().toLowerCase().endsWith(GUIDE_SUFFIX)) {
            setName(getName().substring(0, getName().length() - GUIDE_SUFFIX.length()));
        }
        setWrap(Wrap.NONE);
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String newAuthor) {
        author = newAuthor;
    }

    public String getCopyright() {
        return copyright;
    }

    /**
     * Set the copyright and attempt to extract holder and year from it.
     * 
     * @see #getCopyright()
     * @see #getCopyrightHolder()
     * @see #getCopyrightYear()
     */
    public void setCopyright(String newCopyright) {
        copyright = newCopyright;
        copyrightHolder = null;
        copyrightYear = null;

        if (copyright != null) {
            int patternIndex = 0;

            copyright = copyright.trim();
            while ((patternIndex < POSSIBLE_COPYRIGHT_YEAR_PATTERNS.length) && (copyrightYear == null)) {
                Pattern yearPattern = Pattern.compile(POSSIBLE_COPYRIGHT_YEAR_PATTERNS[patternIndex]);
                Matcher yearMatcher = yearPattern.matcher(copyright);
                if (yearMatcher.find()) {
                    int yearEndIndex = yearMatcher.end();

                    copyrightYear = copyright.substring(0, yearEndIndex);
                    copyrightHolder = copyright.substring(yearEndIndex - 1);
                    // If any of these assertions fail, the pattern needs to be
                    // fixed.
                    assert copyrightYear.length() > 0;
                    assert Character.isDigit(copyrightYear.charAt(0));
                    assert Character.isWhitespace(copyrightYear.charAt(copyrightYear.length() - 1));
                    copyrightYear = copyrightYear.trim();
                    copyrightYear = tools.withoutWhiteSpace(copyrightYear);
                    copyrightHolder = copyrightHolder.trim();
                } else {
                    patternIndex += 1;
                }
            }
        }
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String newVersion) {
        version = newVersion;
    }

    @Override
    public String toString() {
        String result = "DatabaseInfo: " + getName();
        return result;
    }

    /**
     * The copyright holder.
     */
    public String getCopyrightHolder() {
        return copyrightHolder;
    }

    /**
     * The year (or year intervall) of the copyright of the form "YYYY" or
     * "YYYY-YYYY".
     */
    public String getCopyrightYear() {
        return copyrightYear;
    }
}
