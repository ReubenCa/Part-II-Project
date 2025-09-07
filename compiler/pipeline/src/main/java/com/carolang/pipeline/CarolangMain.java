package com.carolang.pipeline;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.carolang.ASTtoNetRules;
import com.carolang.IOptimiser;
import com.carolang.LambdaNetToC;
import com.carolang.NoOptimisations;
import com.carolang.fullOptimiser;
import com.carolang.smallOptimiser;
import com.carolang.TypeInferer;
import com.carolang.asttolambdatree.App;
import com.carolang.common.types.Type;
import com.carolang.common.exceptions.MalformedProgramException;
import com.carolang.common.interaction_rules.ProgramBase;
import com.carolang.common.ast_nodes.Node;
import com.carolang.frontend.carolangLexer;
import com.carolang.frontend.carolangParser;

public class CarolangMain {

    @Parameter(names = { "-r", "--runtime" }, description = "Path to the runtime folder")
    String runtimeFolder;

    @Parameter(names = { "-n",
            "--nets" }, description = "Optional path to file to output json file specifying the generated program")
    String JsonFileOutput = null;

    @Parameter(names = {
            "--debugnets" }, description = "Optional path to file to output json file specifying the program before some of the finishing touches and optimisation have beein applied")
    String debugJsonFileOutput = null;

    @Parameter(names = { "-o" }, description = "Output file name")
    String outputFileNameRelevantToCompiler;

    @Parameter(names = { "-t", "--threads" }, description = "Number of threads to use")
    int numThreads = 1;

    @Parameter(names = { "--INITIAL_BUFFER_SIZE" }, description = "Initial buffer size")
    int initialBufferSize = 1024;

    @Parameter(names = {
            "--MIN_QUEUE_SIZE_TO_ATTEMPT_STEAL" }, description = "Minimum queue size to attempt steal")
    int minQueueSizeToAttemptSteal = 6;

    @Parameter(names = {
            "--MIN_QUEUE_SIZE_TO_ACTUALLY_STEAL" }, description = "Minimum queue size to actually steal")
    int minQueueSizeToActuallySteal = 2;

    @Parameter(names = { "--MAX_ITEMS_TO_STEAL" }, description = "Maximum number of items to steal")
    int maxItemsToSteal = 128;

    @Parameter(names = { "--FRACTION_TO_STEAL" }, description = "Fraction of items to steal")
    int fractionToSteal = 2;

    @Parameter(names = { "-d",
            "--debug" }, description = "Enable debug mode, compiles the C unoptimized with logs being written to stdout.")
    boolean debug_mode = false;

    @Parameter(names = { "--RECORD_WORK_PER_THREAD"})
    boolean recordWorkPerThread = false;

    @Parameter(names = {"--NoOptimise"})
    boolean noOptimise = false;

    @Parameter(names = {"--SmallOptimise"})
    boolean smallOptimise = false;

    @Parameter(names = {"--RECORD_THREAD_STALLS"})
    boolean recordThreadStalls = false;

    @Parameter(names = {"--RECORD_QUEUE_SIZE"})
    boolean recordQueueSize = false;

    @Parameter(names = {"--EXPERIMENTAL_MEMORY_MANAGER"})
    boolean experimentalMemoryManager = false;

    public static void main(String[] args) throws IOException, InterruptedException {

        CarolangMain Pipeline = new CarolangMain();
        try
        {
                    
        JCommander.newBuilder()
                .addObject(Pipeline)
                .build()
                .parse(args);
        }
        catch(ParameterException e)
        {
            System.out.println("Invalid Command Line Argument used\n");
            e.getJCommander().usage();
            System.exit(1);
            
        }

        CharStream input = CharStreams.fromStream(System.in);

        try {
            Pipeline.Pipeline(input);
        } catch (MalformedProgramException e) {
            System.out.println("Compilation Error:\n" + e.getError());
            System.exit(2);
        }
    }

    private void Pipeline(CharStream input) throws MalformedProgramException, IOException, InterruptedException {

        carolangLexer lexer = new carolangLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        carolangParser parser = new carolangParser(tokens);
        ParseTree tree = parser.carolang();// begin parsing at init rule - NOTE THIS FUNCTION NAME IS DECIDED BY START
                                           // SYMBOL OF YOUR GRAMMAR
        // System.out.println(tree.toStringTree(parser));
        Node lambdaTree = App.ASTtoLambdaTree(tree);
        TypeInferer.InferTypes(lambdaTree);
        Type programType = lambdaTree.getType();
        System.out.println("Program type: " + programType);
      //  Node hoistedTree = LambdaHoister.LambdaHoist(lambdaTree);
        ProgramBase program = ASTtoNetRules.ASTtoNetRules(lambdaTree, programType, debugJsonFileOutput);// When program Type is function
                                                                                    // will need to split into input and


        IOptimiser optimiser;
        if(noOptimise)
        {
            optimiser = new NoOptimisations();
        }
        else if(smallOptimise)
        {
            optimiser = new smallOptimiser();
        }
        else
        {
            optimiser = new fullOptimiser();
        }
        
     
        ProgramBase optimisedProgram = optimiser.Optimise(program);

        if (JsonFileOutput != null) {
            String jsonToWrite = optimisedProgram.toString();
            Path path = Path.of(JsonFileOutput);
            Files.writeString(path, jsonToWrite, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        String Ccode = LambdaNetToC.GenerateCCode(optimisedProgram, programType);

        // Path path = Path.of(runtimeFolder, "build", "compilerOutput.c");
        Path fileToWriteCProgramIn;
        if (debug_mode) {
            fileToWriteCProgramIn = Path.of(runtimeFolder, "build", "compilerOutput.c");
        } else {
            fileToWriteCProgramIn = Files.createTempFile("compilerOutput", ".c");
        }

        Files.writeString(fileToWriteCProgramIn, Ccode, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

        StringBuilder makeCommand = new StringBuilder();
        makeCommand.append("make");
        if (debug_mode) {
            makeCommand.append(" release_debug_mode ");
        } else {
            makeCommand.append(" release ");
        }

        makeCommand.append("COMPILER_OUTPUT=%s ".formatted(fileToWriteCProgramIn.toAbsolutePath().toString()));

        File file = new File(outputFileNameRelevantToCompiler);
        String absolutePath;
        if (file.isAbsolute()) {
            absolutePath = file.getPath();
        } else {
            absolutePath = file.getAbsolutePath();
        }
        makeCommand.append("OUTDIR=%s".formatted(absolutePath));

        makeCommand.append(" NUMBER_OF_THREADS=%d ".formatted(numThreads));

        makeCommand.append(" INITIAL_BUFFER_SIZE=%d ".formatted(initialBufferSize));

        makeCommand.append(" MIN_QUEUE_SIZE_TO_ATTEMPT_STEAL=%d ".formatted(minQueueSizeToAttemptSteal));

        makeCommand.append(" MIN_QUEUE_SIZE_TO_ACTUALLY_STEAL=%d ".formatted(minQueueSizeToActuallySteal));

        makeCommand.append(" MAX_ITEMS_TO_STEAL=%d ".formatted(maxItemsToSteal));

        makeCommand.append(" FRACTION_TO_STEAL=%d ".formatted(fractionToSteal));
        
        if(recordWorkPerThread)
        {
            makeCommand.append(" RECORD_WORK_PER_THREAD=1 ");
        }
        if(recordThreadStalls)
        {
            makeCommand.append(" RECORD_THREAD_STALLS=1 ");
        }
        if(recordQueueSize)
        {
            makeCommand.append(" RECORD_QUEUE_SIZE=1 ");
        }
        if(experimentalMemoryManager)
        {
            makeCommand.append(" EXPERIMENTAL_MEMORY_MANAGER=1 ");
        }
        //if (debug_mode) {
            System.out.println("Running Command \"%s\" ".formatted(makeCommand));
       // }
        File workingDir = new File(runtimeFolder);
        Process p = Runtime.getRuntime().exec(makeCommand.toString(), null, workingDir);
        p.waitFor();
        // System.out.println("Current working directory: " +
        // Paths.get("").toAbsolutePath());

        // It was convenient to have the compiler also run the code and output it while
        // developing
        // File workingDir = new File(runtimeFolder);
        // Process p = Runtime.getRuntime().exec("make debug", null, workingDir);

        // try (BufferedReader reader = new BufferedReader(new
        // InputStreamReader(p.getInputStream()));
        // BufferedReader errorReader = new BufferedReader(new
        // InputStreamReader(p.getErrorStream()))) {

        // String line;
        // while ((line = reader.readLine()) != null) {
        // System.out.println("STDOUT: " + line);
        // }
        // while ((line = errorReader.readLine()) != null) {
        // System.err.println("STDERR: " + line);
        // }
        // }
        // p.waitFor();

        System.exit(p.exitValue());

    }

}
