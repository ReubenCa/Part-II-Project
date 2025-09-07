import benchmarker
from statistics import mean, stdev
from os import sys, path
sys.path.append(path.dirname(path.dirname(path.abspath(__file__))))

from compiler_options import compiler_options
from progressbar import progressbar
import json
from datetime import datetime
import matplotlib.pyplot as plt
from matplotlib.ticker import MaxNLocator

def testAtDifferentThreadNumbers(programFile, input, threadcountstouse, Iterations, options : compiler_options ):
    
    plt.clf()
    list_of_results = [None]*(len(threadcountstouse))
    for index, i in enumerate(threadcountstouse):
        print(f"({index}/{len(threadcountstouse)}) Testing with {i} threads")
        options.number_of_threads = i
        result = benchmarker.TimeAverages(programFile, input, options, Iterations)
        list_of_results[index] = benchmarker.getListOfTimes(result)

    averages = list(map(lambda lis: mean(lis), list_of_results))
    standard_deviations = list(map(lambda lis: stdev(lis), list_of_results))
    print("Averages: ", averages)
    print("Standard Deviations: ", standard_deviations)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_data = {
        "timestamp": timestamp,
        "parameters": {
            "compilerOptions": options.getArgumentsForCompiler(),
            "programFile": programFile,
            "input": input,
            "threadcounts": list(threadcountstouse),
            "Iterations": Iterations
        },
        "results": {
            "averages": averages,
            "standard_deviations": standard_deviations
        }
    }

    output_filename = f"results_{timestamp}.json"
    with open(output_filename, "w") as json_file:
        json.dump(output_data, json_file, indent=4)

    print(f"Results saved to {output_filename}")


    
    plt.errorbar(list(map(int, threadcountstouse)), averages, yerr=standard_deviations, fmt='-o', capsize=5, label='Execution Time')
    plt.title(f'Execution time of {programFile} with input {input} (n={Iterations})')
    plt.xlabel('Number of Threads')
    plt.ylabel('Average Execution Time (s)')
    plt.ylim(0, None)
    ax = plt.gca()
    ax.xaxis.set_major_locator(MaxNLocator(integer=True))  # Force x-axis ticks to be integers
    plt.legend()
    plt.grid(True)
    # Save the plot as an image
    plot_filename = f"plot_{timestamp}.svg"
    plt.savefig(plot_filename, dpi = 1200)
    plt.show()

    print(f"Plot saved to {plot_filename}")


if __name__ == "__main__":
    #testAtDifferentThreadNumbers("tripleCollatz.txt", "30", 20, 1000, 1)
    #testAtDifferentThreadNumbers("fib.txt", "26", range(1,19,2), 25) #26 takes 0.1 seconds on single thread
    #testAtDifferentThreadNumbers("tripleCollatz.txt", "30", 20, 50, 1)
    #testAtDifferentThreadNumbers("fib.txt", "30", 20, 50, 1) #26 takes 0.1 seconds on single thread
    #testAtDifferentThreadNumbers("tripleCollatz.txt", "230",  range(1, 20, 2), 15) # takes 10 seconds on single thread
    #testAtDifferentThreadNumbers("fib.txt", "35", range(1,19,1), 100, 1) #35 takes ~ 8.5 seconds on a single thread
    # testAtDifferentThreadNumbers("tripleCollatz.txt", "380", 20, 10, 1)  # takes 20 seconds on single thread
    #testAtDifferentThreadNumbers("fib.txt", "37", range(1,19,3), 10, compiler_options(-1, min_queue_steal=2)) #37 takes ~ 22 seconds on a single thread
    #testAtDifferentThreadNumbers("tripleCollatz.txt", "230", range(1,19,3), 5, compiler_options(-1, min_queue_steal=4))
    
   # testAtDifferentThreadNumbers("fib.txt", "37", range(1,19,1), 10, compiler_options(-1))
    testAtDifferentThreadNumbers("fib.txt", "35", range(1,19,1), 100, compiler_options(-1))
    testAtDifferentThreadNumbers("fib.txt", "37", range(1,19,1), 25, compiler_options(-1))

    testAtDifferentThreadNumbers("tripleCollatz.txt", "30", range(1,19,1), 1000, compiler_options(-1))
    testAtDifferentThreadNumbers("tripleCollatz.txt", "230", range(1,19,1), 100, compiler_options(-1))
    testAtDifferentThreadNumbers("tripleCollatz.txt", "380", range(1,19,1), 25, compiler_options(-1))
    
