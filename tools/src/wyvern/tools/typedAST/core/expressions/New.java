package wyvern.tools.typedAST.core.expressions;

import wyvern.tools.errors.ErrorMessage;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.errors.ToolError;
import wyvern.tools.typedAST.abs.CachingTypedAST;
import wyvern.tools.typedAST.abs.Declaration;
import wyvern.tools.typedAST.core.TypeVarDecl;
import wyvern.tools.typedAST.core.binding.AbstractBinding;
import wyvern.tools.typedAST.core.binding.objects.ClassBinding;
import wyvern.tools.typedAST.core.binding.evaluation.LateValueBinding;
import wyvern.tools.typedAST.core.binding.NameBindingImpl;
import wyvern.tools.typedAST.core.binding.evaluation.ValueBinding;
import wyvern.tools.typedAST.core.binding.typechecking.TypeBinding;
import wyvern.tools.typedAST.core.declarations.ClassDeclaration;
import wyvern.tools.typedAST.core.declarations.DeclSequence;
import wyvern.tools.typedAST.core.declarations.ValDeclaration;
import wyvern.tools.typedAST.core.values.Obj;
import wyvern.tools.typedAST.interfaces.CoreAST;
import wyvern.tools.typedAST.interfaces.CoreASTVisitor;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.typedAST.interfaces.Value;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.types.extensions.ClassType;
import wyvern.tools.types.extensions.TypeApp;
import wyvern.tools.types.extensions.TypeDeclUtils;
import wyvern.tools.types.extensions.TypeVar;
import wyvern.tools.util.Reference;
import wyvern.tools.util.TreeWriter;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class New extends CachingTypedAST implements CoreAST {
	ClassDeclaration cls;
	
	Map<String, TypedAST> args = new HashMap<String, TypedAST>();
	boolean isGeneric = false;

	private static final ClassDeclaration EMPTY = new ClassDeclaration("Empty", "", "", null, FileLocation.UNKNOWN);
	private static int generic_num = 0;
	private DeclSequence seq;
	private Type ct;

	public void setBody(DeclSequence seq) {
		this.seq = seq;
	}

	public DeclSequence getDecls() { return seq; }

	public static void resetGenNum() {
		generic_num = 0;
	}

	public New(Map<String, TypedAST> args, FileLocation fileLocation) {
		this.args = args;
		this.location = fileLocation;
	}

	public New(DeclSequence seq, FileLocation fileLocation) {
		this.seq = seq;
		this.location = fileLocation;
	}

	@Override
	public void writeArgsToTree(TreeWriter writer) {
		// FIXME: Not sure if this is rigth (Alex).
		for (TypedAST a : this.args.values()) {
			writer.writeArgs(a);
		}
	}

	@Override
	protected Type doTypecheck(Environment env, Optional<Type> expected) {
		// TODO check arg types
		// Type argTypes = args.typecheck();
		
		ClassBinding classVarTypeBinding = (ClassBinding) env.lookupBinding("class", ClassBinding.class).orElse(null);

		// System.out.println("classVarTypeBinding = " + classVarTypeBinding);

		if (classVarTypeBinding != null) { //In a class method
			ClassDeclaration enclosing = classVarTypeBinding.getClassDecl();

			//The type var declarations here
			Environment tvDeclEnv = Environment.getEmptyEnvironment();
					StreamSupport.stream(seq.getDeclIterator().spliterator(), false).filter(el->el instanceof TypeVarDecl)
					.<TypeVarDecl>map(tv->(TypeVarDecl)tv).reduce(tvDeclEnv, (ienv,tv)->tv.extendType(ienv, env), (a,b)->a);

			//This code looks up the parent's type bindings to instantiate it, does so, then adds its new decls
			//Get all type vars
			Environment parentArgs = classVarTypeBinding.getClassDecl().getTypeArgsEnv();
			Environment allTypeVars = parentArgs.extend(tvDeclEnv);

			//Find bindings for the parent type params
			List<TypeBinding> tbs = parentArgs.getBoundNames().stream().filter(name -> name != null).map(allTypeVars::lookupType).collect(Collectors.toList());
			List<Type> newBindings = tbs.stream().filter(el->el!=null).<Type>map(AbstractBinding::getType).collect(Collectors.toList());

			//Apply the type params
			ClassType appliedType = (newBindings.size() > 0)?(ClassType)TypeApp.getAppliedType(newBindings,enclosing.getType()):(ClassType)enclosing.getType();

			Collections.reverse(tbs);
			Environment nTbEnv = tbs.stream().reduce(Environment.getEmptyEnvironment(), (a,b)->a.extend(b), (a,b)->a);

			//Find unbound bindings
			Stream<TypeBinding> typeBindingStream = allTypeVars.getBoundNames().stream().filter(name -> name != null)
					.map(nTbEnv::lookupType).filter(tb -> tb == null);
			if (typeBindingStream.count() > 0)
				throw new RuntimeException("Cannot instantiate a type lambda");


			Environment declEnv = appliedType.getEnv();
			Environment innerEnv = seq.extendName(Environment.getEmptyEnvironment(), env).extend(declEnv);
			seq.typecheck(env.extend(new NameBindingImpl("this", new ClassType(new Reference<>(innerEnv), new Reference<>(innerEnv), new LinkedList<>(), classVarTypeBinding.getClassDecl().getName()))), Optional.empty());

			Environment nnames = seq.extend(declEnv, declEnv.extend(env));

			Environment objTee = TypeDeclUtils.getTypeEquivalentEnvironment(nnames.extend(declEnv));
			Type classVarType = new ClassType(new Reference<>(nnames.extend(declEnv)), new Reference<>(objTee), new LinkedList<>(), classVarTypeBinding.getClassDecl().getName());

			// TODO SMELL: do I really need to store this?  Can get it any time from the type
			cls = classVarTypeBinding.getClassDecl();
			ct = classVarType;

			return classVarType;
		} else { // Standalone
			
			isGeneric = true;
			Environment innerEnv = seq.extendType(Environment.getEmptyEnvironment(), env);
			Environment savedInner = env.extend(innerEnv);
			innerEnv = seq.extendName(innerEnv, savedInner);

			Environment declEnv = env.extend(new NameBindingImpl("this", new ClassType(new Reference<>(innerEnv), new Reference<>(innerEnv), new LinkedList<>(), null)));
			final Environment ideclEnv = StreamSupport.stream(seq.getDeclIterator().spliterator(), false).
					reduce(declEnv, (oenv,decl)->(decl instanceof ClassDeclaration)?decl.extend(oenv, savedInner):oenv,(a,b)->a.extend(b));
			seq.getDeclIterator().forEach(decl -> decl.typecheck(ideclEnv, Optional.<Type>empty()));


			Environment mockEnv = Environment.getEmptyEnvironment();

			LinkedList<Declaration> decls = new LinkedList<>();

			mockEnv = getGenericDecls(env, mockEnv, decls);
			Environment nnames = (seq.extendName(mockEnv,mockEnv.extend(env)));

			ClassDeclaration classDeclaration = new ClassDeclaration("generic" + generic_num++, "", "", new DeclSequence(decls), mockEnv, new LinkedList<String>(), getLocation());
			cls = classDeclaration;
			Environment tee = TypeDeclUtils.getTypeEquivalentEnvironment(nnames.extend(mockEnv));
			
			ct = new ClassType(new Reference<>(nnames.extend(mockEnv)), new Reference<>(tee), new LinkedList<String>(), null);
			return ct;
		}
	}

	private Environment getGenericDecls(Environment env, Environment mockEnv, LinkedList<Declaration> decls) {
		for (Entry<String, TypedAST> elem : args.entrySet()) {
			ValDeclaration e = new ValDeclaration(elem.getKey(), elem.getValue(), elem.getValue().getLocation());
			e.typecheck(env, Optional.empty());
			mockEnv = e.extend(mockEnv, mockEnv);
			decls.add(e);
		}
		return mockEnv;
	}

	@Override
	public Value evaluate(Environment env) {
		Environment argValEnv = Environment.getEmptyEnvironment();
		for (Entry<String, TypedAST> elem : args.entrySet())
			argValEnv = argValEnv.extend(new ValueBinding(elem.getKey(), elem.getValue().evaluate(env)));

		ClassBinding classVarTypeBinding = (ClassBinding) env.lookupBinding("class", ClassBinding.class).orElse(null);
		ClassDeclaration classDecl;
		if (classVarTypeBinding != null)
			classDecl = classVarTypeBinding.getClassDecl();
		else {

			Environment mockEnv = Environment.getEmptyEnvironment();

			LinkedList<Declaration> decls = new LinkedList<>();

			mockEnv = getGenericDecls(env, mockEnv, decls);

			classDecl = new ClassDeclaration("generic" + generic_num++, "", "", new DeclSequence(), mockEnv, new LinkedList<String>(), getLocation());
		}

		AtomicReference<Value> objRef = new AtomicReference<>();
		Environment evalEnv = env.extend(new LateValueBinding("this", objRef, ct));
		classDecl.evalDecl(evalEnv, classDecl.extendWithValue(Environment.getEmptyEnvironment()));
		final Environment ideclEnv = StreamSupport.stream(seq.getDeclIterator().spliterator(), false).
				reduce(evalEnv, (oenv,decl)->(decl instanceof ClassDeclaration)?decl.evalDecl(oenv):oenv, Environment::extend);
		Environment objenv = seq.bindDecls(ideclEnv, seq.extendWithDecls(classDecl.getFilledBody(objRef)));
		objRef.set(new Obj(objenv.extend(argValEnv)));
		
		// System.out.println("Finished evaluating new: " + this);
		
		return objRef.get();
	}

	@Override
	public Map<String, TypedAST> getChildren() {
		HashMap<String,TypedAST> outMap = new HashMap<>();
		outMap.put("seq", (seq==null)? new DeclSequence(Arrays.asList()) : seq);
		return outMap;
	}

	@Override
	public TypedAST doClone(Map<String, TypedAST> newChildren) {

		New aNew = new New(new HashMap<>(), location);
		aNew.setBody((DeclSequence) newChildren.get("seq"));
		aNew.cls = cls;
		return aNew;
	}

	public ClassDeclaration getClassDecl() {
		return cls;
	}

	@Override
	public void accept(CoreASTVisitor visitor) {
		//TODO: fix args
		visitor.visit(this);
	}
	
	public Map<String, TypedAST> getArgs() {
		return args;
	}

	private FileLocation location = FileLocation.UNKNOWN;
	
	@Override
	public FileLocation getLocation() {
		return location; 
	}

	public boolean isGeneric() {
		return isGeneric;
	}
}