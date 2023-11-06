package navigatorlh.Model;

import org.graphstream.algorithm.AStar.DistanceCosts;
import org.graphstream.algorithm.AStar;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.algorithm.AStar.Costs;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.file.FileSinkDGS;
import org.graphstream.ui.view.Viewer;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.mapsforge.map.rendertheme.renderinstruction.Symbol;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalTime;
import java.time.Duration;
import java.text.DecimalFormat;
import java.math.BigDecimal;
import java.util.*;

import navigatorlh.Util.*;

public class GraphGenerator {

    private final Graph graph;
    private HashMap<String, ArrayList<String>> name_ID = new HashMap<>();
    public HashMap<String, ArrayList<String>> getName_ID() {
        return name_ID;
    }

    private HashMap<String, String> id_name_LiaData = new HashMap<>();
    private HashSet<String> lstIdNode = new HashSet<String>();
    private Quad quadTree;

    public GraphGenerator() {
        this.graph = new MultiGraph("Graph");
        File osmDonnees = new File("./app/src/main/resources/fichier_lehavre_routes_only.xml");
        File liaStops = new File("./app/src/main/resources/stops.txt");
        File liaStopsTime = new File("./app/src/main/resources/stop_times.txt");
        // Nourrir le graph, lstIDNode, id_name_LiaData et name_ID
        // Ajouter OSM avant d'ajouter Lia
        try {
            this.ajouterOSM(osmDonnees);
            this.ajouterLIA(liaStops, liaStopsTime);
        } catch (IOException | JDOMException e) {
            e.printStackTrace();
        }
    }

    public void ajouterOSM(File fichier) throws IOException, JDOMException {
        // In XML file, it indique that
        // <bounds minlon="0.00948" minlat="49.46350" maxlon="0.18720" maxlat="49.55410"
        // origin="0.48.3"/>
        quadTree = new Quad(new Point(0.00948, 49.46350), new Point(0.18720, 49.55410));
        DecimalFormat formatter = new DecimalFormat("#0.000000");
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(fichier);
        Element root = document.getRootElement();

        List<Element> nodes = root.getChildren("node");

        for (Element node : nodes) {
            String nodeId = node.getAttributeValue("id");

            String name = null;
            String public_transport = null;
            for (Element child : node.getChildren()) {
                if (child.getAttribute("k").getValue().equals("name") && child.getAttribute("v") != null) {
                    name = child.getAttributeValue("v");
                }
                if (child.getAttribute("k").getValue().equals("public_transport") && child.getAttribute("v") != null) {
                    public_transport = child.getAttributeValue("v");
                }
            }

            Node node1 = graph.addNode(nodeId);
            lstIdNode.add(nodeId);
            BigDecimal lat = new BigDecimal(node.getAttributeValue("lat"));
            BigDecimal lon = new BigDecimal(node.getAttributeValue("lon"));
            Double latDouble = Double.parseDouble(formatter.format(lat));
            Double lonDouble = Double.parseDouble(formatter.format(lon));

            node1.setAttribute("lat", latDouble);
            node1.setAttribute("lon", lonDouble);
            // ADD Node ID, lat, lon to Quad Tree
            quadTree.insert(new NodeQuad(new Point(lonDouble, latDouble), nodeId));

            if (name != null) {
                node1.setAttribute("name", name);
                if (public_transport != null) {
                    node1.setAttribute("public_transport", public_transport);
                    if (name_ID.containsKey(name)) {
                        name_ID.get(name).add(nodeId);

                    } else {
                        name_ID.put(name, new ArrayList<>());
                        name_ID.get(name).add(nodeId);
                        System.out.println(name + ":" + name_ID.get(name));

                    }
                }
            }
        }

        List<Element> ways = root.getChildren("way");
        for (Element way : ways) {
            List<Element> wayNodes = way.getChildren("nd");
            String wayId = way.getAttributeValue("id");

            for (int i = 0; i < wayNodes.size() - 1; i++) {
                Element nd = wayNodes.get(i);
                Element ndNext = wayNodes.get(i + 1);

                String ndId = nd.getAttributeValue("ref");
                String ndNextId = ndNext.getAttributeValue("ref");

                Node node1 = graph.getNode(ndId);
                Node node2 = graph.getNode(ndNextId);

                if (node1 == null && i - 1 >= 0) {
                    node1 = graph.getNode(wayNodes.get(i - 1).getAttributeValue("ref"));
                }

                if (node2 == null && i + 2 < wayNodes.size()) {
                    node2 = graph.getNode(wayNodes.get(i + 2).getAttributeValue("ref"));
                }

                if (node1 == null || node2 == null) {
                    continue;
                }

                org.graphstream.graph.Edge edge = graph.addEdge(wayId + "_" + i, node1, node2);
                double latA = (Double) edge.getNode0().getAttribute("lat");
                double lonA = (Double) edge.getNode0().getAttribute("lon");
                double latB = (Double) edge.getNode1().getAttribute("lat");
                double lonB = (Double) edge.getNode1().getAttribute("lon");

                // Calculate the distance using the Haversine formula
                double distance = haversine(latA, lonA, latB, lonB);
                double walkingSpeed = 1.4;
                // Calculate walking time in seconds
                double walkingTimeSeconds = distance / walkingSpeed;

                edge.setAttribute("weight", walkingTimeSeconds);
            }

        }
        // Connect Transport Station to normal Node
        int i = 0;
        for (String nameArret : name_ID.keySet()) {
            // Normally there are only two maximum in name_ID.get(nameArret)
            for (String idArret : name_ID.get(nameArret)) {
                Node nodeArret = graph.getNode(idArret);
                Double lonAr, latAr;
                lonAr = (Double) nodeArret.getAttribute("lon");
                latAr = (Double) nodeArret.getAttribute("lat");
                // Find ClosesNodes
                List<NodeQuad> lstNodeQuad = quadTree.findClosestNodes(new Point(lonAr, latAr), 1);
                for (NodeQuad nodeQuad : lstNodeQuad) {
                    String idNode = nodeQuad.getData();
                    Edge edge = graph.addEdge("TN_" + idArret + "_" + idNode + i, idArret, idNode);

                    // Calculate the weight
                    double latA = (Double) edge.getNode0().getAttribute("lat");
                    double lonA = (Double) edge.getNode0().getAttribute("lon");
                    double latB = (Double) edge.getNode1().getAttribute("lat");
                    double lonB = (Double) edge.getNode1().getAttribute("lon");

                    // Calculate the distance using the Haversine formula
                    double distance = haversine(latA, lonA, latB, lonB);
                    double walkingSpeed = 1.4;
                    // Calculate walking time in seconds
                    double walkingTimeSeconds = distance / walkingSpeed;

                    edge.setAttribute("weight", walkingTimeSeconds);
                    i++;
                }
            }
        }
    }

    public void ajouterLIA(File liaStops, File liaStopsTime) throws FileNotFoundException {
        Scanner scanner = new Scanner(liaStops);
        scanner.nextLine();
        // Read stops.txt
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            Scanner donnee = new Scanner(line);
            donnee.useDelimiter(",");

            String stopId = donnee.next();
            String stopCode = donnee.next();
            String stopName = donnee.next().replace("\"", "");
            String stopDesc = donnee.next();
            double stopLat = Double.parseDouble(donnee.next().trim());
            double stopLon = Double.parseDouble(donnee.next().trim());
            String zoneId = donnee.next();
            String stopUrl = donnee.next();
            String locationType = donnee.next();
            String parentStation = donnee.next();
            String stopCity = donnee.next();
            donnee.close();

            id_name_LiaData.put(stopId, stopName);

            Node node1 = graph.addNode(stopId);
            quadTree.insert(new NodeQuad(new Point(stopLon, stopLat), stopId));
            node1.setAttribute("lat", stopLat);
            node1.setAttribute("lon", stopLon);
            node1.setAttribute("name", stopName);
            List<NodeQuad> lstNodeQuad = quadTree.findClosestNodes(new Point(stopLon, stopLat), 1);
            for (NodeQuad nodeQuad : lstNodeQuad) {
                String idNode = nodeQuad.getData();
                Edge edge = graph.addEdge("LN_" + stopId + "_" + idNode, stopId, idNode);

                // Calculate the weight
                double latA = (Double) edge.getNode0().getAttribute("lat");
                double lonA = (Double) edge.getNode0().getAttribute("lon");
                double latB = (Double) edge.getNode1().getAttribute("lat");
                double lonB = (Double) edge.getNode1().getAttribute("lon");

                // Calculate the distance using the Haversine formula
                double distance = haversine(latA, lonA, latB, lonB);
                double walkingSpeed = 1.4;
                // Calculate walking time in seconds
                double walkingTimeSeconds = distance / walkingSpeed;

                edge.setAttribute("weight", walkingTimeSeconds);

            }
            name_ID.computeIfAbsent(stopName, k -> new ArrayList<>()).add(stopId);
        }
        scanner.close();
        // Read stops_time.txt
        scanner = new Scanner(liaStopsTime);
        scanner.nextLine();

        Node previousNode = null;
        int previousStopSequence = -1;
        String previousNodeArrivalTimeString = "";

        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            Scanner donnee = new Scanner(line);
            donnee.useDelimiter(",");

            String tripId = donnee.next();
            if (!tripId.endsWith("GSemHLia-L-Ma-J-V-00"))
                continue;

            String arrivalTimeString = donnee.next();
            String departureTimeString = donnee.next();
            String stopId = donnee.next();
            int stopSequence = Integer.parseInt(donnee.next());
            String pickupType = donnee.next();
            String dropOffType = donnee.next();
            String shapeDistTraveled = donnee.next();
            String timepoint = donnee.next();
            donnee.close(); // Close the scanner
            if (previousStopSequence == -1) {
                // Initialise previousNodeArrivalTimeString if the scanner is reading the
                // beginning of the file stop_times.txt
                previousNodeArrivalTimeString = arrivalTimeString;
            }
            // En cas Nouveau ligne de transport
            if (stopSequence < previousStopSequence) {
                //Update "previous" variable to current
                previousStopSequence = stopSequence;
                previousNodeArrivalTimeString = arrivalTimeString;
                previousNode = null;
            }

            if (previousNode == null) {
                previousNode = graph.getNode(stopId);
                // Nouveau ligne de transport, nous NE créons PAS edge entre previousNode et
                // node2

                continue;
            }

            Node node2 = graph.getNode(stopId);
            if (node2 == null) {
                continue;
            }

            // System.out.println("Lien creer entre " + previousNode.getAttribute("name") +
            // " et "
            // + node2.getAttribute("name"));
            Edge edge = graph.addEdge(tripId + "_" + stopSequence, previousNode, node2);
            edge.setAttribute("name", previousNode.getAttribute("name") + " - " + node2.getAttribute("name"));
            // Add Weight

            LocalTime previousNodeArrivalTime = LocalTime.parse(previousNodeArrivalTimeString);
            LocalTime arrivalTime = LocalTime.parse(arrivalTimeString);

            // Calculate the time gap in seconds
            Duration duration = Duration.between(previousNodeArrivalTime, arrivalTime);
            long secondsGap = duration.getSeconds();
            if (secondsGap < 0)
            {   
                // If the arrival time is the next day, we add 86400 seconds to the gap

                secondsGap = 86400 + secondsGap;
            }
            edge.setAttribute("weight", secondsGap);
            previousNode = node2;
            previousStopSequence = stopSequence;
            previousNodeArrivalTimeString = arrivalTimeString;
        }
        scanner.close();

    }

    public Node getNodeByLiaID(String stopID) {
        String nameNode = id_name_LiaData.get(stopID);
        String idNode = name_ID.get(nameNode).get(0);
        return this.graph.getNode(idNode);
    }

    public void afficher() {
        this.graph.setAttribute("ui.stylesheet", "edge { text-size: 12px; text-color: blue; text-alignment: under; }");
        System.setProperty("org.graphstream.ui", "swing");
        Viewer display = graph.display();
        display.disableAutoLayout();
    }

    public void sauvegarder() throws IOException {
        FileSinkDGS fileSinkDGS = new FileSinkDGS();
        fileSinkDGS.writeAll(this.graph, "./app/src/main/resources/graph.dgs");
    }

    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the Earth in kilometers

        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c * 1000; // Distance in meters
    }

    public static void main(String[] args) throws IOException, JDOMException {
        GraphGenerator graphGenerator = new GraphGenerator();
        graphGenerator.sauvegarder();

        // graphGenerator.findRoute("1189922134", "8165170497");

    }

    public Path findRoute(String depart, String arrive) {
        // AStar astar = new AStar(this.graph);
        // astar.compute(arrive, depart);
        // System.out.println(astar.getShortestPath());
        String nameDepart = name_ID.get(depart).get(0);
        String nameArrive = name_ID.get(arrive).get(0);
        Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, "weight");
        dijkstra.init(graph);
        dijkstra.setSource(graph.getNode(nameDepart));
        dijkstra.compute();
        System.out.println(dijkstra.getPath(graph.getNode(nameArrive)));
        return dijkstra.getPath(graph.getNode(nameArrive));
    }
}