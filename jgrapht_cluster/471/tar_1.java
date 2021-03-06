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
 * along with this library; if not, write to the Free Software Foundation,
 * Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */
/* ------------------------------
 * ConnectivityInspectorTest.java
 * ------------------------------
 * (C) Copyright 2003-2005, by Barak Naveh and Contributors.
 *
 * Original Author:  Barak Naveh
 * Contributor(s):   John V. Sichi
 *
 * $Id: ConnectivityInspectorTest.java,v 1.11 2005/05/25 00:38:37 perfecthash
 * Exp $
 *
 * Changes
 * -------
 * 07-Aug-2003 : Initial revision (BN);
 * 20-Apr-2005 : Added StrongConnectivityInspector test (JVS);
 *
 */
package org._3pq.jgrapht.alg;

import java.util.*;

import junit.framework.*;

import org._3pq.jgrapht.*;
import org._3pq.jgrapht.graph.*;


/**
 * .
 *
 * @author Barak Naveh
 */
public class ConnectivityInspectorTest extends TestCase
{

    //~ Static fields/initializers --------------------------------------------

    private static final String V1 = new String("v1");
    private static final String V2 = new String("v2");
    private static final String V3 = new String("v3");
    private static final String V4 = new String("v4");

    //~ Instance fields -------------------------------------------------------

    //
    Edge<String> m_e1;
    Edge<String> m_e2;
    Edge<String> m_e3;
    Edge<String> m_e3_b;
    Edge<String> m_u;

    //~ Methods ---------------------------------------------------------------

    /**
     * .
     *
     * @return a graph
     */
    public Pseudograph<String,Edge<String>> create()
    {
        Pseudograph<String,Edge<String>> g = new Pseudograph<String,Edge<String>>();

        assertEquals(0, g.vertexSet().size());
        g.addVertex(V1);
        assertEquals(1, g.vertexSet().size());
        g.addVertex(V2);
        assertEquals(2, g.vertexSet().size());
        g.addVertex(V3);
        assertEquals(3, g.vertexSet().size());
        g.addVertex(V4);
        assertEquals(4, g.vertexSet().size());

        assertEquals(0, g.edgeSet().size());

        m_e1 = g.addEdge(V1, V2);
        assertEquals(1, g.edgeSet().size());

        m_e2 = g.addEdge(V2, V3);
        assertEquals(2, g.edgeSet().size());

        m_e3 = g.addEdge(V3, V1);
        assertEquals(3, g.edgeSet().size());

        m_e3_b = g.addEdge(V3, V1);
        assertEquals(4, g.edgeSet().size());
        assertNotNull(m_e3_b);

        m_u = g.addEdge(V1, V1);
        assertEquals(5, g.edgeSet().size());
        m_u = g.addEdge(V1, V1);
        assertEquals(6, g.edgeSet().size());

        return g;
    }

    /**
     * .
     */
    public void testDirectedGraph()
    {
        ListenableDirectedGraph<String,DirEdge<String>>
                g = new ListenableDirectedGraph<String,DirEdge<String>>();
        g.addVertex(V1);
        g.addVertex(V2);
        g.addVertex(V3);

        g.addEdge(V1, V2);

        ConnectivityInspector<String,DirEdge<String>>
                inspector = new ConnectivityInspector<String,DirEdge<String>>(g);
        g.addGraphListener(inspector);

        assertEquals(false, inspector.isGraphConnected());

        g.addEdge(V1, V3);

        assertEquals(true, inspector.isGraphConnected());
    }

    /**
     * .
     */
    public void testIsGraphConnected()
    {
        Pseudograph<String,Edge<String>> g = create();
        ConnectivityInspector<String,Edge<String>>
                inspector = new ConnectivityInspector<String,Edge<String>>(g);

        assertEquals(false, inspector.isGraphConnected());

        g.removeVertex(V4);
        inspector = new ConnectivityInspector<String,Edge<String>>(g);
        assertEquals(true, inspector.isGraphConnected());

        g.removeVertex(V1);
        assertEquals(1, g.edgeSet().size());

        g.removeEdge(m_e2);
        g.addEdge(V2, V2);
        assertEquals(1, g.edgeSet().size());

        inspector = new ConnectivityInspector<String,Edge<String>>(g);
        assertEquals(false, inspector.isGraphConnected());
    }

    /**
     * .
     */
    public void testStronglyConnected1()
    {
        DirectedGraph<String,DirEdge<String>>
                g = new DefaultDirectedGraph<String,DirEdge<String>>();
        g.addVertex(V1);
        g.addVertex(V2);
        g.addVertex(V3);
        g.addVertex(V4);

        g.addEdge(V1, V2);
        g.addEdge(V2, V1); // strongly connected

        g.addEdge(V3, V4); // only weakly connected

        StrongConnectivityInspector<String,DirEdge<String>>
                inspector = new StrongConnectivityInspector<String,DirEdge<String>>(g);

        // convert from List to Set because we need to ignore order
        // during comparison
        Set<Set<String>> actualSets = new HashSet<Set<String>>(inspector.stronglyConnectedSets());

        // construct the expected answer
        Set<Set<String>> expectedSets = new HashSet<Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add(V1);
        set.add(V2);
        expectedSets.add(set);
        set = new HashSet<String>();
        set.add(V3);
        expectedSets.add(set);
        set = new HashSet<String>();
        set.add(V4);
        expectedSets.add(set);

        assertEquals(expectedSets, actualSets);

        actualSets.clear();

        List<DirectedSubgraph<String,DirEdge<String>>>
                subgraphs = inspector.stronglyConnectedSubgraphs();
        for (DirectedSubgraph<String, DirEdge<String>> sg : subgraphs) {
            actualSets.add(sg.vertexSet());

            StrongConnectivityInspector<String,DirEdge<String>> ci =
                new StrongConnectivityInspector<String,DirEdge<String>>(sg);
            assertTrue(ci.isStronglyConnected());
        }
        
        assertEquals(expectedSets, actualSets);
    }

    /**
     * .
     */
    public void testStronglyConnected2()
    {
        DirectedGraph<String,DirEdge<String>>
                g = new DefaultDirectedGraph<String,DirEdge<String>>();
        g.addVertex(V1);
        g.addVertex(V2);
        g.addVertex(V3);
        g.addVertex(V4);

        g.addEdge(V1, V2);
        g.addEdge(V2, V1); // strongly connected

        g.addEdge(V4, V3); // only weakly connected
        g.addEdge(V3, V2); // only weakly connected

        StrongConnectivityInspector<String,DirEdge<String>> inspector =
            new StrongConnectivityInspector<String,DirEdge<String>>(g);

        // convert from List to Set because we need to ignore order
        // during comparison
        Set<Set<String>> actualSets = new HashSet<Set<String>>(inspector.stronglyConnectedSets());

        // construct the expected answer
        Set<Set<String>> expectedSets = new HashSet<Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add(V1);
        set.add(V2);
        expectedSets.add(set);
        set = new HashSet<String>();
        set.add(V3);
        expectedSets.add(set);
        set = new HashSet<String>();
        set.add(V4);
        expectedSets.add(set);

        assertEquals(expectedSets, actualSets);

        actualSets.clear();

        List<DirectedSubgraph<String,DirEdge<String>>>
                subgraphs = inspector.stronglyConnectedSubgraphs();
        for (DirectedSubgraph<String, DirEdge<String>> sg : subgraphs) {
            actualSets.add(sg.vertexSet());

            StrongConnectivityInspector<String,DirEdge<String>> ci =
                new StrongConnectivityInspector<String,DirEdge<String>>(sg);
            assertTrue(ci.isStronglyConnected());
        }

        assertEquals(expectedSets, actualSets);
    }

    /**
     * .
     */
    public void testStronglyConnected3()
    {
        DirectedGraph<String,DirEdge<String>>
                g = new DefaultDirectedGraph<String,DirEdge<String>>();
        g.addVertex(V1);
        g.addVertex(V2);
        g.addVertex(V3);
        g.addVertex(V4);

        g.addEdge(V1, V2);
        g.addEdge(V2, V3);
        g.addEdge(V3, V1); // strongly connected

        g.addEdge(V1, V4);
        g.addEdge(V2, V4);
        g.addEdge(V3, V4); // weakly connected

        StrongConnectivityInspector<String,DirEdge<String>> inspector =
            new StrongConnectivityInspector<String,DirEdge<String>>(g);

        // convert from List to Set because we need to ignore order
        // during comparison
        Set<Set<String>> actualSets = new HashSet<Set<String>>(inspector.stronglyConnectedSets());

        // construct the expected answer
        Set<Set<String>> expectedSets = new HashSet<Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add(V1);
        set.add(V2);
        set.add(V3);
        expectedSets.add(set);
        set = new HashSet<String>();
        set.add(V4);
        expectedSets.add(set);

        assertEquals(expectedSets, actualSets);

        actualSets.clear();

        List<DirectedSubgraph<String,DirEdge<String>>>
                subgraphs = inspector.stronglyConnectedSubgraphs();

        for (DirectedSubgraph<String, DirEdge<String>> sg : subgraphs) {
            actualSets.add(sg.vertexSet());

            StrongConnectivityInspector<String, DirEdge<String>> ci =
                new StrongConnectivityInspector<String, DirEdge<String>>(sg);
            assertTrue(ci.isStronglyConnected());
        }

        assertEquals(expectedSets, actualSets);
    }
}
