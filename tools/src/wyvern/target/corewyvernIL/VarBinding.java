package wyvern.target.corewyvernIL;

import java.io.IOException;

import wyvern.target.corewyvernIL.expression.Expression;
import wyvern.target.corewyvernIL.expression.IExpr;
import wyvern.target.corewyvernIL.type.ValueType;

public class VarBinding extends ASTNode {
	private String varName;
	private ValueType type;
	private IExpr expr;
	
	public VarBinding(String varName, ValueType type, IExpr toReplace) {
		this.varName = varName;
		this.type = type;
		this.expr = toReplace;
		if (toReplace == null) throw new RuntimeException();
	}

	public String getVarName() {
		return varName;
	}

	public ValueType getType() {
		return type;
	}

	public Expression getExpression() {
		return (Expression) expr;
	}
	
	public void doPrettyPrint(Appendable dest, String indent) throws IOException {
		dest.append(indent).append(getVarName()).append(" : ");
		getType().doPrettyPrint(dest, indent);
		dest.append(" = ");
		getExpression().doPrettyPrint(dest,indent);
		dest.append('\n');
	}
}
