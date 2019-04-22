stacksize 16
global 2 "  -> Number %u is: %u\n\0"
global 25 "\n\n\0"
global 28  "Fibonacci number generator\n==========================\n\nPlease enter 2 digits: \0"



jmp __start



//$0 is offset of string (has to end with \0)	(will be changed)
//$1 is offset of arguments						(will be changed)
//$2-$8 reserved (will be written)
//%u is the only replacement possibility (% can be escaped with \%)
printf:
	printf_loop:
		mov $3, [$0]
		jz $3, printf_end

		add $0, 1
		mov $4, [$0]

		mov $2, 92
		jne $3, $2, printf_skip_escape 	// '\'

		je $4, $2, printf_escape        // '\'
		mov $2, 37
		je $4, $2, printf_escape        // '%'
		jmp printf_error

		printf_escape:

		mov $3, $4
		add $0, 1

		printf_skip_escape:

		mov $2, 37
		jne $3, $2, printf_replace_escape  // '%'

		mov $2, 117						   // 'u'
		jne $4, $2, printf_error

		add $0, 1
		mov $7, $0
		mov $8, $1
		mov $0, [$1]
		add $8, 1
		call print_decimal
		mov $1, $8
		mov $0, $7
		jmp printf_loop

		printf_replace_escape:
		out $3
		jmp printf_loop

	jmp printf_end
	printf_error:
		out 73
		out 78
		out 86

	printf_end:
	ret

//$0 is the decimal
//$1-$6 will be changed
print_decimal:
	mov $5, $0
	mov $6, 0

	print_decimal_loop0:
		mov $3, 10
		div $0, $1, $5, $3
		add $1, 48
		push $1
		add $6, 1
		mov $5, $0
		jnz $5, print_decimal_loop0

	print_decimal_loop1:
		jz $6, print_decimal_end
		sub $6, 1
		pop $5
		out $5
		jmp print_decimal_loop1

	print_decimal_end:
	ret


__start:

mov $0, 28
call printf

mov $9, 1
mov $10, 0
mov $12, 0
in $14
in $15
sub $14,48
sub $15,48
mov $16, 10
mul $13, $14, $16
add $13, $15

mov $0, 25
call printf

main_loop:
	mov $11, $9
	add $11, $10
	mov $9, $10
	mov $10, $11

	mov $0, 2
	mov $1, 0
	mov [0], $12
	mov [1], $11
	call printf

	add $12, 1
	jl $12, $13, main_loop

