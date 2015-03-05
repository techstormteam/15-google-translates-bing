/**
 * @author <a href=mailto:volkodavav@gmail.com>volkodavav</a>
 */
package com.translator.google.core;

import java.io.Serializable;

public class Translation implements Serializable
{
	private String translatedText;

	public Translation()
	{
	}

	public Translation(String translatedText)
	{
		this.translatedText = translatedText;
	}

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
		return "Translations{" + "translatedText=" + translatedText + '}';
	}
}
