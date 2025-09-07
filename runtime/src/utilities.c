#include "structs.h"
#include "memoryManager.h"
#include <stdlib.h>
#include <stdatomic.h>
#include <assert.h>
#include <stdbool.h>
#include "workStealingQueue.h"
#include <stdio.h>
#include <string.h>
Agent *allocateAgent(int auxPortCount, Queue* q)
{
    // TODO: have Caro compiler leave a hint for GCC about max aux port count
    Agent *allocatedMemory = (Agent *)mallocWrapper(sizeof(Agent) + (auxPortCount) * sizeof(Port), q);
    if (allocatedMemory == NULL)
    {
        exit(-1);
    }
    return allocatedMemory;
}

intDataAgent *allocateIntAgent(unsigned int auxPortCount, int value, Queue* q)
{
    intDataAgent *allocatedMemory = (intDataAgent *)mallocWrapper(sizeof(intDataAgent) + (auxPortCount) * sizeof(Port), q);
    if (allocatedMemory == NULL)
    {
        exit(-1);
    }
    allocatedMemory->data = value;
    return allocatedMemory; // So we allocate the data on the 'left' of the agent Type and the ports on the right but we always return a pointer to the agent type
}

floatDataAgent *allocateFloatAgent(unsigned int auxPortCount, float value, Queue* q)
{
    floatDataAgent *allocatedMemory = (floatDataAgent *)mallocWrapper(sizeof(floatDataAgent) + (auxPortCount) * sizeof(Port), q);
    if (allocatedMemory == NULL)
    {
        exit(-1);
    }
    allocatedMemory->data = value;
    return allocatedMemory;}

Wire *CreateWire(Queue *queue, char hotness, Port *port1, Port *port2)
{
    assert(hotness <= 2);

    Wire *w = mallocWrapper(sizeof(Wire), queue);
    if (w == NULL)
    {
        exit(-1);
    }

    if (hotness == 2)
    {
        Enqueue(queue, w);
    }
    else
    {
        atomic_init(&(w->hotness), hotness);
    }
    w->wireEnd1 = port1;
    w->wireEnd2 = port2;

    port1->pointingAt = w;
    port2->pointingAt = w;

    return w;
}

bool isFirst(Port* p)
{
    return (p -> pointingAt -> wireEnd1 == p);
}

void IncreaseHotness(Queue *q, Wire *w)
{
    atomic_int_fast8_t *current = &(w->hotness);
    if(*current == 1)
    {
        Enqueue(q, w);
        return;
    }
    int oldValue = atomic_fetch_add(current, 1);
    if (oldValue == 1)
    {
        Enqueue(q, w);
    }
}

void HandleLackOfRule(void)
{
#if DEBUG
    // Want a nice error and no UB if this happens while debugging
    printf("No rule for this combination of agents\n");
    exit(EXIT_FAILURE);
#else
    // On Optimised builds let compiler know this will never happen
    __builtin_unreachable();
#endif
}

Port *GetOtherPort(Port *p)
{
    if (isFirst(p))
    {
        return p->pointingAt->wireEnd2;
    }
    else
    {
        return p->pointingAt->wireEnd1;
    }
}

extern int NESTED_LIST_OUTPUT_DEPTH;

extern void OutputElement(int* agentType);

extern int PRINCIPLE_PORT_OFFSET;

extern int EMPTY_LIST_AGENT_TYPE;





static void outputHelper(Agent *firstAgent, int depth)
{
    if (depth == 0)
    {
        OutputElement(&(firstAgent -> type));
        freeWrapper(firstAgent);
    }
    else
    {
        fwrite("[", 1, 1, stdout);
        Agent *current = firstAgent;
        if (current-> type != EMPTY_LIST_AGENT_TYPE)
        {
            
            Port* currentAuxPort0 = &(current -> auxPorts[0]);
            Port* nextAgentInListPrinciplePort = GetOtherPort(currentAuxPort0);
            Port* currentAuxPort1 = &(current -> auxPorts[1]);
            Port* innerListPrinciplePort = GetOtherPort(currentAuxPort1);

            Agent* innerList = (Agent *)(((char *)innerListPrinciplePort) - PRINCIPLE_PORT_OFFSET);

            outputHelper(innerList, depth - 1);

            Agent* oldCurrent = current;
            current= (Agent *)(((char *)nextAgentInListPrinciplePort) - PRINCIPLE_PORT_OFFSET);
            freeWrapper(currentAuxPort0 -> pointingAt);
            freeWrapper(currentAuxPort1 -> pointingAt);
            freeWrapper(oldCurrent);
        }
        while (current-> type != EMPTY_LIST_AGENT_TYPE)
        {
            fwrite("; ", 1, strlen("; "), stdout);
            
            Port* currentAuxPort0 = &(current -> auxPorts[0]);
            Port* nextAgentInListPrinciplePort = GetOtherPort(currentAuxPort0);
            Port* currentAuxPort1 = &(current -> auxPorts[1]);
            Port* innerListPrinciplePort = GetOtherPort(currentAuxPort1);

            Agent* innerList = (Agent *)(((char *)innerListPrinciplePort) - PRINCIPLE_PORT_OFFSET);

            outputHelper(innerList, depth - 1);

            Agent* oldCurrent = current;
            current= (Agent *)(((char *)nextAgentInListPrinciplePort) - PRINCIPLE_PORT_OFFSET);
            freeWrapper(currentAuxPort0 -> pointingAt);
            freeWrapper(currentAuxPort1 -> pointingAt);
            freeWrapper(oldCurrent);
        }
        fwrite("]", 1, 1, stdout);
        freeWrapper(current);
    }
}

void output(Port* portToStart)
{
#ifndef DEBUG
    if (NESTED_LIST_OUTPUT_DEPTH > 0)
    {
        // Make a big buffer if we are going to print a list
        static char outbuf[1 << 16]; // 64 KB buffer
        setvbuf(stdout, outbuf, _IOFBF, sizeof(outbuf));
    }
#endif
//portToStart is a principle port 
Agent* firstAgent = (Agent*)(((char *)portToStart) - PRINCIPLE_PORT_OFFSET);
outputHelper(firstAgent , NESTED_LIST_OUTPUT_DEPTH);
}

