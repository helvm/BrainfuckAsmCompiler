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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ControlFlowSegments {
    
    public final CodeCreation codeCreation;
    public final List <Integer> offsetsSorted;
    public final Map <Integer, Integer> offset2SegmentIndex = new HashMap<>();
    
    public ControlFlowSegments (CodeCreation codeCreation) {
        this.codeCreation = codeCreation;
        final Parser parsed = codeCreation.parsed;
        
        Set <Integer> startPts = new HashSet<>();
        startPts.add(0);
        startPts.add(parsed.commands.length);
        for (int i=0; i<parsed.labelOffsets.length; i++)
            startPts.add(parsed.labelOffsets[i]);
        
        for (int i=0; i<parsed.commands.length; i++)
            if (parsed.commands[i].isJumpInstruction())
                startPts.add(i+1);
        
        List <Integer> startPtsSorted = new ArrayList<>();
        startPtsSorted.addAll(startPts);
        Collections.sort(startPtsSorted);
        
        offsetsSorted = startPtsSorted;
        for (int i=0; i<offsetsSorted.size(); i++)
            offset2SegmentIndex.put(offsetsSorted.get(i), i);
    }
    
    public int getSegmentIndex (int cmdOffset) {
        if (cmdOffset == codeCreation.parsed.commands.length)
            return offset2SegmentIndex.get(offsetsSorted.get(offsetsSorted.size()-1));
        else if (offset2SegmentIndex.containsKey(cmdOffset))
            return offset2SegmentIndex.get(cmdOffset);
        else
            throw new RuntimeException("unknown jump destination. (internal error)");
    }
    
    public String createMainLoop () {
        final MiniCode miniCode = codeCreation.miniCode;
        StringBuilder ret = new StringBuilder();
        ret.append(codeCreation.createGlobalInit()).append(miniCode.move(0, miniCode.getReservedMainLoopPosition())).append("+[[-]>[-<+>]<");
        for (int i=0; i<offsetsSorted.size(); i++) {
            final int fromIncl = offsetsSorted.get(i), toExcl = (i < offsetsSorted.size()-1 ? offsetsSorted.get(i+1) : codeCreation.parsed.commands.length);
            ret.append(miniCode.copyContent(miniCode.getReservedMainLoopPosition(), miniCode.getReservedMainLoopPosition(), miniCode.getReservedMainLoopPosition()+3, miniCode.getReservedMainLoopPosition()+2, miniCode.getReservedMainLoopPosition())).append(">>>+<[[-]>-<]>[-").append(miniCode.move(miniCode.getReservedMainLoopPosition()+3, 0)).append(codeCreation.create(this, fromIncl, toExcl)).append(miniCode.move(0, miniCode.getReservedMainLoopPosition())).append((i != offsetsSorted.size()-1 ? miniCode.constantAdd(offsetsSorted.size()+1) : "[-]")).append(">>>]<<<" + (i != offsetsSorted.size()-1 ? "-" : ""));
        }
        ret.append("]");
        return ret.toString();
    }
    
}
