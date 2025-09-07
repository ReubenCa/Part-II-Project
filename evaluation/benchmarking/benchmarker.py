from os import sys, path
import subprocess
from time import sleep
sys.path.append(path.dirname(path.dirname(path.abspath(__file__))))

from compiler_options import compiler_options
import builder
import re
import progressbar

runtime_path = path.join(path.dirname(__file__), '..', '..', 'runtime')
output_path = path.join(runtime_path, 'build')



def getProgramOutputs(programFile, input, options, n):
    ALL_OUTPUTS = [None]*n
    builder.CompileProgram(programFile, options)
    t = 0
    for i in progressbar.progressbar(range(n)):
        while True:
            run_command = ["./program.o"]
            try:
                result = subprocess.run(
                    run_command,
                    cwd=output_path,
                    capture_output=True,
                    text=True,
                    input=input,
                    timeout=300 - 4*options.number_of_threads + t
                )
            except subprocess.TimeoutExpired:
                t+=20
                print("Timeout expired")
                continue
            try:
                result.check_returncode()
            except subprocess.CalledProcessError as e:
                print(f"Error encountered {e.output}")
                continue
            output = result.stdout.strip()
            ALL_OUTPUTS[i] = output
            break

    return ALL_OUTPUTS

def TimeAverages(programFile, input, options, n):
    ALL_TIMING_INFOS = [None]*n
    builder.CompileProgram(programFile, options)
    t = 0
    for i in progressbar.progressbar(range(n)):
        while True:
            run_command = ["/usr/bin/time", "-v", "./program.o"]
            try:
                result = subprocess.run(
                    run_command,
                    cwd=output_path,
                    capture_output=True,
                    text=True,
                 input=input,
                    timeout=90 - options.number_of_threads + t
                )
            except subprocess.TimeoutExpired:
                t+=5
                print("Timeout expired")
                sleep(10)
                continue
            try:
                result.check_returncode()
            except subprocess.CalledProcessError as e:
                print(f"Error encountered {e.output}")
                continue
            output = result.stdout.strip()
            timing_info = parse_time_output(result.stderr.strip())
            ALL_TIMING_INFOS[i] = timing_info
            break

    return ALL_TIMING_INFOS

def parse_time_output(output: str) -> dict:
    result = {}
    lines = output.strip().splitlines()
    
    for line in lines:
        line = line.strip()
        if ": " in line:
            key, value = line.split(": ", 1)
            key = key.strip()
            value = value.strip()
            
          
            if re.match(r'^-?\d+(\.\d+)?$', value): 
                value = float(value) if '.' in value else int(value)
            elif value.endswith('%'):  # Percent
                try:
                    value = float(value.strip('%'))
                except ValueError:
                    pass
            result[key] = value
    
    return result

def getListOfTimes(timing_info: list[dict]) -> list[float]:
    #return list(map(lambda x: x['Elapsed (wall clock) time (h:mm:ss or m:ss)'], timing_info))
    timing_infos = []
    for info in timing_info:
        if 'Elapsed (wall clock) time (h:mm:ss or m:ss)' in info.keys():
            time = info['Elapsed (wall clock) time (h:mm:ss or m:ss)']
            time = time_str_to_seconds(time)
            timing_infos.append(time)
        else:
            print(info)
    return timing_infos

def time_str_to_seconds(time_str):
    try:
        minutes, seconds = time_str.split(':')
        total_seconds = int(minutes) * 60 + float(seconds)
        return total_seconds
    except ValueError:
        raise ValueError(f"Invalid time format: {time_str}")


if __name__ == "__main__":
    s = TimeAverages("fib.txt", "30", compiler_options(number_of_threads=20), 100)
    print(getListOfTimes(s))


