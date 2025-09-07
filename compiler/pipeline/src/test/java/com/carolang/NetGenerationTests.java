// package com.carolang;

// import static org.junit.jupiter.api.Assertions.assertTrue;

// import org.junit.jupiter.api.Test;

// import com.carolang.NetInerpreter.NetInterpreter;
// import com.carolang.NetInerpreter.TooLongToReduceException;
// import com.carolang.common.exceptions.MalformedProgramException;
// import com.carolang.common.interaction_rules.IProgram;
// import com.carolang.common.ast_nodes.Node;

// public class NetGenerationTests {
//    @Test
//    public void Test1() throws MalformedProgramException {
//       testProgramOutput(2, "(fun x -> 2) 4");
//    }

//    @Test
//    public void Test2() throws MalformedProgramException {
//       Node hoistedTree = TestUtils.stringToHoistedLambdaTree("(fun x -> + x x)");
//       IProgram rules = new constructionProgram(hoistedTree);
//       System.out.println(rules.toString());
//    }

//    @Test
//    public void Test3() throws MalformedProgramException {
//       testProgramOutput(4, "(fun x -> x) 4");
//    }

//    @Test
//    public void Test4() throws MalformedProgramException {
//       testProgramOutput(5, "(fun x -> x)  ((fun y -> y) 5)");
//    }

//    @Test
//    public void Test5() throws MalformedProgramException {
//       testProgramOutput(5, "(fun x -> x)  (fun y -> y) 5");

//    }

//    @Test
//    public void Test6() throws MalformedProgramException {
//       testProgramOutput(5, "(fun x -> x) 5");
//    }

//    @Test
//    public void Test7() throws MalformedProgramException {
//       testProgramOutput(5, "(fun x -> x)  ((fun y -> y) 5)");
//    }

//    @Test
//    public void Test7a() throws MalformedProgramException {
//       testProgramOutput(5, "(fun x -> x)  (fun y -> y) (fun z -> z) 5");
//    }

//    @Test
//    public void Test8() throws MalformedProgramException {
//       StringBuilder Program = new StringBuilder("5");
//       for (int i = 0; i < 10; i++) {
//          testProgramOutput(5, Program.toString());
//          Program.insert(0, "(fun x-> x)");
//       }
//    }

//    @Test
//    public void Test9() throws MalformedProgramException {
//       testProgramOutput(6, "(fun x -> (+) x 1)  5");
//    }

//    @Test
//    public void Test9a() throws MalformedProgramException {
//       testProgramOutput(1, "(fun x -> (fun y -> x)) 1 2");
//    }

//    @Test
//    public void Test10() throws MalformedProgramException {
//       testProgramOutput(8, "(fun y -> (fun x -> (+) x y)) 3 5");
//    }

//    @Test
//    public void Test11() throws MalformedProgramException {
//       testProgramOutput(10, "(fun z -> (fun y -> (fun x -> (+) ((+) x y) z))) 3 5 2");
//    }

//    @Test
//    public void Test12b() throws MalformedProgramException
//    {
//       testProgramOutput(1, "(fun f -> f 1) (fun x -> x)" );
//    }

//    @Test
//    public void Test12a() throws MalformedProgramException {
//       testProgramOutput(1, "let identity = (fun x->x) in identity 1");
//    }

//    @Test
//    public void Test12d() throws MalformedProgramException {
//       testProgramOutput(8, "(fun f -> f 3 5) (+)");
//    }

//    @Test
//    public void Test12e() throws MalformedProgramException {
//       testProgramOutput(8, "(fun f -> f 3 5) (fun y -> (fun x -> (+) x y))");
//    }

//    @Test
//    public void Test12f() throws MalformedProgramException {
//       testProgramOutput(8, "let add = (fun y -> (fun x -> (+) x y)) in add 3 5");
//       /*(fun f -> f 3 5) (fun y -> (fun x -> (+) x y)) */
//    }

//    @Test
//    public void Test13a() throws MalformedProgramException {
//       testProgramOutput(3, "(fun f -> f 1) (+) 2");
     
//    }

//    @Test
//    public void Test13b() throws MalformedProgramException {
//       testProgramOutput(3, "let f = (fun g -> g 1) in (f (+)) 2");
//    }

//    @Test
//    public void Test14() throws MalformedProgramException {
//       testProgramOutput(3, "(fun g -> g 2) ((+) 1)  ");
//    }

//    @Test
//    public void Test15() throws MalformedProgramException {
//       testProgramOutput(3, "(fun g -> fun h -> (g h 1)) (+) 2");
//    }

//    @Test
//    public void Test16() throws MalformedProgramException {
//       testProgramOutput(3, "(fun h -> ((+) h 1)) 2");
//    }

//    @Test
//    public void Test17() throws MalformedProgramException {
//       testProgramOutput(1, "(fun g -> (fun h -> 1)) (+) 2");
//    }

//    @Test
//    public void Test18() throws MalformedProgramException {
//       testProgramOutput(6, "(fun a-> (fun b -> (fun c -> (+) a ((+) b c)))) 1 2 3");
//    }

//    //@Test
//    public void Test19() throws MalformedProgramException
//    {
//       testProgramOutput(3, "let addTwo = (fun a-> (fun b -> (+) a b)) in (fun f -> f 1 2 ) addTwo");
//    }

//    //@Test
//    public void Test19a() throws MalformedProgramException
//    {
//       testProgramOutput(3, "(fun addTwo -> (fun f -> f 1 2 ) addTwo) (fun a-> (fun b -> (+) a b))");
//    }

//    @Test
//    public void Test19c() throws MalformedProgramException
//    {
//       testProgramOutput(1, "(fun addOne -> (fun f -> f 1) addOne) (fun a-> (+) a 0)");
//    }

//    @Test
//    public void Test19b() throws MalformedProgramException
//    {
//       testProgramOutput(3, "(fun f -> f 1 2 ) (fun a-> (fun b -> (+) a b)) ");
//    }


//    //@Test Tests 19, 19a and 20 show that inlining is actually necessary and its only the interpreter doing reductions in a lucky order that has led to them passing
//    //(ie they actually lacked confluence)
//    public void Test20() throws MalformedProgramException
//    {
//       testProgramOutput(6, "let addThree = (fun a-> (fun b -> (fun c -> (+) a ((+) b c)))) in (fun f -> f 1 2 3) addThree");
//    }

  

//    @Test
//    public void Test21() throws MalformedProgramException
//    {
//       testProgramOutput(6,  "(fun f -> f) (fun x -> (+) x 1) 5");
//    }


//   // @Test
//    public void Test22() throws MalformedProgramException
//    {
//       testProgramOutput(7,  " let succ = (fun x ->(+) x 1) in "+
//       "(let succfun = (fun f -> (fun x -> f (succ x))) in (succfun (fun z -> (+) 2 z))  4)");
//    }

//    @Test
//    public void Test22a() throws MalformedProgramException
//    {
//       testProgramOutput(8,  "let five = 5 in let three = 3 in (+) five three");
//    }

//   // @Test
//    public void Test22b() throws MalformedProgramException
//    {
//       testProgramOutput(8,  "(fun succ -> (fun succfun -> (succfun (fun z -> (+) 2 z)) 5) "+
//       "((fun f -> (fun x -> f (succ x)))) ) (fun x -> (+) x 1)");
//    }

//    //@Test
//    public void Test22c() throws MalformedProgramException
//    {
//       testProgramOutput(8,   "(fun s -> (s (fun z -> (+) 2 z)) 5)" + 
//       "((fun f -> (fun x -> f ((fun k -> (+) k 1) x))))");
//    }

//    @Test
//    public void Test22d() throws MalformedProgramException
//    {
//       testProgramOutput(8,   "( ((fun f -> (fun x -> f ((fun k -> (+) k 1) x)))) (fun z -> (+) 2 z)) 5");
//    }

//    @Test
//    public void Test22e() throws MalformedProgramException
//    {
//       testProgramOutput(8,   " ((( (fun x -> (fun z -> (+) 2 z) ((fun k -> (+) k 1) x))))) 5");
//    }

//    @Test
//    public void Test23() throws MalformedProgramException
//    {
//       testProgramOutput(6,  "(fun f -> (fun x -> f ((+) x 1))) (fun x -> x) 5");
//    }

//    @Test
//    public void Test24() throws MalformedProgramException
//    {
//       testProgramOutput(6,  "(fun f -> (fun x -> f ((+) x 1))) (fun x -> x) 5");
//    }

//    @Test
//    public void Test25() throws MalformedProgramException
//    {
//       testProgramOutput(8,  "(fun f -> (fun x -> f ((+) x 1))) (fun x -> (+) x 2) 5");
//    }

//    @Test
//    public void Test26() throws MalformedProgramException
//    {
//       testProgramOutput(8,  "(fun s -> (fun f -> (fun x -> f (s x))) (fun k -> (+) k 2) 5) (fun z -> (+) z 1)");
//    }

//    @Test
//    public void Test27() throws MalformedProgramException
//    {
//       testProgramOutput(8,  "(fun s ->(fun x -> (fun k -> (+) k 2) (s x))  5) (fun z -> (+) z 1)");
//    }

//    @Test
//    public void Test28() throws MalformedProgramException
//    {
//       testProgramOutput(8,  "(fun s -> (fun x -> (+)  (s x) 2)  5) (fun z -> (+) z 1)");
//    }

//    @Test
//    public void Test29() throws MalformedProgramException
//    {
//       testProgramOutput(6,  "(fun s -> s 5) (fun z -> (+) z 1)");
//    }

//    @Test void Test30() throws MalformedProgramException
//    {
//       testProgramOutput(1,  "(fun f -> (fun x -> f x)) (fun z -> z) 1");
//    }

//    //Interpreter doesn't yet support duplication


//    // @Test
//    // public void Test22() throws MalformedProgramException
//    // {
//    //    testProgramOutput(6,  "let double = (fun f -> (fun x -> f (f x))) in double (fun y -> (+) y 1) 3;;");
//    // }


//    // @Test
//    // public void Test23() throws MalformedProgramException
//    // {
//    //    test(fun succ -> (fun succfun -> (succfun (fun z -> (+) 2 z)) 5) ((fun f -> (fun x -> f (succ x)))) ) (fun x -> (+) x 1)
//    // public void Test24() throws MalformedProgramException
//    // {
//    //    testProgramOutput(2,  "(fun x -> (+) x x) 1");
//    // }

//    @Test 
//    void Test31() throws MalformedProgramException
//    {
//       testProgramOutput(1,  "(==) 5 5");
//    }

//    @Test
//    void Test32() throws MalformedProgramException
//    {
//       testProgramOutput(6, "( fun x-> (fun y -> (fun z -> (+) x  ((+) z y) ) ) ) 1 2 3");
//    }

//    @Test
//    void Test33() throws MalformedProgramException
//    {
//       testProgramOutput(0, "( fun x-> (fun y -> (fun z -> (+) x  ((+) z y) ) ) ) (-1) (-2) 3");
//    }

   


//     private void testProgramOutput(int expected, String program) throws MalformedProgramException {
//         Node hoistedTree = TestUtils.stringToHoistedLambdaTree(program);

//         IProgram rules = new constructionProgram(hoistedTree);
//         try {
//             Integer result = NetInterpreter.Interpret(rules);
//             assertTrue(result == expected);
//         } catch (TooLongToReduceException e) {
//             throw new RuntimeException();
//         }
//     }
// }
