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

	private static final String DOCUMENTS_FILEPATH = "C:/Users/Ankita Sarkar/git/DMML2018-2/DMML2018-2/datasets/docword.toy.txt";
//	private static final String DOCUMENTS_FILEPATH = "/Users/sahil/Documents/CMI/Assignments/DMML/docword.toy.txt";
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
		int iterationCounter = 0;
		while (!this.hasConverged()) {
			System.out.println("Clustering iteration "+ ++iterationCounter);
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
		System.out.print("Initializing "+k+" means...");
		int d = numberOfDocuments / k;
		for(int i=1;i<=k;++i) {
			kMeans.add(new Centroid(i, data.get(d*k)));
		}
		System.out.println(" done.");
	}

	/************************* *************************/

	/**
	 * @description Cluster given set of points based on
	 * given centroids and given metric. Uses Jaccard
	 * metric by default.
	 */
	private void cluster(boolean useAngleDistance) {
		double minDistance = Integer.MAX_VALUE;
		double currentDistance = 0;
		int closestCentroidID = -1;
		int k = kMeans.size();
		for (int currentDocumentID = 1; currentDocumentID <= this.numberOfDocuments; ++currentDocumentID) {
			minDistance = Integer.MAX_VALUE;
			for (int currentCentroidID = 1; currentCentroidID <= k; ++currentCentroidID) {
				currentDistance = useAngleDistance ? getAngleDistance(currentDocumentID, currentCentroidID) : getJaccardDistance(currentDocumentID, currentCentroidID);
				if (minDistance > currentDistance) {
					closestCentroidID = currentCentroidID;
					minDistance = currentDistance;
				}
			}
			previousMembership[currentDocumentID-1] = currentMembership[currentDocumentID-1];
			currentMembership[currentDocumentID-1] = closestCentroidID;
			
		}
	}

	/************************* *************************/

	/**
	 * @description Recompute centroids by taking average
	 * of formed clusters.
	 * TODO : Improve the implementation. Perhaps a customised WountList that supports addition?
	 */
	private void recomputeCentroids() {
		System.out.println("Recomputing Centroids... ");
		int k = kMeans.size();
		Wount currentWount = new Wount(-1,-1);
		int[] clusterSizes = new int[k];
		int currentCentroidID = -1;
		/*
		 * Iterate over documents
		 * Add the word counts of each document to the sum for the appropriate cluster
		 * Each document contributes +1 to the size of its cluster
		 * TODO : Is it better to make k passes of the membership array?
		 */
		// reset the coordinates for each centroid
		for (int i = 0; i < k; ++i) {
			kMeans.get(i).setCoordinates(new ArrayList<Wount>());
		}

		// iterate over membership array and sum up coordinates per cluster. also keep size.
		for (int currentDocumentID = 1; currentDocumentID <= this.numberOfDocuments; ++currentDocumentID) {
			currentCentroidID = currentMembership[currentDocumentID-1];
//			System.out.println("Document "+currentDocumentID+" is in cluster "+currentCentroidID);
			clusterSizes[currentCentroidID-1]++;
//			System.out.println(" of size at least "+clusterSizes[currentCentroidID-1]);
//			System.out.println("Cluster sum was "+kMeans.get(currentCentroidID-1).getCoordinates());
//			System.out.println("Document word count is "+data.get(currentDocumentID));
			kMeans.get(currentCentroidID-1).setCoordinates(addCoordinates(kMeans.get(currentCentroidID-1).getCoordinates(),data.get(currentDocumentID)));
//			System.out.println("Cluster sum updated to) "+kMeans.get(currentCentroidID-1).getCoordinates());
		}

		// iterate over clusterID-1
		for (int i = 0; i < k; ++i) {
			// iterate over coordinates of cluster i+1
			for (int j = 0; j < kMeans.get(i).getCoordinatesSize(); ++j) {
				// divide coordinates by cluster size
				kMeans.get(i).divideCoordinates((double)clusterSizes[i]);
			}
		}
		System.out.println("...done.");
	}

	/************************* *************************/

	/**
	 * @description Check whether the clustering method has
	 * converged to its final point. Uses a threshold defined
	 * as a constant.
	 */
	private boolean hasConverged() {
		System.out.println("Checking for convergence with threshold "+ACCEPTANCE_THRESHOLD);
		int sameCount = 0;
		for (int i=0;i<kMeans.size();++i) {
			System.out.println(kMeans.get(i));
		}
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
		int documentCounter = 0;
		int centroidCounter = 0;
		int documentSize = data.get(documentId).size();
		int centroidSize = kMeans.get(centroidId-1).getCoordinatesSize();
		int currentCentroidWord = 0;
		int currentDocumentWord = 0;
		while(true) {
			if(documentCounter >= documentSize) {
				if(centroidCounter >= centroidSize) {
					break;
				} else {
					currentCentroidWord = kMeans.get(centroidId-1).getCoordinates().get(centroidCounter).getId();
					tempIDF = Math.log(this.numberOfDocuments) - Math.log(documentFrequencies.get(currentCentroidWord-1));
					centroidNorm += Math.pow(kMeans.get(centroidId-1).getCoordinates().get(centroidCounter++).getCount()*tempIDF, 2);
				}
			} else {
				if(centroidCounter >= centroidSize) {
					currentDocumentWord = data.get(documentId).get(documentCounter).getId();
					tempIDF = Math.log(this.numberOfDocuments) - Math.log(documentFrequencies.get(currentDocumentWord-1));
					documentNorm += Math.pow(data.get(documentId).get(documentCounter++).getCount()*tempIDF,2);
				} else {
					currentCentroidWord = kMeans.get(centroidId-1).getCoordinates().get(centroidCounter).getId();
					currentDocumentWord = data.get(documentId).get(documentCounter).getId();
					if(currentDocumentWord < currentCentroidWord) {
						tempIDF = Math.log(this.numberOfDocuments) - Math.log(documentFrequencies.get(currentDocumentWord-1));
						documentNorm += Math.pow(data.get(documentId).get(documentCounter++).getCount()*tempIDF,2);
					} else if(currentDocumentWord > currentCentroidWord) {
						tempIDF = Math.log(this.numberOfDocuments) - Math.log(documentFrequencies.get(currentCentroidWord-1));
						centroidNorm += Math.pow(kMeans.get(centroidId-1).getCoordinates().get(centroidCounter++).getCount()*tempIDF, 2);
					} else { // if(currentDocumentWord == currentCentroidWord) {
						tempIDF = Math.log(this.numberOfDocuments) - Math.log(documentFrequencies.get(currentCentroidWord-1));
						// equivalently, tempIDF = Math.log(this.numberOfDocuments) - Math.log(documentFrequencies.get(currentDocumentWord))
						dotProduct += data.get(documentId).get(documentCounter).getCount()*kMeans.get(centroidId-1).getCoordinates().get(centroidCounter).getCount()*Math.pow(tempIDF,2);
						documentNorm += Math.pow(data.get(documentId).get(documentCounter++).getCount()*tempIDF,2);
						centroidNorm += Math.pow(kMeans.get(centroidId-1).getCoordinates().get(centroidCounter++).getCount()*tempIDF, 2);
					}
				}
			}
		}
		System.out.print(dotProduct+" "+Math.sqrt(documentNorm*centroidNorm)+" ");
		return Math.acos(dotProduct/Math.sqrt(documentNorm*centroidNorm));
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
		int dataCounter = 0, centroidCounter = 0;
		int dataSize = data.get(documentId).size();
		int centroidSize = kMeans.get(centroidId-1).getCoordinatesSize();

		// Calculate cardinality of intersection.
		while(true) {
			if(dataCounter >= dataSize) {
				if(centroidCounter >= centroidSize) {
					break;
				} else {
					centroidCounter++;
				}
			} else {
				if(centroidCounter >= centroidSize) {
					dataCounter++;
				} else {
					if(data.get(documentId).get(dataCounter).getId() < kMeans.get(centroidId-1).getCoordinates().get(centroidCounter).getId()) {
						dataCounter++;
					} else if(data.get(documentId).get(dataCounter).getId() > kMeans.get(centroidId-1).getCoordinates().get(centroidCounter).getId()) {
						centroidCounter++;
					} else { // if(data.get(documentId).get(dataCounter).getId() < kMeans.get(centroidId).getCoordinates().get(centroidCounter).getId()) {
						intersection++;
						dataCounter++;
						centroidCounter++;
					}
				}
			}
		}

		// Calculate cardinality of union and symmetric difference.
		int union = data.get(documentId).size() + kMeans.get(centroidId-1).getCoordinates().size() - intersection;
		int symmetricDifference = union - intersection;
		//	System.out.println();
		//	System.out.println(intersection + ", " + union);
		return (double)symmetricDifference/(double)union;
	}

	/************************* *************************/

	/**
	 * @description Adds two vectors to obtain the sum vector
	 * with coordinates sorted in increasing order.
	 */
	private List<Wount> addCoordinates(List<Wount> A, List<Wount> B) {
		List<Wount> ans = new ArrayList<Wount>();
		int aIndex = 0, bIndex = 0;

		while(true) {
			if(aIndex >= A.size()) {
				if(bIndex >= B.size()) {
					break;
				} else {
					ans.add(B.get(bIndex++));
				}
			} else {
				if(bIndex >= B.size()) {
					ans.add(A.get(aIndex++));
				} else {
					if(A.get(aIndex).getId() < B.get(bIndex).getId()) {
						ans.add(A.get(aIndex++));
					} else if(A.get(aIndex).getId() > B.get(bIndex).getId()) {
						ans.add(B.get(bIndex++));
					} else { // if(A.get(aIndex).getId() == B.get(bIndex).getId()) {
						ans.add(new Wount(A.get(aIndex).getId(), A.get(aIndex++).getCount() + B.get(bIndex++).getCount()));
					}
				}
			}
		}
		return ans;
	}

	/************************* *************************/

	/**
	 * @throws IOException 
	 * @description Main function for local testing.
	 */
	public static void main(String[] args) throws IOException {
		List<Wount> a = new ArrayList<Wount>();
		a.add(new Wount(1, 3));
		a.add(new Wount(3, 1));
		a.add(new Wount(5, 2));
		a.add(new Wount(6, 2));
		a.add(new Wount(10, 1));
		a.add(new Wount(14, 3));
		a.add(new Wount(17, 5));
		a.add(new Wount(18, 2));

		List<Wount> b = new ArrayList<Wount>();
		b.add(new Wount(2, 1));
		b.add(new Wount(3, 2));
		b.add(new Wount(6, 4));
		b.add(new Wount(7, 1));
		b.add(new Wount(12, 1));
		b.add(new Wount(14, 2));
		b.add(new Wount(15, 3));
		b.add(new Wount(18, 1));

		KMeansClustering km = new KMeansClustering(2, false);
		System.out.println(km.addCoordinates(a, b));
		for (int c = 0; c < km.kMeans.size() ; c++) {
			System.out.println("Mean "+(c+1));
			for (int i = 0; i < km.kMeans.get(c).getCoordinatesSize(); ++i) {
				System.out.print(km.kMeans.get(c).getCoordinates().get(i).getId() + " ");
				if ((i + 1) % 20 == 0) {
					System.out.println();
				}
			}
			for (int i = 1; i <= km.numberOfDocuments; ++i) {
				System.out.println("\n" + km.getJaccardDistance(i, c+1));
//				System.out.println("\n" + km.getAngleDistance(i, c+1));
			} 
		}
		for (int d = 1; d <= km.numberOfDocuments; ++d) {
			System.out.println(km.data.get(d));
		}
	}
}
