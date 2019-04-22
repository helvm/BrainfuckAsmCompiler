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
package assembler;

public class Optimizer {
    
    public static String optimizeSimple (String in, boolean beQuiet) {
        String ret = in;
        while (true) {
            String optimized = optimizeSimpleStep(ret);
            if (optimized == null)
                break;
            ret = optimized;
        }
        if (!beQuiet)
            System.out.println("Optimized to " + ret.length() + " ..     (" + (in.length()-ret.length()) + " / " + in.length() + ")");
        return ret;
    }
    
    public static String optimizeSimpleStep (String in) {
        boolean [] remove = new boolean [in.length()];
        boolean removed = false;
        for (int i=0; i<in.length()-1; i++) {
            if ((in.charAt(i) == '<' && in.charAt(i+1) == '>') || (in.charAt(i) == '>' && in.charAt(i+1) == '<') || (in.charAt(i) == '-' && in.charAt(i+1) == '+') || (in.charAt(i) == '+' && in.charAt(i+1) == '-')) {
                remove[i] = true;
                remove[i+1] = true;
                removed = true;
                i++;
            }
        }
        if (!removed)
            return null;
        else {
            StringBuilder ret = new StringBuilder();
            for (int i=0; i<remove.length; i++)
                if (!remove[i])
                    ret.append(in.charAt(i));
            return ret.toString();
        }
    }
    
}
