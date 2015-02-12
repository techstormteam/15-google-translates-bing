package com.techstorm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	private static List<Integer> columnIndexes;
	
	public static void main(String[] args) throws Exception {
		Config config = readConfiguration();
		
		Map<String,List<List<String>>> outputs = new HashMap<String,List<List<String>>>(); 
		
		List<Account> googleAccounts = new ArrayList<Account>();
		List<Account> bingAccounts = new ArrayList<Account>();
		
		// read csv file for each rows
		CSV csv = CSV.separator(',') // delimiter of fields
			.noQuote() 
			.create(); // new instance is immutable
		
		csv.read(config.accountsCsv, new CSVReadProc() {
			public void procRow(int rowIndex, String... values) {
				if (rowIndex == 0) {
					return;
				}
				
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
		if (googleAccounts != null && !googleAccounts.isEmpty()) {
			translator = OnlineGoogleTranslator.createInstance(googleAccounts.get(0).apiKey);
		}
		if (bingAccounts != null && !bingAccounts.isEmpty()) {
			setAuthForBingTranslates(bingAccounts.get(0).clientId, bingAccounts.get(0).secret);
		}
				
		columnIndexes = null;
		try {
			columnIndexes = new ArrayList<Integer>();
			String[] columnArray = config.columns.split(",");
			for (String column : columnArray) {
				columnIndexes.add(Integer.parseInt(column));
			}
		} catch (NumberFormatException e) {
			// nothing
		}
		
		
		
		if (columnIndexes == null) {
			System.exit(1);
		}
		
		for (String toLanguage : config.toLanguages) {
			List<List<String>> csvContent = new ArrayList<List<String>>();
			outputs.put(toLanguage, csvContent);
		}
		
		
		
		csv.read(config.inputCsv, new CSVReadProc() {
			public void procRow(int rowIndex, String... values) {
				for (String toLanguage : config.toLanguages) {
					
					List<List<String>> csvContent = outputs.get(toLanguage);
					List<String> rowContent = new ArrayList<String>();
					for (int index = 0; index < values.length; index++) {
						String value = values[index];
						for (Integer columnIndex : columnIndexes) {
							if (columnIndex - 1 == index) { 
								
									try {
										if (config.useGoogle) {
											value = googleTranslates(value, config.fromLanguage, toLanguage);
										} else if (config.useBing) {
											value = bingTranslates(value, config.fromLanguage, toLanguage);
										}
									} catch (Exception e) {
										e.printStackTrace();
									}
								
							}
						}
						rowContent.add(value);
					}
					csvContent.add(rowContent);
				}
			}
		});
		
		for (String languageKey : outputs.keySet()) {
			
			csv.write(getOutputCsvLanguage(config.outputCsv, languageKey), new CSVWriteProc() {
				public void process(CSVWriter out) {
					List<List<String>> csvContent = outputs.get(languageKey);
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
		
	}
	
	private static String getOutputCsvLanguage(String outputOriginalFilename, String languageKey) {
		StringBuilder result = new StringBuilder();
		String[] parts = outputOriginalFilename.split("\\.");
		if (parts != null && parts.length > 0) {
			result.append(parts[0]);
		}
		result.append("_");
		result.append(languageKey);
		result.append(".csv");
		return result.toString();
	}
	
	private static Config readConfiguration() {
		Config result = new Config();
		
		Properties prop = new Properties();
		InputStream input = null;
	 
		try {
	 
			input = new FileInputStream("config.properties");
	 
			// load a properties file
			prop.load(input);
	 
			String google = prop.getProperty("google");
			if (google.equalsIgnoreCase("YES")) {
				result.useGoogle = true;
			}
			String bing = prop.getProperty("bing");
			if (bing.equalsIgnoreCase("YES")) {
				result.useBing = true;
			}
			
			String inputCsv = prop.getProperty("inputCsv");
			result.inputCsv = inputCsv;
			String outputCsv = prop.getProperty("outputCsv");
			result.outputCsv = outputCsv;
			String accountsCsv = prop.getProperty("accountsCsv");
			result.accountsCsv = accountsCsv;
			String columns = prop.getProperty("columns");
			result.columns = columns;
			
			
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
				+ " --columns <TRANSLATES COLUMN INDEXES. e.i: '1,3,4'>");
	}
	
	private static String googleTranslates(String text, String fromLang, String toLang) throws Exception {
		// general constants
        final String SOURCE_TEXT = text;
        final Language SOURCE_LANGUAGE = getGLanguageObject(fromLang);
        final Language TARGET_LANGUAGE = getGLanguageObject(toLang);

        Translation translation = translator.translate(SOURCE_TEXT, SOURCE_LANGUAGE, TARGET_LANGUAGE);
        return translation.getTranslatedText();
	}
	
	private static String bingTranslates(String text, String fromLang, String toLang) throws Exception {
	    String translatedText = Translate.execute(text, getBLanguageObject(fromLang), getBLanguageObject(toLang));
	    
	    return translatedText;
	}
	
	private static void setAuthForBingTranslates(String clientId, String secret) {
		Translate.setClientId(clientId);
	    Translate.setClientSecret(secret);
	}
	
}

