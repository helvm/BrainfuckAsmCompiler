stacksize 32

global 1 "  -> %u is divisor.\n\0"
global 22 "%u is a prime-number.\0"
global 44 "%u is not a prime-number.\0"
global 70 "\n\n\0"
global 73  "Prime test\n==========\n\nPlease enter 3 digits (between 001 and 255): \0"



mov $0, 73
call printf

in $0
sub $0, 48
in $1
sub $1, 48
in $2
sub $2, 48
mov $5, 100
mul $3, $0, $5
mov $5, 10
mul $4, $1, $5
add $3, $4
add $3, $2

mov $0, 70
push $3
call printf
pop $3

mov $4, $3
mov $5, 2
mov $6, 0
jle $4, $5, skip_primetest_loop

sub $4, 1

primetest_loop:
	div $0, $1, $3, $4
	jnz $1, primetest_loop_no_divisor

	push $4
	add $6, 1

	primetest_loop_no_divisor:
	sub $4, 1
	mov $5, 2
	jge $4, $5, primetest_loop

skip_primetest_loop:
mov $0, 70
push $3
push $6
call printf
pop $6
pop $3

jz $6, is_prime

push $6
mov $0, 44
mov $1, 0
mov [0], $3
call printf
mov $0, 70
call printf
pop $6

primetest_print_primes_loop:
	pop $0
	mov [0], $0
	mov $0, 1
	mov $1, 0
	push $6
	call printf
	pop $6
	sub $6, 1
	jnz $6, primetest_print_primes_loop

jmp __end

is_prime:

mov $0, 22
mov $1, 0
mov [0], $3
call printf
mov $0, 70
call printf


jmp __end


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


__end: