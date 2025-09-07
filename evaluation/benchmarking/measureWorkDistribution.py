
from benchmarker import getProgramOutputs
from compiler_options import compiler_options
import re
from statistics import mean, stdev
import datetime
import json
import matplotlib.pyplot as plt
from matplotlib.ticker import MaxNLocator

def getWorkDistributions(programFile, input, options, n):
    options.record_work_per_thread = True
    outputs = getProgramOutputs(programFile, input, options, n)
    work_distributions = list(map(getWorkFromOutput, outputs))
    return work_distributions
    # print("Work distributions: ", work_distributions)
    # print("Work distributions std: ", work_distributions_std)
    # print("Work distributions mean: ", work_distributions_mean)
    # print("Work distributions cov: ", work_distributions_cov)


def getWorkFromOutput(s):
    reductions = re.findall(r'(\d+)\s+reductions', s)
    reduction_list = list(map(int, reductions))
    return reduction_list

def calcualteWorkDistributionsOverInputSize(inputs_sizes, programFile, options, n, timestamp):
    covs = len(inputs_sizes) * [None]
    covs_std = len(inputs_sizes) * [None]
    work_distributions = len(inputs_sizes) * [None]
    work_distributions_std = len(inputs_sizes) * [None]
    work_distributions_mean = len(inputs_sizes) * [None]
    for index, i in enumerate(inputs_sizes):
        print(f"{index+1}/{len(inputs_sizes)} Testing with input {i}")
        work_distributions = getWorkDistributions(programFile, str(i), options, n)
        work_distributions_std = list(map(stdev, work_distributions))
        work_distributions_mean = list(map(mean, work_distributions))
        work_distributions_cov = list(map(lambda x: x[0]/x[1], zip(work_distributions_std, work_distributions_mean)))
        covs[index] = mean(work_distributions_cov)
        covs_std[index] = stdev(work_distributions_cov)
    
    output_data = {
        "timestamp": timestamp,
        "parameters": {
            "compilerOptions": options.getArgumentsForCompiler(),
            "programFile": programFile,
            "inputs_sizes": list(inputs_sizes),
            "n": n
        },
        "results": {
            "work_distributions": work_distributions,
            "work_distributions_std": work_distributions_std,
            "work_distributions_mean": work_distributions_mean,
            "work_distributions_cov": work_distributions_cov,
            "covs": covs,
            "covs_std": covs_std
        }
    }
    output_filename = f"results_workDistribution_{timestamp}.json"
    with open(output_filename, "w") as json_file:
        json.dump(output_data, json_file, indent=4)
    return covs, covs_std


def calculateAndPlotWorkDistributionsOverInputSize(inputs_sizes, programFile, options, n):
    plt.clf()
    timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    covs, covs_std = calcualteWorkDistributionsOverInputSize(inputs_sizes, programFile, options, n, timestamp)
    
    plt.errorbar(inputs_sizes, covs, yerr=covs_std, fmt='-o', capsize=5, label='Cov of Work Distribution',color='green')
    plt.title(f'Work distribution of {programFile} (n={n})')
    plt.xlabel('Input')
    plt.ylabel('Coefficient of Variance of interactions per thread')
    ax = plt.gca()
    plt.ylim(0,None)
    plt.xlim(0,None)
    ax.yaxis.set_major_locator(MaxNLocator(integer=True))  # Force x-axis ticks to be integers
    plt.legend()
    plt.grid(True)
    # Save the plot as an image
    plot_filename = f"plot_workDistribution_{timestamp}.svg"
    plt.savefig(plot_filename, dpi = 1200)
    print(f"Plot saved as {plot_filename}")
    plt.show()

if __name__ == "__main__":
    #calculateAndPlotWorkDistributionsOverInputSize(range(4, 40,2), "fib.txt", compiler_options(18), 15)
    calculateAndPlotWorkDistributionsOverInputSize(range(10, 800, 35), "tripleCollatz.txt", compiler_options(18), 15)
    calculateAndPlotWorkDistributionsOverInputSize(range(5, 40, 1), "fib.txt", compiler_options(18), 15)
    3.0