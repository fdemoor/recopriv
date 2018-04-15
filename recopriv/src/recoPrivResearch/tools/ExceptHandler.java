package recoPrivResearch.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Random;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.eval.DataModelBuilder;
import org.apache.mahout.cf.taste.model.DataModel;
//import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.AveragingPreferenceInferrer;

import com.opencsv.CSVWriter;

import recoPrivResearch.tools.Parameters;

/**
 * Collection of wrapper functions around library functions which require exception handling.
 */

public class ExceptHandler {

	/**
	 * Simple wrapper around UserSimilarity.userSimilarity().
	 * Returns Nan if the similarity of user1 and user2 is unknown.
	 */
	public static double getSim(UserSimilarity sim, long user1, long user2) {
		double result = -1.0;
		try {
			result = sim.userSimilarity(user1, user2);
		} catch(TasteException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Simple wrapper around DataModelBuilder.buildDataModel().
	 * Create a new DataModel based on model with Sybil users injected according to modelBuilder.
	 */
	public static DataModel injectSybils(DataModelBuilder modelBuilder, DataModel model) {
		DataModel attackedModel = null;
		try {
			attackedModel = modelBuilder.buildDataModel(GenericDataModel.toDataMap(model));
		} catch(TasteException e) {
			e.printStackTrace();
		}
		return attackedModel;
	}

	/**
	 * Simple wrapper around RecommenderBuilder.buildRecommender().
	 */
	public static Recommender buildRecSys(RecommenderBuilder builder, DataModel model) {
		Recommender rec = null;
		try {
			rec = builder.buildRecommender(model);
		} catch(TasteException e) {
			e.printStackTrace();
		}
		return rec;
	}

	/**
	 * Simple wrapper around Recommender.recommend().
	 * TODO: update comments to reflect use of choiceBehavior and rand, similarly to getNeighborhood.
	 */
	public static List<RecommendedItem> getRecommendations(Recommender recsys, long userID, int nbReco, String choiceBehavior, Random rand) {
		List<RecommendedItem> result = null;
		try {
			result = ((GenericUserBasedRecommender) recsys).recommend(userID, nbReco, choiceBehavior, rand);
		} catch(TasteException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Simple wrapper around UserNeighborhood.getUserNeighborhood().
	 * TODO: update comments from boolean randomChoice to String choiceBehavior
	 * If randomChoice is false, use Mahout's default method.
	 * Else use a modified version which chooses randomly among equally similar neighbors, using rand as source of randomness.
	 * Returns null if ID has no neighbors in neighborhood.
	 */
	public static long[] getNeighborhood(UserNeighborhood neighborhood, long ID, String choiceBehavior, Random rand) {
		long[] result = null;
		try {
			/*if(!randomChoice) {*/
				//result = neighborhood.getUserNeighborhood(ID);
			//} else {
				//result = neighborhood.getUserNeighborhood(ID, rand);
			/*}*/
			result = neighborhood.getUserNeighborhood(ID, choiceBehavior, rand);
		} catch(TasteException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Simple wrapper around DataModel.getPreferenceValue().
	 * Returns -1.0 if userID has no opinion about itemID in model.
	 */
	public static float getItemValue(DataModel model, long userID, long itemID) {
		float result = (float) -1.0;
		try  {
			Float pref = model.getPreferenceValue(userID, itemID);
			if(pref != null) { // userID has an opinion on itemID
				result = pref;
			}
		} catch(TasteException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Simple wrapper around DataModel.getNumUsers().
	 */
	public static int getModelNumUsers(DataModel model) {
		int nbUsers = -1;
		try {
			nbUsers = model.getNumUsers();
		} catch(TasteException e) {
			e.printStackTrace();
		}
		return nbUsers;
	}

	/**
	 * Simple wrapper around DataModel.getNumItems().
	 */
	public static int getModelNumItems(DataModel model) {
		int nbUsers = -1;
		try {
			nbUsers = model.getNumItems();
		} catch(TasteException e) {
			e.printStackTrace();
		}
		return nbUsers;
	}


	/**
	 * Simple wrapper around GenericDataModel.toDataMap().
	 */
	public static FastByIDMap<PreferenceArray> dataModelToFastByIDMap(DataModel model) {
		FastByIDMap<PreferenceArray> result = null;
		try {
			result = GenericDataModel.toDataMap(model);
		} catch(TasteException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Simple wrapper around DataModel.getPreferencesFromUser().
	 */
	public static PreferenceArray getPreferences(DataModel model, long userID) {
		PreferenceArray result = null;
		try {
			result = model.getPreferencesFromUser(userID);
		} catch(TasteException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Simple wrapper around DataModel.setPreference().
	 */
	public static void setRating(DataModel model, long userID, long itemID, float rating) {
		try {
			model.setPreference(userID, itemID, rating);
		} catch(TasteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Simple wrapper around FileDataModel's constructor.
	 */
	public static DataModel createDataModel(String datasetPath) {
		DataModel model = null;
		try{
			model = new FileDataModel(new File(datasetPath));
		} catch(IOException e) {
			System.out.println(e);
		}
		return model;
	}

	/**
	 * Simple wrapper around Parameters.log().
	 */
	public static void logParameters(Parameters params) {
		try{
			params.log();
		} catch(IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Simple wrapper around DataModel.getUserIDs().
	 */
	public static LongPrimitiveIterator getModelUserIDs(DataModel model) {
		LongPrimitiveIterator result = null;
		try {
			result = model.getUserIDs();
		} catch(TasteException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Simple wrapper around AveragingPreferenceInferrer's constructor.
	 */
	public static AveragingPreferenceInferrer getAveragingPrefInferrer(DataModel model) {
		AveragingPreferenceInferrer result = null;
		try {
			result = new AveragingPreferenceInferrer(model);
		} catch(TasteException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Simple wrapper around FileInputStream's constructor.
	 */
	public static FileInputStream createFileInputStream(String path) {
		FileInputStream result = null;
		try {
			result = new FileInputStream(path);
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Simple wrapper around ObjectInputStream's constructor.
	 */
	public static ObjectInputStream createObjectInputStream(FileInputStream file) {
		ObjectInputStream result = null;
		try {
			result = new ObjectInputStream(file);
		} catch(IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Simple wrapper around FileWriter's constructor.
	 */
	public static FileWriter createFileWriter(String path) {
		FileWriter result = null;
		try {
			result = new FileWriter(path);
		} catch(IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Simple wrapper around CSVWriter.close().
	 */
	public static void closeCSVWriter(CSVWriter writer) {
		try {
			writer.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
