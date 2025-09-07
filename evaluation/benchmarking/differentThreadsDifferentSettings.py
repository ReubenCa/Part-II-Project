import matplotlib.pyplot as plt
import benchmarker
from statistics import mean, stdev
from os import sys, path
import json
from datetime import datetime

sys.path.append(path.dirname(path.dirname(path.abspath(__file__))))
from compiler_options import compiler_options
from progressbar import progressbar  # if used inside `benchmarker`

def testAtDifferentThreadNumbers(programFile, input, maxThreads, Iterations, option_generator, minThreads=1):
    list_of_results = [None] * (maxThreads - minThreads + 1)
    for i in range(minThreads, maxThreads + 1):
        print(f"({i - minThreads + 1}/{maxThreads - minThreads + 1}) Testing with {i} threads")
        options = option_generator(i)
        result = benchmarker.TimeAverages(programFile, input, options, Iterations)
        list_of_results[i - minThreads] = benchmarker.getListOfTimes(result)

    averages = [mean(lis) for lis in list_of_results]
    standard_deviations = [stdev(lis) for lis in list_of_results]
    return averages, standard_deviations

def save_and_plot_results_multi_config(programFile, input, maxThreads, Iterations, minThreads, config_results):
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_data = {
        "timestamp": timestamp,
        "parameters": {
            "programFile": programFile,
            "input": input,
            "maxNumberOfThreads": maxThreads,
            "Iterations": Iterations,
            "minThreads": minThreads
        },
        "results": {}
    }

    thread_counts = list(range(minThreads, maxThreads + 1))
    plt.figure()

    for config in config_results:
        label = config["label"]
        averages = config["averages"]
        stddevs = config["standard_deviations"]
        output_data["results"][label] = {
            "averages": averages,
            "standard_deviations": stddevs
        }
        plt.errorbar(thread_counts, averages, yerr=stddevs,
                     fmt='-o', capsize=5, label=label)

    plt.title('Performance at Different Thread Counts')
    plt.xlabel('Number of Threads')
    plt.ylabel('Average Execution Time (s)')
    plt.ylim(-0.1, None)
    plt.xlim(minThreads - 0.1, maxThreads + 0.1)
    plt.legend()
    plt.grid(True)

    plot_filename = f"plot_comparison_{timestamp}.svg"
    results_filename = f"results_comparison_{timestamp}.json"
    plt.savefig(plot_filename, dpi=1200)
    plt.show()

    with open(results_filename, "w") as f:
        json.dump(output_data, f, indent=4)

    print(f"Results saved to {results_filename}")
    print(f"Plot saved to {plot_filename}")

# Example usage:
if __name__ == "__main__":
    programFile = "fib.txt"
    input = "34"
    maxThreads = 18
    minThreads = 5
    numOfTrials = 10

    # Define configurations to test
    configs = [
     #   {
     #       "label": "No Optimisations",
     #       "option_generator": lambda threads: compiler_options(threads, noOptimisations=True)
     #   },

        {
            "label": "Linear Bump",
            "option_generator": lambda threads: compiler_options(threads, experimental_memory_manager=True, smallOptimisations=True)
        },
                {
            "label": "Malloc",
            "option_generator": lambda threads: compiler_options(threads, smallOptimisations=True)
        }
    ]

    # Run benchmarks
    config_results = []
    for config in configs:
        print(f"Running: {config['label']}")
        averages, stddevs = testAtDifferentThreadNumbers(
            programFile, input, maxThreads, numOfTrials, config["option_generator"], minThreads
        )
        config_results.append({
            "label": config["label"],
            "averages": averages,
            "standard_deviations": stddevs
        })

    # Save and plot results
    save_and_plot_results_multi_config(programFile, input, maxThreads, numOfTrials, minThreads, config_results)
