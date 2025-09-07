import matplotlib.pyplot as plt
import benchmarker
from statistics import mean, stdev
from os import sys, path
import json
from datetime import datetime
from matplotlib.ticker import MaxNLocator

sys.path.append(path.dirname(path.dirname(path.abspath(__file__))))
from compiler_options import compiler_options
from progressbar import progressbar 

def testAtDifferentInputSizes(programFile, input_range, threads, Iterations, option_generator):
    input_values = list(input_range)
    list_of_results = [None] * len(input_values)

    for idx, input_val in enumerate(input_values):
        print(f"({idx + 1}/{len(input_values)}) Testing with input {input_val}")
        options = option_generator(threads)
        result = benchmarker.TimeAverages(programFile, str(input_val), options, Iterations)
        list_of_results[idx] = benchmarker.getListOfTimes(result)

    averages = [mean(lis) for lis in list_of_results]
    standard_deviations = [stdev(lis) for lis in list_of_results]
    return input_values, averages, standard_deviations

def save_and_plot_results_input_variation(programFile, thread_count, Iterations, input_range, config_results):
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_data = {
        "timestamp": timestamp,
        "parameters": {
            "programFile": programFile,
            "threadCount": thread_count,
            "Iterations": Iterations,
            "inputRange": list(input_range)
        },
        "results": {}
    }

   
    plt.figure()

    for config in config_results:
        label = config["label"]
        averages = config["averages"]
        stddevs = config["standard_deviations"]
        output_data["results"][label] = {
            "averages": averages,
            "standard_deviations": stddevs
        }
        plt.errorbar(list(input_range), averages, yerr=stddevs,
                     fmt='-o', capsize=5, label=label)

    plt.title(f'Performance vs Input Size at {thread_count} Threads (n={Iterations})')
    plt.xlabel('Input Size')
    plt.ylabel('Average Execution Time (s)')
    plt.ylim(0, None)
   # plt.xlim(input_values[0] - 0.5, input_values[-1] + 0.5)
    plt.legend()
    plt.grid(True)
    plt.gca().xaxis.set_major_locator(MaxNLocator(integer=True))  # Force x-axis ticks to be integers

    plot_filename = f"plot_input_variation_{timestamp}.svg"
    results_filename = f"results_input_variation_{timestamp}.json"
    plt.savefig(plot_filename, dpi=1200)
    plt.show()

    with open(results_filename, "w") as f:
        json.dump(output_data, f, indent=4)

    print(f"Results saved to {results_filename}")
    print(f"Plot saved to {plot_filename}")

def run_benchmarks(programFile, input_range, thread_count, numOfTrials, configs):
   
    config_results = []
    for config in configs:
        print(f"Running: {config['label']}")
        input_values, averages, stddevs = testAtDifferentInputSizes(
            programFile, input_range, thread_count, numOfTrials, config["option_generator"]
        )
        config_results.append({
            "label": config["label"],
            "averages": averages,
            "standard_deviations": stddevs
        })
    try:
        save_and_plot_results_input_variation(programFile, thread_count, numOfTrials, input_range, config_results)
    except Exception as e:
        print(f"Error while saving results: {e}")
        print("Continuing with the next configuration...")
    plt.clf()

if __name__ == "__main__":


       

    configs = [
        # {
        #     "label": "Min Queue Steal 2",
        #     "option_generator": lambda threads: compiler_options(threads, min_queue_steal=2, smallOptimisations=True)
        # },
        {
            "label": "Min Queue Steal 4",
            "option_generator": lambda threads: compiler_options(threads, min_queue_steal=4, smallOptimisations=True)
        },
        {
            "label": "Min Queue Steal 6",
            "option_generator": lambda threads: compiler_options(threads, smallOptimisations=True)
        }]


    run_benchmarks("tripleCollatz.txt",range(100,250,10),18,10,configs)


    # configs = [
    #     {
    #         "label": "Large Optimisations",
    #         "option_generator": lambda threads: compiler_options(threads)
    #     },
    #     {
    #         "label": "No Optimisations",
    #         "option_generator": lambda threads: compiler_options(threads, noOptimisations=True)
    #     },
    #     {
    #         "label": "Small Optimisations",
    #         "option_generator": lambda threads: compiler_options(threads, smallOptimisations=True)
    #     }
    # ]

    # run_benchmarks("fib.txt",range(10,36,1),18,10,configs)

    # configs = [
    #     {
    #         "label": "Linear Bump",
    #         "option_generator": lambda threads: compiler_options(threads, experimental_memory_manager=True, smallOptimisations=True)
    #     },
    #     {
    #         "label": "Malloc",
    #         "option_generator": lambda threads: compiler_options(threads, smallOptimisations=True)
    #     }
    # ]


    # run_benchmarks("fib.txt",range(10,32,1),18,10,configs)
    # run_benchmarks("tripleCollatz.txt",range(50,200,10),18,10,configs)

    # run_benchmarks(
    #     "tripleCollatz.txt", range(100,200,5), 18, 5, configs
    #     )

   
