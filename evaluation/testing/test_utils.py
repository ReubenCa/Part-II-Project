
from os import sys, path
sys.path.append(path.dirname(path.dirname(path.abspath(__file__))))

import builder
from compiler_options import compiler_options

def assertCorrectAnswerInt(program, expected_output : int):
    output = builder.getAnswerOutputFromString(program, compiler_options(1))
    assert int(output) == expected_output, f"Expected {expected_output}, but got {output} for program: {program}"

def assertCorrectAnswerFloat(program, expected_output : float):
    output = builder.getAnswerOutputFromString(program, compiler_options(1))
    assert float(output) == expected_output, f"Expected {expected_output}, but got {output} for program: {program}"

def assertCorrectAnswerBool(program, expected_output : bool):
    output = builder.getAnswerOutputFromString(program, compiler_options(1))
    assert (output == 'true') == expected_output, f"Expected {expected_output}, but got {output} for program: {program}"

if __name__ == "__main__":
    assertCorrectAnswerInt("1", 1)
    