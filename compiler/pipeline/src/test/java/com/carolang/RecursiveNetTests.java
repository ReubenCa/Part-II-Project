// package com.carolang;

// import static org.junit.jupiter.api.Assertions.assertThrows;
// import static org.junit.jupiter.api.Assertions.assertTrue;

// import org.junit.jupiter.api.Test;

// import com.carolang.NetInerpreter.NetInterpreter;
// import com.carolang.NetInerpreter.TooLongToReduceException;
// import com.carolang.common.exceptions.MalformedProgramException;
// import com.carolang.common.interaction_rules.IProgram;
// import com.carolang.common.ast_nodes.Node;

// public class RecursiveNetTests {
//     @Test
//     public void Test1() throws MalformedProgramException {
//         testProgramOutput(1, "let rec f  = fun x -> if ((==) x 3) then 1 else f 3 in f 2");
//     }

//    //@Test TODO: There is a bug in the way function Arguments are handled in recursive functions - Look at branch FixingRecursiveFunctions
//     public void Test2() throws MalformedProgramException {
//        testProgramOutput(100, "(fun y->   (let rec f  = fun x -> if ((==) x 3) then y else f 3 in f 2)) 100");
//     }

//     @Test
//     public void Test3() throws MalformedProgramException {
//         testProgramOutput(10, "let rec f  = (fun y-> (fun x -> if ((==) y 3) then 10 else f 3 3)) in f 2 1");
//     }

//     @Test
//     public void Test3a() throws MalformedProgramException {
//         testProgramOutput(10, "let rec f  = (fun y-> (fun x -> if ((==) y 3) then 10 else f 3 3)) in f 3 3");
//         //(fun f -> f 3 3) (fun y -> (fun x -> if ((==) y 3) then 10 else REC 3 3)
//     }

//     @Test
//     public void Test3b() throws MalformedProgramException {
//         testProgramOutput(10, "let rec f  = (fun y-> (fun x -> 10)) in f 3 3");
//     }

//     @Test
//     public void Test3c() throws MalformedProgramException {
//         testProgramOutput(10, "let rec f  = (fun y-> (fun x -> if true then 10 else f 3 3)) in f 3 3");
//     }

//     @Test
//     public void Test3d() throws MalformedProgramException {
//         testProgramOutput(6, "let rec f  = (fun y -> (fun x -> (+) x y)) in f 3 3");
//     }

//     @Test
//     public void Test3e() throws MalformedProgramException {
//         testProgramOutput(10, "let rec f  = (fun y->  if ((==) y 3) then 10 else f 3 ) in f 3 ");
//     }

//     @Test
//     public void Test3f() throws MalformedProgramException {
//         testProgramOutput(10, "(fun y-> (fun x -> if ((==) y 3) then 10 else 4)) 3 3");
//     }

//     // @Test
//     // public void Test3a() throws MalformedProgramException {
//     //     testProgramOutput(1, "let rec f  = (fun y : int -> (fun x : int -> if true then x else (+) 1 (f y))) in f 2 1");
//     // }

//     @Test
//     public void Test4() throws MalformedProgramException {
//         String program = "let rec f = (fun x : int -> ((+) 1 (f x))) in f 5";

//         Node hoistedTree = TestUtils.stringToHoistedLambdaTree(program);

//         IProgram rules = new constructionProgram(hoistedTree);
//         assertThrows(TooLongToReduceException.class, () -> NetInterpreter.Interpret(rules));
//     }

//     //Test //Won't pass as interpreter doesn't support DUPing but by manual inspection seems to give a sensible answer
//     public void Test5() throws MalformedProgramException {
//         testProgramOutput(2, "let rec fib = (fun n -> if (==) n 0 then 1 else if " +
//                         "  (==) n 1 then 1 else ((+) (fib ((+) n (-1))) (fib ((+) n (-2))))) in fib 2");
//     }

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
