package net.sf.grotag.guide;

/**
 * Different kind of how to wrap a paragraph.
 * <ul>
 * <li>DEFAULT - Only used by nodes. Means that the wrapping specified for the whole <code>@database</code> should be used.
 * <li>NONE - Break lines when a new line is specified in the source code.
 * <li>SMART - Start a new paragraph after an empty line. Can be enabled with <code>@smartwrap</code>.
 * <li>WORD - Consider every line a single paragraph. Can be enabled with <code>@wordwrap</code>.
 * </ul>
 * @author Thomas Aglassinger
 */
public enum Wrap {
    DEFAULT, NONE, SMART, WORD
}
