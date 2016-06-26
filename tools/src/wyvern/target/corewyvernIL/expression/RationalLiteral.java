package wyvern.target.corewyvernIL.expression;

import java.util.HashSet;
import java.util.Set;

import wyvern.target.corewyvernIL.Environment;
import wyvern.target.corewyvernIL.astvisitor.ASTVisitor;
import wyvern.target.corewyvernIL.support.TypeContext;
import wyvern.target.corewyvernIL.support.Util;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.target.oir.OIREnvironment;

public class RationalLiteral extends AbstractValue {

	private int numerator;
	private int denominator;

	public RationalLiteral(int numerator, int denominator) {
		super(null, null);
		this.numerator = numerator;
		this.denominator = denominator;
	}

	public int getNumerator() {
		return numerator;
	}

	public void setNumerator(int numerator) {
		this.numerator = numerator;
	}

	public int getDenominator() {
		return denominator;
	}

	public void setDenominator(int denominator) {
		this.denominator = denominator;
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

	public Set<String> getFreeVariables() {
		return new HashSet<>();
	}

	@Override
	public ValueType getType() {
		// TODO Auto-generated method stub
		return null;
	}
}
