package wyvern.tools.types;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import wyvern.target.corewyvernIL.expression.Path;
import wyvern.target.corewyvernIL.support.GenContext;
import wyvern.target.corewyvernIL.type.NominalType;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.tools.errors.ErrorMessage;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.errors.HasLocation;
import wyvern.tools.errors.ToolError;
import wyvern.tools.errors.WyvernException;
import wyvern.tools.typedAST.core.binding.NameBinding;
import wyvern.tools.typedAST.core.binding.typechecking.TypeBinding;
import wyvern.tools.util.TreeWriter;

public class UnresolvedType extends AbstractTypeImpl implements Type {
	private String typeName;

	public UnresolvedType(String typeName, FileLocation loc) {
		super(loc);
		this.typeName = typeName;
	}
	
	public Type resolve(Environment env) {
		if (env.lookupType(typeName) == null) {
			if (env.lookup(this.typeName) != null) {
				// Perhaps its first class?
				NameBinding n = env.lookup(this.typeName);
				Type t = n.getType();
				return t;
			}
			throw new RuntimeException("Cannot find "+typeName +" in environment "+env);
		}
		TypeBinding typeBinding = env.lookupType(typeName);
		if (typeBinding.getMetadata().isPresent() && typeBinding.getMetadata().get().get() != null)
			return typeBinding.getUse().cloneWithBinding(typeBinding);
		else
			return typeBinding.getUse();
	}
	
	@Override
	public String toString() {
		return "UNRESOLVED: " + typeName;
	}

	@Override
	public boolean subtype(Type other, HashSet<SubtypeRelation> subtypes) {
		ToolError.reportError(ErrorMessage.NOT_SUBTYPE,
				HasLocation.UNKNOWN, this.toString(), other.toString());
		return false; // Unreachable.
	}
	
	@Override
	public boolean subtype(Type other) {
		return this.subtype(other, new HashSet<SubtypeRelation>());
	}

    @Override
	public boolean isSimple() {
		return true;
	}

	@Override
	public Map<String, Type> getChildren() {
		return new HashMap<>();
	}

	@Override
	public Type cloneWithChildren(Map<String, Type> newChildren) {
		throw new RuntimeException("Cannot specify a ref type");
	}

	@Override
	public Optional<TypeBinding> getResolvedBinding() {
		return Optional.empty();
	}

    @Override
    public ValueType generateILType() {
        throw new WyvernException("Cannot generate IL form for unresolved type", FileLocation.UNKNOWN);
    }

    public String getName() {
		return typeName;
	}

	@Override
	public ValueType getILType(GenContext ctx) {
		return ctx.lookupType(typeName, this.getLocation());
	}
}
