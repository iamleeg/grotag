package net.sf.grotag.guide;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import net.sf.grotag.common.Tools;
import net.sf.grotag.parse.CommandItem;

/**
 * Information about an <code>@node</code>.
 * 
 * @author Thomas Aglassinger
 */
public class NodeInfo extends AbstractInfo {
    private static final String[] MONOSPACED_FONTS = new String[] { "topaz", "xen" };
    private DatabaseInfo databaseInfo;
    private CommandItem startNode;
    private CommandItem endNode;
    private String title;
    private boolean isProportional;
    private Map<Relation, Link> relationLinkMap;

    public NodeInfo(DatabaseInfo newDatabaseInfo, String newName, String newTitle) {
        super(newName.toLowerCase());
        assert newDatabaseInfo != null;

        databaseInfo = newDatabaseInfo;
        if (newTitle != null) {
            title = newTitle;
        } else {
            title = newName;
        }
        relationLinkMap = new TreeMap<Relation, Link>();
    }

    public String getTitle() {
        return title;
    }

    public CommandItem getStartNode() {
        return startNode;
    }

    public void setStartAndEndNode(CommandItem newStartNode, CommandItem newEndNode) {
        assert newStartNode != null;
        assert newStartNode.getCommandName().equals("node");
        assert newEndNode != null : "@endnode must exist for: " + newStartNode;
        assert newEndNode.getCommandName().equals("endnode") : "name of @node and @endnode must match: @node="
                + newStartNode + ", @endnode=" + newEndNode;
        startNode = newStartNode;
        endNode = newEndNode;
    }

    public CommandItem getEndNode() {
        return endNode;
    }

    @Override
    public String getFontName() {
        String result = super.getFontName();
        if (result == null) {
            result = databaseInfo.getFontName();
        }
        return result;
    }

    @Override
    public Wrap getWrap() {
        Wrap result = super.getWrap();
        if (result == Wrap.DEFAULT) {
            result = databaseInfo.getWrap();
        }
        return result;
    }

    @Override
    public int getFontSize() {
        int result = super.getFontSize();
        if (result == 0) {
            result = databaseInfo.getFontSize();
        }
        return result;
    }

    @Override
    public String toString() {
        String result = "NodeInfo " + getName() + ": start=" + getStartNode() + ", end=" + getEndNode();
        return result;
    }

    public boolean isProportional() {
        boolean result = isProportional;
        if (!result) {
            String fontName = getFontName();
            if (fontName != null) {
                Tools tools = Tools.getInstance();
                fontName = tools.getWithoutLastSuffix(fontName).toLowerCase();
                if (Arrays.binarySearch(MONOSPACED_FONTS, fontName) < 0) {
                    result = true;
                }
            }
        }
        return result;
    }

    public void setProportional(boolean newProportional) {
        isProportional = newProportional;
    }

    public Link getRelation(Relation relation) {
        return relationLinkMap.get(relation);
    }
    
    void setEmptyRelationToDefault(Relation relation, Link defaultLink) {
        assert relation != null;

        if ((getRelation(relation) == null) && (defaultLink != null)){
            relationLinkMap.put(relation, defaultLink);
            assert getRelation(relation) != null;
        }
    }

    Map<Relation, Link> getRelationLinkMap() {
        return relationLinkMap;
    }
}
