#include <stdlib.h>
#include <workStealingQueue.h>
#include <stdio.h>
#define max(a,b) ((a) > (b) ? (a) : (b))
static int threadMemorySize = (int)((long)20e9 / (long)NUMBER_OF_THREADS);

void freeWrapper(void* ptr)
{
    #if EXPERIMENTAL_MEMORY_MANAGER
    return;
    #else 
    free(ptr);
    #endif
}

void* mallocWrapper(int size, Queue* q)
{
    #if EXPERIMENTAL_MEMORY_MANAGER
    char *ptr = getNextFreeMemory(q);
    char* newNextFreeMemory = ptr + size;
    setNextFreeMemory(q, newNextFreeMemory);
    return ptr;

    #else 
    void* ptr = malloc(size);
    return ptr;
    #endif
}

#if EXPERIMENTAL_MEMORY_MANAGER
char *initialiseNextFreeMemory()
{

    char *ptr = malloc(max(threadMemorySize, 2e9));
    if (ptr == NULL)
    {
        printf("Experimental memory allocation failed\n");
        fflush(stdout);
        exit(-1);
    }
    return ptr;
}
#endif