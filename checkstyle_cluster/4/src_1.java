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
 * Checks that an EntityBean defines method ejbFindByPrimaryKey.
 * @author Rick Giles
 */
public class EntityBeanFindByPrimaryKeyCheck
    extends AbstractBeanCheck
{
    /**
     * @see com.puppycrawl.tools.checkstyle.api.Check
     */
    public void visitToken(DetailAST aAST)
    {
        if (Utils.hasImplements(aAST, "javax.ejb.EntityBean")
            && !Utils.isAbstract(aAST)
            && !Utils.hasPublicMethod(aAST, "ejbFindByPrimaryKey", false, 1))
        {
            final DetailAST nameAST = aAST.findFirstToken(TokenTypes.IDENT);
            log(
                aAST.getLineNo(),
                nameAST.getColumnNo(),
                "missingmethod.bean",
                new Object[] {"Entity bean", "ejbFindByPrimaryKey"});
        }
    }
}
