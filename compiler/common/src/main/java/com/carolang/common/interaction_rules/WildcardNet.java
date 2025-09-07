package com.carolang.common.interaction_rules;

/**
 * A wildcard is a net but where agents can be a 'hole' this is useful for being the rewrite result of a function application where we want to generate a rule that isn't actually effected by one of the agents
 * The other option would be to have nodes that can interact without being connected to anything
 * Might be the better option because how does the wildcard get duplicated?
 * Or actually maybe just dont have seperate inlining phase and represent every function with a net
 * But then let rec f = fun x -> (f, x) in f (First (fun a->a)) 1
 * This would be an infinitely big net if we didn't lazily expand nodes
 * And can make some less contrived examples - this one doesnt actually work in Ocmal but if we fully define our if statements and numbers with church numerals in lambda calculus then we can't rely on ifs being lazily evaluated.
 */
// public class WildcardNet extends Net {
    
// }
