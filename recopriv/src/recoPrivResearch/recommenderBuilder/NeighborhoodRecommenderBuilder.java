package recoPrivResearch.recommenderBuilder;

import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;

public interface NeighborhoodRecommenderBuilder extends RecommenderBuilder {

	public UserNeighborhood getNeighborhood();

}
