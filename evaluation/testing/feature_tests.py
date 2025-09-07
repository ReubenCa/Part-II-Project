from test_utils import assertCorrectAnswerInt, assertCorrectAnswerFloat, assertCorrectAnswerBool
#Generated with help from ChatGPT

# Integer tests
def test_int():
    assertCorrectAnswerInt("1", 1)

def test_int_add():
    assertCorrectAnswerInt("(+) 1 2", 3)

def test_int_sub():
    assertCorrectAnswerInt("(-) 5 3", 2)

def test_int_mul():
    assertCorrectAnswerInt("(*) 4 3", 12)

def test_int_div():
    assertCorrectAnswerInt("(/) 10 2", 5)

def test_int_mod():
    assertCorrectAnswerInt("(mod) 10 3", 1)

def test_int_eq_true():
    assertCorrectAnswerBool("(==) 4 4", True)

def test_int_neq_false():
    assertCorrectAnswerBool("(!=) 4 4", False)

def test_int_neq_true():
    assertCorrectAnswerBool("(!=) 5 4", True)

def test_int_eq_false():
    assertCorrectAnswerBool("(==) 4 5", False)

def test_int_lt():
    assertCorrectAnswerBool("(<) 3 5", True)

def test_int_gt():
    assertCorrectAnswerBool("(>) 5 3", True)

def test_int_le():
    assertCorrectAnswerBool("(<=) 4 4", True)

def test_int_ge():
    assertCorrectAnswerBool("(>=) 5 2", True)

# Float tests
def test_float():
    assertCorrectAnswerFloat("1.0", 1.0)

def test_float():
    assertCorrectAnswerFloat("2.0f", 2.0)

def test_float_add():
    assertCorrectAnswerFloat("(+.) 1.5 2.5", 4.0)

def test_float_sub():
    assertCorrectAnswerFloat("(-.) 5.5 2.0", 3.5)

def test_float_mul():
    assertCorrectAnswerFloat("(*.) 2.0 3.0", 6.0)

def test_float_div():
    assertCorrectAnswerFloat("(/.) 7.5 2.5", 3.0)

def test_float_eq_true():
    assertCorrectAnswerBool("(==.) 3.5 3.5", True)

def test_float_eq_false():
    assertCorrectAnswerBool("(==.) 3.5 3.6", False)

def test_int_neq_false():
    assertCorrectAnswerBool("(!=.) 4.0 4.0", False)

def test_int_neq_true():
    assertCorrectAnswerBool("(!=.) 5.0 4.0", True)

def test_float_lt():
    assertCorrectAnswerBool("(<.) 1.1 2.2", True)

def test_float_gt():
    assertCorrectAnswerBool("(>.) 3.3 2.2", True)

def test_float_le():
    assertCorrectAnswerBool("(<=.) 2.2 2.2", True)

def test_float_ge():
    assertCorrectAnswerBool("(>=.) 4.4 4.0", True)