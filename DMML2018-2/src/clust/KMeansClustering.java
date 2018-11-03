package clust;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ds.Centroid;
import ds.Wount;

@SuppressWarnings("unused")
public class KMeansClustering {
	
	private static final String DOCUMENTS_FILEPATH = "";
	private static final String VOCABULARY_FILEPATH = "";
	private static final double ACCEPTANCE_THRESHOLD = 0.9;
	
	private int numberOfDocuments;
	private int numberOfWords;
	
	private Map<Integer, List<Wount>> data;
	private List<Centroid> kMeans;
	private HashMap<Integer, Integer> documentFrequencies;
	private int[] previousMembership;
	private int[] currentMembership;
	
	/************************* *************************/
	
	/**
	 * Constructor
	 * @throws IOException 
	 */
	public KMeansClustering(int k, boolean useAngleDistance) throws IOException {
		this.data = new HashMap<Integer, List<Wount>>();
		this.kMeans = new ArrayList<Centroid>();
		this.documentFrequencies = new HashMap<Integer, Integer>();
		
		/**
		 * Use the documents filepath to read the file and store it
		 * for quick access. Also store metadata. The format of the
		 * metadata data is as follows. It is a list of three integers,
		 * (#documents, #words, #nonzero entries)
		 */
		List<Integer> metadata = readData(DOCUMENTS_FILEPATH);
		this.numberOfDocuments = metadata.get(0);
		this.numberOfWords = metadata.get(1);
		
		// Initialise other fields based on data.
		this.previousMembership = new int[numberOfDocuments];
		this.currentMembership = new int[numberOfDocuments];
		for(int i=0;i<numberOfDocuments;++i) {
			this.previousMembership[i] = -1;
			this.currentMembership[i] = 0;
		}
		
		// Initialise the k different means.
		initialiseKMeans(k);
		// run k-means clustering up to convergence
		while (!this.hasConverged()) {
			this.cluster(useAngleDistance);
			this.recomputeCentroids();
		}
		// TODO : format output
	}
	
	/************************* *************************/
	
	/**
	 * @throws IOException 
	 * @description Read data from the specified filepath. It
	 * assumes that the data is formatted according to certain
	 * rules as follows:
	 * 1) first 3 lines are :
	 * * D = number of documents
	 * * W = number of words
	 * * NNZ = number of nonzero entries
	 * 2) next NNZ lines are :
	 * * docID wordID count
	 * * where count=number of occurrences word wordID in document docID
	 */
	private List<Integer> readData(String filepath) throws IOException {
		List<Integer> ans = new ArrayList<Integer>();
		int currDocId = 0;
		int currWordId = 0;
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		String currentLine = "";
		while((currentLine = br.readLine()) != null) {
			String[] tmpArray = currentLine.split(" ");
			if(tmpArray.length == 3) { // Ignore the first three metadata lines.
				if(Integer.parseInt(tmpArray[0]) != currDocId) { // New documents entries.
					data.put(Integer.parseInt(tmpArray[0]), new ArrayList<Wount>());
					currDocId = Integer.parseInt(tmpArray[0]);
				}
				currWordId = Integer.parseInt(tmpArray[1]);
				data.get(currDocId).add(new Wount(currWordId, Double.parseDouble(tmpArray[2])));
				documentFrequencies.put(currWordId,documentFrequencies.getOrDefault(currWordId,0)+1);
			} else { // Return metadata to store separately.
				ans.add(Integer.parseInt(tmpArray[0]));
			}
		}
		br.close();
		return ans;
		// TODO FileNotFoundException
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
	 * given centroids and given metric. Uses Jaccard
	 * metric by default.
	 */
	private void cluster(boolean useAngleDistance) {
		int currentDocumentID = 0;
		double maxDistance = 0;
		double currentDistance = 0;
		int closestCentroidID = -1;
		int k = kMeans.size();
		Iterator<Integer> documentIDs = data.keySet().iterator();
		while (documentIDs.hasNext()) {
			currentDocumentID = documentIDs.next();
			for (int currentCentroidID = 1; currentCentroidID <= k; ++currentCentroidID) {
				currentDistance = useAngleDistance ? getAngleDistance(currentDocumentID, currentCentroidID) : getJaccardDistance(currentDocumentID, currentCentroidID);
				if (maxDistance < currentDistance) {
					closestCentroidID = currentCentroidID;
					maxDistance = currentDistance;
				}
			}
			previousMembership[currentDocumentID] = currentMembership[currentDocumentID];
			currentMembership[currentDocumentID] = closestCentroidID;
		}
	}
	
	/************************* *************************/
	
	/**
	 * @description Recompute centroids by taking average
	 * of formed clusters.
	 * TODO : Improve the implementation. Perhaps a customised WountList that supports addition?
	 */
	private void recomputeCentroids() {
		int k = kMeans.size();
		Wount currentWount = new Wount(-1,-1);
		double[][] currentSums = new double[k][this.numberOfWords];
		int[] currentSizes = new int[k];
		int currentDocumentID = -1;
		/*
		 * Iterate over documents
		 * Add the word counts of each document to the sum for the appropriate cluster
		 * Each document contributes +1 to the size of its cluster
		 * TODO : Is it better to make k passes of the membership array?
		 */
		Iterator<Integer> documentIter = data.keySet().iterator();
		while (documentIter.hasNext()) {
			currentDocumentID = documentIter.next();
			Iterator<Wount> wountIter = data.get(currentDocumentID).iterator();
			while (wountIter.hasNext()) {
				currentWount = wountIter.next();
				currentSums[currentMembership[currentDocumentID]][currentWount.getId()] += currentWount.getCount();
			}
			currentSizes[currentMembership[currentDocumentID]]++;
		}
		/*
		 * Iterate over clusters
		 * For each word, contribute a size-normalised wount to cluster coordinates
		 * if that wount is nonzero in the cluster
		 */
		Iterator<Centroid> centroidIter = kMeans.iterator();
		Centroid currentCentroid = new Centroid(-1, new ArrayList<Wount>());
		while (centroidIter.hasNext()) {
			currentCentroid = centroidIter.next();
			List<Wount> currentMean = new ArrayList<Wount>();
			for (int wordID = 1; wordID <= this.numberOfWords; ++wordID) {
				if (currentSums[currentCentroid.getId()][wordID] > 0) {
					currentMean.add(new Wount(wordID, currentSums[currentCentroid.getId()][wordID] / currentSizes[currentCentroid.getId()]));
				}
			}
			kMeans.get(currentCentroid.getId()).setCoordinates(currentMean);
		}
		// TODO : Add code to recompute centroids from the currentMembership array.
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
	 * @description Find the angle between two documents in
	 * the vector model using tf-idf weights for words.
	 */
	private double getAngleDistance(int documentId, int centroidId) {
		double dotProduct = 0;
		double documentNorm = 0;
		double centroidNorm = 0;
		double tempIDF = 0;
		Iterator<Wount> centroidIter = kMeans.get(centroidId).getCoordinates().iterator();
		Iterator<Wount> documentIter = data.get(documentId).iterator();
		Wount currentDocumentWount;
		Wount currentCentroidWount;
		while(centroidIter.hasNext() && documentIter.hasNext() ) {
			currentDocumentWount = documentIter.next();
			currentCentroidWount = centroidIter.next();
			while (currentDocumentWount.getId() < currentCentroidWount.getId()) {
				currentDocumentWount = documentIter.next();
			}
			while (currentDocumentWount.getId() > currentCentroidWount.getId()) {
				currentCentroidWount = centroidIter.next();
			}
			// Set tf-idf of the current word w.r.t document tf and centroid tf
			tempIDF = Math.log(this.numberOfDocuments) - Math.log(documentFrequencies.get(currentDocumentWount.getId()));
			dotProduct += currentDocumentWount.getCount()*currentCentroidWount.getCount()*Math.pow(tempIDF,2);
			documentNorm += Math.pow(currentDocumentWount.getCount()*tempIDF,2);
			centroidNorm += Math.pow(currentCentroidWount.getCount()*tempIDF,2);
		}
		return dotProduct/Math.sqrt(documentNorm*centroidNorm);
	}
	
	/************************* *************************/
	
	/**
	 * @description Find the Jaccard distance between a document
	 * and a centroid, both given by ID. Jaccard distance is
	 * the ratio between the sizes of symmetric difference
	 * and union. Note that this converts our model from
	 * 'bag of words' to 'set of words'.
	 */
	private double getJaccardDistance(int documentId, int centroidId) {
		int intersection = 0;
		Iterator<Wount> documentIter = data.get(documentId).iterator();
		Iterator<Wount> centroidIter = kMeans.get(centroidId).getCoordinates().iterator();
		int documentCurrentWordId = Integer.MIN_VALUE;
		int centroidCurrentWordId = Integer.MIN_VALUE;
		
		// Get cardinality of intersection
		while(documentIter.hasNext() || centroidIter.hasNext()) {
			if(documentCurrentWordId == centroidCurrentWordId) {
				if(documentCurrentWordId != Integer.MIN_VALUE) {
					intersection++;
				}
				documentCurrentWordId = documentIter.hasNext() ? documentIter.next().getId() : Integer.MAX_VALUE;
				centroidCurrentWordId = centroidIter.hasNext() ? centroidIter.next().getId() : Integer.MAX_VALUE;
			} else if(documentCurrentWordId < centroidCurrentWordId) {
				documentCurrentWordId = documentIter.hasNext() ? documentIter.next().getId() : Integer.MAX_VALUE;
			} else { // documentCurrentWordId > centroidCurrentWordId
				centroidCurrentWordId = centroidIter.hasNext() ? centroidIter.next().getId() : Integer.MAX_VALUE;
			}
		}
		
		int union = data.get(documentId).size() + kMeans.get(centroidId).getCoordinates().size() - intersection;
		int symmetricDifference = union - intersection;
//		System.out.println(intersection + ", " + union);
		return (double)symmetricDifference/(double)union;
	}
	
	/************************* *************************/
	
	/**
	 * @description Main function for local testing.
	 */
	public static void main(String[] args) {
		
	}
}
