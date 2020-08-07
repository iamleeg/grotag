package net.sf.grotag.guide;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.grotag.common.AmigaPathList;
import net.sf.grotag.common.AmigaTools;
import net.sf.grotag.common.Tools;
import net.sf.grotag.parse.AbstractItem;
import net.sf.grotag.parse.AbstractSource;
import net.sf.grotag.parse.AbstractTextItem;
import net.sf.grotag.parse.CommandItem;
import net.sf.grotag.parse.FileSource;
import net.sf.grotag.parse.ItemReader;
import net.sf.grotag.parse.MessageItem;
import net.sf.grotag.parse.MessagePool;
import net.sf.grotag.parse.NewLineItem;
import net.sf.grotag.parse.SpaceItem;
import net.sf.grotag.parse.StringSource;
import net.sf.grotag.parse.Tag;
import net.sf.grotag.parse.TagOption;
import net.sf.grotag.parse.TagPool;
import net.sf.grotag.parse.TextItem;

/**
 * An Amigaguide document.
 * 
 * @author Thomas Aglassinger
 */
public class Guide {
    private Logger log;
    private AbstractSource guideSource;
    private List<AbstractItem> items;
    private TagPool tagPool;
    private MessagePool messagePool;
    private List<CommandItem> nodeList;
    private Tools tools;
    private int uniqueNodeCounter;
    private Map<String, CommandItem> nodeMap;
    private Map<String, CommandItem> endNodeMap;
    private Map<String, CommandItem> uniqueGlobalCommandsOccurred;
    private Map<String, CommandItem> uniqueNodeCommandsOccurred;
    private boolean hasMacros;
    private DatabaseInfo databaseInfo;
    private Map<String, NodeInfo> nodeInfoMap;
    private List<Link> links;
    private Map<Relation, Link> globalRelationLinkMap;
    private AmigaPathList amigaPaths;

    private Guide(AbstractSource newGuideSource, AmigaPathList newAmigaPaths) {
        assert newGuideSource != null;
        assert newAmigaPaths != null;

        tools = Tools.getInstance();
        messagePool = MessagePool.getInstance();
        log = Logger.getLogger(Guide.class.getName());

        guideSource = newGuideSource;
        amigaPaths = newAmigaPaths;
        tagPool = new TagPool();
        nodeInfoMap = new TreeMap<String, NodeInfo>();
        globalRelationLinkMap = new TreeMap<Relation, Link>();
    }

    private void defineMacros() {
        for (AbstractItem item : items) {
            if (isLineCommand(item)) {
                CommandItem possibleMacroItem = (CommandItem) item;
                String commandName = possibleMacroItem.getCommandName();

                if (commandName.equals("macro")) {
                    Tag macro = createMacro(possibleMacroItem);

                    if (macro != null) {
                        String macroName = macro.getName();
                        Tag existingMacro = tagPool.getTag(macroName, Tag.Scope.INLINE);

                        if (existingMacro != null) {
                            if (existingMacro.isMacro()) {
                                MessageItem currentMacroMessage = new MessageItem(possibleMacroItem,
                                        "ignored duplicate definition of macro " + tools.sourced(macroName));
                                MessageItem existingMacroMessage = new MessageItem(existingMacro.getMacroTextItem(),
                                        "previous definition of macro");

                                currentMacroMessage.setSeeAlso(existingMacroMessage);
                                messagePool.add(currentMacroMessage);
                            } else {
                                messagePool.add(new MessageItem(possibleMacroItem, "replaced standard tag "
                                        + tools.sourced(existingMacro.getName()) + " with macro"));
                            }
                        } else {
                            hasMacros = true;
                            tagPool.addTag(macro);
                        }
                    }
                }
            }
        }
    }

    /**
     * Replace all items calling a macro by the resolved sequence of items.
     */
    private void resolveMacros() throws IOException {
        int itemIndex = 0;
        while (itemIndex < items.size()) {
            AbstractItem item = items.get(itemIndex);
            log.fine("process " + item);
            if (isInlineCommand(item)) {
                CommandItem tagItem = (CommandItem) item;
                Tag macro = tagPool.getMacro(tagItem.getCommandName());
                if (macro != null) {
                    // messagePool.add(new MessageItem(tagItem, "resolving
                    // macro @{" + macro.getName() + "}..."));
                    // Write resolved macro to file and parse it.
                    String resolvedMacro = resolveMacro(tagItem, macro);
                    StringSource macroSource = new StringSource(guideSource.getShortName() + "@macro-"
                            + macro.getName(), resolvedMacro);

                    ItemReader itemReader = new ItemReader(macroSource);
                    itemReader.read();
                    List<AbstractItem> macroItems = itemReader.getItems();

                    assert macroItems.size() > 0;
                    assert macroItems.get(macroItems.size() - 1) instanceof NewLineItem;
                    macroItems.remove(macroItems.size() - 1);
                    items.remove(itemIndex);
                    items.addAll(itemIndex, macroItems);
                    itemIndex -= 1;
                }
            }
            itemIndex += 1;
        }
    }

    private String resolveMacro(CommandItem caller, Tag macro) {
        // Replace macro options.
        String macroText = macro.getMacroTextItem().getText();
        String result = "";
        int i = 0;
        while (i < macroText.length()) {
            char some = macroText.charAt(i);
            if ((some == '$') && (i < (macroText.length() - 1) && Character.isDigit(macroText.charAt(i + 1)))) {
                // TODO: Check how Amigaguide handles integer overflow for macro
                // options.
                int optionIndex = 0;
                String optionText;

                while ((i < (macroText.length() - 1) && Character.isDigit(macroText.charAt(i + 1)))) {
                    i += 1;
                    optionIndex = 10 * optionIndex + (macroText.charAt(i) - '0');
                }
                int accessOptionIndex = 1 + 2 * (optionIndex - 1);

                if (accessOptionIndex < caller.getItems().size()) {
                    optionText = ((AbstractTextItem) caller.getItems().get(accessOptionIndex)).getText();
                    log.fine("  substituting $" + optionIndex + " by: " + tools.sourced(optionText));
                } else {
                    optionText = "";
                    log.fine("  substituting $" + optionIndex + " by empty text");
                }
                result += optionText;
            } else {
                result += some;
            }
            i += 1;
        }
        log.fine("resolved macro: " + result);

        return result;
    }

    private void collectLinks() {
        links = new ArrayList<Link>();

        // Collect links from relations.
        // TODO #3: Check if streams are useful here.
        collectLinksFromRelationMap(globalRelationLinkMap);
        for (NodeInfo nodeInfo : getNodeInfos()) {
            collectLinksFromRelationMap(nodeInfo.getRelationLinkMap());
        }

        // Collect links from link commands.
        for (AbstractItem item : items) {
            if (item instanceof CommandItem) {
                CommandItem command = (CommandItem) item;
                if (command.isLink()) {
                    String label = command.getOriginalCommandName();
                    label = label.substring(1, label.length() - 1);
                    String type = command.getOption(0);
                    Tag linkTag = tagPool.getTag(type, Tag.Scope.LINK);

                    if (linkTag != null) {
                        assert !linkTag.nameEquals(Tag.Name.alink) : "alink must have been replaced: " + command;
                        if (linkTag.nameEquals(Tag.Name.link) || linkTag.nameEquals(Tag.Name.guide)) {
                            String target = command.getOption(1);
                            assert target != null : "empty target: " + command;
                            if (target.length() > 0) {
                                try {
                                    Link link = new Link(command, amigaPaths);
                                    links.add(link);
                                } catch (NumberFormatException error) {
                                    String lineText = command.getOption(2);
                                    log
                                            .log(Level.INFO, "ignored broken line number: " + tools.sourced(lineText),
                                                    error);
                                    MessageItem message = new MessageItem(command.getOptionItem(2),
                                            "ignored broken line number: " + tools.sourced(lineText));
                                    messagePool.add(message);
                                    command.cutOptionsAt(2);
                                    Link link = new Link(command, amigaPaths);
                                    links.add(link);
                                }
                            } else {
                                // Empty link, for example @{"label" link ""}.
                                MessageItem message = new MessageItem(command.getOptionItem(1),
                                        "ignored empty link target");
                                messagePool.add(message);
                            }
                        }
                    } else {
                        // Unknown link type, for example: @{"label" oops}.
                        MessageItem message = new MessageItem(command.getOptionItem(0), "ignored unknown link type "
                                + tools.sourced(type) + ", valid types are: " + tagPool.getValidLinkTypes());
                        messagePool.add(message);
                    }
                }
            }
        }
    }

    private void collectLinksFromRelationMap(Map<Relation, Link> relationMap) {
        assert links != null;
        // TODO #3: Check if streams are useful here.
        for (Link link : relationMap.values()) {
            links.add(link);
        }
    }

    private void collectNodes() {
        nodeList = new ArrayList<CommandItem>();
        nodeMap = new TreeMap<String, CommandItem>();
        endNodeMap = new TreeMap<String, CommandItem>();

        String nodeName = null;
        int i = 0;

        while (i < items.size()) {
            AbstractItem item = items.get(i);
            if (isLineCommand(item)) {
                CommandItem command = (CommandItem) item;
                String commandName = command.getCommandName();

                if (commandName.equals("node")) {
                    if (nodeName != null) {
                        // Add missing @endnode.
                        CommandItem endNodeItem = new CommandItem(command.getFile(), command.getLine(), command
                                .getColumn(), "endnode", false, new ArrayList<AbstractItem>());
                        items.add(i, endNodeItem);
                        endNodeMap.put(nodeName, endNodeItem);
                        i += 1;
                        CommandItem startNodeItem = nodeList.get(nodeList.size() - 1);

                        MessageItem message = new MessageItem(command, "added missing @endnode before @node");
                        MessageItem seeAlso = new MessageItem(startNodeItem, "previous @node");
                        message.setSeeAlso(seeAlso);
                        messagePool.add(message);
                    }
                    nodeName = getNodeNameOrNull(command);
                    if (nodeName != null) {
                        CommandItem nodeWithSameName = nodeMap.get(nodeName);
                        if (nodeWithSameName != null) {
                            // Change duplicate node name to something unique.
                            AbstractTextItem uniqueNodeNameItem = getUniqueNodeNameItem(command.getItems().get(1));
                            nodeName = uniqueNodeNameItem.getText();
                            command.setOption(0, nodeName);
                            MessageItem message = new MessageItem(command, "changed duplicate node name "
                                    + tools.sourced(nodeName) + " to " + tools.sourced(nodeName));
                            MessageItem seeAlso = new MessageItem(nodeWithSameName, "existing node with same name");
                            message.setSeeAlso(seeAlso);
                            messagePool.add(message);
                        }
                    } else {
                        nodeName = getUniqueNodeName();
                        command.getItems().add(
                                new SpaceItem(command.getFile(), command.getLine(), command.getColumn(), " "));
                        command.getItems().add(
                                new TextItem(command.getFile(), command.getLine(), command.getColumn(), nodeName));
                        MessageItem message = new MessageItem(command, "assigned name " + tools.sourced(nodeName)
                                + " to unnamed node");
                        messagePool.add(message);
                    }
                    nodeList.add(command);
                    nodeMap.put(nodeName, command);
                } else if (commandName.equals("endnode")) {
                    if (nodeName == null) {
                        items.remove(i);
                        i -= 1;
                        messagePool.add(new MessageItem(command, "removed dangling @endnode"));
                    } else {
                        endNodeMap.put(nodeName, command);
                        nodeName = null;
                    }
                }

            }
            i += 1;
        }
        if (nodeName != null) {
            appendMissingEndnodeAtEndOfFile(nodeName);
        }

        for (CommandItem node : nodeList) {
            log.fine("node: " + node);
            log.fine("  endnode: " + endNodeMap.get(getNodeName(node)));
        }
    }

    /**
     * All lower case name of <code>node</code> item.
     */
    private String getNodeName(CommandItem node) {
        assert node != null;
        assert node.getCommandName().equals("node");
        String result = node.getOption(0);
        assert result != null : "node must have name: " + node;
        result = result.toLowerCase();
        return result;
    }

    /**
     * All lower case name of <code>node</code> item, or <code>null</code>
     * if no name was passed..
     */
    private String getNodeNameOrNull(CommandItem node) {
        String result;
        if (node.getOption(0) != null) {
            result = getNodeName(node);
        } else {
            result = null;
        }
        return result;
    }

    private void appendMissingEndnodeAtEndOfFile(String nodeName) {
        // Create and add the missing @endnode.
        AbstractItem lastItem = items.get(items.size() - 1);
        assert lastItem instanceof NewLineItem : "lastItem=" + lastItem.getClass().getName();
        CommandItem endNodeItem = new CommandItem(lastItem.getFile(), lastItem.getLine(), lastItem.getColumn(),
                "endnode", false, new ArrayList<AbstractItem>());
        items.add(endNodeItem);
        endNodeMap.put(nodeName, endNodeItem);

        // Now report what we just did.
        CommandItem startNodeItem = nodeMap.get(nodeName);
        MessageItem message = new MessageItem(lastItem, "added missing" + endNodeItem.toShortAmigaguide() + " at end");
        MessageItem seeAlso = new MessageItem(startNodeItem, "matching " + startNodeItem.toShortAmigaguide());
        message.setSeeAlso(seeAlso);
        messagePool.add(message);
    }

    private void assertNodeConsistency() {
        assert nodeList.size() == nodeMap.size();
        assert nodeList.size() == endNodeMap.size();
        assert nodeList.size() == nodeInfoMap.size() : "nls=" + nodeList.size() + ", nims=" + nodeInfoMap.size();

        for (CommandItem nodeItem : nodeList) {
            String nodeName = getNodeName(nodeItem);
            assert nodeName != null;
            assert nodeMap.get(nodeName) != null;
            assert endNodeMap.get(nodeName) != null;
            NodeInfo nodeInfo = nodeInfoMap.get(nodeName);
            assert nodeInfo != null;
            assert nodeInfo.getStartNode() != null;
            assert nodeInfo.getEndNode() != null;
        }
    }

    private void validateCommands() {
        uniqueGlobalCommandsOccurred = new TreeMap<String, CommandItem>();
        uniqueNodeCommandsOccurred = new TreeMap<String, CommandItem>();
        NodeInfo currentNodeInfo = null;
        int itemIndex = 0;

        while (itemIndex < items.size()) {
            AbstractItem item = items.get(itemIndex);
            if (item instanceof CommandItem) {
                CommandItem command = (CommandItem) item;
                log.log(Level.FINE, "validate {0}", item.toPrettyAmigaguide());
                if (command.getCommandName().equals("node")) {
                    assert currentNodeInfo == null;
                    String nodeName = getNodeName(command);
                    String nodeTitle = command.getOption(1);
                    CommandItem matchingEndNode = endNodeMap.get(nodeName);

                    currentNodeInfo = new NodeInfo(getDatabaseInfo(), nodeName, nodeTitle);
                    currentNodeInfo.setStartAndEndNode(command, matchingEndNode);
                    assert !nodeInfoMap.containsKey(nodeName);
                    nodeInfoMap.put(nodeName, currentNodeInfo);

                } else if (command.getCommandName().equals("endnode")) {
                    assert currentNodeInfo != null;
                    currentNodeInfo = null;
                    uniqueNodeCommandsOccurred.clear();
                }

                int oldItemCount = items.size();
                Tag.Scope scope = getScopeFor(command, (currentNodeInfo != null));
                if (scope == Tag.Scope.LINK) {
                    validateLink(itemIndex, command);
                } else {
                    boolean removeCommand = false;
                    Tag tag = tagPool.getTag(command.getCommandName(), scope);
                    if (tag != null) {
                        TagOption[] tagOptions = tag.getOptions();
                        boolean lastOptionIsAnyOrSome = false;
                        int optionIndex = 0;

                        removeCommand = !isValidPossiblyUniqueCommand(command, tag);

                        if (!removeCommand) {
                            validateUnusedAndObsoleteCommand(command, tag);
                        }
                        while (!removeCommand && (tagOptions != null) && (optionIndex < tagOptions.length)) {
                            TagOption tagOption = tagOptions[optionIndex];
                            String optionValue = command.getOption(optionIndex);
                            String validationError = tagOption.validationError(optionValue);
                            if (validationError != null) {
                                AbstractItem baseItem = command.getOptionItem(optionIndex);

                                if (baseItem == null) {
                                    baseItem = command;
                                }
                                MessageItem message = new MessageItem(baseItem, "removed "
                                        + command.toShortAmigaguide() + " because option #" + (optionIndex + 1)
                                        + " is broken: " + validationError);
                                messagePool.add(message);
                                removeCommand = true;
                            } else {
                                assert !lastOptionIsAnyOrSome : "option of type \"any\" or \"some\" must be the last: "
                                        + tag;
                                lastOptionIsAnyOrSome = (tagOption.getType() == TagOption.Type.ANY)
                                        || (tagOption.getType() == TagOption.Type.SOME);
                            }
                            optionIndex += 1;
                        }

                        if (!removeCommand && !lastOptionIsAnyOrSome) {
                            AbstractTextItem optionItem = command.getOptionItem(optionIndex);
                            if (optionItem != null) {
                                MessageItem message = new MessageItem(optionItem, "ignored unexpected option #"
                                        + (optionIndex + 1) + " (and possible further options) for "
                                        + command.toShortAmigaguide() + ": " + tools.sourced(optionItem.getText()));
                                messagePool.add(message);
                            }
                        }

                        // Validate and process special commands.
                        if (!removeCommand) {
                            String commandName = command.getCommandName();
                            AbstractInfo scopedInfo;

                            if (currentNodeInfo == null) {
                                scopedInfo = getDatabaseInfo();
                            } else {
                                scopedInfo = currentNodeInfo;
                            }

                            if (commandName.equals("author")) {
                                databaseInfo.setAuthor(command.getAllOptionsText());
                            } else if (commandName.equals("font")) {
                                removeCommand = !isValidFont(command);
                                if (!removeCommand) {
                                    String fontName = command.getOption(0);
                                    int fontSize = Integer.parseInt(command.getOption(1));
                                    scopedInfo.setFont(fontName, fontSize);
                                }
                            } else if (commandName.equals(Tag.Name.proportional.toString())
                                    && (currentNodeInfo != null)) {
                                currentNodeInfo.setProportional(true);
                            } else if (commandName.equals("smartwrap")) {
                                scopedInfo.setWrap(Wrap.SMART);
                            } else if (commandName.equals("wordwrap")) {
                                scopedInfo.setWrap(Wrap.WORD);
                            } else if (commandName.equals("$ver:")) {
                                databaseInfo.setVersion(command.getAllOptionsText());
                            } else if (commandName.equals("(c)")) {
                                databaseInfo.setCopyright(command.getAllOptionsText());
                            } else if (command.isRelation()) {
                                assert command.getOption(0) != null : "tag must be defined to require 1 option: "
                                        + commandName;
                                Relation relation = Relation.valueOf(commandName);
                                Link link = new Link(command, amigaPaths);
                                if (currentNodeInfo == null) {
                                    assert !globalRelationLinkMap.containsKey(relation) : "tag must be defined to be unique: "
                                            + commandName;
                                    globalRelationLinkMap.put(relation, link);
                                } else {
                                    assert currentNodeInfo.getRelation(relation) == null : "tag must be defined to be unique: "
                                            + commandName;
                                    currentNodeInfo.setEmptyRelationToDefault(relation, link);
                                }
                            }
                        }
                    } else {
                        MessageItem message = new MessageItem(command, "removed unknown command "
                                + command.toShortAmigaguide());
                        messagePool.add(message);
                        removeCommand = true;
                    }

                    // TODO: Move global commands inside node below @database.
                }
                // Adjust itemIndex to whatever number validateXXX() has
                // added or removed.
                itemIndex += items.size() - oldItemCount;
            }
            itemIndex += 1;
        }

        // At this point, a possible missing @endnode should have been fixed
        // already.
        assert currentNodeInfo == null;

        assertNodeConsistency();

        // No more need for those, but GC wouldn't know.
        uniqueGlobalCommandsOccurred = null;
        uniqueNodeCommandsOccurred = null;
    }

    private boolean isValidFont(CommandItem command) {
        boolean result = false;
        String fontSizeText = command.getOption(1);
        if (fontSizeText != null) {
            try {
                int fontSize = Integer.parseInt(fontSizeText);
                result = (fontSize > 0);
            } catch (NumberFormatException error) {
                log.fine("detected invalid font size: " + fontSizeText);
            }
        }
        // Report broken font size.
        if (!result) {
            AbstractItem location = command.getOptionItem(1);
            if (location == null) {
                location = command;
            }
            MessageItem message = new MessageItem(location, "removed " + command.toShortAmigaguide()
                    + " because font size must be a number greater than 0");
            messagePool.add(message);
        }
        return result;
    }

    /**
     * Is <code>command</code> a non-unique command or a unique command that
     * has not occurred so far within the scope defined by <code>tag</code>?
     */
    private boolean isValidPossiblyUniqueCommand(CommandItem command, Tag tag) {
        assert command != null;
        assert tag != null;
        assert command.getCommandName().equals(tag.getName());

        boolean result = true;
        if (tag.isUnique()) {
            Tag.Scope scope = tag.getScope();
            Map<String, CommandItem> uniqueCommandsOccurred;

            if (scope == Tag.Scope.GLOBAL) {
                uniqueCommandsOccurred = uniqueGlobalCommandsOccurred;
            } else {
                assert scope == Tag.Scope.NODE : "scope=" + scope;
                uniqueCommandsOccurred = uniqueNodeCommandsOccurred;
            }

            CommandItem existingUniqueCommand = uniqueCommandsOccurred.get(command.getCommandName());
            if (existingUniqueCommand != null) {
                String messageText = "removed duplicate " + command.toShortAmigaguide()
                        + " because it must be unique within ";
                if (scope == Tag.Scope.GLOBAL) {
                    messageText += "document";
                } else {
                    assert scope == Tag.Scope.NODE;
                    messageText += "node";
                }

                MessageItem message = new MessageItem(command, messageText);
                MessageItem seeAlso = new MessageItem(existingUniqueCommand, "previous occurrence");
                message.setSeeAlso(seeAlso);
                messagePool.add(message);
                result = false;
            } else {
                uniqueCommandsOccurred.put(command.getCommandName(), command);
            }
        }
        return result;
    }

    private void validateUnusedAndObsoleteCommand(CommandItem command, Tag tag) {
        assert command != null;
        assert tag != null;
        assert command.getCommandName().equals(tag.getName());

        String reasonToIgnore = null;

        if (tag.isObsolete()) {
            reasonToIgnore = "obsolete";
        } else if (tag.isUnused()) {
            reasonToIgnore = "unused";
        }

        if (reasonToIgnore != null) {
            MessageItem message = new MessageItem(command, "ignored " + reasonToIgnore + " command "
                    + command.toShortAmigaguide());
            messagePool.add(message);
        }
    }

    private void validateLink(int itemIndex, CommandItem command) {
        String linkType = command.getOption(0);
        String reasonToReplaceLinkByText = null;
        MessageItem seeAlso = null;

        if (linkType != null) {
            Tag linkTag = tagPool.getTag(linkType, Tag.Scope.LINK);
            if (linkTag == null) {
                reasonToReplaceLinkByText = "unknown link";
            } else {
                AbstractItem lastExistingItem = command.getOptionItem(0);
                int optionIndex = 0;
                while ((reasonToReplaceLinkByText == null) && (optionIndex < linkTag.getOptions().length)) {
                    TagOption tagOption = linkTag.getOptions()[optionIndex];
                    AbstractItem linkOptionItem = command.getOptionItem(1 + optionIndex);
                    String linkOption = command.getOption(1 + optionIndex);
                    if (linkOptionItem != null) {
                        lastExistingItem = linkOptionItem;
                    }
                    String validationError = tagOption.validationError(linkOption);
                    if (validationError != null) {
                        reasonToReplaceLinkByText = "link with broken option";
                        seeAlso = new MessageItem(lastExistingItem, validationError);
                    } else {
                        optionIndex += 1;
                    }
                }

                // Change possible alink to link.
                if (linkTag.nameEquals(Tag.Name.alink)) {
                    log.fine("old alink: " + command);
                    command.setOption(0, Tag.Name.link.toString());
                    log.fine("new link: " + command);
                    MessageItem message = new MessageItem(command.getOptionItem(0),
                            "replaced obsolete @{... alink} by @{... link}");
                    messagePool.add(message);
                }

                // Detect and remove additional link options.
                optionIndex += 1;
                if (command.getOption(optionIndex) != null) {
                    log.info("remove link options from: " + command);
                    MessageItem message = new MessageItem(command.getOptionItem(optionIndex),
                            "removed unexpected link options starting with option #" + optionIndex);
                    messagePool.add(message);
                    command.cutOptionsAt(optionIndex);
                }
            }
        } else {
            reasonToReplaceLinkByText = "empty link";
        }
        if (reasonToReplaceLinkByText != null) {
            MessageItem message = new MessageItem(command, "replaced " + reasonToReplaceLinkByText + " by its label: "
                    + command.toPrettyAmigaguide());
            if (seeAlso != null) {
                message.setSeeAlso(seeAlso);
            }
            messagePool.add(message);
            items.set(itemIndex, command.toTextItem());
        }
    }

    private Tag.Scope getScopeFor(CommandItem command, boolean insideNode) {
        assert command != null;
        Tag.Scope result;
        if (command.isInline()) {
            if (command.isLink()) {
                result = Tag.Scope.LINK;
            } else {
                result = Tag.Scope.INLINE;
            }
        } else if (insideNode) {
            result = Tag.Scope.NODE;
        } else {
            result = Tag.Scope.GLOBAL;
        }
        return result;
    }

    /**
     * Is <code>item</code> a line command, for example <code>@node</code>?
     */
    private boolean isLineCommand(AbstractItem item) {
        return (item instanceof CommandItem) && !((CommandItem) item).isInline();
    }

    /**
     * Is <code>item</code> an inline command, for example <code>@{b}</code>?
     */
    private boolean isInlineCommand(AbstractItem item) {
        return (item instanceof CommandItem) && ((CommandItem) item).isInline();
    }

    private AbstractTextItem getUniqueNodeNameItem(AbstractItem location) {
        AbstractTextItem result;
        String nodeName = getUniqueNodeName();
        result = new TextItem(location.getFile(), location.getLine(), location.getLine(), nodeName);
        return result;
    }

    public static Guide createGuide(File newGuideFile, AmigaPathList newAmigaPaths) throws IOException {
        assert newGuideFile != null;

        AmigaTools amigaTools = AmigaTools.getInstance();
        Guide result = new Guide(new FileSource(newGuideFile), newAmigaPaths);
        amigaTools.ensureIsAmigaguide(newGuideFile);
        result.readItems();
        result.defineMacros();
        result.resolveMacros();
        result.collectNodes();
        result.validateCommands();
        result.collectLinks();

        return result;
    }

    private Tag createMacro(CommandItem macro) {
        assert macro.getCommandName().equals("macro");
        Tag result = null;
        String macroName = null;
        int itemCount = macro.getItems().size();

        if (itemCount >= 2) {
            AbstractItem firstItem = macro.getItems().get(0);
            assert firstItem instanceof SpaceItem : "first macro item must be " + SpaceItem.class + " but is "
                    + firstItem.getClass();
            AbstractTextItem macroNameItem = (AbstractTextItem) macro.getItems().get(1);
            macroName = macroNameItem.getText().toLowerCase();

            AbstractTextItem macroTextItem;
            if (itemCount >= 4) {
                AbstractItem thirdItem = macro.getItems().get(2);
                assert thirdItem instanceof SpaceItem : "third macro item must be " + SpaceItem.class + " but is "
                        + firstItem.getClass();
                macroTextItem = (AbstractTextItem) macro.getItems().get(3);
            } else {
                macroTextItem = null;
            }
            result = Tag.createMacro(macroName, macroTextItem);
        }
        return result;
    }

    /**
     * Unique (all lower case) name for a node.
     */
    private String getUniqueNodeName() {
        String result = null;

        do {
            uniqueNodeCounter += 1;
            result = "unnamed." + uniqueNodeCounter;
        } while (nodeMap.containsKey(result));

        assert result.equals(result.toLowerCase());

        return result;
    }

    private void readItems() throws IOException {
        ItemReader itemReader = new ItemReader(guideSource);

        itemReader.read();
        items = itemReader.getItems();

        // Setup initial @database information.
        if (items.size() > 0) {
            AbstractItem firstItem = items.get(0);
            if (firstItem instanceof CommandItem) {
                CommandItem firstCommand = (CommandItem) firstItem;
                if (firstCommand.getCommandName().equals("database")) {
                    String databaseName = firstCommand.getOption(0);
                    if (databaseName == null) {
                        databaseName = guideSource.getShortName();
                        MessageItem message = new MessageItem(guideSource, 0, 0, "changed missing database name to "
                                + tools.sourced(databaseName));
                        messagePool.add(message);
                    }
                    databaseInfo = new DatabaseInfo(databaseName);
                } else {
                    log.info("first command is: " + firstItem);
                }
            } else {
                log.info("first item is: " + firstItem);
            }
        } else {
            log.info("guide is empty: " + guideSource);
        }

        if (getDatabaseInfo() == null) {
            items.clear();
            MessageItem message = new MessageItem(guideSource, 0, 0, "Amigaguide must start with @database.");
            messagePool.add(message);
        }
    }

    /**
     * Items the guide consists of after it has been fixed and cleaned up.
     */
    public List<AbstractItem> getItems() {
        return items;
    }

    /**
     * Information about this guide.
     */
    public DatabaseInfo getDatabaseInfo() {
        return databaseInfo;
    }

    public NodeInfo getNodeInfo(String nodeName) {
        assert nodeName != null;
        assert nodeName.equals(nodeName.toLowerCase()) : "node name must be all lower case: " + nodeName;
        return nodeInfoMap.get(nodeName);
    }

    /**
     * List of <code>NodeInfo</code>s in the same order as the nodes occurred
     * in the guide.
     */
    public List<NodeInfo> getNodeInfos() {
        List<NodeInfo> result = new ArrayList<NodeInfo>(nodeList.size());
        // TODO #3: Check if streams are useful here.
        for (CommandItem nodeCommand : nodeList) {
            String nodeName = getNodeName(nodeCommand);
            assert nodeName != null;
            NodeInfo nodeInfo = getNodeInfo(nodeName);
            assert nodeInfo != null;
            assert nodeInfo.getName().equals(nodeName);
            result.add(nodeInfo);
        }
        assert result.size() == nodeList.size();
        assert result.size() == nodeInfoMap.size();

        return result;
    }

    // TODO: Implement pretty printing with macros and remove
    // checkNoMacrosHaveBeenDefined().
    private void checkNoMacrosHaveBeenDefined() {
        if (hasMacros) {
            throw new IllegalStateException("pretty printing with defined macros must be implemented");
        }
    }

    public void writePretty(Writer writer) throws IOException {
        checkNoMacrosHaveBeenDefined();
        // TODO #3: Check if streams are useful here.
        for (AbstractItem item : getItems()) {
            writer.write(item.toPrettyAmigaguide());
        }
    }

    public void writePretty(File targetFile) throws IOException {
        checkNoMacrosHaveBeenDefined();
        FileOutputStream fileOutStream = new FileOutputStream(targetFile);
        try {
            OutputStreamWriter outStreamWriter = new OutputStreamWriter(fileOutStream, AmigaTools.ENCODING);
            try {
                BufferedWriter writer = new BufferedWriter(outStreamWriter);
                try {
                    writePretty(writer);
                } finally {
                    writer.close();
                }
            } finally {
                outStreamWriter.close();
            }
        } finally {
            fileOutStream.close();
        }
    }

    /**
     * Links and relations to nodes or other files. This does not include pseudo
     * links like "beep" or "rx".
     */
    public List<Link> getLinks() {
        return links;
    }

    /**
     * The source the guide was created from.
     */
    public AbstractSource getSource() {
        return guideSource;
    }

    /**
     * Assuming the source the guide was created from was a file, yield this
     * file.
     */
    public File getSourceFile() {
        return ((FileSource) getSource()).getFile();
    }

    public Link getRelation(Relation relation) {
        return globalRelationLinkMap.get(relation);
    }
}
