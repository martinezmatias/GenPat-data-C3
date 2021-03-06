////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2003  Oliver Burn
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
package com.puppycrawl.tools.checkstyle.checks.j2ee;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/**
 * Checks that a EntityBean ejbCreate method satisfies these requirements:
 * <ul>
 * <li>The access control modifier must be <code>public</code>.</li>
 * <li>The return type must not be void.</li>
 * <li>The method modifier cannot be <code>final</code>
 * or <code>static</code>.</li>
 * </ul>
 * @author Rick Giles
 */
public class EntityBeanEjbCreateCheck
    extends AbstractMethodCheck
{
    /** @see com.puppycrawl.tools.checkstyle.api.Check */
    public void visitToken(DetailAST aAST)
    {
        final DetailAST nameAST = aAST.findFirstToken(TokenTypes.IDENT);
        final String name = nameAST.getText();
        if (name.startsWith("ejbCreate")
            && Utils.implementsEntityBean(aAST))
        {
            checkMethod(aAST);
            if (Utils.isVoid(aAST)) {
                log(nameAST.getLineNo(), nameAST.getColumnNo(),
                    "voidmethod.bean", name);
            }
        }
    }
}
