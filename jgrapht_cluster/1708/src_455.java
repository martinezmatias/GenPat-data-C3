/* ==========================================
 * JGraphT : a free Java graph-theory library
 * ==========================================
 *
 * Project Info:  http://jgrapht.sourceforge.net/
 * Project Lead:  Barak Naveh (barak_naveh@users.sourceforge.net)
 *
 * (C) Copyright 2003, by Barak Naveh and Contributors.
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
/* ----------------------
 * AbstractBaseGraph.java
 * ----------------------
 * (C) Copyright 2003, by Barak Naveh and Contributors.
 *
 * Original Author:  Barak Naveh
 * Contributor(s):   -
 *
 * $Id$
 *
 * Changes
 * -------
 * 24-Jul-2003 : Initial revision (BN);
 *
 */
package org._3pq.jgrapht.graph;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org._3pq.jgrapht.DirectedEdge;
import org._3pq.jgrapht.DirectedGraph;
import org._3pq.jgrapht.Edge;
import org._3pq.jgrapht.EdgeFactory;
import org._3pq.jgrapht.Graph;
import org._3pq.jgrapht.UndirectedEdge;
import org._3pq.jgrapht.UndirectedGraph;
import org._3pq.jgrapht.WeightedElement;

/**
 * The most general implementation of the {@link org._3pq.jgrapht.Graph}
 * interface. Its subclasses add various restrictions to get more specific
 * graphs. The decision whether it is directed or undirected is decided at
 * construction time and cannot be later modified (see constructor for
 * details).
 *
 * @author Barak Naveh
 *
 * @since Jul 24, 2003
 */
public abstract class AbstractBaseGraph extends AbstractGraph implements Graph,
    Cloneable, Serializable {
    private static final String LOOPS_NOT_ALLOWED = "loops not allowed";

    // friendly (to improve performance)
    Map     m_vertexMap;
    boolean m_loopsAllowed;

    // private
    private Class         m_factoryEdgeClass;
    private EdgeFactory   m_edgeFactory;
    private Set           m_edgeSet;
    private transient Set m_unmodifiableEdgeSet   = null;
    private transient Set m_unmodifiableVertexSet = null;
    private Specifics     m_specifics;
    private boolean       m_multipleEdgesAllowed;

    /**
     * Construct a new pseudograph. The pseudograph can either be directed or
     * undirected, depending on the specified edge factory. A sample edge is
     * created using the edge factory to see if the factory is compatible with
     * this class of  graph. For example, if this graph is a
     * <code>DirectedGraph</code> the edge factory must produce
     * <code>DirectedEdge</code>s. If this is not the case, an
     * <code>IllegalArgumentException</code> is thrown.
     *
     * @param ef the edge factory of the new graph.
     * @param multipleEdgesAllowed whether to allow multiple edges or not.
     * @param loopsAllowed whether to allow loops or not.
     *
     * @throws NullPointerException if the specified edge factory is
     *         <code>null</code>.
     * @throws IllegalArgumentException if the edge factory produces
     *         incompatible edges.
     */
    public AbstractBaseGraph( EdgeFactory ef, boolean multipleEdgesAllowed,
        boolean loopsAllowed ) {
        if( ef == null ) {
            throw new NullPointerException(  );
        }

        m_vertexMap                = new HashMap(  );
        m_edgeSet                  = new HashSet(  );
        m_edgeFactory              = ef;
        m_loopsAllowed             = loopsAllowed;
        m_multipleEdgesAllowed     = multipleEdgesAllowed;

        //
        Edge e = ef.createEdge( new Object(  ), new Object(  ) );

        if( e instanceof DirectedEdge && this instanceof DirectedGraph ) {
            m_specifics = new DirectedSpecifics(  );
        }
        else if( e instanceof UndirectedEdge && this instanceof UndirectedGraph ) {
            m_specifics = new UndirectedSpecifics(  );
        }
        else {
            throw new IllegalArgumentException( 
                "graph is incompatible with edge factory" );
        }

        m_factoryEdgeClass = e.getClass(  );
    }

    /**
     * @see Graph#getAllEdges(Object, Object)
     */
    public List getAllEdges( Object sourceVertex, Object targetVertex ) {
        return m_specifics.getAllEdges( sourceVertex, targetVertex );
    }


    /**
     * @see Graph#getEdge(Object, Object)
     */
    public Edge getEdge( Object sourceVertex, Object targetVertex ) {
        return m_specifics.getEdge( sourceVertex, targetVertex );
    }


    /**
     * @see Graph#getEdgeFactory()
     */
    public EdgeFactory getEdgeFactory(  ) {
        return m_edgeFactory;
    }


    /**
     * Returns <code>true</code> if and only if graph loops are allowed in this
     * graph.
     *
     * @return <code>true</code> if and only if graph loops are allowed.
     */
    public boolean isLoopsAllowed(  ) {
        return m_loopsAllowed;
    }


    /**
     * Returns <code>true</code> if and only if multiple edges are allowed in
     * this graph.
     *
     * @return <code>true</code> if and only if multiple edges are allowed.
     */
    public boolean isMultipleEdgesAllowed(  ) {
        return m_multipleEdgesAllowed;
    }


    /**
     * @see Graph#addEdge(Object, Object)
     */
    public Edge addEdge( Object sourceVertex, Object targetVertex ) {
        assertVertexExist( sourceVertex );
        assertVertexExist( targetVertex );

        if( !m_multipleEdgesAllowed
                && containsEdge( sourceVertex, targetVertex ) ) {
            return null;
        }

        if( !m_loopsAllowed && sourceVertex.equals( targetVertex ) ) {
            throw new IllegalArgumentException( LOOPS_NOT_ALLOWED );
        }

        Edge e = m_edgeFactory.createEdge( sourceVertex, targetVertex );

        if( containsEdge( e ) ) { // this restriction should stay!

            return null;
        }
        else {
            m_edgeSet.add( e );
            m_specifics.addEdgeToTouchingVertices( e );

            return e;
        }
    }


    /**
     * @see org._3pq.jgrapht.WeightedGraph#addEdge(Object, Object, double)
     */
    public Edge addEdge( Object sourceVertex, Object targetVertex, double weight ) {
        Edge e = addEdge( sourceVertex, targetVertex );

        if( e != null ) {
            ( (WeightedElement) e ).setWeight( weight );
        }

        return e;
    }


    /**
     * @see Graph#addEdge(Edge)
     */
    public boolean addEdge( Edge e ) {
        if( e == null ) {
            throw new NullPointerException(  );
        }
        else if( containsEdge( e ) ) {
            return false;
        }

        Object sourceVertex = e.getSource(  );
        Object targetVertex = e.getTarget(  );

        assertVertexExist( sourceVertex );
        assertVertexExist( targetVertex );

        assertCompatibleWithEdgeFactory( e );

        if( !m_multipleEdgesAllowed
                && containsEdge( sourceVertex, targetVertex ) ) {
            return false;
        }

        if( !m_loopsAllowed && sourceVertex.equals( targetVertex ) ) {
            throw new IllegalArgumentException( LOOPS_NOT_ALLOWED );
        }

        m_edgeSet.add( e );
        m_specifics.addEdgeToTouchingVertices( e );

        return true;
    }


    /**
     * @see Graph#addVertex(Object)
     */
    public boolean addVertex( Object v ) {
        if( v == null ) {
            throw new NullPointerException(  );
        }
        else if( containsVertex( v ) ) {
            return false;
        }
        else {
            m_vertexMap.put( v, null ); // add with a lazy edge container entry

            return true;
        }
    }


    /**
     * Returns a copy of this graph instance. Edges are cloned but vertices are
     * NOT cloned.
     *
     * @return a shallow copy of this set.
     *
     * @throws RuntimeException
     *
     * @see java.lang.Object#clone()
     */
    public Object clone(  ) {
        try {
            AbstractBaseGraph newGraph = (AbstractBaseGraph) super.clone(  );

            newGraph.m_vertexMap            = new HashMap(  );
            newGraph.m_edgeSet              = new HashSet(  );
            newGraph.m_factoryEdgeClass     = this.m_factoryEdgeClass;
            newGraph.m_edgeFactory          = this.m_edgeFactory;

            newGraph.addAllVertices( this.vertexSet(  ) );
            newGraph.addAllEdgeClones( this.edgeSet(  ) );

            return newGraph;
        }
         catch( CloneNotSupportedException e ) {
            e.printStackTrace(  );
            throw new RuntimeException( e );
        }
    }


    /**
     * @see Graph#containsEdge(Edge)
     */
    public boolean containsEdge( Edge e ) {
        return m_edgeSet.contains( e );
    }


    /**
     * @see Graph#containsVertex(Object)
     */
    public boolean containsVertex( Object v ) {
        return m_vertexMap.containsKey( v );
    }


    /**
     * @see org._3pq.jgrapht.UndirectedGraph#degreeOf(java.lang.Object)
     */
    public int degreeOf( Object vertex ) {
        return m_specifics.degreeOf( vertex );
    }


    /**
     * @see Graph#edgeSet()
     */
    public Set edgeSet(  ) {
        if( m_unmodifiableEdgeSet == null ) {
            m_unmodifiableEdgeSet = Collections.unmodifiableSet( m_edgeSet );
        }

        return m_unmodifiableEdgeSet;
    }


    /**
     * @see Graph#edgesOf(Object)
     */
    public List edgesOf( Object vertex ) {
        return m_specifics.edgesOf( vertex );
    }


    /**
     * @see org._3pq.jgrapht.DirectedGraph#inDegreeOf(java.lang.Object)
     */
    public int inDegreeOf( Object vertex ) {
        return m_specifics.inDegreeOf( vertex );
    }


    /**
     * @see org._3pq.jgrapht.DirectedGraph#incomingEdgesOf(java.lang.Object)
     */
    public List incomingEdgesOf( Object vertex ) {
        return m_specifics.incomingEdgesOf( vertex );
    }


    /**
     * @see org._3pq.jgrapht.DirectedGraph#outDegreeOf(java.lang.Object)
     */
    public int outDegreeOf( Object vertex ) {
        return m_specifics.outDegreeOf( vertex );
    }


    /**
     * @see org._3pq.jgrapht.DirectedGraph#outgoingEdgesOf(java.lang.Object)
     */
    public List outgoingEdgesOf( Object vertex ) {
        return m_specifics.outgoingEdgesOf( vertex );
    }


    /**
     * @see Graph#removeEdge(Object, Object)
     */
    public Edge removeEdge( Object sourceVertex, Object targetVertex ) {
        Edge e = getEdge( sourceVertex, targetVertex );

        if( e != null ) {
            m_specifics.removeEdgeFromTouchingVertices( e );
            m_edgeSet.remove( e );
        }

        return e;
    }


    /**
     * @see Graph#removeEdge(Edge)
     */
    public boolean removeEdge( Edge e ) {
        if( containsEdge( e ) ) {
            m_specifics.removeEdgeFromTouchingVertices( e );
            m_edgeSet.remove( e );

            return true;
        }
        else {
            return false;
        }
    }


    /**
     * @see Graph#removeVertex(Object)
     */
    public boolean removeVertex( Object v ) {
        if( containsVertex( v ) ) {
            List touchingEdgesList = edgesOf( v );

            // cannot iterate over list - will cause ConcurrentModificationException
            Edge[] touchingEdges = new Edge[ touchingEdgesList.size(  ) ];
            touchingEdgesList.toArray( touchingEdges );

            removeAllEdges( touchingEdges );

            m_vertexMap.remove( v ); // remove the vertex itself

            return true;
        }
        else {
            return false;
        }
    }


    /**
     * @see java.lang.Object#toString()
     */
    public String toString(  ) {
        // TODO: write a real javadoc
        return "(" + vertexSet(  ).toString(  ) + ", "
        + edgeSet(  ).toString(  ) + ")";
    }


    /**
     * @see Graph#vertexSet()
     */
    public Set vertexSet(  ) {
        if( m_unmodifiableVertexSet == null ) {
            m_unmodifiableVertexSet =
                Collections.unmodifiableSet( m_vertexMap.keySet(  ) );
        }

        return m_unmodifiableVertexSet;
    }


    private void addAllEdgeClones( Set edgeSet ) {
        for( Iterator iter = edgeSet.iterator(  ); iter.hasNext(  ); ) {
            Edge e = (Edge) iter.next(  );
            addEdge( (Edge) e.clone(  ) );
        }
    }


    private void assertCompatibleWithEdgeFactory( Edge e ) {
        if( e == null ) {
            throw new NullPointerException(  );
        }
        else if( !m_factoryEdgeClass.isInstance( e ) ) {
            throw new ClassCastException( "incompatible edge class" );
        }
    }


    private boolean removeAllEdges( Edge[] edges ) {
        boolean modified = false;

        for( int i = 0; i < edges.length; i++ ) {
            modified |= removeEdge( edges[ i ] );
        }

        return modified;
    }

    /**
     * .
     *
     * @author Barak Naveh
     * @version 1.0
     */
    private abstract class Specifics {
        /**
         * .
         *
         * @param sourceVertex
         * @param targetVertex
         *
         * @return
         */
        public abstract List getAllEdges( Object sourceVertex,
            Object targetVertex );


        /**
         * .
         *
         * @param sourceVertex
         * @param targetVertex
         *
         * @return
         */
        public abstract Edge getEdge( Object sourceVertex, Object targetVertex );


        /**
         * Adds the specified edge to the edge containers of its source and
         * target vertices.
         *
         * @param e
         */
        public abstract void addEdgeToTouchingVertices( Edge e );


        /**
         * .
         *
         * @param vertex
         *
         * @return
         */
        public abstract int degreeOf( Object vertex );


        /**
         * .
         *
         * @param vertex
         *
         * @return
         */
        public abstract List edgesOf( Object vertex );


        /**
         * .
         *
         * @param vertex
         *
         * @return
         */
        public abstract int inDegreeOf( Object vertex );


        /**
         * .
         *
         * @param vertex
         *
         * @return
         */
        public abstract List incomingEdgesOf( Object vertex );


        /**
         * .
         *
         * @param vertex
         *
         * @return
         */
        public abstract int outDegreeOf( Object vertex );


        /**
         * .
         *
         * @param vertex
         *
         * @return
         */
        public abstract List outgoingEdgesOf( Object vertex );


        /**
         * Removes the specified edge from the edge containers of its source
         * and target vertices.
         *
         * @param e
         */
        public abstract void removeEdgeFromTouchingVertices( Edge e );
    }


    /**
     * A container of for vertex edges.
     * 
     * <p>
     * In this edge container we use array lists to minimize memory toll.
     * However, for high-degree vertices we replace the entire edge container
     * with a direct access subclass (to be implemented).
     * </p>
     *
     * @author Barak Naveh
     */
    private static class DirectedEdgeContainer {
        List                   m_incoming             = new ArrayList( 1 );
        List                   m_outgoing             = new ArrayList( 1 );
        private transient List m_unmodifiableIncoming = null;
        private transient List m_unmodifiableOutgoing = null;

        /**
         * A lazy build of unmodifiable incoming edge list.
         *
         * @return
         */
        public List getUnmodifiableIncomingEdges(  ) {
            if( m_unmodifiableIncoming == null ) {
                m_unmodifiableIncoming =
                    Collections.unmodifiableList( m_incoming );
            }

            return m_unmodifiableIncoming;
        }


        /**
         * A lazy build of unmodifiable outgoing edge list.
         *
         * @return
         */
        public List getUnmodifiableOutgoingEdges(  ) {
            if( m_unmodifiableOutgoing == null ) {
                m_unmodifiableOutgoing =
                    Collections.unmodifiableList( m_outgoing );
            }

            return m_unmodifiableOutgoing;
        }


        /**
         * .
         *
         * @param e
         */
        public void addIncomingEdge( Edge e ) {
            m_incoming.add( e );
        }


        /**
         * .
         *
         * @param e
         */
        public void addOutgoingEdge( Edge e ) {
            m_outgoing.add( e );
        }


        /**
         * .
         *
         * @param e
         */
        public void removeIncomingEdge( Edge e ) {
            m_incoming.remove( e );
        }


        /**
         * .
         *
         * @param e
         */
        public void removeOutgoingEdge( Edge e ) {
            m_outgoing.remove( e );
        }
    }


    /**
     * .
     *
     * @author Barak Naveh
     * @version 1.0
     */
    private class DirectedSpecifics extends Specifics {
        private static final String NOT_IN_DIRECTED_GRAPH =
            "no such operation in a directed graph";

        /**
         * @see Graph#getAllEdges(Object, Object)
         */
        public List getAllEdges( Object sourceVertex, Object targetVertex ) {
            List edges = null;

            if( containsVertex( sourceVertex )
                    && containsVertex( targetVertex ) ) {
                edges = new ArrayList(  );

                DirectedEdgeContainer ec = getEdgeContainer( sourceVertex );

                Iterator              iter = ec.m_outgoing.iterator(  );

                while( iter.hasNext(  ) ) {
                    Edge e = (Edge) iter.next(  );

                    if( e.getTarget(  ).equals( targetVertex ) ) {
                        edges.add( e );
                    }
                }
            }

            return edges;
        }


        /**
         * @see Graph#getEdge(Object, Object)
         */
        public Edge getEdge( Object sourceVertex, Object targetVertex ) {
            if( containsVertex( sourceVertex )
                    && containsVertex( targetVertex ) ) {
                DirectedEdgeContainer ec = getEdgeContainer( sourceVertex );

                Iterator              iter = ec.m_outgoing.iterator(  );

                while( iter.hasNext(  ) ) {
                    Edge e = (Edge) iter.next(  );

                    if( e.getTarget(  ).equals( targetVertex ) ) {
                        return e;
                    }
                }
            }

            return null;
        }


        /**
         * @see AbstractBaseGraph#addEdgeToTouchingVertices(Edge)
         */
        public void addEdgeToTouchingVertices( Edge e ) {
            Object source = e.getSource(  );
            Object target = e.getTarget(  );

            getEdgeContainer( source ).addOutgoingEdge( e );

            if( source != target ) {
                getEdgeContainer( target ).addIncomingEdge( e );
            }
        }


        /**
         * @see UndirectedGraph#degreeOf(Object)
         */
        public int degreeOf( Object vertex ) {
            throw new UnsupportedOperationException( NOT_IN_DIRECTED_GRAPH );
        }


        /**
         * @see Graph#edgesOf(Object)
         */
        public List edgesOf( Object vertex ) {
            ArrayList inAndOut =
                new ArrayList( getEdgeContainer( vertex ).m_incoming );
            inAndOut.addAll( getEdgeContainer( vertex ).m_outgoing );

            return Collections.unmodifiableList( inAndOut );
        }


        /**
         * @see DirectedGraph#inDegree(Object)
         */
        public int inDegreeOf( Object vertex ) {
            return getEdgeContainer( vertex ).m_incoming.size(  );
        }


        /**
         * @see DirectedGraph#incomingEdges(Object)
         */
        public List incomingEdgesOf( Object vertex ) {
            return getEdgeContainer( vertex ).getUnmodifiableIncomingEdges(  );
        }


        /**
         * @see DirectedGraph#outDegree(Object)
         */
        public int outDegreeOf( Object vertex ) {
            return getEdgeContainer( vertex ).m_outgoing.size(  );
        }


        /**
         * @see DirectedGraph#outgoingEdges(Object)
         */
        public List outgoingEdgesOf( Object vertex ) {
            return getEdgeContainer( vertex ).getUnmodifiableOutgoingEdges(  );
        }


        /**
         * @see AbstractBaseGraph#removeEdgeFromTouchingVertices(Edge)
         */
        public void removeEdgeFromTouchingVertices( Edge e ) {
            Object source = e.getSource(  );
            Object target = e.getTarget(  );

            getEdgeContainer( source ).removeOutgoingEdge( e );

            if( source != target ) {
                getEdgeContainer( target ).removeIncomingEdge( e );
            }
        }


        /**
         * A lazy build of edge container for specified vertex.
         *
         * @param vertex a vertex in this graph.
         *
         * @return EdgeContainer
         */
        private DirectedEdgeContainer getEdgeContainer( Object vertex ) {
            assertVertexExist( vertex );

            DirectedEdgeContainer ec =
                (DirectedEdgeContainer) m_vertexMap.get( vertex );

            if( ec == null ) {
                ec = new DirectedEdgeContainer(  );
                m_vertexMap.put( vertex, ec );
            }

            return ec;
        }
    }


    /**
     * A container of for vertex edges.
     * 
     * <p>
     * In this edge container we use array lists to minimize memory toll.
     * However, for high-degree vertices we replace the entire edge container
     * with a direct access subclass (to be implemented).
     * </p>
     *
     * @author Barak Naveh
     */
    private static class UndirectedEdgeContainer {
        List                   m_vertexEdges             = new ArrayList( 1 );
        private transient List m_unmodifiableVertexEdges = null;

        /**
         * A lazy build of unmodifiable list of vertex edges
         *
         * @return
         */
        public List getUnmodifiableVertexEdges(  ) {
            if( m_unmodifiableVertexEdges == null ) {
                m_unmodifiableVertexEdges =
                    Collections.unmodifiableList( m_vertexEdges );
            }

            return m_unmodifiableVertexEdges;
        }


        /**
         * .
         *
         * @param e
         */
        public void addEdge( Edge e ) {
            m_vertexEdges.add( e );
        }


        /**
         * .
         *
         * @return
         */
        public int edgeCount(  ) {
            return m_vertexEdges.size(  );
        }


        /**
         * .
         *
         * @param e
         */
        public void removeEdge( Edge e ) {
            m_vertexEdges.remove( e );
        }
    }


    /**
     * .
     *
     * @author Barak Naveh
     * @version 1.0
     */
    private class UndirectedSpecifics extends Specifics {
        private static final String NOT_IN_UNDIRECTED_GRAPH =
            "no such operation in an undirected graph";

        /**
         * @see Graph#getAllEdges(Object, Object)
         */
        public List getAllEdges( Object sourceVertex, Object targetVertex ) {
            List edges = null;

            if( containsVertex( sourceVertex )
                    && containsVertex( targetVertex ) ) {
                edges = new ArrayList(  );

                Iterator iter =
                    getEdgeContainer( sourceVertex ).m_vertexEdges.iterator(  );

                while( iter.hasNext(  ) ) {
                    Edge    e = (Edge) iter.next(  );

                    boolean equalStraight =
                        sourceVertex.equals( e.getSource(  ) )
                        && targetVertex.equals( e.getTarget(  ) );

                    boolean equalInverted =
                        sourceVertex.equals( e.getTarget(  ) )
                        && targetVertex.equals( e.getSource(  ) );

                    if( equalStraight || equalInverted ) {
                        edges.add( e );
                    }
                }
            }

            return edges;
        }


        /**
         * @see Graph#getEdge(Object, Object)
         */
        public Edge getEdge( Object sourceVertex, Object targetVertex ) {
            if( containsVertex( sourceVertex )
                    && containsVertex( targetVertex ) ) {
                Iterator iter =
                    getEdgeContainer( sourceVertex ).m_vertexEdges.iterator(  );

                while( iter.hasNext(  ) ) {
                    Edge    e = (Edge) iter.next(  );

                    boolean equalStraight =
                        sourceVertex.equals( e.getSource(  ) )
                        && targetVertex.equals( e.getTarget(  ) );

                    boolean equalInverted =
                        sourceVertex.equals( e.getTarget(  ) )
                        && targetVertex.equals( e.getSource(  ) );

                    if( equalStraight || equalInverted ) {
                        return e;
                    }
                }
            }

            return null;
        }


        /**
         * @see AbstractBaseGraph#addEdgeToTouchingVertices(Edge)
         */
        public void addEdgeToTouchingVertices( Edge e ) {
            Object source = e.getSource(  );
            Object target = e.getTarget(  );

            getEdgeContainer( source ).addEdge( e );

            if( source != target ) {
                getEdgeContainer( target ).addEdge( e );
            }
        }


        /**
         * @see UndirectedGraph#degree(Object)
         */
        public int degreeOf( Object vertex ) {
            if( m_loopsAllowed ) { // then we must count, and add loops twice

                int  degree = 0;
                List edges = getEdgeContainer( vertex ).m_vertexEdges;

                for( Iterator iter = edges.iterator(  ); iter.hasNext(  ); ) {
                    Edge e = (Edge) iter.next(  );

                    if( e.getSource(  ).equals( e.getTarget(  ) ) ) {
                        degree += 2;
                    }
                    else {
                        degree += 1;
                    }
                }

                return degree;
            }
            else {
                return getEdgeContainer( vertex ).edgeCount(  );
            }
        }


        /**
         * @see Graph#edges(Object)
         */
        public List edgesOf( Object vertex ) {
            return getEdgeContainer( vertex ).getUnmodifiableVertexEdges(  );
        }


        /**
         * @see DirectedGraph#inDegreeOf(Object)
         */
        public int inDegreeOf( Object vertex ) {
            throw new UnsupportedOperationException( NOT_IN_UNDIRECTED_GRAPH );
        }


        /**
         * @see DirectedGraph#incomingEdgesOf(Object)
         */
        public List incomingEdgesOf( Object vertex ) {
            throw new UnsupportedOperationException( NOT_IN_UNDIRECTED_GRAPH );
        }


        /**
         * @see DirectedGraph#outDegreeOf(Object)
         */
        public int outDegreeOf( Object vertex ) {
            throw new UnsupportedOperationException( NOT_IN_UNDIRECTED_GRAPH );
        }


        /**
         * @see DirectedGraph#outgoingEdgesOf(Object)
         */
        public List outgoingEdgesOf( Object vertex ) {
            throw new UnsupportedOperationException( NOT_IN_UNDIRECTED_GRAPH );
        }


        /**
         * @see AbstractBaseGraph#removeEdgeFromTouchingVertices(Edge)
         */
        public void removeEdgeFromTouchingVertices( Edge e ) {
            Object source = e.getSource(  );
            Object target = e.getTarget(  );

            getEdgeContainer( source ).removeEdge( e );

            if( source != target ) {
                getEdgeContainer( target ).removeEdge( e );
            }
        }


        /**
         * A lazy build of edge container for specified vertex.
         *
         * @param vertex a vertex in this graph.
         *
         * @return EdgeContainer
         */
        private UndirectedEdgeContainer getEdgeContainer( Object vertex ) {
            assertVertexExist( vertex );

            UndirectedEdgeContainer ec =
                (UndirectedEdgeContainer) m_vertexMap.get( vertex );

            if( ec == null ) {
                ec = new UndirectedEdgeContainer(  );
                m_vertexMap.put( vertex, ec );
            }

            return ec;
        }
    }
}
