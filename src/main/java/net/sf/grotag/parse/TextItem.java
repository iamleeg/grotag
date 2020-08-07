package net.sf.grotag.parse;

import net.sf.grotag.common.Tools;

/**
 * An item representing text.
 * 
 * @author Thomas Aglassinger
 */
public class TextItem extends AbstractTextItem {
    private Tools tools;

    /**
     * Create text item from <code>newText</code>, resolving escape sequences
     * in the text.
     */
    public TextItem(AbstractSource newSource, int newLine, int newColumn, String newText) {
        super(newSource, newLine, newColumn);

        boolean afterBackslash = false;
        String textWithResolvedEscapes;

        tools = Tools.getInstance();

        textWithResolvedEscapes = "";
        for (int i = 0; i < newText.length(); i += 1) {
            char ch = newText.charAt(i);
            if (afterBackslash) {
                // Tokenizer must ensure that there are only @'s and backslashes
                // at this point.
                assert (ch == '\\') || (ch == '@') : "ch=" + tools.sourced(ch);
                textWithResolvedEscapes += ch;
                afterBackslash = false;
            } else if (ch == '\\') {
                afterBackslash = true;
            } else {
                textWithResolvedEscapes += ch;
            }
        }
        setText(textWithResolvedEscapes);
    }

    @Override
    protected String toStringSuffix() {
        return "<text>" + tools.sourced(getText());
    }

    @Override
    public String toPrettyAmigaguide() {
        String result = "";
        for (int charIndex = 0; charIndex < getText().length(); charIndex += 1) {
            char ch = getText().charAt(charIndex);
            if (ch == '\\') {
                result += "\\\\";
            } else if (ch == '@') {
                result += "@";
            } else {
                result += ch;
            }
        }
        return result;
    }
}
