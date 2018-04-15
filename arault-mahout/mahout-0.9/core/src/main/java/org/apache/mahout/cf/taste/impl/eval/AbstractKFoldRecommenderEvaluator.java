package org.apache.mahout.cf.taste.impl.eval;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Lists;
import org.apache.mahout.cf.taste.common.NoSuchItemException;
import org.apache.mahout.cf.taste.common.NoSuchUserException;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.DataModelBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.common.FullRunningAverageAndStdDev;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.common.RunningAverageAndStdDev;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericPreference;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.common.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public abstract class AbstractKFoldRecommenderEvaluator extends AbstractDifferenceRecommenderSequentialEvaluator {

private final Random random;
public double noEstimateCounterAverage = 0.0;
public double totalEstimateCount = 0.0;
public double totalEstimateCountAverage = 0.0;

private static final Logger log = LoggerFactory
        .getLogger(AbstractKFoldRecommenderEvaluator.class);

public AbstractKFoldRecommenderEvaluator() {
    super();
    random = RandomUtils.getRandom();
}

public double getNoEstimateCounterAverage(){
    return noEstimateCounterAverage;
}

public double getTotalEstimateCount(){
    return totalEstimateCount;
}

public double getTotalEstimateCountAverage(){
    return totalEstimateCountAverage;
}

/**
 * We use the same evaluate function from the RecommenderEvaluator interface
 * the trainingPercentage is used as the number of folds, so it can have
 * values bigger than 0 to the number of folds.
 */
@Override
public double evaluate(RecommenderBuilder recommenderBuilder,
        DataModelBuilder dataModelBuilder, DataModel dataModel,
        double trainingPercentage, double evaluationPercentage)
        throws TasteException {
    Preconditions.checkNotNull(recommenderBuilder);
    Preconditions.checkNotNull(dataModel);
    Preconditions.checkArgument(trainingPercentage >= 0.0,
            "Invalid trainingPercentage: " + trainingPercentage);

    Preconditions.checkArgument(evaluationPercentage >= 0.0
            && evaluationPercentage <= 1.0,
            "Invalid evaluationPercentage: " + evaluationPercentage);

    log.info("Beginning evaluation using {} of {}", trainingPercentage,
            dataModel);

    int numUsers = dataModel.getNumUsers();

    // Get the number of folds
    int noFolds = (int) trainingPercentage;

    // Initialize buckets for the number of folds
    List<FastByIDMap<PreferenceArray>> folds = new ArrayList<FastByIDMap<PreferenceArray>>();
    for (int i = 0; i < noFolds; i++) {
        folds.add(new FastByIDMap<PreferenceArray>(
                1 + (int) (i / noFolds * numUsers)));
    }

    // Split the dataModel into K folds per user
    LongPrimitiveIterator it = dataModel.getUserIDs();
    while (it.hasNext()) {
        long userID = it.nextLong();
        if (random.nextDouble() < evaluationPercentage) {
            splitOneUsersPrefs2(noFolds, folds, userID, dataModel);
        }
    }

    double result = Double.NaN;
    List<Double> intermediateResults = new ArrayList<Double>();
    List<Integer> unableToRecoomend = new ArrayList<Integer>();
    List<Integer> averageEstimateCounterIntermediate = new ArrayList<Integer>();

    noEstimateCounterAverage = 0.0;
    totalEstimateCount = 0.0;
    totalEstimateCountAverage = 0.0;
    int totalEstimateCounter = 0;

    // Rotate the folds. Each time only one is used for testing and the rest
    // k-1 folds are used for training
    for (int k = 0; k < noFolds; k++) {
        FastByIDMap<PreferenceArray> trainingPrefs = new FastByIDMap<PreferenceArray>(
                1 + (int) (evaluationPercentage * numUsers));
        FastByIDMap<PreferenceArray> testPrefs = new FastByIDMap<PreferenceArray>(
                1 + (int) (evaluationPercentage * numUsers));

        for (int i = 0; i < folds.size(); i++) {

            // The testing fold
            testPrefs = folds.get(k);

            // Build the training set from the remaining folds
            if (i != k) {
                for (Map.Entry<Long, PreferenceArray> entry : folds.get(i)
                        .entrySet()) {
                    if (!trainingPrefs.containsKey(entry.getKey())) {
                        trainingPrefs.put(entry.getKey(), entry.getValue());
                    } else {
                        List<Preference> userPreferences = new ArrayList<Preference>();
                        PreferenceArray existingPrefs = trainingPrefs
                                .get(entry.getKey());
                        for (int j = 0; j < existingPrefs.length(); j++) {
                            userPreferences.add(existingPrefs.get(j));
                        }

                        PreferenceArray newPrefs = entry.getValue();
                        for (int j = 0; j < newPrefs.length(); j++) {
                            userPreferences.add(newPrefs.get(j));
                        }
                        trainingPrefs.remove(entry.getKey());
                        trainingPrefs.put(entry.getKey(),
                                new GenericUserPreferenceArray(
                                        userPreferences));

                    }
                }
            }
        }

        DataModel trainingModel = dataModelBuilder == null ? new GenericDataModel(
                trainingPrefs) : dataModelBuilder
                .buildDataModel(trainingPrefs);

        Recommender recommender = recommenderBuilder
                .buildRecommender(trainingModel);

        Double[] retVal = getEvaluation(testPrefs, recommender);
        double intermediate = retVal[0];
        int noEstimateCounter = ((Double)retVal[1]).intValue();
        totalEstimateCounter += ((Double)retVal[2]).intValue();
        averageEstimateCounterIntermediate.add(((Double)retVal[2]).intValue());

        log.info("Evaluation result from fold {} : {}", k, intermediate);
        log.info("Average Unable to recommend  for fold {} in: {} cases out of {}", k, noEstimateCounter, ((Double)retVal[2]).intValue());
        intermediateResults.add(intermediate);
        unableToRecoomend.add(noEstimateCounter);

    }

    double sum = 0;
    double noEstimateSum = 0;
    double totalEstimateSum = 0;
    // Sum the results in each fold
    for (int i = 0; i < intermediateResults.size(); i++) {
        if (!Double.isNaN(intermediateResults.get(i))) {
            sum += intermediateResults.get(i);
            noEstimateSum+=unableToRecoomend.get(i);
            totalEstimateSum+=averageEstimateCounterIntermediate.get(i);
        }
    }

    if (sum > 0) {
        // Get an average for the folds
        result = sum / intermediateResults.size();
    }

    double noEstimateCount = 0;
    if(noEstimateSum>0){
        noEstimateCount = noEstimateSum / unableToRecoomend.size();
    }

    double avgEstimateCount = 0;
    if(totalEstimateSum>0){
        avgEstimateCount = totalEstimateSum / averageEstimateCounterIntermediate.size();
    }

    log.info("Average Evaluation result: {} ", result);
    log.info("Average Unable to recommend in: {} cases out of avg. {} cases or total {} ", noEstimateCount, avgEstimateCount, totalEstimateCounter);

    noEstimateCounterAverage = noEstimateCount;
    totalEstimateCount = totalEstimateCounter;
    totalEstimateCountAverage = avgEstimateCount;
    return result;
}

/**
 * Split the preference values for one user into K folds, randomly
 * Generate random number until is not the same as the previously generated on
 * in order to make sure that at least two buckets are populated.
 * 
 * @param k
 * @param folds
 * @param userID
 * @param dataModel
 * @throws TasteException
 */
private void splitOneUsersPrefs(int k,
        List<FastByIDMap<PreferenceArray>> folds, long userID,
        DataModel dataModel) throws TasteException {

    List<List<Preference>> oneUserPrefs = Lists
            .newArrayListWithCapacity(k + 1);
    for (int i = 0; i < k; i++) {
        oneUserPrefs.add(null);
    }

    PreferenceArray prefs = dataModel.getPreferencesFromUser(userID);

    int size = prefs.length();
    int previousBucket = -1;
    Double rand = -2.0;
    for (int i = 0; i < size; i++) {
        Preference newPref = new GenericPreference(userID,
                prefs.getItemID(i), prefs.getValue(i));
        do {
            rand = random.nextDouble() * k * 10;

            rand = (double) Math.floor(rand / 10);
            // System.out.println("inside Rand "+rand);

        } while (rand.intValue() == previousBucket);
        // System.out.println("outside rand "+rand);
        if (oneUserPrefs.get(rand.intValue()) == null) {
            oneUserPrefs.set(rand.intValue(), new ArrayList<Preference>());
        }
        oneUserPrefs.get(rand.intValue()).add(newPref);

        previousBucket = rand.intValue();

    }

    for (int i = 0; i < k; i++) {
        if (oneUserPrefs.get(i) != null) {
            folds.get(i).put(userID,
                    new GenericUserPreferenceArray(oneUserPrefs.get(i)));
        }
    }

}

/**
 * Split the preference values for one user into K folds, by shuffling.
 * First Shuffle the Preference array for the user. Then distribute the item-preference pairs
 * starting from the first buckets to the k-th bucket, and then start from the beggining.
 * 
 * @param k
 * @param folds
 * @param userID
 * @param dataModel
 * @throws TasteException
 */
private void splitOneUsersPrefs2(int k, List<FastByIDMap<PreferenceArray>> folds, long userID, DataModel dataModel) throws TasteException {

    List<List<Preference>> oneUserPrefs = Lists.newArrayListWithCapacity(k + 1);
    for (int i = 0; i < k; i++) {
        oneUserPrefs.add(null);
    }

    PreferenceArray prefs = dataModel.getPreferencesFromUser(userID);
    int size = prefs.length();


    List<Preference> userPrefs = new ArrayList<Preference>();
    Iterator<Preference> it = prefs.iterator();
    while (it.hasNext()) {
        userPrefs.add(it.next());
    }

    // Shuffle the items
    Collections.shuffle(userPrefs);

    int currentBucket = 0;
    for (int i = 0; i < size; i++) {
        if (currentBucket == k) {
            currentBucket = 0;
        }

        Preference newPref = new GenericPreference(userID, userPrefs.get(i).getItemID(), userPrefs.get(i).getValue());

        if (oneUserPrefs.get(currentBucket) == null) {
            oneUserPrefs.set(currentBucket, new ArrayList<Preference>());
        }
        oneUserPrefs.get(currentBucket).add(newPref);
        currentBucket++;
    }

    for (int i = 0; i < k; i++) {
        if (oneUserPrefs.get(i) != null) {
            folds.get(i).put(userID, new GenericUserPreferenceArray(oneUserPrefs.get(i)));
        }
    }

}

private Double[] getEvaluation(FastByIDMap<PreferenceArray> testPrefs, Recommender recommender) throws TasteException {
    reset();
    Collection<Callable<Void>> estimateCallables = Lists.newArrayList();
    AtomicInteger noEstimateCounter = new AtomicInteger();
    AtomicInteger totalEstimateCounter = new AtomicInteger();
    for (Map.Entry<Long, PreferenceArray> entry : testPrefs.entrySet()) {
        estimateCallables.add(new PreferenceEstimateCallable(recommender, entry.getKey(), entry.getValue(), noEstimateCounter));
    }
    log.info("Beginning evaluation of {} users", estimateCallables.size());
    RunningAverageAndStdDev timing = new FullRunningAverageAndStdDev();
    execute(estimateCallables, noEstimateCounter, timing);


    Double[] retVal = new Double[3];
    retVal[0] = computeFinalEvaluation();
    retVal[1] = (double) noEstimateCounter.get();
    retVal[2] = (double) totalEstimateCounter.get();
    //retVal.put(computeFinalEvaluation(), noEstimateCounter.get());
    //return computeFinalEvaluation();
    return retVal;
}}
