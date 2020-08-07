package net.sf.grotag.parse;

import net.sf.grotag.common.Tools;

/**
 * Definition of an option that can be passed to a Tag.
 * 
 * @see Tag
 * @see Type
 * @author Thomas Aglassinger
 */
public class TagOption {
    /**
     * Possible types for tag options:
     * <ul>
     * <li>ANY - any number of options, including none
     * <li>COLOR - one of the following values: "back", "background", "fill",
     * "filltext", "highlight", "shadow", "shine" and "text"
     * <li>FILE - an existing file
     * <li>FILENODE - an existing amigaguide file + "/" + an existing node
     * <li>GUIDE - an existing amigaguide file
     * <li>NODE - an existing node within the current file
     * <li>NUMBER - an integer number
     * <li>SOME - any number of options but at least 1
     * <li>TEXT - a text </li>
     * 
     * @author Thomas Aglassinger
     * 
     */
    public enum Type {
        ANY, COLOR, FILE, FILENODE, GUIDE, NODE, NUMBER, SOME, TEXT
    }

    /**
     * Valid colors for <code>@{bg}</code> and <code>@{fg}</code>.
     */
    public enum Color {
        BACK, BACKGROUND, FILL, FILLTEXT, HIGHLIGHT, SHADOW, SHINE, TEXT
    }

    private Tools tools;
    private Type type;
    private String defaultValue;

    /**
     * Error that occurred when validating <code>textToValidate</code> against
     * the specification, or <code>null</code> if text is valid. This only
     * takes into account simple syntactical errors, more complex errors such a
     * a <code>link</code> option not pointing to an existing node are not
     * considered here.
     */
    public String validationError(String textToValidate) {
        String result = null;

        if ((getType() != Type.ANY) && (textToValidate == null)) {
            result = "option must be specified";
        } else if (getType() == Type.COLOR) {
            try {
                Color.valueOf(textToValidate.toUpperCase());
            } catch (IllegalArgumentException error) {
                result = "color is " + tools.sourced(textToValidate);
                Boolean isFirst = true;
                for (Color color : Color.values()) {
                    if (!isFirst) {
                        result += ", ";
                    } else {
                        isFirst = false;
                    }
                    result += color.toString().toLowerCase();
                }
            }
        } else if (getType() == Type.NUMBER) {
            try {
                Integer.parseInt(textToValidate);
            } catch (NumberFormatException error) {
                result = tools.sourced(textToValidate) + " must be a number";
            }
        }
        return result;
    }

    public TagOption(Type newType, String newDefaultValue) {
        tools = Tools.getInstance();
        type = newType;
        defaultValue = newDefaultValue;
    }

    public TagOption(Type newType) {
        this(newType, null);
    }

    public Type getType() {
        return type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
