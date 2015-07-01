package nl.uva.se.ql.typechecking;

import nl.uva.se.ql.ast.expression.Binary;
import nl.uva.se.ql.ast.expression.Expression;
import nl.uva.se.ql.ast.expression.ExpressionVisitor;
import nl.uva.se.ql.ast.expression.Unary;
import nl.uva.se.ql.ast.expression.arithmetical.Addition;
import nl.uva.se.ql.ast.expression.arithmetical.Divide;
import nl.uva.se.ql.ast.expression.arithmetical.Modulo;
import nl.uva.se.ql.ast.expression.arithmetical.Multiply;
import nl.uva.se.ql.ast.expression.arithmetical.Negative;
import nl.uva.se.ql.ast.expression.arithmetical.Positive;
import nl.uva.se.ql.ast.expression.arithmetical.Power;
import nl.uva.se.ql.ast.expression.arithmetical.Substraction;
import nl.uva.se.ql.ast.expression.literal.BooleanLiteral;
import nl.uva.se.ql.ast.expression.literal.DecimalLiteral;
import nl.uva.se.ql.ast.expression.literal.IntegerLiteral;
import nl.uva.se.ql.ast.expression.literal.StringLiteral;
import nl.uva.se.ql.ast.expression.logical.And;
import nl.uva.se.ql.ast.expression.logical.Equal;
import nl.uva.se.ql.ast.expression.logical.GreaterOrEqual;
import nl.uva.se.ql.ast.expression.logical.GreaterThen;
import nl.uva.se.ql.ast.expression.logical.LessOrEqual;
import nl.uva.se.ql.ast.expression.logical.LessThen;
import nl.uva.se.ql.ast.expression.logical.Not;
import nl.uva.se.ql.ast.expression.logical.NotEqual;
import nl.uva.se.ql.ast.expression.logical.Or;
import nl.uva.se.ql.ast.expression.variable.Reference;
import nl.uva.se.ql.ast.form.Form;
import nl.uva.se.ql.ast.form.FormVisitor;
import nl.uva.se.ql.ast.statement.CalculatedQuestion;
import nl.uva.se.ql.ast.statement.Condition;
import nl.uva.se.ql.ast.statement.Question;
import nl.uva.se.ql.ast.statement.StatementVisitor;
import nl.uva.se.ql.ast.type.BooleanType;
import nl.uva.se.ql.ast.type.DecimalType;
import nl.uva.se.ql.ast.type.IntegerType;
import nl.uva.se.ql.ast.type.StringType;
import nl.uva.se.ql.ast.type.Type;
import nl.uva.se.ql.ast.type.TypeVisitor;
import nl.uva.se.ql.ast.type.UndefinedType;
import nl.uva.se.ql.typechecking.error.ErrorList;
import nl.uva.se.ql.typechecking.error.InvalidConditionType;
import nl.uva.se.ql.typechecking.error.InvalidOperandType;
import nl.uva.se.ql.typechecking.error.TypeMismatch;
import nl.uva.se.ql.typechecking.error.TypeNotAllowed;
import nl.uva.se.ql.typechecking.error.UndefinedReference;

public class TypeChecker implements FormVisitor, StatementVisitor,
		ExpressionVisitor<Type>, TypeVisitor<Type> {

	private static final Type BOOLEAN = new BooleanType();
	private static final Type INTEGER = new IntegerType();
	private static final Type DECIMAL = new DecimalType();
	private static final Type STRING = new StringType();
	
	private static final Type[] NUMERIC_TYPES = {INTEGER,DECIMAL};
	private static final Type[] ALPHA_NUMERIC_TYPES = {INTEGER,DECIMAL,STRING};
	private static final Type[] ALL_TYPES = {INTEGER,DECIMAL,STRING,BOOLEAN};

	private SymbolTable symbols;
	private ErrorList errors;

	private TypeChecker(SymbolTable symbols) {
		this.symbols = symbols;
		this.errors = new ErrorList();
	}

	public static ErrorList check(Form form) {
		SymbolResult symbolResult = SymbolResolver.resolve(form);
		ErrorList symbolErrorList = symbolResult.getErrorList();

		if (symbolErrorList.hasErrors()) {
			return symbolErrorList;
		}
			
		DependencyTable dependencies = DependencyResolver.resolve(form, 
				symbolResult.getSymbols());
		ErrorList dependencyErrorList = CyclicDependencyChecker.check(dependencies);
		
		if (dependencyErrorList.hasErrors()) {
			return dependencyErrorList;
		}
		
		TypeChecker typeChecker = new TypeChecker(symbolResult.getSymbols());
		typeChecker.visit(form);
		
		return typeChecker.errors;
	}

	public void visit(Form form) {
		form.visitChildren(this);
	}

	public void visit(Question question) {
	}

	public void visit(CalculatedQuestion calculatedQuestion) {
		Type expressionType = calculatedQuestion.getExpression().accept(this);
		Type questionType = calculatedQuestion.getType();
		Type promotedExpressionType = expressionType.promote();
		Type promotedQuesType = questionType.promote();
		
		if (!promotedExpressionType.equals(promotedQuesType)) {
			errors.addError(new TypeMismatch(
				calculatedQuestion.getLineNumber(), calculatedQuestion
				.getOffset(), questionType, expressionType));
		}
	}

	public void visit(Condition condition) {
		Type conditionType = condition.getExpression().accept(this);
		if (!conditionType.equals(BOOLEAN)) {
			errors.addError(new InvalidConditionType(condition.getLineNumber(),
				condition.getOffset(), conditionType));
		}

		condition.visitChildren(this);
	}

	public Type visit(Addition plus) {
		Type type = visitBinaryExpression(plus);
		return defineType(plus, type, ALPHA_NUMERIC_TYPES);
	}

	public Type visit(Divide divide) {
		Type type = visitBinaryExpression(divide);
		return defineType(divide, type, NUMERIC_TYPES);
	}

	public Type visit(Power power) {
		Type type = visitBinaryExpression(power);
		return defineType(power, type, NUMERIC_TYPES);
	}

	public Type visit(Multiply multiply) {
		Type type = visitBinaryExpression(multiply);
		return defineType(multiply, type, NUMERIC_TYPES);
	}

	public Type visit(Modulo modulo) {
		Type type = visitBinaryExpression(modulo);
		return defineType(modulo, type, NUMERIC_TYPES);
	}

	public Type visit(Negative negative) {
		Type type = visitUnaryExpression(negative);
		return defineType(negative, type, NUMERIC_TYPES);
	}

	public Type visit(Positive positive) {
		Type type = visitUnaryExpression(positive);
		return defineType(positive, type, NUMERIC_TYPES);
	}

	public Type visit(Substraction minus) {
		Type type = visitBinaryExpression(minus);
		return defineType(minus, type, NUMERIC_TYPES);
	}

	public Type visit(Not not) {
		Type type = visitUnaryExpression(not);
		return defineType(not, type, BOOLEAN);
	}

	public Type visit(NotEqual notEqual) {
		Type type = visitBinaryExpression(notEqual);
		return defineType(notEqual, type, ALL_TYPES);
	}

	public Type visit(Or or) {
		Type type = visitBinaryExpression(or);
		return defineType(or, type, BOOLEAN);
	}

	public Type visit(LessThen lessThen) {
		Type type = visitBinaryExpression(lessThen);
		return defineType(lessThen, type, ALPHA_NUMERIC_TYPES);
	}

	public Type visit(LessOrEqual lessOrEqual) {
		Type type = visitBinaryExpression(lessOrEqual);
		return defineType(lessOrEqual, type, ALPHA_NUMERIC_TYPES);
	}

	public Type visit(GreaterThen greaterThen) {
		Type type = visitBinaryExpression(greaterThen);
		return defineType(greaterThen, type, ALPHA_NUMERIC_TYPES);
	}

	public Type visit(GreaterOrEqual greaterOrEqual) {
		Type type = visitBinaryExpression(greaterOrEqual);
		return defineType(greaterOrEqual, type, ALPHA_NUMERIC_TYPES);
	}

	public Type visit(Equal equal) {
		Type type = visitBinaryExpression(equal);
		return defineType(equal, type, ALL_TYPES);
	}

	public Type visit(And and) {
		Type type = visitBinaryExpression(and);
		return defineType(and, type, BOOLEAN);
	}

	public Type visit(BooleanLiteral booleanLiteral) {
		return new BooleanType();
	}

	public Type visit(DecimalLiteral decimalLiteral) {
		return new DecimalType();
	}

	public Type visit(IntegerLiteral integerLiteral) {
		return new IntegerType();
	}

	public Type visit(StringLiteral stringLiteral) {
		return new StringType();
	}

	public Type visit(Reference reference) {
		if (symbols.containsSymbol(reference.getName())) {
			return symbols.getTypeForSymbol(reference.getName());
		}

		errors.addError(new UndefinedReference(reference.getLineNumber(),
				reference.getOffset(), reference.getName()));

		return new UndefinedType();
	}
	
	@Override
	public Type visit(BooleanType booleanType) {
		return new BooleanType();
	}

	@Override
	public Type visit(DecimalType decimalType) {
		return new DecimalType();
	}

	@Override
	public Type visit(IntegerType integerType) {
		return new IntegerType();
	}

	@Override
	public Type visit(StringType stringType) {
		return new StringType();
	}

	@Override
	public Type visit(UndefinedType undefinedType) {
		return new UndefinedType();
	}
	
	private Type visitBinaryExpression(Binary binaryExpression) {
		Expression left = binaryExpression.getLeft();
		Expression right = binaryExpression.getRight();
		Type leftType = left.accept(this);
		Type rightType = right.accept(this);
		
		if (leftType.isUndefined()) {
			errors.addError(new UndefinedReference(left.getLineNumber(), 
					left.getOffset(), left.toString()));
			return new UndefinedType();
		}

		if (rightType.isUndefined()) {
			errors.addError(new UndefinedReference(right.getLineNumber(), 
					right.getOffset(), right.toString()));
			return new UndefinedType();
		}
		
		if (leftType.equals(rightType)) {
			return leftType.accept(this);
		}
		
		Type leftPromoted = leftType.promote();
		Type rightPromoted = rightType.promote();
		
		if (leftPromoted.equals(rightPromoted)) {
			return leftPromoted.accept(this);
		}
		
		errors.addError(new InvalidOperandType(left.getLineNumber(), 
				left.getOffset(), leftType, leftType, rightType));
		
		return new UndefinedType();
	}
	
	private Type visitUnaryExpression(Unary unaryExpression) {
		Expression expression = unaryExpression.getSingleExpression();
		Type type = expression.accept(this);
		
		if (type.isUndefined()) {
			errors.addError(new UndefinedReference(expression.getLineNumber(), 
					expression.getOffset(), expression.toString()));
			return new UndefinedType();
		}
		
		return type;
	}
	
	private Type defineType(Expression expr, Type type, Type... types) {
		if (type.isIn(types)) {
			return type;
		} 
		
		errors.addError(new TypeNotAllowed(expr.getLineNumber(), 
				expr.getOffset(), type, expr.getClass().getName()));
		
		return new UndefinedType();
	}
	
}
