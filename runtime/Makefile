CC = gcc
COMPILER_OUTPUT ?= build/compilerOutput.c
SRC = src/*.c $(COMPILER_OUTPUT)
INCLUDE = -I include
BUILD_DIR ?= build
NONRELEASE_OUTPUT = $(BUILD_DIR)/program.o
ASM_NONRELEASE_OUTPUT = $(BUILD_DIR)/program.asm

#Default Values - note the java compiler has its own defaults that are what it calls the makefile with
#this is just for my quick debugging
INITIAL_BUFFER_SIZE ?= 1024
MIN_QUEUE_SIZE_TO_ATTEMPT_STEAL ?= 4
MIN_QUEUE_SIZE_TO_ACTUALLY_STEAL ?= 2
MAX_ITEMS_TO_STEAL ?= 8
FRACTION_TO_STEAL ?= 4
NUMBER_OF_THREADS ?= 1


DEFINES = \
  -DINITIAL_BUFFER_SIZE=$(INITIAL_BUFFER_SIZE) \
  -DMIN_QUEUE_SIZE_TO_ATTEMPT_STEAL=$(MIN_QUEUE_SIZE_TO_ATTEMPT_STEAL) \
  -DMIN_QUEUE_SIZE_TO_ACTUALLY_STEAL=$(MIN_QUEUE_SIZE_TO_ACTUALLY_STEAL) \
  -DMAX_ITEMS_TO_STEAL=$(MAX_ITEMS_TO_STEAL) \
  -DFRACTION_TO_STEAL=$(FRACTION_TO_STEAL) \
  -DNUMBER_OF_THREADS=$(NUMBER_OF_THREADS)



RECORD_WORK_PER_THREAD ?=
RECORD_THREAD_STALLS ?=
RECORD_QUEUE_SIZE ?=
QUEUE_LOGGING ?=
EXPERIMENTAL_MEMORY_MANAGER ?=

RECORDING_FLAGS = 

ifeq ($(RECORD_WORK_PER_THREAD),1)
	RECORDING_FLAGS += -DRECORD_WORK_PER_THREAD
endif

ifeq ($(RECORD_THREAD_STALLS),1)
	RECORDING_FLAGS += -DRECORD_THREAD_STALLS
endif

ifeq ($(RECORD_QUEUE_SIZE),1)
	RECORDING_FLAGS += -DRECORD_QUEUE_SIZE
endif

ifeq ($(or $(RECORD_THREAD_STALLS),$(RECORD_QUEUE_SIZE)),1)
	RECORDING_FLAGS += -DRECORD_NEED_TIMER
endif

ifeq ($(QUEUE_LOGGING),1)
	RECORDING_FLAGS += -DQUEUE_LOGGING
endif

ifeq ($(RULE_LOGGING),1)
	RECORDING_FLAGS += -DRULE_LOGGING
endif

ifeq ($(EXPERIMENTAL_MEMORY_MANAGER),1)
	RECORDING_FLAGS += -DEXPERIMENTAL_MEMORY_MANAGER
endif





# Compiler flags
CFLAGS_DEBUG = -g -DDEBUG -fsanitize=address -fsanitize=undefined -fno-omit-frame-pointer -Wall -pedantic -Wno-unused-variable -O0 $(DEFINES) $(RECORDING_FLAGS) 
CFLAGS_OPTIMIZED = -O3 -DNDEBUG -flto=auto -fwhole-program $(DEFINES) $(RECORDING_FLAGS) 

# Default target
all: release

#ended up with a slightly excessive amount of targets
#But the release ones are for the java program to call (which can be instructed to compile without optimizations)
#The rest are for me when debugging the actual runtime which doesn't need me to run the java compiler and also doesn't need me to specify where to build etc

debug:
	$(CC) $(SRC) $(INCLUDE) $(CFLAGS_DEBUG) -o $(NONRELEASE_OUTPUT) $(DEFINES) $(RECORDING_FLAGS) 

run_debug: debug
	@env ASAN_OPTIONS=detect_leaks=0 ./$(OUTPUT)

optimized:
	$(CC) $(SRC) $(INCLUDE) $(CFLAGS_OPTIMIZED) -o $(NONRELEASE_OUTPUT) $(DEFINES) $(RECORDING_FLAGS) 
	objdump -d $(NONRELEASE_OUTPUT) > $(ASM_NONRELEASE_OUTPUT)

semi_optimized:
	$(CC) $(SRC) $(INCLUDE) -fsanitize=address -fsanitize=undefined -o $(NONRELEASE_OUTPUT) $(RECORDING_FLAGS) $(DEFINES)

run_semi_optimized: semi_optimized
	@env ASAN_OPTIONS=detect_leaks=0 ./$(OUTPUT)

release:
ifndef OUTDIR
	$(error OUTDIR is required. Usage: make release OUTDIR=path/to/output [ASMOUT=path/to/asmfile.asm])
endif
	$(CC) $(SRC) $(INCLUDE) $(CFLAGS_OPTIMIZED) -o $(OUTDIR) $(DEFINES) $(RECORDING_FLAGS)
ifdef ASMOUT
	objdump -d $(OUTDIR)/program.o > $(ASMOUT)
endif



release_debug_mode:
ifndef OUTDIR
	$(error OUTDIR is required. Usage: make release OUTDIR=path/to/output [ASMOUT=path/to/asmfile.asm])
endif
	$(CC) $(SRC) $(INCLUDE) $(DEFINES) -o $(OUTDIR) $(CFLAGS_DEBUG)
ifdef ASMOUT
	objdump -d $(OUTDIR)/program.o > $(ASMOUT)
endif



