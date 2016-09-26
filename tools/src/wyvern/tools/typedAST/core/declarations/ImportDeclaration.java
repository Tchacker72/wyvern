package wyvern.tools.typedAST.core.declarations;

import static wyvern.tools.errors.ErrorMessage.MODULE_TYPE_ERROR;
import static wyvern.tools.errors.ToolError.reportError;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import wyvern.stdlib.Globals;
import wyvern.target.corewyvernIL.FormalArg;
import wyvern.target.corewyvernIL.VarBinding;
import wyvern.target.corewyvernIL.decltype.DeclType;
import wyvern.target.corewyvernIL.decltype.DefDeclType;
import wyvern.target.corewyvernIL.decltype.VarDeclType;
import wyvern.target.corewyvernIL.expression.Expression;
import wyvern.target.corewyvernIL.expression.FFI;
import wyvern.target.corewyvernIL.expression.FFIImport;
import wyvern.target.corewyvernIL.expression.JavaValue;
import wyvern.target.corewyvernIL.expression.Let;
import wyvern.target.corewyvernIL.expression.MethodCall;
import wyvern.target.corewyvernIL.expression.Path;
import wyvern.target.corewyvernIL.expression.Value;
import wyvern.target.corewyvernIL.expression.Variable;
import wyvern.target.corewyvernIL.modules.Module;
import wyvern.target.corewyvernIL.modules.TypedModuleSpec;
import wyvern.target.corewyvernIL.support.GenContext;
import wyvern.target.corewyvernIL.support.GenUtil;
import wyvern.target.corewyvernIL.support.ModuleResolver;
import wyvern.target.corewyvernIL.support.TopLevelContext;
import wyvern.target.corewyvernIL.support.TypeContext;
import wyvern.target.corewyvernIL.support.TypeGenContext;
import wyvern.target.corewyvernIL.support.Util;
import wyvern.target.corewyvernIL.support.View;
import wyvern.target.corewyvernIL.type.DynamicType;
import wyvern.target.corewyvernIL.type.NominalType;
import wyvern.target.corewyvernIL.type.StructuralType;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.tools.errors.ErrorMessage;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.errors.ToolError;
import wyvern.tools.imports.ImportBinder;
import wyvern.tools.interop.FObject;
import wyvern.tools.typedAST.abs.Declaration;
import wyvern.tools.typedAST.core.binding.NameBinding;
import wyvern.tools.typedAST.core.binding.compiler.ImportResolverBinding;
import wyvern.tools.typedAST.interfaces.CoreAST;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.typedAST.transformers.GenerationEnvironment;
import wyvern.tools.typedAST.transformers.ILWriter;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.types.extensions.ClassType;
import wyvern.tools.types.extensions.Unit;
import wyvern.tools.util.EvaluationEnvironment;
import wyvern.tools.util.Pair;
import wyvern.tools.util.TreeWriter;

public class ImportDeclaration extends Declaration implements CoreAST {
  private URI uri;
  private ImportBinder binder;
  private FileLocation location;
  private boolean requireFlag;
  private boolean metadataFlag;
  private String asName;

  public ImportDeclaration(URI inputURI, FileLocation location, String image, boolean isRequire, boolean isMetadata) {
    this.uri = inputURI;
    this.location = location;
    this.requireFlag = isRequire;
    this.metadataFlag = isMetadata;
    this.asName = image;
  }


  public URI getUri() {
    return uri;
  }

  @Override
  public FileLocation getLocation() {
    return location;
  }

  @Override
  public Map<String, TypedAST> getChildren() {
    Map<String, TypedAST> childMap = new HashMap<>();
    return childMap;
  }

  @Override
  public TypedAST cloneWithChildren(Map<String, TypedAST> newChildren) {
    return this;
  }

  @Override
  public String getName() {
    return "";
  }

  @Override
  public DeclType genILType(GenContext ctx) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public wyvern.target.corewyvernIL.decl.Declaration generateDecl(GenContext ctx, GenContext thisContext) {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean isRequire() {
    return this.requireFlag;
  }


  @Override
  public wyvern.target.corewyvernIL.decl.Declaration topLevelGen(GenContext ctx, List<TypedModuleSpec> dependencies) {
    // TODO Auto-generated method stub
    return null;
  }

  public static GenContext extendWithImportCtx(FObject obj, GenContext ctx) {
      if(obj.getWrappedValue() instanceof java.lang.Class) {
          // then this is a Class import
          // and we need to extend the context
          String qualifiedName = ((Class) obj.getWrappedValue()).getName();
          int lastDot = qualifiedName.lastIndexOf('.');
          String className = qualifiedName.substring(lastDot+1);
          String packageName = qualifiedName.substring(0, lastDot);
          ctx = new TypeGenContext(
                  className,
                  new Variable(GenUtil.javaTypesObjectName + packageName),
                  ctx
          );
      }
      return ctx;
  }

  public Pair<VarBinding,GenContext> genBinding(GenContext ctx, List<TypedModuleSpec> dependencies) {
    // add the import's type to the context, and get the import value
    Expression importExp = null;
    String importName = this.getUri().getSchemeSpecificPart();
    ValueType type = null;
    if (importName.contains(".")) {
      importName = importName.substring(importName.lastIndexOf(".")+1);
    }
    final String scheme = this.getUri().getScheme();
    if (ctx.isPresent(scheme, true)) {
      // TODO: hack; replace this by getting FFI metadata from the type of the scheme
      return FFI.importURI(this.getUri(), ctx);
    } else if (scheme.equals("java")) {
      // we are not using Java like a capability in this branch, so check the whitelist!
      if (!Globals.checkSafeJavaImport(this.getUri().getSchemeSpecificPart()))
          ToolError.reportError(ErrorMessage.UNSAFE_JAVA_IMPORT, this, this.getUri().getSchemeSpecificPart());
      return FFI.doJavaImport(getUri(), ctx);
    } else if (this.getUri().getScheme().equals("python")) {
      String moduleName = this.getUri().getRawSchemeSpecificPart();
      importExp = new FFIImport(new NominalType("system", "Python"),
                                moduleName,
                                new NominalType("system", "Dyn"));
      ctx = ctx.extend(importName, new Variable(importName), Util.dynType());
      type = Util.dynType();
    } else if (this.isRequire()) {
		// invariant: modules that require things won't call this 
		// so special case for scripts that require things
		// right now the only supported case is java
		// TODO: make this more generic
		if (importName.equals("java")) {
			// TODO: implement me
			type = Globals.JAVA_IMPORT_TYPE;
			importExp = new FFI(importName, type, this.getLocation());
		    ctx = ctx.extend(importName, importExp, type);
		} else {
			// special case: a script is importing a module called "importName"
			// that requires only java
			
			// load the module
		    String moduleName = this.getUri().getRawSchemeSpecificPart();
			Module m = ctx.getInterpreterState().getResolver().resolveModule(moduleName);
			
			// instantiate the module
			DefDeclType modDeclType = (DefDeclType) ((StructuralType)m.getSpec().getType()).findDecl(Util.APPLY_NAME, ctx);
			List<FormalArg> modArgs = modDeclType.getFormalArgs();
			if (modArgs.size() != 1) {
				ToolError.reportError(ErrorMessage.SCRIPT_REQUIRED_MODULE_ONLY_JAVA, this);
			}
			final ValueType argType = modArgs.get(0).getType();
			if (!argType.equals(Globals.JAVA_IMPORT_TYPE)) {
				ToolError.reportError(ErrorMessage.SCRIPT_REQUIRED_MODULE_ONLY_JAVA, this);
			}
			List<Expression> args = new LinkedList<Expression>();
			args.add(new FFI("java", Globals.JAVA_IMPORT_TYPE, this.getLocation()));
			importExp = new MethodCall(m.getExpression(), Util.APPLY_NAME, args , this);
			
			// type is the result type of the module
			type = modDeclType.getRawResultType();
			// ctx gets extended in the standard way, with a variable
		    ctx = ctx.extend(importName, new Variable(importName), type);
		}
    } else if (scheme.equals("wyv")) {
      // TODO: need to add types for non-java imports
      String moduleName = this.getUri().getSchemeSpecificPart();
      final ModuleResolver resolver = ctx.getInterpreterState().getResolver();
      final Module module = resolver.resolveModule(moduleName);
      final String internalName = module.getSpec().getInternalName();
      if (this.metadataFlag) {
        if (module.getSpec().getType().isResource(ctx))
            ToolError.reportError(ErrorMessage.NO_METADATA_FROM_RESOURCE, this);
        Value v = resolver.wrap(module.getExpression(), module.getDependencies()).interpret(Globals.getStandardEvalContext());
        type = v.getType();
      } else {
        type = module.getSpec().getType();
      }
      ctx = ctx.getInterpreterState().getResolver().extendGenContext(ctx, module.getDependencies());
      if (!ctx.isPresent(internalName, true)) {
          ctx = ctx.extend(internalName, new Variable(internalName), type);
      }
      importExp = new Variable(internalName);
      dependencies.add(module.getSpec());
      dependencies.addAll(module.getDependencies());
      ctx = ctx.extend(importName, new Variable(internalName), type);
    } else {
        ToolError.reportError(ErrorMessage.SCHEME_NOT_RECOGNIZED, this, scheme);
    }
    return new Pair<VarBinding, GenContext>(new VarBinding(importName, type, importExp), ctx);
  }

  @Override
  public void addModuleDecl(TopLevelContext tlc) {
    // no action needed
  }

  @Override
  public void genTopLevel(TopLevelContext tlc) {
	Pair<VarBinding, GenContext> bindingAndCtx = genBinding(tlc.getContext(), tlc.getDependencies());
	VarBinding binding = bindingAndCtx.first;
    GenContext newCtx = bindingAndCtx.second;
    ValueType type = binding.getExpression().typeCheck(newCtx);
    tlc.addLet(binding.getVarName(), type, binding.getExpression(), false);
    tlc.updateContext(newCtx);
  }


  public String getAsName() {
    return asName;
  }
}
