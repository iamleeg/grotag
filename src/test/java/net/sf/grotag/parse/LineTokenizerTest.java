package net.sf.grotag.parse;

import static org.junit.Assert.*;

import java.io.File;

import net.sf.grotag.common.Tools;
import net.sf.grotag.parse.LineTokenizer;

import org.junit.Before;
import org.junit.Test;

public class LineTokenizerTest {
    private Tools tools;

    @Before
    public void setUp() throws Exception {
        tools = Tools.getInstance();
    }

    @Test
    public void testEmpty() {
        testTokenizer("", new String[] {});
    }

    @Test
    public void testEmptyWithTrailingWhiteSpace() {
        testTokenizer(" ", new String[] {});
    }

    @Test
    public void testSimpleCommand() {
        testTokenizer("@wordwrap", new String[] { "@", "wordwrap" });
    }

    @Test
    public void testCommandWithNumber() {
        testTokenizer("@height 42", new String[] { "@", "height", " ", "42" });
    }

    @Test
    public void testLocalCommand() {
        testTokenizer("@{b}", new String[] { "@", "{", "b", "}" });
    }

    @Test
    public void testTextWithBraces() {
        testTokenizer("{b}", new String[] { "{b}" });
    }

    @Test
    public void testDanglingCommandAtEndOfLine() {
        testTokenizer("x@", new String[] { "x\\@" });
    }

    @Test
    public void testDanglingAtSign() {
        testTokenizer("@", new String[] { "\\@" });
    }

    @Test
    public void testDanglingInlineCommandWithCloseBraceAtStartOfLine() {
        testTokenizer("@{", new String[] { "\\@{" });
    }

    @Test
    public void testDanglingInlineCommandWithCloseBrace() {
        testTokenizer("x@{", new String[] { "x\\@{" });
    }

    @Test
    public void testDanglingInlineCommandWithoutOpenBrace() {
        testTokenizer("hugo@sepp.com", new String[] { "hugo\\@sepp.com" });
    }

    @Test
    public void testTextWithBracesAroundLocalCommand() {
        testTokenizer("{@{b}}", new String[] { "{", "@", "{", "b", "}", "}" });
    }

    @Test
    public void testTextWithoutCloseBraces() {
        testTokenizer("{b", new String[] { "{b" });
    }

    @Test
    public void testTextWithLocalCommand() {
        testTokenizer("x@{b}y", new String[] { "x", "@", "{", "b", "}", "y" });
    }

    @Test
    public void testCommandWithString() {
        testTokenizer("@node \"hugo\"", new String[] { "@", "node", " ", "\"hugo\"" });
    }

    @Test
    public void testCommandWithString2() {
        testTokenizer("@NODE M \"L G\"", new String[] { "@", "NODE", " ", "M", " ", "\"L G\"" });
    }

    @Test
    public void testEscaped() {
        testTokenizer("\\\\\\@", new String[] { "\\\\\\@" });
    }

    @Test
    public void testFixedMissingQuote() {
        testTokenizer("@node \"hugo", new String[] { "@", "node", " ", "\"hugo\"" });
    }

    @Test
    public void testFixedMissingCloseBrace() {
        testTokenizer("@{b", new String[] { "@", "{", "b", "}" });
    }

    @Test
    public void testFixedMissingBackslash() {
        testTokenizer("\\x", new String[] { "\\\\x" });
    }

    @Test
    public void testFixedMissingBackslashAtEndOfWord() {
        testTokenizer("\\ x", new String[] { "\\\\", " ", "x" });
    }

    @Test
    public void testFixedMissingBackslashAtEndOfLine() {
        testTokenizer("\\", new String[] { "\\\\" });
    }

    @Test
    public void testSimple() {
        testTokenizer("hugo was here.", new String[] { "hugo", " ", "was", " ", "here." });
    }

    @Test
    public void testInvisibleChar() {
        testTokenizer("hello\u0012hugo", new String[] { "hello", "?", "hugo" });
    }

    @Test
    public void testFormFeedInMiddleOfLine() {
        testTokenizer("\u000cx", new String[] { "?", "x" });
    }

    private void testTokenizer(String text, String[] expectedTokens) {
        AbstractSource source = new StringSource(LineTokenizer.class.getName() + File.separator + "testTokenizer", text);
        LineTokenizer tokenizer = new LineTokenizer(source, 3, text);
        int expectedTokenCount = expectedTokens.length;

        for (int i = 0; i < expectedTokenCount; i += 1) {
            assertTrue("number of tokens must be " + expectedTokenCount + " but is " + i, tokenizer.hasNext());
            tokenizer.advance();
            assertEquals(expectedTokens[i], tokenizer.getToken());
            System.out.println("token: " + tools.sourced(tokenizer.getToken()));
        }
    }
}
