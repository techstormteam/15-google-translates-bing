package com.techstorm;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import sample.util.SimpleCommandLineParser;
import au.com.bytecode.opencsv.CSV;
import au.com.bytecode.opencsv.CSVReadProc;
import au.com.bytecode.opencsv.CSVWriteProc;
import au.com.bytecode.opencsv.CSVWriter;

import com.memetix.mst.translate.Translate;
import com.translator.google.core.Translation;
import com.translator.google.translator.OnlineGoogleTranslator;
import com.translator.google.translator.OnlineGoogleTranslator.Language;


public class TranslatorApp {
	private static OnlineGoogleTranslator translator;
	private static Translation translation;
	
	public static void main(String[] args) throws Exception {
		SimpleCommandLineParser parser = new SimpleCommandLineParser(args);
		
		boolean help = parser.containsKey("help", "h");
		String inputCsv = parser.getValue("inputcsv", "i");
		String columns = parser.getValue("columns", "c");
		String outputCsv = "output.csv";
		String accountsCsv = "accounts.csv";
		
		Config config = readConfiguration();
		
		if (help || inputCsv == null || columns == null) {
			printUsage();
			System.exit(1);
		}
		
		List<Integer> columnIndexes = null;
		try {
			columnIndexes = new ArrayList<Integer>();
			String[] columnArray = columns.split(",");
			for (String column : columnArray) {
				columnIndexes.add(Integer.parseInt(column));
			}
		} catch (NumberFormatException e) {
			// nothing
		}
		
		List<List<String>> csvContent = new ArrayList<List<String>>();
		List<Account> googleAccounts = new ArrayList<Account>();
		List<Account> bingAccounts = new ArrayList<Account>();
		
		if (columnIndexes == null) {
			printUsage();
			System.exit(1);
		}
		
		// read csv file for each rows
		CSV csv = CSV.separator(',') // delimiter of fields
			.noQuote() 
			.create(); // new instance is immutable
		
		csv.read(accountsCsv, new CSVReadProc() {
			public void procRow(int rowIndex, String... values) {
				Account account = new Account();
				account.username = values[0];
				account.password = values[1];
				account.goolgeAccount = values[2].equals("google") ? true : false;
				if (account.goolgeAccount) {
					account.apiKey = values[3];
					googleAccounts.add(account);
				} else {
					account.clientId = values[3];
					account.secret = values[4];
					bingAccounts.add(account);
				}
				
			}
		});
		
		csv.read(inputCsv, new CSVReadProc() {
			public void procRow(int rowIndex, String... values) {
				List<String> rowContent = new ArrayList<String>();
				for (String value : values) {
					rowContent.add(value);
				}
				csvContent.add(rowContent);
			}
		});
		
		for (List<String> rowContent : csvContent) {
			for (Integer columnIndex : columnIndexes) {
				String value = rowContent.get(columnIndex);
				String translatedText = "";
				for (String toLanguage : config.toLanguages) {
					if (config.useGoogle) {
						translatedText = googleTranslates(value, config.fromLanguage, toLanguage);
					} else if (config.useBing) {
						translatedText = bingTranslates(value, config.fromLanguage, toLanguage);
					}
				}
				rowContent.set(columnIndex, translatedText);
			}
		}
		
		csv.write(outputCsv, new CSVWriteProc() {
			public void process(CSVWriter out) {
				for (List<String> rowContent : csvContent) {
					
					if (rowContent != null && !rowContent.isEmpty()) {
						StringBuilder rowString = new StringBuilder();
						for (String value : rowContent) {
							rowString.append(value);
							rowString.append(",");
						}
						rowString.deleteCharAt(rowString.length() - 1);
						out.writeNext(rowString.toString());
					}
					
				}
			}
		});
	}
	
	private static Config readConfiguration() {
		Config result = new Config();
		
		Properties prop = new Properties();
		InputStream input = null;
	 
		try {
	 
			input = new FileInputStream("../config.properties");
	 
			// load a properties file
			prop.load(input);
	 
			String google = prop.getProperty("google");
			if (google.equals("0")) {
				result.useGoogle = true;
			}
			String bing = prop.getProperty("bing");
			if (bing.equals("0")) {
				result.useBing = true;
			}
			String fromLang = prop.getProperty("fromLanguage");
			result.fromLanguage = fromLang;
			
			result.toLanguages = new ArrayList<String>();
			int toLanguageIndex = 1;
			String toLang = prop.getProperty("toLanguage"+toLanguageIndex);
			while (toLang != null) {
				result.toLanguages.add(toLang);
				toLanguageIndex++;
				toLang = prop.getProperty("toLanguage"+toLanguageIndex);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return result;
	}
	
	private static Language getGLanguageObject(String languageString) {
		Language result = Language.ENGLISH;
		for (Language lang : Language.values()) {
			if (languageString.equalsIgnoreCase(lang.name()) 
					|| languageString.equalsIgnoreCase(lang.value)) {
				result = lang;
			}
			
		}
		return result;
	}
	
	private static com.memetix.mst.language.Language getBLanguageObject(String languageString) {
		com.memetix.mst.language.Language result = com.memetix.mst.language.Language.ENGLISH;
		for (com.memetix.mst.language.Language lang : com.memetix.mst.language.Language.values()) {
			if (languageString.equalsIgnoreCase(lang.name())
					|| languageString.equalsIgnoreCase(lang.toString())) {
				result = lang;
			}
		}
		return result;
	}
	
	/**
	 * Shows the usage of how to run the sample from the command-line.
	 */
	private static void printUsage() {
		System.out.println("Usage: java -jar GoogleBingTranslates.jar"
				+ " --inputcsv <CSV PATH>"
				+ " --columns <TRANSLATES COLUMN INDEXES. e.i: '1,3,4'>"
				+ " --fromlang <FROM LANGUAGE. e.i: 'TURKISH' or 'turkish' or 'tr'>"
				+ " --tolang <TO LANGUAGE. e.i: 'ENGLISH' or 'english' or 'en'>"
				+ " --use <TRANSLATE SOURCE. e.i: 'google' or 'bing'>");
	}
	
	private static String googleTranslates(String text, String fromLang, String toLang) throws Exception {
		// general constants
        final String SOURCE_TEXT = text;
        final Language SOURCE_LANGUAGE = getGLanguageObject(fromLang);
        final Language TARGET_LANGUAGE = getGLanguageObject(toLang);

        // specific setting only for Google Translate API v2
        final String API_KEY = "AIzaSyDx4EOP-SO8KM6pCXGOi9D7lv3a4X4S-6g";

		translator = OnlineGoogleTranslator.createInstance(API_KEY);
        translation = translator.translate(SOURCE_TEXT, SOURCE_LANGUAGE, TARGET_LANGUAGE);

        return translation.getTranslatedText();
	}
	
	private static String bingTranslates(String text, String fromLang, String toLang) throws Exception {
		// Set your Windows Azure Marketplace client info - See http://msdn.microsoft.com/en-us/library/hh454950.aspx
	    Translate.setClientId("BingTranslatesAuto");
	    Translate.setClientSecret("2mloy/ylg8VapooWqoULMdxCHrzRHJ0wfRNbh55g/Sk=");

	    String translatedText = Translate.execute(text, getBLanguageObject(fromLang), getBLanguageObject(toLang));
	    return translatedText;
	}
	
	
}

