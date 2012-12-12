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

import com.github.asilvestre.jpurexml.Utils;
import com.github.asilvestre.jpurexml.XmlParseException;

import junit.framework.TestCase;

/**
 * XML to JSON tests
 */
public class JsonConverterTest extends TestCase {

	/**
	 * Test parsing different attribute lists
	 */
	public void testParseAttributes() {
		String[] inputs = new String[] { "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<   root \t\n />",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<   root \t\n  a='&ampa\"b'    />",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><   root   a='1'   b =   \"2\"    >\n\t</root>", };

		String[] outputs = new String[] { "{\"root\":{}}", "{\"root\":{\"@a\":\"&a\\\"b\"}}",
				"{\"root\":{\"@b\":\"2\",\"@a\":\"1\"}}", };

		for (int i = 0; i < inputs.length; i++) {
			try {
				String res = JsonConverter.convertXml(inputs[i]);
				assertEquals(outputs[i], Utils.ReplaceStr(res, " ", ""));
			} catch (XmlParseException e) {
				fail("Error parsing " + e.toString());
			}
		}
	}

	public void testParseContent() {
		String[] inputs = new String[] {
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<   root \t\n> holahola </root>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<   root \t\n  a='&ampa\"b'>adeu</root>", };

		String[] outputs = new String[] { "{\"root\":{\"#content\":\"holahola\"}}",
				"{\"root\":{\"@a\":\"&a\\\"b\",\"#content\":\"adeu\"}}", };

		for (int i = 0; i < inputs.length; i++) {
			try {
				String res = JsonConverter.convertXml(inputs[i]);
				assertEquals(outputs[i], Utils.ReplaceStr(res, " ", ""));
			} catch (XmlParseException e) {
				fail("Error parsing " + e.toString());
			}
		}
	}

	public void testParseChildren() {
		String[] inputs = new String[] { "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<a>   <b/></a>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<a a_a='1'>   <b/></a>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<a a_a='1'> <b/><b/></a>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<a a_a='1'> <b/><b/><c/></a>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<a a_a='1'> <b/><b/><c/>hola</a>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<a a_a='1'> <b><d a_a='2'/></b><c/>hola</a>", };

		String[] outputs = new String[] { "{\"a\":{\"b\":{}}}", "{\"a\":{\"@a_a\":\"1\",\"b\":{}}}",
				"{\"a\":{\"@a_a\":\"1\",\"bs\":[{},{}]}}", "{\"a\":{\"@a_a\":\"1\",\"bs\":[{},{}],\"c\":{}}}",
				"{\"a\":{\"@a_a\":\"1\",\"#content\":\"hola\",\"bs\":[{},{}],\"c\":{}}}",
				"{\"a\":{\"@a_a\":\"1\",\"#content\":\"hola\",\"b\":{\"d\":{\"@a_a\":\"2\"}},\"c\":{}}}", };

		for (int i = 0; i < inputs.length; i++) {
			try {
				String res = JsonConverter.convertXml(inputs[i]);
				assertEquals(outputs[i], Utils.ReplaceStr(res, " ", ""));
			} catch (XmlParseException e) {
				fail("Error parsing " + e.toString());
			}
		}
	}
}
