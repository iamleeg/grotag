package net.sf.grotag.parse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sf.grotag.common.Tools;

public class TagPool {
    private Tools tools;
    private Map<String, Tag> tagMap;
    private Map<String, Tag> linkMap;
    private String validLinkTypes;

    public TagPool() {
        tools = Tools.getInstance();

        TagOption any = new TagOption(TagOption.Type.ANY);
        TagOption color = new TagOption(TagOption.Type.COLOR);
        TagOption file = new TagOption(TagOption.Type.FILE);
        TagOption filenode = new TagOption(TagOption.Type.FILENODE);
        TagOption guide = new TagOption(TagOption.Type.GUIDE);
        TagOption node = new TagOption(TagOption.Type.NODE);
        TagOption number = new TagOption(TagOption.Type.NUMBER);
        TagOption some = new TagOption(TagOption.Type.SOME);
        TagOption text = new TagOption(TagOption.Type.TEXT);

        tagMap = new TreeMap<String, Tag>();
        linkMap = new TreeMap<String, Tag>();

        // Amigaguide version 34 (Workbench 1.3 and 2.x)
        Tag.Version v34 = Tag.Version.V34;
        addTag(new Tag("$ver:", v34, Tag.Scope.GLOBAL, true, some));
        addTag(new Tag("(c)", v34, Tag.Scope.GLOBAL, true, some));
        addTag(new Tag(Tag.Name.author, v34, Tag.Scope.GLOBAL, true, some));
        // TODO: Check if @database has option "some" instead of "any".
        addTag(new Tag(Tag.Name.database, v34, Tag.Scope.GLOBAL, true, any));
        Tag dnodeTag = new Tag(Tag.Name.dnode, v34, Tag.Scope.GLOBAL, true, any);
        dnodeTag.setObsolete(true);
        addTag(dnodeTag);
        addTag(new Tag(Tag.Name.endnode, v34, Tag.Scope.GLOBAL));
        addTag(new Tag(Tag.Name.font, v34, Tag.Scope.GLOBAL, true, new TagOption[] { text, number }));
        Tag heightTag = new Tag(Tag.Name.height, v34, Tag.Scope.GLOBAL, true, number);
        heightTag.setUnused(true);
        addTag(heightTag);
        addTag(new Tag(Tag.Name.help, v34, Tag.Scope.GLOBAL, true, node));
        addTag(new Tag(Tag.Name.index, v34, Tag.Scope.GLOBAL, true, node));
        Tag masterTag = new Tag(Tag.Name.master, v34, Tag.Scope.GLOBAL, true, text);
        masterTag.setUnused(true);
        addTag(masterTag);
        addTag(new Tag(Tag.Name.node, v34, Tag.Scope.GLOBAL, new TagOption[] { some }));
        addTag(new Tag(Tag.Name.rem, v34, Tag.Scope.GLOBAL, new TagOption[] { any }));
        addTag(new Tag(Tag.Name.remark, v34, Tag.Scope.GLOBAL, new TagOption[] { any }));
        // TODO: Mark @width as "unused".
        Tag widthTag = new Tag(Tag.Name.width, v34, Tag.Scope.GLOBAL, true, number);
        widthTag.setUnused(true);
        addTag(widthTag);

        addTag(new Tag(Tag.Name.font, v34, Tag.Scope.NODE, true, new TagOption[] { text, number }));
        addTag(new Tag(Tag.Name.help, v34, Tag.Scope.NODE, true, node));
        addTag(new Tag(Tag.Name.index, v34, Tag.Scope.NODE, true, node));
        addTag(new Tag(Tag.Name.keywords, v34, Tag.Scope.NODE, true, any));
        addTag(new Tag(Tag.Name.next, v34, Tag.Scope.NODE, true, node));
        addTag(new Tag(Tag.Name.prev, v34, Tag.Scope.NODE, true, node));
        addTag(new Tag(Tag.Name.rem, v34, Tag.Scope.NODE, new TagOption[] { any }));
        addTag(new Tag(Tag.Name.remark, v34, Tag.Scope.NODE, new TagOption[] { any }));
        addTag(new Tag(Tag.Name.title, v34, Tag.Scope.NODE, true, text));
        addTag(new Tag(Tag.Name.toc, v34, Tag.Scope.NODE, true, node));

        addTag(new Tag(Tag.Name.bg, v34, Tag.Scope.INLINE, false, color));
        addTag(new Tag(Tag.Name.fg, v34, Tag.Scope.INLINE, false, color));

        // TODO: Use optional "number" instead of "any" for @{alink}
        addTag(Tag.createLink(Tag.Name.alink, v34, new TagOption[] { filenode, any }));
        addTag(Tag.createLink(Tag.Name.close, v34));
        addTag(Tag.createLink(Tag.Name.link, v34, new TagOption[] { filenode, any }));
        addTag(Tag.createLink(Tag.Name.rx, v34, text));
        addTag(Tag.createLink(Tag.Name.rxs, v34, file));
        addTag(Tag.createLink(Tag.Name.system, v34, text));
        addTag(Tag.createLink(Tag.Name.quit, v34));

        // Amigaguide version 39 (Workbench 3.0)
        Tag.Version v39 = Tag.Version.V39;
        addTag(new Tag(Tag.Name.wordwrap, v39, Tag.Scope.GLOBAL, true));
        addTag(new Tag(Tag.Name.xref, v39, Tag.Scope.GLOBAL, true, guide));

        addTag(new Tag(Tag.Name.embed, v39, Tag.Scope.NODE, file));
        addTag(new Tag(Tag.Name.proportional, v39, Tag.Scope.NODE, true));
        addTag(new Tag(Tag.Name.wordwrap, v39, Tag.Scope.NODE, true));

        addTag(new Tag(Tag.Name.b, v39, Tag.Scope.INLINE));
        addTag(new Tag(Tag.Name.i, v39, Tag.Scope.INLINE));
        addTag(new Tag(Tag.Name.u, v39, Tag.Scope.INLINE));
        addTag(new Tag(Tag.Name.ub, v39, Tag.Scope.INLINE));
        addTag(new Tag(Tag.Name.ui, v39, Tag.Scope.INLINE));
        addTag(new Tag(Tag.Name.uu, v39, Tag.Scope.INLINE));

        addTag(Tag.createLink(Tag.Name.beep, v39));
        addTag(Tag.createLink(Tag.Name.guide, v39, guide));

        // Amigaguide version 40 (Workbench 3.1)
        Tag.Version v40 = Tag.Version.V40;
        addTag(new Tag(Tag.Name.macro, v40, Tag.Scope.GLOBAL, new TagOption[] { text, text }));
        addTag(new Tag(Tag.Name.onclose, v40, Tag.Scope.GLOBAL, true, file));
        addTag(new Tag(Tag.Name.onopen, v40, Tag.Scope.GLOBAL, true, file));
        addTag(new Tag(Tag.Name.smartwrap, v40, Tag.Scope.GLOBAL, true));
        addTag(new Tag(Tag.Name.tab, v40, Tag.Scope.GLOBAL, true, number));

        addTag(new Tag(Tag.Name.onclose, v40, Tag.Scope.NODE, true, file));
        addTag(new Tag(Tag.Name.onopen, v40, Tag.Scope.NODE, true, file));
        addTag(new Tag(Tag.Name.smartwrap, v40, Tag.Scope.NODE, true));
        addTag(new Tag(Tag.Name.tab, v40, Tag.Scope.NODE, true, number));

        addTag(new Tag(Tag.Name.amigaguide, v40, Tag.Scope.INLINE));
        addTag(new Tag(Tag.Name.apen, v40, Tag.Scope.INLINE, number));
        addTag(new Tag(Tag.Name.body, v40, Tag.Scope.INLINE));
        addTag(new Tag(Tag.Name.bpen, v40, Tag.Scope.INLINE, number));
        addTag(new Tag(Tag.Name.cleartabs, v40, Tag.Scope.INLINE));
        addTag(new Tag(Tag.Name.code, v40, Tag.Scope.INLINE));
        addTag(new Tag(Tag.Name.jcenter, v40, Tag.Scope.INLINE));
        addTag(new Tag(Tag.Name.jleft, v40, Tag.Scope.INLINE));
        addTag(new Tag(Tag.Name.jright, v40, Tag.Scope.INLINE));
        addTag(new Tag(Tag.Name.lindent, v40, Tag.Scope.INLINE, number));
        addTag(new Tag(Tag.Name.line, v40, Tag.Scope.INLINE));
        addTag(new Tag(Tag.Name.par, v40, Tag.Scope.INLINE));
        addTag(new Tag(Tag.Name.pard, v40, Tag.Scope.INLINE));
        addTag(new Tag(Tag.Name.pari, v40, Tag.Scope.INLINE, number));
        addTag(new Tag(Tag.Name.plain, v40, Tag.Scope.INLINE));
        addTag(new Tag(Tag.Name.settabs, v40, Tag.Scope.INLINE, some));
        addTag(new Tag(Tag.Name.tab, v40, Tag.Scope.INLINE));

        // Collect valid link types.
        List<String> linkTypes = new ArrayList<String>();
        for (Tag tag : linkMap.values()) {
            linkTypes.add(tag.getName());
        }
        Collections.sort(linkTypes);
        validLinkTypes = "";
        boolean atFirst = true;

        for (String linkType : linkTypes) {
            if (atFirst) {
                atFirst = false;
            } else {
                validLinkTypes += ", ";
            }
            validLinkTypes += tools.sourced(linkType);
        }
    }

    private String tagKey(String name, Tag.Scope scope) {
        return scope.toString() + ":" + name.toLowerCase();
    }

    private String tagKey(Tag tag) {
        return tagKey(tag.getName(), tag.getScope());
    }

    public void addTag(Tag tag) {
        Map<String, Tag> targetMap = getMapForScope(tag.getScope());
        targetMap.put(tagKey(tag), tag);
    }

    private Map<String, Tag> getMapForScope(Tag.Scope scope) {
        Map<String, Tag> lookupMap;

        if (scope == Tag.Scope.LINK) {
            lookupMap = linkMap;
        } else {
            lookupMap = tagMap;
        }
        return lookupMap;
    }

    public Tag getTag(String name, Tag.Scope scope) {
        Map<String, Tag> lookupMap = getMapForScope(scope);
        Tag result = lookupMap.get(tagKey(name, scope));
        if ((result == null) && (scope == Tag.Scope.NODE)) {
            result = lookupMap.get(tagKey(name, Tag.Scope.GLOBAL));
        }
        return result;
    }

    public Tag getMacro(String name) {
        Tag result = getTag(name, Tag.Scope.INLINE);
        if ((result != null) && !result.isMacro()) {
            result = null;
        }
        return result;
    }

    /**
     * Comma separated list of all link types supported (for use in error
     * messages).
     */
    public String getValidLinkTypes() {
        return validLinkTypes;
    }
}
