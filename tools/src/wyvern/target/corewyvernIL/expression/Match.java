package wyvern.target.corewyvernIL.expression;

import java.util.List;
import java.util.Set;

import wyvern.target.corewyvernIL.Case;
import wyvern.target.corewyvernIL.Environment;
import wyvern.target.corewyvernIL.astvisitor.ASTVisitor;
import wyvern.target.corewyvernIL.support.EvalContext;
import wyvern.target.corewyvernIL.support.TypeContext;
import wyvern.target.corewyvernIL.type.NominalType;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.target.oir.OIREnvironment;

public class Match extends Expression {

	private Expression matchExpr;
	private Expression elseExpr;
	private List<Case> cases;

	public Match(Expression matchExpr, Expression elseExpr, List<Case> cases) {
		super();
		this.matchExpr = matchExpr;
		this.elseExpr = elseExpr;
		this.cases = cases;
	}

	public Expression getMatchExpr() {
		return matchExpr;
	}

	public Expression getElseExpr() {
		return elseExpr;
	}

	public List<Case> getCases() {
		return cases;
	}

	@Override
	public ValueType typeCheck(TypeContext env) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T, E> T acceptVisitor(ASTVisitor <T, E> emitILVisitor,
			E env, OIREnvironment oirenv) {
		return emitILVisitor.visit(env, oirenv, this);
	}

	@Override
	public Value interpret(EvalContext ctx) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getFreeVariables() {
		Set<String> freeVars = matchExpr.getFreeVariables();
		for (Case c : cases) {
			freeVars.addAll(c.getBody().getFreeVariables());
			freeVars.remove(c.getVarName());
			
			// TODO: fix when Match is implemented
			// Add variable at the root of the path of the NominalType
			// Look up that variable to get the tag to do the tag check.
			// Path p = c.getPattern().getPath();
			// if p returns y.f.x, the variable at the root of the path = y
			
		}
		freeVars.addAll(elseExpr.getFreeVariables());
		return freeVars;
	}
}
