/**
 * @author <a href=mailto:volkodavav@gmail.com>volkodavav</a>
 */
package com.translator.google.translator;

import com.translator.google.core.Assert;
import com.translator.google.core.ProxyWrapper;
import com.translator.google.core.Translation;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import javax.net.ssl.HttpsURLConnection;
import org.codehaus.jackson.map.ObjectMapper;

public class OnlineGoogleTranslator
{
	public static final String DEFAULT_CHARSET = "UTF-8";
	public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.3) Gecko/20100401";
	/**
	 * {0} - text to translate
	 * {1} - language pair
	 */
	private static final String GOOGLE_TRANSLATOR_URL_ENDPOINT_V1_TEMPLATE = "https://ajax.googleapis.com/ajax/services/language/translate?v=1.0&q=%s&langpair=%s";
	/**
	 * {0} - API key
	 * {1} - source language
	 * {2} - target language
	 * {3} - text to translate
	 */
	private static final String GOOGLE_TRANSLATOR_URL_ENDPOINT_V2_TEMPLATE = "https://www.googleapis.com/language/translate/v2?key=%s&source=%s&target=%s&q=%s";
	private ProxyWrapper proxy;
	private String connectionCharset;
	private String userAgent;
	/**
	 * To acquire an API key, visit the APIs Console. In the Services pane, activate the Google Translate API; if the Terms of Service appear, read and accept them.
	 * Next, go to the API Access pane. The API key is near the bottom of that pane, in the section titled "Simple API Access."
	 */
	private String apiKey;
	private boolean obsoleteVersionUsed;

	@Deprecated
	public static OnlineGoogleTranslator createInstance()
	{
		return createInstance(new ProxyWrapper(Proxy.NO_PROXY));
	}

	@Deprecated
	public static OnlineGoogleTranslator createInstance(ProxyWrapper proxy)
	{
		return createInstance(proxy, DEFAULT_CHARSET, DEFAULT_USER_AGENT);
	}

	@Deprecated
	public static OnlineGoogleTranslator createInstance(String connectionCharset, String userAgent)
	{
		return createInstance(new ProxyWrapper(Proxy.NO_PROXY), connectionCharset, userAgent);
	}

	@Deprecated
	public static OnlineGoogleTranslator createInstance(ProxyWrapper proxy, String connectionCharset, String userAgent)
	{
		return new OnlineGoogleTranslator(proxy, connectionCharset, userAgent);
	}

	public static OnlineGoogleTranslator createInstance(String apiKey)
	{
		return createInstance(new ProxyWrapper(Proxy.NO_PROXY), apiKey);
	}

	public static OnlineGoogleTranslator createInstance(ProxyWrapper proxy, String apiKey)
	{
		return createInstance(proxy, DEFAULT_CHARSET, DEFAULT_USER_AGENT, apiKey);
	}

	public static OnlineGoogleTranslator createInstance(String connectionCharset, String userAgent, String apiKey)
	{
		return createInstance(new ProxyWrapper(Proxy.NO_PROXY), connectionCharset, userAgent, apiKey);
	}

	public static OnlineGoogleTranslator createInstance(ProxyWrapper proxy, String connectionCharset, String userAgent, String apiKey)
	{
		return new OnlineGoogleTranslator(proxy, connectionCharset, userAgent, apiKey);
	}

	public Translation translate(String text, Language sourceLanguage, Language targetTanguage) throws IOException
	{
		Assert.containText(text, "Text must contains text");
		Assert.notNull(sourceLanguage, "Source language must not be null");
		Assert.notNull(targetTanguage, "Target language must not be null");

		String url = null;

		if (obsoleteVersionUsed)
		{
			String translateParameters = String.format("%s|%s", sourceLanguage.value, targetTanguage.value);
			translateParameters = URLEncoder.encode(translateParameters, connectionCharset);
			text = URLEncoder.encode(text, connectionCharset);

			url = String.format(GOOGLE_TRANSLATOR_URL_ENDPOINT_V1_TEMPLATE, text, translateParameters);
		}
		else
		{
			text = URLEncoder.encode(text, connectionCharset);
			System.out.println(text);
			url = String.format(GOOGLE_TRANSLATOR_URL_ENDPOINT_V2_TEMPLATE, apiKey, sourceLanguage.value, targetTanguage.value, text);
		}

		String resultPlainText = getTranslatedText(url);
		Translation translation = new Translation();

		if (obsoleteVersionUsed)
		{
			ObjectMapper mapper = new ObjectMapper();
			TranslateResultV1 resultObj = mapper.readValue(resultPlainText, TranslateResultV1.class);

			if (resultObj.isValid())
			{
				String translatedText = resultObj.getResponseData().getTranslatedText();
				translation.setTranslatedText(translatedText);
			}
		}
		else
		{
			ObjectMapper mapper = new ObjectMapper();
			TranslateResultV2 resultObj = mapper.readValue(resultPlainText, TranslateResultV2.class);

			if (resultObj.isValid())
			{
				String translatedText = resultObj.getData().getTranslations()[0].getTranslatedText();
				translation.setTranslatedText(translatedText);
			}
		}

		return translation;
	}

	private String getTranslatedText(String httpsURL) throws IOException
	{
		Assert.containText(httpsURL, "Https URL must contains text");

		HttpsURLConnection connection = (HttpsURLConnection) new URL(httpsURL).openConnection(proxy.getProxy());
		connection.setRequestProperty("Accept-Charset", connectionCharset);
		connection.setRequestProperty("User-Agent", userAgent); // Do as if you're using browser.
		connection.setRequestProperty("Accept-Charset", connectionCharset);
		connection.setRequestProperty("Connection", "Keep-Alive");
		connection.setRequestProperty("Cache-Control", "no-cache");
		connection.setUseCaches(false);

		InetAddress address = InetAddress.getLocalHost();
		String hostname = address.getHostName();
		connection.setRequestProperty("Referer", hostname);

		connection.connect();

		InputStream response = connection.getInputStream();
		String result = getResponseDataAsString(response, getResponseCharset(connection));

		connection.disconnect();

		return result;
	}

	private String getResponseCharset(HttpsURLConnection connection) throws IOException
	{
		// HTTP response encoding
		String contentType = connection.getHeaderField("Content-Type");
		String charset = DEFAULT_CHARSET;
		for (String param : contentType.replace(" ", "").split(";"))
		{
			if (param.startsWith("charset="))
			{
				charset = param.split("=", 2)[1];
				break;
			}
		}

		return charset;
	}

	private String getResponseDataAsString(InputStream stream, String charset) throws UnsupportedEncodingException, IOException
	{
		if (stream == null || charset == null)
		{
			return null;
		}

		StringBuilder result = new StringBuilder();

		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new InputStreamReader(stream, charset));
			for (String line; (line = reader.readLine()) != null;)
			{
				result.append(line);
			}
		}
		finally
		{
			if (reader != null)
			{
				try
				{
					reader.close();
				}
				catch (IOException ioe)
				{
				}
			}
		}

		return result.toString();
	}

	public String getConnectionCharset()
	{
		return connectionCharset;
	}

	public String getUserAgent()
	{
		return userAgent;
	}

	public ProxyWrapper getProxy()
	{
		return proxy;
	}

	public String getApiKey()
	{
		return apiKey;
	}

	public boolean isObsoleteVersionUsed()
	{
		return obsoleteVersionUsed;
	}

	@Deprecated
	private OnlineGoogleTranslator(ProxyWrapper proxy, String connectionCharset, String userAgent)
	{
		Assert.notNull(proxy, "Proxy must not be null");
		Assert.containText(connectionCharset, "Connection Charset must contain text");
		Assert.containText(userAgent, "User Agent must contain text");

		this.proxy = proxy;
		this.connectionCharset = connectionCharset;
		this.userAgent = userAgent;
		this.obsoleteVersionUsed = true;
	}

	private OnlineGoogleTranslator(ProxyWrapper proxy, String connectionCharset, String userAgent, String apiKey)
	{
		Assert.notNull(proxy, "Proxy must not be null");
		Assert.containText(connectionCharset, "Connection Charset must contain text");
		Assert.containText(userAgent, "User Agent must contain text");
		Assert.containText(apiKey, "Api Key must contain text");

		this.proxy = proxy;
		this.connectionCharset = connectionCharset;
		this.userAgent = userAgent;
		this.apiKey = apiKey;
		this.obsoleteVersionUsed = false;
	}

	public static enum Language
	{
		AFRIKAANS(0, "af"),
		ALBANIAN(1, "sq"),
		ARABIC(2, "ar"),
		BELARUSIAN(3, "be"),
		BULGARIAN(4, "bg"),
		CATALAN(5, "ca"),
		CHINESE_SIMPLIFIED(6, "zh-CN"),
		CHINESE_TRADITIONAL(7, "zh-TW"),
		CROATIAN(8, "hr"),
		CZECH(9, "cs"),
		DANISH(10, "da"),
		DUTCH(11, "nl"),
		ENGLISH(12, "en"),
		ESTONIAN(13, "et"),
		FILIPINO(14, "tl"),
		FINNISH(15, "fi"),
		FRENCH(16, "fr"),
		GALICIAN(17, "gl"),
		GERMAN(18, "de"),
		GREEK(19, "el"),
		HAITIAN_CREOLE(20, "ht"),
		HEBREW(21, "iw"),
		HINDI(22, "hi"),
		HUNGARIAN(23, "hu"),
		ICELANDIC(24, "is"),
		INDONESIAN(25, "id"),
		IRISH(26, "ga"),
		ITALIAN(27, "it"),
		JAPANESE(28, "ja"),
		LATVIAN(29, "lv"),
		LITHUANIAN(30, "lt"),
		MACEDONIAN(31, "mk"),
		MALAY(31, "ms"),
		MALTESE(32, "mt"),
		NORWEGIAN(33, "no"),
		PERSIAN(34, "fa"),
		POLISH(35, "pl"),
		PORTUGUESE(36, "pt"),
		ROMANIAN(37, "ro"),
		RUSSIAN(38, "ru"),
		SERBIAN(39, "sr"),
		SLOVAK(40, "sk"),
		SLOVENIAN(41, "sl"),
		SPANISH(42, "es"),
		SWAHILI(43, "sw"),
		SWEDISH(44, "sv"),
		THAI(45, "th"),
		TURKISH(46, "tr"),
		UKRAINIAN(47, "uk"),
		VIETNAMESE(48, "vi"),
		WELSH(49, "cy"),
		YIDDISH(50, "yi");
		public final int id;
		public final String value;

		private Language(int id, String value)
		{
			this.id = id;
			this.value = value;
		}
	}

	private static class TranslateResultV2 implements Serializable
	{
		public static class Data implements Serializable
		{
			private Translation[] translations;

			public Translation[] getTranslations()
			{
				return translations;
			}

			public void setTranslations(Translation[] translations)
			{
				this.translations = translations;
			}

			@Override
			public String toString()
			{
				return "Data{" + "translations=" + translations + '}';
			}
		}
		private Data data;

		public Data getData()
		{
			return data;
		}

		public void setData(Data data)
		{
			this.data = data;
		}

		public boolean isValid()
		{
			return data != null && data.getTranslations() != null && data.getTranslations().length > 0;
		}

		@Override
		public String toString()
		{
			return "TranslateResultV2{" + "data=" + data + '}';
		}
	}

	private static class TranslateResultV1 implements Serializable
	{
		public static class ResponseData implements Serializable
		{
			private String translatedText;

			public String getTranslatedText()
			{
				return translatedText;
			}

			public void setTranslatedText(String translatedText)
			{
				this.translatedText = translatedText;
			}

			@Override
			public String toString()
			{
				return "ResponseData{" + "translatedText=" + translatedText + '}';
			}
		}
		private ResponseData responseData;
		private String responseDetails;
		private int responseStatus;

		public TranslateResultV1()
		{
		}

		public TranslateResultV1(ResponseData responseData, String responseDetails, int responseStatus)
		{
			this.responseData = responseData;
			this.responseDetails = responseDetails;
			this.responseStatus = responseStatus;
		}

		public ResponseData getResponseData()
		{
			return responseData;
		}

		public void setResponseData(ResponseData responseData)
		{
			this.responseData = responseData;
		}

		public String getResponseDetails()
		{
			return responseDetails;
		}

		public void setResponseDetails(String responseDetails)
		{
			this.responseDetails = responseDetails;
		}

		public int getResponseStatus()
		{
			return responseStatus;
		}

		public void setResponseStatus(int responseStatus)
		{
			this.responseStatus = responseStatus;
		}

		public boolean isValid()
		{
			return responseData != null && responseData.getTranslatedText() != null;
		}

		@Override
		public String toString()
		{
			return "TranslateResult{" + "responseData=" + responseData + ", responseDetails=" + responseDetails + ", responseStatus=" + responseStatus + '}';
		}
	}
}
