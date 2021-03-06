package com.puppycrawl.tools.checkstyle.checks.metrics;

import com.puppycrawl.tools.checkstyle.BaseCheckTestCase;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;

import java.io.File;

public class BooleanExpressionComplexityCheckTest extends BaseCheckTestCase
{
    public void test() throws Exception {
        DefaultConfiguration checkConfig =
            createCheckConfig(BooleanExpressionComplexityCheck.class);

        String[] expected = {
                "13:9: Boolean expression complexity is 4 (max allowed is 3).",
                "32:9: Boolean expression complexity is 6 (max allowed is 3).",
        };

        verify(checkConfig, getPath("metrics" + File.separator + "BooleanExpressionComplexityCheckTestInput.java"), expected);
    }

    public void testNoBitwise() throws Exception {
        DefaultConfiguration checkConfig =
            createCheckConfig(BooleanExpressionComplexityCheck.class);
        checkConfig.addAttribute("max", "5");
        checkConfig.addAttribute("tokens", "BXOR,LAND,LOR");

        String[] expected = {
        };

        verify(checkConfig, getPath("metrics" + File.separator + "BooleanExpressionComplexityCheckTestInput.java"), expected);
    }
}
