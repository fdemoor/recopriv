package recoPrivResearch.tools;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class FilesLoader {

    @SuppressWarnings("unchecked")
    public static <T> T loadGeneric(String f) throws IOException, ClassNotFoundException {
	FileInputStream fos = new FileInputStream(f);
	ObjectInputStream oos = new ObjectInputStream(fos);
	T p = (T) oos.readObject();
	fos.close();
	return p;
    }

    public static void logString(String filename, String str) {
	logString(filename, str, false);
    }

    public static void logString(String filename, String str, boolean append) {
	try {
	    // Create non-existing directories in the path represented by
	    // filename
	    Path dirname = Paths.get(filename).getParent();
	    Files.createDirectories(dirname);
	    // Create file
	    FileWriter fstream = new FileWriter(filename, append);
	    BufferedWriter out = new BufferedWriter(fstream);
	    out.write(str);
	    out.close();
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
    }

    public static void saveWhatever(String filename, Object o) {
	try {
	    FileOutputStream fos = new FileOutputStream(filename);
	    ObjectOutputStream oos = new ObjectOutputStream(fos);

	    oos.writeObject(o);
	    oos.flush();
	    fos.close();

	} catch (IOException e1) {
	    e1.printStackTrace();
	}

	System.out.println("Saved Whatever in " + filename);
    }

    public static <T> ArrayList<T> getRandomBestNodes(TreeMap<Double, ? extends List<T>> notations, int nbNodes,
	    Random random) {

	ArrayList<T> sim = new ArrayList<T>();
	b: for (Double d : notations.descendingKeySet()) {
	    List<T> lst = notations.get(d);
	    Collections.shuffle(lst, random);
	    for (T t : lst) {
		if (sim.size() >= nbNodes) {
		    break b;
		}
		sim.add(t);
	    }
	}

	return sim;
    }

    public static <T> Map<T, Double> getRandomBestNodesMap(TreeMap<Double, ? extends List<T>> notations, int nbNodes,
	    Random random) {

	Map<T, Double> sim = new HashMap<T, Double>();
	b: for (Double d : notations.descendingKeySet()) {
	    List<T> lst = notations.get(d);
	    Collections.shuffle(lst, random);
	    for (T t : lst) {
		if (sim.size() >= nbNodes) {
		    break b;
		}
		sim.put(t, d);
	    }
	}

	return sim;
    }

    public static <T> Map<T, Double> getProbaNodes(TreeMap<Double, LinkedList<T>> notations, int nbNodes, Random random) {
	Map<T, Double> map = new HashMap<T, Double>();
	double total = 0.0;
	for (double note : notations.keySet()) {
	    for (T i : notations.get(note)) {
		map.put(i, note);
		total += note;
	    }
	}
	if (map.size() <= nbNodes) {
	    return map;
	}

	Map<T, Double> sim = new HashMap<T, Double>();

	while (sim.size() < nbNodes) {
	    double target = random.nextDouble() * total;
	    double current = 0.0;
	    T curNode;
	    Iterator<T> iter = map.keySet().iterator();
	    do {
		curNode = iter.next();
		current += map.get(curNode);
	    } while (current < (target - 0.00001));
	    total -= map.get(curNode);
	    sim.put(curNode, map.get(curNode));
	    iter.remove();
	}

	return sim;
    }

    // Reflexion

    @SuppressWarnings("unchecked")
    public static <T> T getField(Object target, String field) {
	if (target == null) {
	    throw new RuntimeException("Trying to get a field on a null object : " + field);
	}
	try {
	    Class<?> classe = target.getClass();
	    while (!classe.equals(Object.class)) {
		Field[] fields = classe.getDeclaredFields();
		for (Field f : fields) {
		    if (f.getName().equals(field)) {
			if (!f.isAccessible()) {
			    f.setAccessible(true);
			}
			return (T) f.get(target);
		    }
		}
		classe = classe.getSuperclass();
	    }
	} catch (Exception e) {
	    throw new RuntimeException(e.getMessage());
	}
	throw new RuntimeException("Could not retrieve the field : " + field + " in " + target.getClass());
    }

    public static void setField(Object target, String field, Object value) {
	if (target == null) {
	    System.err.println("Trying to get a field on a null object : " + field);
	    return;
	}
	try {
	    Class<?> classe = target.getClass();
	    while (!classe.equals(Object.class)) {
		// System.out.println("Searching " + field + " in " +
		// classe.getName());
		Field[] fields = classe.getDeclaredFields();
		for (Field f : fields) {
		    if (f.getName().equals(field)) {
			f.setAccessible(true);
			// System.out.println("Field found");
			f.set(target, value);
		    }
		}
		// System.out.println("Not found in " + classe.getName());
		classe = classe.getSuperclass();
	    }
	    // System.out.println("Field Not found in " + target.getClass());
	} catch (Exception e) {
	    System.out.println("Could not set the field : " + target.getClass());
	    e.printStackTrace();
	}
    }

    public static List<Double> liftDoubleList(List<Double> toLift, int lift) {
	List<Double> lst = new ArrayList<Double>();
	for (int i = 0; i < toLift.size(); i++) {
	    double nb = 0, sum = 0;
	    for (int j = i - lift; j <= i + lift; j++) {
		if (j >= 0 && j < toLift.size()) {
		    sum += toLift.get(j);
		    nb++;
		}
	    }
	    double value = sum / nb;
	    lst.add(value);
	}
	return lst;
    }
}
