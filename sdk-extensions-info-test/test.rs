/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#[cfg(test)]
mod tests {
    use anyhow::{anyhow, bail, ensure, Result};
    use itertools::Itertools;
    use std::fmt::Debug;
    use std::io::Read;
    use xml::attribute::OwnedAttribute;
    use xml::reader::{ParserConfig, XmlEvent};

    fn get_attribute(attributes: &[OwnedAttribute], tag: &str, key: &str) -> Result<String> {
        attributes
            .iter()
            .find_map(
                |attr| {
                    if attr.name.local_name == key {
                        Some(attr.value.clone())
                    } else {
                        None
                    }
                },
            )
            .ok_or_else(|| anyhow!("tag {}: missing attribute {}", tag, key))
    }

    fn verify_xml<R: Read>(mut source: R) -> Result<()> {
        #[derive(Debug)]
        struct Sdk {
            id: String,
            shortname: String,
            name: String,
            reference: String,
        }

        #[derive(Debug)]
        struct Symbol {
            #[allow(dead_code)]
            jar: String,
            #[allow(dead_code)]
            pattern: String,
            sdks: Vec<String>,
        }

        // this will error out on XML syntax errors
        let reader = ParserConfig::new().create_reader(&mut source);
        let events: Vec<_> = reader.into_iter().collect::<Result<Vec<_>, _>>()?;

        // parse XML
        let mut sdks = vec![];
        let mut symbols = vec![];
        for (name, attributes) in events.into_iter().filter_map(|e| match e {
            XmlEvent::StartElement { name, attributes, namespace: _ } => {
                Some((name.local_name, attributes))
            }
            _ => None,
        }) {
            match name.as_str() {
                "sdk-extensions-info" => {}
                "sdk" => {
                    let sdk = Sdk {
                        id: get_attribute(&attributes, "sdk", "id")?,
                        shortname: get_attribute(&attributes, "sdk", "shortname")?,
                        name: get_attribute(&attributes, "sdk", "name")?,
                        reference: get_attribute(&attributes, "sdk", "reference")?,
                    };
                    sdks.push(sdk);
                }
                "symbol" => {
                    let symbol = Symbol {
                        jar: get_attribute(&attributes, "symbol", "jar")?,
                        pattern: get_attribute(&attributes, "symbol", "pattern")?,
                        sdks: get_attribute(&attributes, "symbol", "sdks")?
                            .split(',')
                            .map(|s| s.to_owned())
                            .collect(),
                    };
                    symbols.push(symbol);
                }
                _ => bail!("unknown tag '{}'", name),
            }
        }

        // verify all Sdk fields are unique across all Sdk items
        ensure!(
            sdks.iter().duplicates_by(|sdk| &sdk.id).collect::<Vec<_>>().is_empty(),
            "multiple sdk entries with identical id value"
        );
        ensure!(
            sdks.iter().duplicates_by(|sdk| &sdk.shortname).collect::<Vec<_>>().is_empty(),
            "multiple sdk entries with identical shortname value"
        );
        ensure!(
            sdks.iter().duplicates_by(|sdk| &sdk.name).collect::<Vec<_>>().is_empty(),
            "multiple sdk entries with identical name value"
        );
        ensure!(
            sdks.iter().duplicates_by(|sdk| &sdk.reference).collect::<Vec<_>>().is_empty(),
            "multiple sdk entries with identical reference value"
        );

        // verify Sdk id field has the expected format (positive integer)
        for id in sdks.iter().map(|sdk| &sdk.id) {
            ensure!(id.parse::<usize>().is_ok(), "sdk id {} not a positive int", id);
        }

        // verify individual Symbol elements
        let sdk_shortnames: Vec<_> = sdks.iter().map(|sdk| &sdk.shortname).collect();
        for symbol in symbols.iter() {
            ensure!(
                symbol.sdks.iter().duplicates().collect::<Vec<_>>().is_empty(),
                "symbol contains duplicate references to the same sdk"
            );
            ensure!(!symbol.jar.contains(char::is_whitespace), "jar contains whitespace");
            ensure!(!symbol.pattern.contains(char::is_whitespace), "pattern contains whitespace");
            for id in symbol.sdks.iter() {
                ensure!(sdk_shortnames.contains(&id), "symbol refers to non-existent sdk {}", id);
            }
        }

        Ok(())
    }

    #[test]
    fn test_get_attribute() {
        use xml::EventReader;

        let mut iter = EventReader::from_str(r#"<tag a="A" b="B" c="C"/>"#).into_iter();
        let _ = iter.next().unwrap(); // skip start of doc
        let Ok(XmlEvent::StartElement { attributes, .. }) = iter.next().unwrap() else {
            panic!();
        };
        assert_eq!(get_attribute(&attributes, "tag", "a").unwrap(), "A");
        assert!(get_attribute(&attributes, "tag", "no-such-attribute").is_err());
    }

    #[test]
    fn test_verify_xml_correct_input() {
        verify_xml(&include_bytes!("testdata/correct.xml")[..]).unwrap();
    }

    #[test]
    fn test_verify_xml_incorrect_input() {
        macro_rules! assert_err {
            ($input_path:expr, $expected_error:expr) => {
                let error = verify_xml(&include_bytes!($input_path)[..]).unwrap_err().to_string();
                assert_eq!(error, $expected_error);
            };
        }

        assert_err!(
            "testdata/corrupt-xml.xml",
            "25:1 Unexpected end of stream: still inside the root element"
        );
        assert_err!(
            "testdata/duplicate-sdk-id.xml",
            "multiple sdk entries with identical id value"
        );
        assert_err!(
            "testdata/duplicate-sdk-shortname.xml",
            "multiple sdk entries with identical shortname value"
        );
        assert_err!(
            "testdata/duplicate-sdk-name.xml",
            "multiple sdk entries with identical name value"
        );
        assert_err!(
            "testdata/duplicate-sdk-reference.xml",
            "multiple sdk entries with identical reference value"
        );
        assert_err!("testdata/incorrect-sdk-id-format.xml", "sdk id 1.0 not a positive int");
        assert_err!(
            "testdata/duplicate-symbol-sdks.xml",
            "symbol contains duplicate references to the same sdk"
        );
        assert_err!(
            "testdata/symbol-refers-to-non-existent-sdk.xml",
            "symbol refers to non-existent sdk does-not-exist"
        );
        assert_err!("testdata/whitespace-in-jar.xml", "jar contains whitespace");
        assert_err!("testdata/whitespace-in-pattern.xml", "pattern contains whitespace");
    }

    #[test]
    fn test_actual_sdk_extensions_info_contents() {
        verify_xml(&include_bytes!("../sdk-extensions-info.xml")[..]).unwrap();
    }
}
