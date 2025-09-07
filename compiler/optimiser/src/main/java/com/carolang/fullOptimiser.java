package com.carolang;

import com.carolang.common.interaction_rules.ProgramBase;

public class fullOptimiser implements IOptimiser {
    //non static class as we will be able to initialise with different parameters
    @Override
    public ProgramBase Optimise(ProgramBase program)
    {
        program = Trimmer.TrimProgram(program);
        program = Inliner.inlineProgram( new InliningHeuristic(false, false, 2048f), program);
        program = Trimmer.TrimProgram(program);
        program = Inliner.inlineProgram( new InliningHeuristic(false, true, 256f), program);
        program = Trimmer.TrimProgram(program);
        program = Inliner.inlineProgram( new InliningHeuristic(true, true, 256f), program);
        program = Trimmer.TrimProgram(program);
        program = Inliner.inlineProgram( new InliningHeuristic(false, true, 1024f), program);
        program = Trimmer.TrimProgram(program);
        program = Inliner.inlineProgram( new InliningHeuristic(true, true, 512f), program);
        program = Trimmer.TrimProgram(program);
        return program;
    }
}
