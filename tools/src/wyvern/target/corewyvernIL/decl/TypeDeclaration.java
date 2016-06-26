package wyvern.target.corewyvernIL.decl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import wyvern.target.corewyvernIL.Environment;
import wyvern.target.corewyvernIL.astvisitor.ASTVisitor;
import wyvern.target.corewyvernIL.decltype.ConcreteTypeMember;
import wyvern.target.corewyvernIL.decltype.DeclType;
import wyvern.target.corewyvernIL.decltype.ValDeclType;
import wyvern.target.corewyvernIL.expression.Expression;
import wyvern.target.corewyvernIL.support.TypeContext;
import wyvern.target.corewyvernIL.type.Type;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.target.oir.OIREnvironment;
import wyvern.tools.errors.FileLocation;

public class TypeDeclaration extends NamedDeclaration {

	public TypeDeclaration(String typeName, Type sourceType, FileLocation loc) {
		this(typeName, sourceType, null, loc);
	}
	public TypeDeclaration(String typeName, Type sourceType, Expression metadata, FileLocation loc) {
		super(typeName, loc);
		this.sourceType = sourceType;
		this.metadata = metadata;
	}
	
	private Expression metadata;
	private Type sourceType;

	public Type getSourceType() {
		return sourceType;
	}

	@Override
	public <T, E> T acceptVisitor(ASTVisitor <T, E> emitILVisitor,
			E env, OIREnvironment oirenv) {
		return emitILVisitor.visit(env, oirenv, this);
	}

	@Override
	public DeclType typeCheck(TypeContext ctx, TypeContext thisCtx) {
		if (metadata != null)
			metadata.typeCheck(thisCtx);
		return getDeclType();
	}

	@Override
	public void doPrettyPrint(Appendable dest, String indent) throws IOException {
		dest.append(indent).append("type ").append(getName()).append(" = ");
		sourceType.doPrettyPrint(dest, indent);
		dest.append('\n');
	}

	/*@Override
	public String toString() {
		return "type declaration " + super.getName() + " = " + sourceType.toString();
	}*/

	@Override
	public Set<String> getFreeVariables() {
		return new HashSet<>();
	}
	
	/** LIMITATION: only works if sourceType is a ValueType.
	 * in the future maybe we'll extract the ValueType from the source type somehow.
	 */
	@Override
	public DeclType getDeclType() {
		return new ConcreteTypeMember(getName(), (ValueType) sourceType, this.metadata);
	}

}
