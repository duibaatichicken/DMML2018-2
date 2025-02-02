package ds;

/**
 * @author sahil
 * This class provides a way to catalogue a specific word
 * by its unique ID, and its word count in a document.
 *
 */
public class Wount {
	
	private int id;
	private double count;

	/************************* *************************/
	
	/**
	 * Constructor
	 */
	public Wount(int id, double count) {
		this.id = id;
		this.count = count;
	}
	
	/************************* *************************/
	
	/**
	 * @description Getter functions.
	 */
	public int getId() { return this.id; }
	public double getCount() { return this.count; }
	
	/************************* *************************/
	
	/**
	 * @description Setter functions.
	 */
	public void setId(int newID) { this.id = newID; }
	public void setCount(double newCount) { this.count = newCount; }

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "("+this.id+", "+this.count+")";
	}
}


