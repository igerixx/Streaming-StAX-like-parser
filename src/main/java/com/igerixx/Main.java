package com.igerixx;

import com.igerixx.Reader.XMLReader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class Main {
    public static void main(String[] args) throws IOException {
        InputStream is = new FileInputStream("C:\\Users\\igor\\Downloads\\1984 (1).fb2");

        XMLReader reader = new XMLReader(is);

        while (reader.hasNext()) {
            int event = reader.next();
            System.out.println(reader.getText());
        }
    }
}
