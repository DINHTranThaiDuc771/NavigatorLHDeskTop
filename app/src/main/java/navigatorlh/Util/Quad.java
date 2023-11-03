package navigatorlh.Util;

import java.util.*;
// Java Implementation of Quad Tree





public class Quad {
    private static final double UNIT_AREA = 0.001;
    Point topLeft;
    Point botRight;
    NodeQuad n;
    Quad topLeftTree;
    Quad topRightTree;
    Quad botLeftTree;
    Quad botRightTree;

    public Quad() {
        topLeft = new Point(0, 0);
        botRight = new Point(0, 0);
        n = null;
        topLeftTree = null;
        topRightTree = null;
        botLeftTree = null;
        botRightTree = null;
    }

    public Quad(Point topL, Point botR) {
        n = null;
        topLeftTree = null;
        topRightTree = null;
        botLeftTree = null;
        botRightTree = null;
        topLeft = topL;
        botRight = botR;
    }

    public void insert(NodeQuad node) {
        if (node == null) {
            return;
        }

        if (!inBoundary(node.pos)) {
            return;
        }
        // We are at a quad of unit area
        // We cannot subdivide this quad further
        if (Math.abs(topLeft.x - botRight.x) <= Quad.UNIT_AREA && Math.abs(topLeft.y - botRight.y) <= Quad.UNIT_AREA) {
            if (n == null) {
                n = node;
            }
            return;
        }

        if ((topLeft.x + botRight.x) / 2 >= node.pos.x) {
            // Indicates topLeftTree
            if ((topLeft.y + botRight.y) / 2 >= node.pos.y) {
                if (topLeftTree == null) {
                    topLeftTree = new Quad(
                            new Point(topLeft.x, topLeft.y),
                            new Point((topLeft.x + botRight.x) / 2, (topLeft.y + botRight.y) / 2));
                }
                topLeftTree.insert(node);
            }
            // Indicates botLeftTree

            else {
                if (botLeftTree == null) {
                    botLeftTree = new Quad(
                            new Point(topLeft.x, (topLeft.y + botRight.y) / 2),
                            new Point((topLeft.x + botRight.x) / 2, botRight.y));
                }
                botLeftTree.insert(node);
            }
        } else {
            // Indicates topRightTree
            if ((topLeft.y + botRight.y) / 2 >= node.pos.y) {
                if (topRightTree == null) {
                    topRightTree = new Quad(
                            new Point((topLeft.x + botRight.x) / 2, topLeft.y),
                            new Point(botRight.x, (topLeft.y + botRight.y) / 2));
                }
                topRightTree.insert(node);
            } else {
                // Indicates botRightTree
                if (botRightTree == null) {
                    botRightTree = new Quad(
                            new Point((topLeft.x + botRight.x) / 2, (topLeft.y + botRight.y) / 2),
                            new Point(botRight.x, botRight.y));
                }
                botRightTree.insert(node);
            }
        }
    }

    public NodeQuad search(Point p) {
        if (!inBoundary(p)) {
            return null;
        }

        if (n != null) {
            return n;
        }

        if ((topLeft.x + botRight.x) / 2 >= p.x) {
            if ((topLeft.y + botRight.y) / 2 >= p.y) {
                if (topLeftTree == null) {
                    return null;
                }
                return topLeftTree.search(p);
            } else {
                if (botLeftTree == null) {
                    return null;
                }
                return botLeftTree.search(p);
            }
        } else {
            if ((topLeft.y + botRight.y) / 2 >= p.y) {
                if (topRightTree == null) {
                    return null;
                }
                return topRightTree.search(p);
            } else {
                if (botRightTree == null) {
                    return null;
                }
                return botRightTree.search(p);
            }
        }
    }

    public boolean inBoundary(Point p) {
        return (p.x >= topLeft.x && p.x <= botRight.x && p.y >= topLeft.y && p.y <= botRight.y);
    }

    // Method to find the closest nodes to a given point
    public List<NodeQuad> findClosestNodes(Point target, int numberOfClosest) {
        List<NodeQuad> closestNodes = new ArrayList<>();
        findClosestNodesHelper(target, numberOfClosest, closestNodes);
        return closestNodes;
    }

    private void findClosestNodesHelper(Point target, int numberOfClosest, List<NodeQuad> closestNodes) {
        if (n != null) {
            closestNodes.add(n);
        }

        if (topLeftTree != null && topLeftTree.inBoundary(target)) {
            closestNodes.addAll(topLeftTree.findClosestNodes(target, numberOfClosest));
        }

        if (topRightTree != null && topRightTree.inBoundary(target)) {
            closestNodes.addAll(topRightTree.findClosestNodes(target, numberOfClosest));
        }

        if (botLeftTree != null && botLeftTree.inBoundary(target)) {
            closestNodes.addAll(botLeftTree.findClosestNodes(target, numberOfClosest));
        }

        if (botRightTree != null && botRightTree.inBoundary(target)) {
            closestNodes.addAll(botRightTree.findClosestNodes(target, numberOfClosest));
        }

        closestNodes.sort((node1, node2) -> {
            double dist1 = calculateDistance(node1.pos, target);
            double dist2 = calculateDistance(node2.pos, target);
            return Double.compare(dist1, dist2);
        });

        while (closestNodes.size() > numberOfClosest) {
            closestNodes.remove(closestNodes.size() - 1);
        }
    }

    private double calculateDistance(Point p1, Point p2) {
        double dx = p1.x - p2.x;
        double dy = p1.y - p2.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static void main(String[] args) {
        Quad center = new Quad(new Point(0, 0), new Point(8, 8));
        NodeQuad a = new NodeQuad(new Point(1, 1), "1");
        NodeQuad b = new NodeQuad(new Point(2, 5), "2");
        NodeQuad c = new NodeQuad(new Point(7, 6), "3");
        center.insert(a);
        center.insert(b);
        center.insert(c);

        System.out.println("NodeQuad a: " + center.search(new Point(1, 1)).data);
        System.out.println("NodeQuad b: " + center.search(new Point(2, 5)).data);
        System.out.println("NodeQuad c: " + center.search(new Point(7, 6)).data);
        System.out.println("Non-existing node: " + center.search(new Point(5, 5)));
    }
}
