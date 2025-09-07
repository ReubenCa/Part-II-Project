#include <stdlib.h>
#include "structs.h"
#include "workStealingQueue.h"
#include <assert.h>
#include <stddef.h>
#include <utilities.h>
#include <pthread.h>
#include <stdio.h>


extern void INITIALISATION_RULE(Queue *queue, Agent *alwaysNull1, Agent *alwaysNull2);
extern void ApplyRule(Queue *queue, Agent *agent1, Agent *agent2);
extern void ReadInputs(void);

extern Agent *allocateAgent(int auxPorts, Queue *q);
const int PRINCIPLE_PORT_OFFSET = offsetof(Agent, principlePort);
extern Port *OUTPUT_PORT;

atomic_int_fast8_t *stalledQueuesCounter;
int NUMBER_OF_THREADS_ = NUMBER_OF_THREADS;

static Queue **allQueues;

void *threadStart(void *arg)
{
    int id = *((int *)(arg));
    Queue *q = allQueues[id];
    Wire *next = NULL;


#if RECORD_WORK_PER_THREAD
    long total_reductions = 0;
#endif
    while (1)
    {
#if QUEUE_LOGGING
        PrintQueue(q, id);
#endif
        next = Dequeue(q, id);
        if (next == NULL)
        {
#if DEBUG
            printf("Thread T%d Exiting\n", id);
            fflush(stdout);
#endif
#if RECORD_WORK_PER_THREAD
            printf("Thread T%d completed %ld reductions\n", id, total_reductions);
#endif
#if RECORD_TOTAL_STEALS
            printf("Thread T%d completed %ld steals\n", id, getTotalSteals(q));
#endif
            free(arg);
            // freeQueue(q);
            // Dont free queues - since threads might still be stealing (ie checking size of queue and havent realised we are finished)
            return 0;
        }
#if RECORD_WORK_PER_THREAD
        total_reductions++;
#endif

        // assert(next->hotness == 2);//We can only interact hot wires
        // This is not actually true - if starting net is outputting a wire that is already hot it doesn't bother setting the hotness in the struct
        Port *port1Ptr = next->wireEnd1;
        Port *port2Ptr = next->wireEnd2;

        // Now the magic is that we know port1 and port2 are principle ports so next to them in memory is an agent type
        Agent *agent1 = (Agent *)((char *)port1Ptr - PRINCIPLE_PORT_OFFSET);
        Agent *agent2 = (Agent *)((char *)port2Ptr - PRINCIPLE_PORT_OFFSET);
        assert(agent1->type >= 0);
        assert(agent2->type >= 0);
#if RULE_LOGGING
        printf("---------APPLYING RULE----------T%d\n", id);
        printf("Agent 1 type: %d\n", agent1->type);
        printf("Agent 2 type: %d\n", agent2->type);
        fflush(stdout);
#endif

        ApplyRule(q, agent1, agent2);
    }
}

static bool RECORDING = false;

int main()
{
#if RECORD_WORK_PER_THREAD
    RECORDING = true;
#endif
#if RECORD_TOTAL_STEALS
    RECORDING = true;
#endif
#if RECORD_NEED_TIMER
    RECORDING = true;
#endif

    assert(NUMBER_OF_THREADS_ < 128);
// Can't handle more as we only use 8 bits to keep track of finished threads
#if QUEUE_LOGGING
    printf("NUMBEROF_THREADS_ = %d\n", NUMBER_OF_THREADS_);
    PrintQueueParameters(); // Print parameters for queues
#endif



    stalledQueuesCounter = malloc(sizeof(atomic_int_fast8_t));
    if (stalledQueuesCounter == NULL)
    {
        exit(-1);
    }
    atomic_init(stalledQueuesCounter, 0);

    allQueues = malloc(sizeof(Queue *) * NUMBER_OF_THREADS_);
    if (allQueues == NULL)
    {
        exit(-1);
    }
    allQueues[0] = createQueue();

    for (int i = 1; i < NUMBER_OF_THREADS_; i++)
    {
        allQueues[i] = createQueue();
        setQueueToStealFrom(allQueues[i], allQueues[i - 1]);
    }
    setQueueToStealFrom(allQueues[0], allQueues[NUMBER_OF_THREADS_ - 1]);

    // This section is slightly hacky
    // Since starting net is just a regular rule we can treat it as such
    // But we deference a null pointer cleaning up without this
    // Fairly sure GCC will optimise it away anyway
    Queue *q = allQueues[0];
    Agent *DummyAgent1 = allocateAgent(0, q);
    Port DummyPort1;
    DummyPort1.pointingAt = NULL;
    DummyAgent1->principlePort = DummyPort1;
    ReadInputs();
    INITIALISATION_RULE(q, DummyAgent1, NULL);

    Wire *OUTPUT_WIRE = malloc(sizeof(Wire));
    if (OUTPUT_WIRE == NULL)
    {
        exit(-1);
    }
    OUTPUT_WIRE->wireEnd1 = OUTPUT_PORT;
    OUTPUT_PORT->pointingAt = OUTPUT_WIRE;

    pthread_t *threads = malloc(sizeof(pthread_t) * NUMBER_OF_THREADS_ - 1);
    for (int i = 0; i < NUMBER_OF_THREADS_ - 1; i++)
    {
        int *id = malloc(sizeof(int));
        if (id == NULL)
        {
            exit(-1);
        }
        *id = i + 1;
        pthread_create(&(threads[i]), NULL, threadStart, id);
        if (!RECORDING)
        {
            pthread_detach(threads[i]);
        }
    }

    int *zero = malloc(sizeof(int));
    if (zero == NULL)
    {
        exit(-1);
    }
    *zero = 0;

    threadStart(zero);
    if (RECORDING)
    {
        for (int i = 0; i < NUMBER_OF_THREADS_ - 1; i++)
        {
            if(pthread_join(threads[i], NULL) != 0)
            {
                perror("THREAD JOIN ERROR");
                exit(EXIT_FAILURE);
            }
        }
        for(int i =0; i < NUMBER_OF_THREADS; i++)
        {
            outputAllLogs(allQueues[i], i);
        }
    }

#if DEBUG
    printf("Program Exiting - outputting data and cleaning up\n");
    printf("----------OUTPUT----------\n");
#endif
    output(OUTPUT_WIRE->wireEnd1);
    free(OUTPUT_WIRE);
    free(allQueues);
    free(threads);
#if DEBUG
    printf("\n------------------------\n");
#endif

    return 0;
}
