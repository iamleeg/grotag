package net.sf.grotag.guide;

import java.io.File;
import java.util.logging.Logger;

import net.sf.grotag.common.AmigaPathList;
import net.sf.grotag.common.AmigaTools;
import net.sf.grotag.common.Tools;
import net.sf.grotag.parse.CommandItem;
import net.sf.grotag.parse.FileSource;
import net.sf.grotag.parse.Tag;

public class Link {
    /**
     * Enumerator for the different states a link can have.
     * <ul>
     * <li>BROKEN - A link to a non-existent document.
     * <li>VALID - A link to an existing Amigaguide document and node.
     * <li>VALID_GUIDE_BROKEN_NODE - A link to an existing Amigaguide document
     * but a non-existent node.
     * <li>VALID_GUIDE_UNCHECKED_NODE - A link to an existing Amigaguide
     * document but a non-existent node.
     * <li>VALID_OTHER_FILE - A link to an existing file that is not an
     * Amigaguide document.
     * <li>UNCHECKED - Link has not been checked yet (the initial state).
     * <li>UNSUPPORTED - A link of a kind that is not supported by Grotag, for
     * example "rx".
     * </ul>
     * 
     * @author Thomas Aglassinger
     */
    public enum State {
        BROKEN, VALID, VALID_GUIDE_BROKEN_NODE, VALID_GUIDE_UNCHECKED_NODE, VALID_OTHER_FILE, UNCHECKED, UNSUPPORTED
    }

    /**
     * Enumerator to represent the different types of Amigaguide links. The type
     * <code>relation</code> is an internal type to represent relations to
     * other nodes, expressed with commands like <code>@next</code>.
     * 
     * @author Thomas Aglassinger
     */
    public enum Type {
        alink, beep, close, guide, link, quit, relation, rx, rxs, system
    }

    public static final int NO_LINE = -1;

    private int line;
    private String target;
    private Type type;
    private String label;
    private State state;
    private File targetFile;
    private String targetNode;
    private CommandItem linkCommand;

    private Tools tools;

    /**
     * Create a new link from a command. There are 3 types of commands a link
     * can be derived from:
     * <ul>
     * <li>Actual links, for example <code>@{"Overview" link "overview"}</code>.
     *              <li>Relations to other nodes or documents, for example
     *              <code>@next introduction</code>.
     *       <li>Node definitions, for example
     *       <code>@node overview "Overview of features"</code>.
     *       </ul>
     */
    public Link(CommandItem newLinkCommand, AmigaPathList amigaPaths) {
        assert newLinkCommand != null;
        assert newLinkCommand.isLink() || newLinkCommand.isRelation()
                || newLinkCommand.getCommandName().equals(Tag.Name.node.toString()) : newLinkCommand.toPrettyAmigaguide();
        assert newLinkCommand.getOption(0) != null;
        assert amigaPaths != null;

        tools = Tools.getInstance();
        AmigaTools amigaTools = AmigaTools.getInstance();
        Logger log = Logger.getLogger(Link.class.getName());

        String lineText;

        linkCommand = newLinkCommand;
        label = newLinkCommand.getOriginalCommandName();
        if (linkCommand.isLink()) {
            // Remove trailing quotes.
            label = label.substring(1, label.length() - 1);
            String typeText = linkCommand.getOption(0).toLowerCase();
            try {
                if (typeText.equals(Type.relation.toString())) {
                    typeText = "_" + typeText;
                }
                type = Type.valueOf(typeText);
            } catch (IllegalArgumentException error) {
                throw new IllegalLinkTypeException(typeText, error);
            }
            target = linkCommand.getOption(1);
            lineText = linkCommand.getOption(2);
            state = State.UNCHECKED;
        } else if (linkCommand.isRelation()) {
            type = Type.relation;
            target = linkCommand.getOption(0);
            lineText = null;
            state = State.UNCHECKED;
        } else {
            label = "(internal node link)";
            type = Type.relation;
            target = linkCommand.getOption(0);
            lineText = null;
            state = State.VALID;
        }

        if (lineText != null) {
            line = Integer.parseInt(lineText);
        } else {
            line = NO_LINE;
        }
        if (type == Type.guide) {
            // FIXME: Handle non-FileSource properly by using original file.
            // (For example from macros expanding to links.)
            assert newLinkCommand.getFile() instanceof FileSource;
            targetFile = ((FileSource) newLinkCommand.getFile()).getFile();
            targetNode = null;
        } else if ((type == Type.link) || (type == Type.alink) || (type == Type.relation)) {
            // FIXME: Handle non-FileSource properly by using original file.
            // (For example from macros expanding to links.)
            assert newLinkCommand.getFile() instanceof FileSource;
            File guideFile = ((FileSource) newLinkCommand.getFile()).getFile();
            int slashIndex = target.lastIndexOf('/');
            if (slashIndex >= 0) {
                String linkAmigaPath = target.substring(0, slashIndex);
                File baseFolder = guideFile.getParentFile();
                targetFile = amigaTools.getFileFor(linkAmigaPath, baseFolder, amigaPaths);
                targetNode = target.substring(slashIndex + 1);
                log.fine("mapping link: " + tools.sourced(linkAmigaPath) + " -> " + tools.sourced(targetFile) + ", "
                        + tools.sourced(targetNode));
            } else {
                // Link to node in same file.
                targetFile = guideFile;
                targetNode = target;
            }
            targetNode = targetNode.toLowerCase();
        }
    }

    public int getLine() {
        return line;
    }

    /**
     * The target as optional Amiga file path with a slash "/" and the required
     * node name. Example: "Help:MyApplication/Manual.guide/Main".
     */
    public String getAmigaTarget() {
        return target;
    }

    /**
     * The type of the link, for example <code>link</code> or
     * <code>beep</code>, or <code>null</code> in case the command is no
     * standard Amigaguide link type.
     */
    public Type getType() {
        return type;
    }

    /**
     * The label the link will be presented with towards the user. For example,
     * <code>@{" Introduction " link "intro"}</code> would yield " Introduction "
     *     (including the blanks).
     */
    public String getLabel() {
        return label;
    }

    /**
     * Is the link of a type that can be resolved to link to some actual data in
     * another file or node?
     */
    public boolean isDataLink() {
        return (getType() == Type.alink) || (getType() == Type.guide) || (getType() == Type.link || (getType() == Type.relation));
    }

    /**
     * The actual file on the local file system which contains the Amigaguide
     * document where the target node is defined.
     */
    public File getLocalTargetFile() {
        assert isDataLink() : "type=" + getType();
        return targetFile;
    }

    /**
     * The name of the node in the target file.
     */
    public String getTargetNodeName() {
        assert isDataLink() : "type=" + getType();
        return targetNode;
    }

    /**
     * The command from which this link originates.
     */
    public CommandItem getLinkCommand() {
        return linkCommand;
    }

    public State getState() {
        return state;
    }

    public void setState(State newState) {
        state = newState;
    }

    /**
     * Set the target node. This is normally already done by the constructor.
     * However, in case the link is of type "guide", it can only be done once
     * <code>GuidePile</code> knows about all the nodes in the document set.
     */
    // Package visibility because GuidePile.validateLinks() is the only sensible
    // place to call this from.
    void setTargetNodeName(String newTargetNode) {
        assert getType() == Link.Type.guide : "type=" + getType();
        assert getTargetNodeName() == null : "existing targetNode=" + tools.sourced(getTargetNodeName());
        targetNode = newTargetNode;
    }

    @Override
    public String toString() {
        String result = "Link[command=" + getLinkCommand().toPrettyAmigaguide() + ", target="
                + tools.sourced(getAmigaTarget()) + ", state=" + getState() + ", type=" + getType();
        if (isDataLink()) {
            result += ", file=" + tools.sourced(getLocalTargetFile()) + ", node=" + tools.sourced(getTargetNodeName());
        }
        result += "]";
        return result;
    }
}
