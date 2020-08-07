package net.sf.grotag.guide;

/**
 * Information about structural items in an Amigaguide document.
 * 
 * @author Thomas Aglassinger
 */
abstract class AbstractInfo {
    private String name;
    private String fontName;
    private int fontSize;
    private String tableOfContentsLink;
    private String helpLink;
    private String indexLink;
    private Wrap wrap;

    protected AbstractInfo(String newName) {
        assert newName != null;

        name = newName;
        wrap = Wrap.DEFAULT;
    }

    public String getFontName() {
        return fontName;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFont(String newFontName, int newFontSize) {
        assert newFontName != null;
        assert newFontName.length() > 0;
        assert newFontName.toLowerCase().endsWith(".font");
        assert newFontSize > 0;

        fontName = newFontName;
        fontSize = newFontSize;
    }

    public String getTableOfContentsLink() {
        return tableOfContentsLink;
    }

    public void setTableOfContentsLink(String newTableOfContentsLink) {
        tableOfContentsLink = newTableOfContentsLink;
    }

    public String getHelpLink() {
        return helpLink;
    }

    public void setHelpLink(String newHelpLink) {
        helpLink = newHelpLink;
    }

    public String getIndexLink() {
        return indexLink;
    }

    public void setIndexLink(String newIndexLink) {
        indexLink = newIndexLink;
    }

    public Wrap getWrap() {
        return wrap;
    }

    public void setWrap(Wrap newWrap) {
        wrap = newWrap;
    }

    public String getName() {
        return name;
    }

    protected void setName(String newName) {
        name = newName;
    }
}
