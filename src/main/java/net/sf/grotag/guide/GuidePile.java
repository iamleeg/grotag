package net.sf.grotag.guide;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.grotag.common.AmigaPathList;
import net.sf.grotag.common.Tools;
import net.sf.grotag.parse.CommandItem;
import net.sf.grotag.parse.FileSource;
import net.sf.grotag.parse.MessageItem;
import net.sf.grotag.parse.MessagePool;

/**
 * A "pile" of related Amigaguide documents connected via links.
 * 
 * @author Thomas Aglassinger
 */
public class GuidePile {
    private Map<String, Guide> guideMap;
    private List<Guide> guideList;
    private List<Link> linksToValidate;
    private Map<CommandItem, Link> linkMap;
    private Logger log;
    private MessagePool messagePool;
    private Tools tools;
    private AmigaPathList amigaPaths;

    private GuidePile(AmigaPathList newAmigaPaths) {
        assert newAmigaPaths != null;

        log = Logger.getLogger(GuidePile.class.getName());
        tools = Tools.getInstance();
        messagePool = MessagePool.getInstance();

        amigaPaths = newAmigaPaths;
        guideMap = new TreeMap<String, Guide>();
        guideList = new ArrayList<Guide>();
        linksToValidate = new ArrayList<Link>();
        linkMap = new TreeMap<CommandItem, Link>();
    }

    public List<Guide> getGuides() {
        return guideList;
    }

    public File getFirstHtmlFile(File htmlBaseFolder) {
        assert htmlBaseFolder != null;
        assert getGuides() != null;
        assert getGuides().size() > 0;
        Guide firstGuide = getGuides().get(0);
        String firstHtmlName = tools.getWithoutLastSuffix(firstGuide.getSourceFile().getName());
        File result = new File(new File(htmlBaseFolder, firstHtmlName), "index.html");
        return result;
    }

    /**
     * The guide in the pile stored in <code>guideFile</code> or
     * <code>null</code> if no such guide exists. Note that this still is
     * <code>null</code> if the file exists but is not a guide.
     */
    public Guide getGuide(File guideFile) {
        assert guideFile != null;

        Guide result;
        String guidePath = guideFile.getAbsolutePath();
        result = guideMap.get(guidePath);
        return result;
    }

    public Link getLink(CommandItem command) {
        assert command != null;
        assert command.isLink();
        return linkMap.get(command);
    }

    private Guide getCachedGuideFor(File guideFile) throws IOException {
        assert guideFile != null;
        Guide result;
        String guideKey = guideFile.getAbsolutePath();
        result = guideMap.get(guideKey);
        if (result == null) {
            result = Guide.createGuide(guideFile, amigaPaths);
            guideMap.put(guideKey, result);
            guideList.add(result);
        }
        return result;
    }

    private boolean hasCachedGuideFor(File guideFile) {
        assert guideFile != null;
        boolean result;
        String guideKey = guideFile.getAbsolutePath();
        result = guideMap.containsKey(guideKey);
        return result;
    }

    public static GuidePile createGuidePile(File guideFile, AmigaPathList newAmigaPaths) throws IOException {
        assert guideFile != null;
        assert newAmigaPaths != null;
        GuidePile result = new GuidePile(newAmigaPaths);
        result.add(guideFile);
        result.validateLinks();
        result.completeRelations();
        return result;
    }

    private void add(File guideFile) throws IOException {
        assert guideFile != null;

        // Setup links to validate from links in the initial guide.
        Guide guide = getCachedGuideFor(guideFile);
        List<Link> linksToFollow = new LinkedList<Link>(guide.getLinks());

        while (!linksToFollow.isEmpty()) {
            Link link = linksToFollow.get(0);
            linkMap.put(link.getLinkCommand(), link);
            if (link.isDataLink()) {
                File linkedFile = link.getLocalTargetFile();
                assert linkedFile != null;
                if (!hasCachedGuideFor(linkedFile)) {
                    try {
                        Guide linkedGuide = getCachedGuideFor(linkedFile);
                        scheduleLinkForValidation(link);
                        linksToFollow.addAll(linkedGuide.getLinks());
                    } catch (FileNotFoundException error) {
                        link.setState(Link.State.BROKEN);
                        MessageItem message = new MessageItem(link.getLinkCommand(),
                                "ignored link to file that does not exist: " + tools.sourced(linkedFile));
                        messagePool.add(message);
                    } catch (IOException error) {
                        link.setState(Link.State.BROKEN);
                        MessageItem message = new MessageItem(link.getLinkCommand(), "cannot read linked file for "
                                + tools.sourced(link.getAmigaTarget()));
                        MessageItem seeAlso = new MessageItem(new FileSource(linkedFile),
                                "related input/output error: " + error.getMessage());
                        message.setSeeAlso(seeAlso);
                        messagePool.add(message);
                    } catch (IllegalArgumentException errorToIgnore) {
                        link.setState(Link.State.VALID_OTHER_FILE);
                        log.log(Level.WARNING, "skipped non-guide: " + tools.sourced(linkedFile), errorToIgnore);
                    }
                } else {
                    // Link to a file already cached.
                    scheduleLinkForValidation(link);
                }
            } else {
                // Non-file link, no need to follow.
                scheduleLinkForValidation(link, Link.State.UNSUPPORTED);
            }

            linksToFollow.remove(0);
        }
    }

    private void scheduleLinkForValidation(Link linkToValidate, Link.State newState) {
        assert linkToValidate != null;
        assert newState != Link.State.UNCHECKED;
        linkToValidate.setState(newState);
        linksToValidate.add(linkToValidate);
        log.log(Level.INFO, "scheduled link for validation: {0}", linkToValidate);
    }

    private void scheduleLinkForValidation(Link linkToValidate) {
        assert linkToValidate != null;
        scheduleLinkForValidation(linkToValidate, Link.State.VALID_GUIDE_UNCHECKED_NODE);
    }

    private void validateLinks() throws IOException {
        log.info("number of links to validate: " + linksToValidate.size());
        for (Link link : linksToValidate) {
            Link.State linkState = link.getState();
            if ((linkState == Link.State.UNCHECKED) || (linkState == Link.State.VALID_GUIDE_UNCHECKED_NODE)) {
                File linkedFile = link.getLocalTargetFile();
                String linkedNodeName = link.getTargetNodeName();
                assert hasCachedGuideFor(linkedFile);
                Guide guideContainingNode = getCachedGuideFor(linkedFile);
                assert guideContainingNode != null;

                // For guide links, look up the first node of the target.
                if (link.getType() == Link.Type.guide && (linkedNodeName == null)) {
                    List<NodeInfo> targetNodeInfos = guideContainingNode.getNodeInfos();
                    if (targetNodeInfos.size() > 0) {
                        linkedNodeName = targetNodeInfos.get(0).getName();
                        link.setTargetNodeName(linkedNodeName);
                        link.setState(Link.State.VALID);
                    }
                }

                // Validate the node.
                NodeInfo nodeInfo;
                if (linkedNodeName != null) {
                    nodeInfo = guideContainingNode.getNodeInfo(linkedNodeName);
                } else {
                    nodeInfo = null;
                }
                if (nodeInfo != null) {
                    link.setState(Link.State.VALID);
                } else {
                    link.setState(Link.State.VALID_GUIDE_BROKEN_NODE);
                    MessageItem message = new MessageItem(link.getLinkCommand(), "cannot find node "
                            + tools.sourced(linkedNodeName) + " in " + tools.sourced(linkedFile));
                    messagePool.add(message);
                }
            }
        }

        // Ensure there are no unchecked links anymore.
        // TODO #3: Check if streams are useful here.
        for (Link link : linkMap.values()) {
            assert link.getState() != Link.State.UNCHECKED : "unchecked link: "
                    + link.getLinkCommand().toPrettyAmigaguide();
        }
    }
    
    private void completeRelations() {
        for (Guide guide: getGuides()) {
            Link defaultHelpLink = guide.getRelation(Relation.help);
            Link defaultIndexLink = guide.getRelation(Relation.index);
            Link defaultTocLink = guide.getRelation(Relation.contents);
            Link defaultPreviousLink = null;
           
            int nextNodeInfoIndex = 1;
            for (NodeInfo nodeInfo : guide.getNodeInfos()) {
                // Attempt to find the default next node.
                Link defaultNextLink;
                if (nextNodeInfoIndex < guide.getNodeInfos().size()) {
                    NodeInfo nextNodeInfo = guide.getNodeInfos().get(nextNodeInfoIndex);
                    CommandItem nextNodeStartCommand = nextNodeInfo.getStartNode();
                    defaultNextLink = new Link(nextNodeStartCommand, amigaPaths);
                } else {
                    defaultNextLink = null;
                }
                
                // Possibly add default links.
                nodeInfo.setEmptyRelationToDefault(Relation.help, defaultHelpLink);
                nodeInfo.setEmptyRelationToDefault(Relation.index, defaultIndexLink);
                nodeInfo.setEmptyRelationToDefault(Relation.next, defaultNextLink);
                nodeInfo.setEmptyRelationToDefault(Relation.previous, defaultPreviousLink);
                nodeInfo.setEmptyRelationToDefault(Relation.contents, defaultTocLink);
                
                // Prepare to proceed with next node.
                defaultPreviousLink = new Link(nodeInfo.getStartNode(), amigaPaths);
                nextNodeInfoIndex += 1;
            }
        }
    }
}
