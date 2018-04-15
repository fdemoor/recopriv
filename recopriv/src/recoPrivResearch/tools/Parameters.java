package recoPrivResearch.tools;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import recoPrivResearch.tools.FilesLoader;

/**
 * A Parameters object gathers all parameters of an experiment as attribute members.
 * Parameters values are read from the exportParameterVariables.sh file, and
 * automatically assigned to the corresponding attribute.
 * The main method create the exportParameterVariables.sh and generateParametersString.sh
 * files with default values for all parameters.
 * To add a parameters, add a public attribute to this class with a default value and
 * a name ending with '_'.
 */
public class Parameters {

	private static final Logger paramsLogger = LogManager.getLogger(Parameters.class.getName() + "Logging");
	private static final Logger logger = LogManager.getLogger(Parameters.class);

	public long seed_ = 862576678;

	// Required pathes
	public String baseDir_ = "dummy/path";
	public String log4jConfFile_ = "$baseDir/scripts/log4j.properties";
	public String datasetPath_ = "$baseDir/datasets/movieLens/ml-100k/ratings.csv";
	public String testingPath_ = "$baseDir/datasets/movieLens/ml-100k/u.test"; // Unused
	public String outputPath_ = "$baseDir/output/";

	// IDs for experiment and source code tracking
	public int experimentId_ = 0; // Unused
	public String vcsVersionId_ = "unknown";

	// Similarity measure related
	public String similarityType_ = "cosine";
	public boolean twostepUseThreshold_ = true;
	public int twostepIdealNbItems_ = 400;
	public int twostepAttack_ = 0; // Level of attack against 2step
	public int nbExtraItemPerSybil_ = 0; // used if twostepAttack is 1 or 2
	public String twostepFirstStepType_ = "cosine"; // Similarity metric used for first step
	public boolean isGlobalIdealNbItems_ = true; // Use same 'percentileIdealNbItems' for all users
	public double percentIdealNbItems_ = 1.5; // If not global, use percent of profile size
	public boolean randomizeIdealNbItems_ = false;
	public String variantIdealNbItems_ = "u";
	public double percentAdditionalItems_ = 1.0;


	// Number of neighbours for each user
	public int k_ = 10;
	public String neighborhoodType_ = "knn"; // other value = "random" or "ppns"
	public int beta_ = 4; // security metric in PPNS method

	// Evaluators related
	public double likeThreshold_ = 3; // minimum rating for an item to be considered relevant/liked
	public double trainingPercentage_ = 0.8; // Percentage of each user's ratings to use for training
	public double validationPercentage_ = 1; // Percentage of *users* to consider for the evaluation
	public boolean useSequentialEvaluators_ = true;

	// MAE & RMSE evaluators toggles
	public boolean computeMae_ = false;
	public boolean computeRmse_ = false;

	// Information Retrieval Stats realted
	public boolean computeIRStats_ = false;
	public int r_ = 5;  // maximum number of recommendations per user, for precision

	// Information about users above threshold, require 2step metric
	public boolean computeAboveThres_ = false;

	// Information about run-time
	public boolean computeBuildRunTime_ = false;

	// Information about profile size
	public boolean computeSizeDistrib_ = false;

	// Information about memory used
	public boolean computeBuildMemory_ = false;

	// Information about theoretical hypotheses
	public boolean computeHypotheses_ = false;

	// Privacy evaluation related
	public boolean doSybilAttack_ = false;
	public int nbAttackers_ = 1; // Unused
	public int nbTargets_ = 1;
	public long targetID_ = 1; // Value range: [1:<dataset's nb users>]
	public boolean notRatedItemConsideredDisliked_ = true; // Deprecated
	public boolean relativeNbAuxItems_ = true; // Use nbAuxiliaryItems_ if true, percentAuxiliaryItems_ otherwise
	public int nbAuxiliaryItems_ = 8; // Number of items/user known to the attacker to be in their profile
	public double percentAuxiliaryItems_ = 0.2; // Within [0,1], percentage of the target's profile given as aux. items to Sybils
	public String auxiliaryItemsSelectionStrategy_ = "random"; // How auxiliary information is selected
	public int bestWorstFrac_ = 2; // Repartition of best and worst in bestWorstRated strategy
	public int nbSybils_ = 10;
	public String sybilRatingsStrategy_ = "groundtruth"; // How Sybils rate auxiliary items. Can be "neutral", "max", "liked" or "groundtruth".
	public boolean adaptiveSybils_ = false; // Do Sybil users add recommended items in their profile
	public int adaptiveSybilsNbRounds_ = 10; // How many iterations of: get recommendations -> update profile, for Sybil users
	public int sybilsNbRecoPerRound_ = 1; // How many recommendations for each Sybil user to produce per round
	public String neighborChoiceBehavior_ = "random"; // Choose between equally similar neighbors: lower IDs first ("lower"), randomly ("random"), higher IDs first ("higher")
	public boolean doOnlyPrecomputations_ = false; // Compute everything needed for the attack (Sybils, aux. items, etc) but do not perform the attack

	// Number of turns to run the evaluation with same dataset
	public int nbRuns_ = 1;

	static {
		/*
		 * Make sure results are formatted consistently, even on French systems.
		 * Parameters has to be imported and accessed for the locale setting to
		 * be applied.
		 */
		Locale.setDefault(Locale.ENGLISH);
	}

	// Not a parameter
	private final String[] rawParameters;

	public Parameters(String[] params) {
		rawParameters = params;
		autoAssign(params);
	}

	private void autoAssign(String[] params) {
		Map<String, String> parameters = readParameters(params);
		for (String param : parameters.keySet()) {
			try {
				String fieldName = getFieldName(param);
				Field field = this.getClass().getDeclaredField(fieldName);
				if (!setFieldValue(field, parameters.get(param))) {
					logger.warn("Could not assign a parameter: tried to set {} to {}", param, parameters.get(param));
				} else {
					logger.debug("Successfully set {} to {}", param, parameters.get(param));
				}
			} catch (NoSuchFieldException e) { // No field corresponding to the parameter.
				logger.warn("Unknown parameter: {}", param);
			} catch (SecurityException e) { // Should not happen.
				e.printStackTrace();
			}
		}
	}

	private boolean setFieldValue(Field field, String value) {
		try {
			field.setBoolean(this, Boolean.parseBoolean(value));
			return true;
		} catch (Exception e) {
			// not bool
		}
		try {
			field.setFloat(this, Float.parseFloat(value));
			return true;
		} catch (Exception e) {
			// not float
		}
		try {
			field.setDouble(this, Double.parseDouble(value));
			return true;
		} catch (Exception e) {
			// not double
		}
		try {
			field.setShort(this, Short.parseShort(value));
			return true;
		} catch (Exception e) {
			// not short
		}
		try {
			field.setInt(this, Integer.parseInt(value));
			return true;
		} catch (Exception e) {
			// not int
		}
		try {
			field.setLong(this, Long.parseLong(value));
			return true;
		} catch (Exception e) {
			// not long
		}
		try {
			field.setByte(this, Byte.parseByte(value));
			return true;
		} catch (Exception e) {
			// not byte
		}
		try {
			field.set(this, value);
			return true;
		} catch (Exception e) {
			// not String
		}

		return false;
	}

	/**
	 * Generate the two files used to pass the parameters to java from the CLI.
	 * exportParameterVariables.sh export all parameters as Bash environment variables.
	 * generateParametersString.sh generates the string of parameters to be passed to java.
	 * A Bash script uses them by first sourcing exportParameterVariables.sh, then passing
	 * the standard output of generateParametersString.sh as an argument for java.
	 */
	public void generateFiles(String out) throws IllegalArgumentException, IllegalAccessException {
		String baseParams = "#!/bin/bash\n";
		String baseParamsName = "exportParameterVariables.sh";

		String generateParams = "#!/bin/bash\nparams=\"\n";
		String generateParamsName = "generateParametersString.sh";

		for (Field field : this.getClass().getFields()) {
			String fieldName = field.getName();
			String entryName = fieldName.substring(0, fieldName.length() - 1);
			String value = field.get(this).toString();
			baseParams += "export " + entryName + "=" + value + "\n";
			generateParams += entryName + " $" + entryName + "\n";
		}

		generateParams += "\"\n";
		generateParams += "echo $params";

		FilesLoader.logString(out + baseParamsName, baseParams);
		FilesLoader.logString(out + generateParamsName, generateParams);

		String dir = Paths.get(".").toAbsolutePath().normalize().toString();
		logger.info("Created helper scripts {} and {} in {}", baseParamsName, generateParamsName, dir);
	}

	public String getParametersString(String[] params) {
		String str = "";
		try {
			Map<String, String> parameters = readParameters(params);
			for (Field field : this.getClass().getFields()) {
				String fieldName = field.getName();
				String entryName = fieldName.substring(0, fieldName.length() - 1);
				str += fieldName + " " + field.get(this).toString();
				if (parameters.containsKey(entryName)) {
					str += " assigned ";
				} else {
					str += " unassigned ";
					logger.info("Unassigned parameter: {}", fieldName);
				}
				str += "\n";
				if (field.get(this) == null) {
					logger.warn("The field {} is not initialised!", field.getName());
				}
			}
		} catch (Exception e) {
			str = e.toString();
		}
		return str;
	}

	public String logParameters(String logDir, String baseName) {

		// Creating the file content
		String str = "";
		try {
			Map<String, String> parameters = readParameters(rawParameters);
			for (Field field : this.getClass().getFields()) {
				String fieldName = field.getName();
				String entryName = fieldName.substring(0, fieldName.length() - 1);
				str += fieldName + " " + field.get(this).toString();
				if (parameters.containsKey(entryName)) {
					str += " assigned ";
				} else {
					str += " unassigned ";
				}
				str += "\n";
				if (field.get(this) == null) {
					logger.warn("The field {} is not initialised!", field.getName());
				}
			}
		} catch (Exception e) {
			str = e.toString();
		}

		// Logging the parameters
		FilesLoader.logString(logDir + baseName + ".parameters.txt", str);

		return str;

	}

	protected static Map<String, String> readParameters(String[] args) {
		Map<String, String> arguments = new HashMap<String, String>();
		try {
			logger.debug("Called with the following parameters:");
			for (int i = 0; i < args.length; i++) {
				logger.debug("Parameter: {}, value: {}", args[i], args[i + 1]);
				arguments.put(args[i], args[i + 1]);
				i++;
			}

		} catch (RuntimeException e) {
			logger.error("Exception while reading parameters:");
			for (int i = 0; i < args.length; i++) {
				logger.error("Parameter: {}, value: {}", args[i], args[i + 1]);
				i++;
			}
			throw e;
		}
		return arguments;
	}

	// TODO: Throw an exception if the field does not exist.
	private static String getFieldName(String param) {
		return param + "_";
	}

	/**
	 * Generate helper scripts which ease handling of the simulation parameters
	 * in a launcher shell script. Files are generated in the current working
	 * directory when running this method.
	 *
	 * @param toto
	 *            Optionnal values to replace the default parameters
	 */
	public static void main(String[] toto) throws IllegalArgumentException, IllegalAccessException {
		Parameters params = new Parameters(toto);
		params.generateFiles("./");
	}

	public void log() throws IllegalAccessException {
		logger.debug("Logging parameters in parameters.txt, through the Logger named {} (the other Logger being {})", paramsLogger.getName(), logger.getName());
		for (Field field : this.getClass().getFields()) {
			String fieldName = field.getName();
			String entryName = fieldName.substring(0, fieldName.length() - 1);
			String value = field.get(this).toString();
			logger.trace("About to write: {}={}",entryName, value);

			paramsLogger.info("{}={}", entryName, value);
		}
	}
}
