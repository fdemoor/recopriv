package org.apache.mahout.cf.taste.impl.eval;

import org.apache.mahout.cf.taste.impl.common.FullLongRunningAverage;
import org.apache.mahout.cf.taste.impl.common.LongRunningAverage;
import org.apache.mahout.cf.taste.model.Preference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * A {@link org.apache.mahout.cf.taste.eval.RecommenderEvaluator} which computes the "root mean squared"
 * difference between predicted and actual ratings for users. This is the square root of the average of this
 * difference, squared.
 * This class runs sequentially.
 * </p>
 */
public final class RMSRecommenderSequentialEvaluator extends AbstractDifferenceRecommenderSequentialEvaluator {
  
  private static final Logger log = LoggerFactory.getLogger(RMSRecommenderSequentialEvaluator.class);

  private LongRunningAverage average;
  
  @Override
  protected void reset() {
    average = new FullLongRunningAverage();
  }
  
  @Override
  protected void processOneEstimate(float estimatedPreference, Preference realPref) {
    log.debug("Real pref={}, predicted pref={}", realPref.getValue(), estimatedPreference);

    double diff = realPref.getValue() - estimatedPreference;
    log.debug("Squared Error={}", diff*diff);
    average.addDatum(diff * diff);

    log.debug("Prediction nb: {}", average.getCount());
    log.debug("Running avg: {}", average.getAverage());
  }
  
  @Override
  protected double computeFinalEvaluation() {
    log.debug("Final nb of predictions: {}", average.getCount());
    return Math.sqrt(average.getAverage());
  }
  
  @Override
  public String toString() {
    return "RMSRecommenderSequentialEvaluator";
  }
  
}
