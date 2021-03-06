package com.puppycrawl.tools.checkstyle.checks;

import com.puppycrawl.tools.checkstyle.BaseCheckTestCase;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;

public class GenericIllegalRegexpCheckTest
    extends BaseCheckTestCase
{
    private DefaultConfiguration mCheckConfig;

    public void setUp()
    {
        mCheckConfig = createCheckConfig(GenericIllegalRegexpCheck.class);
    }

    public void testIt()
            throws Exception
    {
        final String illegal = "System\\.(out)|(err)\\.print(ln)?\\(";
        mCheckConfig.addAttribute("format", illegal);
        final String[] expected = {
            "69: Line matches the illegal pattern '" + illegal + "'."
        };
        verify(mCheckConfig, getPath("InputSemantic.java"), expected);
    }

    public void testMessageProperty()
        throws Exception
    {
        final String illegal = "System\\.(out)|(err)\\.print(ln)?\\(";
        final String message = "Bad line :(";
        mCheckConfig.addAttribute("format", illegal);
        mCheckConfig.addAttribute("message", message);
        final String[] expected = {
            "69: " + message,
        };
        verify(mCheckConfig, getPath("InputSemantic.java"), expected);
    }

    public void testIgnoreCaseTrue()
            throws Exception
    {
        final String illegal = "SYSTEM\\.(OUT)|(ERR)\\.PRINT(LN)?\\(";
        mCheckConfig.addAttribute("format", illegal);
        mCheckConfig.addAttribute("ignoreCase", "true");
        final String[] expected = {
            "69: Line matches the illegal pattern '" + illegal + "'."
        };
        verify(mCheckConfig, getPath("InputSemantic.java"), expected);
    }

    public void testIgnoreCaseFalse()
            throws Exception
    {
        final String illegal = "SYSTEM\\.(OUT)|(ERR)\\.PRINT(LN)?\\(";
        mCheckConfig.addAttribute("format", illegal);
        mCheckConfig.addAttribute("ignoreCase", "false");
        final String[] expected = {};
        verify(mCheckConfig, getPath("InputSemantic.java"), expected);
    }

    public void testIgnoreCommentsCppStyle()
            throws Exception
    {
        // See if the comment is removed properly
        final String illegal = "don't use trailing comments";
        mCheckConfig.addAttribute("format", illegal);
        mCheckConfig.addAttribute("ignoreComments", "true");
        final String[] expected = {
        };
        verify(mCheckConfig, getPath("InputTrailingComment.java"), expected);
    }

    public void testIgnoreCommentsFalseCppStyle()
            throws Exception
    {
        // See if the comment is removed properly
        final String illegal = "don't use trailing comments";
        mCheckConfig.addAttribute("format", illegal);
        mCheckConfig.addAttribute("ignoreComments", "false");
        final String[] expected = {
            "2: Line matches the illegal pattern '" + illegal + "'."
        };
        verify(mCheckConfig, getPath("InputTrailingComment.java"), expected);
    }

    public void testIgnoreCommentsCStyle()
            throws Exception
    {
        // See if the comment is removed properly
        final String illegal = "c-style 1";
        mCheckConfig.addAttribute("format", illegal);
        mCheckConfig.addAttribute("ignoreComments", "true");
        final String[] expected = {
        };
        verify(mCheckConfig, getPath("InputTrailingComment.java"), expected);
    }

    public void testIgnoreCommentsFalseCStyle()
            throws Exception
    {
        final String illegal = "c-style 1";
        mCheckConfig.addAttribute("format", illegal);
        mCheckConfig.addAttribute("ignoreComments", "false");
        final String[] expected = {
            "17: Line matches the illegal pattern '" + illegal + "'."
        };
        verify(mCheckConfig, getPath("InputTrailingComment.java"), expected);
    }

    public void testIgnoreCommentsMultipleCStyle()
            throws Exception
    {
        // See if a second comment on the same line is removed properly
        final String illegal = "c-style 2";
        mCheckConfig.addAttribute("format", illegal);
        mCheckConfig.addAttribute("ignoreComments", "true");
        final String[] expected = {
        };
        verify(mCheckConfig, getPath("InputTrailingComment.java"), expected);
    }

    public void testIgnoreCommentsMultiLine()
            throws Exception
    {
        final String illegal = "Let's check multi-line comments";
        mCheckConfig.addAttribute("format", illegal);
        mCheckConfig.addAttribute("ignoreComments", "true");
        final String[] expected = {
        };
        verify(mCheckConfig, getPath("InputTrailingComment.java"), expected);
    }

    public void testIgnoreCommentsInlineStart()
            throws Exception
    {
        final String illegal = "long ms /";
        mCheckConfig.addAttribute("format", illegal);
        mCheckConfig.addAttribute("ignoreComments", "true");
        final String[] expected = {
        };
        verify(mCheckConfig, getPath("InputTrailingComment.java"), expected);
    }

    public void testIgnoreCommentsInlineEnd()
            throws Exception
    {
        final String illegal = "int z";
        mCheckConfig.addAttribute("format", illegal);
        mCheckConfig.addAttribute("ignoreComments", "true");
        final String[] expected = {
            "20: Line matches the illegal pattern '" + illegal + "'."
        };
        verify(mCheckConfig, getPath("InputTrailingComment.java"), expected);
    }

    public void testIgnoreCommentsInlineMiddle() throws Exception
    {
        final String illegal = "int y";
        mCheckConfig.addAttribute("format", illegal);
        mCheckConfig.addAttribute("ignoreComments", "true");
        final String[] expected = {
            "21: Line matches the illegal pattern '" + illegal + "'."
        };
        verify(mCheckConfig, getPath("InputTrailingComment.java"), expected);
    }

    public void testIgnoreCommentsNoSpaces()
            throws Exception
    {
        // make sure the comment is not turned into spaces
        final String illegal = "long ms  ";
        mCheckConfig.addAttribute("format", illegal);
        mCheckConfig.addAttribute("ignoreComments", "true");
        final String[] expected = {
        };
        verify(mCheckConfig, getPath("InputTrailingComment.java"), expected);
    }

    public void test1371588()
            throws Exception
    {
        // StackOverflowError with trailing space and ignoreComments
        final String illegal = "\\s+$";
        mCheckConfig.addAttribute("format", illegal);
        mCheckConfig.addAttribute("ignoreComments", "true");
        final String[] expected = {
        };
        verify(mCheckConfig, getPath("InputTrailingComment.java"), expected);
    }
}
