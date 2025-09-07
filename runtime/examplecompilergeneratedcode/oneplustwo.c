#include "structs.h"
#include "workStealingQueue.h"
#include "utilities.h"
#include <stdatomic.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdbool.h>
#include <assert.h>

const static int ADD_AGENT = 1;
const static int INT_AGENT = 2;
const static int OUTPUT_AGENT = 3;
const static int CURRIED_PLUS = 4;


extern void IncreaseHotness(Queue *q, Wire *w);

//(+) 1 2
Queue *Initialise(void)
{
    Agent *addAgent = allocateAgent(2);
    addAgent -> type = ADD_AGENT;
    Port *addAgentPrinciplePort = &(addAgent -> principlePort);

    
    Port *addAgentAuxPort1 = &(addAgent -> auxPorts[0]);
    Port *addAgentAuxPort2 = &(addAgent -> auxPorts[1]);

    intDataAgent *oneAgent = allocateIntAgent(0, 1);
    oneAgent->type = INT_AGENT;
    Port *oneAgentPrinciplePort = &(oneAgent -> principlePort);

    intDataAgent *twoAgent = allocateIntAgent(0, 2);
    twoAgent-> type = INT_AGENT;
    Port *twoAgentPrinciplePort = &(twoAgent -> principlePort);

    Agent *outputAgent = allocateAgent(0);
    outputAgent-> type = OUTPUT_AGENT;
    Port *outputAgentPrinciplePort = &(outputAgent -> principlePort);

    Wire *oneToPlusWire = CreateWire(2, oneAgentPrinciplePort, addAgentPrinciplePort);

    Wire *plusToTwoWire = CreateWire(1, addAgentAuxPort1, twoAgentPrinciplePort);

    Wire *plusToOutputWire = CreateWire(1, addAgentAuxPort2, outputAgentPrinciplePort);


    Queue *startingQueue = createQueue();
    Enqueue(startingQueue, oneToPlusWire);
    return startingQueue;
}



void plusIntRule(Queue *queue, Agent *addAgent, intDataAgent *intAgent)
{
    int data = intAgent -> data;
    intDataAgent *curriedAddAgent = allocateIntAgent(1, data);
    
    Port *curriedAddAgentPrinciplePort = &(curriedAddAgent -> principlePort);
    curriedAddAgent-> type = CURRIED_PLUS;

    // Curried Agent Principle Port points at start of wire which aux port 1 of add was attached to
    // And increase wire's hotness (with a CAS) adding to queue if necessary

    Port *addAuxPort1 = &(addAgent -> auxPorts[0]);
    Wire *addAuxPort1Wire = addAuxPort1->pointingAt;
    
    //So set Principle port of Curried agent to point at this wire
    curriedAddAgentPrinciplePort->pointingAt = addAuxPort1Wire;
    curriedAddAgentPrinciplePort->isFirst = addAuxPort1->isFirst;
    
    if(addAuxPort1->isFirst)
    {
        addAuxPort1Wire -> wireEnd1 = curriedAddAgentPrinciplePort;
    }
    else
    {
        addAuxPort1Wire -> wireEnd2 = curriedAddAgentPrinciplePort;
    }


    IncreaseHotness(queue, addAuxPort1Wire);//TODO: is this assuming sequential consistency? What if the writes above aren't visible by the time the wire made hot?


    // Curried Agent Aux Port 1 now needs to point at where the aux port 2 of the plus agent was
    Port *addAuxPort2 = &(addAgent -> auxPorts[1]);
    Wire *addAuxPort2Wire = addAuxPort2->pointingAt;

    Port *curriedAddAgentAuxPort1 =  &(curriedAddAgent -> auxPorts[0]);
    curriedAddAgentAuxPort1->pointingAt = addAuxPort2Wire;
    curriedAddAgentAuxPort1->isFirst = addAuxPort2->isFirst;

    if(addAuxPort2->isFirst)
    {
        addAuxPort2Wire -> wireEnd1 = curriedAddAgentAuxPort1;
    }
    else
    {
        addAuxPort2Wire -> wireEnd2 = curriedAddAgentAuxPort1;
    }

    //IncreaseHotness(queue, addAuxPort2Wire);


    free(addAgent -> principlePort.pointingAt);
    free(addAgent);
    free(intAgent);
    
}

void outputIntRule(Queue *queue, Agent *outputAgent, intDataAgent *intAgent)
{
    int data = intAgent -> data;
    printf("\"%d\"\n", data);

    free(outputAgent -> principlePort.pointingAt);
    free(outputAgent);
    free(intAgent);
}

void curriedAddrule(Queue *queue, intDataAgent *curriedAddAgent, intDataAgent *intAgent)
{
    int intData = intAgent -> data;
    int curriedData = curriedAddAgent -> data;
    int sum = intData + curriedData;
    intDataAgent *newIntAgent = allocateIntAgent(0, sum);
    newIntAgent->type = INT_AGENT;
    Port *newIntAgentPrinciplePort = &(newIntAgent -> principlePort);

    Port *curriedAddAgentAuxPort1 = &(curriedAddAgent -> auxPorts[0]);
    Wire *curriedAddAgentAuxPort1Wire = curriedAddAgentAuxPort1->pointingAt;
    if(curriedAddAgentAuxPort1->isFirst)
    {
        curriedAddAgentAuxPort1Wire -> wireEnd1 = newIntAgentPrinciplePort;
    }
    else
    {
        curriedAddAgentAuxPort1Wire -> wireEnd2 = newIntAgentPrinciplePort;
    }
    newIntAgentPrinciplePort->pointingAt = curriedAddAgentAuxPort1Wire;
    newIntAgentPrinciplePort->isFirst = curriedAddAgentAuxPort1->isFirst;

    IncreaseHotness(queue, curriedAddAgentAuxPort1Wire);


    free(intAgent -> principlePort.pointingAt);
    free(curriedAddAgent);
    free(intAgent);

}


void ApplyRule(Queue *queue, int *agent1Type, int *agent2Type)
{
    if (*agent1Type == ADD_AGENT && *agent2Type == INT_AGENT)
    {
        plusIntRule(queue, (Agent*)agent1Type, (intDataAgent*)agent2Type);

    }
    else if (*agent1Type == INT_AGENT && *agent2Type == ADD_AGENT)
    {
        plusIntRule(queue, (Agent*)agent2Type, (intDataAgent*)agent1Type);
    }
    else if (*agent1Type == OUTPUT_AGENT && *agent2Type == INT_AGENT)
    {
        outputIntRule(queue, (Agent*)agent1Type, (intDataAgent*)agent2Type);
    }
    else if (*agent1Type == INT_AGENT && *agent2Type == OUTPUT_AGENT)
    {
        outputIntRule(queue, (Agent*)agent2Type, (intDataAgent*)agent1Type);
    }
    else if (*agent1Type == CURRIED_PLUS && *agent2Type == INT_AGENT)
    {
        curriedAddrule(queue, (intDataAgent*)agent1Type, (intDataAgent*)agent2Type);
    }
    else if (*agent1Type == INT_AGENT && *agent2Type == CURRIED_PLUS)
    {
        curriedAddrule(queue, (intDataAgent*)agent2Type, (intDataAgent*)agent1Type);
    }
    else
    {
        printf("Error: Rule not found\n");
    }
}