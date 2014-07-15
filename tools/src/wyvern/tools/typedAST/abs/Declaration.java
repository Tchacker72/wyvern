package wyvern.tools.typedAST.abs;

import wyvern.tools.errors.FileLocation;
import wyvern.tools.typedAST.core.values.UnitVal;
import wyvern.tools.typedAST.interfaces.EnvironmentExtender;
import wyvern.tools.typedAST.interfaces.Value;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.util.Pair;

import java.util.Optional;

// TODO: Consider adding a class "ListOfDeclarations" that only handles indents with decls and make
// Type and Class to be subtypes of that rather than this current Declaration which can be called DeclarationWithBody? (Alex)

// TODO SMELL: probably should have Declarations not be in an "evaluate" part of the AST
public abstract class Declaration extends AbstractTypedAST implements EnvironmentExtender {
	protected Declaration nextDecl = null;

	/** 
	 * Most declarations simply evaluate to unit without any computation
	 */
	@Override
	public final Value evaluate(Environment env) {
		// code smell - can we eliminate this function?
		// throw new RuntimeException("cannot evaluate a decl to a value - use evalDecls to get an updated environment");
		return UnitVal.getInstance(this.getLocation());
	}

	public abstract String getName();

	public final Environment extend(Environment old, Environment against) {
		Environment newEnv = doExtend(old, against);
		if (nextDecl != null)
			newEnv = nextDecl.extend(newEnv, newEnv);
		return newEnv;
	}
	
	public final Environment extendWithSelf(Environment old) {
		return doExtend(old, old);
	}

	protected abstract Environment doExtend(Environment old, Environment against);
	public abstract Environment extendWithValue(Environment old);
	public abstract void evalDecl(Environment evalEnv, Environment declEnv);
	
	public final Environment bindDecl(Environment evalEnv, Environment declEnv) {
		evalDecl(evalEnv, declEnv);
		return evalEnv;
	}
	
	public final Environment bindDecl(Environment evalEnv) {
		return bindDecl(evalEnv, evalEnv);
	}
	
	public final Environment evalDecl(Environment env) {
		return bindDecl(doExtendWithValue(env));
	}
	
	public final Environment doExtendWithValue(Environment old) {
		return extendWithValue(old);
	}

	public Declaration getNextDecl() {
		return nextDecl;
	}

	public boolean isClass() { return false; }
}