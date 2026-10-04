# Alloc : alloue $a0 octets sur le tas (sbrk), adresse dans $v0
entryAlloc:
	li $v0, 9
	syscall
exitAlloc:
	jr $ra
