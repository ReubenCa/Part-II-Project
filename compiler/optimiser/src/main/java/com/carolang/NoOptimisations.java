package com.carolang;

import com.carolang.common.interaction_rules.ProgramBase;

public class NoOptimisations implements IOptimiser {
    @Override
    public ProgramBase Optimise(ProgramBase program)
    {
        program = Trimmer.TrimProgram(program);
        return program;
    }
}
