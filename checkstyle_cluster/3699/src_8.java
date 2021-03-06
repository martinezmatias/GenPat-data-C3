package com.puppycrawl.tools.checkstyle.checks.javadoc;

import com.puppycrawl.tools.checkstyle.BaseCheckTestCase;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;


public class JavadocStyleCheckTest
    extends BaseCheckTestCase
{
    public void testDefaultSettings()
        throws Exception
    {
        final DefaultConfiguration checkConfig =
            createCheckConfig(JavadocStyleCheck.class);
      final String[] expected =
         {
            "20: First sentence should end with a period.",
            "53: First sentence should end with a period.",
            "63:11: Unclosed HTML tag found: <b>This guy is missing end of bold tag",
            "66:7: Extra HTML tag found: </td>Extra tag shouldn't be here",
            "68:19: Unclosed HTML tag found: <code>dummy.",
            "74: First sentence should end with a period.",
            "75:23: Unclosed HTML tag found: <b>should fail",
            "81: First sentence should end with a period.",
            "82:31: Unclosed HTML tag found: <b>should fail",
            "88: First sentence should end with a period.",
            "89:31: Extra HTML tag found: </code>"
            };

        verify(checkConfig, getPath("InputJavadocStyleCheck.java"), expected);
    }

    public void testFirstSentence() throws Exception
    {
       final DefaultConfiguration checkConfig = createCheckConfig(JavadocStyleCheck.class);
       checkConfig.addAttribute("checkFirstSentence", "true");
       checkConfig.addAttribute("checkHtml", "false");
       final String[] expected =
          {
             "20: First sentence should end with a period.",
             "53: First sentence should end with a period.",
             "74: First sentence should end with a period.",
             "81: First sentence should end with a period.",
             "88: First sentence should end with a period.",
          };

       verify(checkConfig, getPath("InputJavadocStyleCheck.java"), expected);
   }

   public void testHtml() throws Exception
   {
      final DefaultConfiguration checkConfig = createCheckConfig(JavadocStyleCheck.class);
      checkConfig.addAttribute("checkFirstSentence", "false");
      checkConfig.addAttribute("checkHtml", "true");
      final String[] expected =
         {
            "63:11: Unclosed HTML tag found: <b>This guy is missing end of bold tag",
            "66:7: Extra HTML tag found: </td>Extra tag shouldn't be here",
            "68:19: Unclosed HTML tag found: <code>dummy.",
            "75:23: Unclosed HTML tag found: <b>should fail",
            "82:31: Unclosed HTML tag found: <b>should fail",
            "89:31: Extra HTML tag found: </code>"
         };

      verify(checkConfig, getPath("InputJavadocStyleCheck.java"), expected);
  }

  public void testScopePublic()
      throws Exception
  {
      final DefaultConfiguration checkConfig =
          createCheckConfig(JavadocStyleCheck.class);
     checkConfig.addAttribute("checkFirstSentence", "true");
     checkConfig.addAttribute("checkHtml", "true");
     checkConfig.addAttribute("scope", "public");
    final String[] expected =
       {
          "88: First sentence should end with a period.",
          "89:31: Extra HTML tag found: </code>"
       };

      verify(checkConfig, getPath("InputJavadocStyleCheck.java"), expected);
  }

  public void testScopeProtected()
      throws Exception
  {
      final DefaultConfiguration checkConfig =
          createCheckConfig(JavadocStyleCheck.class);
     checkConfig.addAttribute("checkFirstSentence", "true");
     checkConfig.addAttribute("checkHtml", "true");
     checkConfig.addAttribute("scope", "protected");
    final String[] expected =
       {
          "74: First sentence should end with a period.",
          "75:23: Unclosed HTML tag found: <b>should fail",
          "88: First sentence should end with a period.",
          "89:31: Extra HTML tag found: </code>"
       };

      verify(checkConfig, getPath("InputJavadocStyleCheck.java"), expected);
  }

  public void testScopePackage()
      throws Exception
  {
      final DefaultConfiguration checkConfig =
          createCheckConfig(JavadocStyleCheck.class);
     checkConfig.addAttribute("checkFirstSentence", "true");
     checkConfig.addAttribute("checkHtml", "true");
     checkConfig.addAttribute("scope", "package");
    final String[] expected =
       {
          "74: First sentence should end with a period.",
          "75:23: Unclosed HTML tag found: <b>should fail",
          "81: First sentence should end with a period.",
          "82:31: Unclosed HTML tag found: <b>should fail",
          "88: First sentence should end with a period.",
          "89:31: Extra HTML tag found: </code>"
       };

      verify(checkConfig, getPath("InputJavadocStyleCheck.java"), expected);
  }

}
