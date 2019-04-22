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
package interpreter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import utils.Utils;

public class SimpleInterpreter {
    
    //cells are from 0 to inv
    //cell-content is 1 byte (0- = 255 / 255+ = 0)
    
    public static final int clusterLenLog2 = 8;
    public static final int clusterLen = (1 << clusterLenLog2);
    public final List <byte []> clusters = new ArrayList<>();
    
    public void execute (String code) {
        int posCell = 0;
        int posCode = 0;
        
        int [] jmpToDst = new int [code.length()];
        for (int i=0; i<jmpToDst.length; i++) {
            posCode = i;
            char cmd = code.charAt(posCode);
            if (cmd == '[') {
                int openBrackets = 1;
                posCode++;
                for (; true; posCode++) {
                    char cmdHere = code.charAt(posCode);
                    if (cmdHere == '[')
                        openBrackets++;
                    else if (cmdHere == ']') {
                        openBrackets--;
                        if (openBrackets == 0)
                            break;
                    }
                }
            } else if (cmd == ']') {
                int openBrackets = -1;
                posCode--;
                for (; true; posCode--) {
                    char cmdHere = code.charAt(posCode);
                    if (cmdHere == '[') {
                        openBrackets++;
                        if (openBrackets == 0)
                            break;
                    } else if (cmdHere == ']')
                        openBrackets--;
                }
            }
            jmpToDst[i] = posCode;
        }
        
        posCode = 0;
        while (posCode != code.length()) {
            char cmd = code.charAt(posCode);
            if (cmd == '[') {
                if (getCell(posCell) == '\0')
                    posCode = jmpToDst[posCode];
                
                posCode++;
            } else if (cmd == ']') {
                if (getCell(posCell) != '\0')
                    posCode = jmpToDst[posCode];
                else
                    posCode++;
            } else {
                if (cmd == '<')
                    posCell--;
                else if (cmd == '>')
                    posCell++;
                else if (cmd == '+')
                    addCell(posCell, 1);
                else if (cmd == '-')
                    addCell(posCell, -1);
                else if (cmd == '.')
                    System.out.print((char)(getCell(posCell) & 0xFF));
                else {
                    int toRead = -1;
                    while (toRead == -1) {
                        try {
                            toRead = System.in.read();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        if (toRead == -1)
                            Utils.sleep(10);
                    }
                    setCell(posCell, (byte)toRead);
                }
                posCode++;
            }
        }
        System.out.println("");
    }
    
    public byte getCell (int position) {
        int clusterPos = (position >> clusterLenLog2);
        while (clusterPos >= clusters.size())
            clusters.add(new byte [clusterLen]);
        return clusters.get(clusterPos)[position%clusterLen];
    }
    
    public void addCell (int position, int toAdd) {
        int clusterPos = (position >> clusterLenLog2);
        while (clusterPos >= clusters.size())
            clusters.add(new byte [clusterLen]);
        clusters.get(clusterPos)[position%clusterLen] += toAdd;
    }
    
    public void setCell (int position, byte set) {
        int clusterPos = (position >> clusterLenLog2);
        while (clusterPos >= clusters.size())
            clusters.add(new byte [clusterLen]);
        clusters.get(clusterPos)[position%clusterLen] = set;
    }
    
}
