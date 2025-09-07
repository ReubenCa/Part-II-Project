import matplotlib.pyplot as plt
import benchmarker
from statistics import mean, stdev
from os import sys, path
sys.path.append(path.dirname(path.dirname(path.abspath(__file__))))

from compiler_options import compiler_options
from progressbar import progressbar
import json
from datetime import datetime


def testAtDifferentThreadNumbers(programFile, input, maxNumberOfThreads, Iterations, doOptimisations : bool, minThreads = 1):
    list_of_results = [None]*(maxNumberOfThreads-minThreads + 1)
    for i in range(minThreads, maxNumberOfThreads + 1):
        print(f"({i-minThreads+1}/{maxNumberOfThreads-minThreads + 1}) Testing with {i} threads")
        options = compiler_options(i, noOptimisations=not doOptimisations)
        result = benchmarker.TimeAverages(programFile, input, options, Iterations)
        list_of_results[i-minThreads] = benchmarker.getListOfTimes(result)

    averages = list(map(lambda lis: mean(lis), list_of_results))
    standard_deviations = list(map(lambda lis: stdev(lis), list_of_results))
    return averages, standard_deviations

def save_and_plot_results_with_comparison(
    programFile, input, maxNumberOfThreads, Iterations, minThreads,
    no_optimisations_averages, no_optimisation_standard_deviations,
    optimisations_averages, optimisation_standard_deviations
):
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_data = {
        "timestamp": timestamp,
        "parameters": {
            "programFile": programFile,
            "input": input,
            "maxNumberOfThreads": maxNumberOfThreads,
            "Iterations": Iterations,
            "minThreads": minThreads
        },
        "results": {
            "no_optimisations": {
                "averages": no_optimisations_averages,
                "standard_deviations": no_optimisation_standard_deviations
            },
            "optimisations": {
                "averages": optimisations_averages,
                "standard_deviations": optimisation_standard_deviations
            }
        }
    }

    output_filename = f"results_comparison_{timestamp}.json"
    with open(output_filename, "w") as json_file:
        json.dump(output_data, json_file, indent=4)

    print(f"Results saved to {output_filename}")

    thread_counts = list(range(minThreads, maxNumberOfThreads + 1))
    plt.errorbar(thread_counts, no_optimisations_averages, yerr=no_optimisation_standard_deviations,
                 fmt='-o', capsize=5, label='Without Optimisations')
    plt.errorbar(thread_counts, optimisations_averages, yerr=optimisation_standard_deviations,
                 fmt='-o', capsize=5, label='With Optimisations')
    plt.title('Performance at Different Thread Counts')
    plt.xlabel('Number of Threads')
    plt.ylabel('Average Execution Time (s)')
    plt.ylim(-0.1, None)
    plt.xlim(minThreads-0.1, maxNumberOfThreads+0.1)
    plt.legend()
    plt.grid(True)
    # Save the plot as an image
    plot_filename = f"plot_comparison_{timestamp}.svg"
    plt.savefig(plot_filename, dpi=1200)
    plt.show()

    print(f"Plot saved to {plot_filename}")


        
if __name__ == "__main__":
    programFile = "tripleCollatz.txt"
    input = "300"
    maxThreads = 20
    minThreads = 10
    numOfTrials = 25
    no_optimisations_averages, no_optimisation_standard_deviations = testAtDifferentThreadNumbers(
        programFile, input, maxThreads, numOfTrials, False, minThreads
    )

    optimisations_averages, optimisation_standard_deviations = testAtDifferentThreadNumbers(
        programFile, input, maxThreads, numOfTrials, True, minThreads
    )
    print("No Optimisations Averages:", no_optimisations_averages)
    print("No Optimisations Standard Deviations:", no_optimisation_standard_deviations)

    print("Optimisations Averages:", optimisations_averages)
    print("Optimisations Standard Deviations:", optimisation_standard_deviations)
    save_and_plot_results_with_comparison(
        programFile, input, maxThreads, numOfTrials, minThreads,
        no_optimisations_averages, no_optimisation_standard_deviations,
        optimisations_averages, optimisation_standard_deviations
    )