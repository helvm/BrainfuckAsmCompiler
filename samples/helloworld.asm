global 0 "Hello world.\n\0"

mov $0, 0

print_loop:
	mov $1, [$0]
	jz $1, break_print

	out $1
	add $0, 1
	jmp print_loop

break_print:
