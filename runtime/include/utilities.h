#ifndef UTILITIES_H
#define UTILITIES_H

#include <structs.h>

Agent *allocateAgent(int PortCount, Queue* q);

intDataAgent *allocateIntAgent(unsigned int PortCount, int data, Queue* q);

floatDataAgent *allocateFloatAgent(unsigned int PortCount, float data, Queue* q);

Wire *CreateWire(Queue* queue ,atomic_int_fast8_t, Port *port1, Port *port2);

void IncreaseHotness(Queue *q, Wire *w);

void output(Port* portToStart);
#endif 