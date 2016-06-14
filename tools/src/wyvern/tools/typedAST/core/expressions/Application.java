package wyvern.tools.typedAST.core.expressions;

import static wyvern.tools.errors.ErrorMessage.TYPE_CANNOT_BE_APPLIED;
import static wyvern.tools.errors.ErrorMessage.VALUE_CANNOT_BE_APPLIED;
import static wyvern.tools.errors.ToolError.reportError;
import static wyvern.tools.errors.ToolError.reportEvalError;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import wyvern.stdlib.Globals;
import wyvern.target.corewyvernIL.FormalArg;
import wyvern.target.corewyvernIL.decl.TypeDeclaration;
import wyvern.target.corewyvernIL.decltype.AbstractTypeMember;
import wyvern.target.corewyvernIL.expression.Expression;
import wyvern.target.corewyvernIL.expression.MethodCall;
import wyvern.target.corewyvernIL.expression.Variable;
import wyvern.target.corewyvernIL.support.CallableExprGenerator;
import wyvern.target.corewyvernIL.support.GenContext;
import wyvern.target.corewyvernIL.type.NominalType;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.tools.errors.ErrorMessage;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.errors.ToolError;
import wyvern.tools.parsing.coreparser.Token;
import wyvern.tools.typedAST.abs.CachingTypedAST;
import wyvern.tools.typedAST.core.declarations.DefDeclaration;
import wyvern.tools.typedAST.core.values.UnitVal;
import wyvern.tools.typedAST.interfaces.ApplyableValue;
import wyvern.tools.typedAST.interfaces.CoreAST;
import wyvern.tools.typedAST.interfaces.CoreASTVisitor;
import wyvern.tools.typedAST.interfaces.ExpressionAST;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.typedAST.interfaces.Value;
import wyvern.tools.typedAST.transformers.ExpressionWriter;
import wyvern.tools.typedAST.transformers.GenerationEnvironment;
import wyvern.tools.typedAST.transformers.ILWriter;
import wyvern.tools.types.ApplyableType;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.types.extensions.Arrow;
import wyvern.tools.types.extensions.Intersection;
import wyvern.tools.util.EvaluationEnvironment;
import wyvern.tools.util.TreeWriter;

public class Application extends CachingTypedAST implements CoreAST {
	private ExpressionAST function;
	private ExpressionAST argument;
    private List<String> generics;

	public Application(TypedAST function, TypedAST argument, FileLocation location) {
		this.function = (ExpressionAST) function;
		this.argument = (ExpressionAST) argument;
		this.location = location;
        this.generics = new LinkedList<String>();
	}

    public Application(TypedAST function, TypedAST argument, FileLocation location, List<Token> generics) {
		this.function = (ExpressionAST) function;
		this.argument = (ExpressionAST) argument;
		this.location = location;
        this.generics = new LinkedList<String>();

        if (generics != null) {
            for(Token t: generics) {
                this.generics.add(t.image);
            }
        }
	}

	@Override
	public void writeArgsToTree(TreeWriter writer) {
		writer.writeArgs(function, argument);
	}

	@Override
	protected Type doTypecheck(Environment env, Optional<Type> expected) {
		Type fnType = function.typecheck(env, Optional.empty());

		Type argument = null;
		if (fnType instanceof Arrow)
			argument = ((Arrow) fnType).getArgument();
		else if (fnType instanceof Intersection) {
			List<Type> args = fnType.getChildren().values().stream()
					.filter(tpe -> tpe instanceof Arrow).map(tpe->((Arrow)tpe).getArgument())
					.collect(Collectors.toList());
			argument = new Intersection(args);
		}
		if (this.argument != null)
			this.argument.typecheck(env, Optional.ofNullable(argument));
		
		if (!(fnType instanceof ApplyableType))
			reportError(TYPE_CANNOT_BE_APPLIED, this, fnType.toString());
		
		return ((ApplyableType)fnType).checkApplication(this, env);
	}

	public TypedAST getArgument() {
		return argument;
	}

	public TypedAST getFunction() {
		return function;
	}


	@Override
	public Value evaluate(EvaluationEnvironment env) {
		TypedAST lhs = function.evaluate(env);
		if (Globals.checkRuntimeTypes && !(lhs instanceof ApplyableValue))
			reportEvalError(VALUE_CANNOT_BE_APPLIED, lhs.toString(), this);
		ApplyableValue fnValue = (ApplyableValue) lhs;
		
		return fnValue.evaluateApplication(this, env);
	}

	@Override
	public void accept(CoreASTVisitor visitor) {
		visitor.visit(this);
	}
	@Override
	public Map<String, TypedAST> getChildren() {
		Hashtable<String, TypedAST> children = new Hashtable<>();
		children.put("function", function);
		children.put("argument", argument);
		return children;
	}

    @Override
    public void codegenToIL(GenerationEnvironment environment, ILWriter writer) {
        List<Expression> arguments;
        if (argument instanceof TupleObject) {
            arguments = Arrays.stream(((TupleObject) argument).getObjects()).map(a -> ExpressionWriter.generate(iw -> a.codegenToIL(environment, iw))).collect(Collectors.toList());
        } else {
            arguments = Arrays.asList(ExpressionWriter.generate(iw->argument.codegenToIL(environment, iw)));
        }
        if (function instanceof Invocation && ((Invocation) function).getArgument() == null) { // straight MethodCall
            writer.write(new MethodCall(ExpressionWriter.generate(iw -> function.codegenToIL(environment, iw)), ((Invocation) function).getOperationName(), arguments, this));
        }
        Expression expr = ExpressionWriter.generate(iw -> function.codegenToIL(environment, iw));
        writer.write(new MethodCall(expr, "call", arguments, this));
    }

    @Override
	public ExpressionAST doClone(Map<String, TypedAST> nc) {
		return new Application((ExpressionAST)nc.get("function"), (ExpressionAST)nc.get("argument"), location);
	}

	private FileLocation location;
	public FileLocation getLocation() {
		return this.location;
	}

	@Override
	public Expression generateIL(GenContext ctx, ValueType expectedType) {
		CallableExprGenerator exprGen = function.getCallableExpr(ctx);
		List<FormalArg> formals = exprGen.getExpectedArgTypes(ctx);

        int offset = 0;
		// generate arguments		
		List<Expression> args = new LinkedList<Expression>();
        if(!this.generics.isEmpty()) {
            for(String generic : this.generics) {
                 args.add(new wyvern.target.corewyvernIL.expression.New(new TypeDeclaration(DefDeclaration.GENERIC_MEMBER, new NominalType("", generic), this.location)));
                offset++;
            }
        }

        if (argument instanceof TupleObject) {
        	ExpressionAST[] raw_args = ((TupleObject) argument).getObjects();
        	for (int i = 0; i < raw_args.length; i++) {
				ValueType expectedArgType = formals.get(i + offset).getType();
				ExpressionAST ast = raw_args[i];
				// TODO: propagate types downward from formals
				args.add(ast.generateIL(ctx, expectedArgType));
			}
        } else if (argument instanceof UnitVal) {
        	// leave args empty
        } else {
            if (formals.size() != 1 + offset) {
    			ToolError.reportError(ErrorMessage.WRONG_NUMBER_OF_ARGUMENTS, this, ""+formals.size());
            }
        	
    		// TODO: propagate types downward from formals
        	args.add(argument.generateIL(ctx, formals.get(0).getType()));
        }
		
		// generate the call
        return exprGen.genExprWithArgs(args, this);
	}
}
