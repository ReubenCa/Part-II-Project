package com.carolang;

import com.carolang.common.interaction_rules.ProgramBase;

public class smallOptimiser implements IOptimiser {
    
    @Override
    public ProgramBase Optimise(ProgramBase program)
    {
        program = Trimmer.TrimProgram(program);
        program = Inliner.inlineProgram( new InliningHeuristic(false, false, 2048f), program);
        program = Trimmer.TrimProgram(program);
        program = Inliner.inlineProgram( new InliningHeuristic(true, false, 128f), program);
        program = Trimmer.TrimProgram(program);
        return program;
    }
    
}
