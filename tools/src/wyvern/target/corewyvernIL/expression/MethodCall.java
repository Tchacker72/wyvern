package wyvern.target.corewyvernIL.expression;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import wyvern.target.corewyvernIL.Environment;
import wyvern.target.corewyvernIL.FormalArg;
import wyvern.target.corewyvernIL.astvisitor.ASTVisitor;
import wyvern.target.corewyvernIL.decltype.DeclType;
import wyvern.target.corewyvernIL.decltype.DefDeclType;
import wyvern.target.corewyvernIL.support.EvalContext;
import wyvern.target.corewyvernIL.support.TypeContext;
import wyvern.target.corewyvernIL.support.View;
import wyvern.target.corewyvernIL.support.ViewExtension;
import wyvern.target.corewyvernIL.type.StructuralType;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.target.oir.OIREnvironment;
import wyvern.tools.errors.ErrorMessage;
import wyvern.tools.errors.HasLocation;
import wyvern.tools.errors.ToolError;

public class MethodCall extends Expression {

	private IExpr objectExpr;
	private String methodName;
	private List<? extends IExpr> args;

	public MethodCall(IExpr e, String methodName,
			List<? extends IExpr> args2, HasLocation location) {
		super(location != null ? location.getLocation():null);
		//if (getLocation() == null || getLocation().line == -1)
		//	throw new RuntimeException("missing location");
		this.objectExpr = e;
		this.methodName = methodName;
		this.args = args2;
		// sanity check
		if (args2.size() > 0 && args2.get(0) == null)
			throw new NullPointerException("invariant: no null args");
	}

	@Override
	public void doPrettyPrint(Appendable dest, String indent) throws IOException {
		objectExpr.doPrettyPrint(dest,indent);
		dest.append('.').append(methodName).append('(');
		boolean first = true;
		for (IExpr arg : args) {
			if (first)
				first = false;
			else
				dest.append(", ");
			arg.doPrettyPrint(dest, indent);
		}
		dest.append(')');
	}

	public IExpr getObjectExpr() {
		return objectExpr;
	}

	public String getMethodName() {
		return methodName;
	}

	public List<? extends IExpr> getArgs() {
		return args;
	}

	@Override
	public ValueType typeCheck(TypeContext ctx) {
		ValueType ot = objectExpr.typeCheck(ctx);
		StructuralType st = ot.getStructuralType(ctx);

		List<DeclType> dts = st.findDecls(methodName, ctx);
		if (dts.isEmpty()) {
			ToolError.reportError(ErrorMessage.NO_SUCH_METHOD, this, methodName);
		}

		ArrayList<ValueType> argTypeList = new ArrayList<ValueType>(args.size());
		for (int i = 0; i < args.size(); ++i) {
			argTypeList.add(args.get(i).typeCheck(ctx));
		}

		TypeContext newCtx = null;
		for (DeclType dt : dts) {
			newCtx = ctx;
			if (!(dt instanceof DefDeclType)) continue;
			DefDeclType ddt = (DefDeclType) dt;

			// check argument compatibility
			List<FormalArg> formalArgs = ddt.getFormalArgs();
			if (args.size() != formalArgs.size()) continue;

			boolean argsTypechecked = true;
			View v = View.from(objectExpr, newCtx);
			for (int i = 0; i < args.size(); ++i) {
				ValueType argType = formalArgs.get(i).getType().adapt(v);
				String name = formalArgs.get(i).getName();
				ValueType actualType = argTypeList.get(i); //e.typeCheck(ctx);
				if (!actualType.isSubtypeOf(argType, newCtx)) {
					// for debugging
					actualType.isSubtypeOf(argType, newCtx);
					argsTypechecked = false;
					break;
				}
				newCtx = newCtx.extend(name, actualType);
				IExpr e = args.get(i);
				if (e instanceof Variable) {
					v = new ViewExtension(new Variable(ddt.getFormalArgs().get(i).getName()), (Variable) e, v);
				}
			}

			if (argsTypechecked) {
				ctx = newCtx;
				// compute result type
				ValueType resultType = ddt.getResultType(v);
				resultType = resultType.adapt(v);
				this.setExprType(resultType);
				return getExprType();
			}
		}

		StringBuilder argTypes = new StringBuilder();
		argTypes.append(methodName);
		argTypes.append("(");
		for (int i = 0; i <= args.size() - 2; ++i) {
			argTypes.append(argTypeList.get(i).toString());
			argTypes.append(", ");
		}
		if (args.size() > 0)
			argTypes.append(argTypeList.get(args.size() - 1).toString());
		argTypes.append(")");
		ToolError.reportError(ErrorMessage.NO_METHOD_WITH_THESE_ARG_TYPES, this, argTypes.toString());
		return null;
	}

	@Override
	public <S, T> T acceptVisitor(ASTVisitor <S, T> emitILVisitor,
			S state) {
		return emitILVisitor.visit(state, this);
	}

	@Override
	public Value interpret(EvalContext ctx) {
		Invokable receiver = (Invokable)objectExpr.interpret(ctx);
		List<Value> argValues = new ArrayList<Value>(args.size());
		for (int i = 0; i < args.size(); ++i) {
			IExpr e = args.get(i);
			argValues.add(e.interpret(ctx));
		}
		return receiver.invoke(methodName, argValues);		
	}

	@Override
	public Set<String> getFreeVariables() {
		Set<String> freeVars = objectExpr.getFreeVariables();
		for (IExpr arg : args) {
			freeVars.addAll(arg.getFreeVariables());
		}
		return freeVars;
	}

}
