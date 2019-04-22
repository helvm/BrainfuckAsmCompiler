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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Utils {
 
    public static void sleep (int msecs) {
        try {
            Thread.sleep(msecs);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T [] copyArray (T [] in) {
        return Arrays.copyOf(in, in.length);
    }

    public static boolean [] copyArray (boolean [] in) {
        return Arrays.copyOf(in, in.length);
    }

    public static byte [] copyArray (byte [] in) {
        return Arrays.copyOf(in, in.length);
    }

    public static char [] copyArray (char [] in) {
        return Arrays.copyOf(in, in.length);
    }

    public static double [] copyArray (double [] in) {
        return Arrays.copyOf(in, in.length);
    }

    public static float [] copyArray (float [] in) {
        return Arrays.copyOf(in, in.length);
    }

    public static int [] copyArray (int [] in) {
        return Arrays.copyOf(in, in.length);
    }

    public static long [] copyArray (long [] in) {
        return Arrays.copyOf(in, in.length);
    }

    public static short [] copyArray (short [] in) {
        return Arrays.copyOf(in, in.length);
    }
    
    public static float [] list2ArrayFloat (List <Float> in) {
        float [] ret = new float[in.size()];
        for (int i=0; i<ret.length; i++)
            ret[i] = in.get(i);
        return ret;
    }

    public static int [] list2ArrayInt (List <Integer> in) {
        int [] ret = new int[in.size()];
        for (int i=0; i<ret.length; i++)
            ret[i] = in.get(i);
        return ret;
    }
    
    public static byte [] list2ArrayByte (List <Byte> in) {
        byte [] ret = new byte[in.size()];
        for (int i=0; i<ret.length; i++)
            ret[i] = in.get(i);
        return ret;
    }
    
    public static float [] set2ArrayFloat (Set <Float> in) {
        float [] ret = new float[in.size()];
        int i = 0;
        for (float toAdd : in)
            ret[i++] = toAdd;
        return ret;
    }

    public static int [] set2ArrayInt (Set <Integer> in) {
        int [] ret = new int[in.size()];
        int i = 0;
        for (int toAdd : in)
            ret[i++] = toAdd;
        return ret;
    }
    
}
