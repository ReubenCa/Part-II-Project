
from benchmarker import getProgramOutputs
from compiler_options import compiler_options
import re
from statistics import mean, stdev
import datetime
import json
import matplotlib.pyplot as plt
from matplotlib.ticker import MaxNLocator


def getThreadStallData(programFile, input, options, n):
    options.record_thread_stalls = True

    outputs = getProgramOutputs(programFile, input, options, n)
    thread_stalls = list(map(getThreadStallsFromOutput, outputs))
    return thread_stalls
    
def getAreaUnderThreadStalls(thread_stalls):
    total_area = 0
    for i in range(1, len(thread_stalls)):
        x1, y1 = thread_stalls[i - 1]
        x2, y2 = thread_stalls[i]
        area = (x2 - x1) * (y1 + y2) / 2
        total_area += area
    return total_area


def getAverageThreadUtilisation(thread_stalls, number_of_threads):
    area = getAreaUnderThreadStalls(thread_stalls)
    total_time = thread_stalls[-1][0] - thread_stalls[0][0]
    return 1-(area / (total_time * number_of_threads))
    
def getThreadStallsFromOutput(s):
    matches = re.findall(r'TS\((\d+), (\d+)\)', s)
    
    timestamps_values = [(int(ts), int(val)) for ts, val in matches]
    timestamps_values.sort(key=lambda x: x[0])
    
    return timestamps_values


    


def plotStalls(programFile, input, options, n):
    lists_of_ts_value_pairs = getThreadStallData(programFile, input, options, n)
    output_data = {
            "parameters": {
                "compilerOptions": options.getArgumentsForCompiler(),
                "programFile": programFile,
                "input": input,
                "n": n
            },
            "results": {
                "thread_stalls": lists_of_ts_value_pairs
            }
    }
    timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    output_filename = f"results_threadStalls_{timestamp}.json"
    with open(output_filename, "w") as json_file:
        json.dump(output_data, json_file, indent=4)
    
    plot_filename = f"results_threadStalls_{timestamp}.svg"
    # Plotting
    plt.clf()
    for i, ts_value_pairs in enumerate(lists_of_ts_value_pairs):
        if not ts_value_pairs:
            continue
        ts, values = zip(*ts_value_pairs)
        values = list(map(lambda x: options.number_of_threads - x, values))
        plt.plot(ts, values, label=f'Run {i+1}')
    plt.xlabel('Time (ns)')
    plt.ylabel('Amount of Threads Active')
    plt.title(f'Thread Activity Over Time For {programFile} with input {input} on {options.number_of_threads} threads')
    plt.legend()
    plt.gca().yaxis.set_major_locator(MaxNLocator(integer=True))
    plt.gca().xaxis.set_major_locator(MaxNLocator(integer=True))
    plt.xlim(0, None)
    plt.tight_layout()
    plt.savefig(plot_filename)
    print("Plot saved as", plot_filename)

def plotAverageUtilisation(programFile, inputs, thread_counts, n, clearPlot = True):
    thread_counts = list(thread_counts)
    assert(thread_counts[0] != 1)
    utilisations = []
    for input, thread_count in zip(inputs, thread_counts):
        options = compiler_options(thread_count, smallOptimisations=True)
        lists_of_ts_value_pairs = getThreadStallData(programFile, str(input), options, n)
        utilisations_list = list(map(lambda x: getAverageThreadUtilisation(x, thread_count), lists_of_ts_value_pairs))
        utilisations.append(utilisations_list)

    mean_of_utilisations = [mean(utilisations) for utilisations in  utilisations]
    stdev_utilisations = [stdev(utilisations) for utilisations in  utilisations]

    # save to json
    output_data = {
        "parameters": {
            "compilerOptions": options.getArgumentsForCompiler(),
            "programFile": programFile,
            "input": input,
            "n": n
        },
        "results": {
            "mean_of_utilisations": mean_of_utilisations,
            "stdev_utilisations": stdev_utilisations
        }
    }
    timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    output_filename = f"results_average_utilisation_{timestamp}.json"
    with open(output_filename, "w") as json_file:
        json.dump(output_data, json_file, indent=4)

    # Plotting
    if clearPlot:
        plt.clf()
        plt.errorbar(thread_counts, mean_of_utilisations, yerr=stdev_utilisations, fmt='o', capsize=5, linestyle='-')
    else:
        plt.errorbar(thread_counts, mean_of_utilisations, yerr=stdev_utilisations, fmt='o', capsize=5, label=programFile, linestyle='-')
        plt.legend()
    plt.xlabel('Number of Threads')
    plt.ylabel(f'Average Thread Utilisation ')
    if clearPlot:
        plt.title(f'Average Thread Utilisation for {programFile} (n= {n})')
    else:
        plt.title(f"Average Thread Utilisation")
    plt.xticks(thread_counts)
    plt.ylim(0,1)
    plt.gca().xaxis.set_major_locator(MaxNLocator(integer=True))
    plt.tight_layout()
    plt.savefig(f"results_average_utilisation_{timestamp}.svg")
    print("Plot saved as", output_filename)
    

def plotAverageUtilisationByInput(programFile, inputs, thread_count, n, clearPlot = True):

    utilisations = []
    for  index, input in enumerate(inputs):
        print(f"Processing input {index+1}/{len(inputs)}: {input}")
        lists_of_ts_value_pairs = getThreadStallData(programFile, str(input), compiler_options(thread_count, min_queue_steal=4 ), n)
        utilisations_list = list(map(lambda x: getAverageThreadUtilisation(x, thread_count), lists_of_ts_value_pairs))
        utilisations.append(utilisations_list)

    mean_of_utilisations = [mean(utilisations) for utilisations in  utilisations]
    stdev_utilisations = [stdev(utilisations) for utilisations in  utilisations]

    # save to json
    output_data = {
        "parameters": {
            "programFile": programFile,
            "inputs": list(inputs),
            "n": n
        },
        "results": {
            "mean_of_utilisations": mean_of_utilisations,
            "stdev_utilisations": stdev_utilisations
        }
    }
    timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    output_filename = f"results_average_utilisation_{timestamp}.json"
    with open(output_filename, "w") as json_file:
        json.dump(output_data, json_file, indent=4)

    # Plotting
    if clearPlot:
        plt.clf()
        plt.errorbar(inputs, mean_of_utilisations, yerr=stdev_utilisations, fmt='o', capsize=5, linestyle='-')
    else:
        plt.errorbar(inputs, mean_of_utilisations, yerr=stdev_utilisations, fmt='o', capsize=5, label=programFile, linestyle='-')
        plt.legend()
    plt.xlabel('Input')
    plt.ylabel(f'Average Thread Utilisation ')
    if clearPlot:
        plt.title(f'Average Thread Utilisation for {programFile} with Aggressive Stealing (n= {n})')
    else:
        plt.title(f"Average Thread Utilisation")
    
    #plt.xticks(thread_counts)
    plt.ylim(0,1)
    plt.gca().xaxis.set_major_locator(MaxNLocator(integer=True))
    plt.tight_layout()
    plt.savefig(f"results_average_utilisation_{timestamp}.svg")
    print("Plot saved as", output_filename)

if __name__ == "__main__":


   #plotStalls("fib.txt", "40", compiler_options(18), 1)
    # plotStalls("tripleCollatz.txt", "1000", compiler_options(18, smallOptimisations=True), 1)
    # plotStalls("tripleCollatz.txt", "700", compiler_options(14, smallOptimisations=True), 1)
    # plotStalls("tripleCollatz.txt", "600", compiler_options(10, smallOptimisations=True), 1)
    # plotStalls("tripleCollatz.txt", "300", compiler_options(5, smallOptimisations=True), 1)
    # plotStalls("fib.txt", "35", compiler_options(18, smallOptimisations=True), 1)

    
    # plotAverageUtilisationByInput("fib.txt", range(10,34,1), 18, 10)
    # plotAverageUtilisationByInput("tripleCollatz.txt", range(100,150,10), 5, 10)

    # plotAverageUtilisationByInput("fib.txt", range(10,36,1), 18, 10)
    # plotAverageUtilisationByInput("tripleCollatz.txt", range(100,250,10), 5, 10)
