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
/* ----------------------
 * AbstractBaseGraph.java
 * ----------------------
 * (C) Copyright 2003, by Barak Naveh and Contributors.
 *
 * Original Author:  Barak Naveh
 * Contributor(s):   John V. Sichi
 *                   Christian Hammer
 *
 * $Id$
 *
 * Changes
 * -------
 * 24-Jul-2003 : Initial revision (BN);
 * 10-Aug-2003 : General edge refactoring (BN);
 * 06-Nov-2003 : Change edge sharing semantics (JVS);
 * 07-Feb-2004 : Enabled serialization (BN);
 * 11-Mar-2004 : Made generic (CH);
 * 01-Jun-2005 : Added EdgeListFactory (JVS);
 * 07-May-2006 : Changed from List<Edge> to Set<Edge> (JVS);
 * 28-May-2006 : Moved connectivity info from edge to graph (JVS);
 *
 */
package org.jgrapht.graph;

import java.io.*;

import java.util.*;

import org.jgrapht.*;
import org.jgrapht.util.*;


/**
 * The most general implementation of the {@link org.jgrapht.Graph}
 * interface. Its subclasses add various restrictions to get more specific
 * graphs. The decision whether it is directed or undirected is decided at
 * construction time and cannot be later modified (see constructor for
 * details).
 *
 * <p>This graph implementation guarantees deterministic vertex and edge set
 * ordering (via {@link LinkedHashMap} and {@link LinkedHashSet}).</p>
 *
 * @author Barak Naveh
 * @since Jul 24, 2003
 */
public abstract class AbstractBaseGraph<V, E>
    extends AbstractGraph<V, E> implements Graph<V, E>, Cloneable,
        Serializable
{

    //~ Static fields/initializers --------------------------------------------

    private static final String LOOPS_NOT_ALLOWED = "loops not allowed";

    //~ Instance fields -------------------------------------------------------

    // friendly (to improve performance)
    /**
     * default: HashMap may be reinitialized to some other map (eg. a TreeMap
     * with a comparator for the used node type)
     * FIXME hb 051124: Is it necessary to map to Object?
     */
    protected Map<V, Object> m_vertexMap;
    boolean m_allowingLoops;

    private EdgeFactory<V, E> m_edgeFactory;
    private EdgeSetFactory<V,E> m_edgeListFactory;
    private Map<E, IntrusiveEdge> m_edgeMap;
    private transient Set<E> m_unmodifiableEdgeSet = null;
    private transient Set<V> m_unmodifiableVertexSet = null;
    private Specifics m_specifics;
    private boolean m_allowingMultipleEdges;

    //~ Constructors ----------------------------------------------------------

    /**
     * Construct a new pseudograph. The pseudograph can either be directed or
     * undirected, depending on the specified edge factory. A sample edge is
     * created using the edge factory to see if the factory is compatible with
     * this class of  graph. For example, if this graph is a <code>
     * DirectedGraph</code> the edge factory must produce <code>
     * DirectedEdge</code> s. If this is not the case, an <code>
     * IllegalArgumentException</code> is thrown.
     *
     * @param ef the edge factory of the new graph.
     * @param allowMultipleEdges whether to allow multiple edges or not.
     * @param allowLoops whether to allow edges that are self-loops or not.
     *
     * @throws NullPointerException if the specified edge factory is <code>
     *                              null</code>.
     */
    public AbstractBaseGraph(EdgeFactory<V, E> ef,
        boolean allowMultipleEdges,
        boolean allowLoops)
    {
        if (ef == null) {
            throw new NullPointerException();
        }
        
        m_vertexMap = new LinkedHashMap<V,Object>();
        m_edgeMap = new LinkedHashMap<E, IntrusiveEdge>();
        m_edgeFactory = ef;
        m_allowingLoops = allowLoops;
        m_allowingMultipleEdges = allowMultipleEdges;

        m_specifics = createSpecifics();

        m_edgeListFactory = new ArrayListFactory<V,E>();
    }

    //~ Methods ---------------------------------------------------------------

    /**
     * @see Graph#getAllEdges(Object, Object)
     */
    public Set<E> getAllEdges(V sourceVertex, V targetVertex)
    {
        return m_specifics.getAllEdges(sourceVertex, targetVertex);
    }

    /**
     * Returns <code>true</code> if and only if self-loops are allowed in this
     * graph. A self loop is an edge that its source and target vertices are
     * the same.
     *
     * @return <code>true</code> if and only if graph loops are allowed.
     */
    public boolean isAllowingLoops()
    {
        return m_allowingLoops;
    }

    /**
     * Returns <code>true</code> if and only if multiple edges are allowed in
     * this graph. The meaning of multiple edges is that there can be many
     * edges going from vertex v1 to vertex v2.
     *
     * @return <code>true</code> if and only if multiple edges are allowed.
     */
    public boolean isAllowingMultipleEdges()
    {
        return m_allowingMultipleEdges;
    }

    /**
     * @see Graph#getEdge(Object, Object)
     */
    public E getEdge(V sourceVertex, V targetVertex)
    {
        return m_specifics.getEdge(sourceVertex, targetVertex);
    }

    /**
     * @see Graph#getEdgeFactory()
     */
    public EdgeFactory<V,E> getEdgeFactory()
    {
        return m_edgeFactory;
    }

    /**
     * Set the {@link EdgeSetFactory} to use for this graph. Initially, a
     * graph is created with a default implementation which always supplies an
     * {@link java.util.ArrayList} with capacity 1.
     *
     * @param edgeListFactory factory to use for subsequently created edge
     *                        lists (this call has no effect on existing edge
     *                        lists)
     */
    public void setEdgeSetFactory(EdgeSetFactory<V,E> edgeListFactory)
    {
        m_edgeListFactory = edgeListFactory;
    }

    /**
     * @see Graph#addEdge(Object, Object)
     */
    public E addEdge(V sourceVertex, V targetVertex)
    {
        assertVertexExist(sourceVertex);
        assertVertexExist(targetVertex);

        if (!m_allowingMultipleEdges
            && containsEdge(sourceVertex, targetVertex)) {
            return null;
        }

        if (!m_allowingLoops && sourceVertex.equals(targetVertex)) {
            throw new IllegalArgumentException(LOOPS_NOT_ALLOWED);
        }

        E e = m_edgeFactory.createEdge(sourceVertex, targetVertex);

        if (containsEdge(e)) { // this restriction should stay!

            return null;
        } else {
            IntrusiveEdge intrusiveEdge =
                createIntrusiveEdge(e, sourceVertex, targetVertex);

            m_edgeMap.put(e, intrusiveEdge);
            m_specifics.addEdgeToTouchingVertices(e);

            return e;
        }
    }

    /**
     * @see Graph#addEdge(V, V, E)
     */
    public boolean addEdge(V sourceVertex, V targetVertex, E e)
    {
        if (e == null) {
            throw new NullPointerException();
        } else if (containsEdge(e)) {
            return false;
        }

        assertVertexExist(sourceVertex);
        assertVertexExist(targetVertex);
        
        if (!m_allowingMultipleEdges
            && containsEdge(sourceVertex, targetVertex)) {
            return false;
        }

        if (!m_allowingLoops && sourceVertex.equals(targetVertex)) {
            throw new IllegalArgumentException(LOOPS_NOT_ALLOWED);
        }

        IntrusiveEdge intrusiveEdge =
            createIntrusiveEdge(e, sourceVertex, targetVertex);

        m_edgeMap.put(e, intrusiveEdge);
        m_specifics.addEdgeToTouchingVertices(e);

        return true;
    }

    private IntrusiveEdge createIntrusiveEdge(
        E e, V sourceVertex, V targetVertex)
    {
        IntrusiveEdge intrusiveEdge;
        if (e instanceof IntrusiveEdge) {
            intrusiveEdge = (IntrusiveEdge) e;
        } else {
            intrusiveEdge = new IntrusiveEdge();
        }
        intrusiveEdge.source = sourceVertex;
        intrusiveEdge.target = targetVertex;
        return intrusiveEdge;
    }

    /**
     * @see Graph#addVertex(Object)
     */
    public boolean addVertex(V v)
    {
        if (v == null) {
            throw new NullPointerException();
        } else if (containsVertex(v)) {
            return false;
        } else {
            m_vertexMap.put(v, null); // add with a lazy edge container entry

            return true;
        }
    }

    /**
     * @see Graph#getEdgeSource(E)
     */
    public V getEdgeSource(E e)
    {
        return (V) getIntrusiveEdge(e).source;
    }

    /**
     * @see Graph#getEdgeTarget(E)
     */
    public V getEdgeTarget(E e)
    {
        return (V) getIntrusiveEdge(e).target;
    }

    private IntrusiveEdge getIntrusiveEdge(E e)
    {
        if (e instanceof IntrusiveEdge) {
            return (IntrusiveEdge) e;
        }

        return m_edgeMap.get(e);
    }

    /**
     * Returns a shallow copy of this graph instance.  Neither edges nor
     * vertices are cloned.
     *
     * @return a shallow copy of this set.
     *
     * @throws RuntimeException
     *
     * @see java.lang.Object#clone()
     */
    @SuppressWarnings("unchecked")    //FIXME hb 28-nov-05: See FIXME below
    public Object clone()
    {
        try {
            //FIXME hb 28-nov-05: In order to flow with Java-Generics, an appropriate constructor should be created (cf. Collections)
            AbstractBaseGraph<V, E> newGraph =
                (AbstractBaseGraph) super.clone();

            newGraph.m_vertexMap = new LinkedHashMap<V, Object>();
            newGraph.m_edgeMap = new LinkedHashMap<E, IntrusiveEdge>();

            newGraph.m_edgeFactory = this.m_edgeFactory;
            newGraph.m_unmodifiableEdgeSet = null;
            newGraph.m_unmodifiableVertexSet = null;

            // NOTE:  it's important for this to happen in an object
            // method so that the new inner class instance gets associated with
            // the right outer class instance
            newGraph.m_specifics = newGraph.createSpecifics();

            Graphs.addGraph(newGraph, this);

            return newGraph;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    /**
     * @see Graph#containsEdge(Edge)
     */
    public boolean containsEdge(E e)
    {
        return m_edgeMap.containsKey(e);
    }

    /**
     * @see Graph#containsVertex(Object)
     */
    public boolean containsVertex(V v)
    {
        return m_vertexMap.containsKey(v);
    }

    /**
     * @see org.jgrapht.UndirectedGraph#degreeOf(java.lang.Object)
     */
    public int degreeOf(V vertex)
    {
        return m_specifics.degreeOf(vertex);
    }

    /**
     * @see Graph#edgeSet()
     */
    public Set<E> edgeSet()
    {
        if (m_unmodifiableEdgeSet == null) {
            m_unmodifiableEdgeSet =
                Collections.unmodifiableSet(m_edgeMap.keySet());
        }

        return m_unmodifiableEdgeSet;
    }

    /**
     * @see Graph#edgesOf(Object)
     */
    public Set<E> edgesOf(V vertex)
    {
        return m_specifics.edgesOf(vertex);
    }

    /**
     * @see org.jgrapht.DirectedGraph#inDegreeOf(java.lang.Object)
     */
    public int inDegreeOf(V vertex)
    {
        return m_specifics.inDegreeOf(vertex);
    }

    /**
     * @see org.jgrapht.DirectedGraph#incomingEdgesOf(java.lang.Object)
     */
    public Set<E> incomingEdgesOf(V vertex)
    {
        return m_specifics.incomingEdgesOf(vertex);
    }

    /**
     * @see org.jgrapht.DirectedGraph#outDegreeOf(java.lang.Object)
     */
    public int outDegreeOf(V vertex)
    {
        return m_specifics.outDegreeOf(vertex);
    }

    /**
     * @see org.jgrapht.DirectedGraph#outgoingEdgesOf(java.lang.Object)
     */
    public Set<E> outgoingEdgesOf(V vertex)
    {
        return m_specifics.outgoingEdgesOf(vertex);
    }

    /**
     * @see Graph#removeEdge(Object, Object)
     */
    public E removeEdge(V sourceVertex, V targetVertex)
    {
        E e = getEdge(sourceVertex, targetVertex);

        if (e != null) {
            m_specifics.removeEdgeFromTouchingVertices(e);
            m_edgeMap.remove(e);
        }

        return e;
    }

    /**
     * @see Graph#removeEdge(Edge)
     */
    public boolean removeEdge(E e)
    {
        if (containsEdge(e)) {
            m_specifics.removeEdgeFromTouchingVertices(e);
            m_edgeMap.remove(e);

            return true;
        } else {
            return false;
        }
    }

    /**
     * @see Graph#removeVertex(Object)
     */
    public boolean removeVertex(V v)
    {
        if (containsVertex(v)) {
            Set<E> touchingEdgesList = edgesOf(v);

            // cannot iterate over list - will cause
            // ConcurrentModificationException
            removeAllEdges(new ArrayList<E>(touchingEdgesList));

            m_vertexMap.remove(v); // remove the vertex itself

            return true;
        } else {
            return false;
        }
    }

    /**
     * @see Graph#vertexSet()
     */
    public Set<V> vertexSet()
    {
        if (m_unmodifiableVertexSet == null) {
            m_unmodifiableVertexSet =
                Collections.unmodifiableSet(m_vertexMap.keySet());
        }

        return m_unmodifiableVertexSet;
    }

    /**
     * @see Graph#getEdgeWeight(E)
     */
    public double getEdgeWeight(E e)
    {
        if (e instanceof DefaultWeightedEdge) {
            return ((DefaultWeightedEdge) e).weight;
        } else {
            return WeightedGraph.DEFAULT_EDGE_WEIGHT;
        }
    }

    /**
     * @see WeightedGraph#setEdgeWeight(E)
     */
    public void setEdgeWeight(E e, double weight)
    {
        assert(e instanceof DefaultWeightedEdge) : e.getClass();
        ((DefaultWeightedEdge) e).weight = weight;
    }
    
    private Specifics createSpecifics()
    {
        if (this instanceof DirectedGraph) {
            return new DirectedSpecifics();
        } else if (this instanceof UndirectedGraph) {
            return new UndirectedSpecifics();
        } else {
            throw new IllegalArgumentException(
                "must be instance of either DirectedGraph or UndirectedGraph");
        }
    }

    //~ Inner Classes ---------------------------------------------------------

    /**
     * .
     *
     * @author Barak Naveh
     */
    private abstract class Specifics implements Serializable
    {
        /**
         * .
         *
         * @param sourceVertex
         * @param targetVertex
         *
         * @return
         */
        public abstract Set<E> getAllEdges(V sourceVertex,
            V targetVertex);

        /**
         * .
         *
         * @param sourceVertex
         * @param targetVertex
         *
         * @return
         */
        public abstract E getEdge(V sourceVertex, V targetVertex);

        /**
         * Adds the specified edge to the edge containers of its source and
         * target vertices.
         *
         * @param e
         */
        public abstract void addEdgeToTouchingVertices(E e);

        /**
         * .
         *
         * @param vertex
         *
         * @return
         */
        public abstract int degreeOf(V vertex);

        /**
         * .
         *
         * @param vertex
         *
         * @return
         */
        public abstract Set<E> edgesOf(V vertex);

        /**
         * .
         *
         * @param vertex
         *
         * @return
         */
        public abstract int inDegreeOf(V vertex);

        /**
         * .
         *
         * @param vertex
         *
         * @return
         */
        public abstract Set<E> incomingEdgesOf(V vertex);

        /**
         * .
         *
         * @param vertex
         *
         * @return
         */
        public abstract int outDegreeOf(V vertex);

        /**
         * .
         *
         * @param vertex
         *
         * @return
         */
        public abstract Set<E> outgoingEdgesOf(V vertex);

        /**
         * Removes the specified edge from the edge containers of its source
         * and target vertices.
         *
         * @param e
         */
        public abstract void removeEdgeFromTouchingVertices(E e);
    }

    private static class ArrayListFactory<VV, EE>
        implements EdgeSetFactory<VV,EE>, Serializable
    {
        private static final long serialVersionUID = 5936902837403445985L;

        /**
         * @see EdgeSetFactory.createEdgeSet
         */
        public Set<EE> createEdgeSet(VV vertex)
        {
            // NOTE:  use size 1 to keep memory usage under control
            // for the common case of vertices with low degree
            return new ArrayUnenforcedSet<EE>(1);
        }
    }

    /**
     * A container of for vertex edges.
     *
     * <p>In this edge container we use array lists to minimize memory toll.
     * However, for high-degree vertices we replace the entire edge container
     * with a direct access subclass (to be implemented).</p>
     *
     * @author Barak Naveh
     */
    private static class DirectedEdgeContainer<VV, EE>
        implements Serializable
    {
        private static final long serialVersionUID = 7494242245729767106L;
        Set<EE> m_incoming;
        Set<EE> m_outgoing;
        private transient Set<EE> m_unmodifiableIncoming = null;
        private transient Set<EE> m_unmodifiableOutgoing = null;

        DirectedEdgeContainer(EdgeSetFactory<VV,EE> edgeListFactory, VV vertex)
        {
            m_incoming = edgeListFactory.createEdgeSet(vertex);
            m_outgoing = edgeListFactory.createEdgeSet(vertex);
        }

        /**
         * A lazy build of unmodifiable incoming edge set.
         *
         * @return
         */
        public Set<EE> getUnmodifiableIncomingEdges()
        {
            if (m_unmodifiableIncoming == null) {
                m_unmodifiableIncoming =
                    Collections.unmodifiableSet(m_incoming);
            }

            return m_unmodifiableIncoming;
        }

        /**
         * A lazy build of unmodifiable outgoing edge set.
         *
         * @return
         */
        public Set<EE> getUnmodifiableOutgoingEdges()
        {
            if (m_unmodifiableOutgoing == null) {
                m_unmodifiableOutgoing =
                    Collections.unmodifiableSet(m_outgoing);
            }

            return m_unmodifiableOutgoing;
        }

        /**
         * .
         *
         * @param e
         */
        public void addIncomingEdge(EE e)
        {
            m_incoming.add(e);
        }

        /**
         * .
         *
         * @param e
         */
        public void addOutgoingEdge(EE e)
        {
            m_outgoing.add(e);
        }

        /**
         * .
         *
         * @param e
         */
        public void removeIncomingEdge(EE e)
        {
            m_incoming.remove(e);
        }

        /**
         * .
         *
         * @param e
         */
        public void removeOutgoingEdge(EE e)
        {
            m_outgoing.remove(e);
        }
    }

    /**
     * .
     *
     * @author Barak Naveh
     */
    private class DirectedSpecifics extends Specifics implements Serializable
    {
        private static final long serialVersionUID = 8971725103718958232L;
        private static final String NOT_IN_DIRECTED_GRAPH =
            "no such operation in a directed graph";

        /**
         * @see Graph#getAllEdges(Object, Object)
         */
        public Set<E> getAllEdges(V sourceVertex, V targetVertex)
        {
            Set<E> edges = null;

            if (containsVertex(sourceVertex)
                && containsVertex(targetVertex)) {
                edges = new ArrayUnenforcedSet<E>();

                DirectedEdgeContainer<V,E> ec = getEdgeContainer(sourceVertex);

                Iterator<E> iter = ec.m_outgoing.iterator();

                while (iter.hasNext()) {
                    E e = iter.next();

                    if (getEdgeTarget(e).equals(targetVertex)) {
                        edges.add(e);
                    }
                }
            }

            return edges;
        }

        /**
         * @see Graph#getEdge(Object, Object)
         */
        public E getEdge(V sourceVertex, V targetVertex)
        {
            if (containsVertex(sourceVertex)
                && containsVertex(targetVertex)) {
                DirectedEdgeContainer<V,E> ec = getEdgeContainer(sourceVertex);

                Iterator<E> iter = ec.m_outgoing.iterator();

                while (iter.hasNext()) {
                    E e = iter.next();

                    if (getEdgeTarget(e).equals(targetVertex)) {
                        return e;
                    }
                }
            }

            return null;
        }

        /**
         * @see AbstractBaseGraph#addEdgeToTouchingVertices(Edge)
         */
        public void addEdgeToTouchingVertices(E e)
        {
            V source = getEdgeSource(e);
            V target = getEdgeTarget(e);

            getEdgeContainer(source).addOutgoingEdge(e);
            getEdgeContainer(target).addIncomingEdge(e);
        }

        /**
         * @see UndirectedGraph#degreeOf(Object)
         */
        public int degreeOf(V vertex)
        {
            throw new UnsupportedOperationException(NOT_IN_DIRECTED_GRAPH);
        }

        /**
         * @see Graph#edgesOf(Object)
         */
        public Set<E> edgesOf(V vertex)
        {
            ArrayUnenforcedSet<E> inAndOut =
                new ArrayUnenforcedSet<E>(getEdgeContainer(vertex).m_incoming);
            inAndOut.addAll(getEdgeContainer(vertex).m_outgoing);

            // we have two copies for each self-loop - remove one of them.
            if (m_allowingLoops) {
                Set<E> loops = getAllEdges(vertex, vertex);

                for (int i = 0; i < inAndOut.size();) {
                    Object e = inAndOut.get(i);

                    if (loops.contains(e)) {
                        inAndOut.remove(i);
                        loops.remove(e); // so we remove it only once
                    } else {
                        i++;
                    }
                }
            }

            return inAndOut;
        }

        /**
         * @see DirectedGraph#inDegree(Object)
         */
        public int inDegreeOf(V vertex)
        {
            return getEdgeContainer(vertex).m_incoming.size();
        }

        /**
         * @see DirectedGraph#incomingEdges(Object)
         */
        public Set<E> incomingEdgesOf(V vertex)
        {
            return getEdgeContainer(vertex).getUnmodifiableIncomingEdges();
        }

        /**
         * @see DirectedGraph#outDegree(Object)
         */
        public int outDegreeOf(V vertex)
        {
            return getEdgeContainer(vertex).m_outgoing.size();
        }

        /**
         * @see DirectedGraph#outgoingEdges(Object)
         */
        public Set<E> outgoingEdgesOf(V vertex)
        {
            return getEdgeContainer(vertex).getUnmodifiableOutgoingEdges();
        }

        /**
         * @see AbstractBaseGraph#removeEdgeFromTouchingVertices(Edge)
         */
        public void removeEdgeFromTouchingVertices(E e)
        {
            V source = getEdgeSource(e);
            V target = getEdgeTarget(e);

            getEdgeContainer(source).removeOutgoingEdge(e);
            getEdgeContainer(target).removeIncomingEdge(e);
        }

        /**
         * A lazy build of edge container for specified vertex.
         *
         * @param vertex a vertex in this graph.
         *
         * @return EdgeContainer
         */
        @SuppressWarnings("unchecked")    // FIXME hb 28-nov-05: see FIXME at decleration m_vertexMap
        private DirectedEdgeContainer<V,E> getEdgeContainer(V vertex)
        {
            assertVertexExist(vertex);

            DirectedEdgeContainer<V,E> ec =
                (DirectedEdgeContainer) m_vertexMap.get(vertex);

            if (ec == null) {
                ec = new DirectedEdgeContainer<V,E>(m_edgeListFactory, vertex);
                m_vertexMap.put(vertex, ec);
            }

            return ec;
        }
    }

    /**
     * A container of for vertex edges.
     *
     * <p>In this edge container we use array lists to minimize memory toll.
     * However, for high-degree vertices we replace the entire edge container
     * with a direct access subclass (to be implemented).</p>
     *
     * @author Barak Naveh
     */
    private static class UndirectedEdgeContainer<VV, EE>
        implements Serializable
    {
        private static final long serialVersionUID = -6623207588411170010L;
        Set<EE> m_vertexEdges;
        private transient Set<EE> m_unmodifiableVertexEdges = null;

        UndirectedEdgeContainer(
            EdgeSetFactory<VV,EE> edgeListFactory,
            VV vertex)
        {
            m_vertexEdges = edgeListFactory.createEdgeSet(vertex);
        }

        /**
         * A lazy build of unmodifiable list of vertex edges
         *
         * @return
         */
        public Set<EE> getUnmodifiableVertexEdges()
        {
            if (m_unmodifiableVertexEdges == null) {
                m_unmodifiableVertexEdges =
                    Collections.unmodifiableSet(m_vertexEdges);
            }

            return m_unmodifiableVertexEdges;
        }

        /**
         * .
         *
         * @param e
         */
        public void addEdge(EE e)
        {
            m_vertexEdges.add(e);
        }

        /**
         * .
         *
         * @return
         */
        public int edgeCount()
        {
            return m_vertexEdges.size();
        }

        /**
         * .
         *
         * @param e
         */
        public void removeEdge(EE e)
        {
            m_vertexEdges.remove(e);
        }
    }

    /**
     * .
     *
     * @author Barak Naveh
     */
    private class UndirectedSpecifics extends Specifics
        implements Serializable
    {
        private static final long serialVersionUID = 6494588405178655873L;
        private static final String NOT_IN_UNDIRECTED_GRAPH =
            "no such operation in an undirected graph";

        /**
         * @see Graph#getAllEdges(Object, Object)
         */
        public Set<E> getAllEdges(V sourceVertex, V targetVertex)
        {
            Set<E> edges = null;

            if (containsVertex(sourceVertex)
                && containsVertex(targetVertex)) {
                edges = new ArrayUnenforcedSet<E>();

                Iterator<E> iter =
                    getEdgeContainer(sourceVertex).m_vertexEdges.iterator();

                while (iter.hasNext()) {
                    E e = iter.next();

                    boolean equalStraight =
                        sourceVertex.equals(getEdgeSource(e))
                        && targetVertex.equals(getEdgeTarget(e));

                    boolean equalInverted =
                        sourceVertex.equals(getEdgeTarget(e))
                        && targetVertex.equals(getEdgeSource(e));

                    if (equalStraight || equalInverted) {
                        edges.add(e);
                    }
                }
            }

            return edges;
        }

        /**
         * @see Graph#getEdge(Object, Object)
         */
        public E getEdge(V sourceVertex, V targetVertex)
        {
            if (containsVertex(sourceVertex)
                && containsVertex(targetVertex)) {
                Iterator<E> iter =
                    getEdgeContainer(sourceVertex).m_vertexEdges.iterator();

                while (iter.hasNext()) {
                    E e = iter.next();

                    boolean equalStraight =
                        sourceVertex.equals(getEdgeSource(e))
                        && targetVertex.equals(getEdgeTarget(e));

                    boolean equalInverted =
                        sourceVertex.equals(getEdgeTarget(e))
                        && targetVertex.equals(getEdgeSource(e));

                    if (equalStraight || equalInverted) {
                        return e;
                    }
                }
            }

            return null;
        }

        /**
         * @see AbstractBaseGraph#addEdgeToTouchingVertices(Edge)
         */
        public void addEdgeToTouchingVertices(E e)
        {
            V source = getEdgeSource(e);
            V target = getEdgeTarget(e);

            getEdgeContainer(source).addEdge(e);

            if (source != target) {
                getEdgeContainer(target).addEdge(e);
            }
        }

        /**
         * @see UndirectedGraph#degreeOf(V)
         */
        public int degreeOf(V vertex)
        {
            if (m_allowingLoops) { // then we must count, and add loops twice

                int degree = 0;
                Set<E> edges = getEdgeContainer(vertex).m_vertexEdges;

                for (E e : edges ) {

                    if (getEdgeSource(e).equals(getEdgeTarget(e))) {
                        degree += 2;
                    } else {
                        degree += 1;
                    }
                }

                return degree;
            } else {
                return getEdgeContainer(vertex).edgeCount();
            }
        }

        /**
         * @see Graph#edgesOf(V)
         */
        public Set<E> edgesOf(V vertex)
        {
            return getEdgeContainer(vertex).getUnmodifiableVertexEdges();
        }

        /**
         * @see DirectedGraph#inDegreeOf(Object)
         */
        public int inDegreeOf(V vertex)
        {
            throw new UnsupportedOperationException(NOT_IN_UNDIRECTED_GRAPH);
        }

        /**
         * @see DirectedGraph#incomingEdgesOf(Object)
         */
        public Set<E> incomingEdgesOf(V vertex)
        {
            throw new UnsupportedOperationException(NOT_IN_UNDIRECTED_GRAPH);
        }

        /**
         * @see DirectedGraph#outDegreeOf(Object)
         */
        public int outDegreeOf(V vertex)
        {
            throw new UnsupportedOperationException(NOT_IN_UNDIRECTED_GRAPH);
        }

        /**
         * @see DirectedGraph#outgoingEdgesOf(Object)
         */
        public Set<E> outgoingEdgesOf(V vertex)
        {
            throw new UnsupportedOperationException(NOT_IN_UNDIRECTED_GRAPH);
        }

        /**
         * @see AbstractBaseGraph#removeEdgeFromTouchingVertices(Edge)
         */
        public void removeEdgeFromTouchingVertices(E e)
        {
            V source = getEdgeSource(e);
            V target = getEdgeTarget(e);

            getEdgeContainer(source).removeEdge(e);

            if (source != target) {
                getEdgeContainer(target).removeEdge(e);
            }
        }

        /**
         * A lazy build of edge container for specified vertex.
         *
         * @param vertex a vertex in this graph.
         *
         * @return EdgeContainer
         */
        @SuppressWarnings("unchecked")    // FIXME hb 28-nov-05: see FIXME at decleration m_vertexMap
        private UndirectedEdgeContainer<V, E> getEdgeContainer(V vertex)
        {
            assertVertexExist(vertex);

            UndirectedEdgeContainer<V, E> ec =
                (UndirectedEdgeContainer) m_vertexMap.get(vertex);

            if (ec == null) {
                ec = new UndirectedEdgeContainer<V,E>(m_edgeListFactory, vertex);
                m_vertexMap.put(vertex, ec);
            }

            return ec;
        }
    }
}
