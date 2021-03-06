////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2015 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle.grammars.java8;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import com.puppycrawl.tools.checkstyle.BaseCheckTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.checks.naming.MemberNameCheck;

public class AnnotationTest extends BaseCheckTestSupport {
    @Override
    protected String getPath(String filename) throws IOException {
        return super.getPath("grammars" + File.separator
                + "java8" + File.separator + filename);
    }

    @Override
    protected String getNonCompilablePath(String filename) throws IOException {
        return super.getNonCompilablePath("grammars" + File.separator
                + "java8" + File.separator + filename);
    }

    @Test
    public void testSimpleTypeAnnotation()
        throws Exception {
        final DefaultConfiguration checkConfig =
                createCheckConfig(MemberNameCheck.class);
        final String[] expected = ArrayUtils.EMPTY_STRING_ARRAY;
        verify(checkConfig, getPath("InputAnnotationsTest1.java"), expected);

    }

    @Test
    public void testAnnotationOnClass()
        throws Exception {
        final DefaultConfiguration checkConfig =
                createCheckConfig(MemberNameCheck.class);
        final String[] expected = ArrayUtils.EMPTY_STRING_ARRAY;
        verify(checkConfig, getNonCompilablePath("InputAnnotationsTest2.java"), expected);

    }

    @Test
    public void testClassCastTypeAnnotation()
        throws Exception {
        final DefaultConfiguration checkConfig =
                createCheckConfig(MemberNameCheck.class);
        final String[] expected = ArrayUtils.EMPTY_STRING_ARRAY;
        verify(checkConfig, getNonCompilablePath("InputAnnotationsTest3.java"), expected);

    }

    @Test
    public void testMethodParametersTypeAnnotation()
        throws Exception {
        final DefaultConfiguration checkConfig =
                createCheckConfig(MemberNameCheck.class);
        final String[] expected = ArrayUtils.EMPTY_STRING_ARRAY;
        verify(checkConfig, getNonCompilablePath("InputAnnotationsTest4.java"), expected);

    }

    @Test
    public void testAnnotationInThrows()
        throws Exception {
        final DefaultConfiguration checkConfig =
                createCheckConfig(MemberNameCheck.class);
        final String[] expected = ArrayUtils.EMPTY_STRING_ARRAY;
        verify(checkConfig, getNonCompilablePath("InputAnnotationsTest5.java"), expected);

    }

    @Test
    public void testAnnotationInGeneric()
        throws Exception {
        final DefaultConfiguration checkConfig =
                createCheckConfig(MemberNameCheck.class);
        final String[] expected = ArrayUtils.EMPTY_STRING_ARRAY;
        verify(checkConfig, getNonCompilablePath("InputAnnotationsTest6.java"), expected);

    }

    @Test
    public void testAnnotationOnConstructorCall()
        throws Exception {
        final DefaultConfiguration checkConfig =
                createCheckConfig(MemberNameCheck.class);
        final String[] expected = ArrayUtils.EMPTY_STRING_ARRAY;
        verify(checkConfig, getNonCompilablePath("InputAnnotationsTest7.java"), expected);

    }

    @Test
    public void testAnnotationNestedCall()
        throws Exception {
        final DefaultConfiguration checkConfig =
                createCheckConfig(MemberNameCheck.class);
        final String[] expected = ArrayUtils.EMPTY_STRING_ARRAY;
        verify(checkConfig, getNonCompilablePath("InputAnnotationsTest8.java"), expected);

    }

    @Test
    public void testAnnotationOnWildcards()
        throws Exception {
        final DefaultConfiguration checkConfig =
                createCheckConfig(MemberNameCheck.class);
        final String[] expected = ArrayUtils.EMPTY_STRING_ARRAY;
        verify(checkConfig, getNonCompilablePath("InputAnnotationsTest9.java"), expected);

    }

    @Test
    public void testAnnotationInCatchParameters()
        throws Exception {
        final DefaultConfiguration checkConfig =
                createCheckConfig(MemberNameCheck.class);
        final String[] expected = ArrayUtils.EMPTY_STRING_ARRAY;
        verify(checkConfig, getNonCompilablePath("InputAnnotationsTest10.java"), expected);

    }

}
