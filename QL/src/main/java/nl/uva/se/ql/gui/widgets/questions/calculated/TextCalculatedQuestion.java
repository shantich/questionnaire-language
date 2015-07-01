package nl.uva.se.ql.gui.widgets.questions.calculated;

import nl.uva.se.ql.ast.statement.Question;
import nl.uva.se.ql.evaluation.value.StringValue;
import nl.uva.se.ql.gui.mediators.Mediator;

public class TextCalculatedQuestion extends BaseCalculatedQuestion<StringValue> {

	public TextCalculatedQuestion(Question question, Mediator mediator) {
		super(question, mediator);
	}

	@Override
	public void setValue(StringValue value) {
		this.value = value;
		label.setText(createText(value.getValue().toString()));
	}

	@Override
	public StringValue getValue() {
		return value;
	}

}
