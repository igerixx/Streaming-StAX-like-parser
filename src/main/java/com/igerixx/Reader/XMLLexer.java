package com.igerixx.Reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class XMLLexer {
    private InputStreamReader isr;
    private final int bufferSize = 16 * 1024; // 16 Kb
    private final int stringBufferSize = 4 * 1024; // 4 Kb
    private final char[] charBuffer = new char[bufferSize];
    private final char[] charString = new char[stringBufferSize];
    private int readByte = 0;
    private int pos = 0;
    private int lastCharIndex = 0;
    private int state = XMLLexerConstants.OUT;
    private boolean trim = true;
    private boolean isStringBufferFull = false;
    private boolean isLastAttribute = true;

    public XMLLexer(InputStream is) throws IOException {
        isr = new InputStreamReader(is, StandardCharsets.UTF_8);
        refill();
    }

    public XMLLexer(InputStream is, Charset charset) throws IOException {
        isr = new InputStreamReader(is, charset);
        refill();
    }

    public XMLLexer(InputStreamReader isr) throws IOException {
        this.isr = isr;
        refill();
    }

    private void refill() throws IOException {
        readByte = isr.read(charBuffer);
    }

    public XMLToken nextToken() throws IOException {
        if (isr == null || (pos >= readByte && readByte != -1)) {
            pos = 0;
            refill();
        }
        else if (readByte == -1)
            return new XMLToken(XMLTokenConstants.END_OF_FILE, new byte[]{'n', 'u', 'l', 'l'}, 4);

        while (pos < readByte) {
            // charBuffer[pos-1] = last char
            // character =         char at the moment
            // charBuffer[pos] =   next char
            char character = charBuffer[pos++];

            // --- Return open tag, end tag, comment, doctype, cdata and content ---
            if (state == XMLLexerConstants.OUT) {
                checkForRefill();
                // --- Open tag processing instruction ---
                if (character == '<' && charBuffer[pos] == '?') {
                    state = XMLLexerConstants.TAG;
                    pos++;
                    return new XMLToken(XMLTokenConstants.PROCESSING_INSTRUCTION_OPEN, new byte[]{'<', '?'}, 2);
                }

                // Clear byteString
                if (lastCharIndex != 0) {
                    Arrays.fill(charString, (char) 0);
                }

                // --- End tag ---
                if (character == '<' && charBuffer[pos] == '/') {
                    int charIndex = 0;
                    checkForRefill(pos+1);
                    character = charBuffer[pos+1];
                    pos+=2;

                    while (character != '>') {
                        checkForRefill();
                        charString[charIndex++] = character;
                        character = charBuffer[pos++];
                    }

                    return new XMLToken(XMLTokenConstants.END_TAG, charString, charIndex);
                }

                // --- Comment, doctype and CData ---
                if (character == '<' && charBuffer[pos] == '!') {
                    // 65 - 'A'
                    // --- Doctype ---
                    if (charBuffer[pos+1] >= 65 && charBuffer[pos+1] != '[') {
                        int charIndex = 0;

                        // Skip DOCTYPE keyword
                        while (charBuffer[pos++] != ' ') {}
                        character = charBuffer[pos++];

                        while (character != '>') {
                            charString[charIndex++] = character;
                            character = charBuffer[pos++];
                        }

                        lastCharIndex = charIndex;
                        return new XMLToken(XMLTokenConstants.DOCTYPE, charString, charIndex);
                    }

                    // --- Comment ---
                    if (charBuffer[pos+1] == '-') {
                        int charIndex = 0;

                        // Skip comment start <!--
                        pos += 3;
                        character = charBuffer[pos++];

                        if (trim) {
                            // 32 - whitespace
                            // Remove spaces and special symbols before comment string
                            while (character <= 32) character = charBuffer[pos++];
                        }

                        // Add characters until next characters is -->
                        while (true) {
                            if (charBuffer[pos] == '-' && charBuffer[pos+1] == '-' && charBuffer[pos+2] == '>') break;

                            charString[charIndex++] = character;
                            character = charBuffer[pos++];
                        }
                        charString[charIndex++] = charBuffer[pos-1];

                        if (trim) {
                            // Remove spaces and special symbols after string
                            if (charString[charIndex] <= 32)
                                while (charString[--charIndex] <= 32) {}
                            charIndex++;
                        }

                        pos += 3;
                        lastCharIndex = charIndex;
                        return new XMLToken(XMLTokenConstants.COMMENT, charString, charIndex);
                    }

                    // --- CData ---
                    if (charBuffer[pos+1] == '[') {
                        int charIndex = 0;

                        // Skip cdata start <![CDATA[ keyword
                        pos += 8;
                        character = charBuffer[pos++];

                        // 32 - whitespace
                        // Remove spaces and special symbols before comment string
                        while (character <= 32) character = charBuffer[pos++];

                        // Add characters until next characters is ]]>
                        while (true) {
                            if (charBuffer[pos] == ']' && charBuffer[pos+1] == ']' && charBuffer[pos+2] == '>') break;

                            charString[charIndex++] = character;
                            character = charBuffer[pos++];
                        }
                        charString[charIndex++] = charBuffer[pos-1];

                        if (trim) {
                            // Remove spaces and special symbols after string
                            if (charString[charIndex] <= 32)
                                while (charString[--charIndex] <= 32) {}
                            charIndex++;
                        }

                        lastCharIndex = charIndex;
                        pos += 3;
                        return new XMLToken(XMLTokenConstants.CDATA, charString, charIndex);
                    }

                    return new XMLToken(XMLTokenConstants.PROCESSING_INSTRUCTION_OPEN, new byte[]{'<', '!'}, 2);
                }

                // --- Open tag ---
                if (character == '<') {
                    state = XMLLexerConstants.TAG;
                    return new XMLToken(XMLTokenConstants.TAG_OPEN, new byte[]{'<'}, 1);
                }

                // After END_TAG state is OUT, so this block needs to add text after it
                // <tag>Text with <b>bold</b> font</tag>
                //                         ^    ^
                //                    END_TAG  CONTENT
                // 32 - whitespace
                // --- Content ---
                if (character >= 32) {
                    pos--;
                    state = XMLLexerConstants.CONTENT;
                    continue;
                }
            }

            // --- Return tag name, attribute name, attribute value, equal and close tag ---
            if (state == XMLLexerConstants.TAG) {
                // --- Equal ---
                if (character == '=') {
                    return new XMLToken(XMLTokenConstants.EQUAL, new byte[]{'='}, 1);
                }

                // --- Close tag of normal, self closed and processing instruction tags ---
                if (character == '>' || (character == '/' && charBuffer[pos] == '>') || (character == '?' && charBuffer[pos] == '>')) {
                    // --- Normal ---
                    if (character == '>') {
                        state = XMLLexerConstants.CONTENT;
                        return new XMLToken(XMLTokenConstants.TAG_CLOSE, new byte[]{'>'}, 1);
                    }
                    state = XMLLexerConstants.OUT;
                    pos++;
                    // --- Self closed ---
                    if (character == '/') {
                        return new XMLToken(XMLTokenConstants.TAG_CLOSE, new byte[]{'/', '>'}, 2);
                    }
                    // --- Processing instruction ---
                    return new XMLToken(XMLTokenConstants.PROCESSING_INSTRUCTION_CLOSE, new byte[]{'?', '>'}, 2);
                }

                // --- End tag ---
                if (character == '<' && charBuffer[pos] == '/') {
                    int charIndex = 0;

                    while (character != '>') {
                        charString[charIndex++] = character;
                        character = charBuffer[pos++];
                    }
                    charString[charIndex++] = character;

                    state = XMLLexerConstants.OUT;
                    return new XMLToken(XMLTokenConstants.END_TAG, charString, charIndex);
                }

                // 32 - whitespace
                // --- Tag name ---
                if ((character != '"' && character != '\'') && character > 32 && isLastAttribute) {
                    int charIndex = 0;

                    while (character != ' ' && character != '>') {
                        charString[charIndex++] = character;
                        character = charBuffer[pos++];

                        if (character == '/') {
                            break;
                        }
                    }

                    pos--;
                    return new XMLToken(XMLTokenConstants.NAME, charString, charIndex);
                }

                // --- Attribute name and close tag ---
                if (character == ' ' || character >= 65) {
                    int charIndex = 0;

                    // 47 - '/'
                    while (character < 47) {
                        character = charBuffer[pos++];
                        if (character == '/' || character == '>') {
                            state = XMLLexerConstants.OUT;
                            if (character == '/') {
                                pos++;
                                return new XMLToken(XMLTokenConstants.TAG_CLOSE, new byte[]{'/', '>'}, 2);
                            }

                            return new XMLToken(XMLTokenConstants.TAG_CLOSE, new char[]{character}, 1);
                        }
                    }

                    while (character != '=') {
                        charString[charIndex++] = character;
                        character = charBuffer[pos++];

                        if (character == '=') {
                            pos--;
                        }
                    }

                    return new XMLToken(XMLTokenConstants.ATTR_NAME, charString, charIndex);
                }

                // --- Attribute value ---
                if (character == '"' || character == '\'') {
                    int charIndex = 0;
                    character = charBuffer[pos++];

                    while (character != '"' && character != '\'') {
                        // Entity check
                        if (character == '&')
                            charString[charIndex++] = entityChange(charBuffer, pos);
                        else
                            charString[charIndex++] = character;

                        checkForRefill();
                        character = charBuffer[pos++];
                    }

                    isLastAttribute = !(charBuffer[pos] >= 65 || charBuffer[pos] == ' ');
                    if (charBuffer[pos] == ' ') {
                        while (charBuffer[pos++] <= 32) {}
                        isLastAttribute = !(charBuffer[pos] >= 65);
                        pos--;
                    }

                    return new XMLToken(XMLTokenConstants.ATTR_VALUE, charString, charIndex);
                }
            }

            // --- Content ---
            if (state == XMLLexerConstants.CONTENT) {
                if (character != '<') {
                    int charIndex = 0;

                    // Clear charString
                    if (lastCharIndex != 0)
                        Arrays.fill(charString, (char) 0);

                    if (trim && !isStringBufferFull) {
                        // 32 - whitespace
                        // Remove spaces and special symbols before string
                        while (character <= 32) {
                            checkForRefill();
                            // --- Tag open ---
                            if (charBuffer[pos] == '<' && (charBuffer[pos+1] != '!' && charBuffer[pos+1] != '/')) {
                                pos++;
                                state = XMLLexerConstants.TAG;
                                return new XMLToken(XMLTokenConstants.TAG_OPEN, new byte[]{'<'}, 1);
                            }
                            // Check for <! keyword
                            else if (charBuffer[pos] == '<') {
                                state = XMLLexerConstants.OUT;
                                return new XMLToken(XMLTokenConstants.TAG_OPEN, new char[]{'<', charBuffer[pos]}, 2);
                            }
                            character = charBuffer[pos++];
                        }
                    }

                    checkForRefill();
                    // Skip if there's <!
                    if (charBuffer[pos] == '<' && charBuffer[pos+1] == '!') continue;

                    while (character != '<') {
                        checkForRefill();

                        if (charIndex == stringBufferSize - 1) {
                            isStringBufferFull = true;
                            break;
                        } else {
                            isStringBufferFull = false;
                        }

                        if (character >= 32) {
                            // Entity check
                            if (character == '&')
                                charString[charIndex++] = entityChange(charBuffer, pos);
                            else
                                charString[charIndex++] = character;
                        }

                        character = charBuffer[pos++];
                    }

                    if (trim && !isStringBufferFull) {
                        // Remove spaces and special symbols after string
                        if (charString[charIndex] <= 32) {
                            while (charString[--charIndex] <= 32) {}
                            charIndex++;
                        }
                    }

                    pos -= pos == 0 ? 0 : 1;
                    lastCharIndex = charIndex;
                    if (!isStringBufferFull)
                        state = XMLLexerConstants.OUT;

                    return new XMLToken(XMLTokenConstants.CONTENT, charString, lastCharIndex);
                } else {
                    state = XMLLexerConstants.TAG;
                    if (charBuffer[pos] == '/' || charBuffer[pos] == '!') {
                        pos--;
                        state = XMLLexerConstants.OUT;
                        if (charBuffer[pos] == '!') continue;
                    }

                    return new XMLToken(XMLTokenConstants.TAG_OPEN, new byte[]{'<'}, 1);
                }
            }
        }

        return new XMLToken(XMLTokenConstants.END_OF_FILE, new byte[]{'n', 'u', 'l', 'l'}, 4);
    }

    private char entityChange(char[] buf, int pos) {
        // &amp;
        if (buf[pos] == 'a' && buf[pos+1] == 'm' && buf[pos+2] == 'p') {
            this.pos += 4;
            return '&';
        }
        // &lt;
        if (buf[pos] == 'l' && buf[pos+1] == 't') {
            this.pos += 3;
            return '<';
        }
        // &gt;
        if (buf[pos] == 'g' && buf[pos+1] == 't') {
            this.pos += 3;
            return '>';
        }
        // &quot;
        if (buf[pos] == 'q' && buf[pos+1] == 'u' && buf[pos+2] == 'o' && buf[pos+3] == 't') {
            this.pos += 5;
            return '"';
        }
        // &apos;
        if (buf[pos] == 'a' && buf[pos+1] == 'p' && buf[pos+2] == 'o' && buf[pos+3] == 's') {
            this.pos += 5;
            return '\'';
        }

        // &#..;
        if (buf[pos] == '#' && buf[pos+1] != 'x') {
            pos++;
            int codeSize = 0;

            while (buf[pos++] != ';') codeSize++;

            switch (codeSize) {
                case 3 -> {
                    this.pos += 5;
                    return (char) ((buf[pos-4] - '0') * 100 + (buf[pos-3] - '0') * 10 + buf[pos-2] - '0');
                }
                case 2 -> {
                    this.pos += 4;
                    return (char) ((buf[pos-3] - '0') * 10 + buf[pos-2] - '0');
                }
                case 1 -> {
                    this.pos += 3;
                    return (char) (buf[pos-2] - '0');
                }
            }
        }

        // &#x..;
        else if (buf[pos] == '#') {
            pos += 2;
            int codeSize = 0;

            while (buf[pos++] != ';') codeSize++;

            switch (codeSize) {
                case 2 -> {
                    this.pos += 5;
                    return (char) ((buf[pos-3] - (buf[pos-3] >= 65 ? '7' : '0')) * 16 +
                                            (buf[pos-2] - (buf[pos-2] >= 65 ? '7' : '0')));
                }
                case 1 -> {
                    this.pos += 4;
                    return (char) (buf[pos-2] - (buf[pos-2] >= 65 ? '7' : '0'));
                }
            }
        }

        return 0;
    }

    private void checkForRefill() throws IOException {
        if (this.pos >= this.bufferSize) {
            this.pos = 0;
            refill();
        }
    }

    private void checkForRefill(int pos) throws IOException {
        if (pos >= this.bufferSize) {
            this.pos = 0;
            refill();
        }
    }

    public boolean hasNext() {
        return pos <= readByte;
    }

    public void trimText(boolean trim) {
        this.trim = trim;
    }
}
