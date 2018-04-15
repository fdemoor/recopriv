package org.apache.mahout.cf.taste.impl.eval;

import org.apache.mahout.cf.taste.impl.common.FullLongRunningAverage;
import org.apache.mahout.cf.taste.impl.common.LongRunningAverage;
import org.apache.mahout.cf.taste.model.Preference;

public class RMSRecommenderEvaluatorKFold extends AbstractKFoldRecommenderEvaluator {

private LongRunningAverage average;


@Override
protected void reset() {
    average = new FullLongRunningAverage();
}

@Override
protected void processOneEstimate(float estimatedPreference, Preference realPref) {
    double diff = realPref.getValue() - estimatedPreference;
    average.addDatum(diff * diff);
}

@Override
protected double computeFinalEvaluation() {
    return Math.sqrt(average.getAverage());
}

@Override
public String toString() {
    return "RMSRecommenderEvaluator";
}}
