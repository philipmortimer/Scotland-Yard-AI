package uk.ac.bris.cs.scotlandyard.ui.ai;


/**
 * Pair class that stores two variables
 * @param <T> The type of the first variable
 * @param <K> The type of the second variable
 */
public class Pair<T, K>{
    private T left;
    private K right;
    /**
     * Creates new value pair
     * @param left The first value
     * @param right The second value
     */
    public Pair(T left, K right){
        this.left = left;
        this.right = right;
    }

    /**
     * Gets the left-hand value
     * @return The value
     */
    public T getLeft(){return left;}

    /**
     * Gets the right-hand value
     * @return The value
     */
    public K getRight(){return right;}
}
