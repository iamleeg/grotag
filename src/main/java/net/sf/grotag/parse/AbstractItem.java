package net.sf.grotag.parse;

/**
 * Abstract item in an Amigaguide token stream.
 * 
 * @author Thomas Aglassinger
 */
public abstract class AbstractItem {
    private AbstractSource source;
    private int line;
    private int column;

    protected AbstractItem(AbstractSource newSource, int newLine, int newColumn) {
        assert newSource != null;
        assert newLine >= 0;
        assert newColumn >= 0;

        source = newSource;
        line = newLine;
        column = newColumn;
    }

    public AbstractSource getFile() {
        return source;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public String toString() {
        String result = source.getShortName();

        result += " [" + getLine() + ":" + getColumn() + "]";
        result += toStringSuffix();
        return result;
    }

    /**
     * Pretty printed Amigaguide source code representation of the item.
     */
    abstract public String toPrettyAmigaguide();

    abstract protected String toStringSuffix();
}
