package wyvern.target.corewyvernIL.type;

import java.io.IOException;

import wyvern.target.corewyvernIL.Environment;
import wyvern.target.corewyvernIL.astvisitor.ASTVisitor;
import wyvern.target.corewyvernIL.support.TypeContext;
import wyvern.target.corewyvernIL.support.View;
import wyvern.target.oir.OIREnvironment;

/**
 * Created by Ben Chung on 6/26/2015.
 */
public class DynamicType extends ValueType {

	@Override
	public boolean equals(Object o) {
		return o instanceof DynamicType;
	}

	@Override
	public int hashCode() {
		return DynamicType.class.hashCode();
	}

	@Override
	public boolean isSubtypeOf(ValueType t, TypeContext ctx) {
		return true;
	}

	@Override
	public void doPrettyPrint(Appendable dest, String indent) throws IOException {
		dest.append("Dyn");
	}
	
	@Override
	public <T, E> T acceptVisitor(ASTVisitor<T, E> emitILVisitor, E env,
			OIREnvironment oirenv) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ValueType adapt(View v) {
		return this;
	}

	@Override
	public void checkWellFormed(TypeContext ctx) {
		// this type is always well-formed!
	}

	@Override
	public ValueType doAvoid(String varName, TypeContext ctx, int count) {
		return this;
	}
}
