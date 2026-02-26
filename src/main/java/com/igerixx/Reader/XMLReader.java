package com.igerixx.Reader;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XMLReader {
    private XMLLexer lexer;
    private XMLToken token, lastToken;
    private String tagName = "";
    private final Map<String, String> attributes = new HashMap<>();
    private XMLAttribute attribute = new XMLAttribute();
    private String text = "";
    private String lastTag = "";
    private int event;
    private boolean ignoreComments = false;

    public XMLReader(InputStream is) throws IOException {
        lexer = new XMLLexer(is);
    }

    public XMLReader(InputStream is, Charset charset) throws IOException {
        lexer = new XMLLexer(is, charset);
    }

    public XMLReader(InputStreamReader isr) throws IOException {
        lexer = new XMLLexer(isr);
    }

    public String getLocalName() {
        return tagName;
    }

    public boolean hasAttributes() {
        return !attributes.isEmpty();
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public List<XMLAttribute> getAttributesList() {
        List<XMLAttribute> list = new ArrayList<>();
        attributes.forEach((k, v) -> {
            list.add(new XMLAttribute(k, v));
        });
        return list;
    }

    public String getAttributeValue(String attributeName) {
        return attributes.get(attributeName);
    }

    public String getText() {
        return text;
    }

    public void trimText(boolean trim) {
        lexer.trimText(trim);
    }

    public void setIgnoreComments(boolean ignoreComments) {
        this.ignoreComments = ignoreComments;
    }

    public boolean hasNext() {
        return event != XMLReaderConstants.END_DOCUMENT;
    }

    public int next() throws IOException {
        if (lexer.hasNext()) {
            while (true) {
                if (token == null) {
                    token = new XMLToken();
                    event = XMLReaderConstants.START_DOCUMENT;
                    return event;
                }

                // If tag is self-closing, it needs to be return twice as START_ELEMENT and END_ELEMENT
                if (lastTag.equals("/>")) {
                    String tempName = tagName;
                    clearData();
                    tagName = tempName;
                    lastTag = "";
                    return XMLReaderConstants.END_ELEMENT;
                }

                token = lexer.nextToken();
                switch (token.getType()) {
                    case XMLTokenConstants.PROCESSING_INSTRUCTION_OPEN, XMLTokenConstants.TAG_OPEN -> {
                        clearData();
                    }
                    case XMLTokenConstants.PROCESSING_INSTRUCTION_CLOSE -> {
                        lastTag = token.getValue();
                        event = XMLReaderConstants.PROCESSING_INSTRUCTION;
                        return event;
                    }
                    case XMLTokenConstants.NAME -> tagName = token.getValue();
                    case XMLTokenConstants.ATTR_NAME -> attribute.setAttributeName(token.getValue());
                    case XMLTokenConstants.ATTR_VALUE -> {
                        attribute.setAttributeValue(token.getValue());
                        attributes.put(attribute.getAttributeName(), attribute.getAttributeValue());
                        attribute = new XMLAttribute();
                    }
                    case XMLTokenConstants.TAG_CLOSE -> {
                        if (token.getValue().equals("/>")) {
                            lastTag = "/>";
                        }
                        event = XMLReaderConstants.START_ELEMENT;
                        return event;
                    }
                    case XMLTokenConstants.CONTENT -> {
                        clearData();
                        text = token.getValue();
                        event = XMLReaderConstants.CHARACTERS;
                        return event;
                    }
                    case XMLTokenConstants.END_TAG -> {
                        attributes.clear();
                        text = "";
                        tagName = token.getValue();
                        event = XMLReaderConstants.END_ELEMENT;
                        return event;
                    }
                    case XMLTokenConstants.DOCTYPE -> {
                        clearData();
                        text = token.getValue();
                        event = XMLReaderConstants.DTD;
                        return event;
                    }
                    case XMLTokenConstants.CDATA -> {
                        clearData();
                        text = token.getValue();
                        event = XMLReaderConstants.CDATA;
                        return event;
                    }
                    case XMLTokenConstants.COMMENT -> {
                        if (!ignoreComments) {
                            clearData();
                            text = token.getValue();
                            event = XMLReaderConstants.COMMENT;
                            return event;
                        }
                    }
                    case XMLTokenConstants.END_OF_FILE -> {
                        clearData();
                        event = XMLReaderConstants.END_DOCUMENT;
                        return event;
                    }
                }
            }
        }

        event = XMLReaderConstants.END_DOCUMENT;
        return event;
    }

    private void clearData() {
        text = "";
        tagName = "";
        attributes.clear();
    }
}