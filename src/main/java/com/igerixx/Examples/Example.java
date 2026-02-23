package com.igerixx.Examples;

import com.igerixx.Reader.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Example {
    public static void main(String[] args) throws IOException {
        String filename = "src\\Files\\file.xml";
        String tagName = "section";

        List<XMLAttribute> attrs = findAttributesByTag(tagName, filename);
        String attrName = !attrs.isEmpty() ? attrs.get(0).getAttributeName() : "";
        String attrValue = !attrs.isEmpty() ? attrs.get(0).getAttributeValue() : "";

        String text = findTextByTag(tagName, filename);

        System.out.println("Tag name: " + tagName);
        System.out.printf("Attribute name: %s\n", attrName);
        System.out.printf("Attribute value: %s\n", attrValue);
        System.out.printf("Text: %s\n", text);
    }

    public static List<XMLAttribute> findAttributesByTag(String tagName, String filename) throws IOException {
        try (InputStream is = new FileInputStream(filename)) {
            XMLReader reader = new XMLReader(is);
            reader.trimText(true);
            reader.setIgnoreComments(true);

            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLReaderConstants.START_ELEMENT
                        && reader.getLocalName().equals(tagName)) {
                    return reader.getAttributes();
                }
            }

            return null;
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException();
        } catch (IOException e) {
            throw new IOException();
        }
    }

    public static String findTextByTag(String tagName, String filename) throws IOException {
        try (InputStream is = new FileInputStream(filename)) {
            XMLReader reader = new XMLReader(is);
            reader.trimText(true);
            reader.setIgnoreComments(true);

            StringBuilder stringBuilder = new StringBuilder();
            boolean inTag = false;

            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLReaderConstants.START_ELEMENT
                        && reader.getLocalName().equals(tagName)) {
                    inTag = true;
                }

                if (inTag) {
                    if (!reader.getText().isEmpty())
                        stringBuilder.append(reader.getText()).append(" ");

                    if (event == XMLReaderConstants.END_ELEMENT
                        && reader.getLocalName().equals(tagName)) {
                        return stringBuilder.toString();
                    }
                }
            }

            return null;
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException();
        } catch (IOException e) {
            throw new IOException();
        }
    }
}
