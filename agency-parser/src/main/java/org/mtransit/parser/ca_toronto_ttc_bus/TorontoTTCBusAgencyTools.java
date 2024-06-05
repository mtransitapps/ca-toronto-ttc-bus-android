package org.mtransit.parser.ca_toronto_ttc_bus;

import static org.mtransit.commons.RegexUtils.ALPHA_NUM_CAR;
import static org.mtransit.commons.RegexUtils.ANY;
import static org.mtransit.commons.RegexUtils.BEGINNING;
import static org.mtransit.commons.RegexUtils.DIGIT_CAR;
import static org.mtransit.commons.RegexUtils.END;
import static org.mtransit.commons.RegexUtils.WHITESPACE_CAR;
import static org.mtransit.commons.RegexUtils.any;
import static org.mtransit.commons.RegexUtils.group;
import static org.mtransit.commons.RegexUtils.mGroup;
import static org.mtransit.commons.RegexUtils.maybe;
import static org.mtransit.commons.RegexUtils.oneOrMore;
import static org.mtransit.commons.StringUtils.EMPTY;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.Cleaner;
import org.mtransit.commons.TorontoTTCCommons;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MTrip;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

// https://open.toronto.ca/dataset/ttc-routes-and-schedules/
// OLD: https://opendata.toronto.ca/TTC/routes/OpenData_TTC_Schedules.zip
public class TorontoTTCBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new TorontoTTCBusAgencyTools().start(args);
	}

	@Nullable
	@Override
	public List<Locale> getSupportedLanguages() {
		return LANG_EN;
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String getAgencyName() {
		return "TTC";
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		final String tripHeadsign = gTrip.getTripHeadsignOrDefault().toLowerCase(getFirstLanguageNN());
		if ("not in service".equals(tripHeadsign)) {
			return true; // exclude
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public boolean defaultRouteIdEnabled() {
		return true;
	}

	@Override
	public boolean useRouteShortNameForRouteId() {
		return true;
	}

	@Override
	public boolean defaultRouteLongNameEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String routeLongName) {
		routeLongName = CleanUtils.toLowerCaseUpperCaseWords(getFirstLanguageNN(), routeLongName);
		routeLongName = CleanUtils.fixMcXCase(routeLongName);
		return CleanUtils.cleanLabel(routeLongName);
	}

	@Override
	public boolean defaultAgencyColorEnabled() {
		return true;
	}

	@Nullable
	@Override
	public String fixColor(@Nullable String color) {
		final String fixedColor = TorontoTTCCommons.fixColor(color);
		if (fixedColor != null) {
			return fixedColor;
		}
		return super.fixColor(color);
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Cleaner STARTS_WITH_DASH_ = new Cleaner(group(
			BEGINNING + group(maybe(oneOrMore(ALPHA_NUM_CAR))) +
					maybe(WHITESPACE_CAR) + "-" + maybe(WHITESPACE_CAR) +
					group(oneOrMore(DIGIT_CAR)) +
					maybe(WHITESPACE_CAR) + any(ANY) + END
	), EMPTY, true);

	private static final String STARTS_WITH_DASH_KEEP_DIRECTION = mGroup(2);

	private static final String STARTS_WITH_DASH_KEEP_RSN = mGroup(3);

	@SuppressWarnings("DuplicateBranchesInSwitch")
	@NotNull
	@Override
	public String cleanDirectionHeadsign(int directionId, boolean fromStopName, @NotNull String directionHeadSign) {
		// east/west north/south and direction ID 1/0 are NOT a match
		final String rsn = STARTS_WITH_DASH_.clean(directionHeadSign, STARTS_WITH_DASH_KEEP_RSN);
		directionHeadSign = STARTS_WITH_DASH_.clean(directionHeadSign, STARTS_WITH_DASH_KEEP_DIRECTION); // keep East/West/North/South
		final String dirLC = directionHeadSign.toLowerCase(getFirstLanguageNN());
		switch (rsn) {
		case "52":
			if (directionId == 1 && dirLC.equalsIgnoreCase("East")) {
				return "West";
			}
			break;
		case "84":
			if (directionId == 1 && dirLC.equalsIgnoreCase("East")) {
				return "West";
			} else if (directionId == 0 && dirLC.equalsIgnoreCase("West")) {
				return "East";
			}
			break;
		case "332":
			if (directionId == 1 && dirLC.equalsIgnoreCase("East")) {
				return "West";
			}
		case "352":
			if (directionId == 1 && dirLC.equalsIgnoreCase("East")) {
				return "West";
			}
			break;
		case "952":
			if (directionId == 1 && dirLC.equalsIgnoreCase("East")) {
				return "West";
			}
			break;
		}
		directionHeadSign = CleanUtils.toLowerCaseUpperCaseWords(getFirstLanguageNN(), directionHeadSign);
		return CleanUtils.cleanLabel(directionHeadSign);
	}

	@NotNull
	@Override
	public List<Integer> getDirectionTypes() {
		return Collections.singletonList(
				MTrip.HEADSIGN_TYPE_DIRECTION
		);
	}

	private static final Pattern KEEP_LETTER_AND_TOWARDS_ = Pattern.compile("(^([a-z]+) - (\\d+)([a-z]?)( (.*) towards)? (.*))", Pattern.CASE_INSENSITIVE);
	private static final String KEEP_LETTER_AND_TOWARDS_REPLACEMENT = "$4 $7";

	private static final Pattern ENDS_EXTRA_FARE_REQUIRED = Pattern.compile("(( -)? extra fare required .*$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern SHORT_TURN_ = CleanUtils.cleanWords("short turn");

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = KEEP_LETTER_AND_TOWARDS_.matcher(tripHeadsign).replaceAll(KEEP_LETTER_AND_TOWARDS_REPLACEMENT);
		tripHeadsign = ENDS_EXTRA_FARE_REQUIRED.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = SHORT_TURN_.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = CleanUtils.removeVia(tripHeadsign);
		tripHeadsign = CleanUtils.toLowerCaseUpperCaseWords(getFirstLanguageNN(), tripHeadsign);
		tripHeadsign = CleanUtils.CLEAN_AT.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern SIDE = Pattern.compile("((^|\\W)(" + "side" + ")(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String SIDE_REPLACEMENT = "$2" + "$4";

	private static final Pattern HS = Pattern.compile("(H\\.S\\.)", Pattern.CASE_INSENSITIVE);
	private static final String HS_REPLACEMENT = "HS";

	private static final Pattern SS = Pattern.compile("(S\\.S\\.)", Pattern.CASE_INSENSITIVE);
	private static final String SS_REPLACEMENT = "SS";

	private static final Pattern CNR = Pattern.compile("(C\\.N\\.R\\.)", Pattern.CASE_INSENSITIVE);
	private static final String CNR_REPLACEMENT = "CNR";

	private static final Pattern CN = Pattern.compile("(C\\.\\s*N\\.)", Pattern.CASE_INSENSITIVE);
	private static final String CN_REPLACEMENT = "CN";

	private static final Pattern CI = Pattern.compile("(C\\.I\\.)", Pattern.CASE_INSENSITIVE);
	private static final String CI_REPLACEMENT = "CI";

	private static final Pattern II = Pattern.compile("(II)", Pattern.CASE_INSENSITIVE);
	private static final String II_REPLACEMENT = "II";

	private static final Pattern GO = Pattern.compile("((^|\\W)(" + "GO" + ")(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String GO_REPLACEMENT = "$2" + "GO" + "$4";

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = CleanUtils.toLowerCaseUpperCaseWords(getFirstLanguageNN(), gStopName);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = SIDE.matcher(gStopName).replaceAll(SIDE_REPLACEMENT);
		gStopName = CleanUtils.cleanBounds(gStopName);
		gStopName = HS.matcher(gStopName).replaceAll(HS_REPLACEMENT);
		gStopName = SS.matcher(gStopName).replaceAll(SS_REPLACEMENT);
		gStopName = CNR.matcher(gStopName).replaceAll(CNR_REPLACEMENT);
		gStopName = CN.matcher(gStopName).replaceAll(CN_REPLACEMENT);
		gStopName = CI.matcher(gStopName).replaceAll(CI_REPLACEMENT);
		gStopName = II.matcher(gStopName).replaceAll(II_REPLACEMENT);
		gStopName = GO.matcher(gStopName).replaceAll(GO_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		return super.getStopCode(gStop); // stop code used as stop tag by real-time API
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		return super.getStopId(gStop); // stop ID used as stop code by real-time API
	}
}
