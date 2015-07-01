# questionnaire-language

Final project of the course Software Construction at the University of Amsterdam within the Master Software Engineering. The project was created in collaboration with Jeff Klaassen who was responsible for the UI.

##### Learning goals

 - Understand basic principles of language implementation (parsing,
   AST, evaluation, generation)
 - Understand basic aspects of code quality (readability,
   changeability, extensibility, etc.)
 - Understand encapsulation and modular design

##### QL Requirements

- Questions are enabled and disabled when different values are
  entered.
  
- The type checker detects:
   * reference to undefined questions
   * duplicate question declarations with different types
   * conditions that are not of the type boolean
   * operands of invalid type to operators
   * cyclic dependencies between questions
   * duplicate labels (warning)

- The language supports booleans, integers and string values.

- Different data types in QL map to different (default) GUI widgets.

Requirements on the implementation:

- The parser of the DSL is implemented using a grammar-based parser
  generator. 

- The internal structure of a DSL program is represented using
  abstract syntax trees.

- QL programs are executed as GUI programs, not command-line
  dialogues. 

- QL programs are executed by interpretation, not code generation.


Example Questionnaire:

```
form taxOfficeExample { 
  boolean hasSoldHouse : 
	"Did you sell a house in 2010?"
  boolean hasBoughtHouse : 
	"Did you buy a house in 2010?"
  boolean hasMaintLoan : 
	"Did you enter a loan?"
  integer age : 
	"Your age?"
  
  if (hasSoldHouse && hasBoughtHouse) {
  	decimal sellingPrice : 
		"What was the selling price?"
  	decimal privateDebt : 
		"Private debts for the sold house:"
  	decimal valueResidue : 
		"Value residue:" (sellingPrice - privateDebt)
  }
}
```
