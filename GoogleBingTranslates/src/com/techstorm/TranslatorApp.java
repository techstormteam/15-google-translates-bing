package com.techstorm;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
	private static Map<String, String> translateCache = new HashMap<String, String>();

	private static String combinationKey(String text, String fromLanguage, String toLanguage) {
		return text + "@" + fromLanguage + "@" + toLanguage;
	}

	public static void main(String[] args) throws Exception {
		Config config = readConfiguration();

		Map<String, List<List<String>>> outputs = new HashMap<String, List<List<String>>>();

		List<Account> googleAccounts = new ArrayList<Account>();
		List<Account> bingAccounts = new ArrayList<Account>();

		// read csv file for each rows
		CSV csv = CSV.separator(',') // delimiter of fields
				.noQuote()
				.charset(StandardCharsets.UTF_8)
				.create(); // new instance is immutable

		csv.read(config.accountsCsv, new CSVReadProc() {
			public void procRow(int rowIndex, String... values) {
				if (rowIndex == 0) {
					return;
				}

				Account account = new Account();
				account.username = values[0];
				account.password = values[1];
				account.goolgeAccount = values[2].equals("google") ? true
						: false;
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
			translator = OnlineGoogleTranslator.createInstance(googleAccounts
					.get(0).apiKey);
		}
		if (bingAccounts != null && !bingAccounts.isEmpty()) {
			setAuthForBingTranslates(bingAccounts.get(0).clientId,
					bingAccounts.get(0).secret);
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
				List<List<String>> previousContent = null;
				for (int languageIndex = 0; languageIndex < config.toLanguages
						.size(); languageIndex++) {
					System.out.println();
					System.out.println();
					
					String toLanguage = config.toLanguages.get(languageIndex);
					List<List<String>> csvContent = outputs.get(toLanguage);
					List<String> rowContent = new ArrayList<String>();
					String[] valuesPreviousOrOriginal = values;
					if (previousContent != null) {
						valuesPreviousOrOriginal = (String[]) previousContent
								.get(rowIndex).toArray(
										new String[previousContent.size()]);
					}
					for (int index = 0; index < valuesPreviousOrOriginal.length; index++) {
						String value = valuesPreviousOrOriginal[index];
						for (Integer columnIndex : columnIndexes) {
							if (columnIndex - 1 == index) {
								
								String fromLanguage = config.fromLanguage;
								if (languageIndex > 0) {
									fromLanguage = config.toLanguages
											.get(languageIndex - 1);
								}
								System.out.println("from: "+fromLanguage+" to:"+toLanguage);
								System.out.println("Previous: "+value);
								System.out.println("---------------------");
								String combinationKey = combinationKey(value,
										fromLanguage, toLanguage);
								if (translateCache.containsKey(combinationKey)) {
									value = translateCache.get(combinationKey(
											value, fromLanguage, toLanguage));
								} else {
//									 If not yet save cache, do translates
									try {
										
										if (toLanguage.equalsIgnoreCase("bing")) {
											value = bingTranslates(value,
													fromLanguage,
													config.toLanguageBing);
										} else if (config.useGoogle) {
											value = googleTranslates(value,
													fromLanguage,
													toLanguage);
										}
										translateCache.put(combinationKey,
												value);
									} catch (Exception e) {
										e.printStackTrace();
									}

								}

							}
						}

						rowContent.add(value);
					}
					previousContent = csvContent;
					csvContent.add(rowContent);

				}
			}
		});

		int index = -1;
		for (String languageKey : outputs.keySet()) {
			index++;
			if (!config.useGoogle && index != outputs.keySet().size() - 1) {
				continue;
			}
			if (!config.useBing && index == outputs.keySet().size() - 1) {
				continue;
			}
			
			csv.write(getOutputCsvLanguage(config.outputCsv, languageKey),
					new CSVWriteProc() {
						public void process(CSVWriter out) {
							List<List<String>> csvContent = outputs
									.get(languageKey);
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

	private static String getOutputCsvLanguage(String outputOriginalFilename,
			String languageKey) {
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
			String toLang = prop.getProperty("toLanguage" + toLanguageIndex);
			while (toLang != null) {
				result.toLanguages.add(toLang);
				toLanguageIndex++;
				toLang = prop.getProperty("toLanguage" + toLanguageIndex);
			}
			if (result.useBing) {
				result.toLanguages.add("bing");
				result.toLanguageBing = prop.getProperty("toLanguageBing");
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

	private static com.memetix.mst.language.Language getBLanguageObject(
			String languageString) {
		com.memetix.mst.language.Language result = com.memetix.mst.language.Language.ENGLISH;
		for (com.memetix.mst.language.Language lang : com.memetix.mst.language.Language
				.values()) {
			if (languageString.equalsIgnoreCase(lang.name())
					|| languageString.equalsIgnoreCase(lang.toString())) {
				result = lang;
			}
		}
		return result;
	}

	private static String googleTranslates(String text, String fromLang,
			String toLang) throws Exception {
		// general constants
		final String SOURCE_TEXT = text;
		final Language SOURCE_LANGUAGE = getGLanguageObject(fromLang);
		final Language TARGET_LANGUAGE = getGLanguageObject(toLang);

		Translation translation = translator.translate(SOURCE_TEXT,
				SOURCE_LANGUAGE, TARGET_LANGUAGE);
		return translation.getTranslatedText();
	}

	private static String bingTranslates(String text, String fromLang,
			String toLang) throws Exception {
		String translatedText = Translate.execute(text,
				getBLanguageObject(fromLang), getBLanguageObject(toLang));

		return translatedText;
	}

	private static void setAuthForBingTranslates(String clientId, String secret) {
		Translate.setClientId(clientId);
		Translate.setClientSecret(secret);
	}

}
