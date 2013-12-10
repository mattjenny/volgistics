import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * 
 * @author mjenny
 * 
 * IMPORTANT:  MAKE SURE that the input CSV has columns in this exact order:
 * 
 * [Last Name], [First Name], [Driver info], [Gender], [5 availability columns, M-F], [School Preference], [Small Groups], [Notes]
 *
 */

public class CSVParser {
	
	private static final int LAST_NAME_INDEX = 0;
	private static final int FIRST_NAME_INDEX = 1;
	private static final int DRIVER_INFO_INDEX = 2;
	private static final int GENDER_INDEX = 3;
	private static final int MONDAY_AVAILABILITY_INDEX = 4;
	private static final int TUESDAY_AVAILABILITY_INDEX = 5;
	private static final int WEDNESDAY_AVAILABILITY_INDEX = 6;
	private static final int THURSDAY_AVAILABILITY_INDEX = 7;
	private static final int FRIDAY_AVAILABILITY_INDEX = 8;
	private static final int SCHOOL_PREFERENCE_INDEX = 9;
	private static final int SMALL_GROUP_INDEX = 10;
	private static final int NOTES_INDEX = 11;
	
	private static final int VOLUNTEER_ID_INDEX = 0;
	private static final int VOLUNTEER_LAST_NAME_INDEX = 1;
	private static final int VOLUNTEER_FIRST_NAME_INDEX = 2;
	private static final int VOLUNTEER_SEATS_INDEX = 3;
	private static final int VOLUNTEER_GENDER_INDEX = 4;
	private static final int VOLUNTEER_ELEMENTARY_SCHOOL_INDEX = 5;
	private static final int VOLUNTEER_MIDDLE_SCHOOL_INDEX = 6;
	private static final int VOLUNTEER_HIGH_SCHOOL_INDEX = 7;
	private static final int VOLUNTEER_SMALL_GROUP_INDEX = 8;
	private static final int VOLUNTEER_NOTES_INDEX = 9;
	private static final String[] VOLUNTEER_DATA_HEADER = {"ID","Last Name","First Name","Seats","Gender","Elementary","Middle","High","Small Group","Notes"};
	
	private static final int COVOLUNTEER_ID_INDEX = 0;
	private static final int COVOLUNTEER_DATA_INDEX = 1;
	private static final String[] COVOLUNTEER_HEADER = {"ID","Data"};
	
	private static final int AVAILABILITY_ID_INDEX = 0;
	private static final int AVAILABILITY_START_DATE_INDEX = 1;
	private static final int AVAILABILITY_END_DATE_INDEX = 2;
	private static final String[] AVAILABILITY_HEADER = {"ID","Begin time","End time"};
	
	int currentId = 0;
	private CSVReader reader = null;
	private List<CSVEntry> entries = null;

	public CSVParser(String filename) {
		try {
			reader = new CSVReader(new FileReader(filename), ',');
			System.out.println("File read.");
			reader.readNext(); //Get rid of the first row
			parseCSV();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void parseCSV() {
		
		ArrayList<CSVEntry> entries = new ArrayList<CSVEntry>();
		String[] nextLine = {""};
		String[] currentFields = {""};
		boolean isFirst = true;
		try {
			while((nextLine = reader.readNext()) != null) {
				if (!nextLine[0].equals("")) {
					if (!isFirst) {
						entries.add(new CSVEntry(currentFields));
					}
					else isFirst = false;
					currentFields = nextLine;
				} else {
					for (int i=0; i<nextLine.length; i++) {
						if (nextLine[i].equals("")) continue;
						currentFields[i] += " " + nextLine[i];
					}
				}
			}
			entries.add(new CSVEntry(currentFields));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.entries = entries;
	}
	
	public List<CSVEntry> getEntries() {
		return entries;
	}
	
	public void writeBetterCSV(String filename) {
		try {
			CSVWriter writer = new CSVWriter(new FileWriter(filename+"-data.csv"), ',');
			writer.writeNext(VOLUNTEER_DATA_HEADER);
			for (CSVEntry entry: entries) {
				writer.writeNext(entry.getVolunteerData());
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			CSVWriter writer = new CSVWriter(new FileWriter(filename+"-covs.csv"), ',');
			writer.writeNext(COVOLUNTEER_HEADER);
			for (CSVEntry entry: entries) {
				writer.writeNext(entry.getCoVolunteerData());
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			CSVWriter writer = new CSVWriter(new FileWriter(filename+"-availability.csv"), ',');
			writer.writeNext(AVAILABILITY_HEADER);
			for (CSVEntry entry: entries) {
				for (String[] row: entry.getAvailability()) {
					writer.writeNext(row);
				}
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public class CSVEntry {
		
		private final String[] DAY_CONSTANTS = {"Monday","Tuesday","Wednesday","Thursday","Friday"};
		
		private DateFormat readFormat = new SimpleDateFormat("hhaE");
		private DateFormat prettyFormat = new SimpleDateFormat("E h:mma");

		String[] volunteerData = new String[10];
		String[] coVolunteerData = new String[2];
		List<String[]> availability = new ArrayList<String[]>();
		boolean hasCoVolunteers = false;
		
		public CSVEntry(String[] fields) {
			
			volunteerData[VOLUNTEER_ID_INDEX] = currentId+"";
			volunteerData[VOLUNTEER_LAST_NAME_INDEX] = fields[LAST_NAME_INDEX];
			volunteerData[VOLUNTEER_FIRST_NAME_INDEX] = fields[FIRST_NAME_INDEX];
			volunteerData[VOLUNTEER_SEATS_INDEX] = calculateSeats(fields[DRIVER_INFO_INDEX]);
			volunteerData[VOLUNTEER_GENDER_INDEX] = fields[GENDER_INDEX];
			volunteerData[VOLUNTEER_ELEMENTARY_SCHOOL_INDEX] = hasElementary(fields[SCHOOL_PREFERENCE_INDEX]);
			volunteerData[VOLUNTEER_MIDDLE_SCHOOL_INDEX] = hasMiddle(fields[SCHOOL_PREFERENCE_INDEX]);
			volunteerData[VOLUNTEER_HIGH_SCHOOL_INDEX] = hasHigh(fields[SCHOOL_PREFERENCE_INDEX]);
			volunteerData[VOLUNTEER_SMALL_GROUP_INDEX] = getSmallGroup(fields[SMALL_GROUP_INDEX]);
			volunteerData[VOLUNTEER_NOTES_INDEX] = fields[NOTES_INDEX];
			
			coVolunteerData[COVOLUNTEER_ID_INDEX] = currentId+"";
			coVolunteerData[COVOLUNTEER_DATA_INDEX] = getCoVolunteers(fields[NOTES_INDEX]);
			
			populateAvailability(fields);
			
			currentId++;
		}
		
		private void populateAvailability(String[] fields) {
			
			String[] daily_availability = {fields[MONDAY_AVAILABILITY_INDEX],
					fields[TUESDAY_AVAILABILITY_INDEX],
					fields[WEDNESDAY_AVAILABILITY_INDEX],
					fields[THURSDAY_AVAILABILITY_INDEX],
					fields[FRIDAY_AVAILABILITY_INDEX]};
			
			for (int i=0; i<daily_availability.length; i++) {
				
				if (daily_availability[i].equals("")) continue;
				
				String currentBegin = "";
				String currentEnd="";
				
				for (String s: daily_availability[i].split(" ")) {
					
					String begin = s.substring(0, s.indexOf("-"));
					String end = s.substring(s.indexOf("-")+1);
					
					if (begin.equals(currentEnd)) {
						currentEnd = end;
					} else {
						if (!currentBegin.equals("")) {
							addAvailability(currentBegin+DAY_CONSTANTS[i], currentEnd+DAY_CONSTANTS[i]);
						}
						currentBegin = begin;
						currentEnd = end;
					}
				}
				addAvailability(currentBegin+DAY_CONSTANTS[i], currentEnd+DAY_CONSTANTS[i]);
			}
			
		}
		
		private void addAvailability(String begin, String end) {
	
			try {
				Date beginDate = readFormat.parse(begin);
				Date endDate = readFormat.parse(end);
				begin = prettyFormat.format(beginDate);
				end = prettyFormat.format(endDate);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
			String[] availabilityString = new String[3];
			availabilityString[AVAILABILITY_ID_INDEX] = currentId+"";
			availabilityString[AVAILABILITY_START_DATE_INDEX] = begin;
			availabilityString[AVAILABILITY_END_DATE_INDEX] = end;
			
			availability.add(availabilityString);
		}
		
		private String calculateSeats(String driverInfo) {
			if (driverInfo.contains("Yes")) {
				return driverInfo.charAt(5) + "";
			} else {
				return "0";
			}
		}
		
		private String hasElementary(String schoolPrefs) {
			return schoolPrefs.contains("Elementary") + "";
		}
		
		private String hasMiddle(String schoolPrefs) {
			return schoolPrefs.contains("Middle") + "";
		}
		
		private String hasHigh(String schoolPrefs) {
			return schoolPrefs.contains("High") + "";
		}
		
		public boolean hasCoVolunteers() {
			return hasCoVolunteers;
		}
		
		public String getSmallGroup(String smallGroup) {
			if (smallGroup.contains("Yes")) {
				return true+"";
			} else {
				return false+"";
			}
		}
		
		private String getCoVolunteers(String notes) {
			int coVolunteerIndex = notes.indexOf("Co-Volunteers");
			if (coVolunteerIndex>=0) {
				hasCoVolunteers = true;
				
				String covs = notes.substring(coVolunteerIndex + 13);
				
				int availabilityIndex = covs.indexOf("Availability");
				int experienceIndex = covs.indexOf("Experience");
				int interestIndex = covs.indexOf("Interest");
				int breakIndex = -1;
				
				if (availabilityIndex >= 0) breakIndex = availabilityIndex;
				if (experienceIndex >= 0) breakIndex = breakIndex>=0 ? Math.min(breakIndex,  experienceIndex) : experienceIndex;
				if (interestIndex >= 0) breakIndex = breakIndex>=0 ? Math.min(breakIndex,  interestIndex) : interestIndex;
				if (breakIndex >= 0) {
					covs = covs.substring(0, breakIndex);
				}
				return covs;
			} else {
				return "";
			}
		}
		
		public String[] getVolunteerData() {
			return volunteerData;
		}
		
		public String[] getCoVolunteerData() {
			return coVolunteerData;
		}
		
		public List<String[]> getAvailability() {
			return availability;
		}
	}
	
	public class CSVField {
		
		private String name;
		private String value;
		
		public CSVField(String name, String value) {
			this.name = name;
			this.value = value;
		}
		
		public String getName() {
			return name;
		}
		
		public String getValue() {
			return value;
		}
	}
	
	public static void main(String[] args) {
		
		CSVParser parser = new CSVParser("/Users/mjenny/Documents/test-volunteers3.csv");
		List<CSVEntry> entries = parser.getEntries();
		for (int i=0; i<10; i++) {
			System.out.println("*****");
			CSVEntry entry = entries.get(i);
			System.out.print("Volunteer Data: ");
			for (String s: entry.getVolunteerData()) {
				System.out.print(s + " ");
			}
			if (entry.hasCoVolunteers()) {
				System.out.print("\nCo-Volunteer Data: ");
				for (String s: entry.getCoVolunteerData()) {
					System.out.print(s + " ");
				}
			}
			System.out.print("\nAvailability Data: ");
			for (String[] row: entry.getAvailability()) {
				System.out.print("\n  ");
				for (String s: row) {
					System.out.print(s + " ");
				}
			}
			System.out.print("\n");
		}
		parser.writeBetterCSV("/Users/mjenny/Documents/test-volunteers-better5");
	}
}
