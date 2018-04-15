package recoPrivResearch.attackEvaluator;

import java.util.List;
import java.util.ArrayList;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.TLongDoubleMap;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TLongDoubleHashMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TLongIterator;
import gnu.trove.iterator.TDoubleIterator;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Gather statistics about an attack, possibly over several rounds.
 * Round numbers start from 1.
 */
public class AttackStats {

	private static final Logger logger = LogManager.getLogger(AttackStats.class);

	private TIntObjectMap<TLongDoubleMap> sybilInfiltrationPerRound;
	private TIntObjectMap<TLongIntMap> yieldPerRound;
	private TIntObjectMap<TLongDoubleMap> accuracyPerRound;
	private TIntObjectMap<TLongDoubleMap> stricterAccuracyPerRound;
	private TIntObjectMap<TLongIntMap> absAccuracyPerRound;
	private TIntObjectMap<TLongIntMap> stricterAbsAccuracyPerRound;
	private TIntObjectMap<TLongDoubleMap> expectedNeighborhoodsFracPerRound;
	private TIntObjectMap<TLongDoubleMap> targetsAreNeighborsPerRound;
	private TIntObjectMap<TLongDoubleMap> targetsNbSybilNeighborsPerRound;
	private TIntObjectMap<TLongIntMap> nbPerfectMatchesPerRound;
	private TIntObjectMap<TLongDoubleMap> fracOfUnwantedNeighborsPerRound;
	private TIntObjectMap<TLongDoubleMap> fracOfUnwantedNeighborsWTargetOnlyPerRound;

	private TDoubleList avgSybilInfiltrationPerRound;
	private TDoubleList avgYieldPerRound;
	private TDoubleList avgAccuracyPerRound;
	private TDoubleList avgStricterAccuracyPerRound;
	private TDoubleList avgAbsAccuracyPerRound;
	private TDoubleList avgStricterAbsAccuracyPerRound;
	private TDoubleList avgExpectedNeighborhoodsFracPerRound;
	private TDoubleList avgTargetsAreNeighborsPerRound;
	private TDoubleList avgTargetsNbSybilNeighborsPerRound;
	private TDoubleList avgNbPerfectMatchesPerRound;
	private TDoubleList avgFracOfUnwantedNeighborsPerRound;
	private TDoubleList avgFracOfUnwantedNeighborsWTargetOnlyPerRound;

	private TLongDoubleMap avgSybilInfiltrationPerTarget;
	private TLongDoubleMap avgYieldPerTarget;
	private TLongDoubleMap avgAccuracyPerTarget;
	private TLongDoubleMap avgStricterAccuracyPerTarget;
	private TLongDoubleMap avgAbsAccuracyPerTarget;
	private TLongDoubleMap avgStricterAbsAccuracyPerTarget;
	private TLongDoubleMap avgExpectedNeighborhoodsFracPerTarget;
	private TLongDoubleMap avgTargetsAreNeighborsPerTarget;
	private TLongDoubleMap avgTargetsNbSybilNeighborsPerTarget;
	private TLongDoubleMap avgNbPerfectMatchesPerTarget;
	private TLongDoubleMap avgFracOfUnwantedNeighborsPerTarget;
	private TLongDoubleMap avgFracOfUnwantedNeighborsWTargetOnlyPerTarget;

	private double avgSybilInfiltration;
	private double avgYield;
	private double avgAccuracy;
	private double avgStricterAccuracy;
	private double avgAbsAccuracy;
	private double avgStricterAbsAccuracy;
	private double avgExpectedNeighborhoodsFrac;
	private double avgTargetsAreNeighbors;
	private double avgTargetsNbSybilNeighbors;
	private double avgNbPerfectMatches;
	private double avgFracOfUnwantedNeighbors;
	private double avgFracOfUnwantedNeighborsWTargetOnly;
	private int nbAuxiliaryItems;
	private double fracSybilsHaveAllSybilNeighbors;
  private double allSybilsOnlySybilNeighbors;
  private double accuracyPrime;

	public AttackStats() {
		this(-1);
	}

	public AttackStats(int nbRounds) {
		if(nbRounds != -1) {
			sybilInfiltrationPerRound = new TIntObjectHashMap<>(nbRounds);
			yieldPerRound = new TIntObjectHashMap<>(nbRounds);
			accuracyPerRound = new TIntObjectHashMap<>(nbRounds);
			stricterAccuracyPerRound = new TIntObjectHashMap<>(nbRounds);
			absAccuracyPerRound = new TIntObjectHashMap<>(nbRounds);
			stricterAbsAccuracyPerRound = new TIntObjectHashMap<>(nbRounds);
			expectedNeighborhoodsFracPerRound = new TIntObjectHashMap<>(nbRounds);
			targetsAreNeighborsPerRound = new TIntObjectHashMap<>(nbRounds);
			targetsNbSybilNeighborsPerRound = new TIntObjectHashMap<>(nbRounds);
			nbPerfectMatchesPerRound = new TIntObjectHashMap<>(nbRounds);
			fracOfUnwantedNeighborsPerRound = new TIntObjectHashMap<>(nbRounds);
			fracOfUnwantedNeighborsWTargetOnlyPerRound = new TIntObjectHashMap<>(nbRounds);

			avgSybilInfiltrationPerRound = new TDoubleArrayList(nbRounds+1);
			avgYieldPerRound = new TDoubleArrayList(nbRounds+1);
			avgAccuracyPerRound = new TDoubleArrayList(nbRounds+1);
			avgStricterAccuracyPerRound = new TDoubleArrayList(nbRounds+1);
			avgAbsAccuracyPerRound = new TDoubleArrayList(nbRounds+1);
			avgStricterAbsAccuracyPerRound = new TDoubleArrayList(nbRounds+1);
			avgExpectedNeighborhoodsFracPerRound = new TDoubleArrayList(nbRounds+1);
			avgTargetsAreNeighborsPerRound = new TDoubleArrayList(nbRounds+1);
			avgTargetsNbSybilNeighborsPerRound = new TDoubleArrayList(nbRounds+1);
			avgNbPerfectMatchesPerRound = new TDoubleArrayList(nbRounds+1);
			avgFracOfUnwantedNeighborsPerRound = new TDoubleArrayList(nbRounds+1);
			avgFracOfUnwantedNeighborsWTargetOnlyPerRound = new TDoubleArrayList(nbRounds+1);
		} else {
			sybilInfiltrationPerRound = new TIntObjectHashMap<>();
			yieldPerRound = new TIntObjectHashMap<>();
			accuracyPerRound = new TIntObjectHashMap<>();
			stricterAccuracyPerRound = new TIntObjectHashMap<>();
			absAccuracyPerRound = new TIntObjectHashMap<>();
			stricterAbsAccuracyPerRound = new TIntObjectHashMap<>();
			expectedNeighborhoodsFracPerRound = new TIntObjectHashMap<>();
			targetsAreNeighborsPerRound = new TIntObjectHashMap<>();
			targetsNbSybilNeighborsPerRound = new TIntObjectHashMap<>();
			nbPerfectMatchesPerRound = new TIntObjectHashMap<>();
			fracOfUnwantedNeighborsPerRound = new TIntObjectHashMap<>();
			fracOfUnwantedNeighborsWTargetOnlyPerRound = new TIntObjectHashMap<>();

			avgSybilInfiltrationPerRound = new TDoubleArrayList();
			avgYieldPerRound = new TDoubleArrayList();
			avgAccuracyPerRound = new TDoubleArrayList();
			avgStricterAccuracyPerRound = new TDoubleArrayList();
			avgAbsAccuracyPerRound = new TDoubleArrayList();
			avgStricterAbsAccuracyPerRound = new TDoubleArrayList();
			avgExpectedNeighborhoodsFracPerRound = new TDoubleArrayList();
			avgTargetsAreNeighborsPerRound = new TDoubleArrayList();
			avgTargetsNbSybilNeighborsPerRound = new TDoubleArrayList();
			avgNbPerfectMatchesPerRound = new TDoubleArrayList();
			avgFracOfUnwantedNeighborsPerRound = new TDoubleArrayList();
			avgFracOfUnwantedNeighborsWTargetOnlyPerRound = new TDoubleArrayList();
		}

		// Padding the lists because round numbers start from 1
		avgSybilInfiltrationPerRound.add(-1.0);
		avgYieldPerRound.add(-1.0);
		avgAccuracyPerRound.add(-1.0);
		avgStricterAccuracyPerRound.add(-1.0);
		avgAbsAccuracyPerRound.add(-1.0);
		avgStricterAbsAccuracyPerRound.add(-1.0);
		avgExpectedNeighborhoodsFracPerRound.add(-1.0);
		avgTargetsAreNeighborsPerRound.add(-1.0);
		avgTargetsNbSybilNeighborsPerRound.add(-1.0);
		avgNbPerfectMatchesPerRound.add(-1.0);
		avgFracOfUnwantedNeighborsPerRound.add(-1.0);
		avgFracOfUnwantedNeighborsWTargetOnlyPerRound.add(-1.0);

		avgSybilInfiltrationPerTarget = new TLongDoubleHashMap();
		avgYieldPerTarget = new TLongDoubleHashMap();
		avgAccuracyPerTarget = new TLongDoubleHashMap();
		avgStricterAccuracyPerTarget = new TLongDoubleHashMap();
		avgAbsAccuracyPerTarget = new TLongDoubleHashMap();
		avgStricterAbsAccuracyPerTarget = new TLongDoubleHashMap();
		avgExpectedNeighborhoodsFracPerTarget = new TLongDoubleHashMap();
		avgTargetsAreNeighborsPerTarget = new TLongDoubleHashMap();
		avgTargetsNbSybilNeighborsPerTarget = new TLongDoubleHashMap();
		avgNbPerfectMatchesPerTarget = new TLongDoubleHashMap();
		avgFracOfUnwantedNeighborsPerTarget = new TLongDoubleHashMap();
		avgFracOfUnwantedNeighborsWTargetOnlyPerTarget = new TLongDoubleHashMap();
	}

	// Setters
	/**
	 * Record fractions as Sybil Infiltration values for one round.
	 * Also compute the average infiltration over all target users for this round.
	 */
	public void setSybilInfiltration(TLongDoubleMap fractions) {
		sybilInfiltrationPerRound.put(sybilInfiltrationPerRound.size()+1, fractions);
		avgSybilInfiltrationPerRound.add(averageLongDoubleMapValues(fractions));
	}

	/**
	 * Record yield as Yield values for one round.
	 * Also compute the average yield over all target users for this round.
	 */
	public void setYield(TLongIntMap yield) {
		yieldPerRound.put(yieldPerRound.size()+1, yield);
		avgYieldPerRound.add(averageLongIntMapValues(yield));
	}

	/**
	 * Set number of auxiliary items selected in the target profile
	 */
	public void setNbAuxiliaryItems(int n) {
		nbAuxiliaryItems = n;
	}

	/**
	 * Record accuracy as Accuracy values (percentage) for one round.
	 * Also compute the average accuracy over all target users for this round.
	 */
	public void setAccuracy(TLongDoubleMap accuracy) {
		accuracyPerRound.put(accuracyPerRound.size()+1, accuracy);
		avgAccuracyPerRound.add(averageLongDoubleMapValues(accuracy));
	}

	/**
	 * Record accuracy as Accuracy values (number of items) for one round.
	 * Also compute the average accuracy over all target users for this round.
	 */
	public void setAbsAccuracy(TLongIntMap accuracy) {
		absAccuracyPerRound.put(absAccuracyPerRound.size()+1, accuracy);
		avgAbsAccuracyPerRound.add(averageLongIntMapValues(accuracy));
	}

	/**
	 * Record accuracy as Accuracy values (percentage, stricter version) for one round.
	 * Also compute the average accuracy over all target users for this round.
	 */
	public void setStricterAccuracy(TLongDoubleMap accuracy) {
		stricterAccuracyPerRound.put(stricterAccuracyPerRound.size()+1, accuracy);
		avgStricterAccuracyPerRound.add(averageLongDoubleMapValues(accuracy));
	}

	/**
	 * Record accuracy as Accuracy values (number of items, stricter version) for one round.
	 * Also compute the average accuracy over all target users for this round.
	 */
	public void setStricterAbsAccuracy(TLongIntMap accuracy) {
		stricterAbsAccuracyPerRound.put(stricterAbsAccuracyPerRound.size()+1, accuracy);
		avgStricterAbsAccuracyPerRound.add(averageLongIntMapValues(accuracy));
	}

	public void setExpectedNeighborhoodsFrac(TLongDoubleMap fractions) {
		expectedNeighborhoodsFracPerRound.put(expectedNeighborhoodsFracPerRound.size()+1, fractions);
		avgExpectedNeighborhoodsFracPerRound.add(averageLongDoubleMapValues(fractions));
	}

	public void setTargetsAreNeighbors(TLongDoubleMap map) {
		targetsAreNeighborsPerRound.put(targetsAreNeighborsPerRound.size()+1, map);
		avgTargetsAreNeighborsPerRound.add(averageLongDoubleMapValues(map));
	}

	public void setTargetsNbSybilNeighbors(TLongDoubleMap map) {
		targetsNbSybilNeighborsPerRound.put(targetsNbSybilNeighborsPerRound.size()+1, map);
		avgTargetsNbSybilNeighborsPerRound.add(averageLongDoubleMapValues(map));
	}

	public void setNbPerfectMatches(TLongIntMap map) {
		nbPerfectMatchesPerRound.put(nbPerfectMatchesPerRound.size()+1, map);
		avgNbPerfectMatchesPerRound.add(averageLongIntMapValues(map));
	}

	public void setFracOfUnwantedNeighbors(TLongDoubleMap map) {
		fracOfUnwantedNeighborsPerRound.put(fracOfUnwantedNeighborsPerRound.size()+1, map);
		avgFracOfUnwantedNeighborsPerRound.add(averageLongDoubleMapValues(map));
	}

	public void setFracOfUnwantedNeighborsWTargetOnly(TLongDoubleMap map) {
		fracOfUnwantedNeighborsWTargetOnlyPerRound.put(fracOfUnwantedNeighborsWTargetOnlyPerRound.size()+1, map);
		avgFracOfUnwantedNeighborsWTargetOnlyPerRound.add(averageLongDoubleMapValues(map));
	}

	/**
	 * Frac of Sybil users that have all other Sybils in their neighborhood
	 */
	 public void setFracSybilsHaveAllSybilNeighbors(double x) {
		 fracSybilsHaveAllSybilNeighbors = x;
	 }

	 public void setAllSybilsOnlySybilNeighbors(double x) {
		 allSybilsOnlySybilNeighbors = x;
	 }
   
   public void setAccuracyPrime(double x) {
		 accuracyPrime = x;
	 }

	// Getters
	// Get 1 round's values
	/**
	 * Sybil Infiltration values for all target users at round roundNumber.
	 */
	public TLongDoubleMap getSybilInfiltrationsAt(int roundNumber) {
		return sybilInfiltrationPerRound.get(roundNumber);
	}

	/**
	 * Yield values for all target users at round roundNumber.
	 */
	public TLongIntMap getYieldsAt(int roundNumber) {
		return yieldPerRound.get(roundNumber);
	}

	/**
	 * Accuracy values (percentage) for all target users at round roundNumber.
	 */
	public TLongDoubleMap getAccuraciesAt(int roundNumber) {
		return accuracyPerRound.get(roundNumber);
	}

	/**
	 * Accuracy values (number of items) for all target users at round roundNumber.
	 */
	public TLongIntMap getAbsAccuraciesAt(int roundNumber) {
		return absAccuracyPerRound.get(roundNumber);
	}

	/**
	 * Stricter accuracy values (percentage) for all target users at round roundNumber.
	 */
	public TLongDoubleMap getStricterAccuraciesAt(int roundNumber) {
		return stricterAccuracyPerRound.get(roundNumber);
	}

	/**
	 * Stricter accuracy values (number of items) for all target users at round roundNumber.
	 */
	public TLongIntMap getStricterAbsAccuraciesAt(int roundNumber) {
		return stricterAbsAccuracyPerRound.get(roundNumber);
	}

	/**
	 * Fraction of neighborhoods as expected for all target users at round roundNumber.
	 */
	public TLongDoubleMap getExpectedNeighborhoodsFracAt(int roundNumber) {
		return expectedNeighborhoodsFracPerRound.get(roundNumber);
	}

	/**
	 * targetsAreNeighborsPerRound values for all target users at round roundNumber.
	 */
	public TLongDoubleMap getTargetsAreNeighborsAt(int roundNumber) {
		return targetsAreNeighborsPerRound.get(roundNumber);
	}

	/**
	 * targetsNbSybilNeighborsPerRound values for all target users at round roundNumber.
	 */
	public TLongDoubleMap getTargetsNbSybilNeighborsAt(int roundNumber) {
		return targetsNbSybilNeighborsPerRound.get(roundNumber);
	}

	/**
	 * nbPerfectMatches values for all target users at round roundNumber.
	 */
	public TLongIntMap getNbPerfectMatchesAt(int roundNumber) {
		return nbPerfectMatchesPerRound.get(roundNumber);
	}

	/**
	 * Fractions of unwanted neighbors (averages over each relevant Sybils' neighborhood) for all target users at round roundNumber.
	 */
	public TLongDoubleMap getFracOfUnwantedNeighborsAt(int roundNumber) {
		return fracOfUnwantedNeighborsPerRound.get(roundNumber);
	}

	/**
	 * Fractions of unwanted neighbors (averages over each relevant Sybils' neighborhood) for all target users at round roundNumber.
	 */
	public TLongDoubleMap getFracOfUnwantedNeighborsWTargetOnlyAt(int roundNumber) {
		return fracOfUnwantedNeighborsWTargetOnlyPerRound.get(roundNumber);
	}



	// Get 1 round's average values
	/**
	 * Average Sybil Infiltration over all target users at a given round.
	 */
	public double getAvgSybilInfiltrationAt(int round) {
		return avgSybilInfiltrationPerRound.get(round);
	}

	/**
	 * Average Yield over all target users at a given round.
	 */
	public double getAvgYieldAt(int round) {
		return avgYieldPerRound.get(round);
	}

	/**
	 * Average Accuracy (percentage) over all target users at a given round.
	 */
	public double getAvgAccuracyAt(int round) {
		return avgAccuracyPerRound.get(round);
	}

	/**
	 * Average Stricter Accuracy (percentage) over all target users at a given round.
	 */
	public double getAvgStricterAccuracyAt(int round) {
		return avgStricterAccuracyPerRound.get(round);
	}

	/**
	 * Average Accuracy (number of items) over all target users at a given round.
	 */
	public double getAvgAbsAccuracyAt(int round) {
		return avgAbsAccuracyPerRound.get(round);
	}

	/**
	 * Average Stricter Accuracy (number of items) over all target users at a given round.
	 */
	public double getAvgStricterAbsAccuracyAt(int round) {
		return avgStricterAbsAccuracyPerRound.get(round);
	}

	/**
	 * Average fraction of neighborhoods as expected over all target users at a given round.
	 */
	public double getAvgExpectedNeighborhoodsFracAt(int round) {
		return avgExpectedNeighborhoodsFracPerRound.get(round);
	}

	/**
	 * Average targetsAreNeighborsPerRound over all target users at a given round.
	 */
	public double getAvgTargetsAreNeighborsAt(int round) {
		return avgTargetsAreNeighborsPerRound.get(round);
	}

	/**
	 * Average targetsNbSybilNeighborsPerRound over all target users at a given round.
	 */
	public double getAvgTargetsNbSybilNeighborsAt(int round) {
		return avgTargetsAreNeighborsPerRound.get(round);
	}

	/**
	 * Average nbPerfectMatchesPerRound over all target users at a given round.
	 */
	public double getAvgNbPerfectMatchesAt(int round) {
		return avgNbPerfectMatchesPerRound.get(round);
	}

	/**
	 * Average fraction of unwanted neighbors over all target users at a given round.
	 */
	public double getAvgFracOfUnwantedNeighborsAt(int round) {
		return avgFracOfUnwantedNeighborsPerRound.get(round);
	}

	/**
	 * Average fraction of unwanted neighbors over all target users at a given round.
	 */
	public double getAvgFracOfUnwantedNeighborsWTargetOnlyAt(int round) {
		return avgFracOfUnwantedNeighborsWTargetOnlyPerRound.get(round);
	}

	// Get average values
	/**
	 * Average Sybil Infiltration over all target users and all rounds.
	 */
	public double getAvgSybilInfiltration() {
		if(sybilInfiltrationPerRound.size() == 1) {
			avgSybilInfiltration = avgSybilInfiltrationPerRound.get(1);
		} else {
			avgSybilInfiltration = averageSybilInfiltration();
		}
		return avgSybilInfiltration;
	}

	/**
	 * Average Yield over all target users and all rounds.
	 */
	public double getAvgYield() {
		if(yieldPerRound.size() == 1) {
			avgYield = avgYieldPerRound.get(1);
		} else {
			avgYield = averageYield();
		}
		return avgYield;
	}

	/**
	 * Average Accuracy (percentage) over all target users and all rounds.
	 */
	public double getAvgAccuracy() {
		if(accuracyPerRound.size() == 1) {
			avgAccuracy = avgAccuracyPerRound.get(1);
		} else {
			avgAccuracy = averageAccuracy();
		}
		return avgAccuracy;
	}

	/**
	 * Average Stricter Accuracy (percentage) over all target users and all rounds.
	 */
	public double getAvgStricterAccuracy() {
		if(stricterAccuracyPerRound.size() == 1) {
			avgStricterAccuracy = avgStricterAccuracyPerRound.get(1);
		} else {
			avgStricterAccuracy = averageStricterAccuracy();
		}
		return avgStricterAccuracy;
	}

	/**
	 * Average Accuracy (number of items) over all target users and all rounds.
	 */
	public double getAvgAbsAccuracy() {
		if(absAccuracyPerRound.size() == 1) {
			avgAbsAccuracy = avgAbsAccuracyPerRound.get(1);
		} else {
			avgAbsAccuracy = averageAbsAccuracy();
		}
		return avgAbsAccuracy;
	}

	/**
	 * Average Stricter Accuracy (number of items) over all target users and all rounds.
	 */
	public double getAvgStricterAbsAccuracy() {
		if(stricterAbsAccuracyPerRound.size() == 1) {
			avgStricterAbsAccuracy = avgStricterAbsAccuracyPerRound.get(1);
		} else {
			avgStricterAbsAccuracy = averageStricterAbsAccuracy();
		}
		return avgStricterAbsAccuracy;
	}

	/**
	 * Average fraction of neighborhood as expected over all target users and all rounds.
	 */
	public double getAvgExpectedNeighborhoodsFrac() {
		if(expectedNeighborhoodsFracPerRound.size() == 1) {
			avgExpectedNeighborhoodsFrac = avgExpectedNeighborhoodsFracPerRound.get(1);
		} else {
			avgExpectedNeighborhoodsFrac = averageExpectedNeighborhoodsFrac();
		}
		return avgExpectedNeighborhoodsFrac;
	}

	/**
	 * Average fraction of Sybils whose neighborhood contains their target over all target users and all rounds.
	 */
	public double getAvgTargetsAreNeighbors() {
		if(targetsAreNeighborsPerRound.size() == 1) {
			avgTargetsAreNeighbors = avgTargetsAreNeighborsPerRound.get(1);
		} else {
			avgTargetsAreNeighbors = averageTargetsAreNeighbors();
		}
		return avgTargetsAreNeighbors;
	}

	/**
	 * Average number of Sybils in a Sybil's neighborhood over all target users and all rounds.
	 */
	public double getAvgTargetsNbSybilNeighbors() {
		if(targetsNbSybilNeighborsPerRound.size() == 1) {
			avgTargetsNbSybilNeighbors = avgTargetsNbSybilNeighborsPerRound.get(1);
		} else {
			avgTargetsNbSybilNeighbors = averageTargetsNbSybilNeighbors();
		}
		return avgTargetsNbSybilNeighbors;
	}

	/**
	 * Average nbPerfectMatches over all target users and all rounds.
	 */
	public double getAvgNbPerfectMatches() {
		if(nbPerfectMatchesPerRound.size() == 1) {
			avgNbPerfectMatches = avgNbPerfectMatchesPerRound.get(1);
		} else {
			avgNbPerfectMatches = averageNbPerfectMatches();
		}
		return avgNbPerfectMatches;
	}

	/**
	 * Average fraction of unwanted neighbors over all target users and all rounds.
	 */
	public double getAvgFracOfUnwantedNeighbors() {
		if(fracOfUnwantedNeighborsPerRound.size() == 1) {
			avgFracOfUnwantedNeighbors = avgFracOfUnwantedNeighborsPerRound.get(1);
		} else {
			avgFracOfUnwantedNeighbors = averageFracOfUnwantedNeighbors();
		}
		return avgFracOfUnwantedNeighbors;
	}

	/**
	 * Average fraction of unwanted neighbors over all target users and all rounds.
	 */
	public double getAvgFracOfUnwantedNeighborsWTargetOnly() {
		if(fracOfUnwantedNeighborsWTargetOnlyPerRound.size() == 1) {
			avgFracOfUnwantedNeighborsWTargetOnly = avgFracOfUnwantedNeighborsWTargetOnlyPerRound.get(1);
		} else {
			avgFracOfUnwantedNeighborsWTargetOnly = averageFracOfUnwantedNeighborsWTargetOnly();
		}
		return avgFracOfUnwantedNeighborsWTargetOnly;
	}

	/**
	 * Frac of Sybil users that have all other Sybils in their neighborhood
	 */
	 public double getFracSybilsHaveAllSybilNeighbors() {
		 return fracSybilsHaveAllSybilNeighbors;
	 }
   
  /**
	 * 1 if all Sybils have only Sybils as neighbors, 0 otherwise
	 */
	 public double getAllSybilsOnlySybilNeighbors() {
		 return allSybilsOnlySybilNeighbors;
	 }
   
  /** Accuracy prime */
   public double getAccuracyPrime() {
    return accuracyPrime;
	 }

	/**
	 * Number of auxiliary items taken in the target profile
	 */
	public int getNbAuxiliaryItems() {
		return nbAuxiliaryItems;
	}

	// Core logic for average values computation
	/**
	 * Actual computation of the average Sybil Infiltration when there is more than 1 round.
	 */
	private double averageSybilInfiltration() {
		double result = 0.0;
		int nbValues = 0;
		for(TIntIterator it=sybilInfiltrationPerRound.keySet().iterator(); it.hasNext();) {
			int round = it.next();
			TLongDoubleMap fractionsOfTheRound = sybilInfiltrationPerRound.get(round);
			for(TLongIterator iter=fractionsOfTheRound.keySet().iterator(); iter.hasNext();) {
				long user = iter.next();
				result += fractionsOfTheRound.get(user);
				nbValues++;
			}
		}
		result = result / (double) nbValues;

		return result;
	}

	/**
	 * Actual computation of the average Yield when there is more than 1 round.
	 */
	private double averageYield() {
		double result = 0.0;
		int nbValues = 0;
		for(TIntIterator it=yieldPerRound.keySet().iterator(); it.hasNext();) {
			int round = it.next();
			TLongIntMap yieldsOfTheRound = yieldPerRound.get(round);
			for(TLongIterator iter=yieldsOfTheRound.keySet().iterator(); iter.hasNext();) {
				long user = iter.next();
				result += yieldsOfTheRound.get(user);
				nbValues++;
			}
		}
		result = result / (double) nbValues;

		return result;
	}

	/**
	 * Actual computation of the average Accuracy (percentage) when there is more than 1 round.
	 */
	private double averageAccuracy() {
		double result = 0.0;
		int nbValues = 0;
		for(TIntIterator it=accuracyPerRound.keySet().iterator(); it.hasNext();) {
			int round = it.next();
			TLongDoubleMap accuraciesOfTheRound = accuracyPerRound.get(round);
			for(TLongIterator iter=accuraciesOfTheRound.keySet().iterator(); iter.hasNext();) {
				long user = iter.next();
				result += accuraciesOfTheRound.get(user);
				nbValues++;
			}
		}
		result = result / (double) nbValues;

		return result;
	}

	/**
	 * Actual computation of the average Stricter Accuracy (percentage) when there is more than 1 round.
	 */
	private double averageStricterAccuracy() {
		double result = 0.0;
		int nbValues = 0;
		for(TIntIterator it=stricterAccuracyPerRound.keySet().iterator(); it.hasNext();) {
			int round = it.next();
			TLongDoubleMap accuraciesOfTheRound = stricterAccuracyPerRound.get(round);
			for(TLongIterator iter=accuraciesOfTheRound.keySet().iterator(); iter.hasNext();) {
				long user = iter.next();
				result += accuraciesOfTheRound.get(user);
				nbValues++;
			}
		}
		result = result / (double) nbValues;

		return result;
	}

	/**
	 * Actual computation of the average Accuracy (number of items) when there is more than 1 round.
	 */
	private double averageAbsAccuracy() {
		double result = 0.0;
		int nbValues = 0;
		for(TIntIterator it=absAccuracyPerRound.keySet().iterator(); it.hasNext();) {
			int round = it.next();
			TLongIntMap accuraciesOfTheRound = absAccuracyPerRound.get(round);
			for(TLongIterator iter=accuraciesOfTheRound.keySet().iterator(); iter.hasNext();) {
				long user = iter.next();
				result += accuraciesOfTheRound.get(user);
				nbValues++;
			}
		}
		result = result / (double) nbValues;

		return result;
	}

	/**
	 * Actual computation of the average Stricter Accuracy (number of items) when there is more than 1 round.
	 */
	private double averageStricterAbsAccuracy() {
		double result = 0.0;
		int nbValues = 0;
		for(TIntIterator it=stricterAbsAccuracyPerRound.keySet().iterator(); it.hasNext();) {
			int round = it.next();
			TLongIntMap accuraciesOfTheRound = stricterAbsAccuracyPerRound.get(round);
			for(TLongIterator iter=accuraciesOfTheRound.keySet().iterator(); iter.hasNext();) {
				long user = iter.next();
				result += accuraciesOfTheRound.get(user);
				nbValues++;
			}
		}
		result = result / (double) nbValues;

		return result;
	}

	/**
	 * Actual computation of the average fraction of neighborhood as expected when there is more than 1 round.
	 */
	private double averageExpectedNeighborhoodsFrac() {
		double result = 0.0;
		int nbValues = 0;
		for(TIntIterator it=expectedNeighborhoodsFracPerRound.keySet().iterator(); it.hasNext();) {
			int round = it.next();
			TLongDoubleMap fractionsOfTheRound = expectedNeighborhoodsFracPerRound.get(round);
			for(TLongIterator iter=fractionsOfTheRound.keySet().iterator(); iter.hasNext();) {
				long target = iter.next();
				result += fractionsOfTheRound.get(target);
				nbValues++;
			}
		}
		result = result / (double) nbValues;

		return result;
	}

	/**
	 * Actual computation of the average fraction of Sybils whose neighborhood contains their target when there is more than 1 round.
	 */
	private double averageTargetsAreNeighbors() {
		double result = 0.0;
		int nbValues = 0;
		for(TIntIterator it=targetsAreNeighborsPerRound.keySet().iterator(); it.hasNext();) {
			int round = it.next();
			TLongDoubleMap nbSybilsWithTargetNeighborOfTheRound = targetsAreNeighborsPerRound.get(round);
			for(TLongIterator iter=nbSybilsWithTargetNeighborOfTheRound.keySet().iterator(); iter.hasNext();) {
				long target = iter.next();
				result += nbSybilsWithTargetNeighborOfTheRound.get(target);
				nbValues++;
			}
		}
		result = result / (double) nbValues;

		return result;
	}

	/**
	 * Actual computation of the average number of Sybils in a Sybil's neighborhood when there is more than 1 round.
	 */
	private double averageTargetsNbSybilNeighbors() {
		double result = 0.0;
		int nbValues = 0;
		for(TIntIterator it=targetsNbSybilNeighborsPerRound.keySet().iterator(); it.hasNext();) {
			int round = it.next();
			TLongDoubleMap nbSybilNeighborsPerTargetOfTheRound = targetsNbSybilNeighborsPerRound.get(round);
			for(TLongIterator iter=nbSybilNeighborsPerTargetOfTheRound.keySet().iterator(); iter.hasNext();) {
				long target = iter.next();
				result += nbSybilNeighborsPerTargetOfTheRound.get(target);
				nbValues++;
			}
		}
		result = result / (double) nbValues;

		return result;
	}

	/**
	 * Actual computation of the average nbPerfectMatches when there is more than 1 round.
	 */
	private double averageNbPerfectMatches() {
		double result = 0.0;
		int nbValues = 0;
		for(TIntIterator it=nbPerfectMatchesPerRound.keySet().iterator(); it.hasNext();) {
			int round = it.next();
			TLongIntMap nbPerfectMatchesOfTheRound = nbPerfectMatchesPerRound.get(round);
			for(TLongIterator iter=nbPerfectMatchesOfTheRound.keySet().iterator(); iter.hasNext();) {
				long user = iter.next();
				result += nbPerfectMatchesOfTheRound.get(user);
				nbValues++;
			}
		}
		result = result / (double) nbValues;

		return result;
	}

	/**
	 * Actual computation of the average fraction of unwanted neighbors when there is more than 1 round.
	 */
	private double averageFracOfUnwantedNeighbors() {
		double result = 0.0;
		int nbValues = 0;
		for(TIntIterator it=fracOfUnwantedNeighborsPerRound.keySet().iterator(); it.hasNext();) {
			int round = it.next();
			TLongDoubleMap fractionsOfTheRound = fracOfUnwantedNeighborsPerRound.get(round);
			for(TLongIterator iter=fractionsOfTheRound.keySet().iterator(); iter.hasNext();) {
				long user = iter.next();
				result += fractionsOfTheRound.get(user);
				nbValues++;
			}
		}
		result = result / (double) nbValues;

		return result;
	}

	/**
	 * Actual computation of the average fraction of unwanted neighbors when there is more than 1 round.
	 */
	private double averageFracOfUnwantedNeighborsWTargetOnly() {
		double result = 0.0;
		int nbValues = 0;
		for(TIntIterator it=fracOfUnwantedNeighborsWTargetOnlyPerRound.keySet().iterator(); it.hasNext();) {
			int round = it.next();
			TLongDoubleMap fractionsOfTheRound = fracOfUnwantedNeighborsWTargetOnlyPerRound.get(round);
			for(TLongIterator iter=fractionsOfTheRound.keySet().iterator(); iter.hasNext();) {
				long user = iter.next();
				result += fractionsOfTheRound.get(user);
				nbValues++;
			}
		}
		result = result / (double) nbValues;

		return result;
	}


	// Get average values for 1 user
	/**
	 * Average Sybil Infiltration for user over all rounds.
	 */
	public double getAvgSybilInfiltrationFor(long user) {
		double result = -1.0;
		if(sybilInfiltrationPerRound.size() == 1) {
			result = sybilInfiltrationPerRound.get(1).get(user);
		} else {
			result = averageSybilInfiltrationFor(user);
		}
		avgSybilInfiltrationPerTarget.put(user, result);
		return result;
	}

	/**
	 * Average Yield for user over all rounds.
	 */
	public double getAvgYieldFor(long user) {
		double result = -1.0;
		if(yieldPerRound.size() == 1) {
			result = yieldPerRound.get(1).get(user);
		} else {
			result = averageYieldFor(user);
		}
		avgYieldPerTarget.put(user, result);
		return result;
	}

	/**
	 * Average Accuracy (percentage) for user over all rounds.
	 */
	public double getAvgAccuracyFor(long user) {
		double result = -1.0;
		if(accuracyPerRound.size() == 1) {
			result = accuracyPerRound.get(1).get(user);
		} else {
			result = averageAccuracyFor(user);
		}
		avgAccuracyPerTarget.put(user, result);
		return result;
	}

	/**
	 * Average Stricter Accuracy (percentage) for user over all rounds.
	 */
	public double getAvgStricterAccuracyFor(long user) {
		double result = -1.0;
		// TODO
		/*if(accuracyPerRound.size() == 1) {*/
			//result = accuracyPerRound.get(1).get(user);
		//} else {
			//result = averageAccuracyFor(user);
		//}
		/*avgAccuracyPerTarget.put(user, result);*/
		return result;
	}

	/**
	 * Average Accuracy (number of items) for user over all rounds.
	 */
	public double getAvgAbsAccuracyFor(long user) {
		double result = -1.0;
		if(absAccuracyPerRound.size() == 1) {
			result = absAccuracyPerRound.get(1).get(user);
		} else {
			// TODO
			//result = averageAbsAccuracyFor(user);
		}
		avgAbsAccuracyPerTarget.put(user, result);
		return result;
	}

	/**
	 * Average Stricter Accuracy (number of items) for user over all rounds.
	 */
	public double getAvgStricterAbsAccuracyFor(long user) {
		double result = -1.0;
		// TODO
		/*if(accuracyPerRound.size() == 1) {*/
			//result = accuracyPerRound.get(1).get(user);
		//} else {
			//result = averageAccuracyFor(user);
		//}
		/*avgAccuracyPerTarget.put(user, result);*/
		return result;
	}

	public double getAvgExpectedNeighborhoodsFracFor(long user) {
		// TODO
		return -1.0;
	}

	public double getAvgTargetsAreNeighborsFor(long user) {
		// TODO
		return -1.0;
	}

	public double getAvgTargetsNbSybilNeighborsFor(long user) {
		// TODO
		return -1.0;
	}

	public double getAvgNbPerfectMatchesFor(long user) {
		// TODO
		return -1.0;
	}

	public double getAvgFracOfUnwantedNeighborsFor(long user) {
		// TODO
		return -1.0;
	}

	public double getAvgFracOfUnwantedNeighborsWTargetOnlyFor(long user) {
		// TODO
		return -1.0;
	}

	/**
	 * Actual computation of the average Sybil Infiltration for user when there is more than 1 round.
	 */
	private double averageSybilInfiltrationFor(long user) {
		double result = 0.0;
		int nbValues = 0;
		for(TIntIterator it=sybilInfiltrationPerRound.keySet().iterator(); it.hasNext();) {
			int round = it.next();
			TLongDoubleMap fractionsOfTheRound = sybilInfiltrationPerRound.get(round);
			if(fractionsOfTheRound.containsKey(user)) {
				result += fractionsOfTheRound.get(user);
				nbValues++;
			}
		}
		result = result / (double) nbValues;

		return result;
	}

	/**
	 * Actual computation of the average Yield for user when there is more than 1 round.
	 */
	private double averageYieldFor(long user) {
		double result = 0.0;
		int nbValues = 0;
		for(TIntIterator it=yieldPerRound.keySet().iterator(); it.hasNext();) {
			int round = it.next();
			TLongIntMap yieldsOfTheRound = yieldPerRound.get(round);
			if(yieldsOfTheRound.containsKey(user)) {
				result += yieldsOfTheRound.get(user);
				nbValues++;
			}
		}
		result = result / (double) nbValues;

		return result;
	}

	/**
	 * Actual computation of the average Accuracy for user when there is more than 1 round.
	 */
	private double averageAccuracyFor(long user) {
		double result = 0.0;
		int nbValues = 0;
		for(TIntIterator it=accuracyPerRound.keySet().iterator(); it.hasNext();) {
			int round = it.next();
			TLongDoubleMap accuraciesOfTheRound = accuracyPerRound.get(round);
			if(accuraciesOfTheRound.containsKey(user)) {
				result += accuraciesOfTheRound.get(user);
				nbValues++;
			}
		}
		result = result / (double) nbValues;

		return result;
	}

	/**
	 * Number of rounds recorded in this object.
	 */
	public int getNbRounds() {
		return yieldPerRound.size();
	}



	// Static methods
	/**
	 * Gather statistics from s1 and s2 in one object.
	 * Rounds from s2 are considered as posterior to the ones from s1.
	 */
	public static AttackStats add(AttackStats s1, AttackStats s2) {
		AttackStats result = new AttackStats(s1.getNbRounds()+s2.getNbRounds());
		for(int i=1; i<=s1.getNbRounds(); i++) {
			result.setSybilInfiltration(s1.getSybilInfiltrationsAt(i));
			result.setYield(s1.getYieldsAt(i));
			result.setAccuracy(s1.getAccuraciesAt(i));
			result.setStricterAccuracy(s1.getStricterAccuraciesAt(i));
			result.setAbsAccuracy(s1.getAbsAccuraciesAt(i));
			result.setStricterAbsAccuracy(s1.getStricterAbsAccuraciesAt(i));
			result.setExpectedNeighborhoodsFrac(s1.getExpectedNeighborhoodsFracAt(i));
			result.setTargetsAreNeighbors(s1.getTargetsAreNeighborsAt(i));
			result.setTargetsNbSybilNeighbors(s1.getTargetsNbSybilNeighborsAt(i));
			result.setNbPerfectMatches(s1.getNbPerfectMatchesAt(i));
			result.setFracOfUnwantedNeighbors(s1.getFracOfUnwantedNeighborsAt(i));
			result.setFracOfUnwantedNeighborsWTargetOnly(s1.getFracOfUnwantedNeighborsWTargetOnlyAt(i));
		}
		for(int i=1; i<=s2.getNbRounds(); i++) {
			result.setSybilInfiltration(s2.getSybilInfiltrationsAt(i));
			result.setYield(s2.getYieldsAt(i));
			result.setAccuracy(s2.getAccuraciesAt(i));
			result.setStricterAccuracy(s2.getStricterAccuraciesAt(i));
			result.setAbsAccuracy(s2.getAbsAccuraciesAt(i));
			result.setStricterAbsAccuracy(s2.getStricterAbsAccuraciesAt(i));
			result.setExpectedNeighborhoodsFrac(s2.getExpectedNeighborhoodsFracAt(i));
			result.setTargetsAreNeighbors(s2.getTargetsAreNeighborsAt(i));
			result.setTargetsNbSybilNeighbors(s2.getTargetsNbSybilNeighborsAt(i));
			result.setNbPerfectMatches(s2.getNbPerfectMatchesAt(i));
			result.setFracOfUnwantedNeighbors(s2.getFracOfUnwantedNeighborsAt(i));
			result.setFracOfUnwantedNeighborsWTargetOnly(s2.getFracOfUnwantedNeighborsWTargetOnlyAt(i));
		}
		return result;
	}

	// Actually not needed
	/**
	 * Average each statistics in stats over nbElements.
	 */
	//public static AttackStats average(AttackStats stats, int nbElements) {
		//return null;
	/*}*/



	// Utilities
	/**
	 * Utility function computing the average of all values contained in a TLongDoubleMap.
	 * Should be moved to a different class.
	 */
	public static double averageLongDoubleMapValues(TLongDoubleMap map) {
		double result = 0.0;
		for(TLongIterator it=map.keySet().iterator(); it.hasNext();) {
			result += map.get(it.next());
		}
		return result / (double) map.size();
	}

	/**
	 * Utility function computing the average of all values contained in a TLongIntMap.
	 * Should be moved to a different class.
	 */
	public static double averageLongIntMapValues(TLongIntMap map) {
		double result = 0.0;
		for(TLongIterator it=map.keySet().iterator(); it.hasNext();) {
			result += map.get(it.next());
		}
		return result / (double) map.size();
	}

	/**
	 * Utility function computing the average of all integer values contained in all the TIntArrayList from map.
	 */
	public static double averageLongIntArrayList(TLongObjectMap<TIntArrayList> map) {
		double result = 0.0;
		int nbValues = 0;
		for(TLongIterator it=map.keySet().iterator(); it.hasNext();) {
			TIntArrayList oneTargetsValues = map.get(it.next());
			for(TIntIterator iter=oneTargetsValues.iterator(); iter.hasNext();) {
				result += iter.next();
				nbValues++;
			}
		}
		return result / (double) nbValues;
	}

	/**
	 * Utility function computing the average of all integer values contained in array.
	 */
	public static double averageIntArrayList(TIntArrayList array) {
		double result = 0.0;
		for(TIntIterator it=array.iterator(); it.hasNext();) {
			result += it.next();
		}
		return result / (double) array.size();
	}

	/**
	 * Utility function computing the average of all double values contained in array.
	 */
	public static double averageDoubleArrayList(TDoubleArrayList array) {
		double result = 0.0;
		for(TDoubleIterator it=array.iterator(); it.hasNext();) {
			result += it.next();
		}
		return result / (double) array.size();
	}

}
