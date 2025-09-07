
from benchmarker import getProgramOutputs
from compiler_options import compiler_options
import re
from statistics import mean, stdev
import datetime
import json
import matplotlib.pyplot as plt
from matplotlib.ticker import MaxNLocator
from collections import defaultdict
import math

def getQueueSizeData(programFile, input, options, resolution=1, minTimeFrame = 0, maxTimeFrame = math.inf):
    options.record_queue_size = True

    queue_sizes = getQueueSizeDataFromOutput(getProgramOutputs(programFile, input, options, 1)[0], resolution,minTimeFrame , maxTimeFrame )
    return queue_sizes
    
    
    
def getQueueSizeDataFromOutput(log_string, resolution=1, minTimeFrame = 0, maxTimeFrame = math.inf):
    pattern = r'QS(\d+).(\d+),(\d+)'
    #data = defaultdict(lambda: [(minTimeFrame, 0)])
    data = defaultdict(list)

    for match in re.finditer(pattern, log_string):
        thread, timestamp, size = match.groups()
        #skip data that is not changed - but in theory there shouldnt be any
        data_length = len(data[int(thread)])

        if int(timestamp) < minTimeFrame or int(timestamp) > maxTimeFrame:
            continue

        if data_length != 0 and abs(int(size) - data[int(thread)][data_length-1][1]) > 1000:
            print(f"Discarded measurement that jumped from {data[int(thread)][data_length-1][1]} to {int(size)} on thread {thread} index {data_length}")
            continue
        data[int(thread)].append((int(timestamp), int(size)))

    return dict(data)

def allQueueSizesOverTime(programFile, input, options, resolution = 1, minTimeFrame = 0, maxTimeFrame = math.inf):
    queue_sizes = getQueueSizeData(programFile, input, options, resolution, minTimeFrame, maxTimeFrame )
    number_of_threads = options.number_of_threads
    


    total_data_size = 0
    for thread, ts_size_pairs in queue_sizes.items():
        total_data_size += (len(ts_size_pairs) + resolution - 1) // resolution

    
    print(f"total data size: {total_data_size}")
    combined_data = [None] * total_data_size 
    combined_data_index = 0
    for thread, ts_size_pairs in queue_sizes.items():
        for index, (ts, size) in  enumerate(ts_size_pairs):
            if index % resolution != 0:
                continue
            combined_data[combined_data_index] = (thread, ts, size)
            combined_data_index += 1

    combined_data.sort(key=lambda x: x[1])

    #compress data as matplotlib cant handle size of this
    print("compressing data from", total_data_size, "to", total_data_size//resolution)
    combined_data = combined_data[::resolution]
    total_data_size = len(combined_data)

    #For each queue we generate a list of its size at every timestamp
    all_queue_sizes = [([None]*number_of_threads) for _ in range(total_data_size)]
    time_stamps = [None] * total_data_size
    current_queue_sizes = [0] * number_of_threads

    for i in range(total_data_size):
        thread, ts, size = combined_data[i]
        time_stamps[i] = ts
        current_queue_sizes[thread] = size
        for j in range(number_of_threads):
            all_queue_sizes[i][j] = current_queue_sizes[j]
    return time_stamps, all_queue_sizes



        
def plotQueueSizesOverTime(programFile, input, options, GRAPH_RESOLUTION=1, minTimeFrame = 0, maxTimeFrame = math.inf):


    number_of_threads = options.number_of_threads
    percentiles = [0.0, 0.2, 0.4, 0.50, 0.6, 0.8, 1.0]
    #colors = ['#cce5ff', '#99ccff', '#66b2ff', '#3399ff', '#66b2ff', '#99ccff', '#cce5ff']
    alphas = [0.2, 0.3, 0.5, 0.8, 0.5, 0.3, 0.2]

    cmap = plt.get_cmap('Spectral')
    colors = [cmap(i) for i in range(len(percentiles))]
    assert len(percentiles) == len(colors) == len(alphas), "percentiles, colors and alphas must be the same length"


    time_stamps, all_queue_sizes = allQueueSizesOverTime(programFile, input, options, GRAPH_RESOLUTION,minTimeFrame, maxTimeFrame)

    #save to json
    output_data = {
            "parameters": {
                "compilerOptions": options.getArgumentsForCompiler(),
                "programFile": programFile,
                "input": input,
            },
            "results": {
                "queue_sizes": all_queue_sizes,
                "time_stamps": time_stamps
            }
    }
    timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    output_filename = f"results_queue_sizes_{timestamp}.json"
    with open(output_filename, "w") as json_file:
        json.dump(output_data, json_file, indent=4)
    plot_filename = f"results_queueSizes_{timestamp}.png"#when saved as svg is over a GB



    
    #to color at least thread_thresholds threads must be at this size
    thread_thresholds = [int(i*(number_of_threads-1)) for i in percentiles]
    print(f"thread thresholds: {thread_thresholds}")

    for i in range(0,len(thread_thresholds)-1, GRAPH_RESOLUTION):
        lower = list(map(lambda x: getNumberOverThreshold(x, thread_thresholds[i]), all_queue_sizes))
        upper = list(map(lambda x: getNumberOverThreshold(x, thread_thresholds[i+1]), all_queue_sizes))
        plt.fill_between(time_stamps, lower, upper, color=colors[i], alpha=alphas[i])
    
    plt.title(f"Queue Sizes Over Single Execution of {programFile} on input {input} ({number_of_threads} threads)")
    plt.xlabel('Time (ns)')
    plt.ylabel('Queue Sizes')
    plt.gca().yaxis.set_major_locator(MaxNLocator(integer=True))


    print("saving plot to", plot_filename)
    plt.savefig(plot_filename)

          
def getNumberOverThreshold(lis, thread_threshold):
    copy = lis.copy()
    copy.sort()
    return copy[thread_threshold]
    


def plotQueueSizes(programFile, input, options, minTimeFrame = 0, maxTimeFrame = math.inf ):
    queue_sizes = getQueueSizeData(programFile, input, options, minTimeFrame=minTimeFrame, maxTimeFrame=maxTimeFrame)
    output_data = {
            "parameters": {
                "compilerOptions": options.getArgumentsForCompiler(),
                "programFile": programFile,
                "input": input,
            },
            "results": {
                "queue_sizes": queue_sizes
            }
    }
    timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    output_filename = f"results_queue_sizes_{timestamp}.json"
    with open(output_filename, "w") as json_file:
        json.dump(output_data, json_file)
    plot_filename = f"results_queueSizes_{timestamp}.svg"
    # Plotting
    print("Generating Plot")
    plt.clf()
    num_threads = len(queue_sizes)
    ncols = 3
    nrows = (num_threads + ncols - 1) // ncols
    fig, axes = plt.subplots(nrows, ncols, sharex=True, sharey=True, figsize=(10, 2.5 * nrows))
    axes = axes.flatten()
    plt.ylim(0,None)
    plt.xlim(0, None)
    for idx, (thread, ts_size_pairs) in enumerate(sorted(queue_sizes.items())):
        if not ts_size_pairs:
            continue
        ts, size = zip(*ts_size_pairs)
        axes[idx].plot(ts, size)
        axes[idx].set_ylabel(f'Thread {thread}')
        axes[idx].yaxis.set_major_locator(MaxNLocator(integer=True))
    for ax in axes[num_threads:]:
        ax.set_visible(False)
    axes[-1].set_xlabel('Time (ns)')
    fig.suptitle(f'Queue Sizes Over Execution of {programFile} on input {input}')
    plt.tight_layout(rect=[0, 0.03, 1, 0.95])
    plt.savefig(plot_filename, dpi=1500)
    print("Plot saved as", plot_filename)

if __name__ == "__main__":
    #plotQueueSizesOverTime("fib.txt", "280", compiler_options(18))
    #plotQueueSizesOverTime("tripleCollatz.txt", "50", compiler_options(5), GRAPH_RESOLUTION=1)
    #plotQueueSizesOverTime("biggerfib.txt", "25", compiler_options(18))
    plotQueueSizes("fib.txt", "35", compiler_options(18))
    plotQueueSizes("tripleCollatz.txt", "300", compiler_options(18))
    #plotQueueSizes("fib.txt", "36", compiler_options(18), minTimeFrame= 0.5*10e9, maxTimeFrame = 3 * 10e9)
    #plt.clf()
    #plotQueueSizesOverTime("tripleCollatz.txt", "250", compiler_options(18, smallOptimisations=True),1,0.3*10e9, 1.2*10e9)