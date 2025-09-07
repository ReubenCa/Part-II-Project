package com.carolang;

import com.carolang.common.interaction_rules.ProgramBase;

public interface IOptimiser {

    //non static class as we will be able to initialise with different parameters
    ProgramBase Optimise(ProgramBase program);

}