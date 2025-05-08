package projetalgo;

import java.util.List;

public class BallTreeNode {
    public Coord center; // center of the ball
    public double radius; // radius of the ball
    public BallTreeNode leftChild; 
    public BallTreeNode rightChild;
    public List<Stop> stops; // leaves of the tree

    public BallTreeNode(final List<Stop> stops) {
        build(stops);
    }

    private void build(List<Stop> stops) {
        if (stops.size() <= 1) {
            this.stops = stops;
            if (!stops.isEmpty()) {
                this.center = stops.get(0).getCoord();
                this.radius = 0; // No radius for a single point because it is a leaf
            }
            return;
        }

        // Find two farthest points
        Stop p1 = stops.get(0);
        Stop p2 = farthestStop(p1, stops);
        Stop p3 = farthestStop(p2, stops);

        // Calculation of the center between p2 and p3
        this.center = new Coord(
            (p3.getCoord().lat() + p2.getCoord().lat()) / 2,
            (p3.getCoord().lon() + p2.getCoord().lon()) / 2);

        // Calculate the radius as the maximum distance from the center to any stop
        double maxDistance = 0;
        for (Stop s : stops) {
            maxDistance = Math.max(maxDistance, center.distanceTo(s.getCoord()));
        }
        this.radius = maxDistance;

        // Separate the stops into two groups
        stops.sort((a, b) -> Double.compare(Coord.distance(a.getCoord(), p1.getCoord()),
            Coord.distance(b.getCoord(), p1.getCoord())));
        int mid = stops.size() / 2;
        List<Stop> leftList = stops.subList(0, mid);
        List<Stop> rightList = stops.subList(mid, stops.size());

        this.leftChild = new BallTreeNode(leftList);
        this.rightChild = new BallTreeNode(rightList);
    }

    private Stop farthestStop(Stop from, List<Stop> stops) {
        Stop farthest = null;
        double maxDistance = -1;
        for (Stop s : stops) {
            double dist = from.getCoord().distanceTo(s.getCoord());
            if (dist > maxDistance) {
                maxDistance = dist;
                farthest = s;
            }
        }
        return farthest;
    }
}
