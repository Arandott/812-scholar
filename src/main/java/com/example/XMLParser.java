// package com.example;

// import java.io.File;
// import javax.xml.parsers.*;
// import org.w3c.dom.*;

// public class XMLParser {

//     public static Document parseXML(File file) throws Exception {
//         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//         factory.setNamespaceAware(true); // 处理命名空间
//         DocumentBuilder builder = factory.newDocumentBuilder();
//         return builder.parse(file);
//     }

//     public static String getTextFromTag(Element element, String namespaceURI, String tag) {
//         NodeList childNodes = element.getElementsByTagNameNS(namespaceURI, tag);
//         if (childNodes.getLength() > 0) {
//             Element childElement = (Element) childNodes.item(0);
//             return childElement.getTextContent().trim();
//         }
//         return null;
//     }

//     public static String getAuthors(Element titleStmt, String namespaceURI) {
//         NodeList authorList = titleStmt.getElementsByTagNameNS(namespaceURI, "author");
//         StringBuilder authors = new StringBuilder();
//         for (int i = 0; i < authorList.getLength(); i++) {
//             Element authorElement = (Element) authorList.item(i);
//             String forename = getTextFromTag(authorElement, namespaceURI, "forename");
//             String surname = getTextFromTag(authorElement, namespaceURI, "surname");
//             if (forename != null && surname != null) {
//                 authors.append(forename).append(" ").append(surname).append("; ");
//             } else if (surname != null) {
//                 authors.append(surname).append("; ");
//             }
//         }
//         return authors.toString().trim();
//     }
// }


package com.example;

import java.io.File;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class XMLParser {

    public static Document parseXML(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // 处理命名空间
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    public static String getTextFromTag(Element element, String namespaceURI, String tag) {
        NodeList childNodes = element.getElementsByTagNameNS(namespaceURI, tag);
        for (int i = 0; i < childNodes.getLength(); i++) {
            Element childElement = (Element) childNodes.item(i);
            if (childElement.getParentNode().isSameNode(element)) {
                return childElement.getTextContent().trim();
            }
        }
        return null;
    }

    public static String getAuthors(Element analytic, String namespaceURI) {
        StringBuilder authors = new StringBuilder();
        NodeList authorList = analytic.getElementsByTagNameNS(namespaceURI, "author");
        for (int i = 0; i < authorList.getLength(); i++) {
            Element authorElement = (Element) authorList.item(i);
            NodeList persNameList = authorElement.getElementsByTagNameNS(namespaceURI, "persName");
            if (persNameList.getLength() > 0) {
                Element persName = (Element) persNameList.item(0);
                String forename = getTextFromTag(persName, namespaceURI, "forename");
                String surname = getTextFromTag(persName, namespaceURI, "surname");
                if (forename != null && surname != null) {
                    authors.append(forename).append(" ").append(surname).append("; ");
                } else if (surname != null) {
                    authors.append(surname).append("; ");
                }
            }
        }
        return authors.toString().trim();
    }
}
