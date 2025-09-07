#include "structs.h"
#include "workStealingQueue.h"
#include <stdlib.h>
#include <stdlib.h>
#include <stdatomic.h>
#include <stdbool.h>
#include <assert.h>
#include <pthread.h>
#if QUEUE_LOGGING
#include <stdio.h>
#elif  RECORD_NEED_TIMER
#include <stdio.h>
#elif DEBUG
#include <stdio.h>
#endif
#if RECORD_NEED_TIMER
#include <time.h>
#endif
#include <memoryManager.h>

extern int NUMBER_OF_THREADS_;
extern atomic_int_fast8_t *stalledQueuesCounter;

int INITIAL_BUFFER_SIZE_ = INITIAL_BUFFER_SIZE;

int MIN_QUEUE_SIZE_TO_ATTEMPT_STEAL_ = MIN_QUEUE_SIZE_TO_ATTEMPT_STEAL;
int MIN_QUEUE_SIZE_TO_ACTUALLY_STEAL_ = MIN_QUEUE_SIZE_TO_ACTUALLY_STEAL;
int MAX_ITEMS_TO_STEAL_ = MAX_ITEMS_TO_STEAL;
int FRACTION_TO_STEAL_ = FRACTION_TO_STEAL;

#if DEBUG
void PrintQueueParameters(void)
{
    printf("INITIAL_BUFFER_SIZE_ = %d\n", INITIAL_BUFFER_SIZE_);
    printf("MIN_QUEUE_SIZE_TO_ATTEMPT_STEAL_ = %d\n", MIN_QUEUE_SIZE_TO_ATTEMPT_STEAL_);
    printf("MIN_QUEUE_SIZE_TO_ACTUALLY_STEAL_ = %d\n", MIN_QUEUE_SIZE_TO_ACTUALLY_STEAL_);
    printf("MAX_ITEMS_TO_STEAL_ = %d\n", MAX_ITEMS_TO_STEAL_);
    printf("FRACTION_TO_STEAL_ = %d\n", FRACTION_TO_STEAL_);
}

#endif

typedef struct Queue
{
    Wire **buffer_start;
    int buffer_capacity;
    Queue *queueToStealFrom;
    int topOffset;
    int bottomOffset;
    int minTopOffset;
    pthread_mutex_t resizeMutex;
    pthread_mutex_t minTopOffsetMutex;
#if RECORD_TOTAL_STEALS
    long totalSteals;
#endif
#if RECORD_THREAD_STALLS
    struct timespec *threadStallsLogTimings;
    int *threadStallsLogData;
    int threadStallsLogIndex;
#endif
#if RECORD_QUEUE_SIZE
    struct timespec *QueueSizeLogTimings;
    int *QueueSizeLogData;
    int QueueSizeLogIndex;
#endif
#if EXPERIMENTAL_MEMORY_MANAGER
    char *nextFreeMemory;
#endif
} Queue;

#if EXPERIMENTAL_MEMORY_MANAGER
char *getNextFreeMemory(Queue *q)
{
    return q -> nextFreeMemory;
}

void setNextFreeMemory(Queue *q, char *nextFreeMemory)
{
    q->nextFreeMemory = nextFreeMemory;
}
#endif

static int LOG_LIMIT = 1e9;
static int QUEUE_SIZE_LOG_FREQ = 1;



#if RECORD_TOTAL_STEALS
long getTotalSteals(Queue *q)
{
    return q->totalSteals;
}
#endif

void resizeBuffer(Queue *q);
int queueSize(Queue *q);
bool Steal(Queue *queueDoingStealing, Queue *queueBeingStolenFrom, int threadID);

void LogStalledThreads(Queue *q, int value)
{
#if RECORD_THREAD_STALLS
    fflush(stdout);
    int index = q->threadStallsLogIndex;
    clock_gettime(CLOCK_PROCESS_CPUTIME_ID, &(q->threadStallsLogTimings[index]));
    q->threadStallsLogData[index] = value;
    q->threadStallsLogIndex++;
    if(index>=LOG_LIMIT)
    {
        printf("LOG_LIMIT EXCEEDED");
        fflush(stdout);
        exit(-1);
    }
#endif 
}

void LogQueueSize(Queue *q)
{
#if RECORD_QUEUE_SIZE
    int index = q->QueueSizeLogIndex;
    q->QueueSizeLogIndex++;
    if(index % QUEUE_SIZE_LOG_FREQ != 0)
    {
        return;
    }
    clock_gettime(CLOCK_PROCESS_CPUTIME_ID, &(q->QueueSizeLogTimings[index/QUEUE_SIZE_LOG_FREQ]));
    q->QueueSizeLogData[index] = queueSize(q);
    if(index>=LOG_LIMIT)
    {
        printf("LOG_LIMIT EXCEEDED");
        fflush(stdout);
        exit(-1);
    }
#endif
}


Wire *Dequeue(Queue *q, int threadID)
{

    if (queueSize(q) == 0)
    {
        // EMPTY
        if (!Steal(q, q->queueToStealFrom, threadID))
        {
            return NULL; // Queue is empty and nothing to steal - program is over
        }
    }
    Wire *wireToReturn = *(q->buffer_start + q->topOffset);
    int newTopOffset = q->topOffset - 1;
    if (newTopOffset < 0)
    {
        newTopOffset = q->buffer_capacity - 1;
    }
    // TODO: do we need to do something if minTopOffset has changed when we finally get it
    // Ie the reason we have the lock is to stop us dequeuing anything under minTopOffset if a steal is in progress and has locked it

    if (q->topOffset == q->minTopOffset)
    {
        if (pthread_mutex_trylock(&(q->minTopOffsetMutex)))
        {
            if (q->topOffset == q->minTopOffset)
            {
                q->minTopOffset = newTopOffset;
            }
            pthread_mutex_unlock(&(q->minTopOffsetMutex));
        }
        else
        {
            return Dequeue(q, threadID);
        }
    }
    q->topOffset = newTopOffset;
#if QUEUE_LOGGING
    printf("Dequeuing address: %p \t T%d\n", (void *)wireToReturn, threadID);
    printf("New Top Offset: %d \t new Min top Offset %d \t T%d\n", q->topOffset, q->minTopOffset, threadID);
#endif
    LogQueueSize(q);
    return wireToReturn;
}

void Enqueue(Queue *q, Wire *w)
{

#if QUEUE_LOGGING
    printf("Enqueuing address: %p\n", (void *)w);
#endif
    if (queueSize(q) == q->buffer_capacity - 1)
    {
        // FULL
        resizeBuffer(q);
    }

    int newTopOffset = q->topOffset + 1;
    if (newTopOffset >= q->buffer_capacity)
    {
        assert(newTopOffset == q->buffer_capacity);
        newTopOffset = 0;
    }
    Wire **placeToInsertNewWire = (q->buffer_start) + newTopOffset;
    *placeToInsertNewWire = w;
    q->topOffset = newTopOffset;
    LogQueueSize(q);
}

Queue *createQueue(void)
{
    Queue *q = malloc(sizeof(Queue));
    if (q == NULL)
    {
        exit(-1);
    }
    q->buffer_start = malloc(sizeof(Wire *) * INITIAL_BUFFER_SIZE_);
    q->topOffset = 0;
    q->bottomOffset = 0; // to be able to use circular buffer we always need one empty cell
    q->minTopOffset = 0;
    q->buffer_capacity = INITIAL_BUFFER_SIZE_;
    pthread_mutex_init(&(q->resizeMutex), NULL);
    pthread_mutex_init(&(q->minTopOffsetMutex), NULL);
    if (q->buffer_start == NULL)
    {
        exit(-1);
    }
#if RECORD_TOTAL_STEALS
    q->totalSteals = 0;
#endif
#if RECORD_THREAD_STALLS
    q->threadStallsLogTimings = malloc(sizeof(struct timespec) * LOG_LIMIT);
    q->threadStallsLogData = malloc(sizeof(int) * LOG_LIMIT);
    q->threadStallsLogIndex = 0;
    if (q->threadStallsLogTimings == NULL || q->threadStallsLogData == NULL)
    {
        exit(-1);
    }
#endif
#if RECORD_QUEUE_SIZE
    q->QueueSizeLogTimings =  malloc(sizeof(struct timespec) * LOG_LIMIT);
    q->QueueSizeLogData = malloc(sizeof(int) * LOG_LIMIT);
    q->QueueSizeLogIndex = 0;
    if (q->QueueSizeLogTimings == NULL || q->QueueSizeLogData == NULL)
    {
        exit(-1);
    }
#endif
#if EXPERIMENTAL_MEMORY_MANAGER
    q->nextFreeMemory = initialiseNextFreeMemory();
#endif
    return q;
}

void setQueueToStealFrom(Queue *q, Queue *queueToStealFrom)
{
    q->queueToStealFrom = queueToStealFrom;
}

void resizeBuffer(Queue *q)
{

    int newBufferCapacity = q->buffer_capacity * 2;
#if QUEUE_LOGGING
    printf("Resizing to %d\n", newBufferCapacity);
#endif
    Wire **newBuffer = malloc(sizeof(Wire *) * newBufferCapacity);
    if (newBuffer == NULL)
    {
        exit(-1);
    }
    pthread_mutex_lock(&(q->resizeMutex));
#if QUEUE_LOGGING
    PrintQueue(q, -1);
#endif
    Wire **oldBuffer = q->buffer_start;
    int index = q->topOffset;
    int bottomOffset = q->bottomOffset;
    Wire **nextToAddInNewBuffer = newBuffer + 1; // Leave 0th one empty as it is where bottom is pointing
    // Start at top and copy until we reach bottom
    while (index != bottomOffset)
    {
        *nextToAddInNewBuffer = *(oldBuffer + index);
#if QUEUE_LOGGING
        printf("Moving %p \n", (void *)*(oldBuffer + index));
        fflush(stdout);
#endif
        nextToAddInNewBuffer++;
        index--;
        if (index < 0)
        {
            index = q->buffer_capacity - 1;
        }
    }

    int newTopOffset = queueSize(q);
    q->topOffset = newTopOffset;
    q->bottomOffset = 0;
    q->minTopOffset = 0; // Safe a steal can't be taking place if we are resizing;

    q->buffer_start = newBuffer;
    q->buffer_capacity = newBufferCapacity;
#if QUEUE_LOGGING
    PrintQueue(q, -1);
#endif
    pthread_mutex_unlock(&(q->resizeMutex));
    free(oldBuffer);
}

int queueSize(Queue *q)
{
    if (q->topOffset >= q->bottomOffset)
    {
        return q->topOffset - q->bottomOffset;
    }
    else
    {
        return (q->buffer_capacity - q->bottomOffset) + q->topOffset;
    }
}

void freeQueue(Queue *q)
{
    free(q->buffer_start);
    free(q);
}

/// @brief
/// @param queueDoingStealing
/// @param queueBeingStolenFrom
/// @return Returns true if we stole something - false means there was nothing to steal and therefore the program needs to terminate
bool Steal(Queue *queueDoingStealing, Queue *queueBeingStolenFrom, int threadID)
{

#if QUEUE_LOGGING
    printf("Queueing Being Stolen From: ");

    // PrintQueue(queueBeingStolenFrom, threadID);
    // Printing a Queue you don't own is a bad idea - it can get messed around and puts junk data in the log which makes it look like a real bug
    printf("Queue Doing Stealing: ");
    PrintQueue(queueDoingStealing, threadID);
    fflush(stdout);
#endif
    if (queueDoingStealing == queueBeingStolenFrom)
    {
        return false; // case where we only have one thread
    }
    // The first pass through the loop ignores the stalled threads counter
    bool first = true;
    bool StalledQueuesCounterSet = false;

    while (true)
    {
        if (!first)
        {
            StalledQueuesCounterSet = true;
            int v = atomic_fetch_add(stalledQueuesCounter, 1);
            LogStalledThreads(queueDoingStealing, v);
        }
        first = false;
#if QUEUE_LOGGING
        printf("Incremented stalled Counter to %d\n", *stalledQueuesCounter);
        fflush(stdout);
#endif

        // first = false;
        int queueToStealSize;
        bool firstInInnerLoop = true;
        while (true)
        {
            if (!firstInInnerLoop && !StalledQueuesCounterSet)
            {
                StalledQueuesCounterSet = true;
                int v = atomic_fetch_add(stalledQueuesCounter, 1);
                LogStalledThreads(queueDoingStealing, v);
            }
            firstInInnerLoop = false;

            if (*stalledQueuesCounter == NUMBER_OF_THREADS_)
            {
                return false; // All threads are looking to steal so we must be out of work
            }

            queueToStealSize = queueSize(queueBeingStolenFrom);
            if (queueToStealSize < MIN_QUEUE_SIZE_TO_ATTEMPT_STEAL_)
            {
                continue; // Spin until something to steal
                // crucially we spin with this heuristic of being empty before touching (decrementing) our stalled threads counter
            }
            if (!pthread_mutex_trylock(&(queueBeingStolenFrom->resizeMutex)))
            {
                // Do this first as we expect it to be very rare that this conflicts with anything
                continue;
            }
            break;
        }
        int amountToSteal = queueToStealSize > MAX_ITEMS_TO_STEAL_ * FRACTION_TO_STEAL_ ? MAX_ITEMS_TO_STEAL_ : (queueToStealSize + FRACTION_TO_STEAL_ - 1) / FRACTION_TO_STEAL_;
        while (amountToSteal > queueDoingStealing->buffer_capacity - 1)
        {
            resizeBuffer(queueDoingStealing); // edge case where buffer capacity is too small
        }

        // if (!first)
        // {
        if (StalledQueuesCounterSet)
        {
            int v = atomic_fetch_add(stalledQueuesCounter, -1);
            StalledQueuesCounterSet = false;
            LogStalledThreads(queueDoingStealing, v);
        }
#if QUEUE_LOGGING
        printf("Decremented stalled Counter to %d\n", *stalledQueuesCounter);
        fflush(stdout);
#endif
        //  }
        // We now copy across the items we want to steal
        // We now need to update the stolen queues bottom IFF the minTopValue is greater than the new bottom
        // To do this we:
        // Read Bottom
        // Read MinimumTop and assert it is below
        // Then try and CAS bottom to the new bottom
        // Repeatedly until the CAS succeeds (maybe with a fail timeout to abort and retry the entire steal operation)
        int stolenFromQueueBottom = queueBeingStolenFrom->bottomOffset;

        int newMinTopOffset = amountToSteal + stolenFromQueueBottom;
        if (newMinTopOffset >= queueBeingStolenFrom->buffer_capacity)
        {
            newMinTopOffset = newMinTopOffset - queueBeingStolenFrom->buffer_capacity;
        }

        int newQueueDoingStealingTop = queueDoingStealing->topOffset;
        // TODO: probably can be weaker than needing full locking here
        // The actual guarantee we need is that any Dequeues by the local thread that happen after we have read the top/bottom info see this minTopOffset

        pthread_mutex_lock(&(queueBeingStolenFrom->minTopOffsetMutex));
        queueBeingStolenFrom->minTopOffset = newMinTopOffset;
        pthread_mutex_unlock(&(queueBeingStolenFrom->minTopOffsetMutex));

        //   atomic_store_explicit(&queueBeingStolenFrom->minTopOffset, newMinTopOffset, memory_order_release);

        for (int i = 0; i < amountToSteal; i++)
        {
            int indexStealingFrom = stolenFromQueueBottom + i + 1;
            if (indexStealingFrom >= queueBeingStolenFrom->buffer_capacity)
            {
                indexStealingFrom = indexStealingFrom - queueBeingStolenFrom->buffer_capacity;
            }
            Wire *wireToSteal = *(queueBeingStolenFrom->buffer_start + indexStealingFrom);
#if QUEUE_LOGGING
            printf("Stealing %p T%d\n", (void *)wireToSteal, threadID);
#endif
            // So we know queue doing stealing is empty
            // We also know it's buffer is large enough to hold all the wires
            newQueueDoingStealingTop = newQueueDoingStealingTop + 1;
            if (newQueueDoingStealingTop >= queueDoingStealing->buffer_capacity)
            {
                newQueueDoingStealingTop = 0;
            }
            Wire **placeToInsertNewWire = (queueDoingStealing->buffer_start) + newQueueDoingStealingTop;
            *placeToInsertNewWire = wireToSteal;
        }
        // Now we have copied items across - to confirm we need to:
        // Ensure that bottom is below minTop
        // CAS

        // We should be able to know if steal is valid from bottom and mintop offset alone
        // Instead Steal is valid if distance from BOTTOM to MIN TOP is greater than amount we want to steal

        int stolenFromQueueMinTop = queueBeingStolenFrom->minTopOffset;
        int DistanceFromBottomToMinTop = stolenFromQueueMinTop - stolenFromQueueBottom;
        if (DistanceFromBottomToMinTop < 0)
        {
            DistanceFromBottomToMinTop = queueBeingStolenFrom->buffer_capacity + DistanceFromBottomToMinTop;
        }

        if (DistanceFromBottomToMinTop < MIN_QUEUE_SIZE_TO_ACTUALLY_STEAL_)
        {
            continue;
        }

        pthread_mutex_lock(&(queueBeingStolenFrom->minTopOffsetMutex));
        stolenFromQueueMinTop = queueBeingStolenFrom->minTopOffset;
        DistanceFromBottomToMinTop = stolenFromQueueMinTop - stolenFromQueueBottom;
        if (DistanceFromBottomToMinTop < 0)
        {
            DistanceFromBottomToMinTop = queueBeingStolenFrom->buffer_capacity + DistanceFromBottomToMinTop;
        }

        if (DistanceFromBottomToMinTop < MIN_QUEUE_SIZE_TO_ACTUALLY_STEAL_)
        {
            pthread_mutex_unlock(&(queueBeingStolenFrom->minTopOffsetMutex));
            continue;
        }

#if QUEUE_LOGGING
        for (int i = 0; i < amountToSteal; i++)
        {
            int indexStealingFrom = stolenFromQueueBottom + i + 1;
            if (indexStealingFrom >= queueBeingStolenFrom->buffer_capacity)
            {
                indexStealingFrom = indexStealingFrom - queueBeingStolenFrom->buffer_capacity;
            }
            assert(*(queueBeingStolenFrom->buffer_start + indexStealingFrom) != (Wire *)0xbebebebebebebebe);
        }
#endif

        // MinTopOffset now can't change - we have lock
        // Steal is also valid
        // So now just need to update bottom
        int newBottomOffset = stolenFromQueueMinTop;
        queueBeingStolenFrom->bottomOffset = newBottomOffset;
        queueBeingStolenFrom->minTopOffset = stolenFromQueueBottom;
        pthread_mutex_unlock(&(queueBeingStolenFrom->minTopOffsetMutex));
        pthread_mutex_unlock(&(queueBeingStolenFrom->resizeMutex));
        queueDoingStealing->topOffset = newQueueDoingStealingTop;
#if RECORD_TOTAL_STEALS
        queueDoingStealing->totalSteals++;
#endif
#if QUEUE_LOGGING
        printf("Steal Success\t T%d\n", threadID);
        printf("Post Steal Queueing Being Stolen From: ");
        // See above on why not to print this
        // PrintQueue(queueBeingStolenFrom, threadID);
        printf("Post Steal Queue Doing Stealing: ");
        PrintQueue(queueDoingStealing, threadID);
        fflush(stdout);
#endif
        return true;
    }
}

#if QUEUE_LOGGING
void PrintQueue(Queue *q, int threadID)
{
    printf("!!Queue contents: ");
    int offset = q->bottomOffset;
    while (offset != q->topOffset)
    {
        offset++;
        if (offset >= q->buffer_capacity)
        {
            offset = 0;
        }
        Wire *wire = *(q->buffer_start + offset);
        printf("[%p] (T%d), ", (void *)wire, threadID);
    }
    printf("T%d!!\n", threadID);
    fflush(stdout);
}
#endif

void outputAllLogs(Queue *q, int threadID)
{
    #if RECORD_THREAD_STALLS
    for(int i = 0; i < q -> threadStallsLogIndex; i++)
    {
        struct timespec ts = q -> threadStallsLogTimings[i];
        long long total_nanoseconds = (((long long)ts.tv_sec) * 1000000000LL) + ts.tv_nsec;
        printf("TS(%lld, %d)",  total_nanoseconds, q -> threadStallsLogData[i]);
    }
    #endif

    #if RECORD_QUEUE_SIZE
    for(int i = 0; i < q -> QueueSizeLogIndex/QUEUE_SIZE_LOG_FREQ; i++)
    {
        struct timespec ts = q -> QueueSizeLogTimings[i];
        long long total_nanoseconds = (((long long)ts.tv_sec) * 1000000000LL) + ts.tv_nsec;
        printf("QS%d.%lld,%d", threadID, total_nanoseconds, q -> QueueSizeLogData[i]);
    }
    #endif
}