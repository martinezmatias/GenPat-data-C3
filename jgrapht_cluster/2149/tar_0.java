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
/* -----------------
 * EdmondsKarpMaximumFlow.java
 * -----------------
 * (C) Copyright 2008-2008, by Ilya Razenshteyn and Contributors.
 *
 * Original Author:  Ilya Razenshteyn
 * Contributor(s):   -
 *
 * $Id$
 *
 * Changes
 * -------
 */
package org.jgrapht.alg;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.interfaces.MaximumFlowAlgorithm;

import java.util.*;


/**
 * A <a href = "http://en.wikipedia.org/wiki/Flow_network">flow network</a> is a
 * directed graph where each edge has a capacity and each edge receives a flow.
 * The amount of flow on an edge can not exceed the capacity of the edge (note,
 * that all capacities must be non-negative). A flow must satisfy the
 * restriction that the amount of flow into a vertex equals the amount of flow
 * out of it, except when it is a source, which "produces" flow, or sink, which
 * "consumes" flow.
 *
 * <p>This class computes maximum flow in a network using <a href =
 * "http://en.wikipedia.org/wiki/Edmonds-Karp_algorithm">Edmonds-Karp
 * algorithm</a>. Be careful: for large networks this algorithm may consume
 * significant amount of time (its upper-bound complexity is O(VE^2), where V -
 * amount of vertices, E - amount of edges in the network).
 *
 * <p>For more details see Andrew V. Goldberg's <i>Combinatorial Optimization
 * (Lecture Notes)</i>.
 */
public final class EdmondsKarpMaximumFlow<V, E> implements MaximumFlowAlgorithm<V, E>
{
    /**
     * Default tolerance.
     */
    public static final double DEFAULT_EPSILON = 0.000000001;

    private DirectedGraph<V, E> network; // our network

    private double epsilon;     // tolerance (DEFAULT_EPSILON or user-defined)
    private int currentSource;  // current source vertex
    private int currentSink;    // current sink vertex
    private int numNodes;       // number of nodes in the network

    private Map<V, Integer> indexer; // mapping from vertices to their indexes
                                     // in the internal representation

    private List<Node> nodes; // internal representation of the network

    

    /**
     * Constructs <tt>MaximumFlow</tt> instance to work with <i>a copy of</i>
     * <tt>network</tt>. Current source and sink are set to <tt>null</tt>. If
     * <tt>network</tt> is weighted, then capacities are weights, otherwise all
     * capacities are equal to one. Doubles are compared using <tt>
     * DEFAULT_EPSILON</tt> tolerance.
     *
     * @param network network, where maximum flow will be calculated
     */
    public EdmondsKarpMaximumFlow(DirectedGraph<V, E> network)
    {
        this(network, DEFAULT_EPSILON);
    }

    /**
     * Constructs <tt>MaximumFlow</tt> instance to work with <i>a copy of</i>
     * <tt>network</tt>. Current source and sink are set to <tt>null</tt>. If
     * <tt>network</tt> is weighted, then capacities are weights, otherwise all
     * capacities are equal to one.
     *
     * @param network network, where maximum flow will be calculated
     * @param epsilon tolerance for comparing doubles
     */
    public EdmondsKarpMaximumFlow(DirectedGraph<V, E> network,
        double epsilon)
    {
        if (network == null) {
            throw new NullPointerException("network is null");
        }
        if (epsilon <= 0) {
            throw new IllegalArgumentException(
                "invalid epsilon (must be positive)");
        }
        for (E e : network.edgeSet()) {
            if (network.getEdgeWeight(e) < -epsilon) {
                throw new IllegalArgumentException(
                    "invalid capacity (must be non-negative)");
            }
        }

        this.network = network;
        this.epsilon = epsilon;

        currentSource = -1;
        currentSink = -1;

        buildInternalNetwork();
    }


    /**
     * Sets current source to <tt>source</tt>, current sink to <tt>sink</tt>,
     * then calculates maximum flow from <tt>source</tt> to <tt>sink</tt>. Note,
     * that <tt>source</tt> and <tt>sink</tt> must be vertices of the <tt>
     * network</tt> passed to the constructor, and they must be different.
     *
     * @param source source vertex
     * @param sink sink vertex
     */
    public MaximumFlow<V, E> buildMaximumFlow(V source, V sink)
    {
        if (!network.containsVertex(source)) {
            throw new IllegalArgumentException(
                "invalid source (null or not from this network)");
        }
        if (!network.containsVertex(sink)) {
            throw new IllegalArgumentException(
                "invalid sink (null or not from this network)");
        }

        if (source.equals(sink)) {
            throw new IllegalArgumentException("source is equal to sink");
        }

        currentSource = indexer.get(source);
        currentSink = indexer.get(sink);

        for (int i = 0; i < numNodes; i++) {
            for (Arc currentArc : nodes.get(i).outgoingArcs) {
                currentArc.flow = 0.0;
            }
        }

        final Map<E, Double> maxFlow = new HashMap<E, Double>();

        double maxFlowValue;

        for (;;) {
            breadthFirstSearch();

            if (!nodes.get(currentSink).visited) {
                maxFlowValue = 0.0;
                for (int i = 0; i < numNodes; i++) {
                    for (Arc currentArc : nodes.get(i).outgoingArcs) {
                        if (currentArc.head == currentSink)
                            maxFlowValue += currentArc.flow;

                        if (currentArc.prototype != null) {
                            maxFlow.put(
                                currentArc.prototype,
                                currentArc.flow);
                        }
                    }
                }
                break;
            }

            augmentFlow();
        }

        return new VerbatimMaximumFlow<V, E>(maxFlowValue, maxFlow);
    }


    // converting the original network into internal more convenient format
    private void buildInternalNetwork()
    {
        numNodes = network.vertexSet().size();
        nodes = new ArrayList<Node>();
        Iterator<V> it = network.vertexSet().iterator();
        indexer = new HashMap<V, Integer>();
        for (int i = 0; i < numNodes; i++) {
            V currentNode = it.next();
            nodes.add(new Node(currentNode));
            indexer.put(currentNode, i);
        }
        for (int i = 0; i < numNodes; i++) {
            V we = nodes.get(i).prototype;
            for (E e : network.outgoingEdgesOf(we)) {
                V he = network.getEdgeTarget(e);
                int j = indexer.get(he);
                Arc e1 = new Arc(i, j, network.getEdgeWeight(e), e);
                Arc e2 = new Arc(j, i, 0.0, null);
                e1.reversed = e2;
                e2.reversed = e1;
                nodes.get(i).outgoingArcs.add(e1);
                nodes.get(j).outgoingArcs.add(e2);
            }
        }
    }

    private void breadthFirstSearch()
    {
        for (int i = 0; i < numNodes; i++) {
            nodes.get(i).visited = false;
            nodes.get(i).lastArcs = null;
        }

        Queue<Integer> queue = new LinkedList<Integer>();
        queue.offer(currentSource);

        nodes.get(currentSource).visited = true;
        nodes.get(currentSource).flowAmount = Double.POSITIVE_INFINITY;

        nodes.get(currentSink).flowAmount = 0.0;

        boolean seenSink = false;

        while (queue.size() != 0) {
            int currentNode = queue.poll();

            for (Arc currentArc : nodes.get(currentNode).outgoingArcs) {
                if ((currentArc.flow + epsilon) < currentArc.capacity) {

                    Node currentArcHead = nodes.get(currentArc.head);

                    if (currentArc.head == currentSink) {

                        currentArcHead.visited = true;

                        if (currentArcHead.lastArcs == null)
                            currentArcHead.lastArcs = new ArrayList<Arc>();

                        currentArcHead.lastArcs.add(currentArc);

                        currentArcHead.flowAmount +=
                            Math.min(
                                nodes.get(currentNode).flowAmount,
                                currentArc.capacity - currentArc.flow);

                        seenSink = true;

                    } else if (!currentArcHead.visited) {

                        currentArcHead.visited = true;

                        currentArcHead.flowAmount =
                            Math.min(
                                nodes.get(currentNode).flowAmount,
                                currentArc.capacity - currentArc.flow);

                        currentArcHead.lastArcs = Collections.singletonList(currentArc);

                        if (!seenSink)
                            queue.add(currentArc.head);
                    }
                }
            }
        }
    }

    private void augmentFlow()
    {
        boolean[] seen = new boolean[nodes.size()];

        for (Arc lastArc : nodes.get(currentSink).lastArcs) {
            double deltaFlow =
                Math.min(
                    nodes.get(lastArc.tail).flowAmount,
                    lastArc.capacity - lastArc.flow);

            if (augmentFlowAlongInternal(deltaFlow, lastArc.tail, seen)) {
                lastArc.flow += deltaFlow;
                lastArc.reversed.flow -= deltaFlow;
            }

            // _DBG
            assert (lastArc.flow + DEFAULT_EPSILON <= lastArc.capacity);
        }
    }

    private boolean augmentFlowAlongInternal(double deltaFlow, int node, boolean[] seen) {
        if (node == currentSource)
            return true;
        if (seen[node])
            return false;

        seen[node] = true;

        Arc prevArc = nodes.get(node).lastArcs.get(0);
        if (augmentFlowAlongInternal(deltaFlow, prevArc.tail, seen)) {
            prevArc.flow += deltaFlow;
            prevArc.reversed.flow -= deltaFlow;
            return true;
        }

        return false;
    }

    /**
     * Returns current source vertex, or <tt>null</tt> if there was no <tt>
     * calculateMaximumFlow</tt> calls.
     *
     * @return current source
     */
    public V getCurrentSource()
    {
        if (currentSource == -1) {
            return null;
        }
        return nodes.get(currentSource).prototype;
    }

    /**
     * Returns current sink vertex, or <tt>null</tt> if there was no <tt>
     * calculateMaximumFlow</tt> calls.
     *
     * @return current sink
     */
    public V getCurrentSink()
    {
        if (currentSink == -1) {
            return null;
        }
        return nodes.get(currentSink).prototype;
    }

    

    // class used for internal representation of network
    class Node
    {
        V prototype; // corresponding node in the original network
        List<Arc> outgoingArcs = new ArrayList<Arc>(); // list of outgoing arcs
                                                       // in the residual
                                                       // network
        boolean visited;    // this mark is used during BFS to mark visited nodes
        List<Arc> lastArcs; // last arc(-s) in the shortest path
        double flowAmount;  // amount of flow, we are able to push here

        Node(
            V prototype)
        {
            this.prototype = prototype;
        }
    }

    // class used for internal representation of network
    class Arc
    {
        int tail; // "from"
        int head; // "to"
        double capacity; // capacity (can be zero)
        double flow; // current flow (can be negative)
        Arc reversed; // for each arc in the original network we are to create
                      // reversed arc
        E prototype; // corresponding edge in the original network, can be null,
                     // if it is reversed arc

        Arc(
            int tail,
            int head,
            double capacity,
            E prototype)
        {
            this.tail = tail;
            this.head = head;
            this.capacity = capacity;
            this.prototype = prototype;
        }
    }
}

// End EdmondsKarpMaximumFlow.java
