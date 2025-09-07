// package com.carolang;

// import static org.junit.jupiter.api.Assertions.assertTrue;

// import org.junit.jupiter.api.Test;

// import com.carolang.NetInerpreter.TooLongToReduceException;
// import com.carolang.common.exceptions.MalformedProgramException;
// import com.carolang.common.interaction_rules.IProgram;
// import com.carolang.common.ast_nodes.Node;

// public class IfStatementTests {

//     @Test
//     public void Test1() throws MalformedProgramException {
//         testProgramOutput(3, "if true then 3 else 2");
//     }

//     @Test
//     public void Test2() throws MalformedProgramException {
//         testProgramOutput(5, "if true then ((+) 4 1) else 2");
//     }

//     @Test
//     public void Test3() throws MalformedProgramException {
//         testProgramOutput(5, "if true then ((+) 4 1) else ((+) 1 1)");
//     }

//     @Test
//     public void Test4() throws MalformedProgramException {
//         testProgramOutput(5, "(fun x -> if true then x else 3) 5");
//     }

//     @Test
//     public void Test5() throws MalformedProgramException {
//         testProgramOutput(5, " ((fun y ->(fun x -> if true then x else y) 5) 3)");
//     }

//     @Test
//     public void Test6() throws MalformedProgramException {
//         testProgramOutput(7, " ((fun y ->(fun x -> if true then x else y) ((+) 5 2)) 3)");
//     }

//     @Test
//     public void Test7() throws MalformedProgramException {
//         testProgramOutput(4, " (fun y -> (fun x -> if true then ((+) x 1) else y)) ((+) 5 2) 3");
//     }

//     @Test
//     public void Test8() throws MalformedProgramException {
//         testProgramOutput(7, " (fun y -> (fun x -> if false then ((+) x 1) else y)) ((+) 5 2) 3");
//     }

//     @Test
//     public void Test9() throws MalformedProgramException {
//         testProgramOutput(2, "if false then 3 else 2");
//     }


//     @Test
//     public void Test10() throws MalformedProgramException {
//         testProgramOutput(5, "if true then " +
//         "if true then 5 else 1" +
//          " else 0");
//     }

//     @Test
//     public void Test11() throws MalformedProgramException {
//         testProgramOutput(99, "if false then " +
//         "if true then 5 else 1" +
//          " else if true then 99 else 100");
//     }

//     @Test
//     public void Test12() throws MalformedProgramException {
//         testProgramOutput(99, "(fun x -> (if false then if true then 5 else 1 else if true then x else 100)) 99");
//     }

//     @Test
//     public void Test12a() throws MalformedProgramException {
//         testProgramOutput(99, "(fun x -> (if false then 1 else if true then x else 100)) 99");
//     }

//     @Test
//     public void Test13() throws MalformedProgramException
//     {
//         testProgramOutput(6, "(if true then (fun x -> (+) x 1) else (fun y -> (+) y 2)) 5");
//     }

//     @Test
//     public void Test14() throws MalformedProgramException
//     {
//         testProgramOutput(15, "(fun y -> (if true then (fun x -> (+) x y) else (fun y -> (+) y 2))) 5 10");
//     }

//     @Test
//     public void Test15() throws MalformedProgramException
//     {
//         testProgramOutput(1, "if ((==) 5 5) then 1 else 2");
//     }

//     @Test
//     public void Test16() throws MalformedProgramException
//     {
//         testProgramOutput(2, "if ((==) 6 5) then 1 else 2");
//     }

//     @Test
//     public void Test17() throws MalformedProgramException
//     {
//         testProgramOutput(1, "(fun x -> (if ((==) x 5) then 1 else 2)) 5");
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
