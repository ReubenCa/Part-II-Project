#ifndef GENERATEDCODE_H
#define GENERATEDCODE_H

#include <structs.h>
#include <workStealingQueue.h>


/**
 * Initialise sets up the starting net and returns the hot queue of reductions that can be made immediately
 */
extern Queue *Initialise(void);
extern void ApplyRule(Queue *queue, AgentType *agent1, AgentType *agent2);

#endif

