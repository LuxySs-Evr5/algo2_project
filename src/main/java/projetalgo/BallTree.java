package projetalgo;

import java.util.ArrayList;
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
    
        // if the closest point of the ball (node) is further than the best known, we can stop
        // because we know it's too far
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

    public List<Stop> findStopsWithinRadius(Stop target, double radiusKm) {
        List<Stop> result = new ArrayList<>();
        findStopsWithinRadius(root, target, radiusKm, result);
        return result;
    }

    private void findStopsWithinRadius(BallTreeNode node, Stop target, double radiusKm, List<Stop> result) {
        if (node == null) {
            return;
        }

        double centerDist = Coord.distance(target.getCoord(), node.center);

        // if the closest point of the ball (node) is further than the radius, we can stop
        // because we know it's too far
        if (centerDist - node.radius > radiusKm) {
            return;
        }

        if (node.stops != null) {
            // It's a leaf node, check all stops
            for (Stop s : node.stops) {
                double dist = Coord.distance(target.getCoord(), s.getCoord());
                if (dist <= radiusKm) {
                    result.add(s);
                }
            }
            return;
        }

        // Otherwise we are in an internal node: we check both children if necessary
        if (node.leftChild != null) {
            double leftDist = Coord.distance(target.getCoord(), node.leftChild.center);
            // Check if the closest point of left child is within the radius
            if (leftDist - node.leftChild.radius <= radiusKm) {
                findStopsWithinRadius(node.leftChild, target, radiusKm, result);
            }
        }
        if (node.rightChild != null) {
            double rightDist = Coord.distance(target.getCoord(), node.rightChild.center);
            // Check if the closest point of right child is within the radius
            if (rightDist - node.rightChild.radius <= radiusKm) {
                findStopsWithinRadius(node.rightChild, target, radiusKm, result);
            }
        }        
    }    
}

