/* ==========================================
 * JGraphT : a free Java graph-theory library
 * ==========================================
 *
 * Project Info:  http://jgrapht.sourceforge.net/
 * Project Lead:  Barak Naveh (http://sourceforge.net/users/barak_naveh)
 *
 * (C) Copyright 2003-2004, by Barak Naveh and Contributors.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */
/* -------------------
 * LabeledElement.java
 * -------------------
 * (C) Copyright 2003, by Barak Naveh and Contributors.
 *
 * Original Author:  Barak Naveh
 * Contributor(s):   Christian Hammer
 *
 * $Id$
 *
 * Changes
 * -------
 * 24-Jul-2003 : Initial revision (BN);
 * 11-Mar-2004 : Made generic (CH);
 *
 */
package org._3pq.jgrapht;

/**
 * An graph element (vertex or edge) that can have a label.
 *
 * @author Barak Naveh
 *
 * @since Jul 14, 2003
 */
public interface LabeledElement<L> {
    /**
     * Sets the specified label object to this element.
     *
     * @param label a label to set to this element.
     */
    public void setLabel( L label );


    /**
     * Returns the element's label, or <code>null</code> if element has no
     * label.
     *
     * @return the element's label, or <code>null</code> if element has no
     *         label.
     */
    public L getLabel(  );


    /**
     * Tests if this element has a label.
     *
     * @return <code>true</code> if the element has a label, otherwise
     *         <code>false</code>.
     */
    public boolean hasLabel(  );
}
