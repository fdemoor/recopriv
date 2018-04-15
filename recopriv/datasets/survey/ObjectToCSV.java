import java.io.FileWriter;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;


public class ObjectToCSV {

  public static Hashtable<Integer, ArrayList<Integer>> loadDistributionHT(String f) {

		Hashtable<Integer, ArrayList<Integer>> p = null;

		try {
			FileInputStream fos = new FileInputStream(f);
			ObjectInputStream oos = new ObjectInputStream(fos);

			p = (Hashtable<Integer, ArrayList<Integer>>) oos.readObject();

			fos.close();

		} catch(Exception e) {
			e.printStackTrace();
		}

		return p;
	}

  public static ArrayList<Integer> loadDistributionAL(String f) {

		ArrayList<Integer> p = null;

		try {
			FileInputStream fos = new FileInputStream(f);
			ObjectInputStream oos = new ObjectInputStream(fos);

			p = (ArrayList<Integer>) oos.readObject();

			fos.close();

		} catch(Exception e) {
			e.printStackTrace();
		}

		return p;
	}


  public static void main(String[] args) {

    try {
      FileWriter outputFile = new FileWriter("survey.csv");
      FileWriter infoFile = new FileWriter("info.log");

      Hashtable<Integer, ArrayList<Integer>> rumors = loadDistributionHT("surveyTrace.nbRumor1035.nbUser390.profiles.object");
      Iterator<Integer> usersIT = rumors.keySet().iterator();

      ArrayList<Integer> userList = loadDistributionAL("surveyTrace.nbRumor1035.nbUser390.userlist.object");
      ArrayList<Integer> itemList = loadDistributionAL("surveyTrace.nbRumor1035.nbUser390.rumorlist.object");

      int nbRatings = 0, nbUsers = userList.size(), nbItems = itemList.size();

      infoFile.write("Number of users: " + String.valueOf(nbUsers) + "\n");
      infoFile.write("Number of items: " + String.valueOf(nbItems) + "\n");

      int user = 0, item = 0;
      while (usersIT.hasNext()) {
        user = usersIT.next();
        ListIterator<Integer> prefIT = rumors.get(user).listIterator();
        while (prefIT.hasNext()) {
          nbRatings++;
          item = prefIT.next();
          outputFile.write(String.valueOf(user));
          outputFile.write(",");
          outputFile.write(String.valueOf(item));
          outputFile.write(",,1\n");
        }
      }

      infoFile.write("Number of ratings: " + String.valueOf(nbRatings) + "\n");

      double sparsity = (1 - nbRatings / (double)(nbUsers * nbItems)) * 100;
      infoFile.write("Sparsity: " + String.valueOf(sparsity) + "\n");

      outputFile.close();
      infoFile.close();

    } catch(IOException e) {
			e.printStackTrace();
		}

  }

}
