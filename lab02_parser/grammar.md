
# Alpine grammar

## Notation

* `list(X)` denotes a list of `X` elements, possibly empty, separated by `,`. For example. `IntegerList -> list(Integer)` represents lists like `1 , 2 , 3`, ` `, or `1`.
* `[ X ]` denotes an optional `X`. For example, `['+']` represents `+` or nothing.

## Grammar

```grammar
Program -> list(Declaration)
Declaration -> Binding | Function | TypeDeclaration

Binding -> 'let' Identifier [':' Type] '=' Expression
Function -> 'fun' FunctionIdentifier '(' [ValueParameterList] ')' ['->' Type] '{' Expression '}'
TypeDeclaration -> 'type' Identifier '=' Type

FunctionIdentifier -> Identifier | InfixOp
ValueParameterList -> list(ValueParameter)
ValueParameterLabel -> Label | '_'
ValueParameterId -> Identifier | '_'
ValueParameter -> [ValueParameterLabel] ValueParameterId [':' Type]


Expression -> InfixExpression
InfixExpression -> Ascribed higherPrecedence(InfixOp InfixExpression)
Ascribed -> PrefixExpression [('@' | '@!' | '@?')  Type]

// The choice between 1st and 2nd here depends on whether there is a whitespace after the operator (2) or not (1) 
PrefixExpression -> InfixOp | InfixOp CompoundExpression | CompoundExpression

CompoundExpression -> PrimaryExpression ['.' Integer | '.' Identifier | '.' InfixOp | '(' LabeledExpressionList ')']

PrimaryExpression -> Identifier | Literal | Record | IfExpression | MatchExpression | LetExpression | '(' Expression ')' | LambdaDef

Literal -> IntegerLiteral | FloatLiteral | StringLiteral | BooleanLiteral

Record -> '#' Identifier  ['(' LabeledExpressionList ')']
LabeledExpressionList -> list(LabeledExpression)
LabelExpression -> [Label ':'] Expression
Label -> Identifier | Keyword


IfExpression -> 'if' Expression 'then' Expression 'else' Expression

MatchExpression -> 'match' Expression MatchBody
MatchBody -> '{' CaseList '}'
CaseList -> list(Case)
Case -> 'case' Pattern 'then' Expression
Pattern -> '_' | RecordPattern | BindingPattern | ValuePattern
RecordPattern -> '#' Identifier [ '(' list(RecordPatternField) ')' ]
RecordPatternField -> Pattern
BindingPattern -> 'let' Identifier [':' Type]
ValuePattern -> Expression

LetExpression -> 'let' Binding 'in' Expression


Type -> SumType
SumType -> PrimaryType ['|' PrimaryType]
PrimaryType -> TypeIdentifier | '(' Type ')' | RecordType

Identifier -> IdentifierToken
IntegerLiteral -> IntegerToken
FloatLiteral -> FloatToken
StringLiteral -> StringToken
BooleanLiteral -> 'true' | 'false'

InfixOp -> '|| ' | '&&' | '<' | '<=' | '>' | '>=' | '==' | '!=' | '...' | '..<' | '+' | '-' | '| ' | '^' | '*' | '/' | '%' | '&' | '<<' | '>>' | '~' | '!'
```
