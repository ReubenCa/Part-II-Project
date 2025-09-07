import subprocess
import os


root_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
compiler_dir = os.path.join(root_dir, 'compiler')
def BuildCompiler():
    
    command = ['mvn', 'package']
    subprocess.run(command, cwd=compiler_dir, stdout=subprocess.DEVNULL)


def CompileProgramCommon(input_source, is_file, compiler_options):
    jar_path = os.path.join('pipeline', 'target', 'pipeline-1.0-SNAPSHOT-jar-with-dependencies.jar')
    runtime_path = os.path.join('..', 'runtime')
    output_file_path = os.path.join(runtime_path, 'build', 'program.o')

    command = ['java', '-ea', '-jar', jar_path, '--runtime', runtime_path, '-o', output_file_path]
    command += compiler_options.getArgumentsForCompiler()

    print(f"Running: ({command})")
    if is_file:
        with open(input_source, 'r') as infile:
            result = subprocess.run(command, cwd=compiler_dir, stdin=infile)
    else:
        result = subprocess.run(command, cwd=compiler_dir, input=input_source, text=True)

    if result.returncode != 0:
        raise RuntimeError(f"Compilation failed with exit code {result.returncode}")

def CompileProgram(programFileName, compiler_options=None):
    input_file_path = os.path.join(os.path.dirname(__file__), 'Programs', programFileName)
    CompileProgramCommon(input_file_path, True, compiler_options)

def CompileProgramFromString(ProgramString, compiler_options=None):
    CompileProgramCommon(ProgramString, False, compiler_options)


def getAnswerOutputFromString(ProgramString,compiler_options):
    CompileProgramFromString(ProgramString, compiler_options)
    runtime_path = os.path.join(root_dir, 'runtime')
    #buildCommand = ['make', 'optimized']
    
   # subprocess.run(buildCommand, cwd=runtime_path, check=True)
    
    program_path = os.path.join(runtime_path, 'build')
    run_command = ['./program.o']

    result = subprocess.run(
        run_command,
        cwd=program_path,
        capture_output=True,
        text=True
    )
    
    result.check_returncode()
    
    return result.stdout.strip()


BuildCompiler()