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
    private XMLToken token = new XMLToken();

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
        else if (readByte == -1) {
            token.setType(XMLTokenConstants.END_OF_FILE);
            token.setStringBuffer(new byte[]{'n', 'u', 'l', 'l'});
            token.setCharStringBuffer(null);
            token.setLength(4);
            return token;
        }

        while (pos < readByte) {
            checkForRefill();
            if (pos == -1) break;
            // charBuffer[pos-2] = last char
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

                    token.setType(XMLTokenConstants.PROCESSING_INSTRUCTION_OPEN);
                    token.setStringBuffer(new byte[]{'<', '?'});
                    token.setCharStringBuffer(null);
                    token.setLength(2);
                    return token;
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

                    token.setType(XMLTokenConstants.END_TAG);
                    token.setStringBuffer(null);
                    token.setCharStringBuffer(charString);
                    token.setLength(charIndex);
                    return token;
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
                            checkForRefill();

                            charString[charIndex++] = character;
                            character = charBuffer[pos++];
                        }

                        lastCharIndex = charIndex;

                        token.setType(XMLTokenConstants.DOCTYPE);
                        token.setStringBuffer(null);
                        token.setCharStringBuffer(charString);
                        token.setLength(charIndex);
                        return token;
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
                            checkForRefill();

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

                        token.setType(XMLTokenConstants.COMMENT);
                        token.setStringBuffer(null);
                        token.setCharStringBuffer(charString);
                        token.setLength(charIndex);
                        return token;
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
                            checkForRefill();
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

                        token.setType(XMLTokenConstants.CDATA);
                        token.setStringBuffer(null);
                        token.setCharStringBuffer(charString);
                        token.setLength(charIndex);
                        return token;
                    }

                    token.setType(XMLTokenConstants.PROCESSING_INSTRUCTION_OPEN);
                    token.setStringBuffer(new byte[]{'<', '!'});
                    token.setCharStringBuffer(null);
                    token.setLength(2);
                    return token;
                }

                // --- Open tag ---
                if (character == '<') {
                    state = XMLLexerConstants.TAG;

                    token.setType(XMLTokenConstants.TAG_OPEN);
                    token.setStringBuffer(new byte[]{'<'});
                    token.setCharStringBuffer(null);
                    token.setLength(1);
                    return token;
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
                    token.setType(XMLTokenConstants.EQUAL);
                    token.setStringBuffer(new byte[]{'='});
                    token.setCharStringBuffer(null);
                    token.setLength(1);
                    return token;
                }

                // --- Close tag of normal, self closed and processing instruction tags ---
                if (character == '>' || (character == '/' && charBuffer[pos] == '>') || (character == '?' && charBuffer[pos] == '>')) {
                    // --- Normal ---
                    if (character == '>') {
                        state = XMLLexerConstants.CONTENT;

                        token.setType(XMLTokenConstants.TAG_CLOSE);
                        token.setStringBuffer(new byte[]{'>'});
                        token.setCharStringBuffer(null);
                        token.setLength(1);
                        return token;
                    }
                    state = XMLLexerConstants.OUT;
                    pos++;
                    // --- Self closed ---
                    if (character == '/') {
                        token.setType(XMLTokenConstants.TAG_CLOSE);
                        token.setStringBuffer(new byte[]{'/', '>'});
                        token.setCharStringBuffer(null);
                        token.setLength(2);
                        return token;
                    }
                    // --- Processing instruction ---
                    token.setType(XMLTokenConstants.PROCESSING_INSTRUCTION_CLOSE);
                    token.setStringBuffer(new byte[]{'?', '>'});
                    token.setCharStringBuffer(null);
                    token.setLength(2);
                    return token;
                }

                // --- End tag ---
                if (character == '<' && charBuffer[pos] == '/') {
                    int charIndex = 0;

                    while (character != '>') {
                        checkForRefill();

                        charString[charIndex++] = character;
                        character = charBuffer[pos++];
                    }
                    charString[charIndex++] = character;

                    state = XMLLexerConstants.OUT;

                    token.setType(XMLTokenConstants.END_TAG);
                    token.setStringBuffer(null);
                    token.setCharStringBuffer(charString);
                    token.setLength(charIndex);
                    return token;
                }

                // 32 - whitespace
                // --- Tag name ---
                if ((character != '"' && character != '\'') && character > 32 && isLastAttribute) {
                    int charIndex = 0;

                    while (character != ' ' && character != '>') {
                        checkForRefill();

                        charString[charIndex++] = character;
                        character = charBuffer[pos++];

                        if (character == '/') {
                            break;
                        }
                    }

                    pos--;

                    token.setType(XMLTokenConstants.NAME);
                    token.setStringBuffer(null);
                    token.setCharStringBuffer(charString);
                    token.setLength(charIndex);
                    return token;
                }

                // --- Attribute name and close tag ---
                if (character == ' ' || character >= 65) {
                    int charIndex = 0;

                    // 47 - '/'
                    while (character < 47) {
                        checkForRefill();

                        character = charBuffer[pos++];
                        if (character == '/' || character == '>') {
                            state = XMLLexerConstants.OUT;
                            if (character == '/') {
                                pos++;

                                token.setType(XMLTokenConstants.TAG_CLOSE);
                                token.setStringBuffer(new byte[]{'/', '>'});
                                token.setCharStringBuffer(null);
                                token.setLength(2);
                                return token;
                            }

                            token.setType(XMLTokenConstants.TAG_CLOSE);
                            token.setStringBuffer(null);
                            token.setCharStringBuffer(new char[]{character});
                            token.setLength(1);
                            return token;
                        }
                    }

                    while (character != '=') {
                        checkForRefill();

                        charString[charIndex++] = character;
                        character = charBuffer[pos++];

                        if (character == '=') {
                            pos--;
                        }
                    }

                    token.setType(XMLTokenConstants.ATTR_NAME);
                    token.setStringBuffer(null);
                    token.setCharStringBuffer(charString);
                    token.setLength(charIndex);
                    return token;
                }

                // --- Attribute value ---
                if (character == '"' || character == '\'') {
                    int charIndex = 0;
                    character = charBuffer[pos++];

                    while (character != '"' && character != '\'') {
                        checkForRefill(pos+1);

                        // Entity check
                        if (character == '&')
                            charString[charIndex++] = entityChange(charBuffer, pos);
                        else
                            charString[charIndex++] = character;

                        character = charBuffer[pos++];
                    }

                    checkForRefill();
                    isLastAttribute = !(charBuffer[pos] >= 65 || charBuffer[pos] == ' ');
                    if (charBuffer[pos] == ' ') {
                        while (charBuffer[pos++] <= 32) {}
                        isLastAttribute = !(charBuffer[pos] >= 65);
                        pos--;
                    }

                    token.setType(XMLTokenConstants.ATTR_VALUE);
                    token.setStringBuffer(null);
                    token.setCharStringBuffer(charString);
                    token.setLength(charIndex);
                    return token;
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
                            checkForRefill(pos+1);
                            // --- Tag open ---
                            if (charBuffer[pos] == '<' && (charBuffer[pos+1] != '!' && charBuffer[pos+1] != '/')) {
                                pos++;
                                state = XMLLexerConstants.TAG;

                                token.setType(XMLTokenConstants.TAG_OPEN);
                                token.setStringBuffer(new byte[]{'<'});
                                token.setCharStringBuffer(null);
                                token.setLength(1);
                                return token;
                            }
                            // Check for <! keyword
                            else if (charBuffer[pos] == '<') {
                                state = XMLLexerConstants.OUT;

                                token.setType(XMLTokenConstants.TAG_OPEN);
                                token.setStringBuffer(null);
                                token.setCharStringBuffer(new char[]{'<', charBuffer[pos+1]});
                                token.setLength(2);
                                return token;
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
                            checkForRefill(pos+1);
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

                    token.setType(XMLTokenConstants.CONTENT);
                    token.setStringBuffer(null);
                    token.setCharStringBuffer(charString);
                    token.setLength(lastCharIndex);
                    return token;
                } else {
                    state = XMLLexerConstants.TAG;
                    if (charBuffer[pos] == '/' || charBuffer[pos] == '!') {
                        pos--;
                        state = XMLLexerConstants.OUT;
                        if (charBuffer[pos] == '!') continue;
                    }

                    token.setType(XMLTokenConstants.TAG_OPEN);
                    token.setStringBuffer(new byte[]{'<'});
                    token.setCharStringBuffer(null);
                    token.setLength(1);
                    return token;
                }
            }
        }

        token.setType(XMLTokenConstants.END_OF_FILE);
        token.setStringBuffer(new byte[]{'n', 'u', 'l', 'l'});
        token.setCharStringBuffer(null);
        token.setLength(4);
        return token;
    }

    private char entityChange(char[] buf, int pos) throws IOException {
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
