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
import org.mtransit.commons.StringUtils;
import org.mtransit.commons.TorontoTTCCommons;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MTrip;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

// https://open.toronto.ca/dataset/ttc-routes-and-schedules/ # ALL (including SUBWAY)
// https://open.toronto.ca/dataset/surface-routes-and-schedules-for-bustime/ BUS & STREETCAR
// http://opendata.toronto.ca/toronto.transit.commission/ttc-routes-and-schedules/SurfaceGTFS.zip
// OLD: http://opendata.toronto.ca/TTC/routes/OpenData_TTC_Schedules.zip
// OLD: http://opendata.toronto.ca/toronto.transit.commission/ttc-routes-and-schedules/OpenData_TTC_Schedules.zip
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

	private static final Pattern NOT_IN_SERVICE_ = Pattern.compile("(Not In Service)", Pattern.CASE_INSENSITIVE);

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (NOT_IN_SERVICE_.matcher(gTrip.getTripHeadsignOrDefault()).matches()) {
			return EXCLUDE;
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public boolean excludeStopTime(@NotNull GStopTime gStopTime) {
		if (NOT_IN_SERVICE_.matcher(gStopTime.getStopHeadsignOrDefault()).matches()) {
			return EXCLUDE;
		}
		return super.excludeStopTime(gStopTime);
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
	public @NotNull String getAgencyColor() {
		return TorontoTTCCommons.TTC_RED;
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
	public boolean directionSplitterEnabled(long routeId) {
		if (routeId == 101L) {
			return true; // 2024-09-05: NORTH/SOUTH have same direction ID (1)
		}
		return super.directionSplitterEnabled(routeId);
	}

	@Override
	public boolean directionOverrideId(long routeId) {
		if (routeId == 101L) {
			return true; // 2024-09-05: NORTH/SOUTH have same direction ID (1)
		}
		return super.directionOverrideId(routeId);
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final String L_ = "L ";

	private static final Pattern DIRECTION_ONLY = Pattern.compile("(^(east|west|north|south)$)", Pattern.CASE_INSENSITIVE);

	@Nullable
	@Override
	public String selectDirectionHeadSign(@Nullable String headSign1, @Nullable String headSign2) {
		if (StringUtils.equals(headSign1, headSign2)) {
			return null; // canNOT select
		}
		final boolean startsWith1 = headSign1 != null && headSign1.startsWith(L_);
		final boolean startsWith2 = headSign2 != null && headSign2.startsWith(L_);
		if (startsWith1) {
			if (!startsWith2) {
				return headSign2;
			}
		} else {
			if (startsWith2) {
				return headSign1;
			}
		}
		final boolean match1 = headSign1 != null && DIRECTION_ONLY.matcher(headSign1).find();
		final boolean match2 = headSign2 != null && DIRECTION_ONLY.matcher(headSign2).find();
		if (match1) {
			if (!match2) {
				return headSign1;
			}
		} else if (match2) {
			return headSign2;
		}
		return null;
	}

	private static final Pattern STARTS_WITH_DASH_ = Pattern.compile("((?<=[A-Z]{4,5})\\s*- .*$)", Pattern.CASE_INSENSITIVE);

	private static final Cleaner STARTS_WITH_RSN_ = new Cleaner(group(
			BEGINNING + group(maybe(oneOrMore(ALPHA_NUM_CAR))) +
					maybe(WHITESPACE_CAR) + "-" + maybe(WHITESPACE_CAR) +
					group(oneOrMore(DIGIT_CAR)) +
					maybe(WHITESPACE_CAR) + any(ANY) + END
	), EMPTY, true);

	private static final String STARTS_WITH_RSN_KEEP_RSN = mGroup(3);

	@SuppressWarnings("DuplicateBranchesInSwitch")
	@NotNull
	@Override
	public String cleanDirectionHeadsign(int directionId, boolean fromStopName, @NotNull String directionHeadSign) {
		final String rsn = STARTS_WITH_RSN_.clean(directionHeadSign, STARTS_WITH_RSN_KEEP_RSN);
		directionHeadSign = STARTS_WITH_DASH_.matcher(directionHeadSign).replaceAll(EMPTY); // keep East/West/North/South
		// east/west north/south and direction ID 1/0 are NOT a match
		final String dirLC = directionHeadSign.toLowerCase(getFirstLanguageNN());
		switch (rsn) {
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
		case "900":
			if (directionId == 1 && dirLC.equalsIgnoreCase("South")) {
				return "North"; // 2024-12-31: SOUTH for both directions
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

	private static final Pattern KEEP_LETTER_AND_TOWARDS_ = Pattern.compile("(^" +
			"(([a-z]+) - )?" + // EAST/WEST/NORTH/SOUTH -
			"(\\d+(/\\d+)?)?" + // 000(/000?)
			"([a-z] )?" + // A (from 000A) <- KEEP 'A'
			"((.*)" + // before to/towards
			"\\s*(towards|to))? " +
			"(.*)" + // after to/towards <- KEEP
			")", Pattern.CASE_INSENSITIVE);
	private static final String KEEP_LETTER_AND_TOWARDS_REPLACEMENT = "$6$10";

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

	private static Pattern makeRSN_RLN_(
			@SuppressWarnings("unused") @NotNull String rsn, // any RSN?
			@NotNull String rln) {
		return Pattern.compile(
				"(" +
						"(\\d+(/\\d+)?)" + // 000(/000?)
						"([a-z] )?" + // A (from 000A)
						"(\\s*(" + rln + ")\\s*)?" +
						")",
				Pattern.CASE_INSENSITIVE);
	}

	private static final String RSN_RLN_REPLACEMENT = "$4";

	@Override
	public @NotNull String cleanStopHeadSign(@NotNull GRoute gRoute, @NotNull GTrip gTrip, @NotNull GStopTime gStopTime, @NotNull String stopHeadsign) {
		stopHeadsign = makeRSN_RLN_(gRoute.getRouteShortName(), gRoute.getRouteLongNameOrDefault())
				.matcher(stopHeadsign).replaceAll(RSN_RLN_REPLACEMENT);
		return super.cleanStopHeadSign(gRoute, gTrip, gStopTime, stopHeadsign);
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
