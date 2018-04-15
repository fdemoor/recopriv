package org.apache.mahout.cf.taste.impl.eval;

import org.apache.mahout.cf.taste.impl.common.FullLongRunningAverage;
import org.apache.mahout.cf.taste.impl.common.LongRunningAverage;
import org.apache.mahout.cf.taste.model.Preference;

/**
 * <p>
 * A {@link org.apache.mahout.cf.taste.eval.RecommenderEvaluator} which computes the average absolute
 * difference between predicted and actual ratings for users.
 * </p>
 * 
 * <p>
 * This algorithm is also called "mean average error".
 * This class runs sequentially.
 * </p>
 */
public final class AverageAbsoluteDifferenceRecommenderSequentialEvaluator extends
    AbstractDifferenceRecommenderSequentialEvaluator {
  
  private LongRunningAverage average;
  
  @Override
  protected void reset() {
    average = new FullLongRunningAverage();
  }
  
  @Override
  protected void processOneEstimate(float estimatedPreference, Preference realPref) {
    average.addDatum(Math.abs(realPref.getValue() - estimatedPreference));
  }
  
  @Override
  protected double computeFinalEvaluation() {
    return average.getAverage();
  }
  
  @Override
  public String toString() {
    return "AverageAbsoluteDifferenceRecommenderSequentialEvaluator";
  }
  
}
