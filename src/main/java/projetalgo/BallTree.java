package projetalgo;

import java.util.List;

public class BallTree {
    private final BallTreeNode root;

    public BallTree(List<Stop> stops) {
        root = new BallTreeNode(stops);
    }

    public BallTreeNode getRoot() {
        return root;
    }

    public Stop findNearest(Stop target) {
        Stop nearest = findNearest(root, target, null, Double.MAX_VALUE);
        return nearest;
    }
    
    private Stop findNearest(BallTreeNode node, Stop target, Stop best, double bestDist) {
        if (node == null) {
            return best;
        }
        double centerDist = Coord.distance(target.getCoord(), node.center);
    
        // if the center of the node is further than the best distance, we can stop
        if (centerDist - node.radius > bestDist) {
            return best;
        }
    
        if (node.stops != null) {
            // it's a leaf node, check all stops
            for (Stop s : node.stops) {
                double dist = Coord.distance(target.getCoord(), s.getCoord());
                if (dist < bestDist) {
                    bestDist = dist;
                    best = s;
                }
            }
            return best;
        }
    
        // Otherwise we are in an internal node: deciding which child to explore first
        double distLeft = Coord.distance(target.getCoord(), node.leftChild.center);
        double distRight = Coord.distance(target.getCoord(), node.rightChild.center);
    
        BallTreeNode first = node.leftChild;
        BallTreeNode second = node.rightChild;
    
        if (distRight < distLeft) {
            first = node.rightChild;
            second = node.leftChild;
        }
    
        // Explore the closest one first
        best = findNearest(first, target, best, bestDist);

        bestDist = Coord.distance(target.getCoord(), best.getCoord());
    
        // Maybe the other one is closer
        if (Coord.distance(target.getCoord(), second.center) - second.radius < bestDist) {
            best = findNearest(second, target, best, bestDist);
        }
    
        return best;
    }
}
