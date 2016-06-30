package wyvern.target.corewyvernIL.support;

import java.util.List;

import wyvern.target.corewyvernIL.FormalArg;
import wyvern.target.corewyvernIL.decltype.ConcreteTypeMember;
import wyvern.target.corewyvernIL.decltype.DeclType;
import wyvern.target.corewyvernIL.decltype.DefDeclType;
import wyvern.target.corewyvernIL.decltype.ValDeclType;
import wyvern.target.corewyvernIL.decltype.VarDeclType;
import wyvern.target.corewyvernIL.expression.Expression;
import wyvern.target.corewyvernIL.expression.FieldGet;
import wyvern.target.corewyvernIL.expression.MethodCall;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.tools.errors.ErrorMessage;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.errors.HasLocation;
import wyvern.tools.errors.ToolError;

public class InvocationExprGenerator implements CallableExprGenerator {

	private final Expression receiver;
	private final DeclType declType;
	private final FileLocation location;
	
	public InvocationExprGenerator(Expression receiver, String operationName, GenContext ctx, FileLocation loc) {
		this.receiver = receiver;
		ValueType rt = receiver.typeCheck(ctx);
		declType = rt.findDecl(operationName, ctx);
		location = loc;
		if (declType == null)
			ToolError.reportError(ErrorMessage.NO_SUCH_METHOD, loc, operationName);
	}
	
	@Override
	public Expression genExpr() {
		if (declType instanceof ValDeclType || declType instanceof VarDeclType) {
			return new FieldGet(receiver, declType.getName(), location);
		} else {
			throw new RuntimeException("eta-expansion of a method reference not implemented");
		}
	}

	@Override
	public Expression genExprWithArgs(List<Expression> args, HasLocation loc) {
		if (declType instanceof ValDeclType || declType instanceof VarDeclType) {
			Expression e = genExpr();
			return new MethodCall(e, Util.APPLY_NAME, args, loc);
		} else {
			return new MethodCall(receiver, declType.getName(), args, loc);			
		}
	}

	@Override
	public DefDeclType getDeclType(TypeContext ctx) {
		if (declType instanceof ValDeclType || declType instanceof VarDeclType) {
			Expression e = genExpr();
			ValueType vt = e.typeCheck(ctx);
			return (DefDeclType)vt.findDecl(Util.APPLY_NAME, ctx);
		} else {
			return (DefDeclType) declType;
		}
	}
}
