/**
 * Copyright © 2014-2015 Paolo Simonetto
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package main;

import ocotillo.geometry.Coordinates;
import ocotillo.geometry.Geom2D;
import ocotillo.geometry.Polygon;
import ocotillo.graph.Edge;
import ocotillo.graph.EdgeAttribute;
import ocotillo.graph.Graph;
import ocotillo.graph.GraphAttribute;
import ocotillo.graph.Node;
import ocotillo.graph.NodeAttribute;
import ocotillo.graph.StdAttribute;
import ocotillo.graph.StdAttribute.ControlPoints;
import ocotillo.graph.layout.LayoutXD;
import ocotillo.graph.layout.fdl.impred.Impred;
import ocotillo.graph.layout.fdl.impred.ImpredConstraint;
import ocotillo.graph.layout.fdl.impred.ImpredForce;
import ocotillo.graph.layout.fdl.impred.ImpredPostProcessing;
import ocotillo.graph.rendering.svg.SvgElement;
import ocotillo.graph.serialization.oco.OcoSaver;
import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JTextArea;

public abstract class SmoothingTest implements Runnable {

    public Graph graph;
    public JTextArea comment = new JTextArea();

    public double distance = 10.0;
    public boolean mov = true;
    public boolean ind = true;
    public boolean sep = false;
    public int iterations = 100;
    public boolean fullOptions = true;

    public abstract String getName();

    public abstract String getDescription();

    public abstract Graph getGraph();

    public abstract void reset();

    public static final Color red = new Color(255, 145, 145, 150);
    public static final Color yellow = new Color(251, 255, 170, 150);
    public static final Color green = new Color(145, 255, 145, 150);
    public static final Color teal = new Color(145, 255, 255, 150);
    public static final Color blue = new Color(145, 145, 255, 150);
    public static final Color purple = new Color(255, 145, 255, 150);
    public static final Color[] defaultColors = new Color[]{red, yellow, green, teal, blue, purple};

    private static List<List<Edge>> extractCurves(Graph graph) {
        List<List<Edge>> curves = new ArrayList<>();
        if (graph.subGraphs().isEmpty()) {
            curves.add(extractCurve(graph));
        }
        for (Graph subgraph : graph.subGraphs()) {
            curves.add(extractCurve(subgraph));
        }
        return curves;
    }

    private static List<Edge> extractCurve(Graph graph) {
        List<Edge> curve = new ArrayList<>();
        Edge currentEdge = graph.edges().iterator().next();
        Node startingNode = currentEdge.source();
        Node currentNode = startingNode;
        curve.add(currentEdge);
        while (currentEdge.otherEnd(currentNode) != startingNode) {
            currentNode = currentEdge.otherEnd(currentNode);
            List<Edge> inOutEdges = new ArrayList<>(graph.inOutEdges(currentNode));
            currentEdge = inOutEdges.get(0) != currentEdge ? inOutEdges.get(0) : inOutEdges.get(1);
            curve.add(currentEdge);
        }
        return curve;
    }

    private static List<Node> extractElements(Graph graph) {
        List<Node> elements = new ArrayList<>();
        for (Node node : graph.nodes()) {
            if (graph.degree(node) == 0) {
                elements.add(node);
            }
        }
        return elements;
    }

    private static NodeAttribute<Collection<Edge>> extractSurroundingEdges(Graph graph, boolean independentBoundaries) {
        Collection<Edge> allEdges = new ArrayList<>(graph.edges());
        NodeAttribute<Collection<Edge>> surroundingEdges = new NodeAttribute<>(allEdges);
        if (independentBoundaries) {
            surroundingEdges.setDefault(new ArrayList<Edge>());
        }
        for (Node node : graph.nodes()) {
            if (graph.degree(node) == 0) {
                surroundingEdges.set(node, allEdges);
            }
        }
        return surroundingEdges;
    }

    private static void duplicateSharedCurves(Graph graph) {
        Set<Node> originalCurveNodes = new HashSet<>();
        Set<Edge> originalCurveEdges = new HashSet<>();
        for (Graph subgraph : graph.subGraphs()) {
            List<Edge> curve = extractCurve(subgraph);
            Map<Node, Node> nodeMap = new HashMap<>();
            for (Edge edge : curve) {
                if (!nodeMap.containsKey(edge.source())) {
                    nodeMap.put(edge.source(), duplicateNode(subgraph, edge.source()));
                    originalCurveNodes.add(edge.source());
                }
                if (!nodeMap.containsKey(edge.target())) {
                    nodeMap.put(edge.target(), duplicateNode(subgraph, edge.target()));
                    originalCurveNodes.add(edge.target());
                }
                duplicateEdge(subgraph, edge, nodeMap);
                originalCurveEdges.add(edge);
            }
        }
        for (Edge edge : originalCurveEdges) {
            graph.forcedRemove(edge);
        }
        for (Node node : originalCurveNodes) {
            graph.forcedRemove(node);
        }
    }

    private static Node duplicateNode(Graph graph, Node node) {
        NodeAttribute<Coordinates> positions = graph.nodeAttribute(StdAttribute.nodePosition);
        NodeAttribute<Coordinates> sizes = graph.nodeAttribute(StdAttribute.nodeSize);
        Node clone = graph.newNode();
        positions.set(clone, new Coordinates(positions.get(node)));
        sizes.set(clone, new Coordinates(sizes.get(node)));
        return clone;
    }

    private static Edge duplicateEdge(Graph graph, Edge edge, Map<Node, Node> nodeMap) {
        EdgeAttribute<ControlPoints> edgePoints = graph.edgeAttribute(StdAttribute.edgePoints);
        Node newSource = nodeMap.get(edge.source());
        Node newTarget = nodeMap.get(edge.target());
        Edge clone = graph.newEdge(newSource, newTarget);
        edgePoints.set(clone, new ControlPoints(edgePoints.get(edge)));
        return clone;
    }

    private static void fillCurves(Graph graph, double boundaryWidth) {
        NodeAttribute<Color> nodeColor = graph.<Color>nodeAttribute(StdAttribute.color);
        for (Node node : graph.nodes()) {
            if (graph.degree(node) != 0) {
                nodeColor.set(node, new Color(0, 0, 0, 0));
            }
        }
        graph.<Color>edgeAttribute(StdAttribute.color).setDefault(new Color(0, 0, 0, 0));

        int clusterColorIdx = 0;
        if (graph.subGraphs().isEmpty()) {
            fillCurve(graph, yellow, boundaryWidth);
        }
        for (Graph subgraph : graph.subGraphs()) {
            if (subgraph.hasGraphAttribute(StdAttribute.color)) {
                fillCurve(subgraph, subgraph.<Color>graphAttribute(StdAttribute.color).get(), boundaryWidth);
            } else {
                fillCurve(subgraph, defaultColors[clusterColorIdx], boundaryWidth);
            }
            clusterColorIdx = (clusterColorIdx + 1) % defaultColors.length;
        }
    }

    private static void fillCurve(Graph graph, Color fillColor, double boundaryWidth) {
        Polygon polygon = extractPolygon(graph);
        GraphAttribute<String> attribute;
        if (graph.hasLocalGraphAttribute(StdAttribute.graphics)) {
            attribute = graph.graphAttribute(StdAttribute.graphics);
        } else {
            attribute = graph.newLocalGraphAttribute(StdAttribute.graphics, "");
        }
        double factor = 0.7;
        Color boundaryColor = new Color((int) (fillColor.getRed() * factor), (int) (fillColor.getGreen() * factor), (int) (fillColor.getBlue() * factor), fillColor.getAlpha());
        attribute.set((new SvgElement.SvgPolygon("", polygon, fillColor, boundaryWidth, boundaryColor)).toString());
    }

    private static List<Polygon> extractPolygons(Graph graph) {
        List<Polygon> polygons = new ArrayList<>();
        if (graph.subGraphs().isEmpty()) {
            polygons.add(extractPolygon(graph));
        }
        for (Graph subgraph : graph.subGraphs()) {
            polygons.add(extractPolygon(subgraph));
        }
        return polygons;
    }

    private static Polygon extractPolygon(Graph graph) {
        List<Edge> curve = extractCurve(graph);
        Polygon polygon = new Polygon();
        Node currentNode = curve.get(0).source();
        for (Edge edge : curve) {
            if (currentNode == edge.source()) {
                polygon.addAll(LayoutXD.edgePoints(edge, graph));
            } else {
                List<Coordinates> edgePoints = LayoutXD.edgePoints(edge, graph);
                Collections.reverse(edgePoints);
                polygon.addAll(edgePoints);
            }
            currentNode = edge.otherEnd(currentNode);
        }
        return polygon;
    }

    private static Impred getImpred(Graph graph, double optimalDistance, boolean movableElements, boolean independentBoundaries, boolean separateBoundaries) {
        return getImpred(graph, optimalDistance, movableElements, independentBoundaries, separateBoundaries, 1.0);
    }

    private static Impred getImpred(Graph graph, double optimalDistance, boolean movableElements, boolean independentBoundaries, boolean separateBoundaries, double factor) {
        Impred.ImpredBuilder builder = new Impred.ImpredBuilder(graph)
                .withForce(new ImpredForce.CurveSmoothing(extractCurves(graph)))
                .withForce(new ImpredForce.SelectedEdgeNodeRepulsion(optimalDistance, graph.edges(), extractElements(graph)))
                .withForce(new ImpredForce.EdgeAttraction(optimalDistance * 0.7))
                .withConstraint(new ImpredConstraint.DecreasingMaxMovement(optimalDistance))
                .withConstraint(new ImpredConstraint.MovementAcceleration(optimalDistance))
                .withConstraint(new ImpredConstraint.SurroundingEdges(extractSurroundingEdges(graph, independentBoundaries)))
                .withPostProcessing(new ImpredPostProcessing.FlexibleEdges(graph.edges(), optimalDistance * 1.45 * factor, optimalDistance * 1.5 * factor))
                .withPostProcessing(new RegionFiller(graph, optimalDistance / 10));

        if (movableElements) {
            builder.withForce(new ImpredForce.SelectedNodeNodeRepulsion(optimalDistance, extractElements(graph)));
        } else {
            builder.withConstraint(new ImpredConstraint.PinnedNodes(extractElements(graph)));
        }

        if (separateBoundaries) {
            builder.withForce(new ImpredForce.EdgeNodeRepulsion(optimalDistance / 15));
        }

        return builder.build();
    }

    public void mainRun() {
        Impred impred = getImpred(graph, distance, mov, ind, sep);
        impred.iterate(iterations);
    }

    private static class RegionFiller extends ImpredPostProcessing {

        private final Graph graph;
        private final double boundaryWidth;

        public RegionFiller(Graph graph, double boundaryWidth) {
            this.graph = graph;
            this.boundaryWidth = boundaryWidth;
        }

        @Override
        protected void execute() {
            fillCurves(graph, boundaryWidth);
        }

    }

    @Override
    public void run() {
        comment.setText("Statistics:\n");

        double sum = 0;
        List<Polygon> originalPolygons = extractPolygons(graph);
        for (Polygon polygon : originalPolygons) {
            sum += Geom2D.isoperimetricQuotient(polygon);
        }

        comment.append("Avarage isoperimetric quotient (initial):  " + sum / originalPolygons.size() + "\n");
        
        long startTime = System.nanoTime();
        mainRun();
        long stopTime = System.nanoTime();
        double seconds = (stopTime - startTime) / 1000000000.0;

        sum = 0;
        List<Polygon> finalPolygons = extractPolygons(graph);
        for (Polygon polygon : finalPolygons) {
            sum += Geom2D.isoperimetricQuotient(polygon);
        }

        comment.append("Avarage isoperimetric quotient (final):  " + sum / finalPolygons.size() + "\n");

        comment.append("Elapsed time (seconds):  " + String.format("%.3f%n", seconds) + "\n");

    }

    public static class TriangleSingle extends SmoothingTest {

        @Override
        public String getName() {
            return "Triangle single";
        }

        @Override
        public String getDescription() {
            return "Single smoothing iteration on a triangle.";
        }

        @Override
        public Graph getGraph() {
            graph = new Graph();
            Node a = graph.newNode();
            Node b = graph.newNode();
            Node c = graph.newNode();
            graph.newEdge(a, b);
            graph.newEdge(b, c);
            graph.newEdge(c, a);

            NodeAttribute<Coordinates> positions = graph.nodeAttribute(StdAttribute.nodePosition);
            positions.set(a, new Coordinates(0, 0));
            positions.set(b, new Coordinates(6, 10));
            positions.set(c, new Coordinates(12, 5));

            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);
                    
            iterations = 1;
            fullOptions = false;
        }

        @Override
        public void mainRun() {
            Impred impred = new Impred.ImpredBuilder(graph)
                    .withForce(new ImpredForce.CurveSmoothing(extractCurves(graph)))
                    .build();

            impred.iterate(iterations);
        }

    }

    public static class ConcaveSingle extends SmoothingTest {

        @Override
        public String getName() {
            return "Concave polygon single";
        }

        @Override
        public String getDescription() {
            return "Critical single smoothing iteration on a concave polygon.";
        }

        @Override
        public Graph getGraph() {
            graph = new Graph();
            Node a = graph.newNode();
            Node b = graph.newNode();
            Node c = graph.newNode();
            Node d = graph.newNode();
            Node e = graph.newNode();
            Node f = graph.newNode();
            graph.newEdge(a, b);
            graph.newEdge(b, c);
            graph.newEdge(c, d);
            graph.newEdge(d, e);
            graph.newEdge(e, f);
            graph.newEdge(f, a);

            NodeAttribute<Coordinates> positions = graph.nodeAttribute(StdAttribute.nodePosition);
            positions.set(a, new Coordinates(0, 0));
            positions.set(b, new Coordinates(20, -20));
            positions.set(f, new Coordinates(20, 20));
            positions.set(d, new Coordinates(4, 0));
            positions.set(c, new Coordinates(8, -4));
            positions.set(e, new Coordinates(8, 4));

            NodeAttribute<String> labels = graph.nodeAttribute(StdAttribute.label);
            labels.set(a, "a");
            labels.set(b, "b");
            labels.set(c, "c");
            labels.set(d, "d");
            labels.set(e, "e");
            labels.set(f, "f");

            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            iterations = 1;
            fullOptions = false;
        }

        @Override
        public void mainRun() {
            Impred impred = new Impred.ImpredBuilder(graph)
                    .withForce(new ImpredForce.CurveSmoothing(extractCurves(graph)))
                    .build();

            impred.iterate(iterations);
        }

    }

    public static class ConcaveFlexibleSingle extends SmoothingTest {

        @Override
        public String getName() {
            return "Concave polygon  flexible single";
        }

        @Override
        public String getDescription() {
            return "Critical single smoothing iteration on a concave polygon, solved by flexible edges.";
        }

        @Override
        public Graph getGraph() {
            graph = new Graph();
            Node a = graph.newNode();
            Node a1 = graph.newNode();
            Node a2 = graph.newNode();
            Node b = graph.newNode();
            Node c = graph.newNode();
            Node d = graph.newNode();
            Node e = graph.newNode();
            Node f = graph.newNode();
            graph.newEdge(a, a1);
            graph.newEdge(a1, b);
            graph.newEdge(b, c);
            graph.newEdge(c, d);
            graph.newEdge(d, e);
            graph.newEdge(e, f);
            graph.newEdge(f, a2);
            graph.newEdge(a2, a);

            NodeAttribute<Coordinates> positions = graph.nodeAttribute(StdAttribute.nodePosition);
            positions.set(a, new Coordinates(0, 0));
            positions.set(a1, new Coordinates(5, -5));
            positions.set(a2, new Coordinates(5, 5));
            positions.set(b, new Coordinates(20, -20));
            positions.set(f, new Coordinates(20, 20));
            positions.set(d, new Coordinates(4, 0));
            positions.set(c, new Coordinates(8, -4));
            positions.set(e, new Coordinates(8, 4));

            NodeAttribute<String> labels = graph.nodeAttribute(StdAttribute.label);
            labels.set(a, "a");
            labels.set(a1, "a1");
            labels.set(a2, "a2");
            labels.set(b, "b");
            labels.set(c, "c");
            labels.set(d, "d");
            labels.set(e, "e");
            labels.set(f, "f");

            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            iterations = 10;
            fullOptions = false;
        }

        @Override
        public void mainRun() {
            Impred impred = new Impred.ImpredBuilder(graph)
                    .withForce(new ImpredForce.CurveSmoothing(extractCurves(graph)))
                    .withPostProcessing(new ImpredPostProcessing.MinIterationTime(500))
                    .build();

            impred.iterate(iterations);
        }

    }

    public static class SpiralFD extends SmoothingTest {

        @Override
        public String getName() {
            return "FD flexible spiral";
        }

        @Override
        public String getDescription() {
            return "Force-directed spiral with flexible edges and no impred constraints.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/Spiral.oco"));
            fillCurves(graph, 2);

            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 13;
            iterations = 2500;
            fullOptions = false;
        }

        @Override
        public void mainRun() {
            Impred impred = new Impred.ImpredBuilder(graph)
                    .withForce(new ImpredForce.CurveSmoothing(extractCurves(graph)))
                    .withPostProcessing(new ImpredPostProcessing.FlexibleEdges(graph.edges(), distance * 1.45, distance * 1.5))
                    .withPostProcessing(new ImpredPostProcessing.MinIterationTime(20))
                    .withPostProcessing(new RegionFiller(graph, 1.5))
                    .build();

            impred.iterate(iterations);
        }

    }

    public static class SpiralConstrFD extends SmoothingTest {

        @Override
        public String getName() {
            return "FD flexible constr. spiral";
        }

        @Override
        public String getDescription() {
            return "Force-directed spiral with impred constraints and flexible edges.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/Spiral.oco"));
            fillCurves(graph, 2);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 13;
            iterations = 2500;
            fullOptions = false;
        }

        @Override
        public void mainRun() {
            NodeAttribute<Collection<Edge>> surroundingEdges = new NodeAttribute<>(graph.edges());

            Impred impred = new Impred.ImpredBuilder(graph)
                    .withForce(new ImpredForce.CurveSmoothing(extractCurves(graph)))
                    .withConstraint(new ImpredConstraint.DecreasingMaxMovement(distance * 1.5))
                    .withConstraint(new ImpredConstraint.SurroundingEdges(surroundingEdges))
                    .withPostProcessing(new ImpredPostProcessing.FlexibleEdges(graph.edges(), distance * 1.45, distance * 1.5))
                    .withPostProcessing(new ImpredPostProcessing.MinIterationTime(20))
                    .withPostProcessing(new RegionFiller(graph, 1.5))
                    .build();

            impred.iterate(iterations);
        }

    }

    public static class AmebaFD extends SmoothingTest {

        @Override
        public String getName() {
            return "FD flexible ameba";
        }

        @Override
        public String getDescription() {
            return "Force-directed ameba with flexible edges and no impred constraints.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/Ameba.oco"));
            fillCurves(graph, 2);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 13;
            iterations = 2500;
            fullOptions = false;
        }

        @Override
        public void mainRun() {
            Impred impred = new Impred.ImpredBuilder(graph)
                    .withForce(new ImpredForce.CurveSmoothing(extractCurves(graph)))
                    .withPostProcessing(new ImpredPostProcessing.FlexibleEdges(graph.edges(), distance * 1.45, distance * 1.5))
                    .withPostProcessing(new RegionFiller(graph, 1.5))
                    .build();

            impred.iterate(iterations);
        }

    }

    public static class AmebaConstrFD extends SmoothingTest {

        @Override
        public String getName() {
            return "FD flexible constr. ameba";
        }

        @Override
        public String getDescription() {
            return "Force-directed ameba with impred constraints and flexible edges.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/Ameba.oco"));
            fillCurves(graph, 2);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 13;
            iterations = 2500;
            fullOptions = false;
        }

        @Override
        public void mainRun() {
            NodeAttribute<Collection<Edge>> surroundingEdges = new NodeAttribute<>(graph.edges());

            Impred impred = new Impred.ImpredBuilder(graph)
                    .withForce(new ImpredForce.CurveSmoothing(extractCurves(graph)))
                    .withConstraint(new ImpredConstraint.DecreasingMaxMovement(20))
                    .withConstraint(new ImpredConstraint.SurroundingEdges(surroundingEdges))
                    .withPostProcessing(new ImpredPostProcessing.FlexibleEdges(graph.edges(), distance * 1.45, distance * 1.5))
                    .withPostProcessing(new RegionFiller(graph, 1.5))
                    .build();

            impred.iterate(iterations);
        }
    }

    public static class SingleSetFixed extends SmoothingTest {

        @Override
        public String getName() {
            return "Single set, fixed elements";
        }

        @Override
        public String getDescription() {
            return "Euler diagram with a single set where the elements cannot move.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/SingleSet.oco"));
            fillCurves(graph, 1.5);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 15;
            mov = false;
            ind = false;
            sep = false;
            iterations = 120;
        }
    }

    public static class SingleSetMoveable extends SmoothingTest {

        @Override
        public String getName() {
            return "Single set, movable elements";
        }

        @Override
        public String getDescription() {
            return "Euler diagram with a single set where the elements can move.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/SingleSet.oco"));
            fillCurves(graph, 1.5);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 15;
            mov = true;
            ind = false;
            sep = false;
            iterations = 120;
        }
    }

    public static class DoubleSetFixed extends SmoothingTest {

        @Override
        public String getName() {
            return "Double set, fixed elements";
        }

        @Override
        public String getDescription() {
            return "Euler diagram with two sets where the elements cannot move.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/DoubleSet.oco"));
            fillCurves(graph, 1.5);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 15;
            mov = false;
            ind = false;
            sep = false;
            iterations = 120;
        }
    }

    public static class DoubleSetMoveable extends SmoothingTest {

        @Override
        public String getName() {
            return "Double set, movable elements";
        }

        @Override
        public String getDescription() {
            return "Euler diagram with two sets where the elements can move.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/DoubleSet.oco"));
            fillCurves(graph, 1.5);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 15;
            mov = true;
            ind = false;
            sep = false;
            iterations = 120;
        }
    }

    public static class Imdb20Fixed extends SmoothingTest {

        @Override
        public String getName() {
            return "Imdb20 (fig. 9a), fixed";
        }

        @Override
        public String getDescription() {
            return "Imdb20 diagram where the elements cannot move.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/Imdb20.oco"));
            duplicateSharedCurves(graph);
            fillCurves(graph, 0.7);
            return graph;
        }

        @Override
        public void reset() {
             graph = getGraph();
            comment.setText(null);

            distance = 7;
            mov = false;
            ind = false;
            sep = false;
            iterations = 25;
        }
    }

    public static class Imdb20FixedIndep extends SmoothingTest {

        @Override
        public String getName() {
            return "Imdb20 (fig. 9a), fixed, indep";
        }

        @Override
        public String getDescription() {
            return "Imdb20 diagram where the elements cannot move and boundaries are independent.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/Imdb20.oco"));
            duplicateSharedCurves(graph);
            fillCurves(graph, 0.7);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 7;
            mov = false;
            ind = true;
            sep = false;
            iterations = 40;
        }
    }

    public static class Imdb20Moveable extends SmoothingTest {

        @Override
        public String getName() {
            return "Imdb20 (fig. 9a), movable";
        }

        @Override
        public String getDescription() {
            return "Imdb20 diagram where the elements can move.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/Imdb20.oco"));
            duplicateSharedCurves(graph);
            fillCurves(graph, 0.7);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 7;
            mov = true;
            ind = false;
            sep = false;
            iterations = 25;
        }
    }

    public static class Imdb20MoveableIndep extends SmoothingTest {

        @Override
        public String getName() {
            return "Imdb20 (fig. 9a), movable, indep";
        }

        @Override
        public String getDescription() {
            return "Imdb20 diagram where the elements can move and the boundaries are independent.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/Imdb20.oco"));
            duplicateSharedCurves(graph);
            fillCurves(graph, 0.7);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 7;
            mov = true;
            ind = true;
            sep = false;
            iterations = 40;
        }
    }

    public static class ManhattanBubble extends SmoothingTest {

        @Override
        public String getName() {
            return "Manhattan BubbleSets (fig. 9)";
        }

        @Override
        public String getDescription() {
            return "BubbleSets over manhattan map.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/ManhattanBubble.oco"));
            fillCurves(graph, 0.8);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 8;
            mov = false;
            ind = true;
            sep = false;
            iterations = 50;
        }
    }

    public static class UntangledFixed extends SmoothingTest {

        @Override
        public String getName() {
            return "Untangled diagrams (fig. 1a), fixed elements";
        }

        @Override
        public String getDescription() {
            return "Untangled Euler diagrams with fixed elements.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/Untangled.oco"));
            fillCurves(graph, 1.0);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 10;
            mov = false;
            ind = true;
            sep = false;
            iterations = 100;
        }
    }

    public static class Untangled extends SmoothingTest {

        @Override
        public String getName() {
            return "Untangled diagram (fig. 1a)";
        }

        @Override
        public String getDescription() {
            return "Untangled Euler diagram.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/Untangled.oco"));
            fillCurves(graph, 1.0);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 10;
            mov = true;
            ind = true;
            sep = false;
            iterations = 100;
        }
    }

    public static class UntangledSmall extends SmoothingTest {

        @Override
        public String getName() {
            return "Untangled diagram small (fig. 5b)";
        }

        @Override
        public String getDescription() {
            return "Untangled Euler diagram.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/Untangled_small.oco"));
            fillCurves(graph, 1.0);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 10;
            mov = true;
            ind = true;
            sep = false;
            iterations = 100;
        }
    }

    public static class GeneralEuler extends SmoothingTest {

        @Override
        public String getName() {
            return "General Euler (fig. 6b)";
        }

        @Override
        public String getDescription() {
            return "Example from General Euler representation.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/GeneralEuler.oco"));
            fillCurves(graph, 1.7);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 17;
            mov = true;
            ind = false;
            sep = false;
            iterations = 300;
        }
    }

    public static class SetVisualizerFixed extends SmoothingTest {

        @Override
        public String getName() {
            return "Set Visualizer fixed";
        }

        @Override
        public String getDescription() {
            return "Example from Set Visualizer.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/SetVisualizer.oco"));
            fillCurves(graph, 1);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 10;
            mov = false;
            ind = true;
            sep = false;
            iterations = 50;
        }
    }

    public static class SetVisualizerMovable extends SmoothingTest {

        @Override
        public String getName() {
            return "Set Visualizer movable";
        }

        @Override
        public String getDescription() {
            return "Example from Set Visualizer.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/SetVisualizer.oco"));
            fillCurves(graph, 1);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 10;
            mov = true;
            ind = true;
            sep = false;
            iterations = 50;
        }
    }

    public static class EulerForce1 extends SmoothingTest {

        @Override
        public String getName() {
            return "Euler Force (Fig. 1a)";
        }

        @Override
        public String getDescription() {
            return "Example from Euler Force paper.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/EulerForce_1.oco"));
            fillCurves(graph, 1.2);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 12;
            mov = true;
            ind = false;
            sep = true;
            iterations = 600;
        }
    }

    public static class EulerForce2 extends SmoothingTest {

        @Override
        public String getName() {
            return "Euler Force (Fig. 1b)";
        }

        @Override
        public String getDescription() {
            return "Example from Euler Force paper.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/EulerForce_2.oco"));
            fillCurves(graph, 1.2);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 12;
            mov = true;
            ind = false;
            sep = true;
            iterations = 400;
        }
    }

    public static class EulerForce3sets extends SmoothingTest {

        private int i;

        public EulerForce3sets(int i) {
            this.i = i;
        }

        @Override
        public String getName() {
            int index = (i + 1) / 2;
            String suffix = i % 2 == 1 ? "" : "x";
            return "Euler Force - 3 sets - " + index + suffix;
        }

        @Override
        public String getDescription() {
            return i % 2 == 1 ? "Example from Euler force software" : "Euler force solution for the diagram above.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/EulerForce_3sets_" + i + ".oco"));
            fillCurves(graph, 1.5);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 15;
            mov = true;
            ind = false;
            sep = true;
            iterations = 350;
        }
    }

    public static class EulerForce4sets extends SmoothingTest {

        private int i;

        public EulerForce4sets(int i) {
            this.i = i;
        }

        @Override
        public String getName() {
            int index = (i + 1) / 2;
            String suffix = i % 2 == 1 ? "" : "x";
            return "Euler Force - 4 sets - " + index + suffix;
        }

        @Override
        public String getDescription() {
            return i % 2 == 1 ? "Example from Euler force software" : "Euler force solution for the diagram above.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/EulerForce_4sets_" + i + ".oco"));
            fillCurves(graph, 1.5);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 15;
            mov = true;
            ind = false;
            sep = true;
            iterations = 350;
        }
    }

    public static class EulerForce5sets extends SmoothingTest {

        private int i;

        public EulerForce5sets(int i) {
            this.i = i;
        }

        @Override
        public String getName() {
            int index = (i + 1) / 2;
            String suffix = i % 2 == 1 ? "" : "x";
            return "Euler Force - 5 sets - " + index + suffix;
        }

        @Override
        public String getDescription() {
            return i % 2 == 1 ? "Example from Euler force software" : "Euler force solution for the diagram above.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/EulerForce_5sets_" + i + ".oco"));
            fillCurves(graph, 1.5);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 15;
            mov = true;
            ind = false;
            sep = true;
            iterations = 500;
        }
    }

    public static class Euler3runtime extends SmoothingTest {

        private final double factor;

        public Euler3runtime(double factor) {
            this.factor = factor;
        }

        @Override
        public String getName() {
            return "Euler 3 - runtime - " + factor;
        }

        @Override
        public String getDescription() {
            return "Runtime for Euler 3 with scaled flexible edges.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/EulerForce_3sets_1.oco"));
            fillCurves(graph, 1.5);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 15;
            mov = true;
            ind = false;
            sep = true;
            iterations = 350;
        }

        @Override
        public void mainRun() {
            Impred impred = getImpred(graph, distance, mov, ind, sep, factor);
            impred.iterate(iterations);
        }
    }

    public static class Euler4runtime extends SmoothingTest {

        private final double factor;

        public Euler4runtime(double factor) {
            this.factor = factor;
        }

        @Override
        public String getName() {
            return "Euler 4 - runtime - " + factor;
        }

        @Override
        public String getDescription() {
            return "Runtime for Euler 4 with scaled flexible edges.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/EulerForce_4sets_1.oco"));
            fillCurves(graph, 1.5);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 15;
            mov = true;
            ind = false;
            sep = true;
            iterations = 350;
        }

        @Override
        public void mainRun() {
            Impred impred = getImpred(graph, distance, mov, ind, sep, factor);
            impred.iterate(iterations);
        }
    }

    public static class Euler5runtime extends SmoothingTest {

        private final double factor;

        public Euler5runtime(double factor) {
            this.factor = factor;
        }

        @Override
        public String getName() {
            return "Euler 5 - runtime - " + factor;
        }

        @Override
        public String getDescription() {
            return "Runtime for Euler 5 with scaled vlexible edges.";
        }

        @Override
        public Graph getGraph() {
            OcoSaver saver = new OcoSaver();
            graph = saver.readFile(new File("data/EulerForce_5sets_3.oco"));
            fillCurves(graph, 1.5);
            return graph;
        }

        @Override
        public void reset() {
            graph = getGraph();
            comment.setText(null);

            distance = 15;
            mov = true;
            ind = false;
            sep = true;
            iterations = 350;
        }

        @Override
        public void mainRun() {
            Impred impred = getImpred(graph, distance, mov, ind, sep, factor);
            impred.iterate(iterations);
        }
    }

}
