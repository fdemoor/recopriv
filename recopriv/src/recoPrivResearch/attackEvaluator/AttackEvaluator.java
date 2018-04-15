package recoPrivResearch.attackEvaluator;

import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.eval.DataModelBuilder;
import org.apache.mahout.cf.taste.model.DataModel;

public interface AttackEvaluator {

	public AttackStats evaluate(RecommenderBuilder builder, DataModelBuilder modelBuilder, DataModel originalModel);

}
