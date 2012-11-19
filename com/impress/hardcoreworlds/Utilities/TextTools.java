package com.impress.hardcoreworlds.Utilities;

public class TextTools {
	public static String listCommas(String[] list) {
		if (list == null) return null;
		StringBuilder result = new StringBuilder();
		for (int i  = 0; i < list.length; i++) {
			if (i > 0) result.append(", ");
			result.append(list[i]);
		}
		return result.toString();
	}
}