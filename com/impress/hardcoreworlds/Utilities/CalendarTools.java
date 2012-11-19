package com.impress.hardcoreworlds.Utilities;

import java.util.Calendar;

public class CalendarTools {
//	public static void main(String[] args) {
//		Calendar time1 = Calendar.getInstance();
//		Calendar time2 = Calendar.getInstance();
//		Calendar time3 = Calendar.getInstance();
//		Calendar time4 = Calendar.getInstance();
//		Calendar time5 = Calendar.getInstance();
//		time1.add(Calendar.SECOND, 38);
//		time2.add(Calendar.SECOND, 194);
//		time3.add(Calendar.SECOND, 721);
//		time4.add(Calendar.SECOND, 294728);
//		time5.add(Calendar.MINUTE, 9999999);
//		System.out.println(timeUntil(time1));
//		System.out.println(timeUntil(time2));
//		System.out.println(timeUntil(time3));
//		System.out.println(timeUntil(time4));
//		System.out.println(timeUntil(time5));
//	}
	public static String timeUntil(Calendar event) {
		int seconds;
		try {
			seconds = safeLongToInt(millisUntil(event) / 1000);
		} catch (IllegalArgumentException e) {
			return "too much time";
		}
		int minutes = seconds / 60;
		int hours = minutes / 60;
		int days = hours / 24;
		seconds = seconds % 60;
		minutes = minutes % 60;
		hours = hours % 24;
		return ((days > 0)? days + " days, " : "") + ((hours > 0)? hours + " hours, " : "")
				+ ((minutes > 0)? minutes + " minutes, " : "") + seconds + " seconds";
	}
	public static Calendar cloneAdd(Calendar original, int field, int amount) {
		Calendar result = (Calendar)original.clone();
		result.add(field, amount);
		return result;
	}
	private static long millisUntil(Calendar event) {
		return event.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
	}
	private static int safeLongToInt(long l) {
	    int i = (int)l;
	    if ((long)i != l) {
	        throw new IllegalArgumentException(l + " cannot be cast to int without changing its value.");
	    }
	    return i;
	}
}