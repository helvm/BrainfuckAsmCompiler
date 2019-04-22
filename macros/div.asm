//result in $0, mod-result in $1
//calculates: $2 / $3
//if $2 OR $3 is 0, it will return $0=0, $1=0
mov $0, 0
mov $1, 0
mov $4, $2
mov $5, $3
jz $4, div_fct_end
jz $5, div_fct_end
add $4, 1

div_fct_loop0:
	mov $1, $5
	div_fct_loop1:
		sub $1, 1
		sub $4, 1
		jz $4, div_fct_loop_end
		jnz $1, div_fct_loop1
	add $0, 1
	jmp div_fct_loop0

div_fct_loop_end:
sub $5, 1
sub $5, $1
mov $1, $5

div_fct_end: