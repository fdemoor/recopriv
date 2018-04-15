package recoPrivResearch.dataModelBuilder;

import java.util.Random;
import java.util.List;
import java.util.Iterator;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.TLongFloatMap;
import gnu.trove.set.TLongSet;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TLongFloatHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.hash.TLongHashSet;
import gnu.trove.iterator.TLongIterator;
import gnu.trove.iterator.TLongFloatIterator;
import gnu.trove.iterator.TLongIntIterator;
import gnu.trove.iterator.TLongObjectIterator;

import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.eval.DataModelBuilder;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.common.TasteException;

import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.impl.model.GenericPreference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import recoPrivResearch.tools.Parameters;
import recoPrivResearch.tools.ExceptHandler;
import recoPrivResearch.attackEvaluator.NearestNeighborSybilAttackEvaluator;

/**
 * Helper class creating a DataModel, used by a RecommenderEvaluator or similar behaving clases.
 * The main entry point is the buildDataModel method which returns a DataModel made of the rawData
 * provided as argument and nbSybils Sybil users per target user. All Sybil users of a given target
 * user have the same profile made up of nbAuxiliaryItems random items taken from the target user's
 * profile. The associated ratings depend on the sybilRatingsStrategy parameter.
 */
public class SybilModelBuilder implements DataModelBuilder {

	private static final Logger logger = LogManager.getLogger(SybilModelBuilder.class);
	private final Random rand;
	private final int nbSybils;
	private final int nbTargets;
	private final long designatedTarget;
	private final boolean relativeNbAuxItems;
	private final int nbAuxiliaryItems;
	private final double percentAuxiliaryItems;
	private final String auxiliaryItemsSelectionStrategy;
	private final String sybilRatingsStrategy;
	private TLongObjectMap<TLongArrayList> auxiliaryItems; // Mapping attacked user ID -> list of auxiliary item IDs
	private TLongObjectMap<TLongArrayList> sybils; // Mapping attacked user ID -> list of Sybil IDs
	private TLongObjectMap<TLongArrayList> learntItems; // Mapping attacked user ID -> list of item IDs learnt by attack, populated by updateSybilProfiles()
	private TLongIntMap nbNonAuxiliaryItems; // Mapping attacked user ID -> nb items left in her profile for Sybils to guess
	private final int twostepAttack;  // level of the attack against twostep metric
	private final int twostepIdealNbItems; // twostep ideal number of extra items
	private int nbExtraItemPerSybil; // twostepAttack number of extra items per sybil
	private static int sybilNbAuxiliaryItems = 0;
	private final int bestWorstFrac;
	private final double percentIdealNbItems;
	private final boolean isGlobalIdealNbItems;
	private final double percentAdditionalItems; // percentage of (|t| - |aux|) as nbAdditionalItems for attack lvl 5

	public SybilModelBuilder(Parameters p, Random r) {
		nbSybils = p.nbSybils_;
		nbTargets = p.nbTargets_;
		designatedTarget = p.targetID_;
		relativeNbAuxItems = p.relativeNbAuxItems_;
		nbAuxiliaryItems = p.nbAuxiliaryItems_;
		percentAuxiliaryItems = p.percentAuxiliaryItems_;
		auxiliaryItemsSelectionStrategy = p.auxiliaryItemsSelectionStrategy_;
		sybilRatingsStrategy = p.sybilRatingsStrategy_;
		rand = r;
		learntItems = new TLongObjectHashMap<>();
		nbNonAuxiliaryItems = new TLongIntHashMap();
		twostepAttack = p.twostepAttack_;
		twostepIdealNbItems = p.twostepIdealNbItems_;
		nbExtraItemPerSybil = p.nbExtraItemPerSybil_;
		bestWorstFrac = p.bestWorstFrac_;
		percentIdealNbItems = p.percentIdealNbItems_;
		isGlobalIdealNbItems = p.isGlobalIdealNbItems_;
		percentAdditionalItems = p.percentAdditionalItems_;


		logger.debug("Sybil rating strategy is {}", sybilRatingsStrategy);
	}

	public DataModel buildDataModel(FastByIDMap<PreferenceArray> rawData) {

		logger.info("Injecting Sybil users in the DataModel...");

		DataModel originalModel = new GenericDataModel(rawData);
		auxiliaryItems = chooseTargets(originalModel);
		sybils = generateSybilIDs(originalModel, auxiliaryItems.keySet());
		FastByIDMap<PreferenceArray> attackedModel = ExceptHandler.dataModelToFastByIDMap(originalModel);
		insertSybils(attackedModel, auxiliaryItems, sybils);
		return new GenericDataModel(attackedModel);
	}

	/**
	 * Randomly choose nbTargets users and nbAuxiliaryItems items in their respective profiles
	 * as auxiliary information for the attacker, from model.
	 * If a chosen target user does not have enough items in her profile, chose another user instead.
	 * If nbTargets is greater than model.getNumUsers(), return model.getNumUsers() mappings.
	 * Returns mappings between a target ID and auxiliary item IDs.
	 * Limitation: cannot return more than Integer.MAX_VALUE targets.
	 */
	private TLongObjectMap<TLongArrayList> chooseTargets(DataModel model) {
		TLongObjectMap<TLongArrayList> result = new TLongObjectHashMap<>(nbTargets);

		int nbUsers = ExceptHandler.getModelNumUsers(model);

		if(nbTargets > nbUsers) {
			logger.warn("Trying to target {} users but the provided DataModel only has {} users", nbTargets, nbUsers);
		}

		TLongSet selectedUsers = new TLongHashSet();
		while(result.size() < nbTargets && selectedUsers.size() < nbUsers) {
			long userID;
			if(nbTargets > 1) { // Selecting a different user from those previously selected
				do {
					int index = rand.nextInt(nbUsers);
					userID = getUserIDAt(model, ++index, nbUsers); // Increment all index values because nextInt() returns values in [0:nbUsers[ while we expect user IDs in [1:nbUsers]
				} while(selectedUsers.contains(userID));
			} else {
				userID = getUserIDAt(model, (int) designatedTarget, nbUsers);
			}
			selectedUsers.add(userID);

			// Selecting nbAuxiliaryItems different items from userID's preferences
			PreferenceArray targetsPref = ExceptHandler.getPreferences(model, userID);

			int nbItems = targetsPref.length();
			logger.debug("Target user {} has {} ratings", userID, nbItems);

			TLongArrayList aux = null;
			if(relativeNbAuxItems) {
				aux = getAuxItemsRelative(model, userID, targetsPref, nbItems);
			} else {
				aux = getAuxItemsAbsolute(model, userID, targetsPref, nbItems, nbAuxiliaryItems);
			}

			if(aux != null) {
				result.put(userID, aux);

				nbNonAuxiliaryItems.put(userID, nbItems - aux.size());
				logger.trace("Target user {} has {}/{} non-auxiliary items", userID, nbItems - aux.size(), nbItems);
			} else {
				logger.debug("user {} has no auxiliary items", userID);
			}
		}

		if(result.size() < nbTargets && selectedUsers.size() == nbUsers) {
			logger.warn("Provided DataModel has less than {} users with {} ratings", nbTargets, nbAuxiliaryItems);
		}

		return result;
	}

	/**
	 * Return the user ID at position index in the list of all user IDs contained in model.
	 * index should be in [1:maxNbUsers].
	 * If model has less than index users, returns the last user ID.
	 * If index is 0 or negative, returns the first user ID.
	 */
	private long getUserIDAt(DataModel model, int index, int maxNbUsers) {
		LongPrimitiveIterator userIDs = ExceptHandler.getModelUserIDs(model);

		// Checking index is within expected range so that userIDs.peek() does not throw NoSuchElementException.
		if(index > maxNbUsers) {
			logger.warn("Asking for target user at {}, but the dataset has less users, returning the last one at {}", index, maxNbUsers);
			index = maxNbUsers;
		} else if(index < 1) {
			logger.warn("Asking for target user at index < 1 ({}), returning user at 1", index);
			index = 1;
		}
		userIDs.skip(index-1);

		return userIDs.peek();
	}

	private void dropWorstRatedItems(PreferenceArray originalItemRatings, TLongArrayList aux) {
		for (Preference pref : originalItemRatings) {
			if (pref.getValue() < 3.0) {
				aux.remove(pref.getItemID());
			}
		}
	}

	private TLongArrayList getAuxItemsAbsolute(DataModel model, long userID, PreferenceArray targetsPref, int nbItems, int nbAuxItems) {
		TLongArrayList aux = null;

		if(nbItems >= nbAuxItems) {
			aux = new TLongArrayList(nbAuxItems);
			long itemID = 0;
			if (auxiliaryItemsSelectionStrategy.contains("bestRated")) {
				PreferenceArray targetsPrefSorted = targetsPref;
				targetsPrefSorted.sortByValueReversed();
				for(int j=0; j<nbAuxItems; j++) {
					itemID = targetsPrefSorted.get(j).getItemID();
					aux.add(itemID);
					logger.trace("Target user {}: item {} has rating {}", userID, itemID, ExceptHandler.getItemValue(model, userID, itemID));
				}

			} else 	if (auxiliaryItemsSelectionStrategy.contains("worstRated")) {
					PreferenceArray targetsPrefSorted = targetsPref;
					targetsPrefSorted.sortByValue();
					for(int j=0; j<nbAuxItems; j++) {
						itemID = targetsPrefSorted.get(j).getItemID();
						aux.add(itemID);
						logger.trace("Target user {}: item {} has rating {}", userID, itemID, ExceptHandler.getItemValue(model, userID, itemID));
					}

			} else if (auxiliaryItemsSelectionStrategy.contains("bestWorstRated")) {
				int fracNumBestWorst = 1;
				int fracDenomBestWorst = bestWorstFrac;
				PreferenceArray targetsPrefSorted = targetsPref;
				targetsPrefSorted.sortByValueReversed();
				int halfNbAuxItems = nbAuxItems * fracNumBestWorst / fracDenomBestWorst;
				for(int j=0; j<halfNbAuxItems; j++) { // Best rated items
					itemID = targetsPrefSorted.get(j).getItemID();
					aux.add(itemID);
					logger.trace("Target user {}: item {} has rating {}", userID, itemID, ExceptHandler.getItemValue(model, userID, itemID));
				}
				targetsPrefSorted.sortByValue();
				for(int j=0; j<(nbAuxItems-halfNbAuxItems); j++) { // Worst rated items
					itemID = targetsPrefSorted.get(j).getItemID();
					aux.add(itemID);
					logger.trace("Target user {}: item {} has rating {}", userID, itemID, ExceptHandler.getItemValue(model, userID, itemID));
				}

			} else if (auxiliaryItemsSelectionStrategy.contains("regularSampling")) {
				PreferenceArray targetsPrefSorted = targetsPref;
				targetsPrefSorted.sortByValueReversed();
				double h = nbItems / nbAuxItems;
				for(int j=0; j<nbAuxItems; j++) {
					int itemIndex = (int) (j * h);
					itemID = targetsPrefSorted.get(itemIndex).getItemID();
					aux.add(itemID);
					logger.trace("Target user {}: item {} has rating {}", userID, itemID, ExceptHandler.getItemValue(model, userID, itemID));
				}

			} else if (auxiliaryItemsSelectionStrategy.contains("mostPopular")) {
				TLongFloatMap globalRatings = getPopularItems(model, targetsPref, nbAuxItems, 1);
				for ( TLongFloatIterator it = globalRatings.iterator(); it.hasNext(); ) {
					it.advance();
					aux.add(it.key());
				}

			} else if (auxiliaryItemsSelectionStrategy.contains("leastPopular")) {
				TLongFloatMap globalRatings = getPopularItems(model, targetsPref, nbAuxItems, 0);
				for ( TLongFloatIterator it = globalRatings.iterator(); it.hasNext(); ) {
					it.advance();
					aux.add(it.key());
				}

			} else if (auxiliaryItemsSelectionStrategy.contains("leastRated")) {
				TLongIntMap globalRatings = getLeastRatedItems(model, targetsPref, nbAuxItems);
				for ( TLongIntIterator it = globalRatings.iterator(); it.hasNext(); ) {
					it.advance();
					aux.add(it.key());
				}

			} else { // Default is random choice
				for(int j=0; j<nbAuxItems; j++) {
					do {
						int itemIndex = rand.nextInt(nbItems);
						itemID = targetsPref.get(itemIndex).getItemID();
					} while(aux.contains(itemID));
					aux.add(itemID);
					logger.trace("Target user {}: item {} has rating {}", userID, itemID, ExceptHandler.getItemValue(model, userID, itemID));
				}
			}

			logger.debug("Target user {}: items {} are auxiliary info for the adversary", userID, aux);

		} else {
			logger.debug("Potential target user {} discarded: only {} items in her profile", userID, nbItems);
		}

		return aux;
	}

	/**
	 * Return nbAuxItems most / least popular items in the global system among the ones in aux info
	 */
	private TLongFloatMap getPopularItems(DataModel model, PreferenceArray targetsPref, int nbAuxItems, int sortingType) {
		TLongFloatMap globalRatings = new TLongFloatHashMap();
		long[] itemsIDs = targetsPref.getIDs();
		long currentRefID = 0;
		float currentRef = 0; // Least popular
		if (sortingType >= 1) {
			currentRef = 5; // Most popular
		}
		for (int j = 0; j < targetsPref.length(); j++) {
		PreferenceArray ratings = null;
			try {
				ratings = model.getPreferencesForItem(itemsIDs[j]);
			} catch (TasteException ex) {
				logger.warn("No item correspond to ID {}", itemsIDs[j]);
			}
			float avg = 0;
			for (int m = 0; m < ratings.length(); m++) {
				avg += ratings.getValue(m);
			}
				// If not yet nbAuxItems in map, add key, and update ref value
			avg = avg / ratings.length();
			if (globalRatings.size() < nbAuxItems) {
				globalRatings.put(itemsIDs[j], avg);
				if ((sortingType == 0 && avg > currentRef) || (sortingType >= 1 && avg < currentRef)) {
					currentRef = avg;
				}
				// If bad comparison with ref value, no need to add
				// else add, remove last ref, and update ref value
			} else if ((sortingType == 0 && avg < currentRef) || (sortingType >= 1 && avg > currentRef)) {
				for ( TLongFloatIterator it = globalRatings.iterator(); it.hasNext(); ) {
					it.advance();
					if ((sortingType == 0 && it.value() > currentRef) || (sortingType >= 1 && it.value() < currentRef)) {
						currentRef = it.value();
						currentRefID = it.key();
					}
				}
				globalRatings.adjustValue(currentRefID, avg);
				for ( TLongFloatIterator it = globalRatings.iterator(); it.hasNext(); ) {
					it.advance();
					if ((sortingType == 0 && it.value() > currentRef) || (sortingType >= 1 && it.value() < currentRef)) {
						currentRef = it.value();
					}
				}
			}
		}
		System.out.format("size = %d%n", globalRatings.size());
		return globalRatings;
	}

	/**
	 * Return nbAuxItems least rated items in the global system among the ones in aux info
	 */
	private TLongIntMap getLeastRatedItems(DataModel model, PreferenceArray targetsPref, int nbAuxItems) {
		TLongIntMap globalRatings = new TLongIntHashMap();
		long[] itemsIDs = targetsPref.getIDs();
		long currentRefID = 0;
		int currentRef = 0; // Least rated
		for (int j = 0; j < targetsPref.length(); j++) {
			int nbTimesRated = 0;
			try {
				nbTimesRated = model.getNumUsersWithPreferenceFor(itemsIDs[j]);
			} catch (TasteException ex) {
				logger.warn("No item correspond to ID {}", itemsIDs[j]);
			}
				// If not yet nbAuxItems in map, add key, and update ref value
			if (globalRatings.size() < nbAuxItems) {
				globalRatings.put(itemsIDs[j], nbTimesRated);
				if (nbTimesRated > currentRef) {
					currentRef = nbTimesRated;
				}
				// If bad comparison with ref value, no need to add
				// else add, remove last ref, and update ref value
			} else if (nbTimesRated < currentRef) {
				for ( TLongIntIterator it = globalRatings.iterator(); it.hasNext(); ) {
					it.advance();
					if (it.value() > currentRef) {
						currentRef = it.value();
						currentRefID = it.key();
					}
				}
				globalRatings.adjustValue(currentRefID, nbTimesRated);
				for ( TLongIntIterator it = globalRatings.iterator(); it.hasNext(); ) {
					it.advance();
					if (it.value() > currentRef) {
						currentRef = it.value();
					}
				}
			}
		}
		return globalRatings;
	}

	private TLongArrayList getAuxItemsRelative(DataModel model, long userID, PreferenceArray targetsPref, int nbItems) {
		int localNbAuxItems = Math.round((float) (nbItems * percentAuxiliaryItems));

		logger.debug("user {}: {} x {} = {}", userID, nbItems, percentAuxiliaryItems, localNbAuxItems);

		// Ensures there is at least 1 item left out of the auxiliary items, as long as we don't expect to have the whole profile as auxiliary information
		// that is when percentAuxiliaryItems is 100%.
		if(localNbAuxItems == nbItems && percentAuxiliaryItems < 1.0) {
			localNbAuxItems = nbItems - 1;
		}

		return getAuxItemsAbsolute(model, userID, targetsPref, nbItems, localNbAuxItems);
	}



	/**
	 * For each target user, returns a list of IDs to be used for Sybil users.
	 * Sybil IDs are guaranteed not to conflict with any real user ID in model.
	 * Sybil IDs are chosen according to getNewID()'s behavior.
	 */
	private TLongObjectMap<TLongArrayList> generateSybilIDs(DataModel model, TLongSet targets) {
		TLongObjectMap<TLongArrayList> result = new TLongObjectHashMap<>(nbTargets);

		TLongSet userIDsInUse = getUserIDSet(model);

		for(TLongIterator it=targets.iterator(); it.hasNext();) {
			long userID = it.next();
			TLongArrayList sybilIDs = new TLongArrayList(nbSybils);
			for(int i=0; i<nbSybils; i++) {
				long sybilID = getNewID(userIDsInUse);
				sybilIDs.add(sybilID);
				userIDsInUse.add(sybilID);
			}

			logger.debug("Target user {}: Sybil user IDs are {}", userID, sybilIDs);

			result.put(userID, sybilIDs);
		}

		return result;
	}

	/**
	 * Returns the set of all user IDs in model.
	 */
	private TLongSet getUserIDSet(DataModel model) {
		int nbUsers = -1;
		try {
			nbUsers = model.getNumUsers();
		} catch(TasteException e) {
			e.printStackTrace();
		}

		TLongSet result = new TLongHashSet(nbUsers);
		LongPrimitiveIterator it = null;
		try {
			it = model.getUserIDs();
		} catch(TasteException e) {
			e.printStackTrace();
		}
		while(it.hasNext()) {
			result.add(it.nextLong());
		}
		return result;
	}

	/** Return an ID which is not contained in takenIDs.
	 * Try the number of elements in takenIDs + 1, then increment until a free ID is found.
	 */
	private long getNewID(TLongSet takenIDs) {
		long result = takenIDs.size() + 1;
		while(takenIDs.contains(result)) {
			result++;
		}
		return result;
	}

	/**
	 * Insert Sybil profiles in rawModel according to auxiliaryItems, sybils, and sybilRatingsStrategy.
	 */
	private void insertSybils(FastByIDMap<PreferenceArray> rawModel, TLongObjectMap<TLongArrayList> auxiliaryItems, TLongObjectMap<TLongArrayList> sybils) {
		for(TLongObjectIterator<TLongArrayList> it=sybils.iterator(); it.hasNext();) {
			it.advance();
			long attackedUserID = it.key();
			TLongArrayList usersAuxItems = auxiliaryItems.get(attackedUserID);
			//dropWorstRatedItems(rawModel.get(attackedUserID), usersAuxItems);
			sybilNbAuxiliaryItems = usersAuxItems.size();
			TLongList usersAuxItemsList = usersAuxItems.subList(0, usersAuxItems.size());

			DataModel model = new GenericDataModel(rawModel);
			int totalNbItems = 0;
			try {
				totalNbItems = model.getNumItems();
			} catch (TasteException ex) {;}
			int nbAvailableItemsPerSybil = (int) ((totalNbItems - usersAuxItems.size()) / nbSybils * percentIdealNbItems);

			if (!isGlobalIdealNbItems) {
				nbExtraItemPerSybil = (int) (sybilNbAuxiliaryItems * percentIdealNbItems);
			}

			TLongArrayList usersAdditionalItems = null;
			if (twostepAttack == 1) {
				usersAdditionalItems = getAdditionalItems(rawModel, nbExtraItemPerSybil, usersAuxItems);
			} else if (twostepAttack == 2) {
				usersAdditionalItems = getAdditionalItems(rawModel, nbSybils * nbExtraItemPerSybil, usersAuxItems);
			} else if (twostepAttack == 3) {
				usersAdditionalItems = getAdditionalItems(rawModel, nbSybils * nbAvailableItemsPerSybil, usersAuxItems);
			} else if (twostepAttack == 4) {
				nbExtraItemPerSybil = ExceptHandler.getPreferences(model, attackedUserID).length() - sybilNbAuxiliaryItems;
				usersAdditionalItems = getAdditionalItems(rawModel, nbSybils * nbExtraItemPerSybil, usersAuxItems);
			} else if (twostepAttack == 5) {
				nbExtraItemPerSybil = ExceptHandler.getPreferences(model, attackedUserID).length() - sybilNbAuxiliaryItems;
				nbExtraItemPerSybil = (int) (percentAdditionalItems * nbExtraItemPerSybil);
				usersAdditionalItems = getCreatedAdditionalItems(rawModel, nbSybils * nbExtraItemPerSybil);
			}


			usersAuxItems.shuffle(rand);
			if (twostepAttack == 2 && relativeNbAuxItems) {
				int nbCommonAuxItems = usersAuxItems.size() - nbExtraItemPerSybil;
				//usersAuxItems.shuffle(rand);
				if (nbCommonAuxItems < 0) {
					logger.warn("Trying to remove more items than possible: diff is {}", nbCommonAuxItems);
					usersAuxItemsList = usersAuxItems.subList(0, 0);
				} else {
					usersAuxItemsList = usersAuxItems.subList(0, nbCommonAuxItems);
				}
			}

			TLongArrayList usersSybils = it.value();

			int sybilNb = 0;

			for(TLongIterator iter=usersSybils.iterator(); iter.hasNext();) {
				long sybilID = iter.next();
				TLongFloatMap sybilRatings = createRatings(rawModel.get(attackedUserID), usersAuxItemsList, sybilRatingsStrategy);

				if (twostepAttack == 1) {
					TLongFloatMap sybilAdditionalRatings = createRatings(usersAdditionalItems, 0, nbExtraItemPerSybil);
					sybilRatings.putAll(sybilAdditionalRatings);
				} else if (twostepAttack == 2) {
					TLongFloatMap sybilAdditionalRatings = createRatings(usersAdditionalItems, sybilNb * nbExtraItemPerSybil, nbExtraItemPerSybil);
					sybilRatings.putAll(sybilAdditionalRatings);
				} else if (twostepAttack == 3) {
					TLongFloatMap sybilAdditionalRatings = createRatings(usersAdditionalItems, sybilNb * nbAvailableItemsPerSybil, nbAvailableItemsPerSybil);
					sybilRatings.putAll(sybilAdditionalRatings);
				} else if (twostepAttack == 4 || twostepAttack == 5) {
					TLongFloatMap sybilAdditionalRatings = createRatings(usersAdditionalItems, sybilNb * nbExtraItemPerSybil, nbExtraItemPerSybil);
					sybilRatings.putAll(sybilAdditionalRatings);
				}

				sybilNb++;
				PreferenceArray sybilProfile = buildSybilProfile(sybilID, sybilRatings);

				logger.trace("Sybil {} has the following profile: {}", sybilID, sybilProfile);

				rawModel.put(sybilID, sybilProfile);
			}
		}
	}

	private PreferenceArray buildSybilProfile(long userID, TLongFloatMap ratings) {
		PreferenceArray result = new GenericUserPreferenceArray(ratings.size());
		//for(int i=0; i<itemIDs.size(); i++) {
		int i = 0;
		for(TLongIterator it=ratings.keySet().iterator(); it.hasNext();) {
			long item = it.next();
			Preference pref = new GenericPreference(userID, item, ratings.get(item));
			result.set(i, pref);
			i++;
		}
		return result;
	}


	private TLongArrayList getAdditionalItems(FastByIDMap<PreferenceArray> rawModel, int nbItems, TLongArrayList auxItems) {
		TLongArrayList aux = null;

		DataModel model = new GenericDataModel(rawModel);
		int totalNbItems = 0;
		try {
			totalNbItems = model.getNumItems();
		} catch (TasteException ex) {;}
		int nbAvailableItems = totalNbItems - auxItems.size();

		if (nbAvailableItems >= nbItems) {
			try {
				TLongArrayList availableItemsIDs = new TLongArrayList(totalNbItems);
				aux = new TLongArrayList(nbItems);
				for (LongPrimitiveIterator it = model.getUserIDs(); it.hasNext();) {
					long userID = it.nextLong();
					PreferenceArray userPrefs =  model.getPreferencesFromUser(userID);
					for (int i = 0; i != userPrefs.length(); i++) {
						long itemID = userPrefs.getItemID(i);
						if (!auxItems.contains(itemID) && !availableItemsIDs.contains(itemID)) {
							availableItemsIDs.add(itemID);
						}
					}
				}
				availableItemsIDs.shuffle(rand);
				long itemID = 0;
				TLongIterator it = availableItemsIDs.iterator();
				int j = 0;
				while (j < nbItems) {
					itemID = it.next();
					aux.add(itemID);
					j++;
				}
			} catch (TasteException ex) {;}

		} else {
			logger.warn("Can't build sybil additional profiles: only {} available items, {} are asked", nbAvailableItems, nbItems);
		}

		return aux;
	}


	private TLongArrayList getCreatedAdditionalItems(FastByIDMap<PreferenceArray> rawModel, int nbItems) {
		TLongArrayList aux = null;

		DataModel model = new GenericDataModel(rawModel);
		long maxItemID = 0;
		try {
			for (LongPrimitiveIterator it = model.getItemIDs(); it.hasNext();) {
				long itemID = it.nextLong();
				if (itemID > maxItemID) {
					maxItemID = itemID;
				}
			}
			aux = new TLongArrayList(nbItems);
			for (long itemID = maxItemID + 1; itemID < maxItemID + nbItems + 1; itemID++) {
				aux.add(itemID);
			}
		} catch (TasteException ex) {;}

		return aux;
	}


	private TLongFloatMap createRatings(PreferenceArray originalItemRatings, TLongList itemIDs, String ratingStrategy) {
		TLongFloatMap result = new TLongFloatHashMap(itemIDs.size());
		for(Preference pref : originalItemRatings) {
			if(itemIDs.contains(pref.getItemID())) {
				result.put(pref.getItemID(), getRatingValue(ratingStrategy, pref.getValue()));
			}
		}
		return result;
	}

	private TLongFloatMap createRatings(TLongArrayList itemIDs, int indexStart, int length) {
		if (itemIDs != null) {
			TLongFloatMap result = new TLongFloatHashMap(itemIDs.size());
			long[] subItemsIDs = itemIDs.toArray(new long[length], indexStart, length);
			for (long itemID : subItemsIDs) {
				result.put(itemID, (float)1.0);
			}
			return result;
		} else {
			return new TLongFloatHashMap();
		}
	}

	private static float getRatingValue(String strategy, float originalRating) {
		float rating;
		switch(strategy) {
			case "groundtruth":
				rating = originalRating;
				break;
			case "neutral":
				rating = (float) 3.0;
				break;
			case "max":
				rating = (float) 5.0;
				break;
			case "liked":
				rating = (float) 4.0;
				break;
			default:
				rating = (float) 2.5;
				break;
		}
		return rating;
	}

	/**
	 * buildDataModel must have been called before using this method.
	 */
	public TLongSet getAttackedUsers() {
		return auxiliaryItems.keySet();
	}

	/**
	 * buildDataModel must have been called before using this method.
	 */
	public TLongObjectMap<TLongArrayList> getAuxiliaryItems() {
		return auxiliaryItems;
	}

	/**
	 * buildDataModel must have been called before using this method.
	 */
	public TLongObjectMap<TLongArrayList> getSybils() {
		return sybils;
	}

	/**
	 * If updateSybilProfiles has no been called at least once before using this method, it will return an empty TLongObjectMap.
	 */
	public TLongObjectMap<TLongArrayList> getLearntItems() {
		return learntItems;
	}

	/**
	 * insertSybils must have been called before using this method
	 */
	public static int getNbAuxiliaryItems() {
		return sybilNbAuxiliaryItems;
	}

	/**
	 * Generate a new DataModel based on model with Sybil users' profile updated with recommendations.
	 * Add item IDs from recommendations to learntItems.
	 */
	public DataModel updateSybilProfiles(DataModel model, TLongObjectMap<List<RecommendedItem>> recommendations) {
		DataModel result = new GenericDataModel(ExceptHandler.dataModelToFastByIDMap(model));
		for(TLongIterator it=recommendations.keySet().iterator(); it.hasNext();) { // for each target user
			long targetUser = it.next();
			PreferenceArray targetsProfile = ExceptHandler.getPreferences(model, targetUser);
			targetsProfile.sortByItem(); // Done once here because it is used by findByItemID() (in addItems())
			List<RecommendedItem> itemsLearnedFromTarget = recommendations.get(targetUser);

			addToLearntItems(targetUser, itemsLearnedFromTarget);

			TLongArrayList targetsSybils = sybils.get(targetUser);
			for(TLongIterator iter=targetsSybils.iterator(); iter.hasNext();) { // for each Sybil user
				long sybil = iter.next();
				addItems(result, sybil, itemsLearnedFromTarget, sybilRatingsStrategy, targetsProfile);
			}
		}
		return result;
	}

	/**
	 * Add the item IDs found in recos in the list of targetID in learntItems.
	 */
	private void addToLearntItems(long targetID, List<RecommendedItem> recos) {
		for(RecommendedItem reco : recos) {
			TLongArrayList itemsSupposedlyLearntFromTarget = learntItems.get(targetID);
			if(itemsSupposedlyLearntFromTarget == null) {
				itemsSupposedlyLearntFromTarget = new TLongArrayList(recos.size());
				learntItems.put(targetID, itemsSupposedlyLearntFromTarget);
			}
			itemsSupposedlyLearntFromTarget.add(reco.getItemID());
		}
	}

	/**
	 * Add all the items from recos to userID's profile in model with ratings depending on strategy.
	 */
	private static void addItems(DataModel model, long userID, List<RecommendedItem> recos, String strat, PreferenceArray targetsProfile) {
		for(RecommendedItem reco : recos) {
			float rating = (float) -1.0;
			if(targetsProfile.hasPrefWithItemID(reco.getItemID())) {
				rating = findByItemID(reco.getItemID(), targetsProfile);
			} else {
				logger.debug("Target user has no opinion about recommended item {}", reco.getItemID());
			}
			ExceptHandler.setRating(model, userID, reco.getItemID(), getRatingValue(strat, rating));
		}
	}

	/**
	 * Find the rating corresponding to itemID in profile.
	 * This method assumes that profile is sorted by ascending item IDs (with PreferenceArray.sortByItem()).
	 * Returns -1.0 if itemID is not contained in profile.
	 */
	public static float findByItemID(long itemID, PreferenceArray profile) {
		float result = (float) -1.0;
		long[] itemIDs = profile.getIDs();
		boolean found = false;
		int i = 0;
		while(!found && i<itemIDs.length) {
			if(itemIDs[i] == itemID) {
				found = true;
				result = profile.getValue(i);
			}
			i++;
		}
		return result;
	}

	public TLongIntMap getNbNonAuxiliaryItems() {
		return nbNonAuxiliaryItems;
	}
}
