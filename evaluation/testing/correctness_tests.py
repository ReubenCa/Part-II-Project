from test_utils import assertCorrectAnswerInt, assertCorrectAnswerFloat, assertCorrectAnswerBool

def test_1():
    assertCorrectAnswerInt("(fun x -> 2) 4", 2)

def test_2():
    assertCorrectAnswerInt("(+) 100 200", 300)

def test_3():
    assertCorrectAnswerInt("(fun x -> x) 4", 4)

def test_4():
    assertCorrectAnswerInt("(fun x -> x) ((fun y -> y) 5)", 5)

def test_5():
    assertCorrectAnswerInt("(fun x -> x) (fun y -> y) 5", 5)

def test_6():
    assertCorrectAnswerInt("(fun x -> x) 5", 5)

def test_7():
    assertCorrectAnswerInt("(fun x -> x) ((fun y -> y) 5)", 5)

def test_7a():
    assertCorrectAnswerInt("(fun x -> x) (fun y -> y) (fun z -> z) 5", 5)

def test_8():
    program = "5"
    for _ in range(10):
        assertCorrectAnswerInt(program, 5)
        program = f"(fun x-> x) {program}"

def test_9():
    assertCorrectAnswerInt("(fun x -> (+) x 1) 5", 6)

def test_9a():
    assertCorrectAnswerInt("(fun x -> (fun y -> x)) 1 2", 1)

def test_10():
    assertCorrectAnswerInt("(fun y -> (fun x -> (+) x y)) 3 5", 8)

def test_11():
    assertCorrectAnswerInt("(fun z -> (fun y -> (fun x -> (+) ((+) x y) z))) 3 5 2", 10)

def test_12b():
    assertCorrectAnswerInt("(fun f -> f 1) (fun x -> x)", 1)

def test_12a():
    assertCorrectAnswerInt("let identity = (fun x->x) in identity 1", 1)

def test_12d():
    assertCorrectAnswerInt("(fun f -> f 3 5) (+)", 8)

def test_12e():
    assertCorrectAnswerInt("(fun f -> f 3 5) (fun y -> (fun x -> (+) x y))", 8)

def test_12f():
    assertCorrectAnswerInt("let add = (fun y -> (fun x -> (+) x y)) in add 3 5", 8)

def test_13a():
    assertCorrectAnswerInt("(fun f -> f 1) (+) 2", 3)

def test_13b():
    assertCorrectAnswerInt("let f = (fun g -> g 1) in (f (+)) 2", 3)

def test_14():
    assertCorrectAnswerInt("(fun g -> g 2) ((+) 1)", 3)

def test_15():
    assertCorrectAnswerInt("(fun g -> fun h -> (g h 1)) (+) 2", 3)

def test_16():
    assertCorrectAnswerInt("(fun h -> ((+) h 1)) 2", 3)

def test_17():
    assertCorrectAnswerInt("(fun g -> (fun h -> 1)) (+) 2", 1)

def test_18():
    assertCorrectAnswerInt("(fun a-> (fun b -> (fun c -> (+) a ((+) b c)))) 1 2 3", 6)

def test_19c():
    assertCorrectAnswerInt("(fun addOne -> (fun f -> f 1) addOne) (fun a-> (+) a 0)", 1)

def test_19b():
    assertCorrectAnswerInt("(fun f -> f 1 2 ) (fun a-> (fun b -> (+) a b))", 3)



def test_20():
    assertCorrectAnswerInt("let addThree = (fun a-> (fun b -> (fun c -> (+) a ((+) b c)))) in (fun f -> f 1 2 3) addThree", 6)

def test_20a():
    assertCorrectAnswerInt("(fun addThree -> (fun f -> f 1 2 3) addThree) (fun a-> (fun b -> (fun c -> (+) a ((+) b c))))",6)

def test_20b():
    assertCorrectAnswerInt("(fun addTwo -> (fun f -> f 1 2 ) addTwo) (fun a-> (fun b ->  (+) a b ))",3)

def test_20c():
    assertCorrectAnswerInt("(fun addTwo -> (fun f -> f 1 2 ) addTwo) (+)",3)

def test_20d():
    assertCorrectAnswerInt("(fun f -> f 1 2) (fun a-> (fun b ->  (+) a b ))",3)

def test_20e():
    assertCorrectAnswerInt("(fun f -> f 1 2) (+)",3)

def test_21():
    assertCorrectAnswerInt("(fun f -> f) (fun x -> (+) x 1) 5", 6)

def test_22a():
    assertCorrectAnswerInt("let five = 5 in let three = 3 in (+) five three", 8)


def test_22c():
    assertCorrectAnswerInt("(fun s -> (s (fun z -> (+) 2 z)) 5) ((fun f -> (fun x -> f ((fun k -> (+) k 1) x))))", 8)


def test_22d():
    assertCorrectAnswerInt("(((fun f -> (fun x -> f ((fun k -> (+) k 1) x))) (fun z -> (+) 2 z)) 5)", 8)

def test_22e():
    assertCorrectAnswerInt("(((fun x -> (fun z -> (+) 2 z) ((fun k -> (+) k 1) x)))) 5", 8)

def test_23():
    assertCorrectAnswerInt("(fun f -> (fun x -> f ((+) x 1))) (fun x -> x) 5", 6)

def test_24():
    assertCorrectAnswerInt("(fun f -> (fun x -> f ((+) x 1))) (fun x -> x) 5", 6)

def test_25():
    assertCorrectAnswerInt("(fun f -> (fun x -> f ((+) x 1))) (fun x -> (+) x 2) 5", 8)

def test_26():
    assertCorrectAnswerInt("(fun s -> (fun f -> (fun x -> f (s x))) (fun k -> (+) k 2) 5) (fun z -> (+) z 1)", 8)

def test_27():
    assertCorrectAnswerInt("(fun s -> (fun x -> (fun k -> (+) k 2) (s x)) 5) (fun z -> (+) z 1)", 8)

def test_28():
    assertCorrectAnswerInt("(fun s -> (fun x -> (+) (s x) 2) 5) (fun z -> (+) z 1)", 8)

def test_29():
    assertCorrectAnswerInt("(fun s -> s 5) (fun z -> (+) z 1)", 6)

def test_30():
    assertCorrectAnswerInt("(fun f -> (fun x -> f x)) (fun z -> z) 1", 1)

def test_31():
    assertCorrectAnswerBool("(==) 5 5", True)

def test_32():
    assertCorrectAnswerInt("(fun x-> (fun y -> (fun z -> (+) x ((+) z y)))) 1 2 3", 6)

def test_33():
    assertCorrectAnswerInt("(fun x-> (fun y -> (fun z -> (+) x ((+) z y)))) (-1) (-2) 3", 0)

def test_34():
    assertCorrectAnswerInt("(-) 10 6", 4)

def test_34():
    assertCorrectAnswerInt("(fun x -> (fun y -> (-) y x)) 6 10", 4)

#def test_34():
    #assertCorrectAnswer("(fun f -> (fun y -> (f (f y)))) (fun x -> (+) x 1) 2", 4)