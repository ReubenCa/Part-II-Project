**Specification**

A caro program can be as simple as a value of type int float or bool e.g.:
'5'
'10f'
'1.2'
'true'
'false'
Are all Caro Programs.

Caro supports operators:

'+'
'+.'
'-'
'-.'
'*'
'*.'
'/'
'/.'
'=='
'==.'
'>='
'>'
'<='
'<'
'!='
'!=.'
'mod'

Operators with the dot are for floats and the others are for ints. Operators are applied prefix so

'+ 5 5'

is a Caro program that evaluates to 10


if statements are supported

'if (==) 5 5 then 1 else 2'

will evaluate to 1


Functions are also supported.

(fun x -> x) 5

will apply the identity function to 5. More specifically it will substitute 5 into every occurence of 'x' on the right hand side of the arrow.

Let statements are supported.
'Let ten = 10 in (+) ten 1'
will evaluate to 11.

We can also recursively bind functions so:
let rec f = (fun x -> if (==) x 0 then 1 else f 0) in f 10
will evaluate to 1.


Lists can be specified as square brackets seperated by semi colons e.g:

[1;2;3]

Basic match statements are supported that match lists to either the empty list or head::tail 

For example

match [1;2;3] with

| [] -> 5

| head :: tail -> head


will evalute to 1


