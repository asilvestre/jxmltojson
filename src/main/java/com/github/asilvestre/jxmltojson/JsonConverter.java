/**
 * Copyright Antoni Silvestre
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.github.asilvestre.jxmltojson;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import com.github.asilvestre.jpurexml.Utils;
import com.github.asilvestre.jpurexml.XmlDoc;
import com.github.asilvestre.jpurexml.XmlParseException;
import com.github.asilvestre.jpurexml.XmlParser;
import com.github.asilvestre.jpurexml.XmlTag;

/**
 * Holds static functions to convert an XML string to a JSON string
 */
public class JsonConverter {
	/**
	 * Convert an XML string to a JSON string
	 * 
	 * @param xml
	 * @return
	 */
	public static String convertXml(String xml) throws XmlParseException {
		return convertXml(xml, new ConverterConfig());
	}

	/**
	 * Convert an XML string to a JSON string
	 * 
	 * @param xml
	 * @param config
	 *            Configuration for the converter, with configuration parameters
	 *            like the prefix for tag attributes
	 * @return
	 */
	public static String convertXml(String xml, ConverterConfig config) throws XmlParseException {
		// First of all parse the XML
		XmlDoc xmlDoc = XmlParser.parseXml(xml);

		String convertedJson = convertXmlTag(xmlDoc.root, config);

		return String.format("{\"%s\": %s}", xmlDoc.root.name, convertedJson);
	}

	/**
	 * Convert an XML tag and its children to JSON
	 * 
	 * @param tag
	 * @param config
	 * @return
	 */
	private static String convertXmlTag(XmlTag tag, ConverterConfig config) {
		// Serializing tag attributes if any
		String attributeStr = convertAttributes(tag, config);

		// Serializing tag contents if any
		String contentStr = "";
		if (!tag.content.isEmpty()) {
			contentStr = String.format("\"%s\": \"%s\"", config.contentId, escapeJsonLiteral(tag.content));
		}

		// Serializing children contents if any
		String childrenStr = convertChildren(tag, config);

		// Joining all the parts of the tag
		String res = "";
		LinkedList<String> tagParts = new LinkedList<String>();
		if (!attributeStr.isEmpty()) {
			tagParts.add(attributeStr);
		}

		if (!contentStr.isEmpty()) {
			tagParts.add(contentStr);
		}

		if (!childrenStr.isEmpty()) {
			tagParts.add(childrenStr);
		}

		Iterator<String> iterParts = tagParts.iterator();
		// First item, the rest will have a comma in between
		if (iterParts.hasNext()) {
			res = iterParts.next();
		}
		while (iterParts.hasNext()) {
			res += String.format(", %s", iterParts.next());
		}

		res = String.format("{%s}", res);

		return res;
	}

	/**
	 * Serialize the attributes of the tag if any
	 * 
	 * @param tag
	 * @return
	 */
	private static String convertAttributes(XmlTag tag, ConverterConfig config) {
		AbstractMap<String, String> attrs = tag.attributes;
		Iterator<Entry<String, String>> iterEntries = attrs.entrySet().iterator();

		String res = "";

		// Serializing the first attribute
		if (iterEntries.hasNext()) {
			Entry<String, String> entry = iterEntries.next();
			res = String.format("\"%s%s\": \"%s\"", config.attributePrefix, escapeJsonLiteral(entry.getKey()),
					escapeJsonLiteral(entry.getValue()));
		}

		// Serializing the rest with commas in between
		while (iterEntries.hasNext()) {
			Entry<String, String> entry = iterEntries.next();
			res += String.format(", \"%s%s\": \"%s\"", config.attributePrefix, escapeJsonLiteral(entry.getKey()),
					escapeJsonLiteral(entry.getValue()));
		}

		return res;
	}

	/**
	 * Serialize the tag children if any
	 * 
	 * @param tag
	 * @param config
	 * @return
	 */
	private static String convertChildren(XmlTag tag, ConverterConfig config) {
		String childrenStr = "";

		// Grouping all children with the same name into arrays
		TreeMap<String, Vector<XmlTag>> childGroups = new TreeMap<String, Vector<XmlTag>>();
		Iterator<XmlTag> iterChildren = tag.children.iterator();
		while (iterChildren.hasNext()) {
			XmlTag child = iterChildren.next();

			Vector<XmlTag> childs;
			// If this group didn't exist before, create it
			if (!childGroups.containsKey(child.name)) {
				childs = new Vector<XmlTag>();
				childGroups.put(child.name, childs);
			} else {
				childs = childGroups.get(child.name);
			}

			childs.add(child);
		}

		// If the tag has children, serialize it
		Iterator<Entry<String, Vector<XmlTag>>> iterGroups = childGroups.entrySet().iterator();
		while (iterGroups.hasNext()) {
			// if the group only has one entry serialize it as is
			Entry<String, Vector<XmlTag>> entry = iterGroups.next();
			String name = entry.getKey();
			Vector<XmlTag> childs = entry.getValue();
			if (childs.size() == 1) {
				XmlTag child = childs.get(0);
				childrenStr += String.format("\"%s\": %s, ", escapeJsonLiteral(child.name),
						convertXmlTag(child, config));
			} else if (childs.size() > 1) {
				String arrayValues = "";
				for (int i = 0; i < childs.size(); i++) {
					XmlTag child = childs.get(i);
					arrayValues += String.format("%s, ", convertXmlTag(child, config));
				}
				// Removing the last comma
				if (arrayValues.length() > 2) {
					arrayValues = arrayValues.substring(0, arrayValues.length() - 2);
				}

				childrenStr += String.format("\"%s%s\": [%s], ", escapeJsonLiteral(name), config.childGroupSuffix,
						arrayValues);
			}
		}
		// Removing the last comma
		if (childrenStr.length() > 2) {
			childrenStr = childrenStr.substring(0, childrenStr.length() - 2);
		}

		return childrenStr;
	}

	private static final char[] EspecialChars = new char[] { '\\', '"', };

	/**
	 * Escape a JSON literal, for now " and \
	 * 
	 * @param literal
	 * @return
	 */
	public static String escapeJsonLiteral(String literal) {
		String res = literal;
		for (int i = 0; i < EspecialChars.length; i++) {
			res = Utils.ReplaceStr(res, "" + EspecialChars[i], "\\" + EspecialChars[i]);
		}

		return res;
	}

}
