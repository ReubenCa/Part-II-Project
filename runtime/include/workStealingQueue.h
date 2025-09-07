#ifndef WORKSTEALINGQUEUE_H
#define WORKSTEALINGQUEUE_H


#include <structs.h>



typedef struct Queue Queue;

Wire *Dequeue(Queue *q, int threadID);

void Enqueue(Queue *q, Wire *w);

Queue *createQueue(void);

void freeQueue(Queue* q);

void setQueueToStealFrom(Queue *q, Queue *queueToStealFrom);

#if QUEUE_LOGGING
void PrintQueue(Queue *q, int ThreadID);

void PrintQueueParameters(void);
#endif

#if RECORD_TOTAL_STEALS
long getTotalSteals(Queue *q);
#endif

void outputAllLogs(Queue *q, int threadID);

#if EXPERIMENTAL_MEMORY_MANAGER
char *getNextFreeMemory(Queue *q);

void setNextFreeMemory(Queue *q, char *nextFreeMemory);
#endif 

#endif


