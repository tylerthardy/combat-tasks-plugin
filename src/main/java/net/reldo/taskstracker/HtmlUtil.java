package net.reldo.taskstracker;

import java.awt.Color;
import java.net.URL;

public class HtmlUtil
{
	public static String HTML_LINE_BREAK = "<br>";

	public static String wrapWithHtml(String text)
	{
		return "<html>" + text + "</html>";
	}

	public static String wrapWithWrappingParagraph(String text, int width)
	{
		return "<p width=\"" + width + "\">" + text + "</p>";
	}

	public static String wrapWithItalics(String text)
	{
		return "<i>" + text + "</i>";
	}

	public static String wrapWithBold(String text)
	{
		return "<b>" + text + "</b>";
	}

	public static String imageTag(URL url)
	{
		return "<img src=\"" + url + "\">";
	}

	public static String colorTag(String color, String text)
	{
		return "<span style=\"color: " + color + "\">" + text + "</span>";
	}

	public static String colorTag(Color color, String text)
	{
		String buf = Integer.toHexString(color.getRGB());
		String hex = "#" + buf.substring(buf.length() - 6);
		return colorTag(hex, text);
	}
}
