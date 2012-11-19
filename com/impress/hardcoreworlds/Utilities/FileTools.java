package com.impress.hardcoreworlds.Utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

public class FileTools {
	public static boolean overwrite = true;
	public static boolean saveHashMap(HashMap<?, ?> hashMap, File file) {
		if (overwrite && file.isFile()) file.delete();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
			oos.writeObject(hashMap);
			oos.flush();
			oos.close();
			return true;
		} catch(Exception e) {
			return false;
		}
	}
	@SuppressWarnings("unchecked")
	public static <T, K> HashMap<T, K> loadHashMap(File file, HashMap<T, K> type) {
		if (!file.isFile()) return null;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
			Object result = ois.readObject();
			return (HashMap<T, K>)result;
		} catch(Exception e) {
			return null;
		}
	}
}