# Streaming StAX-like parser

## Description
A StAX-like parser is a streaming parser inspired by the StAX (Streaming API for XML) approach, which processes data sequentially without loading the entire document into memory. It uses a pull-parsing model, where the user explicitly requests the next token (such as a start tag, text node, or end tag).

### Key characteristics:
 - Stream-based reading of data
 - Low memory consumption
 - Controlled parsing via next() / hasNext()-style methods
 - Ability to process very large documents efficiently
 - No requirement to construct a DOM tree

### Use cases
 - Processing large XML files
 - Network stream parsing
 - Situations where memory efficiency and speed are important

## Installation
Clone repository
```bash
git clone https://github.com/igerixx/Streaming-StAX-like-parser.git
cd src/main/java/com/igerixx/Streaming-StAX-like-parser
```
Build the project, make sure you have Java and Maven installed.
```bash
mvn clean package
```
After the build completes successfully, the `.jar` file will be located in:
```code
target/
```
To use it with Maven add dependency
```xml
<dependency>
    <groupId>com.igerixx.Reader</groupId>
    <artifactId>streaming-xml-parser</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage
Initialization
```java
InputStream is = new FileInputStream("file.xml");
XMLReader reader = new XMLReader(is);
```
```java
InputStream is = new FileInputStream("file.xml");
XMLReader reader = new XMLReader(is, StandardCharsets.UTF_8);
```
```java
InputStream is = new FileInputStream("file.xml");
InputStreamReader isr = new InputStreamReader(is);
XMLReader reader = new XMLReader(isr);
```

Change pereferences
```java
reader.trimText(true);
reader.setIgnoreComments(true);
```

Print all tags
```java
while (reader.hasNext()) {
    int event = reader.next();
    if (event == XMLReaderConstants.START_ELEMENT)
        System.out.println(reader.getLocalName());
}
```

Find tag by name
```java
String tagName = "tag";
while (reader.hasNext()) {
    int event = reader.next();
    if (event == XMLReaderConstants.START_ELEMENT
            && reader.getLocalName().equals(tagName)) {
        break;
    }
}
```

Get tag attributes
```java
List<XMLAttribute> attrs;
String tagName = "tag";
while (reader.hasNext()) {
    int event = reader.next();
    if (event == XMLReaderConstants.START_ELEMENT
            && reader.getLocalName().equals(tagName)) {
        attrs = reader.getAttributes();
        break;
    }
}
```

Get text
```java
String text;
while (reader.hasNext()) {
    int event = reader.next();
    if (event == XMLReaderConstants.CHARACTERS) {
        text = reader.getText();
        break;
    }
}
```

Get CData
```java
String cdata;
while (reader.hasNext()) {
    int event = reader.next();
    if (event == XMLReaderConstants.CDATA) {
        cdata = reader.getText();
        break;
    }
}
```

Get comment
```java
String comment;
reader.setIgnoreComments(false);
while (reader.hasNext()) {
    int event = reader.next();
    if (event == XMLReaderConstants.COMMENT) {
        comment = reader.getText();
        break;
    }
}
```

## License 
MIT License
