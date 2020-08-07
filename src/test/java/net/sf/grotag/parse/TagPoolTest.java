package net.sf.grotag.parse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class TagPoolTest {
    private TagPool tagPool;

    @Before
    public void setUp() throws Exception {
        tagPool = new TagPool();
    }

    @Test
    public void testFindTag() {
        Tag tag;

        tag = tagPool.getTag("b", Tag.Scope.INLINE);
        assertNotNull(tag);
        assertEquals("b", tag.getName());
        assertEquals(Tag.Scope.INLINE, tag.getScope());

        tag = tagPool.getTag("B", Tag.Scope.INLINE);
        assertNotNull(tag);

        tag = tagPool.getTag("xxx", Tag.Scope.INLINE);
        assertNull(tag);

        tag = tagPool.getTag("link", Tag.Scope.LINK);
        assertNotNull(tag);
    }

    @Test
    public void testGetValidLinkTypes() {
        String linkType = tagPool.getValidLinkTypes();
        assertNotNull(linkType);
        int typeCount = linkType.split(",").length;
        assertTrue("typeCount=" + typeCount, typeCount > 0);
        assertFalse(linkType.startsWith(","));
        assertTrue(linkType.indexOf(',') > 0);
    }
}
