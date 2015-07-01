package nl.uva.se.ql.gui.widgets.questions.calculated;

import nl.uva.se.ql.ast.statement.Question;
import nl.uva.se.ql.evaluation.value.IntegerValue;
import nl.uva.se.ql.gui.mediators.Mediator;

public class IntegerCalculatedQuestion extends
		BaseCalculatedQuestion<IntegerValue> {

	public IntegerCalculatedQuestion(Question question, Mediator mediator) {
		super(question, mediator);
	}

	@Override
	public void setValue(IntegerValue value) {
		this.value = value;
		label.setText(createText(value.getValue().toString()));
	}

	@Override
	public IntegerValue getValue() {
		return value;
	}
}
