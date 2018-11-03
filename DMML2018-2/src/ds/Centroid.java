package ds;

import java.util.List;

/**
 * @author sahil
 * This class provides a structure to store the centroid
 * of a cluster of data points along with an ID for
 * distinguishing between centroids.
 *
 */
public class Centroid {

	private int id;
	private List<Wount> coordinates;
	
	/************************* *************************/
	
	/**
	 * Constructor
	 */
	public Centroid(int id, List<Wount> coordinates) {
		this.id = id;
		this.coordinates = coordinates;
	}
	
	/************************* *************************/
	
	/**
	 * @description Getter functions.
	 */
	public int getId() { return this.id; }
	public List<Wount> getCoordinates() { return this.coordinates; }
	
	/************************* *************************/
	
	/**
	 * @description Setter functions.
	 */
	public void setCoordinates(List<Wount> coordinates) { this.coordinates = coordinates; }
	
	/************************* *************************/
		
	/**
	 * @description divides the coordinates uniformly by the given constant
	 */
	public void divideCoordinates(double constant) {
		for (int i = 0; i < this.coordinates.size(); ++i) {
			coordinates.get(i).setCount(coordinates.get(i).getCount() / constant);
		}
	}
	
	/**
	 * @description returns the number of nonzero wounts
	 */
	public int getCoordinatesSize() {
		return this.coordinates.size();
	}
}
