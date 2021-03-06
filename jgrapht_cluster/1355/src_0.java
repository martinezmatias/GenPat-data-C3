/* ==========================================
 * JGraphT : a free Java graph-theory library
 * ==========================================
 *
 * Project Info:  http://jgrapht.sourceforge.net/
 * Project Creator:  Barak Naveh (http://sourceforge.net/users/barak_naveh)
 *
 * (C) Copyright 2003-2008, by Barak Naveh and Contributors.
 *
 * This program and the accompanying materials are dual-licensed under
 * either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation, or (at your option) any
 * later version.
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation.
 */
/* ------------------------
 * SimpleGraphPath.java
 * ------------------------
 * (C) Copyright 2003-2008, by Barak Naveh and Contributors.
 *
 * Original Author:  Rodrigo López Dato
 * Contributor(s):   -
 *
 * $Id$
 *
 * Changes
 * -------
 * 22-Jan-2014 : Initial revision;
 */

package org.jgrapht.graph;

import java.util.*;

import org.jgrapht.*;

/**
 * A vertex-based representation of a simple path. The graph must be simple for
 * the vertices to uniquely determine a path. See {@link SimpleGraph}
 */
public class SimpleGraphPath<V, E>
    implements GraphPath<V, E>
{

    private SimpleGraph<V, E> graph;
    private List<V> vertices;

    /**
     * @param simpleGraph The simple graph where the path is.
     * @param vertices A list of vertices that make up the path.
     * @throws IllegalArgumentException if the vertices are not in the path or
     *         if they do not define a path in the graph.
     */
    public SimpleGraphPath(SimpleGraph<V, E> simpleGraph, List<V> vertices)
    {
        this.graph = simpleGraph;
        this.vertices = vertices;

        for (int i = 0; i < getVertexList().size() - 1; i++) {
            if (getGraph().getEdge(
                getVertexList().get(i),
                getVertexList().get(i + 1)) == null)
            {
                throw new IllegalArgumentException(
                    "The specified vertices do not form a path");
            }
        }

    }

    @Override
    public SimpleGraph<V, E> getGraph()
    {
        return this.graph;
    }

    @Override
    public V getStartVertex()
    {
        return this.getVertexList().get(0);
    }

    @Override
    public V getEndVertex()
    {
        return this.getVertexList().get(getVertexList().size() - 1);
    }

    @Override
    public List<E> getEdgeList()
    {
        List<E> result = new ArrayList<E>();
        for (int i = 0; i < getVertexList().size() - 1; i++) {
            result.add(this.getGraph().getEdge(
                getVertexList().get(i),
                getVertexList().get(i + 1)));
        }
        return result;
    }

    /**
     * @return A list of the vertices that define the path.
     */
    public List<V> getVertexList()
    {
        return vertices;
    }

    @Override
    public double getWeight()
    {
        double total = 0;
        for (E e : getEdgeList()) {
            total += getGraph().getEdgeWeight(e);
        }
        return total;
    }

}
