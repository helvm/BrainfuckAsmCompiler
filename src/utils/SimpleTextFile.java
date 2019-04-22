/*
 * The MIT License
 *
 * Copyright 2019 Hilmar Ackermann.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimpleTextFile {
    
    public final String path;
    public final List <String> lines = new ArrayList<>();
    
    public SimpleTextFile () {
        path = null;
    }
    
    public SimpleTextFile (String _path) {
        path = _path;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(path)));
            String nextLine = null;
            while ((nextLine = reader.readLine()) != null)
                lines.add(nextLine);
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void write (String path) {
        try {
            FileWriter fw = new FileWriter(new File(path));
            fw.write(get());
            fw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public String get () {
        StringBuilder builder = new StringBuilder();
        for (int i=0; i<lines.size(); i++)
            builder.append((i == 0 ? "" : "\n")).append(lines.get(i));
        return builder.toString();
    }
    
}
