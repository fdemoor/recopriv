/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.cf.taste.impl.recommender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.Lists;

import org.apache.mahout.cf.taste.common.NoSuchItemException;
import org.apache.mahout.cf.taste.common.NoSuchUserException;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.similarity.GenericItemSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.GenericUserSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.TwoStepUncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.recommender.IDRescorer;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * <p>
 * A simple class that refactors the "find top N things" logic that is used in
 * several places.
 * </p>
 */
public final class TopItems {

  private static final Logger log = LoggerFactory.getLogger(TopItems.class);
	private static final long[] NO_IDS = new long[0];
	private static final boolean davideDebug = false;
	public static int firstSybilID = 944;

	private TopItems() {
	}


	public static List<RecommendedItem> getTopItems(int howMany,
			LongPrimitiveIterator possibleItemIDs, IDRescorer rescorer,
			Estimator<Long> estimator) throws TasteException {
		Preconditions.checkArgument(possibleItemIDs != null,
				"possibleItemIDs is null");
		Preconditions.checkArgument(estimator != null, "estimator is null");

		Queue<RecommendedItem> topItems = new PriorityQueue<RecommendedItem>(
				howMany + 1,
				Collections.reverseOrder(ByValueRecommendedItemComparator
						.getInstance()));
		boolean full = false;
		double lowestTopValue = Double.NEGATIVE_INFINITY;
		while (possibleItemIDs.hasNext()) {
			long itemID = possibleItemIDs.next();
			if (rescorer == null || !rescorer.isFiltered(itemID)) {
				double preference;
				try {
					preference = estimator.estimate(itemID);
				} catch (NoSuchItemException nsie) {
					continue;
				}
				double rescoredPref = rescorer == null ? preference : rescorer
						.rescore(itemID, preference);
				if (!Double.isNaN(rescoredPref)
						&& (!full || rescoredPref > lowestTopValue)) {
					topItems.add(new GenericRecommendedItem(itemID,
							(float) rescoredPref));
					if (full) {
						topItems.poll();
					} else if (topItems.size() > howMany) {
						full = true;
						topItems.poll();
					}
					lowestTopValue = topItems.peek().getValue();
				}
			}
		}
		int size = topItems.size();
		if (size == 0) {
			return Collections.emptyList();
		}
		List<RecommendedItem> result = Lists.newArrayListWithCapacity(size);
		result.addAll(topItems);
		Collections
				.sort(result, ByValueRecommendedItemComparator.getInstance());
		return result;
	}

	// Original function, similarity ties broken with lower IDs first
	public static long[] getTopUsers(int howMany,
			LongPrimitiveIterator allUserIDs, IDRescorer rescorer,
			Estimator<Long> estimator) throws TasteException {
		Queue<SimilarUser> topUsers = new PriorityQueue<SimilarUser>(
				howMany + 1, Collections.reverseOrder());
		boolean full = false;
		double lowestTopValue = Double.NEGATIVE_INFINITY;
		while (allUserIDs.hasNext()) {
			long userID = allUserIDs.next();
			if (rescorer != null && rescorer.isFiltered(userID)) {
				continue;
			}
			double similarity;
			try {
				similarity = estimator.estimate(userID);
			} catch (NoSuchUserException nsue) {
				continue;
			}
			double rescoredSimilarity = rescorer == null ? similarity
					: rescorer.rescore(userID, similarity);
			if (davideDebug)
				System.out.println("TopU user=" + estimator.toString()
						+ " similarity=" + similarity + " rescored="
						+ rescoredSimilarity + "  neigh=" + userID + " syb: "
						+ (userID < firstSybilID ? "NO " : "YES"));
			if (!Double.isNaN(rescoredSimilarity)
					&& (!full || rescoredSimilarity > lowestTopValue)) {
				topUsers.add(new SimilarUser(userID, rescoredSimilarity));
				if (full) {
					topUsers.poll();
				} else if (topUsers.size() > howMany) {
					full = true;
					topUsers.poll();
				}
				lowestTopValue = topUsers.peek().getSimilarity();
			}
		}
		int size = topUsers.size();
		if (size == 0) {
			return NO_IDS;
		}
		List<SimilarUser> sorted = Lists.newArrayListWithCapacity(size);
		sorted.addAll(topUsers);
		Collections.sort(sorted);
		long[] result = new long[size];
		int i = 0;
		for (SimilarUser similarUser : sorted) {
			result[i++] = similarUser.getUserID();
		}
		if (davideDebug)
			System.out.println("User : " + estimator.toString() + " computed "
					+ result.length + " topUsers. LowestTopValue="
					+ lowestTopValue);

		return result;
	}

	// Similarity ties broken with higher IDs first
	public static long[] getTopUsersHigher(int howMany,
			LongPrimitiveIterator allUserIDs, IDRescorer rescorer,
			Estimator<Long> estimator) throws TasteException {
		// Queue<SimilarUserAlt> topUsers = new
		// PriorityQueue<SimilarUserAlt>(howMany + 1,
		// Collections.reverseOrder());
		List<SimilarUserAlt> topUsers = Lists
				.newArrayListWithCapacity(howMany + 1);
		boolean full = false;
		double lowestTopValue = Double.NEGATIVE_INFINITY;
		while (allUserIDs.hasNext()) {
			long userID = allUserIDs.next();
			if (rescorer != null && rescorer.isFiltered(userID)) {
				continue;
			}
			double similarity;
			try {
				similarity = estimator.estimate(userID);
			} catch (NoSuchUserException nsue) {
				continue;
			}
			double rescoredSimilarity = rescorer == null ? similarity
					: rescorer.rescore(userID, similarity);
			if (davideDebug)
				System.out.println("TopH user=" + estimator.toString()
						+ " similarity=" + similarity + " rescored="
						+ rescoredSimilarity + "  neigh=" + userID + " syb: "
						+ (userID < firstSybilID ? "NO " : "YES"));

			if (!Double.isNaN(rescoredSimilarity)
					&& (!full || rescoredSimilarity > lowestTopValue)) {
				// topUsers.add(new SimilarUserAlt(userID, rescoredSimilarity));
				// if (full) {
				// topUsers.poll();
				// } else if (topUsers.size() > howMany) {
				// full = true;
				// topUsers.poll();
				// }
				// lowestTopValue = topUsers.peek().getSimilarity();
				topUsers.add(new SimilarUserAlt(userID, rescoredSimilarity));
				Collections.sort(topUsers);
				if (full) {
					topUsers.remove(topUsers.size() - 1);
				} else if (topUsers.size() > howMany) {
					full = true;
					topUsers.remove(topUsers.size() - 1);
				}
				lowestTopValue = topUsers.get(topUsers.size() - 1)
						.getSimilarity();
			} else if (!Double.isNaN(rescoredSimilarity)
					&& rescoredSimilarity == lowestTopValue
					&& userID > topUsers.get(topUsers.size() - 1).getUserID()) {
				// topUsers.poll();
				topUsers.add(new SimilarUserAlt(userID, rescoredSimilarity));
				Collections.sort(topUsers);
				// topUsers.remove(topUsers.size()-1);
			}
		}
		int size = topUsers.size();
		// System.out.println("topUsers.size() = " + size);
		// System.out.println("topUsers = " + topUsers);
		if (size == 0) {
			return NO_IDS;
		}
		/* List<SimilarUserAlt> sorted = Lists.newArrayListWithCapacity(size); */
		// sorted.addAll(topUsers);
		/* Collections.sort(sorted); */
		long[] result = new long[howMany];
		// int i = 0;
		for (int i = 0; i < howMany; i++) {
			result[i] = topUsers.get(i).getUserID();
			// System.out.println("Neighor "+topUsers.get(i).getUserID()+", simil = "+topUsers.get(i).getSimilarity());
		}
		if (davideDebug)
			System.out.println("User : " + estimator.toString() + " computed "
					+ result.length + " topUsers. LowestTopValue="
					+ lowestTopValue);

		return result;
	}

	// Similarity ties broken randomly
	public static long[] getTopUsersRandom(int howMany,
			LongPrimitiveIterator allUserIDs, IDRescorer rescorer,
			Estimator<Long> estimator, Random rand) throws TasteException {
		//System.out.println("[DAVIDE] entering getTopUsersRandom with howMany="+howMany);
		if (davideDebug)
			System.out.println("in getTopUsersRandom");
		
		// The head of topUsers contains the user with the lowest similarity
		// value, ties being broken arbitrarily (says the documentation).
		Queue<SimilarUser> topUsers = new PriorityQueue<SimilarUser>(
				howMany + 1, Collections.reverseOrder());
		boolean full = false;
		double lowestTopValue = Double.NEGATIVE_INFINITY;
		// Evaluate the similarity of all users and keep at least the howMany
		// most similar in topUsers.
		// topUsers will contain more than howMany users if several users have a
		// similarity equal to lowestTopValue.
		while (allUserIDs.hasNext()) {
			long userID = allUserIDs.next();
			if (rescorer != null && rescorer.isFiltered(userID)) {
				//System.out.println("[DAVIDE] filtered user "+userID+": skipping");
				continue;
			}
			double similarity;
			try {
				similarity = estimator.estimate(userID);
			} catch (NoSuchUserException nsue) {
				//System.out.println("[DAVIDE] no such user exception with uid "+userID+": skipping");
				continue;
			}
//			System.out.println("[DAVIDE] before rescoring uid= "+userID+" with rescorer="+rescorer+" of class "+ (rescorer == null ? "null" : rescorer.getClass().getCanonicalName())+" and similarity="+similarity);
			double rescoredSimilarity = rescorer == null ? similarity
					: rescorer.rescore(userID, similarity);
//			System.out.println("[DAVIDE] after rescoring got rescoredSimilarity="+rescoredSimilarity);
			if (davideDebug)
				System.out.println("TopR user=" + estimator.toString()
						+ " similarity=" + similarity + " rescored="
						+ rescoredSimilarity + "  neigh=" + userID + " syb: "
						+ (userID < firstSybilID ? "NO " : "YES"));
//			System.out.println("[DAVIDE] before if  at end of loop: rescored="+rescoredSimilarity+" isNaN="+Double.isNaN(rescoredSimilarity)+" full="+full+" lowestTopValue="+lowestTopValue);
			if (!Double.isNaN(rescoredSimilarity)
					&& (!full || rescoredSimilarity >= lowestTopValue)) {
//				System.out.println("[DAVIDE] adding to topUsers ");
				topUsers.add(new SimilarUser(userID, rescoredSimilarity));
				if (rescoredSimilarity > lowestTopValue) {
					if (topUsers.size() > howMany) {
						topUsers.poll();
					}
					lowestTopValue = topUsers.peek().getSimilarity();
				}
//			} else {
//				System.out.println("[DAVIDE] not adding to topUsers ");
			}
		}
		int size = topUsers.size();
		if (size == 0) {
			return NO_IDS;
		}
		// System.out.println("topUsers ("+size+" users) = "+topUsers);
		// System.out.println("topUsers size = "+size);

		List<SimilarUser> sorted = Lists.newArrayListWithCapacity(size);
		sorted.addAll(topUsers);
		Collections.sort(sorted); // Sort from most similar to least similar,
									// w/o reordering equally similar users
//		System.out.println("[DAVIDE] before loop sorted  of size "+sorted.size());

		// Choose the howMany most similar users in sorted, with random
		// selection among users of equal similarity.
		int firstSameSimilarityIndex = 0;
		int lastSameSimilarityIndex = getLastSameSimilarityIndex(sorted,
				firstSameSimilarityIndex);
		int nbSameSimilarity = lastSameSimilarityIndex + 1
				- firstSameSimilarityIndex;
		Set<Integer> selectedIndices = new HashSet<Integer>(howMany);
//		System.out.println("[DAVIDE] initial firstSameSI="+firstSameSimilarityIndex+" lastSameSI="+lastSameSimilarityIndex+" nbSameSim="+nbSameSimilarity);
		
		// long[] result = new long[size];
		long[] result = new long[howMany];
		int i = 0;
		/* for (SimilarUser similarUser : sorted) { */
		// result[i++] = similarUser.getUserID();
		/* } */
		while (i < howMany && lastSameSimilarityIndex>=0) {
//			System.out.println("[DAVIDE] i="+i+" howMany="+howMany);
			if (i == (firstSameSimilarityIndex + nbSameSimilarity)) {
				firstSameSimilarityIndex = i;
				lastSameSimilarityIndex = getLastSameSimilarityIndex(sorted,
						firstSameSimilarityIndex);
				nbSameSimilarity = lastSameSimilarityIndex + 1
						- firstSameSimilarityIndex;
				if (lastSameSimilarityIndex<0){
					continue;
				}
//				System.out.println("[DAVIDE] new firstSameSI="+firstSameSimilarityIndex+" lastSameSI="+lastSameSimilarityIndex+" nbSameSim="+nbSameSimilarity);
			}
			if (davideDebug)
				System.out.println("nbSameSimilarity = " + nbSameSimilarity
						+ " similarity=?");
//			System.out.println("[DAVIDE] getting random Index from sorted of size "+sorted.size()+
//					" with firstSameSI="+firstSameSimilarityIndex+" lastSameSI="+lastSameSimilarityIndex+" "+selectedIndices.size());
			int index = getRandomIndex(sorted, firstSameSimilarityIndex,
					lastSameSimilarityIndex, selectedIndices, rand);
//			System.out.println("[DAVIDE] got random Index= "+index+ " from sorted of size "+sorted.size()+
//					" with firstSameSI="+firstSameSimilarityIndex+" lastSameSI="+lastSameSimilarityIndex+" "+selectedIndices.size());
			
			selectedIndices.add(index);
			result[i++] = sorted.get(index).getUserID();
		}
		if (davideDebug)
			System.out.println("User : " + estimator.toString() + " computed "
					+ result.length + " topUsers. LowestTopValue="
					+ lowestTopValue);
//		System.out.println("[DAVIDE] returning result of length "+result.length);

		return result;
	}


	public static long[] getTwoStepTopUsersRandom(int howMany,
			LongPrimitiveIterator allUserIDs, IDRescorer rescorer,
			Estimator<Long> estimator, TwoStepUncenteredCosineSimilarity twoStep,
			Random rand, long currentUser, HashMap<Long, Double> firstRoundComputations) throws TasteException {
		if (davideDebug)
			System.out.println("in getTopUsersRandom");
		// The head of topUsers contains the user with the lowest similarity
		// value, ties being broken arbitrarily (says the documentation).
		Queue<SimilarUser> topUsers = new PriorityQueue<SimilarUser>(
				howMany + 1, Collections.reverseOrder());
		boolean full = false;
		double lowestTopValue = Double.NEGATIVE_INFINITY;

		HashSet<Double> simValues = new HashSet<Double>();

		// First round computations: compute all cosine similarities
		// and keep them in a HashMap

		while (allUserIDs.hasNext()) {
			long userID = allUserIDs.next();
			if (rescorer != null && rescorer.isFiltered(userID)) {
				continue;
			}
			double similarity;
			try {
				similarity = estimator.estimate(userID);
			} catch (NoSuchUserException nsue) {
				continue;
			}

			firstRoundComputations.put(userID, similarity);

			similarity *=100;
			similarity=Math.round(similarity);
			similarity /=100;
			simValues.add(similarity);

		}

		// Computing threshold for user
		ArrayList<Double> sortedValues = new ArrayList<Double>(simValues);
		Collections.sort(sortedValues);
		int maxPos = sortedValues.size() - 1;
		double percentile = twoStep.getPercentileThreshold();
		int position = (int) (maxPos * percentile);
		double threshold = sortedValues.get(position);
		twoStep.setSimilarityThresholdForUser(currentUser, threshold);
		log.debug("User {}: {} percentile is at position {} among {} in similarity array, threshold={}", currentUser, percentile, position, maxPos, threshold);

		// Iterating through HashMap to adapt similarities above the threshold
		// Return then the nearest neighbors with a priority queue
		lowestTopValue = Double.NEGATIVE_INFINITY;
		for (Map.Entry<Long, Double> entry : firstRoundComputations.entrySet()) {
    	long userID = entry.getKey();
    	double similarity = entry.getValue();

			if (similarity >= threshold) {
				similarity = threshold + twoStep.computeSecondStep(threshold, currentUser, userID);
			}

			double rescoredSimilarity = rescorer == null ? similarity
					: rescorer.rescore(userID, similarity);

			if (!Double.isNaN(rescoredSimilarity)
					&& (!full || rescoredSimilarity >= lowestTopValue)) {
				topUsers.add(new SimilarUser(userID, rescoredSimilarity));
				if (rescoredSimilarity > lowestTopValue) {
					if (topUsers.size() > howMany) {
						topUsers.poll();
					}
					lowestTopValue = topUsers.peek().getSimilarity();
				}
			}
		}

		int size = topUsers.size();
		if (size == 0) {
			return NO_IDS;
		}
		// System.out.println("topUsers ("+size+" users) = "+topUsers);
		// System.out.println("topUsers size = "+size);

		List<SimilarUser> sorted = Lists.newArrayListWithCapacity(size);
		sorted.addAll(topUsers);
		Collections.sort(sorted); // Sort from most similar to least similar,
									// w/o reordering equally similar users
		// System.out.println("sorted = "+sorted);

		// Choose the howMany most similar users in sorted, with random
		// selection among users of equal similarity.
		int firstSameSimilarityIndex = 0;
		int lastSameSimilarityIndex = getLastSameSimilarityIndex(sorted,
				firstSameSimilarityIndex);
		int nbSameSimilarity = lastSameSimilarityIndex + 1
				- firstSameSimilarityIndex;
		Set<Integer> selectedIndices = new HashSet<Integer>(howMany);

		// long[] result = new long[size];
		long[] result = new long[howMany];
		int i = 0;
		/* for (SimilarUser similarUser : sorted) { */
		// result[i++] = similarUser.getUserID();
		/* } */
		while (i < howMany && lastSameSimilarityIndex >= 0) {
			if (i == (firstSameSimilarityIndex + nbSameSimilarity)) {
				firstSameSimilarityIndex = i;
				lastSameSimilarityIndex = getLastSameSimilarityIndex(sorted,
						firstSameSimilarityIndex);
				nbSameSimilarity = lastSameSimilarityIndex + 1
						- firstSameSimilarityIndex;
				if (lastSameSimilarityIndex<0){
					continue;
				}
			}
			if (davideDebug)
				System.out.println("nbSameSimilarity = " + nbSameSimilarity
						+ " similarity=?");
			int index = getRandomIndex(sorted, firstSameSimilarityIndex,
					lastSameSimilarityIndex, selectedIndices, rand);
			selectedIndices.add(index);
			result[i++] = sorted.get(index).getUserID();
		}
		if (davideDebug)
			System.out.println("User : " + estimator.toString() + " computed "
					+ result.length + " topUsers. LowestTopValue="
					+ lowestTopValue);
		return result;
	}

	/**
	 * Return the index in sortedList of the last user with the same similarity
	 * as sortedList.get(firstIndex). firstIndex must be within
	 * [0,sortedList.size()[, otherwise return -1.
	 */
	private static int getLastSameSimilarityIndex(List<SimilarUser> sortedList,
			int firstIndex) {
		int result = -1;
		if (firstIndex >= 0 && firstIndex < sortedList.size()) {
			SimilarUser referenceUser = sortedList.get(firstIndex);
			int i = firstIndex;
			// System.out.println("compareTo value = " +
			// referenceUser.compareTo(sortedList.get(i+1)));
			while (i < sortedList.size()
					&& referenceUser.getSimilarity() == sortedList.get(i)
							.getSimilarity()) {
				i++;
			}
			result = i - 1;
		}
		return result;
	}

	/**
	 * Randomly select an index from sortedList, within [firstIndex,lastIndex],
	 * which is not included in excludedIndices. firstIndex and lastIndex must
	 * be within [0,sortedList.size()[, and firstIndex must be lesser or equal
	 * to lastIndex. Otherwise return -1.
	 */
	private static int getRandomIndex(List<SimilarUser> sortedList,
			int firstIndex, int lastIndex, Set<Integer> excludedIndices,
			Random rand) {
		int result = -1;
		if (firstIndex >= 0 && firstIndex < sortedList.size() && lastIndex >= 0
				&& lastIndex < sortedList.size() && firstIndex <= lastIndex) {
			do {
				int index = rand.nextInt(lastIndex + 1);
				if (index >= firstIndex && !excludedIndices.contains(index)) {
					result = index;
				}
			} while (result == -1);
		}
		return result;
	}

	/**
	 * <p>
	 * Thanks to tsmorton for suggesting this functionality and writing part of
	 * the code.
	 * </p>
	 *
	 * @see GenericItemSimilarity#GenericItemSimilarity(Iterable, int)
	 * @see GenericItemSimilarity#GenericItemSimilarity(org.apache.mahout.cf.taste.similarity.ItemSimilarity,
	 *      org.apache.mahout.cf.taste.model.DataModel, int)
	 */
	public static List<GenericItemSimilarity.ItemItemSimilarity> getTopItemItemSimilarities(
			int howMany,
			Iterator<GenericItemSimilarity.ItemItemSimilarity> allSimilarities) {

		Queue<GenericItemSimilarity.ItemItemSimilarity> topSimilarities = new PriorityQueue<GenericItemSimilarity.ItemItemSimilarity>(
				howMany + 1, Collections.reverseOrder());
		boolean full = false;
		double lowestTopValue = Double.NEGATIVE_INFINITY;
		while (allSimilarities.hasNext()) {
			GenericItemSimilarity.ItemItemSimilarity similarity = allSimilarities
					.next();
			double value = similarity.getValue();
			if (!Double.isNaN(value) && (!full || value > lowestTopValue)) {
				topSimilarities.add(similarity);
				if (full) {
					topSimilarities.poll();
				} else if (topSimilarities.size() > howMany) {
					full = true;
					topSimilarities.poll();
				}
				lowestTopValue = topSimilarities.peek().getValue();
			}
		}
		int size = topSimilarities.size();
		if (size == 0) {
			return Collections.emptyList();
		}
		List<GenericItemSimilarity.ItemItemSimilarity> result = Lists
				.newArrayListWithCapacity(size);
		result.addAll(topSimilarities);
		Collections.sort(result);
		return result;
	}

	public static List<GenericUserSimilarity.UserUserSimilarity> getTopUserUserSimilarities(
			int howMany,
			Iterator<GenericUserSimilarity.UserUserSimilarity> allSimilarities) {

		Queue<GenericUserSimilarity.UserUserSimilarity> topSimilarities = new PriorityQueue<GenericUserSimilarity.UserUserSimilarity>(
				howMany + 1, Collections.reverseOrder());
		boolean full = false;
		double lowestTopValue = Double.NEGATIVE_INFINITY;
		while (allSimilarities.hasNext()) {
			GenericUserSimilarity.UserUserSimilarity similarity = allSimilarities
					.next();
			double value = similarity.getValue();
			if (!Double.isNaN(value) && (!full || value > lowestTopValue)) {
				topSimilarities.add(similarity);
				if (full) {
					topSimilarities.poll();
				} else if (topSimilarities.size() > howMany) {
					full = true;
					topSimilarities.poll();
				}
				lowestTopValue = topSimilarities.peek().getValue();
			}
		}
		int size = topSimilarities.size();
		if (size == 0) {
			return Collections.emptyList();
		}
		List<GenericUserSimilarity.UserUserSimilarity> result = Lists
				.newArrayListWithCapacity(size);
		result.addAll(topSimilarities);
		Collections.sort(result);
		return result;
	}

	public interface Estimator<T> {
		double estimate(T thing) throws TasteException;
	}

	/**
	 * @param allUserIDs
	 * @param estimator
	 * @param percentile
	 *            0 to 100
	 * @return
	 * @throws TasteException
	 */
	//public static double computeSimilarityThreshold(
			//LongPrimitiveIterator allUserIDs, Estimator<Long> estimator,
			//double percentile) throws TasteException {
		//System.err.println("CalledComputeSimilarityThreshold. SHould now never be called exiting");
		//System.exit(-1);
		//HashSet<Double> simValues = new HashSet<Double>();
		//while (allUserIDs.hasNext()) {
			//long userID = allUserIDs.next();

			//double similarity;
			//try {
				//similarity = estimator.estimate(userID);
				//similarity *=100;
				//similarity=Math.round(similarity);
				//similarity /=100;
				//simValues.add(similarity);
			//} catch (NoSuchUserException nsue) {
				//System.err.println("No such user "+userID);
				//continue;
			//}
		//}
		//ArrayList<Double> sortedValues = new ArrayList<Double>(simValues);
		//Collections.sort(sortedValues);
		//int maxPos = sortedValues.size() - 1;
		//int position = (int) (Math.round(maxPos) * percentile / 100);
		//double toRet = sortedValues.get(position);
		//System.out.println("weird returning " + percentile + " for user "
				//+ estimator.toString() + " pos=" + position + " of " + maxPos
				//+ " simValue: " + toRet);
		//return toRet;


	/*}*/

	/**
	 * Compute the set of similarity values using estimator (ie an similarity measure and a fixed user) with all the other users in allUserIDs.
	 * percentile argument seems useless here.
	 */
	public static HashSet<Double> computeSimilarityValues(
			LongPrimitiveIterator allUserIDs, Estimator<Long> estimator,
			double percentile) throws TasteException {

		HashSet<Double> simValues = new HashSet<Double>();
		while (allUserIDs.hasNext()) {
			long userID = allUserIDs.next();

			double similarity;
			try {
				similarity = estimator.estimate(userID);
				similarity *=100;
				similarity=Math.round(similarity);
				similarity /=100;
				simValues.add(similarity);
			} catch (NoSuchUserException nsue) {
				System.err.println("No such user "+userID);
				continue;
			}
		}
		return simValues;


	}


}
