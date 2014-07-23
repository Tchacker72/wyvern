package wyvern.tools.typedAST.extensions;

import wyvern.tools.errors.FileLocation;
import wyvern.tools.typedAST.abs.AbstractTypedAST;
import wyvern.tools.typedAST.core.binding.NameBinding;
import wyvern.tools.typedAST.core.binding.NameBindingImpl;
import wyvern.tools.typedAST.core.declarations.DefDeclaration;
import wyvern.tools.typedAST.core.evaluation.Closure;
import wyvern.tools.typedAST.interfaces.BoundCode;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.typedAST.interfaces.Value;
import wyvern.tools.types.ApplyableType;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.types.TypeResolver;
import wyvern.tools.types.extensions.Arrow;
import wyvern.tools.util.Pair;
import wyvern.tools.util.TreeWriter;

import javax.lang.model.element.Name;
import java.util.*;

public class SpliceBindExn extends AbstractTypedAST implements BoundCode {
	private final TypedAST exn;
	private List<NameBinding> bindings;

	public SpliceBindExn(TypedAST exn, List<NameBinding> bindings) {
		this.exn = exn;
		this.bindings = bindings;
	}

	private Optional<Type> cached = Optional.empty();
	@Override
	public Type getType() {
		return DefDeclaration.getMethodType(bindings, cached.get(), new LinkedList<>());
	}

	@Override
	public List<NameBinding> getArgBindings() {
		return bindings;
	}

	@Override
	public TypedAST getBody() {
		return exn;
	}

	@Override
	public Environment analyze(Type expected, Environment env) {
		Environment outerEnv = getInnerEnv(env);

		Type resType = ((Arrow) expected).getResult();

		return exn.analyze(resType, outerEnv);
	}

	@Override
	public Pair<Type, Environment> synthesize(Environment env) {
		return exn.synthesize(getInnerEnv(env));
	}

	public Environment getInnerEnv(Environment env) {
		Environment outerEnv = env.lookupBinding("oev", TSLBlock.OuterEnviromentBinding.class)
				.map(oeb->oeb.getStore())
				.orElse(Environment.getEmptyEnvironment());

		List<NameBinding> newBindings = new ArrayList<>();
		for (NameBinding binding : bindings)
			newBindings.add(new NameBindingImpl(binding.getName(), TypeResolver.resolve(binding.getType(), env)));
		bindings = newBindings;

		outerEnv = outerEnv.extend(bindings.stream().reduce(Environment.getEmptyEnvironment(), Environment::extend, (a, b) -> b.extend(a)));
		return outerEnv;
	}


	@Override
	public Value evaluate(Environment env) {
		Environment outerEnv = env.lookupBinding("oev", TSLBlock.OuterEnviromentBinding.class)
				.map(oeb->oeb.getStore())
				.orElse(Environment.getEmptyEnvironment());

		return new Closure(this, outerEnv);
	}

	@Override
	public Map<String, TypedAST> getChildren() {
		Map<String, TypedAST> result = new HashMap<>(1);
		result.put("exn", exn);
		return result;
	}

	@Override
	public TypedAST cloneWithChildren(Map<String, TypedAST> newChildren) {
		return new SpliceBindExn(newChildren.get("exn"), bindings);
	}

	@Override
	public FileLocation getLocation() {
		return null;
	}

	@Override
	public void writeArgsToTree(TreeWriter writer) {

	}
}
