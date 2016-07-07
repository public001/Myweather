/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.gweather.utils;

import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import com.gweather.app.R;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

public final class Utils {
	public static final String KEY_PUBLIC = "'093e7b8a05455a0fe9c97ee09f38f5a8'";
	
	public static final int TEMPERATURE_CELSIUS = 0;
	public static final int TEMPERATURE_FAHRENHEIT = 1;
	private static final String WEATHER_PROVIDER_PACKAGE_ID = "com.mediatek.weather";

	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager connectivity = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity != null) {
			NetworkInfo info = connectivity.getActiveNetworkInfo();
			if (info != null) {
				if (info.getState() == NetworkInfo.State.CONNECTED) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isNetworkTypeWifi(Context context) {
		ConnectivityManager connectivity = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity != null) {
			NetworkInfo info = connectivity.getActiveNetworkInfo();
			if (info != null && ConnectivityManager.TYPE_WIFI == info.getType()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * compare day1 to day2
	 * 
	 * @param day1
	 *            one day
	 * @param day2
	 *            other day
	 * @return true : equal ,otherwise false
	 */
	public static boolean isSameDay(long day1, long day2) {
		Calendar c1 = Calendar.getInstance();
		Calendar c2 = Calendar.getInstance();
		c1.setTimeInMillis(day1);
		c2.setTimeInMillis(day2);
		return c1.get(YEAR) == c2.get(YEAR) && c1.get(MONTH) == c2.get(MONTH)
				&& c1.get(DAY_OF_MONTH) == c2.get(DAY_OF_MONTH);
	}

	public static long getDayStart(long day) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(day);
		GregorianCalendar greCalendar = new GregorianCalendar(
				calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
				calendar.get(Calendar.DAY_OF_MONTH));
		long timeDayMillis = greCalendar.getTimeInMillis();
		return timeDayMillis;
	}

	/**
	 * get weather description for resId
	 * 
	 * @param desId
	 * @param context
	 * @return
	 * @throws NameNotFoundException
	 */
	public static String getWeatherDescription(int desId, Context context)
			throws NameNotFoundException {
		Resources resources = context.getPackageManager()
				.getResourcesForApplication(WEATHER_PROVIDER_PACKAGE_ID);
		return resources.getString(desId);
	}

	public static void setClockSize(Context context, RemoteViews clock,
			float scale) {
		float fontSize = context.getResources().getDimension(
				R.dimen.widget_big_font_size);
		clock.setTextViewTextSize(R.id.the_clock, TypedValue.COMPLEX_UNIT_PX,
				fontSize * scale);
	}

	public static void setTimeFormat(RemoteViews clock, int amPmFontSize,
			int clockId) {
		if (clock != null) {
			// Set the best format for 12 hours mode according to the locale
			clock.setCharSequence(clockId, "setFormat12Hour",
					Utils.get12ModeFormat(amPmFontSize));
			// Set the best format for 24 hours mode according to the locale
			clock.setCharSequence(clockId, "setFormat24Hour",
					Utils.get24ModeFormat());
		}
	}

	/***
	 * @param amPmFontSize
	 *            - size of am/pm label (label removed is size is 0).
	 * @return format string for 12 hours mode time
	 */
	public static CharSequence get12ModeFormat(int amPmFontSize) {
		String skeleton = "hma";
		String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(),
				skeleton);
		// Remove the am/pm
		if (amPmFontSize <= 0) {
			pattern.replaceAll("a", "").trim();
		}
		// Replace spaces with "Hair Space"
		pattern = pattern.replaceAll(" ", "\u200A");
		// Build a spannable so that the am/pm will be formatted
		int amPmPos = pattern.indexOf('a');
		if (amPmPos == -1) {
			return pattern;
		}
		Spannable sp = new SpannableString(pattern);
		sp.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), amPmPos,
				amPmPos + 1, Spannable.SPAN_POINT_MARK);
		sp.setSpan(new AbsoluteSizeSpan(amPmFontSize), amPmPos, amPmPos + 1,
				Spannable.SPAN_POINT_MARK);
		sp.setSpan(new TypefaceSpan("sans-serif-condensed"), amPmPos,
				amPmPos + 1, Spannable.SPAN_POINT_MARK);
		return sp;
	}

	public static CharSequence get24ModeFormat() {
		String skeleton = "Hm";
		return DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
	}

	public static boolean isAPGSOpen(Context context) {
		LocationManager locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		return locationManager
				.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
	}
	
}
