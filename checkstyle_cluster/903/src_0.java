package com.puppycrawl.tools.checkstyle.checks.metrics;

import com.puppycrawl.tools.checkstyle.BaseCheckTestCase;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;

public class CyclomaticComplexityCheckTest 
    extends BaseCheckTestCase 
{
    public void test() throws Exception 
    {
        final DefaultConfiguration checkConfig =
            createCheckConfig(CyclomaticComplexityCheck.class);

        checkConfig.addAttribute("max", "0");

        final String[] expected = {
            "4:5: Cyclomatic Complexity is 2 (max allowed is 0).",
            "7:17: Cyclomatic Complexity is 2 (max allowed is 0).",
            "17:5: Cyclomatic Complexity is 6 (max allowed is 0).",
            "27:5: Cyclomatic Complexity is 4 (max allowed is 0).",
            "34:5: Cyclomatic Complexity is 6 (max allowed is 0).",
            "48:5: Cyclomatic Complexity is 4 (max allowed is 0).",
            "58:5: Cyclomatic Complexity is 4 (max allowed is 0).",
            "67:5: Cyclomatic Complexity is 4 (max allowed is 0).",
        };

        verify(checkConfig, getPath("ComplexityCheckTestInput.java"), expected);
    }
}