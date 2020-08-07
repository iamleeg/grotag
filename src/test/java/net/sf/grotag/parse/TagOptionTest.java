package net.sf.grotag.parse;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TagOptionTest {
    @Test
    public void testValidationError() {
        TagOption option;

        option = new TagOption(TagOption.Type.ANY);
        assertNull(option.validationError(null));
        assertNull(option.validationError("hugo"));

        option = new TagOption(TagOption.Type.COLOR);
        assertNull(option.validationError(TagOption.Color.FILL.toString()));
        assertNull(option.validationError(TagOption.Color.FILL.toString().toLowerCase()));
        assertNotNull(option.validationError("hugo"));
        assertNotNull(option.validationError(null));

        option = new TagOption(TagOption.Type.NUMBER);
        assertNull(option.validationError("1234"));
        assertNotNull(option.validationError("hugo"));
        assertNotNull(option.validationError(null));
    }
}
