////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2002  Oliver Burn
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

package com.puppycrawl.tools.checkstyle.checks;

import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.api.DetailAST;

/**
 * Checks for assignments in subexpressions, such as in
 * <code>String s = Integer.toString(i = 2);</code>.
 *
 * Rationale: With the exception of <code>for</code> iterators, all assignments
 * should occur in their own toplevel statement to increase readability.
 * With inner assignments like the above it is difficult to see all places
 * where a variable is set.
 */
public class InnerAssignmentCheck
        extends Check
{
    /**
     * The default tokens.
     * TODO: this has to be tweaked with |= and friends
     */
    private final int[] DEFAULT_TOKENS = new int[] {
        TokenTypes.ASSIGN,
        TokenTypes.PLUS_ASSIGN,
        TokenTypes.MINUS_ASSIGN
    };

    /** @see Check */
    public int[] getDefaultTokens()
    {
        return DEFAULT_TOKENS;
    }

    /** @see Check */
    public void visitToken(DetailAST aAST)
    {
        DetailAST parent1 = aAST.getParent();
        DetailAST parent2 = parent1.getParent();
        DetailAST parent3 = parent2.getParent();

        final boolean assigment = isAssignment(parent1);
        final boolean expr = parent1.getType() == TokenTypes.EXPR;
        final boolean exprList =
                expr && parent2.getType() == TokenTypes.ELIST;
        final boolean methodCall =
                exprList && parent3.getType() == TokenTypes.METHOD_CALL;
        final boolean ctorCall =
                exprList && parent3.getType() == TokenTypes.LITERAL_NEW;

        if (assigment || methodCall || ctorCall) {
            log(aAST.getLineNo(), aAST.getColumnNo(), "assignment.inner.avoid");
        }
    }

    /**
     * Checks if an AST is an assignment operator.
     * @param aAST the AST to check
     * @return true iff aAST is an assignment operator.
     */
    private boolean isAssignment(DetailAST aAST)
    {
        // TODO: make actual tokens available to Check and loop over actual tokens here?
        int[] tokens = getDefaultTokens();

        int astType = aAST.getType();

        for (int i = 0; i < tokens.length; i++) {
            int tokenType = tokens[i];
            if (astType == tokenType) {
                return true;
            }
        }
        return false;
    }


}
