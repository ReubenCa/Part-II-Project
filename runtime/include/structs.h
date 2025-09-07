#ifndef STRUCTS_H
#define STRUCTS_H
#include <stdbool.h>
#include <stdatomic.h>




typedef struct Wire Wire;


typedef struct Port {
    Wire *pointingAt;
} Port;





typedef struct Wire {
    atomic_int_fast8_t hotness;
    Port *wireEnd1;
    Port *wireEnd2;
} Wire;



//Whats important is that the type goes first with the principle port below 
//In the future the generated code could specify size of agent type as it might actually only need a couple of bits
typedef struct Agent {
  int type;
  Port principlePort;
  Port auxPorts[];
} Agent;

typedef struct intDataAgent {
  int type;
  Port principlePort;
  int data;
  Port auxPorts[];
} intDataAgent;

typedef struct floatDataAgent {
  int type;
  Port principlePort;
  float data;
  Port auxPorts[];
} floatDataAgent;


#endif