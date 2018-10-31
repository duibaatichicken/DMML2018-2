package clust;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ds.Centroid;
import ds.Wount;

public class KMeansClustering {
	
	private static final String FILEPATH = "";
	private static final double ACCEPTANCE_THRESHOLD = 0.9;
	
	private int numberOfDocuments;
	private int numberOfWords;
	
	private Map<Integer, List<Wount>> data;
	private List<Centroid> kMeans;
	private int[] previousMembership;
	private int[] currentMembership;
	
	/************************* *************************/
	
	/**
	 * Constructor
	 */
	public KMeansClustering(int k) {
		this.data = new HashMap<Integer, List<Wount>>();
		this.kMeans = new ArrayList<Centroid>();
		
		/**
		 * Use the filepath to read the file and store it
		 * for quick access. Also store metadata. The format of the
		 * metadata data is as follows. It is a list of three integers,
		 * (#documents, #words, //TODO: this attribute)
		 */
		List<Integer> metadata = readData(FILEPATH);
		this.numberOfDocuments = metadata.get(0);
		this.numberOfWords = metadata.get(1);
		
		// Initialise other fields based on data.
		this.previousMembership = new int[numberOfDocuments];
		this.currentMembership = new int[numberOfDocuments];
		for(int i=0;i<numberOfDocuments;++i) {
			this.previousMembership[i] = -1;
			this.currentMembership[i] = 0;
		}
		
		// Initialise the k disfferent means.
		initialiseKMeans(k);
	}
	
	/************************* *************************/
	
	/**
	 * @description Read data from the specified filepath. It
	 * assumes that the data is formatted according to certain
	 * rules as follows:
	 * 1)
	 * 2)
	 * 3)
	 */
	private List<Integer> readData(String filepath) {
		List<Integer> ans = new ArrayList<Integer>();
		try {
			int currDocId = 0;
			BufferedReader br = new BufferedReader(new FileReader(filepath));
			String currentLine = "";
			while((currentLine = br.readLine()) != null) {
				String[] tmpArray = currentLine.split(" ");
				if(tmpArray.length == 3) { // Ignore the first three metadata lines.
					if(Integer.parseInt(tmpArray[0]) != currDocId) { // New documents entries.
						data.put(Integer.parseInt(tmpArray[0]), new ArrayList<Wount>());
						currDocId = Integer.parseInt(tmpArray[0]);
					}
					data.get(currDocId).add(new Wount(Integer.parseInt(tmpArray[1]), Double.parseDouble(tmpArray[2])));
				} else { // Return metadata to store separately.
					ans.add(Integer.parseInt(tmpArray[0]));
				}
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ans;
	}
	
	/************************* *************************/
	
	/**
	 * @description Initially decide on k points to be
	 * the centroids for starting the k-means process.
	 * In this function, we take k points distributed
	 * equally by document ID.
	 */
	private void initialiseKMeans(int k) {
		int d = numberOfDocuments / k;
		for(int i=1;i<=k;++i) {
			kMeans.add(new Centroid(i, data.get(d*k)));
		}
	}
	
	/************************* *************************/
	
	/**
	 * @description Cluster given set of points based on
	 * given centroids and given metric. Uses Euclidean
	 * metric by default.
	 */
	private void cluster(boolean useJaccard) {
		// Add code to cluster vertices to current set of centroids.
	}
	
	/************************* *************************/
	
	/**
	 * @description Recompute centroids by taking average
	 * of formed clusters.
	 */
	private void recomputeCentroids() {
		// Add code to recompute centroids from the currentMembership array.
	}
	
	/************************* *************************/
	
	/**
	 * @description Check whether the clustering method has
	 * converged to its final point. Uses a threshold defined
	 * as a constant.
	 */
	private boolean hasConverged() {
		int sameCount = 0;
		for(int i=0;i<numberOfDocuments;++i) {
			if(previousMembership[i] == currentMembership[i]) {
				sameCount++;
			}
		}
		return (sameCount >= ACCEPTANCE_THRESHOLD * numberOfDocuments);
	}
	
	/************************* *************************/
	
	/**
	 * @description Find the SQUARE of the Euclidean distance
	 * between a document and a centroid, both given by ID.
	 */
	private double getEuclideanDistance(int documentId, int centroidId) {
		double ans = 0;
		
		// TODO: Add code for Euclidean distance SQUARED.
		
		return ans;
	}
	
	/************************* *************************/
	
	/**
	 * @description Find the Jaccard distance between a document
	 * and a centroid, both given by ID.
	 */
	private double getJaccardDistance(int documentId, int centroidId) {
		double ans = 0;
		
		// TODO: Add code for Jaccard distance.
		
		return ans;
	}
}
