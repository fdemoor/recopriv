package recoPrivResearch.attackEvaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TLongDoubleMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongDoubleHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TLongIterator;

import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.eval.DataModelBuilder;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import recoPrivResearch.tools.Parameters;
import recoPrivResearch.tools.ExceptHandler;
import recoPrivResearch.dataModelBuilder.SybilModelBuilder;
import recoPrivResearch.recommenderBuilder.KNNRecommenderBuilder;
import recoPrivResearch.recommenderBuilder.NeighborhoodRecommenderBuilder;

/**
 * Evaluate the success of attacking target users by taking over their neighborhood with Sybil users.
 * The main entry point is evaluate.
 * For each target user, k Sybil users are created with a profile made of some
 * auxiliary items known to be in the target user's profile.
 */

public class NearestNeighborSybilAttackEvaluator implements AttackEvaluator {

	private static final Logger resultsLogger = LogManager.getLogger(NearestNeighborSybilAttackEvaluator.class.getName() + "-results");
	private static final Logger resultsPerRoundLogger = LogManager.getLogger(NearestNeighborSybilAttackEvaluator.class.getName() + "-resultsPerRound");
	private static final Logger resultsPerTargetLogger = LogManager.getLogger(NearestNeighborSybilAttackEvaluator.class.getName() + "-resultsPerTarget");
	private static final Logger resultsDetailsLogger = LogManager.getLogger(NearestNeighborSybilAttackEvaluator.class.getName() + "-resultsDetails");
	private static final Logger resultsAvgByNbPerfectMatchesClassLogger = LogManager.getLogger(NearestNeighborSybilAttackEvaluator.class.getName() + "-resultsByNbPerfectMatchesClass");
	private static final Logger resultsAvgByNbPerfectMatchesClassWTargetOnlyLogger = LogManager.getLogger(NearestNeighborSybilAttackEvaluator.class.getName() + "-resultsByNbPerfectMatchesClassWTargetOnly");
	private static final Logger logger = LogManager.getLogger(NearestNeighborSybilAttackEvaluator.class);
	private final boolean adaptiveSybils;
	private final int adaptiveSybilsNbRounds;
	private final int nbReco;
	private final double likeThreshold;
	private final boolean unknownRatingIsDislike;
	private final int neighborhoodSize;
	private final String neighborChoiceBehavior;
	private final TLongSet realUsers;
	private final Random rand;
	private final long neighorhoodsSeed;

	public NearestNeighborSybilAttackEvaluator(Parameters p, Random r) {
		adaptiveSybils = p.adaptiveSybils_;
		adaptiveSybilsNbRounds = p.adaptiveSybilsNbRounds_;
		nbReco = p.sybilsNbRecoPerRound_;
		likeThreshold = p.likeThreshold_;
		unknownRatingIsDislike = p.notRatedItemConsideredDisliked_;
		neighborhoodSize = p.k_;
		neighborChoiceBehavior = p.neighborChoiceBehavior_;
		realUsers = new TLongHashSet();
		rand = r;
		neighorhoodsSeed = rand.nextLong();
	}

	/**
	 * builder should be a KNNRecommenderBuilder, and modelBuilder should be a SybilModelBuilder.
	 */
	public AttackStats evaluate(RecommenderBuilder builder, DataModelBuilder modelBuilder, DataModel originalModel) {

		gatherAllUserIDs(realUsers, ExceptHandler.getModelUserIDs(originalModel));

		DataModel attackedModel = ExceptHandler.injectSybils(modelBuilder, originalModel);

		SybilModelBuilder sybilBuilder = (SybilModelBuilder) modelBuilder;
		TLongObjectMap<TLongArrayList> auxItems = sybilBuilder.getAuxiliaryItems();
		TLongObjectMap<TLongArrayList> sybilIDs = sybilBuilder.getSybils();

		logger.info("Building the recommendation system...");
		Recommender rec = ExceptHandler.buildRecSys(builder, attackedModel);

		logger.info("Performing attack and measuring its effect...");
		AttackStats result = measureAttackSuccess(builder, rec, sybilBuilder, sybilIDs, auxItems);

		logger.info("Logging results...");
		logResults(result, sybilBuilder.getAttackedUsers());

		return result;
	}

	/**
	 * Measure some statistics about the attack.
	 * Sybil users' behaviour may be adaptive (integrate recommended items in their profile) depending on attribute adaptiveSybils.
	 */
	private AttackStats measureAttackSuccess(RecommenderBuilder builder, Recommender recsys, SybilModelBuilder sybilBuilder, TLongObjectMap<TLongArrayList> sybilUserIDs, TLongObjectMap<TLongArrayList> auxiliaryItems) {
		AttackStats result;
		NeighborhoodRecommenderBuilder castedBuilder = (NeighborhoodRecommenderBuilder) builder;
		Random localRand = new Random(neighorhoodsSeed);
		//Random localRandBis = new Random(neighorhoodsSeed);

		if(!adaptiveSybils) {
			logger.info("Generating recommendations for Sybil users...");
			TLongObjectMap<List<RecommendedItem>> recommendations = getRecoForSybils(recsys, sybilUserIDs, auxiliaryItems, sybilBuilder.getLearntItems(), localRand.nextLong());
			UserNeighborhood neighborhood = castedBuilder.getNeighborhood();
			DataModel model = recsys.getDataModel();
			UserSimilarity cosim = ((GenericUserBasedRecommender) recsys).getSimilarity();

			/*if(logger.isDebugEnabled()) {*/
				//checkTargetsAndSybilsNeighborhoods(model, neighborhood, sybilUserIDs, cosim, neighborChoiceBehavior, localRandBis);
			/*}*/

			logger.info("Measuring statistics...");
			result = measureStats(recommendations, neighborhood, sybilUserIDs, model, cosim, localRand);
		} else {
			AttackStats avgStats = new AttackStats();
			Recommender rec = recsys;
			for(int i=0; i<adaptiveSybilsNbRounds; i++) {
				logger.info("Round {}: Generating recommendations for Sybil users...", i);
				TLongObjectMap<List<RecommendedItem>> recommendations = getRecoForSybils(rec, sybilUserIDs, auxiliaryItems, sybilBuilder.getLearntItems(), localRand.nextLong());
				UserNeighborhood neighborhood = castedBuilder.getNeighborhood();
				DataModel model = rec.getDataModel();
				UserSimilarity cosim = ((GenericUserBasedRecommender) rec).getSimilarity();

				if(logger.isDebugEnabled()) {
					//checkTargetsAndSybilsNeighborhoods(model, neighborhood, sybilUserIDs, cosim, neighborChoiceBehavior, localRand);
					logSybilsAndTargetSimilarity(cosim, sybilUserIDs);
				}

				logger.info("Round {}: Measuring statistics...", i);
				avgStats = AttackStats.add(avgStats, measureStats(recommendations, neighborhood, sybilUserIDs, model, cosim, localRand));

				logger.info("Round {}: Updating DataModel and recommendation system...", i);
				rec = updateModelAndRecSys(builder, sybilBuilder, model, recommendations);
			}
			//result = AttackStats.average(avgStats, adaptiveSybilsNbRounds);
			result = avgStats;
		}

		return result;
	}

	/**
	 * Debug function to log the similarity between each target user and its Sybils.
	 * As Sybils are all supposed to have the same profile, this function uses the similarity value with the 1st Sybil of targetsSybilIDs.
	 */
	private static void logSybilsAndTargetSimilarity(UserSimilarity sim, TLongObjectMap<TLongArrayList> sybilUserIDs) {
		for(TLongIterator it=sybilUserIDs.keySet().iterator(); it.hasNext();) {
			long targetID = it.next();
			TLongArrayList targetsSybilIDs = sybilUserIDs.get(targetID);
			logger.debug("Similarity between target {} and its Sybils = {}", targetID, ExceptHandler.getSim(sim, targetID, targetsSybilIDs.get(0)));
		}
	}

	/**
	 * Generate one list of unique RecommendedItem per target user using all recommendations received by Sybil users.
	 * These lists do not include auxiliaryItems even if some got recommended to Sybil users.
	 * How many recommendations each Sybil user receives is controlled by the nbReco attribute.
	 */
	private TLongObjectMap<List<RecommendedItem>> getRecoForSybils(Recommender recsys, TLongObjectMap<TLongArrayList> sybilUserIDs, TLongObjectMap<TLongArrayList> auxiliaryItems, TLongObjectMap<TLongArrayList> learntItems, long seed) {
		TLongObjectMap<List<RecommendedItem>> result = new TLongObjectHashMap<>(sybilUserIDs.size());
		Random localRand = new Random(seed);

		for(TLongIterator it=sybilUserIDs.keySet().iterator(); it.hasNext();) {
			long targetID = it.next();
			List<RecommendedItem> allRecoForTargetsSybils = new ArrayList<>();

			TLongArrayList targetsSybilIDs = sybilUserIDs.get(targetID);
			for(TLongIterator iter=targetsSybilIDs.iterator(); iter.hasNext();) {
				long sybilID = iter.next();
				List<RecommendedItem> recos = ExceptHandler.getRecommendations(recsys, sybilID, nbReco, neighborChoiceBehavior, localRand);

				logger.debug("Target {}, Sybil {}, recommendations: {}", targetID, sybilID, recos);

				allRecoForTargetsSybils.addAll(recos);
			}
			logger.debug("Sybil users targeting user {} have learnt {} items (with duplicates)", targetID, allRecoForTargetsSybils.size());

			removeAllFrom(auxiliaryItems.get(targetID), allRecoForTargetsSybils);
			NearestNeighborSybilAttackEvaluator.removeDuplicatedRecos(allRecoForTargetsSybils);

			result.put(targetID, allRecoForTargetsSybils);
		}

		return result;
	}

	/**
	 * Remove all elements in toBeRemoved from list, based on the itemID.
	 */
	private void removeAllFrom(TLongArrayList toBeRemoved, List<RecommendedItem> list) {
		int nbRemoved = 0;
		for(Iterator<RecommendedItem> it=list.iterator(); it.hasNext();) {
			if(toBeRemoved.contains(it.next().getItemID())) {
				it.remove();
				nbRemoved++;
			}
		}
		logger.debug("Removed {} occurences of auxiliary items", nbRemoved);
	}

	/**
	 * For each itemID, leave 1 corresponding RecommendedItem in list.
	 * Does not take recommended item's value into account to choose which one
	 * to leave in list.
	 */
	private static void removeDuplicatedRecos(List<RecommendedItem> list) {
		int nbRemoved = 0;
		TLongSet itemIDs = new TLongHashSet();
		for(Iterator<RecommendedItem> it=list.iterator(); it.hasNext();) {
			RecommendedItem reco = it.next();
			if(itemIDs.contains(reco.getItemID())) {
				it.remove();
				nbRemoved++;
			} else {
				itemIDs.add(reco.getItemID());
			}
		}
		logger.debug("Removed {} occurences of duplicated items", nbRemoved);
	}

	/**
	 * Compute and gather in one object 3 measures about one round of the attack on all target users.
	 * Measures (1 value of each per target user) are:
	 * Sybil infiltration: the fraction of a neighborhood which is made of Sybil users.
	 * Sybil neighborhood check: Are Sybil neighborhoods made of k-1 Sybils and the target? How many Sybils have the target as neighbor? How many other Sybils in their neighborhood?
	 * Yield: the number of items learnt (regardless of their like status).
	 * Accuracy: the fraction of items liked by the target user among the items learnt.
	 * Nb perfect matches: how many real users in the dataset have a cosine similarity of 1 with the target user.
	 * The behavior regarding learnt items on which the target has no opinion is controlled by unknownRatingIsDislike.
	 */
	private AttackStats measureStats(TLongObjectMap<List<RecommendedItem>> recommendations, UserNeighborhood neighborhood, TLongObjectMap<TLongArrayList> sybilIDs, DataModel model, UserSimilarity cosim, Random rand) {
		AttackStats result = new AttackStats();

		TLongSet targetIDs = recommendations.keySet();

		result.setNbPerfectMatches(computeNbPerfectMatches(model, targetIDs, cosim));

		computeSybilNeighborhoodsChecks(targetIDs, sybilIDs, neighborhood, result, rand);

		result.setSybilInfiltration(computeNeighborhoodInfiltration(targetIDs, sybilIDs, neighborhood, rand));

		result.setYield(computeYield(targetIDs, recommendations));

		computeAccuracies(targetIDs, recommendations, model, result);

		computeStricterAccuracies(targetIDs, recommendations, model, result);

		result.setNbAuxiliaryItems(computeNbAuxiliaryItems(model));

		return result;
	}

	// Compute the fraction of Sybil infiltration for each neighborhood
	private TLongDoubleMap computeNeighborhoodInfiltration(TLongSet targetIDs, TLongObjectMap<TLongArrayList> sybilIDs, UserNeighborhood neighborhood, Random rand) {
		TLongDoubleMap neighborhoodInfiltration = new TLongDoubleHashMap(targetIDs.size());
		int nbNoNeighbors = 0;
		for(TLongIterator it=targetIDs.iterator(); it.hasNext();) {
			long targetID = it.next();
			TLongArrayList targetsSybilIDs = sybilIDs.get(targetID);
			long[] targetNeighborhood = ExceptHandler.getNeighborhood(neighborhood, targetID, neighborChoiceBehavior, rand);

			logger.debug("Target user {} has neighbors {}", targetID, targetNeighborhood);

			if(targetNeighborhood != null) { // if targetID has no neighbors, he is not included in neighborhoodInfiltration
				neighborhoodInfiltration.put(targetID, getInfiltrationFraction(targetsSybilIDs, targetNeighborhood));
			} else {
				logger.warn("The neighborhood of user {} is null", targetID);
				nbNoNeighbors++;
			}
		}
		if(nbNoNeighbors > 0) {
			logger.warn("{} target users have no neighbors", nbNoNeighbors);
		}
		return neighborhoodInfiltration;
	}

	// Check if each Sybils neighborhood is as expected: k-1 Sybils + target user
	private void computeSybilNeighborhoodsChecks(TLongSet targetIDs, TLongObjectMap<TLongArrayList> sybilIDs, UserNeighborhood neighborhood, AttackStats result, Random rand) {
		int nbTargets = targetIDs.size();
		TLongDoubleMap fracOfExpectedNeighborhoods = new TLongDoubleHashMap(nbTargets);
		TLongDoubleMap targetsAreNeighbors = new TLongDoubleHashMap(nbTargets);
		TLongDoubleMap targetsNbSybilNeighbors = new TLongDoubleHashMap(nbTargets);
		TLongDoubleMap fracOfUnwantedNeighbors = new TLongDoubleHashMap(nbTargets);
		TLongDoubleMap fracOfUnwantedNeighborsWithTargetOnly = new TLongDoubleHashMap(nbTargets);

		for(TLongIterator it=targetIDs.iterator(); it.hasNext();) { // For each target user (targetID)
			long targetID = it.next();
			TLongArrayList sybils = sybilIDs.get(targetID);
			int nbSybils = sybils.size();
			double fracSybilNeighbors = 0;
      int onlySybils = 0;
			double expectedNeighborhoodFraction = 0.0; // Fraction of targetID's Sybils have the expected neighborhood composition
			int nbTargetIsNeighbor = 0; // How many of targetID's Sybils have targetID in their neighborhood
			TDoubleArrayList nbSybilNeighbors = new TDoubleArrayList(nbSybils); // Fraction of Sybil neighbors for each Sybil of targetID
			TDoubleArrayList fractionsOfUnwantedNeighbors = new TDoubleArrayList(nbSybils); // For each Sybil, the fraction of unwanted neighbors
			TDoubleArrayList fractionsOfUnwantedNeighborsWTargetOnly = new TDoubleArrayList(); // For each Sybil who has target as neighbor, the fraction of unwanted neighbors
			for(int i=0; i<nbSybils; i++) { // For each Sybil user (sybilID) targeting targetID
				long sybilID = sybils.get(i);
				long[] sybilNeighborhood = ExceptHandler.getNeighborhood(neighborhood, sybilID, neighborChoiceBehavior, rand);

				//if(result.getNbPerfectMatchesAt(1).get(targetID)==0) {
					logger.debug("Sybil {} (target={}, nbPerfectMatches={}) has neighbors {}", sybilID, targetID, result.getNbPerfectMatchesAt(1).get(targetID), sybilNeighborhood);
				//}

				if(sybilNeighborhood != null) {
					int nbSybilsNeighbors = 0;
					boolean targetIsNeighbor = false;
					int nbUnwantedNeighbors = 0;
					for(int j=0; j<sybilNeighborhood.length; j++) { // For each neighbor of sybilID
						if(sybils.contains(sybilNeighborhood[j])) {
							nbSybilsNeighbors++;
						} else if(sybilNeighborhood[j] == targetID) {
							targetIsNeighbor = true;
							nbTargetIsNeighbor++;
						} else {
							nbUnwantedNeighbors++;
						}
					}
					nbSybilNeighbors.add((nbSybilsNeighbors / (double) (neighborhoodSize - 1)));
					double fraction = nbUnwantedNeighbors / (double) neighborhoodSize;

					logger.debug("target {}, sybil {}, unwanted neighbors = {}", targetID, sybilID, fraction);

					fractionsOfUnwantedNeighbors.add(fraction);
					if(targetIsNeighbor) {
						fractionsOfUnwantedNeighborsWTargetOnly.add(fraction);

						logger.debug("sybil {} has target {} as a neighbor", sybilID, targetID);
					}

					if (nbSybilsNeighbors == (neighborhoodSize-1)) {
						fracSybilNeighbors += 1.0;
					} else if (nbSybilsNeighbors == neighborhoodSize) {
            onlySybils++;
          }

					if(targetIsNeighbor && nbSybilsNeighbors == (neighborhoodSize-1)) {
						expectedNeighborhoodFraction++;
					}
				} else {
					logger.warn("The neighborhood of Sybil user {} is null", sybilID);
				}
			}
			
			expectedNeighborhoodFraction = expectedNeighborhoodFraction / (double) nbSybils;
			double targetIsNeighborFraction = nbTargetIsNeighbor / (double) nbSybils;
			//double targetsNbSybilNeighborsFraction = AttackStats.averageIntArrayList(nbSybilNeighbors) / (double) (neighborhoodSize-1);

      result.setAllSybilsOnlySybilNeighbors((double) onlySybils / (double) nbSybils);
      
			result.setFracSybilsHaveAllSybilNeighbors(fracSybilNeighbors / nbSybils);
			fracOfExpectedNeighborhoods.put(targetID, expectedNeighborhoodFraction);
			targetsAreNeighbors.put(targetID, targetIsNeighborFraction);
			targetsNbSybilNeighbors.put(targetID, AttackStats.averageDoubleArrayList(nbSybilNeighbors));
			fracOfUnwantedNeighbors.put(targetID, AttackStats.averageDoubleArrayList(fractionsOfUnwantedNeighbors));

			if(fractionsOfUnwantedNeighborsWTargetOnly.size() > 0) {
				logger.debug("average fraction of unwanted w/ target only = {}, averaged fractions: {}", AttackStats.averageDoubleArrayList(fractionsOfUnwantedNeighborsWTargetOnly), fractionsOfUnwantedNeighborsWTargetOnly);
				fracOfUnwantedNeighborsWithTargetOnly.put(targetID, AttackStats.averageDoubleArrayList(fractionsOfUnwantedNeighborsWTargetOnly));
			}
		}
		result.setExpectedNeighborhoodsFrac(fracOfExpectedNeighborhoods);
		result.setTargetsAreNeighbors(targetsAreNeighbors);
		result.setTargetsNbSybilNeighbors(targetsNbSybilNeighbors);
		result.setFracOfUnwantedNeighbors(fracOfUnwantedNeighbors);
		result.setFracOfUnwantedNeighborsWTargetOnly(fracOfUnwantedNeighborsWithTargetOnly);
	}

	// Compute attack yield (nb items learnt)/target user
	private TLongIntMap computeYield(TLongSet targetIDs, TLongObjectMap<List<RecommendedItem>> recommendations) {
		TLongIntMap yield = new TLongIntHashMap(targetIDs.size());
		for(TLongIterator it=targetIDs.iterator(); it.hasNext();) {
			long targetID = it.next();
			int nbLearntItems = recommendations.get(targetID).size();

			if(nbLearntItems == 0) {
				logger.warn("Sybil users targeting user {} did not learn anything", targetID);
			}

			yield.put(targetID, nbLearntItems);
		}
		return yield;
	}

	// Compute attack accuracy(nb rated items learnt/nb items learnt)/target user
	// Requires only an item to be rated in target's profile
	private void computeAccuracies(TLongSet targetIDs, TLongObjectMap<List<RecommendedItem>> recommendations, DataModel model, AttackStats result) {
		TLongDoubleMap accuracies = new TLongDoubleHashMap(targetIDs.size());
		TLongIntMap absAccuracies = new TLongIntHashMap(targetIDs.size());
    
    int fakeItems = 0;
    
    /* Compute max item ID to know if item is fake or not */
    long maxItemID = 0;
		try {
			for (LongPrimitiveIterator it = model.getItemIDs(); it.hasNext();) {
				long itemID = it.nextLong();
				if (itemID > maxItemID) {
					maxItemID = itemID;
				}
			}
		} catch (TasteException ex) {}
    
		for(TLongIterator it=targetIDs.iterator(); it.hasNext();) {
			long targetID = it.next();
			int accuracy = 0;
			double accuracyPercent = 0.0;
      double accuracyPercentPrime = 0.0;
			int nbLearntItems = recommendations.get(targetID).size();

			logger.debug("Sybil users targeting user {} have learnt {} items", targetID, nbLearntItems);
			if(nbLearntItems > 0) {
				for(Iterator<RecommendedItem> iter=recommendations.get(targetID).iterator(); iter.hasNext();) {
					long itemID = iter.next().getItemID();
          if (itemID <= maxItemID) {
            float rating = ExceptHandler.getItemValue(model, targetID, itemID);
            if(rating == -1.0) {
              logger.debug("user {} has no opinion on item {}", targetID, itemID);
            } else {
              accuracy++;
            }
          } else {
            fakeItems++;
          }
				}
				accuracyPercent = accuracy / (double) nbLearntItems;
				accuracyPercentPrime = accuracy / (double) (nbLearntItems - fakeItems);
			}
			accuracies.put(targetID, accuracyPercent);
			absAccuracies.put(targetID, accuracy);
      result.setAccuracyPrime(accuracyPercentPrime);
		}
		result.setAccuracy(accuracies);
		result.setAbsAccuracy(absAccuracies);
	}

	// Compute attack accuracy(nb liked items learnt/nb items learnt)/target user
	// Stricter version requiring learnt item to be in target's profile and have a rating greater or equal to likeThreshold.
	private void computeStricterAccuracies(TLongSet targetIDs, TLongObjectMap<List<RecommendedItem>> recommendations, DataModel model, AttackStats result) {
		TLongDoubleMap accuracies = new TLongDoubleHashMap(targetIDs.size());
		TLongIntMap absAccuracies = new TLongIntHashMap(targetIDs.size());
    
    int fakeItems = 0;
    
    /* Compute max item ID to know if item is fake or not */
    long maxItemID = 0;
		try {
			for (LongPrimitiveIterator it = model.getItemIDs(); it.hasNext();) {
				long itemID = it.nextLong();
				if (itemID > maxItemID) {
					maxItemID = itemID;
				}
			}
		} catch (TasteException ex) {}
    
		for(TLongIterator it=targetIDs.iterator(); it.hasNext();) {
			long targetID = it.next();
			int accuracy = 0;
			double accuracyPercent = 0.0;
      double accuracyPercentPrime = 0.0;
			int nbLearntItems = recommendations.get(targetID).size();

			logger.debug("Sybil users targeting user {} have learnt {} items", targetID, nbLearntItems);
			if(nbLearntItems > 0) {
				for(Iterator<RecommendedItem> iter=recommendations.get(targetID).iterator(); iter.hasNext();) {
					long itemID = iter.next().getItemID();
          if (itemID <= maxItemID) {
            float rating = ExceptHandler.getItemValue(model, targetID, itemID);
            if(rating == -1.0) {
              logger.debug("user {} has no opinion on item {}", targetID, itemID);
            }
            /*if(!unknownRatingIsDislike && rating != -1.0) {*/
              //rating = (float) likeThreshold;
            /*}*/
            if(rating >= likeThreshold) {
              accuracy++;
            }
          } else {
            fakeItems++;
          }
				}
				accuracyPercent = accuracy / (double) nbLearntItems;
				accuracyPercentPrime = accuracy / (double) (nbLearntItems - fakeItems);
			}
			accuracies.put(targetID, accuracyPercent);
			absAccuracies.put(targetID, accuracy);
      result.setAccuracyPrime(accuracyPercentPrime);
		}
		result.setStricterAccuracy(accuracies);
		result.setStricterAbsAccuracy(absAccuracies);
	}

	// Compute how many real users have a cosine similarity of 1 with each target user
	private TLongIntMap computeNbPerfectMatches(DataModel model, TLongSet targetIDs, UserSimilarity cosim) {
		TLongIntMap nbPerfectMatches = new TLongIntHashMap(targetIDs.size());

		for(TLongIterator it=targetIDs.iterator(); it.hasNext();) {
			long targetID = it.next();
			int nbMatches = 0;
			for(TLongIterator iter=realUsers.iterator(); iter.hasNext();) {
				long realUser = iter.next();
				double x = ExceptHandler.getSim(cosim, targetID, realUser);
				if(realUser != targetID && x == 1.0) {
					nbMatches++;
				}
			}
			nbPerfectMatches.put(targetID, nbMatches);
		}

		return nbPerfectMatches;
	}

	private int computeNbAuxiliaryItems(DataModel model) {
		return SybilModelBuilder.getNbAuxiliaryItems();
	}

	private void logResults(AttackStats result, TLongSet attackedUsers) {
		logDetailedResults(result);
		logAvgResultsByNbPerfectMatchesClass(result);
		if(adaptiveSybils) {
			logAvgResultsPerRound(result);
		}
		logAvgResultsPerTarget(result, attackedUsers);
		logAvgResults(result);
	}

	private void logDetailedResults(AttackStats result) {
		for(int i=1; i<=result.getNbRounds(); i++) {
			TLongDoubleMap sybilInfiltrations = result.getSybilInfiltrationsAt(i);
			TLongIntMap yields = result.getYieldsAt(i);
			TLongDoubleMap accuracies = result.getAccuraciesAt(i);
			TLongDoubleMap stricterAccuracies = result.getStricterAccuraciesAt(i);
			TLongIntMap absAccuracies = result.getAbsAccuraciesAt(i);
			TLongIntMap stricterAbsAccuracies = result.getStricterAbsAccuraciesAt(i);
			TLongDoubleMap expectedNeighborhoods = result.getExpectedNeighborhoodsFracAt(i);
			TLongDoubleMap targetsAreNeighbors = result.getTargetsAreNeighborsAt(i);
			TLongDoubleMap targetsNbSybilNeighbors = result.getTargetsNbSybilNeighborsAt(i);
			TLongIntMap nbPerfectMatches = result.getNbPerfectMatchesAt(i);
			TLongDoubleMap fracOfUnwantedNeighbors = result.getFracOfUnwantedNeighborsAt(i);
			TLongDoubleMap fracOfUnwantedNeighborsWTargetOnly = result.getFracOfUnwantedNeighborsWTargetOnlyAt(i);

			for(TLongIterator it=sybilInfiltrations.keySet().iterator(); it.hasNext();) {
				long target = it.next();
				resultsDetailsLogger.info("{},{},{},{},{},{},{},{},{},{},{},{},{},{}", i, target, sybilInfiltrations.get(target),
						yields.get(target), accuracies.get(target), stricterAccuracies.get(target), expectedNeighborhoods.get(target),
						targetsAreNeighbors.get(target), targetsNbSybilNeighbors.get(target),
						fracOfUnwantedNeighbors.get(target), fracOfUnwantedNeighborsWTargetOnly.get(target),
						nbPerfectMatches.get(target), absAccuracies.get(target), stricterAbsAccuracies.get(target));
			}
		}
	}

	private void logAvgResultsByNbPerfectMatchesClass(AttackStats result) {
		for(int i=1; i<=result.getNbRounds(); i++) {
			TLongDoubleMap sybilInfiltrations = result.getSybilInfiltrationsAt(i);
			TLongIntMap yields = result.getYieldsAt(i);
			TLongDoubleMap accuracies = result.getAccuraciesAt(i);
			TLongDoubleMap stricterAccuracies = result.getStricterAccuraciesAt(i);
			TLongIntMap absAccuracies = result.getAbsAccuraciesAt(i);
			TLongIntMap stricterAbsAccuracies = result.getStricterAbsAccuraciesAt(i);
			TLongDoubleMap expectedNeighborhoods = result.getExpectedNeighborhoodsFracAt(i);
			TLongDoubleMap targetsAreNeighbors = result.getTargetsAreNeighborsAt(i);
			TLongDoubleMap targetsNbSybilNeighbors = result.getTargetsNbSybilNeighborsAt(i);
			TLongIntMap nbPerfectMatches = result.getNbPerfectMatchesAt(i);
			TLongDoubleMap fracOfUnwantedNeighbors = result.getFracOfUnwantedNeighborsAt(i);
			TLongDoubleMap fracOfUnwantedNeighborsWTargetOnly = result.getFracOfUnwantedNeighborsWTargetOnlyAt(i);

			TIntObjectMap<TLongArrayList> usersByNbPerfectMatchesClass = sortUsersByNbPerfectMatchesClass(nbPerfectMatches);

			//for(TIntIterator it=usersByNbPerfectMatchesClass.keySet().iterator(); it.hasNext();) {
			int[] classes = usersByNbPerfectMatchesClass.keys();
			Arrays.sort(classes);
			for(int nbPerfectMatchesClass : classes) {
				//int nbPerfectMatchesClass = it.next();
				TLongArrayList usersOfTheClass = usersByNbPerfectMatchesClass.get(nbPerfectMatchesClass);
				resultsAvgByNbPerfectMatchesClassLogger.info("{},{},{},{},{},{},{},{},{},{},{},{},{},{}", i, nbPerfectMatchesClass, usersOfTheClass.size(),
						avgLongDoubleMapByClass(sybilInfiltrations, usersOfTheClass),
						avgLongIntMapByClass(yields, usersOfTheClass), avgLongDoubleMapByClass(accuracies, usersOfTheClass),
						avgLongDoubleMapByClass(stricterAccuracies, usersOfTheClass), avgLongDoubleMapByClass(expectedNeighborhoods, usersOfTheClass),
						avgLongDoubleMapByClass(targetsAreNeighbors, usersOfTheClass), avgLongDoubleMapByClass(targetsNbSybilNeighbors, usersOfTheClass),
						avgLongDoubleMapByClass(fracOfUnwantedNeighbors, usersOfTheClass), avgLongDoubleMapByClass(fracOfUnwantedNeighborsWTargetOnly, usersOfTheClass),
						avgLongIntMapByClass(absAccuracies, usersOfTheClass), avgLongIntMapByClass(stricterAbsAccuracies, usersOfTheClass));
			}

			for(int nbPerfectMatchesClass : classes) {
				TLongArrayList usersOfTheClass = usersByNbPerfectMatchesClass.get(nbPerfectMatchesClass);
				removeUsersWithoutSybilsWithTarget(usersOfTheClass, fracOfUnwantedNeighborsWTargetOnly);
				if(usersOfTheClass.size() > 0) {
					resultsAvgByNbPerfectMatchesClassWTargetOnlyLogger.info("{},{},{},{},{},{},{},{},{},{},{},{},{},{}", i, nbPerfectMatchesClass, usersOfTheClass.size(),
							avgLongDoubleMapByClass(sybilInfiltrations, usersOfTheClass),
							avgLongIntMapByClass(yields, usersOfTheClass), avgLongDoubleMapByClass(accuracies, usersOfTheClass),
							avgLongDoubleMapByClass(stricterAccuracies, usersOfTheClass), avgLongDoubleMapByClass(expectedNeighborhoods, usersOfTheClass),
							avgLongDoubleMapByClass(targetsAreNeighbors, usersOfTheClass), avgLongDoubleMapByClass(targetsNbSybilNeighbors, usersOfTheClass),
							avgLongDoubleMapByClass(fracOfUnwantedNeighbors, usersOfTheClass), avgLongDoubleMapByClass(fracOfUnwantedNeighborsWTargetOnly, usersOfTheClass),
							avgLongIntMapByClass(absAccuracies, usersOfTheClass), avgLongIntMapByClass(stricterAbsAccuracies, usersOfTheClass));
				}
			}
		}
	}

	/*private void logAvgResultsByNbPerfectMatchesClassWTargetOnly(AttackStats result) {*/
		//for(int i=1; i<=result.getNbRounds(); i++) {
			//TLongIntMap nbPerfectMatches = result.getNbPerfectMatchesAt(i);
			//TLongDoubleMap fracOfUnwantedNeighborsWTargetOnly = result.getFracOfUnwantedNeighborsWTargetOnlyAt(i);

			//TIntObjectMap<TLongArrayList> usersByNbPerfectMatchesClass = sortUsersByNbPerfectMatchesClass(nbPerfectMatches);

			//int[] classes = usersByNbPerfectMatchesClass.keys();
			//Arrays.sort(classes);
			//for(int nbPerfectMatchesClass : classes) {
				//TLongArrayList usersOfTheClass = usersByNbPerfectMatchesClass.get(nbPerfectMatchesClass);
				//removeUsersWithoutSybilsWithTarget(usersOfTheClass, fracOfUnwantedNeighborsWTargetOnly);
				//if(usersOfTheClass.size() > 0) {
					//resultsAvgByNbPerfectMatchesClassWTargetOnlyLogger.info("{},{},{},{},{},{},{},{},{},{},{}", i, nbPerfectMatchesClass, usersOfTheClass.size(), avgLongDoubleMapByClass(sybilInfiltrations, usersOfTheClass),
							//avgLongIntMapByClass(yields, usersOfTheClass), avgLongDoubleMapByClass(accuracies, usersOfTheClass), avgLongDoubleMapByClass(expectedNeighborhoods, usersOfTheClass),
							//avgLongDoubleMapByClass(targetsAreNeighbors, usersOfTheClass), avgLongDoubleMapByClass(targetsNbSybilNeighbors, usersOfTheClass),
							//avgLongDoubleMapByClass(fracOfUnwantedNeighbors, usersOfTheClass), avgLongDoubleMapByClass(fracOfUnwantedNeighborsWTargetOnly, usersOfTheClass));
				//}
			//}
		//}
	/*}*/

	private void removeUsersWithoutSybilsWithTarget(TLongArrayList users, TLongDoubleMap usersWithTargetOnly) {
		for(TLongIterator it=users.iterator(); it.hasNext();) {
			if(!usersWithTargetOnly.containsKey(it.next())) {
				it.remove();
			}
		}
	}

	/**
	 * Sort users according to the number of totally similar other real users.
	 */
	private TIntObjectMap<TLongArrayList> sortUsersByNbPerfectMatchesClass(TLongIntMap map) {
		TIntObjectMap<TLongArrayList> result = new TIntObjectHashMap<>();

		for(TLongIterator it=map.keySet().iterator(); it.hasNext();) {
			long user = it.next();
			int nbPerfectMatches = map.get(user);

			TLongArrayList sameClassUsers = null;
			if(result.containsKey(nbPerfectMatches)) {
				sameClassUsers = result.get(nbPerfectMatches);
			} else {
				sameClassUsers = new TLongArrayList();
				result.put(nbPerfectMatches, sameClassUsers);
			}

			sameClassUsers.add(user);
		}

		return result;
	}

	private void logAvgResultsPerRound(AttackStats result) {
		for(int i=1; i<=result.getNbRounds(); i++) {
			resultsPerRoundLogger.info("{},{},{},{},{},{},{},{},{},{},{},{},{}", i, result.getAvgSybilInfiltrationAt(i), result.getAvgYieldAt(i), result.getAvgAccuracyAt(i), result.getAvgStricterAccuracyAt(i),
					result.getAvgExpectedNeighborhoodsFracAt(i), result.getAvgTargetsAreNeighborsAt(i), result.getAvgTargetsNbSybilNeighborsAt(i),
					result.getAvgFracOfUnwantedNeighborsAt(i), result.getAvgFracOfUnwantedNeighborsWTargetOnlyAt(i), result.getAvgNbPerfectMatchesAt(i),
					result.getAvgAbsAccuracyAt(i), result.getAvgStricterAbsAccuracyAt(i));
		}
	}

	private void logAvgResultsPerTarget(AttackStats result, TLongSet attackedUsers) {
		// TODO: add logging of proper neighborhood composition
		// TODO: add logging of fraction of unwanted neighbors (both version w/ and w/o targetIsNeighbor constraint)
		// TODO: add logging of number of perfect matches
		// TODO: add logging of absolute accuracy (stricter and not)
		for(TLongIterator it=attackedUsers.iterator(); it.hasNext();) {
			long user = it.next();
			resultsPerTargetLogger.info("{},{},{},{}", user, result.getAvgSybilInfiltrationFor(user), result.getAvgYieldFor(user), result.getAvgAccuracyFor(user));
		}
	}

	private void logAvgResults(AttackStats result) {
		resultsLogger.info("{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{}", result.getAvgSybilInfiltration(), result.getAvgYield(), result.getAvgAccuracy(), result.getAvgStricterAccuracy(),
				result.getAvgExpectedNeighborhoodsFrac(), result.getAvgTargetsAreNeighbors(), result.getAvgTargetsNbSybilNeighbors(),
				result.getAvgFracOfUnwantedNeighbors(), result.getAvgFracOfUnwantedNeighborsWTargetOnly(), result.getAvgNbPerfectMatches(),
				result.getAvgAbsAccuracy(), result.getAvgStricterAbsAccuracy(), result.getNbAuxiliaryItems(),
        result.getFracSybilsHaveAllSybilNeighbors(), result.getAllSybilsOnlySybilNeighbors(), result.getAccuracyPrime());
	}

	/**
	 * Returns the fraction of users in a neighborhood which are Sybil users.
	 */
	private double getInfiltrationFraction(TLongArrayList sybilIDs, long[] neighborhood) {
		int nbSybils = 0;
		for(int i=0; i<neighborhood.length; i++) {
			if(sybilIDs.contains(neighborhood[i])) {
				nbSybils++;
			}
		}
		return nbSybils / (double) neighborhood.length;
	}

	/**
	 * Update Sybil profiles in model with recommendations, then generate a new Recommender object based on this updated DataModel.
	 */
	private Recommender updateModelAndRecSys(RecommenderBuilder builder, SybilModelBuilder sybilBuilder, DataModel model, TLongObjectMap<List<RecommendedItem>> recommendations) {
		DataModel updatedModel = sybilBuilder.updateSybilProfiles(model, recommendations);
		return ExceptHandler.buildRecSys(builder, updatedModel);
	}

	/**
	 * Debug function logging the neighbors of target and Sybil users, as well as their similarities.
	 */
	public static void checkTargetsAndSybilsNeighborhoods(DataModel model, UserNeighborhood neighborhood, TLongObjectMap<TLongArrayList> sybilUserIDs, UserSimilarity cosim, String neighborChoiceBehavior, Random rand) {
		TLongSet watchedIDs = keysAndValuesSet(sybilUserIDs);

		LongPrimitiveIterator IDsIter = ExceptHandler.getModelUserIDs(model);
		while(IDsIter.hasNext()) {
			long user = IDsIter.nextLong();
			if(watchedIDs.contains(user)) {
				String status = "Sybil";
				if(sybilUserIDs.keySet().contains(user)) {
					status = "Target";
				}

				long[] neighbors = ExceptHandler.getNeighborhood(neighborhood, user, neighborChoiceBehavior, rand);

				// Check that cosim(A,B) == cosim(B,A)
				//logger.trace("cosim({},{}) = {}, cosim({},{}) = {} ", user, neighbors[neighbors.length-1], getSim(cosim, user, neighbors[neighbors.length-1]), neighbors[neighbors.length-1], user, getSim(cosim, neighbors[neighbors.length-1], user));

				double[] similarities = new double[neighbors.length];
				for(int i=0; i<neighbors.length; i++) {
					similarities[i] = ExceptHandler.getSim(cosim, user, neighbors[i]);
				}

				logger.debug("{} user {} has the following neighbors: {}, with similarities {}", status, user, neighbors, similarities);
			}
		}

	}

	// Utility functions
	// Should be moved in their own class

	/**
	 * Returns the set of all long values in the keys and in the values of map.
	 */
	public static TLongSet keysAndValuesSet(TLongObjectMap<TLongArrayList> map) {
		TLongSet result = new TLongHashSet();
		for(long target : map.keys()) {
			result.add(target);
			result.addAll(map.get(target));
		}
		return result;
	}

	/**
	 * Compute the average of the subset of values from map identified by the keys found in usersOfTheClass.
	 */
	public static double avgLongDoubleMapByClass(TLongDoubleMap map, TLongArrayList usersOfTheClass) {
		double result = 0.0;
		int nbValues = 0;

		for(TLongIterator it=usersOfTheClass.iterator(); it.hasNext();) {
			long user = it.next();
			if(map.containsKey(user)) {
				result += map.get(user);
				nbValues++;
			} else { // Should not happen when avgLongDoubleMapByClass is called from logAvgResultsByNbPerfectMatchesClass
				logger.warn("user {} from usersOfTheClass has no mapping in map", user);
			}
		}

		if(nbValues > 0) {
			result = result / (double) nbValues;
		}

		return result;
	}

	/**
	 * Compute the average of the subset of values from map identified by the keys found in usersOfTheClass.
	 */
	public static double avgLongIntMapByClass(TLongIntMap map, TLongArrayList usersOfTheClass) {
		double result = 0.0;
		int nbValues = 0;

		for(TLongIterator it=usersOfTheClass.iterator(); it.hasNext();) {
			long user = it.next();
			if(map.containsKey(user)) {
				result += map.get(user);
				nbValues++;
			} else { // Should not happen when avgLongDoubleMapByClass is called from logAvgResultsByNbPerfectMatchesClass
				logger.warn("user {} from usersOfTheClass has no mapping in map", user);
			}
		}

		if(nbValues > 0) {
			result = result / (double) nbValues;
		}

		return result;
	}

	/**
	 * Utility function adding to a set all the integers (user IDs) returned by iter.
	 */
	private static void gatherAllUserIDs(TLongSet set, LongPrimitiveIterator iter) {
		while(iter.hasNext()) {
			set.add(iter.next());
		}
	}

}
