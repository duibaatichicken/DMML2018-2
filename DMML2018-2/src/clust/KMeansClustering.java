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

	private static String TOY_DATASET_FILEPATH = "datasets/docword.toy.txt";
	private static String KOS_DATASET_FILEPATH = "datasets/docword.kos.txt";
	private static String NIPS_DATASET_FILEPATH = "datasets/docword.nips.txt";
	private static String ENRON_DATASET_FILEPATH = "datasets/docword.enron.txt";
	private static final double ACCEPTANCE_THRESHOLD = 0.9;

	private int numberOfDocuments;
	private int numberOfWords;

	private Map<Integer, List<Wount>> data;
	private List<Centroid> kMeans;
	private List<Integer> documentFrequencies;
	private int[] previousMembership;
	private int[] currentMembership;

	/************************* *************************/

	/**
	 * Constructor
	 * @throws IOException 
	 */
	public KMeansClustering(int k, boolean useAngleDistance) throws IOException {
		
		if(System.getProperty("os.name").equals("Windows 10")) {
			TOY_DATASET_FILEPATH = "C:\\Users\\Ankita Sarkar\\git\\DMML2018-2\\DMML2018-2\\datasets\\docword.toy.txt";
			KOS_DATASET_FILEPATH = "C:\\Users\\Ankita Sarkar\\git\\DMML2018-2\\DMML2018-2\\datasets\\docword.kos.txt";
			NIPS_DATASET_FILEPATH = "C:\\Users\\Ankita Sarkar\\git\\DMML2018-2\\DMML2018-2\\datasets\\docword.nips.txt";
			ENRON_DATASET_FILEPATH = "C:\\Users\\Ankita Sarkar\\git\\DMML2018-2\\DMML2018-2\\datasets\\docword.enron.txt";
		} else if(System.getProperty("os.name").equals("Mac OS X")) {
			
		} else {
			// Change filepaths to required format.
			TOY_DATASET_FILEPATH = "";
			KOS_DATASET_FILEPATH = "";
			NIPS_DATASET_FILEPATH = "";
			ENRON_DATASET_FILEPATH = "";
		}
		
		this.data = new HashMap<Integer, List<Wount>>();
		this.kMeans = new ArrayList<Centroid>();
		this.documentFrequencies = new ArrayList<Integer>();

		/**
		 * Use the documents filepath to read the file and store it
		 * for quick access. Also store metadata. The format of the
		 * metadata data is as follows. It is a list of three integers,
		 * (#documents, #words, #nonzero entries)
		 */
		List<Integer> metadata = readData(TOY_DATASET_FILEPATH);
		this.numberOfDocuments = metadata.get(0);
		this.numberOfWords = metadata.get(1);
		//		System.out.println(data);

		// Initialise other fields based on data.
		this.previousMembership = new int[numberOfDocuments];
		this.currentMembership = new int[numberOfDocuments];
		for(int i=0;i<numberOfDocuments;++i) {
			this.previousMembership[i] = -1;
			this.currentMembership[i] = 0;
		}

		// testing code
		//		System.out.println("Document frequencies:\n"+this.documentFrequencies);

		// Initialise the k different means.
		initialiseKMeans(k);

		// Run K-means clustering finitely many iterations. Use for testing only!
		//		int maxIterations = 2;
		//		for(int i = 0;i<maxIterations;++i) {
		//			System.out.println("*** Iteration "+ (i+1) + " ***");
		//			cluster(useAngleDistance);
		//			recomputeCentroids();
		//		}

		//		// Run K-means clustering up to convergence.
		int iterationCounter = 0;
		while (!this.hasConverged()) {
			System.out.println("*** Iteration "+ ++iterationCounter + " ***");
			cluster(useAngleDistance);
			recomputeCentroids();
		}

		displayClusters(false);
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
				documentFrequencies.set(currWordId-1, documentFrequencies.get(currWordId-1)+1);
			} else { // Return metadata to store separately.
				ans.add(Integer.parseInt(tmpArray[0]));
				try {
					for (int i = 0; i < ans.get(1); ++i) {
						documentFrequencies.add(0);
					}
				} catch (IndexOutOfBoundsException e) {
					// pass
				}

			}
		}
		br.close();
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
		System.out.print("Initializing "+ k +" means...");
		int d = numberOfDocuments / k;
		for(int i=1;i<=k;++i) {
			kMeans.add(new Centroid(i, data.get(d*i)));
		}
		System.out.println(" done.");
		System.out.println("-----------------------------");
		//		for(int i=0;i<kMeans.size();++i) {
		//			System.out.println(kMeans.get(i));
		//		}
	}

	/************************* *************************/

	/**
	 * @description Cluster given set of points based on
	 * given centroids and given metric. Uses Jaccard
	 * metric by default.
	 */
	private void cluster(boolean useAngleDistance) {
		System.out.print("Clustering data... ");
		double minDistance = Integer.MAX_VALUE;
		double currentDistance = 0;
		int closestCentroidID = -1;
		int k = kMeans.size();
		for (int currentDocumentID = 1; currentDocumentID <= numberOfDocuments; ++currentDocumentID) {
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
		System.out.println("done!");
		//		for(int i=0;i<numberOfDocuments;++i) {
		//			System.out.print(currentMembership[i] + ",");
		//		}
		//		System.out.println();
	}

	/************************* *************************/

	/**
	 * @description Recompute centroids by taking average
	 * of formed clusters.
	 */
	private void recomputeCentroids() {
		System.out.print("Recomputing centroids... ");
		int k = kMeans.size();
		Wount currentWount = new Wount(-1,-1);
		int[] clusterSizes = new int[k];
		int currentCentroidID = -1;

		// Reset the coordinates for each centroid
		for (int i = 0; i < k; ++i) {
			kMeans.get(i).setCoordinates(new ArrayList<Wount>());
		}

		// Iterate over membership array and sum up coordinates per cluster. Also keep size.
		for (int currentDocumentID = 1; currentDocumentID <= numberOfDocuments; ++currentDocumentID) {
			currentCentroidID = currentMembership[currentDocumentID-1];
			clusterSizes[currentCentroidID-1]++;
			kMeans.get(currentCentroidID-1).setCoordinates(addCoordinates(kMeans.get(currentCentroidID-1).getCoordinates(),data.get(currentDocumentID)));
		}

		// Normalise each centroid.
		for (int i = 0; i < k; ++i) {
			kMeans.get(i).divideCoordinates((double)clusterSizes[i]);
		}
		System.out.println("done!");
		System.out.println();
		//		System.out.println(kMeans);
	}

	/************************* *************************/

	/**
	 * @description Check whether the clustering method has
	 * converged to its final point. Uses a threshold defined
	 * as a constant.
	 */
	private boolean hasConverged() {
		//		System.out.println("Checking for convergence with threshold "+ACCEPTANCE_THRESHOLD);
		int sameCount = 0;
		for (int i=0;i<kMeans.size();++i) {
			//			System.out.println(kMeans.get(i));
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
		//		System.out.print(dotProduct+" "+Math.sqrt(documentNorm*centroidNorm)+" ");
		return Math.acos(dotProduct/Math.sqrt(documentNorm*centroidNorm));
	}

	//	/************************* *************************/
	//		
	//	/**
	//	 * @description computes IDF of given word based on data
	//	 */
	//	private double computeIDF(int wordID) {
	//		
	//	}

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
	 * @description Displays elements in a cluster.
	 */
	private void displayClusters(boolean showClusterSizeOnly) {
		List<List<Integer>> clusters = new ArrayList<List<Integer>>();

		// Initialise k empty clusters.
		for(int i=0;i<kMeans.size();++i) {
			clusters.add(new ArrayList<Integer>());
		}

		// Populate clusters.
		for(int i=0;i<currentMembership.length;++i) {
			clusters.get(currentMembership[i]-1).add(i+1);
		}

		Iterator<List<Integer>> outputIter = clusters.iterator();
		List<Integer> currCluster = null;
		if(showClusterSizeOnly) {
			System.out.println("Cluster sizes:");
			while(outputIter.hasNext()) {
				currCluster = outputIter.next();
				System.out.print(currCluster.size() + ", ");
			}
			System.out.println();
		} else {
			System.out.println("Clusters:");
			while(outputIter.hasNext()) {
				currCluster = outputIter.next();
				System.out.println(currCluster);
			}
		}
	}

	/************************* *************************/

	/**
	 * @throws IOException 
	 * @description Main function for local testing.
	 */
	public static void main(String[] args) throws IOException {
		//		List<Wount> a = new ArrayList<Wount>();
		//		a.add(new Wount(1, 3));
		//		a.add(new Wount(3, 1));
		//		a.add(new Wount(5, 2));
		//		a.add(new Wount(6, 2));
		//		a.add(new Wount(10, 1));
		//		a.add(new Wount(14, 3));
		//		a.add(new Wount(17, 5));
		//		a.add(new Wount(18, 2));
		//
		//		List<Wount> b = new ArrayList<Wount>();
		//		b.add(new Wount(2, 1));
		//		b.add(new Wount(3, 2));
		//		b.add(new Wount(6, 4));
		//		b.add(new Wount(7, 1));
		//		b.add(new Wount(12, 1));
		//		b.add(new Wount(14, 2));
		//		b.add(new Wount(15, 3));
		//		b.add(new Wount(18, 1));

		KMeansClustering km = new KMeansClustering(2, false);
//		System.out.println(System.getProperty("os.name"));
		//		System.out.println(km.addCoordinates(a, b));
		//		for (int c = 0; c < km.kMeans.size() ; c++) {
		//			System.out.println("Mean "+(c+1));
		//			for (int i = 0; i < km.kMeans.get(c).getCoordinatesSize(); ++i) {
		//				System.out.print(km.kMeans.get(c).getCoordinates().get(i).getId() + " ");
		//				if ((i + 1) % 20 == 0) {
		//					System.out.println();
		//				}
		//			}
		//			for (int i = 1; i <= km.numberOfDocuments; ++i) {
		//				System.out.println("\n" + km.getJaccardDistance(i, c+1));
		////				System.out.println("\n" + km.getAngleDistance(i, c+1));
		//			} 
		//		}
		////		for (int d = 1; d <= km.numberOfDocuments; ++d) {
		////			System.out.println(km.data.get(d));
		////		}
	}
}
