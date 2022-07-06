package uk.ac.bris.cs.scotlandyard.ui.ai;

/**
 * Used to store a node on the graph using Dijkstra's algorithm
 */
public class DijkstraNode implements Comparable<DijkstraNode>{
    private final Integer value;//The node ID
    private Integer tentDistance;//The tentative distance between this node and a fixed node
    public static final Integer DIST_SOURCE = 0;
    public static final Integer DIST_INFINITY = -1;

    /**
     * Creates a new node to be used in Dijkstra's algorithm
     * @param value The node ID
     * @param tentDistance The starting tentative distance
     */
    public DijkstraNode(Integer value, Integer tentDistance){
        this.value = value;
        this.tentDistance = tentDistance;
    }

    /**
     * Gets the node ID
     * @return The node ID
     */
    public Integer getValue(){
        return value;
    }

    /**
     * Gets the current tentative distance between this node and a fixed start node
     * @return The distance
     */
    public Integer getTentDistance(){
        return tentDistance;
    }

    /**
     * Sets the tentative distance
     * @param tentDistance The new distance
     */
    public void setTentDistance(Integer tentDistance){
        this.tentDistance = tentDistance;
    }


    @Override
    public int compareTo(DijkstraNode other){
        if(other.getTentDistance() == this.tentDistance) return 0;
        if(this.tentDistance == DIST_INFINITY) return 1;
        if(other.getTentDistance() == DIST_INFINITY) return -1;
        return this.tentDistance > other.getTentDistance() ? 1 : -1;
    }
}
