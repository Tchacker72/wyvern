package wyvern.tools.tests.tagTests;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.Optional;

import org.junit.Assert;

import edu.umn.cs.melt.copper.runtime.logging.CopperParserException;
import wyvern.stdlib.Globals;
import wyvern.tools.errors.ErrorMessage;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.errors.ToolError;
import wyvern.tools.imports.extensions.WyvernResolver;
import wyvern.tools.parsing.Wyvern;
import wyvern.tools.parsing.coreparser.ParseException;
import wyvern.tools.parsing.coreparser.ParseUtils;
import wyvern.tools.parsing.coreparser.Token;
import wyvern.tools.parsing.coreparser.WyvernParser;
import wyvern.tools.parsing.coreparser.WyvernParserConstants;
import wyvern.tools.typedAST.core.expressions.TaggedInfo;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.typedAST.interfaces.Value;
import wyvern.tools.types.Type;

public class TestUtil {
	public static final String BASE_PATH = "src/wyvern/tools/tests/";
	public static final String STDLIB_PATH = BASE_PATH + "stdlib/";
	public static final String LIB_PATH = "src/wyvern/lib/";
	private static final String PLATFORM_PATH = BASE_PATH + "platform/java/stdlib/";
	
	/** Sets up the standard library and platform paths in the Wyvern resolver
	 * 
	 */
	public static void setPaths() {
    	WyvernResolver.getInstance().resetPaths();
		WyvernResolver.getInstance().addPath(STDLIB_PATH);
		WyvernResolver.getInstance().addPath(PLATFORM_PATH);
	}

	/**
	 * Converts the given program into the AST representation.
	 * 
	 * @param program
	 * @return
	 * @throws IOException 
	 * @throws CopperParserException 
	 */
	public static TypedAST getAST(String program) throws CopperParserException, IOException {
		clearGlobalTagInfo();
		return (TypedAST)new Wyvern().parse(new StringReader(program), "test input");
	}
	
	/**
	 * Converts the given program into the TypedAST representation, using the
	 * new Wyvern parser.
	 * 
	 * @param program
	 * @param programName TODO
	 * @return
	 * @throws IOException 
	 * @throws CopperParserException 
	 */
	public static TypedAST getNewAST(String program, String programName) throws ParseException {
		clearGlobalTagInfo();
		Reader r = new StringReader(program);
		WyvernParser<TypedAST,Type> wp = ParseUtils.makeParser(programName, r);
		TypedAST result = wp.CompilationUnit();
		final Token nextToken = wp.token_source.getNextToken();
		if (nextToken.kind != WyvernParserConstants.EOF) {
			ToolError.reportError(ErrorMessage.UNEXPECTED_INPUT, wp.loc(nextToken));			
		}
		return result;
	}
	
	/**
	 * Loads and parses the given file into the TypedAST representation, using the
	 * new Wyvern parser.
	 * 
	 * @param program
	 * @return
	 * @throws IOException 
	 * @throws CopperParserException 
	 */
	public static TypedAST getNewAST(File programLocation) throws ParseException {
		String program = readFile(programLocation);
		return getNewAST(program, programLocation.getPath());
	}
	
	/**
	 * Completely evaluates the given AST, and compares it to the given value.
	 * Does typechecking first, then evaluation.
	 * 
	 * @param ast
	 * @param value
	 */
	public static void evaluateExpecting(TypedAST ast, int value) {
		ast.typecheck(Globals.getStandardEnv(), Optional.empty());
		Value v = ast.evaluate(Globals.getStandardEvalEnv());
		String expecting = "IntegerConstant(" + value + ")";
		Assert.assertEquals(expecting, v.toString());
	}
	
	public static void evaluateExpecting(TypedAST ast, String value) {
		ast.typecheck(Globals.getStandardEnv(), Optional.empty());
		Value v = ast.evaluate(Globals.getStandardEvalEnv());
		String expecting = "StringConstant(\"" + value + "\")"; 
		Assert.assertEquals(expecting, v.toString());
	}

	/**
	 * Completely evaluates the given AST, and compares it to the given value.
	 * Does typechecking first, then evaluation.
	 * 
	 * @param ast
	 * @param value
	 */
	public static void evaluateExpectingPerf(TypedAST ast, int value) {
		Value v = ast.evaluate(Globals.getStandardEvalEnv());
		String expecting = "IntegerConstant(" + value + ")"; 
		Assert.assertEquals(expecting, v.toString());
	}
	
	public static void evaluatePerf(TypedAST ast) {
		Value v = ast.evaluate(Globals.getStandardEvalEnv());
		//String expecting = "IntegerConstant(" + value + ")"; 
		//Assert.assertEquals(expecting, v.toString());
	}
	
	/**
	 * First typechecks the AST, then executes it.
	 * 
	 * Any returned value is discarded, but anything printed to stdout will be visible.
	 * 
	 * @param ast
	 */
	public static void evaluate(TypedAST ast) {
		ast.typecheck(Globals.getStandardEnv(), Optional.empty());
		ast.evaluate(Globals.getStandardEvalEnv());
	}
	
	/**
	 * First typechecks the AST, then executes it.
	 * If any file is loaded, it is parsed by the new parser.
	 * 
	 * Any returned value is returned, and anything printed to stdout will be visible.
	 * 
	 * @param ast
	 */
	public static Value evaluateNew(TypedAST ast) {
		boolean oldParserFlag = WyvernResolver.getInstance().setNewParser(true);
		try {
			ast.typecheck(Globals.getStandardEnv(), Optional.empty());
			return ast.evaluate(Globals.getStandardEvalEnv());
		} finally {
			WyvernResolver.getInstance().setNewParser(oldParserFlag);
		}
	}
	
	public static String readFile(String filename) {
		return readFile(new File(filename));
	}
	
	public static String readFile(File file) {
		try {
			StringBuffer b = new StringBuffer();
			for (String s : Files.readAllLines(file.toPath())) {
				//Be sure to add the newline as well
				b.append(s).append("\n");
			}
			return b.toString();
		} catch (IOException e) {
			ToolError.reportError(ErrorMessage.READ_FILE_ERROR, (FileLocation) null, file.getPath());
			//Assert.fail("Failed opening file: " + file.getPath());
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Removes the global tagged-type data.
	 */
	private static void clearGlobalTagInfo() {
		TaggedInfo.clearGlobalTaggedInfos();
	}
}
