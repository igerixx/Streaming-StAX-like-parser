package com.igerixx.Reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class XMLReader {
    private XMLLexer lexer;
    private XMLToken token, lastToken;
    private String tagName = "";
    private final List<XMLAttribute> attributes = new ArrayList<>();
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

    public List<XMLAttribute> getAttributes() {
        return attributes;
    }

    public String getAttributeLocalName(int index) {
        return attributes.get(index).getAttributeName();
    }

    public String getAttributeValue(int index) {
        return attributes.get(index).getAttributeValue();
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
                        attributes.add(attribute);
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