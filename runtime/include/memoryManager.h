#ifndef MEMORYMANAGER_H
#define MEMORYMANAGER_H
#include <workStealingQueue.h>

void freeWrapper(void* ptr);

void* mallocWrapper(int size, Queue* q);

#if EXPERIMENTAL_MEMORY_MANAGER
char *initialiseNextFreeMemory();
#endif

#endif

